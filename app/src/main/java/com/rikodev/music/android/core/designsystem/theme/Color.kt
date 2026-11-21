package com.rikodev.music.android.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — EMERALD DARK & EMERALD LIGHT GLASS COLOR SYSTEM
// Every token keeps its original name so existing files keep compiling cleanly.
// ─────────────────────────────────────────────────────────────────────────────

// Emerald Dark Backgrounds & Surfaces
val ColorAbyssBlack = Color(0xFF040E0E)      // deepest obsidian-emerald base
val ColorVoidDark = Color(0xFF061414)        // main dark background
val ColorDepthDark = Color(0xFF0B2220)       // elevated dark emerald surface
val ColorSurfaceDark = Color(0xFF0F2B28)     // card/sheet dark base
val ColorObjectMat = Color(0xFF0A1D1C)       // album art mat / backing plate

// Emerald Light Backgrounds, Surfaces & Text
val EmeraldLightBg = Color(0xFFE8FAF4)
val EmeraldLightSurface = Color(0xFFF0FDF9)
val EmeraldLightGlass = Color(0xB8E8FAF4)
val EmeraldLightGlassStrong = Color(0xD9FFFFFF)
val EmeraldLightGlassBorder = Color(0x400D9488)
val EmeraldLightPrimary = Color(0xFF0D9488)
val EmeraldLightPrimaryDeep = Color(0xFF059669)
val EmeraldLightSecondary = Color(0xFF14B8A6)
val EmeraldLightTextPrimary = Color(0xFF062626)
val EmeraldLightTextSecondary = Color(0xFF1F4A4A)

// Glass surface colors (Emerald-tinted frosted glass)
val GlassWhite04 = Color(0x140B2220)
val GlassWhite08 = Color(0x2410B981)
val GlassWhite12 = Color(0x330B2220)
val GlassWhite18 = Color(0x4010B981)
val GlassWhite25 = Color(0x5234D399)
val GlassWhite40 = Color(0x666EE7B7)

// Glass border colors
val GlassBorderSubtle = Color(0x226EE7B7)
val GlassBorderLight = Color(0x3834D399)
val GlassBorderStrong = Color(0x596EE7B7)

// Glass specular highlight
val GlassSpecular = Color(0xE6ECFDF5)
val GlassSpecularSoft = Color(0x99A7F3D0)

// Primary Accent — Vivid Emerald (#10B981 / #34D399)
val AccentCyan = Color(0xFF10B981)
val AccentCyanDim = Color(0xFF059669)
val AccentCyanGlow = Color(0x3310B981)

// Secondary Accent — Seafoam Emerald (#34D399 / #6EE7B7)
val AccentViolet = Color(0xFF34D399)
val AccentVioletDim = Color(0xFF0D9488)
val AccentVioletGlow = Color(0x3334D399)

// Semantic badge colors
val AccentGreen = Color(0xFF34D399)
val AccentGreenGlow = Color(0x3334D399)
val AccentOrange = Color(0xFFF59E0B)
val AccentOrangeGlow = Color(0x33F59E0B)
val AccentPink = Color(0xFF2DD4BF)
val AccentPinkGlow = Color(0x332DD4BF)

// Gradient Stops — deep obsidian-emerald ambient
val GradientTop = Color(0xFF051313)
val GradientMid = Color(0xFF071C1B)
val GradientBottom = Color(0xFF0B2523)

// Legacy Aurora token names repointed to Emerald Obsidian-Jade palette
val AuroraTop = Color(0xFF040E0E)
val AuroraIndigo = Color(0xFF071F1E)
val AuroraIndigoDeep = Color(0xFF051616)
val AuroraBase = Color(0xFF040E0E)
val AuroraPurple = Color(0xFF0D9488)
val AuroraVioletDeep = Color(0xFF059669)
val AuroraViolet = Color(0xFF10B981)

// Sub-Screen Sheet Accents — Unified Emerald & Seafoam
val AccentTeal = Color(0xFF34D399)
val EqCurveBlue = Color(0xFF2DD4BF)
val EqCurveViolet = Color(0xFF10B981)
val EqCardGlow = Color(0xFF34D399)
val GradientPinkVioletStart = Color(0xFF059669)
val GradientPinkVioletEnd = Color(0xFF34D399)
val RingTimerStart = Color(0xFF2DD4BF)
val RingTimerEnd = Color(0xFF10B981)
val SongInfoCardTop = Color(0xFF0B2B28)
val SongInfoCardBottom = Color(0xFF061B1A)
val InfoTileTop = Color(0xFF10B981)
val InfoTileBottom = Color(0xFF0D9488)

// Text colors (crisp mint-white on dark emerald)
val TextPrimary = Color(0xFFECFDF5)
val TextSecondary = Color(0xB8ECFDF5)
val TextTertiary = Color(0x85A7F3D0)
val TextDisabled = Color(0x4DA7F3D0)

// Waveform / Visualizer Colors
val WaveformPrimary = AccentCyan
val WaveformSecondary = AccentViolet
val WaveformPlayed = AccentCyan
val WaveformUnplayed = Color(0x3310B981)

// Equalizer Bar Colors
val EqBandColor1 = Color(0xFF059669)
val EqBandColor2 = Color(0xFF10B981)
val EqBandColor3 = Color(0xFF34D399)
val EqBandColor4 = Color(0xFF2DD4BF)
val EqBandColor5 = Color(0xFF6EE7B7)

// Status Colors
val ColorFavoriteActive = Color(0xFF34D399)
val ColorFavoriteInactive = Color(0x66ECFDF5)
val ColorSuccess = Color(0xFF34D399)
val ColorError = Color(0xFFFF5A6A)
val ColorWarning = Color(0xFFFBBF24)

// Dynamic Colors (fallback defaults)
val DynamicColorDefault = Color(0xFF061414)
val DynamicAccentDefault = AccentCyan