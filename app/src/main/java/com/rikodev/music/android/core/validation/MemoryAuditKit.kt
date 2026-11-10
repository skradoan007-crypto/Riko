/*
 * RIKO EXPERIENCE OS — PRODUCTION-GRADE ROADMAP v2.0
 * Milestone 2.9 — Experience Validation
 *
 * File 3 / 10 — MemoryAuditKit.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Gate G2 — Memory Audit. Tracks allocations, references, and releases
 * across the five leak surfaces that historically cause Android memory
 * issues:
 *
 *   1. Bitmap         — album art, wave render targets
 *   2. Coroutine      — long-lived scopes, leaked jobs
 *   3. Flow collector  — collectors that outlive their lifecycle
 *   4. Snapshot        — Compose mutable state retained beyond composition
 *   5. Native          — JNI / NIO / direct ByteBuffers
 *
 * This kit is OBSERVATION-ONLY. It records what was allocated, what
 * was released, and what remains. It never forces GC, never drops
 * references, never clears caches. (Rule #76.)
 *
 * ARCHITECTURE
 * ------------
 *
 *   ┌──────────────────────────────────────────────────────────────┐
 *   │                    MEMORY AUDIT KIT                          │
 *   │                                                              │
 *   │  Allocation Tracker  ──┐                                     │
 *   │  Release Tracker     ──┤                                     │
 *   │  Snapshot Sampler    ──┼──→ LeakDetector ──→ AuditReport     │
 *   │  Cycle Boundary      ──┤                                     │
 *   │  LeakCanary Hook     ──┘                                     │
 *   │                                                              │
 *   │  Five leak surfaces tracked independently                   │
 *   └──────────────────────────────────────────────────────────────┘
 *
 * LEAK SURFACES
 * -------------
 *   Bitmap      — track allocation, recycling, and finalization
 *   Coroutine   — track job creation, cancellation, completion
 *   Flow        — track collector registration and disposal
 *   Snapshot    — track mutable state allocation and disposal
 *   Native      — track direct buffer allocation and release
 *
 * CYCLE DEFINITION
 * ----------------
 * A "navigation cycle" is one round-trip:
 *   Screen A → Screen B → Screen A
 * After each cycle, the audit kit checks whether references created
 * during the cycle have been released. If they remain after the cycle
 * ends, that is a potential leak.
 *
 * RULES ENFORCED
 * --------------
 * #53  Diagnostics Never Affects Engine — kit observes only
 * #76  Performance Guards Never Optimize — never auto-clears
 *
 * © RIKO Experience OS — Validation Tier
 */

package com.rikodev.music.android.core.validation

import java.util.concurrent.atomic.AtomicLong
import androidx.compose.ui.graphics.Path

// ═══════════════════════════════════════════════════════════════════════════
// LEAK SURFACE
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The five leak surfaces tracked by this kit.
 */
enum class LeakSurface(val displayName: String, val description: String) {
    BITMAP("Bitmap", "Album art, wave render targets, decoded images"),
    COROUTINE("Coroutine", "Long-lived scopes, leaked jobs, unfinished work"),
    FLOW_COLLECTOR("Flow Collector", "Collectors that outlive their lifecycle"),
    SNAPSHOT("Snapshot", "Compose mutable state retained beyond composition"),
    NATIVE("Native", "JNI / NIO / direct ByteBuffers not released"),
}

/**
 * Severity of a detected leak.
 */
enum class LeakSeverity {
    /** Small retention, likely transient — investigate if persistent. */
    LOW,

    /** Moderate retention — should be fixed before release. */
    MEDIUM,

    /** Large retention or growing trend — must be fixed before LOCK. */
    HIGH,

    /** Critical — will cause OOM under sustained use. Blocks LOCK. */
    CRITICAL,
}

// ═══════════════════════════════════════════════════════════════════════════
// ALLOCATION TOKEN
// ═══════════════════════════════════════════════════════════════════════════

/**
 * A unique token identifying a tracked allocation.
 *
 * Tokens are assigned monotonically and never reused, so historical
 * reports remain interpretable.
 */
@JvmInline
value class AllocationToken(val value: Long) {
    override fun toString(): String = "Token#${value.toString(16)}"
}

/**
 * Metadata recorded for each tracked allocation.
 */
