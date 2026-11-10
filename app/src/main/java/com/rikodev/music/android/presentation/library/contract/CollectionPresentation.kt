/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B1 — Collection Contract Pack v2 (Final)
 *
 * File 8 / 15 — CollectionPresentation.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * The *presentation contract* declares HOW a collection should
 * look — independent of HOW it is rendered. The renderer (B4) is
 * free to interpret these tokens against the active theme, but it
 * MUST NOT invent layout properties that are not declared here.
 *
 * Presentation is composed of:
 *   • Card Style      — what each card looks like
 *   • Section Style   — how cards group on screen
 *   • Spacing         — rhythm between elements
 *   • Header Type     — how the section header renders
 *   • Wave Visibility — does this collection paint a wave?
 *   • Glass Strength  — how frosted is the background?
 *   • Background      — what sits behind the cards
 *   • Animation       — motion profile
 *   • Scroll Behaviour — snap? free? paged?
 *   • Accent          — accent color derivation rule
 *   • Padding         — outer padding contract
 *   • Elevation       — z-depth of cards
 *   • Shadow          — shadow style
 *   • Divider         — separator between sections
 *   • Radius          — corner radius scale
 *
 * RULE #28 — Motion Explains Hierarchy
 * RULE #33 — Collection Has Identity
 *
 * No logic. Pure declaration.
 */

package com.rikodev.music.android.presentation.library.contract

/**
 * Visual style of an individual card.
 */
enum class CardStyle(internal val description: String) {
    /** Tall hero card — 1.4× aspect, large album art, title overlay. */
    HERO_TALL("Tall hero card — large album art, title overlay."),

    /** Square card with album art and metadata below. */
    SQUARE("Square card — album art with metadata below."),

    /** Compact card — small album art + title only. */
    COMPACT("Compact card — small album art + title only."),

    /** Wide row card — full-width, horizontal layout. */
    ROW_WIDE("Wide row card — full-width, horizontal layout."),

    /** Stacked card with parallax depth — multiple cards visible behind. */
    STACK_DEPTH("Stacked card with parallax depth."),

    /** Text-only card — used for time-travel / decade cards. */
    TEXT_ONLY("Text-only card — minimal chrome, large typography."),
}

/**
 * Visual style of a section (the group of cards).
 */
enum class SectionStyle(internal val description: String) {
    /** Horizontal scrolling strip with peek. */
    HORIZONTAL_STRIP("Horizontal scrolling strip with peek of next card."),

    /** 2-column grid of square cards. */
    GRID_2("2-column grid."),

    /** 3-column dense grid. */
    GRID_3("3-column dense grid."),

    /** Vertical list — full-width rows. */
    LIST("Vertical list — full-width rows."),

    /** Single full-width hero card per row. */
    HERO_SINGLE("Single full-width hero card per row."),

    /** Stacked cards with parallax — 2–3 cards visible at once. */
    STACK("Stacked cards with parallax."),
}

/**
 * Header rendering style for a section.
 */
enum class HeaderType(internal val description: String) {
    /** Large title + subtitle + optional action chip. */
    LARGE_TITLE("Large title + subtitle + optional action chip."),

    /** Medium title + single subtitle line. */
    MEDIUM_TITLE("Medium title + single subtitle line."),

    /** Small title only — minimal. */
    SMALL_TITLE("Small title only — minimal."),

    /** Title with a refresh action. */
    TITLE_WITH_REFRESH("Title with refresh action on the right."),

    /** Title with see-all action. */
    TITLE_WITH_SEE_ALL("Title with see-all action on the right."),

    /** No header — section is identified by spacing alone. */
    NONE("No header — section is identified by spacing alone."),
}

/**
 * How the wave (RIKO's signature motion element) renders for
 * this collection. Rule #28 — Library wave is always capped.
 */
enum class WaveVisibility(
    internal val description: String,
    internal val maxOpacity: Float,
    internal val maxThickness: Float,
) {
    /** Wave never renders for this collection. */
    OFF("No wave.", 0.0f, 0.0f),

    /** Whisper-faint wave — barely visible. */
    SUBTLE("Subtle wave — barely visible.", 0.15f, 0.4f),

    /** Library-standard wave — Rule #28 cap. */
    LIBRARY_STANDARD("Library-standard wave — Rule #28 cap.", 0.3f, 0.5f),

    /** Slightly emphasized wave for hero collections. */
    EMPHASIZED("Emphasized wave — hero collections only.", 0.45f, 0.7f),
}

/**
 * Glass tint strength behind cards.
 */
enum class GlassStrength(internal val weight: Float) {
    NONE(0.0f),
    LIGHT(0.3f),
    MEDIUM(0.5f),
    STRONG(0.7f),
    HEAVY(0.9f),
}

/**
 * Background layer behind the section.
 */
enum class BackgroundLayer(internal val description: String) {
    /** Transparent — sits on the Library's main background. */
    TRANSPARENT("Transparent — Library main background shows through."),

    /** Subtle gradient derived from album art palette. */
    PALETTE_GRADIENT("Subtle gradient from album art palette."),

