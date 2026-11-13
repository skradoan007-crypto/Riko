package com.rikodev.music.android.dna.analysis

import android.content.Context
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rikodev.music.android.audiofx.AndroidPcmSource
import com.rikodev.music.android.audiofx.AudioFileAnalyzer
import com.rikodev.music.android.audiofx.PcmConsumer
import com.rikodev.music.android.audiofx.PcmSource
import com.rikodev.music.android.dna.genome.DnaCodec
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Requires Android MediaExtractor/MediaCodec. Not represented as host-JVM validation. */
@RunWith(AndroidJUnit4::class)
class OfflineDnaAnalyzerDeviceTest {
    @Test fun actualWavAacAndMp3RemainCompatibleWithWaveform() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val source = AndroidPcmSource()
        for (fixture in listOf("waveform-sections.wav", "waveform-sections.m4a", "waveform-sections.mp3", "waveform-sections-44100.mp3")) {
            val file = File(context.cacheDir, "dna-$fixture")
            try {
                instrumentation.context.assets.open(fixture).use { input ->
                    file.outputStream().use { input.copyTo(it) }
                }
                val uri = Uri.fromFile(file)
                val waveform = AudioFileAnalyzer(source).analyze(context, uri)
                assertNotNull("Waveform regression: $fixture", waveform)
                val first = OfflineDnaAnalyzer(source).analyze(context, uri)
                assertTrue("DNA decode failed: $fixture: $first", first is DnaAnalysisOutcome.Complete)
                val a = (first as DnaAnalysisOutcome.Complete).analysis
                val again = OfflineDnaAnalyzer(source).analyze(context, uri)
                assertTrue(again is DnaAnalysisOutcome.Complete)
                val b = (again as DnaAnalysisOutcome.Complete).analysis
                assertEquals(a.audioSha256, b.audioSha256)
                assertArrayEquals(DnaCodec.encodeFeatures(a.features), DnaCodec.encodeFeatures(b.features))
                assertTrue(a.timeline.frameCount > 0)
                assertTrue(kotlin.math.abs(a.timeline.durationUs - waveform!!.durationUs) < 250_000L)
                for (i in 1 until a.timeline.frameCount) assertEquals(100_000L, a.timeline.timeUs(i) - a.timeline.timeUs(i - 1))
            } finally { file.delete() }
        }
    }

    @Test fun decoderFailureAndCancellationNeverPublishPartialDna() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        fun source(cancel: Boolean) = object : PcmSource {
            override suspend fun decode(context: Context, uri: Uri, durationHintMs: Long, consumer: PcmConsumer): Long {
                consumer.onStart(1_000_000)
                consumer.onPcm(ByteBuffer.allocate(4096), 0, 4096, 0, 22050, 1, 2) {}
                if (cancel) throw CancellationException("fixture")
                throw IOException("interrupted decode")
            }
        }
        assertTrue(OfflineDnaAnalyzer(source(false)).analyze(context, Uri.EMPTY) is DnaAnalysisOutcome.Unavailable)
        try {
            OfflineDnaAnalyzer(source(true)).analyze(context, Uri.EMPTY)
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { /* expected */ }
        val empty = object : PcmSource {
            override suspend fun decode(context: Context, uri: Uri, durationHintMs: Long, consumer: PcmConsumer): Long {
                consumer.onStart(0)
                return 0
            }
        }
        assertTrue(OfflineDnaAnalyzer(empty).analyze(context, Uri.EMPTY) is DnaAnalysisOutcome.Unavailable)
    }

    @Test fun invalidFileDoesNotPreventNextValidDecode() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val source = AndroidPcmSource()
        val file = File(context.cacheDir, "dna-broken.wav")
        try {
            assertTrue(OfflineDnaAnalyzer(source).analyze(context, Uri.parse("https://example.invalid/audio.mp3"))
                is DnaAnalysisOutcome.Unavailable)
            file.writeText("invalid audio")
            assertTrue(OfflineDnaAnalyzer(source).analyze(context, Uri.fromFile(file)) is DnaAnalysisOutcome.Unavailable)
            instrumentation.context.assets.open("waveform-sections.wav").use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
            assertTrue(OfflineDnaAnalyzer(source).analyze(context, Uri.fromFile(file)) is DnaAnalysisOutcome.Complete)
        } finally { file.delete() }
    }
}
