/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B3 — Collection Engine
 *
 * File 12 / 14 — DiscoveryStrategies.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Three strategies that focus on surfacing NEW or UNDER-EXPLORED music:
 *
 *   • HiddenGemsStrategy  — low-frequency + high-completion songs
 *   • MoodStrategy         — songs matching the current mood hint
 *   • DiscoveryStrategy    — recently-added + diverse songs
 *
 * These strategies are where the engine's "intelligence" becomes
 * visible to the user. They turn passive data (play counts, mood
 * tags) into active recommendations ("you might like this").
 *
 * RULES ENFORCED HERE
 * -------------------
 * #46  Runtime Never Creates Identity — all ids from input
 * #47  Engine Predicts — these ARE the predictions
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Android, no IO, no time reads.
 *
 * © RIKO Experience OS — Engine Tier
 */

package com.rikodev.music.android.presentation.library.engine.strategy

import com.rikodev.music.android.presentation.library.contract.CollectionContract
import com.rikodev.music.android.presentation.library.contract.CollectionType
import com.rikodev.music.android.presentation.library.engine.EngineInput
import com.rikodev.music.android.presentation.library.engine.SongRef
import com.rikodev.music.android.presentation.library.engine.diagnostics.EngineWarning
import com.rikodev.music.android.presentation.library.engine.ranking.CollectionRanker
import com.rikodev.music.android.presentation.library.engine.ranking.RankingPolicy
import com.rikodev.music.android.presentation.library.engine.scoring.ScoreComposer
import com.rikodev.music.android.presentation.library.engine.scoring.ScoreProfile
import com.rikodev.music.android.presentation.library.engine.scoring.ScoringSignal

/* =================================================================================
 * STRATEGY 4 — HIDDEN GEMS
 * Songs the user has played but rarely — yet when they DO play them,
 * they finish them (high completion). These are "should play more"
 * candidates.
 *
 * Signal logic:
 *   • FREQUENCY inverted: low play count → higher score
 *   • COMPLETION: high completion → higher score
 *   • FRESHNESS: bonus for recently added
 *   • DIVERSITY: enforce interleave
 * ================================================================================= */
object HiddenGemsStrategy : CollectionStrategy {

    override val handledType: CollectionType = CollectionType.HIDDEN_GEMS
    override val name = "HiddenGemsStrategy"

    init { StrategyRegistry.register(this) }

    /** A song is a "hidden gem" candidate if it's been played 1-5 times. */
    private const val MIN_PLAYS = 1
    private const val MAX_PLAYS = 5

    override fun generate(
        contract: CollectionContract,
        input: EngineInput,
        nowMs: Long,
    ): StrategyResult {
        val candidates = filterCandidates(contract, input, nowMs)
        val profile = buildScoreProfile(contract)
        val policy = buildRankingPolicy(contract)

        val warnings = mutableListOf<EngineWarning>()

        if (candidates.isEmpty()) {
            warnings.add(
                EngineWarning.StrategyProducedZero(
                    stableKey = contract.stableKey,
                    strategyName = name,
                    reason = "no songs played 1-5 times with completion > 50%",
                )
            )
        }

        val composition = ScoreComposer.compose(candidates, input, profile, nowMs)

        // INVERT FREQUENCY for hidden gems: low play count → higher score
        val invertedScores = composition.scores.mapValues { (_, score) ->
            val freq = score.contributionFor(ScoringSignal.FREQUENCY)
            if (freq != null && freq.confidence > 0f) {
                val invertedRaw = 1f - freq.rawScore
                val newContribution = freq.copy(
                    rawScore = invertedRaw,
                    note = "low play count (hidden gem candidate)",
                )
                val newBreakdown = score.breakdown.toMutableMap()
                newBreakdown[ScoringSignal.FREQUENCY] = newContribution
                score.copy(breakdown = newBreakdown)
            } else score
        }
        val invertedComposition = composition.copy(scores = invertedScores)

        val ranking = CollectionRanker.rank(
            invertedComposition.scores, candidates, input, policy, nowMs,
        )

        warnings.addAll(invertedComposition.warnings)
        warnings.addAll(ranking.warnings)

        return StrategyResult(
            contract = contract,
            items = ranking.items,
            composition = invertedComposition,
            ranking = ranking,
            warnings = warnings,
            usedFallback = false,
            note = if (candidates.isEmpty()) "No hidden gems found."
            else "${candidates.size} hidden gem candidates considered.",
        )
    }

