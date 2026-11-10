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
import com.rikodev.music.android.core.experience.calibration.WaveCalibration
import com.rikodev.music.android.presentation.debug.components.SliderCardCompact

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — RENDER PANEL
//
// Collapsible panel exposing WaveCalibration.Light + WaveCalibration.Glow
// constants. These control: fresnel, specular, edge glow, rim light,
// reflection, glass, shadow, highlight width, glow radius.
//
// CATEGORIES:
//   Light  — "How shape becomes glass" (31 constants)
//   Glow   — "How presence becomes atmosphere" (7 constants)
//
// PRODUCER: Phase 3.4J.2
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RenderPanel(
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
                text = "━━ RENDER ━━",
                color = Color(0xFFAA44FF),
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
                // Specular
                SectionLabel("Specular")
                SliderCardCompact(
                    "Specular Power",
                    WaveCalibration.Light.SPECULAR_POWER,
                    { WaveCalibration.Light.SPECULAR_POWER = it },
                    min = 2f, max = 20f, enabled = editable
                )
                SliderCardCompact(
                    "Specular Max",
                    WaveCalibration.Light.SPECULAR_MAX,
                    { WaveCalibration.Light.SPECULAR_MAX = it },
                    min = 0.3f, max = 1.0f, enabled = editable
                )
                SliderCardCompact(
                    "Treble Sparkle",
                    WaveCalibration.Light.TREBLE_SPARKLE_FACTOR,
                    { WaveCalibration.Light.TREBLE_SPARKLE_FACTOR = it },
                    min = 0.2f, max = 1.0f, enabled = editable
                )

                // Fresnel
                SectionLabel("Fresnel")
                SliderCardCompact(
                    "Fresnel Base",
                    WaveCalibration.Light.BASE_FRESNEL_POWER,
                    { WaveCalibration.Light.BASE_FRESNEL_POWER = it },
                    min = 1f, max = 8f, enabled = editable
                )
                SliderCardCompact(
                    "Fresnel Max",
                    WaveCalibration.Light.MAX_FRESNEL_POWER,
                    { WaveCalibration.Light.MAX_FRESNEL_POWER = it },
                    min = 2f, max = 10f, enabled = editable
                )

                // Edge Glow
                SectionLabel("Edge Glow")
                SliderCardCompact(
                    "Edge Glow Max",
                    WaveCalibration.Light.EDGE_GLOW_MAX,
                    { WaveCalibration.Light.EDGE_GLOW_MAX = it },
                    min = 0.1f, max = 0.80f, enabled = editable
                )
                SliderCardCompact(
                    "Glow Factor",
                    WaveCalibration.Light.SURFACE_TENSION_GLOW_FACTOR,
                    { WaveCalibration.Light.SURFACE_TENSION_GLOW_FACTOR = it },
                    min = 0.1f, max = 0.60f, enabled = editable
                )

                // Glass
                SectionLabel("Glass")
                SliderCardCompact(
                    "Glass Max",
                    WaveCalibration.Light.GLASS_MAX,
                    { WaveCalibration.Light.GLASS_MAX = it },
                    min = 0.2f, max = 0.80f, enabled = editable
                )
                SliderCardCompact(
                    "Glass Min",
                    WaveCalibration.Light.GLASS_MIN,
                    { WaveCalibration.Light.GLASS_MIN = it },
                    min = 0.0f, max = 0.10f, enabled = editable
                )

                // Highlight Width
                SectionLabel("Highlight Width")
                SliderCardCompact(
                    "Width Min",
                    WaveCalibration.Light.HIGHLIGHT_WIDTH_MIN,
                    { WaveCalibration.Light.HIGHLIGHT_WIDTH_MIN = it },
                    min = 0.10f, max = 0.40f, enabled = editable
                )
                SliderCardCompact(
                    "Width Max",
                    WaveCalibration.Light.HIGHLIGHT_WIDTH_MAX,
                    { WaveCalibration.Light.HIGHLIGHT_WIDTH_MAX = it },
                    min = 0.40f, max = 1.0f, enabled = editable
                )

                // Glow Atmosphere
                SectionLabel("Glow Atmosphere")
                SliderCardCompact(
                    "Ambient Radius",
                    WaveCalibration.Glow.AMBIENT_RADIUS_FRAC,
                    { WaveCalibration.Glow.AMBIENT_RADIUS_FRAC = it },
                    min = 0.10f, max = 0.60f, enabled = editable
                )
                SliderCardCompact(
                    "Outer Aura",
                    WaveCalibration.Glow.OUTER_AURA_PAD,
                    { WaveCalibration.Glow.OUTER_AURA_PAD = it },
                    min = 0.05f, max = 0.50f, enabled = editable
                )

                // Shadow
                SectionLabel("Shadow")
                SliderCardCompact(
                    "Shadow Max",
                    WaveCalibration.Light.SHADOW_MAX,
                    { WaveCalibration.Light.SHADOW_MAX = it },
                    min = 0.1f, max = 0.60f, enabled = editable
                )

                // Smoothing
                SectionLabel("Smoothing")
                SliderCardCompact(
                    "Highlight Smooth",
                    WaveCalibration.Light.HIGHLIGHT_SMOOTHING,
                    { WaveCalibration.Light.HIGHLIGHT_SMOOTHING = it },
                    min = 0.10f, max = 0.60f, enabled = editable
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
        color = Color(0xFF7733AA),
        fontSize = 8.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(top = 4.dp, bottom = 1.dp)
    )
}
