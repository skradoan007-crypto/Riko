package com.rikodev.music.android.presentation.continuum

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

// ─────────────────────────────────────────────────────────────────────────────
// RIKO CONTINUUM — PURE LIQUID GLASS ORGANIC WAVE NAVBAR (P2 + P12)
//
// Sculpted optical liquid glass wave rising smoothly beneath the active tab
// (Listen / Explore / Library):
//   - Deep 28dp organic wave crest with secondary counter-flowing glass aura
//   - Pure translucent crystal glass body + Fresnel top specular + crisp rim
//   - Interruptible spring horizontal wave travel across rapid taps
//   - Custom geometric Headphones, Orbital Planet, and Books icons
// ─────────────────────────────────────────────────────────────────────────────

val ContinuumWaveBarHeight = 88.dp

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
    val targetFraction = activeSection.tabIndex / 2f

    val waveFocus = remember { Animatable(targetFraction) }
    LaunchedEffect(targetFraction, renderProfile.reduceMotion) {
        if (renderProfile.reduceMotion) {
            waveFocus.snapTo(targetFraction)
        } else {
            waveFocus.animateTo(
                targetValue = targetFraction,
                animationSpec = spring(
                    dampingRatio = 0.70f,
                    stiffness = 300f
                )
            )
        }
    }

    val effectivePrimary = if (renderProfile.useDynamicArtworkColors) primaryAccent else AccentViolet
    val effectiveSecondary = if (renderProfile.useDynamicArtworkColors) secondaryAccent else RikoBlue

    val agslShader = remember(renderProfile.useAgslShaders) {
        if (renderProfile.useAgslShaders && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LiquidNexusShaders.createChamberShaderOrNull()
        } else null
    }

    val wavePath = remember { Path() }
    val secondaryWavePath = remember { Path() }
    val crestStrokePath = remember { Path() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .height(ContinuumWaveBarHeight)
            .drawWithCache {
                val crestH = 26.dp.toPx()
                val botCorner = 24.dp.toPx()
                val baseFillColor = Color(0xAA0C0A1A)
                val rimStroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                val outerGlowStroke = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

                onDrawBehind {
                    val focus = waveFocus.value
                    val energy = if (renderProfile.useMdnaReactivity) audioEnergy.coerceIn(0f, 1f) else 0f
                    val focusCenterX = size.width * (1f / 6f + focus * (4f / 6f))

                    // 0. Secondary translucent glass aurora wave behind primary crest
                    if (renderProfile.useOrganicBezier) {
                        LiquidDockGeometry.buildContinuumSecondaryWavePath(
                            outPath = secondaryWavePath,
                            width = size.width,
                            height = size.height,
                            activeFraction = focus,
                            crestHeightPx = crestH,
                            bottomCornerPx = botCorner,
                            phaseRad = liveClock.value
                        )
                        drawPath(
                            path = secondaryWavePath,
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    effectivePrimary.copy(alpha = 0.30f + 0.12f * energy),
                                    effectiveSecondary.copy(alpha = 0.14f),
                                    Color.Transparent
                                ),
                                center = Offset(focusCenterX, crestH * 0.55f),
                                radius = size.width * 0.34f
                            )
                        )
                    }

                    LiquidDockGeometry.buildContinuumWavePath(
                        outPath = wavePath,
                        width = size.width,
                        height = size.height,
                        activeFraction = focus,
                        crestHeightPx = crestH,
                        bottomCornerPx = botCorner,
                        organicMode = renderProfile.useOrganicBezier
                    )
                    LiquidDockGeometry.buildContinuumWaveCrestStroke(
                        outPath = crestStrokePath,
                        width = size.width,
                        height = size.height,
                        activeFraction = focus,
                        crestHeightPx = crestH,
                        organicMode = renderProfile.useOrganicBezier
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
                            timeSec = liveClock.value,
                            focalFractionX = focusCenterX / size.width.coerceAtLeast(1f),
                            energy = energy,
                            primary = effectivePrimary,
                            secondary = effectiveSecondary,
                            base = baseFillColor
                        )
                    } else null

                    // 1. Pure Optical Liquid Glass body
                    drawLiquidGlassBody(
                        path = wavePath,
                        focalX = focusCenterX,
                        focalY = crestH * 0.95f,
                        primaryAccent = effectivePrimary,
                        secondaryAccent = effectiveSecondary,
                        energy = energy,
                        renderProfile = renderProfile,
                        shaderBrush = shaderBrush
                    )

                    // 2. Luminous crest catchlight right along the sculpted wave top edge
                    if (renderProfile.useGlowAndSpecular) {
                        drawPath(
                            path = crestStrokePath,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    effectivePrimary.copy(alpha = 0.12f),
                                    effectiveSecondary.copy(alpha = 0.45f),
                                    Color.White.copy(alpha = 0.65f),
                                    effectivePrimary.copy(alpha = 0.45f),
                                    effectivePrimary.copy(alpha = 0.12f)
                                )
                            ),
                            style = outerGlowStroke,
                            alpha = 0.36f
                        )
                    }
                    drawPath(
                        path = crestStrokePath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.18f),
                                effectivePrimary.copy(alpha = 0.60f),
                                Color.White.copy(alpha = 0.92f),
                                effectiveSecondary.copy(alpha = 0.60f),
                                Color.White.copy(alpha = 0.18f)
                            )
                        ),
                        style = rimStroke
                    )
                }
            }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Bottom
        ) {
            ContinuumSection.ordered.forEach { section ->
                val isSelected = section == activeSection
                ContinuumNavTabItem(
                    section = section,
                    isSelected = isSelected,
                    reduceMotion = renderProfile.reduceMotion,
                    accentColor = effectivePrimary,
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
private fun ContinuumNavTabItem(
    section: ContinuumSection,
    isSelected: Boolean,
    reduceMotion: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) RikoTextOnAccent else RikoTextSecondary,
        label = "continuumIconTint"
    )
    val labelColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else RikoTextSecondary.copy(alpha = 0.80f),
        label = "continuumLabelTint"
    )
    val liftDp by animateDpAsState(
        targetValue = if (isSelected) (-9).dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 340f),
        label = "continuumLift"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected && !reduceMotion) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 360f),
        label = "continuumScale"
    )

    Column(
        modifier = modifier
            .height(78.dp)
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
        Box(
            modifier = Modifier
                .offset(y = if (reduceMotion) 0.dp else liftDp)
                .size(34.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(26.dp)) {
                when (section) {
                    ContinuumSection.LISTEN -> drawHeadphonesIcon(iconColor, isSelected, accentColor)
                    ContinuumSection.EXPLORE -> drawOrbitalPlanetIcon(iconColor, isSelected, accentColor)
                    ContinuumSection.LIBRARY -> drawBooksIcon(iconColor, isSelected, accentColor)
                }
            }
        }
        Spacer(Modifier.height(1.dp))
        Text(
            text = section.label,
            style = RikoTypography.Caption.copy(
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = labelColor,
            maxLines = 1
        )
    }
}

