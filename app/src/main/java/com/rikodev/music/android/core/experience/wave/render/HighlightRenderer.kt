package com.rikodev.music.android.core.experience.wave.render

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — HIGHLIGHT RENDERER
//
// Draws specular highlights, fresnel effects, and edge shine on the ribbon.
//
// This is the PREMIUM layer — the one that makes the ribbon look expensive.
// It reads HighlightState per-point data and converts it into gradient brushes,
// point highlights, and edge glow strokes.
//
// ── RIKO RULES #12 and #15 ────────────────────────────────────────────────
//   #12: Treble creates sparkle. Bass creates body. Highlight creates premium.
//   #15: More energy ≠ More white.
//        High energy = slightly WIDER highlight + STRONGER fresnel.
//        Subtle. Premium. Never garish.
//
// ── EFFECTS ────────────────────────────────────────────────────────────────
//   1. Specular Gradient:   A horizontal gradient brush that follows the ribbon.
//                            Brighter at high-curvature points (Rule #14).
//                            Width modulated by highlightWidth.
//   2. Fresnel Rim:          Edge glow that intensifies at grazing angles.
//                            Uses HighlightPoint.fresnel per-point.
//   3. Edge Shine:           Bright sparkle dots at high-curvature peaks.
//                            Uses HighlightPoint.edgeGlow + HighlightPoint.specularIntensity.
//                            Size modulated by energy, not brightness (Rule #15).
//
// ── RULES ──────────────────────────────────────────────────────────────────
//   #1: NO AUDIO. NO FFT. NO ENERGY computation. Pure visual rendering.
//   #2: Reads ONLY WaveState.highlight + WaveState.path + ColorState.
//   #3: Per-point data is read directly — never recomputed.
//   #4: Specular is a GRADIENT BRUSH, not a flat color.
//   #5: More energy = wider highlight band, NOT brighter pixels (Rule #15).
//
// PRODUCER: Phase 3.4D
// CONSUMER: WaveCanvas (3.4A) — called as Layer 3
//
// ⭐ GOLDEN RULE ⭐
//   HighlightRenderer NEVER computes physics.
//   HighlightRenderer ONLY reads pre-computed data and paints.
// ─────────────────────────────────────────────────────────────────────────────

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.rikodev.music.android.core.experience.ColorState
import com.rikodev.music.android.core.experience.wave.HighlightPoint
import com.rikodev.music.android.core.experience.wave.HighlightState
import com.rikodev.music.android.core.experience.wave.OrganicPath
import com.rikodev.music.android.core.experience.wave.RibbonPoint
import com.rikodev.music.android.core.experience.wave.WaveState
import com.rikodev.music.android.core.experience.calibration.WaveCalibration
import kotlin.math.abs
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas

/**
 * Renders specular highlights, fresnel effects, and edge shine.
 *
 * This is the "premium feel" renderer — it transforms pre-computed
 * highlight data into visual light effects on the ribbon surface.
 */
object HighlightRenderer {

    // ── Rendering Constants ──────────────────────────────────────────────

    /**
     * Maximum specular glow radius in pixels.
     * Scales with highlightWidth from HighlightState.
     * Rule #15: wider, not brighter.
     *
     * Note: Kept local because it's a rendering pixel cap not in WaveCalibration.
     */
    private const val MAX_SPECULAR_RADIUS = 40f

    // ── Reusable Path ───────────────────────────────────────────────────

    /** Path for specular band gradient. */
    private val specularPath = Path()

