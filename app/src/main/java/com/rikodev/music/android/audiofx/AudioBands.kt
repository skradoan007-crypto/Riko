package com.rikodev.music.android.audiofx

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — AUDIO BANDS
//
// Frequency-band energy model. Splits the audio spectrum into three bands
// (bass, mid, treble) with normalized 0f..1f energy values. This is the
// raw material that the Wave Engine, Color Engine, and Glass Engine will
// consume — never computed inline, always read from AudioState.
//
// RESPONSIBILITY: Define the three-band model and helper functions.
// No state, no flows — pure data + math.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Three-band frequency decomposition of the current audio frame.
 *
 * Each band is normalized to 0f..1f where:
 *   0.0 = complete silence in this frequency range
 *   1.0 = maximum energy (clamped, never exceeds 1.0)
 *
 * Consumers (Wave Engine, Color Engine, Glass Physics) read these values
 * from AudioState — they NEVER compute frequency analysis themselves.
 */
data class AudioBands(
    /** Sub-bass + bass energy (20Hz – 250Hz). Drives glass pulse, low-end glow. */
    val bassEnergy: Float = 0f,
    /** Mid-range energy (250Hz – 4kHz). Drives waveform body, core rhythm. */
    val midEnergy: Float = 0f,
    /** High-mid + treble energy (4kHz – 20kHz). Drives shimmer, detail glow. */
    val trebleEnergy: Float = 0f
) {
    /** Overall energy = weighted average favoring mid (where human hearing is most sensitive). */
    val overallEnergy: Float
        get() = (bassEnergy * 0.25f + midEnergy * 0.5f + trebleEnergy * 0.25f)
            .coerceIn(0f, 1f)

    /** Dominant frequency band — useful for color engine routing. */
    val dominantBand: Band
        get() = when {
            bassEnergy >= midEnergy && bassEnergy >= trebleEnergy -> Band.BASS
            trebleEnergy >= midEnergy && trebleEnergy >= bassEnergy -> Band.TREBLE
            else -> Band.MID
        }

    /** Returns true if all bands are below the silence threshold. */
    val isSilent: Boolean
        get() = bassEnergy < SILENCE_THRESHOLD &&
                midEnergy < SILENCE_THRESHOLD &&
                trebleEnergy < SILENCE_THRESHOLD

    companion object {
        /** Below this value, a band is considered silent (not zero — noise floor). */
        const val SILENCE_THRESHOLD = 0.02f
    }
}

/** Frequency band identifier for routing decisions. */
enum class Band {
    BASS,
    MID,
    TREBLE
}

/**
 * Ring buffer of recent energy values for temporal smoothing.
 * Stores the last [capacity] AudioBands snapshots so engines can
 * compute trends (rising, falling, sustained) instead of reacting
 * to single-frame jitter.
 *
 * This prevents "nervous" animations — the UI reads the TREND,
 * not the instantaneous value.
 */
// RUNTIME FIX 10: confirmed via a real device crash log —
// java.util.ConcurrentModificationException at EnergyHistory.getAverageEnergy,
// called from ExperienceDataHub.detectBeat, while ExperienceDataHub.updatePlaybackState
// called energyHistory.clear() concurrently on a different thread. Root cause:
// ExperienceDataHub's bridge coroutines all run on Dispatchers.Default, which is a
// real thread POOL — unlike Dispatchers.Main, coroutines launched on it can execute
// on genuinely different threads at the same time, not just interleaved. push()'s own
// doc comment claimed "called ONLY by the Experience Data Hub (single writer)" — true
// for push() alone, but clear() is a second writer (called on pause/track-change from
// a separate bridge coroutine), and this class was never synchronized against either
// concurrent writes or reads-during-write. This buffer is also exposed externally via
// AudioState.energyHistory, so the fix lives inside this class rather than relying on
// every external caller to coordinate through a lock they don't have access to.
class EnergyHistory(
    val capacity: Int = DEFAULT_HISTORY_SIZE
) {
    private val lock = Any()
    private val buffer = ArrayDeque<AudioBands>(capacity)

    /** Most recent entry (null if empty). */
    val latest: AudioBands? get() = synchronized(lock) { buffer.lastOrNull() }

    /** Number of entries currently stored. */
    val size: Int get() = synchronized(lock) { buffer.size }

    /** Returns true if buffer is full. */
    val isFull: Boolean get() = synchronized(lock) { buffer.size >= capacity }

    /** Average overall energy across all stored frames. */
    val averageEnergy: Float
        get() = synchronized(lock) {
            if (buffer.isEmpty()) 0f
            else buffer.map { it.overallEnergy }.average().toFloat()
        }

    /** Energy trend: positive = rising, negative = falling, ~0 = sustained. */
    val energyTrend: Float
        get() = synchronized(lock) {
            if (buffer.size < 2) return@synchronized 0f
            val recent = buffer.takeLast(4).map { it.overallEnergy }.average().toFloat()
            val older = buffer.takeLast(8).map { it.overallEnergy }.average().toFloat()
            (recent - older).coerceIn(-1f, 1f)
        }

    /** Peak energy observed in the buffer. */
    val peakEnergy: Float
        get() = synchronized(lock) { buffer.maxOfOrNull { it.overallEnergy } ?: 0f }

    /**
     * Push a new frame. If buffer is full, oldest entry is dropped.
     */
    fun push(bands: AudioBands) = synchronized(lock) {
        if (buffer.size >= capacity) buffer.removeFirst()
        buffer.addLast(bands)
        Unit
    }

    /** Clear all history (called on track change, pause, or reset). */
    fun clear() = synchronized(lock) {
        buffer.clear()
    }

    companion object {
        const val DEFAULT_HISTORY_SIZE = 12
    }
}
