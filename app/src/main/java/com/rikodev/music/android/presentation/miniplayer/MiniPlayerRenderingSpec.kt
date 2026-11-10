package com.rikodev.music.android.presentation.miniplayer
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMotion

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.FiniteAnimationSpec
import kotlin.math.PI

// ═══════════════════════════════════════════════════════════════════════════════════════
// RIKO MUSIC — MINI PLAYER RENDERING SPECIFICATION v1.0 (MPRS)
//
// This is NOT a contract. This is the renderer's CONSTITUTION.
//
// Architecture:
//
//   Experience OS
//        │
//        ▼
//   MiniPlayerState
//        │
//        ▼
//   Rendering Specification ⭐ (this file)
//        │
//        ▼
//   Renderer (MiniPlayerBackground, Album, Wave, Controls, etc.)
//        │
//        ▼
//   Compose
//        │
//        ▼
//   Pixels
//
// This spec defines:
//   Chapter A — Visual Language (the grammar of every visual element)
//   Chapter B — Living Objects (personality of each component)
//   Chapter C — Interaction Grammar (unified interaction language)
//   Chapter D — Composition (layout, z-order, spacing, adaptive sizing)
//   Chapter E — Performance Constitution (per-frame discipline)
//   Chapter F — Rendering Dependency Matrix + Rules Registry
//
// ⭐ LOCKED SPEC ⭐
//   Once approved, no changes without Product Director sign-off.
//   Every renderer must conform to this spec. No exceptions.
//
// Production Rules enforced:
//   Rule #17 — Single Snapshot Rule (one component, one snapshot)
//   Rule #18 — Morph Source Opacity (components don't know morph source)
//   Rule #19 — No Hardcoded Duration (all motion from MiniPlayerMotion)
//   Rule #20 — Renderer Never Interprets (renderer receives, renderer paints)
//   Rule #21 — Every Visual Value Traceable (no magic numbers)
//   Rule #22 — Every Component Replaceable in One Afternoon
// ═══════════════════════════════════════════════════════════════════════════════════════


// ═══════════════════════════════════════════════════════════════════════════════════════
// CHAPTER A — VISUAL LANGUAGE
//
// Mini Player এর প্রতিটা visual element-এর grammar।
// এটা code না — visual vocabulary।
// প্রতিটা renderer এই grammar follow করবে।
//
// Visual Elements:
//   A.1 Background   — Ambient atmosphere (NOT wallpaper)
//   A.2 Surface      — Glass container (the physical body)
//   A.3 Glass        — Frosted translucency behavior
//   A.4 Shadow       — Depth and grounding
//   A.5 Glow         — Ambient light emission
//   A.6 Reflection   — Mirror/surface reflection on album art
//   A.7 Noise        — Subtle texture for material feel
//   A.8 Depth        — Z-axis layering and parallax
// ═══════════════════════════════════════════════════════════════════════════════════════

// ─────────────────────────────────────────────────────────────────────────────────────
// A.1 BACKGROUND — Ambient Atmosphere
// ─────────────────────────────────────────────────────────────────────────────────────
//
// Purpose:
//   Mini Player-এর background কোনো wallpaper না। এটা atmosphere।
//   Album art-এর color intelligence-এর condensed ambient form।
//
// Visual Stack (back → front):
//   Layer 1: Color Wash — primary color with very low alpha (atmospheric base)
//   Layer 2: Ambient Gradient — radial gradient from color.primary/secondary
//   Layer 3: Energy Pulse — subtle pulse modulated by energyIntensity
//
// Source of Truth:
//   ALL colors from MiniPlayerColorSnapshot (via MiniPlayerState.color)
//   NO direct ColorState access
//
// Behavior:
//   - Idle: static dark ambient, no pulse
//   - Breathing: subtle sine-wave alpha modulation (14s cycle, shared with NowPlaying)
//   - Playing: ambient brightens with energyIntensity, pulses with pulseAmount
//   - Beat: sharp alpha spike on beat detection
//
// Forbidden:
//   ❌ Blur calculation — Color Brain already solved this
//   ❌ Palette extraction — Color Intelligence Engine already solved this
//   ❌ Dominant color computation — already in ColorState → mapped to snapshot
//   ❌ Bitmap processing — no album art blur in mini player background
//   ❌ FFT / PCM / AudioFrame — Wave Engine already solved this
//
// Dimensions:
//   Fills entire MiniPlayer surface (matches layout.height × layout.widthFraction)
//   Clips to layout.cornerRadius
//
// Energy Response:
//   alpha = baseAlpha + (energyIntensity × energyModulation) + (pulseAmount × sinePhase)
//   baseAlpha = 0.08 (collapsed) → 0.15 (expanded)
//   energyModulation = 0.12
//   Where: energyIntensity, pulseAmount from MiniPlayerColorSnapshot
//
// Trace:
//   Background Alpha → MiniPlayerColorSnapshot.energyIntensity
//                     → MiniPlayerMapper.mapColors()
//                     → ColorState.energyIntensity
//                     → ColorIntelligenceEngine

object MiniPlayerVisualBackground {
    // ── Constants ────────────────────────────────────────────────────────
    /** Base ambient alpha when collapsed and idle. */
    const val BASE_ALPHA_COLLAPSED = 0.08f

    /** Base ambient alpha when fully expanded. */
    const val BASE_ALPHA_EXPANDED = 0.15f

    /** How much energyIntensity modulates the ambient brightness. */
    const val ENERGY_MODULATION = 0.12f

    /** Ambient breathing cycle duration (ms). Shared with NowPlaying drift. */
    const val BREATHING_CYCLE_MS = 14_000

    /** Pulse breathing cycle duration (ms). Faster than ambient breathing. */
    const val PULSE_CYCLE_MS = 6_000

    // ── Gradient configuration ───────────────────────────────────────────
    /** Primary blob center X (fraction of width). */
    const val PRIMARY_CENTER_X = 0.3f

    /** Primary blob center Y (fraction of height). */
    const val PRIMARY_CENTER_Y = 0.2f

    /** Secondary blob center X (fraction of width). */
    const val SECONDARY_CENTER_X = 0.8f

    /** Secondary blob center Y (fraction of height). */
    const val SECONDARY_CENTER_Y = 0.85f

    /** Primary blob radius (fraction of width). */
    const val PRIMARY_RADIUS = 1.2f

    /** Secondary blob radius (fraction of width). */
    const val SECONDARY_RADIUS = 1.0f

    /** Primary blob drift amplitude X. */
    const val DRIFT_AMPLITUDE_X = 0.12f

    /** Primary blob drift amplitude Y. */
    const val DRIFT_AMPLITUDE_Y = 0.08f
}


// ─────────────────────────────────────────────────────────────────────────────────────
// A.2 SURFACE — Glass Container (The Physical Body)
// ─────────────────────────────────────────────────────────────────────────────────────
//
// Purpose:
//   Mini Player-এর main body। এটা একটা physical object — glass material,
//   real depth, real light interaction। Not a flat card.
//
// Visual Properties:
//   - Glass tint: from MiniPlayerColorSnapshot.glassTint, modulated by layout.glassTintAlpha
//   - Border: 0.8dp, GlassBorderLight, alpha = layout.glassTintAlpha × 0.8
//   - Specular: top-left radial highlight (same light source as GlassSurface)
//   - Corner radius: layout.cornerRadius (28dp collapsed → 0dp expanded)
//   - Height: layout.height (80dp collapsed → 240dp expanded)
//   - Width: layout.widthFraction × screenWidth
//   - Shadow: layout.shadowElevation, color from MiniPlayerColorSnapshot.shadowColor
//
// Source of Truth:
//   ALL values from MiniPlayerLayoutSnapshot + MiniPlayerColorSnapshot
//   Uses GlassSurface composable with dynamic style derived from layout state
//
// Morph Behavior:
//   As morphProgress increases:
//   - cornerRadius: 28dp → 0dp (rounded pill → full-width rectangle)
//   - height: 80dp → 240dp
//   - widthFraction: 0.94 → 1.0
//   - glassTintAlpha: 0.10 → 0.0 (glass fades to transparent)
//   - glassShadowAlpha: 0.20 → 0.0
//   - shadowElevation: 8dp → 24dp (lifts off screen)
//   - miniPlayerOpacity: 1.0 → 0.0 (crossfades to NowPlaying)
//
// Components NEVER see morphProgress directly.
// They see: layout.cornerRadius, layout.height, layout.glassTintAlpha, etc.
// (Rule #18 — Morph Source Opacity)
//
// Forbidden:
//   ❌ Own corner radius calculation — layout.cornerRadius already computed
//   ❌ Own height calculation — layout.height already computed
//   ❌ Direct ColorState.glassTint — use state.color.glassTint
//   ❌ Direct morphProgress read — use layout.* values

object MiniPlayerVisualSurface {
    // ── Glass Configuration ─────────────────────────────────────────────
    /** Base tint alpha from GlassTokens.MiniPlayer (collapsed state). */
    const val BASE_GLASS_TINT_ALPHA = 0.10f

    /** Base shadow alpha from GlassTokens.MiniPlayer. */
    const val BASE_GLASS_SHADOW_ALPHA = 0.20f

    /** Border width in dp. */
    const val BORDER_WIDTH_DP = 0.8f

    /** Minimum corner radius (fully expanded). */
    const val CORNER_RADIUS_MIN = 0f

    /** Maximum corner radius (collapsed). */
    const val CORNER_RADIUS_MAX = 28f

    // ── Morph-driven values (for reference; components read layout.*) ────
    /** Collapsed height in dp. */
    const val HEIGHT_COLLAPSED = 80f

    /** Fully expanded height in dp. */
    const val HEIGHT_EXPANDED = 240f

    /** Collapsed width as fraction of screen. */
    const val WIDTH_FRACTION_COLLAPSED = 0.94f

    /** Expanded width (full screen). */
    const val WIDTH_FRACTION_EXPANDED = 1.0f

    /** Shadow elevation collapsed in dp. */
    const val SHADOW_COLLAPSED = 8f

    /** Shadow elevation expanded in dp. */
    const val SHADOW_EXPANDED = 24f
}


// ─────────────────────────────────────────────────────────────────────────────────────
// A.3 GLASS — Frosted Translucency Behavior
// ─────────────────────────────────────────────────────────────────────────────────────
//
// Purpose:
//   Mini Player surface-এর glass material behavior define করে।
//   Glass মানে শুধু blur না — এটা light interaction system।
//
// Glass Stack (within GlassSurface):
//   1. Tint Layer — color overlay (glassTint × layout.glassTintAlpha)
//   2. Specular Highlight — top-left radial catch-light
//   3. Border — thin edge line with glass visibility
//   4. Shadow — depth grounding
//   5. Glow — ambient light emission (energy-reactive)
//
// Morph Effect on Glass:
//   Collapsed (p=0): Full glass treatment — visible tint, border, specular
//   Expanding (p=0.5): Glass fading — tint alpha halved, border dimming
//   Near Handoff (p=0.85): Glass nearly gone — minimal tint, border barely visible
//   Handoff (p=1.0): Glass zero — fully transparent (NowPlaying takes over)
//
// This fade is natural because:
//   - layout.glassTintAlpha = 0.10 × (1 - morphProgress)
//   - layout.glassShadowAlpha = 0.20 × (1 - morphProgress)
//   Components just apply these values. Zero computation.
//
// Forbidden:
//   ❌ Custom blur — use GlassSurface with style token
//   ❌ Custom specular — GlassSurface handles this
//   ❌ Direct alpha calculation — layout.glassTintAlpha already solved

object MiniPlayerVisualGlass {
    /** Specular highlight center X (fraction of width). */
    const val SPECULAR_CENTER_X = 0.20f

    /** Specular highlight center Y (fraction of height). */
    const val SPECULAR_CENTER_Y = 0.12f

    /** Specular highlight radius (fraction of min dimension). */
    const val SPECULAR_RADIUS = 0.45f

    /** Grounding shadow center X (fraction of width). */
    const val GROUNDING_CENTER_X = 0.86f

    /** Grounding shadow center Y (fraction of height). */
    const val GROUNDING_CENTER_Y = 0.92f
}


// ─────────────────────────────────────────────────────────────────────────────────────
// A.4 SHADOW — Depth and Grounding
// ─────────────────────────────────────────────────────────────────────────────────────
//
// Purpose:
//   Mini Player-কে screen-এ ground করে। Shadow মানে depth illusion।
//
// Shadow System:
//   1. Elevation Shadow — Android system shadow (from layout.shadowElevation)
//   2. Color Shadow — tinted shadow using state.color.shadowColor
//   3. Glow Shadow — ambient halo using state.color.ambientGlow
//
// Elevation Shadow:
//   - Collapsed: 8dp elevation (resting on surface)
//   - Expanding: 8dp → 24dp (lifting off)
//   - Energy modulates: when energyIntensity > 0.5f, shadow grows 10%
//
// Color Shadow:
//   - Uses state.color.shadowColor (already computed by Color Brain)
//   - Alpha = layout.glassShadowAlpha × energyIntensity
//   - Never hardcoded black — always from Color Intelligence
//
// Forbidden:
//   ❌ Hardcoded shadow color — state.color.shadowColor
//   ❌ Own elevation calculation — layout.shadowElevation
//   ❌ ShadowLayer with magic values — all traceable

object MiniPlayerVisualShadow {
    /** Base elevation collapsed in dp. */
    const val ELEVATION_COLLAPSED = 8f

    /** Base elevation expanded in dp. */
    const val ELEVATION_EXPANDED = 24f

    /** Energy modulation factor (shadow grows this % when energy > 0.5). */
    const val ENERGY_ELEVATION_BOOST = 0.10f

    /** Shadow base color (warm anchor, from GlassSurface). */
    const val SHADOW_BASE_COLOR = 0xFF211A12

    /** Shadow ambient alpha factor. */
    const val SHADOW_AMBIENT_FACTOR = 0.7f

    /** Glow radius base in dp (collapsed, no glow). */
    const val GLOW_RADIUS_COLLAPSED = 0f

    /** Glow radius expanded in dp. */
    const val GLOW_RADIUS_EXPANDED = 40f

    /** Glow intensity collapsed. */
    const val GLOW_INTENSITY_COLLAPSED = 0f

    /** Glow intensity expanded. */
    const val GLOW_INTENSITY_EXPANDED = 1f
}


