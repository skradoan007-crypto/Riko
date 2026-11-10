/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.2 — Library Screen Runtime
 *
 * File 8 / 8 — LibraryRuntimeDiagnostics.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [LibraryRuntimeDiagnostics] is the *observability layer* for the
 * Library Screen Runtime. It records what the runtime did, why it
 * did it, and how long it took — without ever affecting behavior.
 *
 * Rule #53 — Diagnostics Never Affects Engine.
 * Every diagnostic field is read-only observation. The Reducer never
 * branches on these fields. The Coordinator never branches on them.
 * The Inspector overlay reads them; nothing else does.
 *
 * WHAT THIS FILE IS NOT
 * ---------------------
 * This file is NOT a logger. It does not write to logcat, files, or
 * remote telemetry. It is an in-memory record the host can query.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #42  Diagnostics First-Class     — diagnostics is a first-class citizen
 * #53  Diagnostics Never Affects Engine — observation only
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Android (no Log.d / Log.e / Log.w), no Coroutine, no Flow,
 * no file IO. Pure Kotlin data + a thread-safe accumulator.
 *
 * © RIKO Experience OS — Runtime Tier
 */

package com.rikodev.music.android.presentation.library.screen.runtime

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Severity of a runtime-layer warning.
 */
enum class RuntimeWarningSeverity(
    internal val description: String,
) {
    /** Informational — no action needed. */
    INFO("Informational."),

    /** Soft warning — suboptimal but functional. */
    SOFT("Soft warning."),

    /** Hard warning — degraded behavior. */
    HARD("Hard warning."),

    /** Critical — runtime failed. */
    CRITICAL("Critical."),
}

/**
 * A single runtime-layer warning.
 *
 * @property severity       How serious.
 * @property sectionId      Stable ID of the affected section, or null.
 * @property category       Warning category.
 * @property message        Human-readable message.
 * @property timestampMs    Wall-clock time (epoch ms).
 */
data class RuntimeWarning(
    val severity: RuntimeWarningSeverity,
    val sectionId: String?,
    val category: RuntimeWarningCategory,
    val message: String,
    val timestampMs: Long,
)

/**
 * Categories of runtime-layer warnings.
 */
enum class RuntimeWarningCategory(
    internal val description: String,
) {
    /** Reducer received an unexpected event. */
    UNEXPECTED_EVENT("Unexpected event."),

    /** Coordinator received an action it cannot execute. */
    ACTION_NOT_EXECUTABLE("Action not executable."),

    /** Engine call failed. */
    ENGINE_CALL_FAILED("Engine call failed."),

    /** Navigation failed. */
    NAVIGATION_FAILED("Navigation failed."),

    /** Selection limit reached. */
    SELECTION_LIMIT_REACHED("Selection limit reached."),

    /** Performance budget exceeded. */
    PERFORMANCE_BUDGET_EXCEEDED("Performance budget exceeded."),

    /** State transition took too long. */
    SLOW_STATE_TRANSITION("Slow state transition."),

    /** Action emitted with no Coordinator handler. */
    UNHANDLED_ACTION("Unhandled action."),
}

/**
 * A runtime event — recorded for diagnostics.
 *
 * @property eventKey       Stable key for the event.
 * @property payload        Optional payload (string-string map).
 * @property timestampMs    Wall-clock time (epoch ms).
 */
data class RuntimeEvent(
    val eventKey: String,
    val payload: Map<String, String>,
    val timestampMs: Long,
)

/**
 * A state-transition record.
 *
 * @property fromPhase      Previous phase.
 * @property toPhase        New phase.
 * @property triggerEventKey  Event that triggered the transition.
 * @property timestampMs    Wall-clock time (epoch ms).
 * @property durationMs     How long the transition took.
 */
data class RuntimeStateTransition(
    val fromPhase: String,
    val toPhase: String,
    val triggerEventKey: String,
    val timestampMs: Long,
    val durationMs: Long,
)

