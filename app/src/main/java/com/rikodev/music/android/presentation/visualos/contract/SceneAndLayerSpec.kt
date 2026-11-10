package com.rikodev.music.android.presentation.visualos.contract

// ─────────────────────────────────────────────────────────────────────────────
// RIKO VISUAL OS — SCENE & LAYER SPEC
//
// Defines the runtime contract for individual scenes and layers.
// What data does a scene carry? What capabilities does it declare?
// What visual parameters apply to it?
//
// This is the "model" layer of the Visual OS contract.
// State machine transitions consume these specs.
// ─────────────────────────────────────────────────────────────────────────────

// ═══════════════════════════════════════════════════════════════════════════════
// 1. SCENE SPECIFICATION — Full descriptor for a scene instance
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Complete specification for a scene instance in the Visual OS.
 *
 * Each scene in the scene stack has one of these specs.
 * It declares:
 *   - What type of scene it is (Library, NowPlaying, etc.)
 *   - What layer type it belongs to (Base, Focus, Panel, etc.)
 *   - What its lifecycle state is (Active, Deemphasized, etc.)
 *   - What visual parameters apply (blur, opacity, scale, elevation)
 *   - What capabilities it has (can show panels, has hero, etc.)
 */
data class SceneSpec(
    // ── Identity ──────────────────────────────────────────────────────────

    /** Unique identifier for this scene instance. */
    val id: SceneId,

    /** The type of scene (Library, NowPlaying, etc.). */
    val type: RikoSceneType,

    /** The layer type this scene belongs to (Base, Focus, Panel, etc.). */
    val layerType: RikoLayerType,

    // ── Lifecycle ────────────────────────────────────────────────────────

    /** Current lifecycle state of this scene. */
    val lifecycle: SceneLifecycle = SceneLifecycle.CREATED,

    /** When this scene was created (epoch millis for ordering). */
    val createdAtMs: Long = System.currentTimeMillis(),

    // ── Visual State ────────────────────────────────────────────────────

    /** Current visual parameters — animated by TransitionChoreographer. */
    val visualState: VisualState = VisualState.defaultForLayer(layerType),

    /** Depth plane based on current visual state and layer stack position. */
    val depthPlane: DepthPlane = DepthPlane.FOREGROUND,

    /** Glass tier based on layer type and depth plane. */
    val glassTier: GlassTier = GlassTier.NONE,

    // ── Capabilities ────────────────────────────────────────────────────

    /** What this scene can do. */
    val capabilities: SceneCapabilities = SceneCapabilities.forType(type),

    // ── Context ─────────────────────────────────────────────────────────

    /** Arbitrary contextual data this scene needs (e.g., playlistId). */
    val contextData: Map<String, Any> = emptyMap(),

    /** Whether this scene should block back navigation. */
    val isBackBlocking: Boolean = false
) {
    /** Unique stable key for this scene (never changes). */
    val routeKey: String get() = type.routeKey

    /** Whether this scene is currently visible to the user. */
    val isVisible: Boolean
        get() = lifecycle == SceneLifecycle.ACTIVE || lifecycle == SceneLifecycle.DEEMPHASIZED

    /** Whether this scene is the topmost scene in its layer type. */
    val isTopOfStack: Boolean = true // Updated by reducer

    companion object {
        /** Create a new scene spec with defaults. */
        fun create(
            type: RikoSceneType,
            contextData: Map<String, Any> = emptyMap()
        ): SceneSpec = SceneSpec(
            id = SceneId.generate(),
            type = type,
            layerType = resolveLayerType(type),
            visualState = VisualState.defaultForLayer(resolveLayerType(type)),
            contextData = contextData
        )

        /** Resolve layer type from scene type. */
        fun resolveLayerType(sceneType: RikoSceneType): RikoLayerType = when (sceneType) {
            is RikoSceneType.Library -> RikoLayerType.BASE
            is RikoSceneType.Playlists -> RikoLayerType.BASE
            is RikoSceneType.Search -> RikoLayerType.BASE
            is RikoSceneType.Settings -> RikoLayerType.BASE
            is RikoSceneType.NowPlaying -> RikoLayerType.FOCUS
            is RikoSceneType.PlaylistDetail -> RikoLayerType.FOCUS
            is RikoSceneType.EqualizerPanel -> RikoLayerType.PANEL
            is RikoSceneType.SleepTimerPanel -> RikoLayerType.PANEL
            is RikoSceneType.SongInfoPanel -> RikoLayerType.PANEL
            is RikoSceneType.QueuePanel -> RikoLayerType.PANEL
            is RikoSceneType.ContextMenu -> RikoLayerType.OVERLAY
            is RikoSceneType.QuickInfo -> RikoLayerType.OVERLAY
            is RikoSceneType.MiniPlayerDock -> RikoLayerType.DOCK
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 2. SCENE ID — Stable identifier for scene instances
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Stable identifier for a scene instance.
 *
 * Unlike navigation routes (which are reusable), a SceneId is unique
 * per instance. This allows tracking specific scene instances through
 * the scene stack even when multiple scenes of the same type exist
 * (e.g., two PlaylistDetail scenes).
 */
@JvmInline
value class SceneId(val value: String) {
    companion object {
        private var counter = 0L

        /** Generate a new unique scene ID. */
        fun generate(): SceneId = SceneId("scene_${counter++}_${System.nanoTime()}")
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 3. VISUAL STATE — Animated visual parameters for a scene
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Current visual parameters for a scene/layer.
 *
 * These values are animated by the TransitionChoreographer.
 * They represent the CURRENT interpolated state during transitions,
 * not just the start/end values.
 *
 * Rendering code reads these values directly — no animation logic needed
 * in composables (Rule #60: Screen State Is Already Renderable).
 */
data class VisualState(
    /** Current opacity (0.0 = invisible, 1.0 = fully visible). */
    val opacity: Float = 1.0f,

    /** Current blur radius applied to this layer's background. */
    val blur: Float = 0f, // dp as float for animation

    /** Current scale factor (1.0 = natural size). */
    val scale: Float = 1.0f,

    /** Current translation Y offset (positive = lower, negative = higher). */
    val translationY: Float = 0f,

    /** Current elevation (dp). */
    val elevation: Float = 0f,

    /** Current dim overlay alpha (0.0 = no dim, 1.0 = fully dimmed). */
    val dimAlpha: Float = 0f,

    /** Whether content is currently interactive. */
    val isInteractive: Boolean = true,

    /** Animation progress for entry (0.0 = just started, 1.0 = complete). */
    val entryProgress: Float = 1.0f
) {
    companion object {
        /** Default visual state for a layer type. */
        fun defaultForLayer(layerType: RikoLayerType): VisualState = when (layerType) {
            RikoLayerType.BASE -> VisualState(
                opacity = 1.0f,
                blur = 0f,
                scale = 1.0f,
                elevation = 0f,
                dimAlpha = 0f,
                isInteractive = true,
                entryProgress = 1.0f
            )
            RikoLayerType.FOCUS -> VisualState(
                opacity = 0f,
                scale = ScaleTokens.PANEL_ENTER_START,
                elevation = ElevationTokens.Focus.value,
                isInteractive = false,
                entryProgress = 0f
            )
            RikoLayerType.PANEL -> VisualState(
                opacity = 0f,
                scale = ScaleTokens.PANEL_ENTER_START,
                translationY = 100f, // starts below
                elevation = ElevationTokens.Panel.value,
                isInteractive = false,
                entryProgress = 0f
            )
            RikoLayerType.OVERLAY -> VisualState(
                opacity = 0f,
                scale = ScaleTokens.OVERLAY_ENTER_START,
                elevation = ElevationTokens.Overlay.value,
                isInteractive = false,
                entryProgress = 0f
            )
            RikoLayerType.DOCK -> VisualState(
                opacity = 0f,
                scale = 0.9f,
                elevation = ElevationTokens.Dock.value,
                isInteractive = true,
                entryProgress = 0f
            )
        }

        /** Target visual state for a fully active layer. */
        fun activeForLayer(layerType: RikoLayerType): VisualState = when (layerType) {
            RikoLayerType.BASE -> VisualState(
                opacity = 1.0f,
                blur = 0f,
                scale = 1.0f,
                elevation = 0f,
                dimAlpha = 0f,
                isInteractive = true,
                entryProgress = 1.0f
            )
            RikoLayerType.FOCUS -> VisualState(
                opacity = 1.0f,
                scale = 1.0f,
                elevation = ElevationTokens.Focus.value,
                isInteractive = true,
                entryProgress = 1.0f
            )
            RikoLayerType.PANEL -> VisualState(
                opacity = 1.0f,
                scale = 1.0f,
                translationY = 0f,
                elevation = ElevationTokens.Panel.value,
                isInteractive = true,
                entryProgress = 1.0f
            )
            RikoLayerType.OVERLAY -> VisualState(
                opacity = 1.0f,
                scale = 1.0f,
                elevation = ElevationTokens.Overlay.value,
                isInteractive = true,
                entryProgress = 1.0f
            )
            RikoLayerType.DOCK -> VisualState(
                opacity = 1.0f,
                scale = 1.0f,
                elevation = ElevationTokens.Dock.value,
                isInteractive = true,
                entryProgress = 1.0f
            )
        }

        /** De-emphasized visual state for a layer (something opened above it). */
        fun deemphasizedForLayer(layerType: RikoLayerType): VisualState = when (layerType) {
            RikoLayerType.BASE -> VisualState(
                opacity = OpacityTokens.deEmphasisSteps.last(),
                blur = BlurTokens.deEmphasisSteps.last().value,
                scale = ScaleTokens.deEmphasisSteps.last(),
                dimAlpha = OpacityTokens.DIM_FOR_FOCUS,
                isInteractive = false,
                entryProgress = 1.0f
            )
            RikoLayerType.FOCUS -> VisualState(
                opacity = OpacityTokens.deEmphasisSteps[3],
                blur = BlurTokens.deEmphasisSteps[3].value,
                scale = ScaleTokens.deEmphasisSteps[3],
                dimAlpha = OpacityTokens.DIM_FOR_PANEL,
                isInteractive = false,
                entryProgress = 1.0f
            )
            else -> defaultForLayer(layerType) // Panel/Overlay/Dock don't get de-emphasized
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 4. SCENE CAPABILITIES — What a scene can do
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Capabilities that a scene declares.
 *
 * Different scene types have different capabilities.
 * NowPlaying can show panels (EQ, timer), Library cannot directly.
 * This prevents invalid scene combinations.
 */
data class SceneCapabilities(
    /** Whether this scene can host panel layers above it. */
    val canShowPanels: Boolean = false,

    /** Whether this scene has a hero section (Rule #57). */
    val hasHero: Boolean = false,

    /** Whether this scene supports wave ambient (background wave). */
    val hasWaveAmbient: Boolean = false,

    /** Whether this scene has a living background (album art driven). */
    val hasLivingBackground: Boolean = false,

    /** Whether this scene coexists with the dock. */
    val coexistsWithDock: Boolean = true,

    /** Whether this scene supports context menu overlay. */
    val supportsContextMenu: Boolean = true,

    /** Whether this scene has bottom-oriented controls (Spec Section 11.2). */
    val hasBottomControls: Boolean = false,

    /** Whether this scene's background reacts to album art. */
    val hasAlbumArtBackground: Boolean = false
) {
    companion object {
        /** Get capabilities for a given scene type. */
        fun forType(type: RikoSceneType): SceneCapabilities = when (type) {
            is RikoSceneType.Library -> SceneCapabilities(
                canShowPanels = false,
                hasHero = true,
                hasWaveAmbient = true,
                hasLivingBackground = true,
                coexistsWithDock = true,
                supportsContextMenu = true,
                hasBottomControls = false,
                hasAlbumArtBackground = false
            )
            is RikoSceneType.Playlists -> SceneCapabilities(
                canShowPanels = false,
                hasHero = true,
                hasWaveAmbient = true,
                hasLivingBackground = true,
                coexistsWithDock = true,
                hasAlbumArtBackground = false
            )
            is RikoSceneType.Search -> SceneCapabilities(
                canShowPanels = false,
                hasHero = false,
                hasWaveAmbient = true,
                hasLivingBackground = true,
                coexistsWithDock = true,
                hasBottomControls = false,
                hasAlbumArtBackground = false
            )
            is RikoSceneType.Settings -> SceneCapabilities(
                canShowPanels = false,
                hasHero = false,
                hasWaveAmbient = false,
                hasLivingBackground = false,
                coexistsWithDock = true,
                hasAlbumArtBackground = false
            )
            is RikoSceneType.NowPlaying -> SceneCapabilities(
                canShowPanels = true,
                hasHero = true,
                hasWaveAmbient = true,
                hasLivingBackground = true,
                coexistsWithDock = false, // Dock morphs to NP
                hasBottomControls = true,
                hasAlbumArtBackground = true
            )
            is RikoSceneType.PlaylistDetail -> SceneCapabilities(
                canShowPanels = false,
                hasHero = true,
                hasWaveAmbient = true,
                hasLivingBackground = true,
                coexistsWithDock = true,
                hasAlbumArtBackground = true
            )
            is RikoSceneType.EqualizerPanel,
            is RikoSceneType.SleepTimerPanel,
            is RikoSceneType.SongInfoPanel,
            is RikoSceneType.QueuePanel -> SceneCapabilities(
                canShowPanels = false,
                hasHero = false,
                hasWaveAmbient = false,
                hasLivingBackground = false,
                coexistsWithDock = true,
                hasAlbumArtBackground = false
            )
            is RikoSceneType.ContextMenu,
            is RikoSceneType.QuickInfo -> SceneCapabilities(
                canShowPanels = false,
                hasHero = false,
                hasWaveAmbient = false,
                hasLivingBackground = false,
                coexistsWithDock = true,
                supportsContextMenu = false,
                hasAlbumArtBackground = false
            )
            is RikoSceneType.MiniPlayerDock -> SceneCapabilities(
                canShowPanels = false,
                hasHero = false,
                hasWaveAmbient = false,
                hasLivingBackground = false,
                coexistsWithDock = true,
                supportsContextMenu = true,
                hasAlbumArtBackground = false
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 5. TRANSITION SPEC — What happens during a transition
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Specification for a single transition between visual states.
 *
 * This describes WHAT transitions happen and HOW they're choreographed.
 * The TransitionChoreographer reads this to compute actual animation values.
 *
 * From Spec Section 7.4: "All motion is timeline-based."
 * Each transition has a defined type, duration, motion word,
 * and per-property behavior.
 */
data class TransitionSpec(
    /** What type of transition this is. */
    val type: TransitionType,

    /** The scene being transitioned (entering or leaving). */
    val targetSceneId: SceneId,

    /** The scene below (receiving de-emphasis or restoration). */
    val backgroundSceneId: SceneId? = null,

    /** Duration of the transition in milliseconds. */
    val durationMs: Int = KeyframeTimings.SCENE_TRANSITION,

    /** Motion vocabulary for this transition. */
    val motionWord: MotionWord = MotionWord.SPRING,

    /** Spring config for physics-based animation. */
    val spring: SpringConfig = SpringTokens.Smooth,

    /** Keyframe checkpoints for this transition. */
    val keyframes: KeyframeSequence = KeyframeSequence.defaultForType(type),

    /** Whether this transition can be interrupted by another transition. */
    val isInterruptible: Boolean = true,

    /** Whether to animate background de-emphasis/restoration. */
    val animateBackground: Boolean = true,

    /** Whether this is an enter (true) or exit (false) transition. */
    val isEnter: Boolean = true
) {
    companion object {
        /** Create spec for pushing a scene. */
        fun push(target: SceneSpec, background: SceneSpec? = null): TransitionSpec =
            TransitionSpec(
                type = TransitionType.PUSH,
                targetSceneId = target.id,
                backgroundSceneId = background?.id,
                durationMs = when (target.layerType) {
                    RikoLayerType.FOCUS -> KeyframeTimings.SCENE_TRANSITION
                    RikoLayerType.PANEL -> KeyframeTimings.PANEL_TRANSITION
                    RikoLayerType.OVERLAY -> KeyframeTimings.OVERLAY_TRANSITION
                    RikoLayerType.DOCK -> KeyframeTimings.DOCK_TRANSITION
                    RikoLayerType.BASE -> KeyframeTimings.BASE_SWITCH_TRANSITION
                },
                motionWord = when (target.layerType) {
                    RikoLayerType.FOCUS -> MotionWord.SPRING
                    RikoLayerType.PANEL -> MotionWord.LIFT
                    RikoLayerType.OVERLAY -> MotionWord.SCALE
                    RikoLayerType.DOCK -> MotionWord.SPRING
                    RikoLayerType.BASE -> MotionWord.HANDOFF
                },
                spring = when (target.layerType) {
                    RikoLayerType.FOCUS -> SpringTokens.Smooth
                    RikoLayerType.PANEL -> SpringTokens.Comfortable
                    RikoLayerType.OVERLAY -> SpringTokens.Snappy
                    RikoLayerType.DOCK -> SpringTokens.Snappy
                    RikoLayerType.BASE -> SpringTokens.Smooth
                },
                keyframes = KeyframeSequence.defaultForType(TransitionType.PUSH),
                isEnter = true,
                animateBackground = target.layerType != RikoLayerType.OVERLAY
            )

        /** Create spec for popping a scene. */
        fun pop(target: SceneSpec, background: SceneSpec): TransitionSpec =
            TransitionSpec(
                type = TransitionType.POP,
                targetSceneId = target.id,
                backgroundSceneId = background.id,
                durationMs = when (target.layerType) {
                    RikoLayerType.FOCUS -> KeyframeTimings.SCENE_TRANSITION
                    RikoLayerType.PANEL -> KeyframeTimings.PANEL_TRANSITION
                    RikoLayerType.OVERLAY -> KeyframeTimings.OVERLAY_TRANSITION
                    RikoLayerType.DOCK -> KeyframeTimings.DOCK_TRANSITION
                    RikoLayerType.BASE -> KeyframeTimings.BASE_SWITCH_TRANSITION
                },
                motionWord = when (target.layerType) {
                    RikoLayerType.PANEL -> MotionWord.LIFT
                    RikoLayerType.OVERLAY -> MotionWord.FADE
                    else -> MotionWord.SPRING
                },
                isEnter = false,
                animateBackground = true
            )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 6. KEYFRAME SEQUENCE — Timed animation checkpoints
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * A sequence of keyframes defining a transition's timeline.
 *
 * From Spec Section 7.4:
 *   0ms: hidden
 *   80ms: lift starts
 *   140ms: blur starts
 *   220ms: settle
 *   300ms: active
 *
 * Each keyframe defines the target values for visual properties at that
 * point in time. The TransitionChoreographer interpolates between keyframes.
 */
data class KeyframeSequence(
    /** Ordered list of keyframe checkpoints. */
    val checkpoints: List<KeyframeCheckpoint>
) {
    companion object {
        /** Default keyframe sequence for a given transition type. */
        fun defaultForType(type: TransitionType): KeyframeSequence = when (type) {
            TransitionType.PUSH -> pushKeyframes
            TransitionType.POP -> popKeyframes
            TransitionType.SWITCH -> switchKeyframes
            TransitionType.HANDOFF -> handoffKeyframes
            TransitionType.PANEL_OPEN -> panelOpenKeyframes
            TransitionType.PANEL_CLOSE -> panelCloseKeyframes
            TransitionType.OVERLAY_SHOW -> overlayShowKeyframes
            TransitionType.OVERLAY_HIDE -> overlayHideKeyframes
            TransitionType.DOCK_APPEAR -> dockAppearKeyframes
            TransitionType.DOCK_HIDE -> dockHideKeyframes
            TransitionType.NONE -> KeyframeSequence(emptyList())
        }

        // ── Push Keyframes (Spec Section 7.4 exact values) ──────────

        private val pushKeyframes = KeyframeSequence(listOf(
            KeyframeCheckpoint(
                timeMs = KeyframeTimings.CHECKPOINT_0,
                enteringOpacity = 0f,
                enteringScale = ScaleTokens.ENTER_START,
                enteringTranslationY = 0f,
                backgroundBlur = 0f,
                backgroundScale = ScaleTokens.NATURAL,
                backgroundDim = 0f,
                label = "hidden"
            ),
            KeyframeCheckpoint(
                timeMs = KeyframeTimings.CHECKPOINT_1,
                enteringOpacity = 0.3f,
                enteringScale = 0.96f,
                enteringTranslationY = -4f,
                backgroundBlur = 2f,
                backgroundScale = 0.995f,
                backgroundDim = 0.03f,
                label = "lift_starts"
            ),
            KeyframeCheckpoint(
                timeMs = KeyframeTimings.CHECKPOINT_2,
                enteringOpacity = 0.65f,
                enteringScale = 0.98f,
                enteringTranslationY = -2f,
                backgroundBlur = 8f,
                backgroundScale = ScaleTokens.SLIGHT_SHRINK,
                backgroundDim = 0.08f,
                label = "blur_starts"
            ),
            KeyframeCheckpoint(
                timeMs = KeyframeTimings.CHECKPOINT_3,
                enteringOpacity = 0.9f,
                enteringScale = 0.99f,
                enteringTranslationY = 0f,
                backgroundBlur = 16f,
                backgroundScale = 0.97f,
                backgroundDim = 0.12f,
                label = "settle"
            ),
            KeyframeCheckpoint(
                timeMs = KeyframeTimings.CHECKPOINT_4,
                enteringOpacity = 1f,
                enteringScale = 1f,
                enteringTranslationY = 0f,
                backgroundBlur = BlurTokens.Heavy.value,
                backgroundScale = ScaleTokens.MEDIUM_SHRINK,
                backgroundDim = OpacityTokens.DIM_FOR_FOCUS,
                label = "active"
            )
        ))

        // ── Pop Keyframes (reverse of push) ──────────────────────────

        private val popKeyframes = KeyframeSequence(listOf(
            KeyframeCheckpoint(
                timeMs = 0,
                enteringOpacity = 1f,
                enteringScale = 1f,
                enteringTranslationY = 0f,
                backgroundBlur = BlurTokens.Heavy.value,
                backgroundScale = ScaleTokens.MEDIUM_SHRINK,
                backgroundDim = OpacityTokens.DIM_FOR_FOCUS,
                label = "active"
            ),
            KeyframeCheckpoint(
                timeMs = 80,
                enteringOpacity = 0.9f,
                enteringScale = 0.99f,
                enteringTranslationY = 2f,
                backgroundBlur = 16f,
                backgroundScale = 0.97f,
                backgroundDim = 0.12f,
                label = "lift_starts"
            ),
            KeyframeCheckpoint(
                timeMs = 140,
                enteringOpacity = 0.65f,
                enteringScale = 0.98f,
                enteringTranslationY = 4f,
                backgroundBlur = 8f,
                backgroundScale = ScaleTokens.SLIGHT_SHRINK,
                backgroundDim = 0.08f,
                label = "blur_fading"
            ),
            KeyframeCheckpoint(
                timeMs = 220,
                enteringOpacity = 0.3f,
                enteringScale = 0.96f,
                backgroundBlur = 2f,
                backgroundScale = 0.995f,
                backgroundDim = 0.03f,
                label = "settle"
            ),
            KeyframeCheckpoint(
                timeMs = 300,
                enteringOpacity = 0f,
                enteringScale = ScaleTokens.ENTER_START,
                backgroundBlur = 0f,
                backgroundScale = ScaleTokens.NATURAL,
                backgroundDim = 0f,
                label = "gone"
            )
        ))

        // ── Switch Keyframes (tab-to-tab, no background de-emphasis) ─

        private val switchKeyframes = KeyframeSequence(listOf(
            KeyframeCheckpoint(
                timeMs = 0,
                enteringOpacity = 0f,
                enteringScale = 0.97f,
                enteringTranslationY = 0f,
                backgroundBlur = 0f,
                backgroundScale = ScaleTokens.NATURAL,
                backgroundDim = 0f,
                label = "hidden"
            ),
            KeyframeCheckpoint(
                timeMs = 140,
                enteringOpacity = 0.7f,
                enteringScale = 0.99f,
                enteringTranslationY = 0f,
                backgroundBlur = 0f,
                backgroundScale = ScaleTokens.NATURAL,
                backgroundDim = 0f,
                label = "fading_in"
            ),
            KeyframeCheckpoint(
                timeMs = 280,
                enteringOpacity = 1f,
                enteringScale = 1f,
                enteringTranslationY = 0f,
                backgroundBlur = 0f,
                backgroundScale = ScaleTokens.NATURAL,
                backgroundDim = 0f,
                label = "active"
            )
        ))

        // ── Handoff Keyframes (MiniPlayer → NowPlaying) ──────────────

        private val handoffKeyframes = KeyframeSequence(listOf(
            KeyframeCheckpoint(
                timeMs = 0,
                enteringOpacity = 0f,
                enteringScale = ScaleTokens.HANDOFF_START,
                enteringTranslationY = 50f,
                backgroundBlur = 0f,
                backgroundScale = ScaleTokens.NATURAL,
                backgroundDim = 0f,
                label = "dock_position"
            ),
            KeyframeCheckpoint(
                timeMs = 120,
                enteringOpacity = 0.5f,
                enteringScale = 0.94f,
                enteringTranslationY = 20f,
                backgroundBlur = 8f,
                backgroundScale = ScaleTokens.SLIGHT_SHRINK,
                backgroundDim = 0.08f,
                label = "lifting"
            ),
            KeyframeCheckpoint(
                timeMs = 280,
                enteringOpacity = 0.9f,
                enteringScale = 0.98f,
                enteringTranslationY = 2f,
                backgroundBlur = 30f,
                backgroundScale = ScaleTokens.MEDIUM_SHRINK,
                backgroundDim = 0.14f,
                label = "settling"
            ),
            KeyframeCheckpoint(
                timeMs = 400,
                enteringOpacity = 1f,
                enteringScale = 1f,
                enteringTranslationY = 0f,
                backgroundBlur = BlurTokens.Heavy.value,
                backgroundScale = ScaleTokens.MEDIUM_SHRINK,
                backgroundDim = OpacityTokens.DIM_FOR_FOCUS,
                label = "now_playing_active"
            )
        ))

        // ── Panel Open Keyframes (lift from bottom) ──────────────────

        private val panelOpenKeyframes = KeyframeSequence(listOf(
            KeyframeCheckpoint(
                timeMs = 0,
                enteringOpacity = 0f,
                enteringScale = ScaleTokens.PANEL_ENTER_START,
                enteringTranslationY = 80f,
                backgroundBlur = 0f,
                backgroundScale = ScaleTokens.NATURAL,
                backgroundDim = 0f,
                label = "below_view"
            ),
            KeyframeCheckpoint(
                timeMs = 100,
                enteringOpacity = 0.4f,
                enteringScale = 0.97f,
                enteringTranslationY = 30f,
                backgroundBlur = 6f,
                backgroundScale = 0.99f,
                backgroundDim = 0.05f,
                label = "lifting"
            ),
            KeyframeCheckpoint(
                timeMs = 250,
                enteringOpacity = 0.9f,
                enteringScale = 0.995f,
                enteringTranslationY = 2f,
                backgroundBlur = BlurTokens.Medium.value,
                backgroundScale = ScaleTokens.SLIGHT_SHRINK,
                backgroundDim = OpacityTokens.DIM_FOR_PANEL,
                label = "settling"
            ),
            KeyframeCheckpoint(
                timeMs = 350,
                enteringOpacity = 1f,
                enteringScale = 1f,
                enteringTranslationY = 0f,
                backgroundBlur = BlurTokens.Strong.value,
                backgroundScale = ScaleTokens.SLIGHT_SHRINK,
                backgroundDim = OpacityTokens.DIM_FOR_PANEL,
                label = "panel_active"
            )
        ))

        // ── Panel Close Keyframes (reverse lift) ─────────────────────

        private val panelCloseKeyframes = KeyframeSequence(listOf(
            KeyframeCheckpoint(
                timeMs = 0,
                enteringOpacity = 1f,
                enteringScale = 1f,
                enteringTranslationY = 0f,
                backgroundBlur = BlurTokens.Strong.value,
                backgroundScale = ScaleTokens.SLIGHT_SHRINK,
                backgroundDim = OpacityTokens.DIM_FOR_PANEL,
                label = "panel_active"
            ),
            KeyframeCheckpoint(
                timeMs = 100,
                enteringOpacity = 0.9f,
                enteringScale = 0.995f,
                enteringTranslationY = 2f,
                backgroundBlur = BlurTokens.Medium.value,
                backgroundDim = OpacityTokens.DIM_FOR_PANEL * 0.6f,
                label = "starting_descent"
            ),
            KeyframeCheckpoint(
                timeMs = 250,
                enteringOpacity = 0.4f,
                enteringScale = 0.97f,
                enteringTranslationY = 30f,
                backgroundBlur = 6f,
                backgroundDim = 0.05f,
                label = "descending"
            ),
            KeyframeCheckpoint(
                timeMs = 350,
                enteringOpacity = 0f,
                enteringScale = ScaleTokens.PANEL_ENTER_START,
                enteringTranslationY = 80f,
                backgroundBlur = 0f,
                backgroundScale = ScaleTokens.NATURAL,
                backgroundDim = 0f,
                label = "gone"
            )
        ))

        // ── Overlay Show Keyframes ─────────────────────────────────

        private val overlayShowKeyframes = KeyframeSequence(listOf(
            KeyframeCheckpoint(
                timeMs = 0,
                enteringOpacity = 0f,
                enteringScale = ScaleTokens.OVERLAY_ENTER_START,
                enteringTranslationY = 4f,
                backgroundBlur = 0f,
                backgroundScale = ScaleTokens.NATURAL,
                backgroundDim = 0f,
                label = "hidden"
            ),
            KeyframeCheckpoint(
                timeMs = 100,
                enteringOpacity = 0.7f,
                enteringScale = 0.95f,
                enteringTranslationY = 0f,
                backgroundBlur = 0f,
                backgroundScale = ScaleTokens.NATURAL,
                backgroundDim = OpacityTokens.DIM_FOR_OVERLAY,
                label = "appearing"
            ),
            KeyframeCheckpoint(
                timeMs = 200,
                enteringOpacity = 1f,
                enteringScale = 1f,
                enteringTranslationY = 0f,
                backgroundBlur = 0f,
                backgroundScale = ScaleTokens.NATURAL,
                backgroundDim = OpacityTokens.DIM_FOR_OVERLAY,
                label = "visible"
            )
        ))

        // ── Overlay Hide Keyframes ──────────────────────────────────

        private val overlayHideKeyframes = KeyframeSequence(listOf(
            KeyframeCheckpoint(
                timeMs = 0,
                enteringOpacity = 1f,
                enteringScale = 1f,
                backgroundDim = OpacityTokens.DIM_FOR_OVERLAY,
                label = "visible"
            ),
            KeyframeCheckpoint(
                timeMs = 100,
                enteringOpacity = 0.5f,
                enteringScale = 0.96f,
                backgroundDim = OpacityTokens.DIM_FOR_OVERLAY * 0.5f,
                label = "fading"
            ),
            KeyframeCheckpoint(
                timeMs = 200,
                enteringOpacity = 0f,
                enteringScale = ScaleTokens.OVERLAY_ENTER_START,
                backgroundDim = 0f,
                label = "gone"
            )
        ))

        // ── Dock Appear Keyframes ──────────────────────────────────

        private val dockAppearKeyframes = KeyframeSequence(listOf(
            KeyframeCheckpoint(
                timeMs = 0,
                enteringOpacity = 0f,
                enteringScale = 0.85f,
                enteringTranslationY = 20f,
                label = "hidden"
            ),
            KeyframeCheckpoint(
                timeMs = 150,
                enteringOpacity = 0.8f,
                enteringScale = 0.97f,
                enteringTranslationY = 4f,
                label = "sliding_up"
            ),
            KeyframeCheckpoint(
                timeMs = 250,
                enteringOpacity = 1f,
                enteringScale = 1f,
                enteringTranslationY = 0f,
                label = "docked"
            )
        ))

        // ── Dock Hide Keyframes ────────────────────────────────────

        private val dockHideKeyframes = KeyframeSequence(listOf(
            KeyframeCheckpoint(
                timeMs = 0,
                enteringOpacity = 1f,
                enteringScale = 1f,
                enteringTranslationY = 0f,
                label = "docked"
            ),
            KeyframeCheckpoint(
                timeMs = 120,
                enteringOpacity = 0.5f,
                enteringScale = 0.95f,
                enteringTranslationY = 8f,
                label = "sliding_down"
            ),
            KeyframeCheckpoint(
                timeMs = 200,
                enteringOpacity = 0f,
                enteringScale = 0.85f,
                enteringTranslationY = 20f,
                label = "hidden"
            )
        ))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 7. KEYFRAME CHECKPOINT — Single timed checkpoint in a transition
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * A single checkpoint in a transition timeline.
 *
 * At each checkpoint, both the entering element and the background
 * have defined visual states. The TransitionChoreographer interpolates
 * between checkpoints to create smooth animation.
 */
data class KeyframeCheckpoint(
    /** Time in milliseconds from transition start. */
    val timeMs: Int,

    /** Opacity of the entering/leaving element at this point. */
    val enteringOpacity: Float = 0f,

    /** Scale of the entering/leaving element. */
    val enteringScale: Float = 1f,

    /** Vertical translation of entering/leaving element (dp). */
    val enteringTranslationY: Float = 0f,

    /** Blur applied to background at this point. */
    val backgroundBlur: Float = 0f,

    /** Scale of background at this point. */
    val backgroundScale: Float = 1f,

    /** Dim alpha applied to background at this point. */
    val backgroundDim: Float = 0f,

    /** Human-readable label for this checkpoint. */
    val label: String = ""
)

// ═══════════════════════════════════════════════════════════════════════════════
// 8. SURFACE SPEC — Glass surface in layered context
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Specification for a glass surface within the layered system.
 *
 * A Surface is a visual element (card, panel, overlay, dock) that
 * exists within a scene and has glass properties.
 * Unlike a Scene (which is a full visual state), a Surface is
 * a component within a scene.
 *
 * From Spec Section 4.3 glossary: "Surface = glass card / panel / object"
 */
data class SurfaceSpec(
    /** Unique identifier for this surface instance. */
    val id: String,

    /** The scene this surface belongs to. */
    val sceneId: SceneId,

    /** Type of surface. */
    val type: SurfaceType,

    /** Glass style to apply (references GlassTokens). */
    val glassStyleName: String,

    /** Size constraints. */
    val sizeConstraints: SurfaceSizeConstraints = SurfaceSizeConstraints(),

    /** Position within the scene (determines layout behavior). */
    val position: SurfacePosition = SurfacePosition.FLOATING,

    /** Whether this surface is currently visible. */
    val isVisible: Boolean = true,

    /** Whether this surface is interactive. */
    val isInteractive: Boolean = true,

    /** Z-order relative to other surfaces in the same scene. */
    val zIndex: Int = 0
)

/** Types of surfaces in the Visual OS. */
enum class SurfaceType {
    /** Glass card (collection card, song item, etc.). */
    CARD,

    /** Floating panel (equalizer, timer, settings sheet). */
    PANEL,

    /** Context menu / dropdown. */
    CONTEXT_MENU,

    /** Tooltip / quick info. */
    TOOLTIP,

    /** Dock surface (mini player). */
    DOCK,

    /** Button with glass treatment. */
    BUTTON,

    /** Input field with glass background. */
    INPUT_FIELD,

    /** Hero section (album art container). */
    HERO_CONTAINER
}

/** Size constraints for a surface. */
data class SurfaceSizeConstraints(
    /** Maximum width fraction (0-1 of parent). */
    val maxWidthFraction: Float = 1.0f,
    /** Maximum height fraction (0-1 of parent). */
    val maxHeightFraction: Float = 1.0f,
    /** Minimum width in dp. */
    val minWidthDp: Int = 0,
    /** Minimum height in dp. */
    val minHeightDp: Int = 0,
    /** Whether width is adaptive (fills available space). */
    val adaptiveWidth: Boolean = true
)

/** Where a surface is positioned within its scene. */
enum class SurfacePosition {
    /** Floating — positioned freely within the scene. */
    FLOATING,

    /** Docked at bottom — above bottom nav, below content. */
    BOTTOM_DOCKED,

    /** Centered — in the middle of the scene. */
    CENTERED,

    /** Anchored — attached to another element (context menu to trigger). */
    ANCHORED,

    /** Full-width — stretches across screen width. */
    FULL_WIDTH
}
