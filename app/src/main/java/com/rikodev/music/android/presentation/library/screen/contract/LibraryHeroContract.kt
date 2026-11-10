/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.1 — Library Screen Constitution
 *
 * File 4 / 15 — LibraryHeroContract.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * The Hero is the screen's opening statement. It is the first thing
 * the user sees in the content slot — usually "Continue Listening"
 * or a featured collection. It is the *beginning* of the story
 * (Rule #54).
 *
 * The Hero is NOT just a big card. It is a *cinematic moment*. It
 * sets the tone for the entire Library — its motion tells the user
 * "this app is alive", its artwork tells "this app cares about
 * music", its personality tells "this app knows you".
 *
 * RULES ENFORCED HERE
 * -------------------
 * #28  Motion Explains Hierarchy   — Hero motion is the strongest
 * #29  Motion Explains Hierarchy   — Hero hierarchy = 1.0 (highest)
 * #33  Collection Has Identity     — Hero has its own identity
 * #54  Screen Tells a Story        — Hero is the beginning
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
 * The prominence of the Hero on screen.
 *
 * Drives card size, motion weight, and visual weight.
 */
enum class LibraryHeroProminence(
    internal val description: String,
    internal val cardHeightDp: Int,
    internal val cardWidthFraction: Float,
    internal val hierarchyWeight: Float,
) {
    /** Primary Hero — full-width, tall, strongest motion. */
    PRIMARY(
        description = "Primary — full-width, tall, strongest motion.",
        cardHeightDp = 280,
        cardWidthFraction = 0.92f,
        hierarchyWeight = 1.0f,
    ),

    /** Secondary Hero — narrower, shorter, medium motion. */
    SECONDARY(
        description = "Secondary — narrower, shorter, medium motion.",
        cardHeightDp = 220,
        cardWidthFraction = 0.78f,
        hierarchyWeight = 0.75f,
    ),

    /** Compact Hero — for tablets / landscape. */
    COMPACT(
        description = "Compact — for tablets / landscape.",
        cardHeightDp = 180,
        cardWidthFraction = 0.60f,
        hierarchyWeight = 0.65f,
    ),
}

/**
 * The shape of a Hero card.
 *
 * @property aspectRatioW      Width component of aspect ratio.
 * @property aspectRatioH      Height component of aspect ratio.
 * @property cornerRadiusDp    Corner radius.
 * @property elevationDp       Elevation in dp.
 * @property artworkAspectRatioW  Artwork aspect ratio width.
 * @property artworkAspectRatioH  Artwork aspect ratio height.
 */
data class LibraryHeroCardShape(
    val aspectRatioW: Float,
    val aspectRatioH: Float,
    val cornerRadiusDp: Int,
    val elevationDp: Float,
    val artworkAspectRatioW: Float,
    val artworkAspectRatioH: Float,
) {
    companion object {
        /** Standard hero card — 16:9 with tall artwork. */
        val STANDARD = LibraryHeroCardShape(
            aspectRatioW = 16f,
            aspectRatioH = 9f,
            cornerRadiusDp = 24,
            elevationDp = 8f,
            artworkAspectRatioW = 16f,
            artworkAspectRatioH = 9f,
        )

        /** Cinematic hero card — 21:9, premium feel. */
        val CINEMATIC = LibraryHeroCardShape(
            aspectRatioW = 21f,
            aspectRatioH = 9f,
            cornerRadiusDp = 28,
            elevationDp = 12f,
            artworkAspectRatioW = 21f,
            artworkAspectRatioH = 9f,
        )

        /** Compact hero card — 4:3, for landscape. */
        val COMPACT = LibraryHeroCardShape(
            aspectRatioW = 4f,
            aspectRatioH = 3f,
            cornerRadiusDp = 16,
            elevationDp = 4f,
            artworkAspectRatioW = 4f,
            artworkAspectRatioH = 3f,
        )
    }
}

/**
 * The motion profile for the Hero.
 *
 * Rule #29 — Motion Explains Hierarchy.
 * The Hero has the strongest motion in the screen. Its entrance
 * is slowest and most deliberate; its scroll parallax is deepest.
 *
 * @property entranceDurationMs   Entrance animation duration (ms).
 * @property entranceDelayMs      Delay before entrance begins (ms).
 * @property parallaxFactor       0.0–1.0. How much the hero parallaxes on scroll.
 * @property settleDurationMs     How long the hero takes to settle after scroll.
 * @property dismissOnScrollThresholdDp  Scroll offset at which hero dismisses.
 * @property motionIntensity      0.0–1.0. Overall motion intensity.
 */
data class LibraryHeroMotionProfile(
    val entranceDurationMs: Long,
    val entranceDelayMs: Long,
    val parallaxFactor: Float,
    val settleDurationMs: Long,
    val dismissOnScrollThresholdDp: Int,
    val motionIntensity: Float,
) {
    init {
        require(parallaxFactor in 0f..1f) {
            "parallaxFactor must be in [0, 1], got $parallaxFactor"
        }
        require(motionIntensity in 0f..1f) {
            "motionIntensity must be in [0, 1], got $motionIntensity"
        }
    }

    companion object {
        /** Standard motion — balanced entrance + medium parallax. */
        val STANDARD = LibraryHeroMotionProfile(
            entranceDurationMs = 480,
            entranceDelayMs = 80,
            parallaxFactor = 0.4f,
            settleDurationMs = 240,
            dismissOnScrollThresholdDp = 200,
            motionIntensity = 0.6f,
        )

        /** Cinematic motion — slow entrance + deep parallax. */
        val CINEMATIC = LibraryHeroMotionProfile(
            entranceDurationMs = 720,
            entranceDelayMs = 160,
            parallaxFactor = 0.6f,
            settleDurationMs = 360,
            dismissOnScrollThresholdDp = 240,
            motionIntensity = 0.8f,
        )

        /** Reduced motion — for accessibility. */
        val REDUCED = LibraryHeroMotionProfile(
            entranceDurationMs = 200,
            entranceDelayMs = 0,
            parallaxFactor = 0.0f,
            settleDurationMs = 100,
            dismissOnScrollThresholdDp = 160,
            motionIntensity = 0.2f,
        )
    }
}

/**
 * The Hero's overlay spec — text and gradient over artwork.
 *
 * @property gradientStartOpacity   Gradient opacity at the top.
 * @property gradientEndOpacity     Gradient opacity at the bottom.
 * @property textPosition           Where the title sits on the artwork.
 * @property textColorHex           Text color (hex).
 * @property textShadowEnabled      True if text has a drop shadow.
 */
data class LibraryHeroOverlaySpec(
    val gradientStartOpacity: Float,
    val gradientEndOpacity: Float,
    val textPosition: LibraryHeroTextPosition,
    val textColorHex: String,
    val textShadowEnabled: Boolean,
) {
    init {
        require(gradientStartOpacity in 0f..1f) {
            "gradientStartOpacity must be in [0, 1]"
        }
        require(gradientEndOpacity in 0f..1f) {
            "gradientEndOpacity must be in [0, 1]"
        }
    }

    companion object {
        /** Default overlay — bottom gradient + bottom-left text. */
        val DEFAULT = LibraryHeroOverlaySpec(
            gradientStartOpacity = 0.0f,
            gradientEndOpacity = 0.85f,
            textPosition = LibraryHeroTextPosition.BOTTOM_LEFT,
            textColorHex = "#FFFFFF",
            textShadowEnabled = true,
        )
    }
}

/**
 * Position of text on a Hero card.
 */
enum class LibraryHeroTextPosition(
    internal val description: String,
) {
    /** Text sits at top-left of the artwork. */
    TOP_LEFT("Top-left."),

    /** Text sits at top-center of the artwork. */
    TOP_CENTER("Top-center."),

    /** Text sits at bottom-left of the artwork. */
    BOTTOM_LEFT("Bottom-left."),

    /** Text sits at bottom-center of the artwork. */
    BOTTOM_CENTER("Bottom-center."),

    /** Text sits at center of the artwork. */
    CENTER("Center."),
}

/**
 * The Library Hero Contract.
 *
 * @property prominence         Hero prominence on screen.
 * @property cardShape          Card shape spec.
 * @property motionProfile      Motion profile.
 * @property overlaySpec        Overlay (gradient + text) spec.
 * @property isVisible          True if the Hero is currently visible.
 * @property horizontalMarginDp Horizontal margin around the hero.
 * @property topMarginDp        Margin above the hero.
 * @property bottomMarginDp     Margin below the hero.
 */
data class LibraryHeroContract(
    val prominence: LibraryHeroProminence,
    val cardShape: LibraryHeroCardShape,
    val motionProfile: LibraryHeroMotionProfile,
    val overlaySpec: LibraryHeroOverlaySpec,
    val isVisible: Boolean,
    val horizontalMarginDp: Int,
    val topMarginDp: Int,
    val bottomMarginDp: Int,
) {
    companion object {
        /** Default Hero — primary prominence, standard motion. */
        val DEFAULT = LibraryHeroContract(
            prominence = LibraryHeroProminence.PRIMARY,
            cardShape = LibraryHeroCardShape.STANDARD,
            motionProfile = LibraryHeroMotionProfile.STANDARD,
            overlaySpec = LibraryHeroOverlaySpec.DEFAULT,
            isVisible = true,
            horizontalMarginDp = 16,
            topMarginDp = 8,
            bottomMarginDp = 24,
        )

        /** Cinematic Hero — slow, deep, premium. */
        val CINEMATIC = LibraryHeroContract(
            prominence = LibraryHeroProminence.PRIMARY,
            cardShape = LibraryHeroCardShape.CINEMATIC,
            motionProfile = LibraryHeroMotionProfile.CINEMATIC,
            overlaySpec = LibraryHeroOverlaySpec.DEFAULT,
            isVisible = true,
            horizontalMarginDp = 16,
            topMarginDp = 8,
            bottomMarginDp = 28,
        )

        /** Compact Hero — for landscape / tablets. */
        val COMPACT = LibraryHeroContract(
            prominence = LibraryHeroProminence.COMPACT,
            cardShape = LibraryHeroCardShape.COMPACT,
            motionProfile = LibraryHeroMotionProfile.STANDARD,
            overlaySpec = LibraryHeroOverlaySpec.DEFAULT,
            isVisible = true,
            horizontalMarginDp = 16,
            topMarginDp = 8,
            bottomMarginDp = 16,
        )
    }
}
