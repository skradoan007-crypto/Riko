package com.rikodev.vyrox.graphics.gl

import android.content.Context
import android.opengl.GLES20
import androidx.annotation.OptIn
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram
import com.rikodev.vyrox.core.model.ColorGradeState
import java.util.concurrent.atomic.AtomicReference

/**
 * Universal OpenGL ES 2.0 / 3.0 compatible color grading shader program.
 * Uses standard GLSL ES 1.00 syntax (attribute/varying/texture2D/gl_FragColor)
 * so it compiles reliably inside Media3's EGL context on ALL Android GPUs
 * (Mali, Adreno, PowerVR, MediaTek) for BOTH live preview and Transformer export.
 */
@OptIn(UnstableApi::class)
class VyRoxColorGlEffect(
    private val stateRef: AtomicReference<ColorGradeState>,
    private val allowNoOp: Boolean = false
) : GlEffect {

    constructor(fixedState: ColorGradeState) : this(AtomicReference(fixedState), allowNoOp = true)

    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram {
        return VyRoxColorShaderProgram(stateRef, useHdr)
    }

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean {
        if (!allowNoOp) return false
        val g = stateRef.get()
        return !g.enabled || g.isNeutral()
    }
}

@OptIn(UnstableApi::class)
private class VyRoxColorShaderProgram(
    private val stateRef: AtomicReference<ColorGradeState>,
    useHdr: Boolean
) : BaseGlShaderProgram(useHdr, /* texturePoolCapacity = */ 1) {

    private val glProgram: GlProgram
    private var texWidth = 1
    private var texHeight = 1

    init {
        try {
            glProgram = GlProgram(VERTEX_SHADER_UNIVERSAL, FRAGMENT_SHADER_UNIVERSAL)
            glProgram.setBufferAttribute(
                "aFramePosition",
                GlUtil.getNormalizedCoordinateBounds(),
                GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE
            )
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException(e)
        }
    }

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        texWidth = inputWidth.coerceAtLeast(1)
        texHeight = inputHeight.coerceAtLeast(1)
        return Size(texWidth, texHeight)
    }

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        try {
            val grade = stateRef.get()
            glProgram.use()
            glProgram.setSamplerTexIdUniform("uTexSampler", inputTexId, 0)
            glProgram.setFloatsUniform(
                "uTexelSize",
                floatArrayOf(1f / texWidth.toFloat(), 1f / texHeight.toFloat())
            )
            glProgram.setFloatUniform("uEnabled", if (grade.enabled) 1f else 0f)
            glProgram.setFloatUniform("uExposure", grade.exposure)
            glProgram.setFloatUniform("uBrightness", grade.brightness)
            glProgram.setFloatUniform("uContrast", grade.contrast)
            glProgram.setFloatUniform("uHighlights", grade.highlights)
            glProgram.setFloatUniform("uShadows", grade.shadows)
            glProgram.setFloatUniform("uWarmth", grade.warmth)
            glProgram.setFloatUniform("uTint", grade.tint)
            glProgram.setFloatUniform("uSaturation", grade.saturation)
            glProgram.setFloatUniform("uVibrance", grade.vibrance)
            glProgram.setFloatUniform("uFade", grade.fade)
            glProgram.setFloatUniform("uSharpen", grade.sharpen)
            glProgram.setFloatUniform("uVignette", grade.vignette)
            glProgram.setFloatUniform("uGrain", grade.grain)

            val timeSeed = ((presentationTimeUs / 33_333L) % 1024L).toFloat() * 0.0137f
            glProgram.setFloatUniform("uTimeSeed", timeSeed)

            glProgram.bindAttributesAndUniforms()
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            GlUtil.checkGlError()
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException(e, presentationTimeUs)
        }
    }

    override fun release() {
        super.release()
        try {
            glProgram.delete()
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException(e)
        }
    }

    companion object {
        private const val VERTEX_SHADER_UNIVERSAL = """
attribute vec4 aFramePosition;
varying vec2 vTexCoord;
void main() {
    gl_Position = aFramePosition;
    vTexCoord = aFramePosition.xy * 0.5 + 0.5;
}
"""

        private const val FRAGMENT_SHADER_UNIVERSAL = """
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif

uniform sampler2D uTexSampler;
uniform vec2 uTexelSize;
uniform float uEnabled;
uniform float uExposure;
uniform float uBrightness;
uniform float uContrast;
uniform float uHighlights;
uniform float uShadows;
uniform float uWarmth;
uniform float uTint;
uniform float uSaturation;
uniform float uVibrance;
uniform float uFade;
uniform float uSharpen;
uniform float uVignette;
uniform float uGrain;
uniform float uTimeSeed;

varying vec2 vTexCoord;

float luma(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

void main() {
    vec4 src = texture2D(uTexSampler, vTexCoord);
    vec3 rgb = src.rgb;

    if (uEnabled < 0.5) {
        gl_FragColor = src;
        return;
    }

    if (uSharpen > 0.001) {
        vec3 n = texture2D(uTexSampler, vTexCoord + vec2(0.0, uTexelSize.y)).rgb;
        vec3 s = texture2D(uTexSampler, vTexCoord - vec2(0.0, uTexelSize.y)).rgb;
        vec3 e = texture2D(uTexSampler, vTexCoord + vec2(uTexelSize.x, 0.0)).rgb;
        vec3 w = texture2D(uTexSampler, vTexCoord - vec2(uTexelSize.x, 0.0)).rgb;
        vec3 blur = (n + s + e + w) * 0.25;
        rgb = rgb + (rgb - blur) * (uSharpen * 1.4);
    }

    rgb *= exp2(uExposure);
    rgb += vec3(uBrightness * 0.30);

    rgb = (rgb - 0.5) * (1.0 + uContrast * 0.85) + 0.5;
    rgb = clamp(rgb, 0.0, 1.0);

    float y = luma(rgb);
    float shadowMask = (1.0 - smoothstep(0.0, 0.55, y));
    float highlightMask = smoothstep(0.45, 1.0, y);
    rgb += shadowMask * (uShadows * 0.35);
    rgb += highlightMask * (uHighlights * 0.35);

    rgb.r += uWarmth * 0.14;
    rgb.b -= uWarmth * 0.14;
    rgb.g += uTint * 0.12;
    rgb.r -= uTint * 0.05;
    rgb.b -= uTint * 0.05;
    rgb = clamp(rgb, 0.0, 1.0);

    float y2 = luma(rgb);
    float maxC = max(rgb.r, max(rgb.g, rgb.b));
    float minC = min(rgb.r, min(rgb.g, rgb.b));
    float curSat = maxC - minC;
    float vibBoost = uVibrance * (1.0 - curSat) * 0.9;
    float totalSat = max(0.0, 1.0 + uSaturation + vibBoost);
    rgb = mix(vec3(y2), rgb, totalSat);

    rgb = mix(rgb, vec3(0.08) + rgb * 0.88, clamp(uFade, 0.0, 1.0));

    if (uVignette > 0.001) {
        vec2 centered = vTexCoord - 0.5;
        float dist = length(centered);
        float vig = smoothstep(0.85, 0.25, dist * (1.0 + uVignette * 0.9));
        rgb *= mix(1.0, vig, uVignette * 0.85);
    }

    if (uGrain > 0.001) {
        float noise = hash12(vTexCoord * 512.0 + vec2(uTimeSeed * 97.0, uTimeSeed * 53.0)) - 0.5;
        rgb += vec3(noise * uGrain * 0.14);
    }

    gl_FragColor = vec4(clamp(rgb, 0.0, 1.0), src.a);
}
"""
    }
}
