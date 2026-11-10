/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B3 — Collection Engine
 *
 * File 2 / 14 — EngineInput.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * The pure-Kotlin input shape the engine consumes. The engine NEVER
 * touches Room, MediaStore, ContentResolver, or any Android data
 * source. Instead, the host (ViewModel / Repository) maps its data
 * into [EngineInput] and hands it to the engine.
 *
 * This separation is what makes the engine unit-testable on the JVM
 * without robolectric — same win as the B2 reducer.
 *
 * DESIGN
 * ------
 * Each "Ref" data class is a lightweight, serializable-shaped view
 * of a domain entity. The engine reads these refs and produces
 * [com.rikodev.music.android.presentation.library.runtime.CardSnapshot]s.
 *
 * The host is responsible for:
 *   1. Querying Room / MediaStore
 *   2. Mapping entities → Refs (this file)
 *   3. Calling engine.generate(input)
 *   4. Pushing the engine's output into CollectionRuntimeState
 *
 * RULES ENFORCED HERE
 * -------------------
 * #31  Runtime Owns Truth     — engine output IS truth; input is just a view
 * #44  Stable Key Never Changes — every ref has a stable string id
 * #46  Runtime Never Creates Identity — engine never invents ids; it
 *                                        only reads ids from input refs
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Android imports. No Room annotations. No Coroutine. No Flow.
 * Pure data classes only.
 *
 * © RIKO Experience OS — Engine Tier
 */

package com.rikodev.music.android.presentation.library.engine

/**
 * A lightweight reference to a Song, sufficient for the engine to
 * score and rank without touching the database.
 *
 * RULE #44 — [songId] is a stable string. The host MUST derive it
 * from the underlying MediaStore/Room id, never from a list index.
 */
data class SongRef(
    /** Stable song id — derived from MediaStore / Room. Rule #44. */
    val songId: String,
    /** Stable album id — empty string if unknown. */
    val albumId: String,
    /** Stable artist id — empty string if unknown. */
    val artistId: String,
    /** Display title. */
    val title: String,
    /** Display artist name. */
    val artistName: String,
    /** Display album name. */
    val albumName: String,
    /** Track duration in milliseconds. */
    val durationMs: Long,
    /** Genre tag — empty string if unknown. */
    val genre: String,
    /** Year of release, or 0 if unknown. */
    val year: Int,
    /** True if the file is marked as instrumental. */
    val isInstrumental: Boolean,
    /** Wall-clock epoch ms when the song was added to the library. */
    val dateAddedMs: Long,
    /** Wall-clock epoch ms when the song was last played, or 0. */
    val lastPlayedAtMs: Long,
    /** Total play count (lifetime). */
    val playCount: Int,
    /** Total skip count (lifetime). */
    val skipCount: Int,
    /** Average completion ratio (0.0 → 1.0). 1.0 = always finished. */
    val averageCompletion: Float,
    /** True if the user marked this song as favourite. */
    val isFavorite: Boolean,
    /** Wall-clock epoch ms when the song was favourited, or 0. */
    val favoritedAtMs: Long,
    /** Optional mood tag — empty string if not annotated. */
    val moodTag: String,
    /** Optional BPM, or 0 if not analyzed. */
    val bpm: Int,
) {
    val hasBeenPlayed: Boolean get() = playCount > 0
    val hasBeenSkippedRecently: Boolean get() = skipCount > 0 && lastPlayedAtMs > 0
    val completionPercent: Int get() = (averageCompletion * 100).toInt().coerceIn(0, 100)
}

/**
 * A lightweight reference to an Album.
 */
