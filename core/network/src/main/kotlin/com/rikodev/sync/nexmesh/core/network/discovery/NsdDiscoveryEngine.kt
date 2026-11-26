package com.rikodev.sync.nexmesh.core.network.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Tier 1 NSD Discovery Engine.
 * - API 24–33: Uses a strict sequential Mutex resolve queue to prevent FAILURE_ALREADY_ACTIVE crashes.
 * - API 34+: Uses registerServiceInfoCallback() with a dedicated executor.
 */
class NsdDiscoveryEngine(context: Context) {
    private val nsdManager = context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val resolveMutex = Mutex()
    private val isDiscovering = AtomicBoolean(false)
    private val isRegistered = AtomicBoolean(false)

    private val _discoveredPeers = MutableSharedFlow<DiscoveredPeer>(extraBufferCapacity = 32)
    val discoveredPeers: SharedFlow<DiscoveredPeer> = _discoveredPeers.asSharedFlow()

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private val api34Callbacks = mutableListOf<Any>()
    private val callbackExecutor = Executors.newSingleThreadExecutor()

    fun registerNodeService(nodeId: String, deviceName: String, port: Int) {
        if (!isRegistered.compareAndSet(false, true)) return

        val info = NsdServiceInfo().apply {
            serviceName = "NexMesh_${nodeId.takeLast(8)}"
            serviceType = SERVICE_TYPE
            setPort(port)
            setAttribute("id", nodeId)
            setAttribute("name", deviceName.take(32))
        }

        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) = Unit
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                isRegistered.set(false)
            }
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                isRegistered.set(false)
            }
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                isRegistered.set(false)
            }
        }
        registrationListener = listener
        runCatching {
            nsdManager.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
        }.onFailure {
            isRegistered.set(false)
        }
    }

    fun unregisterNodeService() {
        val listener = registrationListener ?: return
        registrationListener = null
        runCatching { nsdManager.unregisterService(listener) }
        isRegistered.set(false)
    }

    fun startDiscovery(localNodeId: String) {
        if (!isDiscovering.compareAndSet(false, true)) return

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) = Unit
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType != SERVICE_TYPE && !serviceInfo.serviceType.startsWith("_nexmesh")) return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    resolveApi34Plus(serviceInfo, localNodeId)
                } else {
                    scope.launch { resolveLegacySequential(serviceInfo, localNodeId) }
                }
            }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit
            override fun onDiscoveryStopped(serviceType: String) {
                isDiscovering.set(false)
            }
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                isDiscovering.set(false)
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                isDiscovering.set(false)
            }
        }
        discoveryListener = listener
        runCatching {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        }.onFailure {
            isDiscovering.set(false)
        }
    }

    fun stopDiscovery() {
        val listener = discoveryListener
        discoveryListener = null
        if (listener != null) {
            runCatching { nsdManager.stopServiceDiscovery(listener) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            synchronized(api34Callbacks) {
                api34Callbacks.forEach { cb ->
                    runCatching {
                        nsdManager.unregisterServiceInfoCallback(cb as NsdManager.ServiceInfoCallback)
                    }
                }
                api34Callbacks.clear()
            }
        }
        isDiscovering.set(false)
    }

    @Suppress("DEPRECATION")
    private suspend fun resolveLegacySequential(info: NsdServiceInfo, localNodeId: String) {
        resolveMutex.withLock {
            val resolved = suspendCancellableCoroutine<NsdServiceInfo?> { cont ->
                nsdManager.resolveService(info, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        if (cont.isActive) cont.resume(null)
                    }
                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        if (cont.isActive) cont.resume(serviceInfo)
                    }
                })
            } ?: return@withLock

            val host = resolved.host?.hostAddress ?: return@withLock
            val nodeId = resolved.attributes["id"]?.decodeToString() ?: resolved.serviceName
            if (nodeId == localNodeId) return@withLock
            val name = resolved.attributes["name"]?.decodeToString() ?: resolved.serviceName

            _discoveredPeers.tryEmit(
                DiscoveredPeer(
                    nodeId = nodeId,
                    deviceName = name,
                    host = host,
                    port = resolved.port,
                    tier = DiscoveryTier.NSD
                )
            )
        }
    }

    private fun resolveApi34Plus(info: NsdServiceInfo, localNodeId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        val cb = object : NsdManager.ServiceInfoCallback {
            override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) = Unit
            override fun onServiceUpdated(serviceInfo: NsdServiceInfo) {
                val host = serviceInfo.hostAddresses.firstOrNull()?.hostAddress ?: return
                val nodeId = serviceInfo.attributes["id"]?.decodeToString() ?: serviceInfo.serviceName
                if (nodeId == localNodeId) return
                val name = serviceInfo.attributes["name"]?.decodeToString() ?: serviceInfo.serviceName
                _discoveredPeers.tryEmit(
                    DiscoveredPeer(
                        nodeId = nodeId,
                        deviceName = name,
                        host = host,
                        port = serviceInfo.port,
                        tier = DiscoveryTier.NSD
                    )
                )
            }
            override fun onServiceLost() = Unit
            override fun onServiceInfoCallbackUnregistered() = Unit
        }
        synchronized(api34Callbacks) { api34Callbacks.add(cb) }
        runCatching { nsdManager.registerServiceInfoCallback(info, callbackExecutor, cb) }
    }

    companion object {
        const val SERVICE_TYPE = "_nexmesh._tcp."
    }
}
