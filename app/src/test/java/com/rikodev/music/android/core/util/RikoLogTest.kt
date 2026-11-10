package com.rikodev.music.android.core.util

import android.util.Log
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog

/**
 * Phase 27 (C8) contract: debug/info logs must not spam release logcat;
 * warn/error are essential telemetry and always log.
 *
 * Robolectric captures android.util.Log into ShadowLog so we can assert
 * the gate without a device. Each test uses a UNIQUE tag — ShadowLog has
 * no clearLogs() in this Robolectric version, so isolation is per-tag.
 */
@RunWith(RobolectricTestRunner::class)
class RikoLogTest {

    @Test
    fun debug_logsInDebugBuild() {
        val tag = "RikoLogTest_debug"
        RikoLog.d(tag, "debug message")
        val tagged = ShadowLog.getLogsForTag(tag)
        // In debug (BuildConfig.DEBUG = true in unit tests) message must land.
        if (com.rikodev.music.android.BuildConfig.DEBUG) {
            assertTrue("debug build must log via RikoLog.d", tagged.any { it.msg == "debug message" })
        } else {
            assertFalse("release build must NOT log via RikoLog.d", tagged.any { it.msg == "debug message" })
        }
    }

    @Test
    fun info_gatedSameAsDebug() {
        val tag = "RikoLogTest_info"
        RikoLog.i(tag, "info message")
        val tagged = ShadowLog.getLogsForTag(tag)
        if (com.rikodev.music.android.BuildConfig.DEBUG) {
            assertTrue(tagged.any { it.msg == "info message" })
        } else {
            assertFalse(tagged.any { it.msg == "info message" })
        }
    }

    @Test
    fun warn_alwaysLogs() {
        val tag = "RikoLogTest_warn"
        RikoLog.w(tag, "warn message")
        val tagged = ShadowLog.getLogsForTag(tag)
        assertTrue("warn must always log (essential telemetry)", tagged.any { it.msg == "warn message" })
    }

    @Test
    fun error_alwaysLogs() {
        val tag = "RikoLogTest_error"
        RikoLog.e(tag, "error message")
        val tagged = ShadowLog.getLogsForTag(tag)
        assertTrue("error must always log (essential telemetry)", tagged.any { it.msg == "error message" })
    }

    @Test
    fun debug_lazyLambda_notEvaluatedInRelease() {
        var evaluated = false
        RikoLog.d("RikoLogTest_lazy") {
            evaluated = true
            "lazy"
        }
        if (com.rikodev.music.android.BuildConfig.DEBUG) {
            assertTrue("debug build must evaluate the lazy message", evaluated)
        } else {
            assertFalse("release build must NOT evaluate the lazy message", evaluated)
        }
    }
}
