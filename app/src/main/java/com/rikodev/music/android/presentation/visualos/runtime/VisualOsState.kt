package com.rikodev.music.android.presentation.visualos.runtime

import com.rikodev.music.android.presentation.visualos.contract.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// RIKO VISUAL OS — STATE MODEL
//
// The single source of truth for the entire Visual OS layer.
// This state is immutable (Rule: State is immutable).
// Reducer returns new state instances; nothing mutates in place.
//
// Architecture: Contract → Runtime → Reducer → Mapper → Rendering → Integration
// Rule #70: Architecture Freeze — this follows the frozen pipeline.
// ─────────────────────────────────────────────────────────────────────────────

// ═══════════════════════════════════════════════════════════════════════════════
// 1. VISUAL OS STATE — Root state for the entire Visual OS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Root state for the RIKO Visual OS.
 *
 * This state contains everything needed to render the entire layered UI:
 *   - Scene stack (what scenes exist, in what order)
 *   - Layer states (visual parameters for each layer)
 *   - Active transition (if any transition is in progress)
 *   - Focus ownership (who owns user interaction)
 *   - Dock state (mini player visibility)
 *   - Background state (alive, breathing, album-art-driven)
 *   - Overlay state (context menu, tooltip)
 *
 * This state is:
 *   - Immutable: Reducer returns new instances (never mutates)
 *   - Serializable: Can be saved/restored for process death
 *   - Observable: Compose collects changes via StateFlow
 *   - Single Source: All rendering reads from this state (Rule #60)
 */
data class VisualOsState(
    // ── Scene Stack ────────────────────────────────────────────────────

    /**
     * Ordered stack of scene specs.
     * Index 0 = base scene (bottom of stack).
     * Last index = topmost active scene.
     *
     * The scene stack is the core data structure of Visual OS.
     * Unlike traditional navigation back stacks, this stack represents
     * a visual depth hierarchy — each entry has a layer type and visual
     * state that defines how it appears relative to other entries.
     */
    val sceneStack: List<SceneSpec> = emptyList(),

    // ── Transition State ──────────────────────────────────────────────

    /**
     * Currently active transition, if any.
     * null means no transition in progress — all scenes at rest.
     *
     * Only ONE transition can be active at a time.
     * If a new transition is requested while one is in progress,
     * the current one is either completed immediately or interrupted.
     */
    val activeTransition: ActiveTransition? = null,

    // ── Focus State ────────────────────────────────────────────────────

    /**
     * Who currently owns user focus / interaction.
     * Determined by the topmost scene in the stack.
     */
    val focusOwner: FocusOwner = FocusOwner.BASE,

    // ── Dock State ─────────────────────────────────────────────────────

    /**
     * State of the persistent mini player dock.
     * Dock is always present except when:
     *   - No song is playing (hidden)
     *   - NowPlaying is active (morphed into focus scene)
     */
    val dockState: DockState = DockState(),

    // ── Overlay State ────────────────────────────────────────────────

    /**
     * State of the overlay layer (context menu, tooltip).
     * Overlays are transient — appear and disappear quickly.
     * Only one overlay can be active at a time.
     */
    val overlayState: OverlayState = OverlayState(),

    // ── Background State ──────────────────────────────────────────────

    /**
     * State of the living background system.
     * The background is always alive (Spec: "never dead").
     * Its visual properties change based on:
     *   - Current song's album art colors
     *   - Depth plane of the active scene
     *   - Breathing animation phase
     */
    val backgroundState: LivingBackgroundState = LivingBackgroundState(),

    // ── Song Context ──────────────────────────────────────────────────

    /**
     * Current song context for background reactivity.
     * When song changes, background colors transition smoothly.
     * This data drives the living background engine.
     */
    val songContext: SongContext? = null,

    // ── System State ──────────────────────────────────────────────────

    /**
     * System-wide state flags.
     */
    val systemState: VisualOsSystemState = VisualOsSystemState()
) {
    // ── Derived Properties ────────────────────────────────────────────

    /** The current base scene (first in stack). */
    val baseScene: SceneSpec?
        get() = sceneStack.firstOrNull { it.layerType == RikoLayerType.BASE }

    /** The current focus scene (topmost non-base, non-panel, non-overlay). */
    val focusScene: SceneSpec?
        get() = sceneStack.lastOrNull { it.layerType == RikoLayerType.FOCUS }

    /** The current panel scene (if any panel is open). */
    val activePanel: SceneSpec?
        get() = sceneStack.lastOrNull { it.layerType == RikoLayerType.PANEL }

    /** Whether any transition is currently in progress. */
    val isTransitioning: Boolean
        get() = activeTransition != null

    /** Whether any panel is currently open. */
    val isPanelOpen: Boolean
        get() = activePanel != null

    /** Whether dock should be visible. */
    val isDockVisible: Boolean
        get() = dockState.visibility == DockVisibility.VISIBLE

    /** Whether an overlay is currently showing. */
    val isOverlayShowing: Boolean
        get() = overlayState.isActive

    /** Total number of scenes in the stack. */
    val stackDepth: Int
        get() = sceneStack.size

    /** Whether the scene stack is empty (shouldn't normally happen). */
    val isEmpty: Boolean
        get() = sceneStack.isEmpty()

    companion object {
        /** Initial state — empty stack, no transitions, no dock. */
        fun initial(): VisualOsState = VisualOsState(
            sceneStack = listOf(
                SceneSpec.create(RikoSceneType.Library).copy(
                    lifecycle = SceneLifecycle.ACTIVE,
                    visualState = VisualState.activeForLayer(RikoLayerType.BASE)
                )
            ),
            focusOwner = FocusOwner.BASE,
            dockState = DockState(visibility = DockVisibility.HIDDEN),
            backgroundState = LivingBackgroundState()
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 2. ACTIVE TRANSITION — In-progress transition state
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * State of a currently active transition.
 *
 * Contains the transition spec (what's happening), the current progress
 * (how far along), and interpolated visual states for both the entering
 * and leaving scenes.
 *
 * The TransitionChoreographer updates this state every frame during
 * the transition. When progress reaches 1.0f, the transition completes
 * and the reducer applies the final visual states.
 */
data class ActiveTransition(
    /** The transition specification. */
    val spec: TransitionSpec,

    /** Current progress (0.0 = start, 1.0 = complete). */
    val progress: Float = 0f,

    /** Interpolated visual state for the entering/leaving scene. */
    val enteringVisualState: VisualState = VisualState(),

    /** Interpolated visual state for the background scene. */
    val backgroundVisualState: VisualState = VisualState(),

    /** When this transition started (epoch millis). */
    val startedAtMs: Long = System.currentTimeMillis(),

    /** Whether this transition has been interrupted by another. */
    val isInterrupted: Boolean = false
) {
    /** Whether this transition is complete. */
    val isComplete: Boolean get() = progress >= 1.0f

    /** Elapsed time since start. */
    val elapsedMs: Long
        get() = System.currentTimeMillis() - startedAtMs
}

// ═══════════════════════════════════════════════════════════════════════════════
// 3. DOCK STATE — Mini Player dock
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * State of the persistent mini player dock.
 *
 * From Spec Section 10.5: "Dock scene — Mini Player."
 * From Rule #67: "Dock never interrupts."
 * From Spec Section 11.2: "Bottom-oriented controls."
 */
data class DockState(
    /** Current visibility of the dock. */
    val visibility: DockVisibility = DockVisibility.HIDDEN,

    /** Visual state for dock animation. */
    val visualState: VisualState = VisualState.defaultForLayer(RikoLayerType.DOCK),

    /** Whether dock is currently morphing to NowPlaying. */
    val isMorphing: Boolean = false,

    /** Morph progress (0.0 = dock, 1.0 = NowPlaying). */
    val morphProgress: Float = 0f,

    /** Song title for dock display. */
    val songTitle: String = "",

    /** Song artist for dock display. */
    val songArtist: String = "",

    /** Album art color accent for dock tint. */
    val accentColor: Long = 0xFF000000,

    /** Whether playback is active (for play/pause icon). */
    val isPlaying: Boolean = false
)

/** Dock visibility states. */
enum class DockVisibility {
    /** Dock is hidden — no song playing or NowPlaying active. */
    HIDDEN,

    /** Dock is visible — song playing, showing mini player. */
    VISIBLE,

    /** Dock is transitioning — appearing or disappearing. */
    TRANSITIONING
}

// ═══════════════════════════════════════════════════════════════════════════════
// 4. OVERLAY STATE — Context menu, tooltip, quick info
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * State of the overlay layer.
 *
 * From Spec Section 10.4: "Overlay scene — context menu, quick info, tooltips."
 * Overlays are transient — appear briefly, dismiss on outside tap or action.
 */
data class OverlayState(
    /** Whether an overlay is currently active. */
    val isActive: Boolean = false,

    /** Type of active overlay. */
    val type: OverlayType = OverlayType.NONE,

    /** Visual state for overlay animation. */
    val visualState: VisualState = VisualState.defaultForLayer(RikoLayerType.OVERLAY),

    /** Position anchor for anchored overlays (context menu). */
    val anchorPosition: OverlayAnchor? = null,

    /** Content data for the overlay (varies by type). */
    val contentData: Map<String, Any> = emptyMap(),

    /** Whether the overlay can be dismissed by tapping outside. */
    val dismissOnOutsideTap: Boolean = true
)

/** Types of overlays. */
enum class OverlayType {
    /** No overlay. */
    NONE,

    /** Context menu — anchored to trigger element. */
    CONTEXT_MENU,

    /** Quick info tooltip — brief information. */
    QUICK_INFO,

    /** Toast / snackbar — transient notification. */
    TOAST
}

/** Anchor position for an anchored overlay. */
data class OverlayAnchor(
    /** X position in pixels. */
    val x: Float,
    /** Y position in pixels. */
    val y: Float,
    /** Width of the trigger element. */
    val triggerWidth: Float = 0f,
    /** Height of the trigger element. */
    val triggerHeight: Float = 0f,
    /** Whether to show above or below the anchor. */
    val preferAbove: Boolean = true
)

// ═══════════════════════════════════════════════════════════════════════════════
// 5. LIVING BACKGROUND STATE — Alive, breathing, reactive
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * State of the living background system.
 *
 * From Spec Section 6.3: "Background must live — album art driven,
 * context aware, softly moving, breath-like, never dead."
 *
 * The background is NOT a scene — it's a persistent visual layer that
 * underlies all scenes. Its appearance changes based on:
 *   - Current song's album art extracted colors
 *   - Which scene is active (different scenes have different bg treatment)
 *   - Breathing animation phase (sinusoidal pulsing)
 *   - Transition state (blur/opacity changes during transitions)
 */
data class LivingBackgroundState(
    // ── Color State ───────────────────────────────────────────────────

    /**
     * Extracted palette from current album art.
     * Empty if no song is playing or colors haven't been extracted.
     * List of ARGB color longs.
     */
    val albumArtColors: List<Long> = emptyList(),

    /**
     * Dominant color from album art (for tinting).
     * 0 if not available.
     */
    val dominantColor: Long = 0xFF000000,

    /**
     * Vibrant color from album art (for accents).
     * 0 if not available.
     */
    val vibrantColor: Long = 0xFF000000,

    // ── Animation State ────────────────────────────────────────────────

    /**
     * Current breathing phase (0.0 to 2*PI for full cycle).
     * Updated by animation loop every frame.
     */
    val breathingPhase: Float = 0f,

    /**
     * Breathing intensity — how pronounced the breathing effect is.
     * 0.0 = no breathing (reduced motion), 1.0 = full breathing.
     */
    val breathingIntensity: Float = BackgroundTokens.BREATHING_AMPLITUDE,

    /**
     * Current drift offset X (pixels).
     * Slow, continuous horizontal movement.
     */
    val driftOffsetX: Float = 0f,

    /**
     * Current drift offset Y (pixels).
     * Slow, continuous vertical movement.
     */
    val driftOffsetY: Float = 0f,

    // ── Visual Parameters ─────────────────────────────────────────────

    /**
     * Current background blur (dp).
     * Changes during transitions — more blur when focus scene is active.
     */
    val blur: Float = 0f,

    /**
     * Current background opacity.
     * 1.0 = fully visible, lower values = dimmed.
     */
    val opacity: Float = 1.0f,

    /**
     * Current background scale.
     * Slightly > 1.0 for "breathing" expansion effect.
     */
    val scale: Float = 1.0f,

    /**
     * Current color saturation multiplier.
     * Applied to extracted colors to prevent over-saturation.
     */
    val saturationMultiplier: Float = 1.0f,

    // ── Context ───────────────────────────────────────────────────────

    /**
     * Which scene type the background is currently serving.
     * Different scenes may have different background treatments:
     *   - Library: subtle gradient
     *   - NowPlaying: dramatic album art diffusion
     */
    val servingScene: RikoSceneType? = null,

    /** Whether the background is currently in "alive" mode (animating). */
    val isAlive: Boolean = true
) {
    companion object {
        /** Initial background state — no colors, breathing at default. */
        fun initial(): LivingBackgroundState = LivingBackgroundState(
            breathingPhase = 0f,
            breathingIntensity = BackgroundTokens.BREATHING_AMPLITUDE,
            blur = 0f,
            opacity = 1.0f,
            scale = 1.0f,
            isAlive = true
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 6. SONG CONTEXT — Current song for background reactivity
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Context about the current song for background reactivity.
 *
 * When song changes, background transitions to new colors extracted
 * from the new album art. This context drives that transition.
 */
data class SongContext(
    /** Song ID. */
    val songId: Long,

    /** Song title (for dock display). */
    val title: String,

    /** Song artist (for dock display). */
    val artist: String,

    /** Album art URI (for color extraction). */
    val albumArtUri: String? = null,

    /** Pre-extracted album art colors (if available). */
    val extractedColors: List<Long> = emptyList(),

    /** Whether the song is currently playing. */
    val isPlaying: Boolean = true
)

// ═══════════════════════════════════════════════════════════════════════════════
// 7. VISUAL OS SYSTEM STATE — System-wide flags
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * System-wide state flags for the Visual OS.
 *
 * These flags affect global behavior but don't change per-scene.
 */
data class VisualOsSystemState(
    /** Whether reduced motion mode is enabled (accessibility). */
    val reducedMotion: Boolean = false,

    /** Current device refresh rate (Hz). */
    val refreshRateHz: Int = 60,

    /** Whether the system is in landscape orientation. */
    val isLandscape: Boolean = false,

    /** Whether the system is in multi-window mode. */
    val isMultiWindow: Boolean = false,

    /** Whether immersive mode is active (hide system bars). */
    val isImmersive: Boolean = false,

    /** Frame timing budget in milliseconds (derived from refresh rate). */
    val frameBudgetMs: Float = 16.67f
) {
    companion object {
        /** Compute frame budget from refresh rate. */
        fun frameBudgetForHz(hz: Int): Float = 1000f / hz
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 8. RENDER SNAPSHOT — What composables actually read
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Render-ready snapshot computed by the Mapper.
 *
 * Composable functions read from this snapshot, NOT from VisualOsState directly.
 * The Mapper transforms the raw state into render-ready data:
 *   - Computes final visual parameters (interpolated during transitions)
 *   - Resolves z-ordering for all layers
 *   - Computes background parameters
 *   - Strips internal state that composables don't need
 *
 * This separation ensures:
 *   - Rule #60: Screen State Is Already Renderable
 *   - Rule #58: Runtime Never Knows Rendering
 *   - Rule #59: Mapper Is The Last Translator
 */
data class VisualOsRenderSnapshot(
    // ── Scene Layer Rendering ────────────────────────────────────────

    /**
     * Ordered list of renderable scene layers.
     * Each entry has all visual parameters needed for rendering.
     * Ordered by z-index (lowest first, highest last).
     */
    val renderableLayers: List<RenderableLayer> = emptyList(),

    // ── Background Rendering ──────────────────────────────────────────

    /** Parameters for rendering the living background. */
    val backgroundParams: BackgroundRenderParams = BackgroundRenderParams(),

    // ── Dock Rendering ────────────────────────────────────────────────

    /** Parameters for rendering the mini player dock. */
    val dockParams: DockRenderParams = DockRenderParams(),

    // ── Overlay Rendering ──────────────────────────────────────────────

    /** Parameters for rendering the overlay (if active). */
    val overlayParams: OverlayRenderParams? = null,

    // ── Transition Rendering ───────────────────────────────────────────

    /** Parameters for rendering transition effects (if transitioning). */
    val transitionParams: TransitionRenderParams? = null,

    // ── Global State ──────────────────────────────────────────────────

    /** Whether reduced motion is active. */
    val reducedMotion: Boolean = false,

    /** Current frame budget in ms. */
    val frameBudgetMs: Float = 16.67f
)

/**
 * A single renderable layer — what a scene looks like right now.
 * Composable functions read this to render the scene.
 */
data class RenderableLayer(
    /** Scene identifier. */
    val sceneId: SceneId,
    /** Scene type. */
    val sceneType: RikoSceneType,
    /** Layer type in the visual hierarchy. */
    val layerType: RikoLayerType,
    /** All current visual parameters (opacity, blur, scale, etc.). */
    val visualState: VisualState,
    /** Whether this layer should be rendered. */
    val shouldRender: Boolean = true,
    /** Z-index for rendering order. */
    val zIndex: Int = 0,
    /** Whether this layer owns interaction. */
    val ownsInteraction: Boolean = false,
    /** Glass tier for this layer. */
    val glassTier: GlassTier = GlassTier.NONE,
    /** Scene capabilities (what this scene can do). */
    val capabilities: SceneCapabilities = SceneCapabilities()
)

/** Parameters for rendering the living background. */
data class BackgroundRenderParams(
    val albumArtColors: List<Long> = emptyList(),
    val dominantColor: Long = 0xFF000000,
    val vibrantColor: Long = 0xFF000000,
    val blur: Float = 0f,
    val opacity: Float = 1.0f,
    val scale: Float = 1.0f,
    val breathingPhase: Float = 0f,
    val breathingIntensity: Float = 0f,
    val driftOffsetX: Float = 0f,
    val driftOffsetY: Float = 0f,
    val saturationMultiplier: Float = 1.0f,
    val isAlive: Boolean = true
)

/** Parameters for rendering the mini player dock. */
data class DockRenderParams(
    val isVisible: Boolean = false,
    val visualState: VisualState = VisualState(),
    val songTitle: String = "",
    val songArtist: String = "",
    val accentColor: Long = 0xFF000000,
    val isPlaying: Boolean = false
)

/** Parameters for rendering the overlay. */
data class OverlayRenderParams(
    val type: OverlayType = OverlayType.NONE,
    val visualState: VisualState = VisualState(),
    val anchorPosition: OverlayAnchor? = null,
    val contentData: Map<String, Any> = emptyMap(),
    val dismissOnOutsideTap: Boolean = true
)

/** Parameters for rendering transition effects. */
data class TransitionRenderParams(
    val type: TransitionType = TransitionType.NONE,
    val progress: Float = 0f,
    val enteringSceneId: SceneId? = null,
    val backgroundSceneId: SceneId? = null
)
