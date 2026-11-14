package com.rikodev.music.android.dna.render.visual

import com.rikodev.music.android.dna.core.BandSource

// ─────────────────────────────────────────────────────────────────────────────
// AURORA CURTAIN (Phase 4 — visual 3 of 9)
//
// Vertical curtains of northern light swaying like fabric in slow wind.
// Mirrored spectrum drives how tall and bright each curtain burns. Class
// reactions:
//
//   bass      — curtains fold deep and flare wide, tall burning walls
//   vocal     — gentle sway, thin elegant curtains, slow breathing
//   low vocal — deep slow rolling waves
//   low sound — almost still, faint glow, calm
//   full mix  — sky-wide fire, maximum sway
// ─────────────────────────────────────────────────────────────────────────────

/** CPU-side spec of the curtain laws — the VERT shader mirrors these formulas. */
object AuroraMath {
    /** Horizontal sway — mirrors AuroraVisual.VERT `sway`. */
    fun sway(body: Float, fold: Float): Float = 0.10f * body * (0.4f + fold)

    /** Curtain amplitude — mirrors AuroraVisual.VERT `amp`. */
    fun amplitude(body: Float, fold: Float, mid: Float): Float =
        (0.05f + 0.15f * body) * (0.4f + 0.6f * fold) + mid * 0.06f

    /** Curtain height — mirrors AuroraVisual.VERT `height`. */
    fun height(energy: Float, breathe: Float, phase: Float): Float =
        (0.55f + 0.45f * energy) * (1f + breathe * 0.08f * kotlin.math.sin(phase.toDouble()).toFloat())

    /** Curtain light — mirrors AuroraVisual.VERT `light`. */
    fun light(energy: Float, flare: Float, body: Float, punch: Float): Float =
        0.12f + 1.3f * energy + 0.5f * flare * body + 0.6f * punch
}

object AuroraVisual {
    const val ID = "aurora"
    const val NAME = "Aurora Curtain"

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
    float curtain = floor(aId / uColumns);
    float v = curtain / 15.0;
    float column = mod(aId, uColumns);
    float t = column / uColumns;
    float body = clamp(uBass * uGain, 0.0, 1.0);
    float mids = clamp(uMid * uGain, 0.0, 1.0);

    // Mirrored spectrum across the sky — curtains burn with the music.
    float freq = abs(2.0 * t - 1.0) * 39.0;
    int lo = int(floor(freq));
    int hi = int(min(float(lo) + 1.0, 39.0));
    float energy = clamp(mix(uBands[lo], uBands[hi], fract(freq)) * uGain, 0.0, 1.0);

    // Sway and fold — AuroraMath.sway/amplitude are the documented spec.
    float x = t * 2.0 - 1.0;
    float sway = 0.10 * body * (0.4 + uFold);
    float amp = (0.05 + 0.15 * body) * (0.4 + 0.6 * uFold) + mids * 0.06;
    float wave = sin(x * 2.5 + uPhase + v * 4.0 * uDetail) * amp
               + sin(x * 4.5 - uPhase * 0.7 + v * 2.5 * uDetail) * amp * 0.5;
    float yCenter = wave + (v - 0.5) * 0.12 * uDetail;

    // Height — AuroraMath.height is the documented spec.
    float height = (0.55 + 0.45 * energy) * (1.0 + uBreathe * 0.08 * sin(uPhase * 0.3));
    float y = yCenter + (t - 0.5) * 2.0 * height * 0.0;

    vec2 scale = min(uViewport.x, uViewport.y) / uViewport;
    // Curtains hang from the sky: each column's points stack from top to center.
    float stack = (column / uColumns);
    y = yCenter + (stack - 0.5) * height;
    float xx = x + sway * sin(uPhase * 0.5 + v * 3.0 + stack * 2.0);
    gl_Position = vec4(vec2(xx * 0.95, y) * scale, 0.0, 1.0);

    float depth = clamp(0.6 + stack * 0.6, 0.35, 1.25);
    gl_PointSize = clamp(uPointSize * (0.5 + 0.9 * energy) * (0.8 + 0.5 * uWidth)
        * (0.7 + 0.5 * depth) * mix(2.3, 1.0, step(1.5, uPass)), 1.0, uMaxPointSize);

    // Light — AuroraMath.light is the documented spec; shimmer and hash here.
    float light = 0.12 + 1.3 * energy + 0.5 * uFlare * body + 0.6 * uPunch;
    light *= 0.72 + 0.28 * hash(aId + 9.0);
    light *= 1.0 + uSparkle * 0.3 * sin(uPhase * 2.3 + hash(aId + 4.0) * TAU + x * 4.0);
    vLight = light * depth;
    vColor = mix(mix(vec3(0.16, 0.90, 1.0), vec3(0.35, 1.0, 0.6), stack),
                 mix(vec3(0.45, 0.55, 1.0), vec3(0.75, 0.35, 1.0), stack), uPalette);
}
"""
}

class AuroraRenderer(source: BandSource, refreshRateHz: Float = 60f) :
    BaseVisualRenderer(source, refreshRateHz) {
    override val vert: String = AuroraVisual.VERT
    override val rows: Int = 16
}
