package com.rikodev.music.android.presentation.miniplayer

import com.rikodev.music.android.core.experience.ColorState
import com.rikodev.music.android.core.experience.wave.WaveState
import com.rikodev.music.android.domain.model.PlayerState
import com.rikodev.music.android.domain.model.Song
import com.rikodev.music.android.presentation.minicontact.MiniPlayerInterruption
import androidx.compose.material.icons.filled.Pause
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.platform.LocalDensity

// ═══════════════════════════════════════════════════════════════════════════
// RIKO MUSIC — MINI PLAYER 2.0: INPUT EVENTS
//
// All events that can modify MiniPlayerState.
// Events flow into the reducer: Event → Reducer → State → UI.
//
// Event sources:
//   1. Experience OS (PlayerState, ColorState, WaveState changes)
//   2. User gestures (tap, drag, fling, long-press)
//   3. System events (back press, config change, memory pressure)
//   4. Internal animation callbacks (settle complete, morph complete)
//
// ⭐ RULE: Events are INPUT ONLY.
//   They describe WHAT happened, not HOW to respond.
//   The reducer decides the response.
// ═══════════════════════════════════════════════════════════════════════════

/**
 * All events that can modify [MiniPlayerState].
 *
 * Every state change in Mini Player goes through this sealed class.
 * The reducer ([MiniPlayerReducer]) is the ONLY function that produces
 * new state from these events.
 *
 * Events are organized by source:
 *   - Experience OS events: data flowing in from ViewModel
 *   - Gesture events: from the gesture recognition engine
 *   - Control events: from button taps
 *   - Navigation events: from navigation changes
 *   - Animation events: from spring/animation callbacks
 *   - System events: from Android system interrupts
 */
sealed class MiniPlayerEvent {

    // ───────────────────────────────────────────────────────────────────
    // EXPERIENCE OS EVENTS — Data flowing IN from ViewModel
    // ───────────────────────────────────────────────────────────────────
    // These carry raw Experience OS data. The reducer maps them
    // into MiniPlayerState's lightweight snapshots.

    /**
     * Full playback state changed.
     * Carries the raw [PlayerState] — reducer extracts needed fields.
     */
    data class PlaybackChanged(
        val playerState: PlayerState
    ) : MiniPlayerEvent()

    /**
     * Color palette changed (new song, artwork loaded, or analysis complete).
     * Carries the raw [ColorState] — reducer maps to [MiniPlayerColorSnapshot].
     */
    data class ColorChanged(
        val colorState: ColorState
    ) : MiniPlayerEvent()

    /**
     * Wave state updated (per frame during playback).
     * Carries the raw [WaveState] — reducer maps to [MiniPlayerWaveSnapshot].
     */
    data class WaveChanged(
        val waveState: WaveState
    ) : MiniPlayerEvent()

    /**
     * Favorite status changed for current song.
     */
    data class FavoriteChanged(
        val isFavorite: Boolean
    ) : MiniPlayerEvent()

    /**
     * New track started or track information updated.
     * If [song] is null, no track is loaded.
     */
    data class TrackChanged(
        val song: Song?,
        val albumArtUri: String?
    ) : MiniPlayerEvent()

    // ───────────────────────────────────────────────────────────────────
    // GESTURE EVENTS — From gesture recognition engine
    // ───────────────────────────────────────────────────────────────────
    // These carry gesture data. The reducer updates morph/gesture state.

    /**
     * Surface tapped (not on a button) — triggers expand morph.
     */
    data object TapExpand : MiniPlayerEvent()

    /**
     * Vertical drag update — finger moved up or down.
     *
     * @param deltaY Vertical pixel delta since last event.
     *              Negative = finger moving UP (expand).
     *              Positive = finger moving DOWN (contract/dismiss).
     * @param velocity Current vertical velocity in pixels/second.
     * @param timestampMs Timestamp of this drag event for frame pacing.
     */
    data class Drag(
        val deltaY: Float,
        val velocity: Float,
        val timestampMs: Long
    ) : MiniPlayerEvent()

    /**
     * Horizontal drag update — finger moved left or right (skip gesture).
     *
     * @param deltaX Horizontal pixel delta since last event.
     *              Negative = left (next). Positive = right (previous).
     * @param velocity Current horizontal velocity in pixels/second.
     */
    data class HorizontalDrag(
        val deltaX: Float,
        val velocity: Float
    ) : MiniPlayerEvent()

    /**
     * Vertical drag released — finger lifted.
     * Reducer decides: commit expand, commit dismiss, or spring back.
     *
     * @param velocity Final velocity at release (pixels/second).
     */
    data class DragReleased(
        val velocity: Float
    ) : MiniPlayerEvent()

    /**
     * Fast upward fling — commit expand to Now Playing.
     *
     * @param velocity Fling velocity in pixels/second (always positive).
     */
    data class FlingExpand(
        val velocity: Float
    ) : MiniPlayerEvent()

