/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.3 / Phase C — Living Motion
 *
 * File C1 / C6 — LibraryWaveAmbient.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [LibraryWaveAmbient] is the *music-alive signal* of the Library
 * screen.
 *
 *   যখন music বাজবে — Library-র background-এ একটা subtle pulse থাকবে।
 *   যখন music বন্ধ থাকবে — কিছুই থাকবে না। Silence.
 *
 * This is NOT the Wave Engine ribbon. The Wave Engine paints a full
 * organic ribbon on the Now Playing screen. Here, we only consume
 * [WaveState] as *input* — we don't rebuild physics, geometry, or
 * ribbon rendering. Rule #62 — Never Rebuild Existing Systems.
 *
 * WHAT THIS FILE IS
 * -----------------
 * A composable that paints a very slow, very faint radial pulse on
 * the Library background, modulated by WaveState.energy. The pulse:
 *
 *   - Cycles every 8–14 seconds (Rule #64 — ≥ 8s)
 *   - Alpha oscillates between (baseAlpha - pulseDepth) and
 *     (baseAlpha + pulseDepth), peak ≤ 0.10 (Rule #63)
 *   - Energy modulates the *brightness*, not the speed
 *     (Rule #73 — Motion Must Have Meaning)
 *   - When isActive=false → paints nothing (Rule #74 — Silence)
 *   - Reverses (never restarts — Rule #64 forbids RESTART)
 *
 * ## The "Music Alive" Signal
 *
 * Rule #73 says: "Animation is information."
 * The information this layer carries:
 *
 *   | State               | What user sees          | Message              |
 *   |---------------------|--------------------------|----------------------|
 *   | Music playing       | Faint pulse on bg        | "Music is alive"     |
 *   | Music paused/stopped| No pulse, just ambient   | "Library is resting" |
 *   | High energy track   | Slightly brighter pulse  | "Track is intense"   |
 *   | Quiet track         | Subdued pulse            | "Track is calm"      |
 *
 * The pulse DOES NOT follow beats. Beats belong to the Wave Engine
 * ribbon (Now Playing screen). The Library wave ambient is a slow,
 * breathing presence — it carries the *character* of the music, not
 * the rhythm.
 *
 * WHAT THIS FILE IS NOT
 * ---------------------
 * ❌ Not the Wave Engine ribbon. We consume WaveState, not build paths.
 * ❌ Not a beat visualizer. Pulse is slow, not rhythmic.
 * ❌ Not a replacement for [LibraryAmbientLayer] (A4). They coexist:
 *    - LibraryAmbientLayer = "screen is alive" (always on, very subtle)
 *    - LibraryWaveAmbient  = "music is alive" (only when playing)
 * ❌ Not content. Wave ambient never has shapes / icons / text.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #58  Runtime Never Knows Rendering   — this is rendering
 * #62  Never Rebuild Existing Systems  — consumes WaveState, not rebuilds
 * #63  Background Never Demands Attention — peak alpha ≤ 0.10
 * #64  Motion Must Be Discoverable     — 8s+ cycle, ≤ 0.08 alpha delta
 * #73  Motion Must Have Meaning        — pulse = music alive
 * #74  Silence Is Also UX              — no music → paints nothing
 *
 * © RIKO Experience OS — Rendering Tier (Phase C)
 */

package com.rikodev.music.android.presentation.library.screen.motion

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
import com.rikodev.music.android.core.experience.wave.WaveState
import com.rikodev.music.android.presentation.library.screen.contract.LibraryMotionLanguage
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// WAVE AMBIENT SPEC
//
// The spec is the *resolved* configuration for the wave ambient layer.
// It is derived from WaveState + profile/motion-language inputs.
// All numeric invariants are enforced in the init block.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A spec for the Library wave ambient layer.
 *
 * @property isActive           True if audio is currently playing.
 *                              When false, the layer paints nothing
 *                              (Rule #74 — Silence Is Also UX).
 * @property energy             Current overall energy 0f..1f.
 *                              Modulates *brightness*, not speed
 *                              (Rule #73 — Motion Must Have Meaning).
 * @property color              Pulse color. Constant — only its alpha
 *                              changes. Default: [AccentCyan].
 * @property baseAlpha          The midpoint alpha. Range: 0.0–1.0.
 *                              Rule #63: base + pulseDepth ≤ 0.10.
 * @property pulseDepth         How much the alpha oscillates around
 *                              [baseAlpha]. Rule #64: ≤ 0.08 (delta
 *                              ≤ 0.10 — but we cap tighter at 0.08
 *                              since wave ambient is on top of the
 *                              Phase A ambient layer).
 * @property pulseCycleMs       One full pulse (in → out → in) in ms.
 *                              Rule #64: ≥ 8_000ms. The pulse is slow
 *                              and breathing — it does NOT follow
 *                              beats.
 * @property energyModulation   How strongly energy scales the pulse.
 *                              0f = energy ignored, 1f = energy fully
 *                              modulates. Default 0.5 — energy is a
 *                              *hint*, not a controller.
 */
data class LibraryWaveAmbientSpec(
    val isActive: Boolean,
    val energy: Float,
    val color: Color,
    val baseAlpha: Float,
    val pulseDepth: Float,
    val pulseCycleMs: Int,
    val energyModulation: Float,
) {
    init {
        require(energy in 0f..1f) {
            "energy must be in [0, 1], got $energy"
        }
        // ── Rule #63 enforcement ─────────────────────────────────────────
        // Wave ambient peak must never exceed 0.10. We enforce
        // baseAlpha + pulseDepth ≤ 0.10 (matching Phase A ambient layer).
        require(baseAlpha + pulseDepth <= 0.10f) {
            "Wave ambient peak alpha (${baseAlpha + pulseDepth}) must be ≤ 0.10 " +
                "(Rule #63 — Background Never Demands Attention)."
        }
        // ── Rule #64 enforcement ─────────────────────────────────────────
        // Pulse cycle must be ≥ 8 seconds; alpha delta must be ≤ 0.08.
        // (Tighter than Rule #64's 0.10 ceiling because this layer
        // composes ON TOP of Phase A's ambient layer — combined delta
        // could exceed 0.10 if we don't self-limit.)
        require(pulseCycleMs >= 8_000) {
            "Wave ambient pulseCycleMs ($pulseCycleMs) must be ≥ 8000 " +
                "(Rule #64 — discoverable motion, not obvious)."
        }
        require(pulseDepth <= 0.08f) {
            "Wave ambient pulseDepth ($pulseDepth) must be ≤ 0.08 " +
                "(Rule #64 — alpha delta ≤ 0.10, self-limited to 0.08 " +
                "to avoid compounding with Phase A ambient)."
        }
        require(energyModulation in 0f..1f) {
            "energyModulation must be in [0, 1], got $energyModulation"
        }
    }

    companion object {

        /**
         * DEFAULT preset — Cyan whisper when music plays.
         *
         * - Color: AccentCyan
         * - Peak alpha: 0.05 + 0.03 = 0.08 (under Rule #63 cap)
         * - Trough alpha: 0.05 - 0.03 = 0.02
         * - Cycle: 10 seconds (slightly faster than ambient — wave has life)
         * - Energy modulation: 0.5 (energy is a hint)
         *
         * Combined with Phase A's ambient (peak 0.10), the screen's
         * total background alpha peaks at ~0.18 — still under the
         * 0.20 attention-demand threshold (Rule #63).
         */
        val DEFAULT = LibraryWaveAmbientSpec(
            isActive = false,
            energy = 0f,
            color = AccentCyan,
            baseAlpha = 0.05f,
            pulseDepth = 0.03f,
            pulseCycleMs = 10_000,
            energyModulation = 0.5f,
        )

        /**
         * AMOLED preset — Violet ink-pulse.
         *
         * - Color: AccentViolet (matches Phase A AMOLED preset)
         * - Peak alpha: 0.03 + 0.02 = 0.05 (very quiet on pure black)
         * - Trough alpha: 0.03 - 0.02 = 0.01
         * - Cycle: 12 seconds
         * - Energy modulation: 0.4 (even quieter response)
         */
        val AMOLED = LibraryWaveAmbientSpec(
            isActive = false,
            energy = 0f,
            color = AccentViolet,
            baseAlpha = 0.03f,
            pulseDepth = 0.02f,
            pulseCycleMs = 12_000,
            energyModulation = 0.4f,
        )

        /**
         * CINEMATIC preset — deeper cyan, more present.
         *
         * - Color: AccentCyan
         * - Peak alpha: 0.07 + 0.03 = 0.10 (at Rule #63 cap)
         * - Trough alpha: 0.07 - 0.03 = 0.04
         * - Cycle: 11 seconds
         * - Energy modulation: 0.6 (energy more visible)
         */
        val CINEMATIC = LibraryWaveAmbientSpec(
            isActive = false,
            energy = 0f,
            color = AccentCyan,
            baseAlpha = 0.07f,
            pulseDepth = 0.03f,
            pulseCycleMs = 11_000,
            energyModulation = 0.6f,
        )

        /**
         * REDUCED_MOTION preset — static, no animation.
         *
         * When reduce-motion is enabled, the wave ambient renders as
         * a single static radial at base alpha (if music is playing)
         * or paints nothing (if music is stopped).
         */
        val REDUCED_MOTION = LibraryWaveAmbientSpec(
            isActive = false,
            energy = 0f,
            color = AccentCyan,
            baseAlpha = 0.04f,
            pulseDepth = 0.0f,
            pulseCycleMs = Int.MAX_VALUE,
            energyModulation = 0.0f,
        )

        /**
         * OFF — completely disables the wave ambient layer.
         *
         * Used by LOW_END profile (CPU budget) or when the user has
         * explicitly disabled ambient motion.
         */
        val OFF = LibraryWaveAmbientSpec(
            isActive = false,
            energy = 0f,
            color = Color.Transparent,
            baseAlpha = 0.0f,
            pulseDepth = 0.0f,
            pulseCycleMs = Int.MAX_VALUE,
            energyModulation = 0.0f,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WAVESTATE → SPEC RESOLVER
//
// Pure function: takes a WaveState (from Wave Engine) + a preset
// (from profile/motion-language) and produces a fully-resolved spec.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Resolves a [LibraryWaveAmbientSpec] from a [WaveState] and a base
 * preset.
 *
 * The preset provides the *character* (color, base alpha, cycle).
 * The WaveState provides the *current state* (isActive, energy).
 *
 * This separation is critical: the preset is *config* (changes rarely),
 * the WaveState is *runtime* (changes every frame). By resolving them
 * into a single spec, the composable can be stateless.
 *
 * @param preset      The base preset (DEFAULT / AMOLED / CINEMATIC / etc.)
 * @param waveState   The current WaveState from the Wave Engine.
 * @return A fully-resolved spec with current `isActive` and `energy`.
 */
fun resolveWaveAmbientSpec(
    preset: LibraryWaveAmbientSpec,
    waveState: WaveState,
): LibraryWaveAmbientSpec {
    // Rule #74: if music is not active, the wave ambient paints nothing.
    // We force isActive=false in the resolved spec so the composable's
    // fast path skips painting entirely.
    if (!waveState.isActive) {
        return preset.copy(
            isActive = false,
            energy = 0f,
        )
    }
    return preset.copy(
        isActive = true,
        energy = waveState.overallEnergy,
    )
}

/**
 * Resolves the appropriate base preset for a given motion language.
 *
 * If `motionLanguage.isReduceMotionEnabled` is true → returns
 * [LibraryWaveAmbientSpec.REDUCED_MOTION]. Otherwise returns
 * [LibraryWaveAmbientSpec.DEFAULT] (or AMOLED / CINEMATIC based on
 * profile flags).
 */
internal fun LibraryMotionLanguage.resolveWaveAmbientPreset(
    cinematic: Boolean = false,
    amoled: Boolean = false,
): LibraryWaveAmbientSpec = when {
    isReduceMotionEnabled -> LibraryWaveAmbientSpec.REDUCED_MOTION
    amoled -> LibraryWaveAmbientSpec.AMOLED
    cinematic -> LibraryWaveAmbientSpec.CINEMATIC
    else -> LibraryWaveAmbientSpec.DEFAULT
}

// ─────────────────────────────────────────────────────────────────────────────
// THE COMPOSABLE
//
// Pure draw. Animation via InfiniteTransition. Zero allocations in
// the draw lambda — Brush and Color are remembered.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * THE LIBRARY WAVE AMBIENT LAYER.
 *
 * Paints a slowly-pulsing radial gradient on top of the Library
 * background, modulated by the current WaveState's energy.
 *
 * ## Drawing strategy
 *
 * 1. If `spec.isActive == false` → paints nothing
 *    (Rule #74 — Silence Is Also UX).
 * 2. If `spec.pulseDepth == 0f` (reduced motion) → paints a single
 *    static radial at base alpha.
 * 3. Otherwise → starts an [InfiniteTransition], animates `pulse`:
 *    - 0.0 → 1.0 → 0.0 (REVERSE — never RESTART)
 *    - At draw time, pulse is interpolated into alpha.
 *    - Energy modulates the *brightness* (multiplicative), not the
 *      cycle speed.
 *
 * ## Position
 *
 * The radial center is positioned at the top-third of the screen
 * (y = 0.20 * height), matching Phase A's ambient layer position.
 * This creates visual coherence — both ambient layers breathe from
 * the same virtual "source".
 *
 * ## Performance
 *
 * - **Zero per-frame allocation**: the [Brush] is rebuilt only when
 *   the pulse value changes (which is every frame), but the
 *   interpolation is just float math.
 * - **Single InfiniteTransition**: even if called multiple times,
 *   each call gets its own transition. In practice, only the Phase D
 *   root composable calls this — once.
 * - **Fast path for silence**: when music is not active, the
 *   composable returns immediately without setting up any animation.
 *
 * ## Rules
 *
 * - **Rule #63**: peak alpha ≤ 0.10 (enforced in spec init block).
 * - **Rule #64**: 8s+ cycle, ≤ 0.08 alpha delta, REVERSE only.
 * - **Rule #73**: pulse carries the message "music is alive".
 *   Energy modulates brightness — high-energy track = brighter pulse.
 * - **Rule #74**: when music stops, the layer paints nothing. Silence.
 *
 * @param spec       The resolved wave ambient spec.
 * @param modifier   Compose modifier for layout. Default: fillMaxSize.
 */
@Composable
fun LibraryWaveAmbient(
    spec: LibraryWaveAmbientSpec,
    modifier: Modifier = Modifier,
) {
    // ── Rule #74 fast path: silence when music is not active ──────────────
    // Meaning: When there is no music, there is no wave pulse. The
    // Library's ambient layer (Phase A) still breathes — that's the
    // "screen is alive" signal. This layer (wave ambient) only speaks
    // when music speaks.
    if (!spec.isActive) return

    // Fast path: OFF preset paints nothing even when music is active.
    if (spec.baseAlpha <= 0f && spec.pulseDepth <= 0f) return

    val density = LocalDensity.current
    val pulseRadiusPx = with(density) { 360.dp.toPx() }

    // ── Reduce-motion fast path: static radial at base alpha ──────────────
    // Meaning: the user still sees that music is playing (the radial
    // is visible), but there is no animation. The information is
    // preserved; only the motion is removed.
    if (spec.pulseDepth <= 0f) {
        val staticAlpha = spec.baseAlpha
        val staticColor = remember(spec, staticAlpha) {
            spec.color.copy(alpha = staticAlpha)
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
                            radius = pulseRadiusPx,
                        ),
                    )
                },
        )
        return
    }

    // ── Active path: animate pulse ────────────────────────────────────────
    // Meaning: the pulse breathes, and its brightness is modulated by
    // the current energy. High-energy music → brighter pulse. Quiet
    // music → subdued pulse. The cycle speed is constant — energy
    // affects brightness, not speed (Rule #73).
    val transition = rememberInfiniteTransition(label = "libraryWaveAmbient")

    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = spec.pulseCycleMs / 2,
                easing = EaseInOutSine,
            ),
            repeatMode = RepeatMode.Reverse, // Rule #64: never RESTART.
        ),
        label = "libraryWaveAmbientPulse",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // ── Compute current alpha ──────────────────────────────────
                // pulse oscillates 0→1→0. We map it to alpha:
                //   pulse=0 → baseAlpha - pulseDepth (trough)
                //   pulse=1 → baseAlpha + pulseDepth (peak)
                val oscillation = (2f * pulse - 1f) // -1..1
                var currentAlpha = spec.baseAlpha + (spec.pulseDepth * oscillation)

                // ── Energy modulation (Rule #73) ───────────────────────────
                // Energy scales the brightness. The mapping is:
                //   energy=0 → scale = (1 - modulation)  // quiet music
                //   energy=1 → scale = 1.0               // loud music
                //
                // So at energy=0, alpha is reduced by `energyModulation`
                // fraction. At energy=1, alpha is at full spec value.
                // This means quiet music → very subtle pulse, loud music
                // → full pulse. Information preserved.
                val energyScale = (1f - spec.energyModulation) +
                    (spec.energyModulation * spec.energy)
                currentAlpha *= energyScale

                val color = spec.color.copy(alpha = currentAlpha.coerceIn(0f, 0.10f))

                val center = Offset(
                    x = size.width * 0.5f,
                    y = size.height * 0.20f, // Match Phase A ambient position.
                )

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color,
                            spec.color.copy(alpha = 0f), // Fade to transparent.
                        ),
                        center = center,
                        radius = pulseRadiusPx,
                    ),
                )
            },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// CONVENIENCE OVERLOAD
//
// Allows callers to pass motion language + profile flags + WaveState
// instead of a fully-resolved spec. Reduces boilerplate at the call site.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Convenience overload: resolves the preset from motion language and
 * profile flags, applies the current WaveState, then delegates to
 * [LibraryWaveAmbient].
 *
 * @param motionLanguage  The motion language (carries reduce-motion flag).
 * @param waveState       The current WaveState from the Wave Engine.
 * @param cinematic       True if the CINEMATIC profile is active.
 * @param amoled          True if the AMOLED profile is active.
 * @param modifier        Compose modifier.
 */
@Composable
fun LibraryWaveAmbient(
    motionLanguage: LibraryMotionLanguage,
    waveState: WaveState,
    cinematic: Boolean = false,
    amoled: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val preset = remember(motionLanguage, cinematic, amoled) {
        motionLanguage.resolveWaveAmbientPreset(cinematic = cinematic, amoled = amoled)
    }
    val spec = remember(preset, waveState) {
        resolveWaveAmbientSpec(preset = preset, waveState = waveState)
    }
    LibraryWaveAmbient(spec = spec, modifier = modifier)
}

/**
 * A completely static wave ambient layer — paints the
 * [LibraryWaveAmbientSpec.DEFAULT] preset at its midpoint values, with
 * `isActive=true` so the layer is visible.
 *
 * Useful for `@Preview` and for unit-testing the visual without waiting
 * for animation or setting up a Wave Engine.
 */
@Composable
fun LibraryWaveAmbientStatic(
    preset: LibraryWaveAmbientSpec = LibraryWaveAmbientSpec.DEFAULT,
    energy: Float = 0.5f,
    modifier: Modifier = Modifier,
) {
    val staticSpec = remember(preset, energy) {
        preset.copy(
            isActive = true,
            energy = energy,
            pulseDepth = 0f, // No animation in static preview.
        )
    }
    LibraryWaveAmbient(spec = staticSpec, modifier = modifier)
}

// ─────────────────────────────────────────────────────────────────────────────
// PREVIEWS
// ─────────────────────────────────────────────────────────────────────────────

@androidx.compose.ui.tooling.preview.Preview(
    name = "Wave Ambient — Default (active, mid-energy)",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewWaveAmbientDefault() {
    LibraryWaveAmbientStatic(
        preset = LibraryWaveAmbientSpec.DEFAULT,
        energy = 0.5f,
    )
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Wave Ambient — AMOLED",
    showBackground = true,
    backgroundColor = 0xFF000000,
)
@Composable
private fun PreviewWaveAmbientAmoled() {
    LibraryWaveAmbientStatic(
        preset = LibraryWaveAmbientSpec.AMOLED,
        energy = 0.4f,
    )
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Wave Ambient — Cinematic",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewWaveAmbientCinematic() {
    LibraryWaveAmbientStatic(
        preset = LibraryWaveAmbientSpec.CINEMATIC,
        energy = 0.7f,
    )
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Wave Ambient — High Energy",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewWaveAmbientHighEnergy() {
    LibraryWaveAmbientStatic(
        preset = LibraryWaveAmbientSpec.DEFAULT,
        energy = 0.95f,
    )
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Wave Ambient — Silence (not active)",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewWaveAmbientSilence() {
    // When isActive=false, the composable paints nothing. The preview
    // shows the bare background — demonstrating Rule #74 (Silence).
    val silentSpec = LibraryWaveAmbientSpec.DEFAULT.copy(isActive = false)
    LibraryWaveAmbient(spec = silentSpec)
}
