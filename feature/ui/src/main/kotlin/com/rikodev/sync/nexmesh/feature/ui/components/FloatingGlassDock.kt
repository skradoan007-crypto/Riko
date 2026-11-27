package com.rikodev.sync.nexmesh.feature.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.rikodev.sync.nexmesh.feature.ui.theme.NexMeshColors
import com.rikodev.sync.nexmesh.feature.ui.theme.liquidGlassSurface

enum class NexMeshTab {
    DASHBOARD,
    VDFS_DRIVE,
    COMPUTE,
    TRANSFER,
    DIAGNOSTICS,
    PAIRING
}

/**
 * Floating Split Liquid Glass Bottom Dock.
 * Properly respects Android WindowInsets (navigationBarsPadding + imePadding)
 * so it sits cleanly above both 3-Button and Gesture System Navigation Bars on every device.
 */
@Composable
fun FloatingGlassDock(
    currentTab: NexMeshTab,
    onSelectTab: (NexMeshTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .liquidGlassSurface(
                    shape = RoundedCornerShape(50),
                    topColor = Color(0xDCFFFFFF),
                    bottomColor = Color(0xC2F2F6FF),
                    elevation = 10.dp
                )
                .padding(horizontal = 6.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DockIconButton(Icons.Rounded.GridView, "হোম", currentTab == NexMeshTab.DASHBOARD) { onSelectTab(NexMeshTab.DASHBOARD) }
            DockIconButton(Icons.Rounded.CloudQueue, "ক্লাউড ড্রাইভ", currentTab == NexMeshTab.VDFS_DRIVE) { onSelectTab(NexMeshTab.VDFS_DRIVE) }
            DockIconButton(Icons.Rounded.Memory, "ভিডিও প্রসেসিং", currentTab == NexMeshTab.COMPUTE) { onSelectTab(NexMeshTab.COMPUTE) }
            DockIconButton(Icons.Rounded.SwapVert, "ফাইল ট্রান্সফার", currentTab == NexMeshTab.TRANSFER) { onSelectTab(NexMeshTab.TRANSFER) }
            DockIconButton(Icons.Rounded.Settings, "সেটিংস", currentTab == NexMeshTab.DIAGNOSTICS) { onSelectTab(NexMeshTab.DIAGNOSTICS) }
        }

        Box(
            modifier = Modifier
                .size(56.dp)
                .liquidGlassSurface(
                    shape = CircleShape,
                    topColor = if (currentTab == NexMeshTab.PAIRING) NexMeshColors.CyanLensStart else Color(0xDCFFFFFF),
                    bottomColor = if (currentTab == NexMeshTab.PAIRING) NexMeshColors.CyanLensEnd else Color(0xC2F2F6FF),
                    elevation = 10.dp
                )
                .clickable { onSelectTab(NexMeshTab.PAIRING) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.QrCode2,
                contentDescription = "ডিভাইস যুক্ত করুন",
                tint = NexMeshColors.InkPrimary,
                modifier = Modifier.size(25.dp)
            )
        }
    }
}

@Composable
private fun DockIconButton(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .then(
                if (selected) {
                    Modifier.liquidGlassSurface(
                        shape = CircleShape,
                        topColor = Color(0xD9E4ECFF),
                        bottomColor = Color(0xCCD2E2FF),
                        elevation = 2.dp
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) NexMeshColors.InkPrimary else NexMeshColors.InkSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}
