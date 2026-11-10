package com.rikodev.music.android.core.experience.wave

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — WAVE ENERGY
//
// The energy composition layer of the Wave Engine.
// This is NOT raw audio. This is UNDERSTOOD energy — bass becomes pressure,
// mid becomes texture, treble becomes spark. Each field has a specific
// PURPOSE in the ribbon's behavior, not just a number.
//
// ── RIKO RULE #10 ──
//   "The ribbon never reacts to sound. It reacts to energy."
//
//   Energy = Bass + Mid + Treble + Attack + Decay + History + Momentum
//
//   A kick drum ≠ energy. A snare ≠ energy. Bass ≠ energy.
//   Energy is the COMPOSITION of all these elements into a single
//   organic force that drives the ribbon's physical behavior.
//
// PRODUCER: Energy Solver (Step 3.3B) + Emotional Inertia (Step 3.3C)
// CONSUMER: Ribbon Physics (Step 3.3D) — reads this to compute movement
//
// ALL values normalized 0f..1f unless otherwise noted.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Composed energy state for the Organic Energy Ribbon™.
 *
 * This is the output of the Energy Solver (Rule #10) enriched by the
 * Emotional Inertia solver. It is NOT raw frequency data — each field
 * represents a specific character of energy that the ribbon responds to.
 *
 * NAMING PHILOSOPHY:
 *   - bassPressure  → bass doesn't "jump" the ribbon, it creates INTERNAL PRESSURE
 *   - midTexture    → mid doesn't "thicken" the ribbon, it creates SURFACE TEXTURE
 *   - trebleSpark   → treble doesn't "brighten" the ribbon, it creates SPARK/SHIMMER
 *   - momentum      → not current energy, but ACCUMULATED energy over time
 *   - expansion     → how much the ribbon should EXPAND (not explode)
 *
 * CONSUMER RULES:
 *   ✅ Read any field for physics computation
 *   ✅ Compare with previous frame for delta detection
 *   ❌ NEVER create a new WaveEnergy instance (only solvers write)
 *   ❌ NEVER modify any field
 */
data class WaveEnergy(

    // ── Core Composition (from Energy Solver — Rule #10) ────────────────────

    /**
     * Overall composed energy 0f..1f.
     * The weighted sum of bass + mid + treble + attack + decay + history.
     * This is the PRIMARY driver for ribbon amplitude and mode selection.
     */
    val overallEnergy: Float = 0f,

    /**
     * Bass pressure 0f..1f.
     * How much internal pressure the bass frequencies create inside the ribbon.
     * High bass pressure → ribbon feels "full", pressure builds before expansion.
     * Unlike cheap visualizers, this doesn't directly jump the ribbon — it
     * creates force that Ribbon Physics converts into controlled expansion.
     */
    val bassPressure: Float = 0f,

    /**
     * Mid texture 0f..1f.
     * How much surface detail the mid frequencies create on the ribbon.
     * High mid texture → ribbon surface has visible detail/granularity.
     * Drives turbulence and surface tension modulation in Ribbon Physics.
     */
    val midTexture: Float = 0f,

    /**
     * Treble spark 0f..1f.
     * How much sparkle/shimmer the treble frequencies create on the ribbon.
     * High treble spark → ribbon edges have bright highlights.
     * Drives specular intensity and glow sharpness in the Renderer.
     */
    val trebleSpark: Float = 0f,

    // ── Temporal Characteristics (from Energy Solver + Emotional Inertia) ──

    /**
     * Attack speed 0f..1f.
     * How FAST energy is currently rising. 0 = no onset, 1 = sharpest possible onset.
     * Used by Emotional Inertia for fast-attack response (ribbon responds quickly
     * to rising energy but releases slowly — "breathing" feel).
     */
    val attack: Float = 0f,

    /**
     * Release speed 0f..1f.
     * How FAST energy is currently falling. 0 = no decay, 1 = sharpest possible decay.
     * Used by Emotional Inertia for slow-release (ribbon doesn't die instantly —
     * it gracefully settles like water).
     */
    val release: Float = 0f,

    /**
     * Momentum 0f..1f.
     * ACCUMULATED energy over recent frames. Not current energy — memory of energy.
     * A kick creates high attack but low momentum. Sustained bass creates high momentum.
     * This is the core of "Emotional Inertia" — the ribbon remembers energy.
     *
     * After a kick at frame 1, momentum persists through frames 2→3→4→5→6→7→8.
     * The ribbon doesn't instantly die. It gradually decays with character.
     */
    val momentum: Float = 0f,

    /**
     * Inertia factor 0f..1f.
     * How strongly past frames influence the CURRENT energy reading.
     * 0 = purely reactive (no memory), 1 = fully inertial (past dominates).
     * The Emotional Inertia solver modulates this based on audio character:
     *   - Fast transients → lower inertia (respond to the moment)
     *   - Sustained tones → higher inertia (smooth, flowing movement)
     */
    val inertiaFactor: Float = 0f,

    // ── Derived Values ─────────────────────────────────────────────────────

    /**
     * Silence level 0f..1f. Inverse of energy.
     * 1.0 = complete silence, 0.0 = fully active.
     * Used by Ribbon Physics for "breathing" behavior — even in silence,
     * the ribbon should have a tiny amount of life (breathing ≠ dead).
     */
    val silence: Float = 1f,

    /**
     * Beat confidence 0f..1f. Passed from AudioState.
     * How certain we are a beat just occurred. Drives micro-pulse reactions
     * in the ribbon — a subtle "thump" feeling synchronized to the rhythm.
     */
    val beatConfidence: Float = 0f,

    /**
     * Combined ribbon pressure 0f..1f.
     * Bass-weighted pressure that drives ribbon expansion.
     * This is NOT bassPressure directly — it's the composition of pressure
     * from all bands, weighted toward bass (which creates the physical "push").
     *
     * Formula: bassPressure * 0.5 + midTexture * 0.3 + trebleSpark * 0.2
     */
    val pressure: Float = 0f,

    /**
     * Ribbon expansion factor 0f..1f.
     * How much the ribbon should EXPAND (grow wider/thicker).
     * 0 = fully collapsed (silence), 1 = maximum expansion.
     *
     * CRITICAL: This is EXPANSION, not explosion.
     * Cheap visualizers: bass > threshold → explode
     * Riko Ribbon: energy composition → controlled expansion with physics
     */
    val expansion: Float = 0f

) {
    /**
     * True if there is meaningful energy to drive the ribbon.
     * Threshold is intentionally low (0.01) — even tiny energy should
     * produce visible ribbon movement (breathing behavior).
     */
    val isActive: Boolean
        get() = overallEnergy > 0.01f

    /**
     * True if the ribbon should be in an expanded state.
     * Used for mode selection in the pipeline.
     */
    val isExpanding: Boolean
        get() = expansion > 0.5f

    /**
     * True if energy is currently rising (attack > release).
     * Used by Ribbon Physics for asymmetric response.
     */
    val isRising: Boolean
        get() = attack > release

    /**
     * Dominant energy character — which band is driving the ribbon.
     * Used for routing decisions (color, glow, texture emphasis).
     */
    val dominantCharacter: EnergyCharacter
        get() = when {
            bassPressure >= midTexture && bassPressure >= trebleSpark -> EnergyCharacter.PRESSURE
            trebleSpark >= midTexture && trebleSpark >= bassPressure -> EnergyCharacter.SPARK
            else -> EnergyCharacter.TEXTURE
        }

    companion object {
        /** Pre-built idle state — no energy, full silence. */
        val IDLE = WaveEnergy(silence = 1f)
    }
}

/**
 * Character of the current energy — which band is dominant.
 * Used by Ribbon Physics and Renderer for routing decisions.
 */
enum class EnergyCharacter {
    /** Bass-dominant → ribbon feels "pushed", pressure-driven movement. */
    PRESSURE,
    /** Mid-dominant → ribbon surface has visible texture/detail. */
    TEXTURE,
    /** Treble-dominant → ribbon edges sparkle and shimmer. */
    SPARK
}
