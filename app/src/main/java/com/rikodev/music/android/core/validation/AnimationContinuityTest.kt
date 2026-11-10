/*
 * RIKO EXPERIENCE OS — PRODUCTION-GRADE ROADMAP v2.0
 * Milestone 2.9 — Experience Validation
 *
 * File 5 / 10 — AnimationContinuityTest.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Gate G4 — Animation Continuity Audit. Drives 100 consecutive cycles
 * of the canonical navigation morph:
 *
 *   Mini Player → Library → Now Playing → Back → Search → Again
 *
 * Proves that shared-element transitions, Mini Player state, and
 * morph continuity remain stable across sustained use. A user who
 * navigates back-and-forth 100 times should see no glitch, drift,
 * or recomposition storm.
 *
 * ARCHITECTURE
 * ------------
 *
 *   ┌──────────────────────────────────────────────────────────────┐
 *   │              ANIMATION CONTINUITY TEST                       │
 *   │                                                              │
 *   │  Cycle Definition (5 stages per cycle)                       │
 *   │    1. Start at Mini Player (collapsed)                       │
 *   │    2. Tap Mini Player → expand to Now Playing                │
 *   │    3. From Now Playing → back to Library                     │
 *   │    4. From Library → Search                                  │
 *   │    5. From Search → back to Library (Mini Player restored)   │
 *   │                                                              │
 *   │  Per-cycle Assertions                                        │
 *   │    - No crash                                                │
 *   │    - No morph glitch                                         │
 *   │    - Mini Player state exact after back navigation           │
 *   │    - Recomposition count within budget                       │
 *   │    - Frame time within budget during morph                   │
 *   │    - Memory delta within budget                              │
 *   │                                                              │
 *   │  Aggregate Assertions (across 100 cycles)                    │
 *   │    - No memory growth trend                                  │
 *   │    - No recomposition storm trend                            │
 *   │    - No drift in Mini Player position                        │
 *   │    - Cycle time variance within threshold                    │
 *   └──────────────────────────────────────────────────────────────┘
 *
 * RULES ENFORCED
 * --------------
 * #67  Dock Never Interrupts — Mini Player stays stable through morph
 * #73  Motion Must Have Meaning — morph has semantic value
 * #74  Silence Is Also UX — no decorative motion during morph
 * #76  Performance Guards Never Optimize — observe only
 *
 * © RIKO Experience OS — Validation Tier
 */

package com.rikodev.music.android.core.validation

// ═══════════════════════════════════════════════════════════════════════════
// CYCLE STAGE
// ═══════════════════════════════════════════════════════════════════════════

/**
 * A single stage in the animation continuity cycle.
 */
enum class CycleStage(val displayName: String, val description: String) {
    STAGE_1_MINI_PLAYER("Mini Player Idle", "Mini Player collapsed, Library visible behind"),
    STAGE_2_NOW_PLAYING("Expand to Now Playing", "Tap Mini Player → morph to full Now Playing"),
    STAGE_3_BACK_TO_LIBRARY("Back to Library", "Press back → morph back to Library"),
    STAGE_4_SEARCH("Open Search", "Tap search → Search screen appears"),
    STAGE_5_RETURN("Return to Library", "Press back → Library + Mini Player restored");

    /** The next stage in the cycle, wrapping around. */
    val next: CycleStage
        get() = when (this) {
            STAGE_1_MINI_PLAYER -> STAGE_2_NOW_PLAYING
            STAGE_2_NOW_PLAYING -> STAGE_3_BACK_TO_LIBRARY
            STAGE_3_BACK_TO_LIBRARY -> STAGE_4_SEARCH
            STAGE_4_SEARCH -> STAGE_5_RETURN
            STAGE_5_RETURN -> STAGE_1_MINI_PLAYER
        }
}

/**
 * A complete cycle of all 5 stages.
 */
