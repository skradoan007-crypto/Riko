package com.rikodev.music.android.presentation.visualos.engine

import com.rikodev.music.android.presentation.visualos.contract.*
import com.rikodev.music.android.presentation.visualos.runtime.*
import kotlin.math.sin
import kotlin.math.cos
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// RIKO VISUAL OS — TRANSITION CHOREOGRAPHER
//
// The motion engine of RIKO Visual OS.
// Computes interpolated visual states during transitions using keyframes,
// spring physics, and motion vocabulary.
//
// From Spec Section 7.4: "All motion is timeline-based."
// From Spec Section 7.3: "Movement should be soft, physical, spring-like, believable."
//
// This engine is OBSERVATION-ONLY (Rule #76): it computes values,
// it does NOT modify state directly. The reducer consumes these values.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Transition choreographer — computes interpolated visual states.
 *
 * Given a transition spec and current progress (0.0-1.0), this engine
 * computes:
 *   - Entering element's visual state (opacity, scale, translation)
 *   - Background element's visual state (blur, scale, dim)
 *
 * The engine uses keyframe sequences with configurable spring physics
 * to create motion that feels:
 *   - Soft and physical (spring-based, not linear)
 *   - Purposeful (every motion has meaning per Spec Section 7.1)
 *   - Choreographed (multi-property coordination per Spec Section 5)
 */
object TransitionChoreographer {

    /**
     * Compute interpolated visual states for a transition at given progress.
     *
     * @param spec Transition specification (defines keyframes, spring, duration)
     * @param progress Current progress (0.0 = start, 1.0 = complete)
     * @param isReducedMotion Whether to use simplified motion
     * @return Pair of (entering visual state, background visual state)
     */
    fun compute(
        spec: TransitionSpec,
        progress: Float,
        isReducedMotion: Boolean = false
    ): Pair<VisualState, VisualState> {
        if (isReducedMotion) {
            return computeReducedMotion(spec, progress)
        }

        val keyframes = spec.keyframes.checkpoints
        if (keyframes.isEmpty()) {
            return VisualState() to VisualState()
        }

        // Find the two keyframes we're interpolating between
        val fromKf = findKeyframeBefore(keyframes, progress, spec.durationMs)
        val toKf = findKeyframeAfter(keyframes, progress, spec.durationMs)

        // Compute local progress between the two keyframes
        val localProgress = computeLocalProgress(progress, spec.durationMs, fromKf, toKf)

        // Apply easing (spring-like curve, not linear)
        val easedProgress = applyEasing(localProgress, spec.spring)

        // Interpolate all visual properties
        val enteringState = VisualState(
            opacity = lerp(fromKf.enteringOpacity, toKf.enteringOpacity, easedProgress),
            scale = lerp(fromKf.enteringScale, toKf.enteringScale, easedProgress),
            translationY = lerp(fromKf.enteringTranslationY, toKf.enteringTranslationY, easedProgress),
            blur = lerp(fromKf.backgroundBlur, toKf.backgroundBlur, easedProgress),
            isInteractive = progress > 0.8f,
            entryProgress = progress
        )

        val backgroundState = VisualState(
            opacity = 1f,
            blur = lerp(fromKf.backgroundBlur, toKf.backgroundBlur, easedProgress),
            scale = lerp(fromKf.backgroundScale, toKf.backgroundScale, easedProgress),
            dimAlpha = lerp(fromKf.backgroundDim, toKf.backgroundDim, easedProgress),
            isInteractive = progress < 0.1f,
            entryProgress = 1f
        )

        return enteringState to backgroundState
    }

    /**
     * Compute transition for reduced motion accessibility.
     * Simplified: no spatial motion, just simple fade.
     */
    private fun computeReducedMotion(
        spec: TransitionSpec,
        progress: Float
    ): Pair<VisualState, VisualState> {
        val enteringState = VisualState(
            opacity = progress,
            scale = 1f,
            translationY = 0f,
            isInteractive = progress > 0.5f,
            entryProgress = progress
        )

        val backgroundState = VisualState(
            opacity = 1f,
            scale = 1f,
            dimAlpha = if (spec.animateBackground) progress * 0.1f else 0f,
            isInteractive = progress < 0.5f,
            entryProgress = 1f
        )

        return enteringState to backgroundState
    }

    // ═════════════════════════════════════════════════════════════════════
    // KEYFRAME LOOKUP
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Find the keyframe checkpoint at or before the current progress.
     */
    private fun findKeyframeBefore(
        keyframes: List<KeyframeCheckpoint>,
        progress: Float,
        totalDuration: Int
    ): KeyframeCheckpoint {
        val currentTimeMs = (progress * totalDuration).toInt()
        for (i in keyframes.lastIndex downTo 0) {
            if (keyframes[i].timeMs <= currentTimeMs) {
                return keyframes[i]
            }
        }
        return keyframes.first()
    }

