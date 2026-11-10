package com.rikodev.music.android.dna.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class SpectrumTripleBufferTest {

    @Test
    fun acquire_isFalse_beforeAnyPublish() {
        val buf = SpectrumTripleBuffer(4)
        assertFalse(buf.acquire())
    }

    @Test
    fun publish_thenAcquire_deliversFrameOnce() {
        val buf = SpectrumTripleBuffer(3)
        buf.publish(floatArrayOf(0.1f, 0.2f, 0.3f), 42L)
        assertTrue(buf.acquire())
        assertArrayEquals(floatArrayOf(0.1f, 0.2f, 0.3f), buf.frontLevels, 0f)
        assertEquals(42L, buf.frontTimestampNanos)
        assertFalse("no new frame published", buf.acquire())
        // Front stays readable after a failed acquire.
        assertArrayEquals(floatArrayOf(0.1f, 0.2f, 0.3f), buf.frontLevels, 0f)
    }

    @Test
    fun latestWins_whenConsumerIsSlow() {
        val buf = SpectrumTripleBuffer(2)
        buf.publish(floatArrayOf(1f, 1f), 1L)
        buf.publish(floatArrayOf(2f, 2f), 2L)
        buf.publish(floatArrayOf(3f, 3f), 3L)
        assertTrue(buf.acquire())
        assertArrayEquals(floatArrayOf(3f, 3f), buf.frontLevels, 0f)
        assertEquals(3L, buf.frontTimestampNanos)
        assertFalse(buf.acquire())
    }

    @Test
    fun shortSource_isZeroPadded_longSource_isTruncated() {
        val buf = SpectrumTripleBuffer(3)
        buf.publish(floatArrayOf(0.5f), 1L)
        assertTrue(buf.acquire())
        assertArrayEquals(floatArrayOf(0.5f, 0f, 0f), buf.frontLevels, 0f)
        buf.publish(floatArrayOf(9f, 8f, 7f, 6f, 5f), 2L)
        assertTrue(buf.acquire())
        assertArrayEquals(floatArrayOf(9f, 8f, 7f), buf.frontLevels, 0f)
    }

    @Test
    fun concurrent_producerConsumer_neverTears_andNeverGoesBackwards() {
        val size = 40
        val frames = 200_000
        val buf = SpectrumTripleBuffer(size)
        val done = AtomicBoolean(false)
        val failure = AtomicReference<String?>(null)

        val producer = Thread {
            val src = FloatArray(size)
            for (i in 1..frames) {
                java.util.Arrays.fill(src, i.toFloat())
                buf.publish(src, i.toLong())
            }
            done.set(true)
        }

        var last = 0f
        var reads = 0
        producer.start()
        while (true) {
            val finished = done.get()
            if (buf.acquire()) {
                reads++
                val levels = buf.frontLevels
                val v = levels[0]
                for (k in 1 until size) {
                    if (levels[k] != v) { failure.set("torn frame: [0]=$v [$k]=${levels[k]}"); break }
                }
                if (buf.frontTimestampNanos.toFloat() != v) failure.set("timestamp/levels mismatch")
                if (v < last) failure.set("went backwards: $v < $last")
                last = v
            }
            if (failure.get() != null || finished && !buf.acquire()) break
        }
        producer.join()
        assertEquals(null, failure.get())
        assertTrue("consumer should have seen frames", reads > 0)
    }
}
