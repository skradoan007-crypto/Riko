package com.rikodev.music.android.audiofx

import javax.inject.Inject
import javax.inject.Singleton
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — AUDIO NORMALIZER
//
// Normalizes raw audio frames into a consistent 0f..1f range regardless
// of source (PCM, Visualizer) or device-specific volume levels.
//
// RESPONSIBILITY: Map raw audio values to the 0f..1f contract that
// AudioState consumers expect. No smoothing, no beat detection —
// pure normalization math.
//
// INVARIANT: Output frame always has all values in 0f..1f.
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class AudioNormalizer @Inject constructor() {

    /** Running peak tracker — decays slowly to adapt to dynamic range changes. */
    private var runningPeak = 0f

    /** Decay factor for running peak (prevents it from locking to a single loud moment). */
    private val peakDecay = 0.995f

    /** Minimum energy floor — prevents division by zero and keeps silent states smooth. */
    private val noiseFloor = 0.001f

    /**
     * Normalize a raw audio frame to 0f..1f range.
     *
     * Processing:
     *   1. Clamp raw values to physical limits
     *   2. Update running peak tracker
     *   3. Scale RMS/peak relative to running peak
     *   4. Estimate frequency bands if source doesn't provide them
     *   5. Apply noise floor
     *
     * @param frame Raw audio frame from any source
     * @return Normalized frame with all values in 0f..1f
     */
    fun normalize(frame: AudioFrame): AudioFrame {
        // IDLE frames pass through unchanged
        if (frame.source == AudioSource.IDLE) {
            runningPeak = 0f
            return frame
        }

        // Step 1: Update running peak tracker
        val rawPeak = frame.peak.coerceAtLeast(frame.rms)
        runningPeak = (runningPeak * peakDecay).coerceAtLeast(rawPeak)

        // Step 2: Normalize RMS and peak relative to running peak
        val normalizedRms = if (runningPeak > noiseFloor) {
            (frame.rms / runningPeak).coerceIn(0f, 1f)
        } else {
            0f
        }

        val normalizedPeak = if (runningPeak > noiseFloor) {
            (frame.peak / runningPeak).coerceIn(0f, 1f)
        } else {
            0f
        }

        // Step 3: Normalize or estimate frequency bands
        val normalizedBands = if (hasBandData(frame)) {
            frame.bands.copy(
                bassEnergy = frame.bands.bassEnergy.coerceIn(0f, 1f),
                midEnergy = frame.bands.midEnergy.coerceIn(0f, 1f),
                trebleEnergy = frame.bands.trebleEnergy.coerceIn(0f, 1f)
            )
        } else {
            // No band data — estimate from overall energy
            estimateBands(normalizedRms, normalizedPeak)
        }

        // Step 4: Apply noise floor
        val isSilent = normalizedRms < noiseFloor && normalizedPeak < noiseFloor

        return frame.copy(
            rms = normalizedRms,
            peak = normalizedPeak,
            bands = normalizedBands,
            isSilent = isSilent
        )
    }

    /**
     * Check if the frame has actual frequency band data.
     */
    private fun hasBandData(frame: AudioFrame): Boolean {
        return frame.bands.bassEnergy > 0f ||
               frame.bands.midEnergy > 0f ||
               frame.bands.trebleEnergy > 0f
    }

    /**
     * Estimate frequency bands from overall RMS when no FFT data is available.
     *
     * Simple heuristic: distribute energy across bands based on overall level.
     * This is NOT accurate frequency analysis — it's a fallback for the
     * Visualizer path which only provides waveform data, not FFT.
     *
     * Future: Replace with proper FFT when we add frequency analysis to PCM bridge.
     */
    private fun estimateBands(rms: Float, peak: Float): AudioBands {
        if (rms < noiseFloor) return AudioBands()

        // Default distribution: energy biased toward mid-range
        // (most music energy lives in the mid band)
        val bass = (rms * 0.4f).coerceIn(0f, 1f)
        val mid = (rms * 0.7f).coerceIn(0f, 1f)
        val treble = (rms * 0.3f).coerceIn(0f, 1f)

        return AudioBands(
            bassEnergy = bass,
            midEnergy = mid,
            trebleEnergy = treble
        )
    }

    /**
     * Reset normalizer state (called on track change or pause).
     */
    fun reset() {
        runningPeak = 0f
    }
}
