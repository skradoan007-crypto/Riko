package com.rikodev.music.android.core.experience

import androidx.core.graphics.ColorUtils
import javax.inject.Inject
import javax.inject.Singleton
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — PALETTE ANALYZER
//
// Analyzes raw color swatches (from Android Palette API) to extract
// intelligence about the artwork: temperature, luminance, contrast,
// monochrome detection, dominant hue/saturation.
//
// This is the "brain" between raw palette data and ColorState.
// It does NOT produce Compose Colors — it produces analysis metrics
// that ColorIntelligenceEngine uses to make rendering decisions.
//
// RESPONSIBILITY: Pure analysis. No rendering, no Flow, no Composable.
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class PaletteAnalyzer @Inject constructor() {

    /**
     * Analyze a set of color swatches and return rich palette intelligence.
     *
     * @param swatches Raw colors from Palette API (dominant, vibrant, etc.)
     * @return PaletteAnalysis with all derived metrics
     */
    fun analyze(swatches: PaletteSwatches): PaletteAnalysis {
        val dominant = swatches.dominant
        val vibrant = swatches.vibrant

        // If no real data, return empty analysis
        if (dominant == 0 && vibrant == 0) {
            return PaletteAnalysis()
        }

        // Analyze the dominant color first
        val dominantHsl = colorToHSL(dominant)
        val vibrantHsl = colorToHSL(vibrant)

        // Analyze luminance spread across all non-zero swatches
        val allColors = listOfNotNull(
            dominant.takeIf { it != 0 },
            vibrant.takeIf { it != 0 },
            swatches.darkVibrant.takeIf { it != 0 },
            swatches.lightVibrant.takeIf { it != 0 },
            swatches.muted.takeIf { it != 0 },
            swatches.darkMuted.takeIf { it != 0 }
        )

        val luminances = allColors.map { colorToHSL(it)[2] }
        val luminance = luminances.average().toFloat()
        val minLum = luminances.minOrNull() ?: 0f
        val maxLum = luminances.maxOrNull() ?: 0f

        return PaletteAnalysis(
            dominant = dominant,
            vibrant = vibrant,
            darkVibrant = swatches.darkVibrant,
            lightVibrant = swatches.lightVibrant,
            muted = swatches.muted,
            darkMuted = swatches.darkMuted,

            // Hue: from dominant (or vibrant if dominant is gray)
            hue = if (dominantHsl[1] > 0.08f) dominantHsl[0]
                  else if (vibrantHsl[1] > 0.08f) vibrantHsl[0]
                  else -1f,

            // Saturation: weighted average of dominant + vibrant
            saturation = (dominantHsl[1] + vibrantHsl[1] * 0.5f)
                .coerceIn(0f, 1f) / 1.5f,

            // Luminance: average across all swatches
            luminance = luminance,

            // Dark: average luminance below threshold
            isDark = luminance < 0.25f,

            // Monochrome: very low saturation across all swatches
            isMono = allSaturationLow(allColors),

            // Temperature: based on dominant hue distribution
            temperature = classifyTemperature(dominantHsl, vibrantHsl, allColors),

            // Contrast: spread between darkest and lightest swatch
            contrast = if (maxLum - minLum > 0.01f) {
                ((maxLum - minLum) / maxLum).coerceIn(0f, 1f)
            } else 0f
        )
    }

    // ═════════════════════════════════════════════════════════════════════
    // TEMPERATURE CLASSIFICATION
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Classify the overall temperature of the artwork.
     *
     * Warm hues: 0-60 (red-yellow), 300-360 (magenta-red)
     * Cool hues: 180-300 (cyan-blue-magenta)
     * Neutral: monochrome or very low saturation
     */
    private fun classifyTemperature(
        dominantHsl: FloatArray,
        vibrantHsl: FloatArray,
        allColors: List<Int>
    ): ColorTemperature {
        // Count warm vs cool pixels across all swatches
        var warmWeight = 0f
        var coolWeight = 0f
        var totalWeight = 0f

        for (color in allColors) {
            val hsl = colorToHSL(color)
            val sat = hsl[1]
            if (sat < 0.08f) continue // skip gray

            val weight = sat // saturation = confidence in hue
            totalWeight += weight

            val hue = hsl[0]
            if (isWarmHue(hue)) warmWeight += weight
            else coolWeight += weight
        }

        if (totalWeight < 0.1f) return ColorTemperature.NEUTRAL

        val warmRatio = warmWeight / totalWeight

        return when {
            warmRatio > 0.7f -> ColorTemperature.WARM
            warmRatio < 0.3f -> ColorTemperature.COOL
            else -> {
                // Mixed — check if one dominates by a wider margin
                if (warmRatio > 0.55f) ColorTemperature.WARM_ACCENT
                else ColorTemperature.WARM_ACCENT
            }
        }
    }

    /**
     * Check if a hue angle falls in the "warm" range.
     * Warm: reds (0-30), oranges/yellows (30-70), magentas/pinks (300-360)
     */
    private fun isWarmHue(hue: Float): Boolean {
        return hue in 0f..70f || hue in 300f..360f
    }

    // ═════════════════════════════════════════════════════════════════════
    // MONOCHROME DETECTION
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Check if ALL provided colors have low saturation.
     * If 80%+ of colors have saturation < 0.10, it's monochrome.
     */
    private fun allSaturationLow(colors: List<Int>): Boolean {
        if (colors.isEmpty()) return true
        val lowSatCount = colors.count { colorToHSL(it)[1] < 0.10f }
        return lowSatCount.toFloat() / colors.size > 0.8f
    }

    // ═════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═════════════════════════════════════════════════════════════════════

    private fun colorToHSL(color: Int): FloatArray {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        return hsl
    }
}

/**
 * Raw palette swatches from the Android Palette API.
 * Input to PaletteAnalyzer.
 */
data class PaletteSwatches(
    val dominant: Int = 0,
    val vibrant: Int = 0,
    val darkVibrant: Int = 0,
    val lightVibrant: Int = 0,
    val muted: Int = 0,
    val darkMuted: Int = 0
)
