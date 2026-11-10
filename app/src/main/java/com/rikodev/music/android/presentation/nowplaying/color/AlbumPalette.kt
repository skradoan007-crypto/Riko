package com.rikodev.music.android.presentation.nowplaying.color

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import kotlin.math.abs

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — ALBUM PALETTE (master-prompt §7)
//
// Palette → PlasmaColors (accent + ridge set). Accents are punched to the
// reference saturation/value range (S 0.60-0.82, V 0.92-1.0) regardless of
// dull art; ridges use a fixed cool trio + the accent. Near-grayscale art
// falls back to #4A86FF. A ridge within ±25° of the accent hue is shifted
// +40° so layers stay distinguishable.
//
// Two mappings behind ColorMode (ΔE00 calibration chooses):
//   FixedTrioPlusAccent (default; matches R1/R2 measurements)
//   SwatchDriven (vibrant/lightVibrant/darkVibrant/muted → ridges)
// ─────────────────────────────────────────────────────────────────────────────

enum class ColorMode { FixedTrioPlusAccent, SwatchDriven }

data class PlasmaColors(
    val accent: Color,
    val c0: Color,
    val c1: Color,
    val c2: Color
) {
    companion object {
        val DEFAULT = PlasmaColors(
            accent = Color(0xFF3F89FD),
            c0 = Color(0xFF2B59E2),
            c1 = Color(0xFF771FFD),
            c2 = Color(0xFFF942E8)
        )
    }
}

object AlbumPalette {

    const val FALLBACK_ACCENT = 0xFF4A86FFL
    private const val GRAYSCALE_SAT_THRESHOLD = 0.15f

    // Fixed ridge trio (measured R1/R2).
    private const val FIXED_BLUE = 0xFF2B59E2L
    private const val FIXED_VIOLET = 0xFF771FFDL
    private const val FIXED_MAGENTA = 0xFFF942E8L

    /**
     * Punch transform: S = max(S, 0.70), V = max(V, 0.95). Grayscale
     * (S < 0.15) → fallback accent.
     */
    fun punch(argb: Int): Int {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        if (hsv[1] < GRAYSCALE_SAT_THRESHOLD) return FALLBACK_ACCENT.toInt()
        hsv[1] = maxOf(hsv[1], 0.70f)
        hsv[2] = maxOf(hsv[2], 0.95f)
        return android.graphics.Color.HSVToColor(android.graphics.Color.alpha(argb), hsv)
    }

    /** Hue-shift a ridge by +degrees when it collides with the accent hue. */
    fun shiftHue(argb: Int, degrees: Float): Int {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        hsv[0] = (hsv[0] + degrees) % 360f
        return android.graphics.Color.HSVToColor(android.graphics.Color.alpha(argb), hsv)
    }

    fun hueOf(argb: Int): Float {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        return hsv[0]
    }

    fun hueDistance(a: Float, b: Float): Float {
        val d = abs(a - b) % 360f
        return if (d > 180f) 360f - d else d
    }

    /** Fixed trio + accent, with collision hue-shift. */
    fun fixedTrioPlusAccent(seedArgb: Int): PlasmaColors {
        val accent = punch(seedArgb)
        val accentHue = hueOf(accent)
        var c0 = FIXED_BLUE.toInt()
        var c1 = FIXED_VIOLET.toInt()
        val c2 = FIXED_MAGENTA.toInt()
        if (hueDistance(hueOf(c0), accentHue) < 25f) c0 = shiftHue(c0, 40f)
        if (hueDistance(hueOf(c1), accentHue) < 25f) c1 = shiftHue(c1, 40f)
        return PlasmaColors(
            accent = Color(accent),
            c0 = Color(c0),
            c1 = Color(c1),
            c2 = Color(c2)
        )
    }

    /**
     * Swatch-driven mapping (original uColorLeft/Right idea): vibrant →
     * accent, lightVibrant → c1, darkVibrant → c0, muted → c2, each punched.
     * Missing swatches fall back to the fixed trio entry.
     */
    fun swatchDriven(
        vibrant: Int?,
        lightVibrant: Int?,
        darkVibrant: Int?,
        muted: Int?
    ): PlasmaColors {
        val accent = punch(vibrant ?: FALLBACK_ACCENT.toInt())
        return PlasmaColors(
            accent = Color(accent),
            c0 = Color(punch(darkVibrant ?: FIXED_BLUE.toInt())),
            c1 = Color(punch(lightVibrant ?: FIXED_VIOLET.toInt())),
            c2 = Color(punch(muted ?: FIXED_MAGENTA.toInt()))
        )
    }

    fun resolve(
        seedArgb: Int?,
        mode: ColorMode = ColorMode.FixedTrioPlusAccent
    ): PlasmaColors {
        val seed = seedArgb ?: FALLBACK_ACCENT.toInt()
        return when (mode) {
            ColorMode.FixedTrioPlusAccent -> fixedTrioPlusAccent(seed)
            ColorMode.SwatchDriven -> swatchDriven(seed, null, null, null)
        }
    }

    /**
     * White-on-scrim contrast guard: text is always white on ≥55% black,
     * which passes 4.5:1 even against pure-white artwork. Returns true when
     * [scrimAlpha] meets the floor.
     */
    fun scrimPassesContrast(scrimAlpha: Float): Boolean = scrimAlpha >= 0.55f

    /** ΔE-ish hue distance helper for calibration reports (0 = identical). */
    fun deltaHue(a: Int, b: Int): Float = hueDistance(hueOf(a), hueOf(b))

    fun toArgb(color: Color): Int = color.toArgb()
    fun luminance(argb: Int): Double = ColorUtils.calculateLuminance(argb)
}
