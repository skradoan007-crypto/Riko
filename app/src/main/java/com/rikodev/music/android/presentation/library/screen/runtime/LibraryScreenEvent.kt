/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.2 — Library Screen Runtime
 *
 * File 2 / 8 — LibraryScreenEvent.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [LibraryScreenEvent] is the *sealed* vocabulary of everything that
 * can happen to the Library screen — user input, system signals,
 * and engine results.
 *
 * Events are *intent*-shaped. They describe *what happened*, never
 * *what to do*. The reducer decides what to do; events just inform.
 *
 * WHY SEALED
 * ----------
 * A sealed event vocabulary means the compiler enforces exhaustive
 * matching. Adding a new event forces the reducer to handle it —
 * no silent drops, no forgotten cases.
 *
 * EVENT CATEGORIES
 * ----------------
 *   • User events     — taps, long-presses, gestures
 *   • Scroll events   — scroll position changes, pull-to-refresh
 *   • Selection events — enter/exit selection, toggle item
 *   • System events   — phase changes, configuration changes
 *   • Engine events   — refresh completed, snapshot received
 *   • Navigation events — back pressed, deep link received
 *
 * RULES ENFORCED HERE
 * -------------------
 * #44  Stable Key Never Changes    — event IDs / keys are stable
 * #48  Actions Describe Intent     — events describe what happened
 * #55  Screen Owns Composition     — events are screen-level
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Compose, no Android framework, no Coroutine, no Flow, no
 * ViewModel. Pure Kotlin data.
 *
 * © RIKO Experience OS — Runtime Tier
 */

package com.rikodev.music.android.presentation.library.screen.runtime

import com.rikodev.music.android.presentation.library.screen.contract.LibraryGestureDirection
import com.rikodev.music.android.presentation.library.screen.contract.LibraryNavigationDestination
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.platform.LocalDensity

/**
 * The sealed vocabulary of Library screen events.
 *
 * Every event is a data class carrying the *minimum* information
 * the reducer needs to react. Events never carry behavior — only
 * data.
 */
sealed class LibraryScreenEvent {

    // -----------------------------------------------------------------
    // USER EVENTS — Taps, long-presses, gestures
    // -----------------------------------------------------------------

    /** User tapped a card. */
    data class CardTapped(
        val sectionId: String,
        val itemId: String,
        val stableKey: String,
    ) : LibraryScreenEvent()

    /** User long-pressed a card (opens context menu). */
    data class CardLongPressed(
        val sectionId: String,
        val itemId: String,
        val stableKey: String,
    ) : LibraryScreenEvent()

    /** User double-tapped a card (toggle favorite). */
    data class CardDoubleTapped(
        val sectionId: String,
        val itemId: String,
        val stableKey: String,
    ) : LibraryScreenEvent()

    /** User swiped a card. */
    data class CardSwiped(
        val sectionId: String,
        val itemId: String,
        val stableKey: String,
        val direction: LibraryGestureDirection,
    ) : LibraryScreenEvent()

    /** User tapped a section header. */
    data class SectionHeaderTapped(
        val sectionId: String,
        val stableKey: String,
        val actionKey: String?,
    ) : LibraryScreenEvent()

    /** User tapped the "See all" action on a section. */
    data class SectionSeeAllTapped(
        val sectionId: String,
        val stableKey: String,
    ) : LibraryScreenEvent()

    /** User tapped a chip in the top bar. */
    data class ChipTapped(
        val actionKey: String,
    ) : LibraryScreenEvent()

    /** User tapped the hero card. */
    data class HeroTapped(
        val collectionStableKey: String,
        val primaryItemId: String,
    ) : LibraryScreenEvent()

    /** User dismissed the hero. */
    data class HeroDismissed(
        val collectionStableKey: String,
    ) : LibraryScreenEvent()

    /** User tapped the Dock (collapsed). */
    data class DockTapped(
        val trackId: String?,
    ) : LibraryScreenEvent()

    /** User long-pressed the Dock (morph to Now Playing). */
    data class DockLongPressed(
        val trackId: String?,
    ) : LibraryScreenEvent()

    /** User tapped the play/pause button on the Dock. */
    data class DockPlayPauseTapped(
        val trackId: String?,
    ) : LibraryScreenEvent()

    /** User tapped the next button on the Dock. */
    data class DockNextTapped(
        val trackId: String?,
    ) : LibraryScreenEvent()

    /** User tapped the previous button on the Dock. */
    data class DockPreviousTapped(
        val trackId: String?,
    ) : LibraryScreenEvent()

    // -----------------------------------------------------------------
    // SCROLL EVENTS
    // -----------------------------------------------------------------

