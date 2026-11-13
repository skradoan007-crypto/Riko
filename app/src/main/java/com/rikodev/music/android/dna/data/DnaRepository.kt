package com.rikodev.music.android.dna.data

import android.content.Context
import com.rikodev.music.android.dna.analysis.*
import com.rikodev.music.android.dna.data.DnaProfiles
import com.rikodev.music.android.dna.genome.*
import com.rikodev.music.android.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.rikodev.music.android.dna.data.dnaKey
import javax.inject.Inject

sealed class DnaProfileResult {
    data class Ready(val genome: DnaGenome, val timeline: DnaTimeline) : DnaProfileResult()
    data class Unavailable(val reason: DnaAnalysisFailure, val diagnostic: String? = null) : DnaProfileResult()
}

@javax.inject.Singleton
class DnaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyzer: OfflineDnaAnalyzer,
    private val profiles: DnaProfiles
) {
    private val gate = Mutex()

    /** One attempt returns the genome and timeline together; failure never triggers a second decode. */
    suspend fun load(song: Song): DnaProfileResult = gate.withLock { withContext(Dispatchers.IO) {
        try {
            val key = song.dnaKey()
            val cached = profiles.load(key)
            if (cached != null) {
                val timeline = try {
                    android.util.AtomicFile(timelineFile(key)).openRead().use { DnaTimelineCodec.read(it) }
                } catch (cancel: CancellationException) { throw cancel }
                catch (_: java.io.IOException) { null }
                catch (_: IllegalArgumentException) { null }
                // A missing/interrupted/corrupt cache can be rebuilt without deleting the profile/history.
                if (timeline != null) return@withContext DnaProfileResult.Ready(cached, timeline)
            }
            when (val result = analyzer.analyze(context, song.uri, song.duration)) {
                is DnaAnalysisOutcome.Unavailable -> DnaProfileResult.Unavailable(result.reason, result.diagnostic)
                is DnaAnalysisOutcome.Complete -> {
                    val a = result.analysis
                    val genome = DnaGenomeMapper.generate(a.audioSha256, a.features, DnaAlgorithmVersion.GENESIS_V1)
                    val atomic = android.util.AtomicFile(timelineFile(key))
                    val stream = atomic.startWrite()
                    try { DnaTimelineCodec.write(a.timeline, stream); atomic.finishWrite(stream) }
                    catch (error: Throwable) { atomic.failWrite(stream); throw error }
                    profiles.storeIfAbsent(key, a.features, genome, song)
                    DnaProfileResult.Ready(requireNotNull(profiles.load(key)), a.timeline)
                }
            }
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { DnaProfileResult.Unavailable(DnaAnalysisFailure.STORAGE) }
    } }

    // Existing background/export callers retain their nullable contracts.
    suspend fun genome(song: Song): DnaGenome? = (load(song) as? DnaProfileResult.Ready)?.genome
    suspend fun timeline(song: Song): DnaTimeline? = (load(song) as? DnaProfileResult.Ready)?.timeline

    private fun timelineFile(key: String) = java.io.File(context.filesDir, "mdna-timelines/$key.bin")
        .also { it.parentFile?.mkdirs() }
}
