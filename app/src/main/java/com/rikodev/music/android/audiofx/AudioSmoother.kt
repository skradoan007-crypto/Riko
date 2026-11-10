package com.rikodev.music.android.audiofx

import javax.inject.Inject
import javax.inject.Singleton
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — AUDIO SMOOTHER
//
// Prevents "nervous" UI animations by applying temporal smoothing to
// audio data. Without smoothing, raw audio frames produce jittery,
// seizure-inducing reactive UI. With smoothing, transitions are organic,
// fluid, and pleasant — the difference between "data visualization"
// and "living experience."
//
// Two smoothing strategies:
//   1. Frame smoothing: Interpolates between consecutive values
//   2. Envelope follower: Attack-decay curve for overall loudness
//
// RESPONSIBILITY: Transform raw normalized values into smooth,
// animation-friendly values. No normalization, no beat detection.
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class AudioSmoother @Inject constructor() {

    // ── Frame smoothing state ─────────────────────────────────────────────
    private var smoothRms = 0f
    private var smoothPeak = 0f
    private var smoothBass = 0f
    private var smoothMid = 0f
    private var smoothTreble = 0f

    /**
     * How much of the new value to blend in per frame.
     * 0.3 = 30% new + 70% old = smooth, slightly laggy
     * 0.7 = 70% new + 30% old = responsive, less smooth
     *
     * This value is tuned for ~60fps audio callbacks.
     */
    private val smoothingFactor = 0.35f

    // ── Envelope follower state ────────────────────────────────────────────
    private var envelopeValue = 0f

    /**
     * Envelope attack speed — how fast the envelope RISES to match input.
     * Higher = more responsive to sudden loudness increases.
     */
    private val attackFactor = 0.6f

    /**
     * Envelope release speed — how fast the envelope FALLS when input drops.
     * Lower = longer sustain, more "breathing" feel.
     */
    private val releaseFactor = 0.08f

    /**
     * Smooth a single audio frame.
     *
     * Each value is interpolated with its previous smoothed value:
     *   smooth = smooth * (1 - factor) + raw * factor
     *
     * This creates an exponential moving average that reduces jitter
     * while maintaining responsiveness to real changes.
     *
     * @param frame Normalized audio frame (values in 0f..1f)
     * @return Same frame structure with smoothed values
     */
    fun smooth(frame: AudioFrame): AudioFrame {
        if (frame.source == AudioSource.IDLE) {
            reset()
            return frame
        }

        // Smooth each band independently
        smoothBass = lerp(smoothBass, frame.bands.bassEnergy, smoothingFactor)
        smoothMid = lerp(smoothMid, frame.bands.midEnergy, smoothingFactor)
        smoothTreble = lerp(smoothTreble, frame.bands.trebleEnergy, smoothingFactor)

        // Smooth overall metrics
        smoothRms = lerp(smoothRms, frame.rms, smoothingFactor)
        smoothPeak = lerp(smoothPeak, frame.peak, smoothingFactor)

        return frame.copy(
            rms = smoothRms,
            peak = smoothPeak,
            bands = AudioBands(
                bassEnergy = smoothBass,
                midEnergy = smoothMid,
                trebleEnergy = smoothTreble
            )
        )
    }

    /**
     * ADSR-inspired envelope follower for overall loudness.
     *
     * Unlike simple smoothing (which treats attack and release equally),
     * the envelope has asymmetric response:
     *   - Fast attack: Quickly rises when audio gets louder (0.6 factor)
     *   - Slow release: Slowly falls when audio gets quieter (0.08 factor)
     *
     * This creates the "breathing" feel — the glow lifts instantly on
     * a beat hit, then slowly fades during the quiet gap between beats.
     *
     * @param currentEnergy Current normalized energy (0f..1f)
     * @return Envelope value (0f..1f), smoothed with attack/release curve
     */
    fun envelope(currentEnergy: Float): Float {
        return if (currentEnergy > envelopeValue) {
            // Attack: fast rise
            lerp(envelopeValue, currentEnergy, attackFactor)
        } else {
            // Release: slow fall
            lerp(envelopeValue, currentEnergy, releaseFactor)
        }
    }

    /**
     * Linear interpolation between current and target values.
     * Simple, predictable, no branching or conditionals.
     */
    private fun lerp(current: Float, target: Float, factor: Float): Float {
        return current + (target - current) * factor
    }

    /**
     * Reset all smoothed values to zero.
     * Called on pause, track change, or reset.
     */
    fun reset() {
        smoothRms = 0f
        smoothPeak = 0f
        smoothBass = 0f
        smoothMid = 0f
        smoothTreble = 0f
        envelopeValue = 0f
    }
}
