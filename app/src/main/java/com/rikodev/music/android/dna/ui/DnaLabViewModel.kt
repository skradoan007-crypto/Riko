package com.rikodev.music.android.dna.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import com.rikodev.music.android.dna.analysis.*
import com.rikodev.music.android.dna.data.DnaProfiles
import com.rikodev.music.android.dna.genome.*
import com.rikodev.music.android.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject

@HiltViewModel
class DnaLabViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyzer: OfflineDnaAnalyzer,
    private val profiles: DnaProfiles
) : ViewModel() {
    suspend fun genome(song: Song): DnaGenome? = withContext(Dispatchers.IO) {
        // Local cache key only. Genome identity itself comes exclusively from audio content/features.
        val key = MessageDigest.getInstance("SHA-256")
            .digest("${song.uri}|${song.size}|${song.dateModified}".toByteArray(Charsets.UTF_8))
            .joinToString("") { (it.toInt() and 255).toString(16).padStart(2,'0') }
        profiles.load(key)?.let { return@withContext it }
        when(val result = analyzer.analyze(context,song.uri,song.duration)) {
            is DnaAnalysisOutcome.Complete -> {
                val a = result.analysis
                val genome = DnaGenomeMapper.generate(a.audioSha256,a.features,DnaAlgorithmVersion.GENESIS_V1)
                profiles.storeIfAbsent(key,a.features,genome)
                profiles.load(key)
            }
            is DnaAnalysisOutcome.Unavailable -> null
        }
    }
}
