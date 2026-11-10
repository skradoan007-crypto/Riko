/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.1 — Library Screen Constitution
 *
 * File 3 / 15 — LibraryDockContract.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * The Library Dock is the screen's anchor layer. It is the mini
 * player that sits at the bottom and tells the user "music is here,
 * always". It is the last slot in the story (Rule #54).
 *
 * The Dock is NOT just a mini player. It is a *breathing presence*.
 * When collapsed, it shows what's playing. When expanded, it shows
 * the queue. When tapped, it morphs into the Now Playing screen.
 * When hidden, it leaves behind a subtle wave — a hint that music
 * is still alive.
 *
 * STATES
 * ------
 *   • COLLAPSED   — peek view, artwork + title + controls
 *   • EXPANDED    — full mini-player with progress + queue peek
 *   • MORPHING    — transitioning to/from Now Playing
 *   • HIDDEN      — off-screen (no music loaded yet)
 *
 * RULES ENFORCED HERE
 * -------------------
 * #29  Motion Explains Hierarchy   — morph motion is hierarchy-aware
 * #33  Collection Has Identity     — dock identity is "now playing"
 * #54  Screen Tells a Story        — dock anchors the user
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
 * The state of the Library Dock.
 */
enum class LibraryDockState(
    internal val description: String,
    internal val heightFraction: Float,
) {
    /** Hidden — no music loaded. The dock is off-screen. */
    HIDDEN(
        description = "Hidden — no music loaded.",
        heightFraction = 0.0f,
    ),

    /** Collapsed — peek view with artwork + title + play/pause. */
    COLLAPSED(
        description = "Collapsed — peek view.",
        heightFraction = 0.55f,
    ),

    /** Expanded — full mini-player with progress + queue peek. */
    EXPANDED(
        description = "Expanded — full mini-player.",
        heightFraction = 1.0f,
    ),

    /** Morphing — transitioning to/from Now Playing. */
    MORPHING(
        description = "Morphing — transitioning to Now Playing.",
        heightFraction = 1.0f,
    ),
}

/**
 * How the Dock is positioned.
 */
enum class LibraryDockPosition(
    internal val description: String,
    internal val floatingMarginDp: Int,
) {
    /** Floating — dock floats above content with margin. */
    FLOATING(
        description = "Floating — above content with margin.",
        floatingMarginDp = 8,
    ),

    /** Edge-attached — dock sits flush at the bottom edge. */
    EDGE_ATTACHED(
        description = "Edge-attached — flush at bottom.",
        floatingMarginDp = 0,
    ),

    /** Inset — dock sits within the content, leaving a small gap. */
    INSET(
        description = "Inset — within content, small gap.",
        floatingMarginDp = 4,
    ),
}

/**
 * The peek rules — when should the Dock be visible vs. hidden.
 *
 * @property showWhenEmpty       True if the dock shows a "ready to play"
 *                               hint when no music is loaded.
 * @property showWhenPlaying     True if the dock shows during playback.
 * @property showWhenPaused      True if the dock shows when paused.
 * @property hideOnScrollDown    True if the dock hides when user scrolls
 *                               down (maximizes content space).
 * @property hideDelayMs         Delay (ms) before hiding after scroll stops.
 * @property revealOnScrollUp    True if the dock reveals on scroll up.
 */
data class LibraryDockPeekRule(
    val showWhenEmpty: Boolean,
    val showWhenPlaying: Boolean,
    val showWhenPaused: Boolean,
    val hideOnScrollDown: Boolean,
    val hideDelayMs: Long,
    val revealOnScrollUp: Boolean,
) {
    companion object {
        /** Default peek rule — dock always present when music exists. */
        val DEFAULT = LibraryDockPeekRule(
            showWhenEmpty = false,
            showWhenPlaying = true,
            showWhenPaused = true,
            hideOnScrollDown = false,
            hideDelayMs = 0L,
            revealOnScrollUp = true,
        )

        /** Scroll-aware peek rule — dock hides on scroll down. */
        val SCROLL_AWARE = LibraryDockPeekRule(
            showWhenEmpty = false,
            showWhenPlaying = true,
            showWhenPaused = true,
            hideOnScrollDown = true,
            hideDelayMs = 1200L,
            revealOnScrollUp = true,
        )
    }
}

/**
 * The glass layer spec for the Dock.
 *
 * The Dock's glass is distinct from the Top Bar's glass — it is
 * typically stronger, because the Dock needs to feel like a solid
 * anchor even when content scrolls underneath it.
 *
 * @property glassKind           Kind of glass effect.
 * @property blurRadiusDp        Blur radius in dp.
 * @property backgroundOpacity   Background opacity (0.0–1.0).
 * @property borderColorHex      Optional border color (hex).
 * @property borderWidthDp       Border width in dp (0 = no border).
 * @property hasShadow           True if the dock casts a shadow.
 */
data class LibraryDockGlassSpec(
    val glassKind: LibraryDockGlassKind,
    val blurRadiusDp: Int,
    val backgroundOpacity: Float,
    val borderColorHex: String?,
    val borderWidthDp: Int,
    val hasShadow: Boolean,
) {
    init {
        require(backgroundOpacity in 0f..1f) {
            "backgroundOpacity must be in [0, 1], got $backgroundOpacity"
        }
    }

    companion object {
        /** Default glass — medium blur, medium opacity, subtle border. */
        val DEFAULT = LibraryDockGlassSpec(
            glassKind = LibraryDockGlassKind.FROSTED,
            blurRadiusDp = 20,
            backgroundOpacity = 0.7f,
            borderColorHex = "#33FFFFFF",
            borderWidthDp = 1,
            hasShadow = true,
        )

        /** Strong glass — heavy blur, high opacity, no border. */
        val STRONG = LibraryDockGlassSpec(
            glassKind = LibraryDockGlassKind.FROSTED,
            blurRadiusDp = 32,
            backgroundOpacity = 0.85f,
            borderColorHex = null,
            borderWidthDp = 0,
            hasShadow = true,
        )

        /** AMOLED glass — minimal blur, pure dark. */
        val AMOLED = LibraryDockGlassSpec(
            glassKind = LibraryDockGlassKind.SOLID,
            blurRadiusDp = 0,
            backgroundOpacity = 0.92f,
            borderColorHex = "#22FFFFFF",
            borderWidthDp = 1,
            hasShadow = false,
        )
    }
}

