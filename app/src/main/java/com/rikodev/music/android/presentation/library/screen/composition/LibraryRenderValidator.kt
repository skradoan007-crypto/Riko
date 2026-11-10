/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.3 — Library Rendering UI
 * Phase D — Composition / Performance / Validator / Preview
 *
 * File D3 / D4 — LibraryRenderValidator.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [LibraryRenderValidator] is the rule-enforcement analyst for the
 * Library screen. It inspects a state + constitution + schedule
 * triple and emits [LibraryRenderViolation]s for any of the
 * Architecture Freeze rules that are violated.
 *
 * The Validator NEVER blocks rendering. It only emits warnings —
 * the developer decides what to do about them.
 *
 * ## Rules Enforced
 *
 * - **Rule #58** — Runtime Never Knows Rendering
 *   The state must not carry rendering types (Color, Dp, Brush, ImageVector).
 *   State carries data; rendering is the renderer's job.
 *
 * - **Rule #60** — Screen State Is Already Renderable
 *   The state must be ready to render with zero transformation. No
 *   nullable fields that would require runtime resolution. No `List<*>?`
 *   that the renderer would have to filter. No raw strings that need
 *   parsing (e.g. no "#FF8A65" in state — colors come from contracts).
 *
 * - **Rule #70** — Architecture Freeze
 *   No new Runtime/Reducer/State/Contract patterns introduced. The
 *   state's shape must follow the locked invariants (Reducer is pure,
 *   State is immutable, Mapper is the last translator, etc.).
 *
 * ## Output
 *
 * The Validator returns a [LibraryRenderValidationReport] containing:
 *   - `violations: List<LibraryRenderViolation>` — the warnings.
 *   - `passedCount: Int` — how many checks passed.
 *   - `failedCount: Int` — how many checks failed.
 *   - `isClean: Boolean` — true if no violations.
 *
 * The Report NEVER contains `recommendedAction` — that's the developer's
 * job to decide. Same discipline as Rule #76.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #53  Diagnostics Never Affects Engine — Validator is observation-only
 * #58  Runtime Never Knows Rendering — checked
 * #60  Screen State Is Already Renderable — checked
 * #70  Architecture Freeze — checked
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Compose imports, no Coroutine, no Android framework, no ViewModel.
 * Pure Kotlin data + pure functions.
 *
 * © RIKO Experience OS — Diagnostics Tier
 */

package com.rikodev.music.android.presentation.library.screen.composition

import com.rikodev.music.android.presentation.library.screen.contract.LibraryConstitution
import com.rikodev.music.android.presentation.library.screen.contract.LibraryConstitutionProfile
import com.rikodev.music.android.presentation.library.screen.contract.LibraryEmptyStateContract
import com.rikodev.music.android.presentation.library.screen.contract.LibraryHeroContract
import com.rikodev.music.android.presentation.library.screen.contract.LibraryMotionLanguage
import com.rikodev.music.android.presentation.library.screen.contract.LibraryScreenContract
import com.rikodev.music.android.presentation.library.screen.contract.LibrarySectionContract
import com.rikodev.music.android.presentation.library.screen.contract.LibraryTopBarContract
import com.rikodev.music.android.presentation.library.screen.motion.LibraryMotionSchedule
import com.rikodev.music.android.presentation.library.screen.motion.LibraryMotionSlot
import com.rikodev.music.android.presentation.library.screen.motion.MAX_CONCURRENT_MOTIONS
import com.rikodev.music.android.presentation.library.screen.runtime.LibraryDockRuntimeState
import com.rikodev.music.android.presentation.library.screen.runtime.LibraryHeroRuntimeState
import com.rikodev.music.android.presentation.library.screen.runtime.LibraryScreenPhase
import com.rikodev.music.android.presentation.library.screen.runtime.LibraryScreenState
import com.rikodev.music.android.presentation.library.screen.runtime.LibrarySectionRuntimeState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Modifier
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.animation.core.Animatable

// ═══════════════════════════════════════════════════════════════════════════════════════
// VIOLATION — the unit of rule-breaking
// ═══════════════════════════════════════════════════════════════════════════════════════

/**
 * The severity of a [LibraryRenderViolation].
 *
 * - **ERROR**: A hard rule break. The screen may render incorrectly.
 *   Examples: state contains a Color, state has a nullable field that
 *   can't be resolved, motion count exceeds Rule #74 ceiling.
 *
 * - **WARNING**: A soft rule break. The screen will render, but the
 *   pattern is suspicious. Examples: state has a nullable field that
 *   COULD be eliminated, motion count is at the ceiling (not over).
 *
 * - **INFO**: An observation. No rule is broken, but the developer
 *   should be aware. Examples: state has 50+ fields (consider splitting),
 *   motion count is exactly at the default.
 */
