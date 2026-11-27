package com.rikodev.sync.nexmesh.core.guardian.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.rikodev.sync.nexmesh.core.guardian.power.SwarmLockManager

/**
 * Android 14/15 compliant Foreground Service (connectedDevice | dataSync).
 * - Acquires scoped PARTIAL_WAKE_LOCK + WifiLock ONLY while active tasks are running.
 * - Immediately releases all locks when stopped or idle to prevent unnecessary battery drain.
 * - Handles Android 15 dataSync timeout cleanly via onTimeout().
 */
class SwarmForegroundService : Service() {

    private lateinit var lockManager: SwarmLockManager

    override fun onCreate() {
        super.onCreate()
        lockManager = SwarmLockManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            gracefulStop("ইউজার ব্যাকগ্রাউন্ড সার্ভিস বন্ধ করেছেন")
            return START_NOT_STICKY
        }

        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Nex Mesh ব্যাকগ্রাউন্ডে চালু আছে"
        val body = intent?.getStringExtra(EXTRA_BODY) ?: "নিরাপদ নেটওয়ার্ক কাজ চলমান"
        val needsWakeLock = intent?.getBooleanExtra(EXTRA_NEEDS_WAKELOCK, true) ?: true

        val stopIntent = Intent(this, SwarmForegroundService::class.java).apply { action = ACTION_STOP }
        val stopPi = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationFactory.buildServiceNotification(this, title, body, stopPi)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val fgsType = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            startForeground(NotificationFactory.NOTIFICATION_ID, notification, fgsType)
        } else {
            startForeground(NotificationFactory.NOTIFICATION_ID, notification)
        }

        if (needsWakeLock) {
            lockManager.acquireForActiveTransfer()
        } else {
            lockManager.releaseAll()
        }

        return START_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        onTimeoutCallback?.invoke()
        gracefulStop("সেশন সময়সীমা পূর্ণ হওয়ায় চেকপয়েন্ট সেভ করে থামানো হয়েছে")
    }

    private fun gracefulStop(reason: String) {
        onGracefulShutdownCallback?.invoke(reason)
        lockManager.releaseAll()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        lockManager.releaseAll()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.rikodev.sync.nexmesh.action.START_SWARM"
        const val ACTION_STOP = "com.rikodev.sync.nexmesh.action.STOP_SWARM"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
        const val EXTRA_NEEDS_WAKELOCK = "extra_needs_wakelock"

        @Volatile
        var onTimeoutCallback: (() -> Unit)? = null

        @Volatile
        var onGracefulShutdownCallback: ((String) -> Unit)? = null

        fun start(context: Context, title: String, body: String, needsWakeLock: Boolean = true) {
            val intent = Intent(context, SwarmForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_BODY, body)
                putExtra(EXTRA_NEEDS_WAKELOCK, needsWakeLock)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, SwarmForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            runCatching { context.startService(intent) }
        }
    }
}
