package com.rikodev.music.android.audiofx

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64

/** Versioned payload in the existing Room text column; no destructive DB migration.
 * Old ambiguous CSV (even-length RMS mistaken for peak/RMS pairs) is a cache miss.
 * Locale-independent, two unsigned 16-bit values per bucket (~5.5KB / 1024 buckets).
 */
internal object WaveformCacheCodec {
    const val VERSION = "riko-wf3"
    fun encode(identity: String, data: WaveformData): String {
        val bytes = ByteBuffer.allocate(data.bucketCount * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until data.bucketCount) {
            bytes.putShort((data.peakAt(i) * 65535f + 0.5f).toInt().toShort())
            bytes.putShort((data.rmsAt(i) * 65535f + 0.5f).toInt().toShort())
        }
        return "$VERSION|$identity|${data.durationUs}|${data.bucketCount}|${Base64.getEncoder().encodeToString(bytes.array())}"
    }
    fun decode(identity: String, payload: String): WaveformData? = try {
        require(payload.length < 24_000)
        val parts = payload.split('|', limit = 5)
        require(parts.size == 5 && parts[0] == VERSION && parts[1] == identity)
        val duration = parts[2].toLong()
        val count = parts[3].toInt()
        require(count in 1..4096 && duration > 0L)
        val bytes = Base64.getDecoder().decode(parts[4])
        require(bytes.size == count * 4)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val peaks = FloatArray(count)
        val rms = FloatArray(count)
        for (i in 0 until count) {
            peaks[i] = (buffer.short.toInt() and 65535) / 65535f
            rms[i] = (buffer.short.toInt() and 65535) / 65535f
        }
        WaveformData(peaks, rms, count, duration)
    } catch (_: IllegalArgumentException) { null }
}
