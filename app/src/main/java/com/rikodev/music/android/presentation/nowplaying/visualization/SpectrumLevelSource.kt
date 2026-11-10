package com.rikodev.music.android.presentation.nowplaying.visualization

import com.rikodev.music.android.core.experience.wave.WaveEnergy
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — SPECTRUM LEVEL SOURCE (visualizer_plan §13 + §14)
//
// The ONE place the renderer's band data is assembled, and the ONE place the
// fallback spectrum is synthesised. Two rules it exists to enforce:
//
//   1. ZERO allocation per update. Every array is preallocated and reused, so
//      a ~30Hz emission no longer churns four FloatArrays per frame through
//      the young generation. The holder is remembered on `bandCount` only —
//      that changes on a user setting, never per frame.
//
//   2. OWNERSHIP: the holder is MUTATED, so callers invoke [update] from a
//      DRAW-phase lambda (Canvas / onDrawBehind), never in a composition body.
//      Mutating remembered state during composition is invisible to Compose
//      and breaks recomposition reasoning (audit F10).
//
// Fallback policy (visualizer_plan §16 — "visualization dies gracefully,
// playback never dies"): when the PCM tap is stale or absent (paused, offload
// path, pre-first-frame) the bands are synthesised from the Wave Engine's REAL
// energies — never random noise — so the screen keeps breathing the same song,
// with attack/release smoothing so it never twitches.
// ─────────────────────────────────────────────────────────────────────────────

class SpectrumLevelSource(val bandCount: Int) {

    /** Smoothed 0..1 levels, sized to [bandCount]. Read-only by contract. */
    val levels = FloatArray(bandCount)

    /** Peak-hold markers, sized to [bandCount]. Read-only by contract. */
    val peaks = FloatArray(bandCount)

    /** True when [peaks] holds real data for this frame. */
    var hasPeaks: Boolean = false
        private set

    private val rawLevels = FloatArray(bandCount)
    private val rawPeaks = FloatArray(bandCount)
    private val fallback = FloatArray(FALLBACK_BARS)

    /**
     * Rebuilds [levels] (and [peaks]) for this frame. Draw-phase only.
     *
     * @param source 40 canonical DSP bands from the newest frame.
     * @param sourcePeaks matching peak-hold values, or null when unavailable.
     * @param tapLive true while the PCM tap is delivering fresh frames.
     * @param waveActive true while the Wave Engine has a live song signal.
     * @param energy the Wave Engine's real energy snapshot (fallback input).
     * @param pulseAmount palette pulse 0..1 (fallback shimmer input).
     * @param sensitivity user level scaling, 0.5× soft .. 1.5× hot.
     */
    fun update(
        source: FloatArray,
        sourcePeaks: FloatArray?,
        tapLive: Boolean,
        waveActive: Boolean,
        energy: WaveEnergy,
        pulseAmount: Float,
        sensitivity: Float
    ) {
        if (tapLive && source.isNotEmpty()) {
            hasPeaks = sourcePeaks != null && sourcePeaks.size == source.size
            resample(source, rawLevels)
            if (hasPeaks) resample(sourcePeaks!!, rawPeaks)
        } else {
            hasPeaks = false
            synthesizeFallback(waveActive, energy, pulseAmount)
            resample(fallback, rawLevels)
        }
        applySensitivity(rawLevels, levels, sensitivity)
        if (hasPeaks) applySensitivity(rawPeaks, peaks, sensitivity)
    }

    /** Linear resample of [src] into [dst] — allocation-free. */
    private fun resample(src: FloatArray, dst: FloatArray) {
        if (src.size == dst.size) {
            src.copyInto(dst)
            return
        }
        if (src.isEmpty() || dst.isEmpty()) {
            dst.fill(0f)
            return
        }
        val scale = (src.size - 1f) / (dst.size - 1f).coerceAtLeast(1f)
        for (i in dst.indices) {
            val t = i * scale
            val lo = t.toInt().coerceIn(0, src.size - 1)
            val hi = (lo + 1).coerceAtMost(src.size - 1)
            dst[i] = src[lo] + (src[hi] - src[lo]) * (t - lo)
        }
    }

    /** In-place sensitivity scaling (0.5× soft .. 1.5× hot). */
    private fun applySensitivity(src: FloatArray, dst: FloatArray, sensitivity: Float) {
        for (i in src.indices) dst[i] = (src[i] * sensitivity).coerceIn(0f, 1f)
    }

    /**
     * Real-energy fallback shape: bass → mid → treble crossfade across the band
     * axis, with attack/release smoothing so it never twitches. Mirrors the
     * original inline curve exactly so the fallback still looks like RIKO.
     */
    private fun synthesizeFallback(
        waveActive: Boolean,
        energy: WaveEnergy,
        pulseAmount: Float
    ) {
        val master = if (waveActive) {
            0.25f + 0.75f * energy.overallEnergy.coerceIn(0f, 1f)
        } else {
            0.06f
        }
        for (i in 0 until FALLBACK_BARS) {
            val t = i / (FALLBACK_BARS - 1f)
            val band = when {
                t < 0.33f -> {
                    val k = t / 0.33f
                    energy.bassPressure + (energy.midTexture - energy.bassPressure) * k
                }
                t < 0.66f -> {
                    val k = (t - 0.33f) / 0.33f
                    energy.midTexture + (energy.trebleSpark - energy.midTexture) * k
                }
                else -> energy.trebleSpark * (1f - (t - 0.66f) / 0.34f * 0.5f)
            }
            val shimmer = 0.85f + 0.15f * sin(t * 12.566f + pulseAmount * 6.283f)
            val target = (band.coerceIn(0f, 1f) * shimmer * master).coerceIn(0.02f, 1f)
            val prev = fallback[i]
            fallback[i] = prev + (target - prev) * if (target > prev) 0.55f else 0.18f
        }
    }

    companion object {
        /** Bars the fallback curve is defined over before resampling. */
        const val FALLBACK_BARS = 40

        /** User-facing sensitivity range (the settings sheet reads this). */
        val SENSITIVITY_RANGE: ClosedFloatingPointRange<Float> get() = 0.5f..1.5f
    }
}