// ─────────────────────────────────────────────────────────────────────────────────────
// A.5 GLOW — Ambient Light Emission
// ─────────────────────────────────────────────────────────────────────────────────────
//
// Purpose:
//   Mini Player-এর চারপাশে soft light halo। Energy-reactive.
//
// Behavior:
//   Collapsed (p < 0.50): No glow (glowRadius = 0)
//   Expanding (p > 0.50): Glow appears and grows
//   Fully expanded: Maximum glow (radius = 40dp, intensity = 1.0)
//   Energy: glowIntensity modulated by state.color.energyIntensity
//
// Source:
//   Color: state.color.ambientGlow (from Color Intelligence)
//   Radius: layout.glowRadius (from Mapper, morphProgress-derived)
//   Intensity: layout.glowIntensity (from Mapper, morphProgress-derived)
//
// Implementation:
//   Canvas → drawCircle with radialGradient
//   Center: surface center
//   Colors: [ambientGlow.copy(alpha = glowAlpha), ambientGlow.copy(alpha = 0), Transparent]
//
// Forbidden:
//   ❌ Own glow radius calculation — layout.glowRadius
//   ❌ Own color computation — state.color.ambientGlow

object MiniPlayerVisualGlow {
    // All values come from layout snapshot. Constants here for reference only.
    /** Glow threshold — glow only appears above this morphProgress. */
    const val GLOW_MORPH_THRESHOLD = 0.50f

    /** Maximum glow radius in dp. */
    const val GLOW_RADIUS_MAX_DP = 40f
}


// ─────────────────────────────────────────────────────────────────────────────────────
// A.6 REFLECTION — Mirror/Surface Reflection on Album Art
// ─────────────────────────────────────────────────────────────────────────────────────
//
// Purpose:
//   Album art-এর উপরে subtle reflection — যেন album art একটা glossy
//   surface-এ বসে আছে। Premium feel.
//
// Behavior:
//   Collapsed (p < 0.70): No reflection (reflectionAlpha = 0)
//   Expanding (p > 0.70): Reflection fades in
//   Near Handoff: Maximum reflection (alpha = 0.25)
//
// Implementation:
//   Diagonal gradient overlay on album art:
//   - White specular: top-left to bottom-right, alpha = reflectionAlpha
//   - Creates a "glass over print" effect
//
// Source:
//   Alpha: layout.reflectionAlpha (from Mapper)
//   No other source needed — pure gradient overlay
//
// Forbidden:
//   ❌ Real-time reflection calculation — pre-computed gradient
//   ❌ Bitmap manipulation — pure Compose gradient
//   ❌ Own alpha calculation — layout.reflectionAlpha

object MiniPlayerVisualReflection {
    /** Reflection threshold — appears above this morphProgress. */
    const val REFLECTION_MORPH_THRESHOLD = 0.70f

    /** Maximum reflection alpha. */
    const val REFLECTION_ALPHA_MAX = 0.25f

    /** Gradient angle in degrees (diagonal, top-left to bottom-right). */
    const val GRADIENT_ANGLE_DEGREES = 135f
}


// ─────────────────────────────────────────────────────────────────────────────────────
// A.7 NOISE — Subtle Texture for Material Feel
// ─────────────────────────────────────────────────────────────────────────────────────
//
// Purpose:
//   Glass surface-এ very subtle noise texture — যেন frosted glass আসলেই
//   frosted। Without noise, glass looks like a flat gradient.
//
// Implementation:
//   Canvas drawRect with pre-generated NoisePattern
//   Alpha = 0.03 — barely visible, just enough to break gradient banding
//
// Note:
//   This is a PRE-GENERATED texture (static Bitmap), NOT per-frame noise.
//   Created once, cached in remember, reused every frame.
//   Zero per-frame allocation. (Performance Constitution — Chapter E)
//
// Source:
//   Static — no state dependency
//   Alpha: fixed at 0.03 (too subtle to need energy modulation)
//
// Forbidden:
//   ❌ Per-frame noise generation — pre-generated only
//   ❌ Random number generation — deterministic texture
//   ❌ Bitmap creation per frame — cached in remember

object MiniPlayerVisualNoise {
    /** Noise texture alpha — barely visible. */
    const val NOISE_ALPHA = 0.03f

    /** Noise pattern tile size in pixels (square). */
    const val NOISE_TILE_SIZE = 128
}


// ─────────────────────────────────────────────────────────────────────────────────────
// A.8 DEPTH — Z-Axis Layering and Parallax
// ─────────────────────────────────────────────────────────────────────────────────────
//
// Purpose:
//   Mini Player-এর elements-এর z-ordering define করে। কোন element কার
//   উপরে, কোনটা নিচে। এটা visual depth hierarchy.
//
// Z-Order (back → front):
//   Z-0: Background (ambient atmosphere)
//   Z-1: Surface shadow (elevation shadow + color shadow)
//   Z-2: Surface glow (ambient halo)
//   Z-3: Surface body (glass container)
//   Z-4: Noise texture (over glass)
//   Z-5: Wave ribbon (within surface)
//   Z-6: Album art (within surface)
//   Z-7: Album reflection (over album art)
//   Z-8: Metadata text (song title, artist)
//   Z-9: Progress indicator (waveform strip)
//   Z-10: Controls (play/pause, next, prev)
//   Z-11: Press feedback overlay (scale, haptic visual)
//   Z-12: Gesture overlay (drag visual, dismiss indicator)
//
// This z-order is MANDATORY. Every MiniPlayerLayout must respect it.
// Rearranging z-order requires spec change.
//
// Parallax:
//   Mini Player-এ parallax minimal — only during morph.
//   Album art has slight upward float during expansion (5dp offset at p=1.0)
//   This comes from layout computation, not component calculation.

object MiniPlayerVisualDepth {
    /** Z-Order layers (back to front). Higher index = closer to user. */
    enum class ZLayer(val index: Int) {
        BACKGROUND(0),
        SHADOW(1),
        GLOW(2),
        SURFACE_BODY(3),
        NOISE(4),
        WAVE_RIBBON(5),
        ALBUM_ART(6),
        ALBUM_REFLECTION(7),
        METADATA(8),
        PROGRESS(9),
        CONTROLS(10),
        PRESS_FEEDBACK(11),
        GESTURE_OVERLAY(12)
    }

    /** Album art parallax offset at full expansion (dp). */
    const val ALBUM_PARALLAX_MAX_DP = 5f
}


// ═══════════════════════════════════════════════════════════════════════════════════════
// CHAPTER B — LIVING OBJECTS
//
// প্রতিটা component-এর personality।
// এটা শুধু rendering spec না — personality definition।
//
// Objects:
//   B.1 MiniPlayerBackground — Ambient atmosphere
//   B.2 MiniPlayerSurface    — Glass container body
//   B.3 MiniPlayerAlbum      — Album art with life
//   B.4 MiniPlayerWave       — Living emotional indicator
//   B.5 MiniPlayerMetadata   — Song information
//   B.6 MiniPlayerControls   — Music interaction
//   B.7 MiniPlayerProgress   — Playback timeline
//   B.8 MiniPlayerShadow     — Depth layer (external shadow)
//   B.9 MiniPlayerGesture    — Touch interaction layer
//   B.10 MiniPlayerLayout    — Component composition root
// ═══════════════════════════════════════════════════════════════════════════════════════


// ─────────────────────────────────────────────────────────────────────────────────────
// B.1 MINI PLAYER BACKGROUND — Ambient Atmosphere
// ─────────────────────────────────────────────────────────────────────────────────────

object MiniPlayerBackgroundSpec {

    // ── Identity ────────────────────────────────────────────────────────
    const val PURPOSE = "Ambient atmosphere. NOT wallpaper."
    const val PERSONALITY = "Breathes. Never flashes. Never jumps. Slow organic drift."

    // ── Inputs (what it reads from state) ────────────────────────────────
    // Rule #17: Single Snapshot — this component reads only state.color
    //
    // Reads:
    //   state.color.primary          → primary blob color
    //   state.color.secondary       → secondary blob color
    //   state.color.energyIntensity → brightness modulation
    //   state.color.pulseAmount     → pulse depth
    //   state.color.glassTint       → tint overlay color
    //   state.color.isIdle          → breathing vs static
    //   layout.height              → canvas size
    //   layout.widthFraction        → canvas size
    //   layout.cornerRadius         → clip shape

    // ── Outputs (what it renders) ───────────────────────────────────────
    //   - Canvas with radial gradient blobs
    //   - Animated breathing (14s cycle)
    //   - Energy-modulated alpha

    // ── Visual Personality ──────────────────────────────────────────────
    //
    // ALWAYS:
    //   - Slow drift (never static, never fast)
    //   - Smooth color transitions (RikoMotion.colorTransition())
    //   - Energy-responsive brightness
    //
    // NEVER:
    //   - Flash on beat (that's Wave's job)
    //   - Jump on song change (crossfade only)
    //   - Show hard edges (always radial gradients)
    //   - Use pure black (always has color tint)

    // ── Motion Language ──────────────────────────────────────────────────
    //   Color transitions: RikoMotion.colorTransition() (700ms, FastOutSlowIn)
    //   Breathing cycle: infiniteRepeatable(tween(14000ms, LinearEasing))
    //   Pulse cycle: infiniteRepeatable(tween(6000ms, LinearEasing))
    //   All from MiniPlayerMotion (Rule #19 — No Hardcoded Duration)

    // ── Glass Behaviour ──────────────────────────────────────────────────
    //   Background is BEHIND glass. It does not have glass treatment itself.
    //   It renders on Canvas, clipped to surface shape.

    // ── Shadow Behaviour ────────────────────────────────────────────────
    //   No shadow. Background is the bottom-most layer.

    // ── Failure Behaviour ───────────────────────────────────────────────
    //   If state.color == DEFAULT → renders dark ambient (ColorAbyssBlack-ish)
    //   If state.color == FALLBACK → renders fallback gradient
    //   Never crashes, never shows blank, never shows error color

    // ── Performance Budget ──────────────────────────────────────────────
    //   Target: 0.5ms per frame
    //   Canvas draws: 3 (base fill + 2 gradients)
    //   Allocations per frame: 0
    //   Path objects reused: yes (pre-created in remember)

    // ── Acceptance Tests ──────────────────────────────────────────────────
    //   AT-BG-1: Idle state shows subtle dark ambient
    //   AT-BG-2: Playing state shows colored ambient matching album art
    //   AT-BG-3: Song change crossfades colors (700ms)
    //   AT-BG-4: Energy increase brightens ambient
    //   AT-BG-5: No visible banding in gradients
    //   AT-BG-6: Clips to corner radius exactly

    // ── Future Extension ─────────────────────────────────────────────────
    //   - Album art blur backdrop (Android S+, like NowPlaying)
    //   - Multi-color gradient mesh
    //   - Reactive particle field

    // ── Forbidden Things ─────────────────────────────────────────────────
    //   ❌ Blur calculation
    //   ❌ Palette extraction
    //   ❌ Dominant color computation
    //   ❌ Bitmap processing
    //   ❌ Direct ColorState access
    //   ❌ Direct WaveState access
    //   ❌ remember {} with mutable state
    //   ❌ FFT / PCM / AudioFrame
    //   ❌ Hardcoded animation duration
    //   ❌ Business logic
}


// ─────────────────────────────────────────────────────────────────────────────────────
// B.2 MINI PLAYER SURFACE — Glass Container Body
// ─────────────────────────────────────────────────────────────────────────────────────

object MiniPlayerSurfaceSpec {

    // ── Identity ────────────────────────────────────────────────────────
    const val PURPOSE = "Physical glass container. The body of the Mini Experience Surface."
    const val PERSONALITY = "Solid. Grounded. Transforms smoothly. Never glitches."

    // ── Inputs ──────────────────────────────────────────────────────────
    // Rule #17: Reads layout.* and state.color.* only
    //
    // Reads:
    //   state.color.glassTint     → tint color
    //   state.color.shadowColor   → shadow tint
    //   state.color.ambientGlow  → glow color
    //   state.color.isDarkArtwork → brightness adaptation
    //   layout.height             → surface height
    //   layout.widthFraction      → surface width
    //   layout.cornerRadius       → corner radius
    //   layout.glassTintAlpha     → tint intensity
    //   layout.glassShadowAlpha   → shadow intensity
    //   layout.shadowElevation    → elevation
    //   layout.glowRadius         → glow presence
    //   layout.glowIntensity      → glow brightness
    //   layout.miniPlayerOpacity  → crossfade alpha

    // ── Visual Personality ──────────────────────────────────────────────
    //
    // ALWAYS:
    //   - Rounded corners (when collapsed)
    //   - Glass treatment (tint + border + specular)
    //   - Smooth morph transitions
    //   - Consistent with GlassSurface behavior across the app
    //
    // NEVER:
    //   - Hard edges during morph
    //   - Flicker on state change
    //   - Incorrect corner clipping
    //   - Content overflow beyond glass boundary

    // ── Morph Behavior ──────────────────────────────────────────────────
    //   Surface wraps ALL content. During morph:
    //   - Height grows: 80dp → 240dp
    //   - Width expands: 94% → 100%
    //   - Corners soften: 28dp → 0dp
    //   - Glass fades: tintAlpha 0.10 → 0.0
    //   - Shadow lifts: 8dp → 24dp
    //   - Crossfades: opacity 1.0 → 0.0 (to NowPlaying)
    //
    //   ALL values from layout snapshot. Component applies them.
    //   ZERO computation in composables. (Rule #20)

    // ── Performance Budget ──────────────────────────────────────────────
    //   Target: 0.3ms per frame (delegated to GlassSurface)
    //   GlassSurface is the only render target here
    //   Surface only configures parameters, doesn't draw

    // ── Acceptance Tests ────────────────────────────────────────────────
    //   AT-SF-1: Collapsed state matches existing MiniPlayer visual
    //   AT-SF-2: Morph progress 0.5 shows half-expanded surface
    //   AT-SF-3: Corner radius follows layout.cornerRadius exactly
    //   AT-SF-4: Glass tint alpha matches layout.glassTintAlpha
    //   AT-SF-5: Shadow elevation matches layout.shadowElevation
    //   AT-SF-6: Crossfade opacity matches layout.miniPlayerOpacity
    //   AT-SF-7: Width matches layout.widthFraction × screenWidth

    // ── Forbidden Things ─────────────────────────────────────────────────
    //   ❌ Direct morphProgress read
    //   ❌ Own height/corner/width calculation
    //   ❌ Direct ColorState access
    //   ❌ Direct PlayerState access
    //   ❌ remember {} with mutable state for layout values
    //   ❌ Hardcoded dimensions
    //   ❌ Business logic
}


// ─────────────────────────────────────────────────────────────────────────────────────
// B.3 MINI PLAYER ALBUM — Album Art with Life
// ─────────────────────────────────────────────────────────────────────────────────────

object MiniPlayerAlbumSpec {

