/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B4 — Library Rendering Runtime
 *
 * File 10 / 13 — RenderTheme.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [RenderTheme] is the *resolved* theme spec for a section / item.
 *
 * The contract layer declares personality + presentation tokens.
 * The runtime carries them through. The renderer (B4) *resolves*
 * them against the active theme + collection personality, producing
 * concrete color hex strings, typography scale hints, and elevation
 * values the UI can consume directly.
 *
 * WHY A SEPARATE FILE
 * -------------------
 * Theme resolution is non-trivial — it involves:
 *   • Looking up the collection's personality
 *   • Looking up the personality's preferred color tone
 *   • Resolving against the active surface / theme variant
 *   • Applying accessibility adjustments (contrast)
 *   • Applying motion-derived hierarchy weights
 *
 * Centralizing this in [RenderThemeResolver] means the UI never
 * has to interpret contract tokens. The UI just reads resolved
 * hex strings and typography keys.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #33  Collection Has Identity     — theme follows personality, not random
 * #37  Collection Has Personality Before Data — personality drives theme
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Android, no Compose, no Coroutine, no Flow. Pure Kotlin data.
 *
 * © RIKO Experience OS — Rendering Tier
 */

package com.rikodev.music.android.presentation.library.rendering.theme

/**
 * The resolved theme for a single render element.
 *
 * @property accentHex         Primary accent color — hex string like "#FF8A65".
 * @property accentSecondaryHex  Secondary accent, used for gradients / outlines.
 * @property surfaceHex        Background color for the section / card.
 * @property onSurfaceHex      Foreground (text) color on the surface.
 * @property onAccentHex       Foreground color on top of the accent.
 * @property typographyKey     Typography scale key (e.g. "headlineMedium").
 * @property cornerRadiusDp    Corner radius for cards in this section.
 * @property elevationDp       Card elevation in dp.
 * @property personalityKey    Stable key of the source personality.
 *                             For diagnostics only. Rule #53.
 */
data class RenderTheme(
    val accentHex: String,
    val accentSecondaryHex: String,
    val surfaceHex: String,
    val onSurfaceHex: String,
    val onAccentHex: String,
    val typographyKey: String,
    val cornerRadiusDp: Int,
    val elevationDp: Float,
    val personalityKey: String,
) {
    companion object {
        /** Neutral fallback theme — used when no personality is resolved. */
        val NEUTRAL = RenderTheme(
            accentHex = "#607D8B",
            accentSecondaryHex = "#455A64",
            surfaceHex = "#FFFFFF",
            onSurfaceHex = "#212121",
            onAccentHex = "#FFFFFF",
            typographyKey = "bodyMedium",
            cornerRadiusDp = 12,
            elevationDp = 0f,
            personalityKey = "neutral",
        )
    }
}

/**
 * Typography scale keys — the UI maps each to a concrete font + size.
 *
 * The renderer never specifies font families or sizes directly —
 * it specifies a *role*, and the UI applies the active typography
 * scale to that role. This decouples rendering from font selection.
 */
enum class RenderTypographyKey(
    internal val description: String,
    internal val relativeSize: Float,
) {
    /** Hero title — largest, used for hero sections. */
    HERO_TITLE("Hero title — largest.", 1.6f),

    /** Large title — section headers for hero sections. */
    LARGE_TITLE("Large title.", 1.4f),

    /** Medium title — section headers for standard sections. */
    MEDIUM_TITLE("Medium title.", 1.2f),

    /** Small title — section headers for compact sections. */
    SMALL_TITLE("Small title.", 1.0f),

    /** Body — primary text on cards. */
    BODY("Body text.", 0.95f),

    /** Body medium — slightly emphasized body. */
    BODY_MEDIUM("Body medium.", 0.95f),

    /** Caption — tertiary labels. */
    CAPTION("Caption.", 0.8f),

    /** Overline — badges and small labels. */
    OVERLINE("Overline.", 0.7f),

    /** Empty state title. */
    EMPTY_TITLE("Empty state title.", 1.3f),

    /** Empty state message. */
    EMPTY_MESSAGE("Empty state message.", 0.95f),
}

/**
 * Personality-keyed accent palette.
 *
 * Each personality has a preferred accent + secondary. The renderer
 * looks up the personality, then applies the active surface (light /
 * dark) adjustment to produce final hex values.
 *
 * The values below are the *baseline* (light-mode) hex strings. The
 * resolver applies a darkening / lightening pass for dark mode.
 */
