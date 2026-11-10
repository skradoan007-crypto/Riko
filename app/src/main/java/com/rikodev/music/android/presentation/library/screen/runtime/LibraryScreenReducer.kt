/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.2 — Library Screen Runtime
 *
 * File 4 / 8 — LibraryScreenReducer.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [LibraryScreenReducer] is the *pure* function that transforms the
 * Library screen state in response to events.
 *
 * Signature:
 *
 *   (State, Event) → Pair<State, List<Action>>
 *
 * The reducer:
 *   • Returns a NEW state (never mutates the old one).
 *   • Returns a list of Action *requests* (never executes them).
 *   • NEVER calls the Engine, Audio Runtime, or any side effect.
 *   • NEVER predicts what the Engine will return.
 *
 * Rule #47 — Reducer Never Predicts.
 * The reducer does not guess what the Engine will do. It emits a
 * RefreshCollection action; the Coordinator executes it; the Engine
 * returns a result; the result arrives as an EngineRefreshCompleted
 * event; the reducer handles that event in a separate pass.
 *
 * ARCHITECTURE
 * ------------
 *
 *   Event         Reducer          State + Actions
 *    │              │                   │
 *    │ ───────────► │ ────────────────► │
 *    │              │                   │
 *    │              │                   ▼
 *    │              │              Coordinator
 *    │              │                   │
 *    │              │                   │ (executes actions,
 *    │              │                   │  calls Engine)
 *    │              │                   │
 *    │              │                   ▼
 *    │              │              Engine result
 *    │              │                   │
 *    │              │                   │ (as new Event)
 *    │ ◄────────────┼───────────────────┘
 *    │              │
 *    │ ───────────► │ (next pass)
 *
 * RULES ENFORCED HERE
 * -------------------
 * #31  Runtime Owns Truth        — reducer owns state transitions
 * #44  Stable Key Never Changes  — all keys are stable strings
 * #47  Reducer Never Predicts    — reducer never calls Engine
 * #48  Actions Describe Intent   — emits intent actions only
 * #55  Screen Owns Composition   — reducer is screen-level
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Compose, no Android framework, no Coroutine, no Flow, no
 * ViewModel, NO Engine calls, NO Audio Runtime calls. Pure function.
 *
 * © RIKO Experience OS — Runtime Tier
 */

package com.rikodev.music.android.presentation.library.screen.runtime

import com.rikodev.music.android.presentation.library.screen.contract.LibraryDockState
import com.rikodev.music.android.presentation.library.screen.contract.LibraryEmptyStateKind
import com.rikodev.music.android.presentation.library.screen.contract.LibraryGestureDirection
import com.rikodev.music.android.presentation.library.screen.contract.LibraryNavigationDestination
import com.rikodev.music.android.presentation.library.screen.contract.LibraryScreenOrientation
import com.rikodev.music.android.presentation.library.screen.contract.LibraryTopBarState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.filled.Pause

/**
 * THE LIBRARY SCREEN REDUCER.
 *
 * Pure function. No side effects. No Engine calls. No async.
 *
 * Usage:
 *   val (newState, actions) = LibraryScreenReducer.reduce(state, event)
 *   coordinator.execute(actions)  // host does this
 */
object LibraryScreenReducer {

