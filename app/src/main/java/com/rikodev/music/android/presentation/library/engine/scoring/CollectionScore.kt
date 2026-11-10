/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B3 — Collection Engine
 *
 * File 3 / 14 — CollectionScore.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * The data shape for "how much does this item deserve to be in this
 * collection?".
 *
 * Each candidate item (song, album, artist, playlist) receives a
 * [CollectionScore] from each [Scorer]. The [ScoreComposer] then
 * combines these per-scorer results into a single final score that
 * the [CollectionRanker] uses to sort items.
 *
 * DESIGN
 * ------
 * A score is NOT a single number. It is:
 *   • A primary float value (0.0 → 1.0, normalized)
 *   • A breakdown by signal (so we can explain WHY)
 *   • A weight (how much this signal matters for THIS collection)
 *   • A confidence (how reliable the signal is — low if data missing)
 *
 * Why confidence matters: on cold start, we have no play history.
 * Frequency score would be 0, but confidence is also 0. The composer
 * must know to fall back to recency-of-added or random sampling
 * rather than trusting a 0 frequency score.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #42  Diagnostics Is First-Class — every score is explainable
 * #46  Runtime Never Creates Identity — score never invents ids
 * #47  Engine Predicts — scoring IS the prediction
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Android. No IO. Pure data + math.
 *
 * © RIKO Experience OS — Engine Tier
 */

package com.rikodev.music.android.presentation.library.engine.scoring

/**
 * The kind of signal a [Scorer] produces. Used as a stable key in
 * the [CollectionScore.breakdown] map so the Inspector UI can show
 * "why did this item rank here?".
 *
 * RULE #44 — these internalIds are stable forever. Renaming any of
 * them breaks persisted score explanations.
 */
enum class ScoringSignal(
    val internalId: String,
    val displayName: String,
    val description: String,
) {
    /** How often the item has been played. */
    FREQUENCY(
        internalId = "frequency",
        displayName = "Frequency",
        description = "How often the user plays this item.",
    ),

    /** Whether the user typically finishes the item or skips. */
    COMPLETION(
        internalId = "completion",
        displayName = "Completion",
        description = "Ratio of times the user finished vs. skipped.",
    ),

    /** Negative signal — how often the user skips. */
    SKIP(
        internalId = "skip",
        displayName = "Skip Rate",
        description = "How often the user skips this item. Lower is better.",
    ),

    /** How recently the item was played. */
    RECENCY(
        internalId = "recency",
        displayName = "Recency",
        description = "How recently the user played this item.",
    ),

    /** Whether the user explicitly favourited the item. */
    FAVORITE(
        internalId = "favorite",
        displayName = "Favourite",
        description = "Whether the user marked this item as favourite.",
    ),

    /** Whether the item is part of the current listening session. */
    SESSION(
        internalId = "session",
        displayName = "Session",
        description = "Whether the item is part of the current session.",
    ),

    /** How recently the item was added to the library. */
    FRESHNESS(
        internalId = "freshness",
        displayName = "Freshness",
        description = "How recently the item was added to the library.",
    ),

    /** Diversity boost — used to ensure variety in a collection. */
    DIVERSITY(
        internalId = "diversity",
        displayName = "Diversity",
        description = "Boost to ensure variety across artists/albums.",
    ),

    /** Mood-based match — derived from mood tags / time of day. */
    MOOD(
        internalId = "mood",
        displayName = "Mood",
        description = "Match between the item's mood and the current context.",
    ),

    /** Random component — used to break ties and surface variety. */
    ENTROPY(
        internalId = "entropy",
        displayName = "Entropy",
        description = "Controlled randomness to break ties and surface variety.",
    );

    companion object {
        /** Lookup by stable id, or null. */
        fun byId(internalId: String): ScoringSignal? =
            entries.firstOrNull { it.internalId == internalId }
    }
}

/**
 * A single signal's contribution to an item's final score.
 *
 * One [Scorer] produces one [SignalContribution] per item.
 */
data class SignalContribution(
    /** Which signal produced this contribution. */
    val signal: ScoringSignal,
    /** Raw score in 0.0 → 1.0 range (after normalization). */
    val rawScore: Float,
    /** Weight applied to this signal for THIS collection. 0.0 → 1.0. */
    val weight: Float,
    /** Confidence in the score. 0.0 = no data, 1.0 = full data. */
    val confidence: Float,
    /** Final weighted contribution = rawScore × weight × confidence. */
    val weightedScore: Float,
    /** Optional note explaining the score (e.g. "played 23 times"). */
    val note: String?,
) {
    val isConfident: Boolean get() = confidence >= 0.5f
    val isZero: Boolean get() = weightedScore == 0f

    companion object {
        /** Empty contribution — used when a signal doesn't apply. */
        fun zero(signal: ScoringSignal): SignalContribution = SignalContribution(
            signal = signal,
            rawScore = 0f,
            weight = 0f,
            confidence = 0f,
            weightedScore = 0f,
            note = null,
        )
    }
}

/**
 * The complete score for a single candidate item.
 *
 * Produced by [ScoreComposer] after combining all [SignalContribution]s.
 */
