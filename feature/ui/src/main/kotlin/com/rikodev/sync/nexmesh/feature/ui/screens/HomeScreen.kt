package com.rikodev.sync.nexmesh.feature.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.sync.nexmesh.feature.ui.components.GlassCard
import com.rikodev.sync.nexmesh.feature.ui.components.LiquidGlassPillButton
import com.rikodev.sync.nexmesh.feature.ui.theme.LocalNexMeshPalette

@Composable
fun HomeScreen(
    connectedStoragePhoneName: String?,
    connectedStorageCount: Int,
    remoteAvailableBytes: Long,
    localSharedFolderCount: Int,
    localSharedFileCount: Int,
    isStorageProviderActive: Boolean,
    recentActivity: List<String>,
    onOpenSystemFiles: () -> Unit,
    onConnectDeviceClick: () -> Unit,
    onManageStorageClick: () -> Unit,
    headerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalNexMeshPalette.current
    val remoteGb = remoteAvailableBytes / (1024.0 * 1024.0 * 1024.0)
    val isActingAsMain = connectedStoragePhoneName != null
    val isActingAsStorage = (localSharedFolderCount + localSharedFileCount) > 0 && isStorageProviderActive

    val detectedRoleLabel = when {
        isActingAsMain && isActingAsStorage -> "Main & Storage Phone"
        isActingAsMain -> "Main Phone"
        isActingAsStorage -> "Storage Phone"
        else -> "Auto-Detect Ready"
    }

    val isLive = isActingAsMain || isActingAsStorage

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        headerContent()

        // Hero Liquid Glass Storage Card (Automatically adapts to Main Phone or Storage Phone)
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 28.dp,
            elevated = true
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = detectedRoleLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = p.inkAccent
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isLive) p.statusOnline.copy(alpha = 0.14f)
                                else p.inkSecondary.copy(alpha = 0.12f)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isLive) p.statusOnline else p.inkSecondary)
                        )
                        Text(
                            text = when {
                                isActingAsMain -> "Connected"
                                isActingAsStorage -> "Sharing Live"
                                else -> "Standby"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isLive) p.statusOnline else p.inkSecondary
                        )
                    }
                }

                Text(
                    text = when {
                        isActingAsMain -> String.format("%.1f GB Free", remoteGb)
                        isActingAsStorage -> "$localSharedFolderCount Folders • $localSharedFileCount Files"
                        else -> "Live Virtual Storage"
                    },
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = p.inkPrimary
                )

                Text(
                    text = when {
                        isActingAsMain ->
                            "Streaming live from $connectedStoragePhoneName • Original files stay on your Storage Phone."
                        isActingAsStorage ->
                            "This phone is sharing selected folders and files on demand. Pair or reconnect from your Main Phone."
                        else ->
                            "Pair another phone via QR or Link to stream its storage, or pick folders in Storage to share this phone automatically."
                    },
                    fontSize = 12.sp,
                    color = p.inkSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isActingAsMain) {
                        LiquidGlassPillButton(
                            text = "Open in Files App",
                            onClick = onOpenSystemFiles,
                            isPrimary = true,
                            modifier = Modifier.weight(1.1f)
                        )
                        LiquidGlassPillButton(
                            text = "Devices",
                            onClick = onConnectDeviceClick,
                            isPrimary = false,
                            modifier = Modifier.weight(0.9f)
                        )
                    } else {
                        LiquidGlassPillButton(
                            text = "Connect Phone",
                            onClick = onConnectDeviceClick,
                            isPrimary = true,
                            modifier = Modifier.weight(1f)
                        )
                        LiquidGlassPillButton(
                            text = "Share Folders",
                            onClick = onManageStorageClick,
                            isPrimary = false,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Compact Asymmetric Metrics Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassCard(modifier = Modifier.weight(1f), cornerRadius = 20.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Remote Storage", fontSize = 12.sp, color = p.inkSecondary)
                    Text("$connectedStorageCount Connected", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = p.inkPrimary)
                }
            }
            GlassCard(modifier = Modifier.weight(1f), cornerRadius = 20.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Shared Here", fontSize = 12.sp, color = p.inkSecondary)
                    Text(
                        "$localSharedFolderCount Folders • $localSharedFileCount Files",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = p.inkPrimary
                    )
                }
            }
        }

        // Recent Storage Activity
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Recent Activity",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = p.inkPrimary
                )
                if (recentActivity.isEmpty()) {
                    Text(
                        text = "No storage activity yet. Connect a phone or share a folder to get started.",
                        fontSize = 12.sp,
                        color = p.inkSecondary
                    )
                } else {
                    recentActivity.take(6).forEach { event ->
                        Text(
                            text = "• $event",
                            fontSize = 12.sp,
                            color = p.inkSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(98.dp).navigationBarsPadding())
    }
}
