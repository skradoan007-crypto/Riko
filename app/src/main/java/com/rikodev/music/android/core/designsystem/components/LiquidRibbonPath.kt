package com.rikodev.music.android.core.designsystem.components

import androidx.compose.ui.graphics.Path
import kotlin.math.floor

/** Size-dependent cached full path, plus a tiny reusable moving-edge path.
 * Regular progress never rebuilds the full-song outline. Only the final ~5%
 * (at most 31 cubic segments) is tapered into the seek-circle centreline.
 */
internal class LiquidRibbonPath(values: FloatArray, private val left: Float, private val span: Float,
                                private val baseline: Float, private val amplitude: Float) {
    private val curve = LiquidCurve(values)
    val full = Path()
    val tail = Path()
    var seam = left
        private set
    private var lastHead = Float.NaN
    private val tx = FloatArray(32)
    private val ty = FloatArray(32)
    private val slopes = FloatArray(32)
    init {
        full.moveTo(left, baseline)
        full.lineTo(left, baseline - values[0] * amplitude)
        val step = span / values.lastIndex
        for (i in 0 until values.lastIndex) {
            val a = values[i]; val b = values[i + 1]
            val lo = minOf(a, b); val hi = maxOf(a, b)
            val x = left + i * step
            full.cubicTo(x + step / 3f, baseline - (a + curve.slopes[i] / 3f).coerceIn(lo, hi) * amplitude,
                x + step * 2f / 3f, baseline - (b - curve.slopes[i + 1] / 3f).coerceIn(lo, hi) * amplitude,
                x + step, baseline - b * amplitude)
        }
        full.lineTo(left + span, baseline); full.lineTo(left, baseline); full.close()
    }
    fun updateHead(head: Float, taperPx: Float) {
        if (head == lastHead) return
        lastHead = head
        val step = span / curve.values.lastIndex
        seam = left + floor(((head - taperPx - left) / step).coerceAtLeast(0f)) * step
        val distance = (head - seam).coerceAtLeast(0f)
        val count = (kotlin.math.ceil(distance / step).toInt() + 2).coerceIn(2, tx.size)
        for (i in 0 until count) {
            tx[i] = seam + distance * i / (count - 1)
            val t = if (distance > 0f) ((head - tx[i]) / distance).coerceIn(0f, 1f) else 0f
            ty[i] = curve.sample((tx[i] - left) / span) * t * t * (3f - 2f * t)
        }
        slopes.fill(0f)
        for (i in 1 until count - 1) {
            val a = ty[i] - ty[i - 1]; val b = ty[i + 1] - ty[i]
            slopes[i] = if (a * b <= 0f) 0f else 2f * a * b / (a + b)
        }
        tail.reset(); tail.moveTo(seam, baseline); tail.lineTo(seam, baseline - ty[0] * amplitude)
        for (i in 0 until count - 1) {
            val dx = (tx[i + 1] - tx[i]) / 3f
            val lo = minOf(ty[i], ty[i + 1]); val hi = maxOf(ty[i], ty[i + 1])
            tail.cubicTo(tx[i] + dx, baseline - (ty[i] + slopes[i] / 3f).coerceIn(lo, hi) * amplitude,
                tx[i + 1] - dx, baseline - (ty[i + 1] - slopes[i + 1] / 3f).coerceIn(lo, hi) * amplitude,
                tx[i + 1], baseline - ty[i + 1] * amplitude)
        }
        tail.lineTo(head, baseline); tail.lineTo(seam, baseline); tail.close()
    }
}
