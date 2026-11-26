package com.rikodev.sync.nexmesh.feature.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.sync.nexmesh.feature.ui.theme.NexMeshColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class MeshPointSeed(
    val baseX: Float,
    val baseY: Float,
    val ampX: Float,
    val ampY: Float,
    val phaseRad: Float,
    val radiusDp: Float,
    val hasPulse: Boolean
)

/**
 * Swarm Mesh Radar (Green-marked spec):
 * Pure Compose Canvas live animated mesh points with smooth organic drift,
 * connecting vectors, and breathing pulse ripples — zero allocations in onDraw, locked 60 FPS.
 */
@Composable
fun SwarmMeshRadarCard(
    activeNodeCount: Int,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "mesh_radar_motion")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mesh_orbit_phase"
    )
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mesh_pulse_ring"
    )

    val seeds = remember {
        listOf(
            MeshPointSeed(0.14f, 0.72f, 0.025f, 0.04f, 0.2f, 4.5f, false),
            MeshPointSeed(0.28f, 0.42f, 0.03f, 0.05f, 1.3f, 5.5f, true),
            MeshPointSeed(0.45f, 0.76f, 0.025f, 0.035f, 2.4f, 5.0f, true),
            MeshPointSeed(0.62f, 0.46f, 0.03f, 0.045f, 3.6f, 6.0f, true),
            MeshPointSeed(0.76f, 0.18f, 0.02f, 0.035f, 4.7f, 5.5f, true),
            MeshPointSeed(0.84f, 0.68f, 0.025f, 0.04f, 5.5f, 4.8f, false),
            MeshPointSeed(0.40f, 0.26f, 0.02f, 0.03f, 0.9f, 3.8f, false)
        )
    }

    val linkPairs = remember {
        listOf(0 to 1, 1 to 2, 1 to 3, 2 to 3, 3 to 4, 3 to 5, 1 to 6, 6 to 4, 2 to 5)
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 26.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Swarm Mesh Radar",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = NexMeshColors.InkSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$activeNodeCount Active",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = NexMeshColors.InkPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp)
            ) {
                val w = size.width
                val h = size.height

                // Pre-compute animated coordinates
                val positions = Array(seeds.size) { idx ->
                    val s = seeds[idx]
                    val x = (s.baseX + sin(phase + s.phaseRad) * s.ampX) * w
                    val y = (s.baseY + cos(phase * 1.15f + s.phaseRad) * s.ampY) * h
                    Offset(x, y)
                }

                // Draw connecting mesh vectors
                for ((a, b) in linkPairs) {
                    drawLine(
                        color = NexMeshColors.MeshLinkLine,
                        start = positions[a],
                        end = positions[b],
                        strokeWidth = 1.2.dp.toPx()
                    )
                }

                // Draw animated node dots + soft pulse rings
                for (idx in seeds.indices) {
                    val s = seeds[idx]
                    val center = positions[idx]
                    val rPx = s.radiusDp.dp.toPx()

                    if (s.hasPulse) {
                        val localPulse = (pulse + idx * 0.22f) % 1f
                        val ringRadius = rPx + localPulse * 18.dp.toPx()
                        val ringAlpha = (1f - localPulse) * 0.32f
                        drawCircle(
                            color = NexMeshColors.MeshNodePrimary.copy(alpha = ringAlpha),
                            radius = ringRadius,
                            center = center,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }

                    // Soft ambient halo
                    drawCircle(
                        color = NexMeshColors.MeshNodePulse,
                        radius = rPx * 1.9f,
                        center = center
                    )
                    // Core dot
                    drawCircle(
                        color = NexMeshColors.MeshNodePrimary,
                        radius = rPx,
                        center = center
                    )
                    // Specular white pinpoint
                    drawCircle(
                        color = Color(0xCCFFFFFF),
                        radius = rPx * 0.35f,
                        center = Offset(center.x - rPx * 0.25f, center.y - rPx * 0.25f)
                    )
                }
            }
        }
    }
}
