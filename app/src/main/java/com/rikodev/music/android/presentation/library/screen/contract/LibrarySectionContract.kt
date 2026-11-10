/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.1 — Library Screen Constitution
 *
 * File 5 / 15 — LibrarySectionContract.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * A Library Section is a *story beat* within the content slot.
 * Each section groups related collections (Hero, Smart Collections,
 * Albums, Artists, etc.). The order of sections IS the story
 * (Rule #54).
 *
 * This contract is distinct from [CollectionSectionContract]
 * (B1, file 12). That contract describes *how a single collection
 * is hosted*. This contract describes *how a screen-level section
 * (which may host multiple collections) is laid out*.
 *
 * RELATIONSHIP
 * ------------
 *   CollectionSectionContract  (B1 — per-collection, within a section)
 *               ↓
 *   LibrarySectionContract     (B5.1 — screen-level, may host multiple)
 *               ↓
 *   LibraryScreenContract      (B5.1 — top-level screen shape)
 *
 * RULES ENFORCED HERE
 * -------------------
 * #33  Collection Has Identity     — section identity is stable
 * #44  Stable Key Never Changes    — sectionId is a stable string
 * #54  Screen Tells a Story        — section order is story order
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Compose, no Android framework, no Coroutine, no Flow, no
 * ViewModel. Pure Kotlin data declaration.
 *
 * © RIKO Experience OS — Constitution Tier
 */

package com.rikodev.music.android.presentation.library.screen.contract

/**
 * The kind of Library section.
 *
 * Each kind has a distinct visual treatment. The order of sections
 * on screen follows the story (Rule #54):
 *
 *   HERO             → opening
 *   SMART_COLLECTION → intelligence
 *   ALBUM_GRID       → catalog
 *   ARTIST_SECTION   → artists
 *   FOLDER_SECTION   → files
 *   PLAYLIST_SECTION → curated
 *   GENERIC          → fallback
 */
enum class LibrarySectionKind(
    internal val description: String,
    internal val defaultOrdinal: Int,
) {
    /** Hero section — opening statement. */
    HERO(
        description = "Hero section — opening statement.",
        defaultOrdinal = 0,
    ),

    /** Smart collection — engine-curated row. */
    SMART_COLLECTION(
        description = "Smart collection — engine-curated.",
        defaultOrdinal = 10,
    ),

    /** Album grid — catalog of albums. */
    ALBUM_GRID(
        description = "Album grid — catalog.",
        defaultOrdinal = 20,
    ),

    /** Artist section — list or grid of artists. */
    ARTIST_SECTION(
        description = "Artist section.",
        defaultOrdinal = 30,
    ),

    /** Folder section — file-system view. */
    FOLDER_SECTION(
        description = "Folder section — file-system view.",
        defaultOrdinal = 40,
    ),

    /** Playlist section — user / auto playlists. */
    PLAYLIST_SECTION(
        description = "Playlist section.",
        defaultOrdinal = 50,
    ),

    /** Genre section — genre clusters. */
    GENRE_SECTION(
        description = "Genre section.",
        defaultOrdinal = 60,
    ),

    /** Time-period section — decades / years. */
    TIME_PERIOD_SECTION(
        description = "Time-period section — decades / years.",
        defaultOrdinal = 70,
    ),

    /** Generic fallback section. */
    GENERIC(
        description = "Generic fallback section.",
        defaultOrdinal = 100,
    ),
}

/**
 * The padding profile for a section.
 *
 * @property topDp          Padding above the section.
 * @property bottomDp       Padding below the section.
 * @property horizontalDp   Horizontal padding.
 * @property innerGapDp     Gap between items within the section.
 */
data class LibrarySectionPadding(
    val topDp: Int,
    val bottomDp: Int,
    val horizontalDp: Int,
    val innerGapDp: Int,
) {
    companion object {
        /** Standard padding — balanced rhythm. */
        val STANDARD = LibrarySectionPadding(
            topDp = 8,
            bottomDp = 24,
            horizontalDp = 16,
            innerGapDp = 12,
        )

        /** Hero padding — extra space below for cinematic feel. */
        val HERO = LibrarySectionPadding(
            topDp = 8,
            bottomDp = 32,
            horizontalDp = 0,
            innerGapDp = 16,
        )

        /** Compact padding — for dense sections (genres, folders). */
        val COMPACT = LibrarySectionPadding(
            topDp = 4,
            bottomDp = 16,
            horizontalDp = 16,
            innerGapDp = 8,
        )

        /** Spacious padding — for mood / featured sections. */
        val SPACIOUS = LibrarySectionPadding(
            topDp = 16,
            bottomDp = 32,
            horizontalDp = 16,
            innerGapDp = 16,
        )
    }
}

/**
 * The divider between sections.
 *
 * @property kind              Divider kind.
 * @property heightDp          Divider height (or gap if whitespace).
 * @property insetStartDp      Inset from start (left in LTR).
 * @property insetEndDp        Inset from end (right in LTR).
 * @property colorHex          Optional color (hex), null for transparent.
 */
data class LibrarySectionDivider(
    val kind: LibrarySectionDividerKind,
    val heightDp: Int,
    val insetStartDp: Int,
    val insetEndDp: Int,
    val colorHex: String?,
) {
    companion object {
        /** Whitespace divider — just a gap. */
        val WHITESPACE = LibrarySectionDivider(
            kind = LibrarySectionDividerKind.WHITESPACE,
            heightDp = 16,
            insetStartDp = 0,
            insetEndDp = 0,
            colorHex = null,
        )

        /** Faded line divider — subtle horizontal line. */
        val FADED_LINE = LibrarySectionDivider(
            kind = LibrarySectionDividerKind.FADED_LINE,
            heightDp = 1,
            insetStartDp = 16,
            insetEndDp = 16,
            colorHex = "#1AFFFFFF",
        )

        /** Full line divider — solid horizontal line. */
        val FULL_LINE = LibrarySectionDivider(
            kind = LibrarySectionDividerKind.SOLID_LINE,
            heightDp = 1,
            insetStartDp = 0,
            insetEndDp = 0,
            colorHex = "#33FFFFFF",
        )
    }
}

/**
 * Kind of section divider.
 */
enum class LibrarySectionDividerKind(
    internal val description: String,
) {
    /** Whitespace — no visible divider, just a gap. */
    WHITESPACE("Whitespace — gap only."),

    /** Faded line — subtle horizontal line. */
    FADED_LINE("Faded line — subtle."),

    /** Solid line — full-opacity horizontal line. */
    SOLID_LINE("Solid line."),

    /** Gradient — fades from start to end. */
    GRADIENT("Gradient — fades from start to end."),

    /** Section gap with chip — small chip in the middle (e.g. "ALBUMS"). */
    CHIP("Gap with chip in the middle."),
}

/**
 * The header style for a section.
 *
 * @property titleText          Header title text (localized).
 * @property subtitleText       Optional subtitle.
 * @property titleTypographyKey Typography role for the title.
 * @property subtitleTypographyKey  Typography role for the subtitle.
 * @property actionLabel        Optional action label (e.g. "See all").
 * @property actionKey          Stable key for the action (click routing).
 * @property accentColorHex     Accent color (hex).
 * @property isVisible          True if the header is visible.
 */
data class LibrarySectionHeaderStyle(
    val titleText: String,
    val subtitleText: String?,
    val titleTypographyKey: String,
    val subtitleTypographyKey: String,
    val actionLabel: String?,
    val actionKey: String?,
    val accentColorHex: String,
    val isVisible: Boolean,
) {
    companion object {
        /** Default header — large title, no subtitle, no action. */
        val DEFAULT = LibrarySectionHeaderStyle(
            titleText = "",
            subtitleText = null,
            titleTypographyKey = "titleLarge",
            subtitleTypographyKey = "bodySmall",
            actionLabel = null,
            actionKey = null,
            accentColorHex = "#FFFFFF",
            isVisible = true,
        )

        /** Header with "See all" action. */
        fun withSeeAll(title: String, actionKey: String): LibrarySectionHeaderStyle =
            LibrarySectionHeaderStyle(
                titleText = title,
                subtitleText = null,
                titleTypographyKey = "titleLarge",
                subtitleTypographyKey = "bodySmall",
                actionLabel = "See all",
                actionKey = actionKey,
                accentColorHex = "#FFFFFF",
                isVisible = true,
            )
    }
}

/**
 * The Library Section Contract (screen-level).
 *
 * @property sectionId          Stable identity. Rule #44.
 * @property kind               Section kind — drives default styling.
 * @property ordinal            Sort order within the screen.
 * @property padding            Padding profile.
 * @property divider            Divider after this section.
 * @property headerStyle        Header style.
 * @property isVisible          True if the section is visible.
 * @property isCollapsible      True if the user can collapse this section.
 * @property isCollapsed        True if currently collapsed.
 * @property hostedCollectionIds  Collection stableKeys hosted here.
 */
data class LibrarySectionContract(
    val sectionId: String,
    val kind: LibrarySectionKind,
    val ordinal: Int,
    val padding: LibrarySectionPadding,
    val divider: LibrarySectionDivider,
    val headerStyle: LibrarySectionHeaderStyle,
    val isVisible: Boolean,
    val isCollapsible: Boolean,
    val isCollapsed: Boolean,
    val hostedCollectionIds: List<String>,
) {
    companion object {
        /** Hero section template. */
        fun hero(sectionId: String, hostedCollectionId: String): LibrarySectionContract =
            LibrarySectionContract(
                sectionId = sectionId,
                kind = LibrarySectionKind.HERO,
                ordinal = LibrarySectionKind.HERO.defaultOrdinal,
                padding = LibrarySectionPadding.HERO,
                divider = LibrarySectionDivider.WHITESPACE,
                headerStyle = LibrarySectionHeaderStyle.DEFAULT.copy(isVisible = false),
                isVisible = true,
                isCollapsible = false,
                isCollapsed = false,
                hostedCollectionIds = listOf(hostedCollectionId),
            )

        /** Smart collection section template. */
        fun smart(
            sectionId: String,
            title: String,
            hostedCollectionId: String,
        ): LibrarySectionContract = LibrarySectionContract(
            sectionId = sectionId,
            kind = LibrarySectionKind.SMART_COLLECTION,
            ordinal = LibrarySectionKind.SMART_COLLECTION.defaultOrdinal,
            padding = LibrarySectionPadding.SPACIOUS,
            divider = LibrarySectionDivider.FADED_LINE,
            headerStyle = LibrarySectionHeaderStyle.withSeeAll(title, "seeAll.$sectionId"),
            isVisible = true,
            isCollapsible = true,
            isCollapsed = false,
            hostedCollectionIds = listOf(hostedCollectionId),
        )

        /** Album grid section template. */
        fun albums(sectionId: String, title: String, hostedCollectionIds: List<String>): LibrarySectionContract =
            LibrarySectionContract(
                sectionId = sectionId,
                kind = LibrarySectionKind.ALBUM_GRID,
                ordinal = LibrarySectionKind.ALBUM_GRID.defaultOrdinal,
                padding = LibrarySectionPadding.STANDARD,
                divider = LibrarySectionDivider.WHITESPACE,
                headerStyle = LibrarySectionHeaderStyle.withSeeAll(title, "seeAll.$sectionId"),
                isVisible = true,
                isCollapsible = true,
                isCollapsed = false,
                hostedCollectionIds = hostedCollectionIds,
            )
    }

    /** True if the section is currently collapsed (cards hidden). */
    val isCurrentlyCollapsed: Boolean get() = isCollapsible && isCollapsed

    /** Number of collections hosted by this section. */
    val hostedCollectionCount: Int get() = hostedCollectionIds.size
}
