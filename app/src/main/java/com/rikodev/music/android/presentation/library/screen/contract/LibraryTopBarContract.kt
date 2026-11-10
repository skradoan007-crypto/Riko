/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.1 — Library Screen Constitution
 *
 * File 2 / 15 — LibraryTopBarContract.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * The Library Top Bar is the screen's orientation layer. It tells
 * the user "you are in Library, and these are the things you can
 * do here". It is the first slot in the story (Rule #54).
 *
 * The Top Bar is NOT a toolbar in the traditional sense. It is a
 * *quiet authority* — large title that breathes, a subtitle that
 * whispers context, and a small set of action chips that invite
 * (never demand) interaction.
 *
 * STATES
 * ------
 * The Top Bar has three states:
 *   • EXPANDED  — large title, full subtitle, all chips visible
 *   • COMPACT   — title shrinks, subtitle hidden, chips remain
 *   • HIDDEN    — fully off-screen (immersive / fullscreen)
 *
 * Transitions between states are driven by scroll, not by time.
 * See [LibraryTopBarCollapseRule].
 *
 * RULES ENFORCED HERE
 * -------------------
 * #29  Motion Explains Hierarchy   — collapse motion is hierarchy-aware
 * #33  Collection Has Identity     — top bar identity is "Library"
 * #54  Screen Tells a Story        — top bar orients the user
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
 * The state of the Library Top Bar.
 */
enum class LibraryTopBarState(
    internal val description: String,
    internal val heightFraction: Float,
) {
    /** Expanded — large title, full subtitle, all chips visible. */
    EXPANDED(
        description = "Expanded — large title, full subtitle, all chips.",
        heightFraction = 1.0f,
    ),

    /** Compact — title shrinks, subtitle hidden, chips remain. */
    COMPACT(
        description = "Compact — small title, no subtitle, chips remain.",
        heightFraction = 0.58f,
    ),

    /** Hidden — fully off-screen (immersive / fullscreen modes). */
    HIDDEN(
        description = "Hidden — off-screen for immersive modes.",
        heightFraction = 0.0f,
    ),
}

/**
 * Rule for when the Top Bar should collapse.
 *
 * Driven by scroll position — the Top Bar collapses as the user
 * scrolls down, expands as they scroll back to top.
 *
 * @property collapseThresholdDp   Scroll offset (dp) at which collapse begins.
 * @property fullCollapseThresholdDp  Scroll offset at which collapse completes.
 * @property expandThresholdDp     Scroll offset below which the bar re-expands.
 * @property isAnchored            True if the bar snaps between states (no
 *                                intermediate positions). False for smooth.
 */
data class LibraryTopBarCollapseRule(
    val collapseThresholdDp: Int,
    val fullCollapseThresholdDp: Int,
    val expandThresholdDp: Int,
    val isAnchored: Boolean,
) {
    init {
        require(collapseThresholdDp >= 0) {
            "collapseThresholdDp must be >= 0, got $collapseThresholdDp"
        }
        require(fullCollapseThresholdDp >= collapseThresholdDp) {
            "fullCollapseThresholdDp must be >= collapseThresholdDp"
        }
        require(expandThresholdDp >= 0) {
            "expandThresholdDp must be >= 0, got $expandThresholdDp"
        }
    }

    companion object {
        /** Standard collapse rule — collapse between 80dp and 160dp scroll. */
        val STANDARD = LibraryTopBarCollapseRule(
            collapseThresholdDp = 80,
            fullCollapseThresholdDp = 160,
            expandThresholdDp = 40,
            isAnchored = false,
        )

        /** Anchored (snapping) collapse rule — for premium feel. */
        val ANCHORED = LibraryTopBarCollapseRule(
            collapseThresholdDp = 96,
            fullCollapseThresholdDp = 96,
            expandThresholdDp = 48,
            isAnchored = true,
        )
    }
}

/**
 * A chip in the Top Bar — small action affordance.
 *
 * Chips are not buttons — they are *invitations*. Each chip has a
 * specific role and a specific prominence. The Top Bar never shows
 * more than 3 chips at once (Rule of Three).
 *
 * @property kind            Chip kind — drives icon + behavior.
 * @property prominence      Visual prominence.
 * @property isVisible       True if the chip is currently visible.
 * @property badgeCount      Optional badge count (e.g. new songs).
 * @property isEnabled       False if the chip is currently disabled.
 */
data class LibraryTopBarChip(
    val kind: LibraryTopBarChipKind,
    val prominence: LibraryTopBarChipProminence,
    val isVisible: Boolean,
    val badgeCount: Int,
    val isEnabled: Boolean,
) {
    companion object {
        val SEARCH = LibraryTopBarChip(
            kind = LibraryTopBarChipKind.SEARCH,
            prominence = LibraryTopBarChipProminence.PRIMARY,
            isVisible = true,
            badgeCount = 0,
            isEnabled = true,
        )

        val REFRESH = LibraryTopBarChip(
            kind = LibraryTopBarChipKind.REFRESH,
            prominence = LibraryTopBarChipProminence.SECONDARY,
            isVisible = true,
            badgeCount = 0,
            isEnabled = true,
        )

        val SORT = LibraryTopBarChip(
            kind = LibraryTopBarChipKind.SORT,
            prominence = LibraryTopBarChipProminence.SECONDARY,
            isVisible = true,
            badgeCount = 0,
            isEnabled = true,
        )
    }
}

/**
 * Kind of chip in the Top Bar.
 */
