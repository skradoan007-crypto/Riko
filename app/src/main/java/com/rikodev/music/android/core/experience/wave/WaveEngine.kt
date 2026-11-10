package com.rikodev.music.android.core.experience.wave

import com.rikodev.music.android.audiofx.AudioState
import com.rikodev.music.android.core.experience.ColorState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Path
import androidx.compose.material.icons.filled.Pause
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — WAVE ENGINE
//
// The conductor of the Wave Engine pipeline.
//
// This is NOT a solver. This is NOT a renderer. This does NOT compute physics,
// energy, geometry, or light. This CONNECTS solvers.
//
// The Wave Engine is the single public API of the entire Wave subsystem.
// Consumers (Renderer, Glass, Motion, Navigation) see ONLY:
//   StateFlow<WaveState>
//
// Nobody sees EnergySolver. Nobody sees RibbonPhysicsSolver. Nobody sees
// OrganicPathGenerator. Nobody sees HighlightSolver.
//
// The Wave Engine is the ONLY component allowed to create WaveState.
//
// ── ARCHITECTURE ────────────────────────────────────────────────────────────
//
//   AudioState (from Experience Hub)
//       │
//       ▼
//   WaveEngine.observe(audioStateFlow, colorStateFlow)
//       │
//       ├── [EnergySolver]        → WaveFrame (energy + mode)
//       │
//       ├── [RibbonPhysicsSolver] → WaveFrame (ribbon physics)
//       │
//       ├── [MotionSolver]         → MotionState (movement dynamics)
//       │
//       ├── [OrganicPathGenerator] → OrganicPath (geometry)
//       │
//       └── [HighlightSolver]      → HighlightState (light + glass)
//               │
//               ▼
//        StateFlow<WaveState>  ← published output
//        StateFlow<WaveDiagnostics>  ← for Inspector (3.3J)
//
// ── RULES ───────────────────────────────────────────────────────────────────
//   #1: WaveEngine NEVER computes. Only calls solvers.
//        No sin(), cos(), Bezier, pressure, momentum, fresnel.
//   #2: Pipeline is immutable. Each stage uses .copy().
//        No shared mutable objects in the pipeline.
//   #3: Error resilience. If a solver crashes,
//        WaveEngine falls back to previous frame and continues.
//   #4: All solvers are constructor-injected. Never created internally.
//   #5: Frame timing is measured per-solver. Published via diagnostics.
//   #6: Lifecycle: observe → pause/resume → stop → reset.
//   #7: Zero Compose, zero Canvas, zero Path, zero DrawScope.
//
// ── ERROR STRATEGY ──────────────────────────────────────────────────────────
//   Per-solver try/catch. On failure:
//     - Use previous frame's output for that stage
//     - Increment error count
//     - Log last error message
//     - Continue pipeline with what we have
//     - Publish partial state (better than nothing)
//
// PRODUCER: Wave Engine Orchestrator (Step 3.3G)
// CONSUMER: Canvas Renderer (Phase 3.4), Glass (3.4), Motion (3.5)
//
// ⭐ GOLDEN RULE ⭐
//   Nobody creates WaveState.
//   Everybody subscribes.
//   Only Wave Engine writes.
// ─────────────────────────────────────────────────────────────────────────────


// ═══════════════════════════════════════════════════════════════════════════
// ENGINE STATUS — Lifecycle state of the Wave Engine
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Lifecycle state of the Wave Engine.
 *
 *   READY   → Engine constructed, not yet observing
 *   RUNNING → Actively processing audio frames
 *   PAUSED  → Observing but not processing (audio paused)
 *   IDLE    → No observation, no processing
 *   ERROR   → Observation failed (recoverable)
 */
enum class EngineStatus {
    READY,
    RUNNING,
    PAUSED,
    IDLE,
    ERROR
}


// ═══════════════════════════════════════════════════════════════════════════
// WAVE DIAGNOSTICS — Per-frame performance and state telemetry
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Per-frame diagnostics published alongside WaveState.
 *
 * Used by the Experience Inspector (Step 3.3J) for live engine debugging.
 * Shows solver costs, key values, FPS, errors — everything needed
 * to understand what the engine is doing without reading source code.
 *
 * INVARIANT: All timing values are in nanoseconds (raw).
 *   Millisecond values are computed getters (for display convenience).
 */
