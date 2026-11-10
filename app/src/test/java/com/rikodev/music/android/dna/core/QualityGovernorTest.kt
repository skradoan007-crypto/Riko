package com.rikodev.music.android.dna.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QualityGovernorTest {

    private fun runFor(g: QualityGovernor, seconds: Float, dtMs: Float, canStepDown: Boolean = true): Boolean {
        var t = 0f
        while (t < seconds * 1000f) {
            if (g.onFrame(dtMs, canStepDown)) return true
            t += dtMs
        }
        return false
    }

    @Test
    fun healthyFrames_neverStepDown() {
        val g = QualityGovernor(60f)
        assertFalse(runFor(g, 30f, 16.7f))
    }

    @Test
    fun sustainedSlowFrames_stepDown_afterWarmup() {
        val g = QualityGovernor(60f)
        assertFalse("warm-up must be respected", runFor(g, 2.0f, 40f))
        assertTrue(runFor(g, 10f, 40f))
    }

    @Test
    fun hitches_areIgnored() {
        val g = QualityGovernor(60f)
        runFor(g, 3f, 16.7f)
        repeat(50) { assertFalse(g.onFrame(900f, true)) }
    }

    @Test
    fun atTheFloor_neverSignals() {
        val g = QualityGovernor(60f)
        assertFalse(runFor(g, 20f, 50f, canStepDown = false))
    }

    @Test
    fun disabled_neverSignals() {
        val g = QualityGovernor(60f)
        g.enabled = false
        assertFalse(runFor(g, 20f, 50f))
    }

    @Test
    fun highRefreshDisplay_dropsTo60fps_isTreatedAsTooSlow() {
        val g = QualityGovernor(120f)       // budget 8.3ms → 16.7ms frames are 2x over
        assertTrue(runFor(g, 12f, 16.7f))
    }

    @Test
    fun afterAChange_waitsForWarmupAgain() {
        val g = QualityGovernor(60f)
        assertTrue(runFor(g, 12f, 40f))
        assertFalse(runFor(g, 2.0f, 40f))
    }
}
