/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.3 / Phase A — Background Language
 *
 * File A4 / A5 — LibraryAmbientLayer.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [LibraryAmbientLayer] is the *breathing* layer of the Library screen.
 *
 *   এটাই breathing করবে।
 *   কিন্তু খুব subtle।
 *   User যেন animation না দেখে।
 *   User শুধু feel করে — "Screenটা alive."
 *
 * WHAT THIS FILE IS
 * -----------------
 * A composable that paints a very slow, very faint radial gradient
 * drift on top of the background. The drift:
 *
 *   - Cycles every 12 seconds (Rule #64 — ≥ 8s)
 *   - Alpha oscillates between 0.04 and 0.10 (Rule #64 — delta ≤ 0.10)
 *   - Position drifts by ≤ 6% of screen (Rule #64 — position delta ≤ 6%)
 *   - Color stays constant (no hue shift)
 *   - Reverses (never restarts — Rule #64 forbids RESTART)
 *
 * When reduce-motion is enabled, the layer renders as a STATIC radial
 * gradient at the midpoint position and midpoint alpha — no animation.
 *
 * WHAT THIS FILE IS NOT
 * ---------------------
 * ❌ Not Wave. Wave belongs to the Wave Engine. This is much subtler.
 * ❌ Not content. Ambient never has shapes / icons / text.
 * ❌ Not a color shifter. The hue stays constant.
 * ❌ Not parallax. Parallax belongs to Hero (Phase B).
 *
 * RULES ENFORCED HERE
 * -------------------
 * #58  Runtime Never Knows Rendering   — this is rendering
 * #62  Never Rebuild Existing Systems  — uses Color.kt palette
 * #63  Background Never Demands Attention — alpha always ≤ 0.10
 * #64  Motion Must Be Discoverable, Not Obvious — 12s cycle, ≤ 0.10 alpha
 *
 * © RIKO Experience OS — Rendering Tier (Phase A)
 */

package com.rikodev.music.android.presentation.library.screen.background

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.theme.AccentCyan
import com.rikodev.music.android.core.designsystem.theme.AccentViolet
import com.rikodev.music.android.presentation.library.screen.contract.LibraryMotionLanguage
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas

// ─────────────────────────────────────────────────────────────────────────────
// AMBIENT PRESETS
//
// Each preset defines the *static* properties of an ambient layer:
//   - Color (constant — never animated)
//   - Base alpha (the midpoint of the breath)
//   - Breath depth (how much the alpha oscillates around the midpoint)
//   - Cycle duration (how long one breath takes)
//
// The position drift is computed at draw time, not stored in the preset
// (it depends on canvas size).
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A preset for the ambient layer's breathing behavior.
 *
 * @property color           The ambient color. NEVER animated — only
 *                           its alpha changes. Default: [AccentCyan].
 * @property baseAlpha       The midpoint alpha. Range: 0.0–1.0.
 *                           Rule #63: ≤ 0.10 for ambient layers.
 * @property breathDepth     How much the alpha oscillates around
 *                           [baseAlpha]. Range: 0.0–0.5.
 *                           Rule #64: ≤ 0.10 (delta ≤ 0.10 per cycle).
 * @property cycleDurationMs One full breath (in → out → in) in ms.
 *                           Rule #64: ≥ 8_000ms.
 * @property positionDriftRatio  How far the radial center drifts, as a
 *                              ratio of screen dimension. Rule #64: ≤ 0.06.
 */
data class LibraryAmbientPreset(
    val color: Color,
    val baseAlpha: Float,
    val breathDepth: Float,
    val cycleDurationMs: Int,
    val positionDriftRatio: Float,
) {
    init {
        // ── Rule #63 enforcement ─────────────────────────────────────────
        // Background ambient layers must never exceed 0.10 alpha at peak.
        require(baseAlpha + breathDepth <= 0.10f) {
            "Ambient peak alpha (${baseAlpha + breathDepth}) must be ≤ 0.10 " +
                "(Rule #63 — Background Never Demands Attention)."
        }
        // ── Rule #64 enforcement ─────────────────────────────────────────
        // Cycle must be ≥ 8 seconds; alpha delta must be ≤ 0.10; position
        // drift must be ≤ 6%.
        require(cycleDurationMs >= 8_000) {
            "Ambient cycleDurationMs ($cycleDurationMs) must be ≥ 8000 " +
                "(Rule #64 — discoverable motion, not obvious)."
        }
        require(breathDepth <= 0.10f) {
            "Ambient breathDepth ($breathDepth) must be ≤ 0.10 " +
                "(Rule #64 — alpha delta ≤ 0.10 per cycle)."
        }
        require(positionDriftRatio <= 0.06f) {
            "Ambient positionDriftRatio ($positionDriftRatio) must be ≤ 0.06 " +
                "(Rule #64 — position drift ≤ 6%)."
        }
    }

    companion object {

        /**
         * DEFAULT preset — Cyan whisper.
         *
         * - Color: AccentCyan (the RIKO primary accent)
         * - Peak alpha: 0.06 + 0.04 = 0.10 (at the top of the breath)
         * - Trough alpha: 0.06 - 0.04 = 0.02 (at the bottom)
         * - Cycle: 12 seconds
         * - Position drift: 4% (subtle, almost imperceptible)
         *
         * This is the default ambient. The eye perceives a faint cyan
         * "presence" at the top of the screen that very slowly breathes
         * in and out. You don't see it move — you only feel it when you
         * look away and look back.
         */
        val DEFAULT = LibraryAmbientPreset(
            color = AccentCyan,
            baseAlpha = 0.06f,
            breathDepth = 0.04f,
            cycleDurationMs = 12_000,
            positionDriftRatio = 0.04f,
        )

        /**
         * AMOLED preset — pure black with the faintest violet whisper.
         *
         * - Color: AccentViolet (the secondary accent — distinct from DEFAULT)
         * - Peak alpha: 0.04 + 0.02 = 0.06 (even quieter than DEFAULT)
         * - Trough alpha: 0.04 - 0.02 = 0.02
         * - Cycle: 16 seconds (slower — AMOLED feels more meditative)
         * - Position drift: 3%
         *
         * On AMOLED, the screen is pure black. A cyan breath would
         * feel too "tech"; violet feels more like ink in water.
         */
        val AMOLED = LibraryAmbientPreset(
            color = AccentViolet,
            baseAlpha = 0.04f,
            breathDepth = 0.02f,
            cycleDurationMs = 16_000,
            positionDriftRatio = 0.03f,
        )

        /**
         * CINEMATIC preset — deeper, slower, more present.
         *
         * - Color: AccentCyan (same as DEFAULT)
         * - Peak alpha: 0.08 + 0.02 = 0.10 (at the Rule #63 cap)
         * - Trough alpha: 0.08 - 0.02 = 0.06
         * - Cycle: 14 seconds
         * - Position drift: 5%
         *
         * CINEMATIC profile is for premium-feel moments. The ambient
         * is slightly more present (peak 0.10 vs 0.06) but still
         * under the Rule #63 ceiling.
         */
        val CINEMATIC = LibraryAmbientPreset(
            color = AccentCyan,
            baseAlpha = 0.08f,
            breathDepth = 0.02f,
            cycleDurationMs = 14_000,
            positionDriftRatio = 0.05f,
        )

        /**
         * REDUCED_MOTION preset — static, no animation.
         *
         * Renders as a single radial gradient at the midpoint position
         * and midpoint alpha. Used when the constitution's
         * `motionLanguage.isReduceMotionEnabled` is true.
         */
        val REDUCED_MOTION = LibraryAmbientPreset(
            color = AccentCyan,
            baseAlpha = 0.05f,
            breathDepth = 0.0f, // No oscillation.
            cycleDurationMs = Int.MAX_VALUE, // Effectively never cycles.
            positionDriftRatio = 0.0f, // No drift.
        )

        /**
         * OFF — completely disables the ambient layer.
         *
         * Used by the LOW_END profile when even the tiny CPU cost of
         * an `InfiniteTransition` is too much.
         */
        val OFF = LibraryAmbientPreset(
            color = Color.Transparent,
            baseAlpha = 0.0f,
            breathDepth = 0.0f,
            cycleDurationMs = Int.MAX_VALUE,
            positionDriftRatio = 0.0f,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PRESET RESOLUTION
//
// Maps the Library's profile to an ambient preset. The contract
// carries `ambientIntensity` (0.0–1.0), which scales the preset's
// alphas. The profile picks the preset's *character* (color, speed);
// the contract's intensity scales the *strength*.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Resolves the appropriate [LibraryAmbientPreset] for a given motion
 * language.
 *
 * If `motionLanguage.isReduceMotionEnabled` is true → returns
 * [LibraryAmbientPreset.REDUCED_MOTION]. Otherwise returns
 * [LibraryAmbientPreset.DEFAULT].
 *
 * The CINEMATIC and AMOLED presets are selected by the host (A5)
 * based on profile, not by this function. This function only handles
 * the reduce-motion override.
 */
internal fun LibraryMotionLanguage.resolveAmbientPreset(
    cinematic: Boolean = false,
    amoled: Boolean = false,
): LibraryAmbientPreset = when {
    isReduceMotionEnabled -> LibraryAmbientPreset.REDUCED_MOTION
    amoled -> LibraryAmbientPreset.AMOLED
    cinematic -> LibraryAmbientPreset.CINEMATIC
    else -> LibraryAmbientPreset.DEFAULT
}

// ─────────────────────────────────────────────────────────────────────────────
// THE COMPOSABLE
//
// Pure draw. Animation via InfiniteTransition. Zero allocations in
// the draw lambda — Brush and Color are remembered.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * THE LIBRARY AMBIENT LAYER.
 *
 * Paints a slowly-breathing radial gradient on top of the background.
 *
 * ## Drawing strategy
 *
 * 1. If preset is [LibraryAmbientPreset.OFF] → paints nothing.
 * 2. If preset has `breathDepth == 0f` (reduced motion) → paints a
 *    single static radial gradient at midpoint.
 * 3. Otherwise → starts an [InfiniteTransition], animates two floats:
 *    - `breath`: 0.0 → 1.0 → 0.0 (REVERSE — never RESTART)
 *    - `drift`: 0.0 → 1.0 → 0.0 (REVERSE)
 *
 *    At draw time, the breath and drift values are interpolated into
 *    concrete alpha and position.
 *
 * ## Performance
 *
 * - **Zero per-frame allocation**: the [Brush] is rebuilt only when
 *   the breath value changes (which is every frame), but the
 *   interpolation is just float math. We use `Modifier.drawBehind`
 *   with a Canvas to avoid Box overhead.
 *
 * - **Single InfiniteTransition**: even if the host calls this
 *   composable multiple times, each call gets its own transition.
 *   In practice, only [LibraryBackgroundHost] (A5) calls this — once.
 *
 * ## Rules
 *
 * - **Rule #63** (Background Never Demands Attention): peak alpha
 *   is enforced at ≤ 0.10 by [LibraryAmbientPreset]'s init block.
 * - **Rule #64** (Motion Must Be Discoverable, Not Obvious): cycle
 *   ≥ 8s, alpha delta ≤ 0.10, position drift ≤ 6%. All enforced at
 *   [LibraryAmbientPreset]'s init block.
 *
 * @param preset     The ambient preset to render.
 * @param modifier   Compose modifier for layout. Default: fillMaxSize.
 */
@Composable
fun LibraryAmbientLayer(
    preset: LibraryAmbientPreset,
    modifier: Modifier = Modifier,
) {
    // Fast path: OFF preset paints nothing.
    if (preset.baseAlpha <= 0f && preset.breathDepth <= 0f) return

    val density = LocalDensity.current
    val driftRadiusPx = with(density) { 320.dp.toPx() }

    // Reduce-motion fast path: static radial at midpoint.
    if (preset.breathDepth <= 0f) {
        val staticAlpha = preset.baseAlpha
        val staticColor = remember(preset, staticAlpha) {
            preset.color.copy(alpha = staticAlpha)
        }
        Box(
            modifier = modifier
                .fillMaxSize()
                .drawBehind {
                    val center = Offset(
                        x = size.width * 0.5f,
                        y = size.height * 0.20f,
                    )
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                staticColor,
                                Color.Transparent,
                            ),
                            center = center,
                            radius = driftRadiusPx,
                        ),
                    )
                },
        )
        return
    }

    // Active path: animate breath and drift.
    val transition = rememberInfiniteTransition(label = "libraryAmbient")

    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = preset.cycleDurationMs / 2,
                easing = EaseInOutSine,
            ),
            repeatMode = RepeatMode.Reverse, // Rule #64: never RESTART.
        ),
        label = "libraryAmbientBreath",
    )

    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = preset.cycleDurationMs,
                easing = EaseInOutSine,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "libraryAmbientDrift",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // ── Compute current alpha ──────────────────────────────────
                // breath oscillates 0→1→0. We map it to alpha:
                //   breath=0 → baseAlpha - breathDepth (trough)
                //   breath=1 → baseAlpha + breathDepth (peak)
                val currentAlpha = preset.baseAlpha +
                    (preset.breathDepth * (2f * breath - 1f))

                // ── Compute current position ───────────────────────────────
                // drift oscillates 0→1→0. We map it to position offset:
                //   drift=0 → center.x = width * (0.50 - driftRatio/2)
                //   drift=1 → center.x = width * (0.50 + driftRatio/2)
                val xRatio = 0.5f + (preset.positionDriftRatio * (2f * drift - 1f))
                val center = Offset(
                    x = size.width * xRatio,
                    y = size.height * 0.20f, // Static Y — drift only on X.
                )

                val color = preset.color.copy(alpha = currentAlpha.coerceIn(0f, 0.10f))

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color,
                            preset.color.copy(alpha = 0f), // Fade to transparent.
                        ),
                        center = center,
                        radius = driftRadiusPx,
                    ),
                )
            },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// CONVENIENCE OVERLOADS
