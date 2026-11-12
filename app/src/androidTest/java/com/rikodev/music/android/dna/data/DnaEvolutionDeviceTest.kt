package com.rikodev.music.android.dna.data

import android.net.Uri
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.rikodev.music.android.data.db.RikoDatabase
import com.rikodev.music.android.dna.genome.*
import com.rikodev.music.android.domain.model.Song
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class DnaEvolutionDeviceTest {
    @Test fun countsThresholdOncePreservesIdentityAndBackfillsNewProfiles() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).build()
        try {
            val profiles = DnaProfiles(db)
            val listening = DnaListening(db)
            val song = Song(41, "Fixture", "Artist", "Album", 1, 120_000, 123,
                "", Uri.parse("content://media/external/audio/media/41"), 1, 1, 1, 2026, 128, 44100, "audio/mpeg")
            val features = DnaFeatures.fromNormalized(.5,.3,.2,2000.0,.3,120.0,.8,20.0,DoubleArray(12))
            val base = DnaGenomeMapper.map(features,1L,DnaAlgorithmVersion.GENESIS_V1)
            val row = listening.save(song,0L,5_000) // Skip: recorded but not a meaningful listen.
            assertEquals(0L,db.playHistoryDao().meaningfulListenCount(song.id,song.meaningfulMs()))
            listening.save(song,row,60_000)
            profiles.storeIfAbsent(song.dnaKey(),features,base,song) // Earlier meaningful history is retained.
            assertEquals(1,profiles.generation(song.dnaKey()))
            listening.save(song,row,80_000) // Updating the same listen must not evolve twice.
            assertEquals(1,profiles.generation(song.dnaKey()))
            listening.save(song,0L,60_000)
            assertEquals(2,profiles.generation(song.dnaKey()))
            listening.save(song,row,10_000) // Delayed/stale update cannot reduce duration.
            assertEquals(80_000L,db.playHistoryDao().duration(row))
            assertEquals(base,profiles.load(song.dnaKey()))
            repeat(80) { db.dnaProfileDao().incrementGeneration(song.dnaKey()) }
            assertEquals(64,profiles.generation(song.dnaKey()))
        } finally { db.close() }
    }
}
