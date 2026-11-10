package com.rikodev.music.android.core.util

import android.util.Log
import com.rikodev.music.android.BuildConfig

/**
 * Release-noise guard (Phase C8). Debug-level logs cost startup/scroll time
 * in release builds and add logcat spam during playback. [d] is a no-op in
 * release; warn/error always log (essential telemetry, Plan.txt §13 rule:
 * "remove noisy logs from release while preserving actionable crash/error
 * telemetry").
 */
object RikoLog {
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.d(tag, message)
    }

    fun d(tag: String, message: () -> String) {
        if (BuildConfig.DEBUG) Log.d(tag, message())
    }

    fun i(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.i(tag, message)
    }

    fun w(tag: String, message: String, tr: Throwable? = null) {
        Log.w(tag, message, tr)
    }

    fun e(tag: String, message: String, tr: Throwable? = null) {
        Log.e(tag, message, tr)
    }
}
