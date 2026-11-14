package com.rikodev.music.android.dna.render.visual

import com.rikodev.music.android.dna.core.BandSource
import kotlin.math.pow

// ─────────────────────────────────────────────────────────────────────────────
// FRACTAL BLOOM (Phase 4 — visual 5 of 9)
//
// A recursive mandala that blooms inward and outward with the music. Each
// particle rides a petal orbit whose complexity grows with the class detail.
// Class reactions:
//
//   bass      — violent blooming outward, deep folds, burning flare
//   vocal     — gentle breathing, clean petals, slow elegant rotation
//   low vocal — deep slow pulse through the mandala
//   low sound — almost closed, faint glow, resting mandala
//   full mix  — maximum chaotic bloom, fire everywhere
// ─────────────────────────────────────────────────────────────────────────────

/** CPU-side spec of the bloom laws — the VERT shader mirrors these formulas. */
object BloomMath {
    /** Mandala radius — mirrors BloomVisual.VERT `radius`. */
    fun radius(body: Float, fold: Float, breathe: Float, phase: Float): Float =
        0.52f + 0.12f * body * fold + 0.04f * breathe * kotlin.math.sin(phase.toDouble()).toFloat()

    /** Petal wave amplitude — mirrors BloomVisual.VERT `amp`. */
    fun amplitude(energy: Float, fold: Float, detail: Float): Float =
        0.12f * energy * (0.5f + 0.5f * fold) * (0.7f + 0.3f * detail)

    /** Bloom light — mirrors BloomVisual.VERT `light`. */
    fun light(energy: Float, body: Float, flare: Float, punch: Float): Float =
        0.14f + 1.35f * energy + 0.55f * flare * body + 0.65f * punch
}

object BloomVisual {
    const val ID = "bloom"
    const val NAME = "Fractal Bloom"

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

    // Mirrored spectrum drives petal energy.
    float freq = abs(2.0 * column / uColumns - 1.0) * 39.0;
    int lo = int(floor(freq));
    int hi = int(min(float(lo) + 1.0, 39.0));
    float energy = clamp(mix(uBands[lo], uBands[hi], fract(freq)) * uGain, 0.0, 1.0);

    // Mandala radius — BloomMath.radius is the documented spec.
    float radius = 0.52 + 0.12 * body * uFold + 0.04 * uBreathe * sin(uPhase * 0.35);

    // Petal wave — BloomMath.amplitude is the documented spec.
    float petals = floor(8.0 + uDetail * 12.0);
    float wave = sin(angle * petals + uPhase + v * 4.0 * uDetail) * 0.5
               + sin(angle * petals * 2.0 - uPhase * 0.7 + v * 2.0) * 0.25;
    float amp = 0.12 * energy * (0.5 + 0.5 * uFold) * (0.7 + 0.3 * uDetail);
    float r = radius + wave * amp * (0.3 + v * 0.7);

    // Slow rotation + class spin.
    float spin = uRoll * 0.3 + uTime * 0.08 * (0.4 + uFold);
    float x = r * cos(angle + spin);
    float y = r * sin(angle + spin);
    float z = wave * body * 0.15 * uFold;

    vec2 scale = min(uViewport.x, uViewport.y) / uViewport;
    gl_Position = vec4(vec2(x, y) * scale, 0.0, 1.0);

    float depth = clamp(0.6 + z * 1.2, 0.35, 1.3);
    gl_PointSize = clamp(uPointSize * (0.5 + 1.0 * energy + 0.3 * body) * (0.8 + 0.5 * uWidth)
        * (0.7 + 0.5 * depth) * mix(2.3, 1.0, step(1.5, uPass)), 1.0, uMaxPointSize);

    // Light — BloomMath.light is the documented spec; shimmer and hash here.
    float light = 0.14 + 1.35 * energy + 0.55 * uFlare * body + 0.65 * uPunch;
    light *= 0.73 + 0.27 * hash(aId + 15.0);
    light *= 1.0 + uSparkle * 0.3 * sin(uPhase * 2.2 + hash(aId + 8.0) * TAU + angle * 3.0);
    vLight = light * depth;
    vColor = mix(mix(vec3(1.0, 0.88, 0.0), vec3(1.0, 0.98, 0.42), depth * 0.6),
                 mix(vec3(0.45, 0.55, 1.0), vec3(0.35, 0.95, 1.0), depth * 0.6), uPalette);
}
"""
}

class BloomRenderer(source: BandSource, refreshRateHz: Float = 60f) :
    BaseVisualRenderer(source, refreshRateHz) {
    override val vert: String = BloomVisual.VERT
    override val rows: Int = 32
}
