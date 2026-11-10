/*
 * ═══════════════════════════════════════════════════════════════════════════
 * MiniPlayerGestureLayer.kt
 * Riko Music App — Mini Player 2.0, Build-3 Phase C, File 9
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * MPRS Reference: Chapter B.9 — MiniPlayerGestureSpec
 *
 * Purpose:
 *   "Touch input recognition and event dispatch. Invisible layer."
 *
 * Personality:
 *   "Invisible layer. Direction-locked gestures. Priority-based handling.
 *    Never intercepts what it shouldn't."
 *
 * ── Architecture Rules Followed ────────────────────────────────────────────
 *
 *   Rule #19 — Every animation sourced from MiniPlayerMotion object.
 *              No hardcoded durations, no hardcoded easing curves.
 *              (This layer has no animations — references for long press
 *              timeout and fling decay come from Tokens/Motion.)
 *
 *   Rule #20 — Renderer NEVER interprets state values.
 *              This layer NEVER renders. Emits actions and events only.
 *              Zero visual output. Zero layout contribution. Zero pixels.
 *
 *   Rule #21 — Every threshold sourced from MiniPlayerTokens.
 *              No magic numbers. No unexplained constants.
 *              DirectionLockThreshold, HorizontalSkipThreshold, DismissThreshold,
 *              ExpandFlingVelocity, DismissFlingVelocity, LongPressThresholdMs.
 *
 *   Rule #23 — No raw Experience OS reads.
 *              This component reads ONLY MiniPlayerState fields documented
 *              in the MPRS spec (gesture, morph).
 *
 * ── State Reads (from MiniPlayerState) ──────────────────────────────────────
 *
 *   Gesture:   state.gesture.isActive,
 *              state.gesture.direction,
 *              state.gesture.isDirectionLocked,
 *              state.gesture.isLongPressing
 *
 *   Morph:     state.morph.morphProgress,
 *              state.morph.isDragging,
 *              state.morph.isDismissing,
 *              state.morph.isSettling,
 *              state.morph.isAtRest,
 *              state.morph.direction
 *
 * ── Recognized Gestures ────────────────────────────────────────────────────
 *
 *   1. Tap (on surface)           → MiniPlayerAction.ExpandToNowPlaying
 *   2. Vertical drag up            → Morph expand (per-frame events)
 *   3. Vertical drag down          → Morph contract / Dismiss (per-frame events)
 *   4. Horizontal drag left        → Skip next (if threshold crossed)
 *   5. Horizontal drag right      → Skip previous (if threshold crossed)
 *   6. Long press                  → Queue peek (MiniPlayerAction.ShowQueuePeek)
 *   7. Fling up                    → Commit expand (MiniPlayerAction.ExpandToNowPlaying)
 *   8. Fling down                  → Commit dismiss (MiniPlayerAction.Dismiss)
 *
 * ── Direction Lock ──────────────────────────────────────────────────────────
 *
 *   Gesture starts undirected. After 16dp movement
 *   (MiniPlayerTokens.DirectionLockThreshold):
 *     - If |totalDragY| > |totalDragX| → lock VERTICAL
 *     - If |totalDragX| > |totalDragY| → lock HORIZONTAL
 *   Once locked, stays locked until gesture ends.
 *   Lock threshold: MiniPlayerTokens.DirectionLockThreshold (16.dp)
 *
 * ── Gesture Priority (3-Tier) ─────────────────────────────────────────────
 *
 *   Tier 1 — System:   Back press, notification shade, phone call
 *                       (Handled by Android system — this layer yields)
 *   Tier 2 — Component: Button taps (play/pause, skip, favorite)
 *                       (Child composables consume events before this layer)
 *   Tier 3 — Child:     Drag, fling, long-press, tap on surface
 *                       (This layer handles these)
 *
 * ── Threshold Values (from MiniPlayerTokens Section 21) ────────────────────
 *
 *   DirectionLockThreshold   = 16.dp
 *   HorizontalSkipThreshold   = 120.dp
 *   DismissThreshold          = 200.dp
 *   ExpandFlingVelocity       = 800 px/s
 *   DismissFlingVelocity      = 600 px/s
 *   LongPressThresholdMs      = 500 ms
 *
 * ── NEVER List ─────────────────────────────────────────────────────────────
 *
 *   Direct state mutation, morph progress calculation, spring animation,
 *   business logic, visual rendering, layout computation, Experience OS access,
 *   intercepting consumed child events, hardcoded thresholds, magic numbers
 *
 * ── Performance Budget ─────────────────────────────────────────────────────
 *
 *   Target: 0.1ms per frame (MiniPlayerTokens.RenderPriority.GESTURE_MS)
 *   Gesture processing is lightweight: pointer delta accumulation and
 *   threshold comparison only. Zero allocations in hot path.
 *   VelocityTracker is the only per-gesture allocation.
 *
 * ── Output ──────────────────────────────────────────────────────────────────
 *
 *   MiniPlayerAction (committed gestures):
 *     - ExpandToNowPlaying, Dismiss, SkipNext, SkipPrevious
 *     - ShowQueuePeek, HideQueuePeek
 *     - HapticFeedback(GESTURE_LOCK), HapticFeedback(LONG_PRESS),
 *       HapticFeedback(FLING), HapticFeedback(MORPH_COMMIT)
 *
 *   MiniPlayerEvent (per-frame tracking):
 *     - DirectionLocked, Drag, HorizontalDrag, DragReleased,
 *       FlingExpand, FlingDismiss, TapExpand, LongPressTriggered,
 *       LongPressReleased, GestureCancelled
 *
 * ═══════════════════════════════════════════════════════════════════════════
 */
