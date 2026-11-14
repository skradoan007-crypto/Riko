package com.rikodev.music.android.dna.render.visual

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.rikodev.music.android.audiofx.SpectrumBands
import com.rikodev.music.android.audiofx.visualizer.MdnaSpectrumMath
import com.rikodev.music.android.dna.core.BandSource
import com.rikodev.music.android.dna.core.LiveDotInput
import com.rikodev.music.android.dna.core.QualityGovernor
import com.rikodev.music.android.dna.render.MdnaQuality
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

// ─────────────────────────────────────────────────────────────────────────────
// RIKO VISUAL FRAMEWORK (Phase 4)
//
// The shared skeleton every RIKO visual renders with:
//
//   draw 0 (own program) — starfield + faint nebula backdrop, always behind.
//   draw 1 (uPass 1)     — the visual's dot HALO: wide, soft, dim.
//   draw 2 (uPass 2)     — the visual's dot CORE: sharp, bright.
//
// One reusable VBO of aId floats per program, additive blending
// (GL_ONE, GL_ONE) everywhere — crowded dots merge into solid neon fire.
//
// A visual supplies ONLY its vertex shader ([BaseVisualRenderer.vert]):
// geometry, light and colour per aId. The glow fragment shader is shared.
// Every visual receives the SAME live data through [STANDARD_UNIFORMS] —
// spectrum bands, energy envelopes, beat punch and the ClassReaction
// character (uFold/uFlare/uWidth/uBreathe/uDetail/uSparkle) — and MUST
// change its behaviour per sound class through them. That contract is
// enforced by RikoVisualsContractTest.
// ─────────────────────────────────────────────────────────────────────────────

/** The standard uniform set every RIKO visual shader may declare and read. */
object VisualContract {
    const val U_VIEWPORT = 0
    const val U_COLUMNS = 1
    const val U_POINT_SIZE = 2
    const val U_MAX_POINT_SIZE = 3
    const val U_PALETTE = 4
    const val U_SEED = 5
    const val U_GAIN = 6
    const val U_BASS = 7
    const val U_MID = 8
    const val U_PUNCH = 9
    const val U_PHASE = 10
    const val U_ROLL = 11
    const val U_PASS = 12
    const val U_FOLD = 13
    const val U_FLARE = 14
    const val U_WIDTH = 15
    const val U_BREATHE = 16
    const val U_DETAIL = 17
    const val U_SPARKLE = 18
    const val U_BANDS = 19
    const val U_TRAIL = 20
    const val U_TIME = 21

    val STANDARD_UNIFORMS = arrayOf(
        "uViewport", "uColumns", "uPointSize", "uMaxPointSize", "uPalette", "uSeed", "uGain",
        "uBass", "uMid", "uPunch", "uPhase", "uRoll", "uPass",
        "uFold", "uFlare", "uWidth", "uBreathe", "uDetail", "uSparkle",
        "uBands[0]", "uTrail[0]", "uTime"
    )

    /** The six class-reaction knobs — a visual must use at least three. */
    val REACTION_UNIFORMS = listOf("uFold", "uFlare", "uWidth", "uBreathe", "uDetail", "uSparkle")
}

/** Shared soft-dot glow fragment shader for every visual's halo/core passes. */
object VisualDots {
    const val FRAG = """
precision mediump float;
uniform float uPass;
varying vec3 vColor;
varying float vLight;
void main() {
    vec2 p = gl_PointCoord * 2.0 - 1.0;
    float r2 = dot(p, p);
    if (r2 > 1.0) discard;
    float light = (uPass < 1.5)
        ? 0.22 * (exp(-r2 * 2.0) + 0.10 * exp(-r2 * 0.9))
        : exp(-r2 * 4.5) + 0.12 * exp(-r2 * 1.5);
    gl_FragColor = vec4(vColor * vLight * light, 1.0);
}
"""
}