data class CollectionScore(
    /** Stable id of the item being scored (songId, albumId, etc.). Rule #44. */
    val itemId: String,
    /** Per-signal contributions, keyed by [ScoringSignal]. */
    val breakdown: Map<ScoringSignal, SignalContribution>,
    /** Final blended score in 0.0 → 1.0 range. */
    val finalScore: Float,
    /** Overall confidence in the final score. 0.0 → 1.0. */
    val confidence: Float,
    /** Top 3 reasons (signal names) for this score — for Inspector UI. */
    val topReasons: List<String>,
    /** Whether this score had any non-zero signal. */
    val hasAnySignal: Boolean,
) {

    /** Look up a specific signal's contribution. */
    fun contributionFor(signal: ScoringSignal): SignalContribution? =
        breakdown[signal]

    /** Convenience — the highest-weighted signal. */
    val dominantSignal: ScoringSignal?
        get() = breakdown.values
            .filter { it.weight > 0f }
            .maxByOrNull { it.weightedScore }
            ?.signal

    companion object {

        /** A zero score — used for items that should never surface. */
        fun zero(itemId: String): CollectionScore = CollectionScore(
            itemId = itemId,
            breakdown = emptyMap(),
            finalScore = 0f,
            confidence = 0f,
            topReasons = emptyList(),
            hasAnySignal = false,
        )

        /**
         * Build a score from a list of contributions.
         * The final score is the weighted sum, normalized by total weight.
         */
        fun compose(
            itemId: String,
            contributions: List<SignalContribution>,
        ): CollectionScore {
            if (contributions.isEmpty()) return zero(itemId)

            val breakdown = contributions.associateBy { it.signal }
            val totalWeight = contributions.sumOf { it.weight.toDouble() }.toFloat()
            val weightedSum = contributions.sumOf { it.weightedScore.toDouble() }.toFloat()
            val finalScore = if (totalWeight <= 0f) 0f else (weightedSum / totalWeight).coerceIn(0f, 1f)
            val confidence = if (contributions.isEmpty()) 0f
                else contributions.map { it.confidence }.average().toFloat()

            val topReasons = contributions
                .filter { it.weightedScore > 0f }
                .sortedByDescending { it.weightedScore }
                .take(3)
                .map { c ->
                    val note = c.note?.let { " — $it" } ?: ""
                    "${c.signal.displayName}${note}"
                }

            return CollectionScore(
                itemId = itemId,
                breakdown = breakdown,
                finalScore = finalScore,
                confidence = confidence,
                topReasons = topReasons,
                hasAnySignal = contributions.any { it.weightedScore > 0f },
            )
        }
    }
}

/**
 * The weight profile for a collection.
 *
 * Different collections care about different signals. "Recently Played"
 * cares mostly about RECENCY. "Favourites" cares mostly about FAVORITE.
 * "Hidden Gems" cares about FREQUENCY (low) + COMPLETION (high).
 *
 * Each strategy configures a [ScoreProfile] that the [ScoreComposer]
 * uses to weight each scorer's output.
 */
