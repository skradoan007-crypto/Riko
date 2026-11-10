/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B4 — Library Rendering Runtime
 *
 * File 6 / 13 — RenderVisualState.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [RenderVisualState] is the *top-level* visual state of the
 * Library — what kind of screen the user is looking at.
 *
 * This is the render-layer mirror of [CollectionRuntimePhase] (B2).
 * The runtime speaks in terms of "phase" (COLD_START, HYDRATING,
 * READY, ...). The UI speaks in terms of "visual state" (Loading,
 * Ready, Empty, Error, PermissionRequired).
 *
 * The mapping is *not* 1:1 — multiple runtime phases may collapse
 * into one visual state, and one runtime phase may produce different
 * visual states depending on context (e.g. PARTIAL_READY → Ready
 * with a subtle refresh indicator, or Loading if no sections yet).
 *
 * RULES ENFORCED HERE
 * -------------------
 * #30  Empty State Has Emotion   — Empty is a first-class visual state
 * #31  Runtime Owns Truth        — UI reads this, not the runtime phase
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Android, no Compose, no Coroutine, no Flow. Pure Kotlin data.
 *
 * © RIKO Experience OS — Rendering Tier
 */

package com.rikodev.music.android.presentation.library.rendering.state

/**
 * The visual state of the Library.
 *
 * Each subclass carries the *minimum* information the UI needs to
 * render that state. The UI matches on the variant and applies
 * the corresponding screen-level Composable.
 *
 * Variants are deliberately sealed — adding a new visual state
 * forces the compiler to demand a handler in every consumer.
 */
sealed class RenderVisualState {

    /**
     * Library is loading — show a skeleton / shimmer.
     *
     * @property reason           Why loading. Drives loading UI.
     * @property estimatedProgress  0.0–1.0. 0 = unknown. The UI may
     *                             show an indeterminate indicator if 0.
     * @property visibleSectionsHint  How many sections are expected
     *                             when ready. Lets the UI size the
     *                             skeleton correctly.
     */
    data class Loading(
        val reason: com.rikodev.music.android.presentation.library.rendering.model.LoadingReason,
        val estimatedProgress: Float,
        val visibleSectionsHint: Int,
    ) : RenderVisualState() {
        init {
            require(estimatedProgress in 0f..1f) {
                "estimatedProgress must be in [0, 1], got $estimatedProgress"
            }
            require(visibleSectionsHint >= 0) {
                "visibleSectionsHint must be >= 0, got $visibleSectionsHint"
            }
        }
    }

    /**
     * Library is ready — sections are visible and interactive.
     *
     * @property isPartial       True if some sections are still loading.
     *                           The UI shows a subtle refresh indicator.
     * @property refreshingCount How many sections are currently refreshing.
     *                           0 if not partial.
     */
    data class Ready(
        val isPartial: Boolean,
        val refreshingCount: Int,
    ) : RenderVisualState() {
        init {
            require(refreshingCount >= 0) {
                "refreshingCount must be >= 0, got $refreshingCount"
            }
        }

        companion object {
            val FULL = Ready(isPartial = false, refreshingCount = 0)
        }
    }

    /**
     * Library is empty — no songs to generate collections from.
     *
     * Rule #30 — Empty State Has Emotion.
     * This is NOT just "show a sad face". The empty state carries
     * an emotional payload — a title, a message, an illustration
     * hint, and a primary CTA. The renderer resolves the *right*
     * empty state based on context (first-launch vs. all-songs-deleted
     * vs. permission-revoked-and-empty).
     *
     * @property emptyKind       Which kind of empty (drives copy + illustration).
     * @property title           Localized title text.
     * @property message         Localized message text.
     * @property illustrationHint  Illustration to draw.
     * @property primaryAction   Optional CTA (e.g. "Import music").
     * @property secondaryAction Optional secondary CTA.
     */
    data class Empty(
        val emptyKind: LibraryEmptyKind,
        val title: String,
        val message: String,
        val illustrationHint: EmptyIllustrationHint,
        val primaryAction: EmptyAction?,
        val secondaryAction: EmptyAction?,
    ) : RenderVisualState()

