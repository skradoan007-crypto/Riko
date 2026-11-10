package com.rikodev.music.android.presentation.visualos.contract

// ─────────────────────────────────────────────────────────────────────────────
// RIKO VISUAL OS — CONSTITUTION
//
// This is the UI/UX Constitution of RIKO Experience OS.
// Every visual decision in the app must be traceable to a rule defined here.
// This file is the single source of truth for the Visual OS layer grammar.
//
// Design Philosophy (from spec):
//   - Screen change ≠ simple left-right slide
//   - Base layer stays behind, focus layer floats above
//   - Panels are floating instruments, not full-screen pages
//   - Navigation = choreography, not replacement
//   - Background never dies, never hides — only de-emphasizes
//   - Motion is information, not decoration (Rule #73)
//   - Glass is hierarchy, not decoration
//   - Every pixel must be explainable (Spec Section 22)
//
// Frozen Architecture: Contract → Runtime → Reducer → Mapper → Rendering → Integration
// Rule #70 applies — no new architecture invention.
// Rules #1-#83 all enforced.
// ─────────────────────────────────────────────────────────────────────────────

// ═══════════════════════════════════════════════════════════════════════════════
// 1. SCENE TYPES — What exists as a visual state
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Defines every possible scene in RIKO Visual OS.
 *
 * A Scene is NOT a "page" or "screen." It is a visual state of the app
 * that may consist of multiple layers. Scene types are stable — new scenes
 * require new sealed variants, which follows our contract-first discipline
 * (Rule #70: Architecture Freeze).
 *
 * Scene lifecycle:
 *   Created → Active → (Paused) → Background → Resumed → Destroyed
 *
 * A scene transitions through these states as layers push/pop above it.
 * A Base scene never "closes" — it only moves to background state when
 * a Focus scene activates.
 */
sealed class RikoSceneType(val routeKey: String) {

    // ── Base Scenes (root context, always at bottom of scene stack) ────────

    /**
     * All Music / Library home — the default base scene.
     * Shows collections, hero, sections, dock.
     * This scene is the "ground floor" — it never teleports, never disappears.
     * When focus scenes open above it, this scene de-emphasizes (blur + dim + scale).
     */
    data object Library : RikoSceneType("library")

    /**
     * Playlists listing — alternate base scene.
     * Same de-emphasis rules as Library when other layers stack above.
     */
    data object Playlists : RikoSceneType("playlists")

    /**
     * Search — can be base OR focus depending on navigation context.
     * When navigated from tab: base. When navigated from within: focus.
     */
    data object Search : RikoSceneType("search")

    /**
     * Settings — base scene, follows same layer behavior as Library.
     */
    data object Settings : RikoSceneType("settings")

    // ── Focus Scenes (active main scene, floats above base) ────────────────

    /**
     * Now Playing — THE hero scene. Visual heart of RIKO.
     * Album art is the visual focus. Controls are secondary.
     * Background reacts to artwork colors. Progress is alive.
     * This scene has special rendering priority (Rule #57: every screen has one hero).
     */
    data object NowPlaying : RikoSceneType("now_playing")

    /**
     * Playlist Detail — focus scene showing playlist contents.
     * Opens above Library base. Has its own hero section.
     */
    data class PlaylistDetail(val playlistId: Long) : RikoSceneType("playlist_detail_$playlistId")

    // ── Panel Scenes (floating instruments, not full-screen) ──────────────

    /**
     * Equalizer — floating glass panel.
     * Opens from NowPlaying. Background (NP) blurs progressively.
     * Panel is bounded, never full-screen (Spec Section 9).
     */
    data object EqualizerPanel : RikoSceneType("eq_panel")

    /**
     * Sleep Timer — floating instrument layer (Spec Section 9).
     * Circular dial controls. Glass object behavior.
     * Background stays alive behind it.
     */
    data object SleepTimerPanel : RikoSceneType("timer_panel")

    /**
     * Song Info — floating panel for track metadata.
     */
    data object SongInfoPanel : RikoSceneType("info_panel")

    /**
     * Queue — medium-large panel showing current queue.
     * Opens from NowPlaying. Can be nearly full-height but never full-screen.
     */
    data object QueuePanel : RikoSceneType("queue_panel")

    // ── Overlay Scenes (transient, top layer) ──────────────────────────────

    /**
     * Context Menu — appears on long-press / right-click.
     * Small, targeted, auto-dismisses on outside tap.
     */
    data object ContextMenu : RikoSceneType("context_menu")

    /**
     * Quick Info / Tooltip — transient information surface.
     */
    data object QuickInfo : RikoSceneType("quick_info")

    // ── Dock (persistent, always present) ──────────────────────────────────

    /**
     * Mini Player — persistent dock surface.
     * Always visible except when NowPlaying is active.
     * Bottom-oriented. Thumb-friendly.
     * Morph capability: can expand to NowPlaying via handoff animation.
     */
    data object MiniPlayerDock : RikoSceneType("mini_player_dock")

    companion object {
        /** All base-level scenes (tab destinations). */
        val baseScenes: Set<RikoSceneType> = setOf(
            Library, Playlists, Search, Settings
        )

        /** All panel scenes (floating instruments). */
        val panelScenes: Set<RikoSceneType> = setOf(
            EqualizerPanel, SleepTimerPanel, SongInfoPanel, QueuePanel
        )

        /** All overlay scenes (transient). */
        val overlayScenes: Set<RikoSceneType> = setOf(
            ContextMenu, QuickInfo
        )

        /** Tab routes that appear in bottom navigation. */
        val tabRoutes: List<RikoSceneType> = listOf(
            Library, Playlists, NowPlaying, Search, Settings
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 2. LAYER TYPES — Depth planes inside a scene
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Defines the five layer types in RIKO's visual grammar.
 *
 * Layer ≠ z-index. Layer means:
 *   - Depth (visual distance from user)
 *   - Priority (interaction ownership)
 *   - Focus state (who receives input)
 *   - Visibility state (how much of this layer is shown)
 *
 * From Spec Section 4.1:
 *   Base Layer → Focus Layer → Panel Layer → Overlay Layer → Dock Layer
 *
 * Each type has strict behavioral rules defined in [LayerBehavior].
 */
enum class RikoLayerType {
    /**
     * Root context layer. Home / Library / base tab.
     * Never teleports. Only de-emphasizes when higher layers activate.
     * De-emphasis = progressive blur + dim + scale-down (never hide).
     */
    BASE,

    /**
     * Active main scene. Now Playing / detail screens.
     * Receives full user interaction ownership.
     * Background behind it stays alive (Rule: background never dies).
     */
    FOCUS,

    /**
     * Floating instrument. Equalizer, Timer, Settings sheet.
     * NOT full-screen. Medium or small glass panel.
     * Background (focus scene) blurs progressively when panel opens.
     * Panel has its own glass hierarchy within its bounds.
     */
    PANEL,

    /**
     * Transient top layer. Context menu, quick action, tooltip.
     * Auto-dismisses. Small surface. No background blur on lower layers.
     * Brief appearance — no heavy animation needed, but still keyframe-driven.
     */
    OVERLAY,

    /**
     * Persistent small surface. Mini Player dock.
     * Always visible (except when NowPlaying is active, as dock morphs to focus).
     * Bottom-oriented for thumb reach (Spec Section 11.2).
     * Never interrupts (Rule #67).
     */
    DOCK
}

// ═══════════════════════════════════════════════════════════════════════════════
// 3. LAYER BEHAVIOR — Strict rules per layer type
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Defines the strict behavioral contract for each layer type.
 *
 * These rules are NOT suggestions — they are enforced by the TransitionChoreographer.
 * If any animation violates these rules, it is rejected (Spec Section 22:
 * "Nothing is allowed to happen without logic").
 *
 * From Spec Section 4.2:
 *   - Base layer never teleports
 *   - New layer never instantly pops
 *   - Open = scale + opacity + blur simultaneously
 *   - Close = reverse motion
 *   - Background de-emphasizes, never disappears
 */
data class LayerBehavior(
    /** This layer's type. */
    val type: RikoLayerType,

    // ── Appearance Rules ──────────────────────────────────────────────────

    /**
     * Whether this layer enters with a keyframe-driven animation.
     * ALL layers must enter with animation. "Nothing should just appear"
     * (Spec Section 7.2).
     */
    val requiresEntryAnimation: Boolean = true,

    /**
     * Whether this layer exits with reverse animation.
     * Close = reverse of open motion (Spec Section 4.2).
     */
    val requiresExitAnimation: Boolean = true,

    /**
     * Whether this layer can appear instantly (emergency only).
     * Only OVERLAY may skip animation for dismiss-on-outside-tap.
     */
    val allowsInstantDismiss: Boolean = false,

    // ── Background Interaction Rules ───────────────────────────────────────

    /**
     * Whether this layer causes the layer immediately below to blur.
     * Panel and Overlay cause progressive blur on Focus/Base.
     * Dock does NOT blur (Rule #67: Dock never interrupts).
     */
    val blursBackground: Boolean = false,

    /**
     * Whether this layer causes the layer immediately below to dim.
     * Dim = reduced opacity, not hidden.
     */
    val dimsBackground: Boolean = false,

    /**
     * Whether this layer causes the layer below to scale down.
     * Scale provides depth cue — background feels further away.
     */
    val scalesBackground: Boolean = false,

    // ── Ownership Rules ──────────────────────────────────────────────────

    /**
     * Whether this layer owns all touch interaction.
     * Focus and Panel own interaction. Overlay owns but is dismissible.
     * Dock does NOT own — it shares with Base.
     */
    val ownsInteraction: Boolean = true,

    /**
     * Whether this layer can coexist with Dock.
     * All layers except NowPlaying coexist with Dock.
     */
    val coexistsWithDock: Boolean = true,

    /**
     * Maximum size this layer can occupy (fraction of screen).
     * 1.0 = full screen (for Base/Focus only).
     * Panels are bounded — 0.4-0.85 depending on content.
     * Overlay is small — 0.1-0.3.
     * Dock is fixed height.
     */
    val maxScreenFraction: Float = 1.0f,

    /**
     * Minimum size this layer can occupy (fraction of screen).
     * Ensures mobile-friendly sizing (Spec Section 1: "layering mobile-friendly").
     */
    val minScreenFraction: Float = 0.0f,

    // ── Z-Order Rules ─────────────────────────────────────────────────────

    /** Base z-index for this layer type. Higher = closer to user. */
    val baseZIndex: Int = 0,

    /** Whether multiple instances of this layer can stack. */
    val allowsStacking: Boolean = false
) {
    companion object {
        // ── Predefined behaviors for each layer type ──────────────────

        val Base = LayerBehavior(
            type = RikoLayerType.BASE,
            requiresEntryAnimation = false,     // Base is always present
            requiresExitAnimation = false,
            blursBackground = false,
            dimsBackground = false,
            scalesBackground = false,
            ownsInteraction = true,
            coexistsWithDock = true,
            maxScreenFraction = 1.0f,
            minScreenFraction = 1.0f,
            baseZIndex = 0,
            allowsStacking = false
        )

        val Focus = LayerBehavior(
            type = RikoLayerType.FOCUS,
            requiresEntryAnimation = true,
            requiresExitAnimation = true,
            blursBackground = true,               // Blurs base when opening
            dimsBackground = true,                // Dims base
            scalesBackground = true,              // Scales base down for depth
            ownsInteraction = true,
            coexistsWithDock = false,             // NowPlaying hides dock (morph)
            maxScreenFraction = 1.0f,
            minScreenFraction = 1.0f,
            baseZIndex = 100,
            allowsStacking = false
        )

        val Panel = LayerBehavior(
            type = RikoLayerType.PANEL,
            requiresEntryAnimation = true,        // Lift animation
            requiresExitAnimation = true,         // Reverse lift
            blursBackground = true,                // Progressive blur on focus
            dimsBackground = true,                 // Progressive dim
            scalesBackground = false,             // No scale for panel (it's floating)
            ownsInteraction = true,
            coexistsWithDock = true,
            maxScreenFraction = 0.85f,             // Bounded! (Spec Section 17.5)
            minScreenFraction = 0.35f,
            baseZIndex = 200,
            allowsStacking = false
        )

        val Overlay = LayerBehavior(
            type = RikoLayerType.OVERLAY,
            requiresEntryAnimation = true,
            requiresExitAnimation = true,
            allowsInstantDismiss = true,          // Can dismiss on outside tap
            blursBackground = false,              // Overlay doesn't blur below
            dimsBackground = true,                 // Slight dim
            scalesBackground = false,
            ownsInteraction = true,
            coexistsWithDock = true,
            maxScreenFraction = 0.30f,
            minScreenFraction = 0.10f,
            baseZIndex = 300,
            allowsStacking = false
        )

        val Dock = LayerBehavior(
            type = RikoLayerType.DOCK,
            requiresEntryAnimation = true,
            requiresExitAnimation = true,
            blursBackground = false,
            dimsBackground = false,
            scalesBackground = false,
            ownsInteraction = false,                // Shares with base
            coexistsWithDock = true,
            maxScreenFraction = 0.15f,             // Fixed small height
            minScreenFraction = 0.10f,
            baseZIndex = 50,
            allowsStacking = false
        )

        /** Get behavior for a given layer type. */
        fun forType(type: RikoLayerType): LayerBehavior = when (type) {
            RikoLayerType.BASE -> Base
            RikoLayerType.FOCUS -> Focus
            RikoLayerType.PANEL -> Panel
            RikoLayerType.OVERLAY -> Overlay
            RikoLayerType.DOCK -> Dock
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 4. SCENE LIFECYCLE — State machine for each scene
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Lifecycle states for a RIKO scene.
 *
 * Unlike traditional Activity/Fragment lifecycles, RIKO scenes have additional
 * states that reflect the layered visual model:
 *
 *   Created → Active → Deemphasized → Resumed → Destroyed
 *
 * "Deemphasized" is unique to RIKO — a scene is still alive and rendering,
 * but at reduced visual prominence (blurred, dimmed, scaled) because
 * another layer has opened above it. It is NOT "paused" or "stopped."
 */
enum class SceneLifecycle {
    /** Scene has been created but not yet visible. */
    CREATED,

    /** Scene is fully visible and receiving interaction. */
    ACTIVE,

    /**
     * Scene is alive but de-emphasized. Still rendering, still in memory.
     * Another layer has opened above — this scene's visual properties
     * have been adjusted (blur, dim, scale) per LayerBehavior rules.
     *
     * This is NOT "paused" — the scene is still technically visible,
     * just visually recessed to create depth hierarchy.
     */
    DEEMPHASIZED,

    /**
     * Scene has been navigated away from but remains in the back stack.
     * Not rendering, but state is preserved for potential return.
     */
    BACKGROUND,

    /** Scene has been permanently removed. Resources freed. */
    DESTROYED
}

// ═══════════════════════════════════════════════════════════════════════════════
// 5. FOCUS OWNERSHIP — Who owns the user's attention
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Defines which layer currently owns the user's focus.
 *
 * Focus ownership determines:
 *   - Who receives touch events
 *   - Who receives system back press
 *   - Which layer's background is "alive" vs "de-emphasized"
 *
 * From Spec Section 4.3: "Layer means depth, priority, interaction ownership,
 * focus state, visibility state."
 */
enum class FocusOwner {
    /** Base scene (Library/Playlists/Search/Settings) owns focus. */
    BASE,

    /** Focus scene (NowPlaying, detail screen) owns focus. */
    FOCUS_SCENE,

    /** Panel (Equalizer, Timer, etc.) owns focus. */
    PANEL,

    /** Overlay (context menu, tooltip) owns focus. */
    OVERLAY,

    /** No layer owns focus (transitioning state). */
    TRANSITIONING
}

// ═══════════════════════════════════════════════════════════════════════════════
// 6. VISUAL DEPTH MODEL — Foreground / Middle / Background
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Three-plane depth model from Spec Section 6.5.
 *
 * Users must immediately perceive which content is foreground (crisp),
 * which is middle (translucent), and which is background (soft, reactive).
 * This is not z-index alone — it's a visual treatment applied to content
 * based on its depth plane.
 */
enum class DepthPlane {
    /**
     * Foreground — crisp, full opacity, full interaction.
     * The currently active scene or panel.
     */
    FOREGROUND,

    /**
     * Middle — translucent, reduced interaction.
     * Scenes that are de-emphasized but still visible.
     * Glass effect applied: blur + dim + slight scale-down.
     */
    MIDDLE,

    /**
     * Background — soft, reactive, no direct interaction.
     * Living background layer. Album-art-driven. Breathing.
     * Never dead (Spec Section 6.3).
     */
    BACKGROUND
}

// ═══════════════════════════════════════════════════════════════════════════════
// 7. GLASS TIER — Glass intensity per layer
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Glass intensity tiers for different layer contexts.
 *
 * From Spec Section 6.1: "Glass is not decoration. It is hierarchy,
 * readability, depth, separation, premium surface."
 *
 * Each tier defines how prominent the glass treatment is for content
 * at that depth plane. Foreground content has minimal glass (it IS the
 * content), while middle-layer content needs glass to be readable against
 * the background behind it.
 */
enum class GlassTier {
    /** No glass — content is the primary surface (NowPlaying controls, etc.) */
    NONE,

    /** Light glass — subtle frosted treatment (cards within active scene). */
    LIGHT,

    /** Medium glass — clear frosted separation (panels floating above scenes). */
    MEDIUM,

    /** Heavy glass — strong frosted treatment (de-emphasized layers). */
    HEAVY,

    /** Full glass — maximum frosted separation (overlay backgrounds, scrims). */
    FULL
}

// ═══════════════════════════════════════════════════════════════════════════════
// 8. TRANSITION TYPES — How layers move
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Types of transitions in RIKO Visual OS.
 *
 * From Spec Section 10.6:
 *   - open = lift + blur background + scale focus layer
 *   - close = reverse lift + restore background
 *   - switch = context handoff
 *   - modal = floating panel, not activity swap
 *
 * Every transition is keyframe-driven (Spec Section 7.4):
 *   0ms → 80ms → 140ms → 220ms → 300ms
 */
enum class TransitionType {
    /** A new layer pushes on top. Background de-emphasizes. */
    PUSH,

    /** Top layer pops off. Background restores. */
    POP,

    /** Focus transfers between same-level scenes (Library → Search). */
    SWITCH,

    /** Handoff from Dock to Focus (MiniPlayer → NowPlaying morph). */
    HANDOFF,

    /** Panel appears as floating instrument. */
    PANEL_OPEN,

    /** Panel disappears with reverse motion. */
    PANEL_CLOSE,

    /** Overlay appears (brief, targeted). */
    OVERLAY_SHOW,

    /** Overlay disappears. */
    OVERLAY_HIDE,

    /** Dock appears (song started). */
    DOCK_APPEAR,

    /** Dock disappears (song ended or NowPlaying active). */
    DOCK_HIDE,

    /** No transition — scene is already in desired state. */
    NONE
}

// ═══════════════════════════════════════════════════════════════════════════════
// 9. MOTION VOCABULARY — The language of movement
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Motion vocabulary from Spec Section 13.
 *
 * These are not animation presets — they are semantic motion types that
 * communicate meaning to the user. Every motion in RIKO must use one of
 * these vocabulary words (Spec Section 7.1: "Motion must explain").
 *
 * Implementation details (spring constants, durations) live in
 * VisualOsTokens.kt and TransitionChoreographer.kt.
 * This enum defines the INTENT, not the math.
 */
enum class MotionWord {
    /**
     * Damped oscillation — like a physical spring settling.
     * Used for: panel lift, button press, card expand.
     * Feels: natural, physical, alive.
     */
    SPRING,

    /**
     * Gradual deceleration to rest — like a ball rolling to a stop.
     * Used for: scene transitions, scroll deceleration.
     * Feels: calm, controlled, intentional.
     */
    SETTLE,

    /**
     * Slow, continuous, ambient movement — like clouds drifting.
     * Used for: background breathing, wave ambient, idle motion.
     * Feels: alive, organic, never dead (Spec Section 6.3).
     */
    DRIFT,

    /**
     * Focus transfer — smooth handoff between two layers.
     * Used for: MiniPlayer → NowPlaying, tab switch.
     * Feels: seamless, connected, choreographed (Spec Section 5).
     */
    HANDOFF,

    /**
     * Sinusoidal pulsing — like a heartbeat or breathing.
     * Used for: background alive movement, progress pulse, wave energy.
     * Feels: organic, rhythmic, living (Spec Section 6.3: "breath-like").
     */
    BREATHING,

    /**
     * Overshoot then return — like pulling a rubber band.
     * Used for: scrubber elastic, gesture overshoot, bounce-back.
     * Feels: responsive, playful, physical (Spec Section 8.3).
     */
    ELASTIC,

    /**
     * Fade in/out — simple opacity change.
     * Used ONLY for: scrim dimming, subtle content transitions.
     * NOT for major transitions — those must have spatial motion.
     * Feels: gentle, minimal, respectful (Rule #74: Silence Is Also UX).
     */
    FADE,

    /**
     * Vertical movement — lift from bottom or drop to bottom.
     * Used for: panel open/close, sheet presentation.
     * Feels: natural, bottom-oriented (Spec Section 11.2).
     */
    LIFT,

    /**
     * Scale change — growing or shrinking to indicate depth.
     * Used for: background de-emphasis, focus emphasis, zoom.
     * Feels: spatial, depth-related, hierarchical (Spec Section 4.2).
     */
    SCALE
}

// ═══════════════════════════════════════════════════════════════════════════════
// 10. GLOSSARY — Shared vocabulary (Spec Section 16)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Glossary of RIKO Visual OS terms.
 *
 * These terms are used throughout the codebase. When a developer encounters
 * a term like "Handoff" or "Breathing" in code, they can trace it back
 * to this glossary for the canonical definition.
 *
 * From Spec Section 16 — "Core Glossary."
 */
object VisualOsGlossary {
    /** One visual state of the app. NOT a "page." */
    const val SCENE = "Scene"

    /** Depth plane inside a scene. NOT z-index alone. */
    const val LAYER = "Layer"

    /** Glass card / panel / object — a visual surface element. */
    const val SURFACE = "Surface"

    /** Background scene staying alive while focus layer is active. */
    const val CONTEXT = "Context"

    /** Active user-owned scene. */
    const val FOCUS = "Focus"

    /** Temporary top layer. */
    const val OVERLAY = "Overlay"

    /** Persistent small surface (Mini Player). */
    const val DOCK = "Dock"

    /** Focus transfer between layers. */
    const val HANDOFF = "Handoff"

    /** Timed animation checkpoint (0ms, 80ms, 140ms, 220ms, 300ms). */
    const val KEYFRAME = "Keyframe"

    /** Attention reduction / depth signal. */
    const val BLUR = "Blur"

    /** Visibility control. */
    const val OPACITY = "Opacity"

    /** Depth cue via size change. */
    const val SCALE = "Scale"

    /** Perceived height above background. */
    const val ELEVATION = "Elevation"

    /** Glass realism effect — top-left catch light. */
    const val REFLECTION = "Reflection"

    /** Focus / energy feedback — soft light emission. */
    const val GLOW = "Glow"

    /** Soft alive movement — background never dies (Spec Section 6.3). */
    const val BREATHING = "Breathing"
}

// ═══════════════════════════════════════════════════════════════════════════════
// 11. DESIGN PROMISE — Five tests every scene must pass
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * The five-point quality test from Spec Section 20.
 *
 * Every scene, every transition, every visual state in RIKO must pass
 * ALL five of these tests. If any test fails — redesign.
 *
 * This is enforced in ValidationPreviewKit (Rule #77: Preview Is Documentation)
 * and in IdentityTestProtocol (M2.9 Gate 6).
 */
object VisualOsDesignPromise {
    /** 1. Does it feel premium? */
    const val PREMIUM = "premium"

    /** 2. Does it feel smooth? */
    const val SMOOTH = "smooth"

    /** 3. Does it feel alive? */
    const val ALIVE = "alive"

    /** 4. Does it preserve hierarchy? */
    const val HIERARCHY = "hierarchy"

    /** 5. Does it look like RIKO? */
    const val IDENTITY = "identity"

    /** All five tests as a set. */
    val allTests: Set<String> = setOf(PREMIUM, SMOOTH, ALIVE, HIERARCHY, IDENTITY)
}

// ═══════════════════════════════════════════════════════════════════════════════
// 12. CORE DESIGN PRINCIPLES — Ordered by priority (Spec Section 3)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Design philosophy priorities from Spec Section 3.
 *
 * These are NOT equal — they have a strict priority order.
 * When two principles conflict, the higher-priority one wins.
 * For example: Performance (6) must NEVER compromise Identity (1).
 */
object DesignPriority {
    /** 1. Premium first — user must feel premium immediately. */
    const val P1_PREMIUM = "Premium First"

    /** 2. Motion second — all motion smooth, natural, purposeful. */
    const val P2_MOTION = "Motion Second"

    /** 3. Now Playing third — music context hero surface. */
    const val P3_NOW_PLAYING = "Now Playing Third"

    /** 4. Glass fourth — glass defines visual hierarchy. */
    const val P4_GLASS = "Glass Fourth"

    /** 5. Music function fifth — UI doesn't overtake function. */
    const val P5_FUNCTION = "Music Function Fifth"

    /** 6. Performance last — optimize without destroying identity. */
    const val P6_PERFORMANCE = "Performance Last"

    /**
     * Priority order — index 0 is highest priority.
     * When conflict arises, lower index wins.
     */
    val order: List<String> = listOf(
        P1_PREMIUM, P2_MOTION, P3_NOW_PLAYING,
        P4_GLASS, P5_FUNCTION, P6_PERFORMANCE
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// 13. IMPLEMENTATION RULES — From Spec Section 18
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Concrete implementation rules for the Visual OS team.
 *
 * From Spec Section 18 — "Implementation notes for the UI team."
 * These rules guide every code decision in the visualos/ package.
 */
object VisualOsImplementationRules {

    // ── Architecture Rules ────────────────────────────────────────────────

    /**
     * Prefer scene host + layered surfaces over traditional page navigation.
     * RIKO's visual model is layered, not page-based.
     */
    const val PREFER_SCENE_HOST = "Prefer scene host + layered surfaces"

    /**
     * All animations must use explicit duration and easing from tokens.
     * Never hardcode animation values.
     */
    const val EXPLICIT_ANIMATION_SPECS = "Use animation specs with explicit duration and easing"

    /**
     * Background rendering is separate from foreground content.
     * They are two independent rendering paths that composite together.
     */
    const val SEPARATE_BACKGROUND = "Keep background separate from foreground content"

    /**
     * Use snapshots for state-driven rendering (Rule #3).
     * State is computed once, then rendered from snapshot.
     */
    const val SNAPSHOT_RENDERING = "Use snapshots for state-driven rendering"

    /**
     * Overlay must not couple directly to playback logic.
     * Overlay reads from state, not from PlayerManager directly.
     */
    const val DECOUPLED_OVERLAY = "Avoid coupling overlay directly to playback logic"

    /**
     * All transition values must be deterministic.
     * Same input state → same output transition.
     */
    const val DETERMINISTIC_TRANSITIONS = "Keep transition values deterministic"

    /**
     * Panel sizes are adaptive but bounded.
     * Never full-screen for panels (Spec Section 17.5).
     */
    const val BOUNDED_PANELS = "Make panel sizes adaptive but bounded"

    /**
     * Background blur must be progressive, not instant.
     * Animates from 0 blur → target blur over keyframe duration.
     */
    const val PROGRESSIVE_BLUR = "Make background blur progressive, not instant"

    // ── Visual Rules ────────────────────────────────────────────────────

    /**
     * No over-coloring. Too much color kills premium feel.
     * (Spec Section 17.1)
     */
    const val NO_OVER_COLORING = "No over-coloring"

    /**
     * No random effects. Every glow, blur, and motion must have purpose.
     * (Spec Section 17.2)
     */
    const val NO_RANDOM_EFFECTS = "No random effects"

    /**
     * No visual noise. If something doesn't help hierarchy, remove it.
     * (Spec Section 17.3)
     */
    const val NO_VISUAL_NOISE = "No visual noise"

    /**
     * No dead background. Background should react, but softly.
     * (Spec Section 17.4)
     */
    const val NO_DEAD_BACKGROUND = "No dead background"

    /**
     * No full-screen everything. Panels stay as elegant floating layers.
     * (Spec Section 17.5)
     */
    const val NO_FULL_SCREEN_EVERYTHING = "No full-screen everything"
}

// ═══════════════════════════════════════════════════════════════════════════════
// 14. TRANSITION GRAMMAR — From Spec Section 10.6
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * HarmonyOS-inspired transition grammar.
 *
 * From Spec Section 10.6:
 *   - open = lift + blur background + scale focus layer
 *   - close = reverse lift + restore background
 *   - switch = context handoff
 *   - modal = floating panel, not activity swap
 */
object TransitionGrammar {
    const val OPEN = "lift + blur_background + scale_focus"
    const val CLOSE = "reverse_lift + restore_background"
    const val SWITCH = "context_handoff"
    const val MODAL = "floating_panel"
    const val NAVIGATION = "choreography"
    const val SCREEN = "alive_scene"
    const val TRANSITION = "handoff_not_replacement"
}

// ═══════════════════════════════════════════════════════════════════════════════
// 15. ERGONOMIC RULES — One UI inspired (Spec Section 11)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Ergonomic design rules inspired by Samsung One UI.
 *
 * From Spec Section 11:
 *   - Comfortable touch areas
 *   - Bottom-oriented controls
 *   - Natural hand-friendly layout
 *   - Soft rounded surfaces
 *   - Clean typography hierarchy
 */
object ErgonomicRules {
    /** Minimum touch target size in dp. */
    const val MIN_TOUCH_TARGET_DP = 48

    /** Controls should be in bottom 60% of screen for thumb reach. */
    const val THUMB_ZONE_FRACTION = 0.6f

    /** Default corner radius for soft surfaces. */
    const val SOFT_CORNER_RADIUS_DP = 16

    /** Maximum number of hierarchy levels in typography. */
    const val MAX_TEXT_HIERARCHY_LEVELS = 4

    /**
     * Typography hierarchy:
     *   1. Primary title (largest, boldest)
     *   2. Secondary metadata (medium, regular weight)
     *   3. Small supporting labels (small, light weight)
     *   4. No unnecessary text noise
     */
    const val TEXT_HIERARCHY = "primary_title → secondary_metadata → small_labels"
}
