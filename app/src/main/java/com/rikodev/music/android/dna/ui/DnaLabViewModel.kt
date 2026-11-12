package com.rikodev.music.android.dna.ui

import androidx.lifecycle.ViewModel
import com.rikodev.music.android.dna.data.DnaRepository
import com.rikodev.music.android.dna.data.DnaProfiles
import com.rikodev.music.android.dna.data.dnaKey
import com.rikodev.music.android.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DnaLabViewModel @Inject constructor(private val repository: DnaRepository,
    private val profiles: DnaProfiles) : ViewModel() {
    suspend fun timeline(song: Song) = repository.timeline(song)
    suspend fun genome(song: Song) = repository.genome(song)
    suspend fun generation(song: Song) = profiles.generation(song.dnaKey())
}
