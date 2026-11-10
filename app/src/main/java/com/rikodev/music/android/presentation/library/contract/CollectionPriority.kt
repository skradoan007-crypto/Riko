/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B1 — Collection Contract Pack v2 (Final)
 *
 * File 4 / 15 — CollectionPriority.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * A collection's priority is the *order weight* it carries in the
 * Library. It is NOT a sort key for songs inside the collection —
 * it is the position the collection occupies among other collections
 * on the screen.
 *
 * Priority drives:
 *   • Default vertical position in the Library feed
 *   • Whether the collection can be promoted to the hero strip
 *   • Whether the collection is shown on cold start
 *   • How aggressively the engine re-evaluates it
 *
 * RULE #26 — Collection First
 * RULE #31 — Runtime Owns Truth
 * Priority is a contract-level truth, not a runtime preference.
 *
 * No logic. Pure declaration.
 */

package com.rikodev.music.android.presentation.library.contract

/**
 * Five priority tiers a RIKO collection can occupy.
 *
 * Order matters: PRIMARY > SECONDARY > OPTIONAL > BACKGROUND > HIDDEN.
 */
enum class CollectionPriority(
    val displayName: String,
    val internalId: String,
    val description: String,
    val order: Int,
    val displayWeight: Float,
    val canBePromoted: Boolean,
    val canBeDemoted: Boolean,
    val fallbackBehavior: PriorityFallback,
) {

    /**
     * Always visible. Always near the top. Cannot be demoted by the
     * user except by disabling the entire category.
     */
    PRIMARY(
        displayName = "Primary",
        internalId = "primary",
        description = "Hero-tier collections. Always shown, near the top " +
            "of the Library feed.",
        order = 0,
        displayWeight = 1.0f,
        canBePromoted = false,
        canBeDemoted = false,
        fallbackBehavior = PriorityFallback.ALWAYS_SHOW,
    ),

    /**
     * High-visibility but secondary. The user may collapse but not hide.
     */
    SECONDARY(
        displayName = "Secondary",
        internalId = "secondary",
        description = "Visible by default, but may be collapsed or " +
            "demoted to OPTIONAL by the user.",
        order = 1,
        displayWeight = 0.7f,
        canBePromoted = true,
        canBeDemoted = true,
        fallbackBehavior = PriorityFallback.SHOW_IF_NON_EMPTY,
    ),

    /**
     * Surfaced only when relevant. Quiet by default.
     */
    OPTIONAL(
        displayName = "Optional",
        internalId = "optional",
        description = "Surfaced only when the engine determines " +
            "relevance, or the user explicitly enables it.",
        order = 2,
        displayWeight = 0.4f,
        canBePromoted = true,
        canBeDemoted = true,
        fallbackBehavior = PriorityFallback.SHOW_IF_RELEVANT,
    ),

    /**
     * Never shown in the main feed. Reachable via Search or Settings.
     */
    BACKGROUND(
        displayName = "Background",
        internalId = "background",
        description = "Not shown in the main Library feed. Reachable " +
            "via Search or dedicated settings surface.",
        order = 3,
        displayWeight = 0.1f,
        canBePromoted = true,
        canBeDemoted = false,
        fallbackBehavior = PriorityFallback.HIDE_FROM_FEED,
    ),

    /**
     * Never visible to the user. Diagnostic / developer-only.
     */
    HIDDEN(
        displayName = "Hidden",
        internalId = "hidden",
        description = "Never rendered in production UI. Surfaced only " +
            "via developer diagnostic overlays.",
        order = 4,
        displayWeight = 0.0f,
        canBePromoted = false,
        canBeDemoted = false,
        fallbackBehavior = PriorityFallback.NEVER_SHOW,
    ),
    ;

    val stableKey: String get() = internalId

    /** True if this priority is part of the user-visible feed. */
    val isFeedEligible: Boolean get() = this != BACKGROUND && this != HIDDEN

    /** True if this priority is allowed on cold start. */
    val isColdStartEligible: Boolean get() = this == PRIMARY || this == SECONDARY
}

/**
 * What the runtime should do when a collection's engine produces
 * no items, or the user has hidden its category.
 */
enum class PriorityFallback(internal val description: String) {
    ALWAYS_SHOW("Render even if empty — empty state is part of the experience."),
    SHOW_IF_NON_EMPTY("Render only when the engine produced ≥1 item."),
    SHOW_IF_RELEVANT("Render only when engine context signals relevance."),
    HIDE_FROM_FEED("Do not render in main feed; keep searchable."),
    NEVER_SHOW("Never render in any user-facing surface."),
}
