package com.rikodev.music.android.core.experience.wave.render

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — WAVE RIBBON HOST
//
// Convenience wrapper that connects a WaveEngine directly to WaveCanvas.
//
// This composable is the simplest possible bridge:
//   WaveEngine → WaveCanvas → Renderers → Screen
//
// In production, WaveCanvas is called directly from NowPlayingScreen
// with flows from PlayerViewModel (which injects WaveEngine).
//
// This wrapper exists for cases where a composable has direct access
// to a WaveEngine instance and wants a one-line ribbon:
//
//   WaveRibbonHost(waveEngine, colorStateFlow, modifier = Modifier.fillMaxSize())
//
// ── USAGE ────────────────────────────────────────────────────────────────────
//   Direct from ViewModel:
//     WaveRibbonHost(
//       waveEngine = waveEngine,
//       colorStateFlow = colorIntelligenceEngine.colorState
//     )
//
//   With debug overlay:
//     val textMeasurer = rememberTextMeasurer()
//     val overlay = remember(textMeasurer) { InspectorOverlay(textMeasurer) }
//     WaveRibbonHost(
//       waveEngine = waveEngine,
//       colorStateFlow = colorState,
//       debugOverlay = overlay
//     )
//
// PRODUCER: Phase 3.4J (Integration Milestone)
// CONSUMER: Any screen that wants a Wave ribbon with minimal setup
//
// ⭐ NOTE
//   In NowPlayingScreen, WaveCanvas is called directly (not through this
//   wrapper) because the ViewModel already exposes waveState + colorState.
//   This wrapper is for convenience in future screens (MiniPlayer, LockScreen,
//   Background Service, etc.) where the ViewModel may not be available.
// ─────────────────────────────────────────────────────────────────────────────

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rikodev.music.android.core.experience.ColorState
import com.rikodev.music.android.core.experience.wave.WaveEngine
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.foundation.Canvas

/**
 * One-line composable that renders the Wave ribbon from a WaveEngine instance.
 *
 * This is syntactic sugar over WaveCanvas — it reads the engine's
 * StateFlows and passes them to the render pipeline.
 *
 * @param waveEngine The WaveEngine instance (must already be observing audio)
 * @param colorStateFlow ColorState flow from Color Intelligence Engine
 * @param modifier Compose modifier for layout
 * @param debugOverlay Optional debug overlay (Inspector)
 */
@Composable
fun WaveRibbonHost(
    waveEngine: WaveEngine,
    colorStateFlow: StateFlow<ColorState>,
    modifier: Modifier = Modifier,
    debugOverlay: WaveDebugOverlay? = null
) {
    WaveCanvas(
        waveState = waveEngine.state,
        colorState = colorStateFlow,
        modifier = modifier,
        debugOverlay = debugOverlay
    )
}
