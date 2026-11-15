package com.rikodev.music.android.dna.ui

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.view.View
import com.rikodev.music.android.dna.render.visual.BaseVisualRenderer

/**
 * Generic GLSurfaceView for any RIKO visual. Uses RENDERMODE_CONTINUOUSLY
 * for reliable rendering (the renderer internally throttles via governor).
 */
@SuppressLint("ViewConstructor")
class VisualGLSurfaceView(context: Context, val renderer: BaseVisualRenderer) : GLSurfaceView(context) {
    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 0, 0)
        preserveEGLContextOnPause = false
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
}
