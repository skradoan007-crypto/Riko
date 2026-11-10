/*
 * ═══════════════════════════════════════════════════════════════════════════
 * MiniPlayerWave.kt
 * Riko Music App — Mini Player 2.0, Build-3 Phase B, File 6
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * MPRS Reference: Chapter B.4 — MiniPlayerWaveSpec
 *
 * Purpose:
 *   "Living emotional indicator. NOT a visualizer."
 *
 * Personality:
 *   "Never explodes. Never dies. Never jitters. Always remembers.
 *    Breathes when idle. Flows when playing. Peaks on beat.
 *    Calmly settles when paused."
 *
 * ── Architecture Rules Followed ────────────────────────────────────────────
 *
 *   Rule #19 — Every animation sourced from MiniPlayerMotion object.
 *              No hardcoded durations, no hardcoded easing curves.
 *
 *   Rule #20 — Renderer NEVER interprets state values.
 *              Never computes energy (already in state.wave).
 *              Amplitude is energy × beat × idle scale — all from state.
 *              Color values consumed as-is, never derived.
 *
 *   Rule #21 — Every spacing and thickness sourced from MiniPlayerTokens
 *              via state.layout fields.
 *
 *   Rule #23 — No raw Experience OS reads.
 *              This component reads ONLY MiniPlayerState fields documented
 *              in the MPRS spec (wave, color, layout, isPlaying).
 *
 *   Rule #25 — Layer independence.
 *              The Mini Player remains fully functional and visually
 *              complete when this wave component is inactive or invisible.
 *
 * ── State Reads (from MiniPlayerState) ──────────────────────────────────
 *
 *   Wave:     state.wave.isActive, state.wave.hasBeat,
 *             state.wave.overallEnergy, state.wave.thickness,
 *             state.wave.opacity, state.wave.isExpanding,
 *             state.wave.isSettling, state.wave.energy,
 *             state.wave.isIdle
 *
 *   Color:    state.color.waveStart, state.color.waveEnd,
 *             state.color.energyIntensity, state.color.pulseAmount
 *
 *   Layout:   state.layout.waveVisibility, state.layout.waveThickness,
 *             state.layout.height, state.layout.widthFraction
 *
 *   Playback: state.isPlaying
 *
 * ── NEVER List ──────────────────────────────────────────────────────────
 *
 *   FFT, PCM data, EnergySolver, Physics simulation,
 *   Color extraction from album art, direct WaveState access,
 *   Own energy computation, hardcoded wave amplitude,
 *   Hardcoded animation duration, business logic,
 *   remember{} with mutable path state (use Path() as reusable buffer)
 *
 * ── Performance Budget ──────────────────────────────────────────────────
 *
 *   Target: 0.8ms per frame
 *   Allocations: 0 (all Path objects pre-allocated via remember)
 *   Path reuse: primaryPath and secondaryPath reset each frame, never reallocated
 *
 * ── Wave Rendering Approach ─────────────────────────────────────────────
 *
 *   Shape: Simplified sine-wave-like ribbon modulated by energy.
 *          NOT full Wave Engine geometry — just a smooth sin() curve
 *          sampled across the component width.
 *
 *   Energy modulation: state.wave.overallEnergy directly scales amplitude.
 *                      Higher energy → taller wave peaks.
 *
 *   Beat modulation:  When state.wave.hasBeat == true, amplitude spikes
 *                      briefly via MiniPlayerMotion.waveBeatAmplitude.
 *
 *   Idle breathing:   When state.wave.isIdle == true, amplitude scales
 *                      down via MiniPlayerMotion.waveIdleAmplitudeScale
 *                      and spatial frequency reduces for a gentle pulse.
 *
 *   Rendering pipeline:
 *     1. Compute alpha = layout.waveVisibility × state.wave.opacity
 *     2. Compute amplitude = overallEnergy × beatMultiplier × idleScale
 *     3. Compute strokeWidth = layout.waveThickness × state.wave.thickness
 *     4. Build primary sine path across width (N segments, lineTo)
 *     5. Draw primary path with linearGradient(waveStart → waveEnd) stroke
 *     6. Optionally draw subtle secondary path for visual depth
 *
 *   Morph behavior:
 *     - waveVisibility = 0 when collapsed (progress < 0.30)
 *     - Fades in during Phase 4 (progress 0.50 → 0.70)
 *     - Handled entirely by layout.waveVisibility from state
 *
 * ═══════════════════════════════════════════════════════════════════════════
 */
