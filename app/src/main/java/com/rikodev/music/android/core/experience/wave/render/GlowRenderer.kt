package com.rikodev.music.android.core.experience.wave.render

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — GLOW RENDERER
//
// Draws all glow, bloom, and aura effects BEHIND the ribbon.
//
// This is Layer 0 — the bottom-most layer in the rendering pipeline.
// Everything drawn here appears behind the ribbon, glass, and highlights.
// It creates the atmosphere, the "presence" before the ribbon even appears.
//
// ── EFFECTS ────────────────────────────────────────────────────────────────
//   1. Ambient Bloom:     A soft, wide glow behind the ribbon area.
//                          Uses ColorState.ambientGlow color.
//                          Scales with overallEnergy. Always visible (even idle).
//   2. Beat Pulse:        A brief radial flash on beat detection.
//                          Driven by WaveState.hasBeat.
//                          Expands outward from the ribbon center.
//   3. Outer Aura:        A large, diffuse halo around the ribbon bounding box.
//                          Follows the ribbon's bounding box with generous padding.
//                          Color from ColorState.primary with low alpha.
//   4. Inner Aura:        A tighter, more concentrated glow directly under
//                          the ribbon path. Follows the path closely.
//   5. Shadow Bloom:      A downward-projected soft shadow beneath the ribbon.
//                          Driven by HighlightState.shadowStrength.
//                          Color from ColorState.shadowColor.
//
// ── RULES ──────────────────────────────────────────────────────────────────
//   #1: NO AUDIO. NO FFT. NO ENERGY computation. Pure visual rendering.
//   #2: Reads WaveState.energy + WaveState.path.boundingBox + ColorState.
//   #3: All glows use radial gradients — never solid fills.
//   #4: Glow is BEHIND the ribbon — drawn first, overwritten by ribbon.
//   #5: Even idle state has a subtle ambient glow (the ribbon is never in pure black).
//
// PRODUCER: Phase 3.4E
// CONSUMER: WaveCanvas (3.4A) — called as Layer 0
//
// ⭐ GOLDEN RULE ⭐
//   GlowRenderer NEVER computes physics.
//   GlowRenderer ONLY reads pre-computed state and draws atmosphere.
// ─────────────────────────────────────────────────────────────────────────────

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import com.rikodev.music.android.core.experience.ColorState
import com.rikodev.music.android.core.experience.wave.BoundingBox
import com.rikodev.music.android.core.experience.wave.OrganicPath
import com.rikodev.music.android.core.experience.wave.WaveState
import kotlin.math.max
import kotlin.math.min
import com.rikodev.music.android.core.experience.calibration.WaveCalibration
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas

/**
 * Renders glow, bloom, aura, and shadow effects behind the ribbon.
 *
 * This is the atmosphere layer — it creates the visual context
 * that makes the ribbon feel like it exists in a space, not just
 * floating on a flat background.
 */
object GlowRenderer {

    // ── Rendering Constants ──────────────────────────────────────────────
    // All numeric constants moved to WaveCalibration

    /**
     * Idle ambient glow color — very subtle, cool blue.
     */
    private val IDLE_GLOW = Color(0x0A19B7FF)

    // ── Beat Pulse State (frame-to-frame continuity) ────────────────────

    /**
     * Current beat pulse intensity. Decays each frame.
     * Spikes to 1.0 on beat detection, then decays by WaveCalibration.Glow.BEAT_PULSE_DECAY.
     * @Volatile because it's written on the render thread and read on draw.
     */
    @Volatile
    private var beatPulseIntensity = 0f

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Draw all glow effects onto the canvas.
     *
     * Called by WaveCanvas as Layer 0 (bottom layer).
     * Called whenever state.isVisible == true.
     *
     * @param scope The DrawScope (canvas context)
     * @param state Current WaveState (contains energy, path, highlight, beat)
     * @param color Current ColorState (provides glow colors)
     */
    fun draw(scope: DrawScope, state: WaveState, color: ColorState) {
        val w = scope.size.width
        val h = scope.size.height
        val cx = w * 0.5f
        val cy = h * 0.5f

        // ── Effect 1: Ambient Bloom (always visible) ────────────────────
        drawAmbientBloom(scope, state, color, w, h, cx, cy)

        // ── Effect 2: Outer Aura (follows bounding box) ──────────────────
        if (state.path.hasGeometry) {
            drawOuterAura(scope, state, color, w, h)
        }

        // ── Effect 3: Inner Aura (follows ribbon path closely) ───────────
        if (state.path.hasGeometry && state.overallEnergy > 0.02f) {
            drawInnerAura(scope, state, color, w, h)
        }

        // ── Effect 4: Shadow Bloom (below ribbon) ──────────────────────
        if (state.highlight.needsShadow && state.path.hasGeometry) {
            drawShadowBloom(scope, state, color, w, h)
        }

        // ── Effect 5: Beat Pulse (on beat detection) ────────────────────
        drawBeatPulse(scope, state, color, w, h, cx, cy)
    }

