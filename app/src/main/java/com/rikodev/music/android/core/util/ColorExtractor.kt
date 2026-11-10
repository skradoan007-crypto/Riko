package com.rikodev.music.android.core.util

import android.graphics.Bitmap
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.rikodev.music.android.domain.model.AlbumArtColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — COLOR EXTRACTOR
//
// This is the SOUL of the reactive UI. Every song's album art produces a
// unique color palette that flows through the entire screen:
//   background gradient, glass tint, waveform color, EQ bar colors.
//
// Pipeline: Bitmap → Palette.generate() → extract swatches →
//           force-dark for readability → AlbumArtColors
//
// Critical rule: the background MUST stay dark (luminance < 0.22) no matter
// what the album art looks like, or the glass surfaces become invisible.
// This is why we always derive from darkVibrant/darkMuted and manually
// darken anything too bright.
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class ColorExtractor @Inject constructor() {

    /**
     * Extract a full color palette from an album art bitmap.
     * Runs on Default dispatcher since Palette generation is CPU-bound.
     */
    suspend fun extractColors(bitmap: Bitmap?): AlbumArtColors = withContext(Dispatchers.Default) {
        if (bitmap == null) return@withContext AlbumArtColors()

        try {
            val palette = Palette.from(bitmap)
                .maximumColorCount(24)
                .generate()

            val vibrant = palette.vibrantSwatch?.rgb
            val darkVibrant = palette.darkVibrantSwatch?.rgb
            val lightVibrant = palette.lightVibrantSwatch?.rgb
            val muted = palette.mutedSwatch?.rgb
            val darkMuted = palette.darkMutedSwatch?.rgb
            val dominant = palette.dominantSwatch?.rgb

            // Background must ALWAYS be dark — force luminance down if needed
            val safeDominant = forceDarkEnough(
                darkVibrant ?: darkMuted ?: dominant ?: 0xFF0A0A1A.toInt(),
                maxLuminance = 0.18
            )

            val safeDarkVibrant = forceDarkEnough(
                darkVibrant ?: dominant ?: 0xFF0A0A1A.toInt(),
                maxLuminance = 0.15
            )

            val safeDarkMuted = forceDarkEnough(
                darkMuted ?: dominant ?: 0xFF0F0F20.toInt(),
                maxLuminance = 0.12
            )

            // Vibrant accent should stay vibrant — boost saturation if too dull
            val safeVibrant = ensureVibrant(vibrant ?: dominant ?: 0xFF00D4FF.toInt())

            AlbumArtColors(
                dominant = safeDominant,
                vibrant = safeVibrant,
                darkVibrant = safeDarkVibrant,
                lightVibrant = lightVibrant ?: lightenColor(safeVibrant),
                muted = muted ?: safeDominant,
                darkMuted = safeDarkMuted
            )
        } catch (e: Exception) {
            AlbumArtColors()  // fallback to defaults
        }
    }

    /**
     * Force a color's luminance below a threshold by scaling down RGB.
     * Preserves hue/saturation character while ensuring it works as a dark bg.
     */
    private fun forceDarkEnough(color: Int, maxLuminance: Double): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        if (hsl[2] > maxLuminance) {
            hsl[2] = maxLuminance.toFloat()
        }
        // Also cap saturation slightly so it doesn't look neon-on-black harsh
        if (hsl[1] > 0.65f) hsl[1] = 0.65f
        return ColorUtils.HSLToColor(hsl)
    }

    /**
     * Ensure an accent color has enough saturation/lightness to pop on dark glass.
     */
    private fun ensureVibrant(color: Int): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        // BUG FIX: for a grayscale/near-grayscale source colour (e.g. a
        // black-and-white album cover), hue is mathematically undefined —
        // ColorUtils defaults it to 0°, which IS red. Boosting saturation
        // on that undefined hue was manufacturing a fake red tint out of
        // an image with no colour information at all. When saturation is
        // this low, treat it as "no real colour" and use a calm, neutral
        // default hue instead of trusting the meaningless one.
        if (hsl[1] < 0.08f) {
            hsl[0] = 205f
            hsl[1] = 0.35f
            hsl[2] = hsl[2].coerceIn(0.55f, 0.75f)
            return ColorUtils.HSLToColor(hsl)
        }
        if (hsl[1] < 0.45f) hsl[1] = 0.45f
        if (hsl[2] < 0.45f) hsl[2] = 0.45f
        if (hsl[2] > 0.75f) hsl[2] = 0.75f
        return ColorUtils.HSLToColor(hsl)
    }

    private fun lightenColor(color: Int): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        hsl[2] = (hsl[2] + 0.25f).coerceAtMost(0.92f)
        hsl[1] = (hsl[1] * 0.7f)
        return ColorUtils.HSLToColor(hsl)
    }
}
