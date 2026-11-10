package com.rikodev.music.android.core.designsystem.animation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — REDUCE MOTION
//
// True when the system animator duration scale is 0 (accessibility /
// developer "remove animations"). Screens use this to swap animation for
// instant state changes — rises, scales, marquee and pops become static.
// Previously private to PlayerSheetFull; promoted here so Splash and any
// future surface share one definition (G-7 reduced-motion paths).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        android.provider.Settings.Global.getFloat(
            context.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }
}
