package com.rikodev.music.android.dna.render.visual

import com.rikodev.music.android.dna.core.BandSource

// ─────────────────────────────────────────────────────────────────────────────
// PARTICLE SWARM ORGANISM (Phase 4 — visual 4 of 9)
//
// A living swarm of particles that breathes, strikes and drifts as ONE
// organism. Each particle orbits on its own lobe of the sphere, so the swarm
// keeps a coherent silhouette while the whole mass moves. Class reactions:
//
//   bass      — the organism contracts violently and flares (strike pose)
//   vocal     — gentle breathing, smooth flowing orbits, calm
//   low vocal — deep slow swelling
//   low sound — almost frozen, faint glow, resting organism
//   full mix  — maximum chaos and fire
// ─────────────────────────────────────────────────────────────────────────────

/** CPU-side spec of the swarm laws — the VERT shader mirrors these formulas. */
object SwarmMath {
    /** Organism radius — mirrors SwarmVisual.VERT `radius`. */
    fun radius(body: Float, fold: Float, breathe: Float, phase: Float): Float =
        0.62f - 0.18f * body * fold + 0.04f * breathe * kotlin.math.sin((phase * 0.4f).toDouble()).toFloat()

    /** Orbital energy push — mirrors SwarmVisual.VERT `push`. */
    fun push(energy: Float, body: Float, punch: Float): Float =
        0.10f * energy + 0.14f * body * punch

    /** Particle light — mirrors SwarmVisual.VERT `light`. */
    fun light(energy: Float, body: Float, flare: Float, punch: Float): Float =
        0.15f + 1.4f * energy + 0.55f * flare * body + 0.7f * punch
}

object SwarmVisual {
    const val ID = "swarm"
    const val NAME = "Particle Swarm Organism"

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
    float body = clamp(uBass * uGain, 0.0, 1.0);

    // Each particle rides its own lobe frequency — a coherent swarm body.
    float freq = abs(2.0 * column / uColumns - 1.0) * 39.0;
    int lo = int(floor(freq));
    int hi = int(min(float(lo) + 1.0, 39.0));
    float energy = clamp(mix(uBands[lo], uBands[hi], fract(freq)) * uGain, 0.0, 1.0);

    // Organism radius — SwarmMath.radius is the documented spec.
    float radius = 0.62 - 0.18 * body * uFold + 0.04 * uBreathe * sin(uPhase * 0.4);

    // Orbital wobble and energy push — SwarmMath.push is the documented spec.
    float wobble = 0.10 * sin(uPhase * 0.6 + v * 3.0 * uDetail + angle * 2.0) * (0.3 + uFold * body);
    float push = 0.10 * energy + 0.14 * body * uPunch;
    float r = radius + wobble * 0.4 + push;

    // Slow orbit + class spin.
    float orbit = uRoll * 0.4 + uTime * 0.06 * (0.3 + uFold);
    float x = r * cos(angle + orbit);
    float y = r * sin(angle + orbit) * 0.85;
    float z = r * sin(angle * 2.0 + uPhase * 0.5) * 0.2 * uFold;

    vec2 scale = min(uViewport.x, uViewport.y) / uViewport;
    gl_Position = vec4(vec2(x, y) * scale, 0.0, 1.0);

    float depth = clamp(0.6 + z * 1.2, 0.35, 1.3);
    gl_PointSize = clamp(uPointSize * (0.5 + 1.0 * energy + 0.4 * body) * (0.8 + 0.5 * uWidth)
        * (0.7 + 0.5 * depth) * mix(2.3, 1.0, step(1.5, uPass)), 1.0, uMaxPointSize);

    // Light — SwarmMath.light is the documented spec; shimmer and hash here.
    float light = 0.15 + 1.4 * energy + 0.55 * uFlare * body + 0.7 * uPunch;
    light *= 0.74 + 0.26 * hash(aId + 11.0);
    light *= 1.0 + uSparkle * 0.3 * sin(uPhase * 2.4 + hash(aId + 6.0) * TAU);
    vLight = light * depth;
    vColor = mix(mix(vec3(1.0, 0.88, 0.0), vec3(1.0, 0.98, 0.42), depth * 0.6),
                 mix(vec3(0.45, 0.55, 1.0), vec3(0.35, 0.95, 1.0), depth * 0.6), uPalette);
}
"""
}

class SwarmRenderer(source: BandSource, refreshRateHz: Float = 60f) :
    BaseVisualRenderer(source, refreshRateHz) {
    override val vert: String = SwarmVisual.VERT
    override val rows: Int = 32
}
