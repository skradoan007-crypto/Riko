/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B1 — Collection Contract Pack v2 (Final)
 *
 * File 12 / 15 — CollectionSectionContract.kt  ⭐ NEW in v2
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * A Collection is a logical grouping of tracks. A Section is the
 * *physical layout surface* on screen that contains one or more
 * collections.
 *
 * These are NOT the same thing.
 *
 *   Collection = data + identity + emotion
 *   Section    = layout + spacing + scroll + header
 *
 * A Section may host a single Collection (the common case) or it
 * may host several Collections blended together (e.g. a "Discovery"
 * section that blends HiddenGems + ForgottenClassics).
 *
 * RULE #41 — Section Owns Layout (v2)
 *   Collection knows nothing about layout.
 *   Layout knows nothing about engine.
 *   Section is the bridge.
 *
 * Section drives:
 *   • Card spacing rhythm
 *   • Section header style (Title + subtitle + chip)
 *   • Scroll mode (free, snap, paged)
 *   • Sticky header behaviour
 *   • Divider between sections
 *   • Wave visibility at the section level
 *
 * No logic. Pure declaration.
 */

package com.rikodev.music.android.presentation.library.contract

/**
 * The layout shape of a section.
 */
enum class SectionLayout(internal val description: String) {
    /** Horizontal scrolling strip — peek of next card visible. */
    HORIZONTAL_STRIP("Horizontal scrolling strip with peek."),

    /** 2-column grid of square cards. */
    GRID_2("2-column grid."),

    /** 3-column dense grid. */
    GRID_3("3-column dense grid."),

    /** 4-column dense grid — for compact lists like folders. */
    GRID_4("4-column dense grid."),

    /** Vertical list — full-width rows. */
    LIST("Vertical list — full-width rows."),

    /** Single full-width hero card — for Artist Spotlight. */
    HERO_SINGLE("Single full-width hero card."),

    /** Stacked cards with parallax — for Time Travel. */
    STACK_PARALLAX("Stacked cards with parallax."),
}

/**
 * Scroll mode for the section.
 */
enum class SectionScrollMode(internal val description: String) {
    FREE("Free scrolling — no snap."),
    SNAP_TO_CARD("Snap to card boundaries."),
    PAGED("Paged — one card per swipe."),
    VERTICAL_LIST("Vertical list — part of the main column."),
    LOCKED("No scrolling — fixed number of cards visible."),
}

/**
 * Sticky behaviour for the section header.
 */
enum class SectionStickyBehaviour(internal val description: String) {
    NONE("Header scrolls with content."),
    STICKY_WHEN_ACTIVE("Header pins to top when section is in view."),
    STICKY_COMPACT("Header pins to a compact form when section is in view."),
    STICKY_FADED("Header pins and fades to 50% opacity."),
}

/**
 * Divider style between this section and its neighbours.
 */
enum class SectionDivider(internal val description: String) {
    NONE("No divider — spacing alone separates sections."),
    THIN_LINE("Thin hairline divider."),
    FADED_LINE("Hairline with fade at both ends."),
    WHITESPACE("Extra whitespace — 32dp between sections."),
    GRADIENT("Gradient fade — soft transition."),
}

/**
 * Wave visibility at the section level.
 *
 * Note: this is the *section's* wave, not the *cards'* wave.
 * A section can have an ambient wave that lives behind the cards.
 */
enum class SectionWaveVisibility(
    internal val description: String,
    internal val opacity: Float,
) {
    OFF("No section wave.", 0.0f),
    SUBTLE("Subtle wave — palette-tinted.", 0.10f),
    STANDARD("Standard section wave.", 0.20f),
    EMPHASIZED("Emphasized section wave — hero sections.", 0.30f),
}

/**
 * Padding profile for the section (outer + inner gap).
 */
data class SectionPadding(
    val horizontalDp: Int,
    val verticalDp: Int,
    val cardGapDp: Int,
    val headerToContentGapDp: Int,
) {
    companion object {
        val NONE = SectionPadding(0, 0, 0, 0)
        val TIGHT = SectionPadding(8, 6, 6, 4)
        val STANDARD = SectionPadding(16, 12, 10, 8)
        val SPACIOUS = SectionPadding(20, 16, 14, 12)
        val HERO = SectionPadding(24, 20, 16, 14)
    }
}

/**
 * Section header style — drives the title + subtitle + chip layout.
 */
enum class SectionHeaderStyle(internal val description: String) {
    NONE("No header — section identified by spacing alone."),
    TITLE_ONLY("Title only — single line."),
    TITLE_SUBTITLE("Title + single subtitle line."),
    TITLE_WITH_REFRESH("Title + refresh action on the right."),
    TITLE_WITH_SEE_ALL("Title + see-all action on the right."),
    TITLE_WITH_TOGGLE("Title + toggle chip (collapse / expand)."),
    LARGE_TITLE_SUBTITLE("Large title + subtitle + optional chip."),
}

/**
 * The complete contract for a Section on the Library screen.
 *
 * A Section does NOT know what songs it contains — that's the
 * Collection's job. A Section knows only:
 *   • how it lays out cards,
 *   • how it scrolls,
 *   • whether its header sticks,
 *   • how it divides from its neighbours,
 *   • whether it has its own ambient wave,
 *   • what padding rhythm it uses,
 *   • which collection(s) it currently hosts.
 *
 * The runtime (B2) holds a List<CollectionSectionContract> in
 * LibraryState. The UI (B4) renders each section per its contract.
 */
