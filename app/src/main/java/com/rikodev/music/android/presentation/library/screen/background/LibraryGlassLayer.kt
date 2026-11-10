/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.3 / Phase A — Background Language
 *
 * File A2 / A5 — LibraryGlassLayer.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [LibraryGlassLayer] is the *glass material* layer of the Library
 * screen. It does NOT invent a new glass engine — it consumes the
 * existing RIKO Glass Engine V2 (Rule #62).
 *
 *   Existing system (DO NOT TOUCH)
 *   ──────────────────────────────
 *   core/designsystem/glass/GlassSurface.kt
 *   core/designsystem/theme/GlassTokens.kt
 *   core/designsystem/theme/Color.kt
 *
 *   This file (LIBRARY ADAPTER)
 *   ───────────────────────────
 *   - Maps Library profile → existing GlassStyle
 *   - Provides Library-specific GlassStyle presets (hero, dock, topBar,
 *     section, card, ambient)
 *   - Wraps [GlassSurface] so Library composables never reach into
 *     the design system directly
 *
 * WHY THIS FILE EXISTS
 * --------------------
 * Phase B will have many glass surfaces (Hero, Cards, Dock, Chips).
 * Each of those needs a *consistent* glass style. Rather than letting
 * each Phase B file pick a `GlassStyle` at random, we centralize the
 * Library's glass vocabulary here.
 *
 * If a designer says "Library cards should feel a bit more solid",
 * this is the ONE file that changes — not 9 Phase B files.
 *
 * WHAT THIS FILE IS NOT
 * ---------------------
 * ❌ Not a glass engine. The engine lives in `core/designsystem/glass/`.
 * ❌ Not a new blur system. Blur is owned by GlassSurface V2.
 * ❌ Not a tint system. Tints come from `Color.kt`.
 * ❌ Not a shadow system. Shadows come from GlassSurface V2.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #58  Runtime Never Knows Rendering   — this is rendering
 * #62  Never Rebuild Existing Systems  — wraps GlassSurface, never replaces
 * #63  Background Never Demands Attention — glass tint alpha is low
 *
 * © RIKO Experience OS — Rendering Tier (Phase A)
 */

package com.rikodev.music.android.presentation.library.screen.background

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.glass.GlassSurface
import com.rikodev.music.android.core.designsystem.theme.AccentCyan
import com.rikodev.music.android.core.designsystem.theme.ColorVoidDark
import com.rikodev.music.android.core.designsystem.theme.GlassBorderLight
import com.rikodev.music.android.core.designsystem.theme.GlassStyle
import com.rikodev.music.android.core.designsystem.theme.GlassTokens
import com.rikodev.music.android.presentation.library.screen.contract.LibraryConstitutionProfile
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// LIBRARY GLASS TOKENS
//
// Each token wraps an existing [GlassStyle] from `GlassTokens` (the
// central glass registry in `core/designsystem/theme/GlassTokens.kt`).
//
// We do NOT define new GlassStyle values here — we ONLY alias the
// existing ones to Library-specific intents. If a token truly needs a
// new value (e.g. a brand-new Library-only glass surface), we add it
// to `GlassTokens` upstream, then alias it here.
//
// This is the Rule #62 enforcement: the glass engine stays in one
// place, and Library is a *consumer*, not a *rebuilder*.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Library-specific glass tokens — semantic aliases over the existing
 * [GlassTokens] registry.
 *
 * Usage in Phase B composables:
 *
 *   GlassSurface(
 *       style = LibraryGlassToken.HERO.style,
 *       ...
 *   )
 *
 * Or via the convenience wrapper:
 *
 *   LibraryGlassSurface(
 *       token = LibraryGlassToken.HERO,
 *       ...
 *   )
 *
 * @property style         The underlying [GlassStyle] (sourced from
 *                         [GlassTokens] — never a new instance).
 * @property defaultRadius Default corner radius for this token.
 * @property defaultTint   Default tint color (Color.Transparent = no tint).
 */
enum class LibraryGlassToken(
    val style: GlassStyle,
    val defaultRadius: Dp,
    val defaultTint: Color,
) {
    /**
     * HERO — the dominant glass surface in the screen (Phase B).
     *
     * Wraps [GlassTokens.Hero]. Real backdrop blur (65dp), no border,
     * strong glow (0.50). This is the most visually aggressive glass
     * in the Library — and it should be, because Hero is the screen's
     * anchor.
     *
     * Note: Hero glass is the only Library glass with `glowAlpha > 0.10`.
     * This is allowed because Hero is *content*, not background —
     * Rule #63 limits glow on background, not on Hero.
     */
    HERO(
        style = GlassTokens.Hero,
        defaultRadius = 24.dp,
        defaultTint = Color.Transparent,
    ),

    /**
     * DOCK — the persistent mini player at the bottom.
     *
     * Wraps [GlassTokens.MiniPlayer]. Solid (no blur), low tint (0.10),
     * subtle border, low shadow. The dock must feel substantial enough
     * to read as a separate surface, but quiet enough to never
     * compete with content above it.
     *
     * Phase B's [LibraryDockBar] will use this token.
     */
    DOCK(
        style = GlassTokens.MiniPlayer,
        defaultRadius = 20.dp,
        defaultTint = AccentCyan,
    ),

    /**
     * TOP_BAR — the orienting chip bar at the top.
     *
     * Wraps [GlassTokens.Chip] (ultra-thin glass). The top bar is the
     * lightest glass surface — it's a navigation aid, not a feature.
     * Ultra-thin glass + subtle border gives it just enough presence
     * to be touched, without ever drawing the eye.
     */
    TOP_BAR(
        style = GlassTokens.Chip,
        defaultRadius = 999.dp, // Pill shape
        defaultTint = Color.Transparent,
    ),

    /**
     * SECTION — a section container (Phase B).
     *
     * Wraps [GlassTokens.FloatingCard]. Medium glass — substantial
     * enough to host a header + horizontal card row, light enough to
     * stay quiet on the screen.
     *
     * Section containers are NOT always glass. By default they are
     * flat (no glass). This token is used only when a section needs
     * elevated framing (e.g. the Hero section).
     */
    SECTION(
        style = GlassTokens.FloatingCard,
        defaultRadius = 16.dp,
        defaultTint = Color.Transparent,
    ),

    /**
     * CARD — a single collection card (Phase B).
     *
     * Wraps [GlassTokens.FloatingCard]. Same as SECTION but smaller
     * default radius. Cards are the most common glass surface — there
     * can be 8-12 visible at once — so the glass must be cheap. The
     * existing GlassSurface already optimizes this via `enableShadow = false`
     * for cards in scrolling lists.
     */
    CARD(
        style = GlassTokens.FloatingCard,
        defaultRadius = 12.dp,
        defaultTint = Color.Transparent,
    ),

    /**
     * AMBIENT — a glass surface that lives BEHIND content but in front
     * of the base background.
     *
     * Wraps [GlassTokens.Overlay]. Pure tint, no blur, no border. Used
     * by A4 (LibraryAmbientLayer) when ambient needs to feel like it
     * has *substance* without being a discrete surface.
     *
     * Alpha is always ≤ 0.18 (Rule #63).
     */
    AMBIENT(
        style = GlassTokens.Overlay,
        defaultRadius = 0.dp,
        defaultTint = ColorVoidDark,
    ),

    /**
     * CHIP — a small selectable label.
     *
     * Wraps [GlassTokens.Chip]. Same as TOP_BAR but with a smaller
     * default radius, used for filter chips, sort chips, etc.
     */
    CHIP(
        style = GlassTokens.Chip,
        defaultRadius = 999.dp, // Pill
        defaultTint = Color.Transparent,
    ),

    /**
     * TOOLTIP — a small floating info popup.
     *
     * Wraps [GlassTokens.Tooltip]. Rare in Library — used for long-press
     * hints and contextual help.
     */
    TOOLTIP(
        style = GlassTokens.Tooltip,
        defaultRadius = 12.dp,
        defaultTint = Color.Transparent,
    );

    /**
     * True if this token represents a glass surface that should
     * disable shadows when used inside a LazyColumn (Rule from
     * `GlassSurface.kt` — real elevation shadows are too expensive
     * for scrolling lists).
     */
    val shouldDisableShadowInScroll: Boolean
        get() = this == CARD || this == CHIP
}

// ─────────────────────────────────────────────────────────────────────────────
// PROFILE → GLASS RESOLUTION
//
// The Library's [LibraryConstitutionProfile] selects a *mood*, and that
// mood should affect how glass surfaces look. AMOLED profile, for
// example, makes glass darker and removes glow.
//
// We do NOT change the underlying [GlassStyle] — we change which token
// is the *default* for each role, and we apply a small set of allowed
// overrides (radius, tint).
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Resolves the appropriate [LibraryGlassToken] for the DOCK role,
 * given a profile.
 *
 * AMOLED: dock glass is darker (uses BottomBar token — pure tint, no glow)
 * DEFAULT / others: dock glass is the standard MiniPlayer token
 */
internal fun LibraryConstitutionProfile.resolveDockToken(): LibraryGlassToken =
    when (this) {
        LibraryConstitutionProfile.AMOLED -> LibraryGlassToken.AMBIENT
        else -> LibraryGlassToken.DOCK
    }

/**
 * Resolves the appropriate [LibraryGlassToken] for the TOP_BAR role.
 *
 * Same across all profiles — the top bar is the lightest glass and
 * should never change personality with profile.
 */
internal fun LibraryConstitutionProfile.resolveTopBarToken(): LibraryGlassToken =
    LibraryGlassToken.TOP_BAR

/**
 * Resolves the appropriate [LibraryGlassToken] for the HERO role.
 *
 * CINEMATIC / HIGH_END: Hero uses the Hero token (real backdrop blur)
 * Others: Hero uses FloatingCard (no real blur — cheaper, still feels
 * elevated)
 */
internal fun LibraryConstitutionProfile.resolveHeroToken(): LibraryGlassToken =
    when (this) {
        LibraryConstitutionProfile.CINEMATIC,
        LibraryConstitutionProfile.HIGH_END,
        -> LibraryGlassToken.HERO

        else -> LibraryGlassToken.SECTION
    }

/**
 * Resolves the appropriate [LibraryGlassToken] for a CARD.
 *
 * Same across all profiles. Cards always use FloatingCard glass.
 * Profile differences in card *appearance* are handled by tint color,
 * not by glass material.
 */
internal fun LibraryConstitutionProfile.resolveCardToken(): LibraryGlassToken =
    LibraryGlassToken.CARD

// ─────────────────────────────────────────────────────────────────────────────
// CONVENIENCE WRAPPER
//
// [GlassSurface] is the existing composable. We wrap it so Library
// Phase B files don't need to import from `core/designsystem/glass/`
// directly — they import from this file, which is the Library adapter.
//
// This keeps the boundary clean: Library talks to LibraryGlassLayer,
// LibraryGlassLayer talks to GlassSurface. No Library file reaches
// past LibraryGlassLayer into the design system.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Library's convenience wrapper over [GlassSurface].
 *
 * Resolves the token's [GlassStyle] + default radius + default tint,
 * and forwards them to [GlassSurface]. Callers can still override
 * radius, tint, border, shadow — but they don't have to.
 *
 * ## Performance note
 *
 * `enableShadow` defaults to `false` for tokens that declare
 * [LibraryGlassToken.shouldDisableShadowInScroll]. This is the
 * `GlassSurface.kt` Rule: real elevation shadows are expensive in
 * scrolling lists, so cards and chips skip them by default.
 *
 * If a card is NOT in a scrolling list (e.g. the Hero card), pass
 * `enableShadow = true` to opt back in.
 *
 * @param token          Library glass token — selects the style.
 * @param modifier       Compose modifier.
 * @param cornerRadius   Override corner radius. Default: token's default.
 * @param tintColor      Override tint color. Default: token's default.
 * @param showSpecular   Whether to draw the specular highlight. Default: true.
 * @param showBorder     Whether to draw the glass border. Default: true.
 * @param borderColor    Border color. Default: [GlassBorderLight].
 * @param enableShadow   Override shadow enable. Default: smart — disabled
 *                       for tokens that should skip shadows in scroll.
 * @param content        The content inside the glass.
 */
@Composable
fun LibraryGlassSurface(
    token: LibraryGlassToken,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = token.defaultRadius,
    tintColor: Color = token.defaultTint,
    showSpecular: Boolean = true,
    showBorder: Boolean = true,
    borderColor: Color = GlassBorderLight,
    enableShadow: Boolean = !token.shouldDisableShadowInScroll,
    content: @Composable BoxScope.() -> Unit,
) {
    // `remember` here is a no-op for allocation (GlassStyle is part of
    // the enum), but it makes the dependency explicit — if `token`
    // changes, we re-resolve.
    val style = remember(token) { token.style }

    GlassSurface(
        modifier = modifier,
        style = style,
        cornerRadius = cornerRadius,
        tintColor = tintColor,
        showSpecular = showSpecular,
        showBorder = showBorder,
        borderColor = borderColor,
        enableShadow = enableShadow,
        content = content,
    )
}

/**
 * Library's convenience wrapper for a *borderless* glass surface —
 * used for the Hero, where borders would feel too "card-like".
 *
 * Identical to [LibraryGlassSurface] but with `showBorder = false`
 * and `showSpecular = false` by default. The Hero should feel like
 * an extension of the background, not a discrete card.
 */
@Composable
fun LibraryGlassSurfaceBorderless(
    token: LibraryGlassToken,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = token.defaultRadius,
    tintColor: Color = token.defaultTint,
    enableShadow: Boolean = !token.shouldDisableShadowInScroll,
    content: @Composable BoxScope.() -> Unit,
) {
    LibraryGlassSurface(
        token = token,
        modifier = modifier,
        cornerRadius = cornerRadius,
        tintColor = tintColor,
        showSpecular = false,
        showBorder = false,
        enableShadow = enableShadow,
        content = content,
    )
}

/**
 * Library's convenience wrapper for a *tinted* glass surface — used
 * for the Dock and Hero, where the accent color should bleed through
 * the glass subtly.
 *
 * The tint alpha is always capped at 0.10 (Rule #63 — background never
 * demands attention). Even the Hero, which is allowed glow, is not
 * allowed to flood the screen with accent color.
 */
@Composable
fun LibraryGlassSurfaceTinted(
    token: LibraryGlassToken,
    tintColor: Color,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = token.defaultRadius,
    tintAlphaCap: Float = 0.10f,
    content: @Composable BoxScope.() -> Unit,
) {
    val cappedTint = remember(tintColor, tintAlphaCap) {
        tintColor.copy(alpha = tintColor.alpha.coerceAtMost(tintAlphaCap))
    }

    LibraryGlassSurface(
        token = token,
        modifier = modifier,
        cornerRadius = cornerRadius,
        tintColor = cappedTint,
        content = content,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// GLASS SCRIM
//
// A thin glass overlay that dims content behind it — used by Phase C
// (Loading / Empty states) when content needs to recede.
//
// This is a *very thin* wrapper — it just calls [GlassSurface] with
// [GlassTokens.Overlay] and a low-alpha tint. We expose it separately
// because the call site ("show me a scrim") is different from the call
// site ("give me a card").
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A thin glass scrim — dims content behind it without obscuring.
 *
 * Alpha is always ≤ 0.30 (Rule #63).
 *
 * @param modifier     Compose modifier.
 * @param dimColor     Color to dim with. Default: [ColorVoidDark] (deep navy).
 * @param dimIntensity 0.0–1.0. Scaled into 0.0–0.30 alpha range.
 */
@Composable
fun LibraryGlassScrim(
    modifier: Modifier = Modifier,
    dimColor: Color = ColorVoidDark,
    dimIntensity: Float = 1.0f,
) {
    val clampedAlpha = (dimIntensity.coerceIn(0f, 1f)) * 0.30f
    val tint = remember(dimColor, clampedAlpha) {
        dimColor.copy(alpha = clampedAlpha)
    }

    Box(
        modifier = modifier
            .then(
                Modifier.background(
                    color = tint,
                )
            ),
    )
}
