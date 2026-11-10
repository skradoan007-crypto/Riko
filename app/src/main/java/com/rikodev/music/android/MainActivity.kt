package com.rikodev.music.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.rikodev.music.android.core.designsystem.theme.DynamicColors
import com.rikodev.music.android.core.designsystem.theme.RikoMusicTheme
import com.rikodev.music.android.presentation.navigation.RikoDestination
import com.rikodev.music.android.presentation.navigation.RikoNavGraph
import com.rikodev.music.android.presentation.settings_v2.host.SettingsHostDependencies
import com.rikodev.music.android.presentation.splash.RikoSplashScreen
import com.rikodev.music.android.presentation.nowplaying.PlayerViewModel
import com.rikodev.music.android.audiofx.ExperienceDataHub
import com.rikodev.music.android.core.experience.ColorState
import com.rikodev.music.android.presentation.visualos.host.ProvideVisualOs
import com.rikodev.music.android.presentation.visualos.host.VisualOsCoordinator
import com.rikodev.music.android.presentation.visualos.host.VisualOsCoordinatorWiring
import com.rikodev.music.android.presentation.visualos.host.rememberSystemState
import com.rikodev.music.android.presentation.visualos.runtime.SongContext
import com.rikodev.music.android.presentation.visualos.runtime.VisualOsEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

// ─────────────────────────────────────────────────────────────────────────────
// RIKO EXPERIENCE OS — MAIN ACTIVITY
//
// Visual OS Integration (v2.0):
//   - VisualOsCoordinator তৈরি ও lifecycle manage করে
//   - ProvideVisualOs দিয়ে NavGraph wrap করে
//   - Player state → Song context sync করে
//   - System state (device tier, orientation, reduced motion) detect করে
//   - External callbacks wire করে (navigation, playback, color extraction)
//
// Integration Order (Product Director specified):
//   1. Root nav graph-এর চারপাশে VisualOsWiring
//   2. Library / Now Playing / Settings / Search scenes → scene model map
//   3. Back press chain verify
//   4. Overlay open/close handoff verify
//   5. Dock persistence verify
//   6. Now Playing background blur/restore verify
//   7. Device tier animation budget check
//   8. Accessibility and focus order check
//
// Rule #83: Coordinator Executes — callbacks bridge to real Android services
// ─────────────────────────────────────────────────────────────────────────────

