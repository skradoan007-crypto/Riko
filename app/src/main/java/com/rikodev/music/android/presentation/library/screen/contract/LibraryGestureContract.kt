/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.1 — Library Screen Constitution
 *
 * File 7 / 15 — LibraryGestureContract.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * The Library Gesture Contract declares *what gestures the user
 * can perform* and *what each gesture triggers*. It is the
 * interaction vocabulary of the Library screen.
 *
 * RIKO's gesture philosophy: every gesture must be *discoverable*
 * (the user can figure it out without a manual) and *forgiving*
 * (small mistakes don't break the experience). Gestures that fail
 * either test belong in a context menu, not as a primary action.
 *
 * GESTURE VOCABULARY
 * ------------------
 *   • Tap          — primary action (open / play)
 *   • Long press   — secondary action (context menu)
 *   • Drag         — reorder / swipe-to-dismiss
 *   • Swipe        — dismiss / navigate
 *   • Pinch        — zoom (grid density)
 *   • Double tap   — favorite / unfavorite
 *
 * RULES ENFORCED HERE
 * -------------------
 * #48  Actions Describe Intent     — gesture → action mapping is explicit
 * #54  Screen Tells a Story        — gestures support the story, not break it
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
 * The kind of gesture.
 */
enum class LibraryGestureKind(
    internal val description: String,
    internal val isDiscrete: Boolean,
) {
    /** Tap — single, brief touch. */
    TAP("Tap — single brief touch.", isDiscrete = true),

    /** Long press — sustained touch. */
    LONG_PRESS("Long press — sustained touch.", isDiscrete = true),

    /** Double tap — two quick taps. */
    DOUBLE_TAP("Double tap — two quick taps.", isDiscrete = true),

    /** Drag — sustained touch with movement. */
    DRAG("Drag — sustained touch with movement.", isDiscrete = false),

    /** Swipe — quick drag that releases. */
    SWIPE("Swipe — quick drag that releases.", isDiscrete = true),

    /** Pinch — two-finger pinch. */
    PINCH("Pinch — two-finger.", isDiscrete = false),

    /** Edge swipe — swipe from a screen edge. */
    EDGE_SWIPE("Edge swipe — from screen edge.", isDiscrete = true),
}

/**
 * Direction of a swipe / drag gesture.
 */
enum class LibraryGestureDirection(
    internal val description: String,
) {
    /** Up — finger moves upward. */
    UP("Up."),

    /** Down — finger moves downward. */
    DOWN("Down."),

    /** Left — finger moves leftward. */
    LEFT("Left."),

    /** Right — finger moves rightward. */
    RIGHT("Right."),
}

/**
 * Tap spec — thresholds for tap detection.
 *
 * @property maxTouchSlopDp       Max movement (dp) before a tap becomes a drag.
 * @property maxDurationMs        Max duration (ms) before a tap becomes a long press.
 * @property maxDoubleTapGapMs    Max gap (ms) between two taps for double-tap.
 */
data class LibraryTapSpec(
    val maxTouchSlopDp: Int,
    val maxDurationMs: Long,
    val maxDoubleTapGapMs: Long,
) {
    companion object {
        val DEFAULT = LibraryTapSpec(
            maxTouchSlopDp = 8,
            maxDurationMs = 200,
            maxDoubleTapGapMs = 280,
        )
    }
}

/**
 * Long press spec.
 *
 * @property minDurationMs        Min duration (ms) for long press detection.
 * @property maxMovementDp        Max movement (dp) allowed during long press.
 * @property hapticFeedbackEnabled  True if haptic feedback fires on long press.
 */
data class LibraryLongPressSpec(
    val minDurationMs: Long,
    val maxMovementDp: Int,
    val hapticFeedbackEnabled: Boolean,
) {
    companion object {
        val DEFAULT = LibraryLongPressSpec(
            minDurationMs = 400,
            maxMovementDp = 16,
            hapticFeedbackEnabled = true,
        )

        /** Shorter long-press — for power users. */
        val QUICK = LibraryLongPressSpec(
            minDurationMs = 280,
            maxMovementDp = 16,
            hapticFeedbackEnabled = true,
        )
    }
}

/**
 * Drag spec.
 *
 * @property minDragDistanceDp    Min distance (dp) before drag starts.
 * @property isReorderEnabled     True if drag reorders items.
 * @property isSwipeToDismissEnabled  True if swipe dismisses items.
 * @property hapticFeedbackOnReorder  True if haptic fires on reorder.
 */
data class LibraryDragSpec(
    val minDragDistanceDp: Int,
    val isReorderEnabled: Boolean,
    val isSwipeToDismissEnabled: Boolean,
    val hapticFeedbackOnReorder: Boolean,
) {
    companion object {
        val DEFAULT = LibraryDragSpec(
            minDragDistanceDp = 16,
            isReorderEnabled = false,
            isSwipeToDismissEnabled = false,
            hapticFeedbackOnReorder = true,
        )

        /** Reorder-enabled drag — for queue / playlist editing. */
        val REORDER = LibraryDragSpec(
            minDragDistanceDp = 16,
            isReorderEnabled = true,
            isSwipeToDismissEnabled = false,
            hapticFeedbackOnReorder = true,
        )
    }
}

/**
 * Swipe spec.
 *
 * @property minSwipeDistanceDp   Min distance (dp) for a swipe to register.
 * @property minVelocityDpPerSec  Min velocity (dp/s) for a swipe.
 * @property maxOffAxisAngleDeg   Max angle (degrees) off the swipe axis.
 * @property swipeDirections      Allowed swipe directions.
 */
data class LibrarySwipeSpec(
    val minSwipeDistanceDp: Int,
    val minVelocityDpPerSec: Int,
    val maxOffAxisAngleDeg: Float,
    val swipeDirections: Set<LibraryGestureDirection>,
) {
    companion object {
        /** Horizontal swipe — left / right only. */
        val HORIZONTAL = LibrarySwipeSpec(
            minSwipeDistanceDp = 48,
            minVelocityDpPerSec = 200,
            maxOffAxisAngleDeg = 30f,
            swipeDirections = setOf(
                LibraryGestureDirection.LEFT,
                LibraryGestureDirection.RIGHT,
            ),
        )

        /** Vertical swipe — up / down only. */
        val VERTICAL = LibrarySwipeSpec(
            minSwipeDistanceDp = 48,
            minVelocityDpPerSec = 200,
            maxOffAxisAngleDeg = 30f,
            swipeDirections = setOf(
                LibraryGestureDirection.UP,
                LibraryGestureDirection.DOWN,
            ),
        )

        /** Omnidirectional swipe. */
        val ANY = LibrarySwipeSpec(
            minSwipeDistanceDp = 48,
            minVelocityDpPerSec = 200,
            maxOffAxisAngleDeg = 90f,
            swipeDirections = setOf(
                LibraryGestureDirection.UP,
                LibraryGestureDirection.DOWN,
                LibraryGestureDirection.LEFT,
                LibraryGestureDirection.RIGHT,
            ),
        )
    }
}

/**
 * A gesture-to-action mapping.
 *
 * Declares what action key a gesture should dispatch.
 *
 * @property gestureKind          Kind of gesture.
 * @property direction            Optional direction (for swipe / drag).
 * @property actionKey            Stable key for the action to dispatch.
 *                                The runtime (B5.2) maps this to a
 *                                LibraryScreenAction.
 * @property isPrimary            True if this is the primary action for
 *                                the target element.
 */
data class LibraryGestureMapping(
    val gestureKind: LibraryGestureKind,
    val direction: LibraryGestureDirection?,
    val actionKey: String,
    val isPrimary: Boolean,
) {
    companion object {
        /** Standard mappings for a card. */
        val CARD_TAP = LibraryGestureMapping(
            gestureKind = LibraryGestureKind.TAP,
            direction = null,
            actionKey = "card.open",
            isPrimary = true,
        )

        val CARD_LONG_PRESS = LibraryGestureMapping(
            gestureKind = LibraryGestureKind.LONG_PRESS,
            direction = null,
            actionKey = "card.contextMenu",
            isPrimary = false,
        )

        val CARD_DOUBLE_TAP = LibraryGestureMapping(
            gestureKind = LibraryGestureKind.DOUBLE_TAP,
            direction = null,
            actionKey = "card.toggleFavorite",
            isPrimary = false,
        )

        val CARD_SWIPE_LEFT = LibraryGestureMapping(
            gestureKind = LibraryGestureKind.SWIPE,
            direction = LibraryGestureDirection.LEFT,
            actionKey = "card.dismiss",
            isPrimary = false,
        )

        val CARD_SWIPE_RIGHT = LibraryGestureMapping(
            gestureKind = LibraryGestureKind.SWIPE,
            direction = LibraryGestureDirection.RIGHT,
            actionKey = "card.addToQueue",
            isPrimary = false,
        )
    }
}

/**
 * The Library Gesture Contract.
 *
 * @property tapSpec             Tap thresholds.
 * @property longPressSpec       Long press thresholds.
 * @property dragSpec            Drag spec.
 * @property swipeSpec           Swipe spec.
 * @property gestureMappings     Gesture-to-action mappings.
 * @property hapticFeedbackGlobal  True if haptics are enabled screen-wide.
 */
data class LibraryGestureContract(
    val tapSpec: LibraryTapSpec,
    val longPressSpec: LibraryLongPressSpec,
    val dragSpec: LibraryDragSpec,
    val swipeSpec: LibrarySwipeSpec,
    val gestureMappings: List<LibraryGestureMapping>,
    val hapticFeedbackGlobal: Boolean,
) {
    companion object {
        val DEFAULT = LibraryGestureContract(
            tapSpec = LibraryTapSpec.DEFAULT,
            longPressSpec = LibraryLongPressSpec.DEFAULT,
            dragSpec = LibraryDragSpec.DEFAULT,
            swipeSpec = LibrarySwipeSpec.HORIZONTAL,
            gestureMappings = listOf(
                LibraryGestureMapping.CARD_TAP,
                LibraryGestureMapping.CARD_LONG_PRESS,
                LibraryGestureMapping.CARD_DOUBLE_TAP,
            ),
            hapticFeedbackGlobal = true,
        )
    }

    /** Look up the gesture mapping for a given gesture kind. */
    fun mappingsFor(kind: LibraryGestureKind): List<LibraryGestureMapping> =
        gestureMappings.filter { it.gestureKind == kind }
}
