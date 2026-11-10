package com.rikodev.music.android.presentation.visualos.runtime

import com.rikodev.music.android.presentation.visualos.contract.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// RIKO VISUAL OS — MAPPER
//
// Transforms VisualOsState into VisualOsRenderSnapshot.
//
// Rule #59: "Mapper Is The Last Translator"
// Rule #60: "Screen State Is Already Renderable"
// Rule #55: "Screen Owns Composition" (Mapper prepares, Renderer composes)
// Rule #58: "Runtime Never Knows Rendering"
//
// The Mapper is the ONLY place where state gets translated into renderable
// form. Composable functions read from the snapshot, never from raw state.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Mapper for the Visual OS state machine.
 *
 * Transforms the immutable state into a render-ready snapshot that
 * Composable functions can directly consume. This separation ensures:
 *
 * 1. State stays pure and domain-oriented
 * 2. Rendering concerns are isolated here
 * 3. Composables receive pre-computed, ready-to-render data
 * 4. Transition interpolation happens here, not in composables
 */
object VisualOsMapper {

    /**
     * Map state to render-ready snapshot.
     *
     * @param state Current Visual OS state
     * @return Snapshot ready for rendering
     */
    fun map(state: VisualOsState): VisualOsRenderSnapshot {
        return VisualOsRenderSnapshot(
            renderableLayers = computeRenderableLayers(state),
            backgroundParams = computeBackgroundParams(state),
            dockParams = computeDockParams(state),
            overlayParams = computeOverlayParams(state),
            transitionParams = computeTransitionParams(state),
            reducedMotion = state.systemState.reducedMotion,
            frameBudgetMs = state.systemState.frameBudgetMs
        )
    }

    // ═════════════════════════════════════════════════════════════════════
    // RENDERABLE LAYERS
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Compute renderable layer list from scene stack.
     *
     * Each scene in the stack becomes a renderable layer with computed
     * visual parameters. Layers are ordered by z-index (lowest first).
     *
     * During transitions, the transitioning scene's visual state is
     * interpolated between checkpoints by the TransitionChoreographer.
     * Here, we apply the transition progress to get final visual values.
     */
    private fun computeRenderableLayers(state: VisualOsState): List<RenderableLayer> {
        val transition = state.activeTransition
        val layers = mutableListOf<RenderableLayer>()

        for ((index, scene) in state.sceneStack.withIndex()) {
            val isTopOfStack = index == state.sceneStack.lastIndex
            val behavior = LayerBehavior.forType(scene.layerType)

            // Compute visual state: use transition values if this scene
            // is the target of the active transition, otherwise use scene's own state
            val visualState = when {
                transition != null && transition.spec.isEnter &&
                    transition.spec.targetSceneId == scene.id -> {
                    transition.enteringVisualState
                }
                transition != null && !transition.spec.isEnter &&
                    transition.spec.targetSceneId == scene.id -> {
                    // Exiting scene — reverse animation
                    transition.enteringVisualState.copy(
                        opacity = 1f - transition.progress + (1f - transition.enteringVisualState.opacity) * transition.progress,
                        entryProgress = 1f - transition.progress
                    )
                }
                transition != null &&
                    transition.spec.backgroundSceneId == scene.id -> {
                    transition.backgroundVisualState
                }
                else -> scene.visualState
            }

            // Determine interaction ownership
            val ownsInteraction = when {
                state.isOverlayShowing && scene.id != state.overlayState.let { /* check which scene has overlay */ null } -> false
                isTopOfStack && behavior.ownsInteraction -> true
                isTopOfStack && !behavior.ownsInteraction -> false
                else -> false
            }

            // Determine glass tier based on depth plane
            val glassTier = computeGlassTier(scene, state)

            // Determine z-index
            val zIndex = computeZIndex(scene, index, state)

            // Determine whether to render (skip fully invisible layers)
            val shouldRender = scene.isVisible || (transition != null &&
                (transition.spec.targetSceneId == scene.id))

            layers.add(
                RenderableLayer(
                    sceneId = scene.id,
                    sceneType = scene.type,
                    layerType = scene.layerType,
                    visualState = visualState,
                    shouldRender = shouldRender,
                    zIndex = zIndex,
                    ownsInteraction = ownsInteraction,
                    glassTier = glassTier,
                    capabilities = scene.capabilities
                )
            )
        }

        return layers.sortedBy { it.zIndex }
    }

