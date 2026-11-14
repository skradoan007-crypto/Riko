package com.rikodev.music.android.audiofx

import org.junit.Assert.*
import org.junit.Test

/**
 * Synthetic-spectrum tests for the live sound-type classifier.
 * Every fixture fills real band ranges (via SpectrumBands.bandIndex) so the
 * tests exercise the exact production frequency mapping.
 */
class SoundClassifierTest {
    private val sampleRate = 44100

    /** One Hz range filled at a level. */
    private fun bands(fromHz: Float, toHz: Float, level: Float): FloatArray {
        val out = FloatArray(SpectrumBands.COUNT)
        val lo = SpectrumBands.bandIndex(fromHz, sampleRate)
        val hi = SpectrumBands.bandIndex(toHz, sampleRate).coerceAtLeast(lo + 1)
        for (i in lo until hi.coerceAtMost(out.size)) out[i] = level
        return out
    }

    private fun merge(vararg parts: FloatArray): FloatArray {
        val out = FloatArray(SpectrumBands.COUNT)
        for (p in parts) for (i in out.indices) if (i < p.size && p[i] > out[i]) out[i] = p[i]
        return out
    }

    /** Runs 50ms frames until settled and returns the final state. */
    private fun settle(c: SoundClassifier, levels: FloatArray, frames: Int = 12): SoundClassState {
        var ts = 1_000_000L
        var state = SoundClassState.IDLE
        repeat(frames) { state = c.step(levels, sampleRate, ts); ts += 50_000_000L }
        return state
    }

    // ── Class identification ─────────────────────────────────────────────

    @Test fun quietMusicClassifiesAsLowSound() {
        val state = settle(SoundClassifier(), bands(40f, 18000f, 0.02f))
        assertEquals(SoundClass.LOW_SOUND, state.soundClass)
    }

    @Test fun deepBassSectionClassifiesAsDeepBass() {
        val state = settle(SoundClassifier(), bands(40f, 80f, 0.9f))
        assertEquals(SoundClass.DEEP_BASS, state.soundClass)
        assertTrue(state.deepBass > 0.5f && state.confidence > 0.3f)
    }

    @Test fun midBassSectionClassifiesAsMidBass() {
        val state = settle(SoundClassifier(), bands(80f, 160f, 0.9f))
        assertEquals(SoundClass.MID_BASS, state.soundClass)
    }

    @Test fun highBassSectionClassifiesAsHighBass() {
        val state = settle(SoundClassifier(), bands(160f, 300f, 0.9f))
        assertEquals(SoundClass.HIGH_BASS, state.soundClass)
    }

    @Test fun lowVocalSectionClassifiesAsLowVocal() {
        val state = settle(SoundClassifier(), bands(300f, 600f, 0.9f))
        assertEquals(SoundClass.LOW_VOCAL, state.soundClass)
    }

    @Test fun clearVocalSectionClassifiesAsVocalOnly() {
        val vocal = merge(bands(600f, 3000f, 0.9f), bands(300f, 600f, 0.2f))
        val state = settle(SoundClassifier(), vocal)
        assertEquals(SoundClass.VOCAL_ONLY, state.soundClass)
        assertTrue(state.confidence > 0.3f)
    }

    @Test fun allRegionsStrongClassifiesAsFullMix() {
        val state = settle(SoundClassifier(), bands(40f, 18000f, 0.7f))
        assertEquals(SoundClass.FULL_MIX, state.soundClass)
    }

    // ── Hysteresis & attack/decay physics ────────────────────────────────

    @Test fun firstRealSignalAdoptsImmediately() {
        val c = SoundClassifier()
        val bass = bands(40f, 80f, 0.9f)
        c.step(bass, sampleRate, 1_000_000L)
        val state = c.step(bass, sampleRate, 51_000_000L)
        assertEquals(SoundClass.DEEP_BASS, state.soundClass)
    }

    @Test fun briefSpikeDoesNotFlipClass() {
        val c = SoundClassifier()
        val vocal = merge(bands(600f, 3000f, 0.9f), bands(300f, 600f, 0.2f))
        val bass = bands(40f, 80f, 0.9f)
        var ts = 1_000_000L
        repeat(12) { c.step(vocal, sampleRate, ts); ts += 50_000_000L }
        assertEquals(SoundClass.VOCAL_ONLY, c.step(vocal, sampleRate, ts).soundClass); ts += 50_000_000L
        // A single bass frame must NOT flip the class…
        assertEquals(SoundClass.VOCAL_ONLY, c.step(bass, sampleRate, ts).soundClass); ts += 50_000_000L
        // …and the class stays as the vocal section continues.
        assertEquals(SoundClass.VOCAL_ONLY, c.step(vocal, sampleRate, ts).soundClass)
    }

    @Test fun persistentChangeSwitchesAfterHold() {
        val c = SoundClassifier()
        val vocal = merge(bands(600f, 3000f, 0.9f), bands(300f, 600f, 0.2f))
        val bass = bands(40f, 80f, 0.9f)
        var ts = 1_000_000L
        repeat(12) { c.step(vocal, sampleRate, ts); ts += 50_000_000L }
        assertEquals(SoundClass.VOCAL_ONLY, c.step(vocal, sampleRate, ts).soundClass); ts += 50_000_000L
        // A sustained bass section must switch once it persists past the hold.
        var state = SoundClassState.IDLE
        repeat(10) { state = c.step(bass, sampleRate, ts); ts += 50_000_000L }
        assertEquals(SoundClass.DEEP_BASS, state.soundClass)
        assertTrue(state.confidence > 0.3f)
    }

    @Test fun resetReturnsToIdle() {
        val c = SoundClassifier()
        settle(c, bands(40f, 80f, 0.9f))
        c.reset()
        val state = c.step(bands(40f, 80f, 0f), sampleRate, 500_000_000L)
        assertEquals(SoundClass.LOW_SOUND, state.soundClass)
        assertEquals(0f, state.total, 0.001f)
    }
}
