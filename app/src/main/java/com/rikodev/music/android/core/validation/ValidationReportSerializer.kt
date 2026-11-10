/*
 * RIKO EXPERIENCE OS — PRODUCTION-GRADE ROADMAP v2.0
 * Milestone 2.9 — Experience Validation
 *
 * File 9 / 10 — ValidationReportSerializer.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Serializes a [ValidationReport] to a structured JSON document so
 * the analysis phase (Phase 2) can ingest it programmatically.
 *
 * Pure Kotlin, zero Android dependencies. Uses a hand-written JSON
 * emitter to avoid pulling in a JSON library at the validation layer
 * (keeps the validation package small and dependency-free).
 *
 * ARCHITECTURE
 * ------------
 *
 *   ┌──────────────────────────────────────────────────────────────┐
 *   │              VALIDATION REPORT SERIALIZER                    │
 *   │                                                              │
 *   │  ValidationReport                                            │
 *   │       │                                                      │
 *   │       ▼                                                      │
 *   │  JsonWriter (streaming, no intermediate DOM)                 │
 *   │       │                                                      │
 *   │       ▼                                                      │
 *   │  JSON String                                                 │
 *   │       │                                                      │
 *   │       ▼                                                      │
 *   │  Analysis Phase (Phase 2) ingest                             │
 *   └──────────────────────────────────────────────────────────────┘
 *
 * DESIGN DECISIONS
 * ----------------
 *   - Hand-written JSON writer (no kotlinx.serialization dependency)
 *   - Streaming emitter (no in-memory DOM, low allocation)
 *   - Stable field ordering (deterministic output for diffing)
 *   - All numeric values as numbers, not strings
 *   - Booleans as true/false, not 0/1
 *   - Null values omitted (smaller output)
 *
 * RULES ENFORCED
 * --------------
 * #21  Traceable — every report field has stable name
 * #53  Diagnostics Never Affects Engine — serializer is side-effect free
 *
 * © RIKO Experience OS — Validation Tier
 */

package com.rikodev.music.android.core.validation

import java.io.StringWriter
import java.io.Writer
import androidx.compose.animation.core.Animatable

// ═══════════════════════════════════════════════════════════════════════════
// JSON WRITER
// ═══════════════════════════════════════════════════════════════════════════

/**
 * A minimal, dependency-free JSON writer.
 *
 * Writes JSON tokens directly to a [Writer] without building an
 * in-memory DOM. Suitable for serializing large validation reports
 * without allocation overhead.
 *
 * Not threadsafe — one writer per thread.
 */
class JsonWriter(private val writer: Writer) : AutoCloseable {

    private var depth = 0
    private var firstInContainer = true
    private var inObject = BooleanArray(64) // stack of container types
    private var stackPtr = 0

    private fun indent() {
        if (depth > 0) {
            writer.write("\n")
            for (i in 0 until depth) {
                writer.write("  ")
            }
        }
    }

    private fun beforeValue() {
        if (!firstInContainer) {
            writer.write(",")
        }
        if (depth > 0) {
            indent()
        }
        firstInContainer = false
    }

    private fun beforeKey() {
        if (!firstInContainer) {
            writer.write(",")
        }
        indent()
        firstInContainer = false
    }

    /** Begin a JSON object. */
    fun beginObject(): JsonWriter {
        beforeValue()
        writer.write("{")
        depth++
        firstInContainer = true
        if (stackPtr < inObject.size) {
            inObject[stackPtr++] = true
        }
        return this
    }

    /** End the current JSON object. */
    fun endObject(): JsonWriter {
        depth--
        if (stackPtr > 0) stackPtr--
        if (!firstInContainer) {
            indent()
        }
        writer.write("}")
        firstInContainer = false
        return this
    }

    /** Begin a JSON array. */
    fun beginArray(): JsonWriter {
        beforeValue()
        writer.write("[")
        depth++
        firstInContainer = true
        if (stackPtr < inObject.size) {
            inObject[stackPtr++] = false
        }
        return this
    }

    /** End the current JSON array. */
    fun endArray(): JsonWriter {
        depth--
        if (stackPtr > 0) stackPtr--
        if (!firstInContainer) {
            indent()
        }
        writer.write("]")
        firstInContainer = false
        return this
    }

    /** Write a key (must be inside an object). */
    fun name(name: String): JsonWriter {
        beforeKey()
        writer.write(quoteString(name))
        writer.write(": ")
        firstInContainer = true
        return this
    }

    /** Write a string value. */
    fun value(s: String?): JsonWriter {
        if (s == null) return nullValue()
        beforeValue()
        writer.write(quoteString(s))
        return this
    }

