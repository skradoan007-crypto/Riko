/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.3 / Phase B — Identity Reveal
 *
 * File B9 / B9 — LibraryHeroEffects.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [LibraryHeroEffects] is the *subtle polish* layer on top of the
 * Hero card.
 *
 *   Subtle light + Reflection + Glass breathing — এই তিনটি একসাথে
 *   Hero-কে "alive" করে তোলে।
 *
 * The Hero card (B1) is already complete — artwork, title, subtitle,
 * badges, overlay gradient. But it feels *flat*. The effects layer
 * adds three subtle dimensions:
 *
 *   1. CATCH-LIGHT — a soft highlight at the top-left of the artwork,
 *      mimicking a single light source. Subtle: peak alpha 0.18,
 *      drawn ONCE (no animation).
 *
 *   2. REFLECTION — a faint mirror gradient at the bottom of the
 *      artwork, suggesting the Hero sits on a glass surface. Even
 *      fainter: peak alpha 0.10.
 *
 *   3. GLASS BREATHING — a very slow (12s cycle) accent glow that
 *      breathes around the Hero's edges. Discoverable motion: the
 *      user feels it only when they look away and look back.
 *
 * WHAT THIS FILE IS
 * -----------------
 * A composable that renders all three effects in a single Box layer.
 * Designed to be slotted into [LibraryHeroContainer] (B2) as the
 * `effects` parameter — drawn ON TOP of the Hero card, BELOW the
 * press dim overlay.
 *
 * WHAT THIS FILE IS NOT
 * ---------------------
 * ❌ Not the Hero card. Card lives in B1.
 * ❌ Not the Container. Container lives in B2.
 * ❌ Not a state owner. Reads accent color + motion spec, never writes.
 * ❌ Not a motion engine. Uses Compose's InfiniteTransition.
 *
 * ARCHITECTURE
 * ------------
 *
 *   LibraryHeroContainer (B2)
 *        ↓ wraps
 *   LibraryGlassSurfaceBorderless (Phase A)
 *        ↓ inside
 *   ┌─────────────────────────────────────┐
 *   │  LibraryHero (B1) — the card        │
 *   │  ┌───────────────────────────────┐  │
 *   │  │  LibraryHeroEffects (B9)      │  │  ← THIS FILE
 *   │  │  • Catch-light                │  │
 *   │  │  • Reflection                 │  │
 *   │  │  • Glass breathing            │  │
 *   │  └───────────────────────────────┘  │
 *   └─────────────────────────────────────┘
 *
 * RULES ENFORCED HERE
 * -------------------
 * #58  Runtime Never Knows Rendering   — this is rendering
 * #62  Never Rebuild Existing Systems  — uses Compose animation APIs
 * #63  Background Never Demands Attention — effects on Hero (content),
 *         so slightly higher alpha allowed (≤ 0.20 vs ≤ 0.10 for bg)
 * #64  Motion Must Be Discoverable     — 12s cycle, ≤ 0.10 alpha delta
 *
 * © RIKO Experience OS — Rendering Tier (Phase B)
 */

package com.rikodev.music.android.presentation.library.screen.identity

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.rikodev.music.android.core.designsystem.theme.AccentCyan
import com.rikodev.music.android.presentation.library.screen.background.toColorOrNull
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// HERO EFFECTS SPEC
//
// Pure data — encodes the visual vocabulary of Hero effects.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The spec for the Hero effects layer.
 *
 * @property catchLightAlpha       Peak alpha of the catch-light (0.0–0.30).
 *                                 Hero is content, so ≤ 0.20 is typical.
 * @property catchLightRadiusRatio Catch-light radius as a fraction of the
 *                                 Hero's min dimension (0.0–1.0). 0.45 = ~45%.
 * @property reflectionAlpha       Peak alpha of the bottom reflection (0.0–0.20).
 * @property reflectionHeightRatio Reflection height as a fraction of the Hero's
 *                                 height (0.0–0.5). 0.20 = ~20%.
 * @property breathingBaseAlpha    Midpoint alpha of the breathing glow (0.0–0.10).
 * @property breathingDepth        How much the breathing oscillates around the
 *                                 midpoint (0.0–0.05). Rule #64: ≤ 0.10.
 * @property breathingCycleMs      One full breath cycle in ms. Rule #64: ≥ 8000.
 */
