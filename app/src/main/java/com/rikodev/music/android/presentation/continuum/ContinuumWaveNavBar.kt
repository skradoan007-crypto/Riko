package com.rikodev.music.android.presentation.continuum

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.animation.LocalRikoClock
import com.rikodev.music.android.core.designsystem.riko.components.RikoHapticEvent
import com.rikodev.music.android.core.designsystem.riko.components.rememberRikoHaptic
import com.rikodev.music.android.core.designsystem.riko.theme.AccentMagenta
import com.rikodev.music.android.core.designsystem.riko.theme.AccentViolet
import com.rikodev.music.android.core.designsystem.riko.theme.RikoBlue
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextSecondary
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// RIKO CONTINUUM CORE — CONTINUOUS ORGANIC LIQUID NAVIGATION SURFACE (P16)
//
// Exact visual & interaction fidelity to the CONTINUUM concept board:
//   - NOT a straight pill or flat rectangle with lines inside.
//   - The ENTIRE outer dock silhouette is a continuous living liquid ribbon
//     whose upper & lower boundaries organically pinch between portals and
//     swell seamlessly around all 3 portals (Listen / Explore / Library).
//   - The active destination swells upward & outward as an integrated elevated
//     circular liquid portal; inactive portals sit subtly recessed inside the
//     liquid body.
//   - Multi-layered translucent indigo/violet/blue flowing liquid veils +
//     feathered organic contour highlights (zero harsh thin sine-wave gimmicks).
//   - Full 5-stage interaction storyboard:
//       1. Idle (calm living flow)
//       2. Touch / Hold (core awakens)
//       3. Slide-to-Switch (live fluid portal preview under finger; vertical
//          scroll-safe, direct tap always supported)
//       4. World Morph (old peak relaxes, liquid volume flows, new peak rises)
//       5. Arrived (elastic spring settle)
// ─────────────────────────────────────────────────────────────────────────────

val ContinuumWaveBarHeight = 84.dp

