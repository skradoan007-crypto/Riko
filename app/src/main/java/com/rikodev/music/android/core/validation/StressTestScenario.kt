/*
 * RIKO EXPERIENCE OS — PRODUCTION-GRADE ROADMAP v2.0
 * Milestone 2.9 — Experience Validation
 *
 * File 6 / 10 — StressTestScenario.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Gate G5 — Stress Test. Defines a chaotic, interleaved action stream
 * that drives the system through every major state transition while
 * external events (rotation, background, Bluetooth, headset, call)
 * fire concurrently. Proves the system remains coherent under load.
 *
 * This file declares the action vocabulary, scenario generator, and
 * assertions. The analysis phase feeds these actions into a test
 * harness; the harness records outcomes and feeds them back for
 * evaluation.
 *
 * ACTION CATEGORIES
 * -----------------
 *   - User actions    : scroll, play, queue, favorite, search
 *   - System events   : rotate, config change
 *   - Lifecycle       : background, foreground
 *   - Audio routing   : bluetooth connect/disconnect, headset plug/unplug
 *   - Interruption    : incoming call (audio focus loss), notification
 *
 * ARCHITECTURE
 * ------------
 *
 *   ┌──────────────────────────────────────────────────────────────┐
 *   │                  STRESS TEST SCENARIO                        │
 *   │                                                              │
 *   │  Action Vocabulary                                           │
 *   │    UserAction      ──┐                                       │
 *   │    SystemEvent      ─┤                                       │
 *   │    LifecycleEvent   ─┤                                       │
 *   │    AudioEvent       ─┤                                       │
 *   │    InterruptionEvent─┘                                       │
 *   │                                                              │
 *   │  Scenario Generator                                          │
 *   │    Interleaved with weighted randomness                     │
 *   │    Deterministic seed for reproducibility                   │
 *   │                                                              │
 *   │  Outcome Recorder                                            │
 *   │    Per-action: success / failure / crash / ANR               │
 *   │                                                              │
 *   │  Assertions                                                  │
 *   │    No crash, no ANR, no audio glitch, state coherent         │
 *   └──────────────────────────────────────────────────────────────┘
 *
 * RULES ENFORCED
 * --------------
 * #53  Diagnostics Never Affects Engine — observe only
 * #72  Identity Before Feature — identity stable under stress
 * #83  Coordinator Executes — Rule #83 verified under chaos
 *
 * © RIKO Experience OS — Validation Tier
 */

package com.rikodev.music.android.core.validation

import java.util.Random
import androidx.compose.material.icons.filled.Pause

// ═══════════════════════════════════════════════════════════════════════════
// ACTION VOCABULARY
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Marker interface for all stress test actions.
 */
sealed interface StressAction {

    /** Human-readable name for the action. */
    val displayName: String

    /** Estimated duration in ms (for scheduling). */
    val estimatedDurationMs: Long

    /** Category the action belongs to. */
    val category: ActionCategory
}

/**
 * Category of stress action.
 */
enum class ActionCategory(val displayName: String) {
    USER("User Action"),
    SYSTEM("System Event"),
    LIFECYCLE("Lifecycle Event"),
    AUDIO_ROUTING("Audio Routing"),
    INTERRUPTION("Interruption"),
}

// ─── User actions ───────────────────────────────────────────────────────────

sealed class UserAction : StressAction {
    override val category: ActionCategory get() = ActionCategory.USER

    data class Scroll(val direction: ScrollDirection, val durationMs: Long) : UserAction() {
        override val displayName: String = "Scroll $direction (${durationMs}ms)"
        override val estimatedDurationMs: Long = durationMs
    }

    data class Play(val songId: Long) : UserAction() {
        override val displayName: String = "Play song $songId"
        override val estimatedDurationMs: Long = 500
    }

    data class Pause(val dummy: Int = 0) : UserAction() {
        override val displayName: String = "Pause playback"
        override val estimatedDurationMs: Long = 200
        companion object
    }

