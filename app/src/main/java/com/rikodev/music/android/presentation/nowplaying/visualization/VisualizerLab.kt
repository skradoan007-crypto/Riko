package com.rikodev.music.android.presentation.nowplaying.visualization

import androidx.compose.ui.graphics.Color
import com.rikodev.music.android.data.repository.VisualizerPrefs
import com.rikodev.music.android.data.repository.VisualizerTheme

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — VISUALIZER LAB MODEL
//
// The Lab is a product surface, not a demo canvas: users pick a preset,
// switch the colour identity, tune sensitivity / glow / detail, and every
// choice persists. This file is the pure part of that model — presets as
// bundles of settings, preset⇄prefs matching (chip selection is derived,
// never stored, so state can never go stale), and theme → draw-colour
// mapping. UI lives in AuroraSpectrum.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * One-tap configurations. Each bundle is a complete, coherent Lab setup —
 * tapping a preset applies ALL of its fields at once (one atomic write),
 * then the user refines from there.
 *
 *   SIGNATURE — the RIKO default: artwork palette, contour, full detail.
 *   CALM      — soft violet drift, slower feel (low sensitivity), sparse.
 *   PULSE     — cyan bars, hotter response, rich band count.
 *   STUDIO    — monochrome metering, no glow: for precise listening.
 */
enum class VisualizerLabPreset(
    val label: String,
    val styleName: String,
    val theme: VisualizerTheme,
    val sensitivity: Float,
    val glowEnabled: Boolean,
    val bandDensity: Int
) {
    SIGNATURE("Signature", "AURORA", VisualizerTheme.ALBUM_ACCENT, 1.0f, true, 40),
    CALM("Calm", "AURORA", VisualizerTheme.AURORA_VIOLET, 0.7f, true, 32),
    PULSE("Pulse", "CLASSIC", VisualizerTheme.NEON_CYAN, 1.3f, true, 48),
    STUDIO("Studio", "CLASSIC", VisualizerTheme.MONO, 1.0f, false, 48),
    SMOOTH("Smooth", "LINE", VisualizerTheme.ALBUM_ACCENT, 1.0f, true, 40),
    MINIMAL("Minimal", "DOT", VisualizerTheme.MONO, 0.9f, false, 32),
    BARS("Bars", "WAVEFORM", VisualizerTheme.NEON_CYAN, 1.1f, true, 48);

    fun matches(prefs: VisualizerPrefs): Boolean =
        styleName == prefs.styleName &&
            theme == prefs.theme &&
            sensitivity == prefs.sensitivity &&
            glowEnabled == prefs.glowEnabled &&
            bandDensity == prefs.bandDensity

    /** Apply this bundle on top of the live prefs (keeps [VisualizerPrefs.enabled]). */
    fun applyTo(prefs: VisualizerPrefs): VisualizerPrefs = prefs.copy(
        styleName = styleName,
        theme = theme,
        sensitivity = sensitivity,
        glowEnabled = glowEnabled,
        bandDensity = bandDensity
    )
}

/** Derive the currently matching preset, or null when the setup is custom. */
fun matchingPreset(prefs: VisualizerPrefs): VisualizerLabPreset? =
    VisualizerLabPreset.entries.firstOrNull { it.matches(prefs) }

/**
 * Theme → renderer colours. ALBUM_ACCENT defers to the artwork-derived
 * palette (one song = one environment); fixed themes are stable identities.
 */
fun VisualizerTheme.drawColors(album: SpectrumDrawColors): SpectrumDrawColors = when (this) {
    VisualizerTheme.ALBUM_ACCENT -> album
    VisualizerTheme.NEON_CYAN -> SpectrumDrawColors(
        waveStart = Color(0xFF22D3EE),
        waveEnd = Color(0xFF2563EB),
        primary = Color(0xFF67E8F9),
        ambientGlow = Color(0xFF0E7490)
    )
    VisualizerTheme.AURORA_VIOLET -> SpectrumDrawColors(
        waveStart = Color(0xFFA78BFA),
        waveEnd = Color(0xFF6D28D9),
        primary = Color(0xFFC4B5FD),
        ambientGlow = Color(0xFF4C1D95)
    )
    VisualizerTheme.MONO -> SpectrumDrawColors(
        waveStart = Color(0xFFE5E7EB),
        waveEnd = Color(0xFF6B7280),
        primary = Color(0xFFF9FAFB),
        ambientGlow = Color(0xFF374151)
    )
}

/** Swatch gradient for the theme picker chip. */
fun VisualizerTheme.swatchColors(album: SpectrumDrawColors): List<Color> =
    drawColors(album).let { listOf(it.waveStart, it.primary, it.waveEnd) }

/** Display band buckets offered in the Lab (sparse / balanced / rich). */
enum class VisualizerDensity(val label: String, val bands: Int) {
    SPARSE("Sparse", 32),
    BALANCED("Balanced", 40),
    RICH("Rich", 48);

    companion object {
        fun forBands(bands: Int): VisualizerDensity =
            entries.firstOrNull { it.bands == bands } ?: BALANCED
    }
}
