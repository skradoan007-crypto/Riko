package com.rikodev.music.android.presentation.miniplayer

import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.animation.core.Animatable

// ═══════════════════════════════════════════════════════════════════════════════════════
// RIKO MUSIC — MINI PLAYER DESIGN TOKENS
//
// Single source of truth for ALL visual values in Mini Player.
// If Glass needs to be more premium — change ONE file.
// If spacing needs adjustment — change ONE file.
//
// ⭐ DESIGN PRINCIPLE ⭐
//   Intent-based tokens, not raw values.
//   Each token describes WHAT it controls, not WHERE it's used.
//   Components reference tokens by name, never by value.
//
// ⭐ RULE #21 COMPLIANCE ⭐
//   Every value here is traceable to its origin.
//   No magic numbers. No unexplained constants.
//
// ⭐ OVER-ENGINEERING GUARD ⭐
//   Every token here must answer:
//   "Will the user FEEL the difference if this changes?"
//   If the answer is "no" — remove it.
// ═══════════════════════════════════════════════════════════════════════════════════════

object MiniPlayerTokens {

    // ═════════════════════════════════════════════════════════════════════
    // SECTION 1 — SPACING
    // ═════════════════════════════════════════════════════════════════════

    // ── External Spacing (surface to screen edge) ────────────────────────
    /** Horizontal padding between mini player and screen edge. */
    val SurfaceHorizontalPadding = 12.dp

    /** Bottom margin above navigation bar. */
    val SurfaceBottomMargin = 8.dp

    /** Extra bottom padding for gesture navigation mode. */
    val GestureNavExtraPadding = 8.dp

    // ── Internal Spacing (within surface) ─────────────────────────────────
    /** Horizontal content padding inside surface. */
    val ContentPaddingHorizontal = 10.dp

    /** Gap between album art and metadata text column. */
    val AlbumMetadataGap = 12.dp

    /** Gap between control buttons. */
    val ControlsGap = 8.dp

    /** Gap between metadata column and controls row. */
    val MetadataControlsGap = 8.dp

    /** Bottom padding below progress strip. */
    val ProgressBottomPadding = 8.dp

    // ── Layout Margins ────────────────────────────────────────────────────
    /** Minimum distance from screen bottom edge. */
    val MinBottomMargin = 8.dp

    // ═════════════════════════════════════════════════════════════════════
    // SECTION 2 — DIMENSIONS
    // ═════════════════════════════════════════════════════════════════════

    // ── Surface ──────────────────────────────────────────────────────────
    /** Collapsed surface height. */
    val SurfaceHeightCollapsed = 80.dp

    /** Expanded surface height. */
    val SurfaceHeightExpanded = 240.dp

    /** Minimum surface width (never narrower regardless of screen). */
    val SurfaceMinWidth = 280.dp

    /** Collapsed width as fraction of screen. */
    const val SurfaceWidthFractionCollapsed = 0.94f

    /** Expanded width as fraction of screen. */
    const val SurfaceWidthFractionExpanded = 1.0f

    // ── Album Art ────────────────────────────────────────────────────────
    /** Collapsed album art size. */
    val AlbumArtSizeCollapsed = 52.dp

    /** Expanded album art size. */
    val AlbumArtSizeExpanded = 180.dp

    /** Album art corner radius (collapsed). */
    val AlbumCornerRadiusCollapsed = 8.dp

    /** Album art corner radius (expanded). */
    val AlbumCornerRadiusExpanded = 16.dp

    /** Expanded album height on tablets. */
    val AlbumExpandedHeightTablet = 280.dp

    // ── Controls ──────────────────────────────────────────────────────────
    /** Play/Pause button size. */
    val PlayPauseButtonSize = 38.dp

    /** Skip Next/Previous button size. */
    val SkipButtonSize = 38.dp

    /** Icon size inside buttons. */
    val IconSize = 20.dp

    // ── Progress ──────────────────────────────────────────────────────────
    /** Waveform strip height. */
    val ProgressStripHeight = 4.dp

    /** Waveform strip horizontal padding. */
    val ProgressStripHorizontalPadding = 14.dp

    /** Number of waveform bars in progress strip. */
    const val ProgressBarCount = 40

