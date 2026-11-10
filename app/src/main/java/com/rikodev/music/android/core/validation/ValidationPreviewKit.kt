/*
 * RIKO EXPERIENCE OS — PRODUCTION-GRADE ROADMAP v2.0
 * Milestone 2.9 — Experience Validation
 *
 * File 10 / 10 — ValidationPreviewKit.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Living Documentation for the M2.9 Validation subsystem (Rule #77).
 *
 * Each preview is a self-contained scenario that demonstrates what a
 * validation report looks like for a specific outcome. Previews are
 * used by:
 *   - The analysis phase to understand expected report shapes
 *   - New developers to learn what each gate verifies
 *   - The Product Director to sanity-check the validation design
 *
 * ARCHITECTURE
 * ------------
 *
 *   ┌──────────────────────────────────────────────────────────────┐
 *   │                VALIDATION PREVIEW KIT                        │
 *   │                                                              │
 *   │  Sample Reports                                              │
 *   │    - All-pass ideal report                                   │
 *   │    - Mixed pass/fail realistic report                        │
 *   │    - Catastrophic failure report                             │
 *   │    - Incomplete (gates missing) report                       │
 *   │                                                              │
 *   │  Sample Scenarios                                            │
 *   │    - G1 tier-by-tier preview                                 │
 *   │    - G2 leak-by-surface preview                              │
 *   │    - G3 rate-by-rate preview                                 │
 *   │    - G4 cycle-by-cycle preview                               │
 *   │    - G5 action-by-action preview                             │
 *   │    - G6 reviewer-by-reviewer preview                         │
 *   │                                                              │
 *   │  Documentation Strings                                       │
 *   │    - What each gate verifies                                 │
 *   │    - How to interpret each field                             │
 *   │    - Common failure patterns                                 │
 *   └──────────────────────────────────────────────────────────────┘
 *
 * RULES ENFORCED
 * --------------
 * #21  Traceable — previews have stable scenario names
 * #53  Diagnostics Never Affects Engine — previews are pure data
 * #77  Preview Is Documentation — every preview self-documents
 *
 * © RIKO Experience OS — Validation Tier
 */

package com.rikodev.music.android.core.validation

// ═══════════════════════════════════════════════════════════════════════════
// PREVIEW SCENARIO
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Identifies a preview scenario.
 *
 * Stable IDs are used as Compose preview names and JSON keys.
 */
enum class ValidationPreviewScenario(val displayName: String, val description: String) {

    /** Ideal case — all six gates pass cleanly. */
    IDEAL_ALL_PASS(
        "Ideal — All Gates Pass",
        "Reference scenario where every gate passes with margin. " +
            "Use this to understand the report shape when validation succeeds.",
    ),

    /** Realistic case — most gates pass, a few have warnings. */
    REALISTIC_MOSTLY_PASS(
        "Realistic — Mostly Pass",
        "Typical scenario where 4 of 6 gates pass and 2 have minor failures. " +
            "Use this to understand non-blocking failure handling.",
    ),

    /** Catastrophic case — multiple gates fail, LOCK blocked. */
    CATASTROPHIC_FAILURE(
        "Catastrophic — Multiple Failures",
        "Worst-case scenario where 3+ gates fail. LOCK is blocked. " +
            "Use this to understand the blocking gate reporting format.",
    ),

    /** Incomplete case — not all gates have run yet. */
    INCOMPLETE_MISSING_GATES(
        "Incomplete — Missing Gates",
        "Scenario where only some gates have run. Exit decision is BLOCKED " +
            "because required gates are missing.",
    ),

    /** G1-only deep dive — per-tier results. */
    G1_TIER_DEEPDIVE(
        "G1 — Tier Deep Dive",
        "Per-tier breakdown of the Library Reality Test. Shows how " +
            "10K/20K/50K/100K scale results are reported.",
    ),