data class WaveDiagnostics(

    // ── Status ──────────────────────────────────────────────────────────

    /** Current engine lifecycle status. */
    val status: EngineStatus = EngineStatus.IDLE,

    // ── Frame Info ──────────────────────────────────────────────────────

    /** Current ribbon behavioral mode. */
    val ribbonMode: RibbonMode = RibbonMode.IDLE,

    /** Number of geometry segments in the current path. */
    val segmentCount: Int = 0,

    /** Frames per second (computed over 1-second windows). */
    val fps: Float = 0f,

    // ── Solver Timing (nanoseconds) ────────────────────────────────────

    /** Energy Solver execution time. */
    val energyTimeNs: Long = 0L,

    /** Ribbon Physics Solver execution time. */
    val physicsTimeNs: Long = 0L,

    /** Motion Solver execution time. */
    val motionTimeNs: Long = 0L,

    /** Organic Path Generator execution time. */
    val geometryTimeNs: Long = 0L,

    /** Highlight Solver execution time. */
    val highlightTimeNs: Long = 0L,

    /** Total pipeline execution time (all solvers combined). */
    val totalTimeNs: Long = 0L,

    // ── Key State Values (snapshot for Inspector) ───────────────────────

    /** Current ribbon pressure 0f..1f. */
    val pressure: Float = 0f,

    /** Current energy momentum 0f..1f. */
    val momentum: Float = 0f,

    /** Current highlight intensity 0f..1f. */
    val highlightIntensity: Float = 0f,

    /** Current expansion level 0f..1f. */
    val expansion: Float = 0f,

    /** Current overall energy 0f..1f. */
    val overallEnergy: Float = 0f,

    /** Number of geometry points in the current path. */
    val geometryPointCount: Int = 0,

    /** Average path curvature 0f..1f. */
    val curvature: Float = 0f,

    /** Current ribbon thickness 0f..1f. */
    val thickness: Float = 0f,

    /** Current ribbon opacity 0f..1f. */
    val opacity: Float = 0f,

    /** Current motion velocity. */
    val velocity: Float = 0f,

    /** Current motion offset from center. */
    val offset: Float = 0f,

    // ── Error Tracking ──────────────────────────────────────────────────

    /** Total error count since last reset. */
    val errorCount: Int = 0,

    /** Last error message (null if no error). */
    val lastError: String? = null,

    // ── Source ──────────────────────────────────────────────────────────

    /** Audio source type (PCM / VISUALIZER / IDLE). */
    val source: String = "IDLE",

    // ── Timing ──────────────────────────────────────────────────────────

    /** Frame timestamp. */
    val timestamp: Long = 0L

) {
    // ── Millisecond convenience getters (for display) ──────────────────

    val energyTimeMs: Float get() = energyTimeNs / 1_000_000f
    val physicsTimeMs: Float get() = physicsTimeNs / 1_000_000f
    val motionTimeMs: Float get() = motionTimeNs / 1_000_000f
    val geometryTimeMs: Float get() = geometryTimeNs / 1_000_000f
    val highlightTimeMs: Float get() = highlightTimeNs / 1_000_000f
    val totalTimeMs: Float get() = totalTimeNs / 1_000_000f

    companion object {
        /** Pre-built idle diagnostics. */
        val IDLE = WaveDiagnostics()
    }
}


