/*
 * RIKO EXPERIENCE OS — LIBRARY INTERACTION RUNTIME
 * Build B5.4 — Library Interaction Runtime
 *
 * File 1 / 15 — LibraryGesture.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * The Library Gesture vocabulary. This file declares *every gesture*
 * the Library screen can recognize — nothing more, nothing less.
 *
 * It is the **alphabet** of interaction. No interpretation. No
 * resolution. No priority. No intent generation. Those concerns live
 * in `GestureResolver.kt` and downstream consumers.
 *
 * This file is the single source of truth for "what is a gesture".
 * Adding a new gesture means adding a new sealed variant here — and
 * only here. Every other file consumes this vocabulary.
 *
 * RIKO'S GESTURE PHILOSOPHY
 * -------------------------
 *   1. A gesture is *coordinates + time*. It is never a UI decision.
 *      (Rule #80 — Gesture Never Knows UI)
 *   2. A gesture is *replayable*. Same coordinates + time → same
 *      gesture object, byte-identical. (Rule #81)
 *   3. A gesture carries its *source* (which layer produced it) so the
 *      resolver can apply priority without re-reading the screen tree.
 *   4. A gesture does NOT carry its outcome. Outcome is decided by
 *      `GestureResolver` after conflict resolution. (Rule #82)
 *
 * 15 GESTURES
 * ----------
 *   Tap, DoubleTap, LongPress, Press, Hover, Drag, DragCancel, Fling,
 *   Swipe, Scroll, Pinch, MultiTouch, EdgeSwipe, Context, DockPull
 *
 * RULES ENFORCED HERE
 * -------------------
 * #78  Interaction Creates Intent, Never Action — gestures are data,
 *      not action emitters.
 * #80  Gesture Never Knows UI — no Glass / Wave / Card / Hero / Dock
 *      reference appears anywhere in this file.
 * #81  Every Interaction Is Replayable — every variant is a `data class`
 *      with only serializable primitive / value-class fields.
 * #82  Every Gesture Has Explanation — `GestureMetrics` carries the
 *      raw numbers the resolver will use to produce an explanation.
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Compose. No Android framework. No Coroutine. No Flow. No
 * ViewModel. No function with side effects. No `var`. No `lateinit`.
 * No nullable field without an explicit `?` declaration. No method
 * other than `equals` / `hashCode` / `copy` / `toString` (data class
 * defaults) and pure derivations on `val` fields.
 *
 * © RIKO Experience OS — Interaction Runtime
 */

package com.rikodev.music.android.presentation.library.interaction

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 1 — PRIMITIVE VALUE TYPES
// ═══════════════════════════════════════════════════════════════════════════
//
// These primitives are deliberately value classes / data classes (not
// type aliases) so the type system prevents accidental mixing of
// pixels, dp, ms, and velocity values. A `Pixels` cannot be passed
// where `Dp` is expected, even though both are Int internally.
// -------------------------------------------------------------------------

/**
 * A signed distance in raw screen pixels. Used for pointer coordinates
 * reported by the platform (offsetX, offsetY, etc.).
 *
 * Rule #80 reminder: a gesture never converts pixels to dp using a
 * UI-supplied density — conversion happens in the resolver, which is
 * allowed to know about UI. Gestures themselves stay in pixels.
 */
@JvmInline
value class Pixels(val value: Int) {
    operator fun plus(other: Pixels): Pixels = Pixels(value + other.value)
    operator fun minus(other: Pixels): Pixels = Pixels(value - other.value)
    operator fun compareTo(other: Pixels): Int = value.compareTo(other.value)
}

/**
 * A signed distance in density-independent pixels. Used for thresholds
 * (touch slop, drag distance, swipe distance) and for *derived* metrics
 * (total movement reported as dp for human-readable explanation).
 *
 * Gestures accept `Dp` for thresholds because thresholds are part of
 * the gesture's *definition*, not part of UI state.
 */
