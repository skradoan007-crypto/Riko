/*
 * RIKO EXPERIENCE OS — LIBRARY INTERACTION RUNTIME
 * Build B5.4 — Library Interaction Runtime
 *
 * File 4 / 15 — SelectionRuntime.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Provide *pure functions* for every selection operation the Library
 * supports. The Runtime is the engine that the [SelectionReducer]
 * uses to compute new [SelectionState]s from [SelectionIntent]s.
 *
 * SELECTION MODES
 * ---------------
 *   NONE     — no selection active. User is browsing normally.
 *   SINGLE   — one item selected. Tap on another replaces selection.
 *   MULTIPLE — multiple items selected. Tap toggles items individually.
 *   RANGE    — range selection. Anchor + extend-to defines the range.
 *
 * Selection modes can transition:
 *   NONE       → SINGLE      (tap)
 *   NONE       → MULTIPLE    (long-press → context menu → "select")
 *   SINGLE     → MULTIPLE    (long-press on different item)
 *   MULTIPLE   → RANGE       (shift-click / long-press + tap)
 *   *          → NONE        (clear)
 *
 * ADDITIONAL CONCEPTS
 * -------------------
 *   PINNED  — items that stay selected even when "Clear" is invoked.
 *             Used for "lock these songs while I browse".
 *   FOCUSED — a single highlighted item, distinct from selection.
 *             Used for keyboard navigation focus (TalkBack / D-pad).
 *
 * RULES ENFORCED HERE
 * -------------------
 * #79  Selection Is Independent — this file owns selection state.
 *      Cards never own selection. They query this state.
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
// SECTION 1 — SELECTION MODE
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The active selection mode.
 *
 * - NONE:      no selection active
 * - SINGLE:    one item selected; tap replaces
 * - MULTIPLE:  many items selected; tap toggles
 * - RANGE:     anchor + extend-to defines a contiguous range
 */
enum class SelectionMode(
    internal val displayName: String,
    internal val allowsMultiSelect: Boolean,
) {
    NONE("None", allowsMultiSelect = false),
    SINGLE("Single", allowsMultiSelect = false),
    MULTIPLE("Multiple", allowsMultiSelect = true),
    RANGE("Range", allowsMultiSelect = true),
}

/**
 * The scope in which selection operates. Selection is scoped — you
 * cannot select items from two different sections simultaneously.
 *
 * Example: a user starts selection in "Albums" section. They cannot
 * also select items from "Songs" section until they clear and start
 * fresh in "Songs".
 */
@JvmInline
value class SelectionScope(val value: String)

/**
 * Identifies an item being selected. Stable across the lifetime of
 * the item. Typically the domain id (Song.id, Album.id, etc.).
 */
@JvmInline
value class SelectionItemId(val value: Long)

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 2 — SELECTION ANCHOR (for RANGE mode)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The anchor point for range selection. When the user shift-clicks
 * (or long-press-then-taps), the range extends from the anchor to
 * the new target.
 *
 * @property itemId     The anchored item id.
 * @property itemIndex  The anchored item's index in the visible list.
 *                      Required because range selection operates on
 *                      visible indices, not domain ids (the user
 *                      sees a list, not a database).
 * @property scopeId    The scope (section) the anchor lives in.
 */
data class SelectionAnchor(
    val itemId: SelectionItemId,
    val itemIndex: Int,
    val scopeId: SelectionScope,
)

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 3 — SELECTION STATE
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The full selection state of the Library.
 *
 * Rule #79: this state is owned centrally (here), not by individual
 * cards. Cards query `selectedIds.contains(itemId)` to render their
 * selection state.
 *
 * @property mode            Current selection mode.
 * @property scopeId         Active selection scope. Null when mode=NONE.
 * @property selectedIds     All currently-selected item ids (excluding
 *                            pinned ids — those live in `pinnedIds`).
 * @property pinnedIds       Items pinned to selection — survive Clear.
 * @property focusedItemId   A single focused item (keyboard / D-pad
 *                            focus). Distinct from selection.
 * @property anchor          Range anchor. Null unless mode=RANGE.
 * @property lastChangedAtMs Wall-clock of last mutation (for
 *                            Inspector / undo).
 */
