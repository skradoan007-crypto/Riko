package com.rikodev.music.android.dna.data

import android.content.Context
import com.rikodev.music.android.dna.analysis.*
import com.rikodev.music.android.dna.data.DnaProfiles
import com.rikodev.music.android.dna.genome.*
import com.rikodev.music.android.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.rikodev.music.android.dna.data.dnaKey
import javax.inject.Inject

@javax.inject.Singleton
class DnaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyzer: OfflineDnaAnalyzer,
    private val profiles: DnaProfiles
) {
    private val gate = Mutex()
    suspend fun genome(song: Song): DnaGenome? = gate.withLock { withContext(Dispatchers.IO) {
        // Local cache key only. Genome identity itself comes exclusively from audio content/features.
        val key = song.dnaKey()
        profiles.load(key)?.let { if (timelineFile(key).exists()) return@withContext it }
        when(val result = analyzer.analyze(context,song.uri,song.duration)) {
            is DnaAnalysisOutcome.Complete -> {
                val a = result.analysis
                val genome = DnaGenomeMapper.generate(a.audioSha256,a.features,DnaAlgorithmVersion.GENESIS_V1)
                val atomic = android.util.AtomicFile(timelineFile(key))
                val stream = atomic.startWrite()
                try { DnaTimelineCodec.write(a.timeline, stream); atomic.finishWrite(stream) }
                catch (error: Throwable) { atomic.failWrite(stream); throw error }
                profiles.storeIfAbsent(key,a.features,genome,song)
                profiles.load(key)
            }
            is DnaAnalysisOutcome.Unavailable -> null
        }
    }
    }
    private fun timelineFile(key: String) = java.io.File(context.filesDir, "mdna-timelines/$key.bin").also { it.parentFile?.mkdirs() }
    suspend fun timeline(song: Song): DnaTimeline? = withContext(Dispatchers.IO) {
        val file = timelineFile(song.dnaKey())
        if (!file.exists()) genome(song)
        gate.withLock { if (!file.exists()) null else android.util.AtomicFile(file).openRead().use { DnaTimelineCodec.read(it) } }
    }
}