/** Shared starfield + nebula backdrop (its own tiny program). */
object VisualBackdrop {
    const val VERT = """
precision highp float;
attribute float aId;
uniform vec2 uViewport;
uniform float uPointSize;
uniform float uMaxPointSize;
uniform float uSeed;
uniform float uPhase;
varying vec3 vColor;
varying float vLight;
const float TAU = 6.28318530718;
float hash(float x) { return fract(sin(x * 12.9898 + uSeed * 39.71) * 43758.5453); }
void main() {
    vec2 scale = min(uViewport.x, uViewport.y) / uViewport;
    if (aId < 192.0) {
        // Stars: hash positions, slow drift, gentle twinkle.
        float h1 = hash(aId + 3.0);
        float h2 = hash(aId + 61.0);
        float h3 = hash(aId + 131.0);
        vec2 pos = vec2(h1 * 2.4 - 1.2, h2 * 2.4 - 1.2);
        pos += 0.012 * vec2(sin(uPhase * 0.07 + h3 * TAU), cos(uPhase * 0.05 + h1 * TAU));
        gl_Position = vec4(pos * scale, 0.0, 1.0);
        gl_PointSize = clamp(uPointSize * (0.22 + 0.55 * h3), 1.0, uMaxPointSize);
        float tw = 0.55 + 0.45 * sin(uPhase * (0.25 + h1) + h2 * TAU);
        vLight = (0.08 + 0.24 * h3) * tw;
        vColor = mix(vec3(0.62, 0.70, 1.0), vec3(1.0, 0.97, 0.85), step(0.8, h2));
    } else {
        // Nebula: 4 clusters x 16 faint slow clouds.
        float id2 = aId - 192.0;
        float cluster = floor(id2 / 16.0);
        float h1 = hash(cluster * 7.0 + 1.0);
        float h2 = hash(cluster * 7.0 + 29.0);
        vec2 center = vec2(h1 * 1.7 - 0.85, h2 * 1.7 - 0.85);
        float h3 = hash(id2 + 211.0);
        float h4 = hash(id2 + 907.0);
        vec2 pos = center + (vec2(h3, h4) - 0.5) * 0.38;
        gl_Position = vec4(pos * scale, 0.0, 1.0);
        gl_PointSize = clamp(uPointSize * (26.0 + 8.0 * h3), 1.0, uMaxPointSize);
        vLight = 0.013 + 0.010 * h4;
        vColor = mix(vec3(0.30, 0.22, 0.62), vec3(0.12, 0.30, 0.55), h1);
    }
}
"""
    const val FRAG = """
precision mediump float;
varying vec3 vColor;
varying float vLight;
void main() {
    vec2 p = gl_PointCoord * 2.0 - 1.0;
    float r2 = dot(p, p);
    if (r2 > 1.0) discard;
    float light = exp(-r2 * 3.0) + 0.10 * exp(-r2 * 1.2);
    gl_FragColor = vec4(vColor * vLight * light, 1.0);
}
"""
}

/**
 * GL boilerplate for one visual: shader program, VBOs, per-frame input
 * stepping, standard uniform upload and the three-pass draw.
 * Subclasses supply [vert] and [rows] only.
 */
