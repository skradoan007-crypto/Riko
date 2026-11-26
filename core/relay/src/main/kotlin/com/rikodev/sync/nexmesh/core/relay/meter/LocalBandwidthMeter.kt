package com.rikodev.sync.nexmesh.core.relay.meter

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

data class BandwidthSnapshot(
    val lanBytesTransferred: Long = 0L,
    val relayBytesSent: Long = 0L,
    val relayBytesReceived: Long = 0L,
    val relayThroughputMbps: Double = 0.0
) {
    val totalRelayBytes: Long get() = relayBytesSent + relayBytesReceived
    val totalRelayMb: Double get() = totalRelayBytes / (1024.0 * 1024.0)
    val lanMb: Double get() = lanBytesTransferred / (1024.0 * 1024.0)
}

/**
 * Thread-safe Local Bandwidth Meter tracking LAN vs Remote Relay traffic independently.
 */
class LocalBandwidthMeter {
    private val lanBytes = AtomicLong(0L)
    private val relaySent = AtomicLong(0L)
    private val relayRecv = AtomicLong(0L)
    private val windowStartNs = AtomicLong(System.nanoTime())
    private val windowRelayBytes = AtomicLong(0L)

    private val _snapshot = MutableStateFlow(BandwidthSnapshot())
    val snapshot: StateFlow<BandwidthSnapshot> = _snapshot.asStateFlow()

    fun recordLanBytes(bytes: Long) {
        if (bytes <= 0L) return
        lanBytes.addAndGet(bytes)
        publish()
    }

    fun recordRelaySent(bytes: Long) {
        if (bytes <= 0L) return
        relaySent.addAndGet(bytes)
        windowRelayBytes.addAndGet(bytes)
        publish()
    }

    fun recordRelayReceived(bytes: Long) {
        if (bytes <= 0L) return
        relayRecv.addAndGet(bytes)
        windowRelayBytes.addAndGet(bytes)
        publish()
    }

    private fun publish() {
        val elapsedSec = ((System.nanoTime() - windowStartNs.get()).coerceAtLeast(1L)) / 1e9
        val mbps = (windowRelayBytes.get() / (1024.0 * 1024.0)) / elapsedSec
        _snapshot.value = BandwidthSnapshot(
            lanBytesTransferred = lanBytes.get(),
            relayBytesSent = relaySent.get(),
            relayBytesReceived = relayRecv.get(),
            relayThroughputMbps = mbps
        )
    }
}