    /**
     * Compute glass tier for a scene based on its position and state.
     */
    private fun computeGlassTier(scene: SceneSpec, state: VisualOsState): GlassTier {
        return when {
            // De-emphasized scenes get heavy glass
            scene.lifecycle == SceneLifecycle.DEEMPHASIZED -> GlassTier.HEAVY
            // Active base scenes — no glass on the scene itself
            scene.layerType == RikoLayerType.BASE && scene.lifecycle == SceneLifecycle.ACTIVE -> GlassTier.NONE
            // Active focus scenes — light glass
            scene.layerType == RikoLayerType.FOCUS && scene.lifecycle == SceneLifecycle.ACTIVE -> GlassTier.LIGHT
            // Panels — medium glass (they ARE glass surfaces)
            scene.layerType == RikoLayerType.PANEL -> GlassTier.MEDIUM
            // Overlays — medium glass
            scene.layerType == RikoLayerType.OVERLAY -> GlassTier.MEDIUM
            // Default
            else -> GlassTier.LIGHT
        }
    }

    /**
     * Compute z-index for a scene.
     *
     * Z-index determines rendering order (higher = rendered on top).
     * The dock always sits between base and focus.
     */
    private fun computeZIndex(scene: SceneSpec, index: Int, state: VisualOsState): Int {
        val baseZ = when (scene.layerType) {
            RikoLayerType.BASE -> 0
            RikoLayerType.DOCK -> 50
            RikoLayerType.FOCUS -> 100
            RikoLayerType.PANEL -> 200
            RikoLayerType.OVERLAY -> 300
        }

        // Add index offset for multiple scenes of the same type (rare)
        return baseZ + (index * 10)
    }

    // ═════════════════════════════════════════════════════════════════════
    // BACKGROUND PARAMETERS
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Compute background rendering parameters from state.
     *
     * The background is always alive (Spec: "never dead").
     * Its visual parameters change based on:
     *   - Active scene (NowPlaying has dramatic background)
     *   - Transition state (blur/opacity during transitions)
     *   - Breathing animation (sinusoidal pulsing)
     */
    private fun computeBackgroundParams(state: VisualOsState): BackgroundRenderParams {
        val bg = state.backgroundState

        // Compute breathing scale effect
        val breathingScaleDelta = if (state.systemState.reducedMotion) {
            0f
        } else {
            bg.breathingIntensity * kotlin.math.sin(bg.breathingPhase).toFloat()
        }

        // Compute background blur based on scene stack
        val baseBlur = when (state.focusScene?.type) {
            is RikoSceneType.NowPlaying -> BlurTokens.Maximum.value
            is RikoSceneType.PlaylistDetail -> BlurTokens.Heavy.value
            else -> BlurTokens.Medium.value
        }

        // If panel is open, increase background blur
        val panelBlurBonus = if (state.isPanelOpen) 10f else 0f

        return BackgroundRenderParams(
            albumArtColors = bg.albumArtColors,
            dominantColor = bg.dominantColor,
            vibrantColor = bg.vibrantColor,
            blur = baseBlur + panelBlurBonus,
            opacity = when {
                state.isPanelOpen -> OpacityTokens.HIGH
                state.focusScene != null -> OpacityTokens.FULL
                else -> OpacityTokens.FULL
            },
            scale = 1.0f + breathingScaleDelta,
            breathingPhase = bg.breathingPhase,
            breathingIntensity = if (state.systemState.reducedMotion) 0f else bg.breathingIntensity,
            driftOffsetX = if (state.systemState.reducedMotion) 0f else bg.driftOffsetX,
            driftOffsetY = if (state.systemState.reducedMotion) 0f else bg.driftOffsetY,
            saturationMultiplier = bg.saturationMultiplier,
            isAlive = bg.isAlive
        )
    }

    // ═════════════════════════════════════════════════════════════════════
    // DOCK PARAMETERS
    // ═════════════════════════════════════════════════════════════════════

    private fun computeDockParams(state: VisualOsState): DockRenderParams {
        val dock = state.dockState
        return DockRenderParams(
            isVisible = dock.visibility == DockVisibility.VISIBLE,
            visualState = dock.visualState,
            songTitle = dock.songTitle,
            songArtist = dock.songArtist,
            accentColor = dock.accentColor,
            isPlaying = dock.isPlaying
        )
    }

    // ═════════════════════════════════════════════════════════════════════
    // OVERLAY PARAMETERS
    // ═════════════════════════════════════════════════════════════════════

    private fun computeOverlayParams(state: VisualOsState): OverlayRenderParams? {
        if (!state.isOverlayShowing) return null
        val overlay = state.overlayState
        return OverlayRenderParams(
            type = overlay.type,
            visualState = overlay.visualState,
            anchorPosition = overlay.anchorPosition,
            contentData = overlay.contentData,
            dismissOnOutsideTap = overlay.dismissOnOutsideTap
        )
    }

    // ═════════════════════════════════════════════════════════════════════
    // TRANSITION PARAMETERS
    // ═════════════════════════════════════════════════════════════════════

    private fun computeTransitionParams(state: VisualOsState): TransitionRenderParams? {
        val transition = state.activeTransition ?: return null
        return TransitionRenderParams(
            type = transition.spec.type,
            progress = transition.progress,
            enteringSceneId = transition.spec.targetSceneId,
            backgroundSceneId = transition.spec.backgroundSceneId
        )
    }
}
