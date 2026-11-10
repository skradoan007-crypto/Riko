package com.rikodev.music.android.core.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — DEVICE TILT (for the parallax album art)
//
// Reads the accelerometer and exposes a smoothed, small-range tilt value —
// the "surprise" detail: tilt the phone and the album art responds like a
// physical object sitting in your hand, not a flat image. Deliberately
// subtle (clamped range) so it reads as premium physics, not a gimmick.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun rememberDeviceTilt(): State<Pair<Float, Float>> {
    val context = LocalContext.current
    val tiltX = remember { mutableFloatStateOf(0f) }
    val tiltY = remember { mutableFloatStateOf(0f) }

    val tiltState = remember {
    derivedStateOf {
        tiltX.floatValue to tiltY.floatValue
    }
}

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // event.values[0] = X tilt (left/right), [1] = Y tilt (up/down),
                // range roughly -9.8..9.8 (gravity). Smooth + clamp to a small,
                // premium-feeling range rather than a jumpy full-range response.
                val rawX = (event.values.getOrElse(0) { 0f } / 9.8f).coerceIn(-1f, 1f)
                val rawY = (event.values.getOrElse(1) { 0f } / 9.8f).coerceIn(-1f, 1f)
                tiltX.floatValue = tiltX.floatValue * 0.85f + rawX * 0.15f
                tiltY.floatValue = tiltY.floatValue * 0.85f + rawY * 0.15f
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    return tiltState
}
