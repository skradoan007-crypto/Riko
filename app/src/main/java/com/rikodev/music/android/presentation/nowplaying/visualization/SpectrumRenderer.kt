package com.rikodev.music.android.presentation.nowplaying.visualization

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — SPECTRUM RENDERER (visualizer_plan §7 + §12 + §14)
//
// ONE Canvas, one batch drawing pass — never one Composable per bar.
// Two styles (v1): AURORA (signature) + CLASSIC (precise alternate).
//
// Micro-reactivity from the SAME fft data (plan §12):
//   Bass    → lower glow expansion
//   Mid     → contour movement
//   Treble  → micro detail shimmer
//   Overall → master intensity
// ─────────────────────────────────────────────────────────────────────────────

data class SpectrumDrawColors(
    val waveStart: Color,
    val waveEnd: Color,
    val primary: Color,
    val ambientGlow: Color
)

fun DrawScope.drawSpectrum(
    style: SpectrumStyle,
    levels: FloatArray,
    peaks: FloatArray?,
    colors: SpectrumDrawColors,
    beatKick: Float,
    bassEnergy: Float,
    midEnergy: Float,
    trebleEnergy: Float,
    overallEnergy: Float,
    phase: Float,
    glowEnabled: Boolean,
    barGap: Dp = 4.dp,
    history: SpectrogramHistory? = null,
    glowScale: Float = 1f
) {
    when (style) {
        SpectrumStyle.AURORA -> drawAurora(
            levels, colors, beatKick, bassEnergy, midEnergy,
            trebleEnergy, overallEnergy, phase, glowEnabled, glowScale
        )
        SpectrumStyle.CLASSIC -> drawClassic(
            levels, peaks, colors, beatKick, phase, barGap
        )
        SpectrumStyle.SPECTROGRAM -> {
            if (history != null) {
                drawSpectrogram(levels, colors, history)
            } else {
                drawShapeOrbital(
                    levels, colors, beatKick, bassEnergy, midEnergy,
                    trebleEnergy, overallEnergy, phase, glowEnabled, glowScale
                )
            }
        }
        SpectrumStyle.SHAPE -> drawShapeOrbital(
            levels, colors, beatKick, bassEnergy, midEnergy,
            trebleEnergy, overallEnergy, phase, glowEnabled, glowScale
        )
        SpectrumStyle.LINE -> drawLineSpectrum(
            levels, colors, beatKick, bassEnergy, overallEnergy,
            phase, glowEnabled, glowScale
        )
        SpectrumStyle.DOT -> drawDotSpectrum(
            levels, colors, beatKick, bassEnergy, midEnergy,
            trebleEnergy, overallEnergy, phase, glowEnabled, glowScale
        )
        SpectrumStyle.WAVEFORM -> drawWaveformBars(
            levels, colors, beatKick, overallEnergy, phase, glowEnabled, glowScale
        )
    }
}

private fun DrawScope.drawAurora(
    levels: FloatArray,
    colors: SpectrumDrawColors,
    beatKick: Float,
    bassEnergy: Float,
    midEnergy: Float,
    trebleEnergy: Float,
    overallEnergy: Float,
    phase: Float,
    glowEnabled: Boolean,
    glowScale: Float = 1f
) {
    if (levels.isEmpty()) return
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return
    val cx = w / 2f
    val cy = h / 2f
    val n = levels.size

    val master = (0.35f + 0.65f * overallEnergy.coerceIn(0f, 1f)) * beatKick
    val maxAmp = h * 0.42f

    // Layer 3 (back): ambient glow — bass expands it; glowScale = studio knob.
    if (glowEnabled) {
        val glowRadius = w * (0.45f + 0.25f * bassEnergy.coerceIn(0f, 1f))
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    colors.ambientGlow.copy(alpha = glowScale.coerceIn(0f, 1f)),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = glowRadius.coerceAtLeast(1f)
            )
        )
    }

    // Smooth mirrored contour points. Mid moves the contour, treble adds
    // micro detail, phase keeps 60Hz motion alive between 30Hz DSP frames.
    val topY = FloatArray(n) { i ->
        val t = i / (n - 1f).coerceAtLeast(1f)
        val micro = 1f + 0.06f * trebleEnergy * sin(t * 25.13f + phase * 2.2f + i)
        val sway = 1f + 0.10f * midEnergy * sin(t * 6.28f + phase * 0.9f)
        val v = (levels[i].coerceIn(0f, 1f) * micro * sway * master).coerceIn(0.015f, 1f)
        cy - v * maxAmp
    }

    fun smoothPath(mirror: Boolean): Path = Path().apply {
        for (i in 0 until n) {
            val x = i / (n - 1f).coerceAtLeast(1f) * w
            val y = if (mirror) cy + (cy - topY[i]) else topY[i]
            if (i == 0) moveTo(x, y)
            else {
                val px = (i - 1) / (n - 1f).coerceAtLeast(1f) * w
                val py = if (mirror) cy + (cy - topY[i - 1]) else topY[i - 1]
                val mx = (px + x) / 2f
                quadraticBezierTo(px, py, mx, (py + y) / 2f)
                if (i == n - 1) lineTo(x, y)
            }
        }
    }

    val topPath = smoothPath(mirror = false)
    val bottomPath = smoothPath(mirror = true)

    // Layer 2: soft filled gradient spectrum (mirrored).
    val fill = Path().apply {
        addPath(topPath)
        for (i in n - 1 downTo 0) {
            val x = i / (n - 1f).coerceAtLeast(1f) * w
            lineTo(x, cy + (cy - topY[i]))
        }
        close()
    }
    drawPath(
        path = fill,
        brush = Brush.verticalGradient(
            colors = listOf(
                colors.waveStart.copy(alpha = 0.85f),
                colors.waveEnd.copy(alpha = 0.85f),
                colors.waveEnd.copy(alpha = 0.55f)
            ),
            startY = 0f,
            endY = h
        )
    )

    // Layer 1: thin precise contour (top + mirrored bottom).
    val contourColor = Color.White.copy(alpha = 0.75f)
    drawPath(topPath, contourColor, style = Stroke(width = 2.dp.toPx()))
    drawPath(bottomPath, colors.primary.copy(alpha = 0.45f), style = Stroke(width = 1.5.dp.toPx()))

    // Subtle reflection under the spectrum.
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                colors.waveEnd.copy(alpha = 0.18f),
                Color.Transparent
            ),
            startY = cy + maxAmp * 0.4f,
            endY = h
        )
    )
}

