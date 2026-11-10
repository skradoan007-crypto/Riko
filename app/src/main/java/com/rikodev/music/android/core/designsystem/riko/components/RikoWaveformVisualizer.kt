package com.rikodev.music.android.core.designsystem.riko.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextSecondary
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — WAVEFORM VISUALIZER (SM-UI-SPEC-001 v2.0, Section 2.20)
//
// Row of ~46 vertical bars, 2dp wide, 3dp gap, heights 6-40dp forming a
// mirrored/symmetrical amplitude pattern with 3-4 peak clusters (decorative
// generative — does not need real audio FFT).
// Played portion: text-primary (white) at 90% opacity.
// Unplayed: text-secondary (white 40-45%).
// Bar caps rounded (radius 1dp). Tap/drag anywhere seeks playback (the whole
// row acts as a custom Slider).
//
// Spec 5.8 optional live bounce: 2-3 bars near the playhead get a subtle
// 0.9-1.1 height oscillation while playing, paused on pause/seek.
// ─────────────────────────────────────────────────────────────────────────────

private val WaveformHeight = 48.dp
private val WaveformBarWidth = 2.dp
private val WaveformBarGap = 3.dp
private val WaveformBarMinHeight = 6.dp
private val WaveformBarMaxHeight = 40.dp
private const val WaveformBarCount = 46

/**
 * @param progress playback fraction 0..1 — direct 1:1 mapping, no easing.
 * @param onSeek called with the tapped/dragged fraction (0..1).
 * @param isPlaying enables the live playhead bounce (spec 5.8, optional).
 */
@Composable
fun RikoWaveformVisualizer(
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false
) {
    var widthPx by remember { mutableFloatStateOf(0f) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableFloatStateOf(0f) } // 0f not dragging, 1f dragging

    // Generative amplitude pattern — deterministic per bar index, 3-4 peak
    // clusters, mirrored feel (spec 2.20).
    val amplitudes = remember {
        FloatArray(WaveformBarCount) { i ->
            val t = i.toFloat() / (WaveformBarCount - 1)
            val cluster1 = 0.28f * abs(sin(t * 9.4f))
            val cluster2 = 0.22f * abs(sin(t * 4.1f + 1.2f))
            val cluster3 = 0.20f * abs(sin(t * 14.7f + 0.5f))
            val edgeFade = sin(t * Math.PI.toFloat()).coerceAtLeast(0.25f)
            min(1f, (0.30f + cluster1 + cluster2 + cluster3) * edgeFade)
        }
    }

    val playedColor = RikoTextPrimary.copy(alpha = 0.90f)
    val unplayedColor = RikoTextSecondary.copy(alpha = 0.75f)

    Canvas(
        modifier = modifier
            .height(WaveformHeight)
            .onSizeChanged { widthPx = it.width.toFloat() }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (widthPx > 0) onSeek((offset.x / widthPx).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, _ ->
                        if (widthPx > 0) {
                            dragFraction = (change.position.x / widthPx).coerceIn(0f, 1f)
                            dragging = 1f
                        }
                    },
                    onDragEnd = {
                        onSeek(dragFraction)
                        dragging = 0f
                    },
                    onDragCancel = { dragging = 0f }
                )
            }
    ) {
        val w = size.width
        val h = size.height
        val barW = WaveformBarWidth.toPx()
        val gap = WaveformBarGap.toPx()
        val totalW = WaveformBarCount * barW + (WaveformBarCount - 1) * gap
        val barSlot = w / WaveformBarCount
        val activeFraction = if (dragging > 0.5f) dragFraction else progress

        val maxH = WaveformBarMaxHeight.toPx()
        val minH = WaveformBarMinHeight.toPx()

        for (i in 0 until WaveformBarCount) {
            val center = i * barSlot + barSlot / 2f
            val played = (center / w) <= activeFraction

            val amp = amplitudes[i]
            val barH = minH + amp * (maxH - minH)
            val top = (h - barH) / 2f

            drawRoundRect(
                color = if (played) playedColor else unplayedColor,
                topLeft = Offset(center - barW / 2f, top),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(barW / 2f)
            )
        }
    }
}
