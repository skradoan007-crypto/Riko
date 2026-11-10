package com.rikodev.music.android.audiofx

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — SPECTRUM FRAME (Spectrum visualizer data model)
//
// One analysis frame for the Aurora Spectrum renderer: 40 logarithmic
// bands (see SpectrumBands) with per-band levels + peak-hold markers,
// all normalized 0f..1f. Produced on ExoPlayer's audio thread by
// PcmAudioProcessor, consumed on the UI thread by AuroraSpectrum.
//
// THREADING: the frame is a snapshot — PcmAudioProcessor copies its
// working arrays into a new frame per emission (~30Hz), so the UI never
// reads arrays the audio thread is mutating.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A single spectrum analysis frame.
 *
 * @param bands Normalized 0f..1f smoothed band levels, [SpectrumBands.COUNT] entries.
 * @param peaks Normalized 0f..1f peak-hold markers, same size as [bands].
 * @param sampleRate Audio sample rate in Hz (defines the Nyquist top of the bands).
 * @param timestampNanos [System.nanoTime] when the frame was produced.
 * @param isActive False when no PCM is flowing (paused, offload path, pre-first-frame).
 */
data class SpectrumFrame(
    val bands: FloatArray = FloatArray(SpectrumBands.COUNT),
    val peaks: FloatArray = FloatArray(SpectrumBands.COUNT),
    val sampleRate: Int = 0,
    val timestampNanos: Long = 0L,
    val isActive: Boolean = false
) {
    companion object {
        /** Idle frame — the renderer falls back to energy interpolation. */
        val IDLE = SpectrumFrame()
    }
}
