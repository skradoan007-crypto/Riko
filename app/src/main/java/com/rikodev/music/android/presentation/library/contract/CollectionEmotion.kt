/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B1 — Collection Contract Pack v2 (Final)
 *
 * File 3 / 15 — CollectionEmotion.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Every collection in RIKO carries an emotional identity before it
 * carries a list of songs. This is what makes the Library feel
 * alive rather than indexed.
 *
 * Emotion drives:
 *   • Wave rendering parameters (intensity, speed)
 *   • Glass tint warmth
 *   • Animation curve (gentle vs snappy)
 *   • Card spacing rhythm
 *   • Empty-state copy tone
 *
 * RULE #28 — Motion Explains Hierarchy
 * RULE #29 — Empty State Has Emotion
 * RULE #37 — Collection Has Personality Before Data
 *
 * A collection's emotion is NOT the audio's emotion. It's the
 * feeling the *collection itself* wants to evoke. "Hidden Gems"
 * is dreamy — even if the songs inside are upbeat.
 *
 * No logic. Pure declaration.
 */

package com.rikodev.music.android.presentation.library.contract

/**
 * Perceived warmth of an emotion — drives glass tint and ambient glow.
 */
enum class EmotionWarmth(internal val weight: Float) {
    COLD(-1.0f),
    NEUTRAL(0.0f),
    WARM(1.0f),
}

/**
 * How strongly an emotion should drive the wave. Library wave is
 * capped at 0.3 opacity regardless (Rule #28), but the *shape*
 * still varies.
 */
enum class EmotionIntensity(internal val magnitude: Float) {
    SILENT(0.0f),
    SUBTLE(0.25f),
    GENTLE(0.5f),
    PRESENT(0.75f),
    DOMINANT(1.0f),
}

/**
 * The 21 emotional identities a RIKO collection can carry.
 *
 * Pairs and oppositions are declared so the engine (B3) can avoid
 * surfacing two contradictory collections side-by-side.
 */
