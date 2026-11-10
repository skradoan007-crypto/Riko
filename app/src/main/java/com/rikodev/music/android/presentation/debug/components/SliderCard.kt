package com.rikodev.music.android.presentation.debug.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — SLIDER CARD
//
// A single calibration slider with label, value display, and optional range.
// This is the primary control for real-time parameter adjustment.
//
// DESIGN:
//   - Dark background card with rounded corners
//   - Monospace font (developer tool)
//   - Label on left, value on right, slider in middle
//   - Active color: RIKO accent blue (#19B7FF)
//   - Disabled state: grayed out (read-only mode)
//
// USAGE:
//   SliderCard(
//       label = "Pressure Decay",
//       value = 0.96f,
//       min = 0.90f,
//       max = 0.99f,
//       onValueChange = { WaveCalibration.Physics.PRESSURE_DECAY = it },
//       enabled = isEditable
//   )
//
// "Pressure ↓ Ribbon changes. No restart. No rebuild. Live."
// — Wave Laboratory Slider Rules
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SliderCard(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    min: Float = 0f,
    max: Float = 1f,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0D0D1A), RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label
        Text(
            text = label,
            color = if (enabled) Color(0xFFAAAAAA) else Color(0xFF555555),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(100.dp)
        )

        // Slider
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = if (enabled) Color(0xFF19B7FF) else Color(0xFF444444),
                activeTrackColor = if (enabled) Color(0xFF19B7FF) else Color(0xFF333333),
                inactiveTrackColor = Color(0xFF1A1A2E)
            )
        )

        // Value
        Text(
            text = "%.3f".format(value),
            color = if (enabled) Color(0xFF00FF88) else Color(0xFF444444),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(48.dp)
        )
    }
}

/**
 * Compact slider card with 2-column layout for dense panels.
 */
@Composable
fun SliderCardCompact(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    min: Float = 0f,
    max: Float = 1f,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (enabled) Color(0xFF888888) else Color(0xFF444444),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(80.dp)
        )

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            enabled = enabled,
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            colors = SliderDefaults.colors(
                thumbColor = if (enabled) Color(0xFF19B7FF) else Color(0xFF444444),
                activeTrackColor = if (enabled) Color(0xFF19B7FF) else Color(0xFF333333),
                inactiveTrackColor = Color(0xFF1A1A2E)
            )
        )

        Text(
            text = "%.3f".format(value),
            color = if (enabled) Color(0xFF00FF88) else Color(0xFF444444),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(42.dp)
        )
    }
}
