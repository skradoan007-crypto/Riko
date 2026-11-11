package com.rikodev.music.android.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.platform.app.InstrumentationRegistry
import com.rikodev.music.android.data.db.entity.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

// A real Room-created v5 database, not a v6 database with PRAGMA user_version changed.
@Database(entities = [PlaylistEntity::class, PlaylistSongEntity::class, FavoriteEntity::class,
    PlayHistoryEntity::class, EqSettingsEntity::class, EqPresetEntity::class,
    WaveformCacheEntity::class, PlaybackStateEntity::class], version = 5, exportSchema = false)
abstract class LegacyDnaV5Fixture : RoomDatabase()

class DnaMigrationTest {
    @Test fun migratesRealRoomV5WithoutLosingExistingTables() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "dna-migration-fixture.db"
        context.deleteDatabase(name)
        val old = Room.databaseBuilder(context, LegacyDnaV5Fixture::class.java, name).build()
        val sql = old.openHelper.writableDatabase
        sql.execSQL("INSERT INTO playlists(id,name,description,cover_uri,created_at,modified_at,is_favorites) VALUES(7,'Keep me','Existing playlist',NULL,1,2,0)")
        sql.execSQL("INSERT INTO playlist_songs(playlist_id,song_id,position,added_at) VALUES(7,41,0,123)")
        sql.execSQL("INSERT INTO eq_settings(id,enabled,preset_name,band_0_level,band_1_level,band_2_level,band_3_level,band_4_level,band_levels_csv,virtualizer_enabled,virtualizer_strength,bassboost_enabled,bassboost_strength) VALUES(1,1,'Custom',100,-100,0,200,300,'100,-100,0,200,300',1,400,1,500)")
        sql.execSQL("INSERT INTO eq_presets(id,name,band_levels,created_at) VALUES(5,'Keep EQ','100,-100,0,200,300',123)")
        sql.execSQL("INSERT INTO waveform_cache(songId,buckets_csv,analyzed_at) VALUES(41,'original-cache-payload',123)")
        sql.execSQL("INSERT INTO playback_state(id,queue_song_ids,current_index,position_ms,shuffle_enabled,repeat_mode,updated_at) VALUES(1,'41,42',1,54321,1,2,123)")
        sql.execSQL("INSERT INTO favorites(song_id,added_at) VALUES(41,123)")
        sql.execSQL("INSERT INTO play_history(song_id,played_at,play_duration) VALUES(41,123,0),(41,124,45000)")
        val tables = mutableListOf<String>()
        sql.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name != 'room_master_table'").use {
            while (it.moveToNext()) tables.add(it.getString(0))
        }
        fun snapshot(db: androidx.sqlite.db.SupportSQLiteDatabase, table: String): List<List<String?>> =
            db.query("SELECT * FROM `$table` ORDER BY rowid").use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(List(cursor.columnCount) { column -> cursor.getString(column) })
                }
            }
        val rows = tables.associateWith { table -> snapshot(sql, table) }
        old.close()
        val upgraded = Room.databaseBuilder(context, RikoDatabase::class.java, name).addMigrations(MIGRATION_5_6).build()
        try {
            val db = upgraded.openHelper.writableDatabase // Room validates all v6 tables here.
            for ((table, before) in rows) assertEquals("Preserve every value in $table", before, snapshot(db, table))
            db.query("SELECT added_at FROM favorites WHERE song_id=41").use { assertTrue(it.moveToFirst()); assertEquals(123,it.getInt(0)) }
            assertEquals(1L,upgraded.playHistoryDao().meaningfulListenCount(41,30000))
            assertEquals(0L,upgraded.playHistoryDao().meaningfulListenCount(41,0))
            val dao=upgraded.dnaProfileDao()
            dao.insertIfAbsent(DnaProfileEntity("song",1,1,byteArrayOf(1),0))
            dao.advanceGeneration("song",2)
            dao.insertIfAbsent(DnaProfileEntity("song",2,1,byteArrayOf(2),0))
            assertEquals(1L,dao.get("song")!!.seed)
            assertEquals(2,dao.get("song")!!.generation)
        } finally { upgraded.close(); context.deleteDatabase(name) }
    }
}