private fun DrawScope.drawClassic(
    levels: FloatArray,
    peaks: FloatArray?,
    colors: SpectrumDrawColors,
    beatKick: Float,
    phase: Float,
    barGap: Dp
) {
    if (levels.isEmpty()) return
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return
    val cy = h / 2f
    val n = levels.size
    // Phase-driven shimmer keeps identical band values alive at display rate.
    val gap = barGap.toPx()
    val bw = ((w - gap * (n - 1)) / n).coerceAtLeast(1f)
    val maxBar = h * 0.42f
    val minBar = 3.dp.toPx()
    val barBrush = Brush.verticalGradient(listOf(colors.waveStart, colors.waveEnd))
    for (i in 0 until n) {
        val shimmer = 0.94f + 0.06f * sin(i * 0.7f + phase * 1.8f)
        val bh = (levels[i].coerceIn(0f, 1f) * shimmer * maxBar * beatKick).coerceAtLeast(minBar)
        val x = i * (bw + gap)
        drawRoundRect(
            brush = barBrush,
            topLeft = Offset(x, cy - bh),
            size = Size(bw, bh * 2f),
            cornerRadius = CornerRadius(bw / 2f, bw / 2f)
        )
        if (peaks != null && i < peaks.size) {
            val ph = (peaks[i] * maxBar * beatKick).coerceAtLeast(0f)
            if (ph > bh + 2.dp.toPx()) {
                val capH = 3.dp.toPx()
                val capColor = colors.primary.copy(alpha = 0.85f)
                drawRoundRect(
                    color = capColor,
                    topLeft = Offset(x, cy - ph - capH / 2f),
                    size = Size(bw, capH),
                    cornerRadius = CornerRadius(capH / 2f, capH / 2f)
                )
                drawRoundRect(
                    color = capColor,
                    topLeft = Offset(x, cy + ph - capH / 2f),
                    size = Size(bw, capH),
                    cornerRadius = CornerRadius(capH / 2f, capH / 2f)
                )
            }
        }
    }
}

/**
 * SPECTROGRAM / waterfall (plan 5.2/5.3) � time flows left?right with the
 * newest column at the right edge; bass sits at the bottom. The rolling
 * history owns its bitmap; a frame draw is two blit slices (ring wrap).
 */
private fun DrawScope.drawSpectrogram(
    levels: FloatArray,
    colors: SpectrumDrawColors,
    history: SpectrogramHistory
) {
    if (levels.isEmpty()) return
    history.push(levels)

    val img = history.image
    val cols = img.width
    val rows = img.height
    val w = size.width.toInt().coerceAtLeast(1)
    val h = size.height.toInt().coerceAtLeast(1)
    val oldest = history.writeColumn
    val leftW = ((cols - oldest) * w / cols).coerceIn(0, w)

    if (leftW > 0) {
        drawImage(
            image = img,
            srcOffset = IntOffset(oldest, 0),
            srcSize = IntSize(cols - oldest, rows),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(leftW, h)
        )
    }
    if (oldest > 0) {
        drawImage(
            image = img,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(oldest, rows),
            dstOffset = IntOffset(leftW, 0),
            dstSize = IntSize(w - leftW, h)
        )
    }
}

