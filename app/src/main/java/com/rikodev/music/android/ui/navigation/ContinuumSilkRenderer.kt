package com.rikodev.music.android.ui.navigation

import android.graphics.BlurMaskFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlin.math.abs

internal class ContinuumDrawCache {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    val pathA = Path()
    val pathB = Path()
    val pathC = Path()
    val pathD = Path()
    val clipPath = Path()
    val arcRect = RectF()
}

internal fun DrawScope.renderContinuumLayers1To4(
    metrics: ContinuumMetrics,
    activeProgress: Float,
    colors: ContinuumNavColors,
    renderTier: ContinuumRenderTier,
    cache: ContinuumDrawCache,
    idlePulse: Float = 0.5f,
    touchAwaken: Float = 0f,
    selectionWave: Float = 1f
) {
    if (metrics.width <= 2f || metrics.height <= 2f) return

    val w0 = nodeWeight(activeProgress, 0)
    val w1 = nodeWeight(activeProgress, 1)
    val w2 = nodeWeight(activeProgress, 2)
    val weights = floatArrayOf(w0, w1, w2)

    val r0 = lerpFloat(metrics.inactiveRadius, metrics.activeRadius, w0)
    val r1 = lerpFloat(metrics.inactiveRadius, metrics.activeRadius, w1)
    val r2 = lerpFloat(metrics.inactiveRadius, metrics.activeRadius, w2)
    val radii = floatArrayOf(r0, r1, r2)

    val activeX = interpolateSlotX(metrics.slotX, activeProgress)
    val activeR = metrics.activeRadius * (1f + 0.03f * touchAwaken)

    // LAYER 1 & 2: Full-Width Frosted Twilight-Indigo Glass Visor (Unchanged)
    drawFrostedGlassVisor(
        metrics = metrics,
        activeX = activeX,
        colors = colors,
        renderTier = renderTier,
        cache = cache
    )

    // LAYER 3: Taut Diagonal Silk Sweep + Unified Electric-Violet & Lavender Neon Edge Glow
    drawLiquidSilkVeils(
        metrics = metrics,
        weights = weights,
        radii = radii,
        activeX = activeX,
        activeR = activeR,
        idlePulse = idlePulse,
        touchAwaken = touchAwaken,
        colors = colors,
        renderTier = renderTier,
        cache = cache
    )

    // LAYER 4A: Clean Frosted Glass Dome Pockets for Inactive Nodes (Unchanged)
    for (i in 0..2) {
        val inactiveAlpha = (1f - weights[i]).coerceIn(0f, 1f)
        if (inactiveAlpha > 0.02f) {
            drawInactiveGlassPocket(
                cx = metrics.slotX[i],
                cy = metrics.centerY,
                radius = radii[i],
                alphaFactor = inactiveAlpha,
                isLeftSide = i == 0,
                isRightSide = i == 2,
                colors = colors,
                renderTier = renderTier,
                cache = cache
            )
        }
    }

    // LAYER 4B: State C Subtle Expanding Audio-Wave Sonar Arcs on Selection
    if (selectionWave in 0.01f..0.98f && renderTier != ContinuumRenderTier.LITE) {
        drawSelectionWaveArcs(
            cx = activeX,
            cy = metrics.centerY,
            radius = activeR,
            waveProgress = selectionWave,
            colors = colors,
            cache = cache
        )
    }

    // LAYER 4C: Seamless Dark Translucent Core + 110° Curved Bottom Arc Glow + Razor-Thin Unified Liner
    drawFresnelActiveOrb(
        cx = activeX,
        cy = metrics.centerY,
        radius = activeR,
        idlePulse = idlePulse,
        touchAwaken = touchAwaken,
        colors = colors,
        renderTier = renderTier,
        cache = cache
    )
}

