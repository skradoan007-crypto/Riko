/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.1 — Library Screen Constitution
 *
 * File 8 / 15 — LibraryNavigationContract.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * The Library Navigation Contract declares *where the user can go
 * from the Library* and *how they get there*. It is the routing
 * layer — but declared as data, not as code.
 *
 * RIKO's navigation philosophy: navigation is *story-consistent*.
 * A user should never feel "teleported" — every transition should
 * explain itself through motion. Going to Now Playing is a morph
 * from the Dock. Going to Search is a slide from the Top Bar. Going
 * to an Album is a hero-card expansion.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #44  Stable Key Never Changes    — route keys are stable strings
 * #48  Actions Describe Intent     — navigation actions are explicit
 * #54  Screen Tells a Story        — navigation extends the story
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
 * The destination of a Library navigation.
 *
 * Each destination is a stable string key. The host (B5.5)
 * resolves these to actual Compose navigation routes.
 */
enum class LibraryNavigationDestination(
    internal val description: String,
    internal val routeKey: String,
) {
    /** Stay on Library — no navigation. */
    NONE("Stay on Library.", "library"),

    /** Open Search experience. */
    SEARCH("Open Search.", "search"),

    /** Open Now Playing (from Dock tap). */
    NOW_PLAYING("Open Now Playing.", "nowPlaying"),

    /** Open Album detail. */
    ALBUM_DETAIL("Open Album detail.", "album/{albumId}"),

    /** Open Artist detail. */
    ARTIST_DETAIL("Open Artist detail.", "artist/{artistId}"),

    /** Open Playlist detail. */
    PLAYLIST_DETAIL("Open Playlist detail.", "playlist/{playlistId}"),

    /** Open Genre detail. */
    GENRE_DETAIL("Open Genre detail.", "genre/{genreId}"),

    /** Open Folder detail. */
    FOLDER_DETAIL("Open Folder detail.", "folder/{folderId}"),

    /** Open Settings. */
    SETTINGS("Open Settings.", "settings"),

    /** Open Queue. */
    QUEUE("Open Queue.", "queue"),

    /** Open Lyrics. */
    LYRICS("Open Lyrics.", "lyrics"),

    /** Open Equalizer. */
    EQUALIZER("Open Equalizer.", "equalizer"),

    /** Go back to Library (from a sub-screen). */
    BACK("Go back to Library.", "library"),
}

/**
 * The transition style between Library and a destination.
 *
 * @property kind              Transition kind.
 * @property durationMs        Duration in ms.
 * @property isHeroTransition  True if this is a hero-card morph
 *                             (shared element transition).
 */
data class LibraryNavigationTransition(
    val kind: LibraryNavigationTransitionKind,
    val durationMs: Long,
    val isHeroTransition: Boolean,
) {
    companion object {
        /** Morph — for Dock → Now Playing (shared element). */
        val MORPH = LibraryNavigationTransition(
            kind = LibraryNavigationTransitionKind.MORPH,
            durationMs = 480,
            isHeroTransition = true,
        )

        /** Slide — for Library → Search / Settings. */
        val SLIDE = LibraryNavigationTransition(
            kind = LibraryNavigationTransitionKind.SLIDE,
            durationMs = 320,
            isHeroTransition = false,
        )

        /** Fade — for cross-fade transitions. */
        val FADE = LibraryNavigationTransition(
            kind = LibraryNavigationTransitionKind.FADE,
            durationMs = 240,
            isHeroTransition = false,
        )

        /** Expand — for hero-card → Album detail. */
        val EXPAND = LibraryNavigationTransition(
            kind = LibraryNavigationTransitionKind.EXPAND,
            durationMs = 420,
            isHeroTransition = true,
        )
    }
}

/**
 * Kind of navigation transition.
 */
enum class LibraryNavigationTransitionKind(
    internal val description: String,
) {
    /** Morph — shared element transition (Dock → Now Playing). */
    MORPH("Morph — shared element."),

    /** Slide — slides in from a direction. */
    SLIDE("Slide from direction."),

    /** Fade — crossfade between screens. */
    FADE("Crossfade."),

    /** Expand — hero card expands to fill screen. */
    EXPAND("Expand from hero card."),

    /** Collapse — inverse of Expand. */
    COLLAPSE("Collapse to hero card."),

    /** None — instant cut. */
    NONE("Instant cut."),
}

/**
 * A deep link into the Library.
 *
 * @property deepLinkKey        Stable deep link key.
 * @property parameterKeys      Parameter keys the deep link accepts.
 * @property destination        Destination when the deep link fires.
 */