    /**
     * Find the keyframe checkpoint after the current progress.
     */
    private fun findKeyframeAfter(
        keyframes: List<KeyframeCheckpoint>,
        progress: Float,
        totalDuration: Int
    ): KeyframeCheckpoint {
        val currentTimeMs = (progress * totalDuration).toInt()
        for (kf in keyframes) {
            if (kf.timeMs > currentTimeMs) {
                return kf
            }
        }
        return keyframes.last()
    }

    /**
     * Compute local progress between two keyframes.
     */
    private fun computeLocalProgress(
        globalProgress: Float,
        totalDuration: Int,
        from: KeyframeCheckpoint,
        to: KeyframeCheckpoint
    ): Float {
        val fromTime = from.timeMs.toFloat()
        val toTime = to.timeMs.toFloat()
        val currentTime = globalProgress * totalDuration
        val segmentDuration = toTime - fromTime

        return if (segmentDuration <= 0f) {
            1f
        } else {
            ((currentTime - fromTime) / segmentDuration).coerceIn(0f, 1f)
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // EASING
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Apply spring-like easing curve to linear progress.
     *
     * This approximates spring physics behavior as a mathematical curve.
     * For actual spring animation, Compose's Animatable handles this.
     * This easing is used for keyframe interpolation where we need
     * a deterministic mapping from progress → eased value.
     */
    private fun applyEasing(
        progress: Float,
        spring: SpringConfig
    ): Float {
        // Cubic approximation of spring behavior
        // damping=1.0 → no overshoot (critically damped)
        // damping<1.0 → slight overshoot
        // damping>1.0 → sluggish

        return when {
            spring.damping >= 1.0f -> {
                // Critically/over-damped: use smooth step
                smoothStep(progress)
            }
            spring.damping >= 0.8f -> {
                // Slightly under-damped: very subtle overshoot
                val overshoot = (1f - spring.damping) * 0.08f
                elasticOut(progress, overshoot)
            }
            else -> {
                // Under-damped: noticeable overshoot
                val overshoot = (1f - spring.damping) * 0.15f
                elasticOut(progress, overshoot)
            }
        }
    }

    /**
     * Hermite smoothstep: smooth acceleration and deceleration.
     * f(t) = 3t² - 2t³
     */
    private fun smoothStep(t: Float): Float {
        val tc = t.coerceIn(0f, 1f)
        return tc * tc * (3f - 2f * tc)
    }

    /**
     * Elastic-out easing with controlled overshoot.
     */
    private fun elasticOut(t: Float, overshoot: Float): Float {
        if (t <= 0f) return 0f
        if (t >= 1f) return 1f
        val p = 0.4f
        val s = p / 4f
        return (1f + overshoot) * ((2f * (-10f * t / p).let { kotlin.math.exp(it) }) *
                sin((t - s) * (2f * kotlin.math.PI.toFloat()) / p) + 1f)
    }

    /**
     * Linear interpolation.
     */
    private fun lerp(from: Float, to: Float, t: Float): Float {
        return from + (to - from) * t.coerceIn(0f, 1f)
    }

    // ═════════════════════════════════════════════════════════════════════
    // BREATHING ANIMATION
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Compute breathing animation values.
     *
     * From Spec Section 6.3: "Background must live — softly moving, breath-like."
     *
     * Breathing uses a sinusoidal function with configurable:
     *   - Cycle duration (BackgroundTokens.BREATHING_CYCLE_MS)
     *   - Amplitude (BackgroundTokens.BREATHING_AMPLITUDE)
     *   - Phase offset (for multiple breathing elements)
     *
     * @param elapsedMs Time elapsed since animation start
     * @param cycleMs Full breathing cycle duration
     * @param amplitude Maximum displacement
     * @param phaseOffset Phase offset for organic feel
     * @return Breathing values (scale delta, opacity delta)
     */
    fun computeBreathing(
        elapsedMs: Long,
        cycleMs: Long = BackgroundTokens.BREATHING_CYCLE_MS,
        amplitude: Float = BackgroundTokens.BREATHING_AMPLITUDE,
        phaseOffset: Float = 0f
    ): BreathingValues {
        val phase = ((elapsedMs.toFloat() / cycleMs) * 2f * kotlin.math.PI.toFloat() + phaseOffset)
        val sinValue = sin(phase.toDouble()).toFloat()

        // Scale oscillates around 1.0
        val scaleDelta = amplitude * sinValue

        // Opacity oscillates very subtly (±0.02)
        val opacityDelta = amplitude * 0.5f * sinValue

        return BreathingValues(
            scaleDelta = scaleDelta,
            opacityDelta = opacityDelta,
            phase = phase % (2f * kotlin.math.PI.toFloat()),
            sinValue = sinValue
        )
    }

    /**
     * Compute drift values for slow ambient movement.
     *
     * From Spec Section 6.3: "softly moving"
     * From BackgroundTokens: "drift speed = 0.5 pixels/second"
     */
    fun computeDrift(
        elapsedMs: Long,
        speed: Float = BackgroundTokens.DRIFT_SPEED,
        maxDistance: Float = BackgroundTokens.MAX_DRIFT_DISTANCE.value,
        phaseOffsetX: Float = 0f,
        phaseOffsetY: Float = 1.5f
    ): DriftValues {
        val timeSeconds = elapsedMs / 1000f

        // X drift: sinusoidal with period ~20 seconds
        val xProgress = (timeSeconds * speed * 0.05f + phaseOffsetX)
        val xOffset = sin(xProgress.toDouble()).toFloat() * maxDistance * 0.5f

        // Y drift: sinusoidal with different period (~25 seconds) for organic feel
        val yProgress = (timeSeconds * speed * 0.04f + phaseOffsetY)
        val yOffset = sin(yProgress.toDouble()).toFloat() * maxDistance * 0.3f

        return DriftValues(
            offsetX = xOffset.coerceIn(-maxDistance, maxDistance),
            offsetY = yOffset.coerceIn(-maxDistance, maxDistance)
        )
    }

    // ═════════════════════════════════════════════════════════════════════
    // HANDOFF COMPUTATION
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Compute handoff animation between dock and NowPlaying.
     *
     * From Spec Section 5: "switch = context handoff"
     * The handoff morphs the dock into the NowPlaying screen.
     * This requires interpolating:
     *   - Position (bottom → center)
     *   - Size (small → full)
     *   - Content (album art thumbnail → full album art)
     *   - Controls (play/pause only → full transport)
     *   - Background (none → dramatic album art backdrop)
     *
     * @param progress Handoff progress (0.0 = dock, 1.0 = NowPlaying)
     * @param dockBounds Dock's current bounds
     * @return HandoffValues with interpolated properties
     */
    fun computeHandoff(
        progress: Float,
        dockBounds: HandoffBounds = HandoffBounds()
    ): HandoffValues {
        val eased = smoothStep(progress.coerceIn(0f, 1f))

        // Album art size: dock thumbnail → full screen hero
        val artSize = lerp(dockBounds.artSize, dockBounds.screenHeight * 0.4f, eased)

        // Y position: bottom dock → center of screen
        val yPosition = lerp(dockBounds.screenHeight - dockBounds.dockHeight, 0f, eased)

        // Corner radius: dock radius → 0 (full screen)
        val cornerRadius = lerp(dockBounds.dockCornerRadius, 0f, eased)

        // Opacity of transport controls: 0 → 1
        val controlsOpacity = if (progress > 0.5f) {
            smoothStep((progress - 0.5f) * 2f)
        } else 0f

        // Background blur: 0 → heavy
        val backgroundBlur = lerp(0f, BlurTokens.Maximum.value, eased)

        // Background dim: 0 → DIM_FOR_FOCUS
        val backgroundDim = lerp(0f, OpacityTokens.DIM_FOR_FOCUS, eased)

        return HandoffValues(
            artSize = artSize,
            yPosition = yPosition,
            cornerRadius = cornerRadius,
            controlsOpacity = controlsOpacity,
            backgroundBlur = backgroundBlur,
            backgroundDim = backgroundDim,
            progress = eased
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SUPPORTING DATA CLASSES
// ═══════════════════════════════════════════════════════════════════════════════

/** Breathing animation output values. */
data class BreathingValues(
    /** Scale delta from 1.0 (positive = larger, negative = smaller). */
    val scaleDelta: Float = 0f,
    /** Opacity delta (very subtle). */
    val opacityDelta: Float = 0f,
    /** Current phase in radians. */
    val phase: Float = 0f,
    /** Raw sin value (-1 to 1). */
    val sinValue: Float = 0f
)

/** Drift animation output values. */
data class DriftValues(
    /** X offset in dp. */
    val offsetX: Float = 0f,
    /** Y offset in dp. */
    val offsetY: Float = 0f
)

/** Dock bounds for handoff computation. */
data class HandoffBounds(
    val screenWidth: Float = 400f,
    val screenHeight: Float = 800f,
    val dockHeight: Float = 64f,
    val dockCornerRadius: Float = 20f,
    val artSize: Float = 44f
)

/** Handoff animation output values. */
data class HandoffValues(
    val artSize: Float = 0f,
    val yPosition: Float = 0f,
    val cornerRadius: Float = 0f,
    val controlsOpacity: Float = 0f,
    val backgroundBlur: Float = 0f,
    val backgroundDim: Float = 0f,
    val progress: Float = 0f
)