    /** Path for fresnel rim stroke. */
    private val fresnelPath = Path()

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Draw highlight effects onto the canvas.
     *
     * Called by WaveCanvas as Layer 3 (top layer).
     * Only called when state.needsHighlight == true.
     *
     * @param scope The DrawScope (canvas context)
     * @param state Current WaveState (contains path, highlight)
     * @param color Current ColorState (provides primary/secondary for gradients)
     */
    fun draw(scope: DrawScope, state: WaveState, color: ColorState) {
        val path = state.path
        val highlight = state.highlight
        if (!path.hasGeometry || highlight.points.isEmpty()) return

        val w = scope.size.width
        val h = scope.size.height
        val maxThickness = h * WaveCalibration.Layout.MAX_THICKNESS_FRAC

        // ── Effect 1: Specular Gradient Band ─────────────────────────────
        drawSpecularBand(scope, path, highlight, w, h, maxThickness, color)

        // ── Effect 2: Fresnel Rim Glow ───────────────────────────────────
        if (highlight.fresnel > 0.08f) {
            drawFresnelRim(scope, path, highlight, w, h, maxThickness, color)
        }

        // ── Effect 3: Peak Sparkle Dots ───────────────────────────────────
        drawPeakSparkles(scope, path, highlight, w, h, maxThickness, color)
    }

    // ── Effect 1: Specular Gradient Band ───────────────────────────────────

    /**
     * Draw a horizontal gradient band that follows the ribbon.
     * The gradient varies per-point based on specularIntensity.
     * Width varies based on highlightWidth (Rule #15: wider, not brighter).
     *
     * The band sits in the upper portion of the ribbon,
     * offset from center by the light direction.
     */
    private fun drawSpecularBand(
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

        val halfThick = computeHalfThickness(points, maxThickness)
        fun cx(nx: Float) = WaveCalibration.Layout.H_PAD + nx * WaveCalibration.Layout.H_SPAN * w
        fun cy(ny: Float) = h * 0.5f + ny * WaveCalibration.Layout.Y_SCALE * h

        // Specular band position: upper portion of ribbon, shifted by light direction
        val bandWidth = halfThick.map { it * (WaveCalibration.HighlightRender.SPECULAR_BAND_FRAC + highlight.highlightWidth * 0.3f) }
            .average().toFloat()
            .coerceAtLeast(WaveCalibration.Layout.MIN_THICKNESS_PX)

        // Position: offset toward top based on average light direction
        val bandY = -halfThick.map { it * (0.15f + highlight.lightDirection * 0.2f) }
            .average().toFloat()

        specularPath.reset()

        // Build the specular band as a filled polygon along the ribbon
        specularPath.moveTo(
            cx(points.first().x),
            cy(points.first().y) + bandY
        )

        for (i in 0 until points.size - 1) {
            specularPath.lineTo(
                cx(points[i + 1].x),
                cy(points[i + 1].y) + bandY
            )
        }

        for (i in points.indices.reversed()) {
            specularPath.lineTo(
                cx(points[i].x),
                cy(points[i].y) + bandY + bandWidth
            )
        }

        specularPath.close()

        // Specular intensity-based alpha
        // Rule #15: alpha is subtle, width does the talking
        val specularAlpha = (highlight.specularIntensity * 0.35f)
            .coerceIn(0f, 0.4f)

        // Gradient brush: primary color modulated by per-point specular
        val startColor = color.primary
        val endColor = color.secondary

        val brush = Brush.horizontalGradient(
            colors = listOf(
                startColor.copy(alpha = specularAlpha * 0.5f),
                startColor.copy(alpha = specularAlpha),
                endColor.copy(alpha = specularAlpha),
                endColor.copy(alpha = specularAlpha * 0.5f)
            ),
            startX = cx(0f),
            endX = cx(1f)
        )

        scope.drawPath(
            path = specularPath,
            brush = brush,
            blendMode = BlendMode.Screen
        )
    }

    // ── Effect 2: Fresnel Rim Glow ────────────────────────────────────────