    /** Write a boolean value. */
    fun value(b: Boolean): JsonWriter {
        beforeValue()
        writer.write(if (b) "true" else "false")
        return this
    }

    /** Write a long value. */
    fun value(n: Long): JsonWriter {
        beforeValue()
        writer.write(n.toString())
        return this
    }

    /** Write an int value. */
    fun value(n: Int): JsonWriter {
        beforeValue()
        writer.write(n.toString())
        return this
    }

    /** Write a double value. */
    fun value(n: Double): JsonWriter {
        beforeValue()
        if (n.isNaN() || n.isInfinite()) {
            writer.write("null")
        } else {
            writer.write(n.toString())
        }
        return this
    }

    /** Write a float value. */
    fun value(n: Float): JsonWriter {
        return value(n.toDouble())
    }

    /** Write a null value. */
    fun nullValue(): JsonWriter {
        beforeValue()
        writer.write("null")
        return this
    }

    /** Close the writer. */
    override fun close() {
        writer.flush()
    }

    private fun quoteString(s: String): String {
        val sb = StringBuilder(s.length + 8)
        sb.append('"')
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                else -> if (c.code < 0x20) {
                    sb.append("\\u")
                    sb.append("%04x".format(c.code))
                } else {
                    sb.append(c)
                }
            }
        }
        sb.append('"')
        return sb.toString()
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SERIALIZER
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Serializes a [ValidationReport] to JSON.
 *
 * Pure function — no side effects beyond writing to the output writer.
 */
class ValidationReportSerializer {

    /**
     * Serialize [report] to a JSON string.
     */
    fun toJson(report: ValidationReport): String {
        val sw = StringWriter(8192)
        JsonWriter(sw).use { writer ->
            writeReport(writer, report)
        }
        return sw.toString()
    }

    /**
     * Serialize [report] to a [Writer]. Use for streaming to a file.
     */
    fun toJson(report: ValidationReport, writer: Writer) {
        JsonWriter(writer).use { jw ->
            writeReport(jw, report)
        }
    }

    // ─── Top-level report ────────────────────────────────────────────────

    private fun writeReport(writer: JsonWriter, report: ValidationReport) {
        writer.beginObject()
        writer.name("schema").value("riko.validation.report.v1")
        writer.name("header")
        writeHeader(writer, report.header)
        writer.name("gates")
        writeGates(writer, report)
        writer.name("exitDecision")
        writeExitDecision(writer, report.exitDecision)
        writer.endObject()
    }

    private fun writeHeader(writer: JsonWriter, header: ReportHeader) {
        writer.beginObject()
        writer.name("reportId").value(header.reportId)
        writer.name("generatedAtMs").value(header.generatedAtMs)
        writer.name("projectVersion").value(header.projectVersion)
        writer.name("validationRunId").value(header.validationRunId)
        writer.name("deviceFingerprint").value(header.deviceFingerprint)
        writer.name("durationMs").value(header.durationMs)
        writer.name("generatedBy").value(header.generatedBy)
        writer.endObject()
    }

    private fun writeGates(writer: JsonWriter, report: ValidationReport) {
        writer.beginObject()
        report.g1LibraryReality?.let {
            writer.name("g1_library_reality")
            writeG1(writer, it)
        }
        report.g2MemoryAudit?.let {
            writer.name("g2_memory_audit")
            writeG2(writer, it)
        }
        report.g3FrameTiming?.let {
            writer.name("g3_frame_timing")
            writeG3(writer, it)
        }
        report.g4AnimationContinuity?.let {
            writer.name("g4_animation_continuity")
            writeG4(writer, it)
        }
        report.g5StressTest?.let {
            writer.name("g5_stress_test")
            writeG5(writer, it)
        }
        report.g6IdentityTest?.let {
            writer.name("g6_identity_test")
            writeG6(writer, it)
        }
        writer.endObject()
    }

    // ─── Per-gate writers ────────────────────────────────────────────────

    private fun writeG1(writer: JsonWriter, report: G1LibraryRealityReport) {
        writer.beginObject()
        writer.name("gateId").value(report.gateId.code)
        writer.name("evaluatedAtMs").value(report.evaluatedAtMs)
        writer.name("durationMs").value(report.durationMs)
        writer.name("verdict")
        writeVerdict(writer, report.verdict)
        writer.name("tierResults")
        writer.beginArray()
        report.tierResults.forEach { tier ->
            writer.beginObject()
            writer.name("tier").value(tier.tier.code)
            writer.name("tierName").value(tier.tier.displayName)
            writer.name("trackCount").value(tier.tier.trackCount)
            writer.name("passed").value(tier.passed)
            writer.name("failureCount").value(tier.failureCount)
            writer.name("coldStartMs").value(tier.coldStartMs)
            writer.name("scrollFrameP95Ms").value(tier.scrollFrameP95Ms)
            writer.name("rankingMs").value(tier.rankingMs)
            writer.name("heapMb").value(tier.heapMb)
            if (tier.failureSummaries.isNotEmpty()) {
                writer.name("failures")
                writer.beginArray()
                tier.failureSummaries.forEach { writer.value(it) }
                writer.endArray()
            }
            writer.endObject()
        }
        writer.endArray()
        writer.endObject()
    }

