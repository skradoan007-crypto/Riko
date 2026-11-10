package com.rikodev.music.android.presentation.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rikodev.music.android.core.designsystem.riko.components.ARTWORK_PX_HERO
import com.rikodev.music.android.core.designsystem.riko.components.ARTWORK_PX_SHELF
import com.rikodev.music.android.core.designsystem.riko.components.HERO_CROSSFADE_MS
import com.rikodev.music.android.core.designsystem.riko.components.RikoPlaylistCard
import com.rikodev.music.android.core.designsystem.riko.components.rememberAlbumArtworkModel
import com.rikodev.music.android.core.designsystem.riko.components.RikoSectionHeader
import com.rikodev.music.android.core.designsystem.riko.components.RikoTopBar
import com.rikodev.music.android.core.designsystem.riko.components.rikoScreenFoundation
import com.rikodev.music.android.core.designsystem.riko.shell.LocalRikoShellMetrics
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.core.designsystem.riko.theme.RikoThemeV2
import com.rikodev.music.android.domain.model.Song
import com.rikodev.music.android.presentation.home.components.HomeCategory
import com.rikodev.music.android.presentation.home.components.HomeCategoryChips
import com.rikodev.music.android.presentation.home.components.HomeEmpty
import com.rikodev.music.android.presentation.home.components.HomeError
import com.rikodev.music.android.presentation.home.components.HomeFeaturedBanner
import com.rikodev.music.android.presentation.home.components.HomeSkeleton

