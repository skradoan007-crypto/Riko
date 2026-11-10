package com.rikodev.music.android.presentation.debug.components.panels

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.music.android.presentation.debug.LabPerformance
import com.rikodev.music.android.presentation.debug.components.ValueChip

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — PERFORMANCE PANEL
//
// Collapsible panel showing real-time performance metrics.
//
// METRICS:
//   - FPS (frame rate)
//   - Frame Time (ms per frame)
//   - Memory (MB)
//   - GC Count
//   - Draw Calls
//   - Bezier Count
//   - Point Count
//
// WARNING THRESHOLDS:
//   FPS < 55        → red warning
//   Frame Time > 16ms → red warning (dropping below 60fps)
//
// PRODUCER: Phase 3.4J.2
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PerformancePanel(
    expanded: Boolean,
    onToggle: () -> Unit,
    performance: LabPerformance,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .background(Color(0xFF0A0A18), RoundedCornerShape(8.dp))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "━━ PERFORMANCE ━━",
                color = Color(0xFFFFAA00),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (expanded) "▼" else "▶",
                color = Color(0xFF666666),
                fontSize = 10.sp
            )
        }

        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Frame metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ValueChip(
                        label = "FPS",
                        value = "${performance.fps}",
                        warning = performance.fps > 0 && performance.fps < 55,
                        modifier = Modifier.weight(1f)
                    )
                    ValueChip(
                        label = "Frame",
                        value = "%.1fms".format(performance.frameTimeMs),
                        warning = performance.frameTimeMs > 16f,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Memory & GC
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ValueChip(
                        label = "Memory",
                        value = "%.1fMB".format(performance.memoryMB),
                        warning = performance.memoryMB > 100f,
                        modifier = Modifier.weight(1f)
                    )
                    ValueChip(
                        label = "GC",
                        value = "${performance.gcCount}",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Render stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ValueChip(
                        label = "Draw Calls",
                        value = "${performance.drawCalls}",
                        modifier = Modifier.weight(1f)
                    )
                    ValueChip(
                        label = "Bezier",
                        value = "${performance.bezierCount}",
                        modifier = Modifier.weight(1f)
                    )
                }

                ValueChip(
                    label = "Points",
                    value = "${performance.pointCount}",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.padding(bottom = 4.dp))
            }
        }
    }
}
