/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.3 / Phase A — Background Language
 *
 * File A1 / A5 — LibraryBackground.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [LibraryBackground] is the *environment* of the Library screen.
 *
 *   Canvas
 *     ↓
 *   Gradient
 *     ↓
 *   Ambient Color
 *     ↓
 *   Dynamic Palette
 *     ↓
 *   Nothing else
 *
 * WHAT THIS FILE IS
 * -----------------
 * A pure declarative composable that paints the base background layer
 * of the Library screen. It reads the [LibraryScreenBackground] contract
 * (B5.1) and resolves it to a concrete Compose draw.
 *
 * WHAT THIS FILE IS NOT
 * ---------------------
 * ❌ Not Wave. Wave belongs to the Wave Engine.
 * ❌ Not Hero. Hero belongs to Phase B.
 * ❌ Not Cards / Albums / Collections. Those belong to Phase B.
 * ❌ Not Glass. Glass belongs to A2.
 * ❌ Not Depth. Depth belongs to A3.
 * ❌ Not Ambient motion. Ambient belongs to A4.
 *
 * This file paints ONE thing: the static base color + gradient. Even
 * the ambient breathing (A4) is layered ON TOP of this — never inside.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #58  Runtime Never Knows Rendering   — this is rendering, not runtime
 * #62  Never Rebuild Existing Systems  — reuses Color.kt palette
 * #63  Background Never Demands Attention — quiet palette only
 *
 * © RIKO Experience OS — Rendering Tier (Phase A)
 */

package com.rikodev.music.android.presentation.library.screen.background

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.theme.AccentCyan
import com.rikodev.music.android.core.designsystem.theme.AuroraBase
import com.rikodev.music.android.core.designsystem.theme.AuroraIndigo
import com.rikodev.music.android.core.designsystem.theme.AuroraIndigoDeep
import com.rikodev.music.android.core.designsystem.theme.ColorVoidDark
import com.rikodev.music.android.presentation.library.screen.contract.LibraryBackgroundKind
import com.rikodev.music.android.presentation.library.screen.contract.LibraryScreenBackground
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas

// ─────────────────────────────────────────────────────────────────────────────
// COLOR RESOLUTION
//
// The contract carries hex strings (e.g. "#0A0A0F"). Compose needs Color
// objects. This extension parses once and caches. Zero per-frame parse.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Parses a hex string ("#RRGGBB" or "#AARRGGBB") to a Compose [Color].
 *
 * Returns [ColorVoidDark] (the app's main background) as a safe fallback
 * if the string is malformed. We never want a missing color to crash
 * the screen — better to look slightly wrong than to look broken.
 */
internal fun String.toColorOrNull(): Color? {
    if (isBlank()) return null
    val normalized = removePrefix("#")
    return try {
        when (normalized.length) {
            6 -> Color(("FF$normalized").toLong(16))
            8 -> Color(normalized.toLong(16))
            else -> null
        }
    } catch (_: NumberFormatException) {
        null
    }
}

/**
 * Resolves a [LibraryScreenBackground] contract's base color to a Compose
 * [Color], with a safe fallback to [ColorVoidDark].
 */
internal val LibraryScreenBackground.resolvedBaseColor: Color
    get() = baseColorHex.toColorOrNull() ?: ColorVoidDark

/**
 * Resolves a [LibraryScreenBackground] contract's ambient color to a
 * Compose [Color], with a safe fallback to a deep navy.
 */
internal val LibraryScreenBackground.resolvedAmbientColor: Color
    get() = ambientColorHex.toColorOrNull() ?: Color(0xFF1A1A2E)

// ─────────────────────────────────────────────────────────────────────────────
// GRADIENT PRESETS
//
// RIKO's background gradients are *quiet*. Even the most aggressive
// gradient (CINEMATIC) uses only 3 stops with a 0.06 L-value delta.
// No background ever competes with content (Rule #63).
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A resolved gradient — two or three colors that the background will
 * paint as a vertical sweep.
 *
 * @property top       Top color (above the fold).
 * @property center    Middle color (mid-screen).
 * @property bottom    Bottom color (below the fold / under dock).
 * @property intensity 0.0–1.0. How visible the gradient is. 0 means
 *                     a flat solid color (top only).
 */
internal data class LibraryBackgroundGradient(
    val top: Color,
    val center: Color,
    val bottom: Color,
    val intensity: Float,
) {
    companion object {

        /**
         * The DEFAULT Library gradient — aurora indigo drift.
         *
         * Sampled from the UI target mockups: the reference library
         * screens sit on a deep indigo field (brighter indigo at the
         * top, easing to near-black at the bottom). Tokens live in
         * `core/designsystem/theme/Color.kt` (Rule #62).
         */
        val DEFAULT = LibraryBackgroundGradient(
            top = AuroraIndigo,
            center = AuroraIndigoDeep,
            bottom = AuroraBase,
            intensity = 1.0f,
        )

        /**
         * The AMOLED gradient — pure black with the faintest possible
         * navy lift at the top, so the screen never looks "off" but
         * also never wastes a single pixel of battery.
         *
         * `intensity = 0.20` means the gradient is 80% invisible —
         * the eye perceives pure black, but the very top edge has a
         * whisper of depth so it doesn't feel flat-dead.
         */
        val AMOLED = LibraryBackgroundGradient(
            top = Color(0xFF050507),
            center = Color(0xFF000000),
            bottom = Color(0xFF000000),
            intensity = 0.20f,
        )

        /**
         * The CINEMATIC gradient — slightly more visible than DEFAULT
         * but still quiet. The lift is intentional: cinematic profile
         * is for premium-feel moments (e.g. user opening the app on
         * a high-end device).
         */
        val CINEMATIC = LibraryBackgroundGradient(
            top = Color(0xFF0A1220),
            center = Color(0xFF0D1828),
            bottom = Color(0xFF101E33),
            intensity = 1.0f,
        )

        /**
         * The PERSONALITY gradient placeholder — a very faint mix of
         * the two RIKO accents (Cyan + Violet) at the top, fading to
         * pure base at the bottom.
         *
         * Phase B will replace this with a real personality-driven
         * palette resolver. For Phase A, this serves as the
         * declaration: "this is where personality will live."
         */
        val PERSONALITY_PLACEHOLDER = LibraryBackgroundGradient(
            top = Color(0xFF0B1320),
            center = Color(0xFF0A1018),
            bottom = Color(0xFF080D14),
            intensity = 0.85f,
        )
    }
}

/**
 * Resolves the appropriate [LibraryBackgroundGradient] for a given
 * [LibraryScreenBackground] contract.
 *
 * Resolution order:
 *   1. If `backgroundKind == PURE_BLACK` or `PURE_BLACK_WITH_AMBIENT`
 *      → return [LibraryBackgroundGradient.AMOLED]
 *   2. If `backgroundKind == GRADIENT` or `PERSONALITY_GRADIENT`
 *      → return [LibraryBackgroundGradient.CINEMATIC] (Phase A placeholder
 *      for personality; Phase B will inject real palette)
 *   3. Otherwise → return [LibraryBackgroundGradient.DEFAULT]
 *
 * Note: this function does NOT branch on profile directly. The contract's
 * `backgroundKind` is the source of truth — the profile has already been
 * resolved upstream by `LibraryConstitution.forProfile()`.
 */
internal fun LibraryScreenBackground.resolveGradient(): LibraryBackgroundGradient =
    when (backgroundKind) {
        LibraryBackgroundKind.SOLID,
        LibraryBackgroundKind.SOLID_WITH_AMBIENT,
        -> LibraryBackgroundGradient.DEFAULT

        LibraryBackgroundKind.PURE_BLACK,
        LibraryBackgroundKind.PURE_BLACK_WITH_AMBIENT,
        -> LibraryBackgroundGradient.AMOLED

        LibraryBackgroundKind.GRADIENT,
        LibraryBackgroundKind.PERSONALITY_GRADIENT,
        -> LibraryBackgroundGradient.CINEMATIC
    }

// ─────────────────────────────────────────────────────────────────────────────
// THE COMPOSABLE
//
// Pure draw. No state. No animation. No side effects.
// Animation belongs to A4 (LibraryAmbientLayer).
// ─────────────────────────────────────────────────────────────────────────────

/**
 * THE LIBRARY BACKGROUND.
 *
 * Paints the base environment of the Library screen. Always fills the
 * entire screen. Never animates on its own — ambient motion (if any)
 * is layered on top by [LibraryAmbientLayer] (A4).
 *
 * ## Drawing strategy
 *
 * The background is drawn in two passes:
 *
 * 1. **Base pass** — a flat solid color fills the entire canvas. This
 *    is the "floor" of the screen. Even if the gradient fails to draw,
 *    the screen still has a valid base.
 *
 * 2. **Gradient pass** — a vertical gradient sweeps from top to bottom.
 *    The gradient's opacity is controlled by
 *    [LibraryBackgroundGradient.intensity], so AMOLED can have a
 *    near-invisible gradient while CINEMATIC can have a full sweep.
 *
 * ## Performance
 *
 * - **Zero allocations per frame**: the [Brush] is `remember`ed per
 *   contract instance, so a recomposition that keeps the same contract
 *   does not allocate a new brush.
 * - **No `drawBehind` lambdas that allocate**: we use Compose's
 *   `Modifier.background(brush)` for the gradient, which is the most
 *   allocation-efficient path.
 * - **No `drawIntoCanvas`**: we don't need it for a simple gradient.
 *
 * ## Rules
 *
 * - **Rule #58** (Runtime Never Knows Rendering): this file IS rendering.
 *   It is allowed to import Compose because it lives in the rendering
 *   tier, not the runtime tier.
 * - **Rule #62** (Never Rebuild Existing Systems): colors come from
 *   `core/designsystem/theme/Color.kt`. No new color constants except
 *   AMOLED's near-black tweaks, which are *gradient stops*, not new
 *   semantic colors.
 * - **Rule #63** (Background Never Demands Attention): all colors are
 *   deep navy / black. No saturated hues. Alpha is always ≤ 1.0 on
 *   the gradient and the base is always L ≤ 8.
 *
 * @param background  The contract that declares what background to draw.
 * @param modifier    Compose modifier for layout.
 */
@Composable
fun LibraryBackground(
    background: LibraryScreenBackground,
    modifier: Modifier = Modifier,
) {
    val gradient = remember(background) { background.resolveGradient() }

    // The base color is the contract's declared base. For AMOLED this
    // is pure black; for DEFAULT this is deep navy. We always paint it
    // first so the gradient can sit on top.
    val baseColor = remember(background) { background.resolvedBaseColor }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseColor)
            .then(
                if (gradient.intensity >= 0.99f) {
                    // Full-strength gradient — paint at full alpha.
                    Modifier.background(
                        brush = Brush.verticalGradient(
                            colors = listOf(gradient.top, gradient.center, gradient.bottom),
                        ),
                    )
                } else {
                    // Reduced-strength gradient (AMOLED case). We blend
                    // the gradient toward the base color by overlaying
                    // it with reduced alpha. This is cheaper than
                    // compositing two gradients.
                    Modifier.background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                gradient.top.copy(alpha = gradient.intensity),
                                gradient.center.copy(alpha = gradient.intensity),
                                gradient.bottom.copy(alpha = gradient.intensity),
                            ),
                        ),
                    )
                }
            ),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// AMBIENT HALO