    /**
     * Reduce an event into a new state + action requests.
     *
     * @property state   Current state (immutable).
     * @property event   Event to process.
     * @return           New state + list of action requests.
     */
    fun reduce(
        state: LibraryScreenState,
        event: LibraryScreenEvent,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        return when (event) {
            // ----- USER EVENTS -----
            is LibraryScreenEvent.CardTapped ->
                reduceCardTapped(state, event)
            is LibraryScreenEvent.CardLongPressed ->
                reduceCardLongPressed(state, event)
            is LibraryScreenEvent.CardDoubleTapped ->
                reduceCardDoubleTapped(state, event)
            is LibraryScreenEvent.CardSwiped ->
                reduceCardSwiped(state, event)
            is LibraryScreenEvent.SectionHeaderTapped ->
                reduceSectionHeaderTapped(state, event)
            is LibraryScreenEvent.SectionSeeAllTapped ->
                reduceSectionSeeAllTapped(state, event)
            is LibraryScreenEvent.ChipTapped ->
                reduceChipTapped(state, event)
            is LibraryScreenEvent.HeroTapped ->
                reduceHeroTapped(state, event)
            is LibraryScreenEvent.HeroDismissed ->
                reduceHeroDismissed(state, event)
            is LibraryScreenEvent.DockTapped ->
                reduceDockTapped(state, event)
            is LibraryScreenEvent.DockLongPressed ->
                state to emptyList() // TODO: implement reduceDockLongPressed
            is LibraryScreenEvent.DockPlayPauseTapped ->
                reduceDockPlayPauseTapped(state, event)
            is LibraryScreenEvent.DockNextTapped ->
                reduceDockNextTapped(state, event)
            is LibraryScreenEvent.DockPreviousTapped ->
                reduceDockPreviousTapped(state, event)

            // ----- SCROLL EVENTS -----
            is LibraryScreenEvent.ScrollChanged ->
                reduceScrollChanged(state, event)
            is LibraryScreenEvent.FastScrollStarted ->
                reduceFastScrollStarted(state, event)
            is LibraryScreenEvent.FastScrollEnded ->
                reduceFastScrollEnded(state, event)
            is LibraryScreenEvent.PullToRefreshStarted ->
                reducePullToRefreshStarted(state, event)
            is LibraryScreenEvent.PullToRefreshProgress ->
                reducePullToRefreshProgress(state, event)
            is LibraryScreenEvent.PullToRefreshReleased ->
                reducePullToRefreshReleased(state, event)

            // ----- SELECTION EVENTS -----
            is LibraryScreenEvent.SelectionEntered ->
                reduceSelectionEntered(state, event)
            is LibraryScreenEvent.SelectionToggled ->
                reduceSelectionToggled(state, event)
            is LibraryScreenEvent.SelectionSelectAllInSection ->
                reduceSelectionSelectAllInSection(state, event)
            is LibraryScreenEvent.SelectionCleared ->
                reduceSelectionCleared(state)
            is LibraryScreenEvent.SelectionExited ->
                reduceSelectionExited(state)

            // ----- SYSTEM EVENTS -----
            is LibraryScreenEvent.ConstitutionLoaded ->
                reduceConstitutionLoaded(state, event)
            is LibraryScreenEvent.OrientationChanged ->
                reduceOrientationChanged(state, event)
            is LibraryScreenEvent.ScreenShapeChanged ->
                reduceScreenShapeChanged(state, event)
            is LibraryScreenEvent.BackPressed ->
                reduceBackPressed(state)
            is LibraryScreenEvent.ScreenForegrounded ->
                reduceScreenForegrounded(state)
            is LibraryScreenEvent.ScreenBackgrounded ->
                reduceScreenBackgrounded(state)
            is LibraryScreenEvent.ConfigurationChanged ->
                reduceConfigurationChanged(state, event)

            // ----- ENGINE EVENTS -----
            is LibraryScreenEvent.EngineRefreshStarted ->
                reduceEngineRefreshStarted(state, event)
            is LibraryScreenEvent.EngineRefreshCompleted ->
                reduceEngineRefreshCompleted(state, event)
            is LibraryScreenEvent.EngineRefreshFailed ->
                reduceEngineRefreshFailed(state, event)
            is LibraryScreenEvent.SnapshotReceived ->
                reduceSnapshotReceived(state, event)
            is LibraryScreenEvent.EngineFatalError ->
                reduceEngineFatalError(state, event)
            is LibraryScreenEvent.PlaybackStateChanged ->
                reducePlaybackStateChanged(state, event)
            is LibraryScreenEvent.TrackChanged ->
                reduceTrackChanged(state, event)

            // ----- NAVIGATION EVENTS -----
            is LibraryScreenEvent.NavigateRequested ->
                reduceNavigateRequested(state, event)
            is LibraryScreenEvent.NavigationCompleted ->
                reduceNavigationCompleted(state, event)
            is LibraryScreenEvent.DeepLinkReceived ->
                reduceDeepLinkReceived(state, event)

            // ----- EMPTY STATE EVENTS -----
            is LibraryScreenEvent.EmptyStatePrimaryActionTapped ->
                reduceEmptyStatePrimaryTapped(state, event)
            is LibraryScreenEvent.EmptyStateSecondaryActionTapped ->
                reduceEmptyStateSecondaryTapped(state, event)
            else ->
                state to emptyList()
        }
    }

    // -----------------------------------------------------------------
    // USER EVENT HANDLERS
    // -----------------------------------------------------------------

