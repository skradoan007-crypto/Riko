/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B3 — Collection Engine
 *
 * File 1 / 14 — EngineDiagnostics.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Engine-level diagnostics. Distinct from the per-collection
 * [com.rikodev.music.android.presentation.library.contract.CollectionDiagnosticsContract]
 * which lives in the Constitution and describes a SINGLE collection's
 * cache/health/warning state.
 *
 * This file describes the ENGINE as a whole:
 *
 *   • How long did each stage take?
 *   • Did the engine skip any collection? Why?
 *   • Were there fallbacks (e.g. strategy produced 0 cards → fallback)?
 *   • What was the input size vs. output size?
 *   • Is the engine healthy, degraded, or failing?
 *
 * WHY THIS IS A SEPARATE FILE
 * ---------------------------
 * Rule #42 (Diagnostics Is First-Class) lives at the contract layer —
 * it answers "what's the state of THIS collection?".
 *
 * Engine diagnostics answers a different question:
 *   "What did the engine DO on the last generation cycle?"
 *
 * The contract is per-collection and stable forever (Rule #39).
 * Engine diagnostics is per-cycle and disposable — the engine only
 * keeps the last N reports. They serve different audiences:
 *   - Contract diagnostics → Inspector UI for users/devs
 *   - Engine diagnostics  → Engine telemetry, perf dashboards,
 *                            post-mortem analysis
 *
 * RULES ENFORCED HERE
 * -------------------
 * #42  Diagnostics Is First-Class  — engine reports its own behaviour
 * #46  Runtime Never Creates Identity — diagnostics never invents
 *                                        collection identity; it only
 *                                        references stableKeys produced
 *                                        by the Constitution
 * #47  Reducer Never Predicts      — engine predicts; diagnostics
 *                                        records the prediction
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Android, no Coroutine, no Compose, no IO, no time reads.
 * All timestamps are PASSED IN by the caller. This file is a pure
 * data shape + factory helpers.
 *
 * © RIKO Experience OS — Engine Tier
 */

package com.rikodev.music.android.presentation.library.engine.diagnostics

/**
 * Overall health of the engine, computed at the end of a generation
 * cycle.
 *
 * This is engine-level — distinct from
 * [com.rikodev.music.android.presentation.library.contract.DiagnosticHealth]
 * which is per-collection.
 */
enum class EngineHealth(
    val internalId: String,
    val severity: EngineHealthSeverity,
    val summary: String,
) {
    /** Engine ran cleanly, no warnings, all strategies produced output. */
    OPTIMAL(
        internalId = "optimal",
        severity = EngineHealthSeverity.OK,
        summary = "All strategies produced output within budget.",
    ),

    /** Engine ran, but at least one non-critical warning was emitted. */
    HEALTHY(
        internalId = "healthy",
        severity = EngineHealthSeverity.OK,
        summary = "Engine completed with non-critical warnings.",
    ),

    /** One or more strategies produced empty output; fallbacks applied. */
    DEGRADED(
        internalId = "degraded",
        severity = EngineHealthSeverity.WARN,
        summary = "Some strategies fell back to defaults.",
    ),

    /** Engine completed but exceeded its time budget. */
    SLOW(
        internalId = "slow",
        severity = EngineHealthSeverity.WARN,
        summary = "Engine exceeded its time budget.",
    ),

    /** A strategy threw — engine continued but that collection is missing. */
    PARTIAL_FAILURE(
        internalId = "partial_failure",
        severity = EngineHealthSeverity.ERROR,
        summary = "One or more strategies failed; partial result returned.",
    ),

    /** Engine could not produce ANY output. Caller must show error UI. */
    FATAL(
        internalId = "fatal",
        severity = EngineHealthSeverity.ERROR,
        summary = "Engine could not produce output.",
    ),

    /** Engine hasn't run yet — cold start. */
    UNSTARTED(
        internalId = "unstarted",
        severity = EngineHealthSeverity.SILENT,
        summary = "Engine has not yet been invoked.",
    );

    val isOk: Boolean get() = severity == EngineHealthSeverity.OK
    val isWarn: Boolean get() = severity == EngineHealthSeverity.WARN
    val isError: Boolean get() = severity == EngineHealthSeverity.ERROR
}

/**
 * Severity bucket for an [EngineHealth] or [EngineWarning].
 */
enum class EngineHealthSeverity(
    val internalId: String,
    val rank: Int,
) {
    /** Silent — engine hasn't run or has nothing to say. */
    SILENT(internalId = "silent", rank = 0),

    /** OK — everything is fine. */
    OK(internalId = "ok", rank = 1),

    /** Warn — non-blocking issue, but the engine recovered. */
    WARN(internalId = "warn", rank = 2),

    /** Error — blocking issue for at least one collection. */
    ERROR(internalId = "error", rank = 3),

    /** Critical — engine-wide failure. */
    CRITICAL(internalId = "critical", rank = 4);

    companion object {
        /** Returns the more severe of two buckets. */
        fun worst(a: EngineHealthSeverity, b: EngineHealthSeverity): EngineHealthSeverity =
            if (a.rank >= b.rank) a else b
    }
}

/**
 * A single engine-level warning emitted during a generation cycle.
 *
 * Warnings are non-fatal — the engine still produced output, but
 * something noteworthy happened. The Inspector UI surfaces these
 * to developers; users never see them.
 *
 * RULE #46 — [collectionStableKey] is referenced, never invented.
 * If the warning is engine-wide (not tied to a specific collection),
 * [collectionStableKey] is null.
 */
sealed class EngineWarning(
    val code: String,
    val message: String,
    val collectionStableKey: String?,
) {

    /** A strategy returned zero cards. */
    data class StrategyProducedZero(
        val stableKey: String,
        val strategyName: String,
        val reason: String,
    ) : EngineWarning(
        code = "STRATEGY_ZERO_OUTPUT",
        message = "Strategy '$strategyName' produced 0 cards for '$stableKey': $reason",
        collectionStableKey = stableKey,
    )

    /** A strategy threw an exception; engine caught it and fell back. */
    data class StrategyThrew(
        val stableKey: String,
        val strategyName: String,
        val errorType: String,
        val errorMessage: String,
    ) : EngineWarning(
        code = "STRATEGY_THREW",
        message = "Strategy '$strategyName' threw $errorType for '$stableKey': $errorMessage",
        collectionStableKey = stableKey,
    )

    /** A scorer received negative or NaN weights — clamped to 0. */
    data class ScorerWeightsClamped(
        val stableKey: String,
        val scorerName: String,
        val originalWeight: Float,
    ) : EngineWarning(
        code = "SCORER_WEIGHTS_CLAMPED",
        message = "Scorer '$scorerName' had invalid weight $originalWeight; clamped to 0.",
        collectionStableKey = stableKey,
    )

    /** Ranking diversity step removed too many items — cap raised. */
    data class DiversityOverPruned(
        val stableKey: String,
        val before: Int,
        val after: Int,
        val target: Int,
    ) : EngineWarning(
        code = "DIVERSITY_OVER_PRUNED",
        message = "Diversity reduced '$stableKey' from $before → $after (target $target).",
        collectionStableKey = stableKey,
    )

    /** Cache returned a stale entry past its hard TTL. */
    data class CacheServedStale(
        val stableKey: String,
        val ageMs: Long,
        val hardTtlMs: Long,
    ) : EngineWarning(
        code = "CACHE_SERVED_STALE",
        message = "Cache served '$stableKey' at ${ageMs}ms (hard TTL ${hardTtlMs}ms).",
        collectionStableKey = stableKey,
    )

    /** Engine exceeded its time budget for this cycle. */
    data class TimeBudgetExceeded(
        val budgetMs: Long,
        val actualMs: Long,
    ) : EngineWarning(
        code = "TIME_BUDGET_EXCEEDED",
        message = "Engine cycle took ${actualMs}ms (budget ${budgetMs}ms).",
        collectionStableKey = null,
    )

    /** Input data was empty or too small to generate meaningful output. */
    data class InputTooSmall(
        val inputSize: Int,
        val minimumRequired: Int,
    ) : EngineWarning(
        code = "INPUT_TOO_SMALL",
        message = "Input size $inputSize < minimum $minimumRequired; engine ran with reduced output.",
        collectionStableKey = null,
    )

    /** A required capability flag was missing on the contract. */
    data class MissingCapability(
        val stableKey: String,
        val capabilityFlag: String,
    ) : EngineWarning(
        code = "MISSING_CAPABILITY",
        message = "Contract '$stableKey' is missing capability '$capabilityFlag'.",
        collectionStableKey = stableKey,
    )

    /** Engine-wide warning not covered by the above. */
    data class Generic(
        val codeOverride: String,
        val messageOverride: String,
        val stableKeyOverride: String?,
    ) : EngineWarning(
        code = codeOverride,
        message = messageOverride,
        collectionStableKey = stableKeyOverride,
    )
}

/**
 * Per-stage timing for a single generation cycle.
 *
 * Each stage is measured in milliseconds. A stage that did not run
 * has timing 0. The sum of all stages equals [GenerationReport.totalDurationMs].
 */
data class StageTiming(
    /** Strategy selection + dispatch. */
    val strategyDispatchMs: Long,
    /** Per-item scoring across all strategies. */
    val scoringMs: Long,
    /** Ranking pipeline (normalize → blend → diversity → dedupe). */
    val rankingMs: Long,
    /** Cache read + write. */
    val cacheMs: Long,
    /** CardSnapshot construction + payload packing. */
    val packagingMs: Long,
    /** Diagnostics assembly. */
    val diagnosticsMs: Long,
) {
    val totalMs: Long
        get() = strategyDispatchMs + scoringMs + rankingMs +
            cacheMs + packagingMs + diagnosticsMs

    /** True if any single stage exceeded its soft budget. */
    val hasSlowStage: Boolean
        get() = strategyDispatchMs > SOFT_BUDGET_STRATEGY_MS ||
            scoringMs > SOFT_BUDGET_SCORING_MS ||
            rankingMs > SOFT_BUDGET_RANKING_MS ||
            cacheMs > SOFT_BUDGET_CACHE_MS

    companion object {
        const val SOFT_BUDGET_STRATEGY_MS = 50L
        const val SOFT_BUDGET_SCORING_MS = 80L
        const val SOFT_BUDGET_RANKING_MS = 40L
        const val SOFT_BUDGET_CACHE_MS = 20L

        val EMPTY = StageTiming(
            strategyDispatchMs = 0,
            scoringMs = 0,
            rankingMs = 0,
            cacheMs = 0,
            packagingMs = 0,
            diagnosticsMs = 0,
        )
    }
}

/**
 * The result of a single engine generation cycle.
 *
 * One cycle = "engine.generate(request) was called once". A cycle
 * may produce multiple collections (one per active contract).
 */
data class GenerationReport(
    /** Wall-clock start time of the cycle (epoch ms, caller-supplied). */
    val startedAt: Long,
    /** Wall-clock end time of the cycle (epoch ms, caller-supplied). */
    val endedAt: Long,
    /** Per-stage timing breakdown. */
    val stages: StageTiming,
    /** Total duration = endedAt - startedAt. Should equal stages.totalMs. */
    val totalDurationMs: Long,
    /** Number of input items the engine considered. */
    val inputSize: Int,
    /** Number of collections the engine attempted to produce. */
    val requestedCollections: Int,
    /** Number of collections actually produced (post-failure). */
    val producedCollections: Int,
    /** Number of cards produced across all collections. */
    val producedCards: Int,
    /** Number of strategies that ran. */
    val strategiesRun: Int,
    /** Number of strategies that failed. */
    val strategiesFailed: Int,
    /** Number of cache hits (entries served without regeneration). */
    val cacheHits: Int,
    /** Number of cache misses (entries regenerated). */
    val cacheMisses: Int,
    /** Warnings emitted during this cycle. */
    val warnings: List<EngineWarning>,
    /** Final health verdict for this cycle. */
    val health: EngineHealth,
    /** Optional human-readable note (e.g. "fallback to top-played"). */
    val note: String?,
) {
    val hasWarnings: Boolean get() = warnings.isNotEmpty()
    val warningCount: Int get() = warnings.size
    val cacheHitRatio: Float
        get() {
            val total = cacheHits + cacheMisses
            return if (total == 0) 0f else cacheHits.toFloat() / total
        }
}

/**
 * The complete engine diagnostics state.
 *
 * Held by [com.rikodev.music.android.presentation.library.engine.CollectionEngine]
 * and exposed to the Inspector UI (B4) for developers.
 *
 * RULE #47 — Engine predicts; diagnostics records the prediction.
 * The engine doesn't try to predict its own future health from
 * historical data here — that's a separate concern. This file
 * just records WHAT HAPPENED.
 */
data class EngineDiagnostics(
    /** Most recent generation report (or null if engine hasn't run). */
    val lastReport: GenerationReport?,
    /** Rolling window of the last N reports (oldest first). */
    val recentReports: List<GenerationReport>,
    /** Number of total generation cycles since engine start. */
    val totalCycles: Int,
    /** Number of cycles that ended in PARTIAL_FAILURE or FATAL. */
    val failedCycles: Int,
    /** Wall-clock time (epoch ms) of the first ever cycle. */
    val firstCycleAt: Long,
    /** Wall-clock time (epoch ms) of the most recent cycle. */
    val lastCycleAt: Long,
    /** Engine-wide rolling health — worst of last 3 reports. */
    val rollingHealth: EngineHealth,
    /** All unresolved warnings across recent cycles. */
    val outstandingWarnings: List<EngineWarning>,
) {

    val hasRun: Boolean get() = lastReport != null
    val failureRate: Float
        get() = if (totalCycles == 0) 0f else failedCycles.toFloat() / totalCycles

    companion object {

        /** Initial state before the engine has ever run. */
        val UNSTARTED = EngineDiagnostics(
            lastReport = null,
            recentReports = emptyList(),
            totalCycles = 0,
            failedCycles = 0,
            firstCycleAt = 0L,
            lastCycleAt = 0L,
            rollingHealth = EngineHealth.UNSTARTED,
            outstandingWarnings = emptyList(),
        )

        /** Maximum number of recent reports kept in the rolling window. */
        const val ROLLING_WINDOW_SIZE = 10
    }

    /**
     * Append a new report to this diagnostics state, returning a new
     * immutable copy. Used by the engine after each generate() call.
     */
    fun appendReport(report: GenerationReport): EngineDiagnostics {
        val newRecent = (recentReports + report).takeLast(ROLLING_WINDOW_SIZE)
        val newTotal = totalCycles + 1
        val newFailed = failedCycles +
            if (report.health == EngineHealth.PARTIAL_FAILURE ||
                report.health == EngineHealth.FATAL
            ) 1 else 0
        val newRollingHealth = computeRollingHealth(newRecent)
        val newOutstanding = newRecent
            .flatMap { it.warnings }
            .distinctBy { it.code + (it.collectionStableKey ?: "") }

        return EngineDiagnostics(
            lastReport = report,
            recentReports = newRecent,
            totalCycles = newTotal,
            failedCycles = newFailed,
            firstCycleAt = if (firstCycleAt == 0L) report.startedAt else firstCycleAt,
            lastCycleAt = report.endedAt,
            rollingHealth = newRollingHealth,
            outstandingWarnings = newOutstanding,
        )
    }

    /**
     * Compute the rolling health from the last few reports.
     * The worst health in the last 3 reports wins.
     */
    private fun computeRollingHealth(
        reports: List<GenerationReport>,
    ): EngineHealth {
        if (reports.isEmpty()) return EngineHealth.UNSTARTED
        val lastThree = reports.takeLast(3)
        val worstSeverity = lastThree
            .map { it.health.severity }
            .reduce { acc, sev -> EngineHealthSeverity.worst(acc, sev) }
        return lastThree
            .map { it.health }
            .first { it.severity == worstSeverity }
    }
}
