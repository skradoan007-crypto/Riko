package com.rikodev.sync.nexmesh.core.guardian.power

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import java.io.Closeable

/**
 * Manages scoped PARTIAL_WAKE_LOCK and version-aware WifiLock (API 29+ WIFI_MODE_FULL_LOW_LATENCY).
 * Automatically releases all locks on completion or cancellation.
 */
class SwarmLockManager(context: Context) : Closeable {
    private val powerManager = context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    @Synchronized
    fun acquireForActiveTransfer(timeoutMs: Long = 30L * 60L * 1000L) {
        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NexMesh::SwarmTransferWakeLock").apply {
                setReferenceCounted(false)
            }
        }
        if (wakeLock?.isHeld == false) {
            wakeLock?.acquire(timeoutMs)
        }

        if (wifiLock == null) {
            @Suppress("DEPRECATION")
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                WifiManager.WIFI_MODE_FULL_LOW_LATENCY
            } else {
                WifiManager.WIFI_MODE_FULL_HIGH_PERF
            }
            wifiLock = wifiManager.createWifiLock(mode, "NexMesh::SwarmWifiLock").apply {
                setReferenceCounted(false)
            }
        }
        if (wifiLock?.isHeld == false) {
            wifiLock?.acquire()
        }
    }

    @Synchronized
    fun releaseAll() {
        runCatching {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        }
        runCatching {
            if (wifiLock?.isHeld == true) wifiLock?.release()
        }
    }

    override fun close() = releaseAll()
}
