package com.rikodev.sync.nexmesh.feature.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.sync.nexmesh.core.vdfs.entity.SwarmNodeEntity
import com.rikodev.sync.nexmesh.core.vdfs.health.FileAvailabilityState
import com.rikodev.sync.nexmesh.core.vdfs.health.VirtualFileHealthReport
import com.rikodev.sync.nexmesh.core.vdfs.storage.MountedSafVault
import com.rikodev.sync.nexmesh.feature.ui.components.GlassCard
import com.rikodev.sync.nexmesh.feature.ui.components.KineticSquareSlider
import com.rikodev.sync.nexmesh.feature.ui.components.LiquidGlassPillButton
import com.rikodev.sync.nexmesh.feature.ui.components.RikoSignatureSwitch
import com.rikodev.sync.nexmesh.feature.ui.components.RikoSwitchState
import com.rikodev.sync.nexmesh.feature.ui.theme.NexMeshColors
import com.rikodev.sync.nexmesh.feature.ui.theme.liquidGlassSurface

/**
 * Phase 2 — Virtual Distributed File System (VDFS) Browser Screen.
 * Pure Light Liquid Glass UI matching the approved Nex Mesh design language.
 */
@Composable
fun VirtualDriveScreen(
    mountedVault: MountedSafVault?,
    swarmNodes: List<SwarmNodeEntity>,
    files: List<VirtualFileHealthReport>,
    redundancy2xSwitchState: RikoSwitchState,
    vdfsStatusMessage: String?,
    onToggle2xRedundancy: (Boolean) -> Unit,
    onMountSafFolder: () -> Unit,
    onIngestFileToVdfs: () -> Unit,
    onDeleteFile: (String) -> Unit,
    headerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalPoolBytes = swarmNodes.sumOf { it.totalBytes }.coerceAtLeast(1L)
    val freePoolBytes = swarmNodes.sumOf { it.freeBytes }.coerceAtLeast(0L)
    val usedFraction = ((totalPoolBytes - freePoolBytes).toFloat() / totalPoolBytes.toFloat()).coerceIn(0f, 1f)
    val freeGb = freePoolBytes / (1024.0 * 1024.0 * 1024.0)
    val totalGb = totalPoolBytes / (1024.0 * 1024.0 * 1024.0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        headerContent()

        Text(
            text = "Virtual Distributed Drive",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = NexMeshColors.InkPrimary
        )

        // Swarm Storage Pool Summary + KineticSquareSlider usage meter
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Swarm Cloud Pool (${swarmNodes.size} Nodes)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NexMeshColors.InkPrimary
                )
                Text(
                    text = String.format("%.2f GB Free of %.2f GB Total", freeGb, totalGb),
                    fontSize = 12.sp,
                    color = NexMeshColors.InkSecondary
                )
                KineticSquareSlider(progress = usedFraction)
                Text(
                    text = if (mountedVault != null) {
                        "SAF Vault: ${mountedVault.displayName}"
                    } else {
                        "No SAF folder mounted yet (ACTION_OPEN_DOCUMENT_TREE)"
                    },
                    fontSize = 12.sp,
                    color = NexMeshColors.InkAccentIndigo
                )
            }
        }

        // Optional 2x Redundancy Toggle with Riko Signature Switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlassSurface(
                    shape = RoundedCornerShape(50),
                    topColor = NexMeshColors.MintPillStart,
                    bottomColor = NexMeshColors.MintPillEnd,
                    elevation = 5.dp
                )
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "2x Redundancy Mirror",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NexMeshColors.InkPrimary
                )
                Text(
                    text = "Replicates chunks across 2 distinct nodes",
                    fontSize = 11.sp,
                    color = NexMeshColors.InkSecondary
                )
            }
            RikoSignatureSwitch(
                state = redundancy2xSwitchState,
                onToggleRequest = onToggle2xRedundancy
            )
        }

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LiquidGlassPillButton(
                text = "Mount SAF Vault",
                onClick = onMountSafFolder,
                isPrimary = false,
                modifier = Modifier.weight(1f)
            )
            LiquidGlassPillButton(
                text = "Upload to VDFS",
                onClick = onIngestFileToVdfs,
                isPrimary = true,
                modifier = Modifier.weight(1f)
            )
        }

        if (!vdfsStatusMessage.isNullOrBlank()) {
            Text(
                text = vdfsStatusMessage,
                fontSize = 12.sp,
                color = NexMeshColors.InkAccentIndigo
            )
        }

        // Committed Virtual Files List
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Stored Virtual Files (${files.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NexMeshColors.InkPrimary
                )

                if (files.isEmpty()) {
                    Text(
                        text = "Your Virtual Distributed Drive is empty. Tap 'Upload to VDFS' to automatically chunk and distribute a file.",
                        fontSize = 12.sp,
                        color = NexMeshColors.InkSecondary
                    )
                } else {
                    files.forEach { report ->
                        val statusColor = when (report.availability) {
                            FileAvailabilityState.HEALTHY -> NexMeshColors.StatusHealthy
                            FileAvailabilityState.DEGRADED -> NexMeshColors.StatusWarning
                            FileAvailabilityState.UNAVAILABLE -> NexMeshColors.StatusCritical
                        }
                        val sizeMb = report.file.totalSize / (1024.0 * 1024.0)

                        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = report.file.originalName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NexMeshColors.InkPrimary
                                    )
                                    Text(
                                        text = String.format(
                                            "%.2f MB • %d Chunks • %dx Replica • %d Nodes",
                                            sizeMb,
                                            report.file.chunkCount,
                                            report.file.replicationFactor,
                                            report.hostingNodeCount
                                        ),
                                        fontSize = 11.sp,
                                        color = NexMeshColors.InkSecondary
                                    )
                                    Text(
                                        text = "${report.availability.name} (${report.onlineChunkCount}/${report.file.chunkCount} chunks online)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = statusColor
                                    )
                                }
                                LiquidGlassPillButton(
                                    text = "Delete",
                                    onClick = { onDeleteFile(report.file.fileId) },
                                    isPrimary = false
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(92.dp))
    }
}
