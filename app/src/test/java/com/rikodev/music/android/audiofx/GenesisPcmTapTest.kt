package com.rikodev.music.android.audiofx

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class GenesisPcmTapTest {
    private fun processor() = PcmAudioProcessor().apply {
        configure(AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)); flush()
    }
    private fun audio(): ByteArray {
        val buffer = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN)
        repeat(1024) { i ->
            val sample = (sin(i * 2.0 * Math.PI * 440 / 44100) * 12000).toInt().toShort()
            buffer.putShort(sample); buffer.putShort(sample)
        }
        return buffer.array()
    }
    @Test fun liveTapReceivesFiniteBandsAndPassthroughIsByteExact() {
        val processor = processor()
        var frames = 0
        processor.rawTap = object : RawSpectrumTap {
            override fun onFrame(levels: FloatArray, timestampNanos: Long) {
                assertEquals(40, levels.size)
                assertTrue(levels.all { it.isFinite() && it in 0f..1f })
                assertTrue(levels.any { it > 0f })
                frames++
            }
            override fun onReset() = Unit
        }
        val input = audio()
        processor.queueInput(ByteBuffer.allocateDirect(input.size).put(input).apply { flip() })
        val output = ByteArray(input.size); processor.output.get(output)
        assertArrayEquals(input, output); assertEquals(1, frames)
    }
    @Test fun brokenTapIsDetachedWithoutChangingAudio() {
        val processor = processor()
        processor.rawTap = object : RawSpectrumTap {
            override fun onFrame(levels: FloatArray, timestampNanos: Long) { error("test consumer") }
            override fun onReset() = Unit
        }
        val input = audio()
        processor.queueInput(ByteBuffer.allocateDirect(input.size).put(input).apply { flip() })
        val output = ByteArray(input.size); processor.output.get(output)
        assertArrayEquals(input, output); assertNull(processor.rawTap)
    }
    @Test fun flushNotifiesDiscontinuity() {
        val processor = processor()
        var resets = 0
        processor.rawTap = object : RawSpectrumTap {
            override fun onFrame(levels: FloatArray, timestampNanos: Long) = Unit
            override fun onReset() { resets++ }
        }
        processor.flush()
        assertEquals(1, resets)
    }
}
