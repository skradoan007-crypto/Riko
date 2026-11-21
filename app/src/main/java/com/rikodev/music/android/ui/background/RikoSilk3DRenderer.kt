package com.rikodev.music.android.ui.background

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.os.SystemClock
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

// Import your app module's R class here, e.g.: import com.rikodev.music.android.R

internal class RikoSilk3DRenderer(
    private val appContext: Context,
    private val darkFieldResId: Int,
    private val lightFieldResId: Int
) : GLSurfaceView.Renderer {

    // Lock-free target state written from Compose (0 allocations per frame!)
    @Volatile var targetThemeMix: Float = 0f
    @Volatile var targetAudioEnergy: Float = 0f
    @Volatile var targetPigmentAmount: Float = 0f
    @Volatile var targetPrimaryR: Float = 0.95f
    @Volatile var targetPrimaryG: Float = 0.11f
    @Volatile var targetPrimaryB: Float = 0.27f
    @Volatile var targetSecondaryR: Float = 1.00f
    @Volatile var targetSecondaryG: Float = 0.56f
    @Volatile var targetSecondaryB: Float = 0.20f
    @Volatile var touchX: Float = 0.5f
    @Volatile var touchY: Float = 0.5f
    @Volatile var pendingTouchPulse: Float = 0f

    // Smoothly damped live GPU state
    private var curThemeMix = 0f
    private var curAudioEnergy = 0f
    private var curPigmentAmount = 0f
    private var curP1R = 0.95f; private var curP1G = 0.11f; private var curP1B = 0.27f
    private var curP2R = 1.00f; private var curP2G = 0.56f; private var curP2B = 0.20f
    private var curTouchImpulse = 0f

    private var monotonicTime = 0f
    private var lastFrameMs = 0L
    private var viewportAspect = 0.46f

    private var program = 0
    private var posLoc = -1
    private var darkTexLoc = -1
    private var lightTexLoc = -1
    private var timeLoc = -1
    private var aspectLoc = -1
    private var themeLoc = -1
    private var audioLoc = -1
    private var touchPosLoc = -1
    private var touchImpLoc = -1
    private var pigAmtLoc = -1
    private var pig1Loc = -1
    private var pig2Loc = -1

    private var quadVbo = 0
    private var darkTexId = 0
    private var lightTexId = 0

    fun triggerTouchImpulse(normX: Float, normY: Float, strength: Float = 1.0f) {
        touchX = normX.coerceIn(0f, 1f)
        touchY = normY.coerceIn(0f, 1f)
        pendingTouchPulse = strength.coerceIn(0f, 1f)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glClearColor(0.01f, 0.03f, 0.04f, 1.0f)

        program = createProgram(
            RikoFluidShaders.FLUID_VERTEX_SHADER,
            RikoFluidShaders.FLUID_FRAGMENT_SHADER
        )
        posLoc = GLES20.glGetAttribLocation(program, "aPos")
        darkTexLoc = GLES20.glGetUniformLocation(program, "uDarkFieldTex")
        lightTexLoc = GLES20.glGetUniformLocation(program, "uLightFieldTex")
        timeLoc = GLES20.glGetUniformLocation(program, "uTime")
        aspectLoc = GLES20.glGetUniformLocation(program, "uAspect")
        themeLoc = GLES20.glGetUniformLocation(program, "uThemeMix")
        audioLoc = GLES20.glGetUniformLocation(program, "uAudioEnergy")
        touchPosLoc = GLES20.glGetUniformLocation(program, "uTouchPos")
        touchImpLoc = GLES20.glGetUniformLocation(program, "uTouchImpulse")
        pigAmtLoc = GLES20.glGetUniformLocation(program, "uPigmentAmount")
        pig1Loc = GLES20.glGetUniformLocation(program, "uPigmentPrimary")
        pig2Loc = GLES20.glGetUniformLocation(program, "uPigmentSecondary")

        uploadQuadVbo()
        darkTexId = loadLightFieldTexture(darkFieldResId, fallbackArgb = 0xFF061A19.toInt())
        lightTexId = loadLightFieldTexture(lightFieldResId, fallbackArgb = 0xFFB8E2D6.toInt())
        lastFrameMs = SystemClock.uptimeMillis()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width.coerceAtLeast(1), height.coerceAtLeast(1))
        viewportAspect = width.toFloat() / height.coerceAtLeast(1).toFloat()
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = SystemClock.uptimeMillis()
        val dt = ((now - lastFrameMs).coerceIn(1L, 50L)) / 1000f
        lastFrameMs = now
        monotonicTime += dt

        val themeK = (dt * 4.8f).coerceIn(0f, 1f)     // ~650ms seamless Dark <-> Light morph
        val pigmentK = (dt * 0.95f).coerceIn(0f, 1f)  // ~3.0s organic cream-and-dye marbling entry
        val audioK = (dt * 7.5f).coerceIn(0f, 1f)     // silky damped bass breathing

        curThemeMix += (targetThemeMix - curThemeMix) * themeK
        curAudioEnergy += (targetAudioEnergy - curAudioEnergy) * audioK
        curPigmentAmount += (targetPigmentAmount - curPigmentAmount) * pigmentK

        curP1R += (targetPrimaryR - curP1R) * pigmentK
        curP1G += (targetPrimaryG - curP1G) * pigmentK
        curP1B += (targetPrimaryB - curP1B) * pigmentK
        curP2R += (targetSecondaryR - curP2R) * pigmentK
        curP2G += (targetSecondaryG - curP2G) * pigmentK
        curP2B += (targetSecondaryB - curP2B) * pigmentK

        val pulse = pendingTouchPulse
        if (pulse > curTouchImpulse) {
            curTouchImpulse = pulse
            pendingTouchPulse = 0f
        } else {
            curTouchImpulse = (curTouchImpulse - dt * 1.45f).coerceAtLeast(0f)
        }

        GLES20.glUseProgram(program)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, darkTexId)
        GLES20.glUniform1i(darkTexLoc, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, lightTexId)
        GLES20.glUniform1i(lightTexLoc, 1)

        GLES20.glUniform1f(timeLoc, monotonicTime)
        GLES20.glUniform1f(aspectLoc, viewportAspect)
        GLES20.glUniform1f(themeLoc, curThemeMix)
        GLES20.glUniform1f(audioLoc, curAudioEnergy)
        GLES20.glUniform2f(touchPosLoc, touchX, touchY)
        GLES20.glUniform1f(touchImpLoc, curTouchImpulse)
        GLES20.glUniform1f(pigAmtLoc, curPigmentAmount)
        GLES20.glUniform3f(pig1Loc, curP1R, curP1G, curP1B)
        GLES20.glUniform3f(pig2Loc, curP2R, curP2G, curP2B)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, quadVbo)
        GLES20.glEnableVertexAttribArray(posLoc)
        GLES20.glVertexAttribPointer(posLoc, 2, GLES20.GL_FLOAT, false, 0, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(posLoc)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    private fun uploadQuadVbo() {
        val buffers = IntArray(1)
        GLES20.glGenBuffers(1, buffers, 0)
        quadVbo = buffers[0]
        val quadData = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)
        val quadBuf: FloatBuffer = ByteBuffer.allocateDirect(quadData.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().put(quadData)
        quadBuf.position(0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, quadVbo)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, quadData.size * 4, quadBuf, GLES20.GL_STATIC_DRAW)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    private fun loadLightFieldTexture(resId: Int, fallbackArgb: Int): Int {
        val texIds = IntArray(1)
        GLES20.glGenTextures(1, texIds, 0)
        val texId = texIds[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        val opts = BitmapFactory.Options().apply {
            inScaled = false
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bmp = BitmapFactory.decodeResource(appContext.resources, resId, opts)
            ?: Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply { eraseColor(fallbackArgb) }

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
        bmp.recycle() // Immediately free CPU Java heap! (0 MB RAM overhead!)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        return texId
    }

    private fun createProgram(vSrc: String, fSrc: String): Int {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vSrc)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fSrc)
        val prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, vs)
        GLES20.glAttachShader(prog, fs)
        GLES20.glLinkProgram(prog)
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)
        return prog
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)
        return shader
    }
}
