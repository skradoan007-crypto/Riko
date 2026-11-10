package com.rikodev.music.android.presentation.nowplaying

import android.content.Context
import android.graphics.RuntimeShader
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — PLASMA WAVEFORM CANVAS (master-prompt §5 + §6)
//
// One shared timeline component: white baseline + 4-5 filled plasma ridges
// above it + accent thumb with the ONLY glow. No blur on the waveform,
// per-element alpha inside the shader, phase integrated on CPU
// (never uTime * speed), position read in the DRAW phase.
// ─────────────────────────────────────────────────────────────────────────────

enum class WaveStyle { PLASMA, CLASSIC }

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun PlasmaWaveformCanvas(
    progress: () -> Float,
    motion: () -> com.rikodev.music.android.core.experience.wave.WaveEnergy,
    isPlaying: Boolean,
    isBuffering: Boolean = false,
    isScrubbing: Boolean = false,
    style: WaveStyle = WaveStyle.PLASMA,
    isTimelineVisible: () -> Boolean = { true },
    colorLeft: Color,
    colorMid: Color,
    colorRight: Color,
    modifier: Modifier = Modifier,
) {
    val shader = remember { RuntimeShader(PLASMA_AGSL) }
    val brush = remember(shader) { ShaderBrush(shader) }

    // System policies (§9): reduced motion halts decorative drift; battery
    // saver clamps speed + amplitude so the GPU is respected.
    val context = LocalContext.current
    val isReducedMotion = remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }
    val isBatterySaver = remember(context) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        pm?.isPowerSaveMode == true
    }

    val phase = rememberWavePhase(
        speedTarget = {
            when {
                isReducedMotion -> PlasmaTokens.ReducedMotionSpeed
                isBatterySaver -> PlasmaTokens.BatterySaverSpeed
                isBuffering -> PlasmaTokens.BufferSpeed
                isPlaying -> PlasmaTokens.PlaySpeed
                else -> PlasmaTokens.IdleSpeed
            }
        },
        // C5: skip frame loop while the timeline is hidden (sheet collapsed).
        active = { isTimelineVisible() }
    )

    // State machine (§9): playing 1.0 (grow 800ms) · paused 0.25 (550ms) ·
    // scrubbing 0.10 · buffering 0.15 · reduced motion 0.25 · battery saver ≤0.60.
    // Animated, never snapped.
    // Plan 4.4: isScrubbing is a PARAM — the canonical rikoSeek in
    // PlasmaTimeline owns all interaction (this renderer is visual-only).
    val targetAmp = when {
        isReducedMotion -> PlasmaTokens.ReducedMotionAmp
        isScrubbing -> PlasmaTokens.ScrubAmp
        isBuffering -> PlasmaTokens.BufferAmp
        isPlaying -> if (isBatterySaver) PlasmaTokens.BatterySaverMaxAmp else 1.0f
        else -> PlasmaTokens.IdleAmp
    }
    val amp by animateFloatAsState(
        targetValue = targetAmp,
        animationSpec = spring(stiffness = 200f, dampingRatio = 0.8f),
        label = "uAmp"
    )
    val scrubAnim by animateFloatAsState(
        targetValue = if (isScrubbing) 1f else 0f, label = "uScrub"
    )
    val plasmaTarget = when {
        style != WaveStyle.PLASMA -> 0f
        isBatterySaver -> PlasmaTokens.BatterySaverPlasma
        else -> 1f
    }
    val plasmaAnim by animateFloatAsState(targetValue = plasmaTarget, label = "uPlasma")

    // 48dp touch target centered on the baseline; the 64dp canvas lets the
    // thumb glow overflow the row without clipping the gesture area.
    // Plan 4.4: no gesture here — seek lives in PlasmaTimeline's rikoSeek
    // (the old seek-on-down fired accidental jumps).
    // Uniform colors + band scratch are allocated ONCE (colors are slow,
    // per song) — never per frame (plan 4.4 alloc rule).
    val bandsScratch = remember { FloatArray(6) }
    val colorLeftUniform = remember(colorLeft) {
        android.graphics.Color.valueOf(colorLeft.red, colorLeft.green, colorLeft.blue, colorLeft.alpha)
    }
    val colorMidUniform = remember(colorMid) {
        android.graphics.Color.valueOf(colorMid.red, colorMid.green, colorMid.blue, colorMid.alpha)
    }
    val accentUniform = remember(colorRight) {
        android.graphics.Color.valueOf(colorRight.red, colorRight.green, colorRight.blue, colorRight.alpha)
    }
    val c2Uniform = remember { android.graphics.Color.valueOf(0.97f, 0.25f, 0.91f, 1.0f) }
    val inkOnUniform = remember { android.graphics.Color.valueOf(1f, 1f, 1f, 1f) }
    val inkOffUniform = remember { android.graphics.Color.valueOf(1f, 1f, 1f, PlasmaTokens.InactiveAlpha) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .requiredHeight(PlasmaTokens.CanvasHeight)
            .frameRateHigh(isAnimating = isPlaying || isScrubbing)
            .drawWithCache {
                val barX = PlasmaTokens.BarInset.toPx()
                val barLen = size.width - 2 * barX
                val uPx = size.width / PlasmaTokens.RefCardWidthDp

                shader.setFloatUniform("uSize", size.width, size.height)
                shader.setFloatUniform("uBar", barX, barLen, size.height / 2f)
                shader.setFloatUniform("uPx", uPx)
                shader.setFloatUniform(
                    "uWindow",
                    PlasmaTokens.HumpStartU,
                    PlasmaTokens.HumpEndOfProgress,
                    PlasmaTokens.HumpPeakU
                )

                onDrawBehind {
                    // Plan 4.4: ZERO per-frame allocation — uniform colors and
                    // the band scratch live in remember; only primitives are
                    // written here. Motion comes from the Wave Engine's
                    // attack/release fields (never quantized color state).
                    val e = motion()
                    val scratch = bandsScratch
                    scratch[0] = e.bassPressure
                    scratch[1] = e.midTexture
                    scratch[2] = e.trebleSpark
                    scratch[3] = e.beatConfidence
                    scratch[4] = e.attack
                    scratch[5] = e.momentum
                    shader.setFloatUniform("uPhase", phase.value)
                    shader.setFloatUniform("uProgress", progress().coerceIn(0f, 1f))
                    shader.setFloatUniform("uAmp", amp)
                    shader.setFloatUniform("uPlasma", plasmaAnim)
                    shader.setFloatUniform("uScrub", scrubAnim)
                    shader.setFloatUniform("uAudio", e.overallEnergy.coerceIn(0f, 1f))
                    shader.setFloatUniform("uPulse", e.beatConfidence.coerceIn(0f, 1f))
                    shader.setFloatUniform("uBands", scratch)

                    shader.setColorUniform("uAccent", accentUniform)
                    shader.setColorUniform("uC0", colorLeftUniform)
                    shader.setColorUniform("uC1", colorMidUniform)
                    shader.setColorUniform("uC2", c2Uniform)
                    shader.setColorUniform("uInkOn", inkOnUniform)
                    shader.setColorUniform("uInkOff", inkOffUniform)

                    drawRect(brush)
                }
            }
    )
}