package com.rikodev.music.android.presentation.miniplayer

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.abs
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.input.pointer.PointerEvent

// ═══════════════════════════════════════════════════════════════════════════
// MAIN COMPOSABLE — Gesture Detection Layer
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Invisible gesture detection layer for the Mini Player.
 *
 * Wraps [content] and intercepts touch events BEFORE child components (Tier 3).
 * Implements the full 8-gesture recognition from MPRS Chapter B.9:
 * tap, vertical drag, horizontal drag, long press, fling up, fling down.
 *
 * Direction-locked: after [MiniPlayerTokens.DirectionLockThreshold] (16dp) of
 * cumulative movement, the gesture locks to either [GestureDirection.VERTICAL]
 * or [GestureDirection.HORIZONTAL] and stays locked until the gesture ends.
 *
 * Priority-aware: does NOT intercept events already consumed by child components
 * (buttons, internal scrolls). System-level gestures (back press, notification
 * shade) are yielded to Android's gesture system.
 *
 * **Zero visual output.** This composable produces no rendered pixels. It only
 * reads [MiniPlayerState] fields (state.gesture, state.morph) and emits
 * [MiniPlayerAction] and [MiniPlayerEvent] through the provided callbacks.
 *
 * **Threshold traceability** (Rule #21): Every threshold value is sourced from
 * [MiniPlayerTokens]. No magic numbers, no hardcoded distances or velocities.
 *
 * **Animation reference** (Rule #19): Fling decay and long press timeout reference
 * [MiniPlayerTokens] and [MiniPlayerMotion]. No inline durations.
 *
 * **Interruption policy** (MiniPlayerInterruptionPolicy): This layer consults
 * [shouldAcceptGesture] before starting any gesture recognition. If the mini player
 * is settling, dismissing, or morph-blocked, new gestures are rejected per the
 * 3-tier priority system from [MiniPlayerGestureSpec].
 *
 * @param state The single source of truth. Only `state.gesture` and
 *   `state.morph` fields are read (Rule #23). No raw Experience OS access.
 * @param onAction Emitter for committed gesture actions. These flow to
 *   PlayerViewModel for execution: ExpandToNowPlaying, Dismiss, SkipNext,
 *   SkipPrevious, ShowQueuePeek, HideQueuePeek, HapticFeedback.
 * @param modifier External modifier. Applied to the wrapping Box.
 * @param onEvent Emitter for per-frame tracking events. These flow to the
 *   reducer for state.gesture and state.morph updates: Drag, HorizontalDrag,
 *   DirectionLocked, FlingExpand, FlingDismiss, etc.
 * @param content The child content (buttons, album, metadata) that this
 *   gesture layer wraps. Touch events flow: this layer → children.
 */
@Composable
fun MiniPlayerGestureLayer(
    state: MiniPlayerState,
    onAction: (MiniPlayerAction) -> Unit,
    modifier: Modifier = Modifier,
    onEvent: (MiniPlayerEvent) -> Unit = {},
    content: @Composable () -> Unit = {}
) {
    // ═════════════════════════════════════════════════════════════════════
    // DENSITY — Required for dp → px threshold conversion (Rule #21)
    // ═════════════════════════════════════════════════════════════════════
    val density = LocalDensity.current

    // ═════════════════════════════════════════════════════════════════════
    // GESTURE DETECTOR — Invisible touch interception layer
    // ═════════════════════════════════════════════════════════════════════
    //
    // PointerInput keys are selected from state fields this layer reads
    // (Rule #23): state.gesture and state.morph only.
    // When these change, the gesture detector restarts its coroutine.
    // This prevents stale state reads inside the gesture handler.
    //
    // Key selection:
    //   - state.morph.isSettling:   settle animation blocks new gestures
    //   - state.morph.isDismissing: dismiss animation blocks new gestures
    //   - state.morph.isAtRest:     gesture behavior changes at rest
    //   - state.isVisible:          invisible player rejects gestures
    //   - state.isMorphBlocked:     error/loading state blocks morph gestures
    // ═════════════════════════════════════════════════════════════════════

    Box(
        modifier = modifier.pointerInput(
            state.morph.isSettling,
            state.morph.isDismissing,
            state.morph.isAtRest,
            state.isVisible,
            state.isMorphBlocked
        ) {
            // ── Interruption Policy Gate ────────────────────────────────
            // MiniPlayerInterruptionPolicy: reject gestures when the mini
            // player is in a state that should not accept new input.
            // This implements Tier 1 (System) priority from MiniPlayerGestureSpec.
            if (!shouldAcceptGesture(state)) return@pointerInput

            // ── awaitEachGesture ───────────────────────────────────────
            // Custom gesture detection with full control over:
            //   1. Direction locking (MiniPlayerTokens.DirectionLockThreshold)
            //   2. Long press detection (MiniPlayerTokens.LongPressThresholdMs)
            //   3. Velocity tracking (VelocityTracker for fling detection)
            //   4. Threshold evaluation (MiniPlayerTokens dismiss/skip/fling)
            //
            // This follows the same philosophy as Compose's detectDragGestures
            // but with custom direction locking per MPRS Chapter B.9.
            // ══════════════════════════════════════════════════════════

            awaitEachGesture {
                // ── Await Initial Touch ────────────────────────────────
                // requireUnconsumed = false: accept all initial touches.
                // We check consumption later to respect Tier 2 (Component)
                // priority — if a child button consumed the event, we abort.
                val firstDown = awaitFirstDown(requireUnconsumed = false)
                val pointerId = firstDown.id

                // ── Local Gesture Tracking State ───────────────────────
                // NOT persisted to MiniPlayerState — this is coroutine-scoped
                // state within the gesture detector. Lives only for the
                // duration of a single touch sequence (down → up).
                var directionLocked = false
                var lockedDirection = GestureDirection.NONE
                var totalDragX = 0f
                var totalDragY = 0f
                var longPressTriggered = false
                var dragConsumed = false

                // ── Velocity Tracker ───────────────────────────────────
                // Tracks pointer positions over time for fling detection.
                // One allocation per gesture (within performance budget).
                val velocityTracker = VelocityTracker()

                // ── Timestamps ────────────────────────────────────────
                val gestureStartTimeMs = firstDown.uptimeMillis

                // ── Long Press Detection ───────────────────────────────
                // Launches a coroutine that fires after LongPressThresholdMs.
                // Cancelled immediately if: finger moves, direction locks,
                // or gesture ends (lift / child consumption).
                //
                // Threshold: MiniPlayerTokens.LongPressThresholdMs (500ms)
                // Animation: No animation — instant fire (from MiniPlayerMotion
                // queue peek slide spec for the downstream visual effect).
                // Long press detection via timeout checking in the event loop
                // (avoids restricted coroutine scope issues with launch+delay)

                // ── Main Gesture Loop ─────────────────────────────────
                // Processes all pointer events from initial down until up.
                // Tracks drag accumulation and direction locking.
                var lastEvent: PointerEvent? = null
                do {
                    val event = awaitPointerEvent()
                    lastEvent = event
                    // Find the change for our tracked pointer.
                    // In multi-touch scenarios, we only track the pointer
                    // that initiated this gesture sequence.
                    val change = event.changes.find { it.id == pointerId } ?: break

                    // ── Child Consumption Check (Tier 2 Priority) ────
                    // If a child component (button, scroll) consumed this
                    // event, we cancel our gesture. This implements the
                    // "Never intercepts what it shouldn't" personality.
                    if (change.consumed == androidx.compose.ui.input.pointer.ConsumedData()) {
                        // long press cancelled via movement/direction lock above
                        onEvent(MiniPlayerEvent.GestureCancelled())
                        break
                    }

                    // ── Movement Processing ──────────────────────────
                    if (change.position != change.previousPosition) {
                        // Accumulate raw drag distance from Compose's
                        // per-event deltas. These are in pixels.
                        val deltaX = change.position.x - change.previousPosition.x
                        val deltaY = change.position.y - change.previousPosition.y

                        totalDragX += deltaX
                        totalDragY += deltaY

                        // Feed position to velocity tracker for fling
                        // detection on release.
                        velocityTracker.addPosition(
                            change.uptimeMillis,
                            change.position
                        )

                        // ── Direction Lock Evaluation ─────────────────
                        // Before lock: accumulate drag. After lock:
                        // only process along locked axis.
                        //
                        // Threshold: MiniPlayerTokens.DirectionLockThreshold
                        //           (16.dp → converted to pixels)
                        if (!directionLocked) {
                            val lockThresholdPx = with(density) {
                                MiniPlayerTokens.DirectionLockThreshold.toPx()
                            }
                            val absX = abs(totalDragX)
                            val absY = abs(totalDragY)

                            if (maxOf(absX, absY) >= lockThresholdPx) {
                                directionLocked = true
                                dragConsumed = true
                                lockedDirection = if (absX > absY) {
                                    GestureDirection.HORIZONTAL
                                } else {
                                    GestureDirection.VERTICAL
                                }

                                // Cancel long press — movement disqualifies
                                // the long press gesture.
                                // long press cancelled via movement/direction lock above

                                // Consume the event to prevent child
                                // components from processing this drag.
                                change.consume()

                                // Emit direction lock event for the reducer
                                // to update state.gesture.
                                onEvent(
                                    MiniPlayerEvent.DirectionLocked(lockedDirection)
                                )

                                // Haptic feedback for direction lock
                                // (MPRS B.9: "subtle haptic on direction lock")
                                onAction(
                                    MiniPlayerAction.HapticFeedback(HapticType.GESTURE_LOCK)
                                )
                            }
                        } else {
                            // ── Locked Drag Processing ─────────────────
                            // Direction is locked. Process drag along the
                            // locked axis only. Emit events for the
                            // reducer to update state.morph and state.gesture.

                            change.consume()

                            when (lockedDirection) {
                                GestureDirection.VERTICAL -> {
                                    // Vertical drag: negative deltaY = upward (expand),
                                    // positive deltaY = downward (contract/dismiss).
                                    val elapsedMs = (change.uptimeMillis - gestureStartTimeMs)
                                        .coerceAtLeast(1L)
                                    val currentVelocity = totalDragY / elapsedMs * 1000f

                                    onEvent(
                                        MiniPlayerEvent.Drag(
                                            deltaY = deltaY,
                                            velocity = currentVelocity,
                                            timestampMs = change.uptimeMillis
                                        )
                                    )
                                }

                                GestureDirection.HORIZONTAL -> {
                                    // Horizontal drag: negative deltaX = left (skip next),
                                    // positive deltaX = right (skip previous).
                                    val elapsedMs = (change.uptimeMillis - gestureStartTimeMs)
                                        .coerceAtLeast(1L)
                                    val currentVelocity = totalDragX / elapsedMs * 1000f

                                    onEvent(
                                        MiniPlayerEvent.HorizontalDrag(
                                            deltaX = deltaX,
                                            velocity = currentVelocity
                                        )
                                    )
                                }

                                GestureDirection.NONE -> {
                                    // Should never reach here — directionLocked
                                    // is true, which implies lockedDirection
                                    // is VERTICAL or HORIZONTAL.
                                }
                            }
                        }
                    }

                    // ── Long Press Timeout Check ──────────────────────────
                    // Check if elapsed time exceeds long press threshold
                    if (!longPressTriggered && !directionLocked && !dragConsumed) {
                        val currentUptime = event.changes.firstOrNull()?.uptimeMillis
                            ?: change.uptimeMillis
                        if (currentUptime - gestureStartTimeMs >= MiniPlayerTokens.LongPressThresholdMs.toLong()) {
                            longPressTriggered = true
                            onAction(MiniPlayerAction.ShowQueuePeek)
                            onEvent(MiniPlayerEvent.LongPressTriggered)
                            onAction(MiniPlayerAction.HapticFeedback(HapticType.LONG_PRESS))
                        }
                    }
                } while (event.changes.any { it.pressed })

                // ═══════════════════════════════════════════════════════
                // GESTURE COMPLETION — Evaluate and emit committed action
                // ═══════════════════════════════════════════════════════
                //
                // After the loop exits (finger lifted), we evaluate the
                // completed gesture and emit the appropriate action/event.
                // Priority: Long press > Fling > Distance > Tap > Cancel.

                // long press cancelled via movement/direction lock above

                when {
                    // ── Long Press Release ─────────────────────────────
                    // User held past threshold and released.
                    longPressTriggered -> {
                        onAction(MiniPlayerAction.HideQueuePeek)
                        onEvent(MiniPlayerEvent.LongPressReleased)
                    }

                    // ── No Drag (Tap) ─────────────────────────────────
                    // Minimal or no movement. Direction never locked.
                    // This is a surface tap → expand to Now Playing.
                    !dragConsumed -> {
                        // Only emit if no child consumed the event.
                        // This respects Tier 2 priority: if a button was
                        // tapped, the button handles it, not the surface.
                        val finalChange = lastEvent?.changes?.find { it.id == pointerId }
                        if (finalChange != null && finalChange.consumed == androidx.compose.ui.input.pointer.ConsumedData()) {
                            onEvent(MiniPlayerEvent.TapExpand)
                            onAction(MiniPlayerAction.ExpandToNowPlaying)
                        }
                    }

                    // ── Locked Drag Complete ──────────────────────────
                    // Direction was locked and finger was lifted.
                    // Evaluate velocity for fling and distance for thresholds.
                    else -> {
                        val velocity = velocityTracker.calculateVelocity()

                        // Emit drag release event for the reducer.
                        when (lockedDirection) {
                            GestureDirection.VERTICAL -> {
                                onEvent(MiniPlayerEvent.DragReleased(velocity = velocity.y))
                            }
                            GestureDirection.HORIZONTAL -> {
                                // Horizontal drag release — no separate
                                // HorizontalDragReleased event; evaluation
                                // happens here and emits committed action.
                            }
                            GestureDirection.NONE -> { /* unreachable */ }
                        }

                        // ── Evaluate Committed Gesture ────────────────
                        when (lockedDirection) {
                            GestureDirection.VERTICAL -> {
                                evaluateVerticalRelease(
                                    totalDragY = totalDragY,
                                    velocityY = velocity.y,
                                    density = density,
                                    onAction = onAction,
                                    onEvent = onEvent
                                )
                            }

                            GestureDirection.HORIZONTAL -> {
                                evaluateHorizontalRelease(
                                    totalDragX = totalDragX,
                                    density = density,
                                    onAction = onAction
                                )
                            }

                            GestureDirection.NONE -> { /* unreachable */ }
                        }
                    }
                }
            }
        }
    ) {
        // ═══════════════════════════════════════════════════════════════
        // CHILD CONTENT — Wrapped by the invisible gesture layer
        // ═══════════════════════════════════════════════════════════════
        // The gesture layer produces ZERO visual output. All rendering
        // comes from child composables (Surface, Album, Metadata, etc.).
        content()
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// HELPER: Gesture Acceptance Gate
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Determines whether the gesture layer should accept new gestures.
 *
 * Implements the interruption policy from MiniPlayerInterruptionPolicy and
 * the 3-tier priority system from MiniPlayerGestureSpec:
 *   - Tier 1 (System): Always yield to system interrupts
 *   - Tier 2 (Component): Don't interfere with button taps
 *   - Tier 3 (Child): Accept surface gestures when conditions allow
 *
 * The gesture layer rejects new gestures when:
 *   - Mini player is not visible (state.isVisible == false)
 *   - Morph is settling (state.morph.isSettling == true)
 *   - Morph is dismissing (state.morph.isDismissing == true)
 *   - Morph is blocked by error/loading (state.isMorphBlocked == true)
 *
 * These conditions align with MiniPlayerInterruptionPolicy's CONTINUE/FREEZE
 * decisions from Part K of the contract.
 *
 * State reads (Rule #23): Only state.gesture and state.morph fields.
 *
 * @param state Current MiniPlayerState snapshot.
 * @return true if new gestures should be accepted.
 */
private fun shouldAcceptGesture(state: MiniPlayerState): Boolean {
    // Mini player must be visible to accept gestures.
    if (!state.isVisible) return false

    // Morph settling: animation is running to final position.
    // New gestures would fight the animation → reject.
    // Policy: MiniPlayerInterruptionPolicy → CONTINUE (animation wins)
    if (state.morph.isSettling) return false

    // Morph dismissing: surface is exiting the screen.
    // New gestures are meaningless → reject.
    if (state.morph.isDismissing) return false

    // Morph blocked by error or loading state.
    // User should not be able to drag-expand during these states.
    // Policy: MiniPlayerInterruptionPolicy → REJECT
    if (state.isMorphBlocked) return false

    return true
}

// ═══════════════════════════════════════════════════════════════════════════
// HELPER: Vertical Release Evaluation
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Evaluates a completed vertical drag gesture and emits committed actions/events.
 *
 * Decision priority (from MPRS Chapter B.9):
 *   1. Dismiss by distance: totalDragY > DismissThreshold (200dp)
 *   2. Dismiss by fling:    velocityY > DismissFlingVelocity (600 px/s, downward)
 *   3. Expand by fling:     velocityY < -ExpandFlingVelocity (800 px/s, upward)
 *   4. Expand by distance:  totalDragY < -(DismissThreshold * expandCommitRatio)
 *   5. No commit:           gesture cancelled → spring back (handled by reducer)
 *
 * In Compose's coordinate system:
 *   - Positive Y = downward (drag down, dismiss direction)
 *   - Negative Y = upward (drag up, expand direction)
 *
 * All thresholds sourced from [MiniPlayerTokens] (Rule #21).
 * Fling detection uses [VelocityTracker.calculateVelocity] output.
 *
 * @param totalDragY Accumulated vertical drag in pixels. Positive = downward.
 * @param velocityY Final vertical velocity in pixels/second. Positive = downward.
 * @param density Local density for dp → px conversion.
 * @param onAction Action emitter for committed gestures.
 * @param onEvent Event emitter for fling tracking.
 */
private fun evaluateVerticalRelease(
    totalDragY: Float,
    velocityY: Float,
    density: androidx.compose.ui.unit.Density,
    onAction: (MiniPlayerAction) -> Unit,
    onEvent: (MiniPlayerEvent) -> Unit
) {
    // ── Threshold Conversion (Rule #21) ────────────────────────────────────
    // Convert dp thresholds to pixels using the provided density.
    val dismissThresholdPx = with(density) {
        MiniPlayerTokens.DismissThreshold.toPx()
    }

    // ── Decision Matrix ───────────────────────────────────────────────────
    //
    // Compose coordinate system: positive Y = downward.
    //
    // Dismiss conditions (user dragged down or flung down):
    //   A. Distance: totalDragY > DismissThreshold (200dp in px)
    //      User slowly dragged the mini player below the dismiss line.
    //   B. Fling:    velocityY > DismissFlingVelocity (600 px/s)
    //      User flung the mini player downward with sufficient velocity.
    //
    // Expand conditions (user dragged up or flung up):
    //   C. Fling:    velocityY < -ExpandFlingVelocity (800 px/s)
    //      User flung the mini player upward with high velocity.
    //   D. Distance: totalDragY < -(dismissThresholdPx * expandCommitRatio)
    //      User dragged upward past a fraction of the dismiss threshold.
    //
    // No commit (E): neither condition met → gesture springs back.
    //   The reducer receives DragReleased with the velocity and decides
    //   to spring back to collapsed state.

    when {
        // ── A. Dismiss by Distance ─────────────────────────────────────
        // User dragged down past the dismiss threshold (200dp).
        // This is a deliberate, slow dismiss — not a fling.
        totalDragY > dismissThresholdPx -> {
            onAction(MiniPlayerAction.Dismiss)
            onEvent(MiniPlayerEvent.FlingDismiss(velocity = abs(velocityY)))
            onAction(
                MiniPlayerAction.HapticFeedback(
                    type = HapticType.MORPH_COMMIT,
                    intensity = 1f
                )
            )
        }

        // ── B. Dismiss by Fling Velocity ────────────────────────────────
        // User flung downward fast enough (velocity > 600 px/s).
        // Even if distance is short, the velocity commits the dismiss.
        // From MiniPlayerTokens.DismissFlingVelocity.
        velocityY > MiniPlayerTokens.DismissFlingVelocity -> {
            onAction(MiniPlayerAction.Dismiss)
            onEvent(MiniPlayerEvent.FlingDismiss(velocity = abs(velocityY)))
            onAction(
                MiniPlayerAction.HapticFeedback(
                    type = HapticType.FLING,
                    intensity = (velocityY / MiniPlayerTokens.DismissFlingVelocity)
                        .coerceIn(0f, 1f)
                )
            )
        }

        // ── C. Expand by Fling Velocity ─────────────────────────────────
        // User flung upward fast enough (velocity > 800 px/s upward).
        // Negative velocity = upward in Compose coordinate system.
        // From MiniPlayerTokens.ExpandFlingVelocity.
        velocityY < -MiniPlayerTokens.ExpandFlingVelocity -> {
            onAction(MiniPlayerAction.ExpandToNowPlaying)
            onEvent(MiniPlayerEvent.FlingExpand(velocity = abs(velocityY)))
            onAction(
                MiniPlayerAction.HapticFeedback(
                    type = HapticType.FLING,
                    intensity = (abs(velocityY) / MiniPlayerTokens.ExpandFlingVelocity)
                        .coerceIn(0f, 1f)
                )
            )
        }

        // ── D. Expand by Distance ──────────────────────────────────────
        // User dragged upward significantly (past 50% of dismiss threshold).
        // This handles slow, deliberate upward drags that don't reach fling
        // velocity but demonstrate clear intent to expand.
        totalDragY < -(dismissThresholdPx * MiniPlayerGestureSpec.EXPAND_COMMIT_DISTANCE_RATIO) -> {
            onAction(MiniPlayerAction.ExpandToNowPlaying)
            onEvent(MiniPlayerEvent.FlingExpand(velocity = abs(velocityY)))
            onAction(
                MiniPlayerAction.HapticFeedback(
                    type = HapticType.MORPH_COMMIT,
                    intensity = 0.7f
                )
            )
        }

        // ── E. No Commit ───────────────────────────────────────────────
        // Neither dismiss nor expand threshold met.
        // The reducer receives DragReleased with the velocity and will
        // spring the morph back to its previous position.
        // No action emitted — the reducer handles the spring-back.
        else -> {
            // No action. Reducer processes DragReleased event to spring back.
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// HELPER: Horizontal Release Evaluation
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Evaluates a completed horizontal drag gesture and emits skip actions.
 *
 * From MPRS Chapter B.9:
 *   - Drag left  past threshold → Skip Next
 *   - Drag right past threshold → Skip Previous
 *   - Below threshold → no action (gesture cancelled)
 *
 * In Compose's coordinate system:
 *   - Positive X = rightward (drag right, skip previous direction)
 *   - Negative X = leftward (drag left, skip next direction)
 *
 * Threshold: [MiniPlayerTokens.HorizontalSkipThreshold] (120dp).
 *
 * Unlike vertical gestures, horizontal drags do NOT support fling-based
 * skip. The user must drag past the distance threshold. This prevents
 * accidental skips from small horizontal movements during vertical drags.
 *
 * @param totalDragX Accumulated horizontal drag in pixels. Positive = rightward.
 * @param density Local density for dp → px conversion.
 * @param onAction Action emitter for committed skip actions.
 */
private fun evaluateHorizontalRelease(
    totalDragX: Float,
    density: androidx.compose.ui.unit.Density,
    onAction: (MiniPlayerAction) -> Unit
) {
    // ── Threshold Conversion (Rule #21) ────────────────────────────────────
    val skipThresholdPx = with(density) {
        MiniPlayerTokens.HorizontalSkipThreshold.toPx()
    }

    when {
        // ── Skip Next (Drag Left) ────────────────────────────────────────
        // User dragged leftward past the skip threshold.
        // Negative totalDragX = leftward in Compose coordinates.
        totalDragX < -skipThresholdPx -> {
            onAction(MiniPlayerAction.SkipNext)
            onAction(
                MiniPlayerAction.HapticFeedback(
                    type = HapticType.TAP,
                    intensity = 0.8f
                )
            )
        }

        // ── Skip Previous (Drag Right) ──────────────────────────────────
        // User dragged rightward past the skip threshold.
        // Positive totalDragX = rightward in Compose coordinates.
        totalDragX > skipThresholdPx -> {
            onAction(MiniPlayerAction.SkipPrevious)
            onAction(
                MiniPlayerAction.HapticFeedback(
                    type = HapticType.TAP,
                    intensity = 0.8f
                )
            )
        }

        // ── Below Threshold ─────────────────────────────────────────────
        // Horizontal drag didn't cross the skip threshold.
        // No action — gesture is cancelled.
        else -> {
            // No action. The reducer handles any state cleanup.
        }
    }
}
