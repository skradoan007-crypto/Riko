/*
 * RIKO EXPERIENCE OS — LIBRARY INTERACTION RUNTIME
 * Build B5.4 — Library Interaction Runtime
 *
 * File 11 / 15 — HapticContract.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Define the full haptic vocabulary for the Library Interaction
 * Runtime. Every vibration the Library emits is declared here —
 * nothing is left to ad-hoc Vibrator calls.
 *
 * HAPTIC PATTERNS
 * ---------------
 *   TAP                  — light tick on tap (selection / open)
 *   LONG_PRESS           — medium tick on long-press fire
 *   SELECTION_TOGGLE     — light tick when toggling selection
 *   SELECTION_ENTERED    — soft tick when entering selection mode
 *   SELECTION_CLEARED    — soft tick when clearing selection
 *   REORDER_HOVER        — very light tick when hovering over new index
 *                          during reorder drag
 *   REORDER_COMMIT       — medium tick when reorder commits
 *   SWIPE_TO_DISMISS     — light tick when dismiss commits
 *   DOCK_MORPH_PROGRESS  — periodic very light ticks during morph
 *   DOCK_COMMIT          — medium tick when Dock morph commits
 *   DOCK_CANCEL          — soft tick when Dock morph cancels
 *   PIN_TOGGLE           — light tick when pinning/unpinning
 *   CONTEXT_MENU_OPEN    — medium tick when context menu opens
 *   ERROR                — sharp buzz on error (e.g. drag failed)
 *   SUCCESS              — soft double-tick on success (e.g. playlist created)
 *
 * DESIGN PHILOSOPHY
 * -----------------
 * RIKO's haptics are *quiet*. The default amplitude is low; the
 * default duration is short. Haptics should feel like the screen is
 * *breathing*, not *shouting*.
 *
 *   - Default amplitude: 60/255 (light)
 *   - Default duration: 20ms (tick)
 *   - Long-press: 40ms, amplitude 100 (more substantial)
 *   - Error: 80ms, amplitude 200 (unmistakable but not painful)
 *
 * All haptics respect the system's "haptic feedback" setting. If the
 * user disables haptics at the system level, no haptic fires — even
 * if the spec says hapticEnabled=true.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #58  Runtime Never Knows Rendering — HapticContract produces data
 *      (amplitude, duration, waveform), not Vibrator calls.
 * #80  Gesture Never Knows UI — HapticContract does not reference
 *      any Composable or View.
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Compose. No Android framework (no Vibrator, no VibrationEffect).
 * No Coroutine. No Flow. No ViewModel. No `var`. No side effects.
 *
 * © RIKO Experience OS — Interaction Runtime
 */

package com.rikodev.music.android.presentation.library.interaction

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 1 — HAPTIC PATTERN
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Every haptic pattern the Library can emit.
 *
 * Adding a new pattern requires:
 *   1. Adding a variant here.
 *   2. Adding a matching spec in [HapticContract.SPECS].
 *   3. Updating the Validator to ensure the pattern is exercised.
 */
enum class HapticPattern(
    internal val displayName: String,
    internal val category: HapticCategory,
) {
    TAP("Tap", HapticCategory.LIGHT),
    LONG_PRESS("Long Press", HapticCategory.MEDIUM),
    SELECTION_TOGGLE("Selection Toggle", HapticCategory.LIGHT),
    SELECTION_ENTERED("Selection Entered", HapticCategory.LIGHT),
    SELECTION_CLEARED("Selection Cleared", HapticCategory.LIGHT),
    REORDER_HOVER("Reorder Hover", HapticCategory.VERY_LIGHT),
    REORDER_COMMIT("Reorder Commit", HapticCategory.MEDIUM),
    SWIPE_TO_DISMISS("Swipe to Dismiss", HapticCategory.LIGHT),
    DOCK_MORPH_PROGRESS("Dock Morph Progress", HapticCategory.VERY_LIGHT),
    DOCK_COMMIT("Dock Commit", HapticCategory.MEDIUM),
    DOCK_CANCEL("Dock Cancel", HapticCategory.LIGHT),
    PIN_TOGGLE("Pin Toggle", HapticCategory.LIGHT),
    CONTEXT_MENU_OPEN("Context Menu Open", HapticCategory.MEDIUM),
    ERROR("Error", HapticCategory.STRONG),
    SUCCESS("Success", HapticCategory.MEDIUM),
}

