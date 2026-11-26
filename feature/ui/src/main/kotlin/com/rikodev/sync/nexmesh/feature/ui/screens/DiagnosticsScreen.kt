package com.rikodev.sync.nexmesh.feature.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.sync.nexmesh.core.guardian.oem.OemSurvivalGuide
import com.rikodev.sync.nexmesh.core.nativeengine.NativeBenchmarkResult
import com.rikodev.sync.nexmesh.core.relay.ledger.CreditLedgerState
import com.rikodev.sync.nexmesh.core.relay.meter.BandwidthSnapshot
import com.rikodev.sync.nexmesh.core.relay.tunnel.RelayTunnelState
import com.rikodev.sync.nexmesh.feature.ui.components.GlassCard
import com.rikodev.sync.nexmesh.feature.ui.components.LiquidGlassPillButton
import com.rikodev.sync.nexmesh.feature.ui.components.RikoSignatureSwitch
import com.rikodev.sync.nexmesh.feature.ui.components.RikoSwitchState
import com.rikodev.sync.nexmesh.feature.ui.theme.NexMeshColors
import com.rikodev.sync.nexmesh.feature.ui.theme.liquidGlassSurface

@Composable
fun DiagnosticsScreen(
    localNodeId: String,
    localIp: String?,
    benchmarkResult: NativeBenchmarkResult?,
    isBenchmarkRunning: Boolean,
    oemGuide: OemSurvivalGuide,
    relayState: RelayTunnelState,
    relaySwitchState: RikoSwitchState,
    relayError: String?,
    bandwidth: BandwidthSnapshot,
    ledgerState: CreditLedgerState,
    onToggleRelay: (Boolean, String) -> Unit,
    onRunBenchmark: () -> Unit,
    onOpenOemSettings: () -> Unit,
    headerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    var relayUrl by remember { mutableStateOf("wss://relay.nexmesh.local/v1/tunnel") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        headerContent()

        Text(
            text = "Engine, Relay & Guardian",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = NexMeshColors.InkPrimary
        )

        // Node Identity Card
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Node Identity", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NexMeshColors.InkPrimary)
                Text("Node ID: $localNodeId", fontSize = 12.sp, color = NexMeshColors.InkSecondary)
                Text("Local LAN IP: ${localIp ?: "Offline"}", fontSize = 12.sp, color = NexMeshColors.InkSecondary)
            }
        }

        // Phase 4: Optional Zero-Knowledge E2EE Remote Relay & Micro-Utility Ledger
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassSurface(
                            shape = RoundedCornerShape(50),
                            topColor = NexMeshColors.MintPillStart,
                            bottomColor = NexMeshColors.MintPillEnd,
                            elevation = 3.dp
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "E2EE Remote Relay",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NexMeshColors.InkPrimary
                        )
                        Text(
                            text = "Zero-Knowledge • ${relayState.name}",
                            fontSize = 11.sp,
                            color = NexMeshColors.InkSecondary
                        )
                    }
                    RikoSignatureSwitch(
                        state = relaySwitchState,
                        onToggleRequest = { wantOn -> onToggleRelay(wantOn, relayUrl) }
                    )
                }

                OutlinedTextField(
                    value = relayUrl,
                    onValueChange = { relayUrl = it },
                    label = { Text("Relay Endpoint (wss://)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = String.format(
                        "Bandwidth Meter • LAN: %.2f MB (Free)  |  Relay: %.2f MB",
                        bandwidth.lanMb,
                        bandwidth.totalRelayMb
                    ),
                    fontSize = 12.sp,
                    color = NexMeshColors.InkPrimary
                )
                Text(
                    text = String.format(
                        "Pay-Per-GB Credit Ledger • Remaining: %.2f GB  |  Used: %.2f GB (Zero Ads • Zero Subscription)",
                        ledgerState.remainingGb,
                        ledgerState.usedGb
                    ),
                    fontSize = 11.sp,
                    color = NexMeshColors.InkSecondary
                )
                if (!relayError.isNullOrBlank()) {
                    Text(
                        text = "Relay status: $relayError",
                        fontSize = 11.sp,
                        color = NexMeshColors.StatusWarning
                    )
                }
            }
        }

        // C++17 Native Benchmark Card
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "C++17 NDK vs JVM Benchmark",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NexMeshColors.InkPrimary
                )
                if (benchmarkResult != null) {
                    Text("ABI: ${benchmarkResult.engineAbi}", fontSize = 12.sp, color = NexMeshColors.InkSecondary)
                    Text(
                        text = String.format(
                            "Native LZ4: %.1f MB/s  |  JVM Deflater: %.1f MB/s",
                            benchmarkResult.nativeLz4Mbps,
                            benchmarkResult.jvmDeflaterMbps
                        ),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = NexMeshColors.InkPrimary
                    )
                    Text(
                        text = String.format(
                            "Native xxHash64: %.1f MB/s  |  JVM CRC32: %.1f MB/s",
                            benchmarkResult.nativeHashMbps,
                            benchmarkResult.jvmCrc32Mbps
                        ),
                        fontSize = 12.sp,
                        color = NexMeshColors.InkSecondary
                    )
                    Text(
                        text = String.format("Compression Ratio: %.2fx (%.1f MB tested)", benchmarkResult.compressionRatio, benchmarkResult.totalMegabytes),
                        fontSize = 12.sp,
                        color = NexMeshColors.StatusHealthy
                    )
                } else {
                    Text(
                        text = "Run a real 128KB DirectByteBuffer streaming benchmark on this device.",
                        fontSize = 12.sp,
                        color = NexMeshColors.InkSecondary
                    )
                }

                LiquidGlassPillButton(
                    text = if (isBenchmarkRunning) "Benchmarking..." else "Run C++ vs JVM Benchmark",
                    onClick = onRunBenchmark,
                    isPrimary = true,
                    enabled = !isBenchmarkRunning,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // OEM Battery Optimization Survival Card
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "OEM Background Survival (${oemGuide.manufacturer})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NexMeshColors.InkPrimary
                )
                Text(
                    text = if (oemGuide.isBatteryOptimizationIgnored) {
                        "Battery Optimization: Unrestricted"
                    } else {
                        "Battery Optimization: Restricted (Background Node may pause)"
                    },
                    fontSize = 12.sp,
                    color = if (oemGuide.isBatteryOptimizationIgnored) NexMeshColors.StatusHealthy else NexMeshColors.StatusWarning
                )
                oemGuide.manualSteps.forEachIndexed { idx, step ->
                    Text("${idx + 1}. $step", fontSize = 12.sp, color = NexMeshColors.InkSecondary)
                }
                LiquidGlassPillButton(
                    text = "Open Battery Settings",
                    onClick = onOpenOemSettings,
                    isPrimary = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(92.dp))
    }
}