data class AlbumRef(
    /** Stable album id. Rule #44. */
    val albumId: String,
    /** Stable artist id — empty if compilation. */
    val artistId: String,
    /** Display title. */
    val title: String,
    /** Display artist name. */
    val artistName: String,
    /** Number of songs in the album. */
    val songCount: Int,
    /** Total play count across all songs in the album. */
    val totalPlayCount: Int,
    /** Wall-clock epoch ms when any song in the album was last played. */
    val lastPlayedAtMs: Long,
    /** Wall-clock epoch ms when the album was added. */
    val dateAddedMs: Long,
    /** Year of release, or 0. */
    val year: Int,
    /** Genre tag — empty if unknown. */
    val genre: String,
    /** True if the user favourited the album. */
    val isFavorite: Boolean,
    /** List of song ids in this album. */
    val songIds: List<String>,
) {
    val hasBeenPlayed: Boolean get() = totalPlayCount > 0
    val averagePlayPerSong: Float
        get() = if (songCount == 0) 0f else totalPlayCount.toFloat() / songCount
}

/**
 * A lightweight reference to an Artist.
 */
data class ArtistRef(
    /** Stable artist id. Rule #44. */
    val artistId: String,
    /** Display name. */
    val name: String,
    /** Number of songs by this artist. */
    val songCount: Int,
    /** Number of albums by this artist. */
    val albumCount: Int,
    /** Total play count across all songs. */
    val totalPlayCount: Int,
    /** Wall-clock epoch ms when any song by this artist was last played. */
    val lastPlayedAtMs: Long,
    /** True if the user favourited the artist. */
    val isFavorite: Boolean,
    /** List of album ids by this artist. */
    val albumIds: List<String>,
) {
    val hasBeenPlayed: Boolean get() = totalPlayCount > 0
}

/**
 * A lightweight reference to a Playlist.
 */
data class PlaylistRef(
    /** Stable playlist id. Rule #44. */
    val playlistId: String,
    /** Display name. */
    val name: String,
    /** Playlist type — "user", "system_recent", "system_liked", etc. */
    val type: String,
    /** Number of songs in the playlist. */
    val songCount: Int,
    /** Wall-clock epoch ms when the playlist was created. */
    val createdAtMs: Long,
    /** Wall-clock epoch ms when the playlist was last modified. */
    val modifiedAtMs: Long,
    /** Wall-clock epoch ms when the playlist was last played, or 0. */
    val lastPlayedAtMs: Long,
    /** List of song ids in the playlist, in order. */
    val songIds: List<String>,
    /** True if the user pinned this playlist. */
    val isPinned: Boolean,
) {
    val hasBeenPlayed: Boolean get() = lastPlayedAtMs > 0
}

/**
 * A single play event in the user's listening history.
 *
 * Used by the engine to compute recency, frequency, and completion
 * signals. The host decides how many events to provide — typically
 * the last 200-500 plays.
 */
data class PlayEvent(
    /** Stable song id that was played. Rule #44. */
    val songId: String,
    /** Wall-clock epoch ms when the play event occurred. */
    val playedAtMs: Long,
    /** How many milliseconds of the song were actually played. */
    val listenedMs: Long,
    /** Total duration of the song at the time of play. */
    val durationMs: Long,
    /** True if the user manually skipped (vs. natural end). */
    val wasSkipped: Boolean,
    /** True if the play completed naturally (listenedMs ≥ 90% of duration). */
    val wasCompleted: Boolean,
    /** Optional session id — groups plays in the same listening session. */
    val sessionId: String,
) {
    /** Completion ratio (0.0 → 1.0). */
    val completionRatio: Float
        get() = if (durationMs == 0L) 0f else (listenedMs.toFloat() / durationMs).coerceIn(0f, 1f)
}

/**
 * Context about the current listening session.
 *
 * A "session" is a continuous listening period — e.g. user opened
 * the app, played 5 songs, closed the app. The host decides when
 * a session starts/ends.
 */
