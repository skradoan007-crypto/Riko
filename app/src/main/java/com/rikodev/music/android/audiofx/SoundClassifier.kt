package com.rikodev.music.android.audiofx

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — SOUND CLASSIFIER (Phase 2)
//
// Turns the live 40-band spectrum into a STABLE sound type:
//   LOW_SOUND · DEEP_BASS · MID_BASS · HIGH_BASS · LOW_VOCAL · VOCAL_ONLY · FULL_MIX
//
// DESIGN:
//   - Six frequency groups (deep/mid/high bass, low vocal, vocal, treble),
//     each measured as perceptual level = (mean v^4)^0.25 over its bands —
//     group size never biases the result.
//   - Asymmetric smoothing: ~25ms attack (hits arrive fast) / ~250ms
//     release (sections glide out), time-based so 60Hz and 120Hz agree.
//   - Hysteresis: a new class must PERSIST for 180ms before adoption —
//     one-frame spikes never flip the visuals. A first real signal after
//     quiet is adopted immediately.
//
// PURE + allocation-free after construction (JVM unit tests). Safe to call
// from the audio thread: no locks, no I/O, never throws for any input.
// ─────────────────────────────────────────────────────────────────────────────
class SoundClassifier {

    // Group order: 0 deep, 1 mid, 2 high, 3 lowVocal, 4 vocal, 5 treble, 6 total.
    private val smooth = FloatArray(GROUPS)
    private val bounds = IntArray(7)
    private var cachedRate = 0

    private var lastNs = 0L
    private var current = SoundClass.LOW_SOUND
    private var pending = SoundClass.LOW_SOUND
    private var pendingSinceNs = 0L
    private var lastSwitchNs = 0L

    /**
     * Classify one spectrum frame. [levels] are perceptual band levels 0..1
     * (SpectrumBands.COUNT entries expected; any size is handled safely).
     * Returns the hysteresis-stable state including smoothed group energies.
     */
    fun step(levels: FloatArray, sampleRate: Int, timestampNanos: Long): SoundClassState {
        ensureBounds(sampleRate)
        val dt = if (lastNs == 0L) 0f else ((timestampNanos - lastNs) / 1e9f).coerceIn(0f, 0.1f)
        lastNs = timestampNanos

        // Six band ranges (deep, mid, high, lowVocal, vocal, treble).
        for (g in 0 until BAND_GROUPS) {
            smoothTo(g, level(levels, bounds[g], bounds[g + 1]), dt)
        }
        // Index 6 is the total across all bands, not a band range.
        smoothTo(TOTAL_INDEX, level(levels, 0, SpectrumBands.COUNT), dt)

        val candidate = decide()
        if (candidate == current) {
            pending = current
            pendingSinceNs = timestampNanos
        } else if (current == SoundClass.LOW_SOUND) {
            adopt(candidate, timestampNanos)
        } else if (candidate != pending) {
            pending = candidate
            pendingSinceNs = timestampNanos
        } else if (timestampNanos - pendingSinceNs >= HOLD_NS && timestampNanos - lastSwitchNs >= HOLD_NS) {
            adopt(candidate, timestampNanos)
        }

        return SoundClassState(
            soundClass = current,
            confidence = confidenceFor(current),
            deepBass = smooth[0],
            midBass = smooth[1],
            highBass = smooth[2],
            lowVocal = smooth[3],
            vocal = smooth[4],
            treble = smooth[5],
            total = smooth[6],
            timestamp = timestampNanos
        )
    }

    fun reset() {
        smooth.fill(0f)
        lastNs = 0L
        current = SoundClass.LOW_SOUND
        pending = SoundClass.LOW_SOUND
        pendingSinceNs = 0L
        lastSwitchNs = 0L
    }

    // ── Decision ladder (runs on smoothed values) ────────────────────────

