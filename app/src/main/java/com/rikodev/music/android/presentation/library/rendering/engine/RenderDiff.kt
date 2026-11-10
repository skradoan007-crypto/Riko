/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B4 — Library Rendering Runtime
 *
 * File 8 / 13 — RenderDiff.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [RenderDiff] is the result of comparing two [LibraryRenderSnapshot]s.
 *
 * Why diff? Compose recomposes efficiently when it knows *exactly*
 * what changed. Without a diff, the UI either:
 *   (a) Recomposes everything (slow, janky)
 *   (b) Reimplements its own diff logic (duplicated, often wrong)
 *
 * The renderer (B4) is the natural place to compute the diff — it
 * knows the structure, it knows what stable keys mean, and it can
 * surface the diff as part of the render pipeline.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #44  Stable Key Never Changes    — diff keys are stable strings
 * #53  Diagnostics Never Affects Engine — diff never mutates state
 *
 * DESIGN
 * ------
 * The diff is computed by [SnapshotRenderer] (file 11) and returned
 * alongside the new snapshot. The UI consumes both:
 *   • The new snapshot drives the next render.
 *   • The diff drives *targeted* animations (item enter / exit /
 *     reorder) instead of a full re-render.
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Android, no Compose, no Coroutine, no Flow. Pure Kotlin data.
 *
 * © RIKO Experience OS — Rendering Tier
 */

package com.rikodev.music.android.presentation.library.rendering.engine

/**
 * The diff between two render snapshots.
 *
 * Top-level diff = a list of section-level diffs + a header diff
 * + a state diff. The UI applies these in order to animate the
 * transition from old → new.
 *
 * @property headerDiff       What changed in the header.
 * @property stateDiff        What changed in the top-level visual state.
 * @property sectionDiffs     Per-section diffs (added / removed / changed).
 * @property totalChangedItems  Sum of item-level changes across sections.
 * @property isFullRerender   True if the diff is so large that a full
 *                             re-render is cheaper than incremental.
 *                             The UI may skip animations in this case.
 */
data class RenderDiff(
    val headerDiff: HeaderDiff,
    val stateDiff: StateDiff,
    val sectionDiffs: List<SectionDiff>,
    val totalChangedItems: Int,
    val isFullRerender: Boolean,
) {
    companion object {
        /** The "nothing changed" diff — produced when snapshots are identical. */
        val NO_CHANGE = RenderDiff(
            headerDiff = HeaderDiff.NO_CHANGE,
            stateDiff = StateDiff.NO_CHANGE,
            sectionDiffs = emptyList(),
            totalChangedItems = 0,
            isFullRerender = false,
        )
    }

    /** True if the diff indicates any change at all. */
    val hasChanges: Boolean
        get() = headerDiff.hasChanges ||
            stateDiff.hasChanges ||
            sectionDiffs.isNotEmpty() ||
            totalChangedItems > 0

    /** Number of sections added in this diff. */
    val sectionsAdded: Int get() = sectionDiffs.count { it is SectionDiff.Added }

    /** Number of sections removed in this diff. */
    val sectionsRemoved: Int get() = sectionDiffs.count { it is SectionDiff.Removed }

    /** Number of sections whose items changed in this diff. */
    val sectionsChanged: Int get() = sectionDiffs.count { it is SectionDiff.Changed }
}

/**
 * Diff for the Library header.
 */
data class HeaderDiff(
    val titleChanged: Boolean,
    val subtitleChanged: Boolean,
    val accentChanged: Boolean,
    val motionHintChanged: Boolean,
    val visibilityChanged: Boolean,
) {
    companion object {
        val NO_CHANGE = HeaderDiff(
            titleChanged = false,
            subtitleChanged = false,
            accentChanged = false,
            motionHintChanged = false,
            visibilityChanged = false,
        )
    }

    val hasChanges: Boolean
        get() = titleChanged || subtitleChanged || accentChanged ||
            motionHintChanged || visibilityChanged
}

/**
 * Diff for the top-level visual state.
 */
data class StateDiff(
    val stateChanged: Boolean,
    val oldStateName: String,
    val newStateName: String,
) {
    companion object {
        val NO_CHANGE = StateDiff(
            stateChanged = false,
            oldStateName = "",
            newStateName = "",
        )
    }

    val hasChanges: Boolean get() = stateChanged
}

