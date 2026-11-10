package com.rikodev.music.android.audiofx

import kotlin.math.ln

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — SPECTRUM BANDS (pure DSP math for the visualizer)
//
// Zero Android, zero state-flow, zero allocation in the hot path — pure
// functions + one small stateful Smoother whose arrays are preallocated.
// Fully unit-testable (see SpectrumBandsTest).
//
// v1 spec (visualizer_plan §20): 1024 FFT → Hann → magnitude → dB
// (noise floor −60dB, ceiling 0dB) → 40 logarithmic bands (~40Hz →
// Nyquist) → attack/release smoothing + peak hold/decay.
// ─────────────────────────────────────────────────────────────────────────────

object SpectrumBands {

    /** Number of logarithmic visual bands (v1 balanced tier). */
    const val COUNT = 40

    /** Bottom of the visual range — below this everything folds into band 0. */
    const val MIN_HZ = 40f

    /** dB noise floor mapped to 0f. */
    const val DB_FLOOR = -60f

    /** dB ceiling mapped to 1f. */
    const val DB_CEILING = 0f

    /** Attack rate: how fast bars rise toward louder targets (per update). */
    const val ATTACK = 0.45f

    /** Release rate: how fast bars fall toward quieter targets (per update). */
    const val RELEASE = 0.12f

    /** How long a peak marker holds before decaying (nanoseconds). */
    const val PEAK_HOLD_NS = 180_000_000L

    /** Peak fall rate toward the live value once the hold expires (per update). */
    const val PEAK_FALL = 0.30f

    /**
     * Maps a frequency to a band index, logarithmic across
     * [MIN_HZ, sampleRate/2]. Below MIN_HZ folds into band 0;
     * above Nyquist clamps to the last band.
     */
    fun bandIndex(freqHz: Float, sampleRate: Int): Int {
        val nyquist = sampleRate / 2f
        if (sampleRate <= 0 || freqHz < MIN_HZ) return 0
        if (freqHz >= nyquist) return COUNT - 1
        val t = ln(freqHz / MIN_HZ) / ln(nyquist / MIN_HZ)
        return (t * COUNT).toInt().coerceIn(0, COUNT - 1)
    }

    /**
     * Normalizes a linear FFT magnitude to 0f..1f via dB scaling:
     * 20·log10(mag + eps), floor −60dB → 0, ceiling 0dB → 1.
     */
    fun dbNormalize(magnitude: Float): Float {
        if (magnitude <= 0f) return 0f
        val db = 20f * log10(magnitude)
        return ((db - DB_FLOOR) / (DB_CEILING - DB_FLOOR)).coerceIn(0f, 1f)
    }

    /** log10 for Float without pulling in extra deps. */
    private fun log10(x: Float): Float = (ln(x) / ln(10f))

    /**
     * Stateful attack/release smoother with peak hold/decay.
     * All arrays preallocated; [update] mutates [levels]/[peaks] in place —
     * the caller copies them out when emitting a frame.
     */
    class Smoother(val count: Int = COUNT) {
        val levels = FloatArray(count)
        val peaks = FloatArray(count)
        private val peakSinceNanos = LongArray(count)

        fun update(targets: FloatArray, nowNanos: Long) {
            for (i in 0 until count) {
                val target = targets[i].coerceIn(0f, 1f)
                val prev = levels[i]
                levels[i] = prev + (target - prev) *
                        if (target > prev) ATTACK else RELEASE
                if (target >= peaks[i]) {
                    peaks[i] = target
                    peakSinceNanos[i] = nowNanos
                } else if (nowNanos - peakSinceNanos[i] > PEAK_HOLD_NS) {
                    peaks[i] += (levels[i] - peaks[i]) * PEAK_FALL
                }
            }
        }

        fun reset() {
            levels.fill(0f)
            peaks.fill(0f)
            peakSinceNanos.fill(0L)
        }
    }
}
