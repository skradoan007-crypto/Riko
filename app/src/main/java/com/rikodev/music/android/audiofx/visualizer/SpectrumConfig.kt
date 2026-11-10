package com.rikodev.music.android.audiofx.visualizer

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — SPECTRUM CONFIG (visualizer_plan §15 + §20)
//
// Device-tier performance gates. DSP quality scales down on low-end,
// UI frame-rate never does (plan rule: "DSP quality komabe, UI frame-rate noy").
//
//   Tier A flagship: FFT 2048, 48 bands, 40Hz DSP, 120Hz render, extra glow
//   Tier B balanced: FFT 1024, 40 bands, 30-35Hz DSP, 60/90Hz render
//   Tier C low-end:  FFT 512,  32 bands, 20-25Hz DSP, 60Hz render, no blur
//
// v1 default = Tier B (1024 / 40 bands / ~30Hz). DSP stays 1024 for v1;
// tiers select display resampling + glow budget, so the audio thread never
// reallocates mid-playback.
// ─────────────────────────────────────────────────────────────────────────────

enum class SpectrumTier { LOW, BALANCED, FLAGSHIP }

object SpectrumConfig {

    const val FFT_LOW = 512
    const val FFT_BALANCED = 1024
    const val FFT_FLAGSHIP = 2048

    const val BANDS_LOW = 32
    const val BANDS_BALANCED = 40
    const val BANDS_FLAGSHIP = 48

    /** ~40Hz DSP cap for flagship (25ms), ~30Hz balanced (33ms), ~22Hz low (45ms). */
    const val EMIT_INTERVAL_FLAGSHIP_NS = 25_000_000L
    const val EMIT_INTERVAL_BALANCED_NS = 33_000_000L
    const val EMIT_INTERVAL_LOW_NS = 45_000_000L

    /** A/V sync: PCM tap runs ahead of audible output by the AudioTrack buffer. */
    const val AV_SYNC_DELAY_MS = 120L

    /** Stale threshold: older than this → renderer falls back, never dead bars. */
    const val STALE_NS = 500_000_000L

    fun tierFor(isLowRam: Boolean, cpuCores: Int, refreshHz: Float): SpectrumTier =
        when {
            isLowRam || cpuCores <= 4 -> SpectrumTier.LOW
            cpuCores >= 8 && refreshHz >= 90f -> SpectrumTier.FLAGSHIP
            else -> SpectrumTier.BALANCED
        }

    fun fftSizeFor(tier: SpectrumTier): Int = when (tier) {
        SpectrumTier.LOW -> FFT_LOW
        SpectrumTier.BALANCED -> FFT_BALANCED
        SpectrumTier.FLAGSHIP -> FFT_FLAGSHIP
    }

    fun bandCountFor(tier: SpectrumTier): Int = when (tier) {
        SpectrumTier.LOW -> BANDS_LOW
        SpectrumTier.BALANCED -> BANDS_BALANCED
        SpectrumTier.FLAGSHIP -> BANDS_FLAGSHIP
    }

    fun emitIntervalFor(tier: SpectrumTier): Long = when (tier) {
        SpectrumTier.LOW -> EMIT_INTERVAL_LOW_NS
        SpectrumTier.BALANCED -> EMIT_INTERVAL_BALANCED_NS
        SpectrumTier.FLAGSHIP -> EMIT_INTERVAL_FLAGSHIP_NS
    }

    fun glowEnabledFor(tier: SpectrumTier): Boolean = tier != SpectrumTier.LOW

    /**
     * Resamples a 40-band DSP frame to the tier's display band count.
     * Pure, allocation per call is one FloatArray of [targetCount] — called
     * at ~30Hz on the UI thread only, never on the audio thread.
     */
    fun resampleToDisplay(source40: FloatArray, targetCount: Int): FloatArray {
        if (source40.size == targetCount) return source40.copyOf()
        val out = FloatArray(targetCount)
        for (i in out.indices) {
            val t = i / (targetCount - 1f) * (source40.size - 1f)
            val lo = t.toInt().coerceIn(0, source40.size - 1)
            val hi = (lo + 1).coerceIn(0, source40.size - 1)
            val frac = t - lo
            out[i] = source40[lo] + (source40[hi] - source40[lo]) * frac
        }
        return out
    }
}
