/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.3 / Phase C — Living Motion
 *
 * File C6 / C6 — LibraryReducedMotionFallback.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [LibraryReducedMotionFallback] is the *silence layer* of the Library
 * screen.
 *
 *   যখন reduce-motion চালু — সব motion বন্ধ।
 *   শুধু crossfade।
 *   কিন্তু information preserve থাকে।
 *
 * Rule #74 — Silence Is Also UX.
 * Premium apps জানে কখন থামতে হয়। অনেক user-এর জন্য motion
 * problematic — vestibular disorders, epilepsy, motion sensitivity।
 * তাদের জন্য RIKO শুধু motion বন্ধ করে না — সে information হজম
 * করার একটা বিকল্প পথ দেয়।
 *
 * ## What This File Is
 *
 * A set of *wrapper composables* that detect reduce-motion and
 * substitute motion with crossfade or instant transitions. Other
 * Phase C files (C1, C3, C4) call into this file's helpers to
 * decide whether to animate or not.
 *
 * ## Why a Separate File?
 *
 * Reduce-motion is a *cross-cutting concern*. Every motion-producing
 * composable needs to honor it. Centralizing the fallback logic
 * ensures consistency — no composable can "forget" to honor the
 * accessibility contract.
 *
 * ## Three Fallback Strategies
 *
 * When reduce-motion is on, motions are replaced with one of three
 * strategies:
 *
 *   1. **CROSSFADE** — for state transitions (Hero content change,
 *      Dock track change, section refresh). The new state fades in
 *      over the old.
 *
 *   2. **INSTANT** — for entrance animations (Hero entrance, section
 *      stagger). Content appears immediately at its final position.
 *
 *   3. **STATIC** — for continuous motions (breathing, drift). The
 *      motion is frozen at its midpoint value.
 *
 * ## Audit
 *
 * The fallback exposes an audit function [auditReducedMotionCoverage]
 * that verifies every motion slot in the schedule has a fallback
 * strategy defined. This is used by the diagnostics layer (B5.2) to
 * verify accessibility compliance at runtime.
 *
 * WHAT THIS FILE IS NOT
 * ---------------------
 * ❌ Not a motion producer. It only *wraps* existing motions.
 * ❌ Not a settings screen. User-facing settings are in B5.5.
 * ❌ Not a contract. The contract is in B5.1 [LibraryAccessibilityContract].
 *
 * RULES ENFORCED HERE
 * -------------------
 * #58  Runtime Never Knows Rendering   — this is rendering
 * #64  Motion Must Be Discoverable     — fallback uses instant/crossfade
 * #73  Motion Must Have Meaning        — fallback preserves information
 * #74  Silence Is Also UX              — reduce-motion = silence
 *
 * © RIKO Experience OS — Rendering Tier (Phase C)
 */

package com.rikodev.music.android.presentation.library.screen.motion

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.rikodev.music.android.presentation.library.screen.contract.LibraryMotionLanguage
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.draw.drawBehind

// ─────────────────────────────────────────────────────────────────────────────
// FALLBACK STRATEGY
//
// The three ways a motion can be replaced when reduce-motion is on.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The strategy for replacing a motion when reduce-motion is on.
 *
 * Rule #74 — Silence Is Also UX.
 * Each motion slot has a designated fallback strategy. This ensures
 * that disabling motion doesn't break the experience — it just makes
 * it quieter.
 */
enum class LibraryReducedMotionStrategy(
    val description: String,
) {
    /** Crossfade — new state fades in over old.
     *  Used for: state transitions (track change, content refresh). */
    CROSSFADE("Crossfade — state transition."),

    /** Instant — content appears at its final position immediately.
     *  Used for: entrance animations (Hero entrance, section stagger). */
    INSTANT("Instant — entrance appears immediately."),

    /** Static — motion is frozen at its midpoint value.
     *  Used for: continuous motions (breathing, drift). */
    STATIC("Static — continuous motion frozen at midpoint."),

    /** Hidden — the motion is removed entirely, no visual replacement.
     *  Used for: purely decorative motions that carry no information. */
    HIDDEN("Hidden — motion removed, no replacement."),
}

// ─────────────────────────────────────────────────────────────────────────────
// FALLBACK REGISTRY
//
// Maps each motion slot to its designated fallback strategy.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The fallback strategy for each [LibraryMotionSlot].
 *
 * This registry is consulted when `motionLanguage.isReduceMotionEnabled`
 * is true. Each slot's strategy determines how its motion is replaced.
 */
