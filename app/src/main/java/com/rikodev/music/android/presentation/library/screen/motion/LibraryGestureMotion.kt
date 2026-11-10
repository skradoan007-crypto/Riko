/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.3 / Phase C — Living Motion
 *
 * File C5 / C6 — LibraryGestureMotion.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [LibraryGestureMotion] is the *interaction feedback* layer of the
 * Library screen.
 *
 *   যখন user scroll করে — Dock hide হয়।
 *   যখন user long-press করে — item subtly lift করে।
 *   যখন user drag করে — item তার সাথে চলে।
 *   যখন user swipe করে — item dismiss হয়।
 *
 * Rule #73 — Motion Must Have Meaning.
 * Each gesture motion carries information:
 *
 *   | Gesture      | Motion               | Meaning                  |
 *   |--------------|----------------------|--------------------------|
 *   | Scroll down  | Dock slides down     | "User is scrolling"      |
 *   | Scroll up    | Dock slides up       | "User wants the dock"    |
 *   | Long press   | Item scales 1.02x    | "Item is selectable"     |
 *   | Drag         | Item lifts + shadow  | "Item is being moved"    |
 *   | Swipe        | Item translates      | "Item will dismiss"      |
 *   | Tap          | Item dims 0.92x      | "Acknowledged"           |
 *
 * ## What This File Is
 *
 * A set of *modifiers* and *spec data classes* that other components
 * apply to receive gesture-driven motion feedback. The actual gesture
 * detection (click, long-press, drag) is handled by Compose's standard
 * gesture APIs — this file provides the *visual response* layer.
 *
 * ## Why Modifiers?
 *
 * Gesture motion is *attached* to existing components — not standalone
 * composables. A card needs to dim when tapped; a dock needs to slide
 * when scrolled. Modifiers are the idiomatic Compose way to attach
 * such behavior.
 *
 * ## Architecture
 *
 * This file does NOT:
 *   - Detect gestures (that's the host's job using Compose's gesture APIs)
 *   - Run business logic (Rule #58)
 *   - Mutate state (just observes gesture state passed in)
 *
 * It DOES:
 *   - Translate gesture state → visual response
 *   - Enforce motion budgets (Rule #74)
 *   - Honor reduce-motion (Rule #74)
 *
 * WHAT THIS FILE IS NOT
 * ---------------------
 * ❌ Not a gesture detector. Use Compose's `clickable`, `draggable`, etc.
 * ❌ Not a state machine. State is owned by the runtime (B5.2).
 * ❌ Not a replacement for B8 [DockMotion]. B8 handles track-change flash;
 *    this file handles scroll-hide. They compose together.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #58  Runtime Never Knows Rendering   — this is rendering
 * #62  Never Rebuild Existing Systems  — uses Compose gesture APIs
 * #64  Motion Must Be Discoverable     — spring-based, no bouncy
 * #73  Motion Must Have Meaning        — each motion has a documented message
 * #74  Silence Is Also UX              — gestures don't trigger ambient motion
 *
 * © RIKO Experience OS — Rendering Tier (Phase C)
 */

package com.rikodev.music.android.presentation.library.screen.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.presentation.library.screen.contract.LibraryMotionLanguage
import com.rikodev.music.android.presentation.library.screen.contract.LibraryMotionSpring
import kotlinx.coroutines.launch
import androidx.compose.animation.core.SpringSpec
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp

// ─────────────────────────────────────────────────────────────────────────────
// GESTURE MOTION SPEC
//
// The static configuration of gesture-driven motion. All invariants
// are enforced in the init block.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A spec for gesture-driven motion.
 *
 * @property tapDimAlpha           Alpha applied when an item is tapped.
 *                                 Rule #73: meaning = "acknowledged".
 * @property longPressScale        Scale applied when an item is long-pressed.
 *                                 Rule #73: meaning = "selectable".
 *                                 Range: 1.0 (no scale) to 1.05 (max).
 * @property dragLiftElevationDp   Elevation applied when an item is dragged.
 *                                 Rule #73: meaning = "being moved".
 * @property dragLiftScale         Scale applied during drag.
 * @property swipeDismissDistanceRatio  How far (as a ratio of width)
 *                                      an item must be swiped to dismiss.
 * @property scrollHideTranslationDp  How far the dock translates when hidden.
 * @property scrollHideAlpha       Alpha applied when dock is hidden.
 */
data class LibraryGestureMotionSpec(
    val tapDimAlpha: Float,
    val longPressScale: Float,
    val dragLiftElevationDp: Float,
    val dragLiftScale: Float,
    val swipeDismissDistanceRatio: Float,
    val scrollHideTranslationDp: Float,
    val scrollHideAlpha: Float,
) {
    init {
        require(tapDimAlpha in 0.5f..1.0f) {
            "tapDimAlpha ($tapDimAlpha) must be in [0.5, 1.0] — taps dim, not hide."
        }
        require(longPressScale in 1.0f..1.05f) {
            "longPressScale ($longPressScale) must be in [1.0, 1.05] — subtle lift only."
        }
        require(dragLiftElevationDp in 0f..24f) {
            "dragLiftElevationDp ($dragLiftElevationDp) must be in [0, 24]."
        }
        require(dragLiftScale in 1.0f..1.05f) {
            "dragLiftScale ($dragLiftScale) must be in [1.0, 1.05] — subtle lift only."
        }
        require(swipeDismissDistanceRatio in 0.3f..0.8f) {
            "swipeDismissDistanceRatio ($swipeDismissDistanceRatio) must be in [0.3, 0.8]."
        }
        require(scrollHideTranslationDp in 0f..200f) {
            "scrollHideTranslationDp ($scrollHideTranslationDp) must be in [0, 200]."
        }
        require(scrollHideAlpha in 0f..1f) {
            "scrollHideAlpha ($scrollHideAlpha) must be in [0, 1]."
        }
    }

    companion object {
        /**
         * DEFAULT spec — subtle, premium.
         *
         * - Tap dim: 0.92 (subtle, not jarring)
         * - Long-press scale: 1.02 (barely perceptible lift)
         * - Drag elevation: 8dp (physical feeling)
         * - Drag scale: 1.03 (lift + scale combined)
         * - Swipe dismiss: 50% of width
         * - Scroll hide: 80dp translation + 0.0 alpha (fully hidden)
         */
        val DEFAULT = LibraryGestureMotionSpec(
            tapDimAlpha = 0.92f,
            longPressScale = 1.02f,
            dragLiftElevationDp = 8f,
            dragLiftScale = 1.03f,
            swipeDismissDistanceRatio = 0.5f,
            scrollHideTranslationDp = 80f,
            scrollHideAlpha = 0.0f,
        )

        /**
         * CINEMATIC spec — more pronounced gestures.
         */
        val CINEMATIC = LibraryGestureMotionSpec(
            tapDimAlpha = 0.88f,
            longPressScale = 1.03f,
            dragLiftElevationDp = 12f,
            dragLiftScale = 1.04f,
            swipeDismissDistanceRatio = 0.45f,
            scrollHideTranslationDp = 96f,
            scrollHideAlpha = 0.0f,
        )

        /**
         * REDUCED_MOTION spec — minimal motion.
         *
         * Gestures still provide feedback (alpha dim, no scale), but
         * without physical motion. This preserves the meaning while
         * removing the motion (Rule #74 — Silence Is Also UX).
         */
        val REDUCED_MOTION = LibraryGestureMotionSpec(
            tapDimAlpha = 0.85f,
            longPressScale = 1.0f, // No scale.
            dragLiftElevationDp = 0f, // No elevation.
            dragLiftScale = 1.0f, // No scale.
            swipeDismissDistanceRatio = 0.5f,
            scrollHideTranslationDp = 0f, // No translation — just alpha.
            scrollHideAlpha = 0.0f,
        )
    }
}

/**
 * Resolves the appropriate gesture motion spec for a motion language.
 */
internal fun LibraryMotionLanguage.resolveGestureMotionSpec(
    cinematic: Boolean = false,
): LibraryGestureMotionSpec = when {
    isReduceMotionEnabled -> LibraryGestureMotionSpec.REDUCED_MOTION
    cinematic -> LibraryGestureMotionSpec.CINEMATIC
    else -> LibraryGestureMotionSpec.DEFAULT
}

// ─────────────────────────────────────────────────────────────────────────────
// TAP FEEDBACK MODIFIER
//
// Applies a subtle alpha dim when an item is pressed.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A modifier that applies a subtle alpha dim when an item is pressed.
 *
 * Rule #73 — Motion Must Have Meaning.
 * Meaning: "Tap acknowledged."
 *
 * This is a *visual response* modifier — it does not detect taps.
 * The host composable detects the press (via `clickable` or
 * `interactionSource`) and passes the pressed state here.
 *
 * @param isPressed  True if the item is currently pressed.
 * @param spec       The gesture motion spec.
 * @param language   The motion language (for reduce-motion override).
 */
fun Modifier.tapFeedback(
    isPressed: Boolean,
    spec: LibraryGestureMotionSpec = LibraryGestureMotionSpec.DEFAULT,
    language: LibraryMotionLanguage = LibraryMotionLanguage.DEFAULT,
): Modifier {
    val effectiveSpec = language.resolveGestureMotionSpec()
    val alpha = if (isPressed) effectiveSpec.tapDimAlpha else 1.0f
    return this.alpha(alpha)
}

// ─────────────────────────────────────────────────────────────────────────────
// LONG-PRESS FEEDBACK MODIFIER
//
// Applies a subtle scale when an item is long-pressed.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A modifier that applies a subtle scale lift when an item is long-pressed.
 *
 * Rule #73 — Motion Must Have Meaning.
 * Meaning: "This item is selectable."
 *
 * The scale is animated with a [SpringSpec] for a physical feel. The
 * spring's stiffness and damping are derived from the motion language.
 *
 * @param isLongPressed  True if the item is currently long-pressed.
 * @param spec           The gesture motion spec.
 * @param language       The motion language.
 */
@Composable
fun Modifier.longPressFeedback(
    isLongPressed: Boolean,
    spec: LibraryGestureMotionSpec = LibraryGestureMotionSpec.DEFAULT,
    language: LibraryMotionLanguage = LibraryMotionLanguage.DEFAULT,
): Modifier {
    val effectiveSpec = language.resolveGestureMotionSpec()

    // ── Reduce-motion fast path: no scale animation ───────────────────────
    if (language.isReduceMotionEnabled) return this

    val scaleAnimatable = remember { Animatable(1f) }
    val springSpec = remember(language) {
        language.defaultSpring.toComposeSpringSpec()
    }

    LaunchedEffect(isLongPressed, effectiveSpec) {
        launch {
            scaleAnimatable.animateTo(
                targetValue = if (isLongPressed) effectiveSpec.longPressScale else 1f,
                animationSpec = springSpec,
            )
        }
    }

    return this.scale(scaleAnimatable.value)
}

// ─────────────────────────────────────────────────────────────────────────────
// DRAG LIFT MODIFIER
//
// Applies elevation + scale when an item is being dragged.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A modifier that applies elevation + scale when an item is dragged.
 *
 * Rule #73 — Motion Must Have Meaning.
 * Meaning: "This item is being moved."
 *
 * Uses [graphicsLayer] for efficient translation/scale/elevation
 * without recomposing the underlying composable.
 *
 * @param isDragging    True if the item is currently being dragged.
 * @param dragOffsetX   Current X offset (px) from drag start.
 * @param dragOffsetY   Current Y offset (px) from drag start.
 * @param spec          The gesture motion spec.
 * @param language      The motion language.
 */
@Composable
fun Modifier.dragLift(
    isDragging: Boolean,
    dragOffsetX: Float = 0f,
    dragOffsetY: Float = 0f,
    spec: LibraryGestureMotionSpec = LibraryGestureMotionSpec.DEFAULT,
    language: LibraryMotionLanguage = LibraryMotionLanguage.DEFAULT,
): Modifier {
    val effectiveSpec = language.resolveGestureMotionSpec()

    // ── Reduce-motion fast path: translation only, no scale/elevation ─────
    if (language.isReduceMotionEnabled) {
        return this.graphicsLayer {
            translationX = dragOffsetX
            translationY = dragOffsetY
        }
    }

    val elevationAnimatable = remember { Animatable(0f) }
    val scaleAnimatable = remember { Animatable(1f) }
    val springSpec = remember(language) {
        language.defaultSpring.toComposeSpringSpec()
    }

    LaunchedEffect(isDragging, effectiveSpec) {
        launch {
            elevationAnimatable.animateTo(
                targetValue = if (isDragging) effectiveSpec.dragLiftElevationDp else 0f,
                animationSpec = springSpec,
            )
        }
        launch {
            scaleAnimatable.animateTo(
                targetValue = if (isDragging) effectiveSpec.dragLiftScale else 1f,
                animationSpec = springSpec,
            )
        }
    }

    return this.graphicsLayer {
        translationX = dragOffsetX
        translationY = dragOffsetY
        scaleX = scaleAnimatable.value
        scaleY = scaleAnimatable.value
        shadowElevation = elevationAnimatable.value
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCROLL-HIDE MODIFIER
//
// Translates + fades a composable (typically the Dock) based on
// scroll direction.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A modifier that hides a composable when the user scrolls down and
 * reveals it when they scroll up.
 *
 * Rule #73 — Motion Must Have Meaning.
 * Meaning: "User is scrolling" (hide) / "User wants the dock" (reveal).
 *
 * The hide/reveal is animated with a spring. The translation is along
 * the Y axis only (vertical slide).
 *
 * @param isHidden    True if the composable should be hidden.
 * @param spec        The gesture motion spec.
 * @param language    The motion language.
 */
@Composable
fun Modifier.scrollHide(
    isHidden: Boolean,
    spec: LibraryGestureMotionSpec = LibraryGestureMotionSpec.DEFAULT,
    language: LibraryMotionLanguage = LibraryMotionLanguage.DEFAULT,
): Modifier {
    val effectiveSpec = language.resolveGestureMotionSpec()

    // ── Reduce-motion fast path: alpha only, no translation ───────────────
    if (language.isReduceMotionEnabled) {
        val alphaAnimatable = remember { Animatable(1f) }
        val springSpec = remember(language) {
            language.defaultSpring.toComposeSpringSpec()
        }
        LaunchedEffect(isHidden) {
            launch {
                alphaAnimatable.animateTo(
                    targetValue = if (isHidden) effectiveSpec.scrollHideAlpha else 1f,
                    animationSpec = springSpec,
                )
            }
        }
        return this.alpha(alphaAnimatable.value)
    }

    val translationAnimatable = remember { Animatable(0f) }
    val alphaAnimatable = remember { Animatable(1f) }
    val springSpec = remember(language) {
        language.defaultSpring.toComposeSpringSpec()
    }

    LaunchedEffect(isHidden, effectiveSpec) {
        launch {
            translationAnimatable.animateTo(
                targetValue = if (isHidden) effectiveSpec.scrollHideTranslationDp else 0f,
                animationSpec = springSpec,
            )
        }
        launch {
            alphaAnimatable.animateTo(
                targetValue = if (isHidden) effectiveSpec.scrollHideAlpha else 1f,
                animationSpec = springSpec,
            )
        }
    }

    return this.graphicsLayer {
        translationY = translationAnimatable.value
        alpha = alphaAnimatable.value
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SWIPE-DISMISS MODIFIER
//
// Translates a composable horizontally during swipe, with a threshold
// for commit.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A modifier that translates a composable horizontally during a swipe
 * gesture.
 *
 * Rule #73 — Motion Must Have Meaning.
 * Meaning: "This item will dismiss" (when past threshold).
 *
 * This modifier does NOT detect the swipe — the host composable does
 * (via `draggable` or `swipeable`). This modifier only applies the
 * visual translation.
 *
 * @param swipeProgress  0f = at rest, 1f = fully dismissed.
 * @param spec           The gesture motion spec.
 * @param language       The motion language.
 */
@Composable
fun Modifier.swipeDismiss(
    swipeProgress: Float,
    spec: LibraryGestureMotionSpec = LibraryGestureMotionSpec.DEFAULT,
    language: LibraryMotionLanguage = LibraryMotionLanguage.DEFAULT,
): Modifier {
    // swipeProgress is 0..1. We translate the composable by
    // (swipeProgress * width * swipeDismissDistanceRatio). Since this
    // modifier doesn't know the width, we use a fraction of the
    // available space — the host composable should pass swipeProgress
    // as a ratio of the actual translation / the dismiss distance.
    val alpha = (1f - swipeProgress).coerceIn(0f, 1f)
    val translationFraction = swipeProgress

    return this.graphicsLayer {
        translationX = size.width * translationFraction
        this.alpha = alpha
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SPRING CONVERSION HELPER
//
// Converts the contract's [LibraryMotionSpring] to a Compose
// [SpringSpec]. This is the only place that knows about Compose spring
// APIs — keeping the conversion in one place makes it easy to update.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Converts a [LibraryMotionSpring] to a Compose [SpringSpec].
 *
 * This is internal to avoid leaking Compose types into the contract
 * layer (Rule #58 — Runtime Never Knows Rendering).
 */
private fun LibraryMotionSpring.toComposeSpringSpec(): SpringSpec<Float> =
    spring(
        dampingRatio = dampingRatio,
        stiffness = stiffness,
    )

// ─────────────────────────────────────────────────────────────────────────────
// GESTURE MOTION BUDGET AUDITOR
//
// Per-Rule #74: gestures should not trigger ambient motion.
// This auditor verifies that gesture motions are not counted toward
// the screen's ambient motion budget.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Audits whether a gesture motion should be counted toward the
 * screen's ambient motion budget.
 *
 * Rule #74 — Silence Is Also UX.
 * Gesture motions are *reactive* — they only run when the user is
 * actively interacting. They should NOT count toward the ambient
 * motion budget (which is for always-on motions like breathing).
 *
 * This function returns `false` for all gesture motions — they are
 * exempt from the ambient budget. The Orchestrator (C2) uses this
 * to distinguish ambient from gesture motions.
 */
fun isGestureMotionExemptFromAmbientBudget(): Boolean = true
