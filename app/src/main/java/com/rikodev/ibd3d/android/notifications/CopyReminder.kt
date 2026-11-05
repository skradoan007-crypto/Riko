package com.rikodev.ibd3d.android.notifications

import android.app.*
import android.content.*
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.rikodev.ibd3d.android.MainActivity
import com.rikodev.ibd3d.android.R

/** Only remembers copies made inside this app; never monitors the global clipboard. */
object CopyReminder {
    private const val ID = 4001
    private const val CHANNEL = "copied_code_reminders"
    private val handler = Handler(Looper.getMainLooper())
    private var pending: Triple<String, String, Long>? = null
    var enabled = true
    fun copied(name: String, code: String) { pending = if (enabled) Triple(name, code, SystemClock.elapsedRealtime()) else null }
    fun foreground(context: Context) {
        handler.removeCallbacksAndMessages(null)
        context.getSystemService(NotificationManager::class.java).cancel(ID)
    }
    fun background(context: Context) {
        val item = pending ?: return
        pending = null
        if (!enabled || SystemClock.elapsedRealtime() - item.third > 10_000) return
        if (context.getSystemService(KeyguardManager::class.java).isKeyguardLocked) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Copied code reminders", NotificationManager.IMPORTANCE_HIGH).apply { description = "Your last copied code when leaving IBD3D Panel" })
        val intent = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        fun post(timeout: Long, alert: Boolean) {
            val notification = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_copy_notification).setContentTitle("${item.first} • ${item.second}")
                .setContentText("You Just Copied. Use It")
                .setStyle(NotificationCompat.BigTextStyle().bigText("${item.first}\n${item.second}\nYou Just Copied. Use It"))
                .setContentIntent(intent).setAutoCancel(true).setTimeoutAfter(timeout)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setOnlyAlertOnce(!alert).setSilent(!alert).build()
            try { manager.notify(ID, notification) } catch (_: SecurityException) {}
        }
        fun visible() = manager.activeNotifications.any { it.id == ID }
        post(7000, true)
        handler.postDelayed({ if (visible()) post(5000, true) }, 2000)
        handler.postDelayed({ if (visible()) post(3000, false) }, 4000)
        handler.postDelayed({ manager.cancel(ID) }, 7000)
    }
}