abstract class BaseVisualRenderer(
    private val source: BandSource,
    refreshRateHz: Float = 60f
) : GLSurfaceView.Renderer {
    init { require(source.bandCount == SpectrumBands.COUNT) }

    /** The visual's vertex shader — geometry, light and colour per aId. */
    protected abstract val vert: String

    /** Dot-grid rows; columns = points / rows. Must divide every tier size. */
    protected abstract val rows: Int

    /** Shared glow fragment shader; override only for exotic shapes. */
    protected open val frag: String = VisualDots.FRAG

    // ── Public tuning — same contract as LiveDotRenderer (hosts set these) ──
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

    protected val input = LiveDotInput(source)
    private val governor = QualityGovernor(refreshRateHz)
    private var visualProgram = 0
    private var bgProgram = 0
    private val buffers = IntArray(2) // [0] backdrop ids, [1] visual ids
    private var visualIdAttribute = 0
    private var bgIdAttribute = 0
    private val uniforms = IntArray(VisualContract.STANDARD_UNIFORMS.size)
    private val bgUniforms = IntArray(5)
    private var width = 1
    private var height = 1
    private var lastFrame = 0L
    private var timeAccum = 0f
    private var previousQuality = quality
    private var maxPointSize = 1f

    /** Points for a quality tier. Divisible by [rows] in every tier. */
    protected open fun pointsFor(quality: MdnaQuality): Int = when (quality) {
        MdnaQuality.LOW -> 3840
        MdnaQuality.MID -> 9216
        MdnaQuality.HIGH -> MAX_POINTS
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        visualProgram = 0; bgProgram = 0; buffers[0] = 0; buffers[1] = 0
        error = null; lastFrame = 0L
        input.reset(); governor.reset(); frameMs = 0f
        timeAccum = 0f
        hasSignal = false; bass = 0f; mid = 0f; treble = 0f
        try {
            bgProgram = link(VisualBackdrop.VERT, VisualBackdrop.FRAG)
            visualProgram = link(vert, frag)
            for (i in VisualContract.STANDARD_UNIFORMS.indices) {
                uniforms[i] = GLES20.glGetUniformLocation(visualProgram, VisualContract.STANDARD_UNIFORMS[i])
            }
            visualIdAttribute = GLES20.glGetAttribLocation(visualProgram, "aId")
            check(visualIdAttribute >= 0) { "Visual shader missing aId" }
            check(uniforms[VisualContract.U_VIEWPORT] >= 0 && uniforms[VisualContract.U_PASS] >= 0) {
                "Visual shader missing core uniforms"
            }
            val bgNames = arrayOf("uViewport", "uPointSize", "uMaxPointSize", "uSeed", "uPhase")
            for (i in bgNames.indices) bgUniforms[i] = GLES20.glGetUniformLocation(bgProgram, bgNames[i])
            bgIdAttribute = GLES20.glGetAttribLocation(bgProgram, "aId")
            check(bgIdAttribute >= 0 && bgUniforms.all { it >= 0 }) { "Backdrop shader input missing" }

            buffers[0] = makeIdBuffer(BACKGROUND_POINTS)
            buffers[1] = makeIdBuffer(MAX_POINTS)

            val range = FloatArray(2)
            GLES20.glGetFloatv(GLES20.GL_ALIASED_POINT_SIZE_RANGE, range, 0)
            maxPointSize = range[1].coerceAtLeast(1f)
            GLES20.glDisable(GLES20.GL_DEPTH_TEST)
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE)
            check(GLES20.glGetError() == GLES20.GL_NO_ERROR) { "Visual GPU initialization failed" }
        } catch (failure: Exception) {
            error = failure.message ?: "Visual GPU initialization failed"
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
        if (visualProgram == 0 || bgProgram == 0 || error != null) return

        val now = displayFrameNanos.takeIf { it > 0L } ?: System.nanoTime()
        val elapsed = if (lastFrame == 0L) 0f else (now - lastFrame).coerceAtLeast(0L) / 1e9f
        lastFrame = now
        if (elapsed in 0.001f..0.25f) {
            frameMs = if (frameMs == 0f) elapsed * 1000f else frameMs + (elapsed * 1000f - frameMs) * .08f
        }
        if (elapsed in 0f..0.25f) timeAccum += elapsed
        if (previousQuality != quality) { governor.reset(); previousQuality = quality }
        governor.enabled = autoQuality && playing && !frozen
        if (governor.onFrame(elapsed * 1000f, quality != MdnaQuality.LOW)) quality = quality.lower() ?: quality

        val bassEnd = SpectrumBands.bandIndex(250f, sampleRateHz.coerceAtLeast(8000))
        val midEnd = SpectrumBands.bandIndex(4000f, sampleRateHz.coerceAtLeast(8000))
        input.bassEnd = bassEnd
        input.midEnd = midEnd
        input.sampleRateHz = sampleRateHz
        input.step(now, elapsed, playing, frozen)
        hasSignal = input.hasSignal
        bass = MdnaSpectrumMath.envelope(input.fast, 0, bassEnd)
        mid = MdnaSpectrumMath.envelope(input.fast, bassEnd, midEnd)
        treble = MdnaSpectrumMath.envelope(input.fast, midEnd, 40)

        // Pass 0 — shared starfield + nebula.
        GLES20.glUseProgram(bgProgram)
        GLES20.glUniform2f(bgUniforms[0], width.toFloat(), height.toFloat())
        GLES20.glUniform1f(bgUniforms[1], pointSize())
        GLES20.glUniform1f(bgUniforms[2], maxPointSize)
        GLES20.glUniform1f(bgUniforms[3], seed.coerceIn(0f, 1f))
        GLES20.glUniform1f(bgUniforms[4], input.phase)
        bindIds(buffers[0], bgIdAttribute)
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, BACKGROUND_POINTS)
        GLES20.glDisableVertexAttribArray(bgIdAttribute)

        // Passes 1+2 — the visual's halo and core.
        val points = pointsFor(quality)
        GLES20.glUseProgram(visualProgram)
        GLES20.glUniform2f(uniforms[VisualContract.U_VIEWPORT], width.toFloat(), height.toFloat())
        GLES20.glUniform1f(uniforms[VisualContract.U_COLUMNS], points / rows.toFloat())
        GLES20.glUniform1f(uniforms[VisualContract.U_POINT_SIZE], pointSize())
        GLES20.glUniform1f(uniforms[VisualContract.U_MAX_POINT_SIZE], maxPointSize)
        GLES20.glUniform1f(uniforms[VisualContract.U_PALETTE], palette.coerceIn(0f, 1f))
        GLES20.glUniform1f(uniforms[VisualContract.U_SEED], seed.coerceIn(0f, 1f))
        GLES20.glUniform1f(uniforms[VisualContract.U_GAIN], sensitivity.coerceIn(.5f, 3f))
        GLES20.glUniform1f(uniforms[VisualContract.U_BASS], input.bass)
        GLES20.glUniform1f(uniforms[VisualContract.U_MID], input.mid)
        GLES20.glUniform1f(uniforms[VisualContract.U_PUNCH], input.punch)
        GLES20.glUniform1f(uniforms[VisualContract.U_PHASE], input.phase)
        GLES20.glUniform1f(uniforms[VisualContract.U_ROLL], input.roll)
        GLES20.glUniform1f(uniforms[VisualContract.U_TIME], timeAccum)
        val reaction = input.reaction
        GLES20.glUniform1f(uniforms[VisualContract.U_FOLD], reaction.fold)
        GLES20.glUniform1f(uniforms[VisualContract.U_FLARE], reaction.flare)
        GLES20.glUniform1f(uniforms[VisualContract.U_WIDTH], reaction.width)
        GLES20.glUniform1f(uniforms[VisualContract.U_BREATHE], reaction.breathe)
        GLES20.glUniform1f(uniforms[VisualContract.U_DETAIL], reaction.detail)
        GLES20.glUniform1f(uniforms[VisualContract.U_SPARKLE], reaction.sparkle)
        GLES20.glUniform1fv(uniforms[VisualContract.U_BANDS], 40, input.fast, 0)
        GLES20.glUniform1fv(uniforms[VisualContract.U_TRAIL], 40, input.trail, 0)
        bindIds(buffers[1], visualIdAttribute)
        GLES20.glUniform1f(uniforms[VisualContract.U_PASS], 1f)
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, points)
        GLES20.glUniform1f(uniforms[VisualContract.U_PASS], 2f)
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, points)
        GLES20.glDisableVertexAttribArray(visualIdAttribute)

        if (GLES20.glGetError() != GLES20.GL_NO_ERROR) {
            error = "Visual GPU draw failed"; release()
        }
    }

    /** GL thread, while the owning context is current. */
    fun release() {
        if (visualProgram != 0) GLES20.glDeleteProgram(visualProgram)
        if (bgProgram != 0) GLES20.glDeleteProgram(bgProgram)
        GLES20.glDeleteBuffers(2, buffers, 0)
        visualProgram = 0; bgProgram = 0; buffers[0] = 0; buffers[1] = 0
    }

    // ── internals ────────────────────────────────────────────────────────

    private fun pointSize(): Float = (minOf(width, height) / 260f).coerceIn(1.6f, 6f)

    private fun makeIdBuffer(count: Int): Int {
        val vertices = ByteBuffer.allocateDirect(count * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        for (i in 0 until count) vertices.put(i.toFloat())
        vertices.flip()
        val handle = IntArray(1)
        GLES20.glGenBuffers(1, handle, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, handle[0])
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, count * 4, vertices, GLES20.GL_STATIC_DRAW)
        return handle[0]
    }

    private fun bindIds(buffer: Int, attribute: Int) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer)
        GLES20.glEnableVertexAttribArray(attribute)
        GLES20.glVertexAttribPointer(attribute, 1, GLES20.GL_FLOAT, false, 4, 0)
    }

    private fun link(vertCode: String, fragCode: String): Int {
        val vs = compile(GLES20.GL_VERTEX_SHADER, vertCode)
        val fs = try { compile(GLES20.GL_FRAGMENT_SHADER, fragCode) }
            catch (failure: Exception) { GLES20.glDeleteShader(vs); throw failure }
        val program = GLES20.glCreateProgram()
        try {
            GLES20.glAttachShader(program, vs); GLES20.glAttachShader(program, fs)
            GLES20.glLinkProgram(program)
        } finally { GLES20.glDeleteShader(vs); GLES20.glDeleteShader(fs) }
        val status = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
        check(status[0] != 0) { "Visual shader link failed: ${GLES20.glGetProgramInfoLog(program).take(512)}" }
        return program
    }

    private fun compile(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code); GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val message = GLES20.glGetShaderInfoLog(shader).take(512)
            GLES20.glDeleteShader(shader)
            error("Visual shader compile failed: $message")
        }
        return shader
    }

    private companion object {
        const val MAX_POINTS = 15360
        const val BACKGROUND_POINTS = 256
    }
}