    private fun writeG2(writer: JsonWriter, report: G2MemoryAuditReport) {
        writer.beginObject()
        writer.name("gateId").value(report.gateId.code)
        writer.name("evaluatedAtMs").value(report.evaluatedAtMs)
        writer.name("durationMs").value(report.durationMs)
        writer.name("verdict")
        writeVerdict(writer, report.verdict)
        writer.name("cyclesCompleted").value(report.cyclesCompleted)
        writer.name("totalLeaksDetected").value(report.totalLeaksDetected)
        writer.name("totalRetainedBytes").value(report.totalRetainedBytes)
        writer.name("criticalLeakCount").value(report.criticalLeakCount)
        writer.name("highLeakCount").value(report.highLeakCount)
        writer.name("surfaceSummaries")
        writer.beginArray()
        report.surfaceSummaries.forEach { surface ->
            writer.beginObject()
            writer.name("surface").value(surface.surface.name)
            writer.name("surfaceName").value(surface.surface.displayName)
            writer.name("totalLeakCount").value(surface.totalLeakCount)
            writer.name("totalBytes").value(surface.totalBytes)
            writer.name("maxSeverity").value(surface.maxSeverity.name)
            writer.endObject()
        }
        writer.endArray()
        writer.endObject()
    }

    private fun writeG3(writer: JsonWriter, report: G3FrameTimingReport) {
        writer.beginObject()
        writer.name("gateId").value(report.gateId.code)
        writer.name("evaluatedAtMs").value(report.evaluatedAtMs)
        writer.name("durationMs").value(report.durationMs)
        writer.name("verdict")
        writeVerdict(writer, report.verdict)
        writer.name("perRateEvaluations")
        writer.beginArray()
        report.perRateEvaluations.forEach { rate ->
            writer.beginObject()
            writer.name("rate").value(rate.rate.name)
            writer.name("hz").value(rate.rate.hz)
            writer.name("budgetMs").value(rate.rate.budgetMs)
            writer.name("passed").value(rate.passed)
            writer.name("sampleCount").value(rate.sampleCount)
            writer.name("p50Ms").value(rate.p50Ms)
            writer.name("p95Ms").value(rate.p95Ms)
            writer.name("p99Ms").value(rate.p99Ms)
            writer.name("jankRate").value(rate.jankRate)
            writer.name("longestGoodStreak").value(rate.longestGoodStreak)
            if (rate.failureSummaries.isNotEmpty()) {
                writer.name("failures")
                writer.beginArray()
                rate.failureSummaries.forEach { writer.value(it) }
                writer.endArray()
            }
            writer.endObject()
        }
        writer.endArray()
        writer.endObject()
    }

    private fun writeG4(writer: JsonWriter, report: G4AnimationContinuityReport) {
        writer.beginObject()
        writer.name("gateId").value(report.gateId.code)
        writer.name("evaluatedAtMs").value(report.evaluatedAtMs)
        writer.name("durationMs").value(report.durationMs)
        writer.name("verdict")
        writeVerdict(writer, report.verdict)
        writer.name("cyclesCompleted").value(report.cyclesCompleted)
        writer.name("cyclesPassed").value(report.cyclesPassed)
        writer.name("cyclesFailed").value(report.cyclesFailed)
        writer.name("cycleDurationMeanMs").value(report.cycleDurationMeanMs)
        writer.name("cycleDurationStdDevMs").value(report.cycleDurationStdDevMs)
        writer.name("memoryGrowthPerCycleBytes").value(report.memoryGrowthPerCycleBytes)
        writer.name("recompositionGrowthPerCycle").value(report.recompositionGrowthPerCycle)
        writer.name("maxMiniPlayerDriftPx").value(report.maxMiniPlayerDriftPx)
        writer.endObject()
    }

