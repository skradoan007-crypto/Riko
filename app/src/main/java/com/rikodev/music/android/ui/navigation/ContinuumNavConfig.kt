package com.rikodev.music.android.ui.navigation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

enum class ContinuumTab(val label: String) {
    EXPLORE("Explore"),   // Left: Ringed Planet
    LISTEN("Listen"),     // Center: Headphones
    LIBRARY("Library")    // Right: 3-Layer Stack
}

enum class ContinuumRenderTier {
    ULTRA,      // 14 gossamer silk strands + BlurMaskFilter + Fresnel glass orb
    BALANCED,   // 10 gossamer silk strands + medium blur passes
    LITE        // 8 gossamer silk strands + zero BlurMaskFilter for low-RAM devices
}

@Immutable
data class ContinuumNavColors(
    // Pixel-matched frosted twilight indigo visor
    val glassTop: Color = Color(0xD924224A),
    val glassMid: Color = Color(0xE81A173B),
    val glassBottom: Color = Color(0xF512102B),
    val glassRimCenter: Color = Color(0x78A89CE8),
    val glassRimSide: Color = Color(0x2252478A),

    // Pixel-matched cool cosmic indigo & periwinkle silk
    val silkDeepIndigo: Color = Color(0xFF4736A9),
    val silkElectricIndigo: Color = Color(0xFF6B5CE7),
    val silkPeriwinkle: Color = Color(0xFFA67AFF),
    val silkIceLavender: Color = Color(0xFFE9DDFF),

    // Pixel-matched Fresnel plasma orb (no hot magenta blotch)
    val orbOuterBloom: Color = Color(0xFF7B61FF),
    val orbInnerCenter: Color = Color(0xFF483BA7),
    val orbInnerEdge: Color = Color(0xFF8D7DF7),
    val orbBottomCaustic: Color = Color(0xFFDCCBFF),
    val orbRimBright: Color = Color(0xFFFEEBFF),
    val orbRimSide: Color = Color(0xFFAA87FF),

    val pocketRimHighlight: Color = Color(0xFFA298CC),
    val activeIcon: Color = Color(0xFFFFFFFF),
    val inactiveIcon: Color = Color(0xD9E4DCFF),
    val gesturePill: Color = Color(0x4FC8BEF2)
)

internal class ContinuumMetrics(
    val width: Float,
    val height: Float,
    val centerY: Float,
    val slotX: FloatArray,
    val inactiveRadius: Float,
    val activeRadius: Float
) {
    companion object {
        fun calculate(width: Float, height: Float): ContinuumMetrics {
            val centerY = height * 0.50f
            val leftX = width * 0.155f
            val centerX = width * 0.500f
            val rightX = width * 0.845f
            return ContinuumMetrics(
                width = width,
                height = height,
                centerY = centerY,
                slotX = floatArrayOf(leftX, centerX, rightX),
                inactiveRadius = height * 0.305f,
                activeRadius = height * 0.375f
            )
        }
    }
}
