package com.rikodev.music.android.audiofx

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/** Shared single-decoder offline PCM source. Borrowed buffers are consumed synchronously.
 * Extracted from AudioFileAnalyzer; waveform and DNA never own duplicate decode loops.
 */
@Singleton
class AndroidPcmSource @Inject constructor() : PcmSource {
    private val decoderLock = Mutex()
    override suspend fun decode(context: Context, uri: Uri, durationHintMs: Long,
                                consumer: PcmConsumer): Long = withContext(Dispatchers.IO) {
        decoderLock.withLock {
            val jobContext = currentCoroutineContext()
            val tid = android.os.Process.myTid()
            val originalPriority = android.os.Process.getThreadPriority(tid)
            try { android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND) } catch (_: Exception) { }
            android.os.Trace.beginSection("RIKO.Offline.Decode")
            var extractor: MediaExtractor? = null
            var codec: MediaCodec? = null
            try {
                require(uri.scheme == null || uri.scheme == "file" || uri.scheme == "content") {
                    "Offline analysis accepts local files and content providers only"
                }
                val source = MediaExtractor().also { extractor = it }
                source.setDataSource(context, uri, null)
                val track = (0 until source.trackCount).firstOrNull {
                    source.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
                } ?: throw IllegalArgumentException("No audio track")
                val format = source.getTrackFormat(track)
                val timestampNormalizer = Mp3PcmTimestampNormalizer(format.getString(MediaFormat.KEY_MIME) == "audio/mpeg")
                source.selectTrack(track)
                val startPts = source.sampleTime.coerceAtLeast(0L)
                val hint = if (format.containsKey(MediaFormat.KEY_DURATION))
                    format.getLong(MediaFormat.KEY_DURATION).coerceAtLeast(0L) else durationHintMs.coerceIn(0L, Long.MAX_VALUE / 1000L) * 1000L
                consumer.onStart(hint)
                var observedEndUs = 0L
                fun deliver(buffer: java.nio.ByteBuffer, offset: Int, size: Int, pts: Long,
                            sampleRate: Int, channelCount: Int, pcmEncoding: Int) {
                    require(sampleRate > 0 && channelCount in 1..32) { "Unsupported PCM format" }
                    require(pcmEncoding == PcmFrames.ENCODING_PCM_16BIT || pcmEncoding == PcmFrames.ENCODING_PCM_FLOAT) { "Unsupported PCM format" }
                    val stride = PcmFrames.bytesPerSample(pcmEncoding) * channelCount
                    require(size % stride == 0 && offset >= 0 && size >= 0 && offset.toLong() + size <= buffer.limit()) { "Invalid PCM bounds/alignment" }
                    val canonicalPts = timestampNormalizer.normalize(pts, size / stride, sampleRate)
                    consumer.onPcm(buffer, offset, size, canonicalPts, sampleRate, channelCount, pcmEncoding) {
                        jobContext.ensureActive()
                    }
                    if (size > 0) observedEndUs = maxOf(observedEndUs,
                        canonicalPts + size.toLong() / stride * 1_000_000L / sampleRate)
                }
                fun verifyComplete() {
                    jobContext.ensureActive()
                    if (observedEndUs <= 0) throw java.io.IOException("Empty decode")
                    if (hint > 0 && observedEndUs + maxOf(250_000L, hint / 100) < hint)
                        throw java.io.IOException("Decode ended before declared duration")
                }
                // Raw WAV/PCM is already decoded; Android need not provide an
                // audio/raw MediaCodec. Deliver the same borrowed-buffer contract.
                if (format.getString(MediaFormat.KEY_MIME) == "audio/raw") {
                    val encoding = if (format.containsKey(MediaFormat.KEY_PCM_ENCODING))
                        format.getInteger(MediaFormat.KEY_PCM_ENCODING) else PcmFrames.ENCODING_PCM_16BIT
                    val rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    val maximum = if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE))
                        format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) else 256 * 1024
                    val input = java.nio.ByteBuffer.allocateDirect(maximum.coerceIn(32 * 1024, 1024 * 1024))
                        .order(ByteOrder.LITTLE_ENDIAN)
                    while (true) {
                        jobContext.ensureActive()
                        input.clear()
                        val size = source.readSampleData(input, 0)
                        if (size < 0) break
                        require(size <= input.capacity()) { "Raw PCM chunk exceeds buffer capacity" }
                        deliver(input, 0, size, source.sampleTime - startPts, rate, channels, encoding)
                        if (!source.advance()) break
                    }
                    verifyComplete()
                    return@withLock hint
                }
                format.setInteger(MediaFormat.KEY_PCM_ENCODING, PcmFrames.ENCODING_PCM_16BIT)
                val decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!).also { codec = it }
                decoder.configure(format, null, null, 0)
                decoder.start()
                var rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                var encoding = PcmFrames.ENCODING_PCM_16BIT
                var inputDone = false
                var outputDone = false
                var lastProgress = System.nanoTime()
                val info = MediaCodec.BufferInfo()
                while (!outputDone) {
                    jobContext.ensureActive()
                    var progressed = false
                    if (!inputDone) {
                        val index = decoder.dequeueInputBuffer(0L)
                        if (index >= 0) {
                            val input = requireNotNull(decoder.getInputBuffer(index))
                            input.clear()
                            val size = source.readSampleData(input, 0)
                            if (size < 0) {
                                decoder.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                decoder.queueInputBuffer(index, 0, size, source.sampleTime, 0)
                                source.advance()
                            }
                            progressed = true
                        }
                    }
                    val index = decoder.dequeueOutputBuffer(info, if (progressed) 0L else 1_000L)
                    when {
                        index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val output = decoder.outputFormat
                            rate = output.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            channels = output.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                            encoding = if (output.containsKey(MediaFormat.KEY_PCM_ENCODING))
                                output.getInteger(MediaFormat.KEY_PCM_ENCODING) else PcmFrames.ENCODING_PCM_16BIT
                            require(encoding == PcmFrames.ENCODING_PCM_16BIT || encoding == PcmFrames.ENCODING_PCM_FLOAT) { "Unsupported PCM format" }
                            progressed = true
                        }
                        index >= 0 -> {
                            try {
                                if (info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                                    val output = requireNotNull(decoder.getOutputBuffer(index)).order(ByteOrder.LITTLE_ENDIAN)
                                    deliver(output, info.offset, info.size, info.presentationTimeUs - startPts,
                                        rate, channels, encoding)
                                }
                                outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                            } finally { decoder.releaseOutputBuffer(index, false) }
                            progressed = true
                        }
                    }
                    if (progressed) lastProgress = System.nanoTime()
                    else if (System.nanoTime() - lastProgress > 5_000_000_000L) throw java.io.IOException("Decoder stalled")
                }
                verifyComplete()
                hint
            } finally {
                try { codec?.stop() } catch (_: Exception) { }
                try { codec?.release() } catch (_: Exception) { }
                try { extractor?.release() } catch (_: Exception) { }
                android.os.Trace.endSection()
                try { android.os.Process.setThreadPriority(originalPriority) } catch (_: Exception) { }
            }
        }
    }
}
