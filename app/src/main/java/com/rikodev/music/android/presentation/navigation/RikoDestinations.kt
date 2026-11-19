package com.rikodev.music.android.presentation.navigation

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — NAVIGATION DESTINATIONS
// Sealed routes for type-safe navigation across the CONTINUUM sections
// (Listen, Explore, Library) plus sub-destinations (detail views, search,
// settings, onboarding, now playing handoff).
// ─────────────────────────────────────────────────────────────────────────────

sealed class RikoDestination(val route: String) {

    // ── CONTINUUM Primary Section Roots ──────────────────────────────────────
    data object AllSongs : RikoDestination("all_songs")        // Listen section (Home)
    data object MyMusic : RikoDestination("my_music")          // Explore section (All Songs/Albums/Artists/Folders)
    data object LibraryHub : RikoDestination("library_hub")    // Library section (Playlists + Favorites)

    // ── Direct & Pushed Destinations ─────────────────────────────────────────
    data object Favorites : RikoDestination("favorites")       // Direct favorites route (preserved)
    data object Playlists : RikoDestination("playlists")       // Direct playlists route (preserved)
    data object Settings : RikoDestination("settings")         // Settings screen (accessible from headers)
    data object NowPlaying : RikoDestination("now_playing")
    data object Search : RikoDestination("search")
    data object Onboarding : RikoDestination("onboarding")     // first-run (spec 3.1)

    // ── Plan 5 — Collection detail routes ────────────────────────────────────
    data object AlbumDetail : RikoDestination("album_detail/{albumId}") {
        fun createRoute(albumId: Long) = "album_detail/$albumId"
        const val ARG_ALBUM_ID = "albumId"
    }

    data object ArtistDetail : RikoDestination("artist_detail/{artistId}") {
        fun createRoute(artistId: Long) = "artist_detail/$artistId"
        const val ARG_ARTIST_ID = "artistId"
    }

    data object FolderDetail : RikoDestination("folder_detail/{folderPath}") {
        fun createRoute(folderPath: String) = "folder_detail/${android.net.Uri.encode(folderPath)}"
        const val ARG_FOLDER_PATH = "folderPath"
    }

    // ── Sub-destinations ──────────────────────────────────────────────────────
    data object PlaylistDetail : RikoDestination("playlist_detail/{playlistId}") {
        fun createRoute(playlistId: Long) = "playlist_detail/$playlistId"
        const val ARG_PLAYLIST_ID = "playlistId"
    }

    companion object {
        /** CONTINUUM 3-section bottom-nav roots, in order: Listen, Explore, Library. */
        val bottomNavItems = listOf(AllSongs, MyMusic, LibraryHub)
    }
}
