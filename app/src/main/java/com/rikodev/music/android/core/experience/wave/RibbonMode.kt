package com.rikodev.music.android.core.experience.wave

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — RIBBON MODE
//
// Behavioral mode of the Organic Energy Ribbon™.
//
// The ribbon is NOT always "on" or "off". It has distinct behavioral modes
// based on the current energy landscape. Each mode changes how the ribbon
// responds physically — thickness, movement speed, turbulence, expansion.
//
// Mode transitions are NOT instant — they blend via the Emotional Inertia
// solver (3.3C). The ribbon doesn't snap between modes; it flows between
// them like a fluid changing viscosity.
//
// PRODUCER: Emotional Inertia Solver (Step 3.3C) — reads WaveEnergy, outputs mode
// CONSUMER: Ribbon Physics (Step 3.3D) — adjusts physics parameters per mode
//
// TRANSITION RULE:
//   Mode changes happen through INERTIA BLEND, not hard cuts.
//   When energy rises from BREATHING → ACTIVE, the ribbon gradually
//   thickens and expands over several frames — never a snap.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Behavioral mode of the ribbon at the current moment.
 *
 * Each mode defines a CHARACTER of ribbon behavior, not a binary state.
 * The ribbon is always "alive" — even IDLE has subtle breathing.
 *
 * MODE DECISION CRITERIA (approximate — Emotional Inertia solver fine-tunes):
 *
 *   IDLE           → No audio data, no playback. Minimal breathing only.
 *   BREATHING      → Energy < 0.05. Almost invisible, but alive.
 *   THIN           → Low energy, vocal/ambient. Ribbon thins but stays alive.
 *   ACTIVE         → Normal energy. Ribbon moves organically with music.
 *   PRESSURE_BUILD → Energy rising fast (high attack). Pressure building inside.
 *   EXPANDING      → High energy. Controlled expansion (NOT explosion).
 *   CALM_SETTLING  → Music stopped/paused. Graceful decay — water settling.
 */
enum class RibbonMode {

    /**
     * No audio data available. No playback active.
     *
     * Ribbon behavior:
     *   - Nearly invisible, minimal breathing movement
     *   - Thickness at absolute minimum
     *   - No energy response
     *   - Slowest movement speed
     *   - Transition: gently blends to BREATHING when audio arrives
     */
    IDLE,

    /**
     * Very low energy. Audio is barely audible or in a quiet passage.
     *
     * Ribbon behavior:
     *   - Visible but very subtle — "breathing"
     *   - Thin ribbon with slow undulation
     *   - Almost invisible but DEFINITELY alive
     *   - Low turbulence, high viscosity
     *   - This is the "3 seconds to know it's Riko" baseline —
     *     even silence produces visible ribbon life
     */
    BREATHING,

    /**
     * Low-to-moderate energy. Vocal passages, ambient sections, quiet instruments.
     *
     * Ribbon behavior:
     *   - Ribbon thins compared to ACTIVE
     *   - BUT stays alive (room ambience still exists)
     *   - Smooth, gentle movement
     *   - Moderate texture from mid frequencies
     *   - Transition: can blend to ACTIVE or back to BREATHING
     */
    THIN,

    /**
     * Normal playback energy. The default "music is playing" state.
     *
     * Ribbon behavior:
     *   - Full organic movement responding to energy composition
     *   - Normal thickness, smooth expansion
     *   - Bass pressure creates internal force
     *   - Mid texture creates surface detail
     *   - Treble spark creates edge highlights
     *   - This is the most common mode during playback
     */
    ACTIVE,

    /**
     * Energy is rising rapidly. A kick is approaching or building.
     *
     * Ribbon behavior:
     *   - Pressure builds INSIDE the ribbon (not external jump)
     *   - Ribbon thickens slightly in preparation
     *   - Surface tension increases — ribbon feels "tight"
     *   - NOT expansion yet — this is the buildup phase
     *   - If energy sustains → transitions to EXPANDING
     *   - If energy drops → transitions back to ACTIVE or THIN
     */
    PRESSURE_BUILD,

    /**
     * High energy. Phonk drop, bass-heavy section, intense passage.
     *
     * Ribbon behavior:
     *   - Controlled EXPANSION (not explosion!)
     *   - Ribbon grows thicker and wider
     *   - Maximum surface detail and turbulence
     *   - Glow intensifies, specular highlights brighten
     *   - This is where Riko looks most dramatic —
     *     but it's EXPANSION not EXPLOSION
     *   - Difference: explosion = instant max. Expansion = controlled growth.
     */
    EXPANDING,

    /**
     * Music has stopped or paused. Ribbon is gracefully settling.
     *
     * Ribbon behavior:
     *   - Energy decays — ribbon gradually calms
     *   - Like water settling after being disturbed:
     *     ~~~~~~~~~ → ~~~~~~~ → ~~~~~ → ~~~~ → ~~ → —
     *   - Gradual thinning, slowing, calming
     *   - NEVER instantly disappears — graceful decay
     *   - Eventually reaches IDLE after full settlement
     *   - This is the "premium" feel — cheap visualizers just vanish
     */
    CALM_SETTLING
}