    /** G2-only deep dive — per-surface leaks. */
    G2_SURFACE_DEEPDIVE(
        "G2 — Surface Deep Dive",
        "Per-surface breakdown of the Memory Audit. Shows how leaks across " +
            "Bitmap/Coroutine/Flow/Snapshot/Native are reported.",
    ),

    /** G3-only deep dive — per-rate frame timing. */
    G3_RATE_DEEPDIVE(
        "G3 — Rate Deep Dive",
        "Per-refresh-rate breakdown of the Frame Timing Audit. Shows how " +
            "30/60/90/120/144 Hz results are reported.",
    ),

    /** G4-only deep dive — per-cycle continuity. */
    G4_CYCLE_DEEPDIVE(
        "G4 — Cycle Deep Dive",
        "Per-cycle breakdown of the Animation Continuity Test. Shows how " +
            "100 cycles of morph results are reported.",
    ),

    /** G5-only deep dive — per-action stress outcomes. */
    G5_ACTION_DEEPDIVE(
        "G5 — Action Deep Dive",
        "Per-action-category breakdown of the Stress Test. Shows how " +
            "500 interleaved actions are reported.",
    ),

    /** G6-only deep dive — per-reviewer identity. */
    G6_REVIEWER_DEEPDIVE(
        "G6 — Reviewer Deep Dive ⭐",
        "Per-reviewer breakdown of the Identity Test. Shows how " +
            "3 reviewers × 3 sessions results are reported.",
    ),
}

