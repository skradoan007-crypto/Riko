package com.rikodev.music.android.dna.render

/**
 * Quality tiers. [pointFactor] scales a template's MID point budget; the budgets
 * are STARTING GUESSES that must be tuned from real FPS / frame-time numbers.
 */
enum class MdnaQuality(val label: String, val pointFactor: Float, val blurPasses: Int, val pointScale: Float) {
    LOW("Low", 0.3125f, 1, 0.9f),
    MID("Mid", 1.0f, 1, 1.0f),
    HIGH("High", 2.5f, 2, 1.1f);

    fun next(): MdnaQuality = entries[(ordinal + 1) % entries.size]

    /** One tier cheaper, or null when already at the floor. */
    fun lower(): MdnaQuality? = if (ordinal == 0) null else entries[ordinal - 1]
}
