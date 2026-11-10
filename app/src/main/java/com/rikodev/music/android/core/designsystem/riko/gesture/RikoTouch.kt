package com.rikodev.music.android.core.designsystem.riko.gesture

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.theme.RikoStateTokens

/**
 * Canonical touch vocabulary (plan 2.3) — the ONE source for every gesture
 * dimension in RIKO. Screens and design-system components read tokens from
 * here; nothing hand-rolls its own scale, radius or target size again.
 *
 * Slop and long-press timing are deliberately NOT here: those always come
 * from `LocalViewConfiguration` (device/accessibility dependent).
 */
object RikoTouch {

    /** Platform minimum interactive target — pad hit areas, never shrink. */
    val TouchTarget: Dp = 48.dp

    /** Press depression — the single scale for every pressable surface. */
    val PressScale: Float = RikoStateTokens.PressScale

    /** Grabbed node while a value-drag is live (EQ bands, seek thumbs). */
    const val GrabScale: Float = 1.35f

    /** Lift scale for a row being reordered. */
    const val DragLiftScale: Float = 1.03f

    /** Node hit-test radius for grab gestures (EQ knobs, thumbs). */
    val GrabRadius: Dp = 28.dp

    /** Visual emphasis halo radius around a grabbed node. */
    val GrabGlowRadius: Dp = 22.dp
}
