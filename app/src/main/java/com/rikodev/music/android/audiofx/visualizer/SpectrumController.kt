package com.rikodev.music.android.audiofx.visualizer

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — SPECTRUM CONTROLLER (visualizer_plan §16)
//
// Offload/passthrough paths bypass the AudioProcessor chain, so the PCM tap
// can go quiet while music still plays. Rule:
//
//   Visualization is secondary. Audio playback is sacred.
//
// This controller is the single place that answers "can the tap receive
// data right now?" — fresh frame + active flag + young timestamp. Stale or
// idle → renderer falls back to synthetic/energy interpolation, playback
// never breaks, no permission is ever requested.
// ─────────────────────────────────────────────────────────────────────────────

object SpectrumController {

    /**
     * True when the tap is delivering live data: active flag set, band size
     * matches the DSP contract, timestamp younger than [SpectrumConfig.STALE_NS].
     */
    fun isTapLive(
        isActive: Boolean,
        bandSize: Int,
        frameAgeNanos: Long
    ): Boolean = isActive &&
        bandSize == com.rikodev.music.android.audiofx.SpectrumBands.COUNT &&
        frameAgeNanos < SpectrumConfig.STALE_NS

    /** Display band count for the current tier (resampled in the renderer). */
    fun displayBandsFor(tier: SpectrumTier): Int = SpectrumConfig.bandCountFor(tier)
}
