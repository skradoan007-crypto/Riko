package com.rikodev.music.android.dna.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BandFollowerTest {

    @Test
    fun riseTowardsTarget_withAttackTimeConstant() {
        val f = BandFollower(1)
        val target = floatArrayOf(1f)
        // One time-constant → 1 - 1/e ≈ 0.632
        f.step(target, 0.020f, attackSec = 0.020f, releaseSec = 0.100f)
        assertEquals(0.632f, f.out[0], 0.005f)
        repeat(20) { f.step(target, 0.016f, 0.020f, 0.100f) }
        assertTrue(f.out[0] > 0.99f)
    }

    @Test
    fun release_isSlowerThanAttack_forSameDt() {
        val up = BandFollower(1)
        up.step(floatArrayOf(1f), 0.016f, 0.012f, 0.090f)
        val down = BandFollower(1)
        down.out[0] = 1f
        down.step(floatArrayOf(0f), 0.016f, 0.012f, 0.090f)
        val risen = up.out[0]
        val fallen = 1f - down.out[0]
        assertTrue("attack moves farther than release ($risen vs $fallen)", risen > fallen)
    }

    @Test
    fun result_isFrameRateIndependent() {
        val one = BandFollower(1)
        one.step(floatArrayOf(1f), 0.016f, 0.030f, 0.100f)
        val two = BandFollower(1)
        two.step(floatArrayOf(1f), 0.008f, 0.030f, 0.100f)
        two.step(floatArrayOf(1f), 0.008f, 0.030f, 0.100f)
        assertEquals(one.out[0], two.out[0], 1e-5f)
    }

    @Test
    fun zeroTau_snapsToTarget() {
        val f = BandFollower(2)
        f.step(floatArrayOf(0.7f, 0.2f), 0.016f, 0f, 0f)
        assertEquals(0.7f, f.out[0], 1e-6f)
        assertEquals(0.2f, f.out[1], 1e-6f)
    }

    @Test
    fun hugeDt_isClamped_andShortTargetIsTolerated() {
        val f = BandFollower(3)
        f.step(floatArrayOf(1f), 5f, 0.5f, 0.5f)   // 1 target entry for 3 bands
        assertTrue(f.out[0] in 0f..1f)
        assertEquals(0f, f.out[1], 0f)
    }

    @Test
    fun beatEnvelope_punchesOnRise_andDecays() {
        val e = BeatEnvelope()
        repeat(120) { e.step(0.2f, 0.016f) }       // steady energy → no punch
        assertTrue(e.pulse < 0.05f)
        val hit = e.step(0.9f, 0.016f)             // sudden bass hit
        assertTrue("pulse should jump (was $hit)", hit > 0.3f)
        repeat(60) { e.step(0.9f, 0.016f) }        // sustained → settles back down
        assertTrue(e.pulse < hit)
    }
}
