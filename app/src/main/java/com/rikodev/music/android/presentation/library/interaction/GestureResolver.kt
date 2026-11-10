/*
 * RIKO EXPERIENCE OS — LIBRARY INTERACTION RUNTIME
 * Build B5.4 — Library Interaction Runtime
 *
 * File 3 / 15 — GestureResolver.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Translate a recognized [LibraryGesture] into a structured
 * [InteractionIntent] that the Reducer can consume.
 *
 * The Resolver is the bridge between *physical motion* (a Tap on
 * coordinates 240,512) and *user intent* ("the user wants to open
 * collection X").
 *
 * ARCHITECTURE
 * ------------
 *   LibraryGesture (data)
 *        +
 *   GestureResolutionContext (target + selection mode + ...)
 *        ↓
 *   GestureResolver.resolve() — PURE
 *        ↓
 *   GestureResolution
 *        ├── outcome: ACCEPTED / IGNORED / DEFERRED
 *        ├── intent:  InteractionIntent? (null if no intent produced)
 *        └── explanation: GestureExplanation (Rule #82)
 *
 * The Reducer receives the Intent and decides what Action (if any)
 * to emit. The Resolver never emits Actions (Rule #78).
 *
 * CONFLICT RESOLUTION
 * -------------------
 * The Resolver is also responsible for resolving *internal* conflicts
 * where a single physical gesture could be interpreted multiple ways:
 *
 *   Example: Long Press on a card while a Scroll is in-flight.
 *   - If movement > maxMovementDp → Long Press is CANCELLED,
 *     Scroll wins. Intent = ScrollIntent.
 *   - If duration > minDurationMs AND movement <= maxMovementDp →
 *     Long Press is ACCEPTED. Intent = ContextMenuRequestedIntent.
 *
 * External conflicts (multiple gestures from different sources) are
 * handled by [GesturePriority] / [resolveConflict] — the Resolver
 * consumes the winner and produces an Intent from it.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #78  Interaction Creates Intent, Never Action — Resolver emits
 *      Intent only, never Action.
 * #80  Gesture Never Knows UI — Resolver receives target info via
 *      `GestureResolutionContext`, not by reaching into the Composable
 *      tree.
 * #82  Every Gesture Has Explanation — every `GestureResolution`
 *      carries a `GestureExplanation`.
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
// SECTION 1 — INTERACTION INTENT (Rule #78)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * An Intent is the *user's will*, translated from a physical gesture.
 *
 * Intents are NOT Actions. The Reducer consumes Intents and decides:
 *   - Should an Action be emitted? (e.g. PlaySong)
 *   - Should state be mutated? (e.g. enter selection mode)
 *   - Should the Intent be ignored? (e.g. tap during drag)
 *
 * This indirection exists because the same gesture can mean different
 * things depending on state. A Tap on a song:
 *   - In normal mode → PlaySong
 *   - In selection mode → Toggle selection
 *   - During a drag → Ignored (let drag finish)
 *
 * The Reducer has full state visibility to make this decision; the
 * Resolver does not (Rule #80).
 *
 * Every Intent carries:
 *   - `sourceGestureId` — links back to the gesture that produced it
 *     (Rule #81 replay).
 *   - `source` — the screen layer (GestureSource).
 *   - `timestampMs` — wall-clock time of resolution.
 */
sealed class InteractionIntent {

    abstract val sourceGestureId: String
    abstract val source: GestureSource
    abstract val timestampMs: Long

    // ── Tap intents ───────────────────────────────────────────────────

    /** User tapped on a collection card. */
    data class CardTapped(
        override val sourceGestureId: String,
        override val source: GestureSource,
        override val timestampMs: Long,
        val targetId: String,
        val targetType: GestureTargetType,
        val targetItemId: Long,
    ) : InteractionIntent()

    /** User tapped on a song (distinct from card — may have different behavior). */
    data class SongTapped(
        override val sourceGestureId: String,
        override val source: GestureSource,
        override val timestampMs: Long,
        val songId: Long,
        val contextPlaylistId: Long?,
    ) : InteractionIntent()

    /** User tapped on the Hero section. */
    data class HeroTapped(
        override val sourceGestureId: String,
        override val source: GestureSource,
        override val timestampMs: Long,
    ) : InteractionIntent()

    /** User tapped on the Dock. */
    data class DockTapped(
        override val sourceGestureId: String,
        override val source: GestureSource,
        override val timestampMs: Long,
    ) : InteractionIntent()

    // ── Long-press / context intents ──────────────────────────────────

    /** User long-pressed an item — typically opens context menu. */
    data class ContextMenuRequested(
        override val sourceGestureId: String,
        override val source: GestureSource,
        override val timestampMs: Long,
        val targetId: String,
        val targetType: GestureTargetType,
        val targetItemId: Long,
        val trigger: ContextTrigger,
    ) : InteractionIntent()

    // ── Double-tap intents ────────────────────────────────────────────

