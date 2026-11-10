package com.rikodev.music.android.core.designsystem.riko.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — SOUND MOVES DESIGN SYSTEM (SM-UI-SPEC-001 v2.0, Section 1.2)
//
// SOURCE OF TRUTH: docs/Plans/SoundMoves_UI_Enterprise_Workflow_v2.txt
// All values are programmatically sampled from the 3 reference images.
//
// COLOR DISTRIBUTION:
//   Layered indigo canvas (4-stop gradient + violet glow blobs) ·
//   white glass surfaces (10% fill / 14% border) · Aurora accent gradient
//   (violet→magenta) on every primary/active element · white text hierarchy.
//
// RULE: Every primary/active/selected element uses the Aurora gradient
//       (accent-violet #8A2BF0 → accent-magenta #C92CEE). Flat violet
//       (#8A2BF0) is the flat fallback where a Brush is not wired yet.
// RULE: Never hardcode Color(0xFF...) in UI — always use these tokens.
// ─────────────────────────────────────────────────────────────────────────────

// ── Canvas — App background (Section 1.2-A, layered) ─────────────────────────
// Layer 1 base vertical gradient stops:
val CanvasTop = Color(0xFF0A0A52)      // 0%  deep indigo-navy
val CanvasMid = Color(0xFF060750)      // 30% indigo
val CanvasLowMid = Color(0xFF04041D)   // 60% near-black navy
val CanvasBottom = Color(0xFF020210)   // 100% almost pure black

/** Full App Canvas: 4-stop vertical gradient (Layer 1). */
val RikoCanvasGradient = Brush.verticalGradient(
    0f to CanvasTop,
    0.30f to CanvasMid,
    0.60f to CanvasLowMid,
    1f to CanvasBottom
)

/** Ambient glow blob color (Layer 2) — violet, 10–14% opacity, heavy blur. */
val RikoGlowBlob = Color(0xFF6C3CE9)

/** Shadow blob (Layer 3) for list-heavy screens. */
val RikoShadowBlob = Color.Black.copy(alpha = 0.35f)

// Flat fallbacks kept for legacy call-sites (to be migrated to gradient).
val RikoBackground = CanvasLowMid
val RikoBackgroundElevated = CanvasMid

// ── Aurora — Primary Brand Gradient (Section 1.2-B) ──────────────────────────
val AccentViolet = Color(0xFF8A2BF0)   // stop 0%
val AccentMagenta = Color(0xFFC92CEE)  // stop 100%

/** Aurora gradient: 90° (left→right) default; mirrored instances use reversed. */
val RikoAuroraColors = listOf(AccentViolet, AccentMagenta)

fun rikoAuroraBrush(horizontal: Boolean = true): Brush =
    if (horizontal) Brush.horizontalGradient(RikoAuroraColors)
    else Brush.horizontalGradient(listOf(AccentMagenta, AccentViolet))

// ── Legacy accent names (kept for call-site compatibility, repointed) ────────
/** Flat accent — aurora violet (gradient upgrade happens per-component). */
val RikoPurple = AccentViolet
val RikoPurpleBright = AccentMagenta
val RikoPurpleDeep = AccentViolet
val RikoBlue = AccentViolet
val RikoBlueSoft = AccentMagenta

// ── Text Hierarchy (Section 1.2-D) ───────────────────────────────────────────
val RikoTextPrimary = Color.White                          // 100%
val RikoTextSecondary = Color.White.copy(alpha = 0.60f)    // 60%
val RikoTextTertiary = Color.White.copy(alpha = 0.52f)     // 52% — WCAG AA for body text
val RikoTextDisabled = Color.White.copy(alpha = 0.24f)
val RikoTextOnAccent = Color.White

// ── Glass (Section 1.2-C) — WHITE over canvas, not dark fill ─────────────────
val RikoGlass = Color.White.copy(alpha = 0.10f)
val RikoGlassStrong = Color.White.copy(alpha = 0.16f)
val RikoGlassBorder = Color.White.copy(alpha = 0.14f)
val RikoGlassBorderActive = Color.White.copy(alpha = 0.30f)

// ── Surfaces (glass-backed containers; composited over canvas) ───────────────
val RikoSurface = Color.White.copy(alpha = 0.10f)
val RikoSurfaceElevated = Color.White.copy(alpha = 0.16f)
val RikoSurfaceStrong = Color.White.copy(alpha = 0.22f)

// ── Divider (Section 1.2-E) ──────────────────────────────────────────────────
val RikoDivider = Color.White.copy(alpha = 0.08f)

// ── Bottom Navigation (Section 2.6) ──────────────────────────────────────────
// Floating glass pill: white 10% fill, white 14% border, radius-full.
// Active item: 52dp circle filled with the Aurora gradient.
val RikoNavCapsule = Color.White.copy(alpha = 0.10f)
val RikoNavCapsuleBorder = Color.White.copy(alpha = 0.14f)
val RikoNavCapsuleActive = AccentViolet

// ── Semantic / Status ────────────────────────────────────────────────────────
val RikoSuccess = Color(0xFF34D399)
val RikoError = Color(0xFFFF6B6B)          // soft coral — destructive rows (D11)
val RikoWarning = Color(0xFFFBBF24)

// Favorite-active follows the accent rule (aurora violet).
val RikoFavoriteActive = AccentViolet
val RikoFavoriteInactive = Color.White.copy(alpha = 0.60f)

// ── Toggle (Section 2.16) ────────────────────────────────────────────────────
// ON track: Aurora gradient (flat fallback = violet). OFF: white 14%.
val RikoToggleTrackOff = Color.White.copy(alpha = 0.14f)
val RikoToggleTrackOn = AccentViolet
val RikoToggleThumb = Color.White

// ── Settings promo banner scrim (Section 2.17) ───────────────────────────────
val RikoBannerScrim = Color(0xFF1B4FBF)

// ── Accent Gradients (legacy names, repointed to Aurora) ─────────────────────
/** Primary CTA / selected control: aurora violet → magenta. */
val RikoPrimaryGradient = RikoAuroraColors

/** Full-screen ambient backdrop glow layers. */
val RikoAmbientGradient = listOf(RikoGlowBlob, CanvasMid)

/** Purple → magenta transition for hero accents. */
val RikoPurpleBlueGradient = RikoAuroraColors

// ── Active Toggle / CTA Fill ─────────────────────────────────────────────────
val RikoActiveToggle = AccentViolet

// ── Ambient Glow Limits (enforcement constants) ──────────────────────────────
// Spec 1.2-A Layer 2: violet at 10–14% opacity, blur ~120dp.
const val AMBIENT_GLOW_MIN_ALPHA = 0.10f
const val AMBIENT_GLOW_MAX_ALPHA = 0.14f
const val AMBIENT_GLOW_BLUR_DP = 120