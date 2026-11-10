package com.rikodev.music.android.core.experience.calibration

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — EXPERIENCE PRESET
//
// The personality DNA of the Wave Engine.
//
// Each preset defines 9 key physics parameters that control the entire
// engine feel. Same renderer. Same solver pipeline. Same architecture.
// Only these coefficients change.
//
// One line:
//   WaveCalibration.applyPreset(ExperienceProfiles.LiquidGlass)
//   → entire engine personality transforms.
//
// PRESET MAPPING:
//   pressureDecay     → Physics.PRESSURE_DECAY       (ribbon sustain)
//   momentumRise      → Energy.MOMENTUM_RISE         (responsiveness)
//   momentumFall      → Energy.MOMENTUM_FALL         (tail length)
//   expansionDecay    → Energy.EXPANSION_RELEASE_DECAY (release feel)
//   breathingOpacity  → Physics.OPACITY_MINIMUM      (idle presence)
//   glowIntensity     → Glow.AMBIENT_RADIUS_FRAC     (atmosphere)
//   highlightWidth    → Light.HIGHLIGHT_WIDTH_MAX     (glass character)
//   turbulence        → Physics.VISCOSITY_ENERGY_W    (surface texture)
//   ribbonThickness   → Physics.THICKNESS_ACTIVE      (body mass)
//
// EXTENSIBILITY:
//   This data class covers the 9 "personality" parameters.
//   Future versions can add more fields — the engine will
//   use defaults for any field the preset doesn't override.
//
// "100 line is better than 1000 line of CODE" ⭐
// ─────────────────────────────────────────────────────────────────────────────

data class ExperiencePreset(
    /** How much pressure carries over per frame (0.90–0.99). Higher = longer sustain. */
    val pressureDecay: Float,
    /** How fast momentum follows rising energy (0.10–0.40). Higher = more responsive. */
    val momentumRise: Float,
    /** How slowly momentum decays on falling energy (0.02–0.15). Lower = longer tail. */
    val momentumFall: Float,
    /** Per-frame expansion release decay (0.90–0.99). Higher = slower release. */
    val expansionDecay: Float,
    /** Minimum opacity when idle/breathing (0.05–0.25). Higher = more visible at rest. */
    val breathingOpacity: Float,
    /** Ambient glow radius fraction (0.15–0.50). Higher = more atmospheric glow. */
    val glowIntensity: Float,
    /** Maximum highlight width fraction (0.30–0.85). Higher = wider glass band. */
    val highlightWidth: Float,
    /** Viscosity energy weight (0.10–0.80). Higher = smoother, less chaotic surface. */
    val turbulence: Float,
    /** Active mode ribbon thickness (0.15–0.60). Higher = thicker, more body. */
    val ribbonThickness: Float,
    /** The profile identity this preset belongs to. */
    val profile: ExperienceProfile = ExperienceProfile.CLASSIC,
    /** Human-readable name for UI display. */
    val displayName: String = "Custom",
    /** Optional description of this preset's character. */
    val description: String = ""
) {
    // ═══════════════════════════════════════════════════════════════════════
    // SNAPSHOT SYSTEM — Save / Load / Compare
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Serialize this preset to a simple string map.
     * Used for snapshot save/load and debug logging.
     *
     * Output example:
     *   {"profile":"LiquidGlass","pressureDecay":0.965,"momentumRise":0.24,...}
     */
    fun toSnapshot(): Map<String, Any> = mapOf(
        "profile" to profile.name,
        "pressureDecay" to pressureDecay,
        "momentumRise" to momentumRise,
        "momentumFall" to momentumFall,
        "expansionDecay" to expansionDecay,
        "breathingOpacity" to breathingOpacity,
        "glowIntensity" to glowIntensity,
        "highlightWidth" to highlightWidth,
        "turbulence" to turbulence,
        "ribbonThickness" to ribbonThickness
    )

    /**
     * Pretty-printed snapshot for debug display.
     * Example:
     *   ┌─ LiquidGlass ─────────────────┐
     *   │ Pressure Decay    0.965       │
     *   │ Momentum Rise     0.240       │
     *   │ ...
     *   └───────────────────────────────┘
     */
    fun toSnapshotString(): String {
        val name = displayName
        val w = maxOf(name.length + 4, 32)
        val sb = StringBuilder()
        sb.appendLine("┌─ $name ${"─".repeat(maxOf(0, w - name.length - 4))}")
        sb.appendLine("│ Pressure Decay    ${pressureDecay.format(3)}")
        sb.appendLine("│ Momentum Rise     ${momentumRise.format(3)}")
        sb.appendLine("│ Momentum Fall     ${momentumFall.format(3)}")
        sb.appendLine("│ Expansion Decay   ${expansionDecay.format(3)}")
        sb.appendLine("│ Breathing Opacity ${breathingOpacity.format(3)}")
        sb.appendLine("│ Glow Intensity    ${glowIntensity.format(3)}")
        sb.appendLine("│ Highlight Width   ${highlightWidth.format(3)}")
        sb.appendLine("│ Turbulence        ${turbulence.format(3)}")
        sb.appendLine("│ Ribbon Thickness  ${ribbonThickness.format(3)}")
        sb.appendLine("└${"─".repeat(w)}")
        return sb.toString()
    }

    companion object {
        /**
         * Reconstruct a preset from a snapshot map.
         * Returns null if required fields are missing.
         */
        fun fromSnapshot(snapshot: Map<String, Any>): ExperiencePreset? {
            return try {
                ExperiencePreset(
                    pressureDecay = (snapshot["pressureDecay"] as? Number)?.toFloat() ?: return null,
                    momentumRise = (snapshot["momentumRise"] as? Number)?.toFloat() ?: return null,
                    momentumFall = (snapshot["momentumFall"] as? Number)?.toFloat() ?: return null,
                    expansionDecay = (snapshot["expansionDecay"] as? Number)?.toFloat() ?: return null,
                    breathingOpacity = (snapshot["breathingOpacity"] as? Number)?.toFloat() ?: return null,
                    glowIntensity = (snapshot["glowIntensity"] as? Number)?.toFloat() ?: return null,
                    highlightWidth = (snapshot["highlightWidth"] as? Number)?.toFloat() ?: return null,
                    turbulence = (snapshot["turbulence"] as? Number)?.toFloat() ?: return null,
                    ribbonThickness = (snapshot["ribbonThickness"] as? Number)?.toFloat() ?: return null,
                    profile = try {
                        ExperienceProfile.valueOf(snapshot["profile"] as? String ?: "CLASSIC")
                    } catch (_: Exception) {
                        ExperienceProfile.CLASSIC
                    }
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}

/** Format a float to N decimal places as a string. */
private fun Float.format(decimals: Int): String =
    "%.${decimals}f".format(this)
