package com.rikodev.music.android.core.experience.wave.render

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — RIBBON RENDERER
//
// Draws the Organic Energy Ribbon™ body.
//
// This renderer converts the OrganicPath (center line + BezierControls + normals)
// into a filled Compose Path with varying thickness — the ink brush effect.
//
// ── RULES ──────────────────────────────────────────────────────────────────
//   #1: NO AUDIO. NO FFT. NO ENERGY. Pure geometry draw.
//   #2: Reads OrganicPath.points, OrganicPath.controls, RibbonPoint fields.
//   #3: Builds a filled Path by offsetting the center line along normals.
//   #4: Uses ColorState.waveStart/waveEnd for the gradient brush.
//   #5: Thickness varies per-point (ink brush profile from RibbonPoint.thickness).
//   #6: Opacity varies per-point (fade at head/tail from RibbonPoint.opacity).
//   #7: The ribbon is drawn as a CLOSED, FILLED polygon:
//         Top edge:  forward (left → right) using Bezier curves
//         Bottom edge: backward (right → left) using Bezier curves
//         Close path → fill with horizontal gradient
//
// ── GEOMETRY PIPELINE ──────────────────────────────────────────────────────
//   For each RibbonPoint[i]:
//     halfThickness[i] = point.thickness * canvasHeight * RIBBON_SCALE
//     topOffset[i] = Offset(point.x * w, point.y * h - halfThickness[i])
//     bottomOffset[i] = Offset(point.x * w, point.y * h + halfThickness[i])
//
//   For each BezierControl[i] (between point[i] and point[i+1]):
//     Interpolate thickness between points
//     Offset control points by interpolated normals
//
//   Top path:   moveTo(top[0]) → cubicTo(topCP1, topCP2, top[i+1]) ...
//   Bottom path: lineTo(bottom[N]) → cubicTo(bottomCP2, bottomCP1, bottom[i]) ...
//   Close → Fill
//
// PRODUCER: Phase 3.4B
// CONSUMER: WaveCanvas (3.4A) — called as Layer 1
//
// ⭐ GOLDEN RULE ⭐
//   RibbonRenderer NEVER computes physics.
//   RibbonRenderer ONLY reads geometry and draws.
// ─────────────────────────────────────────────────────────────────────────────

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.util.lerp
import com.rikodev.music.android.core.experience.ColorState
import com.rikodev.music.android.core.experience.calibration.WaveCalibration
import com.rikodev.music.android.core.experience.wave.BezierControl
import com.rikodev.music.android.core.experience.wave.OrganicPath
import com.rikodev.music.android.core.experience.wave.RibbonPoint
import com.rikodev.music.android.core.experience.wave.WaveState
import kotlin.math.abs
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.Canvas

/**
 * Renders the organic ribbon body from pre-computed geometry.
 *
 * This object is stateless — all state is passed in via parameters.
 * It can be called from any DrawScope without side effects.
 */
object RibbonRenderer {

    /**
     * Default ribbon color when ColorState is idle.
     * Subtle blue-gray — visible but not dominant.
     */
    private val IDLE_COLOR = Color(0xFF3A5F8A)

    // ── Pre-allocated Path (reused per frame for zero allocation) ────────