enum class PersonalityPalette(
    internal val personalityKey: String,
    internal val accentHexLight: String,
    internal val accentSecondaryHexLight: String,
    internal val accentHexDark: String,
    internal val accentSecondaryHexDark: String,
) {
    QUIET_CURATOR(
        personalityKey = "quiet_curator",
        accentHexLight = "#5C6BC0",
        accentSecondaryHexLight = "#3949AB",
        accentHexDark = "#7986CB",
        accentSecondaryHexDark = "#5C6BC0",
    ),
    WARM_FRIEND(
        personalityKey = "warm_friend",
        accentHexLight = "#FF8A65",
        accentSecondaryHexLight = "#FF5722",
        accentHexDark = "#FFAB91",
        accentSecondaryHexDark = "#FF8A65",
    ),
    CONFIDENT_GUIDE(
        personalityKey = "confident_guide",
        accentHexLight = "#26A69A",
        accentSecondaryHexLight = "#00897B",
        accentHexDark = "#4DB6AC",
        accentSecondaryHexDark = "#26A69A",
    ),
    CURIOUS_EXPLORER(
        personalityKey = "curious_explorer",
        accentHexLight = "#AB47BC",
        accentSecondaryHexLight = "#8E24AA",
        accentHexDark = "#BA68C8",
        accentSecondaryHexDark = "#AB47BC",
    ),
    REFLECTIVE_LIBRARIAN(
        personalityKey = "reflective_librarian",
        accentHexLight = "#789262",
        accentSecondaryHexLight = "#558B2F",
        accentHexDark = "#9CCC65",
        accentSecondaryHexDark = "#7CB342",
    ),
    PLAYFUL_DISCOVERER(
        personalityKey = "playful_discoverer",
        accentHexLight = "#FFB74D",
        accentSecondaryHexLight = "#FF9800",
        accentHexDark = "#FFCC80",
        accentSecondaryHexDark = "#FFB74D",
    ),
    MINIMAL_ARCHITECT(
        personalityKey = "minimal_architect",
        accentHexLight = "#455A64",
        accentSecondaryHexLight = "#263238",
        accentHexDark = "#78909C",
        accentSecondaryHexDark = "#546E7A",
    ),
    TECHNICAL_ANALYST(
        personalityKey = "technical_analyst",
        accentHexLight = "#42A5F5",
        accentSecondaryHexLight = "#1E88E5",
        accentHexDark = "#64B5F6",
        accentSecondaryHexDark = "#42A5F5",
    ),
    NOSTALGIC_TIMEKEEPER(
        personalityKey = "nostalgic_timekeeper",
        accentHexLight = "#A1887F",
        accentSecondaryHexLight = "#795548",
        accentHexDark = "#BCAAA4",
        accentSecondaryHexDark = "#A1887F",
    );

    companion object {
        /** Look up a palette by personality key. Falls back to MINIMAL_ARCHITECT. */
        fun forPersonality(personalityKey: String): PersonalityPalette =
            entries.firstOrNull { it.personalityKey == personalityKey }
                ?: MINIMAL_ARCHITECT
    }
}

/**
 * Active surface variant — drives color resolution.
 */
enum class RenderSurfaceVariant(internal val description: String) {
    /** Light mode — bright surfaces, dark text. */
    LIGHT("Light mode."),

    /** Dark mode — dark surfaces, bright text. */
    DARK("Dark mode."),

    /** AMOLED — pure black surfaces for OLED screens. */
    AMOLED("AMOLED — pure black."),
}

/**
 * Resolves a [RenderTheme] from a personality key + surface variant.
 *
 * This is a *pure* function — no side effects. The renderer calls
 * it during snapshot production; the UI never calls it.
 *
 * Rule #53 — Diagnostics Never Affects Engine.
 * Theme resolution never mutates state.
 */
object RenderThemeResolver {

    /**
     * Resolve a theme for a section.
     *
     * @property personalityKey    Stable key of the section's personality.
     * @property surfaceVariant    Active surface variant.
     * @property hierarchyWeight   Hierarchy weight (0.0–1.0) from [RenderMotion].
     *                             Higher weight → stronger accent + higher elevation.
     * @property cornerRadiusOverride  Optional override for corner radius.
     * @property elevationOverride     Optional override for elevation.
     */
    fun resolve(
        personalityKey: String,
        surfaceVariant: RenderSurfaceVariant,
        hierarchyWeight: Float,
        cornerRadiusOverride: Int? = null,
        elevationOverride: Float? = null,
    ): RenderTheme {
        val palette = PersonalityPalette.forPersonality(personalityKey)
        val isDark = surfaceVariant != RenderSurfaceVariant.LIGHT

        val (accentHex, accentSecondaryHex) = if (isDark) {
            palette.accentHexDark to palette.accentSecondaryHexDark
        } else {
            palette.accentHexLight to palette.accentSecondaryHexLight
        }

        val (surfaceHex, onSurfaceHex) = when (surfaceVariant) {
            RenderSurfaceVariant.LIGHT -> "#FFFFFF" to "#212121"
            RenderSurfaceVariant.DARK -> "#121212" to "#ECECEC"
            RenderSurfaceVariant.AMOLED -> "#000000" to "#ECECEC"
        }

        val onAccentHex = "#FFFFFF"

        val baseRadius = 12
        val radiusScaled = (baseRadius + (hierarchyWeight * 4).toInt()).coerceIn(8, 20)
        val cornerRadius = cornerRadiusOverride ?: radiusScaled

        val baseElevation = 0f
        val elevationScaled = baseElevation + (hierarchyWeight * 6f)
        val elevation = elevationOverride ?: elevationScaled

        return RenderTheme(
            accentHex = accentHex,
            accentSecondaryHex = accentSecondaryHex,
            surfaceHex = surfaceHex,
            onSurfaceHex = onSurfaceHex,
            onAccentHex = onAccentHex,
            typographyKey = typographyKeyFor(hierarchyWeight),
            cornerRadiusDp = cornerRadius,
            elevationDp = elevation,
            personalityKey = personalityKey,
        )
    }

    private fun typographyKeyFor(hierarchyWeight: Float): String = when {
        hierarchyWeight >= 0.9f -> RenderTypographyKey.HERO_TITLE.name
        hierarchyWeight >= 0.7f -> RenderTypographyKey.LARGE_TITLE.name
        hierarchyWeight >= 0.5f -> RenderTypographyKey.MEDIUM_TITLE.name
        hierarchyWeight >= 0.3f -> RenderTypographyKey.SMALL_TITLE.name
        else -> RenderTypographyKey.BODY.name
    }.lowercase()
}
