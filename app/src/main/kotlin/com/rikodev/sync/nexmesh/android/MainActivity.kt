package com.rikodev.sync.nexmesh.android

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.StatFs
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.sync.nexmesh.android.di.AppContainer
import com.rikodev.sync.nexmesh.core.guardian.thermal.ThermalReading
import com.rikodev.sync.nexmesh.core.nativeengine.NativeEngineBridge
import com.rikodev.sync.nexmesh.core.network.discovery.HotspotGatewayScanner
import com.rikodev.sync.nexmesh.core.protocol.model.QrPairingUri
import com.rikodev.sync.nexmesh.core.relay.tunnel.RelayTunnelState
import com.rikodev.sync.nexmesh.core.vdfs.entity.SwarmNodeEntity
import com.rikodev.sync.nexmesh.core.vdfs.entity.SwarmNodeStatus
import com.rikodev.sync.nexmesh.feature.ui.components.ExitConfirmationDialog
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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as NexMeshApplication).container

        setContent {
            NexMeshTheme {
                NexMeshRootApp(
                    container = container,
                    onExitApp = { finish() }
                )
            }
        }
    }
}

@Composable
private fun NexMeshRootApp(
    container: AppContainer,
    onExitApp: () -> Unit
) {
    val runtime = container.runtimeController

    // Proper Android Navigation Back Stack
    val backStack = remember { mutableStateListOf(NexMeshTab.DASHBOARD) }
    val currentTab = backStack.last()
    var showExitDialog by remember { mutableStateOf(false) }

    fun navigateTo(tab: NexMeshTab) {
        if (tab == currentTab) return
        if (tab == NexMeshTab.DASHBOARD) {
            backStack.clear()
            backStack.add(NexMeshTab.DASHBOARD)
        } else {
            backStack.remove(tab)
            backStack.add(tab)
        }
    }

    // Intercept Back Button: pop previous screen if on sub-screen, show Exit Popup on Home
    BackHandler(enabled = true) {
        if (showExitDialog) {
            showExitDialog = false
        } else if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        } else {
            showExitDialog = true
        }
    }

    // Collect Application-scoped runtime states (survives minimize / screen-off / activity recreation)
    val selectedModeIndex by runtime.selectedModeIndex.collectAsState()
    val nodeServerSwitchState by runtime.nodeServerSwitchState.collectAsState()
    val redundancy2xSwitchState by runtime.redundancy2xSwitchState.collectAsState()
    val hwAccelSwitchState by runtime.hwAccelSwitchState.collectAsState()
    val lz4SwitchState by runtime.lz4SwitchState.collectAsState()
    val selectedFileName by runtime.selectedFileName.collectAsState()
    val selectedVideoName by runtime.selectedVideoName.collectAsState()
    val activeSession by runtime.activeSession.collectAsState()
    val outgoingChallenge by runtime.outgoingChallenge.collectAsState()
    val pairingStatusMessage by runtime.pairingStatusMessage.collectAsState()
    val vdfsStatusMessage by runtime.vdfsStatusMessage.collectAsState()
    val benchmarkResult by runtime.benchmarkResult.collectAsState()
    val isBenchmarkRunning by runtime.isBenchmarkRunning.collectAsState()
    val hasActiveBackgroundWork by runtime.hasActiveBackgroundWork.collectAsState()

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
            runtime.selectedFileUri.value = uri
            runtime.selectedFileName.value = container.safAdapter.describeFile(uri)?.displayName ?: uri.lastPathSegment
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runtime.selectedVideoUri.value = uri
            runtime.selectedVideoName.value = container.safAdapter.describeFile(uri)?.displayName ?: uri.lastPathSegment
        }
    }

    val vdfsUploadLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runtime.ingestFileToVdfs(uri)
        }
    }

    val safTreeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            val mounted = container.safVaultMountManager.mountTreeUri(treeUri)
            runtime.vdfsStatusMessage.value = if (mounted != null) {
                "ফোল্ডার যুক্ত হয়েছে: ${mounted.displayName}"
            } else {
                "ফোল্ডার যুক্ত করতে সমস্যা হয়েছে"
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
                    contentDescription = "সুরক্ষা স্ট্যাটাস",
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
                        runtime.selectedModeIndex.value = idx
                        if (idx == 1 && nodeServerSwitchState == RikoSwitchState.OFF) {
                            runtime.toggleNodeServer(true)
                        }
                    },
                    safety = safety,
                    peers = discoveredPeers,
                    trustedCount = trustedPeers.size,
                    lastThroughputMbps = transferProgress.throughputMbps,
                    onRefreshDiscovery = {
                        container.discoveryOrchestrator.startAllDiscovery(container.localIdentity.nodeId)
                    },
                    onPeerClick = { peer ->
                        runtime.initiateHandshake(peer.host, peer.port, peer.nodeId) {
                            navigateTo(NexMeshTab.PAIRING)
                        }
                    },
                    headerContent = header
                )

                NexMeshTab.VDFS_DRIVE -> VirtualDriveScreen(
                    mountedVault = mountedVault,
                    swarmNodes = vdfsNodes,
                    files = vdfsFiles,
                    redundancy2xSwitchState = redundancy2xSwitchState,
                    vdfsStatusMessage = vdfsStatusMessage,
                    onToggle2xRedundancy = { wantOn ->
                        runtime.redundancy2xSwitchState.value = if (wantOn) RikoSwitchState.ON else RikoSwitchState.OFF
                    },
                    onMountSafFolder = { safTreeLauncher.launch(null) },
                    onIngestFileToVdfs = { vdfsUploadLauncher.launch(arrayOf("*/*")) },
                    onDeleteFile = { fileId ->
                        runtime.appScope.launch { container.vdfsOrchestrator.deleteVirtualFile(fileId) }
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
                            runtime.hwAccelSwitchState.value = if (wantOn) RikoSwitchState.ON else RikoSwitchState.OFF
                        }
                    },
                    onPickVideo = { videoPickerLauncher.launch(arrayOf("video/mp4", "video/*")) },
                    onStartCompute = { runtime.startSplitCompute(vdfsNodes) },
                    headerContent = header
                )

                NexMeshTab.PAIRING -> PairingScreen(
                    localQrUri = localQrUri,
                    nodeServerSwitchState = nodeServerSwitchState,
                    onToggleNodeServer = { runtime.toggleNodeServer(it) },
                    pendingChallenge = incomingChallenge ?: outgoingChallenge,
                    onApproveSas = { token -> runtime.approveSas(token) },
                    onRejectSas = { token -> runtime.rejectSas(token) },
                    onPairWithUri = { raw ->
                        QrPairingUri.parse(raw).onSuccess { uri ->
                            container.discoveryOrchestrator.registerFromQrUri(uri)
                            runtime.initiateHandshake(uri.host, uri.port, uri.nodeId)
                        }.onFailure {
                            runtime.pairingStatusMessage.value = "সঠিক লিংক নয়: ${it.message}"
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
                            runtime.lz4SwitchState.value = if (wantOn) RikoSwitchState.ON else RikoSwitchState.OFF
                        }
                    },
                    onPickSafFile = { safFileLauncher.launch(arrayOf("*/*")) },
                    onPickSafFolder = { safTreeLauncher.launch(null) },
                    onStartOrResumeTransfer = { runtime.startOrResumeFileTransfer() },
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
                    onToggleRelay = { wantOn, url -> runtime.toggleRemoteRelay(wantOn, url) },
                    onRunBenchmark = { runtime.runNativeBenchmark() },
                    onOpenOemSettings = { container.oemBatteryHelper.openBestBatterySettings() },
                    headerContent = header
                )
            }

            FloatingGlassDock(
                currentTab = currentTab,
                onSelectTab = { navigateTo(it) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            ExitConfirmationDialog(
                visible = showExitDialog,
                hasActiveBackgroundWork = hasActiveBackgroundWork,
                onDismiss = { showExitDialog = false },
                onConfirmExit = {
                    showExitDialog = false
                    onExitApp()
                }
            )
        }
    }
}
