package com.rikodev.music.android.dna.core

// ─────────────────────────────────────────────────────────────────────────────
// RIKO DNA (MDNA) — QUALITY GOVERNOR
//
// "Smoothest in the world" includes WEAK phones: when frames keep missing the
// display's budget, drop one quality tier instead of stuttering. It only ever
// steps DOWN (no flapping), waits out shader warm-up after start and after every
// change, and ignores one-off hitches (pause/resume, rotation).
//
// Generic over the tier type so it stays pure (unit-tested on the JVM).
// ─────────────────────────────────────────────────────────────────────────────
class QualityGovernor(refreshRateHz: Float) {

    private val budgetMs: Float = 1000f / refreshRateHz.coerceIn(30f, 240f)
    private var sinceChangeMs = 0f
    private var overMs = 0f

    @Volatile var enabled: Boolean = true

    /**
     * Feed one frame time. Returns true exactly when the caller should step
     * DOWN one quality tier now.
     */
    fun onFrame(dtMs: Float, canStepDown: Boolean): Boolean {
        if (dtMs > HITCH_MS) return false          // pause/resume hitch — not a signal
        sinceChangeMs += dtMs
        if (!enabled || !canStepDown) {
            overMs = 0f
            return false
        }
        if (sinceChangeMs < WARMUP_MS) return false
        overMs = if (dtMs > budgetMs * SLOW_FACTOR) overMs + dtMs else (overMs - dtMs * 2f).coerceAtLeast(0f)
        if (overMs >= OVER_LIMIT_MS) {
            overMs = 0f
            sinceChangeMs = 0f
            return true
        }
        return false
    }

    /** Call when quality is changed from outside (user pressed a button). */
    fun reset() {
        sinceChangeMs = 0f
        overMs = 0f
    }

    private companion object {
        const val SLOW_FACTOR = 1.6f
        const val OVER_LIMIT_MS = 1500f
        const val WARMUP_MS = 2500f
        const val HITCH_MS = 250f
    }
}