    // ── Identity ────────────────────────────────────────────────────────
    const val PURPOSE = "Visual anchor. Song identity. NOT a thumbnail."
    const val PERSONALITY =
        "Never rotates. Never shakes. " +
        "Floats (gentle bob). " +
        "Breathes (subtle scale pulse). " +
        "Tilts (device accelerometer). " +
        "Reflects (glass overlay). " +
        "Returns (always back to rest)."

    // ── Inputs ──────────────────────────────────────────────────────────
    // Rule #17: Reads state.currentSong, state.albumArtUri, state.color, layout.*
    //
    // Reads:
    //   state.currentSong        → song identity (for content description)
    //   state.albumArtUri        → album art image URI
    //   state.color.ambientGlow  → glow color behind album
    //   state.color.shadowColor  → shadow tint
    //   state.color.energyIntensity → glow intensity modulation
    //   state.wave.hasBeat       → beat ripple trigger
    //   state.interaction.pressScale → press feedback scale
    //   state.interaction.isBeatReacting → beat reaction state
    //   layout.albumArtSize      → album art size (52dp → 180dp)
    //   layout.reflectionAlpha   → glass reflection intensity
    //   layout.cornerRadius      → clip shape

    // ── Visual Personality ──────────────────────────────────────────────
    //
    // ALWAYS:
    //   - Centered in its allocated space
    //   - Rounded corners matching RikoShape.MiniArtwork (8dp collapsed, 16dp expanded)
    //   - ContentScale.Crop (fills bounds, no letterboxing)
    //   - Coil AsyncImage for loading (shared image loader)
    //   - Placeholder when no art (gradient placeholder from colors)
    //
    // NEVER:
    //   ❌ Rotates (never spin, never carousel)
    //   ❌ Shakes (no vibration, no jitter)
    //   ❌ Teleports (always animated to new position)
    //   ❌ Pops (no scale bounce on appear)
    //   ❌ Stretches disproportionately
    //   ❌ Shows loading spinner over art

    // ── Motion Language ──────────────────────────────────────────────────
    //   Float: sin-based vertical bob, amplitude 3dp, period 4000ms
    //     - Paused: amplitude reduces to 1dp, period slows to 8000ms
    //     - Idle: no float (static)
    //   Beat ripple: scale 1.0 → 1.02 → 1.0, 200ms spring
    //     - Triggered by state.wave.hasBeat
    //   Press scale: state.interaction.pressScale (0.92 when pressed)
    //   Color transition: RikoMotion.colorTransition() on song change
    //   All durations from MiniPlayerMotion (Rule #19)

    // ── Glass Behaviour ──────────────────────────────────────────────────
    //   Reflection overlay (diagonal gradient, white, alpha = layout.reflectionAlpha)
    //   Visible only when layout.reflectionAlpha > 0 (morphProgress > 0.70)
    //   This creates "glass over print" premium feel

    // ── Shadow Behaviour ────────────────────────────────────────────────
    //   Elevation shadow: follows layout.shadowElevation
    //   Color shadow: state.color.shadowColor, alpha modulated by energy
    //   Glow: state.color.ambientGlow, radius = layout.glowRadius
    //   During morph: shadow lifts, glow appears

    // ── Failure Behaviour ───────────────────────────────────────────────
    //   No album art (albumArtUri == null):
    //     - Show gradient placeholder using state.color.primary → secondary
    //     - Rounded shape matching album art bounds
    //     - Music note icon centered
    //   Art load failure:
    //     - Same placeholder
    //     - No retry animation (Coil handles retry internally)

    // ── Performance Budget ──────────────────────────────────────────────
    //   Target: 1.0ms per frame
    //   Image decode: handled by Coil (cached, not per-frame)
    //   Float animation: graphicsLayer{} (GPU accelerated)
    //   Reflection overlay: simple gradient (cheap)
    //   Allocations per frame: 0

    // ── Acceptance Tests ──────────────────────────────────────────────────
    //   AT-AL-1: Collapsed shows 52dp album art with 8dp corner radius
    //   AT-AL-2: Expanded shows up to 180dp album art
    //   AT-AL-3: No album art shows gradient placeholder
    //   AT-AL-4: Song change crossfades album art
    //   AT-AL-5: Beat detected triggers subtle scale pulse
    //   AT-AL-6: Float animation visible when playing
    //   AT-AL-7: Float stops when paused (slow settle)
    //   AT-AL-8: Reflection visible only when expanding (p > 0.70)
    //   AT-AL-9: Press scales to 0.92 on play/pause button press
    //   AT-AL-10: Size matches layout.albumArtSize exactly

    // ── Future Extension ─────────────────────────────────────────────────
    //   - Device accelerometer tilt (like HeroAlbum)
    //   - Parallax depth effect during morph
    //   - Artwork color extraction for dynamic border

    // ── Forbidden Things ─────────────────────────────────────────────────
    //   ❌ Rotation animation
    //   ❌ Shake / vibration effect
    //   ❌ Pop-in animation
    //   ❌ remember {} with mutable bitmap state
    //   ❌ Direct ColorState access
    //   ❌ Direct WaveState access
    //   ❌ FFT / PCM / Energy computation
    //   ❌ Own size calculation — layout.albumArtSize
    //   ❌ Hardcoded animation duration
    //   ❌ Business logic (play/pause/seek)
    //   ❌ Own reflection alpha — layout.reflectionAlpha
}


// ─────────────────────────────────────────────────────────────────────────────────────
// B.4 MINI PLAYER WAVE — Living Emotional Indicator
// ─────────────────────────────────────────────────────────────────────────────────────

object MiniPlayerWaveSpec {

    // ── Identity ────────────────────────────────────────────────────────
    const val PURPOSE = "Living emotional indicator. NOT a visualizer."
    const val PERSONALITY =
        "Never explodes. Never dies. Never jitters. " +
        "Always remembers. " +
        "Breathes when idle. " +
        "Flows when playing. " +
        "Peaks on beat. " +
        "Calmly settles when paused."

    // ── Inputs ──────────────────────────────────────────────────────────
    // Rule #17: Reads state.wave and state.color only
    //
    // Reads:
    //   state.wave.isActive         → wave is alive (vs idle)
    //   state.wave.hasBeat          → beat pulse trigger
    //   state.wave.overallEnergy    → wave intensity/height
    //   state.wave.thickness        → stroke width multiplier
    //   state.wave.opacity          → wave visibility
    //   state.wave.isExpanding      → expansion animation trigger
    //   state.wave.isSettling       → settling animation trigger
    //   state.wave.energy           → energy for modulation
    //   state.wave.isIdle           → idle state (breathing only)
    //   state.color.waveStart      → gradient start color
    //   state.color.waveEnd        → gradient end color
    //   state.color.isIdle          → color idle check
    //   state.color.energyIntensity → color modulation
    //   state.color.pulseAmount     → pulse depth
    //   layout.waveVisibility       → wave alpha (0 → 1 during morph Phase 4)
    //   layout.waveThickness        → thickness multiplier (0.5 → 1.0 during morph)
    //   layout.height               → available height for wave
    //   layout.widthFraction        → available width for wave
    //   state.isPlaying             → play/pause state for breathing

    // ── Visual Personality ──────────────────────────────────────────────
    //
    // ALWAYS:
    //   - Smooth flowing ribbon (not jagged bars)
    //   - Color gradient from waveStart → waveEnd
    //   - Energy-responsive height/intensity
    //   - Beat-reactive peak
    //   - Gentle idle breathing when not playing
    //
    // NEVER:
    //   ❌ Explode (no burst effect)
    //   ❌ Die (no sudden disappearance — always fades)
    //   ❌ Jitter (no random noise — smooth interpolation only)
    //   ❌ Show hard edges (rounded caps, smooth curves)
    //   ❌ Flash white on beat (color pulse, not flash)
    //   ❌ Use FFT bars (ribbon, not histogram)
    //   ❌ Compute energy (already in state.wave.overallEnergy)

    // ── Wave Rendering ──────────────────────────────────────────────────
    //   Mini Player wave is a SIMPLIFIED version of Now Playing wave.
    //   It does NOT use the full Wave Engine geometry (OrganicPath, RibbonState).
    //   It renders a smooth sine-wave-like ribbon modulated by energy.
    //
    //   Why simplified?
    //     Mini Player is 80dp tall collapsed. Full wave geometry is overkill.
    //     The simplified wave reads state.wave.overallEnergy and creates a
    //     smooth animated ribbon that responds to the same energy data.
    //
    //   Wave shape:
    //     - Base: gentle sine wave (amplitude modulated by energy)
    //     - Energy modulation: higher energy = higher amplitude
    //     - Beat modulation: on hasBeat, amplitude spikes briefly
    //     - Idle: gentle breathing sine (low amplitude, slow)
    //
    //   Rendering:
    //     Canvas → drawPath with smooth curve
    //     Stroke: waveThickness × state.wave.thickness
    //     Color: gradient(waveStart, waveEnd)
    //     Alpha: layout.waveVisibility × state.wave.opacity

    // ── Morph Behavior ──────────────────────────────────────────────────
    //   Collapsed (p < 0.30): waveVisibility = 0, wave not visible
    //   Phase 4 (p 0.50 → 0.70): waveVisibility 0 → 1, wave fades in
    //   Expanded (p > 0.70): full visibility
    //
    //   The wave ribbon appears during morph Phase 4 (WAVE phase).
    //   This matches the contract: Phase 4 = "Wave ribbon appearing"

    // ── Performance Budget ──────────────────────────────────────────────
    //   Target: 0.8ms per frame
    //   Canvas draws: 1 (single path)
    //   Path points: ~40 (smooth curve)
    //   Allocations per frame: 0 (path reused)
    //   No bitmap creation
    //   No object creation

    // ── Acceptance Tests ────────────────────────────────────────────────
    //   AT-WV-1: Idle state shows subtle breathing wave
    //   AT-WV-2: Playing state shows energy-modulated wave
    //   AT-WV-3: Beat triggers amplitude spike
    //   AT-WV-4: Song change crossfades wave colors
    //   AT-WV-5: Wave not visible when collapsed (p < 0.30)
    //   AT-WV-6: Wave fades in during Phase 4 (p 0.50 → 0.70)
    //   AT-WV-7: Wave fully visible when expanded
    //   AT-WV-8: Pause → wave settles calmly (no sudden death)
    //   AT-WV-9: Gradient colors match state.color.waveStart/waveEnd
    //   AT-WV-10: Thickness follows layout.waveThickness

    // ── Future Extension ─────────────────────────────────────────────────
    //   - Full Wave Engine geometry (OrganicPath rendering)
    //   - Multiple ribbon layers
    //   - Glass highlight on wave

    // ── Forbidden Things ─────────────────────────────────────────────────
    //   ❌ FFT computation
    //   ❌ PCM data reading
    //   ❌ EnergySolver / Physics calculation
    //   ❌ Color extraction
    //   ❌ Direct WaveState access (use state.wave)
    //   ❌ Direct AudioState access
    //   ❌ Energy self-computation
    //   ❌ Hardcoded wave amplitude
    //   ❌ Hardcoded animation duration
    //   ❌ Business logic
    //   ❌ remember {} with mutable path state (reuse path object)
}


// ─────────────────────────────────────────────────────────────────────────────────────
// B.5 MINI PLAYER METADATA — Song Information
// ─────────────────────────────────────────────────────────────────────────────────────

object MiniPlayerMetadataSpec {

    // ── Identity ────────────────────────────────────────────────────────
    const val PURPOSE = "Song identity display. NOT a text editor."
    const val PERSONALITY =
        "Always readable. Never overlaps. " +
        "Truncates gracefully. " +
        "Crossfades on song change. " +
        "Color adapts to artwork. " +
        "Never animates position (only content)."

    // ── Inputs ──────────────────────────────────────────────────────────
    // Rule #17: Reads state.currentSong, state.color, layout.* only
    //
    // Reads:
    //   state.currentSong?.title   → song title text
    //   state.currentSong?.artist  → artist name text
    //   state.color.primary        → title color accent (optional)
    //   state.color.dominantLuminance → text brightness adaptation
    //   state.color.isDarkArtwork  → text color selection
    //   state.isPlaying            → now-playing indicator animation
    //   layout.controlsOpacity     → metadata visibility during morph
    //   layout.height              → available vertical space

    // ── Visual Personality ──────────────────────────────────────────────
    //
    // ALWAYS:
    //   - Title: headlineMedium weight, single line, TextOverflow.Ellipsis
    //   - Artist: bodyMedium weight, single line, TextOverflow.Ellipsis
    //   - Color: TextPrimary (adapts to dark/light artwork)
    //   - MaxLines = 1 for both (no multiline expansion)
    //
    // NEVER:
    //   ❌ Show more than 1 line
    //   ❌ Auto-resize font (fixed typography from theme)
    //   ❌ Animate position (slide in/out — crossfade only)
    //   ❌ Show album name in mini player (NowPlaying only)
    //   ❌ Show duration/position (that's Progress component's job)
    //   ❌ Marquee / scrolling text

    // ── Layout ─────────────────────────────────────────────────────────
    //   Collapsed: Title and artist side by side with album art
    //     [Album] Title...          [▶] [>>]
    //             Artist...
    //   Expanded: Title and artist below album art
    //     [Album Art — larger]
    //     Title...
    //     Artist...
    //
    //   Layout changes driven by layout.height and layout.albumArtSize.
    //   Component does NOT decide layout — it reads layout.* values.

    // ── Song Change Behavior ────────────────────────────────────────────
    //   Crossfade text content: AnimatedContent or Crossfade
    //   Duration: from MiniPlayerMotion
    //   No slide animation — content morphs in place

    // ── Performance Budget ──────────────────────────────────────────────
    //   Target: 0.2ms per frame (text rendering)
    //   Allocations: 0 (static text composables)
    //   Recomposition: only on song change or color change

    // ── Acceptance Tests ────────────────────────────────────────────────
    //   AT-MD-1: Shows title and artist when song loaded
    //   AT-MD-2: Empty when no song loaded
    //   AT-MD-3: Long title truncates with ellipsis
    //   AT-MD-4: Long artist truncates with ellipsis
    //   AT-MD-5: Song change crossfades text
    //   AT-MD-6: Text color adapts to artwork luminance
    //   AT-MD-7: Single line only (no wrap)
    //   AT-MD-8: Fades out during morph Phase 3 (controlsOpacity → 0)

    // ── Forbidden Things ─────────────────────────────────────────────────
    //   ❌ Multiple lines
    //   ❌ Font size animation
    //   ❌ Position animation (slide/move)
    //   ❌ Marquee / scrolling text
    //   ❌ Direct PlayerState access
    //   ❌ Hardcoded text color
    //   ❌ Business logic
}


// ─────────────────────────────────────────────────────────────────────────────────────
// B.6 MINI PLAYER CONTROLS — Music Interaction
// ─────────────────────────────────────────────────────────────────────────────────────

