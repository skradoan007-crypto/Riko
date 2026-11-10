/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.1 — Library Screen Constitution
 *
 * File 11 / 15 — LibraryEmptyStateContract.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * The Library Empty State Contract declares the *screen-level*
 * empty state — what the user sees when the Library has no content
 * to show at all (no songs, no permission, etc.).
 *
 * This is distinct from [RenderEmptyState] (B4, file 7), which is
 * the *per-section* empty state. The screen-level empty state
 * replaces the entire content slot, not just one section.
 *
 * Rule #30 — Empty State Has Emotion.
 * Rule #54 — Screen Tells a Story.
 * An empty Library is still a story — it's the *opening chapter*.
 * The user just opened RIKO for the first time; the empty state
 * is RIKO's first words to them.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #30  Empty State Has Emotion     — empty is first-class
 * #37  Collection Has Personality Before Data — empty state has voice
 * #54  Screen Tells a Story        — empty state is the opening chapter
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
 * The kind of screen-level empty state.
 *
 * Each kind has a distinct emotional tone and recovery path.
 */
enum class LibraryEmptyStateKind(
    internal val description: String,
    internal val emotionalTone: String,
) {
    /** First launch — no songs imported yet. Welcoming tone. */
    FIRST_LAUNCH(
        description = "First launch — empty library.",
        emotionalTone = "Welcoming, anticipatory.",
    ),

    /** Permission denied — songs exist but inaccessible. Apologetic tone. */
    PERMISSION_DENIED(
        description = "Permission denied — songs inaccessible.",
        emotionalTone = "Apologetic, helpful.",
    ),

    /** All songs deleted — library went silent. Somber tone. */
    ALL_DELETED(
        description = "All songs deleted.",
        emotionalTone = "Somber, gentle.",
    ),

    /** No collections generated — songs exist but engine produced nothing. */
    NO_COLLECTIONS(
        description = "Songs exist, no collections generated.",
        emotionalTone = "Curious, investigative.",
    ),

    /** Filter active, no matches. Neutral tone. */
    FILTER_NO_MATCH(
        description = "Filter produced no matches.",
        emotionalTone = "Neutral, actionable.",
    ),

    /** Search returned no results. Curious tone. */
    SEARCH_NO_RESULT(
        description = "Search returned no results.",
        emotionalTone = "Curious, helpful.",
    ),

    /** Loading failed. Apologetic tone. */
    LOAD_FAILED(
        description = "Library failed to load.",
        emotionalTone = "Apologetic, recoverable.",
    ),
}

/**
 * The shape of an empty state.
 *
 * @property kind                  Empty state kind.
 * @property title                 Title text (localized, personality-shaped).
 * @property message               Message text (explains why + how to fix).
 * @property illustrationKind      Illustration concept.
 * @property primaryAction         Optional primary CTA.
 * @property secondaryAction       Optional secondary CTA.
 * @property motionIntensity       0.0–1.0. How much motion the empty state has.
 * @property isRecoverable         True if the user can take an action.
 */
data class LibraryEmptyStateShape(
    val kind: LibraryEmptyStateKind,
    val title: String,
    val message: String,
    val illustrationKind: LibraryEmptyIllustrationKind,
    val primaryAction: LibraryEmptyAction?,
    val secondaryAction: LibraryEmptyAction?,
    val motionIntensity: Float,
    val isRecoverable: Boolean,
) {
    init {
        require(motionIntensity in 0f..1f) {
            "motionIntensity must be in [0, 1], got $motionIntensity"
        }
    }

    companion object {
        /** First-launch empty state — welcoming. */
        val FIRST_LAUNCH = LibraryEmptyStateShape(
            kind = LibraryEmptyStateKind.FIRST_LAUNCH,
            title = "Your library is silent",
            message = "RIKO listens to your music and shapes collections around your taste. " +
                "Bring your songs in — the silence will end.",
            illustrationKind = LibraryEmptyIllustrationKind.WELCOMING_HAND,
            primaryAction = LibraryEmptyAction(
                label = "Import music",
                actionKey = "empty.import",
                isPrimary = true,
            ),
            secondaryAction = LibraryEmptyAction(
                label = "Grant permission",
                actionKey = "empty.grantPermission",
                isPrimary = false,
            ),
            motionIntensity = 0.3f,
            isRecoverable = true,
        )

        /** Permission denied empty state — apologetic. */
        val PERMISSION_DENIED = LibraryEmptyStateShape(
            kind = LibraryEmptyStateKind.PERMISSION_DENIED,
            title = "Music is locked away",
            message = "RIKO can't see your songs until you grant permission. " +
                "Your library will rebuild the moment access returns.",
            illustrationKind = LibraryEmptyIllustrationKind.LOCKED_VAULT,
            primaryAction = LibraryEmptyAction(
                label = "Grant access",
                actionKey = "empty.grantPermission",
                isPrimary = true,
            ),
            secondaryAction = LibraryEmptyAction(
                label = "Open settings",
                actionKey = "empty.openSettings",
                isPrimary = false,
            ),
            motionIntensity = 0.2f,
            isRecoverable = true,
        )

        /** All-deleted empty state — somber. */
        val ALL_DELETED = LibraryEmptyStateShape(
            kind = LibraryEmptyStateKind.ALL_DELETED,
            title = "Your library went quiet",
            message = "All your songs were removed. The collections will rebuild " +
                "the moment music returns.",
            illustrationKind = LibraryEmptyIllustrationKind.EMPTY_SHELF,
            primaryAction = LibraryEmptyAction(
                label = "Import music",
                actionKey = "empty.import",
                isPrimary = true,
            ),
            secondaryAction = null,
            motionIntensity = 0.15f,
            isRecoverable = true,
        )

        /** Load-failed empty state — apologetic + recoverable. */
        val LOAD_FAILED = LibraryEmptyStateShape(
            kind = LibraryEmptyStateKind.LOAD_FAILED,
            title = "Library couldn't load",
            message = "Something went wrong while building your Library. " +
                "Try again — RIKO will pick up where it left off.",
            illustrationKind = LibraryEmptyIllustrationKind.BROKEN_WAVE,
            primaryAction = LibraryEmptyAction(
                label = "Try again",
                actionKey = "empty.retry",
                isPrimary = true,
            ),
            secondaryAction = null,
            motionIntensity = 0.2f,
            isRecoverable = true,
        )
    }
}

