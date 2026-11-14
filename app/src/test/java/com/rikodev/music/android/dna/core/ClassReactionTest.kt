package com.rikodev.music.android.dna.core

import com.rikodev.music.android.audiofx.SoundClass
import org.junit.Assert.*
import org.junit.Test

/** The reaction matrix is the design contract — these tests pin it down. */
class ClassReactionTest {

    @Test fun everyClassHasADistinctCharacter() {
        val profiles = SoundClass.values().map { ClassReaction.forClass(it) }
        assertEquals("no two sound types may share a reaction profile",
            profiles.size, profiles.toSet().size)
    }

    @Test fun reactionMatrixMatchesDesign() {
        val deep = ClassReaction.forClass(SoundClass.DEEP_BASS)
        val vocal = ClassReaction.forClass(SoundClass.VOCAL_ONLY)
        val quiet = ClassReaction.forClass(SoundClass.LOW_SOUND)
        val full = ClassReaction.forClass(SoundClass.FULL_MIX)
        val high = ClassReaction.forClass(SoundClass.HIGH_BASS)

        // Bass folds monstrously; vocals ripple thin.
        assertTrue(deep.fold > vocal.fold)
        // Vocal sections breathe; deep bass barely does.
        assertTrue(vocal.breathe > deep.breathe)
        // The vocal ring is thin and elegant; the bass ring is fat fire.
        assertTrue(vocal.width < deep.width)
        // Quiet is a near-still, faint rest state.
        assertEquals(0f, quiet.fold, 0f)
        assertTrue(quiet.flare < 0.2f && quiet.speed < 0.3f)
        // Full mix wears the widest corona of all.
        val maxFlare = SoundClass.values().maxOf { ClassReaction.forClass(it).flare }
        assertEquals(maxFlare, full.flare, 0f)
        // High bass sparkles more than deep bass (tighter, brighter ripples).
        assertTrue(high.sparkle > deep.sparkle)
        assertTrue(high.speed > deep.speed)
    }

    @Test fun morphConvergesTowardTarget() {
        val follower = ClassReactionFollower()
        val goal = ClassReaction.forClass(SoundClass.DEEP_BASS)
        repeat(120) { follower.step(SoundClass.DEEP_BASS, 1f / 60f) } // 2s ≫ 220ms
        assertEquals(goal.fold, follower.reaction.fold, 0.02f)
        assertEquals(goal.flare, follower.reaction.flare, 0.02f)
        assertEquals(goal.width, follower.reaction.width, 0.02f)
    }

    @Test fun morphStartsAtRestAndGlidesNotSnaps() {
        val follower = ClassReactionFollower()
        assertEquals(ClassReaction.forClass(SoundClass.LOW_SOUND), follower.reaction)
        follower.step(SoundClass.VOCAL_ONLY, 1f / 60f)
        val goal = ClassReaction.forClass(SoundClass.VOCAL_ONLY)
        // One frame in, the character has moved but not arrived — a glide.
        assertTrue(follower.reaction.breathe > 0f && follower.reaction.breathe < goal.breathe)
    }

    @Test fun resetReturnsToRestCharacter() {
        val follower = ClassReactionFollower()
        repeat(60) { follower.step(SoundClass.FULL_MIX, 1f / 60f) }
        follower.reset()
        assertEquals(ClassReaction.forClass(SoundClass.LOW_SOUND), follower.reaction)
    }
}
