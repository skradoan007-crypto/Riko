package com.rikodev.music.android.dna.render.visual

import com.rikodev.music.android.audiofx.SoundClass
import com.rikodev.music.android.dna.core.ClassReaction
import org.junit.Assert.*
import org.junit.Test

/**
 * Per-class deterministic behaviour specs for the visuals. The GLSL mirrors
 * these formulas (see the "documented spec" comments in each VERT shader);
 * these tests pin the design intent per sound class.
 */
class VisualBehaviorTest {

    // ── Pulse Rings ──────────────────────────────────────────────────────

    @Test fun pulseBassClassIsFasterAndThickerThanVocal() {
        val deep = ClassReaction.forClass(SoundClass.DEEP_BASS)
        val vocal = ClassReaction.forClass(SoundClass.VOCAL_ONLY)
        assertTrue(PulseMath.speed(deep.fold, 0.9f, 0.5f) > PulseMath.speed(vocal.fold, 0.2f, 0f))
        assertTrue(PulseMath.thickness(deep.width) > PulseMath.thickness(vocal.width))
    }

    @Test fun pulseQuietClassIsFaint() {
        val quiet = ClassReaction.forClass(SoundClass.LOW_SOUND)
        val full = ClassReaction.forClass(SoundClass.FULL_MIX)
        assertTrue(PulseMath.light(1f, quiet.flare, 0f, quiet.fold) <
            PulseMath.light(1f, full.flare, 0.9f, full.fold))
    }

    @Test fun pulseRingLightPeaksMidLife() {
        val deep = ClassReaction.forClass(SoundClass.DEEP_BASS)
        assertTrue(PulseMath.light(1f, deep.flare, 0.9f, deep.fold) >
            PulseMath.light(0f, deep.flare, 0.9f, deep.fold))
    }

    // ── Spectral Ribbon ──────────────────────────────────────────────────

    @Test fun ribbonBassClassFoldsDeeperThanVocal() {
        val deep = ClassReaction.forClass(SoundClass.DEEP_BASS)
        val vocal = ClassReaction.forClass(SoundClass.VOCAL_ONLY)
        assertTrue(RibbonMath.amplitude(0.8f, 0.8f, deep.fold) >
            RibbonMath.amplitude(0.8f, 0.8f, vocal.fold))
    }

    @Test fun ribbonQuietClassIsAlmostStill() {
        val quiet = ClassReaction.forClass(SoundClass.LOW_SOUND)
        assertTrue(RibbonMath.amplitude(0.02f, 0f, quiet.fold) < 0.06f)
    }

    @Test fun ribbonLightFollowsEnergyAndFlare() {
        val full = ClassReaction.forClass(SoundClass.FULL_MIX)
        val quiet = ClassReaction.forClass(SoundClass.LOW_SOUND)
        assertTrue(RibbonMath.light(0.9f, 0.8f, 0.5f, full.flare, 0.8f) >
            RibbonMath.light(0.05f, 0f, 0f, quiet.flare, 0f))
    }
}
