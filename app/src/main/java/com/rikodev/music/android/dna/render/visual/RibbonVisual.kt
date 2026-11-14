package com.rikodev.music.android.dna.render.visual

import com.rikodev.music.android.dna.core.BandSource

// ─────────────────────────────────────────────────────────────────────────────
// SPECTRAL RIBBON (Phase 4 — visual 1 of 9)
//
// A fabric of spectrum-following strands. Every point rides the mirrored
// spectrum along x, so the ribbon BURNS where the music is strong and lies
// flat where it is quiet. Class reactions:
//
//   bass      — the fabric folds with huge waves, thick burning strands
//   vocal     — thin ripple, clean parallel ribs (uDetail), slow breathing
//   low vocal — deep slow swell
//   low sound — near-flat calm lines, faint light
//   full mix  — plasma fire everywhere
// ─────────────────────────────────────────────────────────────────────────────

/** CPU-side spec of the ribbon laws — the VERT shader mirrors these formulas. */
object RibbonMath {
    /** Wave displacement amplitude — mirrors RibbonVisual.VERT `amp`. */
    fun amplitude(energy: Float, body: Float, fold: Float): Float =
        (0.05f + 0.22f * energy) * (0.5f + 0.5f * fold) + body * 0.12f * fold

    /** Strand light before hash/shimmer — mirrors RibbonVisual.VERT `light`. */
    fun light(energy: Float, body: Float, punch: Float, flare: Float, bass: Float): Float =
        0.18f + 1.5f * energy + 0.6f * body + 0.6f * punch + 0.5f * flare * bass
}

object RibbonVisual {
    const val ID = "ribbon"
    const val NAME = "Spectral Ribbon"

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
    float strand = floor(aId / uColumns);
    float v = strand / 23.0;
    float column = mod(aId, uColumns);
    float x = column / uColumns * 2.0 - 1.0;

    // Mirrored spectrum across the ribbon — the fabric follows the music.
    float freq = abs(x) * 39.0;
    int lo = int(floor(freq));
    int hi = int(min(float(lo) + 1.0, 39.0));
    float energy = clamp(mix(uBands[lo], uBands[hi], fract(freq)) * uGain, 0.0, 1.0);
    float trailE = clamp(mix(uTrail[lo], uTrail[hi], fract(freq)) * uGain, 0.0, 1.0);
    float transient = clamp(abs(energy - trailE) * 2.0, 0.0, 1.0);
    float body = clamp(uBass * uGain, 0.0, 1.0);

    // Fabric wave — RibbonMath.amplitude is the documented spec.
    float wave = 0.60 * sin(3.0 * x + uPhase + v * 6.0 * uDetail)
               + 0.40 * sin(5.0 * x - uPhase * 0.8 + v * 4.0 * uDetail);
    float amp = (0.05 + 0.22 * energy) * (0.5 + 0.5 * uFold) + body * 0.12 * uFold;

    float breathe = 1.0 + uBreathe * 0.06 * sin(uPhase * 0.3 + v * 3.0);
    float y = (v - 0.5) * 1.1 * breathe + wave * amp;
    float z = wave * body * 0.25 * uFold;

    vec2 scale = min(uViewport.x, uViewport.y) / uViewport;
    gl_Position = vec4(vec2(x * 0.95, y) * scale, 0.0, 1.0);

    float depth = clamp(0.6 + z * 1.2, 0.35, 1.3);
    gl_PointSize = clamp(uPointSize * (0.6 + 1.1 * energy) * (0.8 + 0.5 * uWidth)
        * (0.7 + 0.5 * depth) * mix(2.3, 1.0, step(1.5, uPass)), 1.0, uMaxPointSize);

    // RibbonMath.light is the documented spec; shimmer and hash live here.
    float light = 0.18 + 1.5 * energy + 0.6 * body + 0.6 * uPunch + 0.5 * uFlare * uBass;
    light += 0.8 * transient;
    light *= 0.75 + 0.25 * hash(aId);
    light *= 1.0 + uSparkle * 0.30 * sin(uPhase * 2.5 + hash(aId + 7.0) * TAU + x * 6.0);
    vLight = light * depth;
    vColor = mix(mix(vec3(1.0, 0.88, 0.0), vec3(1.0, 0.98, 0.42), depth * 0.6),
                 mix(vec3(0.45, 0.55, 1.0), vec3(0.35, 0.95, 1.0), depth * 0.6), uPalette);
}
"""
}

class RibbonRenderer(source: BandSource, refreshRateHz: Float = 60f) :
    BaseVisualRenderer(source, refreshRateHz) {
    override val vert: String = RibbonVisual.VERT
    override val rows: Int = 24
}
