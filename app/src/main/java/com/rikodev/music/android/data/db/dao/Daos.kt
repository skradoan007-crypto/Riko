package com.rikodev.music.android.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.rikodev.music.android.data.db.entity.EqPresetEntity
import com.rikodev.music.android.data.db.entity.EqSettingsEntity
import com.rikodev.music.android.data.db.entity.FavoriteEntity
import com.rikodev.music.android.data.db.entity.PlayHistoryEntity
import com.rikodev.music.android.data.db.entity.PlaylistEntity
import com.rikodev.music.android.data.db.entity.PlaylistSongEntity
import com.rikodev.music.android.data.db.entity.PlaybackStateEntity
import com.rikodev.music.android.data.db.entity.WaveformCacheEntity
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — ROOM DAOs
// All queries return Flow so the UI updates reactively when data changes.
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY is_favorites DESC, modified_at DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: Long): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE is_favorites = 1 LIMIT 1")
    suspend fun getFavoritesPlaylist(): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :id AND is_favorites = 0")
    suspend fun deletePlaylist(id: Long)

    // Get all song IDs for a playlist, ordered by position
    @Query("""
        SELECT song_id FROM playlist_songs 
        WHERE playlist_id = :playlistId 
        ORDER BY position ASC
    """)
    fun getSongIdsForPlaylist(playlistId: Long): Flow<List<Long>>

    @Query("""
        SELECT song_id FROM playlist_songs 
        WHERE playlist_id = :playlistId 
        ORDER BY position ASC
    """)
    suspend fun getSongIdsForPlaylistSync(playlistId: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToPlaylist(entry: PlaylistSongEntity)

    @Query("DELETE FROM playlist_songs WHERE playlist_id = :playlistId AND song_id = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Query("UPDATE playlist_songs SET position = :position WHERE playlist_id = :playlistId AND song_id = :songId")
    suspend fun updateSongPosition(playlistId: Long, songId: Long, position: Int)

    @Transaction
    suspend fun reorderSongs(playlistId: Long, orderedSongIds: List<Long>) {
        orderedSongIds.forEachIndexed { index, songId ->
            updateSongPosition(playlistId, songId, index)
        }
    }

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlist_id = :playlistId")
    suspend fun getSongCount(playlistId: Long): Int

    @Query("UPDATE playlists SET modified_at = :timestamp WHERE id = :id")
    suspend fun updateModifiedAt(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE playlists SET cover_uri = :uri, modified_at = :timestamp WHERE id = :id")
    suspend fun updateCoverUri(id: Long, uri: String?, timestamp: Long = System.currentTimeMillis())
}

@Dao
interface FavoriteDao {

    @Query("SELECT song_id FROM favorites ORDER BY added_at DESC")
    fun getAllFavoriteSongIds(): Flow<List<Long>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE song_id = :songId)")
    fun isFavorite(songId: Long): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE song_id = :songId)")
    suspend fun isFavoriteSync(songId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE song_id = :songId")
    suspend fun removeFavorite(songId: Long)

    @Transaction
    suspend fun toggleFavorite(songId: Long) {
        if (isFavoriteSync(songId)) {
            removeFavorite(songId)
        } else {
            addFavorite(FavoriteEntity(songId))
        }
    }

    @Query("SELECT COUNT(*) FROM favorites")
    suspend fun getFavoriteCount(): Int
}

@Dao
interface PlayHistoryDao {
    // Rows with zero duration are legacy track-start events, never meaningful listens.
    @Query("SELECT COUNT(*) FROM play_history WHERE song_id = :songId AND play_duration >= :minimumListenedMs AND :minimumListenedMs > 0")
    suspend fun meaningfulListenCount(songId: Long, minimumListenedMs: Long): Long


    @Query("SELECT song_id FROM play_history ORDER BY played_at DESC LIMIT :limit")
    fun getRecentlyPlayed(limit: Int = 50): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: PlayHistoryEntity): Long

    @Query("SELECT play_duration FROM play_history WHERE id = :id")
    suspend fun duration(id: Long): Long?

    @Query("UPDATE play_history SET play_duration = :duration WHERE id = :id AND play_duration < :duration")
    suspend fun updateDuration(id: Long, duration: Long)

    @Query("DELETE FROM play_history WHERE played_at < :beforeTimestamp")
    suspend fun clearOldHistory(beforeTimestamp: Long)

    @Query("DELETE FROM play_history")
    suspend fun clearAllHistory()
}

@Dao
interface EqSettingsDao {

    @Query("SELECT * FROM eq_settings WHERE id = 1")
    fun getEqSettings(): Flow<EqSettingsEntity?>

    @Query("SELECT * FROM eq_settings WHERE id = 1")
    suspend fun getEqSettingsSync(): EqSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveEqSettings(settings: EqSettingsEntity)

    @Query("UPDATE eq_settings SET enabled = :enabled WHERE id = 1")
    suspend fun setEnabled(enabled: Boolean)

    @Query("UPDATE eq_settings SET preset_name = :preset WHERE id = 1")
    suspend fun setPreset(preset: String)
}

@Dao
interface EqPresetDao {

    @Query("SELECT * FROM eq_presets ORDER BY created_at ASC")
    fun observeAll(): Flow<List<EqPresetEntity>>

    @Query("SELECT COUNT(*) FROM eq_presets")
    suspend fun count(): Int

    @Query("SELECT * FROM eq_presets WHERE id = :id")
    suspend fun getById(id: Long): EqPresetEntity?

    @Query("SELECT * FROM eq_presets WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): EqPresetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(preset: EqPresetEntity): Long

    @Query("UPDATE eq_presets SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM eq_presets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM eq_presets WHERE name = :name")
    suspend fun deleteByName(name: String)
}

@Dao
interface WaveformDao {

    @Query("SELECT * FROM waveform_cache WHERE songId = :songId")
    suspend fun getWaveform(songId: Long): WaveformCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWaveform(entity: WaveformCacheEntity)

    @Query("DELETE FROM waveform_cache WHERE songId NOT IN (SELECT songId FROM waveform_cache ORDER BY analyzed_at DESC, songId DESC LIMIT :limit)")
    suspend fun trimToNewest(limit: Int)
}

@Dao
interface PlaybackStateDao {
    @Query("SELECT * FROM playback_state WHERE id = 1")
    suspend fun getState(): PlaybackStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveState(state: PlaybackStateEntity)
}
