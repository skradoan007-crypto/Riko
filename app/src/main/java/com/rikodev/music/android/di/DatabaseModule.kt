package com.rikodev.music.android.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rikodev.music.android.data.db.RikoDatabase
import com.rikodev.music.android.data.db.dao.EqPresetDao
import com.rikodev.music.android.data.db.dao.EqSettingsDao
import com.rikodev.music.android.data.db.dao.FavoriteDao
import com.rikodev.music.android.data.db.dao.PlayHistoryDao
import com.rikodev.music.android.data.db.dao.PlaylistDao
import com.rikodev.music.android.data.db.dao.WaveformDao
import com.rikodev.music.android.data.db.dao.PlaybackStateDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // v3: adds eq_presets table (user-created equalizer presets).
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `eq_presets` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`band_levels` TEXT NOT NULL, " +
                    "`created_at` INTEGER NOT NULL)"
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_eq_presets_name` ON `eq_presets` (`name`)"
            )
        }
    }

    // v4 (plan 6.1): eq_settings gains band_levels_csv — full-band CSV so
    // 6/10-band device layouts survive restart (the legacy 5 columns cannot).
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `eq_settings` ADD COLUMN `band_levels_csv` TEXT NOT NULL DEFAULT ''"
            )
        }
    }

    // v5 (Wave 8): playback_state — queue/position persistence for process-death recovery.
    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `playback_state` (" +
                    "`id` INTEGER NOT NULL PRIMARY KEY, " +
                    "`queue_song_ids` TEXT NOT NULL DEFAULT '', " +
                    "`current_index` INTEGER NOT NULL DEFAULT 0, " +
                    "`position_ms` INTEGER NOT NULL DEFAULT 0, " +
                    "`shuffle_enabled` INTEGER NOT NULL DEFAULT 0, " +
                    "`repeat_mode` INTEGER NOT NULL DEFAULT 0, " +
                    "`updated_at` INTEGER NOT NULL DEFAULT 0)"
            )
        }
    }

    @Provides
    @Singleton
    fun provideRikoDatabase(@ApplicationContext context: Context): RikoDatabase {
        return Room.databaseBuilder(
            context,
            RikoDatabase::class.java,
            "riko_music.db"
        )
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun providePlaylistDao(database: RikoDatabase): PlaylistDao = database.playlistDao()

    @Provides
    @Singleton
    fun provideFavoriteDao(database: RikoDatabase): FavoriteDao = database.favoriteDao()

    @Provides
    @Singleton
    fun providePlayHistoryDao(database: RikoDatabase): PlayHistoryDao = database.playHistoryDao()

    @Provides
    @Singleton
    fun provideEqSettingsDao(database: RikoDatabase): EqSettingsDao = database.eqSettingsDao()

    @Provides
    @Singleton
    fun provideEqPresetDao(database: RikoDatabase): EqPresetDao = database.eqPresetDao()

    @Provides
    @Singleton
    fun provideWaveformDao(database: RikoDatabase): WaveformDao = database.waveformDao()

    @Provides
    @Singleton
    fun providePlaybackStateDao(database: RikoDatabase): PlaybackStateDao = database.playbackStateDao()
}
