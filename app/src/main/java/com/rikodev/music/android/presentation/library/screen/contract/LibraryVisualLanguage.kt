/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.1 — Library Screen Constitution
 *
 * File 12 / 15 — LibraryVisualLanguage.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * The Library Visual Language declares the *visual vocabulary* of
 * the Library screen — color roles, typography roles, iconography
 * roles. It is the *semantic* layer that sits between raw tokens
 * (file 14) and concrete rendering (B5.3).
 *
 * Why a separate layer? Because raw tokens (#FF8A65) carry no
 * meaning. Semantic roles ("accentSecondary") carry meaning. The
 * renderer reads semantic roles; the theme resolver (B4 file 10)
 * maps roles to concrete values.
 *
 * RIKO's visual philosophy: the visual language is *quiet*. The
 * Library is not a billboard — it is a gallery. Colors are muted,
 * typography is generous, icons are subtle. The *content* (album
 * art, song titles) is loud; the *chrome* is quiet.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #33  Collection Has Identity     — visual language serves identity
 * #37  Collection Has Personality Before Data — personality drives language
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
 * A semantic color role in the Library visual language.
 *
 * Each role has a name + a description of where it is used. The
 * theme resolver maps each role to a concrete hex value.
 */
enum class LibraryColorRole(
    internal val roleName: String,
    internal val description: String,
) {
    /** Primary accent — the most prominent color on screen. */
    ACCENT_PRIMARY(
        roleName = "accentPrimary",
        description = "Primary accent — most prominent color.",
    ),

    /** Secondary accent — supporting color. */
    ACCENT_SECONDARY(
        roleName = "accentSecondary",
        description = "Secondary accent — supporting color.",
    ),

    /** Surface background — the base color of the screen. */
    SURFACE_BASE(
        roleName = "surfaceBase",
        description = "Surface background — base color.",
    ),

    /** Surface raised — cards / elevated elements. */
    SURFACE_RAISED(
        roleName = "surfaceRaised",
        description = "Surface raised — cards / elevated elements.",
    ),

    /** Surface overlay — modals / sheets. */
    SURFACE_OVERLAY(
        roleName = "surfaceOverlay",
        description = "Surface overlay — modals / sheets.",
    ),

    /** On-surface primary — primary text color. */
    ON_SURFACE_PRIMARY(
        roleName = "onSurfacePrimary",
        description = "On-surface primary text.",
    ),

    /** On-surface secondary — secondary text color. */
    ON_SURFACE_SECONDARY(
        roleName = "onSurfaceSecondary",
        description = "On-surface secondary text.",
    ),

    /** On-surface tertiary — tertiary text color. */
    ON_SURFACE_TERTIARY(
        roleName = "onSurfaceTertiary",
        description = "On-surface tertiary text.",
    ),

    /** On-accent — text color on top of accent. */
    ON_ACCENT(
        roleName = "onAccent",
        description = "Text color on top of accent.",
    ),

    /** Outline — borders / dividers. */
    OUTLINE(
        roleName = "outline",
        description = "Outline — borders / dividers.",
    ),

    /** Outline variant — subtle borders. */
    OUTLINE_VARIANT(
        roleName = "outlineVariant",
        description = "Outline variant — subtle borders.",
    ),

    /** Scrim — overlay over content (e.g. behind modals). */
    SCRIM(
        roleName = "scrim",
        description = "Scrim — overlay over content.",
    ),

    /** Wave — the ambient wave layer color. */
    WAVE(
        roleName = "wave",
        description = "Wave layer color.",
    ),

    /** Glass tint — the tint applied to glass surfaces. */
    GLASS_TINT(
        roleName = "glassTint",
        description = "Glass tint color.",
    ),

    /** Error — error state color. */
    ERROR(
        roleName = "error",
        description = "Error state color.",
    ),

    /** Success — success state color. */
    SUCCESS(
        roleName = "success",
        description = "Success state color.",
    ),

    /** Warning — warning state color. */
    WARNING(
        roleName = "warning",
        description = "Warning state color.",
    ),
}

/**
 * A semantic typography role in the Library visual language.
 *
 * The renderer reads these roles; the UI layer (B5.3) maps each
 * role to a concrete TextStyle (font family + size + weight).
 */
enum class LibraryTypographyRole(
    internal val roleName: String,
    internal val description: String,
    internal val relativeSize: Float,
    internal val defaultWeight: Int,
) {
    /** Hero title — largest, for hero card titles. */
    HERO_TITLE(
        roleName = "heroTitle",
        description = "Hero title — largest.",
        relativeSize = 1.8f,
        defaultWeight = 700,
    ),

    /** Headline large — for the top bar title (expanded). */
    HEADLINE_LARGE(
        roleName = "headlineLarge",
        description = "Headline large — top bar expanded.",
        relativeSize = 1.5f,
        defaultWeight = 600,
    ),

    /** Headline medium — for the top bar title (compact). */
    HEADLINE_MEDIUM(
        roleName = "headlineMedium",
        description = "Headline medium — top bar compact.",
        relativeSize = 1.3f,
        defaultWeight = 600,
    ),

    /** Title large — for section headers. */
    TITLE_LARGE(
        roleName = "titleLarge",
        description = "Title large — section headers.",
        relativeSize = 1.15f,
        defaultWeight = 600,
    ),

    /** Title medium — for card titles. */
    TITLE_MEDIUM(
        roleName = "titleMedium",
        description = "Title medium — card titles.",
        relativeSize = 1.0f,
        defaultWeight = 500,
    ),

    /** Title small — for small card titles. */
    TITLE_SMALL(
        roleName = "titleSmall",
        description = "Title small — small card titles.",
        relativeSize = 0.9f,
        defaultWeight = 500,
    ),

    /** Body large — primary body text. */
    BODY_LARGE(
        roleName = "bodyLarge",
        description = "Body large — primary body text.",
        relativeSize = 0.95f,
        defaultWeight = 400,
    ),

    /** Body medium — secondary body text. */
    BODY_MEDIUM(
        roleName = "bodyMedium",
        description = "Body medium — secondary body text.",
        relativeSize = 0.88f,
        defaultWeight = 400,
    ),

    /** Body small — tertiary body text. */
    BODY_SMALL(
        roleName = "bodySmall",
        description = "Body small — tertiary body text.",
        relativeSize = 0.8f,
        defaultWeight = 400,
    ),

    /** Label — for chips / buttons. */
    LABEL(
        roleName = "label",
        description = "Label — chips / buttons.",
        relativeSize = 0.82f,
        defaultWeight = 500,
    ),

    /** Caption — for badges / metadata. */
    CAPTION(
        roleName = "caption",
        description = "Caption — badges / metadata.",
        relativeSize = 0.72f,
        defaultWeight = 400,
    ),

    /** Overline — for tiny labels above titles. */
    OVERLINE(
        roleName = "overline",
        description = "Overline — tiny labels above titles.",
        relativeSize = 0.68f,
        defaultWeight = 600,
    ),

    /** Empty state title. */
    EMPTY_TITLE(
        roleName = "emptyTitle",
        description = "Empty state title.",
        relativeSize = 1.4f,
        defaultWeight = 600,
    ),

    /** Empty state message. */
    EMPTY_MESSAGE(
        roleName = "emptyMessage",
        description = "Empty state message.",
        relativeSize = 0.95f,
        defaultWeight = 400,
    ),
}

/**
 * A semantic iconography role.
 */
enum class LibraryIconographyRole(
    internal val roleName: String,
    internal val description: String,
    internal val defaultSizeDp: Int,
) {
    /** Chip icon — small icon inside a top bar chip. */
    CHIP(
        roleName = "chip",
        description = "Chip icon.",
        defaultSizeDp = 20,
    ),

    /** Card icon — small icon on a card (e.g. explicit badge). */
    CARD(
        roleName = "card",
        description = "Card icon.",
        defaultSizeDp = 16,
    ),

    /** Action icon — for action buttons. */
    ACTION(
        roleName = "action",
        description = "Action icon.",
        defaultSizeDp = 24,
    ),

    /** Decorative icon — for empty states. */
    DECORATIVE(
        roleName = "decorative",
        description = "Decorative icon.",
        defaultSizeDp = 48,
    ),

    /** Navigation icon — for back / menu. */
    NAVIGATION(
        roleName = "navigation",
        description = "Navigation icon.",
        defaultSizeDp = 24,
    ),

    /** Status icon — for playback state. */
    STATUS(
        roleName = "status",
        description = "Status icon.",
        defaultSizeDp = 20,
    ),
}

/**
 * The visual density of the Library.
 *
 * @property description   Density description.
 * @property horizontalPaddingDp  Horizontal padding scale.
 * @property verticalGapDp         Vertical gap scale.
 * @property cardDensity           Cards per row (for grids).
 */
enum class LibraryVisualDensity(
    internal val description: String,
    internal val horizontalPaddingDp: Int,
    internal val verticalGapDp: Int,
    internal val cardDensity: Int,
) {
    /** Compact — maximum information density. */
    COMPACT(
        description = "Compact — maximum density.",
        horizontalPaddingDp = 12,
        verticalGapDp = 8,
        cardDensity = 3,
    ),

    /** Standard — balanced density. */
    STANDARD(
        description = "Standard — balanced.",
        horizontalPaddingDp = 16,
        verticalGapDp = 12,
        cardDensity = 2,
    ),

    /** Spacious — generous breathing room. */
    SPACIOUS(
        description = "Spacious — generous breathing room.",
        horizontalPaddingDp = 20,
        verticalGapDp = 16,
        cardDensity = 2,
    ),

    /** Cinematic — maximum breathing room, one card at a time. */
    CINEMATIC(
        description = "Cinematic — one card at a time.",
        horizontalPaddingDp = 24,
        verticalGapDp = 24,
        cardDensity = 1,
    ),
}

/**
 * The Library Visual Language.
 *
 * @property density          Visual density.
 * @property iconStyle        Icon style.
 * @property cornerRadiusStyle  Corner radius style.
 * @property motionLanguage   Motion language reference.
 */
data class LibraryVisualLanguage(
    val density: LibraryVisualDensity,
    val iconStyle: LibraryIconStyle,
    val cornerRadiusStyle: LibraryCornerRadiusStyle,
) {
    companion object {
        val DEFAULT = LibraryVisualLanguage(
            density = LibraryVisualDensity.STANDARD,
            iconStyle = LibraryIconStyle.ROUNDED,
            cornerRadiusStyle = LibraryCornerRadiusStyle.MEDIUM,
        )

        val COMPACT = LibraryVisualLanguage(
            density = LibraryVisualDensity.COMPACT,
            iconStyle = LibraryIconStyle.SHARP,
            cornerRadiusStyle = LibraryCornerRadiusStyle.SMALL,
        )

        val CINEMATIC = LibraryVisualLanguage(
            density = LibraryVisualDensity.CINEMATIC,
            iconStyle = LibraryIconStyle.ROUNDED,
            cornerRadiusStyle = LibraryCornerRadiusStyle.LARGE,
        )
    }
}

/**
 * Style of icons in the Library.
 */
enum class LibraryIconStyle(
    internal val description: String,
) {
    /** Filled — solid icons. */
    FILLED("Filled — solid icons."),

    /** Outlined — stroke-only icons. */
    OUTLINED("Outlined — stroke-only."),

    /** Rounded — rounded corners on icons. */
    ROUNDED("Rounded — rounded corners."),

    /** Sharp — sharp corners on icons. */
    SHARP("Sharp — sharp corners."),

    /** Two-tone — dual-color icons. */
    TWO_TONE("Two-tone — dual-color."),
}

/**
 * Corner radius style.
 */
enum class LibraryCornerRadiusStyle(
    internal val description: String,
    internal val smallDp: Int,
    internal val mediumDp: Int,
    internal val largeDp: Int,
    internal val extraLargeDp: Int,
) {
    /** Sharp — minimal radius. */
    SHARP(
        description = "Sharp — minimal radius.",
        smallDp = 2,
        mediumDp = 4,
        largeDp = 8,
        extraLargeDp = 12,
    ),

    /** Small — subtle radius. */
    SMALL(
        description = "Small — subtle radius.",
        smallDp = 4,
        mediumDp = 8,
        largeDp = 12,
        extraLargeDp = 16,
    ),

    /** Medium — balanced radius. */
    MEDIUM(
        description = "Medium — balanced.",
        smallDp = 8,
        mediumDp = 12,
        largeDp = 16,
        extraLargeDp = 24,
    ),

    /** Large — generous radius. */
    LARGE(
        description = "Large — generous.",
        smallDp = 12,
        mediumDp = 16,
        largeDp = 24,
        extraLargeDp = 32,
    ),

    /** Extra-large — for hero cards. */
    EXTRA_LARGE(
        description = "Extra-large — hero cards.",
        smallDp = 16,
        mediumDp = 24,
        largeDp = 32,
        extraLargeDp = 48,
    ),
}
