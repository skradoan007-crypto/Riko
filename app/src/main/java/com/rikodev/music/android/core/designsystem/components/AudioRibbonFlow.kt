package com.rikodev.music.android.core.designsystem.components

import kotlin.math.exp
import kotlin.math.sqrt

/** Small, allocation-free audio-driven ribbon emitter. Time only transports
 * membranes; their height comes from playback PCM (or a real cached envelope).
 * Silence emits nothing. No random values, sine oscillator or full-song slab.
 */
internal class AudioRibbonFlow {
    private val born = FloatArray(9) { -100f }
    private val levels = FloatArray(9)
    private val filtered = FloatArray(3)
    private val next = floatArrayOf(0f, 0.16f, 0.33f)
    private val slots = IntArray(3)
    private var time = 0f
    var hasHistory = false; private set
    fun reset() {
        born.fill(-100f); levels.fill(0f); filtered.fill(0f); slots.fill(0)
        next[0] = 0f; next[1] = 0.16f; next[2] = 0.33f
        time = 0f; hasHistory = false
    }
    fun advance(seconds: Float, amplitude: Float) {
        if (!seconds.isFinite() || seconds < 0f) return
        val dt = seconds.coerceAtMost(0.05f)
        time += dt
        val input = if (amplitude.isFinite()) amplitude.coerceIn(0f, 1f) else 0f
        for (layer in 0..2) {
            val rate = if (input > filtered[layer]) 18f - layer * 4f else 5f - layer
            filtered[layer] += (input - filtered[layer]) * (1f - exp(-rate * dt))
            if (time >= next[layer]) {
                val index = layer * 3 + slots[layer]
                slots[layer] = (slots[layer] + 1) % 3
                born[index] = time
                // Noise floor suppresses tails; quiet songs do not get a fixed minimum height.
                levels[index] = if (input < 0.0015f) 0f else sqrt(filtered[layer]) * (0.90f - layer * 0.12f)
                if (levels[index] > 0f) hasHistory = true
                next[layer] = time + 0.72f + layer * 0.11f
            }
        }
    }
    fun height(layer: Int, position: Float): Float {
        val u = position.coerceIn(0f, 1f)
        var total = 0f
        for (slot in 0..2) {
            val index = layer * 3 + slot
            val age = (time - born[index]) / (1.65f + layer * 0.20f)
            if (age !in 0f..1f || levels[index] == 0f) continue
            val centre = 1f - age
            val width = 0.15f + 0.31f * smooth(age)
            val distance = kotlin.math.abs(u - centre) / width
            val lobe = 1f - smooth(distance)
            val life = smooth(age / 0.12f) * (1f - smooth((age - 0.72f) / 0.28f))
            total += levels[index] * life * lobe
        }
        // Long left feather; short birth feather joins the circle exactly.
        return (total / (0.65f + total)) * smooth(u / 0.16f) * smooth((1f - u) / 0.08f)
    }
    private fun smooth(value: Float): Float {
        val t = value.coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
}
