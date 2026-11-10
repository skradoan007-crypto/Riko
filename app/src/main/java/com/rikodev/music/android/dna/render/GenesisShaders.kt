package com.rikodev.music.android.dna.render

/** Genesis P0: contour lattices on two deformed spheres, generated entirely on the GPU. */
internal object GenesisShaders {
    // 96 latitude contours; quality changes samples along each contour, not its topology.
    const val ROWS = 96
    const val VERT = """
#version 300 es
precision highp float;
precision highp int;
uniform mat4 uMvp;
uniform vec3 uCamDir;
uniform float uTime;
uniform float uBands[40];
uniform float uBass;
uniform float uMid;
uniform float uTreble;
uniform float uPulse;
uniform float uPointsPerLayer;
uniform float uPointScale;
uniform float uGain;
out vec3 vColor;
out float vPin;
const float PI = 3.14159265359;
const float TAU = 6.28318530718;
const float ROWS = 96.0;
float spectrum(float x) {
    float p = clamp(x, 0.0, 38.999);
    int i = int(p);
    return mix(uBands[i], uBands[i+1], fract(p));
}
void main() {
    float shell = floor(float(gl_VertexID) / uPointsPerLayer);
    float id = mod(float(gl_VertexID), uPointsPerLayer);
    float cols = uPointsPerLayer / ROWS;
    float row = floor(id / cols);
    float theta = (id + 0.5) / uPointsPerLayer * PI;
    float phi = mod(id, cols) / cols * TAU + shell * 0.026;
    vec3 n = vec3(sin(theta)*cos(phi), cos(theta), sin(theta)*sin(phi));
    float t = uTime;
    // Continuous low-order folds preserve a coherent silhouette; no random flashing.
    float flow = sin(phi*3.0 + theta*2.0 + t*0.52) * sin(theta);
    float fold = sin(theta*6.0 - phi*2.0 - t*0.68);
    float energy = spectrum((0.5 + 0.5*sin(phi + theta*1.6))*38.0);
    float detail = sin(phi*17.0 + theta*13.0 + t*1.3) * uTreble * 0.008;
    float radius = (0.83 - shell*0.068) * (1.0 + uBass*0.105 + uPulse*0.035)
        + flow*(0.022 + uMid*0.055) + fold*sin(theta)*(0.009 + energy*0.026) + detail*sin(theta);
    // Latitude displacement creates flowing contours rather than rigid wireframe parallels.
    float twist = 0.11*sin(theta*3.0 + t*0.32) + uMid*0.07*flow;
    vec3 p = vec3(n.x*cos(twist)-n.z*sin(twist), n.y, n.x*sin(twist)+n.z*cos(twist));
    p.y += sin(phi*2.0 + theta*4.0 - t*0.43) * sin(theta) * (0.012 + uMid*0.028);
    gl_Position = uMvp * vec4(p*radius, 1.0);
    float facing = dot(normalize(p), uCamDir);
    float rim = pow(1.0-abs(facing), 2.2);
    float front = mix(0.22, 1.0, smoothstep(-0.12, 0.25, facing));
    vec3 cyan = vec3(0.10, 0.83, 1.0);
    vec3 violet = vec3(0.45, 0.30, 1.0);
    vec3 pink = vec3(1.0, 0.24, 0.73);
    float gradient = clamp(0.50 + n.x*0.43 - n.y*0.24, 0.0, 1.0);
    vec3 col = mix(cyan, violet, smoothstep(0.12, 0.66, gradient));
    col = mix(col, pink, smoothstep(0.55, 0.95, gradient));
    // Sparse deterministic pin lights woven into the contour lattice.
    float pin = step(0.992, fract(sin(id*127.1 + shell*31.7)*43758.5453));
    float luminous = (0.20 + rim*0.65 + energy*0.20) * front;
    luminous *= mix(1.0, 0.38, shell);
    col = mix(col, vec3(0.82, 0.94, 1.0), pin*0.65);
    vPin = pin;
    vColor = col * (luminous + pin*(0.12+uTreble*0.28)*front) * uGain;
    gl_PointSize = clamp(uPointScale*(1.30 + pin*1.0 + rim*0.28) * 3.2/max(gl_Position.w,0.1), 1.0, 5.0);
}
"""
    const val FRAG = """
#version 300 es
precision mediump float;
in vec3 vColor;
in float vPin;
uniform float uLine;
out vec4 fragColor;
void main() {
    if (uLine > 0.5) {
        fragColor = vec4(vColor*0.72, 1.0);
        return;
    }
    vec2 p = gl_PointCoord*2.0-1.0;
    float r2 = dot(p,p);
    if (r2 > 1.0) discard;
    float core = exp(-r2*3.4);
    fragColor = vec4(vColor*core*(0.06+vPin*1.8), 1.0);
}
"""
}