private fun DrawScope.drawHeadphonesIcon(color: Color, active: Boolean, glow: Color) {
    val w = size.width
    val h = size.height
    val stroke = Stroke(width = 2.1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    if (active) {
        drawCircle(
            brush = Brush.radialGradient(listOf(glow.copy(alpha = 0.52f), Color.Transparent)),
            radius = w * 0.72f,
            center = Offset(w * 0.5f, h * 0.5f)
        )
    }
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

private fun DrawScope.drawOrbitalPlanetIcon(color: Color, active: Boolean, glow: Color) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val cy = h * 0.52f
    val stroke = Stroke(width = 2.0.dp.toPx(), cap = StrokeCap.Round)
    if (active) {
        drawCircle(
            brush = Brush.radialGradient(listOf(glow.copy(alpha = 0.56f), Color.Transparent)),
            radius = w * 0.75f,
            center = Offset(cx, cy)
        )
    }
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

private fun DrawScope.drawBooksIcon(color: Color, active: Boolean, glow: Color) {
    val w = size.width
    val h = size.height
    val stroke = Stroke(width = 1.9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    val style = if (active) Fill else stroke
    if (active) {
        drawCircle(
            brush = Brush.radialGradient(listOf(glow.copy(alpha = 0.50f), Color.Transparent)),
            radius = w * 0.72f,
            center = Offset(w * 0.5f, h * 0.52f)
        )
    }
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
