package com.rikodev.music.android.dna.analysis

import android.content.Context
import android.net.Uri
import com.rikodev.music.android.audiofx.PcmSource
import com.rikodev.music.android.audiofx.PcmConsumer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

sealed class DnaAnalysisOutcome {
    data class Complete(val analysis: DnaAnalysis) : DnaAnalysisOutcome()
    // Deliberately does not expose file paths, URIs or codec exception messages.
    data class Unavailable(val category: String) : DnaAnalysisOutcome()
}

/** Shares the waveform decoder's concurrency gate; all work remains offline and off UI. */
@Singleton
class OfflineDnaAnalyzer @Inject constructor(private val pcmSource: PcmSource) {
    suspend fun analyze(context: Context, uri: Uri, durationHintMs: Long = 0L): DnaAnalysisOutcome =
        withContext(Dispatchers.IO) {
            val job = currentCoroutineContext()
            try {
                val analyzer = DnaStreamingAnalyzer(Runnable { job.ensureActive() })
                pcmSource.decode(context, uri, durationHintMs, object : PcmConsumer {
                    override fun onStart(durationUs: Long) = Unit
                    override fun onPcm(buffer: ByteBuffer, offset: Int, size: Int, ptsUs: Long,
                        sampleRate: Int, channels: Int, encoding: Int, checkCancelled: () -> Unit) {
                        checkCancelled()
                        analyzer.accept(buffer, offset, size, ptsUs, sampleRate, channels, encoding)
                    }
                })
                job.ensureActive()
                DnaAnalysisOutcome.Complete(analyzer.finish())
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                DnaAnalysisOutcome.Unavailable(error.javaClass.simpleName)
            }
        }
}
