package com.rikodev.sync.nexmesh.android.runtime

import android.net.Uri
import com.rikodev.sync.nexmesh.android.di.AppContainer
import com.rikodev.sync.nexmesh.core.compute.engine.ComputeWorkerNode
import com.rikodev.sync.nexmesh.core.guardian.service.SwarmForegroundService
import com.rikodev.sync.nexmesh.core.nativeengine.NativeBenchmarkResult
import com.rikodev.sync.nexmesh.core.nativeengine.NativeEngineBridge
import com.rikodev.sync.nexmesh.core.network.discovery.HotspotGatewayScanner
import com.rikodev.sync.nexmesh.core.protocol.crypto.Base64UrlCompat
import com.rikodev.sync.nexmesh.core.protocol.crypto.EcdhKeyExchange
import com.rikodev.sync.nexmesh.core.protocol.model.PairConfirmRequest
import com.rikodev.sync.nexmesh.core.protocol.model.PairInitRequest
import com.rikodev.sync.nexmesh.core.protocol.session.ActiveSwarmSession
import com.rikodev.sync.nexmesh.core.protocol.session.PendingPairingChallenge
import com.rikodev.sync.nexmesh.core.vdfs.entity.SwarmNodeEntity
import com.rikodev.sync.nexmesh.core.vdfs.entity.SwarmNodeStatus
import com.rikodev.sync.nexmesh.feature.ui.components.RikoSwitchState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.security.SecureRandom
import java.util.UUID

enum class ActiveBackgroundTask(val labelBn: String) {
    NODE_SERVER("নোড শেয়ারিং"),
    FILE_TRANSFER("ফাইল ট্রান্সফার"),
    VDFS_UPLOAD("ক্লাউড ড্রাইভ সিঙ্ক"),
    SPLIT_COMPUTE("ভিডিও প্রসেসিং"),
    REMOTE_RELAY("রিমোট সংযোগ")
}

/**
 * Application-scoped Runtime Controller.
 * Keeps all ongoing network transfers, Node hosting, Split-Compute, VDFS ingestion,
 * and Remote Relay alive even when the user minimizes the app or turns off the screen.
 * Automatically starts SwarmForegroundService + scoped WakeLock ONLY while active tasks exist,
 * and releases everything immediately when idle.
 */
