package com.rikodev.music.android.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `dna_profile` (`songKey` TEXT NOT NULL, `seed` INTEGER NOT NULL, `algoVersion` INTEGER NOT NULL, `featureBlob` BLOB NOT NULL, `generation` INTEGER NOT NULL, PRIMARY KEY(`songKey`))")
    }
}
