/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.1 — Library Screen Constitution
 *
 * File 6 / 15 — LibraryScrollContract.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * The Library Scroll Contract declares *how* the content slot
 * scrolls. Scroll is not just "the list moves up and down" — it
 * is a *continuous gesture* that should feel like physical motion.
 *
 * RIKO's scroll feel is the sum of many small decisions:
 *   • Snap behavior (free / paged / snap-to-card)
 *   • Overscroll (stretch / glow / nothing)
 *   • Fast scroll (alphabetical jump bar)
 *   • Pull-to-refresh (threshold + spring back)
 *   • Fling decay (how quickly momentum dies)
 *   • Nested scroll (inner horizontal strips + outer vertical)
 *
 * Each of these is declared here as a contract. The renderer (B5.3)
 * maps each to a concrete Compose scroll configuration.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #29  Motion Explains Hierarchy   — scroll motion reflects hierarchy
 * #54  Screen Tells a Story        — scroll advances / retreats the story
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
 * The scroll mode of the content slot.
 */
enum class LibraryScrollMode(
    internal val description: String,
    internal val isPaged: Boolean,
) {
    /** Free scroll — content moves freely with momentum. */
    FREE("Free scroll — momentum-based.", isPaged = false),

    /** Snap to card — content snaps to card boundaries. */
    SNAP_TO_CARD("Snap to card boundaries.", isPaged = true),

    /** Paged — content moves one page at a time. */
    PAGED("Paged — one page at a time.", isPaged = true),

    /** Locked — scroll is disabled (e.g. selection mode). */
    LOCKED("Scroll locked.", isPaged = false),
}

/**
 * Overscroll behavior — what happens at the scroll edges.
 */
enum class LibraryOverscrollBehavior(
    internal val description: String,
) {
    /** Stretch — content stretches elastically at the edge. */
    STRETCH("Stretch — elastic at edges."),

    /** Glow — a subtle glow appears at the edge. */
    GLOW("Glow at edges."),

    /** Bounce — content bounces back (iOS-style). */
    BOUNCE("Bounce back."),

    /** None — content stops hard at the edge. */
    NONE("Hard stop at edges."),
}

/**
 * Fast scroll spec — the jump-bar on the right edge.
 *
 * @property isEnabled           True if fast scroll is enabled.
 * @property thumbWidthDp        Thumb width in dp.
 * @property thumbHeightDp       Thumb height in dp.
 * @property thumbColorHex       Thumb color (hex).
 * @property bubbleVisible       True if a bubble shows the current label.
 * @property bubbleSizeDp        Bubble diameter in dp.
 * @property bubbleColorHex      Bubble color (hex).
 * @property activationDistanceDp  How far the user must scroll before
 *                                 the thumb appears.
 */
data class LibraryFastScrollSpec(
    val isEnabled: Boolean,
    val thumbWidthDp: Int,
    val thumbHeightDp: Int,
    val thumbColorHex: String,
    val bubbleVisible: Boolean,
    val bubbleSizeDp: Int,
    val bubbleColorHex: String,
    val activationDistanceDp: Int,
) {
    companion object {
        /** Default fast scroll — visible thumb + bubble. */
        val DEFAULT = LibraryFastScrollSpec(
            isEnabled = true,
            thumbWidthDp = 4,
            thumbHeightDp = 48,
            thumbColorHex = "#88FFFFFF",
            bubbleVisible = true,
            bubbleSizeDp = 56,
            bubbleColorHex = "#FF5252",
            activationDistanceDp = 200,
        )

        /** Minimal fast scroll — thumb only, no bubble. */
        val MINIMAL = LibraryFastScrollSpec(
            isEnabled = true,
            thumbWidthDp = 3,
            thumbHeightDp = 36,
            thumbColorHex = "#66FFFFFF",
            bubbleVisible = false,
            bubbleSizeDp = 0,
            bubbleColorHex = "#000000",
            activationDistanceDp = 400,
        )

        /** Disabled fast scroll. */
        val DISABLED = LibraryFastScrollSpec(
            isEnabled = false,
            thumbWidthDp = 0,
            thumbHeightDp = 0,
            thumbColorHex = "#000000",
            bubbleVisible = false,
            bubbleSizeDp = 0,
            bubbleColorHex = "#000000",
            activationDistanceDp = 0,
        )
    }
}

/**
 * Pull-to-refresh spec.
 *
 * @property isEnabled             True if pull-to-refresh is enabled.
 * @property activationThresholdDp  Pull distance (dp) required to trigger refresh.
 * @property maxPullDistanceDp      Maximum pull distance (resistance applied beyond).
 * @property indicatorVisible       True if a refresh indicator is visible.
 * @property indicatorPosition      Where the indicator appears.
 * @property indicatorColorHex      Indicator color (hex).
 * @property springBackDurationMs   How long the spring-back animation takes.
 */