class SwarmRuntimeController(private val container: AppContainer) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val activeTasksLock = Any()
    private val activeTasks = linkedSetOf<ActiveBackgroundTask>()

    private val _hasActiveBackgroundWork = MutableStateFlow(false)
    val hasActiveBackgroundWork: StateFlow<Boolean> = _hasActiveBackgroundWork.asStateFlow()

    val selectedModeIndex = MutableStateFlow(0) // 0=Controller, 1=Node, 2=Kiosk
    val nodeServerSwitchState = MutableStateFlow(RikoSwitchState.OFF)
    val redundancy2xSwitchState = MutableStateFlow(RikoSwitchState.OFF)
    val hwAccelSwitchState = MutableStateFlow(
        if (container.hardwareCodecCapabilities.isSupportedForHardwareCompute) RikoSwitchState.ON else RikoSwitchState.LOCKED
    )
    val lz4SwitchState = MutableStateFlow(
        if (NativeEngineBridge.isAvailable) RikoSwitchState.ON else RikoSwitchState.LOCKED
    )

    val selectedFileUri = MutableStateFlow<Uri?>(null)
    val selectedFileName = MutableStateFlow<String?>(null)
    val selectedVideoUri = MutableStateFlow<Uri?>(null)
    val selectedVideoName = MutableStateFlow<String?>(null)

    val activeSession = MutableStateFlow<ActiveSwarmSession?>(null)
    val activePeerHost = MutableStateFlow<String?>(null)
    val activePeerPort = MutableStateFlow(HotspotGatewayScanner.DEFAULT_SWARM_PORT)

    val outgoingChallenge = MutableStateFlow<PendingPairingChallenge?>(null)
    val outgoingTargetHost = MutableStateFlow<String?>(null)
    val outgoingTargetPort = MutableStateFlow(HotspotGatewayScanner.DEFAULT_SWARM_PORT)

    val pairingStatusMessage = MutableStateFlow<String?>(null)
    val vdfsStatusMessage = MutableStateFlow<String?>(null)

    val benchmarkResult = MutableStateFlow<NativeBenchmarkResult?>(null)
    val isBenchmarkRunning = MutableStateFlow(false)

    private fun acquireBackgroundTask(task: ActiveBackgroundTask) {
        val summary: String
        synchronized(activeTasksLock) {
            activeTasks.add(task)
            _hasActiveBackgroundWork.value = true
            summary = activeTasks.joinToString(" • ") { it.labelBn }
        }
        SwarmForegroundService.start(
            context = container.appContext,
            title = "Nex Mesh ব্যাকগ্রাউন্ডে সক্রিয়",
            body = "চলমান: $summary",
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
                remainingSummary = activeTasks.joinToString(" • ") { it.labelBn }
            }
        }
        if (remainingSummary == null) {
            SwarmForegroundService.stop(container.appContext)
        } else {
            SwarmForegroundService.start(
                context = container.appContext,
                title = "Nex Mesh ব্যাকগ্রাউন্ডে সক্রিয়",
                body = "চলমান: $remainingSummary",
                needsWakeLock = true
            )
        }
    }

    fun onServiceStoppedExternally() {
        synchronized(activeTasksLock) {
            activeTasks.clear()
            _hasActiveBackgroundWork.value = false
        }
        nodeServerSwitchState.value = RikoSwitchState.OFF
    }

    fun toggleNodeServer(enable: Boolean) {
        appScope.launch {
            nodeServerSwitchState.value = RikoSwitchState.ENGAGING
            delay(180)
            runCatching {
                if (enable) {
                    container.nodeServer.start(HotspotGatewayScanner.DEFAULT_SWARM_PORT)
                    container.discoveryOrchestrator.startAdvertising(
                        nodeId = container.localIdentity.nodeId,
                        deviceName = container.localIdentity.deviceName,
                        tcpPort = HotspotGatewayScanner.DEFAULT_SWARM_PORT
                    )
                    acquireBackgroundTask(ActiveBackgroundTask.NODE_SERVER)
                    nodeServerSwitchState.value = RikoSwitchState.ON
                } else {
                    container.discoveryOrchestrator.stopAdvertising()
                    container.nodeServer.stop()
                    releaseBackgroundTask(ActiveBackgroundTask.NODE_SERVER)
                    nodeServerSwitchState.value = RikoSwitchState.OFF
                }
            }.onFailure {
                nodeServerSwitchState.value = RikoSwitchState.OFF
                releaseBackgroundTask(ActiveBackgroundTask.NODE_SERVER)
                pairingStatusMessage.value = "সার্ভার চালু করতে সমস্যা: ${it.message}"
            }
        }
    }

    fun initiateHandshake(host: String, port: Int, expectedNodeId: String? = null, onChallengeReady: () -> Unit = {}) {
        appScope.launch {
            pairingStatusMessage.value = "$host:$port-এর সাথে নিরাপদ সংযোগ যাচাই চলছে..."
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
                    error("ডিভাইস আইডি মেলেনি—নিরাপত্তার কারণে বাতিল করা হয়েছে।")
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
                pairingStatusMessage.value = "দুই ফোনে ৬-সংখ্যার নিরাপত্তা কোড মিলিয়ে অনুমতি দিন।"
                onChallengeReady()
            }.onFailure {
                pairingStatusMessage.value = "সংযোগ ব্যর্থ হয়েছে: ${it.message}"
            }
        }
    }

    fun approveSas(token: String) {
        val incoming = container.pendingIncomingChallenge.value
        val outgoing = outgoingChallenge.value
        if (incoming?.pairingToken == token) {
            container.sessionManager.setLocalDecision(token, approved = true)
            container.clearPendingChallenge()
            pairingStatusMessage.value = "অনুমতি দেওয়া হয়েছে—অন্য ফোনের নিশ্চিতকরণের অপেক্ষায়।"
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
                        activeSession.value = container.sessionManager.promoteToActiveSession(
                            peerNodeId = outgoing.peerNodeId,
                            peerDeviceName = outgoing.peerDeviceName,
                            material = outgoing.derivedMaterial,
                            sessionId = resp.sessionId!!
                        )
                        activePeerHost.value = host
                        activePeerPort.value = port
                        outgoingChallenge.value = null
                        container.controllerClient.fetchHealth(host, port)?.let { h ->
                            container.vdfsOrchestrator.upsertNodeTelemetry(
                                SwarmNodeEntity(
                                    nodeId = h.nodeId,
                                    name = h.deviceName,
                                    totalBytes = h.freeStorageBytes * 2,
                                    freeBytes = h.freeStorageBytes,
                                    batteryPct = h.batteryPct,
                                    tempCelsius = h.tempCelsius,
                                    status = if (h.thermalThrottled) SwarmNodeStatus.THROTTLED else SwarmNodeStatus.ONLINE,
                                    lastSeen = System.currentTimeMillis(),
                                    host = host,
                                    port = h.serverPort,
                                    isCharging = h.isCharging
                                )
                            )
                        }
                        pairingStatusMessage.value = "${outgoing.peerDeviceName}-এর সাথে নিরাপদ সংযোগ সফল হয়েছে!"
                    } else {
                        pairingStatusMessage.value = "অন্য ফোনেও 'অনুমতি দিন' চাপুন (${resp.status})"
                    }
                }.onFailure {
                    pairingStatusMessage.value = "নিশ্চিতকরণ ব্যর্থ: ${it.message}"
                }
            }
        }
    }

    fun rejectSas(token: String) {
        container.sessionManager.setLocalDecision(token, approved = false)
        container.clearPendingChallenge()
        outgoingChallenge.value = null
        pairingStatusMessage.value = "সংযোগ অনুরোধ বাতিল করা হয়েছে।"
    }

    fun startOrResumeFileTransfer() {
        val uri = selectedFileUri.value ?: return
        val sess = activeSession.value ?: return
        val host = activePeerHost.value ?: return
        val port = activePeerPort.value

        appScope.launch {
            acquireBackgroundTask(ActiveBackgroundTask.FILE_TRANSFER)
            try {
                val res = container.transferEngine.uploadSafFile(
                    fileUri = uri,
                    host = host,
                    port = port,
                    session = sess,
                    existingTransferId = container.transferEngine.progress.value.transferId.takeIf { it.isNotBlank() },
                    useNativeLz4 = lz4SwitchState.value == RikoSwitchState.ON
                )
                res.onSuccess { container.bandwidthMeter.recordLanBytes(it.transferredBytes) }
            } finally {
                releaseBackgroundTask(ActiveBackgroundTask.FILE_TRANSFER)
            }
        }
    }

    fun ingestFileToVdfs(uri: Uri) {
        appScope.launch {
            acquireBackgroundTask(ActiveBackgroundTask.VDFS_UPLOAD)
            vdfsStatusMessage.value = "ফাইল ভাগ করে শেয়ার্ড ড্রাইভে সংরক্ষণ করা হচ্ছে..."
            try {
                val sessionsMap = activeSession.value?.let { mapOf(it.peerNodeId to it) } ?: emptyMap()
                val replication = if (redundancy2xSwitchState.value == RikoSwitchState.ON) 2 else 1
                container.vdfsOrchestrator.ingestFileToVdfs(
                    fileUri = uri,
                    replicationFactor = replication,
                    activeSessionsByNodeId = sessionsMap
                ).onSuccess { vf ->
                    container.bandwidthMeter.recordLanBytes(vf.totalSize)
                    vdfsStatusMessage.value = "'${vf.originalName}' সফলভাবে যুক্ত হয়েছে (${vf.chunkCount} ভাগ)"
                }.onFailure { err ->
                    vdfsStatusMessage.value = "ড্রাইভে সংরক্ষণ ব্যর্থ: ${err.message}"
                }
            } finally {
                releaseBackgroundTask(ActiveBackgroundTask.VDFS_UPLOAD)
            }
        }
    }

    fun startSplitCompute(vdfsNodes: List<SwarmNodeEntity>) {
        val videoUri = selectedVideoUri.value ?: return
        appScope.launch {
            acquireBackgroundTask(ActiveBackgroundTask.SPLIT_COMPUTE)
            try {
                val jobId = "cmp_${UUID.randomUUID().toString().take(8)}"
                val workDir = File(container.appContext.cacheDir, "compute_$jobId")
                val splitRes = container.keyframeVideoSplitter.splitVideoAtKeyframes(
                    sourceUri = videoUri,
                    outputDir = File(workDir, "segments"),
                    desiredSegments = maxOf(2, vdfsNodes.size)
                )
                if (splitRes.isSuccess) {
                    val segments = splitRes.getOrThrow().map { it.file }
                    val workers = vdfsNodes.ifEmpty {
                        listOf(
                            SwarmNodeEntity(
                                container.localIdentity.nodeId,
                                container.localIdentity.deviceName,
                                0L, 0L, 100, null, SwarmNodeStatus.ONLINE, System.currentTimeMillis()
                            )
                        )
                    }.map {
                        ComputeWorkerNode(
                            nodeId = it.nodeId,
                            name = it.name,
                            isLocal = it.nodeId == container.localIdentity.nodeId,
                            hardwareCodecAvailable = container.hardwareCodecCapabilities.isSupportedForHardwareCompute
                        )
                    }
                    val processed = container.splitComputeOrchestrator.executeDistributedJob(
                        jobId = jobId,
                        segmentInputs = segments,
                        workers = workers,
                        canProceedThermalBattery = { container.thermalGuard.snapshot.value.canRunHeavyTasks }
                    ) { segIdx, segFile, _ ->
                        container.hardwareTranscodeWorker.processSegment(
                            inputSegment = segFile,
                            outputSegment = File(workDir, "processed/seg_$segIdx.mp4"),
                            requireHardwareCodec = hwAccelSwitchState.value == RikoSwitchState.ON,
                            canProceedThermalBattery = { container.thermalGuard.snapshot.value.canRunHeavyTasks }
                        )
                    }
                    processed.onSuccess { outSegs ->
                        val reassembled = container.videoSegmentReassembler.reassemble(
                            orderedSegmentFiles = outSegs,
                            outputFile = File(container.appContext.filesDir, "nexmesh_compute_$jobId.mp4")
                        )
                        reassembled.onSuccess { res ->
                            container.splitComputeOrchestrator.markReassembled(res.outputFile, res.sha256)
                        }
                    }
                }
            } finally {
                releaseBackgroundTask(ActiveBackgroundTask.SPLIT_COMPUTE)
            }
        }
    }

    fun toggleRemoteRelay(wantOn: Boolean, url: String) {
        appScope.launch {
            if (wantOn) {
                val sess = activeSession.value ?: return@launch
                acquireBackgroundTask(ActiveBackgroundTask.REMOTE_RELAY)
                val res = container.relayClient.connect(url, container.localIdentity.nodeId, sess)
                if (res.isFailure) {
                    releaseBackgroundTask(ActiveBackgroundTask.REMOTE_RELAY)
                }
            } else {
                container.relayClient.disconnect()
                releaseBackgroundTask(ActiveBackgroundTask.REMOTE_RELAY)
            }
        }
    }

    fun runNativeBenchmark() {
        appScope.launch {
            isBenchmarkRunning.value = true
            benchmarkResult.value = container.nativeBenchmarkRunner.runStreamingBenchmark()
            isBenchmarkRunning.value = false
        }
    }
}