/**
 * Category of a haptic — determines the default amplitude range.
 */
enum class HapticCategory(
    internal val defaultAmplitude: Int,
    internal val defaultDurationMs: Long,
) {
    VERY_LIGHT(defaultAmplitude = 30, defaultDurationMs = 10),
    LIGHT(defaultAmplitude = 60, defaultDurationMs = 20),
    MEDIUM(defaultAmplitude = 100, defaultDurationMs = 40),
    STRONG(defaultAmplitude = 200, defaultDurationMs = 80),
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 2 — HAPTIC SPEC
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The shape of a haptic's waveform over time.
 */
enum class HapticWaveform {
    /** Single brief pulse. */
    SINGLE_PULSE,

    /** Two quick pulses (tick-tick). */
    DOUBLE_PULSE,

    /** Smooth ramp-up (fade-in). */
    RAMP_UP,

    /** Smooth ramp-down (fade-out). */
    RAMP_DOWN,

    /** Wave (sine-like modulation). */
    WAVE,

    /** Sharp hard tick (very brief, high amplitude). */
    SHARP_TICK,
}

/**
 * A single timed haptic pulse within a waveform.
 *
 * @property delayMs     Delay (ms) from the start of the haptic.
 * @property durationMs  Duration (ms) of this pulse.
 * @property amplitude   Amplitude [0..255].
 */
data class HapticPulse(
    val delayMs: Long,
    val durationMs: Long,
    val amplitude: Int,
) {
    init {
        require(amplitude in 0..255) { "amplitude must be in [0, 255]" }
        require(durationMs >= 0) { "durationMs must be non-negative" }
        require(delayMs >= 0) { "delayMs must be non-negative" }
    }
}

/**
 * Full specification of a haptic.
 *
 * @property pattern        The pattern this spec describes.
 * @property waveform       Waveform shape.
 * @property pulses         Timed pulses (the actual waveform data).
 * @property isEnabled      True if this haptic should fire. False
 *                           when the spec exists but the system / user
 *                           has disabled it.
 * @property respectsSystemSetting  True if this haptic respects the
 *                           system's haptic-feedback setting. False
 *                           only for critical haptics (e.g. ERROR)
 *                           that should fire regardless.
 */
data class HapticSpec(
    val pattern: HapticPattern,
    val waveform: HapticWaveform,
    val pulses: List<HapticPulse>,
    val isEnabled: Boolean,
    val respectsSystemSetting: Boolean,
) {
    val totalDurationMs: Long
        get() = pulses.maxOfOrNull { it.delayMs + it.durationMs } ?: 0

    companion object {
        /**
         * Build a single-pulse haptic.
         */
        fun singlePulse(
            pattern: HapticPattern,
            amplitude: Int,
            durationMs: Long,
            isEnabled: Boolean = true,
            respectsSystemSetting: Boolean = true,
        ): HapticSpec = HapticSpec(
            pattern = pattern,
            waveform = HapticWaveform.SINGLE_PULSE,
            pulses = listOf(HapticPulse(0, durationMs, amplitude)),
            isEnabled = isEnabled,
            respectsSystemSetting = respectsSystemSetting,
        )

        /**
         * Build a double-pulse haptic (tick-tick).
         */
        fun doublePulse(
            pattern: HapticPattern,
            amplitude: Int,
            durationMs: Long,
            gapMs: Long,
            isEnabled: Boolean = true,
            respectsSystemSetting: Boolean = true,
        ): HapticSpec = HapticSpec(
            pattern = pattern,
            waveform = HapticWaveform.DOUBLE_PULSE,
            pulses = listOf(
                HapticPulse(0, durationMs, amplitude),
                HapticPulse(durationMs + gapMs, durationMs, amplitude),
            ),
            isEnabled = isEnabled,
            respectsSystemSetting = respectsSystemSetting,
        )

        /**
         * Build a sharp-tick haptic.
         */
        fun sharpTick(
            pattern: HapticPattern,
            amplitude: Int,
            isEnabled: Boolean = true,
            respectsSystemSetting: Boolean = true,
        ): HapticSpec = HapticSpec(
            pattern = pattern,
            waveform = HapticWaveform.SHARP_TICK,
            pulses = listOf(HapticPulse(0, 8, amplitude)),
            isEnabled = isEnabled,
            respectsSystemSetting = respectsSystemSetting,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 3 — HAPTIC CONTRACT (THE FROZEN SPECS)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The Haptic Contract — frozen specifications for every haptic
 * pattern. Adding a new pattern requires extending both
 * [HapticPattern] and this object.
 *
 * These specs are deliberately conservative. RIKO haptics should
 * feel *premium-quiet*, not *arcade-loud*.
 */
object HapticContract {

    /**
     * The full spec table. Lookup by [HapticPattern].
     */
    val SPECS: Map<HapticPattern, HapticSpec> = mapOf(
        HapticPattern.TAP to HapticSpec.sharpTick(
            pattern = HapticPattern.TAP,
            amplitude = 50,
        ),

        HapticPattern.LONG_PRESS to HapticSpec.singlePulse(
            pattern = HapticPattern.LONG_PRESS,
            amplitude = 100,
            durationMs = 30,
        ),

        HapticPattern.SELECTION_TOGGLE to HapticSpec.sharpTick(
            pattern = HapticPattern.SELECTION_TOGGLE,
            amplitude = 60,
        ),

        HapticPattern.SELECTION_ENTERED to HapticSpec.singlePulse(
            pattern = HapticPattern.SELECTION_ENTERED,
            amplitude = 50,
            durationMs = 15,
        ),

        HapticPattern.SELECTION_CLEARED to HapticSpec.singlePulse(
            pattern = HapticPattern.SELECTION_CLEARED,
            amplitude = 40,
            durationMs = 15,
        ),

        HapticPattern.REORDER_HOVER to HapticSpec.sharpTick(
            pattern = HapticPattern.REORDER_HOVER,
            amplitude = 25,
        ),

        HapticPattern.REORDER_COMMIT to HapticSpec.singlePulse(
            pattern = HapticPattern.REORDER_COMMIT,
            amplitude = 90,
            durationMs = 25,
        ),

        HapticPattern.SWIPE_TO_DISMISS to HapticSpec.singlePulse(
            pattern = HapticPattern.SWIPE_TO_DISMISS,
            amplitude = 70,
            durationMs = 20,
        ),

        HapticPattern.DOCK_MORPH_PROGRESS to HapticSpec.sharpTick(
            pattern = HapticPattern.DOCK_MORPH_PROGRESS,
            amplitude = 20,
        ),

        HapticPattern.DOCK_COMMIT to HapticSpec.singlePulse(
            pattern = HapticPattern.DOCK_COMMIT,
            amplitude = 110,
            durationMs = 35,
        ),

        HapticPattern.DOCK_CANCEL to HapticSpec.singlePulse(
            pattern = HapticPattern.DOCK_CANCEL,
            amplitude = 40,
            durationMs = 15,
        ),

        HapticPattern.PIN_TOGGLE to HapticSpec.sharpTick(
            pattern = HapticPattern.PIN_TOGGLE,
            amplitude = 55,
        ),

        HapticPattern.CONTEXT_MENU_OPEN to HapticSpec.singlePulse(
            pattern = HapticPattern.CONTEXT_MENU_OPEN,
            amplitude = 80,
            durationMs = 25,
        ),

        HapticPattern.ERROR to HapticSpec.doublePulse(
            pattern = HapticPattern.ERROR,
            amplitude = 200,
            durationMs = 50,
            gapMs = 30,
            respectsSystemSetting = false, // errors always fire
        ),

        HapticPattern.SUCCESS to HapticSpec.doublePulse(
            pattern = HapticPattern.SUCCESS,
            amplitude = 90,
            durationMs = 25,
            gapMs = 40,
        ),
    )

    /**
     * Get the spec for a pattern. Returns the default (silent)
     * spec if not found.
     */
    fun specFor(pattern: HapticPattern): HapticSpec =
        SPECS[pattern] ?: SILENT_SPEC

    /**
     * Check if a pattern is enabled.
     */
    fun isEnabled(pattern: HapticPattern): Boolean =
        SPECS[pattern]?.isEnabled == true

    /**
     * The silent spec — used as a fallback when a pattern is not
     * found or when haptics are globally disabled.
     */
    val SILENT_SPEC: HapticSpec = HapticSpec(
        pattern = HapticPattern.TAP,
        waveform = HapticWaveform.SINGLE_PULSE,
        pulses = emptyList(),
        isEnabled = false,
        respectsSystemSetting = true,
    )

    // ─────────────────────────────────────────────────────────────────
    // PROFILE PRESETS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Different haptic profiles for different user preferences.
     *
     * - DEFAULT:  standard haptics on
     * - MINIMAL:  only critical haptics (errors, long-press)
     * - SILENT:   no haptics at all
     * - STRONG:   louder haptics for users who want more feedback
     */
    enum class HapticProfile(
        internal val displayName: String,
        internal val amplitudeMultiplier: Float,
        internal val enabledPatterns: Set<HapticPattern>,
    ) {
        DEFAULT(
            displayName = "Default",
            amplitudeMultiplier = 1.0f,
            enabledPatterns = HapticPattern.values().toSet(),
        ),

        MINIMAL(
            displayName = "Minimal",
            amplitudeMultiplier = 0.8f,
            enabledPatterns = setOf(
                HapticPattern.LONG_PRESS,
                HapticPattern.REORDER_COMMIT,
                HapticPattern.DOCK_COMMIT,
                HapticPattern.ERROR,
                HapticPattern.SUCCESS,
            ),
        ),

        SILENT(
            displayName = "Silent",
            amplitudeMultiplier = 0f,
            enabledPatterns = emptySet(),
        ),

        STRONG(
            displayName = "Strong",
            amplitudeMultiplier = 1.4f,
            enabledPatterns = HapticPattern.values().toSet(),
        ),
    }

    /**
     * Get the spec for a pattern, adjusted for the user's profile.
     */
    fun specFor(pattern: HapticPattern, profile: HapticProfile): HapticSpec {
        val baseSpec = specFor(pattern)
        if (!profile.enabledPatterns.contains(pattern)) return SILENT_SPEC
        if (profile.amplitudeMultiplier <= 0f) return SILENT_SPEC

        val adjustedPulses = baseSpec.pulses.map { pulse ->
            pulse.copy(
                amplitude = (pulse.amplitude * profile.amplitudeMultiplier)
                    .toInt()
                    .coerceIn(0, 255),
            )
        }

        return baseSpec.copy(pulses = adjustedPulses)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 4 — HAPTIC INTENT + ACTION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * An Intent to emit a haptic. Emitted by various reducers when an
 * action's spec says hapticEnabled=true.
 */
data class HapticIntent(
    val pattern: HapticPattern,
    val profile: HapticContract.HapticProfile,
    val sourceGestureId: String?,
)

/**
 * An Action to emit a haptic. Emitted by the Coordinator after
 * consuming a [HapticIntent].
 */
data class HapticAction(
    val spec: HapticSpec,
    val sourceGestureId: String?,
)

/**
 * Convert a [HapticIntent] into a [HapticAction] using the
 * HapticContract specs.
 */
fun HapticIntent.toAction(): HapticAction = HapticAction(
    spec = HapticContract.specFor(pattern, profile),
    sourceGestureId = sourceGestureId,
)
