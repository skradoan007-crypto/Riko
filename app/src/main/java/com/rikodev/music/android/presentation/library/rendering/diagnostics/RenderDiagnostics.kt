/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B4 — Library Rendering Runtime
 *
 * File 13 / 13 — RenderDiagnostics.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [RenderDiagnostics] is the *observability layer* for the Library
 * Rendering Runtime. It records what the renderer did, why it did
 * it, and how long it took — without ever affecting rendering.
 *
 * Rule #53 — Diagnostics Never Affects Engine.
 * This file is the operationalization of that rule. Every diagnostic
 * field is read-only observation. The renderer never branches on
 * these fields. The Inspector overlay reads them; nothing else does.
 *
 * WHAT THIS FILE IS NOT
 * ---------------------
 * This file is NOT a logger. It does not write to logcat, files, or
 * remote telemetry. It is an in-memory record the host can query.
 *
 * This file is NOT a profiler. It records coarse timings, not
 * per-frame micro-timings. For per-frame profiling, use the
 * platform profiler (Studio profiler, Perfetto, etc.).
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
 * © RIKO Experience OS — Rendering Tier
 */

package com.rikodev.music.android.presentation.library.rendering.diagnostics

import com.rikodev.music.android.presentation.library.rendering.engine.SectionDiff
import com.rikodev.music.android.presentation.library.rendering.layout.LayoutDecisionLog
import com.rikodev.music.android.presentation.library.rendering.layout.LayoutSourceHint
import com.rikodev.music.android.presentation.library.rendering.model.RenderDiagnosticsSummary
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Severity of a render-layer warning.
 */
enum class RenderWarningSeverity(internal val description: String) {
    /** Informational — no action needed. */
    INFO("Informational."),

    /** Soft warning — something suboptimal happened, but rendering continued. */
    SOFT("Soft warning — suboptimal but functional."),

    /** Hard warning — rendering produced a degraded result. */
    HARD("Hard warning — degraded rendering."),

    /** Critical — rendering failed entirely. */
    CRITICAL("Critical — rendering failed."),
}

/**
 * A single render-layer warning.
 *
 * Warnings are produced when the renderer encounters a situation
 * that *did not* throw an exception but *did* produce a suboptimal
 * result. Examples:
 *   • A section's contract was missing → placeholder used.
 *   • Item count below layout threshold → layout downgraded.
 *   • Personality palette missing → neutral fallback used.
 *   • Diff computation hit the "full re-render" threshold.
 *
 * @property severity       How serious.
 * @property stableKey      Stable key of the affected section, or null
 *                          if the warning is snapshot-wide.
 * @property category       Warning category — drives Inspector grouping.
 * @property message        Human-readable message.
 * @property timestamp      Wall-clock time (epoch ms).
 */
data class RenderWarning(
    val severity: RenderWarningSeverity,
    val stableKey: String?,
    val category: RenderWarningCategory,
    val message: String,
    val timestamp: Long,
)

/**
 * Categories of render-layer warnings.
 */
enum class RenderWarningCategory(internal val description: String) {
    /** Contract lookup failed — section used placeholder. */
    CONTRACT_MISSING("Contract lookup failed."),

    /** Runtime snapshot missing for an active section. */
    SNAPSHOT_MISSING("Runtime snapshot missing."),

    /** Layout was downgraded due to item count. */
    LAYOUT_DOWNGRADED("Layout downgraded due to item count."),

    /** Theme resolution fell back to neutral. */
    THEME_FALLBACK("Theme fell back to neutral."),

    /** Empty state copy was generated for an unknown kind. */
    EMPTY_STATE_FALLBACK("Empty state used fallback copy."),

    /** Diff computation triggered full re-render. */
    DIFF_FULL_RERENDER("Diff triggered full re-render."),

    /** Render duration exceeded threshold. */
    SLOW_RENDER("Render duration exceeded threshold."),

    /** Snapshot schema mismatch — UI may be older than runtime. */
    SCHEMA_MISMATCH("Schema mismatch between UI and runtime."),
}