    /**
     * Draw the fresnel rim glow along the ribbon's edge.
     * Fresnel is strongest at edges (grazing angles) and weakest at center.
     * The rim glow follows the ribbon outline exactly.
     */
    private fun drawFresnelRim(
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
        if (points.size < 2 || controls.isEmpty()) return

        val halfThick = computeHalfThickness(points, maxThickness)
        fun cx(nx: Float) = WaveCalibration.Layout.H_PAD + nx * WaveCalibration.Layout.H_SPAN * w
        fun cy(ny: Float) = h * 0.5f + ny * WaveCalibration.Layout.Y_SCALE * h

        // Build the ribbon outline for fresnel stroke (same as RibbonRenderer)
        fresnelPath.reset()
        RibbonRenderer.buildRibbonPath(
            points = points,
            controls = controls,
            w = w,
            h = h,
            maxThickness = maxThickness * 1.02f, // Slightly expanded for rim
            path = fresnelPath
        )

        // Fresnel alpha: subtle, modulated by overall fresnel strength
        val rimAlpha = (highlight.fresnel * 0.25f)
            .coerceIn(0f, 0.35f)

        val rimColor = if (color.isIdle) {
            Color.White.copy(alpha = rimAlpha * 0.3f)
        } else {
            color.primary.copy(alpha = rimAlpha)
        }

        scope.drawPath(
            path = fresnelPath,
            color = rimColor,
            style = Stroke(
                width = WaveCalibration.HighlightRender.FRESNEL_STROKE_WIDTH * highlight.fresnel.coerceIn(0.5f, 1f),
                pathEffect = PathEffect.cornerPathEffect(3f)
            )
        )
    }

    // ── Effect 3: Peak Sparkle Dots ──────────────────────────────────────

    /**
     * Draw radial glow dots at high-curvature, high-specular peaks.
     * Rule #14: edge glow is caused by curvature, not opacity.
     * Rule #15: more energy = larger glow, not brighter glow.
     *
     * Only the top MAX_PEAK_SPARKLES peaks are drawn to avoid overdraw.
     */
    private fun drawPeakSparkles(
        scope: DrawScope,
        path: OrganicPath,
        highlight: HighlightState,
        w: Float,
        h: Float,
        maxThickness: Float,
        color: ColorState
    ) {
        val points = path.points
        val hlPoints = highlight.points
        if (points.size != hlPoints.size) return

        fun cx(nx: Float) = WaveCalibration.Layout.H_PAD + nx * WaveCalibration.Layout.H_SPAN * w
        fun cy(ny: Float) = h * 0.5f + ny * WaveCalibration.Layout.Y_SCALE * h

        // Find peaks: points with high specular + high edge glow
        val peaks = hlPoints.mapIndexed { i, hl ->
            val combined = hl.specularIntensity * 0.6f + hl.edgeGlow * 0.4f
            Triple(i, combined, hl)
        }
            .filter { it.second > 0.15f }
            .sortedByDescending { it.second }
            .take(WaveCalibration.HighlightRender.MAX_PEAK_SPARKLES)

        if (peaks.isEmpty()) return

        // Draw each peak as a radial glow dot
        for ((index, intensity, hl) in peaks) {
            if (index >= points.size) continue
            val point = points[index]

            // Rule #15: radius scales with energy, NOT alpha
            val radius = (WaveCalibration.HighlightRender.MIN_SPECULAR_RADIUS +
                (MAX_SPECULAR_RADIUS - WaveCalibration.HighlightRender.MIN_SPECULAR_RADIUS) * intensity)
                .coerceIn(WaveCalibration.HighlightRender.MIN_SPECULAR_RADIUS, MAX_SPECULAR_RADIUS)

            // Alpha stays subtle
            val alpha = (intensity * 0.3f).coerceIn(0f, 0.35f)

            val center = Offset(cx(point.x), cy(point.y))

            // Radial gradient glow
            val glowColor = if (color.isIdle) {
                Color.White.copy(alpha = alpha * 0.5f)
            } else {
                color.accent.copy(alpha = alpha)
            }

            scope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor,
                        glowColor.copy(alpha = 0f)
                    ),
                    center = center,
                    radius = radius
                ),
                center = center,
                radius = radius,
                blendMode = BlendMode.Screen
            )
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Compute half-thickness array for all points.
     * Shared utility used by all sub-effects.
     */
    private fun computeHalfThickness(points: List<RibbonPoint>, maxThickness: Float): FloatArray {
        val halfThick = FloatArray(points.size)
        for (i in points.indices) {
            val t = points[i].thickness.coerceIn(0f, 1f)
            halfThick[i] = (t * maxThickness * 0.5f)
                .coerceAtLeast(WaveCalibration.Layout.MIN_THICKNESS_PX * 0.5f)
        }
        return halfThick
    }
}
