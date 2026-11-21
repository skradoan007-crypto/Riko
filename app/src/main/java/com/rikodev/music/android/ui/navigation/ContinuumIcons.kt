package com.rikodev.music.android.ui.navigation

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

internal fun DrawScope.drawContinuumIcon(
    tab: ContinuumTab,
    center: Offset,
    baseSize: Float,
    activeWeight: Float,
    colors: ContinuumNavColors,
    renderTier: ContinuumRenderTier,
    glowPaint: Paint,
    animProgress: Float = 0f // One-shot 0f -> 1f on tab switch; 0f or 1f = exact resting pose
) {
    val scale = 1.0f + (0.14f * activeWeight)
    val iconSize = baseSize * scale
    val tint = lerpColor(colors.inactiveIcon, colors.activeIcon, activeWeight)
    val t = animProgress.coerceIn(0f, 1f)

    when (tab) {
        ContinuumTab.EXPLORE -> drawPlanetIcon(
            center = center,
            size = iconSize * 1.06f,
            strokeWidth = (iconSize * 0.062f).coerceAtLeast(1.5f),
            color = tint,
            activeWeight = activeWeight,
            glowColor = colors.silkBrightLavender,
            renderTier = renderTier,
            glowPaint = glowPaint,
            animProgress = t
        )

        ContinuumTab.LISTEN -> drawHeadphonesIcon(
            center = center,
            size = iconSize * 1.04f,
            strokeWidth = (iconSize * 0.076f).coerceAtLeast(1.8f),
            color = tint,
            activeWeight = activeWeight,
            glowColor = colors.silkBrightLavender,
            renderTier = renderTier,
            glowPaint = glowPaint,
            animProgress = t
        )

        ContinuumTab.LIBRARY -> drawStackedLayersIcon(
            center = center,
            size = iconSize * 1.02f,
            strokeWidth = (iconSize * 0.066f).coerceAtLeast(1.6f),
            color = tint,
            activeWeight = activeWeight,
            glowColor = colors.silkBrightLavender,
            renderTier = renderTier,
            glowPaint = glowPaint,
            animProgress = t
        )
    }
}

/**
 * EXPLORE (Neptune / Ringed Planet):
 * Smooth 360-degree space orbital spin + subtle 3D ring tilt wobble + twinkling star sparkles + orbital shine.
 * Settles back to exact -30 deg resting pose at t = 0f / 1f.
 */
private fun DrawScope.drawPlanetIcon(
    center: Offset,
    size: Float,
    strokeWidth: Float,
    color: Color,
    activeWeight: Float,
    glowColor: Color,
    renderTier: ContinuumRenderTier,
    glowPaint: Paint,
    animProgress: Float
) {
    val isAnimating = animProgress > 0.001f && animProgress < 0.999f
    val env = if (isAnimating) sin(animProgress * PI).toFloat() else 0f
    val spinEase = if (isAnimating) easeInOutCubic(animProgress) else 0f

    val planetPop = 1f + 0.09f * env
    val planetRadius = size * 0.31f * planetPop
    val ringRx = size * 0.56f * (1f + 0.06f * env)
    val ringRy = size * 0.16f * (1f + 0.38f * sin(animProgress * 2f * PI).toFloat() * env)
    val ringRotation = -30f + 360f * spinEase

    val planetPath = Path().apply {
        addOval(
            Rect(
                left = center.x - planetRadius,
                top = center.y - planetRadius,
                right = center.x + planetRadius,
                bottom = center.y + planetRadius
            )
        )
    }

    val ringPath = Path().apply {
        addArc(
            oval = Rect(
                left = center.x - ringRx,
                top = center.y - ringRy,
                right = center.x + ringRx,
                bottom = center.y + ringRy
            ),
            startAngleDegrees = -24f,
            sweepAngleDegrees = 228f
        )
    }

    val glowBoost = 1f + 0.45f * env
    if (activeWeight > 0.05f && renderTier != ContinuumRenderTier.LITE) {
        drawIconGlowPath(planetPath, strokeWidth * 2.4f, glowColor, (activeWeight * 0.65f * glowBoost).coerceAtMost(0.95f), glowPaint)
        withTransform({ rotate(degrees = ringRotation, pivot = center) }) {
            drawIconGlowPath(ringPath, strokeWidth * 2.2f, glowColor, (activeWeight * 0.55f * glowBoost).coerceAtMost(0.90f), glowPaint)
        }
    }

    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(path = planetPath, color = color, style = stroke)

    withTransform({ rotate(degrees = ringRotation, pivot = center) }) {
        drawPath(path = ringPath, color = color, style = stroke)

        // Bright orbital star-glint riding along the spinning ring
        if (isAnimating) {
            val glintAlpha = env * 0.95f
            val angleRad = ((-24f + 228f * spinEase) * PI / 180.0)
            val gx = center.x + ringRx * cos(angleRad).toFloat()
            val gy = center.y + ringRy * sin(angleRad).toFloat()
            drawCircle(
                color = Color.White.copy(alpha = glintAlpha),
                radius = strokeWidth * (1.25f + 0.5f * env),
                center = Offset(gx, gy)
            )
        }
    }

    // Space star sparkles (twinkle & fade cleanly before t = 1f)
    if (isAnimating) {
        val star1Pulse = bellPulse(animProgress, center = 0.36f, width = 0.20f)
        if (star1Pulse > 0.02f) {
            drawSpaceSparkle(
                cx = center.x + size * 0.48f,
                cy = center.y - size * 0.42f,
                radius = size * 0.16f * star1Pulse,
                rotationDeg = 45f * animProgress,
                alpha = star1Pulse * 0.95f
            )
        }

        val star2Pulse = bellPulse(animProgress, center = 0.66f, width = 0.18f)
        if (star2Pulse > 0.02f) {
            drawSpaceSparkle(
                cx = center.x - size * 0.46f,
                cy = center.y + size * 0.36f,
                radius = size * 0.12f * star2Pulse,
                rotationDeg = -60f * animProgress,
                alpha = star2Pulse * 0.85f
            )
        }
    }
}

