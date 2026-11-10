package com.rikodev.music.android.presentation.library

// ═══════════════════════════════════════════════════════════════════════════
// RIKO MUSIC — MILESTONE 2: LIBRARY RUNTIME — STATE REDUCER
//
// Phase A, File 5: LibraryReducer.kt
//
// Pure function: (State, Event) → (State, Action?)
// No side effects. No Compose. No Android framework. No business logic.
// No DAO. No Room. No Repository. No navigation.
// Deterministic. Testable. Production-ready.
//
// Flow: LibraryEvent → reduce() → Pair<LibraryState, List<LibraryAction>>
//
// ⭐ RULES ⭐
//   #31 — Runtime Owns Truth: Reducer is the ONLY place events create state.
//   #33 — Cards Never Fetch: Reducer never touches Repository.
//   #27 — Screen Never Sorts: Reducer never sorts content.
//
// Architecture:
//
//   LibraryEvent
//       │
//       ▼
//   LibraryReducer.reduce(state, event)
//       │
//       ├──→ New LibraryState (immutable)
//       └──→ List<LibraryAction> (output intents, empty = no side effects)
//
// ⭐ PATTERN: Same DNA as MiniPlayerReducer ⭐
//   Pure function. Sealed event dispatch. copy() for new state.
//   Actions are ONE-WAY output — reducer never executes them.
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Pure reducer for [LibraryState].
 *
 * Takes the current state and an event, returns a new state
 * and an optional list of output actions.
 *
 * Same input always produces the same output (pure function).
 * No coroutines, no suspend, no I/O, no Android framework.
 * Safe to call from any thread.
 */
object LibraryReducer {

