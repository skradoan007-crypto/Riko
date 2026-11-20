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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.music.android.core.designsystem.animation.LocalRikoClock
import com.rikodev.music.android.core.designsystem.riko.components.RikoHapticEvent
import com.rikodev.music.android.core.designsystem.riko.components.rememberRikoHaptic
import com.rikodev.music.android.core.designsystem.riko.theme.AccentMagenta
import com.rikodev.music.android.core.designsystem.riko.theme.AccentViolet
import com.rikodev.music.android.core.designsystem.riko.theme.RikoBlue
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextOnAccent
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextSecondary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography
import com.rikodev.music.android.presentation.continuum.LiquidGlassStyle.drawLiquidGlassBody
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// RIKO CONTINUUM CORE — LIVING ORB & SILK-WAVE NAVIGATION BAR (P14)
//
// Matches the CONTINUUM CORE visual & motion storyboard (1 -> 5):
//   - Floating Pure Optical Liquid Glass capsule dock (rounded pill/capsule)
//   - Intertwined luminous violet-cyan-white silk wave ribbons flowing
//     continuously horizontally between the 3 world orbs
//   - Active orb swells to 52dp glowing crystal halo; inactive orbs rest at
//     38dp frosted dark glass circles
//   - Supports BOTH Tap AND Slide-to-Switch with interruptible spring travel,
//     touch-awakening energy pulse, elastic orb morph, and haptic feedback
// ─────────────────────────────────────────────────────────────────────────────

