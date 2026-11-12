package com.rikodev.music.android.dna.render

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.view.Choreographer
import android.view.MotionEvent
import android.view.GestureDetector
import android.view.accessibility.AccessibilityNodeInfo
import android.os.Bundle
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

    private var running = false
    private val choreographer = Choreographer.getInstance()
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            renderer.displayFrameNanos = frameTimeNanos
            requestRender()
            choreographer.postFrameCallback(this)
        }
    }
    override fun onResume() {
        super.onResume()
        if (!running) {
            running = true
            choreographer.postFrameCallback(frameCallback)
        }
    }
    override fun onPause() {
        running = false
        choreographer.removeFrameCallback(frameCallback)
        queueEvent { renderer.camera.cancel(); renderer.release() }
        super.onPause()
    }
    override fun onDetachedFromWindow() {
        running = false
        choreographer.removeFrameCallback(frameCallback)
        super.onDetachedFromWindow()
    }

    var onPulse: (() -> Unit)? = null
    private var multiplePointers = false
    private val gestures = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            queueEvent { renderer.camera.cancel() }
            return true
        }
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            if (!multiplePointers) performClick()
            return true
        }
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
            if (!multiplePointers) queueEvent { renderer.dragBy(-dx * DRAG_DEG_PER_PX, -dy * DRAG_DEG_PER_PX) }
            return true
        }
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
            if (!multiplePointers && android.animation.ValueAnimator.areAnimatorsEnabled())
                queueEvent { renderer.camera.fling(vx * DRAG_DEG_PER_PX, vy * DRAG_DEG_PER_PX) }
            return true
        }
    })
    private val scaleDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val factor = detector.scaleFactor
                queueEvent { renderer.zoomBy(factor) }
                return true
            }
        })

    init {
        isClickable = true
        isFocusable = true
        contentDescription = "Music DNA organism. Tap to pulse. Drag to rotate, pinch to zoom."
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 0, 0)
        preserveEGLContextOnPause = false
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun performClick(): Boolean {
        super.performClick()
        queueEvent { renderer.camera.pulse() }
        onPulse?.invoke()
        return true
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD)
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD)
    }
    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean {
        if (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD || action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) {
            queueEvent { renderer.zoomBy(if (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) 1.15f else 1f / 1.15f) }
            return true
        }
        return super.performAccessibilityAction(action, arguments)
    }
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.actionMasked == MotionEvent.ACTION_DOWN) {
            multiplePointers = false
            parent?.requestDisallowInterceptTouchEvent(true)
        }
        if (e.pointerCount > 1) multiplePointers = true
        scaleDetector.onTouchEvent(e)
        gestures.onTouchEvent(e)
        if (e.actionMasked == MotionEvent.ACTION_CANCEL) queueEvent { renderer.camera.cancel() }
        if (e.actionMasked == MotionEvent.ACTION_UP || e.actionMasked == MotionEvent.ACTION_CANCEL)
            parent?.requestDisallowInterceptTouchEvent(false)
        return true
    }

    private companion object {
        const val DRAG_DEG_PER_PX = 0.35f
    }
}
