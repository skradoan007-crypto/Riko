package com.rikodev.music.android.presentation.allsongs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rikodev.music.android.data.repository.MusicRepository
import com.rikodev.music.android.data.repository.PlaylistRepository
import com.rikodev.music.android.data.repository.SortBy
import com.rikodev.music.android.data.repository.SortOrder
import com.rikodev.music.android.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.hilt.navigation.compose.hiltViewModel

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — ALL SONGS VIEWMODEL
// Scans entire device library via MediaStore, supports sort + favorite toggle.
// ─────────────────────────────────────────────────────────────────────────────

data class AllSongsUiState(
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
    val sortBy: SortBy = SortBy.TITLE,
    val sortOrder: SortOrder = SortOrder.ASCENDING,
    val totalDurationFormatted: String = "0:00",
    // Real "Recently Added" — independently sorted by actual MediaStore
    // DATE_ADDED regardless of the user's chosen main sort, capped to a
    // reasonable carousel length. Not derived from `songs` (which may be
    // sorted by title/artist/etc.) — its own real query.
    val recentlyAdded: List<Song> = emptyList()
)

@HiltViewModel
class AllSongsViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _sortBy = MutableStateFlow(SortBy.TITLE)
    private val _sortOrder = MutableStateFlow(SortOrder.ASCENDING)
    private val sortParams = combine(_sortBy, _sortOrder) { by, order -> by to order }

    private val sortedSongs = sortParams.flatMapLatest { (sortBy, sortOrder) ->
        musicRepository.getAllSongs(sortBy, sortOrder)
    }

    private val recentlyAdded = musicRepository.getAllSongs(SortBy.DATE_ADDED, SortOrder.DESCENDING)

    val uiState: StateFlow<AllSongsUiState> = combine(
        sortedSongs, _sortBy, _sortOrder, recentlyAdded
    ) { songs, sortBy, sortOrder, recent ->
        val totalMs = songs.sumOf { it.duration }
        val totalMinutes = totalMs / 60000
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        val formatted = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

        AllSongsUiState(
            songs = songs,
            isLoading = false,
            sortBy = sortBy,
            sortOrder = sortOrder,
            totalDurationFormatted = formatted,
            recentlyAdded = recent.take(10)
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AllSongsUiState())

    fun setSortBy(sortBy: SortBy) {
        _sortBy.value = sortBy
    }

    fun toggleSortOrder() {
        _sortOrder.value = if (_sortOrder.value == SortOrder.ASCENDING)
            SortOrder.DESCENDING else SortOrder.ASCENDING
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            playlistRepository.toggleFavorite(songId)
        }
    }
}