/**
 * Per-cycle runtime diagnostic record.
 *
 * @property cycleId            Stable ID for the cycle.
 * @property eventCategory      Category of the triggering event.
 * @property eventKey           Stable key of the triggering event.
 * @property actionsEmitted     Number of actions emitted.
 * @property requestsEmitted    Number of coordinator requests emitted.
 * @property reducerDurationMs  Reducer duration (ms).
 * @property coordinatorDurationMs  Coordinator duration (ms).
 * @property totalDurationMs    Total cycle duration (ms).
 */
data class RuntimeCycleRecord(
    val cycleId: String,
    val eventCategory: String,
    val eventKey: String,
    val actionsEmitted: Int,
    val requestsEmitted: Int,
    val reducerDurationMs: Long,
    val coordinatorDurationMs: Long,
    val totalDurationMs: Long,
) {
    val isSlow: Boolean get() = totalDurationMs > 16L
}

/**
 * Rolling-window diagnostics for the Library Screen Runtime.
 *
 * Keeps the last [maxCycles] cycle records + the last [maxWarnings]
 * warnings + the last [maxTransitions] state transitions. Thread-safe.
 *
 * @property maxCycles        Maximum cycle records to keep.
 * @property maxWarnings      Maximum warnings to keep.
 * @property maxTransitions   Maximum state transitions to keep.
 * @property maxEvents        Maximum events to keep.
 */
class LibraryRuntimeDiagnostics(
    private val maxCycles: Int = 20,
    private val maxWarnings: Int = 50,
    private val maxTransitions: Int = 20,
    private val maxEvents: Int = 100,
) {
    private val cycles = CopyOnWriteArrayList<RuntimeCycleRecord>()
    private val warnings = CopyOnWriteArrayList<RuntimeWarning>()
    private val transitions = CopyOnWriteArrayList<RuntimeStateTransition>()
    private val events = CopyOnWriteArrayList<RuntimeEvent>()

    /**
     * Append a cycle record.
     */
    fun recordCycle(record: RuntimeCycleRecord) {
        cycles.add(record)
        while (cycles.size > maxCycles) cycles.removeAt(0)
    }

    /**
     * Append a warning.
     */
    fun recordWarning(warning: RuntimeWarning) {
        warnings.add(warning)
        while (warnings.size > maxWarnings) warnings.removeAt(0)
    }

    /**
     * Append a state transition.
     */
    fun recordTransition(transition: RuntimeStateTransition) {
        transitions.add(transition)
        while (transitions.size > maxTransitions) transitions.removeAt(0)
    }

    /**
     * Append a runtime event.
     */
    fun recordEvent(event: RuntimeEvent) {
        events.add(event)
        while (events.size > maxEvents) events.removeAt(0)
    }

    /**
     * Recent cycle records, newest first.
     */
    fun recentCycles(): List<RuntimeCycleRecord> = cycles.reversed()

    /**
     * Recent warnings, newest first.
     */
    fun recentWarnings(): List<RuntimeWarning> = warnings.reversed()

    /**
     * Recent state transitions, newest first.
     */
    fun recentTransitions(): List<RuntimeStateTransition> = transitions.reversed()

    /**
     * Recent events, newest first.
     */
    fun recentEvents(): List<RuntimeEvent> = events.reversed()

    /**
     * Count of warnings by severity.
     */
    fun warningCountBySeverity(): Map<RuntimeWarningSeverity, Int> =
        warnings.groupingBy { it.severity }.eachCount()

    /**
     * Average cycle duration over recent cycles.
     */
    fun averageCycleDurationMs(): Long {
        if (cycles.isEmpty()) return 0L
        return cycles.sumOf { it.totalDurationMs } / cycles.size
    }

    /**
     * Average reducer duration over recent cycles.
     */
    fun averageReducerDurationMs(): Long {
        if (cycles.isEmpty()) return 0L
        return cycles.sumOf { it.reducerDurationMs } / cycles.size
    }

    /**
     * Average coordinator duration over recent cycles.
     */
    fun averageCoordinatorDurationMs(): Long {
        if (cycles.isEmpty()) return 0L
        return cycles.sumOf { it.coordinatorDurationMs } / cycles.size
    }

    /**
     * True if the last cycle was slow (above [slowThresholdMs]).
     */
    fun wasLastCycleSlow(slowThresholdMs: Long = 32L): Boolean {
        val last = cycles.lastOrNull() ?: return false
        return last.totalDurationMs > slowThresholdMs
    }

    /**
     * Number of slow cycles in the recent window.
     */
    fun slowCycleCount(slowThresholdMs: Long = 32L): Int =
        cycles.count { it.totalDurationMs > slowThresholdMs }

    /**
     * Clear all records.
     */
    fun clear() {
        cycles.clear()
        warnings.clear()
        transitions.clear()
        events.clear()
    }
}