data class SelectionState(
    val mode: SelectionMode,
    val scopeId: SelectionScope?,
    val selectedIds: Set<SelectionItemId>,
    val pinnedIds: Set<SelectionItemId>,
    val focusedItemId: SelectionItemId?,
    val anchor: SelectionAnchor?,
    val lastChangedAtMs: Long,
) {
    val isActive: Boolean get() = mode != SelectionMode.NONE
    val isEmpty: Boolean get() = selectedIds.isEmpty() && pinnedIds.isEmpty()
    val totalCount: Int get() = selectedIds.size + pinnedIds.size

    /**
     * All selected ids, including pinned. Useful for batch operations
     * like "delete all selected" — pinned items should be included.
     */
    val allSelectedIds: Set<SelectionItemId> get() = selectedIds + pinnedIds

    /**
     * Is the given item selected (either in selectedIds or pinnedIds)?
     */
    fun isSelected(id: SelectionItemId): Boolean =
        id in selectedIds || id in pinnedIds

    /**
     * Is the given item focused?
     */
    fun isFocused(id: SelectionItemId): Boolean = focusedItemId == id

    /**
     * Is the given item pinned?
     */
    fun isPinned(id: SelectionItemId): Boolean = id in pinnedIds

    companion object {
        val IDLE = SelectionState(
            mode = SelectionMode.NONE,
            scopeId = null,
            selectedIds = emptySet<SelectionItemId>(),
            pinnedIds = emptySet<SelectionItemId>(),
            focusedItemId = null,
            anchor = null,
            lastChangedAtMs = 0L,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 4 — VISIBLE ITEM SCOPE (for RANGE / INVERT)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * A snapshot of the visible items in a scope, with their indices.
 *
 * Required by operations that need to know "what items are visible
 * right now" — e.g. RANGE selection needs to know which items fall
 * between the anchor and the extend-to target.
 *
 * Rule #80: this is a *snapshot of visible items*, not a UI reference.
 * The Runtime does not query the LazyColumn directly — it receives
 * the visible items via this data class.
 *
 * @property scopeId      The scope these items belong to.
 * @property items        Ordered list of visible items with indices.
 */
data class VisibleItemScope(
    val scopeId: SelectionScope,
    val items: List<VisibleItem>,
) {
    /**
     * Get items between [fromIndex] and [toIndex] inclusive.
     */
    fun itemsBetween(fromIndex: Int, toIndex: Int): List<VisibleItem> {
        if (items.isEmpty()) return emptyList()
        val safeFrom = fromIndex.coerceIn(0, items.lastIndex)
        val safeTo = toIndex.coerceIn(0, items.lastIndex)
        val (start, end) = if (safeFrom <= safeTo) safeFrom to safeTo else safeTo to safeFrom
        return items.subList(start, end + 1)
    }

    /**
     * Find the index of an item by id. Returns -1 if not visible.
     */
    fun indexOf(itemId: SelectionItemId): Int =
        items.indexOfFirst { it.id == itemId }
}

/**
 * A single visible item with its index.
 */
data class VisibleItem(
    val id: SelectionItemId,
    val index: Int,
)

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 5 — SELECTION RUNTIME OPERATIONS (PURE)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The Selection Runtime — pure functions for every selection operation.
 *
 * Every function takes the current [SelectionState] (plus any required
 * inputs) and returns a new [SelectionState]. No mutation. No side
 * effects. No I/O.
 *
 * These functions are called by [SelectionReducer] (next file) — the
 * Reducer dispatches Intents and calls these operations.
 */
object SelectionRuntime {

    // ─────────────────────────────────────────────────────────────────
    // SINGLE selection
    // ─────────────────────────────────────────────────────────────────

    /**
     * Select a single item. Replaces any existing selection.
     *
     * - If mode was NONE → transitions to SINGLE.
     * - If mode was SINGLE → replaces selection.
     * - If mode was MULTIPLE → clears multi-select and selects this one.
     * - If mode was RANGE → clears range and selects this one.
     *
     * Pinned ids are preserved.
     */
    fun selectSingle(
        state: SelectionState,
        itemId: SelectionItemId,
        scopeId: SelectionScope,
        nowMs: Long,
    ): SelectionState = state.copy(
        mode = SelectionMode.SINGLE,
        scopeId = scopeId,
        selectedIds = setOf(itemId),
        pinnedIds = state.pinnedIds, // preserved
        anchor = null, // SINGLE has no anchor
        focusedItemId = itemId,
        lastChangedAtMs = nowMs,
    )

    // ─────────────────────────────────────────────────────────────────
    // MULTIPLE selection
    // ─────────────────────────────────────────────────────────────────

    /**
     * Start multi-selection with an initial item.
     *
     * Transitions from NONE/SINGLE → MULTIPLE.
     * The initial item is added to selectedIds (if not already pinned).
     */
    fun startMultiple(
        state: SelectionState,
        itemId: SelectionItemId,
        scopeId: SelectionScope,
        nowMs: Long,
    ): SelectionState {
        val newSelectedIds = if (itemId in state.pinnedIds) {
            state.selectedIds // don't double-add pinned
        } else {
            state.selectedIds + itemId
        }
        return state.copy(
            mode = SelectionMode.MULTIPLE,
            scopeId = scopeId,
            selectedIds = newSelectedIds,
            anchor = null,
            focusedItemId = itemId,
            lastChangedAtMs = nowMs,
        )
    }

    /**
     * Toggle a single item's selection.
     *
     * Only valid in MULTIPLE mode. In other modes, use [selectSingle].
     */
    fun toggle(
        state: SelectionState,
        itemId: SelectionItemId,
        nowMs: Long,
    ): SelectionState {
        if (state.mode != SelectionMode.MULTIPLE) {
            return state.copy(lastChangedAtMs = nowMs)
        }

        return when {
            itemId in state.pinnedIds -> {
                // Toggle off a pinned item
                state.copy(
                    pinnedIds = state.pinnedIds - itemId,
                    focusedItemId = itemId,
                    lastChangedAtMs = nowMs,
                )
            }
            itemId in state.selectedIds -> {
                // Deselect
                state.copy(
                    selectedIds = state.selectedIds - itemId,
                    focusedItemId = itemId,
                    lastChangedAtMs = nowMs,
                )
            }
            else -> {
                // Select
                state.copy(
                    selectedIds = state.selectedIds + itemId,
                    focusedItemId = itemId,
                    lastChangedAtMs = nowMs,
                )
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // RANGE selection
    // ─────────────────────────────────────────────────────────────────

    /**
     * Start a range selection with an anchor.
     *
     * The anchor is the "from" point. Subsequent calls to [extendRange]
     * will select everything between the anchor and the extend-to target.
     */
    fun startRange(
        state: SelectionState,
        itemId: SelectionItemId,
        itemIndex: Int,
        scopeId: SelectionScope,
        nowMs: Long,
    ): SelectionState {
        val anchor = SelectionAnchor(
            itemId = itemId,
            itemIndex = itemIndex,
            scopeId = scopeId,
        )
        return state.copy(
            mode = SelectionMode.RANGE,
            scopeId = scopeId,
            selectedIds = setOf(itemId),
            pinnedIds = state.pinnedIds,
            anchor = anchor,
            focusedItemId = itemId,
            lastChangedAtMs = nowMs,
        )
    }

    /**
     * Extend the range to a new target.
     *
     * Selects all items between the current anchor and [targetIndex]
     * (inclusive). The anchor does NOT move — subsequent calls to
     * extendRange will always extend from the same anchor.
     *
     * Pinned ids are preserved.
     */
    fun extendRange(
        state: SelectionState,
        targetId: SelectionItemId,
        targetIndex: Int,
        visibleScope: VisibleItemScope,
        nowMs: Long,
    ): SelectionState {
        val anchor = state.anchor ?: return state.copy(lastChangedAtMs = nowMs)
        if (anchor.scopeId != visibleScope.scopeId) {
            // Cross-scope range extension is not allowed
            return state.copy(lastChangedAtMs = nowMs)
        }

        val itemsInRange = visibleScope.itemsBetween(
            fromIndex = anchor.itemIndex,
            toIndex = targetIndex,
        )
        val idsInRange = itemsInRange.map { it.id }.toSet()

        return state.copy(
            mode = SelectionMode.RANGE,
            selectedIds = idsInRange,
            pinnedIds = state.pinnedIds,
            anchor = anchor, // anchor does not move
            focusedItemId = targetId,
            lastChangedAtMs = nowMs,
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // INVERT
    // ─────────────────────────────────────────────────────────────────

    /**
     * Invert the selection within the current scope.
     *
     * Selected items become deselected; deselected items become selected.
     * Pinned items are preserved.
     *
     * Requires a [VisibleItemScope] to know what "all items" means.
     */
    fun invert(
        state: SelectionState,
        visibleScope: VisibleItemScope,
        nowMs: Long,
    ): SelectionState {
        if (state.scopeId != visibleScope.scopeId) return state.copy(lastChangedAtMs = nowMs)

        val allVisibleIds = visibleScope.items.map { it.id }.toSet()
        val inverted = allVisibleIds - state.selectedIds - state.pinnedIds

        // Force MULTIPLE mode if not already
        val newMode = if (inverted.isEmpty()) SelectionMode.NONE else SelectionMode.MULTIPLE

        return state.copy(
            mode = newMode,
            selectedIds = inverted,
            pinnedIds = state.pinnedIds,
            anchor = null,
            focusedItemId = state.focusedItemId,
            lastChangedAtMs = nowMs,
        )
    }

    /**
     * Select all visible items in the current scope.
     */
    fun selectAll(
        state: SelectionState,
        visibleScope: VisibleItemScope,
        nowMs: Long,
    ): SelectionState {
        if (state.scopeId != visibleScope.scopeId) return state.copy(lastChangedAtMs = nowMs)

        val allVisibleIds = visibleScope.items.map { it.id }.toSet()
        val newSelectedIds = allVisibleIds - state.pinnedIds

        return state.copy(
            mode = if (newSelectedIds.isEmpty()) state.mode else SelectionMode.MULTIPLE,
            selectedIds = newSelectedIds,
            pinnedIds = state.pinnedIds,
            anchor = null,
            focusedItemId = state.focusedItemId,
            lastChangedAtMs = nowMs,
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // CLEAR
    // ─────────────────────────────────────────────────────────────────

    /**
     * Clear the selection.
     *
     * @property clearPinned  If true, also clears pinned items. Default
     *                         false — pinned items survive Clear by
     *                         design.
     */
    fun clear(
        state: SelectionState,
        nowMs: Long,
        clearPinned: Boolean = false,
    ): SelectionState = state.copy(
        mode = SelectionMode.NONE,
        scopeId = null,
        selectedIds = emptySet<SelectionItemId>(),
        pinnedIds = if (clearPinned) emptySet<SelectionItemId>() else state.pinnedIds,
        anchor = null,
        focusedItemId = null,
        lastChangedAtMs = nowMs,
    )

    // ─────────────────────────────────────────────────────────────────
    // PINNED
    // ─────────────────────────────────────────────────────────────────

    /**
     * Pin all currently-selected items.
     *
     * Pinned items:
     *   - Survive Clear (unless clearPinned=true is passed)
     *   - Are visually distinguished (lock icon)
     *   - Are included in batch operations
     */
    fun pinSelected(
        state: SelectionState,
        nowMs: Long,
    ): SelectionState {
        val newPinnedIds = state.pinnedIds + state.selectedIds
        return state.copy(
            pinnedIds = newPinnedIds,
            selectedIds = emptySet<SelectionItemId>(),
            // Stay in current mode — user may continue selecting
            mode = if (state.mode == SelectionMode.SINGLE) SelectionMode.NONE else state.mode,
            lastChangedAtMs = nowMs,
        )
    }

    /**
     * Unpin a specific item.
     */
    fun unpin(
        state: SelectionState,
        itemId: SelectionItemId,
        nowMs: Long,
    ): SelectionState = state.copy(
        pinnedIds = state.pinnedIds - itemId,
        lastChangedAtMs = nowMs,
    )

    /**
     * Unpin all pinned items.
     */
    fun unpinAll(
        state: SelectionState,
        nowMs: Long,
    ): SelectionState = state.copy(
        pinnedIds = emptySet<SelectionItemId>(),
        lastChangedAtMs = nowMs,
    )

    // ─────────────────────────────────────────────────────────────────
    // FOCUSED
    // ─────────────────────────────────────────────────────────────────

    /**
     * Set the focused item. Distinct from selection — used for
     * keyboard / D-pad / TalkBack focus.
     *
     * Does NOT change selection. Just sets the focus pointer.
     */
    fun focus(
        state: SelectionState,
        itemId: SelectionItemId?,
        nowMs: Long,
    ): SelectionState = state.copy(
        focusedItemId = itemId,
        lastChangedAtMs = nowMs,
    )

    /**
     * Move focus by an offset. Used for D-pad navigation.
     *
     * @property delta  +1 = next item, -1 = previous item.
     * @property visibleScope  The visible items to navigate within.
     */
    fun moveFocus(
        state: SelectionState,
        delta: Int,
        visibleScope: VisibleItemScope,
        nowMs: Long,
    ): SelectionState {
        if (visibleScope.items.isEmpty()) return state.copy(lastChangedAtMs = nowMs)

        val currentIndex = state.focusedItemId?.let { id ->
            visibleScope.indexOf(id)
        } ?: -1

        val newIndex = when {
            currentIndex < 0 -> 0
            delta > 0 -> (currentIndex + delta).coerceAtMost(visibleScope.items.lastIndex)
            delta < 0 -> (currentIndex + delta).coerceAtLeast(0)
            else -> currentIndex
        }

        val newFocusedId = visibleScope.items.getOrNull(newIndex)?.id
        return state.copy(
            focusedItemId = newFocusedId,
            lastChangedAtMs = nowMs,
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // SCOPE TRANSITION
    // ─────────────────────────────────────────────────────────────────

    /**
     * Transition to a new scope. Selections from the old scope are
     * cleared (pinned items survive, since they are scope-independent).
     *
     * This is called when the user navigates to a different section
     * of the Library (e.g. from "Albums" to "Songs").
     */
    fun transitionScope(
        state: SelectionState,
        newScopeId: SelectionScope,
        nowMs: Long,
    ): SelectionState {
        if (state.scopeId == newScopeId) return state.copy(lastChangedAtMs = nowMs)

        return state.copy(
            mode = SelectionMode.NONE,
            scopeId = null, // will be set when user starts selecting in new scope
            selectedIds = emptySet<SelectionItemId>(),
            pinnedIds = state.pinnedIds, // preserved across scopes
            anchor = null,
            focusedItemId = null,
            lastChangedAtMs = nowMs,
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // QUERY HELPERS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Get the count of selected items (including pinned).
     */
    fun selectedCount(state: SelectionState): Int = state.totalCount

    /**
     * Check if any items are selected (including pinned).
     */
    fun hasSelection(state: SelectionState): Boolean = state.totalCount > 0

    /**
     * Check if the selection is in a state where batch operations
     * (delete, share, add-to-playlist) make sense.
     */
    fun canPerformBatchAction(state: SelectionState): Boolean =
        state.totalCount > 0

    /**
     * Get all selected item ids as a flat list (selectedIds + pinnedIds).
     */
    fun allSelectedIds(state: SelectionState): List<SelectionItemId> =
        state.allSelectedIds.toList()

    /**
     * Check if the current selection is homogeneous (all items are
     * the same type — e.g. all songs, or all albums).
     *
     * Heterogeneous selections limit available actions (e.g. you
     * can't "Add to Playlist" a mix of songs and albums).
     *
     * This is a *hint* — the actual type-checking happens in the
     * Reducer, which has access to the domain layer. The Runtime
     * can only check if all ids are in the same scope.
     */
    fun isHomogeneousScope(state: SelectionState): Boolean =
        state.scopeId != null
}
