package com.rikodev.music.android.dna.lab

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.view.View
import com.rikodev.music.android.dna.render.MdnaGlRenderer

/** Separate Canvas/AGSL effect, API 33+ only. GLES remains the real 3D organism. */
@TargetApi(33)
class GenesisAtmosphere(context: Context, private val renderer: MdnaGlRenderer) : View(context) {
    var failure: String? = null
        private set
    var active = false
    private val paint = Paint()
    private val shader = try { RuntimeShader(SOURCE) } catch (e: Exception) {
        failure = (e.message ?: e.javaClass.simpleName).take(160)
        null
    }
    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        isClickable = false
        paint.shader = shader
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!active || shader == null || failure != null) return
        try {
            shader.setFloatUniform("size", width.toFloat(), height.toFloat())
            shader.setFloatUniform("time", renderer.motionTimeSec)
            shader.setFloatUniform("bass", renderer.bassLevel)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        } catch (e: Exception) {
            failure = (e.message ?: e.javaClass.simpleName).take(160)
            active = false // optional atmosphere fails closed; GLES keeps rendering
        }
        if (active) postInvalidateOnAnimation()
    }
    companion object {
        const val SOURCE = """
uniform float2 size;
uniform float time;
uniform float bass;
half4 main(float2 pixel) {
    float2 p = (pixel - size*0.5) / max(min(size.x,size.y),1.0);
    float r = length(p);
    float angle = atan(p.y,p.x);
    float halo = exp(-pow((r-0.34-bass*0.012)/0.065,2.0));
    float drift = 0.72 + 0.28*sin(angle*2.0-time*0.25);
    float a = halo*drift*(0.022+bass*0.014);
    half3 c = mix(half3(0.1,0.7,1.0),half3(0.7,0.2,0.9),half(p.x+0.5));
    return half4(c*a,a);
}
"""
    }
}
