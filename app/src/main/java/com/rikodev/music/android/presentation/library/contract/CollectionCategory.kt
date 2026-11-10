/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B1 — Collection Contract Pack v2 (Final)
 *
 * File 2 / 15 — CollectionCategory.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * A Collection's *category* is the highest-level semantic group it
 * belongs to. Categories drive:
 *   • Search filter chips
 *   • Settings toggles (user can disable whole categories)
 *   • Analytics grouping
 *   • Default sort behaviour inside Section
 *
 * Categories are NOT genres. Categories are HOW the collection was
 * produced — by listening behaviour, by emotion, by user action, by
 * library structure, etc.
 *
 * RULE #37 — Collection Has Personality Before Data
 * A category is part of that personality.
 *
 * No logic. Pure declaration.
 */

package com.rikodev.music.android.presentation.library.contract

/**
 * Top-level semantic group a [CollectionType] belongs to.
 */
enum class CollectionCategory(
    val displayName: String,
    val internalId: String,
    val description: String,
    val allowsUserCustomization: Boolean,
    val systemManaged: Boolean,
    val displayedInSearch: Boolean,
    val displayOrder: Int,
    val canBeDisabled: Boolean,
) {

    /** Behavioural — based on what the user actually does. */
    LISTENING(
        displayName = "Listening",
        internalId = "listening",
        description = "Collections derived from your listening behaviour — " +
            "what you played, finished, or returned to.",
        allowsUserCustomization = false,
        systemManaged = true,
        displayedInSearch = true,
        displayOrder = 0,
        canBeDisabled = false,
    ),

    /** Feeling-led — "play me something warm". */
    EMOTION(
        displayName = "Emotion",
        internalId = "emotion",
        description = "Collections grouped by emotional tone — warm, " +
            "reflective, dreamy, powerful.",
        allowsUserCustomization = false,
        systemManaged = true,
        displayedInSearch = true,
        displayOrder = 1,
        canBeDisabled = true,
    ),

    /** Time-and-place led — morning, night, commute, study. */
    MOOD(
        displayName = "Mood",
        internalId = "mood",
        description = "Collections shaped by time of day, activity, " +
            "or situational context.",
        allowsUserCustomization = false,
        systemManaged = true,
        displayedInSearch = true,
        displayOrder = 2,
        canBeDisabled = true,
    ),

    /** Engine-curated exploration — gems, forgotten, similar, surprise. */
    DISCOVERY(
        displayName = "Discovery",
        internalId = "discovery",
        description = "Collections engineered to surface the parts of " +
            "your library you haven't met yet.",
        allowsUserCustomization = false,
        systemManaged = true,
        displayedInSearch = true,
        displayOrder = 3,
        canBeDisabled = true,
    ),

    /** Structural — All Songs, Recently Added, Favorites. */
    LIBRARY(
        displayName = "Library",
        internalId = "library",
        description = "Collections that organize your library itself.",
        allowsUserCustomization = true,
        systemManaged = false,
        displayedInSearch = true,
        displayOrder = 4,
        canBeDisabled = false,
    ),

    /** Artist-centric collections. */
    ARTIST(
        displayName = "Artists",
        internalId = "artist",
        description = "Collections centred on artists you listen to.",
        allowsUserCustomization = false,
        systemManaged = true,
        displayedInSearch = true,
        displayOrder = 5,
        canBeDisabled = true,
    ),

    /** Album-centric collections. */
    ALBUM(
        displayName = "Albums",
        internalId = "album",
        description = "Collections centred on albums you own.",
        allowsUserCustomization = false,
        systemManaged = true,
        displayedInSearch = true,
        displayOrder = 6,
        canBeDisabled = true,
    ),

    /** Folder-structure based collections. */
    FOLDER(
        displayName = "Folders",
        internalId = "folder",
        description = "Collections mirroring your on-device folder structure.",
        allowsUserCustomization = false,
        systemManaged = true,
        displayedInSearch = false,
        displayOrder = 7,
        canBeDisabled = true,
    ),

    /** User-authored or system-suggested playlists. */
    PLAYLIST(
        displayName = "Playlists",
        internalId = "playlist",
        description = "Playlists you created or that RIKO maintains for you.",
        allowsUserCustomization = true,
        systemManaged = false,
        displayedInSearch = true,
        displayOrder = 8,
        canBeDisabled = false,
    ),

    /** Historical / time-travel — past listening. */
    HISTORY(
        displayName = "History",
        internalId = "history",
        description = "Collections that revisit your past listening.",
        allowsUserCustomization = false,
        systemManaged = true,
        displayedInSearch = true,
        displayOrder = 9,
        canBeDisabled = true,
    ),

    /** Engine-generated smart collections (mood-based, weather, time). */
    SMART(
        displayName = "Smart",
        internalId = "smart",
        description = "Context-aware collections generated by RIKO's engine.",
        allowsUserCustomization = false,
        systemManaged = true,
        displayedInSearch = true,
        displayOrder = 10,
        canBeDisabled = true,
    ),

    /** Behind a feature flag — for early access or testing. */
    EXPERIMENTAL(
        displayName = "Experimental",
        internalId = "experimental",
        description = "Experimental collections under active development. " +
            "Subject to change without notice.",
        allowsUserCustomization = false,
        systemManaged = true,
        displayedInSearch = false,
        displayOrder = 11,
        canBeDisabled = true,
    ),
    ;

    val stableKey: String get() = internalId

    /** True if the user is allowed to permanently hide this category. */
    val isUserHideable: Boolean get() = canBeDisabled && !systemManaged.not().let { false }

    /** True if the engine owns the contents of this category. */
    val isEngineOwned: Boolean get() = systemManaged
}
