package com.rikodev.music.android.audiofx

import java.nio.ByteBuffer

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — PCM FRAME READER (visualizer_plan §21, Phase-1/2 test matrix)
//
// Pure, Android-free decoding of the interleaved PCM that ExoPlayer hands
// the spectrum tap, so the format handling can be unit-tested on the JVM —
// no device, no android.util.Log stub:
//
//   44.1k / 48k · mono / stereo · 16-bit / float
//
// The two encodings named here are exactly the two Media3's AudioSink can
// negotiate. Anything else is decoded at 16-bit width and PcmAudioProcessor
// logs a warning rather than silently producing a meaningless spectrum.
//
// The encoding ints mirror androidx.media3.common.C, but are declared
// locally so this file has zero Android / Media3 imports:
//   C.ENCODING_PCM_16BIT = 2 · C.ENCODING_PCM_FLOAT = 4
// ─────────────────────────────────────────────────────────────────────────────

object PcmFrames {

    /** Mirrors `C.ENCODING_PCM_16BIT`. */
    const val ENCODING_PCM_16BIT = 2

    /** Mirrors `C.ENCODING_PCM_FLOAT`. */
    const val ENCODING_PCM_FLOAT = 4

    /** Bytes per single sample for [encoding]; anything unknown → 16-bit. */
    fun bytesPerSample(encoding: Int): Int =
        if (encoding == ENCODING_PCM_FLOAT) 4 else 2

    /** Bytes per interleaved frame — every channel of one instant. */
    fun strideBytes(encoding: Int, channels: Int): Int =
        bytesPerSample(encoding) * channels.coerceAtLeast(1)

    /** Whole frames available from [startByte] to the buffer's limit. */
    fun frameCount(buffer: ByteBuffer, startByte: Int, stride: Int): Int {
        if (stride <= 0) return 0
        val span = buffer.limit() - startByte
        return if (span <= 0) 0 else span / stride
    }

    /**
     * Mono-mixes the interleaved frame that begins at absolute
     * [frameByteOffset] in [buffer].
     *
     * Uses ABSOLUTE reads only — the buffer's own position is never moved,
     * which is what keeps the downstream passthrough byte-exact.
     *
     * @return the mean of all channels in [-1, 1]; 0f for a truncated tail.
     */
    fun monoFrame(
        buffer: ByteBuffer,
        frameByteOffset: Int,
        encoding: Int,
        channels: Int
    ): Float {
        val ch = channels.coerceAtLeast(1)
        val width = bytesPerSample(encoding)
        if (frameByteOffset < 0) return 0f
        if (frameByteOffset + width * ch > buffer.limit()) return 0f
        val isFloat = encoding == ENCODING_PCM_FLOAT
        var acc = 0f
        for (c in 0 until ch) {
            val offset = frameByteOffset + c * width
            acc += if (isFloat) {
                buffer.getFloat(offset)
            } else {
                buffer.getShort(offset).toFloat() / 32768f
            }
        }
        return acc / ch
    }
}
