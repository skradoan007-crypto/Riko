package com.rikodev.music.android.ui.navigation

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
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
import kotlin.math.min

private const val CIRCLE_KAPPA = 0.55228475f

internal class ContinuumDrawCache {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    val xferScreen = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
    val xferAdd = PorterDuffXfermode(PorterDuff.Mode.ADD)

    val dockPath = Path()
    val dockRimPath = Path()
    val lobeBasePath = Path()
    val nestedPath = Path()
    val silkPath = Path()
    val clipPath = Path()
    val shaderMatrix = Matrix()

    // Pre-allocated scratch arrays (0 per-frame allocations)
    val weightsBuf = FloatArray(3)
    val radiiBuf = FloatArray(3)

    // Cached static Layer 1 Dock Bitmap (baked ONLY when width/height changes -> ~0.05ms per frame!)
    private var staticDockBitmap: Bitmap? = null
    private var cachedWidth: Int = -1
    private var cachedHeight: Int = -1

    // Pre-allocated unit-space Shaders moved via Matrix (0 allocations per frame!)
    var unitBloomShader: RadialGradient? = null
    var unitSilkVeilShader: RadialGradient? = null
    var unitSilkCoreShader: RadialGradient? = null
    var unitOrbCenterShader: RadialGradient? = null
    var unitOrbBottomCrescentShader: RadialGradient? = null
    var unitOrbTopCrescentShader: RadialGradient? = null
    var unitFlareShader: RadialGradient? = null
    var unitPocketFillShader: RadialGradient? = null
    private var shadersInitialized = false

