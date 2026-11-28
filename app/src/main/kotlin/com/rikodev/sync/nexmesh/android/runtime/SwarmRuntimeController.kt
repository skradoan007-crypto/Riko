package com.rikodev.sync.nexmesh.android.runtime

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.rikodev.sync.nexmesh.android.di.AppContainer
import com.rikodev.sync.nexmesh.android.provider.NexMeshDocumentsProvider
import com.rikodev.sync.nexmesh.core.guardian.service.SwarmForegroundService
import com.rikodev.sync.nexmesh.core.nativeengine.NativeBenchmarkResult
import com.rikodev.sync.nexmesh.core.network.discovery.HotspotGatewayScanner
import com.rikodev.sync.nexmesh.core.protocol.crypto.Base64UrlCompat
import com.rikodev.sync.nexmesh.core.protocol.crypto.EcdhKeyExchange
import com.rikodev.sync.nexmesh.core.protocol.model.PairConfirmRequest
import com.rikodev.sync.nexmesh.core.protocol.model.PairInitRequest
import com.rikodev.sync.nexmesh.core.protocol.model.RemoteVaultFileItem
import com.rikodev.sync.nexmesh.core.protocol.model.RemoteVaultListResponse
import com.rikodev.sync.nexmesh.core.protocol.model.TrustedPeerRecord
import com.rikodev.sync.nexmesh.core.protocol.model.TrustedReconnectRequest
import com.rikodev.sync.nexmesh.core.protocol.session.ActiveSwarmSession
import com.rikodev.sync.nexmesh.core.protocol.session.PendingPairingChallenge
import com.rikodev.sync.nexmesh.feature.ui.components.RikoSwitchState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ActiveBackgroundTask(val label: String) {
    STORAGE_PROVIDER("Storage Sharing"),
    LIVE_STREAMING("Live File Streaming")
}

