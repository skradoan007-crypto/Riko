package com.rikodev.sync.nexmesh.core.network.discovery

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress

@Serializable
private data class UdpBeaconFrame(
    val magic: String = "NEXMESH_V1",
    val nodeId: String,
    val deviceName: String,
    val tcpPort: Int
)

/**
 * Tier 1 Fallback: UDP Broadcast Beacon on default port 47777.
 */
class UdpBeaconEngine(private val udpPort: Int = DEFAULT_UDP_PORT) {
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _discoveredPeers = MutableSharedFlow<DiscoveredPeer>(extraBufferCapacity = 32)
    val discoveredPeers: SharedFlow<DiscoveredPeer> = _discoveredPeers.asSharedFlow()

    private var broadcastJob: Job? = null
    private var listenJob: Job? = null

    fun startBroadcasting(nodeId: String, deviceName: String, tcpPort: Int) {
        broadcastJob?.cancel()
        broadcastJob = scope.launch {
            val payload = json.encodeToString(
                UdpBeaconFrame(nodeId = nodeId, deviceName = deviceName, tcpPort = tcpPort)
            ).toByteArray(Charsets.UTF_8)

            DatagramSocket().use { socket ->
                socket.broadcast = true
                val broadcastAddr = InetAddress.getByName("255.255.255.255")
                while (isActive) {
                    runCatching {
                        val packet = DatagramPacket(payload, payload.size, broadcastAddr, udpPort)
                        socket.send(packet)
                    }
                    delay(3_000L)
                }
            }
        }
    }

    fun stopBroadcasting() {
        broadcastJob?.cancel()
        broadcastJob = null
    }

    fun startListening(localNodeId: String) {
        listenJob?.cancel()
        listenJob = scope.launch {
            runCatching {
                DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(udpPort))
                    soTimeout = 2_000
                }.use { socket ->
                    val buffer = ByteArray(1024)
                    while (isActive) {
                        val packet = DatagramPacket(buffer, buffer.size)
                        runCatching {
                            socket.receive(packet)
                            val raw = String(packet.data, 0, packet.length, Charsets.UTF_8)
                            val frame = json.decodeFromString<UdpBeaconFrame>(raw)
                            if (frame.magic == "NEXMESH_V1" && frame.nodeId != localNodeId) {
                                val host = packet.address?.hostAddress ?: return@runCatching
                                _discoveredPeers.tryEmit(
                                    DiscoveredPeer(
                                        nodeId = frame.nodeId,
                                        deviceName = frame.deviceName,
                                        host = host,
                                        port = frame.tcpPort,
                                        tier = DiscoveryTier.UDP_BEACON
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun stopListening() {
        listenJob?.cancel()
        listenJob = null
    }

    companion object {
        const val DEFAULT_UDP_PORT = 47777
    }
}
