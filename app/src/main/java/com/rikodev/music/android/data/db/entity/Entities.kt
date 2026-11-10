package com.rikodev.music.android.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — ROOM DATABASE ENTITIES
// Persistent local storage for playlists, favorites, history, EQ settings.
// MediaStore provides the actual song files — Room stores user data only.
// ─────────────────────────────────────────────────────────────────────────────

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    @ColumnInfo(name = "cover_uri")
    val coverUri: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "modified_at")
    val modifiedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_favorites")
    val isFavorites: Boolean = false
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlist_id", "song_id"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlist_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlist_id"), Index("song_id")]
)
data class PlaylistSongEntity(
    @ColumnInfo(name = "playlist_id")
    val playlistId: Long,
    @ColumnInfo(name = "song_id")
    val songId: Long,
    @ColumnInfo(name = "position")
    val position: Int,
    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    @ColumnInfo(name = "song_id")
    val songId: Long,
    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "play_history")
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "song_id")
    val songId: Long,
    @ColumnInfo(name = "played_at")
    val playedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "play_duration")
    val playDuration: Long = 0L    // how long was actually listened to
)

@Entity(tableName = "eq_settings")
data class EqSettingsEntity(
    @PrimaryKey
    val id: Int = 1,   // single row — always id=1
    @ColumnInfo(name = "enabled")
    val enabled: Boolean = true,
    @ColumnInfo(name = "preset_name")
    val presetName: String = "Custom",
    @ColumnInfo(name = "band_0_level")
    val band0Level: Int = 0,
    @ColumnInfo(name = "band_1_level")
    val band1Level: Int = 0,
    @ColumnInfo(name = "band_2_level")
    val band2Level: Int = 0,
    @ColumnInfo(name = "band_3_level")
    val band3Level: Int = 0,
    @ColumnInfo(name = "band_4_level")
    val band4Level: Int = 0,
    // Plan 6.1: ALL device bands (5/6/10-band layouts) as CSV of millibels.
    // Legacy five columns above stay for rollback/older readers; the CSV is
    // authoritative whenever non-empty.
    @ColumnInfo(name = "band_levels_csv")
    val bandLevelsCsv: String = "",
    @ColumnInfo(name = "virtualizer_enabled")
    val virtualizerEnabled: Boolean = false,
    @ColumnInfo(name = "virtualizer_strength")
    val virtualizerStrength: Int = 0,
    @ColumnInfo(name = "bassboost_enabled")
    val bassBoostEnabled: Boolean = false,
    @ColumnInfo(name = "bassboost_strength")
    val bassBoostStrength: Int = 0
)

/** A user-created equalizer preset. Band levels stored as CSV of millibels. */
@Entity(
    tableName = "eq_presets",
    indices = [Index(value = ["name"], unique = true)]
)
data class EqPresetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "band_levels")
    val bandLevels: String,          // e.g. "300,-100,0,0,200"
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

// Real, per-song waveform — computed ONCE by decoding the actual audio
// file (see AudioFileAnalyzer), then cached here so it's never
// recomputed on repeat plays. Not generated/fake data: this is the real
// RMS-per-time-bucket of the song's own decoded PCM.
@Entity(tableName = "waveform_cache")
data class WaveformCacheEntity(
    @PrimaryKey
    val songId: Long,
    @ColumnInfo(name = "buckets_csv")
    val bucketsCsv: String,   // Versioned compact waveform payload; column name retained for Room compatibility
    @ColumnInfo(name = "analyzed_at")
    val analyzedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playback_state")
data class PlaybackStateEntity(
    @PrimaryKey
    val id: Int = 1,   // single row — always id=1
    @ColumnInfo(name = "queue_song_ids")
    val queueSongIds: String = "",   // comma-separated song IDs
    @ColumnInfo(name = "current_index")
    val currentIndex: Int = 0,
    @ColumnInfo(name = "position_ms")
    val positionMs: Long = 0L,
    @ColumnInfo(name = "shuffle_enabled")
    val shuffleEnabled: Boolean = false,
    @ColumnInfo(name = "repeat_mode")
    val repeatMode: Int = 0,   // 0=OFF, 1=ONE, 2=ALL (Player.REPEAT_MODE_*)
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
