package com.rikodev.music.android.dna.render

// ─────────────────────────────────────────────────────────────────────────────
// RIKO DNA (MDNA) — GLSL ES 3.00 SOURCES
//
// Each source was validated offline with glslangValidator (ESSL 3.00) — and each
// vertex/fragment pair LINKED with `-l` — before being pasted here; THIS FILE is
// the source of truth. Re-validate after edits. Sources start with a newline:
// callers must trim() before glShaderSource.
//
// Every scene shader draws WITHOUT vertex buffers: a point's position, layer and
// colour come from gl_VertexID + the common uniforms, so the point count is only
// a draw-call argument (quality tiers cost zero memory).
//
// COMMON UNIFORMS (a template declares only what it uses; the engine sets all of
// them by name and an unused/optimised-out one has location -1, which GL ignores):
//   uMvp uCamDir uTime uBands[40] uBass uMid uTreble uPulse uPointsPerLayer
//   uPointScale uGain uSeed uView(half-extent of the z=0 plane)
//   uHist uHistHead uHistFrac   (rolling spectrum history, texture unit 2)
// ─────────────────────────────────────────────────────────────────────────────
internal object MdnaShaders {

    const val SCENE_VERT: String = """
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

const float PI = 3.141592653589793;
const float TAU = 6.283185307179586;
const float GOLDEN_FRACT = 0.6180339887498949;
const float SHELL_R[4] = float[4](0.30, 0.47, 0.64, 0.82);

float bandAt(float pos) {
    float p = clamp(pos, 0.0, 38.999);
    int i0 = int(floor(p));
    float f = p - float(i0);
    return mix(uBands[i0], uBands[i0 + 1], f);
}

void main() {
    float vid = float(gl_VertexID);
    float shellF = floor(vid / uPointsPerLayer);
    float idx = vid - shellF * uPointsPerLayer + 0.5;
    int shell = int(shellF);
    float n = uPointsPerLayer;

    // Fibonacci lattice on the unit sphere, generated from the vertex id alone.
    float y = 1.0 - 2.0 * idx / n;
    float rr = sqrt(max(0.0, 1.0 - y * y));
    float phi = fract(idx * GOLDEN_FRACT + shellF * 0.137) * TAU;
    vec3 dir = vec3(cos(phi) * rr, y, sin(phi) * rr);

    // Left/right mirrored: bass on one axis, treble on the opposite one.
    float az = abs(atan(dir.z, dir.x)) / PI;
    float b = bandAt(az * 38.0);

    float e;
    if (shell == 0) {
        e = uBass;
    } else if (shell == 1) {
        e = 0.5 * (uBass + uMid);
    } else if (shell == 2) {
        e = uMid;
    } else {
        e = uTreble;
    }

    float sf = float(shell);
    float t = uTime;
    float wob = sin(dir.x * 3.1 + t * 0.8) * sin(dir.y * 2.6 - t * 0.6)
              + 0.6 * sin(dir.z * 3.9 + dir.x * 1.7 + t * 1.0);

    float radius = SHELL_R[shell] * (1.0
        + wob * (0.03 + 0.09 * e)
        + 0.18 * b
        + uPulse * 0.07 * (1.0 - 0.18 * sf));

    gl_Position = uMvp * vec4(dir * radius, 1.0);

    float facing = dot(dir, uCamDir);
    float rim = pow(1.0 - abs(facing), 1.6);
    float sideDim = facing > 0.0 ? 1.0 : 0.5;
    float bright = (0.22 + 0.95 * rim + 0.55 * b + 0.35 * uPulse) * sideDim;

    vec3 magenta = vec3(0.96, 0.25, 0.71);
    vec3 violet = vec3(0.61, 0.36, 1.0);
    vec3 cyan = vec3(0.15, 0.75, 1.0);
    vec3 col = mix(mix(magenta, violet, smoothstep(0.0, 1.2, sf)), cyan, smoothstep(1.4, 3.0, sf));
    float whiten = b * 0.55 + (shell == 0 ? 0.35 * uPulse : 0.0);
    col = mix(col, vec3(1.0), clamp(whiten, 0.0, 0.8));

    vColor = col * bright * 0.55 * uGain;

    float size = uPointScale * (1.4 + 1.8 * b + 1.1 * uPulse * (1.0 - 0.2 * sf)) * (2.8 / max(gl_Position.w, 0.1));
    gl_PointSize = clamp(size, 1.0, 28.0);
}
"""

