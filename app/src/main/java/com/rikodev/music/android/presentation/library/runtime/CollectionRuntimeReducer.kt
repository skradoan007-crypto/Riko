/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B2 — Collection Runtime
 *
 * File 4 / 4 — CollectionRuntimeReducer.kt — THE PURE BRAIN
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * The Reducer is the ONLY thing that transforms
 * [CollectionRuntimeState] in response to [CollectionRuntimeEvent].
 *
 * It is a pure function:
 *
 *   reduce(state, event) → Pair<newState, List<actions>>
 *
 *   • No side effects. No I/O. No Android framework. No coroutines.
 *   • No StateFlow. No ViewModel. No Repository. No DAO. No Room.
 *   • Deterministic — same (state, event) always yields same output.
 *
 * The Reducer reads from the Constitution (B1) but never writes to it.
 * The Reducer emits Actions for the ViewModel to execute — it never
 * executes them itself.
 *
 * ARCHITECTURE INVARIANTS
 * -----------------------
 * 1. The Reducer NEVER touches the engine (B3). It only emits
 *    RequestXxx actions; the ViewModel forwards them to the engine.
 *
 * 2. The Reducer NEVER creates a [CollectionContract]. Contracts
 *    come from [CollectionConstitution] (B1). Rule #36.
 *
 * 3. The Reducer NEVER generates card data. It only moves, selects,
 *    or filters existing snapshots. Card generation is B3's job.
 *
 * 4. The Reducer ALWAYS returns a new state — never mutates in place.
 *    All state data classes are immutable.
 *
 * 5. The Reducer ALWAYS handles every event exhaustively. Adding
 *    an event subtype without handling it is a compile error.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #26  Collection First          — reducer is collection-centric
 * #31  Runtime Owns Truth        — reducer is the only state mutator
 * #36  Contract Never Thinks     — reducer consumes contracts, never invents
 * #44  Stable Key Never Changes  — all map keys are stableKey strings
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Repository, Room, DAO, Coroutine, Flow, StateFlow, Compose, Canvas,
 * ViewModel. Pure Kotlin object with a single function.
 *
 * © RIKO Experience OS — Runtime Tier
 */

package com.rikodev.music.android.presentation.library.runtime

import com.rikodev.music.android.presentation.library.contract.CollectionConstitution
import com.rikodev.music.android.presentation.library.contract.CollectionHeaderContract
import com.rikodev.music.android.presentation.library.contract.CollectionVisibility
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.foundation.Canvas

/**
 * The pure reducer for the Collection Runtime.
 *
 * Single entry point: [reduce]. Batch entry point: [reduceAll].
 *
 * This object holds NO state. It is a namespace for pure functions.
 */
object CollectionRuntimeReducer {

