package com.rikodev.music.android.core.experience.wave

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — WAVE STATE
//
// The published output of the Wave Engine.
// This is what consumers (Renderer, Glass, Motion, Navigation) subscribe to.
//
// Wave State is to Wave Engine what Audio State is to Experience Hub
// and Color State is to Color Brain — the single published snapshot.
//
// ⭐ GOLDEN RULE ⭐
//   Nobody creates WaveState.
//   Everybody subscribes.
//   Only Wave Engine writes.
//
// Architecture:
//
//   AudioState ──→ Wave Engine Pipeline ──→ WaveState
//   ColorState ────────────────────────────────┘ (for color reading)
//                      │
//                      ▼
//              StateFlow<WaveState>
//                      │
//        ┌─────────────┼─────────────┐
//        │             │             │
//     Renderer      Glass       Motion
//     (Canvas)    (Physics)    (Sync)
//
// WHAT IT CONTAINS:
//   - WaveEnergy: composed energy (Rule #10)
//   - RibbonState: physics output (fluid dynamics)
//   - RibbonMode: behavioral mode
//   - OrganicPath: complete ribbon geometry (Step 3.3E)
//   - HighlightState: light/reflection/glass data (Step 3.3F)
//   - Status flags: isPlaying, isActive, timestamp
//
// FUTURE EXTENSIONS:
//   - Glass interaction data (3.3G+)
//   - Motion sync data (3.3I)
//
// CONSUMER RULES:
//   ✅ Read any field from collect {}
//   ✅ Compare current vs previous for animation triggers
//   ✅ Use energy + ribbon together for rendering decisions
//   ❌ NEVER create a new WaveState instance
//   ❌ NEVER modify any field
//   ❌ NEVER cache a reference (always read from flow)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The published state of the Wave Engine at the current frame.
 *
 * This is a frozen snapshot — immutable, thread-safe, copy-on-write.
 * The Wave Engine is the ONLY component allowed to create new instances.
 * All consumers read this via `StateFlow<WaveState>.collect { ... }`.
 *
 * EXTENSIBILITY:
 *   As the pipeline grows (3.3E → 3.3F → 3.3G → 3.3I), new fields
 *   are ADDED to this state. Existing fields never change meaning.
 *   Path geometry, render hints, glass data, motion data — all added
 *   here as the pipeline matures.
 */
data class WaveState(

    // ── Energy (from Energy Solver + Emotional Inertia) ─────────────────────
    /**
     * Composed energy state — the "soul" of the ribbon.
     * Contains bassPressure, midTexture, trebleSpark, momentum, expansion, etc.
     * This drives everything: physics, mode, rendering intensity.
     */
    val energy: WaveEnergy = WaveEnergy.IDLE,

    // ── Physics (from Ribbon Physics) ──────────────────────────────────────
    /**
     * Ribbon physics state — the "body" of the ribbon.
     * Contains elasticity, viscosity, pressure, turbulence, thickness, opacity.
     * This drives the Path Generator to compute geometry.
     */
    val ribbon: RibbonState = RibbonState.IDLE,

    // ── Behavior (from Emotional Inertia) ────────────────────────────────────
    /**
     * Current behavioral mode — the "mood" of the ribbon.
     * IDLE, BREATHING, THIN, ACTIVE, PRESSURE_BUILD, EXPANDING, CALM_SETTLING.
     * This determines how physics parameters are modulated.
     */
    val mode: RibbonMode = RibbonMode.IDLE,

    // ── Status ─────────────────────────────────────────────────────────────
    /**
     * True if audio is currently playing and the ribbon is actively driven.
     * False during pause, stop, or idle. Used by Renderer to decide
     * whether to draw the full ribbon or just the breathing baseline.
     */
    val isActive: Boolean = false,

    /**
     * True if the ribbon is in a state where it should be visible.
     * Unlike [isActive], this is true even during BREATHING (subtle movement)
     * and CALM_SETTLING (graceful decay). The ribbon is always visible —
     * it just changes character.
     */
    val isVisible: Boolean = true,

    // ── Timing ─────────────────────────────────────────────────────────────
    /**
     * Timestamp of this state snapshot (System.nanoTime()).
     * Used by consumers for frame timing and delta calculations.
     */
    val timestamp: Long = 0L,

    // ── Geometry (from Organic Path Generator — Step 3.3E) ──────────────
    /**
     * Complete ribbon geometry for this frame.
     * Contains: center line points, Bezier controls, normals,
     * thickness profile, opacity profile, glass pre-computation.
     * The renderer reads this directly — zero recalculation.
     */
    val path: OrganicPath = OrganicPath.EMPTY,

    // ── Highlight (from Highlight Solver — Step 3.3F) ───────────────────
    /**
     * Per-point light/reflection/glass data for this frame.
     * Contains: specular, edge glow, rim light, fresnel, reflection,
     * glass opacity, shadow, light direction.
     * The renderer reads this directly — zero recalculation.
     */
    val highlight: HighlightState = HighlightState.IDLE,

    // ── FUTURE EXTENSION POINTS ───────────────────────────────────────────
    // Step 3.3G+: val glassInteraction: GlassInteraction = GlassInteraction.IDLE
    // Step 3.3I: val motionSync: MotionSync = MotionSync.IDLE

) {
    /**
     * True if this is the completely idle state (no energy, no physics).
     * The ribbon is in its most minimal breathing state.
     */
    val isIdle: Boolean
        get() = !isActive && mode == RibbonMode.IDLE && energy.overallEnergy < 0.01f

    /**
     * True if the ribbon is in an expanding state (high energy).
     * Used by Glass Physics and Motion for synchronized reactions.
     */
    val isExpanding: Boolean
        get() = mode == RibbonMode.EXPANDING || ribbon.isExpanding

    /**
     * True if the ribbon is settling down (music just stopped/paused).
     * Used by Renderer to blend from active → calm → breathing → idle.
     */
    val isSettling: Boolean
        get() = mode == RibbonMode.CALM_SETTLING

    /**
     * True if a beat was detected in the current frame.
     * Used by Motion Language for micro-pulse effects.
     */
    val hasBeat: Boolean
        get() = energy.beatConfidence > 0.5f

    /**
     * Convenience: get the ribbon thickness directly.
     * Consumers don't need to drill into ribbon.thickness.
     */
    val thickness: Float
        get() = ribbon.thickness

    /**
     * Convenience: get the ribbon opacity directly.
     */
    val opacity: Float
        get() = ribbon.opacity

    /**
     * Convenience: get overall energy directly.
     */
    val overallEnergy: Float
        get() = energy.overallEnergy

    /**
     * Convenience: does this state have ribbon geometry?
     */
    val hasGeometry: Boolean
        get() = path.hasGeometry

    /**
     * Convenience: does this state need any highlight pass?
     */
    val needsHighlight: Boolean
        get() = highlight.needsHighlight

    /**
     * Convenience: does this state need glass overlay?
     */
    val needsGlass: Boolean
        get() = highlight.needsGlass

    companion object {
        /** Pre-built idle state — minimal breathing, no energy, no geometry. */
        val IDLE = WaveState(
            ribbon = RibbonState.IDLE,
            isVisible = true,
            path = OrganicPath.EMPTY,
            highlight = HighlightState.IDLE,
            timestamp = 0L
        )
    }
}
