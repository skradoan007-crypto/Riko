/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B3 — Collection Engine
 *
 * File 5 / 14 — ScoreComposer.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Takes raw [SignalContribution]s from the 10 scorers + a
 * [ScoreProfile] (which says how much each signal matters for THIS
 * collection) and produces a single [CollectionScore] per item.
 *
 * This is the LAST step before the ranker takes over. After this:
 *   • Each item has a final 0.0 → 1.0 score
 *   • Each item has a confidence
 *   • Each item has top-3 reasons for the Inspector UI
 *
 * DESIGN
 * ------
 * The composer is stateless and pure. Given the same inputs, it
 * always returns the same outputs.
 *
 * It handles 3 tricky cases:
 *
 *   1. LOW CONFIDENCE FALLBACK
 *      If the dominant signal has confidence < threshold, the composer
 *      boosts ENTROPY weight so the ranker can surface variety
 *      instead of trusting unreliable data.
 *
 *   2. NEGATIVE SIGNAL INVERSION
 *      SKIP and (for HiddenGems) FREQUENCY are negative signals.
 *      The scorers already return inverted raw scores (high skip →
 *      low score), so the composer can treat them uniformly.
 *
 *   3. ZERO-WEIGHT COLLECTIONS
 *      If the profile has zero total weight (misconfiguration), the
 *      composer emits a zero score with a warning. The strategy
 *      will then fallback to "top N by play count".
 *
 * RULES ENFORCED HERE
 * -------------------
 * #42  Diagnostics Is First-Class — composer emits warning on low confidence
 * #46  Runtime Never Creates Identity — composer reads itemId from scorers
 * #47  Reducer Never Predicts — composer IS the prediction logic
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Android, no IO, no time reads (nowMs is passed in).
 *
 * © RIKO Experience OS — Engine Tier
 */

package com.rikodev.music.android.presentation.library.engine.scoring

import com.rikodev.music.android.presentation.library.engine.EngineInput
import com.rikodev.music.android.presentation.library.engine.SongRef
import com.rikodev.music.android.presentation.library.engine.diagnostics.EngineWarning
import androidx.compose.animation.core.Animatable

/**
 * The result of composing scores for a single collection.
 *
 * Bundles the per-item scores + any warnings the composer emitted
 * (e.g. "low confidence — entropy boosted", "zero-weight profile").
 */
data class CompositionResult(
    /** Stable key of the collection these scores belong to. Rule #44. */
    val collectionStableKey: String,
    /** Per-item scores, keyed by itemId. */
    val scores: Map<String, CollectionScore>,
    /** Items that had NO confident signal — candidates for fallback. */
    val lowConfidenceItemIds: Set<String>,
    /** Warnings emitted during composition. */
    val warnings: List<EngineWarning>,
) {
    val hasScores: Boolean get() = scores.isNotEmpty()
    val scoreCount: Int get() = scores.size
}

/**
 * Stateless composer. Calling [compose] is the only entry point.
 */
object ScoreComposer {

    /**
     * Compose final scores for a set of songs against a single collection.
     *
     * @param songs          The candidate songs.
     * @param input          The full engine input.
     * @param profile        The score profile for this collection.
     * @param nowMs          Wall-clock "now".
     * @return               A [CompositionResult] with per-item scores.
     */
    fun compose(
        songs: List<SongRef>,
        input: EngineInput,
        profile: ScoreProfile,
        nowMs: Long,
    ): CompositionResult {

        val warnings = mutableListOf<EngineWarning>()

        // ── Case 3: zero-weight profile ─────────────────────────────────
        if (profile.totalWeight <= 0f) {
            warnings.add(
                EngineWarning.Generic(
                    codeOverride = "ZERO_WEIGHT_PROFILE",
                    messageOverride = "Profile for '${profile.collectionStableKey}' has total weight 0; " +
                        "all items will receive zero scores.",
                    stableKeyOverride = profile.collectionStableKey,
                )
            )
            val zeroScores = songs.associate { it.songId to CollectionScore.zero(it.songId) }
            return CompositionResult(
                collectionStableKey = profile.collectionStableKey,
                scores = zeroScores,
                lowConfidenceItemIds = songs.map { it.songId }.toSet(),
                warnings = warnings,
            )
        }

        // ── Determine active scorers from the profile ──────────────────
        val activeScorers = ScorerRegistry.forProfile(profile)
        if (activeScorers.isEmpty()) {
            warnings.add(
                EngineWarning.Generic(
                    codeOverride = "NO_ACTIVE_SCORERS",
                    messageOverride = "No scorers matched profile for '${profile.collectionStableKey}'.",
                    stableKeyOverride = profile.collectionStableKey,
                )
            )
            val zeroScores = songs.associate { it.songId to CollectionScore.zero(it.songId) }
            return CompositionResult(
                collectionStableKey = profile.collectionStableKey,
                scores = zeroScores,
                lowConfidenceItemIds = songs.map { it.songId }.toSet(),
                warnings = warnings,
            )
        }

        // ── Score each song ────────────────────────────────────────────
        val lowConfidenceIds = mutableSetOf<String>()
        val scores = LinkedHashMap<String, CollectionScore>(songs.size)

        for (song in songs) {
            val contributions = activeScorers.map { scorer ->
                val raw = scorer.score(song, input, nowMs)
                // Apply weight from profile
                val weight = profile.weights()[scorer.signal] ?: 0f
                // Weighted score = raw × weight × confidence
                val weighted = raw.rawScore * weight * raw.confidence
                raw.copy(weight = weight, weightedScore = weighted)
            }

            // ── Case 1: low confidence fallback ─────────────────────────
            val avgConfidence = if (contributions.isEmpty()) 0f
                else contributions.map { it.confidence }.average().toFloat()

            val finalContributions = if (avgConfidence < profile.minConfidenceThreshold) {
                lowConfidenceIds.add(song.songId)
                // Boost entropy weight to surface variety
                val entropyBoost = contributions.map { c ->
                    if (c.signal == ScoringSignal.ENTROPY) {
                        val newWeight = (c.weight + 0.3f).coerceAtMost(1f)
                        c.copy(
                            weight = newWeight,
                            weightedScore = c.rawScore * newWeight * c.confidence,
                        )
                    } else c
                }
                entropyBoost
            } else {
                contributions
            }

            val score = CollectionScore.compose(song.songId, finalContributions)
            scores[song.songId] = score
        }

        // ── Warn if many items had low confidence ──────────────────────
        if (lowConfidenceIds.size > songs.size / 2 && songs.isNotEmpty()) {
            warnings.add(
                EngineWarning.Generic(
                    codeOverride = "MAJORITY_LOW_CONFIDENCE",
                    messageOverride = "${lowConfidenceIds.size}/${songs.size} items in " +
                        "'${profile.collectionStableKey}' had low confidence; entropy boosted.",
                    stableKeyOverride = profile.collectionStableKey,
                )
            )
        }

        return CompositionResult(
            collectionStableKey = profile.collectionStableKey,
            scores = scores,
            lowConfidenceItemIds = lowConfidenceIds,
            warnings = warnings,
        )
    }

    /**
     * Quick helper: compose a single song's score.
     * Used by strategies that score one item at a time.
     */
    fun composeOne(
        song: SongRef,
        input: EngineInput,
        profile: ScoreProfile,
        nowMs: Long,
    ): CollectionScore {
        val result = compose(listOf(song), input, profile, nowMs)
        return result.scores[song.songId] ?: CollectionScore.zero(song.songId)
    }
}
