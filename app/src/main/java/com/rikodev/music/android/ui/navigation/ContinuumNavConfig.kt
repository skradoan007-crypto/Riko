package com.rikodev.music.android.ui.navigation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

enum class ContinuumTab(val label: String) {
    EXPLORE("Explore"),   // Left: Ringed Planet
    LISTEN("Listen"),     // Center: Headphones (default)
    LIBRARY("Library")    // Right: 3-Layer Isometric Stack
}

enum class ContinuumRenderTier {
    ULTRA,      // Full BlurMaskFilter + Multi-pass silk caustics + Idle pulse & sonar rings
    BALANCED,   // Medium blur passes, smooth 60fps for mid-range devices
    LITE        // Zero BlurMaskFilter, pure radial/linear gradient falloff for low-RAM devices
}

@Immutable
data class ContinuumNavColors(
    // Pixel-matched frosted twilight indigo glass visor
    val glassTop: Color = Color(0xDB2A2654),
    val glassMid: Color = Color(0xE825214C),
    val glassBottom: Color = Color(0xF21E1A40),
    val glassRimCenter: Color = Color(0x8CD8CCFF),
    val glassRimSide: Color = Color(0x3C766AC2),

    // Creamy volumetric silk veil & caustic folds
    val silkDeepIndigo: Color = Color(0xFF3E2D94),
    val silkRoyalViolet: Color = Color(0xFF6B4AE0),
    val silkElectricViolet: Color = Color(0xFF9C6CFF),
    val silkBrightLavender: Color = Color(0xFFE6D4FF),
    val silkPureWhite: Color = Color(0xFFFCF8FF),

    // Deep royal-indigo Fresnel plasma orb + 6 o'clock lens-flare caustic
    val orbOuterBloom: Color = Color(0xFF834CF5),
    val orbOuterDeep: Color = Color(0xFF4F24C2),
    val orbInnerCenter: Color = Color(0xFF3C2D96),
    val orbInnerMid: Color = Color(0xFF533EC4),
    val orbInnerEdge: Color = Color(0xFF7E62EB),
    val orbInnerRimGlow: Color = Color(0xFFB9A0FF),
    val orbFlareCore: Color = Color(0xFFFCF6FF),
    val orbFlareMid: Color = Color(0xFFD5B4FF),
    val orbRimTopBot: Color = Color(0xFFFDF8FF),
    val orbRimSide: Color = Color(0xFF9B75F0),

    // Clean translucent frosted glass dome pockets
    val pocketInner: Color = Color(0x8C2C2756),
    val pocketEdge: Color = Color(0x529085C8),
    val pocketRim: Color = Color(0xFFDAD2FA),

    val activeIcon: Color = Color(0xFFFFFFFF),
    val inactiveIcon: Color = Color(0xEAEAE6FF),
    val gesturePill: Color = Color(0x66C8BFE8)
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
            val leftX = width * 0.152f
            val centerX = width * 0.500f
            val rightX = width * 0.848f
            return ContinuumMetrics(
                width = width,
                height = height,
                centerY = centerY,
                slotX = floatArrayOf(leftX, centerX, rightX),
                inactiveRadius = height * 0.300f,
                activeRadius = height * 0.375f
            )
        }
    }
}
