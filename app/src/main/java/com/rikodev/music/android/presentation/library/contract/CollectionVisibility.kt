/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B1 — Collection Contract Pack v2 (Final)
 *
 * File 5 / 15 — CollectionVisibility.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Visibility is the *runtime* state of a collection on screen.
 * Priority decides where it lives in the model; Visibility decides
 * how it currently renders.
 *
 * The contract defines the set of legal visibility states and their
 * allowed transitions. The runtime (B2) is free to move a collection
 * between these states in response to user actions (collapse, expand)
 * or engine signals (insufficient items, diagnostics).
 *
 * RULE #32 — Every Card Owns One Snapshot
 * RULE #34 — Header Explains Context
 * Visibility is the runtime expression of "what does this section
 * currently look like to the user".
 *
 * No logic. Pure declaration.
 */

package com.rikodev.music.android.presentation.library.contract

/**
 * The runtime visibility state of a collection on screen.
 *
 * Sealed class (not enum) because each state carries different
 * metadata — collapse depth, adaptive reason, developer flag, etc.
 */
sealed class CollectionVisibility {

    /** Stable string identity for analytics and persistence. */
    abstract val internalId: String

    /** True if the section header is rendered (even if cards are not). */
    abstract val rendersHeader: Boolean

    /** True if any cards are rendered. */
    abstract val rendersCards: Boolean

    /** True if this state can be exited by a user action. */
    abstract val userOverridable: Boolean

    /** Whether this state should be persisted across cold starts. */
    abstract val persistent: Boolean

    /**
     * The default state. Section is fully expanded, all cards rendered.
     */
    data class Visible(
        val expandedCards: Int = Int.MAX_VALUE,
    ) : CollectionVisibility() {
        override val internalId = "visible"
        override val rendersHeader = true
        override val rendersCards = true
        override val userOverridable = true
        override val persistent = false
    }

    /**
     * Section is collapsed — header rendered, cards hidden. The user
     * can expand again with a single tap. RIKO remembers the
     * collapsed state across sessions.
     */
    data class Collapsed(
        val rememberAcrossSessions: Boolean = true,
    ) : CollectionVisibility() {
        override val internalId = "collapsed"
        override val rendersHeader = true
        override val rendersCards = false
        override val userOverridable = true
        override val persistent = true
    }

    /**
     * Section is *partially* expanded — only the top N cards are
     * shown, with a "show more" affordance. Useful for collections
     * with 50+ items where showing all would dominate the feed.
     */
    data class Expanded(
        val visibleCardCount: Int,
        val totalCount: Int,
        val collapsedAfter: Int = visibleCardCount,
    ) : CollectionVisibility() {
        override val internalId = "expanded"
        override val rendersHeader = true
        override val rendersCards = true
        override val userOverridable = true
        override val persistent = false
    }

    /**
     * Only visible when developer mode is on. Used for diagnostic
     * collections like DIAGNOSTIC_ORPHANS.
     */
    data object DeveloperOnly : CollectionVisibility() {
        override val internalId = "developer_only"
        override val rendersHeader = true
        override val rendersCards = true
        override val userOverridable = false
        override val persistent = false
    }

    /**
     * Never rendered. The collection exists in the engine but is
     * suppressed (e.g. insufficient items, category disabled by user).
     */
    data class Hidden(
        val reason: VisibilitySuppressionReason,
    ) : CollectionVisibility() {
        override val internalId = "hidden"
        override val rendersHeader = false
        override val rendersCards = false
        override val userOverridable = false
        override val persistent = false
    }

    /**
     * Visibility is determined at runtime by engine context. The
     * contract declines to predict; the runtime decides.
     */
    data class Adaptive(
        val rule: AdaptiveVisibilityRule,
    ) : CollectionVisibility() {
        override val internalId = "adaptive"
        override val rendersHeader = true
        override val rendersCards = true
        override val userOverridable = true
        override val persistent = false
    }
}

/**
 * Why a collection is currently suppressed at runtime.
 */
enum class VisibilitySuppressionReason(internal val description: String) {
    INSUFFICIENT_ITEMS("Engine produced fewer items than the collection's minimum."),
    CATEGORY_DISABLED("User disabled this collection's category in settings."),
    EMPTY_LIBRARY("Library has no songs yet — no collection can be generated."),
    PERMISSION_REQUIRED("Required permission (e.g. media access) not granted."),
    ENGINE_ERROR("Engine failed to produce a result for this collection."),
    EXPERIMENTAL_FLAG_OFF("Collection is experimental and the flag is off."),
    DEVELOPER_MODE_OFF("Collection is developer-only and developer mode is off."),
}

/**
 * Rules the engine (B3) can use to compute adaptive visibility.
 * The contract declares the legal rule set; the engine chooses.
 */
enum class AdaptiveVisibilityRule(internal val description: String) {
    TIME_OF_DAY("Show only during certain hours (e.g. NIGHT_CALM after 9pm)."),
    RECENT_ACTIVITY("Show only if user recently played something relevant."),
    COLLECTION_HEALTH("Hide if engine diagnostics report low confidence."),
    CONTEXT_MATCH("Show only if current context (weather, activity) matches."),
    ROTATION("Rotate with other collections to avoid feed stagnation."),
}