/**
 * SHAPE / orbital (plan 5.2) � concentric pressure rings + orbiting sparks.
 * Shape VOCABULARY reimplemented independently from the projectM reference
 * (LGPL � no external code); every radius is band-driven from the canonical
 * SpectrumBands. Zero allocation per frame.
 */
private fun DrawScope.drawShapeOrbital(
    levels: FloatArray,
    colors: SpectrumDrawColors,
    beatKick: Float,
    bassEnergy: Float,
    midEnergy: Float,
    trebleEnergy: Float,
    overallEnergy: Float,
    phase: Float,
    glowEnabled: Boolean,
    glowScale: Float = 1f
) {
    if (levels.isEmpty()) return
    val cx = size.width / 2f
    val cy = size.height / 2f
    val maxR = min(cx, cy) * 0.92f
    val ringStroke = 2.dp.toPx()

    // Core glow � the song's overall energy.
    if (glowEnabled) {
        drawCircle(
            color = colors.ambientGlow.copy(alpha = (0.14f + 0.26f * overallEnergy) * glowScale.coerceIn(0f, 1f)),
            radius = maxR * (0.34f + 0.10f * beatKick),
            center = Offset(cx, cy)
        )
    }

    // Three pressure rings: bass (inner) ? mid ? treble (outer).
    drawCircle(
        color = colors.primary.copy(alpha = 0.55f),
        radius = maxR * (0.32f + 0.22f * bassEnergy + 0.08f * beatKick),
        center = Offset(cx, cy),
        style = Stroke(width = ringStroke)
    )
    drawCircle(
        color = colors.waveStart.copy(alpha = 0.45f),
        radius = maxR * (0.55f + 0.18f * midEnergy),
        center = Offset(cx, cy),
        style = Stroke(width = ringStroke)
    )
    drawCircle(
        color = colors.waveEnd.copy(alpha = 0.40f),
        radius = maxR * (0.78f + 0.14f * trebleEnergy),
        center = Offset(cx, cy),
        style = Stroke(width = ringStroke)
    )

    // Orbiting sparks � one per band slice, radius = its level, angle = phase.
    val n = levels.size
    val sparks = min(n, 24)
    val dotBase = 3.dp.toPx()
    for (i in 0 until sparks) {
        val lvl = levels[i * n / sparks].coerceIn(0f, 1f)
        val ang = phase * 0.6f + i * (2.0 * Math.PI / sparks).toFloat()
        val r = maxR * (0.42f + 0.50f * lvl) + beatKick * maxR * 0.07f
        drawCircle(
            color = colors.ambientGlow.copy(alpha = 0.35f + 0.60f * lvl),
            radius = dotBase + 5.dp.toPx() * lvl,
            center = Offset(cx + cos(ang) * r, cy + sin(ang) * r)
        )
    }
}

// -----------------------------------------------------------------------------
// LINE SPECTRUM � smooth interpolated line + gradient fill
// -----------------------------------------------------------------------------

private fun DrawScope.drawLineSpectrum(
    levels: FloatArray,
    colors: SpectrumDrawColors,
    beatKick: Float,
    bassEnergy: Float,
    overallEnergy: Float,
    phase: Float,
    glowEnabled: Boolean,
    glowScale: Float = 1f
) {
    if (levels.isEmpty()) return
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return
    val n = levels.size
    val baselineY = h * 0.72f
    val maxAmp = h * 0.55f * (0.4f + 0.6f * overallEnergy) * beatKick

    val step = w / (n - 1).coerceAtLeast(1)
    val path = Path()
    val fillPath = Path()

    path.moveTo(0f, baselineY - levels[0].coerceIn(0f, 1f) * maxAmp)
    fillPath.moveTo(0f, baselineY)

    for (i in 1 until n) {
        val x = i * step
        val y = baselineY - levels[i].coerceIn(0f, 1f) * maxAmp
        val prevX = (i - 1) * step
        val prevY = baselineY - levels[i - 1].coerceIn(0f, 1f) * maxAmp
        val midX = (prevX + x) / 2f
        val midY = (prevY + y) / 2f
        path.quadraticBezierTo(prevX, prevY, midX, midY)
        fillPath.quadraticBezierTo(prevX, prevY, midX, midY)
    }
    fillPath.lineTo(w, baselineY)
    fillPath.close()

    val fillBrush = Brush.verticalGradient(
        colorStops = arrayOf(
            0.0f to colors.waveStart.copy(alpha = 0.60f * glowScale),
            0.5f to colors.primary.copy(alpha = 0.35f * glowScale),
            1.0f to Color.Transparent
        ),
        startY = baselineY - maxAmp,
        endY = baselineY
    )
    drawPath(fillPath, brush = fillBrush)

    if (glowEnabled) {
        drawPath(
            path = path,
            color = colors.waveStart.copy(alpha = 0.45f * glowScale),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )
    }

    drawPath(
        path = path,
        color = colors.waveStart,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )
}

