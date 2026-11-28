package com.rikodev.sync.nexmesh.feature.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.sync.nexmesh.feature.ui.theme.LocalNexMeshPalette
import com.rikodev.sync.nexmesh.feature.ui.theme.liquidGlassSurface

enum class NexMeshTab(val label: String) {
    HOME("Home"),
    STORAGE("Storage"),
    DEVICES("Devices"),
    SETTINGS("Settings")
}

/**
 * Clean 4-Tab Floating Liquid Glass Navigation Bar (Home, Storage, Devices, Settings).
 * Properly handles WindowInsets (navigationBarsPadding + imePadding) for Gesture & 3-Button Navigation.
 */
@Composable
fun FloatingGlassDock(
    currentTab: NexMeshTab,
    onSelectTab: (NexMeshTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalNexMeshPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .liquidGlassSurface(
                shape = RoundedCornerShape(30.dp),
                topColor = p.glassElevatedTop,
                bottomColor = p.glassElevatedBottom,
                rimTop = p.glassRimTop,
                rimBottom = p.glassRimBottom,
                shadowColor = p.glassShadow,
                elevation = 12.dp
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DockTabItem(Icons.Rounded.Home, "Home", currentTab == NexMeshTab.HOME, Modifier.weight(1f)) {
            onSelectTab(NexMeshTab.HOME)
        }
        DockTabItem(Icons.Rounded.FolderOpen, "Storage", currentTab == NexMeshTab.STORAGE, Modifier.weight(1f)) {
            onSelectTab(NexMeshTab.STORAGE)
        }
        DockTabItem(Icons.Rounded.Devices, "Devices", currentTab == NexMeshTab.DEVICES, Modifier.weight(1f)) {
            onSelectTab(NexMeshTab.DEVICES)
        }
        DockTabItem(Icons.Rounded.Settings, "Settings", currentTab == NexMeshTab.SETTINGS, Modifier.weight(1f)) {
            onSelectTab(NexMeshTab.SETTINGS)
        }
    }
}

@Composable
private fun DockTabItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val p = LocalNexMeshPalette.current
    Box(
        modifier = modifier
            .then(
                if (selected) {
                    Modifier.liquidGlassSurface(
                        shape = RoundedCornerShape(22.dp),
                        topColor = p.accentPillTop,
                        bottomColor = p.accentPillBottom,
                        rimTop = p.glassRimTop,
                        rimBottom = p.glassRimBottom,
                        shadowColor = p.glassShadow,
                        elevation = 2.dp
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) p.inkAccent else p.inkSecondary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) p.inkAccent else p.inkSecondary
            )
        }
    }
}