data class SessionContext(
    /** Stable session id. Rule #44. */
    val sessionId: String,
    /** Wall-clock epoch ms when the session started. */
    val startedAtMs: Long,
    /** Wall-clock epoch ms when the session ended, or 0 if ongoing. */
    val endedAtMs: Long,
    /** Song ids played in this session, in order. */
    val playedSongIds: List<String>,
    /** Average completion ratio across all plays in this session. */
    val averageCompletion: Float,
    /** True if the session was mostly skips (>50% skipped). */
    val wasSkipHeavy: Boolean,
) {
    val isOngoing: Boolean get() = endedAtMs == 0L
    val durationMs: Long get() = (if (endedAtMs == 0L) 0L else endedAtMs) - startedAtMs
    val playCount: Int get() = playedSongIds.size
}

/**
 * The complete input to a single engine generation cycle.
 *
 * Built by the host (ViewModel / Repository) and passed to
 * [CollectionEngine.generate].
 *
 * RULE #46 — Engine never creates identity. Every id the engine
 * references comes from THIS input. If a song isn't in [songs],
 * the engine cannot surface it.
 */
data class EngineInput(
    /** All songs in the library. May be empty on cold start. */
    val songs: List<SongRef>,
    /** All albums in the library. */
    val albums: List<AlbumRef>,
    /** All artists in the library. */
    val artists: List<ArtistRef>,
    /** All playlists (user + system). */
    val playlists: List<PlaylistRef>,
    /** Recent play events, oldest first. */
    val playHistory: List<PlayEvent>,
    /** Current and recent sessions. */
    val sessions: List<SessionContext>,
    /** Set of favourited song ids. */
    val favoriteSongIds: Set<String>,
    /** Set of pinned collection stableKeys (user pinned). */
    val pinnedCollectionKeys: Set<String>,
    /** Wall-clock epoch ms — the "current time" for this cycle. */
    val nowMs: Long,
    /** True if this is the first generation after app install. */
    val coldStart: Boolean,
    /** True if the user has granted media permission. */
    val permissionsGranted: Boolean,
    /** Optional mood hint from the host (e.g. "focus", "evening"). */
    val moodHint: String?,
    /** Optional time-of-day bucket — "morning", "afternoon", "evening", "night". */
    val timeOfDayBucket: String?,
) {

    val songCount: Int get() = songs.size
    val albumCount: Int get() = albums.size
    val artistCount: Int get() = artists.size
    val playlistCount: Int get() = playlists.size
    val totalLibrarySize: Int get() = songCount + albumCount + artistCount + playlistCount

    /** True if the library is effectively empty for generation purposes. */
    val isEmpty: Boolean get() = songs.isEmpty() && albums.isEmpty() && playlists.isEmpty()

    /** Look up a song by stable id. */
    fun songById(songId: String): SongRef? = songs.firstOrNull { it.songId == songId }

    /** Look up an album by stable id. */
    fun albumById(albumId: String): AlbumRef? = albums.firstOrNull { it.albumId == albumId }

    /** Look up an artist by stable id. */
    fun artistById(artistId: String): ArtistRef? = artists.firstOrNull { it.artistId == artistId }

    /** Look up a playlist by stable id. */
    fun playlistById(playlistId: String): PlaylistRef? = playlists.firstOrNull { it.playlistId == playlistId }

    /** All play events for a given song, oldest first. */
    fun historyFor(songId: String): List<PlayEvent> =
        playHistory.filter { it.songId == songId }

    /** Most recent play event for a song, or null. */
    fun lastPlayFor(songId: String): PlayEvent? =
        playHistory.lastOrNull { it.songId == songId }

    companion object {

        /** Empty input — used in tests and cold-start fallbacks. */
        val EMPTY = EngineInput(
            songs = emptyList(),
            albums = emptyList(),
            artists = emptyList(),
            playlists = emptyList(),
            playHistory = emptyList(),
            sessions = emptyList(),
            favoriteSongIds = emptySet(),
            pinnedCollectionKeys = emptySet(),
            nowMs = 0L,
            coldStart = true,
            permissionsGranted = false,
            moodHint = null,
            timeOfDayBucket = null,
        )
    }
}