// ═══════════════════════════════════════════════════════════════════════════
// WAVE ENGINE — The Orchestrator
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The conductor of the Wave Engine pipeline.
 *
 * Connects all 5 solvers into a single processing pipeline and publishes
 * the result as [StateFlow][WaveState].
 *
 * ## Construction
 * All solvers are injected via constructor. The Wave Engine never creates
 * solvers internally. This enables:
 *   - Unit testing with mock solvers
 *   - Hilt DI (Step 3.3H)
 *   - Solver swapping without touching the engine
 *
 * ## Usage
 * ```kotlin
 * val engine = WaveEngine(
 *     energySolver = EnergySolver(),
 *     ribbonPhysicsSolver = RibbonPhysicsSolver(),
 *     motionSolver = MotionSolver(),
 *     pathGenerator = OrganicPathGenerator(),
 *     highlightSolver = HighlightSolver(),
 *     scope = viewModelScope
 * )
 *
 * engine.observe(hub.audioState, colorEngine.colorState)
 *
 * // Later, in Compose:
 * val waveState by engine.state.collectAsStateWithLifecycle()
 * val diagnostics by engine.diagnostics.collectAsStateWithLifecycle()
 * ```
 *
 * ## Performance
 * - Per-solver timing measured with System.nanoTime()
 * - Published via WaveDiagnostics for Inspector debugging
 * - Target: total pipeline < 2ms at 60fps (16ms budget)
 *
 * ## Error Resilience
 * Each solver is wrapped in try-catch. A single solver failure does NOT
 * crash the engine. The previous frame's output is used as fallback.
 *
 * INVARIANTS:
 *   - Immutable pipeline: each stage creates new instances via .copy()
 *   - Never computes physics/energy/geometry directly
 *   - Publishes on every frame, even with partial results
 *
 * @param energySolver          Composes energy from audio (Step 3.3B)
 * @param ribbonPhysicsSolver   Computes ribbon fluid physics (Step 3.3C)
 * @param motionSolver          Computes spring-mass movement (Step 3.3D)
 * @param pathGenerator         Generates organic ribbon geometry (Step 3.3E)
 * @param highlightSolver       Computes light/reflection/glass (Step 3.3F)
 * @param scope                 Coroutine scope for flow collection
 */
