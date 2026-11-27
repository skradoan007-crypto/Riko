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
                "Nex Mesh ব্যাকগ্রাউন্ড ইঞ্জিন",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "চলমান ফাইল ট্রান্সফার, নোড সার্ভার ও রিমোট সংযোগ সচল রাখে"
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

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPi = if (launchIntent != null) {
            PendingIntent.getActivity(
                context,
                1,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            null
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(contentPi)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "বন্ধ করুন", stopPendingIntent)
            .build()
    }
}