    // ═════════════════════════════════════════════════════════════════════
    // SECTION 3 — CORNER RADIUS
    // ═════════════════════════════════════════════════════════════════════

    /** Surface corner radius (collapsed — rounded pill). */
    val SurfaceCornerRadiusCollapsed = 28.dp

    /** Surface corner radius (expanded — full-width rectangle). */
    val SurfaceCornerRadiusExpanded = 0.dp

    // ═════════════════════════════════════════════════════════════════════
    // SECTION 4 — ELEVATION
    // ═════════════════════════════════════════════════════════════════════

    /** Shadow elevation (collapsed — resting on surface). */
    val ShadowElevationCollapsed = 8.dp

    /** Shadow elevation (expanded — lifting off screen). */
    val ShadowElevationExpanded = 24.dp

    /** Energy modulation factor (shadow grows this % when energy > 0.5). */
    const val EnergyElevationBoost = 0.10f

    // ═════════════════════════════════════════════════════════════════════
    // SECTION 5 — GLASS ALPHA
    // ═════════════════════════════════════════════════════════════════════

    /** Glass tint alpha (collapsed, from GlassTokens.MiniPlayer). */
    const val GlassTintAlpha = 0.10f

    /** Glass shadow alpha (collapsed, from GlassTokens.MiniPlayer). */
    const val GlassShadowAlpha = 0.20f

    /** Glass border alpha (collapsed). */
    const val GlassBorderAlpha = 0.08f

    /** Glass border width. */
    val GlassBorderWidth = 0.8.dp

    /** Hover tint alpha boost. */
    const val HoverTintAlphaBoost = 0.02f

    // ═════════════════════════════════════════════════════════════════════
    // SECTION 6 — SHADOW ALPHA
    // ═════════════════════════════════════════════════════════════════════

    /** Shadow base color (warm anchor). */
    const val ShadowBaseColor = 0xFF211A12

    /** Shadow ambient alpha factor. */
    const val ShadowAmbientFactor = 0.7f

    // ═════════════════════════════════════════════════════════════════════
    // SECTION 7 — GLOW
    // ═════════════════════════════════════════════════════════════════════

    /** Glow radius (collapsed — no glow). */
    val GlowRadiusCollapsed = 0.dp

    /** Glow radius (expanded — maximum halo). */
    val GlowRadiusExpanded = 40.dp

    /** Glow morph threshold — glow only appears above this progress. */
    const val GlowMorphThreshold = 0.50f

    /** Glow intensity collapsed. */
    const val GlowIntensityCollapsed = 0f

    /** Glow intensity expanded. */
    const val GlowIntensityExpanded = 1f

    /** Glow color radius for colorGlow modifier. */
    val GlowColorRadius = 24.dp

    /** Glow default alpha for colorGlow modifier. */
    const val GlowDefaultAlpha = 0.35f

    // ═════════════════════════════════════════════════════════════════════
    // SECTION 8 — REFLECTION
    // ═════════════════════════════════════════════════════════════════════

    /** Reflection morph threshold — appears above this progress. */
    const val ReflectionMorphThreshold = 0.70f

    /** Maximum reflection alpha. */
    const val ReflectionAlphaMax = 0.25f

    /** Reflection gradient angle (diagonal). */
    const val ReflectionGradientAngle = 135f

    // ═════════════════════════════════════════════════════════════════════
    // SECTION 9 — NOISE
    // ═════════════════════════════════════════════════════════════════════

    /** Noise texture alpha — barely visible, breaks gradient banding. */
    const val NoiseAlpha = 0.03f

    /** Noise pattern tile size in pixels (square). */
    const val NoiseTileSize = 128

    // ═════════════════════════════════════════════════════════════════════
    // SECTION 10 — BEAT PULSE
    // ═════════════════════════════════════════════════════════════════════

    /** Album art scale peak on beat. */
    const val BeatAlbumScalePeak = 1.02f

    /** Beat pulse duration reference (actual spec from MiniPlayerMotion). */
    const val BeatPulseDurationMs = 200

    // ═════════════════════════════════════════════════════════════════════
    // SECTION 11 — ALBUM FLOAT
    // ═════════════════════════════════════════════════════════════════════

    /** Album art float amplitude (playing). */
    val AlbumFloatAmplitude = 3.dp