    private fun reduceCardTapped(
        state: LibraryScreenState,
        event: LibraryScreenEvent.CardTapped,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        // In selection mode, tap toggles selection instead of opening.
        if (state.selection.isActive) {
            return reduceSelectionToggled(state, LibraryScreenEvent.SelectionToggled(event.itemId))
        }

        val actions = listOf(
            LibraryScreenAction.PlayTrack(
                trackId = event.itemId,
                queue = emptyList(), // Coordinator fills the queue
                sourceStableKey = event.stableKey,
            ),
            LibraryScreenAction.TriggerHaptic("haptic.tap"),
        )
        return state to actions
    }

    private fun reduceCardLongPressed(
        state: LibraryScreenState,
        event: LibraryScreenEvent.CardLongPressed,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        // If not in selection mode, enter it.
        if (!state.selection.isActive) {
            val enterAction = LibraryScreenAction.EnterSelection(
                anchorSectionId = event.sectionId,
                firstItemId = event.itemId,
            )
            val showMenuAction = LibraryScreenAction.ShowContextMenu(
                sectionId = event.sectionId,
                itemId = event.itemId,
                stableKey = event.stableKey,
            )
            val newState = state.copy(
                phase = if (state.phase == LibraryScreenPhase.READY)
                    LibraryScreenPhase.SELECTION_MODE else state.phase,
                selection = state.selection.copy(
                    isActive = true,
                    selectedItemIds = setOf(event.itemId),
                    anchorSectionId = event.sectionId,
                ),
            )
            return newState to listOf(enterAction, showMenuAction,
                LibraryScreenAction.TriggerHaptic("haptic.longPress"))
        }
        // Already in selection mode — show context menu for the item.
        return state to listOf(
            LibraryScreenAction.ShowContextMenu(
                sectionId = event.sectionId,
                itemId = event.itemId,
                stableKey = event.stableKey,
            ),
            LibraryScreenAction.TriggerHaptic("haptic.longPress"),
        )
    }

    private fun reduceCardDoubleTapped(
        state: LibraryScreenState,
        event: LibraryScreenEvent.CardDoubleTapped,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        // Double-tap toggles favorite.
        val action = LibraryScreenAction.AddToFavorites(event.itemId)
        return state to listOf(action, LibraryScreenAction.TriggerHaptic("haptic.doubleTap"))
    }

    private fun reduceCardSwiped(
        state: LibraryScreenState,
        event: LibraryScreenEvent.CardSwiped,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        val action = when (event.direction) {
            LibraryGestureDirection.LEFT ->
                LibraryScreenAction.RemoveFromCollection(event.stableKey, event.itemId)
            LibraryGestureDirection.RIGHT ->
                LibraryScreenAction.AddToQueue(event.itemId)
            else -> return state to emptyList()
        }
        return state to listOf(action, LibraryScreenAction.TriggerHaptic("haptic.swipe"))
    }

    private fun reduceSectionHeaderTapped(
        state: LibraryScreenState,
        event: LibraryScreenEvent.SectionHeaderTapped,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        // Tap on header toggles collapse (if collapsible).
        val newSections = state.sections.map { section ->
            if (section.sectionId == event.sectionId) {
                section.copy(isCollapsed = !section.isCollapsed)
            } else section
        }
        return state.copy(sections = newSections) to
            listOf(LibraryScreenAction.TriggerHaptic("haptic.tap"))
    }

    private fun reduceSectionSeeAllTapped(
        state: LibraryScreenState,
        event: LibraryScreenEvent.SectionSeeAllTapped,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        val action = LibraryScreenAction.NavigateTo(
            destination = LibraryNavigationDestination.NONE, // host resolves from sectionId
            parameters = mapOf("sectionId" to event.sectionId, "stableKey" to event.stableKey),
        )
        return state to listOf(action)
    }

    private fun reduceChipTapped(
        state: LibraryScreenState,
        event: LibraryScreenEvent.ChipTapped,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        val action = when (event.actionKey) {
            "SEARCH" -> LibraryScreenAction.NavigateTo(
                destination = LibraryNavigationDestination.SEARCH,
                parameters = emptyMap(),
            )
            "REFRESH" -> LibraryScreenAction.RefreshAllCollections(force = false)
            "SORT" -> LibraryScreenAction.ShowContextMenu(
                sectionId = "",
                itemId = "",
                stableKey = "sortMenu",
            )
            "FILTER" -> LibraryScreenAction.ShowContextMenu(
                sectionId = "",
                itemId = "",
                stableKey = "filterMenu",
            )
            else -> return state to emptyList()
        }
        return state to listOf(action, LibraryScreenAction.TriggerHaptic("haptic.tap"))
    }

