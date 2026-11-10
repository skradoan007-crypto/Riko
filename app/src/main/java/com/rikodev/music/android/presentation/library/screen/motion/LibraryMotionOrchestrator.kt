/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.3 / Phase C — Living Motion
 *
 * File C2 / C6 — LibraryMotionOrchestrator.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [LibraryMotionOrchestrator] is the *clock* of the Library screen.
 *
 *   কে কখন নড়বে।
 *   কার phase offset কত।
 *   কখন silence থাকবে।
 *
 * The Orchestrator does NOT run animations. It only *schedules* them —
 * producing a deterministic plan that other components consume. This
 * separation is critical:
 *
 *   - Orchestrator = pure data (schedule, offsets, budgets)
 *   - Composables  = pure rendering (consume schedule, paint motion)
 *
 * ## Why an Orchestrator?
 *
 * Rule #69 — Sections Breathe Independently.
 * Without coordination, every section would start its entrance at the
 * same moment. The screen would feel mechanical.
 *
 * Rule #74 — Silence Is Also UX.
 * Without a budget, every component would add its own motion. The
 * screen would feel chaotic.
 *
 * The Orchestrator solves both:
 *   - It hands each section a *stagger offset* (so entrances cascade)
 *   - It hands each breathing element a *phase offset* (so breaths
 *     don't sync)
 *   - It audits the total motion count (so silence is preserved)
 *
 * ## What the Orchestrator is NOT
 *
 * ❌ Not a `@Composable`. It produces data; composables consume it.
 * ❌ Not a `ViewModel`. It has no lifecycle; it's a pure resolver.
 * ❌ Not a `Reducer`. It doesn't transform state; it transforms intent.
 * ❌ Not an animator. It doesn't run `Animatable` or `AnimationState`.
 *
 * ## Architecture Placement
 *
 *   LibraryScreenReducer (B5.2)   → produces LibraryScreenState
 *                  │
 *                  ▼
 *   LibraryMotionOrchestrator (C2) → produces LibraryMotionSchedule
 *                  │
 *                  ▼
 *   LibraryScreenComposable (D1)   → consumes schedule + state
 *                  │
 *                  ▼
 *   Hero / Section / Dock / etc.   → each reads its own slice
 *
 * The Orchestrator sits between Reducer and Composable. It is a pure
 * function: `(State, Constitution) → Schedule`. No state mutation,
 * no side effects.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #58  Runtime Never Knows Rendering   — this is pure Kotlin data
 * #64  Motion Must Be Discoverable    — phase offsets ≥ 1.2s
 * #69  Sections Breathe Independently — stagger ≥ 40ms, phase ≥ 1.2s
 * #73  Motion Must Have Meaning       — each motion has a documented role
 * #74  Silence Is Also UX             — total motion count ≤ 5
 *
 * © RIKO Experience OS — Rendering Tier (Phase C)
 */

package com.rikodev.music.android.presentation.library.screen.motion

import com.rikodev.music.android.presentation.library.screen.contract.LibraryMotionChoreography
import com.rikodev.music.android.presentation.library.screen.contract.LibraryMotionLanguage
import com.rikodev.music.android.presentation.library.screen.runtime.LibraryScreenPhase
import com.rikodev.music.android.presentation.library.screen.runtime.LibraryScreenState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// MOTION SLOT
//
// A named "slot" in the motion schedule. Each slot is one logical
// motion that the screen will run.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A named motion slot on the Library screen.
 *
 * Each slot represents ONE logical motion. The Orchestrator assigns
 * each slot:
 *   - An entrance delay (when it should start, in ms)
 *   - A phase offset (for breathing / ambient motions, in ms)
 *   - Whether it is currently active
 *
 * Rule #73 — Motion Must Have Meaning.
 * Every slot has a documented `meaning`. If you can't write the meaning
 * in one sentence, the motion doesn't belong on the screen.
 */
enum class LibraryMotionSlot(
    val meaning: String,
) {
    /** Phase A ambient layer breathing.
     *  Meaning: "Screen is alive." Always on (unless reduce-motion). */
    AMBIENT_BREATH("Screen is alive."),

    /** Phase C1 wave ambient pulse.
     *  Meaning: "Music is alive." Only when audio is active. */
    WAVE_PULSE("Music is alive."),

    /** Hero entrance animation.
     *  Meaning: "This is the headline of the screen." Fires on first compose. */
    HERO_ENTRANCE("This is the screen's headline."),

    /** Hero idle breathing (subtle glow modulation).
     *  Meaning: "Hero is at rest, not frozen." Continuous. */
    HERO_BREATH("Hero is at rest, not frozen."),

    /** Section entrance stagger.
     *  Meaning: "Organic, not mechanical." Fires on first compose, cascades. */
    SECTION_ENTRANCE("Organic, not mechanical."),

    /** Dock track-change flash.
     *  Meaning: "A new track started." Fires on trackChange. */
    DOCK_TRACK_FLASH("A new track started."),

    /** Dock scroll-hide.
     *  Meaning: "User is scrolling." Fires on scroll-down. */
    DOCK_SCROLL_HIDE("User is scrolling."),

    /** Loading shimmer.
     *  Meaning: "Content is coming." Only during LOADING phase. */
    LOADING_SHIMMER("Content is coming."),

    /** Empty state breath.
     *  Meaning: "Library is resting, not dead." Only during EMPTY phase. */
    EMPTY_BREATH("Library is resting, not dead."),
}

// ─────────────────────────────────────────────────────────────────────────────
// MOTION SCHEDULE ENTRY
//
// The per-slot schedule: when to start, what phase offset, is it active.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A single entry in the motion schedule.
 *
 * @property slot           The motion slot this entry describes.
 * @property entranceDelayMs  Delay (in ms) before this motion starts.
 *                            For entrance motions, this is the stagger.
 *                            For continuous motions (breathing), this
 *                            is the phase offset from screen start.
 * @property phaseOffsetMs  Phase offset (in ms) for continuous motions.
 *                          Used to desynchronize breathing elements.
 *                          Rule #69: ≥ 1_200ms between any two
 *                          breathing elements.
 * @property isActive       True if this motion is currently scheduled
 *                          to run. Inactive slots paint nothing.
 * @property priority       1 (highest) to 5 (lowest). When the total
 *                          active motion count exceeds the budget
 *                          (Rule #74), lower-priority slots are
 *                          deactivated first.
 */
data class LibraryMotionScheduleEntry(
    val slot: LibraryMotionSlot,
    val entranceDelayMs: Long,
    val phaseOffsetMs: Long,
    val isActive: Boolean,
    val priority: Int,
) {
    init {
        require(priority in 1..5) {
            "priority must be in [1, 5], got $priority"
        }
        require(entranceDelayMs >= 0L) {
            "entranceDelayMs must be ≥ 0, got $entranceDelayMs"
        }
        require(phaseOffsetMs >= 0L) {
            "phaseOffsetMs must be ≥ 0, got $phaseOffsetMs"
        }
    }

    companion object {
        /** A disabled entry — paints nothing. */
        fun disabled(slot: LibraryMotionSlot) = LibraryMotionScheduleEntry(
            slot = slot,
            entranceDelayMs = 0L,
            phaseOffsetMs = 0L,
            isActive = false,
            priority = 5,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MOTION SCHEDULE
//
// The complete schedule for one frame of the Library screen.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The complete motion schedule for the Library screen at a given moment.
 *
 * Produced by [resolveMotionSchedule]. Consumed by composables.
 *
 * @property entries       All schedule entries, indexed by slot.
 * @property activeCount   Number of currently-active motions.
 *                         Rule #74: ≤ 5 at any time.
 * @property isReducedMotion  True if all motions are disabled / static.
 * @property choreography  The active choreography (from motion language).
 */
data class LibraryMotionSchedule(
    val entries: Map<LibraryMotionSlot, LibraryMotionScheduleEntry>,
    val activeCount: Int,
    val isReducedMotion: Boolean,
    val choreography: LibraryMotionChoreography,
) {
    /** Look up the schedule entry for a slot. */
    fun entryFor(slot: LibraryMotionSlot): LibraryMotionScheduleEntry =
        entries[slot] ?: LibraryMotionScheduleEntry.disabled(slot)

    /** True if the given slot is currently active. */
    fun isActive(slot: LibraryMotionSlot): Boolean =
        entries[slot]?.isActive == true

    /** The entrance delay for a slot (0 if inactive). */
    fun entranceDelayFor(slot: LibraryMotionSlot): Long =
        entries[slot]?.takeIf { it.isActive }?.entranceDelayMs ?: 0L

    /** The phase offset for a slot (0 if inactive). */
    fun phaseOffsetFor(slot: LibraryMotionSlot): Long =
        entries[slot]?.takeIf { it.isActive }?.phaseOffsetMs ?: 0L

    companion object {
        /** An empty schedule — all slots disabled. Used for cold start. */
        val SILENT = LibraryMotionSchedule(
            entries = emptyMap(),
            activeCount = 0,
            isReducedMotion = true,
            choreography = LibraryMotionChoreography.SIMULTANEOUS,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STAGGER BUDGET
//
// Per-Rule #69: section entrances cascade with ≥ 40ms stagger.
// Per-Rule #64: phase offsets ≥ 1.2s for breathing motions.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Stagger budget for entrance animations.
 *
 * Rule #69 — Sections Breathe Independently.
 * Each section enters after the previous, with a small delay. This
 * produces the "organic" feel that separates premium apps from
 * mechanical ones.
 *
 * @property perSectionMs   Delay between consecutive section entrances.
 *                          Rule #69: ≥ 40ms.
 * @property heroLeadMs     How long before the first section the Hero enters.
 *                          Hero leads, sections follow.
 * @property dockLeadMs     How long after the last section the Dock enters.
 *                          Dock closes the entrance sequence.
 * @property maxTotalMs     Maximum total entrance duration. After this,
 *                          all remaining slots activate simultaneously.
 */
data class LibraryStaggerBudget(
    val perSectionMs: Long,
    val heroLeadMs: Long,
    val dockLeadMs: Long,
    val maxTotalMs: Long,
) {
    init {
        // ── Rule #69 enforcement ─────────────────────────────────────────
        require(perSectionMs >= 40L) {
            "perSectionMs ($perSectionMs) must be ≥ 40 " +
                "(Rule #69 — section stagger ≥ 40ms)."
        }
        require(heroLeadMs >= 0L) {
            "heroLeadMs ($heroLeadMs) must be ≥ 0."
        }
        require(dockLeadMs >= 0L) {
            "dockLeadMs ($dockLeadMs) must be ≥ 0."
        }
        require(maxTotalMs in 0L..3_000L) {
            "maxTotalMs ($maxTotalMs) must be in [0, 3000]."
        }
    }

    companion object {
        /** DEFAULT stagger — Hero leads by 80ms, sections 60ms apart. */
        val DEFAULT = LibraryStaggerBudget(
            perSectionMs = 60L,
            heroLeadMs = 80L,
            dockLeadMs = 120L,
            maxTotalMs = 1_800L,
        )

        /** CINEMATIC stagger — slower, more dramatic. */
        val CINEMATIC = LibraryStaggerBudget(
            perSectionMs = 100L,
            heroLeadMs = 160L,
            dockLeadMs = 240L,
            maxTotalMs = 2_400L,
        )

        /** REDUCED_MOTION stagger — all simultaneous (no cascade). */
        val REDUCED_MOTION = LibraryStaggerBudget(
            perSectionMs = 0L,
            heroLeadMs = 0L,
            dockLeadMs = 0L,
            maxTotalMs = 0L,
        )
    }
}

/**
 * Phase offset budget for breathing / continuous motions.
 *
 * Rule #69 — Sections Breathe Independently.
 * Without phase offsets, all breathing elements would sync — producing
 * a "mechanical" feel. Phase offsets desynchronize them.
 *
 * @property ambientPhaseMs     Phase offset for the ambient breath.
 * @property heroBreathPhaseMs  Phase offset for the Hero breath.
 * @property wavePulsePhaseMs   Phase offset for the wave pulse.
 * @property emptyBreathPhaseMs Phase offset for the empty state breath.
 *
 * Each offset is relative to the screen's "time zero" (first compose).
 * Offsets are large enough that two breathing elements never sync.
 */
data class LibraryPhaseBudget(
    val ambientPhaseMs: Long,
    val heroBreathPhaseMs: Long,
    val wavePulsePhaseMs: Long,
    val emptyBreathPhaseMs: Long,
) {
    init {
        // ── Rule #69 enforcement ─────────────────────────────────────────
        // Any two breathing motions must have phase offsets differing by
        // ≥ 1_200ms. This prevents sync.
        val offsets = listOf(
            ambientPhaseMs,
            heroBreathPhaseMs,
            wavePulsePhaseMs,
            emptyBreathPhaseMs,
        )
        offsets.forEachIndexed { i, a ->
            offsets.drop(i + 1).forEach { b ->
                require(kotlin.math.abs(a - b) >= 1_200L) {
                    "Phase offsets must differ by ≥ 1200ms (Rule #69). " +
                        "Found $a and $b (delta ${kotlin.math.abs(a - b)})."
                }
            }
        }
    }

    companion object {
        /**
         * DEFAULT phase budget.
         *
         * - Ambient:    0ms     (screen's natural breath)
         * - Hero:       2_400ms (offset by 2.4s — out of sync with ambient)
         * - Wave pulse: 4_800ms (offset by 4.8s — out of sync with both)
         * - Empty:      1_200ms (offset by 1.2s — minimal but compliant)
         */
        val DEFAULT = LibraryPhaseBudget(
            ambientPhaseMs = 0L,
            heroBreathPhaseMs = 2_400L,
            wavePulsePhaseMs = 4_800L,
            emptyBreathPhaseMs = 1_200L,
        )

        /**
         * AMOLED phase budget — larger offsets (slower feel).
         */
        val AMOLED = LibraryPhaseBudget(
            ambientPhaseMs = 0L,
            heroBreathPhaseMs = 3_200L,
            wavePulsePhaseMs = 6_400L,
            emptyBreathPhaseMs = 1_600L,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MOTION BUDGET AUDIT
//
// Per-Rule #74: total active motion count ≤ 5 at any time.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Maximum number of simultaneously-active motions on the Library screen.
 *
 * Rule #74 — Silence Is Also UX.
 * This constant is the hard ceiling. The Orchestrator will deactivate
 * lower-priority slots before exceeding this number.
 */
internal const val MAX_CONCURRENT_MOTIONS = 5

// ─────────────────────────────────────────────────────────────────────────────
// SCHEDULE RESOLVER
//
// The pure function that turns (State, Constitution) → Schedule.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Resolves the complete motion schedule for the Library screen.
 *
 * This is the main entry point of the Orchestrator. It is a pure
 * function — same inputs always produce the same outputs. No state
 * mutation, no side effects.
 *
 * ## Inputs
 *
 * - `state`           : The current LibraryScreenState (from B5.2 Reducer)
 * - `motionLanguage`  : The motion language (carries reduce-motion flag)
 * - `staggerBudget`   : The stagger budget (DEFAULT / CINEMATIC / REDUCED)
 * - `phaseBudget`     : The phase budget (DEFAULT / AMOLED)
 *
 * ## Outputs
 *
 * A [LibraryMotionSchedule] containing per-slot entrance delays,
 * phase offsets, and active flags.
 *
 * ## Decision Logic
 *
 * 1. If `motionLanguage.isReduceMotionEnabled` → all motions disabled
 *    except crossfade replacements (handled by C6).
 * 2. If `state.phase == LOADING` → LOADING_SHIMMER active, others dimmed.
 * 3. If `state.phase == EMPTY` → EMPTY_BREATH active, others dimmed.
 * 4. If `state.dock.isPlaying` → WAVE_PULSE active, else inactive.
 * 5. Apply stagger budget for entrance motions.
 * 6. Apply phase budget for breathing motions.
 * 7. Audit active count — if > MAX_CONCURRENT_MOTIONS, disable
 *    lowest-priority slots first.
 *
 * @param state           The current Library screen state.
 * @param motionLanguage  The motion language.
 * @param staggerBudget   The stagger budget (default = DEFAULT).
 * @param phaseBudget     The phase budget (default = DEFAULT).
 * @return A complete motion schedule.
 */
fun resolveMotionSchedule(
    state: LibraryScreenState,
    motionLanguage: LibraryMotionLanguage,
    staggerBudget: LibraryStaggerBudget = LibraryStaggerBudget.DEFAULT,
    phaseBudget: LibraryPhaseBudget = LibraryPhaseBudget.DEFAULT,
): LibraryMotionSchedule {
    val isReducedMotion = motionLanguage.isReduceMotionEnabled

    // ── Reduced-motion fast path ──────────────────────────────────────────
    // All motions disabled. The C6 ReducedMotionFallback handles
    // crossfade replacements. The Orchestrator's job here is to
    // produce a SILENT schedule.
    if (isReducedMotion) {
        return LibraryMotionSchedule.SILENT.copy(
            isReducedMotion = true,
            choreography = motionLanguage.defaultChoreography,
        )
    }

    val entries = mutableMapOf<LibraryMotionSlot, LibraryMotionScheduleEntry>()

    // ── AMBIENT_BREATH (always on, unless reduce-motion) ──────────────────
    // Priority 1 — highest. The screen is always alive.
    entries[LibraryMotionSlot.AMBIENT_BREATH] = LibraryMotionScheduleEntry(
        slot = LibraryMotionSlot.AMBIENT_BREATH,
        entranceDelayMs = 0L,
        phaseOffsetMs = phaseBudget.ambientPhaseMs,
        isActive = true,
        priority = 1,
    )

    // ── WAVE_PULSE (only when music is playing) ───────────────────────────
    // Priority 2 — high. Music is alive signal.
    val isMusicActive = state.dock.isPlaying
    entries[LibraryMotionSlot.WAVE_PULSE] = LibraryMotionScheduleEntry(
        slot = LibraryMotionSlot.WAVE_PULSE,
        entranceDelayMs = 0L,
        phaseOffsetMs = phaseBudget.wavePulsePhaseMs,
        isActive = isMusicActive,
        priority = 2,
    )

    // ── HERO_ENTRANCE (only on first compose / phase change) ──────────────
    // Priority 2 — high. The screen's headline.
    val heroEntranceActive = state.hasHero &&
        state.phase != LibraryScreenPhase.COLD_START
    entries[LibraryMotionSlot.HERO_ENTRANCE] = LibraryMotionScheduleEntry(
        slot = LibraryMotionSlot.HERO_ENTRANCE,
        entranceDelayMs = staggerBudget.heroLeadMs,
        phaseOffsetMs = 0L,
        isActive = heroEntranceActive,
        priority = 2,
    )

    // ── HERO_BREATH (continuous, when hero visible) ───────────────────────
    // Priority 3 — medium. Hero is at rest.
    val heroBreathActive = state.hasHero
    entries[LibraryMotionSlot.HERO_BREATH] = LibraryMotionScheduleEntry(
        slot = LibraryMotionSlot.HERO_BREATH,
        entranceDelayMs = 0L,
        phaseOffsetMs = phaseBudget.heroBreathPhaseMs,
        isActive = heroBreathActive,
        priority = 3,
    )

    // ── SECTION_ENTRANCE (staggered) ──────────────────────────────────────
    // Priority 3 — medium. Organic cascade.
    val sectionEntranceActive = state.visibleSectionCount > 0 &&
        state.phase != LibraryScreenPhase.COLD_START
    // The Orchestrator only marks this slot active. Per-section stagger
    // values are computed by the composable using `staggerBudget.perSectionMs`
    // and the section's index. Here we just provide the slot.
    entries[LibraryMotionSlot.SECTION_ENTRANCE] = LibraryMotionScheduleEntry(
        slot = LibraryMotionSlot.SECTION_ENTRANCE,
        entranceDelayMs = staggerBudget.heroLeadMs + staggerBudget.perSectionMs,
        phaseOffsetMs = 0L,
        isActive = sectionEntranceActive,
        priority = 3,
    )

    // ── DOCK_TRACK_FLASH (only when track changes) ────────────────────────
    // Priority 2 — high. Time-sensitive.
    // The Orchestrator marks this active whenever dock has a track.
    // The actual flash fires only when the track ID changes — that's
    // the composable's job (using `LaunchedEffect(trackId)`).
    entries[LibraryMotionSlot.DOCK_TRACK_FLASH] = LibraryMotionScheduleEntry(
        slot = LibraryMotionSlot.DOCK_TRACK_FLASH,
        entranceDelayMs = staggerBudget.heroLeadMs +
            (staggerBudget.perSectionMs * state.visibleSectionCount.coerceAtMost(5)) +
            staggerBudget.dockLeadMs,
        phaseOffsetMs = 0L,
        isActive = state.dock.hasTrack,
        priority = 2,
    )

    // ── DOCK_SCROLL_HIDE (only when dock visible and user can scroll) ─────
    // Priority 4 — low. Reactive.
    entries[LibraryMotionSlot.DOCK_SCROLL_HIDE] = LibraryMotionScheduleEntry(
        slot = LibraryMotionSlot.DOCK_SCROLL_HIDE,
        entranceDelayMs = 0L,
        phaseOffsetMs = 0L,
        isActive = state.dock.isVisible && state.scroll.maxScrollOffsetDp > 0,
        priority = 4,
    )

    // ── LOADING_SHIMMER (only during LOADING phase) ───────────────────────
    // Priority 1 — highest during loading. All other motions dimmed.
    val isLoading = state.isLoading
    entries[LibraryMotionSlot.LOADING_SHIMMER] = LibraryMotionScheduleEntry(
        slot = LibraryMotionSlot.LOADING_SHIMMER,
        entranceDelayMs = 0L,
        phaseOffsetMs = 0L,
        isActive = isLoading,
        priority = 1,
    )

    // ── EMPTY_BREATH (only during EMPTY phase) ────────────────────────────
    // Priority 2 — high. Library is resting.
    val isEmpty = state.phase == LibraryScreenPhase.EMPTY
    entries[LibraryMotionSlot.EMPTY_BREATH] = LibraryMotionScheduleEntry(
        slot = LibraryMotionSlot.EMPTY_BREATH,
        entranceDelayMs = 0L,
        phaseOffsetMs = phaseBudget.emptyBreathPhaseMs,
        isActive = isEmpty,
        priority = 2,
    )

    // ── Rule #74 audit: total active motion count ≤ 5 ─────────────────────
    // If we exceed the budget, disable lowest-priority slots first.
    val auditedEntries = auditMotionBudget(entries)

    val activeCount = auditedEntries.values.count { it.isActive }

    return LibraryMotionSchedule(
        entries = auditedEntries,
        activeCount = activeCount,
        isReducedMotion = false,
        choreography = motionLanguage.defaultChoreography,
    )
}

/**
 * Audits the motion budget and disables lowest-priority slots if the
 * total active count exceeds [MAX_CONCURRENT_MOTIONS].
 *
 * Rule #74 — Silence Is Also UX.
 * The screen must never have more than 5 simultaneous motions. When
 * the count exceeds 5, we disable slots in priority order (5 first,
 * then 4, then 3, etc.) until the count is within budget.
 *
 * Priority 1 slots are never disabled — they represent the screen's
 * baseline aliveness (ambient breath, loading shimmer).
 */
private fun auditMotionBudget(
    entries: Map<LibraryMotionSlot, LibraryMotionScheduleEntry>,
): Map<LibraryMotionSlot, LibraryMotionScheduleEntry> {
    val active = entries.filterValues { it.isActive }
    if (active.size <= MAX_CONCURRENT_MOTIONS) return entries

    val result = entries.toMutableMap()
    // Disable in reverse priority order (5, 4, 3, 2 — never 1).
    val prioritiesToDisable = (5 downTo 2)
    var currentActive = active.size

    for (priority in prioritiesToDisable) {
        if (currentActive <= MAX_CONCURRENT_MOTIONS) break
        val atPriority = active.filter { it.value.priority == priority }
        atPriority.forEach { (slot, _) ->
            result[slot] = result[slot]!!.copy(isActive = false)
            currentActive--
            if (currentActive <= MAX_CONCURRENT_MOTIONS) return@forEach
        }
    }

    return result
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION-SPECIFIC STAGGER HELPER
//
// The Orchestrator marks SECTION_ENTRANCE active, but each section
// needs its own delay. This helper computes per-section delays.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Computes the entrance delay for a specific section, given its index
 * in the visible section list.
 *
 * Rule #69 — Sections Breathe Independently.
 * Each section enters after the previous, with `perSectionMs` delay.
 * The first section enters after the Hero (heroLeadMs + perSectionMs).
 * Subsequent sections each add another `perSectionMs`.
 *
 * @param sectionIndex   0-based index of the section in the visible list.
 * @param schedule       The motion schedule (provides base delay).
 * @param staggerBudget  The stagger budget (provides perSectionMs).
 * @return Entrance delay in ms. 0 if section entrance is inactive.
 */
fun sectionEntranceDelay(
    sectionIndex: Int,
    schedule: LibraryMotionSchedule,
    staggerBudget: LibraryStaggerBudget = LibraryStaggerBudget.DEFAULT,
): Long {
    val entry = schedule.entryFor(LibraryMotionSlot.SECTION_ENTRANCE)
    if (!entry.isActive) return 0L
    return entry.entranceDelayMs + (staggerBudget.perSectionMs * sectionIndex)
}

// ─────────────────────────────────────────────────────────────────────────────
// MOTION AUDIT REPORT
//
// A diagnostic snapshot of the current motion schedule. Used by
// the diagnostics layer (B5.2 LibraryRuntimeDiagnostics) to verify
// Rule #74 compliance at runtime.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A diagnostic snapshot of the motion schedule.
 *
 * Produced by [auditMotion]. Used by the diagnostics layer to verify
 * Rule #74 compliance at runtime. This is observation-only — it never
 * affects behavior (Rule #53).
 *
 * @property totalSlots        Total number of motion slots defined.
 * @property activeSlots       Number of currently-active slots.
 * @property activeSlotNames   Names of active slots (for logging).
 * @property isOverBudget      True if activeSlots > MAX_CONCURRENT_MOTIONS.
 * @property budgetLimit       The MAX_CONCURRENT_MOTIONS constant.
 */
data class LibraryMotionAuditReport(
    val totalSlots: Int,
    val activeSlots: Int,
    val activeSlotNames: List<String>,
    val isOverBudget: Boolean,
    val budgetLimit: Int,
)

/**
 * Produces a diagnostic audit report of the given schedule.
 *
 * Rule #53 — Diagnostics Never Affects Engine.
 * This function is read-only. It does not modify the schedule.
 */
fun auditMotion(schedule: LibraryMotionSchedule): LibraryMotionAuditReport {
    val active = schedule.entries.filterValues { it.isActive }
    return LibraryMotionAuditReport(
        totalSlots = schedule.entries.size,
        activeSlots = active.size,
        activeSlotNames = active.keys.map { it.name }.sorted(),
        isOverBudget = active.size > MAX_CONCURRENT_MOTIONS,
        budgetLimit = MAX_CONCURRENT_MOTIONS,
    )
}
