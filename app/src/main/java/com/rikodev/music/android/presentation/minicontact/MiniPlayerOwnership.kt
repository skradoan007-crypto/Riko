package com.rikodev.music.android.presentation.minicontact

/**
 * Ownership model for the Mini Player morph.
 *
 * Determines which component "owns" the visual space during a morph
 * transition. Used by the reducer to decide which component renders
 * the intermediate frames.
 */
object MiniPlayerOwnership {
    /**
     * Determine who owns the morph at a given progress point.
     *
     * @param progress Current morph progress (0 = collapsed, 1 = expanded).
     * @param isForward Whether the morph is expanding (true) or contracting (false).
     * @return The owner label string.
     */
    fun ownerAt(progress: Float, isForward: Boolean): String {
        return when {
            progress < 0.15f -> "MINI_PLAYER"
            progress < 0.85f -> "MORPH_TRANSITION"
            else -> "NOW_PLAYING"
        }
    }
}
