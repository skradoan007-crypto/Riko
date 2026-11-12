package com.rikodev.music.android.audiofx.visualizer

import com.rikodev.music.android.audiofx.PcmFrames
import com.rikodev.music.android.audiofx.SpectrumBands
import com.rikodev.music.android.core.experience.audio.Fft
import java.nio.ByteBuffer
import kotlin.math.cos
import kotlin.math.sqrt

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — SPECTRUM ENGINE — **the single canonical DSP owner**
//
// This class is the one place in the project where audio becomes spectrum:
//
//   Media3 PCM AudioProcessor
//        └─ PcmAudioProcessor ....... thin playback-facing adapter
//             ├─ PcmFrames ........... format-aware byte → frame
//             └─ SpectrumEngine ...... ◀── THIS FILE (canonical DSP)
//                  ├─ Hann window
//                  ├─ Fft ............ radix-2 (core/experience/audio/Fft.kt)
//                  ├─ magnitude → dB
//                  └─ SpectrumBands .. log bands + smoothing + tuning constants
//
// There used to be three copies of this pipeline (an inline radix-2 inside
// PcmAudioProcessor, this engine, and a dead TeeAudioTap). The duplicates are
// gone: an algorithm lives here once, or it does not live at all.
//
// Zero allocation after construction — window, FFT tables, complex buffers and
// band accumulators are preallocated, so analyze() is safe on the audio thread.
// ─────────────────────────────────────────────────────────────────────────────

class SpectrumEngine(val fftSize: Int = SpectrumConfig.FFT_BALANCED) {

    private val fft = Fft(fftSize)

    /** Hann window (periodic form), precomputed once. */
    private val hann = FloatArray(fftSize) { i ->
        (0.5f * (1f - cos(2.0 * Math.PI * i / (fftSize - 1)).toFloat()))
    }

    // ── Workspace — owned by the engine, never reallocated per frame ──
    private val real = FloatArray(fftSize)
    private val imag = FloatArray(fftSize)
    private val binSum = FloatArray(SpectrumBands.COUNT)
    private val binCount = IntArray(SpectrumBands.COUNT)
    private val mdnaPower = DoubleArray(SpectrumBands.COUNT)
    private val windowEnergy = hann.sumOf { (it * it).toDouble() }

    /** Bytes per interleaved frame for a given format. */
    fun strideBytes(encoding: Int, channels: Int): Int =
        PcmFrames.strideBytes(encoding, channels)

    /**
     * CANONICAL analysis entry point.
     *
     * Reads [frameCount] interleaved frames starting at absolute [startByte]
     * of [buffer] (absolute reads only — the caller's buffer position is never
     * touched), mono-mixes them, applies the Hann window, runs the FFT and
     * writes the 40 logarithmic, dB-normalized, tilt-compensated band levels
     * into [outTargets].
     *
     * The caller owns the "enough audio yet" guard and picks [startByte] so the
     * window covers the NEWEST audio available.
     *
     * @return frames actually placed in the window (0 when nothing was read).
     */
    fun analyze(
        buffer: ByteBuffer,
        startByte: Int,
        frameCount: Int,
        encoding: Int,
        channels: Int,
        sampleRateHz: Int,
        outTargets: FloatArray,
        outMdna: FloatArray? = null
    ): Int {
        require(outTargets.size == SpectrumBands.COUNT) {
            "outTargets must hold ${SpectrumBands.COUNT} bands"
        }
        if (sampleRateHz <= 0 || frameCount <= 0) {
            outTargets.fill(0f)
            outMdna?.fill(0f)
            return 0
        }
        val stride = PcmFrames.strideBytes(encoding, channels)
        // If more frames arrived than the window holds, keep the newest ones.
        val windowFirst = (frameCount - fftSize).coerceAtLeast(0)
        val take = (frameCount - windowFirst).coerceAtMost(fftSize)

        var n = 0
        while (n < take) {
            real[n] = PcmFrames.monoFrame(
                buffer,
                startByte + (windowFirst + n) * stride,
                encoding,
                channels
            ) * hann[n]
            imag[n] = 0f
            n++
        }
        while (n < fftSize) {
            real[n] = 0f
            imag[n] = 0f
            n++
        }

        fft.forward(real, imag)
        reduceToBands(sampleRateHz, outTargets, outMdna)
        return take
    }