    /** Reusable path for the ribbon body. Reset before each draw call. */
    private val ribbonPath = Path()

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Draw the ribbon body onto the canvas.
     *
     * Called by WaveCanvas as Layer 1 (the body layer).
     *
     * @param scope The DrawScope (canvas context with size, density)
     * @param state Current WaveState snapshot (contains path, ribbon, mode)
     * @param color Current ColorState (provides wave gradient colors)
     */
    fun draw(scope: DrawScope, state: WaveState, color: ColorState) {
        val path = state.path
        if (!path.hasGeometry) return

        val points = path.points
        val controls = path.controls
        if (points.isEmpty()) return

        val w = scope.size.width
        val h = scope.size.height
        val maxThickness = h * WaveCalibration.Layout.MAX_THICKNESS_FRAC * 0.7f

        // Build the filled ribbon path
        ribbonPath.reset()
        buildRibbonPath(
            points = points,
            controls = controls,
            w = w,
            h = h,
            maxThickness = maxThickness,
            path = ribbonPath
        )

        // Create gradient brush from ColorState
        val brush = createRibbonBrush(state, color, w, h)

        // Draw the filled ribbon
        scope.drawPath(
            path = ribbonPath,
            brush = brush,
            alpha = state.ribbon.opacity.coerceIn(0f, 0.5f),
            style = androidx.compose.ui.graphics.drawscope.Fill
        )

        // Draw soft edge stroke for depth (optional — only when visible enough)
        if (state.ribbon.opacity > 0.15f && state.ribbon.thickness > 0.03f) {
            drawRibbonEdgeStroke(scope, points, controls, w, h, maxThickness, color)
        }
    }

    // ── Internal: Path Building ────────────────────────────────────────────

    /**
     * Build a filled ribbon path from center line + normals + thickness.
     *
     * Algorithm:
     *   1. For each point, compute top/bottom offsets along the normal
     *   2. Top edge: forward path using cubicTo with offset controls
     *   3. Bottom edge: backward path using cubicTo with offset controls
     *   4. Close the path
     *
     * The offset at each point is: normal × halfThickness.
     * Since our normals point upward (normalY ≈ -1 to -0), the top edge
     * is above the center and bottom edge is below.
     */
    /**
     * Render-layer smoothing window for the ribbon center line.
     *
     * The engine emits sparse center points; when consecutive points jump
     * (bass whips), raw cubic segments overshoot into sharp triangles —
     * the "corrupted waveform" look at the ribbon head. A 3-point moving
     * average removes point-to-point spikes while preserving wave motion.
     * Solver output is untouched; this only affects how points are drawn.
     */
    private const val SMOOTH_CLAMP = 1f

    internal fun buildRibbonPath(
        points: List<RibbonPoint>,
        controls: List<BezierControl>,
        w: Float,
        h: Float,
        maxThickness: Float,
        path: Path
    ) {
        val n = points.size
        if (n < 2) return

        // Smoothed center-line Y (normalized): average of prev/self/next,
        // clamped to sane range as hygiene against out-of-range samples.
        val smoothY = FloatArray(n) { i ->
            val y0 = points[(i - 1).coerceAtLeast(0)].y
            val y1 = points[i].y
            val y2 = points[(i + 1).coerceAtMost(n - 1)].y
            ((y0 + y1 + y2) / 3f).coerceIn(-SMOOTH_CLAMP, SMOOTH_CLAMP)
        }

        // Compute half-thickness for each point in pixels.
        // Thickness is smoothed like Y: per-point width jumps read as a
        // zigzag edge on the filled body (same corrupted look, other axis).
        val halfThick = FloatArray(n)
        for (i in 0 until n) {
            val t0 = points[(i - 1).coerceAtLeast(0)].thickness.coerceIn(0f, 1f)
            val t1 = points[i].thickness.coerceIn(0f, 1f)
            val t2 = points[(i + 1).coerceAtMost(n - 1)].thickness.coerceIn(0f, 1f)
            val t = (t0 + t1 + t2) / 3f
            halfThick[i] = (t * maxThickness * 0.5f)
                .coerceAtLeast(WaveCalibration.Layout.MIN_THICKNESS_PX * 0.5f)
        }

        // Helper: convert normalized coordinates to canvas coordinates
        fun toCanvasX(nx: Float): Float = WaveCalibration.Layout.H_PAD + nx * WaveCalibration.Layout.H_SPAN * w
        fun toCanvasY(ny: Float): Float = h * 0.5f + ny * WaveCalibration.Layout.Y_SCALE * h

        // ── Top edge: left → right ────────────────────────────────────
        val first = points[0]
        path.moveTo(
            toCanvasX(first.x),
            toCanvasY(smoothY[0]) - halfThick[0]
        )

        for (i in 0 until controls.size) {
            val ctrl = controls[i]
            val next = points[i + 1]

            // Offset control points by interpolated half-thickness
            val ctrlHalfT = (halfThick[i] + halfThick[i + 1]) * 0.5f

            path.cubicTo(
                toCanvasX(ctrl.cp1X),
                toCanvasY(ctrl.cp1Y.coerceIn(-SMOOTH_CLAMP, SMOOTH_CLAMP)) - ctrlHalfT,
                toCanvasX(ctrl.cp2X),
                toCanvasY(ctrl.cp2Y.coerceIn(-SMOOTH_CLAMP, SMOOTH_CLAMP)) - ctrlHalfT,
                toCanvasX(next.x),
                toCanvasY(smoothY[i + 1]) - halfThick[i + 1]
            )
        }

        // ── Bottom edge: right → left (reversed) ─────────────────────
        val last = points.last()
        path.lineTo(
            toCanvasX(last.x),
            toCanvasY(smoothY[n - 1]) + halfThick[n - 1]
        )

        for (i in controls.indices.reversed()) {
            val ctrl = controls[i]
            val prev = points[i]

            val ctrlHalfT = (halfThick[i] + halfThick[i + 1]) * 0.5f

            // Reversed cubic: the path goes backward so CP order flips
            path.cubicTo(
                toCanvasX(ctrl.cp2X),
                toCanvasY(ctrl.cp2Y.coerceIn(-SMOOTH_CLAMP, SMOOTH_CLAMP)) + ctrlHalfT,
                toCanvasX(ctrl.cp1X),
                toCanvasY(ctrl.cp1Y.coerceIn(-SMOOTH_CLAMP, SMOOTH_CLAMP)) + ctrlHalfT,
                toCanvasX(prev.x),
                toCanvasY(smoothY[i]) + halfThick[i]
            )
        }

        path.close()
    }