object MiniPlayerControlsSpec {

    // ── Identity ────────────────────────────────────────────────────────
    const val PURPOSE = "Music interaction. NOT playback logic."
    const val PERSONALITY =
        "Never pop. Never teleport. " +
        "Always pressure-based. " +
        "Bouncy on release. " +
        "Icon changes are instant. " +
        "Never disabled-looking."

    // ── Inputs ──────────────────────────────────────────────────────────
    // Rule #17: Reads state.isPlaying, state.isLoading, state.isFavorite,
    //           state.interaction.*, layout.controlsOpacity only
    //
    // Reads:
    //   state.isPlaying              → play/pause icon
    //   state.isLoading              → loading indicator
    //   state.isFavorite             → favorite icon state
    //   state.interaction.pressScale → press feedback
    //   state.interaction.pressTarget → which button is pressed
    //   state.interaction.isPressed  → press state
    //   layout.controlsOpacity       → visibility during morph (1 → 0)
    //   layout.albumArtSize          → button size scaling reference

    // ── Buttons ──────────────────────────────────────────────────────────
    //   Collapsed layout (left → right):
    //     [Album Art] [Title/Artist] [PlayPause] [SkipNext]
    //
    //   Controls:
    //     1. PlayPause — primary control, 38dp, SmallBubbleIcon style
    //     2. SkipNext — secondary control, 38dp, SmallBubbleIcon style
    //     3. (Future: SkipPrevious — visible when expanded)
    //     4. (Future: Favorite — visible when expanded)
    //
    //   Note: Collapsed Mini Player shows only 2 buttons (PlayPause + SkipNext).
    //   Expanded state shows full controls (PlayPause + Prev + Next + Favorite).

    // ── Visual Personality ──────────────────────────────────────────────
    //
    // ALWAYS:
    //   - Pressure-based scale (press → 0.92, release → 1.0)
    //   - Spring animation on release (RikoMotion.iconPress())
    //   - Icon swap is instant (no morph between play/pause icons)
    //   - Consistent with SmallBubbleIcon across the app
    //
    // NEVER:
    //   ❌ Pop appearance (no scale-in animation on first render)
    //   ❌ Teleport (no position jumps)
    //   ❌ Disabled appearance (buttons always look tappable)
    //   ❌ Ripple effect (we use scale, not Material ripple)
    //   ❌ Own press detection (interaction state from MiniPlayerState)

    // ── Output ──────────────────────────────────────────────────────────
    //   Controls EMIT actions, never EXECUTE them.
    //   onPlayPause: () → emit MiniPlayerAction.TogglePlayPause
    //   onNext: () → emit MiniPlayerAction.SkipNext
    //   onPrevious: () → emit MiniPlayerAction.SkipPrevious
    //   onFavorite: () → emit MiniPlayerAction.ToggleFavorite
    //   onHaptic: (HapticType) → emit MiniPlayerAction.HapticFeedback
    //
    //   (Rule: Mini Player is OUTPUT ONLY for actions)

    // ── Morph Behavior ──────────────────────────────────────────────────
    //   Collapsed: controlsOpacity = 1.0, fully visible
    //   Phase 3 (FADE, p 0.30 → 0.50): controlsOpacity 1.0 → 0.0
    //   Phase 4+: controlsOpacity = 0.0, invisible
    //
    //   Controls fade out BEFORE wave appears. Clean handoff.

    // ── Performance Budget ──────────────────────────────────────────────
    //   Target: 0.3ms per frame
    //   Icon rendering: Material Icons (vector, no decode)
    //   Scale animation: graphicsLayer{} (GPU)
    //   Allocations: 0

    // ── Acceptance Tests ────────────────────────────────────────────────
    //   AT-CT-1: PlayPause shows pause icon when playing
    //   AT-CT-2: PlayPause shows play icon when paused
    //   AT-CT-3: PlayPause toggles on tap
    //   AT-CT-4: SkipNext visible in collapsed state
    //   AT-CT-5: Press scales button to 0.92
    //   AT-CT-6: Release springs back with bounce
    //   AT-CT-7: Fades out during Phase 3 (controlsOpacity → 0)
    //   AT-CT-8: Loading state doesn't disable buttons
    //   AT-CT-9: Haptic fires on button press (via action emit)

    // ── Future Extension ───────────────────────────────────────────────
    //   - SkipPrevious button (expanded state)
    //   - Favorite button (expanded state)
    //   - Repeat/Shuffle indicators
    //   - Queue peek on long-press

    // ── Forbidden Things ─────────────────────────────────────────────────
    //   ❌ play() / pause() / seek() / favorite() — emit actions only
    //   ❌ Direct PlayerManager access
    //   ❌ Direct PlaybackService access
    //   ❌ Direct PlayerState access
    //   ❌ remember {} with mutable interaction state
    //   ❌ Hardcoded animation duration
    //   ❌ Own press detection logic
    //   ❌ Material ripple (use scale feedback)
    //   ❌ Business logic
}


// ─────────────────────────────────────────────────────────────────────────────────────
// B.7 MINI PLAYER PROGRESS — Playback Timeline
// ─────────────────────────────────────────────────────────────────────────────────────

object MiniPlayerProgressSpec {

    // ── Identity ────────────────────────────────────────────────────────
    const val PURPOSE = "Playback position indicator. NOT a seek bar."
    const val PERSONALITY =
        "Always smooth. Never jumpy. " +
        "Color flows with artwork. " +
        "Breathes when idle. " +
        "Progress is the only computation."

    // ── Inputs ──────────────────────────────────────────────────────────
    // Rule #17: Reads state.progress, state.color, layout.* only
    //
    // Reads:
    //   state.progress            → playback position (0.0 → 1.0)
    //   state.isPlaying           → animation state (breathing vs static)
    //   state.color.waveStart     → played color start
    //   state.color.waveEnd       → played color end
    //   state.color.primary       → accent tint
    //   layout.controlsOpacity     → visibility during morph
    //   layout.height             → available height (4dp strip)

    // ── Visual Rendering ────────────────────────────────────────────────
    //   Collapsed: thin waveform strip at bottom of surface (4dp height)
    //     - 40 bars representing audio envelope
    //     - Played portion: gradient(waveStart → waveEnd)
    //     - Unplayed portion: TextPrimary.copy(alpha = 0.15)
    //     - Smooth progress tracking (not snapping)
    //
    //   This matches the existing MiniPlayer waveform strip behavior.
    //   Upgraded: colors from state.color (not hardcoded accentColor).

    // ── Morph Behavior ──────────────────────────────────────────────────
    //   Fades with controls (controlsOpacity)
    //   Collapsed: visible, 4dp strip
    //   Phase 3+: fades out with controls

    // ── Performance Budget ──────────────────────────────────────────────
    //   Target: 0.5ms per frame
    //   Canvas draws: 40 lines (bar chart)
    //   Allocations: 0 (envelope array cached)
    //   Recomposition: only on progress change

    // ── Acceptance Tests ────────────────────────────────────────────────
    //   AT-PR-1: Shows played/unplayed gradient
    //   AT-PR-2: Progress tracks smoothly (no jumps)
    //   AT-PR-3: Colors match state.color.waveStart/waveEnd
    //   AT-PR-4: Unplayed portion is low-alpha
    //   AT-PR-5: Fades out during morph Phase 3
    //   AT-PR-6: 4dp height in collapsed state

    // ── Future Extension ───────────────────────────────────────────────
    //   - Seekable progress (tap to seek) in expanded state
    //   - Time display (current / total)
    //   - Buffer indicator

    // ── Forbidden Things ───────────────────────────────────────────────
    //   ❌ seek() / playback control — display only (collapsed)
    //   ❌ Direct PlayerState access
    //   ❌ Hardcoded color (use state.color)
    //   ❌ remember {} with mutable envelope state
    //   ❌ Per-frame envelope computation (cache in remember)
    //   ❌ Business logic
}


// ─────────────────────────────────────────────────────────────────────────────────────
// B.8 MINI PLAYER SHADOW — External Depth Layer
// ─────────────────────────────────────────────────────────────────────────────────────

object MiniPlayerShadowSpec {

    // ── Identity ────────────────────────────────────────────────────────
    const val PURPOSE = "External depth layer. Grounds the Mini Experience Surface."
    const val PERSONALITY = "Always below surface. Energy-reactive. Never harsh."

    // ── Inputs ──────────────────────────────────────────────────────────
    //   state.color.shadowColor     → shadow tint color
    //   state.color.energyIntensity → shadow intensity modulation
    //   state.color.ambientGlow     → glow color
    //   layout.shadowElevation      → elevation in dp
    //   layout.glowRadius           → glow radius
    //   layout.glowIntensity        → glow brightness
    //   layout.glassShadowAlpha     → shadow alpha

    // ── Implementation ──────────────────────────────────────────────────
    //   Delegates to GlassSurface's shadow system.
    //   MiniPlayerShadow is a conceptual layer — actual shadow rendering
    //   happens in MiniPlayerSurface via GlassSurface parameters.
    //
    //   Shadow is NOT a separate composable. It's a configuration of Surface.
    //   This spec exists to define shadow behavior clearly.

    // ── Forbidden Things ─────────────────────────────────────────────────
    //   ❌ Own shadow calculation — use layout.shadowElevation
    //   ❌ Hardcoded shadow color — use state.color.shadowColor
    //   ❌ Hardcoded elevation — use layout.shadowElevation
}


// ─────────────────────────────────────────────────────────────────────────────────────
// B.9 MINI PLAYER GESTURE — Touch Interaction Layer
// ─────────────────────────────────────────────────────────────────────────────────────

object MiniPlayerGestureSpec {

    // ── Identity ────────────────────────────────────────────────────────
    const val PURPOSE = "Touch input recognition and event dispatch."
    const val PERSONALITY =
        "Invisible layer. " +
        "Direction-locked gestures. " +
        "Priority-based handling. " +
        "Never intercepts what it shouldn't."

    // ── Inputs ──────────────────────────────────────────────────────────
    //   state.gesture.isDirectionLocked → gesture state
    //   state.gesture.direction         → locked direction
    //   state.gesture.isActive          → gesture in progress
    //   state.gesture.accumulatedDragY → vertical drag
    //   state.morph.morphProgress       → morph state
    //   state.morph.isDragging          → is user dragging
    //   onAction: (MiniPlayerAction) → Unit  → action dispatcher

    // ── Gesture Priority (3-Tier) ──────────────────────────────────────
    //   Tier 1 — System: Back press, notification shade, phone call
    //   Tier 2 — Component: Button taps, long-press
    //   Tier 3 — Child: Internal scroll, drag

    // ── Recognized Gestures ─────────────────────────────────────────────
    //   1. Tap (on surface) → MiniPlayerAction.ExpandToNowPlaying
    //   2. Vertical drag up → Morph expand
    //   3. Vertical drag down → Morph contract / Dismiss
    //   4. Horizontal drag left → Skip next (if threshold crossed)
    //   5. Horizontal drag right → Skip previous (if threshold crossed)
    //   6. Long press → Queue peek
    //   7. Fling up → Commit expand
    //   8. Fling down → Commit dismiss

    // ── Direction Lock ──────────────────────────────────────────────────
    //   Gesture starts undirected. After 16dp movement:
    //   - If vertical > horizontal → lock VERTICAL
    //   - If horizontal > vertical → lock HORIZONTAL
    //   Once locked, stays locked until gesture ends.
    //   Lock threshold: 16dp

    // ── Threshold Values ───────────────────────────────────────────────
    const val DIRECTION_LOCK_THRESHOLD_DP = 16f
    const val HORIZONTAL_SKIP_THRESHOLD_DP = 120f
    const val DISMISS_THRESHOLD_DP = 200f
    const val EXPAND_FLING_VELOCITY = 800f  // pixels/second
    const val DISMISS_FLING_VELOCITY = 600f  // pixels/second
    const val LONG_PRESS_THRESHOLD_MS = 500L  // milliseconds

    // ── Commit Ratios ─────────────────────────────────────────────────
    /** Fraction of dismiss threshold for distance-based expand commit.
     *  When user drags upward past (DISMISS_THRESHOLD × this ratio),
     *  the expand commits even without fling velocity. */
    const val EXPAND_COMMIT_DISTANCE_RATIO = 0.5f

    // ── Visual Feedback ──────────────────────────────────────────────────
    //   Drag: surface moves with finger (dragOffsetY applied as translationY)
    //   Direction lock: subtle haptic (MiniPlayerAction.HapticFeedback(GESTURE_LOCK))
    //   Fling commit: haptic (MiniPlayerAction.HapticFeedback(FLING))
    //   No visual drag indicator needed — the surface IS the indicator

    // ── Forbidden Things ─────────────────────────────────────────────────
    //   ❌ Direct state mutation (emit events only)
    //   ❌ Morph progress calculation (that's Reducer's job)
    //   ❌ Spring animation (that's Reducer/ViewModel's job)
    //   ❌ Business logic
}


// ─────────────────────────────────────────────────────────────────────────────────────
// B.10 MINI PLAYER LAYOUT — Component Composition Root
// ─────────────────────────────────────────────────────────────────────────────────────

object MiniPlayerLayoutSpec {

    // ── Identity ────────────────────────────────────────────────────────
    const val PURPOSE = "Component composition root. Orders z-layers, sizes content."
    const val PERSONALITY = "Invisible organizer. Zero visual output. Pure structure."

    // ── Inputs ──────────────────────────────────────────────────────────
    //   Full MiniPlayerState (delegates to sub-components)
    //   onAction: (MiniPlayerAction) → Unit

    // ── Composition Order (back → front, matching Z-Order from A.8) ─────
    //   MiniPlayerBackground  (Z-0)
    //   GlassSurface + Shadow (Z-1, Z-2, Z-3)
    //   Noise Texture         (Z-4)
    //   MiniPlayerWave        (Z-5)
    //   MiniPlayerAlbum       (Z-6, Z-7 with reflection)
    //   MiniPlayerMetadata    (Z-8)
    //   MiniPlayerProgress    (Z-9)
    //   MiniPlayerControls    (Z-10)
    //   Gesture Overlay        (Z-11, Z-12)

    // ── Collapsed Layout ────────────────────────────────────────────────
    //   ┌─────────────────────────────────────────────────────┐
    //   │  [Album 52dp] Title...              [▶] [>>]       │
    //   │              Artist...                              │
    //   │  ▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░ (progress 4dp)      │
    //   └─────────────────────────────────────────────────────┘
    //   Total: 80dp, cornerRadius 28dp, widthFraction 0.94

