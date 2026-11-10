/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B4 — Library Rendering Runtime
 *
 * File 5 / 13 — RenderMotion.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [RenderMotion] is the *declarative* motion contract for a section.
 *
 * RIKO's Rule #29 says "Motion Explains Hierarchy". This file is
 * the operationalization of that rule — every section carries an
 * explicit motion spec, and the spec is derived from the section's
 * hierarchy position, not from the renderer's whim.
 *
 * WHAT THIS FILE IS NOT
 * ---------------------
 * This file is NOT a tween / animation library. It does not specify
 * spring constants, easing curves, or duration in milliseconds.
 * Those are *implementation details* owned by the Compose layer (B5+).
 *
 * This file specifies *intent* — "enter from below", "settle gently",
 * "explain that this section is more important than the next".
 *
 * The UI translates each [RenderMotion] variant into concrete
 * animation parameters at draw time. The benefit: motion specs
 * travel with the snapshot, are testable, and survive a Compose
 * rewrite without change.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #29  Motion Explains Hierarchy   — hierarchy encoded via motion weight
 * #28  Motion Explains Hierarchy   — section-level motion is explicit
 *
 * RELATIONSHIP TO CONTRACTS
 * -------------------------
 * Sources:
 *   • [AnimationCadence]    (B1 — CollectionPersonality.kt)
 *   • [CollectionPersonality]   (B1 — drives cadence default)
 *   • Section ordinal / position (B2 — runtime)
 *
 * Outputs:
 *   • [RenderMotion]            (B4 — this file)
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Android, no Compose, no Coroutine, no Flow. Pure Kotlin data.
 *
 * © RIKO Experience OS — Rendering Tier
 */

package com.rikodev.music.android.presentation.library.rendering.motion

/**
 * Declarative motion spec for a section.
 *
 * The UI consumes this and translates it to concrete animations.
 * The renderer never says "use spring with damping 0.7" — it says
 * "this section settles gently because it's reflective".
 *
 * @property entrance       How the section enters the viewport.
 * @property scrollBehavior How the section reacts to scroll.
 * @property itemEntrance   How items within the section enter.
 * @property refreshBehavior How the section reacts to a refresh.
 * @property cadence        Coarse cadence — drives duration / damping.
 * @property hierarchyWeight  0.0–1.0. Higher = more prominent motion.
 *                            Encodes hierarchy. Rule #29.
 * @property priority       Lower numbers animate first (stagger).
 */
data class RenderMotion(
    val entrance: MotionEntrance,
    val scrollBehavior: MotionScrollBehavior,
    val itemEntrance: MotionItemEntrance,
    val refreshBehavior: MotionRefreshBehavior,
    val cadence: MotionCadence,
    val hierarchyWeight: Float,
    val priority: Int,
) {
    companion object {
        /**
         * Default motion for a standard section — gentle entrance,
         * stable scroll, snappy item entrance.
         */
        val DEFAULT = RenderMotion(
            entrance = MotionEntrance.FADE_IN_UP,
            scrollBehavior = MotionScrollBehavior.PARALLAX_SUBTLE,
            itemEntrance = MotionItemEntrance.STAGGERED,
            refreshBehavior = MotionRefreshBehavior.SUBTLE_PULSE,
            cadence = MotionCadence.BALANCED,
            hierarchyWeight = 0.5f,
            priority = 10,
        )

        /**
         * Motion for the hero / featured section — bolder, slower,
         * more deliberate. Higher hierarchy weight.
         */
        val HERO = RenderMotion(
            entrance = MotionEntrance.FADE_IN_SCALE,
            scrollBehavior = MotionScrollBehavior.PARALLAX_STRONG,
            itemEntrance = MotionItemEntrance.CASCADE,
            refreshBehavior = MotionRefreshBehavior.CROSSFADE,
            cadence = MotionCadence.GENTLE,
            hierarchyWeight = 1.0f,
            priority = 0,
        )

        /**
         * Motion for an empty-state section — quiet, almost still.
         * Empty states should not announce themselves loudly.
         */
        val EMPTY_QUIET = RenderMotion(
            entrance = MotionEntrance.FADE_IN_ONLY,
            scrollBehavior = MotionScrollBehavior.STATIC,
            itemEntrance = MotionItemEntrance.NONE,
            refreshBehavior = MotionRefreshBehavior.NONE,
            cadence = MotionCadence.DREAMY,
            hierarchyWeight = 0.2f,
            priority = 20,
        )

        /**
         * Motion for a loading / skeleton section — subtle shimmer,
         * no entrance, no item motion.
         */
        val LOADING_SHIMMER = RenderMotion(
            entrance = MotionEntrance.NONE,
            scrollBehavior = MotionScrollBehavior.STATIC,
            itemEntrance = MotionItemEntrance.NONE,
            refreshBehavior = MotionRefreshBehavior.SHIMMER,
            cadence = MotionCadence.BALANCED,
            hierarchyWeight = 0.3f,
            priority = 5,
        )
    }
}

