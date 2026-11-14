package com.rikodev.music.android.dna.core

import kotlin.math.pow

/** Render-thread state. No demo clock, decoder, database or offline fallback. */
class LiveDotInput(private val source: BandSource) {
    init { require(source.bandCount == 40) }
    private val target = FloatArray(40)
    private val fastFollower = BandFollower(40)
    private val trailFollower = BandFollower(40)
    val fast: FloatArray get() = fastFollower.out
    val trail: FloatArray get() = trailFollower.out
    var hasSignal = false
        private set

    // ── v2 energy envelopes: asymmetric attack/decay physics ───────────
    // Fast attack keeps hits punchy, slow release glides back to the rim —
    // time constants (not per-frame rates), so 60Hz and 120Hz feel identical.
    private val envTarget = FloatArray(1)
    private val bassFollower = BandFollower(1)
    private val midFollower = BandFollower(1)
    private val punchEnvelope = BeatEnvelope()

    /** Band split for the bass envelope (≈250Hz @ 44.1kHz); renderer updates it. */
    var bassEnd = 11

    /** Band split for the mid envelope (≈4kHz @ 44.1kHz); renderer updates it. */
    var midEnd = 29

    /** Smoothed bass envelope 0..1 (20Hz–250Hz feel): ~28ms attack, ~200ms release. */
    val bass: Float get() = bassFollower.out[0]

    /** Smoothed mid envelope 0..1 (250Hz–4kHz feel): ~35ms attack, ~220ms release. */
    val mid: Float get() = midFollower.out[0]

    /** Beat punch 0..1 — instant attack on a rise above its own slow average. */
    val punch: Float get() = punchEnvelope.pulse

    /** Accumulating 3D wave phase (radians), wrapped to [0, TAU). */
    var phase = 0f
        private set

    /** Slow 3D roll/twist angle (radians), wrapped to [0, TAU). */
    var roll = 0f
        private set

    fun step(nowNanos: Long, dt: Float, playing: Boolean, frozen: Boolean) {
        hasSignal = playing && source.sample(nowNanos, target)
        if (!hasSignal) target.fill(0f)
        // A pause or missing PCM settles to rest even when motion is frozen.
        if (!frozen || !hasSignal) {
            fastFollower.step(target, dt, .018f, .115f)
            trailFollower.step(target, dt, .090f, .240f)
            // Energy envelopes from the followed bands (v^4 power sum, like
            // MdnaSpectrumMath.envelope) — no raw-FFT jitter ever reaches the mesh.
            envTarget[0] = envelope(fastFollower.out, 0, bassEnd)
            bassFollower.step(envTarget, dt, .028f, .200f)
            envTarget[0] = envelope(fastFollower.out, bassEnd, midEnd)
            midFollower.step(envTarget, dt, .035f, .220f)
            punchEnvelope.step(envelope(target, 0, bassEnd), dt)
        }
        // The wave only moves while music actually plays — no autonomous drift.
        if (hasSignal && !frozen) {
            phase = wrap(phase + (2.4f + 4.8f * bass) * dt)
            roll = wrap(roll + (0.10f + 0.30f * mid) * dt)
        }
    }

    fun reset() {
        target.fill(0f); fastFollower.reset(); trailFollower.reset()
        bassFollower.reset(); midFollower.reset(); punchEnvelope.reset()
        phase = 0f; roll = 0f; hasSignal = false
    }

    /** Summed band power (v^4 → ^0.25), perceptual envelope over a band range. */
    private fun envelope(levels: FloatArray, first: Int, end: Int): Float {
        var power = 0.0
        for (i in first.coerceAtLeast(0) until end.coerceAtMost(levels.size)) {
            val v = levels[i]
            if (v.isFinite() && v > 0f) { val d = v.toDouble(); power += d * d * d * d }
        }
        return minOf(1f, power.pow(.25).toFloat())
    }

    private fun wrap(angle: Float): Float {
        val t = angle % TAU
        return if (t < 0f) t + TAU else t
    }

    private companion object { const val TAU = 6.28318530718f }
}
