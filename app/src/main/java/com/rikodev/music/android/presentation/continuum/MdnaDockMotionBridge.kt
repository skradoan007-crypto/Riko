package com.rikodev.music.android.presentation.continuum

import com.rikodev.music.android.audiofx.AudioState

// ─────────────────────────────────────────────────────────────────────────────
// RIKO LIQUID NEXUS — MDNA / AUDIO REACTIVITY BRIDGE (P3 + P8)
//
// Consumes the existing ExperienceDataHub AudioState snapshot (no duplicate
// FFT or UI-thread audio decoding). Normalizes, clamps, and smoothly
// interpolates contour motion parameters:
//   - Slow music       -> gentle breathing
//   - Energetic music  -> slightly livelier organic contour motion
//   - Bass-heavy music -> controlled pulse without excessive jelly distortion
//   - Paused           -> smoothly settles to calm stillness
//   - MDNA unavailable -> subtle deterministic ambient motion (never fake sync)
// ─────────────────────────────────────────────────────────────────────────────

data class DockMotionFrame(
    /** Normalized contour energy 0f..1f */
    val energy: Float = 0f,
    /** Controlled bass pulse 0f..1f */
    val bassPulse: Float = 0f,
    /** Phase velocity multiplier (radians/sec) */
    val phaseSpeed: Float = 0.55f,
    /** True when driven by real audio/MDNA analysis rather than ambient fallback */
    val isAudioDriven: Boolean = false
)

object MdnaDockMotionBridge {

    fun computeTarget(
        audioState: AudioState,
        isPlaying: Boolean,
        reactivityEnabled: Boolean
    ): DockMotionFrame {
        if (!isPlaying) {
            return DockMotionFrame(
                energy = 0f,
                bassPulse = 0f,
                phaseSpeed = 0.18f,
                isAudioDriven = false
            )
        }
        if (!reactivityEnabled) {
            return DockMotionFrame(
                energy = 0.14f,
                bassPulse = 0.05f,
                phaseSpeed = 0.45f,
                isAudioDriven = false
            )
        }

        val hasRealSignal = audioState.silenceLevel > 0.02f ||
            audioState.energy > 0.01f ||
            audioState.bassEnergy > 0.01f ||
            audioState.transient > 0.01f

        if (!hasRealSignal) {
            // Honest deterministic ambient fallback when live MDNA data isn't flowing yet
            return DockMotionFrame(
                energy = 0.22f,
                bassPulse = 0.08f,
                phaseSpeed = 0.65f,
                isAudioDriven = false
            )
        }

        val clampedEnergy = (
            audioState.energy * 0.55f +
                audioState.midEnergy * 0.25f +
                audioState.trebleEnergy * 0.20f
            ).coerceIn(0f, 1f)

        val controlledBass = (
            audioState.bassEnergy * 0.72f +
                audioState.beatConfidence * 0.18f +
                audioState.transient * 0.10f
            ).coerceIn(0f, 0.85f) // clamped so sudden spikes never look uncontrolled

        val speed = (0.55f + clampedEnergy * 0.85f + controlledBass * 0.30f).coerceIn(0.45f, 1.75f)

        return DockMotionFrame(
            energy = clampedEnergy,
            bassPulse = controlledBass,
            phaseSpeed = speed,
            isAudioDriven = true
        )
    }

    /** Smooth exponential interpolation between previous and target motion frame. */
    fun smoothStep(
        current: DockMotionFrame,
        target: DockMotionFrame,
        dtSec: Float
    ): DockMotionFrame {
        val dt = dtSec.coerceIn(0f, 0.1f)
        // Faster attack, gentler release when pausing/calming down
        val rate = if (target.energy >= current.energy) 7.5f else 3.2f
        val alpha = 1f - kotlin.math.exp(-dt * rate)
        return DockMotionFrame(
            energy = current.energy + (target.energy - current.energy) * alpha,
            bassPulse = current.bassPulse + (target.bassPulse - current.bassPulse) * alpha,
            phaseSpeed = current.phaseSpeed + (target.phaseSpeed - current.phaseSpeed) * alpha,
            isAudioDriven = target.isAudioDriven
        )
    }
}
