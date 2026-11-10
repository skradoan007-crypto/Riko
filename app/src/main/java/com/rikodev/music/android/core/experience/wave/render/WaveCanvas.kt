package com.rikodev.music.android.core.experience.wave.render

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — WAVE CANVAS
//
// The single entry point for all Wave rendering.
//
// This is a THIN composable. It does ONE thing:
//   1. Collect WaveState from the engine
//   2. Collect ColorState from Color Brain
//   3. Provide a Canvas
//   4. Delegate to renderers in the correct order
//
// It does NOT compute. It does NOT transform. It does NOT interpolate.
// It collects and delegates. Period.
//
// ── RIKO RULE #16 ──
//   Engine drives UI. UI never drives Engine.
//
//   The Canvas READS WaveState. It never writes.
//   The Canvas READS ColorState. It never writes.
//   If the engine is idle, the canvas draws idle.
//   If the engine is active, the canvas draws active.
//   The canvas has NO opinion about what to draw.
//
// ── RENDER ORDER (bottom to top) ──────────────────────────────────────────
//   Layer 0: Glow          (ambient bloom, beat pulse, aura, shadow bloom)
//   Layer 1: Ribbon        (the organic body — filled bezier path)
//   Layer 2: Glass         (refraction, frost, edge light, internal reflection)
//   Layer 3: Highlight      (specular, fresnel, edge shine — premium feel)
//
// Each layer is optional: skipped entirely when state indicates inactivity.
// No layer reaches into another layer's state.
// No layer contains audio logic, FFT, or energy computation.
//
// PRODUCER: Phase 3.4A (WaveCanvas — this file)
// CONSUMER: WavePreviewScreen (3.4H), NowPlayingScreen (3.4J ✅), WaveRibbonHost (3.4J)
//
// ⭐ GOLDEN RULE ⭐
//   WaveCanvas NEVER computes.
//   WaveCanvas ONLY collects and delegates.
// ─────────────────────────────────────────────────────────────────────────────

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.rikodev.music.android.core.experience.ColorState
import com.rikodev.music.android.core.experience.wave.WaveState
import kotlinx.coroutines.flow.StateFlow

/**
 * The single composable that renders the Wave Engine's output.
 *
 * Subscribes to the Wave Engine's [StateFlow<WaveState>] and Color Brain's
 * [StateFlow<ColorState>], then delegates all drawing to specialized renderers.
 *
 * @param waveState Flow of WaveState from WaveEngine.state
 * @param colorState Flow of ColorState from ColorIntelligenceEngine
 * @param modifier Compose modifier for layout
 * @param debugOverlay Optional debug overlay renderer (Inspector — Step 3.4I).
 *                    When null, no overlay is drawn.
 */
@Composable
fun WaveCanvas(
    waveState: StateFlow<WaveState>,
    colorState: StateFlow<ColorState>,
    modifier: Modifier = Modifier,
    debugOverlay: WaveDebugOverlay? = null
) {
    val state by waveState.collectAsStateWithLifecycle()
    val color by colorState.collectAsStateWithLifecycle()

    Canvas(modifier = modifier) {
        // ── Layer 0: Glow (behind everything) ─────────────────────────────
        if (state.isVisible) {
            GlowRenderer.draw(
                scope = this,
                state = state,
                color = color
            )
        }

        // ── Layer 1: Ribbon (the body) ────────────────────────────────────
        if (state.hasGeometry) {
            RibbonRenderer.draw(
                scope = this,
                state = state,
                color = color
            )
        }

        // ── Layer 2: Glass (on top of ribbon) ─────────────────────────────
        if (state.hasGeometry && state.needsGlass) {
            GlassRibbonRenderer.draw(
                scope = this,
                state = state,
                color = color
            )
        }

        // ── Layer 3: Highlight (specular, fresnel, edge shine) ────────────
        if (state.hasGeometry && state.needsHighlight) {
            HighlightRenderer.draw(
                scope = this,
                state = state,
                color = color
            )
        }

        // ── Debug Overlay (optional — Inspector V2 overlay) ────────────────
        if (debugOverlay != null) {
            debugOverlay.draw(this, state)
        }
    }
}

/**
 * Interface for debug overlay rendering.
 * Implemented by InspectorOverlay (Step 3.4I).
 *
 * The overlay draws ON TOP of everything — text, metrics, wireframes.
 * It has access to WaveState but should NOT modify the canvas clip
 * or transform in ways that affect the render layers below.
 */
interface WaveDebugOverlay {
    /**
     * Draw the debug overlay on the canvas.
     * Called AFTER all render layers have completed.
     *
     * @param scope The DrawScope (same canvas as renderers)
     * @param state Current WaveState snapshot
     */
    fun draw(scope: androidx.compose.ui.graphics.drawscope.DrawScope, state: WaveState)
}