    data class Resume(val dummy: Int = 0) : UserAction() {
        override val displayName: String = "Resume playback"
        override val estimatedDurationMs: Long = 200
        companion object
    }

    data class SkipNext(val dummy: Int = 0) : UserAction() {
        override val displayName: String = "Skip to next"
        override val estimatedDurationMs: Long = 300
        companion object
    }

    data class SkipPrevious(val dummy: Int = 0) : UserAction() {
        override val displayName: String = "Skip to previous"
        override val estimatedDurationMs: Long = 300
        companion object
    }

    data class SeekTo(val positionMs: Long) : UserAction() {
        override val displayName: String = "Seek to $positionMs ms"
        override val estimatedDurationMs: Long = 300
    }

    data class QueueAdd(val songId: Long) : UserAction() {
        override val displayName: String = "Add song $songId to queue"
        override val estimatedDurationMs: Long = 300
    }

    data class QueueReorder(val fromIndex: Int, val toIndex: Int) : UserAction() {
        override val displayName: String = "Reorder queue $fromIndex → $toIndex"
        override val estimatedDurationMs: Long = 400
    }

    data class FavoriteToggle(val songId: Long) : UserAction() {
        override val displayName: String = "Toggle favorite for song $songId"
        override val estimatedDurationMs: Long = 200
    }

    data class Search(val query: String) : UserAction() {
        override val displayName: String = "Search \"$query\""
        override val estimatedDurationMs: Long = 1_000
    }

    data class Navigate(val destination: NavigationTarget) : UserAction() {
        override val displayName: String = "Navigate to $destination"
        override val estimatedDurationMs: Long = 400
    }
}

enum class ScrollDirection { UP, DOWN, LEFT, RIGHT }

enum class NavigationTarget(val displayName: String) {
    LIBRARY("Library"),
    NOW_PLAYING("Now Playing"),
    SEARCH("Search"),
    QUEUE("Queue"),
    SETTINGS("Settings"),
    PLAYLISTS("Playlists"),
}

// ─── System events ──────────────────────────────────────────────────────────

sealed class SystemEvent : StressAction {
    override val category: ActionCategory get() = ActionCategory.SYSTEM

    data class Rotate(val orientation: Orientation) : SystemEvent() {
        override val displayName: String = "Rotate to $orientation"
        override val estimatedDurationMs: Long = 800
    }

    data class ConfigChange(val changeType: ConfigChangeType) : SystemEvent() {
        override val displayName: String = "Config change: $changeType"
        override val estimatedDurationMs: Long = 600
    }
}

enum class Orientation { PORTRAIT, LANDSCAPE, REVERSE_PORTRAIT, REVERSE_LANDSCAPE }

enum class ConfigChangeType(val displayName: String) {
    FONT_SCALE("Font scale"),
    LOCALE("Locale"),
    THEME("Theme"),
    DARK_MODE("Dark mode toggle"),
}

// ─── Lifecycle events ──────────────────────────────────────────────────────

sealed class LifecycleEvent : StressAction {
    override val category: ActionCategory get() = ActionCategory.LIFECYCLE

    data class Background(val durationMs: Long) : LifecycleEvent() {
        override val displayName: String = "Background (${durationMs}ms)"
        override val estimatedDurationMs: Long = durationMs
    }

    data object Foreground : LifecycleEvent() {
        override val displayName: String = "Foreground"
        override val estimatedDurationMs: Long = 500
    }

    data class ProcessDeath(val restoreAfterMs: Long) : LifecycleEvent() {
        override val displayName: String = "Process death (restore in ${restoreAfterMs}ms)"
        override val estimatedDurationMs: Long = restoreAfterMs + 1_000
    }
}

// ─── Audio routing events ──────────────────────────────────────────────────

sealed class AudioRoutingEvent : StressAction {
    override val category: ActionCategory get() = ActionCategory.AUDIO_ROUTING

