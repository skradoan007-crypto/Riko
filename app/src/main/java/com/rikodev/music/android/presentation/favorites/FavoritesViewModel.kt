package com.rikodev.music.android.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rikodev.music.android.data.repository.MusicRepository
import com.rikodev.music.android.data.repository.PlaylistRepository
import com.rikodev.music.android.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — FAVORITES VIEWMODEL
// Real filter of the same song list AllSongsViewModel uses (already carries a
// real isFavorite flag per song from favoriteDao) — not a separate/duplicated
// data source, just a different view of the same real data.
// ─────────────────────────────────────────────────────────────────────────────

data class FavoritesUiState(
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
    /** G-7: the one state this screen was missing. */
    val error: Boolean = false
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    /** Bumping this key re-subscribes the upstream flow (error retry). */
    private val refreshKey = MutableStateFlow(0)

    val uiState: StateFlow<FavoritesUiState> = refreshKey.flatMapLatest {
        musicRepository.getAllSongs()
            .map { songs -> FavoritesUiState(songs = songs.filter { it.isFavorite }, isLoading = false) }
            .catch { emit(FavoritesUiState(isLoading = false, error = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FavoritesUiState())

    fun retry() {
        refreshKey.value += 1
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            playlistRepository.toggleFavorite(songId)
        }
    }
}
