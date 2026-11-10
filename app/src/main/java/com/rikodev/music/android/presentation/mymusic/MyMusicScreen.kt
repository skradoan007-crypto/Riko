package com.rikodev.music.android.presentation.mymusic
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.platform.LocalDensity

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.ui.semantics.Role
import com.rikodev.music.android.core.designsystem.riko.components.RikoTopBar
import com.rikodev.music.android.core.designsystem.riko.components.RikoTopBarStyle
import com.rikodev.music.android.core.designsystem.riko.theme.RikoThemeV2
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rikodev.music.android.core.designsystem.riko.shell.LocalRikoShellMetrics
import com.rikodev.music.android.core.designsystem.riko.theme.RikoCornerRadius
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlass
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSurface
import com.rikodev.music.android.presentation.home.components.HomeEmpty
import com.rikodev.music.android.presentation.collection.components.CollectionEmptyState
import com.rikodev.music.android.presentation.home.components.HomeError
import com.rikodev.music.android.presentation.mymusic.components.FolderLibraryContent
import com.rikodev.music.android.core.designsystem.riko.components.RikoFilterTabPill
import com.rikodev.music.android.presentation.mymusic.components.LibraryModeSelector
import com.rikodev.music.android.presentation.mymusic.components.LibrarySortSheet
import com.rikodev.music.android.presentation.mymusic.components.LibraryToolbar
import com.rikodev.music.android.presentation.mymusic.components.MyMusicHeader
import com.rikodev.music.android.presentation.mymusic.components.SongLibraryContent
import com.rikodev.music.android.presentation.nowplaying.PlayerViewModel

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// RIKO â€” MY MUSIC SCREEN (PLAN 4)
//
// The Library browsing surface: Header â†’ Mode Selector â†’ Toolbar â†’ Content.
//
// PRESERVED: playback actions (playQueue/toggleFavorite via PlayerViewModel +
// HomeViewModel-pattern repos), permission/scanner contract, Plan 2 shell.
// 4 modes per spec 3.6 / D4: Suggested / Folders / Downloads / New -- Downloads has
// no real data source (spec 1). Sort = real MediaStore fields only (spec 9).
// Modes: content switcher â€” NOT a navigation replacement (spec Â§4).
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
fun MyMusicScreen(
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    myMusicViewModel: MyMusicViewModel = hiltViewModel(),
    onSongClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    /** Plan 5: folder taps -> detail routes (spec 35). */
    onFolderClick: (String) -> Unit = {},
    /** SM spec 3.6 â€” AppBar B actions: back + heart (Favourites tab). */
    onBackClick: () -> Unit = {},
    onFavoritesClick: () -> Unit = {},
    /** SM spec D12 â€” Playlists sub-destination entry. */
    onPlaylistsClick: () -> Unit = {}
) {
    val uiState by myMusicViewModel.uiState.collectAsStateWithLifecycle()
    // Granular: the old whole-PlayerState collect recomposed this list 4x/sec.
    val currentSong by playerViewModel.currentSong.collectAsStateWithLifecycle()
    val playing by playerViewModel.isPlaying.collectAsStateWithLifecycle()

    // Mode = single source of truth (spec Â§5); survives process restore.
    // spec 3.6: "Suggested" is the default active tab (D4 confirmed).
    var mode by rememberSaveable { mutableStateOf(LibraryMode.Suggested) }
    var showSortSheet by rememberSaveable { mutableStateOf(false) }

    // Per-mode scroll states, hoisted + saveable (spec Â§30) â€” mode round-trips
    // keep position; states are NEVER shared across modes.
    val songsListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val foldersListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    // "New" tab (spec D4) = newest additions first -- own list + own scroll
    // state so it never shares position with Suggested (spec 30).
    val newSongs = remember(uiState.songs) { uiState.songs.sortedByDescending { it.dateAdded } }
    val newListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    val shellMetrics = LocalRikoShellMetrics.current
    val density = LocalDensity.current
    val statusBarTop = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
    val navigationBarsBottom = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
    val contentPadding = PaddingValues(
        bottom = shellMetrics.contentBottomPadding + navigationBarsBottom + RikoSpacing.XL
    )

    // Same permission contract as MainActivity/Plan 3 (spec Â§40) â€” reused, not replaced.
    // P0 FIX: callback previously did nothing â€” when permission was already
    // granted (2nd+ launch) the launcher returned instantly with NO dialog and
    // NO refresh, so Scan Music was a silent no-op. Always rescan after the flow.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { myMusicViewModel.scanLibrary() }

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
            myMusicViewModel.scanLibrary()
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // SM spec 3.6 â€” AppBar B: back + centered "My Music" + heart (2.3).
        // Back pops to the previous tab origin (Home by default).
        RikoTopBar(
            title = "My Music",
            style = RikoTopBarStyle.Centered,
            onBack = { onBackClick() },
            actions = {
                Icon(
                    Icons.Filled.QueueMusic,
                    contentDescription = "Playlists",
                    tint = RikoThemeV2.Colors.onBackground,
                    modifier = Modifier.size(48.dp).clickable(role = Role.Button, onClick = onPlaylistsClick).padding(12.dp)
                )
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = "Favorites",
                    tint = RikoThemeV2.Colors.onBackground,
                    modifier = Modifier.size(48.dp).clickable(role = Role.Button, onClick = onFavoritesClick).padding(12.dp)
                )
            }
        )
        Box(Modifier.padding(top = RikoSpacing.LG)) {
            LibraryModeSelector(selectedMode = mode, onModeSelected = { mode = it })
        }
        // D12: Playlists is reached via onPlaylistsClick from header actions.
        if (mode != LibraryMode.Downloads && !uiState.isLoading && !uiState.error && uiState.songs.isNotEmpty()) {
            val (count, label) = when (mode) {
                LibraryMode.Suggested -> uiState.songs.size to "Songs"
                LibraryMode.Folders -> uiState.folders.size to "Folders"
                LibraryMode.Downloads -> 0 to "Downloads"
                LibraryMode.New -> uiState.songs.size to "New"
            }
            LibraryToolbar(
                itemCount = count,
                itemLabel = label,
                sortBy = uiState.sortBy,
                onSortClick = { showSortSheet = true }
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.isLoading -> LibrarySkeleton()
                uiState.error -> HomeError(onRetry = myMusicViewModel::retry)
                uiState.songs.isEmpty() -> HomeEmpty(onScanRequest = ::requestMusicAccess)
                else -> when (mode) {
                    LibraryMode.Suggested -> SongLibraryContent(
                        songs = uiState.songs,
                        currentSongId = currentSong?.id,
                        isPlaying = playing,
                        listState = songsListState,
                        contentPadding = contentPadding,
                        onSongClick = { index ->
                            playerViewModel.playQueue(uiState.songs, index)
                            onSongClick()
                        },
                        onFavorite = myMusicViewModel::toggleFavorite
                    )
                    LibraryMode.Folders -> FolderLibraryContent(
                        folders = uiState.folders,
                        listState = foldersListState,
                        contentPadding = contentPadding,
                        onFolderClick = { folder -> onFolderClick(folder.id) }
                    )
                    LibraryMode.Downloads ->
                        // spec 3.6/1: tab exists, no data source -- typed empty, no scan CTA.
                        CollectionEmptyState(
                            title = "Downloads",
                            message = "Downloaded tracks will appear here."
                        )
                    LibraryMode.New -> SongLibraryContent(
                        songs = newSongs,
                        currentSongId = currentSong?.id,
                        isPlaying = playing,
                        listState = newListState,
                        contentPadding = contentPadding,
                        onSongClick = { index ->
                            playerViewModel.playQueue(newSongs, index)
                            onSongClick()
                        },
                        onFavorite = myMusicViewModel::toggleFavorite
                    )
                }
            }
        }
    }

    if (showSortSheet) {
        LibrarySortSheet(
            currentSort = uiState.sortBy,
            currentOrder = uiState.sortOrder,
            onSortSelected = { sort ->
                myMusicViewModel.setSortBy(sort)
                showSortSheet = false
            },
            onOrderSelected = { order ->
                if (order != uiState.sortOrder) myMusicViewModel.toggleSortOrder()
                showSortSheet = false
            },
            onDismiss = { showSortSheet = false }
        )
    }
}

/** Library loading: 5 skeleton rows â€” dark shimmer, no giant spinner (spec Â§14). */
@Composable
private fun LibrarySkeleton() {
    val transition = rememberInfiniteTransition(label = "librarySkeleton")
    val shimmer by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "libraryShimmer"
    )
    // Dark shimmer (see HomeSkeleton): low white alphas, never gray blocks.
    val base = RikoSurface.copy(alpha = 0.10f + 0.06f * shimmer)
    val soft = RikoGlass.copy(alpha = 0.05f + 0.03f * shimmer)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = RikoSpacing.XL, vertical = RikoSpacing.MD)
    ) {
        repeat(5) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = RikoSpacing.SM)
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(RikoCornerRadius.SM))
                        .background(soft)
                )
                Spacer(Modifier.padding(RikoSpacing.SM))
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        Modifier
                            .height(14.dp)
                            .fillMaxSize(0.6f)
                            .clip(RoundedCornerShape(50))
                            .background(base)
                    )
                    Spacer(Modifier.height(RikoSpacing.XS))
                    Box(
                        Modifier
                            .height(11.dp)
                            .fillMaxSize(0.35f)
                            .clip(RoundedCornerShape(50))
                            .background(soft)
                    )
                }
            }
        }
    }
}
