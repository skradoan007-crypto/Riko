package com.rikodev.music.android.dna.render.visual

import com.rikodev.music.android.dna.core.BandSource
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.abs
import kotlin.math.PI
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// LIVE·DOT SPECTRUM v3 (Phase 6 — THE CROWN JEWEL, target video match)
//
// The hero visual: a concentric polar dot-matrix mesh living inside a neon
// corona ring. Rest state = all dots merged at the outer rim (thick bright
// border, empty dark center). On beat, 5 vibration zones erupt on the ring,
// sending waves crashing inward that fold the mesh like 3D fabric — dots
// overlap into bright ribbons where the fold is deep, spread into individual
// points where it is shallow. Between beats the mesh snaps back to the rim.
//
// TARGET VIDEO BEHAVIOR (verified from frame-by-frame summary):
//   00:00-00:01 rest:      dots at rim, only bright ring visible
//   00:01-00:03 beat drop: 5 vibration points erupt → wave rushes inward
//   00:02 peak:            3D dome/sphere mesh fills the circle
//   00:05-00:11 folding:   mesh folds/twists like fabric, depth at center
//   00:04/00:08/00:11 retraction: dots snap back to rim between beats
//
// TECHNIQUE:
//   - Concentric polar grid (48 rings × columns), edge-clamped at rest
//   - Inward wave propagation with per-ring phase delay (traveling wave)
//   - 5 beat-vibration zones on the rim (uPunch-driven, localized)
//   - 3D z-displacement + pitch/roll rotation → fabric fold overlap
//   - Additive blending: overlap = bright neon ribbons (bloom effect)
//   - Fast retraction: BeatEnvelope snap + ringFactor edge-clamp
// ─────────────────────────────────────────────────────────────────────────────

/** CPU-side spec of the LIVE·DOT laws — the VERT shader mirrors these formulas. */
object LiveDotMath {
    const val VIBRATION_ZONES = 5

    /** Rest radius — edge-clamped, all rings near outer rim at rest. */
    fun restRadius(ringFactor: Float): Float = 1f - 0.06f * ringFactor

    /** Inward fold depth — bass + vibration push shells inward. */
    fun foldDepth(body: Float, ringFactor: Float, fold: Float, vibration: Float): Float =
        body * ringFactor * (0.45f + 0.55f * vibration) * (0.6f + 0.4f * fold)

    /** Wave amplitude — smooth harmonics scaled by energy and class. */
    fun waveAmplitude(body: Float, mids: Float, ringFactor: Float, fold: Float): Float =
        0.18f * (0.4f + 0.6f * mids) * ringFactor * (0.15f + 0.85f * body) * (0.6f + 0.4f * fold)

    /** 3D fabric fold depth. */
    fun depth(body: Float, ringFactor: Float, fold: Float): Float =
        body * ringFactor * fold

    /**
     * Beat vibration at an angle — localized punch zones on the rim.
     * Strongest at the outer edge (ringFactor=0), fades inward.
     * Creates the "4-6 vibration points" from the target video.
     */
    fun vibration(angle: Float, punch: Float, ringFactor: Float): Float {
        val zoneStep = (2.0 * PI / VIBRATION_ZONES).toFloat()
        val zoneAngle = floor(angle / zoneStep) * zoneStep
        val dist = abs(angle - zoneAngle)
        val rimWeight = (1f - ringFactor) * (1f - ringFactor)
        return punch * exp(-dist * 12f) * rimWeight
    }

    /** Inward propagation phase delay — outer rings react first. */
    fun propagationDelay(v: Float): Float = v * 2.5f
}