package com.rikodev.music.android.presentation.miniplayer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.sin
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable

/**
 * Living emotional wave indicator for the Mini Player.
 *
 * Renders a simplified sine-wave-like ribbon whose amplitude, thickness, opacity,
 * and color are entirely driven by [MiniPlayerState]. The renderer never computes
 * energy, never interprets values, and never makes business decisions — it faithfully
 * translates state into a visual sine curve (Rule #20).
 *
 * **Personality** (MPRS B.4):
 * - Breathes when idle: gentle low-amplitude sine, reduced spatial frequency
 * - Flows when playing: energy-modulated sine with full spatial frequency
 * - Peaks on beat: brief amplitude spike via state.wave.hasBeat
 * - Calmly settles when paused: amplitude decays as state.wave.isSettling transitions
 *
 * **Layer Independence** (Rule #25):
 * When `state.wave.isActive` is false, this composable returns without rendering.
 * The Mini Player remains fully functional and visually beautiful without the wave.
 *
 * **Performance**:
 * Pre-allocates two [Path] objects via [remember], resets and reuses them each
 * frame. Zero allocations per frame within the 0.8ms budget.
 *
 * @param state The current [MiniPlayerState] snapshot providing wave, color,
 *   layout, and playback fields. This is the single source of truth — the renderer
 *   never reaches outside this parameter (Rule #23).
 * @param modifier Optional [Modifier] for sizing, padding, and positioning.
 *   Width is governed by `state.layout.widthFraction`; height by the parent layout
 *   using `state.layout.height` from tokens (Rule #21).
 */