    /**
     * Library is in an error state — engine failed.
     *
     * @property reason          Why error. Drives error UI + recovery.
     * @property title           Localized title.
     * @property message         Localized message.
     * @property retryAction     Optional retry CTA.
     */
    data class Error(
        val reason: com.rikodev.music.android.presentation.library.rendering.model.ErrorReason,
        val title: String,
        val message: String,
        val retryAction: ErrorRetryAction?,
    ) : RenderVisualState()

    /**
     * Library requires a permission (e.g. media access).
     *
     * Distinct from Empty because the recovery path is different —
     * the user must grant a permission, not import content.
     *
     * @property permissionKey   Stable key for the permission needed.
     * @property title           Localized title.
     * @property message         Localized message.
     * @property grantAction     CTA to grant the permission.
     * @property settingsAction  Optional CTA to open app settings.
     */
    data class PermissionRequired(
        val permissionKey: String,
        val title: String,
        val message: String,
        val grantAction: PermissionAction,
        val settingsAction: PermissionAction?,
    ) : RenderVisualState()
}

/**
 * Kind of empty state — drives copy tone and illustration.
 *
 * Rule #30 — Empty State Has Emotion.
 * Different emptiness feels different. First-launch empty is
 * welcoming. All-songs-deleted empty is somber. Permission-revoked
 * empty is apologetic. The renderer picks the right kind.
 */
enum class LibraryEmptyKind(
    internal val description: String,
    internal val emotionalTone: String,
) {
    /** First-ever launch — no songs imported yet. Welcoming tone. */
    FIRST_LAUNCH(
        description = "First launch — library is empty.",
        emotionalTone = "Welcoming, anticipatory.",
    ),

    /** User had songs, but all were deleted. Somber tone. */
    ALL_DELETED(
        description = "All songs removed.",
        emotionalTone = "Somber, gentle.",
    ),

    /** Permission revoked, no songs accessible. Apologetic tone. */
    PERMISSION_REVOKED(
        description = "Permission revoked; songs inaccessible.",
        emotionalTone = "Apologetic, helpful.",
    ),

    /** Songs exist, but engine produced no collections. Curious tone. */
    NO_COLLECTIONS(
        description = "Songs exist but no collections generated.",
        emotionalTone = "Curious, investigative.",
    ),

    /** Filter active, no matches. Neutral tone. */
    FILTER_NO_MATCH(
        description = "Active filter produced no matches.",
        emotionalTone = "Neutral, actionable.",
    ),
}

/**
 * Illustration hint for an empty state.
 *
 * The renderer does NOT load bitmaps. It declares which illustration
 * *concept* should be drawn. The UI maps each hint to an actual
 * illustration asset (Lottie / SVG / Compose Canvas).
 */
enum class EmptyIllustrationHint(internal val description: String) {
    /** Welcoming illustration — e.g. an open hand offering music. */
    WELCOMING("Welcoming illustration."),

    /** Empty shelf — minimal, slightly somber. */
    EMPTY_SHELF("Empty shelf illustration."),

    /** Locked vault — permission-related. */
    LOCKED_VAULT("Locked vault illustration."),

    /** Searching magnifier — investigative. */
    SEARCHING("Searching illustration."),

    /** Filter with no results — neutral. */
    FILTER_EMPTY("Filter with no results illustration."),

    /** No illustration — text-only empty state. */
    NONE("No illustration; text-only."),
}

/**
 * CTA action on an empty state.
 *
 * @property label         Localized button label.
 * @property actionKey     Stable key the UI dispatches when tapped.
 *                         The UI never interprets the label — it
 *                         dispatches the actionKey.
 * @property isPrimary     True if this is the primary CTA (filled button).
 */
data class EmptyAction(
    val label: String,
    val actionKey: String,
    val isPrimary: Boolean,
)

/**
 * CTA action on an error state.
 */
data class ErrorRetryAction(
    val label: String,
    val actionKey: String,
)

/**
 * CTA action on a permission-required state.
 */
data class PermissionAction(
    val label: String,
    val actionKey: String,
)