/**
 * Per-section diff.
 *
 * Sealed — three variants only (Added / Removed / Changed).
 * Sections whose identity + content are unchanged do not appear
 * in the diff list at all.
 */
sealed class SectionDiff {

    /** Stable identity of the section this diff applies to. Rule #44. */
    abstract val stableKey: String

    /**
     * A new section appeared in the snapshot.
     *
     * @property ordinal  Position where the section was inserted.
     */
    data class Added(
        override val stableKey: String,
        val ordinal: Int,
    ) : SectionDiff()

    /**
     * A section was removed from the snapshot.
     */
    data class Removed(
        override val stableKey: String,
    ) : SectionDiff()

    /**
     * A section's content changed (items added / removed / reordered,
     * layout changed, motion changed, etc.).
     *
     * @property itemDiffs       Per-item diffs within this section.
     * @property layoutChanged   True if the section's layout variant changed.
     * @property motionChanged   True if the section's motion spec changed.
     * @property accentChanged   True if the section's accent color changed.
     * @property visibilityChanged  True if the section's visibility toggled.
     * @property refreshChanged  True if the section's refresh state toggled.
     */
    data class Changed(
        override val stableKey: String,
        val itemDiffs: List<ItemDiff>,
        val layoutChanged: Boolean,
        val motionChanged: Boolean,
        val accentChanged: Boolean,
        val visibilityChanged: Boolean,
        val refreshChanged: Boolean,
    ) : SectionDiff() {

        /** Number of items added. */
        val itemsAdded: Int get() = itemDiffs.count { it is ItemDiff.Added }

        /** Number of items removed. */
        val itemsRemoved: Int get() = itemDiffs.count { it is ItemDiff.Removed }

        /** Number of items whose content changed. */
        val itemsChanged: Int get() = itemDiffs.count { it is ItemDiff.Changed }

        /** Number of items that moved position. */
        val itemsMoved: Int get() = itemDiffs.count { it is ItemDiff.Moved }

        /** True if any structural change happened (added / removed / moved). */
        val hasStructuralChange: Boolean
            get() = itemsAdded > 0 || itemsRemoved > 0 || itemsMoved > 0
    }
}

/**
 * Per-item diff within a section.
 */
sealed class ItemDiff {

    /** Stable identity of the item this diff applies to. Rule #44. */
    abstract val itemId: String

    /**
     * A new item appeared in the section.
     *
     * @property toOrdinal  Position where the item was inserted.
     */
    data class Added(
        override val itemId: String,
        val toOrdinal: Int,
    ) : ItemDiff()

    /**
     * An item was removed from the section.
     *
     * @property fromOrdinal  Position the item used to occupy.
     */
    data class Removed(
        override val itemId: String,
        val fromOrdinal: Int,
    ) : ItemDiff()

    /**
     * An item's content changed (title / subtitle / artwork / badges).
     *
     * @property fromOrdinal  Old position.
     * @property toOrdinal    New position (usually same as fromOrdinal).
     * @property titleChanged       True if title text changed.
     * @property subtitleChanged    True if subtitle changed.
     * @property artworkChanged     True if artwork changed.
     * @property badgesChanged      True if badges changed.
     * @property selectionChanged   True if selection state toggled.
     * @property playingChanged     True if now-playing state toggled.
     */
    data class Changed(
        override val itemId: String,
        val fromOrdinal: Int,
        val toOrdinal: Int,
        val titleChanged: Boolean,
        val subtitleChanged: Boolean,
        val artworkChanged: Boolean,
        val badgesChanged: Boolean,
        val selectionChanged: Boolean,
        val playingChanged: Boolean,
    ) : ItemDiff()

    /**
     * An item moved to a different position without content change.
     */
    data class Moved(
        override val itemId: String,
        val fromOrdinal: Int,
        val toOrdinal: Int,
    ) : ItemDiff()
}

/**
 * Computes a diff between two snapshots.
 *
 * This is a *pure* function — no side effects, no allocations
 * beyond the diff result. The renderer calls it as part of producing
 * a new snapshot; the UI never calls it directly.
 *
 * Rule #53 — Diagnostics Never Affects Engine.
 * The diff is *informational* to the UI. It never mutates state.
 */
object RenderDiffCalculator {

