package com.rikodev.sync.nexmesh.android

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.Toast
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.rikodev.sync.nexmesh.android.di.AppContainer
import com.rikodev.sync.nexmesh.android.provider.NexMeshDocumentsProvider
import com.rikodev.sync.nexmesh.core.network.discovery.HotspotGatewayScanner
import com.rikodev.sync.nexmesh.core.protocol.model.QrPairingUri
import com.rikodev.sync.nexmesh.core.protocol.model.RemoteVaultFileItem
import com.rikodev.sync.nexmesh.feature.ui.components.ExitConfirmationDialog
import com.rikodev.sync.nexmesh.feature.ui.components.FloatingGlassDock
import com.rikodev.sync.nexmesh.feature.ui.components.NexMeshTab
import com.rikodev.sync.nexmesh.feature.ui.components.QrScannerOverlay
import com.rikodev.sync.nexmesh.feature.ui.components.RikoSwitchState
import com.rikodev.sync.nexmesh.feature.ui.screens.DevicesScreen
import com.rikodev.sync.nexmesh.feature.ui.screens.HomeScreen
import com.rikodev.sync.nexmesh.feature.ui.screens.SettingsScreen
import com.rikodev.sync.nexmesh.feature.ui.screens.StorageScreen
import com.rikodev.sync.nexmesh.feature.ui.theme.LiquidGlassBackdrop
import com.rikodev.sync.nexmesh.feature.ui.theme.LocalNexMeshPalette
import com.rikodev.sync.nexmesh.feature.ui.theme.NexMeshTheme
import com.rikodev.sync.nexmesh.feature.ui.theme.liquidGlassSurface

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as NexMeshApplication).container

        setContent {
            val isDarkTheme by container.runtimeController.isDarkTheme.collectAsState()
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val controller = WindowCompat.getInsetsController(window, view)
                    controller.isAppearanceLightStatusBars = !isDarkTheme
                    controller.isAppearanceLightNavigationBars = !isDarkTheme
                }
            }

            NexMeshTheme(darkTheme = isDarkTheme) {
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
    val context = LocalContext.current
    val runtime = container.runtimeController

    val backStack = remember { mutableStateListOf(NexMeshTab.HOME) }
    val currentTab = backStack.last()
    var showExitDialog by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }

    fun navigateTo(tab: NexMeshTab) {
        if (tab == currentTab) return
        if (tab == NexMeshTab.HOME) {
            backStack.clear()
            backStack.add(NexMeshTab.HOME)
        } else {
            backStack.remove(tab)
            backStack.add(tab)
        }
    }

    val roleIndex by runtime.roleIndex.collectAsState()
    val isDarkTheme by runtime.isDarkTheme.collectAsState()
    val storageProviderSwitchState by runtime.storageProviderSwitchState.collectAsState()
    val activeSession by runtime.activeSession.collectAsState()
    val remoteAvailableBytes by runtime.remoteAvailableBytes.collectAsState()
    val remoteVault by runtime.remoteVault.collectAsState()
    val remoteSubfolderStack by runtime.remoteSubfolderStack.collectAsState()
    val isLoadingRemoteVault by runtime.isLoadingRemoteVault.collectAsState()
    val outgoingChallenge by runtime.outgoingChallenge.collectAsState()
    val pairingStatusMessage by runtime.pairingStatusMessage.collectAsState()
    val storageStatusMessage by runtime.storageStatusMessage.collectAsState()
    val recentActivity by runtime.recentActivity.collectAsState()
    val benchmarkResult by runtime.benchmarkResult.collectAsState()
    val isBenchmarkRunning by runtime.isBenchmarkRunning.collectAsState()
    val hasActiveBackgroundWork by runtime.hasActiveBackgroundWork.collectAsState()

    val discoveredPeers by container.discoveryOrchestrator.peers.collectAsState()
    val trustedPeers by container.trustedPeerStore.peers.collectAsState()
    val incomingChallenge by container.pendingIncomingChallenge.collectAsState()
    val sharedEntries by container.safVaultMountManager.sharedEntries.collectAsState()

    val sharedFolders = remember(sharedEntries) { sharedEntries.filter { it.isFolder } }
    val sharedFiles = remember(sharedEntries) { sharedEntries.filter { !it.isFolder } }

    BackHandler(enabled = true) {
        when {
            showQrScanner -> showQrScanner = false
            showExitDialog -> showExitDialog = false
            currentTab == NexMeshTab.STORAGE && remoteSubfolderStack.isNotEmpty() -> {
                runtime.navigateRemoteBack()
            }
            backStack.size > 1 -> backStack.removeAt(backStack.lastIndex)
            else -> showExitDialog = true
        }
    }

    val notificationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val cameraPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showQrScanner = true
        } else {
            runtime.pairingStatusMessage.value = "Camera permission is required to scan a pairing QR code."
        }
    }

    // Reliable SAF Folder Picker (fixes "Select This Folder" persistence across Android 7–15)
    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val treeUri = result.data?.data
            if (treeUri != null) {
                runtime.addSharedFolder(treeUri)
            }
        }
    }

    // Reliable SAF Multi-File Picker (zero file duplication)
    val multiFilePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val uris = mutableListOf<Uri>()
            val clip = data?.clipData
            if (clip != null) {
                for (i in 0 until clip.itemCount) {
                    clip.getItemAt(i)?.uri?.let { uris.add(it) }
                }
            } else {
                data?.data?.let { uris.add(it) }
            }
            if (uris.isNotEmpty()) {
                runtime.addSharedFiles(uris)
            }
        }
    }

    fun launchFolderPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            )
        }
        runCatching { folderPickerLauncher.launch(intent) }
            .onFailure {
                Toast.makeText(context, "Unable to open folder picker on this device.", Toast.LENGTH_SHORT).show()
            }
    }

    fun launchMultiFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }
        runCatching { multiFilePickerLauncher.launch(intent) }
            .onFailure {
                Toast.makeText(context, "Unable to open file picker on this device.", Toast.LENGTH_SHORT).show()
            }
    }

    fun openSystemFilesApp() {
        val rootUri = DocumentsContract.buildRootUri(
            NexMeshDocumentsProvider.AUTHORITY,
            NexMeshDocumentsProvider.ROOT_ID_PRIMARY
        )
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(rootUri, DocumentsContract.Root.MIME_TYPE_ITEM)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val opened = runCatching {
            context.startActivity(viewIntent)
            true
        }.getOrDefault(false)

        if (!opened) {
            // Fallback: open system DocumentsUI browse intent pointed at NexMesh root
            val browseIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, rootUri)
                }
            }
            runCatching { context.startActivity(browseIntent) }.onFailure {
                Toast.makeText(
                    context,
                    "Open your Files app and choose NexMesh from the sidebar.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun streamOpenRemoteFile(item: RemoteVaultFileItem) {
        val docUri = DocumentsContract.buildDocumentUri(NexMeshDocumentsProvider.AUTHORITY, item.fileId)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(docUri, item.mimeType.ifBlank { "*/*" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, "Open ${item.name} (Live Stream)"))
            runtime.recordActivity("Streaming ${item.name}")
        }.onFailure {
            Toast.makeText(
                context,
                "No app found to open ${item.name}. Try opening NexMesh in your Files app.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun openQrScannerWithPermission() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            showQrScanner = true
        } else {
            cameraPermLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(currentTab, activeSession) {
        if (currentTab == NexMeshTab.STORAGE && activeSession != null) {
            runtime.refreshRemoteStorage()
        }
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
        val p = LocalNexMeshPalette.current
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
                    contentDescription = "NexMesh Logo",
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "NexMesh",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = p.inkPrimary
                )
            }
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .liquidGlassSurface(
                        shape = CircleShape,
                        topColor = p.glassElevatedTop,
                        bottomColor = p.glassElevatedBottom,
                        rimTop = p.glassRimTop,
                        rimBottom = p.glassRimBottom,
                        elevation = 4.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = "Encrypted Connection Status",
                    tint = if (activeSession != null) p.statusOnline else p.inkSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    LiquidGlassBackdrop {
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentTab) {
                NexMeshTab.HOME -> HomeScreen(
                    roleIndex = roleIndex,
                    onRoleSelect = { runtime.setRole(it) },
                    connectedStoragePhoneName = activeSession?.peerDeviceName,
                    connectedStorageCount = if (activeSession != null) 1 else 0,
                    remoteAvailableBytes = remoteAvailableBytes,
                    localSharedFolderCount = sharedFolders.size,
                    localSharedFileCount = sharedFiles.size,
                    isStorageProviderActive = storageProviderSwitchState == RikoSwitchState.ON,
                    recentActivity = recentActivity,
                    onOpenSystemFiles = { openSystemFilesApp() },
                    onAddStorageClick = { navigateTo(NexMeshTab.DEVICES) },
                    onManageStorageClick = { navigateTo(NexMeshTab.STORAGE) },
                    headerContent = header
                )

                NexMeshTab.STORAGE -> StorageScreen(
                    connectedStoragePhoneName = activeSession?.peerDeviceName,
                    remoteVault = remoteVault,
                    isInsideRemoteSubfolder = remoteSubfolderStack.isNotEmpty(),
                    isLoadingRemoteVault = isLoadingRemoteVault,
                    sharedFolders = sharedFolders,
                    sharedFiles = sharedFiles,
                    statusMessage = storageStatusMessage,
                    onRefreshRemoteStorage = { runtime.refreshRemoteStorage() },
                    onNavigateRemoteBack = { runtime.navigateRemoteBack() },
                    onOpenRemoteFolder = { folder -> runtime.openRemoteSubfolder(folder) },
                    onStreamOpenRemoteFile = { item -> streamOpenRemoteFile(item) },
                    onOpenSystemFiles = { openSystemFilesApp() },
                    onSelectFolder = { launchFolderPicker() },
                    onSelectFiles = { launchMultiFilePicker() },
                    onRemoveSharedEntry = { entryId -> runtime.removeSharedEntry(entryId) },
                    headerContent = header
                )

                NexMeshTab.DEVICES -> DevicesScreen(
                    localQrUri = localQrUri,
                    storageProviderSwitchState = storageProviderSwitchState,
                    connectedPeerNodeId = activeSession?.peerNodeId,
                    trustedPeers = trustedPeers,
                    nearbyPeers = discoveredPeers,
                    pendingChallenge = incomingChallenge ?: outgoingChallenge,
                    pairingStatusMessage = pairingStatusMessage,
                    onToggleStorageProvider = { runtime.toggleStorageProvider(it) },
                    onOpenQrScanner = { openQrScannerWithPermission() },
                    onRefreshNearby = {
                        container.discoveryOrchestrator.startAllDiscovery(container.localIdentity.nodeId)
                    },
                    onPairNearbyPeer = { peer ->
                        val existingTrusted = trustedPeers.firstOrNull { it.nodeId == peer.nodeId }
                        if (existingTrusted != null) {
                            runtime.reconnectTrustedPeer(existingTrusted.copy(lastKnownHost = peer.host, lastKnownPort = peer.port))
                        } else {
                            runtime.initiateHandshake(peer.host, peer.port, peer.nodeId)
                        }
                    },
                    onReconnectTrustedPeer = { record -> runtime.reconnectTrustedPeer(record) },
                    onRemoveTrustedPeer = { nodeId -> runtime.removeTrustedPeer(nodeId) },
                    onApproveSas = { token -> runtime.approveSas(token) },
                    onRejectSas = { token -> runtime.rejectSas(token) },
                    headerContent = header
                )

                NexMeshTab.SETTINGS -> SettingsScreen(
                    deviceName = container.localIdentity.deviceName,
                    localIp = localIp,
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = { runtime.setDarkTheme(it) },
                    oemGuide = remember { container.oemBatteryHelper.inspectGuide() },
                    benchmarkResult = benchmarkResult,
                    isBenchmarkRunning = isBenchmarkRunning,
                    onOpenSystemFiles = { openSystemFilesApp() },
                    onOpenBatterySettings = { container.oemBatteryHelper.openBestBatterySettings() },
                    onRunBenchmark = { runtime.runNativeBenchmark() },
                    headerContent = header
                )
            }

            FloatingGlassDock(
                currentTab = currentTab,
                onSelectTab = { navigateTo(it) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            if (showQrScanner) {
                QrScannerOverlay(
                    onQrScanned = { raw ->
                        showQrScanner = false
                        QrPairingUri.parse(raw).onSuccess { parsed ->
                            container.discoveryOrchestrator.registerFromQrUri(parsed)
                            val existing = trustedPeers.firstOrNull { it.nodeId == parsed.nodeId }
                            if (existing != null) {
                                runtime.reconnectTrustedPeer(
                                    existing.copy(lastKnownHost = parsed.host, lastKnownPort = parsed.port)
                                )
                            } else {
                                runtime.initiateHandshake(parsed.host, parsed.port, parsed.nodeId)
                            }
                        }.onFailure { err ->
                            runtime.pairingStatusMessage.value = "Invalid QR code: ${err.message}"
                        }
                    },
                    onClose = { showQrScanner = false }
                )
            }

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