    /** User double-tapped — typically toggles favorite. */
    data class DoubleTapped(
        override val sourceGestureId: String,
        override val source: GestureSource,
        override val timestampMs: Long,
        val targetId: String,
        val targetType: GestureTargetType,
        val targetItemId: Long,
    ) : InteractionIntent()

    // ── Swipe / fling intents ─────────────────────────────────────────

    /** User swiped (discrete, directional). */
    data class Swiped(
        override val sourceGestureId: String,
        override val source: GestureSource,
        override val timestampMs: Long,
        val direction: GestureDirection,
        val distanceDp: Dp,
        val velocityDpPerSec: DpPerSec,
        val targetId: String?,
    ) : InteractionIntent()

    /** User flung (fast release with velocity). */
    data class Flung(
        override val sourceGestureId: String,
        override val source: GestureSource,
        override val timestampMs: Long,
        val direction: GestureDirection,
        val velocity: GestureVector,
        val targetId: String?,
    ) : InteractionIntent()

    // ── Edge swipe intents ────────────────────────────────────────────

    /** User swiped from a screen edge — typically system back. */
    data class EdgeSwiped(
        override val sourceGestureId: String,
        override val source: GestureSource,
        override val timestampMs: Long,
        val edge: ScreenEdge,
        val direction: GestureDirection,
        val distanceDp: Dp,
    ) : InteractionIntent()

    // ── Dock pull intents ─────────────────────────────────────────────

    /** User is pulling the Dock up — morph in progress. */
    data class DockPullProgressed(
        override val sourceGestureId: String,
        override val source: GestureSource,
        override val timestampMs: Long,
        val progress: Float,
    ) : InteractionIntent()

    /** User pulled the Dock past the commit threshold — expand to Now Playing. */
    data class DockPullCommitted(
        override val sourceGestureId: String,
        override val source: GestureSource,
        override val timestampMs: Long,
    ) : InteractionIntent()

    /** User cancelled the Dock pull (released before commit). */
    data class DockPullCancelled(
        override val sourceGestureId: String,
        override val source: GestureSource,
        override val timestampMs: Long,
        val reason: DragCancelReason,
    ) : InteractionIntent()

    // ── Drag intents ──────────────────────────────────────────────────

    /** A drag has started — Reducer may enter reorder / swipe-to-dismiss mode. */
    data class DragStarted(
        override val sourceGestureId: String,
        override val source: GestureSource,
        override val timestampMs: Long,
        val targetId: String,
        val startPosition: GesturePoint,
        val isReorder: Boolean,
        val isSwipeToDismiss: Boolean,
    ) : InteractionIntent()

    /** A drag has moved — Reducer updates drag preview state. */
    data class DragMoved(
        override val sourceGestureId: String,
        override val source: GestureSource,
        override val timestampMs: Long,
        val targetId: String,
        val currentPosition: GesturePoint,
        val totalDelta: GestureVector,
        val lastDelta: GestureVector,
    ) : InteractionIntent()

    /** A drag has ended — Reducer commits the reorder / dismiss. */
    data class DragEnded(
        override val sourceGestureId: String,
        override val source: GestureSource,
        override val timestampMs: Long,
        val targetId: String,
        val endPosition: GesturePoint,
        val velocity: GestureVector,
    ) : InteractionIntent()

    /** A drag was cancelled — Reducer rolls back preview state. */
    data class DragCancelled(
        override val sourceGestureId: String,
        override val source: GestureSource,
        override val timestampMs: Long,
        val targetId: String,
        val reason: DragCancelReason,
    ) : InteractionIntent()

    // ── Scroll intents ────────────────────────────────────────────────

    /** User scrolled within a container — Reducer updates scroll state. */
    data class Scrolled(
        override val sourceGestureId: String,
        override val source: GestureSource,
        override val timestampMs: Long,
        val delta: GestureVector,
        val direction: GestureDirection,
        val isFastScroll: Boolean,
    ) : InteractionIntent()

    // ── Pinch / multi-touch intents ───────────────────────────────────

    /** User pinched — Reducer updates zoom / grid density. */
    data class Pinched(
        override val sourceGestureId: String,
        override val source: GestureSource,
        override val timestampMs: Long,
        val scale: Scale,
        val focalPoint: GesturePoint,
    ) : InteractionIntent()

    /** User performed a multi-touch gesture (3+ pointers). */
    data class MultiTouched(
        override val sourceGestureId: String,
        override val source: GestureSource,
        override val timestampMs: Long,
        val pointerCount: Int,
        val centroid: GesturePoint,
        val rotation: Degrees,
    ) : InteractionIntent()

    // ── Press / hover intents ─────────────────────────────────────────

    /** User pressed (touch-down) — Reducer may show press-state visual. */
    data class Pressed(
        override val sourceGestureId: String,
        override val source: GestureSource,
        override val timestampMs: Long,
        val targetId: String,
        val position: GesturePoint,
        val pressure: Float,
    ) : InteractionIntent()