/**
 * Kind of glass effect on the Dock.
 */
enum class LibraryDockGlassKind(
    internal val description: String,
) {
    /** Frosted — backdrop blur with translucent fill. */
    FROSTED("Frosted glass — backdrop blur with translucent fill."),

    /** Solid — opaque fill, no blur. */
    SOLID("Solid — opaque fill, no blur."),

    /** Gradient — gradient fill, optional blur. */
    GRADIENT("Gradient fill."),

    /** Personality-tinted — color shifts with active song. */
    PERSONALITY_TINTED("Personality-tinted — color follows active song."),
}

/**
 * The Dock's breathing spec — subtle pulsing animation.
 *
 * The Dock "breathes" with the music. When playing, it pulses
 * gently to the beat. When paused, it settles into stillness.
 * This is part of RIKO's "alive" feel.
 *
 * @property isEnabled           True if breathing is enabled.
 * @property intensity           0.0–1.0. How strong the breathing is.
 * @property periodMs            Breath cycle duration in ms.
 * @property pauseWhenIdle       True if breathing pauses when not playing.
 */
data class LibraryDockBreathingSpec(
    val isEnabled: Boolean,
    val intensity: Float,
    val periodMs: Long,
    val pauseWhenIdle: Boolean,
) {
    init {
        require(intensity in 0f..1f) {
            "intensity must be in [0, 1], got $intensity"
        }
    }

    companion object {
        /** Default breathing — subtle, 4-second cycle. */
        val DEFAULT = LibraryDockBreathingSpec(
            isEnabled = true,
            intensity = 0.15f,
            periodMs = 4000L,
            pauseWhenIdle = true,
        )

        /** Energetic breathing — stronger, faster. */
        val ENERGETIC = LibraryDockBreathingSpec(
            isEnabled = true,
            intensity = 0.3f,
            periodMs = 2400L,
            pauseWhenIdle = true,
        )

        /** No breathing — for accessibility (reduce motion). */
        val NONE = LibraryDockBreathingSpec(
            isEnabled = false,
            intensity = 0.0f,
            periodMs = 0L,
            pauseWhenIdle = true,
        )
    }
}

/**
 * The Library Dock Contract.
 *
 * @property state              Current Dock state.
 * @property position           Dock position on screen.
 * @property peekRule           When the dock is visible.
 * @property glassSpec          Glass layer spec.
 * @property breathingSpec      Breathing animation spec.
 * @property cornerRadiusDp     Corner radius for the dock container.
 * @property expandOnTap        True if tapping the dock expands it.
 * @property morphToNowPlaying  True if long-pressing morphs to Now Playing.
 */
data class LibraryDockContract(
    val state: LibraryDockState,
    val position: LibraryDockPosition,
    val peekRule: LibraryDockPeekRule,
    val glassSpec: LibraryDockGlassSpec,
    val breathingSpec: LibraryDockBreathingSpec,
    val cornerRadiusDp: Int,
    val expandOnTap: Boolean,
    val morphToNowPlaying: Boolean,
) {
    companion object {
        /** Default Dock — floating, medium glass, default breathing. */
        val DEFAULT = LibraryDockContract(
            state = LibraryDockState.COLLAPSED,
            position = LibraryDockPosition.FLOATING,
            peekRule = LibraryDockPeekRule.DEFAULT,
            glassSpec = LibraryDockGlassSpec.DEFAULT,
            breathingSpec = LibraryDockBreathingSpec.DEFAULT,
            cornerRadiusDp = 20,
            expandOnTap = true,
            morphToNowPlaying = true,
        )

        /** AMOLED Dock — edge-attached, solid dark, no breathing. */
        val AMOLED = LibraryDockContract(
            state = LibraryDockState.COLLAPSED,
            position = LibraryDockPosition.EDGE_ATTACHED,
            peekRule = LibraryDockPeekRule.DEFAULT,
            glassSpec = LibraryDockGlassSpec.AMOLED,
            breathingSpec = LibraryDockBreathingSpec.NONE,
            cornerRadiusDp = 0,
            expandOnTap = true,
            morphToNowPlaying = true,
        )

        /** Scroll-aware Dock — hides on scroll down. */
        val SCROLL_AWARE = LibraryDockContract(
            state = LibraryDockState.COLLAPSED,
            position = LibraryDockPosition.FLOATING,
            peekRule = LibraryDockPeekRule.SCROLL_AWARE,
            glassSpec = LibraryDockGlassSpec.DEFAULT,
            breathingSpec = LibraryDockBreathingSpec.DEFAULT,
            cornerRadiusDp = 20,
            expandOnTap = true,
            morphToNowPlaying = true,
        )
    }

    /** True if the Dock is currently visible. */
    val isVisible: Boolean get() = state != LibraryDockState.HIDDEN

    /** True if the Dock is currently expanded. */
    val isExpanded: Boolean get() = state == LibraryDockState.EXPANDED

    /** True if the Dock is currently morphing. */
    val isMorphing: Boolean get() = state == LibraryDockState.MORPHING
}