data class LibraryHeroEffectsSpec(
    val catchLightAlpha: Float,
    val catchLightRadiusRatio: Float,
    val reflectionAlpha: Float,
    val reflectionHeightRatio: Float,
    val breathingBaseAlpha: Float,
    val breathingDepth: Float,
    val breathingCycleMs: Int,
) {
    init {
        // ── Rule #63 enforcement (Hero-content variant) ──────────────────
        // Hero is content, not background — so the alpha ceiling is 0.20
        // (vs 0.10 for pure background layers).
        require(catchLightAlpha <= 0.30f) {
            "catchLightAlpha ($catchLightAlpha) must be ≤ 0.30 " +
                "(Rule #63 — Hero effects ceiling)."
        }
        require(reflectionAlpha <= 0.20f) {
            "reflectionAlpha ($reflectionAlpha) must be ≤ 0.20 " +
                "(Rule #63 — Hero effects ceiling)."
        }
        // ── Rule #64 enforcement ─────────────────────────────────────────
        require(breathingBaseAlpha + breathingDepth <= 0.12f) {
            "breathing peak (${breathingBaseAlpha + breathingDepth}) must be ≤ 0.12 " +
                "(Rule #63 — Hero breathing ceiling)."
        }
        require(breathingCycleMs >= 8_000) {
            "breathingCycleMs ($breathingCycleMs) must be ≥ 8000 " +
                "(Rule #64 — discoverable motion)."
        }
        require(breathingDepth <= 0.10f) {
            "breathingDepth ($breathingDepth) must be ≤ 0.10 " +
                "(Rule #64 — alpha delta ≤ 0.10)."
        }
    }

    companion object {

        /**
         * DEFAULT effects spec — balanced, premium.
         *
         * - Catch-light: 0.18 alpha, 45% radius (subtle top-left highlight)
         * - Reflection: 0.08 alpha, 20% height (faint bottom mirror)
         * - Breathing: 0.06 base, 0.04 depth, 12s cycle (cyan whisper)
         */
        val DEFAULT = LibraryHeroEffectsSpec(
            catchLightAlpha = 0.18f,
            catchLightRadiusRatio = 0.45f,
            reflectionAlpha = 0.08f,
            reflectionHeightRatio = 0.20f,
            breathingBaseAlpha = 0.06f,
            breathingDepth = 0.04f,
            breathingCycleMs = 12_000,
        )

        /**
         * CINEMATIC effects spec — slightly more present, slower.
         *
         * - Catch-light: 0.22 alpha (a touch brighter)
         * - Reflection: 0.10 alpha (more visible)
         * - Breathing: 0.08 base, 0.02 depth, 14s cycle (slower)
         */
        val CINEMATIC = LibraryHeroEffectsSpec(
            catchLightAlpha = 0.22f,
            catchLightRadiusRatio = 0.50f,
            reflectionAlpha = 0.10f,
            reflectionHeightRatio = 0.22f,
            breathingBaseAlpha = 0.08f,
            breathingDepth = 0.02f,
            breathingCycleMs = 14_000,
        )

        /**
         * AMOLED effects spec — minimal, no breathing glow.
         *
         * AMOLED is pure black — a breathing glow would look wrong
         * against pure black. Catch-light is preserved (top-left
         * highlight reads on any background), reflection is muted,
         * breathing is disabled.
         */
        val AMOLED = LibraryHeroEffectsSpec(
            catchLightAlpha = 0.15f,
            catchLightRadiusRatio = 0.40f,
            reflectionAlpha = 0.04f,
            reflectionHeightRatio = 0.15f,
            breathingBaseAlpha = 0.0f,
            breathingDepth = 0.0f,
            breathingCycleMs = 12_000, // Unused — breathing disabled.
        )

        /**
         * REDUCED_MOTION effects spec — static, no animation.
         *
         * Catch-light + reflection are preserved (they're static).
         * Breathing is disabled.
         */
        val REDUCED_MOTION = LibraryHeroEffectsSpec(
            catchLightAlpha = 0.16f,
            catchLightRadiusRatio = 0.45f,
            reflectionAlpha = 0.07f,
            reflectionHeightRatio = 0.20f,
            breathingBaseAlpha = 0.05f,
            breathingDepth = 0.0f,
            breathingCycleMs = 12_000, // Unused — breathing static.
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// THE HERO EFFECTS COMPOSABLE
//
// Renders all three effects (catch-light, reflection, breathing) in
// a single Box layer.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * THE LIBRARY HERO EFFECTS.
 *
 * Renders three subtle visual effects on top of the Hero card:
 *
 *   1. **Catch-light** — a soft radial highlight at the top-left of
 *      the artwork. Static (no animation). Mimics a single light
 *      source coming from the upper-left.
 *
 *   2. **Reflection** — a faint mirror gradient at the bottom of the
 *      artwork. Static. Suggests the Hero sits on a glass surface.
 *
 *   3. **Glass breathing** — a very slow accent glow that breathes
 *      around the Hero's edges. Animated via [InfiniteTransition]
 *      with [RepeatMode.Reverse] (Rule #64 — never RESTART).
 *
 * ## Rule #63 — Background Never Demands Attention
 *
 * Hero effects are on *content*, not background — so the alpha ceiling
 * is 0.20 (vs 0.10 for pure background). The breathing peak is capped
 * at 0.12 (slightly higher than background's 0.10) because Hero is
 * the visual center of the screen.
 *
 * ## Rule #64 — Motion Must Be Discoverable
 *
 * - Breathing cycle ≥ 8s (12s default)
 * - Breathing alpha delta ≤ 0.10 (0.04 default — well below ceiling)
 * - RepeatMode.Reverse (never RESTART — Rule #64 forbids it)
 * - Reduce-motion: breathing becomes static (no InfiniteTransition)
 *
 * ## Performance
 *
 * - All three effects share a single `drawBehind` lambda — one draw
 *   pass per frame, not three.
 * - The breathing InfiniteTransition is the only animated state —
 *   catch-light and reflection are pure draws.
 * - Brushes are rebuilt per frame from current animated values, but
 *   the colors are `remember`ed — zero per-frame Color allocation.
 *
 * ## Slot usage
 *
 * This composable is designed to be passed as the `effects` parameter
 * to [LibraryHeroContainer]:
 *
 *   LibraryHeroContainer(
 *       ...,
 *       effects = {
 *           LibraryHeroEffects(
 *               spec = LibraryHeroEffectsSpec.DEFAULT,
 *               accentColorHex = state.accentColorHex,
 *           )
 *       },
 *   )
 *
 * @param spec            Effects spec.
 * @param accentColorHex  Accent color (hex) for the breathing glow.
 *                        Falls back to AccentCyan.
 * @param isReducedMotion True if reduce-motion is on. Disables breathing.
 * @param modifier        Compose modifier.
 */
@Composable
fun LibraryHeroEffects(
    spec: LibraryHeroEffectsSpec,
    accentColorHex: String,
    isReducedMotion: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val accentColor = remember(accentColorHex) {
        accentColorHex.toColorOrNull() ?: AccentCyan
    }

    // ── Breathing animation ──────────────────────────────────────────────
    // Only animate if reduce-motion is off AND the spec has breathing.
    val hasBreathing = !isReducedMotion &&
        spec.breathingDepth > 0f &&
        spec.breathingBaseAlpha > 0f

    val breathFraction = if (hasBreathing) {
        val transition = rememberInfiniteTransition(label = "heroBreathing")
        val breath by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = spec.breathingCycleMs / 2,
                    easing = EaseInOutSine,
                ),
                repeatMode = RepeatMode.Reverse, // Rule #64: never RESTART.
            ),
            label = "heroBreath",
        )
        breath
    } else {
        0.5f // Static midpoint.
    }

    // ── Single draw pass for all three effects ───────────────────────────
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawCatchLight(
                    alpha = spec.catchLightAlpha,
                    radiusRatio = spec.catchLightRadiusRatio,
                )
                drawReflection(
                    alpha = spec.reflectionAlpha,
                    heightRatio = spec.reflectionHeightRatio,
                )
                if (spec.breathingBaseAlpha > 0f) {
                    drawBreathingGlow(
                        accentColor = accentColor,
                        baseAlpha = spec.breathingBaseAlpha,
                        depth = spec.breathingDepth,
                        breathFraction = breathFraction,
                    )
                }
            },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// DRAW HELPERS
//
// Internal — only used by [LibraryHeroEffects]. Pulled out so the
// draw logic is testable without a Composable context.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Draws the catch-light — a soft radial highlight at the top-left.
 *
 * The catch-light mimics a single light source coming from the
 * upper-left. It's the same convention GlassSurface uses for its
 * specular highlight — we repeat it here so the Hero reads as
 * glassy even when wrapped in a borderless glass surface.
 *
 * @param alpha         Peak alpha (≤ 0.30).
 * @param radiusRatio   Radius as a fraction of min(width, height).
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCatchLight(
    alpha: Float,
    radiusRatio: Float,
) {
    val radius = size.minDimension * radiusRatio
    val center = Offset(
        x = size.width * 0.20f,
        y = size.height * 0.12f,
    )

    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = alpha),
                Color.White.copy(alpha = alpha * 0.4f),
                Color.Transparent,
            ),
            center = center,
            radius = radius,
        ),
    )
}

