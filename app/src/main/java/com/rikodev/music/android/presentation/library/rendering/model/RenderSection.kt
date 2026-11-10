/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B4 — Library Rendering Runtime
 *
 * File 2 / 13 — RenderSection.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * A [RenderSection] is the *visual* representation of one
 * [CollectionSectionContract] + its [CollectionRuntimeSnapshot].
 *
 * The runtime speaks in terms of contracts and snapshots. The UI
 * speaks in terms of "what do I draw, in what order, with what
 * motion". [RenderSection] is the translation layer between those
 * two vocabularies.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #28  Motion Explains Hierarchy   — section-level motion hint
 * #29  Motion Explains Hierarchy   — hierarchy is encoded in elevation + motion
 * #33  Collection Has Identity     — stableKey is the section's identity
 * #44  Stable Key Never Changes    — stableKey is always a string
 * #51  Ranking Never Filters Identity — render layer respects identity order
 *
 * RELATIONSHIP TO RUNTIME
 * -----------------------
 * A [RenderSection] is built FROM:
 *   • [CollectionSectionContract]      — sectionId, order, collapse rules
 *   • [CollectionContract]             — identity, personality, presentation
 *   • [CollectionRuntimeSnapshot]      — cards, visibility, refresh state
 *   • Active theme + tokens            — color, typography resolution
 *
 * A [RenderSection] is CONSUMED BY:
 *   • Compose UI (B5+)                 — draws the section as declared
 *   • Inspector overlay                — uses diagnostics fields
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Android, no Compose, no Coroutine, no Flow. Pure Kotlin data.
 *
 * © RIKO Experience OS — Rendering Tier
 */

package com.rikodev.music.android.presentation.library.rendering.model

import com.rikodev.music.android.presentation.library.rendering.layout.RenderLayout
import com.rikodev.music.android.presentation.library.rendering.motion.RenderMotion
import com.rikodev.music.android.presentation.library.rendering.state.RenderEmptyState
import androidx.compose.ui.unit.Dp
import androidx.compose.animation.core.Animatable

/**
 * The resolved visual representation of one Library section.
 *
 * @property stableKey        Identity — matches [CollectionContract.stableKey]. Rule #44.
 * @property sectionId        Bridge to [CollectionSectionContract.sectionId].
 * @property title            Display title (already localized + personality-shaped).
 * @property subtitle         Optional subtitle (e.g. "Played 3 times this week").
 * @property ordinal          Sort order within the parent snapshot (0-based).
 * @property layout           Resolved layout spec (grid / list / strip / stack).
 * @property motion           Motion spec for this section. Rule #28 / #29.
 * @property items            Ordered list of render items. May be empty.
 * @property emptyState       Empty-state spec, used when [items] is empty.
 *                            Rule #30 — Empty State Has Emotion.
 * @property accentHex        Resolved accent color for the section.
 * @property elevationDp      Visual elevation — encodes hierarchy. Rule #29.
 * @property isVisible        False if the section is collapsed / hidden.
 * @property isRefreshing     True if the engine is currently refreshing.
 * @property totalCount       Total items the engine considered (may exceed items.size).
 * @property renderFlags      Per-section flags (see [RenderSectionFlags]).
 */
data class RenderSection(
    val stableKey: String,
    val sectionId: String,
    val title: String,
    val subtitle: String?,
    val ordinal: Int,
    val layout: RenderLayout,
    val motion: RenderMotion,
    val items: List<RenderItem>,
    val emptyState: RenderEmptyState,
    val accentHex: String,
    val elevationDp: Float,
    val isVisible: Boolean,
    val isRefreshing: Boolean,
    val totalCount: Int,
    val renderFlags: RenderSectionFlags,
) {
    /** True if the section has zero items to display. */
    val isEmpty: Boolean get() = items.isEmpty()

    /** Number of items currently rendered (post-collapse, post-cap). */
    val visibleItemCount: Int get() = if (isVisible) items.size else 0

    /** Convenience: first item, or null if empty. */
    val firstItem: RenderItem? get() = items.firstOrNull()

    /** Convenience: last item, or null if empty. */
    val lastItem: RenderItem? get() = items.lastOrNull()

    /**
     * Stable identity for diffing — combines stableKey + sectionId.
     * Two sections with the same identity should be considered "the
     * same section" for diffing purposes, even if their items differ.
     */
    val identityKey: String get() = "$stableKey::$sectionId"
}

/**
 * Per-section render flags — coarse-grained booleans that drive
 * visual decisions without forcing the UI to interpret diagnostics.
 *
 * These flags are derived from the runtime state (refresh, error,
 * etc.) — they are NOT diagnostics themselves.
 */
data class RenderSectionFlags(
    /** True if the section is at the top of the screen (sticky header). */
    val isSticky: Boolean,
    /** True if the section is newly added in this snapshot. */
    val isNewlyAdded: Boolean,
    /** True if the section's items were re-ordered since last snapshot. */
    val wasReordered: Boolean,
    /** True if the section is in a degraded state (partial data). */
    val isDegraded: Boolean,
    /** True if the section is in a featured / hero position. */
    val isFeatured: Boolean,
    /** True if the section is the user's "currently active" section. */
    val isActive: Boolean,
) {
    companion object {
        val DEFAULT = RenderSectionFlags(
            isSticky = false,
            isNewlyAdded = false,
            wasReordered = false,
            isDegraded = false,
            isFeatured = false,
            isActive = false,
        )
    }
}