/**
 * 120Hz opt-in (master-prompt §6.3): Modifier.preferredFrameRate exists only
 * on newer Compose UI (1.9+). This BOM pins 1.7.x, so a direct reference
 * would not compile. No-op fallback keeps dt-based animation correct at
 * 60/90/120Hz; upgrade the BOM to light up the real hint.
 */
private fun Modifier.frameRateHigh(isAnimating: Boolean): Modifier = this

@Composable
fun LegacyGradientProgressBar(
    progress: Float,
    colorLeft: Color,
    colorMid: Color,
    colorRight: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(Color.White.copy(alpha = 0.2f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(4.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(colorLeft, colorMid, colorRight)
                    )
                )
        )
    }
}

const val PLASMA_AGSL = """
uniform float2 uSize;
uniform float3 uBar;
uniform float uPx;
uniform float uProgress;
uniform float uPhase;
uniform float uAmp;
uniform float uPlasma;
uniform float uScrub;
uniform float uAudio;
uniform float uPulse;
uniform float uBands[6];
uniform float3 uWindow;

layout(color) uniform half4 uAccent;
layout(color) uniform half4 uC0;
layout(color) uniform half4 uC1;
layout(color) uniform half4 uC2;
layout(color) uniform half4 uInkOn;
layout(color) uniform half4 uInkOff;

// Value-noise 2D (1-D ribbon evolving in time: sampled as (space, time)).
float hash(float2 p) {
    p = fract(p * float2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}
float snoise(float2 uv) {
    float2 i = floor(uv);
    float2 f = fract(uv);
    float a = hash(i);
    float b = hash(i + float2(1.0, 0.0));
    float c = hash(i + float2(0.0, 1.0));
    float d = hash(i + float2(1.0, 1.0));
    float2 u = f*f*(3.0-2.0*f);
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float bump(float x) {
    float t = clamp(1.0 - x*x, 0.0, 1.0);
    return t * t * (3.0 - 2.0 * t);
}

half4 main(float2 p) {
    float barX = uBar.x;
    float barLen = uBar.y;
    float baseY = uBar.z;

    float maxHump = 12.5 * uPx;
    float strokeRad = (3.25 * uPx) / 2.0;
    float glowRadius = 26.0 * uPx;

    // Early-out: outside baseline band, ridge band, and thumb-glow disc.
    float u = (p.x - barX) / barLen;
    float tx = barX + uProgress * barLen;
    float glowDist = length(p - float2(tx, baseY));
    if (u < -0.08 || u > 1.08) {
        if (glowDist > glowRadius * 2.2) { return half4(0.0); }
    } else {
        float above = baseY - p.y;
        if (above > maxHump + 3.0 * uPx || p.y - baseY > strokeRad + glowRadius * 2.2) {
            if (glowDist > glowRadius * 2.2) { return half4(0.0); }
        }
    }

    // Ridge window: trails the thumb (endU = 0.85 * progress).
    float s = uWindow.x;
    float endU = uWindow.y * uProgress;
    float win = smoothstep(s, s + 0.10, u) * (1.0 - smoothstep(endU - 0.14, endU, u));

    half4 outColor = half4(0.0);
    // Range-reduce phase inside the shader so sin stays accurate after hours.
    float phase = fract(uPhase * 0.15915494) * 6.28318;

    // Audio-reactive global boost: synthetic or FFT energy lifts all ridges.
    float audioBoost = 1.0 + 0.45 * uAudio;

    // 5 ridges, back -> front.
    for(int k = 0; k < 5; k++) {
        float centerK = 0.5;
        float w0K = 0.12;
        float gain = 1.0;
        half4 layerColor = uAccent;
        bool isStartBump = false;

        if(k==0) { centerK=0.42; w0K=0.16; gain=1.0; layerColor=uC0; }
        else if(k==1) { centerK=0.34; w0K=0.13; gain=0.85; layerColor=uC1; }
        else if(k==2) { centerK=0.50; w0K=0.11; gain=0.75; layerColor=uC2; }
        else if(k==3) { centerK=0.58; w0K=0.12; gain=1.0; layerColor=uAccent; }
        else { centerK=0.09; w0K=0.05; gain=0.35; layerColor=uAccent; isStartBump=true; }

        // Start bump is absolute (u ~= 0.09), others ride the window.
        float ck = mix(s, endU, centerK) + 0.03 * sin(0.31 * phase * (1.0 + 0.13 * float(k)) + float(k));
        if (isStartBump) { ck = 0.09; }
        float wk = w0K * (1.0 + 0.18 * sin(0.47 * phase + float(k)*1.2));
        float nk = 0.82 + 0.18 * snoise(float2(u*5.0 + 11.3*float(k), phase*0.25));

        float uPrime = u + 0.035 * sin(6.283*1.7*u + float(k)) * cos(phase*0.2);

        float bandVal = uBands[k];
        float hk = maxHump * uAmp * uPlasma * gain * audioBoost * (1.0 + 0.55*bandVal + 0.35*uPulse) * bump((uPrime - ck)/wk) * win * nk * (1.0 - 0.9*uScrub);

        float above = baseY - p.y;
        float cov = smoothstep(-0.75, 0.75, hk - above);
        float hot = pow(1.0 - clamp(above/max(hk, 1.0), 0.0, 1.0), 2.0);

        half4 cBlend = mix(layerColor, half4(1.0), 0.70 * hot);
        float alpha = 0.90 * cov;

        // Premultiplied source-over (un-premultiplied output is forbidden).
        outColor = cBlend * alpha + outColor * (1.0 - alpha);
    }

    // Baseline: straight in plasma mode; AOSP-style squiggle as uPlasma -> 0.
    float slopeCorr = 1.0;
    float y_c = baseY;
    if (uPlasma < 0.999) {
        float classicAmp = (1.0 - uPlasma) * 1.6 * uPx * uAmp;
        float x = p.x;
        float wave = sin(6.283 * x / (20.0 * uPx) - phase);
        float taper = smoothstep(barX, barX + 0.1 * barLen, x) * (1.0 - smoothstep(tx - 0.1 * barLen, tx, x));
        y_c = baseY + classicAmp * taper * wave;
        float slope = classicAmp * taper * 6.283 / (20.0 * uPx) * cos(6.283 * x / (20.0 * uPx) - phase);
        slopeCorr = sqrt(1.0 + slope * slope);
    }
    float distBase = abs(p.y - y_c) / slopeCorr;
    float covBase = smoothstep(strokeRad + 0.75, strokeRad - 0.75, distBase);
    // 2dp gap after the thumb ring before the inactive track resumes.
    float inactiveStart = tx + 2.0 * uPx;
    float activeMask = step(p.x, tx);
    float inactiveMask = step(inactiveStart, p.x);
    // Active line blends toward the accent as uPlasma -> 0 (classic mode).
    half4 activeColor = mix(uAccent, uInkOn, uPlasma);
    half4 trackColor = activeColor * activeMask + uInkOff * inactiveMask;
    float trackCov = covBase * (activeMask + inactiveMask);
    outColor = trackColor * trackCov + outColor * (1.0 - trackCov);

    // Thumb: accent fill + white ring; glow behind (the ONLY glow).
    float thumbOuter = (16.5 * uPx) / 2.0;
    float thumbFill = thumbOuter - (2.5 * uPx);
    float2 td = p - float2(tx, baseY);
    float dThumb = length(td);

    float ringRadius = thumbOuter * (1.0 + 0.25 * uScrub);
    float fillRadius = thumbFill * (1.0 + 0.25 * uScrub);

    float sigma = 0.36 * glowRadius;
    float glowAlpha = 0.55 * exp(-(dThumb*dThumb)/(sigma*sigma)) * (1.0 + 0.4 * uScrub);
    half4 glowColor = uAccent * glowAlpha;
    outColor = glowColor + outColor;

    float covRing = smoothstep(ringRadius + 0.75, ringRadius - 0.75, dThumb);
    float covFill = smoothstep(fillRadius + 0.75, fillRadius - 0.75, dThumb);

    outColor = uInkOn * covRing + outColor * (1.0 - covRing);
    outColor = uAccent * covFill + outColor * (1.0 - covFill);

    return outColor;
}
"""
