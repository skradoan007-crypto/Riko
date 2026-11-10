package com.rikodev.music.android.dna.core

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicLongArray

// ─────────────────────────────────────────────────────────────────────────────
// RIKO DNA (MDNA) — SPECTRUM HISTORY RING (time-addressed, single producer)
//
// The audio thread appends timestamped band frames; the render thread asks for
// "the spectrum as of time T" and gets a linearly interpolated frame. That is the
// whole latency-compensation trick: the tap runs AHEAD of the speaker by the
// output latency L, so the renderer asks for (now − L) — the sound the listener
// is hearing RIGHT NOW — at its own vsync cadence instead of at the DSP cadence.
//
// CONCURRENCY (one producer thread, one consumer thread):
//   - producer: write slot data, write its timestamp, THEN publish `head`
//     (volatile release). Timestamps live in an AtomicLongArray because a plain
//     64-bit read can tear on 32-bit ARM phones.
//   - consumer: reads `head` (acquire) and only touches slots at least GUARD
//     frames away from the one being overwritten, so a read can never overlap a
//     write unless the producer laps it by GUARD frames inside microseconds.
//   - reset: a timestamp FLOOR (no head rewind), so a seek never races a read.
//
// Pure + Android-free (unit-tested on the JVM).
// ─────────────────────────────────────────────────────────────────────────────
class SpectrumHistoryRing(val bandCount: Int, val capacity: Int = 64) {

    init {
        require(bandCount > 0) { "bandCount must be positive" }
        require(capacity >= GUARD * 2) { "capacity too small" }
    }

    private val levels = Array(capacity) { FloatArray(bandCount) }
    private val stamps = AtomicLongArray(capacity)
    private val head = AtomicLong(0L)

    @Volatile private var floorNanos = Long.MIN_VALUE

    /** Producer thread only (the audio thread). */
    fun publish(src: FloatArray, timestampNanos: Long) {
        val h = head.get()
        val slot = (h % capacity).toInt()
        val dst = levels[slot]
        val n = if (src.size < bandCount) src.size else bandCount
        System.arraycopy(src, 0, dst, 0, n)
        if (n < bandCount) java.util.Arrays.fill(dst, n, bandCount, 0f)
        stamps.set(slot, timestampNanos)
        head.set(h + 1)
    }

    /** Any thread: frames stamped before [nowNanos] are ignored from now on. */
    fun markReset(nowNanos: Long) {
        floorNanos = nowNanos
    }

    /**
     * Consumer thread only. Writes the spectrum as of [targetNanos] into [out]
     * (interpolated between the two bracketing frames) and returns true.
     *
     * Returns false — and fills [out] with zeros — when there is nothing
     * trustworthy to show: empty ring, everything older than the last reset, or
     * the newest frame is more than [STALE_NS] older than the target (playback
     * stopped). While the target is merely AHEAD of the newest frame (the audio
     * tail still playing out after a pause) the newest frame is held.
     *
     * A read is VALIDATED after the fact: if the producer lapped the slots we
     * just read (consumer preempted for a whole ring), the result is discarded
     * and re-taken, so a torn frame can never reach the screen.
     */
    fun sample(targetNanos: Long, out: FloatArray): Boolean {
        var attempt = 0
        while (attempt < MAX_ATTEMPTS) {
            when (trySample(targetNanos, out)) {
                OK -> return true
                NONE -> return none(out)
            }
            attempt++
        }
        return none(out)
    }

    private fun trySample(targetNanos: Long, out: FloatArray): Int {
        val h = head.get()
        if (h == 0L) return NONE
        val floor = floorNanos

        val newest = h - 1
        val newestTs = stamps.get(slotOf(newest))
        if (newestTs < floor || targetNanos - newestTs > STALE_NS) return NONE

        var minUsed: Long
        if (targetNanos >= newestTs) {
            minUsed = newest
            copy(slotOf(newest), out)
        } else {
            val oldest = if (h - (capacity - GUARD) > 0L) h - (capacity - GUARD) else 0L
            var i = newest
            while (i > oldest && stamps.get(slotOf(i)) > targetNanos) i--
            minUsed = i

            val tsA = stamps.get(slotOf(i))
            if (tsA > targetNanos) {
                // Target is older than everything we may safely read: oldest frame wins.
                copy(slotOf(i), out)
            } else if (tsA < floor) {
                // Frame A predates the reset; B is the first valid one.
                copy(slotOf(i + 1), out)
            } else {
                val b = slotOf(i + 1)
                val tsB = stamps.get(b)
                val span = (tsB - tsA).coerceAtLeast(1L)
                val t = ((targetNanos - tsA).toFloat() / span.toFloat()).coerceIn(0f, 1f)
                lerp(slotOf(i), b, t, out)
            }
        }

        // Validation. getAndAdd(0) is a volatile read-MODIFY-write: it also orders the
        // plain float reads above BEFORE it (a bare volatile read would not).
        val after = head.getAndAdd(0L)
        return if (after - minUsed >= capacity) RETRY else OK
    }

    private fun slotOf(index: Long): Int = (index % capacity).toInt()

    private fun none(out: FloatArray): Boolean {
        java.util.Arrays.fill(out, 0f)
        return false
    }

    private fun copy(slot: Int, out: FloatArray) {
        val n = if (out.size < bandCount) out.size else bandCount
        System.arraycopy(levels[slot], 0, out, 0, n)
    }

    private fun lerp(a: Int, b: Int, t: Float, out: FloatArray) {
        val la = levels[a]
        val lb = levels[b]
        val n = if (out.size < bandCount) out.size else bandCount
        for (k in 0 until n) out[k] = la[k] + (lb[k] - la[k]) * t
    }

    companion object {
        /** Slots kept clear of the producer's write position. */
        const val GUARD = 4

        /** Newest frame older than this (relative to the target) = playback stopped. */
        const val STALE_NS = 300_000_000L

        private const val MAX_ATTEMPTS = 3
        private const val NONE = 0
        private const val OK = 1
        private const val RETRY = 2
    }
}
