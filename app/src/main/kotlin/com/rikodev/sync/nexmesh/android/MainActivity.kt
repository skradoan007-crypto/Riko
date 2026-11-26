package com.rikodev.sync.nexmesh.android

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.StatFs
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.sync.nexmesh.core.compute.engine.ComputeWorkerNode
import com.rikodev.sync.nexmesh.core.guardian.service.SwarmForegroundService
import com.rikodev.sync.nexmesh.core.guardian.thermal.ThermalReading
import com.rikodev.sync.nexmesh.core.nativeengine.NativeBenchmarkResult
import com.rikodev.sync.nexmesh.core.nativeengine.NativeEngineBridge
import com.rikodev.sync.nexmesh.core.network.discovery.HotspotGatewayScanner
import com.rikodev.sync.nexmesh.core.protocol.crypto.Base64UrlCompat
import com.rikodev.sync.nexmesh.core.protocol.crypto.EcdhKeyExchange
import com.rikodev.sync.nexmesh.core.protocol.model.PairConfirmRequest
import com.rikodev.sync.nexmesh.core.protocol.model.PairInitRequest
import com.rikodev.sync.nexmesh.core.protocol.model.QrPairingUri
import com.rikodev.sync.nexmesh.core.protocol.session.ActiveSwarmSession
import com.rikodev.sync.nexmesh.core.protocol.session.PendingPairingChallenge
import com.rikodev.sync.nexmesh.core.relay.tunnel.RelayTunnelState
import com.rikodev.sync.nexmesh.core.vdfs.entity.SwarmNodeEntity
import com.rikodev.sync.nexmesh.core.vdfs.entity.SwarmNodeStatus
import com.rikodev.sync.nexmesh.feature.ui.components.FloatingGlassDock
import com.rikodev.sync.nexmesh.feature.ui.components.NexMeshTab
import com.rikodev.sync.nexmesh.feature.ui.components.RikoSwitchState
import com.rikodev.sync.nexmesh.feature.ui.screens.DashboardScreen
import com.rikodev.sync.nexmesh.feature.ui.screens.DiagnosticsScreen
import com.rikodev.sync.nexmesh.feature.ui.screens.PairingScreen
import com.rikodev.sync.nexmesh.feature.ui.screens.SplitComputeScreen
import com.rikodev.sync.nexmesh.feature.ui.screens.TransferScreen
import com.rikodev.sync.nexmesh.feature.ui.screens.VirtualDriveScreen
import com.rikodev.sync.nexmesh.feature.ui.theme.LiquidGlassBackdrop
import com.rikodev.sync.nexmesh.feature.ui.theme.NexMeshColors
import com.rikodev.sync.nexmesh.feature.ui.theme.NexMeshTheme
import com.rikodev.sync.nexmesh.feature.ui.theme.liquidGlassSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.security.SecureRandom
import java.util.UUID

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as NexMeshApplication).container

        setContent {
            NexMeshTheme {
                NexMeshRootApp(container = container)
            }
        }
    }
}

