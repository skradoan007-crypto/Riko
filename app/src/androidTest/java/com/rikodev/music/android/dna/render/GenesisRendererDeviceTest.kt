package com.rikodev.music.android.dna.render

import android.os.Build
import android.graphics.RuntimeShader
import com.rikodev.music.android.dna.lab.GenesisAtmosphere
import org.junit.Assume.assumeTrue
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES30
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rikodev.music.android.dna.core.GenesisDemoSource
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer

/** Real GLES driver tests; run on API 26/32/33+ devices. Not a physical timing benchmark. */
@RunWith(AndroidJUnit4::class)
class GenesisRendererDeviceTest {
    @android.annotation.TargetApi(33)
    @Test fun agslAtmosphereCompilesOnSupportedAndroid() {
        assumeTrue(Build.VERSION.SDK_INT >= 33)
        val shader = RuntimeShader(GenesisAtmosphere.SOURCE)
        shader.setFloatUniform("size", 256f, 256f)
        shader.setFloatUniform("time", 1f)
        shader.setFloatUniform("bass", 0.5f)
    }

    @Test fun rendersAllTiersAndRecreatesContextWithoutStaleObjects() {
        val renderer = MdnaGlRenderer(GenesisDemoSource(), 2f).apply {
            template = MdnaTemplate.GENESIS_ORB
            autoQuality = false
        }
        repeat(2) {
            val session = EglSession()
            try {
                renderer.onSurfaceCreated(null, null)
                renderer.onSurfaceChanged(null, 256, 256)
                for (tier in MdnaQuality.entries) {
                    renderer.quality = tier
                    renderer.onDrawFrame(null)
                    renderer.onDrawFrame(null)
                    assertNull(renderer.error)
                    assertNull(renderer.templateError)
                    assertEquals(MdnaTemplate.GENESIS_ORB.totalPoints(tier), renderer.drawnPoints)
                    val pixels = ByteBuffer.allocateDirect(256 * 256 * 4)
                    GLES30.glReadPixels(0, 0, 256, 256, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, pixels)
                    assertEquals(GLES30.GL_NO_ERROR, GLES30.glGetError())
                    var lit = 0
                    for (i in 0 until 256 * 256) {
                        if ((pixels.get(i * 4).toInt() and 255) > 8 ||
                            (pixels.get(i * 4 + 2).toInt() and 255) > 8) lit++
                    }
                    assertTrue("No visible geometry on $tier", lit > 100)
                }
                renderer.animationEnabled = false
                renderer.onDrawFrame(null)
                val pausedTime = renderer.motionTimeSec
                renderer.onDrawFrame(null)
                assertEquals(pausedTime, renderer.motionTimeSec, 0f)
                // First pass deliberately destroys the context without release (driver context loss).
                // Second pass exercises explicit GL-thread disposal.
                if (it == 1) renderer.release()
            } finally { session.close() }
        }
    }

    private class EglSession {
        private val display: EGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        private val surface: EGLSurface
        private val context: EGLContext
        init {
            val version = IntArray(2)
            check(EGL14.eglInitialize(display, version, 0, version, 1))
            val configs = arrayOfNulls<EGLConfig>(1)
            val count = IntArray(1)
            check(EGL14.eglChooseConfig(display, intArrayOf(
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_RENDERABLE_TYPE, 0x40, // EGL_OPENGL_ES3_BIT_KHR
                EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_NONE), 0, configs, 0, 1, count, 0) && count[0] > 0)
            val config = requireNotNull(configs[0])
            context = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE), 0)
            surface = EGL14.eglCreatePbufferSurface(display, config,
                intArrayOf(EGL14.EGL_WIDTH, 256, EGL14.EGL_HEIGHT, 256, EGL14.EGL_NONE), 0)
            check(EGL14.eglMakeCurrent(display, surface, surface, context))
        }
        fun close() {
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(display, surface)
            EGL14.eglDestroyContext(display, context)
            EGL14.eglTerminate(display)
        }
    }
}
