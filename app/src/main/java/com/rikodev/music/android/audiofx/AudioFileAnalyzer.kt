package com.rikodev.music.android.audiofx

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Existing waveform API; decoding is now owned by the shared offline PCM source. */
@Singleton
class AudioFileAnalyzer @Inject constructor(
    private val pcmSource: PcmSource
) {
    suspend fun analyze(context: Context, uri: Uri, bucketCount: Int = 1024,
                        durationHintMs: Long = 0L): WaveformData? = withContext(Dispatchers.IO) {
        require(bucketCount in 1..4096)
        var buckets: WaveformBuckets? = null
        try {
            val hint = pcmSource.decode(context, uri, durationHintMs, object : PcmConsumer {
                override fun onStart(durationUs: Long) { buckets = WaveformBuckets(durationUs) }
                override fun onPcm(buffer: java.nio.ByteBuffer, offset: Int, size: Int, ptsUs: Long,
                    sampleRate: Int, channels: Int, encoding: Int, checkCancelled: () -> Unit) {
                    requireNotNull(buckets).addPcm(buffer, offset, size, ptsUs, sampleRate,
                        channels, encoding, checkCancelled)
                }
            })
            currentCoroutineContext().ensureActive()
            buckets?.finish(bucketCount, hint)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            Log.w("AudioFileAnalyzer", "Analysis unavailable: ${error.javaClass.simpleName}")
            null
        }
    }
}
