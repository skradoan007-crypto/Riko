/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B4 — Library Rendering Runtime
 *
 * File 4 / 13 — RenderLayout.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * A [RenderLayout] is the *resolved* layout instruction for a
 * section. It tells the UI *how* to lay out the items — grid, list,
 * strip, stack, hero — and the spacing / sizing rules that go with
 * each variant.
 *
 * The runtime never says "this section is a 2-column grid". It says
 * "this section uses CardLayoutPreference.GRID_SQUARE" (a contract
 * enum). The renderer (B4) interprets that enum, plus the active
 * CollectionPresentation, plus the device class, and produces a
 * concrete [RenderLayout] the UI can consume directly.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #28  Motion Explains Hierarchy   — layout carries motion hints
 * #33  Collection Has Identity     — layout is per-section, not global
 *
 * RELATIONSHIP TO CONTRACTS
 * -------------------------
 * Sources:
 *   • [CardLayoutPreference]     (B1 — CollectionPersonality.kt)
 *   • [SectionStyle]             (B1 — CollectionPresentation.kt)
 *   • [CardStyle]                (B1 — CollectionPresentation.kt)
 *   • [CollectionPresentation]   (B1 — full presentation contract)
 *
 * Outputs:
 *   • [RenderLayout]             (B4 — this file)
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Android, no Compose, no Coroutine, no Flow. Pure Kotlin data.
 *
 * © RIKO Experience OS — Rendering Tier
 */

package com.rikodev.music.android.presentation.library.rendering.layout

/**
 * The concrete layout variant the UI should apply.
 *
 * Each variant is a sealed subclass carrying the parameters the UI
 * needs to render it correctly. The UI matches on the variant and
 * applies the corresponding Composable.
 *
 * Variants are deliberately *concrete* — there is no "CUSTOM" escape
 * hatch. Adding a new layout means adding a new variant, which means
 * the compiler will force every consumer to handle it.
 */
sealed class RenderLayout {

    /**
     * Card spacing in dp — applied uniformly across the layout.
     */
    abstract val spacingDp: Int

    /**
     * Outer padding in dp — applied around the entire section.
     */
    abstract val outerPaddingDp: Int

    /**
     * Whether the layout scrolls horizontally (true) or vertically (false).
     * Drives whether the UI nests this section in a horizontal scroller.
     */
    abstract val scrollsHorizontally: Boolean

    /**
     * Whether the layout should snap (paged) when scrolling.
     */
    abstract val snapsOnScroll: Boolean

    /**
     * 2-column grid of square cards.
     *
     * Used for: Most Played, Favorites, Recently Added.
     * Visual feel: balanced, scannable, gallery-like.
     */
    data class GridSquare2(
        override val spacingDp: Int = 12,
        override val outerPaddingDp: Int = 16,
        override val scrollsHorizontally: Boolean = false,
        override val snapsOnScroll: Boolean = false,
        val rowSpacingDp: Int = 16,
    ) : RenderLayout()

    /**
     * 3-column dense grid of small square cards.
     *
     * Used for: Discovery, Genres, Moods.
     * Visual feel: dense, exploratory, slightly compressed.
     */
    data class GridDense3(
        override val spacingDp: Int = 8,
        override val outerPaddingDp: Int = 16,
        override val scrollsHorizontally: Boolean = false,
        override val snapsOnScroll: Boolean = false,
        val rowSpacingDp: Int = 12,
    ) : RenderLayout()

    /**
     * Horizontal hero strip — large cards, one per screen-width peek.
     *
     * Used for: Continue Listening, Forgotten Classics, Featured.
     * Visual feel: cinematic, focused, one-at-a-time.
     */
    data class HeroStrip(
        override val spacingDp: Int = 16,
        override val outerPaddingDp: Int = 16,
        override val scrollsHorizontally: Boolean = true,
        override val snapsOnScroll: Boolean = true,
        val cardWidthDp: Int = 280,
        val cardHeightDp: Int = 360,
        val peekNextCardDp: Int = 32,
    ) : RenderLayout()