    data class BluetoothConnect(val deviceName: String) : AudioRoutingEvent() {
        override val displayName: String = "Bluetooth connect: $deviceName"
        override val estimatedDurationMs: Long = 1_500
    }

    data class BluetoothDisconnect(val deviceName: String) : AudioRoutingEvent() {
        override val displayName: String = "Bluetooth disconnect: $deviceName"
        override val estimatedDurationMs: Long = 1_000
    }

    data class HeadsetPlug(val wired: Boolean) : AudioRoutingEvent() {
        override val displayName: String = "Headset plug (${if (wired) "wired" else "wireless"})"
        override val estimatedDurationMs: Long = 500
    }

    data class HeadsetUnplug(val dummy: Int = 0) : AudioRoutingEvent() {
        override val displayName: String = "Headset unplug"
        override val estimatedDurationMs: Long = 500
        companion object
    }

    data class AudioOutputChange(val output: AudioOutput) : AudioRoutingEvent() {
        override val displayName: String = "Audio output → $output"
        override val estimatedDurationMs: Long = 800
    }
}

enum class AudioOutput { SPEAKER, BLUETOOTH_A2DP, WIRED_HEADSET, USB, HDMI }

// ─── Interruption events ───────────────────────────────────────────────────

sealed class InterruptionEvent : StressAction {
    override val category: ActionCategory get() = ActionCategory.INTERRUPTION

    data class IncomingCall(val durationMs: Long) : InterruptionEvent() {
        override val displayName: String = "Incoming call (${durationMs}ms)"
        override val estimatedDurationMs: Long = durationMs
    }

    data class Notification(val packageName: String) : InterruptionEvent() {
        override val displayName: String = "Notification from $packageName"
        override val estimatedDurationMs: Long = 1_000
    }