/**
 * Helpers for producing common warnings.
 */
object RuntimeWarnings {

    fun unexpectedEvent(
        eventKey: String,
        sectionId: String?,
        nowMs: Long,
    ): RuntimeWarning = RuntimeWarning(
        severity = RuntimeWarningSeverity.SOFT,
        sectionId = sectionId,
        category = RuntimeWarningCategory.UNEXPECTED_EVENT,
        message = "Reducer received unexpected event: $eventKey",
        timestampMs = nowMs,
    )

    fun actionNotExecutable(
        actionKey: String,
        sectionId: String?,
        nowMs: Long,
    ): RuntimeWarning = RuntimeWarning(
        severity = RuntimeWarningSeverity.HARD,
        sectionId = sectionId,
        category = RuntimeWarningCategory.ACTION_NOT_EXECUTABLE,
        message = "Coordinator cannot execute action: $actionKey",
        timestampMs = nowMs,
    )

    fun engineCallFailed(
        stableKey: String,
        errorMessage: String,
        nowMs: Long,
    ): RuntimeWarning = RuntimeWarning(
        severity = RuntimeWarningSeverity.HARD,
        sectionId = null,
        category = RuntimeWarningCategory.ENGINE_CALL_FAILED,
        message = "Engine call failed for $stableKey: $errorMessage",
        timestampMs = nowMs,
    )

    fun selectionLimitReached(
        limit: Int,
        nowMs: Long,
    ): RuntimeWarning = RuntimeWarning(
        severity = RuntimeWarningSeverity.SOFT,
        sectionId = null,
        category = RuntimeWarningCategory.SELECTION_LIMIT_REACHED,
        message = "Selection limit reached ($limit items)",
        timestampMs = nowMs,
    )

    fun performanceBudgetExceeded(
        budgetMs: Long,
        actualMs: Long,
        nowMs: Long,
    ): RuntimeWarning = RuntimeWarning(
        severity = RuntimeWarningSeverity.SOFT,
        sectionId = null,
        category = RuntimeWarningCategory.PERFORMANCE_BUDGET_EXCEEDED,
        message = "Performance budget exceeded: $budgetMs ms budget, $actualMs ms actual",
        timestampMs = nowMs,
    )

    fun slowStateTransition(
        fromPhase: String,
        toPhase: String,
        durationMs: Long,
        nowMs: Long,
    ): RuntimeWarning = RuntimeWarning(
        severity = RuntimeWarningSeverity.SOFT,
        sectionId = null,
        category = RuntimeWarningCategory.SLOW_STATE_TRANSITION,
        message = "Slow state transition: $fromPhase → $toPhase took $durationMs ms",
        timestampMs = nowMs,
    )

    fun unhandledAction(
        actionKey: String,
        nowMs: Long,
    ): RuntimeWarning = RuntimeWarning(
        severity = RuntimeWarningSeverity.HARD,
        sectionId = null,
        category = RuntimeWarningCategory.UNHANDLED_ACTION,
        message = "No Coordinator handler for action: $actionKey",
        timestampMs = nowMs,
    )
}
