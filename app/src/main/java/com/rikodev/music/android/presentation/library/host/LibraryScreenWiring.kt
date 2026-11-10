/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.5 — Library Host Integration
 *
 * Module 3 / 7 — LibraryScreenWiring.kt ⭐
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [LibraryScreenWiring] is the single Compose entry point for the
 * Library screen. It wires together:
 *
 *   - LibraryComposition (B5.3 — pure rendering)
 *   - LibraryInteractionRuntime (B5.4 — gesture/selection/context)
 *   - LibraryViewModelBridge (B5.5 — state + reducer + coordinator)
 *   - LibraryHostCoordinator (B5.5 — side-effect executor, Rule #83)
 *
 * The screen's ONLY job is:
 *   1. Construct the Host Coordinator with screen-scoped dependencies
 *      (NavController, HapticFeedback, SnackbarHostState, etc.)
 *   2. Register it with the ViewModel
 *   3. Collect LibraryState via StateFlow
 *   4. Feed state into LibraryComposition
 *   5. Route UI callbacks → ViewModel.dispatch()
 *
 * The screen contains ZERO business logic. ZERO state decisions.
 * ZERO direct Repository / PlayerManager access. Everything flows
 * through the ViewModel → Reducer → Coordinator chain.
 *
 * ARCHITECTURE
 * ------------
 *
 *   ┌─────────────────────────────────────────────────────┐
 *   │                Compose Screen (this file)            │
 *   │                                                     │
 *   │  ┌───────────────┐   ┌──────────────────────┐      │
 *   │  │   ViewModel   │   │  Host Coordinator    │      │
 *   │  │   (Bridge)    │───│  (Rule #83)          │      │
 *   │  └───────────────┘   └──────────────────────┘      │
 *   │          │                      │                   │
 *   │     StateFlow              Side Effects             │
 *   │          │                      │                   │
 *   │          ▼                      ▼                   │
 *   │  ┌──────────────────────────────────────────┐       │
 *   │  │       LibraryComposition (pure)          │       │
 *   │  │       + Interaction Runtime              │       │
 *   │  └──────────────────────────────────────────┘       │
 *   └─────────────────────────────────────────────────────┘
 *
 * RULES ENFORCED HERE
 * -------------------
 * #31  Runtime Owns Truth         — UI reads StateFlow only
 * #33  Cards Never Fetch          — no Repository access here
 * #55  Screen Owns Composition    — this file is the composition root
 * #72  Identity Before Feature    — screen reflects Library identity
 * #83  Coordinator Executes       — Host Coordinator wired here
 *
 * © RIKO Experience OS — Host Integration Tier
 */

package com.rikodev.music.android.presentation.library.host

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rikodev.music.android.presentation.library.LibraryEvent
import com.rikodev.music.android.presentation.library.screen.composition.LibraryComposition
import com.rikodev.music.android.presentation.library.screen.contract.LibraryConstitution
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import androidx.compose.animation.core.Animatable

// ═══════════════════════════════════════════════════════════════════════════
// PUBLIC API — used by the NavGraph
// ═══════════════════════════════════════════════════════════════════════════

/**
 * THE LIBRARY SCREEN — single Compose entry point.
 *
 * Usage in NavGraph:
 *   composable("library") {
 *       LibraryScreenWiring(
 *           navController = navController,
 *           contentPadding = innerPadding,
 *       )
 *   }
 *
 * This is the ONLY composable the NavGraph calls. Everything else
 * (Composition, Interaction, Coordinator wiring) happens internally.
 *
 * @param navController   For navigation side effects.
 * @param contentPadding  Padding from the scaffold / inset.
 * @param modifier        External modifier.
 */
@Composable
fun LibraryScreenWiring(
    navController: NavController,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier,
) {
    val viewModel: LibraryViewModelBridge = hiltViewModel()
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // ── 1. Construct ports (screen-scoped Android dependencies) ────────────
    val ports = remember(navController, hapticFeedback, snackbarHostState) {
        LibraryScreenPorts(
            navigationPort = NavControllerNavigationPort(navController),
            hapticPort = ComposeHapticPort(hapticFeedback),
            snackbarPort = SimpleSnackbarPort { message, durationMs ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = null,
                        duration = when {
                            durationMs < 1500 -> androidx.compose.material3.SnackbarDuration.Short
                            durationMs > 3000 -> androidx.compose.material3.SnackbarDuration.Long
                            else -> androidx.compose.material3.SnackbarDuration.Short
                        },
                    )
                }
            },
            permissionPort = ActivityPermissionPort(
                requestFn = { _ ->
                    // Permission launching is wired at the Activity level.
                    // For now, we just emit a PermissionResult(false) event.
                    viewModel.dispatch(LibraryEvent.PermissionResult(granted = false))
                },
                openSettingsFn = buildOpenAppSettingsIntent(context),
            ),
            contextMenuPort = StateContextMenuPort(
                onShow = { _, _, _ -> /* ContextMenu UI lives in Composition */ },
                onDismiss = { /* dismiss handled by Composition */ },
            ),
            selectionPort = CallbackSelectionPort(
                onEnter = { _, firstItemId ->
                    viewModel.dispatch(LibraryEvent.LongPressItem(firstItemId.toLongOrNull() ?: 0L))
                },
                onExit = { viewModel.dispatch(LibraryEvent.ClearSelection) },
                onBatch = { _, _ -> /* batch handled by Composition */ },
            ),
            accessibilityPort = CallbackAccessibilityPort(
                onAnnounce = { _ -> /* announce via semantics */ },
            ),
            importPort = CallbackImportPort(
                onImport = { /* file picker launch via Activity */ },
            ),
            enginePort = object : LibraryEnginePort {
                override suspend fun refreshCollection(stableKey: String, force: Boolean) = false
                override suspend fun refreshAllCollections(force: Boolean) = false
                override suspend fun invalidateCollection(stableKey: String) = false
                override suspend fun requestSnapshot(stableKey: String?) = false
            },
        )
    }

    // ── 2. Bind the Host Coordinator via the ViewModel ─────────────────────
    //    The ViewModel owns data deps (PlayerManager, Repository, Engine);
    //    the screen owns Android ports (NavController, Haptic, Snackbar).
    //    bindHostCoordinator() bridges both inside the ViewModel.
    LaunchedEffect(viewModel, ports) {
        viewModel.bindHostCoordinator(
            scope = coroutineScope,
            ports = ports,
        )
    }

    // ── 3. Collect state ───────────────────────────────────────────────────
    val libraryState by viewModel.libraryState.collectAsState()
    val screenState by viewModel.screenState.collectAsState()
    val waveState by viewModel.waveEngine.state.collectAsState()

    // ── 4. Render ──────────────────────────────────────────────────────────
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        LibraryComposition(
            constitution = LibraryConstitution.DEFAULT,
            state = screenState,
            waveState = waveState,
            modifier = Modifier.fillMaxSize(),
            playPauseIcon = if (libraryState.dock.isDocked)
                Icons.Filled.Pause else Icons.Filled.PlayArrow,
            nextIcon = Icons.Filled.SkipNext,
            onHeroTap = {
                viewModel.dispatch(LibraryEvent.DockTapped)
            },
            onSectionTap = { sectionId ->
                viewModel.dispatch(LibraryEvent.StickySectionChanged(sectionId))
            },
            onCardTap = { cardKey ->
                // Card taps are routed to the appropriate destination.
                // The cardKey encodes the type + id (e.g., "album:42").
                val parts = cardKey.split(":")
                if (parts.size == 2) {
                    when (parts[0]) {
                        "album" -> parts[1].toLongOrNull()?.let {
                            viewModel.dispatch(LibraryEvent.TapAlbum(it))
                        }
                        "artist" -> parts[1].toLongOrNull()?.let {
                            viewModel.dispatch(LibraryEvent.TapArtist(it))
                        }
                        "playlist" -> parts[1].toLongOrNull()?.let {
                            viewModel.dispatch(LibraryEvent.TapPlaylist(it))
                        }
                        "folder" -> {
                            viewModel.dispatch(LibraryEvent.TapFolder(parts[1]))
                        }
                        "collection" -> {
                            viewModel.dispatch(LibraryEvent.TapCollection(parts[1]))
                        }
                        "song" -> parts[1].toLongOrNull()?.let {
                            viewModel.dispatch(LibraryEvent.TapSong(it, null))
                        }
                    }
                }
            },
            onDockTap = {
                viewModel.dispatch(LibraryEvent.DockTapped)
            },
            onPlayPauseTap = {
                viewModel.togglePlayPause()
            },
            onNextTap = {
                viewModel.skipToNext()
            },
            onEmptyAction = { actionKey ->
                when (actionKey) {
                    "IMPORT_MUSIC" -> viewModel.refresh()
                    "REQUEST_PERMISSION" -> viewModel.dispatch(LibraryEvent.PermissionResult(granted = false))
                    "RETRY" -> viewModel.refresh()
                }
            },
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SCREEN PORTS BUNDLE — convenient holder for screen-scoped dependencies
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Bundle of ports constructed at the screen level.
 *
 * Held in a single class so we can `remember` them together (they all
 * share the same lifetime — tied to the screen's composition).
 */
data class LibraryScreenPorts(
    val navigationPort: LibraryNavigationPort,
    val hapticPort: LibraryHapticPort,
    val snackbarPort: LibrarySnackbarPort,
    val permissionPort: LibraryPermissionPort,
    val contextMenuPort: LibraryContextMenuPort,
    val selectionPort: LibrarySelectionPort,
    val accessibilityPort: LibraryAccessibilityPort,
    val enginePort: LibraryEnginePort,
    val importPort: LibraryImportPort,
)

// ═══════════════════════════════════════════════════════════════════════════
// INTERACTION RUNTIME HOST — wraps B5.4 interaction layer
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The Interaction Runtime Host.
 *
 * Wraps the B5.4 interaction layer (gestures, selection, context menu,
 * drag, haptic) and exposes a single composable that overlays the
 * Library screen with gesture detection.
 *
 * The interaction layer is "always on" — it listens to pointer events
 * on top of the rendering layer. Gestures are interpreted into Intents,
 * Intents are dispatched as Events to the ViewModel.
 *
 * Rule #78 — Interaction Creates Intent, Never Action.
 * Rule #79 — Selection Is Independent.
 * Rule #80 — Gesture Never Knows UI.
 */
@Composable
fun LibraryInteractionRuntimeHost(
    modifier: Modifier = Modifier,
    onIntent: (LibraryInteractionIntent) -> Unit,
    content: @Composable () -> Unit,
) {
    // The actual gesture detection is implemented in B5.4's
    // GestureResolver and AccessibilityInteraction. Here we host
    // them as a transparent overlay.
    //
    // For now, this is a pass-through — the gesture layer is
    // integrated inside LibraryComposition via its pointer-input
    // modifiers. This wrapper exists so the screen can register
    // additional accessibility listeners if needed.
    Box(modifier = modifier) {
        content()
    }
}

/**
 * A sealed vocabulary of intents the Interaction Runtime can produce.
 *
 * Rule #78 — Interaction Creates Intent, Never Action.
 * These are NOT actions. They are intent signals that the ViewModel
 * translates into Events, which the Reducer reduces.
 */
sealed class LibraryInteractionIntent {
    /** User tapped a card. */
    data class CardTapped(val cardKey: String) : LibraryInteractionIntent()

    /** User long-pressed an item — enter selection mode. */
    data class ItemLongPressed(val itemId: Long) : LibraryInteractionIntent()

    /** User toggled selection on an item. */
    data class SelectionToggled(val itemId: Long) : LibraryInteractionIntent()

    /** User cleared selection. */
    object SelectionCleared : LibraryInteractionIntent()

    /** User opened a context menu. */
    data class ContextMenuOpened(val itemId: Long, val itemType: String) : LibraryInteractionIntent()

    /** User dismissed a context menu. */
    object ContextMenuDismissed : LibraryInteractionIntent()

    /** User performed a dock pull (swipe up on dock). */
    object DockPull : LibraryInteractionIntent()

    /** User performed a pinch gesture. */
    data class Pinched(val scale: Float) : LibraryInteractionIntent()

    /** User scrolled. */
    data class Scrolled(val delta: Float) : LibraryInteractionIntent()
}
