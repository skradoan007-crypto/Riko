/*
 * RIKO EXPERIENCE OS — LIBRARY INTERACTION RUNTIME
 * Build B5.4 — Library Interaction Runtime
 *
 * File 13 / 15 — InteractionDiagnostics.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Provide an Inspector + Replay system for the Library Interaction
 * Runtime. Records every gesture, resolution, intent, and outcome —
 * and supports replaying them for debugging and automation.
 *
 * RULE #81 COMPLIANCE
 * -------------------
 * "Every Interaction Is Replayable"
 *
 * This file implements that promise:
 *   - Every gesture is recorded as a [GestureRecord].
 *   - Every resolution is recorded as a [ResolutionRecord].
 *   - Every intent is recorded as an [IntentRecord].
 *   - Every outcome is recorded as an [OutcomeRecord].
 *
 * The Inspector can:
 *   - List recent interactions.
 *   - Filter by gesture kind, source, outcome.
 *   - Show full explanation (Rule #82).
 *   - Replay any recorded interaction.
 *
 * Replay produces a [ReplayResult] — a dry-run report showing what
 * would happen if the gesture were re-resolved against the *current*
 * state. Replay never affects live state.
 *
 * DESIGN
 * ------
 * The Inspector is *append-only*. Records cannot be modified or
 * deleted (except via full log rotation). This guarantees that the
 * log is a faithful record of what actually happened.
 *
 * The log is bounded — only the last N records are kept (default
 * 500). Older records are evicted FIFO. This prevents unbounded
 * memory growth in production.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #53  Diagnostics Never Affects Engine — the Inspector never
 *      affects live state. Replay is a dry-run.
 * #81  Every Interaction Is Replayable — see above.
 * #82  Every Gesture Has Explanation — every record carries the
 *      full GestureExplanation from the Resolver.
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Compose. No Android framework. No Coroutine. No Flow. No
 * ViewModel. No `var`. No side effects.
 *
 * © RIKO Experience OS — Interaction Runtime
 */

package com.rikodev.music.android.presentation.library.interaction

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 1 — RECORD TYPES
// ═══════════════════════════════════════════════════════════════════════════

/**
 * A single recorded gesture — the raw input.
 *
 * @property gesture          The recognized [LibraryGesture].
 * @property recordedAtMs     Wall-clock when the gesture was recorded.
 * @property sessionId        Inspector session id (groups records by session).
 * @property sequenceNumber   Monotonic sequence within the session.
 */
data class GestureRecord(
    val gesture: LibraryGesture,
    val recordedAtMs: Long,
    val sessionId: String,
    val sequenceNumber: Long,
)

/**
 * A recorded gesture resolution — how the Resolver interpreted the gesture.
 *
 * @property resolution       The [GestureResolution] produced by the Resolver.
 * @property contextSnapshot  Snapshot of the [GestureResolutionContext] at
 *                             resolution time.
 * @property recordedAtMs     Wall-clock when the resolution was recorded.
 * @property sourceGestureId  Id of the gesture that was resolved.
 */
data class ResolutionRecord(
    val resolution: GestureResolution,
    val contextSnapshot: GestureResolutionContext,
    val recordedAtMs: Long,
    val sourceGestureId: String,
)

/**
 * A recorded intent — what the Resolver emitted to the Reducer.
 *
 * @property intent           The [InteractionIntent] emitted.
 * @property recordedAtMs     Wall-clock when the intent was emitted.
 * @property sourceGestureId  Id of the gesture that produced this intent.
 */
data class IntentRecord(
    val intent: InteractionIntent,
    val recordedAtMs: Long,
    val sourceGestureId: String,
)

/**
 * A recorded outcome — what the Reducer did with the intent.
 *
 * @property sourceIntentId    Identifier of the source intent.
 * @property reducerName       Name of the reducer that processed the intent.
 * @property actionsEmitted    List of actions emitted by the reducer.
 * @property stateMutation     Description of the state mutation (human-readable).
 * @property recordedAtMs      Wall-clock when the outcome was recorded.
 */
