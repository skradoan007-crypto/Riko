package com.rikodev.sync.nexmesh.feature.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Adaptive Liquid Glass + Modern Glassmorphism Palette supporting both
 * refined Light Mode and calm Obsidian Dark Mode — zero neon clutter.
 */
@Immutable
data class NexMeshPalette(
    val isDark: Boolean,
    val bgTop: Color,
    val bgMid: Color,
    val bgBottom: Color,
    val ambientOrbA: Color,
    val ambientOrbB: Color,
    val glassSurfaceTop: Color,
    val glassSurfaceBottom: Color,
    val glassElevatedTop: Color,
    val glassElevatedBottom: Color,
    val glassRimTop: Color,
    val glassRimBottom: Color,
    val glassShadow: Color,
    val accentPrimary: Color,
    val accentPillTop: Color,
    val accentPillBottom: Color,
    val mintPillTop: Color,
    val mintPillBottom: Color,
    val switchActiveTrack: Color,
    val inkPrimary: Color,
    val inkSecondary: Color,
    val inkAccent: Color,
    val statusOnline: Color,
    val statusWarning: Color,
    val statusOffline: Color
)

val NexMeshLightPalette = NexMeshPalette(
    isDark = false,
    bgTop = Color(0xFFF6F7FB),
    bgMid = Color(0xFFEEF1FA),
    bgBottom = Color(0xFFE1E9F6),
    ambientOrbA = Color(0x45B7D4FF),
    ambientOrbB = Color(0x38D6C7FF),
    glassSurfaceTop = Color(0xC8FFFFFF),
    glassSurfaceBottom = Color(0x96FFFFFF),
    glassElevatedTop = Color(0xE0FFFFFF),
    glassElevatedBottom = Color(0xB8F5F8FF),
    glassRimTop = Color(0xF5FFFFFF),
    glassRimBottom = Color(0x2864748B),
    glassShadow = Color(0x16243658),
    accentPrimary = Color(0xFF3B6EF5),
    accentPillTop = Color(0xDDE4ECFF),
    accentPillBottom = Color(0xCCCDDDFD),
    mintPillTop = Color(0xD8E3F8F2),
    mintPillBottom = Color(0xC2C8EFE4),
    switchActiveTrack = Color(0xFF2FA88F),
    inkPrimary = Color(0xFF101828),
    inkSecondary = Color(0xFF526079),
    inkAccent = Color(0xFF1E3A8A),
    statusOnline = Color(0xFF129A74),
    statusWarning = Color(0xFFD97706),
    statusOffline = Color(0xFFDC2626)
)

val NexMeshDarkPalette = NexMeshPalette(
    isDark = true,
    bgTop = Color(0xFF0C101A),
    bgMid = Color(0xFF111726),
    bgBottom = Color(0xFF151E30),
    ambientOrbA = Color(0x2E3B6EF5),
    ambientOrbB = Color(0x246D4AFF),
    glassSurfaceTop = Color(0x26FFFFFF),
    glassSurfaceBottom = Color(0x14FFFFFF),
    glassElevatedTop = Color(0x33FFFFFF),
    glassElevatedBottom = Color(0x1EFFFFFF),
    glassRimTop = Color(0x4DFFFFFF),
    glassRimBottom = Color(0x14FFFFFF),
    glassShadow = Color(0x55000000),
    accentPrimary = Color(0xFF6B9BFF),
    accentPillTop = Color(0x405B86FA),
    accentPillBottom = Color(0x2B4469D6),
    mintPillTop = Color(0x332FA88F),
    mintPillBottom = Color(0x221E7F6B),
    switchActiveTrack = Color(0xFF34C4A5),
    inkPrimary = Color(0xFFF3F6FC),
    inkSecondary = Color(0xFFA0AEC6),
    inkAccent = Color(0xFFBCD4FF),
    statusOnline = Color(0xFF34D399),
    statusWarning = Color(0xFFFBBF24),
    statusOffline = Color(0xFFF87171)
)

val LocalNexMeshPalette = staticCompositionLocalOf { NexMeshLightPalette }

// Compatibility aliases for existing canvas components
object NexMeshColors {
    val BgWarmIvory = Color(0xFFF6F7FB)
    val BgWhisperLavender = Color(0xFFEEF1FA)
    val BgPeriwinkleMist = Color(0xFFE1E9F6)
    val GlassSurface = Color(0xC8FFFFFF)
    val GlassSurfaceSoft = Color(0x96FFFFFF)
    val GlassSpecularTop = Color(0xF5FFFFFF)
    val GlassBorderCool = Color(0x2864748B)
    val CyanLensStart = Color(0xDDE4ECFF)
    val CyanLensEnd = Color(0xCCCDDDFD)
    val MintPillStart = Color(0xD8E3F8F2)
    val MintPillEnd = Color(0xC2C8EFE4)
    val MintTrackActive = Color(0xFF2FA88F)
    val LavenderCtaStart = Color(0xDDE4ECFF)
    val LavenderCtaEnd = Color(0xCCCDDDFD)
    val SkyTrackFillStart = Color(0xCC7FB2FA)
    val SkyTrackFillEnd = Color(0xCCA4C4FD)
    val InkPrimary = Color(0xFF101828)
    val InkSecondary = Color(0xFF526079)
    val InkAccentIndigo = Color(0xFF1E3A8A)
    val MeshNodePrimary = Color(0xFF3B6EF5)
    val MeshNodePulse = Color(0x405B86FA)
    val MeshLinkLine = Color(0x303B6EF5)
    val StatusHealthy = Color(0xFF129A74)
    val StatusWarning = Color(0xFFD97706)
    val StatusCritical = Color(0xFFDC2626)
}
