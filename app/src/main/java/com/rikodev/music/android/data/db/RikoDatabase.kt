package com.rikodev.music.android.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rikodev.music.android.data.db.dao.EqPresetDao
import com.rikodev.music.android.data.db.dao.EqSettingsDao
import com.rikodev.music.android.data.db.dao.FavoriteDao
import com.rikodev.music.android.data.db.dao.PlayHistoryDao
import com.rikodev.music.android.data.db.dao.PlaylistDao
import com.rikodev.music.android.data.db.dao.PlaybackStateDao
import com.rikodev.music.android.data.db.dao.WaveformDao
import com.rikodev.music.android.data.db.entity.EqPresetEntity
import com.rikodev.music.android.data.db.entity.EqSettingsEntity
import com.rikodev.music.android.data.db.entity.FavoriteEntity
import com.rikodev.music.android.data.db.entity.PlayHistoryEntity
import com.rikodev.music.android.data.db.entity.PlaylistEntity
import com.rikodev.music.android.data.db.entity.PlaylistSongEntity
import com.rikodev.music.android.data.db.entity.WaveformCacheEntity
import com.rikodev.music.android.data.db.entity.PlaybackStateEntity

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        FavoriteEntity::class,
        PlayHistoryEntity::class,
        EqSettingsEntity::class,
        EqPresetEntity::class,
        WaveformCacheEntity::class,
        PlaybackStateEntity::class
    ],
    // v5 (Wave 8): playback_state — queue/position persistence for process-death recovery
    version = 5,
    exportSchema = true
)
abstract class RikoDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun playHistoryDao(): PlayHistoryDao
    abstract fun eqSettingsDao(): EqSettingsDao
    abstract fun eqPresetDao(): EqPresetDao
    abstract fun waveformDao(): WaveformDao
    abstract fun playbackStateDao(): PlaybackStateDao
}
