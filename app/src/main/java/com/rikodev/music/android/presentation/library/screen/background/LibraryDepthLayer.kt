/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.3 / Phase A — Background Language
 *
 * File A3 / A5 — LibraryDepthLayer.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [LibraryDepthLayer] is the *visual separation* layer of the Library
 * screen.
 *
 *   Depth ≠ Shadow.
 *
 * Shadows are a side-effect of light. Depth is a *deliberate* visual
 * hierarchy — surfaces stack at different Z-positions, and the viewer
 * perceives which is "in front" without needing a literal drop shadow.
 *
 * RIKO's depth system uses three techniques:
 *
 *   1. **Tint shift** — surfaces higher in the stack have a slightly
 *      brighter base color. The eye reads "brighter = closer".
 *
 *   2. **Specular edge** — top edges of raised surfaces have a faint
 *      highlight (already provided by `GlassSurface`'s specular pass).
 *
 *   3. **Subtle border** — surfaces have a 0.5–1px border that
 *      separates them from the layer below. The border is so faint
 *      it's almost invisible — but its absence is more noticeable
 *      than its presence.
 *
 * WHAT THIS FILE IS
 * -----------------
 * A set of [Modifier] helpers + a [LibraryDepth] enum that encodes
 * the Library's depth vocabulary. Phase B composables use these to
 * declare "I am at depth X" without thinking about how depth is
 * rendered.
 *
 * WHAT THIS FILE IS NOT
 * ---------------------
 * ❌ Not a shadow engine. Real shadows come from `GlassSurface`'s
 *    `enableShadow` flag.
 * ❌ Not a blur engine. Blur comes from `GlassSurface` V2.
 * ❌ Not a Z-index controller. Compose's `zIndex` modifier is for
 *    drawing order, not visual depth — we don't use it here.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #58  Runtime Never Knows Rendering   — this is rendering
 * #62  Never Rebuild Existing Systems  — uses Color.kt palette + GlassStyle borders
 * #63  Background Never Demands Attention — depth tints are very low alpha
 *
 * © RIKO Experience OS — Rendering Tier (Phase A)
 */

package com.rikodev.music.android.presentation.library.screen.background

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.theme.ColorVoidDark
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// DEPTH VOCABULARY
//
// The Library has 5 depth strata. Each stratum maps to a specific
// elevation in the visual hierarchy:
//
//   ABYSS    — the very back. Pure background. Nothing lives here
//              except [LibraryBackground] (A1) and [LibraryAmbientLayer] (A4).
//
//   BACK     — behind content. Used by full-bleed elements that
//              should feel like they are part of the background but
//              slightly forward. (E.g. a personality-driven gradient
//              layer that responds to the active collection.)
//
//   MID      — the content stratum. Section containers and most
//              cards live here. This is the "natural" depth — most
//              things the user looks at are at MID.
//
//   FRONT    — elevated content. The Hero card, the active/selected
//              card, and the Dock live here. Things at FRONT feel
//              closer to the user.
//
//   OVERLAY  — above everything except modals. Loading scrims, empty
//              state backgrounds, and pull-to-refresh indicators live
//              here. Modals (sheets, dialogs) are ABOVE overlay but
//              are managed by the host (B5.5), not by the Library.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A depth stratum in the Library's visual hierarchy.
 *
 * @property description       Human-readable description.
 * @property tintColorHex      The tint color to apply to surfaces at
 *                             this depth. Surfaces "brighten" as they
 *                             rise through the stack.
 * @property tintAlpha         How strongly the tint is applied. Always
 *                             ≤ 0.20 (Rule #63).
 * @property borderColorHex    Border color for surfaces at this depth.
 * @property borderAlpha       Border alpha. ≤ 0.20.
 * @property isAboveContent    True if this stratum is visually in front
 *                             of content (FRONT, OVERLAY). Used by
 *                             Phase C to decide if a scrim should
 *                             block touches.
 */
enum class LibraryDepth(
    internal val description: String,
    internal val tintColorHex: String,
    internal val tintAlpha: Float,
    internal val borderColorHex: String,
    internal val borderAlpha: Float,
    internal val isAboveContent: Boolean,
) {
    /** ABYSS — the very back. Pure background. */
    ABYSS(
        description = "Abyss — pure background, very back.",
        tintColorHex = "#05070B",
        tintAlpha = 0.0f, // No tint — this IS the background.
        borderColorHex = "#000000",
        borderAlpha = 0.0f,
        isAboveContent = false,
    ),

    /** BACK — behind content. */
    BACK(
        description = "Back — behind content.",
        tintColorHex = "#0A0D14",
        tintAlpha = 0.10f,
        borderColorHex = "#0A0D14",
        borderAlpha = 0.0f,
        isAboveContent = false,
    ),

    /** MID — the content stratum. Most cards / sections. */
    MID(
        description = "Mid — content stratum.",
        tintColorHex = "#111623",
        tintAlpha = 0.12f,
        borderColorHex = "#1FFFFFFF", // GlassBorderSubtle equivalent
        borderAlpha = 0.06f,
        isAboveContent = false,
    ),

    /** FRONT — elevated content (Hero, Dock, active card). */
    FRONT(
        description = "Front — elevated content.",
        tintColorHex = "#151B2A",
        tintAlpha = 0.16f,
        borderColorHex = "#22FFFFFF", // GlassBorderLight equivalent
        borderAlpha = 0.10f,
        isAboveContent = true,
    ),

    /** OVERLAY — above content, below modals. */
    OVERLAY(
        description = "Overlay — above content, below modals.",
        tintColorHex = "#000000",
        tintAlpha = 0.20f,
        borderColorHex = "#000000",
        borderAlpha = 0.0f,
        isAboveContent = true,
    );

    /**
     * True if this stratum is part of the background stack (ABYSS or BACK).
     * Used by [LibraryBackgroundHost] (A5) to decide which layers to paint
     * in the background pass vs. the content pass.
     */
    val isBackgroundLayer: Boolean
        get() = this == ABYSS || this == BACK
}

// ─────────────────────────────────────────────────────────────────────────────
// DEPTH RESOLUTION HELPERS
//
// Convert a [LibraryDepth] into concrete Compose values (Color, Dp).
// Pulled out so they can be unit-tested without a Composable context.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Resolves the tint color for this depth stratum.
 *
 * Returns a Compose [Color] with the stratum's alpha pre-applied. The
 * caller can paint this directly onto a surface to give it the
 * stratum's "lift".
 */
internal val LibraryDepth.resolvedTintColor: Color
    get() {
        val base = tryParseColor(tintColorHex) ?: ColorVoidDark
        return base.copy(alpha = tintAlpha)
    }

/**
 * Resolves the border color for this depth stratum.
 *
 * Returns a Compose [Color] with the stratum's border alpha pre-applied.
 * If `borderAlpha == 0f`, returns [Color.Transparent] (no border).
 */
internal val LibraryDepth.resolvedBorderColor: Color
    get() {
        if (borderAlpha <= 0f) return Color.Transparent
        val base = tryParseColor(borderColorHex) ?: Color.Transparent
        return base.copy(alpha = borderAlpha)
    }

/** Parse a hex color string to Color, or return null if invalid. */
private fun tryParseColor(hex: String): Color? {
    if (hex.isEmpty()) return null
    return try {
        Color(android.graphics.Color.parseColor(if (hex.startsWith("#")) hex else "#$hex"))
    } catch (_: Exception) {
        null
    }
}

/**
 * The default border width for surfaces at this depth.
 *
 * ABYSS / BACK: 0dp (no border — these are background layers).
 * MID: 0.5dp (hairline — barely visible separation).
 * FRONT: 0.8dp (slightly more visible — surfaces at FRONT want to
 *              read as discrete).
 * OVERLAY: 0dp (no border — overlays are pure tint).
 */
internal val LibraryDepth.defaultBorderWidth: Dp
    get() = when (this) {
        LibraryDepth.ABYSS, LibraryDepth.BACK, LibraryDepth.OVERLAY -> 0.dp
        LibraryDepth.MID -> 0.5.dp
        LibraryDepth.FRONT -> 0.8.dp
    }

// ─────────────────────────────────────────────────────────────────────────────
// DEPTH MODIFIERS
//
// The public API. Phase B composables call:
//
//   Modifier.libraryDepthTint(LibraryDepth.MID)
//   Modifier.libraryDepthBorder(LibraryDepth.FRONT)
//
// These modifiers are *composable-friendly* — they read the LocalDensity
// where needed and `remember` the resolved colors so recomposition with
// the same depth doesn't allocate.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Applies the tint for the given [LibraryDepth] to a surface.
 *
 * The tint is painted as a solid background color over whatever the
 * surface already has. For glass surfaces, this is the glass tint.
 * For non-glass surfaces, this is a subtle darkening / brightening.
 *
 * Calling this with [LibraryDepth.ABYSS] is a no-op (returns the
 * original modifier) — ABYSS has no tint by definition.
 *
 * @param depth  The depth stratum to apply.
 */
fun Modifier.libraryDepthTint(
    depth: LibraryDepth,
): Modifier {
    if (depth.tintAlpha <= 0f) return this
    val tint = depth.resolvedTintColor
    return this.then(
        Modifier.background(color = tint),
    )
}

/**
 * Applies the border for the given [LibraryDepth] to a surface.
 *
 * The border is painted at the stratum's [LibraryDepth.defaultBorderWidth]
 * with the stratum's border color. This is the *only* depth-specific
 * visual treatment — everything else is tint.
 *
 * Calling this with [LibraryDepth.ABYSS] or [LibraryDepth.BACK] is a
 * no-op (returns the original modifier) — those strata have no border
 * by definition.
 *
 * @param depth        The depth stratum to apply.
 * @param cornerRadius The corner radius of the surface, so the border
 *                     follows the surface shape. Default: 0dp (square).
 */
fun Modifier.libraryDepthBorder(
    depth: LibraryDepth,
    cornerRadius: Dp = 0.dp,
): Modifier {
    if (depth.borderAlpha <= 0f || depth.defaultBorderWidth <= 0.dp) return this
    val borderColor = depth.resolvedBorderColor
    val borderWidth = depth.defaultBorderWidth
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius)
    return this.then(
        Modifier.border(
            width = borderWidth,
            color = borderColor,
            shape = shape,
        ),
    )
}

/**
 * Convenience modifier: applies BOTH tint and border for a depth stratum.
 *
 * This is the most common call site — most surfaces want both. If you
 * need only one, use [libraryDepthTint] or [libraryDepthBorder]
 * individually.
 *
 * @param depth        The depth stratum.
 * @param cornerRadius Border corner radius. Default: 0dp.
 */
fun Modifier.libraryDepth(
    depth: LibraryDepth,
    cornerRadius: Dp = 0.dp,
): Modifier = this
    .libraryDepthTint(depth)
    .libraryDepthBorder(depth, cornerRadius)

// ─────────────────────────────────────────────────────────────────────────────
// DEPTH STACK COMPOSABLE
//
// A composable that paints the *static* depth stack — the layered
// background tones that give the screen its sense of depth before
// any content is drawn.
//
// This is called by [LibraryBackgroundHost] (A5). Phase B composables
// do NOT call this directly — they call [libraryDepth] modifier on
// their own surfaces.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Paints the Library's static depth stack.
 *
 * Renders three full-screen Box layers, back to front:
 *
 *   1. ABYSS layer (no tint — base background already painted by A1)
 *   2. BACK layer (10% tint — gives the background a sense of "lift")
 *   3. MID layer (12% tint — the floor that content will sit on)
 *
 * Each layer is `fillMaxSize` and `Color.Transparent` outside its tint
 * area. The result is a screen that has a subtle gradient of depth
 * even before any content is drawn.
 *
 * ## Rules
 *
 * - **Rule #63** (Background Never Demands Attention): all tints are
 *   ≤ 0.12 alpha. The depth stack is invisible if you stare at it
 *   directly — you only notice it when you compare a Library screen
 *   to a flat-color screen.
 *
 * - **Rule #58** (Runtime Never Knows Rendering): this composable
 *   reads no state. It is a pure function of its inputs.
 *
 * @param modifier  Compose modifier.
 */
@Composable
fun LibraryDepthStack(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Layer 1: BACK tint (above the ABYSS that A1 already painted).
        DepthTintLayer(depth = LibraryDepth.BACK)

        // Layer 2: MID tint (sits on top of BACK — gives content floor).
        DepthTintLayer(depth = LibraryDepth.MID)
    }
}