    /** User hovered (mouse / stylus) — Reducer updates hover state. */
    data class Hovered(
        override val sourceGestureId: String,
        override val source: GestureSource,
        override val timestampMs: Long,
        val targetId: String?,
        val entered: Boolean,
        val exited: Boolean,
        val position: GesturePoint,
    ) : InteractionIntent()
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 2 — TARGET TYPE
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Type of the element a gesture landed on. Used by Intents to tell
 * the Reducer what kind of item is being interacted with.
 *
 * Rule #80: this is a *semantic label*, not a UI reference. The
 * Resolver does not know what a `Card` composable is — it only knows
 * "the gesture landed on something the screen calls a CARD".
 */
enum class GestureTargetType {
    SONG,
    ALBUM,
    ARTIST,
    FOLDER,
    PLAYLIST,
    COLLECTION,
    HERO,
    DOCK,
    SEARCH_BAR,
    FILTER_CHIP,
    SORT_CHIP,
    SECTION_HEADER,
    EMPTY_STATE,
    BACKGROUND,
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 3 — RESOLUTION CONTEXT
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Minimal context the Resolver needs to translate a gesture into an
 * Intent. This is *not* UI state — it is a small, serializable
 * snapshot of the screen regions that received the gesture.
 *
 * Rule #80: the Resolver receives target info via this context, not
 * by reaching into the Composable tree.
 *
 * @property targetId          Stable id of the element under the
 *                              gesture. Null if gesture missed all
 *                              targets (e.g. background tap).
 * @property targetType        Semantic type of the target.
 * @property targetItemId      Domain id of the underlying item
 *                              (Song.id, Album.id, etc.). Null if the
 *                              target is not a domain item.
 * @property contextPlaylistId  For Song targets: the playlist context
 *                              the song lives in (for "play next"
 *                              semantics). Null if standalone.
 * @property selectionModeActive  True if the Library is currently in
 *                              multi-selection mode. Affects Tap
 *                              interpretation (toggle vs. open).
 * @property dockMorphInProgress  True if the Dock is currently
 *                              morphing (during DockPull). Affects
 *                              whether other gestures are deferred.
 * @property dragInProgress    True if a drag is currently in flight.
 *                              Affects whether Tap is ignored.
 */
data class GestureResolutionContext(
    val targetId: String?,
    val targetType: GestureTargetType?,
    val targetItemId: Long?,
    val contextPlaylistId: Long?,
    val selectionModeActive: Boolean,
    val dockMorphInProgress: Boolean,
    val dragInProgress: Boolean,
) {
    companion object {
        /** A gesture with no resolved target — typically a background tap. */
        val NO_TARGET = GestureResolutionContext(
            targetId = null,
            targetType = null,
            targetItemId = null,
            contextPlaylistId = null,
            selectionModeActive = false,
            dockMorphInProgress = false,
            dragInProgress = false,
        )

        /**
         * Build a context for a gesture that hit a specific target.
         */
        fun forTarget(
            targetId: String,
            targetType: GestureTargetType,
            targetItemId: Long? = null,
            contextPlaylistId: Long? = null,
            selectionModeActive: Boolean = false,
            dockMorphInProgress: Boolean = false,
            dragInProgress: Boolean = false,
        ): GestureResolutionContext = GestureResolutionContext(
            targetId = targetId,
            targetType = targetType,
            targetItemId = targetItemId,
            contextPlaylistId = contextPlaylistId,
            selectionModeActive = selectionModeActive,
            dockMorphInProgress = dockMorphInProgress,
            dragInProgress = dragInProgress,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 4 — RESOLUTION OUTCOME + EXPLANATION (Rule #82)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Whether a gesture was accepted, ignored, or deferred.
 *
 * - ACCEPTED: gesture produced an Intent that the Reducer should
 *   process.
 * - IGNORED: gesture is meaningful but should not produce an Intent
 *   in the current context (e.g. Tap during a drag). The Reducer
 *   will not be called.
 * - DEFERRED: gesture cannot be resolved yet — needs more samples
 *   (e.g. a Tap that might become a DoubleTap). Caller should hold
 *   state and re-resolve when more data arrives.
 * - REJECTED: gesture failed validation (e.g. movement exceeded slop
 *   for a Tap). The Reducer will not be called.
 */
enum class GestureOutcome {
    ACCEPTED,
    IGNORED,
    DEFERRED,
    REJECTED,
}

/**
 * Structured explanation of how a gesture was resolved (Rule #82).
 *
 * Designed for the Inspector: every field is human-readable and
 * machine-queryable, so the Inspector can render a structured
 * explanation block.
 */
data class GestureExplanation(
    val accepted: Boolean,
    val outcome: GestureOutcome,
    val winnerKind: LibraryGestureKind?,
    val loserKinds: List<LibraryGestureKind>,
    val metrics: GestureMetrics,
    val reason: String,
    val intentProduced: String?,
    val contextSnapshot: String,
) {
    companion object {
        fun accepted(
            winner: LibraryGesture,
            intent: InteractionIntent?,
            reason: String,
            contextSnapshot: String,
        ): GestureExplanation = GestureExplanation(
            accepted = true,
            outcome = GestureOutcome.ACCEPTED,
            winnerKind = winner.kind,
            loserKinds = emptyList(),
            metrics = winner.metrics,
            reason = reason,
            intentProduced = intent?.let { it::class.simpleName },
            contextSnapshot = contextSnapshot,
        )

        fun ignored(
            gesture: LibraryGesture,
            reason: String,
            contextSnapshot: String,
        ): GestureExplanation = GestureExplanation(
            accepted = false,
            outcome = GestureOutcome.IGNORED,
            winnerKind = gesture.kind,
            loserKinds = emptyList(),
            metrics = gesture.metrics,
            reason = reason,
            intentProduced = null,
            contextSnapshot = contextSnapshot,
        )

        fun deferred(
            gesture: LibraryGesture,
            reason: String,
            contextSnapshot: String,
        ): GestureExplanation = GestureExplanation(
            accepted = false,
            outcome = GestureOutcome.DEFERRED,
            winnerKind = gesture.kind,
            loserKinds = emptyList(),
            metrics = gesture.metrics,
            reason = reason,
            intentProduced = null,
            contextSnapshot = contextSnapshot,
        )

        fun rejected(
            gesture: LibraryGesture,
            reason: String,
            contextSnapshot: String,
        ): GestureExplanation = GestureExplanation(
            accepted = false,
            outcome = GestureOutcome.REJECTED,
            winnerKind = gesture.kind,
            loserKinds = emptyList(),
            metrics = gesture.metrics,
            reason = reason,
            intentProduced = null,
            contextSnapshot = contextSnapshot,
        )
    }
}

/**
 * The full result of resolving a gesture.
 */
data class GestureResolution(
    val gesture: LibraryGesture,
    val outcome: GestureOutcome,
    val intent: InteractionIntent?,
    val explanation: GestureExplanation,
) {
    val isAccepted: Boolean get() = outcome == GestureOutcome.ACCEPTED
    val isIgnored: Boolean get() = outcome == GestureOutcome.IGNORED
    val isDeferred: Boolean get() = outcome == GestureOutcome.DEFERRED
    val isRejected: Boolean get() = outcome == GestureOutcome.REJECTED

    companion object {
        fun accepted(
            gesture: LibraryGesture,
            intent: InteractionIntent,
            reason: String,
            contextSnapshot: String,
        ): GestureResolution = GestureResolution(
            gesture = gesture,
            outcome = GestureOutcome.ACCEPTED,
            intent = intent,
            explanation = GestureExplanation.accepted(
                winner = gesture,
                intent = intent,
                reason = reason,
                contextSnapshot = contextSnapshot,
            ),
        )

        fun ignored(
            gesture: LibraryGesture,
            reason: String,
            contextSnapshot: String,
        ): GestureResolution = GestureResolution(
            gesture = gesture,
            outcome = GestureOutcome.IGNORED,
            intent = null,
            explanation = GestureExplanation.ignored(
                gesture = gesture,
                reason = reason,
                contextSnapshot = contextSnapshot,
            ),
        )

        fun deferred(
            gesture: LibraryGesture,
            reason: String,
            contextSnapshot: String,
        ): GestureResolution = GestureResolution(
            gesture = gesture,
            outcome = GestureOutcome.DEFERRED,
            intent = null,
            explanation = GestureExplanation.deferred(
                gesture = gesture,
                reason = reason,
                contextSnapshot = contextSnapshot,
            ),
        )

        fun rejected(
            gesture: LibraryGesture,
            reason: String,
            contextSnapshot: String,
        ): GestureResolution = GestureResolution(
            gesture = gesture,
            outcome = GestureOutcome.REJECTED,
            intent = null,
            explanation = GestureExplanation.rejected(
                gesture = gesture,
                reason = reason,
                contextSnapshot = contextSnapshot,
            ),
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 5 — THE RESOLVER (PURE ENTRY POINT)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Resolve a [gesture] against the current [context] and produce a
 * [GestureResolution].
 *
 * This is the **single entry point** to the Resolver. All per-gesture
 * logic is private and dispatched from here.
 *
 * Purity: this function has no side effects. Given the same inputs,
 * it produces the same output, byte-identical.
 */
fun resolveGesture(
    gesture: LibraryGesture,
    context: GestureResolutionContext,
): GestureResolution {
    val contextSnapshot = "target=${context.targetId ?: "none"}, " +
            "type=${context.targetType ?: "none"}, " +
            "selection=${context.selectionModeActive}, " +
            "drag=${context.dragInProgress}, " +
            "dockMorph=${context.dockMorphInProgress}"

    return when (gesture) {
        is LibraryGesture.Tap          -> resolveTap(gesture, context, contextSnapshot)
        is LibraryGesture.DoubleTap    -> resolveDoubleTap(gesture, context, contextSnapshot)
        is LibraryGesture.LongPress    -> resolveLongPress(gesture, context, contextSnapshot)
        is LibraryGesture.Press        -> resolvePress(gesture, context, contextSnapshot)
        is LibraryGesture.Hover        -> resolveHover(gesture, context, contextSnapshot)
        is LibraryGesture.Drag         -> resolveDrag(gesture, context, contextSnapshot)
        is LibraryGesture.DragCancel   -> resolveDragCancel(gesture, context, contextSnapshot)
        is LibraryGesture.Fling        -> resolveFling(gesture, context, contextSnapshot)
        is LibraryGesture.Swipe        -> resolveSwipe(gesture, context, contextSnapshot)
        is LibraryGesture.Scroll       -> resolveScroll(gesture, context, contextSnapshot)
        is LibraryGesture.Pinch        -> resolvePinch(gesture, context, contextSnapshot)
        is LibraryGesture.MultiTouch   -> resolveMultiTouch(gesture, context, contextSnapshot)
        is LibraryGesture.EdgeSwipe    -> resolveEdgeSwipe(gesture, context, contextSnapshot)
        is LibraryGesture.Context      -> resolveContext(gesture, context, contextSnapshot)
        is LibraryGesture.DockPull     -> resolveDockPull(gesture, context, contextSnapshot)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 6 — PER-GESTURE RESOLVERS (PRIVATE)
// ═══════════════════════════════════════════════════════════════════════════

private fun resolveTap(
    gesture: LibraryGesture.Tap,
    context: GestureResolutionContext,
    contextSnapshot: String,
): GestureResolution {
    // Tap during drag → ignored (let drag finish)
    if (context.dragInProgress) {
        return GestureResolution.ignored(
            gesture = gesture,
            reason = "Tap ignored: a drag is in progress.",
            contextSnapshot = contextSnapshot,
        )
    }

    // Tap during Dock morph → ignored (Dock owns interaction)
    if (context.dockMorphInProgress) {
        return GestureResolution.ignored(
            gesture = gesture,
            reason = "Tap ignored: Dock morph in progress.",
            contextSnapshot = contextSnapshot,
        )
    }

    val target = context.targetType
    val targetId = context.targetId
    val targetItemId = context.targetItemId

    // No target → background tap → ignored
    if (target == null || targetId == null) {
        return GestureResolution.ignored(
            gesture = gesture,
            reason = "Tap on background — no target.",
            contextSnapshot = contextSnapshot,
        )
    }

    val intent: InteractionIntent = when (target) {
        GestureTargetType.SONG -> InteractionIntent.SongTapped(
            sourceGestureId = gesture.id,
            source = gesture.source,
            timestampMs = gesture.timestampMs,
            songId = targetItemId ?: -1L,
            contextPlaylistId = context.contextPlaylistId,
        )

        GestureTargetType.HERO -> InteractionIntent.HeroTapped(
            sourceGestureId = gesture.id,
            source = gesture.source,
            timestampMs = gesture.timestampMs,
        )

        GestureTargetType.DOCK -> InteractionIntent.DockTapped(
            sourceGestureId = gesture.id,
            source = gesture.source,
            timestampMs = gesture.timestampMs,
        )

        else -> InteractionIntent.CardTapped(
            sourceGestureId = gesture.id,
            source = gesture.source,
            timestampMs = gesture.timestampMs,
            targetId = targetId,
            targetType = target,
            targetItemId = targetItemId ?: -1L,
        )
    }

    return GestureResolution.accepted(
        gesture = gesture,
        intent = intent,
        reason = "Tap accepted: target=${target.displayName()}, movement=${gesture.movementDp.value}dp, duration=${gesture.duration.value}ms",
        contextSnapshot = contextSnapshot,
    )
}

private fun resolveDoubleTap(
    gesture: LibraryGesture.DoubleTap,
    context: GestureResolutionContext,
    contextSnapshot: String,
): GestureResolution {
    if (context.dragInProgress) {
        return GestureResolution.ignored(
            gesture = gesture,
            reason = "DoubleTap ignored: drag in progress.",
            contextSnapshot = contextSnapshot,
        )
    }

    val target = context.targetType
    val targetId = context.targetId
    val targetItemId = context.targetItemId

    if (target == null || targetId == null) {
        return GestureResolution.ignored(
            gesture = gesture,
            reason = "DoubleTap on background.",
            contextSnapshot = contextSnapshot,
        )
    }

    val intent = InteractionIntent.DoubleTapped(
        sourceGestureId = gesture.id,
        source = gesture.source,
        timestampMs = gesture.timestampMs,
        targetId = targetId,
        targetType = target,
        targetItemId = targetItemId ?: -1L,
    )

    return GestureResolution.accepted(
        gesture = gesture,
        intent = intent,
        reason = "DoubleTap accepted: gap=${gesture.gapMs.value}ms",
        contextSnapshot = contextSnapshot,
    )
}

private fun resolveLongPress(
    gesture: LibraryGesture.LongPress,
    context: GestureResolutionContext,
    contextSnapshot: String,
): GestureResolution {
    if (context.dragInProgress) {
        return GestureResolution.ignored(
            gesture = gesture,
            reason = "LongPress ignored: drag in progress.",
            contextSnapshot = contextSnapshot,
        )
    }

    val target = context.targetType
    val targetId = context.targetId
    val targetItemId = context.targetItemId

    if (target == null || targetId == null) {
        return GestureResolution.ignored(
            gesture = gesture,
            reason = "LongPress on background — no context menu.",
            contextSnapshot = contextSnapshot,
        )
    }

    val intent = InteractionIntent.ContextMenuRequested(
        sourceGestureId = gesture.id,
        source = gesture.source,
        timestampMs = gesture.timestampMs,
        targetId = targetId,
        targetType = target,
        targetItemId = targetItemId ?: -1L,
        trigger = ContextTrigger.LONG_PRESS,
    )

    return GestureResolution.accepted(
        gesture = gesture,
        intent = intent,
        reason = "LongPress accepted: duration=${gesture.duration.value}ms, movement=${gesture.movementDp.value}dp, haptic=${gesture.hapticFired}",
        contextSnapshot = contextSnapshot,
    )
}

private fun resolvePress(
    gesture: LibraryGesture.Press,
    context: GestureResolutionContext,
    contextSnapshot: String,
): GestureResolution {
    if (context.dragInProgress) {
        return GestureResolution.ignored(
            gesture = gesture,
            reason = "Press ignored: drag in progress.",
            contextSnapshot = contextSnapshot,
        )
    }

    val targetId = context.targetId ?: return GestureResolution.ignored(
        gesture = gesture,
        reason = "Press on background.",
        contextSnapshot = contextSnapshot,
    )

    val intent = InteractionIntent.Pressed(
        sourceGestureId = gesture.id,
        source = gesture.source,
        timestampMs = gesture.timestampMs,
        targetId = targetId,
        position = gesture.position,
        pressure = gesture.pressure,
    )

    return GestureResolution.accepted(
        gesture = gesture,
        intent = intent,
        reason = "Press accepted: pressure=${gesture.pressure}, target=$targetId",
        contextSnapshot = contextSnapshot,
    )
}

private fun resolveHover(
    gesture: LibraryGesture.Hover,
    context: GestureResolutionContext,
    contextSnapshot: String,
): GestureResolution {
    val intent = InteractionIntent.Hovered(
        sourceGestureId = gesture.id,
        source = gesture.source,
        timestampMs = gesture.timestampMs,
        targetId = context.targetId,
        entered = gesture.entered,
        exited = gesture.exited,
        position = gesture.position,
    )

    return GestureResolution.accepted(
        gesture = gesture,
        intent = intent,
        reason = "Hover accepted: entered=${gesture.entered}, exited=${gesture.exited}",
        contextSnapshot = contextSnapshot,
    )
}

private fun resolveDrag(
    gesture: LibraryGesture.Drag,
    context: GestureResolutionContext,
    contextSnapshot: String,
): GestureResolution {
    val targetId = context.targetId ?: return GestureResolution.ignored(
        gesture = gesture,
        reason = "Drag without target — likely background pan.",
        contextSnapshot = contextSnapshot,
    )

    val intent = if (context.dragInProgress) {
        // Drag is in flight → emit DragMoved
        InteractionIntent.DragMoved(
            sourceGestureId = gesture.id,
            source = gesture.source,
            timestampMs = gesture.timestampMs,
            targetId = targetId,
            currentPosition = gesture.currentPosition,
            totalDelta = gesture.totalDelta,
            lastDelta = gesture.lastDelta,
        )
    } else {
        // Drag just started → emit DragStarted
        InteractionIntent.DragStarted(
            sourceGestureId = gesture.id,
            source = gesture.source,
            timestampMs = gesture.timestampMs,
            targetId = targetId,
            startPosition = gesture.startPosition,
            isReorder = gesture.isReorder,
            isSwipeToDismiss = gesture.isSwipeToDismiss,
        )
    }

    return GestureResolution.accepted(
        gesture = gesture,
        intent = intent,
        reason = "Drag accepted: totalDelta=${gesture.totalDelta.magnitude.value}px, direction=${gesture.direction}, reorder=${gesture.isReorder}",
        contextSnapshot = contextSnapshot,
    )
}

private fun resolveDragCancel(
    gesture: LibraryGesture.DragCancel,
    context: GestureResolutionContext,
    contextSnapshot: String,
): GestureResolution {
    val intent = InteractionIntent.DragCancelled(
        sourceGestureId = gesture.id,
        source = gesture.source,
        timestampMs = gesture.timestampMs,
        targetId = context.targetId ?: gesture.originalDragId,
        reason = gesture.reason,
    )

    return GestureResolution.accepted(
        gesture = gesture,
        intent = intent,
        reason = "DragCancel accepted: reason=${gesture.reason}",
        contextSnapshot = contextSnapshot,
    )
}

private fun resolveFling(
    gesture: LibraryGesture.Fling,
    context: GestureResolutionContext,
    contextSnapshot: String,
): GestureResolution {
    val intent = InteractionIntent.Flung(
        sourceGestureId = gesture.id,
        source = gesture.source,
        timestampMs = gesture.timestampMs,
        direction = gesture.direction,
        velocity = gesture.velocity,
        targetId = context.targetId,
    )

    return GestureResolution.accepted(
        gesture = gesture,
        intent = intent,
        reason = "Fling accepted: direction=${gesture.direction}, velocity=${gesture.velocity.magnitude.value}px/s",
        contextSnapshot = contextSnapshot,
    )
}

private fun resolveSwipe(
    gesture: LibraryGesture.Swipe,
    context: GestureResolutionContext,
    contextSnapshot: String,
): GestureResolution {
    val intent = InteractionIntent.Swiped(
        sourceGestureId = gesture.id,
        source = gesture.source,
        timestampMs = gesture.timestampMs,
        direction = gesture.direction,
        distanceDp = gesture.distanceDp,
        velocityDpPerSec = gesture.velocityDpPerSec,
        targetId = context.targetId,
    )

    return GestureResolution.accepted(
        gesture = gesture,
        intent = intent,
        reason = "Swipe accepted: direction=${gesture.direction}, distance=${gesture.distanceDp.value}dp, velocity=${gesture.velocityDpPerSec.value}dp/s",
        contextSnapshot = contextSnapshot,
    )
}

private fun resolveScroll(
    gesture: LibraryGesture.Scroll,
    context: GestureResolutionContext,
    contextSnapshot: String,
): GestureResolution {
    val intent = InteractionIntent.Scrolled(
        sourceGestureId = gesture.id,
        source = gesture.source,
        timestampMs = gesture.timestampMs,
        delta = gesture.delta,
        direction = gesture.direction,
        isFastScroll = gesture.isFastScroll,
    )

    return GestureResolution.accepted(
        gesture = gesture,
        intent = intent,
        reason = "Scroll accepted: delta=${gesture.delta.magnitude.value}px, direction=${gesture.direction}, fast=${gesture.isFastScroll}",
        contextSnapshot = contextSnapshot,
    )
}

private fun resolvePinch(
    gesture: LibraryGesture.Pinch,
    context: GestureResolutionContext,
    contextSnapshot: String,
): GestureResolution {
    val intent = InteractionIntent.Pinched(
        sourceGestureId = gesture.id,
        source = gesture.source,
        timestampMs = gesture.timestampMs,
        scale = gesture.scale,
        focalPoint = gesture.focalPoint,
    )

    return GestureResolution.accepted(
        gesture = gesture,
        intent = intent,
        reason = "Pinch accepted: scale=${gesture.scale.value}, rotation=${gesture.rotation.value}deg",
        contextSnapshot = contextSnapshot,
    )
}

private fun resolveMultiTouch(
    gesture: LibraryGesture.MultiTouch,
    context: GestureResolutionContext,
    contextSnapshot: String,
): GestureResolution {
    val intent = InteractionIntent.MultiTouched(
        sourceGestureId = gesture.id,
        source = gesture.source,
        timestampMs = gesture.timestampMs,
        pointerCount = gesture.pointers.size,
        centroid = gesture.centroid,
        rotation = gesture.rotation,
    )

    return GestureResolution.accepted(
        gesture = gesture,
        intent = intent,
        reason = "MultiTouch accepted: pointers=${gesture.pointers.size}, rotation=${gesture.rotation.value}deg",
        contextSnapshot = contextSnapshot,
    )
}

private fun resolveEdgeSwipe(
    gesture: LibraryGesture.EdgeSwipe,
    context: GestureResolutionContext,
    contextSnapshot: String,
): GestureResolution {
    val intent = InteractionIntent.EdgeSwiped(
        sourceGestureId = gesture.id,
        source = gesture.source,
        timestampMs = gesture.timestampMs,
        edge = gesture.edge,
        direction = gesture.direction,
        distanceDp = gesture.distanceDp,
    )

    return GestureResolution.accepted(
        gesture = gesture,
        intent = intent,
        reason = "EdgeSwipe accepted: edge=${gesture.edge}, direction=${gesture.direction}, distance=${gesture.distanceDp.value}dp",
        contextSnapshot = contextSnapshot,
    )
}

private fun resolveContext(
    gesture: LibraryGesture.Context,
    context: GestureResolutionContext,
    contextSnapshot: String,
): GestureResolution {
    val target = context.targetType
    val targetId = context.targetId
    val targetItemId = context.targetItemId

    if (target == null || targetId == null) {
        return GestureResolution.ignored(
            gesture = gesture,
            reason = "Context trigger on background — no menu.",
            contextSnapshot = contextSnapshot,
        )
    }

    val intent = InteractionIntent.ContextMenuRequested(
        sourceGestureId = gesture.id,
        source = gesture.source,
        timestampMs = gesture.timestampMs,
        targetId = targetId,
        targetType = target,
        targetItemId = targetItemId ?: -1L,
        trigger = gesture.trigger,
    )

    return GestureResolution.accepted(
        gesture = gesture,
        intent = intent,
        reason = "Context accepted: trigger=${gesture.trigger}, target=${target.displayName()}",
        contextSnapshot = contextSnapshot,
    )
}

private fun resolveDockPull(
    gesture: LibraryGesture.DockPull,
    context: GestureResolutionContext,
    contextSnapshot: String,
): GestureResolution {
    val intent = when {
        gesture.isCommitted -> InteractionIntent.DockPullCommitted(
            sourceGestureId = gesture.id,
            source = gesture.source,
            timestampMs = gesture.timestampMs,
        )
        gesture.progress >= 0.05f -> InteractionIntent.DockPullProgressed(
            sourceGestureId = gesture.id,
            source = gesture.source,
            timestampMs = gesture.timestampMs,
            progress = gesture.progress,
        )
        else -> return GestureResolution.deferred(
            gesture = gesture,
            reason = "DockPull below commit threshold (progress=${gesture.progress}).",
            contextSnapshot = contextSnapshot,
        )
    }

    return GestureResolution.accepted(
        gesture = gesture,
        intent = intent,
        reason = "DockPull accepted: progress=${gesture.progress}, committed=${gesture.isCommitted}",
        contextSnapshot = contextSnapshot,
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 7 — HELPER EXTENSIONS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Human-readable name for a [GestureTargetType].
 */
private fun GestureTargetType.displayName(): String = when (this) {
    GestureTargetType.SONG            -> "Song"
    GestureTargetType.ALBUM           -> "Album"
    GestureTargetType.ARTIST          -> "Artist"
    GestureTargetType.FOLDER          -> "Folder"
    GestureTargetType.PLAYLIST        -> "Playlist"
    GestureTargetType.COLLECTION      -> "Collection"
    GestureTargetType.HERO            -> "Hero"
    GestureTargetType.DOCK            -> "Dock"
    GestureTargetType.SEARCH_BAR      -> "SearchBar"
    GestureTargetType.FILTER_CHIP     -> "FilterChip"
    GestureTargetType.SORT_CHIP       -> "SortChip"
    GestureTargetType.SECTION_HEADER  -> "SectionHeader"
    GestureTargetType.EMPTY_STATE     -> "EmptyState"
    GestureTargetType.BACKGROUND      -> "Background"
}

/**
 * Convenience: resolve multiple gestures in parallel (e.g. when
 * multiple pointers produce simultaneous gestures).
 *
 * Returns the resolution of the *winning* gesture (per
 * [resolveConflict]) plus the resolutions of all losers (for
 * Inspector diagnostics).
 */
data class MultiGestureResolution(
    val winner: GestureResolution,
    val losers: List<GestureResolution>,
)

/**
 * Resolve a set of competing gestures by first running conflict
 * resolution, then resolving the winner against the context.
 */
fun resolveCompetingGestures(
    gestures: List<LibraryGesture>,
    context: GestureResolutionContext,
): MultiGestureResolution? {
    val outcome = resolveConflict(gestures) ?: return null

    return when (outcome) {
        is ConflictOutcome.Winner -> {
            val winnerResolution = resolveGesture(outcome.gesture, context)
            val loserResolutions = gestures
                .filter { it.id != outcome.gesture.id }
                .map { loser ->
                    GestureResolution.ignored(
                        gesture = loser,
                        reason = "Lost conflict: ${outcome.reason.summary}",
                        contextSnapshot = "winner=${outcome.gesture.id}",
                    )
                }
            MultiGestureResolution(winner = winnerResolution, losers = loserResolutions)
        }

        is ConflictOutcome.NoContest -> {
            val resolution = resolveGesture(outcome.gesture, context)
            MultiGestureResolution(winner = resolution, losers = emptyList())
        }

        is ConflictOutcome.Tie, is ConflictOutcome.Ambiguous -> {
            // On tie / ambiguous, ignore all — Reducer should not receive
            // an Intent for an unresolved conflict.
            val gesturesList = when (outcome) {
                is ConflictOutcome.Tie -> outcome.gestures
                is ConflictOutcome.Ambiguous -> outcome.gestures
                else -> emptyList()
            }
            val ignoredResolutions = gesturesList.map { g ->
                GestureResolution.ignored(
                    gesture = g,
                    reason = "Conflict unresolved",
                    contextSnapshot = "ambiguous",
                )
            }
            // Pick the first as the "winner" for API shape, but mark it ignored.
            MultiGestureResolution(
                winner = ignoredResolutions.first(),
                losers = ignoredResolutions.drop(1),
            )
        }
    }
}
