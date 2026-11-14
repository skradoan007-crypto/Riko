package com.rikodev.music.android.dna.render.visual

import com.rikodev.music.android.dna.core.BandSource

// ─────────────────────────────────────────────────────────────────────────────
// PULSE RINGS (Phase 4 — visual 2 of 9)
//
// Expanding shockwave rings that breathe with the beat. Every ring lives one
// life cycle from birth at the core to death at the rim. Class reactions:
//
//   bass      — fast thick rings, violent expansion, burning flare
//   vocal     — slow thin elegant rings, wide spacing, gentle light
//   low vocal — deep slow pulses
//   low sound — faint rings barely drifting (calm rest state)
//   full mix  — ring storms, maximum light
// ─────────────────────────────────────────────────────────────────────────────

/** CPU-side spec of the ring laws — the VERT shader mirrors these formulas. */
object PulseMath {
    /** Ring expansion speed — mirrors PulseRingsVisual.VERT `speed`. */
    fun speed(fold: Float, body: Float, punch: Float): Float =
        0.10f * (0.3f + fold * 0.5f + body * 0.6f + punch * 0.4f)

    /** Ring thickness multiplier — mirrors PulseRingsVisual.VERT point-size base. */
    fun thickness(width: Float): Float = 0.6f + 0.9f * width

    /** Ring light at life stage 0..1 — mirrors PulseRingsVisual.VERT `light`. */
    fun light(life: Float, flare: Float, body: Float, fold: Float): Float =
        0.15f + 1.4f * life * (0.4f + 0.6f * flare) + 0.8f * body * fold * life
}

object PulseRingsVisual {
    const val ID = "pulse"
    const val NAME = "Pulse Rings"

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
const float PI = 3.14159265359;
float hash(float x) { return fract(sin(x * 12.9898 + uSeed * 39.71) * 43758.5453); }

void main() {
    float ring = floor(aId / uColumns);
    float v = ring / 31.0;
    float column = mod(aId, uColumns);
    float angle = TAU * column / uColumns;
    float body = clamp(uBass * uGain, 0.0, 1.0);

    // Mirrored spectrum ripple around the circumference.
    float freq = abs(2.0 * column / uColumns - 1.0) * 39.0;
    int lo = int(floor(freq));
    int hi = int(min(float(lo) + 1.0, 39.0));
    float energy = clamp(mix(uBands[lo], uBands[hi], fract(freq)) * uGain, 0.0, 1.0);

    // Expanding rings — PulseMath.speed is the documented spec.
    float speed = 0.10 * (0.3 + uFold * 0.5 + body * 0.6 + uPunch * 0.4);
    float cycle = fract(v + uTime * speed);
    float life = sin(cycle * PI);
    float radius = 0.12 + 0.62 * cycle + energy * 0.05 * uFold;

    // Class breathing and a soft circumference ripple.
    radius *= 1.0 + uBreathe * 0.035 * sin(uTime * 0.6 + v * 3.0);
    radius += 0.012 * sin(angle * 5.0 + uPhase * (0.5 + uDetail)) * (0.3 + uFold * body);

    vec2 scale = min(uViewport.x, uViewport.y) / uViewport;
    gl_Position = vec4(radius * vec2(cos(angle), sin(angle)) * scale, 0.0, 1.0);

    // Thickness — PulseMath.thickness is the documented spec.
    gl_PointSize = clamp(uPointSize * (0.6 + 0.9 * uWidth) * (0.5 + 0.9 * life)
        * (1.0 + 0.6 * body + 0.5 * uPunch) * mix(2.3, 1.0, step(1.5, uPass)),
        1.0, uMaxPointSize);

    // Light — PulseMath.light is the documented spec; shimmer and hash here.
    float light = 0.15 + 1.4 * life * (0.4 + 0.6 * uFlare) + 0.8 * body * uFold * life;
    light *= 0.78 + 0.22 * hash(aId + 13.0);
    light *= 1.0 + uSparkle * 0.3 * sin(uPhase * 2.2 + hash(aId + 5.0) * TAU);
    vLight = light;
    vColor = mix(mix(vec3(1.0, 0.90, 0.05), vec3(1.0, 0.98, 0.5), life),
                 mix(vec3(0.45, 0.55, 1.0), vec3(0.35, 0.95, 1.0), life), uPalette);
}
"""
}

class PulseRingsRenderer(source: BandSource, refreshRateHz: Float = 60f) :
    BaseVisualRenderer(source, refreshRateHz) {
    override val vert: String = PulseRingsVisual.VERT
    override val rows: Int = 32
}