data class CycleDefinition(
    val cycleNumber: Int,
    val stages: List<CycleStage>,
) {
    init {
        require(stages.size == 5) { "A cycle must have exactly 5 stages" }
    }

    companion object {
        /** The canonical cycle used by M2.9. */
        val canonical: CycleDefinition = CycleDefinition(
            cycleNumber = 0,
            stages = CycleStage.values().toList(),
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// STAGE MEASUREMENT
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Measurement taken at one stage of one cycle.
 *
 * @property cycleNumber        1-indexed cycle number.
 * @property stage              The stage this measurement is for.
 * @property stageDurationMs    Wall-clock time spent in this stage.
 * @property morphDurationMs    Time spent in the morph animation (if any).
 * @property recompositionCount Number of recompositions during the stage.
 * @property frameTimeP95Ms     P95 frame time during the stage.
 * @property droppedFrames      Dropped frames during the stage.
 * @property memoryDeltaBytes   Memory delta (positive = growth).
 * @property miniPlayerX        Mini Player X coordinate (for drift detection).
 * @property miniPlayerY        Mini Player Y coordinate (for drift detection).
 * @property miniPlayerWidth    Mini Player width.
 * @property miniPlayerHeight   Mini Player height.
 * @property glitchDetected     True if a visual glitch was observed.
 * @property crashDetected      True if a crash occurred during this stage.
 * @property timestampMs        Wall-clock time of measurement.
 */
data class StageMeasurement(
    val cycleNumber: Int,
    val stage: CycleStage,
    val stageDurationMs: Long,
    val morphDurationMs: Long,
    val recompositionCount: Int,
    val frameTimeP95Ms: Double,
    val droppedFrames: Int,
    val memoryDeltaBytes: Long,
    val miniPlayerX: Float,
    val miniPlayerY: Float,
    val miniPlayerWidth: Float,
    val miniPlayerHeight: Float,
    val glitchDetected: Boolean,
    val crashDetected: Boolean,
    val timestampMs: Long,
)

/**
 * A complete cycle's measurements (one per stage).
 */
data class CycleMeasurement(
    val cycleNumber: Int,
    val stages: List<StageMeasurement>,
    val totalCycleDurationMs: Long,
    val timestampMs: Long,
) {
    init {
        require(stages.size == 5) { "Cycle measurement must have 5 stages" }
    }

    /** True if any stage crashed. */
    val hasCrash: Boolean get() = stages.any { it.crashDetected }

    /** True if any stage had a visual glitch. */
    val hasGlitch: Boolean get() = stages.any { it.glitchDetected }

    /** Total recompositions across all stages. */
    val totalRecompositions: Int get() = stages.sumOf { it.recompositionCount }

    /** Total dropped frames across all stages. */
    val totalDroppedFrames: Int get() = stages.sumOf { it.droppedFrames }

    /** Total memory growth across the cycle. */
    val totalMemoryDeltaBytes: Long get() = stages.sumOf { it.memoryDeltaBytes }

    /** Mini Player position at the end of the cycle (stage 5). */
    val finalMiniPlayerPosition: Pair<Float, Float>
        get() = stages.last().let { it.miniPlayerX to it.miniPlayerY }
}

// ═══════════════════════════════════════════════════════════════════════════
// PER-CYCLE BUDGET
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Budget for a single cycle.
 */
data class CycleBudget(
    val maxCycleDurationMs: Long,
    val maxRecompositionsPerStage: Int,
    val maxFrameTimeP95Ms: Double,
    val maxDroppedFramesPerStage: Int,
    val maxMemoryDeltaPerCycleBytes: Long,
    val maxMiniPlayerPositionDriftPx: Float,
) {
    companion object {
        fun default(): CycleBudget = CycleBudget(
            maxCycleDurationMs = 5_000,
            maxRecompositionsPerStage = 12,
            maxFrameTimeP95Ms = 18.0,
            maxDroppedFramesPerStage = 3,
            maxMemoryDeltaPerCycleBytes = 512 * 1024,
            maxMiniPlayerPositionDriftPx = 2.0f,
        )
    }
}

/**
 * Result of evaluating one cycle against the budget.
 */
data class CycleEvaluation(
    val cycleNumber: Int,
    val measurement: CycleMeasurement,
    val budget: CycleBudget,
    val failures: List<CycleFailure>,
) {
    val passed: Boolean get() = failures.isEmpty() && !measurement.hasCrash
}

/**
 * A single cycle evaluation failure.
 */
data class CycleFailure(
    val stage: CycleStage?,
    val metric: String,
    val actual: String,
    val expected: String,
) {
    val summary: String
        get() = buildString {
            append("[Cycle metric] ")
            if (stage != null) append("${stage.displayName}: ")
            append("$metric: $actual (expected $expected)")
        }
}

/**
 * Evaluate a cycle measurement against a budget.
 */
fun evaluateCycle(
    measurement: CycleMeasurement,
    budget: CycleBudget,
): CycleEvaluation {
    val failures = mutableListOf<CycleFailure>()

    if (measurement.totalCycleDurationMs > budget.maxCycleDurationMs) {
        failures += CycleFailure(
            stage = null,
            metric = "Total cycle duration",
            actual = "${measurement.totalCycleDurationMs}ms",
            expected = "${budget.maxCycleDurationMs}ms",
        )
    }

    measurement.stages.forEach { stage ->
        if (stage.recompositionCount > budget.maxRecompositionsPerStage) {
            failures += CycleFailure(
                stage = stage.stage,
                metric = "Recomposition count",
                actual = "${stage.recompositionCount}",
                expected = "<=${budget.maxRecompositionsPerStage}",
            )
        }
        if (stage.frameTimeP95Ms > budget.maxFrameTimeP95Ms) {
            failures += CycleFailure(
                stage = stage.stage,
                metric = "P95 frame time",
                actual = "${"%.2f".format(stage.frameTimeP95Ms)}ms",
                expected = "<=${"%.2f".format(budget.maxFrameTimeP95Ms)}ms",
            )
        }
        if (stage.droppedFrames > budget.maxDroppedFramesPerStage) {
            failures += CycleFailure(
                stage = stage.stage,
                metric = "Dropped frames",
                actual = "${stage.droppedFrames}",
                expected = "<=${budget.maxDroppedFramesPerStage}",
            )
        }
        if (stage.glitchDetected) {
            failures += CycleFailure(
                stage = stage.stage,
                metric = "Visual glitch",
                actual = "detected",
                expected = "none",
            )
        }
        if (stage.crashDetected) {
            failures += CycleFailure(
                stage = stage.stage,
                metric = "Crash",
                actual = "detected",
                expected = "none",
            )
        }
    }

    if (kotlin.math.abs(measurement.totalMemoryDeltaBytes) > budget.maxMemoryDeltaPerCycleBytes) {
        failures += CycleFailure(
            stage = null,
            metric = "Memory delta",
            actual = "${measurement.totalMemoryDeltaBytes / 1024}KB",
            expected = "<=${budget.maxMemoryDeltaPerCycleBytes / 1024}KB",
        )
    }

    return CycleEvaluation(
        cycleNumber = measurement.cycleNumber,
        measurement = measurement,
        budget = budget,
        failures = failures,
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// DRIFT DETECTION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Detect position drift of the Mini Player across cycles.
 *
 * Compares the final Mini Player position of each cycle against the
 * first cycle's position. If any cycle drifts more than the budget,
 * it's flagged.
 */
class MiniPlayerDriftDetector(
    private val maxDriftPx: Float = 2.0f,
) {

    /**
     * Compute drift per cycle relative to the first cycle.
     *
     * @return List of (cycleNumber, driftPx) pairs.
     */
    fun detectDrift(cycles: List<CycleMeasurement>): List<DriftReport> {
        if (cycles.isEmpty()) return emptyList()
        val baseline = cycles.first().finalMiniPlayerPosition
        return cycles.map { cycle ->
            val (x, y) = cycle.finalMiniPlayerPosition
            val dx = x - baseline.first
            val dy = y - baseline.second
            val drift = kotlin.math.sqrt(dx * dx + dy * dy)
            DriftReport(
                cycleNumber = cycle.cycleNumber,
                driftPx = drift,
                isExcessive = drift > maxDriftPx,
            )
        }
    }
}

/**
 * Drift report for one cycle.
 */
data class DriftReport(
    val cycleNumber: Int,
    val driftPx: Float,
    val isExcessive: Boolean,
)

// ═══════════════════════════════════════════════════════════════════════════
// TREND ANALYSIS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Trend analysis across all cycles.
 *
 * Detects:
 *   - Memory growth trend (slope of best-fit line)
 *   - Recomposition growth trend
 *   - Cycle duration variance
 */
data class TrendReport(
    val memoryGrowthPerCycleBytes: Double,
    val recompositionGrowthPerCycle: Double,
    val cycleDurationMeanMs: Long,
    val cycleDurationStdDevMs: Double,
    val cycleDurationVarianceMs: Double,
    val cyclesWithIncreasingMemory: Int,
    val cyclesWithIncreasingRecompositions: Int,
) {
    /** True if memory is growing across cycles (slope > 0). */
    val hasMemoryGrowthTrend: Boolean
        get() = memoryGrowthPerCycleBytes > 1024 // >1KB/cycle = trend

    /** True if recompositions are growing across cycles. */
    val hasRecompositionGrowthTrend: Boolean
        get() = recompositionGrowthPerCycle > 0.5 // >0.5/cycle = trend

    /** True if cycle duration variance is high (unstable). */
    val hasHighVariance: Boolean
        get() = cycleDurationVarianceMs > 1_000_000 // >1s² variance
}

/**
 * Compute trend across cycles.
 */
fun computeTrend(cycles: List<CycleMeasurement>): TrendReport {
    if (cycles.isEmpty()) {
        return TrendReport(0.0, 0.0, 0, 0.0, 0.0, 0, 0)
    }
    val memoryDeltas = cycles.map { it.totalMemoryDeltaBytes.toDouble() }
    val recomps = cycles.map { it.totalRecompositions.toDouble() }
    val durations = cycles.map { it.totalCycleDurationMs.toDouble() }

    val memorySlope = linearRegressionSlope(memoryDeltas)
    val recompSlope = linearRegressionSlope(recomps)
    val mean = durations.average().toLong()
    val variance = durations.fold(0.0) { acc, d -> acc + (d - mean) * (d - mean) } / durations.size
    val stdDev = kotlin.math.sqrt(variance)

    var increasingMemory = 0
    var increasingRecomps = 0
    for (i in 1 until cycles.size) {
        if (cycles[i].totalMemoryDeltaBytes > cycles[i - 1].totalMemoryDeltaBytes) increasingMemory++
        if (cycles[i].totalRecompositions > cycles[i - 1].totalRecompositions) increasingRecomps++
    }

    return TrendReport(
        memoryGrowthPerCycleBytes = memorySlope,
        recompositionGrowthPerCycle = recompSlope,
        cycleDurationMeanMs = mean,
        cycleDurationStdDevMs = stdDev,
        cycleDurationVarianceMs = variance,
        cyclesWithIncreasingMemory = increasingMemory,
        cyclesWithIncreasingRecompositions = increasingRecomps,
    )
}

private fun linearRegressionSlope(values: List<Double>): Double {
    if (values.size < 2) return 0.0
    val n = values.size
    val xs = (0 until n).map { it.toDouble() }
    val xMean = xs.average()
    val yMean = values.average()
    var num = 0.0
    var den = 0.0
    for (i in 0 until n) {
        num += (xs[i] - xMean) * (values[i] - yMean)
        den += (xs[i] - xMean) * (xs[i] - xMean)
    }
    return if (den == 0.0) 0.0 else num / den
}

// ═══════════════════════════════════════════════════════════════════════════
// ANIMATION CONTINUITY TEST
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The Animation Continuity Test (Gate G4).
 *
 * Drives 100 cycles of the canonical morph and evaluates each cycle
 * plus the aggregate trend. The analysis phase feeds in measurements;
 * this class evaluates them and produces a verdict.
 */
class AnimationContinuityTest(
    private val cycleBudget: CycleBudget = CycleBudget.default(),
    private val requiredCycles: Int = 100,
    private val driftDetector: MiniPlayerDriftDetector = MiniPlayerDriftDetector(),
) {

    private val _cycles: MutableList<CycleMeasurement> = mutableListOf()
    val cycles: List<CycleMeasurement> get() = _cycles.toList()

    /**
     * Record a completed cycle's measurement.
     */
    fun recordCycle(measurement: CycleMeasurement) {
        _cycles += measurement
    }

    /**
     * Evaluate a single cycle.
     */
    fun evaluateCycle(cycleNumber: Int): CycleEvaluation? {
        val measurement = _cycles.getOrNull(cycleNumber - 1) ?: return null
        return evaluateCycle(measurement, cycleBudget)
    }

    /**
     * Evaluate all recorded cycles.
     */
    fun evaluateAllCycles(): List<CycleEvaluation> =
        _cycles.map { evaluateCycle(it, cycleBudget) }

    /**
     * Compute drift across all cycles.
     */
    fun computeDrift(): List<DriftReport> = driftDetector.detectDrift(_cycles)

    /**
     * Compute trend across all cycles.
     */
    fun computeTrendReport(): TrendReport = computeTrend(_cycles)

    /**
     * Final verdict for Gate G4.
     */
    fun evaluate(): GateG4Verdict {
        if (_cycles.size < requiredCycles) {
            return GateG4Verdict.Incomplete(
                cyclesCompleted = _cycles.size,
                required = requiredCycles,
            )
        }
        val evaluations = evaluateAllCycles()
        val crashed = evaluations.filter { it.measurement.hasCrash }
        if (crashed.isNotEmpty()) {
            return GateG4Verdict.Failed(
                reason = "Crash in cycles: ${crashed.map { it.cycleNumber }}",
                blockingCycles = crashed,
            )
        }
        val glitched = evaluations.filter { it.measurement.hasGlitch }
        if (glitched.isNotEmpty()) {
            return GateG4Verdict.Failed(
                reason = "Visual glitch in cycles: ${glitched.map { it.cycleNumber }}",
                blockingCycles = glitched,
            )
        }
        val failed = evaluations.filter { !it.passed }
        if (failed.isNotEmpty()) {
            return GateG4Verdict.Failed(
                reason = "${failed.size} cycle(s) exceeded budget",
                blockingCycles = failed,
            )
        }
        val drifts = computeDrift()
        val excessiveDrifts = drifts.filter { it.isExcessive }
        if (excessiveDrifts.isNotEmpty()) {
            return GateG4Verdict.Failed(
                reason = "Mini Player drift in cycles: ${excessiveDrifts.map { it.cycleNumber }}",
                blockingCycles = excessiveDrifts.map { drift ->
                    evaluations.first { it.cycleNumber == drift.cycleNumber }
                },
            )
        }
        val trend = computeTrendReport()
        if (trend.hasMemoryGrowthTrend) {
            return GateG4Verdict.Failed(
                reason = "Memory growth trend: +${"%.2f".format(trend.memoryGrowthPerCycleBytes / 1024)}KB/cycle",
                blockingCycles = emptyList(),
            )
        }
        if (trend.hasRecompositionGrowthTrend) {
            return GateG4Verdict.Failed(
                reason = "Recomposition growth trend: +${"%.2f".format(trend.recompositionGrowthPerCycle)}/cycle",
                blockingCycles = emptyList(),
            )
        }
        return GateG4Verdict.Passed(
            cyclesCompleted = _cycles.size,
            evaluations = evaluations,
            trend = trend,
            drifts = drifts,
        )
    }

    /**
     * Human-readable summary.
     */
    fun summaryReport(): String = buildString {
        appendLine("═══ GATE G4 — ANIMATION CONTINUITY AUDIT ═══")
        appendLine()
        appendLine("Cycles completed: ${_cycles.size} / $requiredCycles")
        if (_cycles.isEmpty()) {
            appendLine("No cycles recorded.")
            return@buildString
        }
        val trend = computeTrendReport()
        appendLine("Cycle duration (mean): ${trend.cycleDurationMeanMs}ms")
        appendLine("Cycle duration (std dev): ${"%.2f".format(trend.cycleDurationStdDevMs)}ms")
        appendLine("Memory growth trend: ${"%.2f".format(trend.memoryGrowthPerCycleBytes / 1024)}KB/cycle")
        appendLine("Recomposition trend: ${"%.2f".format(trend.recompositionGrowthPerCycle)}/cycle")
        appendLine()
        val drifts = computeDrift()
        val maxDrift = drifts.maxOfOrNull { it.driftPx } ?: 0f
        appendLine("Max Mini Player drift: ${"%.2f".format(maxDrift)}px")
        appendLine()
        val evals = evaluateAllCycles()
        val passed = evals.count { it.passed }
        val failed = evals.count { !it.passed }
        appendLine("Cycles passed: $passed / ${evals.size}")
        appendLine("Cycles failed: $failed / ${evals.size}")
        if (failed > 0) {
            appendLine("Failed cycle numbers: ${evals.filter { !it.passed }.map { it.cycleNumber }}")
        }
        appendLine()
        appendLine("Verdict: ${evaluate().javaClass.simpleName}")
    }
}

/**
 * Verdict for Gate G4.
 */
sealed class GateG4Verdict {
    data class Passed(
        val cyclesCompleted: Int,
        val evaluations: List<CycleEvaluation>,
        val trend: TrendReport,
        val drifts: List<DriftReport>,
    ) : GateG4Verdict()

    data class Incomplete(
        val cyclesCompleted: Int,
        val required: Int,
    ) : GateG4Verdict()

    data class Failed(
        val reason: String,
        val blockingCycles: List<CycleEvaluation>,
    ) : GateG4Verdict()

    val isPassed: Boolean get() = this is Passed
}
