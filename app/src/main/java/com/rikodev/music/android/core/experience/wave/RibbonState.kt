package com.rikodev.music.android.core.experience.wave

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — RIBBON STATE
//
// The physics state of the Organic Energy Ribbon™.
//
// This is the output of the Ribbon Physics layer (Step 3.3D).
// It describes HOW the ribbon moves — not what it looks like.
//
// The ribbon behaves as a FLUID, not a wave.
// Fluid has viscosity, elasticity, surface tension, pressure.
// Wave just goes up and down. Fluid has CHARACTER.
//
// PHYSICS MODEL:
//   The ribbon is modeled as a continuous fluid ribbon with:
//   - Internal pressure (from energy)
//   - Surface tension (how tight the surface holds)
//   - Viscosity (how thick/slow the movement is)
//   - Elasticity (how much it springs back)
//   - Turbulence (chaos/randomness in movement)
//   - Damping (how quickly oscillation dies)
//
// PRODUCER: Ribbon Physics Solver (Step 3.3C) — reads WaveEnergy + RibbonMode
// CONSUMER: Path Generator (Step 3.3E) — reads physics to compute shape
//
// ALL values normalized 0f..1f unless otherwise noted.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Physics state of the ribbon at the current frame.
 *
 * Every value here is computed by the Ribbon Physics solver from
 * WaveEnergy input. No external code should create or modify this.
 *
 * DESIGN PHILOSOPHY:
 *   These aren't arbitrary numbers — each value represents a real
 *   physical property of the ribbon-as-fluid model:
 *
 *   elasticity    → How strongly the ribbon resists deformation
 *                    and springs back. High = bouncy, low = mushy.
 *
 *   viscosity     → How "thick" the ribbon fluid is. High viscosity =
 *                    slow, smooth movement (like honey). Low = fast,
 *                    responsive movement (like water).
 *
 *   surfaceTension→ How tight the ribbon surface holds together.
 *                    High = smooth edges, tight form. Low = loose,
 *                    organic edges with natural variation.
 *
 *   pressure      → Internal pressure from energy. Not external force —
 *                    pressure that BUILDS INSIDE the ribbon before
 *                    it expands outward. This is the "breath" of the ribbon.
 *
 * PHYSICS-TO-VISUAL MAPPING:
 *   thickness     → Direct visual output: how wide the ribbon draws
 *   opacity       → Direct visual output: how visible the ribbon is
 *
 *   These two are the ONLY visual fields. Everything else is physics
 *   that the Path Generator (3.3E) interprets into geometry.
 */