    /**
     * Fast downward fling — commit dismiss mini player.
     *
     * @param velocity Fling velocity in pixels/second (always positive).
     */
    data class FlingDismiss(
        val velocity: Float
    ) : MiniPlayerEvent()

    /**
     * Long press triggered — show queue peek.
     */
    data object LongPressTriggered : MiniPlayerEvent()

    /**
     * Long press released — hide queue peek.
     */
    data object LongPressReleased : MiniPlayerEvent()

    /**
     * Gesture cancelled by system (phone call, notification, parent scroll).
     * Reducer applies interruption policy from Part K of contract.
     */
    data class GestureCancelled(
        val interruptType: MiniPlayerInterruption.InterruptType = MiniPlayerInterruption.InterruptType.SYSTEM_INTERRUPT
    ) : MiniPlayerEvent()

    /**
     * Direction lock engaged — gesture direction determined.
     */
    data class DirectionLocked(
        val direction: GestureDirection
    ) : MiniPlayerEvent()

    // ───────────────────────────────────────────────────────────────────
    // CONTROL EVENTS — From button taps
    // ───────────────────────────────────────────────────────────────────
    // These update interaction state (press feedback) and produce actions.

    /**
     * Play/Pause button pressed (finger down).
     */
    data object PlayPausePressed : MiniPlayerEvent()

    /**
     * Skip Next button pressed (finger down).
     */
    data object SkipNextPressed : MiniPlayerEvent()

    /**
     * Skip Previous button pressed (finger down).
     */
    data object SkipPreviousPressed : MiniPlayerEvent()

    /**
     * Favorite button pressed (finger down).
     */
    data object FavoritePressed : MiniPlayerEvent()

    /**
     * Any button released (finger up).
     */
    data class PressReleased(
        val target: PressTarget
    ) : MiniPlayerEvent()

    // ───────────────────────────────────────────────────────────────────
    // NAVIGATION EVENTS — From navigation state changes
    // ───────────────────────────────────────────────────────────────────

    /**
     * Back pressed while morph is in progress.
     * Reducer applies REVERSE policy from Part K.
     */
    data object BackPressed : MiniPlayerEvent()

    /**
     * Navigated to Now Playing screen — force collapse morph.
     * Used when user navigates via notification or deep link.
     */
    data object NavigatedToNowPlaying : MiniPlayerEvent()

    /**
     * Returned from Now Playing to home — restore collapsed mini.
     */
    data object ReturnedFromNowPlaying : MiniPlayerEvent()

    /**
     * Route changed — check if mini player should be visible.
     * Mini player hides on Now Playing route.
     */
    data class RouteChanged(
        val isNowPlayingRoute: Boolean
    ) : MiniPlayerEvent()

    // ───────────────────────────────────────────────────────────────────
    // ANIMATION EVENTS — From spring/animation callbacks
    // ───────────────────────────────────────────────────────────────────

    /**
     * Morph settle animation completed.
     *
     * @param finalProgress Where the spring settled (0 = collapsed, 1 = expanded).
     */
    data class MorphSettled(
        val finalProgress: Float
    ) : MiniPlayerEvent()

    /**
     * Morph animation reached completion (morphProgress = 1.0).
     * Mini player should hand off to Now Playing.
     */
    data object MorphComplete : MiniPlayerEvent()

    /**
     * Dismiss animation completed.
     * Mini player should be removed from composition tree.
     */
    data object DismissComplete : MiniPlayerEvent()

    /**
     * Beat detected — trigger beat reaction animation.
     */
    data object BeatDetected : MiniPlayerEvent()

    /**
     * Breathing resume delay elapsed — resume idle animations.
     */
    data object BreathingResumeTimeout : MiniPlayerEvent()

    // ───────────────────────────────────────────────────────────────────
    // SYSTEM EVENTS — From Android system
    // ───────────────────────────────────────────────────────────────────

    /**
     * System interrupt (phone call, notification shade, split-screen).
     * Reducer resolves policy from Part K of contract.
     */
    data class SystemInterrupt(
        val type: MiniPlayerInterruption.InterruptType
    ) : MiniPlayerEvent()

    /**
     * Configuration change (rotation, multi-window, density change).
     * Reducer preserves current morph state across config changes.
     */
    data object ConfigChanged : MiniPlayerEvent()

    /**
     * Memory pressure detected (onTrimMemory).
     * Quality degradation handled by PerformanceGate, not reducer.
     * Reducer just records the event for state tracking.
     */
    data class MemoryPressure(
        val level: Int  // ComponentCallbacks2 trim level
    ) : MiniPlayerEvent()

    /**
     * Screen turned off.
     * Mini player should stop all animations and become invisible.
     */
    data object ScreenOff : MiniPlayerEvent()

    /**
     * Screen turned on.
     * Mini player should resume if a song is playing.
     */
    data object ScreenOn : MiniPlayerEvent()

    /**
     * App moved to background.
     */
    data object AppBackgrounded : MiniPlayerEvent()

    /**
     * App moved to foreground.
     */
    data object AppForegrounded : MiniPlayerEvent()
}
