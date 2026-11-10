/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.1 — Library Screen Constitution
 *
 * File 9 / 15 — LibraryAccessibilityContract.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * The Library Accessibility Contract declares the screen-level
 * accessibility structure — landmarks, focus order, live regions,
 * and announcement policies.
 *
 * RIKO is an audio-first product. For many users, accessibility is
 * not a "nice to have" — it is the difference between usable and
 * unusable. This contract ensures accessibility is declared, not
 * improvised.
 *
 * WHAT THIS FILE IS NOT
 * ---------------------
 * This file does NOT specify TalkBack / VoiceOver API calls. It
 * declares *intent* — the UI layer (B5.3) maps each intent to the
 * platform's accessibility API.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #44  Stable Key Never Changes    — focus keys are stable strings
 * #54  Screen Tells a Story        — focus order follows the story
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
 * A landmark on the Library screen.
 *
 * Landmarks are large, navigable regions. Screen reader users jump
 * between landmarks to navigate quickly.
 *
 * @property kind             Landmark kind.
 * @property label            Spoken label (already localized).
 * @property focusKey         Stable key for focus. Rule #44.
 * @property focusOrderHint   Coarse order (lower = earlier).
 */
data class LibraryAccessibilityLandmark(
    val kind: LibraryAccessibilityLandmarkKind,
    val label: String,
    val focusKey: String,
    val focusOrderHint: Int,
) {
    companion object {
        val TOP_BAR = LibraryAccessibilityLandmark(
            kind = LibraryAccessibilityLandmarkKind.BANNER,
            label = "Library top bar",
            focusKey = "landmark.topBar",
            focusOrderHint = 0,
        )

        val CONTENT = LibraryAccessibilityLandmark(
            kind = LibraryAccessibilityLandmarkKind.MAIN,
            label = "Library content",
            focusKey = "landmark.content",
            focusOrderHint = 10,
        )

        val DOCK = LibraryAccessibilityLandmark(
            kind = LibraryAccessibilityLandmarkKind.CONTENT_INFO,
            label = "Now playing",
            focusKey = "landmark.dock",
            focusOrderHint = 100,
        )
    }
}

/**
 * Kind of accessibility landmark.
 */
enum class LibraryAccessibilityLandmarkKind(
    internal val description: String,
) {
    /** Banner — top of screen, contains title / chips. */
    BANNER("Banner — top of screen."),

    /** Main — primary content. */
    MAIN("Main — primary content."),

    /** Navigation — navigation drawer / tabs. */
    NAVIGATION("Navigation region."),

    /** Content info — supplementary (Dock, etc.). */
    CONTENT_INFO("Content info — supplementary."),

    /** Complementary — sidebar / related content. */
    COMPLEMENTARY("Complementary — sidebar."),

    /** Search — search region. */
    SEARCH("Search region."),
}

/**
 * Focus order rule for the Library screen.
 *
 * @property orderedFocusKeys  Stable keys in focus order, top to bottom.
 * @property isLoopEnabled     True if focus wraps from end to start.
 * @property defaultFocusKey   Key to focus when the screen first appears.
 */
data class LibraryAccessibilityFocusOrder(
    val orderedFocusKeys: List<String>,
    val isLoopEnabled: Boolean,
    val defaultFocusKey: String,
) {
    companion object {
        val DEFAULT = LibraryAccessibilityFocusOrder(
            orderedFocusKeys = listOf(
                "landmark.topBar",
                "section.hero",
                "section.smartCollections",
                "section.albums",
                "section.artists",
                "section.folders",
                "landmark.dock",
            ),
            isLoopEnabled = false,
            defaultFocusKey = "section.hero",
        )
    }
}

/**
 * Live region spec — for announcing state changes.
 *
 * @property regionKey         Stable key for the region.
 * @property politeness         Politeness level.
 * @property announcements     What kinds of changes to announce.
 */
data class LibraryAccessibilityLiveRegion(
    val regionKey: String,
    val politeness: LibraryAccessibilityPoliteness,
    val announcements: Set<LibraryAccessibilityAnnouncement>,
) {
    companion object {
        /** Dock live region — announces playback state changes. */
        val DOCK = LibraryAccessibilityLiveRegion(
            regionKey = "liveRegion.dock",
            politeness = LibraryAccessibilityPoliteness.POLITE,
            announcements = setOf(
                LibraryAccessibilityAnnouncement.PLAYBACK_STATE,
                LibraryAccessibilityAnnouncement.TRACK_CHANGE,
            ),
        )

        /** Library state live region — announces loading / ready. */
        val LIBRARY_STATE = LibraryAccessibilityLiveRegion(
            regionKey = "liveRegion.libraryState",
            politeness = LibraryAccessibilityPoliteness.POLITE,
            announcements = setOf(
                LibraryAccessibilityAnnouncement.LOADING_COMPLETE,
                LibraryAccessibilityAnnouncement.REFRESH_COMPLETE,
                LibraryAccessibilityAnnouncement.ERROR,
            ),
        )
    }
}

