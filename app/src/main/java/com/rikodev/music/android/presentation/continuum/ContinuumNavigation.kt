package com.rikodev.music.android.presentation.continuum

import com.rikodev.music.android.presentation.navigation.RikoDestination

// ─────────────────────────────────────────────────────────────────────────────
// RIKO CONTINUUM — NAVIGATION CORE & TIMEFOLD HISTORY (P1 + P9)
//
// Three primary living sections:
//   - LISTEN  : Home, Recently Played, Continue Listening, Now Playing
//   - EXPLORE : All Songs, Albums, Artists, Folders, Search, Discovery
//   - LIBRARY : Playlists, Favorites, Saved Collections, Personal Management
//
// Preserves independent per-section routes and a lightweight Timefold
// navigation history stack (strictly separate from playback history).
// ─────────────────────────────────────────────────────────────────────────────

enum class ContinuumSection(
    val label: String,
    val rootRoute: String,
    val tabIndex: Int
) {
    LISTEN(
        label = "Listen",
        rootRoute = RikoDestination.AllSongs.route,
        tabIndex = 0
    ),
    EXPLORE(
        label = "Explore",
        rootRoute = RikoDestination.MyMusic.route,
        tabIndex = 1
    ),
    LIBRARY(
        label = "Library",
        rootRoute = RikoDestination.LibraryHub.route,
        tabIndex = 2
    );

    companion object {
        val ordered: List<ContinuumSection> = listOf(LISTEN, EXPLORE, LIBRARY)

        fun fromIndex(index: Int): ContinuumSection =
            ordered.getOrElse(index) { LISTEN }

        fun sectionForRoute(route: String?, fallback: ContinuumSection = LISTEN): ContinuumSection = when {
            route == null -> fallback
            route == RikoDestination.AllSongs.route -> LISTEN
            route == RikoDestination.MyMusic.route ||
                route == RikoDestination.Search.route ||
                route.startsWith("album_detail") ||
                route.startsWith("artist_detail") ||
                route.startsWith("folder_detail") -> EXPLORE
            route == RikoDestination.LibraryHub.route ||
                route == RikoDestination.Playlists.route ||
                route == RikoDestination.Favorites.route ||
                route.startsWith("playlist_detail") -> LIBRARY
            else -> fallback
        }
    }
}

/**
 * Contextual actions surfaced in the Liquid Nexus secondary/contextual pocket,
 * adapted to the active CONTINUUM section and connected to real app actions.
 */
enum class ContinuumContextAction(val label: String) {
    QUEUE("Queue"),
    LYRICS("Lyrics"),
    MDNA("MDNA"),
    ARTIST("Artist"),
    ALBUM("Album"),
    SONG_INFO("Song Info"),
    FAVORITE("Favorite"),
    ADD_TO_PLAYLIST("Add to Playlist"),
    PLAYLISTS("Playlists");

    companion object {
        fun forSection(section: ContinuumSection): List<ContinuumContextAction> = when (section) {
            ContinuumSection.LISTEN -> listOf(QUEUE, LYRICS, MDNA)
            ContinuumSection.EXPLORE -> listOf(ARTIST, ALBUM, SONG_INFO)
            ContinuumSection.LIBRARY -> listOf(FAVORITE, ADD_TO_PLAYLIST, PLAYLISTS)
        }
    }
}

/**
 * A single Timefold navigation entry (navigation history, distinct from playback history).
 */
data class TimefoldEntry(
    val section: ContinuumSection,
    val route: String,
    val timestampMs: Long = 0L
)

/**
 * Immutable smart-context navigation state that preserves each section's
 * last visited route (e.g., an AlbumDetail inside Explore) so switching tabs
 * and returning restores the user's exact context.
 */
data class ContinuumNavHistoryState(
    val activeSection: ContinuumSection = ContinuumSection.LISTEN,
    val lastRouteBySection: Map<ContinuumSection, String> = mapOf(
        ContinuumSection.LISTEN to ContinuumSection.LISTEN.rootRoute,
        ContinuumSection.EXPLORE to ContinuumSection.EXPLORE.rootRoute,
        ContinuumSection.LIBRARY to ContinuumSection.LIBRARY.rootRoute
    ),
    val timefoldStack: List<TimefoldEntry> = listOf(
        TimefoldEntry(ContinuumSection.LISTEN, ContinuumSection.LISTEN.rootRoute, 0L)
    ),
    val librarySubTab: Int = 0 // 0 = Playlists, 1 = Favorites
) {
    fun targetRouteFor(section: ContinuumSection): String =
        lastRouteBySection[section] ?: section.rootRoute
}

object ContinuumNavHistoryReducer {

    private const val MAX_TIMEFOLD_ENTRIES = 32

    /**
     * Records a route visit within the appropriate section and appends to the
     * Timefold navigation trail without duplicating consecutive identical entries.
     */
    fun onRouteVisited(
        state: ContinuumNavHistoryState,
        route: String,
        timestampMs: Long = System.currentTimeMillis()
    ): ContinuumNavHistoryState {
        if (route == RikoDestination.NowPlaying.route || route == RikoDestination.Onboarding.route) {
            return state
        }
        val section = ContinuumSection.sectionForRoute(route, fallback = state.activeSection)
        val updatedMap = state.lastRouteBySection.toMutableMap()
        // Settings is an overlay/utility route; keep each section's music context intact.
        if (route != RikoDestination.Settings.route) {
            updatedMap[section] = route
        }
        val last = state.timefoldStack.lastOrNull()
        val updatedStack = if (last != null && last.route == route && last.section == section) {
            state.timefoldStack
        } else {
            (state.timefoldStack + TimefoldEntry(section, route, timestampMs))
                .takeLast(MAX_TIMEFOLD_ENTRIES)
        }
        return state.copy(
            activeSection = if (route == RikoDestination.Settings.route) state.activeSection else section,
            lastRouteBySection = updatedMap,
            timefoldStack = updatedStack
        )
    }

    /**
     * Switches section; if the user taps the already-active section while deep
     * inside a detail screen, resets that section back to its root route.
     */
    fun onSectionSelected(
        state: ContinuumNavHistoryState,
        targetSection: ContinuumSection,
        timestampMs: Long = System.currentTimeMillis()
    ): Pair<ContinuumNavHistoryState, String> {
        val targetRoute = if (state.activeSection == targetSection) {
            targetSection.rootRoute
        } else {
            state.targetRouteFor(targetSection)
        }
        val updatedMap = state.lastRouteBySection.toMutableMap().apply {
            put(targetSection, targetRoute)
        }
        val next = state.copy(
            activeSection = targetSection,
            lastRouteBySection = updatedMap,
            timefoldStack = (state.timefoldStack + TimefoldEntry(targetSection, targetRoute, timestampMs))
                .takeLast(MAX_TIMEFOLD_ENTRIES)
        )
        return next to targetRoute
    }

    /**
     * Updates the selected sub-tab inside Library Hub (0 = Playlists, 1 = Favorites).
     */
    fun onLibrarySubTabSelected(
        state: ContinuumNavHistoryState,
        subTab: Int
    ): ContinuumNavHistoryState = state.copy(librarySubTab = subTab.coerceIn(0, 1))
}
