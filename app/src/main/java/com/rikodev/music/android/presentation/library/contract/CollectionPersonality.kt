/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B1 — Collection Contract Pack v2 (Final)
 *
 * File 6 / 15 — CollectionPersonality.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * A collection's *personality* is the voice it speaks in. When RIKO
 * writes copy for an empty state, a tooltip, or a refresh hint, the
 * personality decides tone, cadence, and vocabulary.
 *
 * Personality is the RIKO-unique layer most music apps don't have.
 * It is what makes "Hidden Gems" feel like a quiet curator and
 * "Surprise Me" feel like an excitable friend.
 *
 * RULE #37 — Collection Has Personality Before Data
 * RULE #29 — Empty State Has Emotion
 *
 * Personality drives:
 *   • Empty-state copy tone (Gentle vs Energetic vs Reflective)
 *   • Header subtitle voice (formal vs casual)
 *   • Card animation style (snappy vs drifting)
 *   • Card layout preference (grid vs strip)
 *
 * No logic. Pure declaration.
 */

package com.rikodev.music.android.presentation.library.contract

/**
 * Layout preference for cards in a collection.
 */
enum class CardLayoutPreference(internal val description: String) {
    /** Horizontal scrolling strip — cards 1.5× tall as wide. */
    HERO_STRIP("Horizontal hero strip — large cards, one per screen-width peek."),

    /** Tighter horizontal strip — 2 cards visible at a time. */
    COMPACT_STRIP("Horizontal compact strip — 2–3 cards visible at once."),

    /** 2-column grid of square cards. */
    GRID_SQUARE("2-column square grid."),

    /** 3-column grid of small square cards. */
    GRID_DENSE("3-column dense square grid."),

    /** Vertical list — full-width cards. */
    LIST_VERTICAL("Vertical list — full-width rows."),

    /** Stacked cards with parallax depth. */
    STACK_DEPTH("Stacked hero cards with parallax depth."),
}

/**
 * Animation cadence — drives spring damping and duration.
 */
enum class AnimationCadence(internal val damping: Float, internal val duration: Float) {
    SNAPPY(0.8f, 200f),
    BALANCED(0.7f, 280f),
    GENTLE(0.6f, 360f),
    DREAMY(0.5f, 480f),
    DRIFTING(0.4f, 640f),
}

/**
 * Voice tone — drives copy and tooltip wording.
 */
enum class VoiceTone(internal val description: String) {
    QUIET("Understated. Suggests, never insists."),
    WARM("Conversational. Speaks like a friend who knows your taste."),
    CONFIDENT("Direct. Knows what it's offering and why."),
    CURIOUS("Inquisitive. Asks before it asserts."),
    REFLECTIVE("Slow and weighted. Speaks in past or future tense."),
    PLAYFUL("Light. Surprises and delights with word choice."),
    MINIMAL("Says almost nothing. Lets the music speak."),
    TECHNICAL("Precise. Uses metrics, ratios, percentages."),
}

/**
 * The nine personalities a RIKO collection can have.
 *
 * Personalities are *not* user-configurable — they are intrinsic
 * to the collection's purpose. ContinueListening is always Gentle.
 * Discovery is always Curious. This is what gives the Library its
 * character as a whole.
 */
