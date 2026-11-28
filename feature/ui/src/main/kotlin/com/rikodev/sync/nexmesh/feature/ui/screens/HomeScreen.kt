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
import com.rikodev.sync.nexmesh.feature.ui.components.LiquidGlassSegmentedBar
import com.rikodev.sync.nexmesh.feature.ui.theme.LocalNexMeshPalette

@Composable
fun HomeScreen(
    roleIndex: Int, // 0 = Main Phone, 1 = Storage Phone
    onRoleSelect: (Int) -> Unit,
    connectedStoragePhoneName: String?,
    connectedStorageCount: Int,
    remoteAvailableBytes: Long,
    localSharedFolderCount: Int,
    localSharedFileCount: Int,
    isStorageProviderActive: Boolean,
    recentActivity: List<String>,
    onOpenSystemFiles: () -> Unit,
    onAddStorageClick: () -> Unit,
    onManageStorageClick: () -> Unit,
    headerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalNexMeshPalette.current
    val remoteGb = remoteAvailableBytes / (1024.0 * 1024.0 * 1024.0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        headerContent()

        // Device Role Selector (Main Phone vs Storage Phone)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LiquidGlassSegmentedBar(
                options = listOf("Main Phone", "Storage Phone"),
                selectedIndex = roleIndex,
                onSelect = onRoleSelect,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = if (roleIndex == 0) {
                    "Use paired Storage Phones as live virtual network storage without copying files locally."
                } else {
                    "Share selected folders or files from this phone so your Main Phone can stream them live."
                },
                fontSize = 12.sp,
                color = p.inkSecondary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // Hero Liquid Glass Storage Card
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
                        text = if (roleIndex == 0) "Available Remote Storage" else "Storage Sharing Status",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = p.inkSecondary
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (connectedStoragePhoneName != null || isStorageProviderActive) {
                                    p.statusOnline.copy(alpha = 0.14f)
                                } else {
                                    p.inkSecondary.copy(alpha = 0.12f)
                                }
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (connectedStoragePhoneName != null || isStorageProviderActive) p.statusOnline else p.inkSecondary
                                )
                        )
                        Text(
                            text = when {
                                roleIndex == 1 && isStorageProviderActive -> "Live"
                                connectedStoragePhoneName != null -> "Connected"
                                else -> "Offline"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (connectedStoragePhoneName != null || isStorageProviderActive) p.statusOnline else p.inkSecondary
                        )
                    }
                }

                Text(
                    text = when {
                        roleIndex == 0 && connectedStoragePhoneName != null -> String.format("%.1f GB", remoteGb)
                        roleIndex == 0 -> "No Device Linked"
                        isStorageProviderActive -> "$localSharedFolderCount Folders Shared"
                        else -> "Sharing Paused"
                    },
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = p.inkPrimary
                )

                Text(
                    text = when {
                        roleIndex == 0 && connectedStoragePhoneName != null ->
                            "Streaming live from $connectedStoragePhoneName • Zero local file duplication"
                        roleIndex == 0 ->
                            "Pair an unused Android phone to access its folders directly from your Files app."
                        else ->
                            "Selected folders and files remain on this device and stream on demand."
                    },
                    fontSize = 12.sp,
                    color = p.inkSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (roleIndex == 0) {
                        LiquidGlassPillButton(
                            text = "Open in Files App",
                            onClick = onOpenSystemFiles,
                            isPrimary = true,
                            modifier = Modifier.weight(1.1f)
                        )
                        LiquidGlassPillButton(
                            text = "Add Storage",
                            onClick = onAddStorageClick,
                            isPrimary = false,
                            modifier = Modifier.weight(0.9f)
                        )
                    } else {
                        LiquidGlassPillButton(
                            text = "Manage Shared Folders",
                            onClick = onManageStorageClick,
                            isPrimary = true,
                            modifier = Modifier.fillMaxWidth()
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
                    Text("Storage Phones", fontSize = 12.sp, color = p.inkSecondary)
                    Text("$connectedStorageCount Connected", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = p.inkPrimary)
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
                        text = "No storage activity yet. Pair a device or select a shared folder to begin.",
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