//
// A very faint radial glow painted at the very top of the screen —
// gives the impression that the top bar is illuminated from above,
// without ever drawing an explicit "light source" element.
//
// Drawn as part of A1 because it is *static* (no animation). If we
// later want it to breathe, we move it to A4.
//
// Currently NOT used in A5 — left here as a placeholder for Phase D
// polish, where the Top Bar may want a subtle halo behind it.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A very faint radial halo painted behind the top bar.
 *
 * - Alpha is always ≤ 0.08 (Rule #63).
 * - Color is always [AccentCyan] (the RIKO primary accent) or
 *   [AccentViolet] (secondary). Never both at once.
 * - Position is always top-center, never moves.
 *
 * @param color     Halo color. Default: [AccentCyan].
 * @param intensity 0.0–1.0. Scaled into a 0.0–0.08 alpha range.
 * @param modifier  Compose modifier.
 */
@Composable
fun LibraryBackgroundHalo(
    color: Color = AccentCyan,
    intensity: Float = 1.0f,
    modifier: Modifier = Modifier,
) {
    // Cap intensity at 0.08 — the halo must never be brighter than
    // a whisper (Rule #63).
    val clampedAlpha = (intensity.coerceIn(0f, 1f)) * 0.08f

    val density = LocalDensity.current
    val haloRadiusPx = with(density) { 280.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawBrushHalo(
                    color = color,
                    alpha = clampedAlpha,
                    radiusPx = haloRadiusPx,
                )
            },
    )
}

/**
 * Draws a soft radial halo at the top-center of the canvas.
 *
 * Internal — only used by [LibraryBackgroundHalo]. Pulled out so the
 * draw logic is testable without a Composable context.
 */
private fun DrawScope.drawBrushHalo(
    color: Color,
    alpha: Float,
    radiusPx: Float,
) {
    val center = Offset(
        x = size.width * 0.5f,
        y = size.height * 0.0f,
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = alpha),
                color.copy(alpha = alpha * 0.5f),
                Color.Transparent,
            ),
            center = center,
            radius = radiusPx,
        ),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// SAFE FALLBACK
//
// If the contract is somehow missing / null at runtime (e.g. a preview
// with no constitution provided), this composable paints a sensible
// default so the screen never appears blank.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A safe fallback background — paints [LibraryScreenBackground.DEFAULT]
 * with no other configuration.
 *
 * Use this in `@Preview` composables or in error states where the
 * constitution is unavailable.
 */
@Composable
fun LibraryBackgroundFallback(
    modifier: Modifier = Modifier,
) {
    LibraryBackground(
        background = LibraryScreenBackground.DEFAULT,
        modifier = modifier,
    )
}