    private fun decide(): SoundClass {
        val deep = smooth[0]
        val mid = smooth[1]
        val high = smooth[2]
        val lowVocal = smooth[3]
        val vocal = smooth[4]
        val treble = smooth[5]
        val total = smooth[6]

        if (total < QUIET) return SoundClass.LOW_SOUND

        val bass = max(deep, max(mid, high))
        val bassSum = deep + mid + high
        val voice = vocal + 0.6f * lowVocal

        // Everything strong AND balanced → full mix (choruses, drops).
        if (bass > 0.15f && voice > 0.15f && treble > 0.12f) {
            val weakest = min(bass, min(voice, treble))
            if (weakest > 0.35f * max(bass, max(voice, treble))) return SoundClass.FULL_MIX
        }
        // Clear vocals leading over bass → vocal only (verses, acapella).
        if (vocal > 0.15f && vocal > bass * 1.15f && vocal > lowVocal * 1.1f) return SoundClass.VOCAL_ONLY
        // Low vocal region leading → chest voice / low singing.
        if (lowVocal > 0.15f && lowVocal > bass * 1.1f && lowVocal > vocal * 0.85f) return SoundClass.LOW_VOCAL
        // Bass leads → the strongest bass region names the class.
        if (bass > 0.12f && bassSum > voice * 0.75f) return when {
            deep >= mid && deep >= high -> SoundClass.DEEP_BASS
            mid >= high -> SoundClass.MID_BASS
            else -> SoundClass.HIGH_BASS
        }
        return SoundClass.FULL_MIX
    }

    private fun confidenceFor(cls: SoundClass): Float {
        val deep = smooth[0]
        val mid = smooth[1]
        val high = smooth[2]
        val lowVocal = smooth[3]
        val vocal = smooth[4]
        val treble = smooth[5]
        val bass = max(deep, max(mid, high))
        val voice = vocal + 0.6f * lowVocal
        return when (cls) {
            SoundClass.LOW_SOUND -> (1f - smooth[6] / QUIET).coerceIn(0f, 1f)
            SoundClass.VOCAL_ONLY -> margin(vocal, max(bass, lowVocal))
            SoundClass.LOW_VOCAL -> margin(lowVocal, max(bass, vocal))
            SoundClass.DEEP_BASS -> margin(deep, max(max(mid, high), voice))
            SoundClass.MID_BASS -> margin(mid, max(max(deep, high), voice))
            SoundClass.HIGH_BASS -> margin(high, max(max(deep, mid), voice))
            SoundClass.FULL_MIX ->
                (min(bass, min(voice, treble)) / max(0.01f, max(bass, max(voice, treble))))
                    .coerceIn(0f, 1f)
        }
    }

    private fun adopt(cls: SoundClass, timestampNanos: Long) {
        current = cls
        pending = cls
        pendingSinceNs = timestampNanos
        lastSwitchNs = timestampNanos
    }

    private fun smoothTo(index: Int, target: Float, dt: Float) {
        val c = smooth[index]
        val tau = if (target > c) ATTACK_SEC else RELEASE_SEC
        smooth[index] = c + (target - c) * coeff(dt, tau)
    }

    // ── Math helpers ─────────────────────────────────────────────────────

    /** Perceptual level of a band range: (mean v^4)^0.25 — size-independent. */
    private fun level(levels: FloatArray, first: Int, end: Int): Float {
        val lo = first.coerceIn(0, levels.size)
        val hi = end.coerceIn(lo, levels.size)
        if (hi <= lo) return 0f
        var power = 0.0
        for (i in lo until hi) {
            val v = levels[i]
            if (v.isFinite() && v > 0f) { val d = v.toDouble(); power += d * d * d * d }
        }
        return min(1f, (power / (hi - lo)).pow(0.25).toFloat())
    }

    private fun margin(winner: Float, rival: Float): Float =
        ((winner - rival) / max(winner, 0.01f)).coerceIn(0f, 1f)

    private fun coeff(dtSec: Float, tauSec: Float): Float = 1f - exp(-dtSec / tauSec)

    private fun ensureBounds(sampleRate: Int) {
        val rate = sampleRate.coerceAtLeast(8000)
        if (rate == cachedRate) return
        bounds[0] = 0
        bounds[1] = SpectrumBands.bandIndex(80f, rate)
        bounds[2] = SpectrumBands.bandIndex(160f, rate)
        bounds[3] = SpectrumBands.bandIndex(300f, rate)
        bounds[4] = SpectrumBands.bandIndex(600f, rate)
        bounds[5] = SpectrumBands.bandIndex(3000f, rate)
        bounds[6] = SpectrumBands.COUNT
        cachedRate = rate
    }

    private companion object {
        const val GROUPS = 7
        const val BAND_GROUPS = 6
        const val TOTAL_INDEX = 6
        const val ATTACK_SEC = 0.025f
        const val RELEASE_SEC = 0.25f
        const val QUIET = 0.08f
        const val HOLD_NS = 180_000_000L
    }
}
