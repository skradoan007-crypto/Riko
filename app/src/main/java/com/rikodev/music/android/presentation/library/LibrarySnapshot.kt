package com.rikodev.music.android.presentation.library

import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.Animatable

// ═══════════════════════════════════════════════════════════════════════════
// RIKO MUSIC — MILESTONE 2: LIBRARY RUNTIME — CARD SNAPSHOTS
//
// Phase A, File 2: LibrarySnapshot.kt
//
// Rule #28: Every card owns exactly ONE snapshot.
// Rule #33: Cards never fetch. Only snapshot.
//
// Each snapshot is a PRE-COMPUTED, IMMUTABLE data package.
// The card renderer reads this and renders. Zero computation in UI.
// Zero Repository access. Zero DAO. Zero Room query.
//
// Architecture:
//
//   Repository (Room DAO)
//       │
//       ▼
//   LibraryMapper (pure)
//       │
//       ▼
//   Snapshot (AlbumSnapshot, ArtistSnapshot, etc.)
//       │
//       ▼
//   Card Renderer (reads snapshot only)
//
// ⭐ RULE #28 ⭐
//   AlbumCard reads AlbumSnapshot. NOTHING ELSE.
//   ArtistCard reads ArtistSnapshot. NOTHING ELSE.
//   FolderCard reads FolderSnapshot. NOTHING ELSE.
//   Every card = ONE snapshot. ONE owner.
// ═══════════════════════════════════════════════════════════════════════════

// ─────────────────────────────────────────────────────────────────────────────
// ALBUM SNAPSHOT
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Pre-computed snapshot for an Album card.
 *
 * Contains everything an AlbumCard renderer needs:
 * metadata, artwork reference, song count, and display properties.
 *
 * Source: Repository → AlbumWithSongs → mapped by LibraryMapper.
 * Card NEVER touches Repository or DAO.
 *
 * Rule #32: Collection Has Identity.
 * Album has purpose, presentation context, energy hint.
 */