/**
 * Kind of illustration for an empty state.
 *
 * The renderer (B5.3) maps each kind to a concrete illustration
 * asset (Lottie / SVG / Compose Canvas).
 */
enum class LibraryEmptyIllustrationKind(
    internal val description: String,
    internal val hasAnimation: Boolean,
) {
    /** Welcoming hand — open palm offering music. */
    WELCOMING_HAND("Welcoming hand illustration.", hasAnimation = true),

    /** Empty shelf — minimal, slightly somber. */
    EMPTY_SHELF("Empty shelf illustration.", hasAnimation = false),

    /** Locked vault — permission-related. */
    LOCKED_VAULT("Locked vault illustration.", hasAnimation = true),

    /** Searching magnifier — investigative. */
    SEARCHING("Searching illustration.", hasAnimation = true),

    /** Filter with no results — neutral. */
    FILTER_EMPTY("Filter empty illustration.", hasAnimation = false),

    /** Broken wave — error state. */
    BROKEN_WAVE("Broken wave illustration.", hasAnimation = true),

    /** No illustration — text-only empty state. */
    NONE("No illustration; text-only.", hasAnimation = false),
}

/**
 * An action button on an empty state.
 *
 * @property label        Localized button label.
 * @property actionKey    Stable key the runtime dispatches when tapped.
 * @property isPrimary    True if this is the primary CTA (filled button).
 */
data class LibraryEmptyAction(
    val label: String,
    val actionKey: String,
    val isPrimary: Boolean,
)

/**
 * The Library Empty State Contract.
 *
 * @property shape                The empty state shape.
 * @property isVisible            True if the empty state is currently visible.
 * @property replacesEntireContent  True if the empty state replaces the
 *                                  entire content slot (vs. just one section).
 * @property topBarRemainsVisible   True if the top bar stays visible above
 *                                  the empty state.
 * @property dockRemainsVisible      True if the dock stays visible below
 *                                  the empty state.
 */
data class LibraryEmptyStateContract(
    val shape: LibraryEmptyStateShape,
    val isVisible: Boolean,
    val replacesEntireContent: Boolean,
    val topBarRemainsVisible: Boolean,
    val dockRemainsVisible: Boolean,
) {
    companion object {
        /** Hidden empty state — Library has content. */
        val HIDDEN = LibraryEmptyStateContract(
            shape = LibraryEmptyStateShape.FIRST_LAUNCH,
            isVisible = false,
            replacesEntireContent = false,
            topBarRemainsVisible = true,
            dockRemainsVisible = true,
        )

        /** First-launch empty state — top bar + dock remain visible. */
        val FIRST_LAUNCH_VISIBLE = LibraryEmptyStateContract(
            shape = LibraryEmptyStateShape.FIRST_LAUNCH,
            isVisible = true,
            replacesEntireContent = true,
            topBarRemainsVisible = true,
            dockRemainsVisible = false,
        )

        /** Permission denied empty state — top bar visible, dock hidden. */
        val PERMISSION_DENIED_VISIBLE = LibraryEmptyStateContract(
            shape = LibraryEmptyStateShape.PERMISSION_DENIED,
            isVisible = true,
            replacesEntireContent = true,
            topBarRemainsVisible = true,
            dockRemainsVisible = false,
        )
    }
}