    private fun reduceHeroTapped(
        state: LibraryScreenState,
        event: LibraryScreenEvent.HeroTapped,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        val action = LibraryScreenAction.PlayTrack(
            trackId = event.primaryItemId,
            queue = emptyList(),
            sourceStableKey = event.collectionStableKey,
        )
        return state to listOf(action, LibraryScreenAction.TriggerHaptic("haptic.tap"))
    }

    private fun reduceHeroDismissed(
        state: LibraryScreenState,
        event: LibraryScreenEvent.HeroDismissed,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        val newHero = state.hero.copy(
            isVisible = false,
            isDismissed = true,
        )
        return state.copy(hero = newHero) to emptyList()
    }

    private fun reduceDockTapped(
        state: LibraryScreenState,
        event: LibraryScreenEvent.DockTapped,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        if (event.trackId == null) return state to emptyList()
        val action = LibraryScreenAction.NavigateTo(
            destination = LibraryNavigationDestination.NOW_PLAYING,
            parameters = mapOf("trackId" to event.trackId),
        )
        return state to listOf(action)
    }

    private fun reduceDockLongPressed(
        state: LibraryScreenState,
        event: LibraryScreenEvent.DockTapped,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        if (event.trackId == null) return state to emptyList()
        val action = LibraryScreenAction.NavigateTo(
            destination = LibraryNavigationDestination.NOW_PLAYING,
            parameters = mapOf("trackId" to event.trackId),
        )
        return state to listOf(action, LibraryScreenAction.TriggerHaptic("haptic.longPress"))
    }

    private fun reduceDockPlayPauseTapped(
        state: LibraryScreenState,
        event: LibraryScreenEvent.DockPlayPauseTapped,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        return state to listOf(
            LibraryScreenAction.TogglePlayPause,
            LibraryScreenAction.TriggerHaptic("haptic.tap"),
        )
    }

    private fun reduceDockNextTapped(
        state: LibraryScreenState,
        event: LibraryScreenEvent.DockNextTapped,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        return state to listOf(
            LibraryScreenAction.SkipToNext,
            LibraryScreenAction.TriggerHaptic("haptic.tap"),
        )
    }

    private fun reduceDockPreviousTapped(
        state: LibraryScreenState,
        event: LibraryScreenEvent.DockPreviousTapped,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        return state to listOf(
            LibraryScreenAction.SkipToPrevious,
            LibraryScreenAction.TriggerHaptic("haptic.tap"),
        )
    }

    // -----------------------------------------------------------------
    // SCROLL EVENT HANDLERS
    // -----------------------------------------------------------------

    private fun reduceScrollChanged(
        state: LibraryScreenState,
        event: LibraryScreenEvent.ScrollChanged,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        val newScroll = state.scroll.copy(
            scrollOffsetDp = event.offsetDp,
            maxScrollOffsetDp = event.maxOffsetDp,
            isAtTop = event.isAtTop,
            isAtBottom = event.isAtBottom,
        )

        // Top bar collapse rule — collapse between 80 and 160 dp scroll.
        val newTopBarState = when {
            event.offsetDp < 40 -> LibraryTopBarState.EXPANDED
            event.offsetDp > 160 -> LibraryTopBarState.COMPACT
            else -> state.topBar.state
        }
        val newTopBar = state.topBar.copy(
            state = newTopBarState,
            scrollOffsetDp = event.offsetDp,
        )

        return state.copy(scroll = newScroll, topBar = newTopBar) to emptyList()
    }

    private fun reduceFastScrollStarted(
        state: LibraryScreenState,
        event: LibraryScreenEvent.FastScrollStarted,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        return state.copy(scroll = state.scroll.copy(isFastScrolling = true)) to emptyList()
    }

    private fun reduceFastScrollEnded(
        state: LibraryScreenState,
        event: LibraryScreenEvent.FastScrollEnded,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        return state.copy(scroll = state.scroll.copy(isFastScrolling = false)) to
            listOf(LibraryScreenAction.TriggerHaptic("haptic.fastScrollEnd"))
    }

