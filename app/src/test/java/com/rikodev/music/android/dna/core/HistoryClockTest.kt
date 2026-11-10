package com.rikodev.music.android.dna.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryClockTest {

    private fun rowsPerSecond(fps: Float): Int {
        val clock = HistoryClock()
        var rows = 0
        val dt = 1f / fps
        var t = 0f
        while (t < 1f) {
            rows += clock.advance(dt)
            t += dt
        }
        return rows
    }

    @Test
    fun rowRate_isIndependentOfFrameRate() {
        for (fps in listOf(30f, 60f, 90f, 120f, 144f)) {
            val rows = rowsPerSecond(fps)
            assertTrue("at $fps fps got $rows rows/s", rows in 23..25)
        }
    }

    @Test
    fun frac_staysInUnitRange() {
        val clock = HistoryClock()
        var t = 0f
        while (t < 3f) {
            clock.advance(0.007f)
            assertTrue("frac=${clock.frac}", clock.frac >= 0f && clock.frac < 1f)
            t += 0.007f
        }
    }

    @Test
    fun hitch_isCapped_andLeavesNoBacklog() {
        val clock = HistoryClock()
        val rows = clock.advance(0.5f)
        assertTrue(rows in 1..HistoryClock.MAX_ROWS_PER_FRAME)
        assertEquals(0, clock.advance(0.001f))
    }

    @Test
    fun quantize_mapsUnitRangeToBytes_andClamps() {
        val out = ByteArray(5) { 7 }
        HistoryClock.quantize(floatArrayOf(0f, 1f, 0.5f, -3f), out)
        assertEquals(0, out[0].toInt() and 0xFF)
        assertEquals(255, out[1].toInt() and 0xFF)
        assertEquals(128, out[2].toInt() and 0xFF)
        assertEquals(0, out[3].toInt() and 0xFF)
        assertEquals("tail beyond the source is zeroed", 0, out[4].toInt() and 0xFF)
        HistoryClock.quantize(floatArrayOf(9f), out)
        assertEquals(255, out[0].toInt() and 0xFF)
    }
}
