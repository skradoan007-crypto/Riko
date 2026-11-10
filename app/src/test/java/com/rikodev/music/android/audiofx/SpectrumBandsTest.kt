package com.rikodev.music.android.audiofx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class SpectrumBandsTest {

    @Test fun bandIndex_clampsBelowMinAndAboveNyquist() {
        assertEquals(0, SpectrumBands.bandIndex(10f, 44100))
        assertEquals(0, SpectrumBands.bandIndex(0f, 44100))
        assertEquals(0, SpectrumBands.bandIndex(100f, 0))
        assertEquals(
            SpectrumBands.COUNT - 1,
            SpectrumBands.bandIndex(22050f, 44100)
        )
        assertEquals(
            SpectrumBands.COUNT - 1,
            SpectrumBands.bandIndex(30000f, 44100)
        )
    }

    @Test fun bandIndex_isMonotonicAcrossLogSweep() {
        var prev = -1
        // 40Hz → 22kHz in 2^(1/8) steps: index must never step backwards.
        var f = SpectrumBands.MIN_HZ
        while (f <= 22000f) {
            val idx = SpectrumBands.bandIndex(f, 44100)
            assertTrue("freq $f mapped backwards: $idx < $prev", idx >= prev)
            prev = idx
            f *= 2f.pow(1f / 8f)
        }
        // Full range is actually used (not collapsed into a few bands).
        assertTrue(prev >= SpectrumBands.COUNT - 2)
    }

    @Test fun dbNormalize_mapsFloorToZeroAndCeilingToOne() {
        assertEquals(0f, SpectrumBands.dbNormalize(0f))
        assertEquals(0f, SpectrumBands.dbNormalize(-1f))
        // mag 1.0 → 0dB → 1.0
        assertEquals(1f, SpectrumBands.dbNormalize(1f))
        // mag 0.001 → −60dB → 0.0
        assertEquals(0f, SpectrumBands.dbNormalize(0.001f))
        // mag ~0.0316 → −30dB → 0.5
        assertEquals(0.5f, SpectrumBands.dbNormalize(0.0316f), 0.02f)
        // Huge magnitudes clamp, never exceed 1.
        assertEquals(1f, SpectrumBands.dbNormalize(1000f))
    }

    @Test fun smoother_attackIsFasterThanRelease() {
        val up = SpectrumBands.Smoother(1)
        up.update(floatArrayOf(1f), 0L)
        val upStep = up.levels[0]

        val down = SpectrumBands.Smoother(1)
        down.levels[0] = 1f
        down.peaks[0] = 1f
        down.update(floatArrayOf(0f), 0L)
        val downStep = 1f - down.levels[0]

        assertEquals(SpectrumBands.ATTACK, upStep, 1e-6f)
        assertEquals(SpectrumBands.RELEASE, downStep, 1e-6f)
        assertTrue("attack must exceed release", upStep > downStep)
    }

    @Test fun smoother_peakHoldsThenDecays() {
        val s = SpectrumBands.Smoother(1)
        s.update(floatArrayOf(1f), 0L)
        assertEquals(1f, s.peaks[0], 1e-6f)

        // Inside the hold window the peak sticks even as the level falls.
        s.update(floatArrayOf(0.2f), SpectrumBands.PEAK_HOLD_NS / 2)
        assertEquals(1f, s.peaks[0], 1e-6f)

        // Past the hold the peak falls toward the live level.
        s.update(floatArrayOf(0.2f), SpectrumBands.PEAK_HOLD_NS * 2)
        assertTrue("peak should decay after hold", s.peaks[0] < 1f)
        assertTrue("peak never drops below live level", s.peaks[0] >= s.levels[0])
    }

    @Test fun smoother_resetClearsState() {
        val s = SpectrumBands.Smoother(2)
        s.update(floatArrayOf(1f, 0.8f), 0L)
        s.reset()
        assertEquals(0f, s.levels[0])
        assertEquals(0f, s.levels[1])
        assertEquals(0f, s.peaks[0])
        assertEquals(0f, s.peaks[1])
    }
}
