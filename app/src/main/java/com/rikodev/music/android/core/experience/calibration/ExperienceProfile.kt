package com.rikodev.music.android.core.experience.calibration

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — EXPERIENCE PROFILE
//
// The identity layer of the Riko Wave Engine.
//
// Each profile represents a distinct personality for the engine.
// Same solver. Same renderer. Same architecture.
// Only the WaveCalibration coefficients change.
//
// ARCHITECTURE LAYER:
//   ExperienceProfile  → Identity (enum — what)
//   ExperiencePreset   → DNA (data class — specific values)
//   ExperienceProfiles → Registry (object — 5 presets)
//   WaveCalibration    → BIOS (active runtime values)
//   All Solvers        → Read from WaveCalibration
//   WaveCanvas         → Read from WaveState
//
// "One line. Entire personality change." — RIKO Preset Philosophy
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 5 pre-defined engine personalities for the Riko Wave Engine.
 *
 * Each profile maps to an ExperiencePreset in ExperienceProfiles.
 * The preset contains 9 physics coefficients that define HOW the ribbon feels.
 *
 * Usage:
 *   WaveCalibration.applyPreset(ExperienceProfiles.fromProfile(ExperienceProfile.FLUID))
 *   → entire engine personality transforms. One line. Zero code changes.
 *
 * EXTENSION:
 *   Future profiles can be added here without changing any solver or renderer.
 *   The Configuration Architecture ensures behavior is decoupled from code.
 */
enum class ExperienceProfile {
    /** Default RIKO feel. Balanced, musical, premium. */
    CLASSIC,

    /** Slow, viscous, honey-like. Long sustain, smooth surface. */
    FLUID,

    /** Bouncy, springy, reactive. Quick response, more surface activity. */
    ELASTIC,

    /** Maximum glass effect. Wide highlight, smooth surface, wet look. */
    LIQUID_GLASS,

    /** Extreme coefficients for discovering new feels. Not for production. */
    EXPERIMENTAL
}
