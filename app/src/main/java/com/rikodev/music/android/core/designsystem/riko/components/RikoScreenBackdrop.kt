package com.rikodev.music.android.core.designsystem.riko.components

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.rikodev.music.android.core.designsystem.riko.theme.RikoThemeV2

/**
 * Screen foundation (plan 7.1) — ONE background language across the app:
 * neutral dark foundation + controlled aurora atmosphere (the shell canvas
 * breathes faintly through) + screen-specific depth:
 *
 *   Now Playing  — richest (its own ambient stack, depth 0 = untouched)
 *   Home/search  — depth 0.72..0.78 (content-first)
 *   Settings     — calmer (0.85)
 *   Library      — readability first (0.82)
 *
 * The old split (five screens painting flat #0D0716 over the aurora while
 * others stayed transparent) made the atmosphere change on every tab switch —
 * this helper is the single foundation every screen shares.
 */
fun Modifier.rikoScreenFoundation(depth: Float = 0.78f): Modifier {
    val d = depth.coerceIn(0f, 1f)
    return this.background(
        Brush.verticalGradient(
            listOf(
                RikoThemeV2.Colors.background.copy(alpha = d * 0.55f),
                RikoThemeV2.Colors.background.copy(alpha = d)
            )
        )
    )
}
