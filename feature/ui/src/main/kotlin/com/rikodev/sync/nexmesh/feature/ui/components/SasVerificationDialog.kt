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
import com.rikodev.sync.nexmesh.feature.ui.theme.LocalNexMeshPalette

@Composable
fun SasVerificationCard(
    peerName: String,
    sasCode: String,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalNexMeshPalette.current
    val formattedSas = if (sasCode.length == 6) "${sasCode.take(3)} ${sasCode.drop(3)}" else sasCode
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        elevated = true
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Confirm Pairing • $peerName",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = p.inkPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Verify that this 6-digit security code matches on both phones before approving.",
                fontSize = 12.sp,
                color = p.inkSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = formattedSas,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = p.inkAccent
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LiquidGlassPillButton(
                    text = "Approve Device",
                    onClick = onApprove,
                    isPrimary = true,
                    modifier = Modifier.weight(1.2f)
                )
                LiquidGlassPillButton(
                    text = "Decline",
                    onClick = onReject,
                    isPrimary = false,
                    modifier = Modifier.weight(0.8f)
                )
            }
        }
    }
}
