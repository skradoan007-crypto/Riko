/*
 * RIKO EXPERIENCE OS — PRODUCTION-GRADE ROADMAP v2.0
 * Milestone 2.9 — Experience Validation
 *
 * File 8 / 10 — ValidationReport.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * The structured report format for all M2.9 validation gates. Each
 * gate produces a [GateReport]; the [ValidationReport] aggregates
 * all six gate reports plus the final [ExitDecision].
 *
 * The report is the single artifact handed off to the analysis phase
 * (Phase 2). It is consumed by [ValidationReportSerializer] to export
 * as JSON, and by [ValidationPreviewKit] for living documentation.
 *
 * ARCHITECTURE
 * ------------
 *
 *   ┌──────────────────────────────────────────────────────────────┐
 *   │                   VALIDATION REPORT                          │
 *   │                                                              │
 *   │  Header                                                      │
 *   │    - Report ID                                               │
 *   │    - Generated at                                            │
 *   │    - Project version                                         │
 *   │    - Validation run ID                                       │
 *   │    - Device fingerprint                                      │
 *   │                                                              │
 *   │  Body — 6 Gate Reports                                       │
 *   │    G1 — Library Reality Test                                 │
 *   │    G2 — Memory Audit                                         │
 *   │    G3 — Frame Timing                                         │
 *   │    G4 — Animation Continuity                                 │
 *   │    G5 — Stress Test                                          │
 *   │    G6 — Identity Test ⭐                                      │
 *   │                                                              │
 *   │  Footer — Exit Decision                                      │
 *   │    - Approved / Blocked                                      │
 *   │    - Blocking gates (if any)                                 │
 *   │    - Next steps                                              │
 *   └──────────────────────────────────────────────────────────────┘
 *
 * RULES ENFORCED
 * --------------
 * #21  Traceable — every report has stable IDs and timestamps
 * #53  Diagnostics Never Affects Engine — report is observation only
 * #76  Performance Guards Never Optimize — report never triggers fixes
 *
 * © RIKO Experience OS — Validation Tier
 */

package com.rikodev.music.android.core.validation

// ═══════════════════════════════════════════════════════════════════════════
// REPORT HEADER
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Header metadata for a validation report.
 *
 * @property reportId           Stable unique ID.
 * @property generatedAtMs      Wall-clock generation time.
 * @property projectVersion     RIKO version under validation (e.g. "M2.9-rc1").
 * @property validationRunId    ID of this validation run.
 * @property deviceFingerprint  Device the validation ran on.
 * @property durationMs         Total validation run duration.
 * @property generatedBy        Who/what generated the report.
 */
