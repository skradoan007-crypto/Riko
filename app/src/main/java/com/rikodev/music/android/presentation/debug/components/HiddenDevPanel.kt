package com.rikodev.music.android.presentation.debug.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.music.android.presentation.debug.LabPerformance

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — HIDDEN DEVELOPER PANEL
//
// Long press to open. Normal user NEVER sees this.
//
// Shows deep internals:
//   - Frame Time
//   - Memory
//   - GC
//   - CPU usage estimate
//   - Renderer name
//   - Bezier Count
//   - Point Count
//   - Draw Calls
//
// ACCESS: Long press on the Ribbon Viewport area.
// EXIT: Tap anywhere outside, or tap the close button.
//
// PRODUCER: Phase 3.4J.2
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HiddenDevPanel(
    visible: Boolean,
    performance: LabPerformance,
    activeProfile: String,
    quality: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xCC000000), RoundedCornerShape(8.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "⚠ HIDDEN DEV PANEL ⚠",
                color = Color(0xFFFF4444),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DevRow("Frame Time", "%.2fms".format(performance.frameTimeMs))
                DevRow("FPS", "${performance.fps}")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DevRow("Memory", "%.1fMB".format(performance.memoryMB))
                DevRow("GC", "${performance.gcCount}")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DevRow("Draw Calls", "${performance.drawCalls}")
                DevRow("Bezier", "${performance.bezierCount}")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DevRow("Points", "${performance.pointCount}")
                DevRow("Quality", quality)
            }

            DevRow("Profile", activeProfile)
        }
    }
}

@Composable
private fun DevRow(label: String, value: String) {
    Text(
        text = "$label: ",
        color = Color(0xFF666666),
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace
    )
    Text(
        text = value,
        color = Color(0xFF00FF88),
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace
    )
}
