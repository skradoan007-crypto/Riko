/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.1 — Library Screen Constitution
 *
 * File 14 / 15 — LibraryTokens.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Library Tokens are the *raw design tokens* of the Library screen —
 * spacing scale, radius scale, elevation scale, opacity scale.
 *
 * Tokens are the smallest unit of design. They carry no semantic
 * meaning — that comes from [LibraryVisualLanguage] (file 12) which
 * maps semantic roles to token values.
 *
 * WHY A SEPARATE FILE
 * -------------------
 * Centralizing tokens here means:
 *   1. Spacing is consistent across the entire screen.
 *   2. Changing a token (e.g. base spacing from 4dp to 8dp) updates
 *      every consumer automatically.
 *   3. The Inspector overlay can surface "this element uses spacing
 *      tokens.SPACING_16" — useful for debugging layout.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #44  Stable Key Never Changes    — token keys are stable strings
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Compose, no Android framework, no Coroutine, no Flow, no
 * ViewModel. Pure Kotlin data declaration.
 *
 * © RIKO Experience OS — Constitution Tier
 */

package com.rikodev.music.android.presentation.library.screen.contract

/**
 * Spacing tokens — the base spacing scale.
 *
 * All spacing in the Library should be one of these values (or a
 * simple multiple). Off-scale spacing creates visual jitter.
 */
enum class LibrarySpacingToken(
    internal val tokenName: String,
    internal val valueDp: Int,
) {
    /** 0dp — no spacing. */
    NONE("spacing.none", 0),

    /** 2dp — micro spacing (icon padding). */
    MICRO("spacing.micro", 2),

    /** 4dp — extra-small spacing (badge padding). */
    EXTRA_SMALL("spacing.extraSmall", 4),

    /** 8dp — small spacing (chip padding). */
    SMALL("spacing.small", 8),

    /** 12dp — medium-small spacing (card inner padding). */
    MEDIUM_SMALL("spacing.mediumSmall", 12),

    /** 16dp — medium spacing (standard horizontal padding). */
    MEDIUM("spacing.medium", 16),

    /** 20dp — medium-large spacing. */
    MEDIUM_LARGE("spacing.mediumLarge", 20),

    /** 24dp — large spacing (section gap). */
    LARGE("spacing.large", 24),

    /** 32dp — extra-large spacing (between sections). */
    EXTRA_LARGE("spacing.extraLarge", 32),

    /** 48dp — huge spacing (empty state padding). */
    HUGE("spacing.huge", 48),

    /** 64dp — massive spacing (hero top margin). */
    MASSIVE("spacing.massive", 64),
}

/**
 * Radius tokens — corner radius scale.
 */
enum class LibraryRadiusToken(
    internal val tokenName: String,
    internal val valueDp: Int,
) {
    /** 0dp — no radius (sharp). */
    NONE("radius.none", 0),

    /** 2dp — subtle radius (badges). */
    SUBTLE("radius.subtle", 2),

    /** 4dp — small radius (chips). */
    SMALL("radius.small", 4),

    /** 8dp — medium radius (small cards). */
    MEDIUM("radius.medium", 8),

    /** 12dp — large radius (standard cards). */
    LARGE("radius.large", 12),

    /** 16dp — extra-large radius (hero cards). */
    EXTRA_LARGE("radius.extraLarge", 16),

    /** 24dp — huge radius (hero cards, dock). */
    HUGE("radius.huge", 24),

    /** 32dp — massive radius (full-bleed cards). */
    MASSIVE("radius.massive", 32),

    /** -1 — fully circular. */
    CIRCLE("radius.circle", -1),
}

/**
 * Elevation tokens — z-depth scale.
 *
 * RIKO uses elevation sparingly — most surfaces are flat. Elevation
 * is reserved for floating elements (Dock, modals, FAB).
 */
enum class LibraryElevationToken(
    internal val tokenName: String,
    internal val valueDp: Float,
) {
    /** 0dp — flat. */
    FLAT("elevation.flat", 0f),

    /** 1dp — subtle raised. */
    SUBTLE("elevation.subtle", 1f),

    /** 2dp — small raised. */
    SMALL("elevation.small", 2f),

    /** 4dp — medium raised (cards on scroll). */
    MEDIUM("elevation.medium", 4f),

    /** 8dp — large raised (Dock). */
    LARGE("elevation.large", 8f),

    /** 12dp — extra-large raised (modals). */
    EXTRA_LARGE("elevation.extraLarge", 12f),

    /** 16dp — huge raised (FAB). */
    HUGE("elevation.huge", 16f),

    /** 24dp — massive raised (fullscreen modals). */
    MASSIVE("elevation.massive", 24f),
}

