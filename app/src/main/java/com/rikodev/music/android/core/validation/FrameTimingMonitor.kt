/*
 * RIKO EXPERIENCE OS — PRODUCTION-GRADE ROADMAP v2.0
 * Milestone 2.9 — Experience Validation
 *
 * File 4 / 10 — FrameTimingMonitor.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Gate G3 — Frame Timing Audit. Records per-frame durations across
 * the five refresh rates RIKO must support:
 *
 *   30 Hz    — Budget 33.33ms  (low-power mode, battery saver)
 *   60 Hz    — Budget 16.67ms  (standard)
 *   90 Hz    — Budget 11.11ms  (Pixel 7+, mid-tier high-refresh)
 *   120 Hz   — Budget  8.33ms  (flagship high-refresh)
 *   144 Hz   — Budget  6.94ms  (gaming-tier, foldable inner display)
 *
 * This monitor is OBSERVATION-ONLY. It records frame times, computes
 * statistics, and reports them. It never throttles frame rate, never
 * disables animations, never reduces glass blur. (Rule #76.)
 *
 * ARCHITECTURE
 * ------------
 *
 *   ┌──────────────────────────────────────────────────────────────┐
 *   │                  FRAME TIMING MONITOR                        │
 *   │                                                              │
 *   │  Frame Sample Source ──┐                                     │
 *   │  (Choreographer /      │                                     │
 *   │   OnFrameListener)     │                                     │
 *   │                        ▼                                     │
 *   │                Frame Timing Buffer                          │
 *   │                        │                                     │
 *   │                        ▼                                     │
 *   │              Per-Rate Calculator                           │
 *   │              (P50/P95/P99/jank rate)                       │
 *   │                        │                                     │
 *   │                        ▼                                     │
 *   │              Budget Evaluator                               │
 *   │              (pass/fail per rate)                           │
 *   └──────────────────────────────────────────────────────────────┘
 *
 * STATISTICS PER REFRESH RATE
 * ---------------------------
 *   - P50 (median) frame time
 *   - P95 frame time (represents sustained load)
 *   - P99 frame time (represents worst-case spikes)
 *   - Jank rate (% of frames exceeding budget)
 *   - Missed-frame count
 *   - Longest sustained good streak
 *
 * RULES ENFORCED
 * --------------
 * #63  Background Never Demands Attention — frame time at idle <budget
 * #64  Motion Must Be Discoverable — animation frame time <budget
 * #74  Silence Is Also UX — idle frame count minimal
 * #76  Performance Guards Never Optimize — report only
 *
 * © RIKO Experience OS — Validation Tier
 */

package com.rikodev.music.android.core.validation

// ═══════════════════════════════════════════════════════════════════════════
// REFRESH RATE
// ═══════════════════════════════════════════════════════════════════════════

/**
 * A supported refresh rate.
 *
 * @property hz              Refresh rate in Hz.
 * @property budgetMs        Frame time budget in ms (1000 / hz).
 * @property toleranceMs     Acceptable over-budget tolerance before
 *                           a frame counts as "jank". Default = 0.
 * @property displayName     Human-readable name.
 */
