package com.rikodev.music.android.presentation.collection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rikodev.music.android.data.repository.MusicRepository
import com.rikodev.music.android.data.repository.PlaylistRepository
import com.rikodev.music.android.data.repository.SortBy
import com.rikodev.music.android.data.repository.SortOrder
import com.rikodev.music.android.domain.model.Song
import com.rikodev.music.android.presentation.home.AlbumUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — COLLECTION DETAIL VIEWMODELS (PLAN 5)
//
// Thin VMs for the three NEW detail screens (Album/Artist/Folder — these had
// no existing screens). Data comes ONLY from the existing MusicRepository
// song list (client-side grouping, same contract as Plan 3/4 — spec §25).
// Playback stays in PlayerViewModel at the screen level.
// ─────────────────────────────────────────────────────────────────────────────

data class CollectionDetailUiState(
    val isLoading: Boolean = true,
    val error: Boolean = false,
    val notFound: Boolean = false,
    val songs: List<Song> = emptyList(),
    val identity: CollectionIdentity? = null,
    val subAlbums: List<AlbumUi> = emptyList() // artist screen only
)

internal fun durationFormatted(songs: List<Song>): String {
    val totalMinutes = songs.sumOf { it.duration } / 60000
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
}

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val albumId: Long = savedStateHandle["albumId"] ?: -1L

    val uiState: StateFlow<CollectionDetailUiState> =
        musicRepository.getAllSongs(SortBy.TITLE, SortOrder.ASCENDING).map { songs ->
            val albumSongs = songs.filter { it.albumId == albumId }
            val first = albumSongs.firstOrNull()
            CollectionDetailUiState(
                isLoading = false,
                notFound = albumSongs.isEmpty(),
                songs = albumSongs,
                identity = if (first == null) null else AlbumIdentity(
                    title = first.album,
                    metadata = buildList {
                        add(first.artist)
                        first.year.takeIf { it > 0 }?.let { add(it.toString()) }
                        add("${albumSongs.size} tracks • ${durationFormatted(albumSongs)}")
                    },
                    artworkModel = if (albumId > 0) {
                        "content://media/external/audio/albumart/$albumId"
                    } else null
                )
            )
        }.catch {
            emit(CollectionDetailUiState(isLoading = false, error = true))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CollectionDetailUiState())

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch { playlistRepository.toggleFavorite(songId) }
    }
}

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    // Synthetic artist id = artist name hash (PlaybackIntegration convention).
    private val artistId: Long = savedStateHandle["artistId"] ?: -1L

    val uiState: StateFlow<CollectionDetailUiState> =
        musicRepository.getAllSongs(SortBy.TITLE, SortOrder.ASCENDING).map { songs ->
            val artistSongs = songs.filter { it.artist.hashCode().toLong() == artistId }
            val name = artistSongs.firstOrNull()?.artist ?: ""
            val albums = artistSongs.groupBy { it.albumId }.map { (albumId, group) ->
                AlbumUi(
                    id = albumId,
                    title = group.first().album,
                    artist = name,
                    artworkModel = if (albumId > 0) {
                        "content://media/external/audio/albumart/$albumId"
                    } else null,
                    songCount = group.size,
                    songs = group
                )
            }
            CollectionDetailUiState(
                isLoading = false,
                notFound = artistSongs.isEmpty(),
                songs = artistSongs,
                subAlbums = albums,
                identity = if (artistSongs.isEmpty()) null else ArtistIdentity(
                    title = name,
                    metadata = listOf("${artistSongs.size} tracks • ${albums.size} albums")
                )
            )
        }.catch {
            emit(CollectionDetailUiState(isLoading = false, error = true))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CollectionDetailUiState())

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch { playlistRepository.toggleFavorite(songId) }
    }
}

@HiltViewModel
class FolderDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    // Folder path arrives URL-encoded from the nav route (paths contain '/').
    private val folderPath: String = savedStateHandle.get<String>("folderPath")
        ?.let { android.net.Uri.decode(it) } ?: ""

    val uiState: StateFlow<CollectionDetailUiState> =
        musicRepository.getAllSongs(SortBy.TITLE, SortOrder.ASCENDING).map { songs ->
            // PERF P2 (RC-12): string compare — was File(...).parent stat() per song
            val folderSongs = songs.filter { it.path.substringBeforeLast('/') == folderPath }
            CollectionDetailUiState(
                isLoading = false,
                notFound = folderSongs.isEmpty(),
                songs = folderSongs,
                identity = if (folderSongs.isEmpty()) null else FolderIdentity(
                    title = folderPath.substringAfterLast('/').ifEmpty { folderPath },
                    metadata = listOf("${folderSongs.size} songs • ${durationFormatted(folderSongs)}"),
                    path = folderPath
                )
            )
        }.catch {
            emit(CollectionDetailUiState(isLoading = false, error = true))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CollectionDetailUiState())

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch { playlistRepository.toggleFavorite(songId) }
    }
}