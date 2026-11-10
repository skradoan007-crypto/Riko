package com.rikodev.music.android.core.experience.wave

import com.rikodev.music.android.audiofx.AudioState
import androidx.compose.ui.graphics.Path

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — WAVE FRAME
//
// The per-frame pipeline carrier for the Wave Engine.
//
// This is the "working envelope" that flows through the pipeline:
//
//   AudioState
//       │
//       ▼
//   [Energy Solver]      → writes WaveEnergy into frame
//       │
//       ▼
//   [Emotional Inertia]   → enriches WaveEnergy (momentum, inertia)
//       │
//       ▼
//   [Ribbon Physics]      → writes RibbonState into frame
//       │
//       ▼
//   [Path Generator]       → (future: writes path geometry)
//       │
//       ▼
//   Extract WaveState ← published to consumers
//
// Each solver receives a WaveFrame, reads its input section,
// computes its output, and returns an UPDATED WaveFrame.
// This is a functional pipeline — no shared mutable state.
//
// WaveFrame is INTERNAL to the Wave Engine.
// Consumers (UI, Glass, Motion) read WaveState, NOT WaveFrame.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Per-frame working data for the Wave Engine pipeline.
 *
 * Each solver stage reads specific fields and writes its output:
 *   - Energy Solver: reads [audio], writes [energy]
 *   - Emotional Inertia: reads [energy], enriches [energy], writes [mode]
 *   - Ribbon Physics: reads [energy] + [mode], writes [ribbon]
 *   - Path Generator: reads [ribbon], writes path geometry (future)
 *
 * INVARIANTS:
 *   - Immutable — each stage creates a new copy with updated fields
 *   - [audio] is set once at pipeline entry, never modified
 *   - Pipeline flows top-to-bottom: audio → energy → mode → ribbon → path
 *   - No stage reads a field that a LATER stage writes
 *
 * PERFORMANCE NOTE:
 *   This frame is created ONCE per animation frame (~60fps = 16ms budget).
 *   All operations must be allocation-free after initialization.
 *   Use .copy() for stage outputs — data class copy is lightweight.
 */
data class WaveFrame(

    // ── Input (set once at pipeline entry, read-only) ──────────────────────

    /**
     * Source audio snapshot — the AudioState from Experience Data Hub.
     * Set once when the frame enters the pipeline. Never modified by solvers.
     * All energy values are read from here by the Energy Solver.
     */
    val audio: AudioState = AudioState.IDLE,

    // ── Energy Layer (written by Energy Solver + Emotional Inertia) ────────

    /**
     * Composed energy state.
     * Written by Energy Solver (Rule #10), then enriched by Emotional Inertia.
     * Contains: overallEnergy, bassPressure, midTexture, trebleSpark,
     * attack, release, momentum, inertiaFactor, pressure, expansion.
     */
    val energy: WaveEnergy = WaveEnergy.IDLE,

    // ── Behavior Layer (written by Emotional Inertia) ──────────────────────

    /**
     * Current ribbon behavioral mode.
     * Written by Emotional Inertia solver based on energy composition.
     * Determines how Ribbon Physics interprets energy into movement.
     */
    val mode: RibbonMode = RibbonMode.IDLE,

    // ── Physics Layer (written by Ribbon Physics) ─────────────────────────

    /**
     * Ribbon physics state.
     * Written by Ribbon Physics solver based on energy + mode.
     * Contains: elasticity, viscosity, pressure, turbulence, thickness, etc.
     */
    val ribbon: RibbonState = RibbonState.IDLE,

    // ── Timing ─────────────────────────────────────────────────────────────

    /**
     * Frame timestamp (System.nanoTime()).
     * Carried from AudioState through the pipeline for timing calculations.
     */
    val timestamp: Long = 0L

) {
    /**
     * True if this frame has no meaningful data — everything is idle.
     */
    val isIdle: Boolean
        get() = mode == RibbonMode.IDLE
                && energy.overallEnergy < 0.01f
                && audio.isIdle

    /**
     * True if this frame has audio data available for processing.
     */
    val hasAudioData: Boolean
        get() = audio.hasData

    /**
     * True if this frame has energy data (Energy Solver has run).
     */
    val hasEnergy: Boolean
        get() = energy.isActive

    /**
     * True if physics has been computed (Ribbon Physics has run).
     */
    val hasPhysics: Boolean
        get() = ribbon.isAlive && ribbon.thickness > 0.01f

    companion object {
        /**
         * Create a fresh pipeline frame from AudioState.
         * This is the entry point — audio goes in, idle energy/ribbon/mode.
         * Each subsequent solver enriches this frame.
         */
        fun fromAudio(audio: AudioState): WaveFrame = WaveFrame(
            audio = audio,
            energy = WaveEnergy.IDLE,
            mode = if (audio.isPlaying) RibbonMode.BREATHING else RibbonMode.IDLE,
            ribbon = RibbonState.IDLE,
            timestamp = audio.timestamp
        )

        /** Pre-built idle frame — no audio, no energy, no physics. */
        val IDLE = WaveFrame()
    }
}