@Composable
private fun NexMeshRootApp(container: com.rikodev.sync.nexmesh.android.di.AppContainer) {
    val scope = rememberCoroutineScope()

    var currentTab by remember { mutableStateOf(NexMeshTab.DASHBOARD) }
    var selectedModeIndex by remember { mutableIntStateOf(0) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedVideoName by remember { mutableStateOf<String?>(null) }
    var activeSession by remember { mutableStateOf<ActiveSwarmSession?>(null) }
    var activePeerHost by remember { mutableStateOf<String?>(null) }
    var activePeerPort by remember { mutableIntStateOf(HotspotGatewayScanner.DEFAULT_SWARM_PORT) }

    var pairingStatusMessage by remember { mutableStateOf<String?>(null) }
    var vdfsStatusMessage by remember { mutableStateOf<String?>(null) }
    var outgoingChallenge by remember { mutableStateOf<PendingPairingChallenge?>(null) }
    var outgoingTargetHost by remember { mutableStateOf<String?>(null) }
    var outgoingTargetPort by remember { mutableIntStateOf(HotspotGatewayScanner.DEFAULT_SWARM_PORT) }

    var nodeServerSwitchState by remember { mutableStateOf(RikoSwitchState.OFF) }
    var redundancy2xSwitchState by remember { mutableStateOf(RikoSwitchState.OFF) }
    var hwAccelSwitchState by remember {
        mutableStateOf(
            if (container.hardwareCodecCapabilities.isSupportedForHardwareCompute) RikoSwitchState.ON else RikoSwitchState.LOCKED
        )
    }
    var lz4SwitchState by remember {
        mutableStateOf(if (NativeEngineBridge.isAvailable) RikoSwitchState.ON else RikoSwitchState.LOCKED)
    }
    var benchmarkResult by remember { mutableStateOf<NativeBenchmarkResult?>(null) }
    var isBenchmarkRunning by remember { mutableStateOf(false) }

    val safety by container.thermalGuard.snapshot.collectAsState()
    val discoveredPeers by container.discoveryOrchestrator.peers.collectAsState()
    val trustedPeers by container.trustedPeerStore.peers.collectAsState()
    val transferProgress by container.transferEngine.progress.collectAsState()
    val incomingChallenge by container.pendingIncomingChallenge.collectAsState()
    val mountedVault by container.safVaultMountManager.mountedVault.collectAsState()
    val vdfsFiles by container.vdfsOrchestrator.observeVirtualDrive().collectAsState(initial = emptyList())
    val vdfsNodes by container.vdfsOrchestrator.observeSwarmNodes().collectAsState(initial = emptyList())
    val computeSnapshot by container.splitComputeOrchestrator.snapshot.collectAsState()

    val relayState by container.relayClient.state.collectAsState()
    val relayError by container.relayClient.lastError.collectAsState()
    val bandwidth by container.bandwidthMeter.snapshot.collectAsState()
    val ledgerState by container.usageCreditLedger.state.collectAsState()

    val relaySwitchState = when (relayState) {
        RelayTunnelState.CONNECTED -> RikoSwitchState.ON
        RelayTunnelState.CONNECTING -> RikoSwitchState.ENGAGING
        RelayTunnelState.DISCONNECTED, RelayTunnelState.ERROR -> {
            if (activeSession == null) RikoSwitchState.LOCKED else RikoSwitchState.OFF
        }
    }

    val notificationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val safFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                container.appContext.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            selectedFileUri = uri
            selectedFileName = container.safAdapter.describeFile(uri)?.displayName ?: uri.lastPathSegment
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedVideoUri = uri
            selectedVideoName = container.safAdapter.describeFile(uri)?.displayName ?: uri.lastPathSegment
        }
    }

    val vdfsUploadLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                vdfsStatusMessage = "Chunking & distributing file across VDFS..."
                val sessionsMap = activeSession?.let { mapOf(it.peerNodeId to it) } ?: emptyMap()
                val replication = if (redundancy2xSwitchState == RikoSwitchState.ON) 2 else 1
                container.vdfsOrchestrator.ingestFileToVdfs(
                    fileUri = uri,
                    replicationFactor = replication,
                    activeSessionsByNodeId = sessionsMap
                ).onSuccess { vf ->
                    container.bandwidthMeter.recordLanBytes(vf.totalSize)
                    vdfsStatusMessage = "Committed '${vf.originalName}' (${vf.chunkCount} chunks, ${vf.replicationFactor}x replica)"
                }.onFailure { err ->
                    vdfsStatusMessage = "VDFS ingest failed: ${err.message}"
                }
            }
        }
    }

    val safTreeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            val mounted = container.safVaultMountManager.mountTreeUri(treeUri)
            vdfsStatusMessage = if (mounted != null) {
                "SAF Vault mounted: ${mounted.displayName}"
            } else {
                "Failed to mount selected SAF folder"
            }
        }
    }

    LaunchedEffect(safety) {
        val stat = StatFs(container.appContext.filesDir.absolutePath)
        val tempC = (safety.temperature as? ThermalReading.Available)?.celsius
        container.vdfsOrchestrator.upsertNodeTelemetry(
            SwarmNodeEntity(
                nodeId = container.localIdentity.nodeId,
                name = container.localIdentity.deviceName,
                totalBytes = stat.totalBytes,
                freeBytes = stat.availableBytes,
                batteryPct = safety.batteryPct,
                tempCelsius = tempC,
                status = if (safety.canRunHeavyTasks) SwarmNodeStatus.ONLINE else SwarmNodeStatus.THROTTLED,
                lastSeen = System.currentTimeMillis(),
                host = "127.0.0.1",
                port = HotspotGatewayScanner.DEFAULT_SWARM_PORT,
                isCharging = safety.isCharging
            )
        )
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        container.discoveryOrchestrator.startAllDiscovery(container.localIdentity.nodeId)
    }

    fun toggleNodeServer(enable: Boolean) {
        scope.launch {
            nodeServerSwitchState = RikoSwitchState.ENGAGING
            delay(180)
            runCatching {
                if (enable) {
                    container.nodeServer.start(HotspotGatewayScanner.DEFAULT_SWARM_PORT)
                    container.discoveryOrchestrator.startAdvertising(
                        nodeId = container.localIdentity.nodeId,
                        deviceName = container.localIdentity.deviceName,
                        tcpPort = HotspotGatewayScanner.DEFAULT_SWARM_PORT
                    )
                    SwarmForegroundService.start(
                        container.appContext,
                        "Nex Mesh Node Active",
                        "Listening on port ${HotspotGatewayScanner.DEFAULT_SWARM_PORT} (E2EE)"
                    )
                    nodeServerSwitchState = RikoSwitchState.ON
                } else {
                    container.discoveryOrchestrator.stopAdvertising()
                    container.nodeServer.stop()
                    SwarmForegroundService.stop(container.appContext)
                    nodeServerSwitchState = RikoSwitchState.OFF
                }
            }.onFailure {
                nodeServerSwitchState = RikoSwitchState.OFF
                pairingStatusMessage = "Server toggle error: ${it.message}"
            }
        }
    }

    fun initiateHandshake(host: String, port: Int, expectedNodeId: String? = null) {
        scope.launch {
            pairingStatusMessage = "Running ECDH P-256 handshake with $host:$port..."
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
                    error("Node ID mismatch! Potential MITM rejected.")
                }

                val material = EcdhKeyExchange.deriveSessionMaterial(
                    localEphemeralPrivate = eph.private,
                    peerEphemeralPublic = EcdhKeyExchange.decodePublicKeyB64Url(resp.workerEphemeralPubKeyB64),
                    controllerStaticPubBytes = container.localIdentity.publicKey.encoded,
                    workerStaticPubBytes = Base64UrlCompat.decode(resp.workerStaticPubKeyB64),
                    controllerNonce = nonce,
                    workerNonce = Base64UrlCompat.decode(resp.workerNonceB64)
                )
                outgoingTargetHost = host
                outgoingTargetPort = port
                outgoingChallenge = PendingPairingChallenge(
                    pairingToken = resp.pairingToken,
                    peerNodeId = resp.workerNodeId,
                    peerDeviceName = resp.workerDeviceName,
                    peerStaticPubKeyB64 = resp.workerStaticPubKeyB64,
                    sasCode = material.sasCode6Digits,
                    derivedMaterial = material,
                    expiresAtEpochMs = resp.expiresAtEpochMs
                )
                pairingStatusMessage = "Verify 6-digit SAS code on both devices."
                currentTab = NexMeshTab.PAIRING
            }.onFailure {
                pairingStatusMessage = "Handshake failed: ${it.message}"
            }
        }
    }

    val localIp = container.discoveryOrchestrator.localIpv4Address()
    val localQrUri = remember(localIp, container.localIdentity.nodeId) {
        if (localIp != null) {
            QrPairingUri(
                host = localIp,
                port = HotspotGatewayScanner.DEFAULT_SWARM_PORT,
                publicKeyB64Url = container.localIdentity.publicKeyB64Url,
                nodeId = container.localIdentity.nodeId,
                expiresAtEpochMs = System.currentTimeMillis() + 10 * 60 * 1000L
            ).toUriString()
        } else {
            ""
        }
    }

    val header: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 10.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.ic_nexmesh_logo),
                    contentDescription = "Nex Mesh Logo",
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Nex Mesh",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = NexMeshColors.InkPrimary
                )
            }
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .liquidGlassSurface(
                        shape = CircleShape,
                        topColor = Color(0xCCFFFFFF),
                        bottomColor = Color(0xA6EAF1FF),
                        elevation = 4.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = "E2EE Active",
                    tint = if (activeSession != null) NexMeshColors.StatusHealthy else NexMeshColors.InkSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    LiquidGlassBackdrop {
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentTab) {
                NexMeshTab.DASHBOARD -> DashboardScreen(
                    selectedModeIndex = selectedModeIndex,
                    onModeSelect = { idx ->
                        selectedModeIndex = idx
                        if (idx == 1 && nodeServerSwitchState == RikoSwitchState.OFF) {
                            toggleNodeServer(true)
                        }
                    },
                    safety = safety,
                    peers = discoveredPeers,
                    trustedCount = trustedPeers.size,
                    lastThroughputMbps = transferProgress.throughputMbps,
                    onRefreshDiscovery = {
                        container.discoveryOrchestrator.startAllDiscovery(container.localIdentity.nodeId)
                    },
                    onPeerClick = { peer -> initiateHandshake(peer.host, peer.port, peer.nodeId) },
                    headerContent = header
                )

                NexMeshTab.VDFS_DRIVE -> VirtualDriveScreen(
                    mountedVault = mountedVault,
                    swarmNodes = vdfsNodes,
                    files = vdfsFiles,
                    redundancy2xSwitchState = redundancy2xSwitchState,
                    vdfsStatusMessage = vdfsStatusMessage,
                    onToggle2xRedundancy = { wantOn ->
                        redundancy2xSwitchState = if (wantOn) RikoSwitchState.ON else RikoSwitchState.OFF
                    },
                    onMountSafFolder = { safTreeLauncher.launch(null) },
                    onIngestFileToVdfs = { vdfsUploadLauncher.launch(arrayOf("*/*")) },
                    onDeleteFile = { fileId ->
                        scope.launch { container.vdfsOrchestrator.deleteVirtualFile(fileId) }
                    },
                    headerContent = header
                )

                NexMeshTab.COMPUTE -> SplitComputeScreen(
                    capabilities = container.hardwareCodecCapabilities,
                    snapshot = computeSnapshot,
                    selectedVideoName = selectedVideoName,
                    hwAccelSwitchState = hwAccelSwitchState,
                    onToggleHwAccel = { wantOn ->
                        if (container.hardwareCodecCapabilities.isSupportedForHardwareCompute) {
                            hwAccelSwitchState = if (wantOn) RikoSwitchState.ON else RikoSwitchState.OFF
                        }
                    },
                    onPickVideo = { videoPickerLauncher.launch(arrayOf("video/mp4", "video/*")) },
                    onStartCompute = {
                        val videoUri = selectedVideoUri ?: return@SplitComputeScreen
                        scope.launch {
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
                                        requireHardwareCodec = hwAccelSwitchState == RikoSwitchState.ON,
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
                        }
                    },
                    headerContent = header
                )

                NexMeshTab.PAIRING -> PairingScreen(
                    localQrUri = localQrUri,
                    nodeServerSwitchState = nodeServerSwitchState,
                    onToggleNodeServer = { toggleNodeServer(it) },
                    pendingChallenge = incomingChallenge ?: outgoingChallenge,
                    onApproveSas = { token ->
                        if (incomingChallenge?.pairingToken == token) {
                            container.sessionManager.setLocalDecision(token, approved = true)
                            container.clearPendingChallenge()
                            pairingStatusMessage = "Worker approved SAS! Waiting for Controller confirmation."
                        } else if (outgoingChallenge?.pairingToken == token) {
                            val ch = outgoingChallenge!!
                            val host = outgoingTargetHost ?: return@PairingScreen
                            scope.launch {
                                runCatching {
                                    val resp = container.controllerClient.confirmPairing(
                                        host,
                                        outgoingTargetPort,
                                        PairConfirmRequest(token, container.localIdentity.nodeId, true)
                                    )
                                    if (resp.status == "APPROVED" && resp.sessionId != null) {
                                        activeSession = container.sessionManager.promoteToActiveSession(
                                            peerNodeId = ch.peerNodeId,
                                            peerDeviceName = ch.peerDeviceName,
                                            material = ch.derivedMaterial,
                                            sessionId = resp.sessionId!!
                                        )
                                        activePeerHost = host
                                        activePeerPort = outgoingTargetPort
                                        outgoingChallenge = null
                                        container.controllerClient.fetchHealth(host, outgoingTargetPort)?.let { h ->
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
                                        pairingStatusMessage = "E2EE Session established with ${ch.peerDeviceName}!"
                                    } else {
                                        pairingStatusMessage = "Peer status: ${resp.status} (Approve on Worker too)"
                                    }
                                }.onFailure {
                                    pairingStatusMessage = "Confirm failed: ${it.message}"
                                }
                            }
                        }
                    },
                    onRejectSas = { token ->
                        container.sessionManager.setLocalDecision(token, approved = false)
                        container.clearPendingChallenge()
                        outgoingChallenge = null
                        pairingStatusMessage = "Pairing rejected."
                    },
                    onPairWithUri = { raw ->
                        QrPairingUri.parse(raw).onSuccess { uri ->
                            container.discoveryOrchestrator.registerFromQrUri(uri)
                            initiateHandshake(uri.host, uri.port, uri.nodeId)
                        }.onFailure {
                            pairingStatusMessage = "Invalid QR URI: ${it.message}"
                        }
                    },
                    pairingStatusMessage = pairingStatusMessage,
                    headerContent = header
                )

                NexMeshTab.TRANSFER -> TransferScreen(
                    snapshot = transferProgress,
                    selectedFileName = selectedFileName,
                    hasActiveSession = activeSession != null,
                    lz4SwitchState = lz4SwitchState,
                    onToggleLz4 = { wantOn ->
                        if (NativeEngineBridge.isAvailable) {
                            lz4SwitchState = if (wantOn) RikoSwitchState.ON else RikoSwitchState.OFF
                        }
                    },
                    onPickSafFile = { safFileLauncher.launch(arrayOf("*/*")) },
                    onPickSafFolder = { safTreeLauncher.launch(null) },
                    onStartOrResumeTransfer = {
                        val uri = selectedFileUri ?: return@TransferScreen
                        val sess = activeSession ?: return@TransferScreen
                        val host = activePeerHost ?: return@TransferScreen
                        scope.launch {
                            SwarmForegroundService.start(
                                container.appContext,
                                "Nex Mesh Streaming",
                                "Encrypting & transferring ${selectedFileName ?: "file"}"
                            )
                            val txRes = container.transferEngine.uploadSafFile(
                                fileUri = uri,
                                host = host,
                                port = activePeerPort,
                                session = sess,
                                existingTransferId = transferProgress.transferId.takeIf { it.isNotBlank() },
                                useNativeLz4 = lz4SwitchState == RikoSwitchState.ON
                            )
                            txRes.onSuccess { container.bandwidthMeter.recordLanBytes(it.transferredBytes) }
                            SwarmForegroundService.stop(container.appContext)
                        }
                    },
                    onPauseTransfer = { container.transferEngine.pauseTransfer() },
                    headerContent = header
                )

                NexMeshTab.DIAGNOSTICS -> DiagnosticsScreen(
                    localNodeId = container.localIdentity.nodeId,
                    localIp = localIp,
                    benchmarkResult = benchmarkResult,
                    isBenchmarkRunning = isBenchmarkRunning,
                    oemGuide = remember { container.oemBatteryHelper.inspectGuide() },
                    relayState = relayState,
                    relaySwitchState = relaySwitchState,
                    relayError = relayError,
                    bandwidth = bandwidth,
                    ledgerState = ledgerState,
                    onToggleRelay = { wantOn, url ->
                        scope.launch {
                            if (wantOn) {
                                val sess = activeSession ?: return@launch
                                container.relayClient.connect(url, container.localIdentity.nodeId, sess)
                            } else {
                                container.relayClient.disconnect()
                            }
                        }
                    },
                    onRunBenchmark = {
                        scope.launch {
                            isBenchmarkRunning = true
                            benchmarkResult = container.nativeBenchmarkRunner.runStreamingBenchmark()
                            isBenchmarkRunning = false
                        }
                    },
                    onOpenOemSettings = { container.oemBatteryHelper.openBestBatterySettings() },
                    headerContent = header
                )
            }

            FloatingGlassDock(
                currentTab = currentTab,
                onSelectTab = { currentTab = it },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
