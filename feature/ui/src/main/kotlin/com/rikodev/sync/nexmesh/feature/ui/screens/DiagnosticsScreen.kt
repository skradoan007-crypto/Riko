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

/**
 * Categorized, user-friendly Settings & Device Screen with Expandable Advanced Settings.
 * Keeps every underlying technical feature intact while making the UI intuitive for everyday users.
 */
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
    var showAdvanced by remember { mutableStateOf(false) }

    val relayStatusBn = when (relayState) {
        RelayTunnelState.CONNECTED -> "সংযুক্ত"
        RelayTunnelState.CONNECTING -> "সংযোগ হচ্ছে..."
        RelayTunnelState.ERROR -> "সংযোগে সমস্যা"
        RelayTunnelState.DISCONNECTED -> "বন্ধ আছে"
    }

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
                text = "সেটিংস ও ডিভাইস",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = NexMeshColors.InkPrimary
            )
            Text(
                text = "সংযোগ, ডেটা ব্যবহার, ব্যাটারি সুরক্ষা ও অ্যাডভান্সড টুলস",
                fontSize = 12.sp,
                color = NexMeshColors.InkSecondary
            )
        }

        // Category 1: এই ডিভাইস
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "এই ডিভাইস",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NexMeshColors.InkPrimary
                )
                Text(
                    text = "ডিভাইস আইডি: $localNodeId",
                    fontSize = 12.sp,
                    color = NexMeshColors.InkSecondary
                )
                Text(
                    text = "লোকাল নেটওয়ার্ক ঠিকানা: ${localIp ?: "নেটওয়ার্ক বিচ্ছিন্ন"}",
                    fontSize = 12.sp,
                    color = NexMeshColors.InkSecondary
                )
            }
        }

        // Category 2: রিমোট সংযোগ ও ডেটা
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "রিমোট সংযোগ ও ডেটা",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NexMeshColors.InkPrimary
                )

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
                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                        Text(
                            text = "নিরাপদ রিমোট সংযোগ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NexMeshColors.InkPrimary
                        )
                        Text(
                            text = "দূর থেকে এনক্রিপ্টেড ফাইল অ্যাক্সেস • $relayStatusBn",
                            fontSize = 11.sp,
                            color = NexMeshColors.InkSecondary
                        )
                    }
                    RikoSignatureSwitch(
                        state = relaySwitchState,
                        onToggleRequest = { wantOn -> onToggleRelay(wantOn, relayUrl) }
                    )
                }

                Text(
                    text = String.format(
                        "ডেটা ব্যবহার: লোকাল %.2f MB (ফ্রি)  |  রিমোট %.2f MB",
                        bandwidth.lanMb,
                        bandwidth.totalRelayMb
                    ),
                    fontSize = 12.sp,
                    color = NexMeshColors.InkPrimary
                )
                Text(
                    text = String.format(
                        "ডেটা ক্রেডিট: অবশিষ্ট %.2f GB  |  ব্যবহৃত %.2f GB (বিজ্ঞাপন ও সাবস্ক্রিপশন মুক্ত)",
                        ledgerState.remainingGb,
                        ledgerState.usedGb
                    ),
                    fontSize = 11.sp,
                    color = NexMeshColors.InkSecondary
                )
                if (!relayError.isNullOrBlank()) {
                    Text(
                        text = "রিমোট স্ট্যাটাস: $relayError",
                        fontSize = 11.sp,
                        color = NexMeshColors.StatusWarning
                    )
                }
            }
        }

        // Category 3: ব্যাকগ্রাউন্ড ও ব্যাটারি সুরক্ষা
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ব্যাকগ্রাউন্ড ও ব্যাটারি সুরক্ষা (${oemGuide.manufacturer})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NexMeshColors.InkPrimary
                )
                Text(
                    text = if (oemGuide.isBatteryOptimizationIgnored) {
                        "ব্যাকগ্রাউন্ড অনুমতি চালু আছে — অ্যাপ মিনিমাইজ করলেও কাজ চলবে"
                    } else {
                        "ব্যাটারি সীমাবদ্ধতা চালু আছে — মিনিমাইজ করলে সিস্টেম কাজ থামিয়ে দিতে পারে"
                    },
                    fontSize = 12.sp,
                    color = if (oemGuide.isBatteryOptimizationIgnored) NexMeshColors.StatusHealthy else NexMeshColors.StatusWarning
                )
                oemGuide.manualSteps.forEachIndexed { idx, step ->
                    Text("${idx + 1}. $step", fontSize = 12.sp, color = NexMeshColors.InkSecondary)
                }
                LiquidGlassPillButton(
                    text = "ব্যাটারি সেটিংস খুলুন",
                    onClick = onOpenOemSettings,
                    isPrimary = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Category 4: Expandable Advanced Settings
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdvanced = !showAdvanced },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "অ্যাডভান্সড সেটিংস",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NexMeshColors.InkPrimary
                        )
                        Text(
                            text = "পারফরম্যান্স টেস্ট (C++17 NDK), রিমোট সার্ভার ঠিকানা ও টেকনিক্যাল তথ্য",
                            fontSize = 11.sp,
                            color = NexMeshColors.InkSecondary
                        )
                    }
                    Text(
                        text = if (showAdvanced) "লুকান" else "খুলুন",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NexMeshColors.InkAccentIndigo
                    )
                }

                AnimatedVisibility(visible = showAdvanced) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = relayUrl,
                            onValueChange = { relayUrl = it },
                            label = { Text("রিমোট রিলে সার্ভার (Relay Endpoint wss://)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "পারফরম্যান্স টেস্ট (C++17 NDK vs JVM Benchmark)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NexMeshColors.InkPrimary
                        )

                        if (benchmarkResult != null) {
                            Text("আর্কিটেকচার (ABI): ${benchmarkResult.engineAbi}", fontSize = 12.sp, color = NexMeshColors.InkSecondary)
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
                                text = String.format("কম্প্রেশন অনুপাত: %.2fx (%.1f MB পরীক্ষিত)", benchmarkResult.compressionRatio, benchmarkResult.totalMegabytes),
                                fontSize = 12.sp,
                                color = NexMeshColors.StatusHealthy
                            )
                        } else {
                            Text(
                                text = "আপনার ডিভাইসের C++ নেটিভ ইঞ্জিন বনাম সাধারণ JVM গতি পরীক্ষা করুন।",
                                fontSize = 12.sp,
                                color = NexMeshColors.InkSecondary
                            )
                        }

                        LiquidGlassPillButton(
                            text = if (isBenchmarkRunning) "পরীক্ষা চলছে..." else "পারফরম্যান্স টেস্ট চালান",
                            onClick = onRunBenchmark,
                            isPrimary = true,
                            enabled = !isBenchmarkRunning,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(96.dp).navigationBarsPadding())
    }
}