    override fun filterCandidates(
        contract: CollectionContract,
        input: EngineInput,
        nowMs: Long,
    ): List<SongRef> {
        return input.songs.filter { song ->
            song.playCount in MIN_PLAYS..MAX_PLAYS &&
                song.averageCompletion >= 0.5f
        }
    }

    override fun buildScoreProfile(contract: CollectionContract): ScoreProfile =
        ScoreProfile.hiddenGems(contract.stableKey)

    override fun buildRankingPolicy(contract: CollectionContract): RankingPolicy =
        RankingPolicy.hiddenGems(contract.stableKey, contract.constraints.maximumItems)
}

/* =================================================================================
 * STRATEGY 5 — MOOD
 * Surfaces songs matching the current mood hint or time-of-day bucket.
 * Mood hint comes from the host (e.g. user picked "Focus" or host
 * inferred "Evening" from clock).
 *
 * Signal logic:
 *   • MOOD: dominant — songs matching the hint score high
 *   • FAVORITE: secondary — favourited songs in this mood get boost
 *   • COMPLETION: secondary — songs the user typically finishes
 * ================================================================================= */
object MoodStrategy : CollectionStrategy {

    override val handledType: CollectionType = CollectionType.MOOD_BASED
    override val name = "MoodStrategy"

    init { StrategyRegistry.register(this) }

    override fun generate(
        contract: CollectionContract,
        input: EngineInput,
        nowMs: Long,
    ): StrategyResult {
        val candidates = filterCandidates(contract, input, nowMs)
        val profile = buildScoreProfile(contract)
        val policy = buildRankingPolicy(contract)

        val warnings = mutableListOf<EngineWarning>()

        if (input.moodHint.isNullOrBlank() && input.timeOfDayBucket.isNullOrBlank()) {
            warnings.add(
                EngineWarning.Generic(
                    codeOverride = "MOOD_HINT_MISSING",
                    messageOverride = "No mood hint or time-of-day provided; " +
                        "Mood collection will be empty.",
                    stableKeyOverride = contract.stableKey,
                )
            )
        }

        if (candidates.isEmpty()) {
            warnings.add(
                EngineWarning.StrategyProducedZero(
                    stableKey = contract.stableKey,
                    strategyName = name,
                    reason = "no songs with mood tags matching hint",
                )
            )
        }

        val composition = ScoreComposer.compose(candidates, input, profile, nowMs)
        val ranking = CollectionRanker.rank(
            composition.scores, candidates, input, policy, nowMs,
        )

        warnings.addAll(composition.warnings)
        warnings.addAll(ranking.warnings)

        val hint = input.moodHint ?: input.timeOfDayBucket ?: "unknown"
        return StrategyResult(
            contract = contract,
            items = ranking.items,
            composition = composition,
            ranking = ranking,
            warnings = warnings,
            usedFallback = false,
            note = "Mood hint: '$hint'. ${candidates.size} candidates.",
        )
    }

    override fun filterCandidates(
        contract: CollectionContract,
        input: EngineInput,
        nowMs: Long,
    ): List<SongRef> {
        // All songs — the MoodScorer will down-rank non-matching ones.
        // We don't filter at the candidate stage because mood is a soft signal.
        return input.songs
    }

    override fun buildScoreProfile(contract: CollectionContract): ScoreProfile =
        ScoreProfile.moodDriven(contract.stableKey)

