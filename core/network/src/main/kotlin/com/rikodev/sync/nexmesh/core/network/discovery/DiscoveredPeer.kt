package com.rikodev.sync.nexmesh.core.network.discovery

enum class DiscoveryTier {
    NSD,
    UDP_BEACON,
    HOTSPOT_GATEWAY,
    QR_DIRECT
}

data class DiscoveredPeer(
    val nodeId: String,
    val deviceName: String,
    val host: String,
    val port: Int,
    val publicKeyB64Url: String? = null,
    val tier: DiscoveryTier,
    val lastSeenEpochMs: Long = System.currentTimeMillis()
)