data class ReportHeader(
    val reportId: String,
    val generatedAtMs: Long,
    val projectVersion: String,
    val validationRunId: String,
    val deviceFingerprint: String,
    val durationMs: Long,
    val generatedBy: String,
) {
    init {
        require(reportId.isNotBlank()) { "Report ID cannot be blank" }
        require(projectVersion.isNotBlank()) { "Project version cannot be blank" }
        require(validationRunId.isNotBlank()) { "Validation run ID cannot be blank" }
        require(durationMs >= 0) { "Duration cannot be negative" }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// GATE REPORT (POLYMORPHIC)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Marker for per-gate report payloads.
 *
 * Each gate has its own report type so the analysis phase can
 * deserialize and inspect them independently.
 */
sealed interface GateReport {

    /** Which gate this report is for. */
    val gateId: ValidationGateId

    /** Verdict for this gate. */
    val verdict: GateVerdict

    /** When this gate was evaluated. */
    val evaluatedAtMs: Long

    /** Duration the gate ran for. */
    val durationMs: Long

    /** Human-readable summary. */
    val summary: String
}

/**
 * Verdict for a single gate.
 */
sealed class GateVerdict {
    /** Gate passed. */
    data object Passed : GateVerdict()

    /** Gate did not run or did not collect enough data. */
    data class Incomplete(val reason: String) : GateVerdict()

    /** Gate failed. Includes blocking reasons. */
    data class Failed(val reason: String, val blockingItems: List<String>) : GateVerdict()

    /** Gate was skipped (e.g. waived by Product Director). */
    data class Skipped(val reason: String) : GateVerdict()

    val isPassed: Boolean get() = this is Passed
    val isBlocked: Boolean get() = this is Failed
}

// ─── G1: Library Reality Test Report ───────────────────────────────────────

/**
 * Report for Gate G1 — Library Reality Test.
 */
data class G1LibraryRealityReport(
    override val evaluatedAtMs: Long,
    override val durationMs: Long,
    val tierResults: List<TierResultSummary>,
    override val verdict: GateVerdict,
) : GateReport {

    override val gateId: ValidationGateId get() = ValidationGateId.LIBRARY_REALITY

    override val summary: String
        get() = buildString {
            append("G1 — Library Reality: ")
            append("${tierResults.size} tiers tested, ")
            append("${tierResults.count { it.passed }} passed, ")
            append("${tierResults.count { !it.passed }} failed")
        }
}

/**
 * Summary of one tier's result, for the report.
 */
data class TierResultSummary(
    val tier: LibraryScaleTier,
    val passed: Boolean,
    val failureCount: Int,
    val failureSummaries: List<String>,
    val coldStartMs: Long,
    val scrollFrameP95Ms: Long,
    val rankingMs: Long,
    val heapMb: Long,
)

// ─── G2: Memory Audit Report ───────────────────────────────────────────────

/**
 * Report for Gate G2 — Memory Audit.
 */
data class G2MemoryAuditReport(
    override val evaluatedAtMs: Long,
    override val durationMs: Long,
    val cyclesCompleted: Int,
    val totalLeaksDetected: Int,
    val totalRetainedBytes: Long,
    val criticalLeakCount: Int,
    val highLeakCount: Int,
    val surfaceSummaries: List<LeakSurfaceSummaryAggregate>,
    override val verdict: GateVerdict,
) : GateReport {

    override val gateId: ValidationGateId get() = ValidationGateId.MEMORY_AUDIT

    override val summary: String
        get() = buildString {
            append("G2 — Memory Audit: ")
            append("$cyclesCompleted cycles, ")
            append("$totalLeaksDetected leaks, ")
            append("${totalRetainedBytes / 1024}KB retained, ")
            append("$criticalLeakCount critical, $highLeakCount high")
        }
}

/**
 * Aggregated leak summary per surface.
 */
data class LeakSurfaceSummaryAggregate(
    val surface: LeakSurface,
    val totalLeakCount: Int,
    val totalBytes: Long,
    val maxSeverity: LeakSeverity,
)

// ─── G3: Frame Timing Report ───────────────────────────────────────────────

/**
 * Report for Gate G3 — Frame Timing.
 */
data class G3FrameTimingReport(
    override val evaluatedAtMs: Long,
    override val durationMs: Long,
    val perRateEvaluations: List<RateEvaluationSummary>,
    override val verdict: GateVerdict,
) : GateReport {

    override val gateId: ValidationGateId get() = ValidationGateId.FRAME_TIMING

    override val summary: String
        get() = buildString {
            append("G3 — Frame Timing: ")
            append("${perRateEvaluations.size} rates tested, ")
            append("${perRateEvaluations.count { it.passed }} passed, ")
            append("${perRateEvaluations.count { !it.passed }} failed")
        }
}

/**
 * Summary of one refresh rate's evaluation.
 */
data class RateEvaluationSummary(
    val rate: RefreshRate,
    val passed: Boolean,
    val sampleCount: Int,
    val p50Ms: Double,
    val p95Ms: Double,
    val p99Ms: Double,
    val jankRate: Double,
    val longestGoodStreak: Int,
    val failureSummaries: List<String>,
)

// ─── G4: Animation Continuity Report ──────────────────────────────────────

/**
 * Report for Gate G4 — Animation Continuity.
 */
data class G4AnimationContinuityReport(
    override val evaluatedAtMs: Long,
    override val durationMs: Long,
    val cyclesCompleted: Int,
    val cyclesPassed: Int,
    val cyclesFailed: Int,
    val cycleDurationMeanMs: Long,
    val cycleDurationStdDevMs: Double,
    val memoryGrowthPerCycleBytes: Double,
    val recompositionGrowthPerCycle: Double,
    val maxMiniPlayerDriftPx: Float,
    override val verdict: GateVerdict,
) : GateReport {

    override val gateId: ValidationGateId get() = ValidationGateId.ANIMATION_CONTINUITY

    override val summary: String
        get() = buildString {
            append("G4 — Animation Continuity: ")
            append("$cyclesCompleted cycles, ")
            append("$cyclesPassed passed, ")
            append("$cyclesFailed failed, ")
            append("drift=${"%.2f".format(maxMiniPlayerDriftPx)}px")
        }
}

// ─── G5: Stress Test Report ────────────────────────────────────────────────

/**
 * Report for Gate G5 — Stress Test.
 */
data class G5StressTestReport(
    override val evaluatedAtMs: Long,
    override val durationMs: Long,
    val totalActions: Int,
    val successCount: Int,
    val failureCount: Int,
    val crashCount: Int,
    val anrCount: Int,
    val stateInconsistentCount: Int,
    val successRate: Double,
    val criticalFailuresSummary: List<String>,
    val failuresByCategory: Map<String, Int>,
    override val verdict: GateVerdict,
) : GateReport {

    override val gateId: ValidationGateId get() = ValidationGateId.STRESS_TEST

    override val summary: String
        get() = buildString {
            append("G5 — Stress Test: ")
            append("$totalActions actions, ")
            append("${"%.2f".format(successRate * 100)}% success, ")
            append("$crashCount crashes, $anrCount ANRs")
        }
}

// ─── G6: Identity Test Report ⭐ ───────────────────────────────────────────

/**
 * Report for Gate G6 — Identity Test. ⭐
 */
data class G6IdentityTestReport(
    override val evaluatedAtMs: Long,
    override val durationMs: Long,
    val reviewersPassed: Int,
    val totalReviewers: Int,
    val meanRecognitionMs: Long,
    val mostCitedSignal: IdentitySignal?,
    val reviewerResults: List<ReviewerResultSummary>,
    override val verdict: GateVerdict,
) : GateReport {

    override val gateId: ValidationGateId get() = ValidationGateId.IDENTITY_TEST

    override val summary: String
        get() = buildString {
            append("G6 — Identity Test ⭐: ")
            append("$reviewersPassed/$totalReviewers reviewers passed, ")
            append("mean ${meanRecognitionMs}ms, ")
            append("top signal: ${mostCitedSignal?.displayName ?: "—"}")
        }
}

/**
 * Summary of one reviewer's identity test result.
 */
data class ReviewerResultSummary(
    val reviewerId: String,
    val displayName: String,
    val familiarity: String,
    val recognitionPassCount: Int,
    val totalSessions: Int,
    val meanRecognitionMs: Long,
    val mostCitedSignal: IdentitySignal?,
    val reviewerPass: Boolean,
)

// ═══════════════════════════════════════════════════════════════════════════
// FULL VALIDATION REPORT
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The complete validation report for one M2.9 run.
 *
 * Contains the header, all six gate reports, and the final exit
 * decision. This is the single artifact handed off to analysis.
 */
data class ValidationReport(
    val header: ReportHeader,
    val g1LibraryReality: G1LibraryRealityReport?,
    val g2MemoryAudit: G2MemoryAuditReport?,
    val g3FrameTiming: G3FrameTimingReport?,
    val g4AnimationContinuity: G4AnimationContinuityReport?,
    val g5StressTest: G5StressTestReport?,
    val g6IdentityTest: G6IdentityTestReport?,
    val exitDecision: ExitDecision,
) {

    /**
     * All gate reports in canonical order.
     */
    val allGateReports: List<GateReport>
        get() = listOfNotNull(
            g1LibraryReality,
            g2MemoryAudit,
            g3FrameTiming,
            g4AnimationContinuity,
            g5StressTest,
            g6IdentityTest,
        )

    /**
     * Number of gates that passed.
     */
    val gatesPassedCount: Int
        get() = allGateReports.count { it.verdict.isPassed }

    /**
     * Number of gates that failed.
     */
    val gatesFailedCount: Int
        get() = allGateReports.count { it.verdict.isBlocked }

    /**
     * Number of gates not yet run.
     */
    val gatesIncompleteCount: Int
        get() = allGateReports.count { it.verdict is GateVerdict.Incomplete }

    /**
     * True if the exit decision approves the LOCK.
     */
    val isApproved: Boolean get() = exitDecision.isApproved

    /**
     * Generate a top-level human-readable summary.
     */
    fun topSummary(): String = buildString {
        appendLine("═══ RIKO M2.9 VALIDATION REPORT ═══")
        appendLine("Report ID:       ${header.reportId}")
        appendLine("Generated at:    ${header.generatedAtMs}")
        appendLine("Project version: ${header.projectVersion}")
        appendLine("Validation run:  ${header.validationRunId}")
        appendLine("Device:          ${header.deviceFingerprint}")
        appendLine("Duration:        ${header.durationMs / 1000}s")
        appendLine()
        appendLine("Gate Results:")
        allGateReports.forEach { report ->
            val verdictStr = when (report.verdict) {
                is GateVerdict.Passed -> "✓ PASS"
                is GateVerdict.Incomplete -> "? INCOMPLETE"
                is GateVerdict.Failed -> "✗ FAIL"
                is GateVerdict.Skipped -> "- SKIPPED"
            }
            appendLine("  [${report.gateId.code}] $verdictStr — ${report.summary}")
        }
        appendLine()
        appendLine("Exit Decision: ${if (isApproved) "APPROVED — Milestone 2 may LOCK" else "BLOCKED — see blocking gates"}")
        if (!isApproved) {
            val blocked = exitDecision as ExitDecision.Blocked
            blocked.blockingGates.forEach { gate ->
                appendLine("  ⚠ [${gate.id.code}] ${gate.title}: ${gate.reason}")
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// REPORT BUILDER
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Builder for constructing a [ValidationReport] incrementally.
 *
 * Gates can be added as they complete. The exit decision is
 * computed at [build] time from the gate verdicts.
 */
class ValidationReportBuilder(
    private val header: ReportHeader,
) {

    private var g1: G1LibraryRealityReport? = null
    private var g2: G2MemoryAuditReport? = null
    private var g3: G3FrameTimingReport? = null
    private var g4: G4AnimationContinuityReport? = null
    private var g5: G5StressTestReport? = null
    private var g6: G6IdentityTestReport? = null

    fun withG1(report: G1LibraryRealityReport) = apply { g1 = report }
    fun withG2(report: G2MemoryAuditReport) = apply { g2 = report }
    fun withG3(report: G3FrameTimingReport) = apply { g3 = report }
    fun withG4(report: G4AnimationContinuityReport) = apply { g4 = report }
    fun withG5(report: G5StressTestReport) = apply { g5 = report }
    fun withG6(report: G6IdentityTestReport) = apply { g6 = report }

    /**
     * Build the final report. The exit decision is computed from
     * the gate verdicts — if any blocking gate failed or is incomplete,
     * the decision is Blocked.
     */
    fun build(): ValidationReport {
        val allReports = listOfNotNull(g1, g2, g3, g4, g5, g6)
        val blocking = allReports.filter { gate ->
            gateIdIsBlocking(gate.gateId) && !gate.verdict.isPassed
        }
        val exitDecision = if (blocking.isEmpty() && allReports.size == 6) {
            ExitDecision.Approved(
                gatesPassed = allReports.count { it.verdict.isPassed },
                totalGates = 6,
            )
        } else {
            ExitDecision.Blocked(
                blockingGates = blocking.map { gate ->
                    BlockingGate(
                        id = gate.gateId,
                        title = gate.gateId.name,
                        reason = when (val v = gate.verdict) {
                            is GateVerdict.Failed -> v.reason
                            is GateVerdict.Incomplete -> v.reason
                            is GateVerdict.Skipped -> v.reason
                            is GateVerdict.Passed -> "passed"
                        },
                    )
                }
            )
        }
        return ValidationReport(
            header = header,
            g1LibraryReality = g1,
            g2MemoryAudit = g2,
            g3FrameTiming = g3,
            g4AnimationContinuity = g4,
            g5StressTest = g5,
            g6IdentityTest = g6,
            exitDecision = exitDecision,
        )
    }

    private fun gateIdIsBlocking(id: ValidationGateId): Boolean = when (id) {
        ValidationGateId.LIBRARY_REALITY,
        ValidationGateId.MEMORY_AUDIT,
        ValidationGateId.FRAME_TIMING,
        ValidationGateId.ANIMATION_CONTINUITY,
        ValidationGateId.STRESS_TEST,
        ValidationGateId.IDENTITY_TEST -> true
    }
}
