package com.rikodev.music.android.core.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// RIKO GLASS ENGINE V2 — INTENT-BASED GLASS TOKENS
//
// Design philosophy:
//   ❌ GlassTokens.BlurLarge    ← value-based, no context
//   ✅ GlassTokens.MiniPlayer  ← intent-based, designer communication
//
// Each token is a complete GLASS STYLE for a specific UI context.
// Values are derived from the Step 2.6 Glass Audit Report — not arbitrary.
//
// Future usage:
//   GlassSurface(style = GlassTokens.MiniPlayer)
//
// Each style encapsulates:
//   Blur         — backdrop blur radius (0.dp = no blur, V2 will add real blur)
//   Tint Alpha   — color overlay intensity (how much the accent/base tints the glass)
//   Border Alpha — edge visibility (glass border presence)
//   Glow Alpha   — outer light emission (ambient glow around the glass)
//   Shadow Alpha — depth shadow intensity (elevation-based grounding)
//
// ⭐ Rule: We design for UI INTENT, not raw values.
//   Apple, Google, Material 3, Fluent — all modern design systems
//   have moved to intent-based tokens. Riko follows the same path.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Complete glass style definition.
 *
 * Holds every optical property a glass surface needs — no piecing together
 * separate blur/tint/shadow tokens at each call site. Pass one object, get
 * the full glass treatment.
 */
data class GlassStyle(
    /** Backdrop blur radius. 0.dp means no blur (gradient-only glass). */
    val blur: Dp,
    /** Color tint overlay intensity applied on top of the frosted base. */
    val tintAlpha: Float,
    /** Edge border visibility — how pronounced the glass edge is. */
    val borderAlpha: Float,
    /** Ambient glow emission — soft light bleeding outward from the glass. */
    val glowAlpha: Float,
    /** Depth shadow — elevation-based grounding that anchors the glass. */
    val shadowAlpha: Float
)

/**
 * Central glass token registry.
 *
 * Every token represents a real UI context in Riko Music.
 * Values are derived from the Glass Audit Report (Step 2.6):
 *   - tintAlpha values match actual `.copy(alpha = ...)` usage per component
 *   - shadowAlpha maps from GlassIntensity elevation scale
 *   - borderAlpha derived from GlassBorderLight visibility
 *   - glowAlpha extracted from colorGlow / radialGradient patterns
 *
 * Do NOT add a token unless a real UI component will consume it.
 * Do NOT split a token into sub-values at call sites — use the whole style.
 */
object GlassTokens {

    // ── MiniPlayer ──────────────────────────────────────────────────────────
    // Always-visible floating bar at screen bottom. SOLID glass — needs to
    // feel substantial but transparent enough to show content scrolling beneath.
    // Audit: accentColor.copy(alpha = 0.10f), GlassIntensity.SOLID
    val MiniPlayer = GlassStyle(
        blur = 0.dp,          // V2: will add backdrop blur
        tintAlpha = 0.10f,
        borderAlpha = 0.08f,
        glowAlpha = 0.00f,
        shadowAlpha = 0.20f
    )

    // ── BottomBar ───────────────────────────────────────────────────────────
    // Navigation bar docked at screen bottom. Dark-tinted SOLID glass —
    // needs to feel grounded and separated from content above.
    // Audit: Color.Black.copy(alpha = 0.25f), GlassIntensity.SOLID
    val BottomBar = GlassStyle(
        blur = 0.dp,
        tintAlpha = 0.25f,
        borderAlpha = 0.12f,
        glowAlpha = 0.00f,
        shadowAlpha = 0.20f
    )

    // ── Dialog ──────────────────────────────────────────────────────────────
    // Confirmation / input dialogs. SOLID glass with highest prominence —
    // demands attention while maintaining glass aesthetic.
    // Audit: accentColor.copy(alpha = 0.06f), GlassIntensity.SOLID, cornerRadius 26.dp
    val Dialog = GlassStyle(
        blur = 0.dp,
        tintAlpha = 0.06f,
        borderAlpha = 0.08f,
        glowAlpha = 0.00f,
        shadowAlpha = 0.24f
    )

    // ── FloatingCard ───────────────────────────────────────────────────────
    // Standalone cards that float above content. MEDIUM glass — substantial
    // enough to read as a distinct surface, light enough for layering.
    // Audit: accentColor.copy(alpha = 0.08f) on active items, GlassIntensity.MEDIUM
    val FloatingCard = GlassStyle(
        blur = 0.dp,
        tintAlpha = 0.08f,
        borderAlpha = 0.06f,
        glowAlpha = 0.10f,
        shadowAlpha = 0.12f
    )

    // ── Hero ──────────────────────────────────────────────────────────────
    // Full-screen backdrop behind Now Playing. The most visually aggressive
    // glass — real optical blur of album art at 65dp, half-opacity.
    // Audit: .blur(65.dp), .alpha(0.5f), no border, no shadow
    val Hero = GlassStyle(
        blur = 65.dp,
        tintAlpha = 0.00f,
        borderAlpha = 0.00f,
        glowAlpha = 0.50f,
        shadowAlpha = 0.00f
    )

    // ── Overlay ────────────────────────────────────────────────────────────
    // Bottom sheet scrim and dimming overlays. No blur, no border — pure
    // tinted transparency that separates layers visually.
    // Audit: Color(0xFF211A12).copy(alpha = 0.18f) from GlassBottomSheetContainer
    val Overlay = GlassStyle(
        blur = 0.dp,
        tintAlpha = 0.18f,
        borderAlpha = 0.00f,
        glowAlpha = 0.00f,
        shadowAlpha = 0.00f
    )

    // ── Chip ───────────────────────────────────────────────────────────────
    // Small selectable labels (EQ presets, filter tags). ULTRA_THIN glass —
    // minimal presence, just enough frosted surface to group content.
    // Audit: accentColor.copy(alpha = 0.05-0.22f), GlassIntensity.ULTRA_THIN
    val Chip = GlassStyle(
        blur = 0.dp,
        tintAlpha = 0.05f,
        borderAlpha = 0.08f,
        glowAlpha = 0.00f,
        shadowAlpha = 0.04f
    )

    // ── AlbumOverlay ───────────────────────────────────────────────────────
    // Gradient and glow layers composited on top of album art. No blur needed
    // since it sits directly on the image. Accent glow + specular highlight.
    // Audit: accentColor.copy(alpha = 0.25f), Color.White.copy(alpha = 0.30f)
    val AlbumOverlay = GlassStyle(
        blur = 0.dp,
        tintAlpha = 0.25f,
        borderAlpha = 0.00f,
        glowAlpha = 0.30f,
        shadowAlpha = 0.00f
    )

    // ── Tooltip ────────────────────────────────────────────────────────────
    // Small floating info popups. LIGHT glass — minimal but visible, with
    // subtle shadow for depth and faint glow for glass feel.
    val Tooltip = GlassStyle(
        blur = 0.dp,
        tintAlpha = 0.10f,
        borderAlpha = 0.08f,
        glowAlpha = 0.08f,
        shadowAlpha = 0.08f
    )
}
