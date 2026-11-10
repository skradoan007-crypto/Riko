package com.rikodev.music.android.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — DARK GLASS COLOR SYSTEM
// Dark-only foundation for the whole product.
// Every token keeps its original name so existing files keep compiling.
// ─────────────────────────────────────────────────────────────────────────────

// Backgrounds
val ColorAbyssBlack = Color(0xFF05070B)     // deepest base
val ColorVoidDark = Color(0xFF0A0D14)        // main app background
val ColorDepthDark = Color(0xFF111623)       // elevated surface substrate
val ColorSurfaceDark = Color(0xFF151B2A)     // card/sheet base
val ColorObjectMat = Color(0xFF121726)       // album art mat / backing plate

// Glass surface colors
val GlassWhite04 = Color(0x0AFFFFFF)
val GlassWhite08 = Color(0x14FFFFFF)
val GlassWhite12 = Color(0x1FFFFFFF)
val GlassWhite18 = Color(0x2EFFFFFF)
val GlassWhite25 = Color(0x40FFFFFF)
val GlassWhite40 = Color(0x66FFFFFF)

// Glass border colors
val GlassBorderSubtle = Color(0x14FFFFFF)
val GlassBorderLight = Color(0x22FFFFFF)
val GlassBorderStrong = Color(0x38FFFFFF)

// Glass specular highlight
val GlassSpecular = Color(0xE6FFFFFF)
val GlassSpecularSoft = Color(0x99FFFFFF)

// Primary Accent — Electric Cyan
val AccentCyan = Color(0xFF19B7FF)
val AccentCyanDim = Color(0xFF0C8ABF)
val AccentCyanGlow = Color(0x3319B7FF)

// Secondary Accent — Violet Glass
val AccentViolet = Color(0xFF9A6CFF)
val AccentVioletDim = Color(0xFF6E45D6)
val AccentVioletGlow = Color(0x339A6CFF)

// Additional semantic badge colors — Settings icon badges need more variety
// than the core Cyan/Violet accent pair (Battery Saver=green, Light=orange,
// etc., matching the reference design's per-item semantic coloring).
val AccentGreen = Color(0xFF3DDC84)
val AccentGreenGlow = Color(0x333DDC84)
val AccentOrange = Color(0xFFFF9142)
val AccentOrangeGlow = Color(0x33FF9142)
val AccentPink = Color(0xFFFF5C9E)
val AccentPinkGlow = Color(0x33FF5C9E)

// Gradient Stops — deep ambient
val GradientTop = Color(0xFF09111D)
val GradientMid = Color(0xFF0D1524)
val GradientBottom = Color(0xFF131A2B)

// ── Aurora Violet Identity (sampled from the UI target mockups) ──────────
// The reference design reads as a deep indigo → bright violet gradient.
// These tokens are the sampled anchors for that look:
//   Now Playing top   #050219 · library top #0C0B56 · library base #050213
//   mid purples       #503080            · bright violet #A020F0
val AuroraTop = Color(0xFF050219)         // Now Playing screen top (near-black indigo)
val AuroraIndigo = Color(0xFF0C0B56)      // library screen top (deep indigo)
val AuroraIndigoDeep = Color(0xFF05052F)  // library mid stop
val AuroraBase = Color(0xFF050213)        // library bottom / base substrate
val AuroraPurple = Color(0xFF503080)      // mid purple
val AuroraVioletDeep = Color(0xFF6A22C4)  // readability-safe violet stop
val AuroraViolet = Color(0xFFA020F0)      // bright violet accent / bottom glow

// ── Sub-Screen Sheet Accents (sampled from UI target/Tab Target v2) ──────
// Equalizer: mint-teal gains + fill over a blue-violet glowing curve.
// Toggles / Save preset / selected timer chip: violet→pink gradient pair.
// Sleep-timer countdown ring: sky-blue → green sweep. Song-info card:
// indigo-violet top fading into deep teal.
val AccentTeal = Color(0xFF3BE8B0)          // EQ gain labels / area fill
val EqCurveBlue = Color(0xFF8FA5FF)         // EQ curve line start (periwinkle)
val EqCurveViolet = Color(0xFFA97BFF)       // EQ curve line end
val EqCardGlow = Color(0xFF2EE6A8)          // EQ master-card ambient glow
val GradientPinkVioletStart = Color(0xFFB76BFF)  // toggles / save / selected chip
val GradientPinkVioletEnd = Color(0xFFFF5C9E)
val RingTimerStart = Color(0xFF4FA8FF)      // sleep-timer ring sweep start
val RingTimerEnd = Color(0xFF3DDC84)        // sleep-timer ring sweep end
val SongInfoCardTop = Color(0xFF2B2F6E)     // song-info card gradient (violet-blue)
val SongInfoCardBottom = Color(0xFF175247)  // → deep teal
val InfoTileTop = Color(0xFF4FA8FF)         // song-info header tile gradient
val InfoTileBottom = Color(0xFF2B4BD8)

// Text colors
val TextPrimary = Color(0xFFF4F7FB)
val TextSecondary = Color(0xB3F4F7FB)
val TextTertiary = Color(0x80F4F7FB)
val TextDisabled = Color(0x4DF4F7FB)

// Waveform / Visualizer Colors
val WaveformPrimary = AccentCyan
val WaveformSecondary = AccentViolet
val WaveformPlayed = AccentCyan
val WaveformUnplayed = Color(0x3319B7FF)

// Equalizer Bar Colors
val EqBandColor1 = AccentCyan
val EqBandColor2 = Color(0xFF2FD4FF)
val EqBandColor3 = AccentViolet
val EqBandColor4 = Color(0xFFB48CFF)
val EqBandColor5 = Color(0xFFFF6FD8)

// Status Colors
val ColorFavoriteActive = Color(0xFFFF4D7D)
val ColorFavoriteInactive = Color(0x66F4F7FB)
val ColorSuccess = Color(0xFF34D399)
val ColorError = Color(0xFFFF5A6A)
val ColorWarning = Color(0xFFFBBF24)

// Dynamic Colors (set at runtime by Palette API)
val DynamicColorDefault = Color(0xFF0F1729)
val DynamicAccentDefault = AccentCyan