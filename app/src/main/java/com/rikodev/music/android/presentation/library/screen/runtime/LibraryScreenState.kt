/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.2 — Library Screen Runtime
 *
 * File 1 / 8 — LibraryScreenState.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [LibraryScreenState] is the *live, immutable* state of the Library
 * screen. It is the runtime mirror of [LibraryConstitution] (B5.1) —
 * the constitution declares *shape*, the state carries *current
 * values*.
 *
 * Data flow:
 *
 *   LibraryConstitution          (B5.1 — frozen, immutable)
 *               │
 *               ▼
 *   LibraryScreenState           (B5.2 — this file — live, immutable)
 *               │
 *               ▼
 *   LibraryScreenReducer         (B5.2 — produces new state from events)
 *               │
 *               ▼
 *   LibraryScreenCoordinator     (B5.2 — orchestrates engine calls)
 *               │
 *               ▼
 *   LibraryRenderSnapshot        (B4 — UI's only input)
 *
 * RULES ENFORCED HERE
 * -------------------
 * #31  Runtime Owns Truth        — UI reads state, not constitution
 * #44  Stable Key Never Changes  — all keys are stable strings
 * #45  Contracts Must Be Serializable — state is serializable-shaped
 * #54  Screen Tells a Story      — state carries story-aware fields
 * #55  Screen Owns Composition   — state is screen-level, not component-level
 * #57  Every Screen Has One Hero — state has at most one hero
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Compose, no Android framework, no Coroutine, no Flow, no
 * ViewModel. Pure Kotlin data.
 *
 * © RIKO Experience OS — Runtime Tier
 */

package com.rikodev.music.android.presentation.library.screen.runtime

import com.rikodev.music.android.presentation.library.screen.contract.LibraryConstitution
import com.rikodev.music.android.presentation.library.screen.contract.LibraryConstitutionProfile
import com.rikodev.music.android.presentation.library.screen.contract.LibraryDockState
import com.rikodev.music.android.presentation.library.screen.contract.LibraryEmptyStateKind
import com.rikodev.music.android.presentation.library.screen.contract.LibraryNavigationDestination
import com.rikodev.music.android.presentation.library.screen.contract.LibraryScreenOrientation
import com.rikodev.music.android.presentation.library.screen.contract.LibraryTopBarState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.animation.core.Animatable

/**
 * The phase the Library Screen Runtime is currently in.
 *
 * Mirrors [com.rikodev.music.android.presentation.library.runtime.CollectionRuntimePhase]
 * but is screen-specific. Multiple collection-runtime phases may
 * collapse into one screen phase.
 */
enum class LibraryScreenPhase(
    internal val description: String,
    internal val isInteractive: Boolean,
) {
    /** Cold start — constitution loaded, no state hydrated yet. */
    COLD_START(
        description = "Cold start — no state hydrated.",
        isInteractive = false,
    ),

    /** Loading — engine is producing the first batch. */
    LOADING(
        description = "Loading — engine producing first batch.",
        isInteractive = false,
    ),

    /** Partial ready — some sections ready, others still loading. */
    PARTIAL_READY(
        description = "Partial ready — some sections ready.",
        isInteractive = true,
    ),

    /** Ready — all visible sections ready. */
    READY(
        description = "Ready — all sections ready.",
        isInteractive = true,
    ),

    /** Empty — Library has no content (no songs / no permission). */
    EMPTY(
        description = "Empty — no content.",
        isInteractive = true,
    ),

    /** Error — engine failed. */
    ERROR(
        description = "Error — engine failed.",
        isInteractive = true,
    ),

    /** Permission required — cannot access songs. */
    PERMISSION_REQUIRED(
        description = "Permission required.",
        isInteractive = true,
    ),

    /** Selection mode — user is multi-selecting items. */
    SELECTION_MODE(
        description = "Selection mode active.",
        isInteractive = true,
    ),
}

/**
 * Sub-state for the Top Bar.
 *
 * @property state             Current Top Bar state.
 * @property titleText         Current title text.
 * @property subtitleText      Current subtitle text (or null).
 * @property chipActionKeys    Ordered list of chip action keys (visible).
 * @property badgeCounts       Map of chip action key → badge count.
 * @property scrollOffsetDp    Current scroll offset (drives collapse).
 */
data class LibraryTopBarRuntimeState(
    val state: LibraryTopBarState,
    val titleText: String,
    val subtitleText: String?,
    val chipActionKeys: List<String>,
    val badgeCounts: Map<String, Int>,
    val scrollOffsetDp: Int,
) {
    companion object {
        fun initial(constitution: LibraryConstitution): LibraryTopBarRuntimeState {
            val title = constitution.topBar.title
            return LibraryTopBarRuntimeState(
                state = title.let { constitution.topBar.state },
                titleText = title.titleText,
                subtitleText = title.subtitleText,
                chipActionKeys = constitution.topBar.chips
                    .filter { it.isVisible }
                    .map { it.kind.name },
                badgeCounts = emptyMap(),
                scrollOffsetDp = 0,
            )
        }
    }

    /** True if the top bar is currently collapsed (scrolled). */
    val isCollapsed: Boolean get() = state == LibraryTopBarState.COMPACT

    /** True if the top bar is visible (not hidden). */
    val isVisible: Boolean get() = state != LibraryTopBarState.HIDDEN
}

/**
 * Sub-state for the Dock.
 *
 * @property state             Current Dock state.
 * @property currentTrackId    Stable ID of the currently-playing track.
 * @property trackTitle        Track title text.
 * @property trackArtist       Track artist text.
 * @property isPlaying         True if currently playing.
 * @property progressFraction  0.0–1.0. Playback progress.
 * @property artworkKey        Stable key for the artwork loader.
 * @property accentColorHex    Per-track accent color (hex).
 */
data class LibraryDockRuntimeState(
    val state: LibraryDockState,
    val currentTrackId: String?,
    val trackTitle: String,
    val trackArtist: String,
    val isPlaying: Boolean,
    val progressFraction: Float,
    val artworkKey: String,
    val accentColorHex: String,
) {
    init {
        require(progressFraction in 0f..1f) {
            "progressFraction must be in [0, 1], got $progressFraction"
        }
    }

    companion object {
        val EMPTY = LibraryDockRuntimeState(
            state = LibraryDockState.HIDDEN,
            currentTrackId = null,
            trackTitle = "",
            trackArtist = "",
            isPlaying = false,
            progressFraction = 0f,
            artworkKey = "",
            accentColorHex = "#FFFFFF",
        )
    }

    /** True if the Dock has a track loaded. */
    val hasTrack: Boolean get() = currentTrackId != null

    /** True if the Dock is visible. */
    val isVisible: Boolean get() = state != LibraryDockState.HIDDEN
}

/**
 * Sub-state for the Hero.
 *
 * Rule #57 — Every Screen Has One Hero.
 * There is at most ONE hero on the screen at any time.
 *
 * @property isVisible         True if the hero is visible.
 * @property collectionStableKey  Stable key of the hero's source collection.
 * @property primaryItemId     Stable ID of the primary hero card.
 * @property primaryTitle      Title text on the hero card.
 * @property primarySubtitle   Subtitle text on the hero card.
 * @property artworkKey        Stable key for the artwork loader.
 * @property accentColorHex    Hero accent color (hex).
 * @property isDismissed       True if the user has dismissed the hero.
 */
data class LibraryHeroRuntimeState(
    val isVisible: Boolean,
    val collectionStableKey: String,
    val primaryItemId: String,
    val primaryTitle: String,
    val primarySubtitle: String,
    val artworkKey: String,
    val accentColorHex: String,
    val isDismissed: Boolean,
) {
    companion object {
        val EMPTY = LibraryHeroRuntimeState(
            isVisible = false,
            collectionStableKey = "",
            primaryItemId = "",
            primaryTitle = "",
            primarySubtitle = "",
            artworkKey = "",
            accentColorHex = "#FFFFFF",
            isDismissed = false,
        )
    }
}

/**
 * Sub-state for a single section.
 *
 * Rule #55 — Screen Owns Composition.
 * A section does not know what comes before or after it. It only
 * knows its own state. Composition (order) is owned by the screen.
 *
 * @property sectionId              Stable identity. Rule #44.
 * @property stableKey              Bridge to collection stableKey.
 * @property isVisible              True if the section is visible.
 * @property isCollapsed            True if the user collapsed it.
 * @property isRefreshing           True if currently refreshing.
 * @property itemCount              Number of items currently shown.
 * @property totalCount             Total items available.
 * @property firstVisibleItemIndex  Index of first visible item.
 * @property lastVisibleItemIndex   Index of last visible item.
 */
data class LibrarySectionRuntimeState(
    val sectionId: String,
    val stableKey: String,
    val isVisible: Boolean,
    val isCollapsed: Boolean,
    val isRefreshing: Boolean,
    val itemCount: Int,
    val totalCount: Int,
    val firstVisibleItemIndex: Int,
    val lastVisibleItemIndex: Int,
) {
    /** True if the section is empty (no items). */
    val isEmpty: Boolean get() = itemCount == 0

    /** True if the section is currently visible and not collapsed. */
    val isRendered: Boolean get() = isVisible && !isCollapsed
}

/**
 * Sub-state for multi-selection.
 *
 * @property isActive           True if selection mode is active.
 * @property selectedItemIds    Set of selected item IDs. Rule #44.
 * @property selectionLimit     Max items selectable.
 * @property anchorSectionId    Section where selection started.
 */
data class LibrarySelectionRuntimeState(
    val isActive: Boolean,
    val selectedItemIds: Set<String>,
    val selectionLimit: Int,
    val anchorSectionId: String?,
) {
    companion object {
        val INACTIVE = LibrarySelectionRuntimeState(
            isActive = false,
            selectedItemIds = emptySet(),
            selectionLimit = 50,
            anchorSectionId = null,
        )
    }

    val selectedCount: Int get() = selectedItemIds.size
    val isAtLimit: Boolean get() = selectedItemIds.size >= selectionLimit
    val hasSelection: Boolean get() = selectedItemIds.isNotEmpty()
}

/**
 * Sub-state for scroll.
 *
 * @property scrollOffsetDp         Current scroll offset in dp.
 * @property maxScrollOffsetDp      Maximum scroll offset.
 * @property isAtTop                True if scrolled to top.
 * @property isAtBottom             True if scrolled to bottom.
 * @property isFastScrolling        True if fast-scrolling (thumb dragged).
 * @property isPullingToRefresh     True if pull-to-refresh gesture active.
 * @property pullDistanceDp         Current pull distance (dp).
 */
data class LibraryScrollRuntimeState(
    val scrollOffsetDp: Int,
    val maxScrollOffsetDp: Int,
    val isAtTop: Boolean,
    val isAtBottom: Boolean,
    val isFastScrolling: Boolean,
    val isPullingToRefresh: Boolean,
    val pullDistanceDp: Int,
) {
    companion object {
        val INITIAL = LibraryScrollRuntimeState(
            scrollOffsetDp = 0,
            maxScrollOffsetDp = 0,
            isAtTop = true,
            isAtBottom = false,
            isFastScrolling = false,
            isPullingToRefresh = false,
            pullDistanceDp = 0,
        )
    }

    /** Scroll progress from 0.0 (top) to 1.0 (bottom). */
    val scrollProgress: Float
        get() = if (maxScrollOffsetDp > 0) {
            (scrollOffsetDp.toFloat() / maxScrollOffsetDp).coerceIn(0f, 1f)
        } else {
            0f
        }
}

/**
 * Sub-state for navigation.
 *
 * @property pendingDestination   Destination the screen is navigating to.
 * @property pendingTransitionKey  Transition key for the pending navigation.
 * @property lastNavigationMs     Wall-clock time of last navigation.
 * @property isTransitioning      True if a transition is in progress.
 */
data class LibraryNavigationRuntimeState(
    val pendingDestination: LibraryNavigationDestination,
    val pendingTransitionKey: String,
    val lastNavigationMs: Long,
    val isTransitioning: Boolean,
) {
    companion object {
        val IDLE = LibraryNavigationRuntimeState(
            pendingDestination = LibraryNavigationDestination.NONE,
            pendingTransitionKey = "",
            lastNavigationMs = 0L,
            isTransitioning = false,
        )
    }
}

/**
 * Sub-state for empty state.
 *
 * @property kind                Empty state kind (drives copy + illustration).
 * @property isVisible           True if the empty state is visible.
 * @property replacesContent     True if it replaces the entire content slot.
 */
data class LibraryEmptyRuntimeState(
    val kind: LibraryEmptyStateKind,
    val isVisible: Boolean,
    val replacesContent: Boolean,
) {
    companion object {
        val HIDDEN = LibraryEmptyRuntimeState(
            kind = LibraryEmptyStateKind.FIRST_LAUNCH,
            isVisible = false,
            replacesContent = false,
        )
    }
}

/**
 * THE LIBRARY SCREEN STATE.
 *
 * This is the single source of truth for "what is the Library screen
 * currently showing and what state is it in". The UI never reads the
 * constitution or the collection runtime directly — only this state.
 *
 * Rule #31 — Runtime Owns Truth.
 * Rule #55 — Screen Owns Composition.
 *
 * @property phase              Current screen phase.
 * @property profile            Active constitution profile.
 * @property orientation        Current screen orientation.
 * @property topBar             Top Bar sub-state.
 * @property dock               Dock sub-state.
 * @property hero               Hero sub-state (at most one). Rule #57.
 * @property sections           Ordered list of section sub-states.
 * @property selection          Multi-selection sub-state.
 * @property scroll             Scroll sub-state.
 * @property navigation         Navigation sub-state.
 * @property emptyState         Empty state sub-state.
 * @property lastUpdatedMs      Wall-clock time (epoch ms) of last update.
 * @property errorMessage       Optional error message (if phase == ERROR).
 * @property activeCollectionStableKey  Stable key of the "active" collection
 *                                      (e.g. the one currently being viewed).
 */
data class LibraryScreenState(
    val phase: LibraryScreenPhase,
    val profile: LibraryConstitutionProfile,
    val orientation: LibraryScreenOrientation,
    val topBar: LibraryTopBarRuntimeState,
    val dock: LibraryDockRuntimeState,
    val hero: LibraryHeroRuntimeState,
    val sections: List<LibrarySectionRuntimeState>,
    val selection: LibrarySelectionRuntimeState,
    val scroll: LibraryScrollRuntimeState,
    val navigation: LibraryNavigationRuntimeState,
    val emptyState: LibraryEmptyRuntimeState,
    val lastUpdatedMs: Long,
    val errorMessage: String?,
    val activeCollectionStableKey: String?,
) {
    companion object {

        /**
         * Cold-start state — produced before any data has hydrated.
         */
        fun coldStart(constitution: LibraryConstitution): LibraryScreenState =
            LibraryScreenState(
                phase = LibraryScreenPhase.COLD_START,
                profile = constitution.profile,
                orientation = LibraryScreenOrientation.PORTRAIT,
                topBar = LibraryTopBarRuntimeState.initial(constitution),
                dock = LibraryDockRuntimeState.EMPTY,
                hero = LibraryHeroRuntimeState.EMPTY,
                sections = emptyList(),
                selection = LibrarySelectionRuntimeState.INACTIVE,
                scroll = LibraryScrollRuntimeState.INITIAL,
                navigation = LibraryNavigationRuntimeState.IDLE,
                emptyState = LibraryEmptyRuntimeState.HIDDEN,
                lastUpdatedMs = 0L,
                errorMessage = null,
                activeCollectionStableKey = null,
            )
    }

    /** True if the screen is in any loading-ish phase. */
    val isLoading: Boolean
        get() = phase == LibraryScreenPhase.COLD_START ||
            phase == LibraryScreenPhase.LOADING

    /** True if the screen is ready for interaction. */
    val isReady: Boolean
        get() = phase == LibraryScreenPhase.READY ||
            phase == LibraryScreenPhase.PARTIAL_READY

    /** True if the screen is in a blocked state. */
    val isBlocked: Boolean
        get() = phase == LibraryScreenPhase.ERROR ||
            phase == LibraryScreenPhase.EMPTY ||
            phase == LibraryScreenPhase.PERMISSION_REQUIRED

    /** True if selection mode is active. */
    val isInSelectionMode: Boolean
        get() = phase == LibraryScreenPhase.SELECTION_MODE ||
            selection.isActive

    /** Total visible sections. */
    val visibleSectionCount: Int
        get() = sections.count { it.isRendered }

    /** Total visible items across all sections. */
    val totalVisibleItems: Int
        get() = sections.filter { it.isRendered }.sumOf { it.itemCount }

    /** Look up a section by its sectionId. */
    fun sectionFor(sectionId: String): LibrarySectionRuntimeState? =
        sections.firstOrNull { it.sectionId == sectionId }

    /** Look up a section by its collection stableKey. */
    fun sectionForStableKey(stableKey: String): LibrarySectionRuntimeState? =
        sections.firstOrNull { it.stableKey == stableKey }

    /** True if any section is currently refreshing. */
    val hasAnyRefreshInProgress: Boolean
        get() = sections.any { it.isRefreshing }

    /** True if the hero is currently visible. Rule #57. */
    val hasHero: Boolean get() = hero.isVisible && !hero.isDismissed
}
