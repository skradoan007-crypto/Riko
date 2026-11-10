package com.rikodev.music.android.core.experience.wave

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — RIBBON POINT
//
// A single point on the Organic Energy Ribbon™.
//
// This is NOT a 2D coordinate. This is a RICH data point that carries
// every piece of information the renderer and glass engine need at
// this exact position along the ribbon.
//
// WHY SO MANY FIELDS?
//   Because the renderer needs to know AT EACH POINT:
//   - Where is it? (x, y)
//   - What's the surface doing? (normalX, normalY — for lighting)
//   - How thick should the ribbon be here? (thickness — ink brush profile)
//   - How visible? (opacity — edge fade)
//   - What's the energy doing? (pressure — for highlight intensity)
//   - How fast is it moving? (velocity — for motion blur, if ever needed)
//   - How curved? (curvature — for specular concentration)
//   - What should glass see here? (highlightIntensity, reflectionFactor, edgeAlpha)
//
// The Highlight Solver (3.3F) READS these fields — it doesn't recalculate.
// The Glass Engine (3.4) READS these fields — it doesn't recalculate.
// Everything the renderer and glass need is PRE-COMPUTED here.
//
// PRODUCER: Organic Path Generator (Step 3.3E)
// CONSUMER: Highlight Solver (3.3F), Canvas Renderer (3.4), Glass (3.4)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A single point on the ribbon with full per-point metadata.
 *
 * All values are pre-computed by the Path Generator.
 * Renderers and glass engines read these directly — zero recalculation.
 */
data class RibbonPoint(

    // ── Position ────────────────────────────────────────────────────────

    /** Horizontal position, normalized 0f..1f across the ribbon width. */
    val x: Float = 0f,

    /** Vertical position, normalized -1f..+1f (0 = center line). */
    val y: Float = 0f,

    // ── Surface Geometry ─────────────────────────────────────────────────

    /**
     * X component of the surface normal vector at this point.
     * The normal is perpendicular to the ribbon's tangent direction.
     * Used by the Highlight Solver for specular calculation.
     */
    val normalX: Float = 0f,

    /**
     * Y component of the surface normal vector at this point.
     * Perpendicular to tangent, pointing "outward" from the ribbon surface.
     */
    val normalY: Float = 1f,

    /**
     * Angle of the ribbon surface at this point (radians).
     * Derived from atan2 of the tangent direction.
     *
     * Future-proof field for Phase 3.4 (Canvas Rendering):
     *   - Shimmer animation: highlight oscillation based on surface angle
     *   - Anisotropic highlight: elongated specular along surface direction
     *   - Directional blur: motion blur aligned to ribbon's local angle
     *
     * NOT used by Highlight Solver (3.3F) — read-only by Canvas renderer.
     */
    val surfaceAngle: Float = 0f,

    // ── Physics ─────────────────────────────────────────────────────────

    /** Ribbon pressure at this point (from RibbonState.pressure). */
    val pressure: Float = 0f,

    /** Vertical velocity at this point (from MotionSolver). */
    val velocity: Float = 0f,

    /**
     * Curvature at this point — how much the ribbon bends here.
     *   0.0 = straight (no bend)
     *   1.0 = maximum bend
     * Used by Highlight Solver: specular concentrates at high-curvature points.
     */
    val curvature: Float = 0f,

    // ── Visual Profile (ink brush) ───────────────────────────────────────

    /**
     * Local ribbon thickness at this point 0f..1f.
     * This is the ink brush profile applied to the base thickness:
     *   - Head (x≈0): medium
     *   - Body (x≈0.3–0.7): full
     *   - Tail (x≈0.8–0.9): thinning
     *   - End (x≈1.0): fading to zero
     */
    val thickness: Float = 0f,

    /**
     * Local opacity at this point 0f..1f.
     * The ribbon fades at head and tail for smooth entry/exit.
     * Body opacity is driven by RibbonState.opacity.
     */
    val opacity: Float = 0f,

    // ── Glass Pre-computation (read by Highlight Solver, not recalculated) ──

    /**
     * How bright the highlight should be at this point 0f..1f.
     * Pre-computed from: trebleSpark × position_factor.
     * The Highlight Solver reads this and may modulate further.
     */
    val highlightIntensity: Float = 0f,

    /**
     * How much the ribbon reflects at this point 0f..1f.
     * Higher at center (body), lower at edges (head/tail).
     * Used by Glass Physics for reflection mapping.
     */
    val reflectionFactor: Float = 0f,

    /**
     * Edge alpha — how visible the ribbon edge is at this point 0f..1f.
     * Used by Glass Physics for edge lighting.
     * Higher at body, lower at head/tail fade zones.
     */
    val edgeAlpha: Float = 0f
)
