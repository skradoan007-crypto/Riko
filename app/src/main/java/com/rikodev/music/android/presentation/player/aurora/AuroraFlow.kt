package com.rikodev.music.android.presentation.player.aurora

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import com.rikodev.music.android.core.designsystem.animation.rememberReduceMotion
import com.rikodev.music.android.core.experience.ColorState
import com.rikodev.music.android.core.experience.wave.WaveState
import com.rikodev.music.android.presentation.nowplaying.rememberWavePhase
import kotlin.math.sin
import kotlinx.coroutines.flow.StateFlow

/**
 * AuroraFlow (plan 4.2) — the RIKO One-UI-9-category wave layer:
 * fluid + colourful + premium + restrained + musical (plan 4.3).
 *
 * `Artwork = identity` (palette from ColorState) · `Music = motion`
 * (uniforms from [AuroraMotion] via WaveEnergy).
 *
 * Primary path: API 33+ → AGSL [android.graphics.RuntimeShader] — one
 * `drawRect`, shader + brush remembered, zero per-frame allocation, all
 * motion math in the shader (draw-phase only).
 * Fallback: API < 33 → cached-[Path] Canvas ribbons, same parameter contract.
 * Reduced motion → static palette gradient.
 *
 * Performance rules (plan 4.2): no shader construction per frame, no object
 * allocation per frame, no composition-driven 30/60Hz invalidation, adaptive
 * quality tier. GPU budget is judged from device frame evidence (Wave 0/10
 * numbers), never from an arbitrary milliseconds target.
 */

enum class AuroraQuality { FULL, LITE, STATIC }

