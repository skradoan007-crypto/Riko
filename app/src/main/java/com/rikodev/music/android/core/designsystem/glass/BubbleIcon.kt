package com.rikodev.music.android.core.designsystem.glass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMotion
import com.rikodev.music.android.core.designsystem.riko.theme.RikoStateTokens
import com.rikodev.music.android.core.designsystem.animation.LocalRikoClock
import com.rikodev.music.android.core.designsystem.animation.oscillate
import com.rikodev.music.android.core.designsystem.theme.AccentCyan
import com.rikodev.music.android.core.designsystem.theme.AccentViolet
import com.rikodev.music.android.core.designsystem.theme.GlassBorderLight
import com.rikodev.music.android.core.designsystem.theme.GlassSpecular
import com.rikodev.music.android.core.designsystem.theme.GlassWhite08
import com.rikodev.music.android.core.designsystem.theme.GlassWhite12
import com.rikodev.music.android.core.designsystem.riko.theme.RikoShape
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — BUBBLE ICON SYSTEM (light-glass edition)
//
// Every icon still "floats" and every press still springs — but ALL of them
// now read timing from ONE shared LocalRikoClock (see RikoLiveClock.kt)
// instead of each running its own infiniteTransition. Same organic feel,
// far fewer animations running concurrently → smoother scrolling/swiping.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Primary Bubble Icon Button — the big glass play/pause bubble.
 *
 * @param shineEnabled Adds a slow moving light-sweep highlight (used for the
 * Play button so it visibly "breathes" even when nothing is pressed).
 */
@Composable
fun BubbleIconButton(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    glowColor: Color = AccentCyan,
    secondaryColor: Color = AccentViolet,
    floatDelay: Int = 0,
    floatAmplitude: Dp = 6.dp,
    floatDuration: Int = 3200,
    enableFloat: Boolean = true,
    shineEnabled: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val clock = LocalRikoClock.current

    // ── Press animation (the only per-instance state we actually need) ──────
    var isPressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) RikoStateTokens.PressScale else 1f,
        animationSpec = RikoMotion.StiffSpring(),
        label = "press_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                // Float + rotation read the SHARED clock — no dedicated
                // ticker for this button. graphicsLayer means only the
                // composited layer moves; nothing underneath re-draws.
                val angle = clock.value
                if (enableFloat) {
                    val t = oscillate(angle, floatDelay, floatDuration)
                    translationY = (t - 0.5f) * 2f * floatAmplitude.value
                    rotationZ = (t - 0.5f) * 3f
                }
                scaleX = pressScale
                scaleY = pressScale
            }
            .pointerInput(enabled) {
                detectTapGestures(
                    onPress = {
                        if (enabled) {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        }
                    },
                    onTap = { if (enabled) onClick() }
                )
            }
            .drawBehind {
                drawOuterBubbleShell(glowColor, secondaryColor)
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.62f)
                .drawWithContent {
                    drawInnerGlassGlyph(glowColor, shineEnabled, clock.value)
                    drawContent()
                },
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

/**
 * Outer bubble shell — the large glass sphere that wraps everything.
 */
private fun DrawScope.drawOuterBubbleShell(
    primaryColor: Color,
    secondaryColor: Color
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = size.minDimension / 2f

    drawCircle(color = GlassWhite08, radius = radius, center = center)

    drawCircle(
        brush = Brush.sweepGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.7f),
                secondaryColor.copy(alpha = 0.5f),
                primaryColor.copy(alpha = 0.3f),
                Color.White.copy(alpha = 0.6f),
                primaryColor.copy(alpha = 0.7f)
            ),
            center = center
        ),
        radius = radius,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
    )

    // DESIGN FIX (real annotated-screenshot feedback): was 0.80 alpha
    // covering ~50% of the button width — the most extreme instance of
    // the "glossy hotspot" problem, on the app's most prominent button.
    // Reduced to a small, subtle top-edge highlight.
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                GlassSpecular.copy(alpha = 0.22f),
                Color.Transparent
            ),
            center = Offset(size.width * 0.30f, size.height * 0.22f),
            radius = radius * 0.32f
        ),
        topLeft = Offset(size.width * 0.14f, size.height * 0.08f),
        size = Size(size.width * 0.30f, size.height * 0.22f)
    )

    // Bottom-right soft ink shadow — grounds the bubble on light glass
    // (a white reflection here would vanish against the light shell).
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x14211A12), Color.Transparent),
            center = Offset(size.width * 0.80f, size.height * 0.82f),
            radius = radius * 0.32f
        ),
        topLeft = Offset(size.width * 0.55f, size.height * 0.60f),
        size = Size(size.width * 0.38f, size.height * 0.34f)
    )

    val particlePositions = listOf(
        Offset(size.width * 0.28f, size.height * 0.40f),
        Offset(size.width * 0.55f, size.height * 0.32f),
        Offset(size.width * 0.38f, size.height * 0.62f),
        Offset(size.width * 0.68f, size.height * 0.55f),
        Offset(size.width * 0.42f, size.height * 0.28f),
    )
    particlePositions.forEachIndexed { i, pos ->
        val particleRadius = (1.2f - i * 0.15f).dp.toPx().coerceAtLeast(0.5.dp.toPx())
        drawCircle(
            color = primaryColor.copy(alpha = 0.35f - i * 0.05f),
            radius = particleRadius,
            center = pos
        )
    }
}

