/*
 * RIKO EXPERIENCE OS — LIBRARY INTERACTION RUNTIME
 * Build B5.4 — Library Interaction Runtime
 *
 * File 10 / 15 — LongPressRuntime.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Define the state machine for long-press recognition. Pure: same
 * inputs → same outputs, byte-identical.
 *
 * LONG-PRESS LIFECYCLE
 * --------------------
 *   IDLE ──(touch down)──→ PENDING
 *   PENDING ──(duration >= minDurationMs AND movement <= maxMovementDp)──→ FIRED
 *   PENDING ──(movement > maxMovementDp)──→ CANCELLED
 *   PENDING ──(released before minDurationMs)──→ IDLE (was a tap)
 *   FIRED ──(released)──→ COMPLETED
 *   FIRED ──(movement > maxMovementDp after fire)──→ CANCELLED
 *   CANCELLED ──(cleanup)──→ IDLE
 *
 * The long-press fires AT the threshold crossing — not at finger-up.
 * This is what makes it feel responsive. The haptic fires at the same
 * moment.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #78  Interaction Creates Intent, Never Action — long-press events
 *      produce Intents, not Actions.
 * #80  Gesture Never Knows UI — LongPressRuntime does not reference
 *      any Composable. It only models state.
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
// SECTION 1 — LONG-PRESS LIFECYCLE
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The lifecycle state of a long-press recognition.
 */
enum class LongPressLifecycle(
    internal val displayName: String,
    internal val isPending: Boolean,
    internal val isFired: Boolean,
) {
    /** No long-press in progress. */
    IDLE("Idle", isPending = false, isFired = false),

    /** Touch-down received, waiting for threshold to be reached. */
    PENDING("Pending", isPending = true, isFired = false),

    /** Long-press threshold reached — haptic should fire. */
    FIRED("Fired", isPending = false, isFired = true),

    /** Long-press completed (finger lifted after firing). */
    COMPLETED("Completed", isPending = false, isFired = false),

    /** Long-press cancelled (movement exceeded threshold or parent
     *  intercepted). */
    CANCELLED("Cancelled", isPending = false, isFired = false);

    val isTerminal: Boolean get() = this == COMPLETED
    val isActive: Boolean get() = this == PENDING || this == FIRED
}

/**
 * Reason a long-press was cancelled.
 */
enum class LongPressCancelReason {
    /** Movement exceeded maxMovementDp. */
    MOVEMENT_EXCEEDED,

    /** Parent intercepted the gesture. */
    PARENT_INTERCEPTED,

    /** System gesture took over. */
    SYSTEM_GESTURE,

    /** Finger lifted before threshold (was a tap). */
    RELEASED_BEFORE_THRESHOLD,

