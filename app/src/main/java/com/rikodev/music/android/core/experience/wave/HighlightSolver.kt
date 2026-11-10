package com.rikodev.music.android.core.experience.wave

import javax.inject.Inject
import javax.inject.Singleton

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import com.rikodev.music.android.core.experience.ColorState
import com.rikodev.music.android.core.experience.calibration.WaveCalibration
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — HIGHLIGHT SOLVER
//
// The light and glass engine for the Organic Energy Ribbon™.
//
// Before this step: ribbon has shape, physics, motion, geometry.
// After this step:  ribbon has LIGHT, REFLECTION, GLASS, DEPTH.
//
// This is NOT a renderer. This does NOT draw anything.
// This COMPUTES how light behaves on the ribbon's surface.
// The Canvas renderer (Phase 3.4) reads these values and draws.
//
// ── RIKO RULES #11–15 (LOCKED) ──────────────────────────────────────────────
//   #11: Light follows the ribbon. Never the camera.
//        → Light direction computed from ribbon normals + curvature.
//        → Camera position is NOT an input.
//   #12: Treble creates sparkle. Bass creates body. Highlight creates premium.
//        → specularIntensity driven by trebleSpark × curvature
//        → glassOpacity driven by bassPressure × surfaceTension
//        → rimLight driven by overall energy × normal angle
//   #13: Reflection must never be painted. It must be COMPUTED.
//        → reflectionAlpha = normal · lightDir × energy × ribbon.opacity
//        → Never a constant. Never a gradient hack.
//   #14: Edge glow is caused by curvature. Not opacity.
//        → edgeGlow = curvature × energy × surfaceTension
//        → Low curvature = no glow. High curvature = bright glow.
//   #15: More energy ≠ More white.
//        → High energy = slightly wider highlight
//        → High energy = slightly stronger fresnel
//        → High energy = slightly richer reflection
//        → Subtle. Premium. Never blown out.
//
// COLOR INTEGRATION:
//   The Highlight Solver READS ColorState for metadata only:
//     - dominantLuminance: affects light intensity scaling
//     - contrastLevel: affects specular contrast
//     - tintStrength: modulates glass tint amount
//     - isDarkArtwork: adjusts shadow/glow intensity
//   The solver NEVER decides colors. Only how light behaves.
//   Color values (waveStart, waveEnd, glassTint) are for the renderer.
//
// IMPORTS:
//   ZERO Compose imports. ZERO Canvas imports. ZERO Color imports.
//   Only kotlin.math.
//
// PRODUCER: Highlight Solver (Step 3.3F)
// INPUT:    OrganicPath, WaveFrame, RibbonState, ColorState
// OUTPUT:   HighlightState
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class HighlightSolver @Inject constructor() {

    // ═══════════════════════════════════════════════════════════════════════
    // CONSTANTS
    // ═══════════════════════════════════════════════════════════════════════

    companion object { // All constants now live in WaveCalibration (Physics BIOS).
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STATE — Temporal smoothing buffers
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Previous frame's per-point specular values for temporal smoothing.
     * Prevents highlight flicker between frames.
     */
    private var prevSpecular: FloatArray = FloatArray(0)

    /**
     * Previous frame's per-point fresnel values for temporal smoothing.
     */
    private var prevFresnel: FloatArray = FloatArray(0)

    /**
     * Previous frame's per-point glass values for temporal smoothing.
     */
    private var prevGlass: FloatArray = FloatArray(0)

    /**
     * Frame count for animation phases (breathing shimmer).
     */
    private var frameCount: Int = 0

    // ═══════════════════════════════════════════════════════════════════════
    // MAIN SOLVE — 8-stage pipeline
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Compute highlight state for the current frame.
     *
     * Pipeline:
     *   1. Extract ribbon normals
     *   2. Compute light direction (per-point, follows ribbon)
     *   3. Fresnel approximation (per-point)
     *   4. Specular strength (per-point)
     *   5. Reflection position & alpha (per-point)
     *   6. Edge glow (per-point)
     *   7. Shadow softness (per-point)
     *   8. Assemble HighlightState
     *
     * @param path      The OrganicPath from the Path Generator
     * @param frame     The WaveFrame carrying energy + mode + ribbon
     * @param ribbon    The RibbonState with physics values
     * @param color     The ColorState for metadata (luminance, contrast, tint)
     * @return HighlightState with per-point and frame-level light data
     */
    fun solve(
        path: OrganicPath,
        frame: WaveFrame,
        ribbon: RibbonState,
        color: ColorState
    ): HighlightState {
        frameCount++

        // ── Early exit: no geometry ────────────────────────────────────
        if (path.points.isEmpty()) {
            return HighlightState(
                specularIntensity = 0f,
                glassOpacity = 0f,
                fresnelPower = WaveCalibration.Light.BASE_FRESNEL_POWER,
                viewAngle = 1f,
                surfaceNormal = 1f,
                timestamp = frame.timestamp
            )
        }

        val points = path.points
        val pointCount = points.size

        // ── Extract energy & ribbon values for this frame ──────────────
        val energy = frame.energy
        val mode = frame.mode
        val overallEnergy = energy.overallEnergy
        val bassPressure = energy.bassPressure
        val trebleSpark = energy.trebleSpark
        val midTexture = energy.midTexture
        val momentum = energy.momentum

        // ── Artwork adaptation ───────────────────────────────────────────
        val isDarkArtwork = color.isDarkArtwork
        val luminanceBoost = if (isDarkArtwork) WaveCalibration.Light.DARK_ARTWORK_HIGHLIGHT_BOOST else 1f
        val contrastBoost = min(color.contrastLevel * WaveCalibration.Light.HIGH_CONTRAST_SHARPEN, 0.15f)

        // ── Mode-based energy scaling ───────────────────────────────────
        // Different modes get different light treatment.
        val modeLightScale = when (mode) {
            RibbonMode.IDLE          -> 0.15f
            RibbonMode.BREATHING     -> 0.25f
            RibbonMode.THIN          -> 0.4f
            RibbonMode.ACTIVE        -> 0.7f
            RibbonMode.PRESSURE_BUILD -> 0.85f
            RibbonMode.EXPANDING     -> 1.0f
            RibbonMode.CALM_SETTLING -> 0.5f * min(1f, momentum * 2f)
        }

        // ═══════════════════════════════════════════════════════════════
        // STAGE 1: Extract ribbon normals
        // ═══════════════════════════════════════════════════════════════
        // Normals are already in RibbonPoint.normalX, normalY.
        // We validate and compute normal magnitude for fresnel.
        val normalMagnitudes = FloatArray(pointCount)
        for (i in 0 until pointCount) {
            val nx = points[i].normalX
            val ny = points[i].normalY
            normalMagnitudes[i] = sqrt(nx * nx + ny * ny)
        }

        // ═══════════════════════════════════════════════════════════════
        // STAGE 2: Compute light direction (per-point)
        // ═══════════════════════════════════════════════════════════════
        // Rule #11: Light follows the ribbon. Never the camera.
        // Base light is upper-left (-45 degrees).
        // Per-point: deflected by local curvature + ribbon's overall bend.
        val lightDirsX = FloatArray(pointCount)
        val lightDirsY = FloatArray(pointCount)

        // Compute overall ribbon bend direction from average curvature
        var avgCurvatureSign = 0f
        for (i in 0 until pointCount) {
            // Curvature sign tells us which direction the ribbon bends
            avgCurvatureSign += points[i].curvature
        }
        avgCurvatureSign /= pointCount

        // Bend direction shifts the base light angle
        val bendLightAngle = WaveCalibration.Light.BASE_LIGHT_ANGLE + avgCurvatureSign * WaveCalibration.Light.LIGHT_BEND_INFLUENCE

        for (i in 0 until pointCount) {
            // Local curvature deflects light
            val localCurvature = points[i].curvature
            val deflection = localCurvature * WaveCalibration.Light.LIGHT_FOLLOW_CURVATURE

            // Final light direction at this point
            val angle = bendLightAngle + deflection
            lightDirsX[i] = cos(angle)
            lightDirsY[i] = sin(angle)
        }

        // ═══════════════════════════════════════════════════════════════
        // STAGE 3: Fresnel approximation (per-point)
        // ═══════════════════════════════════════════════════════════════
        // Fresnel: surfaces at grazing angles reflect more.
        // Approximation: pow(1 - |dot(normal, view)|, power)
        // Rule #15: Higher energy = slightly sharper fresnel power.

        // Fresnel power: base 3.0, up to 5.0 with energy
        val fresnelPower = WaveCalibration.Light.BASE_FRESNEL_POWER + (WaveCalibration.Light.MAX_FRESNEL_POWER - WaveCalibration.Light.BASE_FRESNEL_POWER) * overallEnergy

        val fresnelValues = FloatArray(pointCount)
        for (i in 0 until pointCount) {
            // Normalize the normal
            val mag = normalMagnitudes[i]
            val safeMag = if (mag < 0.001f) 1f else mag
            val nx = points[i].normalX / safeMag
            val ny = points[i].normalY / safeMag

            // Dot product with view direction
            val dotNormalView = abs(nx * WaveCalibration.Light.VIEW_DIR_X + ny * WaveCalibration.Light.VIEW_DIR_Y)
            // Clamp to valid range for pow()
            val clampedDot = min(max(dotNormalView, 0f), 1f)

            // Fresnel approximation
            val rawFresnel = (1f - clampedDot).pow(fresnelPower)

            // Mode scaling + energy modulation
            // Rule #15: subtle increase with energy
            val energyFresnelBoost = rawFresnel + overallEnergy * 0.08f
            fresnelValues[i] = min(energyFresnelBoost, 1f) * modeLightScale
        }

        // ═══════════════════════════════════════════════════════════════
        // STAGE 4: Specular strength (per-point)
        // ═══════════════════════════════════════════════════════════════
        // Rule #12: Treble creates sparkle.
        // Specular = curvature_concentration × treble × light_alignment
        val specularValues = FloatArray(pointCount)

        for (i in 0 until pointCount) {
            val point = points[i]
            val curvature = point.curvature

            // Specular concentrates at high-curvature points (peaks).
            // Low curvature = no specular. High curvature = sparkle.
            val curvatureFactor = if (curvature > 0.1f) {
                (curvature - 0.1f) / 0.9f // normalize 0.1..1.0 to 0..1
            } else {
                0f
            }

            // Light alignment: how much the surface normal faces the light
            val mag = normalMagnitudes[i]
            val safeMag = if (mag < 0.001f) 1f else mag
            val nx = point.normalX / safeMag
            val ny = point.normalY / safeMag
            val dotNormalLight = nx * lightDirsX[i] + ny * lightDirsY[i]
            // Specular: power of the half-vector alignment
            val specularDot = min(max(dotNormalLight, 0f), 1f)
            val specularConcentration = specularDot.pow(WaveCalibration.Light.SPECULAR_POWER + contrastBoost)

            // Rule #12: Treble creates sparkle
            val sparkleFactor = WaveCalibration.Light.TREBLE_SPARKLE_FACTOR * trebleSpark
            val bassBodyFactor = bassPressure * 0.3f

            // Combine: curvature x light x treble
            val rawSpecular = curvatureFactor * specularConcentration * (sparkleFactor + bassBodyFactor) * WaveCalibration.Light.SPECULAR_MAX

            // Artwork adaptation: dark backgrounds boost highlights
            val adaptedSpecular = rawSpecular * luminanceBoost

            // Mode scaling
            specularValues[i] = min(adaptedSpecular * modeLightScale, WaveCalibration.Light.SPECULAR_MAX)
        }

        // ═══════════════════════════════════════════════════════════════
        // STAGE 5: Reflection position & alpha (per-point)
        // ═══════════════════════════════════════════════════════════════
        // Rule #13: Reflection must never be painted. It must be COMPUTED.
        // Reflection = normal·light × point's base reflectionFactor × energy
        val reflectionAlphas = FloatArray(pointCount)
        val reflectionOffsets = FloatArray(pointCount)

        for (i in 0 until pointCount) {
            val point = points[i]

            // Light reflection on surface
            val mag = normalMagnitudes[i]
            val safeMag = if (mag < 0.001f) 1f else mag
            val nx = point.normalX / safeMag
            val ny = point.normalY / safeMag
            val dotNL = nx * lightDirsX[i] + ny * lightDirsY[i]

            // Reflection alpha: base from point's pre-computed factor
            val pointReflection = point.reflectionFactor
            // Modulated by light alignment × energy
            val lightReflection = min(max(dotNL, 0f), 1f)
            val energyReflection = overallEnergy * 0.5f + bassPressure * 0.2f

            reflectionAlphas[i] = min(
                (WaveCalibration.Light.REFLECTION_BASE
                        + pointReflection * WaveCalibration.Light.POINT_REFLECTION_WEIGHT
                        + lightReflection * energyReflection),
                WaveCalibration.Light.REFLECTION_MAX
            ) * modeLightScale

            // Reflection offset: where the reflection sits relative to center.
            // Shifts toward light source direction.
            val baseOffset = 0.5f
            val lightShift = (lightDirsX[i] + 1f) * 0.5f * WaveCalibration.Light.REFLECTION_LIGHT_SHIFT // map -1..1 to shift
            val curvatureShift = point.curvature * WaveCalibration.Light.REFLECTION_CURVATURE_SHIFT
            reflectionOffsets[i] = min(max(
                baseOffset + lightShift - curvatureShift,
                0f
            ), 1f)
        }

        // ═══════════════════════════════════════════════════════════════
        // STAGE 6: Edge glow (per-point)
        // ═══════════════════════════════════════════════════════════════
        // Rule #14: Edge glow is caused by curvature. Not opacity.
        // Glow = curvature × energy × surfaceTension
        val edgeGlowValues = FloatArray(pointCount)

        for (i in 0 until pointCount) {
            val point = points[i]
            val curvature = point.curvature

            // Only glow where curvature exceeds threshold
            val glowable = if (curvature > WaveCalibration.Light.EDGE_GLOW_CURVATURE_THRESHOLD) {
                (curvature - WaveCalibration.Light.EDGE_GLOW_CURVATURE_THRESHOLD) / (1f - WaveCalibration.Light.EDGE_GLOW_CURVATURE_THRESHOLD)
            } else {
                0f
            }

            // Surface tension amplifies glow (tight ribbon = more glow)
            val tensionGlow = ribbon.surfaceTension * WaveCalibration.Light.SURFACE_TENSION_GLOW_FACTOR

            // Energy drives glow intensity
            val energyGlow = overallEnergy * 0.5f + momentum * 0.3f

            // Mid texture adds subtle inner glow
            val textureGlow = midTexture * 0.15f

            edgeGlowValues[i] = min(
                glowable * (tensionGlow + energyGlow + textureGlow),
                WaveCalibration.Light.EDGE_GLOW_MAX
            ) * modeLightScale
        }

        // ═══════════════════════════════════════════════════════════════
        // STAGE 7: Shadow softness (per-point)
        // ═══════════════════════════════════════════════════════════════
        val shadowValues = FloatArray(pointCount)

        for (i in 0 until pointCount) {
            val point = points[i]

            // Shadow only appears above energy threshold
            if (overallEnergy < WaveCalibration.Light.SHADOW_ENERGY_THRESHOLD) {
                shadowValues[i] = 0f
                continue
            }

            // Thickness drives shadow (thicker ribbon = more shadow)
            val thicknessShadow = point.thickness * WaveCalibration.Light.SHADOW_THICKNESS_WEIGHT

            // Energy above threshold drives shadow visibility
            val energyAboveThreshold = (overallEnergy - WaveCalibration.Light.SHADOW_ENERGY_THRESHOLD) / (1f - WaveCalibration.Light.SHADOW_ENERGY_THRESHOLD)

            // Compression (ribbon squeezed = stronger shadow underneath)
            val compressionShadow = (1f - ribbon.compression) * 0.15f

            shadowValues[i] = min(
                energyAboveThreshold * (thicknessShadow + compressionShadow + 0.1f),
                WaveCalibration.Light.SHADOW_MAX
            ) * modeLightScale
        }

        // ═══════════════════════════════════════════════════════════════
        // STAGE 8: Rim light (per-point)
        // ═══════════════════════════════════════════════════════════════
        // Rim light: ribbon edge illuminated from behind.
        // Driven by: normal angle to view + energy + thickness
        val rimLightValues = FloatArray(pointCount)

        for (i in 0 until pointCount) {
            val point = points[i]

            // How edge-on the surface is to the view
            // When normal is perpendicular to view → maximum rim light
            val mag = normalMagnitudes[i]
            val safeMag = if (mag < 0.001f) 1f else mag
            val nx = point.normalX / safeMag
            val ny = point.normalY / safeMag
            val dotNV = nx * WaveCalibration.Light.VIEW_DIR_X + ny * WaveCalibration.Light.VIEW_DIR_Y
            // Rim light = 1 - dot(normal, view)
            val edgeOn = 1f - min(max(dotNV, 0f), 1f)

            // Energy drives rim light intensity
            val energyRim = overallEnergy * 0.5f + momentum * 0.2f

            // Thickness: thicker ribbon = more visible rim
            val thicknessRim = min(point.thickness, 1f) * 0.2f

            rimLightValues[i] = min(
                (edgeOn * WaveCalibration.Light.RIM_LIGHT_NORMAL_FACTOR + energyRim + thicknessRim),
                WaveCalibration.Light.RIM_LIGHT_MAX
            ) * modeLightScale
        }

        // ═══════════════════════════════════════════════════════════════
        // GLASS & HIGHLIGHT WIDTH (computed per-point)
        // ═══════════════════════════════════════════════════════════════
        val glassValues = FloatArray(pointCount)
        val highlightWidths = FloatArray(pointCount)

        for (i in 0 until pointCount) {
            val point = points[i]

            // Glass opacity: surfaceTension × energy × opacity
            val tensionGlass = ribbon.surfaceTension * WaveCalibration.Light.GLASS_TENSION_WEIGHT
            val energyGlass = overallEnergy * 0.3f
            val opacityGlass = point.opacity * WaveCalibration.Light.GLASS_OPACITY_WEIGHT
            glassValues[i] = min(max(
                WaveCalibration.Light.GLASS_MIN + tensionGlass + energyGlass + opacityGlass,
                WaveCalibration.Light.GLASS_MIN
            ), WaveCalibration.Light.GLASS_MAX) * modeLightScale

            // Highlight width: Rule #15 — wider with energy, not brighter
            val baseWidth = WaveCalibration.Light.HIGHLIGHT_WIDTH_MIN
            val energyWidth = (WaveCalibration.Light.HIGHLIGHT_WIDTH_MAX - baseWidth) * overallEnergy
            // Specular presence also slightly widens the band
            val specularWidth = specularValues[i] * 0.15f
            highlightWidths[i] = min(baseWidth + energyWidth + specularWidth, WaveCalibration.Light.HIGHLIGHT_WIDTH_MAX)
        }

        // ═══════════════════════════════════════════════════════════════
        // TEMPORAL SMOOTHING — prevent flicker
        // ═══════════════════════════════════════════════════════════════
        // Resize buffers if point count changed (quality change)
        if (prevSpecular.size != pointCount) {
            prevSpecular = FloatArray(pointCount)
            prevFresnel = FloatArray(pointCount)
            prevGlass = FloatArray(pointCount)
        }

        // Breathing shimmer: subtle oscillation in specular during IDLE/BREATHING
        val shimmerPhase = sin(frameCount * 0.015f) * 0.03f
        val isBreathingMode = mode == RibbonMode.IDLE || mode == RibbonMode.BREATHING

        val highlightPoints = ArrayList<HighlightPoint>(pointCount)
        var sumSpecular = 0f
        var sumEdgeGlow = 0f
        var sumRimLight = 0f
        var sumReflectionAlpha = 0f
        var sumGlass = 0f
        var sumFresnel = 0f
        var sumShadow = 0f
        var sumHighlightWidth = 0f
        var sumReflectionOffset = 0f
        var sumViewAngle = 0f
        var sumNormalMag = 0f

        for (i in 0 until pointCount) {
            // Smooth specular with previous frame
            val smoothedSpecular = prevSpecular[i] * (1f - WaveCalibration.Light.HIGHLIGHT_SMOOTHING)
                    + specularValues[i] * WaveCalibration.Light.HIGHLIGHT_SMOOTHING
            // Add breathing shimmer in idle/breathing modes
            val finalSpecular = if (isBreathingMode) {
                min(max(smoothedSpecular + shimmerPhase, 0f), WaveCalibration.Light.SPECULAR_MAX)
            } else {
                smoothedSpecular
            }

            // Smooth fresnel
            val smoothedFresnel = prevFresnel[i] * (1f - WaveCalibration.Light.HIGHLIGHT_SMOOTHING)
                    + fresnelValues[i] * WaveCalibration.Light.HIGHLIGHT_SMOOTHING

            // Smooth glass
            val smoothedGlass = prevGlass[i] * (1f - WaveCalibration.Light.HIGHLIGHT_SMOOTHING)
                    + glassValues[i] * WaveCalibration.Light.HIGHLIGHT_SMOOTHING

            // View angle for this point
            val mag = normalMagnitudes[i]
            val safeMag = if (mag < 0.001f) 1f else mag
            val nx = points[i].normalX / safeMag
            val ny = points[i].normalY / safeMag
            val viewAngle = min(max(nx * WaveCalibration.Light.VIEW_DIR_X + ny * WaveCalibration.Light.VIEW_DIR_Y, 0f), 1f)

            // Build per-point highlight data
            highlightPoints.add(
                HighlightPoint(
                    specularIntensity = finalSpecular,
                    edgeGlow = edgeGlowValues[i],
                    rimLight = rimLightValues[i],
                    reflectionAlpha = reflectionAlphas[i],
                    glassOpacity = smoothedGlass,
                    fresnel = smoothedFresnel,
                    lightDirectionX = lightDirsX[i],
                    lightDirectionY = lightDirsY[i],
                    shadowStrength = shadowValues[i],
                    highlightWidth = highlightWidths[i],
                    reflectionOffset = reflectionOffsets[i]
                )
            )

            // Accumulate for frame-level averages
            sumSpecular += finalSpecular
            sumEdgeGlow += edgeGlowValues[i]
            sumRimLight += rimLightValues[i]
            sumReflectionAlpha += reflectionAlphas[i]
            sumGlass += smoothedGlass
            sumFresnel += smoothedFresnel
            sumShadow += shadowValues[i]
            sumHighlightWidth += highlightWidths[i]
            sumReflectionOffset += reflectionOffsets[i]
            sumViewAngle += viewAngle
            sumNormalMag += mag

            // Store for next frame's smoothing
            prevSpecular[i] = finalSpecular
            prevFresnel[i] = smoothedFresnel
            prevGlass[i] = smoothedGlass
        }

        // ═══════════════════════════════════════════════════════════════
        // ASSEMBLE: Build frame-level HighlightState
        // ═══════════════════════════════════════════════════════════════
        val count = pointCount.toFloat()
        return HighlightState(
            points = highlightPoints,

            // Frame-level averages
            specularIntensity = sumSpecular / count,
            edgeGlow = sumEdgeGlow / count,
            rimLight = sumRimLight / count,
            reflectionAlpha = sumReflectionAlpha / count,
            glassOpacity = sumGlass / count,
            fresnel = sumFresnel / count,
            lightDirection = bendLightAngle,
            shadowStrength = sumShadow / count,
            highlightWidth = sumHighlightWidth / count,
            reflectionOffset = sumReflectionOffset / count,

            // Fresnel details
            viewAngle = sumViewAngle / count,
            surfaceNormal = sumNormalMag / count,
            fresnelPower = fresnelPower,

            timestamp = frame.timestamp
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // RESET — Clear all temporal state
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Reset all temporal smoothing buffers and frame counter.
     * Call when: track changes, player stops, engine reset.
     */
    fun reset() {
        prevSpecular = FloatArray(0)
        prevFresnel = FloatArray(0)
        prevGlass = FloatArray(0)
        frameCount = 0
    }
}
