/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.2 — Library Screen Runtime
 *
 * File 7 / 8 — LibraryScreenCoordinator.kt ⭐
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [LibraryScreenCoordinator] is the *orchestration layer* of the
 * Library screen. It receives Action requests from the Reducer,
 * executes them (calling the Engine / Audio Runtime / Navigation),
 * and emits new Events when results arrive.
 *
 * THE PURE REDUCER PATTERN (Rule #47)
 * -----------------------------------
 * The Reducer is pure:
 *
 *   (State, Event) → Pair<State, List<Action>>
 *
 * The Coordinator is impure (it has side effects), but it never
 * touches State directly. It only:
 *   1. Receives Action requests.
 *   2. Executes them (Engine calls, Audio calls, Nav calls).
 *   3. Emits new Events when results arrive.
 *
 * The Reducer then handles those Events in a separate pass,
 * producing a new State.
 *
 * ARCHITECTURE
 * ------------
 *
 *   User Input ─► Event ─► Reducer ─► (State, Actions)
 *                                              │
 *                                              ▼
 *                                         Coordinator
 *                                              │
 *                                              │ (executes actions:
 *                                              │  Engine, Audio, Nav)
 *                                              │
 *                                              ▼
 *                                         Result Events
 *                                              │
 *   ◄──────────────────────────────────────────┘
 *   (fed back into Reducer)
 *
 * WHY THIS SEPARATION MATTERS
 * ----------------------------
 *   1. The Reducer is unit-testable without the Engine.
 *   2. The Coordinator is the only place async work happens.
 *   3. The Reducer never predicts what the Engine will return.
 *
 * ASYNC MODEL
 * -----------
 * The Coordinator itself is *pure Kotlin* — no coroutines, no
 * Android. It does NOT execute async work directly. Instead, it
 * returns [CoordinatorRequest]s that the host (B5.5 ViewModel)
 * executes. This keeps B5.2 pure-Kotlin and unit-testable.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #47  Reducer Never Predicts      — Coordinator, not Reducer, calls Engine
 * #48  Actions Describe Intent     — Coordinator executes intent
 * #53  Diagnostics Never Affects Engine — diagnostics recorded but never acted on
 * #55  Screen Owns Composition     — Coordinator is screen-level
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Compose, no Android framework, no Coroutine, no Flow, no
 * ViewModel. Pure Kotlin.
 *
 * © RIKO Experience OS — Runtime Tier
 */

package com.rikodev.music.android.presentation.library.screen.runtime

import com.rikodev.music.android.presentation.library.engine.CollectionEngine
import com.rikodev.music.android.presentation.library.runtime.CollectionRuntimeState
import com.rikodev.music.android.presentation.library.screen.contract.LibraryConstitution
import com.rikodev.music.android.presentation.library.screen.contract.LibraryNavigationDestination
import androidx.compose.material.icons.filled.Pause
import androidx.compose.animation.core.Animatable
import com.rikodev.music.android.presentation.library.screen.runtime.RuntimeEvent

/**
 * THE LIBRARY SCREEN COORDINATOR.
 *
 * Receives Action requests from the Reducer, plans the side-effect
 * work, and returns [CoordinatorRequest]s the host (B5.5 ViewModel)
 * executes. The host feeds results back as new Events.
 *
 * The Coordinator itself is *pure* — it does not execute async work
 * directly. It plans what needs to happen; the host does it.
 *
 * Why pure? Because B5.2 must remain unit-testable on JVM. Async
 * execution is the host's responsibility (B5.5).
 */
class LibraryScreenCoordinator(
    private val constitution: LibraryConstitution,
    private val engine: CollectionEngine?,
    private val diagnostics: LibraryRuntimeDiagnostics = LibraryRuntimeDiagnostics(),
) {

    /**
     * Process a list of Action requests from the Reducer.
     *
     * Returns a list of [CoordinatorRequest]s the host should execute.
     * Each request describes *what* to do; the host does *how*.
     *
     * @property state     Current screen state.
     * @property actions   Action requests from the Reducer.
     * @return             Coordinator requests for the host to execute.
     */
    fun processActions(
        state: LibraryScreenState,
        actions: List<LibraryScreenAction>,
    ): List<CoordinatorRequest> {
        if (actions.isEmpty()) return emptyList()

        return actions.mapNotNull { action ->
            processAction(state, action)
        }
    }

    /**
     * Process a single Action request.
     *
     * Returns null if the action is purely informational (e.g.
     * diagnostics) and needs no host execution.
     */
    private fun processAction(
        state: LibraryScreenState,
        action: LibraryScreenAction,
    ): CoordinatorRequest? {
        // Record diagnostics first (Rule #53 — observation only).
        if (action is LibraryScreenAction.RecordDiagnostic) {
            diagnostics.recordEvent(
                RuntimeEvent(
                    eventKey = action.diagnosticKey,
                    payload = action.payload,
                    timestampMs = state.lastUpdatedMs,
                )
            )
            return null
        }

        return when {
            action.requiresEngine() -> planEngineRequest(state, action)
            action.requiresAudioRuntime() -> planAudioRequest(state, action)
            action.requiresNavigation() -> planNavigationRequest(state, action)
            else -> planSystemRequest(state, action)
        }
    }

    // -----------------------------------------------------------------
    // ENGINE REQUEST PLANNING
    // -----------------------------------------------------------------

    private fun planEngineRequest(
        state: LibraryScreenState,
        action: LibraryScreenAction,
    ): CoordinatorRequest? {
        if (engine == null) return null

        return when (action) {
            is LibraryScreenAction.RefreshCollection ->
                CoordinatorRequest.RefreshCollection(
                    stableKey = action.stableKey,
                    force = action.force,
                )

            is LibraryScreenAction.RefreshAllCollections ->
                CoordinatorRequest.RefreshAllCollections(
                    force = action.force,
                )

            is LibraryScreenAction.InvalidateCollection ->
                CoordinatorRequest.InvalidateCollection(
                    stableKey = action.stableKey,
                )

            is LibraryScreenAction.RequestSnapshot ->
                CoordinatorRequest.RequestSnapshot(
                    stableKey = action.stableKey,
                )

            else -> null
        }
    }

    // -----------------------------------------------------------------
    // AUDIO REQUEST PLANNING
    // -----------------------------------------------------------------

    private fun planAudioRequest(
        state: LibraryScreenState,
        action: LibraryScreenAction,
    ): CoordinatorRequest = when (action) {
        is LibraryScreenAction.PlayTrack ->
            CoordinatorRequest.PlayTrack(
                trackId = action.trackId,
                queue = action.queue,
                sourceStableKey = action.sourceStableKey,
            )

        is LibraryScreenAction.TogglePlayPause ->
            CoordinatorRequest.TogglePlayPause

        is LibraryScreenAction.SkipToNext ->
            CoordinatorRequest.SkipToNext

        is LibraryScreenAction.SkipToPrevious ->
            CoordinatorRequest.SkipToPrevious

        is LibraryScreenAction.SeekTo ->
            CoordinatorRequest.SeekTo(action.positionFraction)

        else ->
            CoordinatorRequest.NoOp
    }

    // -----------------------------------------------------------------
    // NAVIGATION REQUEST PLANNING
    // -----------------------------------------------------------------

    private fun planNavigationRequest(
        state: LibraryScreenState,
        action: LibraryScreenAction,
    ): CoordinatorRequest = when (action) {
        is LibraryScreenAction.NavigateTo ->
            CoordinatorRequest.NavigateTo(
                destination = action.destination,
                parameters = action.parameters,
                transitionKey = constitution.navigation
                    .transitionFor(action.destination).kind.name,
            )

        is LibraryScreenAction.NavigateBack ->
            CoordinatorRequest.NavigateBack

        is LibraryScreenAction.OpenDeepLink ->
            CoordinatorRequest.OpenDeepLink(
                deepLinkKey = action.deepLinkKey,
                parameters = action.parameters,
            )

        else -> CoordinatorRequest.NoOp
    }

    // -----------------------------------------------------------------
    // SYSTEM REQUEST PLANNING
    // -----------------------------------------------------------------

    private fun planSystemRequest(
        state: LibraryScreenState,
        action: LibraryScreenAction,
    ): CoordinatorRequest = when (action) {
        is LibraryScreenAction.RequestPermission ->
            CoordinatorRequest.RequestPermission(action.permissionKey)

        is LibraryScreenAction.OpenAppSettings ->
            CoordinatorRequest.OpenAppSettings

        is LibraryScreenAction.ImportMusic ->
            CoordinatorRequest.ImportMusic

        is LibraryScreenAction.ShowToast ->
            CoordinatorRequest.ShowToast(
                messageKey = action.messageKey,
                durationMs = action.durationMs,
            )

        is LibraryScreenAction.ShowContextMenu ->
            CoordinatorRequest.ShowContextMenu(
                sectionId = action.sectionId,
                itemId = action.itemId,
                stableKey = action.stableKey,
            )

        is LibraryScreenAction.DismissContextMenu ->
            CoordinatorRequest.DismissContextMenu

        is LibraryScreenAction.TriggerHaptic ->
            CoordinatorRequest.TriggerHaptic(action.hapticKey)

        is LibraryScreenAction.AnnounceForAccessibility ->
            CoordinatorRequest.AnnounceForAccessibility(action.messageKey)

        is LibraryScreenAction.EnterSelection ->
            CoordinatorRequest.EnterSelection(
                anchorSectionId = action.anchorSectionId,
                firstItemId = action.firstItemId,
            )

        is LibraryScreenAction.ExitSelection ->
            CoordinatorRequest.ExitSelection

        is LibraryScreenAction.PerformBatchAction ->
            CoordinatorRequest.PerformBatchAction(
                selectedItemIds = action.selectedItemIds,
                batchActionKey = action.batchActionKey,
            )

        // Collection actions (favorites, queue, playlist, remove)
        is LibraryScreenAction.AddToFavorites ->
            CoordinatorRequest.AddToFavorites(action.trackId)

        is LibraryScreenAction.RemoveFromFavorites ->
            CoordinatorRequest.RemoveFromFavorites(action.trackId)

        is LibraryScreenAction.AddToQueue ->
            CoordinatorRequest.AddToQueue(action.trackId)

        is LibraryScreenAction.AddToPlaylist ->
            CoordinatorRequest.AddToPlaylist(
                trackId = action.trackId,
                playlistId = action.playlistId,
            )

        is LibraryScreenAction.RemoveFromCollection ->
            CoordinatorRequest.RemoveFromCollection(
                stableKey = action.stableKey,
                trackId = action.trackId,
            )

        else -> CoordinatorRequest.NoOp
    }

    /**
     * Convenience: reduce + coordinate in one call.
     *
     * Useful for the host (B5.5) that wants a single entry point.
     * Returns the new state + the coordinator requests.
     */
    fun reduceAndCoordinate(
        state: LibraryScreenState,
        event: LibraryScreenEvent,
    ): Pair<LibraryScreenState, List<CoordinatorRequest>> {
        val (newState, actions) = LibraryScreenReducer.reduce(state, event)
        val requests = processActions(newState, actions)
        return newState to requests
    }

    /**
     * Reset the coordinator's diagnostics.
     */
    fun resetDiagnostics() {
        diagnostics.clear()
    }
}

/**
 * A request from the Coordinator to the host (B5.5 ViewModel).
 *
 * The host executes these requests. Results arrive as new Events,
 * which the Reducer handles in a separate pass.
 *
 * Why requests instead of direct execution? Because B5.2 must
 * remain pure Kotlin (no Android / Coroutine). The host owns async.
 */
sealed class CoordinatorRequest {

    // ----- ENGINE REQUESTS -----

    /** Refresh a specific collection. */
    data class RefreshCollection(
        val stableKey: String,
        val force: Boolean,
    ) : CoordinatorRequest()

    /** Refresh all collections. */
    data class RefreshAllCollections(
        val force: Boolean,
    ) : CoordinatorRequest()

    /** Invalidate cache for a collection. */
    data class InvalidateCollection(
        val stableKey: String,
    ) : CoordinatorRequest()

    /** Request a new render snapshot. */
    data class RequestSnapshot(
        val stableKey: String?,
    ) : CoordinatorRequest()

    // ----- NAVIGATION REQUESTS -----

    /** Navigate to a destination. */
    data class NavigateTo(
        val destination: LibraryNavigationDestination,
        val parameters: Map<String, String>,
        val transitionKey: String,
    ) : CoordinatorRequest()

    /** Navigate back. */
    object NavigateBack : CoordinatorRequest()

    /** Open a deep link. */
    data class OpenDeepLink(
        val deepLinkKey: String,
        val parameters: Map<String, String>,
    ) : CoordinatorRequest()

    // ----- AUDIO REQUESTS -----

    /** Play a track. */
    data class PlayTrack(
        val trackId: String,
        val queue: List<String>,
        val sourceStableKey: String,
    ) : CoordinatorRequest()

    /** Toggle play/pause. */
    object TogglePlayPause : CoordinatorRequest()

    /** Skip to next track. */
    object SkipToNext : CoordinatorRequest()

    /** Skip to previous track. */
    object SkipToPrevious : CoordinatorRequest()

    /** Seek to a position. */
    data class SeekTo(
        val positionFraction: Float,
    ) : CoordinatorRequest()

    // ----- COLLECTION REQUESTS -----

    /** Add a track to favorites. */
    data class AddToFavorites(
        val trackId: String,
    ) : CoordinatorRequest()

    /** Remove a track from favorites. */
    data class RemoveFromFavorites(
        val trackId: String,
    ) : CoordinatorRequest()

    /** Add a track to the queue. */
    data class AddToQueue(
        val trackId: String,
    ) : CoordinatorRequest()

    /** Add a track to a playlist. */
    data class AddToPlaylist(
        val trackId: String,
        val playlistId: String,
    ) : CoordinatorRequest()

    /** Remove a track from a collection. */
    data class RemoveFromCollection(
        val stableKey: String,
        val trackId: String,
    ) : CoordinatorRequest()

    // ----- SELECTION REQUESTS -----

    /** Enter selection mode. */
    data class EnterSelection(
        val anchorSectionId: String,
        val firstItemId: String,
    ) : CoordinatorRequest()

    /** Exit selection mode. */
    object ExitSelection : CoordinatorRequest()

    /** Perform a batch action on selected items. */
    data class PerformBatchAction(
        val selectedItemIds: Set<String>,
        val batchActionKey: String,
    ) : CoordinatorRequest()

    // ----- SYSTEM REQUESTS -----

    /** Request a permission. */
    data class RequestPermission(
        val permissionKey: String,
    ) : CoordinatorRequest()

    /** Open app settings. */
    object OpenAppSettings : CoordinatorRequest()

    /** Import music (open file picker). */
    object ImportMusic : CoordinatorRequest()

    /** Show a toast / snackbar. */
    data class ShowToast(
        val messageKey: String,
        val durationMs: Long,
    ) : CoordinatorRequest()

    /** Show a context menu. */
    data class ShowContextMenu(
        val sectionId: String,
        val itemId: String,
        val stableKey: String,
    ) : CoordinatorRequest()

    /** Dismiss the context menu. */
    object DismissContextMenu : CoordinatorRequest()

    /** Trigger haptic feedback. */
    data class TriggerHaptic(
        val hapticKey: String,
    ) : CoordinatorRequest()

    /** Announce a message for accessibility. */
    data class AnnounceForAccessibility(
        val messageKey: String,
    ) : CoordinatorRequest()

    /** No operation — used as a fallback. */
    object NoOp : CoordinatorRequest()
}
