package com.rikodev.music.android.audiofx.visualizer

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — SPECTRUM FRAME DELAY LINE (A/V sync compensation)
//
// The PCM tap sits INSIDE ExoPlayer's processor chain, i.e. BEFORE the
// AudioSink, so analysis always runs ahead of what the speaker is actually
// playing by roughly one AudioTrack buffer. Without compensation the bars
// move early — a real, visible sync error on a kick drum.
//
// This is a fixed-length ring of preallocated band snapshots: each push writes
// one slot and (once primed) reports the slot from (slots-1) emissions ago,
// which is exactly the delay we want to publish. Zero allocation per frame.
//
// Publishing a frame is a VISUALIZATION-only concern. Nothing here touches
// playback timing, and the constant it is sized from lives in SpectrumConfig.
//
// Pure + Android-free so the ring semantics are unit-tested on the JVM.
// ─────────────────────────────────────────────────────────────────────────────

class SpectrumFrameDelay(val bandCount: Int, slotCount: Int) {

    init {
        require(bandCount > 0) { "bandCount must be positive" }
        require(slotCount >= 2) { "a delay line needs at least 2 slots" }
    }

    val slotCount: Int = slotCount

    private val levels = Array(slotCount) { FloatArray(bandCount) }
    private val peaks = Array(slotCount) { FloatArray(bandCount) }
    private var writeIndex = 0
    private var filled = 0

    /**
     * Stores one frame and returns the slot that is now due, or [NOT_READY]
     * while the line is still priming (the first `slots - 1` pushes).
     *
     * The returned slot's arrays are reused on the next wrap, so the caller
     * must COPY what it needs (SpectrumFrame already snapshots via copyOf()).
     */
    fun push(frameLevels: FloatArray, framePeaks: FloatArray): Int {
        val write = writeIndex
        frameLevels.copyInto(levels[write], 0, 0, bandCount)
        framePeaks.copyInto(peaks[write], 0, 0, bandCount)
        // Slots are written in order, so the next slot to be overwritten holds
        // the OLDEST frame — that is the one (slotCount - 1) emissions old.
        val due = if (write + 1 == slotCount) 0 else write + 1
        writeIndex = due
        filled++
        return if (filled >= slotCount) due else NOT_READY
    }

    /** Levels for a slot previously returned by [push]. Read-only by contract. */
    fun levelsAt(slot: Int): FloatArray = levels[slot]

    /** Peaks for a slot previously returned by [push]. Read-only by contract. */
    fun peaksAt(slot: Int): FloatArray = peaks[slot]

    /** Forget everything — call on seek / track change / pipeline reset. */
    fun clear() {
        for (i in 0 until slotCount) {
            levels[i].fill(0f)
            peaks[i].fill(0f)
        }
        writeIndex = 0
        filled = 0
    }

    companion object {
        /** Returned by [push] while the ring is still priming. */
        const val NOT_READY = -1

        /**
         * Slots needed to delay by [delayMs] when frames are emitted every
         * [emitIntervalNanos]. A ring of N slots delays by N-1 emissions.
         */
        fun slotsFor(delayMs: Long, emitIntervalNanos: Long): Int {
            if (delayMs <= 0L || emitIntervalNanos <= 0L) return 2
            val intervals = kotlin.math.ceil(
                delayMs.toDouble() * 1_000_000.0 / emitIntervalNanos.toDouble()
            ).toInt()
            return (intervals + 1).coerceIn(2, MAX_SLOTS)
        }

        /** Hard ceiling so a bad constant can never allocate an absurd ring. */
        const val MAX_SLOTS = 17
    }
}
