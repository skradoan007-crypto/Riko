package com.rikodev.music.android.audiofx

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * visualizer_plan §21 Phase-1 matrix: the PCM tap must survive
 * mono/stereo × 16-bit/float without ever moving the buffer position
 * (pass-through is byte-exact — "audio playback is sacred").
 */
class PcmFramesTest {

    private fun buf16(vararg samples: Short): ByteBuffer =
        ByteBuffer.allocate(samples.size * 2)
            .order(ByteOrder.LITTLE_ENDIAN)
            .apply {
                for (s in samples) putShort(s)
                flip()
            }

    private fun bufFloat(vararg samples: Float): ByteBuffer =
        ByteBuffer.allocate(samples.size * 4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .apply {
                for (s in samples) putFloat(s)
                flip()
            }

    @Test fun stride_matchesEncodingAndChannelCount() {
        assertEquals(2, PcmFrames.strideBytes(PcmFrames.ENCODING_PCM_16BIT, 1))
        assertEquals(4, PcmFrames.strideBytes(PcmFrames.ENCODING_PCM_16BIT, 2))
        assertEquals(4, PcmFrames.strideBytes(PcmFrames.ENCODING_PCM_FLOAT, 1))
        assertEquals(8, PcmFrames.strideBytes(PcmFrames.ENCODING_PCM_FLOAT, 2))
        // Degenerate channel count is clamped to 1, never zero (no div-by-zero):
        // 16-bit × 1 channel = 2 bytes — NOT 4.
        assertEquals(2, PcmFrames.strideBytes(PcmFrames.ENCODING_PCM_16BIT, 0))
    }

    @Test fun monoFrame_16BitStereoAveragesBothChannels() {
        // frame 0 = (16384, −16384) → 0.5 / −0.5 → 0.0
        // frame 1 = (−32768, −32768) → −1.0 / −1.0 → −1.0
        val buffer = buf16(16384, -16384, -32768, -32768)
        val stride = PcmFrames.strideBytes(PcmFrames.ENCODING_PCM_16BIT, 2)
        assertEquals(0f, PcmFrames.monoFrame(buffer, 0, PcmFrames.ENCODING_PCM_16BIT, 2), 1e-4f)
        assertEquals(
            -1f,
            PcmFrames.monoFrame(buffer, stride, PcmFrames.ENCODING_PCM_16BIT, 2),
            1e-4f
        )
    }

    @Test fun monoFrame_16BitMonoReadsSingleChannel() {
        val buffer = buf16(32767, -32768)
        assertEquals(1f, PcmFrames.monoFrame(buffer, 0, PcmFrames.ENCODING_PCM_16BIT, 1), 1e-3f)
        assertEquals(-1f, PcmFrames.monoFrame(buffer, 2, PcmFrames.ENCODING_PCM_16BIT, 1), 1e-4f)
    }

    @Test fun monoFrame_floatStereoAveragesBothChannels() {
        val buffer = bufFloat(1f, -1f, 0.5f, 0.25f)
        val stride = PcmFrames.strideBytes(PcmFrames.ENCODING_PCM_FLOAT, 2)
        assertEquals(0f, PcmFrames.monoFrame(buffer, 0, PcmFrames.ENCODING_PCM_FLOAT, 2), 1e-6f)
        assertEquals(
            0.375f,
            PcmFrames.monoFrame(buffer, stride, PcmFrames.ENCODING_PCM_FLOAT, 2),
            1e-6f
        )
    }

    @Test fun monoFrame_neverMovesBufferPosition() {
        val buffer = buf16(100, 200, 300, 400)
        buffer.position(2)
        PcmFrames.monoFrame(buffer, 0, PcmFrames.ENCODING_PCM_16BIT, 2)
        assertEquals(2, buffer.position())
    }

    @Test fun monoFrame_truncatedTailIsSilence() {
        // Three shorts = 6 bytes; a stereo frame at byte 4 would need bytes
        // 4..7 → past the limit, so it must read as silence, not garbage.
        val buffer = buf16(1, 2, 3)
        assertEquals(0f, PcmFrames.monoFrame(buffer, 4, PcmFrames.ENCODING_PCM_16BIT, 2), 1e-6f)
    }

    @Test fun frameCount_countsWholeFramesOnly() {
        val buffer = buf16(1, 2, 3) // 6 bytes
        assertEquals(1, PcmFrames.frameCount(buffer, 0, 4))
        assertEquals(3, PcmFrames.frameCount(buffer, 0, 2))
        assertEquals(0, PcmFrames.frameCount(buffer, 0, 0))
        assertEquals(0, PcmFrames.frameCount(buffer, 6, 2))
    }

    // ── Phase-7 A-task: tap behaviour across seek / track-change / pause ─────
    // PcmFrames is stateless by design, so these pin the invariants the
    // processor depends on when Media3 starts feeding it after each event.

    @Test fun trackChange_secondBufferReadsAsFreshStream() {
        // Event: next track → processor receives a brand-new buffer. No
        // history from buffer A may leak into buffer B's first frame.
        val trackA = buf16(16000, 16000)
        val trackB = buf16(-8192, -8192)
        assertEquals(
            0.488f,
            PcmFrames.monoFrame(trackA, 0, PcmFrames.ENCODING_PCM_16BIT, 2),
            1e-3f
        )
        assertEquals(
            -0.25f,
            PcmFrames.monoFrame(trackB, 0, PcmFrames.ENCODING_PCM_16BIT, 2),
            1e-3f
        )
        assertEquals(1, PcmFrames.frameCount(trackB, 0, 4))
    }

    @Test fun seek_zeroedGapBufferReadsSilenceThenFreshAudio() {
        // Event: seek → sink may hand a run of silence (flush) before the
        // resumed audio. Silence must decode to 0, next frame to real audio.
        val silence = buf16(0, 0, 0, 0)
        val resumed = buf16(32767, 32767)
        val stride = PcmFrames.strideBytes(PcmFrames.ENCODING_PCM_16BIT, 2)
        assertEquals(0f, PcmFrames.monoFrame(silence, 0, PcmFrames.ENCODING_PCM_16BIT, 2), 1e-6f)
        assertEquals(0f, PcmFrames.monoFrame(silence, stride, PcmFrames.ENCODING_PCM_16BIT, 2), 1e-6f)
        assertEquals(1f, PcmFrames.monoFrame(resumed, 0, PcmFrames.ENCODING_PCM_16BIT, 2), 1e-3f)
    }

    @Test fun pause_emptyAndOffsetBuffersYieldNothing() {
    // Event: pause → processor may see an empty buffer or an offset at/past
    // its limit. Both must be "zero frames", never garbage reads.
        val buffer = buf16(1, 2, 3, 4)
        assertEquals(0, PcmFrames.frameCount(buffer, buffer.limit(), 4))
        assertEquals(0f, PcmFrames.monoFrame(buffer, buffer.limit(), PcmFrames.ENCODING_PCM_16BIT, 2), 1e-6f)
        assertEquals(0f, PcmFrames.monoFrame(buffer, -1, PcmFrames.ENCODING_PCM_16BIT, 2), 1e-6f)
    }
}
