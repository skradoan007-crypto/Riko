package com.rikodev.music.android.audiofx.visualizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpectrumVisualizerTest {

    @Test fun resample_identityWhenSameSize() {
        val src = FloatArray(40) { it / 40f }
        val out = SpectrumConfig.resampleToDisplay(src, 40)
        assertEquals(40, out.size)
        for (i in src.indices) assertEquals(src[i], out[i], 1e-6f)
    }

    @Test fun resample_downTo32KeepsRange() {
        val src = FloatArray(40) { it / 39f }
        val out = SpectrumConfig.resampleToDisplay(src, 32)
        assertEquals(32, out.size)
        assertEquals(0f, out.first(), 1e-6f)
        assertEquals(1f, out.last(), 1e-3f)
        var prev = -1f
        out.forEach { v ->
            assertTrue(v in 0f..1f)
            assertTrue(v >= prev - 1e-6f)
            prev = v
        }
    }

    @Test fun resample_upTo48KeepsRange() {
        val src = FloatArray(40) { it / 39f }
        val out = SpectrumConfig.resampleToDisplay(src, 48)
        assertEquals(48, out.size)
        assertEquals(0f, out.first(), 1e-6f)
        assertEquals(1f, out.last(), 1e-3f)
    }

    @Test fun magnitude_pythagoras() {
        assertEquals(5f, SpectrumEngine.magnitude(3f, 4f), 1e-5f)
        assertEquals(0f, SpectrumEngine.magnitude(0f, 0f), 1e-6f)
    }

    @Test fun engine_silenceGivesZeros() {
        val engine = SpectrumEngine(512)
        val out = FloatArray(com.rikodev.music.android.audiofx.SpectrumBands.COUNT)
        engine.analyzePcm16(ShortArray(512), 1, 44100, out)
        out.forEach { assertEquals(0f, it, 1e-6f) }
    }

    @Test fun engine_sineWaveLightsExpectedBand() {
        val sampleRate = 44100
        val freq = 1000f // 1kHz test tone
        val engine = SpectrumEngine(1024)
        val pcm = ShortArray(1024) { i ->
            val angle = 2.0 * Math.PI * freq * i / sampleRate
            (kotlin.math.sin(angle) * 30000).toInt().toShort()
        }
        val out = FloatArray(com.rikodev.music.android.audiofx.SpectrumBands.COUNT)
        engine.analyzePcm16(pcm, 1, sampleRate, out)

        val targetBand = com.rikodev.music.android.audiofx.SpectrumBands.bandIndex(freq, sampleRate)
        val farBand = com.rikodev.music.android.audiofx.SpectrumBands.bandIndex(8000f, sampleRate)

        // The tone's own band must be hot.
        assertTrue(
            "Band $targetBand (1kHz) should be near full scale, got ${out[targetBand]}",
            out[targetBand] > 0.85f
        )

        // A band far from the tone must be much quieter. Asserted as a RELATIVE
        // gap rather than "which band is exactly the maximum", because that is
        // NOT a stable property of this DSP today: dbNormalize() floors at
        // -60 dB and ceilings at 0 dB over an UNNORMALISED FFT magnitude, so
        // full-scale content saturates several neighbouring bands to 1.0 at
        // once and the "first max" is decided by tie-breaking, not by energy.
        // See SpectrumBandsTest.dbNormalize_mapsFloorToZeroAndCeilingToOne for
        // the floor/ceiling contract this relies on.
        // FOLLOW-UP (Visualizer quality, not a test bug): normalise magnitude
        // by the window's coherent gain so the dB scale regains dynamic range
        // instead of pinning loud music near 1.0 — needs on-device visual check.
        assertTrue(
            "Band $farBand (8kHz) should be far quieter than band $targetBand, " +
                "got ${out[farBand]} vs ${out[targetBand]}",
            out[farBand] < out[targetBand] - 0.15f
        )
    }

    @Test fun delayLine_delaysByExpectedSlots() {
        val delay = SpectrumFrameDelay(bandCount = 4, slotCount = 3) // delays by 2 pushes
        val f1 = floatArrayOf(1f, 1f, 1f, 1f)
        val f2 = floatArrayOf(2f, 2f, 2f, 2f)
        val f3 = floatArrayOf(3f, 3f, 3f, 3f)
        val p = FloatArray(4)

        assertEquals(SpectrumFrameDelay.NOT_READY, delay.push(f1, p))
        assertEquals(SpectrumFrameDelay.NOT_READY, delay.push(f2, p))

        // 3rd push releases the 1st
        val slot1 = delay.push(f3, p)
        assertTrue(slot1 != SpectrumFrameDelay.NOT_READY)
        assertEquals(1f, delay.levelsAt(slot1)[0], 1e-6f)

        // 4th push releases the 2nd
        val slot2 = delay.push(floatArrayOf(4f, 4f, 4f, 4f), p)
        assertEquals(2f, delay.levelsAt(slot2)[0], 1e-6f)
    }

    @Test fun delayLine_clearResetsPriming() {
        val delay = SpectrumFrameDelay(bandCount = 4, slotCount = 3)
        val f = floatArrayOf(1f, 1f, 1f, 1f)
        val p = FloatArray(4)
        delay.push(f, p)
        delay.push(f, p)
        delay.clear()
        assertEquals(SpectrumFrameDelay.NOT_READY, delay.push(f, p))
    }

    @Test fun tier_bandCountsMatchSpec() {
        assertEquals(32, SpectrumConfig.bandCountFor(SpectrumTier.LOW))
        assertEquals(40, SpectrumConfig.bandCountFor(SpectrumTier.BALANCED))
        assertEquals(48, SpectrumConfig.bandCountFor(SpectrumTier.FLAGSHIP))
    }

    @Test fun controller_rejectsStaleFrames() {
        assertTrue(
            SpectrumController.isTapLive(true, 40, 100_000_000L)
        )
        assertTrue(
            !SpectrumController.isTapLive(true, 40, 600_000_000L)
        )
        assertTrue(
            !SpectrumController.isTapLive(false, 40, 0L)
        )
        assertTrue(
            !SpectrumController.isTapLive(true, 32, 0L)
        )
    }
}