    // ── Spacing ─────────────────────────────────────────────────────────
    const val HORIZONTAL_PADDING_DP = 12f
    const val CONTENT_HORIZONTAL_PADDING_DP = 10f
    const val ALBUM_METADATA_GAP_DP = 12f
    const val CONTROLS_GAP_DP = 8f
    const val PROGRESS_BOTTOM_PADDING_DP = 8f
    const val BUTTON_SIZE_DP = 38f

    // ── Morph Layout ───────────────────────────────────────────────────
    //   Morph changes layout smoothly via layout.* values.
    //   Layout does NOT compute morph — it reads layout.* values.
    //   All transitions are implicit via animated layout values.

    // ── Forbidden Things ─────────────────────────────────────────────────
    //   ❌ Own layout calculation (reads layout.* values)
    //   ❌ Direct morphProgress (reads layout.* values)
    //   ❌ Z-order violation
    //   ❌ Hardcoded spacing (use constants from this spec)
    //   ❌ Business logic
}


// ═══════════════════════════════════════════════════════════════════════════════════════
// CHAPTER C — INTERACTION GRAMMAR
//
// Mini Player-এ interaction কোনো random gesture না — এটা grammar।
// প্রতিটা interaction একটা defined sequence follow করে।
//
// Grammar Flow:
//   Hover → Press → Hold → Drag → Cancel → Release → Commit → Morph
//
// প্রতিটা step-এর exact behavior এখানে defined।
// ═══════════════════════════════════════════════════════════════════════════════════════

// ─────────────────────────────────────────────────────────────────────────────────────
// C.1 INTERACTION STATES — The 10 Visual States of Mini Player
// ─────────────────────────────────────────────────────────────────────────────────────
//
// Mini Player একটা living object। ১০টা visual state আছে।
// এটা শুধু "Collapsed" আর "Expanded" না — এটা alive।
//
// State Transitions:
//   Closed ← → Breathing ← → Beat ← → Hover ← → Press
//                                                ↓
//   Disappear ← → Handoff ← → Morph ← → Drag ← → Stretch
//
// Each state has:
//   - Trigger condition
//   - Visual response
//   - Duration/easing
//   - What it transitions to next

object MiniPlayerInteractionStates {

    // ── State 1: CLOSED ─────────────────────────────────────────────────
    // Trigger: No song playing OR app backgrounded OR screen off
    // Visual: Mini player not in composition tree (isVisible = false)
    // Duration: N/A (instant removal via AnimatedVisibility)
    // Transition: → Breathing (when song starts)
    object Closed {
        const val VISIBILITY = false
        const val MORPH_PROGRESS = 0f
    }

    // ── State 2: BREATHING ───────────────────────────────────────────────
    // Trigger: Song playing, no user interaction, idle energy
    // Visual: Subtle breathing animation on surface + album art float
    //   - Surface: no visible breathing (glass is static)
    //   - Album art: gentle vertical bob (3dp, 4000ms cycle)
    //   - Wave: breathing sine wave (low amplitude)
    //   - Background: ambient drift (14s cycle)
    // Duration: Continuous (infiniteRepeatable)
    // Transition: → Beat (when energy > 0.5), → Hover (when finger enters)
    object Breathing {
        const val ALBUM_FLOAT_AMPLITUDE_DP = 3f
        const val ALBUM_FLOAT_PERIOD_MS = 4000
        const val WAVE_BREATH_AMPLITUDE = 0.1f
    }

    // ── State 3: BEAT ──────────────────────────────────────────────────
    // Trigger: state.wave.hasBeat == true
    // Visual: Beat reaction across components
    //   - Album art: scale pulse 1.0 → 1.02 → 1.0 (200ms spring)
    //   - Wave: amplitude spike
    //   - Background: alpha spike
    //   - Glow: brief intensification
    // Duration: 200ms per beat (spring-based, not fixed)
    // Transition: → Breathing (after beat settles), → Beat (next beat)
    object Beat {
        const val ALBUM_SCALE_PEAK = 1.02f
        const val ALBUM_PULSE_DURATION_MS = 200
    }

    // ── State 4: HOVER ─────────────────────────────────────────────────
    // Trigger: Finger enters mini player bounds (pointer enters)
    // Visual: Very subtle lightening of surface
    //   - Surface: glass tint alpha increases by 0.02
    //   - Controls: slight scale anticipation (1.0 → 1.01)
    // Duration: 100ms (RikoMotion.fast() — SM-UI-SPEC-001 §5.1 duration-fast)
    // Transition: → Press (on pointer down), → Breathing (on pointer exit)
    object Hover {
        const val TINT_ALPHA_BOOST = 0.02f
        const val ANTICIPATION_SCALE = 1.01f
    }

    // ── State 5: PRESS ──────────────────────────────────────────────────
    // Trigger: Finger down on surface (not on button)
    // Visual:
    //   - Surface: scale 1.0 → 0.97 (pressed feel)
    //   - Interaction: pressScale = 0.92
    //   - Haptic: HapticType.TAP queued
    //   - Breathing: paused (breathPaused = true)
    // Duration: Instant on press, spring release on lift
    // Transition: → Drag (on horizontal/vertical move), → Breathing (on release)
    object Press {
        const val SURFACE_PRESS_SCALE = 0.97f
        const val BUTTON_PRESS_SCALE = 0.92f
    }

    // ── State 6: DRAG ───────────────────────────────────────────────────
    // Trigger: Finger moves past direction lock threshold (16dp)
    // Visual:
    //   - Surface: translates with finger (translationY = dragOffsetY)
    //   - Corner radius: follows layout.cornerRadius (28dp → dynamic)
    //   - Height: follows layout.height (80dp → dynamic)
    //   - Width: follows layout.widthFraction
    //   - Breathing: paused
    //   - Controls: follow layout.controlsOpacity
    // Duration: Real-time tracking (no animation delay)
    // Transition: → Stretch (if vertical, morph), → Cancel (if system interrupt)
    object Drag {
        // Thresholds from MiniPlayerGestureSpec
    }

    // ── State 7: STRETCH ───────────────────────────────────────────────
    // Trigger: Vertical drag with morphProgress 0.00 → 0.15
    // Visual: Surface begins physical stretching
    //   - Height: 80dp → 120dp (Phase 1: STRETCH)
    //   - Width: 0.94 → 0.96 (slight widening)
    //   - Corner radius: 28dp → 22dp (softening)
    //   - Glass: still visible
    // Duration: Matches drag velocity (user-controlled)
    // Transition: → Morph (past Phase 1), → Breathing (on release below threshold)
    object Stretch {
        // Phase 1 boundaries from contract
    }

    // ── State 8: MORPH ──────────────────────────────────────────────────
    // Trigger: morphProgress 0.15 → 0.85
    // Visual: Full morph pipeline (Phase 2-5)
    //   Phase 2 (SCALE): Album grows, text grows
    //   Phase 3 (FADE): Controls fade out
    //   Phase 4 (WAVE): Wave ribbon appears
    //   Phase 5 (ALBUM): Album floats with shadow/reflection
    // Duration: ~400-600ms (user drag speed or spring settle)
    // Transition: → Handoff (past Phase 5), → Breathing (on cancel/spring back)
    object Morph {
        // Phase boundaries from MiniPlayerMorphState.MorphPhase
    }

    // ── State 9: HANDOFF ────────────────────────────────────────────────
    // Trigger: morphProgress 0.85 → 1.00
    // Visual: Crossfade to NowPlaying
    //   - MiniPlayer opacity: 1.0 → 0.0
    //   - NowPlaying opacity: 0.0 → 1.0
    //   - Both composables in tree simultaneously
    //   - Owner: MorphOwner.SHARED → MorphOwner.NOW_PLAYING
    // Duration: ~200ms (fast crossfade)
    // Transition: → Closed (when opacity = 0, remove from composition)
    object Handoff {
        const val CROSSFADE_START = 0.85f
        const val CROSSFADE_END = 1.0f
    }

    // ── State 10: DISAPPEAR ──────────────────────────────────────────────
    // Trigger: Dismiss gesture (downward fling past threshold)
    // Visual:
    //   - Surface: slides down + fades out
    //   - Scale: 1.0 → 0.95 (shrinking away)
    //   - Opacity: 1.0 → 0.0
    // Duration: 200ms (RikoMotion.normal() — SM-UI-SPEC-001 §5.1 duration-base)
    // Transition: → Closed (when opacity = 0)
    object Disappear {
        const val DISMISS_SCALE = 0.95f
    }
}


// ─────────────────────────────────────────────────────────────────────────────────────
// C.2 INTERACTION GRAMMAR FLOW — Unified Interaction Language
// ─────────────────────────────────────────────────────────────────────────────────────
//
// Meaning:
//   same interaction language everywhere.
//   Hover, Press, Hold, Drag, Cancel, Release, Commit, Morph
//   এই ধাপগুলো সব interaction-এ same — button, surface, gesture, সবখানে।

object MiniPlayerInteractionGrammar {

    // ── STEP 1: HOVER ────────────────────────────────────────────────────
    // Definition: Pointer enters interactive bounds (no contact)
    // Response: Subtle anticipation (tint boost, scale 1.01)
    // Duration: 150ms
    // Cancel: → Breathing (pointer exits)

    // ── STEP 2: PRESS ───────────────────────────────────────────────────
    // Definition: Pointer down (finger touches screen)
    // Response: Scale down + haptic queue + breathing pause
    // Duration: Instant
    // Cancel: → Drag (move), → Release (lift)

    // ── STEP 3: HOLD ───────────────────────────────────────────────────
    // Definition: Press held > 500ms without movement
    // Response: Long-press trigger (queue peek)
    // Duration: 500ms threshold
    // Cancel: → Drag (move), → Release (lift)

    // ── STEP 4: DRAG ───────────────────────────────────────────────────
    // Definition: Press + movement past direction lock threshold
    // Response: Surface follows finger, direction locked
    // Duration: Real-time
    // Cancel: → Cancel (system interrupt)

    // ── STEP 5: CANCEL ──────────────────────────────────────────────────
    // Definition: System interrupt during gesture (phone call, notification)
    // Response: Spring back to pre-gesture state
    //   - If morph was in progress → spring to collapsed
    //   - If horizontal drag → spring to center
    //   - Haptic: HapticType.TAP (subtle cancel feedback)
    // Duration: Spring-based (RikoMotion.SoftSpring)

    // ── STEP 6: RELEASE ─────────────────────────────────────────────────
    // Definition: Finger lifts from screen
    // Response: Spring settle or fling commit
    //   - If velocity > threshold → FLING (commit)
    //   - If velocity < threshold → SPRING BACK (cancel)
    //   - Release scale animation on buttons
    // Duration: Spring-based

    // ── STEP 7: COMMIT ─────────────────────────────────────────────────
    // Definition: Gesture committed (fling or threshold crossed)
    // Response: Execute the gesture's intent
    //   - Expand fling → Morph to NowPlaying
    //   - Dismiss fling → Disappear animation
    //   - Horizontal skip → emit SkipNext/SkipPrevious action
    //   - Tap → emit ExpandToNowPlaying action
    // Duration: Varies by action type

    // ── STEP 8: MORPH ──────────────────────────────────────────────────
    // Definition: Visual transition between collapsed and expanded
    // Response: 6-phase pipeline (STRETCH → SCALE → FADE → WAVE → ALBUM → HANDOFF)
    // Duration: 400-600ms (drag) or spring settle (tap)
    // Final: → Handoff (crossfade to NowPlaying)
}


// ─────────────────────────────────────────────────────────────────────────────────────
// C.3 ANIMATION INTERRUPTION POLICY — What Happens When Animations Collide
// ─────────────────────────────────────────────────────────────────────────────────────
//
// When a new interaction starts during an ongoing animation,
// the system must resolve the conflict. These are the rules.

object MiniPlayerInterruptionPolicy {

    // ── Policy Matrix ────────────────────────────────────────────────────
    // Current State | New Event | Policy
    // ──────────────────────────────────────────────────────────────────────
    // Breathing    | Tap        → INTERRUPT → Press (breathing stops)
    // Breathing    | Beat       → STACK → Breathing + Beat (both active)
    // Breathing    | Drag       → INTERRUPT → Press → Drag
    // Beat         | Tap        → INTERRUPT → Press
    // Beat         | Drag       → INTERRUPT → Press → Drag
    // Hover        | Press      → CONTINUE → Press (hover is press precursor)
    // Press        | Drag       → CONTINUE → Drag (press becomes drag)
    // Press        | Release    → CONTINUE → Breathing (spring release)
    // Drag         | Cancel     → INTERRUPT → Spring Back
    // Drag         | Release    → EVALUATE → Commit or Spring Back
    // Morph        | Cancel     → INTERRUPT → Spring Back to Collapsed
    // Morph        | Release    → EVALUATE → Continue or Spring Back
    // Morph        | BackPress  → INTERRUPT → REVERSE → Spring to Collapsed
    // Handoff      | Cancel     → INTERRUPT → REVERSE → Spring to Collapsed
    // Disappear    | Any        → IGNORE → Disappear completes

    // ── Resolution Rules ────────────────────────────────────────────────
    //
    // INTERRUPT: Current animation stops. New state takes over.
    //   Spring velocity preserved from interrupted animation.
    //
    // STACK: Both states coexist (e.g., breathing + beat).
    //   Neither cancels the other. They layer.
    //
    // CONTINUE: Natural progression (e.g., hover → press → drag).
    //   No animation reset needed.
    //
    // EVALUATE: System decides based on thresholds.
    //   E.g., drag release: velocity > threshold → commit, else spring back.
    //
    // REVERSE: Animation reverses with current velocity.
    //   Used for back-press during morph — morph springs back to collapsed.
    //
    // IGNORE: Current animation completes regardless.
    //   Used for dismiss — once disappearing, nothing can stop it.

    // ── Interrupt Types (from MiniPlayerContract Part K) ───────────────
    enum class InterruptType {
        SYSTEM_INTERRUPT,      // Phone call, notification shade
        GESTURE_CANCEL,        // Parent scroll intercepts
        CONFIG_CHANGE,         // Rotation, multi-window
        MEMORY_PRESSURE,       // onTrimMemory
        NAVIGATION_CHANGE,     // Route change during morph
        BACK_PRESS,            // System back during morph
        SCREEN_OFF,            // Screen turns off
        APP_BACKGROUND         // App moves to background
    }
}


// ═══════════════════════════════════════════════════════════════════════════════════════
// CHAPTER D — COMPOSITION
//
// Mini Player-এর layout, z-order, spacing, safe area, adaptive sizing.
// এটা structural spec — components এই structure follow করবে।
// ═══════════════════════════════════════════════════════════════════════════════════════

