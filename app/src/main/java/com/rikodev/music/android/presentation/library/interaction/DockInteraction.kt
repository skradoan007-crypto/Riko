/*
 * RIKO EXPERIENCE OS — LIBRARY INTERACTION RUNTIME
 * Build B5.4 — Library Interaction Runtime
 *
 * File 8 / 15 — DockInteraction.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Define the interaction vocabulary for the Mini Player Dock —
 * specifically, the *morph* between collapsed (dock) and expanded
 * (Now Playing) states driven by the DockPull gesture.
 *
 * The Dock is the only persistent surface on the Library screen. It
 * has unique interaction semantics:
 *
 *   1. Tap → expand to Now Playing (instant, animated)
 *   2. Pull up → morph to Now Playing (gesture-driven, continuous)
 *   3. Pull down (from Now Playing) → collapse back to Dock
 *   4. Swipe left/right → skip track
 *   5. Long press → context menu (queue, sleep timer, etc.)
 *
 * This file models the *morph state machine* — transitions between
 * COLLAPSED, EXPANDING, EXPANDED, COLLAPSING. The actual animation
 * is driven by the UI layer (Phase B Identity + Phase C Motion),
 * which reads [DockInteractionState] and renders accordingly.
 *
 * MORPH AWARENESS
 * ---------------
 * "Morph awareness" means: while a morph is in progress, the Dock
 * rejects all non-morph gestures. This prevents the user from
 * tapping the Dock mid-pull and causing a confusing state.
 *
 * Other surfaces (cards, hero) also reject gestures during a morph,
 * because the Dock visually overlaps them during the morph animation.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #67  Dock Never Interrupts — Dock gestures do not interrupt
 *      playback or other surfaces' gestures.
 * #78  Interaction Creates Intent, Never Action — Dock interactions
 *      produce Intents, not Actions.
 * #80  Gesture Never Knows UI — DockInteraction does not reference
 *      the Dock composable. It only models state.
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Compose. No Android framework. No Coroutine. No Flow. No
 * ViewModel. No `var`. No side effects. All functions are pure.
 *
 * © RIKO Experience OS — Interaction Runtime
 */

package com.rikodev.music.android.presentation.library.interaction

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 1 — DOCK MORPH STATE
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The lifecycle state of the Dock morph.
 *
 * State transitions:
 *
 *   COLLAPSED ──(pull start)──→ EXPANDING
 *   EXPANDING ──(commit)──→ EXPANDED
 *   EXPANDING ──(cancel)──→ COLLAPSING
 *   EXPANDED ──(pull down)──→ COLLAPSING
 *   COLLAPSING ──(complete)──→ COLLAPSED
 *
 * The morph is *continuous* — the user's finger drives the progress
 * value (0.0 = collapsed, 1.0 = expanded). The state machine labels
 * which "phase" of the morph we're in.
 */
