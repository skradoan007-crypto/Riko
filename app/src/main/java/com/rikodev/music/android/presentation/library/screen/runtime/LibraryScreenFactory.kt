/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.2 — Library Screen Runtime
 *
 * File 6 / 8 — LibraryScreenFactory.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [LibraryScreenFactory] produces initial / default / specialized
 * Library screen states. It is the only place where states are
 * *constructed from scratch* — the reducer only *transitions*
 * states, never creates them.
 *
 * Factories are useful for:
 *   • Cold start — initial state before any data
 *   • Empty states — different kinds of emptiness
 *   • Error states — different kinds of errors
 *   • Test fixtures — predictable states for unit tests
 *
 * RULES ENFORCED HERE
 * -------------------
 * #44  Stable Key Never Changes    — keys are stable from construction
 * #46  Runtime Never Creates Identity — factory uses contract-provided keys
 * #55  Screen Owns Composition     — factory composes the initial screen
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Compose, no Android framework, no Coroutine, no Flow, no
 * ViewModel. Pure Kotlin functions.
 *
 * © RIKO Experience OS — Runtime Tier
 */

package com.rikodev.music.android.presentation.library.screen.runtime

import com.rikodev.music.android.presentation.library.runtime.CollectionRuntimeState
import com.rikodev.music.android.presentation.library.screen.contract.LibraryConstitution
import com.rikodev.music.android.presentation.library.screen.contract.LibraryConstitutionProfile
import com.rikodev.music.android.presentation.library.screen.contract.LibraryDockState
import com.rikodev.music.android.presentation.library.screen.contract.LibraryEmptyStateKind
import com.rikodev.music.android.presentation.library.screen.contract.LibraryNavigationDestination
import com.rikodev.music.android.presentation.library.screen.contract.LibraryScreenOrientation
import com.rikodev.music.android.presentation.library.screen.contract.LibraryTopBarState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp

/**
 * THE LIBRARY SCREEN FACTORY.
 *
 * Pure functions — no side effects, no async. Constructs initial /
 * default / specialized states.
 */
object LibraryScreenFactory {