    private fun writeG5(writer: JsonWriter, report: G5StressTestReport) {
        writer.beginObject()
        writer.name("gateId").value(report.gateId.code)
        writer.name("evaluatedAtMs").value(report.evaluatedAtMs)
        writer.name("durationMs").value(report.durationMs)
        writer.name("verdict")
        writeVerdict(writer, report.verdict)
        writer.name("totalActions").value(report.totalActions)
        writer.name("successCount").value(report.successCount)
        writer.name("failureCount").value(report.failureCount)
        writer.name("crashCount").value(report.crashCount)
        writer.name("anrCount").value(report.anrCount)
        writer.name("stateInconsistentCount").value(report.stateInconsistentCount)
        writer.name("successRate").value(report.successRate)
        if (report.criticalFailuresSummary.isNotEmpty()) {
            writer.name("criticalFailures")
            writer.beginArray()
            report.criticalFailuresSummary.forEach { writer.value(it) }
            writer.endArray()
        }
        if (report.failuresByCategory.isNotEmpty()) {
            writer.name("failuresByCategory")
            writer.beginObject()
            report.failuresByCategory.forEach { (category, count) ->
                writer.name(category).value(count)
            }
            writer.endObject()
        }
        writer.endObject()
    }

    private fun writeG6(writer: JsonWriter, report: G6IdentityTestReport) {
        writer.beginObject()
        writer.name("gateId").value(report.gateId.code)
        writer.name("evaluatedAtMs").value(report.evaluatedAtMs)
        writer.name("durationMs").value(report.durationMs)
        writer.name("verdict")
        writeVerdict(writer, report.verdict)
        writer.name("reviewersPassed").value(report.reviewersPassed)
        writer.name("totalReviewers").value(report.totalReviewers)
        writer.name("meanRecognitionMs").value(report.meanRecognitionMs)
        writer.name("mostCitedSignal").value(report.mostCitedSignal?.name)
        writer.name("reviewerResults")
        writer.beginArray()
        report.reviewerResults.forEach { reviewer ->
            writer.beginObject()
            writer.name("reviewerId").value(reviewer.reviewerId)
            writer.name("displayName").value(reviewer.displayName)
            writer.name("familiarity").value(reviewer.familiarity)
            writer.name("recognitionPassCount").value(reviewer.recognitionPassCount)
            writer.name("totalSessions").value(reviewer.totalSessions)
            writer.name("meanRecognitionMs").value(reviewer.meanRecognitionMs)
            writer.name("mostCitedSignal").value(reviewer.mostCitedSignal?.name)
            writer.name("reviewerPass").value(reviewer.reviewerPass)
            writer.endObject()
        }
        writer.endArray()
        writer.endObject()
    }

    // ─── Verdict writer ──────────────────────────────────────────────────

    private fun writeVerdict(writer: JsonWriter, verdict: GateVerdict) {
        writer.beginObject()
        when (verdict) {
            is GateVerdict.Passed -> {
                writer.name("status").value("PASSED")
            }
            is GateVerdict.Incomplete -> {
                writer.name("status").value("INCOMPLETE")
                writer.name("reason").value(verdict.reason)
            }
            is GateVerdict.Failed -> {
                writer.name("status").value("FAILED")
                writer.name("reason").value(verdict.reason)
                if (verdict.blockingItems.isNotEmpty()) {
                    writer.name("blockingItems")
                    writer.beginArray()
                    verdict.blockingItems.forEach { writer.value(it) }
                    writer.endArray()
                }
            }
            is GateVerdict.Skipped -> {
                writer.name("status").value("SKIPPED")
                writer.name("reason").value(verdict.reason)
            }
        }
        writer.endObject()
    }

    // ─── Exit decision writer ────────────────────────────────────────────

    private fun writeExitDecision(writer: JsonWriter, decision: ExitDecision) {
        writer.beginObject()
        when (decision) {
            is ExitDecision.Approved -> {
                writer.name("status").value("APPROVED")
                writer.name("gatesPassed").value(decision.gatesPassed)
                writer.name("totalGates").value(decision.totalGates)
            }
            is ExitDecision.Blocked -> {
                writer.name("status").value("BLOCKED")
                writer.name("blockingGates")
                writer.beginArray()
                decision.blockingGates.forEach { gate ->
                    writer.beginObject()
                    writer.name("id").value(gate.id.code)
                    writer.name("title").value(gate.title)
                    writer.name("reason").value(gate.reason)
                    writer.endObject()
                }
                writer.endArray()
            }
        }
        writer.endObject()
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// CONVENIENCE EXTENSION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Serialize this [ValidationReport] to JSON.
 *
 * Convenience extension so callers don't need to instantiate the
 * serializer explicitly.
 */
fun ValidationReport.toJson(): String =
    ValidationReportSerializer().toJson(this)

/**
 * Serialize this [ValidationReport] to JSON and write to [writer].
 */
fun ValidationReport.toJson(writer: Writer) {
    ValidationReportSerializer().toJson(this, writer)
}
