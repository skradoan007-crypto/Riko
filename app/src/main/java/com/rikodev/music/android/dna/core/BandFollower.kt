package com.rikodev.music.android.dna.core

import kotlin.math.exp

// ─────────────────────────────────────────────────────────────────────────────
// RIKO DNA (MDNA) — TIME-BASED BAND FOLLOWER + BEAT ENVELOPE
//
// The DSP tap delivers frames at ~22–40Hz; the display refreshes at 60–120Hz.
// The follower runs every RENDER frame and eases the drawn levels toward the
// newest DSP frame with attack/release TIME CONSTANTS (seconds), not per-update
// rates — so the feel is identical at 60Hz and 120Hz and never steps at the
// DSP cadence. Fast attack keeps hits punchy, slower release keeps it silky.
//
// Pure + Android-free (unit-tested on the JVM).
// ─────────────────────────────────────────────────────────────────────────────
class BandFollower(val size: Int) {

    /** Currently drawn levels, 0..1. Read-only by contract. */
    val out = FloatArray(size)

    fun step(target: FloatArray, dtSec: Float, attackSec: Float, releaseSec: Float) {
        val dt = dtSec.coerceIn(0f, MAX_DT)
        val ka = coeff(dt, attackSec)
        val kr = coeff(dt, releaseSec)
        val n = if (target.size < size) target.size else size
        for (i in 0 until n) {
            val t = target[i]
            val c = out[i]
            out[i] = c + (t - c) * (if (t > c) ka else kr)
        }
    }

    fun reset() = out.fill(0f)

    companion object {
        const val MAX_DT = 0.1f

        /** 1 - e^(-dt/tau); snaps to the target when tau is ~0. */
        fun coeff(dtSec: Float, tauSec: Float): Float =
            if (tauSec <= 1e-5f) 1f else 1f - exp(-dtSec / tauSec)
    }
}

/**
 * Turns a bass-energy signal into a 0..1 "punch": instant attack on a rise
 * above its own slow average, short release. Drives scale/size/bloom kicks.
 */
class BeatEnvelope {
    private var fast = 0f
    private var slow = 0f

    /** Latest punch value, 0..1. */
    var pulse = 0f
        private set

    fun step(energy: Float, dtSec: Float): Float {
        val dt = dtSec.coerceIn(0f, BandFollower.MAX_DT)
        fast += (energy - fast) * BandFollower.coeff(dt, 0.030f)
        slow += (energy - slow) * BandFollower.coeff(dt, 0.350f)
        val raw = ((fast - slow) * GAIN).coerceIn(0f, 1f)
        pulse = if (raw > pulse) raw else pulse + (raw - pulse) * BandFollower.coeff(dt, 0.120f)
        return pulse
    }

    fun reset() {
        fast = 0f
        slow = 0f
        pulse = 0f
    }

    private companion object {
        const val GAIN = 3.5f
    }
}