val ContinuumWaveBarHeight = 76.dp

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
                        dampingRatio = 0.68f,
                        stiffness = 320f
                    )
                )
            }
        }
    }

    // Core awakens brightly on touch/slide (Storyboard Step 2 & 3)
    val touchAwaken by animateFloatAsState(
        targetValue = if (isSliding) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 360f),
        label = "continuumCoreAwaken"
    )

    val effectivePrimary = if (renderProfile.useDynamicArtworkColors) primaryAccent else AccentViolet
    val effectiveSecondary = if (renderProfile.useDynamicArtworkColors) secondaryAccent else RikoBlue

    val agslShader = remember(renderProfile.useAgslShaders) {
        if (renderProfile.useAgslShaders && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LiquidNexusShaders.createChamberShaderOrNull()
        } else null
    }

    val capsulePath = remember { Path() }
    val ribbonPath1 = remember { Path() }
    val ribbonPath2 = remember { Path() }
    val ribbonPath3 = remember { Path() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .height(ContinuumWaveBarHeight)
            // Slide-to-Switch gesture across the CONTINUUM dock (Storyboard Steps 2–5)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
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
                        if (!dragged && dx > 14f) {
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
                }
            }
            .drawWithCache {
                val cornerPx = 30.dp.toPx()
                val baseFillColor = Color(0xAA0C0A1C)

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

                    val shaderBrush = if (
                        agslShader != null &&
                        renderProfile.useAgslShaders &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ) {
                        LiquidNexusShaders.updateAndBuildBrush(
                            shader = agslShader,
                            width = size.width,
                            height = size.height,
                            timeSec = time,
                            focalFractionX = focusCenterX / size.width.coerceAtLeast(1f),
                            energy = (energy + touchAwaken * 0.45f).coerceIn(0f, 1f),
                            primary = effectivePrimary,
                            secondary = effectiveSecondary,
                            base = baseFillColor
                        )
                    } else null

                    // 1. Pure Optical Liquid Glass floating capsule
                    drawLiquidGlassBody(
                        path = capsulePath,
                        focalX = focusCenterX,
                        focalY = midY,
                        primaryAccent = effectivePrimary,
                        secondaryAccent = effectiveSecondary,
                        energy = (energy + touchAwaken * 0.35f).coerceIn(0f, 1f),
                        renderProfile = renderProfile,
                        shaderBrush = shaderBrush
                    )

                    // 2. CONTINUUM CORE living intertwined aurora silk wave ribbons inside the glass capsule
                    clipPath(capsulePath) {
                        val ampBase = size.height * (0.20f + 0.08f * energy + 0.08f * touchAwaken)
                        val steps = 40

                        fun buildSilkWave(path: Path, freq: Float, phaseOffset: Float, ampMul: Float) {
                            path.reset()
                            for (i in 0..steps) {
                                val u = i / steps.toFloat()
                                val x = u * size.width
                                val dx = (x - focusCenterX) / (size.width * 0.24f).coerceAtLeast(1f)
                                // Wave gracefully swells & arches around the active orb
                                val focusEnvelope = 0.45f + 0.75f * exp(-dx * dx)
                                val y = midY + sin(u * PI.toFloat() * freq + time * 1.15f + phaseOffset) *
                                    ampBase * ampMul * focusEnvelope
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                        }

                        buildSilkWave(ribbonPath1, freq = 2.6f, phaseOffset = 0f, ampMul = 1.0f)
                        buildSilkWave(ribbonPath2, freq = 3.2f, phaseOffset = 1.85f, ampMul = 0.78f)
                        buildSilkWave(ribbonPath3, freq = 2.1f, phaseOffset = 3.40f, ampMul = 0.56f)

                        // Soft wide aurora glow behind the silk wave
                        if (renderProfile.selectedTier != VisualQualityTier.LITE) {
                            drawPath(
                                path = ribbonPath1,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        effectivePrimary.copy(alpha = 0.14f),
                                        effectivePrimary.copy(alpha = 0.55f + 0.20f * touchAwaken),
                                        Color(0xFF7DD3FC).copy(alpha = 0.55f),
                                        effectiveSecondary.copy(alpha = 0.55f),
                                        effectivePrimary.copy(alpha = 0.14f)
                                    )
                                ),
                                style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }

                        // Primary crisp luminous silk strand
                        drawPath(
                            path = ribbonPath1,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.18f),
                                    effectivePrimary.copy(alpha = 0.85f),
                                    Color.White.copy(alpha = 0.92f),
                                    Color(0xFF93C5FD).copy(alpha = 0.85f),
                                    Color.White.copy(alpha = 0.18f)
                                )
                            ),
                            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // Secondary intertwining lavender-cyan strand
                        if (renderProfile.useOrganicBezier) {
                            drawPath(
                                path = ribbonPath2,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        effectiveSecondary.copy(alpha = 0.65f),
                                        Color.White.copy(alpha = 0.75f),
                                        effectivePrimary.copy(alpha = 0.65f),
                                        Color.Transparent
                                    )
                                ),
                                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                            drawPath(
                                path = ribbonPath3,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFF60A5FA).copy(alpha = 0.55f),
                                        Color.White.copy(alpha = 0.55f),
                                        Color.Transparent
                                    )
                                ),
                                style = Stroke(width = 1.1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }

                        // Luminous energy halo pooling behind the active orb
                        val orbHaloRadius = 46.dp.toPx() * (1f + 0.14f * touchAwaken + 0.10f * energy)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.34f + 0.14f * touchAwaken),
                                    effectivePrimary.copy(alpha = 0.62f),
                                    effectiveSecondary.copy(alpha = 0.24f),
                                    Color.Transparent
                                ),
                                center = Offset(focusCenterX, midY),
                                radius = orbHaloRadius
                            ),
                            radius = orbHaloRadius,
                            center = Offset(focusCenterX, midY)
                        )
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
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
        targetValue = if (isSelected) Color.White else RikoTextSecondary.copy(alpha = 0.85f),
        label = "continuumOrbIconTint"
    )
    val orbSize by animateDpAsState(
        targetValue = if (isSelected) 50.dp else 38.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 340f),
        label = "continuumOrbSize"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected && !reduceMotion) 1.14f else 0.94f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 360f),
        label = "continuumOrbIconScale"
    )

    Column(
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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Living Glass Orb (50dp glowing active orb vs 38dp calm frosted glass orb)
        Box(
            modifier = Modifier
                .size(orbSize)
                .clip(CircleShape)
                .background(
                    if (isSelected) {
                        Brush.radialGradient(
                            colors = listOf(
                                primaryAccent.copy(alpha = 0.82f),
                                secondaryAccent.copy(alpha = 0.52f),
                                Color(0xCC1E1542)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x88201D38),
                                Color(0xAA0E0C1E)
                            )
                        )
                    }
                )
                .border(
                    width = if (isSelected) 1.6.dp else 1.0.dp,
                    brush = Brush.verticalGradient(
                        colors = if (isSelected) {
                            listOf(Color.White.copy(alpha = 0.95f), primaryAccent.copy(alpha = 0.65f))
                        } else {
                            listOf(Color.White.copy(alpha = 0.34f), Color.White.copy(alpha = 0.12f))
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

        if (!isSelected) {
            Text(
                text = section.label,
                style = RikoTypography.Caption.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = RikoTextSecondary.copy(alpha = 0.72f),
                maxLines = 1
            )
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