    /**
     * Convenience entry point for already-deinterleaved 16-bit PCM — offline
     * analysis and unit tests. Shares the identical window → FFT → band path,
     * so there is no second algorithm to keep in sync.
     */
    fun analyzePcm16(
        pcm: ShortArray,
        channels: Int,
        sampleRateHz: Int,
        outTargets: FloatArray,
        outMdna: FloatArray? = null
    ) {
        require(outTargets.size == SpectrumBands.COUNT) {
            "outTargets must hold ${SpectrumBands.COUNT} bands"
        }
        if (sampleRateHz <= 0 || pcm.isEmpty()) {
            outTargets.fill(0f)
            outMdna?.fill(0f)
            return
        }
        val ch = channels.coerceAtLeast(1)
        val frames = pcm.size / ch
        val windowFirst = (frames - fftSize).coerceAtLeast(0)
        val take = (frames - windowFirst).coerceAtMost(fftSize)

        var n = 0
        while (n < take) {
            val base = (windowFirst + n) * ch
            var acc = 0f
            for (c in 0 until ch) acc += pcm[base + c] / 32768f
            real[n] = (acc / ch) * hann[n]
            imag[n] = 0f
            n++
        }
        while (n < fftSize) {
            real[n] = 0f
            imag[n] = 0f
            n++
        }

        fft.forward(real, imag)
        reduceToBands(sampleRateHz, outTargets, outMdna)
    }

    /**
     * Bin → magnitude → dB → logarithmic band aggregation, shared by both entry
     * points. A band's energy is the MEAN magnitude of the bins inside it, which
     * keeps the wide high-frequency bands from shouting over the narrow bass ones.
     */
    private fun reduceToBands(sampleRateHz: Int, outTargets: FloatArray, outMdna: FloatArray?) {
        require(outMdna == null || outMdna.size == SpectrumBands.COUNT)
        mdnaPower.fill(0.0)
        binSum.fill(0f)
        binCount.fill(0)
        val hzPerBin = sampleRateHz.toFloat() / fftSize
        val nyquistBin = fftSize / 2
        for (bin in 1 until nyquistBin) {
            val mag = sqrt(real[bin] * real[bin] + imag[bin] * imag[bin])
            val band = SpectrumBands.bandIndex(bin * hzPerBin, sampleRateHz)
            binSum[band] += mag
            if (outMdna != null) mdnaPower[band] += real[bin].toDouble()*real[bin] + imag[bin].toDouble()*imag[bin]
            binCount[band]++
        }
        for (b in 0 until SpectrumBands.COUNT) {
            val avg = if (binCount[b] == 0) 0f else binSum[b] / binCount[b]
            outTargets[b] = (SpectrumBands.dbNormalize(avg) * tiltGainFor(b)).coerceIn(0f, 1f)
            if (outMdna != null) outMdna[b] = MdnaSpectrumMath.level(mdnaPower[b], fftSize, windowEnergy)
        }
    }

    companion object {
        /** Magnitude: sqrt(re² + im²). */
        fun magnitude(re: Float, im: Float): Float = sqrt(re * re + im * im)

        /**
         * Per-band tilt compensation: treble magnitudes are naturally small, so
         * ramp the gain with log position (band 0 → ×1.0, last band → ×2.6).
         */
        fun tiltGainFor(band: Int): Float =
            1f + (band / (SpectrumBands.COUNT - 1f)) * 1.6f
    }
}
