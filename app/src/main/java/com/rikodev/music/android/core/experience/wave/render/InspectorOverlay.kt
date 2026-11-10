package com.rikodev.music.android.core.experience.wave.render

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — INSPECTOR OVERLAY (V2)
//
// Live performance overlay drawn ON TOP of the render layers.
//
// This is the debug/tuning HUD for the Wave render pipeline.
// It displays real-time metrics: FPS, draw time, segment count,
// pressure, momentum, quality level, and engine status.
//
// ── WHAT IT SHOWS ──────────────────────────────────────────────────────────
//   Zone 1: Performance    FPS, Draw Time, Skipped Frames
//   Zone 2: Geometry       Segments, Points, Curvature
//   Zone 3: Energy         Overall, Bass, Mid, Treble
//   Zone 4: Ribbon          Thickness, Opacity, Mode
//   Zone 5: Highlight       Specular, Fresnel, Glass, Shadow
//   Zone 6: Quality         Current Quality Level
//
// ── RULES ──────────────────────────────────────────────────────────────────
//   #1: Overlay NEVER affects render layers below it.
//   #2: Overlay is drawn in a dedicated pass (last in WaveCanvas).
//   #3: No physics computation — pure read + display.
//   #4: When production build, overlay should be disabled.
//
// PRODUCER: Phase 3.4I
// CONSUMER: WaveCanvas (as WaveDebugOverlay — Step 3.4A)
// ─────────────────────────────────────────────────────────────────────────────

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.rikodev.music.android.core.experience.wave.WaveState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas

/**
 * Live performance overlay for the Wave render pipeline.
 *
 * Implements [WaveDebugOverlay] to be passed to WaveCanvas.
 * Requires a [TextMeasurer] obtained from `rememberTextMeasurer()`
 * in the Composable context.
 *
 * Usage in a @Composable:
 *   val textMeasurer = rememberTextMeasurer()
 *   val overlay = remember { InspectorOverlay(textMeasurer) }
 *   WaveCanvas(..., debugOverlay = overlay)
 */
class InspectorOverlay(
    private val textMeasurer: TextMeasurer
) : WaveDebugOverlay {

    // ── Configuration ────────────────────────────────────────────────────

    /** Whether the overlay is currently visible. Toggle at runtime. */
    var isEnabled: Boolean = true

    /** Current FPS value (updated externally from WaveDiagnostics). */
    var currentFps: Int = 0

    /** Draw time in ms (updated externally from RenderPerformance.DrawTimer). */
    var drawTimeMs: Float = 0f

    /** Quality level name string. */
    var qualityName: String = "BALANCED"

    /** Skipped frames count in last second. */
    var skippedFrames: Int = 0

    // ── Colors ───────────────────────────────────────────────────────────

    private val BG_COLOR = Color(0xCC000000)
    private val LABEL_COLOR = Color(0xFF888888)
    private val VALUE_COLOR = Color(0xFF00FF88)
    private val WARN_COLOR = Color(0xFFFF6644)
    private val HEADER_COLOR = Color(0xFF4488FF)

    // ── Text Styles ─────────────────────────────────────────────────────

    private val headerStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        color = HEADER_COLOR
    )

    private val labelStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 9.sp,
        color = LABEL_COLOR
    )

    private val valueStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 9.sp,
        color = VALUE_COLOR
    )

    private val warnStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 9.sp,
        color = WARN_COLOR
    )

    // ── WaveDebugOverlay Implementation ──────────────────────────────────

    override fun draw(scope: DrawScope, state: WaveState) {
        if (!isEnabled) return

        val w = scope.size.width
        val lineH = 14f

        val leftX = 12f
        val col1X = leftX
        val col2X = w * 0.55f
        val startY = 16f

        // Background panel
        scope.drawRect(
            color = BG_COLOR,
            topLeft = Offset(leftX - 4f, startY - 4f),
            size = Size(w - 24f, lineH * 14.5f)
        )

        // ── Column 1: Performance + Geometry + Quality ───────────────

        drawText(scope, "── PERFORMANCE ──", col1X, startY, headerStyle)

        val fpsStyle = if (currentFps >= 55) valueStyle else warnStyle
        drawRow(scope, "FPS", "$currentFps", col1X, startY + lineH, fpsStyle)
        val dtStyle = if (drawTimeMs < 12f) valueStyle else warnStyle
        drawRow(scope, "Draw", "%.1fms".format(drawTimeMs), col1X, startY + lineH * 2, dtStyle)
        drawRow(scope, "Skip", "$skippedFrames", col1X, startY + lineH * 3)

        // Geometry
        drawText(scope, "── GEOMETRY ──", col1X, startY + lineH * 4.5f, headerStyle)
        drawRow(scope, "Segs", "${state.path.segmentCount}", col1X, startY + lineH * 5.5f)
        drawRow(scope, "Pts", "${state.path.pointCount}", col1X, startY + lineH * 6.5f)

        // Quality
        drawText(scope, "── QUALITY ──", col1X, startY + lineH * 8f, headerStyle)
        drawRow(scope, "Level", qualityName, col1X, startY + lineH * 9f)

        // ── Column 2: Energy + Ribbon + Highlight ──────────────────

        drawText(scope, "── ENERGY ──", col2X, startY, headerStyle)
        drawRow(scope, "Overall", fmt(state.overallEnergy), col2X, startY + lineH)
        drawRow(scope, "Bass", fmt(state.energy.bassPressure), col2X, startY + lineH * 2)
        drawRow(scope, "Mid", fmt(state.energy.midTexture), col2X, startY + lineH * 3)
        drawRow(scope, "Treble", fmt(state.energy.trebleSpark), col2X, startY + lineH * 4)

        // Ribbon
        drawText(scope, "── RIBBON ──", col2X, startY + lineH * 5.5f, headerStyle)
        drawRow(scope, "Mode", state.mode.name, col2X, startY + lineH * 6.5f)
        drawRow(scope, "Thick", fmt(state.ribbon.thickness), col2X, startY + lineH * 7.5f)

        // Highlight
        drawText(scope, "── HIGHLIGHT ──", col2X, startY + lineH * 9f, headerStyle)
        drawRow(scope, "Spec", fmt(state.highlight.specularIntensity), col2X, startY + lineH * 10f)
        drawRow(scope, "Fres", fmt(state.highlight.fresnel), col2X, startY + lineH * 11f)
        drawRow(scope, "Glass", fmt(state.highlight.glassOpacity), col2X, startY + lineH * 12f)
    }

    // ── Drawing Helpers ───────────────────────────────────────────────────

    private fun drawText(
        scope: DrawScope,
        text: String,
        x: Float,
        y: Float,
        style: TextStyle
    ) {
        scope.drawText(
            textMeasurer = textMeasurer,
            text = text,
            topLeft = Offset(x, y),
            style = style
        )
    }

    private fun drawRow(
        scope: DrawScope,
        label: String,
        value: String,
        x: Float,
        y: Float,
        valueStyle: TextStyle = this.valueStyle
    ) {
        drawText(scope, label, x, y, labelStyle)
        drawText(scope, value, x + 56f, y, valueStyle)
    }

    private fun fmt(value: Float): String = "%.2f".format(value)
}
