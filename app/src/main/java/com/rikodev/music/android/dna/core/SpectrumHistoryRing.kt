package com.rikodev.music.android.dna.core

import java.util.concurrent.atomic.AtomicIntegerArray
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicLongArray

/**
 * Bounded single-producer time-addressed spectrum history. Each slot carries its own
 * sequence stamp: readers reject an in-progress overwrite, even before head advances.
 * Atomic float bits avoid plain-array data races on ARM. No lock or allocation in publish/sample.
 * Each consumer owns its output array; multiple read-only consumers are safe.
 */
class SpectrumHistoryRing(val bandCount: Int, val capacity: Int = 64) {
    init { require(bandCount > 0); require(capacity >= GUARD * 2) }
    private val values = AtomicIntegerArray(capacity * bandCount)
    private val stamps = AtomicLongArray(capacity)
    private val sequences = AtomicLongArray(capacity)
    private val head = AtomicLong(0)
    private val floor = AtomicLong(Long.MIN_VALUE)

    fun publish(src: FloatArray, timestampNanos: Long) {
        val h = head.get()
        val slot = slotOf(h)
        sequences.set(slot, h * 2 + 1) // odd: in progress
        for (i in 0 until bandCount) {
            values.set(slot * bandCount + i, (if (i < src.size) src[i] else 0f).toRawBits())
        }
        stamps.set(slot, timestampNanos)
        sequences.set(slot, h * 2 + 2) // even: complete
        head.set(h + 1)
    }

    /** Monotonic floor prevents a concurrent older reset from resurrecting stale frames. */
    fun markReset(nowNanos: Long) {
        var old = floor.get()
        while (nowNanos > old && !floor.compareAndSet(old, nowNanos)) old = floor.get()
    }

    fun sample(targetNanos: Long, out: FloatArray): Boolean {
        repeat(3) {
            val h = head.get()
            if (h == 0L) return none(out)
            val reset = floor.get()
            val newest = h - 1
            val latestTime = stamps.get(slotOf(newest))
            if (latestTime < reset || targetNanos - latestTime > STALE_NS) return none(out)
            val oldest = (h - (capacity - GUARD)).coerceAtLeast(0L)
            var a = newest
            while (a > oldest && stamps.get(slotOf(a)) > targetNanos) a--
            var b = (a + 1).coerceAtMost(newest)
            val ta = stamps.get(slotOf(a))
            val tb = stamps.get(slotOf(b))
            if (ta < reset) a = b
            if (ta >= targetNanos || a == newest) b = a
            val sa = slotOf(a)
            val sb = slotOf(b)
            val va = a * 2 + 2
            val vb = b * 2 + 2
            if (sequences.get(sa) != va || sequences.get(sb) != vb) return@repeat
            val t = if (a == b) 0f else
                ((targetNanos - ta).toFloat() / (tb - ta).coerceAtLeast(1L)).coerceIn(0f, 1f)
            for (i in out.indices) {
                out[i] = if (i >= bandCount) 0f else {
                    val x = Float.fromBits(values.get(sa * bandCount + i))
                    val y = Float.fromBits(values.get(sb * bandCount + i))
                    x + (y - x) * t
                }
            }
            if (sequences.get(sa) == va && sequences.get(sb) == vb && floor.get() == reset &&
                stamps.get(sa) >= reset && stamps.get(sb) >= reset) return true
        }
        return none(out)
    }

    private fun slotOf(index: Long) = (index % capacity).toInt()
    private fun none(out: FloatArray): Boolean { out.fill(0f); return false }

    companion object {
        const val GUARD = 4
        const val STALE_NS = 300_000_000L
    }
}