data class AllocationRecord(
    val token: AllocationToken,
    val surface: LeakSurface,
    val cycleId: CycleId,
    val description: String,
    val sizeBytes: Long,
    val stackTrace: String?,
    val allocatedAtMs: Long,
) {
    init {
        require(sizeBytes >= 0) { "Size cannot be negative" }
    }
}

/**
 * Metadata recorded when an allocation is released.
 */
data class ReleaseRecord(
    val token: AllocationToken,
    val releasedAtMs: Long,
    val releasePath: String?,
)

// ═══════════════════════════════════════════════════════════════════════════
// CYCLE ID
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Identifies a single navigation cycle.
 *
 * Format: "C{n}" where n is the cycle number (1-indexed).
 */
@JvmInline
value class CycleId(val value: String) {
    val number: Int get() = value.removePrefix("C").toIntOrNull() ?: 0
    override fun toString(): String = value
}

// ═══════════════════════════════════════════════════════════════════════════
// ALLOCATION TRACKER
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Tracks allocations and releases across navigation cycles.
 *
 * Thread-safe — uses concurrent collections so allocations from
 * multiple threads (e.g. main + background coroutines) are tracked
 * correctly.
 *
 * This class is the central record-keeper. Leak detectors query it
 * to determine what remains retained.
 */
class AllocationTracker {

    private val nextToken = AtomicLong(1)
    private val allocations: MutableMap<AllocationToken, AllocationRecord> = mutableMapOf()
    private val releases: MutableMap<AllocationToken, ReleaseRecord> = mutableMapOf()
    private val cycleBoundaries: MutableMap<CycleId, CycleBoundary> = mutableMapOf()

    // ─── Allocation / release ──────────────────────────────────────────────

    /**
     * Record a new allocation.
     *
     * @return the token assigned to this allocation. Callers should
     *         retain the token and pass it to [recordRelease] when
     *         the resource is freed.
     */
    @Synchronized
    fun recordAllocation(
        surface: LeakSurface,
        cycleId: CycleId,
        description: String,
        sizeBytes: Long,
        stackTrace: String? = null,
    ): AllocationToken {
        val token = AllocationToken(nextToken.getAndIncrement())
        val record = AllocationRecord(
            token = token,
            surface = surface,
            cycleId = cycleId,
            description = description,
            sizeBytes = sizeBytes,
            stackTrace = stackTrace,
            allocatedAtMs = System.currentTimeMillis(),
        )
        allocations[token] = record
        return token
    }

    /**
     * Record the release of a previously-tracked allocation.
     */
    @Synchronized
    fun recordRelease(
        token: AllocationToken,
        releasePath: String? = null,
    ) {
        require(token in allocations) {
            "Cannot release unknown allocation $token"
        }
        releases[token] = ReleaseRecord(
            token = token,
            releasedAtMs = System.currentTimeMillis(),
            releasePath = releasePath,
        )
    }

    // ─── Cycle boundaries ─────────────────────────────────────────────────

    /**
     * Mark the start of a navigation cycle.
     */
    @Synchronized
    fun markCycleStart(cycleId: CycleId, screenTransition: String) {
        require(cycleId !in cycleBoundaries) {
            "Cycle $cycleId already started"
        }
        cycleBoundaries[cycleId] = CycleBoundary(
            id = cycleId,
            screenTransition = screenTransition,
            startedAtMs = System.currentTimeMillis(),
            completedAtMs = null,
        )
    }

    /**
     * Mark the completion of a navigation cycle. After this, any
     * allocation created during the cycle that has not been released
     * is a potential leak.
     */
    @Synchronized
    fun markCycleEnd(cycleId: CycleId) {
        val boundary = cycleBoundaries[cycleId]
            ?: error("Cycle $cycleId was never started")
        require(boundary.completedAtMs == null) {
            "Cycle $cycleId already completed"
        }
        cycleBoundaries[cycleId] = boundary.copy(completedAtMs = System.currentTimeMillis())
    }

    // ─── Queries ──────────────────────────────────────────────────────────

    /**
     * All allocations, alive or released.
     */
    @Synchronized
    fun allAllocations(): List<AllocationRecord> = allocations.values.toList()

    /**
     * Allocations that have not been released.
     */
    @Synchronized
    fun retainedAllocations(): List<AllocationRecord> =
        allocations.values.filter { it.token !in releases }

