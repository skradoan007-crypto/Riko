package com.rikodev.music.android.service

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.compose.material.icons.filled.Pause

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — HEADSET RECEIVER
// Pauses playback when headset is unplugged — standard behavior for any
// premium music app. Nobody wants music blasting through speakers after
// pulling out earphones.
// ─────────────────────────────────────────────────────────────────────────────

class HeadsetReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_HEADSET_PLUG -> {
                val state = intent.getIntExtra("state", -1)
                if (state == 0) {
                    // Headset unplugged → pause
                    broadcastPauseAction(context)
                }
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                // Bluetooth device disconnected → pause
                broadcastPauseAction(context)
            }
        }
    }

    private fun broadcastPauseAction(context: Context) {
        // The MediaSession handles pause via AudioFocus, but we send an explicit
        // pause intent as well to ensure immediate response
        val pauseIntent = Intent(RIKO_PAUSE_ACTION).apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(pauseIntent)
    }

    companion object {
        const val RIKO_PAUSE_ACTION = "com.rikodev.music.android.PAUSE"
    }
}
