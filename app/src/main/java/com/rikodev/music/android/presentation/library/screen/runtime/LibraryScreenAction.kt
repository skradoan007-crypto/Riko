/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.2 — Library Screen Runtime
 *
 * File 3 / 8 — LibraryScreenAction.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [LibraryScreenAction] is the *sealed* vocabulary of side-effect
 * requests the reducer can emit. Actions describe *intent* — they
 * never execute anything themselves.
 *
 * THE PURE REDUCER PATTERN (Rule #47 — Reducer Never Predicts)
 * ------------------------------------------------------------
 * The reducer is a pure function:
 *
 *   (State, Event) → Pair<State, List<Action>>
 *
 * It returns a new state + a list of action *requests*. The
 * Coordinator (file 7) receives the actions and executes them —
 * calling the Engine, dispatching navigation, etc.
 *
 * This separation means:
 *   1. The reducer is unit-testable without the Engine.
 *   2. The Coordinator is the only place async work happens.
 *   3. The reducer never predicts what the Engine will return.
 *
 * Rule #48 — Actions Describe Intent.
 * Action names are *nouns*, not verbs. "RefreshCollection" not
 * "RefreshCollectionNow". The intent is clear; the timing is not.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #44  Stable Key Never Changes    — action keys are stable strings
 * #47  Reducer Never Predicts      — actions are intent, not execution
 * #48  Actions Describe Intent     — noun-style names
 * #55  Screen Owns Composition     — actions are screen-level
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Compose, no Android framework, no Coroutine, no Flow, no
 * ViewModel. Pure Kotlin data.
 *
 * © RIKO Experience OS — Runtime Tier
 */

package com.rikodev.music.android.presentation.library.screen.runtime

import com.rikodev.music.android.presentation.library.screen.contract.LibraryNavigationDestination
import androidx.compose.material.icons.filled.Pause
import androidx.compose.animation.core.Animatable

/**
 * The sealed vocabulary of Library screen actions.
 *
 * Actions are *side-effect requests*. The reducer emits them; the
 * Coordinator executes them. Actions never carry behavior — only
 * the data the Coordinator needs to perform the side effect.
 */
sealed class LibraryScreenAction {

    // -----------------------------------------------------------------
    // ENGINE ACTIONS — requests to the Collection Engine
    // -----------------------------------------------------------------

    /** Request the Engine to refresh a collection. */
    data class RefreshCollection(
        val stableKey: String,
        val force: Boolean,
    ) : LibraryScreenAction()

    /** Request the Engine to refresh all collections. */
    data class RefreshAllCollections(
        val force: Boolean,
    ) : LibraryScreenAction()

    /** Request the Engine to invalidate cache for a collection. */
    data class InvalidateCollection(
        val stableKey: String,
    ) : LibraryScreenAction()

    /** Request the Engine to produce a new render snapshot. */
    data class RequestSnapshot(
        val stableKey: String?,
    ) : LibraryScreenAction()

    // -----------------------------------------------------------------
    // NAVIGATION ACTIONS
    // -----------------------------------------------------------------

    /** Navigate to a destination. */
    data class NavigateTo(
        val destination: LibraryNavigationDestination,
        val parameters: Map<String, String>,
    ) : LibraryScreenAction()

    /** Navigate back. */
    object NavigateBack : LibraryScreenAction()

    /** Open a deep link. */
    data class OpenDeepLink(
        val deepLinkKey: String,
        val parameters: Map<String, String>,
    ) : LibraryScreenAction()

    // -----------------------------------------------------------------
    // PLAYBACK ACTIONS — requests to the Audio Runtime
    // -----------------------------------------------------------------

    /** Play a specific track. */
    data class PlayTrack(
        val trackId: String,
        val queue: List<String>,
        val sourceStableKey: String,
    ) : LibraryScreenAction()

    /** Toggle play/pause. */
    object TogglePlayPause : LibraryScreenAction()

    /** Skip to next track. */
    object SkipToNext : LibraryScreenAction()

    /** Skip to previous track. */
    object SkipToPrevious : LibraryScreenAction()

    /** Seek to a position. */
    data class SeekTo(
        val positionFraction: Float,
    ) : LibraryScreenAction()

    // -----------------------------------------------------------------
    // COLLECTION ACTIONS — modify collections
    // -----------------------------------------------------------------

    /** Add a track to favorites. */
    data class AddToFavorites(
        val trackId: String,
    ) : LibraryScreenAction()

    /** Remove a track from favorites. */
    data class RemoveFromFavorites(
        val trackId: String,
    ) : LibraryScreenAction()

    /** Add a track to the queue. */
    data class AddToQueue(
        val trackId: String,
    ) : LibraryScreenAction()

    /** Add a track to a playlist. */
    data class AddToPlaylist(
        val trackId: String,
        val playlistId: String,
    ) : LibraryScreenAction()

    /** Remove a track from a collection (e.g. Recently Played). */
    data class RemoveFromCollection(
        val stableKey: String,
        val trackId: String,
    ) : LibraryScreenAction()

    // -----------------------------------------------------------------
    // SELECTION ACTIONS
    // -----------------------------------------------------------------

    /** Enter selection mode. */
    data class EnterSelection(
        val anchorSectionId: String,
        val firstItemId: String,
    ) : LibraryScreenAction()

    /** Exit selection mode. */
    object ExitSelection : LibraryScreenAction()

    /** Perform a batch action on selected items. */
    data class PerformBatchAction(
        val selectedItemIds: Set<String>,
        val batchActionKey: String,
    ) : LibraryScreenAction()

    // -----------------------------------------------------------------
    // SYSTEM ACTIONS
    // -----------------------------------------------------------------

    /** Request permission (e.g. media access). */
    data class RequestPermission(
        val permissionKey: String,
    ) : LibraryScreenAction()

    /** Open app settings. */
    object OpenAppSettings : LibraryScreenAction()

    /** Import music (open file picker). */
    object ImportMusic : LibraryScreenAction()

    /** Show a toast / snackbar. */
    data class ShowToast(
        val messageKey: String,
        val durationMs: Long,
    ) : LibraryScreenAction()

    /** Show a context menu. */
    data class ShowContextMenu(
        val sectionId: String,
        val itemId: String,
        val stableKey: String,
    ) : LibraryScreenAction()

    /** Dismiss the context menu. */
    object DismissContextMenu : LibraryScreenAction()

    /** Trigger haptic feedback. */
    data class TriggerHaptic(
        val hapticKey: String,
    ) : LibraryScreenAction()

    /** Announce a screen-reader message. */
    data class AnnounceForAccessibility(
        val messageKey: String,
    ) : LibraryScreenAction()

    // -----------------------------------------------------------------
    // DIAGNOSTICS ACTIONS — observation only (Rule #53)
    // -----------------------------------------------------------------

    /** Record a diagnostic event. */
    data class RecordDiagnostic(
        val diagnosticKey: String,
        val payload: Map<String, String>,
    ) : LibraryScreenAction()
}

/**
 * The category of an action — for diagnostics grouping.
 *
 * Rule #53 — Diagnostics Never Affects Engine.
 */
enum class LibraryScreenActionCategory(
    internal val description: String,
) {
    /** Engine action. */
    ENGINE("Engine."),

    /** Navigation action. */
    NAVIGATION("Navigation."),

    /** Playback action. */
    PLAYBACK("Playback."),

    /** Collection action. */
    COLLECTION("Collection."),

    /** Selection action. */
    SELECTION("Selection."),

    /** System action. */
    SYSTEM("System."),

    /** Diagnostics action. */
    DIAGNOSTICS("Diagnostics."),
}

/**
 * Categorize an action — for diagnostics only.
 *
 * Rule #53 — Diagnostics Never Affects Engine.
 */
fun LibraryScreenAction.category(): LibraryScreenActionCategory = when (this) {
    is LibraryScreenAction.RefreshCollection,
    is LibraryScreenAction.RefreshAllCollections,
    is LibraryScreenAction.InvalidateCollection,
    is LibraryScreenAction.RequestSnapshot ->
        LibraryScreenActionCategory.ENGINE

    is LibraryScreenAction.NavigateTo,
    is LibraryScreenAction.NavigateBack,
    is LibraryScreenAction.OpenDeepLink ->
        LibraryScreenActionCategory.NAVIGATION

    is LibraryScreenAction.PlayTrack,
    is LibraryScreenAction.TogglePlayPause,
    is LibraryScreenAction.SkipToNext,
    is LibraryScreenAction.SkipToPrevious,
    is LibraryScreenAction.SeekTo ->
        LibraryScreenActionCategory.PLAYBACK

    is LibraryScreenAction.AddToFavorites,
    is LibraryScreenAction.RemoveFromFavorites,
    is LibraryScreenAction.AddToQueue,
    is LibraryScreenAction.AddToPlaylist,
    is LibraryScreenAction.RemoveFromCollection ->
        LibraryScreenActionCategory.COLLECTION

    is LibraryScreenAction.EnterSelection,
    is LibraryScreenAction.ExitSelection,
    is LibraryScreenAction.PerformBatchAction ->
        LibraryScreenActionCategory.SELECTION

    is LibraryScreenAction.RequestPermission,
    is LibraryScreenAction.OpenAppSettings,
    is LibraryScreenAction.ImportMusic,
    is LibraryScreenAction.ShowToast,
    is LibraryScreenAction.ShowContextMenu,
    is LibraryScreenAction.DismissContextMenu,
    is LibraryScreenAction.TriggerHaptic,
    is LibraryScreenAction.AnnounceForAccessibility ->
        LibraryScreenActionCategory.SYSTEM

    is LibraryScreenAction.RecordDiagnostic ->
        LibraryScreenActionCategory.DIAGNOSTICS
}

/**
 * True if the action requires the Coordinator to call the Engine.
 *
 * The Coordinator uses this to decide which actions to forward.
 */
fun LibraryScreenAction.requiresEngine(): Boolean = when (this) {
    is LibraryScreenAction.RefreshCollection,
    is LibraryScreenAction.RefreshAllCollections,
    is LibraryScreenAction.InvalidateCollection,
    is LibraryScreenAction.RequestSnapshot -> true
    else -> false
}

/**
 * True if the action requires the Coordinator to call the Audio Runtime.
 */
fun LibraryScreenAction.requiresAudioRuntime(): Boolean = when (this) {
    is LibraryScreenAction.PlayTrack,
    is LibraryScreenAction.TogglePlayPause,
    is LibraryScreenAction.SkipToNext,
    is LibraryScreenAction.SkipToPrevious,
    is LibraryScreenAction.SeekTo -> true
    else -> false
}

/**
 * True if the action requires the Coordinator to navigate.
 */
fun LibraryScreenAction.requiresNavigation(): Boolean = when (this) {
    is LibraryScreenAction.NavigateTo,
    is LibraryScreenAction.NavigateBack,
    is LibraryScreenAction.OpenDeepLink -> true
    else -> false
}