@Composable
fun AuroraFlow(
    colorState: StateFlow<ColorState>,
    waveState: StateFlow<WaveState>,
    isPlaying: Boolean = true,
    isVisible: () -> Boolean = { true },
    modifier: Modifier = Modifier
) {
    val reduceMotion = rememberReduceMotion()
    val quality = when {
        reduceMotion -> AuroraQuality.STATIC
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> AuroraQuality.FULL
        else -> AuroraQuality.LITE
    }

    // Phase lives in a frame-clock loop whose value is read ONLY in draw
    // lambdas (plan R15) — speed follows musical momentum.
    // C5: gate the loop on sheet visibility so it stops when collapsed.
    val phase = rememberWavePhase(
        speedTarget = { 0.45f + 0.9f * waveState.value.energy.momentum },
        active = isVisible
    )

    when (quality) {
        AuroraQuality.STATIC -> AuroraFlowStatic(colorState = colorState, modifier = modifier)
        AuroraQuality.FULL -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AuroraFlowShader(
                colorState = colorState,
                waveState = waveState,
                isPlaying = isPlaying,
                phase = phase,
                modifier = modifier
            )
        } else {
            AuroraFlowCanvas(colorState = colorState, waveState = waveState, isPlaying = isPlaying, phase = phase, modifier = modifier)
        }
        AuroraQuality.LITE -> AuroraFlowCanvas(colorState = colorState, waveState = waveState, isPlaying = isPlaying, phase = phase, modifier = modifier)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PRIMARY: AGSL shader (API 33+)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Flowing aurora ribbons: three noise-warped sine bands (bass wide / mid /
 * treble sparkle) + a central beat glow over a deep violet base. The wave
 * WIDTH follows band pressure, the wave BRIGHTNESS follows musical energy,
 * attack punches come from transient/beat — never a clock-driven pulse.
 */
private val AURORA_AGSL = """
    uniform float2 uSize;
    uniform float uPhase;
    uniform float uBass;
    uniform float uMid;
    uniform float uTreble;
    uniform float uBeat;
    uniform float uTransient;
    uniform float uEnergy;
    uniform float uMotion;
    uniform float3 uColorA;
    uniform float3 uColorB;
    uniform float3 uColorGlow;

    float hash12(float2 p) {
        float3 p3 = fract(float3(p.xyx) * 0.1031);
        p3 += dot(p3, p3.yzx + 33.33);
        return fract((p3.x + p3.y) * p3.z);
    }

    float vnoise(float2 p) {
        float2 i = floor(p);
        float2 f = fract(p);
        float2 u = f * f * (3.0 - 2.0 * f);
        return mix(
            mix(hash12(i), hash12(i + float2(1.0, 0.0)), u.x),
            mix(hash12(i + float2(0.0, 1.0)), hash12(i + float2(1.0, 1.0)), u.x),
            u.y
        );
    }

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / uSize;
        float t = uPhase;
        float sway = 0.35 + 0.65 * uMotion;

        // Ribbon 0 — bass: wide, slow, pressure-driven width.
        float n0 = vnoise(uv * float2(1.6, 2.2) + float2(t * 0.05, 0.0));
        float y0 = 0.5 + 0.13 * sin(t * 0.22 + 1.7) * sway + (n0 - 0.5) * 0.22 * (0.4 + uBass);
        float w0 = 0.10 + 0.12 * uBass + 0.05 * uBeat;
        float m0 = exp(-pow(abs(uv.y - y0) / w0, 1.7));
        float3 acc = uColorA * m0 * (0.35 + 0.65 * uEnergy);

        // Ribbon 1 — mid texture.
        float n1 = vnoise(uv * float2(2.1, 2.8) + float2(-t * 0.04, 5.2));
        float y1 = 0.5 + 0.10 * sin(t * 0.17 + 3.4) * sway + (n1 - 0.5) * 0.18 * (0.4 + uMid);
        float w1 = 0.08 + 0.08 * uMid;
        float m1 = exp(-pow(abs(uv.y - y1) / w1, 1.7));
        acc += uColorB * m1 * (0.30 + 0.55 * uEnergy);

        // Ribbon 2 — treble sparkle + transient attack.
        float n2 = vnoise(uv * float2(3.0, 3.6) + float2(t * 0.09, 9.1));
        float y2 = 0.5 + 0.16 * sin(t * 0.28 + 5.9) * sway + (n2 - 0.5) * 0.14 * (0.4 + uTreble);
        float w2 = 0.05 + 0.05 * uTreble + 0.04 * uTransient;
        float m2 = exp(-pow(abs(uv.y - y2) / w2, 1.7));
        acc += uColorGlow * m2 * (0.25 + 0.55 * uTreble + 0.35 * uTransient);

        // Central glow — beat punches, energy sustains.
        float d = distance(uv, float2(0.5, 0.42));
        float glow = exp(-d * d * 6.0) * (0.12 + 0.50 * uBeat + 0.15 * uEnergy);
        acc += uColorGlow * glow;

        // Deep base + bottom aurora rise (RIKO violet identity).
        float3 base = float3(0.02, 0.01, 0.05);
        float3 col = base + acc;
        col = mix(col * 0.82, col, uv.y);
        col += uColorA * smoothstep(0.55, 1.0, uv.y) * 0.10;
        return half4(half3(col), 1.0);
    }
""".trimIndent()

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun AuroraFlowShader(
    colorState: StateFlow<ColorState>,
    waveState: StateFlow<WaveState>,
    isPlaying: Boolean,
    phase: State<Float>,
    modifier: Modifier
) {
    val shader = remember { android.graphics.RuntimeShader(AURORA_AGSL) }
    val brush = remember(shader) { ShaderBrush(shader) }
    val colors = colorState.collectAsStateWithLifecycle()
    val wave = waveState.collectAsStateWithLifecycle()

    Canvas(modifier = modifier) {
        // Draw-phase reads only — no composition invalidation (plan R15).
        val c = colors.value
        val m = wave.value.energy.toAuroraMotion()
        val motionGain = if (isPlaying) 1f else 0.15f

        shader.setFloatUniform("uSize", size.width, size.height)
        shader.setFloatUniform("uPhase", phase.value)
        shader.setFloatUniform("uBass", m.bass)
        shader.setFloatUniform("uMid", m.mid)
        shader.setFloatUniform("uTreble", m.treble)
        shader.setFloatUniform("uBeat", m.beat)
        shader.setFloatUniform("uTransient", m.transient)
        shader.setFloatUniform("uEnergy", m.energy)
        shader.setFloatUniform("uMotion", m.momentum * motionGain)
        shader.setFloatUniform("uColorA", c.primary.red, c.primary.green, c.primary.blue)
        shader.setFloatUniform("uColorB", c.secondary.red, c.secondary.green, c.secondary.blue)
        shader.setFloatUniform("uColorGlow", c.ambientGlow.red, c.ambientGlow.green, c.ambientGlow.blue)

        drawRect(brush = brush)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FALLBACK: cached-Path Canvas ribbons (API < 33) — same parameter contract
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AuroraFlowCanvas(
    colorState: StateFlow<ColorState>,
    waveState: StateFlow<WaveState>,
    isPlaying: Boolean,
    phase: State<Float>,
    modifier: Modifier
) {
    val colors = colorState.collectAsStateWithLifecycle()
    val wave = waveState.collectAsStateWithLifecycle()
    // Zero per-frame allocation (plan 4.2): paths live in remember.
    val pathA = remember { Path() }
    val pathB = remember { Path() }
    val pathGlow = remember { Path() }

    Canvas(modifier = modifier) {
        val c = colors.value
        val m = wave.value.energy.toAuroraMotion()
        val motionGain = if (isPlaying) 1f else 0.15f
        val t = phase.value
        val w = size.width
        val h = size.height

        drawRect(color = Color(0xFF05020E))

        // Three soft ribbons — same motion semantics as the shader path.
        fun ribbon(path: Path, yBase: Float, amp: Float, speed: Float, offset: Float, widthBoost: Float) {
            path.reset()
            val steps = 24
            val top = h * yBase
            val halfW = h * (0.05f + widthBoost)
            var started = false
            for (i in 0..steps) {
                val x = w * i / steps
                val warp = sin(t * speed + offset + i * 0.35f) * amp * (0.4f + 0.6f * m.momentum * motionGain)
                val y = top + warp
                if (!started) {
                    path.moveTo(x, y - halfW)
                    started = true
                } else {
                    path.lineTo(x, y - halfW)
                }
            }
            for (i in steps downTo 0) {
                val x = w * i / steps
                val warp = sin(t * speed + offset + i * 0.35f) * amp * (0.4f + 0.6f * m.momentum * motionGain)
                path.lineTo(x, top + warp + halfW)
            }
            path.close()
        }

        ribbon(pathA, yBase = 0.42f, amp = h * 0.10f, speed = 0.22f, offset = 1.7f, widthBoost = 0.06f + 0.10f * m.bass)
        drawPath(
            path = pathA,
            color = c.primary.copy(alpha = (0.16f + 0.30f * m.energy) * motionGain)
        )

        ribbon(pathB, yBase = 0.52f, amp = h * 0.08f, speed = -0.17f, offset = 3.4f, widthBoost = 0.04f + 0.08f * m.mid)
        drawPath(
            path = pathB,
            color = c.secondary.copy(alpha = (0.14f + 0.26f * m.energy) * motionGain)
        )

        ribbon(pathGlow, yBase = 0.47f, amp = h * 0.12f, speed = 0.28f, offset = 5.9f, widthBoost = 0.02f + 0.05f * m.treble)
        drawPath(
            path = pathGlow,
            color = c.ambientGlow.copy(alpha = (0.10f + 0.30f * m.treble + 0.25f * m.transient) * motionGain)
        )

        // Central beat glow — stacked translucent circles, no brush alloc.
        val beat = m.beat
        val glowR = w * (0.18f + 0.12f * beat)
        drawCircle(
            color = c.ambientGlow.copy(alpha = (0.10f + 0.22f * beat + 0.08f * m.energy) * motionGain),
            radius = glowR,
            center = Offset(w * 0.5f, h * 0.42f)
        )

        // Bottom aurora rise (RIKO violet identity) — plain rect, zero alloc.
        drawRect(
            color = c.primary.copy(alpha = 0.10f),
            topLeft = Offset(0f, h * 0.72f),
            size = androidx.compose.ui.geometry.Size(w, h * 0.28f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STATIC: reduced-motion — palette gradient, zero motion
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AuroraFlowStatic(
    colorState: StateFlow<ColorState>,
    modifier: Modifier
) {
    val colors = colorState.collectAsStateWithLifecycle()
    Canvas(modifier = modifier) {
        val c = colors.value
        drawRect(color = Color(0xFF05020E))
        drawRect(
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(
                    c.primary.copy(alpha = 0.18f),
                    Color.Transparent,
                    c.secondary.copy(alpha = 0.14f)
                )
            )
        )
    }
}