@JvmInline
value class Dp(val value: Int) {
    operator fun plus(other: Dp): Dp = Dp(value + other.value)
    operator fun minus(other: Dp): Dp = Dp(value - other.value)
    operator fun compareTo(other: Dp): Int = value.compareTo(other.value)
}

/**
 * A duration in milliseconds.
 */
@JvmInline
value class Millis(val value: Long) {
    operator fun plus(other: Millis): Millis = Millis(value + other.value)
    operator fun minus(other: Millis): Millis = Millis(value - other.value)
    operator fun compareTo(other: Millis): Int = value.compareTo(other.value)
}

/**
 * A velocity in dp-per-second. Used for swipe / fling classification.
 */
@JvmInline
value class DpPerSec(val value: Int) {
    operator fun plus(other: DpPerSec): DpPerSec = DpPerSec(value + other.value)
    operator fun compareTo(other: DpPerSec): Int = value.compareTo(other.value)
}

/**
 * A scale factor. Used for pinch (1.0 = no scale, >1 = zoom in, <1 = zoom out).
 */
@JvmInline
value class Scale(val value: Float) {
    operator fun compareTo(other: Scale): Int = value.compareTo(other.value)
}

/**
 * A rotation in degrees. Used for multi-touch (twist gesture).
 */
@JvmInline
value class Degrees(val value: Float) {
    operator fun plus(other: Degrees): Degrees = Degrees(value + other.value)
    operator fun minus(other: Degrees): Degrees = Degrees(value - other.value)
}

/**
 * A monotonically increasing gesture sequence number. Used for replay
 * ordering and for inspector correlation. Not a wall-clock timestamp.
 */
@JvmInline
value class GestureSequence(val value: Long)

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 2 — POINT + BOUNDS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * A 2D point in raw screen pixels. Always relative to the screen
 * origin (top-left). Never relative to a Composable.
 *
 * Rule #80: a gesture never says "10dp from the card's top-left" —
 * it says "screen pixel (240, 512)". The resolver / inspector can
 * translate to local coordinates later if needed.
 */
data class GesturePoint(
    val x: Pixels,
    val y: Pixels,
) {
    fun distanceTo(other: GesturePoint): Pixels {
        val dx = (other.x.value - x.value)
        val dy = (other.y.value - y.value)
        return Pixels(kotlin.math.sqrt(dx.toDouble() * dx.toDouble() + dy.toDouble() * dy.toDouble()).toInt())
    }

    companion object {
        val ZERO = GesturePoint(Pixels(0), Pixels(0))
    }
}

/**
 * A 2D vector (delta) in raw screen pixels. Used for drag / scroll
 * deltas and for fling velocities (as a vector rather than scalar).
 */
data class GestureVector(
    val dx: Pixels,
    val dy: Pixels,
) {
    val magnitude: Pixels
        get() {
            val m = kotlin.math.sqrt(dx.value.toDouble() * dx.value.toDouble() +
                    dy.value.toDouble() * dy.value.toDouble()).toInt()
            return Pixels(m)
        }

    companion object {
        val ZERO = GestureVector(Pixels(0), Pixels(0))
    }
}

/**
 * A rectangular bounds in raw screen pixels. Carried by every gesture
 * so the resolver knows *where on the screen* the gesture happened —
 * e.g. "Dock Pull" must originate within the bottom-edge band.
 *
 * Bounds are inclusive (left <= x <= right, top <= y <= bottom).
 */