enum class CollectionEmotion(
    val displayName: String,
    val internalId: String,
    val description: String,
    val warmth: EmotionWarmth,
    val intensity: EmotionIntensity,
    val animationSpeed: Float,
    val glassStrength: Float,
    val sampleCollectionIds: List<String>,
    val pairsWith: List<String>,
    val opposes: List<String>,
) {

    CALM(
        displayName = "Calm",
        internalId = "calm",
        description = "Still waters. Slow tempo, low wave, soft glass.",
        warmth = EmotionWarmth.NEUTRAL,
        intensity = EmotionIntensity.SUBTLE,
        animationSpeed = 0.5f,
        glassStrength = 0.4f,
        sampleCollectionIds = listOf("night_calm", "sleep_drift"),
        pairsWith = listOf("warm", "minimal", "dreamy"),
        opposes = listOf("powerful", "alive"),
    ),

    WARM(
        displayName = "Warm",
        internalId = "warm",
        description = "Like late afternoon sun through a window.",
        warmth = EmotionWarmth.WARM,
        intensity = EmotionIntensity.GENTLE,
        animationSpeed = 0.7f,
        glassStrength = 0.6f,
        sampleCollectionIds = listOf("favorites", "romantic_mood"),
        pairsWith = listOf("calm", "organic", "sunset"),
        opposes = listOf("cold", "minimal"),
    ),

    COLD(
        displayName = "Cold",
        internalId = "cold",
        description = "Glass and chrome. Minimal warmth, high clarity.",
        warmth = EmotionWarmth.COLD,
        intensity = EmotionIntensity.GENTLE,
        animationSpeed = 0.8f,
        glassStrength = 0.7f,
        sampleCollectionIds = listOf("vocal_only"),
        pairsWith = listOf("minimal", "focused"),
        opposes = listOf("warm", "organic"),
    ),

    FOCUSED(
        displayName = "Focused",
        internalId = "focused",
        description = "Clarity over warmth. Engineered for sustained attention.",
        warmth = EmotionWarmth.NEUTRAL,
        intensity = EmotionIntensity.SUBTLE,
        animationSpeed = 0.4f,
        glassStrength = 0.3f,
        sampleCollectionIds = listOf("study_focus", "instrumental_escape"),
        pairsWith = listOf("minimal", "cold"),
        opposes = listOf("alive", "powerful"),
    ),

    DREAMY(
        displayName = "Dreamy",
        internalId = "dreamy",
        description = "Hazy edges, slow drift, ambient glow.",
        warmth = EmotionWarmth.WARM,
        intensity = EmotionIntensity.PRESENT,
        animationSpeed = 0.4f,
        glassStrength = 0.8f,
        sampleCollectionIds = listOf("hidden_gems", "sleep_drift"),
        pairsWith = listOf("floating", "calm"),
        opposes = listOf("focused", "minimal"),
    ),

    POWERFUL(
        displayName = "Powerful",
        internalId = "powerful",
        description = "Forward momentum. Snappy motion, brighter accents.",
        warmth = EmotionWarmth.WARM,
        intensity = EmotionIntensity.DOMINANT,
        animationSpeed = 1.2f,
        glassStrength = 0.5f,
        sampleCollectionIds = listOf("most_played", "workout_pump"),
        pairsWith = listOf("alive", "morning"),
        opposes = listOf("calm", "dreamy"),
    ),

    DARK(
        displayName = "Dark",
        internalId = "dark",
        description = "Low-key, late-night, intimate.",
        warmth = EmotionWarmth.COLD,
        intensity = EmotionIntensity.GENTLE,
        animationSpeed = 0.6f,
        glassStrength = 0.7f,
        sampleCollectionIds = listOf("melancholy_journey"),
        pairsWith = listOf("reflective", "night"),
        opposes = listOf("bright", "morning"),
    ),

    BRIGHT(
        displayName = "Bright",
        internalId = "bright",
        description = "Daylight. Open, optimistic, energetic.",
        warmth = EmotionWarmth.WARM,
        intensity = EmotionIntensity.PRESENT,
        animationSpeed = 1.0f,
        glassStrength = 0.5f,
        sampleCollectionIds = listOf("recently_added", "morning_energy"),
        pairsWith = listOf("alive", "morning"),
        opposes = listOf("dark", "melancholy"),
    ),

    NIGHT(
        displayName = "Night",
        internalId = "night",
        description = "Quiet hours. Low light, soft pulse.",
        warmth = EmotionWarmth.COLD,
        intensity = EmotionIntensity.SUBTLE,
        animationSpeed = 0.5f,
        glassStrength = 0.7f,
        sampleCollectionIds = listOf("night_calm", "sleep_drift"),
        pairsWith = listOf("calm", "dreamy"),
        opposes = listOf("morning", "bright"),
    ),

    MORNING(
        displayName = "Morning",
        internalId = "morning",
        description = "Fresh start. Light, awakening, optimistic.",
        warmth = EmotionWarmth.WARM,
        intensity = EmotionIntensity.PRESENT,
        animationSpeed = 1.0f,
        glassStrength = 0.5f,
        sampleCollectionIds = listOf("morning_energy"),
        pairsWith = listOf("bright", "alive"),
        opposes = listOf("night", "dark"),
    ),

    SUNSET(
        displayName = "Sunset",
        internalId = "sunset",
        description = "The slow cool-down between day and night.",
        warmth = EmotionWarmth.WARM,
        intensity = EmotionIntensity.GENTLE,
        animationSpeed = 0.7f,
        glassStrength = 0.6f,
        sampleCollectionIds = listOf("evening_unwind"),
        pairsWith = listOf("warm", "reflective"),
        opposes = listOf("morning", "alive"),
    ),

    RAIN(
        displayName = "Rain",
        internalId = "rain",
        description = "Soft, persistent, meditative.",
        warmth = EmotionWarmth.COLD,
        intensity = EmotionIntensity.SUBTLE,
        animationSpeed = 0.6f,
        glassStrength = 0.7f,
        sampleCollectionIds = listOf("instrumental_escape"),
        pairsWith = listOf("calm", "dreamy"),
        opposes = listOf("fire", "bright"),
    ),

    FIRE(
        displayName = "Fire",
        internalId = "fire",
        description = "Heat and motion. Loud, present, alive.",
        warmth = EmotionWarmth.WARM,
        intensity = EmotionIntensity.DOMINANT,
        animationSpeed = 1.3f,
        glassStrength = 0.4f,
        sampleCollectionIds = listOf("workout_pump"),
        pairsWith = listOf("powerful", "alive"),
        opposes = listOf("rain", "calm"),
    ),

    OCEAN(
        displayName = "Ocean",
        internalId = "ocean",
        description = "Vast, deep, unhurried.",
        warmth = EmotionWarmth.NEUTRAL,
        intensity = EmotionIntensity.GENTLE,
        animationSpeed = 0.5f,
        glassStrength = 0.6f,
        sampleCollectionIds = listOf("commute_music"),
        pairsWith = listOf("floating", "calm"),
        opposes = listOf("fire", "powerful"),
    ),

    GLASS(
        displayName = "Glass",
        internalId = "glass",
        description = "Translucent, structured, exact.",
        warmth = EmotionWarmth.COLD,
        intensity = EmotionIntensity.SUBTLE,
        animationSpeed = 0.7f,
        glassStrength = 0.9f,
        sampleCollectionIds = listOf("study_focus"),
        pairsWith = listOf("minimal", "focused"),
        opposes = listOf("organic", "warm"),
    ),

    MINIMAL(
        displayName = "Minimal",
        internalId = "minimal",
        description = "Bare essentials. Negative space speaks.",
        warmth = EmotionWarmth.NEUTRAL,
        intensity = EmotionIntensity.SILENT,
        animationSpeed = 0.5f,
        glassStrength = 0.3f,
        sampleCollectionIds = listOf("all_songs", "instrumental_escape"),
        pairsWith = listOf("focused", "cold"),
        opposes = listOf("alive", "powerful"),
    ),

    PREMIUM(
        displayName = "Premium",
        internalId = "premium",
        description = "Curated, weighted, deliberate. Not flashy — confident.",
        warmth = EmotionWarmth.WARM,
        intensity = EmotionIntensity.GENTLE,
        animationSpeed = 0.7f,
        glassStrength = 0.7f,
        sampleCollectionIds = listOf("favorites"),
        pairsWith = listOf("warm", "organic"),
        opposes = listOf("minimal", "cold"),
    ),

    ORGANIC(
        displayName = "Organic",
        internalId = "organic",
        description = "Natural rhythm, breathing motion, hand-made feel.",
        warmth = EmotionWarmth.WARM,
        intensity = EmotionIntensity.GENTLE,
        animationSpeed = 0.6f,
        glassStrength = 0.5f,
        sampleCollectionIds = listOf("pinned", "playlist_user"),
        pairsWith = listOf("warm", "calm"),
        opposes = listOf("glass", "cold"),
    ),

    ALIVE(
        displayName = "Alive",
        internalId = "alive",
        description = "Moving, surprising, unpredictable.",
        warmth = EmotionWarmth.WARM,
        intensity = EmotionIntensity.DOMINANT,
        animationSpeed = 1.2f,
        glassStrength = 0.5f,
        sampleCollectionIds = listOf("surprise_me", "riko_radio"),
        pairsWith = listOf("powerful", "bright"),
        opposes = listOf("calm", "minimal"),
    ),

    FLOATING(
        displayName = "Floating",
        internalId = "floating",
        description = "Weightless. Drift without arrival.",
        warmth = EmotionWarmth.NEUTRAL,
        intensity = EmotionIntensity.GENTLE,
        animationSpeed = 0.5f,
        glassStrength = 0.6f,
        sampleCollectionIds = listOf("discovery", "similar_to", "commute_music"),
        pairsWith = listOf("dreamy", "ocean"),
        opposes = listOf("focused", "powerful"),
    ),

    REFLECTIVE(
        displayName = "Reflective",
        internalId = "reflective",
        description = "Looking back. Slow, weighted, meaningful.",
        warmth = EmotionWarmth.NEUTRAL,
        intensity = EmotionIntensity.GENTLE,
        animationSpeed = 0.5f,
        glassStrength = 0.6f,
        sampleCollectionIds = listOf("forgotten_classics", "time_travel", "on_this_day"),
        pairsWith = listOf("sunset", "dreamy"),
        opposes = listOf("bright", "alive"),
    ),

    GENTLE(
        displayName = "Gentle",
        internalId = "gentle",
        description = "Soft, kind, unhurried. The voice of RIKO itself.",
        warmth = EmotionWarmth.WARM,
        intensity = EmotionIntensity.SUBTLE,
        animationSpeed = 0.6f,
        glassStrength = 0.5f,
        sampleCollectionIds = listOf("continue_listening"),
        pairsWith = listOf("warm", "calm"),
        opposes = listOf("powerful", "alive"),
    ),

    /**
     * Reserved for collections whose emotion is determined at runtime
     * (e.g. MoodBased, WeatherBased, TimeBased). The contract cannot
     * predict the emotion; it must defer to the engine.
     */
    ADAPTIVE(
        displayName = "Adaptive",
        internalId = "adaptive",
        description = "Emotion resolved by the engine at runtime, " +
            "based on live context.",
        warmth = EmotionWarmth.NEUTRAL,
        intensity = EmotionIntensity.GENTLE,
        animationSpeed = 0.8f,
        glassStrength = 0.5f,
        sampleCollectionIds = listOf("mood_based", "weather_based", "time_based"),
        pairsWith = emptyList(),
        opposes = emptyList(),
    ),

    /**
     * Reserved for collections whose engine is not yet designed —
     * e.g. GenreDeepDive (depends on genre detection that ships later).
     */
    EXPLORER(
        displayName = "Explorer",
        internalId = "explorer",
        description = "Curious, investigative. The user is on a journey.",
        warmth = EmotionWarmth.WARM,
        intensity = EmotionIntensity.PRESENT,
        animationSpeed = 0.9f,
        glassStrength = 0.5f,
        sampleCollectionIds = listOf("genre_deep_dive"),
        pairsWith = listOf("alive", "floating"),
        opposes = listOf("calm", "reflective"),
    ),

    ;

    val stableKey: String get() = internalId

    /** True if this emotion pairs *well* with another (no clash). */
    fun pairsWith(other: CollectionEmotion): Boolean = other.internalId in pairsWith

    /** True if surfacing this emotion next to [other] would feel off. */
    fun opposes(other: CollectionEmotion): Boolean = other.internalId in opposes
}