    /**
     * Allocations created during [cycleId] that have not been released
     * after the cycle ended. These are potential leaks.
     */
    @Synchronized
    fun leaksForCycle(cycleId: CycleId): List<AllocationRecord> {
        val boundary = cycleBoundaries[cycleId] ?: return emptyList()
        val cycleEnd = boundary.completedAtMs ?: return emptyList()
        return allocations.values.filter { record ->
            record.cycleId == cycleId &&
                record.token !in releases &&
            record.allocatedAtMs <= cycleEnd
        }
    }

    /**
     * Total bytes retained across all surfaces.
     */
    @Synchronized
    fun totalRetainedBytes(): Long =
        retainedAllocations().sumOf { it.sizeBytes }

    /**
     * Retained bytes per surface.
     */
    @Synchronized
    fun retainedBytesBySurface(): Map<LeakSurface, Long> =
        retainedAllocations().groupBy { it.surface }
            .mapValues { (_, records) -> records.sumOf { it.sizeBytes } }

    /**
     * All cycle boundaries.
     */
    @Synchronized
    fun cycles(): List<CycleBoundary> = cycleBoundaries.values.toList()

    // ─── Reset ────────────────────────────────────────────────────────────

    /**
     * Reset the tracker. Use between independent validation runs.
     * Should NOT be called mid-cycle — doing so loses context.
     */
    @Synchronized
    fun reset() {
        allocations.clear()
        releases.clear()
        cycleBoundaries.clear()
        nextToken.set(1)
    }
}

/**
 * Boundary record for a navigation cycle.
 */
data class CycleBoundary(
    val id: CycleId,
    val screenTransition: String,
    val startedAtMs: Long,
    val completedAtMs: Long?,
) {
    val isComplete: Boolean get() = completedAtMs != null
    val elapsedMs: Long? get() = completedAtMs?.let { it - startedAtMs }
}

// ═══════════════════════════════════════════════════════════════════════════
// LEAK DETECTOR
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Detects leaks by querying an [AllocationTracker] after each cycle.
 *
 * Assigns severity based on retained size and surface. Produces
 * structured [LeakReport]s that the audit kit aggregates.
 */
class LeakDetector(
    private val tracker: AllocationTracker,
) {

    /**
     * Severity thresholds in bytes. Retentions above these per-cycle
     * per-surface trigger the corresponding severity.
     */
    data class SeverityThresholds(
        val lowBytes: Long = 64 * 1024,            // 64 KB
        val mediumBytes: Long = 512 * 1024,         // 512 KB
        val highBytes: Long = 4 * 1024 * 1024,      // 4 MB
        val criticalBytes: Long = 16 * 1024 * 1024, // 16 MB
    )

    private var thresholds: SeverityThresholds = SeverityThresholds()

    /**
     * Override the default severity thresholds.
     */
    fun setThresholds(newThresholds: SeverityThresholds) {
        thresholds = newThresholds
    }

    /**
     * Run leak detection for a completed cycle.
     *
     * Returns a [LeakReport] describing all leaks found, or null if
     * the cycle is not complete or has no leaks.
     */
    fun detectForCycle(cycleId: CycleId): LeakReport? {
        val leaks = tracker.leaksForCycle(cycleId)
        if (leaks.isEmpty()) return null
        val bySurface = leaks.groupBy { it.surface }
        val surfaceSummaries = bySurface.map { (surface, records) ->
            LeakSurfaceSummary(
                surface = surface,
                leakCount = records.size,
                totalBytes = records.sumOf { it.sizeBytes },
                severity = classifySeverity(records.sumOf { it.sizeBytes }),
                sampleDescriptions = records.take(3).map { it.description },
            )
        }
        return LeakReport(
            cycleId = cycleId,
            totalLeaks = leaks.size,
            totalRetainedBytes = leaks.sumOf { it.sizeBytes },
            surfaceSummaries = surfaceSummaries.sortedByDescending { it.totalBytes },
            detectedAtMs = System.currentTimeMillis(),
        )
    }

    /**
     * Run leak detection across all completed cycles.
     */
    fun detectAll(): List<LeakReport> =
        tracker.cycles()
            .filter { it.isComplete }
            .mapNotNull { detectForCycle(it.id) }

    private fun classifySeverity(retainedBytes: Long): LeakSeverity = when {
        retainedBytes >= thresholds.criticalBytes -> LeakSeverity.CRITICAL
        retainedBytes >= thresholds.highBytes -> LeakSeverity.HIGH
        retainedBytes >= thresholds.mediumBytes -> LeakSeverity.MEDIUM
        retainedBytes >= thresholds.lowBytes -> LeakSeverity.LOW
        else -> LeakSeverity.LOW
    }
}