/**
 * LISTEN (Headphones):
 * Playful 2-beat rhythmic groove sway (head-bob) + bass-thump earcup bounce + brief sound pulse.
 * Settles back to exact resting pose at t = 0f / 1f.
 */
private fun DrawScope.drawHeadphonesIcon(
    center: Offset,
    size: Float,
    strokeWidth: Float,
    color: Color,
    activeWeight: Float,
    glowColor: Color,
    renderTier: ContinuumRenderTier,
    glowPaint: Paint,
    animProgress: Float
) {
    val isAnimating = animProgress > 0.001f && animProgress < 0.999f
    val env = if (isAnimating) sin(animProgress * PI).toFloat() else 0f

    // 2 rhythmic bass beats inside the 720ms window
    val beat1 = if (isAnimating) bellPulse(animProgress, center = 0.26f, width = 0.13f) else 0f
    val beat2 = if (isAnimating) bellPulse(animProgress, center = 0.62f, width = 0.14f) else 0f
    val beat = (beat1 + beat2 * 0.82f).coerceAtMost(1f)

    // Smooth left-right-left head-bob tilt
    val swayDeg = if (isAnimating) {
        sin(animProgress * 3f * PI).toFloat() * (1f - animProgress * 0.40f) * 14f * env
    } else 0f
    val bounceY = -size * 0.085f * beat
    val cupSpread = size * 0.052f * beat
    val cupStretchH = 1f + 0.09f * beat

    val halfW = size * 0.36f + cupSpread * 0.45f
    val bandTop = center.y - size * 0.37f - size * 0.02f * beat
    val bandBottom = center.y + size * 0.10f

    val archPath = Path().apply {
        addArc(
            oval = Rect(
                left = center.x - halfW,
                top = bandTop,
                right = center.x + halfW,
                bottom = bandBottom + size * 0.25f
            ),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 180f
        )
        moveTo(center.x - halfW, center.y - size * 0.01f)
        lineTo(center.x - halfW, center.y + size * 0.14f)
        moveTo(center.x + halfW, center.y - size * 0.01f)
        lineTo(center.x + halfW, center.y + size * 0.14f)
    }

    val cupW = size * 0.165f
    val cupH = size * 0.32f * cupStretchH
    val cupTop = center.y + size * 0.03f - (cupH - size * 0.32f) * 0.5f
    val cupCorner = CornerRadius(cupW * 0.42f, cupW * 0.42f)

    val cupsPath = Path().apply {
        addRoundRect(
            RoundRect(
                left = center.x - halfW - cupW * 0.12f - cupSpread * 0.35f,
                top = cupTop,
                right = center.x - halfW + cupW * 0.88f - cupSpread * 0.35f,
                bottom = cupTop + cupH,
                cornerRadius = cupCorner
            )
        )
        addRoundRect(
            RoundRect(
                left = center.x + halfW - cupW * 0.88f + cupSpread * 0.35f,
                top = cupTop,
                right = center.x + halfW + cupW * 0.12f + cupSpread * 0.35f,
                bottom = cupTop + cupH,
                cornerRadius = cupCorner
            )
        )
    }

    val glowBoost = 1f + 0.38f * beat
    withTransform({
        translate(left = 0f, top = bounceY)
        rotate(degrees = swayDeg, pivot = center)
    }) {
        if (activeWeight > 0.05f && renderTier != ContinuumRenderTier.LITE) {
            drawIconGlowPath(archPath, strokeWidth * 2.4f, glowColor, (activeWeight * 0.72f * glowBoost).coerceAtMost(0.95f), glowPaint)
            drawIconGlowPath(cupsPath, strokeWidth * 2.2f, glowColor, (activeWeight * 0.65f * glowBoost).coerceAtMost(0.92f), glowPaint)
        }

        val stroke = Stroke(width = strokeWidth * 1.1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(path = archPath, color = color, style = stroke)
        drawPath(path = cupsPath, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(path = cupsPath, color = color.copy(alpha = color.alpha * (0.30f + 0.25f * beat)))

        // Subtle inner acoustic beat pulse between earcups on bass thump
        if (beat > 0.12f) {
            val waveR = size * (0.10f + 0.06f * beat)
            val waveAlpha = (beat * 0.68f).coerceIn(0f, 0.72f)
            val waveStroke = Stroke(width = strokeWidth * 0.72f, cap = StrokeCap.Round)
            drawArc(
                color = Color.White.copy(alpha = waveAlpha),
                startAngle = -35f,
                sweepAngle = 70f,
                useCenter = false,
                topLeft = Offset(center.x - waveR * 1.35f, center.y + size * 0.15f - waveR),
                size = androidx.compose.ui.geometry.Size(waveR * 2f, waveR * 2f),
                style = waveStroke
            )
            drawArc(
                color = Color.White.copy(alpha = waveAlpha),
                startAngle = 145f,
                sweepAngle = 70f,
                useCenter = false,
                topLeft = Offset(center.x - waveR * 0.65f, center.y + size * 0.15f - waveR),
                size = androidx.compose.ui.geometry.Size(waveR * 2f, waveR * 2f),
                style = waveStroke
            )
        }
    }
}

/**
 * LIBRARY (3-Layer Isometric Stack):
 * Layers fan apart vertically, then sharply knock/clap into each other ("bari khabe")
 * with elastic recoil + collision glow flash, then settle back to exact resting stack.
 */
private fun DrawScope.drawStackedLayersIcon(
    center: Offset,
    size: Float,
    strokeWidth: Float,
    color: Color,
    activeWeight: Float,
    glowColor: Color,
    renderTier: ContinuumRenderTier,
    glowPaint: Paint,
    animProgress: Float
) {
    val isAnimating = animProgress > 0.001f && animProgress < 0.999f

    // Piecewise Lottie-style spring-clap curve:
    // 0.00..0.28: Fan open (gap 1.0 -> 1.52)
    // 0.28..0.46: Sharp clap/knock together! (gap 1.52 -> 0.48)
    // 0.46..0.66: Elastic rebound apart (gap 0.48 -> 1.24)
    // 0.66..0.82: Second gentle tap (gap 1.24 -> 0.80)
    // 0.82..1.00: Smooth settle back to exact 1.00
    val gapMultiplier = if (!isAnimating) {
        1f
    } else {
        when {
            animProgress < 0.28f -> lerpF(1.00f, 1.52f, easeOutCubic(animProgress / 0.28f))
            animProgress < 0.46f -> lerpF(1.52f, 0.48f, easeInCubic((animProgress - 0.28f) / 0.18f))
            animProgress < 0.66f -> lerpF(0.48f, 1.24f, easeOutCubic((animProgress - 0.46f) / 0.20f))
            animProgress < 0.82f -> lerpF(1.24f, 0.80f, easeInOutCubic((animProgress - 0.66f) / 0.16f))
            else -> lerpF(0.80f, 1.00f, easeOutCubic((animProgress - 0.82f) / 0.18f))
        }
    }

    // Impact squash & glow flash right when layers collide at t ~ 0.46 and t ~ 0.82
    val clap1 = if (isAnimating) bellPulse(animProgress, center = 0.46f, width = 0.08f) else 0f
    val clap2 = if (isAnimating) bellPulse(animProgress, center = 0.82f, width = 0.08f) else 0f
    val impact = (clap1 + clap2 * 0.45f).coerceAtMost(1f)

    // Subtle playful tilt during fan-out, snapping straight on clap
    val topTiltDeg = if (isAnimating) {
        sin(animProgress * 2f * PI).toFloat() * (1f - animProgress) * 7f
    } else 0f

    val squashX = 1f + 0.095f * impact
    val halfW = size * 0.41f * squashX
    val diamondHalfH = size * 0.17f * (1f - 0.06f * impact)
    val baseGap = size * 0.185f
    val layerGap = baseGap * gapMultiplier

    // Keep middle layer anchored vertically so top & bottom visibly slam into it
    val midY = center.y + baseGap * 0.08f
    val topY = midY - layerGap
    val botY = midY + layerGap

    val topLayerPath = Path().apply {
        moveTo(center.x, topY - diamondHalfH)
        lineTo(center.x + halfW, topY)
        lineTo(center.x, topY + diamondHalfH)
        lineTo(center.x - halfW, topY)
        close()
    }

    val lowerLayersPath = Path().apply {
        moveTo(center.x - halfW, midY)
        lineTo(center.x, midY + diamondHalfH)
        lineTo(center.x + halfW, midY)

        moveTo(center.x - halfW, botY)
        lineTo(center.x, botY + diamondHalfH)
        lineTo(center.x + halfW, botY)
    }

    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val glowBoost = 1f + 0.55f * impact

    // Draw lower & middle layers
    if (activeWeight > 0.05f && renderTier != ContinuumRenderTier.LITE) {
        drawIconGlowPath(lowerLayersPath, strokeWidth * (2.3f + 0.8f * impact), glowColor, (activeWeight * 0.65f * glowBoost).coerceAtMost(0.98f), glowPaint)
    }
    drawPath(path = lowerLayersPath, color = color, style = stroke)

    // Draw top diamond with subtle playful tilt into the clap
    withTransform({ rotate(degrees = topTiltDeg, pivot = Offset(center.x, topY)) }) {
        if (activeWeight > 0.05f && renderTier != ContinuumRenderTier.LITE) {
            drawIconGlowPath(topLayerPath, strokeWidth * (2.3f + 0.8f * impact), glowColor, (activeWeight * 0.68f * glowBoost).coerceAtMost(0.98f), glowPaint)
        }
        drawPath(path = topLayerPath, color = color, style = stroke)
        if (impact > 0.05f) {
            drawPath(path = topLayerPath, color = Color.White.copy(alpha = impact * 0.28f))
        }
    }
}

private fun DrawScope.drawSpaceSparkle(
    cx: Float,
    cy: Float,
    radius: Float,
    rotationDeg: Float,
    alpha: Float
) {
    if (radius <= 0.5f || alpha <= 0.01f) return
    val inner = radius * 0.24f
    val starPath = Path().apply {
        moveTo(cx, cy - radius)
        quadraticTo(cx + inner * 0.35f, cy - inner * 0.35f, cx + radius, cy)
        quadraticTo(cx + inner * 0.35f, cy + inner * 0.35f, cx, cy + radius)
        quadraticTo(cx - inner * 0.35f, cy + inner * 0.35f, cx - radius, cy)
        quadraticTo(cx - inner * 0.35f, cy - inner * 0.35f, cx, cy - radius)
        close()
    }
    withTransform({ rotate(degrees = rotationDeg, pivot = Offset(cx, cy)) }) {
        drawPath(path = starPath, color = Color.White.copy(alpha = alpha.coerceIn(0f, 1f)))
    }
}

private fun DrawScope.drawIconGlowPath(
    path: Path,
    strokeWidth: Float,
    glowColor: Color,
    alpha: Float,
    glowPaint: Paint
) {
    drawIntoCanvas { canvas ->
        glowPaint.reset()
        glowPaint.isAntiAlias = true
        glowPaint.style = Paint.Style.STROKE
        glowPaint.strokeCap = Paint.Cap.ROUND
        glowPaint.strokeJoin = Paint.Join.ROUND
        glowPaint.strokeWidth = strokeWidth
        glowPaint.color = glowColor.copy(alpha = alpha.coerceIn(0f, 1f)).toArgb()
        glowPaint.maskFilter = BlurMaskFilter((strokeWidth * 1.5f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        canvas.nativeCanvas.drawPath(path.asAndroidPath(), glowPaint)
    }
}

private fun bellPulse(t: Float, center: Float, width: Float): Float {
    val d = (t - center) / width.coerceAtLeast(0.001f)
    return exp(-(d * d))
}

private fun easeInCubic(t: Float): Float {
    val c = t.coerceIn(0f, 1f)
    return c * c * c
}

private fun easeOutCubic(t: Float): Float {
    val c = 1f - t.coerceIn(0f, 1f)
    return 1f - c * c * c
}

private fun easeInOutCubic(t: Float): Float {
    val c = t.coerceIn(0f, 1f)
    return if (c < 0.5f) 4f * c * c * c else 1f - (-2f * c + 2f).pow(3) / 2f
}

private fun lerpF(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction.coerceIn(0f, 1f)
}

internal fun lerpColor(start: Color, stop: Color, fraction: Float): Color {
    val t = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (stop.red - start.red) * t,
        green = start.green + (stop.green - start.green) * t,
        blue = start.blue + (stop.blue - start.blue) * t,
        alpha = start.alpha + (stop.alpha - start.alpha) * t
    )
}
