package com.rikodev.music.android.core.experience

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — COLOR STATE
//
// The single source of truth for ALL color intelligence in Riko.
// Published by ColorIntelligenceEngine. Read by Wave, Glass, Motion, Nav.
//
// This replaces the flat 6-field AlbumArtColors with a rich,
// analysis-driven palette that understands the artwork AND the audio.
//
// ⭐ GOLDEN RULE ⭐
//   Nobody computes ColorState.
//   Everybody subscribes.
//   Only Color Intelligence Engine writes.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Temperature classification of the current artwork.
 * Derived from hue distribution analysis.
 */
enum class ColorTemperature {
    WARM,       // Reds, oranges, yellows dominate
    COOL,       // Blues, greens, purples dominate
    NEUTRAL,    // Grayscale, near-grayscale, or balanced mix
    WARM_ACCENT // Cool base with warm accent (or vice versa)
}

/**
 * The unified color intelligence state.
 *
 * Every field is derived from album art analysis + audio energy context.
 * This is NOT a raw palette dump — each color has a SPECIFIC PURPOSE
 * in the Riko visual language.
 *
 * INVARIANT: All Compose Color values are fully opaque (alpha = 1.0)
 * unless explicitly noted (glow/tint colors may have alpha).
 */
data class ColorState(

    // ── Core Palette (from album art analysis) ───────────────────────────
    /** Primary accent — the most recognizable color from the artwork. */
    val primary: Color = Color(0xFF19B7FF),

    /** Secondary accent — complementary or analogous to primary. */
    val secondary: Color = Color(0xFF9A6CFF),

    /** Tertiary accent — for small highlights, badges, notifications. */
    val accent: Color = Color(0xFFFF6FD8),

    // ── Audio-Reactive Colors ────────────────────────────────────────────
    /**
     * Wave start color — the color at the base/root of the waveform ribbon.
     * Derives from primary but shifts warm with bass energy.
     */
    val waveStart: Color = Color(0xFF19B7FF),

    /**
     * Wave end color — the color at the tip/crest of the waveform ribbon.
     * Derives from secondary but brightens with treble energy.
     */
    val waveEnd: Color = Color(0xFF9A6CFF),

    /** Glass tint — subtle color overlay for glass surfaces when audio is active. */
    val glassTint: Color = Color(0x14FFFFFF),

    /** Background gradient top color — derived from dominant dark swatch. */
    val backgroundGradient: Color = Color(0xFF09111D),

    /** Ambient glow color — the soft halo behind the album art. Responds to energy. */
    val ambientGlow: Color = Color(0x3319B7FF),

    /** Shadow color — tinted shadows instead of pure black. From dominant hue. */
    val shadowColor: Color = Color(0x33000000),

    // ── Analysis Metadata (read by engines for routing decisions) ────────
    /** Dominant luminance 0f..1f — how bright the artwork's dominant color is. */
    val dominantLuminance: Float = 0.15f,

    /** True if the artwork is predominantly dark (luminance < 0.25). */
    val isDarkArtwork: Boolean = true,

    /**
     * True if the artwork is essentially grayscale / monochrome.
     * Triggers special rendering: no hue shifts, use default accent palette.
     */
    val isMonochrome: Boolean = false,

    /** Temperature classification of the artwork. */
    val temperature: ColorTemperature = ColorTemperature.COOL,

    /** Contrast level 0f..1f — how much contrast between light and dark areas. */
    val contrastLevel: Float = 0.5f,

    /** Hue of the dominant color (0..360). -1 if monochrome. */
    val dominantHue: Float = -1f,

    /** Saturation of the dominant color 0f..1f. */
    val dominantSaturation: Float = 0f,

    // ── Energy-Reactive Modifiers (updated per frame by engine) ──────────
    /** Current audio energy mapped to color intensity 0f..1f. */
    val energyIntensity: Float = 0f,

    /** How much the palette should "pulse" with the beat 0f..1f. */
    val pulseAmount: Float = 0f,

    /** How much the glass should tint with the dominant color 0f..1f. */
    val tintStrength: Float = 0f,

    // ── Source tracking ────────────────────────────────────────────────
    /** True if this state was derived from a real album art (not defaults). */
    val isFromArtwork: Boolean = false
) {
    /**
     * Convenience: get a list of the core palette for gradient builders.
     */
    val corePalette: List<Color>
        get() = listOf(primary, secondary, accent)

    /**
     * Convenience: get wave gradient colors as a pair.
     */
    val waveGradient: Pair<Color, Color>
        get() = waveStart to waveEnd

    /**
     * Convenience: is this the idle/default state?
     */
    val isIdle: Boolean
        get() = !isFromArtwork && energyIntensity == 0f

    companion object {
        /** Pre-built idle state — used before any artwork is loaded. */
        val IDLE = ColorState()
    }
}

/**
 * Palette analysis result — intermediate data produced by PaletteAnalyzer
 * before ColorIntelligenceEngine converts it to ColorState.
 */
data class PaletteAnalysis(
    /** All swatches extracted from the artwork. */
    val dominant: Int = 0,
    val vibrant: Int = 0,
    val darkVibrant: Int = 0,
    val lightVibrant: Int = 0,
    val muted: Int = 0,
    val darkMuted: Int = 0,

    // ── Derived properties ────────────────────────────────────────────────
    val hue: Float = -1f,
    val saturation: Float = 0f,
    val luminance: Float = 0f,
    val isDark: Boolean = true,
    val isMono: Boolean = false,
    val temperature: ColorTemperature = ColorTemperature.COOL,
    val contrast: Float = 0.5f
)
