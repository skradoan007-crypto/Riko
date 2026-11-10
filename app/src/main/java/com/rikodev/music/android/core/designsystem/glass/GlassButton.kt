package com.rikodev.music.android.core.designsystem.glass

import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMotion
import com.rikodev.music.android.core.designsystem.riko.theme.RikoStateTokens
import com.rikodev.music.android.core.designsystem.theme.AccentCyan
import com.rikodev.music.android.core.designsystem.theme.GlassTokens
import com.rikodev.music.android.core.designsystem.riko.theme.RikoShape
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — GLASS BUTTONS (V2 — Style-Driven)
//
// Buttons do NOT calculate their own glass properties. They delegate visual
// rendering entirely to GlassSurface via GlassTokens.
//
// Button responsibility: Click, Press animation, Content (icon + text).
// Glass responsibility:  Blur, Tint, Border, Shadow, Glow, Specular.
//
// Two variants:
//   Primary   — Filled accent pill. Opaque accent background + glass frost.
//   Secondary — Outlined pill. Transparent glass + accent border.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Shared press-state driver for all glass buttons.
 * Returns the interaction source (for clickable) and the animated scale factor.
 */
@Composable
private fun rememberButtonPressScale(): Pair<MutableInteractionSource, Float> {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) RikoStateTokens.PressScale else 1f,
        animationSpec = RikoMotion.MediumSpring(),
        label = "glass_btn_press"
    )
    return interactionSource to scale
}

/**
 * Filled glass button — opaque accent background with glass frost overlay.
 * For the primary action (e.g. "Play All").
 *
 * Glass style: FloatingCard — medium glass with glow and shadow for depth.
 * Accent tint: fully opaque → reads as a filled button, not a translucent card.
 */
@Composable
fun GlassButtonPrimary(
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector? = null,
    accentColor: Color = AccentCyan,
    onClick: () -> Unit
) {
    val (interactionSource, pressScale) = rememberButtonPressScale()

    GlassSurface(
        modifier = modifier
            .height(48.dp)
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale },
        style = GlassTokens.FloatingCard,
        shape = RikoShape.Pill,
        tintColor = accentColor,
        showBorder = false
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(it, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, color = Color.White, style = RikoTypography.Button)
        }
    }
}

/**
 * Outlined glass button — transparent glass with accent border.
 * For the secondary action (e.g. "Shuffle").
 *
 * Glass style: Chip — ultra-thin glass with subtle border.
 * Border alpha comes from GlassTokens.Chip.borderAlpha.
 */
@Composable
fun GlassButtonSecondary(
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector? = null,
    accentColor: Color = AccentCyan,
    onClick: () -> Unit
) {
    val (interactionSource, pressScale) = rememberButtonPressScale()

    GlassSurface(
        modifier = modifier
            .height(48.dp)
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale },
        style = GlassTokens.Chip,
        shape = RikoShape.Pill,
        borderColor = accentColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(it, null, tint = accentColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, color = accentColor, style = RikoTypography.Button)
        }
    }
}