    /** Scroll position changed. */
    data class ScrollChanged(
        val offsetDp: Int,
        val maxOffsetDp: Int,
        val isAtTop: Boolean,
        val isAtBottom: Boolean,
    ) : LibraryScreenEvent()

    /** User started a fast-scroll gesture (thumb dragged). */
    data class FastScrollStarted(
        val targetSectionId: String?,
    ) : LibraryScreenEvent()

    /** User ended a fast-scroll gesture. */
    data class FastScrollEnded(
        val finalSectionId: String?,
    ) : LibraryScreenEvent()

    /** User started a pull-to-refresh gesture. */
    data class PullToRefreshStarted(
        val initialPullDistanceDp: Int,
    ) : LibraryScreenEvent()

    /** Pull-to-refresh distance changed. */
    data class PullToRefreshProgress(
        val pullDistanceDp: Int,
    ) : LibraryScreenEvent()

    /** Pull-to-refresh released — triggered if past threshold. */
    data class PullToRefreshReleased(
        val finalPullDistanceDp: Int,
        val triggered: Boolean,
    ) : LibraryScreenEvent()

    // -----------------------------------------------------------------
    // SELECTION EVENTS
    // -----------------------------------------------------------------

    /** User entered selection mode (long-press on a card). */
    data class SelectionEntered(
        val anchorSectionId: String,
        val firstItemId: String,
    ) : LibraryScreenEvent()

    /** User toggled selection of an item. */
    data class SelectionToggled(
        val itemId: String,
    ) : LibraryScreenEvent()

    /** User selected all items in a section. */
    data class SelectionSelectAllInSection(
        val sectionId: String,
    ) : LibraryScreenEvent()

    /** User cleared selection. */
    object SelectionCleared : LibraryScreenEvent()

    /** User exited selection mode. */
    object SelectionExited : LibraryScreenEvent()

    // -----------------------------------------------------------------
    // SYSTEM EVENTS
    // -----------------------------------------------------------------

    /** Constitution was loaded (cold start). */
    data class ConstitutionLoaded(
        val profileName: String,
    ) : LibraryScreenEvent()

    /** Screen orientation changed. */
    data class OrientationChanged(
        val newOrientation: com.rikodev.music.android.presentation.library.screen.contract.LibraryScreenOrientation,
    ) : LibraryScreenEvent()

    /** Screen shape changed (rotation, fold, etc.). */
    data class ScreenShapeChanged(
        val widthDp: Int,
        val heightDp: Int,
    ) : LibraryScreenEvent()

    /** User pressed the back button. */
    object BackPressed : LibraryScreenEvent()

    /** User opened the screen (foregrounded). */
    object ScreenForegrounded : LibraryScreenEvent()

    /** User backgrounded the screen. */
    object ScreenBackgrounded : LibraryScreenEvent()

    /** Configuration changed (theme, density, etc.). */
    data class ConfigurationChanged(
        val isDarkTheme: Boolean,
        val isReduceMotion: Boolean,
        val isHighContrast: Boolean,
    ) : LibraryScreenEvent()

    // -----------------------------------------------------------------
    // ENGINE EVENTS — results from the Collection Engine (via Coordinator)
    // -----------------------------------------------------------------

    /** Engine started producing a snapshot. */
    data class EngineRefreshStarted(
        val stableKey: String,
    ) : LibraryScreenEvent()

    /** Engine completed a refresh for a collection. */
    data class EngineRefreshCompleted(
        val stableKey: String,
        val itemCount: Int,
        val durationMs: Long,
    ) : LibraryScreenEvent()

    /** Engine failed to refresh a collection. */
    data class EngineRefreshFailed(
        val stableKey: String,
        val errorMessage: String,
    ) : LibraryScreenEvent()

    /** Engine produced a new render snapshot. */
    data class SnapshotReceived(
        val snapshotFingerprint: String,
        val sectionCount: Int,
        val totalItemCount: Int,
    ) : LibraryScreenEvent()

    /** Engine reported a fatal error. */
    data class EngineFatalError(
        val errorMessage: String,
    ) : LibraryScreenEvent()

    /** Playback state changed (from audio runtime). */
    data class PlaybackStateChanged(
        val trackId: String?,
        val isPlaying: Boolean,
        val progressFraction: Float,
    ) : LibraryScreenEvent()

    /** Track changed (from audio runtime). */
    data class TrackChanged(
        val trackId: String,
        val title: String,
        val artist: String,
        val artworkKey: String,
        val accentColorHex: String,
    ) : LibraryScreenEvent()

    // -----------------------------------------------------------------
    // NAVIGATION EVENTS
    // -----------------------------------------------------------------

    /** User requested navigation to a destination. */
    data class NavigateRequested(
        val destination: LibraryNavigationDestination,
        val parameters: Map<String, String>,
    ) : LibraryScreenEvent()

