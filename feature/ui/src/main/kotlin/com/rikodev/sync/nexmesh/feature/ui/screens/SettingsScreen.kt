package com.rikodev.sync.nexmesh.feature.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.sync.nexmesh.core.guardian.oem.OemSurvivalGuide
import com.rikodev.sync.nexmesh.core.nativeengine.NativeBenchmarkResult
import com.rikodev.sync.nexmesh.feature.ui.components.GlassCard
import com.rikodev.sync.nexmesh.feature.ui.components.LiquidGlassPillButton
import com.rikodev.sync.nexmesh.feature.ui.components.RikoSignatureSwitch
import com.rikodev.sync.nexmesh.feature.ui.components.RikoSwitchState
import com.rikodev.sync.nexmesh.feature.ui.theme.LocalNexMeshPalette

@Composable
fun SettingsScreen(
    deviceName: String,
    localIp: String?,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    oemGuide: OemSurvivalGuide,
    benchmarkResult: NativeBenchmarkResult?,
    isBenchmarkRunning: Boolean,
    onOpenSystemFiles: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onRunBenchmark: () -> Unit,
    headerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalNexMeshPalette.current
    var showAdvanced by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        headerContent()

        Column {
            Text(
                text = "Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = p.inkPrimary
            )
            Text(
                text = "Appearance, storage preferences, background service, and security",
                fontSize = 12.sp,
                color = p.inkSecondary
            )
        }

        // Appearance
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                    Text("Appearance", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = p.inkPrimary)
                    Text(
                        text = if (isDarkTheme) "Dark Liquid Glass Theme" else "Light Liquid Glass Theme",
                        fontSize = 12.sp,
                        color = p.inkSecondary
                    )
                }
                RikoSignatureSwitch(
                    state = if (isDarkTheme) RikoSwitchState.ON else RikoSwitchState.OFF,
                    onToggleRequest = onToggleDarkTheme,
                    activeTrackColor = p.switchActiveTrack
                )
            }
        }

        // Storage Preferences
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Storage Preferences", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = p.inkPrimary)
                Text(
                    text = "NexMesh registers as a system Document Provider so compatible Android file managers stream remote files live without creating permanent local copies.",
                    fontSize = 12.sp,
                    color = p.inkSecondary
                )
                LiquidGlassPillButton(
                    text = "Open NexMesh in System Files",
                    onClick = onOpenSystemFiles,
                    isPrimary = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Background Service & Battery Optimization
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Background Service (${oemGuide.manufacturer})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = p.inkPrimary
                )
                Text(
                    text = if (oemGuide.isBatteryOptimizationIgnored) {
                        "Unrestricted background access is enabled."
                    } else {
                        "Battery optimization is restricted — your Storage Phone may pause when the screen turns off."
                    },
                    fontSize = 12.sp,
                    color = if (oemGuide.isBatteryOptimizationIgnored) p.statusOnline else p.statusWarning
                )
                LiquidGlassPillButton(
                    text = "Manage Battery Settings",
                    onClick = onOpenBatterySettings,
                    isPrimary = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Security and Privacy
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Security & Privacy", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = p.inkPrimary)
                Text("• End-to-End Encrypted streaming (ECDH P-256 + AES-256-GCM)", fontSize = 12.sp, color = p.inkSecondary)
                Text("• Selective Folder & File Access — never scans unselected storage", fontSize = 12.sp, color = p.inkSecondary)
                Text("• Zero Ads • Zero Subscriptions • Zero Permanent File Duplication", fontSize = 12.sp, color = p.inkSecondary)
            }
        }

        // Network & About (Expandable Advanced Diagnostics)
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdvanced = !showAdvanced },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("About NexMesh & Diagnostics", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = p.inkPrimary)
                        Text("v2.0.0 • $deviceName • ${localIp ?: "Offline"}", fontSize = 12.sp, color = p.inkSecondary)
                    }
                    Text(
                        text = if (showAdvanced) "Hide" else "Show",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = p.inkAccent
                    )
                }

                AnimatedVisibility(visible = showAdvanced) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (benchmarkResult != null) {
                            Text("Native ABI: ${benchmarkResult.engineAbi}", fontSize = 12.sp, color = p.inkSecondary)
                            Text(
                                text = String.format(
                                    "Native LZ4: %.1f MB/s  |  JVM Deflater: %.1f MB/s",
                                    benchmarkResult.nativeLz4Mbps,
                                    benchmarkResult.jvmDeflaterMbps
                                ),
                                fontSize = 12.sp,
                                color = p.inkPrimary
                            )
                        }
                        LiquidGlassPillButton(
                            text = if (isBenchmarkRunning) "Running Benchmark..." else "Run Native Performance Test",
                            onClick = onRunBenchmark,
                            isPrimary = false,
                            enabled = !isBenchmarkRunning,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(98.dp).navigationBarsPadding())
    }
}
