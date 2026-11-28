package com.rikodev.sync.nexmesh.feature.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.sync.nexmesh.core.protocol.model.RemoteVaultFileItem
import com.rikodev.sync.nexmesh.core.protocol.model.RemoteVaultListResponse
import com.rikodev.sync.nexmesh.core.vdfs.storage.SharedStorageEntry
import com.rikodev.sync.nexmesh.feature.ui.components.GlassCard
import com.rikodev.sync.nexmesh.feature.ui.components.LiquidGlassPillButton
import com.rikodev.sync.nexmesh.feature.ui.theme.LocalNexMeshPalette

@Composable
fun StorageScreen(
    connectedStoragePhoneName: String?,
    remoteVault: RemoteVaultListResponse?,
    isInsideRemoteSubfolder: Boolean,
    isLoadingRemoteVault: Boolean,
    sharedFolders: List<SharedStorageEntry>,
    sharedFiles: List<SharedStorageEntry>,
    statusMessage: String?,
    onRefreshRemoteStorage: () -> Unit,
    onNavigateRemoteBack: () -> Unit,
    onOpenRemoteFolder: (RemoteVaultFileItem) -> Unit,
    onStreamOpenRemoteFile: (RemoteVaultFileItem) -> Unit,
    onOpenSystemFiles: () -> Unit,
    onSelectFolder: () -> Unit,
    onSelectFiles: () -> Unit,
    onRemoveSharedEntry: (String) -> Unit,
    headerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalNexMeshPalette.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        headerContent()

        Column {
            Text(
                text = "Shared Storage",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = p.inkPrimary
            )
            Text(
                text = "Stream remote files live without copying them to this phone",
                fontSize = 12.sp,
                color = p.inkSecondary
            )
        }

        // Section 1: Live Remote Storage Phone Browser
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp, elevated = true) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (connectedStoragePhoneName != null) {
                                "Remote Storage • $connectedStoragePhoneName"
                            } else {
                                "Remote Storage"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = p.inkPrimary
                        )
                        Text(
                            text = when {
                                connectedStoragePhoneName == null -> "Connect a Storage Phone in Devices to browse live folders"
                                remoteVault?.mounted == true -> remoteVault.vaultName ?: "Live Network Folders"
                                else -> "No folders shared on $connectedStoragePhoneName yet"
                            },
                            fontSize = 12.sp,
                            color = p.inkSecondary
                        )
                    }
                    LiquidGlassPillButton(
                        text = if (isLoadingRemoteVault) "Loading..." else "Refresh",
                        onClick = onRefreshRemoteStorage,
                        isPrimary = true,
                        enabled = connectedStoragePhoneName != null && !isLoadingRemoteVault
                    )
                }

                if (connectedStoragePhoneName != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isInsideRemoteSubfolder) {
                            LiquidGlassPillButton(
                                text = "Back",
                                onClick = onNavigateRemoteBack,
                                isPrimary = false
                            )
                        }
                        LiquidGlassPillButton(
                            text = "Open in Android Files",
                            onClick = onOpenSystemFiles,
                            isPrimary = false,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val items = remoteVault?.files.orEmpty()
                    if (items.isEmpty()) {
                        Text(
                            text = "On your Storage Phone, open the Storage tab and tap 'Select Folder' or 'Select Files' to share.",
                            fontSize = 12.sp,
                            color = p.inkSecondary
                        )
                    } else {
                        items.forEach { item ->
                            val sizeMb = item.sizeBytes / (1024.0 * 1024.0)
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (item.isDirectory) onOpenRemoteFolder(item) else onStreamOpenRemoteFile(item)
                                    },
                                cornerRadius = 16.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (item.isDirectory) Icons.Rounded.Folder else Icons.Rounded.Description,
                                            contentDescription = null,
                                            tint = if (item.isDirectory) p.accentPrimary else p.inkSecondary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Column {
                                            Text(
                                                text = item.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = p.inkPrimary
                                            )
                                            Text(
                                                text = if (item.isDirectory) "Live Remote Folder" else String.format("%.2f MB • Live Stream", sizeMb),
                                                fontSize = 11.sp,
                                                color = p.inkSecondary
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (item.isDirectory) "Open" else "Stream",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = p.inkAccent
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!statusMessage.isNullOrBlank()) {
            Text(
                text = statusMessage,
                fontSize = 12.sp,
                color = p.inkAccent,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // Section 2: Selective Sharing From This Phone (Select Folder & Select Files)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LiquidGlassPillButton(
                text = "Select Folder",
                onClick = onSelectFolder,
                isPrimary = true,
                modifier = Modifier.weight(1f)
            )
            LiquidGlassPillButton(
                text = "Select Files",
                onClick = onSelectFiles,
                isPrimary = false,
                modifier = Modifier.weight(1f)
            )
        }

        // Shared Folders List
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Shared Folders (${sharedFolders.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = p.inkPrimary
                )
                if (sharedFolders.isEmpty()) {
                    Text(
                        text = "No folders selected yet. Tap 'Select Folder' to choose a folder on this phone to share with your Main Phone.",
                        fontSize = 12.sp,
                        color = p.inkSecondary
                    )
                } else {
                    sharedFolders.forEach { entry ->
                        SharedEntryRow(
                            entry = entry,
                            onRemove = { onRemoveSharedEntry(entry.id) }
                        )
                    }
                }
            }
        }

        // Shared Files List
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Shared Files (${sharedFiles.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = p.inkPrimary
                )
                if (sharedFiles.isEmpty()) {
                    Text(
                        text = "No individual files selected. Tap 'Select Files' to share specific files without exposing whole folders.",
                        fontSize = 12.sp,
                        color = p.inkSecondary
                    )
                } else {
                    sharedFiles.forEach { entry ->
                        SharedEntryRow(
                            entry = entry,
                            onRemove = { onRemoveSharedEntry(entry.id) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(98.dp).navigationBarsPadding())
    }
}

@Composable
private fun SharedEntryRow(
    entry: SharedStorageEntry,
    onRemove: () -> Unit
) {
    val p = LocalNexMeshPalette.current
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (entry.isFolder) Icons.Rounded.Folder else Icons.Rounded.Description,
                    contentDescription = null,
                    tint = if (entry.isFolder) p.accentPrimary else p.inkSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = entry.displayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = p.inkPrimary
                    )
                    Text(
                        text = if (entry.isFolder) {
                            "Original Device • Access Granted"
                        } else {
                            String.format("%.2f MB • Original File", entry.sizeBytes / (1024.0 * 1024.0))
                        },
                        fontSize = 11.sp,
                        color = p.statusOnline
                    )
                }
            }
            LiquidGlassPillButton(
                text = "Remove",
                onClick = onRemove,
                isPrimary = false
            )
        }
    }
}
