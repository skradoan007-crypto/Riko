package com.rikodev.music.android.core.designsystem.riko.gesture

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.semantics.Role
import com.rikodev.music.android.core.designsystem.riko.components.RikoHapticEvent
import com.rikodev.music.android.core.designsystem.riko.components.rememberRikoHaptic
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMotion
import kotlin.math.abs
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Canonical gesture primitives (plan 2.3) — one interaction vocabulary:
 *
 *  * press response  = [RikoTouch.PressScale] spring scale, always
 *  * capture rules   = consume ONLY after the gesture has captured
 *                      (slop arbitration keeps parent scroll/sheet owners)
 *  * fresh state     = every callback is `rememberUpdatedState`-wrapped, so a
 *                      running gesture never drives a stale closure (plan R1)
 *  * axis discipline = drags are direction-locked (plan R3)
 *  * haptic grammar  = [RikoHapticEvent] only (plan R5)
 *
 * Slop / long-press timing always come from `viewConfiguration` — never
 * hardcoded (plan R7).
 */

/**
 * Press-scale + click. The single press response for ordinary controls.
 */
@Composable
fun Modifier.rikoPress(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    scale: Float = RikoTouch.PressScale,
    onClick: () -> Unit
): Modifier {
    val currentOnClick by rememberUpdatedState(onClick)
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) scale else 1f,
        animationSpec = RikoMotion.snappy(),
        label = "rikoPressScale"
    )
    return this
        .graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = Role.Button,
            onClick = { currentOnClick() }
        )
}

/**
 * Tap with the canonical press response + CONFIRMATION haptic. Optional
 * [onLongClick] keeps one primitive for tap-or-hold controls (the hold wins
 * past the timer; until then the click path keeps ownership).
 */
@Composable
fun Modifier.rikoTap(
    enabled: Boolean = true,
    withHaptic: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier {
    val haptic = rememberRikoHaptic()
    val currentOnClick by rememberUpdatedState(onClick)
    val base = rikoPress(enabled = enabled, scale = RikoTouch.PressScale) {
        if (withHaptic) haptic.fire(RikoHapticEvent.CONFIRMATION)
        currentOnClick()
    }
    return if (onLongClick == null) base else base.rikoLongPress(onLongClick)
}

/**
 * Direction-locked drag. Only [orientation] deltas reach [onDelta]; the
 * change is consumed after the drag detector captures (plan R2/R3).
 */
@Composable
fun Modifier.rikoAxisDrag(
    orientation: Orientation,
    enabled: Boolean = true,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDelta: (Float) -> Unit
): Modifier {
    val currentOnDelta by rememberUpdatedState(onDelta)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    return this.pointerInput(orientation, enabled) {
        if (!enabled) return@pointerInput
        // True axis-lock (plan R3): the axis-specific detector refuses the
        // other axis's gestures entirely — a vertical flick on a horizontal
        // drag surface falls through to the parent scroll/sheet owner.
        if (orientation == Orientation.Vertical) {
            detectVerticalDragGestures(
                onDragStart = { currentOnDragStart() },
                onDragEnd = { currentOnDragEnd() },
                onDragCancel = { currentOnDragEnd() },
                onVerticalDrag = { change, dragAmount ->
                    change.consume()
                    currentOnDelta(dragAmount)
                }
            )
        } else {
            detectHorizontalDragGestures(
                onDragStart = { currentOnDragStart() },
                onDragEnd = { currentOnDragEnd() },
                onDragCancel = { currentOnDragEnd() },
                onHorizontalDrag = { change, dragAmount ->
                    change.consume()
                    currentOnDelta(dragAmount)
                }
            )
        }
    }
}

/**
 * Long-press grab drag — the intended UX for value controls (EQ bands, custom
 * thumbs): hold to grab, then the value tracks the finger. Nothing is
 * consumed until the hold is recognized, so parent scrolling keeps ordinary
 * flicks (plan R1/R2/R5).
 */
@Composable
fun Modifier.rikoGrabDrag(
    orientation: Orientation,
    enabled: Boolean = true,
    onGrab: () -> Unit = {},
    onRelease: () -> Unit = {},
    onDelta: (Float) -> Unit
): Modifier {
    val currentOnDelta by rememberUpdatedState(onDelta)
    val currentOnGrab by rememberUpdatedState(onGrab)
    val currentOnRelease by rememberUpdatedState(onRelease)
    return this.pointerInput(orientation, enabled) {
        if (!enabled) return@pointerInput
        val touchSlop = viewConfiguration.touchSlop
        val longPressMs = viewConfiguration.longPressTimeoutMillis
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val cancelled = withTimeoutOrNull(longPressMs) {
                while (true) {
                    val ev = awaitPointerEvent()
                    val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                    if (!c.pressed || c.isConsumed) break
                    val d = c.positionChange()
                    if (abs(d.x) > touchSlop || abs(d.y) > touchSlop) break
                }
                false
            }
            if (cancelled != null) return@awaitEachGesture
            down.consume()
            currentOnGrab()
            while (true) {
                val ev = awaitPointerEvent()
                val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                if (!c.pressed || c.isConsumed) break
                val d = c.positionChange()
                if (d != Offset.Zero) {
                    c.consume()
                    currentOnDelta(
                        if (orientation == Orientation.Vertical) d.y else d.x
                    )
                }
            }
            currentOnRelease()
        }
    }
}

