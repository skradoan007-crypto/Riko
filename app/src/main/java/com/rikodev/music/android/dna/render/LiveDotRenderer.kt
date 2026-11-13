package com.rikodev.music.android.dna.render

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.rikodev.music.android.audiofx.SpectrumBands
import com.rikodev.music.android.audiofx.visualizer.MdnaSpectrumMath
import com.rikodev.music.android.dna.core.LiveDotInput
import com.rikodev.music.android.dna.core.BandSource
import com.rikodev.music.android.dna.core.QualityGovernor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/** One reusable VBO, one GL_POINTS draw, no offscreen passes or frame allocations. */
class LiveDotRenderer(private val source: BandSource, refreshRateHz: Float = 60f) : GLSurfaceView.Renderer {
    init { require(source.bandCount == SpectrumBands.COUNT) }
    @Volatile var quality = MdnaQuality.MID
    @Volatile var autoQuality = true
    @Volatile var playing = true
    @Volatile var frozen = false
    @Volatile var palette = 0f
    @Volatile var seed = 0f
    @Volatile var sensitivity = 1.5f
    @Volatile var sampleRateHz = 44100
    @Volatile var displayFrameNanos = 0L
    @Volatile var error: String? = null
        private set
    @Volatile var frameMs = 0f
        private set
    @Volatile var hasSignal = false
        private set
    @Volatile var bass = 0f
        private set
    @Volatile var mid = 0f
        private set
    @Volatile var treble = 0f
        private set

    private val input = LiveDotInput(source)
    private val governor = QualityGovernor(refreshRateHz)
    private var program = 0
    private val buffer = IntArray(1)
    private var idAttribute = 0
    private val uniforms = IntArray(10)
    private var width = 1
    private var height = 1
    private var lastFrame = 0L
    private var previousQuality = quality
    private var maxPointSize = 1f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Context loss invalidates old handles; never delete them in a new context.
        program = 0; buffer[0] = 0; error = null; lastFrame = 0L
        input.reset(); governor.reset(); frameMs = 0f
        hasSignal = false; bass = 0f; mid = 0f; treble = 0f
        try {
            val vs = compile(GLES20.GL_VERTEX_SHADER, LiveDotShaders.VERT)
            val fs = try { compile(GLES20.GL_FRAGMENT_SHADER, LiveDotShaders.FRAG) }
                catch (failure: Exception) { GLES20.glDeleteShader(vs); throw failure }
            program = GLES20.glCreateProgram()
            try {
                GLES20.glAttachShader(program, vs); GLES20.glAttachShader(program, fs)
                GLES20.glLinkProgram(program)
            } finally { GLES20.glDeleteShader(vs); GLES20.glDeleteShader(fs) }
            val status = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
            check(status[0] != 0) { "Dot shader link failed: ${GLES20.glGetProgramInfoLog(program).take(512)}" }
            val names = arrayOf("uViewport", "uColumns", "uPointSize", "uMaxPointSize", "uPalette",
                "uSeed", "uGain", "uBass", "uBands[0]", "uTrail[0]")
            for (i in 0..9) uniforms[i] = GLES20.glGetUniformLocation(program, names[i])
            idAttribute = GLES20.glGetAttribLocation(program, "aId")
            check(idAttribute >= 0 && (0..9).all { uniforms[it] >= 0 }) { "Dot shader input missing" }
            val vertices = ByteBuffer.allocateDirect(MAX_POINTS * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            for (i in 0 until MAX_POINTS) vertices.put(i.toFloat())
            vertices.flip()
            GLES20.glGenBuffers(1, buffer, 0)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer[0])
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, MAX_POINTS * 4, vertices, GLES20.GL_STATIC_DRAW)
            val range = FloatArray(2)
            GLES20.glGetFloatv(GLES20.GL_ALIASED_POINT_SIZE_RANGE, range, 0)
            maxPointSize = range[1].coerceAtLeast(1f)
            GLES20.glDisable(GLES20.GL_DEPTH_TEST)
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE)
            check(GLES20.glGetError() == GLES20.GL_NO_ERROR) { "Dot GPU initialization failed" }
        } catch (failure: Exception) {
            error = failure.message ?: "Dot GPU initialization failed"
            release()
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        this.width = width.coerceAtLeast(1); this.height = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, this.width, this.height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        if (program == 0 || error != null) return
        val now = displayFrameNanos.takeIf { it > 0L } ?: System.nanoTime()
        val elapsed = if (lastFrame == 0L) 0f else (now - lastFrame).coerceAtLeast(0L) / 1e9f
        lastFrame = now
        if (elapsed in 0.001f..0.25f) {
            frameMs = if (frameMs == 0f) elapsed * 1000f else frameMs + (elapsed * 1000f - frameMs) * .08f
        }
        if (previousQuality != quality) { governor.reset(); previousQuality = quality }
        governor.enabled = autoQuality && playing && !frozen
        if (governor.onFrame(elapsed * 1000f, quality != MdnaQuality.LOW)) quality = quality.lower() ?: quality
        input.step(now, elapsed, playing, frozen)
        hasSignal = input.hasSignal
        val bassEnd = SpectrumBands.bandIndex(250f, sampleRateHz.coerceAtLeast(8000))
        val midEnd = SpectrumBands.bandIndex(4000f, sampleRateHz.coerceAtLeast(8000))
        bass = MdnaSpectrumMath.envelope(input.fast, 0, bassEnd)
        mid = MdnaSpectrumMath.envelope(input.fast, bassEnd, midEnd)
        treble = MdnaSpectrumMath.envelope(input.fast, midEnd, 40)
        val points = when (quality) { MdnaQuality.LOW -> 3840; MdnaQuality.MID -> 9216; MdnaQuality.HIGH -> MAX_POINTS }
        GLES20.glUseProgram(program)
        GLES20.glUniform2f(uniforms[0], width.toFloat(), height.toFloat())
        GLES20.glUniform1f(uniforms[1], points / 48f)
        GLES20.glUniform1f(uniforms[2], (minOf(width, height) / 260f).coerceIn(1.6f, 6f))
        GLES20.glUniform1f(uniforms[3], maxPointSize)
        GLES20.glUniform1f(uniforms[4], palette.coerceIn(0f, 1f))
        GLES20.glUniform1f(uniforms[5], seed.coerceIn(0f, 1f))
        GLES20.glUniform1f(uniforms[6], sensitivity.coerceIn(.5f, 3f))
        GLES20.glUniform1f(uniforms[7], bass)
        GLES20.glUniform1fv(uniforms[8], 40, input.fast, 0)
        GLES20.glUniform1fv(uniforms[9], 40, input.trail, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer[0])
        GLES20.glEnableVertexAttribArray(idAttribute)
        GLES20.glVertexAttribPointer(idAttribute, 1, GLES20.GL_FLOAT, false, 4, 0)
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, points)
        GLES20.glDisableVertexAttribArray(idAttribute)
        if (GLES20.glGetError() != GLES20.GL_NO_ERROR) {
            error = "Dot GPU draw failed"; release()
        }
    }

    /** GL thread, while the owning context is current. */
    fun release() {
        if (program != 0) GLES20.glDeleteProgram(program)
        if (buffer[0] != 0) GLES20.glDeleteBuffers(1, buffer, 0)
        program = 0; buffer[0] = 0
    }

    private fun compile(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code); GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val message = GLES20.glGetShaderInfoLog(shader).take(512)
            GLES20.glDeleteShader(shader)
            error("Dot shader compile failed: $message")
        }
        return shader
    }

    private companion object { const val MAX_POINTS = 15360 }
}
