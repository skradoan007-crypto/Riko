package com.rikodev.music.android.audiofx

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises actual Android MediaExtractor/MediaCodec, not a JVM DSP surrogate.
 * Assets are generated stereo test tones with known energetic/silent/quiet
 * sections (and inverted channels), not user music or a production fallback.
 */
@RunWith(AndroidJUnit4::class)
class AudioFileAnalyzerDeviceTest {
    @Test fun wavAndAacProduceTimeAlignedDistinctRealEnvelopes() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        for (extension in listOf("wav", "m4a")) {
            val file = File(instrumentation.targetContext.cacheDir, "waveform-fixture.$extension")
            try {
                instrumentation.context.assets.open("waveform-sections.$extension").use { source ->
                    file.outputStream().use { source.copyTo(it) }
                }
                val data = AudioFileAnalyzer().analyze(instrumentation.targetContext, Uri.fromFile(file), 512)!!
                fun average(from: Int, to: Int) = (from..to).map { data.rmsAt(it) }.average()
                assertTrue("Energetic section lost: $extension", average(20,100) > 0.45)
                assertTrue("Silence became energy: $extension", average(150,220) < 0.005)
                assertTrue("Quiet section not quiet: $extension", average(280,350) in 0.015..0.05)
                assertTrue("Inverted channels cancelled: $extension", average(410,490) > 0.15)
                val repeat = AudioFileAnalyzer().analyze(instrumentation.targetContext, Uri.fromFile(file), 512)!!
                assertArrayEquals(data.peaks, repeat.peaks, 0.0001f)
                assertArrayEquals(data.rms, repeat.rms, 0.0001f)
            } finally { file.delete() }
        }
    }
    @Test fun appDoesNotDeclareMicrophonePermission() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        @Suppress("DEPRECATION")
        val requested = context.packageManager.getPackageInfo(context.packageName,
            android.content.pm.PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
        assertFalse(requested.contains(android.Manifest.permission.RECORD_AUDIO))
    }
    @Test fun invalidAudioIsUnavailableWithoutThrowing() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "broken-audio.bin")
        try {
            file.writeText("not an audio file")
            assertNull(AudioFileAnalyzer().analyze(context, Uri.fromFile(file)))
        } finally { file.delete() }
    }
}
