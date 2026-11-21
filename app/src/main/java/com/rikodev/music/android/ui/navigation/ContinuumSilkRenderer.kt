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
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlin.math.abs

private const val CIRCLE_KAPPA = 0.55228475f

internal class ContinuumDrawCache {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    val xferScreen = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
    val xferAdd = PorterDuffXfermode(PorterDuff.Mode.ADD)

    val pathA = Path()
    val pathB = Path()
    val pathC = Path()
    val pathD = Path()
    val clipPath = Path()
    val arcRect = RectF()
    val shaderMatrix = Matrix()

    // Pre-allocated scratch arrays (0 per-frame allocations)
    val weightsBuf = FloatArray(3)
    val radiiBuf = FloatArray(3)

    // Pre-baked static Layer 1 & 2 Frosted Twilight-Indigo Glass Visor Bitmap (~0.05ms blit per frame)
    private var staticVisorBitmap: Bitmap? = null
    private var cachedWidth: Int = -1
    private var cachedHeight: Int = -1
    private var cachedVisorColors: ContinuumNavColors? = null

    // Pre-allocated unit-space Shaders moved via Matrix (0 allocations per frame!)
    var unitCocoonShader: RadialGradient? = null
    var unitRibbonWingShader: RadialGradient? = null
    var unitBottomHorizonShader: RadialGradient? = null
    var unitDiagAuraShader: RadialGradient? = null
    var unitDiagFilamentShader: RadialGradient? = null
    var unitDiagShimmerShader: RadialGradient? = null
    var unitPocketFillShader: RadialGradient? = null
    var unitOrbOuterBloomShader: RadialGradient? = null
    var unitOrbCoreShader: RadialGradient? = null
    var unitOrbVerticalRimShader: LinearGradient? = null
    var unitOrbInnerRimShader: LinearGradient? = null
    var unitBottomArcWideShader: LinearGradient? = null
    var unitBottomArcTightShader: LinearGradient? = null
    var unitTopArcShader: LinearGradient? = null
    private var shadersInitialized = false
    private var cachedShaderColors: ContinuumNavColors? = null