data class ScoreProfile(
    /** Stable key — matches the collection this profile is for. Rule #44. */
    val collectionStableKey: String,
    /** Weight for FREQUENCY signal (0.0 → 1.0). */
    val frequencyWeight: Float,
    /** Weight for COMPLETION signal. */
    val completionWeight: Float,
    /** Weight for SKIP signal (applied as negative). */
    val skipWeight: Float,
    /** Weight for RECENCY signal. */
    val recencyWeight: Float,
    /** Weight for FAVORITE signal. */
    val favoriteWeight: Float,
    /** Weight for SESSION signal. */
    val sessionWeight: Float,
    /** Weight for FRESHNESS signal. */
    val freshnessWeight: Float,
    /** Weight for DIVERSITY signal. */
    val diversityWeight: Float,
    /** Weight for MOOD signal. */
    val moodWeight: Float,
    /** Weight for ENTROPY signal. */
    val entropyWeight: Float,
    /** Minimum confidence threshold below which the engine falls back. */
    val minConfidenceThreshold: Float,
) {

    /** Returns all non-zero weights as a map of signal → weight. */
    fun weights(): Map<ScoringSignal, Float> = mapOf(
        ScoringSignal.FREQUENCY to frequencyWeight,
        ScoringSignal.COMPLETION to completionWeight,
        ScoringSignal.SKIP to skipWeight,
        ScoringSignal.RECENCY to recencyWeight,
        ScoringSignal.FAVORITE to favoriteWeight,
        ScoringSignal.SESSION to sessionWeight,
        ScoringSignal.FRESHNESS to freshnessWeight,
        ScoringSignal.DIVERSITY to diversityWeight,
        ScoringSignal.MOOD to moodWeight,
        ScoringSignal.ENTROPY to entropyWeight,
    ).filterValues { it > 0f }

    val totalWeight: Float
        get() = frequencyWeight + completionWeight + skipWeight +
            recencyWeight + favoriteWeight + sessionWeight +
            freshnessWeight + diversityWeight + moodWeight + entropyWeight

    companion object {

        /**
         * Build a profile from a sparse map of signal → weight.
         * Missing signals default to 0.
         */
        fun fromWeights(
            collectionStableKey: String,
            weights: Map<ScoringSignal, Float>,
            minConfidenceThreshold: Float = 0.3f,
        ): ScoreProfile = ScoreProfile(
            collectionStableKey = collectionStableKey,
            frequencyWeight = weights[ScoringSignal.FREQUENCY] ?: 0f,
            completionWeight = weights[ScoringSignal.COMPLETION] ?: 0f,
            skipWeight = weights[ScoringSignal.SKIP] ?: 0f,
            recencyWeight = weights[ScoringSignal.RECENCY] ?: 0f,
            favoriteWeight = weights[ScoringSignal.FAVORITE] ?: 0f,
            sessionWeight = weights[ScoringSignal.SESSION] ?: 0f,
            freshnessWeight = weights[ScoringSignal.FRESHNESS] ?: 0f,
            diversityWeight = weights[ScoringSignal.DIVERSITY] ?: 0f,
            moodWeight = weights[ScoringSignal.MOOD] ?: 0f,
            entropyWeight = weights[ScoringSignal.ENTROPY] ?: 0f,
            minConfidenceThreshold = minConfidenceThreshold,
        )

        /** Preset: recency-dominated (e.g. Recently Played). */
        fun recencyHeavy(stableKey: String) = fromWeights(
            stableKey,
            mapOf(
                ScoringSignal.RECENCY to 0.65f,
                ScoringSignal.FREQUENCY to 0.15f,
                ScoringSignal.COMPLETION to 0.10f,
                ScoringSignal.SKIP to 0.05f,
                ScoringSignal.ENTROPY to 0.05f,
            ),
        )

        /** Preset: favorite-dominated (e.g. Favourites). */
        fun favoriteHeavy(stableKey: String) = fromWeights(
            stableKey,
            mapOf(
                ScoringSignal.FAVORITE to 0.70f,
                ScoringSignal.FREQUENCY to 0.15f,
                ScoringSignal.RECENCY to 0.10f,
                ScoringSignal.ENTROPY to 0.05f,
            ),
        )

        /** Preset: low-frequency + high-completion (e.g. Hidden Gems). */
        fun hiddenGems(stableKey: String) = fromWeights(
            stableKey,
            mapOf(
                ScoringSignal.FREQUENCY to 0.30f,  // inverted below
                ScoringSignal.COMPLETION to 0.30f,
                ScoringSignal.FRESHNESS to 0.15f,
                ScoringSignal.DIVERSITY to 0.15f,
                ScoringSignal.ENTROPY to 0.10f,
            ),
        )

        /** Preset: long-ago played (e.g. Forgotten Favourites). */
        fun forgotten(stableKey: String) = fromWeights(
            stableKey,
            mapOf(
                ScoringSignal.RECENCY to 0.40f,  // inverted below
                ScoringSignal.FAVORITE to 0.25f,
                ScoringSignal.FREQUENCY to 0.20f,
                ScoringSignal.COMPLETION to 0.10f,
                ScoringSignal.ENTROPY to 0.05f,
            ),
        )

        /** Preset: mood-dominated (e.g. Evening, Focus). */
        fun moodDriven(stableKey: String) = fromWeights(
            stableKey,
            mapOf(
                ScoringSignal.MOOD to 0.45f,
                ScoringSignal.FAVORITE to 0.20f,
                ScoringSignal.COMPLETION to 0.15f,
                ScoringSignal.DIVERSITY to 0.10f,
                ScoringSignal.ENTROPY to 0.10f,
            ),
        )

        /** Preset: discovery — freshness + diversity + entropy. */
        fun discovery(stableKey: String) = fromWeights(
            stableKey,
            mapOf(
                ScoringSignal.FRESHNESS to 0.35f,
                ScoringSignal.DIVERSITY to 0.25f,
                ScoringSignal.ENTROPY to 0.20f,
                ScoringSignal.FREQUENCY to 0.10f,  // inverted
                ScoringSignal.MOOD to 0.10f,
            ),
        )

        /** Preset: continue listening — session + recency. */
        fun continueListening(stableKey: String) = fromWeights(
            stableKey,
            mapOf(
                ScoringSignal.SESSION to 0.45f,
                ScoringSignal.RECENCY to 0.30f,
                ScoringSignal.FREQUENCY to 0.15f,
                ScoringSignal.COMPLETION to 0.10f,
            ),
        )

        /** Preset: balanced (default for unknown collections). */
        fun balanced(stableKey: String) = fromWeights(
            stableKey,
            mapOf(
                ScoringSignal.FREQUENCY to 0.20f,
                ScoringSignal.COMPLETION to 0.15f,
                ScoringSignal.RECENCY to 0.20f,
                ScoringSignal.FAVORITE to 0.15f,
                ScoringSignal.DIVERSITY to 0.15f,
                ScoringSignal.ENTROPY to 0.15f,
            ),
        )
    }
}