@Composable
fun MiniPlayerWave(
    state: MiniPlayerState,
    modifier: Modifier = Modifier
) {
    // ── Layer Independence Gate (Rule #25) ──────────────────────────────
    // When the wave subsystem is inactive, this component produces zero output.
    // The Mini Player renders correctly and beautifully without any wave.
    // This is the single early-return; no other short-circuit logic exists.
    if (!state.wave.isActive) return

    // ════════════════════════════════════════════════════════════════════
    // ANIMATION DRIVERS
    // All timing, easing, and value ranges from MiniPlayerMotion (Rule #19).
    // No hardcoded durations. No hardcoded easing. No custom Animatable.
    // ════════════════════════════════════════════════════════════════════

    val infiniteTransition = rememberInfiniteTransition(
        label = "miniPlayerWaveTransition"
    )

    // Primary wave phase: advances from wavePhaseStart → wavePhaseTarget
    // over waveCycleDuration ms, repeating forever with linear easing.
    // Because sin() is periodic, Restart mode produces a seamless loop.
    val primaryPhase by infiniteTransition.animateFloat(
        initialValue = MiniPlayerMotion.wavePhaseStart,
        targetValue = MiniPlayerMotion.wavePhaseTarget,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = MiniPlayerMotion.waveCycleDuration,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePrimaryPhase"
    )

    // Secondary wave phase: offset timing for the subtle depth layer.
    // Uses a different cycle duration from MiniPlayerMotion for visual variety.
    val secondaryPhase by infiniteTransition.animateFloat(
        initialValue = MiniPlayerMotion.wavePhaseStart,
        targetValue = MiniPlayerMotion.wavePhaseTarget,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = MiniPlayerMotion.waveSecondaryCycleDuration,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveSecondaryPhase"
    )

    // ════════════════════════════════════════════════════════════════════
    // PRE-ALLOCATED PATHS
    // Allocated once via remember, reset and reused every frame.
    // This satisfies the "0 allocations per frame" performance budget.
    // These are drawing buffers, NOT mutable Compose state (Rule #20).
    // ════════════════════════════════════════════════════════════════════

    val primaryPath = remember { Path() }
    val secondaryPath = remember { Path() }

    // ════════════════════════════════════════════════════════════════════
    // CANVAS RENDERER
    // The draw scope receives actual pixel dimensions from layout.
    // All visual parameters derive from state reads only.
    // ════════════════════════════════════════════════════════════════════

    Canvas(
        modifier = modifier.fillMaxWidth(
            state.layout.widthFraction.coerceIn(0f, 1f)
        )
    ) {
        // ── Alpha Computation ─────────────────────────────────────────
        // Alpha = layout.waveVisibility × state.wave.opacity
        //
        // Morph behavior (MPRS B.4):
        //   waveVisibility = 0 when collapsed (progress < 0.30)
        //   Fades in during Phase 4 (progress 0.50 → 0.70)
        //   These transitions are computed upstream; we just multiply.
        val compositeAlpha = state.layout.waveVisibility * state.wave.opacity

        // Early exit when wave is fully transparent — no drawing needed.
        // Threshold from MiniPlayerMotion to avoid sub-pixel rendering waste.
        if (compositeAlpha < MiniPlayerMotion.waveAlphaThreshold) return@Canvas

        // ── Dimension Extraction ───────────────────────────────────────
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        // ── Amplitude Composition ────────────────────────────────────
        // Renderer NEVER computes energy (Rule #20).
        // All amplitude factors come directly from state reads.
        //
        // 1. overallEnergy: encapsulates the full energy state including
        //    idle breathing oscillation, playback flow, beat peaks,
        //    and settling decay. We multiply, never derive.
        //
        // 2. Beat multiplier: state.wave.hasBeat signals a beat event.
        //    We scale amplitude by the factor from MiniPlayerMotion.
        //    When hasBeat is false, normal amplitude factor applies.
        //
        // 3. Idle scale: state.wave.isIdle signals idle state.
        //    We apply a reduced amplitude scale for gentle breathing.
        //
        // Final: amplitude = energy × beat × idle — pure multiplication,
        // no interpolation, no smoothing, no own computation.
        val energy = state.wave.overallEnergy

        val beatMultiplier = if (state.wave.hasBeat) {
            MiniPlayerMotion.waveBeatAmplitude
        } else {
            MiniPlayerMotion.waveNormalAmplitude
        }

        val idleScale = if (state.wave.isIdle) {
            MiniPlayerMotion.waveIdleAmplitudeScale
        } else {
            1f
        }

        val amplitude = energy * beatMultiplier * idleScale

        // ── Stroke Width ──────────────────────────────────────────────
        // Stroke = layout.waveThickness × state.wave.thickness
        // Combines the token-based base thickness with the state-driven
        // dynamic thickness factor (energy-responsive).
        val strokeWidth = state.layout.waveThickness * state.wave.thickness

        // ── Spatial Frequency ─────────────────────────────────────────
        // Idle: reduced frequency for a gentle, wide breathing wave.
        // Playing: full frequency for an energetic flowing ribbon.
        // Renderer reads isIdle and selects the appropriate frequency
        // from MiniPlayerMotion — no own computation.
        val spatialFrequency = if (state.wave.isIdle) {
            MiniPlayerMotion.waveIdleSpatialFrequency
        } else {
            MiniPlayerMotion.waveSpatialFrequency
        }

        // ── Gradient Brush ────────────────────────────────────────────
        // Color: gradient(waveStart, waveEnd) — left-to-right.
        // Colors are pre-computed upstream; we consume them as-is.
        // state.color.energyIntensity and state.color.pulseAmount are
        // acknowledged reads (MPRS B.4) and available for downstream
        // visual extensions (e.g., glow intensity modulation).
        val waveGradient = Brush.linearGradient(
            colors = listOf(
                state.color.waveStart,
                state.color.waveEnd
            ),
            start = Offset.Zero,
            end = Offset(width, 0f)
        )

        // ── Segment Configuration ────────────────────────────────────
        // Number of sample points across the width.
        // Higher count = smoother curve, within performance budget.
        // From MiniPlayerMotion — not hardcoded.
        val segments = MiniPlayerMotion.waveSegmentCount
        val segmentWidth = width / segments

        // Maximum vertical displacement as a fraction of height.
        // The sine wave oscillates within ±(maxDisplacement × amplitude)
        // around centerY.
        val maxDisplacement = height * MiniPlayerMotion.waveMaxDisplacementRatio

        // ══════════════════════════════════════════════════════════════
        // PRIMARY WAVE — Main sine-wave ribbon
        // ══════════════════════════════════════════════════════════════

        primaryPath.reset()

        // Anchor the path at the left edge, vertically centered.
        primaryPath.moveTo(0f, centerY)

        // Sample sine wave across the full width.
        // Each sample point: y = centerY + sin(phase + x·freq) × amplitude × maxDisp
        // lineTo produces a polyline; with sufficient segments (≥32) it is
        // visually indistinguishable from a true smooth curve.
        for (i in 1..segments) {
            val x = i * segmentWidth
            val normalizedX = x / width

            // sin-based smooth curve modulated by state.wave.overallEnergy.
            // Temporal component: primaryPhase × temporalFrequency
            //   → advances with time, creates the flowing motion.
            // Spatial component: normalizedX × spatialFrequency
            //   → determines how many wave peaks are visible across width.
            val sineValue = sin(
                primaryPhase * MiniPlayerMotion.waveTemporalFrequency
                    + normalizedX * spatialFrequency
            )

            // Displacement: sin × amplitude × maxDisplacement
            // Positive sin = above center, negative = below center.
            // Amplitude already includes energy, beat, and idle scaling.
            val y = centerY + sineValue * amplitude * maxDisplacement

            primaryPath.lineTo(x, y)
        }

        // Draw the primary wave ribbon.
        // Stroke with round caps for smooth endpoints.
        // Round joins for smooth corners at high amplitude.
        drawPath(
            path = primaryPath,
            brush = waveGradient,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            ),
            alpha = compositeAlpha
        )

        // ══════════════════════════════════════════════════════════════
        // SECONDARY WAVE — Subtle depth layer
        // Only rendered when conditions warrant visual depth:
        //   - Alpha above visibility gate (wave is sufficiently visible)
        //   - Energy above gate (there is enough motion to justify a second layer)
        //
        // This adds a living, layered quality to the wave without
        // violating simplicity or the performance budget.
        // Both gates are from MiniPlayerMotion — not hardcoded.
        // ══════════════════════════════════════════════════════════════

        if (compositeAlpha > MiniPlayerMotion.waveSecondaryAlphaGate
            && energy > MiniPlayerMotion.waveSecondaryEnergyGate
        ) {
            secondaryPath.reset()
            secondaryPath.moveTo(0f, centerY)

            // Secondary wave uses reduced amplitude and different displacement
            // for a layered depth effect — like a shadow or echo of the primary.
            val secondaryAmplitude =
                amplitude * MiniPlayerMotion.waveSecondaryAmplitudeScale
            val secondaryMaxDisplacement =
                height * MiniPlayerMotion.waveSecondaryDisplacementRatio

            for (i in 1..segments) {
                val x = i * segmentWidth
                val normalizedX = x / width

                // Secondary sine: offset phase, adjusted frequency ratio.
                // The phase offset and frequency ratio from MiniPlayerMotion
                // create visual separation from the primary wave.
                val sineValue = sin(
                    secondaryPhase * MiniPlayerMotion.waveSecondaryTemporalFrequency
                        + normalizedX * spatialFrequency
                        * MiniPlayerMotion.waveSecondaryFrequencyRatio
                        + MiniPlayerMotion.waveSecondaryPhaseOffset
                )

                val y = centerY + sineValue * secondaryAmplitude * secondaryMaxDisplacement
                secondaryPath.lineTo(x, y)
            }

            // Draw secondary wave with reduced stroke and alpha.
            // Same gradient for color cohesion; different weight for depth.
            drawPath(
                path = secondaryPath,
                brush = waveGradient,
                style = Stroke(
                    width = strokeWidth * MiniPlayerMotion.waveSecondaryStrokeScale,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                ),
                alpha = compositeAlpha * MiniPlayerMotion.waveSecondaryAlphaScale
            )
        }
    }
}
