/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B4 — Library Rendering Runtime
 *
 * File 1 / 13 — LibraryRenderSnapshot.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * The Library Render Snapshot is the *final* visual contract the
 * UI (B5+ Compose layer) consumes. It is a pure, immutable, fully-
 * resolved description of *what to draw* — never *how to draw it*.
 *
 * Data flow:
 *
 *   CollectionConstitution      (B1 — frozen)
 *               │
 *               ▼
 *   CollectionEngine            (B3 — intelligence)
 *               │
 *               ▼
 *   CollectionRuntimeState      (B2 — live runtime, holds snapshots)
 *               │
 *               ▼
 *   LibraryRenderSnapshot       (B4 — this file — UI's ONLY input)
 *               │
 *               ▼
 *   Compose / Android UI        (B5+ — never touches anything above)
 *
 * STRATEGIC SUGGESTION (post-B3 lock):
 * -------------------------------------
 * "CollectionRuntimeSnapshot এটাই হবে UI-এর একমাত্র input।"
 * The UI must NEVER know about Strategy / Scorer / Ranking / Cache.
 * If the entire engine is rewritten, the UI must not change.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #29  Motion Explains Hierarchy   — motion hints travel with sections
 * #30  Empty State Has Emotion     — empty state is a first-class citizen
 * #31  Runtime Owns Truth          — UI reads this snapshot, nothing else
 * #44  Stable Key Never Changes    — all keys are stable strings
 * #45  Contracts Must Be Serializable — render model is also serializable-shaped
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Repository, Room, DAO, Coroutine, Flow, StateFlow, Compose, Canvas,
 * ViewModel. This is pure Kotlin data — no Android framework, no async.
 *
 * © RIKO Experience OS — Rendering Tier
 */

package com.rikodev.music.android.presentation.library.rendering.model

import com.rikodev.music.android.presentation.library.rendering.state.RenderVisualState
import androidx.compose.foundation.Canvas

/**
 * The single, authoritative visual contract for the Library UI.
 *
 * The renderer (B5+) reads ONLY this. It does not consult the engine,
 * the runtime, the contract, or the constitution directly. Every
 * visual decision is pre-resolved here.
 *
 * Why this matters: with a single resolved snapshot, the UI becomes
 * trivial to diff, animate, and snapshot-test. The rendering layer
 * is also the natural seam for cross-platform ports — a future iOS
 * renderer can consume the same `LibraryRenderSnapshot`.
 *
 * @property schemaVersion  Bumped when the render model shape changes
 *                          in a way that breaks older consumers.
 *                          Allows the UI to feature-gate gracefully.
 * @property state          Visual state — Ready / Loading / Empty / etc.
 *                          Drives the top-level screen shell.
 * @property sections       Ordered list of visible render sections.
 *                          Empty when state != READY.
 * @property header         Resolved header model (title, subtitle, motion).
 *                          Always present, even in loading state.
 * @property createdAt      Wall-clock time (epoch ms) when this snapshot
 *                          was produced. The UI uses this to discard
 *                          stale snapshots arriving out of order.
 * @property sourceFingerprint  Hash-style fingerprint of the source
 *                          [CollectionRuntimeState]. Used for change
 *                          detection without deep equality checks.
 * @property diagnostics    Lightweight render-layer diagnostics
 *                          (see RenderDiagnostics). Never affects
 *                          rendering (Rule #53).
 */
data class LibraryRenderSnapshot(
    val schemaVersion: Int,
    val state: RenderVisualState,
    val sections: List<RenderSection>,
    val header: RenderHeader,
    val createdAt: Long,
    val sourceFingerprint: String,
    val diagnostics: RenderDiagnosticsSummary,
) {
    companion object {
        /** Current render-model schema. Bump on breaking shape changes. */
        const val CURRENT_SCHEMA_VERSION: Int = 1

        /**
         * Cold-start empty snapshot — used before the renderer has run.
         * The UI must be able to handle this without crashing.
         */
        val COLD_START = LibraryRenderSnapshot(
            schemaVersion = CURRENT_SCHEMA_VERSION,
            state = RenderVisualState.Loading(
                reason = LoadingReason.COLD_START,
                estimatedProgress = 0f,
                visibleSectionsHint = 0,
            ),
            sections = emptyList(),
            header = RenderHeader.PLACEHOLDER,
            createdAt = 0L,
            sourceFingerprint = "",
            diagnostics = RenderDiagnosticsSummary.NONE,
        )
    }

    /** True if the snapshot is in a state where sections can be drawn. */
    val isReady: Boolean
        get() = state is RenderVisualState.Ready && sections.isNotEmpty()

    /** True if the snapshot represents a blocked state (error / permission). */
    val isBlocked: Boolean
        get() = state is RenderVisualState.Error || state is RenderVisualState.PermissionRequired

    /** True if the snapshot is empty (no collections, but no error). */
    val isEmpty: Boolean
        get() = state is RenderVisualState.Empty

    /** Total number of render items across all sections. */
    val totalItemCount: Int
        get() = sections.sumOf { it.items.size }

    /** Number of sections that are currently visible (not collapsed). */
    val visibleSectionCount: Int
        get() = sections.count { it.isVisible }

    /** Look up a section by its stable key. Rule #44. */
    fun sectionFor(stableKey: String): RenderSection? =
        sections.firstOrNull { it.stableKey == stableKey }
}

/**
 * Lightweight render-layer diagnostics summary.
 *
 * The full diagnostics live in [RenderDiagnostics] (see diagnostics/
 * subpackage). This summary is what travels with the snapshot — just
 * enough for an Inspector overlay to surface "why did this snapshot
 * take 80ms?" or "which section had the highest re-rank churn?".
 *
 * Rule #53 — Diagnostics Never Affects Engine.
 * This summary is for observation only. The renderer never branches
 * on these fields.
 */
data class RenderDiagnosticsSummary(
    /** Total time spent producing this snapshot, in milliseconds. */
    val renderDurationMs: Long,
    /** Number of sections that were re-used from the previous render. */
    val reusedSections: Int,
    /** Number of sections that were freshly computed. */
    val computedSections: Int,
    /** Number of items whose visual state changed since previous render. */
    val changedItems: Int,
    /** Number of items whose layout changed (e.g. card style swap). */
    val relayoutedItems: Int,
    /** Optional human-readable note for the Inspector overlay. */
    val note: String?,
) {
    companion object {
        val NONE = RenderDiagnosticsSummary(
            renderDurationMs = 0L,
            reusedSections = 0,
            computedSections = 0,
            changedItems = 0,
            relayoutedItems = 0,
            note = null,
        )
    }

    /** True if every section was reused (zero recompute). */
    val isFullyReused: Boolean get() = computedSections == 0 && reusedSections > 0
}

/**
 * Resolved header model — what the Library header should show.
 *
 * The header is special: it sits above all sections and is always
 * visible (except in fullscreen modes). It deserves its own resolved
 * model so the UI never has to peek into a [RenderSection] for it.
 *
 * @property title          Primary title text.
 * @property subtitle       Optional subtitle / contextual hint.
 * @property accentHex      Resolved accent color, hex string like "#FF8A65".
 *                          Derived from active theme + personality.
 * @property motionHint     How the header should animate on state change.
 * @property isVisible      False when the header is suppressed (e.g. fullscreen).
 */
data class RenderHeader(
    val title: String,
    val subtitle: String?,
    val accentHex: String,
    val motionHint: HeaderMotionHint,
    val isVisible: Boolean,
) {
    companion object {
        val PLACEHOLDER = RenderHeader(
            title = "",
            subtitle = null,
            accentHex = "#000000",
            motionHint = HeaderMotionHint.NONE,
            isVisible = false,
        )
    }
}

/**
 * Motion hint for the header — a coarse-grained gesture to the UI
 * about *how* the header should transition between states.
 *
 * Rule #29 — Motion Explains Hierarchy.
 * The header is the highest hierarchy element; its motion is the
 * reference for everything below.
 */
enum class HeaderMotionHint(internal val description: String) {
    /** No motion — initial render or no state change. */
    NONE("No motion."),

    /** Header enters from above — used on cold-start to ready transition. */
    ENTER_FROM_TOP("Header slides in from the top."),

    /** Header subtitle crossfades — used when context changes. */
    SUBTITLE_CROSSFADE("Subtitle crossfades; title stays stable."),

    /** Header accent color animates — used when personality changes. */
    ACCENT_SHIFT("Accent color animates to the new personality."),

    /** Header collapses into a compact form — used on scroll. */
    COLLAPSE("Header compresses to a compact sticky form."),

    /** Header expands back to full form — used when scroll returns to top. */
    EXPAND("Header expands back to full form."),
}

/**
 * Reason the Library is in a loading state.
 *
 * Each reason maps to a *different* loading UI — a cold start shows
 * a different skeleton than a refresh, which is different again from
 * a permission flow. The UI must not have to guess.
 */
enum class LoadingReason(internal val description: String) {
    /** First-ever render — no previous data available. */
    COLD_START("First render — no previous data."),

    /** User pulled to refresh — show inline refresh indicator. */
    PULL_TO_REFRESH("User-initiated refresh."),

    /** Engine signaled an invalidation — quiet refresh in background. */
    ENGINE_INVALIDATION("Engine invalidated cache; background refresh."),

    /** Context changed (e.g. time-of-day) — partial refresh. */
    CONTEXT_CHANGE("Context change — partial refresh."),

    /** Permission just granted — re-hydrating from scratch. */
    PERMISSION_GRANTED("Permission granted — full hydration."),
}

/**
 * Reason the Library is in an error state.
 *
 * Errors are not just "something broke". Each error class has a
 * distinct recovery path the UI can offer the user.
 */
enum class ErrorReason(internal val description: String) {
    /** Engine threw an unrecoverable exception. */
    ENGINE_FAILURE("Engine failed to produce a snapshot."),

    /** Underlying data source (Room / MediaStore) is unreadable. */
    DATA_SOURCE_UNAVAILABLE("Data source unavailable."),

    /** Snapshot arrived but failed schema validation. */
    SCHEMA_MISMATCH("Snapshot schema mismatch — UI is older than runtime."),

    /** Unknown / unexpected error. */
    UNKNOWN("Unknown error."),
}
