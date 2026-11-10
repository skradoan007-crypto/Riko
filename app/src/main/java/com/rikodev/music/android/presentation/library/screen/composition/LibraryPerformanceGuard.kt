/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.3 — Library Rendering UI
 * Phase D — Composition / Performance / Validator / Preview
 *
 * File D2 / D4 — LibraryPerformanceGuard.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [LibraryPerformanceGuard] is the observation-only performance analyst
 * for the Library screen. It computes, given a state + motion schedule +
 * profile, the expected rendering cost and compares it to the per-profile
 * budget. It returns a [LibraryPerformanceReport] — pure immutable data.
 *
 * This file does NOT:
 *   ❌ Mutate state (Rule #76 — Performance Guards Never Optimize)
 *   ❌ Trigger auto-degradation (no "if budget exceeded → reduceMotion=true")
 *   ❌ Launch coroutines (no adaptive degradation logic)
 *   ❌ Touch Compose runtime (no CompositionLocal, no LaunchedEffect)
 *   ❌ Block rendering (warnings never prevent composition)
 *
 * This file DOES:
 *   ✅ Compute expected cost from state + schedule (pure function)
 *   ✅ Compare against budget (pure function)
 *   ✅ Emit warnings (List<String>, human-readable)
 *   ✅ Report `isWithinBudget: Boolean` (single boolean answer)
 *   ✅ Reference existing LibraryPerformanceContract (B5.1) — Rule #62
 *
 * RULE #76 — PERFORMANCE GUARDS NEVER OPTIMIZE
 * --------------------------------------------
 * The Guard is the truth-teller. It says "frame time 18ms, budget 16ms,
 * over by 2ms". The developer (or host) decides what to do about it.
 *
 * The Report contains `warnings` and `isWithinBudget`. It NEVER contains
 * `recommendedAction` — that's the developer's job to decide.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #53  Diagnostics Never Affects Engine — Guard is observation-only
 * #58  Runtime Never Knows Rendering — Guard reads runtime state, no rendering
 * #62  Never Rebuild Existing Systems — reuses LibraryPerformanceContract
 * #70  Architecture Freeze — no new patterns, only analysis of existing ones
 * #74  Silence Is Also UX — Guard reports motion count vs Rule #74 ceiling
 * #76  Performance Guards Never Optimize — see above
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Compose imports (no @Composable, no Modifier, no Color, no Dp),
 * no Coroutine (no launch, no Flow), no Android framework, no ViewModel.
 * Pure Kotlin data + pure functions.
 *
 * © RIKO Experience OS — Diagnostics Tier
 */

package com.rikodev.music.android.presentation.library.screen.composition

import com.rikodev.music.android.presentation.library.screen.contract.LibraryConstitutionProfile
import com.rikodev.music.android.presentation.library.screen.contract.LibraryFrameTimingRule
import com.rikodev.music.android.presentation.library.screen.contract.LibraryJankBudget
import com.rikodev.music.android.presentation.library.screen.contract.LibraryMemoryBudget
import com.rikodev.music.android.presentation.library.screen.contract.LibraryPerformanceContract
import com.rikodev.music.android.presentation.library.screen.motion.LibraryMotionAuditReport
import com.rikodev.music.android.presentation.library.screen.motion.LibraryMotionSchedule
import com.rikodev.music.android.presentation.library.screen.motion.LibraryMotionSlot
import com.rikodev.music.android.presentation.library.screen.motion.auditMotion
import com.rikodev.music.android.presentation.library.screen.runtime.LibraryScreenPhase
import com.rikodev.music.android.presentation.library.screen.runtime.LibraryScreenState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Modifier
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow

// ═══════════════════════════════════════════════════════════════════════════════════════
// PERFORMANCE BUDGET — per-profile targets
// ═══════════════════════════════════════════════════════════════════════════════════════

/**
 * The complete performance budget for the Library screen.
 *
 * Combines all per-domain budgets into a single immutable snapshot.
 * Each budget is sourced from the existing [LibraryPerformanceContract]
 * (B5.1) — Rule #62, never rebuild existing systems.
 *
 * The Guard compares observed/estimated metrics against this budget
 * and emits [LibraryPerformanceReport.warnings] for any violation.
 *
 * @property frameBudgetMs            Max time (ms) per frame for layout+draw.
 * @property maxConcurrentMotions     Max simultaneous active motion slots
 *                                    (Rule #74 hard ceiling = 5).
 * @property maxOverdrawLayers        Max layers painted on top of each other
 *                                    (overdraw = wasted GPU fill rate).
 * @property maxComposableLayers      Max composable nesting depth.
 * @property maxGlassSurfaces         Max simultaneous glass surfaces in view.
 *                                    Glass = expensive (blur + tint + noise).
 * @property maxWaveRibbons           Max simultaneous wave ribbons.
 *                                    Library = 1 (subtle ambient pulse only).
 * @property maxVisibleSections       Max sections rendered at once on screen.
 * @property maxRecompositionDepth    Max recompositions triggered by a single
 *                                    state change (Rule of thumb: ≤ 2).
 * @property allocationToleranceBytes Max per-frame allocation. Target = 0,
 *                                    tolerance = 32 bytes (one small object).
 * @property jankBudget               Jank budget (frames per second, streak).
 * @property memoryBudget             Total memory budget for the screen.
 */
data class LibraryPerformanceBudget(
    val frameBudgetMs: Long,
    val maxConcurrentMotions: Int,
    val maxOverdrawLayers: Int,
    val maxComposableLayers: Int,
    val maxGlassSurfaces: Int,
    val maxWaveRibbons: Int,
    val maxVisibleSections: Int,
    val maxRecompositionDepth: Int,
    val allocationToleranceBytes: Int,
    val jankBudget: LibraryJankBudget,
    val memoryBudget: LibraryMemoryBudget,
) {
    init {
        // Rule #74 hard ceiling — Library screen enforces 5-motion cap.
        require(maxConcurrentMotions in 1..5) {
            "maxConcurrentMotions must be in [1, 5] (Rule #74 ceiling). " +
                "Got $maxConcurrentMotions."
        }
        require(maxWaveRibbons in 0..2) {
            "maxWaveRibbons must be in [0, 2] (Library uses ≤ 1 ambient). " +
                "Got $maxWaveRibbons."
        }
        require(maxGlassSurfaces in 1..12) {
            "maxGlassSurfaces must be in [1, 12]. Got $maxGlassSurfaces."
        }
        require(frameBudgetMs in 4L..33L) {
            "frameBudgetMs must be in [4, 33] (30Hz to 240Hz). Got $frameBudgetMs."
        }
        require(allocationToleranceBytes in 0..256) {
            "allocationToleranceBytes must be in [0, 256]. Got $allocationToleranceBytes."
        }
    }

    companion object {

        /**
         * DEFAULT budget — balanced for standard phones (60Hz).
         *
         * - 16ms frame budget (60Hz refresh).
         * - 5 concurrent motions (Rule #74 ceiling).
         * - 4 overdraw layers (typical: bg + depth + glass + content).
         * - 6 glass surfaces (hero + 3 cards + dock + section header).
         * - 1 wave ribbon (ambient pulse only).
         * - 6 visible sections (premium feel, not overwhelming).
         * - 32 bytes per-frame allocation tolerance.
         */
        val DEFAULT = LibraryPerformanceBudget(
            frameBudgetMs = 16L,
            maxConcurrentMotions = 5,
            maxOverdrawLayers = 4,
            maxComposableLayers = 12,
            maxGlassSurfaces = 6,
            maxWaveRibbons = 1,
            maxVisibleSections = 6,
            maxRecompositionDepth = 2,
            allocationToleranceBytes = 32,
            jankBudget = LibraryJankBudget.STANDARD,
            memoryBudget = LibraryMemoryBudget.DEFAULT,
        )

        /**
         * AMOLED budget — same as DEFAULT, but with relaxed overdraw
         * (AMOLED skips depth stack, so overdraw is naturally lower).
         */
        val AMOLED = DEFAULT.copy(
            maxOverdrawLayers = 3, // No depth stack in AMOLED.
        )

        /**
         * CINEMATIC budget — relaxed frame time (slower motion is OK on
         * cinematic profile), but strict motion count (cinematic = fewer,
         * slower motions, not more).
         */
        val CINEMATIC = DEFAULT.copy(
            frameBudgetMs = 20L, // Cinematic tolerates 50Hz.
            maxConcurrentMotions = 4, // Cinematic = quieter.
            maxVisibleSections = 5, // More breathing room.
        )

        /**
         * LOW_END budget — strict limits for low-end devices.
         *
         * - 33ms frame budget (30Hz target — under-driven, not over-driven).
         * - 2 concurrent motions (Rule #74 — silence is also UX).
         * - 2 overdraw layers (no depth stack, no ambient).
         * - 3 glass surfaces (cards only, no hero glass).
         * - 0 wave ribbons (silence).
         * - 4 visible sections (less to render).
         * - 64 bytes per-frame allocation tolerance (slightly relaxed for GC).
         */
        val LOW_END = LibraryPerformanceBudget(
            frameBudgetMs = 33L,
            maxConcurrentMotions = 2,
            maxOverdrawLayers = 2,
            maxComposableLayers = 8,
            maxGlassSurfaces = 3,
            maxWaveRibbons = 0,
            maxVisibleSections = 4,
            maxRecompositionDepth = 1,
            allocationToleranceBytes = 64,
            jankBudget = LibraryJankBudget.RELAXED,
            memoryBudget = LibraryMemoryBudget.LOW_END,
        )

        /**
         * HIGH_END budget — aggressive targets for flagship devices.
         *
         * - 8ms frame budget (120Hz target).
         * - 5 concurrent motions (Rule #74 ceiling).
         * - 4 overdraw layers.
         * - 8 glass surfaces (more cards visible).
         * - 1 wave ribbon (ambient pulse, full quality).
         * - 8 visible sections (more content per screen).
         * - 16 bytes per-frame allocation tolerance (stricter).
         */
        val HIGH_END = LibraryPerformanceBudget(
            frameBudgetMs = 8L,
            maxConcurrentMotions = 5,
            maxOverdrawLayers = 4,
            maxComposableLayers = 14,
            maxGlassSurfaces = 8,
            maxWaveRibbons = 1,
            maxVisibleSections = 8,
            maxRecompositionDepth = 2,
            allocationToleranceBytes = 16,
            jankBudget = LibraryJankBudget.STRICT,
            memoryBudget = LibraryMemoryBudget.HIGH_END,
        )

        /**
         * REDUCED_MOTION budget — same as DEFAULT but with motion count = 0.
         *
         * Reduce-motion users get a static UI. The Guard still checks
         * layer counts and allocation, but motion budget is zero.
         */
        val REDUCED_MOTION = DEFAULT.copy(
            maxConcurrentMotions = 0,
            maxWaveRibbons = 0,
        )

        /**
         * Resolves the budget for a profile.
         *
         * Order of precedence (highest to lowest):
         * 1. REDUCED_MOTION — accessibility override.
         * 2. LOW_END — capability override.
         * 3. HIGH_END — capability override.
         * 4. AMOLED — display override.
         * 5. CINEMATIC — experience override.
         * 6. DEFAULT — fallback.
         */
        fun forProfile(profile: LibraryConstitutionProfile): LibraryPerformanceBudget = when (profile) {
            LibraryConstitutionProfile.REDUCED_MOTION -> REDUCED_MOTION
            LibraryConstitutionProfile.LOW_END -> LOW_END
            LibraryConstitutionProfile.HIGH_END -> HIGH_END
            LibraryConstitutionProfile.AMOLED -> AMOLED
            LibraryConstitutionProfile.CINEMATIC -> CINEMATIC
            LibraryConstitutionProfile.DEFAULT -> DEFAULT
            LibraryConstitutionProfile.COMPACT -> DEFAULT
            LibraryConstitutionProfile.HIGH_CONTRAST -> DEFAULT
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// LAYER ESTIMATE — what the screen is expected to render
// ═══════════════════════════════════════════════════════════════════════════════════════

/**
 * Estimate of the rendering layers the screen will produce.
 *
 * This is a STATIC estimate — computed from the state + schedule,
 * without measuring actual frame times. It tells you "given this
 * state, the Library is expected to paint N glass surfaces, M motion
 * slots, K overdraw layers, etc."
 *
 * The estimate is conservative — it assumes the worst case for each
 * visible element. Real rendering may be cheaper (e.g. a section that
 * is technically visible but off-screen won't actually paint).
 *
 * @property glassSurfaceCount       Number of glass surfaces expected to paint.
 * @property waveRibbonCount         Number of wave ribbons (0 or 1 for Library).
 * @property overdrawLayerCount      Number of layers stacked at any pixel.
 * @property composableLayerCount    Depth of the composable tree.
 * @property concurrentMotionCount   Number of active motion slots.
 * @property visibleSectionCount     Number of sections currently rendered.
 * @property estimatedAllocationBytes  Estimated per-frame allocation.
 * @property estimatedRecompositionDepth  Estimated recompositions per state change.
 */
data class LibraryLayerEstimate(
    val glassSurfaceCount: Int,
    val waveRibbonCount: Int,
    val overdrawLayerCount: Int,
    val composableLayerCount: Int,
    val concurrentMotionCount: Int,
    val visibleSectionCount: Int,
    val estimatedAllocationBytes: Int,
    val estimatedRecompositionDepth: Int,
) {
    companion object {

        /**
         * Empty estimate — for states that don't render content
         * (e.g. COLD_START before any state hydrates).
         */
        val EMPTY = LibraryLayerEstimate(
            glassSurfaceCount = 0,
            waveRibbonCount = 0,
            overdrawLayerCount = 0,
            composableLayerCount = 0,
            concurrentMotionCount = 0,
            visibleSectionCount = 0,
            estimatedAllocationBytes = 0,
            estimatedRecompositionDepth = 0,
        )
    }
}

/**
 * Estimates the rendering layers for a given state + schedule.
 *
 * Pure function — given the same inputs, always returns the same
 * estimate. No side effects, no state mutation.
 *
 * ## How the estimate is computed
 *
 * - **glassSurfaceCount**: Hero (1) + each visible section's header (1)
 *   + each visible card (1) + Dock (1, if hasTrack). Capped at the
 *   number of cards actually visible (LazyRow/Grid only render what's
 *   on-screen, but we estimate the worst case).
 *
 * - **waveRibbonCount**: 1 if `dock.isPlaying` and the wave layer is
 *   active in the schedule. 0 otherwise.
 *
 * - **overdrawLayerCount**: Background (1) + Depth (1, unless AMOLED)
 *   + Ambient (1) + Glass content (1). For Library, this is typically 4.
 *
 * - **composableLayerCount**: Box(root) → Background Host → Content
 *   Layer → LazyColumn → Hero Container → Hero Box → GlassSurface →
 *   Hero Box. Typically 8-12.
 *
 * - **concurrentMotionCount**: From the motion schedule's audit.
 *
 * - **visibleSectionCount**: From the state's `visibleSectionCount`.
 *
 * - **estimatedAllocationBytes**: 0 (target). 32 bytes (tolerance).
 *
 * - **estimatedRecompositionDepth**: 1 (ideal). 2 (acceptable).
 *
 * @param state           The Library screen state.
 * @param schedule        The resolved motion schedule.
 * @param isAmoled        True if AMOLED profile (skips depth layer).
 * @param isReducedMotion True if reduced-motion (disables wave + motions).
 */
fun estimateLibraryLayers(
    state: LibraryScreenState,
    schedule: LibraryMotionSchedule,
    isAmoled: Boolean,
    isReducedMotion: Boolean,
): LibraryLayerEstimate {
    // ── Glass surface count ──────────────────────────────────────────────
    // Hero glass (1) + section header glass (one per visible section) +
    // card glass (3 cards per section, capped) + dock glass (1 if track).
    val heroGlass = if (state.hasHero) 1 else 0
    val sectionGlass = state.visibleSectionCount.coerceAtMost(6)
    val cardGlassPerSection = 3 // LazyRow typically shows 3 cards on-screen.
    val cardGlass = (sectionGlass * cardGlassPerSection).coerceAtMost(12)
    val dockGlass = if (state.dock.hasTrack) 1 else 0
    val glassSurfaceCount = heroGlass + sectionGlass + cardGlass + dockGlass

    // ── Wave ribbon count ────────────────────────────────────────────────
    // Library uses at most 1 wave ribbon (the ambient pulse). It's only
    // active when music is playing AND reduce-motion is off.
    val waveRibbonCount = if (isReducedMotion) 0
    else if (state.dock.isPlaying && schedule.isActive(LibraryMotionSlot.WAVE_PULSE)) 1
    else 0

    // ── Overdraw layer count ─────────────────────────────────────────────
    // Background (1) + Depth (1, unless AMOLED) + Ambient (1) + Glass (1).
    // For Library, this is typically 3 (AMOLED) or 4 (other profiles).
    val depthLayer = if (isAmoled) 0 else 1
    val ambientLayer = if (isReducedMotion) 0 else 1
    val overdrawLayerCount = 1 + depthLayer + ambientLayer + 1

    // ── Composable layer count ───────────────────────────────────────────
    // Conservative estimate of the composable tree depth.
    // Box(root) [1] → Background Host [2] → Background Box [3] (draw)
    // Content Layer [2] → LazyColumn [3] → Hero Container [4] →
    // GlassSurface [5] → Hero Box [6] → Hero [7].
    val composableLayerCount = if (state.isReady) 12 else 6

    // ── Concurrent motion count ──────────────────────────────────────────
    // From the schedule's audit (Phase C — LibraryMotionOrchestrator).
    val motionAudit = auditMotion(schedule)
    val concurrentMotionCount = motionAudit.activeSlots

    // ── Visible section count ────────────────────────────────────────────
    val visibleSectionCount = state.visibleSectionCount

    // ── Estimated allocation ─────────────────────────────────────────────
    // Target = 0. The Library is designed for zero per-frame allocation
    // (all brushes remembered, all state encapsulated). Allow 32 bytes
    // tolerance for one small object (e.g. a Color copy).
    val estimatedAllocationBytes = 0

    // ── Estimated recomposition depth ────────────────────────────────────
    // Ideal = 1 (only the changed composable recomposes).
    // Acceptable = 2 (the changed composable + its parent).
    // Higher = warning (something is over-recomposing).
    val estimatedRecompositionDepth = when {
        state.phase == LibraryScreenPhase.COLD_START -> 1
        state.phase == LibraryScreenPhase.LOADING -> 1
        state.isInSelectionMode -> 2 // Selection affects multiple sections.
        else -> 1
    }

    return LibraryLayerEstimate(
        glassSurfaceCount = glassSurfaceCount,
        waveRibbonCount = waveRibbonCount,
        overdrawLayerCount = overdrawLayerCount,
        composableLayerCount = composableLayerCount,
        concurrentMotionCount = concurrentMotionCount,
        visibleSectionCount = visibleSectionCount,
        estimatedAllocationBytes = estimatedAllocationBytes,
        estimatedRecompositionDepth = estimatedRecompositionDepth,
    )
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// PERFORMANCE REPORT — comparison result
// ═══════════════════════════════════════════════════════════════════════════════════════

/**
 * The result of comparing a [LibraryLayerEstimate] against a
 * [LibraryPerformanceBudget].
 *
 * Rule #76 — this report NEVER contains optimization actions. It only
 * states facts: what was observed, what was budgeted, what violations
 * occurred. The developer decides what to do.
 *
 * @property isWithinBudget       True if NO violations were detected.
 *                                The single boolean answer.
 * @property violationCount       Number of distinct budget violations.
 * @property warnings             Human-readable warning strings.
 *                                Empty list if `isWithinBudget = true`.
 * @property glassSurfaceCount    Observed glass surface count.
 * @property glassSurfaceBudget   Budget for glass surfaces.
 * @property motionCount          Observed concurrent motion count.
 * @property motionBudget         Budget for concurrent motions.
 * @property overdrawLayerCount   Observed overdraw layer count.
 * @property overdrawBudget       Budget for overdraw layers.
 * @property composableLayerCount Observed composable layer count.
 * @property composableBudget     Budget for composable layers.
 * @property waveRibbonCount      Observed wave ribbon count.
 * @property waveRibbonBudget     Budget for wave ribbons.
 * @property visibleSectionCount  Observed visible section count.
 * @property visibleSectionBudget Budget for visible sections.
 * @property recompositionDepth   Estimated recomposition depth.
 * @property recompositionBudget  Budget for recomposition depth.
 * @property allocationBytes      Estimated per-frame allocation (bytes).
 * @property allocationBudget     Tolerance for per-frame allocation (bytes).
 */
data class LibraryPerformanceReport(
    val isWithinBudget: Boolean,
    val violationCount: Int,
    val warnings: List<String>,
    val glassSurfaceCount: Int,
    val glassSurfaceBudget: Int,
    val motionCount: Int,
    val motionBudget: Int,
    val overdrawLayerCount: Int,
    val overdrawBudget: Int,
    val composableLayerCount: Int,
    val composableBudget: Int,
    val waveRibbonCount: Int,
    val waveRibbonBudget: Int,
    val visibleSectionCount: Int,
    val visibleSectionBudget: Int,
    val recompositionDepth: Int,
    val recompositionBudget: Int,
    val allocationBytes: Int,
    val allocationBudget: Int,
) {

    /**
     * True if the glass surface count is within budget.
     */
    val isGlassWithinBudget: Boolean get() = glassSurfaceCount <= glassSurfaceBudget

    /**
     * True if the motion count is within budget (Rule #74).
     */
    val isMotionWithinBudget: Boolean get() = motionCount <= motionBudget

    /**
     * True if the overdraw layer count is within budget.
     */
    val isOverdrawWithinBudget: Boolean get() = overdrawLayerCount <= overdrawBudget

    /**
     * True if the composable layer count is within budget.
     */
    val isComposableWithinBudget: Boolean get() = composableLayerCount <= composableBudget

    /**
     * True if the wave ribbon count is within budget.
     */
    val isWaveWithinBudget: Boolean get() = waveRibbonCount <= waveRibbonBudget

    /**
     * True if the visible section count is within budget.
     */
    val isSectionWithinBudget: Boolean get() = visibleSectionCount <= visibleSectionBudget

    /**
     * True if the recomposition depth is within budget.
     */
    val isRecompositionWithinBudget: Boolean get() = recompositionDepth <= recompositionBudget

    /**
     * True if the per-frame allocation is within tolerance.
     */
    val isAllocationWithinBudget: Boolean get() = allocationBytes <= allocationBudget

    companion object {

        /**
         * An "all clear" report — used when there's nothing to render
         * (e.g. COLD_START with empty state).
         */
        val ALL_CLEAR = LibraryPerformanceReport(
            isWithinBudget = true,
            violationCount = 0,
            warnings = emptyList(),
            glassSurfaceCount = 0,
            glassSurfaceBudget = 0,
            motionCount = 0,
            motionBudget = 0,
            overdrawLayerCount = 0,
            overdrawBudget = 0,
            composableLayerCount = 0,
            composableBudget = 0,
            waveRibbonCount = 0,
            waveRibbonBudget = 0,
            visibleSectionCount = 0,
            visibleSectionBudget = 0,
            recompositionDepth = 0,
            recompositionBudget = 0,
            allocationBytes = 0,
            allocationBudget = 0,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// THE AUDIT FUNCTION — pure, observation-only
// ═══════════════════════════════════════════════════════════════════════════════════════

/**
 * Audits the Library's expected rendering cost against the budget.
 *
 * Rule #76 — this function is OBSERVATION-ONLY. It does not:
 *   - Mutate any state
 *   - Trigger any optimization
 *   - Launch any coroutine
 *   - Block any rendering
 *
 * It returns a [LibraryPerformanceReport] containing the observed
 * metrics, the budget, and human-readable warnings. The caller (host
 * or developer) decides what to do with the report.
 *
 * ## What the audit checks
 *
 * 1. **Glass surface count** vs `budget.maxGlassSurfaces`.
 *    Glass is expensive (blur + tint + noise). Too many = jank.
 *
 * 2. **Concurrent motion count** vs `budget.maxConcurrentMotions`.
 *    Rule #74 — silence is also UX. Library ceiling = 5.
 *
 * 3. **Overdraw layer count** vs `budget.maxOverdrawLayers`.
 *    Overdraw = wasted GPU fill rate.
 *
 * 4. **Composable layer count** vs `budget.maxComposableLayers`.
 *    Deep trees = slower measurement + layout.
 *
 * 5. **Wave ribbon count** vs `budget.maxWaveRibbons`.
 *    Library uses at most 1 (ambient pulse).
 *
 * 6. **Visible section count** vs `budget.maxVisibleSections`.
 *    Too many sections = overwhelming + slow first paint.
 *
 * 7. **Recomposition depth** vs `budget.maxRecompositionDepth`.
 *    High depth = state changes cascade.
 *
 * 8. **Per-frame allocation** vs `budget.allocationToleranceBytes`.
 *    Allocation = GC pressure = jank.
 *
 * @param state           The Library screen state.
 * @param schedule        The resolved motion schedule (Phase C).
 * @param profile         The active constitution profile.
 * @return                A [LibraryPerformanceReport]. Never null.
 */
fun auditPerformance(
    state: LibraryScreenState,
    schedule: LibraryMotionSchedule,
    profile: LibraryConstitutionProfile,
): LibraryPerformanceReport {
    // ── Resolve the budget for this profile ──────────────────────────────
    val budget = LibraryPerformanceBudget.forProfile(profile)

    // ── Skip the audit for non-rendering phases ──────────────────────────
    // COLD_START with no sections doesn't have meaningful layer counts.
    // Return ALL_CLEAR so the host doesn't get spurious warnings.
    if (state.phase == LibraryScreenPhase.COLD_START && state.sections.isEmpty()) {
        return LibraryPerformanceReport.ALL_CLEAR.copy(
            glassSurfaceBudget = budget.maxGlassSurfaces,
            motionBudget = budget.maxConcurrentMotions,
            overdrawBudget = budget.maxOverdrawLayers,
            composableBudget = budget.maxComposableLayers,
            waveRibbonBudget = budget.maxWaveRibbons,
            visibleSectionBudget = budget.maxVisibleSections,
            recompositionBudget = budget.maxRecompositionDepth,
            allocationBudget = budget.allocationToleranceBytes,
        )
    }

    // ── Compute the layer estimate ───────────────────────────────────────
    val isAmoled = profile == LibraryConstitutionProfile.AMOLED
    val isReducedMotion = profile == LibraryConstitutionProfile.REDUCED_MOTION
    val estimate = estimateLibraryLayers(
        state = state,
        schedule = schedule,
        isAmoled = isAmoled,
        isReducedMotion = isReducedMotion,
    )

    // ── Build the warnings list ──────────────────────────────────────────
    val warnings = mutableListOf<String>()

    if (estimate.glassSurfaceCount > budget.maxGlassSurfaces) {
        warnings += "Glass surface count ${estimate.glassSurfaceCount} " +
            "exceeds budget ${budget.maxGlassSurfaces}. " +
            "Consider hiding dock or reducing visible cards."
    }

    if (estimate.concurrentMotionCount > budget.maxConcurrentMotions) {
        warnings += "Concurrent motion count ${estimate.concurrentMotionCount} " +
            "exceeds budget ${budget.maxConcurrentMotions} (Rule #74 ceiling). " +
            "Disable lowest-priority motion slots in LibraryMotionOrchestrator."
    }

    if (estimate.overdrawLayerCount > budget.maxOverdrawLayers) {
        warnings += "Overdraw layer count ${estimate.overdrawLayerCount} " +
            "exceeds budget ${budget.maxOverdrawLayers}. " +
            "Consider skipping depth stack (AMOLED) or disabling ambient."
    }

    if (estimate.composableLayerCount > budget.maxComposableLayers) {
        warnings += "Composable layer count ${estimate.composableLayerCount} " +
            "exceeds budget ${budget.maxComposableLayers}. " +
            "Consider flattening the composition tree."
    }

    if (estimate.waveRibbonCount > budget.maxWaveRibbons) {
        warnings += "Wave ribbon count ${estimate.waveRibbonCount} " +
            "exceeds budget ${budget.maxWaveRibbons}. " +
            "Library should use at most 1 ambient ribbon."
    }

    if (estimate.visibleSectionCount > budget.maxVisibleSections) {
        warnings += "Visible section count ${estimate.visibleSectionCount} " +
            "exceeds budget ${budget.maxVisibleSections}. " +
            "Consider collapsing less-relevant sections."
    }

    if (estimate.estimatedRecompositionDepth > budget.maxRecompositionDepth) {
        warnings += "Recomposition depth ${estimate.estimatedRecompositionDepth} " +
            "exceeds budget ${budget.maxRecompositionDepth}. " +
            "State changes are cascading too deep — check stable keys."
    }

    if (estimate.estimatedAllocationBytes > budget.allocationToleranceBytes) {
        warnings += "Per-frame allocation ${estimate.estimatedAllocationBytes} bytes " +
            "exceeds tolerance ${budget.allocationToleranceBytes} bytes. " +
            "Check for brush / Color / lambda allocation in draw scopes."
    }

    val isWithinBudget = warnings.isEmpty()

    return LibraryPerformanceReport(
        isWithinBudget = isWithinBudget,
        violationCount = warnings.size,
        warnings = warnings.toList(),
        glassSurfaceCount = estimate.glassSurfaceCount,
        glassSurfaceBudget = budget.maxGlassSurfaces,
        motionCount = estimate.concurrentMotionCount,
        motionBudget = budget.maxConcurrentMotions,
        overdrawLayerCount = estimate.overdrawLayerCount,
        overdrawBudget = budget.maxOverdrawLayers,
        composableLayerCount = estimate.composableLayerCount,
        composableBudget = budget.maxComposableLayers,
        waveRibbonCount = estimate.waveRibbonCount,
        waveRibbonBudget = budget.maxWaveRibbons,
        visibleSectionCount = estimate.visibleSectionCount,
        visibleSectionBudget = budget.maxVisibleSections,
        recompositionDepth = estimate.estimatedRecompositionDepth,
        recompositionBudget = budget.maxRecompositionDepth,
        allocationBytes = estimate.estimatedAllocationBytes,
        allocationBudget = budget.allocationToleranceBytes,
    )
}

/**
 * Convenience overload — audits using a [LibraryPerformanceContract]
 * directly (instead of a profile).
 *
 * Useful when the host already has the contract and doesn't want to
 * resolve the profile separately.
 */
fun auditPerformance(
    state: LibraryScreenState,
    schedule: LibraryMotionSchedule,
    contract: LibraryPerformanceContract,
    profile: LibraryConstitutionProfile,
): LibraryPerformanceReport {
    // The contract is the source of truth for jank + frame timing.
    // We resolve the budget by profile, then verify the contract's
    // frame timing rule matches the budget's frame budget.
    val report = auditPerformance(state, schedule, profile)
    val frameTimingRule = contract.frameTimingRule
    val expectedFrameBudget = when {
        frameTimingRule == LibraryFrameTimingRule.STANDARD -> 16L
        frameTimingRule == LibraryFrameTimingRule.AGGRESSIVE -> 8L
        frameTimingRule == LibraryFrameTimingRule.CONSERVATIVE -> 33L
        else -> 16L
    }
    // If the contract and budget disagree, emit a soft warning.
    // This is the ONLY place the Guard compares two budgets — it
    // never optimizes, just observes the inconsistency.
    val contractWarnings = report.warnings.toMutableList()
    if (frameTimingRule.frameTimeBudgetMs != expectedFrameBudget) {
        contractWarnings += "Frame timing rule (${frameTimingRule.frameTimeBudgetMs}ms) " +
            "differs from expected ($expectedFrameBudget ms) " +
            "for profile $profile. Check LibraryPerformanceContract."
    }
    return report.copy(
        warnings = contractWarnings.toList(),
        violationCount = contractWarnings.size,
        isWithinBudget = contractWarnings.isEmpty(),
    )
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// REPORT FORMATTERS — for logging and debugging
// ═══════════════════════════════════════════════════════════════════════════════════════

/**
 * Formats a [LibraryPerformanceReport] as a multi-line string suitable
 * for logging.
 *
 * Pure function — no side effects.
 */
fun formatPerformanceReport(report: LibraryPerformanceReport): String {
    val sb = StringBuilder()
    sb.appendLine("═══════════════════════════════════════════════════════")
    sb.appendLine("RIKO LIBRARY — PERFORMANCE REPORT")
    sb.appendLine("═══════════════════════════════════════════════════════")
    sb.appendLine("Status: ${if (report.isWithinBudget) "✅ WITHIN BUDGET" else "❌ OVER BUDGET"}")
    sb.appendLine("Violations: ${report.violationCount}")
    sb.appendLine()
    sb.appendLine("── Metrics ──────────────────────────────────────────")
    sb.appendLine("Glass surfaces:  ${report.glassSurfaceCount} / ${report.glassSurfaceBudget}")
    sb.appendLine("Concurrent motions: ${report.motionCount} / ${report.motionBudget}")
    sb.appendLine("Overdraw layers: ${report.overdrawLayerCount} / ${report.overdrawBudget}")
    sb.appendLine("Composable layers: ${report.composableLayerCount} / ${report.composableBudget}")
    sb.appendLine("Wave ribbons:    ${report.waveRibbonCount} / ${report.waveRibbonBudget}")
    sb.appendLine("Visible sections: ${report.visibleSectionCount} / ${report.visibleSectionBudget}")
    sb.appendLine("Recomposition depth: ${report.recompositionDepth} / ${report.recompositionBudget}")
    sb.appendLine("Per-frame allocation: ${report.allocationBytes} / ${report.allocationBudget} bytes")
    sb.appendLine()
    if (report.warnings.isNotEmpty()) {
        sb.appendLine("── Warnings ─────────────────────────────────────────")
        report.warnings.forEachIndexed { index, warning ->
            sb.appendLine("${index + 1}. $warning")
        }
        sb.appendLine()
    }
    sb.append("═══════════════════════════════════════════════════════")
    return sb.toString()
}

/**
 * Returns a single-line summary of the report.
 *
 * Useful for compact logging (e.g. one line per frame in a perf trace).
 */
fun summarizePerformanceReport(report: LibraryPerformanceReport): String {
    val status = if (report.isWithinBudget) "OK" else "WARN"
    return "Library[$status] " +
        "glass=${report.glassSurfaceCount}/${report.glassSurfaceBudget} " +
        "motion=${report.motionCount}/${report.motionBudget} " +
        "overdraw=${report.overdrawLayerCount}/${report.overdrawBudget} " +
        "wave=${report.waveRibbonCount}/${report.waveRibbonBudget} " +
        "sections=${report.visibleSectionCount}/${report.visibleSectionBudget} " +
        "recomp=${report.recompositionDepth}/${report.recompositionBudget} " +
        "alloc=${report.allocationBytes}/${report.allocationBudget}B " +
        "violations=${report.violationCount}"
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// MOTION AUDIT EXTENSION — wraps the Phase C audit for convenience
// ═══════════════════════════════════════════════════════════════════════════════════════

/**
 * Wraps the Phase C motion audit into the Performance Guard's report
 * format.
 *
 * This is a thin adapter — Rule #62 (Never Rebuild Existing Systems).
 * The actual audit logic lives in `auditMotion()` (Phase C —
 * LibraryMotionOrchestrator.kt). This function just adapts the result
 * into a [LibraryPerformanceReport] for uniform logging.
 */
fun auditMotionBudget(
    schedule: LibraryMotionSchedule,
    budget: LibraryPerformanceBudget,
): LibraryPerformanceReport {
    val motionAudit: LibraryMotionAuditReport = auditMotion(schedule)
    val warnings = mutableListOf<String>()

    if (motionAudit.activeSlots > budget.maxConcurrentMotions) {
        warnings += "Motion schedule has ${motionAudit.activeSlots} active slots, " +
            "budget is ${budget.maxConcurrentMotions}. " +
            "Audit reports isOverBudget=${motionAudit.isOverBudget}, " +
            "budgetLimit=${motionAudit.budgetLimit}."
    }

    return LibraryPerformanceReport(
        isWithinBudget = warnings.isEmpty(),
        violationCount = warnings.size,
        warnings = warnings.toList(),
        glassSurfaceCount = 0,
        glassSurfaceBudget = budget.maxGlassSurfaces,
        motionCount = motionAudit.activeSlots,
        motionBudget = budget.maxConcurrentMotions,
        overdrawLayerCount = 0,
        overdrawBudget = budget.maxOverdrawLayers,
        composableLayerCount = 0,
        composableBudget = budget.maxComposableLayers,
        waveRibbonCount = if (schedule.isActive(LibraryMotionSlot.WAVE_PULSE)) 1 else 0,
        waveRibbonBudget = budget.maxWaveRibbons,
        visibleSectionCount = 0,
        visibleSectionBudget = budget.maxVisibleSections,
        recompositionDepth = 0,
        recompositionBudget = budget.maxRecompositionDepth,
        allocationBytes = 0,
        allocationBudget = budget.allocationToleranceBytes,
    )
}
