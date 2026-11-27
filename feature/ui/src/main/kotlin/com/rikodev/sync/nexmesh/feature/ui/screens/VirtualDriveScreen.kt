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

        Column {
            Text(
                text = "শেয়ার্ড ক্লাউড ড্রাইভ",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = NexMeshColors.InkPrimary
            )
            Text(
                text = "সংযুক্ত সব ফোনের খালি জায়গা একসাথে প্রাইভেট ক্লাউড হিসেবে ব্যবহার করুন",
                fontSize = 12.sp,
                color = NexMeshColors.InkSecondary
            )
        }

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
                        "নির্বাচিত ফোল্ডার: ${mountedVault.displayName}"
                    } else {
                        "এখনো কোনো স্টোরেজ ফোল্ডার যুক্ত করা হয়নি। নিচের বাটনে ট্যাপ করে একটি ফোল্ডার বেছে নিন।"
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

        if (!vdfsStatusMessage.isNullOrBlank()) {
            Text(
                text = vdfsStatusMessage,
                fontSize = 12.sp,
                color = NexMeshColors.InkAccentIndigo
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
                    text = "সংরক্ষিত ফাইলসমূহ (${files.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NexMeshColors.InkPrimary
                )

                if (files.isEmpty()) {
                    Text(
                        text = "আপনার ড্রাইভ এখনো খালি। যেকোনো ফাইল সংযুক্ত ফোনগুলোতে ভাগ করে নিরাপদে রাখতে 'ড্রাইভে রাখুন' বাটনে ট্যাপ করুন।",
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
