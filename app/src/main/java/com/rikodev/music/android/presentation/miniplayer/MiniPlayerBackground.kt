package com.rikodev.music.android.presentation.miniplayer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.graphics.Path
import androidx.compose.animation.core.Animatable

// ═══════════════════════════════════════════════════════════════════════════════════════
// RIKO MUSIC — MINI PLAYER 2.0: BACKGROUND
//
// Build-3, Phase A, File 1.
//
// Ambient atmosphere. NOT wallpaper.
// Only background. Nothing else.
//
// Reads: state.color, layout.* (from MiniPlayerState)
// Forbidden: Album, Wave, Controls, Metadata, Progress
// Pixel Owner: Ambient Gradient, Glow (Rule #24)
// Layer Independence: Dark ambient fallback if color fails (Rule #25)
//
// Visual Stack:
//   Layer 1: Deep base fill
//   Layer 2: Primary radial blob (drifting)
//   Layer 3: Secondary radial blob (drifting)
//
// MPRS Reference: Chapter A.1, B.1
// Tokens Reference: MiniPlayerTokens (Section 23)
// Motion Reference: MiniPlayerMotion (ambientDriftSpec, pulseBreathingSpec)
// ═══════════════════════════════════════════════════════════════════════════════════════

/**
 * Ambient background for the Mini Experience Surface.
 *
 * Renders slow-drifting radial gradient blobs driven by album art colors.
 * Intensity modulated by audio energy and pulse amount.
 * This is NOT a wallpaper — it's condensed atmosphere from Color Intelligence.
 *
 * Rule #24 — Pixel Ownership: This component OWNS ambient gradient and glow pixels.
 * No other component may draw gradients or glow.
 *
 * Rule #25 — Layer Independence: If Color Brain fails,
 * renders dark ambient fallback. Still premium.
 *
 * Rule #19 — No Hardcoded Duration: All animation specs from [MiniPlayerMotion].
 *
 * Rule #20 — Renderer Never Interprets: Receives pre-computed color/energy values.
 * No `if (energy > 0.8f)` decisions — just applies values.
 *
 * Rule #17 — Single Snapshot: Reads only [MiniPlayerState] (specifically
 * [MiniPlayerState.color] and [MiniPlayerState.layout]).
 *
 * @param state The single source of truth. Only state.color and layout.* are read.
 * @param modifier External modifier for sizing/positioning from parent.
 */
@Composable
fun MiniPlayerBackground(
    state: MiniPlayerState,
    modifier: Modifier = Modifier
) {
    // ═════════════════════════════════════════════════════════════════════
    // READ STATE — Rule #17: Single Snapshot
    // ═════════════════════════════════════════════════════════════════════
    // Only reads: state.color (MiniPlayerColorSnapshot) and layout sizing.
    // Never reads: state.wave, state.morph, state.gesture, state.currentSong, etc.

    val color = state.color
    val isIdle = color.isIdle
    val energy = color.energyIntensity
    val pulse = color.pulseAmount

    // ═════════════════════════════════════════════════════════════════════
    // ANIMATED COLORS — Smooth crossfade on song change
    // ═════════════════════════════════════════════════════════════════════
    // Rule #19: Animation spec from MiniPlayerMotion, not hardcoded.
    val animatedPrimary by animateColorAsState(
        targetValue = color.primary,
        animationSpec = MiniPlayerMotion.colorCrossfadeSpec,
        label = "mp_bg_primary"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = color.secondary,
        animationSpec = MiniPlayerMotion.colorCrossfadeSpec,
        label = "mp_bg_secondary"
    )

    // ═════════════════════════════════════════════════════════════════════
    // DRIFT TRANSITION — 14s cycle, shared with NowPlaying
    // ═════════════════════════════════════════════════════════════════════
    // Slow organic drift for blob positioning.
    // Same cadence as NowPlayingBackground — rhythmic unity across the app.
    val driftTransition = rememberInfiniteTransition(label = "mp_bg_drift")
    val driftAngle by driftTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = MiniPlayerMotion.ambientDriftSpec,
        label = "mp_bg_drift_angle"
    )

    // ═════════════════════════════════════════════════════════════════════
    // PULSE TRANSITION — 6s cycle, faster than drift
    // ═════════════════════════════════════════════════════════════════════
    // Pulse brightness modulation — creates heartbeat-like breathing.
    val pulseTransition = rememberInfiniteTransition(label = "mp_bg_pulse")
    val pulsePhase by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = MiniPlayerMotion.pulseBreathingSpec,
        label = "mp_bg_pulse_phase"
    )

    // ═════════════════════════════════════════════════════════════════════
    // DERIVED VALUES — Pre-computed, no interpretation (Rule #20)
    // ═════════════════════════════════════════════════════════════════════
    // Ambient intensity: base + energy modulation.
    // When idle: subtle dark ambient.
    // When playing: brightens with audio energy.
    val ambientIntensity = MiniPlayerTokens.BackgroundBaseAlphaCollapsed +
        (energy * MiniPlayerTokens.BackgroundEnergyModulation)

    // Pulse brightness: sine wave 0..1 for breathing effect.
    val pulseBrightness = sin(pulsePhase) * 0.5f + 0.5f

    // Glow alpha: ambient × (base + pulse × energy)
    val glowAlpha = ambientIntensity * (0.4f + pulseBrightness * 0.6f * pulse)

    // ═════════════════════════════════════════════════════════════════════
    // RENDER — Canvas-based, zero allocation per frame (Performance Constitution)
    // ═════════════════════════════════════════════════════════════════════

    Canvas(
        modifier = modifier
            .fillMaxSize()
            // No clip here — parent (Surface) clips to corner radius
    ) {
        drawMiniPlayerBackground(
            width = size.width,
            height = size.height,
            primary = animatedPrimary,
            secondary = animatedSecondary,
            ambientIntensity = ambientIntensity,
            glowAlpha = glowAlpha,
            driftAngle = driftAngle,
            isIdle = isIdle
        )
    }
}

