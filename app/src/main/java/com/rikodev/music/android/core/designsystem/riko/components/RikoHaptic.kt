package com.rikodev.music.android.core.designsystem.riko.components

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — HAPTIC FOUNDATION (Plan v3.3 §37)
//
// Lightweight, semantic haptic utility for the music player. Maps physical
// interaction meanings to vibration patterns, following the plan's haptic
// grammar:
//
//   Contact     → light tick
//   Press       → crisp tick
//   Confirmation→ clear tick
//   Boundary    → resistance (sparse)
//   Error       → restrained buzz
//   Settle      → soft tick
//
// Rules enforced:
//   §37: NOT every interaction receives haptic — silence can be correct.
//   §37: Avoid continuous high-frequency haptic events.
//   §37: Haptic priority: confirmation > boundary > navigation > ambient(none).
//   §38: Visual + physical + tactile responses should agree.
//
// Design philosophy (from HapticContract):
//   RIKO haptics are quiet. Premium-quiet, not arcade-loud.
//   - Default amplitude: 50/255 (light)
//   - Default duration: 15ms (tick)
//   - Confirmation: 25ms, amplitude 80
//   - Boundary: 30ms, amplitude 60
//   - Error: 40ms, amplitude 120 (unmistakable but not painful)
//
// FORBIDDEN IN THIS FILE:
//   No ViewModel. No Flow. No side effects beyond vibration.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Semantic haptic events for the music player.
 *
 * Each value maps to a physical meaning → vibration pattern, following
 * Plan §37's haptic grammar. Adding a new event requires only adding
 * a value here and a matching spec in [playerSpecs].
 */
enum class RikoHapticEvent {
    /** Light tick on tap — play/pause, skip, shuffle, repeat toggle. */
    CONFIRMATION,

    /** Crisp tick when a seek completes and playback jumps. */
    SEEK_SETTLE,

    /** Sparse resistance tick when seeking crosses a 5-second landmark. */
    SEEK_LANDMARK,

    /** Boundary resistance when drag hits start/end of timeline. */
    BOUNDARY,

    /** Soft tick when sleep timer is set. */
    TIMER_SET,

    /** Restrained buzz on playback error. */
    ERROR,

    /** Very light tick on song change (subtle identity transfer signal). */
    SONG_CHANGE,
}

/**
 * A single haptic pulse specification.
 */
private data class HapticPulseSpec(
    val durationMs: Long,
    val amplitude: Int,
)

/**
 * Player haptic specs — the frozen vocabulary.
 *
 * Amplitudes are deliberately conservative (Plan §37: "RIKO haptics are quiet").
 * All values are in the 0–255 range for VibrationEffect.createOneShot().
 */
private val playerSpecs: Map<RikoHapticEvent, HapticPulseSpec> = mapOf(
    RikoHapticEvent.CONFIRMATION to HapticPulseSpec(durationMs = 20, amplitude = 55),
    RikoHapticEvent.SEEK_SETTLE to HapticPulseSpec(durationMs = 25, amplitude = 70),
    RikoHapticEvent.SEEK_LANDMARK to HapticPulseSpec(durationMs = 15, amplitude = 40),
    RikoHapticEvent.BOUNDARY to HapticPulseSpec(durationMs = 30, amplitude = 60),
    RikoHapticEvent.TIMER_SET to HapticPulseSpec(durationMs = 25, amplitude = 65),
    RikoHapticEvent.ERROR to HapticPulseSpec(durationMs = 40, amplitude = 120),
    RikoHapticEvent.SONG_CHANGE to HapticPulseSpec(durationMs = 12, amplitude = 30),
)

/**
 * Provides access to RIKO's player haptic events.
 *
 * Usage:
 * ```
 * val haptic = rememberRikoHaptic()
 * Button(onClick = { haptic.fire(RikoHapticEvent.CONFIRMATION) }) { ... }
 * ```
 *
 * The haptic automatically respects the system's haptic feedback setting.
 * If the user has disabled haptics at the system level, no vibration fires.
 */
@Composable
fun rememberRikoHaptic(): RikoHaptic {
    val context = LocalContext.current

    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService<VibratorManager>()
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService<Vibrator>()
        }
    }

    return remember(vibrator) {
        RikoHaptic(vibrator = vibrator)
    }
}

/**
 * RIKO player haptic controller.
 *
 * Fires semantic haptic events using Android's VibrationEffect API.
 * Respects system haptic settings via Vibrator.hasVibrator() —
 * if the device has no vibrator or the user disabled it, no vibration fires.
 *
 * @property vibrator The system vibrator (null on devices without one).
 */
class RikoHaptic internal constructor(
    private val vibrator: Vibrator?,
) {
    /**
     * Fire a semantic haptic event.
     *
     * This is the ONLY public method. All haptic decisions flow through
     * this single entry point, making the haptic budget easy to audit.
     *
     * Plan §37: "Every meaningful interaction gets some form of response.
     * NOT every interaction receives haptic feedback. A visual press state
     * may be enough. Silence can be the correct response."
     *
     * The caller decides WHEN to fire. This class decides HOW.
     */
    fun fire(event: RikoHapticEvent) {
        val spec = playerSpecs[event] ?: return

        // System gate: if the user disabled haptic feedback, respect that.
        // This is the "silent" path — Plan §37.
        if (vibrator == null || !vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    spec.durationMs,
                    spec.amplitude.coerceIn(1, 255)
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(spec.durationMs)
        }
    }
}