data class AlbumSnapshot(
    /** Album unique ID. */
    val id: Long,

    /** Album name. */
    val name: String,

    /** Artist name for this album. */
    val artistName: String,

    /** Number of songs in this album. */
    val songCount: Int,

    /** Total duration of all songs in this album (ms). */
    val totalDuration: Long,

    /** Year of release (0 if unknown). */
    val year: Int,

    /** Album art content URI string. Null if no artwork. */
    val artUri: String? = null,

    /** Primary color extracted from album art. */
    val artColor: Color = Color(0xFF1A1A2E),

    /** Dominant luminance of album art (for text contrast). */
    val artLuminance: Float = 0.15f,

    /** Whether this album is in favorites. */
    val isFavorite: Boolean = false,

    /** Energy hint based on album's audio characteristics. */
    val energyHint: EnergyHint = EnergyHint.UNKNOWN
) {
    companion object {
        /** Placeholder when album data is loading. */
        val PLACEHOLDER = AlbumSnapshot(
            id = -1L,
            name = "",
            artistName = "",
            songCount = 0,
            totalDuration = 0L,
            year = 0
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ARTIST SNAPSHOT
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Pre-computed snapshot for an Artist card.
 *
 * Source: Repository → ArtistWithSongs → mapped by LibraryMapper.
 */
data class ArtistSnapshot(
    /** Artist unique ID. */
    val id: Long,

    /** Artist name. */
    val name: String,

    /** Number of songs by this artist. */
    val songCount: Int,

    /** Number of albums by this artist. */
    val albumCount: Int,

    /** Artist image URI (if available). Null uses initial letter. */
    val imageUri: String? = null,

    /** Primary color associated with this artist (from most played album). */
    val accentColor: Color = Color(0xFF19B7FF),

    /** Whether this artist is in favorites. */
    val isFavorite: Boolean = false
) {
    /** Initial letter for avatar fallback when no image. */
    val initialLetter: String
        get() = name.take(1).uppercase()

    companion object {
        val PLACEHOLDER = ArtistSnapshot(
            id = -1L,
            name = "",
            songCount = 0,
            albumCount = 0
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FOLDER SNAPSHOT
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Pre-computed snapshot for a Folder card.
 *
 * Source: Repository → FolderWithSongs → mapped by LibraryMapper.
 */
data class FolderSnapshot(
    /** Folder unique path (used as ID). */
    val path: String,

    /** Folder name (last segment of path). */
    val name: String,

    /** Number of songs in this folder. */
    val songCount: Int,

    /** Total size of all songs in this folder (bytes). */
    val totalSize: Long,

    /** Deepest nested folder level (1 = root-level). */
    val depth: Int = 1,

    /** Whether this folder contains sub-folders. */
    val hasSubFolders: Boolean = false,

    /** Formatted total size string. e.g. "245.3 MB" */
    val totalSizeFormatted: String = ""
) {
    companion object {
        val PLACEHOLDER = FolderSnapshot(
            path = "",
            name = "",
            songCount = 0,
            totalSize = 0L
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PLAYLIST SNAPSHOT
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Pre-computed snapshot for a Playlist card.
 *
 * Source: Repository → Playlist → mapped by LibraryMapper.
 */
data class PlaylistSnapshot(
    /** Playlist unique ID. */
    val id: Long,

    /** Playlist name. */
    val name: String,

    /** Playlist description. */
    val description: String,

    /** Number of songs in this playlist. */
    val songCount: Int,

    /** Total duration of all songs (ms). */
    val totalDuration: Long,

    /** Custom cover URI. Null uses song artwork mosaic. */
    val coverUri: String? = null,

    /** Whether this is the built-in Favorites playlist. */
    val isFavorites: Boolean = false,

    /** Creation timestamp (epoch ms). */
    val createdAt: Long,

    /** Last modified timestamp (epoch ms). */
    val modifiedAt: Long
) {
    companion object {
        val PLACEHOLDER = PlaylistSnapshot(
            id = -1L,
            name = "",
            description = "",
            songCount = 0,
            totalDuration = 0L,
            createdAt = 0L,
            modifiedAt = 0L
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SONG SNAPSHOT — For list/grid views
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Pre-computed snapshot for a Song item (in list or grid).
 *
 * This is the most common snapshot — used in song lists,
 * collection contents, search results, etc.
 *
 * Source: Repository → Song → mapped by LibraryMapper.
 */
data class SongSnapshot(
    /** Song unique ID. */
    val id: Long,

    /** Song title. */
    val title: String,

    /** Artist name. */
    val artist: String,

    /** Album name. */
    val album: String,

    /** Album ID (for navigation to album detail). */
    val albumId: Long,

    /** Duration in milliseconds. */
    val duration: Long,

    /** Formatted duration string. e.g. "3:45" */
    val durationFormatted: String,

    /** Album art URI. Null if no artwork. */
    val artUri: String? = null,

    /** Primary color from album art. */
    val artColor: Color = Color(0xFF1A1A2E),

    /** Whether this song is in favorites. */
    val isFavorite: Boolean = false,

    /** Date added (epoch ms). */
    val dateAdded: Long,

    /** Listening frequency rank (1 = most played). 0 = never played. */
    val playCount: Int = 0,

    /** Energy hint based on audio characteristics. */
    val energyHint: EnergyHint = EnergyHint.UNKNOWN
) {
    companion object {
        val PLACEHOLDER = SongSnapshot(
            id = -1L,
            title = "",
            artist = "",
            album = "",
            albumId = -1L,
            duration = 0L,
            durationFormatted = "0:00",
            dateAdded = 0L
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COLLECTION SNAPSHOT — Smart Collections (Rule #32)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Pre-computed snapshot for a Smart Collection card.
 *
 * Rule #32: Collection Has Identity.
 * Every collection has Purpose, Emotion, Priority, Visibility, Presentation.
 *
 * Examples of Smart Collections:
 *   - Recently Played
 *   - Morning Energy
 *   - Night Calm
 *   - Favorites
 *   - High Energy
 *   - Never Played
 *   - Hidden Gems
 *   - New Imports
 *
 * Source: Collection Engine → mapped by LibraryMapper.
 * UI NEVER computes what goes into a collection.
 */
data class CollectionSnapshot(
    /** Collection unique ID. */
    val id: String,

    /** Collection display name. e.g. "Morning Energy" */
    val name: String,

    /** Collection type. Determines icon, color, and presentation. */
    val type: CollectionType,

    /** Contextual subtitle. e.g. "12 songs from the last 3 days" (Rule #34). */
    val subtitle: String = "",

    /** Number of items in this collection. */
    val itemCount: Int,

    /** Priority for display ordering (lower = higher priority). */
    val priority: Int,

    /** Whether this collection is visible to the user. */
    val isVisible: Boolean = true,

    /** Cover art URI (first item's artwork or custom). Null for generated. */
    val coverUri: String? = null,

    /** Primary color for this collection's presentation. */
    val accentColor: Color = Color(0xFF19B7FF),

    /** Energy hint for this collection's typical content. */
    val energyHint: EnergyHint = EnergyHint.UNKNOWN,

    /** Last updated timestamp (epoch ms). */
    val lastUpdated: Long = 0L
) {
    companion object {
        val PLACEHOLDER = CollectionSnapshot(
            id = "",
            name = "",
            type = CollectionType.RECENTLY_PLAYED,
            itemCount = 0,
            priority = Int.MAX_VALUE
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SUPPORTING TYPES
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Smart Collection types.
 *
 * Each type has a defined purpose and emotion (Rule #32).
 * Collection Engine decides which collections exist and their contents.
 */
enum class CollectionType {
    /** Songs played in the recent past. */
    RECENTLY_PLAYED,

    /** Songs added recently to the library. */
    RECENTLY_ADDED,

    /** Most played songs. */
    MOST_PLAYED,

    /** User's favorite songs. */
    FAVORITES,

    /** High energy songs (upbeat, fast tempo). */
    HIGH_ENERGY,

    /** Calm/relaxed songs (slow tempo, ambient). */
    CALM,

    /** Songs never played by the user. */
    NEVER_PLAYED,

    /** Songs played less than 3 times (hidden gems). */
    HIDDEN_GEMS,

    /** Newly imported songs (just added to device). */
    NEW_IMPORTS,

    /** Custom user-created collection. */
    CUSTOM,

    /** Songs from a specific time period. */
    TIME_BASED,

    /** Songs grouped by mood/emotion. */
    MOOD_BASED,

    /** Songs grouped by genre. */
    GENRE_BASED
}

/**
 * Energy hint for content presentation.
 *
 * Used by Collection Engine and Mapper to tag content with energy level.
 * Renderers use this for visual presentation (color warmth, wave intensity, etc.).
 */
enum class EnergyHint {
    /** Unknown or not yet analyzed. */
    UNKNOWN,

    /** Very calm — ambient, meditation, classical. */
    VERY_CALM,

    /** Calm — ballads, acoustic, slow songs. */
    CALM,

    /** Moderate — pop, rock, normal tempo. */
    MODERATE,

    /** Energetic — dance, electronic, upbeat. */
    ENERGETIC,

    /** Very energetic — metal, hard rock, intense. */
    VERY_ENERGETIC
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION SNAPSHOT — Organized content sections for display
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A section of content in Library.
 *
 * Sections group related content together. Each section has a sticky header,
 * and contains a list of snapshots of a specific type.
 *
 * Rule #26: Library renders collections, not database rows.
 * Sections are collections, not SQL query results.
 *
 * Source: Collection Engine → organized by sorting logic → this.
 */
data class LibrarySection(
    /** Section unique ID. */
    val id: String,

    /** Section title (sticky header text). */
    val title: String,

    /** Section subtitle (context, Rule #34). */
    val subtitle: String = "",

    /** Type of content in this section. */
    val contentType: SectionContentType,

    /** Snapshots in this section (already sorted by Collection Engine). */
    val albums: List<AlbumSnapshot> = emptyList(),
    val artists: List<ArtistSnapshot> = emptyList(),
    val folders: List<FolderSnapshot> = emptyList(),
    val playlists: List<PlaylistSnapshot> = emptyList(),
    val songs: List<SongSnapshot> = emptyList(),
    val collections: List<CollectionSnapshot> = emptyList(),

    /** Whether this section should be visible. */
    val isVisible: Boolean = true,

    /** Whether this section is currently expanded (collapsible sections). */
    val isExpanded: Boolean = true
) {
    /** Total item count across all content types. */
    val totalCount: Int
        get() = albums.size + artists.size + folders.size +
                 playlists.size + songs.size + collections.size

    enum class SectionContentType {
        COLLECTIONS,
        ALBUMS,
        ARTISTS,
        FOLDERS,
        PLAYLISTS,
        SONGS,
        SMART_COLLECTIONS
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LIBRARY CONTENT — Full organized content snapshot
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Full organized Library content.
 *
 * This is the "database" that the Library renderer reads.
 * It's a pre-organized, pre-sorted, pre-filtered snapshot of all content.
 *
 * Source: Repository → Collection Engine → Smart Sorting → LibraryMapper → this.
 * Rule #31: Runtime Owns Truth. This IS the truth for Library UI.
 */
data class LibraryContentSnapshot(
    /** Ordered sections for display. */
    val sections: List<LibrarySection> = emptyList(),

    /** Quick-access smart collections (top of Library, before sections). */
    val smartCollections: List<CollectionSnapshot> = emptyList(),

    /** Whether content is currently being refreshed. */
    val isRefreshing: Boolean = false
) {
    /** Total content count across all sections. */
    val totalItemCount: Int
        get() = sections.sumOf { it.totalCount } + smartCollections.size

    companion object {
        /** Empty content — used for empty library state. */
        val EMPTY = LibraryContentSnapshot()

        /** Loading placeholder. */
        val LOADING = LibraryContentSnapshot(isRefreshing = true)
    }
}