    private fun reducePullToRefreshStarted(
        state: LibraryScreenState,
        event: LibraryScreenEvent.PullToRefreshStarted,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        return state.copy(scroll = state.scroll.copy(
            isPullingToRefresh = true,
            pullDistanceDp = event.initialPullDistanceDp,
        )) to emptyList()
    }

    private fun reducePullToRefreshProgress(
        state: LibraryScreenState,
        event: LibraryScreenEvent.PullToRefreshProgress,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        return state.copy(scroll = state.scroll.copy(
            pullDistanceDp = event.pullDistanceDp,
        )) to emptyList()
    }

    private fun reducePullToRefreshReleased(
        state: LibraryScreenState,
        event: LibraryScreenEvent.PullToRefreshReleased,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        val newScroll = state.scroll.copy(
            isPullingToRefresh = false,
            pullDistanceDp = 0,
        )
        val actions = if (event.triggered) {
            listOf(
                LibraryScreenAction.RefreshAllCollections(force = true),
                LibraryScreenAction.TriggerHaptic("haptic.refreshTrigger"),
            )
        } else {
            emptyList()
        }
        return state.copy(scroll = newScroll) to actions
    }

    // -----------------------------------------------------------------
    // SELECTION EVENT HANDLERS
    // -----------------------------------------------------------------

    private fun reduceSelectionEntered(
        state: LibraryScreenState,
        event: LibraryScreenEvent.SelectionEntered,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        val newSelection = state.selection.copy(
            isActive = true,
            selectedItemIds = setOf(event.firstItemId),
            anchorSectionId = event.anchorSectionId,
        )
        val newState = state.copy(
            phase = if (state.phase == LibraryScreenPhase.READY)
                LibraryScreenPhase.SELECTION_MODE else state.phase,
            selection = newSelection,
        )
        return newState to listOf(
            LibraryScreenAction.EnterSelection(event.anchorSectionId, event.firstItemId),
            LibraryScreenAction.TriggerHaptic("haptic.selectionEnter"),
        )
    }

    private fun reduceSelectionToggled(
        state: LibraryScreenState,
        event: LibraryScreenEvent.SelectionToggled,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        if (!state.selection.isActive) return state to emptyList()

        val newItems = if (event.itemId in state.selection.selectedItemIds) {
            state.selection.selectedItemIds - event.itemId
        } else {
            if (state.selection.isAtLimit) {
                return state to listOf(
                    LibraryScreenAction.ShowToast("selection.limitReached", 2000L),
                )
            }
            state.selection.selectedItemIds + event.itemId
        }
        val newSelection = state.selection.copy(selectedItemIds = newItems)
        return state.copy(selection = newSelection) to
            listOf(LibraryScreenAction.TriggerHaptic("haptic.selectionToggle"))
    }

    private fun reduceSelectionSelectAllInSection(
        state: LibraryScreenState,
        event: LibraryScreenEvent.SelectionSelectAllInSection,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        val section = state.sectionFor(event.sectionId) ?: return state to emptyList()
        // Coordinator will fill in the actual item IDs; reducer just signals intent.
        return state to listOf(LibraryScreenAction.PerformBatchAction(
            selectedItemIds = emptySet(),
            batchActionKey = "selectAll.${event.sectionId}",
        ))
    }

    private fun reduceSelectionCleared(
        state: LibraryScreenState,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        val newSelection = state.selection.copy(selectedItemIds = emptySet())
        return state.copy(selection = newSelection) to emptyList()
    }

    private fun reduceSelectionExited(
        state: LibraryScreenState,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        val newSelection = LibrarySelectionRuntimeState.INACTIVE
        val newPhase = if (state.phase == LibraryScreenPhase.SELECTION_MODE)
            LibraryScreenPhase.READY else state.phase
        return state.copy(
            phase = newPhase,
            selection = newSelection,
        ) to listOf(
            LibraryScreenAction.ExitSelection,
            LibraryScreenAction.TriggerHaptic("haptic.selectionExit"),
        )
    }

    // -----------------------------------------------------------------
    // SYSTEM EVENT HANDLERS
    // -----------------------------------------------------------------

    private fun reduceConstitutionLoaded(
        state: LibraryScreenState,
        event: LibraryScreenEvent.ConstitutionLoaded,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        return state.copy(phase = LibraryScreenPhase.LOADING) to
            listOf(LibraryScreenAction.RefreshAllCollections(force = false))
    }

