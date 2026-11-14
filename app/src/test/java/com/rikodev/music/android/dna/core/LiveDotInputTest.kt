package com.rikodev.music.android.dna.core

import com.rikodev.music.android.audiofx.SoundClass
import org.junit.Assert.*
import org.junit.Test

class LiveDotInputTest {
    private class Source : BandSource {
        override val bandCount = 40
        var available = true
        var calls = 0
        val values = FloatArray(40)
        override fun sample(nowNanos: Long, out: FloatArray): Boolean {
            calls++
            // Deliberately leave stale output on loss to test defensive clearing.
            if (available) values.copyInto(out)
            return available
        }
    }
    private fun advance(input: LiveDotInput, frames: Int = 180, playing: Boolean = true, frozen: Boolean = false) {
        repeat(frames) { input.step(it * 16_666_667L, 1f / 60f, playing, frozen) }
    }
    @Test fun pauseClearsFrozenResponseWithoutReadingSource() {
        val source = Source().apply { values.fill(.7f) }
        val input = LiveDotInput(source)
        advance(input)
        assertTrue(input.fast[0] > .69f)
        val before = source.calls
        advance(input, playing = false, frozen = true)
        assertFalse(input.hasSignal)
        assertEquals(before, source.calls)
        assertTrue(input.fast.all { it < 1e-8f } && input.trail.all { it < 3e-6f })
    }
    @Test fun lostPcmDecaysInsteadOfHoldingPreviousSong() {
        val source = Source().apply { values[24] = .8f }
        val input = LiveDotInput(source)
        advance(input)
        source.available = false
        advance(input)
        assertFalse(input.hasSignal)
        assertTrue(input.fast.all { it < 1e-8f } && input.trail.all { it < 3e-6f })
        source.available = true; source.values.fill(0f); source.values[5] = .3f
        advance(input)
        assertTrue(input.hasSignal && input.fast[5] > .29f)
        assertTrue(input.fast[24] < 1e-8f)
    }
    @Test fun localBandAndFreezeHaveNoAutonomousChanges() {
        val source = Source().apply { values[18] = .5f }
        val input = LiveDotInput(source)
        advance(input)
        assertTrue(input.fast.indices.all { it == 18 || input.fast[it] == 0f })
        val held = input.fast.copyOf()
        source.values.fill(.9f)
        advance(input, frozen = true)
        assertArrayEquals(held, input.fast, 0f)
        advance(input)
        assertTrue(input.fast.all { it > .89f })
        input.reset()
        assertFalse(input.hasSignal)
        assertTrue(input.fast.all { it == 0f } && input.trail.all { it == 0f })
    }
    @Test fun equalElapsedTimeAcrossRefreshRates() {
        val source = Source().apply { values.fill(.8f) }
        val at60 = LiveDotInput(source)
        val at120 = LiveDotInput(source)
        repeat(12) { at60.step(it * 16_666_667L, 1f/60, true, false) }
        repeat(24) { at120.step(it * 8_333_333L, 1f/120, true, false) }
        assertArrayEquals(at60.fast, at120.fast, 1e-5f)
        assertArrayEquals(at60.trail, at120.trail, 1e-5f)
    }

    // ── v2 energy envelopes: asymmetric attack/decay + wave phase ──────
    @Test fun energyEnvelopesTrackBassAndMid() {
        val source = Source().apply { values[2] = .8f; values[20] = .8f }
        val input = LiveDotInput(source)
        advance(input)
        assertTrue("bass envelope should follow the bass bands", input.bass > .5f)
        assertTrue("mid envelope should follow the mid bands", input.mid > .5f)
        assertTrue("wave phase should advance while the signal is live", input.phase > 0f)
        source.values.fill(0f)
        advance(input)
        assertTrue(input.bass < .05f && input.mid < .05f)
    }
    @Test fun phaseHoldsWhilePausedOrFrozen() {
        val source = Source().apply { values.fill(.5f) }
        val input = LiveDotInput(source)
        advance(input, frames = 10)
        assertTrue(input.phase > 0f)
        val held = input.phase
        advance(input, frames = 60, playing = false)
        assertEquals(held, input.phase, 0f)
        advance(input, frames = 60, frozen = true)
        assertEquals(held, input.phase, 0f)
    }
    @Test fun resetClearsEnergyState() {
        val source = Source().apply { values.fill(.6f) }
        val input = LiveDotInput(source)
        advance(input)
        input.reset()
        assertEquals(0f, input.bass, 0f)
        assertEquals(0f, input.mid, 0f)
        assertEquals(0f, input.punch, 0f)
        assertEquals(0f, input.phase, 0f)
        assertEquals(0f, input.roll, 0f)
        assertEquals(SoundClass.LOW_SOUND, input.soundClass)
    }

    // ── Phase 3: display-domain sound class + reaction morph ────────────
    @Test fun soundClassFollowsSourceContent() {
        val source = Source().apply { values.fill(0f); for (i in 0..3) values[i] = .9f }
        val input = LiveDotInput(source)
        advance(input, frames = 30)
        assertEquals(SoundClass.DEEP_BASS, input.soundClass)
        assertTrue("reaction should morph toward the bass character", input.reaction.fold > 1.0f)
    }
    @Test fun vocalSourceMorphsToVocalCharacter() {
        val source = Source().apply {
            values.fill(0f)
            for (i in 17..26) values[i] = .9f // 600Hz–3kHz vocal region @ 44.1kHz
        }
        val input = LiveDotInput(source)
        advance(input, frames = 30)
        assertEquals(SoundClass.VOCAL_ONLY, input.soundClass)
        assertTrue("vocal character breathes more than it folds",
            input.reaction.breathe > input.reaction.fold)
    }
}
