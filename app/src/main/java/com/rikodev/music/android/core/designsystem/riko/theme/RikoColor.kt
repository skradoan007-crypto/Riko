package com.rikodev.music.android.core.designsystem.riko.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — EMERALD LIQUID GLASS DESIGN SYSTEM
// All legacy token names preserved for 100% compile compatibility across screens.
// ─────────────────────────────────────────────────────────────────────────────

// Canvas — Obsidian-Emerald dark fallback stops
val CanvasTop = Color(0xFF041010)
val CanvasMid = Color(0xFF061414)
val CanvasLowMid = Color(0xFF071A1A)
val CanvasBottom = Color(0xFF030B0B)

val RikoCanvasGradient = Brush.verticalGradient(
    0f to CanvasTop,
    0.30f to CanvasMid,
    0.60f to CanvasLowMid,
    1f to CanvasBottom
)

/** Ambient glow blob color — emerald jade. */
val RikoGlowBlob = Color(0xFF10B981)
val RikoShadowBlob = Color.Black.copy(alpha = 0.35f)

val RikoBackground = CanvasMid
val RikoBackgroundElevated = Color(0xFF0B2220)

// Primary Brand Gradient — Vivid Emerald -> Seafoam Mint
val AccentViolet = Color(0xFF10B981)   // stop 0% (Emerald Primary)
val AccentMagenta = Color(0xFF34D399)  // stop 100% (Emerald Bright)

val RikoAuroraColors = listOf(AccentViolet, AccentMagenta)

fun rikoAuroraBrush(horizontal: Boolean = true): Brush =
    if (horizontal) Brush.horizontalGradient(RikoAuroraColors)
    else Brush.horizontalGradient(listOf(AccentMagenta, AccentViolet))

// Legacy accent names repointed to Emerald
val RikoPurple = AccentViolet
val RikoPurpleBright = AccentMagenta
val RikoPurpleDeep = Color(0xFF059669)
val RikoBlue = Color(0xFF2DD4BF)
val RikoBlueSoft = Color(0xFF6EE7B7)

// Text Hierarchy (Mint-White for Dark; Light components read MaterialTheme.colorScheme)
val RikoTextPrimary = Color(0xFFECFDF5)
val RikoTextSecondary = Color(0xFFECFDF5).copy(alpha = 0.70f)
val RikoTextTertiary = Color(0xFFA7F3D0).copy(alpha = 0.58f)
val RikoTextDisabled = Color(0xFFA7F3D0).copy(alpha = 0.28f)
val RikoTextOnAccent = Color.White

// Glass — Translucent Obsidian-Emerald Frosted Glass
val RikoGlass = Color(0x990B2220)
val RikoGlassStrong = Color(0xBA0F2B28)
val RikoGlassBorder = Color(0x3834D399)
val RikoGlassBorderActive = Color(0x736EE7B7)

// Surfaces
val RikoSurface = Color(0x990B2220)
val RikoSurfaceElevated = Color(0xBA0F2B28)
val RikoSurfaceStrong = Color(0xCC133531)

// Divider
val RikoDivider = Color(0x246EE7B7)

// Bottom Navigation fallback tokens
val RikoNavCapsule = Color(0x99071A1A)
val RikoNavCapsuleBorder = Color(0x3834D399)
val RikoNavCapsuleActive = AccentViolet

// Semantic / Status
val RikoSuccess = Color(0xFF34D399)
val RikoError = Color(0xFFFF6B6B)
val RikoWarning = Color(0xFFFBBF24)

val RikoFavoriteActive = Color(0xFF34D399)
val RikoFavoriteInactive = Color(0xFFECFDF5).copy(alpha = 0.62f)

// Toggle
val RikoToggleTrackOff = Color(0x336EE7B7)
val RikoToggleTrackOn = AccentViolet
val RikoToggleThumb = Color.White

// Settings banner scrim
val RikoBannerScrim = Color(0xFF0D9488)

// Accent Gradients
val RikoPrimaryGradient = RikoAuroraColors
val RikoAmbientGradient = listOf(RikoGlowBlob, CanvasMid)
val RikoPurpleBlueGradient = listOf(Color(0xFF059669), Color(0xFF2DD4BF))
val RikoActiveToggle = AccentViolet

const val AMBIENT_GLOW_MIN_ALPHA = 0.10f
const val AMBIENT_GLOW_MAX_ALPHA = 0.14f
const val AMBIENT_GLOW_BLUR_DP = 120