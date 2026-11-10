package com.rikodev.music.android.presentation.visualos.host

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import com.rikodev.music.android.presentation.visualos.contract.*
import com.rikodev.music.android.presentation.visualos.engine.*
import com.rikodev.music.android.presentation.visualos.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.filled.Pause

// ─────────────────────────────────────────────────────────────────────────────
// RIKO VISUAL OS — COORDINATOR
//
// The execution engine of the Visual OS.
// Rule #83: "Coordinator Executes. Reducer Never Does."
//
// The Coordinator:
//   - Receives actions from the reducer
//   - Executes them asynchronously (coroutines)
//   - Dispatches resulting events back to the reducer
//   - Bridges to external services (PlayerManager, ColorExtractor)
//   - Manages animation frame loop
//   - NEVER modifies state directly (always through events)
//
// Pattern: Same as LibraryHostCoordinator (B5.5), SearchHostCoordinator (M3),
// QueueHostCoordinator (M4), etc.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Coordinator for the Visual OS subsystem.
 *
 * This is the "brain" that executes actions produced by the reducer.
 * It bridges between:
 *   - Pure state machine (reducer)
 *   - External services (PlayerManager, ColorExtractor)
 *   - Animation system (Compose Animatable)
 *   - Background engine (living animations)
 *
 * The Coordinator follows the established pattern:
 *   Coordinator is pure Kotlin, returns no Composable results.
 *   It manages coroutines and flows, not UI rendering.
 */
class VisualOsCoordinator {

    // ── State Management ────────────────────────────────────────────────

    private val _state = MutableStateFlow(VisualOsState.initial())

    /**
     * Plan 1.5: high-frequency motion frames (breathing/drift) live in their
     * own flow — writing them into [_state] at up to 60fps invalidated every
     * slow consumer (nav back-chain root) on every tick. Renderers read this
     * in their draw phase; nothing at composition scope subscribes.
     */
    data class BackgroundMotionFrame(
        val breathingPhase: Float = 0f,
        val driftOffsetX: Float = 0f,
        val driftOffsetY: Float = 0f
    )

    private val _backgroundMotion = MutableStateFlow(BackgroundMotionFrame())
    val backgroundMotion: StateFlow<BackgroundMotionFrame> = _backgroundMotion.asStateFlow()
    val state: StateFlow<VisualOsState> = _state.asStateFlow()

    // ── Animation State ────────────────────────────────────────────────

    /** Current animation progress for active transition. */
    private var transitionAnimatable: Animatable<Float, AnimationVector1D>? = null

    /** Animation start time for background breathing/drift. */
    private var backgroundAnimStartTime = System.currentTimeMillis()

    /** Coroutine scope for animation operations. Provided by host. */
    private var coroutineScope: CoroutineScope? = null

    /**
     * PERF FIX (real crash/lag report, real device): the background loop's
     * Job was never stored anywhere before this fix — meaning nothing in
     * this file could ever actually cancel it once started. dispose() only
     * nulled the `coroutineScope` field, which does nothing to a coroutine
     * already running on it. Combined with the loop never checking app
     * visibility/lifecycle or DeviceTier, once started it ran a state
     * mutation + StateFlow emission every ~16ms for the rest of the process
     * lifetime, including fully backgrounded — real, continuous CPU/battery
     * cost that can trigger exactly "severe lag" and the OS killing the
     * process ("keeps stopping"). See pauseBackgroundAnimation() /
     * resumeBackgroundAnimation() / dispose(), and VisualOsWiring.kt for
     * the lifecycle hookup.
     */
    private var backgroundAnimJob: Job? = null

    /** Set via init(scope, tier) — controls the loop's update rate. */
    private var deviceTier: DeviceTier = DeviceTier.TIER_MEDIUM

    // ── External Service Bridges ────────────────────────────────────────

    /** Callback to navigate to NowPlaying (bridges to existing nav). */
    var onNavigateToNowPlaying: (() -> Unit)? = null

