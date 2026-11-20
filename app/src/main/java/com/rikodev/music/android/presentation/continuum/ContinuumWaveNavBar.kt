package com.rikodev.music.android.presentation.continuum

import android.os.Build
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.geometry.RoundRect
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
// RIKO CONTINUUM CORE — LIVING ORB & SILK-WAVE NAVIGATION BAR (P15 — 100% MATCH)
//
// Exact visual & interaction fidelity to the CONTINUUM CORE specification:
//   - Deep obsidian-indigo translucent glass dock capsule (no milky washout,
//     zero text clutter — 3 pure centered orbs on the luminous centerline)
//   - Vivid intertwined multi-layer glowing aurora silk wave ribbons flowing
//     continuously between all 3 orbs (violet + cyan + crisp white core)
//   - Active orb swells to 54dp radiant crystal orb with double neon-glass
//     ring & soft outer bloom; inactive orbs rest at 40dp dark frosted glass
//   - Full 5-stage motion: Idle -> Touch/Hold Awaken -> Slide-to-Switch
//     fluid light trail -> Elastic World Morph -> Arrived
// ─────────────────────────────────────────────────────────────────────────────

val ContinuumWaveBarHeight = 74.dp

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
    val waveFocus = remember { Animatable(targetFraction) }
    var isTouched by remember { mutableStateOf(false) }
    var isSliding by remember { mutableStateOf(false) }
    var slideFraction by remember { mutableFloatStateOf(targetFraction) }

    LaunchedEffect(targetFraction, isSliding, renderProfile.reduceMotion) {
        if (!isSliding) {
            if (renderProfile.reduceMotion) {
                waveFocus.snapTo(targetFraction)
            } else {
                waveFocus.animateTo(
                    targetValue = targetFraction,
                    animationSpec = spring(
                        dampingRatio = 0.66f,
                        stiffness = 300f
                    )
                )
            }
        }
    }

    // Core awakens brightly on touch, hold, or slide (Storyboard Steps 2–3)
    val touchAwaken by animateFloatAsState(
        targetValue = if (isTouched || isSliding) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.74f, stiffness = 360f),
        label = "continuumCoreAwaken"
    )

    val effectivePrimary = if (renderProfile.useDynamicArtworkColors) primaryAccent else AccentViolet
    val effectiveSecondary = if (renderProfile.useDynamicArtworkColors) secondaryAccent else RikoBlue
    val neonCyan = Color(0xFF7DD3FC)

    val capsulePath = remember { Path() }
    val ribbonPath1 = remember { Path() }
    val ribbonPath2 = remember { Path() }
    val ribbonPath3 = remember { Path() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(ContinuumWaveBarHeight)
            // Touch/Hold Awaken + Slide-to-Switch across the CONTINUUM dock
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isTouched = true
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    val startX = down.position.x
                    var dragged = false
                    var lastIdx = currentActive.tabIndex

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            if (dragged) {
                                change.consume()
                                isSliding = false
                                val finalIdx = (slideFraction * 2f).roundToInt().coerceIn(0, 2)
                                val target = ContinuumSection.fromIndex(finalIdx)
                                if (target != currentActive) {
                                    haptic.fire(RikoHapticEvent.CONFIRMATION)
                                    currentOnSelect(target)
                                }
                            }
                            break
                        }
                        val dx = abs(change.position.x - startX)
                        if (!dragged && dx > 12f) {
                            dragged = true
                            isSliding = true
                        }
                        if (dragged) {
                            change.consume()
                            val rawFrac = ((change.position.x / w - 1f / 6f) / (4f / 6f)).coerceIn(0f, 1f)
                            slideFraction = rawFrac
                            val hoverIdx = (rawFrac * 2f).roundToInt().coerceIn(0, 2)
                            if (hoverIdx != lastIdx) {
                                lastIdx = hoverIdx
                                haptic.fire(RikoHapticEvent.CONFIRMATION)
                                currentOnSelect(ContinuumSection.fromIndex(hoverIdx))
                            }
                        }
                    }
                    isSliding = false
                    isTouched = false
                }
            }
            .drawWithCache {
                val cornerPx = 32.dp.toPx()

                onDrawBehind {
                    val focus = if (isSliding) slideFraction else waveFocus.value
                    val energy = if (renderProfile.useMdnaReactivity) audioEnergy.coerceIn(0f, 1f) else 0f
                    val focusCenterX = size.width * (1f / 6f + focus * (4f / 6f))
                    val midY = size.height * 0.50f
                    val time = liveClock.value

                    capsulePath.reset()
                    capsulePath.addRoundRect(
                        RoundRect(
                            left = 0f,
                            top = 0f,
                            right = size.width,
                            bottom = size.height,
                            cornerRadius = CornerRadius(cornerPx, cornerPx)
                        )
                    )

                    // 1. Deep dark obsidian-indigo glass dock base (makes glowing aurora ribbons POP!)
                    drawPath(
                        path = capsulePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xCC120E26),
                                Color(0xE0080614)
                            ),
                            startY = 0f,
                            endY = size.height
                        )
                    )

                    // 2. Clipped living aurora ribbons & radiant orb bloom inside the capsule
                    clipPath(capsulePath) {
                        val ampBase = size.height * (0.24f + 0.08f * energy + 0.10f * touchAwaken)
                        val steps = 48

                        fun buildSilkWave(path: Path, freq: Float, phaseOffset: Float, ampMul: Float) {
                            path.reset()
                            for (i in 0..steps) {
                                val u = i / steps.toFloat()
                                val x = u * size.width
                                val dx = (x - focusCenterX) / (size.width * 0.25f).coerceAtLeast(1f)
                                val focusEnvelope = 0.42f + 0.85f * exp(-dx * dx)
                                val y = midY + sin(u * PI.toFloat() * freq + time * 1.45f + phaseOffset) *
                                    ampBase * ampMul * focusEnvelope
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                        }

                        buildSilkWave(ribbonPath1, freq = 2.4f, phaseOffset = 0f, ampMul = 1.0f)
                        buildSilkWave(ribbonPath2, freq = 3.0f, phaseOffset = 2.05f, ampMul = 0.82f)
                        buildSilkWave(ribbonPath3, freq = 1.9f, phaseOffset = 3.80f, ampMul = 0.58f)

                        // Deep radiant aurora mist behind the active orb
                        val mistR = 64.dp.toPx() * (1f + 0.16f * touchAwaken + 0.10f * energy)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    effectivePrimary.copy(alpha = 0.72f + 0.18f * touchAwaken),
                                    effectiveSecondary.copy(alpha = 0.36f),
                                    Color.Transparent
                                ),
                                center = Offset(focusCenterX, midY),
                                radius = mistR
                            ),
                            radius = mistR,
                            center = Offset(focusCenterX, midY)
                        )

                        // Wide soft neon ribbon aura
                        if (renderProfile.selectedTier != VisualQualityTier.LITE) {
                            drawPath(
                                path = ribbonPath1,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        effectivePrimary.copy(alpha = 0.16f),
                                        effectivePrimary.copy(alpha = 0.68f + 0.22f * touchAwaken),
                                        neonCyan.copy(alpha = 0.68f),
                                        effectiveSecondary.copy(alpha = 0.68f),
                                        effectivePrimary.copy(alpha = 0.16f)
                                    )
                                ),
                                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }

                        // Primary vivid glowing silk strand (white-lavender-cyan plasma)
                        drawPath(
                            path = ribbonPath1,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    effectivePrimary.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.90f),
                                    neonCyan.copy(alpha = 0.95f),
                                    Color.White.copy(alpha = 0.90f),
                                    effectiveSecondary.copy(alpha = 0.35f)
                                )
                            ),
                            style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // Secondary & tertiary intertwining plasma strands
                        if (renderProfile.useOrganicBezier) {
                            drawPath(
                                path = ribbonPath2,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        effectiveSecondary.copy(alpha = 0.78f),
                                        Color.White.copy(alpha = 0.85f),
                                        effectivePrimary.copy(alpha = 0.78f),
                                        Color.Transparent
                                    )
                                ),
                                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                            drawPath(
                                path = ribbonPath3,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        neonCyan.copy(alpha = 0.65f),
                                        Color.White.copy(alpha = 0.65f),
                                        Color.Transparent
                                    )
                                ),
                                style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }

                        // Fluid sliding light-orb trail during Slide-to-Switch (Storyboard Step 3)
                        if (touchAwaken > 0.05f) {
                            val trailR = 30.dp.toPx() * (0.85f + 0.25f * touchAwaken)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.45f * touchAwaken),
                                        neonCyan.copy(alpha = 0.40f * touchAwaken),
                                        Color.Transparent
                                    ),
                                    center = Offset(focusCenterX, midY),
                                    radius = trailR
                                ),
                                radius = trailR,
                                center = Offset(focusCenterX, midY)
                            )
                        }

                        // Subtle curved top Fresnel glass highlight (kept thin so dark crystal stays deep)
                        drawRect(
                            brush = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.00f to Color.White.copy(alpha = 0.18f),
                                    0.12f to Color.White.copy(alpha = 0.04f),
                                    0.35f to Color.Transparent,
                                    0.90f to Color.Transparent,
                                    1.00f to Color.White.copy(alpha = 0.08f)
                                ),
                                startY = 0f,
                                endY = size.height
                            )
                        )
                    }

                    // 3. Crisp 1.25dp optical glass capsule border
                    drawPath(
                        path = capsulePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.46f + 0.20f * touchAwaken),
                                effectivePrimary.copy(alpha = 0.38f),
                                Color.White.copy(alpha = 0.22f)
                            ),
                            startY = 0f,
                            endY = size.height
                        ),
                        style = Stroke(width = 1.25.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ContinuumSection.ordered.forEach { section ->
                val isSelected = section == activeSection
                ContinuumOrbTabItem(
                    section = section,
                    isSelected = isSelected,
                    reduceMotion = renderProfile.reduceMotion,
                    primaryAccent = effectivePrimary,
                    secondaryAccent = effectiveSecondary,
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
private fun ContinuumOrbTabItem(
    section: ContinuumSection,
    isSelected: Boolean,
    reduceMotion: Boolean,
    primaryAccent: Color,
    secondaryAccent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else RikoTextSecondary.copy(alpha = 0.88f),
        label = "continuumOrbIconTint"
    )
    val orbSize by animateDpAsState(
        targetValue = if (isSelected) 54.dp else 40.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 320f),
        label = "continuumOrbSize"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected && !reduceMotion) 1.18f else 0.96f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 350f),
        label = "continuumOrbIconScale"
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
                contentDescription = "${section.label} section"
            },
        contentAlignment = Alignment.Center
    ) {
        // Pure centered Living Glass Orb (54dp glowing active orb / 40dp calm frosted dark glass orb)
        Box(
            modifier = Modifier
                .size(orbSize)
                .clip(CircleShape)
                .background(
                    if (isSelected) {
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFE9D5FF).copy(alpha = 0.45f),
                                primaryAccent.copy(alpha = 0.86f),
                                secondaryAccent.copy(alpha = 0.65f),
                                Color(0xDD1A1038)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x991C1834),
                                Color(0xCC0A0818)
                            )
                        )
                    }
                )
                .border(
                    width = if (isSelected) 1.8.dp else 1.0.dp,
                    brush = Brush.verticalGradient(
                        colors = if (isSelected) {
                            listOf(Color.White.copy(alpha = 0.96f), primaryAccent.copy(alpha = 0.72f))
                        } else {
                            listOf(Color.White.copy(alpha = 0.36f), Color.White.copy(alpha = 0.14f))
                        }
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(23.dp)
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
    val stroke = Stroke(width = 2.0.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
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
    val stroke = Stroke(width = 1.9.dp.toPx(), cap = StrokeCap.Round)
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
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
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