    /** Solid surface — color from theme. */
    SOLID_SURFACE("Solid surface color from theme."),

    /** Glass blur — frosted, palette-tinted. */
    GLASS_BLUR("Frosted glass — palette-tinted."),

    /** Dark scrim — used for night-themed sections. */
    DARK_SCRIM("Dark scrim — night-themed sections."),
}

/**
 * Animation profile for cards entering / leaving the section.
 */
enum class AnimationProfile(
    internal val damping: Float,
    internal val durationMs: Int,
    internal val description: String,
) {
    NONE(1.0f, 0, "No animation."),
    SUBTLE(0.8f, 180, "Subtle fade-in."),
    BALANCED(0.7f, 260, "Balanced fade + slight scale."),
    GENTLE(0.6f, 340, "Gentle drift-in."),
    DREAMY(0.5f, 460, "Dreamy drift-in with motion blur."),
    HERO_REVEAL(0.65f, 520, "Hero reveal — large cards unfold."),
}

/**
 * Scroll behaviour for the section.
 */
enum class ScrollBehaviour(internal val description: String) {
    /** Free scrolling — no snap. */
    FREE("Free scrolling — no snap."),

    /** Snap to card boundaries. */
    SNAP_TO_CARD("Snap to card boundaries."),

    /** Paged — one card per swipe. */
    PAGED("Paged — one card per swipe."),

    /** Vertical list scrolling — section is part of the main column. */
    VERTICAL_LIST("Vertical list scrolling — part of main column."),

    /** Sticky header — header pins to top when section is in view. */
    STICKY_HEADER("Sticky header — header pins to top when section is in view."),
}

/**
 * Accent color derivation rule for the collection.
 */
enum class AccentStyle(internal val description: String) {
    /** No accent — use theme default. */
    NONE("No accent — theme default."),

    /** Derive accent from the first card's album art. */
    FROM_FIRST_CARD("Accent derived from first card's album art."),

    /** Derive accent from dominant emotion. */
    FROM_EMOTION("Accent derived from dominant emotion warmth."),

    /** Fixed accent per collection type (declared in Constitution). */
    FIXED_PER_TYPE("Fixed accent per collection type — declared in Constitution."),

    /** Gradient — blend of top 3 cards' palette. */
    GRADIENT_TOP_3("Gradient — blend of top 3 cards' palette."),
}

/**
 * Outer padding contract.
 */
enum class PaddingProfile(
    val horizontalDp: Int,
    val verticalDp: Int,
    val cardGapDp: Int,
) {
    NONE(0, 0, 0),
    TIGHT(8, 8, 6),
    STANDARD(16, 12, 10),
    SPACIOUS(20, 16, 14),
    HERO(24, 20, 16),
}

/**
 * Elevation profile (z-depth of cards).
 */
enum class ElevationProfile(internal val dp: Float) {
    FLAT(0f),
    LOW(1f),
    MEDIUM(3f),
    HIGH(6f),
    FLOATING(10f),
}

/**
 * Shadow style — paired with elevation.
 */
enum class ShadowStyle(internal val description: String) {
    NONE("No shadow."),
    SOFT("Soft ambient shadow."),
    DROP("Drop shadow — directional."),
    INNER("Inner shadow — recessed look."),
    GLOW("Glow shadow — accent-tinted bloom."),
}

/**
 * Divider style between sections.
 */
enum class DividerStyle(internal val description: String) {
    NONE("No divider — sections separated by spacing alone."),
    THIN_LINE("Thin line — 0.5dp hairline."),
    HAIRLINE_FADED("Hairline with fade at both ends."),
    SECTION_GAP("Section gap — 24dp whitespace."),
    GRADIENT_FADE("Gradient fade — soft transition between sections."),
}

/**
 * Corner radius scale.
 */
enum class RadiusScale(
    val cardDp: Int,
    val headerDp: Int,
    val backgroundDp: Int,
) {
    SHARP(0, 0, 0),
    SUBTLE(6, 4, 8),
    STANDARD(12, 8, 16),
    SOFT(16, 12, 20),
    PILL(24, 16, 28),
}

/**
 * The complete presentation contract for a collection.
 *
 * This data class is referenced from [CollectionContract] (File 14).
 * The renderer (B4) reads these tokens and resolves them against
 * the active theme. The contract NEVER references theme directly —
 * it stays theme-agnostic.
 */
