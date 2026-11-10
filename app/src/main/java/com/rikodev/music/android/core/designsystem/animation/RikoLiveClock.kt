package com.rikodev.music.android.core.designsystem.animation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — SHARED LIVE CLOCK
//
// PERFORMANCE: every "living" effect (button float, thumbnail zoom, play-
// button shine, background drift) used to spin up ITS OWN
// rememberInfiniteTransition(). With ~8 buttons on one screen that meant
// 8-16 independent Choreographer-driven animations running forever, even
// completely idle — the real cause of touch/swipe jank.
//
// Fix: ONE infinite transition for the whole app, producing a single
// looping angle (0 → 2π). Every effect reads this one value and adds its
// own phase offset with sin()/cos() — cheap math, zero extra tickers.
// Visually each button still looks independent (different phase = different
// timing) but the OS only has one animation to drive.
//
// RUNTIME FIX 4 (real report — "floating object = lag cause"): this ONE
// remaining transition was still mounted permanently at RikoNavGraph's
// root with zero lifecycle awareness — same root pattern as RuntimeFix1-3.
// Compose's window Recomposer is generally lifecycle-aware, but that
// couldn't be verified against a real device from this sandbox, and the
// RuntimeFix2 lesson stands: freezing the *output* isn't a real fix if the
// underlying loop keeps ticking. So this now actually UNMOUNTS the
// rememberInfiniteTransition on ON_STOP (not just holds its last value)
// and remounts a fresh one on ON_START.
// ─────────────────────────────────────────────────────────────────────────────

private val staticClock = mutableFloatStateOf(0f)

val LocalRikoClock = staticCompositionLocalOf<State<Float>> { staticClock }

/**
 * Wrap a screen (or the whole nav graph) in this once. Everything below —
 * BubbleIconButton floats, Ken Burns zoom, play-button shine, animated
 * background blobs — can then read [LocalRikoClock] instead of creating
 * a new animation.
 */
@Composable
fun ProvideRikoLiveClock(
    periodMillis: Int = 7000,
    content: @Composable () -> Unit
) {
    // Track real Activity visibility — same LifecycleEventObserver pattern
    // already used in RikoNavGraph.kt (RuntimeFix2), nothing new added.
    val lifecycleOwner = LocalLifecycleOwner.current
    var isForeground by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> isForeground = false
                Lifecycle.Event.ON_START -> isForeground = true
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Held value while backgrounded — deliberately the same static default
    // this file already used before ON_STOP/ON_START existed. NOT synced to
    // the live angle: reading angle.value here (in the composable body,
    // outside a draw-scope lambda) would resubscribe this whole composable
    // — and everything in `content()` below it — to recompose on every
    // single animation frame, which is worse than the bug being fixed.
    // Icons simply hold their last-drawn position while backgrounded
    // (nobody's watching) and resume cleanly from 0f on ON_START.
    val clockState: State<Float> = if (isForeground) {
        // Real, live infinite transition — only exists in composition
        // while the app is actually visible. Leaving this `if` branch
        // (isForeground = false) disposes it for real, which is what
        // stops the Choreographer callback loop — not a flag check.
        val transition = rememberInfiniteTransition(label = "riko_live_clock")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(periodMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "riko_clock_angle"
        )
    } else {
        staticClock
    }

    CompositionLocalProvider(LocalRikoClock provides clockState) {
        content()
    }
}

/** 0f..1f smooth oscillation derived from the shared clock, with a phase offset. */
fun oscillate(clockAngle: Float, phaseOffsetMillis: Int = 0, periodMillis: Int = 7000): Float {
    val phaseRadians = (phaseOffsetMillis.toFloat() / periodMillis.toFloat()) * (2 * Math.PI).toFloat()
    return (kotlin.math.sin(clockAngle + phaseRadians) * 0.5f + 0.5f)
}
