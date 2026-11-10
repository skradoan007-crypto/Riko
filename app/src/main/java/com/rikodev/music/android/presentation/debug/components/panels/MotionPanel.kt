package com.rikodev.music.android.presentation.debug.components.panels

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
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
import com.rikodev.music.android.core.experience.calibration.WaveCalibration
import com.rikodev.music.android.presentation.debug.components.SliderCardCompact
import androidx.compose.ui.geometry.Offset

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — MOTION PANEL
//
// Collapsible panel exposing WaveCalibration.Motion constants.
// These control: spring model, energy force, drag, friction,
// direction hysteresis, momentum, offset, breathing.
//
// CATEGORY: Motion — "How fluid becomes movement"
// SOURCE: WaveCalibration.Motion (18 constants)
//
// PRODUCER: Phase 3.4J.2
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MotionPanel(
    expanded: Boolean,
    onToggle: () -> Unit,
    editable: Boolean,
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
                text = "━━ MOTION ━━",
                color = Color(0xFF44AA88),
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
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Spring
                SectionLabel("Spring")
                SliderCardCompact(
                    "Spring Const",
                    WaveCalibration.Motion.SPRING_CONSTANT,
                    { WaveCalibration.Motion.SPRING_CONSTANT = it },
                    min = 0.01f, max = 0.25f, enabled = editable
                )
                SliderCardCompact(
                    "Spring Settle",
                    WaveCalibration.Motion.SPRING_CONSTANT_SETTLING,
                    { WaveCalibration.Motion.SPRING_CONSTANT_SETTLING = it },
                    min = 0.05f, max = 0.30f, enabled = editable
                )

                // Energy Force
                SectionLabel("Energy Force")
                SliderCardCompact(
                    "Energy Force",
                    WaveCalibration.Motion.ENERGY_FORCE_SCALE,
                    { WaveCalibration.Motion.ENERGY_FORCE_SCALE = it },
                    min = 0.005f, max = 0.05f, enabled = editable
                )
                SliderCardCompact(
                    "Pressure Force",
                    WaveCalibration.Motion.PRESSURE_FORCE_SCALE,
                    { WaveCalibration.Motion.PRESSURE_FORCE_SCALE = it },
                    min = 0.002f, max = 0.03f, enabled = editable
                )
                SliderCardCompact(
                    "Beat Impulse",
                    WaveCalibration.Motion.BEAT_IMPULSE,
                    { WaveCalibration.Motion.BEAT_IMPULSE = it },
                    min = 0.005f, max = 0.06f, enabled = editable
                )

                // Drag
                SectionLabel("Drag")
                SliderCardCompact(
                    "Drag Base",
                    WaveCalibration.Motion.DRAG_BASE,
                    { WaveCalibration.Motion.DRAG_BASE = it },
                    min = 0.01f, max = 0.20f, enabled = editable
                )
                SliderCardCompact(
                    "Drag Expanding",
                    WaveCalibration.Motion.DRAG_EXPANDING,
                    { WaveCalibration.Motion.DRAG_EXPANDING = it },
                    min = 0.01f, max = 0.15f, enabled = editable
                )
                SliderCardCompact(
                    "Drag Breathing",
                    WaveCalibration.Motion.DRAG_BREATHING,
                    { WaveCalibration.Motion.DRAG_BREATHING = it },
                    min = 0.02f, max = 0.20f, enabled = editable
                )

                // Friction
                SectionLabel("Friction")
                SliderCardCompact(
                    "Friction",
                    WaveCalibration.Motion.FRICTION_BASE,
                    { WaveCalibration.Motion.FRICTION_BASE = it },
                    min = 0.05f, max = 0.40f, enabled = editable
                )

                // Breathing
                SectionLabel("Breathing")
                SliderCardCompact(
                    "Breath Amp",
                    WaveCalibration.Motion.BREATH_AMPLITUDE,
                    { WaveCalibration.Motion.BREATH_AMPLITUDE = it },
                    min = 0.001f, max = 0.01f, enabled = editable
                )
                SliderCardCompact(
                    "Breath Speed",
                    WaveCalibration.Motion.BREATH_SPEED,
                    { WaveCalibration.Motion.BREATH_SPEED = it },
                    min = 0.005f, max = 0.05f, enabled = editable
                )

                // Offset
                SectionLabel("Offset")
                SliderCardCompact(
                    "Max Offset",
                    WaveCalibration.Motion.OFFSET_MAX,
                    { WaveCalibration.Motion.OFFSET_MAX = it },
                    min = 0.1f, max = 1.0f, enabled = editable
                )

                Spacer(Modifier.padding(bottom = 8.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = "  $text",
        color = Color(0xFF337755),
        fontSize = 8.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(top = 4.dp, bottom = 1.dp)
    )
}