data class CollectionPresentation(
    val cardStyle: CardStyle,
    val sectionStyle: SectionStyle,
    val headerType: HeaderType,
    val waveVisibility: WaveVisibility,
    val glassStrength: GlassStrength,
    val backgroundLayer: BackgroundLayer,
    val animationProfile: AnimationProfile,
    val scrollBehaviour: ScrollBehaviour,
    val accentStyle: AccentStyle,
    val padding: PaddingProfile,
    val elevation: ElevationProfile,
    val shadow: ShadowStyle,
    val divider: DividerStyle,
    val radius: RadiusScale,
) {

    companion object {

        /** Used when a collection does not declare its own presentation. */
        val DEFAULT = CollectionPresentation(
            cardStyle = CardStyle.SQUARE,
            sectionStyle = SectionStyle.HORIZONTAL_STRIP,
            headerType = HeaderType.MEDIUM_TITLE,
            waveVisibility = WaveVisibility.LIBRARY_STANDARD,
            glassStrength = GlassStrength.MEDIUM,
            backgroundLayer = BackgroundLayer.TRANSPARENT,
            animationProfile = AnimationProfile.BALANCED,
            scrollBehaviour = ScrollBehaviour.SNAP_TO_CARD,
            accentStyle = AccentStyle.FROM_FIRST_CARD,
            padding = PaddingProfile.STANDARD,
            elevation = ElevationProfile.LOW,
            shadow = ShadowStyle.SOFT,
            divider = DividerStyle.SECTION_GAP,
            radius = RadiusScale.STANDARD,
        )

        /** Hero collection presentation — for ContinueListening, RecentlyPlayed. */
        val HERO = DEFAULT.copy(
            cardStyle = CardStyle.HERO_TALL,
            sectionStyle = SectionStyle.HORIZONTAL_STRIP,
            headerType = HeaderType.LARGE_TITLE,
            waveVisibility = WaveVisibility.EMPHASIZED,
            glassStrength = GlassStrength.STRONG,
            animationProfile = AnimationProfile.HERO_REVEAL,
            scrollBehaviour = ScrollBehaviour.SNAP_TO_CARD,
            padding = PaddingProfile.HERO,
            elevation = ElevationProfile.MEDIUM,
            shadow = ShadowStyle.SOFT,
            radius = RadiusScale.SOFT,
        )

        /** Grid presentation — for AllAlbums, AllArtists. */
        val GRID = DEFAULT.copy(
            cardStyle = CardStyle.SQUARE,
            sectionStyle = SectionStyle.GRID_3,
            headerType = HeaderType.MEDIUM_TITLE,
            waveVisibility = WaveVisibility.OFF,
            glassStrength = GlassStrength.NONE,
            backgroundLayer = BackgroundLayer.TRANSPARENT,
            animationProfile = AnimationProfile.SUBTLE,
            scrollBehaviour = ScrollBehaviour.FREE,
            padding = PaddingProfile.STANDARD,
            elevation = ElevationProfile.FLAT,
            shadow = ShadowStyle.NONE,
            radius = RadiusScale.STANDARD,
        )

        /** List presentation — for AllSongs, History. */
        val LIST = DEFAULT.copy(
            cardStyle = CardStyle.ROW_WIDE,
            sectionStyle = SectionStyle.LIST,
            headerType = HeaderType.SMALL_TITLE,
            waveVisibility = WaveVisibility.OFF,
            glassStrength = GlassStrength.NONE,
            backgroundLayer = BackgroundLayer.TRANSPARENT,
            animationProfile = AnimationProfile.SUBTLE,
            scrollBehaviour = ScrollBehaviour.VERTICAL_LIST,
            padding = PaddingProfile.STANDARD,
            elevation = ElevationProfile.FLAT,
            shadow = ShadowStyle.NONE,
            radius = RadiusScale.SUBTLE,
        )

        /** Discovery presentation — for HiddenGems, Discovery. */
        val DISCOVERY = DEFAULT.copy(
            cardStyle = CardStyle.SQUARE,
            sectionStyle = SectionStyle.HORIZONTAL_STRIP,
            headerType = HeaderType.TITLE_WITH_REFRESH,
            waveVisibility = WaveVisibility.SUBTLE,
            glassStrength = GlassStrength.LIGHT,
            backgroundLayer = BackgroundLayer.PALETTE_GRADIENT,
            animationProfile = AnimationProfile.GENTLE,
            scrollBehaviour = ScrollBehaviour.SNAP_TO_CARD,
            accentStyle = AccentStyle.GRADIENT_TOP_3,
            padding = PaddingProfile.SPACIOUS,
            elevation = ElevationProfile.LOW,
            shadow = ShadowStyle.SOFT,
            radius = RadiusScale.SOFT,
        )

        /** Mood presentation — for NightCalm, MorningEnergy. */
        val MOOD = DEFAULT.copy(
            cardStyle = CardStyle.HERO_TALL,
            sectionStyle = SectionStyle.HORIZONTAL_STRIP,
            headerType = HeaderType.LARGE_TITLE,
            waveVisibility = WaveVisibility.LIBRARY_STANDARD,
            glassStrength = GlassStrength.STRONG,
            backgroundLayer = BackgroundLayer.DARK_SCRIM,
            animationProfile = AnimationProfile.GENTLE,
            scrollBehaviour = ScrollBehaviour.SNAP_TO_CARD,
            accentStyle = AccentStyle.FROM_EMOTION,
            padding = PaddingProfile.SPACIOUS,
            elevation = ElevationProfile.LOW,
            shadow = ShadowStyle.GLOW,
            radius = RadiusScale.SOFT,
        )
    }
}
