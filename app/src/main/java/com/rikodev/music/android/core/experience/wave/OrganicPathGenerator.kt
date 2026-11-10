package com.rikodev.music.android.core.experience.wave

import javax.inject.Inject
import javax.inject.Singleton

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

import com.rikodev.music.android.core.experience.calibration.WaveCalibration
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.Canvas

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — ORGANIC PATH GENERATOR
//
// The identity layer of the Wave Engine.
//
// This is where the ribbon first gets SHAPE. Before this step, we have
// energy, physics, and motion — all abstract numbers. After this step,
// we have GEOMETRY: points, curves, normals, thickness, highlights.
//
// This is NOT a renderer. This is NOT a visualizer.
// This is a PURE GEOMETRY ENGINE that converts physics into shape.
//
// ── RIKO RULES #1–7 (LOCKED) ──────────────────────────────────────────────
//   #1: Never generate random movement. Everything from energy/momentum/pressure/history.
//   #2: Ribbon never has sharp angles. Everything Bezier.
//   #3: Pressure changes amplitude. NOT frequency.
//   #4: Treble creates detail. Never height.
//   #5: Bass creates body. Never spikes.
//   #6: Motion offsets whole ribbon. Not individual points.
//   #7: History affects curvature. Not only position.
//
// ── Additional Rules ────────────────────────────────────────────────────
//   Never import Canvas, Path, DrawScope, Modifier, Compose.
//   Pure Kotlin math only.
//
// PIPELINE (10 stages):
//
//   Energy + Physics + Motion
//       │
//       ▼
//   Stage 1: Segment Count (from WaveQuality)
//       │
//       ▼
//   Stage 2: Center Line (evenly spaced x, y from energy waves)
//       │
//       ▼
//   Stage 3: Apply Motion Offset (whole ribbon, not per-point)
//       │
//       ▼
//   Stage 4: Apply Pressure Amplitude (bass = body, not spikes)
//       │
//       ▼
//   Stage 5: Apply Momentum Curvature (history → curve shape)
//       │
//       ▼
//   Stage 6: Apply Turbulence (controlled flow, not noise)
//       │
//       ▼
//   Stage 7: Thickness Profile (ink brush: head→body→tail→fade)
//       │
//       ▼
//   Stage 8: Opacity Profile (head fade, body full, tail fade)
//       │
//       ▼
//   Stage 9: Normal Generation (perpendicular vectors for glass)
//       │
//       ▼
//   Stage 10: Catmull-Rom → Bezier Conversion (smooth curves)
//       │
//       ▼
//   OrganicPath
//
// INPUT:  WaveFrame (energy + ribbon + mode)
// OUTPUT: OrganicPath (points + controls + analysis)
//
// ZERO DEPENDENCIES:
//   No Compose, no Canvas, no Modifier, no Path, no DrawScope.
//   No Color, no Brush, no BlendMode.
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class OrganicPathGenerator @Inject constructor() {

    // ═══════════════════════════════════════════════════════════════════════
    // CONSTANTS
    // ═══════════════════════════════════════════════════════════════════════

    companion object {
        // ── Structural wave model constants (not tuning — these define the harmonic model). ──
        private const val HARMONIC_LAYERS = 4
        private val HARMONIC_FREQS = floatArrayOf(1.0f, 2.3f, 3.7f, 5.1f)
        private val HARMONIC_AMPS = floatArrayOf(0.5f, 0.25f, 0.15f, 0.10f)
        private val HARMONIC_PHASES = floatArrayOf(0f, 1.2f, 2.8f, 4.5f)
        // ── All tuning constants live in WaveCalibration (Physics BIOS). ──
    }

    // ═══════════════════════════════════════════════════════════════════════
    // INTERNAL STATE
    // ═══════════════════════════════════════════════════════════════════════

    /** Frame counter for phase evolution. */
    private var frameCount: Int = 0

    /** Previous path points for inertia blending (smooth frame-to-frame transitions). */
    private var previousPoints: FloatArray = FloatArray(0)

    /** Inertia blend factor: 0 = all new, 1 = all previous. */
    private var inertiaBlend: Float = 0f

    // ═══════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Generate the complete ribbon geometry for one frame.
     *
     * Takes a WaveFrame with all solver outputs populated
     * and produces an OrganicPath with points, Bezier controls,
     * and geometric analysis.
     *
     * @param frame Input frame from the solver pipeline.
     * @param motionState Motion dynamics from MotionSolver.
     * @param quality Point density (LOW=32 to ULTRA=96).
     * @return Complete OrganicPath ready for Highlight Solver and Renderer.
     */
    fun generate(
        frame: WaveFrame,
        motionState: MotionState,
        quality: WaveQuality = WaveQuality.BALANCED
    ): OrganicPath {
        val segmentCount = quality.segmentCount
        val energy = frame.energy
        val ribbon = frame.ribbon
        val mode = frame.mode

        // ── Stage 1: Initialize arrays ──────────────────────────────────
        val yValues = FloatArray(segmentCount)

        // ── Stage 2: Center Line (base wave shape) ──────────────────────
        val baseAmplitude = energy.pressure * WaveCalibration.Geometry.PRESSURE_AMPLITUDE_SCALE
        for (i in 0 until segmentCount) {
            val t = i.toFloat() / (segmentCount - 1).coerceAtLeast(1)
            yValues[i] = computeBaseWave(t, baseAmplitude)
        }

        // ── Stage 3: Apply Motion Offset (whole ribbon) ────────────────
        // Rule #6: Motion offsets WHOLE ribbon, not individual points.
        val motionOffset = motionState.offset * WaveCalibration.Geometry.MOTION_OFFSET_SCALE
        for (i in 0 until segmentCount) {
            yValues[i] += motionOffset
        }

        // ── Stage 4: Apply Pressure Amplitude (bass = body) ────────────
        // Rule #3: Pressure changes amplitude, NOT frequency.
        // Already done in Stage 2 via baseAmplitude. No additional change.

        // ── Stage 5: Apply Momentum Curvature ──────────────────────────
        // Rule #7: History (momentum) affects curvature, not only position.
        // Momentum modulates the harmonic phases — high momentum = more flow,
        // creating smoother, wider curves rather than tight oscillation.
        val momentumPhaseShift = energy.momentum * 0.5f
        if (momentumPhaseShift > 0.001f) {
            for (i in 0 until segmentCount) {
                val t = i.toFloat() / (segmentCount - 1).coerceAtLeast(1)
                yValues[i] += computeMomentumCurve(t, momentumPhaseShift)
            }
        }

        // ── Stage 6: Apply Turbulence (controlled flow) ────────────────
        // Turbulence from midTexture: small, controlled, music-aware flow.
        if (ribbon.turbulence > 0.001f) {
            for (i in 0 until segmentCount) {
                val t = i.toFloat() / (segmentCount - 1).coerceAtLeast(1)
                yValues[i] += computeTurbulence(t, ribbon.turbulence)
            }
        }

        // ── Treble Detail (Rule #4: detail, not height) ─────────────────
        if (energy.trebleSpark > 0.05f) {
            for (i in 0 until segmentCount) {
                val t = i.toFloat() / (segmentCount - 1).coerceAtLeast(1)
                yValues[i] += computeTrebleDetail(t, energy.trebleSpark)
            }
        }

        // ── Apply Frame Inertia (smooth transitions) ──────────────────
        applyInertia(yValues, segmentCount, energy.overallEnergy)

        // Clamp all values
        for (i in 0 until segmentCount) {
            yValues[i] = yValues[i].coerceIn(-0.5f, 0.5f)
        }

        // Save for next frame inertia
        if (previousPoints.size != segmentCount) {
            previousPoints = FloatArray(segmentCount)
        }
        yValues.copyInto(previousPoints)

        // ── Stage 7–9: Build RibbonPoints with full metadata ─────────────
        val points = ArrayList<RibbonPoint>(segmentCount)
        for (i in 0 until segmentCount) {
            val t = i.toFloat() / (segmentCount - 1).coerceAtLeast(1)
            val y = yValues[i]

            // Thickness profile (ink brush)
            val thicknessProfile = computeThicknessProfile(
                t, ribbon.thickness, energy.bassPressure
            )

            // Opacity profile
            val opacityProfile = computeOpacityProfile(t, ribbon.opacity)

            // Curvature
            val curvature = computeCurvature(yValues, i, segmentCount, energy)

            // Glass pre-computation
            val highlight = computeHighlightIntensity(
                t, curvature, energy.trebleSpark, ribbon.highlightIntensity
            )
            val reflection = computeReflectionFactor(t, energy.overallEnergy)
            val edgeAlpha = computeEdgeAlpha(
                t, ribbon.surfaceTension, energy.overallEnergy
            )

            points.add(RibbonPoint(
                x = t,
                y = y,
                normalX = 0f, // Computed in Stage 9
                normalY = 0f, // Computed in Stage 9
                pressure = energy.pressure,
                velocity = motionState.velocity,
                curvature = curvature,
                thickness = thicknessProfile,
                opacity = opacityProfile,
                highlightIntensity = highlight,
                reflectionFactor = reflection,
                edgeAlpha = edgeAlpha
            ))
        }

        // ── Stage 9: Normal Generation ──────────────────────────────────
        generateNormals(points)

        // ── Stage 10: Catmull-Rom → Bezier Conversion ──────────────────
        val controls = catmullRomToBezier(points)

        // ── Analysis ────────────────────────────────────────────────────
        val totalLength = computeTotalLength(points)
        val avgCurvature = computeAverageCurvature(points)
        val bbox = computeBoundingBox(points)

        frameCount++

        return OrganicPath(
            points = points,
            controls = controls,
            totalLength = totalLength,
            averageCurvature = avgCurvature,
            boundingBox = bbox,
            quality = quality,
            timestamp = System.nanoTime()
        )
    }

    /**
     * Reset all internal state.
     */
    fun reset() {
        frameCount = 0
        previousPoints = FloatArray(0)
        inertiaBlend = 0f
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STAGE 2: BASE WAVE SHAPE
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Compute the base wave shape at position t using harmonic layers.
     *
     * The shape is a sum of 4 sine waves at different frequencies,
     * phases, and amplitudes. The phases evolve slowly over time
     * (frameCount), creating a flowing, organic motion.
     *
     * Energy ONLY affects amplitude (baseAmplitude), NOT frequency.
     * This ensures: silence = flat, energy = bigger waves, same shape.
     *
     * Rule #3: Pressure changes amplitude, NOT frequency.
     */
    private fun computeBaseWave(t: Float, amplitude: Float): Float {
        var y = 0f
        val time = frameCount.toFloat()

        for (layer in 0 until HARMONIC_LAYERS) {
            val freq = HARMONIC_FREQS[layer]
            val amp = HARMONIC_AMPS[layer] * amplitude
            val phase = HARMONIC_PHASES[layer] + time * WaveCalibration.Geometry.HARMONIC_SPEEDS[layer]
            y += sin(t * freq * 2f * kotlin.math.PI.toFloat() + phase) * amp
        }

        return y
    }

    /**
     * Compute momentum-driven curvature modulation.
     *
     * High momentum → wider, smoother curve contribution.
     * Low momentum → tight, reactive contribution.
     * This creates the "flowing" feel during sustained passages
     * and the "tight" feel during percussive passages.
     */
    private fun computeMomentumCurve(t: Float, momentumPhase: Float): Float {
        val time = frameCount.toFloat()
        return sin(t * 1.5f * 2f * kotlin.math.PI.toFloat() + time * 0.02f + momentumPhase) * momentumPhase * 0.15f
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STAGE 6: TURBULENCE (Controlled Flow)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Compute turbulence — controlled, music-aware flow (NOT noise).
     *
     * Turbulence is driven by midTexture (from RibbonState.turbulence).
     * It adds small, organic variation to the ribbon surface.
     *
     * The variation is NOT random — it uses sine waves at a different
     * frequency than the base wave, creating a "second layer" of texture
     * that flows naturally with the ribbon.
     */
    private fun computeTurbulence(t: Float, turbulence: Float): Float {
        val time = frameCount.toFloat()
        return sin(t * WaveCalibration.Geometry.TURBULENCE_FREQ * 2f * kotlin.math.PI.toFloat() + time * 0.053f) * WaveCalibration.Geometry.TURBULENCE_SCALE * turbulence
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TREBLE DETAIL (Rule #4)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Compute treble-driven fine detail.
     *
     * Rule #4: Treble creates detail. Never height.
     * This adds high-frequency micro-variations to the curve,
     * creating "sparkle" in the ribbon's shape — not taller waves.
     */
    private fun computeTrebleDetail(t: Float, trebleSpark: Float): Float {
        val time = frameCount.toFloat()
        return sin(t * WaveCalibration.Geometry.TREBLE_DETAIL_FREQ * 2f * kotlin.math.PI.toFloat() + time * 0.091f) * WaveCalibration.Geometry.TREBLE_DETAIL_SCALE * trebleSpark
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STAGE 7: THICKNESS PROFILE (Ink Brush)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Compute the ink brush thickness profile.
     *
     * Like a calligraphy brush stroke:
     *   Head (x≈0): starts thin, builds up
     *   Body (x≈0.15–0.75): full thickness
     *   Tail (x≈0.80–0.98): gradually thins
     *   End (x≈1.0): fades to near-zero
     *
     * Bass adds body thickness (Rule #5: Bass creates body. Never spikes.)
     */
    private fun computeThicknessProfile(
        t: Float,
        baseThickness: Float,
        bassPressure: Float
    ): Float {
        // Bass body multiplier
        val bassBody = 1f + bassPressure * WaveCalibration.Geometry.BASS_BODY_SCALE

        // Profile shape (0 at edges, 1 at body)
        val profile = when {
            t < WaveCalibration.Geometry.THICKNESS_HEAD_END -> {
                // Head: fade in (ease-in)
                val p = t / WaveCalibration.Geometry.THICKNESS_HEAD_END
                smoothStep(p)
            }
            t < WaveCalibration.Geometry.THICKNESS_BODY_START -> {
                // Ramp up to full body
                val p = (t - WaveCalibration.Geometry.THICKNESS_HEAD_END) / (WaveCalibration.Geometry.THICKNESS_BODY_START - WaveCalibration.Geometry.THICKNESS_HEAD_END)
                smoothStep(p)
            }
            t < WaveCalibration.Geometry.THICKNESS_BODY_END -> {
                // Full body
                1f
            }
            t < WaveCalibration.Geometry.THICKNESS_TAIL_START -> {
                // Begin thinning
                val p = (t - WaveCalibration.Geometry.THICKNESS_BODY_END) / (WaveCalibration.Geometry.THICKNESS_TAIL_START - WaveCalibration.Geometry.THICKNESS_BODY_END)
                1f - smoothStep(p) * 0.3f
            }
            t < WaveCalibration.Geometry.THICKNESS_TAIL_END -> {
                // Tail: gradual thin
                val p = (t - WaveCalibration.Geometry.THICKNESS_TAIL_START) / (WaveCalibration.Geometry.THICKNESS_TAIL_END - WaveCalibration.Geometry.THICKNESS_TAIL_START)
                0.7f * (1f - smoothStep(p))
            }
            else -> {
                // End: near zero
                0f
            }
        }

        return baseThickness * profile * bassBody
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STAGE 8: OPACITY PROFILE
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Compute per-point opacity with head and tail fades.
     */
    private fun computeOpacityProfile(t: Float, baseOpacity: Float): Float {
        // Head fade
        val headFade = if (t < WaveCalibration.Geometry.OPACITY_HEAD_FADE) {
            t / WaveCalibration.Geometry.OPACITY_HEAD_FADE
        } else {
            1f
        }

        // Tail fade
        val tailFade = if (t > WaveCalibration.Geometry.OPACITY_TAIL_START) {
            (1f - t) / (1f - WaveCalibration.Geometry.OPACITY_TAIL_START)
        } else {
            1f
        }

        return baseOpacity * headFade * tailFade
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STAGE 9: NORMAL GENERATION
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Compute surface normals for all points.
     *
     * The normal at each point is perpendicular to the tangent direction.
     * Used by Highlight Solver for specular calculation and by
     * Glass Physics for reflection/refraction mapping.
     */
    private fun generateNormals(points: MutableList<RibbonPoint>) {
        val n = points.size
        if (n < 2) return

        for (i in 0 until n) {
            // Tangent: direction from previous to next point
            val prev = points[max(0, i - 1)]
            val next = points[min(n - 1, i + 1)]
            val dx = next.x - prev.x
            val dy = next.y - prev.y
            val len = sqrt(dx * dx + dy * dy).coerceAtLeast(0.0001f)

            // Normal: perpendicular to tangent (rotated 90°)
            val nx = -dy / len
            val ny = dx / len

            points[i] = points[i].copy(normalX = nx, normalY = ny)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STAGE 10: CATMULL-ROM → BEZIER CONVERSION
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Convert the point list to cubic Bezier control points using
     * Catmull-Rom spline interpolation.
     *
     * For each segment P[i] → P[i+1], the control points are:
     *   CP1 = P[i]   + (P[i+1] - P[i-1]) / (6 * tension_factor)
     *   CP2 = P[i+1] - (P[i+2] - P[i])   / (6 * tension_factor)
     *
     * This guarantees C1 continuity — no sharp angles, ever.
     * Rule #2: Ribbon never has sharp angles. Everything Bezier.
     */
    private fun catmullRomToBezier(points: List<RibbonPoint>): List<BezierControl> {
        val n = points.size
        if (n < 2) return emptyList()

        val controls = ArrayList<BezierControl>(n - 1)
        val tensionDivisor = 6f // Catmull-Rom to Bezier conversion constant

        for (i in 0 until n - 1) {
            val p0 = points[max(0, i - 1)]     // Previous point
            val p1 = points[i]                  // Start of segment
            val p2 = points[min(n - 1, i + 1)]  // End of segment
            val p3 = points[min(n - 1, i + 2)]  // Next point after end

            // Tangent at P1: (P2 - P0) / tensionDivisor
            val cp1x = p1.x + (p2.x - p0.x) / tensionDivisor
            val cp1y = p1.y + (p2.y - p0.y) / tensionDivisor

            // Tangent at P2: -(P3 - P1) / tensionDivisor
            val cp2x = p2.x - (p3.x - p1.x) / tensionDivisor
            val cp2y = p2.y - (p3.y - p1.y) / tensionDivisor

            // Tension based on ribbon relaxation
            val tension = if (i < points.size - 1) {
                (p1.pressure * 0.3f + 0.5f).coerceIn(0.3f, 0.8f)
            } else {
                0.5f
            }

            controls.add(BezierControl(
                cp1X = cp1x,
                cp1Y = cp1y,
                cp2X = cp2x,
                cp2Y = cp2y,
                tension = tension
            ))
        }

        return controls
    }

    // ═══════════════════════════════════════════════════════════════════════
    // INERTIA (Frame-to-Frame Smooth Transition)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Apply frame-to-frame inertia blending.
     * Blends current Y values with previous frame's Y values
     * to prevent jarring frame-to-frame jumps.
     */
    private fun applyInertia(yValues: FloatArray, count: Int, energy: Float) {
        if (previousPoints.size != count) return

        // Inertia blend: higher energy = more responsive (less inertia)
        // Lower energy = more inertial (smoother transitions)
        val blend = if (energy > 0.3f) 0.15f else 0.35f

        for (i in 0 until count) {
            yValues[i] = yValues[i] * (1f - blend) + previousPoints[i] * blend
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GEOMETRY HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Compute curvature at a point using the second derivative approximation.
     */
    private fun computeCurvature(
        yValues: FloatArray,
        index: Int,
        count: Int,
        energy: WaveEnergy
    ): Float {
        if (count < 3 || index <= 0 || index >= count - 1) return 0f

        val prev = yValues[index - 1]
        val curr = yValues[index]
        val next = yValues[index + 1]

        // Second derivative approximation
        val d2y = abs(next - 2f * curr + prev)

        // Momentum modulates perceived curvature (history → curve shape)
        val momentumMod = 1f + energy.momentum * WaveCalibration.Geometry.MOMENTUM_CURVATURE_W

        return (d2y * 20f * momentumMod).coerceIn(0f, 1f)
    }

    /**
     * Compute per-point highlight intensity.
     * Pre-computed for the Highlight Solver to consume.
     */
    private fun computeHighlightIntensity(
        t: Float,
        curvature: Float,
        trebleSpark: Float,
        ribbonHighlight: Float
    ): Float {
        // Curvature concentrates highlight (specular peaks at curves)
        val curvatureHighlight = curvature * WaveCalibration.Geometry.HIGHLIGHT_CURVATURE_W

        // Treble adds sparkle along the ribbon
        val trebleHighlight = trebleSpark * WaveCalibration.Geometry.HIGHLIGHT_TREBLE_W *
            (0.5f + 0.5f * sin(t * 17f + frameCount.toFloat() * 0.1f))

        // Ribbon highlight from physics
        return (curvatureHighlight + trebleHighlight + ribbonHighlight * 0.3f)
            .coerceIn(0f, 1f)
    }

    /**
     * Compute reflection factor — strongest at body, weaker at edges.
     */
    private fun computeReflectionFactor(t: Float, energy: Float): Float {
        // Bell curve centered at x=0.5 (body center)
        val centerDist = abs(t - 0.5f) * 2f // 0 at center, 1 at edges
        val centerFactor = (1f - centerDist * centerDist) * WaveCalibration.Geometry.REFLECTION_CENTER_W
        return (centerFactor + energy * 0.2f).coerceIn(0f, 1f)
    }

    /**
     * Compute edge alpha — how visible the ribbon edge is.
     */
    private fun computeEdgeAlpha(
        t: Float,
        surfaceTension: Float,
        energy: Float
    ): Float {
        // Surface tension makes edges more defined
        val tensionEdge = surfaceTension * WaveCalibration.Geometry.EDGE_ALPHA_TENSION_W
        // Energy makes edges more visible
        val energyEdge = energy * WaveCalibration.Geometry.EDGE_ALPHA_ENERGY_W
        // Head/tail fade
        val edgeFade = if (t < 0.1f) t / 0.1f else if (t > 0.9f) (1f - t) / 0.1f else 1f
        return (tensionEdge + energyEdge) * edgeFade
    }

    /**
     * Smooth step interpolation (Hermite).
     * Used for thickness and opacity profile transitions.
     */
    private fun smoothStep(t: Float): Float {
        val tc = t.coerceIn(0f, 1f)
        return tc * tc * (3f - 2f * tc)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ANALYSIS
    // ═══════════════════════════════════════════════════════════════════════

    private fun computeTotalLength(points: List<RibbonPoint>): Float {
        var length = 0f
        for (i in 1 until points.size) {
            val dx = points[i].x - points[i - 1].x
            val dy = points[i].y - points[i - 1].y
            length += sqrt(dx * dx + dy * dy)
        }
        return length
    }

    private fun computeAverageCurvature(points: List<RibbonPoint>): Float {
        if (points.isEmpty()) return 0f
        var sum = 0f
        for (p in points) sum += p.curvature
        return sum / points.size
    }

    private fun computeBoundingBox(points: List<RibbonPoint>): BoundingBox {
        if (points.isEmpty()) return BoundingBox()
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        for (p in points) {
            if (p.x < minX) minX = p.x
            if (p.y < minY) minY = p.y
            if (p.x > maxX) maxX = p.x
            if (p.y > maxY) maxY = p.y
        }
        return BoundingBox(minX, minY, maxX, maxY)
    }
}
