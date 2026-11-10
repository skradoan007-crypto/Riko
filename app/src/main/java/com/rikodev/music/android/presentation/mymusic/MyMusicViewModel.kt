package com.rikodev.music.android.presentation.mymusic

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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — MY MUSIC VIEWMODEL (PLAN 4)
//
// Presentation/query composition ONLY (Plan 4 scope) — no data-system changes.
//   Songs:   MusicRepository.getAllSongs(sortBy, sortOrder) — REAL MediaStore sort
//   Albums:  client-side groupBy albumId   (same contract as Plan 3, spec §25)
//   Artists: client-side groupBy artist
//   Folders: client-side groupBy song.path parent directory
//
// Sort state is MY-MUSIC-LOCAL — it never leaks into Home or other screens
// (spec §34: no global sort state causing cross-mode weirdness).
// ─────────────────────────────────────────────────────────────────────────────

/** Utility shelf item — a device folder holding songs (derived from paths). */
data class FolderUi(
    val id: String,          // absolute folder path = stable key (spec §26)
    val name: String,        // last path segment
    val songCount: Int,
    val songs: List<Song>
)

data class MyMusicUiState(
    val isLoading: Boolean = true,
    val error: Boolean = false,
    val songs: List<Song> = emptyList(),
    val folders: List<FolderUi> = emptyList(),
    val totalDurationFormatted: String = "0:00",
    val sortBy: SortBy = SortBy.TITLE,
    val sortOrder: SortOrder = SortOrder.ASCENDING
)

@HiltViewModel
class MyMusicViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _sortBy = MutableStateFlow(SortBy.TITLE)
    private val _sortOrder = MutableStateFlow(SortOrder.ASCENDING)
    private val refreshKey = MutableStateFlow(0)
    private val sortParams = combine(_sortBy, _sortOrder) { by, order -> by to order }

    val uiState: StateFlow<MyMusicUiState> = refreshKey.flatMapLatest {
        combine(
            sortParams.flatMapLatest { (by, order) ->
                musicRepository.getAllSongs(by, order)
            },
            _sortBy,
            _sortOrder
        ) { songs, by, order ->
            buildState(songs, by, order)
        }.catch {
            emit(
                MyMusicUiState(
                    isLoading = false,
                    error = true,
                    sortBy = _sortBy.value,
                    sortOrder = _sortOrder.value
                )
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MyMusicUiState())

    /** P0 FIX: Scan Music button — force a MediaStore rescan via shared cache. */
    fun scanLibrary() {
        musicRepository.refresh()
    }

    private fun buildState(songs: List<Song>, by: SortBy, order: SortOrder): MyMusicUiState {
        // PERF P2 (RC-12): string parent — was File(...).parent stat() per song on Main
        val folders = songs.groupBy { song ->
            song.path.substringBeforeLast('/', "").ifEmpty { "Unknown" }
        }.map { (path, group) ->
            FolderUi(
                id = path,
                name = path.substringAfterLast('/').ifEmpty { path },
                songCount = group.size,
                songs = group
            )
        }.sortedBy { it.name }

        val totalMs = songs.sumOf { it.duration }
        val totalMinutes = totalMs / 60000
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        val formatted = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

        return MyMusicUiState(
            isLoading = false,
            songs = songs,
            folders = folders,
            totalDurationFormatted = formatted,
            sortBy = by,
            sortOrder = order
        )
    }

    fun setSortBy(sortBy: SortBy) {
        _sortBy.value = sortBy
    }

    fun toggleSortOrder() {
        _sortOrder.value = if (_sortOrder.value == SortOrder.ASCENDING) {
            SortOrder.DESCENDING
        } else {
            SortOrder.ASCENDING
        }
    }

    fun retry() {
        refreshKey.value++
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            playlistRepository.toggleFavorite(songId)
        }
    }
}