    // ── Effect 1: Ambient Bloom ──────────────────────────────────────────

    /**
     * A soft, wide radial glow behind the ribbon.
     * Always visible — even in idle, the ribbon has a subtle halo.
     * Color from ColorState.ambientGlow, modulated by energy.
     */
    private fun drawAmbientBloom(
        scope: DrawScope,
        state: WaveState,
        color: ColorState,
        w: Float,
        h: Float,
        cx: Float,
        cy: Float
    ) {
        val diagonal = kotlin.math.sqrt(w * w + h * h)
        val baseRadius = diagonal * WaveCalibration.Glow.AMBIENT_RADIUS_FRAC

        // Energy modulates radius (not just alpha — wider presence)
        val energyExpand = state.overallEnergy.coerceIn(0f, 1f) * 0.15f
        val radius = baseRadius * (1f + energyExpand) * 0.8f

        val baseColor = if (color.isIdle) IDLE_GLOW else color.ambientGlow

        // Modulate alpha with energy: more energy = slightly more visible bloom
        val alpha = baseColor.alpha * (0.6f + state.overallEnergy * 0.4f)
            .coerceIn(0f, 1f)

        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    baseColor.copy(alpha = alpha),
                    baseColor.copy(alpha = alpha * 0.4f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = radius
            ),
            center = Offset(cx, cy),
            radius = radius
        )
    }

    // ── Effect 2: Outer Aura ──────────────────────────────────────────────

    /**
     * A larger, softer glow that follows the ribbon's bounding box.
     * Creates the impression that the ribbon emits light into its surroundings.
     */
    private fun drawOuterAura(
        scope: DrawScope,
        state: WaveState,
        color: ColorState,
        w: Float,
        h: Float
    ) {
        val bbox = state.path.boundingBox

        // Convert bounding box to canvas coordinates
        val canvasLeft = (WaveCalibration.Layout.H_PAD + bbox.minX * WaveCalibration.Layout.H_SPAN) * w
        val canvasTop = (h * 0.5f + bbox.minY * WaveCalibration.Layout.Y_SCALE * h)
        val canvasRight = (WaveCalibration.Layout.H_PAD + bbox.maxX * WaveCalibration.Layout.H_SPAN) * w
        val canvasBottom = (h * 0.5f + bbox.maxY * WaveCalibration.Layout.Y_SCALE * h)

        val canvasWidth = canvasRight - canvasLeft
        val canvasHeight = canvasBottom - canvasTop

        // Add generous padding for the aura
        val padX = canvasWidth * WaveCalibration.Glow.OUTER_AURA_PAD
        val padY = canvasHeight * WaveCalibration.Glow.OUTER_AURA_PAD + h * 0.05f

        val auraLeft = canvasLeft - padX
        val auraTop = canvasTop - padY
        val auraWidth = canvasWidth + padX * 2f
        val auraHeight = canvasHeight + padY * 2f

        val auraCenter = Offset(
            auraLeft + auraWidth * 0.5f,
            auraTop + auraHeight * 0.5f
        )

        val auraRadius = kotlin.math.sqrt(
            auraWidth * auraWidth + auraHeight * auraHeight
        ) * 0.5f

        val baseColor = if (color.isIdle) {
            Color(0x0D3A5F8A)
        } else {
            color.primary
        }

        // Aura alpha scales with energy
        val auraAlpha = (state.overallEnergy * 0.2f + 0.05f)
            .coerceIn(0f, 0.25f).coerceAtMost(0.15f)

        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    baseColor.copy(alpha = auraAlpha),
                    baseColor.copy(alpha = auraAlpha * 0.3f),
                    Color.Transparent
                ),
                center = auraCenter,
                radius = auraRadius
            ),
            center = auraCenter,
            radius = auraRadius
        )
    }

    // ── Effect 3: Inner Aura ──────────────────────────────────────────────

    /**
     * A tighter glow directly under the ribbon path.
     * Follows the path closely, creating a "glow beneath the body" effect.
     * Uses an elliptical gradient centered on the ribbon's center.
     */
    private fun drawInnerAura(
        scope: DrawScope,
        state: WaveState,
        color: ColorState,
        w: Float,
        h: Float
    ) {
        val bbox = state.path.boundingBox

        val canvasCx = (WaveCalibration.Layout.H_PAD + bbox.centerX * WaveCalibration.Layout.H_SPAN) * w
        val canvasCy = (h * 0.5f + bbox.centerY * WaveCalibration.Layout.Y_SCALE * h) + h * 0.02f

        val canvasWidth = bbox.width * WaveCalibration.Layout.H_SPAN * w
        val innerRadius = canvasWidth * 0.4f

        val baseColor = if (color.isIdle) {
            Color(0x0D3A5F8A)
        } else {
            color.waveStart
        }

        val innerAlpha = (state.overallEnergy * 0.15f + state.ribbon.opacity * 0.05f)
            .coerceIn(0f, 0.2f).coerceAtMost(0.15f)

        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    baseColor.copy(alpha = innerAlpha),
                    baseColor.copy(alpha = innerAlpha * 0.3f),
                    Color.Transparent
                ),
                center = Offset(canvasCx, canvasCy),
                radius = innerRadius
            ),
            center = Offset(canvasCx, canvasCy),
            radius = innerRadius
        )
    }

    // ── Effect 4: Shadow Bloom ────────────────────────────────────────────

    /**
     * A soft shadow projected downward from the ribbon.
     * The shadow uses ColorState.shadowColor (tinted, not pure black).
     * Driven by HighlightState.shadowStrength.
     */
    private fun drawShadowBloom(
        scope: DrawScope,
        state: WaveState,
        color: ColorState,
        w: Float,
        h: Float
    ) {
        val bbox = state.path.boundingBox
        val shadowStrength = state.highlight.shadowStrength

        // Shadow center: below the ribbon
        val shadowCy = (h * 0.5f + bbox.maxY * WaveCalibration.Layout.Y_SCALE * h) + h * WaveCalibration.Glow.SHADOW_OFFSET_FRAC
        val shadowCx = (WaveCalibration.Layout.H_PAD + bbox.centerX * WaveCalibration.Layout.H_SPAN) * w

        val shadowWidth = bbox.width * WaveCalibration.Layout.H_SPAN * w * 0.7f
        val shadowRadius = shadowWidth * 0.5f

        val shadowColor = color.shadowColor

        val alpha = (shadowStrength * 0.5f)
            .coerceIn(0f, 0.4f)

        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    shadowColor.copy(alpha = alpha),
                    shadowColor.copy(alpha = alpha * 0.3f),
                    Color.Transparent
                ),
                center = Offset(shadowCx, shadowCy),
                radius = shadowRadius
            ),
            center = Offset(shadowCx, shadowCy),
            radius = shadowRadius,
            blendMode = BlendMode.Multiply
        )
    }

    // ── Effect 5: Beat Pulse ───────────────────────────────────────────────

    /**
     * A brief radial flash on beat detection.
     * Spikes intensity on beat, decays quickly (WaveCalibration.Glow.BEAT_PULSE_DECAY per frame).
     * The pulse is centered on the canvas and expands outward.
     */
    private fun drawBeatPulse(
        scope: DrawScope,
        state: WaveState,
        color: ColorState,
        w: Float,
        h: Float,
        cx: Float,
        cy: Float
    ) {
        // Update beat pulse state
        if (state.hasBeat) {
            beatPulseIntensity = 1f
        } else {
            beatPulseIntensity *= WaveCalibration.Glow.BEAT_PULSE_DECAY
        }

        // Skip if intensity is negligible
        if (beatPulseIntensity < 0.02f) return

        val diagonal = kotlin.math.sqrt(w * w + h * h)
        val pulseRadius = diagonal * WaveCalibration.Glow.BEAT_PULSE_RADIUS_FRAC * (1f + (1f - beatPulseIntensity) * 0.3f)

        val pulseColor = if (color.isIdle) {
            Color(0x15FFFFFF)
        } else {
            color.primary
        }

        val alpha = (beatPulseIntensity * 0.15f)
            .coerceIn(0f, 0.2f)

        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    pulseColor.copy(alpha = alpha),
                    pulseColor.copy(alpha = alpha * 0.3f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = pulseRadius
            ),
            center = Offset(cx, cy),
            radius = pulseRadius,
            blendMode = BlendMode.Screen
        )
    }

}