    private fun reduceOrientationChanged(
        state: LibraryScreenState,
        event: LibraryScreenEvent.OrientationChanged,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        return state.copy(orientation = event.newOrientation) to emptyList()
    }

    private fun reduceScreenShapeChanged(
        state: LibraryScreenState,
        event: LibraryScreenEvent.ScreenShapeChanged,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        // Shape change may require re-rendering.
        return state to listOf(LibraryScreenAction.RequestSnapshot(stableKey = null))
    }

    private fun reduceBackPressed(
        state: LibraryScreenState,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        // Back first clears selection, then collapses dock, then navigates.
        return when {
            state.selection.isActive ->
                reduceSelectionExited(state)
            state.dock.state == LibraryDockState.EXPANDED ->
                state.copy(dock = state.dock.copy(state = LibraryDockState.COLLAPSED)) to emptyList()
            else ->
                state to listOf(LibraryScreenAction.NavigateBack)
        }
    }

    private fun reduceScreenForegrounded(
        state: LibraryScreenState,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        return state to listOf(LibraryScreenAction.RefreshAllCollections(force = false))
    }

    private fun reduceScreenBackgrounded(
        state: LibraryScreenState,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        // On background, exit selection mode.
        return if (state.selection.isActive) reduceSelectionExited(state)
        else state to emptyList()
    }

    private fun reduceConfigurationChanged(
        state: LibraryScreenState,
        event: LibraryScreenEvent.ConfigurationChanged,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        // Configuration change may require a new snapshot.
        return state to listOf(LibraryScreenAction.RequestSnapshot(stableKey = null))
    }

    // -----------------------------------------------------------------
    // ENGINE EVENT HANDLERS
    // -----------------------------------------------------------------

    private fun reduceEngineRefreshStarted(
        state: LibraryScreenState,
        event: LibraryScreenEvent.EngineRefreshStarted,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        val newSections = state.sections.map { section ->
            if (section.stableKey == event.stableKey) section.copy(isRefreshing = true)
            else section
        }
        return state.copy(sections = newSections) to emptyList()
    }

    private fun reduceEngineRefreshCompleted(
        state: LibraryScreenState,
        event: LibraryScreenEvent.EngineRefreshCompleted,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        val newSections = state.sections.map { section ->
            if (section.stableKey == event.stableKey) {
                section.copy(
                    isRefreshing = false,
                    itemCount = event.itemCount,
                    totalCount = maxOf(section.totalCount, event.itemCount),
                )
            } else section
        }
        val newPhase = when {
            newSections.all { !it.isRefreshing && it.itemCount > 0 } -> LibraryScreenPhase.READY
            newSections.any { it.itemCount > 0 } -> LibraryScreenPhase.PARTIAL_READY
            else -> state.phase
        }
        return state.copy(sections = newSections, phase = newPhase) to
            listOf(LibraryScreenAction.RequestSnapshot(stableKey = null))
    }

    private fun reduceEngineRefreshFailed(
        state: LibraryScreenState,
        event: LibraryScreenEvent.EngineRefreshFailed,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        val newSections = state.sections.map { section ->
            if (section.stableKey == event.stableKey) section.copy(isRefreshing = false)
            else section
        }
        return state.copy(sections = newSections) to listOf(
            LibraryScreenAction.ShowToast("refresh.failed", 3000L),
            LibraryScreenAction.RecordDiagnostic(
                diagnosticKey = "engine.refreshFailed",
                payload = mapOf("stableKey" to event.stableKey, "error" to event.errorMessage),
            ),
        )
    }

    private fun reduceSnapshotReceived(
        state: LibraryScreenState,
        event: LibraryScreenEvent.SnapshotReceived,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        // Snapshot received — just record diagnostics. UI will pick it up.
        return state to listOf(
            LibraryScreenAction.RecordDiagnostic(
                diagnosticKey = "snapshot.received",
                payload = mapOf(
                    "fingerprint" to event.snapshotFingerprint,
                    "sections" to event.sectionCount.toString(),
                    "items" to event.totalItemCount.toString(),
                ),
            ),
        )
    }

    private fun reduceEngineFatalError(
        state: LibraryScreenState,
        event: LibraryScreenEvent.EngineFatalError,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        return state.copy(
            phase = LibraryScreenPhase.ERROR,
            errorMessage = event.errorMessage,
        ) to listOf(
            LibraryScreenAction.RecordDiagnostic(
                diagnosticKey = "engine.fatalError",
                payload = mapOf("error" to event.errorMessage),
            ),
        )
    }

