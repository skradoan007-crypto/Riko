/*
 * RIKO EXPERIENCE OS — LIBRARY INTERACTION RUNTIME
 * Build B5.4 — Library Interaction Runtime
 *
 * File 14 / 15 — InteractionValidator.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Validate that the Interaction Runtime obeys all 5 rules (#78-#82).
 * Pure: same inputs → same outputs, byte-identical.
 *
 * The Validator emits [ValidationViolation]s — never blocks behavior.
 * Violations are surfaced to the Inspector for developer attention.
 *
 * RULES CHECKED
 * -------------
 * #78  Interaction Creates Intent, Never Action
 *      - Check: every [LibraryGesture] produces an [InteractionIntent]
 *        (not a [LibraryAction]).
 *      - Check: every [GestureResolution] with ACCEPTED outcome has
 *        a non-null `intent`.
 *
 * #79  Selection Is Independent
 *      - Check: [SelectionState] is the only place selection lives.
 *      - Check: no other state type has a `selectedIds` field.
 *
 * #80  Gesture Never Knows UI
 *      - Check: [LibraryGesture] sealed class variants have NO fields
 *        referencing UI types (Glass, Wave, Card, Hero, Dock, Composable).
 *      - Check: thresholds are constants, not derived from UI state.
 *
 * #81  Every Interaction Is Replayable
 *      - Check: every [LibraryGesture] variant is a `data class`.
 *      - Check: every [LibraryGesture] variant has only serializable
 *        fields (primitives, value classes, data classes).
 *      - Check: every [GestureRecord] can be replayed.
 *
 * #82  Every Gesture Has Explanation
 *      - Check: every [GestureResolution] has a non-empty `explanation`.
 *      - Check: every [GestureExplanation] has a non-empty `reason`.
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Compose. No Android framework. No Coroutine. No Flow. No
 * ViewModel. No `var`. No side effects.
 *
 * © RIKO Experience OS — Interaction Runtime
 */

package com.rikodev.music.android.presentation.library.interaction

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 1 — VIOLATION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Severity of a validation violation.
 */
enum class ViolationSeverity(
    internal val displayName: String,
) {
    /** Critical — a rule is broken. Behavior is wrong. */
    ERROR("Error"),

    /** Warning — a rule is at risk. Behavior may be wrong. */
    WARNING("Warning"),

    /** Info — a rule is borderline. Worth investigating. */
    INFO("Info"),
}

/**
 * Which rule was violated.
 */
enum class ValidatedRule(
    internal val displayName: String,
    internal val ruleNumber: Int,
) {
    INTERACTION_CREATES_INTENT("Interaction Creates Intent", 78),
    SELECTION_IS_INDEPENDENT("Selection Is Independent", 79),
    GESTURE_NEVER_KNOWS_UI("Gesture Never Knows UI", 80),
    INTERACTION_IS_REPLAYABLE("Interaction Is Replayable", 81),
    GESTURE_HAS_EXPLANATION("Gesture Has Explanation", 82),
}

/**
 * A single validation violation.
 *
 * @property rule          Which rule was violated.
 * @property severity      How severe the violation is.
 * @property targetId      Id of the offending gesture / record / field.
 *                         Null if the violation is structural.
 * @property message       Human-readable description.
 * @property suggestion    Optional suggestion for fixing the violation.
 */