    /**
     * Compute the diff between [oldSnapshot] and [newSnapshot].
     *
     * If [oldSnapshot] is null (cold start), returns a "full re-render" diff
     * where every section in [newSnapshot] is reported as Added.
     */
    fun compute(
        oldSnapshot: SnapshotLike?,
        newSnapshot: SnapshotLike,
    ): RenderDiff {
        if (oldSnapshot == null) {
            return fullRerender(newSnapshot)
        }

        val headerDiff = computeHeaderDiff(oldSnapshot.header, newSnapshot.header)
        val stateDiff = computeStateDiff(oldSnapshot.state, newSnapshot.state)
        val sectionDiffs = computeSectionDiffs(oldSnapshot.sections, newSnapshot.sections)
        val totalChangedItems = sectionDiffs.sumOf { sectionChangedItemCount(it) }
        val isFullRerender = sectionDiffs.size > newSnapshot.sections.size ||
            (oldSnapshot.sections.size > 6 && sectionDiffs.size > oldSnapshot.sections.size / 2)

        return RenderDiff(
            headerDiff = headerDiff,
            stateDiff = stateDiff,
            sectionDiffs = sectionDiffs,
            totalChangedItems = totalChangedItems,
            isFullRerender = isFullRerender,
        )
    }

    private fun fullRerender(snapshot: SnapshotLike): RenderDiff {
        val sectionDiffs = snapshot.sections.mapIndexed { index, section ->
            SectionDiff.Added(stableKey = section.stableKey, ordinal = index)
        }
        return RenderDiff(
            headerDiff = HeaderDiff.NO_CHANGE,
            stateDiff = StateDiff(stateChanged = true, oldStateName = "", newStateName = snapshot.stateName),
            sectionDiffs = sectionDiffs,
            totalChangedItems = snapshot.totalItemCount,
            isFullRerender = true,
        )
    }

    private fun computeHeaderDiff(
        old: HeaderLike,
        new: HeaderLike,
    ): HeaderDiff = HeaderDiff(
        titleChanged = old.title != new.title,
        subtitleChanged = old.subtitle != new.subtitle,
        accentChanged = old.accentHex != new.accentHex,
        motionHintChanged = old.motionHint != new.motionHint,
        visibilityChanged = old.isVisible != new.isVisible,
    )

    private fun computeStateDiff(
        old: StateLike,
        new: StateLike,
    ): StateDiff = StateDiff(
        stateChanged = old.stateName != new.stateName,
        oldStateName = old.stateName,
        newStateName = new.stateName,
    )

    private fun computeSectionDiffs(
        old: List<SectionLike>,
        new: List<SectionLike>,
    ): List<SectionDiff> {
        val oldByKey = old.associateBy { it.stableKey }
        val newByKey = new.associateBy { it.stableKey }

        val added = new.filter { it.stableKey !in oldByKey.keys }
            .mapIndexed { index, section ->
                SectionDiff.Added(stableKey = section.stableKey, ordinal = index)
            }

        val removed = old.filter { it.stableKey !in newByKey.keys }
            .map { section ->
                SectionDiff.Removed(stableKey = section.stableKey)
            }

        val changed = new.filter { it.stableKey in oldByKey.keys }
            .mapNotNull { newSection ->
                val oldSection = oldByKey.getValue(newSection.stableKey)
                computeChangedSectionDiff(oldSection, newSection)
            }

        return added + removed + changed
    }

    private fun computeChangedSectionDiff(
        old: SectionLike,
        new: SectionLike,
    ): SectionDiff.Changed? {
        val itemDiffs = computeItemDiffs(old.items, new.items)

        val layoutChanged = old.layoutKey != new.layoutKey
        val motionChanged = old.motionKey != new.motionKey
        val accentChanged = old.accentHex != new.accentHex
        val visibilityChanged = old.isVisible != new.isVisible
        val refreshChanged = old.isRefreshing != new.isRefreshing

        if (itemDiffs.isEmpty() &&
            !layoutChanged && !motionChanged && !accentChanged &&
            !visibilityChanged && !refreshChanged
        ) {
            return null
        }

        return SectionDiff.Changed(
            stableKey = new.stableKey,
            itemDiffs = itemDiffs,
            layoutChanged = layoutChanged,
            motionChanged = motionChanged,
            accentChanged = accentChanged,
            visibilityChanged = visibilityChanged,
            refreshChanged = refreshChanged,
        )
    }

