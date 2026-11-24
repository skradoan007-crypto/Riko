package com.rikodev.vyrox.android

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

@HiltAndroidApp
class VyRoxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val logDir = File(filesDir, "crash_logs").apply { mkdirs() }
                File(logDir, "last_crash.txt").writeText(
                    "Thread: ${thread.name}\n${sw}",
                    Charsets.UTF_8
                )
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}
