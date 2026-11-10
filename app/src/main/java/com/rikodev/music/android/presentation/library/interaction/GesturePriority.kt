/*
 * RIKO EXPERIENCE OS — LIBRARY INTERACTION RUNTIME
 * Build B5.4 — Library Interaction Runtime
 *
 * File 2 / 15 — GesturePriority.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Define the *priority hierarchy* of all Library gestures and provide
 * pure functions to resolve conflicts when multiple gestures compete
 * for the same pointer event.
 *
 * HIERARCHY (locked)
 * ------------------
 *   SYSTEM  >  DOCK  >  HERO  >  CARD  >  CHILD  >  OVERLAY
 *
 *   - SYSTEM  : back gesture, notification shade, accessibility
 *   - DOCK    : Mini Player Dock (always persistent, always wins over cards)
 *   - HERO    : Hero section (dominant element on screen)
 *   - CARD    : Collection card
 *   - CHILD   : A child inside a card (chip, button)
 *   - OVERLAY : Transient overlay (context menu, snackbar)
 *
 * WHY THIS ORDER
 * --------------
 * The hierarchy reflects *user intent priority*, not visual stacking.
 *
 *   - SYSTEM always wins because the OS owns the user's attention.
 *   - DOCK beats CARD because the Dock is persistent and small —
 *     a finger landing on the Dock almost certainly meant to interact
 *     with the Dock, not the card behind it.
 *   - HERO beats CARD because the Hero is the screen's focal point;
 *     gestures on it should not be accidentally swallowed by cards.
 *   - CARD beats CHILD because a card-level gesture (tap, long press)
 *     is more likely the user's intent than a child-level gesture
 *     (chip tap), unless the child explicitly claims the gesture.
 *   - OVERLAY loses to everything below because overlays are transient
 *     and only consume gestures when explicitly focused (modal sheet,
 *     open menu). When not focused, gestures fall through.
 *
 * CONFLICT RESOLUTION DISCIPLINE
 * ------------------------------
 *   1. Compare priorities. Higher priority wins outright.
 *   2. If priorities are equal, compare *gesture kind* — discrete
 *      gestures (Tap, LongPress) win over continuous (Drag, Scroll)
 *      at the moment of release; continuous gestures win during
 *      in-flight motion.
 *   3. If still equal, compare *temporal order* — earliest start wins.
 *   4. If still equal, compare *pointer count* — multi-pointer wins
 *      over single-pointer.
 *   5. If still equal, the conflict is *ambiguous* — resolver emits
 *      `ConflictOutcome.Ambiguous` and the diagnostic surface flags
 *      it for tuning.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #80  Gesture Never Knows UI — priorities are derived from
 *      `GestureSource` (a label), not from UI tree traversal.
 * #82  Every Gesture Has Explanation — every conflict resolution
 *      produces a `GestureExplanation` carrying the reason.
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
// SECTION 1 — PRIORITY ENUM
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The 6-level priority hierarchy for Library gestures.
 *
 * `order` is a stable integer used for comparison. Lower order value
 * means higher priority (SYSTEM = 0 wins over OVERLAY = 5).
 *
 * This enum is intentionally separate from `GestureSource` even though
 * they share names — `GestureSource` answers "where did this gesture
 * come from?" and `GesturePriority` answers "how important is it?".
 * Today the mapping is 1:1, but future overlays (e.g. a modal context
 * sheet that should win over DOCK) may decouple them.
 */
