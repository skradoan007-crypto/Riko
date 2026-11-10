package com.rikodev.music.android.core.experience.wave

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — HIGHLIGHT STATE
//
// The light, reflection, and glass rendering data for the ribbon.
//
// This is NOT a color. This is NOT a brush. This is NOT a paint instruction.
// This is pure RENDERING DATA — numbers that tell the Canvas (Phase 3.4)
// exactly how light behaves on the ribbon's surface.
//
// Every field here is COMPUTED by Highlight Solver (Step 3.3F).
// The renderer READS these fields — it never recalculates.
// "Never calculate twice."
//
// ── RIKO RULES #11–15 (LIGHT) ──────────────────────────────────────────────
//   #11: Light follows the ribbon. Never the camera.
//        Highlight bends WITH the ribbon, not toward the viewer.
//   #12: Treble creates sparkle. Bass creates body. Highlight creates premium.
//        Three completely different concepts.
//   #13: Reflection must never be painted. It must be COMPUTED.
//   #14: Edge glow is caused by curvature. Not opacity.
//   #15: More energy ≠ More white.
//        High energy = slightly wider highlight + stronger fresnel + richer reflection.
//        Subtle. Premium.
//
// PRODUCER: Highlight Solver (Step 3.3F)
// CONSUMER: Canvas Renderer (Phase 3.4), Glass Physics (Phase 3.4)
//
// ⭐ GOLDEN RULE ⭐
//   Nobody creates HighlightState.
//   Everybody subscribes.
//   Only Highlight Solver writes.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Pre-computed per-point highlight data carried alongside the ribbon path.
 *
 * For each RibbonPoint in the path, the Highlight Solver writes one
 * HighlightPoint. The renderer reads these directly — zero recalculation.
 *
 * INVARIANTS:
 *   - Immutable, created fresh each frame
 *   - All values clamped to valid ranges (0f..1f unless noted)
 *   - Derived from RibbonPoint geometry + energy + ColorState metadata
 */
