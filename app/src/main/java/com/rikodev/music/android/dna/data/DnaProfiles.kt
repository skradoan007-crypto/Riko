package com.rikodev.music.android.dna.data

import com.rikodev.music.android.data.db.RikoDatabase
import com.rikodev.music.android.data.db.entity.DnaProfileEntity
import com.rikodev.music.android.dna.genome.*
import androidx.room.withTransaction
import com.rikodev.music.android.domain.model.Song
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DnaProfiles @Inject constructor(private val database: RikoDatabase) {
    suspend fun generation(key: String): Int = database.dnaProfileDao().get(key)?.generation ?: 0

    suspend fun load(songKey: String): DnaGenome? {
        val row = database.dnaProfileDao().get(songKey) ?: return null
        require(row.generation >= 0)
        return DnaGenomeMapper.map(DnaCodec.decodeFeatures(row.featureBlob), row.seed,
            DnaAlgorithmVersion.fromId(row.algoVersion))
    }
    suspend fun storeIfAbsent(songKey: String, features: DnaFeatures, genome: DnaGenome, song: Song? = null) = database.withTransaction {
        require(songKey.isNotBlank() && songKey.length <= 512)
        require(DnaGenomeMapper.map(features,genome.seed,genome.algorithmVersion) == genome)
        database.dnaProfileDao().insertIfAbsent(DnaProfileEntity(songKey,genome.seed,
            genome.algorithmVersion.id,DnaCodec.encodeFeatures(features),
                if (song == null) 0 else database.playHistoryDao().meaningfulListenCount(song.id, song.meaningfulMs()).coerceAtMost(64).toInt()))
    }
}
