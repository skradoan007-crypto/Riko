package com.rikodev.music.android.core.designsystem.components

import com.rikodev.music.android.audiofx.WaveformData
import kotlin.math.ln
import kotlin.math.sqrt

/** Audio-only, time-aligned profiles. Positive weighted smoothing cannot invent
 * peaks beyond its input window; digital silence remains zero. There is no
 * random oscillator, phase, travelling packet or per-frame height mutation.
 */
internal class LiquidEnvelope private constructor(
    val body: FloatArray, val depth: FloatArray, val crest: FloatArray
) {
    companion object {
        fun from(data: WaveformData, sampleCount: Int = 160): LiquidEnvelope {
            val n = sampleCount.coerceIn(32, 256)
            val peak = FloatArray(n)
            val rms = FloatArray(n)
            for (i in 0 until n) {
                val begin = i.toDouble() * data.bucketCount / n
                val end = (i + 1.0) * data.bucketCount / n
                var energy = 0.0
                var weight = 0.0
                for (j in begin.toInt() until kotlin.math.ceil(end).toInt().coerceAtMost(data.bucketCount)) {
                    val overlap = (minOf(end, j + 1.0) - maxOf(begin, j.toDouble())).coerceAtLeast(0.0)
                    peak[i] = maxOf(peak[i], data.peakAt(j))
                    energy += data.rmsAt(j).toDouble() * data.rmsAt(j) * overlap
                    weight += overlap
                }
                rms[i] = if (weight > 0.0) sqrt(energy / weight).toFloat() else 0f
            }
            val sorted = FloatArray(data.bucketCount) { data.peakAt(it) }.apply { sort() }
            val reference = sorted[((sorted.size - 1) * 0.95f).toInt()]
            // Shared gain for peak AND RMS, capped at 2x. A single outlier
            // cannot flatten a song, and very quiet files are not made loud.
            val gain = 1f / reference.coerceAtLeast(0.5f)
            fun compress(value: Float): Float = (ln(1f + 3f * value * gain) / ln(4f)).coerceIn(0f, 1f)
            val p = FloatArray(n) { compress(peak[it]) }
            val e = FloatArray(n) { compress(rms[it]) }
            val body = smooth(FloatArray(n) { p[it] * 0.28f + e[it] * 0.72f }, maxOf(2, n / 32))
            val depth = smooth(FloatArray(n) { p[it] * 0.12f + e[it] * 0.88f }, maxOf(3, n / 18))
            val crest = smooth(FloatArray(n) { (p[it] - e[it]).coerceAtLeast(0f) * 0.55f + e[it] * 0.42f }, maxOf(2, n / 26))
            for (i in 0 until n) {
                if (peak[i] < 0.00003f) { body[i] = 0f; depth[i] = 0f; crest[i] = 0f }
                // Feather track start/end without shifting its time alignment.
                val u = i.toFloat() / (n - 1)
                val fade = smoothstep(u / 0.025f) * smoothstep((1f - u) / 0.025f)
                body[i] *= fade; depth[i] *= fade; crest[i] *= fade
            }
            return LiquidEnvelope(body, depth, crest)
        }
        private fun smoothstep(value: Float): Float {
            val t = value.coerceIn(0f, 1f)
            return t * t * (3f - 2f * t)
        }
        private fun smooth(input: FloatArray, radius: Int): FloatArray = FloatArray(input.size) { i ->
            var sum = 0f
            var weights = 0f
            for (j in maxOf(0, i - radius)..minOf(input.lastIndex, i + radius)) {
                val weight = (radius + 1 - kotlin.math.abs(i - j)).toFloat()
                sum += input[j] * weight; weights += weight
            }
            sum / weights
        }
    }
}
