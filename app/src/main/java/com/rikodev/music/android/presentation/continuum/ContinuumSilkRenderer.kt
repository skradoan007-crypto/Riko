package com.rikodev.music.android.presentation.continuum

import android.graphics.BlurMaskFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
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
    val clipPath = Path()
}

internal fun DrawScope.renderContinuumLayers1To4(
    metrics: ContinuumMetrics,
    activeProgress: Float,
    colors: ContinuumNavColors,
    renderTier: ContinuumRenderTier,
    cache: ContinuumDrawCache
) {
    if (metrics.width <= 2f || metrics.height <= 2f) return

    val w0 = nodeWeight(activeProgress, 0)
    val w1 = nodeWeight(activeProgress, 1)
    val w2 = nodeWeight(activeProgress, 2)

    val r0 = lerpFloat(metrics.inactiveRadius, metrics.activeRadius, w0)
    val r1 = lerpFloat(metrics.inactiveRadius, metrics.activeRadius, w1)
    val r2 = lerpFloat(metrics.inactiveRadius, metrics.activeRadius, w2)
    val radii = floatArrayOf(r0, r1, r2)

    val activeX = interpolateSlotX(metrics.slotX, activeProgress)
    val activeR = metrics.activeRadius

    // LAYER 1 & 2: Outer Glassmorphic Curved Base Container
    drawOuterGlassContainer(
        metrics = metrics,
        activeX = activeX,
        activeProgress = activeProgress,
        colors = colors,
        renderTier = renderTier,
        cache = cache
    )

    // LAYER 3: Closed Silk Cocoon Envelope + Crossed Volumetric Silk Ribbons + Caustic Streams
    drawSilkCocoonAndRibbons(
        metrics = metrics,
        radii = radii,
        activeX = activeX,
        activeR = activeR,
        colors = colors,
        renderTier = renderTier,
        cache = cache
    )

    // LAYER 4A: Soft Glass Dome Pockets for Inactive Slots
    val weights = floatArrayOf(w0, w1, w2)
    for (i in 0..2) {
        val inactiveAlpha = (1f - weights[i]).coerceIn(0f, 1f)
        if (inactiveAlpha > 0.02f) {
            drawInactiveGlassPocket(
                cx = metrics.slotX[i],
                cy = metrics.centerY,
                radius = radii[i],
                alphaFactor = inactiveAlpha,
                colors = colors,
                renderTier = renderTier,
                cache = cache
            )
        }
    }

    // LAYER 4B: Volumetric 3D Plasma Orb at Active Position
    drawVolumetricActiveOrb(
        cx = activeX,
        cy = metrics.centerY,
        radius = activeR,
        colors = colors,
        renderTier = renderTier,
        cache = cache
    )
}

