/*
 * RIKO EXPERIENCE OS — LIBRARY INTERACTION RUNTIME
 * Build B5.4 — Library Interaction Runtime
 *
 * File 5 / 15 — SelectionReducer.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Translate [SelectionIntent]s into [SelectionState] mutations +
 * [SelectionAction]s. Pure: same inputs → same outputs, byte-identical.
 *
 * ARCHITECTURE
 * ------------
 *   SelectionIntent (from GestureResolver / UI)
 *        ↓
 *   SelectionReducer.reduce(state, intent) — PURE
 *        ↓
 *   Pair<SelectionState, List<SelectionAction>>
 *
 * The Reducer calls [SelectionRuntime] operations to compute new state.
 * It does NOT call the Repository, the Engine, or any side-effectful API.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #47  Reducer Never Calls Engine — Reducer only calls pure Runtime.
 * #78  Interaction Creates Intent, Never Action — Reducer consumes
 *      Intents, emits Actions. The Action list may be empty (state-
 *      only mutation) or non-empty (state + side-effect request).
 * #79  Selection Is Independent — Reducer is the single entry point
 *      for selection mutations. No other file mutates SelectionState.
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Compose. No Android framework. No Coroutine. No Flow. No
 * ViewModel. No `var`. No side effects. All functions are pure.
 *
 * © RIKO Experience OS — Interaction Runtime
 */

