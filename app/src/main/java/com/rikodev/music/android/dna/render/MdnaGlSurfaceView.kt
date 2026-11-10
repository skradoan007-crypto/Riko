package com.rikodev.music.android.dna.render

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.rikodev.music.android.dna.core.BandSource

// ─────────────────────────────────────────────────────────────────────────────
// RIKO DNA (MDNA) — GL SURFACE VIEW
//
// GLES 3.0, no depth/stencil, continuous vsync-locked rendering on the view's
// own render thread. One-finger drag rotates, pinch zooms. Hosts [renderer] so
// callers can tune it and read its stats.
// ─────────────────────────────────────────────────────────────────────────────
@SuppressLint("ViewConstructor")
class MdnaGlSurfaceView(
    context: Context,
    source: BandSource,
    refreshRateHz: Float = 60f
) : GLSurfaceView(context) {

    val renderer = MdnaGlRenderer(source, context.resources.displayMetrics.density, refreshRateHz)

    private var lastX = 0f
    private var lastY = 0f

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                renderer.zoomBy(detector.scaleFactor)
                return true
            }
        }
    )

    init {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 0, 0)
        preserveEGLContextOnPause = true
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(e)
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = e.x
                lastY = e.y
            }
            MotionEvent.ACTION_MOVE -> if (!scaleDetector.isInProgress && e.pointerCount == 1) {
                renderer.dragBy((e.x - lastX) * DRAG_DEG_PER_PX, (e.y - lastY) * DRAG_DEG_PER_PX)
                lastX = e.x
                lastY = e.y
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val remaining = if (e.actionIndex == 0) 1 else 0
                lastX = e.getX(remaining)
                lastY = e.getY(remaining)
            }
        }
        return true
    }

    private companion object {
        const val DRAG_DEG_PER_PX = 0.35f
    }
}
