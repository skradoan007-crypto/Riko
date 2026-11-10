package com.rikodev.music.android.core.designsystem.glass

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.theme.AccentCyan
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — GLASS LOADING INDICATOR
//
// Replaces stock Material CircularProgressIndicator (used while the library
// scan / search is in flight). A rotating glass ring with a bright gradient
// arc, matching the same visual language as everything else — not a plain
// Material spinner.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GlassLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    accentColor: Color = AccentCyan,
    secondaryColor: Color = Color.White
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glass_loader")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing)
        ),
        label = "loader_rotation"
    )

    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = size.toPx() * 0.09f

        // Faint full ring — the "glass track"
        drawCircle(
            color = Color(0x14211A12),
            radius = (this.size.minDimension - strokeWidth) / 2f,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Bright rotating arc
        rotate(rotation) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        accentColor.copy(alpha = 0.3f),
                        accentColor,
                        secondaryColor
                    )
                ),
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}