    const val SCENE_FRAG: String = """
#version 300 es
precision mediump float;

in vec3 vColor;
out vec4 fragColor;

void main() {
    vec2 d = gl_PointCoord - vec2(0.5);
    float r2 = dot(d, d) * 4.0;
    float a = clamp(1.0 - r2, 0.0, 1.0);
    a = a * a;
    fragColor = vec4(vColor * a, 1.0);
}
"""

    const val HALO_VERT: String = """
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
uniform float uSeed;

out vec3 vColor;
out float vSoft;

const float PI = 3.141592653589793;
const float TAU = 6.283185307179586;
const float GOLDEN_FRACT = 0.6180339887498949;
const float RADIUS = 0.88;

float bandAt(float pos) {
    float p = clamp(pos, 0.0, 38.999);
    int i0 = int(floor(p));
    float f = p - float(i0);
    return mix(uBands[i0], uBands[i0 + 1], f);
}

void main() {
    float n = uPointsPerLayer;
    float idx = float(gl_VertexID) + 0.5;

    // Fibonacci lattice with its poles on the X axis: seen face-on the rows
    // converge toward left/right, like a warped grid.
    float y = 1.0 - 2.0 * idx / n;
    float rr = sqrt(max(0.0, 1.0 - y * y));
    float phi = fract(idx * GOLDEN_FRACT) * TAU;
    vec3 dir = vec3(y, cos(phi) * rr, sin(phi) * rr);

    float facing = dot(dir, uCamDir);
    float rim = pow(1.0 - abs(facing), 1.5);

    // Rim deformation follows the spectrum around the circle (mirrored).
    float theta = atan(dir.y, dir.x);
    float b = bandAt(abs(theta) / PI * 38.0);
    float wob = sin(theta * 3.0 + uTime * 0.9 + uSeed) * 0.6 + sin(theta * 5.0 - uTime * 1.3) * 0.4;

    float radius = RADIUS * (1.0 + rim * (0.22 * b + 0.045 * wob * (0.4 + uMid)) + uPulse * 0.05);
    gl_Position = uMvp * vec4(dir * radius, 1.0);

    vec3 cyan = vec3(0.20, 0.85, 1.0);
    vec3 magenta = vec3(0.98, 0.28, 0.85);
    vec3 inner = vec3(0.40, 0.32, 1.0);
    vec3 edge = mix(magenta, cyan, 0.5 + 0.5 * sin(theta));
    vec3 col = mix(inner, edge, smoothstep(0.05, 0.6, rim));
    col = mix(col, vec3(1.0), clamp(rim * 0.45 + b * rim * 0.5, 0.0, 0.9));

    float sideDim = facing > 0.0 ? 1.0 : 0.35;
    float bright = (0.10 + 1.10 * rim + 0.55 * b * rim + 0.30 * uPulse * rim) * sideDim;
    vColor = col * bright * 0.5 * uGain;
    vSoft = 0.0;

    float size = uPointScale * (1.0 + 1.6 * rim + 1.4 * b * rim) * (2.8 / max(gl_Position.w, 0.1));
    gl_PointSize = clamp(size, 1.0, 26.0);
}
"""

