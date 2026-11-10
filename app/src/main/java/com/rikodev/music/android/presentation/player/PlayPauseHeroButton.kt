package com.rikodev.music.android.presentation.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.components.V2TestTags
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMotion
import com.rikodev.music.android.core.designsystem.riko.theme.RikoStateTokens
import com.rikodev.music.android.core.designsystem.riko.theme.RikoThemeV2


@Composable
fun PlayPauseHeroButton(
    isPlaying: Boolean,
    energy: Float,
    hasBeat: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 72.dp
) {
    // Brand-anchored hero: V2 CTA gradient family (pink ΓåÆ violet), stable on
    // any artwork and across palette transitions. Never artwork-dynamic.
    val brandPink = RikoThemeV2.Colors.ctaGradient[0]
    val brandViolet = RikoThemeV2.Colors.ctaGradient[1]

    // ΓöÇΓöÇ Pressure state (held down vs released) ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
    var isPressed by remember { mutableStateOf(false) }

    // ΓöÇΓöÇ Ripple state (triggered on tap, animates expansion + fade) ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
    var rippleActive by remember { mutableStateOf(false) }

    // ΓöÇΓöÇ Press scale: deeper press ΓåÆ smaller scale ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
    val pressScale by animateFloatAsState(
        targetValue = when {
            isPressed -> RikoStateTokens.PressScale  // spec 5.4 depth (A80)
            else -> 1f
        },
        animationSpec = RikoMotion.iconPress(),  // Bouncy spring for physical feel
        label = "hero_press_scale"
    )

    // ΓöÇΓöÇ Button fill color: brand pink playing, dimmed brand paused ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
    val buttonFillColor by animateColorAsState(
        targetValue = when {
            isPlaying -> brandPink
            else -> brandPink.copy(alpha = 0.45f)
        },
        animationSpec = RikoMotion.colorMorphTransition(),
        label = "hero_fill_color"
    )

    // ΓöÇΓöÇ Glow intensity: energy-driven when playing, off when paused ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
    // Beat adds a micro-pulse spike on top of the base glow.
    val beatPulse = if (hasBeat && isPlaying) 0.15f else 0f
    val targetGlow = if (isPlaying) {
        (energy * 0.5f) + beatPulse
    } else {
        0f
    }
    val glowIntensity by animateFloatAsState(
        targetValue = targetGlow,
        animationSpec = RikoMotion.SoftSpring(),
        label = "hero_glow"
    )

    // ΓöÇΓöÇ Inner ring brightness follows energy ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
    val innerRingAlpha by animateFloatAsState(
        targetValue = 0.25f + (energy * 0.35f),
        animationSpec = RikoMotion.fast(),
        label = "hero_inner_ring"
    )

    // ΓöÇΓöÇ Specular brightness follows energy ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
    val specularAlpha by animateFloatAsState(
        targetValue = 0.3f + (energy * 0.25f),
        animationSpec = RikoMotion.fast(),
        label = "hero_specular"
    )

    // ΓöÇΓöÇ Elevation: 16dp idle, 8dp pressed, up to 20dp when playing+energy ΓöÇ
    val elevation by animateFloatAsState(
        targetValue = when {
            isPressed -> 8f
            isPlaying -> 16f + (energy * 4f)  // up to 20dp
            else -> 16f
        },
        animationSpec = RikoMotion.MediumSpring(),
        label = "hero_elevation"
    )

    // ΓöÇΓöÇ Ripple animation (expansion + fade) ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
    // Build 26 Phase 8: Ripple uses spring instead of tween ΓÇö
    // organic expansion, interruptible if user taps rapidly.
    val rippleRadius by animateFloatAsState(
        targetValue = if (rippleActive) 1f else 0f,
        animationSpec = RikoMotion.MediumSpring(),
        label = "hero_ripple_radius"
    )
    val rippleAlpha by animateFloatAsState(
        targetValue = if (rippleActive) 0.4f else 0f,
        animationSpec = RikoMotion.SoftSpring(),
        label = "hero_ripple_alpha"
    )

    // The ripple is brand sourced, never artwork sourced.
    val rippleColor = brandPink

    Box(
        modifier = modifier
            .size(buttonSize)
            .testTag(V2TestTags.NP_PLAY_PAUSE)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .shadow(
                elevation = elevation.dp,
                shape = CircleShape,
                ambientColor = brandPink.copy(alpha = glowIntensity * 0.6f),
                spotColor = brandViolet.copy(alpha = glowIntensity * 0.8f)
            )
            .clip(CircleShape)
            .drawBehind {
                val buttonPx = size.minDimension

                // ΓöÇΓöÇ Outer glow aura (energy-reactive, brand violet) ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
                if (glowIntensity > 0.01f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                brandViolet.copy(alpha = glowIntensity * 0.5f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = buttonPx * 0.72f
                        ),
                        radius = buttonPx * 0.72f
                    )
                }

                // ΓöÇΓöÇ Glass fill: translucent radial gradient ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            buttonFillColor.copy(
                                red = (buttonFillColor.red * 1.12f).coerceAtMost(1f),
                                green = (buttonFillColor.green * 1.12f).coerceAtMost(1f),
                                blue = (buttonFillColor.blue * 1.12f).coerceAtMost(1f)
                            ),
                            buttonFillColor
                        ),
                        center = Offset(buttonPx * 0.35f, buttonPx * 0.3f),
                        radius = buttonPx * 0.9f
                    )
                )

                // ΓöÇΓöÇ Inner ring: brightness follows energy ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
                drawCircle(
                    color = Color.White.copy(alpha = innerRingAlpha),
                    radius = (buttonPx / 2f) - 6.dp.toPx(),
                    style = Stroke(width = 1.2.dp.toPx())
                )

                // ΓöÇΓöÇ Specular catch-light ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = specularAlpha),
                            Color.Transparent
                        ),
                        center = Offset(buttonPx * 0.28f, buttonPx * 0.22f),
                        radius = buttonPx * 0.35f
                    ),
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width * 0.6f, size.height * 0.5f)
                )

                // ΓöÇΓöÇ Glass ripple emanating from center ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
                if (rippleAlpha > 0.01f && rippleRadius > 0.01f) {
                    drawCircle(
                        color = rippleColor.copy(alpha = rippleAlpha),
                        radius = (buttonPx / 2f) * rippleRadius,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        // Deeper press: shadow reduces, scale decreases
                        isPressed = true
                        tryAwaitRelease()
                        // Quick release ΓåÆ spring bounce back with overshoot
                        isPressed = false
                    },
                    onTap = {
                        // Trigger ripple + play/pause
                        rippleActive = true
                        onClick()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // ΓöÇΓöÇ Ripple lifecycle: auto-dismiss after animation completes ΓöÇΓöÇΓöÇΓöÇΓöÇ
        // We use a side-effect via LaunchedEffect inside the draw scope
        // equivalent ΓÇö reset ripple when expansion completes.
        if (rippleActive && rippleRadius >= 0.95f) {
            rippleActive = false
        }

        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color.White,
            modifier = Modifier.size(buttonSize * 0.42f)
        )
    }
}
