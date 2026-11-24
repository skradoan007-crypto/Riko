package com.rikodev.vyrox.feature.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.rikodev.vyrox.core.design.LiquidGlassSurface
import com.rikodev.vyrox.core.design.LocalVyRoxGlass
import com.rikodev.vyrox.core.design.VyRoxPalette
import com.rikodev.vyrox.core.model.ProjectSummary
import java.io.File
import java.util.Locale

@Composable
fun HomeScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onOpenEditor: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val glass = LocalVyRoxGlass.current

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importVideoAndOpen(uri, onOpenEditor)
        }
    }

    val launchImport = { videoPicker.launch(arrayOf("video/*")) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(glass.backgroundBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 120.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = glass.inkPrimary, fontWeight = FontWeight.ExtraBold)) { append("Vy") }
                        withStyle(SpanStyle(brush = glass.brandGradient, fontWeight = FontWeight.ExtraBold)) { append("Rox") }
                    },
                    fontSize = 26.sp,
                    letterSpacing = (-0.5).sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LiquidGlassSurface(modifier = Modifier.size(40.dp), cornerRadius = 20.dp, onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                            contentDescription = "Switch Theme",
                            tint = VyRoxPalette.Amber,
                            modifier = Modifier.align(Alignment.Center).size(20.dp)
                        )
                    }
                    LiquidGlassSurface(modifier = Modifier.size(40.dp), cornerRadius = 20.dp, onClick = { viewModel.onTabSelected(3) }) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            tint = glass.inkPrimary,
                            modifier = Modifier.align(Alignment.Center).size(20.dp)
                        )
                    }
                }
            }

            state.statusBanner?.let { banner ->
                Spacer(Modifier.height(12.dp))
                LiquidGlassSurface(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp, onClick = { viewModel.dismissBanner() }) {
                    Text(banner, color = VyRoxPalette.Cyan, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(14.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            when (state.selectedTab) {
                0 -> HomeDashboardContent(state, launchImport, onOpenEditor, { viewModel.onTabSelected(it) }, { viewModel.deleteProject(it) })
                1 -> ProjectsTabContent(state, launchImport, onOpenEditor) { viewModel.deleteProject(it) }
                2 -> PresetsTabContent(state) { viewModel.deleteCustomPreset(it) }
                3 -> SettingsTabContent(state, isDarkTheme, onToggleTheme, { viewModel.clearCache() }, { viewModel.clearCrashLog() })
            }
        }

        // Floating Liquid Glass Bottom Nav (4 Real Working Tabs + Center Scissors FAB)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp)
                .fillMaxWidth()
        ) {
            LiquidGlassSurface(modifier = Modifier.fillMaxWidth().height(66.dp), cornerRadius = 33.dp) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavItem(Icons.Rounded.Home, "Home", state.selectedTab == 0) { viewModel.onTabSelected(0) }
                    NavItem(Icons.Rounded.VideoLibrary, "Projects", state.selectedTab == 1) { viewModel.onTabSelected(1) }
                    Spacer(Modifier.width(56.dp))
                    NavItem(Icons.Rounded.Palette, "Presets", state.selectedTab == 2) { viewModel.onTabSelected(2) }
                    NavItem(Icons.Rounded.Settings, "Settings", state.selectedTab == 3) { viewModel.onTabSelected(3) }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-14).dp)
                    .size(58.dp)
                    .shadow(14.dp, CircleShape, ambientColor = VyRoxPalette.Magenta, spotColor = VyRoxPalette.Violet)
                    .clip(CircleShape)
                    .background(glass.fabGradient)
                    .border(1.5.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                    .clickable(onClick = launchImport),
                contentAlignment = Alignment.Center
            ) {
                if (state.isImporting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.ContentCut, contentDescription = "Import & Edit", tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }
        }
    }
}