    // ── Internal: Brush Creation ──────────────────────────────────────────

    /**
     * Create the horizontal gradient brush for the ribbon fill.
     * Uses ColorState.waveStart → ColorState.waveEnd.
     * When idle, uses a subtle default color.
     *
     * The head/tail 8% fade to transparent: the ribbon's thinnest, whippiest
     * region sits at the screen margins, where an isolated bright blob reads
     * as a rendering glitch instead of atmosphere. Fading the fill at the
     * margins keeps the ribbon a backdrop wash; the timeline band (not the
     * margins) is where the song's waveform lives.
     */
    private fun createRibbonBrush(
        state: WaveState,
        color: ColorState,
        w: Float,
        h: Float
    ): Brush {
        val startColor = if (color.isIdle) IDLE_COLOR else color.waveStart
        val endColor = if (color.isIdle) IDLE_COLOR else color.waveEnd

        return Brush.horizontalGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                0.08f to startColor,
                0.92f to endColor,
                1f to Color.Transparent
            ),
            startX = 0f,
            endX = w
        )
    }

    // ── Internal: Edge Stroke ────────────────────────────────────────────

    /**
     * Draw a subtle edge stroke around the ribbon for depth separation.
     * Uses a thinner, more opaque stroke to create a "rim" effect.
     */
    private fun drawRibbonEdgeStroke(
        scope: DrawScope,
        points: List<RibbonPoint>,
        controls: List<BezierControl>,
        w: Float,
        h: Float,
        maxThickness: Float,
        color: ColorState
    ) {
        ribbonPath.reset()
        // Build the same path but slightly larger
        buildRibbonPath(
            points = points,
            controls = controls,
            w = w,
            h = h,
            maxThickness = maxThickness * 1.04f, // 4% larger for edge
            path = ribbonPath
        )

        val edgeColor = if (color.isIdle) {
            IDLE_COLOR.copy(alpha = 0.3f)
        } else {
            color.primary.copy(alpha = 0.15f)
        }

        scope.drawPath(
            path = ribbonPath,
            color = edgeColor,
            style = Stroke(width = 1f)
        )
    }
}
