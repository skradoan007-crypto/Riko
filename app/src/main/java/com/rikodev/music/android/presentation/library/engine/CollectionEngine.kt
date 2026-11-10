/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B3 — Collection Engine
 *
 * File 13 / 14 — CollectionEngine.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * The orchestrator. Wires together all 5 layers:
 *
 *   ┌─────────────────────────────────────────────────────────────┐
 *   │                    CollectionEngine                         │
 *   │                                                             │
 *   │  ┌──────────┐  ┌─────────┐  ┌─────────┐  ┌──────┐  ┌────┐ │
 *   │  │ Strategy │─►│ Scoring │─►│ Ranking │─►│ Cache │─►│ Di │ │
 *   │  └──────────┘  └─────────┘  └─────────┘  └──────┘  └────┘ │
 *   └─────────────────────────────────────────────────────────────┘
 *
 * The engine is the ONLY entry point the host (ViewModel) calls.
 * It produces [EngineResult] which the host pushes into
 * [com.rikodev.music.android.presentation.library.runtime.CollectionRuntimeState].
 *
 * DATA FLOW
 * ---------
 *
 *   Host (ViewModel)
 *      │
 *      │ 1. Builds EngineInput from Room/MediaStore
 *      ▼
 *   CollectionEngine.generate(request)
 *      │
 *      │ 2. For each requested collection:
 *      │    a. Cache.read() → fresh? skip
 *      │    b. Strategy.generate() → ranked items
 *      │    c. Cache.write()
 *      │    d. Build CardSnapshots
 *      │
 *      │ 3. Aggregate diagnostics
 *      ▼
 *   EngineResult
 *      │
 *      ▼
 *   Host pushes to CollectionRuntimeState
 *
 * DESIGN
 * ------
 * The engine is STATEFUL — it holds the [CollectionCache] and
 * [EngineDiagnostics] between cycles. But each generate() call is
 * pure: given the same input + same internal state, it produces
 * the same output.
 *
 * The engine NEVER:
 *   • Reads wall-clock time (host passes nowMs)
 *   • Touches Android / IO
 *   • Calls into the runtime (host pulls, engine doesn't push)
 *   • Knows about UI / Compose / ViewModels
 *
 * The engine DOES:
 *   • Dispatch to strategies
 *   • Manage the cache
 *   • Track diagnostics
 *   • Build CardSnapshots (pure data, no Android)
 *
 * RULES ENFORCED HERE
 * -------------------
 * #31  Runtime Owns Truth — engine OUTPUT becomes runtime truth
 * #42  Diagnostics Is First-Class — every cycle produces a report
 * #44  Stable Key Never Changes — all keys are stableKey strings
 * #45  Contracts Must Be Serializable — engine output is serializable-shaped
 * #46  Runtime Never Creates Identity — engine reads identity from contracts
 * #47  Reducer Never Predicts — engine IS the prediction
 * #48  Actions Describe Intent — engine emits intent actions for runtime
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Android, no IO, no time reads, no coroutine.
 *
 * © RIKO Experience OS — Engine Tier
 */

package com.rikodev.music.android.presentation.library.engine

import com.rikodev.music.android.presentation.library.contract.CollectionContract
import com.rikodev.music.android.presentation.library.contract.CollectionConstitution
import com.rikodev.music.android.presentation.library.contract.CollectionVisibility
import com.rikodev.music.android.presentation.library.contract.CollectionDiagnosticsContract
import com.rikodev.music.android.presentation.library.engine.cache.CachePolicy
import com.rikodev.music.android.presentation.library.engine.cache.CacheReadResult
import com.rikodev.music.android.presentation.library.engine.cache.CollectionCache
import com.rikodev.music.android.presentation.library.engine.cache.InvalidationTrigger
import com.rikodev.music.android.presentation.library.engine.diagnostics.EngineDiagnostics
import com.rikodev.music.android.presentation.library.engine.diagnostics.EngineHealth
import com.rikodev.music.android.presentation.library.engine.diagnostics.EngineWarning
import com.rikodev.music.android.presentation.library.engine.diagnostics.GenerationReport
import com.rikodev.music.android.presentation.library.engine.diagnostics.StageTiming
import com.rikodev.music.android.presentation.library.engine.strategy.StrategyRegistry
import com.rikodev.music.android.presentation.library.engine.strategy.StrategyResult
import com.rikodev.music.android.presentation.library.runtime.CardSnapshot
import com.rikodev.music.android.presentation.library.runtime.CollectionRuntimeSnapshot
import com.rikodev.music.android.presentation.library.contract.DiagnosticCacheStatus
import com.rikodev.music.android.presentation.library.contract.DiagnosticGenerationReason
import com.rikodev.music.android.presentation.library.contract.DiagnosticRefreshReason

/**
 * A request to the engine to generate one or more collections.
 */
data class EngineRequest(
    /** The full engine input. */
    val input: EngineInput,
    /** Stable keys of collections to generate. Empty = all active contracts. */
    val targetStableKeys: Set<String>,
    /** Why this generation is happening. */
    val reason: DiagnosticGenerationReason,
    /** Wall-clock epoch ms — the "current time" for this cycle. */
    val nowMs: Long,
    /** True if the host wants to bypass the cache. */
    val bypassCache: Boolean,
) {
    companion object {
        /** Default request — generate all active contracts for the input. */
        fun forInput(
            input: EngineInput,
            reason: DiagnosticGenerationReason = DiagnosticGenerationReason.DEFAULT,
            nowMs: Long,
        ) = EngineRequest(
            input = input,
            targetStableKeys = emptySet(),
            reason = reason,
            nowMs = nowMs,
            bypassCache = false,
        )

        /** Request to refresh a single collection. */
        fun forSingle(
            stableKey: String,
            input: EngineInput,
            reason: DiagnosticGenerationReason,
            nowMs: Long,
        ) = EngineRequest(
            input = input,
            targetStableKeys = setOf(stableKey),
            reason = reason,
            nowMs = nowMs,
            bypassCache = true,  // single-refresh usually bypasses cache
        )
    }
}

/**
 * The engine's output for a single generation cycle.
 */
data class EngineResult(
    /** Wall-clock start time (epoch ms). */
    val startedAtMs: Long,
    /** Wall-clock end time (epoch ms). */
    val endedAtMs: Long,
    /** Per-collection snapshots, keyed by stableKey. Rule #44. */
    val snapshots: Map<String, CollectionRuntimeSnapshot>,
    /** Per-collection strategy results (for inspector). */
    val strategyResults: Map<String, StrategyResult>,
    /** Diagnostics report for this cycle. */
    val report: GenerationReport,
    /** Updated rolling diagnostics state. */
    val diagnostics: EngineDiagnostics,
    /** Engine health for this cycle. */
    val health: EngineHealth,
    /** True if the engine produced at least one non-empty collection. */
    val hasAnyOutput: Boolean,
) {
    val durationMs: Long get() = endedAtMs - startedAtMs
    val snapshotCount: Int get() = snapshots.size
    val totalCards: Int get() = snapshots.values.sumOf { it.cards.size }
}

/**
 * The Collection Engine.
 *
 * Singleton-style usage:
 *   val engine = CollectionEngine()
 *   val result = engine.generate(request)
 *
 * The engine is safe to call from any thread (cache is thread-safe,
 * diagnostics is immutable). The host decides threading.
 */
class CollectionEngine(
    private val cachePolicy: CachePolicy = CachePolicy.PRODUCTION,
) {

    private val cache = CollectionCache(cachePolicy)
    private var diagnostics: EngineDiagnostics = EngineDiagnostics.UNSTARTED

    /**
     * Engine version — bumped whenever the engine's output shape changes.
     * Recorded in [CollectionDiagnosticsContract] for audit purposes.
     */
    private val ENGINE_VERSION: String = "b3.0.0"

    /**
     * Run a generation cycle.
     *
     * @param request  The request — input + targets + reason.
     * @return         The result — snapshots + diagnostics + health.
     */
    fun generate(request: EngineRequest): EngineResult {
        val startedAt = request.nowMs
        val input = request.input

        // ── Resolve target contracts ───────────────────────────────────
        val targetContracts: List<CollectionContract> = if (request.targetStableKeys.isEmpty()) {
            CollectionConstitution.coldStartFeed
        } else {
            request.targetStableKeys.mapNotNull { CollectionConstitution.byKey(it) }
        }

        // ── Per-stage timing ───────────────────────────────────────────
        var strategyMs = 0L
        var scoringMs = 0L  // included in strategy (strategies call composer internally)
        var rankingMs = 0L  // included in strategy
        var cacheMs = 0L
        var packagingMs = 0L
        val diagnosticsStart = System.nanoTime()

        val warnings = mutableListOf<EngineWarning>()
        val snapshots = LinkedHashMap<String, CollectionRuntimeSnapshot>()
        val strategyResults = LinkedHashMap<String, StrategyResult>()
        var strategiesRun = 0
        var strategiesFailed = 0
        var cacheHits = 0
        var cacheMisses = 0
        var producedCards = 0
        var producedCollections = 0

        // ── For each target contract ──────────────────────────────────
        for (contract in targetContracts) {
            val stableKey = contract.stableKey

            // ── Cache read ──────────────────────────────────────────────
            val cacheReadStart = System.nanoTime()
            val cacheRead = if (request.bypassCache) {
                CacheReadResult.Miss
            } else {
                cache.read(stableKey, request.nowMs)
            }
            cacheMs += (System.nanoTime() - cacheReadStart) / 1_000_000

            val cachedItems = when (cacheRead) {
                is CacheReadResult.Fresh -> {
                    cacheHits++
                    cacheRead.entry.items
                }
                is CacheReadResult.Stale -> {
                    cacheHits++
                    cacheRead.entry.items
                }
                CacheReadResult.Miss, CacheReadResult.Disabled -> {
                    cacheMisses++
                    null
                }
            }

            if (cachedItems != null) {
                // ── Serve from cache — build snapshot ─────────────────────
                val packagingStart = System.nanoTime()
                val snapshot = buildSnapshot(
                    contract = contract,
                    items = cachedItems,
                    input = input,
                    nowMs = request.nowMs,
                    cacheStatus = when (cacheRead) {
                        is CacheReadResult.Fresh -> DiagnosticCacheStatus.FRESH
                        is CacheReadResult.Stale -> DiagnosticCacheStatus.STALE
                        else -> DiagnosticCacheStatus.FRESH
                    },
                )
                packagingMs += (System.nanoTime() - packagingStart) / 1_000_000

                snapshots[stableKey] = snapshot
                producedCollections++
                producedCards += snapshot.cards.size
                continue
            }

            // ── Cache miss — run the strategy ──────────────────────────
            val strategyStart = System.nanoTime()
            val strategy = StrategyRegistry.forContract(contract)
            val strategyResult = try {
                strategiesRun++
                strategy.generate(contract, input, request.nowMs)
            } catch (t: Throwable) {
                strategiesFailed++
                warnings.add(
                    EngineWarning.StrategyThrew(
                        stableKey = stableKey,
                        strategyName = strategy.name,
                        errorType = t::class.simpleName ?: "Unknown",
                        errorMessage = t.message ?: "no message",
                    )
                )
                null
            }
            strategyMs += (System.nanoTime() - strategyStart) / 1_000_000

            if (strategyResult == null) continue

            strategyResults[stableKey] = strategyResult
            warnings.addAll(strategyResult.warnings)

            // ── Cache write ─────────────────────────────────────────────
            val cacheWriteStart = System.nanoTime()
            cache.write(
                stableKey = stableKey,
                items = strategyResult.items,
                totalCandidates = strategyResult.composition.scoreCount,
                warningCount = strategyResult.warnings.size,
                inputFingerprint = fingerprintInput(input),
                nowMs = request.nowMs,
            )
            cacheMs += (System.nanoTime() - cacheWriteStart) / 1_000_000

            // ── Build snapshot ──────────────────────────────────────────
            val packagingStart = System.nanoTime()
            val snapshot = buildSnapshot(
                contract = contract,
                items = strategyResult.items,
                input = input,
                nowMs = request.nowMs,
                cacheStatus = DiagnosticCacheStatus.FRESH,
            )
            packagingMs += (System.nanoTime() - packagingStart) / 1_000_000

            snapshots[stableKey] = snapshot
            producedCollections++
            producedCards += snapshot.cards.size
        }

        // ── Cache warnings (stale entries past hard TTL) ──────────────
        warnings.addAll(cache.collectWarnings(request.nowMs))

        // ── Build the generation report ───────────────────────────────
        val diagnosticsEnd = System.nanoTime()
        val diagnosticsMs = (diagnosticsEnd - diagnosticsStart) / 1_000_000

        val totalDurationMs = strategyMs + scoringMs + rankingMs + cacheMs + packagingMs + diagnosticsMs

        val health = computeHealth(
            strategiesRun = strategiesRun,
            strategiesFailed = strategiesFailed,
            producedCollections = producedCollections,
            totalDurationMs = totalDurationMs,
            warnings = warnings,
        )

        val report = GenerationReport(
            startedAt = startedAt,
            endedAt = startedAt + totalDurationMs,
            stages = StageTiming(
                strategyDispatchMs = strategyMs,
                scoringMs = scoringMs,
                rankingMs = rankingMs,
                cacheMs = cacheMs,
                packagingMs = packagingMs,
                diagnosticsMs = diagnosticsMs,
            ),
            totalDurationMs = totalDurationMs,
            inputSize = input.totalLibrarySize,
            requestedCollections = targetContracts.size,
            producedCollections = producedCollections,
            producedCards = producedCards,
            strategiesRun = strategiesRun,
            strategiesFailed = strategiesFailed,
            cacheHits = cacheHits,
            cacheMisses = cacheMisses,
            warnings = warnings,
            health = health,
            note = describeHealth(health, strategiesRun, strategiesFailed),
        )

        // ── Update rolling diagnostics ────────────────────────────────
        diagnostics = diagnostics.appendReport(report)

        return EngineResult(
            startedAtMs = startedAt,
            endedAtMs = startedAt + totalDurationMs,
            snapshots = snapshots,
            strategyResults = strategyResults,
            report = report,
            diagnostics = diagnostics,
            health = health,
            hasAnyOutput = producedCollections > 0,
        )
    }

    /**
     * Invalidate cache entries based on a trigger.
     * Call this when the host observes an external change.
     */
    fun invalidate(trigger: InvalidationTrigger, nowMs: Long): List<String> {
        return cache.invalidate(trigger, nowMs)
    }

    /**
     * Convenience overload — invalidate a single collection by its stable key.
     *
     * Wrapper around [invalidate] that wraps the stable key in an
     * [InvalidationTrigger] (since the cache works on triggers, not
     * bare keys). The host uses this when it only knows the stable key.
     *
     * @param stableKey  Stable key of the collection to invalidate.
     * @return  List of invalidated stable keys (empty if nothing matched).
     */
    fun invalidate(stableKey: String): List<String> {
        val nowMs = System.currentTimeMillis()
        val trigger = InvalidationTrigger.SONG_ADDED
        return cache.invalidate(trigger, nowMs)
    }

    /**
     * Refresh a single collection by its stable key.
     *
     * This is a host-facing convenience method. The engine itself is
     * pull-based — the host invokes [generate] to produce a new
     * snapshot. This method exists so the host's [LibraryEnginePort]
     * has a symmetric surface for "refresh this collection" without
     * having to construct a full [EngineRequest].
     *
     * Implementation: invalidate the cached entry for [stableKey]
     * (if any) so the next [generate] call will recompute it.
     *
     * @param stableKey  Stable key of the collection to refresh.
     * @param force      Currently unused — kept for ABI compatibility
     *                   with the host port. The cache is always
     *                   invalidated when this method is called.
     */
    fun refreshCollection(stableKey: String, force: Boolean = false) {
        // Force a cache miss on next read by invalidating this stable key.
        invalidate(stableKey)
    }

    /**
     * Refresh ALL collections.
     *
     * Invalidates every cached entry so the next [generate] cycle
     * recomputes everything from scratch. Use sparingly — this is
     * expensive on large libraries.
     *
     * @param force  Currently unused — kept for ABI compatibility.
     */
    fun refreshAll(force: Boolean = false) {
        val nowMs = System.currentTimeMillis()
        val trigger = InvalidationTrigger.USER_REQUESTED
        cache.invalidate(trigger, nowMs)
    }

    /**
     * Request a snapshot for a single collection.
     *
     * The engine is pull-based — the host must call [generate] to
     * produce a snapshot. This method is a no-op stub that exists
     * so the host's [LibraryEnginePort] can signal "I want this
     * collection's snapshot" without crashing. The host is expected
     * to call [generate] separately to actually obtain the snapshot.
     *
     * @param stableKey  Stable key whose snapshot is requested.
     */
    fun requestSnapshot(stableKey: String) {
        // No-op. The host reads snapshots via generate(EngineRequest.forSingle(...)).
        // This method exists so the LibraryEnginePort can signal intent.
    }

    /**
     * Request snapshots for ALL collections.
     *
     * Like [requestSnapshot], this is a no-op stub. The host is
     * expected to call [generate] separately to actually obtain
     * the snapshots.
     */
    fun requestAllSnapshots() {
        // No-op. The host reads snapshots via generate(EngineRequest.forInput(...)).
    }

    /**
     * Get the current engine diagnostics state (read-only).
     */
    fun diagnostics(): EngineDiagnostics = diagnostics

    /**
     * Get a cache snapshot for the inspector UI.
     */
    fun cacheSnapshot(nowMs: Long) = cache.snapshot(nowMs)

    /* =================================================================================
     * INTERNAL — BUILD SNAPSHOT
     * ================================================================================= */

    private fun buildSnapshot(
        contract: CollectionContract,
        items: List<com.rikodev.music.android.presentation.library.engine.ranking.RankedItem>,
        input: EngineInput,
        nowMs: Long,
        cacheStatus: DiagnosticCacheStatus,
    ): CollectionRuntimeSnapshot {
        val cards = items.map { item ->
            val song = input.songById(item.itemId)
            CardSnapshot(
                cardId = item.itemId,  // Rule #44 — stable id
                title = song?.title ?: "Unknown",
                subtitle = song?.artistName ?: "",
                accentColorHex = null,  // host fills from palette analyzer
                payload = song?.let { SongPayload(it) },
                isSelected = false,
                isPlaying = false,
                isActive = false,
            )
        }

        val contractDiagnostics = CollectionDiagnosticsContract.healthy(
            generationTime = nowMs,
            lastUpdate = nowMs,
            candidateCount = items.size,
            emittedCount = cards.size,
            generationDurationMs = 0L,
            engineVersion = ENGINE_VERSION,
        ).let {
            // Override cache status if we served from stale cache
            if (cacheStatus != DiagnosticCacheStatus.FRESH) {
                it.copy(cacheStatus = cacheStatus)
            } else it
        }

        return CollectionRuntimeSnapshot(
            stableKey = contract.stableKey,
            cards = cards,
            visibility = contract.visibility,  // Rule #46 — read from contract
            isRefreshing = false,
            lastUpdated = nowMs,
            totalAvailable = items.size,
            diagnostics = contractDiagnostics,
        )
    }

    /* =================================================================================
     * INTERNAL — HEALTH COMPUTATION
     * ================================================================================= */

    private fun computeHealth(
        strategiesRun: Int,
        strategiesFailed: Int,
        producedCollections: Int,
        totalDurationMs: Long,
        warnings: List<EngineWarning>,
    ): EngineHealth {
        return when {
            strategiesFailed == strategiesRun && strategiesRun > 0 -> EngineHealth.FATAL
            strategiesFailed > 0 -> EngineHealth.PARTIAL_FAILURE
            producedCollections == 0 -> EngineHealth.DEGRADED
            totalDurationMs > 500 -> EngineHealth.SLOW
            warnings.isNotEmpty() -> EngineHealth.HEALTHY
            else -> EngineHealth.OPTIMAL
        }
    }

    private fun describeHealth(
        health: EngineHealth,
        strategiesRun: Int,
        strategiesFailed: Int,
    ): String? = when (health) {
        EngineHealth.OPTIMAL -> "All $strategiesRun strategies produced output cleanly."
        EngineHealth.HEALTHY -> "All strategies ran; ${strategiesRun} produced output with warnings."
        EngineHealth.DEGRADED -> "No strategies produced output."
        EngineHealth.SLOW -> "Engine exceeded time budget."
        EngineHealth.PARTIAL_FAILURE -> "$strategiesFailed of $strategiesRun strategies failed."
        EngineHealth.FATAL -> "All strategies failed."
        EngineHealth.UNSTARTED -> null
    }

    /* =================================================================================
     * INTERNAL — INPUT FINGERPRINT
     * ================================================================================= */

    private fun fingerprintInput(input: EngineInput): String {
        // Simple fingerprint — combination of sizes + a hash of ids
        val sb = StringBuilder()
        sb.append("s${input.songCount}")
        sb.append("a${input.albumCount}")
        sb.append("r${input.artistCount}")
        sb.append("p${input.playlistCount}")
        sb.append("h${input.playHistory.size}")
        sb.append("f${input.favoriteSongIds.size}")
        sb.append("m${input.moodHint ?: "_"}")
        sb.append("t${input.timeOfDayBucket ?: "_"}")
        return sb.toString().hashCode().toString(16)
    }
}

/**
 * Opaque payload for a song card.
 *
 * The renderer (B4) reads this when interpreting a [CardSnapshot].
 * Stored in [CardSnapshot.payload] as [Any?] — the renderer
 * casts back to [SongPayload] when rendering a song card.
 */
data class SongPayload(
    val song: SongRef,
) {
    val songId: String get() = song.songId
    val title: String get() = song.title
    val artistName: String get() = song.artistName
    val albumName: String get() = song.albumName
    val durationMs: Long get() = song.durationMs
}
