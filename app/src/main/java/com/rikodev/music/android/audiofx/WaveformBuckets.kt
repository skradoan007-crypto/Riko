package com.rikodev.music.android.audiofx

import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/** Bounded streaming PCM reduction. RMS is weighted by samples, peak is a MAX.
 * Time comes from decoder PTS plus frame/sampleRate, never decoder buffer index.
 * Unknown/long durations merge adjacent cells without retaining raw PCM.
 */
internal class WaveformBuckets(durationHintUs: Long, private val capacity: Int = 4096) {
    private var widthUs = max(1_000L, (durationHintUs.coerceAtLeast(1L) + capacity - 1) / capacity)
    private val energy = DoubleArray(capacity)
    private val counts = LongArray(capacity)
    private val peaks = FloatArray(capacity)
    var endUs: Long = 0L
        private set
    private fun growTo(timeUs: Long) {
        while (timeUs / widthUs >= capacity) {
            for (i in 0 until capacity / 2) {
                energy[i] = energy[i * 2] + energy[i * 2 + 1]
                counts[i] = counts[i * 2] + counts[i * 2 + 1]
                peaks[i] = max(peaks[i * 2], peaks[i * 2 + 1])
            }
            java.util.Arrays.fill(energy, capacity / 2, capacity, 0.0)
            java.util.Arrays.fill(counts, capacity / 2, capacity, 0L)
            java.util.Arrays.fill(peaks, capacity / 2, capacity, 0f)
            widthUs *= 2
        }
    }
    fun addPcm(buffer: ByteBuffer, offset: Int, size: Int, ptsUs: Long,
               sampleRate: Int, channels: Int, encoding: Int, checkCancelled: () -> Unit = {}) {
        require(sampleRate > 0 && channels in 1..32)
        require(encoding == PcmFrames.ENCODING_PCM_16BIT || encoding == PcmFrames.ENCODING_PCM_FLOAT)
        require(offset >= 0 && size >= 0 && offset.toLong() + size <= buffer.limit())
        val bytes = PcmFrames.bytesPerSample(encoding)
        val frames = size / (bytes * channels)
        for (frame in 0 until frames) {
            if (frame % 2048 == 0) checkCancelled()
            val timeUs = ptsUs + frame * 1_000_000L / sampleRate
            if (timeUs < 0L) continue // codec priming before timeline zero
            require(timeUs < 366L * 24 * 60 * 60 * 1_000_000) { "Invalid decoder timestamp" }
            growTo(timeUs)
            val cell = (timeUs / widthUs).toInt()
            var frameEnergy = 0.0
            var framePeak = 0f
            for (channel in 0 until channels) {
                val index = offset + (frame * channels + channel) * bytes
                val value = if (encoding == PcmFrames.ENCODING_PCM_FLOAT) buffer.getFloat(index)
                    else buffer.getShort(index) / 32768f
                val sample = if (value.isFinite()) value.coerceIn(-1f, 1f) else 0f
                framePeak = max(framePeak, abs(sample))
                frameEnergy += sample.toDouble() * sample
            }
            // Average channel ENERGY, not phase-cancellable mono amplitude.
            energy[cell] += frameEnergy / channels
            counts[cell]++
            peaks[cell] = max(peaks[cell], framePeak)
            endUs = max(endUs, timeUs + (1_000_000L + sampleRate - 1) / sampleRate)
        }
    }
    fun finish(bucketCount: Int, durationHintUs: Long): WaveformData? {
        if (endUs <= 0L) return null
        require(bucketCount in 1..4096)
        // Keep the container's timeline when a codec emits a short padding
        // tail. Large metadata disagreements fall back to observed duration.
        val tolerance = maxOf(250_000L, durationHintUs / 100L)
        val duration = if (durationHintUs > 0L && endUs <= durationHintUs + tolerance)
            durationHintUs else max(endUs, durationHintUs)
        val outPeak = FloatArray(bucketCount)
        val outEnergy = DoubleArray(bucketCount)
        val outCount = DoubleArray(bucketCount)
        for (cell in counts.indices) {
            if (counts[cell] == 0L) continue
            val start = cell * widthUs
            val end = minOf(start + widthUs, duration, endUs)
            if (end <= start) continue
            val first = (start.toDouble() * bucketCount / duration).toInt().coerceIn(0, bucketCount - 1)
            val last = ((end - 1).toDouble() * bucketCount / duration).toInt().coerceIn(first, bucketCount - 1)
            for (bucket in first..last) {
                val left = maxOf(start.toDouble(), bucket.toDouble() * duration / bucketCount)
                val right = minOf(end.toDouble(), (bucket + 1.0) * duration / bucketCount)
                val weight = (right - left) / (end - start)
                outPeak[bucket] = max(outPeak[bucket], peaks[cell])
                outEnergy[bucket] += energy[cell] * weight
                outCount[bucket] += counts[cell] * weight
            }
        }
        val outRms = FloatArray(bucketCount) {
            if (outCount[it] > 0.0) sqrt(outEnergy[it] / outCount[it]).toFloat().coerceAtMost(outPeak[it]) else 0f
        }
        return WaveformData(outPeak, outRms, bucketCount, duration)
    }
}