val LibraryReducedMotionFallbackRegistry: Map<LibraryMotionSlot, LibraryReducedMotionStrategy> =
    mapOf(
        LibraryMotionSlot.AMBIENT_BREATH to LibraryReducedMotionStrategy.STATIC,
        LibraryMotionSlot.WAVE_PULSE to LibraryReducedMotionStrategy.STATIC,
        LibraryMotionSlot.HERO_ENTRANCE to LibraryReducedMotionStrategy.INSTANT,
        LibraryMotionSlot.HERO_BREATH to LibraryReducedMotionStrategy.STATIC,
        LibraryMotionSlot.SECTION_ENTRANCE to LibraryReducedMotionStrategy.INSTANT,
        LibraryMotionSlot.DOCK_TRACK_FLASH to LibraryReducedMotionStrategy.CROSSFADE,
        LibraryMotionSlot.DOCK_SCROLL_HIDE to LibraryReducedMotionStrategy.CROSSFADE,
        LibraryMotionSlot.LOADING_SHIMMER to LibraryReducedMotionStrategy.STATIC,
        LibraryMotionSlot.EMPTY_BREATH to LibraryReducedMotionStrategy.STATIC,
    )

/**
 * Looks up the fallback strategy for a motion slot.
 *
 * If no strategy is registered, returns [LibraryReducedMotionStrategy.HIDDEN]
 * (the safest default — no motion, no replacement).
 */
fun LibraryMotionSlot.reducedMotionStrategy(): LibraryReducedMotionStrategy =
    LibraryReducedMotionFallbackRegistry[this]
        ?: LibraryReducedMotionStrategy.HIDDEN

// ─────────────────────────────────────────────────────────────────────────────
// CROSSFADE WRAPPER
//
// A composable that applies crossfade when reduce-motion is on,
// or delegates to the normal content when off.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A composable that applies crossfade transitions between content
 * states when reduce-motion is on.
 *
 * Rule #74 — Silence Is Also UX.
 * When reduce-motion is enabled, state transitions use crossfade
 * instead of motion. The crossfade duration is short (200ms) — long
 * enough to be perceived, short enough to not feel slow.
 *
 * When reduce-motion is off, this composable delegates to the
 * `content` lambda directly — no wrapping, no overhead.
 *
 * @param targetState    The current state. When this changes, the
 *                       content crossfades to the new state.
 * @param motionLanguage The motion language (for reduce-motion check).
 * @param modifier       Compose modifier.
 * @param content        The content composable, given the current state.
 */
