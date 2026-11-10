/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B4 — Library Rendering Runtime
 *
 * File 7 / 13 — RenderEmptyState.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [RenderEmptyState] is the *per-section* empty state — what to
 * show when a specific collection has zero items to display.
 *
 * This is distinct from [RenderVisualState.Empty] (file 6), which
 * is the *Library-wide* empty state. A section can be empty even
 * when the Library is overall Ready — e.g. "Forgotten Classics"
 * might be empty if the user has no songs older than 30 days.
 *
 * RULE #30 — Empty State Has Emotion.
 * Per-section empty states are the *most common* emptiness the user
 * encounters. They deserve as much craft as the Library-wide empty
 * state. Each section's empty state carries:
 *   • A title  (already localized + personality-shaped)
 *   • A message (explains *why* this section is empty)
 *   • An illustration hint
 *   • An optional CTA (e.g. "Listen to more songs")
 *
 * PERSONALITY-DRIVEN COPY
 * -----------------------
 * The renderer resolves the copy from the section's [CollectionPersonality].
 * A "Hidden Gems" empty state speaks quietly ("Nothing hiding yet.").
 * A "Surprise Me" empty state speaks playfully ("Hmm, no surprises
 * right now — try again later!"). The UI never has to invent copy.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #30  Empty State Has Emotion     — empty is a first-class visual element
 * #37  Collection Has Personality Before Data — copy tone follows personality
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Android, no Compose, no Coroutine, no Flow. Pure Kotlin data.
 *
 * © RIKO Experience OS — Rendering Tier
 */

package com.rikodev.music.android.presentation.library.rendering.state

/**
 * Per-section empty state.
 *
 * Used by [com.rikodev.music.android.presentation.library.rendering.model.RenderSection]
 * when its `items` list is empty. The UI matches on the section's
 * empty state and renders the corresponding empty-state Composable
 * *inside* the section's slot.
 *
 * @property sectionKind       Which kind of section is empty. Drives copy.
 * @property title             Localized title (e.g. "Nothing hiding yet.").
 * @property message           Localized message — explains why + how to fix.
 * @property illustrationHint  Illustration concept for the empty slot.
 * @property primaryAction     Optional CTA inside the section.
 * @property tone              Emotional tone — for the Inspector overlay.
 * @property isRecoverable     True if the user can take an action to fill
 *                             this section (e.g. listen to more songs).
 *                             False if it's a system-imposed emptiness.
 */
data class RenderEmptyState(
    val sectionKind: RenderEmptySectionKind,
    val title: String,
    val message: String,
    val illustrationHint: EmptyIllustrationHint,
    val primaryAction: EmptyAction?,
    val tone: RenderEmptyTone,
    val isRecoverable: Boolean,
) {
    companion object {
        /**
         * Generic fallback empty state — used when the renderer has
         * no personality-specific copy available. Should rarely trigger.
         */
        val FALLBACK = RenderEmptyState(
            sectionKind = RenderEmptySectionKind.GENERIC,
            title = "Nothing here yet.",
            message = "This section is empty.",
            illustrationHint = EmptyIllustrationHint.EMPTY_SHELF,
            primaryAction = null,
            tone = RenderEmptyTone.NEUTRAL,
            isRecoverable = false,
        )
    }
}

/**
 * Kind of section that is empty.
 *
 * Each kind maps to a different copy template in the renderer's
 * personality resolver. Adding a new kind = adding new copy.
 */
enum class RenderEmptySectionKind(
    internal val description: String,
    internal val defaultTone: RenderEmptyTone,
) {
    /** Continue Listening section — usually means fresh session. */
    CONTINUE_LISTENING(
        description = "Continue Listening is empty.",
        defaultTone = RenderEmptyTone.GENTLE,
    ),

    /** Favorites section — usually means user hasn't favorited yet. */
    FAVORITES(
        description = "Favorites is empty.",
        defaultTone = RenderEmptyTone.WARM,
    ),

    /** Hidden Gems section — usually means user hasn't played enough. */
    HIDDEN_GEMS(
        description = "Hidden Gems is empty.",
        defaultTone = RenderEmptyTone.QUIET,
    ),

    /** Forgotten Classics section — usually means library is new. */
    FORGOTTEN_CLASSICS(
        description = "Forgotten Classics is empty.",
        defaultTone = RenderEmptyTone.REFLECTIVE,
    ),

    /** Mood-based section — usually means no mood matches current context. */
    MOOD_BASED(
        description = "Mood-based collection is empty.",
        defaultTone = RenderEmptyTone.PLAYFUL,
    ),

    /** Discovery section — usually means library is too small. */
    DISCOVERY(
        description = "Discovery is empty.",
        defaultTone = RenderEmptyTone.CURIOUS,
    ),

    /** Most Played section — usually means not enough play history. */
    MOST_PLAYED(
        description = "Most Played is empty.",
        defaultTone = RenderEmptyTone.NEUTRAL,
    ),

    /** Recently Added section — usually means no new songs. */
    RECENTLY_ADDED(
        description = "Recently Added is empty.",
        defaultTone = RenderEmptyTone.NEUTRAL,
    ),

    /** Recently Played section — usually means fresh session. */
    RECENTLY_PLAYED(
        description = "Recently Played is empty.",
        defaultTone = RenderEmptyTone.NEUTRAL,
    ),

    /** User playlists section. */
    PLAYLISTS(
        description = "Playlists is empty.",
        defaultTone = RenderEmptyTone.WARM,
    ),

    /** Generic fallback — kind unknown. */
    GENERIC(
        description = "Section is empty (unknown kind).",
        defaultTone = RenderEmptyTone.NEUTRAL,
    ),
}

/**
 * Emotional tone of an empty state.
 *
 * Drives illustration style, motion cadence, and copy shape.
 * The UI may also use this to adjust the section's background tint
 * subtly — e.g. a reflective empty state gets a slightly cooler tint.
 */
enum class RenderEmptyTone(
    internal val description: String,
    internal val motionCadenceHint: String,
) {
    /** Quiet — for sections that should not announce their emptiness. */
    QUIET(
        description = "Quiet, understated.",
        motionCadenceHint = "DRIFTING",
    ),

    /** Warm — for sections that invite the user in. */
    WARM(
        description = "Warm, inviting.",
        motionCadenceHint = "GENTLE",
    ),

    /** Gentle — soft, supportive. */
    GENTLE(
        description = "Gentle, supportive.",
        motionCadenceHint = "GENTLE",
    ),

    /** Reflective — slow, weighted. */
    REFLECTIVE(
        description = "Reflective, slow.",
        motionCadenceHint = "DREAMY",
    ),

    /** Playful — light, surprising. */
    PLAYFUL(
        description = "Playful, light.",
        motionCadenceHint = "BALANCED",
    ),

    /** Curious — inquisitive, investigative. */
    CURIOUS(
        description = "Curious, inquisitive.",
        motionCadenceHint = "BALANCED",
    ),

    /** Neutral — informational only. */
    NEUTRAL(
        description = "Neutral, informational.",
        motionCadenceHint = "BALANCED",
    ),
}
