package com.rikodev.music.android.audiofx

/**
 * Optional undelayed spectrum callback from the audio processing thread.
 * Implementations must copy the mutable band data if they need to retain it.
 * Methods must return promptly; do not block the audio thread.
 */
interface RawSpectrumTap {
    fun onFrame(levels: FloatArray, timestampNanos: Long)
    fun onReset()
}
