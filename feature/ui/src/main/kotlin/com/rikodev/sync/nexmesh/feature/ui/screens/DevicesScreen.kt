package com.rikodev.sync.nexmesh.feature.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.sync.nexmesh.core.network.discovery.DiscoveredPeer
import com.rikodev.sync.nexmesh.core.protocol.model.TrustedPeerRecord
import com.rikodev.sync.nexmesh.core.protocol.session.PendingPairingChallenge
import com.rikodev.sync.nexmesh.feature.ui.components.GlassCard
import com.rikodev.sync.nexmesh.feature.ui.components.LiquidGlassPillButton
import com.rikodev.sync.nexmesh.feature.ui.components.QrCodeGenerator
import com.rikodev.sync.nexmesh.feature.ui.components.SasVerificationCard
import com.rikodev.sync.nexmesh.feature.ui.theme.LocalNexMeshPalette

@Composable
fun DevicesScreen(
    localQrUri: String,
    isListeningForPeers: Boolean,
    connectedPeerNodeId: String?,
    trustedPeers: List<TrustedPeerRecord>,
    nearbyPeers: List<DiscoveredPeer>,
    pendingChallenge: PendingPairingChallenge?,
    pairingStatusMessage: String?,
    onEnsureListening: () -> Unit,
    onStopListening: () -> Unit,
    onOpenQrScanner: () -> Unit,
    onConnectWithLink: (String) -> Unit,
    onLinkCopied: () -> Unit,
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
    val context = LocalContext.current
    var showQrCard by remember { mutableStateOf(false) }
    var linkInput by remember { mutableStateOf("") }
    var copiedFeedback by remember { mutableStateOf(false) }
    val qrBitmap = remember(localQrUri) { QrCodeGenerator.generate(localQrUri, 440) }

    fun copyPairingLink() {
        if (localQrUri.isBlank()) return
        onEnsureListening()
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("NexMesh Pairing Link", localQrUri))
        copiedFeedback = true
        onLinkCopied()
    }

    fun pasteFromClipboard() {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val text = cm?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.trim().orEmpty()
        if (text.isNotBlank()) {
            linkInput = text
        }
    }

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
                text = "Pair once via QR or Link — NexMesh detects Main and Storage roles automatically",
                fontSize = 12.sp,
                color = p.inkSecondary
            )
        }

        // Unified Pairing Card: Scan QR, Show QR & Copy Link, or Connect via Link
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp, elevated = true) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pair a Phone",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = p.inkPrimary
                    )
                    if (isListeningForPeers) {
                        LiquidGlassPillButton(
                            text = "Stop Sharing",
                            onClick = onStopListening,
                            isPrimary = false
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LiquidGlassPillButton(
                        text = "Scan QR",
                        onClick = onOpenQrScanner,
                        isPrimary = true,
                        modifier = Modifier.weight(1f)
                    )
                    LiquidGlassPillButton(
                        text = if (showQrCard) "Hide QR & Link" else "Show QR & Link",
                        onClick = {
                            val next = !showQrCard
                            showQrCard = next
                            copiedFeedback = false
                            if (next) onEnsureListening()
                        },
                        isPrimary = false,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (showQrCard) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "NexMesh Pairing QR Code",
                                modifier = Modifier.size(196.dp)
                            )
                            Text(
                                text = localQrUri,
                                fontSize = 11.sp,
                                color = p.inkSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                            LiquidGlassPillButton(
                                text = if (copiedFeedback) "Link Copied" else "Copy Pairing Link",
                                onClick = { copyPairingLink() },
                                isPrimary = false,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                text = "Connect to Wi-Fi or Hotspot to generate a pairing QR and link.",
                                fontSize = 12.sp,
                                color = p.inkSecondary
                            )
                        }
                    }
                }

                // Connect via Pairing Link
                OutlinedTextField(
                    value = linkInput,
                    onValueChange = { linkInput = it },
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = "Paste riko-swarm:// pairing link...",
                            fontSize = 12.sp,
                            color = p.inkSecondary
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = p.accentPrimary,
                        unfocusedBorderColor = p.glassRimBottom,
                        focusedTextColor = p.inkPrimary,
                        unfocusedTextColor = p.inkPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LiquidGlassPillButton(
                        text = "Paste",
                        onClick = { pasteFromClipboard() },
                        isPrimary = false,
                        modifier = Modifier.weight(0.8f)
                    )
                    LiquidGlassPillButton(
                        text = "Connect via Link",
                        onClick = {
                            val trimmed = linkInput.trim()
                            if (trimmed.isNotBlank()) {
                                onConnectWithLink(trimmed)
                            }
                        },
                        isPrimary = true,
                        enabled = linkInput.isNotBlank(),
                        modifier = Modifier.weight(1.2f)
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
                        text = "No paired devices yet. Scan a QR code or paste a pairing link above to connect.",
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
                        text = "No nearby devices found automatically. Use Scan QR or Connect via Link on the same Wi-Fi or Hotspot.",
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
                                    text = "Connect",
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