    const val TERRAIN_VERT: String = """
#version 300 es
precision highp float;
precision highp int;

uniform mat4 uMvp;
uniform float uTime;
uniform float uBass;
uniform float uPulse;
uniform float uPointsPerLayer;
uniform float uPointScale;
uniform float uGain;
uniform vec2 uView;
uniform sampler2D uHist;
uniform float uHistHead;
uniform float uHistFrac;

out vec3 vColor;
out float vSoft;

const float ROWS = 56.0;
const float HIST = 64.0;
const float BANDS = 40.0;

// History texel: x = band, y = ring row. Row 0 of "age" is the newest.
float histAt(float band, float age) {
    float row = mod(uHistHead - age + HIST * 4.0, HIST);
    return texelFetch(uHist, ivec2(int(band), int(row)), 0).r;
}

float levelAt(float bandPos, float age) {
    float bp = clamp(bandPos, 0.0, BANDS - 1.001);
    float b0 = floor(bp);
    float f = bp - b0;
    float a0 = floor(age);
    float fa = age - a0;
    float v00 = histAt(b0, a0);
    float v10 = histAt(b0 + 1.0, a0);
    float v01 = histAt(b0, a0 + 1.0);
    float v11 = histAt(b0 + 1.0, a0 + 1.0);
    return mix(mix(v00, v10, f), mix(v01, v11, f), fa);
}

void main() {
    float cols = uPointsPerLayer / ROWS;
    float id = float(gl_VertexID);
    float row = floor(id / cols);
    float colIdx = id - row * cols;
    float cx = (colIdx + 0.5) / cols;

    // Rows scroll away from the viewer continuously between history pushes.
    float age = row + uHistFrac;
    float t = age / ROWS;

    // Mirrored: bass in the middle, treble at the edges.
    float u = abs(cx * 2.0 - 1.0);
    float lvl = levelAt(u * (BANDS - 1.0), age);

    float amp = lvl * (0.50 + 0.22 * uBass + 0.12 * uPulse) * mix(1.0, 0.6, t);
    float x = (cx * 2.0 - 1.0) * uView.x * 1.04;
    float yy = -uView.y * 0.36 + amp * uView.y * 1.25;
    float z = -t * 3.0;
    gl_Position = uMvp * vec4(x, yy, z, 1.0);

    // Depth of field: a focus band around t = 0.30, soft and dim elsewhere.
    float soft = clamp(abs(t - 0.30) * 2.2, 0.0, 1.0);
    vSoft = soft;

    vec3 low = vec3(0.15, 0.35, 1.0);
    vec3 mid = vec3(0.62, 0.42, 1.0);
    vec3 top = vec3(0.85, 0.95, 1.0);
    vec3 col = mix(low, mid, smoothstep(0.0, 0.55, lvl));
    col = mix(col, top, smoothstep(0.55, 1.0, lvl));

    float fade = (1.0 - 0.55 * t) * mix(1.0, 0.45, soft);
    vColor = col * (0.35 + 1.1 * lvl) * fade * 0.55 * uGain;

    float size = uPointScale * (1.5 + 2.8 * soft + 1.0 * lvl) * (2.8 / max(gl_Position.w, 0.1));
    gl_PointSize = clamp(size, 1.0, 30.0);
}
"""

    const val LENS_VERT: String = """
#version 300 es
precision highp float;
precision highp int;

uniform mat4 uMvp;
uniform float uTime;
uniform float uBands[40];
uniform float uBass;
uniform float uMid;
uniform float uTreble;
uniform float uPulse;
uniform float uPointsPerLayer;
uniform float uPointScale;
uniform float uGain;
uniform float uSeed;
uniform vec2 uView;

out vec3 vColor;
out float vSoft;

const float STRANDS = 28.0;

float hash(float n) {
    return fract(sin(n * 127.1 + 311.7) * 43758.5453);
}

float bandAt(float pos) {
    float p = clamp(pos, 0.0, 38.999);
    int i0 = int(floor(p));
    float f = p - float(i0);
    return mix(uBands[i0], uBands[i0 + 1], f);
}

void main() {
    float n = uPointsPerLayer;
    float id = float(gl_VertexID);

    // ~5% of the points are drifting bokeh instead of strand points.
    if (hash(id) < 0.05) {
        float hx = hash(id + 1.0) * 2.0 - 1.0;
        float hy = hash(id + 2.0) * 2.0 - 1.0;
        float hz = hash(id + 3.0) - 0.5;
        float drift = sin(uTime * 0.35 + hash(id + 4.0) * 6.2831) * 0.05;
        vec3 p = vec3(hx * uView.x * 1.05, hy * uView.y * 0.6 + drift, hz * 1.4);
        gl_Position = uMvp * vec4(p, 1.0);
        vSoft = 1.0;
        vec3 warm = mix(vec3(1.0, 0.55, 0.75), vec3(0.55, 0.65, 1.0), hash(id + 5.0));
        vColor = warm * (0.20 + 0.55 * uTreble + 0.25 * uPulse) * 0.6 * uGain;
        float s = uPointScale * (3.0 + 4.0 * hash(id + 6.0)) * (2.8 / max(gl_Position.w, 0.1));
        gl_PointSize = clamp(s, 2.0, 40.0);
        return;
    }

    // Run along the screen's LONG axis: horizontal on landscape, vertical on a portrait phone.
    bool tall = uView.y > uView.x;
    float along = tall ? uView.y : uView.x;
    float across = tall ? uView.x : uView.y;

    float s = mod(id, STRANDS);
    float k = floor(id / STRANDS);
    float perStrand = n / STRANDS;
    float u = (k + 0.5) / perStrand;
    float x = u * 2.0 - 1.0;
    float ax = abs(x);
    float sNorm = s / (STRANDS - 1.0) * 2.0 - 1.0;

    float b = bandAt(ax * 30.0);
    float energy = 0.35 * uBass + 0.40 * uMid + 0.25 * uTreble;

    // Waist at the centre: all strands share one phase at x = 0 and fan out from there.
    float env = 0.10 + 0.90 * smoothstep(0.0, 0.85, ax);
    float amp = across * (0.12 + 0.36 * energy + 0.18 * uPulse + 0.12 * b) * env;
    float phase = x * (7.0 + 5.0 * uTreble) + uTime * (1.4 + 2.0 * uMid) + sNorm * x * 4.5 + uSeed;
    float y = amp * sin(phase) + sNorm * across * 0.07 * ax;

    vec3 p = tall ? vec3(y, x * along * 1.05, sNorm * 0.18)
                    : vec3(x * along * 1.05, y, sNorm * 0.18);
    gl_Position = uMvp * vec4(p, 1.0);
    vSoft = 0.0;

    vec3 cyan = vec3(0.30, 0.80, 1.0);
    vec3 violet = vec3(0.50, 0.40, 1.0);
    vec3 pink = vec3(0.90, 0.45, 1.0);
    vec3 col = mix(mix(cyan, violet, smoothstep(0.0, 0.5, u)), pink, smoothstep(0.5, 1.0, u));

    float glow = 1.0 + 1.6 * exp(-ax * ax * 18.0) * (0.5 + uPulse + b);
    col = mix(col, vec3(1.0), clamp((glow - 1.0) * 0.25, 0.0, 0.7));
    vColor = col * (0.30 + 0.55 * energy + 0.35 * b) * glow * 0.5 * uGain;

    float size = uPointScale * (1.1 + 0.8 * glow) * (2.8 / max(gl_Position.w, 0.1));
    gl_PointSize = clamp(size, 1.0, 24.0);
}
"""