enum class CollectionPersonality(
    val displayName: String,
    val internalId: String,
    val description: String,
    val voiceTone: VoiceTone,
    val cadence: AnimationCadence,
    val preferredLayout: CardLayoutPreference,
    val idealForCollections: List<String>,
    val emptyStateStyle: EmptyStateStyle,
    val refreshHintStyle: RefreshHintStyle,
) {

    GENTLE(
        displayName = "Gentle",
        internalId = "gentle",
        description = "Quiet, kind, unhurried. The voice of RIKO itself.",
        voiceTone = VoiceTone.QUIET,
        cadence = AnimationCadence.GENTLE,
        preferredLayout = CardLayoutPreference.HERO_STRIP,
        idealForCollections = listOf("continue_listening", "recently_finished"),
        emptyStateStyle = EmptyStateStyle.SOFT_SUGGESTION,
        refreshHintStyle = RefreshHintStyle.WHISPER,
    ),

    EXPLORER(
        displayName = "Explorer",
        internalId = "explorer",
        description = "Curious and forward-leaning. Loves the unknown.",
        voiceTone = VoiceTone.CURIOUS,
        cadence = AnimationCadence.BALANCED,
        preferredLayout = CardLayoutPreference.COMPACT_STRIP,
        idealForCollections = listOf("discovery", "similar_to", "genre_deep_dive"),
        emptyStateStyle = EmptyStateStyle.OPEN_QUESTION,
        refreshHintStyle = RefreshHintStyle.GENTLE_INVITATION,
    ),

    TEACHER(
        displayName = "Teacher",
        internalId = "teacher",
        description = "Explains the why. Patient, generous with context.",
        voiceTone = VoiceTone.CONFIDENT,
        cadence = AnimationCadence.BALANCED,
        preferredLayout = CardLayoutPreference.LIST_VERTICAL,
        idealForCollections = listOf("artist_spotlight", "decade_mix"),
        emptyStateStyle = EmptyStateStyle.CONTEXTUAL_HINT,
        refreshHintStyle = RefreshHintStyle.EXPLANATORY,
    ),

    STORY_TELLER(
        displayName = "Storyteller",
        internalId = "storyteller",
        description = "Speaks in past and future tense. Weaves time.",
        voiceTone = VoiceTone.REFLECTIVE,
        cadence = AnimationCadence.DREAMY,
        preferredLayout = CardLayoutPreference.STACK_DEPTH,
        idealForCollections = listOf("time_travel", "on_this_day", "forgotten_classics"),
        emptyStateStyle = EmptyStateStyle.NOSTALGIC_PROMPT,
        refreshHintStyle = RefreshHintStyle.WHISPER,
    ),

    MINIMAL(
        displayName = "Minimal",
        internalId = "minimal",
        description = "Says almost nothing. Lets the music speak.",
        voiceTone = VoiceTone.MINIMAL,
        cadence = AnimationCadence.SNAPPY,
        preferredLayout = CardLayoutPreference.LIST_VERTICAL,
        idealForCollections = listOf("all_songs", "all_albums", "all_artists", "all_folders"),
        emptyStateStyle = EmptyStateStyle.FACTUAL,
        refreshHintStyle = RefreshHintStyle.NONE,
    ),

    COLLECTOR(
        displayName = "Collector",
        internalId = "collector",
        description = "Catalogues and curates. Proud of the inventory.",
        voiceTone = VoiceTone.TECHNICAL,
        cadence = AnimationCadence.BALANCED,
        preferredLayout = CardLayoutPreference.GRID_SQUARE,
        idealForCollections = listOf("favorites", "pinned", "playlist_user"),
        emptyStateStyle = EmptyStateStyle.FACTUAL,
        refreshHintStyle = RefreshHintStyle.EXPLANATORY,
    ),

    COMPANION(
        displayName = "Companion",
        internalId = "companion",
        description = "Walks beside the user. Warm, present, never insistent.",
        voiceTone = VoiceTone.WARM,
        cadence = AnimationCadence.GENTLE,
        preferredLayout = CardLayoutPreference.HERO_STRIP,
        idealForCollections = listOf("night_calm", "morning_energy", "evening_unwind", "sleep_drift"),
        emptyStateStyle = EmptyStateStyle.SOFT_SUGGESTION,
        refreshHintStyle = RefreshHintStyle.WHISPER,
    ),

    ENERGETIC(
        displayName = "Energetic",
        internalId = "energetic",
        description = "Forward momentum. Speaks quickly, moves quickly.",
        voiceTone = VoiceTone.PLAYFUL,
        cadence = AnimationCadence.SNAPPY,
        preferredLayout = CardLayoutPreference.COMPACT_STRIP,
        idealForCollections = listOf("most_played", "workout_pump", "surprise_me", "riko_radio"),
        emptyStateStyle = EmptyStateStyle.PLAYFUL_PROMPT,
        refreshHintStyle = RefreshHintStyle.GENTLE_INVITATION,
    ),

    REFLECTIVE(
        displayName = "Reflective",
        internalId = "reflective",
        description = "Slow, weighted, meaningful. The thinking voice.",
        voiceTone = VoiceTone.REFLECTIVE,
        cadence = AnimationCadence.DREAMY,
        preferredLayout = CardLayoutPreference.GRID_SQUARE,
        idealForCollections = listOf("forgotten_classics", "history_full", "least_played"),
        emptyStateStyle = EmptyStateStyle.NOSTALGIC_PROMPT,
        refreshHintStyle = RefreshHintStyle.WHISPER,
    ),

    ;

    val stableKey: String get() = internalId
}

/**
 * Copy tone for empty-state messages.
 */
enum class EmptyStateStyle(internal val description: String) {
    SOFT_SUGGESTION("Gentle prompt — 'maybe try adding a few favorites?'"),
    OPEN_QUESTION("Curious prompt — 'what would you like to discover?'"),
    CONTEXTUAL_HINT("Explanatory — 'this collection appears once you have ≥5 finished tracks.'"),
    NOSTALGIC_PROMPT("Memory-led — 'once you've played a few, this is where they'll live.'"),
    FACTUAL("Plain statement — 'No songs yet.'"),
    PLAYFUL_PROMPT("Light and friendly — 'nothing here yet — let's fix that!'"),
}

/**
 * Copy tone for refresh hints (the small subtitle that explains
 * when a smart collection was last regenerated).
 */
enum class RefreshHintStyle(internal val description: String) {
    WHISPER("Subtle, low-emphasis — 'refreshed 3h ago'."),
    GENTLE_INVITATION("Friendly — 'tap to refresh' with a small icon."),
    EXPLANATORY("Educational — 'we re-rank this every time you finish a track'."),
    NONE("No refresh hint shown."),
}
