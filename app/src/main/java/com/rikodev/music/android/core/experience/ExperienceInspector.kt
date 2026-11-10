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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.music.android.audiofx.AudioState
import com.rikodev.music.android.audiofx.ExperienceDataHub

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — EXPERIENCE INSPECTOR
//
// Developer-only floating debug panel. Shows live audio + color state
// for debugging reactive UI behavior.
//
// NOT included in release builds. Controlled by BuildConfig.IS_DEBUG.
//
// Usage:
//   if (BuildConfig.DEBUG) {
//       ExperienceInspector(experienceDataHub, colorIntelligenceEngine)
//   }
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ExperienceInspector(
    experienceDataHub: ExperienceDataHub,
    colorIntelligenceEngine: ColorIntelligenceEngine,
    modifier: Modifier = Modifier
) {
    val audioState by experienceDataHub.audioState.collectAsStateWithLifecycle()
    val colorState by colorIntelligenceEngine.colorState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xCC0A0A1A))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Header
            Text(
                text = "Riko Experience Inspector",
                color = Color(0xFF19B7FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Spacer(Modifier.height(4.dp))

            // Audio State Section
            InspectorSection("AUDIO") {
                InspectorRow("Source", audioState.source.name)
                InspectorRow("Energy", "%.2f".format(audioState.energy))
                InspectorRow("Bass", "%.2f".format(audioState.bassEnergy))
                InspectorRow("Mid", "%.2f".format(audioState.midEnergy))
                InspectorRow("Treble", "%.2f".format(audioState.trebleEnergy))
                InspectorRow("Peak", "%.2f".format(audioState.peak))
                InspectorRow("RMS", "%.2f".format(audioState.rms))
                InspectorRow("Beat", if (audioState.beatConfidence > 0.5f) "YES" else "NO")
                InspectorRow("Silent", if (audioState.isSilent) "YES" else "NO")
                InspectorRow("Transient", "%.2f".format(audioState.transient))
                InspectorRow("Envelope", "%.2f".format(audioState.envelope))
                InspectorRow("Playing", if (audioState.isPlaying) "YES" else "NO")
            }

            Spacer(Modifier.height(4.dp))

            // Color State Section
            InspectorSection("COLOR") {
                InspectorRow("Primary", colorState.primary.toHexString())
                InspectorRow("Secondary", colorState.secondary.toHexString())
                InspectorRow("WaveStart", colorState.waveStart.toHexString())
                InspectorRow("WaveEnd", colorState.waveEnd.toHexString())
                InspectorRow("Temp", colorState.temperature.name)
                InspectorRow("Luminance", "%.2f".format(colorState.dominantLuminance))
                InspectorRow("Mono", if (colorState.isMonochrome) "YES" else "NO")
                InspectorRow("Dark Art", if (colorState.isDarkArtwork) "YES" else "NO")
                InspectorRow("Contrast", "%.2f".format(colorState.contrastLevel))
                InspectorRow("Pulse", "%.2f".format(colorState.pulseAmount))
                InspectorRow("Tint", "%.2f".format(colorState.tintStrength))
                InspectorRow("Intensity", "%.2f".format(colorState.energyIntensity))
                InspectorRow("From Art", if (colorState.isFromArtwork) "YES" else "NO")
            }

            Spacer(Modifier.height(4.dp))

            // Color preview strip
            Row(
                modifier = Modifier.fillMaxWidth().height(20.dp),
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
        }
    }
}

@Composable
private fun InspectorSection(title: String, content: @Composable () -> Unit) {
    Text(
        text = "── $title ──",
        color = Color(0xFF667788),
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace
    )
    content()
}

@Composable
private fun InspectorRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
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
            color = Color(0xFFDDDDDD),
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
            .height(20.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color)
    )
}

private fun Color.toHexString(): String {
    val argb = this.toArgb()
    return "#${argb.toString(16).uppercase().padStart(8, '0')}"
}
