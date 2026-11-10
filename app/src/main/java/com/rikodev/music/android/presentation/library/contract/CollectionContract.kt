/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B1 — Collection Contract Pack v2 (Final)
 *
 * File 14 / 15 — CollectionContract.kt — THE CONSTITUTION
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * This file is the Constitution of the RIKO Library.
 *
 * Every collection RIKO ships — past, present, future — has a
 * single [CollectionContract] entry in [CollectionConstitution].
 * The contract combines all 13 prior files into one master
 * declaration per collection.
 *
 * RULE #36 — Contract Never Thinks
 * RULE #37 — Collection Has Personality Before Data
 * RULE #38 — Every Collection Can Explain Itself
 * RULE #39 — Contracts Are Forever
 * RULE #40 — Metadata Lives Forever (v2)
 * RULE #41 — Section Owns Layout (v2)
 * RULE #42 — Diagnostics Is First-Class (v2)
 * RULE #43 — Contracts Are Documentation (v2)
 *
 * THE PROMISE
 * -----------
 * A new developer joining the project one year from now should
 * be able to open this single file and understand:
 *
 *   • Which collections RIKO has.
 *   • What each one means (emotionally, behaviourally).
 *   • What it can do (capabilities).
 *   • How it should look (presentation).
 *   • What limits it respects (constraints).
 *   • How to introspect it (diagnostics).
 *   • Why it exists (metadata).
 *
 * WITHOUT reading any runtime, engine, or UI file.
 *
 * This is what makes RIKO a Constitution-driven product, not a
 * feature-driven one.
 *
 * No logic. No side effects. No Android imports. Pure declaration.
 *
 * © RIKO Experience OS — Constitution Tier
 */

package com.rikodev.music.android.presentation.library.contract

/**
 * The master contract for a single RIKO collection.
 *
 * Composed of:
 *   • Identity        — type, category, emotion, priority, visibility
 *   • Personality     — voice + cadence + layout preference
 *   • Capabilities    — what the user is allowed to do
 *   • Presentation    — how it looks
 *   • Constraints     — hard limits the engine respects
 *   • Metadata        — the Identity Card (Rule #40)
 *   • Diagnostics     — first-class introspection (Rule #42)
 *
 * The contract has zero methods. It is pure data. The runtime,
 * engine, and UI all read it — none of them write to it.
 */
data class CollectionContract(
    val type: CollectionType,
    val category: CollectionCategory,
    val emotion: CollectionEmotion,
    val priority: CollectionPriority,
    val visibility: CollectionVisibility,
    val personality: CollectionPersonality,
    val capability: CollectionCapability,
    val presentation: CollectionPresentation,
    val constraints: CollectionConstraints,
    val metadata: CollectionMetadata,
    val diagnostics: CollectionDiagnosticsContract,
) {

    /** Stable identity — never localized, never renamed. */
    val stableKey: String get() = type.stableKey

    /** True if this contract is currently behind a feature flag. */
    val isLocked: Boolean get() = type.isLocked

    /** True if this contract is allowed in the user-visible feed. */
    val isFeedEligible: Boolean get() = priority.isFeedEligible && visibility.rendersCards

    /** True if this contract is allowed on cold start. */
    val isColdStartEligible: Boolean
        get() = priority.isColdStartEligible && type.visibleByDefault && !type.developerOnly
}

/**
 * THE CONSTITUTION — the complete, locked catalogue of every
 * collection RIKO knows about.
 *
 * Adding an entry here is a Constitution-level decision. The
 * runtime (B2) does not generate contracts — it consumes them.
 * The engine (B3) does not invent collections — it produces
 * data for collections declared here.
 *
 * If a [CollectionType] exists but has no entry here, the runtime
 * MUST treat it as a contract violation and surface a diagnostic.
 */
object CollectionConstitution {

    // ─────────────────────────────────────────────────────────────
    // LISTENING CONTINUITY
    // ─────────────────────────────────────────────────────────────

