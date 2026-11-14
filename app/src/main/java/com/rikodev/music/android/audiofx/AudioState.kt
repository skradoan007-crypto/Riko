package com.rikodev.music.android.audiofx

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — AUDIO STATE
//
// The single source of truth for ALL audio experience data.
// Every engine (Wave, Color, Motion, Glass) reads from here.
// ONLY the Experience Data Hub writes to this.
//
// ⭐ GOLDEN RULE ⭐
//   Nobody owns AudioState.
//   Everybody subscribes.
//   Only Experience Hub writes.
//
// This is a READ-ONLY snapshot for consumers. It is published as
// StateFlow<AudioState> — one flow, many readers, zero duplicates.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Song metadata relevant to experience engines.
 * Populated from Song model + future DSP analysis.
 * Part of AudioState so engines don't need separate Song access.
 */
data class AudioMetadata(
    /** Song title for context-aware rendering (future). */
    val title: String = "",

    /** Genre tag (future: from MediaStore or ML classification). */
    val genre: String = "",

    /** Beats per minute (future: from offline BPM detection). */
    val bpm: Float = 0f,

    /** Sample rate of the current track (Hz). From Song model. */
    val sampleRate: Int = 0,

    /** Channel count (1 = mono, 2 = stereo). From PCM processor. */
    val channels: Int = 0,

    /** Bitrate of the current track (kbps). From Song model. */
    val bitrate: Int = 0,

    /** Duration of the current track (ms). From PlayerState. */
    val duration: Long = 0L,

    /** ReplayGain normalization value (future). */
    val replayGain: Float = 0f,

    /** Mood classification (future: from ML). */
    val mood: String = ""
)

/**
 * The heart of the Riko Experience System.
 *
 * AudioState is a frozen snapshot — immutable, thread-safe, copy-on-write.
 * The Experience Data Hub is the ONLY component allowed to create new instances.
 * All engines read this via `StateFlow<AudioState>.collect { ... }`.
 *
 * DESIGN DECISIONS:
 *   - No setters, no mutators — pure data class
 *   - All Floats are normalized 0f..1f (except explicit Hz/ms values)
 *   - History is pre-computed inside this state, not lazy
 *   - Metadata is embedded, not separate — one read gives everything
 *
 * CONSUMER RULES:
 *   ✅ Read any field from collect {}
 *   ✅ Compare current vs previous for animation triggers
 *   ✅ Use energyHistory for trend detection
 *   ❌ NEVER create a new AudioState instance
 *   ❌ NEVER modify any field
 *   ❌ NEVER cache a reference (always read from flow)
 */
data class AudioState(
    // ── Playback State (from PlayerManager) ───────────────────────────────
    /** Is audio actively playing right now? */
    val isPlaying: Boolean = false,

    /** Current playback position in milliseconds. */
    val position: Long = 0L,

    /** Total duration of the current track in milliseconds. */
    val duration: Long = 0L,

    /** Buffered position — how far ahead the player has fetched. */
    val bufferedPosition: Long = 0L,

    /** Current playback speed multiplier (1.0f = normal). */
    val playbackSpeed: Float = 1.0f,

    // ── Energy & Loudness (from PCM / Visualizer, normalized) ────────────
    /** Overall energy level 0f..1f — the primary driver for all reactive UI. */
    val energy: Float = 0f,

    /** Bass frequency energy 0f..1f (20Hz–250Hz). */
    val bassEnergy: Float = 0f,

    /** Mid frequency energy 0f..1f (250Hz–4kHz). */
    val midEnergy: Float = 0f,

    /** Treble frequency energy 0f..1f (4kHz–20kHz). */
    val trebleEnergy: Float = 0f,

    /** Peak amplitude in the current frame 0f..1f. */
    val peak: Float = 0f,

    /** RMS (root-mean-square) amplitude — steady-state loudness. */
    val rms: Float = 0f,

    /** Silence level — how "dead" the audio is right now. 0 = full silence, 1 = active. */
    val silenceLevel: Float = 0f,

    // ── Beat & Transient Detection ───────────────────────────────────────
    /** Beat confidence 0f..1f. How certain we are a beat just occurred. */
    val beatConfidence: Float = 0f,

    /** Transient detection 0f..1f. Sharp onset energy spike (snare hit, etc.). */
    val transient: Float = 0f,

    // ── Envelope (smoothed loudness curve) ────────────────────────────────
    /** ADSR-inspired envelope follower 0f..1f. Smoothed energy without jitter. */
    val envelope: Float = 0f,

    // ── Sound Type Classification (Phase 2) ─────────────────────────────
    /**
     * Live sound-type classification: which kind of sound dominates right now
     * (deep bass / mid bass / high bass / low vocal / vocal only / low sound /
     * full mix) with confidence and per-region energies. Written only by the
     * Experience Data Hub from the 40-band spectrum tap.
     */
    val soundClass: SoundClassState = SoundClassState.IDLE,

    // ── Waveform (time-domain snapshot) ───────────────────────────────────
    /** Current waveform frame (128 samples). Empty if no Visualizer data. */
    val waveform: FloatArray = FloatArray(0),

    // ── Source & Timing ──────────────────────────────────────────────────
    /** Where the current data came from. */
    val source: AudioSource = AudioSource.IDLE,

    /** Timestamp of the latest frame (System.nanoTime()). */
    val timestamp: Long = 0L,

    // ── Metadata (from Song model + future DSP) ───────────────────────────
    /** Song metadata relevant to experience engines. */
    val metadata: AudioMetadata = AudioMetadata(),

    // ── History (pre-computed, ready for trend analysis) ──────────────────
    /** Rolling history of recent energy values (last 12 frames). */
    val energyHistory: EnergyHistory = EnergyHistory()
) {
    /** Normalized progress through the current track (0f..1f). */
    val progress: Float
        get() = if (duration > 0L) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

    /** Dominant frequency band — useful for color routing. */
    val dominantBand: Band
        get() = when {
            bassEnergy >= midEnergy && bassEnergy >= trebleEnergy -> Band.BASS
            trebleEnergy >= midEnergy && trebleEnergy >= bassEnergy -> Band.TREBLE
            else -> Band.MID
        }

    /** Is the audio currently silent (below threshold in all bands)? */
    val isSilent: Boolean
        get() = energy < AudioBands.SILENCE_THRESHOLD &&
                bassEnergy < AudioBands.SILENCE_THRESHOLD &&
                midEnergy < AudioBands.SILENCE_THRESHOLD &&
                trebleEnergy < AudioBands.SILENCE_THRESHOLD

    /** Is there any real audio data available? */
    val hasData: Boolean
        get() = source != AudioSource.IDLE && energy > 0f

    /** Is this the initial/idle state (no playback, no data)? */
    val isIdle: Boolean
        get() = !isPlaying && source == AudioSource.IDLE && energy == 0f

    /**
     * Create an idle/default AudioState with song metadata populated.
     * Used when playback starts but no audio frames have arrived yet.
     */
    fun withMetadata(
        title: String = "",
        sampleRate: Int = 0,
        channels: Int = 0,
        bitrate: Int = 0,
        duration: Long = 0L
    ): AudioState = copy(
        metadata = metadata.copy(
            title = title,
            sampleRate = sampleRate,
            channels = channels,
            bitrate = bitrate,
            duration = duration
        )
    )

    /**
     * Create an idle AudioState — all values zeroed, no source.
     * This is the safe default before any audio data arrives.
     */
    companion object {
        /** Pre-built idle state to avoid repeated allocation. */
        val IDLE = AudioState()
    }
}
