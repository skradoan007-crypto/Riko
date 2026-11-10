package com.rikodev.music.android.dna.core

// ─────────────────────────────────────────────────────────────────────────────
// RIKO DNA (MDNA) — BAND SOURCES
//
// The renderer asks a BandSource for "the spectrum right now" once per vsync.
// Where the numbers come from (and how latency is compensated) is the source's
// business — so the A/B-able pipelines share one renderer.
//
// CONTRACT: [sample] is called from the render thread only and ALWAYS fills
// [out] (zeros when there is no data). The return value says whether the
// numbers are live.
// ─────────────────────────────────────────────────────────────────────────────
interface BandSource {
    val bandCount: Int
    fun sample(nowNanos: Long, out: FloatArray): Boolean
}

/** Latency between "analysed in the tap" and "audible from the speaker". */
fun interface LatencyProvider {
    fun latencyNanos(): Long
}

/** Production path: history ring + latency compensation (audio-thread tap). */
class LatencyCompensatedSource(
    private val ring: SpectrumHistoryRing,
    private val latency: LatencyProvider
) : BandSource {
    override val bandCount: Int get() = ring.bandCount
    override fun sample(nowNanos: Long, out: FloatArray): Boolean =
        ring.sample(nowNanos - latency.latencyNanos(), out)
}

/** Legacy / demo path: newest frame from a [SpectrumTripleBuffer], no timing logic. */
class TripleBufferSource(private val bus: SpectrumTripleBuffer) : BandSource {
    private val held = FloatArray(bus.size)
    override val bandCount: Int get() = bus.size
    override fun sample(nowNanos: Long, out: FloatArray): Boolean {
        if (bus.acquire()) System.arraycopy(bus.frontLevels, 0, held, 0, held.size)
        val n = if (out.size < held.size) out.size else held.size
        System.arraycopy(held, 0, out, 0, n)
        return true
    }
}

/** Lets the lab swap pipelines at runtime without touching the renderer. */
class SwitchableSource(initial: BandSource) : BandSource {
    @Volatile var delegate: BandSource = initial
    override val bandCount: Int = initial.bandCount
    override fun sample(nowNanos: Long, out: FloatArray): Boolean = delegate.sample(nowNanos, out)
}
