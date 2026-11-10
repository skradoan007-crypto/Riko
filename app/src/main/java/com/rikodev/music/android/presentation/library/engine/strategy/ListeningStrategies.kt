/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B3 — Collection Engine
 *
 * File 11 / 14 — ListeningStrategies.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Three strategies that focus on the user's existing listening
 * behaviour:
 *
 *   • ContinueListeningStrategy — "pick up where you left off"
 *   • FavoritesStrategy         — "your favourited songs"
 *   • ForgottenStrategy         — "you used to love these"
 *
 * Each is a stateless object implementing [CollectionStrategy].
 *
 * DESIGN
 * ------
 * Each strategy follows the same 4-step pattern:
 *   1. Filter candidates from input (e.g. only favourited)
 *   2. Build a ScoreProfile (signal weights)
 *   3. Build a RankingPolicy (diversity/dedup/cap)
 *   4. Call ScoreComposer → CollectionRanker → wrap as StrategyResult
 *
 * The pattern is identical so future strategies are easy to add.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #46  Runtime Never Creates Identity — all ids come from input
 * #47  Engine Predicts — these strategies ARE the prediction
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
import com.rikodev.music.android.presentation.library.engine.ranking.RankingPolicy
import com.rikodev.music.android.presentation.library.engine.scoring.ScoreComposer
import com.rikodev.music.android.presentation.library.engine.scoring.ScoreProfile
import com.rikodev.music.android.presentation.library.engine.ranking.CollectionRanker

/* =================================================================================
 * STRATEGY 1 — CONTINUE LISTENING
 * Surfaces songs from the current/last session so the user can resume.
 * Strongest signal: SESSION. Secondary: RECENCY.
 * ================================================================================= */
object ContinueListeningStrategy : CollectionStrategy {

    override val handledType: CollectionType = CollectionType.CONTINUE_LISTENING
    override val name = "ContinueListeningStrategy"

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

        // Empty-candidate fallback: surface top-played songs instead
        val effectiveCandidates = if (candidates.isEmpty()) {
            warnings.add(
                EngineWarning.StrategyProducedZero(
                    stableKey = contract.stableKey,
                    strategyName = name,
                    reason = "no current/recent session; falling back to most-played",
                )
            )
            input.songs.sortedByDescending { it.playCount }.take(20)
        } else candidates

        val composition = ScoreComposer.compose(effectiveCandidates, input, profile, nowMs)
        val ranking = CollectionRanker.rank(
            composition.scores, effectiveCandidates, input, policy, nowMs,
        )

        warnings.addAll(composition.warnings)
        warnings.addAll(ranking.warnings)

        val usedFallback = candidates.isEmpty() && effectiveCandidates.isNotEmpty()
        val note = if (usedFallback) "No session; used top-played fallback."
        else "Resumed from session with ${effectiveCandidates.size} candidates."

        return StrategyResult(
            contract = contract,
            items = ranking.items,
            composition = composition,
            ranking = ranking,
            warnings = warnings,
            usedFallback = usedFallback,
            note = note,
        )
    }

    override fun filterCandidates(
        contract: CollectionContract,
        input: EngineInput,
        nowMs: Long,
    ): List<SongRef> {
        // Find the most recent session (ongoing or last ended)
        val session = input.sessions.lastOrNull()
            ?: return emptyList()

        // Songs in the session that exist in the library
        val sessionSongIds = session.playedSongIds.toSet()
        return input.songs.filter { it.songId in sessionSongIds }
    }

    override fun buildScoreProfile(contract: CollectionContract): ScoreProfile =
        ScoreProfile.continueListening(contract.stableKey)

    override fun buildRankingPolicy(contract: CollectionContract): RankingPolicy =
        RankingPolicy.continueListening(contract.stableKey, contract.constraints.maximumItems)
}

/* =================================================================================
 * STRATEGY 2 — FAVORITES
 * Surfaces the user's favourited songs.
 * Strongest signal: FAVORITE. Secondary: FREQUENCY, RECENCY.
 * ================================================================================= */
object FavoritesStrategy : CollectionStrategy {

    override val handledType: CollectionType = CollectionType.FAVORITES
    override val name = "FavoritesStrategy"

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

        if (candidates.isEmpty()) {
            warnings.add(
                EngineWarning.StrategyProducedZero(
                    stableKey = contract.stableKey,
                    strategyName = name,
                    reason = "user has no favourites yet",
                )
            )
        }

        val composition = ScoreComposer.compose(candidates, input, profile, nowMs)
        val ranking = CollectionRanker.rank(
            composition.scores, candidates, input, policy, nowMs,
        )