/**
 * Seek on a single node: tap anywhere to jump, drag to scrub — one gesture
 * surface (the old NpProgressBar needed two nodes). [onSeek] receives the
 * finger fraction 0..1 along [orientation] (left→right / top→bottom).
 */
@Composable
fun Modifier.rikoSeek(
    orientation: Orientation = Orientation.Horizontal,
    enabled: Boolean = true,
    onSeekStart: () -> Unit = {},
    onSeekEnd: () -> Unit = {},
    onSeek: (Float) -> Unit
): Modifier {
    val currentOnSeek by rememberUpdatedState(onSeek)
    val currentOnSeekStart by rememberUpdatedState(onSeekStart)
    val currentOnSeekEnd by rememberUpdatedState(onSeekEnd)
    return this.pointerInput(orientation, enabled) {
        if (!enabled) return@pointerInput
        val touchSlop = viewConfiguration.touchSlop
        fun fraction(pos: Offset): Float {
            val raw = if (orientation == Orientation.Vertical) {
                if (size.height == 0) 0f else pos.y / size.height
            } else {
                if (size.width == 0) 0f else pos.x / size.width
            }
            return raw.coerceIn(0f, 1f)
        }
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var captured = false
            var abandoned = false
            while (!abandoned) {
                val ev = awaitPointerEvent()
                val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                if (!c.pressed) break
                if (!captured) {
                    val d = c.positionChange()
                    // Axis-lock (plan R3): capture ONLY when our axis crosses
                    // slop first; if the other axis wins, release the gesture
                    // to the parent (sheet collapse, list scroll).
                    val axisDelta =
                        if (orientation == Orientation.Vertical) abs(d.y) else abs(d.x)
                    val crossDelta =
                        if (orientation == Orientation.Vertical) abs(d.x) else abs(d.y)
                    if (axisDelta > touchSlop && axisDelta >= crossDelta) {
                        captured = true // plan R2: consume only after capture
                        currentOnSeekStart()
                        c.consume()
                        currentOnSeek(fraction(c.position))
                    } else if (crossDelta > touchSlop) {
                        abandoned = true // other axis won: not our gesture
                    }
                } else {
                    c.consume()
                    currentOnSeek(fraction(c.position))
                }
            }
            if (!captured && !abandoned) {
                // Tap-jump: quick press+release inside slop = seek to spot.
                captured = true
                currentOnSeekStart()
                currentOnSeek(fraction(down.position))
            }
            if (captured) currentOnSeekEnd()
        }
    }
}

/**
 * Long-press-only companion used by [rikoTap]. Same capture discipline: the
 * click path keeps ownership unless the hold wins past the timer.
 */
@Composable
private fun Modifier.rikoLongPress(onLongClick: () -> Unit): Modifier {
    val current by rememberUpdatedState(onLongClick)
    return this.pointerInput(Unit) {
        val longPressMs = viewConfiguration.longPressTimeoutMillis
        val touchSlop = viewConfiguration.touchSlop
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val cancelled = withTimeoutOrNull(longPressMs) {
                while (true) {
                    val ev = awaitPointerEvent()
                    val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                    if (!c.pressed || c.isConsumed) break
                    val d = c.positionChange()
                    if (abs(d.x) > touchSlop || abs(d.y) > touchSlop) break
                }
                false
            }
            if (cancelled == null) {
                down.consume()
                current()
            }
        }
    }
}
