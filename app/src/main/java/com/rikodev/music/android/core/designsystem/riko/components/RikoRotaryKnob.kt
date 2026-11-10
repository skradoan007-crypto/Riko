package com.rikodev.music.android.core.designsystem.riko.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.gesture.RikoTouch
import com.rikodev.music.android.core.designsystem.riko.gesture.rikoAxisDrag
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * RikoRotaryKnob (plan 5.4) — the studio's value control: vertical drag to
 * turn, detent haptic ticks, 48dp touch target, axis-locked so a horizontal
 * flick falls through to the sheet/scroll owner (no gesture conflict).
 *
 * Independent implementation — the rotary INTERACTION concept comes from the
 * RealTimeSpectrogramFFT reference (unverified license → concept only,
 * `docs/visualizer/VISUALIZER_SOURCE_INVENTORY.md` §6); zero external code.
 *
 * Recomposition discipline: the knob recomposes only while its [value]
 * changes (i.e. while the user is actively turning it) — all callback reads
 * are `rememberUpdatedState`-wrapped.
 */
@Composable
fun RikoRotaryKnob(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onCommit: () -> Unit = {}
) {
    val currentValue by rememberUpdatedState(onValueChange)
    val currentCommit by rememberUpdatedState(onCommit)
    // Sensitivity: a full knob turn covers the 0..1 range.
    val dragScale = 2.2f
    val knobSizePx = with(androidx.compose.ui.platform.LocalDensity.current) {
        RikoTouch.TouchTarget.toPx() * 1.25f
    }

    var dragAccum by remember { mutableFloatStateOf(0f) }
    val displayValue by animateFloatAsState(targetValue = value, label = "rotaryKnob")

    val haptic = rememberRikoHaptic()
    var lastTick by remember { mutableFloatStateOf(value) }

    Canvas(
        modifier = modifier
            .size(RikoTouch.TouchTarget * 1.25f)
            .semantics {
                contentDescription = label
                role = Role.Button
            }
            .rikoAxisDrag(
                orientation = Orientation.Vertical,
                enabled = enabled,
                onDragStart = { dragAccum = 0f },
                onDelta = { delta ->
                    dragAccum += delta
                    val normalizedDelta = -delta / (knobSizePx * dragScale)
                    val next = (value + normalizedDelta).coerceIn(0f, 1f)
                    if (next != value) {
                        currentValue(next)
                        // Detent tick every 5% — throttled, never noisy (R5).
                        if (abs(next - lastTick) >= 0.05f) {
                            haptic.fire(RikoHapticEvent.CONFIRMATION)
                            lastTick = next
                        }
                    }
                },
                onDragEnd = {
                    dragAccum = 0f
                    currentCommit()
                }
            )
    ) {
        val stroke = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
        val inset = 6.dp.toPx()
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        val topLeft = Offset(inset, inset)

        // Track arc — the full sweep.
        drawArc(
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.16f),
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke
        )
        // Value arc — the turned portion.
        drawArc(
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
            startAngle = 135f,
            sweepAngle = 270f * displayValue.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke
        )
        // Pointer notch at the value angle.
        val angleRad = Math.toRadians((135.0 + 270.0 * displayValue.coerceIn(0f, 1f)))
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = arcSize.width / 2f
        drawCircle(
            color = androidx.compose.ui.graphics.Color.White,
            radius = 4.dp.toPx(),
            center = Offset(
                cx + (r * kotlin.math.cos(angleRad)).toFloat(),
                cy + (r * kotlin.math.sin(angleRad)).toFloat()
            )
        )
    }
}

/** Convenience: map a 0..1 knob value into a Float range predictably. */
fun knobValueTo(range: ClosedFloatingPointRange<Float>, knob: Float): Float =
    range.start + (range.endInclusive - range.start) * knob.coerceIn(0f, 1f)

/** Convenience: inverse of [knobValueTo], rounded to milli precision. */
fun knobValueFrom(range: ClosedFloatingPointRange<Float>, value: Float): Float =
    (((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f) * 1000f).roundToInt() / 1000f
