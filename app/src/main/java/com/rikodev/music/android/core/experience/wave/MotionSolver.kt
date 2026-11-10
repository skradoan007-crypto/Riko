package com.rikodev.music.android.core.experience.wave

import javax.inject.Inject
import javax.inject.Singleton

import kotlin.math.abs
import kotlin.math.sin
import com.rikodev.music.android.core.experience.calibration.WaveCalibration
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — MOTION SOLVER
//
// Converts ribbon physics into smooth, organic movement trajectories.
//
// This is NOT a renderer. This is NOT a path generator.
// This is a PURE MATH ENGINE that understands how physics becomes motion.
//
// ── RIKO RULES #11–15 (LOCKED) ─────────────────────────────────────────────
//   #11: Never knows Compose.
//   #12: Never knows Canvas.
//   #13: Never knows colors.
//   #14: Never creates animation.
//   #15: Only converts Physics → Motion.
//
// CORE PRINCIPLE:
//   Movement NEVER teleports.
//
//   WRONG:   x=100 → x=300   (instant jump — cheap visualizer)
//   CORRECT: 100 → 140 → 182 → 221 → 252 → 279 → 300  (smooth trajectory)
//
//   velocity += acceleration
//   velocity *= 0.94f  (drag — natural deceleration)
//   offset   += velocity (displacement accumulates gradually)
//
// PHYSICS MODEL:
//   The ribbon center line is attached to a virtual spring at y=0.
//   Energy pushes the ribbon away from center (force).
//   The spring pulls it back (restoration).
//   Drag slows it down (friction with air).
//   The result: oscillation that never teleports, always smooths.
//
//   Kick:
//     energy force → large acceleration → velocity builds → offset grows
//     But: drag decays velocity each frame → offset grows slower each frame
//     And: spring pulls back → acceleration reverses → offset returns
//     Result: smooth bump that rises and falls naturally
//
//   Silence:
//     no energy force → only spring restoration remains
//     offset gradually returns to 0 (water settling)
//     velocity decays via drag → slower and slower
//     Result: ~~~~~~~~ → ~~~~~~ → ~~~~~ → ~~~~ → ~~ → —
//
//   Sustained bass:
//     constant force → velocity stabilizes at equilibrium
//     offset holds at a displacement proportional to energy
//     Result: steady, flowing movement — not jittery
//
// INPUT:  WaveFrame (energy + ribbon from previous solvers)
// OUTPUT: MotionState (velocity, acceleration, offset, direction, etc.)
//
// ZERO DEPENDENCIES:
//   No Compose, no Canvas, no Modifier, no Path, no DrawScope.
//   No Color, no Brush, no BlendMode.
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class MotionSolver @Inject constructor() {

    // ═══════════════════════════════════════════════════════════════════════
    // CONSTANTS
    // ═══════════════════════════════════════════════════════════════════════

    companion object { // All constants now live in WaveCalibration (Physics BIOS).
    }

    // ═══════════════════════════════════════════════════════════════════════
    // INTERNAL STATE
    // ═══════════════════════════════════════════════════════════════════════

    /** Current vertical velocity. */
    private var velocity: Float = 0f

    /** Current vertical offset from center. */
    private var offset: Float = 0f

    /** Accumulated movement momentum (Emotional Inertia for motion). */
    private var motionMomentum: Float = 0f

    /** Current direction: +1, -1, or 0 (transitional). */
    private var direction: Float = 0f

    /** Frame counter for sine-based breathing. */
    private var frameCount: Int = 0

    // ═══════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Process one frame through the motion solver pipeline.
     *
     * Takes a WaveFrame with populated energy + ribbon physics
     * and returns a MotionState describing the ribbon's movement.
     *
     * Pipeline:
     *   1. Compute energy force (push from energy + pressure + beat)
     *   2. Compute spring restoration (pull toward center)
     *   3. Compute acceleration (force − restoration, dampened by friction)
     *   4. Update velocity (velocity += acceleration, then *= drag)
     *   5. Update offset (offset += velocity, clamped)
     *   6. Compute momentum (asymmetric EMA of speed)
     *   7. Update direction (with hysteresis)
     *   8. Add breathing noise (organic micro-movement)
     *
     * @param frame Input frame from RibbonPhysicsSolver.
     * @return MotionState with all movement dynamics.
     */
    fun solve(frame: WaveFrame): MotionState {
        val energy = frame.energy
        val ribbon = frame.ribbon
        val mode = frame.mode

        val isSettling = mode == RibbonMode.CALM_SETTLING || mode == RibbonMode.IDLE

        // ── Stage 1: Energy Force (what pushes the ribbon) ──────────────
        val energyForce = computeEnergyForce(energy, mode)

        // ── Stage 2: Spring Restoration (what pulls it back) ────────────
        val springK = if (isSettling) WaveCalibration.Motion.SPRING_CONSTANT_SETTLING else WaveCalibration.Motion.SPRING_CONSTANT
        val springRestoration = -offset * springK

        // ── Stage 3: Acceleration (net force, dampened) ────────────────
        val friction = if (isSettling) WaveCalibration.Motion.FRICTION_BASE * 1.5f else WaveCalibration.Motion.FRICTION_BASE
        val rawAcceleration = energyForce + springRestoration
        val acceleration = rawAcceleration * (1f - friction)

        // ── Stage 4: Velocity (integrate acceleration, apply drag) ─────
        velocity += acceleration

        // Drag varies by mode
        val drag = when (mode) {
            RibbonMode.EXPANDING -> WaveCalibration.Motion.DRAG_EXPANDING
            RibbonMode.BREATHING -> WaveCalibration.Motion.DRAG_BREATHING
            RibbonMode.CALM_SETTLING -> WaveCalibration.Motion.DRAG_SETTLING
            else -> WaveCalibration.Motion.DRAG_BASE
        }
        velocity *= (1f - drag)

        // ── Stage 5: Offset (integrate velocity, clamp) ────────────────
        offset += velocity
        offset = offset.coerceIn(-WaveCalibration.Motion.OFFSET_MAX, WaveCalibration.Motion.OFFSET_MAX)

        // Settle snap: when very close to center during settling, snap to zero
        if (isSettling && abs(offset) < WaveCalibration.Motion.SETTLE_THRESHOLD) {
            offset = 0f
            velocity *= 0.5f // Extra damping when nearly settled
        }

        // ── Stage 6: Momentum (asymmetric EMA of speed) ───────────────
        val currentSpeed = abs(velocity)
        motionMomentum = if (currentSpeed > motionMomentum) {
            // Rising: fast follow
            motionMomentum * (1f - WaveCalibration.Motion.MOMENTUM_RISE) + currentSpeed * WaveCalibration.Motion.MOMENTUM_RISE
        } else {
            // Falling: slow decay (12-frame tail for movement too)
            motionMomentum * (1f - WaveCalibration.Motion.MOMENTUM_FALL) + currentSpeed * WaveCalibration.Motion.MOMENTUM_FALL
        }

        // ── Stage 7: Direction (with hysteresis) ────────────────────────
        direction = updateDirection(velocity, direction)

        // ── Stage 8: Breathing (organic micro-movement) ───────────────
        val breathOffset = if (mode == RibbonMode.IDLE || mode == RibbonMode.BREATHING) {
            sin(frameCount.toFloat() * WaveCalibration.Motion.BREATH_SPEED) * WaveCalibration.Motion.BREATH_AMPLITUDE
        } else {
            0f
        }

        val finalOffset = (offset + breathOffset).coerceIn(-WaveCalibration.Motion.OFFSET_MAX, WaveCalibration.Motion.OFFSET_MAX)

        frameCount++

        return MotionState(
            velocity = velocity,
            acceleration = acceleration,
            momentum = motionMomentum,
            direction = direction,
            drag = drag,
            friction = friction,
            offset = finalOffset,
            isSettling = isSettling,
            isCentered = abs(finalOffset) < 0.005f
        )
    }

    /**
     * Reset all internal state to defaults.
     * Called on track change, engine shutdown, or explicit reset.
     */
    fun reset() {
        velocity = 0f
        offset = 0f
        motionMomentum = 0f
        direction = 0f
        frameCount = 0
    }

    /** Current frame index (for debugging). */
    val currentFrameIndex: Int get() = frameCount

    // ═══════════════════════════════════════════════════════════════════════
    // INTERNAL SOLVERS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Compute the energy-driven force that pushes the ribbon.
     *
     * Three sources of force:
     *   1. Overall energy → primary push (scaled by WaveCalibration.Motion.ENERGY_FORCE_SCALE)
     *   2. Bass pressure → secondary push (scaled by WaveCalibration.Motion.PRESSURE_FORCE_SCALE)
     *   3. Beat confidence → impulse thump (one-shot push per beat)
     *
     * The force direction alternates based on energy delta:
     *   - Energy rising → push in current direction (continue movement)
     *   - Energy falling → spring handles return naturally
     *   - Beat → impulse in the direction the ribbon is already moving
     *
     * IMPORTANT: Force is NOT "energy * constant = displacement".
     * Force feeds into acceleration, which feeds into velocity,
     * which feeds into offset. Each step has drag/spring/damping.
     * This is why the ribbon doesn't teleport — it ACCELERATES.
     */
    private fun computeEnergyForce(energy: WaveEnergy, mode: RibbonMode): Float {
        // Base energy force
        val energyPush = energy.overallEnergy * WaveCalibration.Motion.ENERGY_FORCE_SCALE

        // Pressure push (heavier, creates deeper displacement)
        val pressurePush = energy.pressure * WaveCalibration.Motion.PRESSURE_FORCE_SCALE

        // Beat impulse (thump in the direction of current movement)
        val beatPush = if (energy.beatConfidence > 0.5f) {
            WaveCalibration.Motion.BEAT_IMPULSE * energy.beatConfidence * if (direction >= 0f) 1f else -1f
        } else {
            0f
        }

        // Mode modulation
        val modeScale = when (mode) {
            RibbonMode.IDLE -> 0f           // No energy force when idle
            RibbonMode.BREATHING -> 0.3f   // Very subtle
            RibbonMode.THIN -> 0.5f         // Moderate
            RibbonMode.ACTIVE -> 1.0f       // Full
            RibbonMode.PRESSURE_BUILD -> 1.2f // Extra push while building
            RibbonMode.EXPANDING -> 1.5f   // Maximum push during drops
            RibbonMode.CALM_SETTLING -> 0.2f // Minimal (mostly spring)
        }

        // Combine forces with direction
        val totalForce = energyPush + pressurePush + beatPush
        return totalForce * modeScale
    }

    /**
     * Update direction with hysteresis.
     *
     * Without hysteresis, direction would flip every frame when velocity
     * crosses zero — creating jittery, mechanical movement.
     *
     * Hysteresis rules:
     *   - If direction is +1: only flip to -1 when velocity < -THRESHOLD
     *   - If direction is -1: only flip to +1 when velocity > +THRESHOLD
     *   - If direction is  0: set to velocity sign when velocity > NEUTRAL_THRESHOLD
     *
     * This creates a "dead zone" where direction holds steady,
     * preventing rapid flipping and creating smooth, confident movement.
     */
    private fun updateDirection(velocity: Float, currentDirection: Float): Float {
        return when {
            currentDirection > 0f -> {
                // Currently moving down
                when {
                    velocity < -WaveCalibration.Motion.DIRECTION_FLIP_THRESHOLD -> -1f // Flip: now moving up
                    abs(velocity) < WaveCalibration.Motion.DIRECTION_NEUTRAL_THRESHOLD -> 0f // Transitional
                    else -> currentDirection // Hold
                }
            }
            currentDirection < 0f -> {
                // Currently moving up
                when {
                    velocity > WaveCalibration.Motion.DIRECTION_FLIP_THRESHOLD -> 1f  // Flip: now moving down
                    abs(velocity) < WaveCalibration.Motion.DIRECTION_NEUTRAL_THRESHOLD -> 0f // Transitional
                    else -> currentDirection // Hold
                }
            }
            else -> {
                // Neutral — pick direction from velocity
                when {
                    velocity > WaveCalibration.Motion.DIRECTION_FLIP_THRESHOLD -> 1f
                    velocity < -WaveCalibration.Motion.DIRECTION_FLIP_THRESHOLD -> -1f
                    else -> 0f
                }
            }
        }
    }
}
