package com.rikodev.music.android.core.experience.calibration

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — EXPERIENCE PROFILES
//
// 5 pre-defined presets for the Wave Engine personality.
//
// Each preset is a complete ExperiencePreset with calibrated values.
// Same renderer. Same solver pipeline. Same architecture.
// Only coefficients differ — that is the power of RIKO's design.
//
// PRESETS:
//   CLASSIC        → Default RIKO feel. Balanced, musical, premium.
//                     Pressure sustains naturally. Momentum has a 12-frame tail.
//                     Highlight is glass-like, not plastic. Glow is subtle.
//
//   FLUID          → Slower, more viscous. Like honey on glass.
//                     Higher pressure decay (longer sustain). Lower turbulence.
//                     Thicker ribbon. More breathing opacity. Smooth surface.
//
//   ELASTIC        → Bouncier, springy. More reactive to transients.
//                     Lower pressure decay (faster release). Higher momentum rise.
//                     Thinner ribbon. Lower viscosity. More bounce.
//
//   LIQUID_GLASS   → Max glass effect. Highlight-heavy, sharp fresnel.
//                     Very high pressure decay. Wide highlight. Low turbulence.
//                     Glass-like surface tension. Wet, premium look.
//
//   EXPERIMENTAL   → Wild coefficients. For testing extreme boundaries.
//                     Pushes parameters to their limits. Useful for discovering
//                     new feels. Not intended for production use.
//
// USAGE:
//   WaveCalibration.applyPreset(ExperienceProfiles.Classic)
//   WaveCalibration.applyPreset(ExperienceProfiles.LiquidGlass)
//   WaveCalibration.reset()  → back to CLASSIC defaults
//
// EXTENSION:
//   Users can create custom presets by:
//   ExperiencePreset(pressureDecay = 0.955f, momentumRise = 0.30f, ...)
//   WaveCalibration.applyPreset(myCustomPreset)
//
// "One line. Entire personality change." — RIKO Preset Philosophy
// ─────────────────────────────────────────────────────────────────────────────

object ExperienceProfiles {

    // ═══════════════════════════════════════════════════════════════════════
    // CLASSIC — Default RIKO feel
    // ═══════════════════════════════════════════════════════════════════════

    /** Default RIKO feel. Balanced, musical, premium. */
    val Classic = ExperiencePreset(
        pressureDecay = 0.96f,
        momentumRise = 0.25f,
        momentumFall = 0.06f,
        expansionDecay = 0.97f,
        breathingOpacity = 0.12f,
        glowIntensity = 0.35f,
        highlightWidth = 0.75f,
        turbulence = 0.40f,
        ribbonThickness = 0.35f,
        profile = ExperienceProfile.CLASSIC,
        displayName = "Classic",
        description = "Default RIKO feel. Balanced sustain, natural momentum tail, glass highlight, subtle glow."
    )

    // ═══════════════════════════════════════════════════════════════════════
    // FLUID — Slow, viscous, honey-like
    // ═══════════════════════════════════════════════════════════════════════

    /** Slower, more viscous. Like honey on glass. */
    val Fluid = ExperiencePreset(
        pressureDecay = 0.975f,        // Slower pressure release (longer sustain)
        momentumRise = 0.18f,         // Less responsive to transients
        momentumFall = 0.04f,         // Very long momentum tail
        expansionDecay = 0.985f,      // Slow expansion release
        breathingOpacity = 0.15f,      // More visible at rest
        glowIntensity = 0.40f,        // Slightly more atmospheric
        highlightWidth = 0.70f,        // Still glass-like, slightly narrower
        turbulence = 0.25f,           // Smooth surface, less chaotic
        ribbonThickness = 0.42f,      // Thicker ribbon — more body
        profile = ExperienceProfile.FLUID,
        displayName = "Fluid",
        description = "Viscous, slow, meditative. Long sustain, smooth surface, thick body."
    )