    const val SPRITE_SOFT_FRAG: String = """
#version 300 es
precision mediump float;

in vec3 vColor;
in float vSoft;
out vec4 fragColor;

void main() {
    vec2 d = gl_PointCoord - vec2(0.5);
    float r2 = dot(d, d) * 4.0;
    float a = clamp(1.0 - r2, 0.0, 1.0);
    // Sharp dots in focus, wide soft discs out of focus (depth of field / bokeh).
    a = pow(a, mix(2.0, 1.0, vSoft));
    fragColor = vec4(vColor * a, 1.0);
}
"""

    const val POST_VERT: String = """
#version 300 es
precision highp float;

out vec2 vUv;

void main() {
    vec2 p = vec2(float((gl_VertexID << 1) & 2), float(gl_VertexID & 2));
    vUv = p;
    gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);
}
"""

    const val BRIGHT_FRAG: String = """
#version 300 es
precision mediump float;

uniform sampler2D uTex;
uniform vec2 uTexel;
uniform float uThreshold;

in vec2 vUv;
out vec4 fragColor;

void main() {
    vec3 c = texture(uTex, vUv + uTexel * vec2(-1.0, -1.0)).rgb
           + texture(uTex, vUv + uTexel * vec2(1.0, -1.0)).rgb
           + texture(uTex, vUv + uTexel * vec2(-1.0, 1.0)).rgb
           + texture(uTex, vUv + uTexel * vec2(1.0, 1.0)).rgb;
    c *= 0.25;
    float l = max(c.r, max(c.g, c.b));
    float k = smoothstep(uThreshold, uThreshold + 0.25, l);
    fragColor = vec4(c * k, 1.0);
}
"""

    const val BLUR_FRAG: String = """
#version 300 es
precision mediump float;

uniform sampler2D uTex;
uniform vec2 uDir;

in vec2 vUv;
out vec4 fragColor;

void main() {
    vec3 c = texture(uTex, vUv).rgb * 0.2270270270;
    c += (texture(uTex, vUv + uDir * 1.3846153846).rgb
        + texture(uTex, vUv - uDir * 1.3846153846).rgb) * 0.3162162162;
    c += (texture(uTex, vUv + uDir * 3.2307692308).rgb
        + texture(uTex, vUv - uDir * 3.2307692308).rgb) * 0.0702702703;
    fragColor = vec4(c, 1.0);
}
"""

    const val COMPOSITE_FRAG: String = """
#version 300 es
precision mediump float;

uniform sampler2D uScene;
uniform sampler2D uBloom;
uniform float uBloomIntensity;

in vec2 vUv;
out vec4 fragColor;

void main() {
    vec3 s = texture(uScene, vUv).rgb;
    vec3 b = texture(uBloom, vUv).rgb;
    vec3 c = s + b * uBloomIntensity;
    vec2 q = vUv - vec2(0.5);
    c *= 1.0 - 0.35 * dot(q, q);
    fragColor = vec4(c, 1.0);
}
"""
}
