package com.rikodev.music.android.ui.navigation

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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

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

    // LAYER 1 & 2: Symmetrical Curved Frosted Glass Visor
    drawSymmetricalGlassVisor(
        metrics = metrics,
        activeX = activeX,
        colors = colors,
        renderTier = renderTier,
        cache = cache
    )

    // LAYER 3: Dynamic Parametric Silk Cocoon + 14 Twisted Gossamer Silk Strands
    drawGossamerSilkVeil(
        metrics = metrics,
        radii = radii,
        activeX = activeX,
        activeR = activeR,
        colors = colors,
        renderTier = renderTier,
        cache = cache
    )

    // LAYER 4A: Crisp Translucent Glass Dome Pockets for Inactive Nodes
    val weights = floatArrayOf(w0, w1, w2)
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

    // LAYER 4B: Volumetric Fresnel Cosmic-Indigo Plasma Orb at Active Slot
    drawFresnelActiveOrb(
        cx = activeX,
        cy = metrics.centerY,
        radius = activeR,
        colors = colors,
        renderTier = renderTier,
        cache = cache
    )
}

private fun DrawScope.drawSymmetricalGlassVisor(
    metrics: ContinuumMetrics,
    activeX: Float,
    colors: ContinuumNavColors,
    renderTier: ContinuumRenderTier,
    cache: ContinuumDrawCache
) {
    val w = metrics.width
    val h = metrics.height
    val padX = w * 0.012f
    val topCenterY = h * 0.205f
    val topEdgeY = h * 0.365f
    val bottomY = h * 0.945f
    val sideCornerR = h * 0.34f

    val visorPath = cache.pathA.apply {
        reset()
        moveTo(padX, topEdgeY + sideCornerR * 0.45f)
        cubicTo(
            padX, topEdgeY,
            w * 0.14f, topCenterY + h * 0.02f,
            w * 0.50f, topCenterY
        )
        cubicTo(
            w * 0.86f, topCenterY + h * 0.02f,
            w - padX, topEdgeY,
            w - padX, topEdgeY + sideCornerR * 0.45f
        )
        cubicTo(
            w - padX, bottomY - h * 0.03f,
            w - padX - sideCornerR * 0.65f, bottomY,
            w - padX - sideCornerR * 1.25f, bottomY
        )
        lineTo(padX + sideCornerR * 1.25f, bottomY)
        cubicTo(
            padX + sideCornerR * 0.65f, bottomY,
            padX, bottomY - h * 0.03f,
            padX, topEdgeY + sideCornerR * 0.45f
        )
        close()
    }

    val topRimPath = cache.pathB.apply {
        reset()
        moveTo(padX + w * 0.01f, topEdgeY + sideCornerR * 0.30f)
        cubicTo(
            padX + w * 0.02f, topEdgeY,
            w * 0.14f, topCenterY + h * 0.02f,
            w * 0.50f, topCenterY
        )
        cubicTo(
            w * 0.86f, topCenterY + h * 0.02f,
            w - padX - w * 0.02f, topEdgeY,
            w - padX - w * 0.01f, topEdgeY + sideCornerR * 0.30f
        )
    }

    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        val p = cache.paint

        if (renderTier != ContinuumRenderTier.LITE) {
            p.reset()
            p.isAntiAlias = true
            p.style = Paint.Style.FILL
            p.color = Color(0x88070514).toArgb()
            p.maskFilter = BlurMaskFilter((h * 0.11f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
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
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawPath(visorPath, p)

        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(
            activeX, metrics.centerY + h * 0.05f, (w * 0.36f).coerceAtLeast(1f),
            intArrayOf(
                colors.orbOuterBloom.copy(alpha = 0.22f).toArgb(),
                colors.silkDeepIndigo.copy(alpha = 0.08f).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.save()
        nc.clipPath(visorPath)
        nc.drawRect(0f, 0f, w, h, p)
        nc.restore()

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
            floatArrayOf(0f, 0.16f, (activeX / w).coerceIn(0.22f, 0.78f), 0.84f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawPath(topRimPath, p)
    }
}

private fun DrawScope.drawGossamerSilkVeil(
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

    val waistHalfH = h * 0.095f
    val leftEdge = x0 - r0 * 1.14f
    val rightEdge = x2 + r2 * 1.14f
    val steps = 76
    val dx = (rightEdge - leftEdge) / steps

    val cocoonPath = cache.pathA.apply {
        reset()
        moveTo(leftEdge, cy)
        for (s in 0..steps) {
            val x = leftEdge + s * dx
            val env = evalSilkEnvelope(x, x0, x1, x2, r0, r1, r2, waistHalfH)
            lineTo(x, cy - env)
        }
        for (s in steps downTo 0) {
            val x = leftEdge + s * dx
            val env = evalSilkEnvelope(x, x0, x1, x2, r0, r1, r2, waistHalfH)
            lineTo(x, cy + env)
        }
        close()
    }

    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        val p = cache.paint

        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(
            activeX, cy + activeR * 0.22f, (w * 0.48f).coerceAtLeast(1f),
            intArrayOf(
                colors.silkElectricIndigo.copy(alpha = 0.36f).toArgb(),
                colors.silkDeepIndigo.copy(alpha = 0.24f).toArgb(),
                colors.silkDeepIndigo.copy(alpha = 0.09f).toArgb()
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawPath(cocoonPath, p)

        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.strokeWidth = (h * 0.026f).coerceAtLeast(1.8f)
        p.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        if (renderTier != ContinuumRenderTier.LITE) {
            p.maskFilter = BlurMaskFilter((h * 0.036f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        }
        p.shader = RadialGradient(
            activeX, cy + activeR * 0.15f, (w * 0.52f).coerceAtLeast(1f),
            intArrayOf(
                colors.silkPeriwinkle.copy(alpha = 0.52f).toArgb(),
                colors.pocketRimHighlight.copy(alpha = 0.26f).toArgb(),
                colors.pocketRimHighlight.copy(alpha = 0.14f).toArgb()
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawPath(cocoonPath, p)

        val strandCount = when (renderTier) {
            ContinuumRenderTier.ULTRA -> 14
            ContinuumRenderTier.BALANCED -> 10
            ContinuumRenderTier.LITE -> 8
        }

        val strandPath = cache.pathB
        val strandShader = RadialGradient(
            activeX, cy + activeR * 0.38f, (w * 0.44f).coerceAtLeast(1f),
            intArrayOf(
                colors.silkIceLavender.copy(alpha = 0.62f).toArgb(),
                colors.silkPeriwinkle.copy(alpha = 0.34f).toArgb(),
                colors.silkDeepIndigo.copy(alpha = 0.12f).toArgb()
            ),
            floatArrayOf(0f, 0.42f, 1f),
            Shader.TileMode.CLAMP
        )

        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        p.shader = strandShader

        for (i in 0 until strandCount) {
            val u = -0.96f + (1.92f * i / (strandCount - 1).coerceAtLeast(1))
            val edgeWeight = abs(u)
            val alphaScale = (0.24f + 0.56f * edgeWeight * edgeWeight)

            strandPath.reset()
            var started = false
            for (s in 0..steps) {
                val x = leftEdge + s * dx
                val env = evalSilkEnvelope(x, x0, x1, x2, r0, r1, r2, waistHalfH)
                val twist = (PI * (smoothStep(x0, x1, x) + smoothStep(x1, x2, x))).toFloat()
                val phaseOffset = u * 0.24f
                val twistedY = u * cos(twist + phaseOffset) + 0.22f * sin(twist) * (1f - 0.45f * u * u)
                val y = cy + env * twistedY.coerceIn(-1.02f, 1.02f)

                if (!started) {
                    strandPath.moveTo(x, y)
                    started = true
                } else {
                    strandPath.lineTo(x, y)
                }
            }

            p.strokeWidth = (h * (0.011f + 0.008f * edgeWeight)).coerceAtLeast(1.0f)
            p.alpha = (255f * alphaScale).toInt().coerceIn(18, 220)
            nc.drawPath(strandPath, p)
        }

        drawDynamicCausticSelvedges(
            nc = nc,
            leftEdge = leftEdge,
            rightEdge = rightEdge,
            steps = steps,
            dx = dx,
            x0 = x0,
            x1 = x1,
            x2 = x2,
            r0 = r0,
            r1 = r1,
            r2 = r2,
            cy = cy,
            waistHalfH = waistHalfH,
            activeX = activeX,
            activeR = activeR,
            w = w,
            h = h,
            colors = colors,
            renderTier = renderTier,
            cache = cache
        )
    }
}

private fun drawDynamicCausticSelvedges(
    nc: android.graphics.Canvas,
    leftEdge: Float,
    rightEdge: Float,
    steps: Int,
    dx: Float,
    x0: Float,
    x1: Float,
    x2: Float,
    r0: Float,
    r1: Float,
    r2: Float,
    cy: Float,
    waistHalfH: Float,
    activeX: Float,
    activeR: Float,
    w: Float,
    h: Float,
    colors: ContinuumNavColors,
    renderTier: ContinuumRenderTier,
    cache: ContinuumDrawCache
) {
    val selvedgePath = cache.pathC
    val gp = cache.glowPaint

    val edgeSignValues = floatArrayOf(-0.95f, 0.95f)
    for (u in edgeSignValues) {
        selvedgePath.reset()
        for (s in 0..steps) {
            val x = leftEdge + s * dx
            val env = evalSilkEnvelope(x, x0, x1, x2, r0, r1, r2, waistHalfH)
            val twist = (PI * (smoothStep(x0, x1, x) + smoothStep(x1, x2, x))).toFloat()
            val y = cy + env * (u * cos(twist) + 0.16f * sin(twist))
            if (s == 0) selvedgePath.moveTo(x, y) else selvedgePath.lineTo(x, y)
        }

        gp.reset()
        gp.isAntiAlias = true
        gp.style = Paint.Style.STROKE
        gp.strokeCap = Paint.Cap.ROUND
        gp.strokeWidth = (h * 0.055f).coerceAtLeast(3.2f)
        gp.xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
        if (renderTier != ContinuumRenderTier.LITE) {
            gp.maskFilter = BlurMaskFilter((h * 0.068f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        }
        gp.shader = RadialGradient(
            activeX, cy + activeR * 0.55f, (w * 0.30f).coerceAtLeast(1f),
            intArrayOf(
                colors.silkPeriwinkle.copy(alpha = 0.56f).toArgb(),
                colors.silkElectricIndigo.copy(alpha = 0.24f).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawPath(selvedgePath, gp)

        gp.reset()
        gp.isAntiAlias = true
        gp.style = Paint.Style.STROKE
        gp.strokeCap = Paint.Cap.ROUND
        gp.strokeWidth = (h * 0.017f).coerceAtLeast(1.2f)
        gp.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        if (renderTier == ContinuumRenderTier.ULTRA) {
            gp.maskFilter = BlurMaskFilter((h * 0.016f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        }
        gp.shader = RadialGradient(
            activeX, cy + activeR * 0.62f, (w * 0.27f).coerceAtLeast(1f),
            intArrayOf(
                colors.silkIceLavender.copy(alpha = 0.82f).toArgb(),
                colors.silkPeriwinkle.copy(alpha = 0.32f).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawPath(selvedgePath, gp)
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
            cx, cy - radius * 0.15f, (radius * 1.05f).coerceAtLeast(1f),
            intArrayOf(
                Color(0x40241F47).copy(alpha = 0.42f * alphaFactor).toArgb(),
                colors.silkDeepIndigo.copy(alpha = 0.22f * alphaFactor).toArgb(),
                colors.pocketRimHighlight.copy(alpha = 0.18f * alphaFactor).toArgb()
            ),
            floatArrayOf(0f, 0.68f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawCircle(cx, cy, radius, p)

        val gradStartX = when {
            isLeftSide -> cx - radius
            isRightSide -> cx + radius
            else -> cx
        }
        val gradEndX = when {
            isLeftSide -> cx + radius
            isRightSide -> cx - radius
            else -> cx
        }

        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.strokeWidth = (radius * 0.052f).coerceAtLeast(1.1f)
        p.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        if (renderTier != ContinuumRenderTier.LITE) {
            p.maskFilter = BlurMaskFilter((radius * 0.06f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        }
        p.shader = LinearGradient(
            gradStartX, cy - radius, gradEndX, cy + radius,
            intArrayOf(
                colors.pocketRimHighlight.copy(alpha = 0.50f * alphaFactor).toArgb(),
                colors.silkPeriwinkle.copy(alpha = 0.14f * alphaFactor).toArgb(),
                colors.pocketRimHighlight.copy(alpha = 0.24f * alphaFactor).toArgb()
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawCircle(cx, cy, radius * 0.97f, p)
    }
}

private fun DrawScope.drawFresnelActiveOrb(
    cx: Float,
    cy: Float,
    radius: Float,
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
        p.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        p.shader = RadialGradient(
            cx, cy + radius * 0.12f, (radius * 1.92f).coerceAtLeast(1f),
            intArrayOf(
                colors.orbOuterBloom.copy(alpha = 0.58f).toArgb(),
                colors.silkDeepIndigo.copy(alpha = 0.26f).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.50f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawCircle(cx, cy + radius * 0.10f, radius * 1.92f, p)

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
            cx, cy - radius * 0.06f, radius.coerceAtLeast(1f),
            intArrayOf(
                colors.orbInnerCenter.copy(alpha = 0.88f).toArgb(),
                colors.orbInnerCenter.copy(alpha = 0.86f).toArgb(),
                colors.orbInnerEdge.copy(alpha = 0.92f).toArgb(),
                colors.orbBottomCaustic.copy(alpha = 0.95f).toArgb()
            ),
            floatArrayOf(0f, 0.52f, 0.84f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawCircle(cx, cy, radius, p)

        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        p.shader = RadialGradient(
            cx, cy + radius * 1.05f, (radius * 0.82f).coerceAtLeast(1f),
            intArrayOf(
                colors.orbRimBright.copy(alpha = 0.88f).toArgb(),
                colors.orbInnerEdge.copy(alpha = 0.52f).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.42f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawCircle(cx, cy, radius, p)

        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        p.shader = RadialGradient(
            cx, cy - radius * 1.05f, (radius * 0.68f).coerceAtLeast(1f),
            intArrayOf(
                colors.orbRimBright.copy(alpha = 0.55f).toArgb(),
                colors.silkPeriwinkle.copy(alpha = 0.16f).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawCircle(cx, cy, radius, p)
        nc.restore()

        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.strokeWidth = (radius * 0.16f).coerceAtLeast(3.5f)
        p.xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
        if (renderTier != ContinuumRenderTier.LITE) {
            p.maskFilter = BlurMaskFilter((radius * 0.22f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        }
        p.shader = LinearGradient(
            cx, cy - radius, cx, cy + radius,
            intArrayOf(
                colors.orbRimBright.copy(alpha = 0.78f).toArgb(),
                colors.orbRimSide.copy(alpha = 0.45f).toArgb(),
                colors.orbRimBright.copy(alpha = 0.92f).toArgb()
            ),
            floatArrayOf(0f, 0.50f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawCircle(cx, cy, radius, p)

        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.strokeWidth = (radius * 0.048f).coerceAtLeast(1.6f)
        p.shader = LinearGradient(
            cx, cy - radius, cx, cy + radius,
            intArrayOf(
                colors.orbRimBright.toArgb(),
                colors.orbRimSide.copy(alpha = 0.78f).toArgb(),
                colors.orbRimBright.toArgb()
            ),
            floatArrayOf(0f, 0.50f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawCircle(cx, cy, radius, p)
    }
}

private fun evalSilkEnvelope(
    x: Float,
    x0: Float,
    x1: Float,
    x2: Float,
    r0: Float,
    r1: Float,
    r2: Float,
    waistH: Float
): Float {
    val d0 = (x - x0) / (r0 * 1.15f)
    val d1 = (x - x1) / (r1 * 1.15f)
    val d2 = (x - x2) / (r2 * 1.15f)
    val w0 = exp(-(d0 * d0))
    val w1 = exp(-(d1 * d1))
    val w2 = exp(-(d2 * d2))

    val base = waistH + (r0 - waistH) * w0 + (r1 - waistH) * w1 + (r2 - waistH) * w2
    val leftCap = smoothStep(x0 - r0 * 1.14f, x0 - r0 * 0.55f, x)
    val rightCap = 1f - smoothStep(x2 + r2 * 0.55f, x2 + r2 * 1.14f, x)
    val capMask = sqrt((leftCap * rightCap).coerceIn(0f, 1f))
    return base * capMask
}

private fun smoothStep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0).coerceAtLeast(0.0001f)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
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
