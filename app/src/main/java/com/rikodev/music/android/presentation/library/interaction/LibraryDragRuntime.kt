/*
 * RIKO EXPERIENCE OS — LIBRARY INTERACTION RUNTIME
 * Build B5.4 — Library Interaction Runtime
 *
 * File 9 / 15 — LibraryDragRuntime.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Define the state machine for drag operations — reorder,
 * swipe-to-dismiss, and free-form drag. Pure: same inputs → same
 * outputs, byte-identical.
 *
 * DRAG LIFECYCLE
 * --------------
 *   IDLE ──(drag started)──→ PENDING
 *   PENDING ──(exceeded start threshold)──→ ACTIVE
 *   PENDING ──(released before threshold)──→ IDLE (was a tap)
 *   ACTIVE ──(hover over new index)──→ ACTIVE (with updated hover)
 *   ACTIVE ──(released)──→ SETTLING
 *   SETTLING ──(animation done)──→ COMMITTED
 *   ACTIVE ──(cancelled)──→ CANCELLED
 *   CANCELLED ──(cleanup done)──→ IDLE
 *
 * DRAG KINDS
 * ----------
 *   REORDER         — long-press-then-drag in a list. Reorders items.
 *   SWIPE_TO_DISMISS— horizontal drag on a card. Reveals dismiss action.
 *   FREE_FORM       — drag without specific semantic (rare).
 *
 * RULES ENFORCED HERE
 * -------------------
 * #78  Interaction Creates Intent, Never Action — drag updates produce
 *      Intents, not Actions.
 * #80  Gesture Never Knows UI — DragRuntime does not reference the
 *      LazyColumn. It receives a `DragScope` (snapshot of visible items)
 *      and computes hover index from coordinates.
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
// SECTION 1 — DRAG LIFECYCLE
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The lifecycle state of a drag operation.
 */
enum class DragLifecycle(
    internal val displayName: String,
    internal val isActive: Boolean,
    internal val isVisible: Boolean,
) {
    /** No drag in progress. */
    IDLE("Idle", isActive = false, isVisible = false),

    /** Drag has started but has not exceeded the start threshold. */
    PENDING("Pending", isActive = true, isVisible = false),

    /** Drag is active — finger is moving, visual feedback shown. */
    ACTIVE("Active", isActive = true, isVisible = true),

    /** Drag released — settling animation in progress. */
    SETTLING("Settling", isActive = true, isVisible = true),

    /** Drag committed — reorder / dismiss applied. */
    COMMITTED("Committed", isActive = false, isVisible = false),

    /** Drag cancelled — rolling back visual state. */
    CANCELLED("Cancelled", isActive = false, isVisible = true);

    val isTerminal: Boolean get() = this == COMMITTED
}

/**
 * Kind of drag.
 */