/**
 * Per-section render diagnostic record.
 *
 * @property stableKey           Section identity. Rule #44.
 * @property layoutDecision      What layout was chosen + why.
 * @property itemCount           Number of items rendered.
 * @property emptyStateUsed      True if the section showed an empty state.
 * @property themeResolutionMs   Time spent resolving the theme.
 * @property itemResolutionMs    Time spent resolving items.
 * @property totalMs             Total time spent rendering this section.
 */
data class SectionRenderDiagnostic(
    val stableKey: String,
    val layoutDecision: LayoutDecisionLog,
    val itemCount: Int,
    val emptyStateUsed: Boolean,
    val themeResolutionMs: Long,
    val itemResolutionMs: Long,
    val totalMs: Long,
) {
    val isSlow: Boolean get() = totalMs > 16L
}

/**
 * Rolling-window diagnostics for the rendering layer.
 *
 * Keeps the last [maxSnapshots] snapshot-level diagnostics + the
 * last [maxWarnings] warnings. Thread-safe (uses CopyOnWrite lists).
 *
 * The Inspector overlay reads this; nothing else does.
 *
 * @property maxSnapshots   Maximum snapshot records to keep.
 * @property maxWarnings    Maximum warnings to keep.
 * @property maxPerSection  Maximum per-section records to keep per snapshot.
 */
class RenderDiagnostics(
    private val maxSnapshots: Int = 10,
    private val maxWarnings: Int = 50,
    private val maxPerSection: Int = 50,
) {
    private val snapshotRecords = CopyOnWriteArrayList<SnapshotRenderRecord>()
    private val warnings = CopyOnWriteArrayList<RenderWarning>()

    /**
     * Append a snapshot-level record.
     */
    fun appendSnapshotRecord(record: SnapshotRenderRecord) {
        snapshotRecords.add(record)
        while (snapshotRecords.size > maxSnapshots) {
            snapshotRecords.removeAt(0)
        }
    }

    /**
     * Append a warning.
     */
    fun appendWarning(warning: RenderWarning) {
        warnings.add(warning)
        while (warnings.size > maxWarnings) {
            warnings.removeAt(0)
        }
    }

    /**
     * Recent snapshot records, newest first.
     */
    fun recentSnapshots(): List<SnapshotRenderRecord> =
        snapshotRecords.reversed()

    /**
     * Recent warnings, newest first.
     */
    fun recentWarnings(): List<RenderWarning> =
        warnings.reversed()

    /**
     * Count of warnings by severity.
     */
    fun warningCountBySeverity(): Map<RenderWarningSeverity, Int> =
        warnings.groupingBy { it.severity }.eachCount()

    /**
     * Average render duration over the recent snapshots, in milliseconds.
     * Returns 0 if no snapshots recorded.
     */
    fun averageRenderDurationMs(): Long {
        if (snapshotRecords.isEmpty()) return 0L
        return snapshotRecords.sumOf { it.summary.renderDurationMs } / snapshotRecords.size
    }

    /**
     * True if the last render was slow (above [slowThresholdMs]).
     */
    fun wasLastRenderSlow(slowThresholdMs: Long = 32L): Boolean {
        val last = snapshotRecords.lastOrNull() ?: return false
        return last.summary.renderDurationMs > slowThresholdMs
    }

    /**
     * Clear all records.
     */
    fun clear() {
        snapshotRecords.clear()
        warnings.clear()
    }
}

/**
 * A snapshot-level render record.
 *
 * @property snapshotId         Stable identifier for the snapshot
 *                              (typically the sourceFingerprint).
 * @property createdAt          Wall-clock time (epoch ms).
 * @property summary            The diagnostic summary that traveled with
 *                              the snapshot.
 * @property sectionDiagnostics Per-section diagnostics, keyed by stableKey.
 * @property diffSummary        High-level diff summary (counts only).
 */
data class SnapshotRenderRecord(
    val snapshotId: String,
    val createdAt: Long,
    val summary: RenderDiagnosticsSummary,
    val sectionDiagnostics: Map<String, SectionRenderDiagnostic>,
    val diffSummary: DiffSummary,
)

