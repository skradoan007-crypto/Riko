package com.rikodev.music.android.dna.data

import android.content.Context
import android.net.Uri
import androidx.room.Room
import com.rikodev.music.android.audiofx.PcmConsumer
import com.rikodev.music.android.audiofx.PcmSource
import com.rikodev.music.android.data.db.RikoDatabase
import com.rikodev.music.android.dna.analysis.DnaAnalysisFailure
import com.rikodev.music.android.dna.analysis.DnaTimestampException
import com.rikodev.music.android.dna.analysis.OfflineDnaAnalyzer
import com.rikodev.music.android.domain.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.nio.ByteBuffer

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DnaRepositoryTest {
    private lateinit var database: RikoDatabase
    private lateinit var repository: DnaRepository
    private lateinit var cache: File
    private val source = FakePcmSource()
    private val song = Song(91, "Fixture", "Artist", "Album", 1, 100, 4410, "", Uri.parse("content://fixture/91"),
        0, 0, 1, 2026, 128, 22050, "audio/raw")

    @Before fun setup() {
        val context: Context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).allowMainThreadQueries().build()
        cache = File(context.filesDir, "mdna-timelines/${song.dnaKey()}.bin")
        cache.delete()
        repository = DnaRepository(context, OfflineDnaAnalyzer(source), DnaProfiles(database))
    }
    @After fun close() { database.close(); cache.delete() }

    @Test fun failedAttemptRetainsReasonAndExplicitRetryCanRecover() = runBlocking {
        source.failure = DnaTimestampException(false, 14126, 26122, 623, 44100)
        val failure = repository.load(song) as DnaProfileResult.Unavailable
        assertEquals(DnaAnalysisFailure.TIMESTAMP, failure.reason)
        assertEquals("PCM 44100 Hz; frames read 623; expected 14126 us; received 26122 us", failure.diagnostic)
        assertEquals(1, source.calls)
        assertNull(database.dnaProfileDao().get(song.dnaKey()))
        assertFalse(cache.exists())
        source.failure = null
        val result = repository.load(song) as DnaProfileResult.Ready
        assertEquals(2, source.calls)
        assertTrue(result.timeline.frameCount > 0)
        assertEquals(result.genome, (repository.load(song) as DnaProfileResult.Ready).genome)
        assertEquals(2, source.calls) // Both outputs come from cache; no second decode.
    }

    @Test fun corruptTimelineIsRebuiltWithoutReplacingProfile() = runBlocking {
        val initial = repository.load(song) as DnaProfileResult.Ready
        val before = database.dnaProfileDao().get(song.dnaKey())!!
        cache.writeBytes(byteArrayOf(1, 2, 3))
        assertEquals(initial.genome, (repository.load(song) as DnaProfileResult.Ready).genome)
        assertEquals(2, source.calls)
        val after = database.dnaProfileDao().get(song.dnaKey())!!
        assertEquals(before.seed, after.seed)
        assertEquals(before.generation, after.generation)
        assertArrayEquals(before.featureBlob, after.featureBlob)
    }

    @Test fun cancellationPropagatesWithoutSavingProfile() = runBlocking {
        source.failure = CancellationException("cancel fixture")
        try { repository.load(song); fail("Cancellation swallowed") }
        catch (_: CancellationException) { }
        assertEquals(1, source.calls)
        assertFalse(cache.exists())
        assertNull(database.dnaProfileDao().get(song.dnaKey()))
    }

    private class FakePcmSource : PcmSource {
        var calls = 0
        var failure: Exception? = null
        override suspend fun decode(context: Context, uri: Uri, durationHintMs: Long, consumer: PcmConsumer): Long {
            calls++
            failure?.let { throw it }
            consumer.onStart(100_000)
            consumer.onPcm(ByteBuffer.allocate(4410), 0, 4410, 0, 22050, 1, 2) { }
            return 100_000
        }
    }
}