/**
 * Summary of leaks on a single surface in a single cycle.
 */
data class LeakSurfaceSummary(
    val surface: LeakSurface,
    val leakCount: Int,
    val totalBytes: Long,
    val severity: LeakSeverity,
    val sampleDescriptions: List<String>,
)

/**
 * A complete leak report for one cycle.
 */
data class LeakReport(
    val cycleId: CycleId,
    val totalLeaks: Int,
    val totalRetainedBytes: Long,
    val surfaceSummaries: List<LeakSurfaceSummary>,
    val detectedAtMs: Long,
) {
    val hasCriticalLeaks: Boolean
        get() = surfaceSummaries.any { it.severity == LeakSeverity.CRITICAL }

    val hasHighLeaks: Boolean
        get() = surfaceSummaries.any { it.severity >= LeakSeverity.HIGH }

    val summary: String
        get() = buildString {
            append("Cycle ${cycleId.value}: ")
            append("$totalLeaks leak(s), ")
            append("${totalRetainedBytes / 1024}KB retained")
            if (hasCriticalLeaks) append(" [CRITICAL]")
            else if (hasHighLeaks) append(" [HIGH]")
        }
}

// ═══════════════════════════════════════════════════════════════════════════
// MEMORY AUDIT KIT
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The Memory Audit Kit (Gate G2).
 *
 * Combines [AllocationTracker] and [LeakDetector] into a single facade
 * that the analysis phase can drive through a clean API.
 *
 * Usage pattern:
 *   1. [startCycle] before navigating
 *   2. [trackBitmap] / [trackCoroutine] / etc. as resources are created
 *   3. [release] when resources are freed
 *   4. [endCycle] after navigating back
 *   5. [detectLeaks] to inspect the cycle
 *   6. [evaluate] for the final verdict
 */