enum class DragKind(
    internal val displayName: String,
) {
    /** Long-press-then-drag in a list. Reorders items. */
    REORDER("Reorder"),

    /** Horizontal drag on a card. Reveals dismiss action. */
    SWIPE_TO_DISMISS("Swipe to Dismiss"),

    /** Free-form drag (rare, e.g. dragging artwork to dock). */
    FREE_FORM("Free Form"),
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 2 — DRAG THRESHOLDS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Threshold specifications for drag operations.
 *
 * @property startThresholdDp       Min distance (dp) before a PENDING
 *                                   drag becomes ACTIVE.
 * @property commitVelocityDpPerSec  Min velocity (dp/s) for a release
 *                                   to commit (for swipe-to-dismiss).
 * @property dismissThresholdDp      Min distance (dp) for swipe-to-dismiss
 *                                   to commit without velocity.
 * @property hoverSnapDistanceDp     Distance (dp) from item center
 *                                   before hover index updates.
 * @property longPressRequiredMs    Min long-press duration before
 *                                   reorder drag can start.
 */
data class DragThresholdSpec(
    val startThresholdDp: Dp,
    val commitVelocityDpPerSec: DpPerSec,
    val dismissThresholdDp: Dp,
    val hoverSnapDistanceDp: Dp,
    val longPressRequiredMs: Millis,
) {
    init {
        require(startThresholdDp.value > 0) { "startThresholdDp must be positive" }
        require(commitVelocityDpPerSec.value >= 0) { "commitVelocityDpPerSec must be non-negative" }
        require(dismissThresholdDp.value > 0) { "dismissThresholdDp must be positive" }
        require(hoverSnapDistanceDp.value > 0) { "hoverSnapDistanceDp must be positive" }
        require(longPressRequiredMs.value >= 0) { "longPressRequiredMs must be non-negative" }
    }

    companion object {
        val DEFAULT = DragThresholdSpec(
            startThresholdDp = Dp(16),
            commitVelocityDpPerSec = DpPerSec(600),
            dismissThresholdDp = Dp(96),
            hoverSnapDistanceDp = Dp(24),
            longPressRequiredMs = Millis(400),
        )

        /** Tighter thresholds for power users. */
        val TIGHT = DragThresholdSpec(
            startThresholdDp = Dp(12),
            commitVelocityDpPerSec = DpPerSec(800),
            dismissThresholdDp = Dp(72),
            hoverSnapDistanceDp = Dp(16),
            longPressRequiredMs = Millis(280),
        )

        /** Looser thresholds for accessibility. */
        val FORGIVING = DragThresholdSpec(
            startThresholdDp = Dp(24),
            commitVelocityDpPerSec = DpPerSec(400),
            dismissThresholdDp = Dp(120),
            hoverSnapDistanceDp = Dp(36),
            longPressRequiredMs = Millis(600),
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 3 — DRAG TARGET + SCOPE
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The target of a drag operation.
 *
 * @property targetId         Stable id of the dragged element.
 * @property targetType       Type of the target.
 * @property targetItemId     Domain id of the underlying item.
 * @property sourceIndex      Original index in the list (for reorder).
 * @property startPosition    Where the drag started (screen pixels).
 */
data class DragTarget(
    val targetId: String,
    val targetType: GestureTargetType,
    val targetItemId: Long,
    val sourceIndex: Int,
    val startPosition: GesturePoint,
)

/**
 * A snapshot of the items in a draggable list.
 *
 * Rule #80: this is a *snapshot*, not a UI reference. The host (B5.5)
 * builds this from the current LazyColumn state and passes it in.
 *
 * @property items            Ordered list of visible draggable items.
 * @property itemBounds       Per-item bounds (screen pixels) for hover
 *                             detection.
 */
data class DragScope(
    val items: List<DraggableItem>,
    val itemBounds: Map<String, GestureBounds>,
) {
    val isEmpty: Boolean get() = items.isEmpty()

    /**
     * Find the item currently being hovered by [position].
     *
     * Returns null if the position is not over any item.
     */
    fun hoveredItemFor(position: GesturePoint): DraggableItem? {
        return items.firstOrNull { item ->
            itemBounds[item.id]?.contains(position) == true
        }
    }

    /**
     * Get the index of an item by id. Returns -1 if not found.
     */
    fun indexOf(itemId: String): Int =
        items.indexOfFirst { it.id == itemId }
}

/**
 * A single draggable item.
 */
data class DraggableItem(
    val id: String,
    val index: Int,
    val isReorderable: Boolean,
    val isDismissable: Boolean,
)

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 4 — DRAG STATE
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The full state of a drag operation.
 *
 * @property lifecycle        Current lifecycle state.
 * @property kind             Drag kind (reorder / swipe / free-form).
 * @property target           The dragged target.
 * @property currentPosition  Current finger position.
 * @property currentHoverId   Id of the item currently being hovered
 *                             (for reorder preview). Null if not hovering
 *                             any item.
 * @property currentHoverIndex  Index of the hovered item. -1 if none.
 * @property totalDelta       Accumulated delta from start to current.
 * @property lastDelta        Delta since the previous Drag event.
 * @property direction        Dominant drag direction.
 * @property threshold        Active threshold spec.
 * @property activeGestureId  Id of the gesture driving the drag.
 * @property startedAtMs      Wall-clock start time.
 * @property lastUpdatedMs    Wall-clock of last update.
 */
data class DragState(
    val lifecycle: DragLifecycle,
    val kind: DragKind,
    val target: DragTarget?,
    val currentPosition: GesturePoint,
    val currentHoverId: String?,
    val currentHoverIndex: Int,
    val totalDelta: GestureVector,
    val lastDelta: GestureVector,
    val direction: GestureDirection,
    val threshold: DragThresholdSpec,
    val activeGestureId: String?,
    val startedAtMs: Long,
    val lastUpdatedMs: Long,
) {
    val isActive: Boolean get() = lifecycle.isActive
    val isVisible: Boolean get() = lifecycle.isVisible
    val isReorder: Boolean get() = kind == DragKind.REORDER
    val isSwipeToDismiss: Boolean get() = kind == DragKind.SWIPE_TO_DISMISS
    val totalMovementDp: Dp get() = Dp(totalDelta.magnitude.value) // simplified; assumes 1px ≈ 1dp

    companion object {
        val IDLE = DragState(
            lifecycle = DragLifecycle.IDLE,
            kind = DragKind.FREE_FORM,
            target = null,
            currentPosition = GesturePoint.ZERO,
            currentHoverId = null,
            currentHoverIndex = -1,
            totalDelta = GestureVector.ZERO,
            lastDelta = GestureVector.ZERO,
            direction = GestureDirection.NONE,
            threshold = DragThresholdSpec.DEFAULT,
            activeGestureId = null,
            startedAtMs = 0L,
            lastUpdatedMs = 0L,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 5 — DRAG RUNTIME (PURE)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The Library Drag Runtime — pure functions for every drag transition.
 */
object LibraryDragRuntime {

    /**
     * Begin a pending drag.
     *
     * Transitions IDLE → PENDING. Sets up the drag target and
     * initial position. Does NOT make the drag visible yet — that
     * happens when the start threshold is exceeded (see [activate]).
     */
    fun beginDrag(
        state: DragState,
        gesture: LibraryGesture.Drag,
        target: DragTarget,
        threshold: DragThresholdSpec,
        nowMs: Long,
    ): DragState {
        if (state.isActive) return state // another drag is in progress

        val kind = when {
            gesture.isReorder -> DragKind.REORDER
            gesture.isSwipeToDismiss -> DragKind.SWIPE_TO_DISMISS
            else -> DragKind.FREE_FORM
        }

        return state.copy(
            lifecycle = DragLifecycle.PENDING,
            kind = kind,
            target = target,
            currentPosition = gesture.currentPosition,
            currentHoverId = null,
            currentHoverIndex = -1,
            totalDelta = gesture.totalDelta,
            lastDelta = gesture.lastDelta,
            direction = gesture.direction,
            threshold = threshold,
            activeGestureId = gesture.id,
            startedAtMs = nowMs,
            lastUpdatedMs = nowMs,
        )
    }

    /**
     * Update an in-progress drag.
     *
     * - If PENDING and movement exceeds start threshold → transitions to ACTIVE.
     * - If ACTIVE → updates position, hover, delta, direction.
     *
     * Returns the new state. If the gesture id does not match
     * `activeGestureId`, this is a no-op.
     */
    fun updateDrag(
        state: DragState,
        gesture: LibraryGesture.Drag,
        dragScope: DragScope,
        nowMs: Long,
    ): DragState {
        if (state.activeGestureId != gesture.id) return state
        if (!state.isActive) return state

        val newLifecycle = when (state.lifecycle) {
            DragLifecycle.PENDING -> {
                // Check if movement exceeds start threshold
                val movementDp = Dp(gesture.totalDelta.magnitude.value)
                if (movementDp.value >= state.threshold.startThresholdDp.value) {
                    DragLifecycle.ACTIVE
                } else {
                    DragLifecycle.PENDING
                }
            }
            else -> state.lifecycle
        }

        // Compute hover (only for reorder drags)
        val newHoverId: String?
        val newHoverIndex: Int
        if (state.isReorder && newLifecycle == DragLifecycle.ACTIVE) {
            val hovered = dragScope.hoveredItemFor(gesture.currentPosition)
            newHoverId = hovered?.id
            newHoverIndex = hovered?.index ?: -1
        } else {
            newHoverId = state.currentHoverId
            newHoverIndex = state.currentHoverIndex
        }

        return state.copy(
            lifecycle = newLifecycle,
            currentPosition = gesture.currentPosition,
            currentHoverId = newHoverId,
            currentHoverIndex = newHoverIndex,
            totalDelta = gesture.totalDelta,
            lastDelta = gesture.lastDelta,
            direction = gesture.direction,
            lastUpdatedMs = nowMs,
        )
    }

    /**
     * End a drag (user released finger).
     *
     * - If PENDING → transitions to IDLE (was a tap, not a drag).
     * - If ACTIVE → transitions to SETTLING (UI plays settle animation).
     *
     * Returns the new state plus a [DragOutcome] describing what
     * should happen (reorder commit, dismiss commit, no-op).
     */
    fun endDrag(
        state: DragState,
        endPosition: GesturePoint,
        velocity: GestureVector,
        nowMs: Long,
    ): Pair<DragState, DragOutcome> {
        if (!state.isActive) return state to DragOutcome.NoOp

        val target = state.target ?: return state.copy(
            lifecycle = DragLifecycle.CANCELLED,
            lastUpdatedMs = nowMs,
        ) to DragOutcome.Cancelled("No target")

        return when (state.lifecycle) {
            DragLifecycle.PENDING -> {
                // Released before threshold — was a tap, not a drag
                state.copy(
                    lifecycle = DragLifecycle.IDLE,
                    currentPosition = endPosition,
                    lastUpdatedMs = nowMs,
                ) to DragOutcome.NoOp
            }

            DragLifecycle.ACTIVE -> {
                val newState = state.copy(
                    lifecycle = DragLifecycle.SETTLING,
                    currentPosition = endPosition,
                    lastUpdatedMs = nowMs,
                )

                val outcome = when (state.kind) {
                    DragKind.REORDER -> {
                        if (state.currentHoverIndex >= 0 &&
                            state.currentHoverIndex != target.sourceIndex
                        ) {
                            DragOutcome.ReorderCommitted(
                                targetId = target.targetId,
                                fromIndex = target.sourceIndex,
                                toIndex = state.currentHoverIndex,
                            )
                        } else {
                            DragOutcome.NoOp
                        }
                    }

                    DragKind.SWIPE_TO_DISMISS -> {
                        val distanceDp = Dp(state.totalDelta.magnitude.value)
                        val velocityDpPerSec = DpPerSec(velocity.magnitude.value)
                        val shouldDismiss = distanceDp.value >= state.threshold.dismissThresholdDp.value ||
                                velocityDpPerSec.value >= state.threshold.commitVelocityDpPerSec.value

                        if (shouldDismiss) {
                            DragOutcome.DismissCommitted(
                                targetId = target.targetId,
                            )
                        } else {
                            DragOutcome.NoOp
                        }
                    }

                    DragKind.FREE_FORM -> DragOutcome.NoOp
                }

                newState to outcome
            }

            else -> state to DragOutcome.NoOp
        }
    }

    /**
     * Cancel a drag (parent intercepted, system gesture, etc.).
     */
    fun cancelDrag(
        state: DragState,
        reason: DragCancelReason,
        nowMs: Long,
    ): DragState {
        if (!state.isActive) return state

        return state.copy(
            lifecycle = DragLifecycle.CANCELLED,
            lastUpdatedMs = nowMs,
        )
    }

    /**
     * Complete the settle animation — transition SETTLING → COMMITTED.
     */
    fun completeSettle(
        state: DragState,
        nowMs: Long,
    ): DragState {
        if (state.lifecycle != DragLifecycle.SETTLING) return state

        return state.copy(
            lifecycle = DragLifecycle.COMMITTED,
            lastUpdatedMs = nowMs,
        )
    }

    /**
     * Complete the cancel cleanup — transition CANCELLED → IDLE.
     */
    fun completeCancel(
        state: DragState,
        nowMs: Long,
    ): DragState {
        if (state.lifecycle != DragLifecycle.CANCELLED) return state

        return DragState.IDLE.copy(
            lastUpdatedMs = nowMs,
        )
    }

    /**
     * Reset to IDLE after COMMITTED (UI has applied the reorder).
     */
    fun resetToIdle(
        state: DragState,
        nowMs: Long,
    ): DragState = DragState.IDLE.copy(
        lastUpdatedMs = nowMs,
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 6 — DRAG OUTCOME
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The outcome of ending a drag.
 *
 * - NoOp: drag was too small / not actionable. UI rolls back.
 * - ReorderCommitted: user dragged an item to a new position. UI
 *   should commit the reorder.
 * - DismissCommitted: user swiped an item past the dismiss threshold.
 *   UI should commit the dismiss.
 * - Cancelled: drag was cancelled. UI rolls back.
 */
sealed class DragOutcome {
    data object NoOp : DragOutcome()

    data class ReorderCommitted(
        val targetId: String,
        val fromIndex: Int,
        val toIndex: Int,
    ) : DragOutcome()

    data class DismissCommitted(
        val targetId: String,
    ) : DragOutcome()

    data class Cancelled(
        val reason: String,
    ) : DragOutcome()
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 7 — DRAG INTENT (Rule #78)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Intents emitted by drag operations. Consumed by a drag-aware
 * Reducer that updates [DragState] via [LibraryDragRuntime].
 */
sealed class DragIntent {
    data class Started(
        val target: DragTarget,
        val threshold: DragThresholdSpec,
        val sourceGestureId: String,
    ) : DragIntent()

    data class Updated(
        val gesture: LibraryGesture.Drag,
        val dragScope: DragScope,
    ) : DragIntent()

    data class Ended(
        val endPosition: GesturePoint,
        val velocity: GestureVector,
    ) : DragIntent()

    data class Cancelled(
        val reason: DragCancelReason,
    ) : DragIntent()

    data object SettleCompleted : DragIntent()
    data object CancelCompleted : DragIntent()
    data object ReorderApplied : DragIntent()
}

/**
 * Actions emitted by the drag Reducer.
 */
sealed class DragAction {
    data class ReorderItems(
        val targetId: String,
        val fromIndex: Int,
        val toIndex: Int,
    ) : DragAction()

    data class DismissItem(
        val targetId: String,
    ) : DragAction()

    data object DragStartedHaptic : DragAction()
    data object ReorderHoverHaptic : DragAction()
    data object ReorderCommittedHaptic : DragAction()
    data object DismissCommittedHaptic : DragAction()
    data object DragCancelledHaptic : DragAction()
}
