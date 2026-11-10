package com.rikodev.music.android.dna.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyntheticSpectrumTest {

    @Test
    fun values_stayInUnitRange_acrossTime() {
        val out = FloatArray(40)
        var t = 0f
        while (t < 30f) {
            SyntheticSpectrum.fill(out, t)
            for (v in out) assertTrue("out of range at t=$t: $v", v in 0f..1f)
            t += 0.037f
        }
    }

    @Test
    fun kick_onTheBeat_isLouderInBassThanBetweenBeats() {
        val onBeat = FloatArray(40)
        val between = FloatArray(40)
        SyntheticSpectrum.fill(onBeat, 1.0f)      // exactly on a beat (120BPM)
        SyntheticSpectrum.fill(between, 1.4f)     // late in the beat
        assertTrue(onBeat[0] > between[0] + 0.2f)
    }

    @Test
    fun isDeterministic() {
        val a = FloatArray(40)
        val b = FloatArray(40)
        SyntheticSpectrum.fill(a, 3.21f)
        SyntheticSpectrum.fill(b, 3.21f)
        assertArrayEquals(a, b, 0f)
    }
}
