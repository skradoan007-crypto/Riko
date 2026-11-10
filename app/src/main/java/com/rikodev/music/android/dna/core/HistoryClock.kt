package com.rikodev.music.android.dna.core

// ─────────────────────────────────────────────────────────────────────────────
// RIKO DNA (MDNA) — HISTORY CLOCK
//
// Templates like Dot Terrain show the RECENT PAST as rows. Rows are pushed on a
// fixed TIME cadence (not per rendered frame) so the terrain scrolls at the same
// speed at 60Hz, 120Hz or when frames hitch; [frac] (0..1) is how far the next
// row has progressed, letting the shader scroll continuously between pushes.
//
// Pure + Android-free (unit-tested on the JVM).
// ─────────────────────────────────────────────────────────────────────────────
class HistoryClock(val rowIntervalSec: Float = DEFAULT_INTERVAL_SEC) {

    private var acc = 0f

    /** Progress toward the next row, 0 ≤ frac < 1. */
    var frac = 0f
        private set

    /** Advances by [dtSec] and returns how many rows to push now (0..[MAX_ROWS_PER_FRAME]). */
    fun advance(dtSec: Float): Int {
        acc += dtSec.coerceIn(0f, MAX_DT_SEC)
        var rows = 0
        while (acc >= rowIntervalSec && rows < MAX_ROWS_PER_FRAME) {
            acc -= rowIntervalSec
            rows++
        }
        if (acc >= rowIntervalSec) acc = 0f      // never accumulate a backlog
        frac = acc / rowIntervalSec
        return rows
    }

    fun reset() {
        acc = 0f
        frac = 0f
    }

    companion object {
        const val DEFAULT_INTERVAL_SEC = 1f / 24f
        const val MAX_ROWS_PER_FRAME = 4
        private const val MAX_DT_SEC = 0.1f

        /** Float 0..1 → unsigned byte 0..255 (clamped), one history texel per band. */
        fun quantize(levels: FloatArray, out: ByteArray) {
            val n = if (levels.size < out.size) levels.size else out.size
            for (i in 0 until n) {
                val v = levels[i]
                val c = if (v < 0f) 0f else if (v > 1f) 1f else v
                out[i] = (c * 255f + 0.5f).toInt().toByte()
            }
            for (i in n until out.size) out[i] = 0
        }
    }
}