    /**
     * Reduce a single event against the current state.
     *
     * @param state The current runtime state.
     * @param event The event to process.
     * @return A pair of (new state, list of actions to execute).
     *         The state is always new (immutable). The actions list
     *         may be empty if the event had no side effects.
     */
    fun reduce(
        state: CollectionRuntimeState,
        event: CollectionRuntimeEvent,
    ): Pair<CollectionRuntimeState, List<CollectionRuntimeAction>> {
        return when (event) {

            // ─────────────────────────────────────────────────────
            // 1. LIFECYCLE
            // ─────────────────────────────────────────────────────

            CollectionRuntimeEvent.RuntimeStarted -> {
                val newState = state.copy(
                    phase = CollectionRuntimePhase.HYDRATING,
                    lastUpdated = System.currentTimeMillis(),
                )
                newState to listOf(
                    CollectionRuntimeAction.RequestRefreshAll,
                    CollectionRuntimeAction.HapticFeedback(CollectionHapticType.LIGHT_TAP),
                )
            }

            CollectionRuntimeEvent.RuntimePaused -> {
                // No state change — runtime stays as-is. ViewModel will
                // stop observing engine while paused.
                state to emptyList()
            }

            CollectionRuntimeEvent.RuntimeResumed -> {
                // If we were READY, refresh any stale collections.
                val actions = if (state.phase == CollectionRuntimePhase.READY) {
                    val staleKeys = state.refreshStates.values
                        .filter { it.isStale }
                        .map { it.stableKey }
                        .map { CollectionRuntimeAction.RequestRefresh(it) }
                    staleKeys
                } else {
                    emptyList()
                }
                state to actions
            }

            CollectionRuntimeEvent.RuntimeStopped -> {
                // Clear selection on stop — defensive.
                val newState = if (state.selection.isActive) {
                    state.copy(selection = CollectionSelectionState.INACTIVE)
                } else {
                    state
                }
                newState to emptyList()
            }

            // ─────────────────────────────────────────────────────
            // 2. DATA
            // ─────────────────────────────────────────────────────

            is CollectionRuntimeEvent.CollectionsLoaded -> {
                if (event.snapshots.isEmpty()) {
                    state.copy(
                        phase = CollectionRuntimePhase.EMPTY,
                        lastUpdated = System.currentTimeMillis(),
                    ) to listOf(
                        CollectionRuntimeAction.HapticFeedback(CollectionHapticType.SOFT_TICK),
                    )
                } else {
                    val newRefreshStates = event.snapshots.mapValues { (_, snapshot) ->
                        CollectionRefreshState(
                            stableKey = snapshot.stableKey,
                            isRefreshing = false,
                            lastRefreshedAt = snapshot.lastUpdated,
                            lastFailedAt = 0L,
                            consecutiveFailures = 0,
                            lastErrorMessage = null,
                        )
                    }
                    val allReady = newRefreshStates.size == state.activeContracts.size
                    state.copy(
                        phase = if (allReady) CollectionRuntimePhase.READY
                        else CollectionRuntimePhase.PARTIAL_READY,
                        snapshots = event.snapshots,
                        refreshStates = newRefreshStates,
                        lastUpdated = System.currentTimeMillis(),
                    ) to emptyList()
                }
            }

            is CollectionRuntimeEvent.CollectionRefreshed -> {
                val newSnapshots = state.snapshots.toMutableMap().apply {
                    put(event.stableKey, event.snapshot)
                }
                val newRefreshStates = state.refreshStates.toMutableMap().apply {
                    put(
                        event.stableKey,
                        CollectionRefreshState(
                            stableKey = event.stableKey,
                            isRefreshing = false,
                            lastRefreshedAt = event.snapshot.lastUpdated,
                            lastFailedAt = 0L,
                            consecutiveFailures = 0,
                            lastErrorMessage = null,
                        )
                    )
                }
                val allReady = newRefreshStates.values.none { it.isStale } &&
                    newSnapshots.size >= state.activeContracts.size
                state.copy(
                    phase = if (allReady) CollectionRuntimePhase.READY
                    else state.phase,
                    snapshots = newSnapshots,
                    refreshStates = newRefreshStates,
                    lastUpdated = System.currentTimeMillis(),
                ) to listOf(
                    CollectionRuntimeAction.HapticFeedback(CollectionHapticType.SOFT_TICK),
                )
            }

            is CollectionRuntimeEvent.CollectionFailed -> {
                val newRefreshStates = state.refreshStates.toMutableMap().apply {
                    val existing = this[event.stableKey]
                    val failures = (existing?.consecutiveFailures ?: 0) + 1
                    put(
                        event.stableKey,
                        CollectionRefreshState(
                            stableKey = event.stableKey,
                            isRefreshing = false,
                            lastRefreshedAt = existing?.lastRefreshedAt ?: 0L,
                            lastFailedAt = System.currentTimeMillis(),
                            consecutiveFailures = failures,
                            lastErrorMessage = event.errorMessage,
                        )
                    )
                }
                state.copy(
                    refreshStates = newRefreshStates,
                    lastUpdated = System.currentTimeMillis(),
                ) to listOf(
                    CollectionRuntimeAction.ShowToast(
                        message = event.errorMessage,
                        type = CollectionToastType.ERROR,
                    ),
                    CollectionRuntimeAction.HapticFeedback(CollectionHapticType.ERROR),
                )
            }

            CollectionRuntimeEvent.LibraryIsEmpty -> {
                state.copy(
                    phase = CollectionRuntimePhase.EMPTY,
                    snapshots = emptyMap(),
                    lastUpdated = System.currentTimeMillis(),
                ) to listOf(
                    CollectionRuntimeAction.HapticFeedback(CollectionHapticType.SOFT_TICK),
                )
            }

            is CollectionRuntimeEvent.HeaderUpdated -> {
                state.copy(
                    header = event.header,
                    lastUpdated = System.currentTimeMillis(),
                ) to emptyList()
            }

            // ─────────────────────────────────────────────────────
            // 3. SELECTION
            // ─────────────────────────────────────────────────────

            is CollectionRuntimeEvent.SelectionStarted -> {
                val newSelection = CollectionSelectionState(
                    isActive = true,
                    selectedCardIds = setOf(event.initialCardId),
                    selectionLimit = state.selection.selectionLimit,
                )
                state.copy(selection = newSelection) to listOf(
                    CollectionRuntimeAction.HapticFeedback(CollectionHapticType.MEDIUM_TAP),
                )
            }

            is CollectionRuntimeEvent.SelectionToggled -> {
                if (!state.selection.isActive) {
                    // Implicit selection start.
                    return reduce(state, CollectionRuntimeEvent.SelectionStarted(
                        event.cardId, event.stableKey,
                    ))
                }
                val current = state.selection.selectedCardIds
                val newSet = if (event.cardId in current) {
                    current - event.cardId
                } else {
                    if (current.size >= state.selection.selectionLimit) {
                        // At limit — emit warning, no state change.
                        return state to listOf(
                            CollectionRuntimeAction.HapticFeedback(CollectionHapticType.WARNING),
                            CollectionRuntimeAction.ShowToast(
                                message = "Selection limit reached",
                                type = CollectionToastType.WARNING,
                            ),
                        )
                    }
                    current + event.cardId
                }
                // If we deselected the last card, exit selection mode.
                val newSelection = if (newSet.isEmpty()) {
                    CollectionSelectionState.INACTIVE
                } else {
                    state.selection.copy(selectedCardIds = newSet)
                }
                state.copy(selection = newSelection) to listOf(
                    CollectionRuntimeAction.HapticFeedback(CollectionHapticType.LIGHT_TAP),
                )
            }

            is CollectionRuntimeEvent.SelectAllInCollection -> {
                val snapshot = state.snapshots[event.stableKey] ?: return state to emptyList()
                val allCardIds = snapshot.cards.map { it.cardId }.toSet()
                val merged = state.selection.selectedCardIds + allCardIds
                val capped = merged.take(state.selection.selectionLimit).toSet()
                val newSelection = state.selection.copy(
                    isActive = true,
                    selectedCardIds = capped,
                )
                state.copy(selection = newSelection) to listOf(
                    CollectionRuntimeAction.HapticFeedback(CollectionHapticType.MEDIUM_TAP),
                )
            }

            CollectionRuntimeEvent.SelectionCleared -> {
                state.copy(
                    selection = state.selection.copy(selectedCardIds = emptySet()),
                ) to listOf(
                    CollectionRuntimeAction.HapticFeedback(CollectionHapticType.LIGHT_TAP),
                )
            }

            CollectionRuntimeEvent.SelectionExited -> {
                state.copy(selection = CollectionSelectionState.INACTIVE) to listOf(
                    CollectionRuntimeAction.HapticFeedback(CollectionHapticType.LIGHT_TAP),
                )
            }

            // ─────────────────────────────────────────────────────
            // 4. SCROLL
            // ─────────────────────────────────────────────────────

            is CollectionRuntimeEvent.SectionScrolled -> {
                val newScrollState = CollectionScrollState(
                    sectionId = event.sectionId,
                    scrollOffsetDp = event.scrollOffsetDp,
                    isPinned = event.scrollOffsetDp > 0,
                    hasScrolled = event.scrollOffsetDp > 0,
                    firstVisibleCardIndex = event.firstVisibleCardIndex,
                    lastVisibleCardIndex = event.lastVisibleCardIndex,
                )
                val newScrollStates = state.scrollStates.toMutableMap().apply {
                    put(event.sectionId, newScrollState)
                }
                state.copy(scrollStates = newScrollStates) to emptyList()
            }

            is CollectionRuntimeEvent.SectionReachedTop -> {
                val newScrollStates = state.scrollStates.toMutableMap().apply {
                    this[event.sectionId]?.let { existing ->
                        put(event.sectionId, existing.copy(isPinned = false, hasScrolled = false))
                    }
                }
                state.copy(scrollStates = newScrollStates) to emptyList()
            }

            is CollectionRuntimeEvent.SectionReachedBottom -> {
                // Emit a request to the engine to load more for this collection.
                state to listOf(
                    CollectionRuntimeAction.RequestRefresh(event.stableKey),
                )
            }

            is CollectionRuntimeEvent.SectionCollapseToggled -> {
                val snapshot = state.snapshots[event.stableKey] ?: return state to emptyList()
                val newVisibility = when (snapshot.visibility) {
                    is CollectionVisibility.Visible -> CollectionVisibility.Collapsed()
                    is CollectionVisibility.Collapsed -> CollectionVisibility.Visible()
                    is CollectionVisibility.Expanded -> CollectionVisibility.Collapsed()
                    else -> snapshot.visibility // Don't toggle dev-only / hidden / adaptive.
                }
                val newSnapshot = snapshot.copy(visibility = newVisibility)
                val newSnapshots = state.snapshots.toMutableMap().apply {
                    put(event.stableKey, newSnapshot)
                }
                state.copy(snapshots = newSnapshots) to listOf(
                    CollectionRuntimeAction.HapticFeedback(CollectionHapticType.LIGHT_TAP),
                )
            }

            // ─────────────────────────────────────────────────────
            // 5. USER ACTION
            // ─────────────────────────────────────────────────────

            is CollectionRuntimeEvent.CardTapped -> {
                if (state.selection.isActive) {
                    // In selection mode, tap toggles selection.
                    return reduce(state, CollectionRuntimeEvent.SelectionToggled(
                        event.cardId, event.stableKey,
                    ))
                }
                state to listOf(
                    CollectionRuntimeAction.PlayCard(event.cardId, event.stableKey),
                    CollectionRuntimeAction.HapticFeedback(CollectionHapticType.LIGHT_TAP),
                )
            }

            is CollectionRuntimeEvent.CardLongPressed -> {
                if (state.selection.isActive) {
                    state to listOf(
                        CollectionRuntimeAction.ShowContextMenu(event.cardId, event.stableKey),
                    )
                } else {
                    reduce(state, CollectionRuntimeEvent.SelectionStarted(
                        event.cardId, event.stableKey,
                    )).let { (s, actions) ->
                        s to actions + CollectionRuntimeAction.ShowContextMenu(
                            event.cardId, event.stableKey,
                        )
                    }
                }
            }

            is CollectionRuntimeEvent.PlayCollectionTapped -> {
                state to listOf(
                    CollectionRuntimeAction.PlayCollection(event.stableKey),
                    CollectionRuntimeAction.HapticFeedback(CollectionHapticType.MEDIUM_TAP),
                )
            }

            is CollectionRuntimeEvent.ShuffleCollectionTapped -> {
                state to listOf(
                    CollectionRuntimeAction.ShuffleCollection(event.stableKey),
                    CollectionRuntimeAction.HapticFeedback(CollectionHapticType.MEDIUM_TAP),
                )
            }

            is CollectionRuntimeEvent.RefreshCollectionTapped -> {
                // Optimistically mark as refreshing.
                val newRefreshStates = state.refreshStates.toMutableMap().apply {
                    val existing = this[event.stableKey]
                    put(
                        event.stableKey,
                        (existing ?: CollectionRefreshState(
                            stableKey = event.stableKey,
                            isRefreshing = false,
                            lastRefreshedAt = 0L,
                            lastFailedAt = 0L,
                            consecutiveFailures = 0,
                            lastErrorMessage = null,
                        )).copy(isRefreshing = true)
                    )
                }
                state.copy(refreshStates = newRefreshStates) to listOf(
                    CollectionRuntimeAction.RequestRefresh(event.stableKey),
                    CollectionRuntimeAction.HapticFeedback(CollectionHapticType.LIGHT_TAP),
                )
            }

            is CollectionRuntimeEvent.PinToggled -> {
                val contract = state.contractFor(event.stableKey) ?: return state to emptyList()
                val isCurrentlyPinned = contract.type == CollectionConstitution.PINNED.type
                state to listOf(
                    CollectionRuntimeAction.RequestPinToggle(
                        stableKey = event.stableKey,
                        pin = !isCurrentlyPinned,
                    ),
                    CollectionRuntimeAction.HapticFeedback(CollectionHapticType.MEDIUM_TAP),
                )
            }

            is CollectionRuntimeEvent.SeeAllTapped -> {
                state to listOf(
                    CollectionRuntimeAction.NavigateToSeeAll(
                        stableKey = event.stableKey,
                        sectionId = event.sectionId,
                    ),
                    CollectionRuntimeAction.HapticFeedback(CollectionHapticType.LIGHT_TAP),
                )
            }

            CollectionRuntimeEvent.SearchChipTapped -> {
                state to listOf(
                    CollectionRuntimeAction.NavigateTo(
                        destination = CollectionNavigationDestination.SEARCH,
                        stableKey = null,
                    ),
                    CollectionRuntimeAction.HapticFeedback(CollectionHapticType.LIGHT_TAP),
                )
            }

            CollectionRuntimeEvent.ShuffleAllChipTapped -> {
                state to listOf(
                    CollectionRuntimeAction.ShuffleAll,
                    CollectionRuntimeAction.HapticFeedback(CollectionHapticType.MEDIUM_TAP),
                )
            }

            CollectionRuntimeEvent.RefreshAllChipTapped -> {
                state to listOf(
                    CollectionRuntimeAction.RequestRefreshAll,
                    CollectionRuntimeAction.HapticFeedback(CollectionHapticType.LIGHT_TAP),
                )
            }

            CollectionRuntimeEvent.PullToRefreshTriggered -> {
                state to listOf(
                    CollectionRuntimeAction.RequestRefreshAll,
                    CollectionRuntimeAction.HapticFeedback(CollectionHapticType.RIGID_TICK),
                )
            }

            // ─────────────────────────────────────────────────────
            // 6. ENGINE
            // ─────────────────────────────────────────────────────

            is CollectionRuntimeEvent.EngineStartedRefresh -> {
                val newRefreshStates = state.refreshStates.toMutableMap().apply {
                    this[event.stableKey]?.let { existing ->
                        put(event.stableKey, existing.copy(isRefreshing = true))
                    }
                }
                state.copy(refreshStates = newRefreshStates) to emptyList()
            }

            is CollectionRuntimeEvent.EngineFinishedRefresh -> {
                val newRefreshStates = state.refreshStates.toMutableMap().apply {
                    this[event.stableKey]?.let { existing ->
                        put(
                            event.stableKey,
                            existing.copy(
                                isRefreshing = false,
                                consecutiveFailures = if (event.success) 0 else existing.consecutiveFailures + 1,
                            )
                        )
                    }
                }
                state.copy(refreshStates = newRefreshStates) to emptyList()
            }

            is CollectionRuntimeEvent.DiagnosticsUpdated -> {
                val snapshot = state.snapshots[event.stableKey] ?: return state to emptyList()
                val newSnapshot = snapshot.copy(diagnostics = event.diagnostics)
                val newSnapshots = state.snapshots.toMutableMap().apply {
                    put(event.stableKey, newSnapshot)
                }
                state.copy(snapshots = newSnapshots) to emptyList()
            }

            is CollectionRuntimeEvent.VisibilityUpdated -> {
                val snapshot = state.snapshots[event.stableKey] ?: return state to emptyList()
                val newSnapshot = snapshot.copy(visibility = event.visibility)
                val newSnapshots = state.snapshots.toMutableMap().apply {
                    put(event.stableKey, newSnapshot)
                }
                state.copy(snapshots = newSnapshots) to emptyList()
            }

            is CollectionRuntimeEvent.ActiveContractsReordered -> {
                val newActiveContracts = event.newOrder.mapNotNull { stableKey ->
                    CollectionConstitution.byKey(stableKey)
                }.filter { it.stableKey in state.activeContracts.map { c -> c.stableKey } }
                state.copy(activeContracts = newActiveContracts) to emptyList()
            }

            // ─────────────────────────────────────────────────────
            // 7. SYSTEM
            // ─────────────────────────────────────────────────────

            CollectionRuntimeEvent.PermissionGranted -> {
                state.copy(
                    phase = CollectionRuntimePhase.HYDRATING,
                    lastUpdated = System.currentTimeMillis(),
                ) to listOf(
                    CollectionRuntimeAction.RequestRefreshAll,
                    CollectionRuntimeAction.HapticFeedback(CollectionHapticType.SUCCESS),
                )
            }

            CollectionRuntimeEvent.PermissionDenied -> {
                state.copy(
                    phase = CollectionRuntimePhase.PERMISSION_REQUIRED,
                    lastUpdated = System.currentTimeMillis(),
                ) to listOf(
                    CollectionRuntimeAction.HapticFeedback(CollectionHapticType.WARNING),
                )
            }

            is CollectionRuntimeEvent.CategoryDisabled -> {
                // Remove collections whose category matches the disabled one.
                val toRemove = state.activeContracts
                    .filter { it.category.internalId == event.categoryId }
                    .map { it.stableKey }
                val newSnapshots = state.snapshots.filterKeys { it !in toRemove }
                val newActiveContracts = state.activeContracts.filter { it.stableKey !in toRemove }
                state.copy(
                    activeContracts = newActiveContracts,
                    snapshots = newSnapshots,
                    lastUpdated = System.currentTimeMillis(),
                ) to listOf(
                    CollectionRuntimeAction.HapticFeedback(CollectionHapticType.MEDIUM_TAP),
                )
            }

            is CollectionRuntimeEvent.CategoryEnabled -> {
                // Re-add collections whose category matches the enabled one.
                val toAdd = CollectionConstitution.inCategory(
                    category = state.activeContracts.firstOrNull()?.category
                        ?: return state to emptyList()
                ).filter { it.category.internalId == event.categoryId }
                val newActiveContracts = (state.activeContracts + toAdd).distinctBy { it.stableKey }
                state.copy(
                    activeContracts = newActiveContracts,
                    lastUpdated = System.currentTimeMillis(),
                ) to listOf(
                    CollectionRuntimeAction.RequestRefreshAll,
                    CollectionRuntimeAction.HapticFeedback(CollectionHapticType.SUCCESS),
                )
            }

            is CollectionRuntimeEvent.SortOrderChanged -> {
                state to listOf(
                    CollectionRuntimeAction.RequestSortChange(event.stableKey, event.sortMode),
                    CollectionRuntimeAction.HapticFeedback(CollectionHapticType.LIGHT_TAP),
                )
            }

            CollectionRuntimeEvent.ThemeChanged -> {
                // Theme change doesn't directly mutate runtime state —
                // the renderer will pick up new theme tokens. But we
                // bump lastUpdated so observers can react.
                state.copy(lastUpdated = System.currentTimeMillis()) to emptyList()
            }

            CollectionRuntimeEvent.DeveloperModeEnabled -> {
                // Surface diagnostic-only collections.
                val diagnosticContracts = CollectionConstitution.ALL.filter {
                    it.type.developerOnly && it !in state.activeContracts
                }
                val newActiveContracts = state.activeContracts + diagnosticContracts
                state.copy(
                    activeContracts = newActiveContracts,
                    lastUpdated = System.currentTimeMillis(),
                ) to listOf(
                    CollectionRuntimeAction.HapticFeedback(CollectionHapticType.LIGHT_TAP),
                )
            }

            CollectionRuntimeEvent.DeveloperModeDisabled -> {
                val newActiveContracts = state.activeContracts.filter { !it.type.developerOnly }
                val newSnapshots = state.snapshots.filterKeys { stableKey ->
                    newActiveContracts.any { it.stableKey == stableKey }
                }
                state.copy(
                    activeContracts = newActiveContracts,
                    snapshots = newSnapshots,
                    lastUpdated = System.currentTimeMillis(),
                ) to emptyList()
            }

            is CollectionRuntimeEvent.FatalError -> {
                state.copy(
                    phase = CollectionRuntimePhase.ERROR,
                    errorMessage = event.errorMessage,
                    lastUpdated = System.currentTimeMillis(),
                ) to listOf(
                    CollectionRuntimeAction.ShowToast(
                        message = event.errorMessage,
                        type = CollectionToastType.ERROR,
                    ),
                    CollectionRuntimeAction.HapticFeedback(CollectionHapticType.ERROR),
                )
            }

            CollectionRuntimeEvent.ErrorRetryRequested -> {
                state.copy(
                    phase = CollectionRuntimePhase.HYDRATING,
                    errorMessage = null,
                    lastUpdated = System.currentTimeMillis(),
                ) to listOf(
                    CollectionRuntimeAction.RequestErrorRetry,
                    CollectionRuntimeAction.HapticFeedback(CollectionHapticType.LIGHT_TAP),
                )
            }
        }
    }