// ─────────────────────────────────────────────────────────────────────────────────────
// D.1 Z-ORDER — Layer Stacking
// ─────────────────────────────────────────────────────────────────────────────────────
//
// Defined in Chapter A.8 (MiniPlayerVisualDepth.ZLayer).
// Enforced by MiniPlayerLayout composition order.
//
// Back → Front:
//   Z-0:  Background
//   Z-1:  Shadow
//   Z-2:  Glow
//   Z-3:  Surface Body
//   Z-4:  Noise Texture
//   Z-5:  Wave Ribbon
//   Z-6:  Album Art
//   Z-7:  Album Reflection
//   Z-8:  Metadata
//   Z-9:  Progress
//   Z-10: Controls
//   Z-11: Press Feedback
//   Z-12: Gesture Overlay

// ─────────────────────────────────────────────────────────────────────────────────────
// D.2 SPACING SYSTEM — Consistent Gaps and Padding
// ─────────────────────────────────────────────────────────────────────────────────────

object MiniPlayerSpacing {

    // ── External Spacing (MiniPlayerSurface to screen edge) ─────────────
    /** Horizontal padding between mini player and screen edge (collapsed). */
    const val SURFACE_HORIZONTAL_PADDING_DP = 12f

    /** Bottom padding above navigation bar. */
    const val SURFACE_BOTTOM_PADDING_DP = 8f

    // ── Internal Spacing (within surface) ────────────────────────────────
    /** Horizontal padding inside surface. */
    const val CONTENT_PADDING_HORIZONTAL_DP = 10f

    /** Gap between album art and metadata text. */
    const val ALBUM_METADATA_GAP_DP = 12f

    /** Gap between control buttons. */
    const val CONTROLS_GAP_DP = 8f

    /** Gap between metadata and controls (right side). */
    const val METADATA_CONTROLS_GAP_DP = 8f

    /** Bottom padding below progress strip. */
    const val PROGRESS_BOTTOM_PADDING_DP = 8f

    /** Top padding above first row (album + metadata). */
    const val CONTENT_TOP_PADDING_DP = 0f

    // ── Control Dimensions ───────────────────────────────────────────────
    /** Play/Pause button size. */
    const val PLAY_PAUSE_SIZE_DP = 38f

    /** Skip Next/Previous button size. */
    const val SKIP_BUTTON_SIZE_DP = 38f

    /** Icon size inside buttons. */
    const val ICON_SIZE_DP = 20f

    // ── Progress Strip ──────────────────────────────────────────────────
    /** Waveform strip height. */
    const val PROGRESS_HEIGHT_DP = 4f

    /** Waveform strip horizontal padding. */
    const val PROGRESS_HORIZONTAL_PADDING_DP = 14f

    /** Number of waveform bars. */
    const val PROGRESS_BAR_COUNT = 40
}


// ─────────────────────────────────────────────────────────────────────────────────────
// D.3 SAFE AREA — System Bar Insets
// ─────────────────────────────────────────────────────────────────────────────────────

object MiniPlayerSafeArea {

    // Mini Player must respect system insets:
    //   - Navigation bar (bottom)
    //   - Gesture navigation (bottom edge)
    //   - Display cutout (top, if applicable in landscape)
    //
    // Implementation:
    //   MiniPlayerLayout reads WindowInsets
    //   Surface bottom = navigation bar top - 8dp padding
    //   In gesture navigation: extra 8dp bottom padding for swipe area
    //
    // Safe area is handled by the PARENT layout, not by MiniPlayer itself.
    // MiniPlayer renders at its allocated size — parent handles positioning.

    /** Extra bottom padding for gesture navigation mode. */
    const val GESTURE_NAV_EXTRA_PADDING_DP = 8f

    /** Minimum distance from screen bottom edge. */
    const val MIN_BOTTOM_MARGIN_DP = 8f
}


// ─────────────────────────────────────────────────────────────────────────────────────
// D.4 ALIGNMENT — Component Positioning Rules
// ─────────────────────────────────────────────────────────────────────────────────────

object MiniPlayerAlignment {

    // ── Collapsed State ─────────────────────────────────────────────────
    // Row layout (horizontal):
    //   [Album Art] --12dp-- [Metadata Column] --8dp-- [Controls Row]
    //
    // Album Art: Start-aligned (left), vertically centered
    // Metadata: Start-aligned, vertically centered
    //   Title: top of column
    //   Artist: bottom of column
    // Controls: End-aligned (right), vertically centered
    //   PlayPause: rightmost
    //   SkipNext: left of PlayPause
    //
    // Progress strip: full width, bottom-aligned with 8dp padding

    // ── Expanded State ─────────────────────────────────────────────────
    // Column layout (vertical):
    //   [Album Art — centered, larger]
    //   [Metadata — centered, below album]
    //   [Controls — centered, below metadata (if visible)]
    //   [Wave — centered, full width]
    //
    // Album Art: centered horizontally, positioned in upper portion
    // Metadata: centered, below album art
    // Controls: centered, below metadata (only before Phase 3 fade)

    // ── Gravity Rules ────────────────────────────────────────────────────
    // Surface: center horizontally, bottom-aligned (above nav bar)
    // Content: start-aligned collapsed, center-aligned expanded
    // Progress: always fill-width within surface
}


// ─────────────────────────────────────────────────────────────────────────────────────
// D.5 ADAPTIVE SIZING — Width, Height, and Density
// ─────────────────────────────────────────────────────────────────────────────────────

object MiniPlayerAdaptiveSizing {

    // ── Width Behavior ──────────────────────────────────────────────────
    // Collapsed: 94% of screen width (layout.widthFraction = 0.94)
    // Expanded: 100% of screen width (layout.widthFraction = 1.0)
    // Transition: linear interpolation via morphProgress
    //
    // Minimum width: 280dp (never narrower, regardless of screen)
    // Maximum width: screen width (in landscape, this matters)

    /** Minimum mini player width in dp. */
    const val MIN_WIDTH_DP = 280f

    // ── Height Behavior ─────────────────────────────────────────────────
    // Collapsed: 80dp (fixed, not adaptive)
    // Expanded: 240dp (fixed, not adaptive)
    // Transition: via MiniPlayerMorphSpec.heightAt(progress)
    //
    // In landscape: expanded height = screen height × 0.6 (capped at 240dp)
    // On tablet: expanded height = 280dp (larger due to bigger screen)

    /** Expanded height on tablets in dp. */
    const val EXPANDED_HEIGHT_TABLET_DP = 280f

    /** Expanded height in landscape (fraction of screen height). */
    const val EXPANDED_HEIGHT_LANDSCAPE_FRACTION = 0.6f

    // ── Density Scaling ─────────────────────────────────────────────────
    // All dp values scale automatically with display density.
    // No manual density calculation needed — Compose handles this.
    //
    // Font scaling: follows system font size settings
    // Album art: fixed dp, not affected by font scale
    // Controls: fixed dp, not affected by font scale

    // ── Responsive Breakpoints ──────────────────────────────────────────
    // Phone portrait: default specs (this document)
    // Phone landscape: expanded height capped at 60% screen
    // Tablet portrait: expanded height = 280dp
    // Tablet landscape: expanded height = 60% screen, max 320dp
    // Foldable (inner screen): use tablet specs

    enum class DeviceProfile {
        PHONE_PORTRAIT,
        PHONE_LANDSCAPE,
        TABLET_PORTRAIT,
        TABLET_LANDSCAPE,
        FOLDABLE_INNER
    }
}


// ═══════════════════════════════════════════════════════════════════════════════════════
// CHAPTER E — PERFORMANCE CONSTITUTION
//
// Not optimization. Constitution.
// Every frame must follow these rules. No exceptions.
// ═══════════════════════════════════════════════════════════════════════════════════════

object MiniPlayerPerformanceConstitution {

    // ── E.1 PER-FRAME DISCIPLINE ─────────────────────────────────────────
    //
    // Every frame:
    //   ↓
    //   0 allocation
    //   ↓
    //   0 bitmap creation
    //   ↓
    //   0 object creation
    //   ↓
    //   Reuse Path
    //   ↓
    //   Reuse Brush
    //   ↓
    //   Reuse Matrix
    //   ↓
    //   0 GC pressure
    //   ↓
    //   16ms budget respected

    // ── E.2 REUSE RULES ──────────────────────────────────────────────────
    //
    // Path:
    //   Create once in remember {}.
    //   Reuse every frame.
    //   Never create Path() inside drawScope.
    //   Wave ribbon: pre-allocate path with 40 points.
    //
    // Brush:
    //   Create once in remember {}.
    //   Update colors only (not new instances).
    //   Use Brush.verticalGradient / radialGradient with cached color arrays.
    //
    // Paint:
    //   Create once in remember {}.
    //   Update color/alpha/shadowLayer only.
    //   Never new Paint() inside drawScope.
    //
    // Matrix:
    //   Create once in remember {}.
    //   Apply transforms via reset/translate/scale.
    //   Never Matrix() inside drawScope.

    // ── E.3 ALLOCATION BUDGET ────────────────────────────────────────────
    //
    // Per-frame target: 0 bytes allocated
    // Tolerance: < 32 bytes (occasional float boxing)
    //
    // Allowed allocations:
    //   - Primitive float/int (unavoidable in some API calls)
    //   - Enum valueOf (cached by JVM)
    //
    // Forbidden allocations per frame:
    //   - Any data class instantiation
    //   - Any list creation
    //   - Any array creation (use pre-allocated)
    //   - Any string creation (use string resources)
    //   - Any lambda creation (use remembered lambdas)
    //   - Any Bitmap creation
    //   - Any Path/Brush/Paint/Matrix creation

    // ── E.4 FRAME TIME BUDGET ──────────────────────────────────────────
    //
    // Total Mini Player frame budget: 3.5ms (of 16ms)
    //   Background: 0.5ms
    //   Surface (GlassSurface): 0.3ms
    //   Noise: 0.1ms
    //   Wave: 0.8ms
    //   Album: 1.0ms (includes image decode, but Coil caches)
    //   Metadata: 0.2ms
    //   Progress: 0.5ms
    //   Controls: 0.3ms
    //   Gesture: 0.1ms
    //   ─────────────────
    //   Total: 3.8ms → budget: 3.5ms (occasional 4ms OK on 90Hz+)
    //
    // Note: These are targets, not hard limits. The actual rendering
    // pipeline may vary. The key discipline is ZERO allocation per frame.

    // ── E.5 RECOMPOSITION BUDGET ────────────────────────────────────────
    //
    // MiniPlayerState is a single data class.
    // When ANY field changes → full recomposition.
    // This is acceptable because:
    //   1. All rendering is in Canvas (cheap)
    //   2. No expensive subtree (no LazyColumn, no LazyRow)
    //   3. Coil AsyncImage caches (no re-decode on state change)
    //   4. Wave is simplified (not full Wave Engine)
    //
    // Optimization strategy:
    //   - Wave state changes at 60-144Hz → Canvas draws only (no recomposition)
    //   - Color state changes rarely (song change) → animateColorAsState
    //   - Morph state changes during drag → layout changes recompose
    //   - Playback state changes at 1Hz (progress) → progress Canvas only
    //
    // Key: Most per-frame changes only affect Canvas draws,
    // not Compose subtree recomposition.

    // ── E.6 DEVICE TIER ADAPTATION ──────────────────────────────────────
    //
    // DeviceTier determines rendering quality:
    //
    // Tier 1 (Low-end):
    //   - No noise texture
    //   - No specular highlight
    //   - Simplified wave (5 bars instead of 40)
    //   - No ambient glow
    //   - No reflection
    //   - Target: 60Hz
    //
    // Tier 2 (Mid-range):
    //   - Noise texture
    //   - Specular highlight
    //   - Full wave (40 bars)
    //   - Ambient glow (simplified)
    //   - No reflection
    //   - Target: 90Hz
    //
    // Tier 3 (High-end):
    //   - Full rendering (all features)
    //   - Noise + specular + glow + reflection
    //   - Full wave
    //   - Device tilt on album art
    //   - Target: 120Hz
    //
    // Tier 4 (Flagship):
    //   - Everything in Tier 3
    //   - Album art blur backdrop (Android S+)
    //   - Full Wave Engine geometry
    //   - Target: 144Hz

    enum class DeviceTier {
        TIER_1_LOW_END,
        TIER_2_MID_RANGE,
        TIER_3_HIGH_END,
        TIER_4_FLAGSHIP
    }

    data class TierFeatureSet(
        val hasNoise: Boolean = true,
        val hasSpecular: Boolean = true,
        val hasGlow: Boolean = true,
        val hasReflection: Boolean = true,
        val hasDeviceTilt: Boolean = false,
        val hasAlbumBlur: Boolean = false,
        val hasFullWaveGeometry: Boolean = false,
        val waveBarCount: Int = 40,
        val targetRefreshRateHz: Int = 90
    ) {
        companion object {
            fun forTier(tier: DeviceTier): TierFeatureSet = when (tier) {
                DeviceTier.TIER_1_LOW_END -> TierFeatureSet(
                    hasNoise = false,
                    hasSpecular = false,
                    hasGlow = false,
                    hasReflection = false,
                    waveBarCount = 5,
                    targetRefreshRateHz = 60
                )
                DeviceTier.TIER_2_MID_RANGE -> TierFeatureSet(
                    hasNoise = true,
                    hasSpecular = true,
                    hasGlow = true,
                    hasReflection = false,
                    waveBarCount = 40,
                    targetRefreshRateHz = 90
                )
                DeviceTier.TIER_3_HIGH_END -> TierFeatureSet(
                    hasNoise = true,
                    hasSpecular = true,
                    hasGlow = true,
                    hasReflection = true,
                    hasDeviceTilt = true,
                    waveBarCount = 40,
                    targetRefreshRateHz = 120
                )
                DeviceTier.TIER_4_FLAGSHIP -> TierFeatureSet(
                    hasNoise = true,
                    hasSpecular = true,
                    hasGlow = true,
                    hasReflection = true,
                    hasDeviceTilt = true,
                    hasAlbumBlur = true,
                    hasFullWaveGeometry = true,
                    waveBarCount = 40,
                    targetRefreshRateHz = 144
                )
            }
        }
    }

    // ── E.7 MEMORY BUDGET ───────────────────────────────────────────────
    //
    // Mini Player total memory: < 2MB
    //   State objects: < 10KB (single data class)
    //   Cached noise texture: ~128KB (128×128 ARGB)
    //   Cached paths/paints: < 5KB
    //   Coil image cache: shared (not counted)
    //
    // No bitmap pooling needed — Mini Player doesn't create bitmaps.
    // No object pooling needed — no per-frame objects.
}


