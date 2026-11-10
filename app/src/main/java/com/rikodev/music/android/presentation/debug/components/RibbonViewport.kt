package com.rikodev.music.android.presentation.debug.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.experience.wave.WaveState
import com.rikodev.music.android.core.experience.wave.render.InspectorOverlay
import com.rikodev.music.android.core.experience.wave.render.WaveCanvas
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.text.TextMeasurer

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — RIBBON VIEWPORT
//
// The MOST IMPORTANT part of the Wave Laboratory screen.
//
// This is where the ribbon lives. Nothing else.
//
// RULES:
//   ❌ No playlist
//   ❌ No lyrics
//   ❌ No bottom navigation
//   ❌ No mini player
//   ❌ No queue
//   ❌ No buttons
//
//   ✅ Album Art
//   ✅ Ribbon
//   ✅ Black Background
//
// WHY?
//   We are tuning the Ribbon. Not the App.
//   Everything stripped away except what matters.
//
// PRODUCER: Phase 3.4J.2
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RibbonViewport(
    waveState: StateFlow<WaveState>,
    colorState: StateFlow<com.rikodev.music.android.core.experience.ColorState>,
    modifier: Modifier = Modifier,
    showInspector: Boolean = false,
    fps: Int = 0,
    drawTimeMs: Float = 0f,
    qualityName: String = "BALANCED"
) {
    val textMeasurer = rememberTextMeasurer()
    val overlay = rememberInspectorOverlay(textMeasurer, showInspector, fps, drawTimeMs, qualityName)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Color.Black)
            .clipToBounds()
    ) {
        // Simulated album art background (subtle radial gradient)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width * 0.6f
            drawCircle(
                brush = ShaderBrush(
                    RadialGradientShader(
                        center = center,
                        radius = radius,
                        colors = listOf(
                            Color(0xFF0A1628),
                            Color(0xFF050A14),
                            Color.Black
                        )
                    )
                ),
                center = center,
                radius = radius
            )
        }

        // The ribbon canvas
        WaveCanvas(
            waveState = waveState,
            colorState = colorState,
            modifier = Modifier.fillMaxSize(),
            debugOverlay = overlay
        )
    }
}

private fun rememberInspectorOverlay(
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    showInspector: Boolean,
    fps: Int,
    drawTimeMs: Float,
    qualityName: String
): InspectorOverlay? {
    return if (showInspector) {
        InspectorOverlay(textMeasurer).apply {
            isEnabled = true
            this.currentFps = fps
            this.drawTimeMs = drawTimeMs
            this.qualityName = qualityName
        }
    } else {
        null
    }
}