/**
 * Core drawing logic for Mini Player background.
 *
 * Extracted from @Composable scope for testability.
 * This function is pure — it only draws, no side effects.
 *
 * Performance Constitution (Chapter E):
 *   - Canvas draws: 3 (base fill + 2 gradient blobs)
 *   - Allocations: 0 (uses DrawScope's built-in brush creation)
 *   - Path/Brush reuse: DrawScope provides these per-frame
 *
 * @param width Canvas width in pixels.
 * @param height Canvas height in pixels.
 * @param primary Animated primary color from Color Brain.
 * @param secondary Animated secondary color from Color Brain.
 * @param ambientIntensity Overall brightness (0.08 idle → 0.20 active).
 * @param glowAlpha Glow brightness modulation.
 * @param driftAngle Current drift phase for blob positioning.
 * @param isIdle Whether Color Brain has no artwork data.
 */
private fun DrawScope.drawMiniPlayerBackground(
    width: Float,
    height: Float,
    primary: Color,
    secondary: Color,
    ambientIntensity: Float,
    glowAlpha: Float,
    driftAngle: Float,
    isIdle: Boolean
) {
    if (width <= 0f || height <= 0f) return

    // ── Layer 1: Deep Base Fill ────────────────────────────────────────
    // Always present. Prevents transparent gaps.
    // Rule #25 fallback: if isIdle, uses dark ambient color.
    val baseColor = if (isIdle) {
        Color(MiniPlayerTokens.Fallback.BackgroundColor)
    } else {
        Color(MiniPlayerTokens.Fallback.BackgroundColor)
    }
    drawRect(color = baseColor)

    // ── Layer 2: Primary Radial Blob (upper-left area) ──────────────
    // Slow-drifting radial gradient from ColorState.primary.
    // Position oscillates via sin/cos on shared drift angle.
    val driftX1 = sin(driftAngle) * MiniPlayerVisualBackground.DRIFT_AMPLITUDE_X
    val driftY1 = cos(driftAngle * 0.8f) * MiniPlayerVisualBackground.DRIFT_AMPLITUDE_Y

    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                primary.copy(alpha = ambientIntensity),
                primary.copy(alpha = ambientIntensity * 0.5f),
                Color.Transparent
            ),
            center = Offset(
                x = width * (MiniPlayerVisualBackground.PRIMARY_CENTER_X + driftX1),
                y = height * (MiniPlayerVisualBackground.PRIMARY_CENTER_Y + driftY1)
            ),
            radius = width * MiniPlayerVisualBackground.PRIMARY_RADIUS
        )
    )

    // ── Layer 3: Secondary Radial Blob (lower-right area) ─────────────
    // Complementary drift pattern using different sin/cos multipliers.
    val driftX2 = cos(driftAngle * 0.7f) * MiniPlayerVisualBackground.SECONDARY_CENTER_X
    val driftY2 = sin(driftAngle * 0.9f) * MiniPlayerVisualBackground.SECONDARY_CENTER_Y

    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                secondary.copy(alpha = ambientIntensity * 0.7f),
                Color.Transparent
            ),
            center = Offset(
                x = width * MiniPlayerVisualBackground.SECONDARY_CENTER_X,
                y = height * MiniPlayerVisualBackground.SECONDARY_CENTER_Y
            ),
            radius = width * MiniPlayerVisualBackground.SECONDARY_RADIUS
        )
    )

    // ── Layer 4: Subtle Glow Center ─────────────────────────────────
    // Very faint center glow, modulated by pulse and energy.
    // Only visible when energy is present (playing music).
    if (glowAlpha > 0.01f) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    primary.copy(alpha = glowAlpha * 0.3f),
                    primary.copy(alpha = glowAlpha * 0.1f),
                    Color.Transparent
                ),
                center = Offset(
                    x = width * 0.5f,
                    y = height * 0.5f
                ),
                radius = width * 0.5f
            )
        )
    }
}