// -----------------------------------------------------------------------------
// DOT SPECTRUM � individual dots per band, modern minimal look
// -----------------------------------------------------------------------------

private fun DrawScope.drawDotSpectrum(
    levels: FloatArray,
    colors: SpectrumDrawColors,
    beatKick: Float,
    bassEnergy: Float,
    midEnergy: Float,
    trebleEnergy: Float,
    overallEnergy: Float,
    phase: Float,
    glowEnabled: Boolean,
    glowScale: Float = 1f
) {
    if (levels.isEmpty()) return
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return
    val n = levels.size
    val baselineY = h * 0.5f
    val maxAmp = h * 0.4f * (0.4f + 0.6f * overallEnergy) * beatKick

    val step = w / (n + 1)
    val baseDotRadius = 3.dp.toPx()

    for (i in 0 until n) {
        val x = (i + 1) * step
        val lvl = levels[i].coerceIn(0f, 1f)
        val y = baselineY - lvl * maxAmp

        val t = i.toFloat() / (n - 1).coerceAtLeast(1)
        val dotColor = lerpColor(colors.waveStart, colors.waveEnd, t)

        if (glowEnabled) {
            drawCircle(
                color = dotColor.copy(alpha = 0.2f * glowScale),
                radius = baseDotRadius * (2.5f + lvl * 2f),
                center = Offset(x, y)
            )
        }

        drawCircle(
            color = dotColor.copy(alpha = 0.6f + 0.4f * lvl),
            radius = baseDotRadius * (0.8f + lvl * 1.2f),
            center = Offset(x, y)
        )

        drawCircle(
            color = dotColor.copy(alpha = (0.3f + 0.3f * lvl) * 0.5f),
            radius = baseDotRadius * (0.5f + lvl * 0.8f),
            center = Offset(x, baselineY + lvl * maxAmp * 0.4f)
        )
    }
}

// -----------------------------------------------------------------------------
// WAVEFORM BARS � symmetric bars + peak caps, modern minimal
// -----------------------------------------------------------------------------

private fun DrawScope.drawWaveformBars(
    levels: FloatArray,
    colors: SpectrumDrawColors,
    beatKick: Float,
    overallEnergy: Float,
    phase: Float,
    glowEnabled: Boolean,
    glowScale: Float = 1f
) {
    if (levels.isEmpty()) return
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return
    val n = levels.size
    val baselineY = h * 0.5f
    val maxAmp = h * 0.38f * (0.4f + 0.6f * overallEnergy) * beatKick

    val step = w / n
    val barWidth = step * 0.6f
    val barGap = step * 0.4f

    for (i in 0 until n) {
        val x = i * step + barGap / 2f
        val lvl = levels[i].coerceIn(0f, 1f)
        val barHeight = lvl * maxAmp

        val t = i.toFloat() / (n - 1).coerceAtLeast(1)
        val barColor = lerpColor(colors.waveStart, colors.waveEnd, t)

        if (glowEnabled) {
            drawRoundRect(
                color = barColor.copy(alpha = 0.15f * glowScale),
                topLeft = Offset(x - barWidth * 0.2f, baselineY - barHeight * 1.15f),
                size = Size(barWidth * 1.4f, barHeight * 2.3f),
                cornerRadius = CornerRadius(barWidth * 0.5f)
            )
        }

        drawRoundRect(
            color = barColor.copy(alpha = 0.75f + 0.25f * lvl),
            topLeft = Offset(x, baselineY - barHeight),
            size = Size(barWidth, barHeight * 2),
            cornerRadius = CornerRadius(barWidth * 0.3f)
        )

        val peakY = baselineY - barHeight - 2.dp.toPx()
        drawRoundRect(
            color = Color.White.copy(alpha = 0.8f),
            topLeft = Offset(x, peakY),
            size = Size(barWidth, 2.dp.toPx()),
            cornerRadius = CornerRadius(1.dp.toPx())
        )
    }
}

/** Linear interpolation between two colors. */
private fun lerpColor(a: Color, b: Color, t: Float): Color {
    val clampedT = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * clampedT,
        green = a.green + (b.green - a.green) * clampedT,
        blue = a.blue + (b.blue - a.blue) * clampedT,
        alpha = a.alpha + (b.alpha - a.alpha) * clampedT
    )
}
