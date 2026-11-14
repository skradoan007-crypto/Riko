package com.rikodev.music.android.dna.render.visual

import com.rikodev.music.android.dna.core.BandSource

// ─────────────────────────────────────────────────────────────────────────────
// LIQUID MIRROR (Phase 4 — visual 6 of 9)
//
// A molten metal surface that reflects and ripples with every sound. Points
// sit on a grid and displace vertically with mirrored waves—like dropping
// stones in liquid mercury. Class reactions:
//
//   bass      — violent splashes, deep metal waves, bright flare
//   vocal     — gentle elegant ripples, calm breathing surface
//   low vocal — deep slow swelling waves
//   low sound — almost frozen, flat faint mirror
//   full mix  — chaotic boiling fire, maximum waves
// ─────────────────────────────────────────────────────────────────────────────

/** CPU-side spec of the mirror laws — the VERT shader mirrors these formulas. */
object MirrorMath {
    /** Wave displacement — mirrors MirrorVisual.VERT `displace`. */
    fun displacement(energy: Float, body: Float, fold: Float): Float =
        0.10f * energy * (0.5f + 0.5f * fold) + 0.12f * body * fold

    /** Surface breathing — mirrors MirrorVisual.VERT `breathe`. */
    fun breathe(breathe: Float, phase: Float): Float =
        1f + breathe * 0.05f * kotlin.math.sin(phase.toDouble()).toFloat()

    /** Mirror light — mirrors MirrorVisual.VERT `light`. */
    fun light(energy: Float, body: Float, flare: Float, punch: Float): Float =
        0.13f + 1.3f * energy + 0.55f * flare * body + 0.6f * punch
}

object MirrorVisual {
    const val ID = "mirror"
    const val NAME = "Liquid Mirror"

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
    float y = (v - 0.5) * 1.5;
    float body = clamp(uBass * uGain, 0.0, 1.0);

    // Mirrored spectrum across the surface.
    float freq = abs(x) * 39.0;
    int lo = int(floor(freq));
    int hi = int(min(float(lo) + 1.0, 39.0));
    float energy = clamp(mix(uBands[lo], uBands[hi], fract(freq)) * uGain, 0.0, 1.0);

    // Wave displacement — MirrorMath.displacement is the documented spec.
    float wave1 = sin(x * 3.0 + uPhase + y * 2.0 * uDetail) * 0.5;
    float wave2 = sin(x * 5.0 - uPhase * 0.7 + y * 3.5 * uDetail) * 0.3;
    float wave3 = sin((x + y) * 2.5 + uPhase * 0.5) * 0.2;
    float displace = 0.10 * energy * (0.5 + 0.5 * uFold) + 0.12 * body * uFold;
    float z = (wave1 + wave2 + wave3) * displace;

    // Breathing surface.
    float breathe = 1.0 + uBreathe * 0.05 * sin(uPhase * 0.3);
    y *= breathe;

    vec2 scale = min(uViewport.x, uViewport.y) / uViewport;
    gl_Position = vec4(vec2(x * 0.95, y + z) * scale, 0.0, 1.0);

    float depth = clamp(0.6 + z * 1.2, 0.35, 1.3);
    gl_PointSize = clamp(uPointSize * (0.5 + 1.0 * energy + 0.3 * body) * (0.8 + 0.5 * uWidth)
        * (0.7 + 0.5 * depth) * mix(2.3, 1.0, step(1.5, uPass)), 1.0, uMaxPointSize);

    // Light — MirrorMath.light is the documented spec; shimmer and hash here.
    float light = 0.13 + 1.3 * energy + 0.55 * uFlare * body + 0.6 * uPunch;
    light *= 0.72 + 0.28 * hash(aId + 17.0);
    light *= 1.0 + uSparkle * 0.3 * sin(uPhase * 2.4 + hash(aId + 3.0) * TAU + x * 5.0);
    vLight = light * depth;
    vColor = mix(mix(vec3(0.85, 0.90, 0.95), vec3(0.65, 0.75, 0.90), depth),
                 mix(vec3(0.45, 0.55, 1.0), vec3(0.35, 0.95, 1.0), depth), uPalette);
}
"""
}

class MirrorRenderer(source: BandSource, refreshRateHz: Float = 60f) :
    BaseVisualRenderer(source, refreshRateHz) {
    override val vert: String = MirrorVisual.VERT
    override val rows: Int = 32
}