package com.rikodev.music.android.presentation.library.interaction

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 1 — SELECTION INTENT (Rule #78)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * An Intent to mutate selection state. Produced by the GestureResolver
 * or by direct UI input (e.g. "Select All" button).
 *
 * Intents are NOT Actions. They describe *what the user wants to do to
 * selection*, not *what should happen as a result*.
 *
 * Example:
 *   - Intent: ToggleSelection(itemId=42)
 *   - Reducer: calls SelectionRuntime.toggle(state, 42)
 *   - Result: new state with 42 toggled
 *   - Action: SelectionChanged(count=newCount) — emitted so UI can
 *     update the selection action bar.
 *
 * Some Intents produce side-effect Actions:
 *   - Intent: ConfirmDeleteSelected
 *   - Reducer: clears selection
 *   - Action: DeleteItems(itemIds=[...]) — emitted so Coordinator
 *     can execute the actual deletion.
 */
sealed class SelectionIntent {

    /** Select a single item (replaces current selection). */
    data class SelectSingle(
        val itemId: SelectionItemId,
        val scopeId: SelectionScope,
        val sourceGestureId: String?,
    ) : SelectionIntent()

    /** Start multi-selection with an initial item. */
    data class StartMultiple(
        val itemId: SelectionItemId,
        val scopeId: SelectionScope,
        val sourceGestureId: String?,
    ) : SelectionIntent()

    /** Toggle an item's selection (only valid in MULTIPLE mode). */
    data class ToggleSelection(
        val itemId: SelectionItemId,
        val sourceGestureId: String?,
    ) : SelectionIntent()

    /** Start a range selection with an anchor. */
    data class StartRange(
        val itemId: SelectionItemId,
        val itemIndex: Int,
        val scopeId: SelectionScope,
        val sourceGestureId: String?,
    ) : SelectionIntent()

    /** Extend the range to a new target. */
    data class ExtendRange(
        val targetId: SelectionItemId,
        val targetIndex: Int,
        val sourceGestureId: String?,
    ) : SelectionIntent()

    /** Invert the selection within the current scope. */
    data object InvertSelection : SelectionIntent()

    /** Select all visible items in the current scope. */
    data object SelectAll : SelectionIntent()

    /** Clear the selection (pinned items survive). */
    data object ClearSelection : SelectionIntent()

    /** Clear selection including pinned items. */
    data object ClearAll : SelectionIntent()

    /** Pin all currently-selected items. */
    data object PinSelected : SelectionIntent()

    /** Unpin a specific item. */
    data class Unpin(
        val itemId: SelectionItemId,
    ) : SelectionIntent()

    /** Unpin all pinned items. */
    data object UnpinAll : SelectionIntent()

    /** Set the focused item (keyboard / D-pad). */
    data class FocusItem(
        val itemId: SelectionItemId?,
    ) : SelectionIntent()

    /** Move focus by a delta (D-pad navigation). */
    data class MoveFocus(
        val delta: Int,
    ) : SelectionIntent()

    /** Transition to a new scope (clears current scope's selection). */
    data class TransitionScope(
        val newScopeId: SelectionScope,
    ) : SelectionIntent()

    /** Confirm deletion of all selected items. */
    data object ConfirmDeleteSelected : SelectionIntent()

    /** Confirm sharing all selected items. */
    data object ConfirmShareSelected : SelectionIntent()

    /** Confirm adding all selected items to a playlist. */
    data class ConfirmAddToPlaylist(
        val playlistId: Long,
    ) : SelectionIntent()
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 2 — SELECTION ACTION (Output)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * An Action emitted by the SelectionReducer. These represent *requests*
 * for side effects — the Coordinator (B5.5 Host) will execute them.
 *
 * Most Intents produce zero Actions (state-only mutation). A few
 * (ConfirmDelete, ConfirmShare, ConfirmAddToPlaylist) produce Actions.
 */
sealed class SelectionAction {

    /** Emitted whenever selection changes. UI uses this to update the
     *  selection action bar (count, available actions). */
    data class SelectionChanged(
        val newCount: Int,
        val mode: SelectionMode,
    ) : SelectionAction()

    /** Emitted when selection mode is entered. */
    data class SelectionModeEntered(
        val mode: SelectionMode,
        val scopeId: SelectionScope,
    ) : SelectionAction()

    /** Emitted when selection mode is exited. */
    data object SelectionModeExited : SelectionAction()

    /** Request deletion of the given item ids. */
    data class DeleteItems(
        val itemIds: List<SelectionItemId>,
        val scopeId: SelectionScope?,
    ) : SelectionAction()

    /** Request sharing the given item ids. */
    data class ShareItems(
        val itemIds: List<SelectionItemId>,
        val scopeId: SelectionScope?,
    ) : SelectionAction()

    /** Request adding the given item ids to a playlist. */
    data class AddItemsToPlaylist(
        val itemIds: List<SelectionItemId>,
        val playlistId: Long,
    ) : SelectionAction()

    /** Request haptic feedback for a selection toggle. */
    data object SelectionToggleHaptic : SelectionAction()

    /** Request haptic feedback for entering selection mode. */
    data object SelectionEnteredHaptic : SelectionAction()

    /** Request haptic feedback for clearing selection. */
    data object SelectionClearedHaptic : SelectionAction()
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 3 — REDUCER RESULT
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The result of reducing a SelectionIntent.
 *
 * @property newState     The new SelectionState.
 * @property actions      Actions to be executed by the Coordinator.
 * @property explanation  Human-readable explanation (Rule #82).
 */
data class SelectionReducerResult(
    val newState: SelectionState,
    val actions: List<SelectionAction>,
    val explanation: String,
) {
    companion object {
        fun noChange(state: SelectionState, explanation: String): SelectionReducerResult =
            SelectionReducerResult(
                newState = state,
                actions = emptyList(),
                explanation = explanation,
            )

        fun stateOnly(
            newState: SelectionState,
            explanation: String,
        ): SelectionReducerResult = SelectionReducerResult(
            newState = newState,
            actions = if (newState.mode != SelectionMode.NONE) {
                listOf(SelectionAction.SelectionChanged(newState.totalCount, newState.mode))
            } else emptyList(),
            explanation = explanation,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 4 — THE REDUCER (PURE)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The Selection Reducer.
 *
 * Pure function: (State, Intent, Context) → Result.
 *
 * The Context ([SelectionReducerContext]) provides the visible items
 * (for RANGE / INVERT / SELECT_ALL) and the current wall-clock time.
 */
object SelectionReducer {

    /**
     * Reduce a [SelectionIntent] against the current [SelectionState].
     *
     * @param state         Current selection state.
     * @param intent        The intent to apply.
     * @param context       Reducer context (visible items, current time).
     * @return              [SelectionReducerResult] with new state + actions.
     */
    fun reduce(
        state: SelectionState,
        intent: SelectionIntent,
        context: SelectionReducerContext,
    ): SelectionReducerResult {
        return when (intent) {
            is SelectionIntent.SelectSingle -> reduceSelectSingle(state, intent, context)
            is SelectionIntent.StartMultiple -> reduceStartMultiple(state, intent, context)
            is SelectionIntent.ToggleSelection -> reduceToggle(state, intent, context)
            is SelectionIntent.StartRange -> reduceStartRange(state, intent, context)
            is SelectionIntent.ExtendRange -> reduceExtendRange(state, intent, context)
            is SelectionIntent.InvertSelection -> reduceInvert(state, context)
            is SelectionIntent.SelectAll -> reduceSelectAll(state, context)
            is SelectionIntent.ClearSelection -> reduceClear(state, context, clearPinned = false)
            is SelectionIntent.ClearAll -> reduceClear(state, context, clearPinned = true)
            is SelectionIntent.PinSelected -> reducePinSelected(state, context)
            is SelectionIntent.Unpin -> reduceUnpin(state, intent, context)
            is SelectionIntent.UnpinAll -> reduceUnpinAll(state, context)
            is SelectionIntent.FocusItem -> reduceFocus(state, intent, context)
            is SelectionIntent.MoveFocus -> reduceMoveFocus(state, intent, context)
            is SelectionIntent.TransitionScope -> reduceTransitionScope(state, intent, context)
            is SelectionIntent.ConfirmDeleteSelected -> reduceConfirmDelete(state, context)
            is SelectionIntent.ConfirmShareSelected -> reduceConfirmShare(state, context)
            is SelectionIntent.ConfirmAddToPlaylist -> reduceConfirmAddToPlaylist(state, intent, context)
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // PRIVATE IMPLEMENTATION
    // ─────────────────────────────────────────────────────────────────

    private fun reduceSelectSingle(
        state: SelectionState,
        intent: SelectionIntent.SelectSingle,
        context: SelectionReducerContext,
    ): SelectionReducerResult {
        val wasActive = state.isActive
        val newState = SelectionRuntime.selectSingle(
            state = state,
            itemId = intent.itemId,
            scopeId = intent.scopeId,
            nowMs = context.nowMs,
        )
        val actions = buildList {
            if (!wasActive) add(SelectionAction.SelectionModeEntered(SelectionMode.SINGLE, intent.scopeId))
            add(SelectionAction.SelectionChanged(newState.totalCount, newState.mode))
            add(SelectionAction.SelectionToggleHaptic)
        }
        return SelectionReducerResult(
            newState = newState,
            actions = actions,
            explanation = "SelectSingle: item=${intent.itemId.value}, scope=${intent.scopeId.value}",
        )
    }

    private fun reduceStartMultiple(
        state: SelectionState,
        intent: SelectionIntent.StartMultiple,
        context: SelectionReducerContext,
    ): SelectionReducerResult {
        val wasActive = state.isActive
        val newState = SelectionRuntime.startMultiple(
            state = state,
            itemId = intent.itemId,
            scopeId = intent.scopeId,
            nowMs = context.nowMs,
        )
        val actions = buildList {
            if (!wasActive) add(SelectionAction.SelectionModeEntered(SelectionMode.MULTIPLE, intent.scopeId))
            add(SelectionAction.SelectionChanged(newState.totalCount, newState.mode))
            add(SelectionAction.SelectionEnteredHaptic)
        }
        return SelectionReducerResult(
            newState = newState,
            actions = actions,
            explanation = "StartMultiple: item=${intent.itemId.value}, scope=${intent.scopeId.value}",
        )
    }

    private fun reduceToggle(
        state: SelectionState,
        intent: SelectionIntent.ToggleSelection,
        context: SelectionReducerContext,
    ): SelectionReducerResult {
        if (state.mode != SelectionMode.MULTIPLE) {
            return SelectionReducerResult.noChange(
                state = state,
                explanation = "ToggleSelection ignored: mode=${state.mode} (requires MULTIPLE)",
            )
        }

        val newState = SelectionRuntime.toggle(
            state = state,
            itemId = intent.itemId,
            nowMs = context.nowMs,
        )
        val actions = buildList {
            add(SelectionAction.SelectionChanged(newState.totalCount, newState.mode))
            add(SelectionAction.SelectionToggleHaptic)
        }
        return SelectionReducerResult(
            newState = newState,
            actions = actions,
            explanation = "ToggleSelection: item=${intent.itemId.value}, now selected=${newState.isSelected(intent.itemId)}",
        )
    }

    private fun reduceStartRange(
        state: SelectionState,
        intent: SelectionIntent.StartRange,
        context: SelectionReducerContext,
    ): SelectionReducerResult {
        val wasActive = state.isActive
        val newState = SelectionRuntime.startRange(
            state = state,
            itemId = intent.itemId,
            itemIndex = intent.itemIndex,
            scopeId = intent.scopeId,
            nowMs = context.nowMs,
        )
        val actions = buildList {
            if (!wasActive) add(SelectionAction.SelectionModeEntered(SelectionMode.RANGE, intent.scopeId))
            add(SelectionAction.SelectionChanged(newState.totalCount, newState.mode))
            add(SelectionAction.SelectionEnteredHaptic)
        }
        return SelectionReducerResult(
            newState = newState,
            actions = actions,
            explanation = "StartRange: anchor=${intent.itemId.value} (${intent.itemIndex}), scope=${intent.scopeId.value}",
        )
    }

    private fun reduceExtendRange(
        state: SelectionState,
        intent: SelectionIntent.ExtendRange,
        context: SelectionReducerContext,
    ): SelectionReducerResult {
        val visibleScope = context.visibleScopeFor(state.scopeId)
            ?: return SelectionReducerResult.noChange(
                state = state,
                explanation = "ExtendRange ignored: no visible scope for ${state.scopeId}",
            )

        val newState = SelectionRuntime.extendRange(
            state = state,
            targetId = intent.targetId,
            targetIndex = intent.targetIndex,
            visibleScope = visibleScope,
            nowMs = context.nowMs,
        )
        val actions = listOf(
            SelectionAction.SelectionChanged(newState.totalCount, newState.mode),
        )
        return SelectionReducerResult(
            newState = newState,
            actions = actions,
            explanation = "ExtendRange: target=${intent.targetId.value} (${intent.targetIndex}), selected=${newState.selectedIds.size}",
        )
    }

    private fun reduceInvert(
        state: SelectionState,
        context: SelectionReducerContext,
    ): SelectionReducerResult {
        val visibleScope = context.visibleScopeFor(state.scopeId)
            ?: return SelectionReducerResult.noChange(
                state = state,
                explanation = "InvertSelection ignored: no active scope",
            )

        val newState = SelectionRuntime.invert(
            state = state,
            visibleScope = visibleScope,
            nowMs = context.nowMs,
        )
        val actions = buildList {
            add(SelectionAction.SelectionChanged(newState.totalCount, newState.mode))
            add(SelectionAction.SelectionToggleHaptic)
        }
        return SelectionReducerResult(
            newState = newState,
            actions = actions,
            explanation = "InvertSelection: was=${state.selectedIds.size}, now=${newState.selectedIds.size}",
        )
    }

    private fun reduceSelectAll(
        state: SelectionState,
        context: SelectionReducerContext,
    ): SelectionReducerResult {
        val visibleScope = context.visibleScopeFor(state.scopeId)
            ?: return SelectionReducerResult.noChange(
                state = state,
                explanation = "SelectAll ignored: no active scope",
            )

        val newState = SelectionRuntime.selectAll(
            state = state,
            visibleScope = visibleScope,
            nowMs = context.nowMs,
        )
        val actions = listOf(
            SelectionAction.SelectionChanged(newState.totalCount, newState.mode),
        )
        return SelectionReducerResult(
            newState = newState,
            actions = actions,
            explanation = "SelectAll: selected=${newState.selectedIds.size} items",
        )
    }

    private fun reduceClear(
        state: SelectionState,
        context: SelectionReducerContext,
        clearPinned: Boolean,
    ): SelectionReducerResult {
        val wasActive = state.isActive
        val newState = SelectionRuntime.clear(
            state = state,
            nowMs = context.nowMs,
            clearPinned = clearPinned,
        )
        val actions = buildList {
            if (wasActive) add(SelectionAction.SelectionModeExited)
            add(SelectionAction.SelectionClearedHaptic)
        }
        return SelectionReducerResult(
            newState = newState,
            actions = actions,
            explanation = "Clear: clearPinned=$clearPinned, was=${state.totalCount}, now=${newState.totalCount}",
        )
    }

    private fun reducePinSelected(
        state: SelectionState,
        context: SelectionReducerContext,
    ): SelectionReducerResult {
        val wasCount = state.totalCount
        val newState = SelectionRuntime.pinSelected(
            state = state,
            nowMs = context.nowMs,
        )
        val actions = listOf(
            SelectionAction.SelectionChanged(newState.totalCount, newState.mode),
        )
        return SelectionReducerResult(
            newState = newState,
            actions = actions,
            explanation = "PinSelected: pinned $wasCount items, total pinned=${newState.pinnedIds.size}",
        )
    }

    private fun reduceUnpin(
        state: SelectionState,
        intent: SelectionIntent.Unpin,
        context: SelectionReducerContext,
    ): SelectionReducerResult {
        val newState = SelectionRuntime.unpin(
            state = state,
            itemId = intent.itemId,
            nowMs = context.nowMs,
        )
        val actions = listOf(
            SelectionAction.SelectionChanged(newState.totalCount, newState.mode),
        )
        return SelectionReducerResult(
            newState = newState,
            actions = actions,
            explanation = "Unpin: item=${intent.itemId.value}, pinned remaining=${newState.pinnedIds.size}",
        )
    }

    private fun reduceUnpinAll(
        state: SelectionState,
        context: SelectionReducerContext,
    ): SelectionReducerResult {
        val wasCount = state.pinnedIds.size
        val newState = SelectionRuntime.unpinAll(
            state = state,
            nowMs = context.nowMs,
        )
        val actions = listOf(
            SelectionAction.SelectionChanged(newState.totalCount, newState.mode),
        )
        return SelectionReducerResult(
            newState = newState,
            actions = actions,
            explanation = "UnpinAll: removed $wasCount pinned items",
        )
    }

    private fun reduceFocus(
        state: SelectionState,
        intent: SelectionIntent.FocusItem,
        context: SelectionReducerContext,
    ): SelectionReducerResult {
        val newState = SelectionRuntime.focus(
            state = state,
            itemId = intent.itemId,
            nowMs = context.nowMs,
        )
        // Focus changes don't emit SelectionChanged — focus is orthogonal
        return SelectionReducerResult(
            newState = newState,
            actions = emptyList(),
            explanation = "FocusItem: ${intent.itemId?.value ?: "null"}",
        )
    }

    private fun reduceMoveFocus(
        state: SelectionState,
        intent: SelectionIntent.MoveFocus,
        context: SelectionReducerContext,
    ): SelectionReducerResult {
        val visibleScope = context.visibleScopeFor(state.scopeId)
            ?: return SelectionReducerResult.noChange(
                state = state,
                explanation = "MoveFocus ignored: no active scope",
            )

        val newState = SelectionRuntime.moveFocus(
            state = state,
            delta = intent.delta,
            visibleScope = visibleScope,
            nowMs = context.nowMs,
        )
        return SelectionReducerResult(
            newState = newState,
            actions = emptyList(),
            explanation = "MoveFocus: delta=${intent.delta}, focused=${newState.focusedItemId?.value}",
        )
    }

    private fun reduceTransitionScope(
        state: SelectionState,
        intent: SelectionIntent.TransitionScope,
        context: SelectionReducerContext,
    ): SelectionReducerResult {
        val wasActive = state.isActive
        val newState = SelectionRuntime.transitionScope(
            state = state,
            newScopeId = intent.newScopeId,
            nowMs = context.nowMs,
        )
        val actions = buildList {
            if (wasActive) add(SelectionAction.SelectionModeExited)
        }
        return SelectionReducerResult(
            newState = newState,
            actions = actions,
            explanation = "TransitionScope: from=${state.scopeId?.value}, to=${intent.newScopeId.value}",
        )
    }

    private fun reduceConfirmDelete(
        state: SelectionState,
        context: SelectionReducerContext,
    ): SelectionReducerResult {
        if (state.totalCount == 0) {
            return SelectionReducerResult.noChange(
                state = state,
                explanation = "ConfirmDeleteSelected ignored: no selection",
            )
        }

        val itemIds = state.allSelectedIds.toList()
        val newState = SelectionRuntime.clear(
            state = state,
            nowMs = context.nowMs,
            clearPinned = true, // delete clears everything
        )
        val actions = listOf(
            SelectionAction.DeleteItems(itemIds = itemIds, scopeId = state.scopeId),
            SelectionAction.SelectionModeExited,
        )
        return SelectionReducerResult(
            newState = newState,
            actions = actions,
            explanation = "ConfirmDeleteSelected: ${itemIds.size} items",
        )
    }

    private fun reduceConfirmShare(
        state: SelectionState,
        context: SelectionReducerContext,
    ): SelectionReducerResult {
        if (state.totalCount == 0) {
            return SelectionReducerResult.noChange(
                state = state,
                explanation = "ConfirmShareSelected ignored: no selection",
            )
        }

        val itemIds = state.allSelectedIds.toList()
        // Share does NOT clear selection — user may want to share again
        val actions = listOf(
            SelectionAction.ShareItems(itemIds = itemIds, scopeId = state.scopeId),
        )
        return SelectionReducerResult(
            newState = state,
            actions = actions,
            explanation = "ConfirmShareSelected: ${itemIds.size} items",
        )
    }

    private fun reduceConfirmAddToPlaylist(
        state: SelectionState,
        intent: SelectionIntent.ConfirmAddToPlaylist,
        context: SelectionReducerContext,
    ): SelectionReducerResult {
        if (state.totalCount == 0) {
            return SelectionReducerResult.noChange(
                state = state,
                explanation = "ConfirmAddToPlaylist ignored: no selection",
            )
        }

        val itemIds = state.allSelectedIds.toList()
        val newState = SelectionRuntime.clear(
            state = state,
            nowMs = context.nowMs,
            clearPinned = false, // add-to-playlist clears selection but not pinned
        )
        val actions = listOf(
            SelectionAction.AddItemsToPlaylist(itemIds = itemIds, playlistId = intent.playlistId),
            SelectionAction.SelectionModeExited,
        )
        return SelectionReducerResult(
            newState = newState,
            actions = actions,
            explanation = "ConfirmAddToPlaylist: ${itemIds.size} items → playlist ${intent.playlistId}",
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 5 — REDUCER CONTEXT
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Context the SelectionReducer needs.
 *
 * Provides:
 *   - Current wall-clock time (for `lastChangedAtMs`).
 *   - Visible items per scope (for RANGE / INVERT / SELECT_ALL).
 *
 * Rule #80: the visible items are a *snapshot*, not a UI reference.
 * The host (B5.5) is responsible for building this snapshot from the
 * current screen state and passing it to the Reducer.
 */
data class SelectionReducerContext(
    val nowMs: Long,
    val visibleScopes: Map<SelectionScope, VisibleItemScope>,
) {
    /**
     * Get the visible items for a scope. Returns null if the scope
     * is not currently visible.
     */
    fun visibleScopeFor(scope: SelectionScope?): VisibleItemScope? {
        if (scope == null) return null
        return visibleScopes[scope]
    }

    companion object {
        val EMPTY = SelectionReducerContext(
            nowMs = 0L,
            visibleScopes = emptyMap(),
        )
    }
}