enum class GesturePriority(
    internal val order: Int,
    internal val displayName: String,
    internal val rationale: String,
) {
    /** OS-level gestures — back, notification shade, accessibility system. */
    SYSTEM(
        order = 0,
        displayName = "System",
        rationale = "OS owns user attention; always preempts Library.",
    ),

    /** Mini Player Dock — persistent, small, must not lose to cards. */
    DOCK(
        order = 1,
        displayName = "Dock",
        rationale = "Persistent surface; finger on Dock meant Dock.",
    ),

    /** Hero section — focal element of the Library screen. */
    HERO(
        order = 2,
        displayName = "Hero",
        rationale = "Screen focal point; wins over list content.",
    ),

    /** Collection card — primary content unit. */
    CARD(
        order = 3,
        displayName = "Card",
        rationale = "Primary content unit; wins over its own children.",
    ),

    /** Child element inside a card — chip, button, image. */
    CHILD(
        order = 4,
        displayName = "Child",
        rationale = "Smaller target inside a card; loses to card unless claimed.",
    ),

    /** Transient overlay — context menu sheet, snackbar. */
    OVERLAY(
        order = 5,
        displayName = "Overlay",
        rationale = "Transient; only wins when explicitly focused (modal).",
    );

    /**
     * True if this priority beats [other] in a head-to-head comparison.
     * Equal priorities return false — caller must apply a tie-breaker.
     */
    fun beats(other: GesturePriority): Boolean = this.order < other.order
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 2 — SOURCE-TO-PRIORITY MAPPING
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Convert a [GestureSource] (where the gesture came from) into a
 * [GesturePriority] (how important it is).
 *
 * The mapping is currently 1:1 but lives in a function so future
 * overlays can override it without touching every call site.
 */
fun GestureSource.toPriority(): GesturePriority = when (this) {
    GestureSource.SYSTEM  -> GesturePriority.SYSTEM
    GestureSource.DOCK    -> GesturePriority.DOCK
    GestureSource.HERO    -> GesturePriority.HERO
    GestureSource.CARD    -> GesturePriority.CARD
    GestureSource.CHILD   -> GesturePriority.CHILD
    GestureSource.OVERLAY -> GesturePriority.OVERLAY
}

/**
 * Extract the priority of a [LibraryGesture] from its `source` field.
 */
fun LibraryGesture.priority(): GesturePriority = source.toPriority()

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 3 — TIE-BREAKERS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Phase of a gesture lifecycle, used as a tie-breaker when two
 * gestures share the same priority.
 *
 * - IN_FLIGHT: the gesture is still active (finger on screen).
 *   Continuous gestures (Drag, Scroll, Pinch) are IN_FLIGHT.
 * - COMPLETED: the gesture has finished (finger lifted).
 *   Discrete gestures (Tap, LongPress, Swipe) are COMPLETED when
 *   they reach the resolver.
 *
 * During IN_FLIGHT, a continuous gesture wins over a discrete one
 * at the same priority (the user is *doing* something). At
 * COMPLETED, a discrete gesture wins (the user *finished* doing
 * something specific).
 */
enum class GestureLifecyclePhase {
    IN_FLIGHT,
    COMPLETED,
    CANCELLED,
}

/**
 * Classify the lifecycle phase of a gesture.
 *
 * Discrete gestures are always COMPLETED (they only fire once, at
 * completion). Continuous gestures are IN_FLIGHT unless cancelled.
 */
fun LibraryGesture.lifecyclePhase(): GestureLifecyclePhase = when (this) {
    is LibraryGesture.Tap          -> GestureLifecyclePhase.COMPLETED
    is LibraryGesture.DoubleTap    -> GestureLifecyclePhase.COMPLETED
    is LibraryGesture.LongPress    -> GestureLifecyclePhase.COMPLETED
    is LibraryGesture.Press        -> GestureLifecyclePhase.IN_FLIGHT
    is LibraryGesture.Hover        -> GestureLifecyclePhase.IN_FLIGHT
    is LibraryGesture.Drag         -> GestureLifecyclePhase.IN_FLIGHT
    is LibraryGesture.DragCancel   -> GestureLifecyclePhase.CANCELLED
    is LibraryGesture.Fling        -> GestureLifecyclePhase.COMPLETED
    is LibraryGesture.Swipe        -> GestureLifecyclePhase.COMPLETED
    is LibraryGesture.Scroll       -> GestureLifecyclePhase.IN_FLIGHT
    is LibraryGesture.Pinch        -> GestureLifecyclePhase.IN_FLIGHT
    is LibraryGesture.MultiTouch   -> GestureLifecyclePhase.IN_FLIGHT
    is LibraryGesture.EdgeSwipe    -> GestureLifecyclePhase.COMPLETED
    is LibraryGesture.Context      -> GestureLifecyclePhase.COMPLETED
    is LibraryGesture.DockPull     -> GestureLifecyclePhase.IN_FLIGHT
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 4 — CONFLICT OUTCOME
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The outcome of resolving a set of competing gestures.
 *
 * - Winner: the resolver selected exactly one gesture.
 * - Tie: two or more gestures are genuinely equivalent — the caller
 *   must apply an external policy (e.g. prefer the most-recent
 *   gesture, or refuse both).
 * - Ambiguous: the resolver could not decide — diagnostic surface
 *   flags this for threshold tuning.
 * - NoContest: only one gesture was supplied; no conflict existed.
 */
sealed class ConflictOutcome {

    /** A single gesture won outright. */
    data class Winner(
        val gesture: LibraryGesture,
        val reason: ConflictReason,
    ) : ConflictOutcome()

    /** Two or more gestures are tied after all tie-breakers. */
    data class Tie(
        val gestures: List<LibraryGesture>,
        val reason: ConflictReason,
    ) : ConflictOutcome()

    /** The resolver could not decide — needs human tuning. */
    data class Ambiguous(
        val gestures: List<LibraryGesture>,
        val reason: ConflictReason,
    ) : ConflictOutcome()

    /** Only one gesture was supplied — no conflict to resolve. */
    data class NoContest(
        val gesture: LibraryGesture,
    ) : ConflictOutcome()
}

/**
 * Why a conflict resolved the way it did. Carried by every
 * `ConflictOutcome` so the inspector can produce an explanation
 * (Rule #82).
 */
data class ConflictReason(
    val ruleApplied: ConflictRule,
    val winnerPriority: GesturePriority?,
    val loserPriorities: List<GesturePriority>,
    val summary: String,
) {
    companion object {
        fun priority(winner: GesturePriority, losers: List<GesturePriority>): ConflictReason =
            ConflictReason(
                ruleApplied = ConflictRule.PRIORITY,
                winnerPriority = winner,
                loserPriorities = losers,
                summary = "Priority ${winner.displayName} beats ${losers.joinToString { it.displayName }}",
            )

        fun lifecycle(winner: GestureLifecyclePhase, losers: List<GestureLifecyclePhase>): ConflictReason =
            ConflictReason(
                ruleApplied = ConflictRule.LIFECYCLE_TIEBREAK,
                winnerPriority = null,
                loserPriorities = emptyList(),
                summary = "Lifecycle $winner beats $losers",
            )

        fun temporal(winnerTimestampMs: Long, loserTimestampsMs: List<Long>): ConflictReason =
            ConflictReason(
                ruleApplied = ConflictRule.TEMPORAL_TIEBREAK,
                winnerPriority = null,
                loserPriorities = emptyList(),
                summary = "Earliest start wins (${winnerTimestampMs}ms < ${loserTimestampsMs.joinToString { "${it}ms" }})",
            )

        fun pointerCount(winnerCount: Int, loserCounts: List<Int>): ConflictReason =
            ConflictReason(
                ruleApplied = ConflictRule.POINTER_COUNT_TIEBREAK,
                winnerPriority = null,
                loserPriorities = emptyList(),
                summary = "Multi-pointer wins ($winnerCount > ${loserCounts.joinToString { "$it" }})",
            )

        fun ambiguous(): ConflictReason =
            ConflictReason(
                ruleApplied = ConflictRule.AMBIGUOUS,
                winnerPriority = null,
                loserPriorities = emptyList(),
                summary = "No tie-breaker could resolve; tune thresholds.",
            )
    }
}

/**
 * The rule that decided a conflict.
 */
enum class ConflictRule {
    /** Higher priority won outright (e.g. SYSTEM beats CARD). */
    PRIORITY,

    /** Same priority — lifecycle phase decided (IN_FLIGHT vs COMPLETED). */
    LIFECYCLE_TIEBREAK,

    /** Same priority + lifecycle — earliest start time won. */
    TEMPORAL_TIEBREAK,

    /** Same priority + lifecycle + time — multi-pointer won. */
    POINTER_COUNT_TIEBREAK,

    /** All tie-breakers exhausted — human tuning required. */
    AMBIGUOUS,
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 5 — CONFLICT RESOLVER (PURE)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Resolve a set of competing gestures into a single outcome.
 *
 * The algorithm is deterministic and pure:
 *
 *   1. If `gestures` is empty, returns null (caller's responsibility
 *      to handle).
 *   2. If `gestures` has exactly one element, returns
 *      `ConflictOutcome.NoContest`.
 *   3. Otherwise, applies tie-breakers in order:
 *      a. PRIORITY — group by priority; if one group has lower order
 *         than all others, that group wins. Within the winning group,
 *         continue to (b).
 *      b. LIFECYCLE — IN_FLIGHT beats COMPLETED beats CANCELLED.
 *         Within the surviving group, continue to (c).
 *      c. TEMPORAL — earliest `timestampMs` wins. Within the
 *         surviving group (same timestamp), continue to (d).
 *      d. POINTER_COUNT — higher `metrics.pointerCount` wins.
 *      e. If still tied, returns `ConflictOutcome.Ambiguous`.
 *
 * @param gestures The gestures competing for the same pointer event.
 * @return The outcome, or null if `gestures` is empty.
 */
fun resolveConflict(gestures: List<LibraryGesture>): ConflictOutcome? {
    if (gestures.isEmpty()) return null
    if (gestures.size == 1) return ConflictOutcome.NoContest(gestures.first())

    // Step 1: priority
    val byPriority = gestures.groupBy { it.priority() }
    val minOrder = byPriority.keys.minOf { it.order }
    val priorityWinners = byPriority
        .filter { it.key.order == minOrder }
        .values
        .flatten()

    if (priorityWinners.size == 1) {
        val winner = priorityWinners.first()
        val losers = gestures.filter { it !== winner }
        return ConflictOutcome.Winner(
            gesture = winner,
            reason = ConflictReason.priority(
                winner = winner.priority(),
                losers = losers.map { it.priority() },
            ),
        )
    }

    // Step 2: lifecycle
    val byLifecycle = priorityWinners.groupBy { it.lifecyclePhase() }
    val lifecycleOrder = listOf(
        GestureLifecyclePhase.IN_FLIGHT,
        GestureLifecyclePhase.COMPLETED,
        GestureLifecyclePhase.CANCELLED,
    )
    val lifecycleWinners = lifecycleOrder
        .firstOrNull { phase -> byLifecycle.containsKey(phase) }
        ?.let { byPriorityWinningPhase -> byLifecycle[byPriorityWinningPhase] }
        ?: priorityWinners

    if (lifecycleWinners != null && lifecycleWinners.size == 1) {
        val winner = lifecycleWinners.first()
        val losers = gestures.filter { it !== winner }
        return ConflictOutcome.Winner(
            gesture = winner,
            reason = ConflictReason.lifecycle(
                winner = winner.lifecyclePhase(),
                losers = losers.map { it.lifecyclePhase() },
            ),
        )
    }

    val survivors = lifecycleWinners ?: priorityWinners

    // Step 3: temporal (earliest start wins)
    val earliestMs = survivors.minOf { it.timestampMs }
    val temporalWinners = survivors.filter { it.timestampMs == earliestMs }

    if (temporalWinners.size == 1) {
        val winner = temporalWinners.first()
        val losers = gestures.filter { it !== winner }
        return ConflictOutcome.Winner(
            gesture = winner,
            reason = ConflictReason.temporal(
                winnerTimestampMs = winner.timestampMs,
                loserTimestampsMs = losers.map { it.timestampMs },
            ),
        )
    }

    // Step 4: pointer count
    val maxPointers = temporalWinners.maxOf { it.metrics.pointerCount }
    val pointerWinners = temporalWinners.filter { it.metrics.pointerCount == maxPointers }

    if (pointerWinners.size == 1) {
        val winner = pointerWinners.first()
        val losers = gestures.filter { it !== winner }
        return ConflictOutcome.Winner(
            gesture = winner,
            reason = ConflictReason.pointerCount(
                winnerCount = winner.metrics.pointerCount,
                loserCounts = losers.map { it.metrics.pointerCount },
            ),
        )
    }

    // Step 5: ambiguous or tie?
    // If all survivors are equal by all metrics, it's a Tie (deterministic
    // equality). Otherwise, it's Ambiguous (something is off).
    val firstSurvivor = pointerWinners.first()
    val allEqual = pointerWinners.all { it == firstSurvivor }

    return if (allEqual) {
        ConflictOutcome.Tie(
            gestures = pointerWinners,
            reason = ConflictReason.ambiguous(),
        )
    } else {
        ConflictOutcome.Ambiguous(
            gestures = pointerWinners,
            reason = ConflictReason.ambiguous(),
        )
    }
}

/**
 * Resolve a head-to-head conflict between exactly two gestures.
 *
 * Convenience wrapper around [resolveConflict] for the common case.
 */
fun resolvePair(a: LibraryGesture, b: LibraryGesture): ConflictOutcome {
    return resolveConflict(listOf(a, b)) ?: ConflictOutcome.Ambiguous(
        gestures = listOf(a, b),
        reason = ConflictReason.ambiguous(),
    )
}

/**
 * Returns true if [a] would beat [b] in a head-to-head conflict.
 *
 * Pure — does not actually run the resolver; just checks priority.
 */
fun wouldBeat(a: LibraryGesture, b: LibraryGesture): Boolean {
    val pa = a.priority()
    val pb = b.priority()
    if (pa != pb) return pa.beats(pb)

    // Same priority — check lifecycle
    val la = a.lifecyclePhase()
    val lb = b.lifecyclePhase()
    if (la != lb) {
        val order = listOf(
            GestureLifecyclePhase.IN_FLIGHT,
            GestureLifecyclePhase.COMPLETED,
            GestureLifecyclePhase.CANCELLED,
        )
        return order.indexOf(la) < order.indexOf(lb)
    }

    // Same lifecycle — check temporal
    if (a.timestampMs != b.timestampMs) return a.timestampMs < b.timestampMs

    // Same time — check pointer count
    if (a.metrics.pointerCount != b.metrics.pointerCount) {
        return a.metrics.pointerCount > b.metrics.pointerCount
    }

    // Truly equal — caller should treat as Tie
    return false
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 6 — PRIORITY-AWARE GESTURE FILTER
// ═══════════════════════════════════════════════════════════════════════════

/**
 * A pre-computed priority filter. Use to quickly reject gestures that
 * would lose to the current dominant gesture without running the full
 * resolver.
 *
 * Example: while a Drag is active on a CARD, an incoming TAP on a
 * CHILD can be rejected immediately because CARD > CHILD.
 */
data class PriorityFilter(
    val dominantGesture: LibraryGesture,
    val dominantPriority: GesturePriority,
) {
    /**
     * Returns true if [candidate] would be rejected by this filter
     * (i.e. the dominant gesture would beat it).
     */
    fun rejects(candidate: LibraryGesture): Boolean =
        wouldBeat(dominantGesture, candidate)

    /**
     * Returns true if [candidate] would beat the dominant gesture,
     * requiring the filter to be replaced.
     */
    fun displacedBy(candidate: LibraryGesture): Boolean =
        wouldBeat(candidate, dominantGesture) &&
                candidate.priority() != dominantPriority

    companion object {
        /**
         * Create a filter from a dominant gesture.
         */
        fun from(gesture: LibraryGesture): PriorityFilter =
            PriorityFilter(
                dominantGesture = gesture,
                dominantPriority = gesture.priority(),
            )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 7 — PRIORITY MATRIX (DOCUMENTATION)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * A precomputed lookup of which priorities beat which others.
 *
 * Used for documentation and for O(1) lookup in hot paths.
 * `beats[a][b] == true` means `a` beats `b`.
 */
object GesturePriorityMatrix {

    private val allPriorities: List<GesturePriority> = GesturePriority.values().toList()

    /**
     * Lookup table: `beats[a][b]` is true if priority `a` beats `b`.
     */
    val beats: Map<GesturePriority, Map<GesturePriority, Boolean>> =
        allPriorities.associateWith { a ->
            allPriorities.associateWith { b ->
                a.beats(b)
            }
        }

    /**
     * Returns the priority that wins between [a] and [b], or null if
     * they are equal (tie).
     */
    fun winner(a: GesturePriority, b: GesturePriority): GesturePriority? = when {
        a.beats(b) -> a
        b.beats(a) -> b
        else -> null
    }

    /**
     * Returns the lowest-priority gesture from a list (lowest order
     * value = highest priority).
     */
    fun highestPriority(gestures: List<LibraryGesture>): LibraryGesture? =
        gestures.minByOrNull { it.priority().order }

    /**
     * Returns all gestures that share the highest priority among the
     * input list. Used to seed the resolver after the first cut.
     */
    fun highestPriorityGroup(gestures: List<LibraryGesture>): List<LibraryGesture> {
        if (gestures.isEmpty()) return emptyList()
        val minOrder = gestures.minOf { it.priority().order }
        return gestures.filter { it.priority().order == minOrder }
    }
}