data class LibraryPullRefreshSpec(
    val isEnabled: Boolean,
    val activationThresholdDp: Int,
    val maxPullDistanceDp: Int,
    val indicatorVisible: Boolean,
    val indicatorPosition: LibraryRefreshIndicatorPosition,
    val indicatorColorHex: String,
    val springBackDurationMs: Long,
) {
    companion object {
        /** Default pull-to-refresh — top indicator, 80dp activation. */
        val DEFAULT = LibraryPullRefreshSpec(
            isEnabled = true,
            activationThresholdDp = 80,
            maxPullDistanceDp = 160,
            indicatorVisible = true,
            indicatorPosition = LibraryRefreshIndicatorPosition.TOP,
            indicatorColorHex = "#FFFFFF",
            springBackDurationMs = 320,
        )

        /** Disabled pull-to-refresh — when refresh is triggered via chip. */
        val DISABLED = LibraryPullRefreshSpec(
            isEnabled = false,
            activationThresholdDp = 0,
            maxPullDistanceDp = 0,
            indicatorVisible = false,
            indicatorPosition = LibraryRefreshIndicatorPosition.TOP,
            indicatorColorHex = "#000000",
            springBackDurationMs = 0,
        )
    }
}

/**
 * Position of the refresh indicator.
 */
enum class LibraryRefreshIndicatorPosition(
    internal val description: String,
) {
    /** Top — indicator appears at the top of the content slot. */
    TOP("Top of content slot."),

    /** Bottom — indicator appears at the bottom (rare). */
    BOTTOM("Bottom of content slot."),

    /** Floating — indicator floats over the content. */
    FLOATING("Floating over content."),
}

/**
 * Fling spec — how scroll momentum decays.
 *
 * @property friction           Friction coefficient (higher = stops faster).
 * @property minFlingVelocityDp Minimum velocity (dp/s) to start a fling.
 * @property maxFlingVelocityDp Maximum velocity (dp/s) — capped.
 * @property settleDurationMs   How long a fling takes to settle (worst case).
 */
data class LibraryFlingSpec(
    val friction: Float,
    val minFlingVelocityDp: Int,
    val maxFlingVelocityDp: Int,
    val settleDurationMs: Long,
) {
    companion object {
        /** Default fling — natural momentum. */
        val DEFAULT = LibraryFlingSpec(
            friction = 1.0f,
            minFlingVelocityDp = 200,
            maxFlingVelocityDp = 8000,
            settleDurationMs = 1200,
        )

        /** Snappy fling — stops quickly (for utilitarian sections). */
        val SNAPPY = LibraryFlingSpec(
            friction = 1.5f,
            minFlingVelocityDp = 300,
            maxFlingVelocityDp = 6000,
            settleDurationMs = 800,
        )

        /** Dreamy fling — long momentum (for mood sections). */
        val DREAMY = LibraryFlingSpec(
            friction = 0.7f,
            minFlingVelocityDp = 150,
            maxFlingVelocityDp = 10000,
            settleDurationMs = 1800,
        )
    }
}

/**
 * Nested scroll spec — how inner (horizontal) and outer (vertical)
 * scrolls cooperate.
 *
 * @property innerScrollConsumesFirst  True if inner strips consume gesture
 *                                      before outer vertical.
 * @property outerTakesOverThresholdDp  Vertical drag distance (dp) at which
 *                                       outer vertical scroll takes over.
 */
data class LibraryNestedScrollSpec(
    val innerScrollConsumesFirst: Boolean,
    val outerTakesOverThresholdDp: Int,
) {
    companion object {
        val DEFAULT = LibraryNestedScrollSpec(
            innerScrollConsumesFirst = true,
            outerTakesOverThresholdDp = 16,
        )
    }
}

/**
 * The Library Scroll Contract.
 *
 * @property mode              Scroll mode.
 * @property overscrollBehavior  Overscroll behavior.
 * @property fastScrollSpec    Fast scroll spec.
 * @property pullRefreshSpec   Pull-to-refresh spec.
 * @property flingSpec         Fling spec.
 * @property nestedScrollSpec  Nested scroll spec.
 * @property isVertical        True if the main scroll axis is vertical.
 */
data class LibraryScrollContract(
    val mode: LibraryScrollMode,
    val overscrollBehavior: LibraryOverscrollBehavior,
    val fastScrollSpec: LibraryFastScrollSpec,
    val pullRefreshSpec: LibraryPullRefreshSpec,
    val flingSpec: LibraryFlingSpec,
    val nestedScrollSpec: LibraryNestedScrollSpec,
    val isVertical: Boolean,
) {
    companion object {
        /** Default scroll — free, stretch overscroll, fast scroll + pull refresh. */
        val DEFAULT = LibraryScrollContract(
            mode = LibraryScrollMode.FREE,
            overscrollBehavior = LibraryOverscrollBehavior.STRETCH,
            fastScrollSpec = LibraryFastScrollSpec.DEFAULT,
            pullRefreshSpec = LibraryPullRefreshSpec.DEFAULT,
            flingSpec = LibraryFlingSpec.DEFAULT,
            nestedScrollSpec = LibraryNestedScrollSpec.DEFAULT,
            isVertical = true,
        )

        /** Locked scroll — for selection mode / fullscreen. */
        val LOCKED = LibraryScrollContract(
            mode = LibraryScrollMode.LOCKED,
            overscrollBehavior = LibraryOverscrollBehavior.NONE,
            fastScrollSpec = LibraryFastScrollSpec.DISABLED,
            pullRefreshSpec = LibraryPullRefreshSpec.DISABLED,
            flingSpec = LibraryFlingSpec.DEFAULT,
            nestedScrollSpec = LibraryNestedScrollSpec.DEFAULT,
            isVertical = true,
        )
    }
}