/**
 * Politeness level for a live region.
 */
enum class LibraryAccessibilityPoliteness(
    internal val description: String,
) {
    /** Off — no announcements. */
    OFF("Off — no announcements."),

    /** Polite — announce when the user pauses. */
    POLITE("Polite — announce on pause."),

    /** Assertive — interrupt to announce. */
    ASSERTIVE("Assertive — interrupt."),
}

/**
 * Kinds of state changes that can be announced.
 */
enum class LibraryAccessibilityAnnouncement(
    internal val description: String,
) {
    /** Playback state changed (play / pause). */
    PLAYBACK_STATE("Playback state changed."),

    /** Current track changed. */
    TRACK_CHANGE("Track changed."),

    /** Library finished loading. */
    LOADING_COMPLETE("Library finished loading."),

    /** Refresh finished. */
    REFRESH_COMPLETE("Refresh finished."),

    /** An error occurred. */
    ERROR("Error occurred."),

    /** Selection mode entered / exited. */
    SELECTION_MODE("Selection mode changed."),

    /** Item count changed. */
    ITEM_COUNT("Item count changed."),
}

/**
 * Touch exploration spec — for users who rely on touch exploration.
 *
 * @property isTouchExplorationEnabled  True if touch exploration is on.
 * @property hapticFeedbackOnHover       True if haptic fires on hover.
 * @property speechFeedbackOnHover       True if speech fires on hover.
 * @property doubleTapToActivate         True if double-tap (not tap) activates.
 */
data class LibraryAccessibilityTouchExploration(
    val isTouchExplorationEnabled: Boolean,
    val hapticFeedbackOnHover: Boolean,
    val speechFeedbackOnHover: Boolean,
    val doubleTapToActivate: Boolean,
) {
    companion object {
        val DEFAULT = LibraryAccessibilityTouchExploration(
            isTouchExplorationEnabled = true,
            hapticFeedbackOnHover = true,
            speechFeedbackOnHover = true,
            doubleTapToActivate = true,
        )
    }
}

/**
 * The Library Accessibility Contract.
 *
 * @property landmarks         Screen landmarks.
 * @property focusOrder        Focus order rule.
 * @property liveRegions       Live regions.
 * @property touchExploration  Touch exploration spec.
 * @property isReduceMotionEnabled  True if reduce motion is on.
 * @property isHighContrastEnabled  True if high contrast is on.
 * @property isLargeTextEnabled     True if large text is on.
 */
data class LibraryAccessibilityContract(
    val landmarks: List<LibraryAccessibilityLandmark>,
    val focusOrder: LibraryAccessibilityFocusOrder,
    val liveRegions: List<LibraryAccessibilityLiveRegion>,
    val touchExploration: LibraryAccessibilityTouchExploration,
    val isReduceMotionEnabled: Boolean,
    val isHighContrastEnabled: Boolean,
    val isLargeTextEnabled: Boolean,
) {
    companion object {
        val DEFAULT = LibraryAccessibilityContract(
            landmarks = listOf(
                LibraryAccessibilityLandmark.TOP_BAR,
                LibraryAccessibilityLandmark.CONTENT,
                LibraryAccessibilityLandmark.DOCK,
            ),
            focusOrder = LibraryAccessibilityFocusOrder.DEFAULT,
            liveRegions = listOf(
                LibraryAccessibilityLiveRegion.DOCK,
                LibraryAccessibilityLiveRegion.LIBRARY_STATE,
            ),
            touchExploration = LibraryAccessibilityTouchExploration.DEFAULT,
            isReduceMotionEnabled = false,
            isHighContrastEnabled = false,
            isLargeTextEnabled = false,
        )

        /** Reduced-motion accessibility contract. */
        val REDUCED_MOTION = DEFAULT.copy(
            isReduceMotionEnabled = true,
        )

        /** High-contrast accessibility contract. */
        val HIGH_CONTRAST = DEFAULT.copy(
            isHighContrastEnabled = true,
        )
    }

    /** Look up a landmark by its focus key. */
    fun landmarkFor(focusKey: String): LibraryAccessibilityLandmark? =
        landmarks.firstOrNull { it.focusKey == focusKey }

    /** Look up a live region by its key. */
    fun liveRegionFor(regionKey: String): LibraryAccessibilityLiveRegion? =
        liveRegions.firstOrNull { it.regionKey == regionKey }
}
