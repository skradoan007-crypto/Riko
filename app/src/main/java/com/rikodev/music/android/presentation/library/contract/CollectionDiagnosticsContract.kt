/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B1 — Collection Contract Pack v2 (Final)
 *
 * File 13 / 15 — CollectionDiagnosticsContract.kt  ⭐ NEW in v2
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Diagnostics are NOT an add-on. Diagnostics are first-class.
 *
 * Every collection carries a diagnostics contract that the engine
 * (B3) populates and the inspector (UI layer) reads. This means:
 *
 *   • If a collection is empty, we know WHY.
 *   • If a collection is stale, we know HOW stale.
 *   • If the engine made a questionable decision, we know WHAT it
 *     was thinking.
 *   • If the user complains "why is this here?", support can answer.
 *
 * RULE #42 — Diagnostics Is First-Class (v2)
 *   Diagnostics is part of the Constitution, not a debug overlay.
 *
 * In B1, we declare the SHAPE of diagnostics. In B3, the engine
 * fills it in. The contract is the bridge — it makes diagnostics
 * a stable surface that survives engine refactors.
 *
 * No logic. Pure declaration.
 */

package com.rikodev.music.android.presentation.library.contract

/**
 * Cache status — describes whether the engine's result for this
 * collection is fresh, stale, or served from disk.
 */
enum class DiagnosticCacheStatus(
    val internalId: String,
    val description: String,
) {
    FRESH(
        internalId = "fresh",
        description = "Just generated — within the cache TTL window.",
    ),
    STALE(
        internalId = "stale",
        description = "Older than TTL but still served. Refresh pending.",
    ),
    REGENERATING(
        internalId = "regenerating",
        description = "Engine is currently regenerating this collection.",
    ),
    OFFLINE(
        internalId = "offline",
        description = "Served from disk — engine not available.",
    ),
    DISABLED(
        internalId = "disabled",
        description = "Caching is disabled for this collection.",
    ),
    ERROR(
        internalId = "error",
        description = "Last regeneration attempt failed.",
    ),
}

/**
 * Why the engine regenerated (or attempted to regenerate) this
 * collection. Drives the inspector's "regeneration reason" tooltip.
 */
enum class DiagnosticRefreshReason(
    val internalId: String,
    val description: String,
) {
    SCHEDULED(
        internalId = "scheduled",
        description = "Refresh interval elapsed — routine regeneration.",
    ),
    USER_REQUESTED(
        internalId = "user_requested",
        description = "User explicitly tapped refresh.",
    ),
    LIBRARY_CHANGED(
        internalId = "library_changed",
        description = "Underlying library data changed (songs added/removed).",
    ),
    PLAYBACK_EVENT(
        internalId = "playback_event",
        description = "A playback event (song finished, favorited) invalidated the result.",
    ),
    SETTINGS_CHANGED(
        internalId = "settings_changed",
        description = "A user setting (category toggle, sort order) changed.",
    ),
    CONTEXT_CHANGED(
        internalId = "context_changed",
        description = "External context (time, weather, activity) changed.",
    ),
    APP_LAUNCH(
        internalId = "app_launch",
        description = "App cold-started — first regeneration of the session.",
    ),
    RECOVERY(
        internalId = "recovery",
        description = "Recovery from a previous error.",
    ),
    INSPECTOR_FORCED(
        internalId = "inspector_forced",
        description = "Developer forced regeneration via inspector.",
    ),
}

/**
 * Why the engine produced this particular result. Drives the
 * "why is this here?" tooltip users can long-press for.
 */
enum class DiagnosticGenerationReason(
    val internalId: String,
    val description: String,
) {
    DEFAULT(
        internalId = "default",
        description = "Standard generation — no special reason.",
    ),
    FALLBACK(
        internalId = "fallback",
        description = "Primary strategy failed — fallback used.",
    ),
    EMPTY_LIBRARY_PLACEHOLDER(
        internalId = "empty_library_placeholder",
        description = "Library has too few items — placeholder result.",
    ),
    CONTEXTUAL(
        internalId = "contextual",
        description = "Generated based on current context (time, weather).",
    ),
    EXPERIMENTAL(
        internalId = "experimental",
        description = "Experimental strategy — may behave unexpectedly.",
    ),
    DIAGNOSTIC_ONLY(
        internalId = "diagnostic_only",
        description = "Diagnostic-only — never shown to user.",
    ),
    CACHED(
        internalId = "cached",
        description = "Result served from cache without regeneration.",
    ),
}

/**
 * Overall health of a collection — drives the green/yellow/red
 * indicator in the inspector.
 */
enum class DiagnosticHealth(
    val internalId: String,
    val description: String,
    val severity: Int, // 0 = healthy, 1 = info, 2 = warning, 3 = error
) {
    HEALTHY(
        internalId = "healthy",
        description = "All good. No issues.",
        severity = 0,
    ),
    INFO(
        internalId = "info",
        description = "Informational — e.g. result is cached.",
        severity = 1,
    ),
    DEGRADED(
        internalId = "degraded",
        description = "Functional but suboptimal — e.g. fallback used.",
        severity = 2,
    ),
    STALE(
        internalId = "stale",
        description = "Result is significantly older than TTL.",
        severity = 2,
    ),
    INCOMPLETE(
        internalId = "incomplete",
        description = "Engine produced fewer items than the contract minimum.",
        severity = 3,
    ),
    ERROR(
        internalId = "error",
        description = "Last generation attempt failed.",
        severity = 3,
    ),
    UNAVAILABLE(
        internalId = "unavailable",
        description = "Collection cannot be generated — missing data or permissions.",
        severity = 3,
    ),
}

