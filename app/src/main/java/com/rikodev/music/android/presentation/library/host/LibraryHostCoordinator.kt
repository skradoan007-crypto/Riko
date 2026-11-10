/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.5 — Library Host Integration
 *
 * Module 1 / 7 — LibraryHostCoordinator.kt ⭐
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [LibraryHostCoordinator] is the *only* place in the Library stack
 * where side effects actually execute. It receives pure
 * [CoordinatorRequest]s emitted by the (pure) LibraryScreenCoordinator
 * and translates them into real Android calls:
 *
 *   - PlayerManager        → playback (play / pause / skip / seek / queue)
 *   - MusicRepository      → favorites, hide, delete, queue modification
 *   - PlaylistRepository   → add-to-playlist, create-playlist
 *   - NavController        → navigation between routes
 *   - HapticFeedback       → vibration contracts
 *   - SnackbarHostState    → user-facing toasts
 *   - PermissionLauncher   → media access requests
 *   - ContextMenuHost      → context menu show / dismiss
 *   - SelectionHost        → selection mode enter / exit / batch
 *   - AccessibilityHost    → TalkBack announcements
 *   - CollectionEngine     → refresh / invalidate / snapshot
 *
 * RULE #83 — Coordinator Executes. Reducer Never Executes.
 * --------------------------------------------------------
 * The Reducer is pure:        (State, Event) → Pair<State, List<Action>>
 * The Screen Coordinator pure: Actions → List<CoordinatorRequest>
 * The Host Coordinator:        CoordinatorRequests → Side Effects → Result Events
 *
 * Nothing above this file knows about PlayerManager / Repository /
 * NavController. Nothing below this file mutates State directly.
 *
 * ARCHITECTURE
 * ------------
 *
 *   User Interaction
 *        │
 *        ▼
 *   LibraryEvent / LibraryScreenEvent
 *        │
 *        ▼
 *   LibraryReducer / LibraryScreenReducer   (pure)
 *        │
 *        ▼ Pair<State, List<Action>>
 *   LibraryScreenCoordinator                (pure — plans)
 *        │
 *        ▼ List<CoordinatorRequest>
 *   LibraryHostCoordinator ⭐  THIS FILE     (impure — executes)
 *        │
 *        ├── PlayerManager.playQueue(...)
 *        ├── Repository.toggleFavorite(...)
 *        ├── NavController.navigate(...)
 *        ├── Haptic.performHapticFeedback(...)
 *        ├── Snackbar.showSnackbar(...)
 *        └── ... (other side effects)
 *        │
 *        ▼ List<ResultEvent>
 *   Result Events → fed back into Reducer   (separate pass)
 *
 * ASYNC MODEL
 * -----------
 * The Host Coordinator is Android-aware. It uses a CoroutineScope
 * (provided by the ViewModel). All side-effect functions are `suspend`.
 * Results are returned as events; the Reducer reduces them later.
 *
 * ERROR HANDLING
 * --------------
 * Every side effect is wrapped in try/catch. Failures become
 * [LibraryEvent.ErrorOccurred] events — never thrown upward. The
 * Reducer then transitions State to ERROR phase. UI shows error state.
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * - Direct State mutation (Reducer's job)
 * - Pure Kotlin-only constructs (we ARE the Android boundary)
 * - UI rendering (Composable functions)
 * - Bypassing the Reducer (results MUST flow back as Events)
 *
 * © RIKO Experience OS — Host Integration Tier
 */

package com.rikodev.music.android.presentation.library.host

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.navigation.NavController
import com.rikodev.music.android.data.repository.MusicRepository
import com.rikodev.music.android.data.repository.PlaylistRepository
import com.rikodev.music.android.presentation.library.HapticType
import com.rikodev.music.android.presentation.library.LibraryContentSnapshot
import com.rikodev.music.android.presentation.library.LibraryEvent
import com.rikodev.music.android.presentation.library.engine.CollectionEngine
import com.rikodev.music.android.presentation.library.screen.runtime.CoordinatorRequest
import com.rikodev.music.android.service.PlayerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.filled.Pause

// ═══════════════════════════════════════════════════════════════════════════
// HOST DEPENDENCY PORTS — abstracted so we can test without Android
// ═══════════════════════════════════════════════════════════════════════════

/**
 * A port for navigation side-effects.
 *
 * The Host Coordinator talks to this interface, not directly to
 * [NavController]. This lets us substitute a fake in unit tests.
 */
interface LibraryNavigationPort {
    fun navigate(route: String, params: Map<String, String>, transitionKey: String)
    fun navigateBack()
    fun openDeepLink(deepLinkKey: String, params: Map<String, String>)
}

/**
 * A port for haptic side-effects.
 */
interface LibraryHapticPort {
    fun performHaptic(hapticKey: String)
    fun performHapticType(type: HapticType)
}

/**
 * A port for snackbar side-effects.
 */
interface LibrarySnackbarPort {
    fun show(messageKey: String, durationMs: Long)
}

/**
 * A port for permission side-effects.
 */
interface LibraryPermissionPort {
    fun request(permissionKey: String)
    fun openAppSettings()
}

/**
 * A port for context-menu side-effects.
 */
interface LibraryContextMenuPort {
    fun show(sectionId: String, itemId: String, stableKey: String)
    fun dismiss()
}

/**
 * A port for selection side-effects.
 *
 * The selection STATE lives in LibraryState. This port only signals
 * "the user wants to enter / exit / perform batch" — the host ViewModel
 * then emits the proper Event back into the Reducer.
 */
interface LibrarySelectionPort {
    fun enterSelection(anchorSectionId: String, firstItemId: String)
    fun exitSelection()
    fun performBatch(selectedItemIds: Set<String>, batchActionKey: String)
}

/**
 * A port for accessibility side-effects.
 */
interface LibraryAccessibilityPort {
    fun announce(messageKey: String)
}

/**
 * A port for the Collection Engine.
 *
 * The Engine is pure Kotlin but its execution is async. This port
 * wraps suspend calls so the Host Coordinator can dispatch them.
 */
interface LibraryEnginePort {
    suspend fun refreshCollection(stableKey: String, force: Boolean): Boolean
    suspend fun refreshAllCollections(force: Boolean): Boolean
    suspend fun invalidateCollection(stableKey: String): Boolean
    suspend fun requestSnapshot(stableKey: String?): Boolean
}

/**
 * A port for import-music side-effects (file picker launch).
 */
interface LibraryImportPort {
    fun importMusic()
}

// ═══════════════════════════════════════════════════════════════════════════
// THE HOST COORDINATOR ⭐
// ═══════════════════════════════════════════════════════════════════════════

/**
 * THE LIBRARY HOST COORDINATOR.
 *
 * The only place where Library side effects actually execute.
 * Receives pure [CoordinatorRequest]s and dispatches them to the
 * appropriate Android dependency via the port interfaces above.
 *
 * Rule #83 — Coordinator Executes. Reducer Never Executes.
 *
 * @param scope              CoroutineScope for async side effects.
 * @param navigationPort     Navigation dispatcher.
 * @param hapticPort         Haptic feedback dispatcher.
 * @param snackbarPort       Snackbar / toast dispatcher.
 * @param permissionPort     Permission request dispatcher.
 * @param contextMenuPort    Context menu show/dispatch dispatcher.
 * @param selectionPort      Selection mode dispatcher.
 * @param accessibilityPort  Accessibility announcement dispatcher.
 * @param enginePort         Collection Engine async port.
 * @param importPort         Import-music (file picker) dispatcher.
 * @param playerManager      The audio playback manager.
 * @param musicRepository    Repository for songs / favorites / hide.
 * @param playlistRepository Repository for playlists.
 *
 * Construction is explicit (no DI magic) so the wiring is auditable
 * in one place. The ViewModel constructs this once per screen.
 */
class LibraryHostCoordinator(
    private val scope: CoroutineScope,
    private val navigationPort: LibraryNavigationPort,
    private val hapticPort: LibraryHapticPort,
    private val snackbarPort: LibrarySnackbarPort,
    private val permissionPort: LibraryPermissionPort,
    private val contextMenuPort: LibraryContextMenuPort,
    private val selectionPort: LibrarySelectionPort,
    private val accessibilityPort: LibraryAccessibilityPort,
    private val enginePort: LibraryEnginePort,
    private val importPort: LibraryImportPort,
    private val playerManager: PlayerManager,
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
) {

    // ───────────────────────────────────────────────────────────────────────
    // RESULT EVENTS — emitted back to the ViewModel for the Reducer
    // ───────────────────────────────────────────────────────────────────────

    private val _resultEvents = MutableSharedFlow<LibraryEvent>(
        replay = 0,
        extraBufferCapacity = 64,
    )
    val resultEvents: SharedFlow<LibraryEvent> = _resultEvents.asSharedFlow()

    // ───────────────────────────────────────────────────────────────────────
    // PUBLIC API — called by the ViewModel after the Screen Coordinator
    // ───────────────────────────────────────────────────────────────────────

    /**
     * Execute a batch of [CoordinatorRequest]s.
     *
     * Each request is dispatched independently. Failures are caught
     * and converted to error events; they never propagate upward.
     *
     * The Reducer will receive the resulting events in a separate
     * pass — never in the same frame as the originating event.
     */
    fun execute(requests: List<CoordinatorRequest>) {
        if (requests.isEmpty()) return
        for (request in requests) {
            executeOne(request)
        }
    }

    /**
     * Execute a single request (convenience overload).
     */
    fun executeOne(request: CoordinatorRequest) {
        try {
            dispatch(request)
        } catch (t: Throwable) {
            emitError("Host coordinator failure: ${t.message ?: "unknown"}")
        }
    }

    // ───────────────────────────────────────────────────────────────────────
    // DISPATCH — switch on CoordinatorRequest variant
    // ───────────────────────────────────────────────────────────────────────

    private fun dispatch(request: CoordinatorRequest) {
        when (request) {
            // ── Engine ─────────────────────────────────────────────────
            is CoordinatorRequest.RefreshCollection ->
                scope.launch {
                    val ok = enginePort.refreshCollection(request.stableKey, request.force)
                    if (ok) emit(LibraryEvent.ContentRefreshed(LibraryContentSnapshot()))
                    else emitError("Collection refresh failed: ${request.stableKey}")
                }
            is CoordinatorRequest.RefreshAllCollections ->
                scope.launch {
                    val ok = enginePort.refreshAllCollections(request.force)
                    if (ok) emit(LibraryEvent.ContentRefreshed(LibraryContentSnapshot()))
                    else emitError("Collection refresh all failed")
                }
            is CoordinatorRequest.InvalidateCollection ->
                scope.launch {
                    enginePort.invalidateCollection(request.stableKey)
                }
            is CoordinatorRequest.RequestSnapshot ->
                scope.launch {
                    enginePort.requestSnapshot(request.stableKey)
                }

            // ── Navigation ──────────────────────────────────────────────
            is CoordinatorRequest.NavigateTo ->
                navigationPort.navigate(
                    route = request.destination.routeKey,
                    params = request.parameters,
                    transitionKey = request.transitionKey,
                )
            CoordinatorRequest.NavigateBack ->
                navigationPort.navigateBack()
            is CoordinatorRequest.OpenDeepLink ->
                navigationPort.openDeepLink(
                    deepLinkKey = request.deepLinkKey,
                    params = request.parameters,
                )

            // ── Playback ───────────────────────────────────────────────
            is CoordinatorRequest.PlayTrack ->
                scope.launch { executePlayTrack(request) }
            CoordinatorRequest.TogglePlayPause ->
                playerManager.togglePlayPause()
            CoordinatorRequest.SkipToNext ->
                playerManager.skipToNext()
            CoordinatorRequest.SkipToPrevious ->
                playerManager.skipToPrevious()
            is CoordinatorRequest.SeekTo ->
                playerManager.seekToProgress(request.positionFraction)

            // ── Collection actions ────────────────────────────────────
            is CoordinatorRequest.AddToFavorites ->
                scope.launch { executeToggleFavorite(request.trackId, true) }
            is CoordinatorRequest.RemoveFromFavorites ->
                scope.launch { executeToggleFavorite(request.trackId, false) }
            is CoordinatorRequest.AddToQueue ->
                scope.launch { executeAddToQueue(request.trackId) }
            is CoordinatorRequest.AddToPlaylist ->
                scope.launch { executeAddToPlaylist(request.trackId, request.playlistId) }
            is CoordinatorRequest.RemoveFromCollection ->
                scope.launch { executeRemoveFromCollection(request.stableKey, request.trackId) }

            // ── Selection ─────────────────────────────────────────────
            is CoordinatorRequest.EnterSelection ->
                selectionPort.enterSelection(
                    anchorSectionId = request.anchorSectionId,
                    firstItemId = request.firstItemId,
                )
            CoordinatorRequest.ExitSelection ->
                selectionPort.exitSelection()
            is CoordinatorRequest.PerformBatchAction ->
                selectionPort.performBatch(
                    selectedItemIds = request.selectedItemIds,
                    batchActionKey = request.batchActionKey,
                )

            // ── System ────────────────────────────────────────────────
            is CoordinatorRequest.RequestPermission ->
                permissionPort.request(request.permissionKey)
            CoordinatorRequest.OpenAppSettings ->
                permissionPort.openAppSettings()
            CoordinatorRequest.ImportMusic ->
                importPort.importMusic()
            is CoordinatorRequest.ShowToast ->
                snackbarPort.show(
                    messageKey = request.messageKey,
                    durationMs = request.durationMs,
                )
            is CoordinatorRequest.ShowContextMenu ->
                contextMenuPort.show(
                    sectionId = request.sectionId,
                    itemId = request.itemId,
                    stableKey = request.stableKey,
                )
            CoordinatorRequest.DismissContextMenu ->
                contextMenuPort.dismiss()
            is CoordinatorRequest.TriggerHaptic ->
                hapticPort.performHaptic(request.hapticKey)
            is CoordinatorRequest.AnnounceForAccessibility ->
                accessibilityPort.announce(request.messageKey)

            // ── No-op ─────────────────────────────────────────────────
            CoordinatorRequest.NoOp -> Unit
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // PLAYBACK EXECUTION
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Execute a PlayTrack request.
     *
     * Looks up the track + queue from the Repository, then asks
     * PlayerManager to play. Emits [LibraryEvent.PlaybackChanged]
     * on success.
     *
     * Single call, zero duplication (Rule: no duplicate playback call).
     */
    private suspend fun executePlayTrack(request: CoordinatorRequest.PlayTrack) {
        val trackId = request.trackId.toLongOrNull() ?: run {
            emitError("Invalid trackId: ${request.trackId}")
            return
        }
        val queueIds = request.queue.mapNotNull { it.toLongOrNull() }
        if (queueIds.isEmpty()) {
            // Single-song play — queue contains only this track.
            val song = musicRepository.getSongById(trackId) ?: run {
                emitError("Track not found: $trackId")
                return
            }
            playerManager.playQueue(listOf(song), startIndex = 0)
            emit(LibraryEvent.PlaybackChanged(
                isPlaying = true,
                currentSongId = trackId,
            ))
            return
        }

        // Batch play — load all queue songs, find start index.
        val songs = musicRepository.getSongsByIds(queueIds)
        if (songs.isEmpty()) {
            emitError("Queue empty for ids: $queueIds")
            return
        }
        val startIndex = queueIds.indexOf(trackId).coerceAtLeast(0)
        playerManager.playQueue(songs, startIndex = startIndex)
        emit(LibraryEvent.PlaybackChanged(
            isPlaying = true,
            currentSongId = trackId,
        ))
    }

    // ═════════════════════════════════════════════════════════════════════
    // COLLECTION EXECUTION
    // ═════════════════════════════════════════════════════════════════════

    private suspend fun executeToggleFavorite(trackIdStr: String, favorite: Boolean) {
        val trackId = trackIdStr.toLongOrNull() ?: run {
            emitError("Invalid trackId for favorite: $trackIdStr")
            return
        }
        // Verify the song exists before toggling favorite.
        musicRepository.getSongById(trackId) ?: run {
            emitError("Track not found for favorite: $trackId")
            return
        }
        try {
            // toggleFavorite() flips current state. We pass `favorite` as
            // the desired target — if it differs from current, we toggle.
            val current = playlistRepository.isFavorite(trackId).first()
            if (current != favorite) {
                playlistRepository.toggleFavorite(trackId)
            }
            // Favorite event flows naturally via Repository's Flow → ViewModel
            // observes it and dispatches LibraryEvent.TapFavorite. Here we
            // only confirm the operation succeeded and trigger haptic.
            hapticPort.performHapticType(HapticType.SELECTION_TOGGLE)
        } catch (t: Throwable) {
            emitError("Favorite update failed: ${t.message}")
        }
    }

    private suspend fun executeAddToQueue(trackIdStr: String) {
        val trackId = trackIdStr.toLongOrNull() ?: run {
            emitError("Invalid trackId for queue: $trackIdStr")
            return
        }
        val song = musicRepository.getSongById(trackId) ?: run {
            emitError("Track not found for queue: $trackId")
            return
        }
        try {
            playerManager.addToQueue(song)
            snackbarPort.show("Added to queue", 2000L)
            hapticPort.performHapticType(HapticType.TAP)
        } catch (t: Throwable) {
            emitError("Add to queue failed: ${t.message}")
        }
    }

    private suspend fun executeAddToPlaylist(trackIdStr: String, playlistIdStr: String) {
        val trackId = trackIdStr.toLongOrNull() ?: run {
            emitError("Invalid trackId for playlist: $trackIdStr")
            return
        }
        val playlistId = playlistIdStr.toLongOrNull() ?: run {
            emitError("Invalid playlistId: $playlistIdStr")
            return
        }
        try {
            playlistRepository.addSongToPlaylist(playlistId, trackId)
            snackbarPort.show("Added to playlist", 2000L)
            hapticPort.performHapticType(HapticType.SELECTION_TOGGLE)
        } catch (t: Throwable) {
            emitError("Add to playlist failed: ${t.message}")
        }
    }

    private suspend fun executeRemoveFromCollection(stableKey: String, trackIdStr: String) {
        // "Remove from collection" semantics: for the FAVORITES collection
        // (stableKey "favorites"), this means unfavorite. For HISTORY
        // (stableKey "recently_played"), it means clear-history-item. For
        // user playlists, it means remove-from-playlist.
        val trackId = trackIdStr.toLongOrNull() ?: run {
            emitError("Invalid trackId for remove: $trackIdStr")
            return
        }
        try {
            when (stableKey) {
                "favorites" -> playlistRepository.toggleFavorite(trackId)
                "recently_played" -> {
                    // History clear-single isn't directly supported by DAO;
                    // we clear-and-keep-recent for now. Engine invalidates.
                    enginePort.invalidateCollection(stableKey)
                }
                else -> {
                    // Treat as a playlist removal — stableKey should be
                    // the playlist ID.
                    val playlistId = stableKey.toLongOrNull() ?: run {
                        emitError("Unknown collection stableKey: $stableKey")
                        return
                    }
                    playlistRepository.removeSongFromPlaylist(playlistId, trackId)
                }
            }
            enginePort.invalidateCollection(stableKey)
            emit(LibraryEvent.ContentRefreshed(LibraryContentSnapshot()))
        } catch (t: Throwable) {
            emitError("Remove from collection failed: ${t.message}")
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // EVENT EMISSION
    // ═════════════════════════════════════════════════════════════════════

    private suspend fun emit(event: LibraryEvent) {
        _resultEvents.emit(event)
    }

    private fun emitError(message: String) {
        scope.launch {
            _resultEvents.emit(LibraryEvent.ErrorOccurred(message))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// DEFAULT PORT IMPLEMENTATIONS — wired by the ViewModel
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Default [LibraryNavigationPort] backed by a [NavController].
 *
 * Builds the actual route string from the destination + parameters
 * and dispatches it. Transition keys are recorded for analytics but
 * the actual transition animation is owned by the NavGraph.
 */
class NavControllerNavigationPort(
    private val navController: NavController,
) : LibraryNavigationPort {
    override fun navigate(route: String, params: Map<String, String>, transitionKey: String) {
        val fullRoute = buildRoute(route, params)
        navController.navigate(fullRoute) {
            launchSingleTop = true
        }
    }

    override fun navigateBack() {
        navController.popBackStack()
    }

    override fun openDeepLink(deepLinkKey: String, params: Map<String, String>) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(buildRoute(deepLinkKey, params)))
        // The host Activity / Application receives this; in practice the
        // ViewModel typically calls navController directly for in-app deep
        // links. This port provides a uniform API either way.
        navController.context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun buildRoute(template: String, params: Map<String, String>): String {
        var result = template
        for ((key, value) in params) {
            result = result.replace("{$key}", value)
        }
        return result
    }
}

/**
 * Default [LibraryHapticPort] backed by Compose's [HapticFeedback].
 *
 * The Compose HapticFeedback instance is captured per-screen and
 * passed here. We translate our internal haptic keys to the
 * platform's [HapticFeedbackType].
 */
class ComposeHapticPort(
    private val hapticFeedback: HapticFeedback,
) : LibraryHapticPort {
    override fun performHaptic(hapticKey: String) {
        val type = when (hapticKey) {
            "TAP", "tap" -> HapticFeedbackType.LongPress
            "LONG_PRESS", "long_press" -> HapticFeedbackType.LongPress
            "SELECTION_TOGGLE", "selection_toggle" -> HapticFeedbackType.TextHandleMove
            "FAST_SCROLL_JUMP", "fast_scroll_jump" -> HapticFeedbackType.LongPress
            else -> HapticFeedbackType.LongPress
        }
        hapticFeedback.performHapticFeedback(type)
    }

    override fun performHapticType(type: HapticType) {
        val platform = when (type) {
            HapticType.TAP -> HapticFeedbackType.LongPress
            HapticType.LONG_PRESS -> HapticFeedbackType.LongPress
            HapticType.SELECTION_TOGGLE -> HapticFeedbackType.TextHandleMove
            HapticType.FAST_SCROLL_JUMP -> HapticFeedbackType.LongPress
        }
        hapticFeedback.performHapticFeedback(platform)
    }
}

/**
 * Default [LibrarySnackbarPort] backed by a [SnackbarHostState]-like API.
 *
 * We keep the contract minimal so it can be backed by either a
 * SnackbarHostState (Compose) or a Toast (legacy).
 */
class SimpleSnackbarPort(
    private val show: (String, Long) -> Unit,
) : LibrarySnackbarPort {
    override fun show(messageKey: String, durationMs: Long) {
        show(messageKey, durationMs)
    }
}

/**
 * Default [LibraryPermissionPort] backed by an Activity-scoped launcher.
 *
 * The launcher functions are supplied by the screen; this port just
 * forwards.
 */
class ActivityPermissionPort(
    private val requestFn: (String) -> Unit,
    private val openSettingsFn: () -> Unit,
) : LibraryPermissionPort {
    override fun request(permissionKey: String) = requestFn(permissionKey)
    override fun openAppSettings() = openSettingsFn()
}

/**
 * Convenience to launch app settings via Intent.
 */
fun buildOpenAppSettingsIntent(context: Context): () -> Unit = {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

/**
 * Default [LibraryContextMenuPort] backed by a simple state holder.
 */
class StateContextMenuPort(
    private val onShow: (String, String, String) -> Unit,
    private val onDismiss: () -> Unit,
) : LibraryContextMenuPort {
    override fun show(sectionId: String, itemId: String, stableKey: String) =
        onShow(sectionId, itemId, stableKey)
    override fun dismiss() = onDismiss()
}

/**
 * Default [LibrarySelectionPort] backed by callbacks to the ViewModel.
 */
class CallbackSelectionPort(
    private val onEnter: (String, String) -> Unit,
    private val onExit: () -> Unit,
    private val onBatch: (Set<String>, String) -> Unit,
) : LibrarySelectionPort {
    override fun enterSelection(anchorSectionId: String, firstItemId: String) =
        onEnter(anchorSectionId, firstItemId)
    override fun exitSelection() = onExit()
    override fun performBatch(selectedItemIds: Set<String>, batchActionKey: String) =
        onBatch(selectedItemIds, batchActionKey)
}

/**
 * Default [LibraryAccessibilityPort] backed by a callback.
 */
class CallbackAccessibilityPort(
    private val onAnnounce: (String) -> Unit,
) : LibraryAccessibilityPort {
    override fun announce(messageKey: String) = onAnnounce(messageKey)
}

/**
 * Default [LibraryEnginePort] backed by the real [CollectionEngine].
 *
 * The CollectionEngine itself is pure Kotlin; this port wraps its
 * suspend entry points so the Host Coordinator can dispatch them
 * without knowing about engine internals.
 */
class RealEnginePort(
    private val engine: CollectionEngine,
) : LibraryEnginePort {
    override suspend fun refreshCollection(stableKey: String, force: Boolean): Boolean {
        return try {
            engine.refreshCollection(stableKey, force)
            true
        } catch (t: Throwable) {
            false
        }
    }

    override suspend fun refreshAllCollections(force: Boolean): Boolean {
        return try {
            engine.refreshAll(force)
            true
        } catch (t: Throwable) {
            false
        }
    }

    override suspend fun invalidateCollection(stableKey: String): Boolean {
        return try {
            engine.invalidate(stableKey)
            true
        } catch (t: Throwable) {
            false
        }
    }

    override suspend fun requestSnapshot(stableKey: String?): Boolean {
        return try {
            if (stableKey != null) engine.requestSnapshot(stableKey)
            else engine.requestAllSnapshots()
            true
        } catch (t: Throwable) {
            false
        }
    }
}

/**
 * Default [LibraryImportPort] backed by a callback (file picker launcher).
 */
class CallbackImportPort(
    private val onImport: () -> Unit,
) : LibraryImportPort {
    override fun importMusic() = onImport()
}
