package com.rikodev.music.android.dna.render

/** ESSL 1.00 / GLES 2.0. Independent 2D points: no time, camera, mesh or texture input. */
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
uniform float uBands[40];
uniform float uTrail[40];
varying vec3 vColor;
varying float vLight;
const float TAU = 6.28318530718;
float hash(float x) { return fract(sin(x * 12.9898 + uSeed * 39.71) * 43758.5453); }
void main() {
    float row = floor(aId / uColumns);
    float v = row / 47.0;
    float column = mod(aId, uColumns);
    float angle = TAU * (column + 0.45 * mod(row, 2.0)) / uColumns;
    // Mirror the log spectrum around the circle. Adjacent dots share a band,
    // but each row has its own lag, radius and small fixed phase offset.
    float frequency = abs(2.0 * column / uColumns - 1.0) * 39.0;
    int lo = int(floor(frequency));
    int hi = int(min(float(lo) + 1.0, 39.0));
    float fast = mix(uBands[lo], uBands[hi], fract(frequency));
    float trail = mix(uTrail[lo], uTrail[hi], fract(frequency));
    float energy = clamp(mix(trail, fast, 0.35 + 0.65 * v) * uGain, 0.0, 1.0);
    float transient = clamp(abs(fast - trail) * uGain, 0.0, 1.0);
    float jitter = hash(aId + 17.0) - 0.5;
    float phase = angle * (3.0 + floor(uSeed * 4.0)) + row * 0.19;
    // Audio moves each frequency sector; static sinusoids only shape the
    // displacement. With a constant spectrum all vertices remain stationary.
    float body = clamp(uBass * uGain, 0.0, 1.0);
    float radius = 0.06 + 0.68 * sin(v * 1.5707963);
    radius += 0.07 * body;
    radius += energy * (0.025 + 0.115 * sin(phase)) * (0.3 + 0.7 * v);
    radius += transient * 0.025 * jitter;
    angle += energy * 0.16 * sin(phase + v * 2.0) * (1.0 - v);
    angle += jitter * (0.10 * (1.0 - v) + 0.0025) * (0.2 + energy);
    vec2 position = radius * vec2(cos(angle), sin(angle));
    gl_Position = vec4(position * min(uViewport.x, uViewport.y) / uViewport, 0.0, 1.0);
    gl_PointSize = clamp(uPointSize * (0.75 + 0.30 * v + 0.65 * energy + 0.6 * transient), 1.0, uMaxPointSize);
    vec3 warm = mix(vec3(0.76, 0.79, 0.14), vec3(1.0, 0.98, 0.27), v);
    vec3 cool = mix(vec3(0.63, 0.22, 1.0), vec3(0.16, 0.90, 1.0), 0.5 + 0.5 * sin(angle));
    vColor = mix(warm, cool, uPalette);
    float rim = pow(v, 9.0);
    vLight = (0.28 + 1.1 * rim + 2.0 * energy + 1.2 * transient + 0.65 * body) * (0.65 + 0.35 * hash(aId));
    // Sparse inner points, dense luminous edge; fixed mask never flickers.
    vLight *= step(0.78 * (1.0 - v), hash(aId + 251.0));
}
"""
    const val FRAG = """
precision mediump float;
varying vec3 vColor;
varying float vLight;
void main() {
    vec2 p = gl_PointCoord * 2.0 - 1.0;
    float r2 = dot(p, p);
    if (r2 > 1.0) discard;
    float light = exp(-r2 * 3.5) + 0.16 * exp(-r2 * 1.5);
    gl_FragColor = vec4(vColor * vLight * light, 1.0);
}
"""
}