    /** Callback to toggle play/pause (bridges to PlayerManager). */
    var onTogglePlayPause: (() -> Unit)? = null

    /** Callback to skip next (bridges to PlayerManager). */
    var onSkipToNext: (() -> Unit)? = null

    /** Callback to skip previous (bridges to PlayerManager). */
    var onSkipToPrevious: (() -> Unit)? = null

    /** Callback to extract album art colors (bridges to ColorExtractor). */
    var onExtractColors: ((String, (List<Long>, Long, Long) -> Unit) -> Unit)? = null

    /** Callback to handle back press (bridges to activity). */
    var onBackPress: (() -> Unit)? = null

    /**
     * Initialize the coordinator with a coroutine scope.
     * Called from VisualOsWiring (CompositionLocal provider).
     *
     * @param tier Real device capability (see VisualOsDeviceTierProvider) —
     * controls the background loop's update rate. Defaults to MEDIUM if a
     * caller doesn't pass one, matching the pre-fix behavior's rough middle
     * ground rather than silently assuming the most expensive tier.
     */
    fun init(scope: CoroutineScope, tier: DeviceTier = DeviceTier.TIER_MEDIUM) {
        coroutineScope = scope
        deviceTier = tier
        startBackgroundAnimation()
    }

    /**
     * Dispatch an event to the reducer.
     * This is the single entry point for all Visual OS events.
     */
    fun dispatch(event: VisualOsEvent) {
        val currentState = _state.value
        val (newState, actions) = VisualOsReducer.reduce(currentState, event)
        _state.value = newState
        executeActions(actions)
    }