data class LibraryDeepLink(
    val deepLinkKey: String,
    val parameterKeys: List<String>,
    val destination: LibraryNavigationDestination,
) {
    companion object {
        /** Deep link to a specific album. */
        val ALBUM = LibraryDeepLink(
            deepLinkKey = "riko://library/album",
            parameterKeys = listOf("albumId"),
            destination = LibraryNavigationDestination.ALBUM_DETAIL,
        )

        /** Deep link to a specific artist. */
        val ARTIST = LibraryDeepLink(
            deepLinkKey = "riko://library/artist",
            parameterKeys = listOf("artistId"),
            destination = LibraryNavigationDestination.ARTIST_DETAIL,
        )

        /** Deep link to a specific playlist. */
        val PLAYLIST = LibraryDeepLink(
            deepLinkKey = "riko://library/playlist",
            parameterKeys = listOf("playlistId"),
            destination = LibraryNavigationDestination.PLAYLIST_DETAIL,
        )

        /** Deep link to search with an initial query. */
        val SEARCH = LibraryDeepLink(
            deepLinkKey = "riko://library/search",
            parameterKeys = listOf("query"),
            destination = LibraryNavigationDestination.SEARCH,
        )
    }
}

/**
 * Back stack rule — how the back button behaves.
 *
 * @property popsToDestination   Destination the back button pops to.
 * @property clearsSelectionFirst  True if back first clears selection mode.
 * @property collapsesDockFirst  True if back first collapses an expanded dock.
 * @property closesContextMenusFirst  True if back first closes any open menu.
 */
data class LibraryBackStackRule(
    val popsToDestination: LibraryNavigationDestination,
    val clearsSelectionFirst: Boolean,
    val collapsesDockFirst: Boolean,
    val closesContextMenusFirst: Boolean,
) {
    companion object {
        val DEFAULT = LibraryBackStackRule(
            popsToDestination = LibraryNavigationDestination.NONE,
            clearsSelectionFirst = true,
            collapsesDockFirst = true,
            closesContextMenusFirst = true,
        )
    }
}

/**
 * The Library Navigation Contract.
 *
 * @property defaultDestination  Default destination (Library itself).
 * @property transitions          Map of destination → transition.
 * @property deepLinks            Supported deep links.
 * @property backStackRule        Back stack behavior.
 */
data class LibraryNavigationContract(
    val defaultDestination: LibraryNavigationDestination,
    val transitions: Map<LibraryNavigationDestination, LibraryNavigationTransition>,
    val deepLinks: List<LibraryDeepLink>,
    val backStackRule: LibraryBackStackRule,
) {
    companion object {
        val DEFAULT = LibraryNavigationContract(
            defaultDestination = LibraryNavigationDestination.NONE,
            transitions = mapOf(
                LibraryNavigationDestination.NOW_PLAYING to LibraryNavigationTransition.MORPH,
                LibraryNavigationDestination.SEARCH to LibraryNavigationTransition.SLIDE,
                LibraryNavigationDestination.SETTINGS to LibraryNavigationTransition.SLIDE,
                LibraryNavigationDestination.ALBUM_DETAIL to LibraryNavigationTransition.EXPAND,
                LibraryNavigationDestination.ARTIST_DETAIL to LibraryNavigationTransition.EXPAND,
                LibraryNavigationDestination.PLAYLIST_DETAIL to LibraryNavigationTransition.EXPAND,
                LibraryNavigationDestination.GENRE_DETAIL to LibraryNavigationTransition.EXPAND,
                LibraryNavigationDestination.FOLDER_DETAIL to LibraryNavigationTransition.EXPAND,
                LibraryNavigationDestination.QUEUE to LibraryNavigationTransition.SLIDE,
                LibraryNavigationDestination.LYRICS to LibraryNavigationTransition.MORPH,
                LibraryNavigationDestination.EQUALIZER to LibraryNavigationTransition.SLIDE,
                LibraryNavigationDestination.BACK to LibraryNavigationTransition.SLIDE,
            ),
            deepLinks = listOf(
                LibraryDeepLink.ALBUM,
                LibraryDeepLink.ARTIST,
                LibraryDeepLink.PLAYLIST,
                LibraryDeepLink.SEARCH,
            ),
            backStackRule = LibraryBackStackRule.DEFAULT,
        )
    }

    /** Look up the transition for a destination. */
    fun transitionFor(destination: LibraryNavigationDestination): LibraryNavigationTransition =
        transitions[destination] ?: LibraryNavigationTransition.FADE
}