// ═══════════════════════════════════════════════════════════════════════════════════════
// CHAPTER F — RENDERING DEPENDENCY MATRIX + RULES REGISTRY
//
// Future contributor-এর জন্য gold.
// প্রতিটা component কী পড়ে, কী কখনো পড়ে না — একনজাক্টে দেখা যায়।
// ═══════════════════════════════════════════════════════════════════════════════════════

// ─────────────────────────────────────────────────────────────────────────────────────
// F.1 RENDERING DEPENDENCY MATRIX
// ─────────────────────────────────────────────────────────────────────────────────────
//
// Component        | Reads                      | Never Reads
// ───────────────────────────────────────────────────────────────────────────────────
// Background       | state.color                | PlayerState, WaveState, AudioState
// Surface          | state.color, layout.*      | PlayerState, WaveState
// Album            | state.currentSong,         | ColorState, WaveState, AudioState
//                  | state.albumArtUri,          |
//                  | state.color, layout.*       |
// Wave             | state.wave, state.color,   | PlayerState, AudioState
//                  | layout.*                    |
// Metadata         | state.currentSong,          | ColorState (for rendering),
//                  | state.color.luminance,      | WaveState, AudioState
//                  | layout.*                    |
// Controls         | state.isPlaying,            | PlayerState, ColorState,
//                  | state.isLoading,             | WaveState, AudioState
//                  | state.interaction.*,        |
//                  | layout.*                    |
// Progress         | state.progress,             | PlayerState, WaveState,
//                  | state.color, layout.*       | AudioState
// Gesture          | state.gesture, state.morph, | ColorState, WaveState
//                  | layout.*                    |

object MiniPlayerDependencyMatrix {

    // ── Component → State Field Mapping ──────────────────────────────────
    // This is the definitive source of truth for what each component reads.
    // If it's not listed here, the component MUST NOT read it.

    data class ComponentDependency(
        val componentName: String,
        val readsStateColor: Boolean = false,
        val readsStateWave: Boolean = false,
        val readsStateMorph: Boolean = false,
        val readsStateGesture: Boolean = false,
        val readsStateInteraction: Boolean = false,
        val readsStateLayout: Boolean = false,
        val readsCurrentSong: Boolean = false,
        val readsPlaybackState: Boolean = false,
        val readsProgress: Boolean = false,
        val neverReads: List<String> = emptyList()
    )

    val dependencies = listOf(
        ComponentDependency(
            componentName = "MiniPlayerBackground",
            readsStateColor = true,
            readsStateLayout = true,
            neverReads = listOf("PlayerState", "WaveState", "AudioState", "PlayerManager")
        ),
        ComponentDependency(
            componentName = "MiniPlayerSurface",
            readsStateColor = true,
            readsStateLayout = true,
            neverReads = listOf("PlayerState", "WaveState", "AudioState")
        ),
        ComponentDependency(
            componentName = "MiniPlayerAlbum",
            readsStateColor = true,
            readsStateWave = true,
            readsStateInteraction = true,
            readsStateLayout = true,
            readsCurrentSong = true,
            neverReads = listOf("ColorState (raw)", "WaveState (raw)", "AudioState")
        ),
        ComponentDependency(
            componentName = "MiniPlayerWave",
            readsStateColor = true,
            readsStateWave = true,
            readsStateLayout = true,
            readsPlaybackState = true,
            neverReads = listOf("PlayerState (raw)", "AudioState", "PCM", "FFT")
        ),
        ComponentDependency(
            componentName = "MiniPlayerMetadata",
            readsStateColor = true,
            readsStateLayout = true,
            readsCurrentSong = true,
            readsPlaybackState = true,
            neverReads = listOf("ColorState (raw)", "WaveState", "AudioState")
        ),
        ComponentDependency(
            componentName = "MiniPlayerControls",
            readsStateInteraction = true,
            readsStateLayout = true,
            readsPlaybackState = true,
            neverReads = listOf("PlayerState (raw)", "ColorState", "WaveState",
                "AudioState", "PlayerManager", "PlaybackService")
        ),
        ComponentDependency(
            componentName = "MiniPlayerProgress",
            readsStateColor = true,
            readsStateLayout = true,
            readsProgress = true,
            readsPlaybackState = true,
            neverReads = listOf("PlayerState (raw)", "WaveState", "AudioState")
        ),
        ComponentDependency(
            componentName = "MiniPlayerGesture",
            readsStateGesture = true,
            readsStateMorph = true,
            readsStateLayout = true,
            neverReads = listOf("ColorState", "WaveState", "AudioState")
        )
    )
}


// ─────────────────────────────────────────────────────────────────────────────────────
// F.2 RULES REGISTRY — All Production Rules in One Place
// ─────────────────────────────────────────────────────────────────────────────────────

object MiniPlayerRulesRegistry {

    // ═════════════════════════════════════════════════════════════════════
    // RULE #17 — Single Snapshot Rule
    // ═════════════════════════════════════════════════════════════════════
    //
    // Every Mini Player component must render from ONE snapshot only.
    //
    // ❌ Wrong:
    //   MiniPlayerAlbum(
    //       playerState,
    //       colorState,
    //       waveState,
    //       ...
    //   )
    //
    // ✔ Correct:
    //   MiniPlayerAlbum(state.album)
    //   MiniPlayerWave(state.wave)
    //   MiniPlayerShadow(state.shadow)
    //   MiniPlayerControls(state.controls)
    //   MiniPlayerBackground(state.background)
    //
    // One component. One snapshot. One responsibility.

    const val RULE_17 = """
        Rule #17 — Single Snapshot Rule
        Every Mini Player component renders from ONE snapshot only.
        MiniPlayerAlbum receives MiniPlayerState (the full state), 
        but only reads its own fields: state.currentSong, state.albumArtUri,
        state.color, layout.albumArtSize, etc.
        Components NEVER receive multiple raw state objects.
    """

    // ═════════════════════════════════════════════════════════════════════
    // RULE #18 — Morph Source Opacity
    // ═════════════════════════════════════════════════════════════════════
    //
    // Components never know morph progress source.
    // They only receive pre-computed layout values.
    //
    // ❌ Wrong:
    //   val cornerRadius = 28f * (1f - morphProgress)
    //
    // ✔ Correct:
    //   val cornerRadius = layout.cornerRadius  // Already computed
    //
    // Components see: layout.albumSize, layout.cornerRadius,
    // layout.padding, layout.height — not morphProgress.

    const val RULE_18 = """
        Rule #18 — Morph Source Opacity
        Components never know morph progress source.
        They receive pre-computed layout values.
        layout.cornerRadius, layout.albumArtSize, layout.height,
        layout.glassTintAlpha — these are computed by MiniPlayerMapper.
        Components apply them. Zero computation.
    """

    // ═════════════════════════════════════════════════════════════════════
    // RULE #19 — No Hardcoded Duration
    // ═════════════════════════════════════════════════════════════════════
    //
    // No animation duration inside Components. Never.
    // All animation specs come from MiniPlayerMotion.
    //
    // ❌ Wrong:
    //   animateFloatAsState(
    //       animationSpec = tween(420)  // Hardcoded!
    //   )
    //
    // ✔ Correct:
    //   animateFloatAsState(
    //       animationSpec = motion.expandSpec  // From MiniPlayerMotion
    //   )
    //
    // hardcoded 420, 900, 250 — Never.

    const val RULE_19 = """
        Rule #19 — No Hardcoded Duration
        No animation duration inside Components. Never.
        All animation specs come from MiniPlayerMotion.
        Component writes: animateFloatAsState(animationSpec = motion.expandSpec)
        Hardcoded 420, 900, 250 — Never.
    """

    // ═════════════════════════════════════════════════════════════════════
    // RULE #20 — Renderer Never Interprets
    // ═════════════════════════════════════════════════════════════════════
    //
    // Renderer receives. Renderer paints. Nothing more.
    //
    // ❌ Wrong:
    //   if (energy > 0.8f) glow += ...
    //
    // ✔ Correct:
    //   glowIntensity = layout.glowIntensity  // Already solved
    //
    // Renderer doesn't think. Renderer doesn't compute.
    // Every visual value is pre-computed by Mapper or Reducer.

    const val RULE_20 = """
        Rule #20 — Renderer Never Interprets
        Renderer receives. Renderer paints. Nothing more.
        Wrong: if (energy > 0.8f) glow += ...
        Correct: glowIntensity = layout.glowIntensity
        Renderer doesn't think. Every visual value is pre-computed.
    """

    // ═════════════════════════════════════════════════════════════════════
    // RULE #21 — Every Visual Value Traceable
    // ═════════════════════════════════════════════════════════════════════
    //
    // Every visual value must be traceable to its source.
    //
    // Example:
    //   Album Shadow Alpha = 0.36
    //     ↓ trace
    //   MiniPlayerLayoutSnapshot.shadowElevation
    //     ↓ trace
    //   MiniPlayerMapper.mapLayout(morphProgress)
    //     ↓ trace
    //   MiniPlayerState.morph.morphProgress
    //     ↓ trace
    //   MiniPlayerReducer (gesture event)
    //     ↓ trace
    //   User finger position
    //
    // No magic numbers. No unexplained values.

    const val RULE_21 = """
        Rule #21 — Every Visual Value Traceable
        Every visual value must be traceable to its source.
        Album Shadow Alpha = 0.36
          → MiniPlayerLayoutSnapshot.shadowElevation
          → MiniPlayerMapper.mapLayout(morphProgress)
          → MiniPlayerState.morph.morphProgress
          → MiniPlayerReducer (gesture event)
          → User finger position
        No magic numbers. No unexplained values.
    """

    // ═════════════════════════════════════════════════════════════════════
    // RULE #22 — Every Component Replaceable in One Afternoon
    // ═════════════════════════════════════════════════════════════════════
    //
    // If MiniPlayerAlbum V3 comes tomorrow:
    //   Delete. Paste. Done.
    //   No other component breaks.
    //
    // This is possible because:
    //   1. Every component reads from MiniPlayerState (stable contract)
    //   2. Every component emits MiniPlayerAction (stable contract)
    //   3. No component reads another component's internal state
    //   4. Layout depends on component interface, not implementation

    const val RULE_22 = """
        Rule #22 — Every Component Replaceable in One Afternoon
        MiniPlayerAlbum V3: Delete. Paste. Done. No other component breaks.
        Possible because:
        1. Every component reads from MiniPlayerState (stable)
        2. Every component emits MiniPlayerAction (stable)
        3. No component reads another's internal state
        4. Layout depends on interface, not implementation
    """

    // ═════════════════════════════════════════════════════════════════════
    // RULE #23 — No UI Reads Raw Experience OS
    // ═════════════════════════════════════════════════════════════════════
    //
    // (Established in Build-2, carried forward)
    //
    // MiniPlayerAlbum → MiniPlayerState (NOT → ColorState)
    // MiniPlayerWave → MiniPlayerState (NOT → WaveState)
    // MiniPlayerControls → MiniPlayerState (NOT → PlayerState)
    //
    // Only MiniPlayerMapper touches raw Experience OS.
    // Renderer layer NEVER imports ColorState, WaveState, or PlayerState.
    // (Except MiniPlayerMapper.kt, which is in the bridge layer, not render layer.)

    const val RULE_23 = """
        Rule #23 — No UI Reads Raw Experience OS
        MiniPlayerAlbum → MiniPlayerState (NOT → ColorState)
        MiniPlayerWave → MiniPlayerState (NOT → WaveState)
        MiniPlayerControls → MiniPlayerState (NOT → PlayerState)
        Only MiniPlayerMapper touches raw Experience OS.
        Renderer layer NEVER imports ColorState, WaveState, PlayerState.
    """

    // ── All Rules Summary ───────────────────────────────────────────────
    val allRules = listOf(
        "#17" to "Single Snapshot Rule",
        "#18" to "Morph Source Opacity",
        "#19" to "No Hardcoded Duration",
        "#20" to "Renderer Never Interprets",
        "#21" to "Every Visual Value Traceable",
        "#22" to "Every Component Replaceable",
        "#23" to "No UI Reads Raw Experience OS",
        "#24" to "Pixel Ownership Rule",
        "#25" to "Layer Independence"
    )

    // ═════════════════════════════════════════════════════════════════════
    // RULE #24 — Pixel Ownership Rule
    // ═════════════════════════════════════════════════════════════════════
    //
    // Every pixel on screen has EXACTLY ONE owner.
    // No pixel is drawn by two renderers.
    //
    // Pixel Ownership Map:
    //   Album Shadow    → Owner: MiniPlayerShadow
    //   Glow            → Owner: MiniPlayerBackground
    //   Album Reflection → Owner: MiniPlayerAlbum
    //   Wave Ribbon     → Owner: MiniPlayerWave
    //   Glass Frost     → Owner: MiniPlayerSurface
    //   Progress Strip  → Owner: MiniPlayerProgress
    //   Metadata Text   → Owner: MiniPlayerMetadata
    //   Controls Icons  → Owner: MiniPlayerControls
    //   Press Feedback  → Owner: MiniPlayerControls
    //   Noise Texture   → Owner: MiniPlayerSurface
    //   Ambient Gradient → Owner: MiniPlayerBackground
    //
    // ❌ Wrong:
    //   MiniPlayerBackground draws glow. MiniPlayerWave also draws glow.
    //   Result: double alpha, double blur, overdraw, weird artifacts.
    //
    // ✔ Correct:
    //   Glow: MiniPlayerBackground ONLY.
    //   Wave: sends intensity. Background: owns glow rendering.
    //
    // This rule forever prevents the most dangerous rendering bug:
    //   same pixel, drawn by 3 renderers.

    const val RULE_24 = """
        Rule #24 — Pixel Ownership Rule
        Every pixel on screen has EXACTLY ONE owner.
        No pixel is drawn by two renderers.

        Ownership Map:
          Album Shadow     → MiniPlayerShadow
          Glow             → MiniPlayerBackground
          Album Reflection → MiniPlayerAlbum
          Wave Ribbon      → MiniPlayerWave
          Glass Frost      → MiniPlayerSurface
          Progress Strip   → MiniPlayerProgress
          Metadata Text    → MiniPlayerMetadata
          Controls Icons   → MiniPlayerControls
          Press Feedback   → MiniPlayerControls
          Noise Texture    → MiniPlayerSurface
          Ambient Gradient → MiniPlayerBackground

        This forever prevents: same pixel, drawn by 3 renderers.
    """

    // ═════════════════════════════════════════════════════════════════════
    // RULE #25 — Layer Independence
    // ═════════════════════════════════════════════════════════════════════
    //
    // Every layer MUST fail gracefully.
    // Removing any single layer must NOT break the visual.
    //
    // Wave removed → Mini Player still beautiful. Wave is enhancement.
    // Album removed → Placeholder album. Screen still premium.
    // ColorBrain fails → Fallback palette. Still premium.
    // Noise removed → No visible degradation.
    // Glow removed → Slightly less atmosphere, still premium.
    // Reflection removed → Album still looks good.
    // Shadow removed → Less depth, still functional.
    // Progress removed → Controls still work.
    //
    // This follows Apple's design philosophy:
    //   No single visual element is a hard dependency.
    //   Every element enhances, none are required.
    //   The base experience (glass + album + controls) always works.

