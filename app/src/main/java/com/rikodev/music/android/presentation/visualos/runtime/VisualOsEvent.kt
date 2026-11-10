package com.rikodev.music.android.presentation.visualos.runtime

import com.rikodev.music.android.presentation.visualos.contract.*
import androidx.compose.material.icons.filled.Pause
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// RIKO VISUAL OS — EVENTS & ACTIONS
//
// Events: Things that HAPPEN (past tense, per Rule #45: Events Are Verbs).
// Actions: Things the system INTENDS to do (nouns, per Rule #48: Actions Are Nouns).
//
// Events arrive from:
//   - User interaction (tap, swipe, long press)
//   - System (song change, configuration change, back press)
//   - Engine results (color extraction complete, transition complete)
//   - Internal state changes (timer tick, breathing phase update)
//
// Actions describe intent — they are NOT executed by the reducer (Rule #83).
// The Coordinator executes actions.
// ─────────────────────────────────────────────────────────────────────────────

// ═════════════════════════════════════════════════════════════════════════════
// 1. EVENTS — Things that happen (past tense verbs)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Events in the Visual OS state machine.
 *
 * Rule #45: "Events Are Verbs (Past Tense)"
 * Events describe what HAS happened — they are inputs to the reducer.
 * The reducer processes events and produces (new state, actions).
 */
sealed class VisualOsEvent {

    // ── Navigation Events ──────────────────────────────────────────────

    /** User navigated to a scene. */
    data class SceneNavigated(val target: RikoSceneType, val context: Map<String, Any> = emptyMap()) : VisualOsEvent()

    /** User pressed back. */
    data object BackPressed : VisualOsEvent()

    /** System back pressed (from activity). */
    data object SystemBackPressed : VisualOsEvent()

    // ── Layer Events ───────────────────────────────────────────────────

    /** A scene layer was pushed onto the stack. */
    data class LayerPushed(val sceneId: SceneId) : VisualOsEvent()

    /** A scene layer was popped from the stack. */
    data class LayerPopped(val sceneId: SceneId) : VisualOsEvent()

    /** A layer received focus. */
    data class LayerFocused(val sceneId: SceneId) : VisualOsEvent()

    /** A layer lost focus. */
    data class LayerUnfocused(val sceneId: SceneId) : VisualOsEvent()

    // ── Panel Events ─────────────────────────────────────────────────

    /** Panel opened (equalizer, timer, queue, song info). */
    data class PanelOpened(val type: RikoSceneType) : VisualOsEvent()

    /** Panel closed. */
    data class PanelClosed(val sceneId: SceneId) : VisualOsEvent()

    /** Panel dismissed by user (swipe down, back press). */
    data class PanelDismissed(val sceneId: SceneId) : VisualOsEvent()

    // ── Overlay Events ───────────────────────────────────────────────

    /** Overlay shown (context menu, tooltip). */
    data class OverlayShown(val type: OverlayType, val anchor: OverlayAnchor? = null, val data: Map<String, Any> = emptyMap()) : VisualOsEvent()

    /** Overlay hidden. */
    data class OverlayHidden(val type: OverlayType) : VisualOsEvent()

    /** Outside tap detected (may dismiss overlay). */
    data object OverlayOutsideTapped : VisualOsEvent()

    // ── Dock Events ────────────────────────────────────────────────────

    /** Dock appeared (song started playing). */
    data class DockAppeared(val songTitle: String, val songArtist: String, val accentColor: Long, val isPlaying: Boolean) : VisualOsEvent()

    /** Dock disappeared (song ended or NowPlaying opened). */
    data object DockDisappeared : VisualOsEvent()

    /** Dock tapped (navigate to NowPlaying). */
    data object DockTapped : VisualOsEvent()

    /** Dock playback toggled. */
    data object DockPlayPauseToggled : VisualOsEvent()

    /** Dock next track requested. */
    data object DockNextRequested : VisualOsEvent()

    /** Dock previous track requested. */
    data object DockPreviousRequested : VisualOsEvent()

    // ── Transition Events ──────────────────────────────────────────────

    /** Transition ticked (progress updated by animation frame). */
    data class TransitionTicked(val progress: Float) : VisualOsEvent()

    /** Transition completed (progress reached 1.0). */
    data class TransitionCompleted(val sceneId: SceneId) : VisualOsEvent()

    /** Transition interrupted by another transition. */
    data class TransitionInterrupted(val newSceneId: SceneId) : VisualOsEvent()

    // ── Song / Background Events ──────────────────────────────────────

    /** Song changed — update background colors. */
    data class SongChanged(val context: SongContext) : VisualOsEvent()

    /** Album art colors extracted — update background palette. */
    data class AlbumArtColorsExtracted(val colors: List<Long>, val dominant: Long, val vibrant: Long) : VisualOsEvent()

    /** Background breathing phase updated. */
    data class BreathingPhaseUpdated(val phase: Float) : VisualOsEvent()

    /** Background drift updated. */
    data class BackgroundDriftUpdated(val offsetX: Float, val offsetY: Float) : VisualOsEvent()