    fun ensureUnitShaders(colors: ContinuumNavColors) {
        if (shadersInitialized && cachedShaderColors == colors) return
        shadersInitialized = true
        cachedShaderColors = colors

        unitCocoonShader = RadialGradient(
            0f, 0f, 1f,
            intArrayOf(
                colors.silkRoyalViolet.copy(alpha = 0.44f).toArgb(),
                colors.silkDeepIndigo.copy(alpha = 0.25f).toArgb(),
                colors.silkDeepIndigo.copy(alpha = 0.10f).toArgb()
            ),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP
        )

        unitRibbonWingShader = RadialGradient(
            0f, 0f, 1f,
            intArrayOf(
                colors.orbOuterBloom.copy(alpha = 0.52f).toArgb(),
                colors.silkElectricViolet.copy(alpha = 0.28f).toArgb(),
                colors.silkDeepIndigo.copy(alpha = 0.04f).toArgb()
            ),
            floatArrayOf(0f, 0.46f, 1f),
            Shader.TileMode.CLAMP
        )

        unitBottomHorizonShader = RadialGradient(
            0f, 0f, 1f,
            intArrayOf(
                colors.orbFlareMid.copy(alpha = 0.56f).toArgb(),
                colors.orbOuterBloom.copy(alpha = 0.28f).toArgb(),
                colors.pocketRim.copy(alpha = 0.14f).toArgb()
            ),
            floatArrayOf(0f, 0.50f, 1f),
            Shader.TileMode.CLAMP
        )

        unitDiagAuraShader = RadialGradient(
            0f, 0f, 1f,
            intArrayOf(
                colors.orbFlareMid.copy(alpha = 0.56f).toArgb(),
                colors.orbOuterBloom.copy(alpha = 0.36f).toArgb(),
                colors.silkRoyalViolet.copy(alpha = 0.10f).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.36f, 0.70f, 1f),
            Shader.TileMode.CLAMP
        )

        unitDiagFilamentShader = RadialGradient(
            0f, 0f, 1f,
            intArrayOf(
                colors.orbRimTopBot.copy(alpha = 0.94f).toArgb(),
                colors.silkElectricViolet.copy(alpha = 0.60f).toArgb(),
                colors.pocketRim.copy(alpha = 0.18f).toArgb()
            ),
            floatArrayOf(0f, 0.44f, 1f),
            Shader.TileMode.CLAMP
        )

        unitDiagShimmerShader = RadialGradient(
            0f, 0f, 1f,
            intArrayOf(
                colors.silkBrightLavender.copy(alpha = 0.42f).toArgb(),
                colors.orbOuterBloom.copy(alpha = 0.22f).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP
        )

        unitPocketFillShader = RadialGradient(
            0f, 0f, 1f,
            intArrayOf(
                colors.pocketInner.copy(alpha = 0.74f).toArgb(),
                colors.silkDeepIndigo.copy(alpha = 0.32f).toArgb(),
                colors.pocketEdge.copy(alpha = 0.28f).toArgb()
            ),
            floatArrayOf(0f, 0.64f, 1f),
            Shader.TileMode.CLAMP
        )

        // Broad 4-stop smooth exponential outer violet bloom (exact v14)
        unitOrbOuterBloomShader = RadialGradient(
            0f, 0f, 1f,
            intArrayOf(
                colors.orbOuterBloom.copy(alpha = 0.54f).toArgb(),
                colors.silkRoyalViolet.copy(alpha = 0.30f).toArgb(),
                colors.orbOuterDeep.copy(alpha = 0.11f).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.36f, 0.68f, 1f),
            Shader.TileMode.CLAMP
        )

        // Translucent emerald/teal Fresnel orb interior (theme-reactive, no milky pastel disc)
        val centerAlpha = if (colors.isLightMode) 0.82f else 0.74f
        val midAlpha = if (colors.isLightMode) 0.78f else 0.70f
        unitOrbCoreShader = RadialGradient(
            0f, 0f, 1f,
            intArrayOf(
                colors.orbInnerCenter.copy(alpha = centerAlpha).toArgb(),
                colors.orbInnerMid.copy(alpha = midAlpha).toArgb(),
                colors.orbInnerEdge.copy(alpha = 0.52f).toArgb(),
                colors.orbInnerRimGlow.copy(alpha = 0.76f).toArgb()
            ),
            floatArrayOf(0f, 0.60f, 0.86f, 1f),
            Shader.TileMode.CLAMP
        )

        // Unit vertical gradient [-1..+1] for orb rims
        unitOrbVerticalRimShader = LinearGradient(
            0f, -1f, 0f, 1f,
            intArrayOf(
                colors.orbRimTopBot.toArgb(),
                colors.orbRimSide.copy(alpha = 0.90f).toArgb(),
                colors.orbRimTopBot.toArgb()
            ),
            floatArrayOf(0f, 0.50f, 1f),
            Shader.TileMode.CLAMP
        )

        unitOrbInnerRimShader = LinearGradient(
            0f, -1f, 0f, 1f,
            intArrayOf(
                colors.orbFlareMid.copy(alpha = 0.44f).toArgb(),
                colors.orbOuterBloom.copy(alpha = 0.26f).toArgb(),
                colors.orbFlareMid.copy(alpha = 0.60f).toArgb()
            ),
            floatArrayOf(0f, 0.50f, 1f),
            Shader.TileMode.CLAMP
        )

        // Unit horizontal gradients [-1..+1] for smooth 110-degree curved bottom & top arc glows
        unitBottomArcWideShader = LinearGradient(
            -1f, 0f, 1f, 0f,
            intArrayOf(
                Color.Transparent.toArgb(),
                colors.orbOuterBloom.copy(alpha = 0.42f).toArgb(),
                colors.orbFlareMid.copy(alpha = 0.64f).toArgb(),
                colors.orbOuterBloom.copy(alpha = 0.42f).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.22f, 0.50f, 0.78f, 1f),
            Shader.TileMode.CLAMP
        )

        unitBottomArcTightShader = LinearGradient(
            -1f, 0f, 1f, 0f,
            intArrayOf(
                Color.Transparent.toArgb(),
                colors.orbFlareMid.copy(alpha = 0.58f).toArgb(),
                colors.orbFlareCore.copy(alpha = 0.90f).toArgb(),
                colors.orbFlareMid.copy(alpha = 0.58f).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.24f, 0.50f, 0.76f, 1f),
            Shader.TileMode.CLAMP
        )

        unitTopArcShader = LinearGradient(
            -1f, 0f, 1f, 0f,
            intArrayOf(
                Color.Transparent.toArgb(),
                colors.orbFlareCore.copy(alpha = 0.64f).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.50f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    fun getOrBakeStaticVisor(
        widthPx: Int,
        heightPx: Int,
        colors: ContinuumNavColors
    ): Bitmap {
        val existing = staticVisorBitmap
        if (existing != null && !existing.isRecycled && cachedWidth == widthPx && cachedHeight == heightPx && cachedVisorColors == colors) {
            return existing
        }

        existing?.recycle()
        val bmp = Bitmap.createBitmap(widthPx.coerceAtLeast(2), heightPx.coerceAtLeast(2), Bitmap.Config.ARGB_8888)
        val nc = Canvas(bmp)
        val w = widthPx.toFloat()
        val h = heightPx.toFloat()

        // Exact v14 Frosted Twilight-Indigo Glass Visor
        val padX = w * 0.006f
        val topCenterY = h * 0.175f
        val topSideY = h * 0.270f
        val bottomY = h * 0.980f
        val cornerR = h * 0.30f

        pathA.reset()
        pathA.moveTo(padX, topSideY + cornerR * 0.45f)
        pathA.cubicTo(padX, topSideY, w * 0.18f, topCenterY + h * 0.01f, w * 0.50f, topCenterY)
        pathA.cubicTo(w * 0.82f, topCenterY + h * 0.01f, w - padX, topSideY, w - padX, topSideY + cornerR * 0.45f)
        pathA.cubicTo(w - padX, bottomY - h * 0.02f, w - padX - cornerR * 0.50f, bottomY, w - padX - cornerR * 1.10f, bottomY)
        pathA.lineTo(padX + cornerR * 1.10f, bottomY)
        pathA.cubicTo(padX + cornerR * 0.50f, bottomY, padX, bottomY - h * 0.02f, padX, topSideY + cornerR * 0.45f)
        pathA.close()

        pathB.reset()
        pathB.moveTo(padX + w * 0.008f, topSideY + cornerR * 0.32f)
        pathB.cubicTo(padX + w * 0.02f, topSideY, w * 0.18f, topCenterY + h * 0.01f, w * 0.50f, topCenterY)
        pathB.cubicTo(w * 0.82f, topCenterY + h * 0.01f, w - padX - w * 0.02f, topSideY, w - padX - w * 0.008f, topSideY + cornerR * 0.32f)

        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        // One-time blurred shadow occlusion (0ms cost during 60fps animation!)
        p.style = Paint.Style.FILL
        p.color = (if (colors.isLightMode) Color(0x2E063838) else Color(0x80031010)).toArgb()
        p.maskFilter = BlurMaskFilter((h * 0.08f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        nc.drawPath(pathA, p)

        // Frosted twilight-indigo glass fill
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(
            0f, topCenterY, 0f, bottomY,
            intArrayOf(colors.glassTop.toArgb(), colors.glassMid.toArgb(), colors.glassBottom.toArgb()),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawPath(pathA, p)

        // Top specular rim hairline
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
            floatArrayOf(0f, 0.14f, 0.50f, 0.86f, 1f),
            Shader.TileMode.CLAMP
        )
        nc.drawPath(pathB, p)

        staticVisorBitmap = bmp
        cachedWidth = widthPx
        cachedHeight = heightPx
        cachedVisorColors = colors
        return bmp
    }

    fun release() {
        staticVisorBitmap?.recycle()
        staticVisorBitmap = null
        cachedWidth = -1
        cachedHeight = -1
        cachedVisorColors = null
        cachedShaderColors = null
        shadersInitialized = false
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
    val activeR = metrics.activeRadius * (1f + 0.03f * touchIntensity)

    // =========================================================================
    // LAYER 1 & 2: PRE-BAKED FROSTED TWILIGHT-INDIGO VISOR (~0.05ms blit)
    // =========================================================================
    val visorBmp = cache.getOrBakeStaticVisor(wInt, hInt, colors)
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawBitmap(visorBmp, 0f, 0f, cache.bitmapPaint)
    }

    // =========================================================================
    // LAYER 3: EXACT v14 TAUT DIAGONAL SILK SWEEP + SMOOTH LOW HORIZON
    // (Zero duplicate nested outlines, zero hourglass pinch, zero CPU blur!)
    // =========================================================================
    drawV14LiquidSilkVeilsFast(
        metrics = metrics,
        weights = weights,
        radii = radii,
        activeX = activeX,
        activeR = activeR,
        idlePulse = idlePulse,
        touchIntensity = touchIntensity,
        cache = cache
    )

    // =========================================================================
    // LAYER 4A: CLEAN FROSTED GLASS DOME POCKETS FOR INACTIVE NODES
    // =========================================================================
    for (i in 0..2) {
        val inactiveAlpha = (1f - weights[i]).coerceIn(0f, 1f)
        if (inactiveAlpha > 0.02f) {
            drawInactiveGlassPocketFast(
                cx = metrics.slotX[i],
                cy = metrics.centerY,
                radius = radii[i],
                alphaFactor = inactiveAlpha,
                colors = colors,
                cache = cache
            )
        }
    }

    // =========================================================================
    // LAYER 4B: STATE C SUBTLE EXPANDING AUDIO-WAVE SONAR ARCS
    // =========================================================================
    if (wavePulseProgress > 0.01f && wavePulseProgress < 0.98f) {
        drawSelectionWaveArcsFast(
            cx = activeX,
            cy = metrics.centerY,
            radius = activeR,
            waveProgress = wavePulseProgress,
            colors = colors,
            cache = cache
        )
    }

    // =========================================================================
    // LAYER 4C: EXACT v14 SEAMLESS DARK-CORE ORB + 110-DEG CURVED ARC + HAIRLINE
    // (100% GPU Matrix shaders & multi-pass strokes, 0ms CPU BlurMaskFilter!)
    // =========================================================================
    drawV14FresnelActiveOrbFast(
        cx = activeX,
        cy = metrics.centerY,
        radius = activeR,
        idlePulse = idlePulse,
        touchIntensity = touchIntensity,
        cache = cache
    )
}

private fun DrawScope.drawV14LiquidSilkVeilsFast(
    metrics: ContinuumMetrics,
    weights: FloatArray,
    radii: FloatArray,
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

    val waist01 = (x0 + x1) * 0.50f
    val waist12 = (x1 + x2) * 0.50f

    // Exact v14 geometry: low flat-curve bottom horizon + taut diagonal top tangents
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
        cubicTo(x2 + r2 * CIRCLE_KAPPA, cy - r2, x2 + r2, cy - r2 * CIRCLE_KAPPA, x2 + r2, cy)
        cubicTo(x2 + r2, cy + r2 * CIRCLE_KAPPA, x2 + r2 * CIRCLE_KAPPA, cy + r2, x2, cy + r2)
        cubicTo(x2 - r2 * 0.72f, cy + r2, waist12 + (x2 - x1) * 0.14f, botWaistY, waist12, botWaistY)
        cubicTo(waist12 - (x2 - x1) * 0.14f, botWaistY, x1 + r1 * 0.72f, cy + r1, x1, cy + r1)
        cubicTo(x1 - r1 * 0.72f, cy + r1, waist01 + (x1 - x0) * 0.14f, botWaistY, waist01, botWaistY)
        cubicTo(waist01 - (x1 - x0) * 0.14f, botWaistY, x0 + r0 * 0.72f, cy + r0, x0, cy + r0)
        cubicTo(x0 - r0 * CIRCLE_KAPPA, cy + r0, x0 - r0, cy + r0 * CIRCLE_KAPPA, x0 - r0, cy)
        cubicTo(x0 - r0, cy - r0 * CIRCLE_KAPPA, x0 - r0 * CIRCLE_KAPPA, cy - r0, x0, cy - r0)
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

    val energyBoost = 1f + (idlePulse - 0.5f) * 0.08f + touchIntensity * 0.14f

    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        val p = cache.paint
        val gp = cache.glowPaint

        // 3A. Soft Silk Cocoon Fill (Matrix-moved unit shader)
        val cocoonShader = cache.unitCocoonShader
        if (cocoonShader != null) {
            val rad = (w * 0.46f).coerceAtLeast(1f)
            cache.shaderMatrix.reset()
            cache.shaderMatrix.setScale(rad, rad)
            cache.shaderMatrix.postTranslate(activeX, cy + activeR * 0.30f)
            cocoonShader.setLocalMatrix(cache.shaderMatrix)
        }
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.shader = cocoonShader
        p.alpha = (255f * energyBoost).toInt().coerceIn(0, 255)
        nc.drawPath(cocoonPath, p)

        // 3B. Creamy Translucent Diagonal Silk Wing Clipped to Cocoon
        nc.save()
        nc.clipPath(cocoonPath)
        val wingShader = cache.unitRibbonWingShader
        if (wingShader != null) {
            val rad = (w * 0.42f).coerceAtLeast(1f)
            cache.shaderMatrix.reset()
            cache.shaderMatrix.setScale(rad, rad)
            cache.shaderMatrix.postTranslate(activeX, cy + activeR * 0.68f)
            wingShader.setLocalMatrix(cache.shaderMatrix)
        }
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.xfermode = cache.xferScreen
        p.shader = wingShader
        p.alpha = (230f * energyBoost).toInt().coerceIn(0, 255)
        nc.drawPath(ribbonA, p)
        nc.restore()

        // 3C. Single Smooth Low Bottom Horizon Rim
        val botShader = cache.unitBottomHorizonShader
        if (botShader != null) {
            val rad = (w * 0.48f).coerceAtLeast(1f)
            cache.shaderMatrix.reset()
            cache.shaderMatrix.setScale(rad, rad)
            cache.shaderMatrix.postTranslate(activeX, cy + activeR * 0.85f)
            botShader.setLocalMatrix(cache.shaderMatrix)
        }
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = (h * 0.011f).coerceAtLeast(1.1f)
        p.xfermode = cache.xferScreen
        p.shader = botShader
        p.alpha = 225
        nc.drawPath(bottomHorizonPath, p)

        // 3D. Unified Electric-Violet + Lavender Neon Glow on Taut Diagonal Silk (GPU Multi-Pass, 0 CPU Blur!)
        val auraShader = cache.unitDiagAuraShader
        if (auraShader != null) {
            val rad = (w * 0.34f).coerceAtLeast(1f)
            cache.shaderMatrix.reset()
            cache.shaderMatrix.setScale(rad, rad)
            cache.shaderMatrix.postTranslate(activeX, cy + activeR * 0.82f)
            auraShader.setLocalMatrix(cache.shaderMatrix)
        }
        gp.reset()
        gp.isAntiAlias = true
        gp.style = Paint.Style.STROKE
        gp.strokeCap = Paint.Cap.ROUND
        gp.xfermode = cache.xferAdd
        gp.shader = auraShader

        // Soft outer diagonal neon glow pass
        gp.strokeWidth = (h * 0.070f).coerceAtLeast(4.5f)
        gp.alpha = (72f * energyBoost).toInt().coerceIn(0, 140)
        nc.drawPath(diag1, gp)

        // Tighter electric-violet diagonal glow pass
        gp.strokeWidth = (h * 0.032f).coerceAtLeast(2.4f)
        gp.alpha = (135f * energyBoost).toInt().coerceIn(0, 200)
        nc.drawPath(diag1, gp)

        // Crisp lavender-white diagonal silk hairline matching the orb liner
        val filShader = cache.unitDiagFilamentShader
        if (filShader != null) {
            val rad = (w * 0.38f).coerceAtLeast(1f)
            cache.shaderMatrix.reset()
            cache.shaderMatrix.setScale(rad, rad)
            cache.shaderMatrix.postTranslate(activeX, cy + activeR * 0.84f)
            filShader.setLocalMatrix(cache.shaderMatrix)
        }
        gp.strokeWidth = (h * 0.012f).coerceAtLeast(1.2f)
        gp.xfermode = cache.xferScreen
        gp.shader = filShader
        gp.alpha = 245
        nc.drawPath(diag1, gp)
    }
}

private fun DrawScope.drawInactiveGlassPocketFast(
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
            cache.shaderMatrix.postTranslate(cx, cy - radius * 0.12f)
            pocketShader.setLocalMatrix(cache.shaderMatrix)
        }
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.shader = pocketShader
        p.alpha = (255f * alphaFactor).toInt().coerceIn(0, 255)
        nc.drawCircle(cx, cy, radius, p)

        // Clean outer-crescent specular rim (GPU dual-pass, 0 CPU blur)
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.xfermode = cache.xferScreen
        p.strokeWidth = (radius * 0.072f).coerceAtLeast(2.0f)
        p.color = colors.pocketRim.copy(alpha = 0.16f * alphaFactor).toArgb()
        nc.drawCircle(cx, cy, radius * 0.97f, p)

        p.strokeWidth = (radius * 0.036f).coerceAtLeast(1.1f)
        p.color = colors.pocketRim.copy(alpha = 0.44f * alphaFactor).toArgb()
        nc.drawCircle(cx, cy, radius * 0.97f, p)
    }
}

private fun DrawScope.drawSelectionWaveArcsFast(
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
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.xfermode = cache.xferScreen

        for (ring in 0..1) {
            val offset = ring * 0.18f
            val localP = ((waveProgress - offset) / (1f - offset)).coerceIn(0f, 1f)
            if (localP <= 0.01f) continue
            val ringR = radius * (1.06f + 0.58f * localP)
            val alpha = fade * (1f - localP * 0.5f) * (if (ring == 0) 0.42f else 0.26f)
            p.strokeWidth = (radius * 0.036f).coerceAtLeast(1.2f)
            p.color = colors.silkBrightLavender.copy(alpha = alpha.coerceIn(0f, 1f)).toArgb()
            cache.arcRect.set(cx - ringR, cy - ringR, cx + ringR, cy + ringR)
            nc.drawArc(cache.arcRect, -34f, 68f, false, p)
            nc.drawArc(cache.arcRect, 146f, 68f, false, p)
        }
    }
}

private fun DrawScope.drawV14FresnelActiveOrbFast(
    cx: Float,
    cy: Float,
    radius: Float,
    idlePulse: Float,
    touchIntensity: Float,
    cache: ContinuumDrawCache
) {
    val bloomBoost = 1f + (idlePulse - 0.5f) * 0.10f + touchIntensity * 0.16f

    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        val p = cache.paint

        // 1. Broad 4-Stop Smooth Exponential Outer Violet Bloom (Matrix-moved unit shader)
        val bloomR = (radius * 2.15f).coerceAtLeast(1f)
        val bloomShader = cache.unitOrbOuterBloomShader
        if (bloomShader != null) {
            cache.shaderMatrix.reset()
            cache.shaderMatrix.setScale(bloomR, bloomR)
            cache.shaderMatrix.postTranslate(cx, cy + radius * 0.06f)
            bloomShader.setLocalMatrix(cache.shaderMatrix)
        }
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.xfermode = cache.xferScreen
        p.shader = bloomShader
        p.alpha = (255f * bloomBoost).toInt().coerceIn(0, 255)
        nc.drawCircle(cx, cy + radius * 0.06f, bloomR, p)

        // 2. Dark Translucent Cosmic-Indigo Interior (Exact v14)
        val orbClip = cache.clipPath.apply {
            reset()
            addCircle(cx, cy, radius, Path.Direction.CW)
        }
        nc.save()
        nc.clipPath(orbClip)

        val coreShader = cache.unitOrbCoreShader
        if (coreShader != null) {
            val r = radius.coerceAtLeast(1f)
            cache.shaderMatrix.reset()
            cache.shaderMatrix.setScale(r, r)
            cache.shaderMatrix.postTranslate(cx, cy)
            coreShader.setLocalMatrix(cache.shaderMatrix)
        }
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.shader = coreShader
        p.alpha = 255
        nc.drawCircle(cx, cy, radius, p)

        // 3. Thin Inner-Edge Violet Rim Glow (GPU multi-pass, 0 CPU blur)
        val innerRimShader = cache.unitOrbInnerRimShader
        if (innerRimShader != null) {
            val r = radius.coerceAtLeast(1f)
            cache.shaderMatrix.reset()
            cache.shaderMatrix.setScale(r, r)
            cache.shaderMatrix.postTranslate(cx, cy)
            innerRimShader.setLocalMatrix(cache.shaderMatrix)
        }
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.xfermode = cache.xferScreen
        p.shader = innerRimShader
        p.strokeWidth = (radius * 0.16f).coerceAtLeast(4f)
        p.alpha = 110
        nc.drawCircle(cx, cy, radius * 0.94f, p)

        p.strokeWidth = (radius * 0.08f).coerceAtLeast(2.2f)
        p.alpha = 165
        nc.drawCircle(cx, cy, radius * 0.96f, p)
        nc.restore()

        // 4. Smooth 110-Degree Curved Bottom Arc Glow (GPU Multi-Pass, No Pin-Point Dot, No CPU Blur!)
        val outerArcR = radius * 1.01f
        cache.arcRect.set(cx - outerArcR, cy - outerArcR, cx + outerArcR, cy + outerArcR)
        val botWideShader = cache.unitBottomArcWideShader
        if (botWideShader != null) {
            val sx = (radius * 0.90f).coerceAtLeast(1f)
            cache.shaderMatrix.reset()
            cache.shaderMatrix.setScale(sx, 1f)
            cache.shaderMatrix.postTranslate(cx, cy)
            botWideShader.setLocalMatrix(cache.shaderMatrix)
        }
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.xfermode = cache.xferAdd
        p.shader = botWideShader

        // Broad soft bottom cradle glow
        p.strokeWidth = (radius * 0.30f).coerceAtLeast(8f)
        p.alpha = (80f * bloomBoost).toInt().coerceIn(0, 145)
        nc.drawArc(cache.arcRect, 35f, 110f, false, p)

        // Medium bottom cradle glow
        p.strokeWidth = (radius * 0.16f).coerceAtLeast(4.5f)
        p.alpha = (135f * bloomBoost).toInt().coerceIn(0, 200)
        nc.drawArc(cache.arcRect, 38f, 104f, false, p)

        // Tighter curved lavender-white bottom arc fusing diagonal silk into the rim
        cache.arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius)
        val botTightShader = cache.unitBottomArcTightShader
        if (botTightShader != null) {
            val sx = (radius * 0.80f).coerceAtLeast(1f)
            cache.shaderMatrix.reset()
            cache.shaderMatrix.setScale(sx, 1f)
            cache.shaderMatrix.postTranslate(cx, cy)
            botTightShader.setLocalMatrix(cache.shaderMatrix)
        }
        p.xfermode = cache.xferScreen
        p.shader = botTightShader
        p.strokeWidth = (radius * 0.085f).coerceAtLeast(2.6f)
        p.alpha = 235
        nc.drawArc(cache.arcRect, 42f, 96f, false, p)

        // Subtle 12 o'clock curved top specular arc
        val topArcShader = cache.unitTopArcShader
        if (topArcShader != null) {
            val sx = (radius * 0.65f).coerceAtLeast(1f)
            cache.shaderMatrix.reset()
            cache.shaderMatrix.setScale(sx, 1f)
            cache.shaderMatrix.postTranslate(cx, cy)
            topArcShader.setLocalMatrix(cache.shaderMatrix)
        }
        p.shader = topArcShader
        p.strokeWidth = (radius * 0.075f).coerceAtLeast(2.4f)
        p.alpha = 185
        nc.drawArc(cache.arcRect, 232f, 76f, false, p)

        // 5. Tight Vibrant Electric-Violet Neon Halo Hugging the Hairline (GPU dual-pass)
        val rimShader = cache.unitOrbVerticalRimShader
        if (rimShader != null) {
            val r = radius.coerceAtLeast(1f)
            cache.shaderMatrix.reset()
            cache.shaderMatrix.setScale(1f, r)
            cache.shaderMatrix.postTranslate(cx, cy)
            rimShader.setLocalMatrix(cache.shaderMatrix)
        }
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.xfermode = cache.xferAdd
        p.shader = rimShader
        p.strokeWidth = (radius * 0.12f).coerceAtLeast(3.4f)
        p.alpha = (95f * bloomBoost).toInt().coerceIn(0, 155)
        nc.drawCircle(cx, cy, radius, p)

        p.strokeWidth = (radius * 0.062f).coerceAtLeast(2.0f)
        p.alpha = (155f * bloomBoost).toInt().coerceIn(0, 220)
        nc.drawCircle(cx, cy, radius, p)

        // 6. RAZOR-THIN (1.4-1.6dp), ZERO-BLUR High-Contrast White-Lavender Hairline
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.strokeWidth = (radius * 0.024f).coerceAtLeast(1.3f)
        p.shader = rimShader
        p.alpha = 255
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
