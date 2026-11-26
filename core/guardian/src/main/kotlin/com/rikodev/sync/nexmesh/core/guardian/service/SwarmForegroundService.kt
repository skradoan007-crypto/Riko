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
 * Handles Android 15 dataSync 6-hour timeout via onTimeout() with graceful checkpoint flush & lock release.
 */
class SwarmForegroundService : Service() {

    private lateinit var lockManager: SwarmLockManager

    override fun onCreate() {
        super.onCreate()
        lockManager = SwarmLockManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            gracefulStop("Stopped by user")
            return START_NOT_STICKY
        }

        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Nex Mesh Active"
        val body = intent?.getStringExtra(EXTRA_BODY) ?: "Encrypted P2P engine running"
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

        lockManager.acquireForActiveTransfer()
        return START_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        // Android 15 (API 35) mandatory dataSync timeout handler
        onTimeoutCallback?.invoke()
        gracefulStop("Android 15 dataSync session limit reached; checkpoints saved")
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

        @Volatile
        var onTimeoutCallback: (() -> Unit)? = null

        @Volatile
        var onGracefulShutdownCallback: ((String) -> Unit)? = null

        fun start(context: Context, title: String, body: String) {
            val intent = Intent(context, SwarmForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_BODY, body)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, SwarmForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