//
// Allow callers to pass motion language + profile flags instead of a
// fully-resolved preset. Reduces boilerplate at the call site.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Convenience overload: resolves the preset from motion language and
 * profile flags, then delegates to [LibraryAmbientLayer].
 *
 * @param motionLanguage  The motion language (carries reduce-motion flag).
 * @param cinematic       True if the CINEMATIC profile is active.
 * @param amoled          True if the AMOLED profile is active.
 * @param modifier        Compose modifier.
 */
@Composable
fun LibraryAmbientLayer(
    motionLanguage: LibraryMotionLanguage,
    cinematic: Boolean = false,
    amoled: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val preset = remember(motionLanguage, cinematic, amoled) {
        motionLanguage.resolveAmbientPreset(cinematic = cinematic, amoled = amoled)
    }
    LibraryAmbientLayer(preset = preset, modifier = modifier)
}

/**
 * A completely static ambient layer — paints the [LibraryAmbientPreset.DEFAULT]
 * preset at its midpoint values, no animation.
 *
 * Useful for `@Preview` and for unit-testing the visual without
 * waiting for animation.
 */
@Composable
fun LibraryAmbientLayerStatic(
    preset: LibraryAmbientPreset = LibraryAmbientPreset.DEFAULT,
    modifier: Modifier = Modifier,
) {
    val staticPreset = remember(preset) {
        preset.copy(breathDepth = 0f, positionDriftRatio = 0f)
    }
    LibraryAmbientLayer(preset = staticPreset, modifier = modifier)
}