private fun DrawScope.drawFrostedGlassVisor(
    metrics: ContinuumMetrics,
    activeX: Float,
    colors: ContinuumNavColors,
    renderTier: ContinuumRenderTier,
    cache: ContinuumDrawCache
) {
    val w = metrics.width
    val h = metrics.height
    val padX = w * 0.006f
    val topCenterY = h * 0.175f
    val topSideY = h * 0.270f
    val bottomY = h * 0.980f
    val cornerR = h * 0.30f

    val visorPath = cache.pathA.apply {
        reset()
        moveTo(padX, topSideY + cornerR * 0.45f)
        cubicTo(
            padX, topSideY,
            w * 0.18f, topCenterY + h * 0.01f,
            w * 0.50f, topCenterY
        )
        cubicTo(
            w * 0.82f, topCenterY + h * 0.01f,
            w - padX, topSideY,
            w - padX, topSideY + cornerR * 0.45f
        )
        cubicTo(
            w - padX, bottomY - h * 0.02f,
            w - padX - cornerR * 0.50f, bottomY,
            w - padX - cornerR * 1.10f, bottomY
        )
        lineTo(padX + cornerR * 1.10f, bottomY)
        cubicTo(
            padX + cornerR * 0.50f, bottomY,
            padX, bottomY - h * 0.02f,
            padX, topSideY + cornerR * 0.45f
        )
        close()
    }

    val topRimPath = cache.pathB.apply {
        reset()
        moveTo(padX + w * 0.008f, topSideY + cornerR * 0.32f)
        cubicTo(
            padX + w * 0.02f, topSideY,
            w * 0.18f, topCenterY + h * 0.01f,
            w * 0.50f, topCenterY
        )
        cubicTo(
            w * 0.82f, topCenterY + h * 0.01f,
            w - padX - w * 0.02f, topSideY,
            w - padX - w * 0.008f, topSideY + cornerR * 0.32f
        )
    }

    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        val p = cache.paint

        if (renderTier != ContinuumRenderTier.LITE) {
            p.reset()
            p.isAntiAlias = true
            p.style = Paint.Style.FILL
            p.color = Color(0x80060412).toArgb()
            p.maskFilter = BlurMaskFilter((h * 0.08f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
            nc.drawPath(visorPath, p)
        }

        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(
            0f, topCenterY, 0f, bottomY,
            intArrayOf(
                colors.glassTop.toArgb(),
                colors.glassMid.toArgb(),
                colors.glassBottom.toArgb()
            ),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawPath(visorPath, p)

        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(
            activeX, metrics.centerY + h * 0.06f, (w * 0.34f).coerceAtLeast(1f),
            intArrayOf(
                colors.orbOuterBloom.copy(alpha = 0.20f).toArgb(),
                colors.silkDeepIndigo.copy(alpha = 0.08f).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.save()
        nc.clipPath(visorPath)
        nc.drawRect(0f, 0f, w, h, p)
        nc.restore()

        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.strokeWidth = (h * 0.012f).coerceAtLeast(1.2f)
        p.strokeCap = Paint.Cap.ROUND
        p.shader = LinearGradient(
            padX, 0f, w - padX, 0f,
            intArrayOf(
                Color.Transparent.toArgb(),
                colors.glassRimSide.toArgb(),
                colors.glassRimCenter.toArgb(),
                colors.glassRimSide.toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.14f, (activeX / w).coerceIn(0.22f, 0.78f), 0.86f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawPath(topRimPath, p)
    }
}

private fun DrawScope.drawLiquidSilkVeils(
    metrics: ContinuumMetrics,
    weights: FloatArray,
    radii: FloatArray,
    activeX: Float,
    activeR: Float,
    idlePulse: Float,
    touchAwaken: Float,
    colors: ContinuumNavColors,
    renderTier: ContinuumRenderTier,
    cache: ContinuumDrawCache
) {
    val w = metrics.width
    val h = metrics.height
    val cy = metrics.centerY
    val x0 = metrics.slotX[0]
    val x1 = metrics.slotX[1]
    val x2 = metrics.slotX[2]
    val r0 = radii[0]
    val r1 = radii[1]
    val r2 = radii[2]

    val waist01 = (x0 + x1) * 0.50f
    val waist12 = (x1 + x2) * 0.50f
    val k = 0.5523f

    // Exact unchanged v13 geometry
    val botWaistY = cy + h * 0.245f
    val topWaist01Y = lerpFloat(cy + h * 0.08f, cy + h * 0.02f, weights[2])
    val topWaist12Y = lerpFloat(cy + h * 0.08f, cy + h * 0.02f, weights[0])

    val y0Diag = lerpFloat(cy - r0 * 0.99f, cy + r0 * 0.99f, weights[0])
    val y1Diag = lerpFloat(cy - r1 * 0.55f, cy + r1 * 0.99f, weights[1])
    val y2Diag = lerpFloat(cy - r2 * 0.99f, cy + r2 * 0.99f, weights[2])
    val centerTopDipY = lerpFloat(cy - r1 * 0.18f, cy + r1 * 0.35f, weights[1])

    val cocoonPath = cache.pathA.apply {
        reset()
        moveTo(x0, cy - r0)
        cubicTo(x0 + r0 * 0.78f, cy - r0 * 0.96f, waist01 - (x1 - x0) * 0.06f, topWaist01Y, x1, centerTopDipY)
        cubicTo(waist12 + (x2 - x1) * 0.06f, topWaist12Y, x2 - r2 * 0.78f, cy - r2 * 0.96f, x2, cy - r2)
        cubicTo(x2 + r2 * k, cy - r2, x2 + r2, cy - r2 * k, x2 + r2, cy)
        cubicTo(x2 + r2, cy + r2 * k, x2 + r2 * k, cy + r2, x2, cy + r2)
        cubicTo(x2 - r2 * 0.72f, cy + r2, waist12 + (x2 - x1) * 0.14f, botWaistY, waist12, botWaistY)
        cubicTo(waist12 - (x2 - x1) * 0.14f, botWaistY, x1 + r1 * 0.72f, cy + r1, x1, cy + r1)
        cubicTo(x1 - r1 * 0.72f, cy + r1, waist01 + (x1 - x0) * 0.14f, botWaistY, waist01, botWaistY)
        cubicTo(waist01 - (x1 - x0) * 0.14f, botWaistY, x0 + r0 * 0.72f, cy + r0, x0, cy + r0)
        cubicTo(x0 - r0 * k, cy + r0, x0 - r0, cy + r0 * k, x0 - r0, cy)
        cubicTo(x0 - r0, cy - r0 * k, x0 - r0 * k, cy - r0, x0, cy - r0)
        close()
    }

    val ribbonA = cache.pathB.apply {
        reset()
        moveTo(x0, y0Diag)
        cubicTo(
            lerpFloat(x0, x1, 0.34f), lerpFloat(y0Diag, y1Diag, 0.18f),
            lerpFloat(x0, x1, 0.68f), lerpFloat(y0Diag, y1Diag, 0.84f),
            x1, y1Diag
        )
        cubicTo(
            lerpFloat(x1, x2, 0.32f), lerpFloat(y1Diag, y2Diag, 0.16f),
            lerpFloat(x1, x2, 0.66f), lerpFloat(y1Diag, y2Diag, 0.82f),
            x2, y2Diag
        )
        lineTo(x2, cy + r2 * 0.96f)
        cubicTo(x2 - r2 * 0.65f, cy + r2 * 0.96f, waist12 + (x2 - x1) * 0.10f, botWaistY, x1, cy + r1 * 0.98f)
        cubicTo(waist01 - (x1 - x0) * 0.10f, botWaistY, x0 + r0 * 0.65f, cy + r0 * 0.96f, x0, cy + r0 * 0.96f)
        close()
    }

    val bottomHorizonPath = cache.pathC.apply {
        reset()
        moveTo(x0, cy + r0)
        cubicTo(x0 + r0 * 0.72f, cy + r0, waist01 - (x1 - x0) * 0.14f, botWaistY, waist01, botWaistY)
        cubicTo(waist01 + (x1 - x0) * 0.14f, botWaistY, x1 - r1 * 0.72f, cy + r1, x1, cy + r1)
        cubicTo(x1 + r1 * 0.72f, cy + r1, waist12 - (x2 - x1) * 0.14f, botWaistY, waist12, botWaistY)
        cubicTo(waist12 + (x2 - x1) * 0.14f, botWaistY, x2 - r2 * 0.72f, cy + r2, x2, cy + r2)
    }

    val energyBoost = 1f + (idlePulse - 0.5f) * 0.08f + touchAwaken * 0.14f

    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        val p = cache.paint

        // 3A. Soft Silk Cocoon Fill
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(
            activeX, cy + activeR * 0.30f, (w * 0.46f).coerceAtLeast(1f),
            intArrayOf(
                colors.silkRoyalViolet.copy(alpha = (0.44f * energyBoost).coerceAtMost(0.64f)).toArgb(),
                colors.silkDeepIndigo.copy(alpha = 0.25f).toArgb(),
                colors.silkDeepIndigo.copy(alpha = 0.10f).toArgb()
            ),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawPath(cocoonPath, p)

        // 3B. Creamy Translucent Diagonal Silk Wing Clipped to Cocoon (Unified Electric-Violet Tone)
        nc.save()
        nc.clipPath(cocoonPath)

        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        if (renderTier != ContinuumRenderTier.LITE) {
            p.maskFilter = BlurMaskFilter((h * 0.028f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        }
        p.shader = RadialGradient(
            activeX, cy + activeR * 0.68f, (w * 0.42f).coerceAtLeast(1f),
            intArrayOf(
                colors.orbOuterBloom.copy(alpha = (0.54f * energyBoost).coerceAtMost(0.74f)).toArgb(),
                colors.silkElectricViolet.copy(alpha = 0.30f).toArgb(),
                colors.silkDeepIndigo.copy(alpha = 0.05f).toArgb()
            ),
            floatArrayOf(0f, 0.46f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawPath(ribbonA, p)
        nc.restore()

        // 3C. Smooth Low Bottom Outer Silk Rim
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = (h * 0.011f).coerceAtLeast(1.2f)
        p.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        if (renderTier == ContinuumRenderTier.ULTRA) {
            p.maskFilter = BlurMaskFilter((h * 0.008f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        }
        p.shader = RadialGradient(
            activeX, cy + activeR * 0.85f, (w * 0.48f).coerceAtLeast(1f),
            intArrayOf(
                colors.orbFlareMid.copy(alpha = 0.58f).toArgb(),
                colors.orbOuterBloom.copy(alpha = 0.30f).toArgb(),
                colors.pocketRim.copy(alpha = 0.16f).toArgb()
            ),
            floatArrayOf(0f, 0.50f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawPath(bottomHorizonPath, p)

        // 3D. Unified Vibrant Electric-Violet + Lavender Neon Glow on Diagonal Silk Edge
        val diag1 = cache.pathD.apply {
            reset()
            moveTo(x0, y0Diag)
            cubicTo(
                lerpFloat(x0, x1, 0.34f), lerpFloat(y0Diag, y1Diag, 0.14f),
                lerpFloat(x0, x1, 0.66f), lerpFloat(y0Diag, y1Diag, 0.88f),
                x1, y1Diag
            )
            cubicTo(
                lerpFloat(x1, x2, 0.34f), lerpFloat(y1Diag, y2Diag, 0.12f),
                lerpFloat(x1, x2, 0.66f), lerpFloat(y1Diag, y2Diag, 0.86f),
                x2, y2Diag
            )
        }

        val gp = cache.glowPaint
        // Wide vibrant electric-violet neon aura matching the orb liner halo
        gp.reset()
        gp.isAntiAlias = true
        gp.style = Paint.Style.STROKE
        gp.strokeCap = Paint.Cap.ROUND
        gp.strokeWidth = (h * 0.044f).coerceAtLeast(3.2f)
        gp.xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
        if (renderTier != ContinuumRenderTier.LITE) {
            gp.maskFilter = BlurMaskFilter((h * 0.032f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        }
        gp.shader = RadialGradient(
            activeX, cy + activeR * 0.82f, (w * 0.34f).coerceAtLeast(1f),
            intArrayOf(
                colors.orbFlareMid.copy(alpha = (0.62f * energyBoost).coerceAtMost(0.82f)).toArgb(),
                colors.orbOuterBloom.copy(alpha = 0.42f).toArgb(),
                colors.silkRoyalViolet.copy(alpha = 0.12f).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.36f, 0.70f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawPath(diag1, gp)

        // Crisp lavender-white silk liner matching orb hairline color & brightness
        gp.reset()
        gp.isAntiAlias = true
        gp.style = Paint.Style.STROKE
        gp.strokeCap = Paint.Cap.ROUND
        gp.strokeWidth = (h * 0.012f).coerceAtLeast(1.2f)
        gp.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        if (renderTier == ContinuumRenderTier.ULTRA) {
            gp.maskFilter = BlurMaskFilter((h * 0.004f).coerceAtLeast(0.6f), BlurMaskFilter.Blur.NORMAL)
        }
        gp.shader = RadialGradient(
            activeX, cy + activeR * 0.84f, (w * 0.38f).coerceAtLeast(1f),
            intArrayOf(
                colors.orbRimTopBot.copy(alpha = 0.95f).toArgb(),
                colors.silkElectricViolet.copy(alpha = 0.62f).toArgb(),
                colors.pocketRim.copy(alpha = 0.18f).toArgb()
            ),
            floatArrayOf(0f, 0.44f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawPath(diag1, gp)

        // Secondary taut electric-violet silk shimmer
        val diag2 = cache.pathD.apply {
            reset()
            moveTo(x0 + r0 * 0.20f, lerpFloat(y0Diag, cy, 0.16f))
            cubicTo(
                lerpFloat(x0, x1, 0.38f), lerpFloat(y0Diag, y1Diag, 0.28f),
                lerpFloat(x0, x1, 0.68f), lerpFloat(y0Diag, y1Diag, 0.90f),
                x1, y1Diag
            )
            cubicTo(
                lerpFloat(x1, x2, 0.32f), lerpFloat(y1Diag, y2Diag, 0.10f),
                lerpFloat(x1, x2, 0.62f), lerpFloat(y1Diag, y2Diag, 0.72f),
                x2 - r2 * 0.20f, lerpFloat(y2Diag, cy, 0.16f)
            )
        }
        gp.reset()
        gp.isAntiAlias = true
        gp.style = Paint.Style.STROKE
        gp.strokeCap = Paint.Cap.ROUND
        gp.strokeWidth = (h * 0.010f).coerceAtLeast(1.0f)
        gp.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        if (renderTier == ContinuumRenderTier.ULTRA) {
            gp.maskFilter = BlurMaskFilter((h * 0.006f).coerceAtLeast(0.8f), BlurMaskFilter.Blur.NORMAL)
        }
        gp.shader = RadialGradient(
            activeX, cy + activeR * 0.76f, (w * 0.32f).coerceAtLeast(1f),
            intArrayOf(
                colors.silkBrightLavender.copy(alpha = 0.50f).toArgb(),
                colors.orbOuterBloom.copy(alpha = 0.28f).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawPath(diag2, gp)
    }
}

private fun DrawScope.drawInactiveGlassPocket(
    cx: Float,
    cy: Float,
    radius: Float,
    alphaFactor: Float,
    isLeftSide: Boolean,
    isRightSide: Boolean,
    colors: ContinuumNavColors,
    renderTier: ContinuumRenderTier,
    cache: ContinuumDrawCache
) {
    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        val p = cache.paint

        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(
            cx, cy - radius * 0.12f, (radius * 1.05f).coerceAtLeast(1f),
            intArrayOf(
                colors.pocketInner.copy(alpha = 0.74f * alphaFactor).toArgb(),
                colors.silkDeepIndigo.copy(alpha = 0.32f * alphaFactor).toArgb(),
                colors.pocketEdge.copy(alpha = 0.28f * alphaFactor).toArgb()
            ),
            floatArrayOf(0f, 0.64f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawCircle(cx, cy, radius, p)

        val gradStartX = when {
            isLeftSide -> cx - radius * 0.85f
            isRightSide -> cx + radius * 0.85f
            else -> cx
        }
        val gradEndX = when {
            isLeftSide -> cx + radius * 0.85f
            isRightSide -> cx - radius * 0.85f
            else -> cx
        }

        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.strokeWidth = (radius * 0.042f).coerceAtLeast(1.2f)
        p.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        if (renderTier != ContinuumRenderTier.LITE) {
            p.maskFilter = BlurMaskFilter((radius * 0.028f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        }
        p.shader = LinearGradient(
            gradStartX, cy - radius, gradEndX, cy + radius,
            intArrayOf(
                colors.pocketRim.copy(alpha = 0.62f * alphaFactor).toArgb(),
                colors.silkElectricViolet.copy(alpha = 0.14f * alphaFactor).toArgb(),
                colors.pocketRim.copy(alpha = 0.28f * alphaFactor).toArgb()
            ),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawCircle(cx, cy, radius * 0.97f, p)
    }
}

private fun DrawScope.drawSelectionWaveArcs(
    cx: Float,
    cy: Float,
    radius: Float,
    waveProgress: Float,
    colors: ContinuumNavColors,
    cache: ContinuumDrawCache
) {
    val fade = (1f - waveProgress).coerceIn(0f, 1f)
    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        val p = cache.glowPaint
        for (ring in 0..1) {
            val offset = ring * 0.18f
            val localP = ((waveProgress - offset) / (1f - offset)).coerceIn(0f, 1f)
            if (localP <= 0.01f) continue
            val ringR = radius * (1.06f + 0.58f * localP)
            val alpha = fade * (1f - localP * 0.5f) * (if (ring == 0) 0.42f else 0.26f)
            p.reset()
            p.isAntiAlias = true
            p.style = Paint.Style.STROKE
            p.strokeCap = Paint.Cap.ROUND
            p.strokeWidth = (radius * 0.036f).coerceAtLeast(1.2f)
            p.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            p.color = colors.silkBrightLavender.copy(alpha = alpha.coerceIn(0f, 1f)).toArgb()
            cache.arcRect.set(cx - ringR, cy - ringR, cx + ringR, cy + ringR)
            nc.drawArc(cache.arcRect, -34f, 68f, false, p)
            nc.drawArc(cache.arcRect, 146f, 68f, false, p)
        }
    }
}

private fun DrawScope.drawFresnelActiveOrb(
    cx: Float,
    cy: Float,
    radius: Float,
    idlePulse: Float,
    touchAwaken: Float,
    colors: ContinuumNavColors,
    renderTier: ContinuumRenderTier,
    cache: ContinuumDrawCache
) {
    val bloomBoost = 1f + (idlePulse - 0.5f) * 0.10f + touchAwaken * 0.16f

    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        val p = cache.paint

        // 1. Broad, Multi-Stop Smooth Exponential Outer Violet Bloom (Melts Seamlessly Into Silk & Backdrop)
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        p.shader = RadialGradient(
            cx, cy + radius * 0.06f, (radius * 2.15f).coerceAtLeast(1f),
            intArrayOf(
                colors.orbOuterBloom.copy(alpha = (0.54f * bloomBoost).coerceAtMost(0.74f)).toArgb(),
                colors.silkRoyalViolet.copy(alpha = 0.30f).toArgb(),
                colors.orbOuterDeep.copy(alpha = 0.11f).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.36f, 0.68f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawCircle(cx, cy + radius * 0.06f, radius * 2.15f, p)

        // 2. Dark Translucent Cosmic-Indigo Orb Interior (No Hard Pastel Disc Sticker!)
        val orbClip = cache.clipPath.apply {
            reset()
            addCircle(cx, cy, radius, Path.Direction.CW)
        }
        nc.save()
        nc.clipPath(orbClip)

        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(
            cx, cy, radius.coerceAtLeast(1f),
            intArrayOf(
                Color(0xB81F164D).toArgb(),
                Color(0xB32D1E6E).toArgb(),
                colors.orbInnerEdge.copy(alpha = 0.52f).toArgb(),
                colors.orbInnerRimGlow.copy(alpha = 0.74f).toArgb()
            ),
            floatArrayOf(0f, 0.60f, 0.86f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawCircle(cx, cy, radius, p)

        // 3. Thin Inner-Edge Violet Rim Glow (Rapid Falloff Toward Dark Translucent Center)
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.strokeWidth = (radius * 0.11f).coerceAtLeast(3.5f)
        p.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        if (renderTier != ContinuumRenderTier.LITE) {
            p.maskFilter = BlurMaskFilter((radius * 0.10f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        }
        p.shader = LinearGradient(
            cx, cy - radius, cx, cy + radius,
            intArrayOf(
                colors.orbFlareMid.copy(alpha = 0.45f).toArgb(),
                colors.orbOuterBloom.copy(alpha = 0.28f).toArgb(),
                colors.orbFlareMid.copy(alpha = 0.62f).toArgb()
            ),
            floatArrayOf(0f, 0.50f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawCircle(cx, cy, radius * 0.95f, p)
        nc.restore()

        // 4. Smooth 110° Curved Bottom Arc Glow (Replaces Pin-Point White Dot Completely!)
        val outerArcR = radius * 1.01f
        cache.arcRect.set(cx - outerArcR, cy - outerArcR, cx + outerArcR, cy + outerArcR)
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = (radius * 0.22f).coerceAtLeast(6f)
        p.xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
        if (renderTier != ContinuumRenderTier.LITE) {
            p.maskFilter = BlurMaskFilter((radius * 0.18f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        }
        p.shader = LinearGradient(
            cx - radius * 0.90f, cy, cx + radius * 0.90f, cy,
            intArrayOf(
                Color.Transparent.toArgb(),
                colors.orbOuterBloom.copy(alpha = (0.54f * bloomBoost).coerceAtMost(0.75f)).toArgb(),
                colors.orbFlareMid.copy(alpha = (0.80f * bloomBoost).coerceAtMost(0.95f)).toArgb(),
                colors.orbOuterBloom.copy(alpha = (0.54f * bloomBoost).coerceAtMost(0.75f)).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.22f, 0.50f, 0.78f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawArc(cache.arcRect, 35f, 110f, false, p)

        // Tighter curved lavender-white bottom arc fusing diagonal silk cleanly into the rim
        cache.arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius)
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = (radius * 0.085f).coerceAtLeast(3f)
        p.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        if (renderTier != ContinuumRenderTier.LITE) {
            p.maskFilter = BlurMaskFilter((radius * 0.072f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        }
        p.shader = LinearGradient(
            cx - radius * 0.80f, cy, cx + radius * 0.80f, cy,
            intArrayOf(
                Color.Transparent.toArgb(),
                colors.orbFlareMid.copy(alpha = 0.65f).toArgb(),
                colors.orbFlareCore.copy(alpha = 0.92f).toArgb(),
                colors.orbFlareMid.copy(alpha = 0.65f).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.24f, 0.50f, 0.76f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawArc(cache.arcRect, 42f, 96f, false, p)

        // Subtle 12 o'clock curved top specular arc (balanced, zero fog)
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = (radius * 0.065f).coerceAtLeast(2.4f)
        p.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        if (renderTier != ContinuumRenderTier.LITE) {
            p.maskFilter = BlurMaskFilter((radius * 0.065f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        }
        p.shader = LinearGradient(
            cx - radius * 0.65f, cy, cx + radius * 0.65f, cy,
            intArrayOf(
                Color.Transparent.toArgb(),
                colors.orbFlareCore.copy(alpha = 0.68f).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.50f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawArc(cache.arcRect, 232f, 76f, false, p)

        // 5. Tight Vibrant Electric-Violet Neon Halo Hugging the Hairline
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.strokeWidth = (radius * 0.080f).coerceAtLeast(2.6f)
        p.xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
        if (renderTier != ContinuumRenderTier.LITE) {
            p.maskFilter = BlurMaskFilter((radius * 0.090f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        }
        p.shader = LinearGradient(
            cx, cy - radius, cx, cy + radius,
            intArrayOf(
                colors.orbFlareMid.copy(alpha = 0.76f).toArgb(),
                colors.orbOuterBloom.copy(alpha = 0.54f).toArgb(),
                colors.orbFlareCore.copy(alpha = 0.84f).toArgb()
            ),
            floatArrayOf(0f, 0.50f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawCircle(cx, cy, radius, p)

        // 6. RAZOR-THIN (1.4-1.6dp), ZERO-BLUR High-Contrast White-Lavender Hairline
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.strokeWidth = (radius * 0.024f).coerceAtLeast(1.3f)
        p.shader = LinearGradient(
            cx, cy - radius, cx, cy + radius,
            intArrayOf(
                colors.orbRimTopBot.toArgb(),
                colors.orbRimSide.copy(alpha = 0.90f).toArgb(),
                colors.orbRimTopBot.toArgb()
            ),
            floatArrayOf(0f, 0.50f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawCircle(cx, cy, radius, p)
    }
}

internal fun nodeWeight(activeProgress: Float, index: Int): Float {
    val raw = (1f - abs(activeProgress - index.toFloat())).coerceIn(0f, 1f)
    return raw * raw * (3f - 2f * raw)
}

private fun interpolateSlotX(slotX: FloatArray, progress: Float): Float {
    val clamped = progress.coerceIn(0f, 2f)
    return if (clamped <= 1f) {
        lerpFloat(slotX[0], slotX[1], clamped)
    } else {
        lerpFloat(slotX[1], slotX[2], clamped - 1f)
    }
}

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction.coerceIn(0f, 1f)
}
