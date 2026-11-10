package com.rikodev.music.android.presentation.playlist

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rikodev.music.android.data.repository.MusicRepository
import com.rikodev.music.android.data.repository.PlaylistRepository
import com.rikodev.music.android.domain.model.Playlist
import com.rikodev.music.android.domain.model.Song
import com.rikodev.music.android.presentation.navigation.RikoDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.hilt.navigation.compose.hiltViewModel

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// RIKO MUSIC â€” PLAYLISTS VIEWMODEL (list of all playlists)
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

data class PlaylistsUiState(
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = true,
    /** G-7: the index screen had no error state at all. */
    val error: Boolean = false
)

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    /** Bumping this key re-subscribes the upstream flow (error retry). */
    private val refreshKey = MutableStateFlow(0)

    val uiState: StateFlow<PlaylistsUiState> = refreshKey.flatMapLatest {
        playlistRepository.getAllPlaylists()
            .map { list -> PlaylistsUiState(playlists = list, isLoading = false) }
            .catch { emit(PlaylistsUiState(isLoading = false, error = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlaylistsUiState())

    fun retry() {
        refreshKey.value += 1
    }

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    fun openCreateDialog() { _showCreateDialog.value = true }
    fun dismissCreateDialog() { _showCreateDialog.value = false }

    fun createPlaylist(name: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = playlistRepository.createPlaylist(name)
            _showCreateDialog.value = false
            onCreated(id)
        }
    }

    /** Delete a non-favorites playlist - reuses existing repository method. */
    fun deletePlaylist(id: Long) {
        viewModelScope.launch { playlistRepository.deletePlaylist(id) }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// RIKO MUSIC â€” PLAYLIST DETAIL VIEWMODEL
// Handles a single playlist: viewing songs, editing, reordering, cover art.
// Same ViewModel powers both "view" mode and "edit" mode for simplicity.
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

data class PlaylistDetailUiState(
    val playlist: Playlist? = null,
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
    val isEditMode: Boolean = false
)

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val musicRepository: MusicRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val playlistId: Long = checkNotNull(
        savedStateHandle.get<String>(RikoDestination.PlaylistDetail.ARG_PLAYLIST_ID)
    ).toLong()

    private val _isEditMode = MutableStateFlow(false)

    // â”€â”€ Add-songs picker â€” RUNTIME FIX 8 â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // addSongs() below already called playlistRepository.addSongsToPlaylist()
    // correctly; the only thing missing was a UI entry point. See docs.
    private val _showAddSongsSheet = MutableStateFlow(false)
    val showAddSongsSheet: StateFlow<Boolean> = _showAddSongsSheet.asStateFlow()

    private val _selectedSongIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedSongIds: StateFlow<Set<Long>> = _selectedSongIds.asStateFlow()

    fun openAddSongsSheet() {
        _selectedSongIds.value = emptySet()
        _showAddSongsSheet.value = true
    }

    fun dismissAddSongsSheet() {
        _showAddSongsSheet.value = false
        _selectedSongIds.value = emptySet()
    }

    fun toggleSongSelection(songId: Long) {
        _selectedSongIds.value = _selectedSongIds.value.let { current ->
            if (songId in current) current - songId else current + songId
        }
    }

    fun confirmAddSelectedSongs() {
        val ids = _selectedSongIds.value.toList()
        if (ids.isNotEmpty()) addSongs(ids)
        dismissAddSongsSheet()
    }

    val uiState: StateFlow<PlaylistDetailUiState> = kotlinx.coroutines.flow.combine(
        playlistRepository.getPlaylistWithSongs(playlistId),
        _isEditMode
    ) { (playlist, songs), editMode ->
        PlaylistDetailUiState(
            playlist = playlist,
            songs = songs,
            isLoading = false,
            isEditMode = editMode
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlaylistDetailUiState())

    /** Full library minus whatever's already in this playlist â€” no point offering duplicates. */
    val pickableSongs: StateFlow<List<Song>> = kotlinx.coroutines.flow.combine(
        musicRepository.getAllSongs(),
        uiState
    ) { allSongs, state ->
        val alreadyInPlaylist = state.songs.map { it.id }.toSet()
        allSongs.filter { it.id !in alreadyInPlaylist }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleEditMode() { _isEditMode.value = !_isEditMode.value }

    fun renamePlaylist(newName: String) {
        viewModelScope.launch { playlistRepository.renamePlaylist(playlistId, newName) }
    }

    fun setCoverImage(uri: Uri?) {
        viewModelScope.launch { playlistRepository.setPlaylistCover(playlistId, uri) }
    }

    fun removeSong(songId: Long) {
        viewModelScope.launch { playlistRepository.removeSongFromPlaylist(playlistId, songId) }
    }

    fun addSongs(songIds: List<Long>) {
        viewModelScope.launch { playlistRepository.addSongsToPlaylist(playlistId, songIds) }
    }

    fun reorderSongs(orderedSongIds: List<Long>) {
        viewModelScope.launch { playlistRepository.reorderPlaylistSongs(playlistId, orderedSongIds) }
    }

    // â”€â”€ Reorder (Plan 6 Â§22â€“23) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // Move one track up/down within the playlist. Reuses the existing persisted
    // reorder action (reorderPlaylistSongs) â€” no new persistence, no auto-sort.
    fun moveSongUp(index: Int) {
        val songs = uiState.value.songs
        if (index <= 0 || index >= songs.size) return
        moveSongTo(index, index - 1)
    }

    fun moveSongDown(index: Int) {
        val songs = uiState.value.songs
        if (index < 0 || index >= songs.size - 1) return
        moveSongTo(index, index + 1)
    }

    private fun moveSongTo(from: Int, to: Int) {
        val current = uiState.value.songs
        val ids = current.map { it.id }.toMutableList()
        val id = ids.removeAt(from)
        ids.add(to.coerceAtMost(ids.size), id)
        reorderSongs(ids)
    }

    fun deletePlaylist(onDeleted: () -> Unit) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
            onDeleted()
        }
    }

    val isFavoritesPlaylist: Boolean
        get() = uiState.value.playlist?.isFavorites == true
}
