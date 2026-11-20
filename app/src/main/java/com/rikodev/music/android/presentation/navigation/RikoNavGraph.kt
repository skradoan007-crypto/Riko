package com.rikodev.music.android.presentation.navigation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.Icons

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.activity.compose.BackHandler
import com.rikodev.music.android.core.designsystem.theme.LocalDynamicColors
import com.rikodev.music.android.core.designsystem.theme.RikoShapes
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMotion
import com.rikodev.music.android.core.designsystem.riko.theme.RikoComponentSize
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.core.designsystem.theme.TextPrimary
import com.rikodev.music.android.core.designsystem.animation.ProvideRikoLiveClock
import com.rikodev.music.android.core.designsystem.animation.rememberReduceMotion
import com.rikodev.music.android.presentation.continuum.ContinuumLibraryHubScreen
import com.rikodev.music.android.presentation.continuum.ContinuumSection
import com.rikodev.music.android.presentation.continuum.ContinuumViewModel
import com.rikodev.music.android.presentation.continuum.ContinuumWaveBarHeight
import com.rikodev.music.android.presentation.continuum.ContinuumWaveNavBar
import com.rikodev.music.android.presentation.continuum.DeviceRenderCapabilities
import com.rikodev.music.android.presentation.continuum.LiquidNexusEffect
import com.rikodev.music.android.presentation.continuum.LiquidNexusEvent
import com.rikodev.music.android.presentation.continuum.LiquidNexusMode
import com.rikodev.music.android.presentation.continuum.LiquidNexusOverlay
import com.rikodev.music.android.presentation.continuum.LiquidGlassStyle
import com.rikodev.music.android.presentation.continuum.LiquidNexusShaders
import com.rikodev.music.android.presentation.continuum.VisualQualityResolver
import com.rikodev.music.android.presentation.home.HomeScreen
import com.rikodev.music.android.presentation.mymusic.MyMusicScreen
import com.rikodev.music.android.presentation.onboarding.OnboardingScreen
import com.rikodev.music.android.presentation.collection.AlbumDetailScreen
import com.rikodev.music.android.presentation.collection.ArtistDetailScreen
import com.rikodev.music.android.presentation.collection.FolderDetailScreen
import com.rikodev.music.android.presentation.components.GlassBottomSheetContainer
import com.rikodev.music.android.presentation.nowplaying.PlayerViewModel
import com.rikodev.music.android.presentation.nowplaying.EqualizerSheet
import com.rikodev.music.android.presentation.nowplaying.QueueSheet
import com.rikodev.music.android.presentation.nowplaying.SongInfoSheet
import com.rikodev.music.android.presentation.nowplaying.SleepTimerSheet
import com.rikodev.music.android.presentation.playlist.*
import com.rikodev.music.android.presentation.search.SearchScreen
import com.rikodev.music.android.presentation.settings_v2.host.SettingsScreenWiring
import com.rikodev.music.android.core.designsystem.riko.components.RikoBottomNavigation
import com.rikodev.music.android.core.designsystem.riko.components.RikoNavItem
import com.rikodev.music.android.core.designsystem.riko.shell.RikoAmbientColors
import com.rikodev.music.android.core.designsystem.riko.shell.RikoAppShell
import com.rikodev.music.android.presentation.sheet.PlayerSheetHost
import com.rikodev.music.android.presentation.sheet.SheetAnchor
import com.rikodev.music.android.presentation.sheet.rememberPlayerSheetState
import com.rikodev.music.android.presentation.visualos.host.VisualOsBackPressChain
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.rikodev.music.android.presentation.visualos.host.VisualOsNavBridge
import com.rikodev.music.android.presentation.visualos.host.VisualOsCoordinator
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.Pause

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// RIKO EXPERIENCE OS â€” NAVIGATION GRAPH
//
// Root navigation entry point â€” à¦à¦•à¦• integration point for Visual OSà¥¤
//
// Visual OS Integration (v2.0):
//   - VisualOsCoordinator à¦¥à¦¾à¦•à¦²à§‡: Nav â†’ Visual OS bridge à¦¸à¦•à§à¦°à¦¿à¦¯à¦¼
//   - VisualOsCoordinator à¦¨à¦¾ à¦¥à¦¾à¦•à¦²à§‡: legacy mode â€” existing behavior
//
// Architecture: Constitution â†’ "Navigation = choreography, not replacement"
// Rule #83: Coordinator Executes â€” back press chain handled here
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// SM SPEC SECTION 4 — SHELL NAVIGATION ITEMS
// 4 tabs per spec: Home Â· Favourites Â· Library (= My Music root, D12) Â·
// Settings. Playlists + Now Playing + Search are pushed sub-destinations
// (no tab). The Favourites tab uses the FILLED heart (spec 1.4 fill rule).
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
private val shellNavItems = listOf(
    RikoNavItem(Icons.Rounded.Home, "Home"),
    RikoNavItem(Icons.Rounded.Favorite, "Favorites"),
    RikoNavItem(Icons.Rounded.MusicNote, "Library"),
    RikoNavItem(Icons.Rounded.Settings, "Settings")
)