    // ═══════════════════════════════════════════════════════════════════════
    // ELASTIC — Bouncy, springy, reactive
    // ═══════════════════════════════════════════════════════════════════════

    /** Bouncier, springy. More reactive to transients. */
    val Elastic = ExperiencePreset(
        pressureDecay = 0.94f,         // Faster pressure release
        momentumRise = 0.35f,         // Very responsive to transients
        momentumFall = 0.08f,         // Moderate tail (not too sticky)
        expansionDecay = 0.95f,       // Faster expansion release
        breathingOpacity = 0.10f,      // Less visible at rest
        glowIntensity = 0.28f,        // Less glow, more focused
        highlightWidth = 0.65f,        // Narrower, sharper highlight
        turbulence = 0.55f,           // More surface activity
        ribbonThickness = 0.28f,       // Thinner ribbon — lighter feel
        profile = ExperienceProfile.ELASTIC,
        displayName = "Elastic",
        description = "Bouncy, reactive, energetic. Quick response, thinner ribbon, more surface activity."
    )

    // ═══════════════════════════════════════════════════════════════════════
    // LIQUID GLASS — Maximum glass effect
    // ═══════════════════════════════════════════════════════════════════════

    /** Max glass effect. Highlight-heavy, sharp fresnel, wet look. */
    val LiquidGlass = ExperiencePreset(
        pressureDecay = 0.985f,        // Very slow pressure release (sustained body)
        momentumRise = 0.24f,         // Balanced responsiveness
        momentumFall = 0.045f,        // Long, graceful tail
        expansionDecay = 0.988f,       // Very slow release (glass stretches)
        breathingOpacity = 0.14f,      // Visible breathing (glass is always alive)
        glowIntensity = 0.42f,         // More atmospheric glow
        highlightWidth = 0.85f,        // Wide highlight band (wet glass)
        turbulence = 0.20f,           // Very smooth surface (glass-like)
        ribbonThickness = 0.38f,       // Slightly thicker (glass has body)
        profile = ExperienceProfile.LIQUID_GLASS,
        displayName = "Liquid Glass",
        description = "Premium glass. Wide highlight, smooth surface, long sustain, atmospheric glow."
    )

    // ═══════════════════════════════════════════════════════════════════════
    // EXPERIMENTAL — Extreme boundaries for testing
    // ═══════════════════════════════════════════════════════════════════════

    /** Wild coefficients. For testing and discovering new feels. */
    val Experimental = ExperiencePreset(
        pressureDecay = 0.92f,         // Very fast release (snappy)
        momentumRise = 0.45f,         // Extremely responsive
        momentumFall = 0.12f,         // Faster tail decay
        expansionDecay = 0.90f,        // Quick expansion release
        breathingOpacity = 0.08f,      // Barely visible at rest
        glowIntensity = 0.50f,         // Strong glow (pushing limits)
        highlightWidth = 0.50f,        // Medium highlight
        turbulence = 0.70f,           // Very active surface
        ribbonThickness = 0.50f,       // Thick body
        profile = ExperienceProfile.EXPERIMENTAL,
        displayName = "Experimental",
        description = "Extreme coefficients. For discovering new feels. Not for production."
    )

    // ═══════════════════════════════════════════════════════════════════════
    // LOOKUP
    // ═══════════════════════════════════════════════════════════════════════

    /** Get a preset by profile enum. Returns Classic as fallback. */
    fun fromProfile(profile: ExperienceProfile): ExperiencePreset = when (profile) {
        ExperienceProfile.CLASSIC -> Classic
        ExperienceProfile.FLUID -> Fluid
        ExperienceProfile.ELASTIC -> Elastic
        ExperienceProfile.LIQUID_GLASS -> LiquidGlass
        ExperienceProfile.EXPERIMENTAL -> Experimental
    }

    /** All available presets. */
    val all: List<ExperiencePreset> = listOf(
        Classic, Fluid, Elastic, LiquidGlass, Experimental
    )
}
