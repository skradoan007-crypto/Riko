package com.rikodev.music.android.core.experience

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.music.android.audiofx.AudioState
import com.rikodev.music.android.audiofx.ExperienceDataHub
import com.rikodev.music.android.core.experience.wave.WaveDiagnostics
import com.rikodev.music.android.core.experience.wave.WaveState
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — EXPERIENCE INSPECTOR V2
//
// Developer-only floating debug panel. Shows live state from the entire
// Experience OS: Audio, Color, Wave, Motion, Performance.
//
// Upgraded from V1 (Step 3.3J):
//   - Added WAVE section: mode, pressure, momentum, expansion, curvature, etc.
//   - Added PERFORMANCE section: FPS, solver costs, frame time, errors
//   - Added MOTION section: velocity, offset, direction
//   - Added DEVICE section: refresh rate, latency context
//   - Scrollable for small screens
//   - Color-coded severity indicators
//
// NOT included in release builds. Controlled by BuildConfig.IS_DEBUG.
//
// Usage:
//   if (BuildConfig.DEBUG) {
//       ExperienceInspectorV2(
//           experienceDataHub = hub,
//           colorIntelligenceEngine = colorEngine,
//           waveState = waveState,
//           waveDiagnostics = diagnostics
//       )
//   }
//
// CONSUMER: Now Playing screen (debug overlay) or dedicated Inspector Activity.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ExperienceInspectorV2(
    experienceDataHub: ExperienceDataHub,
    colorIntelligenceEngine: ColorIntelligenceEngine,
    waveState: WaveState,
    waveDiagnostics: WaveDiagnostics,
    modifier: Modifier = Modifier
) {
    val audioState by experienceDataHub.audioState.collectAsStateWithLifecycle()
    val colorState by colorIntelligenceEngine.colorState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xE80A0A1A))
                .padding(10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // ── Header ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Riko Experience Inspector V2",
                    color = Color(0xFF19B7FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Status: ${waveDiagnostics.status.name}",
                    color = statusColor(waveDiagnostics.status.name),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // ═══════════════════════════════════════════════════════════════
            // AUDIO SECTION
            // ═══════════════════════════════════════════════════════════════
            InspectorSection("AUDIO") {
                InspectorRow("Source", audioState.source.name)
                InspectorRow("Energy", "%.3f".format(audioState.energy))
                InspectorRow("Bass", "%.3f".format(audioState.bassEnergy))
                InspectorRow("Mid", "%.3f".format(audioState.midEnergy))
                InspectorRow("Treble", "%.3f".format(audioState.trebleEnergy))
                InspectorRow("Peak", "%.3f".format(audioState.peak))
                InspectorRow("RMS", "%.3f".format(audioState.rms))
                InspectorRow("Beat", if (audioState.beatConfidence > 0.5f) "YES" else "no", highlight = audioState.beatConfidence > 0.5f)
                InspectorRow("Silent", if (audioState.isSilent) "YES" else "no", highlight = audioState.isSilent)
                InspectorRow("Transient", "%.3f".format(audioState.transient))
                InspectorRow("Envelope", "%.3f".format(audioState.envelope))
                InspectorRow("Playing", if (audioState.isPlaying) "YES" else "no", highlight = audioState.isPlaying)
            }

            // ═══════════════════════════════════════════════════════════════
            // COLOR SECTION
            // ═══════════════════════════════════════════════════════════════
            InspectorSection("COLOR") {
                InspectorRow("Temp", colorState.temperature.name)
                InspectorRow("Luminance", "%.3f".format(colorState.dominantLuminance))
                InspectorRow("Contrast", "%.3f".format(colorState.contrastLevel))
                InspectorRow("Mono", if (colorState.isMonochrome) "YES" else "no", highlight = colorState.isMonochrome)
                InspectorRow("Dark Art", if (colorState.isDarkArtwork) "YES" else "no")
                InspectorRow("Hue", "%.0f".format(colorState.dominantHue))
                InspectorRow("Sat", "%.3f".format(colorState.dominantSaturation))
                InspectorRow("Pulse", "%.3f".format(colorState.pulseAmount))
                InspectorRow("Tint", "%.3f".format(colorState.tintStrength))
                InspectorRow("Intensity", "%.3f".format(colorState.energyIntensity))
                InspectorRow("From Art", if (colorState.isFromArtwork) "YES" else "no")
            }

            // Color preview strip
            Row(
                modifier = Modifier.fillMaxWidth().height(16.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                ColorSwatch(colorState.primary, Modifier.weight(1f))
                ColorSwatch(colorState.secondary, Modifier.weight(1f))
                ColorSwatch(colorState.accent, Modifier.weight(1f))
                ColorSwatch(colorState.waveStart, Modifier.weight(1f))
                ColorSwatch(colorState.waveEnd, Modifier.weight(1f))
                ColorSwatch(colorState.glassTint, Modifier.weight(1f))
                ColorSwatch(colorState.ambientGlow, Modifier.weight(1f))
            }

            // ═══════════════════════════════════════════════════════════════
            // WAVE SECTION
            // ═══════════════════════════════════════════════════════════════
            InspectorSection("WAVE") {
                InspectorRow("Mode", waveState.mode.name)
                InspectorRow("Energy", "%.3f".format(waveState.overallEnergy))
                InspectorRow("Pressure", "%.3f".format(waveState.ribbon.pressure))
                InspectorRow("Momentum", "%.3f".format(waveState.energy.momentum))
                InspectorRow("Expansion", "%.3f".format(waveState.energy.expansion))
                InspectorRow("Bass", "%.3f".format(waveState.energy.bassPressure))
                InspectorRow("Mid", "%.3f".format(waveState.energy.midTexture))
                InspectorRow("Treble", "%.3f".format(waveState.energy.trebleSpark))
                InspectorRow("Thickness", "%.3f".format(waveState.ribbon.thickness))
                InspectorRow("Opacity", "%.3f".format(waveState.ribbon.opacity))
                InspectorRow("Elasticity", "%.3f".format(waveState.ribbon.elasticity))
                InspectorRow("Viscosity", "%.3f".format(waveState.ribbon.viscosity))
                InspectorRow("Surface T.", "%.3f".format(waveState.ribbon.surfaceTension))
                InspectorRow("Turbulence", "%.3f".format(waveState.ribbon.turbulence))
                InspectorRow("Active", if (waveState.isActive) "YES" else "no", highlight = waveState.isActive)
                InspectorRow("Expanding", if (waveState.isExpanding) "YES" else "no", highlight = waveState.isExpanding)
                InspectorRow("Settling", if (waveState.isSettling) "YES" else "no", highlight = waveState.isSettling)
                InspectorRow("Beat", if (waveState.hasBeat) "YES" else "no", highlight = waveState.hasBeat)
            }

            // ═══════════════════════════════════════════════════════════════
            // MOTION SECTION
            // ═══════════════════════════════════════════════════════════════
            InspectorSection("MOTION") {
                InspectorRow("Velocity", "%.4f".format(waveDiagnostics.velocity))
                InspectorRow("Offset", "%.4f".format(waveDiagnostics.offset))
            }

            // ═══════════════════════════════════════════════════════════════
            // GEOMETRY SECTION
            // ═══════════════════════════════════════════════════════════════
            InspectorSection("GEOMETRY") {
                InspectorRow("Segments", "${waveDiagnostics.segmentCount}")
                InspectorRow("Points", "${waveDiagnostics.geometryPointCount}")
                InspectorRow("Curvature", "%.3f".format(waveDiagnostics.curvature))
                InspectorRow("Has Path", if (waveState.hasGeometry) "YES" else "no")
                InspectorRow("Highlight", if (waveState.needsHighlight) "YES" else "no")
                InspectorRow("Glass", if (waveState.needsGlass) "YES" else "no")
            }

            // ═══════════════════════════════════════════════════════════════
            // PERFORMANCE SECTION
            // ═══════════════════════════════════════════════════════════════
            InspectorSection("PERFORMANCE") {
                InspectorRow("FPS", "%.1f".format(waveDiagnostics.fps), warn = waveDiagnostics.fps < 50f)
                InspectorRow("Energy", "%.2f ms".format(waveDiagnostics.energyTimeMs))
                InspectorRow("Physics", "%.2f ms".format(waveDiagnostics.physicsTimeMs))
                InspectorRow("Motion", "%.2f ms".format(waveDiagnostics.motionTimeMs))
                InspectorRow("Geometry", "%.2f ms".format(waveDiagnostics.geometryTimeMs))
                InspectorRow("Highlight", "%.2f ms".format(waveDiagnostics.highlightTimeMs))
                InspectorRow("TOTAL", "%.2f ms".format(waveDiagnostics.totalTimeMs), warn = waveDiagnostics.totalTimeMs > 4f)
                InspectorRow("Errors", "${waveDiagnostics.errorCount}", warn = waveDiagnostics.errorCount > 0)
            }

            // ═══════════════════════════════════════════════════════════════
            // HIGHLIGHT SECTION (from Highlight Solver)
            // ═══════════════════════════════════════════════════════════════
            InspectorSection("HIGHLIGHT") {
                InspectorRow("Specular", "%.3f".format(waveDiagnostics.highlightIntensity))
                InspectorRow("Glass", "%.3f".format(waveState.highlight.glassOpacity))
                InspectorRow("Fresnel", "%.3f".format(waveState.highlight.fresnel))
                InspectorRow("Rim", "%.3f".format(waveState.highlight.rimLight))
                InspectorRow("Shadow", "%.3f".format(waveState.highlight.shadowStrength))
            }

            // ═══════════════════════════════════════════════════════════════
            // DEVICE CONTEXT
            // ═══════════════════════════════════════════════════════════════
            InspectorSection("DEVICE") {
                InspectorRow("Source", waveDiagnostics.source)
                InspectorRow("Android", android.os.Build.VERSION.RELEASE)
            }

            // ── Footer ──────────────────────────────────────────────────
            Spacer(Modifier.height(4.dp))
            if (!waveDiagnostics.lastError.isNullOrEmpty()) {
                Text(
                    text = "LAST ERROR: ${waveDiagnostics.lastError}",
                    color = Color(0xFFFF4444),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// HELPER COMPOSABLES
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun InspectorSection(title: String, content: @Composable () -> Unit) {
    Text(
        text = "── $title ──",
        color = Color(0xFF667788),
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold
    )
    content()
    Spacer(Modifier.height(2.dp))
}

@Composable
private fun InspectorRow(
    label: String,
    value: String,
    highlight: Boolean = false,
    warn: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 0.5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color(0xFF8899AA),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = value,
            color = when {
                warn -> Color(0xFFFF6644)    // red for warnings
                highlight -> Color(0xFF44FF88) // green for active states
                else -> Color(0xFFDDDDDD)   // default white
            },
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun ColorSwatch(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(16.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(color)
    )
}

/**
 * Color-code engine status.
 */
private fun statusColor(status: String): Color = when (status) {
    "RUNNING" -> Color(0xFF44FF88)
    "PAUSED"  -> Color(0xFFFFAA22)
    "IDLE"    -> Color(0xFF8899AA)
    "READY"   -> Color(0xFF6688CC)
    "ERROR"   -> Color(0xFFFF4444)
    else      -> Color(0xFF8899AA)
}