// ⚠️ Plan 2: production now runs inside the new Riko premium shell
// (RikoAppShell — ambient background + mini player + floating bottom nav).
// Set to true only for design-system development — shows the Plan 1
// Preview Lab instead of the production app.
private const val DESIGN_PREVIEW_MODE = false

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var experienceDataHub: ExperienceDataHub

    @Inject
    lateinit var settingsHostDependencies: SettingsHostDependencies

    @Inject
    lateinit var startupReadiness: com.rikodev.music.android.core.startup.StartupReadiness

    @Inject
    lateinit var musicRepository: com.rikodev.music.android.data.repository.MusicRepository

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* permissions result handled by reactive UI */
        // FIX: refresh songs cache after permissions are granted
        musicRepository.refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Plan 3.3: platform splash (Theme.RikoMusic.Splash) hands off to the
        // RIKO branded composable transition.
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // A14: restore persisted settings side effects (e.g. global
        // RikoMotion.motionScale) at app START — not lazily on Settings open.
        lifecycleScope.launch {
            settingsHostDependencies.loadSettings()
        }

        // Plan 3.2: critical readiness runs while the splash animation plays;
        // background warmup starts only after critical work is done. Both are
        // cheap StateFlow-observable (splash keep-check never does IO).
        lifecycleScope.launch {
            startupReadiness.completeCriticalReadiness()
            startupReadiness.startBackgroundWarmup()
        }

        requestRequiredPermissions()

        setContent {
            val playerViewModel: PlayerViewModel = hiltViewModel()

            // Color Intelligence Bridge: When ColorState has real artwork-derived
            // colors, use those (they're richer — include wave, glow, glass tint).
            // Fall back to legacy AlbumArtColors when ColorState is idle.
            // Plan 1.5: ColorState emits at ~30Hz with audio-energy fields —
            // project ONLY the slow color fields (data-class equals) so energy
            // ticks never recompose the activity root. Energy consumers collect
            // colorState in their own leaves. Single combined flow (not
            // remember-keyed): keying on legacy palette would restart the
            // collector and flash default colors on song change.
            val resolvedDynamic by remember(playerViewModel) {
                combine(
                    playerViewModel.colorState,
                    playerViewModel.dynamicColors
                ) { colorState, legacy ->
                    if (colorState.isFromArtwork) {
                        DynamicColors(
                            dominantColor = colorState.backgroundGradient,
                            vibrantColor = colorState.primary,
                            darkVibrantColor = colorState.shadowColor,
                            lightVibrantColor = colorState.secondary,
                            mutedColor = colorState.glassTint,
                            darkMutedColor = colorState.backgroundGradient
                        )
                    } else {
                        DynamicColors(
                            dominantColor = Color(legacy.dominant),
                            vibrantColor = Color(legacy.vibrant),
                            darkVibrantColor = Color(legacy.darkVibrant),
                            lightVibrantColor = Color(legacy.lightVibrant),
                            mutedColor = Color(legacy.muted),
                            darkMutedColor = Color(legacy.darkMuted)
                        )
                    }
                }.distinctUntilChanged()
            }.collectAsStateWithLifecycle(initialValue = DynamicColors())

            // ═════════════════════════════════════════════════════════
            // VISUAL OS COORDINATOR — একক instance, remember করা
            // ═════════════════════════════════════════════════════════
            // Coordinator হলো Visual OS-এর "brain" — state machine
            // Rule #83: এটাই এক্সিকিউট করে, Reducer কখনো না
            val visualOsCoordinator = remember { VisualOsCoordinator() }

            // NavController — NavGraph-এ shared
            val navController = rememberNavController()

            // ═════════════════════════════════════════════════════════
            // SYSTEM STATE DETECTION — Device Tier + Accessibility
            // ═════════════════════════════════════════════════════════
            // Device tier, refresh rate, reduced motion, orientation, multi-window
            // এই তথ্য coordinator-কে dispatch হয় systemState আপডেট করতে
            val systemState = rememberSystemState()

            // System state coordinator-এ sync করি
            LaunchedEffect(systemState) {
                visualOsCoordinator.dispatch(
                    VisualOsEvent.SystemConfigurationChanged(systemState)
                )
            }

            // ═════════════════════════════════════════════════════════
            // PLAYER STATE → SONG CONTEXT SYNC
            // ═════════════════════════════════════════════════════════
            // গান পরিবর্তন হলে Visual OS coordinator-কে জানাই
            // Coordinator background color extraction trigger করে
            val currentSong by playerViewModel.currentSong.collectAsStateWithLifecycle()
            val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()

            LaunchedEffect(currentSong) {
                val song = currentSong
                if (song != null) {
                    visualOsCoordinator.updateSongContext(
                        SongContext(
                            songId = song.id,
                            title = song.title,
                            artist = song.artist,
                            isPlaying = isPlaying
                        )
                    )
                } else {
                    // কোনো গান নেই — dock hide হবে
                    visualOsCoordinator.dispatch(VisualOsEvent.DockDisappeared)
                }
            }

            // ═════════════════════════════════════════════════════════
            // EXTERNAL CALLBACKS — Coordinator → Real Services Bridge
            // ═════════════════════════════════════════════════════════
            // Coordinator শুধু callbacks call করে — real work এখানে হয়
            // Rule #83: Coordinator Executes via callbacks
            DisposableEffect(visualOsCoordinator, navController) {
                // G-4: single owner — these coordinator->world callbacks are wired
                // ONLY here, through VisualOsCoordinatorWiring (unit-tested
                // ownership contract). VisualOsNavBridge never assigns them
                // (it only syncs Nav -> Coordinator scene changes).
                val disposeWiring = VisualOsCoordinatorWiring.wire(
                    coordinator = visualOsCoordinator,
                    navigateToNowPlaying = {
                        navController.navigate(RikoDestination.NowPlaying.route) {
                            launchSingleTop = true
                        }
                    },
                    togglePlayPause = { playerViewModel.togglePlayPause() },
                    skipToNext = { playerViewModel.skipToNext() },
                    skipToPrevious = { playerViewModel.skipToPrevious() },
                    backPress = {
                        if (!navController.popBackStack()) {
                            // Root: nothing to pop — system back (default) closes the app.
                        }
                    },
                    extractColors = { _, callback ->
                        val cs = playerViewModel.colorState.value
                        if (cs.isFromArtwork) {
                            callback(
                                listOf(
                                    cs.primary.toArgb().toLong(),
                                    cs.secondary.toArgb().toLong(),
                                    cs.accent.toArgb().toLong()
                                ),
                                cs.backgroundGradient.toArgb().toLong(),
                                cs.shadowColor.toArgb().toLong()
                            )
                        } else {
                            callback(emptyList(), 0xFF000000L, 0xFF000000L)
                        }
                    }
                )

                onDispose {
                    // Memory-leak guard — LibraryHostCoordinator pattern.
                    disposeWiring()
                }
            }

            // ═════════════════════════════════════════════════════════
            // THEME + VISUAL OS + SPLASH → NAVIGATION
            // ═════════════════════════════════════════════════════════
            // ⚠️ TEMPORARY (Plan 1): DESIGN_PREVIEW_MODE shows the Preview Lab.
            // REVERT in Plan 2 — restore plain RikoMusicTheme call.
            if (DESIGN_PREVIEW_MODE) {
                com.rikodev.music.android.core.designsystem.riko.theme.RikoTheme {
                    com.rikodev.music.android.core.designsystem.riko.preview.RikoDesignSystemPreviewScreen()
                }
            } else RikoMusicTheme(dynamicColors = resolvedDynamic) {
                // Plan 3.3/3.4: splash stays until BOTH the branded animation
                // and critical readiness complete. The keep-check is pure
                // State reads (never IO). TTFD reports only when Home is real.
                var splashFinished by remember { mutableStateOf(false) }
                val criticalReady by startupReadiness.criticalReady.collectAsStateWithLifecycle()
                val criticalProgress by startupReadiness.criticalProgress.collectAsStateWithLifecycle()
                // FIX: force-hide splash after 8 seconds even if criticalReady
                // never becomes true (e.g. permissions not granted yet).
                var splashTimeout by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(8_000L)
                    splashTimeout = true
                }
                val splashVisible = (!splashFinished || !criticalReady) && !splashTimeout
                // SM spec 3.1 — onboarding shown once per install (first-run
                // gate; splash + onboarding + home on subsequent launches).
                val context = androidx.compose.ui.platform.LocalContext.current
                val prefs = remember { context.getSharedPreferences("riko_onboarding", android.content.Context.MODE_PRIVATE) }
                var showOnboarding by remember { mutableStateOf(prefs.getBoolean("seen", false).not()) }
                LaunchedEffect(splashVisible, showOnboarding, criticalReady) {
                    if (!splashVisible && !showOnboarding && criticalReady) {
                        // 3.4 trustworthy TTFD: one frame AFTER Home composes.
                        withFrameNanos { }
                        runCatching { this@MainActivity.reportFullyDrawn() }
                    }
                }
                if (splashVisible) {
                    RikoSplashScreen(
                        progress = criticalProgress,
                        onFinished = { splashFinished = true }
                    )
                } else if (showOnboarding) {
                    com.rikodev.music.android.core.designsystem.riko.theme.RikoTheme {
                        com.rikodev.music.android.presentation.onboarding.OnboardingScreen(
                            onGetStarted = {
                                prefs.edit().putBoolean("seen", true).apply()
                                showOnboarding = false
                            },
                            onContinueWithGoogle = {
                                prefs.edit().putBoolean("seen", true).apply()
                                showOnboarding = false
                            }
                        )
                    }
                } else {
                    // ── Visual OS Wrapping ─────────────────────────────
                    // ProvideVisualOs হলো THE integration point
                    // NavGraph-কে wrap করে Visual OS layers activate করে
                    // useVisualOs() দিয়ে যেকোনো composable coordinator access পায়
                    ProvideVisualOs(visualOsCoordinator) {
                        RikoNavGraph(
                            navController = navController,
                            visualOsCoordinator = visualOsCoordinator
                        )
                    }
                }
            }
        }
    }

    // RUNTIME FIX 3: startBridges() used to be a one-shot call from
    // onCreate() with no matching stopBridges() anywhere — a third
    // invisible, lifecycle-unaware continuous process (same root pattern as
    // RuntimeFix1's VisualOsCoordinator loop and RuntimeFix2's WaveEngine
    // collection), running PCM/visualizer/player-state collectors at high
    // frequency even while the app was backgrounded. onStart()/onStop() are
    // the Activity's own real lifecycle callbacks, so bridges now start only
    // when RIKO is actually visible and stop the moment it isn't.
    override fun onStart() {
        super.onStart()
        experienceDataHub.startBridges()
    }

    override fun onStop() {
        super.onStop()
        experienceDataHub.stopBridges()
    }

    private fun requestRequiredPermissions() {
        val permissions = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_AUDIO)
                add(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            permissionLauncher.launch(notGranted.toTypedArray())
        }
    }
}