enum class RefreshRate(
    val hz: Int,
    val budgetMs: Double,
    val toleranceMs: Double,
    val displayName: String,
) {
    R_30HZ(30, 33.33, 0.5, "30Hz Low-Power"),
    R_60HZ(60, 16.67, 0.5, "60Hz Standard"),
    R_90HZ(90, 11.11, 0.4, "90Hz Mid-High"),
    R_120HZ(120, 8.33, 0.3, "120Hz Flagship"),
    R_144HZ(144, 6.94, 0.25, "144Hz Gaming");

    init {
        require(hz > 0) { "Hz must be positive" }
        require(budgetMs > 0) { "Budget must be positive" }
        require(toleranceMs >= 0) { "Tolerance cannot be negative" }
    }

    /** The threshold above which a frame is considered jank. */
    val jankThresholdMs: Double get() = budgetMs + toleranceMs

    companion object {
        /** All supported rates in ascending order. */
        val all: List<RefreshRate> = values().toList()

        /**
         * Detect the closest supported rate for an actual measured Hz.
         * Useful for converting device-reported rates to our enum.
         */
        fun closest(measuredHz: Float): RefreshRate {
            return all.minByOrNull { kotlin.math.abs(it.hz - measuredHz) }
                ?: R_60HZ
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// FRAME SAMPLE
// ═══════════════════════════════════════════════════════════════════════════

/**
 * A single frame timing sample.
 *
 * @property frameId            Monotonic frame counter.
 * @property rate                The refresh rate this frame was sampled at.
 * @property frameTimeMs         Total frame time (start to vsync).
 * @property uiThreadTimeMs      Time spent on UI thread.
 * @property renderThreadTimeMs  Time spent on RenderThread (if any).
 * @property compositionCount    Number of Compose compositions this frame.
 * @property isJank              Whether this frame exceeded the jank threshold.
 * @property timestampMs         Wall-clock time of the frame.
 * @property label               Optional label for the active screen or interaction.
 */
data class FrameSample(
    val frameId: Long,
    val rate: RefreshRate,
    val frameTimeMs: Double,
    val uiThreadTimeMs: Double,
    val renderThreadTimeMs: Double,
    val compositionCount: Int,
    val isJank: Boolean,
    val timestampMs: Long,
    val label: String?,
) {
    init {
        require(frameTimeMs >= 0) { "Frame time cannot be negative" }
        require(uiThreadTimeMs >= 0) { "UI thread time cannot be negative" }
        require(renderThreadTimeMs >= 0) { "Render thread time cannot be negative" }
        require(compositionCount >= 0) { "Composition count cannot be negative" }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// FRAME BUFFER
// ═══════════════════════════════════════════════════════════════════════════

/**
 * A rolling buffer of frame samples for a single refresh rate.
 *
 * Sized to hold one second of frames at the rate's max frequency,
 * multiplied by a windowing factor (default 10 seconds).
 *
 * Thread-safe for single-producer, single-consumer access.
 */
class FrameBuffer(
    val rate: RefreshRate,
    private val windowSeconds: Int = 10,
) {

    private val capacity: Int = rate.hz * windowSeconds
    private val samples: ArrayDeque<FrameSample> = ArrayDeque(capacity)

    /**
     * Add a sample. Evicts the oldest if at capacity.
     */
    @Synchronized
    fun add(sample: FrameSample) {
        require(sample.rate == rate) {
            "Sample rate ${sample.rate} does not match buffer rate $rate"
        }
        if (samples.size >= capacity) {
            samples.removeFirst()
        }
        samples.addLast(sample)
    }

    /**
     * Take a snapshot of current samples.
     */
    @Synchronized
    fun snapshot(): List<FrameSample> = samples.toList()

    /**
     * Current sample count.
     */
    @Synchronized
    fun size(): Int = samples.size

    /**
     * Clear all samples.
     */
    @Synchronized
    fun clear() {
        samples.clear()
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// PERCENTILE CALCULATOR
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Statistics computed from a frame sample set.
 */
data class FrameStatistics(
    val rate: RefreshRate,
    val sampleCount: Int,
    val p50Ms: Double,
    val p95Ms: Double,
    val p99Ms: Double,
    val maxMs: Double,
    val minMs: Double,
    val meanMs: Double,
    val jankRate: Double,
    val jankFrameCount: Int,
    val longestGoodStreak: Int,
    val totalCompositionCount: Int,
    val averageCompositionCount: Double,
) {
    init {
        require(sampleCount >= 0) { "Sample count cannot be negative" }
        require(jankRate in 0.0..1.0) { "Jank rate must be 0.0–1.0" }
    }

    /**
     * The frame-time budget for this rate.
     */
    val budgetMs: Double get() = rate.budgetMs

    /**
     * True if P95 frame time is within budget.
     */
    val isP95WithinBudget: Boolean get() = p95Ms <= rate.jankThresholdMs

    /**
     * True if jank rate is within acceptable threshold (default 5%).
     */
    val isJankRateAcceptable: Boolean get() = jankRate <= 0.05
}

/**
 * Compute statistics from a list of frame samples.
 *
 * Returns null if the sample list is empty.
 */
fun computeStatistics(samples: List<FrameSample>): FrameStatistics? {
    if (samples.isEmpty()) return null
    val rate = samples.first().rate
    require(samples.all { it.rate == rate }) {
        "All samples must be from the same refresh rate"
    }
    val sorted = samples.map { it.frameTimeMs }.sorted()
    val n = sorted.size
    val p50 = percentile(sorted, 0.50)
    val p95 = percentile(sorted, 0.95)
    val p99 = percentile(sorted, 0.99)
    val max = sorted.last()
    val min = sorted.first()
    val mean = sorted.sum() / n
    val jankCount = samples.count { it.isJank }
    val jankRate = jankCount.toDouble() / n
    val longestGood = computeLongestGoodStreak(samples)
    val totalComp = samples.sumOf { it.compositionCount }
    val avgComp = totalComp.toDouble() / n
    return FrameStatistics(
        rate = rate,
        sampleCount = n,
        p50Ms = p50,
        p95Ms = p95,
        p99Ms = p99,
        maxMs = max,
        minMs = min,
        meanMs = mean,
        jankRate = jankRate,
        jankFrameCount = jankCount,
        longestGoodStreak = longestGood,
        totalCompositionCount = totalComp,
        averageCompositionCount = avgComp,
    )
}

private fun percentile(sortedAsc: List<Double>, p: Double): Double {
    require(p in 0.0..1.0) { "Percentile must be 0.0–1.0" }
    if (sortedAsc.isEmpty()) return 0.0
    if (sortedAsc.size == 1) return sortedAsc.first()
    val idx = ((sortedAsc.size - 1) * p).toInt().coerceIn(0, sortedAsc.size - 1)
    return sortedAsc[idx]
}

private fun computeLongestGoodStreak(samples: List<FrameSample>): Int {
    var longest = 0
    var current = 0
    for (sample in samples) {
        if (!sample.isJank) {
            current++
            if (current > longest) longest = current
        } else {
            current = 0
        }
    }
    return longest
}

// ═══════════════════════════════════════════════════════════════════════════
// BUDGET EVALUATION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Per-rate budget for the Frame Timing gate.
 */
data class FrameTimingBudget(
    val rate: RefreshRate,
    val maxP95Ms: Double,
    val maxJankRate: Double,
    val minSamples: Int,
    val minLongestGoodStreak: Int,
) {
    init {
        require(maxP95Ms > 0) { "Max P95 must be positive" }
        require(maxJankRate in 0.0..1.0) { "Max jank rate must be 0.0–1.0" }
        require(minSamples > 0) { "Min samples must be positive" }
        require(minLongestGoodStreak > 0) { "Min streak must be positive" }
    }

    companion object {
        /**
         * Default budgets. Tuned for "feels premium" experience.
         * Analysis phase may override.
         */
        fun defaults(): Map<RefreshRate, FrameTimingBudget> = mapOf(
            RefreshRate.R_30HZ to FrameTimingBudget(
                rate = RefreshRate.R_30HZ,
                maxP95Ms = 33.33,
                maxJankRate = 0.01,
                minSamples = 300,
                minLongestGoodStreak = 100,
            ),
            RefreshRate.R_60HZ to FrameTimingBudget(
                rate = RefreshRate.R_60HZ,
                maxP95Ms = 16.67,
                maxJankRate = 0.01,
                minSamples = 600,
                minLongestGoodStreak = 200,
            ),
            RefreshRate.R_90HZ to FrameTimingBudget(
                rate = RefreshRate.R_90HZ,
                maxP95Ms = 11.11,
                maxJankRate = 0.02,
                minSamples = 900,
                minLongestGoodStreak = 300,
            ),
            RefreshRate.R_120HZ to FrameTimingBudget(
                rate = RefreshRate.R_120HZ,
                maxP95Ms = 8.33,
                maxJankRate = 0.03,
                minSamples = 1200,
                minLongestGoodStreak = 400,
            ),
            RefreshRate.R_144HZ to FrameTimingBudget(
                rate = RefreshRate.R_144HZ,
                maxP95Ms = 6.94,
                maxJankRate = 0.05,
                minSamples = 1440,
                minLongestGoodStreak = 500,
            ),
        )
    }
}

/**
 * Result of evaluating statistics against a budget.
 */
data class FrameTimingEvaluation(
    val rate: RefreshRate,
    val statistics: FrameStatistics,
    val budget: FrameTimingBudget,
    val failures: List<FrameTimingFailure>,
) {
    val passed: Boolean get() = failures.isEmpty()
}

/**
 * A single budget failure.
 */
data class FrameTimingFailure(
    val metric: String,
    val actual: Double,
    val expected: Double,
    val unit: String,
) {
    val summary: String
        get() = "$metric: $actual $unit (budget $expected $unit)"
}

/**
 * Evaluate statistics against a budget.
 */
fun evaluateAgainst(
    statistics: FrameStatistics,
    budget: FrameTimingBudget,
): FrameTimingEvaluation {
    require(statistics.rate == budget.rate) {
        "Rate mismatch: ${statistics.rate} vs ${budget.rate}"
    }
    val failures = mutableListOf<FrameTimingFailure>()
    if (statistics.p95Ms > budget.maxP95Ms) {
        failures += FrameTimingFailure("P95 frame time", statistics.p95Ms, budget.maxP95Ms, "ms")
    }
    if (statistics.jankRate > budget.maxJankRate) {
        failures += FrameTimingFailure("Jank rate", statistics.jankRate * 100, budget.maxJankRate * 100, "%")
    }
    if (statistics.sampleCount < budget.minSamples) {
        failures += FrameTimingFailure("Sample count", statistics.sampleCount.toDouble(), budget.minSamples.toDouble(), "frames")
    }
    if (statistics.longestGoodStreak < budget.minLongestGoodStreak) {
        failures += FrameTimingFailure("Longest good streak", statistics.longestGoodStreak.toDouble(), budget.minLongestGoodStreak.toDouble(), "frames")
    }
    return FrameTimingEvaluation(
        rate = statistics.rate,
        statistics = statistics,
        budget = budget,
        failures = failures,
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// FRAME TIMING MONITOR
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The Frame Timing Monitor (Gate G3).
 *
 * Holds per-rate buffers and provides a clean API for the analysis
 * phase to feed in samples and read out evaluations.
 *
 * Usage pattern:
 *   1. [selectRate] when the device switches refresh rate
 *   2. [recordFrame] for each frame (driven by Choreographer)
 *   3. [evaluate] at the end of the audit to get per-rate verdicts
 */
class FrameTimingMonitor(
    private var budgets: Map<RefreshRate, FrameTimingBudget> = FrameTimingBudget.defaults(),
) {

    private val buffers: MutableMap<RefreshRate, FrameBuffer> =
        RefreshRate.all.associateWithTo(mutableMapOf()) { FrameBuffer(it) }

    private var nextFrameId: Long = 1L
    private var currentRate: RefreshRate = RefreshRate.R_60HZ
    private var currentLabel: String? = null

    // ─── Rate selection ───────────────────────────────────────────────────

    /**
     * Select the active refresh rate. Subsequent [recordFrame] calls
     * will be attributed to this rate.
     */
    fun selectRate(rate: RefreshRate) {
        currentRate = rate
    }

    /**
     * Auto-select the closest rate for a measured Hz value.
     */
    fun autoSelectRate(measuredHz: Float) {
        currentRate = RefreshRate.closest(measuredHz)
    }

    // ─── Labeling ─────────────────────────────────────────────────────────

    /**
     * Set a label for subsequent frames. Use to attribute frame times
     * to a screen or interaction (e.g. "Library-scroll", "Wave-idle").
     */
    fun setLabel(label: String?) {
        currentLabel = label
    }

    // ─── Recording ────────────────────────────────────────────────────────

    /**
     * Record a single frame.
     *
     * @param frameTimeMs         Total frame time.
     * @param uiThreadTimeMs      UI thread time (default = frame time).
     * @param renderThreadTimeMs  Render thread time (default = 0).
     * @param compositionCount    Compositions this frame (default = 0).
     */
    fun recordFrame(
        frameTimeMs: Double,
        uiThreadTimeMs: Double = frameTimeMs,
        renderThreadTimeMs: Double = 0.0,
        compositionCount: Int = 0,
    ) {
        val rate = currentRate
        val isJank = frameTimeMs > rate.jankThresholdMs
        val sample = FrameSample(
            frameId = nextFrameId++,
            rate = rate,
            frameTimeMs = frameTimeMs,
            uiThreadTimeMs = uiThreadTimeMs,
            renderThreadTimeMs = renderThreadTimeMs,
            compositionCount = compositionCount,
            isJank = isJank,
            timestampMs = System.currentTimeMillis(),
            label = currentLabel,
        )
        buffers.getOrPut(rate) { FrameBuffer(rate) }.add(sample)
    }

    /**
     * Record many frames at once (batch ingest from a log file).
     */
    fun recordBatch(samples: List<FrameSample>) {
        samples.forEach { sample ->
            buffers[sample.rate]!!.add(sample)
        }
    }

    // ─── Queries ──────────────────────────────────────────────────────────

    fun samplesFor(rate: RefreshRate): List<FrameSample> =
        buffers[rate]?.snapshot() ?: emptyList()

    fun sampleCount(rate: RefreshRate): Int =
        buffers[rate]?.size() ?: 0

    fun currentRate(): RefreshRate = currentRate

    // ─── Budget overrides ─────────────────────────────────────────────────

    fun overrideBudget(rate: RefreshRate, budget: FrameTimingBudget) {
        require(budget.rate == rate) { "Budget rate mismatch" }
        budgets = budgets + (rate to budget)
    }

    // ─── Evaluation ───────────────────────────────────────────────────────

    /**
     * Evaluate a single rate.
     */
    fun evaluateRate(rate: RefreshRate): FrameTimingEvaluation? {
        val samples = samplesFor(rate)
        val stats = computeStatistics(samples) ?: return null
        val budget = budgets[rate] ?: error("No budget for $rate")
        return evaluateAgainst(stats, budget)
    }

    /**
     * Evaluate all rates. Returns a per-rate map of evaluations.
     */
    fun evaluateAll(): Map<RefreshRate, FrameTimingEvaluation> {
        return RefreshRate.all.mapNotNull { rate ->
            evaluateRate(rate)?.let { rate to it }
        }.toMap()
    }

    /**
     * Final verdict for Gate G3.
     */
    fun evaluate(): GateG3Verdict {
        val evaluations = evaluateAll()
        if (evaluations.isEmpty()) {
            return GateG3Verdict.Incomplete(evaluatedRates = 0, required = 5)
        }
        val failed = evaluations.filter { !it.value.passed }
        return if (failed.isEmpty()) {
            GateG3Verdict.Passed(
                ratesPassed = evaluations.size,
                evaluations = evaluations,
            )
        } else {
            GateG3Verdict.Failed(
                failedRates = failed.keys.toList(),
                failedEvaluations = failed,
            )
        }
    }

    /**
     * Human-readable summary.
     */
    fun summaryReport(): String = buildString {
        appendLine("═══ GATE G3 — FRAME TIMING AUDIT ═══")
        appendLine()
        val all = evaluateAll()
        if (all.isEmpty()) {
            appendLine("No samples recorded.")
            return@buildString
        }
        all.forEach { (rate, evaluation) ->
            val stats = evaluation.statistics
            appendLine("[${rate.displayName}]")
            appendLine("    samples: ${stats.sampleCount}")
            appendLine("    P50:     ${"%.2f".format(stats.p50Ms)}ms (budget ${"%.2f".format(rate.budgetMs)}ms)")
            appendLine("    P95:     ${"%.2f".format(stats.p95Ms)}ms")
            appendLine("    P99:     ${"%.2f".format(stats.p99Ms)}ms")
            appendLine("    max:     ${"%.2f".format(stats.maxMs)}ms")
            appendLine("    jank:    ${"%.2f".format(stats.jankRate * 100)}% (${stats.jankFrameCount} frames)")
            appendLine("    streak:  ${stats.longestGoodStreak} consecutive good frames")
            appendLine("    verdict: ${if (evaluation.passed) "PASS" else "FAIL"}")
            if (!evaluation.passed) {
                evaluation.failures.forEach { failure ->
                    appendLine("      ✗ ${failure.summary}")
                }
            }
        }
        appendLine()
        appendLine("Verdict: ${evaluate().javaClass.simpleName}")
    }
}

/**
 * Verdict for Gate G3.
 */
sealed class GateG3Verdict {
    data class Passed(
        val ratesPassed: Int,
        val evaluations: Map<RefreshRate, FrameTimingEvaluation>,
    ) : GateG3Verdict()

    data class Incomplete(
        val evaluatedRates: Int,
        val required: Int,
    ) : GateG3Verdict()

    data class Failed(
        val failedRates: List<RefreshRate>,
        val failedEvaluations: Map<RefreshRate, FrameTimingEvaluation>,
    ) : GateG3Verdict() {
        init {
            require(failedRates.isNotEmpty()) { "Failed verdict must list failed rates" }
        }
    }

    val isPassed: Boolean get() = this is Passed
}