    /** Album art float amplitude (paused/idle — reduced). */
    val AlbumIdleFloatAmplitude = 1.dp

    /** Album art parallax offset at full expansion. */
    val AlbumParallaxMaxOffset = 5.dp

    // ═════════════════════════════════════════════════════════════════════
    // SECTION 12 — RIPPLE / PRESS
    // ═════════════════════════════════════════════════════════════════════

    /** Surface press scale (finger down on surface). */
    const val SurfacePressScale = 0.97f

    /** Button press scale (finger down on button). */
    const val ButtonPressScale = 0.92f

    /** Hover anticipation scale (pointer enters bounds). */
    const val HoverAnticipationScale = 1.01f

    /** Dismiss scale (shrinking away animation). */
    const val DismissScale = 0.95f

    // ═════════════════════════════════════════════════════════════════════
    // SECTION 13 — SAFE AREA
    // ═════════════════════════════════════════════════════════════════════

    /** Minimum distance from screen bottom edge. */
    val SafeAreaBottom = 8.dp

    /** Extra padding for gesture navigation. */
    val SafeAreaGestureNavExtra = 8.dp

    // ═════════════════════════════════════════════════════════════════════
    // SECTION 14 — CONTENT PADDING
    // ═════════════════════════════════════════════════════════════════════
    // (Duplicate reference for semantic clarity — values from Section 1)

    /** Same as ContentPaddingHorizontal. Semantic alias for readability. */
    val LayoutContentPadding = ContentPaddingHorizontal

    // ═════════════════════════════════════════════════════════════════════
    // SECTION 15 — LAYOUT MARGINS
    // ═════════════════════════════════════════════════════════════════════
    // (Values from Section 1 — duplicated here for discoverability)

    /** Same as SurfaceHorizontalPadding. */
    val LayoutMarginHorizontal = SurfaceHorizontalPadding

    /** Same as SurfaceBottomMargin. */
    val LayoutMarginBottom = SurfaceBottomMargin

    // ═════════════════════════════════════════════════════════════════════
    // SECTION 16 — Z LEVELS
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Z-level ordering for Mini Player layers.
     * Higher index = closer to user (rendered last).
     * Components MUST follow this order in composition.
     */
    object ZLevels {
        const val BACKGROUND = 0
        const val SHADOW = 1
        const val GLOW = 2
        const val SURFACE_BODY = 3
        const val NOISE = 4
        const val WAVE_RIBBON = 5
        const val ALBUM_ART = 6
        const val ALBUM_REFLECTION = 7
        const val METADATA = 8
        const val PROGRESS_STRIP = 9
        const val CONTROLS = 10
        const val PRESS_FEEDBACK = 11
        const val GESTURE_OVERLAY = 12
    }

    // ═════════════════════════════════════════════════════════════════════
    // SECTION 17 — RENDER PRIORITY
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Frame time budget per component (ms).
     * Total budget: 3.5ms (of 16ms @ 60Hz).
     * Components rendering beyond budget triggers Performance Gate.
     */
    object RenderPriority {
        const val BACKGROUND_MS = 0.5f
        const val SURFACE_MS = 0.3f
        const val SHADOW_MS = 0.1f
        const val NOISE_MS = 0.1f
        const val WAVE_MS = 0.8f
        const val ALBUM_MS = 1.0f
        const val METADATA_MS = 0.2f
        const val CONTROLS_MS = 0.3f
        const val PROGRESS_MS = 0.5f
        const val GESTURE_MS = 0.1f
        const val TOTAL_BUDGET_MS = 3.5f
    }

    // ═════════════════════════════════════════════════════════════════════
    // SECTION 18 — MINIMUM TOUCH TARGET
    // ═════════════════════════════════════════════════════════════════════

    /** Minimum touch target size (accessibility). */
    val MinTouchTargetSize = 48.dp

    /** Button touch target (slightly larger than visual size for accessibility). */
    val ButtonTouchTarget = 44.dp

    // ═════════════════════════════════════════════════════════════════════
    // SECTION 19 — ACCESSIBILITY SIZES
    // ═════════════════════════════════════════════════════════════════════

    /** Minimum tap target for play/pause (accessibility). */
    val PlayPauseTouchTarget = 48.dp

