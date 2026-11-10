package com.rikodev.music.android.audiofx

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — AUDIO FRAME
//
// A single raw audio analysis frame — the data that arrives from a source
// (PCM processor, Visualizer, or fallback) BEFORE normalization and
// smoothing. The Experience Data Hub receives AudioFrames, normalizes
// them, and publishes AudioState.
//
// This is the INPUT contract. AudioState is the OUTPUT contract.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Identifies where this frame originated — determines how much
 * processing the Hub needs to apply.
 */
enum class AudioSource {
    /** Real PCM tap from ExoPlayer's decoder pipeline (highest quality). */
    PCM,

    /** System Visualizer API waveform capture (OEM-restricted on some devices). */
    VISUALIZER,

    /** No real data — playback paused, idle, or no audio session active. */
    IDLE
}

/**
 * A single raw audio analysis frame from one source.
 *
 * This is NOT the final state — it's the raw material that the
 * Experience Data Hub normalizes, smooths, and merges into AudioState.
 *
 * Each field has a meaning:
 *   - rms / peak: overall loudness metrics
 *   - bands: frequency decomposition (may be unset for Visualizer source)
 *   - waveform: time-domain snapshot (from Visualizer's 128-sample capture)
 *   - timestamp: when this frame was captured (for latency calculation)
 *
 * INVARIANT: At least one of `rms`, `peak`, or `waveform` must be non-zero
 * for a non-IDLE frame. IDLE frames have everything zeroed.
 */
data class AudioFrame(
    /** Where this data came from. */
    val source: AudioSource = AudioSource.IDLE,

    /** Root-mean-square amplitude (0f..1f normalized). Overall loudness. */
    val rms: Float = 0f,

    /** Peak amplitude in this frame (0f..1f normalized). Loudest single sample. */
    val peak: Float = 0f,

    /** Frequency band energies — computed by Hub from FFT or estimated from PCM. */
    val bands: AudioBands = AudioBands(),

    /** Raw waveform snapshot (128 samples, time-domain). From Visualizer only. */
    val waveform: FloatArray = FloatArray(0),

    /** True if this frame is a beat (transient detection). */
    val isBeat: Boolean = false,

    /** True if silence detected in this frame. */
    val isSilent: Boolean = true,

    /** Mono or stereo channels detected. */
    val channels: Int = 0,

    /** Sample rate of the source (Hz). 0 if unknown or IDLE. */
    val sampleRate: Int = 0,

    /** System time when this frame was captured (System.nanoTime() for interval calc). */
    val timestamp: Long = 0L
) {
    /** Shorthand: overall energy from bands (or rms if bands not computed). */
    val energy: Float
        get() = if (bands.bassEnergy > 0f || bands.midEnergy > 0f || bands.trebleEnergy > 0f)
            bands.overallEnergy else rms

    /** Convenience: check if this frame has any usable data. */
    val hasData: Boolean
        get() = source != AudioSource.IDLE &&
                (rms > 0f || peak > 0f || waveform.any { it > 0f })

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioFrame) return false
        return source == other.source &&
                rms == other.rms &&
                peak == other.peak &&
                bands == other.bands &&
                isBeat == other.isBeat &&
                isSilent == other.isSilent &&
                channels == other.channels &&
                sampleRate == other.sampleRate &&
                timestamp == other.timestamp
                // waveform comparison is intentionally skipped (expensive, not needed)
    }

    override fun hashCode(): Int {
        var result = source.hashCode()
        result = 31 * result + rms.hashCode()
        result = 31 * result + peak.hashCode()
        result = 31 * result + bands.hashCode()
        result = 31 * result + isBeat.hashCode()
        result = 31 * result + isSilent.hashCode()
        result = 31 * result + channels
        result = 31 * result + sampleRate
        result = 31 * result + timestamp.hashCode()
        return result
    }
}