    /**
     * Reduce: process a single event against current state.
     *
     * @param state Current immutable state.
     * @param event The event to process.
     * @return Pair of (new state, list of output actions).
     */
    fun reduce(
        state: LibraryState,
        event: LibraryEvent
    ): Pair<LibraryState, List<LibraryAction>> {
        val actions = mutableListOf<LibraryAction>()

        val newState = when (event) {
            // ── Navigation ──────────────────────────────────────────
            is LibraryEvent.ScreenEntered -> {
                state
            }
            is LibraryEvent.ScreenExited -> {
                state
            }
            is LibraryEvent.ScreenResumed -> {
                state
            }

            // ── Content Taps ───────────────────────────────────────
            is LibraryEvent.TapAlbum -> {
                actions += LibraryAction.NavigateToAlbum(event.albumId)
                actions += LibraryAction.HapticFeedback(HapticType.TAP)
                state
            }
            is LibraryEvent.TapArtist -> {
                actions += LibraryAction.NavigateToArtist(event.artistId)
                actions += LibraryAction.HapticFeedback(HapticType.TAP)
                state
            }
            is LibraryEvent.TapFolder -> {
                actions += LibraryAction.NavigateToFolder(event.folderPath)
                actions += LibraryAction.HapticFeedback(HapticType.TAP)
                state
            }
            is LibraryEvent.TapPlaylist -> {
                actions += LibraryAction.NavigateToPlaylist(event.playlistId)
                actions += LibraryAction.HapticFeedback(HapticType.TAP)
                state
            }
            is LibraryEvent.TapSong -> {
                actions += LibraryAction.PlaySong(event.songId, event.contextPlaylist)
                actions += LibraryAction.HapticFeedback(HapticType.TAP)
                state
            }
            is LibraryEvent.TapCollection -> {
                actions += LibraryAction.NavigateToCollection(event.collectionId)
                actions += LibraryAction.HapticFeedback(HapticType.TAP)
                state
            }
            is LibraryEvent.TapFavorite -> {
                actions += LibraryAction.ToggleFavorite(event.songId)
                state
            }

            // ── Selection ───────────────────────────────────────────
            is LibraryEvent.LongPressItem -> {
                actions += LibraryAction.EnterSelectionMode(event.itemId)
                actions += LibraryAction.HapticFeedback(HapticType.LONG_PRESS)
                state.copy(
                    selection = LibrarySelectionState(
                        isActive = true,
                        selectedIds = setOf(event.itemId),
                        selectionCount = 1
                    )
                )
            }
            is LibraryEvent.ToggleSelection -> {
                if (!state.selection.isActive) return state to emptyList()
                val newIds = if (event.itemId in state.selection.selectedIds) {
                    state.selection.selectedIds - event.itemId
                } else {
                    state.selection.selectedIds + event.itemId
                }
                val isStillActive = newIds.isNotEmpty()
                state.copy(
                    selection = if (isStillActive) {
                        state.selection.copy(
                            selectedIds = newIds,
                            selectionCount = newIds.size
                        )
                    } else {
                        LibrarySelectionState.IDLE
                    }
                ).also {
                    if (newIds.isNotEmpty()) {
                        actions += LibraryAction.HapticFeedback(HapticType.SELECTION_TOGGLE)
                    }
                }
            }
            is LibraryEvent.SelectAll -> {
                // Selection of all items is handled by ViewModel (it knows the content).
                // Reducer just signals intent.
                state
            }
            is LibraryEvent.ClearSelection -> {
                state.copy(selection = LibrarySelectionState.IDLE)
            }

            // ── Scroll ─────────────────────────────────────────────
            is LibraryEvent.ScrollChanged -> {
                state.copy(
                    scroll = state.scroll.copy(
                        firstVisibleIndex = event.firstVisibleIndex,
                        firstVisibleOffset = event.firstVisibleOffset
                    )
                )
            }
            is LibraryEvent.ScrollDirectionChanged -> {
                state.copy(
                    scroll = state.scroll.copy(direction = event.direction)
                )
            }
            is LibraryEvent.ScrollActiveChanged -> {
                state.copy(
                    scroll = state.scroll.copy(isScrolling = event.isScrolling)
                )
            }
            is LibraryEvent.FastScrollStarted -> {
                state.copy(
                    scroll = state.scroll.copy(isFastScrolling = true)
                )
            }
            is LibraryEvent.FastScrollJump -> {
                actions += LibraryAction.HapticFeedback(HapticType.FAST_SCROLL_JUMP)
                state.copy(
                    scroll = state.scroll.copy(stickySection = event.section)
                )
            }
            is LibraryEvent.FastScrollEnded -> {
                state.copy(
                    scroll = state.scroll.copy(isFastScrolling = false)
                )
            }
            is LibraryEvent.StickySectionChanged -> {
                state.copy(
                    scroll = state.scroll.copy(stickySection = event.section)
                )
            }

            // ── Search ───────────────────────────────────────────
            is LibraryEvent.SearchBarTapped -> {
                state.copy(
                    search = state.search.copy(isExpanded = true)
                )
            }
            is LibraryEvent.SearchQueryChanged -> {
                state.copy(
                    search = state.search.copy(
                        query = event.query,
                        isSearching = event.query.isNotBlank(),
                        hasNoResults = false
                    )
                )
            }
            is LibraryEvent.SearchSubmitted -> {
                state.copy(
                    search = state.search.copy(
                        query = event.query,
                        isSearching = event.query.isNotBlank()
                    )
                )
            }
            is LibraryEvent.SearchCleared -> {
                state.copy(
                    search = LibrarySearchState(isExpanded = true)
                )
            }
            is LibraryEvent.SearchClosed -> {
                state.copy(search = LibrarySearchState.IDLE)
            }
            is LibraryEvent.SearchNoResults -> {
                state.copy(
                    search = state.search.copy(
                        isSearching = false,
                        hasNoResults = true
                    )
                )
            }

            // ── Filter ────────────────────────────────────────────
            is LibraryEvent.FilterChanged -> {
                state.copy(
                    filter = state.filter.copy(contentType = event.contentType),
                    // Close search when filter changes
                    search = LibrarySearchState.IDLE,
                    // Reset selection when filter changes
                    selection = LibrarySelectionState.IDLE
                )
            }
            is LibraryEvent.ToggleFavoritesFilter -> {
                val newFavoritesOnly = !state.filter.favoritesOnly
                state.copy(
                    filter = state.filter.copy(favoritesOnly = newFavoritesOnly)
                )
            }
            is LibraryEvent.FilterChipTapped -> {
                val newFilters = if (event.filterChip in state.filter.activeFilters) {
                    state.filter.activeFilters - event.filterChip
                } else {
                    state.filter.activeFilters + event.filterChip
                }
                state.copy(
                    filter = state.filter.copy(activeFilters = newFilters)
                )
            }

            // ── Header ────────────────────────────────────────────
            is LibraryEvent.HeaderCollapseStart -> {
                // Animation is driven by scroll. Reducer just tracks intent.
                state
            }
            is LibraryEvent.HeaderExpandStart -> {
                state
            }
            is LibraryEvent.HeaderCollapseProgress -> {
                val isExpanded = event.progress < 0.5f
                state.copy(
                    header = state.header.copy(
                        collapseProgress = event.progress,
                        isExpanded = isExpanded
                    )
                )
            }

            // ── Dock ─────────────────────────────────────────────
            is LibraryEvent.DockTapped -> {
                actions += LibraryAction.ExpandMiniPlayer
                state
            }
            is LibraryEvent.DockPressChanged -> {
                state.copy(
                    dock = state.dock.copy(isPressed = event.isPressed)
                )
            }
            is LibraryEvent.DockOffsetChanged -> {
                state.copy(
                    dock = state.dock.copy(dockOffset = event.offset)
                )
            }

            // ── Experience OS ──────────────────────────────────────
            is LibraryEvent.PlaybackChanged -> {
                state.copy(
                    dock = state.dock.copy(
                        isDocked = event.currentSongId != null
                    ),
                    runtime = state.runtime.copy(
                        // If we now have songs and weren't ready, update phase
                        phase = if (event.currentSongId != null &&
                            state.runtime.phase == LibraryPhase.LOADING
                        ) LibraryPhase.READY
                        else state.runtime.phase
                    )
                )
            }
            is LibraryEvent.ColorChanged -> {
                state.copy(color = event.colorSnapshot)
            }
            is LibraryEvent.WaveChanged -> {
                state.copy(wave = event.waveSnapshot)
            }
            is LibraryEvent.ContentRefreshed -> {
                state.copy(
                    runtime = state.runtime.copy(
                        isReady = true,
                        phase = if (state.runtime.phase != LibraryPhase.PERMISSION_REQUIRED)
                            LibraryPhase.READY
                        else state.runtime.phase,
                        isScanning = false
                    )
                )
            }
            is LibraryEvent.CountsUpdated -> {
                state.copy(
                    runtime = state.runtime.copy(
                        totalSongCount = event.songCount,
                        totalAlbumCount = event.albumCount,
                        totalArtistCount = event.artistCount
                    )
                )
            }

            // ── System ───────────────────────────────────────────
            is LibraryEvent.PermissionResult -> {
                if (event.granted) {
                    state.copy(
                        runtime = state.runtime.copy(
                            hasPermission = true,
                            phase = LibraryPhase.LOADING,
                            error = null
                        )
                    )
                } else {
                    state.copy(
                        runtime = state.runtime.copy(
                            hasPermission = false,
                            phase = LibraryPhase.PERMISSION_REQUIRED
                        )
                    )
                }
            }
            is LibraryEvent.ScanStarted -> {
                state.copy(
                    runtime = state.runtime.copy(
                        isScanning = true,
                        scanProgress = 0f,
                        phase = LibraryPhase.SCANNING
                    )
                )
            }
            is LibraryEvent.ScanProgress -> {
                state.copy(
                    runtime = state.runtime.copy(
                        scanProgress = event.progress.coerceIn(0f, 1f)
                    )
                )
            }
            is LibraryEvent.ScanComplete -> {
                state.copy(
                    runtime = state.runtime.copy(
                        isScanning = false,
                        scanProgress = 1f,
                        phase = if (state.runtime.hasPermission) LibraryPhase.READY
                        else state.runtime.phase
                    )
                )
            }
            is LibraryEvent.ConfigChanged -> {
                // Library state survives config changes.
                // No morph to reset (unlike Mini Player).
                // Layout is handled by Compose recomposition.
                state
            }
            is LibraryEvent.MemoryPressure -> {
                state
            }
            is LibraryEvent.ScreenOff -> {
                state
            }
            is LibraryEvent.ScreenOn -> {
                state
            }
            is LibraryEvent.AppBackgrounded -> {
                state
            }
            is LibraryEvent.AppForegrounded -> {
                state
            }
            is LibraryEvent.ErrorOccurred -> {
                state.copy(
                    runtime = state.runtime.copy(
                        phase = LibraryPhase.ERROR,
                        error = event.message,
                        isReady = false
                    )
                )
            }
            is LibraryEvent.ErrorCleared -> {
                state.copy(
                    runtime = state.runtime.copy(
                        phase = if (state.runtime.hasPermission) LibraryPhase.READY
                        else LibraryPhase.PERMISSION_REQUIRED,
                        error = null
                    )
                )
            }
            is LibraryEvent.PullRefresh -> {
                actions += LibraryAction.RefreshContent
                state
            }
            is LibraryEvent.SortChanged -> {
                state.copy(
                    sort = state.sort.copy(
                        currentSortLabel = event.label,
                        isAscending = event.isAscending
                    )
                )
            }
            is LibraryEvent.ToggleSortPicker -> {
                state.copy(
                    sort = state.sort.copy(
                        isPickerVisible = !state.sort.isPickerVisible
                    )
                )
            }
        }

        return newState to actions
    }

    /**
     * Batch reduce: process multiple events sequentially.
     *
     * Useful for coalescing multiple rapid events into a single state update.
     * Each event is processed in order, and actions are accumulated.
     *
     * @param state Initial state.
     * @param events List of events to process in order.
     * @return Pair of (final state, accumulated actions).
     */
    fun reduceAll(
        state: LibraryState,
        events: List<LibraryEvent>
    ): Pair<LibraryState, List<LibraryAction>> {
        var currentState = state
        val allActions = mutableListOf<LibraryAction>()

        for (event in events) {
            val (newState, actions) = reduce(currentState, event)
            currentState = newState
            allActions.addAll(actions)
        }

        return currentState to allActions
    }
}