@Composable
fun ContinuumWaveNavBar(
    activeSection: ContinuumSection,
    onSectionSelected: (ContinuumSection) -> Unit,
    renderProfile: EffectiveRenderProfile,
    primaryAccent: Color = AccentViolet,
    secondaryAccent: Color = AccentMagenta,
    audioEnergy: Float = 0f,
    modifier: Modifier = Modifier
) {
    val haptic = rememberRikoHaptic()
    val liveClock = LocalRikoClock.current
    val currentOnSelect by rememberUpdatedState(onSectionSelected)
    val currentActive by rememberUpdatedState(activeSection)

    val targetFraction = activeSection.tabIndex / 2f
    val focusAnim = remember { Animatable(targetFraction) }
    var isTouched by remember { mutableStateOf(false) }
    var isSliding by remember { mutableStateOf(false) }
    var slideFraction by remember { mutableFloatStateOf(targetFraction) }
    var previewSection by remember { mutableStateOf<ContinuumSection?>(null) }

    LaunchedEffect(targetFraction, isSliding, renderProfile.reduceMotion) {
        if (!isSliding) {
            if (renderProfile.reduceMotion) {
                focusAnim.snapTo(targetFraction)
            } else {
                focusAnim.animateTo(
                    targetValue = targetFraction,
                    animationSpec = spring(
                        dampingRatio = 0.68f,
                        stiffness = 280f
                    )
                )
            }
        }
    }

    val displayedSection = previewSection ?: activeSection

    // Independent spring elevations per portal so old peak relaxes while new peak rises
    val elev0 by animateFloatAsState(
        targetValue = if (displayedSection == ContinuumSection.LISTEN) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 280f),
        label = "portalElev0"
    )
    val elev1 by animateFloatAsState(
        targetValue = if (displayedSection == ContinuumSection.EXPLORE) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 280f),
        label = "portalElev1"
    )
    val elev2 by animateFloatAsState(
        targetValue = if (displayedSection == ContinuumSection.LIBRARY) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 280f),
        label = "portalElev2"
    )

    val touchAwaken by animateFloatAsState(
        targetValue = if (isTouched || isSliding) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.76f, stiffness = 360f),
        label = "continuumAwaken"
    )

    // Deep indigo / violet / purple-blue glass atmosphere matching reference board
    val deepViolet = Color(0xFF7C3AED)
    val luminousLavender = Color(0xFFA78BFA)
    val softIndigoBlue = Color(0xFF4F46E5)
    val primaryGlow = if (renderProfile.useDynamicArtworkColors) {
        androidx.compose.ui.graphics.lerp(deepViolet, primaryAccent, 0.22f)
    } else deepViolet
    val secondaryGlow = if (renderProfile.useDynamicArtworkColors) {
        androidx.compose.ui.graphics.lerp(softIndigoBlue, secondaryAccent, 0.20f)
    } else RikoBlue

    // Reusable zero-allocation paths for all organic liquid layers
    val mainBodyPath = remember { Path() }
    val upperRimPath = remember { Path() }
    val veilLayer1Path = remember { Path() }
    val veilLayer2Path = remember { Path() }
    val silkHighlight1 = remember { Path() }
    val silkHighlight2 = remember { Path() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .height(ContinuumWaveBarHeight)
            // Touch/Hold Awaken + horizontal Slide-to-Switch (scroll-safe: ignores vertical drags)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isTouched = true
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    val startX = down.position.x
                    val startY = down.position.y
                    var horizontalLocked = false
                    var cancelledByVertical = false
                    var lastPreviewIdx = currentActive.tabIndex

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            if (horizontalLocked) {
                                change.consume()
                                val finalIdx = (slideFraction * 2f).roundToInt().coerceIn(0, 2)
                                val chosen = ContinuumSection.fromIndex(finalIdx)
                                isSliding = false
                                previewSection = null
                                if (chosen != currentActive) {
                                    haptic.fire(RikoHapticEvent.CONFIRMATION)
                                    currentOnSelect(chosen)
                                }
                            }
                            break
                        }
                        val dx = abs(change.position.x - startX)
                        val dy = abs(change.position.y - startY)
                        if (!horizontalLocked && !cancelledByVertical) {
                            if (dy > 16f && dy > dx * 1.15f) {
                                // Yield immediately to vertical list scroll
                                cancelledByVertical = true
                                isTouched = false
                            } else if (dx > 14f && dx > dy * 1.2f) {
                                horizontalLocked = true
                                isSliding = true
                            }
                        }
                        if (horizontalLocked) {
                            change.consume()
                            // Map finger X across portal centers [0.16..0.84] -> [0..1]
                            val rawFrac = ((change.position.x / w - 0.16f) / 0.68f).coerceIn(0f, 1f)
                            slideFraction = rawFrac
                            val hoverIdx = (rawFrac * 2f).roundToInt().coerceIn(0, 2)
                            if (hoverIdx != lastPreviewIdx) {
                                lastPreviewIdx = hoverIdx
                                haptic.fire(RikoHapticEvent.CONFIRMATION)
                                previewSection = ContinuumSection.fromIndex(hoverIdx)
                            }
                        }
                    }
                    isSliding = false
                    isTouched = false
                    previewSection = null
                }
            }
            .drawWithCache {
                val samples = 64

                onDrawBehind {
                    val w = size.width
                    val h = size.height
                    if (w <= 0f || h <= 0f) return@onDrawBehind

                    val focus = if (isSliding) slideFraction else focusAnim.value
                    val energy = if (renderProfile.useMdnaReactivity) audioEnergy.coerceIn(0f, 1f) else 0f
                    val time = liveClock.value
                    val reduceMotion = renderProfile.reduceMotion

                    val x0 = w * 0.165f
                    val x1 = w * 0.500f
                    val x2 = w * 0.835f
                    val focusX = x0 + (x2 - x0) * focus
                    val midY = h * 0.54f

                    // Organic liquid boundary evaluation:
                    // Pinches smoothly into a slender waist between portals, swells around
                    // recessed portals, and rises prominently around the active liquid portal!
                    val baseHalfH = 13.5.dp.toPx()
                    val recessedAdd = 11.5.dp.toPx()
                    val activeTopAdd = 24.5.dp.toPx() + (2.5.dp.toPx() * touchAwaken)
                    val activeBotAdd = 19.5.dp.toPx() + (1.5.dp.toPx() * touchAwaken)
                    val portalSigma = w * 0.125f
                    val activeSigma = w * 0.145f
                    val bridgeSigma = w * 0.16f

                    fun gaussian(x: Float, center: Float, sigma: Float): Float {
                        val d = (x - center) / sigma.coerceAtLeast(1f)
                        return exp(-d * d)
                    }

                    fun topContour(x: Float, phaseShift: Float = 0f, scale: Float = 1f): Float {
                        val g0 = gaussian(x, x0, portalSigma)
                        val g1 = gaussian(x, x1, portalSigma)
                        val g2 = gaussian(x, x2, portalSigma)
                        val gActive = gaussian(x, focusX, activeSigma)
                        val travelWave = if (reduceMotion) 0f else {
                            sin((x / w) * PI.toFloat() * 3.0f + time * 0.9f + phaseShift) *
                                (1.8.dp.toPx() + 1.5.dp.toPx() * energy)
                        }
                        val portalLift =
                            g0 * (recessedAdd + (activeTopAdd - recessedAdd) * elev0) +
                                g1 * (recessedAdd + (activeTopAdd - recessedAdd) * elev1) +
                                g2 * (recessedAdd + (activeTopAdd - recessedAdd) * elev2)
                        val activeCarry = gActive * (4.dp.toPx() + 3.dp.toPx() * touchAwaken)
                        return (midY - (baseHalfH + portalLift * scale + activeCarry) + travelWave)
                            .coerceIn(3.dp.toPx(), midY - 6.dp.toPx())
                    }

                    fun bottomContour(x: Float, phaseShift: Float = 0f, scale: Float = 1f): Float {
                        val g0 = gaussian(x, x0, portalSigma)
                        val g1 = gaussian(x, x1, portalSigma)
                        val g2 = gaussian(x, x2, portalSigma)
                        val gActive = gaussian(x, focusX, bridgeSigma)
                        val travelWave = if (reduceMotion) 0f else {
                            sin((x / w) * PI.toFloat() * 2.5f - time * 0.8f + phaseShift) *
                                (1.4.dp.toPx() + 1.2.dp.toPx() * energy)
                        }
                        val portalDrop =
                            g0 * (recessedAdd + (activeBotAdd - recessedAdd) * elev0) +
                                g1 * (recessedAdd + (activeBotAdd - recessedAdd) * elev1) +
                                g2 * (recessedAdd + (activeBotAdd - recessedAdd) * elev2)
                        val activeCarry = gActive * (2.5.dp.toPx() + 2.dp.toPx() * touchAwaken)
                        return (midY + (baseHalfH + portalDrop * scale + activeCarry) + travelWave)
                            .coerceIn(midY + 6.dp.toPx(), h - 2.dp.toPx())
                    }

                    fun buildOrganicRibbon(
                        outPath: Path,
                        outTopRim: Path?,
                        phaseShift: Float,
                        scale: Float,
                        insetX: Float
                    ) {
                        outPath.reset()
                        outTopRim?.reset()
                        val leftX = insetX
                        val rightX = w - insetX
                        val capR = 16.dp.toPx()
                        val innerL = leftX + capR
                        val innerR = rightX - capR

                        // Top curve left -> right
                        val startTopY = topContour(innerL, phaseShift, scale)
                        outPath.moveTo(innerL, startTopY)
                        outTopRim?.moveTo(innerL, startTopY)
                        for (i in 1..samples) {
                            val t = i / samples.toFloat()
                            val x = innerL + (innerR - innerL) * t
                            val y = topContour(x, phaseShift, scale)
                            outPath.lineTo(x, y)
                            outTopRim?.lineTo(x, y)
                        }

                        // Smooth organic right end cap wrapping around the right portal
                        val endTopY = topContour(innerR, phaseShift, scale)
                        val endBotY = bottomContour(innerR, phaseShift, scale)
                        outPath.cubicTo(
                            rightX + 4.dp.toPx(), endTopY + 4.dp.toPx(),
                            rightX + 4.dp.toPx(), endBotY - 4.dp.toPx(),
                            innerR, endBotY
                        )

                        // Bottom curve right -> left
                        for (i in (samples - 1) downTo 0) {
                            val t = i / samples.toFloat()
                            val x = innerL + (innerR - innerL) * t
                            val y = bottomContour(x, phaseShift, scale)
                            outPath.lineTo(x, y)
                        }

                        // Smooth organic left end cap wrapping around the left portal
                        val startBotY = bottomContour(innerL, phaseShift, scale)
                        outPath.cubicTo(
                            leftX - 4.dp.toPx(), startBotY - 4.dp.toPx(),
                            leftX - 4.dp.toPx(), startTopY + 4.dp.toPx(),
                            innerL, startTopY
                        )
                        outPath.close()
                    }

                    // Build primary organic liquid dock silhouette + top Fresnel rim
                    buildOrganicRibbon(
                        outPath = mainBodyPath,
                        outTopRim = upperRimPath,
                        phaseShift = 0f,
                        scale = 1.0f,
                        insetX = 6.dp.toPx()
                    )

                    // ── LAYER 1: Controlled soft outer aurora underglow beneath active portal ──
                    if (renderProfile.selectedTier != VisualQualityTier.LITE) {
                        val glowR = 56.dp.toPx() * (1f + 0.12f * touchAwaken + 0.08f * energy)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    primaryGlow.copy(alpha = 0.56f + 0.16f * touchAwaken),
                                    secondaryGlow.copy(alpha = 0.24f),
                                    Color.Transparent
                                ),
                                center = Offset(focusX, midY),
                                radius = glowR
                            ),
                            radius = glowR,
                            center = Offset(focusX, midY)
                        )
                    }

                    // ── LAYER 2: Main Translucent Deep Indigo-Violet Liquid Glass Body ──
                    drawPath(
                        path = mainBodyPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xC41C163A),
                                Color(0xD80E0B22),
                                Color(0xCC161132)
                            ),
                            startY = 0f,
                            endY = h
                        )
                    )

                    // ── LAYER 3: Multi-Layered Flowing Translucent Veils & Silk Contours (Clipped Inside) ──
                    clipPath(mainBodyPath) {
                        // Internal focal aurora depth pool inside the active liquid swell
                        val innerPoolR = 72.dp.toPx()
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    luminousLavender.copy(alpha = 0.34f + 0.12f * touchAwaken),
                                    primaryGlow.copy(alpha = 0.42f),
                                    secondaryGlow.copy(alpha = 0.18f),
                                    Color.Transparent
                                ),
                                center = Offset(focusX, midY),
                                radius = innerPoolR
                            ),
                            radius = innerPoolR,
                            center = Offset(focusX, midY)
                        )

                        if (renderProfile.useOrganicBezier) {
                            // Flowing translucent veil 1 (phase-offset organic inner ribbon)
                            buildOrganicRibbon(
                                outPath = veilLayer1Path,
                                outTopRim = null,
                                phaseShift = 1.6f,
                                scale = 0.78f,
                                insetX = 12.dp.toPx()
                            )
                            drawPath(
                                path = veilLayer1Path,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        secondaryGlow.copy(alpha = 0.16f),
                                        primaryGlow.copy(alpha = 0.30f),
                                        luminousLavender.copy(alpha = 0.28f),
                                        secondaryGlow.copy(alpha = 0.16f)
                                    )
                                )
                            )

                            // Flowing translucent veil 2 (counter-flowing deeper wave)
                            buildOrganicRibbon(
                                outPath = veilLayer2Path,
                                outTopRim = null,
                                phaseShift = 3.4f,
                                scale = 0.56f,
                                insetX = 18.dp.toPx()
                            )
                            drawPath(
                                path = veilLayer2Path,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFF60A5FA).copy(alpha = 0.22f),
                                        Color.White.copy(alpha = 0.18f),
                                        primaryGlow.copy(alpha = 0.24f),
                                        Color.Transparent
                                    )
                                )
                            )

                            // Soft feathered flowing highlight curves hugging the pinched waists
                            silkHighlight1.reset()
                            silkHighlight2.reset()
                            for (i in 0..samples) {
                                val u = i / samples.toFloat()
                                val x = w * (0.08f + 0.84f * u)
                                val env = 0.35f + 0.65f * gaussian(x, focusX, w * 0.30f)
                                val y1 = midY - 6.dp.toPx() +
                                    sin(u * PI.toFloat() * 2.2f + time * 1.0f) * 8.dp.toPx() * env
                                val y2 = midY + 7.dp.toPx() +
                                    sin(u * PI.toFloat() * 2.6f - time * 0.85f + 2.0f) * 7.dp.toPx() * env
                                if (i == 0) {
                                    silkHighlight1.moveTo(x, y1)
                                    silkHighlight2.moveTo(x, y2)
                                } else {
                                    silkHighlight1.lineTo(x, y1)
                                    silkHighlight2.lineTo(x, y2)
                                }
                            }
                            drawPath(
                                path = silkHighlight1,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        luminousLavender.copy(alpha = 0.42f + 0.20f * touchAwaken),
                                        Color.White.copy(alpha = 0.50f),
                                        Color(0xFF93C5FD).copy(alpha = 0.38f),
                                        Color.Transparent
                                    )
                                ),
                                style = Stroke(width = 2.0.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                            drawPath(
                                path = silkHighlight2,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        primaryGlow.copy(alpha = 0.38f),
                                        luminousLavender.copy(alpha = 0.48f),
                                        Color.Transparent
                                    )
                                ),
                                style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }

                        // Subtle curved optical Fresnel specular band along the upper liquid surface
                        drawRect(
                            brush = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.00f to Color.White.copy(alpha = 0.20f),
                                    0.18f to Color.White.copy(alpha = 0.05f),
                                    0.45f to Color.Transparent,
                                    0.86f to Color.Transparent,
                                    1.00f to Color.White.copy(alpha = 0.08f)
                                ),
                                startY = 0f,
                                endY = h
                            )
                        )
                    }

                    // ── LAYER 4: Crisp Organic Liquid Glass Outer Rim + Top Specular Catchlight ──
                    drawPath(
                        path = mainBodyPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                luminousLavender.copy(alpha = 0.52f + 0.16f * touchAwaken),
                                primaryGlow.copy(alpha = 0.30f),
                                Color.White.copy(alpha = 0.18f)
                            ),
                            startY = 0f,
                            endY = h
                        ),
                        style = Stroke(width = 1.25.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = upperRimPath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.15f),
                                luminousLavender.copy(alpha = 0.65f),
                                Color.White.copy(alpha = 0.85f),
                                luminousLavender.copy(alpha = 0.65f),
                                Color.White.copy(alpha = 0.15f)
                            )
                        ),
                        style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
    ) {
        // 3 balanced portals aligned precisely with x0 (0.165), x1 (0.50), x2 (0.835)
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContinuumSection.ordered.forEach { section ->
                val isSelected = section == displayedSection
                ContinuumLiquidPortalItem(
                    section = section,
                    isSelected = isSelected,
                    reduceMotion = renderProfile.reduceMotion,
                    primaryGlow = primaryGlow,
                    secondaryGlow = secondaryGlow,
                    onClick = {
                        haptic.fire(RikoHapticEvent.CONFIRMATION)
                        onSectionSelected(section)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ContinuumLiquidPortalItem(
    section: ContinuumSection,
    isSelected: Boolean,
    reduceMotion: Boolean,
    primaryGlow: Color,
    secondaryGlow: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else RikoTextSecondary.copy(alpha = 0.84f),
        label = "portalIconTint"
    )
    // Active portal elevates smoothly upward inside the liquid swell; inactive stays subtly recessed
    val portalSize by animateDpAsState(
        targetValue = if (isSelected) 60.dp else 44.dp,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 280f),
        label = "portalSize"
    )
    val liftY by animateDpAsState(
        targetValue = if (isSelected && !reduceMotion) (-2).dp else 2.dp,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 300f),
        label = "portalLift"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected && !reduceMotion) 1.15f else 0.94f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 340f),
        label = "portalIconScale"
    )

    Box(
        modifier = modifier
            .height(ContinuumWaveBarHeight)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .semantics {
                selected = isSelected
                contentDescription = "${section.label} navigation portal"
            },
        contentAlignment = Alignment.Center
    ) {
        // Integrated Circular Liquid Portal (elevated luminous glass when active; subtly recessed when inactive)
        Box(
            modifier = Modifier
                .offset(y = liftY)
                .size(portalSize)
                .clip(CircleShape)
                .background(
                    if (isSelected) {
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFE9D5FF).copy(alpha = 0.34f),
                                primaryGlow.copy(alpha = 0.78f),
                                secondaryGlow.copy(alpha = 0.56f),
                                Color(0xCC181038)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x55181434),
                                Color(0x880B081B)
                            )
                        )
                    }
                )
                .border(
                    width = if (isSelected) 1.6.dp else 0.9.dp,
                    brush = Brush.verticalGradient(
                        colors = if (isSelected) {
                            listOf(
                                Color.White.copy(alpha = 0.94f),
                                Color(0xFFC4B5FD).copy(alpha = 0.70f),
                                primaryGlow.copy(alpha = 0.45f)
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = 0.24f),
                                Color.White.copy(alpha = 0.06f)
                            )
                        }
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            ) {
                when (section) {
                    ContinuumSection.LISTEN -> drawHeadphonesIcon(iconColor, isSelected)
                    ContinuumSection.EXPLORE -> drawOrbitalPlanetIcon(iconColor, isSelected)
                    ContinuumSection.LIBRARY -> drawBooksIcon(iconColor, isSelected)
                }
            }
        }
    }
}

