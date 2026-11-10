package com.rikodev.music.android.audiofx

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/** Offline full-song analysis. Playback's PCM/FFT tap remains untouched: it
 * cannot supply unplayed audio. At most one extra decoder is active at a time.
 * Unsupported output, timeout and partial decodes yield no waveform, never fake data.
 */
@Singleton
class AudioFileAnalyzer @Inject constructor() {
    private val decoderLock = Mutex()
    suspend fun analyze(context: Context, uri: Uri, bucketCount: Int = 1024,
                        durationHintMs: Long = 0L): WaveformData? = withContext(Dispatchers.IO) {
        decoderLock.withLock {
            val jobContext = currentCoroutineContext()
            val tid = android.os.Process.myTid()
            val originalPriority = android.os.Process.getThreadPriority(tid)
            try { android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND) } catch (_: Exception) { }
            android.os.Trace.beginSection("RIKO.Waveform.Decode")
            var extractor: MediaExtractor? = null
            var codec: MediaCodec? = null
            try {
                val source = MediaExtractor().also { extractor = it }
                source.setDataSource(context, uri, null)
                val track = (0 until source.trackCount).firstOrNull {
                    source.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
                } ?: return@withLock null
                val format = source.getTrackFormat(track)
                source.selectTrack(track)
                val startPts = source.sampleTime.coerceAtLeast(0L)
                val hint = if (format.containsKey(MediaFormat.KEY_DURATION))
                    format.getLong(MediaFormat.KEY_DURATION).coerceAtLeast(0L) else durationHintMs.coerceAtLeast(0L) * 1000L
                val buckets = WaveformBuckets(hint)
                // Raw WAV/PCM is already decoded; Android need not provide an
                // audio/raw MediaCodec. Still use the exact same bucket reducer.
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
                        buckets.addPcm(input, 0, size, source.sampleTime - startPts, rate, channels, encoding) {
                            jobContext.ensureActive()
                        }
                        if (!source.advance()) break
                    }
                    return@withLock buckets.finish(bucketCount, hint)
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
                            require(encoding == PcmFrames.ENCODING_PCM_16BIT || encoding == PcmFrames.ENCODING_PCM_FLOAT)
                            progressed = true
                        }
                        index >= 0 -> {
                            try {
                                if (info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                                    val output = requireNotNull(decoder.getOutputBuffer(index)).order(ByteOrder.LITTLE_ENDIAN)
                                    buckets.addPcm(output, info.offset, info.size, info.presentationTimeUs - startPts,
                                        rate, channels, encoding) { jobContext.ensureActive() }
                                }
                                outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                            } finally { decoder.releaseOutputBuffer(index, false) }
                            progressed = true
                        }
                    }
                    if (progressed) lastProgress = System.nanoTime()
                    else if (System.nanoTime() - lastProgress > 5_000_000_000L) return@withLock null
                }
                jobContext.ensureActive()
                buckets.finish(bucketCount, hint)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                Log.w("AudioFileAnalyzer", "Analysis unavailable: ${error.javaClass.simpleName}")
                null
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
