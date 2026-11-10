package com.rikodev.music.android.presentation.nowplaying

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.gestures.Orientation
import com.rikodev.music.android.core.designsystem.riko.gesture.rikoSeek
import com.rikodev.music.android.core.experience.wave.WaveEnergy
import kotlin.math.exp
import kotlin.math.sin

/**
 * PlasmaTimeline (plan 4.4) — the plasma asset wired into the Now Playing
 * seekbar with the CANONICAL interaction:
 *
 *  * seek = [rikoSeek] (axis-locked, tap-jump + drag, consume after capture —
 *    the old renderer seeked on DOWN, causing accidental jumps)
 *  * API 33+ = AGSL [PlasmaWaveformCanvas] (visual-only, zero alloc/frame)
 *  * API < 33 = Canvas ridge fallback with the SAME geometry contract
 *    (PlasmaTokens bar inset / canvas height / progress window)
 */
@Composable
fun PlasmaTimeline(
    progress: () -> Float,
    motion: () -> WaveEnergy,
    isPlaying: Boolean,
    isBuffering: Boolean = false,
    colorLeft: Color,
    colorMid: Color,
    colorRight: Color,
    isTimelineVisible: () -> Boolean = { true },
    onSeekStart: () -> Unit = {},
    onSeek: (Float) -> Unit,
    onSeekEnd: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var scrubbing by remember { mutableStateOf(false) }
    val currentOnSeek by rememberUpdatedState(onSeek)
    val currentOnSeekStart by rememberUpdatedState(onSeekStart)
    val currentOnSeekEnd by rememberUpdatedState(onSeekEnd)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .requiredHeight(PlasmaTokens.CanvasHeight)
            .rikoSeek(
                orientation = Orientation.Horizontal,
                onSeekStart = {
                    scrubbing = true
                    currentOnSeekStart()
                },
                onSeek = { fraction -> currentOnSeek(fraction) },
                onSeekEnd = {
                    scrubbing = false
                    currentOnSeekEnd()
                }
            )
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PlasmaWaveformCanvas(
                progress = progress,
                motion = motion,
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                isScrubbing = scrubbing,
                isTimelineVisible = isTimelineVisible,
                colorLeft = colorLeft,
                colorMid = colorMid,
                colorRight = colorRight,
                modifier = Modifier.matchParentSize()
            )
        } else {
            PlasmaTimelineFallback(
                progress = progress,
                motion = motion,
                colorLeft = colorLeft,
                colorMid = colorMid,
                colorRight = colorRight,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

/**
 * Canvas ridge fallback (API < 33) — same geometry contract as the shader
 * path: bar inset window, baseline + 4 pressure ridges + glowing thumb.
 * Zero per-frame allocation (path reused via remember).
 */
@Composable
private fun PlasmaTimelineFallback(
    progress: () -> Float,
    motion: () -> WaveEnergy,
    colorLeft: Color,
    colorMid: Color,
    colorRight: Color,
    modifier: Modifier = Modifier
) {
    val ridgePath = remember { Path() }
    Canvas(modifier = modifier) {
        val e = motion()
        val prog = progress().coerceIn(0f, 1f)
        val barX = PlasmaTokens.BarInset.toPx()
        val barLen = size.width - 2f * barX
        val midY = size.height / 2f

        // Baseline (ink).
        drawLine(
            color = Color.White.copy(alpha = 0.55f),
            start = Offset(barX, midY),
            end = Offset(barX + barLen, midY),
            strokeWidth = 3f
        )

        // Four pressure ridges — bass widest, momentum tallest. The hump
        // envelope trails the thumb exactly like the shader's progress window.
        for (k in 0..3) {
            val amp = when (k) {
                0 -> e.bassPressure
                1 -> e.midTexture
                2 -> e.trebleSpark
                else -> e.momentum
            }
            val ridgeColor = when (k) {
                0 -> colorLeft
                1 -> colorMid
                2 -> colorRight
                else -> colorRight
            }
            ridgePath.reset()
            val steps = 32
            for (i in 0..steps) {
                val u = i / steps.toFloat()
                val x = barX + barLen * u
                val d = (u - prog) / 0.32f
                val env = exp(-d * d).toFloat()
                val ridge = sin(u * Math.PI).toFloat()
                val y = midY - (0.18f + 0.30f * amp) * env * size.height * (0.55f + 0.45f * ridge)
                if (i == 0) ridgePath.moveTo(x, midY) else ridgePath.lineTo(x, y)
            }
            ridgePath.lineTo(barX + barLen, midY)
            drawPath(
                path = ridgePath,
                color = ridgeColor.copy(alpha = 0.28f + 0.34f * amp)
            )
        }

        // Glowing thumb at progress.
        val thumbX = barX + barLen * prog
        drawCircle(Color.White.copy(alpha = 0.35f), 14f, Offset(thumbX, midY))
        drawCircle(Color.White, 8f, Offset(thumbX, midY))
        drawCircle(
            color = colorRight.copy(alpha = 0.55f),
            radius = 8f,
            center = Offset(thumbX, midY),
            style = Stroke(width = 3f)
        )
    }
}
