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
import com.rikodev.sync.nexmesh.core.network.transfer.TransferProgressSnapshot
import com.rikodev.sync.nexmesh.core.network.transfer.TransferState
import com.rikodev.sync.nexmesh.feature.ui.components.GlassCard
import com.rikodev.sync.nexmesh.feature.ui.components.KineticSquareSlider
import com.rikodev.sync.nexmesh.feature.ui.components.LiquidGlassPillButton
import com.rikodev.sync.nexmesh.feature.ui.components.RikoSignatureSwitch
import com.rikodev.sync.nexmesh.feature.ui.components.RikoSwitchState
import com.rikodev.sync.nexmesh.feature.ui.theme.NexMeshColors
import com.rikodev.sync.nexmesh.feature.ui.theme.liquidGlassSurface

@Composable
fun TransferScreen(
    snapshot: TransferProgressSnapshot,
    selectedFileName: String?,
    hasActiveSession: Boolean,
    lz4SwitchState: RikoSwitchState,
    onToggleLz4: (Boolean) -> Unit,
    onPickSafFile: () -> Unit,
    onPickSafFolder: () -> Unit,
    onStartOrResumeTransfer: () -> Unit,
    onPauseTransfer: () -> Unit,
    headerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val sizeMb = snapshot.totalBytes / (1024.0 * 1024.0)
    val chunkMb = snapshot.logicalChunkSizeBytes / (1024 * 1024)
    val ioKb = snapshot.ioBufferSizeBytes / 1024

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        headerContent()

        Text(
            text = "Streaming Transfer",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = NexMeshColors.InkPrimary
        )

        // File Info Glass Card
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = selectedFileName ?: snapshot.fileName.ifBlank { "No SAF file selected" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NexMeshColors.InkPrimary
                )
                Text(
                    text = if (snapshot.totalBytes > 0) {
                        String.format("Size: %.2f MB • Chunk %d/%d • %s", sizeMb, snapshot.currentChunk, snapshot.totalChunks, snapshot.state.name)
                    } else {
                        "Select a file via Storage Access Framework (Zero MANAGE_EXTERNAL_STORAGE)"
                    },
                    fontSize = 12.sp,
                    color = NexMeshColors.InkSecondary
                )
                if (snapshot.sha256Verified) {
                    Text(
                        text = "SHA-256 Chunk Integrity Verified • ${String.format("%.1f MB/s", snapshot.throughputMbps)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = NexMeshColors.StatusHealthy
                    )
                }
                if (!snapshot.errorMessage.isNullOrBlank()) {
                    Text(
                        text = snapshot.errorMessage!!,
                        fontSize = 12.sp,
                        color = NexMeshColors.StatusCritical
                    )
                }
            }
        }

        // Red-marked spec: Kinetic Square Slider with live-rotating squircle thumb
        KineticSquareSlider(progress = snapshot.progressFraction)

        // Stacked Info Pills
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 50.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${chunkMb}MB Chunks • ${ioKb}KB Buffer • AES-256-GCM",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = NexMeshColors.InkPrimary
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlassSurface(shape = RoundedCornerShape(50))
                .padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LZ4 Native C++ Engine",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = NexMeshColors.InkPrimary
            )
            RikoSignatureSwitch(
                state = lz4SwitchState,
                onToggleRequest = onToggleLz4
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LiquidGlassPillButton(
                text = "Pick File (SAF)",
                onClick = onPickSafFile,
                isPrimary = false,
                modifier = Modifier.weight(1f)
            )
            LiquidGlassPillButton(
                text = "Mount Folder",
                onClick = onPickSafFolder,
                isPrimary = false,
                modifier = Modifier.weight(1f)
            )
        }

        val isTransferring = snapshot.state == TransferState.TRANSFERRING || snapshot.state == TransferState.PREPARING
        LiquidGlassPillButton(
            text = when {
                isTransferring -> "Pause Transfer"
                snapshot.state == TransferState.PAUSED -> "Resume Transfer"
                else -> "Start Encrypted Stream"
            },
            onClick = { if (isTransferring) onPauseTransfer() else onStartOrResumeTransfer() },
            isPrimary = true,
            enabled = hasActiveSession && (selectedFileName != null || snapshot.fileName.isNotBlank()),
            modifier = Modifier.fillMaxWidth()
        )

        if (!hasActiveSession) {
            Text(
                text = "Pair and approve a peer node first to establish an E2EE session.",
                fontSize = 12.sp,
                color = NexMeshColors.InkSecondary
            )
        }

        Spacer(modifier = Modifier.height(92.dp))
    }
}
