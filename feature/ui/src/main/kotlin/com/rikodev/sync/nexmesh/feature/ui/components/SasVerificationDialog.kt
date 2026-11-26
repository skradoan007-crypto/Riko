package com.rikodev.sync.nexmesh.feature.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.sync.nexmesh.feature.ui.theme.NexMeshColors

@Composable
fun SasVerificationCard(
    peerName: String,
    sasCode: String,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedSas = if (sasCode.length == 6) "${sasCode.take(3)} ${sasCode.drop(3)}" else sasCode
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 24.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "SAS Verification • $peerName",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = NexMeshColors.InkSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "SAS Code: $formattedSas",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = NexMeshColors.InkPrimary
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LiquidGlassPillButton(
                    text = "Approve Peer",
                    onClick = onApprove,
                    isPrimary = true,
                    modifier = Modifier.weight(1.2f)
                )
                LiquidGlassPillButton(
                    text = "Reject",
                    onClick = onReject,
                    isPrimary = false,
                    modifier = Modifier.weight(0.8f)
                )
            }
        }
    }
}
