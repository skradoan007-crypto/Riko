package com.rikodev.music.android.core.experience.wave.render

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — GLASS RIBBON RENDERER
//
// Draws glass effects ON TOP of the ribbon body.
//
// This is NOT physics. This is NOT computation.
// This reads HighlightState.glassOpacity, fresnel, reflectionAlpha
// and paints visual glass effects on the ribbon surface.
//
// ── EFFECTS ────────────────────────────────────────────────────────────────
//   1. Soft Frost:          A subtle white noise overlay on the ribbon surface.
//                             Opacity driven by glassOpacity. Never opaque.
//   2. Edge Light:           Bright line along the ribbon's top edge.
//                             Intensity driven by fresnel. Follows the curve.
//   3. Internal Reflection:  A mirrored gradient inside the ribbon body.
//                             Shifted by reflectionOffset from center.
//                             Opacity driven by reflectionAlpha.
//   4. Surface Sheen:        A thin highlight band across the ribbon width.
//                             Position follows lightDirection from HighlightState.
//
// ── RULES ──────────────────────────────────────────────────────────────────
//   #1: NO AUDIO. NO FFT. NO ENERGY. Pure visual glass rendering.
//   #2: Reads ONLY WaveState.path + WaveState.highlight + ColorState.
//   #3: Glass is additive — layered on top of the ribbon, never replaces it.
//   #4: Glass effects scale with glassOpacity from HighlightState.
//   #5: When glassOpacity < 0.05, this renderer is entirely skipped (WaveCanvas).
//
// PRODUCER: Phase 3.4C
// CONSUMER: WaveCanvas (3.4A) — called as Layer 2
//
// ⭐ GOLDEN RULE ⭐
//   GlassRibbonRenderer NEVER computes physics.
//   GlassRibbonRenderer ONLY reads pre-computed data and draws.
// ─────────────────────────────────────────────────────────────────────────────

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.rikodev.music.android.core.experience.ColorState
import com.rikodev.music.android.core.experience.calibration.WaveCalibration
import com.rikodev.music.android.core.experience.wave.BezierControl
import com.rikodev.music.android.core.experience.wave.HighlightPoint
import com.rikodev.music.android.core.experience.wave.HighlightState
import com.rikodev.music.android.core.experience.wave.OrganicPath
import com.rikodev.music.android.core.experience.wave.RibbonPoint
import com.rikodev.music.android.core.experience.wave.WaveState
import kotlin.math.abs
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas

/**
 * Renders glass visual effects on the ribbon surface.
 *
 * This object is stateless — all state comes from WaveState and ColorState.
 * It creates zero long-lived allocations: reusable paths are reset each frame.
 */
object GlassRibbonRenderer {

    // ── Rendering Constants ──────────────────────────────────────────────

    /**
     * Frost base color — cool white, very subtle.
     * Modulated by glassOpacity: never fully white, always translucent.
     */
    private val FROST_COLOR = Color(0x20FFFFFF)

    /**
     * Edge light color — bright white-cyan for the ribbon's top edge.
     * Intensity modulated by fresnel and energy.
     */
    private val EDGE_LIGHT_COLOR = Color(0x40FFFFFF)

    // ── Reusable Paths (zero allocation per frame) ───────────────────────

    /** Path for the edge light stroke along the top of the ribbon. */
    private val edgeLightPath = Path()

    /** Path for the internal reflection gradient band. */
    private val reflectionPath = Path()

    /** Path for the surface sheen highlight band. */
    private val sheenPath = Path()

    /** Reusable path for the ribbon body (shared with RibbonRenderer's approach for frost). */
    private val ribbonPath = Path()

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Draw glass effects onto the canvas.
     *
     * Called by WaveCanvas as Layer 2 (glass layer).
     * Only called when state.needsGlass == true.
     *
     * @param scope The DrawScope (canvas context)
     * @param state Current WaveState (contains path, highlight)
     * @param color Current ColorState (provides tint, glass colors)
     */
    fun draw(scope: DrawScope, state: WaveState, color: ColorState) {
        val path = state.path
        val highlight = state.highlight
        if (!path.hasGeometry || highlight.points.isEmpty()) return

        val w = scope.size.width
        val h = scope.size.height
        val maxThickness = h * WaveCalibration.Layout.MAX_THICKNESS_FRAC

        // ── Effect 1: Edge Light (top edge glow) ─────────────────────────
        drawEdgeLight(scope, path, highlight, w, h, maxThickness, color)

        // ── Effect 2: Internal Reflection ──────────────────────────────────
        if (highlight.needsReflection) {
            drawInternalReflection(scope, path, highlight, w, h, maxThickness, color)
        }

        // ── Effect 3: Surface Sheen ───────────────────────────────────────
        if (highlight.fresnel > 0.05f) {
            drawSurfaceSheen(scope, path, highlight, w, h, maxThickness, color)
        }

        // ── Effect 4: Soft Frost Overlay ──────────────────────────────────
        if (highlight.glassOpacity > 0.1f) {
            drawSoftFrost(scope, path, w, h, maxThickness, highlight, color)
        }
    }