@Singleton
class WaveEngine @Inject constructor(

    // ── Solvers (constructor-injected via Hilt, never created internally) ──
    private val energySolver: EnergySolver,
    private val ribbonPhysicsSolver: RibbonPhysicsSolver,
    private val motionSolver: MotionSolver,
    private val pathGenerator: OrganicPathGenerator,
    private val highlightSolver: HighlightSolver,

    // ── Coroutine scope (injected, typically viewModelScope) ────────────
    private val scope: CoroutineScope

) {

    // ═══════════════════════════════════════════════════════════════════════
    // PUBLIC API — StateFlows
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * The published wave state.
     *
     * Consumers (Renderer, Glass, Motion) subscribe to this.
     * Updated every audio frame (~60fps).
     *
     * ⭐ GOLDEN RULE: Nobody creates WaveState. Everybody subscribes.
     * Only Wave Engine writes.
     */
    private val _state = MutableStateFlow(WaveState.IDLE)
    val state: StateFlow<WaveState> = _state.asStateFlow()

    /**
     * Per-frame diagnostics for the Experience Inspector.
     *
     * Contains solver timings, key values, FPS, errors.
     * Updated every audio frame alongside [state].
     */
    private val _diagnostics = MutableStateFlow(WaveDiagnostics.IDLE)
    val diagnostics: StateFlow<WaveDiagnostics> = _diagnostics.asStateFlow()

    // ═══════════════════════════════════════════════════════════════════════
    // INTERNAL STATE
    // ═══════════════════════════════════════════════════════════════════════

    /** Current engine lifecycle status. */
    @Volatile
    private var engineStatus: EngineStatus = EngineStatus.READY

    /** Latest ColorState from Color Intelligence Engine. Updated via flow. */
    @Volatile
    private var currentColorState: ColorState = ColorState.IDLE

    /** Previous frame outputs — used as fallback on solver errors. */
    private var previousWaveFrame: WaveFrame = WaveFrame.IDLE
    private var previousMotion: MotionState = MotionState.IDLE
    private var previousPath: OrganicPath = OrganicPath.EMPTY
    private var previousHighlight: HighlightState = HighlightState.IDLE

    /** Frame counter for FPS calculation. */
    private var frameCount: Int = 0

    /** Timestamp of the last FPS calculation (System.nanoTime()). */
    private var lastFpsTimestamp: Long = 0L

    /** Frame count at the last FPS calculation. */
    private var framesAtLastFps: Int = 0

    /** Current computed FPS. */
    private var currentFps: Float = 0f

    /** Running error count since last reset. */
    private var totalErrorCount: Int = 0

    /** Last error message from a solver failure. */
    private var lastErrorMessage: String? = null

    /** Observation coroutine jobs — stored for cancellation. */
    private var colorJob: Job? = null
    private var audioJob: Job? = null

    // ═══════════════════════════════════════════════════════════════════════
    // OBSERVE — Subscribe to input flows and start processing
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Subscribe to AudioState and ColorState flows.
     *
     * AudioState updates trigger the full pipeline (Energy → Physics → Motion
     * → Geometry → Highlight → Publish).
     *
     * ColorState updates are stored as a reference — the Highlight Solver
     * reads the latest ColorState each frame.
     *
     * Calling observe() while already observing will cancel the previous
     * observation and restart.
     *
     * @param audioStateFlow   StateFlow from Experience Data Hub
     * @param colorStateFlow   StateFlow from Color Intelligence Engine
     */
    fun observe(
        audioStateFlow: StateFlow<AudioState>,
        colorStateFlow: StateFlow<ColorState>
    ) {
        cancelObservation()

        engineStatus = EngineStatus.RUNNING

        // Subscribe to ColorState — updates are stored, don't trigger pipeline
        colorJob = scope.launch {
            colorStateFlow.collect { color ->
                currentColorState = color
            }
        }

        // Subscribe to AudioState — triggers the full pipeline per frame.
        // StateFlow already conflates via operator fusion (intermediate frames
        // skipped if the pipeline can't keep up) — no .conflate() needed.
        audioJob = scope.launch {
            audioStateFlow.collect { audio ->
                if (engineStatus == EngineStatus.RUNNING) {
                    processFrame(audio, currentColorState)
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PROCESS FRAME — The Pipeline
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Run the full Wave Engine pipeline for one audio frame.
     *
     * Pipeline:
     *   AudioState → Energy → Physics → Motion → Geometry → Highlight → Publish
     *
     * Each stage is wrapped in try-catch for error resilience.
     * On failure, the previous frame's output is used as fallback.
     * The pipeline continues with whatever data is available.
     *
     * Timing is measured per-solver using System.nanoTime().
     * Results are published via WaveDiagnostics.
     *
     * @param audioState   Current audio snapshot from the Hub
     * @param colorState   Current color state from Color Intelligence Engine
     */
    private fun processFrame(
        audioState: AudioState,
        colorState: ColorState
    ) {
        val frameStart = System.nanoTime()

        // ── Timing accumulators ────────────────────────────────────────
        var energyTime = 0L
        var physicsTime = 0L
        var motionTime = 0L
        var geometryTime = 0L
        var highlightTime = 0L
        var hasError = false

        // ── Stage outputs (initialized to previous frame fallbacks) ────
        var frame = previousWaveFrame
        var motion = previousMotion
        var path = previousPath
        var highlight = previousHighlight

        // ═══════════════════════════════════════════════════════════════
        // STAGE 1: Energy Solver
        // Input: AudioState → Output: WaveFrame (energy + mode)
        // ═══════════════════════════════════════════════════════════════
        try {
            val t = System.nanoTime()
            frame = energySolver.solve(audioState)
            energyTime = System.nanoTime() - t
        } catch (e: Exception) {
            hasError = true
            trackError("EnergySolver", e)
            // Use previous frame's energy — pipeline continues
        }

        // ═══════════════════════════════════════════════════════════════
        // STAGE 2: Ribbon Physics Solver
        // Input: WaveFrame → Output: WaveFrame (ribbon physics added)
        // ═══════════════════════════════════════════════════════════════
        try {
            val t = System.nanoTime()
            frame = ribbonPhysicsSolver.solve(frame)
            physicsTime = System.nanoTime() - t
        } catch (e: Exception) {
            hasError = true
            trackError("RibbonPhysicsSolver", e)
        }

        // ═══════════════════════════════════════════════════════════════
        // STAGE 3: Motion Solver
        // Input: WaveFrame → Output: MotionState
        // ═══════════════════════════════════════════════════════════════
        try {
            val t = System.nanoTime()
            motion = motionSolver.solve(frame)
            motionTime = System.nanoTime() - t
        } catch (e: Exception) {
            hasError = true
            trackError("MotionSolver", e)
        }

        // ═══════════════════════════════════════════════════════════════
        // STAGE 4: Organic Path Generator
        // Input: WaveFrame + MotionState → Output: OrganicPath
        // ═══════════════════════════════════════════════════════════════
        try {
            val t = System.nanoTime()
            path = pathGenerator.generate(frame, motion)
            geometryTime = System.nanoTime() - t
        } catch (e: Exception) {
            hasError = true
            trackError("OrganicPathGenerator", e)
        }

        // ═══════════════════════════════════════════════════════════════
        // STAGE 5: Highlight Solver
        // Input: OrganicPath + WaveFrame + RibbonState + ColorState
        // Output: HighlightState
        // ═══════════════════════════════════════════════════════════════
        try {
            val t = System.nanoTime()
            highlight = highlightSolver.solve(
                path = path,
                frame = frame,
                ribbon = frame.ribbon,
                color = colorState
            )
            highlightTime = System.nanoTime() - t
        } catch (e: Exception) {
            hasError = true
            trackError("HighlightSolver", e)
        }

        // ═══════════════════════════════════════════════════════════════
        // PUBLISH — Build WaveState and WaveDiagnostics
        // ═══════════════════════════════════════════════════════════════
        val totalTime = System.nanoTime() - frameStart

        // Store successful outputs for next frame fallback
        if (!hasError) {
            previousWaveFrame = frame
            previousMotion = motion
            previousPath = path
            previousHighlight = highlight
        }

        // Build and publish WaveState
        val waveState = buildWaveState(
            frame = frame,
            motion = motion,
            path = path,
            highlight = highlight
        )
        _state.value = waveState

        // Update FPS counter
        updateFps()

        // Build and publish diagnostics
        val diag = buildDiagnostics(
            frame = frame,
            motion = motion,
            path = path,
            highlight = highlight,
            audioState = audioState,
            energyTimeNs = energyTime,
            physicsTimeNs = physicsTime,
            motionTimeNs = motionTime,
            geometryTimeNs = geometryTime,
            highlightTimeNs = highlightTime,
            totalTimeNs = totalTime
        )
        _diagnostics.value = diag
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BUILD WAVE STATE — Assemble the published output
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Build WaveState from pipeline outputs.
     *
     * This is the ONLY place in the entire codebase that creates WaveState.
     * ⭐ GOLDEN RULE: Only Wave Engine writes.
     */
    private fun buildWaveState(
        frame: WaveFrame,
        motion: MotionState,
        path: OrganicPath,
        highlight: HighlightState
    ): WaveState {
        return WaveState(
            energy = frame.energy,
            ribbon = frame.ribbon,
            mode = frame.mode,
            isActive = frame.hasEnergy && !frame.audio.isIdle,
            isVisible = frame.ribbon.isAlive,
            path = path,
            highlight = highlight,
            timestamp = frame.timestamp
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BUILD DIAGNOSTICS — Assemble the Inspector data
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Build WaveDiagnostics from pipeline outputs and timing.
     */
    private fun buildDiagnostics(
        frame: WaveFrame,
        motion: MotionState,
        path: OrganicPath,
        highlight: HighlightState,
        audioState: AudioState,
        energyTimeNs: Long,
        physicsTimeNs: Long,
        motionTimeNs: Long,
        geometryTimeNs: Long,
        highlightTimeNs: Long,
        totalTimeNs: Long
    ): WaveDiagnostics {
        return WaveDiagnostics(
            status = engineStatus,
            ribbonMode = frame.mode,
            segmentCount = path.segmentCount,
            fps = currentFps,
            energyTimeNs = energyTimeNs,
            physicsTimeNs = physicsTimeNs,
            motionTimeNs = motionTimeNs,
            geometryTimeNs = geometryTimeNs,
            highlightTimeNs = highlightTimeNs,
            totalTimeNs = totalTimeNs,
            pressure = frame.ribbon.pressure,
            momentum = frame.energy.momentum,
            highlightIntensity = highlight.specularIntensity,
            expansion = frame.energy.expansion,
            overallEnergy = frame.energy.overallEnergy,
            geometryPointCount = path.pointCount,
            curvature = path.averageCurvature,
            thickness = frame.ribbon.thickness,
            opacity = frame.ribbon.opacity,
            velocity = motion.velocity,
            offset = motion.offset,
            errorCount = totalErrorCount,
            lastError = lastErrorMessage,
            source = if (audioState.hasData) {
                audioState.source.name
            } else {
                "IDLE"
            },
            timestamp = System.nanoTime()
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FPS TRACKING
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Update FPS counter using a 1-second window.
     * FPS = frames_processed / elapsed_time.
     */
    private fun updateFps() {
        frameCount++
        val now = System.nanoTime()

        if (lastFpsTimestamp == 0L) {
            // First frame — start tracking
            lastFpsTimestamp = now
            framesAtLastFps = frameCount
        } else {
            val elapsed = now - lastFpsTimestamp
            if (elapsed >= 1_000_000_000L) {
                // 1 second elapsed — compute FPS
                val framesDelta = frameCount - framesAtLastFps
                currentFps = framesDelta * 1_000_000_000f / elapsed
                lastFpsTimestamp = now
                framesAtLastFps = frameCount
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ERROR TRACKING
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Track a solver error. Increments count and stores message.
     */
    private fun trackError(solverName: String, error: Exception) {
        totalErrorCount++
        lastErrorMessage = "$solverName: ${error.message}"
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Pause the engine.
     *
     * Stops processing frames but keeps the flow subscriptions alive.
     * The engine can be resumed with [resume].
     *
     * Current WaveState remains published (last frame before pause).
     * Solvers are NOT reset — their temporal state is preserved.
     */
    fun pause() {
        if (engineStatus == EngineStatus.RUNNING) {
            engineStatus = EngineStatus.PAUSED
        }
    }

    /**
     * Resume the engine from a paused state.
     *
     * Resumes processing frames from the subscribed flows.
     * Solvers continue from where they left off (temporal state preserved).
     */
    fun resume() {
        if (engineStatus == EngineStatus.PAUSED) {
            engineStatus = EngineStatus.RUNNING
        }
    }

    /**
     * Stop the engine completely.
     *
     * Cancels all flow subscriptions and resets solver state.
     * Published WaveState returns to IDLE.
     *
     * Call [observe] to restart.
     */
    fun stop() {
        cancelObservation()
        resetSolvers()
        resetInternalState()
        _state.value = WaveState.IDLE
        _diagnostics.value = WaveDiagnostics.IDLE
        engineStatus = EngineStatus.IDLE
    }

    /**
     * Reset all solver internal state.
     *
     * Call when: track changes, player reset, engine restart.
     * Each solver clears its temporal buffers (EMA, ring buffers, etc.).
     */
    fun reset() {
        resetSolvers()
        resetInternalState()
        // Don't cancel observation — keep listening
        // Don't publish IDLE — keep last frame visible
    }

    /**
     * Reset all solver temporal state.
     * Called by [stop] and [reset].
     */
    private fun resetSolvers() {
        energySolver.reset()
        ribbonPhysicsSolver.reset()
        motionSolver.reset()
        pathGenerator.reset()
        highlightSolver.reset()
    }

    /**
     * Reset internal engine state (fallback buffers, counters).
     * Called by [stop] and [reset].
     */
    private fun resetInternalState() {
        previousWaveFrame = WaveFrame.IDLE
        previousMotion = MotionState.IDLE
        previousPath = OrganicPath.EMPTY
        previousHighlight = HighlightState.IDLE
        frameCount = 0
        lastFpsTimestamp = 0L
        framesAtLastFps = 0
        currentFps = 0f
        totalErrorCount = 0
        lastErrorMessage = null
    }

    /**
     * Cancel all observation coroutines.
     * Called by [stop] and [observe] (to restart).
     */
    private fun cancelObservation() {
        colorJob?.cancel()
        audioJob?.cancel()
        colorJob = null
        audioJob = null
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONVENIENCE — Current status access
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Current engine lifecycle status.
     */
    val status: EngineStatus
        get() = engineStatus

    /**
     * True if the engine is actively processing frames.
     */
    val isRunning: Boolean
        get() = engineStatus == EngineStatus.RUNNING

    /**
     * True if the engine is paused (observing but not processing).
     */
    val isPaused: Boolean
        get() = engineStatus == EngineStatus.PAUSED
}
