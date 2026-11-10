package com.rikodev.music.android.core.experience.wave

import com.rikodev.music.android.audiofx.AudioState
import com.rikodev.music.android.core.experience.calibration.WaveCalibration
import javax.inject.Inject
import javax.inject.Singleton
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — ENERGY SOLVER
//
// The brain of the Wave Engine. Converts raw audio into composed energy.
//
// This is NOT a visualizer. This is NOT a renderer. This is NOT an animator.
// This is a PURE MATH ENGINE that understands music energy.
//
// ── RIKO RULES #11–15 (LOCKED) ─────────────────────────────────────────────
//   #11: EnergySolver never knows Compose.
//   #12: EnergySolver never knows Canvas.
//   #13: EnergySolver never knows colors.
//   #14: EnergySolver never creates animation.
//   #15: EnergySolver only converts Audio → Emotion.
//
// Internal Pipeline (7 stages, sequential):
//
//   Stage 1: Raw Audio Extraction
//       │     Read bass, mid, treble, overall, beat, transient from AudioState.
//       │     These are already normalized 0f..1f by the Experience Hub.
//       ▼
//   Stage 2: Energy Composition (Rule #10)
//       │     Bass → bassPressure (not height, PRESSURE)
//       │     Mid  → midTexture   (not thickness, TEXTURE)
//       │     Treble → trebleSpark (not brightness, SPARK)
//       ▼
//   Stage 3: Momentum Solver
//       │     Weighted running average with ASYMMETRIC response:
//       │     Fast rise (follows energy quickly)
//       │     Slow fall (lingers for 8–12 frames after energy drops)
//       │
//       │     Frame: 0.2 → 0.5 → 0.9 → 0.85 → 0.81
//       │     Momentum: LOW → RISING → HIGH → HIGH → HIGH (persists!)
//       ▼
//   Stage 4: Expansion Solver
//       │     Pressure accumulator: builds gradually, releases slowly.
//       │     kick → pressure increases → ribbon expands → holds → slow release
//       │     NOT: kick → jump → drop (cheap visualizer)
//       ▼
//   Stage 5: Inertia Solver
//       │     8–12 frame memory buffer.
//       │     Blends current energy with historical average.
//       │     When rising: 70% current + 30% history (responsive)
//       │     When falling: 92% history + 8% current (inertial, lingers)
//       ▼
//   Stage 6: Ribbon Mode Decision
//       │     Based on energy, playback state, band distribution:
//       │     IDLE → BREATHING → THIN → ACTIVE → PRESSURE_BUILD → EXPANDING
//       │     Plus CALM_SETTLING when playback stops.
//       ▼
//   Stage 7: WaveFrame Assembly
//         Package everything into WaveFrame for the next pipeline stage.
//
// INPUT:  AudioState (from Experience Data Hub via StateFlow)
// OUTPUT: WaveFrame (with WaveEnergy + RibbonMode populated)
//
// PRODUCER: Called per animation frame by the Wave Engine orchestrator.
// CONSUMER: Emotional Inertia Solver (Step 3.3C) reads WaveFrame.energy + mode
//
// ZERO DEPENDENCIES:
//   No Compose, no Canvas, no Modifier, no Path, no DrawScope.
//   No Color, no Brush, no BlendMode.
//   Only: AudioState, WaveEnergy, RibbonMode, WaveFrame — pure Kotlin.
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class EnergySolver @Inject constructor() {

    // ═══════════════════════════════════════════════════════════════════════
    // CONSTANTS
    // ═══════════════════════════════════════════════════════════════════════

    companion object {
        // ── All constants now live in WaveCalibration (Physics BIOS). ──
        // This companion object is kept for HISTORY_SIZE only (structural).
    }

    // ═══════════════════════════════════════════════════════════════════════
    // INTERNAL STATE
    // ═══════════════════════════════════════════════════════════════════════

    /** Energy value from the previous frame — used for attack/release delta. */
    private var previousEnergy: Float = 0f

    /** Accumulated momentum — asymmetric weighted running average. */
    private var momentum: Float = 0f

    /** Expansion accumulator — pressure that builds and slowly releases. */
    private var expansionAccum: Float = 0f

    /** Frame counter — incremented each solve() call. */
    private var frameCount: Int = 0

    /** Ring buffer for inertia history (last HISTORY_SIZE energy values). */
    private val historyBuffer = FloatArray(WaveCalibration.Energy.HISTORY_SIZE)

    /** Current write index into the ring buffer. */
    private var historyWriteIdx: Int = 0

    /** How many valid entries are in the ring buffer (0..WaveCalibration.Energy.HISTORY_SIZE). */
    private var historyFillCount: Int = 0

    /** Last known playback state — for detecting transitions. */
    private var wasPlaying: Boolean = false

    // ═══════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Process one audio frame through the 7-stage energy pipeline.
     *
     * This is the main entry point. Called once per animation frame (~60fps)
     * by the Wave Engine orchestrator. The entire pipeline runs in < 0.1ms
     * — pure math, no allocations, no system calls.
     *
     * @param audioState The current audio snapshot from Experience Data Hub.
     * @return WaveFrame with populated [WaveEnergy] and [RibbonMode].
     *         RibbonState is IDLE — physics is solved in the next pipeline stage.
     */
    fun solve(audioState: AudioState): WaveFrame {
        val solveStartNanos = System.nanoTime()

        // Detect playback transition for state management
        val isPlaying = audioState.isPlaying
        if (!isPlaying && wasPlaying) {
            onPlaybackStopped()
        }
        wasPlaying = isPlaying

        // ── Stage 1: Raw Audio Extraction ─────────────────────────────────
        val bass = audioState.bassEnergy
        val mid = audioState.midEnergy
        val treble = audioState.trebleEnergy
        val overall = audioState.energy
        val beatConf = audioState.beatConfidence
        val transient = audioState.transient

        // ── Stage 2: Attack / Release (needed before composition) ──────────
        val attack = computeAttack(overall, previousEnergy)
        val release = computeRelease(overall, previousEnergy)

        // ── Stage 3: Energy Composition (Rule #10) ───────────────────────
        val bassPressure = composeBassPressure(bass, overall, attack)
        val midTexture = composeMidTexture(mid, treble, bass)
        val trebleSpark = composeTrebleSpark(treble, transient)

        // ── Stage 4: Momentum Solver ───────────────────────────────────────
        val currentMomentum = solveMomentum(overall, attack)
        momentum = currentMomentum

        // ── Stage 5: Expansion Solver ────────────────────────────────────
        val expansion = solveExpansion(bassPressure, overall, attack)

        // ── Stage 6: Inertia Solver ───────────────────────────────────────
        pushHistory(overall)
        val inertiaFactor = solveInertia(overall, attack)

        // ── Stage 7: Ribbon Mode Decision ─────────────────────────────────
        val mode = decideMode(
            isPlaying = isPlaying,
            isIdle = audioState.isIdle,
            hasData = audioState.hasData,
            overall = overall,
            bassPressure = bassPressure,
            midTexture = midTexture,
            trebleSpark = trebleSpark,
            attack = attack,
            expansion = expansion
        )

        // ── Stage 8: Assemble Output ─────────────────────────────────────
        val combinedPressure = bassPressure * 0.5f + midTexture * 0.3f + trebleSpark * 0.2f

        val waveEnergy = WaveEnergy(
            overallEnergy = overall,
            bassPressure = bassPressure,
            midTexture = midTexture,
            trebleSpark = trebleSpark,
            attack = attack,
            release = release,
            momentum = currentMomentum,
            inertiaFactor = inertiaFactor,
            silence = (1f - overall).coerceIn(0f, 1f),
            beatConfidence = beatConf,
            pressure = combinedPressure.coerceIn(0f, 1f),
            expansion = expansion.coerceIn(0f, 1f)
        )

        // Update frame-level state
        previousEnergy = overall
        frameCount++

        return WaveFrame(
            audio = audioState,
            energy = waveEnergy,
            mode = mode,
            ribbon = RibbonState.IDLE,
            timestamp = audioState.timestamp
        )
    }

    /**
     * Reset all internal state.
     * Called on track change, app background, or explicit reset.
     * After reset, the solver starts fresh — no momentum, no history,
     * no expansion residue from the previous track.
     */
    fun reset() {
        previousEnergy = 0f
        momentum = 0f
        expansionAccum = 0f
        frameCount = 0
        historyWriteIdx = 0
        historyFillCount = 0
        wasPlaying = false
        historyBuffer.fill(0f)
    }

    /**
     * Current frame index (for debugging / Experience Inspector).
     * How many frames have been processed since last reset.
     */
    val currentFrameIndex: Int get() = frameCount

    // ═══════════════════════════════════════════════════════════════════════
    // STAGE 1: ATTACK / RELEASE COMPUTATION
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Compute attack speed — how fast energy is RISING.
     *
     * Attack = positive energy delta, scaled by sensitivity.
     * 0.0 = no onset (energy stable or falling).
     * 1.0 = maximum onset speed (energy jumped from 0 to ~0.2 in one frame).
     *
     * PHYSICAL ANALOGY: How quickly you press a spring.
     * Fast press → high attack → ribbon responds quickly.
     * Slow press → low attack → ribbon responds gradually.
     */
    private fun computeAttack(current: Float, previous: Float): Float {
        val delta = current - previous
        return if (delta > 0f) {
            (delta * WaveCalibration.Energy.ATTACK_SENSITIVITY).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    /**
     * Compute release speed — how fast energy is FALLING.
     *
     * Release = negative energy delta, scaled by sensitivity.
     * 0.0 = no decay (energy stable or rising).
     * 1.0 = maximum decay speed (energy dropped from ~1.0 to 0.8 in one frame).
     *
     * PHYSICAL ANALOGY: How quickly you release a spring.
     * Fast release → high release → ribbon starts settling.
     * Slow release → low release → ribbon holds position.
     */
    private fun computeRelease(current: Float, previous: Float): Float {
        val delta = previous - current
        return if (delta > 0f) {
            (delta * WaveCalibration.Energy.RELEASE_SENSITIVITY).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STAGE 2: ENERGY COMPOSITION (RULE #10)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Compose bass pressure from raw bass energy + context.
     *
     * RULE #10: Bass ≠ height. Bass = PRESSURE.
     *
     * Pressure is not just the bass level — it includes:
     *   - Bass energy (primary force)
     *   - Overall energy context (pressure exists within the full mix)
     *   - Attack contribution (rapid change = MORE pressure, like dynamic pressure
     *     in fluid dynamics where pressure depends on velocity, not just position)
     *
     * Formula: bass * 0.60 + overall * 0.25 + attack * 0.15
     */
    private fun composeBassPressure(
        bass: Float,
        overall: Float,
        attack: Float
    ): Float {
        return (bass * WaveCalibration.Energy.BASS_PRESSURE_BASS_W
                + overall * WaveCalibration.Energy.BASS_PRESSURE_OVERALL_W
                + attack * WaveCalibration.Energy.BASS_PRESSURE_ATTACK_W)
            .coerceIn(0f, 1f)
    }

    /**
     * Compose mid texture from raw mid energy + context.
     *
     * Mid frequencies create SURFACE TEXTURE on the ribbon.
     * This is the granularity/detail visible on the ribbon surface.
     *
     * Mid is the primary source. Treble adds upper harmonic texture.
     * Bass adds a subtle fullness to the texture (not zero contribution).
     *
     * Formula: mid * 0.80 + treble * 0.10 + bass * 0.10
     */
    private fun composeMidTexture(
        mid: Float,
        treble: Float,
        bass: Float
    ): Float {
        return (mid * WaveCalibration.Energy.MID_TEXTURE_MID_W
                + treble * WaveCalibration.Energy.MID_TEXTURE_TREBLE_W
                + bass * WaveCalibration.Energy.MID_TEXTURE_BASS_W)
            .coerceIn(0f, 1f)
    }

    /**
     * Compose treble spark from raw treble energy + transient.
     *
     * Treble frequencies create SPARKLE/SHIMMER on the ribbon.
     * Sharp transients (snare hits, hi-hats) add to sparkle even
     * when sustained treble is low.
     *
     * Formula: treble * 0.75 + transient * 0.25
     */
    private fun composeTrebleSpark(
        treble: Float,
        transient: Float
    ): Float {
        return (treble * WaveCalibration.Energy.TREBLE_SPARK_TREBLE_W
                + transient * WaveCalibration.Energy.TREBLE_SPARK_TRANSIENT_W)
            .coerceIn(0f, 1f)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STAGE 3: MOMENTUM SOLVER
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Solve momentum — the Emotional Inertia core.
     *
     * Momentum is NOT current energy. It is ACCUMULATED energy over time.
     * The ribbon remembers energy — it doesn't instantly react and die.
     *
     * ASYMMETRIC RESPONSE:
     *   When energy RISES:  momentum = momentum * 0.75 + energy * 0.25
     *     → Follows quickly (25% new, 75% old) — responsive to transients
     *
     *   When energy FALLS:  momentum = momentum * 0.94 + energy * 0.06
     *     → Lingers slowly (6% new, 94% old) — 8-12 frame tail
     *
     * EXAMPLE:
     *   Frame  Energy  Momentum
     *     1     0.20    0.20   (cold start)
     *     2     0.50    0.42   (rising fast: 0.20*0.75 + 0.50*0.25)
     *     3     0.90    0.60   (still rising)
     *     4     0.85    0.63   (falling! but momentum still high)
     *     5     0.81    0.64   (momentum peaks AFTER energy drops)
     *     6     0.60    0.62   (slow decay: 0.64*0.94 + 0.60*0.06)
     *     7     0.40    0.58   (still lingering)
     *     8     0.20    0.53   (8-frame tail — ribbon still alive)
     *     9     0.10    0.47   (gradual, graceful decay)
     *    10     0.05    0.41   (still not dead)
     *    11     0.02    0.36   (breathing level)
     *    12     0.01    0.31   (minimal but present)
     *
     * This 12-frame tail IS the "Emotional Inertia" — music leaves
     * an impression in the ribbon, just like it does in the human mind.
     */
    private fun solveMomentum(overall: Float, attack: Float): Float {
        return if (attack > 0f) {
            // Energy is rising — fast follow
            momentum * (1f - WaveCalibration.Energy.MOMENTUM_RISE) + overall * WaveCalibration.Energy.MOMENTUM_RISE
        } else {
            // Energy is stable or falling — slow decay
            momentum * (1f - WaveCalibration.Energy.MOMENTUM_FALL) + overall * WaveCalibration.Energy.MOMENTUM_FALL
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STAGE 4: EXPANSION SOLVER
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Solve expansion — controlled growth, NOT explosion.
     *
     * Expansion is driven by an internal PRESSURE ACCUMULATOR.
     * This accumulator:
     *   - Builds quickly when energy rises (pressure builds inside the ribbon)
     *   - Releases VERY slowly when energy falls (ribbon holds expansion)
     *
     * PHYSICAL MODEL: Like inflating a balloon.
     *   1. Air goes in (energy rises) → balloon expands (pressure builds)
     *   2. Stop blowing (energy drops) → balloon doesn't instantly deflate
     *   3. Air slowly leaks out (decay) → balloon gradually shrinks
     *
     * EXPANSION vs EXPLOSION:
     *   Cheap:  bass > 0.7 → wave_height = 1.0  (instant max)
     *   Riko:   bass rises → pressure accumulates → ribbon gradually expands
     *           → holds → slowly releases  (organic, premium)
     */
    private fun solveExpansion(
        bassPressure: Float,
        overall: Float,
        attack: Float
    ): Float {
        // Target expansion from pressure composition
        val target = bassPressure * WaveCalibration.Energy.EXPANSION_PRESSURE_W
                + overall * WaveCalibration.Energy.EXPANSION_OVERALL_W

        if (attack > 0f) {
            // Energy rising: fast buildup
            expansionAccum += (target - expansionAccum) * WaveCalibration.Energy.EXPANSION_ATTACK
        } else {
            // Energy falling: slow release
            // First: natural decay (multiply by 0.97 = 3% loss per frame)
            expansionAccum *= WaveCalibration.Energy.EXPANSION_RELEASE_DECAY
            // Second: slow blend toward target (prevents floating above target)
            expansionAccum += (target - expansionAccum) * WaveCalibration.Energy.EXPANSION_RELEASE_BLEND
        }

        return expansionAccum.coerceIn(0f, 1f)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STAGE 5: INERTIA SOLVER
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Push current energy value to the history ring buffer.
     */
    private fun pushHistory(energy: Float) {
        historyBuffer[historyWriteIdx] = energy
        historyWriteIdx = (historyWriteIdx + 1) % WaveCalibration.Energy.HISTORY_SIZE
        if (historyFillCount < WaveCalibration.Energy.HISTORY_SIZE) historyFillCount++
    }

    /**
     * Solve inertia factor — how strongly history influences current reading.
     *
     * The inertia factor represents the ribbon's "memory" of recent energy.
     * It blends current energy with the 12-frame history average.
     *
     * ASYMMETRIC BLEND:
     *   When RISING:  30% current + 70% history → responsive but grounded
     *   When FALLING:  8% current + 92% history → heavily inertial (lingers)
     *
     * This means:
     *   - A sudden kick: ribbon responds (30% current weight is enough)
     *   - After kick ends: ribbon remembers (92% history = gradual fade)
     *   - Sustained tone: smooth, flowing movement (history is consistent)
     *
     * The inertia factor is NOT the final energy value — it's a MODIFIER
     * that tells the Ribbon Physics solver how much to weight history vs now.
     */
    private fun solveInertia(overall: Float, attack: Float): Float {
        // Average of history buffer
        var historySum = 0f
        val count = historyFillCount
        for (i in 0 until count) {
            historySum += historyBuffer[i]
        }
        val historyAvg = if (count > 0) historySum / count else 0f

        // Asymmetric blend
        val currentWeight = if (attack > 0f) {
            WaveCalibration.Energy.INERTIA_WEIGHT_RISE
        } else {
            WaveCalibration.Energy.INERTIA_WEIGHT_FALL
        }

        return (overall * currentWeight + historyAvg * (1f - currentWeight))
            .coerceIn(0f, 1f)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STAGE 6: RIBBON MODE DECISION
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Decide the ribbon's behavioral mode based on energy landscape.
     *
     * Mode priority (highest to lowest):
     *   1. IDLE — no data, no playback, no energy
     *   2. CALM_SETTLING — playback stopped but energy still present
     *   3. BREATHING — very low energy, music is barely audible
     *   4. THIN — vocal/ambient passage (low bass, low treble, some mid)
     *   5. PRESSURE_BUILD — bass building up with fast attack
     *   6. EXPANDING — high energy, ribbon at maximum presence
     *   7. ACTIVE — default playback state (fallback)
     *
     * NOTE: Mode transitions are NOT smoothed here. The Emotional Inertia
     * solver (Step 3.3C) handles smooth blending between modes.
     * This decision is the "intended" mode — the physics layer decides
     * how quickly to transition.
     */
    private fun decideMode(
        isPlaying: Boolean,
        isIdle: Boolean,
        hasData: Boolean,
        overall: Float,
        bassPressure: Float,
        midTexture: Float,
        trebleSpark: Float,
        attack: Float,
        expansion: Float
    ): RibbonMode {
        // Priority 1: No data at all → IDLE
        if (isIdle) return RibbonMode.IDLE

        // Priority 2: Playback stopped but energy lingers → CALM_SETTLING
        if (!isPlaying && overall > WaveCalibration.Energy.THRESHOLD_IDLE) return RibbonMode.CALM_SETTLING

        // Priority 3: Playback stopped, energy gone → IDLE
        if (!isPlaying) return RibbonMode.IDLE

        // Priority 4: Very low energy → BREATHING
        // The ribbon is almost invisible but DEFINITELY alive
        if (overall <= WaveCalibration.Energy.THRESHOLD_BREATHING) return RibbonMode.BREATHING

        // Priority 5: Vocal / ambient detection
        // Low bass + low treble = no heavy instruments
        // Mid must be present (vocal lives in 250Hz–4kHz)
        if (bassPressure < WaveCalibration.Energy.THRESHOLD_THIN_BAND
            && trebleSpark < WaveCalibration.Energy.THRESHOLD_THIN_BAND
            && midTexture > WaveCalibration.Energy.THRESHOLD_THIN_MID_MIN
        ) {
            return RibbonMode.THIN
        }

        // Priority 6: Bass building with fast attack → PRESSURE_BUILD
        // Pressure is building INSIDE the ribbon, not expanding yet
        if (attack > WaveCalibration.Energy.THRESHOLD_PRESSURE_BUILD_ATTACK
            && bassPressure > WaveCalibration.Energy.THRESHOLD_PRESSURE_BUILD_BASS
        ) {
            return RibbonMode.PRESSURE_BUILD
        }

        // Priority 7: High energy → EXPANDING
        // Ribbon at maximum presence — controlled expansion, NOT explosion
        if (overall > WaveCalibration.Energy.THRESHOLD_EXPANDING || expansion > 0.5f) {
            return RibbonMode.EXPANDING
        }

        // Priority 8: Default active playback
        return RibbonMode.ACTIVE
    }

    // ═══════════════════════════════════════════════════════════════════════
    // INTERNAL STATE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Called when playback transitions from playing → not playing.
     * Does NOT reset everything — allows CALM_SETTLING to work.
     * Momentum and expansion will naturally decay through the pipeline.
     */
    private fun onPlaybackStopped() {
        // Don't reset momentum or expansion — let them decay naturally.
        // This enables the CALM_SETTLING mode: ribbon gracefully settles
        // like water after being disturbed.
        // Full reset happens on track change, not pause.
    }
}