private fun DrawScope.drawOuterGlassContainer(
    metrics: ContinuumMetrics,
    activeX: Float,
    activeProgress: Float,
    colors: ContinuumNavColors,
    renderTier: ContinuumRenderTier,
    cache: ContinuumDrawCache
) {
    val w = metrics.width
    val h = metrics.height
    val padX = w * 0.025f
    val capRadius = h * 0.43f
    val topBaseY = h * 0.145f
    val archLift = h * 0.085f
    val bottomY = h * 0.94f

    val archCenterX = lerpFloat(w * 0.25f, w * 0.75f, (activeProgress / 2f).coerceIn(0f, 1f))
    val archY = topBaseY - archLift

    val containerPath = cache.pathA.apply {
        reset()
        moveTo(padX, h * 0.54f)
        // Left rounded dome
        cubicTo(
            padX, topBaseY + h * 0.08f,
            padX + capRadius * 0.55f, topBaseY + h * 0.02f,
            padX + capRadius * 1.15f, topBaseY
        )
        // Smooth organic arch peaking near active node
        cubicTo(
            archCenterX - w * 0.18f, topBaseY,
            archCenterX - w * 0.11f, archY,
            archCenterX, archY
        )
        cubicTo(
            archCenterX + w * 0.11f, archY,
            archCenterX + w * 0.18f, topBaseY,
            w - padX - capRadius * 1.15f, topBaseY
        )
        // Right rounded dome
        cubicTo(
            w - padX - capRadius * 0.55f, topBaseY + h * 0.02f,
            w - padX, topBaseY + h * 0.08f,
            w - padX, h * 0.54f
        )
        // Lower rounded contour
        cubicTo(
            w - padX, bottomY - h * 0.04f,
            w - padX - capRadius * 0.55f, bottomY,
            w - padX - capRadius * 1.15f, bottomY
        )
        lineTo(padX + capRadius * 1.15f, bottomY)
        cubicTo(
            padX + capRadius * 0.55f, bottomY,
            padX, bottomY - h * 0.04f,
            padX, h * 0.54f
        )
        close()
    }

    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        val p = cache.paint

        // Soft ambient under-shadow
        if (renderTier != ContinuumRenderTier.LITE) {
            p.reset()
            p.isAntiAlias = true
            p.style = Paint.Style.FILL
            p.color = Color(0x7A05020F).toArgb()
            p.maskFilter = BlurMaskFilter((h * 0.12f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
            nc.drawPath(containerPath, p)
        }

        // Translucent frosted indigo glass fill
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(
            0f, archY, 0f, bottomY,
            intArrayOf(
                colors.glassTop.toArgb(),
                colors.glassMid.toArgb(),
                colors.glassBottom.toArgb()
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawPath(containerPath, p)

        // Subtle ambient purple reflection inside glass behind active orb
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(
            activeX, metrics.centerY, (w * 0.34f).coerceAtLeast(1f),
            intArrayOf(
                colors.orbOuterBloom.copy(alpha = 0.24f).toArgb(),
                colors.silkDeepViolet.copy(alpha = 0.09f).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.save()
        nc.clipPath(containerPath)
        nc.drawRect(0f, 0f, w, h, p)
        nc.restore()

        // Razor-thin seamless top specular edge highlight (fades cleanly to transparent on left/right)
        val topRimPath = cache.pathB.apply {
            reset()
            moveTo(padX + capRadius * 0.25f, topBaseY + h * 0.14f)
            cubicTo(
                padX + capRadius * 0.55f, topBaseY + h * 0.02f,
                padX + capRadius * 0.90f, topBaseY,
                padX + capRadius * 1.15f, topBaseY
            )
            cubicTo(
                archCenterX - w * 0.18f, topBaseY,
                archCenterX - w * 0.11f, archY,
                archCenterX, archY
            )
            cubicTo(
                archCenterX + w * 0.11f, archY,
                archCenterX + w * 0.18f, topBaseY,
                w - padX - capRadius * 1.15f, topBaseY
            )
            cubicTo(
                w - padX - capRadius * 0.90f, topBaseY,
                w - padX - capRadius * 0.55f, topBaseY + h * 0.02f,
                w - padX - capRadius * 0.25f, topBaseY + h * 0.14f
            )
        }

        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.strokeWidth = (h * 0.013f).coerceAtLeast(1.1f)
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
            floatArrayOf(0f, 0.18f, (activeX / w).coerceIn(0.25f, 0.75f), 0.82f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawPath(topRimPath, p)
    }
}

private fun DrawScope.drawSilkCocoonAndRibbons(
    metrics: ContinuumMetrics,
    radii: FloatArray,
    activeX: Float,
    activeR: Float,
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

    val waist01X = (x0 + x1) * 0.5f
    val waist12X = (x1 + x2) * 0.5f
    val waistHalfH = h * 0.105f

    val leftOuterX = x0 - r0 * 1.12f
    val rightOuterX = x2 + r2 * 1.12f

    // 1. Closed Outer Silk Cocoon Silhouette (wraps around Left & Right nodes, pinches at waists)
    val cocoonPath = cache.pathA.apply {
        reset()
        moveTo(leftOuterX, cy)
        // Left outer top quadrant
        cubicTo(leftOuterX, cy - r0 * 0.68f, x0 - r0 * 0.55f, cy - r0 * 1.06f, x0, cy - r0 * 1.04f)
        // Sweep from Left top -> Waist 01 top -> Center top
        cubicTo(x0 + r0 * 0.65f, cy - r0 * 1.02f, waist01X - (x1 - x0) * 0.16f, cy - waistHalfH, waist01X, cy - waistHalfH)
        cubicTo(waist01X + (x1 - x0) * 0.16f, cy - waistHalfH, x1 - r1 * 0.65f, cy - r1 * 1.03f, x1, cy - r1 * 1.04f)
        // Sweep from Center top -> Waist 12 top -> Right top
        cubicTo(x1 + r1 * 0.65f, cy - r1 * 1.03f, waist12X - (x2 - x1) * 0.16f, cy - waistHalfH, waist12X, cy - waistHalfH)
        cubicTo(waist12X + (x2 - x1) * 0.16f, cy - waistHalfH, x2 - r2 * 0.65f, cy - r2 * 1.02f, x2, cy - r2 * 1.04f)
        // Right outer dome
        cubicTo(x2 + r2 * 0.55f, cy - r2 * 1.06f, rightOuterX, cy - r2 * 0.68f, rightOuterX, cy)
        cubicTo(rightOuterX, cy + r2 * 0.68f, x2 + r2 * 0.55f, cy + r2 * 1.06f, x2, cy + r2 * 1.04f)
        // Sweep from Right bottom -> Waist 12 bottom -> Center bottom
        cubicTo(x2 - r2 * 0.65f, cy + r2 * 1.02f, waist12X + (x2 - x1) * 0.16f, cy + waistHalfH, waist12X, cy + waistHalfH)
        cubicTo(waist12X - (x2 - x1) * 0.16f, cy + waistHalfH, x1 + r1 * 0.65f, cy + r1 * 1.03f, x1, cy + r1 * 1.04f)
        // Sweep from Center bottom -> Waist 01 bottom -> Left bottom
        cubicTo(x1 - r1 * 0.65f, cy + r1 * 1.03f, waist01X + (x1 - x0) * 0.16f, cy + waistHalfH, waist01X, cy + waistHalfH)
        cubicTo(waist01X - (x1 - x0) * 0.16f, cy + waistHalfH, x0 + r0 * 0.65f, cy + r0 * 1.02f, x0, cy + r0 * 1.04f)
        // Left outer bottom quadrant
        cubicTo(x0 - r0 * 0.55f, cy + r0 * 1.06f, leftOuterX, cy + r0 * 0.68f, leftOuterX, cy)
        close()
    }

    // 2. Crossed Silk Band A (Filled closed ribbon: Top-Left -> Bottom-Center -> Top-Right)
    val bandThickness = h * 0.135f
    val ribbonBandA = cache.pathB.apply {
        reset()
        // Upper/outer edge of silk ribbon A
        moveTo(leftOuterX + r0 * 0.08f, cy - r0 * 0.25f)
        cubicTo(x0 - r0 * 0.45f, cy - r0 * 1.05f, x0 + r0 * 0.35f, cy - r0 * 1.04f, x0 + r0 * 0.72f, cy - r0 * 0.76f)
        cubicTo(waist01X + (x1 - x0) * 0.04f, cy + h * 0.06f, x1 - r1 * 0.58f, cy + r1 * 0.96f, x1, cy + r1 * 0.98f)
        cubicTo(x1 + r1 * 0.58f, cy + r1 * 0.96f, waist12X - (x2 - x1) * 0.04f, cy + h * 0.06f, x2 - r2 * 0.72f, cy - r2 * 0.76f)
        cubicTo(x2 - r2 * 0.35f, cy - r2 * 1.04f, x2 + r2 * 0.45f, cy - r2 * 1.05f, rightOuterX - r2 * 0.08f, cy - r2 * 0.25f)

        // Return inner edge of silk ribbon A (creates finite tapered silk band)
        cubicTo(x2 + r2 * 0.35f, cy - r2 * 0.72f, x2 - r2 * 0.15f, cy - r2 * 0.78f, x2 - r2 * 0.58f, cy - r2 * 0.50f)
        cubicTo(waist12X + (x2 - x1) * 0.02f, cy + h * 0.16f, x1 + r1 * 0.48f, cy + r1 * 0.80f + bandThickness * 0.28f, x1, cy + r1 * 0.82f + bandThickness * 0.35f)
        cubicTo(x1 - r1 * 0.48f, cy + r1 * 0.80f + bandThickness * 0.28f, waist01X - (x1 - x0) * 0.02f, cy + h * 0.16f, x0 + r0 * 0.58f, cy - r0 * 0.50f)
        cubicTo(x0 + r0 * 0.15f, cy - r0 * 0.78f, x0 - r0 * 0.35f, cy - r0 * 0.72f, leftOuterX + r0 * 0.08f, cy - r0 * 0.25f)
        close()
    }

    // 3. Crossed Silk Band B (Filled closed counter-ribbon: Bottom-Left -> Mid-Waist -> Bottom-Right)
    val ribbonBandB = cache.pathC.apply {
        reset()
        moveTo(leftOuterX + r0 * 0.10f, cy + r0 * 0.25f)
        cubicTo(x0 - r0 * 0.42f, cy + r0 * 1.02f, x0 + r0 * 0.35f, cy + r0 * 1.02f, x0 + r0 * 0.74f, cy + r0 * 0.72f)
        cubicTo(waist01X + (x1 - x0) * 0.04f, cy - h * 0.05f, x1 - r1 * 0.62f, cy - r1 * 0.86f, x1, cy - r1 * 0.88f)
        cubicTo(x1 + r1 * 0.62f, cy - r1 * 0.86f, waist12X - (x2 - x1) * 0.04f, cy - h * 0.05f, x2 - r2 * 0.74f, cy + r2 * 0.72f)
        cubicTo(x2 - r2 * 0.35f, cy + r2 * 1.02f, x2 + r2 * 0.42f, cy + r2 * 1.02f, rightOuterX - r2 * 0.10f, cy + r2 * 0.25f)

        // Return inner edge
        cubicTo(x2 + r2 * 0.32f, cy + r2 * 0.72f, x2 - r2 * 0.18f, cy + r2 * 0.76f, x2 - r2 * 0.58f, cy + r2 * 0.48f)
        cubicTo(waist12X + (x2 - x1) * 0.02f, cy - h * 0.14f, x1 + r1 * 0.48f, cy - r1 * 0.68f, x1, cy - r1 * 0.70f)
        cubicTo(x1 - r1 * 0.48f, cy - r1 * 0.68f, waist01X - (x1 - x0) * 0.02f, cy - h * 0.14f, x0 + r0 * 0.58f, cy + r0 * 0.48f)
        cubicTo(x0 + r0 * 0.18f, cy + r0 * 0.76f, x0 - r0 * 0.32f, cy + r0 * 0.72f, leftOuterX + r0 * 0.10f, cy + r0 * 0.25f)
        close()
    }

    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        val p = cache.paint

        // 3A. Soft Misty Silk Cocoon Base Fill
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(
            activeX, cy + activeR * 0.25f, (w * 0.46f).coerceAtLeast(1f),
            intArrayOf(
                colors.silkElectricPurple.copy(alpha = 0.40f).toArgb(),
                colors.silkDeepViolet.copy(alpha = 0.22f).toArgb(),
                colors.silkDeepViolet.copy(alpha = 0.09f).toArgb()
            ),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawPath(cocoonPath, p)

        // 3B. Soft Inner Edge Mist around the Cocoon (NO hard wireframe stroke!)
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.strokeWidth = (h * 0.032f).coerceAtLeast(2f)
        p.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        if (renderTier != ContinuumRenderTier.LITE) {
            p.maskFilter = BlurMaskFilter((h * 0.045f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        }
        p.shader = RadialGradient(
            activeX, cy, (w * 0.50f).coerceAtLeast(1f),
            intArrayOf(
                colors.silkNeonOrchid.copy(alpha = 0.55f).toArgb(),
                colors.silkNeonOrchid.copy(alpha = 0.24f).toArgb(),
                colors.silkNeonOrchid.copy(alpha = 0.12f).toArgb()
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawPath(cocoonPath, p)

        // 3C. Crossed Volumetric Silk Ribbon Band B (softer background counter-twist)
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        if (renderTier == ContinuumRenderTier.ULTRA) {
            p.maskFilter = BlurMaskFilter((h * 0.038f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        }
        p.shader = RadialGradient(
            activeX, cy, (w * 0.42f).coerceAtLeast(1f),
            intArrayOf(
                colors.silkElectricPurple.copy(alpha = 0.34f).toArgb(),
                colors.silkDeepViolet.copy(alpha = 0.16f).toArgb(),
                colors.silkDeepViolet.copy(alpha = 0.04f).toArgb()
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawPath(ribbonBandB, p)

        // 3D. Primary Luminous Silk Ribbon Band A (bright sweeping veil converging under active orb)
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        if (renderTier != ContinuumRenderTier.LITE) {
            p.maskFilter = BlurMaskFilter((h * 0.032f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        }
        p.shader = RadialGradient(
            activeX, cy + activeR * 0.55f, (w * 0.40f).coerceAtLeast(1f),
            intArrayOf(
                colors.silkHighlightWhite.copy(alpha = 0.62f).toArgb(),
                colors.silkNeonOrchid.copy(alpha = 0.42f).toArgb(),
                colors.silkDeepViolet.copy(alpha = 0.12f).toArgb()
            ),
            floatArrayOf(0f, 0.42f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawPath(ribbonBandA, p)

        // 3E. Signature Luminous Silk Caustic Filaments (the soft glowing light sweeps into the bottom of the orb)
        drawSilkCausticSweep(
            nc = nc,
            x0 = x0,
            x1 = x1,
            x2 = x2,
            cy = cy,
            r0 = r0,
            r1 = r1,
            r2 = r2,
            waist01X = waist01X,
            waist12X = waist12X,
            activeX = activeX,
            activeR = activeR,
            h = h,
            w = w,
            colors = colors,
            renderTier = renderTier,
            cache = cache
        )
    }
}

private fun drawSilkCausticSweep(
    nc: android.graphics.Canvas,
    x0: Float,
    x1: Float,
    x2: Float,
    cy: Float,
    r0: Float,
    r1: Float,
    r2: Float,
    waist01X: Float,
    waist12X: Float,
    activeX: Float,
    activeR: Float,
    h: Float,
    w: Float,
    colors: ContinuumNavColors,
    renderTier: ContinuumRenderTier,
    cache: ContinuumDrawCache
) {
    val sweepPath = cache.pathC.apply {
        reset()
        moveTo(x0 - r0 * 0.42f, cy - r0 * 0.94f)
        cubicTo(
            x0 + r0 * 0.55f, cy - r0 * 0.98f,
            waist01X + (x1 - x0) * 0.04f, cy + h * 0.10f,
            x1, cy + r1 * 0.97f
        )
        cubicTo(
            waist12X - (x2 - x1) * 0.04f, cy + h * 0.10f,
            x2 - r2 * 0.55f, cy - r2 * 0.98f,
            x2 + r2 * 0.42f, cy - r2 * 0.94f
        )
    }

    val p = cache.glowPaint

    // Pass 1: Wide soft additive silk bloom
    p.reset()
    p.isAntiAlias = true
    p.style = Paint.Style.STROKE
    p.strokeCap = Paint.Cap.ROUND
    p.strokeWidth = (h * 0.075f).coerceAtLeast(4f)
    p.xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    if (renderTier != ContinuumRenderTier.LITE) {
        p.maskFilter = BlurMaskFilter((h * 0.085f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
    }
    p.shader = RadialGradient(
        activeX, cy + activeR * 0.65f, (w * 0.34f).coerceAtLeast(1f),
        intArrayOf(
            colors.silkNeonOrchid.copy(alpha = 0.52f).toArgb(),
            colors.silkElectricPurple.copy(alpha = 0.24f).toArgb(),
            Color.Transparent.toArgb()
        ),
        floatArrayOf(0f, 0.52f, 1f),
        Shader.TileMode.CLAMP
    )
    nc.drawPath(sweepPath, p)

    // Pass 2: Focused luminous silk core filament (softened, fades seamlessly into the outer pockets)
    p.reset()
    p.isAntiAlias = true
    p.style = Paint.Style.STROKE
    p.strokeCap = Paint.Cap.ROUND
    p.strokeWidth = (h * 0.020f).coerceAtLeast(1.4f)
    p.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
    if (renderTier == ContinuumRenderTier.ULTRA) {
        p.maskFilter = BlurMaskFilter((h * 0.022f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
    }
    p.shader = RadialGradient(
        activeX, cy + activeR * 0.72f, (w * 0.32f).coerceAtLeast(1f),
        intArrayOf(
            colors.silkHighlightWhite.copy(alpha = 0.85f).toArgb(),
            colors.silkNeonOrchid.copy(alpha = 0.36f).toArgb(),
            Color.Transparent.toArgb()
        ),
        floatArrayOf(0f, 0.45f, 1f),
        Shader.TileMode.CLAMP
    )
    nc.drawPath(sweepPath, p)
}

private fun DrawScope.drawInactiveGlassPocket(
    cx: Float,
    cy: Float,
    radius: Float,
    alphaFactor: Float,
    colors: ContinuumNavColors,
    renderTier: ContinuumRenderTier,
    cache: ContinuumDrawCache
) {
    val a = alphaFactor.coerceIn(0f, 1f)
    if (a <= 0.01f || radius <= 1f) return
    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        val p = cache.paint

        // Soft off-center frosted dome fill
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(
            cx - radius * 0.22f,
            cy - radius * 0.28f,
            (radius * 1.25f).coerceAtLeast(1f),
            intArrayOf(
                colors.silkNeonOrchid.copy(alpha = 0.20f * a).toArgb(),
                colors.glassTop.copy(alpha = 0.34f * a).toArgb(),
                colors.glassBottom.copy(alpha = 0.12f * a).toArgb()
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawCircle(cx, cy, radius, p)

        // Subtle directional glass rim
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.strokeWidth = (radius * 0.045f).coerceAtLeast(1.1f)
        p.shader = LinearGradient(
            cx, cy - radius, cx, cy + radius,
            intArrayOf(
                colors.orbRimMid.copy(alpha = 0.34f * a).toArgb(),
                colors.silkDeepViolet.copy(alpha = 0.10f * a).toArgb(),
                colors.orbRimMid.copy(alpha = 0.18f * a).toArgb()
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawCircle(cx, cy, radius, p)
    }
}

private fun DrawScope.drawVolumetricActiveOrb(
    cx: Float,
    cy: Float,
    radius: Float,
    colors: ContinuumNavColors,
    renderTier: ContinuumRenderTier,
    cache: ContinuumDrawCache
) {
    if (radius <= 1f) return
    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        val p = cache.paint

        // 1. Wide outer volumetric Screen aura
        val auraR = (radius * 1.75f).coerceAtLeast(1f)
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        p.shader = RadialGradient(
            cx, cy, auraR,
            intArrayOf(
                colors.orbOuterBloom.copy(alpha = 0.52f).toArgb(),
                colors.silkDeepViolet.copy(alpha = 0.22f).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawCircle(cx, cy, auraR, p)

        // 2. Clipped 3D volumetric plasma interior with bottom magenta hotspot + top lavender reflection
        val clipOrb = cache.clipPath.apply {
            reset()
            addCircle(cx, cy, radius, Path.Direction.CW)
        }
        nc.save()
        nc.clipPath(clipOrb)

        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.color = colors.orbInnerCore.copy(alpha = 0.88f).toArgb()
        nc.drawCircle(cx, cy, radius, p)

        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(
            cx, cy + radius * 0.32f, (radius * 1.35f).coerceAtLeast(1f),
            intArrayOf(
                colors.orbBottomHotspot.copy(alpha = 0.85f).toArgb(),
                colors.silkElectricPurple.copy(alpha = 0.62f).toArgb(),
                colors.orbInnerCore.copy(alpha = 0.25f).toArgb()
            ),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawCircle(cx, cy, radius, p)

        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        p.shader = RadialGradient(
            cx, cy - radius * 0.72f, (radius * 0.80f).coerceAtLeast(1f),
            intArrayOf(
                colors.orbRimBright.copy(alpha = 0.34f).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawCircle(cx, cy, radius, p)
        nc.restore()

        // 3. Blurred neon halo ring underneath crisp vertical-gradient rim
        if (renderTier != ContinuumRenderTier.LITE) {
            val gp = cache.glowPaint
            gp.reset()
            gp.isAntiAlias = true
            gp.style = Paint.Style.STROKE
            gp.strokeWidth = (radius * 0.16f).coerceAtLeast(4f)
            gp.color = colors.silkNeonOrchid.copy(alpha = 0.60f).toArgb()
            gp.maskFilter = BlurMaskFilter((radius * 0.28f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
            nc.drawCircle(cx, cy, radius, gp)
        }

        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.strokeWidth = (radius * 0.058f).coerceAtLeast(1.8f)
        p.shader = LinearGradient(
            cx, cy - radius, cx, cy + radius,
            intArrayOf(
                colors.orbRimBright.toArgb(),
                colors.orbRimMid.copy(alpha = 0.78f).toArgb(),
                colors.orbRimBright.toArgb()
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawCircle(cx, cy, radius, p)
    }
}

internal fun nodeWeight(activeProgress: Float, index: Int): Float =
    (1f - abs(activeProgress - index.toFloat())).coerceIn(0f, 1f)

internal fun lerpFloat(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction.coerceIn(0f, 1f)

internal fun interpolateSlotX(slotX: FloatArray, activeProgress: Float): Float {
    val p = activeProgress.coerceIn(0f, 2f)
    return if (p <= 1f) {
        lerpFloat(slotX[0], slotX[1], p)
    } else {
        lerpFloat(slotX[1], slotX[2], p - 1f)
    }
}