    // ── System Events ────────────────────────────────────────────────

    /** System configuration changed (orientation, multi-window, etc.). */
    data class SystemConfigurationChanged(val state: VisualOsSystemState) : VisualOsEvent()

    /** Reduced motion preference changed. */
    data class ReducedMotionChanged(val enabled: Boolean) : VisualOsEvent()

    /** Refresh rate changed (e.g., display switch). */
    data class RefreshRateChanged(val hz: Int) : VisualOsEvent()

    // ── Handoff Events ────────────────────────────────────────────────

    /** Dock → NowPlaying handoff started. */
    data class HandoffToNowPlayingStarted(val dummy: Int = 0) : VisualOsEvent() {
        companion object
    }

    /** NowPlaying → Dock handoff (reverse, collapse NP to dock). */
    data class HandoffToDockStarted(val dummy: Int = 0) : VisualOsEvent() {
        companion object
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 2. ACTIONS — Intents the system wants to execute
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Actions in the Visual OS state machine.
 *
 * Rule #48: "Actions Are Nouns"
 * Actions describe INTENT — what the system wants to do.
 * Actions are NOT executed by the reducer (Rule #83: Coordinator Executes).
 * The Coordinator receives these actions and executes them asynchronously.
 *
 * Actions are produced by the reducer alongside new state.
 * Pattern: (State, Event) → Pair<State, List<Action>>
 */
sealed class VisualOsAction {

    // ── Scene Actions ─────────────────────────────────────────────────

    /** Push a new scene onto the stack. */
    data class PushScene(val type: RikoSceneType, val context: Map<String, Any> = emptyMap()) : VisualOsAction()

    /** Pop the top scene from the stack. */
    data class PopScene(val sceneId: SceneId) : VisualOsAction()

    /** Replace a scene in the stack (same-layer swap). */
    data class ReplaceScene(val oldSceneId: SceneId, val newType: RikoSceneType, val context: Map<String, Any> = emptyMap()) : VisualOsAction()

    /** Remove all scenes above the specified one. */
    data class PopToScene(val targetSceneId: SceneId) : VisualOsAction()

    // ── Transition Actions ────────────────────────────────────────────

    /** Begin a transition between scenes. */
    data class BeginTransition(val spec: TransitionSpec) : VisualOsAction()

    /** Complete a transition (apply final visual states). */
    data class CompleteTransition(val sceneId: SceneId) : VisualOsAction()

    /** Cancel an in-progress transition. */
    data class CancelTransition(val sceneId: SceneId) : VisualOsAction()

    // ── Background Actions ────────────────────────────────────────────

    /** Update background colors from album art. */
    data class UpdateBackgroundColors(val colors: List<Long>, val dominant: Long, val vibrant: Long) : VisualOsAction()

    /** Update background visual parameters (blur, opacity, scale). */
    data class UpdateBackgroundVisuals(val blur: Float, val opacity: Float, val scale: Float) : VisualOsAction()

    /** Start living background animation (breathing, drift). */
    data object StartBackgroundAnimation : VisualOsAction()

    /** Stop living background animation. */
    data object StopBackgroundAnimation : VisualOsAction()

    // ── Dock Actions ──────────────────────────────────────────────────

    /** Show the mini player dock. */
    data class ShowDock(val title: String, val artist: String, val accentColor: Long, val isPlaying: Boolean) : VisualOsAction()

    /** Hide the mini player dock. */
    data object HideDock : VisualOsAction()

    /** Update dock display content. */
    data class UpdateDockContent(val title: String, val artist: String, val accentColor: Long) : VisualOsAction()

    /** Update dock playback state. */
    data class UpdateDockPlayback(val isPlaying: Boolean) : VisualOsAction()

    // ── Overlay Actions ────────────────────────────────────────────────

    /** Show an overlay. */
    data class ShowOverlay(val type: OverlayType, val anchor: OverlayAnchor? = null, val data: Map<String, Any> = emptyMap()) : VisualOsAction()

    /** Hide the current overlay. */
    data class HideOverlay(val type: OverlayType) : VisualOsAction()

    // ── Navigation Actions ────────────────────────────────────────────

    /** Navigate to NowPlaying (from dock tap or other trigger). */
    data object NavigateToNowPlaying : VisualOsAction()

    /** Handle back press. */
    data object HandleBackPress : VisualOsAction()

    /** Handle system back press (may close overlay, panel, or pop scene). */
    data object HandleSystemBackPress : VisualOsAction()

    // ── Playback Actions (forwarded to PlayerManager via Coordinator) ─

    /** Toggle play/pause (from dock). */
    data object TogglePlayPause : VisualOsAction()

    /** Skip to next track (from dock). */
    data object SkipToNext : VisualOsAction()

    /** Skip to previous track (from dock). */
    data object SkipToPrevious : VisualOsAction()

    // ── System Actions ─────────────────────────────────────────────────

    /** Extract colors from album art URI. */
    data class ExtractAlbumArtColors(val uri: String) : VisualOsAction()

    /** Request frame timing update. */
    data object RequestFrameUpdate : VisualOsAction()
}
