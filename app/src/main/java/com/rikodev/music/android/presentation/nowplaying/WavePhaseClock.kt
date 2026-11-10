package com.rikodev.music.android.presentation.nowplaying

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.exp

/**
 * Phase clock for the wave/plasma visuals.
 *
 * C5: the frame loop is gated on [active] using the WaveformTimeline idiom —
 * `snapshotFlow` + `collectLatest` so the `while(true)` loop is **cancelled**
 * when the surface is hidden (sheet collapsed / lab closed / background),
 * instead of ticking physics every frame while invisible. Default `{ true }`
 * keeps existing callers unchanged.
 *
 * [speedTarget] is captured once (LaunchedEffect(Unit) semantics). It must
 * close over *State delegates* (`isPlaying`, `prefs.motionSpeed`) to see live
 * values — plain params would freeze at first composition.
 */
@Composable
fun rememberWavePhase(
    speedTarget: () -> Float,
    active: () -> Boolean = { true }
): State<Float> {
    val out = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        snapshotFlow { active() }
            .collectLatest { isActive ->
                if (!isActive) return@collectLatest
                var last = 0L
                var speed = 0f
                var phase = out.floatValue
                while (true) {
                    withFrameNanos { now ->
                        val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f)
                        last = now
                        speed += (speedTarget() - speed) * (1f - exp(-dt / 0.6f))
                        phase += dt * speed
                        out.floatValue = phase.toFloat()
                    }
                }
            }
    }
    return out
}
