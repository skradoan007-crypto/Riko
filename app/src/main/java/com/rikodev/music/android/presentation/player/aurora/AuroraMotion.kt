package com.rikodev.music.android.presentation.player.aurora

import androidx.compose.runtime.Immutable
import com.rikodev.music.android.core.experience.wave.WaveEnergy

/**
 * Fast-motion contract (plan 4.1) — the SLOW/FAST split for the Now Playing
 * aurora experience:
 *
 *  * SLOW  = album palette / theme colors / long-term atmosphere → ColorState
 *  * FAST  = bass, mid, treble, transient, beat, momentum → THIS
 *
 * Motion must never be driven by quantized color state (the old 0.05-step
 * staircase made the glow jump in 21 visible steps). The musical attack comes
 * from the Wave Engine's own attack/release shaping (EnergySolver) — this
 * mapping adds no extra smoothing, so attacks stay sharp and releases stay
 * soft exactly as the solver designed them.
 */
@Immutable
data class AuroraMotion(
    val bass: Float = 0f,
    val mid: Float = 0f,
    val treble: Float = 0f,
    val transient: Float = 0f,
    val beat: Float = 0f,
    val momentum: Float = 0f,
    val active: Boolean = false
) {
    /** Overall musical energy — sustain level between attacks. */
    val energy: Float
        get() = ((bass + mid + treble) / 3f).coerceIn(0f, 1f)

    companion object {
        val IDLE = AuroraMotion()
    }
}

/**
 * WaveEnergy → AuroraMotion. Pure and cheap; read in the DRAW phase so the
 * aurora repaints per frame without recomposing composition (plan 4.1 / R15).
 */
fun WaveEnergy.toAuroraMotion(): AuroraMotion = AuroraMotion(
    bass = bassPressure.coerceIn(0f, 1f),
    mid = midTexture.coerceIn(0f, 1f),
    treble = trebleSpark.coerceIn(0f, 1f),
    transient = attack.coerceIn(0f, 1f),
    beat = beatConfidence.coerceIn(0f, 1f),
    momentum = momentum.coerceIn(0f, 1f),
    active = isActive
)
