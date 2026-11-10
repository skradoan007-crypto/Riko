package com.rikodev.music.android.dna.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class SpectrumHistoryRingTest {

    private val ms = 1_000_000L

    private fun ringWith(vararg framesAtMs: Pair<Long, Float>, bands: Int = 2): SpectrumHistoryRing {
        val ring = SpectrumHistoryRing(bands)
        for ((t, v) in framesAtMs) ring.publish(FloatArray(bands) { v }, t * ms)
        return ring
    }

    @Test
    fun empty_returnsFalse_andZeros() {
        val out = floatArrayOf(9f, 9f)
        assertFalse(SpectrumHistoryRing(2).sample(100 * ms, out))
        assertEquals(0f, out[0], 0f)
    }

    @Test
    fun interpolates_betweenBracketingFrames() {
        val ring = ringWith(100L to 0f, 120L to 1f)
        val out = FloatArray(2)
        assertTrue(ring.sample(110 * ms, out))
        assertEquals(0.5f, out[0], 1e-4f)
        assertTrue(ring.sample(105 * ms, out))
        assertEquals(0.25f, out[0], 1e-4f)
    }

    @Test
    fun exactTimestamp_returnsThatFrame() {
        val ring = ringWith(100L to 0.2f, 120L to 0.8f, 140L to 0.4f)
        val out = FloatArray(2)
        assertTrue(ring.sample(120 * ms, out))
        assertEquals(0.8f, out[0], 1e-4f)
    }

    @Test
    fun targetAheadOfNewest_holdsNewest_untilStale() {
        val ring = ringWith(100L to 0.3f, 120L to 0.9f)
        val out = FloatArray(2)
        assertTrue(ring.sample(200 * ms, out))               // 80ms ahead: audio tail still playing
        assertEquals(0.9f, out[0], 1e-4f)
        assertFalse(ring.sample(120 * ms + 301 * ms, out))   // > STALE: playback stopped
        assertEquals(0f, out[0], 0f)
    }

    @Test
    fun targetOlderThanEverything_returnsOldestTrusted() {
        val ring = ringWith(100L to 0.3f, 120L to 0.9f)
        val out = FloatArray(2)
        assertTrue(ring.sample(10 * ms, out))
        assertEquals(0.3f, out[0], 1e-4f)
    }

    @Test
    fun reset_ignoresFramesBeforeTheFloor() {
        val ring = ringWith(100L to 0.5f, 120L to 0.7f)
        ring.markReset(130 * ms)
        val out = FloatArray(2)
        assertFalse(ring.sample(125 * ms, out))
        ring.publish(floatArrayOf(0.1f, 0.1f), 140 * ms)
        ring.publish(floatArrayOf(0.3f, 0.3f), 160 * ms)
        assertTrue(ring.sample(150 * ms, out))
        assertEquals(0.2f, out[0], 1e-4f)
        // Target straddling the reset must not blend the old track into the new one.
        assertTrue(ring.sample(135 * ms, out))
        assertEquals(0.1f, out[0], 1e-4f)
    }

    @Test
    fun wrapAround_keepsInterpolatingOverTheNewestWindow() {
        val ring = SpectrumHistoryRing(1, capacity = 16)
        for (k in 0 until 100) ring.publish(floatArrayOf(k.toFloat()), k * 16 * ms)
        val out = FloatArray(1)
        // ~ 8 frames behind the newest, between frames 91 and 92.
        assertTrue(ring.sample((91 * 16 + 8) * ms, out))
        assertEquals(91.5f, out[0], 1e-3f)
    }

    @Test
    fun latencyCompensatedSource_asksForNowMinusLatency() {
        val ring = ringWith(1_000L to 0f, 1_100L to 1f, 1_200L to 0f)
        val src = LatencyCompensatedSource(ring) { 100 * ms }
        val out = FloatArray(2)
        assertTrue(src.sample(1_150 * ms, out))   // now 1150 − 100ms latency = target 1050
        assertEquals(0.5f, out[0], 1e-4f)
    }

    @Test
    fun concurrent_producerConsumer_neverReturnsTornData() {
        val bands = 40
        val frames = 40_000
        val ring = SpectrumHistoryRing(bands)
        val published = java.util.concurrent.atomic.AtomicInteger(0)
        val failure = AtomicReference<String?>(null)

        // Frame k: every band == k, timestamp == k * 1000ns. ANY consistent sample (a
        // blend of two adjacent frames, or one frame) is therefore uniform across all
        // bands. A torn read mixes frames and shows up as a non-uniform vector.
        // The producer runs far faster (~3µs/frame) than any real DSP tap (~16ms), so
        // this deliberately stresses the lap-around case.
        val producer = Thread {
            val src = FloatArray(bands)
            for (k in 1..frames) {
                java.util.Arrays.fill(src, k.toFloat())
                ring.publish(src, k * 1000L)
                published.set(k)
                val until = System.nanoTime() + 3_000L
                while (System.nanoTime() < until) { /* spin */ }
            }
        }

        val out = FloatArray(bands)
        var checked = 0
        var accurate = 0
        producer.start()
        while (published.get() < frames) {
            val p = published.get()
            if (p < 20) continue
            val target = (p - 8) * 1000L + 500L
            if (!ring.sample(target, out)) { failure.set("live ring reported no data"); break }
            for (k in 1 until bands) {
                if (kotlin.math.abs(out[k] - out[0]) > 1e-3f) {
                    failure.set("torn sample: band0=${out[0]} band$k=${out[k]}")
                    break
                }
            }
            if (failure.get() != null) break
            if (kotlin.math.abs(out[0] - target / 1000f) < 0.05f) accurate++
            checked++
        }
        producer.join()
        assertEquals(null, failure.get())
        assertTrue("checked only $checked samples", checked > 100)
        assertTrue("only $accurate of $checked samples were accurate", accurate * 10 > checked * 9)
    }
}
