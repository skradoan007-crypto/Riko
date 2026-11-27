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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        headerContent()

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "ফাইল ট্রান্সফার",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = NexMeshColors.InkPrimary
            )
            Text(
                text = "সংযুক্ত ফোনে নিরাপদে ও দ্রুত যেকোনো বড় ফাইল পাঠান",
                fontSize = 12.sp,
                color = NexMeshColors.InkSecondary
            )
        }

        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = selectedFileName ?: snapshot.fileName.ifBlank { "কোনো ফাইল নির্বাচন করা হয়নি" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NexMeshColors.InkPrimary
                )
                Text(
                    text = if (snapshot.totalBytes > 0) {
                        String.format("আকার: %.2f MB • অংশ %d/%d", sizeMb, snapshot.currentChunk, snapshot.totalChunks)
                    } else {
                        "পাঠানোর জন্য নিচের বাটন থেকে আপনার ফোনের যেকোনো ফাইল বেছে নিন"
                    },
                    fontSize = 12.sp,
                    color = NexMeshColors.InkSecondary
                )
                if (snapshot.sha256Verified) {
                    Text(
                        text = "ফাইলের নিরাপত্তা যাচাই সম্পন্ন • গতি: ${String.format("%.1f MB/s", snapshot.throughputMbps)}",
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

        KineticSquareSlider(progress = snapshot.progressFraction)

        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 50.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "নিরাপদ এনক্রিপ্টেড ট্রান্সফার • থেমে গেলে পুনরায় শুরু করা যাবে",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = NexMeshColors.InkPrimary
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlassSurface(shape = RoundedCornerShape(50))
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                Text(
                    text = "স্মার্ট ফাইল কম্প্রেশন",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NexMeshColors.InkPrimary
                )
                Text(
                    text = "ডেটা ছোট করে পাঠিয়ে ট্রান্সফারের গতি বাড়ায়",
                    fontSize = 11.sp,
                    color = NexMeshColors.InkSecondary
                )
            }
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
                text = "ফাইল বেছে নিন",
                onClick = onPickSafFile,
                isPrimary = false,
                modifier = Modifier.weight(1f)
            )
            LiquidGlassPillButton(
                text = "ফোল্ডার যুক্ত করুন",
                onClick = onPickSafFolder,
                isPrimary = false,
                modifier = Modifier.weight(1f)
            )
        }

        val isTransferring = snapshot.state == TransferState.TRANSFERRING || snapshot.state == TransferState.PREPARING
        LiquidGlassPillButton(
            text = when {
                isTransferring -> "পাঠানো থামান"
                snapshot.state == TransferState.PAUSED -> "পুনরায় শুরু করুন"
                else -> "ফাইল পাঠানো শুরু করুন"
            },
            onClick = { if (isTransferring) onPauseTransfer() else onStartOrResumeTransfer() },
            isPrimary = true,
            enabled = hasActiveSession && (selectedFileName != null || snapshot.fileName.isNotBlank()),
            modifier = Modifier.fillMaxWidth()
        )

        if (!hasActiveSession) {
            Text(
                text = "ফাইল পাঠাতে আগে নিচের QR আইকন থেকে অন্য একটি ফোন যুক্ত ও অনুমোদন করুন।",
                fontSize = 12.sp,
                color = NexMeshColors.InkSecondary
            )
        }

        Spacer(modifier = Modifier.height(96.dp).navigationBarsPadding())
    }
}
