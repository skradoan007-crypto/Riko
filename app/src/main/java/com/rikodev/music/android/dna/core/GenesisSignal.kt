package com.rikodev.music.android.dna.core

/** Render-thread demo clock. It never injects synthetic data into the live pipeline. */
class GenesisDemoSource(override val bandCount: Int = 40) : BandSource {
    @Volatile var paused = false
    private var previous = 0L
    private var seconds = 0f
    @Volatile private var restartClock = false
    fun resumeClock() { restartClock = true }

    override fun sample(nowNanos: Long, out: FloatArray): Boolean {
        if (restartClock) { previous = 0L; restartClock = false }
        if (previous != 0L && !paused) {
            seconds += ((nowNanos - previous) / 1e9f).coerceIn(0f, 0.1f)
        }
        previous = nowNanos
        if (paused) { out.fill(0f); return false }
        SyntheticSpectrum.fill(out, seconds)
        return true
    }
}

/** Bounded frame clock: presentation cadence is distinct from audio timestamps. */
class GenesisFrameClock {
    private var previous = 0L
    var seconds = 0f
        private set
    var intervalMs = 0f
        private set

    fun step(nowNanos: Long, moving: Boolean): Float {
        val dt = if (previous == 0L) 0f else ((nowNanos - previous) / 1e9f).coerceAtLeast(0f)
        previous = nowNanos
        intervalMs = dt * 1000f
        val bounded = dt.coerceAtMost(0.1f)
        if (moving) seconds += bounded
        return bounded
    }
    fun resume() { previous = 0L }
}
