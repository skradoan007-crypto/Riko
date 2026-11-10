package com.rikodev.music.android.audiofx

import androidx.compose.runtime.Immutable

/** Immutable, full-track, uniformly time-spaced linear PCM amplitudes. */
@Immutable
class WaveformData(peaks: FloatArray, rms: FloatArray, val bucketCount: Int,
                   val durationUs: Long = 0L) {
    private val peakValues = peaks.copyOf()
    private val rmsValues = rms.copyOf()
    // Compatibility accessors return copies: cached analysis cannot be mutated by a UI consumer.
    val peaks: FloatArray get() = peakValues.copyOf()
    val rms: FloatArray get() = rmsValues.copyOf()
    init {
        require(bucketCount in 1..4096 && peaks.size == bucketCount && rms.size == bucketCount)
        require(durationUs >= 0L)
        require(peaks.indices.all { peaks[it].isFinite() && rms[it].isFinite() &&
            peaks[it] in 0f..1f && rms[it] in 0f..peaks[it] })
    }
    fun peakAt(index: Int): Float = peakValues[index]
    fun rmsAt(index: Int): Float = rmsValues[index]
}