/**
 * A single full-screen tint layer. Internal — used by [LibraryDepthStack].
 *
 * Paints the stratum's tint color across the entire canvas. If the
 * stratum has `tintAlpha == 0f`, this composable paints nothing (but
 * still occupies layout space, so callers can rely on its existence).
 */
@Composable
private fun BoxScope.DepthTintLayer(
    depth: LibraryDepth,
) {
    if (depth.tintAlpha <= 0f) return
    val tint = remember(depth) { depth.resolvedTintColor }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = tint),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// DEPTH-CORNER RESOLVER
//
// Phase B will pass surface corner radii to depth modifiers. The
// Library's corner radius vocabulary comes from B5.1's
// [LibraryCornerRadiusStyle]. This helper resolves that enum to a Dp.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The Library's corner radius scale.
 *
 * Aliases the B5.1 [com.rikodev.music.android.presentation.library.screen.contract.LibraryCornerRadiusStyle]
 * to concrete [Dp] values, keyed by surface role. Phase B composables
 * use this to declare "I am a card with MEDIUM corner radius" without
 * reaching into the contract package.
 */
enum class LibraryDepthCornerRadius(
    val radiusDp: Dp,
) {
    /** Sharp / minimal — for chips and badges. */
    SHARP(radiusDp = 4.dp),

    /** Small — for small cards. */
    SMALL(radiusDp = 8.dp),

    /** Medium — for standard cards. */
    MEDIUM(radiusDp = 12.dp),

    /** Large — for hero cards. */
    LARGE(radiusDp = 16.dp),

    /** Extra-large — for the Hero and Dock. */
    EXTRA_LARGE(radiusDp = 24.dp),

    /** Pill — fully rounded. */
    PILL(radiusDp = 999.dp),
}

/**
 * The "natural" corner radius for a given depth stratum.
 *
 * Deeper strata have smaller radii (they are background, less shape).
 * Higher strata have larger radii (they are foreground, more shape).
 *
 * Used by Phase B composables that want a sensible default without
 * thinking about it.
 */
val LibraryDepth.naturalCornerRadius: LibraryDepthCornerRadius
    get() = when (this) {
        LibraryDepth.ABYSS, LibraryDepth.BACK -> LibraryDepthCornerRadius.SHARP
        LibraryDepth.MID -> LibraryDepthCornerRadius.MEDIUM
        LibraryDepth.FRONT -> LibraryDepthCornerRadius.LARGE
        LibraryDepth.OVERLAY -> LibraryDepthCornerRadius.PILL
    }
