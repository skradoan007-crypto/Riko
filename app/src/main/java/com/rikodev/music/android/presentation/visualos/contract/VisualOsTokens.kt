package com.rikodev.music.android.presentation.visualos.contract

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// RIKO VISUAL OS — VISUAL DESIGN TOKENS
//
// Intent-based design tokens for the Visual OS layer.
// Every visual value in the scene/layer system comes from here.
// No hardcoded blur, timing, scale, or spacing values in rendering code.
//
// Philosophy (from GlassTokens.kt):
//   ❌ BlurLarge, DurationFast    ← value-based, no context
//   ✅ SceneOpenBlur, PanelLift  ← intent-based, self-documenting
//
// Frozen Architecture: Contract → Runtime → Reducer → Mapper → Rendering → Integration
// ─────────────────────────────────────────────────────────────────────────────

// ═══════════════════════════════════════════════════════════════════════════════
// 1. KEYFRAME TIMING TOKENS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Keyframe timing tokens from Spec Section 7.4.
 *
 * Every transition in RIKO follows this timeline:
 *   0ms → 80ms → 140ms → 220ms → 300ms
 *
 * These are the absolute timing checkpoints. Different transition types
 * may use different subsets or interpolate between checkpoints,
 * but the checkpoints themselves are sacred.
 */
object KeyframeTimings {

    // ── Checkpoint Durations (from spec) ──────────────────────────────

    /** 0ms: Hidden / initial state. */
    const val CHECKPOINT_0 = 0

    /** 80ms: Lift starts — element begins moving from rest. */
    const val CHECKPOINT_1 = 80

    /** 140ms: Blur starts — background begins de-emphasizing. */
    const val CHECKPOINT_2 = 140

    /** 220ms: Settle begins — element decelerating toward final position. */
    const val CHECKPOINT_3 = 220

    /** 300ms: Active — transition complete, element in final state. */
    const val CHECKPOINT_4 = 300

    // ── Full Transition Durations ─────────────────────────────────────

    /** Standard scene-to-scene transition (push/pop). */
    const val SCENE_TRANSITION = 300

    /** Panel open/close (lift from bottom). */
    const val PANEL_TRANSITION = 350

    /** Overlay show/hide (brief, targeted). */
    const val OVERLAY_TRANSITION = 200

    /** Dock appear/hide (subtle). */
    const val DOCK_TRANSITION = 250

    /** Focus handoff (MiniPlayer → NowPlaying). Longer = more dramatic. */
    const val HANDOFF_TRANSITION = 400

    /** Switch between base scenes (tab-to-tab). */
    const val BASE_SWITCH_TRANSITION = 280

    // ── Phase Durations (for staggered multi-property animations) ─────

    /** Phase 1: Spatial motion (lift/scale/fade) — 0 to 180ms. */
    const val PHASE_SPATIAL = 180

    /** Phase 2: Background de-emphasis (blur/dim) — 80 to 260ms. */
    const val PHASE_BACKGROUND = 180

    /** Phase 3: Content settle — 180 to 300ms. */
    const val PHASE_SETTLE = 120

    /** Overlap between phases (both animating simultaneously). */
    const val PHASE_OVERLAP = 100

    // ── Stagger Delays (for multi-element entry) ───────────────────────

    /** Delay between sequential element entries in a scene. */
    const val ELEMENT_STAGGER = 40

    /** Delay between panel content elements appearing. */
    const val PANEL_ELEMENT_STAGGER = 30

    /** Maximum stagger cascade duration (prevents slow-feeling entries). */
    const val MAX_STAGGER_CASCADE = 300
}

// ═══════════════════════════════════════════════════════════════════════════════
// 2. BLUR TOKENS
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Blur radius tokens for different contexts.
 *
 * From Spec Section 4.2: "blur starts" at 140ms — blur is PROGRESSIVE,
 * not instant. These tokens define the TARGET blur values;
 * the animation interpolates from 0 → target over the keyframe duration.
 *
 * V1 Note: Current GlassSurface uses fake blur (gradient tint).
 * V2 will add RenderEffect-based real backdrop blur.
 * These tokens are future-proofed for real blur values.
 */
object BlurTokens {

    /** No blur — content at natural clarity. */
    val None: Dp = 0.dp

    /** Subtle blur — dock glass, tooltip background. */
    val Subtle: Dp = 8.dp

    /** Light blur — card surface, chip background. */
    val Light: Dp = 16.dp

    /** Medium blur — panel background over focus scene. */
    val Medium: Dp = 24.dp

