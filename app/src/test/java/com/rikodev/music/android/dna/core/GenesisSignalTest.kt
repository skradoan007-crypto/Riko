package com.rikodev.music.android.dna.core

import org.junit.Assert.*
import org.junit.Test

class GenesisSignalTest {
    @Test fun clockFreezesAndDiscardsBackgroundGap() {
        val clock = GenesisFrameClock()
        clock.step(1_000_000_000, true)
        clock.step(1_500_000_000, true)
        assertEquals(0.1f, clock.seconds, 0.0001f)
        clock.step(2_000_000_000, false)
        assertEquals(0.1f, clock.seconds, 0.0001f)
        clock.resume()
        assertEquals(0f, clock.step(20_000_000_000, true), 0f)
        assertEquals(0.1f, clock.seconds, 0.0001f)
    }
    @Test fun demoPauseReturnsSilenceAndDoesNotJumpOnResume() {
        val source = GenesisDemoSource()
        val out = FloatArray(40)
        assertTrue(source.sample(1_000_000_000, out))
        val first = out.copyOf()
        source.paused = true
        assertFalse(source.sample(20_000_000_000, out))
        assertTrue(out.all { it == 0f })
        source.paused = false
        source.sample(20_000_000_000, out)
        assertArrayEquals(first, out, 0.00001f)
    }
    @Test fun demoDiscardsLifecycleGap() {
        val source = GenesisDemoSource()
        val out = FloatArray(40)
        source.sample(1_000_000_000, out)
        val before = out.copyOf()
        source.resumeClock()
        source.sample(60_000_000_000, out)
        assertArrayEquals(before, out, 0f)
    }
    @Test fun followerRejectsNonFiniteSignalAndRemainsBounded() {
        val follower = BandFollower(4)
        follower.step(floatArrayOf(Float.NaN, Float.POSITIVE_INFINITY, -3f, 4f), 0.1f, 0f, 0f)
        assertArrayEquals(floatArrayOf(0f, 0f, 0f, 1f), follower.out, 0f)
    }
    @Test fun resetNeverMovesBackwardAndClearsOversizedOutputTail() {
        val ring = SpectrumHistoryRing(1)
        ring.publish(floatArrayOf(1f), 100)
        ring.markReset(200)
        ring.markReset(50)
        val out = FloatArray(4) { 9f }
        assertFalse(ring.sample(100, out))
        ring.publish(floatArrayOf(0.5f), 300)
        assertTrue(ring.sample(300, out))
        assertArrayEquals(floatArrayOf(0.5f, 0f, 0f, 0f), out, 0f)
    }
    @Test fun sparseNewTrackDoesNotInterpolatePreviousTrack() {
        val ring = SpectrumHistoryRing(2, 8)
        repeat(40) { ring.publish(floatArrayOf(1f, 1f), it.toLong()) }
        ring.markReset(40)
        ring.publish(floatArrayOf(0.2f, 0.2f), 41)
        val out = FloatArray(2)
        assertTrue(ring.sample(40, out))
        assertArrayEquals(floatArrayOf(0.2f, 0.2f), out, 0f)
    }
    @Test fun concurrentTinyRingAcceptsOnlyCoherentFrames() {
        val ring = SpectrumHistoryRing(40, 8)
        val done = java.util.concurrent.atomic.AtomicBoolean(false)
        val published = java.util.concurrent.atomic.AtomicLong(0L)
        val producer = Thread {
            val input = FloatArray(40)
            repeat(60_000) { k ->
                input.fill(k.toFloat()); ring.publish(input, k.toLong()); published.set(k.toLong())
            }
            done.set(true)
        }
        producer.start()
        val out = FloatArray(40)
        var reads = 0
        while (!done.get()) {
            if (ring.sample(published.get(), out)) {
                assertTrue(out.all { it == out[0] })
                reads++
            }
        }
        producer.join()
        assertTrue(ring.sample(published.get(), out))
        assertTrue(out.all { it == 59_999f })
    }
}
