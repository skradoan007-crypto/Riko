package com.rikodev.music.android.ui.navigation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

enum class ContinuumTab(val label: String) {
    EXPLORE("Explore"),   // Left: Ringed Planet
    LISTEN("Listen"),     // Center: Headphones (default)
    LIBRARY("Library")    // Right: 3-Layer Isometric Stack
}

enum class ContinuumRenderTier {
    ULTRA,      // Full multi-pass silk caustics + Idle pulse & sonar rings
    BALANCED,   // Medium passes, smooth 60fps for mid-range devices
    LITE        // Pure radial/linear gradient falloff for low-RAM devices
}

@Immutable
data class ContinuumNavColors(
    // Frosted obsidian-teal glass visor (default = Emerald Dark)
    val glassTop: Color = Color(0xD90A2424),
    val glassMid: Color = Color(0xE6071A1A),
    val glassBottom: Color = Color(0xF0051314),
    val glassRimCenter: Color = Color(0x99A7F3D0),
    val glassRimSide: Color = Color(0x4510B981),

    // Volumetric emerald-jade diagonal silk veil & caustic folds
    val silkDeepIndigo: Color = Color(0xFF064E3B),
    val silkRoyalViolet: Color = Color(0xFF059669),
    val silkElectricViolet: Color = Color(0xFF10B981),
    val silkBrightLavender: Color = Color(0xFF6EE7B7),
    val silkPureWhite: Color = Color(0xFFECFDF5),

    // Deep translucent emerald-teal Fresnel orb + electric emerald outer glow
    val orbOuterBloom: Color = Color(0xFF2DD4BF),
    val orbOuterDeep: Color = Color(0xFF059669),
    val orbInnerCenter: Color = Color(0xFF052E2B),
    val orbInnerMid: Color = Color(0xFF0D6E5E),
    val orbInnerEdge: Color = Color(0xFF10B981),
    val orbInnerRimGlow: Color = Color(0xFF6EE7B7),
    val orbFlareCore: Color = Color(0xFFECFDF5),
    val orbFlareMid: Color = Color(0xFF34D399),
    val orbRimTopBot: Color = Color(0xFFECFDF5),
    val orbRimSide: Color = Color(0xFF2DD4BF),

    // Clean translucent frosted obsidian-teal dome pockets
    val pocketInner: Color = Color(0x8C082222),
    val pocketEdge: Color = Color(0x5234D399),
    val pocketRim: Color = Color(0xFFA7F3D0),

    val activeIcon: Color = Color(0xFFFFFFFF),
    val inactiveIcon: Color = Color(0xE6D1FAE5),
    val gesturePill: Color = Color(0x666EE7B7),
    val isLightMode: Boolean = false
) {
    companion object {
        /** Emerald Dark preset — deep obsidian-teal dock, emerald-jade silk, mint-white neon liner. */
        fun emeraldDark(): ContinuumNavColors = ContinuumNavColors()

        /** Emerald Light preset — frosted pearl-mint dock, aqua-teal silk, rich teal orb, dark marine inactive icons. */
        fun emeraldLight(): ContinuumNavColors = ContinuumNavColors(
            glassTop = Color(0xDCE8FAF4),
            glassMid = Color(0xE6D9F5EC),
            glassBottom = Color(0xEFC8EFE3),
            glassRimCenter = Color(0xCCFFFFFF),
            glassRimSide = Color(0x590D9488),

            silkDeepIndigo = Color(0xFF0D9488),
            silkRoyalViolet = Color(0xFF14B8A6),
            silkElectricViolet = Color(0xFF2DD4BF),
            silkBrightLavender = Color(0xFF99F6E4),
            silkPureWhite = Color(0xFFFFFFFF),

            orbOuterBloom = Color(0xFF14B8A6),
            orbOuterDeep = Color(0xFF0D9488),
            orbInnerCenter = Color(0xFF0B5C54),
            orbInnerMid = Color(0xFF0F766E),
            orbInnerEdge = Color(0xFF14B8A6),
            orbInnerRimGlow = Color(0xFF99F6E4),
            orbFlareCore = Color(0xFFFFFFFF),
            orbFlareMid = Color(0xFF5EEAD4),
            orbRimTopBot = Color(0xFFFFFFFF),
            orbRimSide = Color(0xFF5EEAD4),

            pocketInner = Color(0xA6CCF3E8),
            pocketEdge = Color(0x660D9488),
            pocketRim = Color(0xFFFFFFFF),

            activeIcon = Color(0xFFFFFFFF),
            inactiveIcon = Color(0xE60B3B3C),
            gesturePill = Color(0x660B3B3C),
            isLightMode = true
        )
    }
}

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