data class CollectionSectionContract(
    /** Stable identity for this section — used in deep links + analytics. */
    val sectionId: String,
    /** Display title shown in the section header. */
    val title: String,
    /** Optional subtitle shown below the title. */
    val subtitle: String,
    /** Layout shape of the section. */
    val layout: SectionLayout,
    /** Scroll mode. */
    val scrollMode: SectionScrollMode,
    /** Sticky behaviour of the section header. */
    val stickyBehaviour: SectionStickyBehaviour,
    /** Divider between this section and the next. */
    val divider: SectionDivider,
    /** Wave visibility at the section level. */
    val waveVisibility: SectionWaveVisibility,
    /** Padding profile. */
    val padding: SectionPadding,
    /** Header style. */
    val headerStyle: SectionHeaderStyle,
    /** List of collection internalIds hosted by this section. */
    val hostedCollectionIds: List<String>,
    /** Maximum cards to render in this section (overrides Collection max for layout reasons). */
    val maxCards: Int,
    /** Whether the section is collapsible by the user. */
    val collapsible: Boolean,
    /** Whether the section remembers its collapsed state across sessions. */
    val rememberCollapseState: Boolean,
) {

    companion object {

        /** A section that hosts a single hero collection (e.g. ContinueListening). */
        fun hero(
            sectionId: String,
            title: String,
            subtitle: String,
            hostedCollectionId: String,
        ) = CollectionSectionContract(
            sectionId = sectionId,
            title = title,
            subtitle = subtitle,
            layout = SectionLayout.HORIZONTAL_STRIP,
            scrollMode = SectionScrollMode.SNAP_TO_CARD,
            stickyBehaviour = SectionStickyBehaviour.NONE,
            divider = SectionDivider.WHITESPACE,
            waveVisibility = SectionWaveVisibility.EMPHASIZED,
            padding = SectionPadding.HERO,
            headerStyle = SectionHeaderStyle.LARGE_TITLE_SUBTITLE,
            hostedCollectionIds = listOf(hostedCollectionId),
            maxCards = 10,
            collapsible = true,
            rememberCollapseState = true,
        )

        /** A section that hosts a single smart collection (e.g. HiddenGems). */
        fun smart(
            sectionId: String,
            title: String,
            subtitle: String,
            hostedCollectionId: String,
        ) = CollectionSectionContract(
            sectionId = sectionId,
            title = title,
            subtitle = subtitle,
            layout = SectionLayout.HORIZONTAL_STRIP,
            scrollMode = SectionScrollMode.SNAP_TO_CARD,
            stickyBehaviour = SectionStickyBehaviour.STICKY_WHEN_ACTIVE,
            divider = SectionDivider.FADED_LINE,
            waveVisibility = SectionWaveVisibility.STANDARD,
            padding = SectionPadding.SPACIOUS,
            headerStyle = SectionHeaderStyle.TITLE_WITH_REFRESH,
            hostedCollectionIds = listOf(hostedCollectionId),
            maxCards = 30,
            collapsible = true,
            rememberCollapseState = true,
        )

        /** A section that hosts a library-structure collection (e.g. AllAlbums as a grid). */
        fun grid(
            sectionId: String,
            title: String,
            subtitle: String,
            hostedCollectionId: String,
            columns: Int = 3,
        ) = CollectionSectionContract(
            sectionId = sectionId,
            title = title,
            subtitle = subtitle,
            layout = when (columns) {
                2 -> SectionLayout.GRID_2
                4 -> SectionLayout.GRID_4
                else -> SectionLayout.GRID_3
            },
            scrollMode = SectionScrollMode.FREE,
            stickyBehaviour = SectionStickyBehaviour.STICKY_WHEN_ACTIVE,
            divider = SectionDivider.WHITESPACE,
            waveVisibility = SectionWaveVisibility.OFF,
            padding = SectionPadding.STANDARD,
            headerStyle = SectionHeaderStyle.TITLE_SUBTITLE,
            hostedCollectionIds = listOf(hostedCollectionId),
            maxCards = Int.MAX_VALUE,
            collapsible = false,
            rememberCollapseState = false,
        )

        /** A section that hosts a list collection (e.g. AllSongs). */
        fun list(
            sectionId: String,
            title: String,
            subtitle: String,
            hostedCollectionId: String,
        ) = CollectionSectionContract(
            sectionId = sectionId,
            title = title,
            subtitle = subtitle,
            layout = SectionLayout.LIST,
            scrollMode = SectionScrollMode.VERTICAL_LIST,
            stickyBehaviour = SectionStickyBehaviour.STICKY_WHEN_ACTIVE,
            divider = SectionDivider.THIN_LINE,
            waveVisibility = SectionWaveVisibility.OFF,
            padding = SectionPadding.STANDARD,
            headerStyle = SectionHeaderStyle.TITLE_SUBTITLE,
            hostedCollectionIds = listOf(hostedCollectionId),
            maxCards = Int.MAX_VALUE,
            collapsible = false,
            rememberCollapseState = false,
        )

        /** A section that blends multiple collections (e.g. Discovery blend). */
        fun blend(
            sectionId: String,
            title: String,
            subtitle: String,
            hostedCollectionIds: List<String>,
        ) = CollectionSectionContract(
            sectionId = sectionId,
            title = title,
            subtitle = subtitle,
            layout = SectionLayout.HORIZONTAL_STRIP,
            scrollMode = SectionScrollMode.SNAP_TO_CARD,
            stickyBehaviour = SectionStickyBehaviour.STICKY_WHEN_ACTIVE,
            divider = SectionDivider.GRADIENT,
            waveVisibility = SectionWaveVisibility.SUBTLE,
            padding = SectionPadding.SPACIOUS,
            headerStyle = SectionHeaderStyle.TITLE_WITH_SEE_ALL,
            hostedCollectionIds = hostedCollectionIds,
            maxCards = 30,
            collapsible = true,
            rememberCollapseState = true,
        )
    }
}