data class HighlightPoint(

    // ── Specular (shiny highlight) ──────────────────────────────────────

    /**
     * How bright the specular highlight is at this point 0f..1f.
     * Concentrated at peaks (high curvature) — Rule #14.
     * NOT driven by energy alone. Driven by curvature × energy × treble.
     *   0.0 = no highlight (flat area, low energy)
     *   1.0 = maximum specular (peak curve + high treble)
     */
    val specularIntensity: Float = 0f,

    // ── Edge Glow (curvature-driven) ────────────────────────────────────

    /**
     * How bright the edge glow is at this point 0f..1f.
     * Rule #14: caused by curvature, NOT opacity.
     * High-curvature areas glow brighter at the ribbon's edge.
     *   0.0 = no edge glow (straight section)
     *   1.0 = maximum edge glow (tight curve + energy)
     */
    val edgeGlow: Float = 0f,

    // ── Rim Light (ribbon edge backlight) ───────────────────────────────

    /**
     * How bright the rim light is at this point 0f..1f.
     * Rim light illuminates the ribbon's edge from behind,
     * creating a subtle "halo" effect that separates the ribbon
     * from the background.
     * Driven by: surface normal angle + energy + thickness.
     *   0.0 = no rim light (facing camera directly)
     *   1.0 = maximum rim light (edge-on view + high energy)
     */
    val rimLight: Float = 0f,

    // ── Reflection ──────────────────────────────────────────────────────

    /**
     * How reflective the surface is at this point 0f..1f.
     * Rule #13: computed, never painted.
     * Higher at body (center), lower at head/tail.
     * Modulated by ColorState.glassTint alpha.
     *   0.0 = no reflection (head/tail fade)
     *   1.0 = maximum reflection (body center + high energy)
     */
    val reflectionAlpha: Float = 0f,

    // ── Glass ───────────────────────────────────────────────────────────

    /**
     * How "glass-like" the ribbon appears at this point 0f..1f.
     * Controls the glass overlay strength.
     * Driven by: energy × surfaceTension × opacity.
     *   0.0 = no glass effect (idle/thin ribbon)
     *   0.3 = subtle glass (breathing/active)
     *   0.6+ = strong glass (expanding, high energy)
     */
    val glassOpacity: Float = 0f,

    /**
     * Fresnel approximation at this point 0f..1f.
     * Fresnel effect: surfaces at grazing angles reflect more light.
     * At ribbon edges (normal ≈ perpendicular to view), fresnel is high.
     * At ribbon center (normal ≈ facing view), fresnel is low.
     *
     * Formula: pow(1 - dot(normal, viewDir), fresnelPower)
     * Rule #15: More energy = slightly stronger fresnel, NOT more white.
     *
     *   0.0 = no fresnel (facing directly toward camera)
     *   1.0 = maximum fresnel (grazing angle, maximum reflectivity)
     */
    val fresnel: Float = 0f,

    // ── Light Direction (ribbon-local) ───────────────────────────────────

    /**
     * X component of the light direction vector at this point.
     * Rule #11: Light follows the ribbon, NOT the camera.
     * The light source direction rotates WITH the ribbon's surface.
     * Computed from ribbon normal + ribbon curvature.
     */
    val lightDirectionX: Float = 0f,

    /**
     * Y component of the light direction vector at this point.
     * Together with lightDirectionX, forms the local light direction.
     */
    val lightDirectionY: Float = -1f,

    // ── Shadow ──────────────────────────────────────────────────────────

    /**
     * How soft the shadow is at this point 0f..1f.
     * Soft shadows under the ribbon give depth.
     * Driven by: energy (more energy = sharper shadow) + thickness.
     *   0.0 = no shadow (idle/breathing, too thin to cast shadow)
     *   0.3 = very soft shadow (thin ribbon, diffused)
     *   0.6+ = visible shadow (expanding, thick ribbon)
     */
    val shadowStrength: Float = 0f,

    // ── Geometry ─────────────────────────────────────────────────────────

    /**
     * Width of the specular highlight band at this point 0f..1f.
     * Rule #15: More energy = slightly WIDER highlight, NOT brighter.
     * This controls how many pixels the highlight spans across
     * the ribbon's thickness dimension.
     *   0.0 = no highlight (collapsed)
     *   0.3 = thin highlight line (idle/low energy)
     *   0.6 = medium highlight (active)
     *   1.0 = wide highlight band (expanding)
     */
    val highlightWidth: Float = 0f,

    /**
     * Offset of the reflection from the ribbon center 0f..1f.
     * Reflections don't sit at dead center — they shift toward
     * the light source direction.
     * Computed from: light direction × surface curvature.
     *   0.5 = dead center
     *   0.0 = shifted fully left/toward light
     *   1.0 = shifted fully right/away from light
     */
    val reflectionOffset: Float = 0.5f

) {
    /**
     * True if this point has any meaningful highlight data.
     * Used by renderer to skip processing for invisible points.
     */
    val hasHighlight: Boolean
        get() = specularIntensity > 0.01f
                || edgeGlow > 0.01f
                || rimLight > 0.01f

    /**
     * True if this point should show glass effect.
     */
    val isGlassy: Boolean
        get() = glassOpacity > 0.05f

    /**
     * True if this point casts a visible shadow.
     */
    val hasShadow: Boolean
        get() = shadowStrength > 0.05f

    companion object {
        /** Pre-built idle highlight point — no light, no reflection. */
        val IDLE = HighlightPoint()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HIGHLIGHT STATE — Frame-level highlight summary
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The complete light/reflection state for the ribbon in one frame.
 *
 * Contains both per-point highlight data AND frame-level summary values.
 * The per-point data is indexed 1:1 with OrganicPath.points.
 *
 * INVARIANTS:
 *   - highlightPoints.size == organicPath.points.size (if path exists)
 *   - Immutable, replaced entirely each frame
 *   - Frame-level values are averages/maximums of per-point data
 *
 * PRODUCER: Highlight Solver (Step 3.3F)
 * CONSUMER: Canvas Renderer (Phase 3.4)
 */
data class HighlightState(

    // ── Per-Point Data ──────────────────────────────────────────────────

    /**
     * Per-point highlight data, 1:1 mapped to OrganicPath.points.
     * highlightPoints[i] corresponds to points[i].
     * Empty when idle (no geometry).
     */
    val points: List<HighlightPoint> = emptyList(),

    // ── Frame-Level Light Summary ───────────────────────────────────────

    /**
     * Average specular intensity across the ribbon 0f..1f.
     * Used by renderer to decide overall highlight blend mode.
     */
    val specularIntensity: Float = 0f,

    /**
     * Maximum edge glow value in this frame 0f..1f.
     * Used by renderer to decide if glow pass is needed.
     */
    val edgeGlow: Float = 0f,

    /**
     * Average rim light intensity 0f..1f.
     * Used by renderer for rim light pass budget.
     */
    val rimLight: Float = 0f,

    // ── Frame-Level Reflection Summary ────────────────────────────────────

    /**
     * Average reflection alpha across the ribbon 0f..1f.
     * When > 0.05, the renderer should draw the reflection pass.
     */
    val reflectionAlpha: Float = 0f,

    /**
     * Average glass opacity across the ribbon 0f..1f.
     * When > 0.05, the renderer should draw the glass overlay pass.
     */
    val glassOpacity: Float = 0f,

    // ── Fresnel Summary ───────────────────────────────────────────────────

    /**
     * Average fresnel effect across the ribbon 0f..1f.
     * Drives overall reflectivity modulation.
     */
    val fresnel: Float = 0f,

    // ── Light Direction (frame-level) ──────────────────────────────────────

    /**
     * Primary light direction X for the entire frame.
     * Based on ribbon's overall curvature direction.
     * Rule #11: Light follows the ribbon.
     */
    val lightDirection: Float = 0f,

    // ── Shadow ──────────────────────────────────────────────────────────

    /**
     * Maximum shadow strength in this frame 0f..1f.
     * When > 0.05, the renderer should draw the shadow pass.
     */
    val shadowStrength: Float = 0f,

    // ── Geometry ─────────────────────────────────────────────────────────

    /**
     * Average highlight width across the ribbon 0f..1f.
     * Rule #15: Wider with energy, NOT brighter.
     */
    val highlightWidth: Float = 0f,

    /**
     * Average reflection offset across the ribbon 0f..5f.
     * Where reflections tend to sit relative to ribbon center.
     */
    val reflectionOffset: Float = 0.5f,

    // ── Fresnel Details (for advanced rendering in 3.4) ─────────────────

    /**
     * View angle approximation for the entire ribbon 0f..1f.
     * Average dot product of surface normals with view direction.
     *   1.0 = ribbon faces camera directly
     *   0.0 = ribbon is edge-on to camera
     */
    val viewAngle: Float = 0f,

    /**
     * Surface normal magnitude (average) 0f..1f.
     * Should be close to 1.0 for well-formed normals.
     */
    val surfaceNormal: Float = 1f,

    /**
     * Fresnel power exponent — controls how sharp the fresnel falloff is.
     *   2.0 = broad fresnel (subtle, diffuse)
     *   5.0 = sharp fresnel (glass-like, concentrated at edges)
     * Energy modulates this: higher energy = slightly sharper fresnel.
     */
    val fresnelPower: Float = 3f,

    // ── Metadata ──────────────────────────────────────────────────────────

    /** Frame timestamp (System.nanoTime()). */
    val timestamp: Long = 0L

) {
    /**
     * True if this state has per-point data (non-empty path was processed).
     */
    val hasData: Boolean
        get() = points.isNotEmpty()

    /**
     * True if any highlight pass should be drawn.
     */
    val needsHighlight: Boolean
        get() = specularIntensity > 0.02f
                || edgeGlow > 0.02f
                || rimLight > 0.02f

    /**
     * True if the reflection pass should be drawn.
     */
    val needsReflection: Boolean
        get() = reflectionAlpha > 0.05f

    /**
     * True if the glass overlay pass should be drawn.
     */
    val needsGlass: Boolean
        get() = glassOpacity > 0.05f

    /**
     * True if the shadow pass should be drawn.
     */
    val needsShadow: Boolean
        get() = shadowStrength > 0.05f

    /**
     * True if this is the idle state (no highlights, no reflections).
     */
    val isIdle: Boolean
        get() = !hasData
                && specularIntensity < 0.01f
                && glassOpacity < 0.01f

    companion object {
        /** Pre-built idle state — no light, no glass, no reflection. */
        val IDLE = HighlightState()
    }
}
