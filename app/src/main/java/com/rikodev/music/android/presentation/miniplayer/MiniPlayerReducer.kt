package com.rikodev.music.android.presentation.miniplayer

import com.rikodev.music.android.core.experience.ColorState
import com.rikodev.music.android.core.experience.wave.WaveState
import com.rikodev.music.android.domain.model.PlayerState
import com.rikodev.music.android.presentation.minicontact.MiniPlayerGestures
import com.rikodev.music.android.presentation.minicontact.MiniPlayerInterruption
import com.rikodev.music.android.presentation.minicontact.MiniPlayerMorphSpec
import com.rikodev.music.android.presentation.minicontact.MiniPlayerOwnership
import kotlin.math.abs
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.filled.Pause
import androidx.compose.animation.core.Animatable
import com.rikodev.music.android.domain.model.Song

// ═══════════════════════════════════════════════════════════════════════════
// RIKO MUSIC — MINI PLAYER 2.0: STATE REDUCER
//
// Build-1: Foundation. Build-2: Mapper delegation.
//
// Pure function: (State, Event) → State
// No side effects. No Compose. No Android framework. No business logic.
// Deterministic. Testable. Production-ready.
//
// Flow: MiniPlayerEvent → reduce() → MiniPlayerState → UI
//
// ⭐ RULE (Build-2): Experience OS mapping delegates to MiniPlayerMapper.
//   The reducer no longer contains raw ColorState/WaveState/PlayerState
//   mapping logic. All mapping is centralized in MiniPlayerMapper.
//   This eliminates duplicate logic between Mapper and Reducer.
//
// ⭐ RULE: This is the ONLY place that creates new MiniPlayerState instances
//   via the reducer path (events). The Mapper's mergeExperience also
//   creates instances, but only for Experience OS data merges.
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Pure reducer for [MiniPlayerState].
 *
 * Takes the current state and an event, returns a new state.
 * Same input always produces the same output (pure function).
 *
 * Organized by event source:
 *   1. Experience OS mapping (playback, color, wave, track changes)
 *   2. Gesture handling (drag, fling, cancel)
 *   3. Control feedback (press, release)
 *   4. Navigation (back, route changes)
 *   5. Animation callbacks (settle, complete, beat)
 *   6. System events (interrupt, config, screen)
 */
object MiniPlayerReducer {

    // ── Public API ──────────────────────────────────────────────────────

