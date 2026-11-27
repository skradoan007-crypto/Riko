package com.rikodev.sync.nexmesh.feature.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import com.rikodev.sync.nexmesh.core.protocol.model.RemoteVaultFileItem
import com.rikodev.sync.nexmesh.core.protocol.model.RemoteVaultListResponse
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

@Composable
fun VirtualDriveScreen(
    mountedVault: MountedSafVault?,
    swarmNodes: List<SwarmNodeEntity>,
    files: List<VirtualFileHealthReport>,
    hasActiveSession: Boolean,
    connectedPeerName: String?,
    remoteVault: RemoteVaultListResponse?,
    isLoadingRemoteVault: Boolean,
    isDownloadingRemoteFile: Boolean,
    remoteDownloadProgress: Float,
    redundancy2xSwitchState: RikoSwitchState,
    vdfsStatusMessage: String?,
    onToggle2xRedundancy: (Boolean) -> Unit,
    onMountSafFolder: () -> Unit,
    onIngestFileToVdfs: () -> Unit,
    onRefreshRemoteVault: () -> Unit,
    onDownloadRemoteFile: (RemoteVaultFileItem) -> Unit,
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

        Column {
            Text(
                text = "শেয়ার্ড ক্লাউড ড্রাইভ",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = NexMeshColors.InkPrimary
            )
            Text(
                text = "সংযুক্ত ফোনের শেয়ার করা ফোল্ডার ব্রাউজ ও ডাউনলোড করুন",
                fontSize = 12.sp,
                color = NexMeshColors.InkSecondary
            )
        }

        // Connected Remote Node Shared Folder Browser (Controller -> Node Folder Access!)
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (connectedPeerName != null) {
                                "সংযুক্ত নোডের ফোল্ডার ($connectedPeerName)"
                            } else {
                                "সংযুক্ত নোডের ফোল্ডার"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NexMeshColors.InkPrimary
                        )
                        Text(
                            text = when {
                                !hasActiveSession -> "আগে অন্য ফোনের সাথে পেয়ার ও অনুমোদন করুন"
                                remoteVault?.mounted == true -> "ফোল্ডার: ${remoteVault.vaultName} (${remoteVault.files.size}টি ফাইল)"
                                else -> "নোড ফোনে 'ফোল্ডার যুক্ত করুন' চাপার পর রিফ্রেশ দিন"
                            },
                            fontSize = 11.sp,
                            color = NexMeshColors.InkSecondary
                        )
                    }
                    LiquidGlassPillButton(
                        text = if (isLoadingRemoteVault) "লোড..." else "রিফ্রেশ",
                        onClick = onRefreshRemoteVault,
                        isPrimary = true,
                        enabled = hasActiveSession && !isLoadingRemoteVault
                    )
                }

                if (isDownloadingRemoteFile) {
                    KineticSquareSlider(progress = remoteDownloadProgress)
                }

                if (hasActiveSession && remoteVault?.mounted == true) {
                    if (remoteVault.files.isEmpty()) {
                        Text(
                            text = "নোড ফোনের নির্বাচিত ফোল্ডারে কোনো ফাইল নেই।",
                            fontSize = 12.sp,
                            color = NexMeshColors.InkSecondary
                        )
                    } else {
                        remoteVault.files.forEach { item ->
                            val sizeMb = item.sizeBytes / (1024.0 * 1024.0)
                            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            text = item.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = NexMeshColors.InkPrimary
                                        )
                                        Text(
                                            text = String.format("%.2f MB • %s", sizeMb, item.mimeType),
                                            fontSize = 11.sp,
                                            color = NexMeshColors.InkSecondary
                                        )
                                    }
                                    LiquidGlassPillButton(
                                        text = "ডাউনলোড",
                                        onClick = { onDownloadRemoteFile(item) },
                                        isPrimary = true,
                                        enabled = !isDownloadingRemoteFile
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!vdfsStatusMessage.isNullOrBlank()) {
            Text(
                text = vdfsStatusMessage,
                fontSize = 12.sp,
                color = NexMeshColors.InkAccentIndigo
            )
        }

        // Local Storage Pool + Mounted SAF Folder
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "সম্মিলিত স্টোরেজ পুল (${swarmNodes.size}টি ডিভাইস)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NexMeshColors.InkPrimary
                )
                Text(
                    text = String.format("মোট %.2f GB এর মধ্যে %.2f GB খালি আছে", totalGb, freeGb),
                    fontSize = 12.sp,
                    color = NexMeshColors.InkSecondary
                )
                KineticSquareSlider(progress = usedFraction)
                Text(
                    text = if (mountedVault != null) {
                        "এই ফোনের শেয়ার করা ফোল্ডার: ${mountedVault.displayName}"
                    } else {
                        "এই ফোনে এখনো কোনো ফোল্ডার যুক্ত করা হয়নি (ফাইল শেয়ার বা সেভ করতে ফোল্ডার বেছে নিন)"
                    },
                    fontSize = 12.sp,
                    color = NexMeshColors.InkAccentIndigo
                )
            }
        }

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
            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                Text(
                    text = "ডাবল ব্যাকআপ সুরক্ষা",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NexMeshColors.InkPrimary
                )
                Text(
                    text = "একই ফাইল ২টি আলাদা ফোনে কপি রাখবে, যাতে একটি বন্ধ থাকলেও ফাইল পাওয়া যায়",
                    fontSize = 11.sp,
                    color = NexMeshColors.InkSecondary
                )
            }
            RikoSignatureSwitch(
                state = redundancy2xSwitchState,
                onToggleRequest = onToggle2xRedundancy
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LiquidGlassPillButton(
                text = "ফোল্ডার যুক্ত করুন",
                onClick = onMountSafFolder,
                isPrimary = false,
                modifier = Modifier.weight(1f)
            )
            LiquidGlassPillButton(
                text = "ড্রাইভে রাখুন",
                onClick = onIngestFileToVdfs,
                isPrimary = true,
                modifier = Modifier.weight(1f)
            )
        }

        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "ড্রাইভে সংরক্ষিত ফাইল (${files.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NexMeshColors.InkPrimary
                )

                if (files.isEmpty()) {
                    Text(
                        text = "যেকোনো ফাইল সংযুক্ত ফোনগুলোতে ভাগ করে নিরাপদে রাখতে 'ড্রাইভে রাখুন' বাটনে ট্যাপ করুন।",
                        fontSize = 12.sp,
                        color = NexMeshColors.InkSecondary
                    )
                } else {
                    files.forEach { report ->
                        val (statusText, statusColor) = when (report.availability) {
                            FileAvailabilityState.HEALTHY -> "সম্পূর্ণ প্রস্তুত" to NexMeshColors.StatusHealthy
                            FileAvailabilityState.DEGRADED -> "আংশিক ব্যাকআপ" to NexMeshColors.StatusWarning
                            FileAvailabilityState.UNAVAILABLE -> "ডিভাইস অফলাইন" to NexMeshColors.StatusCritical
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
                                            "%.2f MB • %d ভাগ • %dটি ডিভাইসে",
                                            sizeMb,
                                            report.file.chunkCount,
                                            report.hostingNodeCount
                                        ),
                                        fontSize = 11.sp,
                                        color = NexMeshColors.InkSecondary
                                    )
                                    Text(
                                        text = "$statusText (${report.onlineChunkCount}/${report.file.chunkCount} অংশ অনলাইন)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = statusColor
                                    )
                                }
                                LiquidGlassPillButton(
                                    text = "মুছুন",
                                    onClick = { onDeleteFile(report.file.fileId) },
                                    isPrimary = false
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(96.dp).navigationBarsPadding())
    }
}