object LiveDotVisual {
    const val ID = "livedot"
    const val NAME = "LIVE·DOT SPECTRUM"

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
uniform mediump float uPass;
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
const float R = 0.72;

float hash(float x) { return fract(sin(x * 12.9898 + uSeed * 39.71) * 43758.5453); }

void main() {
    // ── Concentric polar grid ───────────────────────────────────────────
    float ring = floor(aId / uColumns);
    float v = ring / 47.0;                        // 0=outer, 1=inner
    float column = mod(aId, uColumns);
    float angle = TAU * column / uColumns;

    // Edge clamp: rim rows never leave the circle; mid rings fold most.
    float ringFactor = sin(v * PI);

    // ── Audio envelopes ─────────────────────────────────────────────────
    float body = clamp(uBass * uGain, 0.0, 1.0);
    float mids = clamp(uMid * uGain, 0.0, 1.0);

    // ── Beat vibration zones (4-6 points on the outer rim) ─────────────
    // LiveDotMath.vibration is the documented spec.
    // 5 fixed zones: each erupts with punch, strongest at the rim.
    float zoneStep = TAU / 5.0;
    float zoneAngle = floor(angle / zoneStep) * zoneStep;
    float zoneDist = abs(angle - zoneAngle);
    float rimWeight = (1.0 - ringFactor) * (1.0 - ringFactor);
    float vib = uPunch * exp(-zoneDist * 12.0) * rimWeight;

    // ── Inward wave propagation (traveling wave with per-ring delay) ───
    // Outer rings react first; inner rings follow with phase delay.
    // LiveDotMath.propagationDelay is the documented spec.
    float delay = v * 2.5;
    float wavePhase = uPhase - delay;

    // Smooth 3-harmonic wave — zero raw-FFT jitter.
    // uDetail sharpens per-ring ribs (clean parametric lines at 1.0).
    float wave = 0.50 * sin(3.0 * angle + wavePhase + uDetail * 0.25 * ring)
               + 0.30 * sin(5.0 * angle - 1.3 * wavePhase + uDetail * 0.15 * ring)
               + 0.20 * sin(2.0 * angle + 0.7 * wavePhase);

    // ── Radius: rest rim → inward fold → wave displacement ────────────
    // LiveDotMath.restRadius / foldDepth / waveAmplitude are the spec.
    float breathe = 1.0 + uBreathe * 0.035 * sin(uPhase * 0.35 + ring * 0.02);
    float radius = R * (1.0 - 0.06 * ringFactor) * breathe;

    // Inward fold: bass + vibration push shells toward center.
    // The vibration zones create the "4-6 points erupt" effect.
    float fold = body * ringFactor * (0.45 + 0.55 * vib) * (0.6 + 0.4 * uFold);
    radius -= R * fold;

    // Wave displacement: smooth fabric folds.
    float waveAmp = 0.18 * (0.4 + 0.6 * mids) * ringFactor * (0.15 + 0.85 * body) * (0.6 + 0.4 * uFold);
    radius += R * wave * waveAmp;

    // ── 3D depth: fabric folding + vibration depth ─────────────────────
    // When amplitude is high, rows overlap → additive = bright ribbons.
    float z = R * body * sin(angle + wavePhase) * ringFactor * uFold;
    z += vib * 0.15 * ringFactor;                // vibration adds local depth

    // Pitch/roll rotation for 3D feel.
    float pitch = 0.32 * sin(uPhase * 0.11) + 0.10 * body;
    vec3 p = vec3(radius * cos(angle), radius * sin(angle), z);
    float cp = cos(pitch);
    float sp = sin(pitch);
    p = vec3(p.x, p.y * cp - p.z * sp, p.y * sp + p.z * cp);
    float cr = cos(uRoll);
    float sr = sin(uRoll);
    p = vec3(p.x * cr - p.y * sr, p.x * sr + p.y * cr, p.z);

    // Mild perspective projection.
    float persp = 1.0 / (1.0 - p.z * 0.22);
    vec2 pos = p.xy * persp;
    vec2 scale = min(uViewport.x, uViewport.y) / uViewport;
    gl_Position = vec4(pos * scale, 0.0, 1.0);

    // ── Point size: depth-scaled, class-reactive ───────────────────────
    float depth = clamp(0.55 + 0.45 * (p.z * persp / (R * 0.6)), 0.3, 1.35);
    float size = uPointSize * (0.55 + 0.85 * depth) * (1.0 + 0.35 * body + 0.45 * uPunch);
    size *= (0.80 + 0.40 * uWidth);              // class width affects dot size
    gl_PointSize = clamp(size * mix(2.35, 1.0, step(1.5, uPass)), 1.0, uMaxPointSize);

    // ── Light: edge glow + energy + class reactions ────────────────────
    // Edge ribbon glow: shells hugging the rim burn brighter.
    float rimGlow = smoothstep(0.72, 1.0, radius / R);
    float light = 0.30 + 1.15 * rimGlow + 0.55 * body + 0.75 * uPunch;
    light += 0.40 * vib;                          // vibration zones burn bright
    light *= 0.80 + 0.40 * uFlare;               // class flare boosts glow
    light *= 0.72 + 0.28 * hash(aId + 17.0);
    // Class shimmer: high bass sparkles, deep bass glows heavy.
    light *= 1.0 + uSparkle * 0.35 * sin(uPhase * 2.5 + hash(aId + 7.0) * TAU);
    vLight = light;

    // ── Color: neon yellow with depth gradient ─────────────────────────
    vColor = mix(mix(vec3(1.0, 0.88, 0.0), vec3(1.0, 0.98, 0.42), depth * 0.6),
                 mix(vec3(0.45, 0.55, 1.0), vec3(0.35, 0.95, 1.0), depth * 0.6), uPalette);
}
"""
}

class LiveDotRenderer(source: BandSource, refreshRateHz: Float = 60f) :
    BaseVisualRenderer(source, refreshRateHz) {
    override val vert: String = LiveDotVisual.VERT
    override val rows: Int = 48
}