enum class DockMorphState(
    internal val displayName: String,
    internal val isMorphing: Boolean,
    internal val isInteractive: Boolean,
) {
    /** Dock is collapsed — normal Library view. */
    COLLAPSED(
        displayName = "Collapsed",
        isMorphing = false,
        isInteractive = true,
    ),

    /** User is actively pulling the Dock up — morph in progress. */
    EXPANDING(
        displayName = "Expanding",
        isMorphing = true,
        isInteractive = false, // rejecting non-morph gestures
    ),

    /** Dock is fully expanded — Now Playing view. */
    EXPANDED(
        displayName = "Expanded",
        isMorphing = false,
        isInteractive = true,
    ),

    /** Dock is animating back to collapsed (either user-cancelled
     *  pull or user pulling down from EXPANDED). */
    COLLAPSING(
        displayName = "Collapsing",
        isMorphing = true,
        isInteractive = false, // rejecting non-morph gestures
    );

    val isExpandedOrExpanding: Boolean get() = this == EXPANDED || this == EXPANDING
    val isCollapsedOrCollapsing: Boolean get() = this == COLLAPSED || this == COLLAPSING
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 2 — DOCK MORPH SPEC
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Specification of a Dock morph.
 *
 * @property commitThresholdDp   Distance (dp) the user must pull
 *                                before the morph commits. Below this,
 *                                releasing cancels the morph.
 * @property cancelThresholdDp   Distance (dp) below which the morph
 *                                auto-cancels (for "flick down to dismiss").
 * @property maxStretchDp        Maximum distance (dp) the Dock will
 *                                stretch beyond commitThreshold before
 *                                clamping (overscroll effect).
 * @property animationDurationMs Duration (ms) of the commit / cancel
 *                                animation after the user releases.
 */
data class DockMorphSpec(
    val commitThresholdDp: Dp,
    val cancelThresholdDp: Dp,
    val maxStretchDp: Dp,
    val animationDurationMs: Millis,
) {
    init {
        require(commitThresholdDp.value > 0) {
            "commitThresholdDp must be positive"
        }
        require(cancelThresholdDp.value in 0..commitThresholdDp.value) {
            "cancelThresholdDp must be in [0, commitThresholdDp]"
        }
        require(maxStretchDp.value >= 0) {
            "maxStretchDp must be non-negative"
        }
        require(animationDurationMs.value > 0) {
            "animationDurationMs must be positive"
        }
    }

    companion object {
        val DEFAULT = DockMorphSpec(
            commitThresholdDp = Dp(120),
            cancelThresholdDp = Dp(40),
            maxStretchDp = Dp(60),
            animationDurationMs = Millis(280),
        )

        /** Snappier morph — for power users / fast devices. */
        val QUICK = DockMorphSpec(
            commitThresholdDp = Dp(80),
            cancelThresholdDp = Dp(24),
            maxStretchDp = Dp(40),
            animationDurationMs = Millis(180),
        )

        /** Slower morph — for accessibility / reduced motion. */
        val GENTLE = DockMorphSpec(
            commitThresholdDp = Dp(160),
            cancelThresholdDp = Dp(60),
            maxStretchDp = Dp(80),
            animationDurationMs = Millis(420),
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 3 — DOCK INTERACTION STATE
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The full Dock interaction state.
 *
 * @property morphState           Current morph lifecycle state.
 * @property morphProgress        Continuous progress [0..1] from
 *                                 collapsed to expanded. Used by UI
 *                                 to interpolate the morph animation.
 * @property morphSpec            Active morph spec (thresholds).
 * @property activeGestureId      Id of the gesture currently driving
 *                                 the morph. Null when no gesture.
 * @property lastMorphAtMs        Wall-clock of last morph completion.
 * @property swipeEnabled         Whether left/right swipe (skip) is
 *                                 enabled. False during morph.
 * @property longPressEnabled     Whether long press is enabled.
 */
data class DockInteractionState(
    val morphState: DockMorphState,
    val morphProgress: Float,
    val morphSpec: DockMorphSpec,
    val activeGestureId: String?,
    val lastMorphAtMs: Long,
    val swipeEnabled: Boolean,
    val longPressEnabled: Boolean,
) {
    val isMorphing: Boolean get() = morphState.isMorphing
    val isInteractive: Boolean get() = morphState.isInteractive
    val isExpanded: Boolean get() = morphState == DockMorphState.EXPANDED
    val isCollapsed: Boolean get() = morphState == DockMorphState.COLLAPSED

    companion object {
        val IDLE = DockInteractionState(
            morphState = DockMorphState.COLLAPSED,
            morphProgress = 0f,
            morphSpec = DockMorphSpec.DEFAULT,
            activeGestureId = null,
            lastMorphAtMs = 0L,
            swipeEnabled = true,
            longPressEnabled = true,
        )

        fun collapsed(spec: DockMorphSpec = DockMorphSpec.DEFAULT): DockInteractionState =
            IDLE.copy(morphSpec = spec)

        fun expanded(spec: DockMorphSpec = DockMorphSpec.DEFAULT): DockInteractionState =
            DockInteractionState(
                morphState = DockMorphState.EXPANDED,
                morphProgress = 1f,
                morphSpec = spec,
                activeGestureId = null,
                lastMorphAtMs = 0L,
                swipeEnabled = true,
                longPressEnabled = true,
            )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 4 — DOCK INTERACTION RUNTIME (PURE)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The Dock Interaction Runtime — pure functions for every Dock morph
 * transition.
 *
 * Every function takes the current [DockInteractionState] plus a
 * [LibraryGesture.DockPull] (when relevant) and returns a new state.
 */
object DockInteractionRuntime {

    /**
     * Begin a Dock pull morph.
     *
     * Transitions COLLAPSED → EXPANDING. Sets `activeGestureId` and
     * initializes `morphProgress` to a small non-zero value (so the UI
     * can render the morph starting).
     *
     * If the Dock is already morphing, this is a no-op.
     */
    fun beginPull(
        state: DockInteractionState,
        gesture: LibraryGesture.DockPull,
        nowMs: Long,
    ): DockInteractionState {
        if (state.isMorphing) return state

        return state.copy(
            morphState = DockMorphState.EXPANDING,
            morphProgress = gesture.progress,
            activeGestureId = gesture.id,
            swipeEnabled = false,
            longPressEnabled = false,
            lastMorphAtMs = nowMs,
        )
    }

    /**
     * Update an in-progress Dock pull.
     *
     * Updates `morphProgress` based on the gesture's progress. Does
     * not change `morphState` — that's done by [commitPull] or
     * [cancelPull].
     *
     * If the gesture id does not match `activeGestureId`, this is a
     * no-op (a different gesture tried to take over).
     */
    fun updatePull(
        state: DockInteractionState,
        gesture: LibraryGesture.DockPull,
        nowMs: Long,
    ): DockInteractionState {
        if (state.activeGestureId != gesture.id) return state
        if (state.morphState != DockMorphState.EXPANDING) return state

        return state.copy(
            morphProgress = gesture.progress,
            lastMorphAtMs = nowMs,
        )
    }

    /**
     * Commit a Dock pull — transition to EXPANDED.
     *
     * Called when the gesture reports `isCommitted = true`, or when
     * the user releases past the commit threshold.
     */
    fun commitPull(
        state: DockInteractionState,
        gestureId: String,
        nowMs: Long,
    ): DockInteractionState {
        if (state.activeGestureId != gestureId) return state
        if (state.morphState != DockMorphState.EXPANDING) return state

        return state.copy(
            morphState = DockMorphState.EXPANDED,
            morphProgress = 1f,
            activeGestureId = null,
            swipeEnabled = true,
            longPressEnabled = true,
            lastMorphAtMs = nowMs,
        )
    }

    /**
     * Cancel a Dock pull — transition to COLLAPSING (will animate
     * back to COLLAPSED).
     *
     * Called when the user releases before reaching the commit
     * threshold, or when the gesture is cancelled for any reason.
     */
    fun cancelPull(
        state: DockInteractionState,
        gestureId: String,
        reason: DragCancelReason,
        nowMs: Long,
    ): DockInteractionState {
        if (state.activeGestureId != gestureId) return state
        if (state.morphState != DockMorphState.EXPANDING) return state

        return state.copy(
            morphState = DockMorphState.COLLAPSING,
            morphProgress = state.morphProgress, // UI will animate back to 0
            activeGestureId = null,
            swipeEnabled = false,
            longPressEnabled = false,
            lastMorphAtMs = nowMs,
        )
    }

    /**
     * Begin collapsing from EXPANDED (user pulled down from Now Playing).
     *
     * Transitions EXPANDED → COLLAPSING.
     */
    fun beginCollapse(
        state: DockInteractionState,
        gestureId: String,
        nowMs: Long,
    ): DockInteractionState {
        if (state.morphState != DockMorphState.EXPANDED) return state

        return state.copy(
            morphState = DockMorphState.COLLAPSING,
            morphProgress = 1f, // will animate down
            activeGestureId = gestureId,
            swipeEnabled = false,
            longPressEnabled = false,
            lastMorphAtMs = nowMs,
        )
    }

    /**
     * Complete a collapse animation — transition COLLAPSING → COLLAPSED.
     *
     * Called by the UI when the collapse animation finishes.
     */
    fun completeCollapse(
        state: DockInteractionState,
        nowMs: Long,
    ): DockInteractionState {
        if (state.morphState != DockMorphState.COLLAPSING) return state

        return state.copy(
            morphState = DockMorphState.COLLAPSED,
            morphProgress = 0f,
            activeGestureId = null,
            swipeEnabled = true,
            longPressEnabled = true,
            lastMorphAtMs = nowMs,
        )
    }

    /**
     * Complete an expand animation — transition EXPANDING → EXPANDED.
     *
     * Called by the UI when an expand animation (e.g. from a tap)
     * finishes.
     */
    fun completeExpand(
        state: DockInteractionState,
        nowMs: Long,
    ): DockInteractionState {
        if (state.morphState != DockMorphState.EXPANDING) return state

        return state.copy(
            morphState = DockMorphState.EXPANDED,
            morphProgress = 1f,
            activeGestureId = null,
            swipeEnabled = true,
            longPressEnabled = true,
            lastMorphAtMs = nowMs,
        )
    }

    /**
     * Instantly expand the Dock (e.g. from a Tap on the Dock).
     *
     * Bypasses the EXPANDING state — goes directly to EXPANDED. The
     * UI still plays an animation, but the state machine has already
     * reached EXPANDED.
     */
    fun instantExpand(
        state: DockInteractionState,
        nowMs: Long,
    ): DockInteractionState {
        if (state.isMorphing) return state

        return state.copy(
            morphState = DockMorphState.EXPANDED,
            morphProgress = 1f,
            activeGestureId = null,
            swipeEnabled = true,
            longPressEnabled = true,
            lastMorphAtMs = nowMs,
        )
    }

    /**
     * Instantly collapse the Dock (e.g. from a back press in Now Playing).
     */
    fun instantCollapse(
        state: DockInteractionState,
        nowMs: Long,
    ): DockInteractionState {
        if (state.isMorphing) return state

        return state.copy(
            morphState = DockMorphState.COLLAPSED,
            morphProgress = 0f,
            activeGestureId = null,
            swipeEnabled = true,
            longPressEnabled = true,
            lastMorphAtMs = nowMs,
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // QUERY HELPERS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Returns true if the Dock should consume a gesture (it's the
     * active target). False otherwise.
     */
    fun shouldConsumeGesture(
        state: DockInteractionState,
        gestureSource: GestureSource,
    ): Boolean {
        // Dock always consumes gestures on itself
        if (gestureSource == GestureSource.DOCK) return true

        // During morph, Dock consumes ALL gestures (prevents cards behind
        // from responding)
        if (state.isMorphing) return true

        // When expanded, Dock consumes gestures on the whole screen
        // (Now Playing is the active surface)
        if (state.isExpanded) return true

        return false
    }

    /**
     * Returns true if the Dock is in a state where a DockPull gesture
     * can be initiated.
     */
    fun canBeginPull(state: DockInteractionState): Boolean =
        state.morphState == DockMorphState.COLLAPSED

    /**
     * Returns true if the Dock is in a state where a collapse gesture
     * can be initiated (pull-down from Now Playing).
     */
    fun canBeginCollapse(state: DockInteractionState): Boolean =
        state.morphState == DockMorphState.EXPANDED

    /**
     * Compute the progress value to display, accounting for overscroll.
     *
     * If the user has pulled past the commit threshold, the displayed
     * progress follows an easing curve (instead of going to 1.5 or 2.0).
     */
    fun displayedProgress(state: DockInteractionState): Float {
        if (state.morphState == DockMorphState.EXPANDED) return 1f
        if (state.morphState == DockMorphState.COLLAPSED) return 0f

        // During morphing, use raw progress with slight overshoot easing
        val raw = state.morphProgress
        return if (raw > 1f) {
            // Overscroll: ease out beyond 1.0
            1f + (raw - 1f) * 0.3f
        } else {
            raw
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 5 — DOCK INTERACTION INTENT (Rule #78)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Intents emitted by Dock interactions. The Reducer consumes these
 * and updates [DockInteractionState] via [DockInteractionRuntime].
 */
sealed class DockInteractionIntent {
    /** User began pulling the Dock up. */
    data class PullStarted(
        val gestureId: String,
        val sourceGestureId: String?,
    ) : DockInteractionIntent()

    /** User moved the Dock pull (progress updated). */
    data class PullProgressed(
        val gestureId: String,
        val progress: Float,
        val sourceGestureId: String?,
    ) : DockInteractionIntent()

    /** User committed the Dock pull (released past threshold). */
    data class PullCommitted(
        val gestureId: String,
        val sourceGestureId: String?,
    ) : DockInteractionIntent()

    /** User cancelled the Dock pull (released before threshold). */
    data class PullCancelled(
        val gestureId: String,
        val reason: DragCancelReason,
        val sourceGestureId: String?,
    ) : DockInteractionIntent()

    /** User tapped the Dock (instant expand). */
    data object DockTapped : DockInteractionIntent()

    /** User pressed back from Now Playing (instant collapse). */
    data object BackPressed : DockInteractionIntent()

    /** User swiped left/right on the Dock (skip track). */
    data class DockSwiped(
        val direction: GestureDirection,
    ) : DockInteractionIntent()

    /** User long-pressed the Dock (context menu). */
    data object DockLongPressed : DockInteractionIntent()
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 6 — DOCK ACTION (Output)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Actions emitted by the Dock interaction reducer.
 */
sealed class DockInteractionAction {
    /** Expand to Now Playing. */
    data object ExpandToNowPlaying : DockInteractionAction()

    /** Collapse back to Dock. */
    data object CollapseToDock : DockInteractionAction()

    /** Skip to next track. */
    data object SkipToNext : DockInteractionAction()

    /** Skip to previous track. */
    data object SkipToPrevious : DockInteractionAction()

    /** Show Dock context menu. */
    data object ShowDockContextMenu : DockInteractionAction()

    /** Emit a morph-progress haptic. */
    data class MorphHaptic(
        val progress: Float,
    ) : DockInteractionAction()

    /** Emit a commit haptic. */
    data object CommitHaptic : DockInteractionAction()

    /** Emit a cancel haptic. */
    data object CancelHaptic : DockInteractionAction()
}