    private fun computeItemDiffs(
        old: List<ItemLike>,
        new: List<ItemLike>,
    ): List<ItemDiff> {
        val oldById = old.associateBy { it.itemId }
        val newById = new.associateBy { it.itemId }

        val added = new.filter { it.itemId !in oldById.keys }.map { item ->
            val toOrdinal = new.indexOf(item)
            ItemDiff.Added(itemId = item.itemId, toOrdinal = toOrdinal)
        }

        val removed = old.filter { it.itemId !in newById.keys }.map { item ->
            val fromOrdinal = old.indexOf(item)
            ItemDiff.Removed(itemId = item.itemId, fromOrdinal = fromOrdinal)
        }

        val changedOrMoved = new.mapNotNull { newItem ->
            val oldItem = oldById[newItem.itemId] ?: return@mapNotNull null
            val fromOrdinal = old.indexOf(oldItem)
            val toOrdinal = new.indexOf(newItem)

            val contentChanged = oldItem.contentHash != newItem.contentHash

            when {
                contentChanged && fromOrdinal != toOrdinal -> ItemDiff.Changed(
                    itemId = newItem.itemId,
                    fromOrdinal = fromOrdinal,
                    toOrdinal = toOrdinal,
                    titleChanged = oldItem.title != newItem.title,
                    subtitleChanged = oldItem.subtitle != newItem.subtitle,
                    artworkChanged = oldItem.artworkKey != newItem.artworkKey,
                    badgesChanged = oldItem.badgesKey != newItem.badgesKey,
                    selectionChanged = oldItem.isSelected != newItem.isSelected,
                    playingChanged = oldItem.isPlaying != newItem.isPlaying,
                )

                contentChanged -> ItemDiff.Changed(
                    itemId = newItem.itemId,
                    fromOrdinal = fromOrdinal,
                    toOrdinal = toOrdinal,
                    titleChanged = oldItem.title != newItem.title,
                    subtitleChanged = oldItem.subtitle != newItem.subtitle,
                    artworkChanged = oldItem.artworkKey != newItem.artworkKey,
                    badgesChanged = oldItem.badgesKey != newItem.badgesKey,
                    selectionChanged = oldItem.isSelected != newItem.isSelected,
                    playingChanged = oldItem.isPlaying != newItem.isPlaying,
                )

                fromOrdinal != toOrdinal -> ItemDiff.Moved(
                    itemId = newItem.itemId,
                    fromOrdinal = fromOrdinal,
                    toOrdinal = toOrdinal,
                )

                else -> null
            }
        }

        return added + removed + changedOrMoved
    }

    private fun sectionChangedItemCount(diff: SectionDiff): Int = when (diff) {
        is SectionDiff.Added -> 1
        is SectionDiff.Removed -> 1
        is SectionDiff.Changed -> diff.itemDiffs.size
    }
}

// ---------------------------------------------------------------------------
// SNAPSHOT-LIKE STRUCTURAL INTERFACES
// ---------------------------------------------------------------------------
// The diff calculator above operates on lightweight structural views
// over the real snapshot types. This keeps the diff logic testable
// in isolation and avoids coupling the calculator to the full
// snapshot model.
//
// The [SnapshotRenderer] (file 11) is responsible for adapting real
// [LibraryRenderSnapshot] instances to these interfaces before
// invoking [RenderDiffCalculator.compute].
// ---------------------------------------------------------------------------

interface SnapshotLike {
    val stateName: String
    val totalItemCount: Int
    val header: HeaderLike
    val state: StateLike
    val sections: List<SectionLike>
}

interface HeaderLike {
    val title: String
    val subtitle: String?
    val accentHex: String
    val motionHint: String
    val isVisible: Boolean
}

interface StateLike {
    val stateName: String
}

interface SectionLike {
    val stableKey: String
    val layoutKey: String
    val motionKey: String
    val accentHex: String
    val isVisible: Boolean
    val isRefreshing: Boolean
    val items: List<ItemLike>
}

interface ItemLike {
    val itemId: String
    val title: String
    val subtitle: String?
    val artworkKey: String
    val badgesKey: String
    val isSelected: Boolean
    val isPlaying: Boolean
    val contentHash: String
}
