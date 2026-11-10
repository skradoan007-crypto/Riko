package com.rikodev.music.android.presentation.miniplayer

import androidx.compose.ui.graphics.Color
import com.rikodev.music.android.domain.model.Song
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.filled.Pause
import androidx.compose.animation.core.Animatable

// ═══════════════════════════════════════════════════════════════════════════
// RIKO MUSIC — MINI PLAYER 2.0: STATE MODEL
//
// Build-1: Foundation. Pure Kotlin data classes. Immutable. No Compose.
// No business logic. No side effects. No Experience OS computation.
//
// ⭐ CONSUMER RULE ⭐
//   MiniPlayerState is a CONSUMER, not an OWNER.
//   It does NOT create PlayerState, ColorState, or WaveState.
//   It does NOT overlap with NowPlayingUiState.
//
//   Architecture:
//                  Experience State
//                       │
//              ┌────────┴────────┐
//              ▼                  ▼
//      MiniPlayerState    NowPlayingUiState
//        (consumer)          (consumer)
//
//   MiniPlayerState and NowPlayingUiState are siblings.
//   They share the same PARENT but never overlap.
// ═══════════════════════════════════════════════════════════════════════════

// ─────────────────────────────────────────────────────────────────────────────
// COLOR SNAPSHOT — Derived from ColorState
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Lightweight snapshot of [ColorState] for Mini Player rendering.
 * Only the fields Mini Player needs.
 *
 * Source: ColorIntelligenceEngine → ColorState → mapped to this by ViewModel.
 * Mini Player NEVER creates ColorState. It ONLY reads this snapshot.
 */
