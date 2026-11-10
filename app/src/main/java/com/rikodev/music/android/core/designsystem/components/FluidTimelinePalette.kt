package com.rikodev.music.android.core.designsystem.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** A small rendering projection of existing ColorIntelligenceEngine swatches.
 * No artwork decoding, global palette engine or fixed neon colour catalogue.
 */
internal object FluidTimelinePalette {
    data class Tones(val body: Color, val depth: Color, val highlight: Color, val head: Color)
    private fun hue(color: Color): Float {
        val hi = maxOf(color.red, color.green, color.blue)
        val lo = minOf(color.red, color.green, color.blue)
        val delta = hi - lo
        if (delta < 0.025f) return -1f
        return ((when (hi) {
            color.red -> (color.green - color.blue) / delta
            color.green -> (color.blue - color.red) / delta + 2f
            else -> (color.red - color.green) / delta + 4f
        }) * 60f + 360f) % 360f
    }
    fun forArtwork(main: Color, secondary: Color = main): Tones {
        val primaryHue = hue(main)
        if (primaryHue < 0f) return Tones(Color(0xFFB4BBC5), Color(0xFF7F8A98), Color(0xFFF1F4F8), Color(0xFFD0D7E0))
        val secondHue = hue(secondary)
        val distance = if (secondHue < 0f) 0f else ((secondHue - primaryHue + 540f) % 360f) - 180f
        val saturation = ((maxOf(main.red, main.green, main.blue) - minOf(main.red, main.green, main.blue)) /
            maxOf(main.red, main.green, main.blue).coerceAtLeast(0.01f)).coerceIn(0.30f, 0.76f)
        val body = Color.hsv(primaryHue, saturation, 0.93f)
        val depth = Color.hsv((primaryHue + distance.coerceIn(-32f, 32f) + 360f) % 360f,
            (saturation * 0.85f).coerceIn(0.25f, 0.68f), 0.87f)
        return Tones(body, depth, lerp(body, Color.White, 0.64f), lerp(body, Color.White, 0.12f))
    }
    fun playheadForArtwork(main: Color): Color = forArtwork(main).head
}