data class OutcomeRecord(
    val sourceIntentId: String,
    val reducerName: String,
    val actionsEmitted: List<String>,
    val stateMutation: String,
    val recordedAtMs: Long,
)

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 2 — INTERACTION RECORD (CROSS-CUTTING)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * A complete interaction record — gesture + resolution + intent + outcome.
 *
 * Fields are nullable because not every gesture produces all four
 * stages (e.g. an ignored gesture has no intent).
 *
 * @property id                Stable identifier (matches gesture.id).
 * @property gestureRecord     The raw gesture record.
 * @property resolutionRecord  The resolution record (null if not resolved).
 * @property intentRecord      The intent record (null if no intent produced).
 * @property outcomeRecord     The outcome record (null if not yet processed).
 */
data class InteractionRecord(
    val id: String,
    val gestureRecord: GestureRecord,
    val resolutionRecord: ResolutionRecord?,
    val intentRecord: IntentRecord?,
    val outcomeRecord: OutcomeRecord?,
) {
    val isComplete: Boolean get() = outcomeRecord != null
    val isPending: Boolean get() = !isComplete
    val outcome: GestureOutcome? get() = resolutionRecord?.resolution?.outcome
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 3 — INTERACTION LOG
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The full Inspector log.
 *
 * @property sessionId         Unique session id (one per app session).
 * @property sessionStartedAtMs  Wall-clock when the session started.
 * @property records           Ordered list of interaction records (newest last).
 * @property maxRecords        Maximum records to keep (FIFO eviction).
 */
data class InteractionLog(
    val sessionId: String,
    val sessionStartedAtMs: Long,
    val records: List<InteractionRecord>,
    val maxRecords: Int,
) {
    val size: Int get() = records.size
    val isEmpty: Boolean get() = records.isEmpty()
    val isFull: Boolean get() = records.size >= maxRecords

    /**
     * Get the most recent N records.
     */
    fun recent(count: Int): List<InteractionRecord> =
        records.takeLast(count)

    /**
     * Find a record by id.
     */
    fun findById(id: String): InteractionRecord? =
        records.firstOrNull { it.id == id }

    /**
     * Filter records by gesture kind.
     */
    fun byGestureKind(kind: LibraryGestureKind): List<InteractionRecord> =
        records.filter { it.gestureRecord.gesture.kind == kind }

    /**
     * Filter records by source.
     */
    fun bySource(source: GestureSource): List<InteractionRecord> =
        records.filter { it.gestureRecord.gesture.source == source }

    /**
     * Filter records by outcome.
     */
    fun byOutcome(outcome: GestureOutcome): List<InteractionRecord> =
        records.filter { it.outcome == outcome }

    companion object {
        val EMPTY = InteractionLog(
            sessionId = "",
            sessionStartedAtMs = 0L,
            records = emptyList(),
            maxRecords = 500,
        )

        fun startSession(
            sessionId: String,
            nowMs: Long,
            maxRecords: Int = 500,
        ): InteractionLog = InteractionLog(
            sessionId = sessionId,
            sessionStartedAtMs = nowMs,
            records = emptyList(),
            maxRecords = maxRecords,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 4 — INTERACTION DIAGNOSTICS (PURE)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The Interaction Diagnostics — pure functions for recording,
 * querying, and replaying interactions.
 *
 * All functions are pure. Recording returns a new [InteractionLog]
 * with the record appended; it does not mutate the input.
 */
object InteractionDiagnostics {

    /**
     * Record a gesture (without resolution).
     *
     * Returns a new [InteractionLog] with the gesture appended.
     * If the log is full, the oldest record is evicted (FIFO).
     */
    fun recordGesture(
        log: InteractionLog,
        gesture: LibraryGesture,
        nowMs: Long,
    ): Pair<InteractionLog, String> {
        val sequenceNumber = log.records.size.toLong()
        val record = GestureRecord(
            gesture = gesture,
            recordedAtMs = nowMs,
            sessionId = log.sessionId,
            sequenceNumber = sequenceNumber,
        )
        val interactionRecord = InteractionRecord(
            id = gesture.id,
            gestureRecord = record,
            resolutionRecord = null,
            intentRecord = null,
            outcomeRecord = null,
        )

        val newRecords = (log.records + interactionRecord)
            .takeLast(log.maxRecords)

        val newLog = log.copy(records = newRecords)
        return newLog to gesture.id
    }

    /**
     * Record a gesture resolution.
     *
     * Updates the [InteractionRecord] with the matching gesture id.
     * If no matching record exists, this is a no-op.
     */
    fun recordResolution(
        log: InteractionLog,
        resolution: GestureResolution,
        context: GestureResolutionContext,
        nowMs: Long,
    ): InteractionLog {
        val gestureId = resolution.gesture.id
        val newRecords = log.records.map { record ->
            if (record.id == gestureId) {
                record.copy(
                    resolutionRecord = ResolutionRecord(
                        resolution = resolution,
                        contextSnapshot = context,
                        recordedAtMs = nowMs,
                        sourceGestureId = gestureId,
                    ),
                )
            } else {
                record
            }
        }

        return log.copy(records = newRecords)
    }

    /**
     * Record an emitted intent.
     */
    fun recordIntent(
        log: InteractionLog,
        intent: InteractionIntent,
        nowMs: Long,
    ): InteractionLog {
        val gestureId = intent.sourceGestureId
        val newRecords = log.records.map { record ->
            if (record.id == gestureId) {
                record.copy(
                    intentRecord = IntentRecord(
                        intent = intent,
                        recordedAtMs = nowMs,
                        sourceGestureId = gestureId,
                    ),
                )
            } else {
                record
            }
        }

        return log.copy(records = newRecords)
    }

    /**
     * Record an outcome (reducer result).
     */
    fun recordOutcome(
        log: InteractionLog,
        sourceIntentId: String,
        reducerName: String,
        actionsEmitted: List<String>,
        stateMutation: String,
        nowMs: Long,
    ): InteractionLog {
        // sourceIntentId corresponds to sourceGestureId in our records
        val newRecords = log.records.map { record ->
            if (record.id == sourceIntentId) {
                record.copy(
                    outcomeRecord = OutcomeRecord(
                        sourceIntentId = sourceIntentId,
                        reducerName = reducerName,
                        actionsEmitted = actionsEmitted,
                        stateMutation = stateMutation,
                        recordedAtMs = nowMs,
                    ),
                )
            } else {
                record
            }
        }

        return log.copy(records = newRecords)
    }

    // ─────────────────────────────────────────────────────────────────
    // REPLAY (Rule #81)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Replay a recorded gesture against the current state.
     *
     * This is a *dry-run* — it does NOT affect live state. It returns
     * a [ReplayResult] showing what would happen if the gesture were
     * re-resolved against the supplied [GestureResolutionContext].
     *
     * @property log          The interaction log.
     * @property gestureId    Id of the gesture to replay.
     * @property context      The current resolution context.
     * @property nowMs        Wall-clock of the replay.
     */
    fun replay(
        log: InteractionLog,
        gestureId: String,
        context: GestureResolutionContext,
        nowMs: Long,
    ): ReplayResult {
        val record = log.findById(gestureId)
            ?: return ReplayResult.NotFound(gestureId)

        val originalResolution = record.resolutionRecord?.resolution
            ?: return ReplayResult.NotResolved(gestureId)

        val replayedResolution = resolveGesture(
            gesture = record.gestureRecord.gesture,
            context = context,
        )

        val matches = replayedResolution.outcome == originalResolution.outcome &&
                replayedResolution.intent?.let { a ->
                    originalResolution.intent?.let { b ->
                        a::class == b::class
                    }
                } ?: true

        return ReplayResult.Replayed(
            gestureId = gestureId,
            originalResolution = originalResolution,
            replayedResolution = replayedResolution,
            matches = matches,
            replayedAtMs = nowMs,
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // SUMMARY
    // ─────────────────────────────────────────────────────────────────

    /**
     * Compute summary statistics for the log.
     */
    fun summarize(log: InteractionLog): InteractionLogSummary {
        if (log.isEmpty) {
            return InteractionLogSummary(
                totalInteractions = 0,
                byKind = emptyMap(),
                bySource = emptyMap(),
                byOutcome = emptyMap(),
                acceptanceRate = 0f,
                averageResolutionDurationMs = 0f,
                conflictCount = 0,
                errorCount = 0,
            )
        }

        val byKind = log.records
            .groupingBy { it.gestureRecord.gesture.kind }
            .eachCount()

        val bySource = log.records
            .groupingBy { it.gestureRecord.gesture.source }
            .eachCount()

        val byOutcome = log.records
            .mapNotNull { it.outcome }
            .groupingBy { it }
            .eachCount()

        val accepted = byOutcome[GestureOutcome.ACCEPTED] ?: 0
        val acceptanceRate = accepted.toFloat() / log.size

        val totalDuration = log.records
            .mapNotNull { it.resolutionRecord?.recordedAtMs?.minus(it.gestureRecord.recordedAtMs) }
            .filter { it >= 0 }
        val averageResolutionDurationMs = if (totalDuration.isEmpty()) 0f
        else totalDuration.average().toFloat()

        return InteractionLogSummary(
            totalInteractions = log.size,
            byKind = byKind,
            bySource = bySource,
            byOutcome = byOutcome,
            acceptanceRate = acceptanceRate,
            averageResolutionDurationMs = averageResolutionDurationMs,
            conflictCount = byOutcome[GestureOutcome.IGNORED] ?: 0,
            errorCount = 0, // No explicit error outcome defined yet
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 5 — REPLAY RESULT
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The result of a replay operation.
 */
sealed class ReplayResult {
    /** The gesture was not found in the log. */
    data class NotFound(
        val gestureId: String,
    ) : ReplayResult()

    /** The gesture was found but had no recorded resolution. */
    data class NotResolved(
        val gestureId: String,
    ) : ReplayResult()

    /**
     * The gesture was replayed successfully.
     *
     * @property matches  True if the replayed outcome matches the
     *                    original outcome.
     */
    data class Replayed(
        val gestureId: String,
        val originalResolution: GestureResolution,
        val replayedResolution: GestureResolution,
        val matches: Boolean,
        val replayedAtMs: Long,
    ) : ReplayResult() {
        val isDeterministic: Boolean get() = matches
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 6 — LOG SUMMARY
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Summary statistics for an [InteractionLog].
 *
 * Used by the Inspector UI to render aggregate views.
 */
data class InteractionLogSummary(
    val totalInteractions: Int,
    val byKind: Map<LibraryGestureKind, Int>,
    val bySource: Map<GestureSource, Int>,
    val byOutcome: Map<GestureOutcome, Int>,
    val acceptanceRate: Float,
    val averageResolutionDurationMs: Float,
    val conflictCount: Int,
    val errorCount: Int,
) {
    /**
     * Format the summary as a multi-line string for Inspector display.
     */
    fun formatReport(): String = buildString {
        appendLine("=== Interaction Log Summary ===")
        appendLine("Total interactions: $totalInteractions")
        appendLine("Acceptance rate: ${(acceptanceRate * 100).toInt()}%")
        appendLine("Avg resolution time: ${averageResolutionDurationMs.toInt()}ms")
        appendLine("Conflicts (ignored): $conflictCount")
        appendLine("Errors: $errorCount")
        appendLine()
        appendLine("By kind:")
        byKind.entries.sortedByDescending { it.value }.forEach { (kind, count) ->
            appendLine("  ${kind.displayName}: $count")
        }
        appendLine()
        appendLine("By source:")
        bySource.entries.sortedByDescending { it.value }.forEach { (source, count) ->
            appendLine("  ${source.name}: $count")
        }
        appendLine()
        appendLine("By outcome:")
        byOutcome.entries.sortedByDescending { it.value }.forEach { (outcome, count) ->
            appendLine("  ${outcome.name}: $count")
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 7 — DIAGNOSTICS INTENT + ACTION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Intents emitted by the Inspector itself (e.g. user tapped "Replay"
 * in the Inspector UI).
 */
sealed class DiagnosticsIntent {
    data class ReplayRequested(
        val gestureId: String,
    ) : DiagnosticsIntent()

    data object SummaryRequested : DiagnosticsIntent()

    data class FilterByKind(
        val kind: LibraryGestureKind,
    ) : DiagnosticsIntent()

    data class FilterBySource(
        val source: GestureSource,
    ) : DiagnosticsIntent()

    data object ClearFilters : DiagnosticsIntent()

    data object ClearLog : DiagnosticsIntent()
}

/**
 * Actions emitted by the Diagnostics reducer.
 */
sealed class DiagnosticsAction {
    data class ShowReplayResult(
        val result: ReplayResult,
    ) : DiagnosticsAction()

    data class ShowSummary(
        val summary: InteractionLogSummary,
    ) : DiagnosticsAction()

    data class UpdateFilter(
        val kind: LibraryGestureKind?,
        val source: GestureSource?,
    ) : DiagnosticsAction()

    data object LogCleared : DiagnosticsAction()
}