    /** Minimum tap target for skip buttons (accessibility). */
    val SkipTouchTarget = 44.dp

    /** Surface minimum tap target for expand gesture. */
    val SurfaceExpandTouchTarget = 48.dp

    // ═════════════════════════════════════════════════════════════════════
    // SECTION 20 — TYPOGRAPHY SCALE
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Typography references for Mini Player.
     * Uses existing theme tokens — Mini Player does NOT define its own fonts.
     *
     * Title: MaterialTheme.typography.headlineMedium
     * Artist: MaterialTheme.typography.bodyMedium
     * Color: TextPrimary (adapts to dark/light artwork)
     */
    object Typography {
        // No custom values — uses theme tokens.
        // Listed here for documentation completeness.
        const val TITLE_STYLE = "headlineMedium"
        const val ARTIST_STYLE = "bodyMedium"
        const val TITLE_MAX_LINES = 1
        const val ARTIST_MAX_LINES = 1
        const val TITLE_OVERFLOW = "Ellipsis"
        const val ARTIST_OVERFLOW = "Ellipsis"
    }

    // ═════════════════════════════════════════════════════════════════════
    // SECTION 21 — GESTURE THRESHOLDS
    // ═════════════════════════════════════════════════════════════════════

    /** Direction lock threshold (dp). Past this movement, gesture locks direction. */
    val DirectionLockThreshold = 16.dp

    /** Horizontal skip threshold (dp). Past this, drag triggers skip. */
    val HorizontalSkipThreshold = 120.dp

    /** Dismiss threshold (dp). Past this downward drag, mini player dismisses. */
    val DismissThreshold = 200.dp

    /** Expand fling velocity (dp/second). Past this upward velocity, morph commits. */
    const val ExpandFlingVelocity = 800f

    /** Dismiss fling velocity (dp/second). Past this downward velocity, dismiss commits. */
    const val DismissFlingVelocity = 600f

    /** Long press threshold (ms). Past this hold, queue peek triggers. */
    const val LongPressThresholdMs = 500

    // ═════════════════════════════════════════════════════════════════════
    // SECTION 22 — ADAPTIVE SIZING BREAKPOINTS
    // ═════════════════════════════════════════════════════════════════════

    /** Expanded height in landscape (fraction of screen height). */
    const val LandscapeExpandedHeightFraction = 0.6f

    /** Maximum expanded height in landscape on tablets. */
    const val LandscapeMaxHeightTabletDp = 320f

    // ═════════════════════════════════════════════════════════════════════
    // SECTION 23 — BACKGROUND
    // ═════════════════════════════════════════════════════════════════════

    /** Base ambient alpha (collapsed, idle). */
    const val BackgroundBaseAlphaCollapsed = 0.08f

    /** Base ambient alpha (expanded). */
    const val BackgroundBaseAlphaExpanded = 0.15f

    /** Energy modulation factor for background brightness. */
    const val BackgroundEnergyModulation = 0.12f

    /** Ambient breathing cycle (ms). Shared with NowPlaying drift. */
    const val BackgroundBreathingCycleMs = 14_000

    /** Pulse breathing cycle (ms). */
    const val BackgroundPulseCycleMs = 6_000

    // ═════════════════════════════════════════════════════════════════════
    // SECTION 24 — CROSSFADE
    // ═════════════════════════════════════════════════════════════════════

    /** Morph crossfade start (morphProgress). */
    const val CrossfadeStart = 0.85f

    /** Morph crossfade end (morphProgress). */
    const val CrossfadeEnd = 1.0f

    // ═════════════════════════════════════════════════════════════════════
    // SECTION 25 — FALLBACK
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Fallback values when Color Brain fails.
     * From MiniPlayerColorSnapshot.FALLBACK.
     * Rule #25 — Layer Independence: always renders, never crashes.
     */
    object Fallback {
        /** Fallback primary color. */
        const val PrimaryColor = 0xFF1A1A2E

        /** Fallback secondary color. */
        const val SecondaryColor = 0xFF16213E

        /** Fallback accent color. */
        const val AccentColor = 0xFFE94560

        /** Fallback background color (deep dark). */
        const val BackgroundColor = 0xFF09111D
    }
}
