package com.rikodev.music.android.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rikodev.music.android.data.db.entity.DnaProfileEntity

@Dao
interface DnaProfileDao {
    @Query("SELECT * FROM dna_profile WHERE songKey = :songKey")
    suspend fun get(songKey: String): DnaProfileEntity?

    @Query("UPDATE dna_profile SET generation = MIN(64, generation + 1) WHERE songKey = :songKey")
    suspend fun incrementGeneration(songKey: String)

    // Analysis retries must not overwrite identity or an evolved generation.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(profile: DnaProfileEntity): Long

    @Query("UPDATE dna_profile SET generation = :generation WHERE songKey = :songKey AND generation < :generation")
    suspend fun advanceGeneration(songKey: String, generation: Int): Int
}
