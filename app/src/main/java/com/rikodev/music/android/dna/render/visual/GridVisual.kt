package com.rikodev.music.android.dna.render.visual

import com.rikodev.music.android.dna.core.BandSource

// ─────────────────────────────────────────────────────────────────────────────
// BREATHING GRID (Phase 4 — visual 8 of 9)
//
// A wireframe terrain that breathes and undulates with the music—like a
// digital ocean surface. Each grid point displaces vertically with waves
// that follow the spectrum, creating a living landscape. Class reactions:
//
//   bass      — violent terrain folds, deep valleys, bright flare
//   vocal     — gentle rolling hills, calm breathing surface
//   low vocal — deep slow swelling waves
//   low sound — almost flat, faint grid, resting terrain
//   full mix  — chaotic mountainous waves, maximum displacement
// ─────────────────────────────────────────────────────────────────────────────

/** CPU-side spec of the grid laws — the VERT shader mirrors these formulas. */
object GridMath {
    /** Terrain displacement — mirrors GridVisual.VERT `displace`. */
    fun displacement(energy: Float, body: Float, fold: Float): Float =
        0.12f * energy * (0.5f + 0.5f * fold) + 0.10f * body * fold

    /** Breathing amplitude — mirrors GridVisual.VERT `breathe`. */
    fun breathe(breathe: Float, phase: Float): Float =
        1f + breathe * 0.06f * kotlin.math.sin(phase.toDouble()).toFloat()

    /** Grid light — mirrors GridVisual.VERT `light`. */
    fun light(energy: Float, body: Float, flare: Float, punch: Float): Float =
        0.12f + 1.3f * energy + 0.5f * flare * body + 0.6f * punch
}

object GridVisual {
    const val ID = "grid"
    const val NAME = "Breathing Grid"

    const val VERT = """
precision highp float;
attribute float aId;
uniform vec2 uViewport;
uniform float uColumns;
uniform float uPointSize;
uniform float uMaxPointSize;
uniform float uPalette;
uniform float uSeed;
uniform float uGain;
uniform float uBass;
uniform float uMid;
uniform float uPunch;
uniform float uPhase;
uniform float uRoll;
uniform float uPass;
uniform float uFold;
uniform float uFlare;
uniform float uWidth;
uniform float uBreathe;
uniform float uDetail;
uniform float uSparkle;
uniform float uBands[40];
uniform float uTrail[40];
uniform float uTime;
varying vec3 vColor;
varying float vLight;

const float TAU = 6.28318530718;
float hash(float x) { return fract(sin(x * 12.9898 + uSeed * 39.71) * 43758.5453); }

void main() {
    float row = floor(aId / uColumns);
    float v = row / 31.0;
    float column = mod(aId, uColumns);
    float t = column / uColumns;
    float x = t * 2.0 - 1.0;
    float z = (v - 0.5) * 1.5;
    float body = clamp(uBass * uGain, 0.0, 1.0);

    // Mirrored spectrum across the terrain.
    float freq = abs(x) * 39.0;
    int lo = int(floor(freq));
    int hi = int(min(float(lo) + 1.0, 39.0));
    float energy = clamp(mix(uBands[lo], uBands[hi], fract(freq)) * uGain, 0.0, 1.0);

    // Terrain waves — GridMath.displacement is the documented spec.
    float wave1 = sin(x * 3.0 + uPhase + z * 2.5 * uDetail) * 0.5;
    float wave2 = sin(z * 4.0 - uPhase * 0.7 + x * 2.0 * uDetail) * 0.3;
    float wave3 = sin((x + z) * 2.0 + uPhase * 0.5) * 0.2;
    float displace = 0.12 * energy * (0.5 + 0.5 * uFold) + 0.10 * body * uFold;
    float y = (wave1 + wave2 + wave3) * displace;

    // Breathing surface — GridMath.breathe is the documented spec.
    float breathe = 1.0 + uBreathe * 0.06 * sin(uPhase * 0.3);

    vec2 scale = min(uViewport.x, uViewport.y) / uViewport;
    gl_Position = vec4(vec2(x * 0.95, y * breathe) * scale, 0.0, 1.0);

    float depth = clamp(0.6 + y * 1.2, 0.35, 1.3);
    gl_PointSize = clamp(uPointSize * (0.5 + 1.0 * energy + 0.3 * body) * (0.8 + 0.5 * uWidth)
        * (0.7 + 0.5 * depth) * mix(2.3, 1.0, step(1.5, uPass)), 1.0, uMaxPointSize);

    // Light — GridMath.light is the documented spec; shimmer and hash here.
    float light = 0.12 + 1.3 * energy + 0.5 * uFlare * body + 0.6 * uPunch;
    light *= 0.72 + 0.28 * hash(aId + 21.0);
    light *= 1.0 + uSparkle * 0.3 * sin(uPhase * 2.3 + hash(aId + 5.0) * TAU + x * 4.0);
    vLight = light * depth;
    vColor = mix(mix(vec3(0.15, 0.85, 0.55), vec3(0.25, 1.0, 0.7), depth),
                 mix(vec3(0.45, 0.55, 1.0), vec3(0.35, 0.95, 1.0), depth), uPalette);
}
"""
}

class GridRenderer(source: BandSource, refreshRateHz: Float = 60f) :
    BaseVisualRenderer(source, refreshRateHz) {
    override val vert: String = GridVisual.VERT
    override val rows: Int = 32
}
