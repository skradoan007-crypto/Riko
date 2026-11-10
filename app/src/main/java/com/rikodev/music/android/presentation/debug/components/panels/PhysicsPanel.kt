package com.rikodev.music.android.presentation.debug.components.panels

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.rikodev.music.android.core.experience.calibration.WaveCalibration
import com.rikodev.music.android.presentation.debug.components.SliderCardCompact

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — PHYSICS PANEL
//
// Collapsible panel exposing WaveCalibration.Physics constants.
// These control: pressure, elasticity, viscosity, surface tension,
// thickness, opacity, stretch, compression, damping, flow resistance.
//
// CATEGORY: Physics — "How emotion becomes fluid"
// SOURCE: WaveCalibration.Physics (41 constants)
//
// PRODUCER: Phase 3.4J.2
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PhysicsPanel(
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
                text = "━━ PHYSICS ━━",
                color = Color(0xFF4488FF),
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
                // Pressure
                SectionLabel("Pressure")
                SliderCardCompact(
                    "Pressure Decay",
                    WaveCalibration.Physics.PRESSURE_DECAY,
                    { WaveCalibration.Physics.PRESSURE_DECAY = it },
                    min = 0.88f, max = 0.999f, enabled = editable
                )
                SliderCardCompact(
                    "Pressure Attack",
                    WaveCalibration.Physics.PRESSURE_ATTACK,
                    { WaveCalibration.Physics.PRESSURE_ATTACK = it },
                    min = 0.05f, max = 0.50f, enabled = editable
                )

                // Elasticity & Viscosity
                SectionLabel("Elasticity & Viscosity")
                SliderCardCompact(
                    "Elasticity",
                    WaveCalibration.Physics.ELASTICITY_BASE,
                    { WaveCalibration.Physics.ELASTICITY_BASE = it },
                    min = 0.1f, max = 0.90f, enabled = editable
                )
                SliderCardCompact(
                    "Viscosity W",
                    WaveCalibration.Physics.VISCOSITY_ENERGY_W,
                    { WaveCalibration.Physics.VISCOSITY_ENERGY_W = it },
                    min = 0.1f, max = 0.90f, enabled = editable
                )

                // Surface Tension
                SectionLabel("Surface Tension")
                SliderCardCompact(
                    "Tension Base",
                    WaveCalibration.Physics.SURFACE_TENSION_BASE,
                    { WaveCalibration.Physics.SURFACE_TENSION_BASE = it },
                    min = 0.2f, max = 1.0f, enabled = editable
                )

                // Thickness
                SectionLabel("Thickness")
                SliderCardCompact(
                    "Idle Thick",
                    WaveCalibration.Physics.THICKNESS_IDLE,
                    { WaveCalibration.Physics.THICKNESS_IDLE = it },
                    min = 0.005f, max = 0.08f, enabled = editable
                )
                SliderCardCompact(
                    "Active Thick",
                    WaveCalibration.Physics.THICKNESS_ACTIVE,
                    { WaveCalibration.Physics.THICKNESS_ACTIVE = it },
                    min = 0.10f, max = 0.80f, enabled = editable
                )
                SliderCardCompact(
                    "Expand Thick",
                    WaveCalibration.Physics.THICKNESS_EXPANDING,
                    { WaveCalibration.Physics.THICKNESS_EXPANDING = it },
                    min = 0.30f, max = 1.0f, enabled = editable
                )

                // Opacity
                SectionLabel("Opacity")
                SliderCardCompact(
                    "Opacity Min",
                    WaveCalibration.Physics.OPACITY_MINIMUM,
                    { WaveCalibration.Physics.OPACITY_MINIMUM = it },
                    min = 0.02f, max = 0.30f, enabled = editable
                )

                // Damping
                SectionLabel("Damping")
                SliderCardCompact(
                    "Active Damp",
                    WaveCalibration.Physics.DAMPING_ACTIVE,
                    { WaveCalibration.Physics.DAMPING_ACTIVE = it },
                    min = 0.05f, max = 0.50f, enabled = editable
                )
                SliderCardCompact(
                    "Expand Damp",
                    WaveCalibration.Physics.DAMPING_EXPANDING,
                    { WaveCalibration.Physics.DAMPING_EXPANDING = it },
                    min = 0.05f, max = 0.40f, enabled = editable
                )

                // Flow & Drift
                SectionLabel("Flow & Drift")
                SliderCardCompact(
                    "Drift Base",
                    WaveCalibration.Physics.DRIFT_BASE,
                    { WaveCalibration.Physics.DRIFT_BASE = it },
                    min = 0.0f, max = 0.30f, enabled = editable
                )
                SliderCardCompact(
                    "Flow Resist",
                    WaveCalibration.Physics.FLOW_RESISTANCE_BASE,
                    { WaveCalibration.Physics.FLOW_RESISTANCE_BASE = it },
                    min = 0.05f, max = 0.60f, enabled = editable
                )

                androidx.compose.foundation.layout.Spacer(
                    Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = "  $text",
        color = Color(0xFF3366AA),
        fontSize = 8.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(top = 4.dp, bottom = 1.dp)
    )
}
