package com.rikodev.music.android.core.experience.wave

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — MOTION STATE
//
// The movement dynamics of the Organic Energy Ribbon™.
//
// This is the output of the Motion Solver (Step 3.3D).
// It describes HOW the ribbon is moving through space — not its physical
// properties (that's RibbonState), but its TRAJECTORY.
//
// PHYSICAL ANALOGY:
//   RibbonState  = how the ribbon IS (elasticity, viscosity, pressure)
//   MotionState  = where the ribbon is GOING (velocity, offset, direction)
//
// The ribbon center line has a vertical offset from the screen center.
// This offset is driven by energy but constrained by spring physics
// and drag, creating smooth, organic movement that never teleports.
//
// CRITICAL RULE:
//   Movement NEVER teleports.
//   x=100 → x=300 is WRONG.
//   100 → 140 → 182 → 221 → 252 → 279 → 300 is CORRECT.
//
// PRODUCER: Motion Solver (Step 3.3D) — reads WaveEnergy + RibbonState
// CONSUMER: Path Generator (Step 3.3E) — uses offset + velocity for geometry
//
// ALL values are internal physics. Only [offset] maps to visual space
// (the Path Generator converts it to vertical pixel displacement).
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Movement dynamics of the ribbon at the current frame.
 *
 * Every value here is computed by the Motion Solver.
 * The ribbon's center line oscillates vertically based on energy,
 * but with spring physics, drag, and directional hysteresis to
 * prevent jittery, mechanical movement.
 *
 * INVARIANT: All Floats are signed — positive = downward, negative = upward.
 * The Path Generator interprets the sign for vertical displacement.
 */
data class MotionState(

    // ── Core Dynamics ──────────────────────────────────────────────────

    /**
     * Vertical velocity — how fast the ribbon center is currently moving.
     *   Positive = moving downward, Negative = moving upward.
     *   Zero = momentarily still (at peak or trough of oscillation).
     *
     * Updated per frame: velocity += acceleration, then velocity *= drag.
     * The drag prevents runaway velocity and creates natural deceleration.
     */
    val velocity: Float = 0f,

    /**
     * Vertical acceleration — the force currently acting on the ribbon.
     *   Positive = pushing down, Negative = pushing up.
     *
     * Computed from: energy force − spring restoration − damping.
     * The spring restoration pulls the ribbon back toward center (offset=0),
     * preventing it from drifting too far in one direction.
     */
    val acceleration: Float = 0f,

    /**
     * Movement momentum — accumulated velocity over recent frames.
     * Not instantaneous velocity — this is the MOVEMENT MEMORY.
     *
     * Like WaveEnergy.momentum but for movement, not energy.
     * A kick sends the ribbon moving, and this momentum keeps it moving
     * for several frames after the kick ends. Emotional Inertia for motion.
     */
    val momentum: Float = 0f,

    // ── Direction ─────────────────────────────────────────────────────

    /**
     * Current movement direction with hysteresis.
     *   +1.0 = moving downward (or holding downward)
     *   -1.0 = moving upward (or holding upward)
     *    0.0 = transitional / undecided
     *
     * Hysteresis prevents rapid direction flipping. A threshold must be
     * crossed before direction flips, eliminating jittery back-and-forth.
     *
     * Example: If direction is +1.0 and velocity drops to -0.01, direction
     * stays +1.0. Only when velocity reaches -0.05 does direction flip to -1.0.
     */
    val direction: Float = 0f,

    // ── Resistance ─────────────────────────────────────────────────────

    /**
     * Drag coefficient — how much velocity decays per frame.
     *   Applied as: velocity *= (1.0 - drag)
     *   0.0 = no drag (perpetual motion)
     *   1.0 = instant stop (no momentum)
     *
     * Typical range: 0.04–0.12 (94%–96% velocity retention per frame).
     * Higher drag = faster deceleration = less "float".
     * Lower drag = longer momentum = more "float" and inertia.
     */
    val drag: Float = 0.06f,

    /**
     * Friction coefficient — how much acceleration is dampened.
     *   Applied as: acceleration *= (1.0 - friction)
     *   0.0 = full acceleration (no friction)
     *   1.0 = zero acceleration (completely stuck)
     *
     * Prevents the ribbon from overreacting to sudden energy spikes.
     * Higher friction = more damped response = smoother.
     */
    val friction: Float = 0.15f,

    // ── Position ────────────────────────────────────────────────────────

    /**
     * Vertical offset from center — where the ribbon IS right now.
     *   Positive = below center, Negative = above center.
     *   Range: approximately -1.0 to +1.0 (clamped).
     *
     * This is the PRIMARY output that the Path Generator uses.
     * It determines the vertical displacement of the ribbon center line.
     *
     * Updated per frame: offset += velocity, then clamped.
     * The spring restoration in acceleration naturally pulls offset back
     * toward zero, creating oscillation around center.
     */
    val offset: Float = 0f,

    // ── State Flags ───────────────────────────────────────────────────

    /**
     * True if the ribbon is in "calm settling" — actively returning
     * toward center after playback stopped or energy dropped.
     *
     * During settling, the motion solver increases drag and adds extra
     * spring restoration, creating the "water settling" behavior:
     *   ~~~~~~~~ → ~~~~~~ → ~~~~~ → ~~~~ → ~~ → —
     */
    val isSettling: Boolean = false,

    /**
     * True if the ribbon is currently at or near the center line.
     * Used by Path Generator to apply minimum displacement fallback.
     */
    val isCentered: Boolean = true

) {
    /**
     * How fast the ribbon is moving (magnitude, always positive).
     * Used by Path Generator to determine displacement intensity.
     */
    val speed: Float
        get() = kotlin.math.abs(velocity)

    /**
     * How far the ribbon is from center (magnitude, always positive).
     */
    val displacement: Float
        get() = kotlin.math.abs(offset)

    /**
     * True if the ribbon has meaningful movement (not stationary).
     */
    val isMoving: Boolean
        get() = speed > 0.001f

    companion object {
        /** Pre-built idle state — centered, no movement. */
        val IDLE = MotionState()
    }
}