    override fun buildRankingPolicy(contract: CollectionContract): RankingPolicy =
        RankingPolicy.mood(contract.stableKey, contract.constraints.maximumItems)
}

/* =================================================================================
 * STRATEGY 6 — DISCOVERY
 * Surfaces recently-added songs the user hasn't explored yet.
 * Goal: make the user aware of new music in their library.
 *
 * Signal logic:
 *   • FRESHNESS: dominant — songs added recently
 *   • FREQUENCY inverted: songs NOT yet played get boost
 *   • DIVERSITY: enforce interleave + hard cap per artist
 * ================================================================================= */
object DiscoveryStrategy : CollectionStrategy {

    override val handledType: CollectionType = CollectionType.DISCOVERY
    override val name = "DiscoveryStrategy"

    init { StrategyRegistry.register(this) }

    /** "Recently added" window — 14 days. */
    private const val RECENT_WINDOW_MS = 14L * 24 * 60 * 60 * 1000

    override fun generate(
        contract: CollectionContract,
        input: EngineInput,
        nowMs: Long,
    ): StrategyResult {
        val candidates = filterCandidates(contract, input, nowMs)
        val profile = buildScoreProfile(contract)
        val policy = buildRankingPolicy(contract)

        val warnings = mutableListOf<EngineWarning>()

        // Fallback: if no recent additions, surface unplayed songs from the library
        val effectiveCandidates = if (candidates.isEmpty()) {
            val unplayed = input.songs.filter { it.playCount == 0 }.take(50)
            if (unplayed.isNotEmpty()) {
                warnings.add(
                    EngineWarning.StrategyProducedZero(
                        stableKey = contract.stableKey,
                        strategyName = name,
                        reason = "no songs added in last 14 days; using unplayed songs",
                    )
                )
                unplayed
            } else candidates
        } else candidates

        val composition = ScoreComposer.compose(effectiveCandidates, input, profile, nowMs)

        // INVERT FREQUENCY for discovery: songs NOT played → higher score
        val invertedScores = composition.scores.mapValues { (_, score) ->
            val freq = score.contributionFor(ScoringSignal.FREQUENCY)
            if (freq != null && freq.confidence > 0f) {
                val invertedRaw = 1f - freq.rawScore
                val newContribution = freq.copy(
                    rawScore = invertedRaw,
                    note = "unplayed or rarely played (discovery candidate)",
                )
                val newBreakdown = score.breakdown.toMutableMap()
                newBreakdown[ScoringSignal.FREQUENCY] = newContribution
                score.copy(breakdown = newBreakdown)
            } else score
        }
        val invertedComposition = composition.copy(scores = invertedScores)

        val ranking = CollectionRanker.rank(
            invertedComposition.scores, effectiveCandidates, input, policy, nowMs,
        )

        warnings.addAll(invertedComposition.warnings)
        warnings.addAll(ranking.warnings)

        return StrategyResult(
            contract = contract,
            items = ranking.items,
            composition = invertedComposition,
            ranking = ranking,
            warnings = warnings,
            usedFallback = candidates.isEmpty() && effectiveCandidates.isNotEmpty(),
            note = if (candidates.isEmpty()) "Fallback: unplayed songs."
            else "${candidates.size} recently-added candidates.",
        )
    }

    override fun filterCandidates(
        contract: CollectionContract,
        input: EngineInput,
        nowMs: Long,
    ): List<SongRef> {
        val cutoff = nowMs - RECENT_WINDOW_MS
        return input.songs.filter { it.dateAddedMs >= cutoff }
    }

    override fun buildScoreProfile(contract: CollectionContract): ScoreProfile =
        ScoreProfile.discovery(contract.stableKey)

    override fun buildRankingPolicy(contract: CollectionContract): RankingPolicy =
        RankingPolicy.discovery(contract.stableKey, contract.constraints.maximumItems)
}
