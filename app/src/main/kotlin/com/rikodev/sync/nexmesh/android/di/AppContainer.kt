package com.rikodev.sync.nexmesh.android.di

import android.content.Context
import android.os.StatFs
import com.rikodev.sync.nexmesh.android.runtime.SwarmRuntimeController
import com.rikodev.sync.nexmesh.core.compute.codec.HardwareCodecInspector
import com.rikodev.sync.nexmesh.core.compute.engine.SplitComputeOrchestrator
import com.rikodev.sync.nexmesh.core.compute.video.HardwareTranscodeWorker
import com.rikodev.sync.nexmesh.core.compute.video.KeyframeVideoSplitter
import com.rikodev.sync.nexmesh.core.compute.video.VideoSegmentReassembler
import com.rikodev.sync.nexmesh.core.guardian.oem.OemBatteryHelper
import com.rikodev.sync.nexmesh.core.guardian.service.SwarmForegroundService
import com.rikodev.sync.nexmesh.core.guardian.thermal.ThermalBatteryGuard
import com.rikodev.sync.nexmesh.core.guardian.thermal.ThermalReading
import com.rikodev.sync.nexmesh.core.nativeengine.NativeBenchmarkRunner
import com.rikodev.sync.nexmesh.core.network.client.SwarmControllerClient
import com.rikodev.sync.nexmesh.core.network.discovery.DiscoveryOrchestrator
import com.rikodev.sync.nexmesh.core.network.discovery.HotspotGatewayScanner
import com.rikodev.sync.nexmesh.core.network.discovery.NsdDiscoveryEngine
import com.rikodev.sync.nexmesh.core.network.discovery.UdpBeaconEngine
import com.rikodev.sync.nexmesh.core.network.server.SwarmNodeServer
import com.rikodev.sync.nexmesh.core.network.transfer.SafStreamAdapter
import com.rikodev.sync.nexmesh.core.network.transfer.StreamingTransferEngine
import com.rikodev.sync.nexmesh.core.network.transfer.TransferCheckpointStore
import com.rikodev.sync.nexmesh.core.protocol.identity.NodeIdentityManager
import com.rikodev.sync.nexmesh.core.protocol.identity.TrustedPeerStore
import com.rikodev.sync.nexmesh.core.protocol.model.NodeHealthPayload
import com.rikodev.sync.nexmesh.core.protocol.session.PendingPairingChallenge
import com.rikodev.sync.nexmesh.core.protocol.session.SessionManager
import com.rikodev.sync.nexmesh.core.relay.ledger.UsageCreditLedger
import com.rikodev.sync.nexmesh.core.relay.meter.LocalBandwidthMeter
import com.rikodev.sync.nexmesh.core.relay.tunnel.ZeroKnowledgeRelayClient
import com.rikodev.sync.nexmesh.core.vdfs.db.VdfsDatabase
import com.rikodev.sync.nexmesh.core.vdfs.engine.VdfsOrchestrator
import com.rikodev.sync.nexmesh.core.vdfs.storage.SafVaultMountManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext

    val identityManager = NodeIdentityManager(appContext)
    val localIdentity = identityManager.getOrCreateIdentity()
    val trustedPeerStore = TrustedPeerStore(appContext)
    val sessionManager = SessionManager()

    val thermalGuard = ThermalBatteryGuard(appContext).apply { startMonitoring() }
    val oemBatteryHelper = OemBatteryHelper(appContext)
    val nativeBenchmarkRunner = NativeBenchmarkRunner()

    val controllerClient = SwarmControllerClient()
    val checkpointStore = TransferCheckpointStore(File(appContext.filesDir, "nexmesh_checkpoints"))
    val safAdapter = SafStreamAdapter(appContext)

    val vdfsDatabase = VdfsDatabase.getInstance(appContext)
    val safVaultMountManager = SafVaultMountManager(appContext)

    val hardwareCodecCapabilities = HardwareCodecInspector.inspect()
    val keyframeVideoSplitter = KeyframeVideoSplitter(appContext)
    val hardwareTranscodeWorker = HardwareTranscodeWorker(hardwareCodecCapabilities)
    val videoSegmentReassembler = VideoSegmentReassembler()
    val splitComputeOrchestrator = SplitComputeOrchestrator()

    val bandwidthMeter = LocalBandwidthMeter()
    val usageCreditLedger = UsageCreditLedger(File(appContext.noBackupFilesDir, "nexmesh_credit_ledger.json"))
    val relayClient = ZeroKnowledgeRelayClient(bandwidthMeter, usageCreditLedger)

    private val _pendingIncomingChallenge = MutableStateFlow<PendingPairingChallenge?>(null)
    val pendingIncomingChallenge: StateFlow<PendingPairingChallenge?> = _pendingIncomingChallenge.asStateFlow()

    val nodeServer = SwarmNodeServer(
        identityManager = identityManager,
        trustedPeerStore = trustedPeerStore,
        sessionManager = sessionManager,
        checkpointStore = checkpointStore,
        storageStagingDir = File(appContext.filesDir, "nexmesh_received"),
        healthProvider = {
            val snap = thermalGuard.snapshot.value
            val tempC = (snap.temperature as? ThermalReading.Available)?.celsius
            val stat = StatFs(appContext.filesDir.absolutePath)
            NodeHealthPayload(
                nodeId = localIdentity.nodeId,
                deviceName = localIdentity.deviceName,
                batteryPct = snap.batteryPct,
                isCharging = snap.isCharging,
                tempCelsius = tempC,
                thermalThrottled = !snap.canRunHeavyTasks,
                freeStorageBytes = stat.availableBytes,
                totalStorageBytes = stat.totalBytes,
                sharedFolderCount = safVaultMountManager.sharedFolders.size,
                sharedFileCount = safVaultMountManager.sharedFiles.size,
                serverPort = HotspotGatewayScanner.DEFAULT_SWARM_PORT
            )
        },
        vaultChildrenProvider = { parentId -> safVaultMountManager.listChildren(parentId) },
        vaultMetaProvider = { fileId -> safVaultMountManager.getFileMetadata(fileId) },
        vaultFileOpener = { fileId -> safVaultMountManager.openMountedFileInputStream(fileId) },
        vaultPfdOpener = { fileId -> safVaultMountManager.openReadOnlyFileDescriptor(fileId) },
        onPendingChallenge = { challenge ->
            _pendingIncomingChallenge.value = challenge
        }
    )

    val discoveryOrchestrator = DiscoveryOrchestrator(
        nsdEngine = NsdDiscoveryEngine(appContext),
        udpEngine = UdpBeaconEngine(),
        hotspotScanner = HotspotGatewayScanner(appContext, controllerClient)
    )

    val transferEngine = StreamingTransferEngine(
        client = controllerClient,
        safAdapter = safAdapter,
        checkpointStore = checkpointStore,
        canProceedThermalBattery = { thermalGuard.snapshot.value.canRunHeavyTasks }
    )

    val vdfsOrchestrator = VdfsOrchestrator(
        database = vdfsDatabase,
        safAdapter = safAdapter,
        transferEngine = transferEngine
    )

    val runtimeController: SwarmRuntimeController by lazy { SwarmRuntimeController(this) }

    init {
        SwarmForegroundService.onTimeoutCallback = {
            transferEngine.pauseTransfer()
        }
        SwarmForegroundService.onGracefulShutdownCallback = {
            transferEngine.pauseTransfer()
            discoveryOrchestrator.stopAdvertising()
            nodeServer.stop()
            runtimeController.onServiceStoppedExternally()
        }
    }

    fun clearPendingChallenge() {
        _pendingIncomingChallenge.value = null
    }
}
