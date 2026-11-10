package com.rikodev.music.android.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.rikodev.music.android.MainActivity
import com.rikodev.music.android.core.util.RikoLog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — MUSIC PLAYBACK SERVICE (Media3 MediaSessionService core)
//
// This is the engine room of the entire app. It provides:
//   - P5: MediaSession — ONE session wraps PlayerManager's ExoPlayer (both
//     Hilt singletons, same instance). Android recognizes RIKO as a real
//     media player: lock-screen/System UI media controls, headset/Bluetooth
//     media buttons, Assist control surfaces all route through this session.
//     No duplicate player or session exists anywhere in the app.
//   - P3: Persistent playback — the service is a real foreground media
//     service with its own media notification (Media3's built-in
//     DefaultMediaNotificationProvider). android:stopWithTask="false" in
//     the manifest keeps the process alive when the user swipes the task
//     away; playback continues with the UI gone.
//   - P4: RikoNotificationCenter owns the channel infrastructure; this
//     service ensures channels exist before the first notification post.
//
// ONE PLAYER / ONE SESSION — the service never owns a second ExoPlayer; it
// wraps the singleton that the whole UI already drives.
// ─────────────────────────────────────────────────────────────────────────────

@AndroidEntryPoint
class MusicPlaybackService : MediaSessionService() {

    @Inject
    lateinit var playerManager: PlayerManager

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        // Channels must exist before the first post (P4 infra).
        RikoNotificationCenter.ensureChannels(this)

        // MediaSession on top of PlayerManager's ExoPlayer — the single
        // instance Android uses for notifications, lock screen, Bluetooth.
        val session = MediaSession.Builder(this, playerManager.player)
            .setSessionActivity(buildSessionActivityPendingIntent())
            .build()
        mediaSession = session

        // ROOT-CAUSE FIX (system media controls never appearing):
        // Media3's notification machinery (MediaNotificationManager →
        // DefaultMediaNotificationProvider) is attached ONLY to sessions
        // REGISTERED with the service via addSession(). Media3 itself calls
        // addSession() only when (a) a MediaController connects through
        // onGetSession(), (b) an ACTION_MEDIA_BUTTON intent arrives, or (c)
        // a legacy MediaBrowser binds. RIKO has no in-app MediaController —
        // the UI drives the shared ExoPlayer singleton directly — so none of
        // those paths ever ran: the session existed but was never registered,
        // nothing posted the MediaStyle notification (no Android 13+ media
        // card / lock-screen controls) and the service was never promoted to
        // a foreground service.
        //
        // addSession() is Media3's documented public API for this exact
        // app-managed single-session architecture. It wires the initial
        // notification evaluation (works even if playback already started),
        // ongoing updates on state/playWhenReady/timeline/metadata changes,
        // and the startForeground() promotion while playback is ongoing.
        RikoLog.i(TAG, "onCreate: MediaSession created and registered with service")
        addSession(session)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        RikoLog.d(TAG, "onGetSession: controller connecting (${controllerInfo.packageName})")
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // User-facing kill switch (P3): Settings "Exit" row and the
        // notification's stop action both route here.
        if (intent?.action == ACTION_STOP_PLAYBACK) {
            playerManager.stopForUser()
            stopSelf()
            return START_NOT_STICKY
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // P3: swipe-dismiss from Recents must NOT kill playback. Only when
        // nothing is loaded do we let the service settle down.
        val player = mediaSession?.player ?: return
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
        // Else: keep running — a foreground media service survives task
        // removal; the user explicitly stops it from the app (Exit) or by
        // dismissing the media notification.
    }

    override fun onDestroy() {
        RikoLog.i(TAG, "onDestroy: releasing MediaSession")
        mediaSession?.run {
            release()
            mediaSession = null
        }
        // NOTE: deliberately NOT calling playerManager.release() here.
        // PlayerManager is a Hilt @Singleton shared with the ViewModel —
        // it lives for the whole app process, not just this Service's
        // lifecycle. The Service can be destroyed/recreated independently
        // (task removal, memory pressure, crash) while the Activity is
        // still alive; releasing the shared ExoPlayer here would pull it
        // out from under the UI, which is exactly what produced the
        // "reopen and everything's stuck" symptom.
        super.onDestroy()
    }

    private fun buildSessionActivityPendingIntent(): android.app.PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return android.app.PendingIntent.getActivity(
            this, 0, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val TAG = "RIKO_MEDIA_SESSION"

        /** Settings "Exit" / notification stop → full service + player shutdown. */
        const val ACTION_STOP_PLAYBACK =
            "com.rikodev.music.android.service.action.STOP_PLAYBACK"

        /**
         * Start the playback service from the UI. Safe to call repeatedly;
         * Android keeps one instance. Called on first play so the process
         * is promoted to a real foreground service with the media
         * notification before the user can background the app.
         */
        fun ensureStarted(context: Context) {
            val intent = Intent(context, MusicPlaybackService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        /**
         * User-requested full stop (Settings Exit row, notification stop).
         * Routes through the service so the foreground state and the
         * player chain wind down together.
         */
        fun requestStop(context: Context) {
            val intent = Intent(context, MusicPlaybackService::class.java)
                .setAction(ACTION_STOP_PLAYBACK)
            // startService is fine from the foreground; from the
            // background this is still permitted because we are a media
            // app with an active MediaSession (exemption for media buttons).
            context.startService(intent)
        }
    }
}
