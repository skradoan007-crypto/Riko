package com.rikodev.music.android.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — WAVEFORM BAR TIMELINE (Full-Track Waveform)
//
// This component displays the FULL-TRACK waveform amplitude envelope — the
// actual "how loud this part of the song was" data decoded from the audio
// file (via AudioFileAnalyzer → WaveformRepository → WaveformData).
//
// This is DISTINCT from WaveformTimeline (fluid wave) which uses LIVE energy
// for decorative motion. This component shows STRUCTURE and enables SEEKING.
//
// Visual layout:
//   - Vertical bars from center baseline (symmetric)
//   - Peak bars (taller, brighter) + RMS bars (shorter, dimmer)
//   - Played region: accent color
//   - Unplayed region: dimmed color
//   - Playhead: vertical line + optional thumb
//
// Performance:
//   - Draw-phase only (no recomposition on progress update)
//   - Pre-allocated Path/rect reuse
//   - No allocation in draw lambda
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Full-track waveform timeline with seek support.
 *
 * @param peaks Normalized peak amplitude per bucket (0f..1f), from WaveformData.peaks
 * @param rms Normalized RMS amplitude per bucket (0f..1f), from WaveformData.rms
 * @param progress Current playback progress (0f..1f)
 * @param isPlaying Whether playback is active
 * @param playedColor Color for the played region
 * @param unplayedColor Color for the unplayed region
 * @param playheadColor Color for the playhead line
 * @param onSeekStart Called when seek gesture starts
 * @param onSeekChange Called during seek drag with new progress (0f..1f)
 * @param onSeekEnd Called when seek gesture ends with final progress
 * @param height Height of the waveform area
 * @param barCount Number of visible bars (will downsample if peaks.size > barCount)
 * @param showThumb Whether to show a thumb circle at the playhead
 */
@Composable
fun WaveformBarTimeline(
    peaks: FloatArray,
    rms: FloatArray,
    progress: Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    playedColor: Color = com.rikodev.music.android.core.designsystem.theme.AccentCyan,
    unplayedColor: Color = Color.White.copy(alpha = 0.18f),
    playheadColor: Color = Color.White,
    onSeekStart: () -> Unit = {},
    onSeekChange: (Float) -> Unit = {},
    onSeekEnd: (Float) -> Unit = {},
    height: Dp = 56.dp,
    barCount: Int = 120,
    showThumb: Boolean = true
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(progress) }

    val displayProgress = if (isDragging) dragProgress else progress

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    dragProgress = newProgress
                    onSeekChange(newProgress)
                    onSeekEnd(newProgress)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        dragProgress = progress
                        onSeekStart()
                    },
                    onDragEnd = {
                        isDragging = false
                        onSeekEnd(dragProgress)
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newProgress = (dragProgress + dragAmount.x / size.width).coerceIn(0f, 1f)
                        dragProgress = newProgress
                        onSeekChange(newProgress)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
            drawWaveformBars(
                peaks = peaks,
                rms = rms,
                progress = displayProgress,
                playedColor = playedColor,
                unplayedColor = unplayedColor,
                playheadColor = playheadColor,
                barCount = barCount,
                showThumb = showThumb
            )
        }
    }
}

private fun DrawScope.drawWaveformBars(
    peaks: FloatArray,
    rms: FloatArray,
    progress: Float,
    playedColor: Color,
    unplayedColor: Color,
    playheadColor: Color,
    barCount: Int,
    showThumb: Boolean
) {
    if (peaks.isEmpty() || rms.isEmpty()) return

    val w = size.width
    val h = size.height
    val baselineY = h * 0.5f
    val maxBarHeight = h * 0.42f // leave room for playhead thumb

    // Downsample to barCount if needed
    val effectiveBarCount = minOf(barCount, peaks.size)
    val step = peaks.size.toFloat() / effectiveBarCount

    val barWidth = (w / effectiveBarCount) * 0.7f
    val barGap = (w / effectiveBarCount) * 0.3f
    val barRadius = CornerRadius(barWidth * 0.4f, barWidth * 0.4f)

    val playheadX = progress * w

    // Draw bars
    for (i in 0 until effectiveBarCount) {
        val dataIndex = (i * step).toInt().coerceIn(0, peaks.size - 1)
        val peakVal = peaks[dataIndex].coerceIn(0f, 1f)
        val rmsVal = rms[dataIndex].coerceIn(0f, 1f)

        val x = i * (barWidth + barGap) + barGap * 0.5f

        // Determine if this bar is in played or unplayed region
        val isPlayed = x <= playheadX

        // Peak bar (taller, thinner) — use peak amplitude
        val peakHeight = peakVal * maxBarHeight
        val rmsHeight = rmsVal * maxBarHeight

        // Draw RMS bar (background, dimmer)
        val rmsColor = if (isPlayed) playedColor.copy(alpha = 0.4f) else unplayedColor
        drawRoundRect(
            color = rmsColor,
            topLeft = Offset(x, baselineY - rmsHeight),
            size = Size(barWidth, rmsHeight * 2),
            cornerRadius = barRadius
        )

        // Draw Peak bar (foreground, brighter) — slightly narrower
        val peakBarWidth = barWidth * 0.6f
        val peakX = x + (barWidth - peakBarWidth) * 0.5f
        val peakColor = if (isPlayed) playedColor else unplayedColor.copy(alpha = 0.6f)
        drawRoundRect(
            color = peakColor,
            topLeft = Offset(peakX, baselineY - peakHeight),
            size = Size(peakBarWidth, peakHeight * 2),
            cornerRadius = CornerRadius(peakBarWidth * 0.4f, peakBarWidth * 0.4f)
        )
    }

    // Draw playhead line
    val playheadStroke = 2.dp.toPx()
    drawLine(
        color = playheadColor.copy(alpha = 0.8f),
        start = Offset(playheadX, h * 0.08f),
        end = Offset(playheadX, h * 0.92f),
        strokeWidth = playheadStroke,
        cap = StrokeCap.Round
    )

    // Draw thumb circle
    if (showThumb) {
        val thumbRadius = 6.dp.toPx()
        // Glow
        drawCircle(
            color = playheadColor.copy(alpha = 0.2f),
            radius = thumbRadius * 2.5f,
            center = Offset(playheadX, baselineY)
        )
        // Thumb
        drawCircle(
            color = playheadColor,
            radius = thumbRadius,
            center = Offset(playheadX, baselineY)
        )
        // Inner dot
        drawCircle(
            color = playedColor,
            radius = thumbRadius * 0.4f,
            center = Offset(playheadX, baselineY)
        )
    }
}