private fun shellNavIndex(route: String?): Int = when (route) {
    RikoDestination.Favorites.route -> 1
    RikoDestination.MyMusic.route -> 2
    RikoDestination.Settings.route -> 3
    else -> 0   // Home (all_songs) + any pushed screen keeps its origin tab
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun RikoNavGraph(
    navController: NavHostController = rememberNavController(),
    visualOsCoordinator: VisualOsCoordinator? = null
) {
    val playerViewModel: PlayerViewModel = hiltViewModel()
    // PERF FORENSIC P0 (RC-01): only currentSong is read here (mini-slot
    // visibility). Position/progress/isPlaying collect inside the sheet's
    // small leaf composables so the 4Hz ticker never recomposes the
    // NavHost/shell.
    val continuumViewModel: ContinuumViewModel = hiltViewModel()
    val currentSong by playerViewModel.currentSong.collectAsStateWithLifecycle()
    val currentDurationMs by playerViewModel.duration.collectAsStateWithLifecycle()
    val audioState by playerViewModel.audioState.collectAsStateWithLifecycle()
    val albumArtColors by playerViewModel.dynamicColors.collectAsStateWithLifecycle()
    val visualQualityTier by continuumViewModel.visualQualityTier.collectAsStateWithLifecycle()
    val continuumNavState by continuumViewModel.navState.collectAsStateWithLifecycle()
    val nexusState by continuumViewModel.nexusState.collectAsStateWithLifecycle()
    val reduceMotion = rememberReduceMotion()
    val renderProfile = VisualQualityResolver.resolve(
        selectedTier = visualQualityTier,
        capabilities = DeviceRenderCapabilities.current(reduceMotion = reduceMotion),
        shaderFailureCount = LiquidNexusShaders.shaderFailureCount
    )
    // G-5: NP menu "Add to playlist" now has a real picker surface.
    val playlistsViewModel: PlaylistsViewModel = hiltViewModel()
    val playlistsState by playlistsViewModel.uiState.collectAsStateWithLifecycle()

    // ── M1: player sheet owns the anchor truth ──────────────────────────
    // Song taps anywhere expand the sheet instead of navigating to the
    // (blank, retired) now_playing route. Shell mini slot is emptied below;
    // the sheet mini replaces it 1:1, metrics padding unchanged.
    val playerSheetState = rememberPlayerSheetState()
    val sheetScope = rememberCoroutineScope()
    fun expandSheet() {
        sheetScope.launch { playerSheetState.expand() }
    }

    // PERF FIX (real device report â€” lag + app killed after ~20s): the
    // Wave Engine's real-time audio collection ran continuously for the
    // whole app session since playerViewModel is shared/never cleared â€”
    // see the comment in PlayerViewModel.init{}. This actually stops it
    // on background and restarts it on foreground.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, playerViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    playerViewModel.pauseWaveEngineForBackground()
                    continuumViewModel.dispatchNexus(LiquidNexusEvent.LifecycleBackgrounded)
                }
                Lifecycle.Event.ON_START -> {
                    playerViewModel.resumeWaveEngineForForeground()
                    continuumViewModel.dispatchNexus(LiquidNexusEvent.LifecycleRestored)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    val dynamicColors = LocalDynamicColors.current
    val accentColor = com.rikodev.music.android.core.designsystem.riko.theme.AccentViolet
    val secondaryColor = com.rikodev.music.android.core.designsystem.riko.theme.AccentMagenta

    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    LaunchedEffect(currentRoute) {
        currentRoute?.let { continuumViewModel.onRouteVisited(it) }
    }
    LaunchedEffect(currentSong?.id, currentDurationMs) {
        continuumViewModel.dispatchNexus(
            LiquidNexusEvent.TrackChanged(currentSong?.id, currentDurationMs)
        )
    }
    LaunchedEffect(continuumViewModel, playerViewModel) {
        continuumViewModel.effects.collect { effect ->
            when (effect) {
                LiquidNexusEffect.TogglePlayPause -> playerViewModel.togglePlayPause()
                is LiquidNexusEffect.CommitSeek -> playerViewModel.seekTo(effect.positionMs)
                LiquidNexusEffect.OpenFullPlayer -> expandSheet()
                LiquidNexusEffect.CloseFullPlayer -> sheetScope.launch { playerSheetState.collapse() }
                LiquidNexusEffect.BackNotConsumed -> Unit
            }
        }
    }

    val topLevelRoutes = setOf(
        RikoDestination.AllSongs.route,      // Listen section
        RikoDestination.MyMusic.route,       // Explore section
        RikoDestination.LibraryHub.route,    // Library section
        RikoDestination.Favorites.route,     // Library sub-route
        RikoDestination.Playlists.route,     // Library sub-route
        RikoDestination.Settings.route       // Settings utility
    )
    val isTopLevel = currentRoute in topLevelRoutes
    val showMiniPlayer = currentSong != null && currentRoute != RikoDestination.NowPlaying.route
    // Now Playing = immersive fullscreen → bottom nav hidden (2M).
    // M2: the morph sheet hides the shell nav while settled at Full (its
    // Up Next card owns the bottom); reserve/anchor space stays driven by
    // isTopLevel so the sheet's anchors never shift mid-morph.
    val bottomNavVisible = isTopLevel &&
        currentRoute != RikoDestination.NowPlaying.route &&
        playerSheetState.settled != SheetAnchor.Full

    // â”€â”€ Bottom Sheet States â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    var showEqSheet by remember { mutableStateOf(false) }
    var showLyricsSheet by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var showSleepSheet by remember { mutableStateOf(false) }
    var showAddToPlaylistSheet by remember { mutableStateOf(false) }

    // Shell-level transient feedback (A14 undo — Favorites first consumer;
    // Queue remove-no-undo follows in its group).
    val snackbarHostState = remember { SnackbarHostState() }

    val eqSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val lyricsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val queueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val infoSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sleepSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val sheetTopShape = RikoShapes.TopSheet

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // VISUAL OS INTEGRATION â€” Nav â†” Coordinator Bridge
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    //
    // Visual OS à¦¸à¦•à§à¦°à¦¿à¦¯à¦¼ à¦¥à¦¾à¦•à¦²à§‡:
    //   1. VisualOsNavBridge à¦¦à§à¦‡ à¦¦à¦¿à¦•à§‡ sync à¦•à¦°à§‡ (Nav â†” Coordinator)
    //   2. BackHandler à¦¦à¦¿à¦¯à¦¼à§‡ back press chain à¦•à¦¾à¦œ à¦•à¦°à§‡
    //   3. MiniPlayer NavGraph à¦¥à§‡à¦•à§‡ à¦¸à¦°à§‡ à¦¯à¦¾à¦¯à¦¼ (VisualOsHost à¦¦à§‡à¦–à¦¾à¦¯à¦¼)
    //
    // Visual OS inactive à¦¥à¦¾à¦•à¦²à§‡:
    //   - Legacy mode â€” à¦¸à¦¬ existing behavior à¦ à¦¿à¦• à¦†à¦›à§‡
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    val nexusConsumesBack = nexusState.secondaryMenuOpen ||
        nexusState.mode != LiquidNexusMode.Compact
    BackHandler(enabled = nexusConsumesBack) {
        continuumViewModel.dispatchNexus(LiquidNexusEvent.BackPressed)
    }

    if (visualOsCoordinator != null) {
        // â”€â”€ NavBridge: Nav â†” Visual OS sync â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        VisualOsNavBridge(
            navController = navController,
            coordinator = visualOsCoordinator
        )

        // â”€â”€ Back Press Chain â€” Visual OS priority â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        // Plan 1.5: the coordinator publishes motion frames at up to 60fps —
        // the nav root only needs the back-chain flag. Map+distinct so motion
        // ticks never recompose the root scope.
        val chainConsumes by remember(visualOsCoordinator) {
            visualOsCoordinator.state
                .map { it.isOverlayShowing || it.activePanel != null || it.focusScene != null }
                .distinctUntilChanged()
        }.collectAsStateWithLifecycle(initialValue = false)
        // A22 back hierarchy: visualizer overlay -> NP full -> NP mini -> route -> system.
        // The chain covers overlay/panel/focus-scene; nav pop covers everything
        // below the sheet. Enabled ONLY while something is actually poppable, so
        // back at the root reaches the system (exits the app) instead of being
        // swallowed by an always-on handler (G-3 fix).
        val canPopBackStack = navController.previousBackStackEntry != null
        BackHandler(enabled = !nexusConsumesBack && (chainConsumes || canPopBackStack)) {
            val visualOsState = visualOsCoordinator.state.value
            if (!VisualOsBackPressChain.handleBackPress(visualOsState, visualOsCoordinator)) {
                navController.popBackStack()
            }
        }
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // NAVIGATION HOST
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    ProvideRikoLiveClock {
        // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
        // PLAN 2 â€” GLOBAL APP SHELL
        // Ambient background + persistent mini player + floating bottom
        // nav. Screen content stays full-size (scrolls under the shell).
        // Artwork-driven ambient: dynamicColors feed the ambient tints.
        // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
        val scrubBlurDp = if (nexusState.shouldBlurBackground && renderProfile.useRenderEffectBlur) {
            renderProfile.scrubBlurRadiusDp
        } else 0f
        val scrubDimAlpha = when {
            !nexusState.shouldBlurBackground -> 0f
            renderProfile.useDimScrubOverlay -> 0.56f
            else -> 0.24f
        }

        RikoAppShell(
            ambient = RikoAmbientColors(primary = com.rikodev.music.android.core.designsystem.riko.theme.RikoGlowBlob, secondary = com.rikodev.music.android.core.designsystem.riko.theme.RikoGlowBlob),
            miniPlayerVisible = showMiniPlayer,
            bottomNavVisible = bottomNavVisible,
            bottomNavEdgeToEdge = true,
            contentBlurRadiusDp = scrubBlurDp,
            contentDimOverlayAlpha = scrubDimAlpha,
            floatingOverlay = {
                if (showMiniPlayer && playerSheetState.settled != SheetAnchor.Full) {
                    LiquidNexusOverlay(
                        nexusState = nexusState,
                        activeSection = continuumNavState.activeSection,
                        renderProfile = renderProfile,
                        playerViewModel = playerViewModel,
                        onEvent = { continuumViewModel.dispatchNexus(it) },
                        onOpenQueue = { showQueueSheet = true },
                        onOpenLyrics = { showLyricsSheet = true },
                        onOpenSongInfo = { showInfoSheet = true },
                        onOpenAddToPlaylist = { showAddToPlaylistSheet = true },
                        onOpenEqualizer = { showEqSheet = true },
                        onOpenSearch = {
                            navController.navigate(RikoDestination.Search.route) {
                                launchSingleTop = true
                            }
                        },
                        onOpenAlbum = { albumId ->
                            navController.navigate(RikoDestination.AlbumDetail.createRoute(albumId)) {
                                launchSingleTop = true
                            }
                        },
                        onOpenPlaylistsSection = {
                            continuumViewModel.onLibrarySubTabSelected(0)
                            navController.navigate(RikoDestination.LibraryHub.route) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onOpenFullPlayer = { expandSheet() }
                    )
                }
            },
            snackbar = {
                val stackBottom = 16.dp +
                    (if (showMiniPlayer) RikoComponentSize.MiniPlayerHeight + RikoSpacing.SM else 0.dp) +
                    (if (bottomNavVisible) ContinuumWaveBarHeight else 0.dp)
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = stackBottom)
                )
            },
            bottomNav = {
                ContinuumWaveNavBar(
                    activeSection = continuumNavState.activeSection,
                    onSectionSelected = { section ->
                        val targetRoute = continuumViewModel.onSectionSelected(section)
                        navController.navigate(targetRoute) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(RikoDestination.AllSongs.route) { saveState = true }
                        }
                    },
                    renderProfile = renderProfile,
                    primaryAccent = LiquidGlassStyle.sanitizeGlassAccent(albumArtColors.vibrant, accentColor),
                    secondaryAccent = LiquidGlassStyle.sanitizeGlassAccent(albumArtColors.lightVibrant, secondaryColor),
                    audioEnergy = audioState.energy
                )
            }
        ) {
            NavHost(
                navController = navController,
                startDestination = RikoDestination.AllSongs.route,
                modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    val fromSec = ContinuumSection.sectionForRoute(initialState.destination.route).tabIndex
                    val toSec = ContinuumSection.sectionForRoute(targetState.destination.route).tabIndex
                    val dir = if (toSec >= fromSec) 1 else -1
                    slideInHorizontally(animationSpec = RikoMotion.normal()) { it / 5 * dir } +
                        fadeIn(RikoMotion.normal()) +
                        scaleIn(RikoMotion.normal(), initialScale = 0.95f)
                },
                exitTransition = {
                    val fromSec = ContinuumSection.sectionForRoute(initialState.destination.route).tabIndex
                    val toSec = ContinuumSection.sectionForRoute(targetState.destination.route).tabIndex
                    val dir = if (toSec >= fromSec) -1 else 1
                    slideOutHorizontally(animationSpec = RikoMotion.fast()) { it / 6 * dir } +
                        fadeOut(RikoMotion.fast()) +
                        scaleOut(RikoMotion.fast(), targetScale = 0.96f)
                },
                popEnterTransition = { fadeIn(RikoMotion.normal()) },
                popExitTransition = { fadeOut(RikoMotion.fast()) }
            ) {
                composable(RikoDestination.AllSongs.route) {
                    // Plan 3: Home screen â€” NEW premium visuals, SAME route ID,
                    // SAME data sources, SAME playback actions (spec Â§85â€“86).
                    HomeScreen(
                        modifier = Modifier.fillMaxSize(),
                        playerViewModel = playerViewModel,
                        onSongClick = { expandSheet() },
                        onSearchClick = {
                            navController.navigate(RikoDestination.Search.route) {
                                launchSingleTop = true
                            }
                        },
                        onSettingsClick = {
                            navController.navigate(RikoDestination.Settings.route) {
                                launchSingleTop = true
                            }
                        },
                        onOpenLibrary = {
                            navController.navigate(RikoDestination.MyMusic.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable(RikoDestination.MyMusic.route) {
                    // Plan 4 â€” My Music library: Songs/Albums/Artists/Folders
                    // modes. Back returns to Home (spec Â§45â€“46); mode + scroll
                    // state are remembered per mode inside the screen.
                    MyMusicScreen(
                        modifier = Modifier.fillMaxSize(),
                        playerViewModel = playerViewModel,
                        onSongClick = { expandSheet() },
                        onSearchClick = {
                            navController.navigate(RikoDestination.Search.route) {
                                launchSingleTop = true
                            }
                        },
                        onFolderClick = { folderPath ->
                            navController.navigate(RikoDestination.FolderDetail.createRoute(folderPath)) {
                                launchSingleTop = true
                            }
                        },
                        onBackClick = {
                            if (!navController.popBackStack()) {
                                navController.navigate(RikoDestination.AllSongs.route) { launchSingleTop = true }
                            }
                        },
                        onFavoritesClick = {
                            navController.navigate(RikoDestination.Favorites.route) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onPlaylistsClick = {
                            navController.navigate(RikoDestination.Playlists.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable(
                    route = RikoDestination.AlbumDetail.route,
                    arguments = listOf(navArgument(RikoDestination.AlbumDetail.ARG_ALBUM_ID) { type = NavType.LongType })
                ) {
                    // Plan 5 â€” Album detail (shared collection framework).
                    AlbumDetailScreen(
                        modifier = Modifier.fillMaxSize(),
                        playerViewModel = playerViewModel,
                        onBack = { navController.popBackStack() },
                        onSongClick = { expandSheet() }
                    )
                }
                composable(
                    route = RikoDestination.ArtistDetail.route,
                    arguments = listOf(navArgument(RikoDestination.ArtistDetail.ARG_ARTIST_ID) { type = NavType.LongType })
                ) {
                    // Plan 5 â€” Artist detail (identity â†’ songs â†’ albums shelf).
                    ArtistDetailScreen(
                        modifier = Modifier.fillMaxSize(),
                        playerViewModel = playerViewModel,
                        onBack = { navController.popBackStack() },
                        onSongClick = { expandSheet() },
                        onAlbumClick = { albumId ->
                            navController.navigate(RikoDestination.AlbumDetail.createRoute(albumId)) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable(
                    route = RikoDestination.FolderDetail.route,
                    arguments = listOf(navArgument(RikoDestination.FolderDetail.ARG_FOLDER_PATH) { type = NavType.StringType })
                ) {
                    // Plan 5 â€” Folder detail (light utility identity, spec Â§11).
                    FolderDetailScreen(
                        modifier = Modifier.fillMaxSize(),
                        playerViewModel = playerViewModel,
                        onBack = { navController.popBackStack() },
                        onSongClick = { expandSheet() }
                    )
                                }
                composable(RikoDestination.LibraryHub.route) {
                    ContinuumLibraryHubScreen(
                        selectedSubTab = continuumNavState.librarySubTab,
                        onSelectSubTab = continuumViewModel::onLibrarySubTabSelected,
                        playerViewModel = playerViewModel,
                        snackbarHostState = snackbarHostState,
                        onPlaylistClick = { id ->
                            navController.navigate(RikoDestination.PlaylistDetail.createRoute(id))
                        },
                        onPlaylistCreated = { id ->
                            navController.navigate(RikoDestination.PlaylistDetail.createRoute(id)) {
                                launchSingleTop = true
                            }
                        },
                        onSongClick = { expandSheet() },
                        onBrowseExplore = {
                            val target = continuumViewModel.onSectionSelected(ContinuumSection.EXPLORE)
                            navController.navigate(target) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onSearchClick = {
                            navController.navigate(RikoDestination.Search.route) {
                                launchSingleTop = true
                            }
                        },
                        onSettingsClick = {
                            navController.navigate(RikoDestination.Settings.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable(RikoDestination.Favorites.route) {
                    com.rikodev.music.android.presentation.favorites.FavoritesScreen(
                        modifier = Modifier.fillMaxSize(),
                        playerViewModel = playerViewModel,
                        onSongClick = { expandSheet() },
                        onBrowse = {
                            navController.navigate(RikoDestination.MyMusic.route) {
                                launchSingleTop = true
                            }
                        },
                        onSearchClick = {
                            navController.navigate(RikoDestination.Search.route) {
                                launchSingleTop = true
                            }
                        },
                        snackbarHostState = snackbarHostState
                    )
                }
                composable(RikoDestination.Playlists.route) {
                    PlaylistsScreen(
                        modifier = Modifier.fillMaxSize(),
                        onPlaylistClick = { id -> navController.navigate(RikoDestination.PlaylistDetail.createRoute(id)) },
                        onCreated = { id ->
                            navController.navigate(RikoDestination.PlaylistDetail.createRoute(id)) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable(RikoDestination.NowPlaying.route) {
                    // ── CANONICAL NOW PLAYING HANDOFF ────────────────────
                    // The one real Now Playing surface is the morph sheet
                    // (PlayerSheetHost, drawn above this NavHost). This route
                    // survives ONLY for the Visual-OS dock and legacy deep
                    // links: it hands the user to the sheet and removes
                    // itself, so there is exactly one Now Playing state source
                    // and no blank dead-end.
                    LaunchedEffect(Unit) {
                        expandSheet()
                        if (!navController.popBackStack()) {
                            navController.navigate(RikoDestination.AllSongs.route) {
                                launchSingleTop = true
                            }
                        }
                    }
                }
                composable(RikoDestination.Onboarding.route) {
                    // SM spec 3.1 — first-run onboarding. Get Started / Google
                    // both land on Home for now (auth integration out of scope).
                    OnboardingScreen(
                        onGetStarted = {
                            navController.navigate(RikoDestination.AllSongs.route) {
                                popUpTo(RikoDestination.Onboarding.route) { inclusive = true }
                            }
                        },
                        onContinueWithGoogle = {
                            navController.navigate(RikoDestination.AllSongs.route) {
                                popUpTo(RikoDestination.Onboarding.route) { inclusive = true }
                            }
                        }
                    )
                }
                composable(RikoDestination.Search.route) {
                    SearchScreen(
                        modifier = Modifier.fillMaxSize(),
                        playerViewModel = playerViewModel,
                        onBack = { navController.popBackStack() },
                        // Search = the index entry for album/artist details
                        // (My Music grid removal left them unreachable).
                        onAlbumClick = { albumId ->
                            navController.navigate(RikoDestination.AlbumDetail.createRoute(albumId)) {
                                launchSingleTop = true
                            }
                        },
                        onArtistClick = { artistId ->
                            navController.navigate(RikoDestination.ArtistDetail.createRoute(artistId)) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable(RikoDestination.Settings.route) {
                    SettingsScreenWiring(modifier = Modifier.fillMaxSize())
                }
                composable(
                    route = RikoDestination.PlaylistDetail.route,
                    arguments = listOf(navArgument(RikoDestination.PlaylistDetail.ARG_PLAYLIST_ID) { type = NavType.StringType })
                ) {
                    PlaylistDetailScreen(
                        modifier = Modifier.fillMaxSize(),
                        playerViewModel = playerViewModel,
                        onBack = { navController.popBackStack() },
                        onSongClick = { expandSheet() },
                        onDeleted = { navController.popBackStack() }
                    )
                }
            }

            // ── M2: player sheet overlay — above screens, below shell chrome.
            // Content-lambda siblings draw in order, so the sheet covers the
            // NavHost while mini/nav capsules keep floating above it.
            PlayerSheetHost(
                sheetState = playerSheetState,
                viewModel = playerViewModel,
                reserveNavSpace = isTopLevel,
                onCollapse = { sheetScope.launch { playerSheetState.collapse() } },
                    onOpenQueue = { showQueueSheet = true },
                    onOpenEqualizer = { showEqSheet = true },
                    onOpenLyrics = { showLyricsSheet = true },
                    onOpenInfo = { showInfoSheet = true },
                onOpenSleepTimer = { showSleepSheet = true },
                onOpenVisualizer = { /* M6: Aurora spectrum surface */ },
                onOpenAddToPlaylist = { showAddToPlaylistSheet = true },
                showLegacyMiniBar = false,
                onSheetSettledChanged = { anchor ->
                    if (anchor == SheetAnchor.Mini && nexusState.mode == LiquidNexusMode.FullPlayer) {
                        continuumViewModel.dispatchNexus(LiquidNexusEvent.FullPlayerDismissed)
                    } else if (anchor == SheetAnchor.Full && nexusState.mode != LiquidNexusMode.FullPlayer) {
                        continuumViewModel.dispatchNexus(LiquidNexusEvent.FullPlayerRequested)
                    }
                }
            )

            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            // DOCK + BOTTOM NAV LAYER
            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            //
            // Visual OS à¦¸à¦•à§à¦°à¦¿à¦¯à¦¼ à¦¥à¦¾à¦•à¦²à§‡: VisualOsHost dock render à¦•à¦°à§‡
            // Visual OS inactive à¦¥à¦¾à¦•à¦²à§‡: existing MiniPlayer render à¦¹à¦¯à¦¼
            // Bottom Nav à¦¸à¦¬à¦¸à¦®à¦¯à¦¼ à¦à¦–à¦¾à¦¨à§‡ (Visual OS dock !== bottom nav)

            // Plan 2: the old DOCK + BOTTOM NAV Column lived here.
            // Mini player + bottom nav now render inside RikoAppShell (above);
            // Visual OS dock rendering is suppressed via renderDock = false.
        }

        // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
        // VISUAL OS HOST â€” Dock / Overlay / Background
        // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
        // NavHost-à¦à¦° à¦¬à¦¾à¦‡à¦°à§‡, à¦‰à¦ªà¦°à§‡ â€” Z-index à¦…à¦¨à§à¦¸à¦¾à¦°à§‡ layer rendering
        // SM-3: VisualOsHost overlay rendering REMOVED — the Riko shell owns
        // background + mini player + bottom nav. VisualOsHost's overlay
        // (z-100) conflicted with the shell. Files stay dormant for M2.9;
        // coordinator + NavBridge + back-press chain remain active above.

        // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
        // BOTTOM SHEETS â€” Glass panels (existing, managed by NavGraph)
        // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
        // à¦­à¦¬à¦¿à¦·à§à¦¯à¦¤à§‡ Visual OS panel system à¦à¦—à§à¦²à§‹ replace à¦•à¦°à¦¬à§‡
        // à¦à¦–à¦¨ à¦¹à¦¾à¦¤à§‡ à¦°à¦¾à¦–à¦¾ à¦¹à¦šà§à¦›à§‡ â€” backward compatibility
        if (showEqSheet) {
            ModalBottomSheet(
                onDismissRequest = { showEqSheet = false },
                sheetState = eqSheetState,
                containerColor = Color.Transparent,
                contentColor = TextPrimary,
                dragHandle = null,
                shape = sheetTopShape
            ) {
                // Flat Equalizer parity: solid EQ panel — no glass specular
                // shine overlay (it read as a stray blob on dark + light).
                // EqualizerSheet paints its own theme-aware background.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(sheetTopShape)
                        .background(Color.Black)
                ) {
                    EqualizerSheet(viewModel = playerViewModel)
                }
            }
        }
        if (showLyricsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showLyricsSheet = false },
                sheetState = lyricsSheetState,
                containerColor = Color.Transparent,
                contentColor = TextPrimary,
                dragHandle = null,
                shape = sheetTopShape,
                modifier = Modifier.fillMaxHeight(0.85f)
            ) {
                GlassBottomSheetContainer(accentColor = accentColor) {
                    com.rikodev.music.android.presentation.lyrics.host.LyricsScreenWiring(
                        modifier = Modifier.fillMaxSize(),
                        playerViewModel = playerViewModel
                    )
                }
            }
        }
        if (showQueueSheet) {
            ModalBottomSheet(
                onDismissRequest = { showQueueSheet = false },
                sheetState = queueSheetState,
                containerColor = Color.Transparent,
                contentColor = TextPrimary,
                dragHandle = null,
                shape = sheetTopShape,
                modifier = Modifier.fillMaxHeight(0.85f)
            ) {
                GlassBottomSheetContainer(
                    modifier = Modifier.fillMaxHeight(),
                    accentColor = accentColor
                ) {
                    QueueSheet(
                        modifier = Modifier.weight(1f),
                        viewModel = playerViewModel,
                        snackbarHostState = snackbarHostState
                    )
                }
            }
        }
        if (showInfoSheet) {
            ModalBottomSheet(
                onDismissRequest = { showInfoSheet = false },
                sheetState = infoSheetState,
                containerColor = Color.Transparent,
                contentColor = TextPrimary,
                dragHandle = null,
                shape = sheetTopShape
            ) {
                GlassBottomSheetContainer(accentColor = accentColor) {
                    SongInfoSheet(
                        viewModel = playerViewModel,
                        onDismiss = { showInfoSheet = false }
                    )
                }
            }
        }
        if (showAddToPlaylistSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAddToPlaylistSheet = false },
                containerColor = Color.Transparent,
                contentColor = TextPrimary,
                dragHandle = null,
                shape = sheetTopShape,
                modifier = Modifier.fillMaxHeight(0.6f)
            ) {
                GlassBottomSheetContainer(accentColor = accentColor) {
                    AddToPlaylistSheet(
                        playlists = playlistsState.playlists,
                        onPick = { playlist ->
                            currentSong?.let { song ->
                                playerViewModel.addSongToPlaylist(playlist.id, song.id)
                            }
                            showAddToPlaylistSheet = false
                            sheetScope.launch {
                                snackbarHostState.showSnackbar("Added to \"${playlist.name}\"")
                            }
                        },
                        onDismiss = { showAddToPlaylistSheet = false }
                    )
                }
            }
        }
        if (showSleepSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSleepSheet = false },
                sheetState = sleepSheetState,
                containerColor = Color.Transparent,
                contentColor = TextPrimary,
                dragHandle = null,
                shape = sheetTopShape
            ) {
                GlassBottomSheetContainer(accentColor = accentColor) {
                    SleepTimerSheet(viewModel = playerViewModel)
                }
            }
        }
    }
}
