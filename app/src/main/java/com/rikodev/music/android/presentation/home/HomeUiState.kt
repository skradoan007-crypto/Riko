package com.rikodev.music.android.presentation.home

import com.rikodev.music.android.domain.model.Playlist
import com.rikodev.music.android.domain.model.Song

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — HOME UI STATE (PLAN 3)
//
// Data-driven sections (spec §42): a section renders ONLY when its data
// exists — no fake placeholders, no blank filler. Derived Album/Artist data
// is computed client-side from the existing songs list — NO Room/repo changes.
// ─────────────────────────────────────────────────────────────────────────────

/** Album shelf item — derived by grouping songs on albumId. */
data class AlbumUi(
    val id: Long,
    val title: String,
    val artist: String,
    val artworkModel: Any?,
    val songCount: Int,
    val songs: List<Song>
)

/** Hero data for the featured banner — derived in HomeScreen from playback/history.
 *  (Moved from HomeHeroCard.kt when that dormant file was retired, D-9.) */
data class HomeHeroData(
    val sectionLabel: String,
    val title: String,
    val artist: String,
    val artworkModel: Any?,
    /** 0f..1f — hidden when null (spec §12: simple indicator, NOT the waveform). */
    val progress: Float? = null
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val error: Boolean = false,
    /** All songs, A–Z — the "All Songs" list section at the bottom of Home. */
    val songs: List<Song> = emptyList(),
    /** Real play history (play_history table) — newest first, deduped. */
    val recentlyPlayed: List<Song> = emptyList(),
    /** Real MediaStore DATE_ADDED — newest first. */
    val recentlyAdded: List<Song> = emptyList(),
    val favoriteTracks: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val totalDurationFormatted: String = "0:00"
) {
    /** Priority order per spec §47 — sections disappear when data is absent. */
}