    // ── Effect 1: Edge Light ───────────────────────────────────────────────

    /**
     * Draw a bright line along the ribbon's top edge.
     * The edge light follows the ribbon's bezier curve exactly.
     * Intensity varies per-point based on fresnel + edgeGlow.
     */
    private fun drawEdgeLight(
        scope: DrawScope,
        path: OrganicPath,
        highlight: HighlightState,
        w: Float,
        h: Float,
        maxThickness: Float,
        color: ColorState
    ) {
        val points = path.points
        val controls = path.controls
        val hlPoints = highlight.points
        if (points.size < 2 || controls.isEmpty()) return

        edgeLightPath.reset()

        val halfThick = FloatArray(points.size)
        for (i in points.indices) {
            val t = points[i].thickness.coerceIn(0f, 1f)
            halfThick[i] = (t * maxThickness * 0.5f)
                .coerceAtLeast(WaveCalibration.Layout.MIN_THICKNESS_PX * 0.5f)
        }

        fun cx(nx: Float) = WaveCalibration.Layout.H_PAD + nx * WaveCalibration.Layout.H_SPAN * w
        fun cy(ny: Float) = h * 0.5f + ny * WaveCalibration.Layout.Y_SCALE * h

        // Build path along the top edge (center - halfThickness)
        edgeLightPath.moveTo(
            cx(points[0].x),
            cy(points[0].y) - halfThick[0]
        )

        for (i in 0 until controls.size) {
            val ctrl = controls[i]
            val next = points[i + 1]
            val ctrlHalfT = (halfThick[i] + halfThick[i + 1]) * 0.5f

            edgeLightPath.cubicTo(
                cx(ctrl.cp1X),
                cy(ctrl.cp1Y) - ctrlHalfT,
                cx(ctrl.cp2X),
                cy(ctrl.cp2Y) - ctrlHalfT,
                cx(next.x),
                cy(next.y) - halfThick[i + 1]
            )
        }

        // Average fresnel + edge glow for the stroke alpha
        val avgFresnel = hlPoints.map { it.fresnel }.average().toFloat().coerceIn(0f, 1f)
        val avgEdgeGlow = hlPoints.map { it.edgeGlow }.average().toFloat().coerceIn(0f, 1f)
        val edgeAlpha = (avgFresnel * 0.6f + avgEdgeGlow * 0.4f)
            .coerceIn(0f, 0.8f)

        val lightColor = if (color.isIdle) {
            EDGE_LIGHT_COLOR.copy(alpha = edgeAlpha * 0.5f)
        } else {
            color.primary.copy(alpha = edgeAlpha)
        }

        scope.drawPath(
            path = edgeLightPath,
            color = lightColor,
            style = Stroke(
                width = 1.5f,
                pathEffect = PathEffect.cornerPathEffect(2f)
            )
        )

        // Bottom edge (dimmer)
        edgeLightPath.reset()
        edgeLightPath.moveTo(
            cx(points[0].x),
            cy(points[0].y) + halfThick[0]
        )

        for (i in 0 until controls.size) {
            val ctrl = controls[i]
            val next = points[i + 1]
            val ctrlHalfT = (halfThick[i] + halfThick[i + 1]) * 0.5f

            edgeLightPath.cubicTo(
                cx(ctrl.cp1X),
                cy(ctrl.cp1Y) + ctrlHalfT,
                cx(ctrl.cp2X),
                cy(ctrl.cp2Y) + ctrlHalfT,
                cx(next.x),
                cy(next.y) + halfThick[i + 1]
            )
        }

        scope.drawPath(
            path = edgeLightPath,
            color = lightColor.copy(alpha = edgeAlpha * 0.3f),
            style = Stroke(
                width = 1f,
                pathEffect = PathEffect.cornerPathEffect(2f)
            )
        )
    }

    // ── Effect 2: Internal Reflection ────────────────────────────────────

    /**
     * Draw a mirrored gradient band inside the ribbon.
     * The reflection is offset from center by reflectionOffset.
     * Uses the wave gradient colors but at reduced opacity.
     */
    private fun drawInternalReflection(
        scope: DrawScope,
        path: OrganicPath,
        highlight: HighlightState,
        w: Float,
        h: Float,
        maxThickness: Float,
        color: ColorState
    ) {
        val points = path.points
        if (points.size < 2) return

        val halfThick = FloatArray(points.size)
        for (i in points.indices) {
            val t = points[i].thickness.coerceIn(0f, 1f)
            halfThick[i] = (t * maxThickness * 0.5f)
                .coerceAtLeast(WaveCalibration.Layout.MIN_THICKNESS_PX * 0.5f)
        }

        fun cx(nx: Float) = WaveCalibration.Layout.H_PAD + nx * WaveCalibration.Layout.H_SPAN * w
        fun cy(ny: Float) = h * 0.5f + ny * WaveCalibration.Layout.Y_SCALE * h

        // Reflection sits between center and the offset position
        val offset = highlight.reflectionOffset.coerceIn(0f, 1f)
        val reflBandWidth = maxThickness * 0.2f
        val reflCenter = halfThick.map { it * (1f - offset * 2f) }
            .average().toFloat()

        // Build a thin band path for the reflection
        reflectionPath.reset()

        val bandTop = reflCenter - reflBandWidth * 0.5f
        val bandBottom = reflCenter + reflBandWidth * 0.5f

        // Simple band: straight lines (reflection is subtle, doesn't need bezier)
        reflectionPath.moveTo(cx(points.first().x), cy(points.first().y) + bandTop)

        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            reflectionPath.lineTo(
                cx(p2.x),
                cy(p2.y) + bandTop
            )
        }

