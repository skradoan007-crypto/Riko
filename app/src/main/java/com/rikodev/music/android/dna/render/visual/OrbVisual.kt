package com.rikodev.music.android.dna.render.visual

import com.rikodev.music.android.dna.core.BandSource

// ─────────────────────────────────────────────────────────────────────────────
// MORPH ORB (Phase 4 — visual 9 of 9, the crown jewel)
//
// A sphere that changes its very CHARACTER per sound class—not just size or
// brightness, but its geometry morphs. Bass makes it a spiked weapon, vocal
// turns it into a smooth flowing pearl, quiet shrinks it to a resting dot.
// This is the ULTIMATE expression of class-wise reaction. Class reactions:
//
//   bass      — violent spikes explode outward, deep folds, burning flare
//   vocal     — smooth elegant pearl, gentle breathing, clean surface
//   low vocal — deep slow swell, soft undulation
//   low sound — tiny resting sphere, faint glow, almost frozen
//   full mix  — chaotic fire sphere, maximum spikes everywhere
// ─────────────────────────────────────────────────────────────────────────────

/** CPU-side spec of the orb laws — the VERT shader mirrors these formulas. */
object OrbMath {
    /** Sphere radius — mirrors OrbVisual.VERT `radius`. */
    fun radius(body: Float, fold: Float, breathe: Float, phase: Float): Float =
        0.55f - 0.12f * body * fold + 0.05f * breathe * kotlin.math.sin(phase.toDouble()).toFloat()

    /** Spike amplitude (bass = spiky, vocal = smooth) — mirrors VERT `spike`. */
    fun spike(energy: Float, fold: Float, detail: Float): Float =
        0.15f * energy * fold * (0.5f + 0.5f * detail)

    /** Orb light — mirrors OrbVisual.VERT `light`. */
    fun light(energy: Float, body: Float, flare: Float, punch: Float): Float =
        0.15f + 1.4f * energy + 0.55f * flare * body + 0.7f * punch
}

object OrbVisual {
    const val ID = "orb"
    const val NAME = "Morph Orb"

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
    float ring = floor(aId / uColumns);
    float v = ring / 31.0;
    float column = mod(aId, uColumns);
    float angle = TAU * column / uColumns;
    float phi = (v - 0.5) * 3.14159;
    float body = clamp(uBass * uGain, 0.0, 1.0);

    // Mirrored spectrum drives spikes and surface energy.
    float freq = abs(2.0 * column / uColumns - 1.0) * 39.0;
    int lo = int(floor(freq));
    int hi = int(min(float(lo) + 1.0, 39.0));
    float energy = clamp(mix(uBands[lo], uBands[hi], fract(freq)) * uGain, 0.0, 1.0);

    // Sphere base — OrbMath.radius is the documented spec.
    float radius = 0.55 - 0.12 * body * uFold + 0.05 * uBreathe * sin(uPhase * 0.35);

    // Spikes explode with bass energy — OrbMath.spike is the documented spec.
    float spike = 0.15 * energy * uFold * (0.5 + 0.5 * uDetail);
    float surface = sin(angle * 6.0 + uPhase + phi * 4.0 * uDetail) * 0.5
                  + sin(angle * 12.0 - uPhase * 0.7 + phi * 8.0) * 0.25;
    float r = radius + surface * spike;

    // Spherical to cartesian with slow rotation.
    float x = r * cos(phi) * cos(angle + uRoll * 0.3);
    float y = r * sin(phi);
    float z = r * cos(phi) * sin(angle + uRoll * 0.3);

    vec2 scale = min(uViewport.x, uViewport.y) / uViewport;
    gl_Position = vec4(vec2(x, y) * scale, 0.0, 1.0);

    float depth = clamp(0.6 + z * 1.2, 0.35, 1.3);
    gl_PointSize = clamp(uPointSize * (0.5 + 1.0 * energy + 0.4 * body) * (0.8 + 0.5 * uWidth)
        * (0.7 + 0.5 * depth) * mix(2.3, 1.0, step(1.5, uPass)), 1.0, uMaxPointSize);

    // Light — OrbMath.light is the documented spec; shimmer and hash here.
    float light = 0.15 + 1.4 * energy + 0.55 * uFlare * body + 0.7 * uPunch;
    light *= 0.74 + 0.26 * hash(aId + 27.0);
    light *= 1.0 + uSparkle * 0.3 * sin(uPhase * 2.4 + hash(aId + 9.0) * TAU + angle * 5.0);
    vLight = light * depth;
    vColor = mix(mix(vec3(1.0, 0.88, 0.0), vec3(1.0, 0.98, 0.42), depth * 0.6),
                 mix(vec3(0.45, 0.55, 1.0), vec3(0.35, 0.95, 1.0), depth * 0.6), uPalette);
}
"""
}

class OrbRenderer(source: BandSource, refreshRateHz: Float = 60f) :
    BaseVisualRenderer(source, refreshRateHz) {
    override val vert: String = OrbVisual.VERT
    override val rows: Int = 32
}