/**
 * Opacity tokens — transparency scale.
 */
enum class LibraryOpacityToken(
    internal val tokenName: String,
    internal val value: Float,
) {
    /** 0.0 — fully transparent. */
    TRANSPARENT("opacity.transparent", 0.0f),

    /** 0.1 — very low opacity. */
    VERY_LOW("opacity.veryLow", 0.1f),

    /** 0.2 — low opacity. */
    LOW("opacity.low", 0.2f),

    /** 0.3 — subtle opacity. */
    SUBTLE("opacity.subtle", 0.3f),

    /** 0.4 — medium-low opacity. */
    MEDIUM_LOW("opacity.mediumLow", 0.4f),

    /** 0.6 — medium opacity. */
    MEDIUM("opacity.medium", 0.6f),

    /** 0.7 — medium-high opacity. */
    MEDIUM_HIGH("opacity.mediumHigh", 0.7f),

    /** 0.8 — high opacity. */
    HIGH("opacity.high", 0.8f),

    /** 0.9 — very high opacity. */
    VERY_HIGH("opacity.veryHigh", 0.9f),

    /** 1.0 — fully opaque. */
    OPAQUE("opacity.opaque", 1.0f),
}

/**
 * Blur radius tokens — for glass effects.
 */
enum class LibraryBlurToken(
    internal val tokenName: String,
    internal val valueDp: Int,
) {
    /** 0dp — no blur. */
    NONE("blur.none", 0),

    /** 4dp — minimal blur. */
    MINIMAL("blur.minimal", 4),

    /** 8dp — light blur. */
    LIGHT("blur.light", 8),

    /** 16dp — medium blur. */
    MEDIUM("blur.medium", 16),

    /** 24dp — strong blur. */
    STRONG("blur.strong", 24),

    /** 32dp — heavy blur. */
    HEAVY("blur.heavy", 32),

    /** 48dp — maximum blur. */
    MAXIMUM("blur.maximum", 48),
}

/**
 * Stroke width tokens — for borders / dividers.
 */
enum class LibraryStrokeToken(
    internal val tokenName: String,
    internal val valueDp: Float,
) {
    /** 0dp — no stroke. */
    NONE("stroke.none", 0f),

    /** 0.5dp — hairline. */
    HAIRLINE("stroke.hairline", 0.5f),

    /** 1dp — thin. */
    THIN("stroke.thin", 1f),

    /** 2dp — medium. */
    MEDIUM("stroke.medium", 2f),

    /** 3dp — thick. */
    THICK("stroke.thick", 3f),

    /** 4dp — extra-thick. */
    EXTRA_THICK("stroke.extraThick", 4f),
}

/**
 * The Library Tokens collection — a single accessor for all token
 * groups. The renderer (B5.3) reads this; the Inspector overlay
 * surfaces it.
 *
 * @property spacing       Spacing tokens.
 * @property radius        Radius tokens.
 * @property elevation     Elevation tokens.
 * @property opacity       Opacity tokens.
 * @property blur          Blur tokens.
 * @property stroke        Stroke tokens.
 */
data class LibraryTokens(
    val spacing: LibraryTokenGroup<LibrarySpacingToken>,
    val radius: LibraryTokenGroup<LibraryRadiusToken>,
    val elevation: LibraryTokenGroup<LibraryElevationToken>,
    val opacity: LibraryTokenGroup<LibraryOpacityToken>,
    val blur: LibraryTokenGroup<LibraryBlurToken>,
    val stroke: LibraryTokenGroup<LibraryStrokeToken>,
) {
    companion object {
        val DEFAULT = LibraryTokens(
            spacing = LibraryTokenGroup(LibrarySpacingToken.entries),
            radius = LibraryTokenGroup(LibraryRadiusToken.entries),
            elevation = LibraryTokenGroup(LibraryElevationToken.entries),
            opacity = LibraryTokenGroup(LibraryOpacityToken.entries),
            blur = LibraryTokenGroup(LibraryBlurToken.entries),
            stroke = LibraryTokenGroup(LibraryStrokeToken.entries),
        )
    }
}

/**
 * A group of tokens — provides lookup by name.
 */
data class LibraryTokenGroup<T : Enum<T>>(
    val tokens: List<T>,
) {
    /** Look up a token by its name. */
    fun tokenFor(tokenName: String): T? =
        tokens.firstOrNull { token ->
            val field = token::class.java.getMethod("getTokenName").invoke(token) as? String
            field == tokenName
        }
}
