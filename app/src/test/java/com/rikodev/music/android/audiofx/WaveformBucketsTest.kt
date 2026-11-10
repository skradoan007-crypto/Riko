package com.rikodev.music.android.audiofx

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.*
import org.junit.Test

class WaveformBucketsTest {
    private fun pcm(vararg values: Float): ByteBuffer = ByteBuffer.allocate(values.size * 4)
        .order(ByteOrder.LITTLE_ENDIAN).apply { values.forEach { putFloat(it) }; flip() }

    @Test fun peakIsMaximumAndRmsIsEnergyNotMeanChunkAmplitude() {
        val b = WaveformBuckets(1_000_000L)
        val input = pcm(0f, 0f, 0f, 1f)
        b.addPcm(input, 0, input.remaining(), 0, 4, 1, PcmFrames.ENCODING_PCM_FLOAT)
        val data = b.finish(1, 1_000_000L)!!
        assertEquals(1f, data.peakAt(0), 0f)
        assertEquals(0.5f, data.rmsAt(0), 0.00001f)
    }
    @Test fun channelEnergyDoesNotDisappearForInvertedStereo() {
        val b = WaveformBuckets(1_000_000L)
        val input = pcm(0.5f, -0.5f, 0.5f, -0.5f)
        b.addPcm(input, 0, input.remaining(), 0, 2, 2, PcmFrames.ENCODING_PCM_FLOAT)
        val data = b.finish(1, 1_000_000L)!!
        assertEquals(0.5f, data.rmsAt(0), 0.00001f)
    }
    @Test fun positionsComeFromTimestampsNotTheNumberOfDecoderBuffers() {
        val b = WaveformBuckets(4_000_000L)
        val loud = pcm(0.8f, 0.8f)
        b.addPcm(loud, 0, loud.remaining(), 2_000_000L, 2, 1, PcmFrames.ENCODING_PCM_FLOAT)
        val data = b.finish(4, 4_000_000L)!!
        assertEquals(0f, data.peakAt(0), 0f)
        assertEquals(0f, data.peakAt(1), 0f)
        assertEquals(0.8f, data.peakAt(2), 0.00001f)
        assertEquals(0f, data.peakAt(3), 0f)
    }
    @Test fun splittingTheSamePcmIntoUnequalBuffersDoesNotChangeTheResult() {
        val all = FloatArray(1000) { if (it < 400) 0.2f else if (it < 800) 0f else 0.8f }
        val a = WaveformBuckets(1_000_000L)
        val b = WaveformBuckets(1_000_000L)
        val full = pcm(*all)
        a.addPcm(full, 0, full.remaining(), 0, 1000, 1, PcmFrames.ENCODING_PCM_FLOAT)
        for ((start, end) in listOf(0 to 173, 173 to 800, 800 to 1000)) {
            val chunk = pcm(*all.copyOfRange(start, end))
            b.addPcm(chunk, 0, chunk.remaining(), start * 1000L, 1000, 1, PcmFrames.ENCODING_PCM_FLOAT)
        }
        val x = a.finish(100, 1_000_000L)!!; val y = b.finish(100, 1_000_000L)!!
        assertArrayEquals(x.peaks, y.peaks, 0f); assertArrayEquals(x.rms, y.rms, 0.00001f)
    }
    @Test fun unknownLongDurationRemainsBoundedAndPreservesLateAudio() {
        val b = WaveformBuckets(0L)
        val input = pcm(0.6f)
        b.addPcm(input, 0, 4, 3_600_000_000L, 1, 1, PcmFrames.ENCODING_PCM_FLOAT)
        val data = b.finish(1024, 0L)!!
        assertEquals(1024, data.bucketCount)
        assertEquals(0.6f, data.peakAt(1023), 0.00001f)
        assertEquals(0f, data.peakAt(0), 0f)
    }
    @Test fun int16HandlesOffsetsAndTheMinimumSignedValue() {
        val buffer = ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putShort(123).putShort((-32768).toShort()).putShort(0).putShort(0).putShort(0)
        val b = WaveformBuckets(1_000_000L)
        b.addPcm(buffer, 2, 8, 0, 4, 1, PcmFrames.ENCODING_PCM_16BIT)
        val data = b.finish(1, 1_000_000L)!!
        assertEquals(1f, data.peakAt(0), 0f)
        assertEquals(0.5f, data.rmsAt(0), 0.00001f)
    }
    @Test fun cancellationIsCheckedInsideLargeBuffers() {
        val b = WaveformBuckets(1_000_000L)
        try {
            b.addPcm(pcm(1f), 0, 4, 0, 1, 1, PcmFrames.ENCODING_PCM_FLOAT) { throw InterruptedException() }
            fail("Cancellation was ignored")
        } catch (_: InterruptedException) { }
    }
}