    // ═════════════════════════════════════════════════════════════════════
    // ACTION EXECUTION — The heart of Rule #83
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Execute actions produced by the reducer.
     *
     * Each action describes an INTENT. The Coordinator decides HOW to execute it.
     * Actions may result in events being dispatched back (closing the loop).
     */
    private fun executeActions(actions: List<VisualOsAction>) {
        val scope = coroutineScope ?: return

        for (action in actions) {
            when (action) {
                // ── Scene Actions ────────────────────────────────────
                is VisualOsAction.PushScene -> {
                    // Scene push is already handled by the reducer
                    // This action is for external notification
                }
                is VisualOsAction.PopScene -> {
                    dispatch(VisualOsEvent.LayerPopped(action.sceneId))
                }
                is VisualOsAction.ReplaceScene -> {
                    dispatch(VisualOsEvent.SceneNavigated(action.newType, action.context))
                }
                is VisualOsAction.PopToScene -> {
                    dispatch(VisualOsEvent.LayerPopped(action.targetSceneId))
                }

                // ── Transition Actions ──────────────────────────────
                is VisualOsAction.BeginTransition -> {
                    startTransitionAnimation(action.spec, scope)
                }
                is VisualOsAction.CompleteTransition -> {
                    dispatch(VisualOsEvent.TransitionCompleted(action.sceneId))
                }
                is VisualOsAction.CancelTransition -> {
                    dispatch(VisualOsEvent.TransitionInterrupted(action.sceneId))
                }

                // ── Background Actions ──────────────────────────────
                is VisualOsAction.UpdateBackgroundColors -> {
                    // RUNTIME FIX 11: this used to dispatch(AlbumArtColorsExtracted(...))
                    // here — re-firing the exact event that (via the reducer)
                    // produces this same action, an unconditional cycle that
                    // stack-overflowed on the main thread on every song's
                    // color extraction (confirmed via real device crash
                    // log). The reducer no longer emits this action at all
                    // (VisualOsReducer.handleColorsExtracted applies colors
                    // to state directly now), so this branch shouldn't
                    // normally run — but if it ever does, apply directly
                    // rather than re-entering the event system, matching
                    // UpdateBackgroundVisuals' pattern right below.
                    _state.value = _state.value.copy(
                        backgroundState = _state.value.backgroundState.copy(
                            albumArtColors = action.colors,
                            dominantColor = action.dominant,
                            vibrantColor = action.vibrant
                        )
                    )
                }
                is VisualOsAction.UpdateBackgroundVisuals -> {
                    _state.value = _state.value.copy(
                        backgroundState = _state.value.backgroundState.copy(
                            blur = action.blur,
                            opacity = action.opacity,
                            scale = action.scale
                        )
                    )
                }
                is VisualOsAction.StartBackgroundAnimation -> {
                    startBackgroundAnimation()
                }
                is VisualOsAction.StopBackgroundAnimation -> {
                    // Was a no-op comment ("never truly stops") that didn't
                    // even do the "reduce to minimum intensity" it claimed.
                    // Now real: actually pauses the loop. Background is
                    // still "never dead" in spirit — resumeBackgroundAnimation()
                    // (called from VisualOsWiring on ON_START) brings it back
                    // the moment the app is foregrounded again.
                    pauseBackgroundAnimation()
                }

                // ── Dock Actions ────────────────────────────────────
                is VisualOsAction.ShowDock -> {
                    // RUNTIME FIX 11: used to dispatch(DockAppeared(...))
                    // here. handleDockAppeared (the only handler for that
                    // event) emitted this exact ShowDock action back —
                    // an unconditional two-step cycle, confirmed via a
                    // real device StackOverflowError. Applying state
                    // directly closes the cycle regardless of which
                    // reducer function emitted this action.
                    _state.value = _state.value.copy(
                        dockState = _state.value.dockState.copy(
                            visibility = DockVisibility.VISIBLE,
                            visualState = VisualState.activeForLayer(RikoLayerType.DOCK),
                            songTitle = action.title,
                            songArtist = action.artist,
                            accentColor = action.accentColor,
                            isPlaying = action.isPlaying
                        )
                    )
                }
                is VisualOsAction.HideDock -> {
                    dispatch(VisualOsEvent.DockDisappeared)
                }
                is VisualOsAction.UpdateDockContent -> {
                    _state.value = _state.value.copy(
                        dockState = _state.value.dockState.copy(
                            songTitle = action.title,
                            songArtist = action.artist,
                            accentColor = action.accentColor
                        )
                    )
                }
                is VisualOsAction.UpdateDockPlayback -> {
                    _state.value = _state.value.copy(
                        dockState = _state.value.dockState.copy(
                            isPlaying = action.isPlaying
                        )
                    )
                }

                // ── Overlay Actions ──────────────────────────────────
                is VisualOsAction.ShowOverlay -> {
                    // RUNTIME FIX 11: same cycle shape as ShowDock/
                    // UpdateBackgroundColors above — used to dispatch
                    // OverlayShown, whose only handler emitted this same
                    // action back.
                    _state.value = _state.value.copy(
                        overlayState = _state.value.overlayState.copy(
                            isActive = true,
                            type = action.type,
                            visualState = VisualState.defaultForLayer(RikoLayerType.OVERLAY),
                            anchorPosition = action.anchor,
                            contentData = action.data,
                            dismissOnOutsideTap = true
                        ),
                        focusOwner = FocusOwner.OVERLAY
                    )
                }
                is VisualOsAction.HideOverlay -> {
                    _state.value = _state.value.copy(
                        overlayState = _state.value.overlayState.copy(isActive = false),
                        focusOwner = if (_state.value.isPanelOpen) FocusOwner.PANEL
                            else if (_state.value.focusScene != null) FocusOwner.FOCUS_SCENE
                            else FocusOwner.BASE
                    )
                }

                // ── Navigation Actions (bridge to existing system) ──
                is VisualOsAction.NavigateToNowPlaying -> {
                    onNavigateToNowPlaying?.invoke()
                }
                is VisualOsAction.HandleBackPress -> {
                    handleBackPress()
                }
                is VisualOsAction.HandleSystemBackPress -> {
                    handleBackPress()
                    onBackPress?.invoke()
                }

                // ── Playback Actions (bridge to PlayerManager) ────────
                is VisualOsAction.TogglePlayPause -> {
                    onTogglePlayPause?.invoke()
                }
                is VisualOsAction.SkipToNext -> {
                    onSkipToNext?.invoke()
                }
                is VisualOsAction.SkipToPrevious -> {
                    onSkipToPrevious?.invoke()
                }

                // ── System Actions ──────────────────────────────────
                is VisualOsAction.ExtractAlbumArtColors -> {
                    onExtractColors?.invoke(action.uri) { colors, dominant, vibrant ->
                        dispatch(VisualOsEvent.AlbumArtColorsExtracted(colors, dominant, vibrant))
                    }
                }
                is VisualOsAction.RequestFrameUpdate -> {
                    // Frame update is handled by the animation loop
                }
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // TRANSITION ANIMATION
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Start a transition animation.
     *
     * Uses Compose Animatable for smooth, frame-accurate animation.
     * On each frame tick, dispatches TransitionTicked event.
     */
    private fun startTransitionAnimation(spec: TransitionSpec, scope: CoroutineScope) {
        scope.launch {
            val animatable = Animatable(0f)
            transitionAnimatable = animatable

            animatable.animateTo(
                targetValue = 1f,
                animationSpec = when (spec.motionWord) {
                    MotionWord.SPRING, MotionWord.HANDOFF -> spring(
                        dampingRatio = spec.spring.damping,
                        stiffness = spec.spring.stiffness
                    )
                    MotionWord.LIFT -> spring(
                        dampingRatio = 0.85f,
                        stiffness = 300f
                    )
                    MotionWord.SCALE, MotionWord.ELASTIC -> spring(
                        dampingRatio = 0.72f,
                        stiffness = 200f
                    )
                    MotionWord.SETTLE -> tween(
                        durationMillis = spec.durationMs,
                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                    )
                    MotionWord.BREATHING -> tween(
                        durationMillis = spec.durationMs,
                        easing = androidx.compose.animation.core.LinearEasing
                    )
                    else -> tween(
                        durationMillis = spec.durationMs,
                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                    )
                }
            ) {
                // On each frame: compute interpolated visual states
                val progress = value
                val (enteringState, backgroundState) = TransitionChoreographer.compute(
                    spec = spec,
                    progress = progress,
                    isReducedMotion = _state.value.systemState.reducedMotion
                )

                // Update transition state with interpolated values
                _state.value = _state.value.copy(
                    activeTransition = _state.value.activeTransition?.copy(
                        progress = progress,
                        enteringVisualState = enteringState,
                        backgroundVisualState = backgroundState
                    )
                )
            }

            transitionAnimatable = null
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // BACKGROUND ANIMATION LOOP
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Start the background breathing/drift animation loop.
     *
     * Runs continuously while the app is foregrounded — background never
     * stops animating while visible (Spec: "never dead"). Update RATE
     * scales with DeviceTier instead of a flat 60fps for every device,
     * matching this app's own performance principle (reduce update rate
     * on weak devices before dropping visual identity entirely) — the
     * blur radius in NowPlayingScreen already does this same kind of
     * tier gating; this loop didn't, until this fix.
     *
     * Idempotent: cancels any already-running loop before starting a new
     * one, so a second call (e.g. resumeBackgroundAnimation() after a
     * pause, or any future re-dispatch of StartBackgroundAnimation) can
     * never result in two loops running concurrently.
     */
    private fun startBackgroundAnimation() {
        val scope = coroutineScope ?: return
        backgroundAnimJob?.cancel()
        backgroundAnimStartTime = System.currentTimeMillis()

        val frameDelayMs = when (deviceTier) {
            DeviceTier.TIER_HIGH -> 16L   // ~60fps — full atmosphere
            DeviceTier.TIER_MEDIUM -> 32L // ~30fps — still smooth, half the work
            DeviceTier.TIER_LOW -> 66L    // ~15fps — motion still present, minimal cost
        }

        backgroundAnimJob = scope.launch {
            while (true) {
                val elapsed = System.currentTimeMillis() - backgroundAnimStartTime

                // Compute breathing phase
                val breathingValues = TransitionChoreographer.computeBreathing(
                    elapsedMs = elapsed,
                    amplitude = _state.value.backgroundState.breathingIntensity
                )

                // Compute drift
                val driftValues = TransitionChoreographer.computeDrift(
                    elapsedMs = elapsed
                )

                // Dispatch updates — plan 1.5: motion frames go to the
                // dedicated flow, never into the slow UI state object.
                _backgroundMotion.value = BackgroundMotionFrame(
                    breathingPhase = breathingValues.phase,
                    driftOffsetX = driftValues.offsetX,
                    driftOffsetY = driftValues.offsetY
                )

                delay(frameDelayMs)
            }
        }
    }

    /**
     * PERF FIX: actually pause the loop (cancel the Job) instead of the
     * old no-op. Called from VisualOsWiring on ON_STOP (app backgrounded)
     * — this is the fix for the loop previously running forever,
     * including fully backgrounded, for the rest of the process lifetime.
     */
    fun pauseBackgroundAnimation() {
        backgroundAnimJob?.cancel()
        backgroundAnimJob = null
    }

    /**
     * Resume after pauseBackgroundAnimation(). Called from VisualOsWiring
     * on ON_START (app foregrounded again). Safe to call even if nothing
     * was paused (startBackgroundAnimation() is idempotent).
     */
    fun resumeBackgroundAnimation() {
        startBackgroundAnimation()
    }

    // ═════════════════════════════════════════════════════════════════════
    // BACK PRESS HANDLING
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Handle back press following priority chain:
     *   1. Close overlay
     *   2. Close panel
     *   3. Pop focus scene
     *   4. Propagate to system
     */
    private fun handleBackPress() {
        dispatch(VisualOsEvent.SystemBackPressed)
    }

    // ═════════════════════════════════════════════════════════════════════
    // PUBLIC API — Used by external components
    // ═════════════════════════════════════════════════════════════════════

    /** Navigate to a scene. */
    fun navigateTo(type: RikoSceneType, context: Map<String, Any> = emptyMap()) {
        dispatch(VisualOsEvent.SceneNavigated(type, context))
    }

    /** Open a panel (equalizer, timer, etc.). */
    fun openPanel(type: RikoSceneType) {
        dispatch(VisualOsEvent.PanelOpened(type))
    }

    /** Close the current panel. */
    fun closePanel() {
        val panel = _state.value.activePanel ?: return
        dispatch(VisualOsEvent.PanelDismissed(panel.id))
    }

    /** Show an overlay. */
    fun showOverlay(type: OverlayType, anchor: OverlayAnchor? = null, data: Map<String, Any> = emptyMap()) {
        dispatch(VisualOsEvent.OverlayShown(type, anchor, data))
    }

    /** Hide the current overlay. */
    fun hideOverlay() {
        val overlay = _state.value.overlayState
        if (overlay.isActive) {
            dispatch(VisualOsEvent.OverlayHidden(overlay.type))
        }
    }

    /** Update song context (called when song changes). */
    fun updateSongContext(context: SongContext) {
        dispatch(VisualOsEvent.SongChanged(context))
    }

    /** Handle system back press. */
    fun onBackPressed() {
        dispatch(VisualOsEvent.SystemBackPressed)
    }

    /** Dispose resources. */
    fun dispose() {
        // PERF FIX: previously only nulled coroutineScope, which does
        // nothing to a coroutine already launched on it — the loop kept
        // running regardless. Now actually cancels it.
        backgroundAnimJob?.cancel()
        backgroundAnimJob = null
        coroutineScope = null
        transitionAnimatable = null
    }
}