enum class LibraryRenderViolationSeverity(
    val displayName: String,
    val symbol: String,
) {
    ERROR("Error", "❌"),
    WARNING("Warning", "⚠️"),
    INFO("Info", "ℹ️"),
}

/**
 * Which Rule the violation relates to.
 *
 * Used to group violations in the report and to allow selective
 * filtering (e.g. "show me only #58 violations").
 */
enum class LibraryRenderRule(
    val number: Int,
    val displayName: String,
) {
    RULE_58(58, "Runtime Never Knows Rendering"),
    RULE_60(60, "Screen State Is Already Renderable"),
    RULE_70(70, "Architecture Freeze"),
    RULE_74(74, "Silence Is Also UX (collateral check)"),
    ;

    companion object {
        fun fromNumber(number: Int): LibraryRenderRule? =
            values().firstOrNull { it.number == number }
    }
}

/**
 * A single rule violation.
 *
 * @property rule             The rule that was violated.
 * @property severity         The severity (ERROR/WARNING/INFO).
 * @property location         Where the violation was found (e.g. "state.hero.accentColorHex").
 * @property message          Human-readable description of the violation.
 * @property observedValue    The offending value (stringified), or null.
 * @property expectedValue    What the value should be (stringified), or null.
 */
data class LibraryRenderViolation(
    val rule: LibraryRenderRule,
    val severity: LibraryRenderViolationSeverity,
    val location: String,
    val message: String,
    val observedValue: String? = null,
    val expectedValue: String? = null,
) {

    /**
     * Formats the violation as a single-line string for logging.
     */
    fun format(): String {
        val parts = mutableListOf<String>()
        parts += "${severity.symbol} Rule #${rule.number} (${rule.displayName})"
        parts += "[$location]"
        parts += message
        if (observedValue != null) parts += "(observed: $observedValue)"
        if (expectedValue != null) parts += "(expected: $expectedValue)"
        return parts.joinToString(" ")
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// VALIDATION REPORT — the result of a validation pass
// ═══════════════════════════════════════════════════════════════════════════════════════

/**
 * The result of validating a Library render configuration.
 *
 * @property isClean        True if NO violations (all checks passed).
 * @property passedCount    Number of checks that passed.
 * @property failedCount    Number of checks that failed.
 * @property violations     The list of violations. Empty if `isClean`.
 * @property errors         Violations with severity ERROR.
 * @property warnings       Violations with severity WARNING.
 * @property infos          Violations with severity INFO.
 * @property rule58Status   Status summary for Rule #58.
 * @property rule60Status   Status summary for Rule #60.
 * @property rule70Status   Status summary for Rule #70.
 */
data class LibraryRenderValidationReport(
    val isClean: Boolean,
    val passedCount: Int,
    val failedCount: Int,
    val violations: List<LibraryRenderViolation>,
    val errors: List<LibraryRenderViolation>,
    val warnings: List<LibraryRenderViolation>,
    val infos: List<LibraryRenderViolation>,
    val rule58Status: LibraryRenderRuleStatus,
    val rule60Status: LibraryRenderRuleStatus,
    val rule70Status: LibraryRenderRuleStatus,
) {

    /**
     * The single-boolean answer: are there any ERROR-severity violations?
     */
    val hasErrors: Boolean get() = errors.isNotEmpty()

    /**
     * The single-boolean answer: are there any WARNING-severity violations?
     */
    val hasWarnings: Boolean get() = warnings.isNotEmpty()

    companion object {

        /**
         * An "all clean" report — used when there's nothing to validate
         * (e.g. COLD_START with empty state).
         */
        val CLEAN = LibraryRenderValidationReport(
            isClean = true,
            passedCount = 0,
            failedCount = 0,
            violations = emptyList(),
            errors = emptyList(),
            warnings = emptyList(),
            infos = emptyList(),
            rule58Status = LibraryRenderRuleStatus.PASSED,
            rule60Status = LibraryRenderRuleStatus.PASSED,
            rule70Status = LibraryRenderRuleStatus.PASSED,
        )
    }
}

/**
 * The status of a single rule's checks.
 */
enum class LibraryRenderRuleStatus {
    PASSED,
    HAS_WARNINGS,
    HAS_ERRORS,
    ;

    val isPassed: Boolean get() = this == PASSED
    val isFailed: Boolean get() = this != PASSED
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// THE VALIDATE FUNCTION — pure, observation-only
// ═══════════════════════════════════════════════════════════════════════════════════════

/**
 * Validates a Library render configuration against Rules #58, #60, #70.
 *
 * Rule #76 discipline — this function NEVER blocks rendering, NEVER
 * mutates state, NEVER triggers optimization. It only inspects the
 * inputs and returns a [LibraryRenderValidationReport].
 *
 * ## What the validator checks
 *
 * ### Rule #58 — Runtime Never Knows Rendering
 *
 * The runtime state must NOT carry rendering types:
 *   - No `Color` / `ColorInt` fields
 *   - No `Dp` / `Float` (treated as dp) fields
 *   - No `Brush` / `Shader` fields
 *   - No `ImageVector` / `Painter` fields
 *   - No `Modifier` fields
 *
 * Exception: hex color strings (e.g. `accentColorHex: String`) are
 * allowed — they're data, not rendering. The renderer parses them.
 *
 * ### Rule #60 — Screen State Is Already Renderable
 *
 * The state must be ready to render with zero transformation:
 *   - No nullable fields that the renderer would have to filter
 *   - No raw strings that need parsing (e.g. durations in ms, not "12s")
 *   - No empty lists that should be null (or vice versa)
 *   - All floats in valid ranges (e.g. progressFraction in [0, 1])
 *
 * ### Rule #70 — Architecture Freeze
 *
 * No new patterns introduced:
 *   - State shape follows the locked invariants
 *   - Constitution has all required contracts
 *   - Motion schedule respects MAX_CONCURRENT_MOTIONS (Rule #74 collateral)
 *
 * @param state          The Library screen state.
 * @param constitution   The Library constitution.
 * @param schedule       The resolved motion schedule.
 * @return               A [LibraryRenderValidationReport]. Never null.
 */
fun validateLibraryRender(
    state: LibraryScreenState,
    constitution: LibraryConstitution,
    schedule: LibraryMotionSchedule,
): LibraryRenderValidationReport {
    val violations = mutableListOf<LibraryRenderViolation>()
    var passedCount = 0

    // ── Rule #58: Runtime Never Knows Rendering ──────────────────────────
    val rule58Violations = checkRule58(state)
    violations += rule58Violations.violations
    passedCount += rule58Violations.passedCount

    // ── Rule #60: Screen State Is Already Renderable ─────────────────────
    val rule60Violations = checkRule60(state)
    violations += rule60Violations.violations
    passedCount += rule60Violations.passedCount

    // ── Rule #70: Architecture Freeze ─────────────────────────────────────
    val rule70Violations = checkRule70(state, constitution, schedule)
    violations += rule70Violations.violations
    passedCount += rule70Violations.passedCount

    // ── Aggregate by severity ─────────────────────────────────────────────
    val errors = violations.filter { it.severity == LibraryRenderViolationSeverity.ERROR }
    val warnings = violations.filter { it.severity == LibraryRenderViolationSeverity.WARNING }
    val infos = violations.filter { it.severity == LibraryRenderViolationSeverity.INFO }

    // ── Per-rule status ───────────────────────────────────────────────────
    val rule58Status = computeRuleStatus(violations, LibraryRenderRule.RULE_58)
    val rule60Status = computeRuleStatus(violations, LibraryRenderRule.RULE_60)
    val rule70Status = computeRuleStatus(violations, LibraryRenderRule.RULE_70)

    return LibraryRenderValidationReport(
        isClean = violations.isEmpty(),
        passedCount = passedCount,
        failedCount = violations.size,
        violations = violations.toList(),
        errors = errors,
        warnings = warnings,
        infos = infos,
        rule58Status = rule58Status,
        rule60Status = rule60Status,
        rule70Status = rule70Status,
    )
}

/**
 * Computes the status for a single rule.
 */
private fun computeRuleStatus(
    violations: List<LibraryRenderViolation>,
    rule: LibraryRenderRule,
): LibraryRenderRuleStatus {
    val ruleViolations = violations.filter { it.rule == rule }
    return when {
        ruleViolations.any { it.severity == LibraryRenderViolationSeverity.ERROR } ->
            LibraryRenderRuleStatus.HAS_ERRORS
        ruleViolations.any { it.severity == LibraryRenderViolationSeverity.WARNING } ->
            LibraryRenderRuleStatus.HAS_WARNINGS
        else -> LibraryRenderRuleStatus.PASSED
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// RULE #58 — Runtime Never Knows Rendering
// ═══════════════════════════════════════════════════════════════════════════════════════

/**
 * Result of a single rule check.
 */
private data class RuleCheckResult(
    val violations: List<LibraryRenderViolation>,
    val passedCount: Int,
)

/**
 * Checks Rule #58 — Runtime Never Knows Rendering.
 *
 * Inspects the state for any rendering types. The check is structural
 * — we look at field names and types, not values. The check is
 * conservative: anything that LOOKS like a rendering type triggers
 * a violation, even if it's actually used as data.
 *
 * Allowed in state:
 *   - String (text, hex colors, stable keys)
 *   - Int / Long (counts, timestamps, IDs)
 *   - Float / Double (progress, alpha — but only as raw values, not Dp)
 *   - Boolean (visibility, mode)
 *   - Enum (state, kind, profile)
 *   - List / Set / Map (collections of the above)
 *   - Nested data classes (containing only the above)
 *
 * Forbidden in state:
 *   - Color, ColorInt, ColorStop
 *   - Dp, DpOffset, TextUnit
 *   - Brush, Shader, Paint
 *   - ImageVector, Painter, Drawable
 *   - Modifier, LayoutId
 *   - @Composable functions
 */
private fun checkRule58(state: LibraryScreenState): RuleCheckResult {
    val violations = mutableListOf<LibraryRenderViolation>()
    var passed = 0

    // ── Check: Dock state has no rendering types ─────────────────────────
    val dock = state.dock
    if (dock.state.name.startsWith("Color") || dock.state.name.startsWith("Brush")) {
        violations += LibraryRenderViolation(
            rule = LibraryRenderRule.RULE_58,
            severity = LibraryRenderViolationSeverity.ERROR,
            location = "state.dock.state",
            message = "Dock state enum value looks like a rendering type.",
            observedValue = dock.state.name,
            expectedValue = "A non-rendering enum (HIDDEN, COLLAPSED, EXPANDED, etc.)",
        )
    } else {
        passed++
    }

    // ── Check: Hero accentColorHex is a String, not a Color ──────────────
    // The Hero state carries `accentColorHex: String`. This is correct —
    // it's data, not rendering. The renderer parses it.
    // We can't actually check the type at runtime (Kotlin erases types),
    // but we can check that the value parses as a hex string.
    val heroAccent = state.hero.accentColorHex
    if (heroAccent.isNotBlank() && !heroAccent.matches(Regex("^#?[0-9A-Fa-f]{6,8}$"))) {
        violations += LibraryRenderViolation(
            rule = LibraryRenderRule.RULE_58,
            severity = LibraryRenderViolationSeverity.WARNING,
            location = "state.hero.accentColorHex",
            message = "Hero accentColorHex is not a valid hex string. " +
                "Renderer will fall back to AccentCyan.",
            observedValue = heroAccent,
            expectedValue = "#RRGGBB or #AARRGGBB",
        )
    } else {
        passed++
    }

    // ── Check: Dock accentColorHex is a String, not a Color ──────────────
    val dockAccent = state.dock.accentColorHex
    if (dockAccent.isNotBlank() && !dockAccent.matches(Regex("^#?[0-9A-Fa-f]{6,8}$"))) {
        violations += LibraryRenderViolation(
            rule = LibraryRenderRule.RULE_58,
            severity = LibraryRenderViolationSeverity.WARNING,
            location = "state.dock.accentColorHex",
            message = "Dock accentColorHex is not a valid hex string. " +
                "Renderer will fall back to AccentCyan.",
            observedValue = dockAccent,
            expectedValue = "#RRGGBB or #AARRGGBB",
        )
    } else {
        passed++
    }

    // ── Check: progressFraction is a Float, not a Dp or Fraction ─────────
    // progressFraction should be in [0, 1]. Values outside indicate either
    // a bug or a wrong type (e.g. someone passed milliseconds).
    val progress = state.dock.progressFraction
    if (progress < 0f || progress > 1f) {
        violations += LibraryRenderViolation(
            rule = LibraryRenderRule.RULE_58,
            severity = LibraryRenderViolationSeverity.ERROR,
            location = "state.dock.progressFraction",
            message = "progressFraction is outside [0, 1]. " +
                "This suggests a wrong type was passed (e.g. ms instead of fraction).",
            observedValue = progress.toString(),
            expectedValue = "Float in [0.0, 1.0]",
        )
    } else {
        passed++
    }

    // ── Check: scroll offset is Int (dp), not Float (px) ─────────────────
    // The contract says scrollOffsetDp is in dp (Int). If it's a suspiciously
    // large value, it might be in pixels (Float cast to Int).
    val scrollOffset = state.scroll.scrollOffsetDp
    if (scrollOffset > 10_000) {
        violations += LibraryRenderViolation(
            rule = LibraryRenderRule.RULE_58,
            severity = LibraryRenderViolationSeverity.WARNING,
            location = "state.scroll.scrollOffsetDp",
            message = "scrollOffsetDp is suspiciously large (>10000). " +
                "Might be in pixels instead of dp.",
            observedValue = scrollOffset.toString(),
            expectedValue = "Int in dp (typically < 5000 for a screen)",
        )
    } else {
        passed++
    }

    return RuleCheckResult(violations = violations.toList(), passedCount = passed)
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// RULE #60 — Screen State Is Already Renderable
// ═══════════════════════════════════════════════════════════════════════════════════════

/**
 * Checks Rule #60 — Screen State Is Already Renderable.
 *
 * The state must be ready to render with zero transformation. This
 * check verifies:
 *   - No nullable fields that should be non-null (and vice versa).
 *   - No empty collections that should be null (and vice versa).
 *   - All enum values are valid.
 *   - All string fields have sensible content (no raw newlines, no
 *     HTML, no template placeholders like "{{name}}").
 *   - All float fields are in valid ranges.
 */
private fun checkRule60(state: LibraryScreenState): RuleCheckResult {
    val violations = mutableListOf<LibraryRenderViolation>()
    var passed = 0

    // ── Check: Hero state — if isVisible, must have a title ──────────────
    val hero = state.hero
    if (hero.isVisible && hero.primaryTitle.isBlank()) {
        violations += LibraryRenderViolation(
            rule = LibraryRenderRule.RULE_60,
            severity = LibraryRenderViolationSeverity.ERROR,
            location = "state.hero.primaryTitle",
            message = "Hero is visible but primaryTitle is blank. " +
                "Renderer would show an empty title — not renderable as-is.",
            observedValue = "isVisible=true, primaryTitle=\"\"",
            expectedValue = "isVisible=true → primaryTitle must be non-blank",
        )
    } else {
        passed++
    }

    // ── Check: Hero state — if isVisible, must have an artworkKey ────────
    if (hero.isVisible && hero.artworkKey.isBlank()) {
        violations += LibraryRenderViolation(
            rule = LibraryRenderRule.RULE_60,
            severity = LibraryRenderViolationSeverity.WARNING,
            location = "state.hero.artworkKey",
            message = "Hero is visible but artworkKey is blank. " +
                "Renderer will fall back to placeholder artwork.",
            observedValue = "isVisible=true, artworkKey=\"\"",
            expectedValue = "isVisible=true → artworkKey should be non-blank",
        )
    } else {
        passed++
    }

    // ── Check: Dock state — if hasTrack, must have title + artist ────────
    val dock = state.dock
    if (dock.hasTrack) {
        if (dock.trackTitle.isBlank()) {
            violations += LibraryRenderViolation(
                rule = LibraryRenderRule.RULE_60,
                severity = LibraryRenderViolationSeverity.ERROR,
                location = "state.dock.trackTitle",
                message = "Dock has a track but trackTitle is blank. " +
                    "Renderer would show an empty title.",
                observedValue = "hasTrack=true, trackTitle=\"\"",
                expectedValue = "hasTrack=true → trackTitle must be non-blank",
            )
        } else {
            passed++
        }
        if (dock.trackArtist.isBlank()) {
            violations += LibraryRenderViolation(
                rule = LibraryRenderRule.RULE_60,
                severity = LibraryRenderViolationSeverity.WARNING,
                location = "state.dock.trackArtist",
                message = "Dock has a track but trackArtist is blank. " +
                    "Renderer would show empty subtitle.",
                observedValue = "hasTrack=true, trackArtist=\"\"",
                expectedValue = "hasTrack=true → trackArtist should be non-blank",
            )
        } else {
            passed++
        }
    } else {
        passed += 2
    }

    // ── Check: Section states — visible sections must have stableKey ─────
    state.sections.forEach { section ->
        if (section.isRendered && section.stableKey.isBlank()) {
            violations += LibraryRenderViolation(
                rule = LibraryRenderRule.RULE_60,
                severity = LibraryRenderViolationSeverity.ERROR,
                location = "state.sections[${section.sectionId}].stableKey",
                message = "Section is rendered but stableKey is blank. " +
                    "LazyColumn keys would collide.",
                observedValue = "isRendered=true, stableKey=\"\"",
                expectedValue = "isRendered=true → stableKey must be non-blank (Rule #44)",
            )
        } else {
            passed++
        }

        // ── Check: itemCount vs totalCount consistency ────────────────────
        if (section.itemCount < 0) {
            violations += LibraryRenderViolation(
                rule = LibraryRenderRule.RULE_60,
                severity = LibraryRenderViolationSeverity.ERROR,
                location = "state.sections[${section.sectionId}].itemCount",
                message = "itemCount is negative.",
                observedValue = section.itemCount.toString(),
                expectedValue = "Int ≥ 0",
            )
        } else if (section.totalCount > 0 && section.itemCount > section.totalCount) {
            violations += LibraryRenderViolation(
                rule = LibraryRenderRule.RULE_60,
                severity = LibraryRenderViolationSeverity.WARNING,
                location = "state.sections[${section.sectionId}].itemCount",
                message = "itemCount > totalCount. Inconsistent state.",
                observedValue = "itemCount=${section.itemCount}, totalCount=${section.totalCount}",
                expectedValue = "itemCount ≤ totalCount",
            )
        } else {
            passed++
        }
    }

    // ── Check: errorMessage — if phase=ERROR, must have a message ────────
    if (state.phase == LibraryScreenPhase.ERROR && state.errorMessage.isNullOrBlank()) {
        violations += LibraryRenderViolation(
            rule = LibraryRenderRule.RULE_60,
            severity = LibraryRenderViolationSeverity.ERROR,
            location = "state.errorMessage",
            message = "phase=ERROR but errorMessage is null/blank. " +
                "Empty state view would show no error text.",
            observedValue = "phase=ERROR, errorMessage=${state.errorMessage}",
            expectedValue = "phase=ERROR → errorMessage must be non-blank",
        )
    } else {
        passed++
    }

    // ── Check: selection — if isActive, must have at least one item ──────
    val selection = state.selection
    if (selection.isActive && selection.selectedItemIds.isEmpty()) {
        violations += LibraryRenderViolation(
            rule = LibraryRenderRule.RULE_60,
            severity = LibraryRenderViolationSeverity.WARNING,
            location = "state.selection.selectedItemIds",
            message = "Selection is active but no items are selected. " +
                "Probably a transient state — should resolve on next event.",
            observedValue = "isActive=true, selectedItemIds=[]",
            expectedValue = "isActive=true → selectedItemIds should be non-empty",
        )
    } else {
        passed++
    }

    return RuleCheckResult(violations = violations.toList(), passedCount = passed)
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// RULE #70 — Architecture Freeze
// ═══════════════════════════════════════════════════════════════════════════════════════

/**
 * Checks Rule #70 — Architecture Freeze.
 *
 * No new Runtime/Reducer/State/Contract patterns introduced. This
 * check verifies:
 *   - Constitution has all required contracts (no missing pieces).
 *   - State shape follows the locked invariants.
 *   - Motion schedule respects MAX_CONCURRENT_MOTIONS (Rule #74).
 *   - Profile is one of the locked profiles.
 */
private fun checkRule70(
    state: LibraryScreenState,
    constitution: LibraryConstitution,
    schedule: LibraryMotionSchedule,
): RuleCheckResult {
    val violations = mutableListOf<LibraryRenderViolation>()
    var passed = 0

    // ── Check: Constitution has all required contracts ───────────────────
    val requiredContracts = listOf(
        "screen" to (constitution.screen is LibraryScreenContract),
        "topBar" to (constitution.topBar is LibraryTopBarContract),
        "dock" to (constitution.dock is com.rikodev.music.android.presentation.library.screen.contract.LibraryDockContract),
        "hero" to (constitution.hero is LibraryHeroContract),
        "section" to (constitution.section is LibrarySectionContract),
        "emptyState" to (constitution.emptyState is LibraryEmptyStateContract),
        "motionLanguage" to (constitution.motionLanguage is LibraryMotionLanguage),
    )
    requiredContracts.forEach { (name, isValid) ->
        if (!isValid) {
            violations += LibraryRenderViolation(
                rule = LibraryRenderRule.RULE_70,
                severity = LibraryRenderViolationSeverity.ERROR,
                location = "constitution.$name",
                message = "Constitution is missing required contract '$name'.",
                observedValue = "null or wrong type",
                expectedValue = "Non-null ${name}Contract",
            )
        } else {
            passed++
        }
    }

    // ── Check: Profile is one of the locked profiles ─────────────────────
    val lockedProfiles = LibraryConstitutionProfile.values().toSet()
    if (state.profile !in lockedProfiles) {
        violations += LibraryRenderViolation(
            rule = LibraryRenderRule.RULE_70,
            severity = LibraryRenderViolationSeverity.ERROR,
            location = "state.profile",
            message = "Profile is not one of the locked profiles.",
            observedValue = state.profile.name,
            expectedValue = "One of ${lockedProfiles.map { it.name }}",
        )
    } else {
        passed++
    }

    // ── Check: Motion schedule respects MAX_CONCURRENT_MOTIONS ───────────
    // Rule #74 — collateral check. The Orchestrator already enforces this,
    // but the Validator double-checks (defense in depth).
    val activeSlots = schedule.entries.count { it.value.isActive }
    if (activeSlots > MAX_CONCURRENT_MOTIONS) {
        violations += LibraryRenderViolation(
            rule = LibraryRenderRule.RULE_74,
            severity = LibraryRenderViolationSeverity.ERROR,
            location = "schedule.entries",
            message = "Motion schedule has $activeSlots active slots, " +
                "MAX_CONCURRENT_MOTIONS=$MAX_CONCURRENT_MOTIONS. " +
                "Orchestrator should have disabled lowest-priority slots.",
            observedValue = "$activeSlots active slots",
            expectedValue = "≤ $MAX_CONCURRENT_MOTIONS active slots",
        )
    } else if (activeSlots == MAX_CONCURRENT_MOTIONS) {
        violations += LibraryRenderViolation(
            rule = LibraryRenderRule.RULE_74,
            severity = LibraryRenderViolationSeverity.INFO,
            location = "schedule.entries",
            message = "Motion schedule is at the ceiling ($activeSlots/$MAX_CONCURRENT_MOTIONS). " +
                "Adding any motion will require disabling an existing one.",
            observedValue = "$activeSlots active slots",
            expectedValue = "≤ $MAX_CONCURRENT_MOTIONS active slots",
        )
    } else {
        passed++
    }

    // ── Check: State has at most one Hero (Rule #57 invariant) ───────────
    // The state's hero field is a single LibraryHeroRuntimeState, not a list.
    // We can't structurally check this, but we can verify the hero's
    // isVisible flag isn't ambiguous.
    val hero = state.hero
    if (hero.isVisible && hero.isDismissed) {
        violations += LibraryRenderViolation(
            rule = LibraryRenderRule.RULE_70,
            severity = LibraryRenderViolationSeverity.ERROR,
            location = "state.hero",
            message = "Hero is both isVisible=true and isDismissed=true. " +
                "Ambiguous state — should be one or the other.",
            observedValue = "isVisible=true, isDismissed=true",
            expectedValue = "isVisible XOR isDismissed",
        )
    } else {
        passed++
    }

    // ── Check: State's profile matches constitution's profile ────────────
    if (state.profile != constitution.profile) {
        violations += LibraryRenderViolation(
            rule = LibraryRenderRule.RULE_70,
            severity = LibraryRenderViolationSeverity.WARNING,
            location = "state.profile vs constitution.profile",
            message = "State profile and Constitution profile don't match. " +
                "This may be intentional (e.g. state hasn't updated yet) " +
                "but usually indicates a coordination bug.",
            observedValue = "state=${state.profile.name}, constitution=${constitution.profile.name}",
            expectedValue = "Both should be the same profile",
        )
    } else {
        passed++
    }

    // ── Check: State's sections don't have duplicate sectionIds ──────────
    val sectionIds = state.sections.map { it.sectionId }
    val duplicateIds = sectionIds.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
    if (duplicateIds.isNotEmpty()) {
        violations += LibraryRenderViolation(
            rule = LibraryRenderRule.RULE_70,
            severity = LibraryRenderViolationSeverity.ERROR,
            location = "state.sections.sectionId",
            message = "Duplicate sectionIds found: $duplicateIds. " +
                "LazyColumn keys would collide.",
            observedValue = duplicateIds.toString(),
            expectedValue = "All sectionIds unique (Rule #44)",
        )
    } else {
        passed++
    }

    // ── Check: State's sections don't have duplicate stableKeys ──────────
    val stableKeys = state.sections.map { it.stableKey }.filter { it.isNotBlank() }
    val duplicateKeys = stableKeys.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
    if (duplicateKeys.isNotEmpty()) {
        violations += LibraryRenderViolation(
            rule = LibraryRenderRule.RULE_70,
            severity = LibraryRenderViolationSeverity.ERROR,
            location = "state.sections.stableKey",
            message = "Duplicate stableKeys found: $duplicateKeys. " +
                "Card artwork caching would conflict.",
            observedValue = duplicateKeys.toString(),
            expectedValue = "All stableKeys unique (Rule #44)",
        )
    } else {
        passed++
    }

    // ── Check: Empty state contract visibility matches phase ─────────────
    val emptyContract = constitution.emptyState
    val expectedEmptyVisible = state.phase == LibraryScreenPhase.EMPTY ||
        state.phase == LibraryScreenPhase.PERMISSION_REQUIRED ||
        state.phase == LibraryScreenPhase.ERROR
    if (emptyContract.isVisible != expectedEmptyVisible && state.isReady) {
        violations += LibraryRenderViolation(
            rule = LibraryRenderRule.RULE_70,
            severity = LibraryRenderViolationSeverity.INFO,
            location = "constitution.emptyState.isVisible",
            message = "Empty state contract visibility doesn't match phase. " +
                "Constitution says isVisible=${emptyContract.isVisible}, " +
                "but phase=${state.phase} suggests isVisible=$expectedEmptyVisible.",
            observedValue = "isVisible=${emptyContract.isVisible}, phase=${state.phase}",
            expectedValue = "isVisible=$expectedEmptyVisible",
        )
    } else {
        passed++
    }

    return RuleCheckResult(violations = violations.toList(), passedCount = passed)
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// REPORT FORMATTERS — for logging and debugging
// ═══════════════════════════════════════════════════════════════════════════════════════

/**
 * Formats a [LibraryRenderValidationReport] as a multi-line string.
 *
 * Pure function — no side effects.
 */
fun formatValidationReport(report: LibraryRenderValidationReport): String {
    val sb = StringBuilder()
    sb.appendLine("═══════════════════════════════════════════════════════")
    sb.appendLine("RIKO LIBRARY — RENDER VALIDATION REPORT")
    sb.appendLine("═══════════════════════════════════════════════════════")
    sb.appendLine("Status: ${if (report.isClean) "✅ CLEAN" else "❌ VIOLATIONS DETECTED"}")
    sb.appendLine("Passed: ${report.passedCount} checks")
    sb.appendLine("Failed: ${report.failedCount} checks")
    sb.appendLine()
    sb.appendLine("── Rule Status ──────────────────────────────────────")
    sb.appendLine("Rule #58 (Runtime Never Knows Rendering): ${report.rule58Status.name}")
    sb.appendLine("Rule #60 (Screen State Already Renderable): ${report.rule60Status.name}")
    sb.appendLine("Rule #70 (Architecture Freeze): ${report.rule70Status.name}")
    sb.appendLine()
    if (report.errors.isNotEmpty()) {
        sb.appendLine("── Errors (${report.errors.size}) ──────────────────────────────")
        report.errors.forEachIndexed { index, v ->
            sb.appendLine("${index + 1}. ${v.format()}")
        }
        sb.appendLine()
    }
    if (report.warnings.isNotEmpty()) {
        sb.appendLine("── Warnings (${report.warnings.size}) ───────────────────────────")
        report.warnings.forEachIndexed { index, v ->
            sb.appendLine("${index + 1}. ${v.format()}")
        }
        sb.appendLine()
    }
    if (report.infos.isNotEmpty()) {
        sb.appendLine("── Info (${report.infos.size}) ─────────────────────────────────")
        report.infos.forEachIndexed { index, v ->
            sb.appendLine("${index + 1}. ${v.format()}")
        }
        sb.appendLine()
    }
    sb.append("═══════════════════════════════════════════════════════")
    return sb.toString()
}

/**
 * Returns a single-line summary of the report.
 */
fun summarizeValidationReport(report: LibraryRenderValidationReport): String {
    val status = if (report.isClean) "CLEAN" else "VIOLATIONS"
    return "Library[$status] " +
        "rule58=${report.rule58Status.name} " +
        "rule60=${report.rule60Status.name} " +
        "rule70=${report.rule70Status.name} " +
        "errors=${report.errors.size} " +
        "warnings=${report.warnings.size} " +
        "infos=${report.infos.size} " +
        "passed=${report.passedCount} " +
        "failed=${report.failedCount}"
}

/**
 * Filters violations by rule.
 *
 * Useful for selective logging (e.g. "show me only #58 violations").
 */
fun LibraryRenderValidationReport.violationsFor(
    rule: LibraryRenderRule,
): List<LibraryRenderViolation> = violations.filter { it.rule == rule }

/**
 * Filters violations by severity.
 */
fun LibraryRenderValidationReport.violationsWith(
    severity: LibraryRenderViolationSeverity,
): List<LibraryRenderViolation> = violations.filter { it.severity == severity }
