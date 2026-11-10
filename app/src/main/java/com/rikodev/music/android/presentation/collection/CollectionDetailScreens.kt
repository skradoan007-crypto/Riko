package com.rikodev.music.android.presentation.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rikodev.music.android.core.designsystem.riko.components.RikoPlaylistCard
import com.rikodev.music.android.core.designsystem.riko.components.RikoSectionHeader
import com.rikodev.music.android.core.designsystem.riko.components.RikoTopBar
import com.rikodev.music.android.core.designsystem.riko.components.RikoTopBarStyle
import com.rikodev.music.android.core.designsystem.riko.components.rikoScreenFoundation
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.core.designsystem.riko.theme.RikoThemeV2
import com.rikodev.music.android.presentation.collection.components.CollectionEmptyState
import com.rikodev.music.android.presentation.collection.components.CollectionSkeleton
import com.rikodev.music.android.presentation.nowplaying.PlayerViewModel
import com.rikodev.music.android.presentation.playlist.PlaylistDetailContent

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — COLLECTION DETAIL SCREENS (V2)
//
// Thin entity-specific entries over the shared PlaylistDetailContent.
// Each supplies title/data/actions; the content owns structure.
// Playback = existing PlayerViewModel actions ONLY (playQueue).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AlbumDetailScreen(
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    viewModel: AlbumDetailViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onSongClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // PERF FORENSIC P0 (RC-01): granular — track lists must NOT recompose on 4Hz position ticks
    val currentSong by playerViewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    val songs = uiState.songs
    val title = uiState.identity?.title ?: "Album"

    fun playAll() {
        playerViewModel.playQueue(songs, 0)
        onSongClick()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .rikoScreenFoundation(depth = 0.82f)
    ) {
        RikoTopBar(title = title, style = RikoTopBarStyle.Back, onBack = onBack)
        when {
            uiState.isLoading -> CollectionSkeleton()
            uiState.error -> CollectionEmptyState(
                title = "Couldn't load this collection",
                message = "Something went wrong. Please go back and try again."
            )
            uiState.notFound || songs.isEmpty() -> CollectionEmptyState(
                title = "No tracks in this album",
                message = "This album has no songs on this device."
            )
            else -> PlaylistDetailContent(
                title = title,
                songs = songs,
                onPlayAll = ::playAll,
                onPlay = { index ->
                    playerViewModel.playQueue(songs, index)
                    onSongClick()
                },
                onMenu = { song -> viewModel.toggleFavorite(song.id) },
                playingSongId = currentSong?.id,
                isPlayingNow = isPlaying,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ArtistDetailScreen(
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    viewModel: ArtistDetailViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onSongClick: () -> Unit = {},
    onAlbumClick: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // PERF FORENSIC P0 (RC-01): granular — track lists must NOT recompose on 4Hz position ticks
    val currentSong by playerViewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    val songs = uiState.songs
    val title = uiState.identity?.title ?: "Artist"

    fun playAll() {
        playerViewModel.playQueue(songs, 0)
        onSongClick()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .rikoScreenFoundation(depth = 0.82f)
    ) {
        RikoTopBar(title = title, style = RikoTopBarStyle.Back, onBack = onBack)
        when {
            uiState.isLoading -> CollectionSkeleton()
            uiState.error -> CollectionEmptyState(
                title = "Couldn't load this collection",
                message = "Something went wrong. Please go back and try again."
            )
            uiState.notFound || songs.isEmpty() -> CollectionEmptyState(
                title = "No music from this artist",
                message = "This artist has no songs on this device."
            )
            else -> {
                // Albums shelf — only when data supports it.
                if (uiState.subAlbums.isNotEmpty()) {
                    // G-7: the old See-all here was `onSeeAll = {}` (dead) —
                    // no album-index screen exists to target. Title only.
                    RikoSectionHeader(
                        title = "Albums",
                        modifier = Modifier.padding(horizontal = RikoSpacing.LG)
                    )
                    Box(Modifier.padding(top = RikoSpacing.MD, bottom = RikoSpacing.MD)) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = RikoSpacing.LG),
                            horizontalArrangement = Arrangement.spacedBy(RikoSpacing.MD)
                        ) {
                            items(
                                uiState.subAlbums,
                                key = { album -> "artist_album_${album.id}" },
                                contentType = { "v2_card" }
                            ) { album ->
                                RikoPlaylistCard(
                                    title = album.title,
                                    subtitle = "${album.songCount} songs",
                                    artworkModel = album.artworkModel,
                                    onClick = { onAlbumClick(album.id) },
                                    onPlay = {
                                        if (album.songs.isNotEmpty()) {
                                            playerViewModel.playQueue(album.songs, 0)
                                            onSongClick()
                                        }
                                    },
                                    modifier = Modifier.width(140.dp)
                                )
                            }
                        }
                    }
                }
                PlaylistDetailContent(
                    title = title,
                    songs = songs,
                    onPlayAll = ::playAll,
                    onPlay = { index ->
                        playerViewModel.playQueue(songs, index)
                        onSongClick()
                    },
                    onMenu = { song -> viewModel.toggleFavorite(song.id) },
                    playingSongId = currentSong?.id,
                    isPlayingNow = isPlaying,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun FolderDetailScreen(
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    viewModel: FolderDetailViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onSongClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // PERF FORENSIC P0 (RC-01): granular — track lists must NOT recompose on 4Hz position ticks
    val currentSong by playerViewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    val songs = uiState.songs
    val title = uiState.identity?.title ?: "Folder"

    fun playAll() {
        playerViewModel.playQueue(songs, 0)
        onSongClick()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .rikoScreenFoundation(depth = 0.82f)
    ) {
        RikoTopBar(title = title, style = RikoTopBarStyle.Back, onBack = onBack)
        when {
            uiState.isLoading -> CollectionSkeleton()
            uiState.error -> CollectionEmptyState(
                title = "Couldn't load this collection",
                message = "Something went wrong. Please go back and try again."
            )
            uiState.notFound || songs.isEmpty() -> CollectionEmptyState(
                title = "No music found",
                message = "This folder has no songs on this device."
            )
            else -> PlaylistDetailContent(
                title = title,
                songs = songs,
                onPlayAll = ::playAll,
                onPlay = { index ->
                    playerViewModel.playQueue(songs, index)
                    onSongClick()
                },
                onMenu = { song -> viewModel.toggleFavorite(song.id) },
                playingSongId = currentSong?.id,
                isPlayingNow = isPlaying,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