        warnings.addAll(composition.warnings)
        warnings.addAll(ranking.warnings)

        return StrategyResult(
            contract = contract,
            items = ranking.items,
            composition = composition,
            ranking = ranking,
            warnings = warnings,
            usedFallback = false,
            note = if (candidates.isEmpty()) "No favourites yet."
            else "${candidates.size} favourites considered.",
        )
    }

    override fun filterCandidates(
        contract: CollectionContract,
        input: EngineInput,
        nowMs: Long,
    ): List<SongRef> = input.songs.filter { it.isFavorite }

    override fun buildScoreProfile(contract: CollectionContract): ScoreProfile =
        ScoreProfile.favoriteHeavy(contract.stableKey)

    override fun buildRankingPolicy(contract: CollectionContract): RankingPolicy =
        RankingPolicy.favorites(contract.stableKey, contract.constraints.maximumItems)
}

/* =================================================================================
 * STRATEGY 3 — FORGOTTEN FAVOURITES
 * Surfaces songs the user used to love but hasn't played in a while.
 * Strongest signal: RECENCY (inverted — older = better, capped at 90 days).
 * Secondary: FAVORITE, FREQUENCY.
 * ================================================================================= */
object ForgottenStrategy : CollectionStrategy {

    override val handledType: CollectionType = CollectionType.FORGOTTEN_CLASSICS
    override val name = "ForgottenStrategy"

    init { StrategyRegistry.register(this) }

    /** Window: songs not played in the last 30 days. */
    private const val FORGOTTEN_WINDOW_MS = 30L * 24 * 60 * 60 * 1000

    /** Don't surface songs older than 1 year since last play. */
    private const val MAX_AGE_MS = 365L * 24 * 60 * 60 * 1000

    override fun generate(
        contract: CollectionContract,
        input: EngineInput,
        nowMs: Long,
    ): StrategyResult {
        val candidates = filterCandidates(contract, input, nowMs)
        val profile = buildScoreProfile(contract)
        val policy = buildRankingPolicy(contract)

        val warnings = mutableListOf<EngineWarning>()

        // Fallback: if no forgotten favourites, surface all favourites
        val effectiveCandidates = if (candidates.isEmpty()) {
            val favorites = input.songs.filter { it.isFavorite }
            if (favorites.isNotEmpty()) {
                warnings.add(
                    EngineWarning.StrategyProducedZero(
                        stableKey = contract.stableKey,
                        strategyName = name,
                        reason = "no forgotten favourites; using all favourites",
                    )
                )
                favorites
            } else candidates
        } else candidates

        val composition = ScoreComposer.compose(effectiveCandidates, input, profile, nowMs)

        // INVERT RECENCY for forgotten: older = higher score
        // We do this by recomputing the recency contribution as (1 - recency)
        val invertedScores = composition.scores.mapValues { (id, score) ->
            val recency = score.contributionFor(
                com.rikodev.music.android.presentation.library.engine.scoring.ScoringSignal.RECENCY
            )
            if (recency != null && recency.confidence > 0f) {
                val invertedRaw = 1f - recency.rawScore
                val newContribution = recency.copy(rawScore = invertedRaw)
                val newBreakdown = score.breakdown.toMutableMap()
                newBreakdown[com.rikodev.music.android.presentation.library.engine.scoring.ScoringSignal.RECENCY] = newContribution
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
            note = if (candidates.isEmpty()) "Fallback to all favourites."
            else "${candidates.size} forgotten favourites surfaced.",
        )
    }

    override fun filterCandidates(
        contract: CollectionContract,
        input: EngineInput,
        nowMs: Long,
    ): List<SongRef> {
        return input.songs.filter { song ->
            // Must have been played at least once (otherwise can't be "forgotten")
            song.playCount > 0 &&
                // Must NOT have been played in the last FORGOTTEN_WINDOW_MS
                song.lastPlayedAtMs > 0 &&
                (nowMs - song.lastPlayedAtMs) >= FORGOTTEN_WINDOW_MS &&
                // Must NOT be older than MAX_AGE_MS (fully forgotten = irrelevant)
                (nowMs - song.lastPlayedAtMs) <= MAX_AGE_MS
        }
    }

    override fun buildScoreProfile(contract: CollectionContract): ScoreProfile =
        ScoreProfile.forgotten(contract.stableKey)

    override fun buildRankingPolicy(contract: CollectionContract): RankingPolicy =
        RankingPolicy.forgotten(contract.stableKey, contract.constraints.maximumItems)
}
