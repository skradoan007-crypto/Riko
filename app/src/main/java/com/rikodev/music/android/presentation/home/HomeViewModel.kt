package com.rikodev.music.android.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rikodev.music.android.data.db.dao.PlayHistoryDao
import com.rikodev.music.android.data.repository.MusicRepository
import com.rikodev.music.android.data.repository.PlaylistRepository
import com.rikodev.music.android.data.repository.SortBy
import com.rikodev.music.android.data.repository.SortOrder
import com.rikodev.music.android.domain.model.Playlist
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
// RIKO — HOME VIEWMODEL (PLAN 3)
//
// Combines EXISTING data sources only — no duplicate data systems (spec §43):
//   MusicRepository.getAllSongs()        → songs (A–Z) + derived albums/artists
//   MusicRepository.getAllSongs(DATE_ADDED) → recently added
//   PlayHistoryDao.getRecentlyPlayed()   → real listening history (READ-ONLY;
//                                          the DAO already existed unused)
//   PlaylistRepository.getFavoriteSongIds() → favorites
//   PlaylistRepository.getAllPlaylists() → playlists
//
// NO Media3/Room/repo/scanner modification. Playback actions stay in
// PlayerViewModel at the screen level (same pattern as AllSongsScreen).
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    playHistoryDao: PlayHistoryDao
) : ViewModel() {

    /** Bumped by retry() → restarts the whole combine chain after an error. */
    private val refreshKey = MutableStateFlow(0)

    private val allSongs = musicRepository.getAllSongs(SortBy.TITLE, SortOrder.ASCENDING)
    private val recentlyAdded =
        musicRepository.getAllSongs(SortBy.DATE_ADDED, SortOrder.DESCENDING)
    private val recentPlayIds = playHistoryDao.getRecentlyPlayed(HISTORY_LIMIT)
    private val favoriteIds = playlistRepository.getFavoriteSongIds()
    private val playlists = playlistRepository.getAllPlaylists()

    val uiState: StateFlow<HomeUiState> = refreshKey.flatMapLatest {
        combine(allSongs, recentlyAdded, recentPlayIds, favoriteIds, playlists) {
                songs, added, playIds, favIds, pls ->
            buildState(songs, added, playIds, favIds, pls)
        }.catch { emit(HomeUiState(isLoading = false, error = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    private fun buildState(
        songs: List<Song>,
        added: List<Song>,
        playIds: List<Long>,
        favIds: List<Long>,
        playlists: List<Playlist>
    ): HomeUiState {
        val songById = songs.associateBy { it.id }

        // Real history → songs, newest first, deduped (history repeats ids).
        val recent = playIds.distinct().mapNotNull(songById::get).take(10)

        val favSet = favIds.toHashSet()
        val favorites = songs.filter { it.id in favSet }

        val totalMs = songs.sumOf { it.duration }
        val totalMinutes = totalMs / 60000
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        val formatted = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

        return HomeUiState(
            isLoading = false,
            songs = songs,
            recentlyPlayed = recent,
            recentlyAdded = added.take(10),
            favoriteTracks = favorites,
            playlists = playlists,
            totalDurationFormatted = formatted
        )
    }

    fun retry() {
        refreshKey.value++
    }

    /** P0 FIX: Scan Music button — force a MediaStore rescan via shared cache. */
    fun scanLibrary() {
        musicRepository.refresh()
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            playlistRepository.toggleFavorite(songId)
        }
    }

    companion object {
        private const val HISTORY_LIMIT = 20
    }
}