    data class Alarm(val dummy: Int = 0) : InterruptionEvent() {
        override val displayName: String = "Alarm triggered"
        override val estimatedDurationMs: Long = 2_000
        companion object
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SCENARIO GENERATOR
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Configuration for the stress scenario.
 *
 * @property seed                  Deterministic seed.
 * @property totalActions          Total actions to generate.
 * @property userActionWeight      Probability weight for user actions.
 * @property systemEventWeight     Probability weight for system events.
 * @property lifecycleWeight       Probability weight for lifecycle events.
 * @property audioRoutingWeight    Probability weight for audio routing.
 * @property interruptionWeight    Probability weight for interruptions.
 */
data class StressScenarioConfig(
    val seed: Long = 0xBADF00D,
    val totalActions: Int = 500,
    val userActionWeight: Double = 0.60,
    val systemEventWeight: Double = 0.10,
    val lifecycleWeight: Double = 0.10,
    val audioRoutingWeight: Double = 0.10,
    val interruptionWeight: Double = 0.10,
) {
    init {
        val sum = userActionWeight + systemEventWeight + lifecycleWeight + audioRoutingWeight + interruptionWeight
        require(sum in 0.99..1.01) { "Weights must sum to 1.0, got $sum" }
        require(totalActions > 0) { "Total actions must be positive" }
    }
}

/**
 * Generates an interleaved stress scenario.
 *
 * Uses weighted randomness to pick categories, then picks a specific
 * action within the category. Deterministic given the same seed.
 */
class StressScenarioGenerator(
    private val config: StressScenarioConfig = StressScenarioConfig(),
) {

    private val random = Random(config.seed)

    /**
     * Generate the full action stream.
     */
    fun generate(): List<StressAction> {
        return (1..config.totalActions).map { generateOne(it) }
    }

    /**
     * Generate a streaming sequence for memory-efficient ingestion.
     */
    fun stream(): Sequence<StressAction> = sequence {
        for (i in 1..config.totalActions) {
            yield(generateOne(i))
        }
    }

    private fun generateOne(index: Int): StressAction {
        val roll = random.nextDouble()
        return when {
            roll < config.userActionWeight -> generateUserAction(index)
            roll < config.userActionWeight + config.systemEventWeight -> generateSystemEvent(index)
            roll < config.userActionWeight + config.systemEventWeight + config.lifecycleWeight ->
                generateLifecycleEvent(index)
            roll < config.userActionWeight + config.systemEventWeight + config.lifecycleWeight + config.audioRoutingWeight ->
                generateAudioRoutingEvent(index)
            else -> generateInterruptionEvent(index)
        }
    }

    private fun generateUserAction(index: Int): StressAction {
        val subRoll = random.nextDouble()
        return when {
            subRoll < 0.30 -> UserAction.Scroll(
                direction = ScrollDirection.entries[random.nextInt(4)],
                durationMs = 500L + random.nextBoundedLong(2000),
            )
            subRoll < 0.45 -> UserAction.Play(songId = 1L + random.nextBoundedLong(1000))
            subRoll < 0.52 -> UserAction.Pause()
            subRoll < 0.59 -> UserAction.Resume()
            subRoll < 0.67 -> UserAction.SkipNext()
            subRoll < 0.72 -> UserAction.SkipPrevious()
            subRoll < 0.77 -> UserAction.SeekTo(positionMs = random.nextBoundedLong(240_000))
            subRoll < 0.84 -> UserAction.QueueAdd(songId = 1L + random.nextBoundedLong(1000))
            subRoll < 0.89 -> UserAction.QueueReorder(
                fromIndex = random.nextInt(20),
                toIndex = random.nextInt(20),
            )
            subRoll < 0.94 -> UserAction.FavoriteToggle(songId = 1L + random.nextBoundedLong(1000))
            subRoll < 0.98 -> UserAction.Search(
                query = random.pickFrom(listOf("night", "calm", "energy", "rain", "focus", "morning", "hidden gems")),
            )
            else -> UserAction.Navigate(
                destination = NavigationTarget.entries[random.nextInt(NavigationTarget.entries.size)],
            )
        }
    }

    private fun generateSystemEvent(index: Int): StressAction {
        return if (random.nextBoolean()) {
            SystemEvent.Rotate(
                orientation = Orientation.entries[random.nextInt(4)],
            )
        } else {
            SystemEvent.ConfigChange(
                changeType = ConfigChangeType.entries[random.nextInt(ConfigChangeType.entries.size)],
            )
        }
    }

    private fun generateLifecycleEvent(index: Int): StressAction {
        return when (random.nextInt(3)) {
            0 -> LifecycleEvent.Background(durationMs = 1_000L + random.nextBoundedLong(10_000))
            1 -> LifecycleEvent.Foreground
            else -> LifecycleEvent.ProcessDeath(restoreAfterMs = 2_000L + random.nextBoundedLong(5_000))
        }
    }

    private fun generateAudioRoutingEvent(index: Int): StressAction {
        return when (random.nextInt(5)) {
            0 -> AudioRoutingEvent.BluetoothConnect(deviceName = "Pixel Buds Pro")
            1 -> AudioRoutingEvent.BluetoothDisconnect(deviceName = "Pixel Buds Pro")
            2 -> AudioRoutingEvent.HeadsetPlug(wired = random.nextBoolean())
            3 -> AudioRoutingEvent.HeadsetUnplug()
            else -> AudioRoutingEvent.AudioOutputChange(
                output = AudioOutput.entries[random.nextInt(AudioOutput.entries.size)],
            )
        }
    }

    private fun generateInterruptionEvent(index: Int): StressAction {
        return when (random.nextInt(3)) {
            0 -> InterruptionEvent.IncomingCall(durationMs = 5_000L + random.nextBoundedLong(30_000))
            1 -> InterruptionEvent.Notification(packageName = "com.whatsapp")
            else -> InterruptionEvent.Alarm()
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ACTION OUTCOME
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Outcome of executing a single stress action.
 */
data class ActionOutcome(
    val actionIndex: Int,
    val action: StressAction,
    val status: OutcomeStatus,
    val durationMs: Long,
    val errorMessage: String?,
    val stateSnapshot: StateSnapshot?,
    val timestampMs: Long,
)

/**
 * Status of an action outcome.
 */
enum class OutcomeStatus {
    SUCCESS,
    FAILURE,
    CRASH,
    ANR,
    TIMEOUT,
    STATE_INCONSISTENT,
}

/**
 * A snapshot of the system state at the time of an outcome.
 *
 * Used to verify coherence — e.g. queue length should not suddenly
 * drop without a corresponding user action.
 */
data class StateSnapshot(
    val isPlaying: Boolean,
    val currentSongId: Long?,
    val queueSize: Int,
    val playbackPositionMs: Long,
    val audioOutput: AudioOutput,
    val currentScreen: NavigationTarget,
    val miniPlayerVisible: Boolean,
)

// ═══════════════════════════════════════════════════════════════════════════
// STRESS TEST ASSERTIONS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Result of stress test evaluation.
 */
data class StressTestEvaluation(
    val totalActions: Int,
    val successCount: Int,
    val failureCount: Int,
    val crashCount: Int,
    val anrCount: Int,
    val timeoutCount: Int,
    val stateInconsistentCount: Int,
    val failuresByCategory: Map<ActionCategory, Int>,
    val criticalFailures: List<ActionOutcome>,
    val totalDurationMs: Long,
) {
    val successRate: Double get() = if (totalActions == 0) 0.0 else successCount.toDouble() / totalActions
    val crashFreeRate: Double get() = if (totalActions == 0) 0.0 else 1.0 - (crashCount.toDouble() / totalActions)
    val anrRate: Double get() = if (totalActions == 0) 0.0 else anrCount.toDouble() / totalActions
    val passed: Boolean
        get() = crashCount == 0 && anrCount == 0 && stateInconsistentCount == 0 && successRate >= 0.95
}

/**
 * Evaluate a list of action outcomes.
 */
fun evaluateStressOutcomes(outcomes: List<ActionOutcome>): StressTestEvaluation {
    val byCategory = outcomes.filter { it.status != OutcomeStatus.SUCCESS }
        .groupBy { it.action.category }
        .mapValues { (_, list) -> list.size }
    val critical = outcomes.filter {
        it.status in setOf(OutcomeStatus.CRASH, OutcomeStatus.ANR, OutcomeStatus.STATE_INCONSISTENT)
    }
    return StressTestEvaluation(
        totalActions = outcomes.size,
        successCount = outcomes.count { it.status == OutcomeStatus.SUCCESS },
        failureCount = outcomes.count { it.status == OutcomeStatus.FAILURE },
        crashCount = outcomes.count { it.status == OutcomeStatus.CRASH },
        anrCount = outcomes.count { it.status == OutcomeStatus.ANR },
        timeoutCount = outcomes.count { it.status == OutcomeStatus.TIMEOUT },
        stateInconsistentCount = outcomes.count { it.status == OutcomeStatus.STATE_INCONSISTENT },
        failuresByCategory = byCategory,
        criticalFailures = critical,
        totalDurationMs = outcomes.sumOf { it.durationMs },
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// STRESS TEST SCENARIO
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The Stress Test Scenario (Gate G5).
 *
 * Combines the generator and evaluator. The analysis phase drives
 * actions through this class and records outcomes.
 */
class StressTestScenario(
    private val generator: StressScenarioGenerator = StressScenarioGenerator(),
) {

    private val _outcomes: MutableList<ActionOutcome> = mutableListOf()
    val outcomes: List<ActionOutcome> get() = _outcomes.toList()

    /**
     * Generate the action stream.
     */
    fun generateActions(): List<StressAction> = generator.generate()

    /**
     * Record an outcome for an action.
     */
    fun recordOutcome(outcome: ActionOutcome) {
        _outcomes += outcome
    }

    /**
     * Evaluate Gate G5.
     */
    fun evaluate(): GateG5Verdict {
        if (_outcomes.isEmpty()) {
            return GateG5Verdict.Incomplete(actionsExecuted = 0)
        }
        val evaluation = evaluateStressOutcomes(_outcomes)
        return when {
            evaluation.crashCount > 0 -> GateG5Verdict.Failed(
                reason = "${evaluation.crashCount} crash(es) detected",
                evaluation = evaluation,
            )
            evaluation.anrCount > 0 -> GateG5Verdict.Failed(
                reason = "${evaluation.anrCount} ANR(s) detected (threshold: 0)",
                evaluation = evaluation,
            )
            evaluation.stateInconsistentCount > 0 -> GateG5Verdict.Failed(
                reason = "${evaluation.stateInconsistentCount} state inconsistency (ies)",
                evaluation = evaluation,
            )
            evaluation.successRate < 0.95 -> GateG5Verdict.Failed(
                reason = "Success rate ${"%.2f".format(evaluation.successRate * 100)}% below 95%",
                evaluation = evaluation,
            )
            else -> GateG5Verdict.Passed(evaluation = evaluation)
        }
    }

    /**
     * Human-readable summary.
     */
    fun summaryReport(): String = buildString {
        appendLine("═══ GATE G5 — STRESS TEST ═══")
        appendLine()
        if (_outcomes.isEmpty()) {
            appendLine("No actions executed.")
            return@buildString
        }
        val evaluation = evaluateStressOutcomes(_outcomes)
        appendLine("Total actions: ${evaluation.totalActions}")
        appendLine("Success:       ${evaluation.successCount} (${"%.2f".format(evaluation.successRate * 100)}%)")
        appendLine("Failures:      ${evaluation.failureCount}")
        appendLine("Crashes:       ${evaluation.crashCount}")
        appendLine("ANRs:          ${evaluation.anrCount}")
        appendLine("Timeouts:      ${evaluation.timeoutCount}")
        appendLine("Inconsistent:  ${evaluation.stateInconsistentCount}")
        appendLine("Total time:    ${evaluation.totalDurationMs / 1000}s")
        appendLine()
        if (evaluation.failuresByCategory.isNotEmpty()) {
            appendLine("Failures by category:")
            evaluation.failuresByCategory.forEach { (category, count) ->
                appendLine("  ${category.displayName}: $count")
            }
        }
        if (evaluation.criticalFailures.isNotEmpty()) {
            appendLine()
            appendLine("Critical failures (first 5):")
            evaluation.criticalFailures.take(5).forEach { outcome ->
                appendLine("  [${outcome.actionIndex}] ${outcome.action.displayName} → ${outcome.status}")
            }
        }
        appendLine()
        appendLine("Verdict: ${evaluate().javaClass.simpleName}")
    }
}

/**
 * Verdict for Gate G5.
 */
sealed class GateG5Verdict {
    data class Passed(val evaluation: StressTestEvaluation) : GateG5Verdict()

    data class Incomplete(val actionsExecuted: Int) : GateG5Verdict()

    data class Failed(
        val reason: String,
        val evaluation: StressTestEvaluation,
    ) : GateG5Verdict()

    val isPassed: Boolean get() = this is Passed
}

// ═══════════════════════════════════════════════════════════════════════════
// RANDOM HELPERS (java.util.Random extensions)
// ═══════════════════════════════════════════════════════════════════════════

/** Bounded nextLong compatible with all JVM versions. */
private fun Random.nextBoundedLong(bound: Long): Long {
    require(bound > 0) { "Bound must be positive" }
    return (nextLong() and Long.MAX_VALUE) % bound
}

/** Pick a random element from a non-empty list. */
private fun <T> Random.pickFrom(items: List<T>): T {
    require(items.isNotEmpty()) { "Cannot pick from empty list" }
    return items[nextInt(items.size)]
}
