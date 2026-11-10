package com.rikodev.music.android.dna.render

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import com.rikodev.music.android.dna.core.BandFollower
import com.rikodev.music.android.dna.core.BandSource
import com.rikodev.music.android.dna.core.BeatEnvelope
import com.rikodev.music.android.dna.core.HistoryClock
import com.rikodev.music.android.dna.core.QualityGovernor
import java.util.EnumMap
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.min
import kotlin.math.tan

// ─────────────────────────────────────────────────────────────────────────────
// RIKO DNA (MDNA) — GL ENGINE (P2: template system)
//
// Runs entirely on GLSurfaceView's own render thread, locked to the display's
// vsync (RENDERMODE_CONTINUOUSLY). Nothing here touches Compose, StateFlow or the
// main thread: once per frame it asks its [BandSource] for "the spectrum as of
// NOW" (the production source interpolates a timestamped history at
// now − output latency, so the picture lands on the beat the listener hears).
//
// SHARED ENGINE (one copy, every template benefits):
//   signal  → BandFollower (attack/release) + BeatEnvelope (punch) + history texture
//   camera  → SPHERE templates auto-fit the screen; SCREEN templates lay out from uView
//   post    → bright-pass → separable blur → composite (bloom), half-res friendly
//   safety  → QualityGovernor steps DOWN when frames miss budget
//
// TEMPLATE = [MdnaTemplate]: a shader pair + point budget + camera rule. Shaders
// are compiled lazily on first use; a template that fails to compile falls back to
// DNA Bloom and reports [templateError] — it never blanks the screen or crashes.
//
// Failure policy: a GL/shader problem in the ENGINE never crashes playback. It is
// logged, exposed via [error] for the lab overlay, and the frame is cleared.
// ─────────────────────────────────────────────────────────────────────────────
class MdnaGlRenderer(
    private val source: BandSource,
    private val density: Float,
    refreshRateHz: Float = 60f
) : GLSurfaceView.Renderer {

    // ── Tunables (any thread → render thread; @Volatile is enough for scalars) ──
    @Volatile var template: MdnaTemplate = MdnaTemplate.DNA_BLOOM
    @Volatile var quality: MdnaQuality = MdnaQuality.MID
    @Volatile var bloomEnabled: Boolean = true
    @Volatile var bloomIntensity: Float = 1.0f
    @Volatile var attackSec: Float = 0.012f
    @Volatile var releaseSec: Float = 0.090f
    @Volatile var autoRotate: Boolean = true

    /** When true the [QualityGovernor] may step [quality] DOWN if frames keep missing budget. */
    @Volatile var autoQuality: Boolean = true

    /** Per-song variation hook (P3 sets it from the audio genome); 0 = neutral. */
    @Volatile var seed: Float = 0f

    @Volatile private var userYaw = 0f
    @Volatile private var userPitch = 0f
    @Volatile private var zoom = 1f

    // ── Stats for the lab overlay (written on the render thread) ──
    @Volatile var frameMsAvg: Float = 16.7f
        private set
    @Volatile var drawnPoints: Int = 0
        private set
    @Volatile var bloomActive: Boolean = false
        private set
    @Volatile var error: String? = null
        private set
    @Volatile var templateError: String? = null
        private set
    @Volatile var autoDowngrades: Int = 0
        private set
    @Volatile var liveData: Boolean = false
        private set

    fun dragBy(dxDeg: Float, dyDeg: Float) {
        userYaw += dxDeg
        userPitch = (userPitch + dyDeg).coerceIn(-80f, 80f)
    }

    fun zoomBy(factor: Float) {
        zoom = (zoom * factor).coerceIn(0.5f, 2.5f)
    }

    // ── GL objects (render thread only) ──
    private var vao = 0
    private val scenes = EnumMap<MdnaTemplate, ScenePass>(MdnaTemplate::class.java)
    private val failedTemplates = HashSet<MdnaTemplate>()
    private var bright: PostPass? = null
    private var blur: PostPass? = null
    private var composite: PostPass? = null
    private var sceneTarget: Target? = null
    private var bloomA: Target? = null
    private var bloomB: Target? = null
    private var history: MdnaHistoryTexture? = null
    private var width = 1
    private var height = 1

    // ── Signal state (render thread only) ──
    private val follower = BandFollower(source.bandCount)
    private val beat = BeatEnvelope()
    private val histClock = HistoryClock()
    private val target = FloatArray(source.bandCount)
    private val governor = QualityGovernor(refreshRateHz)
    private var lastNanos = 0L
    private var timeSec = 0f
    private var autoYaw = 0f
    private var shownTemplate: MdnaTemplate? = null

    private val proj = FloatArray(16)
    private val view = FloatArray(16)
    private val model = FloatArray(16)
    private val tmp = FloatArray(16)
    private val mvp = FloatArray(16)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        try {
            error = null
            // A new GL context: everything cached from the old one is gone.
            scenes.clear()
            failedTemplates.clear()
            history = null
            val vaos = IntArray(1)
            GLES30.glGenVertexArrays(1, vaos, 0)
            vao = vaos[0]
            GLES30.glBindVertexArray(vao)
            scenes[MdnaTemplate.DNA_BLOOM] = buildScene(MdnaTemplate.DNA_BLOOM)
            bright = PostPass(Program(MdnaShaders.POST_VERT, MdnaShaders.BRIGHT_FRAG))
            blur = PostPass(Program(MdnaShaders.POST_VERT, MdnaShaders.BLUR_FRAG))
            composite = PostPass(Program(MdnaShaders.POST_VERT, MdnaShaders.COMPOSITE_FRAG))
            history = MdnaHistoryTexture(source.bandCount).also { it.create() }
            GLES30.glDisable(GLES30.GL_DEPTH_TEST)
            GLES30.glDisable(GLES30.GL_CULL_FACE)
            GLES30.glClearColor(0f, 0f, 0f, 1f)
            lastNanos = 0L
            histClock.reset()
        } catch (t: Throwable) {
            fail("init", t)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
        width = w.coerceAtLeast(1)
        height = h.coerceAtLeast(1)
        GLES30.glViewport(0, 0, width, height)
        try {
            releaseTargets()
            val st = createTarget(width, height)
            val ba = createTarget((width / 4).coerceAtLeast(1), (height / 4).coerceAtLeast(1))
            val bb = createTarget((width / 4).coerceAtLeast(1), (height / 4).coerceAtLeast(1))
            if (st != null && ba != null && bb != null) {
                sceneTarget = st; bloomA = ba; bloomB = bb
            } else {
                // Partial set is useless — drop everything and render without bloom.
                for (t in listOf(st, ba, bb)) t?.let { deleteTarget(it) }
                Log.w(TAG, "offscreen targets unavailable — bloom disabled")
            }
        } catch (t: Throwable) {
            fail("targets", t)
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = if (lastNanos == 0L) 0.016f else ((now - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.1f)
        lastNanos = now
        frameMsAvg += (dt * 1000f - frameMsAvg) * 0.05f
        timeSec += dt

        if (error != null || scenes.isEmpty()) {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
            return
        }
        try {
            governor.enabled = autoQuality
            val lower = quality.lower()
            if (governor.onFrame(dt * 1000f, lower != null) && lower != null) {
                Log.w(TAG, "frames over budget — quality ${quality.label} → ${lower.label}")
                quality = lower
                autoDowngrades++
            }
            drawFrame(dt, now)
        } catch (t: Throwable) {
            fail("draw", t)
        }
    }

    /** The scene for [wanted], compiling it on first use; falls back to DNA Bloom on failure. */
    private fun sceneFor(wanted: MdnaTemplate): Pair<MdnaTemplate, ScenePass> {
        scenes[wanted]?.let { return wanted to it }
        if (wanted !in failedTemplates) {
            try {
                val s = buildScene(wanted)
                scenes[wanted] = s
                templateError = null
                return wanted to s
            } catch (t: Throwable) {
                failedTemplates += wanted
                templateError = "${wanted.label}: ${t.message}"
                Log.e(TAG, "template ${wanted.label} failed — falling back to DNA Bloom", t)
            }
        }
        template = MdnaTemplate.DNA_BLOOM
        return MdnaTemplate.DNA_BLOOM to scenes.getValue(MdnaTemplate.DNA_BLOOM)
    }

    private fun drawFrame(dt: Float, nowNanos: Long) {
        val (tpl, sc) = sceneFor(template)
        if (tpl != shownTemplate) {
            // New template: start from its own default view, not the previous drag.
            shownTemplate = tpl
            userYaw = 0f
            userPitch = 0f
            autoYaw = 0f
        }

        // 1) Signal: the spectrum as of NOW (source handles latency), then ease toward it.
        liveData = source.sample(nowNanos, target)
        follower.step(target, dt, attackSec, releaseSec)
        val bands = follower.out
        val bass = mean(bands, 0, 6)
        val mid = mean(bands, 6, 20)
        val treble = mean(bands, 20, bands.size)
        val pulse = beat.step(bass, dt)

        // 1b) Rolling history (always fed, so switching to a history template is instant).
        val hist = history
        if (hist != null) {
            repeat(histClock.advance(dt)) { hist.pushRow(bands) }
        }

        // 2) Camera.
        if (autoRotate && tpl.rotates) autoYaw += dt * (8f + 22f * mid)
        val aspect = width.toFloat() / height.toFloat()
        val tanHalf = tan(Math.toRadians(FOV_DEG / 2.0)).toFloat()
        val baseZ = when (tpl.camera) {
            CameraKind.SPHERE -> tpl.fitRadius / (tanHalf * min(1f, aspect) * 0.92f)
            CameraKind.SCREEN -> SCREEN_CAMERA_Z
        }
        val halfH = tanHalf * baseZ
        val halfW = halfH * aspect
        Matrix.perspectiveM(proj, 0, FOV_DEG, aspect, 0.1f, 40f)
        Matrix.setLookAtM(view, 0, 0f, 0f, baseZ / zoom, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.setRotateM(model, 0, userYaw + autoYaw, 0f, 1f, 0f)
        Matrix.rotateM(model, 0, tpl.defaultPitchDeg + userPitch, 1f, 0f, 0f)
        Matrix.multiplyMM(tmp, 0, view, 0, model, 0)
        Matrix.multiplyMM(mvp, 0, proj, 0, tmp, 0)
        // Camera direction in MODEL space = third row of the rotation (column-major).
        val camX = model[2]
        val camY = model[6]
        val camZ = model[10]

        val q = quality
        val perLayer = tpl.pointsPerLayer(q)
        val points = perLayer * tpl.layers
        drawnPoints = points

        val post = sceneTarget != null && bloomEnabled &&
            bright != null && blur != null && composite != null
        bloomActive = post

        // 3) Scene pass (additive points).
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, if (post) sceneTarget!!.fbo else 0)
        GLES30.glViewport(0, 0, width, height)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE)
        GLES30.glUseProgram(sc.program.id)
        GLES30.glUniformMatrix4fv(sc.uMvp, 1, false, mvp, 0)
        GLES30.glUniform3f(sc.uCamDir, camX, camY, camZ)
        GLES30.glUniform1f(sc.uTime, timeSec)
        GLES30.glUniform1fv(sc.uBands, bands.size, bands, 0)
        GLES30.glUniform1f(sc.uBass, bass)
        GLES30.glUniform1f(sc.uMid, mid)
        GLES30.glUniform1f(sc.uTreble, treble)
        GLES30.glUniform1f(sc.uPulse, pulse)
        GLES30.glUniform1f(sc.uPointsPerLayer, perLayer.toFloat())
        GLES30.glUniform1f(sc.uPointScale, density * q.pointScale)
        GLES30.glUniform1f(sc.uGain, tpl.gain(q))
        GLES30.glUniform1f(sc.uSeed, seed)
        GLES30.glUniform2f(sc.uView, halfW, halfH)
        if (tpl.usesHistory && hist != null) {
            hist.bind(HISTORY_UNIT)
            GLES30.glUniform1i(sc.uHist, HISTORY_UNIT)
            GLES30.glUniform1f(sc.uHistHead, hist.head.toFloat())
            GLES30.glUniform1f(sc.uHistFrac, histClock.frac)
        }
        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, points)
        GLES30.glDisable(GLES30.GL_BLEND)

        if (!post) return

        // 4) Bloom: bright-pass + downsample → blur H → blur V (× passes).
        val st = sceneTarget!!
        val a = bloomA!!
        val b = bloomB!!
        val br = bright!!
        val bl = blur!!
        val co = composite!!

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, a.fbo)
        GLES30.glViewport(0, 0, a.w, a.h)
        GLES30.glUseProgram(br.program.id)
        bindTexture(0, st.tex)
        GLES30.glUniform1i(br.uTex, 0)
        GLES30.glUniform2f(br.uTexel, 1f / st.w, 1f / st.h)
        GLES30.glUniform1f(br.uThreshold, 0.55f)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)

        GLES30.glUseProgram(bl.program.id)
        GLES30.glUniform1i(bl.uTex, 0)
        for (i in 0 until q.blurPasses) {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, b.fbo)
            GLES30.glViewport(0, 0, b.w, b.h)
            bindTexture(0, a.tex)
            GLES30.glUniform2f(bl.uDir, 1f / a.w, 0f)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)

            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, a.fbo)
            GLES30.glViewport(0, 0, a.w, a.h)
            bindTexture(0, b.tex)
            GLES30.glUniform2f(bl.uDir, 0f, 1f / b.h)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)
        }

        // 5) Composite to the screen.
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, width, height)
        GLES30.glUseProgram(co.program.id)
        bindTexture(0, st.tex)
        bindTexture(1, a.tex)
        GLES30.glUniform1i(co.uScene, 0)
        GLES30.glUniform1i(co.uBloom, 1)
        GLES30.glUniform1f(co.uBloomIntensity, bloomIntensity * (0.8f + 0.9f * pulse))
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
    }

    // ── helpers ──

    private fun buildScene(t: MdnaTemplate): ScenePass =
        ScenePass(Program(t.vertexSource, t.fragmentSource))

    private fun mean(a: FloatArray, from: Int, to: Int): Float {
        val end = if (to > a.size) a.size else to
        if (end <= from) return 0f
        var s = 0f
        for (i in from until end) s += a[i]
        return s / (end - from)
    }

    private fun bindTexture(unit: Int, tex: Int) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + unit)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex)
    }

    private fun fail(stage: String, t: Throwable) {
        val msg = "MDNA GL $stage failed: ${t.message}"
        Log.e(TAG, msg, t)
        error = msg
    }

    private class Target(val fbo: Int, val tex: Int, val w: Int, val h: Int)

    private fun createTarget(w: Int, h: Int): Target? {
        val tex = IntArray(1)
        GLES30.glGenTextures(1, tex, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex[0])
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA8, w, h, 0,
            GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null
        )
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        val fbo = IntArray(1)
        GLES30.glGenFramebuffers(1, fbo, 0)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo[0])
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, tex[0], 0
        )
        val ok = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER) == GLES30.GL_FRAMEBUFFER_COMPLETE
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        val t = Target(fbo[0], tex[0], w, h)
        if (!ok) {
            deleteTarget(t)
            return null
        }
        return t
    }

    private fun deleteTarget(t: Target) {
        GLES30.glDeleteFramebuffers(1, intArrayOf(t.fbo), 0)
        GLES30.glDeleteTextures(1, intArrayOf(t.tex), 0)
    }

    private fun releaseTargets() {
        sceneTarget?.let { deleteTarget(it) }
        bloomA?.let { deleteTarget(it) }
        bloomB?.let { deleteTarget(it) }
        sceneTarget = null
        bloomA = null
        bloomB = null
    }

    private class Program(vs: String, fs: String) {
        val id: Int = link(vs, fs)
        fun uniform(name: String): Int = GLES30.glGetUniformLocation(id, name)
    }

    /** Every scene shader shares these names; ones a template omits resolve to -1 (ignored by GL). */
    private class ScenePass(val program: Program) {
        val uMvp = program.uniform("uMvp")
        val uCamDir = program.uniform("uCamDir")
        val uTime = program.uniform("uTime")
        val uBands = program.uniform("uBands")
        val uBass = program.uniform("uBass")
        val uMid = program.uniform("uMid")
        val uTreble = program.uniform("uTreble")
        val uPulse = program.uniform("uPulse")
        val uPointsPerLayer = program.uniform("uPointsPerLayer")
        val uPointScale = program.uniform("uPointScale")
        val uGain = program.uniform("uGain")
        val uSeed = program.uniform("uSeed")
        val uView = program.uniform("uView")
        val uHist = program.uniform("uHist")
        val uHistHead = program.uniform("uHistHead")
        val uHistFrac = program.uniform("uHistFrac")
    }

    private class PostPass(val program: Program) {
        val uTex = program.uniform("uTex")
        val uTexel = program.uniform("uTexel")
        val uThreshold = program.uniform("uThreshold")
        val uDir = program.uniform("uDir")
        val uScene = program.uniform("uScene")
        val uBloom = program.uniform("uBloom")
        val uBloomIntensity = program.uniform("uBloomIntensity")
    }

    private companion object {
        const val TAG = "MdnaGl"
        const val FOV_DEG = 42f
        const val SCREEN_CAMERA_Z = 3.0f
        const val HISTORY_UNIT = 2

        fun compile(type: Int, source: String): Int {
            val shader = GLES30.glCreateShader(type)
            GLES30.glShaderSource(shader, source.trim())
            GLES30.glCompileShader(shader)
            val status = IntArray(1)
            GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
            if (status[0] == 0) {
                val log = GLES30.glGetShaderInfoLog(shader)
                GLES30.glDeleteShader(shader)
                throw IllegalStateException("shader compile: $log")
            }
            return shader
        }

        fun link(vs: String, fs: String): Int {
            val v = compile(GLES30.GL_VERTEX_SHADER, vs)
            val f = compile(GLES30.GL_FRAGMENT_SHADER, fs)
            val program = GLES30.glCreateProgram()
            GLES30.glAttachShader(program, v)
            GLES30.glAttachShader(program, f)
            GLES30.glLinkProgram(program)
            GLES30.glDeleteShader(v)
            GLES30.glDeleteShader(f)
            val status = IntArray(1)
            GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, status, 0)
            if (status[0] == 0) {
                val log = GLES30.glGetProgramInfoLog(program)
                GLES30.glDeleteProgram(program)
                throw IllegalStateException("program link: $log")
            }
            return program
        }
    }
}
