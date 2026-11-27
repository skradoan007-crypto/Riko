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
import com.rikodev.sync.nexmesh.core.compute.codec.HardwareCodecCapabilities
import com.rikodev.sync.nexmesh.core.compute.engine.SegmentTaskStatus
import com.rikodev.sync.nexmesh.core.compute.engine.SplitComputeSnapshot
import com.rikodev.sync.nexmesh.feature.ui.components.GlassCard
import com.rikodev.sync.nexmesh.feature.ui.components.KineticSquareSlider
import com.rikodev.sync.nexmesh.feature.ui.components.LiquidGlassPillButton
import com.rikodev.sync.nexmesh.feature.ui.components.RikoSignatureSwitch
import com.rikodev.sync.nexmesh.feature.ui.components.RikoSwitchState
import com.rikodev.sync.nexmesh.feature.ui.theme.NexMeshColors
import com.rikodev.sync.nexmesh.feature.ui.theme.liquidGlassSurface

@Composable
fun SplitComputeScreen(
    capabilities: HardwareCodecCapabilities,
    snapshot: SplitComputeSnapshot,
    selectedVideoName: String?,
    hwAccelSwitchState: RikoSwitchState,
    onToggleHwAccel: (Boolean) -> Unit,
    onPickVideo: () -> Unit,
    onStartCompute: () -> Unit,
    headerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
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
                text = "ভিডিও প্রসেসিং",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = NexMeshColors.InkPrimary
            )
            Text(
                text = "বড় ভিডিও অংশে ভাগ করে একাধিক ফোনের শক্তি একসাথে কাজে লাগান",
                fontSize = 12.sp,
                color = NexMeshColors.InkSecondary
            )
        }

        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "ডিভাইসের ভিডিও সক্ষমতা",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NexMeshColors.InkPrimary
                )
                Text(
                    text = if (capabilities.hasHardwareH264Encoder) {
                        "স্ট্যান্ডার্ড HD ভিডিও (H.264): হার্ডওয়্যার সাপোর্ট আছে"
                    } else {
                        "স্ট্যান্ডার্ড HD ভিডিও (H.264): হার্ডওয়্যার সাপোর্ট নেই"
                    },
                    fontSize = 12.sp,
                    color = if (capabilities.hasHardwareH264Encoder) NexMeshColors.StatusHealthy else NexMeshColors.StatusWarning
                )
                Text(
                    text = if (capabilities.hasHardwareHevcEncoder) {
                        "হাই-এফিশিয়েন্সি ভিডিও (HEVC): হার্ডওয়্যার সাপোর্ট আছে"
                    } else {
                        "হাই-এফিশিয়েন্সি ভিডিও (HEVC): এই ডিভাইসে সাপোর্ট নেই"
                    },
                    fontSize = 12.sp,
                    color = if (capabilities.hasHardwareHevcEncoder) NexMeshColors.StatusHealthy else NexMeshColors.InkSecondary
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
                    text = "হার্ডওয়্যার এক্সেলারেশন",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NexMeshColors.InkPrimary
                )
                Text(
                    text = "ফোনের চিপ ব্যবহার করে দ্রুত ও কম ব্যাটারিতে প্রসেস করবে",
                    fontSize = 11.sp,
                    color = NexMeshColors.InkSecondary
                )
            }
            RikoSignatureSwitch(
                state = hwAccelSwitchState,
                onToggleRequest = onToggleHwAccel
            )
        }

        KineticSquareSlider(progress = snapshot.progressFraction)

        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = selectedVideoName ?: "কোনো MP4 ভিডিও নির্বাচন করা হয়নি",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NexMeshColors.InkPrimary
                )
                Text(
                    text = if (snapshot.statusMessage == "Idle") {
                        "প্রসেস শুরু করতে আগে আপনার গ্যালারি থেকে একটি MP4 ভিডিও বেছে নিন"
                    } else {
                        snapshot.statusMessage
                    },
                    fontSize = 12.sp,
                    color = NexMeshColors.InkSecondary
                )
                if (!snapshot.finalSha256.isNullOrBlank()) {
                    Text(
                        text = "ভিডিও সফলভাবে জোড়া লাগানো ও যাচাই করা হয়েছে",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = NexMeshColors.StatusHealthy
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LiquidGlassPillButton(
                        text = "ভিডিও বেছে নিন",
                        onClick = onPickVideo,
                        isPrimary = false,
                        modifier = Modifier.weight(1f)
                    )
                    LiquidGlassPillButton(
                        text = if (snapshot.isRunning) "চলছে..." else "প্রসেস শুরু করুন",
                        onClick = onStartCompute,
                        isPrimary = true,
                        enabled = !snapshot.isRunning && selectedVideoName != null,
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }
        }

        if (snapshot.segmentTelemetry.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "ভাগ করা কাজের অবস্থা (পুনরায় বরাদ্দ: ${snapshot.reassignedCount})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NexMeshColors.InkPrimary
                    )
                    snapshot.segmentTelemetry.forEach { seg ->
                        val color = when (seg.status) {
                            SegmentTaskStatus.COMPLETED -> NexMeshColors.StatusHealthy
                            SegmentTaskStatus.REASSIGNED -> NexMeshColors.StatusWarning
                            SegmentTaskStatus.FAILED -> NexMeshColors.StatusCritical
                            else -> NexMeshColors.InkSecondary
                        }
                        Text(
                            text = "অংশ #${seg.segmentIndex + 1} • ${seg.assignedNodeName} • ${seg.message}",
                            fontSize = 12.sp,
                            color = color
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(96.dp).navigationBarsPadding())
    }
}