/**
 * Developer-facing flags that the inspector can toggle at runtime
 * to influence engine behaviour for this collection. All default
 * to false; the engine reads them as advisory, not authoritative.
 */
data class DiagnosticDeveloperFlags(
    val forceRegenerate: Boolean,
    val bypassCache: Boolean,
    val verboseLogging: Boolean,
    val renderDiagnosticOverlay: Boolean,
    val freezeResult: Boolean,
    val simulateEmpty: Boolean,
    val simulateError: Boolean,
) {
    companion object {
        val NONE = DiagnosticDeveloperFlags(
            forceRegenerate = false,
            bypassCache = false,
            verboseLogging = false,
            renderDiagnosticOverlay = false,
            freezeResult = false,
            simulateEmpty = false,
            simulateError = false,
        )
    }
}

/**
 * A single warning the engine wants to surface to the inspector.
 */
data class DiagnosticWarning(
    val code: String,
    val message: String,
    val severity: Int,
    val timestamp: Long,
)

/**
 * The complete diagnostics contract for a collection.
 *
 * The engine (B3) populates this; the inspector (UI) reads it.
 * The contract itself contains zero logic — it is a structured
 * snapshot of what the engine was thinking.
 */
data class CollectionDiagnosticsContract(
    /** Wall-clock time (epoch ms) the engine last ran for this collection. */
    val generationTime: Long,
    /** Cache status of the current result. */
    val cacheStatus: DiagnosticCacheStatus,
    /** Why the engine last regenerated. */
    val refreshReason: DiagnosticRefreshReason,
    /** Why the engine produced this particular result. */
    val generationReason: DiagnosticGenerationReason,
    /** Wall-clock time the result was last updated. */
    val lastUpdate: Long,
    /** Overall health. */
    val collectionHealth: DiagnosticHealth,
    /** Active warnings (may be empty). */
    val warnings: List<DiagnosticWarning>,
    /** Developer flags — advisory to the engine. */
    val developerFlags: DiagnosticDeveloperFlags,
    /** How many milliseconds the last generation took. */
    val generationDurationMs: Long,
    /** How many candidate items the engine considered. */
    val candidateCount: Int,
    /** How many items the engine emitted. */
    val emittedCount: Int,
    /** Engine version that produced this result (for audit). */
    val engineVersion: String,
) {

    companion object {

        /** Used when no diagnostics have been recorded yet. */
        val UNDIAGNOSED = CollectionDiagnosticsContract(
            generationTime = 0L,
            cacheStatus = DiagnosticCacheStatus.DISABLED,
            refreshReason = DiagnosticRefreshReason.SCHEDULED,
            generationReason = DiagnosticGenerationReason.DEFAULT,
            lastUpdate = 0L,
            collectionHealth = DiagnosticHealth.UNAVAILABLE,
            warnings = emptyList(),
            developerFlags = DiagnosticDeveloperFlags.NONE,
            generationDurationMs = 0L,
            candidateCount = 0,
            emittedCount = 0,
            engineVersion = "undecided",
        )

        /** Used when the collection is healthy and freshly generated. */
        fun healthy(
            generationTime: Long,
            lastUpdate: Long,
            candidateCount: Int,
            emittedCount: Int,
            generationDurationMs: Long,
            engineVersion: String,
        ) = CollectionDiagnosticsContract(
            generationTime = generationTime,
            cacheStatus = DiagnosticCacheStatus.FRESH,
            refreshReason = DiagnosticRefreshReason.SCHEDULED,
            generationReason = DiagnosticGenerationReason.DEFAULT,
            lastUpdate = lastUpdate,
            collectionHealth = DiagnosticHealth.HEALTHY,
            warnings = emptyList(),
            developerFlags = DiagnosticDeveloperFlags.NONE,
            generationDurationMs = generationDurationMs,
            candidateCount = candidateCount,
            emittedCount = emittedCount,
            engineVersion = engineVersion,
        )

        /** Used when the engine encountered a problem. */
        fun error(
            generationTime: Long,
            message: String,
            engineVersion: String,
        ) = CollectionDiagnosticsContract(
            generationTime = generationTime,
            cacheStatus = DiagnosticCacheStatus.ERROR,
            refreshReason = DiagnosticRefreshReason.RECOVERY,
            generationReason = DiagnosticGenerationReason.DEFAULT,
            lastUpdate = generationTime,
            collectionHealth = DiagnosticHealth.ERROR,
            warnings = listOf(
                DiagnosticWarning(
                    code = "engine.error",
                    message = message,
                    severity = 3,
                    timestamp = generationTime,
                )
            ),
            developerFlags = DiagnosticDeveloperFlags.NONE,
            generationDurationMs = 0L,
            candidateCount = 0,
            emittedCount = 0,
            engineVersion = engineVersion,
        )
    }
}