/**
 * High-level diff summary — counts only, no detail.
 */
data class DiffSummary(
    val sectionsAdded: Int,
    val sectionsRemoved: Int,
    val sectionsChanged: Int,
    val itemsChanged: Int,
    val isFullRerender: Boolean,
) {
    companion object {
        fun from(diff: com.rikodev.music.android.presentation.library.rendering.engine.RenderDiff): DiffSummary {
            val added = diff.sectionDiffs.count { it is SectionDiff.Added }
            val removed = diff.sectionDiffs.count { it is SectionDiff.Removed }
            val changed = diff.sectionDiffs.count { it is SectionDiff.Changed }
            return DiffSummary(
                sectionsAdded = added,
                sectionsRemoved = removed,
                sectionsChanged = changed,
                itemsChanged = diff.totalChangedItems,
                isFullRerender = diff.isFullRerender,
            )
        }

        val NONE = DiffSummary(
            sectionsAdded = 0,
            sectionsRemoved = 0,
            sectionsChanged = 0,
            itemsChanged = 0,
            isFullRerender = false,
        )
    }
}

/**
 * Helpers for producing common warnings.
 */
object RenderWarnings {

    fun contractMissing(stableKey: String, nowMs: Long): RenderWarning = RenderWarning(
        severity = RenderWarningSeverity.HARD,
        stableKey = stableKey,
        category = RenderWarningCategory.CONTRACT_MISSING,
        message = "Contract not found for stableKey=$stableKey; placeholder used.",
        timestamp = nowMs,
    )

    fun snapshotMissing(stableKey: String, nowMs: Long): RenderWarning = RenderWarning(
        severity = RenderWarningSeverity.HARD,
        stableKey = stableKey,
        category = RenderWarningCategory.SNAPSHOT_MISSING,
        message = "Runtime snapshot missing for stableKey=$stableKey.",
        timestamp = nowMs,
    )

    fun layoutDowngraded(
        stableKey: String,
        fromLayout: String,
        toLayout: String,
        itemCount: Int,
        nowMs: Long,
    ): RenderWarning = RenderWarning(
        severity = RenderWarningSeverity.SOFT,
        stableKey = stableKey,
        category = RenderWarningCategory.LAYOUT_DOWNGRADED,
        message = "Layout downgraded from $fromLayout to $toLayout (items=$itemCount).",
        timestamp = nowMs,
    )

    fun themeFallback(stableKey: String, personalityKey: String, nowMs: Long): RenderWarning = RenderWarning(
        severity = RenderWarningSeverity.SOFT,
        stableKey = stableKey,
        category = RenderWarningCategory.THEME_FALLBACK,
        message = "Theme fell back to neutral (personality=$personalityKey not found).",
        timestamp = nowMs,
    )

    fun diffFullRerender(snapshotId: String, nowMs: Long): RenderWarning = RenderWarning(
        severity = RenderWarningSeverity.INFO,
        stableKey = null,
        category = RenderWarningCategory.DIFF_FULL_RERENDER,
        message = "Diff triggered full re-render (snapshot=$snapshotId).",
        timestamp = nowMs,
    )

    fun slowRender(durationMs: Long, thresholdMs: Long, nowMs: Long): RenderWarning = RenderWarning(
        severity = RenderWarningSeverity.SOFT,
        stableKey = null,
        category = RenderWarningCategory.SLOW_RENDER,
        message = "Render took ${durationMs}ms (threshold=${thresholdMs}ms).",
        timestamp = nowMs,
    )

    fun schemaMismatch(expected: Int, actual: Int, nowMs: Long): RenderWarning = RenderWarning(
        severity = RenderWarningSeverity.CRITICAL,
        stableKey = null,
        category = RenderWarningCategory.SCHEMA_MISMATCH,
        message = "Schema mismatch — expected v$expected, got v$actual.",
        timestamp = nowMs,
    )
}