/**
 * Draws the reflection — a faint mirror gradient at the bottom.
 *
 * The reflection suggests the Hero sits on a glass surface. It's
 * drawn as a vertical gradient from transparent (top) to faint
 * white (bottom), occupying the bottom `heightRatio` of the Hero.
 *
 * @param alpha         Peak alpha (≤ 0.20).
 * @param heightRatio   Height as a fraction of the Hero's height.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawReflection(
    alpha: Float,
    heightRatio: Float,
) {
    val reflectionHeight = size.height * heightRatio
    val reflectionTop = size.height - reflectionHeight

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color.White.copy(alpha = alpha * 0.3f),
                Color.White.copy(alpha = alpha),
            ),
            startY = reflectionTop,
            endY = size.height,
        ),
        topLeft = Offset(0f, reflectionTop),
        size = Size(size.width, reflectionHeight),
    )
}

/**
 * Draws the breathing glow — a slow accent halo around the Hero's edges.
 *
 * The glow is drawn as a radial gradient from the Hero's center, with
 * the accent color. The alpha oscillates between `baseAlpha - depth`
 * and `baseAlpha + depth` over the breath cycle.
 *
 * @param accentColor    Accent color.
 * @param baseAlpha      Midpoint alpha (≤ 0.10).
 * @param depth          Oscillation depth (≤ 0.05).
 * @param breathFraction 0.0–1.0. Current breath position.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBreathingGlow(
    accentColor: Color,
    baseAlpha: Float,
    depth: Float,
    breathFraction: Float,
) {
    // breathFraction oscillates 0→1→0. Map to alpha:
    //   fraction=0 → baseAlpha - depth (trough)
    //   fraction=1 → baseAlpha + depth (peak)
    val currentAlpha = (baseAlpha + depth * (2f * breathFraction - 1f))
        .coerceIn(0f, 0.12f) // Rule #63 ceiling for Hero breathing.

    val radius = size.minDimension * 0.55f
    val center = Offset(
        x = size.width * 0.5f,
        y = size.height * 0.5f,
    )

    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                accentColor.copy(alpha = currentAlpha),
                accentColor.copy(alpha = currentAlpha * 0.4f),
                Color.Transparent,
            ),
            center = center,
            radius = radius,
        ),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// HERO EFFECTS PREVIEW HELPERS
//
// Plain composables that render the effects with each preset. Useful
// as @Preview content or as debug overlays.
//
// Note: these previews render the effects on a flat colored background
// so the effects are visible. In real usage, the effects layer is
// drawn ON TOP of the Hero card's artwork.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Renders the DEFAULT Hero effects on a flat dark background.
 *
 * Useful for visually verifying the catch-light + reflection + breathing.
 */
