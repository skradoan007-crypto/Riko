package com.rikodev.music.android.core.designsystem.components

/** The same coordinate system is used for draw, touch and accessibility. */
internal object FluidTimelineGeometry {
    const val MAX_AMPLITUDE_DP = 24f
    const val HEAD_RADIUS_DP = 6.2f
    fun safeFraction(value: Float): Float = if (value.isFinite()) value.coerceIn(0f, 1f) else 0f
    fun fraction(x: Float, width: Float, inset: Float): Float {
        val span = width - 2f * inset
        return if (span <= 0f) 0f else safeFraction((x - inset) / span)
    }
    fun head(progress: Float, width: Float, inset: Float): Float =
        inset + safeFraction(progress) * (width - 2f * inset).coerceAtLeast(0f)
}

/** Monotone cubic Hermite interpolation. Harmonic-mean slopes flatten at
 * extrema; Bezier controls remain inside the neighbouring sample bounds.
 * No Catmull-Rom overshoot followed by a discontinuous clamp.
 */
internal class LiquidCurve(val values: FloatArray) {
    val slopes = FloatArray(values.size)
    init {
        require(values.size >= 2)
        for (i in 1 until values.lastIndex) {
            val a = values[i] - values[i - 1]
            val b = values[i + 1] - values[i]
            slopes[i] = if (a * b <= 0f) 0f else 2f * a * b / (a + b)
        }
    }
    fun sample(fraction: Float): Float {
        val position = FluidTimelineGeometry.safeFraction(fraction) * values.lastIndex
        val i = position.toInt().coerceAtMost(values.lastIndex - 1)
        val t = position - i
        val a = values[i]
        val b = values[i + 1]
        val lo = minOf(a, b)
        val hi = maxOf(a, b)
        val c1 = (a + slopes[i] / 3f).coerceIn(lo, hi)
        val c2 = (b - slopes[i + 1] / 3f).coerceIn(lo, hi)
        val s = 1f - t
        return s * s * s * a + 3f * s * s * t * c1 + 3f * s * t * t * c2 + t * t * t * b
    }
}
