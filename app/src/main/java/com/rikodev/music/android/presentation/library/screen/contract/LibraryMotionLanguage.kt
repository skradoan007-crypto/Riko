/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.1 — Library Screen Constitution
 *
 * File 13 / 15 — LibraryMotionLanguage.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * The Library Motion Language declares the *motion vocabulary* of
 * the Library screen — durations, easings, spring presets, and
 * motion choreography rules.
 *
 * Rule #29 — Motion Explains Hierarchy.
 * Motion in RIKO is never decorative. Every motion has a *reason*:
 *   • To explain hierarchy (hero enters slower than cards)
 *   • To explain change (items fade when content updates)
 *   • To explain continuity (dock morphs to Now Playing)
 *   • To explain state (loading shimmers, empty settles)
 *
 * This file declares the *vocabulary*. The choreography (when each
 * motion fires) is declared in the individual contracts (Hero,
 * Dock, Section, etc.).
 *
 * RULES ENFORCED HERE
 * -------------------
 * #28  Motion Explains Hierarchy
 * #29  Motion Explains Hierarchy
 * #54  Screen Tells a Story        — motion supports the story
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
 * Motion duration presets.
 *
 * Durations are coarse-grained — the UI maps each to a concrete
 * millisecond value. RIKO's motion is *slow* compared to Material
 * defaults, because RIKO wants motion to be *felt*, not just seen.
 */
enum class LibraryMotionDuration(
    internal val description: String,
    internal val durationMs: Long,
) {
    /** Instant — no motion. */
    INSTANT("Instant — no motion.", 0L),

    /** Quick — for small UI feedback (tap, ripple). */
    QUICK("Quick — small UI feedback.", 120L),

    /** Short — for small state changes (chip toggle). */
    SHORT("Short — small state changes.", 200L),

    /** Medium — for medium state changes (section collapse). */
    MEDIUM("Medium — section collapse.", 320L),

    /** Long — for entrance animations. */
    LONG("Long — entrance animations.", 480L),

    /** Extended — for hero entrance. */
    EXTENDED("Extended — hero entrance.", 720L),

    /** Cinematic — for major transitions (Library → Now Playing). */
    CINEMATIC("Cinematic — major transitions.", 960L),
}

/**
 * Motion easing presets.
 *
 * RIKO prefers *deceleration* easings — fast at the start, slow at
 * the end. This gives motion a "settling" feel, like a physical
 * object coming to rest.
 */
enum class LibraryMotionEasing(
    internal val description: String,
    internal val controlPoints: List<Float>,
) {
    /** Linear — constant velocity. Rarely used. */
    LINEAR(
        description = "Linear — constant velocity.",
        controlPoints = listOf(0f, 0f, 1f, 1f),
    ),

    /** Standard decelerate — fast start, slow end. */
    DECELERATE(
        description = "Decelerate — fast start, slow end.",
        controlPoints = listOf(0.0f, 0.0f, 0.2f, 1.0f),
    ),

    /** Standard accelerate — slow start, fast end. */
    ACCELERATE(
        description = "Accelerate — slow start, fast end.",
        controlPoints = listOf(0.4f, 0.0f, 1.0f, 1.0f),
    ),

    /** Standard — balanced. */
    STANDARD(
        description = "Standard — balanced.",
        controlPoints = listOf(0.2f, 0.0f, 0.0f, 1.0f),
    ),

    /** Emphasized — RIKO's signature easing. */
    EMPHASIZED(
        description = "Emphasized — RIKO signature.",
        controlPoints = listOf(0.2f, 0.0f, 0.0f, 1.0f),
    ),

    /** Gentle — soft, slow. */
    GENTLE(
        description = "Gentle — soft, slow.",
        controlPoints = listOf(0.4f, 0.0f, 0.2f, 1.0f),
    ),

    /** Dreamy — very slow, very soft. */
    DREAMY(
        description = "Dreamy — very slow, very soft.",
        controlPoints = listOf(0.5f, 0.0f, 0.0f, 1.0f),
    ),
}

/**
 * Motion spring preset.
 *
 * Springs are physical — they have damping and stiffness. RIKO
 * uses springs for *interactive* motion (drag, swipe, scroll
 * settle) and easings for *scripted* motion (entrance, exit).
 *
 * @property presetName         Stable preset name.
 * @property dampingRatio       0.0–1.0. Higher = less bounce.
 * @property stiffness          Spring stiffness (N/m).
 */
enum class LibraryMotionSpring(
    internal val presetName: String,
    internal val dampingRatio: Float,
    internal val stiffness: Float,
) {
    /** Snappy — high damping, high stiffness. Stops quickly. */
    SNAPPY(
        presetName = "snappy",
        dampingRatio = 0.85f,
        stiffness = 800f,
    ),

    /** Balanced — medium damping, medium stiffness. */
    BALANCED(
        presetName = "balanced",
        dampingRatio = 0.75f,
        stiffness = 400f,
    ),

    /** Gentle — lower damping, lower stiffness. Soft feel. */
    GENTLE(
        presetName = "gentle",
        dampingRatio = 0.65f,
        stiffness = 250f,
    ),

    /** Dreamy — very low damping, low stiffness. Floaty feel. */
    DREAMY(
        presetName = "dreamy",
        dampingRatio = 0.55f,
        stiffness = 150f,
    ),

    /** Drifting — almost no damping. Bouncy. */
    DRIFTING(
        presetName = "drifting",
        dampingRatio = 0.4f,
        stiffness = 100f,
    ),

    /** Bouncy — explicit bounce. */
    BOUNCY(
        presetName = "bouncy",
        dampingRatio = 0.3f,
        stiffness = 350f,
    ),
}