    val RECENTLY_PLAYED = CollectionContract(
        type = CollectionType.RECENTLY_PLAYED,
        category = CollectionCategory.LISTENING,
        emotion = CollectionEmotion.WARM,
        priority = CollectionPriority.PRIMARY,
        visibility = CollectionVisibility.Visible(),
        personality = CollectionPersonality.GENTLE,
        capability = CollectionCapability.SMART,
        presentation = CollectionPresentation.HERO,
        constraints = CollectionConstraints.HERO,
        metadata = CollectionMetadata.RECENTLY_PLAYED,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    val CONTINUE_LISTENING = CollectionContract(
        type = CollectionType.CONTINUE_LISTENING,
        category = CollectionCategory.LISTENING,
        emotion = CollectionEmotion.GENTLE,
        priority = CollectionPriority.PRIMARY,
        visibility = CollectionVisibility.Visible(),
        personality = CollectionPersonality.GENTLE,
        capability = CollectionCapability.SMART,
        presentation = CollectionPresentation.HERO,
        constraints = CollectionConstraints.HERO,
        metadata = CollectionMetadata.CONTINUE_LISTENING,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    val RECENTLY_FINISHED = CollectionContract(
        type = CollectionType.RECENTLY_FINISHED,
        category = CollectionCategory.LISTENING,
        emotion = CollectionEmotion.WARM,
        priority = CollectionPriority.SECONDARY,
        visibility = CollectionVisibility.Visible(),
        personality = CollectionPersonality.REFLECTIVE,
        capability = CollectionCapability.SMART,
        presentation = CollectionPresentation.DEFAULT,
        constraints = CollectionConstraints.SMART,
        metadata = CollectionMetadata.RECENTLY_FINISHED,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    // ─────────────────────────────────────────────────────────────
    // PLAY FREQUENCY
    // ─────────────────────────────────────────────────────────────

    val MOST_PLAYED = CollectionContract(
        type = CollectionType.MOST_PLAYED,
        category = CollectionCategory.LISTENING,
        emotion = CollectionEmotion.POWERFUL,
        priority = CollectionPriority.PRIMARY,
        visibility = CollectionVisibility.Visible(),
        personality = CollectionPersonality.ENERGETIC,
        capability = CollectionCapability.SMART,
        presentation = CollectionPresentation.HERO,
        constraints = CollectionConstraints.SMART,
        metadata = CollectionMetadata.MOST_PLAYED,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    val LEAST_PLAYED = CollectionContract(
        type = CollectionType.LEAST_PLAYED,
        category = CollectionCategory.DISCOVERY,
        emotion = CollectionEmotion.REFLECTIVE,
        priority = CollectionPriority.SECONDARY,
        visibility = CollectionVisibility.Visible(),
        personality = CollectionPersonality.REFLECTIVE,
        capability = CollectionCapability.SMART,
        presentation = CollectionPresentation.DEFAULT,
        constraints = CollectionConstraints.SMART,
        metadata = CollectionMetadata.LEAST_PLAYED,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    val NEVER_PLAYED = CollectionContract(
        type = CollectionType.NEVER_PLAYED,
        category = CollectionCategory.DISCOVERY,
        emotion = CollectionEmotion.FLOATING,
        priority = CollectionPriority.SECONDARY,
        visibility = CollectionVisibility.Visible(),
        personality = CollectionPersonality.EXPLORER,
        capability = CollectionCapability.SMART,
        presentation = CollectionPresentation.DEFAULT,
        constraints = CollectionConstraints.SMART,
        metadata = CollectionMetadata.NEVER_PLAYED,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    // ─────────────────────────────────────────────────────────────
    // LIBRARY INTAKE
    // ─────────────────────────────────────────────────────────────

    val RECENTLY_ADDED = CollectionContract(
        type = CollectionType.RECENTLY_ADDED,
        category = CollectionCategory.LIBRARY,
        emotion = CollectionEmotion.BRIGHT,
        priority = CollectionPriority.PRIMARY,
        visibility = CollectionVisibility.Visible(),
        personality = CollectionPersonality.COLLECTOR,
        capability = CollectionCapability.SMART,
        presentation = CollectionPresentation.HERO,
        constraints = CollectionConstraints.SMART,
        metadata = CollectionMetadata.RECENTLY_ADDED,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    // ─────────────────────────────────────────────────────────────
    // HIDDEN GEMS
    // ─────────────────────────────────────────────────────────────

    val HIDDEN_GEMS = CollectionContract(
        type = CollectionType.HIDDEN_GEMS,
        category = CollectionCategory.DISCOVERY,
        emotion = CollectionEmotion.DREAMY,
        priority = CollectionPriority.PRIMARY,
        visibility = CollectionVisibility.Visible(),
        personality = CollectionPersonality.EXPLORER,
        capability = CollectionCapability.SMART,
        presentation = CollectionPresentation.DISCOVERY,
        constraints = CollectionConstraints.DISCOVERY,
        metadata = CollectionMetadata.HIDDEN_GEMS,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    val FORGOTTEN_CLASSICS = CollectionContract(
        type = CollectionType.FORGOTTEN_CLASSICS,
        category = CollectionCategory.DISCOVERY,
        emotion = CollectionEmotion.REFLECTIVE,
        priority = CollectionPriority.SECONDARY,
        visibility = CollectionVisibility.Visible(),
        personality = CollectionPersonality.STORY_TELLER,
        capability = CollectionCapability.SMART,
        presentation = CollectionPresentation.DISCOVERY,
        constraints = CollectionConstraints.DISCOVERY,
        metadata = CollectionMetadata.FORGOTTEN_CLASSICS,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    // ─────────────────────────────────────────────────────────────
    // MOOD / TIME OF DAY
    // ─────────────────────────────────────────────────────────────

    val NIGHT_CALM = CollectionContract(
        type = CollectionType.NIGHT_CALM,
        category = CollectionCategory.MOOD,
        emotion = CollectionEmotion.NIGHT,
        priority = CollectionPriority.PRIMARY,
        visibility = CollectionVisibility.Adaptive(
            rule = AdaptiveVisibilityRule.TIME_OF_DAY,
        ),
        personality = CollectionPersonality.COMPANION,
        capability = CollectionCapability.SMART,
        presentation = CollectionPresentation.MOOD,
        constraints = CollectionConstraints.MOOD,
        metadata = CollectionMetadata.NIGHT_CALM,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    val MORNING_ENERGY = CollectionContract(
        type = CollectionType.MORNING_ENERGY,
        category = CollectionCategory.MOOD,
        emotion = CollectionEmotion.MORNING,
        priority = CollectionPriority.PRIMARY,
        visibility = CollectionVisibility.Adaptive(
            rule = AdaptiveVisibilityRule.TIME_OF_DAY,
        ),
        personality = CollectionPersonality.COMPANION,
        capability = CollectionCapability.SMART,
        presentation = CollectionPresentation.MOOD,
        constraints = CollectionConstraints.MOOD,
        metadata = CollectionMetadata.MORNING_ENERGY,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    val EVENING_UNWIND = CollectionContract(
        type = CollectionType.EVENING_UNWIND,
        category = CollectionCategory.MOOD,
        emotion = CollectionEmotion.SUNSET,
        priority = CollectionPriority.SECONDARY,
        visibility = CollectionVisibility.Adaptive(
            rule = AdaptiveVisibilityRule.TIME_OF_DAY,
        ),
        personality = CollectionPersonality.COMPANION,
        capability = CollectionCapability.SMART,
        presentation = CollectionPresentation.MOOD,
        constraints = CollectionConstraints.MOOD,
        metadata = CollectionMetadata.EVENING_UNWIND,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    val COMMUTE_MUSIC = CollectionContract(
        type = CollectionType.COMMUTE_MUSIC,
        category = CollectionCategory.MOOD,
        emotion = CollectionEmotion.OCEAN,
        priority = CollectionPriority.SECONDARY,
        visibility = CollectionVisibility.Adaptive(
            rule = AdaptiveVisibilityRule.CONTEXT_MATCH,
        ),
        personality = CollectionPersonality.COMPANION,
        capability = CollectionCapability.SMART,
        presentation = CollectionPresentation.MOOD,
        constraints = CollectionConstraints.MOOD,
        metadata = CollectionMetadata.COMMUTE_MUSIC,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    val STUDY_FOCUS = CollectionContract(
        type = CollectionType.STUDY_FOCUS,
        category = CollectionCategory.MOOD,
        emotion = CollectionEmotion.FOCUSED,
        priority = CollectionPriority.SECONDARY,
        visibility = CollectionVisibility.Adaptive(
            rule = AdaptiveVisibilityRule.CONTEXT_MATCH,
        ),
        personality = CollectionPersonality.MINIMAL,
        capability = CollectionCapability.SMART,
        presentation = CollectionPresentation.MOOD,
        constraints = CollectionConstraints.MOOD,
        metadata = CollectionMetadata.STUDY_FOCUS,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    val SLEEP_DRIFT = CollectionContract(
        type = CollectionType.SLEEP_DRIFT,
        category = CollectionCategory.MOOD,
        emotion = CollectionEmotion.DREAMY,
        priority = CollectionPriority.OPTIONAL,
        visibility = CollectionVisibility.Adaptive(
            rule = AdaptiveVisibilityRule.TIME_OF_DAY,
        ),
        personality = CollectionPersonality.COMPANION,
        capability = CollectionCapability.SMART,
        presentation = CollectionPresentation.MOOD,
        constraints = CollectionConstraints.MOOD,
        metadata = CollectionMetadata.SLEEP_DRIFT,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    val WORKOUT_PUMP = CollectionContract(
        type = CollectionType.WORKOUT_PUMP,
        category = CollectionCategory.MOOD,
        emotion = CollectionEmotion.FIRE,
        priority = CollectionPriority.OPTIONAL,
        visibility = CollectionVisibility.Adaptive(
            rule = AdaptiveVisibilityRule.CONTEXT_MATCH,
        ),
        personality = CollectionPersonality.ENERGETIC,
        capability = CollectionCapability.SMART,
        presentation = CollectionPresentation.MOOD,
        constraints = CollectionConstraints.MOOD,
        metadata = CollectionMetadata.WORKOUT_PUMP,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    // ─────────────────────────────────────────────────────────────
    // EMOTIONAL DEEP CUTS
    // ─────────────────────────────────────────────────────────────

    val ROMANTIC_MOOD = CollectionContract(
        type = CollectionType.ROMANTIC_MOOD,
        category = CollectionCategory.EMOTION,
        emotion = CollectionEmotion.WARM,
        priority = CollectionPriority.OPTIONAL,
        visibility = CollectionVisibility.Visible(),
        personality = CollectionPersonality.COMPANION,
        capability = CollectionCapability.SMART,
        presentation = CollectionPresentation.DISCOVERY,
        constraints = CollectionConstraints.SMART,
        metadata = CollectionMetadata.ROMANTIC_MOOD,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    val MELANCHOLY_JOURNEY = CollectionContract(
        type = CollectionType.MELANCHOLY_JOURNEY,
        category = CollectionCategory.EMOTION,
        emotion = CollectionEmotion.DARK,
        priority = CollectionPriority.OPTIONAL,
        visibility = CollectionVisibility.Visible(),
        personality = CollectionPersonality.REFLECTIVE,
        capability = CollectionCapability.SMART,
        presentation = CollectionPresentation.DISCOVERY,
        constraints = CollectionConstraints.SMART,
        metadata = CollectionMetadata.MELANCHOLY_JOURNEY,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    val INSTRUMENTAL_ESCAPE = CollectionContract(
        type = CollectionType.INSTRUMENTAL_ESCAPE,
        category = CollectionCategory.EMOTION,
        emotion = CollectionEmotion.MINIMAL,
        priority = CollectionPriority.OPTIONAL,
        visibility = CollectionVisibility.Visible(),
        personality = CollectionPersonality.MINIMAL,
        capability = CollectionCapability.SMART,
        presentation = CollectionPresentation.DISCOVERY,
        constraints = CollectionConstraints.SMART,
        metadata = CollectionMetadata.INSTRUMENTAL_ESCAPE,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    val VOCAL_ONLY = CollectionContract(
        type = CollectionType.VOCAL_ONLY,
        category = CollectionCategory.EMOTION,
        emotion = CollectionEmotion.WARM,
        priority = CollectionPriority.OPTIONAL,
        visibility = CollectionVisibility.Visible(),
        personality = CollectionPersonality.COLLECTOR,
        capability = CollectionCapability.SMART,
        presentation = CollectionPresentation.DISCOVERY,
        constraints = CollectionConstraints.SMART,
        metadata = CollectionMetadata.VOCAL_ONLY,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    // ─────────────────────────────────────────────────────────────
    // DISCOVERY
    // ─────────────────────────────────────────────────────────────

    val DISCOVERY = CollectionContract(
        type = CollectionType.DISCOVERY,
        category = CollectionCategory.DISCOVERY,
        emotion = CollectionEmotion.FLOATING,
        priority = CollectionPriority.PRIMARY,
        visibility = CollectionVisibility.Visible(),
        personality = CollectionPersonality.EXPLORER,
        capability = CollectionCapability.SMART,
        presentation = CollectionPresentation.DISCOVERY,
        constraints = CollectionConstraints.DISCOVERY,
        metadata = CollectionMetadata.DISCOVERY,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    val SIMILAR_TO = CollectionContract(
        type = CollectionType.SIMILAR_TO,
        category = CollectionCategory.DISCOVERY,
        emotion = CollectionEmotion.FLOATING,
        priority = CollectionPriority.SECONDARY,
        visibility = CollectionVisibility.Visible(),
        personality = CollectionPersonality.EXPLORER,
        capability = CollectionCapability.SMART,
        presentation = CollectionPresentation.DISCOVERY,
        constraints = CollectionConstraints.DISCOVERY,
        metadata = CollectionMetadata.SIMILAR_TO,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    val SURPRISE_ME = CollectionContract(
        type = CollectionType.SURPRISE_ME,
        category = CollectionCategory.DISCOVERY,
        emotion = CollectionEmotion.ALIVE,
        priority = CollectionPriority.SECONDARY,
        visibility = CollectionVisibility.Visible(),
        personality = CollectionPersonality.ENERGETIC,
        capability = CollectionCapability.SMART,
        presentation = CollectionPresentation.DISCOVERY,
        constraints = CollectionConstraints.DISCOVERY,
        metadata = CollectionMetadata.SURPRISE_ME,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    // ─────────────────────────────────────────────────────────────
    // CONTEXT-AWARE (Experimental)
    // ─────────────────────────────────────────────────────────────

    val MOOD_BASED = CollectionContract(
        type = CollectionType.MOOD_BASED,
        category = CollectionCategory.SMART,
        emotion = CollectionEmotion.ADAPTIVE,
        priority = CollectionPriority.SECONDARY,
        visibility = CollectionVisibility.Adaptive(
            rule = AdaptiveVisibilityRule.COLLECTION_HEALTH,
        ),
        personality = CollectionPersonality.COMPANION,
        capability = CollectionCapability.EXPERIMENTAL,
        presentation = CollectionPresentation.DISCOVERY,
        constraints = CollectionConstraints.SMART,
        metadata = CollectionMetadata.MOOD_BASED,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    val WEATHER_BASED = CollectionContract(
        type = CollectionType.WEATHER_BASED,
        category = CollectionCategory.SMART,
        emotion = CollectionEmotion.ADAPTIVE,
        priority = CollectionPriority.OPTIONAL,
        visibility = CollectionVisibility.Adaptive(
            rule = AdaptiveVisibilityRule.CONTEXT_MATCH,
        ),
        personality = CollectionPersonality.COMPANION,
        capability = CollectionCapability.EXPERIMENTAL,
        presentation = CollectionPresentation.DISCOVERY,
        constraints = CollectionConstraints.SMART,
        metadata = CollectionMetadata.WEATHER_BASED,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    val TIME_BASED = CollectionContract(
        type = CollectionType.TIME_BASED,
        category = CollectionCategory.SMART,
        emotion = CollectionEmotion.ADAPTIVE,
        priority = CollectionPriority.OPTIONAL,
        visibility = CollectionVisibility.Adaptive(
            rule = AdaptiveVisibilityRule.TIME_OF_DAY,
        ),
        personality = CollectionPersonality.COMPANION,
        capability = CollectionCapability.EXPERIMENTAL,
        presentation = CollectionPresentation.DISCOVERY,
        constraints = CollectionConstraints.SMART,
        metadata = CollectionMetadata.TIME_BASED,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    // ─────────────────────────────────────────────────────────────
    // USER-OWNED
    // ─────────────────────────────────────────────────────────────

    val FAVORITES = CollectionContract(
        type = CollectionType.FAVORITES,
        category = CollectionCategory.LIBRARY,
        emotion = CollectionEmotion.WARM,
        priority = CollectionPriority.PRIMARY,
        visibility = CollectionVisibility.Visible(),
        personality = CollectionPersonality.COLLECTOR,
        capability = CollectionCapability.USER_OWNED,
        presentation = CollectionPresentation.DEFAULT,
        constraints = CollectionConstraints.SMART,
        metadata = CollectionMetadata.FAVORITES,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    val PINNED = CollectionContract(
        type = CollectionType.PINNED,
        category = CollectionCategory.LIBRARY,
        emotion = CollectionEmotion.ORGANIC,
        priority = CollectionPriority.PRIMARY,
        visibility = CollectionVisibility.Visible(),
        personality = CollectionPersonality.COLLECTOR,
        capability = CollectionCapability.USER_OWNED,
        presentation = CollectionPresentation.DEFAULT,
        constraints = CollectionConstraints.SMART,
        metadata = CollectionMetadata.PINNED,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    val PLAYLIST_USER = CollectionContract(
        type = CollectionType.PLAYLIST_USER,
        category = CollectionCategory.PLAYLIST,
        emotion = CollectionEmotion.ORGANIC,
        priority = CollectionPriority.PRIMARY,
        visibility = CollectionVisibility.Visible(),
        personality = CollectionPersonality.COLLECTOR,
        capability = CollectionCapability.USER_OWNED,
        presentation = CollectionPresentation.DEFAULT,
        constraints = CollectionConstraints.SMART,
        metadata = CollectionMetadata.PLAYLIST_USER,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    val PLAYLIST_AUTO = CollectionContract(
        type = CollectionType.PLAYLIST_AUTO,
        category = CollectionCategory.PLAYLIST,
        emotion = CollectionEmotion.MINIMAL,
        priority = CollectionPriority.SECONDARY,
        visibility = CollectionVisibility.Visible(),
        personality = CollectionPersonality.MINIMAL,
        capability = CollectionCapability.SMART,
        presentation = CollectionPresentation.DEFAULT,
        constraints = CollectionConstraints.SMART,
        metadata = CollectionMetadata.PLAYLIST_AUTO,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    // ─────────────────────────────────────────────────────────────
    // STRUCTURAL
    // ─────────────────────────────────────────────────────────────

    val ALL_SONGS = CollectionContract(
        type = CollectionType.ALL_SONGS,
        category = CollectionCategory.LIBRARY,
        emotion = CollectionEmotion.MINIMAL,
        priority = CollectionPriority.PRIMARY,
        visibility = CollectionVisibility.Visible(),
        personality = CollectionPersonality.MINIMAL,
        capability = CollectionCapability.LIBRARY_STRUCTURE,
        presentation = CollectionPresentation.LIST,
        constraints = CollectionConstraints.LIBRARY_STRUCTURE,
        metadata = CollectionMetadata.ALL_SONGS,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    val ALL_ALBUMS = CollectionContract(
        type = CollectionType.ALL_ALBUMS,
        category = CollectionCategory.ALBUM,
        emotion = CollectionEmotion.MINIMAL,
        priority = CollectionPriority.PRIMARY,
        visibility = CollectionVisibility.Visible(),
        personality = CollectionPersonality.MINIMAL,
        capability = CollectionCapability.LIBRARY_STRUCTURE,
        presentation = CollectionPresentation.GRID,
        constraints = CollectionConstraints.LIBRARY_STRUCTURE,
        metadata = CollectionMetadata.ALL_ALBUMS,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    val ALL_ARTISTS = CollectionContract(
        type = CollectionType.ALL_ARTISTS,
        category = CollectionCategory.ARTIST,
        emotion = CollectionEmotion.MINIMAL,
        priority = CollectionPriority.PRIMARY,
        visibility = CollectionVisibility.Visible(),
        personality = CollectionPersonality.MINIMAL,
        capability = CollectionCapability.LIBRARY_STRUCTURE,
        presentation = CollectionPresentation.GRID,
        constraints = CollectionConstraints.LIBRARY_STRUCTURE,
        metadata = CollectionMetadata.ALL_ARTISTS,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    val ALL_FOLDERS = CollectionContract(
        type = CollectionType.ALL_FOLDERS,
        category = CollectionCategory.FOLDER,
        emotion = CollectionEmotion.MINIMAL,
        priority = CollectionPriority.SECONDARY,
        visibility = CollectionVisibility.Visible(),
        personality = CollectionPersonality.MINIMAL,
        capability = CollectionCapability.LIBRARY_STRUCTURE,
        presentation = CollectionPresentation.GRID,
        constraints = CollectionConstraints.LIBRARY_STRUCTURE,
        metadata = CollectionMetadata.ALL_FOLDERS,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    // ─────────────────────────────────────────────────────────────
    // DEEP DIVE
    // ─────────────────────────────────────────────────────────────

    val ARTIST_SPOTLIGHT = CollectionContract(
        type = CollectionType.ARTIST_SPOTLIGHT,
        category = CollectionCategory.ARTIST,
        emotion = CollectionEmotion.WARM,
        priority = CollectionPriority.SECONDARY,
        visibility = CollectionVisibility.Visible(),
        personality = CollectionPersonality.TEACHER,
        capability = CollectionCapability.SMART,
        presentation = CollectionPresentation.HERO,
        constraints = CollectionConstraints.SMART,
        metadata = CollectionMetadata.ARTIST_SPOTLIGHT,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    val GENRE_DEEP_DIVE = CollectionContract(
        type = CollectionType.GENRE_DEEP_DIVE,
        category = CollectionCategory.DISCOVERY,
        emotion = CollectionEmotion.EXPLORER,
        priority = CollectionPriority.OPTIONAL,
        visibility = CollectionVisibility.Visible(),
        personality = CollectionPersonality.EXPLORER,
        capability = CollectionCapability.EXPERIMENTAL,
        presentation = CollectionPresentation.DISCOVERY,
        constraints = CollectionConstraints.DISCOVERY,
        metadata = CollectionMetadata.GENRE_DEEP_DIVE,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    val DECADE_MIX = CollectionContract(
        type = CollectionType.DECADE_MIX,
        category = CollectionCategory.DISCOVERY,
        emotion = CollectionEmotion.REFLECTIVE,
        priority = CollectionPriority.OPTIONAL,
        visibility = CollectionVisibility.Visible(),
        personality = CollectionPersonality.STORY_TELLER,
        capability = CollectionCapability.EXPERIMENTAL,
        presentation = CollectionPresentation.DISCOVERY,
        constraints = CollectionConstraints.DISCOVERY,
        metadata = CollectionMetadata.DECADE_MIX,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    val TIME_TRAVEL = CollectionContract(
        type = CollectionType.TIME_TRAVEL,
        category = CollectionCategory.HISTORY,
        emotion = CollectionEmotion.REFLECTIVE,
        priority = CollectionPriority.OPTIONAL,
        visibility = CollectionVisibility.Visible(),
        personality = CollectionPersonality.STORY_TELLER,
        capability = CollectionCapability.EXPERIMENTAL,
        presentation = CollectionPresentation.DEFAULT,
        constraints = CollectionConstraints.HISTORY,
        metadata = CollectionMetadata.TIME_TRAVEL,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    val HISTORY_FULL = CollectionContract(
        type = CollectionType.HISTORY_FULL,
        category = CollectionCategory.HISTORY,
        emotion = CollectionEmotion.MINIMAL,
        priority = CollectionPriority.BACKGROUND,
        visibility = CollectionVisibility.Visible(),
        personality = CollectionPersonality.MINIMAL,
        capability = CollectionCapability.HISTORY,
        presentation = CollectionPresentation.LIST,
        constraints = CollectionConstraints.HISTORY,
        metadata = CollectionMetadata.HISTORY_FULL,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    // ─────────────────────────────────────────────────────────────
    // EXPERIMENTAL / FUTURE
    // ─────────────────────────────────────────────────────────────

    val RIKO_RADIO = CollectionContract(
        type = CollectionType.RIKO_RADIO,
        category = CollectionCategory.SMART,
        emotion = CollectionEmotion.ALIVE,
        priority = CollectionPriority.SECONDARY,
        visibility = CollectionVisibility.Visible(),
        personality = CollectionPersonality.ENERGETIC,
        capability = CollectionCapability.EXPERIMENTAL,
        presentation = CollectionPresentation.HERO,
        constraints = CollectionConstraints.SMART,
        metadata = CollectionMetadata.RIKO_RADIO,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    val ON_THIS_DAY = CollectionContract(
        type = CollectionType.ON_THIS_DAY,
        category = CollectionCategory.HISTORY,
        emotion = CollectionEmotion.REFLECTIVE,
        priority = CollectionPriority.OPTIONAL,
        visibility = CollectionVisibility.Visible(),
        personality = CollectionPersonality.STORY_TELLER,
        capability = CollectionCapability.EXPERIMENTAL,
        presentation = CollectionPresentation.DISCOVERY,
        constraints = CollectionConstraints.HISTORY,
        metadata = CollectionMetadata.ON_THIS_DAY,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    val DIAGNOSTIC_ORPHANS = CollectionContract(
        type = CollectionType.DIAGNOSTIC_ORPHANS,
        category = CollectionCategory.EXPERIMENTAL,
        emotion = CollectionEmotion.MINIMAL,
        priority = CollectionPriority.HIDDEN,
        visibility = CollectionVisibility.DeveloperOnly,
        personality = CollectionPersonality.MINIMAL,
        capability = CollectionCapability.DIAGNOSTIC,
        presentation = CollectionPresentation.LIST,
        constraints = CollectionConstraints.DIAGNOSTIC,
        metadata = CollectionMetadata.DIAGNOSTIC_ORPHANS,
        diagnostics = CollectionDiagnosticsContract.UNDIAGNOSED,
    )

    // ─────────────────────────────────────────────────────────────
    // CATALOGUE — the index of every contract
    // ─────────────────────────────────────────────────────────────

    /**
     * The complete catalogue of all declared contracts.
     *
     * Adding a new entry to [CollectionType] without adding a
     * parallel entry here is a contract violation. The runtime
     * checks this on cold start and surfaces a diagnostic.
     */
    val ALL: List<CollectionContract> = listOf(
        RECENTLY_PLAYED, CONTINUE_LISTENING, RECENTLY_FINISHED,
        MOST_PLAYED, LEAST_PLAYED, NEVER_PLAYED,
        RECENTLY_ADDED,
        HIDDEN_GEMS, FORGOTTEN_CLASSICS,
        NIGHT_CALM, MORNING_ENERGY, EVENING_UNWIND, COMMUTE_MUSIC,
        STUDY_FOCUS, SLEEP_DRIFT, WORKOUT_PUMP,
        ROMANTIC_MOOD, MELANCHOLY_JOURNEY, INSTRUMENTAL_ESCAPE, VOCAL_ONLY,
        DISCOVERY, SIMILAR_TO, SURPRISE_ME,
        MOOD_BASED, WEATHER_BASED, TIME_BASED,
        FAVORITES, PINNED, PLAYLIST_USER, PLAYLIST_AUTO,
        ALL_SONGS, ALL_ALBUMS, ALL_ARTISTS, ALL_FOLDERS,
        ARTIST_SPOTLIGHT, GENRE_DEEP_DIVE, DECADE_MIX, TIME_TRAVEL, HISTORY_FULL,
        RIKO_RADIO, ON_THIS_DAY, DIAGNOSTIC_ORPHANS,
    )

    /** Index by stable key — for O(1) lookup by runtime. */
    private val INDEX: Map<String, CollectionContract> =
        ALL.associateBy { it.stableKey }

    /** Index by CollectionType — for type-safe lookup. */
    private val TYPE_INDEX: Map<CollectionType, CollectionContract> =
        ALL.associateBy { it.type }

    /**
     * Look up a contract by its stable string key.
     * Returns null if no contract exists for the key.
     */
    fun byKey(key: String): CollectionContract? = INDEX[key]

    /**
     * Look up a contract by its [CollectionType].
     * Returns null if no contract is declared for the type — this
     * is a contract violation and should be surfaced as a diagnostic.
     */
    fun byType(type: CollectionType): CollectionContract? = TYPE_INDEX[type]

    /**
     * All contracts visible by default on cold start — the hero feed.
     */
    val coldStartFeed: List<CollectionContract>
        get() = ALL.filter { it.isColdStartEligible }

    /**
     * All contracts that are currently behind a feature flag.
     */
    val lockedContracts: List<CollectionContract>
        get() = ALL.filter { it.isLocked }

    /**
     * All contracts in a given category.
     */
    fun inCategory(category: CollectionCategory): List<CollectionContract> =
        ALL.filter { it.category == category }

    /**
     * All contracts with a given personality.
     */
    fun withPersonality(personality: CollectionPersonality): List<CollectionContract> =
        ALL.filter { it.personality == personality }

    /**
     * All contracts with a given emotion.
     */
    fun withEmotion(emotion: CollectionEmotion): List<CollectionContract> =
        ALL.filter { it.emotion == emotion }

    /**
     * Verify the constitution is internally consistent.
     *
     * Returns the list of contracts that are MISSING from the
     * constitution despite being declared in [CollectionType].
     * An empty list means the constitution is complete.
     *
     * The runtime calls this on cold start. A non-empty result is
     * surfaced as a diagnostic warning — never as a crash.
     */
    fun verifyIntegrity(): List<CollectionType> =
        CollectionType.entries.filter { it !in TYPE_INDEX }

    /** Total contract count — for inspector display. */
    val size: Int get() = ALL.size
}
