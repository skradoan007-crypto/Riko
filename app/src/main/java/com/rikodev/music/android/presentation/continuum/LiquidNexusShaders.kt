package com.rikodev.music.android.presentation.continuum

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import java.util.concurrent.atomic.AtomicInteger

// ─────────────────────────────────────────────────────────────────────────────
// RIKO CONTINUUM + LIQUID NEXUS — OPTICAL LIQUID GLASS AGSL SHADER (API 33+)
//
// Produces pure translucent optical glass with subtle liquid caustic shimmer,
// Fresnel top-edge catchlight, and focal aurora dispersion — never muddy.
// Strictly gated behind API 33+ with automatic failure counter fallback.
// ─────────────────────────────────────────────────────────────────────────────

object LiquidNexusShaders {

    private val failureCounter = AtomicInteger(0)

    val shaderFailureCount: Int
        get() = failureCounter.get()

    fun recordShaderFailure() {
        failureCounter.incrementAndGet()
    }

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
            float focus = exp(-dx * dx * 5.2);

            // Subtle optical liquid caustics inside crystal glass
            float c1 = sin(uv.x * 8.0 + uTime * 0.9 + uv.y * 2.4) * 0.5 + 0.5;
            float c2 = cos(uv.x * 5.2 - uTime * 0.7 - uv.y * 3.8) * 0.5 + 0.5;
            float caustic = pow(c1 * c2, 1.6);

            // Clean translucent crystal dark base + restrained luminous aurora
            float3 glassCore = mix(uBase.rgb, float3(0.08, 0.07, 0.16), 0.35);
            float3 aurora = mix(uPrimary.rgb, uSecondary.rgb, clamp(uv.x * 0.65 + c1 * 0.35, 0.0, 1.0));
            float auroraWeight = clamp(0.10 + focus * (0.26 + 0.14 * uEnergy) + caustic * 0.10, 0.0, 0.44);
            float3 col = mix(glassCore, aurora, auroraWeight);

            // Curved glass Fresnel top specular highlight + subtle bottom rim bounce
            float topSpecular = pow(clamp(1.0 - uv.y * 3.2, 0.0, 1.0), 2.2) * (0.28 + 0.24 * focus);
            float bottomBounce = pow(clamp((uv.y - 0.72) / 0.28, 0.0, 1.0), 2.0) * 0.10;
            col += float3(0.96, 0.97, 1.0) * (topSpecular + bottomBounce + caustic * 0.06);

            float alpha = clamp(0.66 + focus * 0.14 + topSpecular * 0.18, 0.62, 0.86);
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
