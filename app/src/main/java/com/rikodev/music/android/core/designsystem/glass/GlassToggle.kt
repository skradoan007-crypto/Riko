package com.rikodev.music.android.core.designsystem.glass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMotion
import com.rikodev.music.android.core.designsystem.theme.AccentCyan
import com.rikodev.music.android.core.designsystem.theme.GlassWhite08
import com.rikodev.music.android.core.designsystem.theme.GlassBorderLight
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — GLASS TOGGLE
//
// Replaces the stock Material3 Switch everywhere (EQ on/off, Virtualizer,
// BassBoost, every Settings row). Stock Switch is a flat filled pill with a
// plain circular thumb — reads as generic Android, not Liquid Glass.
//
// This is a frosted glass track with a floating glass bubble thumb: same
// specular-highlight DNA as every other bubble in the app, animated with
// spring physics instead of Material's linear slide.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GlassToggle(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color = AccentCyan,
    gradientEnd: Color? = null,
    enabled: Boolean = true
) {
    val trackWidth = 52.dp
    val trackHeight = 30.dp
    val thumbSize = 24.dp
    val padding = 3.dp

    // ON-state track gradient start→end; defaults to a flat accent wash so
    // existing call sites (Settings rows, Queue, …) keep their look.
    val onColors = if (gradientEnd != null) {
        listOf(accentColor, gradientEnd)
    } else {
        listOf(accentColor, accentColor)
    }

    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = RikoMotion.MediumSpring(),
        label = "toggle_thumb"
    )

    val trackAlpha by animateFloatAsState(
        targetValue = if (checked) 0.9f else 1f,
        animationSpec = RikoMotion.colorTransition(),
        label = "toggle_track_alpha"
    )

    Canvas(
        modifier = modifier
            .width(trackWidth)
            .height(trackHeight)
            .semantics { role = Role.Switch }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = { onCheckedChange(!checked) }
            )
    ) {
        val trackRadius = size.height / 2f
        val effectiveAlpha = if (enabled) 1f else 0.4f

        // Track — frosted when off, accent-tinted glass when on.
        // Gradient mode (gradientEnd set) reads nearly opaque like the
        // reference toggles; flat mode keeps the original soft wash.
        drawRoundRect(
            brush = if (checked) {
                if (gradientEnd != null) {
                    Brush.horizontalGradient(
                        listOf(
                            onColors[0].copy(alpha = 0.92f * effectiveAlpha),
                            onColors[1].copy(alpha = 0.92f * effectiveAlpha)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(
                            accentColor.copy(alpha = 0.55f * effectiveAlpha),
                            accentColor.copy(alpha = 0.35f * effectiveAlpha)
                        )
                    )
                }
            } else {
                Brush.horizontalGradient(
                    listOf(
                        GlassWhite08.copy(alpha = effectiveAlpha),
                        GlassWhite08.copy(alpha = effectiveAlpha)
                    )
                )
            },
            cornerRadius = CornerRadius(trackRadius, trackRadius)
        )

        // Track border
        drawRoundRect(
            color = GlassBorderLight.copy(alpha = (if (checked) 0.4f else 1f) * effectiveAlpha),
            cornerRadius = CornerRadius(trackRadius, trackRadius),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
        )

        // Thumb position
        val thumbRadiusPx = (thumbSize / 2).toPx()
        val paddingPx = padding.toPx()
        val minCenterX = paddingPx + thumbRadiusPx
        val maxCenterX = size.width - paddingPx - thumbRadiusPx
        val thumbCenterX = minCenterX + (maxCenterX - minCenterX) * thumbOffset
        val thumbCenterY = size.height / 2f

        // Thumb glow (when on)
        if (checked) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentColor.copy(alpha = 0.5f * effectiveAlpha), Color.Transparent),
                    center = Offset(thumbCenterX, thumbCenterY),
                    radius = thumbRadiusPx * 2.2f
                ),
                radius = thumbRadiusPx * 2.2f,
                center = Offset(thumbCenterX, thumbCenterY)
            )
        }

        // Glass thumb bubble
        drawCircle(
            color = Color.White.copy(alpha = 0.95f * effectiveAlpha),
            radius = thumbRadiusPx,
            center = Offset(thumbCenterX, thumbCenterY)
        )
        drawCircle(
            color = Color(0x1F211A12).copy(alpha = (if (checked) 0.15f else 0.30f) * effectiveAlpha),
            radius = thumbRadiusPx,
            center = Offset(thumbCenterX, thumbCenterY),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    (if (checked) accentColor else Color.White).copy(alpha = 0.3f * effectiveAlpha),
                    Color.Transparent
                ),
                center = Offset(thumbCenterX - thumbRadiusPx * 0.3f, thumbCenterY - thumbRadiusPx * 0.3f),
                radius = thumbRadiusPx
            ),
            radius = thumbRadiusPx * 0.9f,
            center = Offset(thumbCenterX, thumbCenterY)
        )
        // Thumb specular highlight
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.9f * effectiveAlpha), Color.Transparent),
                center = Offset(thumbCenterX - thumbRadiusPx * 0.35f, thumbCenterY - thumbRadiusPx * 0.4f),
                radius = thumbRadiusPx * 0.6f
            ),
            topLeft = Offset(thumbCenterX - thumbRadiusPx * 0.7f, thumbCenterY - thumbRadiusPx * 0.7f),
            size = Size(thumbRadiusPx * 0.9f, thumbRadiusPx * 0.7f)
        )
    }
}