class MemoryAuditKit(
    private val tracker: AllocationTracker = AllocationTracker(),
    private val detector: LeakDetector = LeakDetector(tracker),
) {

    private val _reports: MutableList<LeakReport> = mutableListOf()
    private var cycleCounter = 0
    val reports: List<LeakReport> get() = _reports.toList()

    // ─── Cycle control ────────────────────────────────────────────────────

    /**
     * Start a new navigation cycle.
     *
     * @param screenTransition e.g. "Library → Now Playing"
     * @return the [CycleId] for the new cycle.
     */
    fun startCycle(screenTransition: String): CycleId {
        cycleCounter++
        val id = CycleId("C$cycleCounter")
        tracker.markCycleStart(id, screenTransition)
        return id
    }

    /**
     * End a navigation cycle. After this, allocations created during
     * the cycle that have not been released are flagged as leaks.
     */
    fun endCycle(cycleId: CycleId) {
        tracker.markCycleEnd(cycleId)
    }

    // ─── Per-surface tracking ─────────────────────────────────────────────

    fun trackBitmap(
        cycleId: CycleId,
        description: String,
        sizeBytes: Long,
        stackTrace: String? = null,
    ): AllocationToken =
        tracker.recordAllocation(LeakSurface.BITMAP, cycleId, description, sizeBytes, stackTrace)

    fun trackCoroutine(
        cycleId: CycleId,
        description: String,
        stackTrace: String? = null,
    ): AllocationToken =
        tracker.recordAllocation(LeakSurface.COROUTINE, cycleId, description, 0L, stackTrace)

    fun trackFlowCollector(
        cycleId: CycleId,
        description: String,
        stackTrace: String? = null,
    ): AllocationToken =
        tracker.recordAllocation(LeakSurface.FLOW_COLLECTOR, cycleId, description, 0L, stackTrace)

    fun trackSnapshot(
        cycleId: CycleId,
        description: String,
        sizeBytes: Long,
        stackTrace: String? = null,
    ): AllocationToken =
        tracker.recordAllocation(LeakSurface.SNAPSHOT, cycleId, description, sizeBytes, stackTrace)

    fun trackNative(
        cycleId: CycleId,
        description: String,
        sizeBytes: Long,
        stackTrace: String? = null,
    ): AllocationToken =
        tracker.recordAllocation(LeakSurface.NATIVE, cycleId, description, sizeBytes, stackTrace)

    /**
     * Release a previously-tracked allocation.
     */
    fun release(token: AllocationToken, releasePath: String? = null) {
        tracker.recordRelease(token, releasePath)
    }

    // ─── Detection ────────────────────────────────────────────────────────

    /**
     * Run leak detection for a completed cycle and store the report.
     */
    fun detectLeaks(cycleId: CycleId): LeakReport? {
        val report = detector.detectForCycle(cycleId) ?: return null
        _reports += report
        return report
    }

    /**
     * Run leak detection across all completed cycles.
     */
    fun detectAllLeaks(): List<LeakReport> {
        val all = detector.detectAll()
        _reports.clear()
        _reports.addAll(all)
        return all
    }

    // ─── Queries ──────────────────────────────────────────────────────────

    fun totalRetainedBytes(): Long = tracker.totalRetainedBytes()
    fun retainedBytesBySurface(): Map<LeakSurface, Long> = tracker.retainedBytesBySurface()
    fun cycles(): List<CycleBoundary> = tracker.cycles()

    // ─── Evaluation ───────────────────────────────────────────────────────

    /**
     * Evaluate Gate G2.
     *
     * Pass requires:
     *   - At least 5 cycles completed (Rule #76 — observe across cycles)
     *   - No CRITICAL leaks in any cycle
     *   - No HIGH leaks in 3+ consecutive cycles (trend, not one-off)
     */
    fun evaluate(): GateG2Verdict {
        val completedCycles = tracker.cycles().filter { it.isComplete }
        if (completedCycles.size < 5) {
            return GateG2Verdict.Incomplete(
                completedCycles = completedCycles.size,
                required = 5,
            )
        }
        val critical = _reports.filter { it.hasCriticalLeaks }
        if (critical.isNotEmpty()) {
            return GateG2Verdict.Failed(
                reason = "Critical leaks detected in ${critical.size} cycle(s)",
                blockingReports = critical,
            )
        }
        val highTrend = findHighLeakTrend()
        if (highTrend != null) {
            return GateG2Verdict.Failed(
                reason = "High-severity leaks in 3+ consecutive cycles: ${highTrend.map { it.cycleId.value }}",
                blockingReports = highTrend,
            )
        }
        return GateG2Verdict.Passed(
            cyclesCompleted = completedCycles.size,
            totalLeaksDetected = _reports.size,
            totalRetainedBytes = _reports.sumOf { it.totalRetainedBytes },
        )
    }

    private fun findHighLeakTrend(): List<LeakReport>? {
        val sorted = _reports.sortedBy { it.cycleId.number }
        var streak = 0
        var streakStart = 0
        for (i in sorted.indices) {
            if (sorted[i].hasHighLeaks) {
                if (streak == 0) streakStart = i
                streak++
                if (streak >= 3) {
                    return sorted.subList(streakStart, i + 1)
                }
            } else {
                streak = 0
            }
        }
        return null
    }

    /**
     * Human-readable summary.
     */
    fun summaryReport(): String = buildString {
        appendLine("═══ GATE G2 — MEMORY AUDIT ═══")
        appendLine()
        appendLine("Cycles completed: ${cycles().count { it.isComplete }}")
        appendLine("Total leaks detected: ${_reports.size}")
        appendLine("Total retained: ${totalRetainedBytes() / 1024}KB")
        appendLine()
        if (_reports.isEmpty()) {
            appendLine("No leaks detected.")
        } else {
            appendLine("Leak reports:")
            _reports.take(5).forEach { report ->
                appendLine("  ${report.summary}")
                report.surfaceSummaries.forEach { surface ->
                    appendLine("    [${surface.surface.displayName}] ${surface.leakCount} leaks, ${surface.totalBytes / 1024}KB, ${surface.severity}")
                }
            }
            if (_reports.size > 5) {
                appendLine("  ... and ${_reports.size - 5} more")
            }
        }
        appendLine()
        appendLine("Verdict: ${evaluate().javaClass.simpleName}")
    }
}

/**
 * Verdict for Gate G2.
 */
sealed class GateG2Verdict {
    data class Passed(
        val cyclesCompleted: Int,
        val totalLeaksDetected: Int,
        val totalRetainedBytes: Long,
    ) : GateG2Verdict()

    data class Incomplete(
        val completedCycles: Int,
        val required: Int,
    ) : GateG2Verdict()

    data class Failed(
        val reason: String,
        val blockingReports: List<LeakReport>,
    ) : GateG2Verdict()

    val isPassed: Boolean get() = this is Passed
}