class SwarmRuntimeController(private val container: AppContainer) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val prefs = container.appContext.getSharedPreferences("nexmesh_prefs_v2", Context.MODE_PRIVATE)
    private val activeTasksLock = Any()
    private val activeTasks = linkedSetOf<ActiveBackgroundTask>()

    private val _hasActiveBackgroundWork = MutableStateFlow(false)
    val hasActiveBackgroundWork: StateFlow<Boolean> = _hasActiveBackgroundWork.asStateFlow()

    val roleIndex = MutableStateFlow(prefs.getInt(KEY_ROLE_INDEX, 0))
    val isDarkTheme = MutableStateFlow(prefs.getBoolean(KEY_DARK_THEME, false))
    val storageProviderSwitchState = MutableStateFlow(RikoSwitchState.OFF)

    val activeSession = MutableStateFlow<ActiveSwarmSession?>(null)
    val activePeerHost = MutableStateFlow<String?>(null)
    val activePeerPort = MutableStateFlow(HotspotGatewayScanner.DEFAULT_SWARM_PORT)
    val remoteAvailableBytes = MutableStateFlow(0L)

    // Live Remote Storage Browser State
    val remoteVault = MutableStateFlow<RemoteVaultListResponse?>(null)
    val remoteSubfolderStack = MutableStateFlow<List<String>>(emptyList())
    val isLoadingRemoteVault = MutableStateFlow(false)

    val outgoingChallenge = MutableStateFlow<PendingPairingChallenge?>(null)
    val outgoingTargetHost = MutableStateFlow<String?>(null)
    val outgoingTargetPort = MutableStateFlow(HotspotGatewayScanner.DEFAULT_SWARM_PORT)

    val pairingStatusMessage = MutableStateFlow<String?>(null)
    val storageStatusMessage = MutableStateFlow<String?>(null)
    val recentActivity = MutableStateFlow<List<String>>(emptyList())

    val benchmarkResult = MutableStateFlow<NativeBenchmarkResult?>(null)
    val isBenchmarkRunning = MutableStateFlow(false)

    fun setRole(index: Int) {
        val clamped = index.coerceIn(0, 1)
        roleIndex.value = clamped
        prefs.edit().putInt(KEY_ROLE_INDEX, clamped).apply()
        if (clamped == 1 && storageProviderSwitchState.value == RikoSwitchState.OFF) {
            toggleStorageProvider(true)
        }
    }

    fun setDarkTheme(dark: Boolean) {
        isDarkTheme.value = dark
        prefs.edit().putBoolean(KEY_DARK_THEME, dark).apply()
    }

    fun recordActivity(message: String) {
        val stamp = SimpleDateFormat("HH:mm", Locale.US).format(Date())
        val entry = "$stamp • $message"
        recentActivity.value = (listOf(entry) + recentActivity.value).take(8)
    }

    private fun acquireBackgroundTask(task: ActiveBackgroundTask) {
        val summary: String
        synchronized(activeTasksLock) {
            activeTasks.add(task)
            _hasActiveBackgroundWork.value = true
            summary = activeTasks.joinToString(" • ") { it.label }
        }
        SwarmForegroundService.start(
            context = container.appContext,
            title = "NexMesh Storage Provider Active",
            body = "Running: $summary",
            needsWakeLock = true
        )
    }

    private fun releaseBackgroundTask(task: ActiveBackgroundTask) {
        val remainingSummary: String?
        synchronized(activeTasksLock) {
            activeTasks.remove(task)
            if (activeTasks.isEmpty()) {
                _hasActiveBackgroundWork.value = false
                remainingSummary = null
            } else {
                remainingSummary = activeTasks.joinToString(" • ") { it.label }
            }
        }
        if (remainingSummary == null) {
            SwarmForegroundService.stop(container.appContext)
        } else {
            SwarmForegroundService.start(
                context = container.appContext,
                title = "NexMesh Storage Provider Active",
                body = "Running: $remainingSummary",
                needsWakeLock = true
            )
        }
    }

    fun onServiceStoppedExternally() {
        synchronized(activeTasksLock) {
            activeTasks.clear()
            _hasActiveBackgroundWork.value = false
        }
        storageProviderSwitchState.value = RikoSwitchState.OFF
        recordActivity("Background storage sharing stopped")
    }

    fun toggleStorageProvider(enable: Boolean) {
        appScope.launch {
            storageProviderSwitchState.value = RikoSwitchState.ENGAGING
            delay(180)
            runCatching {
                if (enable) {
                    container.nodeServer.start(HotspotGatewayScanner.DEFAULT_SWARM_PORT)
                    container.discoveryOrchestrator.startAdvertising(
                        nodeId = container.localIdentity.nodeId,
                        deviceName = container.localIdentity.deviceName,
                        tcpPort = HotspotGatewayScanner.DEFAULT_SWARM_PORT
                    )
                    acquireBackgroundTask(ActiveBackgroundTask.STORAGE_PROVIDER)
                    storageProviderSwitchState.value = RikoSwitchState.ON
                    pairingStatusMessage.value = "Storage sharing is active on this phone."
                    recordActivity("Started sharing this phone as Storage Phone")
                } else {
                    container.discoveryOrchestrator.stopAdvertising()
                    container.nodeServer.stop()
                    releaseBackgroundTask(ActiveBackgroundTask.STORAGE_PROVIDER)
                    storageProviderSwitchState.value = RikoSwitchState.OFF
                    pairingStatusMessage.value = "Storage sharing stopped."
                    recordActivity("Stopped sharing this phone as Storage Phone")
                }
            }.onFailure { err ->
                storageProviderSwitchState.value = RikoSwitchState.OFF
                releaseBackgroundTask(ActiveBackgroundTask.STORAGE_PROVIDER)
                pairingStatusMessage.value = "Could not start storage sharing: ${err.message}"
            }
        }
    }

    /**
     * Adds a user-selected SAF folder via ACTION_OPEN_DOCUMENT_TREE without copying files.
     */
    fun addSharedFolder(treeUri: Uri) {
        appScope.launch {
            container.safVaultMountManager.addSharedFolder(treeUri)
                .onSuccess { entry ->
                    storageStatusMessage.value = "Shared folder added: ${entry.displayName}"
                    recordActivity("Shared folder: ${entry.displayName}")
                }
                .onFailure { err ->
                    storageStatusMessage.value = "Could not add folder: ${err.message}"
                }
        }
    }

    /**
     * Adds one or more user-selected SAF files via ACTION_OPEN_DOCUMENT without copying them.
     */
    fun addSharedFiles(fileUris: List<Uri>) {
        if (fileUris.isEmpty()) return
        appScope.launch {
            val added = container.safVaultMountManager.addSharedFiles(fileUris)
            if (added.isNotEmpty()) {
                val label = if (added.size == 1) added.first().displayName else "${added.size} files"
                storageStatusMessage.value = "Shared $label for live streaming."
                recordActivity("Shared $label")
            } else {
                storageStatusMessage.value = "Selected files are already in Shared Files."
            }
        }
    }

    /**
     * Removes a folder or file from NexMesh sharing (original files are never deleted).
     */
    fun removeSharedEntry(entryId: String) {
        appScope.launch {
            container.safVaultMountManager.removeSharedEntry(entryId)
            storageStatusMessage.value = "Removed from NexMesh sharing. Original files remain untouched."
            recordActivity("Removed a shared storage item")
        }
    }

    /**
     * One-time QR / LAN pairing handshake with ECDH P-256 and 6-digit confirmation code.
     */
    fun initiateHandshake(
        host: String,
        port: Int,
        expectedNodeId: String? = null,
        onChallengeReady: () -> Unit = {}
    ) {
        appScope.launch {
            pairingStatusMessage.value = "Connecting securely to $host:$port..."
            runCatching {
                val eph = EcdhKeyExchange.generateEphemeralKeyPair()
                val nonce = ByteArray(16).also { SecureRandom().nextBytes(it) }
                val req = PairInitRequest(
                    controllerNodeId = container.localIdentity.nodeId,
                    controllerDeviceName = container.localIdentity.deviceName,
                    controllerStaticPubKeyB64 = container.localIdentity.publicKeyB64Url,
                    controllerEphemeralPubKeyB64 = EcdhKeyExchange.encodePublicKeyB64Url(eph.public),
                    nonceB64 = Base64UrlCompat.encode(nonce),
                    timestampEpochMs = System.currentTimeMillis()
                )
                val resp = container.controllerClient.initPairing(host, port, req)
                if (expectedNodeId != null && resp.workerNodeId != expectedNodeId) {
                    error("Device identity mismatch — pairing cancelled for security.")
                }

                val material = EcdhKeyExchange.deriveSessionMaterial(
                    localEphemeralPrivate = eph.private,
                    peerEphemeralPublic = EcdhKeyExchange.decodePublicKeyB64Url(resp.workerEphemeralPubKeyB64),
                    controllerStaticPubBytes = container.localIdentity.publicKey.encoded,
                    workerStaticPubBytes = Base64UrlCompat.decode(resp.workerStaticPubKeyB64),
                    controllerNonce = nonce,
                    workerNonce = Base64UrlCompat.decode(resp.workerNonceB64)
                )
                outgoingTargetHost.value = host
                outgoingTargetPort.value = port
                outgoingChallenge.value = PendingPairingChallenge(
                    pairingToken = resp.pairingToken,
                    peerNodeId = resp.workerNodeId,
                    peerDeviceName = resp.workerDeviceName,
                    peerStaticPubKeyB64 = resp.workerStaticPubKeyB64,
                    sasCode = material.sasCode6Digits,
                    derivedMaterial = material,
                    expiresAtEpochMs = resp.expiresAtEpochMs
                )
                pairingStatusMessage.value = "Match the 6-digit code on both phones and tap Approve."
                onChallengeReady()
            }.onFailure { err ->
                pairingStatusMessage.value = "Could not reach phone at $host:$port (${err.message}). Make sure Storage Sharing is ON."
            }
        }
    }

    fun approveSas(token: String) {
        val incoming = container.pendingIncomingChallenge.value
        val outgoing = outgoingChallenge.value
        if (incoming?.pairingToken == token) {
            container.sessionManager.setLocalDecision(token, approved = true)
            container.clearPendingChallenge()
            pairingStatusMessage.value = "Approved on this phone — waiting for the other phone to confirm."
            recordActivity("Approved pairing request from ${incoming.peerDeviceName}")
        } else if (outgoing?.pairingToken == token) {
            val host = outgoingTargetHost.value ?: return
            val port = outgoingTargetPort.value
            appScope.launch {
                runCatching {
                    val resp = container.controllerClient.confirmPairing(
                        host,
                        port,
                        PairConfirmRequest(token, container.localIdentity.nodeId, true)
                    )
                    if (resp.status == "APPROVED" && resp.sessionId != null) {
                        val session = container.sessionManager.promoteToActiveSession(
                            peerNodeId = outgoing.peerNodeId,
                            peerDeviceName = outgoing.peerDeviceName,
                            material = outgoing.derivedMaterial,
                            sessionId = resp.sessionId!!
                        )
                        container.trustedPeerStore.saveTrustedPeer(
                            TrustedPeerRecord(
                                nodeId = outgoing.peerNodeId,
                                deviceName = outgoing.peerDeviceName,
                                publicKeyB64Url = outgoing.peerStaticPubKeyB64,
                                lastKnownHost = host,
                                lastKnownPort = port,
                                pairedAtEpochMs = System.currentTimeMillis()
                            )
                        )
                        activateConnectedStorageSession(session, host, port)
                        outgoingChallenge.value = null
                        pairingStatusMessage.value = "Paired and connected to ${outgoing.peerDeviceName}."
                        recordActivity("Paired with ${outgoing.peerDeviceName}")
                    } else {
                        pairingStatusMessage.value = "Tap Approve on the other phone as well (${resp.status})."
                    }
                }.onFailure { err ->
                    pairingStatusMessage.value = "Confirmation failed: ${err.message}"
                }
            }
        }
    }

    fun rejectSas(token: String) {
        container.sessionManager.setLocalDecision(token, approved = false)
        container.clearPendingChallenge()
        outgoingChallenge.value = null
        pairingStatusMessage.value = "Pairing request declined."
    }

    /**
     * Reconnects to a previously paired Storage Phone without scanning a QR code again.
     */
    fun reconnectTrustedPeer(record: TrustedPeerRecord) {
        appScope.launch {
            val discovered = container.discoveryOrchestrator.peers.value.firstOrNull { it.nodeId == record.nodeId }
            val host = discovered?.host ?: record.lastKnownHost
            val port = discovered?.port ?: record.lastKnownPort
            pairingStatusMessage.value = "Reconnecting to ${record.deviceName}..."
            runCatching {
                val eph = EcdhKeyExchange.generateEphemeralKeyPair()
                val nonce = ByteArray(16).also { SecureRandom().nextBytes(it) }
                val req = TrustedReconnectRequest(
                    nodeId = container.localIdentity.nodeId,
                    deviceName = container.localIdentity.deviceName,
                    staticPubKeyB64 = container.localIdentity.publicKeyB64Url,
                    ephemeralPubKeyB64 = EcdhKeyExchange.encodePublicKeyB64Url(eph.public),
                    nonceB64 = Base64UrlCompat.encode(nonce)
                )
                val resp = container.controllerClient.reconnectTrusted(host, port, req)
                if (!resp.accepted || resp.sessionId.isNullOrBlank()) {
                    error(resp.reason ?: "Reconnect rejected — try pairing again.")
                }
                val material = EcdhKeyExchange.deriveSessionMaterial(
                    localEphemeralPrivate = eph.private,
                    peerEphemeralPublic = EcdhKeyExchange.decodePublicKeyB64Url(resp.workerEphemeralPubKeyB64),
                    controllerStaticPubBytes = container.localIdentity.publicKey.encoded,
                    workerStaticPubBytes = Base64UrlCompat.decode(resp.workerStaticPubKeyB64),
                    controllerNonce = nonce,
                    workerNonce = Base64UrlCompat.decode(resp.workerNonceB64)
                )
                val session = container.sessionManager.promoteToActiveSession(
                    peerNodeId = resp.workerNodeId,
                    peerDeviceName = resp.workerDeviceName,
                    material = material,
                    sessionId = resp.sessionId!!
                )
                container.trustedPeerStore.saveTrustedPeer(
                    record.copy(lastKnownHost = host, lastKnownPort = port)
                )
                activateConnectedStorageSession(session, host, port)
                pairingStatusMessage.value = "Reconnected to ${record.deviceName}."
                recordActivity("Reconnected to ${record.deviceName}")
            }.onFailure { err ->
                pairingStatusMessage.value = "${record.deviceName} is offline or unreachable (${err.message})."
            }
        }
    }

    fun removeTrustedPeer(nodeId: String) {
        val peerName = container.trustedPeerStore.getPeer(nodeId)?.deviceName ?: "device"
        container.trustedPeerStore.revokePeer(nodeId)
        if (activeSession.value?.peerNodeId == nodeId) {
            activeSession.value = null
            activePeerHost.value = null
            remoteAvailableBytes.value = 0L
            remoteVault.value = null
            remoteSubfolderStack.value = emptyList()
            notifyDocumentsProviderRootsChanged()
        }
        pairingStatusMessage.value = "Removed $peerName from paired devices."
        recordActivity("Removed paired phone: $peerName")
    }

    private suspend fun activateConnectedStorageSession(
        session: ActiveSwarmSession,
        host: String,
        port: Int
    ) {
        activeSession.value = session
        activePeerHost.value = host
        activePeerPort.value = port
        remoteSubfolderStack.value = emptyList()
        container.controllerClient.fetchHealth(host, port)?.let { health ->
            remoteAvailableBytes.value = health.freeStorageBytes
        }
        notifyDocumentsProviderRootsChanged()
        refreshRemoteStorage(parentId = null)
    }

    /**
     * Fetches the live folder/file tree from the connected Storage Phone.
     */
    fun refreshRemoteStorage(parentId: String? = remoteSubfolderStack.value.lastOrNull()) {
        val sess = activeSession.value ?: return
        val host = activePeerHost.value ?: return
        val port = activePeerPort.value

        appScope.launch {
            isLoadingRemoteVault.value = true
            runCatching {
                container.controllerClient.listRemoteVaultFiles(host, port, sess.sessionId, parentId)
            }.onSuccess { resp ->
                remoteVault.value = resp
                storageStatusMessage.value = if (!resp.mounted) {
                    "No folders or files shared yet on ${sess.peerDeviceName}."
                } else {
                    "Live view: ${resp.vaultName} (${resp.files.size} items)"
                }
            }.onFailure { err ->
                storageStatusMessage.value = "Storage Phone is offline or unreachable: ${err.message}"
            }
            isLoadingRemoteVault.value = false
        }
    }

    fun openRemoteSubfolder(item: RemoteVaultFileItem) {
        if (!item.isDirectory) return
        remoteSubfolderStack.value = remoteSubfolderStack.value + item.fileId
        refreshRemoteStorage(parentId = item.fileId)
    }

    fun navigateRemoteBack(): Boolean {
        val current = remoteSubfolderStack.value
        if (current.isEmpty()) return false
        val updated = current.dropLast(1)
        remoteSubfolderStack.value = updated
        refreshRemoteStorage(parentId = updated.lastOrNull())
        return true
    }

    fun notifyDocumentsProviderRootsChanged() {
        runCatching {
            val rootsUri = DocumentsContract.buildRootsUri(NexMeshDocumentsProvider.AUTHORITY)
            container.appContext.contentResolver.notifyChange(rootsUri, null)
        }
    }

    fun runNativeBenchmark() {
        appScope.launch {
            isBenchmarkRunning.value = true
            benchmarkResult.value = container.nativeBenchmarkRunner.runStreamingBenchmark()
            isBenchmarkRunning.value = false
            recordActivity("Completed native streaming engine benchmark")
        }
    }

    companion object {
        private const val KEY_ROLE_INDEX = "role_index"
        private const val KEY_DARK_THEME = "dark_theme"
    }
}
