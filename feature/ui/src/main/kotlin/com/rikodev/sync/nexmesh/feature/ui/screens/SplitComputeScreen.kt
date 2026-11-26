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

/**
 * Phase 3 — Distributed Split-Compute Screen (Pure Light Liquid Glass UI).
 */
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

        Text(
            text = "Distributed Split-Compute",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = NexMeshColors.InkPrimary
        )

        // Hardware MediaCodec Capability Card
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Hardware MediaCodec Profile",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NexMeshColors.InkPrimary
                )
                Text(
                    text = "H.264 HW Encoder: ${capabilities.preferredH264EncoderName ?: "Unsupported"}",
                    fontSize = 12.sp,
                    color = if (capabilities.hasHardwareH264Encoder) NexMeshColors.StatusHealthy else NexMeshColors.StatusWarning
                )
                Text(
                    text = "HEVC HW Encoder: ${capabilities.preferredHevcEncoderName ?: "Unsupported"}",
                    fontSize = 12.sp,
                    color = if (capabilities.hasHardwareHevcEncoder) NexMeshColors.StatusHealthy else NexMeshColors.InkSecondary
                )
            }
        }

        // Hardware Acceleration Switch using RikoSignatureSwitch
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
            Text(
                text = "Hardware MediaCodec Pipeline",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = NexMeshColors.InkPrimary
            )
            RikoSignatureSwitch(
                state = hwAccelSwitchState,
                onToggleRequest = onToggleHwAccel
            )
        }

        // Live Progress via KineticSquareSlider
        KineticSquareSlider(progress = snapshot.progressFraction)

        // Video Selection & Launch
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = selectedVideoName ?: "No MP4 video selected",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NexMeshColors.InkPrimary
                )
                Text(
                    text = snapshot.statusMessage,
                    fontSize = 12.sp,
                    color = NexMeshColors.InkSecondary
                )
                if (!snapshot.finalSha256.isNullOrBlank()) {
                    Text(
                        text = "Reassembled SHA-256: ${snapshot.finalSha256!!.take(24)}...",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = NexMeshColors.StatusHealthy
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LiquidGlassPillButton(
                        text = "Pick MP4",
                        onClick = onPickVideo,
                        isPrimary = false,
                        modifier = Modifier.weight(1f)
                    )
                    LiquidGlassPillButton(
                        text = if (snapshot.isRunning) "Processing..." else "Split & Compute",
                        onClick = onStartCompute,
                        isPrimary = true,
                        enabled = !snapshot.isRunning && selectedVideoName != null,
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }
        }

        // Per-Segment Telemetry & Failover Reassignment Feed
        if (snapshot.segmentTelemetry.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Segment Workers (Reassigned: ${snapshot.reassignedCount})",
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
                            text = "Seg #${seg.segmentIndex} • ${seg.assignedNodeName} • ${seg.status.name} (${seg.message})",
                            fontSize = 12.sp,
                            color = color
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(92.dp))
    }
}