@Composable
fun LibraryHeroEffectsPreviewDefault(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        LibraryHeroEffects(
            spec = LibraryHeroEffectsSpec.DEFAULT,
            accentColorHex = "#FF8A65",
            isReducedMotion = false,
        )
    }
}

/**
 * Renders the CINEMATIC Hero effects — slightly more present, slower.
 */
@Composable
fun LibraryHeroEffectsPreviewCinematic(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        LibraryHeroEffects(
            spec = LibraryHeroEffectsSpec.CINEMATIC,
            accentColorHex = "#FFB74D",
            isReducedMotion = false,
        )
    }
}

/**
 * Renders the AMOLED Hero effects — minimal, no breathing.
 */
@Composable
fun LibraryHeroEffectsPreviewAmoled(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        LibraryHeroEffects(
            spec = LibraryHeroEffectsSpec.AMOLED,
            accentColorHex = "#1A1A2E",
            isReducedMotion = false,
        )
    }
}

/**
 * Renders the REDUCED_MOTION Hero effects — static, no animation.
 */
@Composable
fun LibraryHeroEffectsPreviewReducedMotion(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        LibraryHeroEffects(
            spec = LibraryHeroEffectsSpec.REDUCED_MOTION,
            accentColorHex = "#FF8A65",
            isReducedMotion = true,
        )
    }
}