    /** Navigation completed (arrived at destination). */
    data class NavigationCompleted(
        val destination: LibraryNavigationDestination,
    ) : LibraryScreenEvent()

    /** Deep link received. */
    data class DeepLinkReceived(
        val deepLinkKey: String,
        val parameters: Map<String, String>,
    ) : LibraryScreenEvent()

    // -----------------------------------------------------------------
    // EMPTY STATE EVENTS
    // -----------------------------------------------------------------

    /** User tapped the primary action on an empty state. */
    data class EmptyStatePrimaryActionTapped(
        val actionKey: String,
    ) : LibraryScreenEvent()

    /** User tapped the secondary action on an empty state. */
    data class EmptyStateSecondaryActionTapped(
        val actionKey: String,
    ) : LibraryScreenEvent()
}

/**
 * The category of an event — for diagnostics grouping.
 *
 * Rule #53 — Diagnostics Never Affects Engine.
 * The reducer never branches on this; it's for the Inspector.
 */
enum class LibraryScreenEventCategory(
    internal val description: String,
) {
    /** User input event. */
    USER_INPUT("User input."),

    /** Scroll event. */
    SCROLL("Scroll."),

    /** Selection event. */
    SELECTION("Selection."),

    /** System event. */
    SYSTEM("System."),

    /** Engine result event. */
    ENGINE_RESULT("Engine result."),

    /** Navigation event. */
    NAVIGATION("Navigation."),

    /** Empty state event. */
    EMPTY_STATE("Empty state."),
}

/**
 * Categorize an event — for diagnostics only.
 *
 * Rule #53 — Diagnostics Never Affects Engine.
 * The reducer never calls this. It's for the Inspector overlay.
 */
fun LibraryScreenEvent.category(): LibraryScreenEventCategory = when (this) {
    is LibraryScreenEvent.CardTapped,
    is LibraryScreenEvent.CardLongPressed,
    is LibraryScreenEvent.CardDoubleTapped,
    is LibraryScreenEvent.CardSwiped,
    is LibraryScreenEvent.SectionHeaderTapped,
    is LibraryScreenEvent.SectionSeeAllTapped,
    is LibraryScreenEvent.ChipTapped,
    is LibraryScreenEvent.HeroTapped,
    is LibraryScreenEvent.HeroDismissed,
    is LibraryScreenEvent.DockTapped,
    is LibraryScreenEvent.DockLongPressed,
    is LibraryScreenEvent.DockPlayPauseTapped,
    is LibraryScreenEvent.DockNextTapped,
    is LibraryScreenEvent.DockPreviousTapped,
    is LibraryScreenEvent.EmptyStatePrimaryActionTapped,
    is LibraryScreenEvent.EmptyStateSecondaryActionTapped ->
        LibraryScreenEventCategory.USER_INPUT

    is LibraryScreenEvent.ScrollChanged,
    is LibraryScreenEvent.FastScrollStarted,
    is LibraryScreenEvent.FastScrollEnded,
    is LibraryScreenEvent.PullToRefreshStarted,
    is LibraryScreenEvent.PullToRefreshProgress,
    is LibraryScreenEvent.PullToRefreshReleased ->
        LibraryScreenEventCategory.SCROLL

    is LibraryScreenEvent.SelectionEntered,
    is LibraryScreenEvent.SelectionToggled,
    is LibraryScreenEvent.SelectionSelectAllInSection,
    is LibraryScreenEvent.SelectionCleared,
    is LibraryScreenEvent.SelectionExited ->
        LibraryScreenEventCategory.SELECTION

    is LibraryScreenEvent.ConstitutionLoaded,
    is LibraryScreenEvent.OrientationChanged,
    is LibraryScreenEvent.ScreenShapeChanged,
    is LibraryScreenEvent.BackPressed,
    is LibraryScreenEvent.ScreenForegrounded,
    is LibraryScreenEvent.ScreenBackgrounded,
    is LibraryScreenEvent.ConfigurationChanged ->
        LibraryScreenEventCategory.SYSTEM

    is LibraryScreenEvent.EngineRefreshStarted,
    is LibraryScreenEvent.EngineRefreshCompleted,
    is LibraryScreenEvent.EngineRefreshFailed,
    is LibraryScreenEvent.SnapshotReceived,
    is LibraryScreenEvent.EngineFatalError,
    is LibraryScreenEvent.PlaybackStateChanged,
    is LibraryScreenEvent.TrackChanged ->
        LibraryScreenEventCategory.ENGINE_RESULT

    is LibraryScreenEvent.NavigateRequested,
    is LibraryScreenEvent.NavigationCompleted,
    is LibraryScreenEvent.DeepLinkReceived ->
        LibraryScreenEventCategory.NAVIGATION
}