    /**
     * Main reduce function. Routes each event to the correct handler.
     *
     * @param state Current immutable state.
     * @param event The event that triggered the state change.
     * @return New immutable state. Never modifies the input state.
     */
    fun reduce(state: MiniPlayerState, event: MiniPlayerEvent): MiniPlayerState {
        return when (event) {
            // Experience OS data mapping
            is MiniPlayerEvent.PlaybackChanged -> reducePlayback(state, event.playerState)
            is MiniPlayerEvent.ColorChanged -> reduceColor(state, event.colorState)
            is MiniPlayerEvent.WaveChanged -> reduceWave(state, event.waveState)
            is MiniPlayerEvent.FavoriteChanged -> reduceFavorite(state, event.isFavorite)
            is MiniPlayerEvent.TrackChanged -> reduceTrackChanged(state, event.song, event.albumArtUri)

            // Gesture events
            is MiniPlayerEvent.TapExpand -> reduceTapExpand(state)
            is MiniPlayerEvent.Drag -> reduceDrag(state, event.deltaY, event.velocity, event.timestampMs)
            is MiniPlayerEvent.HorizontalDrag -> reduceHorizontalDrag(state, event.deltaX, event.velocity)
            is MiniPlayerEvent.DragReleased -> reduceDragReleased(state, event.velocity)
            is MiniPlayerEvent.FlingExpand -> reduceFlingExpand(state, event.velocity)
            is MiniPlayerEvent.FlingDismiss -> reduceFlingDismiss(state, event.velocity)
            is MiniPlayerEvent.LongPressTriggered -> reduceLongPressStart(state)
            is MiniPlayerEvent.LongPressReleased -> reduceLongPressEnd(state)
            is MiniPlayerEvent.GestureCancelled -> reduceGestureCancelled(state, event.interruptType)
            is MiniPlayerEvent.DirectionLocked -> reduceDirectionLocked(state, event.direction)

            // Control events
            is MiniPlayerEvent.PlayPausePressed -> reduceControlPress(state, PressTarget.PLAY_PAUSE)
            is MiniPlayerEvent.SkipNextPressed -> reduceControlPress(state, PressTarget.SKIP_NEXT)
            is MiniPlayerEvent.SkipPreviousPressed -> reduceControlPress(state, PressTarget.SKIP_PREVIOUS)
            is MiniPlayerEvent.FavoritePressed -> reduceControlPress(state, PressTarget.FAVORITE)
            is MiniPlayerEvent.PressReleased -> reducePressReleased(state, event.target)

            // Navigation events
            is MiniPlayerEvent.BackPressed -> reduceBackPressed(state)
            is MiniPlayerEvent.NavigatedToNowPlaying -> reduceForceExpand(state)
            is MiniPlayerEvent.ReturnedFromNowPlaying -> reduceForceCollapse(state)
            is MiniPlayerEvent.RouteChanged -> reduceRouteChanged(state, event.isNowPlayingRoute)

            // Animation callbacks
            is MiniPlayerEvent.MorphSettled -> reduceMorphSettled(state, event.finalProgress)
            is MiniPlayerEvent.MorphComplete -> reduceMorphComplete(state)
            is MiniPlayerEvent.DismissComplete -> reduceDismissComplete(state)
            is MiniPlayerEvent.BeatDetected -> reduceBeatDetected(state)
            is MiniPlayerEvent.BreathingResumeTimeout -> reduceBreathingResume(state)

            // System events
            is MiniPlayerEvent.SystemInterrupt -> reduceSystemInterrupt(state, event.type)
            is MiniPlayerEvent.ConfigChanged -> reduceConfigChanged(state)
            is MiniPlayerEvent.MemoryPressure -> state // Quality handled by PerformanceGate
            is MiniPlayerEvent.ScreenOff -> reduceScreenOff(state)
            is MiniPlayerEvent.ScreenOn -> reduceScreenOn(state)
            is MiniPlayerEvent.AppBackgrounded -> reduceScreenOff(state)
            is MiniPlayerEvent.AppForegrounded -> reduceScreenOn(state)
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // 1. EXPERIENCE OS MAPPING
    // ═════════════════════════════════════════════════════════════════

    /**
     * Map [PlayerState] into MiniPlayerState playback fields.
     * Delegates to [MiniPlayerMapper] for mapping, then handles
     * the track-change-morph-reset behavior specific to the reducer.
     */
    private fun reducePlayback(
        state: MiniPlayerState,
        playerState: PlayerState
    ): MiniPlayerState {
        val playback = MiniPlayerMapper.mapPlayback(
            playerState = playerState,
            isFavorite = state.isFavorite,
            albumArtUri = state.albumArtUri
        )

        return MiniPlayerMapper.mergeExperience(
            playback = playback,
            color = state.color,
            wave = state.wave,
            currentState = state
        )
    }

    /**
     * Map [ColorState] into [MiniPlayerColorSnapshot].
     * Delegates to [MiniPlayerMapper] — eliminates duplicate mapping logic.
     */
    private fun reduceColor(
        state: MiniPlayerState,
        colorState: ColorState
    ): MiniPlayerState {
        return state.copy(
            color = MiniPlayerMapper.mapColors(colorState)
        )
    }

    /**
     * Map [WaveState] into [MiniPlayerWaveSnapshot].
     * Delegates to [MiniPlayerMapper] — eliminates duplicate mapping logic.
     */
    private fun reduceWave(
        state: MiniPlayerState,
        waveState: WaveState
    ): MiniPlayerState {
        return state.copy(
            wave = MiniPlayerMapper.mapWave(waveState)
        )
    }

    private fun reduceFavorite(
        state: MiniPlayerState,
        isFavorite: Boolean
    ): MiniPlayerState {
        return state.copy(isFavorite = isFavorite)
    }

    /**
     * Handle track change — update song and album art.
     * If morph is in progress, reset to collapsed (seamless UX).
     */
    private fun reduceTrackChanged(
        state: MiniPlayerState,
        song: Song?,
        albumArtUri: String?
    ): MiniPlayerState {
        val wasExpanded = state.morph.morphProgress > 0f

        return state.copy(
            currentSong = song,
            albumArtUri = albumArtUri,
            isVisible = song != null
        ).let { newState ->
            if (wasExpanded && song != null) {
                // Track changed while expanded — reset morph to collapsed
                newState.copy(
                    morph = MiniPlayerMorphState.IDLE,
                    gesture = MiniPlayerGestureState.IDLE,
                    interaction = MiniPlayerInteractionState.IDLE,
                    isExpanded = false
                )
            } else {
                newState
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // 2. GESTURE HANDLING
    // ═════════════════════════════════════════════════════════════════

    /**
     * Surface tap — start expand morph (settling animation).
     * Blocked during error or loading state.
     */
    private fun reduceTapExpand(state: MiniPlayerState): MiniPlayerState {
        if (state.isMorphBlocked) return state
        if (state.morph.morphProgress > 0f) return state // Already morphing

        return state.copy(
            morph = state.morph.copy(
                isSettling = true,
                currentPhase = MorphPhase.STRETCH,
                direction = MiniPlayerMorphState.MorphDirection.EXPANDING,
                owner = MorphOwner.MINI_PLAYER
            ),
            interaction = state.interaction.copy(
                breathPaused = true
            )
        )
    }

    /**
     * Vertical drag — the core morph interaction.
     *
     * Maps drag distance to morphProgress using VERTICAL_MORPH_DISTANCE.
     * Updates gesture tracking state.
     * Pauses breathing animation.
     */
    private fun reduceDrag(
        state: MiniPlayerState,
        deltaY: Float,
        velocity: Float,
        timestampMs: Long
    ): MiniPlayerState {
        if (!state.isVisible || state.currentSong == null) return state
        if (state.isMorphBlocked && state.morph.morphProgress == 0f) return state

        // If fully expanded, only allow downward drag (reverse morph)
        if (state.morph.morphProgress >= 1f && deltaY < 0f) return state

        val currentProgress = state.morph.morphProgress
        val maxDrag = MiniPlayerGestures.VERTICAL_MORPH_DISTANCE

        // Map drag delta to morph progress change
        val progressDelta = -deltaY / maxDrag
        val newProgress = (currentProgress + progressDelta).coerceIn(0f, 1f)

        // Handle dismiss: if at collapsed and dragging down
        val isDismissing = currentProgress <= 0f && deltaY > 0f
        val newDismissOffset = if (isDismissing) {
            state.morph.dragOffsetY + deltaY
        } else {
            0f
        }

        val newDirection = when {
            isDismissing -> MiniPlayerMorphState.MorphDirection.DISMISSING
            deltaY < 0f -> MiniPlayerMorphState.MorphDirection.EXPANDING
            deltaY > 0f -> MiniPlayerMorphState.MorphDirection.CONTRACTING
            else -> state.morph.direction
        }

        return state.copy(
            morph = state.morph.copy(
                morphProgress = newProgress,
                isDragging = true,
                dragOffsetY = newDismissOffset,
                dragVelocityY = velocity,
                isDismissing = isDismissing,
                currentPhase = if (isDismissing) MorphPhase.DISMISSING else phaseFromProgress(newProgress),
                direction = newDirection,
                owner = MorphOwner.valueOf(MiniPlayerOwnership.ownerAt(newProgress, isForward = deltaY <= 0f)),
                lastUpdateTimeMs = timestampMs
            ),
            gesture = state.gesture.copy(
                isActive = true,
                direction = GestureDirection.VERTICAL,
                accumulatedDragY = state.gesture.accumulatedDragY + deltaY,
                velocityY = velocity,
                isDirectionLocked = true
            ),
            interaction = state.interaction.copy(breathPaused = true),
            isExpanded = newProgress > 0f
        )
    }

    /**
     * Horizontal drag — skip gesture tracking.
     * Does NOT change morph state. Only updates gesture accumulator.
     */
    private fun reduceHorizontalDrag(
        state: MiniPlayerState,
        deltaX: Float,
        velocity: Float
    ): MiniPlayerState {
        if (!state.isVisible || state.currentSong == null) return state

        return state.copy(
            gesture = state.gesture.copy(
                isActive = true,
                direction = GestureDirection.HORIZONTAL,
                accumulatedDragX = state.gesture.accumulatedDragX + deltaX,
                velocityX = velocity,
                isDirectionLocked = true
            )
        )
    }

    /**
     * Drag released — decide: commit expand, commit dismiss, or spring back.
     *
     * Decision based on:
     *   - Current morphProgress
     *   - Release velocity (fling detection)
     *   - Contract thresholds
     */
    private fun reduceDragReleased(state: MiniPlayerState, velocity: Float): MiniPlayerState {
        if (!state.gesture.isActive) return state

        val progress = state.morph.morphProgress
        val isUpwardVelocity = velocity < 0f  // Negative = upward
        val absVelocity = abs(velocity)

        return when {
            // Fling expand: fast upward fling
            isUpwardVelocity && absVelocity > MiniPlayerGestures.FLING_EXPAND_VELOCITY -> {
                state.copy(
                    morph = state.morph.copy(
                        isDragging = false,
                        isSettling = true,
                        dragVelocityY = velocity,
                        direction = MiniPlayerMorphState.MorphDirection.EXPANDING
                    ),
                    gesture = MiniPlayerGestureState.IDLE
                )
            }

            // Fling dismiss: fast downward fling from collapsed
            !isUpwardVelocity && absVelocity > MiniPlayerGestures.FLING_DISMISS_VELOCITY
                && progress <= 0.1f -> {
                state.copy(
                    morph = state.morph.copy(
                        isDragging = false,
                        isDismissing = true,
                        dragVelocityY = velocity,
                        direction = MiniPlayerMorphState.MorphDirection.DISMISSING
                    ),
                    gesture = MiniPlayerGestureState.IDLE
                )
            }

            // Commit expand: dragged past commit threshold
            progress >= MiniPlayerMorphSpec.EXPAND_COMMIT_THRESHOLD -> {
                state.copy(
                    morph = state.morph.copy(
                        isDragging = false,
                        isSettling = true,
                        dragVelocityY = velocity,
                        direction = MiniPlayerMorphState.MorphDirection.EXPANDING
                    ),
                    gesture = MiniPlayerGestureState.IDLE
                )
            }

            // Commit dismiss: dragged down from collapsed
            state.morph.isDismissing && state.morph.dragOffsetY > 100f -> {
                state.copy(
                    morph = state.morph.copy(
                        isDragging = false,
                        isDismissing = true,
                        dragVelocityY = velocity,
                        direction = MiniPlayerMorphState.MorphDirection.DISMISSING
                    ),
                    gesture = MiniPlayerGestureState.IDLE
                )
            }

            // Spring back: didn't commit — return to origin
            else -> {
                state.copy(
                    morph = state.morph.copy(
                        isDragging = false,
                        isSettling = true,
                        dragVelocityY = velocity,
                        direction = if (progress > 0.5f) {
                            MiniPlayerMorphState.MorphDirection.EXPANDING
                        } else {
                            MiniPlayerMorphState.MorphDirection.CONTRACTING
                        }
                    ),
                    gesture = MiniPlayerGestureState.IDLE
                )
            }
        }
    }

    /**
     * Fast upward fling — directly commit expand.
     */
    private fun reduceFlingExpand(state: MiniPlayerState, velocity: Float): MiniPlayerState {
        if (state.isMorphBlocked) return state

        return state.copy(
            morph = state.morph.copy(
                isDragging = false,
                isSettling = true,
                dragVelocityY = -abs(velocity), // Ensure upward
                direction = MiniPlayerMorphState.MorphDirection.EXPANDING
            ),
            gesture = MiniPlayerGestureState.IDLE,
            interaction = state.interaction.copy(breathPaused = true)
        )
    }

    /**
     * Fast downward fling — directly commit dismiss.
     */
    private fun reduceFlingDismiss(state: MiniPlayerState, velocity: Float): MiniPlayerState {
        if (state.morph.morphProgress > MiniPlayerMorphSpec.EXPAND_COMMIT_THRESHOLD) {
            // Too far expanded — spring back instead of dismiss
            return state.copy(
                morph = state.morph.copy(
                    isDragging = false,
                    isSettling = true,
                    direction = MiniPlayerMorphState.MorphDirection.CONTRACTING
                ),
                gesture = MiniPlayerGestureState.IDLE
            )
        }

        return state.copy(
            morph = state.morph.copy(
                isDragging = false,
                isDismissing = true,
                dragVelocityY = abs(velocity),
                direction = MiniPlayerMorphState.MorphDirection.DISMISSING
            ),
            gesture = MiniPlayerGestureState.IDLE
        )
    }

    private fun reduceLongPressStart(state: MiniPlayerState): MiniPlayerState {
        return state.copy(
            gesture = state.gesture.copy(isLongPressing = true),
            interaction = state.interaction.copy(
                hapticPending = HapticType.LONG_PRESS
            )
        )
    }

    private fun reduceLongPressEnd(state: MiniPlayerState): MiniPlayerState {
        return state.copy(
            gesture = state.gesture.copy(isLongPressing = false)
        )
    }

    /**
     * Gesture cancelled — apply interruption policy from Part K.
     * Uses resolvePolicy() from contract to determine behavior.
     */
    private fun reduceGestureCancelled(
        state: MiniPlayerState,
        interruptType: MiniPlayerInterruption.InterruptType
    ): MiniPlayerState {
        val currentAnimationName = when {
            state.morph.isDragging && state.morph.direction == MiniPlayerMorphState.MorphDirection.EXPANDING -> "EXPANDING"
            state.morph.isDragging && state.morph.direction == MiniPlayerMorphState.MorphDirection.CONTRACTING -> "CONTRACTING"
            state.morph.isSettling -> "SETTLING"
            state.morph.isDismissing -> "DISMISSING"
            state.morph.morphProgress > 0f -> "EXPANDING"  // Default to expanding if mid-morph
            else -> "IDLE"
        }

        val policy = MiniPlayerInterruption.resolvePolicy(
            currentAnimation = currentAnimationName,
            interruptType = interruptType,
            morphProgress = state.morph.morphProgress
        )

        return when (policy) {
            MiniPlayerInterruption.InterruptPolicy.REVERSE -> state.copy(
                morph = state.morph.copy(
                    isDragging = false,
                    isSettling = true,
                    direction = MiniPlayerMorphState.MorphDirection.CONTRACTING
                ),
                gesture = MiniPlayerGestureState.IDLE
            )

            MiniPlayerInterruption.InterruptPolicy.CANCEL -> state.copy(
                morph = MiniPlayerMorphState.IDLE,
                gesture = MiniPlayerGestureState.IDLE,
                interaction = MiniPlayerInteractionState.IDLE,
                isExpanded = false
            )

            MiniPlayerInterruption.InterruptPolicy.CANCEL_TO_DESTINATION -> state.copy(
                morph = state.morph.copy(
                    isDragging = false,
                    isDismissing = true,
                    isSettling = false
                ),
                gesture = MiniPlayerGestureState.IDLE
            )

            MiniPlayerInterruption.InterruptPolicy.PRESERVE -> state // Keep current state

            MiniPlayerInterruption.InterruptPolicy.PAUSE -> state.copy(
                morph = state.morph.copy(isDragging = false),
                interaction = state.interaction.copy(breathPaused = true)
            )

            MiniPlayerInterruption.InterruptPolicy.ACCELERATE -> state.copy(
                morph = state.morph.copy(isDragging = false, isSettling = true),
                gesture = MiniPlayerGestureState.IDLE
            )

            MiniPlayerInterruption.InterruptPolicy.CONTINUE -> state // No change
        }
    }

    private fun reduceDirectionLocked(
        state: MiniPlayerState,
        direction: GestureDirection
    ): MiniPlayerState {
        return state.copy(
            gesture = state.gesture.copy(direction = direction, isDirectionLocked = true),
            interaction = state.interaction.copy(
                hapticPending = HapticType.GESTURE_LOCK
            )
        )
    }

    // ═════════════════════════════════════════════════════════════════
    // 3. CONTROL FEEDBACK
    // ═════════════════════════════════════════════════════════════════

    /**
     * Button pressed — scale down + queue haptic.
     * Does NOT toggle playback here — that's an Action, handled by ViewModel.
     */
    private fun reduceControlPress(
        state: MiniPlayerState,
        target: PressTarget
    ): MiniPlayerState {
        return state.copy(
            interaction = state.interaction.copy(
                isPressed = true,
                pressTarget = target,
                pressScale = 0.92f,
                hapticPending = HapticType.TAP
            )
        )
    }

    /**
     * Button released — spring scale back.
     */
    private fun reducePressReleased(
        state: MiniPlayerState,
        target: PressTarget
    ): MiniPlayerState {
        return if (state.interaction.pressTarget == target) {
            state.copy(
                interaction = state.interaction.copy(
                    isPressed = false,
                    pressTarget = PressTarget.NONE,
                    pressScale = 1f
                )
            )
        } else {
            state // Press target mismatch — ignore
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // 4. NAVIGATION
    // ═════════════════════════════════════════════════════════════════

    /**
     * Back pressed — REVERSE morph if in progress (Part K policy).
     */
    private fun reduceBackPressed(state: MiniPlayerState): MiniPlayerState {
        return if (state.morph.morphProgress > 0f) {
            state.copy(
                morph = state.morph.copy(
                    isDragging = false,
                    isSettling = true,
                    direction = MiniPlayerMorphState.MorphDirection.CONTRACTING
                ),
                gesture = MiniPlayerGestureState.IDLE
            )
        } else {
            state // At rest — back press goes to navigation, not reducer
        }
    }

    /**
     * Navigated to Now Playing — force expand (instant).
     */
    private fun reduceForceExpand(state: MiniPlayerState): MiniPlayerState {
        return state.copy(
            morph = MiniPlayerMorphState(
                morphProgress = 1f,
                owner = MorphOwner.NOW_PLAYING,
                currentPhase = MorphPhase.HANDOFF
            ),
            isExpanded = true
        )
    }

    /**
     * Returned from Now Playing — force collapse.
     */
    private fun reduceForceCollapse(state: MiniPlayerState): MiniPlayerState {
        return state.copy(
            morph = MiniPlayerMorphState.IDLE,
            gesture = MiniPlayerGestureState.IDLE,
            interaction = MiniPlayerInteractionState.IDLE,
            isExpanded = false
        )
    }

    /**
     * Route changed — hide mini player when on Now Playing route.
     */
    private fun reduceRouteChanged(
        state: MiniPlayerState,
        isNowPlayingRoute: Boolean
    ): MiniPlayerState {
        return if (isNowPlayingRoute) {
            state.copy(isVisible = false)
        } else {
            state.copy(isVisible = state.currentSong != null)
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // 5. ANIMATION CALLBACKS
    // ═════════════════════════════════════════════════════════════════

    /**
     * Morph settle animation completed.
     * If settled at or above commit threshold → mark complete.
     * If settled below → collapse to idle.
     */
    private fun reduceMorphSettled(
        state: MiniPlayerState,
        finalProgress: Float
    ): MiniPlayerState {
        val clampedProgress = finalProgress.coerceIn(0f, 1f)
        val isExpanding = state.morph.direction == MiniPlayerMorphState.MorphDirection.EXPANDING

        return if (isExpanding && clampedProgress >= MiniPlayerMorphSpec.EXPAND_COMMIT_THRESHOLD) {
            // Settled above commit → continue to full expand
            state.copy(
                morph = state.morph.copy(
                    morphProgress = clampedProgress,
                    isSettling = true,
                    currentPhase = phaseFromProgress(clampedProgress)
                ),
                isExpanded = true
            )
        } else if (!isExpanding || clampedProgress < 0.05f) {
            // Settled back to collapsed
            state.copy(
                morph = MiniPlayerMorphState.IDLE,
                isExpanded = false
            )
        } else {
            // Mid-settle — update progress
            state.copy(
                morph = state.morph.copy(
                    morphProgress = clampedProgress,
                    isSettling = false,
                    currentPhase = phaseFromProgress(clampedProgress)
                )
            )
        }
    }

    /**
     * Morph reached 1.0 — handoff to Now Playing.
     */
    private fun reduceMorphComplete(state: MiniPlayerState): MiniPlayerState {
        return state.copy(
            morph = state.morph.copy(
                morphProgress = 1f,
                isSettling = false,
                currentPhase = MorphPhase.HANDOFF,
                owner = MorphOwner.NOW_PLAYING
            ),
            isExpanded = true,
            isVisible = false // Mini player exits, Now Playing takes over
        )
    }

    /**
     * Dismiss animation completed.
     */
    private fun reduceDismissComplete(state: MiniPlayerState): MiniPlayerState {
        return state.copy(
            morph = MiniPlayerMorphState.IDLE,
            gesture = MiniPlayerGestureState.IDLE,
            interaction = MiniPlayerInteractionState.IDLE,
            isVisible = false,
            isExpanded = false
        )
    }

    /**
     * Beat detected — trigger beat reaction in interaction state.
     */
    private fun reduceBeatDetected(state: MiniPlayerState): MiniPlayerState {
        if (state.gesture.isActive) return state // Don't react during gesture

        return state.copy(
            interaction = state.interaction.copy(
                isBeatReacting = true,
                beatProgress = 0f
            )
        )
    }

    /**
     * Breathing resume timeout — allow breathing animation to restart.
     */
    private fun reduceBreathingResume(state: MiniPlayerState): MiniPlayerState {
        return state.copy(
            interaction = state.interaction.copy(breathPaused = false)
        )
    }

    // ═════════════════════════════════════════════════════════════════
    // 6. SYSTEM EVENTS
    // ═════════════════════════════════════════════════════════════════

    private fun reduceSystemInterrupt(
        state: MiniPlayerState,
        type: MiniPlayerInterruption.InterruptType
    ): MiniPlayerState {
        // Delegate to gesture cancel with the specific interrupt type
        return reduceGestureCancelled(state, type)
    }

    private fun reduceConfigChanged(state: MiniPlayerState): MiniPlayerState {
        // Preserve current morph state across config changes (Part K: PRESERVE)
        return state
    }

    private fun reduceScreenOff(state: MiniPlayerState): MiniPlayerState {
        return state.copy(
            isVisible = false,
            morph = MiniPlayerMorphState.IDLE,
            gesture = MiniPlayerGestureState.IDLE,
            interaction = MiniPlayerInteractionState.IDLE,
            isExpanded = false
        )
    }

    private fun reduceScreenOn(state: MiniPlayerState): MiniPlayerState {
        return state.copy(
            isVisible = state.currentSong != null
        )
    }

    // ═════════════════════════════════════════════════════════════════
    // HELPERS
    // ═════════════════════════════════════════════════════════════════

    /**
     * Determine morph phase from morphProgress value.
     * Uses thresholds from Part D of contract.
     */
    internal fun phaseFromProgress(progress: Float): MorphPhase {
        return when {
            progress <= MiniPlayerMorphSpec.PHASE_STRETCH_END -> MorphPhase.STRETCH
            progress <= MiniPlayerMorphSpec.PHASE_SCALE_END -> MorphPhase.SCALE
            progress <= MiniPlayerMorphSpec.PHASE_FADE_END -> MorphPhase.FADE
            progress <= MiniPlayerMorphSpec.PHASE_WAVE_END -> MorphPhase.WAVE
            progress <= MiniPlayerMorphSpec.PHASE_ALBUM_END -> MorphPhase.ALBUM
            progress < 1f -> MorphPhase.HANDOFF
            else -> MorphPhase.HANDOFF
        }
    }
}
