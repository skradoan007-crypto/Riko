package com.rikodev.music.android.presentation.favorites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.rikodev.music.android.core.designsystem.riko.components.RikoIconButton
import com.rikodev.music.android.core.designsystem.riko.components.RikoSortDropdownChip
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextSecondary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography
import com.rikodev.music.android.domain.model.ShuffleMode
import com.rikodev.music.android.presentation.collection.FavoritesIdentity
import com.rikodev.music.android.presentation.collection.RikoCollectionDetail
import com.rikodev.music.android.presentation.collection.components.CollectionEmptyState
import com.rikodev.music.android.presentation.collection.components.CollectionSkeleton
import com.rikodev.music.android.core.designsystem.riko.components.ARTWORK_PX_ROW
import com.rikodev.music.android.core.designsystem.riko.components.RikoTrackRow
import com.rikodev.music.android.core.designsystem.riko.components.rememberAlbumArtworkModel
import com.rikodev.music.android.presentation.nowplaying.PlayerViewModel
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// RIKO - FAVOURITES SCREEN (PLAN 5 spec 10 / PLAN 6 2-10 / PLAN 11 reference)
//
// Reference layout: compact large-title header (Search action),
// segmented [Shuffle purple | Play glass] bar, count + sort strip,
// song rows with duration + circular play button. No large hero (reference).
// Real metadata only; playback via existing PlayerViewModel actions.
// ---------------------------------------------------------------------------

@Composable
fun FavoritesScreen(
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    favoritesViewModel: FavoritesViewModel = hiltViewModel(),
    onSongClick: () -> Unit = {},
    /** Empty-state Browse Music -> existing My Music / Home route (Plan 6 8). */
    onBrowse: () -> Unit = {},
    /** Header Search icon -> existing Search tab/route (Plan 11 reference). */
    onSearchClick: () -> Unit = {},
    /** Shell-level snackbar host for the A14 remove-undo (root owns it). */
    snackbarHostState: SnackbarHostState? = null
) {
    val uiState by favoritesViewModel.uiState.collectAsStateWithLifecycle()
    // Granular flows — the old whole-PlayerState collect recomposed this
    // entire list 4x/sec on the position ticker.
    val shuffleMode by playerViewModel.shuffleMode.collectAsStateWithLifecycle()
    val currentSong by playerViewModel.currentSong.collectAsStateWithLifecycle()
    val playing by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    val undoScope = rememberCoroutineScope()

    // Sort (screen-local; repository order untouched): Date Added → Title →
    // Artist. Chip label shows the ACTIVE sort; the chevron now opens a real
    // menu (G-7: silent cycling made the control undiscoverable).
    var sortMode by rememberSaveable { mutableIntStateOf(0) }
    var sortMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val sortLabels = listOf("Date Added", "Title", "Artist")
    // Plan 1.3: sort rebuilt on EVERY recomposition (O(n log n) + lowercase
    // allocations per comparison) — now keyed remember.
    val songs = remember(uiState.songs, sortMode) {
        when (sortMode) {
            1 -> uiState.songs.sortedBy { it.title.lowercase() }
            2 -> uiState.songs.sortedBy { it.artist.lowercase() }
            else -> uiState.songs.sortedByDescending { it.dateAdded }
        }
    }

    fun shuffleAndPlay() {
        if (shuffleMode == ShuffleMode.OFF) playerViewModel.toggleShuffle()
        playerViewModel.playQueue(songs, 0)
        onSongClick()
    }

    // Real metadata only (Plan 6 3): count always; duration only when non-empty.
    // On error the count would read "0 songs" — honest label instead (G-7).
    val metadata = remember(songs, uiState.error) {
        if (uiState.error) {
            listOf("Unavailable")
        } else {
            buildList<String> {
                add(songs.size.toString() + if (songs.size == 1) " song" else " songs")
                if (songs.isNotEmpty()) {
                    val totalMinutes = songs.sumOf { it.duration } / 60000
                    val duration = if (totalMinutes >= 60) "${totalMinutes / 60}h ${totalMinutes % 60}m" else "${totalMinutes}m"
                    add(duration)
                }
            }
        }
    }

    RikoCollectionDetail(
        identity = FavoritesIdentity(
            title = "Favorites",
            metadata = metadata
        ),
        onBack = null,
        showHero = false,
        headerActions = {
            // G-7: the old heart header button had an empty onClick (dead
            // control). Title + count already identify the screen — dropped.
            RikoIconButton(
                icon = Icons.Rounded.Search,
                contentDescription = "Search",
                onClick = onSearchClick,
                container = true
            )
        },
        onPlay = {
            playerViewModel.playQueue(songs, 0)
            onSongClick()
        },
        onShuffle = if (songs.isEmpty()) null else ::shuffleAndPlay,
        // No songs → no Play/Shuffle bar. The empty state owns its own CTA
        // (Browse Music); a dead action bar above it reads as broken.
        showActions = songs.isNotEmpty(),
        modifier = modifier
    ) {
        when {
            uiState.isLoading -> item { CollectionSkeleton() }
            uiState.error -> item {
                CollectionEmptyState(
                    title = "Couldn't load your favorites",
                    message = "Something went wrong while loading. Try again in a moment.",
                    actionLabel = "Try again",
                    onAction = { favoritesViewModel.retry() }
                )
            }
            songs.isEmpty() -> item {
                CollectionEmptyState(
                    title = "No favorite songs yet",
                    message = "Save songs you love and they'll appear here.",
                    actionLabel = "Browse Music",
                    onAction = onBrowse
                )
            }
            else -> {
                // SM spec 3.4 step 5: "N favourites" + SortDropdownChip (2.14).
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RikoSpacing.XL, vertical = RikoSpacing.SM),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${songs.size} favorites",
                            style = RikoTypography.Overline,
                            color = RikoTextSecondary
                        )
                        Spacer(Modifier.weight(1f))
                        Box {
                            RikoSortDropdownChip(
                                label = sortLabels[sortMode],
                                onClick = { sortMenuExpanded = true }
                            )
                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false }
                            ) {
                                sortLabels.forEachIndexed { index, label ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = label,
                                                style = RikoTypography.Label,
                                                color = if (index == sortMode) {
                                                    RikoTextPrimary
                                                } else {
                                                    RikoTextSecondary
                                                }
                                            )
                                        },
                                        trailingIcon = {
                                            if (index == sortMode) {
                                                Icon(
                                                    Icons.Rounded.Check,
                                                    contentDescription = "Active"
                                                )
                                            }
                                        },
                                        onClick = {
                                            sortMode = index
                                            sortMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                itemsIndexed(songs, key = { _, song -> song.id }, contentType = { _, _ -> "v2_track" }) { index, song ->
                    RikoTrackRow(
                        id = song.id,
                        title = song.title,
                        subtitle = song.artist,
                        duration = song.durationFormatted,
                        artworkModel = rememberAlbumArtworkModel(song.albumId, ARTWORK_PX_ROW, songId = song.id, songUri = song.uri),
                        isPlaying = currentSong?.id == song.id && playing,
                        onClick = {
                            playerViewModel.playQueue(songs, index)
                            onSongClick()
                        },
                        // A14: unfavorite = instant removal + snackbar UNDO
                        // (reversible, so no destructive-confirm dialog per A83).
                        onMenu = {
                            favoritesViewModel.toggleFavorite(song.id)
                            snackbarHostState?.let { host ->
                                undoScope.launch {
                                    val result = host.showSnackbar(
                                        message = "Removed from Favourites",
                                        actionLabel = "UNDO",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        favoritesViewModel.toggleFavorite(song.id)
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
