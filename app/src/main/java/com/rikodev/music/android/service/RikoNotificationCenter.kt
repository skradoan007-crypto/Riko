package com.rikodev.music.android.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — NOTIFICATION CENTER (P4 infrastructure)
//
// Owns notification CHANNEL infrastructure and permission state. No concrete
// notification content lives here yet — the media playback notification is
// Media3's DefaultMediaNotificationProvider (driven by the MediaSession);
// this object exists so any future RIKO notification posts through one
// consistent, channel-safe entry point.
//
// Contract:
//   - ensureChannels() is idempotent; called before the first notification
//     post (MusicPlaybackService.onCreate) and safe to call anywhere.
//   - canPostNotifications() reports whether the app may actually post on
//     this device (Android 13+ runtime permission).
// ─────────────────────────────────────────────────────────────────────────────

object RikoNotificationCenter {

    /** Media playback notifications — silent (IMPORTANCE_LOW), no badge. */
    const val CHANNEL_PLAYBACK = "riko_playback"

    /** General purpose notifications (future use). */
    const val CHANNEL_GENERAL = "riko_general"

    /** Equalizer status — silent (IMPORTANCE_LOW), no sound. Flat EQ parity. */
    const val CHANNEL_EQ = "riko_equalizer"

    private const val EQ_NOTIFICATION_ID = 0xE0

    /** Create both channels if absent. No-op below Android O. */
    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val playback = NotificationChannel(
            CHANNEL_PLAYBACK,
            "Playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Now playing and media controls"
            setShowBadge(false)
        }

        val general = NotificationChannel(
            CHANNEL_GENERAL,
            "General",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "General RIKO notifications"
        }

        val eq = NotificationChannel(
            CHANNEL_EQ,
            "Equalizer",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Equalizer status"
            setShowBadge(false)
            setSound(null, null)
        }

        manager.createNotificationChannel(playback)
        manager.createNotificationChannel(general)
        manager.createNotificationChannel(eq)
    }

    /** True when the app is allowed to post notifications on this device. */
    fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Flat Equalizer parity: "Equalizer on from Riko Music" status notice.
     * Silent degrade when the notification permission is missing.
     */
    fun postEqActive(context: Context, profileName: String?) {
        if (!canPostNotifications(context)) return
        ensureChannels(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent()
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_EQ)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Equalizer on")
            .setContentText(
                if (profileName.isNullOrBlank()) "Equalizer on from Riko Music"
                else "Equalizer on from Riko Music · $profileName"
            )
            .setContentIntent(contentIntent)
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        try {
            manager.notify(EQ_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission raced away — silent degrade.
        }
    }

    fun cancelEqActive(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(EQ_NOTIFICATION_ID)
    }
}