data class RibbonState(

    // ── Fluid Properties (computed from WaveEnergy + RibbonMode) ────────────

    /**
     * Elasticity 0f..1f.
     * How strongly the ribbon resists deformation and springs back.
     *   - High (0.7–1.0): Bouncy, springy response. Energy spikes cause
     *     visible spring-back. Good for punchy, rhythmic music.
     *   - Low (0.0–0.3): Mushy, slow response. Energy dissipates gradually.
     *     Good for ambient, atmospheric music.
     *   - Medium (0.3–0.7): Balanced — the default for most music.
     *
     * Mode modulation:
     *   IDLE: 0.3 (slow, dreamy)
     *   BREATHING: 0.4
     *   ACTIVE: 0.6
     *   PRESSURE_BUILD: 0.8 (tight, ready to spring)
     *   EXPANDING: 0.5 (expanding, not bouncing)
     *   CALM_SETTLING: 0.3 (relaxing back)
     */
    val elasticity: Float = 0.5f,

    /**
     * Viscosity 0f..1f.
     * How "thick" the ribbon fluid is. Controls movement speed and smoothness.
     *   - High viscosity: Slow, smooth, honey-like movement.
     *     Ribbon glides rather than jumps. Perfect for emotional passages.
     *   - Low viscosity: Fast, responsive, water-like movement.
     *     Ribbon reacts quickly to transients. Good for percussion-heavy music.
     *
     * This is the key differentiator from cheap visualizers:
     *   Cheap: bass > threshold → instant jump (zero viscosity)
     *   Riko: bass creates pressure → ribbon glides to new position (viscous)
     */
    val viscosity: Float = 0.5f,

    /**
     * Flow resistance 0f..1f.
     * How much the ribbon resists rapid changes in direction.
     *   - High: Ribbon maintains its current trajectory longer.
     *     Smooth curves, minimal jitter. Good for sustained tones.
     *   - Low: Ribbon changes direction easily.
     *     More reactive, potentially more chaotic. Good for complex rhythms.
     */
    val flowResistance: Float = 0.3f,

    /**
     * Surface tension 0f..1f.
     * How tightly the ribbon surface holds together.
     *   - High: Smooth, tight edges. Ribbon looks polished, controlled.
     *   - Low: Loose, organic edges with natural variation.
     *     More "alive" looking — visible texture in the ribbon boundary.
     */
    val surfaceTension: Float = 0.5f,

    // ── Force & Pressure ──────────────────────────────────────────────────

    /**
     * Internal pressure 0f..1f.
     * The force building INSIDE the ribbon from energy input.
     * This is NOT displacement — it's the POTENTIAL for displacement.
     *
     * Think of it like blowing air into a balloon:
     *   - Low pressure: balloon is normal
     *   - Rising pressure: balloon tightens (PRESSURE_BUILD mode)
     *   - High pressure: balloon starts expanding (EXPANDING mode)
     *
     * The ribbon doesn't JUMP to pressure. Pressure BUILDS, then
     * the ribbon EXPANDS. Two separate phases.
     */
    val pressure: Float = 0f,

    /**
     * Turbulence 0f..1f.
     * How much chaos/randomness is in the ribbon's movement.
     *   - Low: Smooth, predictable movement paths.
     *   - High: Chaotic, organic movement with visible randomness.
     *
     * Driven primarily by midTexture (from WaveEnergy) — mid frequencies
     * create the most visible texture/turbulence in the ribbon surface.
     *
     * Mode modulation:
     *   EXPANDING: highest turbulence (lots of surface activity)
     *   BREATHING: near-zero turbulence (calm, peaceful)
     */
    val turbulence: Float = 0f,

    /**
     * Damping 0f..1f.
     * How quickly oscillation and vibration die down.
     *   - High: Fast damping — oscillation stops quickly.
     *     Ribbon settles fast after energy drops.
     *   - Low: Slow damping — ribbon continues to resonate
     *     after the energy source stops. Creates "ringing" effect.
     *
     * Related to Emotional Inertia: low damping = longer memory tail.
     */
    val damping: Float = 0.3f,

    // ── Movement Dynamics ──────────────────────────────────────────────────

    /**
     * Drift 0f..1f.
     * Slow lateral movement of the ribbon center line.
     * Not driven by energy — this is the ribbon's natural "current".
     * Even in silence, drift gives the ribbon a sense of being alive.
     *
     * In BREATHING/IDLE modes, drift is the primary visible movement.
     */
    val drift: Float = 0f,

    /**
     * Horizontal stretch factor 0f..1f.
     * How much the ribbon stretches horizontally (width direction).
     *   - 1.0 = normal width
     *   - > 1.0 = stretched (ribbon is wider than normal)
     *   - < 1.0 = compressed (ribbon is narrower)
     *
     * Driven by energy and mode. EXPANDING → higher stretch.
     */
    val stretch: Float = 1f,

    /**
     * Vertical compression factor 0f..1f.
     * How much the ribbon compresses vertically (height direction).
     *   - 1.0 = normal height
     *   - < 1.0 = compressed (ribbon is thinner)
     *
     * THIN mode uses compression to make the ribbon thinner
     * without losing its alive quality.
     */
    val compression: Float = 1f,

    // ── Visual Output (computed from physics, consumed by Renderer) ───────

    /**
     * Ribbon thickness 0f..1f.
     * The visual thickness the ribbon should be drawn at.
     * This is the PHYSICS OUTPUT mapped to visual space.
     *
     * Mode ranges (approximate):
     *   IDLE:           0.02 (barely visible)
     *   BREATHING:      0.05–0.10 (thin but alive)
     *   THIN:           0.10–0.20 (noticeably thin)
     *   ACTIVE:         0.20–0.50 (normal, responsive)
     *   PRESSURE_BUILD: 0.40–0.60 (thickening)
     *   EXPANDING:      0.50–1.00 (wide and dramatic)
     *   CALM_SETTLING:   decaying from current → 0.02
     */
    val thickness: Float = 0f,

    /**
     * Ribbon opacity 0f..1f.
     * How visible/transparent the ribbon should be drawn.
     *   - 0.0 = invisible
     *   - 1.0 = fully opaque
     *
     * Even in IDLE/BREATHING modes, opacity is NEVER zero.
     * The ribbon is always at least slightly visible (breathing).
     * Minimum opacity: 0.12 (idle baseline), active up to 1.0.
     */
    val opacity: Float = 0.12f,

    // ── Energy Persistence & Light Response ────────────────────────────────

    /**
     * Highlight intensity 0f..1f.
     * How bright the ribbon's specular highlights are.
     * Driven primarily by treble spark — high treble = more shimmer.
     * Formula: trebleSpark * 0.7 + overallEnergy * 0.3
     * Used by Highlight Solver (3.3F) for specular and edge light computation.
     */
    val highlightIntensity: Float = 0f,

    /**
     * Tail strength 0f..1f.
     * How strongly the ribbon's energy tail persists after the source drops.
     * Directly derived from momentum — the ribbon's Emotional Inertia in action.
     * After a kick, tail strength stays high for 8–12 frames, causing
     * the ribbon to linger instead of instantly dying.
     */
    val tailStrength: Float = 0f,

    /**
     * Relaxation 0f..1f.
     * How relaxed/loose the ribbon's surface is. Inverse of tension.
     *   - High relaxation: ribbon surface is loose, organic, flowing.
     *   - Low relaxation: ribbon surface is tight, controlled, precise.
     * Formula: 1.0 - surfaceTension
     * Used by Path Generator for edge smoothness and curvature modulation.
     */
    val relaxation: Float = 0.5f

) {
    /**
     * True if the ribbon has any visible presence.
     * Threshold is intentionally low — even minimal opacity counts as "alive".
     */
    val isAlive: Boolean
        get() = opacity > 0.01f

    /**
     * True if the ribbon is in a fluid state (has meaningful viscosity and elasticity).
     */
    val isFluid: Boolean
        get() = viscosity > 0.2f && elasticity > 0.2f

    /**
     * True if the ribbon is under significant pressure.
     */
    val isUnderPressure: Boolean
        get() = pressure > 0.4f

    /**
     * True if the ribbon is actively expanding.
     */
    val isExpanding: Boolean
        get() = stretch > 1.1f

    companion object {
        /** Pre-built idle state — minimal life, no energy. */
        val IDLE = RibbonState(
            elasticity = 0.3f,
            viscosity = 0.5f,
            thickness = 0.02f,
            opacity = 0.05f,
            drift = 0.1f
        )
    }
}