// ═══════════════════════════════════════════════════════════════════════════
// PREVIEW KIT
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The Validation Preview Kit (Rule #77).
 *
 * Produces sample [ValidationReport] instances for documentation,
 * testing, and onboarding. Every preview is self-documenting via
 * [ValidationPreviewScenario.description].
 *
 * Usage:
 *   - In Compose previews: render a sample report UI
 *   - In tests: assert serializer round-trips sample reports
 *   - In onboarding: show new developers what reports look like
 */
object ValidationPreviewKit {

    /**
     * Get all preview scenarios in canonical order.
     */
    fun allScenarios(): List<ValidationPreviewScenario> =
        ValidationPreviewScenario.values().toList()

    /**
     * Generate the sample report for a scenario.
     */
    fun reportFor(scenario: ValidationPreviewScenario): ValidationReport =
        when (scenario) {
            ValidationPreviewScenario.IDEAL_ALL_PASS -> idealAllPassReport()
            ValidationPreviewScenario.REALISTIC_MOSTLY_PASS -> realisticMostlyPassReport()
            ValidationPreviewScenario.CATASTROPHIC_FAILURE -> catastrophicFailureReport()
            ValidationPreviewScenario.INCOMPLETE_MISSING_GATES -> incompleteMissingGatesReport()
            ValidationPreviewScenario.G1_TIER_DEEPDIVE -> g1DeepDiveReport()
            ValidationPreviewScenario.G2_SURFACE_DEEPDIVE -> g2DeepDiveReport()
            ValidationPreviewScenario.G3_RATE_DEEPDIVE -> g3DeepDiveReport()
            ValidationPreviewScenario.G4_CYCLE_DEEPDIVE -> g4DeepDiveReport()
            ValidationPreviewScenario.G5_ACTION_DEEPDIVE -> g5DeepDiveReport()
            ValidationPreviewScenario.G6_REVIEWER_DEEPDIVE -> g6DeepDiveReport()
        }

    /**
     * Generate JSON for a scenario. Useful for diffing in tests.
     */
    fun jsonFor(scenario: ValidationPreviewScenario): String {
        val report = reportFor(scenario)
        return report.toJson()
    }

    // ─── Sample report builders ──────────────────────────────────────────

    private fun sampleHeader(): ReportHeader = ReportHeader(
        reportId = "VAL-M2.9-${System.currentTimeMillis()}",
        generatedAtMs = System.currentTimeMillis(),
        projectVersion = "M2.9-rc1",
        validationRunId = "RUN-${(Math.random() * 10000).toInt()}",
        deviceFingerprint = "Pixel-7-Pro-API-34",
        durationMs = 6_300_000, // ~105 minutes
        generatedBy = "ValidationPreviewKit",
    )

    private fun idealAllPassReport(): ValidationReport {
        return ValidationReportBuilder(sampleHeader())
            .withG1(G1LibraryRealityReport(
                evaluatedAtMs = System.currentTimeMillis(),
                durationMs = 1_800_000,
                tierResults = listOf(
                    TierResultSummary(LibraryScaleTier.S1, true, 0, emptyList(), 480, 14, 180, 65),
                    TierResultSummary(LibraryScaleTier.S2, true, 0, emptyList(), 720, 15, 320, 120),
                    TierResultSummary(LibraryScaleTier.S3, true, 0, emptyList(), 1_100, 16, 480, 210),
                    TierResultSummary(LibraryScaleTier.S4, true, 0, emptyList(), 1_700, 18, 920, 360),
                ),
                verdict = GateVerdict.Passed,
            ))
            .withG2(G2MemoryAuditReport(
                evaluatedAtMs = System.currentTimeMillis(),
                durationMs = 2_700_000,
                cyclesCompleted = 8,
                totalLeaksDetected = 0,
                totalRetainedBytes = 0,
                criticalLeakCount = 0,
                highLeakCount = 0,
                surfaceSummaries = LeakSurface.values().map {
                    LeakSurfaceSummaryAggregate(it, 0, 0, LeakSeverity.LOW)
                },
                verdict = GateVerdict.Passed,
            ))
            .withG3(G3FrameTimingReport(
                evaluatedAtMs = System.currentTimeMillis(),
                durationMs = 3_600_000,
                perRateEvaluations = RefreshRate.all.map { rate ->
                    RateEvaluationSummary(
                        rate = rate,
                        passed = true,
                        sampleCount = rate.hz * 10,
                        p50Ms = rate.budgetMs * 0.7,
                        p95Ms = rate.budgetMs * 0.9,
                        p99Ms = rate.budgetMs,
                        jankRate = 0.005,
                        longestGoodStreak = rate.hz * 8,
                        failureSummaries = emptyList(),
                    )
                },
                verdict = GateVerdict.Passed,
            ))
            .withG4(G4AnimationContinuityReport(
                evaluatedAtMs = System.currentTimeMillis(),
                durationMs = 5_400_000,
                cyclesCompleted = 100,
                cyclesPassed = 100,
                cyclesFailed = 0,
                cycleDurationMeanMs = 3_200,
                cycleDurationStdDevMs = 180.0,
                memoryGrowthPerCycleBytes = 256.0,
                recompositionGrowthPerCycle = 0.1,
                maxMiniPlayerDriftPx = 0.8f,
                verdict = GateVerdict.Passed,
            ))
            .withG5(G5StressTestReport(
                evaluatedAtMs = System.currentTimeMillis(),
                durationMs = 7_200_000,
                totalActions = 500,
                successCount = 498,
                failureCount = 2,
                crashCount = 0,
                anrCount = 0,
                stateInconsistentCount = 0,
                successRate = 0.996,
                criticalFailuresSummary = emptyList(),
                failuresByCategory = mapOf("User Action" to 2),
                verdict = GateVerdict.Passed,
            ))
            .withG6(G6IdentityTestReport(
                evaluatedAtMs = System.currentTimeMillis(),
                durationMs = 1_800_000,
                reviewersPassed = 3,
                totalReviewers = 3,
                meanRecognitionMs = 2_800,
                mostCitedSignal = IdentitySignal.WAVE_BREATHING,
                reviewerResults = listOf(
                    ReviewerResultSummary("R1", "A. Khan", "Familiar", 3, 3, 2_100, IdentitySignal.WAVE_BREATHING, true),
                    ReviewerResultSummary("R2", "B. Ahmed", "Casual", 3, 3, 2_900, IdentitySignal.GLASS_DEPTH, true),
                    ReviewerResultSummary("R3", "C. Rahman", "First-time", 2, 3, 3_400, IdentitySignal.COLOR_SATURATION, true),
                ),
                verdict = GateVerdict.Passed,
            ))
            .build()
    }

    private fun realisticMostlyPassReport(): ValidationReport {
        return ValidationReportBuilder(sampleHeader())
            .withG1(G1LibraryRealityReport(
                evaluatedAtMs = System.currentTimeMillis(),
                durationMs = 1_800_000,
                tierResults = listOf(
                    TierResultSummary(LibraryScaleTier.S1, true, 0, emptyList(), 520, 15, 190, 68),
                    TierResultSummary(LibraryScaleTier.S2, true, 0, emptyList(), 780, 16, 330, 125),
                    TierResultSummary(LibraryScaleTier.S3, true, 0, emptyList(), 1_180, 17, 510, 215),
                    TierResultSummary(LibraryScaleTier.S4, false, 2, listOf("Ranking time: 1180ms (budget 1000ms)", "Heap size: 420MB (budget 400MB)"), 1_750, 19, 1_180, 420),
                ),
                verdict = GateVerdict.Failed("S4 tier exceeded budget", listOf("Ranking time", "Heap size")),
            ))
            .withG2(G2MemoryAuditReport(
                evaluatedAtMs = System.currentTimeMillis(),
                durationMs = 2_700_000,
                cyclesCompleted = 7,
                totalLeaksDetected = 3,
                totalRetainedBytes = 1_200_000,
                criticalLeakCount = 0,
                highLeakCount = 0,
                surfaceSummaries = listOf(
                    LeakSurfaceSummaryAggregate(LeakSurface.BITMAP, 2, 800_000, LeakSeverity.MEDIUM),
                    LeakSurfaceSummaryAggregate(LeakSurface.COROUTINE, 1, 400_000, LeakSeverity.LOW),
                ),
                verdict = GateVerdict.Passed,
            ))
            .withG3(G3FrameTimingReport(
                evaluatedAtMs = System.currentTimeMillis(),
                durationMs = 3_600_000,
                perRateEvaluations = RefreshRate.all.map { rate ->
                    val passed = rate != RefreshRate.R_144HZ
                    RateEvaluationSummary(
                        rate = rate,
                        passed = passed,
                        sampleCount = rate.hz * 10,
                        p50Ms = rate.budgetMs * 0.75,
                        p95Ms = rate.budgetMs * 0.95,
                        p99Ms = rate.budgetMs * 1.1,
                        jankRate = if (passed) 0.008 else 0.07,
                        longestGoodStreak = if (passed) rate.hz * 7 else rate.hz * 2,
                        failureSummaries = if (passed) emptyList() else listOf("P95 frame time: 7.4ms (budget 6.94ms)"),
                    )
                },
                verdict = GateVerdict.Failed("144Hz rate exceeded budget", listOf("P95 frame time at 144Hz")),
            ))
            .withG4(G4AnimationContinuityReport(
                evaluatedAtMs = System.currentTimeMillis(),
                durationMs = 5_400_000,
                cyclesCompleted = 100,
                cyclesPassed = 97,
                cyclesFailed = 3,
                cycleDurationMeanMs = 3_350,
                cycleDurationStdDevMs = 240.0,
                memoryGrowthPerCycleBytes = 380.0,
                recompositionGrowthPerCycle = 0.2,
                maxMiniPlayerDriftPx = 1.4f,
                verdict = GateVerdict.Passed,
            ))
            .withG5(G5StressTestReport(
                evaluatedAtMs = System.currentTimeMillis(),
                durationMs = 7_200_000,
                totalActions = 500,
                successCount = 487,
                failureCount = 13,
                crashCount = 0,
                anrCount = 0,
                stateInconsistentCount = 0,
                successRate = 0.974,
                criticalFailuresSummary = emptyList(),
                failuresByCategory = mapOf("User Action" to 8, "Audio Routing" to 5),
                verdict = GateVerdict.Passed,
            ))
            .withG6(G6IdentityTestReport(
                evaluatedAtMs = System.currentTimeMillis(),
                durationMs = 1_800_000,
                reviewersPassed = 3,
                totalReviewers = 3,
                meanRecognitionMs = 3_100,
                mostCitedSignal = IdentitySignal.GLASS_DEPTH,
                reviewerResults = listOf(
                    ReviewerResultSummary("R1", "A. Khan", "Familiar", 3, 3, 2_400, IdentitySignal.GLASS_DEPTH, true),
                    ReviewerResultSummary("R2", "B. Ahmed", "Casual", 2, 3, 3_200, IdentitySignal.WAVE_BREATHING, true),
                    ReviewerResultSummary("R3", "C. Rahman", "First-time", 2, 3, 3_700, IdentitySignal.COLOR_SATURATION, true),
                ),
                verdict = GateVerdict.Passed,
            ))
            .build()
    }

    private fun catastrophicFailureReport(): ValidationReport {
        return ValidationReportBuilder(sampleHeader())
            .withG1(G1LibraryRealityReport(
                evaluatedAtMs = System.currentTimeMillis(),
                durationMs = 1_800_000,
                tierResults = listOf(
                    TierResultSummary(LibraryScaleTier.S1, true, 0, emptyList(), 620, 16, 200, 70),
                    TierResultSummary(LibraryScaleTier.S2, false, 1, listOf("Scroll P95 frame: 22ms (budget 16ms)"), 1_100, 22, 480, 180),
                    TierResultSummary(LibraryScaleTier.S3, false, 3, listOf("Cold start: 1800ms (budget 1200ms)", "Ranking time: 720ms (budget 500ms)", "Heap: 320MB (budget 240MB)"), 1_800, 24, 720, 320),
                    TierResultSummary(LibraryScaleTier.S4, false, 4, listOf("OOM during initial load", "Ranking time: 2400ms (budget 1000ms)", "Heap: 600MB (budget 400MB)", "Jank rate: 12%"), 2_500, 32, 2_400, 600),
                ),
                verdict = GateVerdict.Failed("3 of 4 tiers failed", listOf("S2", "S3", "S4")),
            ))
            .withG2(G2MemoryAuditReport(
                evaluatedAtMs = System.currentTimeMillis(),
                durationMs = 2_700_000,
                cyclesCompleted = 6,
                totalLeaksDetected = 14,
                totalRetainedBytes = 28_000_000,
                criticalLeakCount = 2,
                highLeakCount = 4,
                surfaceSummaries = listOf(
                    LeakSurfaceSummaryAggregate(LeakSurface.BITMAP, 5, 18_000_000, LeakSeverity.CRITICAL),
                    LeakSurfaceSummaryAggregate(LeakSurface.COROUTINE, 3, 4_000_000, LeakSeverity.HIGH),
                    LeakSurfaceSummaryAggregate(LeakSurface.FLOW_COLLECTOR, 2, 2_000_000, LeakSeverity.HIGH),
                    LeakSurfaceSummaryAggregate(LeakSurface.SNAPSHOT, 3, 3_000_000, LeakSeverity.HIGH),
                    LeakSurfaceSummaryAggregate(LeakSurface.NATIVE, 1, 1_000_000, LeakSeverity.MEDIUM),
                ),
                verdict = GateVerdict.Failed("Critical leaks in 2 cycles", listOf("Bitmap leak in C3", "Bitmap leak in C5")),
            ))
            .withG3(G3FrameTimingReport(
                evaluatedAtMs = System.currentTimeMillis(),
                durationMs = 3_600_000,
                perRateEvaluations = RefreshRate.all.map { rate ->
                    val passed = rate == RefreshRate.R_30HZ || rate == RefreshRate.R_60HZ
                    RateEvaluationSummary(
                        rate = rate,
                        passed = passed,
                        sampleCount = rate.hz * 10,
                        p50Ms = rate.budgetMs * 1.2,
                        p95Ms = rate.budgetMs * 1.8,
                        p99Ms = rate.budgetMs * 2.5,
                        jankRate = if (passed) 0.02 else 0.18,
                        longestGoodStreak = if (passed) rate.hz * 5 else rate.hz,
                        failureSummaries = if (passed) emptyList() else listOf("P95: ${rate.budgetMs * 1.8}ms > ${rate.budgetMs}ms", "Jank: 18%"),
                    )
                },
                verdict = GateVerdict.Failed("3 of 5 rates failed", listOf("90Hz", "120Hz", "144Hz")),
            ))
            .withG4(G4AnimationContinuityReport(
                evaluatedAtMs = System.currentTimeMillis(),
                durationMs = 5_400_000,
                cyclesCompleted = 100,
                cyclesPassed = 41,
                cyclesFailed = 59,
                cycleDurationMeanMs = 4_100,
                cycleDurationStdDevMs = 820.0,
                memoryGrowthPerCycleBytes = 12_400.0,
                recompositionGrowthPerCycle = 2.8,
                maxMiniPlayerDriftPx = 18.6f,
                verdict = GateVerdict.Failed("Memory growth trend detected", listOf("+12.4KB/cycle memory growth")),
            ))
            .withG5(G5StressTestReport(
                evaluatedAtMs = System.currentTimeMillis(),
                durationMs = 7_200_000,
                totalActions = 500,
                successCount = 412,
                failureCount = 88,
                crashCount = 4,
                anrCount = 2,
                stateInconsistentCount = 12,
                successRate = 0.824,
                criticalFailuresSummary = listOf(
                    "[87] Navigate to Now Playing → CRASH",
                    "[134] Bluetooth disconnect → ANR",
                    "[201] Process death restore → STATE_INCONSISTENT",
                    "[298] Incoming call → CRASH",
                    "[367] Audio output change → ANR",
                    "[445] Queue reorder → STATE_INCONSISTENT",
                ),
                failuresByCategory = mapOf(
                    "User Action" to 30,
                    "Audio Routing" to 25,
                    "Interruption" to 18,
                    "Lifecycle" to 15,
                ),
                verdict = GateVerdict.Failed("4 crashes, 2 ANRs, 12 state inconsistencies", listOf("crashes", "ANRs")),
            ))
            .withG6(G6IdentityTestReport(
                evaluatedAtMs = System.currentTimeMillis(),
                durationMs = 1_800_000,
                reviewersPassed = 1,
                totalReviewers = 3,
                meanRecognitionMs = 7_400,
                mostCitedSignal = IdentitySignal.HERO_PRESENCE,
                reviewerResults = listOf(
                    ReviewerResultSummary("R1", "A. Khan", "Familiar", 3, 3, 4_100, IdentitySignal.HERO_PRESENCE, true),
                    ReviewerResultSummary("R2", "B. Ahmed", "Casual", 0, 3, 8_200, null, false),
                    ReviewerResultSummary("R3", "C. Rahman", "First-time", 0, 3, 9_900, IdentitySignal.MOTION_LANGUAGE, false),
                ),
                verdict = GateVerdict.Failed("2 of 3 reviewers failed", listOf("R2", "R3")),
            ))
            .build()
    }

    private fun incompleteMissingGatesReport(): ValidationReport {
        return ValidationReportBuilder(sampleHeader())
            .withG1(G1LibraryRealityReport(
                evaluatedAtMs = System.currentTimeMillis(),
                durationMs = 1_800_000,
                tierResults = listOf(
                    TierResultSummary(LibraryScaleTier.S1, true, 0, emptyList(), 510, 15, 190, 68),
                    TierResultSummary(LibraryScaleTier.S2, true, 0, emptyList(), 780, 16, 330, 125),
                ),
                verdict = GateVerdict.Incomplete("Only S1 and S2 tested; S3 and S4 pending"),
            ))
            .withG2(G2MemoryAuditReport(
                evaluatedAtMs = System.currentTimeMillis(),
                durationMs = 900_000,
                cyclesCompleted = 3,
                totalLeaksDetected = 0,
                totalRetainedBytes = 0,
                criticalLeakCount = 0,
                highLeakCount = 0,
                surfaceSummaries = emptyList(),
                verdict = GateVerdict.Incomplete("Only 3 of 5 required cycles completed"),
            ))
            .build()
    }

    private fun g1DeepDiveReport(): ValidationReport {
        return ValidationReportBuilder(sampleHeader())
            .withG1(idealAllPassReport().g1LibraryReality!!)
            .build()
    }

    private fun g2DeepDiveReport(): ValidationReport {
        return ValidationReportBuilder(sampleHeader())
            .withG2(realisticMostlyPassReport().g2MemoryAudit!!)
            .build()
    }

    private fun g3DeepDiveReport(): ValidationReport {
        return ValidationReportBuilder(sampleHeader())
            .withG3(realisticMostlyPassReport().g3FrameTiming!!)
            .build()
    }

    private fun g4DeepDiveReport(): ValidationReport {
        return ValidationReportBuilder(sampleHeader())
            .withG4(idealAllPassReport().g4AnimationContinuity!!)
            .build()
    }

    private fun g5DeepDiveReport(): ValidationReport {
        return ValidationReportBuilder(sampleHeader())
            .withG5(realisticMostlyPassReport().g5StressTest!!)
            .build()
    }

    private fun g6DeepDiveReport(): ValidationReport {
        return ValidationReportBuilder(sampleHeader())
            .withG6(idealAllPassReport().g6IdentityTest!!)
            .build()
    }

    // ─── Documentation helpers ───────────────────────────────────────────

    /**
     * Markdown-style documentation for all preview scenarios.
     * Useful for including in a project README or onboarding doc.
     */
    fun scenarioDocumentation(): String = buildString {
        appendLine("# M2.9 Validation Preview Scenarios")
        appendLine()
        appendLine("Each scenario is a sample [ValidationReport] demonstrating what")
        appendLine("a specific outcome looks like. Use these to understand the report")
        appendLine("shape, test the serializer, or onboard new developers.")
        appendLine()
        allScenarios().forEach { scenario ->
            appendLine("## ${scenario.displayName}")
            appendLine()
            appendLine(scenario.description)
            appendLine()
        }
    }

    /**
     * Brief one-line summary for each gate. Useful for tooltips.
     */
    fun gateDocumentation(): Map<ValidationGateId, String> = mapOf(
        ValidationGateId.LIBRARY_REALITY to
            "Verifies the Library scales from 10K to 100K tracks without OOM or jank.",
        ValidationGateId.MEMORY_AUDIT to
            "Detects leaks in Bitmap, Coroutine, Flow, Snapshot, and Native surfaces across navigation cycles.",
        ValidationGateId.FRAME_TIMING to
            "Verifies frame budget is honored at 30/60/90/120/144 Hz refresh rates.",
        ValidationGateId.ANIMATION_CONTINUITY to
            "Drives 100 morph cycles (Mini Player ↔ Library ↔ Now Playing ↔ Search) to verify continuity.",
        ValidationGateId.STRESS_TEST to
            "Interleaves 500 user/system/lifecycle/audio/interruption actions to verify coherence under chaos.",
        ValidationGateId.IDENTITY_TEST to
            "Cover the RIKO logo — can a reviewer recognize RIKO within 5 seconds? ⭐",
    )
}