/**
 * How a section enters the viewport on first render.
 */
enum class MotionEntrance(internal val description: String) {
    /** No entrance motion — section is just there. */
    NONE("No entrance motion."),

    /** Simple fade in — used for low-hierarchy sections. */
    FADE_IN_ONLY("Fade in only."),

    /** Fade in + slide up — standard entrance. */
    FADE_IN_UP("Fade in and slide up."),

    /** Fade in + scale from 0.96 to 1.0 — premium feel. */
    FADE_IN_SCALE("Fade in with a slight scale-up."),

    /** Slide in from the right — used for newly-added sections. */
    SLIDE_IN_RIGHT("Slide in from the right."),

    /** Slide in from below — used for contextually-revealed sections. */
    SLIDE_IN_BOTTOM("Slide in from below."),
}

/**
 * How a section reacts to scroll.
 */
enum class MotionScrollBehavior(internal val description: String) {
    /** Static — no reaction. */
    STATIC("No scroll reaction."),

    /** Subtle parallax — content moves slightly slower than scroll. */
    PARALLAX_SUBTLE("Subtle parallax."),

    /** Strong parallax — content moves noticeably slower than scroll. */
    PARALLAX_STRONG("Strong parallax."),

    /** Section header sticks to the top while items scroll. */
    STICKY_HEADER("Sticky header behavior."),

    /** Section fades out as it scrolls off-screen. */
    FADE_ON_EXIT("Fades out on scroll exit."),

    /** Section compresses on scroll (e.g. hero card shrinks). */
    COMPRESS_ON_SCROLL("Compresses on scroll."),
}

/**
 * How items within a section enter.
 */
enum class MotionItemEntrance(internal val description: String) {
    /** No item motion — items appear with the section. */
    NONE("No item motion."),

    /** Items stagger in left-to-right or top-to-bottom. */
    STAGGERED("Staggered entrance."),

    /** Items cascade in — each item starts as the previous finishes. */
    CASCADE("Cascade entrance."),

    /** Items appear all at once after a brief delay. */
    BATCH("Batch entrance."),
}

/**
 * How a section reacts to a refresh trigger.
 */
enum class MotionRefreshBehavior(internal val description: String) {
    /** No refresh motion. */
    NONE("No refresh motion."),

    /** Subtle pulse — section briefly dims and brightens. */
    SUBTLE_PULSE("Subtle pulse."),

    /** Crossfade — old items crossfade into new items. */
    CROSSFADE("Crossfade between old and new items."),

    /** Shimmer — section shows a shimmer placeholder during refresh. */
    SHIMMER("Shimmer placeholder during refresh."),

    /** Slide-and-replace — old items slide out, new slide in. */
    SLIDE_REPLACE("Slide out old, slide in new."),
}

/**
 * Coarse motion cadence — drives duration and damping in the UI.
 *
 * This is the render-layer mirror of [AnimationCadence] from the
 * contract layer. The contract layer declares *intent* (SNAPPY,
 * GENTLE, etc.); this layer carries the same intent through to
 * the UI. The UI maps each cadence to concrete spring parameters.
 */
enum class MotionCadence(
    internal val description: String,
    internal val dampingHint: Float,
    internal val durationHintMs: Int,
) {
    /** Snappy — fast, high damping. Used for utilitarian sections. */
    SNAPPY("Fast, decisive motion.", 0.85f, 180),

    /** Balanced — default for most sections. */
    BALANCED("Balanced motion.", 0.75f, 260),

    /** Gentle — slower, lower damping. Used for warm sections. */
    GENTLE("Gentle, slower motion.", 0.65f, 340),

    /** Dreamy — slow, low damping. Used for reflective / mood sections. */
    DREAMY("Dreamy, drifting motion.", 0.55f, 440),

    /** Drifting — very slow, very low damping. Used for empty states. */
    DRIFTING("Drifting, almost still motion.", 0.45f, 600),
}
