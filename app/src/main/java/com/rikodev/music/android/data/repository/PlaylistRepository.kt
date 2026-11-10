package com.rikodev.music.android.data.repository

import android.net.Uri
import com.rikodev.music.android.data.db.dao.FavoriteDao
import com.rikodev.music.android.data.db.dao.PlaylistDao
import com.rikodev.music.android.data.db.entity.PlaylistEntity
import com.rikodev.music.android.data.db.entity.PlaylistSongEntity
import com.rikodev.music.android.domain.model.Playlist
import com.rikodev.music.android.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — PLAYLIST REPOSITORY
//
// Manages user playlists, backed by Room. The "Favorites" playlist is special:
//   - Auto-created on first launch
//   - Cannot be deleted (enforced both at DAO level and UI level)
//   - Cannot be renamed (name is locked to "Favorites")
//   - Its song membership mirrors the FavoriteDao toggle state
//
// All other playlists are fully user-editable: name, cover thumbnail
// (picked from storage), and song membership (add/remove/reorder).
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val favoriteDao: FavoriteDao,
    private val musicRepository: MusicRepository
) {

    /**
     * Ensure the built-in Favorites playlist exists. Call once at app startup.
     */
    suspend fun ensureFavoritesPlaylistExists() = withContext(Dispatchers.IO) {
        val existing = playlistDao.getFavoritesPlaylist()
        if (existing == null) {
            playlistDao.insertPlaylist(
                PlaylistEntity(
                    name = "Favorites",
                    description = "Songs you love",
                    isFavorites = true
                )
            )
        }
    }

    /**
     * All playlists, Favorites always pinned first (handled by DAO query ordering).
     */
    fun getAllPlaylists(): Flow<List<Playlist>> = combine(
        playlistDao.getAllPlaylists(),
        favoriteDao.getAllFavoriteSongIds()
    ) { entities, favoriteIds ->
        entities.map { entity ->
            val songIds = if (entity.isFavorites) {
                favoriteIds
            } else {
                playlistDao.getSongIdsForPlaylistSync(entity.id)
            }
            entity.toDomain(songIds)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Get a single playlist with its live song list (full Song objects, not just IDs).
     */
    fun getPlaylistWithSongs(playlistId: Long): Flow<Pair<Playlist, List<Song>>> {
        return playlistSongIdsFlow(playlistId)
            .flatMapLatest { (entity, songIds) ->
                musicRepository.getAllSongs().map { allSongs ->
                    val songMap = allSongs.associateBy { it.id }
                    val orderedSongs = songIds.mapNotNull { songMap[it] }
                    entity.toDomain(songIds) to orderedSongs
                }
            }
            .flowOn(Dispatchers.IO)
    }

    private fun playlistSongIdsFlow(playlistId: Long): Flow<Pair<PlaylistEntity, List<Long>>> {
        return combine(
            playlistDao.getAllPlaylists(),
            favoriteDao.getAllFavoriteSongIds()
        ) { playlists, favoriteIds ->
            val entity = playlists.find { it.id == playlistId }
                ?: return@combine null
            val songIds = if (entity.isFavorites) {
                favoriteIds
            } else {
                playlistDao.getSongIdsForPlaylistSync(playlistId)
            }
            entity to songIds
        }.distinctUntilChanged { old, new -> old == new }
            .map { it ?: throw IllegalStateException("Playlist not found") }
    }

    /**
     * Create a brand new user playlist.
     */
    suspend fun createPlaylist(name: String, coverUri: Uri? = null): Long = withContext(Dispatchers.IO) {
        playlistDao.insertPlaylist(
            PlaylistEntity(
                name = name.trim().ifBlank { "New Playlist" },
                coverUri = coverUri?.toString()
            )
        )
    }

    /**
     * Rename a playlist. Blocked for the Favorites playlist.
     */
    suspend fun renamePlaylist(playlistId: Long, newName: String) = withContext(Dispatchers.IO) {
        val playlist = playlistDao.getPlaylistById(playlistId) ?: return@withContext
        if (playlist.isFavorites) return@withContext  // Favorites name is locked
        playlistDao.updatePlaylist(
            playlist.copy(name = newName.trim(), modifiedAt = System.currentTimeMillis())
        )
    }

    /**
     * Set a custom cover thumbnail (image picked from device storage).
     */
    suspend fun setPlaylistCover(playlistId: Long, uri: Uri?) = withContext(Dispatchers.IO) {
        playlistDao.updateCoverUri(playlistId, uri?.toString())
    }

    /**
     * Delete a playlist. Blocked for the Favorites playlist (also enforced in DAO).
     */
    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        val playlist = playlistDao.getPlaylistById(playlistId) ?: return@withContext
        if (playlist.isFavorites) return@withContext
        playlistDao.deletePlaylist(playlistId)
    }

    /**
     * Add a song to a playlist. For Favorites, this toggles the favorite flag.
     */
    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) = withContext(Dispatchers.IO) {
        val playlist = playlistDao.getPlaylistById(playlistId) ?: return@withContext
        if (playlist.isFavorites) {
            favoriteDao.addFavorite(com.rikodev.music.android.data.db.entity.FavoriteEntity(songId))
        } else {
            val currentCount = playlistDao.getSongCount(playlistId)
            playlistDao.addSongToPlaylist(
                PlaylistSongEntity(playlistId = playlistId, songId = songId, position = currentCount)
            )
            playlistDao.updateModifiedAt(playlistId)
        }
    }

    suspend fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) = withContext(Dispatchers.IO) {
        songIds.forEach { addSongToPlaylist(playlistId, it) }
    }

    /**
     * Remove a song from a playlist. For Favorites, this un-favorites it.
     */
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) = withContext(Dispatchers.IO) {
        val playlist = playlistDao.getPlaylistById(playlistId) ?: return@withContext
        if (playlist.isFavorites) {
            favoriteDao.removeFavorite(songId)
        } else {
            playlistDao.removeSongFromPlaylist(playlistId, songId)
            playlistDao.updateModifiedAt(playlistId)
        }
    }

    /**
     * Reorder songs within a playlist (drag-and-drop in playlist editor).
     */
    suspend fun reorderPlaylistSongs(playlistId: Long, orderedSongIds: List<Long>) = withContext(Dispatchers.IO) {
        playlistDao.reorderSongs(playlistId, orderedSongIds)
        playlistDao.updateModifiedAt(playlistId)
    }

    // ── Favorites convenience methods ─────────────────────────────────────────

    fun isFavorite(songId: Long): Flow<Boolean> = favoriteDao.isFavorite(songId)

    suspend fun toggleFavorite(songId: Long) = withContext(Dispatchers.IO) {
        favoriteDao.toggleFavorite(songId)
    }

    fun getFavoriteSongIds(): Flow<List<Long>> = favoriteDao.getAllFavoriteSongIds()
}

private fun PlaylistEntity.toDomain(songIds: List<Long>): Playlist = Playlist(
    id = id,
    name = name,
    description = description,
    coverUri = coverUri?.let { Uri.parse(it) },
    songIds = songIds,
    createdAt = createdAt,
    modifiedAt = modifiedAt,
    isFavorites = isFavorites
)
