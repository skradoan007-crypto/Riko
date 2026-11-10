package com.rikodev.music.android.presentation.navigation

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — NAVIGATION DESTINATIONS
// Sealed routes for type-safe navigation across the 5 core screens
// plus their sub-destinations (queue, EQ sheet, playlist detail, etc).
// ─────────────────────────────────────────────────────────────────────────────

sealed class RikoDestination(val route: String) {

    // ── Bottom Nav Top-Level Destinations (SM spec Section 4: 4 tabs) ────────
    data object AllSongs : RikoDestination("all_songs")        // Home tab
    data object Favorites : RikoDestination("favorites")       // Favourites tab
    data object MyMusic : RikoDestination("my_music")         // Library tab root (D12)
    data object Settings : RikoDestination("settings")         // Settings tab

    // ── Pushed destinations (no bottom nav) ──────────────────────────────────
    data object Playlists : RikoDestination("playlists")       // sub-dest of Library (D12)
    data object NowPlaying : RikoDestination("now_playing")
    data object Search : RikoDestination("search")
    data object Onboarding : RikoDestination("onboarding")      // first-run (spec 3.1)

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
        /** SM spec Section 4 — the 4 bottom-nav tabs, in order. */
        val bottomNavItems = listOf(AllSongs, Favorites, MyMusic, Settings)
    }
}
