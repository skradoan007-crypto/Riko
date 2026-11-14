package com.rikodev.music.android.dna.render.visual

import com.rikodev.music.android.dna.core.BandSource

// ─────────────────────────────────────────────────────────────────────────────
// EMBER STORM (Phase 4 — visual 7 of 9)
//
// Rising sparks and fire embers blown by the music's wind. Each particle
// starts at the bottom and drifts upward with turbulent swirls—like sparks
// from a forge. Class reactions:
//
//   bass      — violent upward bursts, thick embers, intense flare
//   vocal     — gentle rising drift, thin elegant sparks, calm sway
//   low vocal — deep slow smoldering rise
//   low sound — almost extinguished, faint drifting glow
//   full mix  — raging fire storm, maximum sparks everywhere
// ─────────────────────────────────────────────────────────────────────────────

/** CPU-side spec of the ember laws — the VERT shader mirrors these formulas. */
object EmberMath {
    /** Upward drift speed — mirrors EmberVisual.VERT `drift`. */
    fun drift(body: Float, fold: Float, punch: Float): Float =
        0.12f + 0.10f * body * fold + 0.08f * punch

    /** Turbulent swirl amplitude — mirrors EmberVisual.VERT `swirl`. */
    fun swirl(energy: Float, fold: Float): Float =
        0.06f * energy * (0.5f + 0.5f * fold)

    /** Ember light — mirrors EmberVisual.VERT `light`. */
    fun light(energy: Float, body: Float, flare: Float, punch: Float): Float =
        0.12f + 1.3f * energy + 0.6f * flare * body + 0.65f * punch
}

object EmberVisual {
    const val ID = "ember"
    const val NAME = "Ember Storm"

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
    float particle = floor(aId / uColumns);
    float v = particle / 31.0;
    float column = mod(aId, uColumns);
    float t = column / uColumns;
    float x = t * 2.0 - 1.0;
    float body = clamp(uBass * uGain, 0.0, 1.0);

    // Mirrored spectrum adds heat to the embers.
    float freq = abs(x) * 39.0;
    int lo = int(floor(freq));
    int hi = int(min(float(lo) + 1.0, 39.0));
    float energy = clamp(mix(uBands[lo], uBands[hi], fract(freq)) * uGain, 0.0, 1.0);

    // Each ember starts at a random bottom position and rises.
    float seed = hash(aId + 23.0);
    float startY = -0.8 + seed * 0.4;
    float startX = x + (seed - 0.5) * 0.8;

    // Upward drift — EmberMath.drift is the documented spec.
    float drift = 0.12 + 0.10 * body * uFold + 0.08 * uPunch;
    float cycle = fract(v + uTime * drift * 0.5);
    float life = 1.0 - cycle;
    float y = startY + cycle * 1.6;

    // Turbulent swirl — EmberMath.swirl is the documented spec.
    float swirl = 0.06 * energy * (0.5 + 0.5 * uFold);
    float sway = sin(uPhase * 0.8 + v * 5.0 * uDetail + seed * TAU) * swirl;
    float xx = startX + sway;

    vec2 scale = min(uViewport.x, uViewport.y) / uViewport;
    gl_Position = vec4(vec2(xx * 0.95, y) * scale, 0.0, 1.0);

    float depth = clamp(0.5 + life * 0.7, 0.3, 1.3);
    gl_PointSize = clamp(uPointSize * (0.4 + 0.9 * energy + 0.3 * body) * (0.8 + 0.5 * uWidth)
        * (0.5 + 0.5 * depth) * mix(2.3, 1.0, step(1.5, uPass)), 1.0, uMaxPointSize);

    // Light — EmberMath.light is the documented spec; shimmer and hash here.
    float light = 0.12 + 1.3 * energy + 0.6 * uFlare * body + 0.65 * uPunch;
    light *= (0.4 + 0.6 * life);
    light *= 0.72 + 0.28 * hash(aId + 19.0);
    light *= 1.0 + uSparkle * 0.3 * sin(uPhase * 2.5 + hash(aId + 7.0) * TAU + seed * 6.0);
    vLight = light;
    vColor = mix(mix(vec3(1.0, 0.45, 0.05), vec3(1.0, 0.75, 0.2), life),
                 mix(vec3(0.45, 0.55, 1.0), vec3(0.75, 0.35, 1.0), life), uPalette);
}
"""
}

class EmberRenderer(source: BandSource, refreshRateHz: Float = 60f) :
    BaseVisualRenderer(source, refreshRateHz) {
    override val vert: String = EmberVisual.VERT
    override val rows: Int = 32
}
