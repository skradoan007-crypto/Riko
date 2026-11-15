package com.rikodev.music.android.audiofx

import android.util.Log

/**
 * Debug logger for visual audio pipeline.
 * Enable with: adb shell setprop log.tag.RikoVisual VERBOSE
 */
object VisualDebug {
    private const val TAG = "RikoVisual"
    private var enabled = true  // Set to false in production

    fun log(message: String) {
        if (enabled) Log.d(TAG, message)
    }

    fun logAudio(hasSignal: Boolean, bass: Float, mid: Float, fastSum: Float) {
        if (enabled) {
            Log.d(TAG, "Audio: hasSignal=$hasSignal bass=$bass mid=$mid fastSum=$fastSum")
        }
    }

    fun logUniform(name: String, value: Float) {
        if (enabled) Log.d(TAG, "Uniform: $name=$value")
    }

    fun logError(message: String) {
        Log.e(TAG, message)
    }
}
