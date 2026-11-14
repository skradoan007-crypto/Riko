package com.rikodev.music.android.dna.core

import com.rikodev.music.android.audiofx.SoundClass
import kotlin.math.exp

// ─────────────────────────────────────────────────────────────────────────────
// RIKO DNA (MDNA) — CLASS REACTION CONTRACT (Phase 3)
//
// The per-class REACTION MATRIX: HOW a visual's character changes per sound
// type. A bass section must feel PHYSICALLY different from a vocal section —
// not just louder. Every MDNA visual consumes these profiles:
//
//   LOW_SOUND  — near-still rest state, faint glow, calm
//   DEEP_BASS  — monstrous slow folds + punch, ring flares wide
//   MID_BASS   — groove punch, balanced fold
//   HIGH_BASS  — quicker tight ripples, bright shimmer
//   LOW_VOCAL  — deep slow swell, low pulse
//   VOCAL_ONLY — thin ripple, clean ribs, thin elegant ring, slow breathing
//   FULL_MIX   — everything at once, full corona, maximum motion
//
// ClassReactionFollower MORPHS between profiles (≈220ms time constant) so the
// character glides when the music section changes — never hard-switches.
// Pure + JVM unit-tested.
// ─────────────────────────────────────────────────────────────────────────────
data class ClassReaction(
    /** Inward fold depth of the wave mesh (0 = rest, 1.3 = monstrous fold). */
    val fold: Float,

    /** Motion speed multiplier (slow swells vs quick ripples). */
    val speed: Float,

    /** Glow/flare intensity (faint rim vs full corona). */
    val flare: Float,

    /** Stroke/ribbon width (thin elegant ring vs fat fire ring). */
    val width: Float,

    /** Slow breathing amplitude. */
    val breathe: Float,

    /** Rib/detail contrast — clean parametric ribs at 1.0. */
    val detail: Float,

    /** 3D roll/twist speed multiplier. */
    val spin: Float,

    /** Micro-shimmer intensity on the dots. */
    val sparkle: Float
) {
    companion object {
        private val LOW_SOUND_PROFILE = ClassReaction(0.00f, 0.15f, 0.15f, 0.45f, 0.20f, 0.20f, 0.10f, 0.05f)
        private val DEEP_BASS_PROFILE = ClassReaction(1.30f, 0.85f, 1.00f, 1.00f, 0.15f, 0.55f, 1.20f, 0.70f)
        private val MID_BASS_PROFILE = ClassReaction(1.05f, 1.10f, 0.85f, 0.85f, 0.25f, 0.65f, 1.00f, 0.60f)
        private val HIGH_BASS_PROFILE = ClassReaction(0.80f, 1.35f, 0.70f, 0.70f, 0.30f, 0.85f, 0.90f, 0.85f)
        private val LOW_VOCAL_PROFILE = ClassReaction(0.55f, 0.50f, 0.50f, 0.75f, 0.80f, 0.50f, 0.50f, 0.30f)
        private val VOCAL_ONLY_PROFILE = ClassReaction(0.35f, 0.40f, 0.35f, 0.45f, 1.00f, 1.00f, 0.35f, 0.40f)
        private val FULL_MIX_PROFILE = ClassReaction(1.10f, 1.00f, 1.20f, 1.15f, 0.50f, 0.75f, 1.10f, 1.00f)

        /** The reaction character for a sound type. Shared immutable preset. */
        fun forClass(cls: SoundClass): ClassReaction = when (cls) {
            SoundClass.LOW_SOUND -> LOW_SOUND_PROFILE
            SoundClass.DEEP_BASS -> DEEP_BASS_PROFILE
            SoundClass.MID_BASS -> MID_BASS_PROFILE
            SoundClass.HIGH_BASS -> HIGH_BASS_PROFILE
            SoundClass.LOW_VOCAL -> LOW_VOCAL_PROFILE
            SoundClass.VOCAL_ONLY -> VOCAL_ONLY_PROFILE
            SoundClass.FULL_MIX -> FULL_MIX_PROFILE
        }
    }
}

/**
 * Morphs the reaction character toward the active class profile.
 * Renders read [reaction] every frame; the snapshot only allocates while the
 * character is still converging — steady state is allocation-free.
 */
class ClassReactionFollower {
    private var goalClass = SoundClass.LOW_SOUND
    private var current = ClassReaction.forClass(SoundClass.LOW_SOUND)

    /** The morphing reaction state. */
    val reaction: ClassReaction get() = current

    fun step(cls: SoundClass, dtSec: Float) {
        goalClass = cls
        val goal = ClassReaction.forClass(goalClass)
        if (current == goal) return
        val k = 1f - exp(-dtSec.coerceIn(0f, 0.1f) / MORPH_TAU_SEC)
        current = ClassReaction(
            current.fold + (goal.fold - current.fold) * k,
            current.speed + (goal.speed - current.speed) * k,
            current.flare + (goal.flare - current.flare) * k,
            current.width + (goal.width - current.width) * k,
            current.breathe + (goal.breathe - current.breathe) * k,
            current.detail + (goal.detail - current.detail) * k,
            current.spin + (goal.spin - current.spin) * k,
            current.sparkle + (goal.sparkle - current.sparkle) * k
        )
    }

    fun reset() {
        goalClass = SoundClass.LOW_SOUND
        current = ClassReaction.forClass(SoundClass.LOW_SOUND)
    }

    private companion object { const val MORPH_TAU_SEC = 0.22f }
}
