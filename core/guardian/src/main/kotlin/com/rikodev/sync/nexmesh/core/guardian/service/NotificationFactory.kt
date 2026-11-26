package com.rikodev.sync.nexmesh.core.guardian.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationFactory {
    const val CHANNEL_ID = "nexmesh_swarm_service_channel"
    const val NOTIFICATION_ID = 47701

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nex Mesh Swarm Engine",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active peer-to-peer encrypted streaming and node hosting"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    fun buildServiceNotification(
        context: Context,
        title: String,
        body: String,
        stopPendingIntent: PendingIntent
    ): Notification {
        ensureChannel(context)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle(title)
            .setContentText(body)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "Stop Swarm", stopPendingIntent)
            .build()
    }
}
