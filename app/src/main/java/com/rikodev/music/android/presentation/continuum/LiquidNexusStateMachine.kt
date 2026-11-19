package com.rikodev.music.android.presentation.continuum

// ─────────────────────────────────────────────────────────────────────────────
// RIKO LIQUID NEXUS — DETERMINISTIC STATE MACHINE (P4, P5, P6, P8)
//
// Single-source-of-truth presentation state for the Liquid Music Dock.
// Eliminates conflicting booleans and impossible UI states:
//   Compact <-> Expanding <-> Expanded <-> Scrubbing / ModeSelection
//                                   ↕
//                              Collapsing
//                                   ↕
//                              FullPlayer
//
// Fully interruptible: expand/collapse can reverse mid-flight, Play/Pause
// always responds immediately, track changes during scrubbing reset safely,
// and incomplete transitions normalize cleanly on lifecycle restore.
// ─────────────────────────────────────────────────────────────────────────────

enum class LiquidNexusMode {
    Compact,
    Expanding,
    Expanded,
    Scrubbing,
    ModeSelection,
    Collapsing,
    FullPlayer;

    val isExpandedOrTransitioning: Boolean
        get() = this == Expanding || this == Expanded || this == Scrubbing || this == ModeSelection

    val isScrubbing: Boolean
        get() = this == Scrubbing
}

data class LiquidNexusState(
    val mode: LiquidNexusMode = LiquidNexusMode.Compact,
    val activeTrackId: Long? = null,
    val scrubFraction: Float? = null,
    val scrubPreviewMs: Long = 0L,
    val scrubDurationMs: Long = 0L,
    val secondaryMenuOpen: Boolean = false
) {
    /** True when the background music content layer should blur/dim for scrubbing focus. */
    val shouldBlurBackground: Boolean
        get() = mode == LiquidNexusMode.Scrubbing

    /** True when the floating Liquid Nexus dock/chamber should be rendered over the shell. */
    val isDockVisible: Boolean
        get() = activeTrackId != null && mode != LiquidNexusMode.FullPlayer
}

sealed interface LiquidNexusEvent {
    data object ExpandRequested : LiquidNexusEvent
    data object CollapseRequested : LiquidNexusEvent
    data class AnimationSettled(val target: LiquidNexusMode) : LiquidNexusEvent
    data object PlayPauseRequested : LiquidNexusEvent
    data class TrackChanged(val songId: Long?, val durationMs: Long) : LiquidNexusEvent
    data class ScrubStarted(val initialFraction: Float, val durationMs: Long) : LiquidNexusEvent
    data class ScrubPositionChanged(val fraction: Float) : LiquidNexusEvent
    data object ScrubCommitted : LiquidNexusEvent
    data object ScrubCanceled : LiquidNexusEvent
    data object ModeSelectionToggled : LiquidNexusEvent
    data object ModeSelectionDismissed : LiquidNexusEvent
    data object SecondaryMenuToggled : LiquidNexusEvent
    data object SecondaryMenuDismissed : LiquidNexusEvent
    data object FullPlayerRequested : LiquidNexusEvent
    data object FullPlayerDismissed : LiquidNexusEvent
    data object BackPressed : LiquidNexusEvent
    data object LifecycleBackgrounded : LiquidNexusEvent
    data object LifecycleRestored : LiquidNexusEvent
}

sealed interface LiquidNexusEffect {
    data object TogglePlayPause : LiquidNexusEffect
    data class CommitSeek(val positionMs: Long) : LiquidNexusEffect
    data object OpenFullPlayer : LiquidNexusEffect
    data object CloseFullPlayer : LiquidNexusEffect
    data object BackNotConsumed : LiquidNexusEffect
}

object LiquidNexusStateMachine {

