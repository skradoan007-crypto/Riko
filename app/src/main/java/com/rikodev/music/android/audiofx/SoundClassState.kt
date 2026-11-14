package com.rikodev.music.android.audiofx

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — SOUND CLASS STATE (Phase 2)
//
// WHAT KIND OF SOUND is dominating right now. This is the contract every
// visual reads to change its CHARACTER per sound type (bass sections fold
// hard, vocal sections breathe, quiet sections rest) instead of just
// pulsing with raw amplitude.
// ─────────────────────────────────────────────────────────────────────────────

/** The seven live sound types RIKO reacts to. */
enum class SoundClass {
    /** Overall energy below the quiet threshold — rest state. */
    LOW_SOUND,

    /** Deep bass leads (40–80Hz) — drops, sub hits, kick weight. */
    DEEP_BASS,

    /** Mid bass leads (80–160Hz) — bassline groove, punch body. */
    MID_BASS,

    /** High bass leads (160–300Hz) — bass melody, upper punch. */
    HIGH_BASS,

    /** Low vocal region leads (300–600Hz) — chest voice, low singing. */
    LOW_VOCAL,

    /** Clear vocals lead with weak bass (600Hz–3kHz) — verses, acapella. */
    VOCAL_ONLY,

    /** Bass, vocals and treble all strong and balanced — choruses, drops. */
    FULL_MIX
}

/**
 * A frozen snapshot of the live sound classification.
 * All energies are perceptual levels 0..1 (RMS of v^4 band power), smoothed
 * with fast-attack / slow-release so visuals never jitter.
 */
data class SoundClassState(
    /** Which sound type is active (hysteresis-stable). */
    val soundClass: SoundClass = SoundClass.LOW_SOUND,

    /** How decisively this class won, 0..1. */
    val confidence: Float = 0f,

    /** Smoothed deep bass energy (40–80Hz). */
    val deepBass: Float = 0f,

    /** Smoothed mid bass energy (80–160Hz). */
    val midBass: Float = 0f,

    /** Smoothed high bass energy (160–300Hz). */
    val highBass: Float = 0f,

    /** Smoothed low vocal energy (300–600Hz). */
    val lowVocal: Float = 0f,

    /** Smoothed vocal energy (600Hz–3kHz). */
    val vocal: Float = 0f,

    /** Smoothed treble energy (3kHz+). */
    val treble: Float = 0f,

    /** Smoothed overall energy (all bands). */
    val total: Float = 0f,

    /** Timestamp of the frame that produced this state. */
    val timestamp: Long = 0L
) {
    companion object {
        /** Pre-built idle state — quiet, no allocation per publish. */
        val IDLE = SoundClassState()
    }
}