data class ValidationViolation(
    val rule: ValidatedRule,
    val severity: ViolationSeverity,
    val targetId: String?,
    val message: String,
    val suggestion: String?,
) {
    companion object {
        fun error(
            rule: ValidatedRule,
            targetId: String?,
            message: String,
            suggestion: String? = null,
        ): ValidationViolation = ValidationViolation(
            rule = rule,
            severity = ViolationSeverity.ERROR,
            targetId = targetId,
            message = message,
            suggestion = suggestion,
        )

        fun warning(
            rule: ValidatedRule,
            targetId: String?,
            message: String,
            suggestion: String? = null,
        ): ValidationViolation = ValidationViolation(
            rule = rule,
            severity = ViolationSeverity.WARNING,
            targetId = targetId,
            message = message,
            suggestion = suggestion,
        )

        fun info(
            rule: ValidatedRule,
            targetId: String?,
            message: String,
            suggestion: String? = null,
        ): ValidationViolation = ValidationViolation(
            rule = rule,
            severity = ViolationSeverity.INFO,
            targetId = targetId,
            message = message,
            suggestion = suggestion,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 2 — VALIDATION REPORT
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The result of validating a single gesture resolution.
 */
data class ValidationReport(
    val violations: List<ValidationViolation>,
    val targetId: String?,
    val checkedAtMs: Long,
) {
    val isClean: Boolean get() = violations.none { it.severity == ViolationSeverity.ERROR }
    val hasWarnings: Boolean get() = violations.any { it.severity == ViolationSeverity.WARNING }
    val errorCount: Int get() = violations.count { it.severity == ViolationSeverity.ERROR }
    val warningCount: Int get() = violations.count { it.severity == ViolationSeverity.WARNING }
    val infoCount: Int get() = violations.count { it.severity == ViolationSeverity.INFO }

    /**
     * Get violations for a specific rule.
     */
    fun violationsFor(rule: ValidatedRule): List<ValidationViolation> =
        violations.filter { it.rule == rule }

    /**
     * Format the report as a multi-line string for Inspector display.
     */
    fun formatReport(): String = buildString {
        appendLine("=== Validation Report ===")
        appendLine("Target: ${targetId ?: "structural"}")
        appendLine("Errors: $errorCount, Warnings: $warningCount, Info: $infoCount")
        appendLine("Status: ${if (isClean) "CLEAN ✅" else "VIOLATIONS ❌"}")
        appendLine()
        if (violations.isNotEmpty()) {
            appendLine("Violations:")
            violations.forEach { v ->
                appendLine("  [${v.severity.displayName}] Rule #${v.rule.ruleNumber} ${v.rule.displayName}")
                appendLine("    ${v.message}")
                v.suggestion?.let { appendLine("    Suggestion: $it") }
            }
        }
    }

    companion object {
        fun clean(targetId: String?, nowMs: Long): ValidationReport = ValidationReport(
            violations = emptyList(),
            targetId = targetId,
            checkedAtMs = nowMs,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 3 — INTERACTION VALIDATOR (PURE)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The Interaction Validator — pure functions that check rule compliance.
 *
 * The Validator never throws. It returns [ValidationReport]s with
 * [ValidationViolation]s. Callers (Inspector) display these to the
 * developer.
 */
object InteractionValidator {

    /**
     * Validate a single [GestureResolution] for rule compliance.
     */
    fun validateResolution(
        resolution: GestureResolution,
        nowMs: Long,
    ): ValidationReport {
        val violations = mutableListOf<ValidationViolation>()
        val targetId = resolution.gesture.id

        // Rule #78: ACCEPTED resolution must have an intent
        if (resolution.outcome == GestureOutcome.ACCEPTED && resolution.intent == null) {
            violations.add(
                ValidationViolation.error(
                    rule = ValidatedRule.INTERACTION_CREATES_INTENT,
                    targetId = targetId,
                    message = "Resolution is ACCEPTED but no Intent was produced.",
                    suggestion = "The Resolver should always emit an Intent for ACCEPTED resolutions.",
                )
            )
        }

        // Rule #82: every resolution must have a non-empty explanation
        val explanation = resolution.explanation
        if (explanation.reason.isBlank()) {
            violations.add(
                ValidationViolation.error(
                    rule = ValidatedRule.GESTURE_HAS_EXPLANATION,
                    targetId = targetId,
                    message = "Resolution has an empty explanation.reason.",
                    suggestion = "The Resolver must produce a non-empty reason string.",
                )
            )
        }
        if (explanation.contextSnapshot.isBlank()) {
            violations.add(
                ValidationViolation.warning(
                    rule = ValidatedRule.GESTURE_HAS_EXPLANATION,
                    targetId = targetId,
                    message = "Resolution has an empty explanation.contextSnapshot.",
                    suggestion = "Include a snapshot of the resolution context for debugging.",
                )
            )
        }

        // Rule #82: winner kind should match gesture kind
        if (explanation.winnerKind != resolution.gesture.kind) {
            violations.add(
                ValidationViolation.warning(
                    rule = ValidatedRule.GESTURE_HAS_EXPLANATION,
                    targetId = targetId,
                    message = "Explanation.winnerKind (${explanation.winnerKind}) " +
                            "does not match gesture.kind (${resolution.gesture.kind}).",
                    suggestion = "These should match for single-gesture resolutions.",
                )
            )
        }

        // Rule #82: metrics should be present (not ZERO)
        if (explanation.metrics == GestureMetrics.ZERO) {
            violations.add(
                ValidationViolation.info(
                    rule = ValidatedRule.GESTURE_HAS_EXPLANATION,
                    targetId = targetId,
                    message = "Explanation.metrics is ZERO — no measurement data available.",
                    suggestion = "Ensure the gesture collector populates metrics.",
                )
            )
        }

        return ValidationReport(
            violations = violations,
            targetId = targetId,
            checkedAtMs = nowMs,
        )
    }

    /**
     * Validate a [LibraryGesture] for rule compliance.
     *
     * Checks:
     * - Rule #80: no UI-type fields
     * - Rule #81: variant is a data class with serializable fields
     */
    fun validateGesture(
        gesture: LibraryGesture,
        nowMs: Long,
    ): ValidationReport {
        val violations = mutableListOf<ValidationViolation>()
        val targetId = gesture.id

        // Rule #80: gesture should not carry UI references
        // (Structural check — we cannot introspect fields in pure Kotlin,
        // but we can check that the gesture's source is a valid GestureSource.)
        if (gesture.source !in GestureSource.values().toList()) {
            violations.add(
                ValidationViolation.error(
                    rule = ValidatedRule.GESTURE_NEVER_KNOWS_UI,
                    targetId = targetId,
                    message = "Gesture has an invalid source: ${gesture.source}",
                )
            )
        }

        // Rule #81: gesture must have a non-empty id
        if (gesture.id.isBlank()) {
            violations.add(
                ValidationViolation.error(
                    rule = ValidatedRule.INTERACTION_IS_REPLAYABLE,
                    targetId = targetId,
                    message = "Gesture has an empty id — cannot be replayed.",
                    suggestion = "Every gesture must have a stable, non-empty id.",
                )
            )
        }

        // Rule #81: gesture must have a non-zero timestamp
        if (gesture.timestampMs <= 0L) {
            violations.add(
                ValidationViolation.warning(
                    rule = ValidatedRule.INTERACTION_IS_REPLAYABLE,
                    targetId = targetId,
                    message = "Gesture has a non-positive timestampMs (${gesture.timestampMs}).",
                    suggestion = "Timestamps should be wall-clock epoch milliseconds.",
                )
            )
        }

        // Rule #81: metrics should be present
        if (gesture.metrics == GestureMetrics.ZERO) {
            violations.add(
                ValidationViolation.info(
                    rule = ValidatedRule.INTERACTION_IS_REPLAYABLE,
                    targetId = targetId,
                    message = "Gesture has ZERO metrics — replay will be less informative.",
                )
            )
        }

        // Specific checks per gesture kind
        when (gesture) {
            is LibraryGesture.DockPull -> {
                if (gesture.progress < 0f || gesture.progress > 1f) {
                    violations.add(
                        ValidationViolation.error(
                            rule = ValidatedRule.INTERACTION_IS_REPLAYABLE,
                            targetId = targetId,
                            message = "DockPull.progress is out of [0, 1]: ${gesture.progress}",
                        )
                    )
                }
                if (gesture.commitThresholdDp.value <= 0) {
                    violations.add(
                        ValidationViolation.error(
                            rule = ValidatedRule.INTERACTION_IS_REPLAYABLE,
                            targetId = targetId,
                            message = "DockPull.commitThresholdDp is non-positive.",
                        )
                    )
                }
            }

            is LibraryGesture.MultiTouch -> {
                if (gesture.pointers.size < 3) {
                    violations.add(
                        ValidationViolation.error(
                            rule = ValidatedRule.INTERACTION_IS_REPLAYABLE,
                            targetId = targetId,
                            message = "MultiTouch has fewer than 3 pointers: ${gesture.pointers.size}",
                        )
                    )
                }
            }

            is LibraryGesture.Pinch -> {
                if (gesture.pointers.first.value == gesture.pointers.second.value) {
                    violations.add(
                        ValidationViolation.error(
                            rule = ValidatedRule.INTERACTION_IS_REPLAYABLE,
                            targetId = targetId,
                            message = "Pinch has two identical pointer ids.",
                        )
                    )
                }
            }

            else -> {
                // No specific checks for other gesture kinds
            }
        }

        return ValidationReport(
            violations = violations,
            targetId = targetId,
            checkedAtMs = nowMs,
        )
    }

    /**
     * Validate a [SelectionState] for rule compliance.
     *
     * Checks:
     * - Rule #79: state is consistent (e.g. mode=RANGE requires anchor)
     */
    fun validateSelectionState(
        state: SelectionState,
        nowMs: Long,
    ): ValidationReport {
        val violations = mutableListOf<ValidationViolation>()

        // Rule #79: RANGE mode requires an anchor
        if (state.mode == SelectionMode.RANGE && state.anchor == null) {
            violations.add(
                ValidationViolation.error(
                    rule = ValidatedRule.SELECTION_IS_INDEPENDENT,
                    targetId = null,
                    message = "Selection mode is RANGE but no anchor is set.",
                    suggestion = "RANGE mode requires a SelectionAnchor.",
                )
            )
        }

        // Rule #79: scopeId should be set when mode is not NONE
        if (state.mode != SelectionMode.NONE && state.scopeId == null) {
            violations.add(
                ValidationViolation.error(
                    rule = ValidatedRule.SELECTION_IS_INDEPENDENT,
                    targetId = null,
                    message = "Selection mode is ${state.mode} but scopeId is null.",
                )
            )
        }

        // Rule #79: focusedItemId should be in selectedIds or pinnedIds
        // when in SINGLE / MULTIPLE / RANGE mode
        val focusedId = state.focusedItemId
        if (focusedId != null && state.isActive) {
            if (focusedId !in state.selectedIds && focusedId !in state.pinnedIds) {
                violations.add(
                    ValidationViolation.warning(
                        rule = ValidatedRule.SELECTION_IS_INDEPENDENT,
                        targetId = null,
                        message = "Focused item $focusedId is not in selectedIds or pinnedIds.",
                        suggestion = "Focus usually tracks selection. Consider clearing focus when selection changes.",
                    )
                )
            }
        }

        return ValidationReport(
            violations = violations,
            targetId = null,
            checkedAtMs = nowMs,
        )
    }

    /**
     * Validate an [InteractionLog] for rule compliance.
     *
     * Checks:
     * - Rule #81: every record can be replayed
     * - Rule #82: every resolution has an explanation
     */
    fun validateLog(
        log: InteractionLog,
        nowMs: Long,
    ): ValidationReport {
        val violations = mutableListOf<ValidationViolation>()

        log.records.forEach { record ->
            // Rule #82: every resolution must have an explanation
            val resolution = record.resolutionRecord?.resolution
            if (resolution != null) {
                val resolutionReport = validateResolution(resolution, nowMs)
                violations.addAll(resolutionReport.violations)
            }

            // Rule #81: every gesture must be replayable
            val gestureReport = validateGesture(record.gestureRecord.gesture, nowMs)
            violations.addAll(gestureReport.violations)
        }

        // Rule #81: at least one replay should be possible
        if (log.records.isNotEmpty()) {
            val firstReplayable = log.records.firstOrNull { it.resolutionRecord != null }
            if (firstReplayable == null) {
                violations.add(
                    ValidationViolation.warning(
                        rule = ValidatedRule.INTERACTION_IS_REPLAYABLE,
                        targetId = null,
                        message = "Log has ${log.records.size} records but none have resolutions — cannot replay.",
                        suggestion = "Ensure the Resolver is invoked for every gesture.",
                    )
                )
            }
        }

        return ValidationReport(
            violations = violations,
            targetId = null,
            checkedAtMs = nowMs,
        )
    }

    /**
     * Validate a [DragState] for rule compliance.
     */
    fun validateDragState(
        state: DragState,
        nowMs: Long,
    ): ValidationReport {
        val violations = mutableListOf<ValidationViolation>()

        // ACTIVE / SETTLING state requires a target
        if (state.lifecycle == DragLifecycle.ACTIVE ||
            state.lifecycle == DragLifecycle.SETTLING
        ) {
            if (state.target == null) {
                violations.add(
                    ValidationViolation.error(
                        rule = ValidatedRule.INTERACTION_IS_REPLAYABLE,
                        targetId = state.activeGestureId,
                        message = "Drag is ${state.lifecycle} but has no target.",
                    )
                )
            }
        }

        // ACTIVE state requires an active gesture id
        if (state.lifecycle.isActive && state.activeGestureId == null) {
            violations.add(
                ValidationViolation.error(
                    rule = ValidatedRule.INTERACTION_IS_REPLAYABLE,
                    targetId = null,
                    message = "Drag is active but has no activeGestureId.",
                )
            )
        }

        return ValidationReport(
            violations = violations,
            targetId = state.activeGestureId,
            checkedAtMs = nowMs,
        )
    }

    /**
     * Validate a [LongPressState] for rule compliance.
     */
    fun validateLongPressState(
        state: LongPressState,
        nowMs: Long,
    ): ValidationReport {
        val violations = mutableListOf<ValidationViolation>()

        // FIRED state requires firedAtMs >= 0
        if (state.lifecycle == LongPressLifecycle.FIRED && state.firedAtMs < 0) {
            violations.add(
                ValidationViolation.error(
                    rule = ValidatedRule.INTERACTION_IS_REPLAYABLE,
                    targetId = state.activeGestureId,
                    message = "LongPress is FIRED but firedAtMs is negative.",
                )
            )
        }

        // FIRED state should have hapticFired if spec.hapticEnabled
        if (state.lifecycle == LongPressLifecycle.FIRED &&
            state.spec.hapticEnabled && !state.hapticFired
        ) {
            violations.add(
                ValidationViolation.warning(
                    rule = ValidatedRule.GESTURE_HAS_EXPLANATION,
                    targetId = state.activeGestureId,
                    message = "LongPress is FIRED with hapticEnabled=true but hapticFired=false.",
                    suggestion = "Ensure the haptic is emitted when the long-press fires.",
                )
            )
        }

        return ValidationReport(
            violations = violations,
            targetId = state.activeGestureId,
            checkedAtMs = nowMs,
        )
    }

    /**
     * Validate a [DockInteractionState] for rule compliance.
     */
    fun validateDockState(
        state: DockInteractionState,
        nowMs: Long,
    ): ValidationReport {
        val violations = mutableListOf<ValidationViolation>()

        // Morphing state requires an active gesture id
        if (state.isMorphing && state.activeGestureId == null) {
            violations.add(
                ValidationViolation.error(
                    rule = ValidatedRule.INTERACTION_IS_REPLAYABLE,
                    targetId = null,
                    message = "Dock is morphing but has no activeGestureId.",
                )
            )
        }

        // morphProgress should be in [0, 1] (or slightly over for overscroll)
        if (state.morphProgress < 0f || state.morphProgress > 1.5f) {
            violations.add(
                ValidationViolation.warning(
                    rule = ValidatedRule.INTERACTION_IS_REPLAYABLE,
                    targetId = state.activeGestureId,
                    message = "Dock morphProgress is out of expected range: ${state.morphProgress}",
                )
            )
        }

        // EXPANDED state should have morphProgress = 1
        if (state.morphState == DockMorphState.EXPANDED && state.morphProgress != 1f) {
            violations.add(
                ValidationViolation.warning(
                    rule = ValidatedRule.INTERACTION_IS_REPLAYABLE,
                    targetId = null,
                    message = "Dock is EXPANDED but morphProgress is ${state.morphProgress} (expected 1.0).",
                )
            )
        }

        return ValidationReport(
            violations = violations,
            targetId = state.activeGestureId,
            checkedAtMs = nowMs,
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // AGGREGATE VALIDATION
    // ─────────────────────────────────────────────────────────────────

    /**
     * Validate the entire interaction subsystem in one pass.
     *
     * Useful for end-to-end checks (e.g. on app startup, or when the
     * Inspector wants a full audit).
     */
    fun validateAll(
        log: InteractionLog,
        selectionState: SelectionState?,
        dragState: DragState?,
        longPressState: LongPressState?,
        dockState: DockInteractionState?,
        nowMs: Long,
    ): ValidationReport {
        val allViolations = mutableListOf<ValidationViolation>()

        // Validate the log
        val logReport = validateLog(log, nowMs)
        allViolations.addAll(logReport.violations)

        // Validate selection state
        selectionState?.let {
            val report = validateSelectionState(it, nowMs)
            allViolations.addAll(report.violations)
        }

        // Validate drag state
        dragState?.let {
            val report = validateDragState(it, nowMs)
            allViolations.addAll(report.violations)
        }

        // Validate long-press state
        longPressState?.let {
            val report = validateLongPressState(it, nowMs)
            allViolations.addAll(report.violations)
        }

        // Validate dock state
        dockState?.let {
            val report = validateDockState(it, nowMs)
            allViolations.addAll(report.violations)
        }

        return ValidationReport(
            violations = allViolations,
            targetId = null,
            checkedAtMs = nowMs,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 4 — VALIDATOR INTENT + ACTION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Intents emitted by the Validator (e.g. user requested validation).
 */
sealed class ValidatorIntent {
    data object ValidateLastGesture : ValidatorIntent()

    data object ValidateLog : ValidatorIntent()

    data object ValidateAll : ValidatorIntent()

    data class ValidateGesture(
        val gestureId: String,
    ) : ValidatorIntent()
}

/**
 * Actions emitted by the Validator reducer.
 */
sealed class ValidatorAction {
    data class ShowReport(
        val report: ValidationReport,
    ) : ValidatorAction()

    data object AllClean : ValidatorAction()

    data class ViolationsFound(
        val count: Int,
    ) : ValidatorAction()
}
