package com.rikodev.sync.nexmesh.core.network.discovery

import com.rikodev.sync.nexmesh.core.protocol.model.QrPairingUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

/**
 * Unified 3-Tier Discovery Orchestrator merging NSD, UDP 47777 Beacon,
 * Hotspot Gateway Probe, and Tier 3 QR Direct Pairing into a deduplicated peer list.
 */
class DiscoveryOrchestrator(
    private val nsdEngine: NsdDiscoveryEngine,
    private val udpEngine: UdpBeaconEngine,
    private val hotspotScanner: HotspotGatewayScanner
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _peers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    val peers: StateFlow<List<DiscoveredPeer>> = _peers.asStateFlow()

    private var collectJob: Job? = null

    fun startAllDiscovery(localNodeId: String, targetPort: Int = HotspotGatewayScanner.DEFAULT_SWARM_PORT) {
        stopAllDiscovery()
        nsdEngine.startDiscovery(localNodeId)
        udpEngine.startListening(localNodeId)

        collectJob = scope.launch {
            launch {
                val hotspotPeers = hotspotScanner.probeHotspotPeers(localNodeId, targetPort)
                hotspotPeers.forEach { upsertPeer(it) }
            }
            merge(nsdEngine.discoveredPeers, udpEngine.discoveredPeers).collect { peer ->
                upsertPeer(peer)
            }
        }
    }

    fun stopAllDiscovery() {
        collectJob?.cancel()
        collectJob = null
        nsdEngine.stopDiscovery()
        udpEngine.stopListening()
    }

    fun startAdvertising(nodeId: String, deviceName: String, tcpPort: Int) {
        nsdEngine.registerNodeService(nodeId, deviceName, tcpPort)
        udpEngine.startBroadcasting(nodeId, deviceName, tcpPort)
    }

    fun stopAdvertising() {
        nsdEngine.unregisterNodeService()
        udpEngine.stopBroadcasting()
    }

    fun registerFromQrUri(uri: QrPairingUri): DiscoveredPeer {
        val peer = DiscoveredPeer(
            nodeId = uri.nodeId,
            deviceName = "Peer ${uri.nodeId.takeLast(6)}",
            host = uri.host,
            port = uri.port,
            publicKeyB64Url = uri.publicKeyB64Url,
            tier = DiscoveryTier.QR_DIRECT
        )
        upsertPeer(peer)
        return peer
    }

    fun localIpv4Address(): String? = hotspotScanner.inspectLocalNetwork().localIpv4

    private fun upsertPeer(peer: DiscoveredPeer) {
        val current = _peers.value.toMutableList()
        val idx = current.indexOfFirst { it.nodeId == peer.nodeId }
        if (idx >= 0) {
            current[idx] = peer.copy(
                publicKeyB64Url = peer.publicKeyB64Url ?: current[idx].publicKeyB64Url
            )
        } else {
            current.add(peer)
        }
        _peers.value = current
    }
}
