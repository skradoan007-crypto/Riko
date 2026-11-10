package com.rikodev.music.android.dna.core

import java.util.concurrent.atomic.AtomicInteger

// ─────────────────────────────────────────────────────────────────────────────
// RIKO DNA (MDNA) — SPECTRUM TRIPLE BUFFER
//
// Lock-free, allocation-free handoff of the newest spectrum frame from ONE
// producer thread to ONE consumer thread (the GL render thread).
//
//   - No StateFlow, no Compose recomposition, no locks, no per-frame garbage.
//   - The consumer ALWAYS reads a complete frame (no tearing) and never blocks
//     the producer; the producer never blocks the consumer.
//   - Frames the consumer was too slow to read are simply replaced by newer
//     ones (latest-wins), which is exactly what a renderer wants.
//
// CONTRACT: exactly one thread calls [publish], exactly one thread calls
// [acquire] / reads [frontLevels]. Pure + Android-free so the semantics are
// unit-tested on the JVM (SpectrumTripleBufferTest).
// ─────────────────────────────────────────────────────────────────────────────
class SpectrumTripleBuffer(val size: Int) {

    init {
        require(size > 0) { "size must be positive" }
    }

    private val slots = Array(3) { FloatArray(size) }
    private val stamps = LongArray(3)

    // Producer-owned slot (being written).
    private var back = 0

    // Consumer-owned slot (being read).
    private var front = 2

    // Shared slot: low 2 bits = slot index, bit 2 = "fresh, not yet consumed".
    private val middle = AtomicInteger(1)

    /** Producer thread only. Copies [src] (zero-padded / truncated to [size]). */
    fun publish(src: FloatArray, timestampNanos: Long) {
        val dst = slots[back]
        val n = if (src.size < size) src.size else size
        System.arraycopy(src, 0, dst, 0, n)
        if (n < size) java.util.Arrays.fill(dst, n, size, 0f)
        stamps[back] = timestampNanos
        val prev = middle.getAndSet(back or DIRTY)
        back = prev and INDEX_MASK
    }

    /**
     * Consumer thread only. Returns true when a newer frame was swapped in;
     * [frontLevels] / [frontTimestampNanos] then describe it. Returns false
     * (and leaves the front frame untouched) when nothing new was published.
     */
    fun acquire(): Boolean {
        if ((middle.get() and DIRTY) == 0) return false
        val prev = middle.getAndSet(front)
        front = prev and INDEX_MASK
        return true
    }

    /** Consumer thread only. Read-only by contract; valid until the next [acquire]. */
    val frontLevels: FloatArray get() = slots[front]

    /** Consumer thread only. Producer-supplied timestamp of [frontLevels]. */
    val frontTimestampNanos: Long get() = stamps[front]

    private companion object {
        const val DIRTY = 0b100
        const val INDEX_MASK = 0b011
    }
}
