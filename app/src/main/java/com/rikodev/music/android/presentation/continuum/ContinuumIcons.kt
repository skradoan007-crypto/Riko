package com.rikodev.music.android.presentation.continuum

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
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

internal fun DrawScope.drawContinuumIcon(
    tab: ContinuumTab,
    center: Offset,
    baseSize: Float,
    activeWeight: Float,
    colors: ContinuumNavColors,
    renderTier: ContinuumRenderTier,
    glowPaint: Paint
) {
    val scale = 1.0f + (0.14f * activeWeight)
    val iconSize = baseSize * scale
    val strokeWidth = (iconSize * 0.088f).coerceAtLeast(1.8f)

    val tint = lerpColor(colors.inactiveIcon, colors.activeIcon, activeWeight)

    when (tab) {
        ContinuumTab.EXPLORE -> drawPlanetIcon(
            center = center,
            size = iconSize,
            strokeWidth = strokeWidth,
            color = tint,
            activeWeight = activeWeight,
            glowColor = colors.silkNeonOrchid,
            renderTier = renderTier,
            glowPaint = glowPaint
        )

        ContinuumTab.LISTEN -> drawHeadphonesIcon(
            center = center,
            size = iconSize,
            strokeWidth = strokeWidth,
            color = tint,
            activeWeight = activeWeight,
            glowColor = colors.silkNeonOrchid,
            renderTier = renderTier,
            glowPaint = glowPaint
        )

        ContinuumTab.LIBRARY -> drawStackedLayersIcon(
            center = center,
            size = iconSize,
            strokeWidth = strokeWidth,
            color = tint,
            activeWeight = activeWeight,
            glowColor = colors.silkNeonOrchid,
            renderTier = renderTier,
            glowPaint = glowPaint
        )
    }
}

private fun DrawScope.drawPlanetIcon(
    center: Offset,
    size: Float,
    strokeWidth: Float,
    color: Color,
    activeWeight: Float,
    glowColor: Color,
    renderTier: ContinuumRenderTier,
    glowPaint: Paint
) {
    val planetRadius = size * 0.30f
    val ringRx = size * 0.52f
    val ringRy = size * 0.165f

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
            startAngleDegrees = -28f,
            sweepAngleDegrees = 236f
        )
    }

    if (activeWeight > 0.05f && renderTier != ContinuumRenderTier.LITE) {
        drawIconGlowPath(planetPath, strokeWidth * 2.2f, glowColor, activeWeight * 0.65f, glowPaint)
        withTransform({ rotate(degrees = -28f, pivot = center) }) {
            drawIconGlowPath(ringPath, strokeWidth * 2.0f, glowColor, activeWeight * 0.55f, glowPaint)
        }
    }

    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(path = planetPath, color = color, style = stroke)

    withTransform({ rotate(degrees = -28f, pivot = center) }) {
        drawPath(path = ringPath, color = color, style = stroke)
    }
}

private fun DrawScope.drawHeadphonesIcon(
    center: Offset,
    size: Float,
    strokeWidth: Float,
    color: Color,
    activeWeight: Float,
    glowColor: Color,
    renderTier: ContinuumRenderTier,
    glowPaint: Paint
) {
    val halfW = size * 0.38f
    val bandTop = center.y - size * 0.36f
    val bandBottom = center.y + size * 0.08f

    val archPath = Path().apply {
        addArc(
            oval = Rect(
                left = center.x - halfW,
                top = bandTop,
                right = center.x + halfW,
                bottom = bandBottom + size * 0.28f
            ),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 180f
        )
        moveTo(center.x - halfW, center.y - size * 0.01f)
        lineTo(center.x - halfW, center.y + size * 0.14f)
        moveTo(center.x + halfW, center.y - size * 0.01f)
        lineTo(center.x + halfW, center.y + size * 0.14f)
    }

    val cupW = size * 0.17f
    val cupH = size * 0.31f
    val cupTop = center.y + size * 0.04f
    val cupCorner = CornerRadius(cupW * 0.45f, cupW * 0.45f)

    val cupsPath = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = center.x - halfW - cupW * 0.15f,
                top = cupTop,
                right = center.x - halfW + cupW * 0.85f,
                bottom = cupTop + cupH,
                cornerRadius = cupCorner
            )
        )
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = center.x + halfW - cupW * 0.85f,
                top = cupTop,
                right = center.x + halfW + cupW * 0.15f,
                bottom = cupTop + cupH,
                cornerRadius = cupCorner
            )
        )
    }

    if (activeWeight > 0.05f && renderTier != ContinuumRenderTier.LITE) {
        drawIconGlowPath(archPath, strokeWidth * 2.4f, glowColor, activeWeight * 0.7f, glowPaint)
        drawIconGlowPath(cupsPath, strokeWidth * 2.2f, glowColor, activeWeight * 0.65f, glowPaint)
    }

    val stroke = Stroke(width = strokeWidth * 1.15f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(path = archPath, color = color, style = stroke)
    drawPath(path = cupsPath, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path = cupsPath, color = color.copy(alpha = color.alpha * 0.25f))
}

private fun DrawScope.drawStackedLayersIcon(
    center: Offset,
    size: Float,
    strokeWidth: Float,
    color: Color,
    activeWeight: Float,
    glowColor: Color,
    renderTier: ContinuumRenderTier,
    glowPaint: Paint
) {
    val halfW = size * 0.42f
    val diamondHalfH = size * 0.18f
    val layerGap = size * 0.19f
    val topCenterY = center.y - layerGap * 0.92f

    val path = Path().apply {
        // Top closed isometric diamond
        moveTo(center.x, topCenterY - diamondHalfH)
        lineTo(center.x + halfW, topCenterY)
        lineTo(center.x, topCenterY + diamondHalfH)
        lineTo(center.x - halfW, topCenterY)
        close()

        // Middle chevron
        val midY = topCenterY + layerGap
        moveTo(center.x - halfW, midY)
        lineTo(center.x, midY + diamondHalfH)
        lineTo(center.x + halfW, midY)

        // Bottom chevron
        val botY = topCenterY + layerGap * 2f
        moveTo(center.x - halfW, botY)
        lineTo(center.x, botY + diamondHalfH)
        lineTo(center.x + halfW, botY)
    }

    if (activeWeight > 0.05f && renderTier != ContinuumRenderTier.LITE) {
        drawIconGlowPath(path, strokeWidth * 2.2f, glowColor, activeWeight * 0.65f, glowPaint)
    }

    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(path = path, color = color, style = stroke)
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
        glowPaint.maskFilter = BlurMaskFilter((strokeWidth * 1.6f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        canvas.nativeCanvas.drawPath(path.asAndroidPath(), glowPaint)
    }
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