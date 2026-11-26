package com.rikodev.sync.nexmesh.core.network.discovery

import android.content.Context
import android.net.ConnectivityManager
import com.rikodev.sync.nexmesh.core.network.client.SwarmControllerClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.NetworkInterface

data class LocalNetworkInfo(
    val localIpv4: String?,
    val gatewayIpv4: String?
)

/**
 * Tier 2 Hotspot Gateway Discovery.
 * Extracts active Wi-Fi/Hotspot local IP and default gateway via ConnectivityManager LinkProperties
 * and performs a safe, rate-limited health probe on the gateway & immediate hotspot hosts.
 */
class HotspotGatewayScanner(
    private val context: Context,
    private val client: SwarmControllerClient
) {
    fun inspectLocalNetwork(): LocalNetworkInfo {
        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNet = cm.activeNetwork
        val props = if (activeNet != null) cm.getLinkProperties(activeNet) else null

        val localIp = props?.linkAddresses
            ?.mapNotNull { it.address as? Inet4Address }
            ?.firstOrNull { !it.isLoopbackAddress }
            ?.hostAddress
            ?: findFallbackIpv4()

        val gatewayIp = props?.routes
            ?.firstOrNull { it.isDefaultRoute && it.gateway is Inet4Address }
            ?.gateway
            ?.hostAddress

        return LocalNetworkInfo(localIpv4 = localIp, gatewayIpv4 = gatewayIp)
    }

    suspend fun probeHotspotPeers(
        localNodeId: String,
        targetPort: Int = DEFAULT_SWARM_PORT
    ): List<DiscoveredPeer> = withContext(Dispatchers.IO) {
        val netInfo = inspectLocalNetwork()
        val candidates = linkedSetOf<String>()

        netInfo.gatewayIpv4?.let { candidates.add(it) }
        val local = netInfo.localIpv4
        if (local != null) {
            val parts = local.split(".")
            if (parts.size == 4) {
                val prefix = "${parts[0]}.${parts[1]}.${parts[2]}"
                // Standard Android hotspot gateway & first 12 DHCP lease slots (rate-limited)
                candidates.add("$prefix.1")
                for (hostOctet in 2..14) {
                    val ip = "$prefix.$hostOctet"
                    if (ip != local) candidates.add(ip)
                }
            }
        }

        val semaphore = Semaphore(6) // Strict concurrency bound to avoid router flood
        coroutineScope {
            candidates.map { ip ->
                async {
                    semaphore.withPermit {
                        val health = client.fetchHealth(ip, targetPort, timeoutMs = 450L)
                        if (health != null && health.nodeId != localNodeId) {
                            DiscoveredPeer(
                                nodeId = health.nodeId,
                                deviceName = health.deviceName,
                                host = ip,
                                port = health.serverPort,
                                tier = DiscoveryTier.HOTSPOT_GATEWAY
                            )
                        } else {
                            null
                        }
                    }
                }
            }.awaitAll().filterNotNull()
        }
    }

    private fun findFallbackIpv4(): String? {
        return runCatching {
            NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { !it.isLoopbackAddress }
                ?.hostAddress
        }.getOrNull()
    }

    companion object {
        const val DEFAULT_SWARM_PORT = 47780
    }
}
