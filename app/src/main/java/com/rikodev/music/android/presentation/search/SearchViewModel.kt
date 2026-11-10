package com.rikodev.music.android.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rikodev.music.android.data.repository.MusicRepository
import com.rikodev.music.android.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import androidx.hilt.navigation.compose.hiltViewModel

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — SEARCH VIEWMODEL
// Debounced live search across title/artist/album via MediaStore LIKE query.
// Albums/Artists sections are derived views of the same matched set (their
// taps navigate to the shared AlbumDetail/ArtistDetail routes).
// ─────────────────────────────────────────────────────────────────────────────

/** Album section row — id is the MediaStore albumId (albumDetail arg). */
data class SearchAlbumUi(
    val albumId: Long,
    val title: String,
    val artist: String,
    val songCount: Int
)

/** Artist section row — id matches ArtistDetail's `artist.hashCode().toLong()`. */
data class SearchArtistUi(
    val artistId: Long,
    val name: String,
    val songCount: Int
)

data class SearchResultUiState(
    val songs: List<Song> = emptyList(),
    val albums: List<SearchAlbumUi> = emptyList(),
    val artists: List<SearchArtistUi> = emptyList(),
    /** G-7: search had no error state (and a throw used to strand isSearching). */
    val error: Boolean = false
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    /** Bumping this key re-runs the current query (error retry). */
    private val refreshKey = MutableStateFlow(0)

    val searchState: StateFlow<SearchResultUiState> = refreshKey
        // Fresh inner subscription per retry: debounce/dedup keep their own
        // history only within one attempt, so the same query re-searches.
        .flatMapLatest { _query.debounce(280).distinctUntilChanged() }
        .flatMapLatest { q ->
            if (q.isBlank()) {
                flowOf(SearchResultUiState())
            } else {
                flow {
                    _isSearching.value = true
                    try {
                        val songs = musicRepository.searchSongs(q)
                        _isSearching.value = false
                        emit(
                            SearchResultUiState(
                                songs = songs,
                                albums = albumsOf(songs),
                                artists = artistsOf(songs)
                            )
                        )
                    } catch (ce: CancellationException) {
                        throw ce
                    } catch (_: Exception) {
                        _isSearching.value = false
                        emit(SearchResultUiState(error = true))
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchResultUiState())

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun clearQuery() {
        _query.value = ""
    }

    fun retry() {
        refreshKey.value += 1
    }

    /** Distinct albums of the matched set, albumId > 0 only (0 = no artwork). */
    private fun albumsOf(songs: List<Song>): List<SearchAlbumUi> =
        songs.filter { it.albumId > 0 }
            .groupBy { it.albumId }
            .map { (albumId, group) ->
                SearchAlbumUi(
                    albumId = albumId,
                    title = group.first().album,
                    artist = group.first().artist,
                    songCount = group.size
                )
            }
            .sortedBy { it.title.lowercase() }

    /** Distinct artists — id formula mirrors ArtistDetailViewModel (L96). */
    private fun artistsOf(songs: List<Song>): List<SearchArtistUi> =
        songs.groupBy { it.artist }
            .map { (name, group) ->
                SearchArtistUi(
                    artistId = name.hashCode().toLong(),
                    name = name,
                    songCount = group.size
                )
            }
            .sortedBy { it.name.lowercase() }
}
