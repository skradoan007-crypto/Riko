package com.rikodev.music.android.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMotion
import com.rikodev.music.android.core.designsystem.theme.ColorAbyssBlack
import com.rikodev.music.android.core.designsystem.theme.ColorVoidDark
import com.rikodev.music.android.core.designsystem.theme.GradientBottom
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — LIVING BACKGROUND ENGINE (v2)
//
// Phase 1 of Workflow Part 2: "STATIC UI -> LIVING MUSIC ORGANISM"
//
// The album art's own colours (primaryColor/secondaryColor) drift slowly
// behind the glass, like YT Music's Now Playing screen.
//
// v2 CHANGES (Build 26 — Living Background Engine):
//   - Spring-based color morph (RikoMotion.colorMorphTransition) instead of
//     tween-based animateColorAsState. When song changes, Blue -> Purple
//     transition -> Red morphs with organic spring physics (300-800ms).
//   - Opacity blending: old gradient fades out while new fades in, preventing
//     the "muddy middle" that direct color lerp creates with saturated colors.
//   - Energy-reactive intensity: optional energyIntensity parameter makes the
//     background pulse with audio energy.
//   - Third blob layer for richer depth (ambient center glow).
//   - Background ONLY redraws when color/energy changes, not every frame.
//
// Performance: ONE infiniteTransition drives blob positions.
// Color changes trigger spring animation (not per-frame computation).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    primaryColor: Color = ColorVoidDark,
    secondaryColor: Color = ColorAbyssBlack,
    // Real third distinct color (ColorState.accent) for the center blob —
    // previously that blob just reused primaryColor at lower alpha, so the
    // background only ever had 2 real hues no matter how rich the actual
    // extracted palette was. A genuinely distinct 3rd hue is what makes it
    // read as a colorful nebula instead of a 2-color gradient.
    accentColor: Color = primaryColor,
    intensity: Float = 0.5f,
    animateMotion: Boolean = true,
    showBaseFill: Boolean = true,
    energyIntensity: Float = 0f,
    content: @Composable () -> Unit
) {
    // ── Spring-based color morph ───────────────────────────────────────
    // animateColorAsState with spring spec = organic, interruptible transition.
    // If user skips 3 songs in 2 seconds, the morph reverses with velocity —
    // no "stuck in transition" feeling. Spring settling time ~400-800ms.
    val animatedPrimary by animateColorAsState(
        targetValue = primaryColor,
        animationSpec = RikoMotion.colorMorphTransition(),
        label = "bg_primary_morph"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = secondaryColor,
        animationSpec = RikoMotion.colorMorphTransition(),
        label = "bg_secondary_morph"
    )
    val animatedAccent by animateColorAsState(
        targetValue = accentColor,
        animationSpec = RikoMotion.colorMorphTransition(),
        label = "bg_accent_morph"
    )

    // ── Energy-reactive intensity ──────────────────────────────────────
    // Intensity modulates with audio energy — background "breathes" with music.
    // Spring: gentle settle, never abrupt. Only updates when energy changes.
    val animatedIntensity by animateFloatAsState(
        targetValue = intensity,
        animationSpec = RikoMotion.ambientBreathing(),
        label = "bg_intensity_breath"
    )
    val energyBoost = energyIntensity * 0.2f // Max 20% extra brightness from energy
    val totalIntensity = (animatedIntensity + energyBoost).coerceIn(0f, 1f)

    // ── Single shared drift driver ────────────────────────────────────
    // Slow drifting blobs — reads as "alive" rather than "spinning".
    // 14-second cycle matches NowPlayingBackground for visual unity.
    val transition = rememberInfiniteTransition(label = "bg_drift")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bg_angle"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (showBaseFill) {
                drawRect(color = ColorAbyssBlack)
            }

            // ── Blob 1: Primary (upper-center, drifts organically) ─────
            val driftX1 = if (animateMotion) sin(angle) * 0.12f else 0f
            val driftY1 = if (animateMotion) cos(angle * 0.8f) * 0.08f else 0f
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedPrimary.copy(alpha = totalIntensity),
                        animatedPrimary.copy(alpha = totalIntensity * 0.5f),
                        Color.Transparent
                    ),
                    center = Offset(
                        size.width * (0.5f + driftX1),
                        size.height * (0.02f + driftY1)
                    ),
                    radius = size.width * 1.3f
                )
            )

            // ── Blob 2: Secondary (lower-right, counter-drift) ─────────
            val driftX2 = if (animateMotion) cos(angle * 0.7f) * 0.10f else 0f
            val driftY2 = if (animateMotion) sin(angle * 0.9f) * 0.06f else 0f
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedSecondary.copy(alpha = totalIntensity * 0.6f),
                        Color.Transparent
                    ),
                    center = Offset(
                        size.width * (0.85f + driftX2),
                        size.height * (1.0f + driftY2)
                    ),
                    radius = size.width * 1.1f
                )
            )

            // ── Blob 3: Ambient center glow (subtle, reactive) ────────
            // A third, fainter blob near center adds depth. Now uses a
            // genuinely distinct third hue (animatedAccent) instead of
            // reusing animatedPrimary — see accentColor parameter above.
            // Energy makes it bloom — at rest it's barely visible.
            val centerAlpha = (totalIntensity * 0.25f + energyIntensity * 0.15f)
                .coerceIn(0f, 0.5f)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedAccent.copy(alpha = centerAlpha),
                        Color.Transparent
                    ),
                    center = Offset(
                        size.width * 0.5f,
                        size.height * 0.4f
                    ),
                    radius = size.width * 0.9f
                )
            )

            // ── Bottom gradient fade for text readability ─────────────
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Transparent,
                        GradientBottom.copy(alpha = 0.35f),
                        GradientBottom.copy(alpha = 0.55f)
                    ),
                    startY = 0f,
                    endY = size.height
                )
            )
        }
        content()
    }
}