    /** Strong blur — focus scene background when panel is open. */
    val Strong: Dp = 40.dp

    /** Heavy blur — base scene when focus scene is active. */
    val Heavy: Dp = 55.dp

    /** Maximum blur — hero backdrop (NowPlaying background). */
    val Maximum: Dp = 65.dp

    // ── Progressive Blur Steps ─────────────────────────────────────────

    /**
     * Progressive blur values for de-emphasis animation.
     * Background blur animates through these steps:
     *   0 → Subtle → Medium → Strong → Heavy
     * Each step corresponds to a keyframe checkpoint.
     */
    val deEmphasisSteps: List<Dp> = listOf(
        None,       // 0ms: no blur (initial)
        Subtle,     // 80ms: barely noticeable
        Light,      // 140ms: blur starts
        Medium,     // 220ms: settling
        Heavy       // 300ms: final de-emphasis
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// 3. OPACITY TOKENS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Opacity tokens for different visual states.
 *
 * Opacity is used for:
 *   - Scene visibility (fade in/out during transitions)
 *   - Background dimming (reducing base scene prominence)
 *   - Overlay scrim (semi-transparent barrier)
 *   - Content hierarchy (secondary content at lower opacity)
 */
object OpacityTokens {

    // ── Visibility Opacities ──────────────────────────────────────────

    /** Fully visible. */
    const val FULL = 1.0f

    /** High visibility — active scene content. */
    const val HIGH = 0.9f

    /** Medium visibility — de-emphasized scene content. */
    const val MEDIUM = 0.7f

    /** Low visibility — background scene content. */
    const val LOW = 0.4f

    /** Faint — subtle background elements. */
    const val FAINT = 0.15f

    /** Invisible — hidden state. */
    const val INVISIBLE = 0.0f

    // ── Dimming Levels ────────────────────────────────────────────────

    /**
     * How much the background dims when a layer opens above it.
     * Progressive: as the layer animates in, dim increases from 0 → target.
     */
    const val DIM_FOR_FOCUS = 0.15f     // Base dims slightly when focus scene opens
    const val DIM_FOR_PANEL = 0.25f     // Focus dims more when panel opens
    const val DIM_FOR_OVERLAY = 0.08f   // Very subtle dim for overlay

    /** Scrim opacity for full-screen overlay (context menu behind). */
    const val SCRIM_STANDARD = 0.40f

    /** Light scrim for bottom sheets. */
    const val SCRIM_LIGHT = 0.18f

    // ── Progressive Opacity Steps (for de-emphasis animation) ──────────

    val deEmphasisSteps: List<Float> = listOf(
        FULL,       // 0ms: full visibility
        HIGH,       // 80ms: barely dimmed
        0.85f,      // 140ms: dim starts
        MEDIUM,     // 220ms: settling
        0.55f       // 300ms: final de-emphasis
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// 4. SCALE TOKENS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Scale tokens for depth cues.
 *
 * From Spec Section 4.3: "Scale = depth cue."
 * When a layer opens above, the layer below scales down slightly
 * to create the perception of depth / distance.
 *
 * Scale is always relative to 1.0f (natural size).
 * Values below 1.0f = smaller (further away).
 * Values above 1.0f = larger (closer / entering).
 */
object ScaleTokens {

    /** Natural size — no scale change. */
    const val NATURAL = 1.0f

    /** Slight scale-down — background de-emphasis for focus scene. */
    const val SLIGHT_SHRINK = 0.96f

    /** Medium scale-down — base scene when panel is open. */
    const val MEDIUM_SHRINK = 0.94f

    /** Strong scale-down — dramatic depth effect. */
    const val STRONG_SHRINK = 0.90f

    /** Slight scale-up — entering element lifting toward user. */
    const val SLIGHT_GROW = 1.02f

    /** Medium scale-up — panel appearing from rest. */
    const val MEDIUM_GROW = 1.04f

    // ── Scale for Enter Animations ────────────────────────────────────

    /** Initial scale when element starts entering (smaller, further away). */
    const val ENTER_START = 0.92f

    /** Final scale when element finishes entering (natural size). */
    const val ENTER_END = 1.0f

    /** Initial scale for panel entering (from bottom). */
    const val PANEL_ENTER_START = 0.95f

    /** Initial scale for overlay entering. */
    const val OVERLAY_ENTER_START = 0.90f

    /** Initial scale for handoff (dock → focus). */
    const val HANDOFF_START = 0.88f

    // ── Progressive Scale Steps (for background de-emphasis) ──────────

    val deEmphasisSteps: List<Float> = listOf(
        NATURAL,          // 0ms: normal size
        0.99f,            // 80ms: barely noticeable
        SLIGHT_SHRINK,    // 140ms: scale starts
        MEDIUM_SHRINK,    // 220ms: settling
        STRONG_SHRINK     // 300ms: final de-emphasis
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// 5. ELEVATION / SHADOW TOKENS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Elevation tokens for perceived height.
 *
 * From Spec Section 4.3: "Elevation = perceived height."
 * Higher elevation = closer to user = larger shadow.
 * Shadow is used sparingly — "shadow only where needed" (Spec Section 6.5).
 */
object ElevationTokens {

    /** No elevation — flat content. */
    val None: Dp = 0.dp

    /** Dock elevation — barely above base content. */
    val Dock: Dp = 4.dp

    /** Card elevation — slight lift for cards in scroll list. */
    val Card: Dp = 6.dp

    /** Panel elevation — floating panel above scene. */
    val Panel: Dp = 12.dp

    /** Focus elevation — scene-level separation. */
    val Focus: Dp = 16.dp

    /** Overlay elevation — highest standard elevation. */
    val Overlay: Dp = 20.dp

    /** Hero elevation — NowPlaying background grandeur. */
    val Hero: Dp = 24.dp
}

// ═══════════════════════════════════════════════════════════════════════════════
// 6. SPRING PHYSICS TOKENS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Spring physics constants for motion vocabulary.
 *
 * These define the "feel" of different motion types.
 * From Spec Section 7.3: "Movement should be soft, physical, spring-like,
 * believable."
 *
 * These tokens are used by TransitionChoreographer to create
 * physics-based animations that feel alive and physical.
 */
object SpringTokens {

    // ── Damping Ratios ────────────────────────────────────────────────

    /**
     * No bounce — critically damped.
     * Settles as fast as possible without overshooting.
     * Used for: transitions that must feel precise.
     */
    const val DAMPING_NO_BOUNCE = 1.0f

    /**
     * Minimal bounce — slightly underdamped.
     * Tiny overshoot, quick settle. Professional feel.
     * Used for: panel lift, scene push.
     */
    const val DAMPING_MINIMAL = 0.85f

    /**
     * Medium bounce — noticeable overshoot.
     * Playful but controlled. Used for: gestures, scrubber.
     */
    const val DAMPING_MEDIUM = 0.72f

    /**
     * High bounce — significant overshoot.
     * Playful, elastic feel. Used for: button press, micro-interactions.
     */
    const val DAMPING_HIGH = 0.55f

    // ── Stiffness Values ──────────────────────────────────────────────

    /**
     * Very low stiffness — slow, dreamy movement.
     * Used for: background breathing, drift.
     */
    const val STIFFNESS_VERY_LOW = 80f

    /**
     * Low stiffness — gentle, organic movement.
     * Used for: ambient wave, living background.
     */
    const val STIFFNESS_LOW = 150f

    /**
     * Medium-low stiffness — comfortable movement.
     * Used for: scene transitions, handoff.
     */
    const val STIFFNESS_MEDIUM_LOW = 250f

    /**
     * Medium stiffness — responsive but not harsh.
     * Used for: panel lift, content entry.
     */
    const val STIFFNESS_MEDIUM = 400f

    /**
     * High stiffness — snappy, immediate response.
     * Used for: button feedback, dock appear.
     */
    const val STIFFNESS_HIGH = 600f

    // ── Preconfigured Springs ─────────────────────────────────────────

    /** Dreamy slow spring — background breathing, drift. */
    val Dreamy = SpringConfig(
        damping = DAMPING_MINIMAL,
        stiffness = STIFFNESS_VERY_LOW
    )

    /** Gentle spring — ambient movement, subtle motion. */
    val Gentle = SpringConfig(
        damping = DAMPING_MINIMAL,
        stiffness = STIFFNESS_LOW
    )

    /** Smooth spring — scene transitions, handoff. */
    val Smooth = SpringConfig(
        damping = DAMPING_MINIMAL,
        stiffness = STIFFNESS_MEDIUM_LOW
    )

    /** Comfortable spring — panel lift, content entry. */
    val Comfortable = SpringConfig(
        damping = DAMPING_NO_BOUNCE,
        stiffness = STIFFNESS_MEDIUM
    )

    /** Snappy spring — button feedback, quick responses. */
    val Snappy = SpringConfig(
        damping = DAMPING_NO_BOUNCE,
        stiffness = STIFFNESS_HIGH
    )

    /** Bouncy spring — playful micro-interactions. */
    val Bouncy = SpringConfig(
        damping = DAMPING_MEDIUM,
        stiffness = STIFFNESS_MEDIUM_LOW
    )

    /** Elastic spring — scrubber, gesture overshoot. */
    val Elastic = SpringConfig(
        damping = DAMPING_HIGH,
        stiffness = STIFFNESS_LOW
    )
}

/**
 * Spring physics configuration.
 * Used by TransitionChoreographer to create Compose [SpringSpec].
 */
data class SpringConfig(
    /** Damping ratio: 1.0 = no bounce, 0.0 = infinite bounce. */
    val damping: Float,
    /** Stiffness: higher = faster response. */
    val stiffness: Float
)

// ═══════════════════════════════════════════════════════════════════════════════
// 7. LIVING BACKGROUND TOKENS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Tokens for the living background system.
 *
 * From Spec Section 6.3: "Background must live — album art driven,
 * context aware, softly moving, breath-like, never dead."
 *
 * From Spec Section 8.2: "Background should extract colors from album art,
 * create wallpaper-like living backdrop, diffuse colors softly,
 * not overfill the screen, keep professional tone."
 */
object BackgroundTokens {

    // ── Breathing Animation ───────────────────────────────────────────

    /**
     * Full breathing cycle duration in milliseconds.
     * One complete inhale-exhale cycle.
     * Slow enough to feel organic, not distracting.
     */
    const val BREATHING_CYCLE_MS = 6000L

    /**
     * Breathing amplitude — how much the background moves/scales during breath.
     * Very subtle — 2% scale change is the maximum.
     * Spec: "breath-like" — barely perceptible but alive.
     */
    const val BREATHING_AMPLITUDE = 0.02f

    /**
     * Breathing phase offset — how much to offset the sinusoidal wave.
     * Multiple breathing elements use different offsets for organic feel.
     */
    const val BREATHING_PHASE_OFFSET = 0.3f

    // ── Color Extraction ──────────────────────────────────────────────

    /**
     * Number of colors to extract from album art.
     * Minimum 3 for gradient, maximum 5 for rich backdrop.
     */
    const val EXTRACTED_COLOR_COUNT = 5

    /**
     * Maximum saturation multiplier for extracted colors.
     * Prevents over-saturation (Spec Section 6.2: "Not too colorful").
     */
    const val MAX_SATURATION_MULTIPLIER = 1.2f

    /**
     * Minimum saturation multiplier.
     * Ensures background has some color even from desaturated art.
     */
    const val MIN_SATURATION_MULTIPLIER = 0.6f

    /**
     * Diffusion radius — how far colors spread from their source position.
     * Larger = more wallpaper-like, smaller = more focused.
     */
    val DIFFUSION_RADIUS: Dp = 200.dp

    // ── Movement Tokens ────────────────────────────────────────────────

    /**
     * Drift speed — how fast background elements move slowly.
     * Very slow — "softly moving" (Spec Section 6.3).
     * Pixels per second.
     */
    const val DRIFT_SPEED = 0.5f

    /**
     * Maximum drift distance — how far an element can drift before returning.
     */
    val MAX_DRIFT_DISTANCE: Dp = 8.dp

    /**
     * Drift return spring — how the element returns after reaching max drift.
     */
    val DRIFT_RETURN_SPRING = SpringTokens.Dreamy

    // ── Transition Background Behavior ─────────────────────────────────

    /**
     * Background color transition duration when song changes.
     * Longer than UI transitions — smooth color wash.
     */
    const val SONG_CHANGE_COLOR_TRANSITION_MS = 800L

    /**
     * Background blur transition duration.
     * Progressive — blur animates over this duration.
     */
    const val BLUR_TRANSITION_MS = 350L
}

// ═══════════════════════════════════════════════════════════════════════════════
// 8. PANEL / OVERLAY SIZING TOKENS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Sizing tokens for panels and overlays.
 *
 * From Spec Section 17.5: "Not every tool should consume the whole screen.
 * Equalizer / timer / settings can stay as elegant floating layers."
 *
 * From Spec Section 1: "mobile-friendly — desktop-like giant windows no."
 */
object PanelTokens {

    // ── Panel Height Fractions (of screen height) ─────────────────────

    /** Minimum panel height — even the smallest panel must be this tall. */
    const val MIN_HEIGHT_FRACTION = 0.35f

    /** Default panel height. */
    const val DEFAULT_HEIGHT_FRACTION = 0.55f

    /** Large panel height — queue, settings. */
    const val LARGE_HEIGHT_FRACTION = 0.75f

    /** Maximum panel height — never truly full-screen. */
    const val MAX_HEIGHT_FRACTION = 0.85f

    // ── Panel Width ──────────────────────────────────────────────────

    /** Panel always full-width (mobile). */
    const val WIDTH_FRACTION = 1.0f

    // ── Panel Corner Radius ───────────────────────────────────────────

    /** Top corner radius for bottom panels. */
    val PANEL_CORNER_TOP: Dp = 28.dp

    /** Bottom corner radius for top panels (if any). */
    val PANEL_CORNER_BOTTOM: Dp = 0.dp

    // ── Specific Panel Sizes ──────────────────────────────────────────

    /** Equalizer panel: medium height. */
    const val EQUALIZER_HEIGHT_FRACTION = 0.55f

    /** Queue panel: large height. */
    const val QUEUE_HEIGHT_FRACTION = 0.80f

    /** Sleep timer: small floating panel. */
    const val TIMER_HEIGHT_FRACTION = 0.45f

    /** Song info: small panel. */
    const val SONG_INFO_HEIGHT_FRACTION = 0.50f

    // ── Overlay Sizes ──────────────────────────────────────────────────

    /** Context menu: anchored to trigger, width bounded. */
    const val CONTEXT_MENU_MAX_WIDTH_DP = 280

    /** Tooltip: auto-sized to content. */
    const val TOOLTIP_MAX_WIDTH_DP = 240

    /** Quick info: card-like floating panel. */
    const val QUICK_INFO_MAX_WIDTH_DP = 320
}

// ═══════════════════════════════════════════════════════════════════════════════
// 9. DOCK TOKENS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Tokens for the persistent dock (Mini Player).
 *
 * From Spec Section 11.2: "Bottom-oriented controls."
 * From Rule #67: "Dock never interrupts."
 */
object DockTokens {

    /** Dock height — compact but thumb-friendly (≥48dp per ergonomic rules). */
    val DOCK_HEIGHT: Dp = 64.dp

    /** Dock corner radius. */
    val DOCK_CORNER: Dp = 20.dp

    /** Dock horizontal margin from screen edges. */
    val DOCK_HORIZONTAL_MARGIN: Dp = 12.dp

    /** Dock bottom margin (above bottom nav). */
    val DOCK_BOTTOM_MARGIN: Dp = 8.dp

    /** Album art thumbnail size in dock. */
    val DOCK_ART_SIZE: Dp = 44.dp

    /** Dock elevation (barely above base content). */
    val DOCK_ELEVATION: Dp = 4.dp

    /** Dock appear animation duration. */
    const val DOCK_APPEAR_DURATION = 250

    /** Dock disappear animation duration. */
    const val DOCK_DISAPPEAR_DURATION = 200
}

// ═══════════════════════════════════════════════════════════════════════════════
// 10. SCENE ENTRY STAGGER TOKENS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Stagger tokens for multi-element scene entry.
 *
 * When a scene opens, elements don't all appear simultaneously.
 * They stagger in with increasing delay — creating a wave effect
 * that feels organic and intentional.
 */
object StaggerTokens {

    /** Delay between each staggered element. */
    const val ELEMENT_DELAY_MS = 35

    /** Maximum number of elements in a stagger cascade. */
    const val MAX_STAGGER_COUNT = 8

    /** Maximum total stagger duration (prevents slow-feeling entries). */
    const val MAX_CASCADE_MS = 280

    /** Stagger animation per element — uses spring, not tween. */
    val STAGGER_SPRING = SpringTokens.Smooth
}

// ═══════════════════════════════════════════════════════════════════════════════
// 11. REDUCED MOTION TOKENS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Tokens for reduced motion accessibility mode.
 *
 * When user has reduced motion enabled in system settings,
 * all animations use these simplified values.
 * Motion is NOT eliminated — it is simplified (Rule #74 spirit:
 * "Silence Is Also UX" — but accessibility comes first).
 */
object ReducedMotionTokens {

    /** All transitions use a single short duration. */
    const val TRANSITION_DURATION = 150

    /** No stagger — all elements appear together. */
    const val STAGGER_DELAY = 0

    /** No scale changes. */
    const val SCALE_CHANGE = 0f

    /** Minimal blur — just enough for hierarchy signal. */
    val BLUR_AMOUNT: Dp = 4.dp

    /** Simple fade — no spatial motion. */
    const val OPACITY_CHANGE = 1.0f
}
