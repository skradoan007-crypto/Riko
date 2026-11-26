package com.rikodev.sync.nexmesh.feature.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.sync.nexmesh.core.protocol.session.PendingPairingChallenge
import com.rikodev.sync.nexmesh.feature.ui.components.GlassCard
import com.rikodev.sync.nexmesh.feature.ui.components.LiquidGlassPillButton
import com.rikodev.sync.nexmesh.feature.ui.components.QrCodeGenerator
import com.rikodev.sync.nexmesh.feature.ui.components.RikoSignatureSwitch
import com.rikodev.sync.nexmesh.feature.ui.components.RikoSwitchState
import com.rikodev.sync.nexmesh.feature.ui.components.SasVerificationCard
import com.rikodev.sync.nexmesh.feature.ui.theme.NexMeshColors
import com.rikodev.sync.nexmesh.feature.ui.theme.liquidGlassSurface

@Composable
fun PairingScreen(
    localQrUri: String,
    nodeServerSwitchState: RikoSwitchState,
    onToggleNodeServer: (Boolean) -> Unit,
    pendingChallenge: PendingPairingChallenge?,
    onApproveSas: (String) -> Unit,
    onRejectSas: (String) -> Unit,
    onPairWithUri: (String) -> Unit,
    pairingStatusMessage: String?,
    headerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val qrBitmap = remember(localQrUri) { QrCodeGenerator.generate(localQrUri, 440) }
    var manualUriInput by remember { mutableStateOf("") }

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
            text = "Secure Pairing",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = NexMeshColors.InkPrimary
        )

        // QR Code Card with ECDH P-256 Pill Header
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 26.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassSurface(
                            shape = RoundedCornerShape(50),
                            topColor = NexMeshColors.LavenderCtaStart,
                            bottomColor = NexMeshColors.LavenderCtaEnd,
                            elevation = 3.dp
                        )
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ECDH P-256 • AES-256-GCM",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NexMeshColors.InkAccentIndigo
                    )
                }

                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "Nex Mesh Node QR Code",
                        modifier = Modifier.size(196.dp)
                    )
                } else {
                    Text(
                        text = "Connect to Wi-Fi / Hotspot to generate Node QR",
                        fontSize = 12.sp,
                        color = NexMeshColors.InkSecondary
                    )
                }
            }
        }

        // Pink-marked spec: Pure Light Luminous Mint Glass Pill (zero dark top bevel) + Riko Signature Switch
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
                text = "Node Server Active",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = NexMeshColors.InkPrimary
            )
            RikoSignatureSwitch(
                state = nodeServerSwitchState,
                onToggleRequest = onToggleNodeServer
            )
        }

        // Active SAS Verification Card (if handshake in progress)
        if (pendingChallenge != null) {
            SasVerificationCard(
                peerName = pendingChallenge.peerDeviceName,
                sasCode = pendingChallenge.sasCode,
                onApprove = { onApproveSas(pendingChallenge.pairingToken) },
                onReject = { onRejectSas(pendingChallenge.pairingToken) }
            )
        }

        // Manual / Direct QR URI Entry (Tier 3)
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Direct URI Pairing (IPv4 / IPv6)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NexMeshColors.InkPrimary
                )
                OutlinedTextField(
                    value = manualUriInput,
                    onValueChange = { manualUriInput = it },
                    placeholder = { Text("riko-swarm://192.168.x.x:47780?pub=...&id=...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LiquidGlassPillButton(
                    text = "Initiate ECDH Handshake",
                    onClick = { onPairWithUri(manualUriInput) },
                    isPrimary = true,
                    enabled = manualUriInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (!pairingStatusMessage.isNullOrBlank()) {
                    Text(
                        text = pairingStatusMessage,
                        fontSize = 12.sp,
                        color = NexMeshColors.InkAccentIndigo
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(92.dp))
    }
}
