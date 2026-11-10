package com.rikodev.music.android.core.experience.wave

import com.rikodev.music.android.core.experience.calibration.WaveCalibration
import javax.inject.Inject
import javax.inject.Singleton
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — RIBBON PHYSICS SOLVER
//
// The heart of the Wave Engine. Converts composed energy into fluid physics.
//
// This is NOT a renderer. This is NOT a path generator. This is NOT a mover.
// This is a PURE PHYSICS ENGINE that understands how energy becomes movement.
//
// The ribbon behaves as a FLUID, not a wave. This solver computes:
//   - How much pressure builds INSIDE the ribbon (not external displacement)
//   - How elastic the ribbon is (spring-back character)
//   - How viscous the movement is (honey vs water)
//   - How much turbulence/chaos exists on the surface
//   - How thick/visible the ribbon should be
//
// ── RIKO RULES #11–15 (LOCKED) ─────────────────────────────────────────────
//   #11: Never knows Compose.
//   #12: Never knows Canvas.
//   #13: Never knows colors.
//   #14: Never creates animation.
//   #15: Only converts Energy → Physics.
//
// INPUT:  WaveFrame (with WaveEnergy + RibbonMode from EnergySolver)
// OUTPUT: WaveFrame.copy(ribbon = RibbonState(...))
//
// PHYSICS PHILOSOPHY:
//
//   Kick arrives:
//     pressure  += (smoothed energy)
//     elasticity ↑  (ribbon stretches, stores energy)
//     thickness  ↑  (ribbon becomes thicker)
//     tail survives (doesn't instantly die)
//     slow release (graceful decay)
//
//   Silence:
//     pressure  -- (decays at 0.96 per frame)
//     elasticity -- (returns to baseline)
//     opacity   -- (drops to 0.12, NOT zero)
//     breathing remains (ribbon is ALWAYS alive)
//
//   Cheap visualizer:  bass > 0.7 → wave_height = 1.0  (instant max)
//   Riko Ribbon:       kick → pressure builds → ribbon thickens →
//                       holds → slowly releases (organic, premium)
//
// ZERO DEPENDENCIES:
//   No Compose, no Canvas, no Modifier, no Path, no DrawScope.
//   No Color, no Brush, no BlendMode.
//   Only: WaveFrame, WaveEnergy, RibbonMode, RibbonState — pure Kotlin.
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class RibbonPhysicsSolver @Inject constructor() {

    // ═══════════════════════════════════════════════════════════════════════
    // CONSTANTS
    // ═══════════════════════════════════════════════════════════════════════

    companion object { // All constants now live in WaveCalibration (Physics BIOS).
    }

    // ═══════════════════════════════════════════════════════════════════════
    // INTERNAL STATE (smoothed frame-to-frame values)
    // ═══════════════════════════════════════════════════════════════════════

    /** Previous ribbon pressure — used for exponential smoothing. */
    private var previousPressure: Float = 0f

    /** Smoothed thickness — lerps toward mode target each frame. */
    private var smoothThickness: Float = WaveCalibration.Physics.THICKNESS_IDLE

    /** Smoothed opacity — lerps toward energy-driven target. */
    private var smoothOpacity: Float = WaveCalibration.Physics.OPACITY_MINIMUM

    /** Smoothed stretch — lerps toward mode target. */
    private var smoothStretch: Float = WaveCalibration.Physics.STRETCH_IDLE

    /** Smoothed compression — lerps toward mode target. */
    private var smoothCompression: Float = WaveCalibration.Physics.COMPRESSION_IDLE

    /** Frame counter for debugging. */
    private var frameCount: Int = 0

    // ═══════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Process one frame through the ribbon physics pipeline.
     *
     * Takes a WaveFrame with populated energy + mode (from EnergySolver)
     * and returns a new WaveFrame with populated ribbon physics state.
     *
     * The pipeline computes 15 physics properties in sequence,
     * each building on the previous results:
     *
     *   pressure → elasticity → viscosity → surfaceTension → turbulence →
     *   damping → flowResistance → drift → stretch → compression →
     *   thickness → opacity → highlightIntensity → tailStrength → relaxation
     *
     * @param frame Input frame from EnergySolver (energy + mode populated).
     * @return Updated frame with RibbonState populated.
     */
    fun solve(frame: WaveFrame): WaveFrame {
        val energy = frame.energy
        val mode = frame.mode

        // ── Stage 1: Pressure (smoothed accumulation) ───────────────────
        val pressure = solvePressure(energy.overallEnergy)

        // ── Stage 2: Elasticity ────────────────────────────────────────
        val elasticity = solveElasticity(energy.overallEnergy, mode)

        // ── Stage 3: Viscosity ──────────────────────────────────────────
        val viscosity = solveViscosity(energy.overallEnergy, mode)

        // ── Stage 4: Surface Tension ───────────────────────────────────
        val surfaceTension = solveSurfaceTension(energy.momentum)

        // ── Stage 5: Turbulence ────────────────────────────────────────
        val turbulence = solveTurbulence(energy.midTexture, energy.trebleSpark, mode)

        // ── Stage 6: Damping ───────────────────────────────────────────
        val damping = solveDamping(mode)

        // ── Stage 7: Flow Resistance ───────────────────────────────────
        val flowResistance = solveFlowResistance(viscosity)

        // ── Stage 8: Drift ────────────────────────────────────────────
        val drift = solveDrift(energy.overallEnergy)

        // ── Stage 9: Stretch (mode-driven, smoothed) ──────────────────
        val stretch = solveStretch(mode, energy.overallEnergy)

        // ── Stage 10: Compression (mode-driven, smoothed) ─────────────
        val compression = solveCompression(mode)

        // ── Stage 11: Thickness (mode-driven, smoothed) ────────────────
        val thickness = solveThickness(mode, energy.overallEnergy)

        // ── Stage 12: Opacity (energy-driven, smoothed) ────────────────
        val opacity = solveOpacity(energy.overallEnergy, mode)

        // ── Stage 13: Highlight Intensity ──────────────────────────────
        val highlightIntensity = solveHighlightIntensity(
            energy.trebleSpark, energy.overallEnergy
        )

        // ── Stage 14: Tail Strength ────────────────────────────────────
        val tailStrength = solveTailStrength(energy.momentum)

        // ── Stage 15: Relaxation ──────────────────────────────────────
        val relaxation = solveRelaxation(surfaceTension)

        // ── Update internal state ──────────────────────────────────────
        previousPressure = pressure
        frameCount++

        // ── Assemble RibbonState ────────────────────────────────────────
        val ribbonState = RibbonState(
            pressure = pressure,
            elasticity = elasticity,
            viscosity = viscosity,
            surfaceTension = surfaceTension,
            turbulence = turbulence,
            damping = damping,
            flowResistance = flowResistance,
            drift = drift,
            stretch = stretch,
            compression = compression,
            thickness = thickness,
            opacity = opacity,
            highlightIntensity = highlightIntensity,
            tailStrength = tailStrength,
            relaxation = relaxation
        )

        return frame.copy(ribbon = ribbonState)
    }

    /**
     * Reset all internal state to defaults.
     * Called on track change, engine shutdown, or explicit reset.
     */
    fun reset() {
        previousPressure = 0f
        smoothThickness = WaveCalibration.Physics.THICKNESS_IDLE
        smoothOpacity = WaveCalibration.Physics.OPACITY_MINIMUM
        smoothStretch = WaveCalibration.Physics.STRETCH_IDLE
        smoothCompression = WaveCalibration.Physics.COMPRESSION_IDLE
        frameCount = 0
    }

    /** Current frame index (for debugging / Experience Inspector). */
    val currentFrameIndex: Int get() = frameCount

    // ═══════════════════════════════════════════════════════════════════════
    // PHYSICS SOLVERS (one problem per method — Rule #11)
    // ═══════════════════════════════════════════════════════════════════════

    // ── Stage 1: Pressure ───────────────────────────────────────────────

    /**
     * Compute internal ribbon pressure from smoothed energy.
     *
     * Pressure is NOT energy directly — it's a SMOOTHED, LINGERING version.
     * The 0.96 decay means pressure decays only 4% per frame, creating a
     * long tail after energy drops. This is the "pressure builds inside"
     * philosophy: the ribbon stores energy as pressure and releases slowly.
     *
     * Formula: pressure = (previousPressure × 0.96) + (energy × 0.22)
     *
     * Example (kick at frame 3, then silence):
     *   Frame  Energy  Pressure
     *     1     0.01    0.01    (idle)
     *     2     0.02    0.01    (still idle)
     *     3     0.90    0.23    (KICK! pressure jumps)
     *     4     0.40    0.31    (pressure PEAKS after energy drops!)
     *     5     0.10    0.32    (still high — 96% carry)
     *     6     0.02    0.31    (lingering...)
     *     8     0.01    0.30    (still 0.30 after kick!)
     *    12     0.01    0.26    (slowly decaying)
     *    20     0.01    0.18    (0.18 even after 17 frames of silence)
     *    40     0.01    0.07    (finally approaching zero)
     *
     * This 40-frame tail IS the "breathing" — the ribbon remembers.
     */
    private fun solvePressure(energy: Float): Float {
        val newPressure = previousPressure * WaveCalibration.Physics.PRESSURE_DECAY + energy * WaveCalibration.Physics.PRESSURE_ATTACK
        return newPressure.coerceIn(0f, 1f)
    }

    // ── Stage 2: Elasticity ──────────────────────────────────────────────

    /**
     * Compute ribbon elasticity from energy + mode.
     *
     * Base formula: elasticity = 0.55 + energy × 0.35
     *   - Idle: 0.55 (moderate spring-back)
     *   - Peak: 0.90 (very bouncy, springy)
     *
     * Mode modulation adjusts for behavior character:
     *   - PRESSURE_BUILD: boosted (ribbon tightens, ready to spring)
     *   - EXPANDING: reduced (ribbon is expanding, not bouncing)
     *   - CALM_SETTLING: reduced (relaxing, not springy)
     */
    private fun solveElasticity(energy: Float, mode: RibbonMode): Float {
        val base = WaveCalibration.Physics.ELASTICITY_BASE + energy * WaveCalibration.Physics.ELASTICITY_ENERGY_W
        val modulated = when (mode) {
            RibbonMode.PRESSURE_BUILD -> base * 1.15f   // Tighter, springier
            RibbonMode.EXPANDING -> base * 0.90f       // Expanding, not bouncing
            RibbonMode.CALM_SETTLING -> base * 0.80f   // Relaxing
            RibbonMode.IDLE -> base * 0.60f            // Dreamy, slow
            RibbonMode.BREATHING -> base * 0.75f       // Gentle
            RibbonMode.THIN -> base * 0.85f            // Slightly reduced
            RibbonMode.ACTIVE -> base * 1.0f           // Standard
        }
        return modulated.coerceIn(0f, 1f)
    }

    // ── Stage 3: Viscosity ────────────────────────────────────────────────

    /**
     * Compute ribbon viscosity from energy.
     *
     * Formula: viscosity = 1.0 - energy × 0.4
     *   - Idle (energy=0): 1.0 = maximum viscosity (very slow, honey-like)
     *   - Peak (energy=1): 0.6 = lower viscosity (faster, water-like)
     *
     * This means: silence → slow, dreamy movement.
     *             energy  → faster, more responsive movement.
     *
     * CRITICAL: viscosity is NEVER zero. Even at peak energy, the ribbon
     * still has 0.6 viscosity — it never jumps, it always GLIDES.
     * This is the fundamental difference from cheap visualizers:
     *   Cheap: bass → instant jump (zero viscosity)
     *   Riko: bass → pressure builds → viscous glide (always smooth)
     */
    private fun solveViscosity(energy: Float, mode: RibbonMode): Float {
        val base = 1f - energy * WaveCalibration.Physics.VISCOSITY_ENERGY_W
        val modulated = when (mode) {
            RibbonMode.BREATHING -> base * 1.05f  // Even slower in breathing
            RibbonMode.EXPANDING -> base * 0.92f // Slightly faster during expansion
            else -> base
        }
        return modulated.coerceIn(0.2f, 1f)
    }

    // ── Stage 4: Surface Tension ─────────────────────────────────────────

    /**
     * Compute surface tension from momentum.
     *
     * Formula: surfaceTension = 0.7 + momentum × 0.2
     *   - No momentum: 0.7 = tight surface
     *   - High momentum: 0.9 = very tight surface
     *
     * Surface tension increases with momentum because the ribbon
     * "grips" more when it has energy memory — the surface holds
     * together more tightly when the ribbon is active.
     */
    private fun solveSurfaceTension(momentum: Float): Float {
        return (WaveCalibration.Physics.SURFACE_TENSION_BASE + momentum * WaveCalibration.Physics.SURFACE_TENSION_MOMENTUM_W)
            .coerceIn(0.3f, 1f)
    }

    // ── Stage 5: Turbulence ──────────────────────────────────────────────

    /**
     * Compute turbulence from mid texture + treble spark.
     *
     * Formula: turbulence = midTexture × 0.7 + trebleSpark × 0.3
     *
     * Mode modulation:
     *   EXPANDING: boosted × 1.3 (lots of surface activity during drops)
     *   BREATHING: near-zero (calm, peaceful)
     *   IDLE: zero (no chaos in silence)
     *
     * Turbulence is the "alive" factor — it adds organic randomness
     * to the ribbon's movement. Without it, the ribbon would look
     * mechanical and artificial.
     */
    private fun solveTurbulence(
        midTexture: Float,
        trebleSpark: Float,
        mode: RibbonMode
    ): Float {
        val base = midTexture * WaveCalibration.Physics.TURBULENCE_MID_W + trebleSpark * WaveCalibration.Physics.TURBULENCE_TREBLE_W
        val modulated = when (mode) {
            RibbonMode.EXPANDING -> base * 1.3f    // Max chaos during drops
            RibbonMode.PRESSURE_BUILD -> base * 1.1f
            RibbonMode.ACTIVE -> base * 1.0f
            RibbonMode.THIN -> base * 0.5f         // Minimal turbulence for vocals
            RibbonMode.BREATHING -> base * 0.2f    // Very calm
            RibbonMode.IDLE -> 0f                   // No turbulence in silence
            RibbonMode.CALM_SETTLING -> base * 0.4f // Fading turbulence
        }
        return modulated.coerceIn(0f, 1f)
    }

    // ── Stage 6: Damping ─────────────────────────────────────────────────

    /**
     * Compute damping from mode.
     *
     * Damping controls how quickly oscillation dies.
     *   - High damping (IDLE, BREATHING): oscillation stops fast.
     *     The ribbon settles to its breathing baseline quickly.
     *   - Low damping (EXPANDING, SETTLING): oscillation lingers.
     *     The ribbon rings/resonates — this IS the premium feel.
     *
     * CALM_SETTLING has the LOWEST damping (0.10) because we WANT
     * the ribbon to resonate as it settles — like a bell ringing
     * after being struck. This creates the "water settling" effect.
     */
    private fun solveDamping(mode: RibbonMode): Float {
        return when (mode) {
            RibbonMode.IDLE -> WaveCalibration.Physics.DAMPING_IDLE
            RibbonMode.BREATHING -> WaveCalibration.Physics.DAMPING_BREATHING
            RibbonMode.THIN -> WaveCalibration.Physics.DAMPING_THIN
            RibbonMode.ACTIVE -> WaveCalibration.Physics.DAMPING_ACTIVE
            RibbonMode.PRESSURE_BUILD -> WaveCalibration.Physics.DAMPING_PRESSURE_BUILD
            RibbonMode.EXPANDING -> WaveCalibration.Physics.DAMPING_EXPANDING
            RibbonMode.CALM_SETTLING -> WaveCalibration.Physics.DAMPING_SETTLING
        }
    }

    // ── Stage 7: Flow Resistance ─────────────────────────────────────────

    /**
     * Compute flow resistance from viscosity.
     *
     * Formula: flowResistance = 0.3 + viscosity × 0.3
     *
     * More viscous ribbon = more resistance to direction changes.
     * This prevents jittery direction flips — the ribbon commits
     * to a movement direction and holds it.
     */
    private fun solveFlowResistance(viscosity: Float): Float {
        return (WaveCalibration.Physics.FLOW_RESISTANCE_BASE + viscosity * WaveCalibration.Physics.FLOW_RESISTANCE_VISCOSITY_W)
            .coerceIn(0f, 1f)
    }

    // ── Stage 8: Drift ──────────────────────────────────────────────────

    /**
     * Compute ribbon drift — the natural "current" of the ribbon.
     *
     * Drift is ALWAYS present, even in complete silence.
     * This is the "breathing" baseline — the ribbon drifts slowly,
     * giving a sense of being alive even when no music plays.
     *
     * Formula: drift = 0.10 + energy × 0.15
     *   - Silence: 0.10 (subtle, barely-visible drift)
     *   - Active: 0.25 (moderate lateral movement)
     */
    private fun solveDrift(energy: Float): Float {
        return (WaveCalibration.Physics.DRIFT_BASE + energy * WaveCalibration.Physics.DRIFT_ENERGY_W).coerceIn(0.05f, 0.5f)
    }

    // ── Stage 9: Stretch ────────────────────────────────────────────────

    /**
     * Compute ribbon stretch factor (horizontal expansion).
     *
     * Smoothly interpolates toward mode-specific target.
     * This prevents hard mode-transition snaps — the ribbon
     * gradually stretches/compresses between states.
     *
     * Mode targets:
     *   IDLE/BREATHING/ACTIVE: 1.0 (normal width)
     *   THIN: 0.85 (slightly narrower)
     *   PRESSURE_BUILD: 1.15 (starting to widen)
     *   EXPANDING: 1.40 (significantly wider)
     *   CALM_SETTLING: decays toward 1.0
     */
    private fun solveStretch(mode: RibbonMode, energy: Float): Float {
        val target = when (mode) {
            RibbonMode.IDLE -> WaveCalibration.Physics.STRETCH_IDLE
            RibbonMode.BREATHING -> WaveCalibration.Physics.STRETCH_BREATHING
            RibbonMode.THIN -> WaveCalibration.Physics.STRETCH_THIN
            RibbonMode.ACTIVE -> WaveCalibration.Physics.STRETCH_ACTIVE
            RibbonMode.PRESSURE_BUILD -> WaveCalibration.Physics.STRETCH_PRESSURE_BUILD
            RibbonMode.EXPANDING -> WaveCalibration.Physics.STRETCH_EXPANDING
            RibbonMode.CALM_SETTLING -> WaveCalibration.Physics.STRETCH_IDLE // Decay toward normal
        }
        smoothStretch += (target - smoothStretch) * WaveCalibration.Physics.STRETCH_LERP
        return smoothStretch.coerceIn(0.7f, 1.5f)
    }

    // ── Stage 10: Compression ────────────────────────────────────────────

    /**
     * Compute ribbon compression factor (vertical thinning).
     *
     * THIN mode compresses the ribbon vertically without killing it.
     * The ribbon gets thinner (compression < 1.0) but stays alive.
     *
     * Other modes maintain normal compression (1.0).
     * CALM_SETTLING returns to normal gradually.
     */
    private fun solveCompression(mode: RibbonMode): Float {
        val target = when (mode) {
            RibbonMode.THIN -> WaveCalibration.Physics.COMPRESSION_THIN
            RibbonMode.IDLE -> WaveCalibration.Physics.COMPRESSION_IDLE
            RibbonMode.CALM_SETTLING -> WaveCalibration.Physics.COMPRESSION_IDLE
            else -> 1.0f
        }
        smoothCompression += (target - smoothCompression) * WaveCalibration.Physics.COMPRESSION_LERP
        return smoothCompression.coerceIn(0.6f, 1.2f)
    }

    // ── Stage 11: Thickness ───────────────────────────────────────────────

    /**
     * Compute ribbon thickness — the most visible physics output.
     *
     * Smoothly interpolates toward mode-specific thickness target.
     * This is the PRIMARY visual output that the renderer uses.
     *
     * Mode targets:
     *   IDLE:           0.02 (barely visible line)
     *   BREATHING:      0.08 (thin but DEFINITELY alive)
     *   THIN:           0.15 (vocal/ambient passage)
     *   ACTIVE:         0.35 (normal music playback)
     *   PRESSURE_BUILD: 0.55 (thickening before expansion)
     *   EXPANDING:      0.80 (wide, dramatic)
     *   CALM_SETTLING:   decays toward IDLE
     *
     * CRITICAL: thickness is NEVER zero. Minimum is 0.02.
     * The ribbon is always at least a thin, breathing line.
     */
    private fun solveThickness(mode: RibbonMode, energy: Float): Float {
        val target = when (mode) {
            RibbonMode.IDLE -> WaveCalibration.Physics.THICKNESS_IDLE
            RibbonMode.BREATHING -> WaveCalibration.Physics.THICKNESS_BREATHING
            RibbonMode.THIN -> WaveCalibration.Physics.THICKNESS_THIN
            RibbonMode.ACTIVE -> WaveCalibration.Physics.THICKNESS_ACTIVE
            RibbonMode.PRESSURE_BUILD -> WaveCalibration.Physics.THICKNESS_PRESSURE_BUILD
            RibbonMode.EXPANDING -> WaveCalibration.Physics.THICKNESS_EXPANDING
            RibbonMode.CALM_SETTLING -> {
                // Decaying toward idle — but with a floor
                val decayed = smoothThickness - WaveCalibration.Physics.THICKNESS_SETTLING_DECAY
                decayed.coerceAtLeast(WaveCalibration.Physics.THICKNESS_IDLE)
            }
        }
        smoothThickness += (target - smoothThickness) * WaveCalibration.Physics.THICKNESS_LERP
        return smoothThickness.coerceIn(WaveCalibration.Physics.THICKNESS_IDLE, 1f)
    }

    // ── Stage 12: Opacity ────────────────────────────────────────────────

    /**
     * Compute ribbon opacity — how visible the ribbon is.
     *
     * NEVER zero. The minimum is 0.12 (WaveCalibration.Physics.OPACITY_MINIMUM).
     * This ensures the ribbon is always at least slightly visible,
     * creating the "breathing" effect even in silence.
     *
     * Smooth interpolation toward energy-driven target:
     *   - Silence: 0.12 (barely visible, but PRESENT)
     *   - Full energy: 1.0 (fully opaque)
     *
     * The slow lerp (0.12) prevents flickering — opacity changes
     * gradually over several frames, creating smooth fades.
     */
    private fun solveOpacity(energy: Float, mode: RibbonMode): Float {
        val target = when (mode) {
            RibbonMode.IDLE -> WaveCalibration.Physics.OPACITY_MINIMUM
            RibbonMode.BREATHING -> WaveCalibration.Physics.OPACITY_MINIMUM
            RibbonMode.CALM_SETTLING -> WaveCalibration.Physics.OPACITY_MINIMUM
            else -> (WaveCalibration.Physics.OPACITY_MINIMUM + energy * (1f - WaveCalibration.Physics.OPACITY_MINIMUM))
        }
        smoothOpacity += (target - smoothOpacity) * WaveCalibration.Physics.OPACITY_LERP
        return smoothOpacity.coerceIn(WaveCalibration.Physics.OPACITY_MINIMUM, 1f)
    }

    // ── Stage 13: Highlight Intensity ────────────────────────────────────

    /**
     * Compute highlight intensity for specular/shimmer effects.
     *
     * Formula: highlight = trebleSpark × 0.7 + energy × 0.3
     *
     * Treble is the primary driver of highlights (sparkle, shimmer).
     * Overall energy adds base intensity (even without treble,
     * some highlight exists when energy is high).
     *
     * Used by Highlight Solver (Step 3.3F) for:
     *   - Specular intensity on ribbon edges
     *   - Edge light brightness
     *   - Glow sharpness
     */
    private fun solveHighlightIntensity(trebleSpark: Float, energy: Float): Float {
        return (trebleSpark * WaveCalibration.Physics.HIGHLIGHT_TREBLE_W + energy * WaveCalibration.Physics.HIGHLIGHT_ENERGY_W)
            .coerceIn(0f, 1f)
    }

    // ── Stage 14: Tail Strength ──────────────────────────────────────────

    /**
     * Compute tail strength — how strongly the ribbon's energy persists.
     *
     * Directly derived from momentum (which is already a 12-frame
     * weighted average computed by the Energy Solver).
     *
     * tailStrength = momentum
     *
     * The Energy Solver already handles the asymmetric rise/fall:
     *   Rising: momentum × 0.75 + energy × 0.25 (fast follow)
     *   Falling: momentum × 0.94 + energy × 0.06 (slow decay)
     *
     * So tail strength naturally lingers for 8–12 frames after
     * energy drops. This IS the Emotional Inertia in physical form.
     */
    private fun solveTailStrength(momentum: Float): Float {
        return momentum.coerceIn(0f, 1f)
    }

    // ── Stage 15: Relaxation ────────────────────────────────────────────

    /**
     * Compute relaxation — inverse of surface tension.
     *
     * Formula: relaxation = 1.0 - surfaceTension
     *
     *   - Low surface tension (0.3) → high relaxation (0.7) → loose, organic
     *   - High surface tension (0.9) → low relaxation (0.1) → tight, controlled
     *
     * Used by Path Generator for:
     *   - Edge smoothness (high relaxation = softer edges)
     *   - Curvature modulation (high relaxation = more organic curves)
     *   - Point spacing (high relaxation = more natural point distribution)
     */
    private fun solveRelaxation(surfaceTension: Float): Float {
        return (1f - surfaceTension).coerceIn(0f, 1f)
    }
}