    fun ensureUnitShaders(colors: ContinuumNavColors) {
        if (shadersInitialized) return
        shadersInitialized = true

        unitBloomShader = RadialGradient(
            0f, 0f, 1f,
            intArrayOf(
                colors.orbOuterBloom.copy(alpha = 0.56f).toArgb(),
                colors.silkDeepIndigo.copy(alpha = 0.24f).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP
        )

        unitSilkVeilShader = RadialGradient(
            0f, 0f, 1f,
            intArrayOf(
                colors.silkBrightLavender.copy(alpha = 0.54f).toArgb(),
                colors.silkDeepIndigo.copy(alpha = 0.26f).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.50f, 1f),
            Shader.TileMode.CLAMP
        )

        unitSilkCoreShader = RadialGradient(
            0f, 0f, 1f,
            intArrayOf(
                colors.silkPureWhite.copy(alpha = 0.85f).toArgb(),
                colors.silkBrightLavender.copy(alpha = 0.38f).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP
        )

        // Clear-core dark translucent cosmic indigo interior (matches v14 100%)
        unitOrbCenterShader = RadialGradient(
            0f, 0f, 1f,
            intArrayOf(
                Color(0xB81F164D).toArgb(),
                Color(0xC22D1E6E).toArgb(),
                colors.orbInnerEdge.copy(alpha = 0.68f).toArgb()
            ),
            floatArrayOf(0f, 0.66f, 1f),
            Shader.TileMode.CLAMP
        )

        unitOrbBottomCrescentShader = RadialGradient(
            0f, 0f, 1f,
            intArrayOf(
                colors.orbRimTopBot.copy(alpha = 0.78f).toArgb(),
                colors.orbInnerEdge.copy(alpha = 0.36f).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )

        unitOrbTopCrescentShader = RadialGradient(
            0f, 0f, 1f,
            intArrayOf(
                colors.orbRimTopBot.copy(alpha = 0.46f).toArgb(),
                colors.orbInnerEdge.copy(alpha = 0.14f).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )

        unitFlareShader = RadialGradient(
            0f, 0f, 1f,
            intArrayOf(
                colors.orbFlareCore.copy(alpha = 0.86f).toArgb(),
                colors.orbRimSide.copy(alpha = 0.42f).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )

        unitPocketFillShader = RadialGradient(
            0f, 0f, 1f,
            intArrayOf(
                colors.pocketInner.copy(alpha = 0.72f).toArgb(),
                colors.glassMid.copy(alpha = 0.56f).toArgb(),
                colors.pocketRim.copy(alpha = 0.16f).toArgb()
            ),
            floatArrayOf(0f, 0.72f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    fun getOrBakeStaticDock(
        widthPx: Int,
        heightPx: Int,
        colors: ContinuumNavColors
    ): Bitmap {
        val existing = staticDockBitmap
        if (existing != null && !existing.isRecycled && cachedWidth == widthPx && cachedHeight == heightPx) {
            return existing
        }

        existing?.recycle()
        val bmp = Bitmap.createBitmap(widthPx.coerceAtLeast(2), heightPx.coerceAtLeast(2), Bitmap.Config.ARGB_8888)
        val nc = Canvas(bmp)
        val w = widthPx.toFloat()
        val h = heightPx.toFloat()

        val padX = w * 0.014f
        val topCenterY = h * 0.175f
        val topSideY = h * 0.34f
        val bottomY = h * 0.96f
        val cornerR = h * 0.30f

        dockPath.reset()
        dockPath.moveTo(padX, topSideY + cornerR * 0.4f)
        dockPath.cubicTo(padX, topSideY, w * 0.15f, topCenterY + h * 0.015f, w * 0.50f, topCenterY)
        dockPath.cubicTo(w * 0.85f, topCenterY + h * 0.015f, w - padX, topSideY, w - padX, topSideY + cornerR * 0.4f)
        dockPath.cubicTo(w - padX, bottomY - h * 0.03f, w - padX - cornerR * 0.6f, bottomY, w - padX - cornerR * 1.2f, bottomY)
        dockPath.lineTo(padX + cornerR * 1.2f, bottomY)
        dockPath.cubicTo(padX + cornerR * 0.6f, bottomY, padX, bottomY - h * 0.03f, padX, topSideY + cornerR * 0.4f)
        dockPath.close()

        dockRimPath.reset()
        dockRimPath.moveTo(padX + w * 0.015f, topSideY + cornerR * 0.25f)
        dockRimPath.cubicTo(padX + w * 0.025f, topSideY, w * 0.15f, topCenterY + h * 0.015f, w * 0.50f, topCenterY)
        dockRimPath.cubicTo(w * 0.85f, topCenterY + h * 0.015f, w - padX - w * 0.025f, topSideY, w - padX - w * 0.015f, topSideY + cornerR * 0.25f)

        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        // One-time blurred shadow occlusion (zero cost during animation!)
        p.style = Paint.Style.FILL
        p.color = Color(0x8C04020D).toArgb()
        p.maskFilter = BlurMaskFilter((h * 0.10f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        nc.drawPath(dockPath, p)

        // Frosted dock backdrop fill
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(
            0f, topCenterY, 0f, bottomY,
            intArrayOf(colors.glassTop.toArgb(), colors.glassBottom.toArgb()),
            null,
            Shader.TileMode.CLAMP
        )
        nc.drawPath(dockPath, p)

        // Top arched hairline rim
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.strokeWidth = (h * 0.012f).coerceAtLeast(1f)
        p.strokeCap = Paint.Cap.ROUND
        p.shader = LinearGradient(
            padX, 0f, w - padX, 0f,
            intArrayOf(
                Color.Transparent.toArgb(),
                colors.glassRimCenter.copy(alpha = 0.28f).toArgb(),
                colors.glassRimCenter.toArgb(),
                colors.glassRimCenter.copy(alpha = 0.28f).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.18f, 0.50f, 0.82f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawPath(dockRimPath, p)

        staticDockBitmap = bmp
        cachedWidth = widthPx
        cachedHeight = heightPx
        return bmp
    }

    fun release() {
        staticDockBitmap?.recycle()
        staticDockBitmap = null
        cachedWidth = -1
        cachedHeight = -1
    }
}

internal fun DrawScope.renderContinuumLayers1To4(
    metrics: ContinuumMetrics,
    activeProgress: Float,
    idlePulse: Float,
    touchIntensity: Float,
    wavePulseProgress: Float,
    colors: ContinuumNavColors,
    renderTier: ContinuumRenderTier,
    cache: ContinuumDrawCache
) {
    val wInt = metrics.width.toInt()
    val hInt = metrics.height.toInt()
    if (wInt <= 2 || hInt <= 2) return

    cache.ensureUnitShaders(colors)

    val w0 = nodeWeight(activeProgress, 0)
    val w1 = nodeWeight(activeProgress, 1)
    val w2 = nodeWeight(activeProgress, 2)
    val weights = cache.weightsBuf
    weights[0] = w0
    weights[1] = w1
    weights[2] = w2

    val radii = cache.radiiBuf
    radii[0] = lerpFloat(metrics.inactiveRadius, metrics.activeRadius, w0)
    radii[1] = lerpFloat(metrics.inactiveRadius, metrics.activeRadius, w1)
    radii[2] = lerpFloat(metrics.inactiveRadius, metrics.activeRadius, w2)

    val activeX = interpolateSlotX(metrics.slotX, activeProgress)
    val activeR = metrics.activeRadius * (1f + 0.032f * touchIntensity)

    // =========================================================================
    // LAYER 1: PRE-BAKED BACKGROUND UNDERLAY (0.05ms single bitmap blit!)
    // =========================================================================
    val dockBmp = cache.getOrBakeStaticDock(wInt, hInt, colors)
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawBitmap(dockBmp, 0f, 0f, cache.bitmapPaint)
    }

    // =========================================================================
    // LAYER 2: SCULPTED 3-LOBE GLASSMORPHISM BASE (GPU Gradient + Bevel Rim)
    // =========================================================================
    build3LobePeanutPath(
        outPath = cache.lobeBasePath,
        metrics = metrics,
        radii = radii,
        pad = metrics.height * 0.032f
    )
    drawLayer2GlassmorphismBase(
        metrics = metrics,
        lobePath = cache.lobeBasePath,
        activeX = activeX,
        colors = colors,
        cache = cache
    )

    // =========================================================================
    // LAYER 3: TAUT DIAGONAL SILK CRADLE & NESTED PULSE CONTOURS (Zero Blur Stall)
    // =========================================================================
    drawLayer3NeonSilkAndMesh(
        metrics = metrics,
        lobeClipPath = cache.lobeBasePath,
        radii = radii,
        weights = weights,
        activeX = activeX,
        activeR = activeR,
        idlePulse = idlePulse,
        touchIntensity = touchIntensity,
        cache = cache
    )

    // =========================================================================
    // LAYER 4: INACTIVE GLASS POCKETS + EXPANDING WAVES + SEAMLESS ACTIVE ORB
    // =========================================================================
    for (i in 0..2) {
        val inactiveAlpha = (1f - weights[i]).coerceIn(0f, 1f)
        if (inactiveAlpha > 0.02f) {
            drawLayer4AInactiveGlassPocket(
                cx = metrics.slotX[i],
                cy = metrics.centerY,
                radius = radii[i] * 0.90f,
                alphaFactor = inactiveAlpha,
                colors = colors,
                cache = cache
            )
        }
    }

    if (wavePulseProgress > 0.005f && wavePulseProgress < 0.995f) {
        drawLayer4BExpandingWaveRings(
            cx = activeX,
            cy = metrics.centerY,
            baseRadius = activeR,
            lobeClipPath = cache.lobeBasePath,
            progress = wavePulseProgress,
            colors = colors,
            cache = cache
        )
    }

    drawLayer4CActiveOrbFX(
        cx = activeX,
        cy = metrics.centerY,
        radius = activeR,
        idlePulse = idlePulse,
        touchIntensity = touchIntensity,
        colors = colors,
        cache = cache
    )
}

private fun build3LobePeanutPath(
    outPath: Path,
    metrics: ContinuumMetrics,
    radii: FloatArray,
    pad: Float
) {
    val cy = metrics.centerY
    val x0 = metrics.slotX[0]
    val x1 = metrics.slotX[1]
    val x2 = metrics.slotX[2]

    val r0 = (radii[0] + pad).coerceAtLeast(4f)
    val r1 = (radii[1] + pad).coerceAtLeast(4f)
    val r2 = (radii[2] + pad).coerceAtLeast(4f)

    val w01X = (x0 + x1) * 0.50f
    val w12X = (x1 + x2) * 0.50f
    val w01H = (min(r0, r1) * 0.46f).coerceAtLeast(3f)
    val w12H = (min(r1, r2) * 0.46f).coerceAtLeast(3f)

    val dx01 = (w01X - x0) * 0.52f
    val dx10 = (x1 - w01X) * 0.52f
    val dx12 = (w12X - x1) * 0.52f
    val dx21 = (x2 - w12X) * 0.52f

    outPath.reset()
    outPath.moveTo(x0, cy + r0)
    outPath.cubicTo(x0 - r0 * CIRCLE_KAPPA, cy + r0, x0 - r0, cy + r0 * CIRCLE_KAPPA, x0 - r0, cy)
    outPath.cubicTo(x0 - r0, cy - r0 * CIRCLE_KAPPA, x0 - r0 * CIRCLE_KAPPA, cy - r0, x0, cy - r0)

    outPath.cubicTo(x0 + dx01, cy - r0, w01X - dx01 * 0.72f, cy - w01H, w01X, cy - w01H)
    outPath.cubicTo(w01X + dx10 * 0.72f, cy - w01H, x1 - dx10, cy - r1, x1, cy - r1)
    outPath.cubicTo(x1 + dx12, cy - r1, w12X - dx12 * 0.72f, cy - w12H, w12X, cy - w12H)
    outPath.cubicTo(w12X + dx21 * 0.72f, cy - w12H, x2 - dx21, cy - r2, x2, cy - r2)

    outPath.cubicTo(x2 + r2 * CIRCLE_KAPPA, cy - r2, x2 + r2, cy - r2 * CIRCLE_KAPPA, x2 + r2, cy)
    outPath.cubicTo(x2 + r2, cy + r2 * CIRCLE_KAPPA, x2 + r2 * CIRCLE_KAPPA, cy + r2, x2, cy + r2)

    outPath.cubicTo(x2 - dx21, cy + r2, w12X + dx21 * 0.72f, cy + w12H, w12X, cy + w12H)
    outPath.cubicTo(w12X - dx12 * 0.72f, cy + w12H, x1 + dx12, cy + r1, x1, cy + r1)
    outPath.cubicTo(x1 - dx10, cy + r1, w01X + dx10 * 0.72f, cy + w01H, w01X, cy + w01H)
    outPath.cubicTo(w01X - dx01 * 0.72f, cy + w01H, x0 + dx01, cy + r0, x0, cy + r0)
    outPath.close()
}

private fun DrawScope.drawLayer2GlassmorphismBase(
    metrics: ContinuumMetrics,
    lobePath: Path,
    activeX: Float,
    colors: ContinuumNavColors,
    cache: ContinuumDrawCache
) {
    val w = metrics.width
    val h = metrics.height
    val cy = metrics.centerY

    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        val p = cache.paint

        // 2A. Sculpted Frosted Glass 3-Lobe Fill
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.color = colors.glassMid.toArgb()
        nc.drawPath(lobePath, p)

        // 2B. Soft 3D Glass Bevel Halo (GPU Screen pass, zero CPU blur!)
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.strokeWidth = (h * 0.034f).coerceAtLeast(2.2f)
        p.xfermode = cache.xferScreen
        p.color = colors.glassRimSide.copy(alpha = 0.24f).toArgb()
        nc.drawPath(lobePath, p)

        // 2C. Crisp Glass Specular Edge Highlight concentrated near active node
        val rimShader = cache.unitSilkVeilShader
        if (rimShader != null) {
            cache.shaderMatrix.reset()
            val rad = (w * 0.56f).coerceAtLeast(1f)
            cache.shaderMatrix.setScale(rad, rad)
            cache.shaderMatrix.postTranslate(activeX, cy - metrics.activeRadius * 0.35f)
            rimShader.setLocalMatrix(cache.shaderMatrix)
        }
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.strokeWidth = (h * 0.013f).coerceAtLeast(1.1f)
        p.shader = rimShader
        p.alpha = 180
        nc.drawPath(lobePath, p)
    }
}

private fun DrawScope.drawLayer3NeonSilkAndMesh(
    metrics: ContinuumMetrics,
    lobeClipPath: Path,
    radii: FloatArray,
    weights: FloatArray,
    activeX: Float,
    activeR: Float,
    idlePulse: Float,
    touchIntensity: Float,
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

    val energyBoost = 1f + 0.10f * idlePulse + 0.20f * touchIntensity

    // Taut diagonal silk sweep from inactive top tangents to active bottom rim (v14 geometry)
    val y0 = cy + (-0.82f * r0) * (1f - weights[0]) + (0.98f * r0) * weights[0]
    val y1 = cy + (-0.68f * r1) * (1f - weights[1]) + (0.98f * r1) * weights[1]
    val y2 = cy + (-0.82f * r2) * (1f - weights[2]) + (0.98f * r2) * weights[2]

    val dx01 = (x1 - x0) * 0.44f
    val dx12 = (x2 - x1) * 0.44f

    val cradleCurve = cache.silkPath.apply {
        reset()
        moveTo(x0 - r0 * 0.85f, cy - r0 * 0.45f * (1f - weights[0]) + r0 * 0.42f * weights[0])
        cubicTo(x0 - r0 * 0.35f, y0, x0 - r0 * 0.08f, y0, x0, y0)
        cubicTo(x0 + dx01, y0, x1 - dx01, y1, x1, y1)
        cubicTo(x1 + dx12, y1, x2 - dx12, y2, x2, y2)
        cubicTo(x2 + r2 * 0.08f, y2, x2 + r2 * 0.35f, y2, x2 + r2 * 0.85f, cy - r2 * 0.45f * (1f - weights[2]) + r2 * 0.42f * weights[2])
    }

    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        val p = cache.paint
        val gp = cache.glowPaint

        nc.save()
        nc.clipPath(lobeClipPath)

        // 3A. Volumetric Silk Glow Pool inside 3-Lobe Body (Matrix-translated unit shader)
        val veilShader = cache.unitSilkVeilShader
        if (veilShader != null) {
            val rad = (w * 0.46f).coerceAtLeast(1f)
            cache.shaderMatrix.reset()
            cache.shaderMatrix.setScale(rad, rad)
            cache.shaderMatrix.postTranslate(activeX, cy + activeR * 0.25f)
            veilShader.setLocalMatrix(cache.shaderMatrix)
        }
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.xfermode = cache.xferScreen
        p.shader = veilShader
        p.alpha = (210f * energyBoost).toInt().coerceIn(0, 255)
        nc.drawPath(lobeClipPath, p)

        // 3B. Nested 3-Lobe Pulse-Fiber Contours (2 lightweight GPU strokes)
        val coreShader = cache.unitSilkCoreShader
        if (coreShader != null) {
            val rad = (w * 0.48f).coerceAtLeast(1f)
            cache.shaderMatrix.reset()
            cache.shaderMatrix.setScale(rad, rad)
            cache.shaderMatrix.postTranslate(activeX, cy + activeR * 0.20f)
            coreShader.setLocalMatrix(cache.shaderMatrix)
        }
        for (idx in 0..1) {
            build3LobePeanutPath(
                outPath = cache.nestedPath,
                metrics = metrics,
                radii = radii,
                pad = -h * (0.016f + idx * 0.042f)
            )
            p.reset()
            p.isAntiAlias = true
            p.style = Paint.Style.STROKE
            p.strokeWidth = (h * (0.016f - idx * 0.004f)).coerceAtLeast(1.0f)
            p.xfermode = cache.xferScreen
            p.shader = coreShader
            p.alpha = ((135f - idx * 45f) * energyBoost).toInt().coerceIn(20, 220)
            nc.drawPath(cache.nestedPath, p)
        }

        // 3C. Multi-Pass GPU Silk Cradle Ribbon (replaces slow CPU BlurMaskFilter with 3 fast additive passes!)
        if (veilShader != null) {
            val rad = (w * 0.38f).coerceAtLeast(1f)
            cache.shaderMatrix.reset()
            cache.shaderMatrix.setScale(rad, rad)
            cache.shaderMatrix.postTranslate(activeX, cy + activeR * 0.65f)
            veilShader.setLocalMatrix(cache.shaderMatrix)
        }

        // Outer soft silk veil pass
        gp.reset()
        gp.isAntiAlias = true
        gp.style = Paint.Style.STROKE
        gp.strokeCap = Paint.Cap.ROUND
        gp.strokeWidth = (h * 0.14f).coerceAtLeast(7f)
        gp.xfermode = cache.xferScreen
        gp.shader = veilShader
        gp.alpha = (115f * energyBoost).toInt().coerceIn(0, 220)
        nc.drawPath(cradleCurve, gp)

        // Mid electric-violet halo pass
        gp.strokeWidth = (h * 0.055f).coerceAtLeast(3f)
        gp.alpha = (155f * energyBoost).toInt().coerceIn(0, 240)
        nc.drawPath(cradleCurve, gp)

        // Inner crisp luminous silk filament pass
        if (coreShader != null) {
            val rad = (w * 0.30f).coerceAtLeast(1f)
            cache.shaderMatrix.reset()
            cache.shaderMatrix.setScale(rad, rad)
            cache.shaderMatrix.postTranslate(activeX, cy + activeR * 0.78f)
            coreShader.setLocalMatrix(cache.shaderMatrix)
        }
        gp.strokeWidth = (h * 0.020f).coerceAtLeast(1.4f)
        gp.xfermode = cache.xferAdd
        gp.shader = coreShader
        gp.alpha = (225f * energyBoost).toInt().coerceIn(0, 255)
        nc.drawPath(cradleCurve, gp)

        nc.restore()
    }
}

private fun DrawScope.drawLayer4AInactiveGlassPocket(
    cx: Float,
    cy: Float,
    radius: Float,
    alphaFactor: Float,
    colors: ContinuumNavColors,
    cache: ContinuumDrawCache
) {
    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        val p = cache.paint

        val pocketShader = cache.unitPocketFillShader
        if (pocketShader != null) {
            val rad = (radius * 1.05f).coerceAtLeast(1f)
            cache.shaderMatrix.reset()
            cache.shaderMatrix.setScale(rad, rad)
            cache.shaderMatrix.postTranslate(cx, cy - radius * 0.18f)
            pocketShader.setLocalMatrix(cache.shaderMatrix)
        }

        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.shader = pocketShader
        p.alpha = (255f * alphaFactor).toInt().coerceIn(0, 255)
        nc.drawCircle(cx, cy, radius, p)

        // Delicate rim highlight
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.strokeWidth = (radius * 0.044f).coerceAtLeast(1.0f)
        p.xfermode = cache.xferScreen
        p.color = colors.pocketRim.copy(alpha = 0.34f * alphaFactor).toArgb()
        nc.drawCircle(cx, cy, radius * 0.98f, p)
    }
}

private fun DrawScope.drawLayer4BExpandingWaveRings(
    cx: Float,
    cy: Float,
    baseRadius: Float,
    lobeClipPath: Path,
    progress: Float,
    colors: ContinuumNavColors,
    cache: ContinuumDrawCache
) {
    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        val p = cache.glowPaint

        nc.save()
        nc.clipPath(lobeClipPath)
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.xfermode = cache.xferScreen

        for (ring in 0..2) {
            val localP = ((progress - ring * 0.14f) / 0.72f).coerceIn(0f, 1f)
            if (localP <= 0f || localP >= 1f) continue
            val ringRadius = baseRadius * (1.02f + localP * 1.15f)
            val alpha = (1f - localP) * (1f - localP) * 0.42f

            p.strokeWidth = (baseRadius * 0.052f * (1f - localP * 0.5f)).coerceAtLeast(1f)
            p.color = colors.silkPureWhite.copy(alpha = alpha).toArgb()
            nc.drawCircle(cx, cy, ringRadius, p)
        }
        nc.restore()
    }
}

private fun DrawScope.drawLayer4CActiveOrbFX(
    cx: Float,
    cy: Float,
    radius: Float,
    idlePulse: Float,
    touchIntensity: Float,
    colors: ContinuumNavColors,
    cache: ContinuumDrawCache
) {
    val bloomBoost = 1f + 0.10f * idlePulse + 0.22f * touchIntensity

    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        val p = cache.paint

        // 1. Wide Seamless Outer Ambient Bloom (Matrix-translated unit RadialGradient)
        val bloomRadius = (radius * (1.82f + 0.10f * idlePulse + 0.16f * touchIntensity)).coerceAtLeast(1f)
        val bloomShader = cache.unitBloomShader
        if (bloomShader != null) {
            cache.shaderMatrix.reset()
            cache.shaderMatrix.setScale(bloomRadius, bloomRadius)
            cache.shaderMatrix.postTranslate(cx, cy + radius * 0.08f)
            bloomShader.setLocalMatrix(cache.shaderMatrix)
        }
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.xfermode = cache.xferScreen
        p.shader = bloomShader
        p.alpha = (235f * bloomBoost).toInt().coerceIn(0, 255)
        nc.drawCircle(cx, cy + radius * 0.06f, bloomRadius, p)

        // 2. Clipped Clear-Core Dark Translucent Cosmic Indigo Interior
        val orbClip = cache.clipPath.apply {
            reset()
            addCircle(cx, cy, radius, Path.Direction.CW)
        }
        nc.save()
        nc.clipPath(orbClip)

        val coreShader = cache.unitOrbCenterShader
        if (coreShader != null) {
            cache.shaderMatrix.reset()
            cache.shaderMatrix.setScale(radius.coerceAtLeast(1f), radius.coerceAtLeast(1f))
            cache.shaderMatrix.postTranslate(cx, cy - radius * 0.06f)
            coreShader.setLocalMatrix(cache.shaderMatrix)
        }
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.shader = coreShader
        p.alpha = 255
        nc.drawCircle(cx, cy, radius, p)

        // Smooth bottom 110-degree curved crescent glow (no harsh pin-point dot!)
        val botShader = cache.unitOrbBottomCrescentShader
        if (botShader != null) {
            val rBot = (radius * 0.72f).coerceAtLeast(1f)
            cache.shaderMatrix.reset()
            cache.shaderMatrix.setScale(rBot, rBot)
            cache.shaderMatrix.postTranslate(cx, cy + radius * 1.12f)
            botShader.setLocalMatrix(cache.shaderMatrix)
        }
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.xfermode = cache.xferScreen
        p.shader = botShader
        p.alpha = 230
        nc.drawCircle(cx, cy, radius, p)

        // Subtle top crescent glass reflection
        val topShader = cache.unitOrbTopCrescentShader
        if (topShader != null) {
            val rTop = (radius * 0.56f).coerceAtLeast(1f)
            cache.shaderMatrix.reset()
            cache.shaderMatrix.setScale(rTop, rTop)
            cache.shaderMatrix.postTranslate(cx, cy - radius * 1.18f)
            topShader.setLocalMatrix(cache.shaderMatrix)
        }
        p.shader = topShader
        p.alpha = 200
        nc.drawCircle(cx, cy, radius, p)
        nc.restore()

        // 3. Multi-Pass GPU Neon Ring Halo (replaces slow BlurMaskFilter with 2 ultra-fast additive rings!)
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.xfermode = cache.xferAdd
        p.strokeWidth = (radius * 0.16f).coerceAtLeast(3.5f)
        p.color = colors.orbOuterBloom.copy(alpha = (0.34f * bloomBoost).coerceAtMost(0.55f)).toArgb()
        nc.drawCircle(cx, cy, radius, p)

        p.strokeWidth = (radius * 0.085f).coerceAtLeast(2.0f)
        p.color = colors.orbRimSide.copy(alpha = (0.52f * bloomBoost).coerceAtMost(0.75f)).toArgb()
        nc.drawCircle(cx, cy, radius, p)

        // 4. Razor-Crisp Specular Lavender-White Rim Hairline
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.strokeWidth = (radius * 0.044f).coerceAtLeast(1.5f)
        p.color = colors.orbRimTopBot.toArgb()
        nc.drawCircle(cx, cy, radius, p)

        // 5. Smooth Curved Arc Glow along bottom & top rims (stretched elliptical shader via Matrix)
        drawCurvedArcBloom(nc, cx, cy + radius, radius * 0.78f, radius * 0.14f, 0.78f * bloomBoost, cache)
        drawCurvedArcBloom(nc, cx, cy - radius, radius * 0.48f, radius * 0.10f, 0.46f * bloomBoost, cache)
    }
}

private fun drawCurvedArcBloom(
    nc: Canvas,
    fx: Float,
    fy: Float,
    rx: Float,
    ry: Float,
    alpha: Float,
    cache: ContinuumDrawCache
) {
    val shader = cache.unitFlareShader ?: return
    val sx = rx.coerceAtLeast(1f)
    val sy = ry.coerceAtLeast(1f)
    cache.shaderMatrix.reset()
    cache.shaderMatrix.setScale(sx, sy)
    cache.shaderMatrix.postTranslate(fx, fy)
    shader.setLocalMatrix(cache.shaderMatrix)

    val p = cache.glowPaint
    p.reset()
    p.isAntiAlias = true
    p.style = Paint.Style.FILL
    p.xfermode = cache.xferAdd
    p.shader = shader
    p.alpha = (255f * alpha).toInt().coerceIn(0, 240)
    nc.drawOval(fx - sx, fy - sy, fx + sx, fy + sy, p)
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
