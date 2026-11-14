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

    // ── Aurora Curtain ───────────────────────────────────────────────────

    @Test fun auroraBassClassFoldsDeeperThanVocal() {
        val deep = ClassReaction.forClass(SoundClass.DEEP_BASS)
        val vocal = ClassReaction.forClass(SoundClass.VOCAL_ONLY)
        assertTrue(AuroraMath.amplitude(0.9f, deep.fold, 0.3f) >
            AuroraMath.amplitude(0.2f, vocal.fold, 0.3f))
        assertTrue(AuroraMath.sway(0.9f, deep.fold) > AuroraMath.sway(0.2f, vocal.fold))
    }

    @Test fun auroraQuietClassIsAlmostStill() {
        val quiet = ClassReaction.forClass(SoundClass.LOW_SOUND)
        assertTrue(AuroraMath.sway(0f, quiet.fold) < 0.02f)
        assertTrue(AuroraMath.light(0.05f, quiet.flare, 0f, 0f) < 0.3f)
    }

    // ── Particle Swarm Organism ──────────────────────────────────────────

    @Test fun swarmBassClassContractsViolently() {
        val deep = ClassReaction.forClass(SoundClass.DEEP_BASS)
        val vocal = ClassReaction.forClass(SoundClass.VOCAL_ONLY)
        // Smaller radius = more contracted (strike pose) for bass.
        assertTrue(SwarmMath.radius(0.9f, deep.fold, 0.2f, 0f) <
            SwarmMath.radius(0.2f, vocal.fold, 1.0f, 0f))
    }

    @Test fun swarmQuietClassIsAlmostFrozen() {
        val quiet = ClassReaction.forClass(SoundClass.LOW_SOUND)
        assertTrue(SwarmMath.push(0.02f, 0f, 0f) < 0.03f)
        assertTrue(SwarmMath.light(0.05f, 0f, quiet.flare, 0f) < 0.3f)
    }

    // ── Fractal Bloom ────────────────────────────────────────────────────

    @Test fun bloomBassClassBloomsViolently() {
        val deep = ClassReaction.forClass(SoundClass.DEEP_BASS)
        val vocal = ClassReaction.forClass(SoundClass.VOCAL_ONLY)
        assertTrue(BloomMath.radius(0.9f, deep.fold, 0.2f, 0f) >
            BloomMath.radius(0.2f, vocal.fold, 1.0f, 0f))
    }

    @Test fun bloomQuietClassIsAlmostClosed() {
        val quiet = ClassReaction.forClass(SoundClass.LOW_SOUND)
        assertTrue(BloomMath.amplitude(0.02f, quiet.fold, quiet.detail) < 0.03f)
        assertTrue(BloomMath.light(0.05f, 0f, quiet.flare, 0f) < 0.3f)
    }

    // ── Liquid Mirror ────────────────────────────────────────────────────

    @Test fun mirrorBassClassSplashesViolently() {
        val deep = ClassReaction.forClass(SoundClass.DEEP_BASS)
        val vocal = ClassReaction.forClass(SoundClass.VOCAL_ONLY)
        assertTrue(MirrorMath.displacement(0.9f, 0.9f, deep.fold) >
            MirrorMath.displacement(0.2f, 0.2f, vocal.fold))
    }

    @Test fun mirrorQuietClassIsAlmostFrozen() {
        val quiet = ClassReaction.forClass(SoundClass.LOW_SOUND)
        assertTrue(MirrorMath.displacement(0.02f, 0f, quiet.fold) < 0.03f)
        assertTrue(MirrorMath.light(0.05f, 0f, quiet.flare, 0f) < 0.3f)
    }

    // ── Ember Storm ──────────────────────────────────────────────────────

    @Test fun emberBassClassRisesViolently() {
        val deep = ClassReaction.forClass(SoundClass.DEEP_BASS)
        val vocal = ClassReaction.forClass(SoundClass.VOCAL_ONLY)
        assertTrue(EmberMath.drift(0.9f, deep.fold, 0.5f) > EmberMath.drift(0.2f, vocal.fold, 0f))
        assertTrue(EmberMath.swirl(0.9f, deep.fold) > EmberMath.swirl(0.2f, vocal.fold))
    }

    @Test fun emberQuietClassIsAlmostExtinguished() {
        val quiet = ClassReaction.forClass(SoundClass.LOW_SOUND)
        assertTrue(EmberMath.swirl(0.02f, quiet.fold) < 0.02f)
        assertTrue(EmberMath.light(0.05f, 0f, quiet.flare, 0f) < 0.3f)
    }

    // ── Breathing Grid ───────────────────────────────────────────────────

    @Test fun gridBassClassFoldsViolently() {
        val deep = ClassReaction.forClass(SoundClass.DEEP_BASS)
        val vocal = ClassReaction.forClass(SoundClass.VOCAL_ONLY)
        assertTrue(GridMath.displacement(0.9f, 0.9f, deep.fold) >
            GridMath.displacement(0.2f, 0.2f, vocal.fold))
    }

    @Test fun gridQuietClassIsAlmostFlat() {
        val quiet = ClassReaction.forClass(SoundClass.LOW_SOUND)
        assertTrue(GridMath.displacement(0.02f, 0f, quiet.fold) < 0.03f)
        assertTrue(GridMath.light(0.05f, 0f, quiet.flare, 0f) < 0.3f)
    }

    // ── Morph Orb ────────────────────────────────────────────────────────

    @Test fun orbBassClassBecomesSpikyWeapon() {
        val deep = ClassReaction.forClass(SoundClass.DEEP_BASS)
        val vocal = ClassReaction.forClass(SoundClass.VOCAL_ONLY)
        // Bass shrinks radius (violent contraction) but explodes spikes.
        assertTrue(OrbMath.radius(0.9f, deep.fold, 0.2f, 0f) <
            OrbMath.radius(0.2f, vocal.fold, 1.0f, 0f))
        assertTrue(OrbMath.spike(0.9f, deep.fold, 0.5f) > OrbMath.spike(0.2f, vocal.fold, 1.0f))
    }

    @Test fun orbQuietClassIsTinyRestingDot() {
        val quiet = ClassReaction.forClass(SoundClass.LOW_SOUND)
        assertTrue(OrbMath.spike(0.02f, quiet.fold, quiet.detail) < 0.02f)
        assertTrue(OrbMath.light(0.05f, 0f, quiet.flare, 0f) < 0.3f)
    }
}