private fun DrawScope.drawHeadphonesIcon(color: Color, active: Boolean) {
    val w = size.width
    val h = size.height
    val stroke = Stroke(width = 2.1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawArc(
        color = color,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(w * 0.14f, h * 0.14f),
        size = Size(w * 0.72f, h * 0.70f),
        style = stroke
    )
    val cupW = w * 0.19f
    val cupH = h * 0.34f
    val cupTop = h * 0.48f
    drawRoundRect(
        color = color,
        topLeft = Offset(w * 0.12f, cupTop),
        size = Size(cupW, cupH),
        cornerRadius = CornerRadius(cupW * 0.45f, cupW * 0.45f),
        style = if (active) Fill else stroke
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(w * 0.69f, cupTop),
        size = Size(cupW, cupH),
        cornerRadius = CornerRadius(cupW * 0.45f, cupW * 0.45f),
        style = if (active) Fill else stroke
    )
}

private fun DrawScope.drawOrbitalPlanetIcon(color: Color, active: Boolean) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val cy = h * 0.52f
    val stroke = Stroke(width = 2.0.dp.toPx(), cap = StrokeCap.Round)
    drawCircle(
        color = color,
        radius = w * 0.27f,
        center = Offset(cx, cy),
        style = if (active) Fill else stroke
    )
    rotate(degrees = -22f, pivot = Offset(cx, cy)) {
        drawOval(
            color = color,
            topLeft = Offset(w * 0.04f, cy - h * 0.13f),
            size = Size(w * 0.92f, h * 0.26f),
            style = stroke
        )
    }
}

private fun DrawScope.drawBooksIcon(color: Color, active: Boolean) {
    val w = size.width
    val h = size.height
    val stroke = Stroke(width = 1.9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    val style = if (active) Fill else stroke
    drawRoundRect(
        color = color,
        topLeft = Offset(w * 0.14f, h * 0.18f),
        size = Size(w * 0.19f, h * 0.64f),
        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
        style = style
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(w * 0.39f, h * 0.24f),
        size = Size(w * 0.19f, h * 0.58f),
        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
        style = style
    )
    rotate(degrees = -14f, pivot = Offset(w * 0.76f, h * 0.80f)) {
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.66f, h * 0.22f),
            size = Size(w * 0.19f, h * 0.60f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = style
        )
    }
}