    fun reduce(
        state: LiquidNexusState,
        event: LiquidNexusEvent
    ): Pair<LiquidNexusState, List<LiquidNexusEffect>> = when (event) {

        is LiquidNexusEvent.PlayPauseRequested -> {
            // Functional priority: Play/Pause executes immediately regardless of animation phase
            state to listOf(LiquidNexusEffect.TogglePlayPause)
        }

        is LiquidNexusEvent.ExpandRequested -> {
            if (state.activeTrackId == null) {
                state to emptyList()
            } else {
                when (state.mode) {
                    LiquidNexusMode.Compact,
                    LiquidNexusMode.Collapsing -> state.copy(
                        mode = LiquidNexusMode.Expanding,
                        secondaryMenuOpen = false
                    ) to emptyList()
                    else -> state to emptyList()
                }
            }
        }

        is LiquidNexusEvent.CollapseRequested -> {
            when (state.mode) {
                LiquidNexusMode.Expanding,
                LiquidNexusMode.Expanded,
                LiquidNexusMode.Scrubbing,
                LiquidNexusMode.ModeSelection -> state.copy(
                    mode = LiquidNexusMode.Collapsing,
                    scrubFraction = null,
                    secondaryMenuOpen = false
                ) to emptyList()
                LiquidNexusMode.FullPlayer -> state.copy(
                    mode = LiquidNexusMode.Expanded,
                    secondaryMenuOpen = false
                ) to listOf(LiquidNexusEffect.CloseFullPlayer)
                else -> state to emptyList()
            }
        }

        is LiquidNexusEvent.AnimationSettled -> {
            val nextMode = when {
                state.mode == LiquidNexusMode.Expanding && event.target == LiquidNexusMode.Expanded ->
                    LiquidNexusMode.Expanded
                state.mode == LiquidNexusMode.Collapsing && event.target == LiquidNexusMode.Compact ->
                    LiquidNexusMode.Compact
                else -> state.mode
            }
            state.copy(mode = nextMode) to emptyList()
        }

        is LiquidNexusEvent.TrackChanged -> {
            if (event.songId == null) {
                LiquidNexusState(mode = LiquidNexusMode.Compact, activeTrackId = null) to emptyList()
            } else {
                val trackActuallyChanged = state.activeTrackId != null && state.activeTrackId != event.songId
                val normalizedMode = if (trackActuallyChanged && state.mode == LiquidNexusMode.Scrubbing) {
                    // Safely exit scrubbing on unexpected track change without committing stale position
                    LiquidNexusMode.Expanded
                } else {
                    state.mode
                }
                state.copy(
                    mode = normalizedMode,
                    activeTrackId = event.songId,
                    scrubFraction = if (trackActuallyChanged) null else state.scrubFraction,
                    scrubDurationMs = event.durationMs.coerceAtLeast(0L)
                ) to emptyList()
            }
        }

        is LiquidNexusEvent.ScrubStarted -> {
            if (state.mode == LiquidNexusMode.Expanded ||
                state.mode == LiquidNexusMode.ModeSelection ||
                state.mode == LiquidNexusMode.Scrubbing
            ) {
                val clamped = event.initialFraction.coerceIn(0f, 1f)
                val safeDuration = event.durationMs.coerceAtLeast(0L)
                state.copy(
                    mode = LiquidNexusMode.Scrubbing,
                    scrubFraction = clamped,
                    scrubDurationMs = safeDuration,
                    scrubPreviewMs = (clamped * safeDuration).toLong().coerceIn(0L, safeDuration),
                    secondaryMenuOpen = false
                ) to emptyList()
            } else {
                state to emptyList()
            }
        }

        is LiquidNexusEvent.ScrubPositionChanged -> {
            if (state.mode == LiquidNexusMode.Scrubbing) {
                val clamped = event.fraction.coerceIn(0f, 1f)
                val safeDuration = state.scrubDurationMs.coerceAtLeast(0L)
                state.copy(
                    scrubFraction = clamped,
                    scrubPreviewMs = (clamped * safeDuration).toLong().coerceIn(0L, safeDuration)
                ) to emptyList()
            } else {
                state to emptyList()
            }
        }

        is LiquidNexusEvent.ScrubCommitted -> {
            if (state.mode == LiquidNexusMode.Scrubbing) {
                val seekTarget = state.scrubPreviewMs.coerceAtLeast(0L)
                val effects = if (state.scrubDurationMs > 0L) {
                    listOf(LiquidNexusEffect.CommitSeek(seekTarget))
                } else {
                    emptyList()
                }
                state.copy(
                    mode = LiquidNexusMode.Expanded,
                    scrubFraction = null
                ) to effects
            } else {
                state to emptyList()
            }
        }

        is LiquidNexusEvent.ScrubCanceled -> {
            if (state.mode == LiquidNexusMode.Scrubbing) {
                state.copy(
                    mode = LiquidNexusMode.Expanded,
                    scrubFraction = null
                ) to emptyList()
            } else {
                state to emptyList()
            }
        }

        is LiquidNexusEvent.ModeSelectionToggled -> {
            when (state.mode) {
                LiquidNexusMode.Expanded -> state.copy(
                    mode = LiquidNexusMode.ModeSelection,
                    secondaryMenuOpen = false
                ) to emptyList()
                LiquidNexusMode.ModeSelection -> state.copy(
                    mode = LiquidNexusMode.Expanded
                ) to emptyList()
                else -> state to emptyList()
            }
        }

        is LiquidNexusEvent.ModeSelectionDismissed -> {
            if (state.mode == LiquidNexusMode.ModeSelection) {
                state.copy(mode = LiquidNexusMode.Expanded) to emptyList()
            } else {
                state to emptyList()
            }
        }

        is LiquidNexusEvent.SecondaryMenuToggled -> {
            if (state.mode == LiquidNexusMode.Expanded || state.mode == LiquidNexusMode.ModeSelection) {
                state.copy(
                    mode = LiquidNexusMode.Expanded,
                    secondaryMenuOpen = !state.secondaryMenuOpen
                ) to emptyList()
            } else {
                state to emptyList()
            }
        }

        is LiquidNexusEvent.SecondaryMenuDismissed -> {
            state.copy(secondaryMenuOpen = false) to emptyList()
        }

        is LiquidNexusEvent.FullPlayerRequested -> {
            if (state.activeTrackId != null) {
                state.copy(
                    mode = LiquidNexusMode.FullPlayer,
                    scrubFraction = null,
                    secondaryMenuOpen = false
                ) to listOf(LiquidNexusEffect.OpenFullPlayer)
            } else {
                state to emptyList()
            }
        }

        is LiquidNexusEvent.FullPlayerDismissed -> {
            if (state.mode == LiquidNexusMode.FullPlayer) {
                state.copy(
                    mode = LiquidNexusMode.Expanded,
                    secondaryMenuOpen = false
                ) to emptyList()
            } else {
                state to emptyList()
            }
        }

        is LiquidNexusEvent.BackPressed -> {
            when {
                state.secondaryMenuOpen -> state.copy(secondaryMenuOpen = false) to emptyList()
                state.mode == LiquidNexusMode.Scrubbing -> state.copy(
                    mode = LiquidNexusMode.Expanded,
                    scrubFraction = null
                ) to emptyList()
                state.mode == LiquidNexusMode.ModeSelection -> state.copy(
                    mode = LiquidNexusMode.Expanded
                ) to emptyList()
                state.mode == LiquidNexusMode.FullPlayer -> state.copy(
                    mode = LiquidNexusMode.Expanded
                ) to listOf(LiquidNexusEffect.CloseFullPlayer)
                state.mode == LiquidNexusMode.Expanded || state.mode == LiquidNexusMode.Expanding -> state.copy(
                    mode = LiquidNexusMode.Collapsing,
                    scrubFraction = null
                ) to emptyList()
                else -> state to listOf(LiquidNexusEffect.BackNotConsumed)
            }
        }

        is LiquidNexusEvent.LifecycleBackgrounded,
        is LiquidNexusEvent.LifecycleRestored -> {
            // Normalize transient/incomplete states into valid stable states
            val normalized = when (state.mode) {
                LiquidNexusMode.Expanding,
                LiquidNexusMode.Scrubbing,
                LiquidNexusMode.ModeSelection -> LiquidNexusMode.Expanded
                LiquidNexusMode.Collapsing -> LiquidNexusMode.Compact
                else -> state.mode
            }
            state.copy(
                mode = normalized,
                scrubFraction = null,
                secondaryMenuOpen = false
            ) to emptyList()
        }
    }
}