enum class LibraryTopBarChipKind(
    internal val description: String,
    internal val defaultIconKey: String,
) {
    /** Search — opens the Search experience. */
    SEARCH("Search chip — opens Search experience.", "icon.search"),

    /** Refresh — triggers a Library refresh. */
    REFRESH("Refresh chip — triggers Library refresh.", "icon.refresh"),

    /** Sort — opens the sort menu. */
    SORT("Sort chip — opens sort menu.", "icon.sort"),

    /** Filter — opens the filter panel. */
    FILTER("Filter chip — opens filter panel.", "icon.filter"),

    /** View toggle — switches between list / grid layouts. */
    VIEW_TOGGLE("View toggle — list / grid switch.", "icon.view"),

    /** Overflow — additional actions in a menu. */
    OVERFLOW("Overflow menu — additional actions.", "icon.overflow"),
}

/**
 * Visual prominence of a Top Bar chip.
 */
enum class LibraryTopBarChipProminence(
    internal val description: String,
    internal val backgroundOpacity: Float,
) {
    /** Primary chip — filled background, highest contrast. */
    PRIMARY("Primary — filled, highest contrast.", 1.0f),

    /** Secondary chip — outlined, medium contrast. */
    SECONDARY("Secondary — outlined, medium contrast.", 0.6f),

    /** Tertiary chip — text-only, low contrast. */
    TERTIARY("Tertiary — text-only, low contrast.", 0.3f),
}

/**
 * The title shape of the Top Bar.
 *
 * @property titleText          The title text (already localized).
 * @property subtitleText       Optional subtitle text.
 * @property titleTypographyKey Typography role for the title.
 * @property subtitleTypographyKey  Typography role for the subtitle.
 * @property titleMaxLines      Max lines for the title (1 or 2).
 */
data class LibraryTopBarTitle(
    val titleText: String,
    val subtitleText: String?,
    val titleTypographyKey: String,
    val subtitleTypographyKey: String,
    val titleMaxLines: Int,
) {
    companion object {
        /** Default Library title. */
        val DEFAULT = LibraryTopBarTitle(
            titleText = "Library",
            subtitleText = null,
            titleTypographyKey = "headlineLarge",
            subtitleTypographyKey = "bodySmall",
            titleMaxLines = 1,
        )

        /** Title with contextual subtitle — time-of-day greeting. */
        fun withGreeting(greeting: String, subtitle: String): LibraryTopBarTitle =
            LibraryTopBarTitle(
                titleText = greeting,
                subtitleText = subtitle,
                titleTypographyKey = "headlineMedium",
                subtitleTypographyKey = "bodySmall",
                titleMaxLines = 1,
            )
    }
}

/**
 * The Library Top Bar Contract.
 *
 * @property state              Current Top Bar state.
 * @property title              Title shape.
 * @property chips              Ordered list of chips (left-to-right).
 * @property collapseRule       Collapse behavior rule.
 * @property accentColorHex     Accent color — hex string.
 * @property glassStrength      Glass strength over the top bar.
 * @property isStickyOnCollapse True if the bar sticks to top when collapsed.
 */
data class LibraryTopBarContract(
    val state: LibraryTopBarState,
    val title: LibraryTopBarTitle,
    val chips: List<LibraryTopBarChip>,
    val collapseRule: LibraryTopBarCollapseRule,
    val accentColorHex: String,
    val glassStrength: LibraryTopBarGlassStrength,
    val isStickyOnCollapse: Boolean,
) {
    companion object {
        /** Default Top Bar — expanded, search + refresh + sort chips. */
        val DEFAULT = LibraryTopBarContract(
            state = LibraryTopBarState.EXPANDED,
            title = LibraryTopBarTitle.DEFAULT,
            chips = listOf(
                LibraryTopBarChip.SEARCH,
                LibraryTopBarChip.REFRESH,
                LibraryTopBarChip.SORT,
            ),
            collapseRule = LibraryTopBarCollapseRule.STANDARD,
            accentColorHex = "#FFFFFF",
            glassStrength = LibraryTopBarGlassStrength.MEDIUM,
            isStickyOnCollapse = true,
        )

        /** Compact Top Bar — for tablets / landscape. */
        val COMPACT = LibraryTopBarContract(
            state = LibraryTopBarState.COMPACT,
            title = LibraryTopBarTitle.DEFAULT,
            chips = listOf(
                LibraryTopBarChip.SEARCH,
                LibraryTopBarChip.REFRESH,
            ),
            collapseRule = LibraryTopBarCollapseRule.ANCHORED,
            accentColorHex = "#FFFFFF",
            glassStrength = LibraryTopBarGlassStrength.LIGHT,
            isStickyOnCollapse = true,
        )
    }

    /** True if the Top Bar is visible (not hidden). */
    val isVisible: Boolean get() = state != LibraryTopBarState.HIDDEN

    /** Number of visible chips. */
    val visibleChipCount: Int get() = chips.count { it.isVisible }

    /** True if the Top Bar is currently in expanded state. */
    val isExpanded: Boolean get() = state == LibraryTopBarState.EXPANDED
}

/**
 * Glass strength over the Top Bar.
 *
 * The Top Bar has a glass layer so content scrolls underneath it
 * with a subtle blur — this is part of RIKO's visual identity.
 */
enum class LibraryTopBarGlassStrength(
    internal val description: String,
    internal val blurRadiusDp: Int,
    internal val backgroundOpacity: Float,
) {
    /** None — fully transparent. */
    NONE("No glass — transparent.", 0, 0.0f),

    /** Light — minimal blur, low opacity. */
    LIGHT("Light glass.", 8, 0.4f),

    /** Medium — moderate blur, medium opacity. */
    MEDIUM("Medium glass.", 16, 0.6f),

    /** Strong — heavy blur, high opacity. */
    STRONG("Strong glass.", 24, 0.8f),
}