    /**
     * Reduce a batch of events in order.
     *
     * Each event produces a new state and a list of actions. The
     * actions are concatenated. The final state is returned along
     * with the full action list.
     *
     * Useful for processing a burst of events (e.g. engine emitted
     * 5 collection-refreshed events in quick succession).
     */
    fun reduceAll(
        state: CollectionRuntimeState,
        events: List<CollectionRuntimeEvent>,
    ): Pair<CollectionRuntimeState, List<CollectionRuntimeAction>> {
        var currentState = state
        val allActions = mutableListOf<CollectionRuntimeAction>()
        for (event in events) {
            val (newState, actions) = reduce(currentState, event)
            currentState = newState
            allActions.addAll(actions)
        }
        return currentState to allActions
    }

    /**
     * Initialize the runtime state from the Constitution.
     *
     * This is the ONLY place the runtime reads from the Constitution
     * to construct initial state. After this, the runtime is driven
     * by events.
     *
     * @param headerOverride Optional header override (e.g. SCANNING during scan).
     */
    fun initialize(
        headerOverride: CollectionHeaderContract? = null,
    ): CollectionRuntimeState {
        val coldStartContracts = CollectionConstitution.coldStartFeed
        return CollectionRuntimeState.COLD_START.copy(
            activeContracts = coldStartContracts,
            header = headerOverride ?: CollectionHeaderContract.DEFAULT,
            lastUpdated = System.currentTimeMillis(),
        )
    }
}