@Composable
fun <T> LibraryReducedCrossfade(
    targetState: T,
    motionLanguage: LibraryMotionLanguage,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    if (!motionLanguage.isReduceMotionEnabled) {
        // ── Normal path: no crossfade, just render ───────────────────────
        content(targetState)
        return
    }

    // ── Reduce-motion path: crossfade ─────────────────────────────────────
    // Duration: 200ms (LibraryMotionDuration.SHORT). Short enough to
    // not feel slow, long enough to be perceived.
    Crossfade(
        targetState = targetState,
        animationSpec = tween(durationMillis = 200),
        modifier = modifier,
        label = "libraryReducedCrossfade",
    ) { state ->
        content(state)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// INSTANT ENTRANCE WRAPPER
//
// A composable that renders content immediately when reduce-motion is
// on, or applies an entrance animation when off.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A composable that renders content immediately when reduce-motion is
 * on, or applies an entrance animation when off.
 *
 * Rule #74 — Silence Is Also UX.
 * When reduce-motion is enabled, entrance animations are skipped —
 * content appears at its final position immediately.
 *
 * When reduce-motion is off, this composable calls the `animated`
 * lambda, which should apply the entrance animation.
 *
 * @param motionLanguage The motion language.
 * @param animated       The animated entrance composable (used when
 *                       reduce-motion is off).
 * @param instant        The instant composable (used when reduce-motion
 *                       is on). If null, falls back to `animated`.
 * @param modifier       Compose modifier.
 */
@Composable
fun LibraryReducedEntrance(
    motionLanguage: LibraryMotionLanguage,
    animated: @Composable () -> Unit,
    instant: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (motionLanguage.isReduceMotionEnabled) {
        // ── Reduce-motion path: instant render ────────────────────────────
        (instant ?: animated)()
    } else {
        // ── Normal path: animated entrance ────────────────────────────────
        animated()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STATIC BREATH WRAPPER
//
// A helper that returns a fixed midpoint value when reduce-motion is on,
// or animates the breath when off.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Returns a fixed breath value when reduce-motion is on.
 *
 * Rule #74 — Silence Is Also UX.
 * When reduce-motion is enabled, breathing motions are frozen at
 * their midpoint (0.5). The visual element still renders — it just
 * doesn't animate.
 *
 * This is a non-composable helper — it returns a Float that the
 * calling composable uses to drive its drawBehind lambda. The actual
 * animation (when reduce-motion is off) is the caller's responsibility.
 *
 * @param motionLanguage The motion language.
 * @param animatedValue  The current animated value (used when reduce-motion is off).
 * @return The breath value to use (0.5 if reduce-motion, else [animatedValue]).
 */
fun resolveBreathValue(
    motionLanguage: LibraryMotionLanguage,
    animatedValue: Float,
): Float = if (motionLanguage.isReduceMotionEnabled) 0.5f else animatedValue

// ─────────────────────────────────────────────────────────────────────────────
// AUDIT
//
// Verifies that every motion slot has a fallback strategy defined.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A diagnostic snapshot of reduce-motion coverage.
 *
 * Rule #53 — Diagnostics Never Affects Engine.
 * This is observation-only — it never affects behavior.
 */
data class LibraryReducedMotionAuditReport(
    val totalSlots: Int,
    val coveredSlots: Int,
    val uncoveredSlotNames: List<String>,
    val isFullyCovered: Boolean,
)

/**
 * Audits the reduce-motion coverage of all motion slots.
 *
 * Verifies that every [LibraryMotionSlot] has an entry in
 * [LibraryReducedMotionFallbackRegistry]. Uncovered slots would
 * fall back to [LibraryReducedMotionStrategy.HIDDEN] — which may
 * not be appropriate for all motions.
 *
 * Rule #53 — Diagnostics Never Affects Engine.
 * This function is read-only. It does not modify the registry.
 */
fun auditReducedMotionCoverage(): LibraryReducedMotionAuditReport {
    val allSlots = LibraryMotionSlot.values().toSet()
    val coveredSlots = LibraryReducedMotionFallbackRegistry.keys
    val uncovered = allSlots - coveredSlots
    return LibraryReducedMotionAuditReport(
        totalSlots = allSlots.size,
        coveredSlots = coveredSlots.size,
        uncoveredSlotNames = uncovered.map { it.name }.sorted(),
        isFullyCovered = uncovered.isEmpty(),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// FALLBACK BUDGET ENFORCER
//
// When reduce-motion is on, ALL motions are replaced with fallbacks.
// This means the ambient motion budget is 0 — perfect silence.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The motion budget when reduce-motion is enabled.
 *
 * Rule #74 — Silence Is Also UX.
 * When reduce-motion is on, the budget is 0 — no ambient motions
 * are allowed. Crossfades and instant entrances are exempt (they
 * are reactive, not ambient).
 */
internal const val REDUCED_MOTION_AMBIENT_BUDGET = 0

/**
 * Returns true if the given slot should be active when reduce-motion
 * is on.
 *
 * Ambient motions (breathing, drift, shimmer) are deactivated.
 * Reactive motions (crossfade for state changes) are still allowed
 * via the wrapper composables above.
 *
 * This is used by [resolveMotionSchedule] in C2 to produce a SILENT
 * schedule when reduce-motion is enabled.
 */
fun LibraryMotionSlot.isActiveUnderReducedMotion(): Boolean = when (this) {
    // Ambient motions — deactivated under reduce-motion.
    LibraryMotionSlot.AMBIENT_BREATH,
    LibraryMotionSlot.WAVE_PULSE,
    LibraryMotionSlot.HERO_BREATH,
    LibraryMotionSlot.LOADING_SHIMMER,
    LibraryMotionSlot.EMPTY_BREATH,
    -> false

    // Entrance motions — replaced with INSTANT (still "active" but instant).
    LibraryMotionSlot.HERO_ENTRANCE,
    LibraryMotionSlot.SECTION_ENTRANCE,
    -> true

    // Reactive motions — replaced with CROSSFADE (still active).
    LibraryMotionSlot.DOCK_TRACK_FLASH,
    LibraryMotionSlot.DOCK_SCROLL_HIDE,
    -> true
}

// ─────────────────────────────────────────────────────────────────────────────
// PREVIEWS
//
// Demonstrate the three fallback strategies in isolation.
// ─────────────────────────────────────────────────────────────────────────────

@androidx.compose.ui.tooling.preview.Preview(
    name = "Reduced Motion — Crossfade (state 1)",
    showBackground = true,
    backgroundColor = 0xFF05070B,
)
@Composable
private fun PreviewReducedCrossfadeState1() {
    val motionLanguage = LibraryMotionLanguage.REDUCED
    LibraryReducedCrossfade(
        targetState = "state1",
        motionLanguage = motionLanguage,
    ) { state ->
        androidx.compose.material3.Text(
            text = "State: $state",
            color = androidx.compose.ui.graphics.Color.White,
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Reduced Motion — Crossfade (state 2)",
    showBackground = true,
    backgroundColor = 0xFF05070B,
)
@Composable
private fun PreviewReducedCrossfadeState2() {
    val motionLanguage = LibraryMotionLanguage.REDUCED
    LibraryReducedCrossfade(
        targetState = "state2",
        motionLanguage = motionLanguage,
    ) { state ->
        androidx.compose.material3.Text(
            text = "State: $state",
            color = androidx.compose.ui.graphics.Color.White,
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Reduced Motion — Instant Entrance",
    showBackground = true,
    backgroundColor = 0xFF05070B,
)
@Composable
private fun PreviewReducedInstantEntrance() {
    val motionLanguage = LibraryMotionLanguage.REDUCED
    LibraryReducedEntrance(
        motionLanguage = motionLanguage,
        animated = {
            androidx.compose.material3.Text(
                text = "Animated entrance (not used)",
                color = androidx.compose.ui.graphics.Color.White,
            )
        },
        instant = {
            androidx.compose.material3.Text(
                text = "Instant entrance (used)",
                color = androidx.compose.ui.graphics.Color.White,
            )
        },
    )
}