    /**
     * Compact horizontal strip — 2–3 cards visible at once.
     *
     * Used for: Hidden Gems, Recently Played, Quick Picks.
     * Visual feel: browseable, casual, multiple-choice.
     */
    data class CompactStrip(
        override val spacingDp: Int = 12,
        override val outerPaddingDp: Int = 16,
        override val scrollsHorizontally: Boolean = true,
        override val snapsOnScroll: Boolean = false,
        val cardWidthDp: Int = 160,
        val cardHeightDp: Int = 200,
        val peekNextCardDp: Int = 16,
    ) : RenderLayout()

    /**
     * Vertical list — full-width rows.
     *
     * Used for: Playlists, Artists (sometimes), Search results.
     * Visual feel: linear, scannable, list-like.
     */
    data class ListVertical(
        override val spacingDp: Int = 0,
        override val outerPaddingDp: Int = 16,
        override val scrollsHorizontally: Boolean = false,
        override val snapsOnScroll: Boolean = false,
        val rowHeightDp: Int = 72,
        val showDivider: Boolean = true,
        val dividerInsetDp: Int = 72,
    ) : RenderLayout()

    /**
     * Stacked hero cards with parallax depth.
     *
     * Used for: Mood-based, Time-travel, Featured hero.
     * Visual feel: layered, dimensional, premium.
     */
    data class StackDepth(
        override val spacingDp: Int = 0,
        override val outerPaddingDp: Int = 16,
        override val scrollsHorizontally: Boolean = true,
        override val snapsOnScroll: Boolean = true,
        val cardWidthDp: Int = 320,
        val cardHeightDp: Int = 400,
        val visibleStackDepth: Int = 3,
        val parallaxFactor: Float = 0.3f,
    ) : RenderLayout()

    /**
     * Single hero card — full-width, one at a time.
     *
     * Used for: Featured "Surprise Me", daily recommendation.
     * Visual feel: bold, declarative, single-focus.
     */
    data class HeroSingle(
        override val spacingDp: Int = 0,
        override val outerPaddingDp: Int = 16,
        override val scrollsHorizontally: Boolean = false,
        override val snapsOnScroll: Boolean = false,
        val cardHeightDp: Int = 280,
    ) : RenderLayout()

    /**
     * Text-only list — for abstract / time-travel cards.
     *
     * Used for: Decade cards, abstract mood cards.
     * Visual feel: typographic, minimal, editorial.
     */
    data class TextList(
        override val spacingDp: Int = 0,
        override val outerPaddingDp: Int = 24,
        override val scrollsHorizontally: Boolean = false,
        override val snapsOnScroll: Boolean = false,
        val rowHeightDp: Int = 88,
        val showDivider: Boolean = true,
    ) : RenderLayout()
}

/**
 * A resolved layout decision — the layout + the reason it was chosen.
 *
 * The reason is for diagnostics only (Rule #53). The UI never
 * branches on it — it just consumes the [layout].
 *
 * @property layout       The resolved layout spec.
 * @property sourceHint   Why this layout was chosen. Diagnostics only.
 */
data class ResolvedLayout(
    val layout: RenderLayout,
    val sourceHint: LayoutSourceHint,
)

/**
 * Why a particular layout was chosen.
 *
 * The renderer consults the contract's [CardLayoutPreference] first,
 * but may override based on device class, item count, accessibility
 * settings, or theme. This enum records the decision path.
 */
enum class LayoutSourceHint(internal val description: String) {
    /** Layout taken directly from the contract's preference. */
    FROM_CONTRACT("Layout taken from contract preference."),

    /** Contract preference overridden by item count (too few for grid). */
    ITEM_COUNT_OVERRIDE("Overridden due to low item count."),

    /** Contract preference overridden by device class (e.g. tablet vs phone). */
    DEVICE_CLASS_OVERRIDE("Overridden due to device class."),

    /** Contract preference overridden by accessibility settings. */
    ACCESSIBILITY_OVERRIDE("Overridden due to accessibility settings."),

    /** Layout chosen because contract left it unspecified. */
    DEFAULT_FALLBACK("Default fallback — contract did not specify."),
}