@Composable
private fun HomeDashboardContent(
    state: HomeUiState,
    launchImport: () -> Unit,
    onOpenEditor: (String) -> Unit,
    onSwitchTab: (Int) -> Unit,
    onDeleteProject: (String) -> Unit
) {
    val glass = LocalVyRoxGlass.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(172.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(glass.heroBannerBrush)
            .border(1.dp, glass.glassBorderBrush, RoundedCornerShape(24.dp))
            .clickable(onClick = launchImport)
            .padding(22.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.CenterStart), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Create\nSomething Amazing", color = Color.White, style = MaterialTheme.typography.headlineMedium)
            Text("Edit • Enhance • Inspire", color = Color.White.copy(alpha = 0.80f), style = MaterialTheme.typography.bodyMedium)
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f))
                .border(1.dp, Color.White.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Import Video", tint = Color.White)
        }
    }

    Spacer(Modifier.height(16.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        QuickActionTile(Modifier.weight(1f), Icons.Rounded.Add, VyRoxPalette.Cyan, "New Edit", launchImport)
        QuickActionTile(Modifier.weight(1f), Icons.Rounded.VideoLibrary, VyRoxPalette.Violet, "Projects") { onSwitchTab(1) }
        QuickActionTile(Modifier.weight(1f), Icons.Rounded.Star, VyRoxPalette.Amber, "Presets") { onSwitchTab(2) }
        QuickActionTile(Modifier.weight(1f), Icons.Rounded.Settings, VyRoxPalette.ElectricBlue, "Settings") { onSwitchTab(3) }
    }

    Spacer(Modifier.height(22.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Recent Projects", color = glass.inkPrimary, style = MaterialTheme.typography.titleLarge)
        Text(
            text = "See All (${state.recentProjects.size})",
            color = VyRoxPalette.ElectricBlue,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.clickable { onSwitchTab(1) }
        )
    }

    Spacer(Modifier.height(12.dp))

    if (state.recentProjects.isEmpty()) {
        LiquidGlassSurface(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp, onClick = launchImport) {
            Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = VyRoxPalette.Cyan, modifier = Modifier.size(28.dp))
                Column {
                    Text("Start Your First Edit", color = glass.inkPrimary, style = MaterialTheme.typography.titleMedium)
                    Text("Tap here or the center Scissors button to pick a video from your device.", color = glass.inkSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    } else {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 8.dp)) {
            items(state.recentProjects, key = { it.projectId }) { project ->
                RecentProjectCard(project, { onOpenEditor(project.projectId) }, { onDeleteProject(project.projectId) })
            }
        }
    }
}

@Composable
private fun ProjectsTabContent(
    state: HomeUiState,
    launchImport: () -> Unit,
    onOpenEditor: (String) -> Unit,
    onDeleteProject: (String) -> Unit
) {
    val glass = LocalVyRoxGlass.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("All Projects (${state.recentProjects.size})", color = glass.inkPrimary, style = MaterialTheme.typography.titleLarge)
        Text("+ Import Video", color = VyRoxPalette.Cyan, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = launchImport))
    }
    Spacer(Modifier.height(12.dp))
    if (state.recentProjects.isEmpty()) {
        LiquidGlassSurface(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, onClick = launchImport) {
            Text("No saved projects yet. Tap to import a video.", color = glass.inkSecondary, modifier = Modifier.padding(18.dp))
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.recentProjects.forEach { p ->
                val sec = (p.durationMs / 1000L).coerceAtLeast(0L)
                LiquidGlassSurface(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp, onClick = { onOpenEditor(p.projectId) }) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(p.title, color = glass.inkPrimary, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${p.aspectRatio.label} · ${String.format(Locale.US, "%02d:%02d", sec / 60, sec % 60)}", color = glass.inkSecondary, fontSize = 12.sp)
                        }
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = glass.inkSecondary, modifier = Modifier.size(22.dp).clickable { onDeleteProject(p.projectId) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetsTabContent(state: HomeUiState, onDeleteCustom: (String) -> Unit) {
    val glass = LocalVyRoxGlass.current
    Text("Color Presets (${state.presets.size})", color = glass.inkPrimary, style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(6.dp))
    Text("Apply any preset inside the Editor → Color panel, or save your own custom looks.", color = glass.inkSecondary, style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(12.dp))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        state.presets.forEach { preset ->
            LiquidGlassSurface(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(preset.name, color = glass.inkPrimary, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (preset.isBuiltIn) "Built-in VyRox Look" else "Custom Saved Preset",
                            color = if (preset.isBuiltIn) VyRoxPalette.Cyan else VyRoxPalette.Amber,
                            fontSize = 12.sp
                        )
                    }
                    if (!preset.isBuiltIn) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete Preset", tint = glass.inkSecondary, modifier = Modifier.clickable { onDeleteCustom(preset.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTabContent(
    state: HomeUiState,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onClearCache: () -> Unit,
    onClearCrashLog: () -> Unit
) {
    val glass = LocalVyRoxGlass.current
    Text("Studio Settings & Diagnostics", color = glass.inkPrimary, style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(14.dp))

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LiquidGlassSurface(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp, onClick = onToggleTheme) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("App Theme", color = glass.inkPrimary, style = MaterialTheme.typography.titleMedium)
                    Text(if (isDarkTheme) "Dark Liquid Glass" else "Light Liquid Glass", color = glass.inkSecondary, fontSize = 12.sp)
                }
                Icon(if (isDarkTheme) Icons.Rounded.DarkMode else Icons.Rounded.LightMode, contentDescription = null, tint = VyRoxPalette.Amber)
            }
        }

        LiquidGlassSurface(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp, onClick = onClearCache) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Temporary Render Cache", color = glass.inkPrimary, style = MaterialTheme.typography.titleMedium)
                    Text("${state.cacheBytes / 1024L} KB used · Tap to clean safely", color = glass.inkSecondary, fontSize = 12.sp)
                }
                Icon(Icons.Rounded.CleaningServices, contentDescription = "Clear Cache", tint = VyRoxPalette.Cyan)
            }
        }

        state.deviceProfile?.let { hw ->
            LiquidGlassSurface(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Hardware Engine Profile", color = glass.inkPrimary, style = MaterialTheme.typography.titleMedium)
                    Text("Tier: ${hw.tier.name} · RAM: ${hw.totalRamMb} MB", color = glass.inkSecondary, fontSize = 13.sp)
                    Text("Shader Engine: Universal GLSL ES 2.0/3.0 · PIP Budget: ${hw.maxRecommendedPipVideoLayers} layers", color = glass.inkSecondary, fontSize = 12.sp)
                }
            }
        }

        state.lastCrashLog?.let { log ->
            LiquidGlassSurface(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Last Crash Diagnostic", color = VyRoxPalette.Magenta, style = MaterialTheme.typography.titleMedium)
                        Text("Clear", color = VyRoxPalette.Cyan, fontSize = 12.sp, modifier = Modifier.clickable(onClick = onClearCrashLog))
                    }
                    Text(log.take(900), color = glass.inkSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(modifier: Modifier, icon: ImageVector, tint: Color, label: String, onClick: () -> Unit) {
    val glass = LocalVyRoxGlass.current
    LiquidGlassSurface(modifier = modifier.height(90.dp), cornerRadius = 18.dp, onClick = onClick) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(tint.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(label, color = glass.inkPrimary, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun RecentProjectCard(project: ProjectSummary, onOpen: () -> Unit, onDelete: () -> Unit) {
    val glass = LocalVyRoxGlass.current
    val totalSec = (project.durationMs / 1000L).coerceAtLeast(0L)
    val durText = String.format(Locale.US, "%02d:%02d", totalSec / 60L, totalSec % 60L)

    Column(modifier = Modifier.width(136.dp).clickable(onClick = onOpen)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(VyRoxPalette.Violet.copy(alpha = 0.65f), VyRoxPalette.ElectricBlue.copy(alpha = 0.75f))))
                .border(1.dp, glass.glassBorderBrush, RoundedCornerShape(18.dp))
        ) {
            if (!project.thumbnailPath.isNullOrBlank()) {
                AsyncImage(
                    model = File(project.thumbnailPath!!),
                    contentDescription = project.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.align(Alignment.Center).size(34.dp))
            Text(
                text = durText,
                color = Color.White,
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(project.title, color = glass.inkPrimary, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = glass.inkSecondary, modifier = Modifier.size(18.dp).clickable(onClick = onDelete))
        }
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val glass = LocalVyRoxGlass.current
    val tint = if (selected) VyRoxPalette.Magenta else glass.inkSecondary
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 6.dp)) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, color = tint, fontSize = 11.sp)
    }
}
