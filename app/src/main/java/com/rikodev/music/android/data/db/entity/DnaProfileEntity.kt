package com.rikodev.music.android.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** featureBlob is DnaCodec's versioned, checksummed canonical summary. */
@Entity(tableName = "dna_profile")
data class DnaProfileEntity(
    @PrimaryKey val songKey: String,
    val seed: Long,
    val algoVersion: Int,
    val featureBlob: ByteArray,
    val generation: Int = 0
)
