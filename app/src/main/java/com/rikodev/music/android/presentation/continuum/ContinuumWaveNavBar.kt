package com.rikodev.music.android.presentation.continuum

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
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
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// RIKO CONTINUUM CORE — 5-LAYER OPTICAL LIQUID NAVIGATION BAR (P17)
//
// Exact 5-Layer Architecture matching the CONTINUUM Logicboard & Reference:
//   Layer 1 : Soft dark atmospheric underlay fade (zero hard rectangular edge)
//   Layer 2 : Borderless soft-curved frosted glassmorphism dome base
//   Layer 3 : Sweeping figure-8 / X-crossing luminous neon fiber mesh ribbons
//             wrapping over inactive orbs and cradling beneath the active orb
//   Layer 4 : Radiant elevated active orb (multi-pass violet bloom + crisp
//             neon-white circle border + 12 & 6 o'clock optical lens flares +
//             expanding ripple wave on selection) & frosted inactive orbs
//   Layer 5 : Crisp foreground icons (Headphones, Orbital Planet, Books)
// ─────────────────────────────────────────────────────────────────────────────

val ContinuumWaveBarHeight = 90.dp

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
    val selectionRipple = remember { Animatable(1f) }
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

    // State C: Expanding audio/liquid ripple rings on every active selection change
    LaunchedEffect(activeSection, renderProfile.reduceMotion) {
        if (!renderProfile.reduceMotion) {
            selectionRipple.snapTo(0f)
            selectionRipple.animateTo(1f, animationSpec = tween(durationMillis = 520))
        }
    }

    val displayedSection = previewSection ?: activeSection

    val elev0 by animateFloatAsState(
        targetValue = if (displayedSection == ContinuumSection.LISTEN) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 280f),
        label = "orbElev0"
    )
    val elev1 by animateFloatAsState(
        targetValue = if (displayedSection == ContinuumSection.EXPLORE) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 280f),
        label = "orbElev1"
    )
    val elev2 by animateFloatAsState(
        targetValue = if (displayedSection == ContinuumSection.LIBRARY) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 280f),
        label = "orbElev2"
    )

    val touchAwaken by animateFloatAsState(
        targetValue = if (isTouched || isSliding) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.74f, stiffness = 360f),
        label = "orbTouchAwaken"
    )

    // Deep indigo / luminous electric violet-lavender palette matching 1000394279.jpg
    val neonViolet = Color(0xFF8B5CF6)
    val electricLavender = Color(0xFFC4B5FD)
    val deepIndigo = Color(0xFF4F46E5)
    val primaryGlow = if (renderProfile.useDynamicArtworkColors) {
        androidx.compose.ui.graphics.lerp(neonViolet, primaryAccent, 0.18f)
    } else neonViolet
    val secondaryGlow = if (renderProfile.useDynamicArtworkColors) {
        androidx.compose.ui.graphics.lerp(deepIndigo, secondaryAccent, 0.16f)
    } else RikoBlue

    // Reusable zero-allocation paths for all 5 layers
    val glassDomePath = remember { Path() }
    val ribbonFillPath = remember { Path() }
    val fiberPaths = remember { List(7) { Path() } }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(ContinuumWaveBarHeight)
            // Touch/Hold Awaken + horizontal Slide-to-Switch (scroll-safe)
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
                                cancelledByVertical = true
                                isTouched = false
                            } else if (dx > 14f && dx > dy * 1.2f) {
                                horizontalLocked = true
                                isSliding = true
                            }
                        }
                        if (horizontalLocked) {
                            change.consume()
                            val rawFrac = ((change.position.x / w - 1f / 6f) / (4f / 6f)).coerceIn(0f, 1f)
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
                    val pulse = if (reduceMotion) 0f else (sin(time * 1.6f) * 0.5f + 0.5f) * 0.14f

                    val x0 = w * (1f / 6f)
                    val x1 = w * 0.500f
                    val x2 = w * (5f / 6f)
                    val orbXs = floatArrayOf(x0, x1, x2)
                    val orbElevs = floatArrayOf(elev0, elev1, elev2)
                    val focusX = x0 + (x2 - x0) * focus
                    val midY = h * 0.52f

                    fun gaussian(x: Float, center: Float, sigma: Float): Float {
                        val d = (x - center) / sigma.coerceAtLeast(1f)
                        return exp(-d * d)
                    }

                    // ── LAYER 1: SOFT DARK ATMOSPHERIC UNDERLAY (NO HARD BOX EDGE) ──
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Transparent,
                                0.24f to Color(0x880B091D),
                                0.65f to Color(0xDC0B091D),
                                1.00f to Color(0xF2080616)
                            ),
                            startY = 0f,
                            endY = h
                        )
                    )

                    // ── LAYER 2: BORDERLESS SOFT-CURVED GLASSMORPHISM BASE DOME ──
                    glassDomePath.reset()
                    glassDomePath.moveTo(0f, h)
                    for (i in 0..samples) {
                        val u = i / samples.toFloat()
                        val x = u * w
                        val arch = sin(u * PI.toFloat())
                        val focusLift = gaussian(x, focusX, w * 0.24f) * 10.dp.toPx()
                        val y = h * 0.26f - arch * 8.dp.toPx() - focusLift
                        glassDomePath.lineTo(x, y.coerceAtLeast(4.dp.toPx()))
                    }
                    glassDomePath.lineTo(w, h)
                    glassDomePath.close()

                    drawPath(
                        path = glassDomePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0x52251E4A),
                                Color(0x8814102D),
                                Color(0xAA0C091E)
                            ),
                            startY = 0f,
                            endY = h
                        )
                    )

                    // ── LAYER 3: SWEEPING FIGURE-8 / X-CROSSING LUMINOUS FIBER MESH RIBBONS ──
                    // Upper & lower sweeping curves wrap OVER the inactive orbs and dip cleanly
                    // INTO a luminous V-cradle beneath the active orb (matching 1000394279.jpg!)
                    val inactiveArch = 24.dp.toPx()
                    val activeCradleDrop = 28.dp.toPx() + 3.dp.toPx() * touchAwaken
                    val orbSig = w * 0.145f
                    val activeSig = w * 0.175f

                    fun ribbonCurveA(x: Float, offsetScale: Float): Float {
                        // Sweeps OVER inactive orbs, dips UNDER active orb (creates the iconic X-crossing!)
                        val g0 = gaussian(x, x0, orbSig) * (1f - elev0 * 0.85f)
                        val g1 = gaussian(x, x1, orbSig) * (1f - elev1 * 0.85f)
                        val g2 = gaussian(x, x2, orbSig) * (1f - elev2 * 0.85f)
                        val gAct = gaussian(x, focusX, activeSig)
                        val drift = if (reduceMotion) 0f else sin((x / w) * PI.toFloat() * 2.5f + time * 1.1f) * 2.2.dp.toPx()
                        return midY - (g0 + g1 + g2) * inactiveArch * offsetScale +
                            gAct * activeCradleDrop * offsetScale + drift
                    }

                    fun ribbonCurveB(x: Float, offsetScale: Float): Float {
                        // Mirrored curve: sweeps UNDER inactive orbs, rises toward active orb's flanks
                        val g0 = gaussian(x, x0, orbSig) * (1f - elev0 * 0.85f)
                        val g1 = gaussian(x, x1, orbSig) * (1f - elev1 * 0.85f)
                        val g2 = gaussian(x, x2, orbSig) * (1f - elev2 * 0.85f)
                        val gAct = gaussian(x, focusX, activeSig)
                        val drift = if (reduceMotion) 0f else cos((x / w) * PI.toFloat() * 2.2f - time * 0.95f) * 2.0.dp.toPx()
                        return midY + (g0 + g1 + g2) * (inactiveArch * 0.92f) * offsetScale -
                            gAct * (activeCradleDrop * 0.48f) * offsetScale + drift
                    }

                    // Translucent glowing liquid ribbon body between Curve A & Curve B
                    ribbonFillPath.reset()
                    for (i in 0..samples) {
                        val x = (i / samples.toFloat()) * w
                        val y = ribbonCurveA(x, 1.0f)
                        if (i == 0) ribbonFillPath.moveTo(x, y) else ribbonFillPath.lineTo(x, y)
                    }
                    for (i in samples downTo 0) {
                        val x = (i / samples.toFloat()) * w
                        ribbonFillPath.lineTo(x, ribbonCurveB(x, 1.0f))
                    }
                    ribbonFillPath.close()

                    drawPath(
                        path = ribbonFillPath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                secondaryGlow.copy(alpha = 0.14f),
                                primaryGlow.copy(alpha = 0.34f + 0.12f * touchAwaken),
                                electricLavender.copy(alpha = 0.42f + 0.12f * touchAwaken),
                                primaryGlow.copy(alpha = 0.34f + 0.12f * touchAwaken),
                                secondaryGlow.copy(alpha = 0.14f)
                            )
                        )
                    )

                    // Multi-strand holographic optical fiber mesh (7 interpolated translucent silk curves)
                    val fiberCount = if (renderProfile.selectedTier == VisualQualityTier.LITE) 2 else 7
                    for (fIdx in 0 until fiberCount) {
                        val frac = if (fiberCount == 1) 0f else fIdx / (fiberCount - 1f)
                        val p = fiberPaths[fIdx]
                        p.reset()
                        for (i in 0..samples) {
                            val x = (i / samples.toFloat()) * w
                            val yA = ribbonCurveA(x, 0.92f + 0.12f * frac)
                            val yB = ribbonCurveB(x, 0.92f + 0.12f * (1f - frac))
                            val y = yA + (yB - yA) * frac
                            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                        }
                        val isEdge = fIdx == 0 || fIdx == fiberCount - 1
                        val strokeW = if (isEdge) 1.7.dp.toPx() else 0.85.dp.toPx()
                        val alphaBase = if (isEdge) 0.72f else 0.34f
                        drawPath(
                            path = p,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.08f),
                                    electricLavender.copy(alpha = alphaBase * (0.85f + 0.25f * touchAwaken)),
                                    Color.White.copy(alpha = if (isEdge) 0.92f else 0.55f),
                                    electricLavender.copy(alpha = alphaBase * (0.85f + 0.25f * touchAwaken)),
                                    Color.White.copy(alpha = 0.08f)
                                )
                            ),
                            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }

                    // ── LAYER 4: INACTIVE FROSTED ORBS + RADIANT ACTIVE ORB WITH BLOOM & LENS FLARES ──
                    // State C: Expanding liquid ripple rings on selection change
                    val rippleT = selectionRipple.value
                    if (rippleT < 0.98f && !reduceMotion) {
                        val rippleAlpha = (1f - rippleT) * 0.52f
                        val r1 = 30.dp.toPx() + 44.dp.toPx() * rippleT
                        val r2 = 24.dp.toPx() + 30.dp.toPx() * rippleT
                        drawCircle(
                            color = electricLavender.copy(alpha = rippleAlpha),
                            radius = r1,
                            center = Offset(focusX, midY),
                            style = Stroke(width = 1.6.dp.toPx())
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = rippleAlpha * 0.7f),
                            radius = r2,
                            center = Offset(focusX, midY),
                            style = Stroke(width = 1.1.dp.toPx())
                        )
                    }

                    // Draw inactive frosted glass orbs (soft feathered translucent spheres)
                    for (idx in 0..2) {
                        val elev = orbElevs[idx]
                        if (elev < 0.95f) {
                            val cx = orbXs[idx]
                            val r = 23.dp.toPx() + 8.dp.toPx() * elev
                            val alpha = (1f - elev).coerceIn(0f, 1f)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0x66302660).copy(alpha = 0.45f * alpha),
                                        Color(0x55181336).copy(alpha = 0.52f * alpha),
                                        Color.Transparent
                                    ),
                                    center = Offset(cx, midY),
                                    radius = r * 1.15f
                                ),
                                radius = r * 1.15f,
                                center = Offset(cx, midY)
                            )
                            drawCircle(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        electricLavender.copy(alpha = 0.55f * alpha),
                                        Color.White.copy(alpha = 0.14f * alpha),
                                        electricLavender.copy(alpha = 0.25f * alpha)
                                    ),
                                    startY = midY - r,
                                    endY = midY + r
                                ),
                                radius = r,
                                center = Offset(cx, midY),
                                style = Stroke(width = 1.1.dp.toPx())
                            )
                        }
                    }

                    // Radiant Active Orb (multi-pass neon violet bloom + crisp white-neon circle + 12/6 o'clock lens flares)
                    val activeOrbR = 32.dp.toPx() + 2.dp.toPx() * touchAwaken
                    // Pass 4A: Wide atmospheric violet bloom
                    val bloomOuterR = 62.dp.toPx() * (1f + pulse + 0.14f * touchAwaken + 0.10f * energy)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryGlow.copy(alpha = 0.65f + 0.18f * touchAwaken),
                                secondaryGlow.copy(alpha = 0.28f),
                                Color.Transparent
                            ),
                            center = Offset(focusX, midY),
                            radius = bloomOuterR
                        ),
                        radius = bloomOuterR,
                        center = Offset(focusX, midY)
                    )

                    // Pass 4B: Deep luminous crystal orb interior
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xCC5B21B6),
                                Color(0xDD2E1065),
                                Color(0xEE180D36)
                            ),
                            center = Offset(focusX, midY - activeOrbR * 0.18f),
                            radius = activeOrbR * 1.05f
                        ),
                        radius = activeOrbR,
                        center = Offset(focusX, midY)
                    )

                    // Pass 4C: Outer neon glow halo ring + crisp bright white-lavender circle border
                    drawCircle(
                        color = primaryGlow.copy(alpha = 0.62f + 0.20f * touchAwaken),
                        radius = activeOrbR,
                        center = Offset(focusX, midY),
                        style = Stroke(width = 6.dp.toPx())
                    )
                    drawCircle(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White,
                                electricLavender.copy(alpha = 0.92f),
                                Color.White
                            ),
                            startY = midY - activeOrbR,
                            endY = midY + activeOrbR
                        ),
                        radius = activeOrbR,
                        center = Offset(focusX, midY),
                        style = Stroke(width = 2.0.dp.toPx())
                    )

                    // Pass 4D: Signature 12 o'clock & 6 o'clock optical lens flares (matching 1000394279.jpg!)
                    val flareR = 16.dp.toPx() * (1f + 0.22f * touchAwaken + pulse)
                    // Top 12 o'clock lens flare
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.92f),
                                electricLavender.copy(alpha = 0.45f),
                                Color.Transparent
                            ),
                            center = Offset(focusX, midY - activeOrbR),
                            radius = flareR
                        ),
                        radius = flareR,
                        center = Offset(focusX, midY - activeOrbR)
                    )
                    // Bottom 6 o'clock lens flare (brighter, anchoring the figure-8 cradle!)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.96f),
                                electricLavender.copy(alpha = 0.58f),
                                Color.Transparent
                            ),
                            center = Offset(focusX, midY + activeOrbR),
                            radius = flareR * 1.25f
                        ),
                        radius = flareR * 1.25f,
                        center = Offset(focusX, midY + activeOrbR)
                    )
                }
            }
    ) {
        // ── LAYER 5: FOREGROUND ICONS (Aligned precisely over the 3 orbs) ──
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContinuumSection.ordered.forEach { section ->
                val isSelected = section == displayedSection
                ContinuumForegroundIconSlot(
                    section = section,
                    isSelected = isSelected,
                    reduceMotion = renderProfile.reduceMotion,
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
private fun ContinuumForegroundIconSlot(
    section: ContinuumSection,
    isSelected: Boolean,
    reduceMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else RikoTextSecondary.copy(alpha = 0.86f),
        label = "fgIconTint"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected && !reduceMotion) 1.24f else 0.96f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 340f),
        label = "fgIconScale"
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
        Canvas(
            modifier = Modifier
                .size(25.dp)
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
