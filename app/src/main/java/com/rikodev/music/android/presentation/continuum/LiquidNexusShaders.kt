package com.rikodev.music.android.presentation.continuum

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import java.util.concurrent.atomic.AtomicInteger

// ─────────────────────────────────────────────────────────────────────────────
// RIKO CONTINUUM + LIQUID NEXUS — AGSL LIQUID SHADERS (API 33+, ULTRA TIER)
//
// Strictly gated behind Build.VERSION_CODES.TIRAMISU and guarded by failure
// counting so repeated compilation/draw errors fall back to custom Bézier
// gradients without crashing or modifying the user's saved ULTRA preference.
// ─────────────────────────────────────────────────────────────────────────────

object LiquidNexusShaders {

    private val failureCounter = AtomicInteger(0)

    val shaderFailureCount: Int
        get() = failureCounter.get()

    fun recordShaderFailure() {
        failureCounter.incrementAndGet()
    }

    /**
     * AGSL liquid aurora shader for the CONTINUUM organic wave navbar and
     * Liquid Nexus chamber. Produces subtle flowing specular refraction around
     * the active focal coordinate without excessive neon noise.
     */
    const val LIQUID_CHAMBER_AGSL: String = """
        uniform float2 uResolution;
        uniform float uTime;
        uniform float uFocalX;
        uniform float uEnergy;
        uniform float4 uPrimary;
        uniform float4 uSecondary;
        uniform float4 uBase;

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / max(uResolution, float2(1.0, 1.0));
            float dx = uv.x - uFocalX;
            float focus = exp(-dx * dx * 6.5);

            float wave1 = sin(uv.x * 7.5 + uTime * 1.15) * 0.5 + 0.5;
            float wave2 = cos(uv.x * 4.2 - uTime * 0.85 + uv.y * 3.1) * 0.5 + 0.5;
            float flow = mix(wave1, wave2, 0.5);

            float4 accent = mix(uPrimary, uSecondary, clamp(flow * 0.7 + focus * 0.3, 0.0, 1.0));
            float edgeLift = smoothstep(0.0, 0.35, 1.0 - uv.y) * (0.28 + 0.25 * uEnergy);
            float crestGlow = focus * (0.34 + 0.22 * uEnergy) * smoothstep(1.0, 0.0, uv.y);

            float3 col = mix(uBase.rgb, accent.rgb, clamp(edgeLift + crestGlow * 0.85, 0.0, 0.92));
            float specular = pow(clamp(1.0 - abs(uv.y - 0.14) * 5.2, 0.0, 1.0), 2.5) * (0.22 + 0.32 * focus);
            col += float3(0.95, 0.92, 1.0) * specular * 0.45;

            float alpha = clamp(uBase.a + crestGlow * 0.20, 0.0, 0.96);
            return half4(col, alpha);
        }
    """

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun createChamberShaderOrNull(): RuntimeShader? {
        if (failureCounter.get() >= VisualQualityResolver.MAX_SHADER_FAILURES_BEFORE_FALLBACK) {
            return null
        }
        return runCatching {
            RuntimeShader(LIQUID_CHAMBER_AGSL)
        }.onFailure {
            recordShaderFailure()
        }.getOrNull()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun updateAndBuildBrush(
        shader: RuntimeShader,
        width: Float,
        height: Float,
        timeSec: Float,
        focalFractionX: Float,
        energy: Float,
        primary: Color,
        secondary: Color,
        base: Color
    ): ShaderBrush? = runCatching {
        shader.setFloatUniform("uResolution", width.coerceAtLeast(1f), height.coerceAtLeast(1f))
        shader.setFloatUniform("uTime", timeSec)
        shader.setFloatUniform("uFocalX", focalFractionX.coerceIn(0f, 1f))
        shader.setFloatUniform("uEnergy", energy.coerceIn(0f, 1f))
        shader.setFloatUniform("uPrimary", primary.red, primary.green, primary.blue, primary.alpha)
        shader.setFloatUniform("uSecondary", secondary.red, secondary.green, secondary.blue, secondary.alpha)
        shader.setFloatUniform("uBase", base.red, base.green, base.blue, base.alpha)
        ShaderBrush(shader)
    }.onFailure {
        recordShaderFailure()
    }.getOrNull()
}