    private fun reducePlaybackStateChanged(
        state: LibraryScreenState,
        event: LibraryScreenEvent.PlaybackStateChanged,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        val newDock = if (event.trackId != null) {
            state.dock.copy(
                state = if (state.dock.state == LibraryDockState.HIDDEN)
                    LibraryDockState.COLLAPSED else state.dock.state,
                currentTrackId = event.trackId,
                isPlaying = event.isPlaying,
                progressFraction = event.progressFraction,
            )
        } else {
            state.dock.copy(state = LibraryDockState.HIDDEN)
        }
        return state.copy(dock = newDock) to emptyList()
    }

    private fun reduceTrackChanged(
        state: LibraryScreenState,
        event: LibraryScreenEvent.TrackChanged,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        val newDock = state.dock.copy(
            state = if (state.dock.state == LibraryDockState.HIDDEN)
                LibraryDockState.COLLAPSED else state.dock.state,
            currentTrackId = event.trackId,
            trackTitle = event.title,
            trackArtist = event.artist,
            artworkKey = event.artworkKey,
            accentColorHex = event.accentColorHex,
        )
        return state.copy(dock = newDock) to listOf(
            LibraryScreenAction.AnnounceForAccessibility(
                "nowPlaying.${event.trackId}",
            ),
        )
    }

    // -----------------------------------------------------------------
    // NAVIGATION EVENT HANDLERS
    // -----------------------------------------------------------------

    private fun reduceNavigateRequested(
        state: LibraryScreenState,
        event: LibraryScreenEvent.NavigateRequested,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        val newNav = state.navigation.copy(
            pendingDestination = event.destination,
            pendingTransitionKey = event.destination.routeKey,
            isTransitioning = true,
        )
        return state.copy(navigation = newNav) to listOf(
            LibraryScreenAction.NavigateTo(event.destination, event.parameters),
        )
    }

    private fun reduceNavigationCompleted(
        state: LibraryScreenState,
        event: LibraryScreenEvent.NavigationCompleted,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        val newNav = state.navigation.copy(
            pendingDestination = LibraryNavigationDestination.NONE,
            pendingTransitionKey = "",
            isTransitioning = false,
        )
        return state.copy(navigation = newNav) to emptyList()
    }

    private fun reduceDeepLinkReceived(
        state: LibraryScreenState,
        event: LibraryScreenEvent.DeepLinkReceived,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        return state to listOf(
            LibraryScreenAction.OpenDeepLink(event.deepLinkKey, event.parameters),
        )
    }

    // -----------------------------------------------------------------
    // EMPTY STATE EVENT HANDLERS
    // -----------------------------------------------------------------

    private fun reduceEmptyStatePrimaryTapped(
        state: LibraryScreenState,
        event: LibraryScreenEvent.EmptyStatePrimaryActionTapped,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        val action = when (event.actionKey) {
            "empty.import" -> LibraryScreenAction.ImportMusic
            "empty.grantPermission" -> LibraryScreenAction.RequestPermission(
                "android.permission.READ_MEDIA_AUDIO",
            )
            "empty.retry" -> LibraryScreenAction.RefreshAllCollections(force = true)
            "empty.openSettings" -> LibraryScreenAction.OpenAppSettings
            else -> return state to emptyList()
        }
        return state to listOf(action, LibraryScreenAction.TriggerHaptic("haptic.tap"))
    }

    private fun reduceEmptyStateSecondaryTapped(
        state: LibraryScreenState,
        event: LibraryScreenEvent.EmptyStateSecondaryActionTapped,
    ): Pair<LibraryScreenState, List<LibraryScreenAction>> {
        val action = when (event.actionKey) {
            "empty.grantPermission" -> LibraryScreenAction.RequestPermission(
                "android.permission.READ_MEDIA_AUDIO",
            )
            "empty.openSettings" -> LibraryScreenAction.OpenAppSettings
            "library.clearFilter" -> LibraryScreenAction.RecordDiagnostic(
                diagnosticKey = "filter.cleared",
                payload = emptyMap(),
            )
            else -> return state to emptyList()
        }
        return state to listOf(action, LibraryScreenAction.TriggerHaptic("haptic.tap"))
    }
}
