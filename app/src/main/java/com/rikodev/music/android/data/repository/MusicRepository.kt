package com.rikodev.music.android.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.rikodev.music.android.data.db.dao.FavoriteDao
import com.rikodev.music.android.domain.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — MUSIC REPOSITORY
// Queries Android MediaStore for all audio files on the device.
// Merges with favorites from Room so every Song knows if it's favorited.
// Returns Flow<List<Song>> so UI stays reactive to MediaStore changes.
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class MusicRepository @Inject constructor(
    private val contentResolver: ContentResolver,
    private val favoriteDao: FavoriteDao
) {
    // MediaStore content URI
    private val audioUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }

    // Columns to query from MediaStore
    private val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.SIZE,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.DATE_ADDED,
        MediaStore.Audio.Media.DATE_MODIFIED,
        MediaStore.Audio.Media.TRACK,
        MediaStore.Audio.Media.YEAR,
        MediaStore.Audio.Media.BITRATE,
        MediaStore.Audio.Media.MIME_TYPE,
    )

    // Only query music files — exclude voice notes, ringtones
    private val selection = "${MediaStore.Audio.Media.IS_MUSIC} = 1 AND ${MediaStore.Audio.Media.DURATION} >= 30000"

    // ── Plan 3.1: ONE shared library cache ──────────────────────────────────
    // Every screen used to launch its own cold MediaStore cursor (Home even
    // ran two). The cache lives on the singleton's own app-lifetime scope —
    // deliberately NOT WhileSubscribed, so it survives screen transitions.
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Unsorted master list; sorted views derive from this in memory. */
    // H5 fix: start as empty list (not null) so `filterNotNull()` consumers
    // emit immediately on subscribe instead of waiting for the first scan.
    // The real MediaStore result replaces this within the debounce window.
    private val songsCache = MutableStateFlow<List<Song>?>(emptyList())

    /** Coalesced refresh signal — a burst of MediaStore events = one rescan. */
    private val refreshSignal = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )

    init {
        repoScope.launch {
            // P0 FIX: `onStart { emit(Unit) }` guarantees the initial scan
            // reaches THIS collector. The old separate
            // `repoScope.launch { refreshSignal.emit(Unit) }` raced with the
            // debounce subscription — SharedFlow has replay=0, so an emit that
            // lands BEFORE collect subscribes is silently DROPPED. That meant
            // on every relaunch (permission already granted → no other trigger)
            // the scan never ran and the library stayed empty forever.
            refreshSignal
                .onStart { emit(Unit) }
                .debounce(300L)
                .collect {
                    // FIX: always set cache, even on failure — prevents infinite
                    // suspension in getAllSongs().first() when query fails.
                    val songs = runCatching { querySongs(selection, null, null) }
                        .getOrDefault(emptyList())
                    songsCache.value = songs
                }
        }
        runCatching {
            contentResolver.registerContentObserver(
                audioUri,
                true,
                object : ContentObserver(null) {
                    override fun onChange(selfChange: Boolean) {
                        refreshSignal.tryEmit(Unit)
                    }
                }
            )
        }
        // NOTE: initial load is emitted via onStart above — NOT via a second
        // coroutine (that was the race).
    }

    /**
     * Force a refresh of the songs cache. Called after permissions are granted.
     */
    fun refresh() {
        refreshSignal.tryEmit(Unit)
    }

    /**
     * Get all songs from MediaStore, merged with favorites state.
     * Plan 3.1: served from the shared cache — ONE cursor scan serves every
     * screen; sorting happens in memory (cheap) instead of re-querying.
     */
    fun getAllSongs(
        sortBy: SortBy = SortBy.TITLE,
        sortOrder: SortOrder = SortOrder.ASCENDING
    ): Flow<List<Song>> = combine(
        songsCache.filterNotNull(),
        favoriteDao.getAllFavoriteSongIds()
    ) { songs, favoriteIds ->
        val favoriteSet = favoriteIds.toSet()
        val cmp: Comparator<Song> = when (sortBy) {
            SortBy.TITLE -> compareBy { it.title }
            SortBy.ARTIST -> compareBy { it.artist }
            SortBy.ALBUM -> compareBy { it.album }
            SortBy.DATE_ADDED -> compareBy { it.dateAdded }
            SortBy.DURATION -> compareBy { it.duration }
            SortBy.SIZE -> compareBy { it.size }
        }
        val sorted = if (sortOrder == SortOrder.DESCENDING) {
            songs.sortedWith(cmp.reversed())
        } else {
            songs.sortedWith(cmp)
        }
        sorted.map { it.copy(isFavorite = it.id in favoriteSet) }
    }.flowOn(Dispatchers.IO)

    /**
     * Get a single song by ID.
     */
    suspend fun getSongById(id: Long): Song? = withContext(Dispatchers.IO) {
        val selection = "${MediaStore.Audio.Media._ID} = ?"
        val selectionArgs = arrayOf(id.toString())
        querySongs(selection, selectionArgs, null).firstOrNull()
    }

    /**
     * Search songs by title, artist, or album.
     */
    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val searchSelection = "${MediaStore.Audio.Media.IS_MUSIC} = 1 AND " +
                "${MediaStore.Audio.Media.DURATION} >= 30000 AND (" +
                "${MediaStore.Audio.Media.TITLE} LIKE ? OR " +
                "${MediaStore.Audio.Media.ARTIST} LIKE ? OR " +
                "${MediaStore.Audio.Media.ALBUM} LIKE ?)"
        val likeQuery = "%$query%"
        querySongs(searchSelection, arrayOf(likeQuery, likeQuery, likeQuery), null)
    }

    /**
     * Get songs by their IDs (for playlist playback).
     */
    suspend fun getSongsByIds(ids: List<Long>): List<Song> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext emptyList()
        val placeholders = ids.joinToString(",") { "?" }
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} = 1 AND ${MediaStore.Audio.Media._ID} IN ($placeholders)"
        val args = ids.map { it.toString() }.toTypedArray()
        val songs = querySongs(selection, args, null)
        // Return in same order as requested IDs
        val songMap = songs.associateBy { it.id }
        ids.mapNotNull { songMap[it] }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun querySongs(
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): List<Song> {
        val songs = mutableListOf<Song>()

        contentResolver.query(
            audioUri,
            projection,
            selection,
            selectionArgs,
            sortOrder ?: "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use

            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val dateModifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val bitrateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BITRATE)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

            do {
                try {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(audioUri, id)

                    songs.add(
                        Song(
                            id = id,
                            title = cursor.getString(titleCol) ?: "Unknown",
                            artist = cursor.getString(artistCol) ?: "Unknown Artist",
                            album = cursor.getString(albumCol) ?: "Unknown Album",
                            albumId = cursor.getLong(albumIdCol),
                            duration = cursor.getLong(durationCol),
                            size = cursor.getLong(sizeCol),
                            path = cursor.getString(dataCol) ?: "",
                            uri = contentUri,
                            dateAdded = cursor.getLong(dateAddedCol) * 1000,
                            dateModified = cursor.getLong(dateModifiedCol) * 1000,
                            trackNumber = cursor.getInt(trackCol),
                            year = cursor.getInt(yearCol),
                            bitrate = cursor.getInt(bitrateCol) / 1000,
                            sampleRate = 44100,  // MediaStore doesn't expose this directly
                            mimeType = cursor.getString(mimeCol) ?: "audio/mpeg"
                        )
                    )
                } catch (e: Exception) {
                    // Skip malformed entries
                }
            } while (cursor.moveToNext())
        }

        return songs
    }

    /**
     * Get album art URI for a given album ID.
     * Android 10+ uses MediaStore.Audio.Albums, older uses ContentUris.
     */
    fun getAlbumArtUri(albumId: Long): Uri {
        return ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"),
            albumId
        )
    }
}

enum class SortBy(val column: String) {
    TITLE(MediaStore.Audio.Media.TITLE),
    ARTIST(MediaStore.Audio.Media.ARTIST),
    ALBUM(MediaStore.Audio.Media.ALBUM),
    DATE_ADDED(MediaStore.Audio.Media.DATE_ADDED),
    DURATION(MediaStore.Audio.Media.DURATION),
    SIZE(MediaStore.Audio.Media.SIZE)
}

enum class SortOrder(val direction: String) {
    ASCENDING("ASC"),
    DESCENDING("DESC")
}