    const val RULE_25 = """
        Rule #25 — Layer Independence
        Every layer MUST fail gracefully.
        Removing any single layer must NOT break the visual.

        Wave removed → still beautiful. Enhancement, not requirement.
        Album removed → placeholder. Still premium.
        ColorBrain fails → fallback palette. Still premium.
        Noise removed → no visible degradation.
        Glow removed → slightly less atmosphere.
        Reflection removed → album still looks good.
        Shadow removed → less depth, still functional.

        No single visual element is a hard dependency.
        Every element enhances, none are required.
    """

    // ── Pixel Ownership Map (definitive) ─────────────────────────────────
    data class PixelOwner(
        val visualElement: String,
        val ownerComponent: String,
        val neverDrawnBy: List<String>
    )

    val pixelOwnershipMap = listOf(
        PixelOwner(
            visualElement = "Album Shadow",
            ownerComponent = "MiniPlayerShadow",
            neverDrawnBy = listOf("MiniPlayerAlbum", "MiniPlayerBackground", "MiniPlayerSurface")
        ),
        PixelOwner(
            visualElement = "Ambient Glow",
            ownerComponent = "MiniPlayerBackground",
            neverDrawnBy = listOf("MiniPlayerWave", "MiniPlayerAlbum", "MiniPlayerSurface")
        ),
        PixelOwner(
            visualElement = "Album Reflection",
            ownerComponent = "MiniPlayerAlbum",
            neverDrawnBy = listOf("MiniPlayerSurface", "MiniPlayerBackground")
        ),
        PixelOwner(
            visualElement = "Wave Ribbon",
            ownerComponent = "MiniPlayerWave",
            neverDrawnBy = listOf("MiniPlayerBackground", "MiniPlayerAlbum")
        ),
        PixelOwner(
            visualElement = "Glass Frost",
            ownerComponent = "MiniPlayerSurface",
            neverDrawnBy = listOf("MiniPlayerBackground", "MiniPlayerAlbum")
        ),
        PixelOwner(
            visualElement = "Noise Texture",
            ownerComponent = "MiniPlayerSurface",
            neverDrawnBy = listOf("MiniPlayerBackground")
        ),
        PixelOwner(
            visualElement = "Progress Strip",
            ownerComponent = "MiniPlayerProgress",
            neverDrawnBy = listOf("MiniPlayerWave", "MiniPlayerControls")
        ),
        PixelOwner(
            visualElement = "Metadata Text",
            ownerComponent = "MiniPlayerMetadata",
            neverDrawnBy = listOf("MiniPlayerControls", "MiniPlayerAlbum")
        ),
        PixelOwner(
            visualElement = "Controls Icons",
            ownerComponent = "MiniPlayerControls",
            neverDrawnBy = listOf("MiniPlayerAlbum", "MiniPlayerProgress")
        ),
        PixelOwner(
            visualElement = "Press Feedback",
            ownerComponent = "MiniPlayerControls",
            neverDrawnBy = listOf("MiniPlayerAlbum", "MiniPlayerSurface")
        )
    )

    // ── Layer Independence Matrix ──────────────────────────────────────
    data class LayerIndependence(
        val layerName: String,
        val isEnhancement: Boolean = true,
        val fallbackBehavior: String
    )

    val layerIndependenceMatrix = listOf(
        LayerIndependence(
            layerName = "MiniPlayerWave",
            fallbackBehavior = "No wave visible. Rest still premium."
        ),
        LayerIndependence(
            layerName = "MiniPlayerBackground",
            isEnhancement = false,
            fallbackBehavior = "Dark ambient fallback. Always visible."
        ),
        LayerIndependence(
            layerName = "MiniPlayerAlbum",
            isEnhancement = false,
            fallbackBehavior = "Gradient placeholder with music note icon."
        ),
        LayerIndependence(
            layerName = "Noise Texture",
            fallbackBehavior = "No visible difference. Skip entirely on Tier 1."
        ),
        LayerIndependence(
            layerName = "Glow",
            fallbackBehavior = "Slightly less atmosphere. No artifacts."
        ),
        LayerIndependence(
            layerName = "Reflection",
            fallbackBehavior = "Album still looks clean. No degradation."
        ),
        LayerIndependence(
            layerName = "Shadow",
            fallbackBehavior = "Less depth perception. Still functional."
        ),
        LayerIndependence(
            layerName = "Progress",
            fallbackBehavior = "No position indicator. Controls still work."
        ),
        LayerIndependence(
            layerName = "Color Intelligence",
            fallbackBehavior = "MiniPlayerColorSnapshot.FALLBACK palette used."
        )
    )
}


// ═══════════════════════════════════════════════════════════════════════════════════════
// CHAPTER F.3 — MINI PLAYER MOTION SPEC
//
// All animation specifications for Mini Player.
// This is the SINGLE source of truth for all motion.
// Rule #19: No hardcoded duration — everything comes from here.
// ═══════════════════════════════════════════════════════════════════════════════════════

/**
 * Motion specifications for Mini Player 2.0.
 *
 * Every animation in Mini Player MUST reference this object.
 * No hardcoded durations. No raw spring() calls.
 * No tween(420) or spring(dampingRatio = 0.72f) inline.
 *
 * Usage:
 *   val pressScale by animateFloatAsState(
 *       targetValue = if (isPressed) 0.92f else 1f,
 *       animationSpec = MiniPlayerMotion.pressScaleSpec
 *   )
 *
 * This object EXTENDS the app's existing RikoMotion system.
 * Mini Player uses RikoMotion for shared animations (color transitions,
 * icon press) and MiniPlayerMotion for mini-specific animations.
 */
object MiniPlayerMotion {

    // ── Press & Release ──────────────────────────────────────────────────
    /** Scale animation for surface press (1.0 → 0.97 → 1.0). */
    val surfacePressSpec: androidx.compose.animation.core.FiniteAnimationSpec<Float> =
        androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        )

    /** Scale animation for button press (1.0 → 0.92 → 1.0). */
    val pressScaleSpec: androidx.compose.animation.core.FiniteAnimationSpec<Float> =
        androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioHighBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        )

    // ── Morph Animations ────────────────────────────────────────────────
    /** Spring for morph settle after drag release. */
    val morphSettleSpec: androidx.compose.animation.core.FiniteAnimationSpec<Float> =
        androidx.compose.animation.core.spring(
            dampingRatio = 0.82f,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        )

    /** Spring for morph reverse (back press during morph). */
    val morphReverseSpec: androidx.compose.animation.core.FiniteAnimationSpec<Float> =
        androidx.compose.animation.core.spring(
            dampingRatio = 0.85f,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
        )

    /** Spring for dismiss animation (surface shrink + slide down). */
    val dismissSpec: androidx.compose.animation.core.FiniteAnimationSpec<Float> =
        androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        )

    // ── Crossfade Animations ────────────────────────────────────────────
    /** Color crossfade (song change). Uses RikoMotion.colorTransition(). */
    val colorCrossfadeSpec: androidx.compose.animation.core.FiniteAnimationSpec<androidx.compose.ui.graphics.Color> =
        RikoMotion.colorTransition()

    /** Content crossfade (text swap on song change). */
    val contentCrossfadeSpec: androidx.compose.animation.core.FiniteAnimationSpec<Float> =
        androidx.compose.animation.core.tween(
            durationMillis = 280,
            easing = RikoMotion.Standard
        )

    // ── Living Animations ────────────────────────────────────────────────
    /** Album art float (infinite breathing bob). */
    val albumFloatSpec: androidx.compose.animation.core.InfiniteRepeatableSpec<Float> =
        androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = 4000,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        )

    /** Album art idle float (slower, lower amplitude). */
    val albumIdleFloatSpec: androidx.compose.animation.core.InfiniteRepeatableSpec<Float> =
        androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = 8000,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        )

    /** Beat pulse (scale 1.0 → 1.02 → 1.0). */
    val beatPulseSpec: androidx.compose.animation.core.FiniteAnimationSpec<Float> =
        androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessHigh
        )

    // ── Ambient Animations ──────────────────────────────────────────────
    /** Background ambient drift (14s cycle, shared with NowPlaying). */
    val ambientDriftSpec: androidx.compose.animation.core.InfiniteRepeatableSpec<Float> =
        androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = 14000,
                easing = androidx.compose.animation.core.LinearEasing
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        )

    /** Background pulse breathing (6s cycle). */
    val pulseBreathingSpec: androidx.compose.animation.core.InfiniteRepeatableSpec<Float> =
        androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = 6000,
                easing = androidx.compose.animation.core.LinearEasing
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        )

    // ── Appear / Disappear ──────────────────────────────────────────────
    /** Mini player appear (slide up + fade in). */
    val appearSpec: androidx.compose.animation.core.FiniteAnimationSpec<Float> =
        RikoMotion.miniPlayerSlide()

    /** Mini player disappear (slide down + fade out). */
    val disappearSpec: androidx.compose.animation.core.FiniteAnimationSpec<Float> =
        RikoMotion.miniPlayerSlide()

    // ── Fling Decay ─────────────────────────────────────────────────────
    /** Fling momentum decay for drag release. */
    val flingDecaySpec: androidx.compose.animation.core.DecayAnimationSpec<Float> =
        RikoMotion.flingDecay()

    // ── Wave Phase ────────────────────────────────────────────────────
    val wavePhaseStart: Float = 0f
    val wavePhaseTarget: Float = (2 * PI).toFloat()
    val waveCycleDuration: Int = 4000
    val waveSecondaryCycleDuration: Int = 6000
    val waveTemporalFrequency: Float = 1.5f
    val waveSpatialFrequency: Float = 4f
    val waveIdleSpatialFrequency: Float = 2f
    val waveSegmentCount: Int = 48
    val waveMaxDisplacementRatio: Float = 0.35f
    val waveAlphaThreshold: Float = 0.01f

    // ── Wave Amplitude ────────────────────────────────────────────────
    val waveBeatAmplitude: Float = 1.5f
    val waveNormalAmplitude: Float = 1.0f
    val waveIdleAmplitudeScale: Float = 0.3f

    // ── Wave Secondary Layer ───────────────────────────────────────────
    val waveSecondaryAlphaGate: Float = 0.3f
    val waveSecondaryEnergyGate: Float = 0.2f
    val waveSecondaryAmplitudeScale: Float = 0.6f
    val waveSecondaryDisplacementRatio: Float = 0.25f
    val waveSecondaryFrequencyRatio: Float = 0.7f
    val waveSecondaryTemporalFrequency: Float = 1.0f
    val waveSecondaryPhaseOffset: Float = 0.5f
    val waveSecondaryStrokeScale: Float = 0.5f
    val waveSecondaryAlphaScale: Float = 0.4f

    // ── Progress / Breathing ───────────────────────────────────────────
    fun progressSmoothSpec() = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 200f
    )
    val breathingAlphaMin: Float = 0.65f
    val breathingCycleDurationMs: Int = 2000

    // ── Waveform Bars ──────────────────────────────────────────────────
    val waveformBarSpacing: Float = 0.5f
    val waveformBarStrokeWidth: Float = 2f
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// CHAPTER F.4 — VISUAL VALUE TRACEABILITY TABLE
//
// Example traces for key visual values.
// Demonstrates Rule #21 in practice.
// ═══════════════════════════════════════════════════════════════════════════════════════

object MiniPlayerTraceabilityTable {

    // ── Example Trace: Background Alpha ─────────────────────────────────
    //
    // Value: 0.14
    //   ↓
    // MiniPlayerBackground renders with alpha = 0.14
    //   ↓
    // alpha = BASE_ALPHA_COLLAPSED + (energyIntensity × ENERGY_MODULATION)
    //   ↓     = 0.08 + (0.5 × 0.12) = 0.14
    // BASE_ALPHA_COLLAPSED = MiniPlayerVisualBackground.BASE_ALPHA_COLLAPSED = 0.08
    // ENERGY_MODULATION = MiniPlayerVisualBackground.ENERGY_MODULATION = 0.12
    //   ↓
    // energyIntensity = state.color.energyIntensity = 0.5
    //   ↓
    // state.color = MiniPlayerColorSnapshot
    //   ↓
    // MiniPlayerMapper.mapColors(ColorState)
    //   ↓
    // ColorState.energyIntensity = 0.5
    //   ↓
    // ColorIntelligenceEngine (analyzes album art + audio energy)

    // ── Example Trace: Album Art Size ───────────────────────────────────
    //
    // Value: 120dp
    //   ↓
    // MiniPlayerAlbum renders with size = layout.albumArtSize = 120dp
    //   ↓
    // layout.albumArtSize = MiniPlayerLayoutSnapshot.albumArtSize = 120dp
    //   ↓
    // MiniPlayerMapper.mapLayout(morphProgress)
    //   ↓
    // MiniPlayerMorphSpec.albumArtSizeAt(0.50) = 120dp
    //   ↓
    // morphProgress = 0.50
    //   ↓
    // MiniPlayerReducer processed Drag event
    //   ↓
    // User dragged finger 200dp upward from bottom

    // ── Example Trace: Wave Gradient Color ─────────────────────────────
    //
    // Value: Color(0xFFE94560)
    //   ↓
    // MiniPlayerWave renders gradient start = state.color.waveStart = Color(0xFFE94560)
    //   ↓
    // state.color = MiniPlayerColorSnapshot
    //   ↓
    // MiniPlayerMapper.mapColors(ColorState).waveStart
    //   ↓
    // ColorState.waveStart = Color(0xFFE94560)
    //   ↓
    // ColorIntelligenceEngine (derived from album art dominant hue + bass warmth)
}


// ═══════════════════════════════════════════════════════════════════════════════════════
// END OF MINI PLAYER RENDERING SPECIFICATION v1.0
//
// Version: 1.0 (Final — Rules #17 through #25)
// Status: APPROVED — Product Director 9.99/10
// No changes without Product Director sign-off.
//
// Build-3 — Render Layer Implementation (4 Phases):
//
//   Phase A — Visual Foundation: Background → Surface → Shadow
//   Phase B — Identity: Album → Metadata → Wave
//   Phase C — Interaction: Controls → Progress → Gesture
//   Phase D — Composition: Layout → Performance → Integration
//
// Preparatory file: MiniPlayerTokens.kt
// ═══════════════════════════════════════════════════════════════════════════════════════
