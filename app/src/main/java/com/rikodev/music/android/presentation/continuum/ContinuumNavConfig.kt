package com.rikodev.music.android.presentation.continuum

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

enum class ContinuumTab(val label: String) {
    EXPLORE("Explore"),   // Left: Ringed Planet
    LISTEN("Listen"),     // Center: Headphones
    LIBRARY("Library")    // Right: 3-Layer Stack
}

enum class ContinuumRenderTier {
    ULTRA,      // Full BlurMaskFilter + Multi-pass silk caustics + Screen/Add blending
    BALANCED,   // Medium blur passes, smooth 60fps for mid-range devices
    LITE        // Zero BlurMaskFilter, pure radial/linear gradient falloff for low-RAM devices
}

@Immutable
data class ContinuumNavColors(
    val glassTop: Color = Color(0xCC181330),
    val glassMid: Color = Color(0xE0110D24),
    val glassBottom: Color = Color(0xF00A0718),
    val glassRimCenter: Color = Color(0x66E9D5FF),
    val glassRimSide: Color = Color(0x289333EA),

    val silkDeepViolet: Color = Color(0xFF6D28D9),
    val silkElectricPurple: Color = Color(0xFF9333EA),
    val silkNeonOrchid: Color = Color(0xFFC084FC),
    val silkHighlightWhite: Color = Color(0xFFF5D0FE),

    val orbOuterBloom: Color = Color(0xFF7E22CE),
    val orbInnerCore: Color = Color(0xFF3B0764),
    val orbBottomHotspot: Color = Color(0xFFC026D3),
    val orbRimBright: Color = Color(0xFFFAF5FF),
    val orbRimMid: Color = Color(0xFFD8B4FE),

    val activeIcon: Color = Color(0xFFFFFFFF),
    val inactiveIcon: Color = Color(0xD9E9D5FF),
    val gesturePill: Color = Color(0x52D8B4FE)
)

internal data class ContinuumMetrics(
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
            val leftX = width * 0.165f
            val centerX = width * 0.500f
            val rightX = width * 0.835f
            return ContinuumMetrics(
                width = width,
                height = height,
                centerY = centerY,
                slotX = floatArrayOf(leftX, centerX, rightX),
                inactiveRadius = height * 0.295f,
                activeRadius = height * 0.365f
            )
        }
    }
}