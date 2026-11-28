package com.rikodev.sync.nexmesh.feature.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import com.rikodev.sync.nexmesh.core.network.discovery.DiscoveredPeer
import com.rikodev.sync.nexmesh.core.protocol.model.TrustedPeerRecord
import com.rikodev.sync.nexmesh.core.protocol.session.PendingPairingChallenge
import com.rikodev.sync.nexmesh.feature.ui.components.GlassCard
import com.rikodev.sync.nexmesh.feature.ui.components.LiquidGlassPillButton
import com.rikodev.sync.nexmesh.feature.ui.components.QrCodeGenerator
import com.rikodev.sync.nexmesh.feature.ui.components.RikoSignatureSwitch
import com.rikodev.sync.nexmesh.feature.ui.components.RikoSwitchState
import com.rikodev.sync.nexmesh.feature.ui.components.SasVerificationCard
import com.rikodev.sync.nexmesh.feature.ui.theme.LocalNexMeshPalette
import com.rikodev.sync.nexmesh.feature.ui.theme.liquidGlassSurface

@Composable
fun DevicesScreen(
    localQrUri: String,
    storageProviderSwitchState: RikoSwitchState,
    connectedPeerNodeId: String?,
    trustedPeers: List<TrustedPeerRecord>,
    nearbyPeers: List<DiscoveredPeer>,
    pendingChallenge: PendingPairingChallenge?,
    pairingStatusMessage: String?,
    onToggleStorageProvider: (Boolean) -> Unit,
    onOpenQrScanner: () -> Unit,
    onRefreshNearby: () -> Unit,
    onPairNearbyPeer: (DiscoveredPeer) -> Unit,
    onReconnectTrustedPeer: (TrustedPeerRecord) -> Unit,
    onRemoveTrustedPeer: (String) -> Unit,
    onApproveSas: (String) -> Unit,
    onRejectSas: (String) -> Unit,
    headerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalNexMeshPalette.current
    var showQrCard by remember { mutableStateOf(false) }
    val qrBitmap = remember(localQrUri) { QrCodeGenerator.generate(localQrUri, 440) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        headerContent()

        Column {
            Text(
                text = "Connected Devices",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = p.inkPrimary
            )
            Text(
                text = "Pair once with QR Code — reconnect anytime without pairing again",
                fontSize = 12.sp,
                color = p.inkSecondary
            )
        }

        // Storage Phone Sharing Toggle (Riko Signature Switch)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlassSurface(
                    shape = RoundedCornerShape(50),
                    topColor = p.mintPillTop,
                    bottomColor = p.mintPillBottom,
                    rimTop = p.glassRimTop,
                    rimBottom = p.glassRimBottom,
                    shadowColor = p.glassShadow,
                    elevation = 5.dp
                )
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                Text(
                    text = "Share This Phone as Storage",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = p.inkPrimary
                )
                Text(
                    text = "Allow paired Main Phones to stream shared folders",
                    fontSize = 11.sp,
                    color = p.inkSecondary
                )
            }
            RikoSignatureSwitch(
                state = storageProviderSwitchState,
                onToggleRequest = onToggleStorageProvider,
                activeTrackColor = p.switchActiveTrack
            )
        }

        // Add New Device Actions (Scan QR / Show QR)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LiquidGlassPillButton(
                text = "Scan QR Code",
                onClick = onOpenQrScanner,
                isPrimary = true,
                modifier = Modifier.weight(1f)
            )
            LiquidGlassPillButton(
                text = if (showQrCard) "Hide QR Code" else "Show Pairing QR",
                onClick = { showQrCard = !showQrCard },
                isPrimary = false,
                modifier = Modifier.weight(1f)
            )
        }

        if (showQrCard) {
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp, elevated = true) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Scan from your other phone",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = p.inkPrimary
                    )
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "NexMesh Pairing QR Code",
                            modifier = Modifier.size(200.dp)
                        )
                    } else {
                        Text(
                            text = "Connect to Wi-Fi or Hotspot to generate a pairing QR code.",
                            fontSize = 12.sp,
                            color = p.inkSecondary
                        )
                    }
                    Text(
                        text = "One-time encrypted pairing • Expires in 10 minutes",
                        fontSize = 11.sp,
                        color = p.inkSecondary
                    )
                }
            }
        }

        if (pendingChallenge != null) {
            SasVerificationCard(
                peerName = pendingChallenge.peerDeviceName,
                sasCode = pendingChallenge.sasCode,
                onApprove = { onApproveSas(pendingChallenge.pairingToken) },
                onReject = { onRejectSas(pendingChallenge.pairingToken) }
            )
        }

        if (!pairingStatusMessage.isNullOrBlank()) {
            Text(
                text = pairingStatusMessage,
                fontSize = 12.sp,
                color = p.inkAccent,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // Previously Paired Trusted Devices
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Paired Phones (${trustedPeers.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = p.inkPrimary
                )
                if (trustedPeers.isEmpty()) {
                    Text(
                        text = "No paired devices yet. Tap 'Scan QR Code' or 'Show Pairing QR' to link a Storage Phone.",
                        fontSize = 12.sp,
                        color = p.inkSecondary
                    )
                } else {
                    trustedPeers.forEach { peer ->
                        val isConnected = peer.nodeId == connectedPeerNodeId
                        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = peer.deviceName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = p.inkPrimary
                                        )
                                        Text(
                                            text = if (isConnected) "Connected • Live Storage Active" else "Paired • Tap Reconnect",
                                            fontSize = 11.sp,
                                            color = if (isConnected) p.statusOnline else p.inkSecondary
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LiquidGlassPillButton(
                                        text = if (isConnected) "Connected" else "Reconnect",
                                        onClick = { onReconnectTrustedPeer(peer) },
                                        isPrimary = true,
                                        enabled = !isConnected,
                                        modifier = Modifier.weight(1f)
                                    )
                                    LiquidGlassPillButton(
                                        text = "Remove",
                                        onClick = { onRemoveTrustedPeer(peer.nodeId) },
                                        isPrimary = false,
                                        modifier = Modifier.weight(0.75f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Nearby Discovered Phones
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
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
                    Text(
                        text = "Nearby on Wi-Fi (${nearbyPeers.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = p.inkPrimary
                    )
                    LiquidGlassPillButton(
                        text = "Scan",
                        onClick = onRefreshNearby,
                        isPrimary = false
                    )
                }
                if (nearbyPeers.isEmpty()) {
                    Text(
                        text = "No nearby devices detected automatically. Make sure both phones share the same Wi-Fi or Hotspot, or use QR Code pairing.",
                        fontSize = 12.sp,
                        color = p.inkSecondary
                    )
                } else {
                    nearbyPeers.forEach { peer ->
                        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(peer.deviceName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = p.inkPrimary)
                                    Text("${peer.host}:${peer.port}", fontSize = 11.sp, color = p.inkSecondary)
                                }
                                LiquidGlassPillButton(
                                    text = "Pair Device",
                                    onClick = { onPairNearbyPeer(peer) },
                                    isPrimary = true
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(98.dp).navigationBarsPadding())
    }
}
