package com.rikodev.music.android.dna.render

/**
 * ESSL 1.00 / GLES 2.0 — LIVE·DOT SPECTRUM v2.
 *
 * Geometry: an edge-clamped concentric shell. All 48 rings rest collapsed at the
 * outer rim (a thick luminous ring, empty interior); on bass the rings fold inward
 * as one huge smooth 3D wave — never micro-jitter, never raw-FFT displacement.
 *
 * Passes (single VBO, three GL_POINTS draws):
 *   uPass 0 — background: starfield + neon corona ring + faint nebula (aId 0..575)
 *   uPass 1 — wave dot halo (soft, wide, dim)
 *   uPass 2 — wave dot core (sharp, bright)
 * Blending is additive (GL_ONE, GL_ONE) everywhere, so overlapping dots at the
 * rim merge into solid neon fire.
 */
internal object LiveDotShaders {
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
varying vec3 vColor;
varying float vLight;
varying float vMode;

const float TAU = 6.28318530718;
const float PI = 3.14159265359;
const float R = 0.72;
const float FIRST_WAVE = 576.0;
const float RINGS = 48.0;

float hash(float x) { return fract(sin(x * 12.9898 + uSeed * 39.71) * 43758.5453); }

void main() {
    vec2 scale = min(uViewport.x, uViewport.y) / uViewport;

    if (uPass < 0.5) {
        // ── Background layer ────────────────────────────────────────────
        float id = aId;
        if (id < 256.0) {
            // Stars: hash positions, slow drift, gentle twinkle.
            vMode = 0.0;
            float h1 = hash(id + 3.0);
            float h2 = hash(id + 61.0);
            float h3 = hash(id + 131.0);
            vec2 pos = vec2(h1 * 2.4 - 1.2, h2 * 2.4 - 1.2);
            pos += 0.012 * vec2(sin(uPhase * 0.07 + h3 * TAU), cos(uPhase * 0.05 + h1 * TAU));
            gl_Position = vec4(pos * scale, 0.0, 1.0);
            gl_PointSize = clamp(uPointSize * (0.22 + 0.55 * h3), 1.0, uMaxPointSize);
            float tw = 0.55 + 0.45 * sin(uPhase * (0.25 + h1) + h2 * TAU);
            vLight = (0.10 + 0.28 * h3) * tw;
            vColor = mix(vec3(0.62, 0.70, 1.0), vec3(1.0, 0.97, 0.85), step(0.8, h2));
        } else if (id < 384.0) {
            // Corona glow: 128 wide soft points riding the rim.
            vMode = 1.0;
            float k = (id - 256.0) / 128.0;
            float angle = TAU * k;
            float wob = 0.016 * sin(angle * 4.0 + uPhase * 0.8) * (0.25 + uBass)
                      + 0.012 * sin(angle * 7.0 - uPhase * 0.55) * uMid;
            float radius = R * (1.0 + 0.02 * uPunch - 0.015 * uBass) + wob;
            vec2 pos = radius * vec2(cos(angle), sin(angle));
            gl_Position = vec4(pos * scale, 0.0, 1.0);
            gl_PointSize = clamp(uPointSize * (5.2 + 3.6 * uBass + 2.8 * uPunch), 1.0, uMaxPointSize);
            vLight = 0.34 + 0.30 * uBass + 0.35 * uPunch;
            vColor = mix(vec3(1.0, 0.92, 0.02), vec3(0.35, 0.85, 1.0), uPalette);
        } else if (id < 448.0) {
            // Corona core: 64 bright sharp points riding the rim.
            vMode = 2.0;
            float k = (id - 384.0) / 64.0;
            float angle = TAU * k;
            float wob = 0.014 * sin(angle * 4.0 + uPhase * 0.8) * (0.25 + uBass)
                      + 0.010 * sin(angle * 7.0 - uPhase * 0.55) * uMid;
            float radius = R * (1.0 + 0.02 * uPunch - 0.015 * uBass) + wob;
            vec2 pos = radius * vec2(cos(angle), sin(angle));
            gl_Position = vec4(pos * scale, 0.0, 1.0);
            gl_PointSize = clamp(uPointSize * (1.35 + 0.55 * uBass + 0.5 * uPunch), 1.0, uMaxPointSize);
            vLight = 0.85 + 0.55 * uPunch;
            vColor = mix(vec3(1.0, 0.97, 0.35), vec3(0.75, 0.97, 1.0), uPalette);
        } else {
            // Nebula: 8 clusters x 16 faint slow clouds.
            vMode = 3.0;
            float id2 = id - 448.0;
            float cluster = floor(id2 / 16.0);
            float h1 = hash(cluster * 7.0 + 1.0);
            float h2 = hash(cluster * 7.0 + 29.0);
            vec2 center = vec2(h1 * 1.7 - 0.85, h2 * 1.7 - 0.85);
            float h3 = hash(id2 + 211.0);
            float h4 = hash(id2 + 907.0);
            vec2 pos = center + (vec2(h3, h4) - 0.5) * 0.38;
            gl_Position = vec4(pos * scale, 0.0, 1.0);
            gl_PointSize = clamp(uPointSize * (26.0 + 8.0 * h3), 1.0, uMaxPointSize);
            vLight = 0.016 + 0.012 * h4;
            vColor = mix(vec3(0.30, 0.22, 0.62), vec3(0.12, 0.30, 0.55), h1);
        }
        return;
    }

    // ── Wave dot passes: edge-clamped concentric shell ──────────────────
    float waveId = aId - FIRST_WAVE;
    float row = floor(waveId / uColumns);
    float v = row / (RINGS - 1.0);
    float column = mod(waveId, uColumns);
    float angle = TAU * (column + 0.45 * mod(row, 2.0)) / uColumns;

    // Edge clamp: rim rows (0 and 47) never leave the circle; mid rows fold most.
    float ringFactor = sin(v * PI);
    float body = clamp(uBass * uGain, 0.0, 1.0);
    float mids = clamp(uMid * uGain, 0.0, 1.0);

    // Smooth low-frequency 3D wave — three sine harmonics, zero raw-FFT jitter.
    float wave = 0.50 * sin(3.0 * angle + uPhase + 0.25 * row)
               + 0.30 * sin(5.0 * angle - 1.3 * uPhase + 0.15 * row)
               + 0.20 * sin(2.0 * angle + 0.7 * uPhase);

    // Rest state: every shell lives in a thick luminous rim, interior empty.
    float radius = R * (1.0 - 0.06 * ringFactor);
    // Beat pulls the shells inward in one big fold; mids deepen the wave.
    radius -= R * 0.45 * body * ringFactor;
    radius += 0.18 * R * wave * (0.4 + 0.6 * mids) * ringFactor * (0.15 + 0.85 * body);

    // 3D fold depth, slow pitch/roll, mild perspective — the wave crashes
    // from one edge to the other instead of pulsing flat.
    float z = R * body * sin(angle + uPhase) * ringFactor;
    float pitch = 0.32 * sin(uPhase * 0.11) + 0.10 * body;
    vec3 p = vec3(radius * cos(angle), radius * sin(angle), z);
    float cp = cos(pitch);
    float sp = sin(pitch);
    p = vec3(p.x, p.y * cp - p.z * sp, p.y * sp + p.z * cp);
    float cr = cos(uRoll);
    float sr = sin(uRoll);
    p = vec3(p.x * cr - p.y * sr, p.x * sr + p.y * cr, p.z);
    float persp = 1.0 / (1.0 - p.z * 0.22);
    vec2 pos = p.xy * persp;
    gl_Position = vec4(pos * scale, 0.0, 1.0);

    float depth = clamp(0.55 + 0.45 * (p.z * persp / (R * 0.6)), 0.3, 1.35);
    float size = uPointSize * (0.55 + 0.85 * depth) * (1.0 + 0.35 * body + 0.45 * uPunch);
    gl_PointSize = clamp(size * mix(2.35, 1.0, step(1.5, uPass)), 1.0, uMaxPointSize);

    // Edge ribbon glow: shells hugging the rim burn brighter and merge.
    float rim = smoothstep(0.72, 1.0, radius / R);
    float light = 0.30 + 1.15 * rim + 0.55 * body + 0.75 * uPunch;
    light *= 0.72 + 0.28 * hash(waveId + 17.0);
    vLight = light;
    vMode = 4.0;
    vColor = mix(mix(vec3(1.0, 0.88, 0.0), vec3(1.0, 0.98, 0.42), depth * 0.6),
                 mix(vec3(0.45, 0.55, 1.0), vec3(0.35, 0.95, 1.0), depth * 0.6), uPalette);
}
"""
    const val FRAG = """
precision mediump float;
uniform float uPass;
varying vec3 vColor;
varying float vLight;
varying float vMode;
void main() {
    vec2 p = gl_PointCoord * 2.0 - 1.0;
    float r2 = dot(p, p);
    if (r2 > 1.0) discard;
    float light;
    if (vMode < 0.5) {
        light = exp(-r2 * 3.0) + 0.10 * exp(-r2 * 1.2);
    } else if (vMode < 1.5) {
        light = exp(-r2 * 2.2) + 0.10 * exp(-r2 * 1.0);
    } else if (vMode < 2.5) {
        light = exp(-r2 * 6.0) + 0.20 * exp(-r2 * 2.0);
    } else if (vMode < 3.5) {
        light = exp(-r2 * 1.8);
    } else if (uPass < 1.5) {
        light = 0.22 * (exp(-r2 * 2.0) + 0.10 * exp(-r2 * 0.9));
    } else {
        light = exp(-r2 * 4.5) + 0.12 * exp(-r2 * 1.5);
    }
    gl_FragColor = vec4(vColor * vLight * light, 1.0);
}
"""
}