private fun DrawScope.drawInnerGlassGlyph(
    glowColor: Color,
    shineEnabled: Boolean = false,
    clockAngle: Float = 0f
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = size.minDimension / 2f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                glowColor.copy(alpha = 0.65f),
                glowColor.copy(alpha = 0.30f),
                glowColor.copy(alpha = 0.10f),
                Color.Transparent
            ),
            center = center,
            radius = radius * 1.2f
        ),
        radius = radius,
        center = center
    )

    // DESIGN FIX: was 0.60 alpha covering 65%x45% of the inner glyph —
    // same "hotspot" problem as the outer shell. Subtle top-edge catch
    // instead of a dominant white blob.
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
            center = Offset(size.width * 0.32f, size.height * 0.22f),
            radius = radius * 0.32f
        ),
        topLeft = Offset(size.width * 0.14f, size.height * 0.04f),
        size = Size(size.width * 0.32f, size.height * 0.24f)
    )

    // ── Reactive shine sweep (Play button) ───────────────────────────────
    // Drawn on the SATURATED glyph fill (not the pale outer shell) so it's
    // actually visible — a soft light blob drifting across, like light
    // catching real glass. Reuses the shared clock, no extra ticker.
    if (shineEnabled) {
        val t = oscillate(clockAngle, 0, 5200)
        val cx = size.width * (0.15f + t * 0.7f)
        val cy = size.height * (0.20f + (1f - t) * 0.35f)
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.55f), Color.Transparent),
                center = Offset(cx, cy),
                radius = radius * 0.4f
            ),
            topLeft = Offset(cx - radius * 0.34f, cy - radius * 0.28f),
            size = Size(radius * 0.68f, radius * 0.56f)
        )
    }
}

/**
 * Small bubble icon — tab bar / toolbar icons. Same shared-clock approach.
 */
@Composable
fun SmallBubbleIcon(
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    isActive: Boolean = false,
    activeColor: Color = AccentCyan,
    floatDelay: Int = 0,
    enableFloat: Boolean = true,
    onClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val clock = LocalRikoClock.current

    var isPressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) RikoStateTokens.PressScale else 1f,
        animationSpec = RikoMotion.MediumSpring(),
        label = "small_press"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                if (enableFloat) {
                    val t = oscillate(clock.value, floatDelay, 2800)
                    translationY = (t - 0.5f) * 2f * 4f
                }
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RikoShape.Circle)
            .drawBehind {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val radius = this.size.minDimension / 2f

                drawCircle(
                    color = if (isActive) activeColor.copy(alpha = 0.25f) else GlassWhite08,
                    radius = radius,
                    center = center
                )

                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            if (isActive) activeColor.copy(alpha = 0.8f) else GlassBorderLight,
                            Color.White.copy(alpha = 0.2f)
                        )
                    ),
                    radius = radius,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.8.dp.toPx())
                )

                // DESIGN FIX (real annotated-screenshot feedback): this used
                // to be a bright white oval at 0.6 alpha covering ~40% of
                // the button — exactly the "glossy plastic hotspot" the
                // brief calls out by name, repeated on every circular
                // button on every screen. Replaced with a small, subtle
                // top-edge highlight: a thin crescent, not a spotlight.
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(this.size.width * 0.30f, this.size.height * 0.22f),
                        radius = radius * 0.35f
                    ),
                    topLeft = Offset(this.size.width * 0.12f, this.size.height * 0.06f),
                    size = Size(this.size.width * 0.30f, this.size.height * 0.22f)
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
