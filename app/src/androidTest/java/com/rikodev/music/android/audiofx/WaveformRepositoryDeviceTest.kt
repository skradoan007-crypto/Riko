package com.rikodev.music.android.audiofx

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rikodev.music.android.data.db.dao.WaveformDao
import com.rikodev.music.android.data.db.entity.WaveformCacheEntity
import com.rikodev.music.android.domain.model.Song
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WaveformRepositoryDeviceTest {
    private class Cache : WaveformDao {
        val rows = java.util.concurrent.ConcurrentHashMap<Long, WaveformCacheEntity>()
        val entered = CompletableDeferred<Unit>()
        var delayedId: Long? = null
        var reads = 0
        override suspend fun getWaveform(songId: Long): WaveformCacheEntity? {
            reads++
            if (songId == delayedId) withContext(NonCancellable) {
                entered.complete(Unit)
                delay(350) // deliberately uncooperative provider, to expose stale-publication races
            }
            return rows[songId]
        }
        override suspend fun saveWaveform(entity: WaveformCacheEntity) { rows[entity.songId] = entity }
        override suspend fun trimToNewest(limit: Int) { }
    }
    private fun song(id: Long, file: File) = Song(id, "Fixture", "Test", "Test", 0L,
        4000L, file.length(), file.path, Uri.fromFile(file), 0L, file.lastModified(), 0, 0, 0, 16000, "audio/wav")

    @Test fun rapidSwitchIgnoresOldResultAndFileMutationInvalidatesMemoryAndPersistentCache() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val files = listOf(File(context.cacheDir,"waveform-a.wav"), File(context.cacheDir,"waveform-b.wav"))
        val cache = Cache().apply { delayedId = 11L }
        val repository = WaveformRepository(cache, AudioFileAnalyzer(AndroidPcmSource()), context)
        try {
            for (file in files) instrumentation.context.assets.open("waveform-sections.wav").use { src ->
                file.outputStream().use { src.copyTo(it) }
            }
            val a = song(11L, files[0]); val b = song(12L, files[1])
            repository.loadWaveformFor(a)
            withTimeout(5000) { cache.entered.await() }
            repository.loadWaveformFor(b)
            val ready = withTimeout(20000) { repository.state.filter { it is WaveformState.Ready }.first() }
            assertEquals(12L, (ready as WaveformState.Ready).songId)
            delay(450)
            assertEquals(12L, (repository.state.value as WaveformState.Ready).songId)
            withTimeout(5000) { while (!cache.rows.containsKey(12L)) delay(10) }
            val reads = cache.reads
            repository.loadWaveformFor(b)
            withTimeout(5000) { repository.state.filter { it is WaveformState.Ready }.first() }
            assertEquals("Memory hit should not read Room", reads, cache.reads)
            files[1].writeText("file was replaced, and is no longer audio")
            repository.loadWaveformFor(b) // even the old Song object must recheck current file metadata
            withTimeout(10000) { repository.state.filter { it is WaveformState.Unavailable }.first() }
            repository.loadWaveformFor(null)
            assertEquals(WaveformState.Idle, repository.state.value)
        } finally {
            repository.loadWaveformFor(null)
            files.forEach { it.delete() }
        }
    }
}
