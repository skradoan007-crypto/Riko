package com.rikodev.sync.nexmesh.feature.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.sync.nexmesh.core.guardian.thermal.HardwareSafetySnapshot
import com.rikodev.sync.nexmesh.core.guardian.thermal.ThermalReading
import com.rikodev.sync.nexmesh.core.network.discovery.DiscoveredPeer
import com.rikodev.sync.nexmesh.feature.ui.components.GlassCard
import com.rikodev.sync.nexmesh.feature.ui.components.LiquidGlassPillButton
import com.rikodev.sync.nexmesh.feature.ui.components.LiquidGlassSegmentedBar
import com.rikodev.sync.nexmesh.feature.ui.components.SwarmMeshRadarCard
import com.rikodev.sync.nexmesh.feature.ui.theme.NexMeshColors

@Composable
fun DashboardScreen(
    selectedModeIndex: Int,
    onModeSelect: (Int) -> Unit,
    safety: HardwareSafetySnapshot,
    peers: List<DiscoveredPeer>,
    trustedCount: Int,
    lastThroughputMbps: Double,
    onRefreshDiscovery: () -> Unit,
    onPeerClick: (DiscoveredPeer) -> Unit,
    headerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val tempLabel = when (val t = safety.temperature) {
        is ThermalReading.Available -> String.format("%.1f°C", t.celsius)
        ThermalReading.Unavailable -> "N/A"
    }
    val batLabel = if (safety.batteryPct >= 0) "${safety.batteryPct}%" else "N/A"
    val speedLabel = if (lastThroughputMbps > 0.0) String.format("%.0f", lastThroughputMbps) else "0"

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        headerContent()

        LiquidGlassSegmentedBar(
            options = listOf("Controller", "Node", "Kiosk"),
            selectedIndex = selectedModeIndex,
            onSelect = onModeSelect,
            modifier = Modifier.fillMaxWidth()
        )

        SwarmMeshRadarCard(activeNodeCount = maxOf(peers.size, trustedCount))

        // 3 Compact Frosted Glass Telemetry Tiles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TelemetrySparkTile(
                value = tempLabel,
                subtitle = if (safety.action.name == "ALLOW") "Thermal" else "Guard",
                points = listOf(0.55f, 0.45f, 0.60f, 0.35f, 0.50f, 0.42f),
                modifier = Modifier.weight(1f)
            )
            TelemetrySparkTile(
                value = batLabel,
                subtitle = if (safety.isCharging) "Chg" else "Bat",
                points = listOf(0.75f, 0.65f, 0.58f, 0.62f, 0.48f, 0.42f),
                modifier = Modifier.weight(1f)
            )
            TelemetrySparkTile(
                value = speedLabel,
                subtitle = "MB/s",
                points = listOf(0.80f, 0.62f, 0.55f, 0.45f, 0.30f, 0.22f),
                modifier = Modifier.weight(1f)
            )
        }

        // Discovered Peers Card
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nearby Swarm Nodes (${peers.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NexMeshColors.InkPrimary
                    )
                    LiquidGlassPillButton(
                        text = "Scan",
                        onClick = onRefreshDiscovery,
                        isPrimary = false
                    )
                }

                if (peers.isEmpty()) {
                    Text(
                        text = "No LAN peers discovered yet. Start Node Server on another phone or scan QR in Pairing.",
                        fontSize = 12.sp,
                        color = NexMeshColors.InkSecondary
                    )
                } else {
                    peers.forEach { peer ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 16.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = peer.deviceName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NexMeshColors.InkPrimary
                                    )
                                    Text(
                                        text = "${peer.host}:${peer.port} • ${peer.tier.name}",
                                        fontSize = 11.sp,
                                        color = NexMeshColors.InkSecondary
                                    )
                                }
                                LiquidGlassPillButton(
                                    text = "Pair",
                                    onClick = { onPeerClick(peer) },
                                    isPrimary = true
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(92.dp))
    }
}

@Composable
private fun TelemetrySparkTile(
    value: String,
    subtitle: String,
    points: List<Float>,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier, cornerRadius = 20.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = NexMeshColors.InkPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = NexMeshColors.InkSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
            ) {
                if (points.size < 2) return@Canvas
                val path = Path()
                val stepX = size.width / (points.size - 1)
                points.forEachIndexed { idx, normY ->
                    val x = idx * stepX
                    val y = normY.coerceIn(0.05f, 0.95f) * size.height
                    if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = NexMeshColors.InkPrimary,
                    style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}