data class GestureBounds(
    val left: Pixels,
    val top: Pixels,
    val right: Pixels,
    val bottom: Pixels,
) {
    fun contains(point: GesturePoint): Boolean =
        point.x.value in left.value..right.value &&
                point.y.value in top.value..bottom.value

    val width: Pixels get() = Pixels(right.value - left.value)
    val height: Pixels get() = Pixels(bottom.value - top.value)

    companion object {
        val ZERO = GestureBounds(Pixels(0), Pixels(0), Pixels(0), Pixels(0))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 3 — GESTURE SOURCE
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The screen layer that produced the gesture. Used by `GesturePriority`
 * to resolve conflicts without re-reading the Composable tree.
 *
 * Hierarchy is fixed (Rule #80, #82):
 *   SYSTEM > DOCK > HERO > CARD > CHILD > OVERLAY
 *
 * - SYSTEM: back gesture, notification shade pull, accessibility
 *   system gestures. Always wins.
 * - DOCK: Mini Player Dock gestures (pull, tap). Beats cards.
 * - HERO: Hero section gestures (parallax, swipe). Beats cards.
 * - CARD: Collection card gestures (tap, long press, swipe). Beats
 *   children.
 * - CHILD: A child element inside a card (chip, button, image). Loses
 *   to the card unless the child explicitly claims the gesture.
 * - OVERLAY: Transient overlay (context menu sheet, snackbar). Loses
 *   to everything below it because overlays consume gestures
 *   explicitly when shown.
 *
 * Rule #80: the source is a *screen-layer label*, not a UI reference.
 * The gesture does not know whether Glass is on or what color the
 * Hero is. It only knows "I came from the DOCK layer".
 */
enum class GestureSource {
    SYSTEM,
    DOCK,
    HERO,
    CARD,
    CHILD,
    OVERLAY,
}

/**
 * Optional pointer identifier. Useful for MultiTouch and Pinch where
 * multiple simultaneous pointers must be tracked individually.
 *
 * The value is platform-assigned (Android's `pointerId` from
 * `PointerInputChange`). Gestures do not interpret it — they only
 * carry it for the resolver.
 */
@JvmInline
value class PointerId(val value: Int)

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 4 — GESTURE METRICS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Measured physical metrics of a gesture. Carried by every gesture
 * variant so the resolver can produce a `GestureExplanation` (Rule #82)
 * without re-measuring.
 *
 * All values are derived from raw gesture data — no UI state consulted.
 */
data class GestureMetrics(
    /** Total movement (dp) from gesture start to gesture end. */
    val totalMovementDp: Dp,

    /** Maximum instantaneous movement (dp) between any two samples. */
    val peakMovementDp: Dp,

    /** Wall-clock duration from gesture start to gesture end. */
    val duration: Millis,

    /** Final velocity (dp/s) at gesture release. Zero for non-velocity gestures. */
    val finalVelocityDpPerSec: DpPerSec,

    /** Peak velocity (dp/s) observed during the gesture. */
    val peakVelocityDpPerSec: DpPerSec,

    /** Number of distinct pointers involved (1 = single touch, 2 = pinch, etc.). */
    val pointerCount: Int,

    /** Number of movement samples collected (debugging / fidelity indicator). */
    val sampleCount: Int,
) {
    companion object {
        val ZERO = GestureMetrics(
            totalMovementDp = Dp(0),
            peakMovementDp = Dp(0),
            duration = Millis(0),
            finalVelocityDpPerSec = DpPerSec(0),
            peakVelocityDpPerSec = DpPerSec(0),
            pointerCount = 1,
            sampleCount = 0,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 5 — THE GESTURE KIND
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The 15 distinct gesture kinds recognized by the Library Interaction
 * Runtime.
 *
 * Adding a new kind requires:
 *   1. Adding a variant to this enum.
 *   2. Adding a matching variant to `LibraryGesture` sealed class.
 *   3. Updating `GestureResolver` to handle the new variant.
 *   4. Updating `InteractionValidator` to ensure the new variant is
 *      exercised by at least one preview.
 *
 * `isDiscrete` separates "happens at one instant" gestures (Tap,
 * LongPress completion, Fling release) from "happens over time"
 * gestures (Drag, Scroll, Pinch). Discrete gestures produce a single
 * Intent; continuous gestures may produce a stream.
 */
enum class LibraryGestureKind(
    internal val displayName: String,
    internal val isDiscrete: Boolean,
    internal val isContinuous: Boolean,
) {
    TAP("Tap", isDiscrete = true, isContinuous = false),
    DOUBLE_TAP("Double Tap", isDiscrete = true, isContinuous = false),
    LONG_PRESS("Long Press", isDiscrete = true, isContinuous = false),
    PRESS("Press", isDiscrete = true, isContinuous = false),
    HOVER("Hover", isDiscrete = false, isContinuous = true),
    DRAG("Drag", isDiscrete = false, isContinuous = true),
    DRAG_CANCEL("Drag Cancel", isDiscrete = true, isContinuous = false),
    FLING("Fling", isDiscrete = true, isContinuous = false),
    SWIPE("Swipe", isDiscrete = true, isContinuous = false),
    SCROLL("Scroll", isDiscrete = false, isContinuous = true),
    PINCH("Pinch", isDiscrete = false, isContinuous = true),
    MULTI_TOUCH("Multi Touch", isDiscrete = false, isContinuous = true),
    EDGE_SWIPE("Edge Swipe", isDiscrete = true, isContinuous = false),
    CONTEXT("Context", isDiscrete = true, isContinuous = false),
    DOCK_PULL("Dock Pull", isDiscrete = false, isContinuous = true),
    ;

    val isEitherDiscreteOrContinuous: Boolean get() = isDiscrete || isContinuous
}

/**
 * Cardinal direction for swipes, drags, flings, scrolls.
 */
enum class GestureDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT,
    UP_LEFT,
    UP_RIGHT,
    DOWN_LEFT,
    DOWN_RIGHT,
    NONE,
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 6 — THE GESTURE SEALED CLASS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Every Library gesture is one of these 15 variants.
 *
 * Common fields carried by every variant (via the abstract properties):
 *   - `id`            — stable identifier for replay correlation.
 *   - `sequence`      — monotonic sequence number for ordering.
 *   - `timestampMs`   — wall-clock start time (epoch ms).
 *   - `source`        — which screen layer produced it.
 *   - `kind`          — the gesture kind enum value.
 *   - `metrics`       — measured physical metrics (Rule #82).
 *
 * Rule #80: NO variant carries UI references. No `Card`, no `Glass`,
 * no `Wave`, no `Hero`, no `Dock`. The variant knows pixels, time,
 * and source layer — nothing else.
 *
 * Rule #81: Every variant is a `data class` with only primitive,
 * value-class, or other `data class` fields. All fields are `val`.
 * This guarantees byte-identical replay.
 *
 * Rule #78: No variant emits an `Intent` or `Action`. Variants are
 * pure data; the `GestureResolver` produces Intents from them.
 */
sealed class LibraryGesture {

    /** Stable identifier (UUID-style, derived from sequence + timestamp). */
    abstract val id: String

    /** Monotonic sequence number assigned by the gesture collector. */
    abstract val sequence: GestureSequence

    /** Wall-clock start time, epoch milliseconds. */
    abstract val timestampMs: Long

    /** Screen layer that produced this gesture. */
    abstract val source: GestureSource

    /** The kind enum value matching this variant. */
    abstract val kind: LibraryGestureKind

    /** Measured physical metrics for explanation (Rule #82). */
    abstract val metrics: GestureMetrics

    // ─────────────────────────────────────────────────────────────────────
    // DISCRETE GESTURES — single point in time
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Tap — single, brief touch within touch slop and tap window.
     *
     * @property position       Where the tap landed (screen pixels).
     * @property bounds         Bounds of the element that received the tap.
     * @property duration       Wall-clock duration from down to up.
     * @property movementDp     Total movement during the tap (must be
     *                          within touch slop to qualify as Tap).
     */
    data class Tap(
        override val id: String,
        override val sequence: GestureSequence,
        override val timestampMs: Long,
        override val source: GestureSource,
        override val metrics: GestureMetrics,
        val position: GesturePoint,
        val bounds: GestureBounds,
        val duration: Millis,
        val movementDp: Dp,
    ) : LibraryGesture() {
        override val kind: LibraryGestureKind get() = LibraryGestureKind.TAP
    }

    /**
     * Double Tap — two quick taps within `maxDoubleTapGapMs`.
     *
     * @property firstTap       The first Tap of the pair.
     * @property secondTap      The second Tap of the pair.
     * @property gapMs          Gap between first tap up and second tap down.
     */
    data class DoubleTap(
        override val id: String,
        override val sequence: GestureSequence,
        override val timestampMs: Long,
        override val source: GestureSource,
        override val metrics: GestureMetrics,
        val firstTap: Tap,
        val secondTap: Tap,
        val gapMs: Millis,
    ) : LibraryGesture() {
        override val kind: LibraryGestureKind get() = LibraryGestureKind.DOUBLE_TAP
    }

    /**
     * Long Press — sustained touch beyond `minDurationMs`, movement
     * within `maxMovementDp`. Fires a haptic at threshold crossing.
     *
     * @property position       Where the press started.
     * @property bounds         Bounds of the element that received the press.
     * @property duration       Total duration (must be >= minDurationMs).
     * @property movementDp     Total movement during the press.
     * @property hapticFired    True if the long-press haptic fired.
     */
    data class LongPress(
        override val id: String,
        override val sequence: GestureSequence,
        override val timestampMs: Long,
        override val source: GestureSource,
        override val metrics: GestureMetrics,
        val position: GesturePoint,
        val bounds: GestureBounds,
        val duration: Millis,
        val movementDp: Dp,
        val hapticFired: Boolean,
    ) : LibraryGesture() {
        override val kind: LibraryGestureKind get() = LibraryGestureKind.LONG_PRESS
    }

    /**
     * Press — touch-down event before classification. Distinct from
     * Tap (which is down + up within window) and LongPress (which
     * requires minimum duration). Press fires immediately on touch-down
     * and is used for press-state visual feedback only.
     *
     * @property position       Where the press landed.
     * @property bounds         Bounds of the element that received the press.
     * @property pressure       Normalized pressure [0..1] (platform-reported).
     */
    data class Press(
        override val id: String,
        override val sequence: GestureSequence,
        override val timestampMs: Long,
        override val source: GestureSource,
        override val metrics: GestureMetrics,
        val position: GesturePoint,
        val bounds: GestureBounds,
        val pressure: Float,
    ) : LibraryGesture() {
        override val kind: LibraryGestureKind get() = LibraryGestureKind.PRESS
    }

    /**
     * Drag Cancel — touch cancelled (e.g. parent intercepted, system
     * gesture took over, finger left the window). Distinct from
     * Drag-End because cancel must roll back any in-progress visual
     * state (reorder preview, swipe-to-dismiss reveal, etc.).
     *
     * @property originalDragId The id of the Drag being cancelled.
     * @property position       Position at cancel moment.
     * @property reason         Why the drag was cancelled.
     */
    data class DragCancel(
        override val id: String,
        override val sequence: GestureSequence,
        override val timestampMs: Long,
        override val source: GestureSource,
        override val metrics: GestureMetrics,
        val originalDragId: String,
        val position: GesturePoint,
        val reason: DragCancelReason,
    ) : LibraryGesture() {
        override val kind: LibraryGestureKind get() = LibraryGestureKind.DRAG_CANCEL
    }

    /**
     * Fling — fast release at the end of a drag, with velocity above
     * `minVelocityDpPerSec`. Distinct from Swipe because Fling carries
     * the velocity vector (not just a direction) and may originate
     * from any drag (not just a directional swipe gesture).
     *
     * @property startPosition  Where the originating drag started.
     * @property endPosition    Where the finger released.
     * @property velocity       Velocity vector at release (dp/s).
     * @property direction      Cardinal direction of the fling.
     */
    data class Fling(
        override val id: String,
        override val sequence: GestureSequence,
        override val timestampMs: Long,
        override val source: GestureSource,
        override val metrics: GestureMetrics,
        val startPosition: GesturePoint,
        val endPosition: GesturePoint,
        val velocity: GestureVector,
        val direction: GestureDirection,
    ) : LibraryGesture() {
        override val kind: LibraryGestureKind get() = LibraryGestureKind.FLING
    }

    /**
     * Swipe — discrete, directional, completed gesture (down → move →
     * up) that crossed `minSwipeDistanceDp` and stayed within
     * `maxOffAxisAngleDeg` of a cardinal axis.
     *
     * Distinct from Fling: Swipe is *directional* (LEFT/RIGHT/UP/DOWN)
     * and does not require high velocity. A slow swipe is still a swipe.
     *
     * @property startPosition  Where the swipe started.
     * @property endPosition    Where the swipe ended.
     * @property direction      Resolved cardinal direction.
     * @property distanceDp     Total swipe distance (dp).
     * @property velocityDpPerSec  Final velocity (dp/s).
     */
    data class Swipe(
        override val id: String,
        override val sequence: GestureSequence,
        override val timestampMs: Long,
        override val source: GestureSource,
        override val metrics: GestureMetrics,
        val startPosition: GesturePoint,
        val endPosition: GesturePoint,
        val direction: GestureDirection,
        val distanceDp: Dp,
        val velocityDpPerSec: DpPerSec,
    ) : LibraryGesture() {
        override val kind: LibraryGestureKind get() = LibraryGestureKind.SWIPE
    }

    /**
     * Edge Swipe — swipe that originated within `edgeBandDp` of a
     * screen edge. Treated as a system-level gesture (priority SYSTEM).
     *
     * @property edge           Which edge the swipe originated from.
     * @property startPosition  Where the swipe started.
     * @property endPosition    Where the swipe ended.
     * @property direction      Resolved cardinal direction (must point
     *                          *away* from the edge).
     * @property distanceDp     Total distance (dp).
     */
    data class EdgeSwipe(
        override val id: String,
        override val sequence: GestureSequence,
        override val timestampMs: Long,
        override val source: GestureSource,
        override val metrics: GestureMetrics,
        val edge: ScreenEdge,
        val startPosition: GesturePoint,
        val endPosition: GesturePoint,
        val direction: GestureDirection,
        val distanceDp: Dp,
    ) : LibraryGesture() {
        override val kind: LibraryGestureKind get() = LibraryGestureKind.EDGE_SWIPE
    }

    /**
     * Context — explicit context-menu trigger (right-click on mouse,
     * context key on keyboard, or long-press that completed and was
     * promoted to a Context gesture rather than firing its own action).
     *
     * Distinct from LongPress because Context is *intent* (open menu)
     * while LongPress is *recognition* (held finger for 400ms). A
     * LongPress may produce a Context gesture via the resolver.
     *
     * @property position       Where the context trigger happened.
     * @property bounds         Bounds of the target element.
     * @property trigger        How the context gesture was triggered.
     */
    data class Context(
        override val id: String,
        override val sequence: GestureSequence,
        override val timestampMs: Long,
        override val source: GestureSource,
        override val metrics: GestureMetrics,
        val position: GesturePoint,
        val bounds: GestureBounds,
        val trigger: ContextTrigger,
    ) : LibraryGesture() {
        override val kind: LibraryGestureKind get() = LibraryGestureKind.CONTEXT
    }

    // ─────────────────────────────────────────────────────────────────────
    // CONTINUOUS GESTURES — happen over time
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Hover — pointer is over an element without pressing. Mouse-only
     * on Android phones; present on tablets / ChromeOS / DeX.
     *
     * @property position       Current pointer position.
     * @property bounds         Bounds of the element being hovered.
     * @property entered        True if pointer just entered the bounds.
     * @property exited         True if pointer just exited the bounds.
     * @property movementDp     Movement during this hover sample.
     */
    data class Hover(
        override val id: String,
        override val sequence: GestureSequence,
        override val timestampMs: Long,
        override val source: GestureSource,
        override val metrics: GestureMetrics,
        val position: GesturePoint,
        val bounds: GestureBounds,
        val entered: Boolean,
        val exited: Boolean,
        val movementDp: Dp,
    ) : LibraryGesture() {
        override val kind: LibraryGestureKind get() = LibraryGestureKind.HOVER
    }

    /**
     * Drag — sustained touch with movement beyond `minDragDistanceDp`.
     * Continuous: emits a stream of Drag events as the finger moves.
     *
     * @property startPosition  Where the drag started.
     * @property currentPosition  Current finger position.
     * @property totalDelta      Accumulated delta from start to current.
     * @property lastDelta      Delta since the previous Drag event.
     * @property direction      Dominant cardinal direction so far.
     * @property isReorder      True if this drag is a reorder drag
     *                          (long-press-then-drag in a list).
     * @property isSwipeToDismiss  True if this drag is a swipe-to-dismiss.
     */
    data class Drag(
        override val id: String,
        override val sequence: GestureSequence,
        override val timestampMs: Long,
        override val source: GestureSource,
        override val metrics: GestureMetrics,
        val startPosition: GesturePoint,
        val currentPosition: GesturePoint,
        val totalDelta: GestureVector,
        val lastDelta: GestureVector,
        val direction: GestureDirection,
        val isReorder: Boolean,
        val isSwipeToDismiss: Boolean,
    ) : LibraryGesture() {
        override val kind: LibraryGestureKind get() = LibraryGestureKind.DRAG
    }

    /**
     * Scroll — vertical or horizontal scroll within a scrollable
     * container. Continuous: emits a stream of Scroll events.
     *
     * Distinct from Drag: Scroll is *content panning* (the container
     * moves its content), Drag is *element manipulation* (an element
     * moves relative to its container). A card swipe is a Drag; the
     * list scrolling under it is a Scroll.
     *
     * @property startPosition  Where the scroll started.
     * @property currentPosition  Current finger position.
     * @property delta          Delta since last Scroll event.
     * @property direction      Scroll direction (UP/DOWN/LEFT/RIGHT).
     * @property containerBounds  Bounds of the scrolling container.
     * @property isFastScroll   True if this scroll is a fast-scroll
     *                          (thumb drag on the scroll bar).
     */
    data class Scroll(
        override val id: String,
        override val sequence: GestureSequence,
        override val timestampMs: Long,
        override val source: GestureSource,
        override val metrics: GestureMetrics,
        val startPosition: GesturePoint,
        val currentPosition: GesturePoint,
        val delta: GestureVector,
        val direction: GestureDirection,
        val containerBounds: GestureBounds,
        val isFastScroll: Boolean,
    ) : LibraryGesture() {
        override val kind: LibraryGestureKind get() = LibraryGestureKind.SCROLL
    }

    /**
     * Pinch — two-finger pinch (zoom). Continuous.
     *
     * @property focalPoint     Midpoint between the two pointers.
     * @property startSpan      Initial distance between pointers (px).
     * @property currentSpan    Current distance between pointers (px).
     * @property scale          Ratio of currentSpan / startSpan.
     * @property rotation       Rotation in degrees since gesture start.
     * @property pointers       The two pointers involved (always size 2).
     */
    data class Pinch(
        override val id: String,
        override val sequence: GestureSequence,
        override val timestampMs: Long,
        override val source: GestureSource,
        override val metrics: GestureMetrics,
        val focalPoint: GesturePoint,
        val startSpan: Pixels,
        val currentSpan: Pixels,
        val scale: Scale,
        val rotation: Degrees,
        val pointers: Pair<PointerId, PointerId>,
    ) : LibraryGesture() {
        init {
            require(pointers.first.value != pointers.second.value) {
                "Pinch requires two distinct pointers"
            }
        }

        override val kind: LibraryGestureKind get() = LibraryGestureKind.PINCH
    }

    /**
     * Multi Touch — three or more simultaneous pointers. Used for
     * advanced gestures (three-finger swipe, four-finger pinch, etc.).
     *
     * RIKO's Library currently has no multi-touch actions defined —
     * this gesture is included for future use (Rule #70: do not add
     * new patterns later; declare the surface now).
     *
     * @property pointers       All pointers involved (size >= 3).
     * @property centroid       Centroid of all pointers.
     * @property span           Average distance from centroid to each pointer.
     * @property rotation       Rotation in degrees since gesture start.
     */
    data class MultiTouch(
        override val id: String,
        override val sequence: GestureSequence,
        override val timestampMs: Long,
        override val source: GestureSource,
        override val metrics: GestureMetrics,
        val pointers: List<PointerId>,
        val centroid: GesturePoint,
        val span: Pixels,
        val rotation: Degrees,
    ) : LibraryGesture() {
        init {
            require(pointers.size >= 3) {
                "MultiTouch requires at least 3 pointers (use Pinch for 2)"
            }
            require(pointers.distinctBy { it.value }.size == pointers.size) {
                "MultiTouch pointers must be distinct"
            }
        }

        override val kind: LibraryGestureKind get() = LibraryGestureKind.MULTI_TOUCH
    }

    /**
     * Dock Pull — swipe up from the Mini Player Dock to expand it
     * into the Now Playing screen. Continuous: emits a stream of
     * DockPull events as the finger moves up.
     *
     * Distinct from Swipe because Dock Pull drives a *morph* animation
     * (the Dock physically grows into Now Playing), not a discrete
     * navigation action. The morph completes when the finger crosses
     * `dockPullCommitThresholdDp`.
     *
     * @property startPosition  Where the pull started (typically the
     *                          Dock surface).
     * @property currentPosition  Current finger position.
     * @property progress       Normalized progress [0..1] toward the
     *                          commit threshold. 0 = just started,
     *                          1 = commit threshold reached.
     * @property commitThresholdDp  Distance (dp) at which the pull
     *                          commits and the Dock morphs into Now Playing.
     * @property isCommitted    True once progress reaches 1.0.
     */
    data class DockPull(
        override val id: String,
        override val sequence: GestureSequence,
        override val timestampMs: Long,
        override val source: GestureSource,
        override val metrics: GestureMetrics,
        val startPosition: GesturePoint,
        val currentPosition: GesturePoint,
        val progress: Float,
        val commitThresholdDp: Dp,
        val isCommitted: Boolean,
    ) : LibraryGesture() {
        init {
            require(progress in 0f..1f) {
                "DockPull progress must be in [0, 1], was $progress"
            }
        }

        override val kind: LibraryGestureKind get() = LibraryGestureKind.DOCK_PULL
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 7 — SUPPORTING ENUMS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Reason a Drag was cancelled. Used by the resolver to decide whether
 * to roll back visual state silently or show a "cancelled" affordance.
 */
enum class DragCancelReason {
    /** Parent intercepted the gesture (e.g. scroll container took over). */
    PARENT_INTERCEPTED,

    /** System gesture took over (e.g. back gesture, notification shade). */
    SYSTEM_GESTURE,

    /** Finger left the window bounds. */
    LEFT_WINDOW,

    /** A higher-priority gesture won the conflict. */
    PRIORITY_LOST,

    /** Programmatic cancellation (e.g. state reset, screen change). */
    PROGRAMMATIC,

    /** Pointer became invalid (e.g. stylus battery died, touch rejected). */
    POINTER_INVALID,
}

/**
 * Screen edge for EdgeSwipe.
 */
enum class ScreenEdge {
    LEFT,
    RIGHT,
    TOP,
    BOTTOM,
}

/**
 * How a Context gesture was triggered.
 */
enum class ContextTrigger {
    /** Long press promoted to Context by the resolver. */
    LONG_PRESS,

    /** Right mouse button click. */
    RIGHT_CLICK,

    /** Keyboard ContextMenu key (or Shift+F10). */
    KEYBOARD,

    /** Accessibility "long-press" action (TalkBack double-tap-and-hold). */
    ACCESSIBILITY,
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 8 — REPLAY HELPERS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * A recorded gesture with its full lifecycle. Used by
 * `InteractionDiagnostics` for replay (Rule #81).
 *
 * This wrapper carries the gesture plus its resolution outcome, so a
 * replay can verify that the same gesture produces the same outcome.
 */
data class RecordedGesture(
    val gesture: LibraryGesture,
    val recordedAtMs: Long,
    val recorderSessionId: String,
)

/**
 * Identifies a gesture by its stable `id`. Used as a key when
 * looking up replays.
 */
@JvmInline
value class GestureId(val value: String)