data class MiniPlayerColorSnapshot(
    val primary: Color = Color(0xFF19B7FF),
    val secondary: Color = Color(0xFF9A6CFF),
    val accent: Color = Color(0xFFFF6FD8),
    val waveStart: Color = Color(0xFF19B7FF),
    val waveEnd: Color = Color(0xFF9A6CFF),
    val glassTint: Color = Color(0x14FFFFFF),
    val ambientGlow: Color = Color(0x3319B7FF),
    val shadowColor: Color = Color(0x33000000),
    val dominantLuminance: Float = 0.15f,
    val isDarkArtwork: Boolean = true,
    val energyIntensity: Float = 0f,
    val pulseAmount: Float = 0f,
    val isIdle: Boolean = true
) {
    companion object {
        /** Default snapshot when no artwork has been analyzed. */
        val DEFAULT = MiniPlayerColorSnapshot()

        /** Fallback palette when Color Brain fails (from MiniPlayerFailure contract). */
        val FALLBACK = MiniPlayerColorSnapshot(
            primary = Color(0xFF1A1A2E),
            secondary = Color(0xFF16213E),
            accent = Color(0xFFE94560),
            isIdle = true
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WAVE SNAPSHOT — Derived from WaveState
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Lightweight snapshot of [WaveState] for Mini Player rendering.
 * Only the fields Mini Player needs.
 *
 * Source: WaveEngine → WaveState → mapped to this by ViewModel.
 * Mini Player NEVER creates WaveState. It ONLY reads this snapshot.
 */
data class MiniPlayerWaveSnapshot(
    val isActive: Boolean = false,
    val hasBeat: Boolean = false,
    val overallEnergy: Float = 0f,
    val thickness: Float = 1f,
    val opacity: Float = 1f,
    val isExpanding: Boolean = false,
    val isSettling: Boolean = false,
    val energy: Float = 0f
) {
    val isIdle: Boolean
        get() = !isActive && energy < 0.01f

    companion object {
        val DEFAULT = MiniPlayerWaveSnapshot()

        /** Breathing idle wave when real wave data is unavailable. */
        val IDLE_BREATHING = MiniPlayerWaveSnapshot(
            isActive = true,     // Still "active" visually (breathing animation)
            hasBeat = false,
            overallEnergy = 0f,
            isExpanding = false,
            isSettling = true
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MORPH STATE — Mini Player's OWN state (from gesture + animation)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Morph phase labels matching the 6-phase pipeline from the contract.
 * Used by the reducer to track which phase the morph is in.
 */
enum class MorphPhase {
    /** At rest — morphProgress = 0. Mini player collapsed. */
    IDLE,

    /** Surface stretching: 80dp → 120dp. morphProgress: 0.00 → 0.15 */
    STRETCH,

    /** Album art scaling, text growing. morphProgress: 0.15 → 0.30 */
    SCALE,

    /** Controls fading out. morphProgress: 0.30 → 0.50 */
    FADE,

    /** Wave ribbon appearing. morphProgress: 0.50 → 0.70 */
    WAVE,

    /** Album floating with shadow/reflection. morphProgress: 0.70 → 0.85 */
    ALBUM,

    /** Handoff to NowPlayingScreen. morphProgress: 0.85 → 1.00 */
    HANDOFF,

    /** Surface exiting screen (dismiss gesture). */
    DISMISSING
}

/**
 * Ownership zones during morph handoff (from Part L of contract).
 */
enum class MorphOwner {
    /** Mini Player owns everything. NowPlaying is NOT in composition tree. */
    MINI_PLAYER,

    /** Both composables in tree. Ownership split by gesture flip threshold. */
    SHARED,

    /** Now Playing owns everything. MiniPlayer is NOT in composition tree. */
    NOW_PLAYING
}

/**
 * Morph transition state.
 *
 * This is Mini Player's OWN state — computed from user gestures and
 * spring animation engine. NOT from Experience OS.
 *
 * Source: User gestures + spring animations → this.
 */
data class MiniPlayerMorphState(
    /** Visual morph progress: 0f = collapsed, 1f = fully expanded. */
    val morphProgress: Float = 0f,

    /** True while user finger is on screen and dragging vertically. */
    val isDragging: Boolean = false,

    /** Visual displacement from drag (pixels). Negative = upward. */
    val dragOffsetY: Float = 0f,

    /** Current drag velocity (pixels/second). Negative = upward. */
    val dragVelocityY: Float = 0f,

    /** True when surface is being dismissed (dragged down from collapsed). */
    val isDismissing: Boolean = false,

    /** True when a spring animation is settling morph to final position. */
    val isSettling: Boolean = false,

    /** Current phase of the morph pipeline. */
    val currentPhase: MorphPhase = MorphPhase.IDLE,

    /** Who owns the visual surface during handoff. */
    val owner: MorphOwner = MorphOwner.MINI_PLAYER,

    /** Direction of current morph transition. */
    val direction: MorphDirection = MorphDirection.NONE,

    /** Timestamp of last morph state change (for animation frame timing). */
    val lastUpdateTimeMs: Long = 0L
) {
    /** Morph direction enum — forward, reverse, or dismissing. */
    enum class MorphDirection { NONE, EXPANDING, CONTRACTING, DISMISSING }

    /** True if morph is at rest (collapsed, no animation, no drag). */
    val isAtRest: Boolean
        get() = !isDragging && !isSettling && !isDismissing && morphProgress == 0f

    /** True if morph has reached completion. */
    val isFullyExpanded: Boolean
        get() = morphProgress >= 1f

    /** True if morph is in the handoff zone (both composables in tree). */
    val isInHandoff: Boolean
        get() = owner == MorphOwner.SHARED

    companion object {
        /** Default state — collapsed, idle, no gesture. */
        val IDLE = MiniPlayerMorphState()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GESTURE STATE — From gesture recognition engine
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Direction of a recognized gesture. Set after direction lock threshold.
 */
enum class GestureDirection { NONE, HORIZONTAL, VERTICAL }

/**
 * Gesture tracking state.
 *
 * Source: Pointer events → gesture engine → this.
 * Mini Player NEVER reads raw pointer events in composables.
 */
data class MiniPlayerGestureState(
    /** True while a gesture is in progress. */
    val isActive: Boolean = false,

    /** Locked direction (set after threshold). NONE before lock. */
    val direction: GestureDirection = GestureDirection.NONE,

    /** Accumulated horizontal drag distance (pixels). */
    val accumulatedDragX: Float = 0f,

    /** Accumulated vertical drag distance (pixels). Negative = upward. */
    val accumulatedDragY: Float = 0f,

    /** Current horizontal velocity (pixels/second). */
    val velocityX: Float = 0f,

    /** Current vertical velocity (pixels/second). */
    val velocityY: Float = 0f,

    /** True while long-press is held (queue peek). */
    val isLongPressing: Boolean = false,

    /** True once gesture direction has been locked. */
    val isDirectionLocked: Boolean = false
) {
    val isAtRest: Boolean
        get() = !isActive && !isLongPressing && direction == GestureDirection.NONE

    companion object {
        val IDLE = MiniPlayerGestureState()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// INTERACTION STATE — Micro-interactions (press, haptic, breathing)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Target of a press interaction — which element is being pressed.
 */
enum class PressTarget {
    /** No press active. */
    NONE,

    /** Play/Pause button. */
    PLAY_PAUSE,

    /** Skip Next button. */
    SKIP_NEXT,

    /** Skip Previous button. */
    SKIP_PREVIOUS,

    /** Favorite/Heart button. */
    FAVORITE,

    /** Main surface area (for expand). */
    SURFACE
}

/**
 * Type of haptic feedback to fire.
 */
enum class HapticType {
    /** Light tap on button. */
    TAP,

    /** Long-press triggered. */
    LONG_PRESS,

    /** Fling gesture committed. */
    FLING,

    /** Direction lock engaged. */
    GESTURE_LOCK,

    /** Morph committed to expand/dismiss. */
    MORPH_COMMIT,

    /** Beat pulse. */
    BEAT
}

/**
 * Interaction state — press animations, haptic feedback, breathing control.
 *
 * Source: Touch events + gesture engine → this.
 */
data class MiniPlayerInteractionState(
    /** True while a press is active (finger down). */
    val isPressed: Boolean = false,

    /** Which element is being pressed. */
    val pressTarget: PressTarget = PressTarget.NONE,

    /** Current press scale (1.0 = normal, 0.95 = pressed down). */
    val pressScale: Float = 1f,

    /** Haptic feedback queued but not yet fired. Null = no pending haptic. */
    val hapticPending: HapticType? = null,

    /** True if breathing animation should pause (during gesture). */
    val breathPaused: Boolean = false,

    /** True if a beat reaction animation is active. */
    val isBeatReacting: Boolean = false,

    /** Beat reaction progress (0 → 1, then retires). */
    val beatProgress: Float = 0f
) {
    val isAtRest: Boolean
        get() = !isPressed && pressScale == 1f && hapticPending == null && !isBeatReacting

    companion object {
        val IDLE = MiniPlayerInteractionState()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LAYOUT SNAPSHOT — Pre-computed from morph progress
// ────────────────────────────────────────────────────────────────────────

/**
 * Pre-computed layout values derived from morphProgress.
 *
 * Components read these directly — zero computation in composables.
 * Computed by the ViewModel/state mapper using MiniPlayerMorphSpec
 * interpolation functions from the contract.
 *
 * Source: morphProgress → MiniPlayerMorphSpec → this.
 */
data class MiniPlayerLayoutSnapshot(
    /** Current surface height (dp). 80dp collapsed → 240dp expanded. */
    val height: Float = 80f,

    /** Current width as fraction of screen (0.94 → 1.0). */
    val widthFraction: Float = 0.94f,

    /** Current corner radius (dp). 28dp → 0dp. */
    val cornerRadius: Float = 28f,

    /** Current album art size (dp). 52dp → 180dp. */
    val albumArtSize: Float = 52f,

    /** Transport controls opacity. 1.0 → 0.0 during Phase 3 (FADE). */
    val controlsOpacity: Float = 1f,

    /** Wave ribbon visibility. 0.0 → 1.0 during Phase 4 (WAVE). */
    val waveVisibility: Float = 0f,

    /** Glass tint alpha. Decreases during morph. */
    val glassTintAlpha: Float = 0.10f,

    /** Glass shadow alpha. Decreases during morph. */
    val glassShadowAlpha: Float = 0.20f,

    /** Shadow elevation (dp). Increases during morph. */
    val shadowElevation: Float = 8f,

    /** Ambient glow radius (dp). Appears during expansion. */
    val glowRadius: Float = 0f,

    /** Ambient glow intensity. Modulated by energy. */
    val glowIntensity: Float = 0f,

    /** Album art glass reflection alpha. Appears after Phase 2. */
    val reflectionAlpha: Float = 0f,

    /** Wave ribbon thickness multiplier (0.5 → 1.0). */
    val waveThickness: Float = 0.5f,

    /** Mini Player surface opacity (for crossfade). */
    val miniPlayerOpacity: Float = 1f,

    /** Now Playing surface opacity (for crossfade). */
    val nowPlayingOpacity: Float = 0f
) {
    companion object {
        /** Collapsed state — all defaults. */
        val COLLAPSED = MiniPlayerLayoutSnapshot()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MAIN STATE — The single source of truth for Mini Player UI
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The complete, immutable state for Mini Player 2.0.
 *
 * Every pixel Mini Player renders is derived from this single data class.
 * Components collect one StateFlow of this type and render.
 * No direct access to Experience OS, no callback spaghetti.
 *
 * ═══ IMMUTABLE ═══
 *   This is a data class. UI cannot mutate it.
 *   Every change creates a new instance via copy().
 *   Flow: PlayerViewModel → StateFlow<MiniPlayerState> → Composable.
 *
 * ═══ CONSUMER RULE ═══
 *   MiniPlayerState does NOT own:
 *     - PlayerState (read-only snapshot)
 *     - ColorState (read-only snapshot)
 *     - WaveState (read-only snapshot)
 *   It does NOT overlap with NowPlayingUiState.
 *   It does NOT contain any business logic state.
 *
 * ═══ NO BUSINESS LOGIC ═══
 *   This state does NOT contain: FFT, Audio Analyzer, Physics Solver,
 *   Palette Extraction, Playback Logic, Queue Logic.
 *   All of those live in Experience OS.
 *
 * ═══ COMPOSITION ═══
 *   Contains 8 sub-states for logical grouping:
 *     color     — from ColorState (Experience OS)
 *     wave      — from WaveState (Experience OS)
 *     morph     — from gesture + animation (local)
 *     gesture   — from gesture engine (local)
 *     interaction — press, haptic, breathing (local)
 *     layout    — pre-computed from morph (derived)
 *     playback  — flat from PlayerState (Experience OS)
 *     visibility — UI visibility control (local)
 */
data class MiniPlayerState(
    // ── Visibility ─────────────────────────────────────────────────────
    /** True when mini player should be in composition tree. */
    val isVisible: Boolean = false,

    /** True when morph is in progress (morphProgress > 0). */
    val isExpanded: Boolean = false,

    // ── Playback (from PlayerState, via ViewModel) ────────────────────
    /** Current song. Null when nothing is playing. */
    val currentSong: Song? = null,

    /** True when media is actively playing. */
    val isPlaying: Boolean = false,

    /** Playback progress: 0.0 → 1.0. */
    val progress: Float = 0f,

    /** Current position in milliseconds. */
    val position: Long = 0L,

    /** Total duration in milliseconds. */
    val duration: Long = 0L,

    /** Album art content URI string. Null if no artwork. */
    val albumArtUri: String? = null,

    /** Whether current song is in favorites. */
    val isFavorite: Boolean = false,

    /** True while next track is loading. */
    val isLoading: Boolean = false,

    /** Error message if playback failed. Null if no error. */
    val playbackError: String? = null,

    // ── Experience Colors (from ColorState, via ViewModel) ───────────
    /** Color snapshot for rendering. Never hardcoded — always from Color Brain. */
    val color: MiniPlayerColorSnapshot = MiniPlayerColorSnapshot(),

    // ── Wave (from WaveState, via ViewModel) ─────────────────────────
    /** Wave snapshot for rendering. Same data source as Now Playing wave. */
    val wave: MiniPlayerWaveSnapshot = MiniPlayerWaveSnapshot(),

    // ── Morph (from gesture + animation, local) ──────────────────────
    /** Morph transition state. Mini Player's OWN state. */
    val morph: MiniPlayerMorphState = MiniPlayerMorphState(),

    // ── Gesture (from gesture engine, local) ──────────────────────────
    /** Current gesture tracking state. */
    val gesture: MiniPlayerGestureState = MiniPlayerGestureState(),

    // ── Interaction (press, haptic, breathing) ────────────────────────
    /** Micro-interaction state for press feedback and haptics. */
    val interaction: MiniPlayerInteractionState = MiniPlayerInteractionState(),

    // ── Layout (pre-computed from morph, derived) ──────────────────────
    /** All layout values computed from morphProgress. Zero computation in UI. */
    val layout: MiniPlayerLayoutSnapshot = MiniPlayerLayoutSnapshot()
) {
    /** True when no song is loaded. */
    val hasNoSong: Boolean
        get() = currentSong == null

    /** True when song is playing AND mini player is visible. */
    val isActive: Boolean
        get() = isVisible && currentSong != null

    /** True when morph is in progress (not idle, not fully complete). */
    val isMorphing: Boolean
        get() = morph.morphProgress > 0f && morph.morphProgress < 1f

    /** True when playback has encountered an error. */
    val hasError: Boolean
        get() = playbackError != null

    /** True when mini player should block morph (error state, loading). */
    val isMorphBlocked: Boolean
        get() = hasError || isLoading

    companion object {
        /** Initial state — nothing playing, nothing visible. */
        val IDLE = MiniPlayerState()
    }
}
