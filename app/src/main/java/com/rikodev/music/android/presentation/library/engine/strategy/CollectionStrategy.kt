/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B3 — Collection Engine
 *
 * File 10 / 14 — CollectionStrategy.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Defines the [CollectionStrategy] interface that all 6 strategies
 * implement, plus [StrategyResult] (what a strategy returns) and
 * [StrategyRegistry] (the lookup table).
 *
 * A strategy is responsible for:
 *
 *   1. Deciding WHICH items from the input are candidates for this
 *      collection (e.g. Favourites strategy only considers favourited
 *      songs; Recently Played only considers songs with lastPlayedAt > 0)
 *
 *   2. Configuring the [ScoreProfile] (signal weights) and
 *      [RankingPolicy] (diversity, dedup, cap) for this collection
 *
 *   3. Calling [ScoreComposer] → [CollectionRanker] to produce the
 *      final ranked list
 *
 *   4. Returning a [StrategyResult] including diagnostics warnings
 *
 * A strategy is NOT responsible for:
 *
 *   • Caching (the engine handles that)
 *   • CardSnapshot construction (the engine handles that)
 *   • Cross-collection coordination (the engine handles that)
 *   • UI / rendering (B4 handles that)
 *
 * RULES ENFORCED HERE
 * -------------------
 * #39  Contracts Are Forever — strategy reads contract from Constitution
 * #46  Runtime Never Creates Identity — strategy never invents ids/types
 * #47  Reducer Never Predicts — strategy IS the prediction
 * #48  Actions Describe Intent — strategy emits intent, not verbs
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Android, no IO, no time reads. Pure logic.
 *
 * © RIKO Experience OS — Engine Tier
 */

package com.rikodev.music.android.presentation.library.engine.strategy

import com.rikodev.music.android.presentation.library.contract.CollectionContract
import com.rikodev.music.android.presentation.library.contract.CollectionConstitution
import com.rikodev.music.android.presentation.library.contract.CollectionType
import com.rikodev.music.android.presentation.library.engine.EngineInput
import com.rikodev.music.android.presentation.library.engine.SongRef
import com.rikodev.music.android.presentation.library.engine.diagnostics.EngineWarning
import com.rikodev.music.android.presentation.library.engine.ranking.RankedItem
import com.rikodev.music.android.presentation.library.engine.ranking.RankingResult
import com.rikodev.music.android.presentation.library.engine.scoring.CompositionResult

/**
 * The result of running a single strategy.
 *
 * Wraps the ranking result + the original contract + any strategy-specific
 * warnings (e.g. "fallback to top-played due to empty candidate set").
 */
data class StrategyResult(
    /** The contract this strategy ran for. Rule #39. */
    val contract: CollectionContract,
    /** The final ranked items, in display order. */
    val items: List<RankedItem>,
    /** The composition result (per-item scores). */
    val composition: CompositionResult,
    /** The ranking result (with dropped-by-stage counts). */
    val ranking: RankingResult,
    /** Strategy-specific warnings (e.g. fallback used). */
    val warnings: List<EngineWarning>,
    /** True if the strategy fell back to a default (e.g. top-played). */
    val usedFallback: Boolean,
    /** Human-readable note for diagnostics. */
    val note: String?,
) {
    val stableKey: String get() = contract.stableKey
    val itemCount: Int get() = items.size
    val hasItems: Boolean get() = items.isNotEmpty()
}

/**
 * The strategy interface.
 *
 * Each strategy knows how to produce ONE collection type. The engine
 * dispatches to the right strategy via [StrategyRegistry].
 *
 * RULE #46 — Strategies MUST read identity (stableKey, type, emotion,
 * priority) from the [CollectionContract] passed in. They MUST NOT
 * invent these. If a strategy finds itself wanting to invent identity,
 * it's a sign the contract is missing a field — fix the contract, not
 * the strategy.
 */
interface CollectionStrategy {

    /** The collection type this strategy handles. */
    val handledType: CollectionType

    /** Stable name for diagnostics. */
    val name: String

    /**
     * Run the strategy for a single contract.
     *
     * @param contract  The contract to generate for.
     * @param input     The full engine input.
     * @param nowMs     Wall-clock "now".
     * @return          The strategy result.
     */
    fun generate(
        contract: CollectionContract,
        input: EngineInput,
        nowMs: Long,
    ): StrategyResult