    /** Programmatic cancellation (screen change, state reset). */
    PROGRAMMATIC,
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 2 — LONG-PRESS SPEC
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Long-press threshold specifications.
 *
 * @property minDurationMs       Min duration (ms) before long-press fires.
 * @property maxMovementDp       Max movement (dp) allowed during the press.
 * @property hapticEnabled       True if haptic feedback fires at threshold.
 * @property postFireMovementDp  Max movement (dp) allowed AFTER the long-press
 *                                has fired (before it cancels). Typically
 *                                larger than maxMovementDp because the user
 *                                may start dragging.
 */
data class LongPressSpec(
    val minDurationMs: Millis,
    val maxMovementDp: Dp,
    val hapticEnabled: Boolean,
    val postFireMovementDp: Dp,
) {
    init {
        require(minDurationMs.value > 0) { "minDurationMs must be positive" }
        require(maxMovementDp.value > 0) { "maxMovementDp must be positive" }
        require(postFireMovementDp.value >= maxMovementDp.value) {
            "postFireMovementDp must be >= maxMovementDp"
        }
    }

    companion object {
        val DEFAULT = LongPressSpec(
            minDurationMs = Millis(400),
            maxMovementDp = Dp(16),
            hapticEnabled = true,
            postFireMovementDp = Dp(48),
        )

        /** Shorter long-press — for power users. */
        val QUICK = LongPressSpec(
            minDurationMs = Millis(280),
            maxMovementDp = Dp(16),
            hapticEnabled = true,
            postFireMovementDp = Dp(40),
        )

        /** Longer long-press — for accessibility (gives more time). */
        val FORGIVING = LongPressSpec(
            minDurationMs = Millis(600),
            maxMovementDp = Dp(24),
            hapticEnabled = true,
            postFireMovementDp = Dp(64),
        )

        /** No haptic — for silent mode. */
        val SILENT = LongPressSpec(
            minDurationMs = Millis(400),
            maxMovementDp = Dp(16),
            hapticEnabled = false,
            postFireMovementDp = Dp(48),
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 3 — LONG-PRESS STATE
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The full state of a long-press recognition.
 *
 * @property lifecycle          Current lifecycle state.
 * @property spec               Active long-press spec.
 * @property startPosition      Where the press started.
 * @property currentPosition    Current finger position.
 * @property startedAtMs        Wall-clock of touch-down.
 * @property lastUpdatedMs      Wall-clock of last update.
 * @property totalMovementDp    Accumulated movement (dp) since start.
 * @property firedAtMs          Wall-clock when long-press fired. -1 if not fired.
 * @property hapticFired        True if haptic feedback was emitted.
 * @property activeGestureId    Id of the gesture driving the long-press.
 * @property targetId           Stable id of the target element.
 */
data class LongPressState(
    val lifecycle: LongPressLifecycle,
    val spec: LongPressSpec,
    val startPosition: GesturePoint,
    val currentPosition: GesturePoint,
    val startedAtMs: Long,
    val lastUpdatedMs: Long,
    val totalMovementDp: Dp,
    val firedAtMs: Long,
    val hapticFired: Boolean,
    val activeGestureId: String?,
    val targetId: String?,
) {
    val isActive: Boolean get() = lifecycle.isActive
    val isFired: Boolean get() = lifecycle == LongPressLifecycle.FIRED
    val isCompleted: Boolean get() = lifecycle == LongPressLifecycle.COMPLETED
    val isCancelled: Boolean get() = lifecycle == LongPressLifecycle.CANCELLED
    val durationMs: Millis get() = Millis(lastUpdatedMs - startedAtMs)

    companion object {
        val IDLE = LongPressState(
            lifecycle = LongPressLifecycle.IDLE,
            spec = LongPressSpec.DEFAULT,
            startPosition = GesturePoint.ZERO,
            currentPosition = GesturePoint.ZERO,
            startedAtMs = 0L,
            lastUpdatedMs = 0L,
            totalMovementDp = Dp(0),
            firedAtMs = -1L,
            hapticFired = false,
            activeGestureId = null,
            targetId = null,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 4 — LONG-PRESS RUNTIME (PURE)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The Long-Press Runtime — pure functions for every long-press transition.
 */
object LongPressRuntime {

    /**
     * Begin a long-press recognition.
     *
     * Transitions IDLE → PENDING. Records the start position and time.
     */
    fun beginPress(
        state: LongPressState,
        position: GesturePoint,
        spec: LongPressSpec,
        targetId: String?,
        gestureId: String,
        nowMs: Long,
    ): LongPressState {
        if (state.isActive) return state

        return LongPressState(
            lifecycle = LongPressLifecycle.PENDING,
            spec = spec,
            startPosition = position,
            currentPosition = position,
            startedAtMs = nowMs,
            lastUpdatedMs = nowMs,
            totalMovementDp = Dp(0),
            firedAtMs = -1L,
            hapticFired = false,
            activeGestureId = gestureId,
            targetId = targetId,
        )
    }

    /**
     * Update an in-progress long-press with new position and time.
     *
     * - If PENDING and duration >= minDurationMs AND movement <= maxMovementDp
     *   → transitions to FIRED.
     * - If PENDING and movement > maxMovementDp → transitions to CANCELLED.
     * - If FIRED and movement > postFireMovementDp → transitions to CANCELLED.
     *
     * Returns the new state plus a [LongPressEvent] describing what
     * happened (e.g. haptic should fire, long-press cancelled).
     */
    fun update(
        state: LongPressState,
        currentPosition: GesturePoint,
        nowMs: Long,
    ): Pair<LongPressState, LongPressEvent> {
        if (!state.isActive) return state to LongPressEvent.NoOp

        val movementPx = state.startPosition.distanceTo(currentPosition).value
        val newMovementDp = Dp(movementPx) // simplified
        val durationMs = nowMs - state.startedAtMs

        val newState = state.copy(
            currentPosition = currentPosition,
            lastUpdatedMs = nowMs,
            totalMovementDp = if (newMovementDp.value > state.totalMovementDp.value) {
                newMovementDp
            } else {
                state.totalMovementDp
            },
        )

        return when (state.lifecycle) {
            LongPressLifecycle.PENDING -> {
                when {
                    newMovementDp.value > state.spec.maxMovementDp.value -> {
                        // Movement exceeded — cancel
                        newState.copy(lifecycle = LongPressLifecycle.CANCELLED) to
                            LongPressEvent.Cancelled(LongPressCancelReason.MOVEMENT_EXCEEDED)
                    }

                    durationMs >= state.spec.minDurationMs.value -> {
                        // Threshold reached — fire
                        val firedState = newState.copy(
                            lifecycle = LongPressLifecycle.FIRED,
                            firedAtMs = nowMs,
                            hapticFired = state.spec.hapticEnabled,
                        )
                        val event = if (state.spec.hapticEnabled) {
                            LongPressEvent.FiredWithHaptic
                        } else {
                            LongPressEvent.Fired
                        }
                        firedState to event
                    }

                    else -> newState to LongPressEvent.NoOp
                }
            }

            LongPressLifecycle.FIRED -> {
                // Check if movement exceeds post-fire threshold
                if (newMovementDp.value > state.spec.postFireMovementDp.value) {
                    newState.copy(lifecycle = LongPressLifecycle.CANCELLED) to
                        LongPressEvent.Cancelled(LongPressCancelReason.MOVEMENT_EXCEEDED)
                } else {
                    newState to LongPressEvent.NoOp
                }
            }

            else -> newState to LongPressEvent.NoOp
        }
    }

    /**
     * Complete the long-press (finger lifted).
     *
     * - If FIRED → transitions to COMPLETED. Emits [LongPressEvent.Completed].
     * - If PENDING → transitions to IDLE (was a tap, not a long-press).
     *   Emits [LongPressEvent.ReleasedBeforeThreshold].
     * - Otherwise → no-op.
     */
    fun complete(
        state: LongPressState,
        nowMs: Long,
    ): Pair<LongPressState, LongPressEvent> {
        return when (state.lifecycle) {
            LongPressLifecycle.FIRED -> {
                state.copy(
                    lifecycle = LongPressLifecycle.COMPLETED,
                    lastUpdatedMs = nowMs,
                ) to LongPressEvent.Completed
            }

            LongPressLifecycle.PENDING -> {
                state.copy(
                    lifecycle = LongPressLifecycle.IDLE,
                    lastUpdatedMs = nowMs,
                ) to LongPressEvent.ReleasedBeforeThreshold
            }

            else -> state to LongPressEvent.NoOp
        }
    }

    /**
     * Cancel the long-press (parent intercepted, system gesture, etc.).
     */
    fun cancel(
        state: LongPressState,
        reason: LongPressCancelReason,
        nowMs: Long,
    ): Pair<LongPressState, LongPressEvent> {
        if (!state.isActive) return state to LongPressEvent.NoOp

        return state.copy(
            lifecycle = LongPressLifecycle.CANCELLED,
            lastUpdatedMs = nowMs,
        ) to LongPressEvent.Cancelled(reason)
    }

    /**
     * Reset to IDLE after COMPLETED or CANCELLED.
     */
    fun reset(nowMs: Long): LongPressState =
        LongPressState.IDLE.copy(lastUpdatedMs = nowMs)

    // ─────────────────────────────────────────────────────────────────
    // QUERY HELPERS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Returns true if the long-press has fired (haptic should have
     * already been emitted).
     */
    fun hasFired(state: LongPressState): Boolean =
        state.lifecycle == LongPressLifecycle.FIRED ||
                state.lifecycle == LongPressLifecycle.COMPLETED

    /**
     * Returns true if the long-press can transition to a drag (i.e.
     * it has fired and the user starts moving).
     */
    fun canTransitionToDrag(state: LongPressState): Boolean =
        state.lifecycle == LongPressLifecycle.FIRED

    /**
     * Compute progress toward the long-press threshold [0..1].
     *
     * Used by the UI to render a progress indicator (e.g. a ripple
     * growing from the touch point).
     */
    fun progressTowardThreshold(state: LongPressState, nowMs: Long): Float {
        if (state.lifecycle != LongPressLifecycle.PENDING) {
            return if (state.lifecycle == LongPressLifecycle.FIRED ||
                state.lifecycle == LongPressLifecycle.COMPLETED
            ) 1f else 0f
        }

        val elapsed = nowMs - state.startedAtMs
        val ratio = elapsed.toFloat() / state.spec.minDurationMs.value.toFloat()
        return ratio.coerceIn(0f, 1f)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 5 — LONG-PRESS EVENT
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Events emitted by the long-press runtime.
 *
 * The Reducer consumes these and may emit Actions (haptic, context
 * menu request, etc.).
 */
sealed class LongPressEvent {
    /** No state change. */
    data object NoOp : LongPressEvent()

    /** Long-press threshold reached (no haptic). */
    data object Fired : LongPressEvent()

    /** Long-press threshold reached + haptic should fire. */
    data object FiredWithHaptic : LongPressEvent()

    /** Long-press completed (finger lifted after firing). */
    data object Completed : LongPressEvent()

    /** Finger lifted before threshold (was a tap). */
    data object ReleasedBeforeThreshold : LongPressEvent()

    /** Long-press cancelled. */
    data class Cancelled(
        val reason: LongPressCancelReason,
    ) : LongPressEvent()
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 6 — LONG-PRESS INTENT (Rule #78)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Intents emitted by long-press operations. Consumed by a Reducer
 * that updates [LongPressState] via [LongPressRuntime].
 */
sealed class LongPressIntent {
    data class Started(
        val position: GesturePoint,
        val spec: LongPressSpec,
        val targetId: String?,
        val sourceGestureId: String,
    ) : LongPressIntent()

    data class Moved(
        val currentPosition: GesturePoint,
    ) : LongPressIntent()

    data object Released : LongPressIntent()

    data class Cancelled(
        val reason: LongPressCancelReason,
    ) : LongPressIntent()

    data object Reset : LongPressIntent()
}

/**
 * Actions emitted by the long-press Reducer.
 */
sealed class LongPressAction {
    /** Fire long-press haptic. */
    data object FireHaptic : LongPressAction()

    /** Long-press fired — open context menu for the target. */
    data class OpenContextMenu(
        val targetId: String,
        val targetPosition: GesturePoint,
    ) : LongPressAction()

    /** Long-press cancelled — emit cancel haptic (soft). */
    data object CancelHaptic : LongPressAction()

    /** Long-press completed — transition to drag mode (if applicable). */
    data class TransitionToDrag(
        val targetId: String,
        val startPosition: GesturePoint,
    ) : LongPressAction()
}
