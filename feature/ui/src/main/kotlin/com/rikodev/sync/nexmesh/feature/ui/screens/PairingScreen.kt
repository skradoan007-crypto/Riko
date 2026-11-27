package com.rikodev.sync.nexmesh.feature.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
    var showManualPairing by remember { mutableStateOf(false) }

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
                text = "ডিভাইস যুক্ত করুন",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = NexMeshColors.InkPrimary
            )
            Text(
                text = "অন্য ফোন থেকে এই QR কোড স্ক্যান করে নিরাপদ সংযোগ তৈরি করুন",
                fontSize = 12.sp,
                color = NexMeshColors.InkSecondary
            )
        }

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
                        text = "নিরাপদ এনক্রিপ্টেড সংযোগ",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NexMeshColors.InkAccentIndigo
                    )
                }

                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "Nex Mesh QR Code",
                        modifier = Modifier.size(196.dp)
                    )
                } else {
                    Text(
                        text = "QR কোড দেখাতে আগে ফোনে Wi-Fi বা হটস্পট চালু করুন।",
                        fontSize = 12.sp,
                        color = NexMeshColors.InkSecondary
                    )
                }
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
                    text = "ডিভাইস শেয়ারিং চালু রাখুন",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NexMeshColors.InkPrimary
                )
                Text(
                    text = "অন্য ফোন যেন এই ডিভাইসটি খুঁজে পায় এবং সংযোগ করতে পারে",
                    fontSize = 11.sp,
                    color = NexMeshColors.InkSecondary
                )
            }
            RikoSignatureSwitch(
                state = nodeServerSwitchState,
                onToggleRequest = onToggleNodeServer
            )
        }

        if (pendingChallenge != null) {
            SasVerificationCard(
                peerName = pendingChallenge.peerDeviceName,
                sasCode = pendingChallenge.sasCode,
                onApprove = { onApproveSas(pendingChallenge.pairingToken) },
                onReject = { onRejectSas(pendingChallenge.pairingToken) }
            )
        }

        // Expandable Manual Pairing (keeps UI clean for regular users)
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showManualPairing = !showManualPairing },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ম্যানুয়াল লিংক দিয়ে যুক্ত করুন",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NexMeshColors.InkPrimary
                        )
                        Text(
                            text = "অ্যাডভান্সড • সরাসরি ঠিকানা পেস্ট করে সংযোগ",
                            fontSize = 11.sp,
                            color = NexMeshColors.InkSecondary
                        )
                    }
                    Text(
                        text = if (showManualPairing) "লুকান" else "খুলুন",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NexMeshColors.InkAccentIndigo
                    )
                }

                AnimatedVisibility(visible = showManualPairing) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = manualUriInput,
                            onValueChange = { manualUriInput = it },
                            placeholder = { Text("riko-swarm://192.168.x.x:47780?pub=...&id=...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        LiquidGlassPillButton(
                            text = "সংযোগ শুরু করুন",
                            onClick = { onPairWithUri(manualUriInput) },
                            isPrimary = true,
                            enabled = manualUriInput.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (!pairingStatusMessage.isNullOrBlank()) {
                    Text(
                        text = pairingStatusMessage,
                        fontSize = 12.sp,
                        color = NexMeshColors.InkAccentIndigo
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(96.dp).navigationBarsPadding())
    }
}