/**
 * Motion choreography rule — how multiple motions relate.
 *
 * @property description   Choreography description.
 * @property staggerMs     Stagger between sibling motions (ms).
 * @property maxConcurrent  Max concurrent sibling motions.
 */
enum class LibraryMotionChoreography(
    internal val description: String,
    internal val staggerMs: Long,
    internal val maxConcurrent: Int,
) {
    /** Simultaneous — all siblings animate at once. */
    SIMULTANEOUS(
        description = "Simultaneous — all at once.",
        staggerMs = 0L,
        maxConcurrent = 999,
    ),

    /** Staggered — siblings animate with a small delay. */
    STAGGERED(
        description = "Staggered — small delay between siblings.",
        staggerMs = 40L,
        maxConcurrent = 999,
    ),

    /** Cascade — each sibling waits for the previous to finish. */
    CASCADE(
        description = "Cascade — sequential.",
        staggerMs = 0L,
        maxConcurrent = 1,
    ),

    /** Batched — siblings animate in small batches. */
    BATCHED(
        description = "Batched — small batches.",
        staggerMs = 80L,
        maxConcurrent = 3,
    ),
}

/**
 * Motion hierarchy weight — encodes Rule #29.
 *
 * Higher weight = more prominent motion. The Hero has the highest
 * weight; a small chip has the lowest.
 */
enum class LibraryMotionHierarchy(
    internal val description: String,
    internal val weight: Float,
) {
    /** Hero — highest hierarchy. Slowest, most prominent motion. */
    HERO(
        description = "Hero — highest hierarchy.",
        weight = 1.0f,
    ),

    /** Featured section — second-highest. */
    FEATURED(
        description = "Featured section.",
        weight = 0.8f,
    ),

    /** Standard section — medium hierarchy. */
    STANDARD(
        description = "Standard section.",
        weight = 0.6f,
    ),

    /** Compact section — lower hierarchy. */
    COMPACT(
        description = "Compact section.",
        weight = 0.4f,
    ),

    /** Card — individual card level. */
    CARD(
        description = "Card level.",
        weight = 0.3f,
    ),

    /** Chip / Button — small element level. */
    CHIP(
        description = "Chip / Button level.",
        weight = 0.2f,
    ),

    /** Decorative — lowest hierarchy. */
    DECORATIVE(
        description = "Decorative — lowest.",
        weight = 0.1f,
    ),
}

/**
 * The Library Motion Language.
 *
 * @property defaultDuration       Default duration for unspecified motions.
 * @property defaultEasing         Default easing.
 * @property defaultSpring         Default spring.
 * @property defaultChoreography   Default choreography.
 * @property isReduceMotionEnabled True if reduce motion is on (overrides).
 */
data class LibraryMotionLanguage(
    val defaultDuration: LibraryMotionDuration,
    val defaultEasing: LibraryMotionEasing,
    val defaultSpring: LibraryMotionSpring,
    val defaultChoreography: LibraryMotionChoreography,
    val isReduceMotionEnabled: Boolean,
) {
    companion object {
        val DEFAULT = LibraryMotionLanguage(
            defaultDuration = LibraryMotionDuration.MEDIUM,
            defaultEasing = LibraryMotionEasing.EMPHASIZED,
            defaultSpring = LibraryMotionSpring.BALANCED,
            defaultChoreography = LibraryMotionChoreography.STAGGERED,
            isReduceMotionEnabled = false,
        )

        /** Cinematic motion language — slower, more dramatic. */
        val CINEMATIC = LibraryMotionLanguage(
            defaultDuration = LibraryMotionDuration.LONG,
            defaultEasing = LibraryMotionEasing.DREAMY,
            defaultSpring = LibraryMotionSpring.GENTLE,
            defaultChoreography = LibraryMotionChoreography.CASCADE,
            isReduceMotionEnabled = false,
        )

        /** Reduced motion language — for accessibility. */
        val REDUCED = LibraryMotionLanguage(
            defaultDuration = LibraryMotionDuration.QUICK,
            defaultEasing = LibraryMotionEasing.LINEAR,
            defaultSpring = LibraryMotionSpring.SNAPPY,
            defaultChoreography = LibraryMotionChoreography.SIMULTANEOUS,
            isReduceMotionEnabled = true,
        )
    }

    /** Resolve a duration, honoring reduce-motion. */
    fun resolveDuration(requested: LibraryMotionDuration): LibraryMotionDuration =
        if (isReduceMotionEnabled) {
            LibraryMotionDuration.INSTANT
        } else {
            requested
        }

    /** Resolve a spring, honoring reduce-motion. */
    fun resolveSpring(requested: LibraryMotionSpring): LibraryMotionSpring =
        if (isReduceMotionEnabled) {
            LibraryMotionSpring.SNAPPY
        } else {
            requested
        }
}