    /**
     * Filter the input songs to the candidate set for this strategy.
     *
     * Default implementation returns all songs. Override per-strategy
     * to restrict the candidate set (e.g. only favourited, only recently
     * played, only added this week).
     */
    fun filterCandidates(
        contract: CollectionContract,
        input: EngineInput,
        nowMs: Long,
    ): List<SongRef> = input.songs

    /**
     * Build the score profile for this collection.
     *
     * Override per-strategy to customize signal weights.
     */
    fun buildScoreProfile(
        contract: CollectionContract,
    ): com.rikodev.music.android.presentation.library.engine.scoring.ScoreProfile

    /**
     * Build the ranking policy for this collection.
     *
     * Override per-strategy to customize diversity/dedup/cap.
     */
    fun buildRankingPolicy(
        contract: CollectionContract,
    ): com.rikodev.music.android.presentation.library.engine.ranking.RankingPolicy
}

/**
 * Lookup table for strategies.
 *
 * The engine queries this to find the strategy for a given
 * [CollectionType]. Strategies self-register here at object-init
 * time (Kotlin object singletons).
 */
object StrategyRegistry {

    private val strategiesByType = LinkedHashMap<CollectionType, CollectionStrategy>()

    /**
     * Register a strategy. Usually called from the strategy class's
     * init block (idempotent — re-registering replaces).
     */
    fun register(strategy: CollectionStrategy) {
        strategiesByType[strategy.handledType] = strategy
    }

    /** Get the strategy for a collection type, or null. */
    fun forType(type: CollectionType): CollectionStrategy? = strategiesByType[type]

    /** All registered strategies. */
    fun all(): List<CollectionStrategy> = strategiesByType.values.toList()

    /** True if a strategy is registered for the given type. */
    fun hasStrategyFor(type: CollectionType): Boolean = strategiesByType.containsKey(type)

    /**
     * Find the strategy for a contract, falling back to a default
     * strategy if none is registered.
     *
     * The default strategy produces a "most played" fallback so that
     * contracts without a dedicated strategy still produce SOME output.
     */
    fun forContract(contract: CollectionContract): CollectionStrategy {
        return strategiesByType[contract.type] ?: DefaultFallbackStrategy
    }
}

/* =================================================================================
 * DEFAULT FALLBACK STRATEGY
 * Used when no specific strategy is registered for a collection type.
 * Produces "most played" output as a safe default.
 * ================================================================================= */
object DefaultFallbackStrategy : CollectionStrategy {

    override val handledType: CollectionType = CollectionType.MOST_PLAYED
    override val name = "DefaultFallback"

    override fun generate(
        contract: CollectionContract,
        input: EngineInput,
        nowMs: Long,
    ): StrategyResult {
        val candidates = filterCandidates(contract, input, nowMs)
        val profile = buildScoreProfile(contract)
        val policy = buildRankingPolicy(contract)

        val composition = com.rikodev.music.android.presentation.library.engine.scoring.ScoreComposer
            .compose(candidates, input, profile, nowMs)
        val ranking = com.rikodev.music.android.presentation.library.engine.ranking.CollectionRanker
            .rank(composition.scores, candidates, input, policy, nowMs)

        val warnings = mutableListOf<EngineWarning>()
        warnings.addAll(composition.warnings)
        warnings.addAll(ranking.warnings)
        warnings.add(
            EngineWarning.Generic(
                codeOverride = "FALLBACK_STRATEGY_USED",
                messageOverride = "No dedicated strategy for '${contract.stableKey}' " +
                    "(type ${contract.type}); used DefaultFallback.",
                stableKeyOverride = contract.stableKey,
            )
        )

        return StrategyResult(
            contract = contract,
            items = ranking.items,
            composition = composition,
            ranking = ranking,
            warnings = warnings,
            usedFallback = true,
            note = "Default fallback: top by play count.",
        )
    }

    override fun filterCandidates(
        contract: CollectionContract,
        input: EngineInput,
        nowMs: Long,
    ): List<SongRef> {
        // Top 100 by play count, or all if fewer
        return input.songs
            .sortedByDescending { it.playCount }
            .take(100)
    }

    override fun buildScoreProfile(
        contract: CollectionContract,
    ) = com.rikodev.music.android.presentation.library.engine.scoring.ScoreProfile.balanced(contract.stableKey)

    override fun buildRankingPolicy(
        contract: CollectionContract,
    ) = com.rikodev.music.android.presentation.library.engine.ranking.RankingPolicy.balanced(
        contract.stableKey,
        contract.constraints.maximumItems,
    )
}