import com.rikodev.music.android.presentation.nowplaying.PlayerViewModel
import java.time.LocalTime

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel(),
    onSongClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onOpenLibrary: () -> Unit = {}
) {
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    // Granular: the old whole-PlayerState collect recomposed this entire
    // screen 4x/sec on the position ticker. Only the current song is needed.
    val currentSong by playerViewModel.currentSong.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    var selectedCategory by rememberSaveable { mutableStateOf(HomeCategory.ALL) }

    // Live title (G-7: the shipped "Prince Lovers" placeholder is gone).
    // Time-based greeting — sticky per composition, same spirit as Plan 3's header.
    val greeting = remember {
        when (LocalTime.now().hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Good night"
        }
    }

    val shellMetrics = LocalRikoShellMetrics.current
    val density = LocalDensity.current
    val statusBarTop = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
    val navigationBarsBottom = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }

    // P0 FIX: callback previously empty â€” when permission was already granted
    // (2nd+ launch) launcher returned instantly with NO dialog and NO refresh,
    // so Scan Music was a silent no-op. Always rescan after the flow.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { homeViewModel.scanLibrary() }

    // Read at composable level (LocalContext.current is @Composable â€” cannot
    // live inside the plain fun requestMusicAccess below).
    val screenContext = androidx.compose.ui.platform.LocalContext.current

    fun requestMusicAccess() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val alreadyGranted = permissions.all {
            androidx.core.content.ContextCompat.checkSelfPermission(
                screenContext, it
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (alreadyGranted) {
            // P0 FIX: 2nd+ launch â€” permission held, skip dialog, rescan directly.
            homeViewModel.scanLibrary()
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    val heroData: HomeHeroData? = when {
        currentSong != null -> {
            val song = currentSong!!
            HomeHeroData(
                sectionLabel = "Continue Listening",
                title = song.title,
                artist = song.artist,
                artworkModel = rememberAlbumArtworkModel(song.albumId, ARTWORK_PX_HERO, HERO_CROSSFADE_MS, songId = song.id, songUri = song.uri),
                progress = null
            )
        }
        uiState.recentlyPlayed.isNotEmpty() -> {
            val song = uiState.recentlyPlayed.first()
            HomeHeroData(
                sectionLabel = "Recently Played",
                title = song.title,
                artist = song.artist,
                artworkModel = rememberAlbumArtworkModel(song.albumId, ARTWORK_PX_HERO, HERO_CROSSFADE_MS, songId = song.id, songUri = song.uri),
                progress = null
            )
        }
        else -> null
    }

    // Category chips (Plan 11) switch the Top Picks shelf source — every
    // option reads real state, never synthetic rows (§127).
    val topPicks: List<Song> = when (selectedCategory) {
        HomeCategory.ALL -> uiState.songs
        HomeCategory.ALBUMS -> uiState.songs.distinctBy { it.albumId }
        HomeCategory.NEW_ARTISTS -> uiState.recentlyAdded.distinctBy { it.artist }
        HomeCategory.NEW_RELEASES -> uiState.recentlyAdded
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .rikoScreenFoundation(depth = 0.72f)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = statusBarTop + RikoSpacing.XL,
                bottom = shellMetrics.contentBottomPadding + navigationBarsBottom + RikoSpacing.XL
            )
        ) {
            // Persistent top bar — stays visible in loading/error/empty too
            // (G-7: it used to vanish with the content branch).
            item {
                RikoTopBar(
                    title = greeting,
                    actions = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = RikoThemeV2.Colors.onBackground,
                            modifier = Modifier.size(48.dp).clickable(role = Role.Button, onClick = onSearchClick).padding(12.dp)
                        )
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = RikoThemeV2.Colors.onBackground,
                            modifier = Modifier.size(48.dp).clickable(role = Role.Button, onClick = onSettingsClick).padding(12.dp)
                        )
                    }
                )
            }
            when {
                uiState.isLoading -> item { HomeSkeleton() }
                uiState.error -> item { HomeError(onRetry = homeViewModel::retry) }
                uiState.songs.isEmpty() -> item { HomeEmpty(onScanRequest = ::requestMusicAccess) }
                else -> {
                    item {
                        HomeCategoryChips(
                            selected = selectedCategory,
                            onSelect = { selectedCategory = it },
                            modifier = Modifier.padding(top = RikoSpacing.MD)
                        )
                    }
                    // Featured banner only when there is something real to
                    // feature (no fake "Feel the Vibe" filler, G-7).
                    if (heroData != null) {
                        item {
                            Box(Modifier.padding(top = RikoSpacing.LG, start = RikoSpacing.XL, end = RikoSpacing.XL)) {
                                HomeFeaturedBanner(
                                    sectionLabel = heroData.sectionLabel,
                                    title = heroData.title,
                                    subtitle = heroData.artist,
                                    artworkModel = heroData.artworkModel,
                                    onPlay = {
                                        if (currentSong != null) {
                                            playerViewModel.togglePlayPause()
                                        } else {
                                            playerViewModel.playQueue(uiState.recentlyPlayed, 0)
                                            onSongClick()
                                        }
                                    }
                                )
                            }
                        }
                    }
                    item {
                        Box(Modifier.padding(top = RikoSpacing.XL)) {
                            HomeScreenSections(
                                recentSongs = uiState.recentlyPlayed.take(10),
                                topSongs = topPicks.take(10),
                                onRecentPlay = { song ->
                                    val index = uiState.recentlyPlayed.indexOfFirst { it.id == song.id }
                                    if (index >= 0) {
                                        playerViewModel.playQueue(uiState.recentlyPlayed, index)
                                        onSongClick()
                                    }
                                },
                                onTopPlay = { song ->
                                    val index = topPicks.indexOfFirst { it.id == song.id }
                                    if (index >= 0) {
                                        playerViewModel.playQueue(topPicks, index)
                                        onSongClick()
                                    }
                                },
                                onSeeAllTop = onOpenLibrary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreenSections(
    recentSongs: List<Song>,
    topSongs: List<Song>,
    onRecentPlay: (Song) -> Unit = {},
    onTopPlay: (Song) -> Unit = {},
    onSeeAllTop: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // An empty "Recently Played" header with nothing under it reads as
        // broken. The section only exists when there is history to show.
        // No history screen exists yet, so no See-all control here (G-7).
        if (recentSongs.isNotEmpty()) {
            RikoSectionHeader(
                title = "Recently Played",
                modifier = Modifier.padding(horizontal = RikoSpacing.XL)
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(top = RikoSpacing.MD),
                horizontalArrangement = Arrangement.spacedBy(RikoSpacing.MD),
                contentPadding = PaddingValues(horizontal = RikoSpacing.XL)
            ) {
                items(recentSongs, key = { it.id }, contentType = { "v2_card" }) { song ->
                    RikoPlaylistCard(
                        title = song.title,
                        subtitle = song.artist,
                        artworkModel = rememberAlbumArtworkModel(song.albumId, ARTWORK_PX_SHELF, songId = song.id, songUri = song.uri),
                        onClick = { onRecentPlay(song) },
                        onPlay = { onRecentPlay(song) },
                        modifier = Modifier.width(160.dp)
                    )
                }
            }
        }
        RikoSectionHeader(
            title = "Top Picks for You",
            actionText = "See All",
            onAction = onSeeAllTop,
            modifier = Modifier.padding(horizontal = RikoSpacing.XL, vertical = RikoSpacing.MD)
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RikoSpacing.MD),
            contentPadding = PaddingValues(horizontal = RikoSpacing.XL)
        ) {
            items(topSongs, key = { it.id }, contentType = { "v2_card" }) { song ->
                RikoPlaylistCard(
                    title = song.title,
                    subtitle = song.artist,
                    artworkModel = rememberAlbumArtworkModel(song.albumId, ARTWORK_PX_SHELF, songId = song.id, songUri = song.uri),
                    onClick = { onTopPlay(song) },
                    onPlay = { onTopPlay(song) },
                    modifier = Modifier.width(160.dp)
                )
            }
        }
    }
}