        for (i in points.indices.reversed()) {
            val p = points[i]
            reflectionPath.lineTo(
                cx(p.x),
                cy(p.y) + bandBottom
            )
        }

        reflectionPath.close()

        val reflAlpha = (highlight.reflectionAlpha * WaveCalibration.GlassRender.REFLECTION_STRENGTH)
            .coerceIn(0f, 0.4f)

        val reflColor = if (color.isIdle) {
            Color.White.copy(alpha = reflAlpha * 0.3f)
        } else {
            color.waveStart.copy(alpha = reflAlpha)
        }

        scope.drawPath(
            path = reflectionPath,
            color = reflColor,
            blendMode = BlendMode.Overlay
        )
    }

    // ── Effect 3: Surface Sheen ──────────────────────────────────────────

    /**
     * Draw a thin highlight band across the ribbon width.
     * Position modulated by fresnel — higher fresnel = sheen moves to edges.
     * Creates the "glass catch light" effect.
     */
    private fun drawSurfaceSheen(
        scope: DrawScope,
        path: OrganicPath,
        highlight: HighlightState,
        w: Float,
        h: Float,
        maxThickness: Float,
        color: ColorState
    ) {
        val points = path.points
        if (points.size < 2) return

        val halfThick = FloatArray(points.size)
        for (i in points.indices) {
            val t = points[i].thickness.coerceIn(0f, 1f)
            halfThick[i] = (t * maxThickness * 0.5f)
                .coerceAtLeast(WaveCalibration.Layout.MIN_THICKNESS_PX * 0.5f)
        }

        fun cx(nx: Float) = WaveCalibration.Layout.H_PAD + nx * WaveCalibration.Layout.H_SPAN * w
        fun cy(ny: Float) = h * 0.5f + ny * WaveCalibration.Layout.Y_SCALE * h

        // Sheen sits in the upper portion of the ribbon
        val sheenHeight = halfThick.map { it * WaveCalibration.GlassRender.SHEEN_HEIGHT_FRAC }
            .average().toFloat()
            .coerceAtLeast(WaveCalibration.Layout.MIN_THICKNESS_PX)

        // Fresnel pushes the sheen toward the edge (away from center)
        val fresnelShift = highlight.fresnel * 0.3f
        val sheenY = -halfThick.map { it * (0.2f + fresnelShift) }
            .average().toFloat()

        sheenPath.reset()

        // Build the sheen band along the ribbon
        sheenPath.moveTo(
            cx(points.first().x),
            cy(points.first().y) + sheenY
        )

        for (i in 0 until points.size - 1) {
            val p = points[i + 1]
            sheenPath.lineTo(
                cx(p.x),
                cy(p.y) + sheenY
            )
        }

        // Close: go back along bottom of sheen band
        for (i in points.indices.reversed()) {
            val p = points[i]
            sheenPath.lineTo(
                cx(p.x),
                cy(p.y) + sheenY + sheenHeight
            )
        }

        sheenPath.close()

        val sheenAlpha = (highlight.fresnel * WaveCalibration.GlassRender.MAX_SHEEN_ALPHA)
            .coerceIn(0f, WaveCalibration.GlassRender.MAX_SHEEN_ALPHA)

        val sheenColor = if (color.isIdle) {
            Color.White.copy(alpha = sheenAlpha * 0.2f)
        } else {
            Color.White.copy(alpha = sheenAlpha * 0.35f)
        }

        scope.drawPath(
            path = sheenPath,
            color = sheenColor,
            blendMode = BlendMode.Softlight
        )
    }

    // ── Effect 4: Soft Frost ────────────────────────────────────────────

    /**
     * Draw a subtle frosted overlay on the entire ribbon.
     * This simulates the "frosted glass" look — a thin white layer
     * that softens the ribbon surface slightly.
     * Opacity is very low — it should be barely perceptible.
     */
    private fun drawSoftFrost(
        scope: DrawScope,
        path: OrganicPath,
        w: Float,
        h: Float,
        maxThickness: Float,
        highlight: HighlightState,
        color: ColorState
    ) {
        // Reuse ribbon body path from RibbonRenderer's approach
        ribbonPath.reset()
        RibbonRenderer.buildRibbonPath(
            points = path.points,
            controls = path.controls,
            w = w,
            h = h,
            maxThickness = maxThickness,
            path = ribbonPath
        )

        val frostAlpha = (highlight.glassOpacity * 0.12f)
            .coerceIn(0f, 0.15f)

        scope.drawPath(
            path = ribbonPath,
            color = FROST_COLOR.copy(alpha = frostAlpha),
            blendMode = BlendMode.Overlay
        )
    }
}
