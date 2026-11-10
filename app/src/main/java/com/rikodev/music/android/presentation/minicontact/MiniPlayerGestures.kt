package com.rikodev.music.android.presentation.minicontact

/**
 * Gesture thresholds and constants for the Mini Player.
 *
 * Centralized in the minicontact contract so both the gesture layer
 * (presentation) and the reducer (state) can reference the same values.
 */
object MiniPlayerGestures {
    /** Maximum vertical drag distance (pixels) for morph. */
    const val VERTICAL_MORPH_DISTANCE = 400f

    /** Minimum upward fling velocity (dp/second) to commit expand. */
    const val FLING_EXPAND_VELOCITY = 800f

    /** Minimum downward fling velocity (dp/second) to commit dismiss. */
    const val FLING_DISMISS_VELOCITY = 600f
}
