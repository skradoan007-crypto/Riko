package com.rikodev.music.android.dna.render

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.view.Choreographer
import android.view.View
import com.rikodev.music.android.dna.core.BandSource

/** Vsync requests a dedicated GL thread. No frame state enters Compose. */
@SuppressLint("ViewConstructor")
class LiveDotSurfaceView(context: Context, source: BandSource, refreshRateHz: Float) : GLSurfaceView(context) {
    val renderer = LiveDotRenderer(source, refreshRateHz)
    private var running = false
    private var lastRequestNanos = 0L
    private val choreographer = Choreographer.getInstance()
    private val callback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            // Settling paused/frozen points needs only 15 Hz. Playback resumes
            // full display cadence immediately, without restarting the GL view.
            if ((renderer.playing && !renderer.frozen) || frameTimeNanos - lastRequestNanos >= 66_666_667L) {
                renderer.displayFrameNanos = frameTimeNanos
                lastRequestNanos = frameTimeNanos
                requestRender()
            }
            choreographer.postFrameCallback(this)
        }
    }
    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 0, 0)
        preserveEGLContextOnPause = false
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    override fun onResume() {
        if (running) return
        super.onResume()
        running = true
        lastRequestNanos = 0L
        choreographer.postFrameCallback(callback)
    }
    override fun onPause() {
        if (!running) return
        running = false
        choreographer.removeFrameCallback(callback)
        // EGL context destruction on pause releases all its GL resources.
        super.onPause()
    }
    override fun onDetachedFromWindow() {
        running = false
        choreographer.removeFrameCallback(callback)
        super.onDetachedFromWindow()
    }
}