    /**
     * Cold-start state — produced before any data has hydrated.
     *
     * The screen shows a loading indicator; the top bar shows a
     * placeholder title; the dock is hidden; no sections.
     */
    fun coldStart(
        constitution: LibraryConstitution,
        orientation: LibraryScreenOrientation = LibraryScreenOrientation.PORTRAIT,
    ): LibraryScreenState = LibraryScreenState(
        phase = LibraryScreenPhase.COLD_START,
        profile = constitution.profile,
        orientation = orientation,
        topBar = LibraryTopBarRuntimeState(
            state = constitution.topBar.state,
            titleText = constitution.topBar.title.titleText,
            subtitleText = constitution.topBar.title.subtitleText,
            chipActionKeys = constitution.topBar.chips
                .filter { it.isVisible }
                .map { it.kind.name },
            badgeCounts = emptyMap(),
            scrollOffsetDp = 0,
        ),
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

    /**
     * Loading state — produced when the engine starts producing
     * the first batch of collections.
     */
    fun loading(
        previousState: LibraryScreenState,
        nowMs: Long,
    ): LibraryScreenState = previousState.copy(
        phase = LibraryScreenPhase.LOADING,
        lastUpdatedMs = nowMs,
    )

    /**
     * Ready state — produced when all visible sections are ready.
     */
    fun ready(
        previousState: LibraryScreenState,
        nowMs: Long,
    ): LibraryScreenState = previousState.copy(
        phase = LibraryScreenPhase.READY,
        lastUpdatedMs = nowMs,
    )

    /**
     * Empty state — produced when the library has no content.
     */
    fun empty(
        previousState: LibraryScreenState,
        kind: LibraryEmptyStateKind,
        nowMs: Long,
    ): LibraryScreenState = previousState.copy(
        phase = LibraryScreenPhase.EMPTY,
        emptyState = LibraryEmptyRuntimeState(
            kind = kind,
            isVisible = true,
            replacesContent = true,
        ),
        lastUpdatedMs = nowMs,
        sections = emptyList(),
        hero = LibraryHeroRuntimeState.EMPTY,
    )

    /**
     * Error state — produced when the engine fails fatally.
     */
    fun error(
        previousState: LibraryScreenState,
        errorMessage: String,
        nowMs: Long,
    ): LibraryScreenState = previousState.copy(
        phase = LibraryScreenPhase.ERROR,
        errorMessage = errorMessage,
        lastUpdatedMs = nowMs,
    )

    /**
     * Permission-required state — produced when media access is missing.
     */
    fun permissionRequired(
        previousState: LibraryScreenState,
        nowMs: Long,
    ): LibraryScreenState = previousState.copy(
        phase = LibraryScreenPhase.PERMISSION_REQUIRED,
        emptyState = LibraryEmptyRuntimeState(
            kind = LibraryEmptyStateKind.PERMISSION_DENIED,
            isVisible = true,
            replacesContent = true,
        ),
        lastUpdatedMs = nowMs,
        sections = emptyList(),
        hero = LibraryHeroRuntimeState.EMPTY,
    )

    /**
     * Selection-mode state — produced when the user enters selection.
     */
    fun selectionMode(
        previousState: LibraryScreenState,
        anchorSectionId: String,
        firstItemId: String,
        nowMs: Long,
    ): LibraryScreenState = previousState.copy(
        phase = LibraryScreenPhase.SELECTION_MODE,
        selection = LibrarySelectionRuntimeState(
            isActive = true,
            selectedItemIds = setOf(firstItemId),
            selectionLimit = 50,
            anchorSectionId = anchorSectionId,
        ),
        lastUpdatedMs = nowMs,
    )

    /**
     * Map a profile to a constitution and produce the initial state.
     */
    fun forProfile(
        profile: LibraryConstitutionProfile,
        orientation: LibraryScreenOrientation = LibraryScreenOrientation.PORTRAIT,
    ): LibraryScreenState {
        val constitution = LibraryConstitution.forProfile(profile)
        return coldStart(constitution, orientation)
    }

    /**
     * Build a state from the collection runtime + playback.
     *
     * This is the *initial mapping* — used when the screen first
     * receives data from the engine. Subsequent updates go through
     * the mapper as well, but with `previousState` set.
     */
    fun fromRuntime(
        constitution: LibraryConstitution,
        collectionRuntime: CollectionRuntimeState,
        playback: PlaybackSnapshot?,
        nowMs: Long,
    ): LibraryScreenState = LibraryScreenMapper.map(
        constitution = constitution,
        collectionRuntime = collectionRuntime,
        playbackState = playback,
        previousState = null,
        nowMs = nowMs,
    )

    /**
     * Update an existing state from the collection runtime + playback.
     *
     * Used when the runtime emits a new state and the screen state
     * needs to be re-mapped while preserving user interactions
     * (scroll position, selection, etc.).
     */
    fun updateFromRuntime(
        previousState: LibraryScreenState,
        constitution: LibraryConstitution,
        collectionRuntime: CollectionRuntimeState,
        playback: PlaybackSnapshot?,
        nowMs: Long,
    ): LibraryScreenState = LibraryScreenMapper.map(
        constitution = constitution,
        collectionRuntime = collectionRuntime,
        playbackState = playback,
        previousState = previousState,
        nowMs = nowMs,
    )

    // -----------------------------------------------------------------
    // TEST FIXTURES
    // -----------------------------------------------------------------

    /**
     * A minimal "ready" state for unit tests.
     *
     * Has one section with three items, dock with a track, hero visible.
     */
    fun testFixtureReady(
        nowMs: Long = 1000L,
    ): LibraryScreenState {
        val constitution = LibraryConstitution.DEFAULT
        return LibraryScreenState(
            phase = LibraryScreenPhase.READY,
            profile = constitution.profile,
            orientation = LibraryScreenOrientation.PORTRAIT,
            topBar = LibraryTopBarRuntimeState(
                state = LibraryTopBarState.EXPANDED,
                titleText = "Library",
                subtitleText = null,
                chipActionKeys = listOf("SEARCH", "REFRESH", "SORT"),
                badgeCounts = emptyMap(),
                scrollOffsetDp = 0,
            ),
            dock = LibraryDockRuntimeState(
                state = LibraryDockState.COLLAPSED,
                currentTrackId = "track-1",
                trackTitle = "Test Track",
                trackArtist = "Test Artist",
                isPlaying = true,
                progressFraction = 0.3f,
                artworkKey = "track-1",
                accentColorHex = "#FF8A65",
            ),
            hero = LibraryHeroRuntimeState(
                isVisible = true,
                collectionStableKey = "continue_listening",
                primaryItemId = "track-1",
                primaryTitle = "Continue Listening",
                primarySubtitle = "Pick up where you left off",
                artworkKey = "track-1",
                accentColorHex = "#FF8A65",
                isDismissed = false,
            ),
            sections = listOf(
                LibrarySectionRuntimeState(
                    sectionId = "section.hero",
                    stableKey = "continue_listening",
                    isVisible = true,
                    isCollapsed = false,
                    isRefreshing = false,
                    itemCount = 5,
                    totalCount = 12,
                    firstVisibleItemIndex = 0,
                    lastVisibleItemIndex = 4,
                ),
                LibrarySectionRuntimeState(
                    sectionId = "section.smartCollections",
                    stableKey = "hidden_gems",
                    isVisible = true,
                    isCollapsed = false,
                    isRefreshing = false,
                    itemCount = 8,
                    totalCount = 30,
                    firstVisibleItemIndex = 0,
                    lastVisibleItemIndex = 7,
                ),
            ),
            selection = LibrarySelectionRuntimeState.INACTIVE,
            scroll = LibraryScrollRuntimeState.INITIAL,
            navigation = LibraryNavigationRuntimeState.IDLE,
            emptyState = LibraryEmptyRuntimeState.HIDDEN,
            lastUpdatedMs = nowMs,
            errorMessage = null,
            activeCollectionStableKey = "continue_listening",
        )
    }

    /**
     * A minimal "empty" state for unit tests.
     */
    fun testFixtureEmpty(
        kind: LibraryEmptyStateKind = LibraryEmptyStateKind.FIRST_LAUNCH,
        nowMs: Long = 1000L,
    ): LibraryScreenState {
        val base = coldStart(LibraryConstitution.DEFAULT)
        return base.copy(
            phase = LibraryScreenPhase.EMPTY,
            emptyState = LibraryEmptyRuntimeState(
                kind = kind,
                isVisible = true,
                replacesContent = true,
            ),
            lastUpdatedMs = nowMs,
        )
    }

    /**
     * A minimal "error" state for unit tests.
     */
    fun testFixtureError(
        errorMessage: String = "Test error",
        nowMs: Long = 1000L,
    ): LibraryScreenState {
        val base = coldStart(LibraryConstitution.DEFAULT)
        return base.copy(
            phase = LibraryScreenPhase.ERROR,
            errorMessage = errorMessage,
            lastUpdatedMs = nowMs,
        )
    }
}
