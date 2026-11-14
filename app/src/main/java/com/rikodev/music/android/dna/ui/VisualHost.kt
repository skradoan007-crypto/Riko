package com.rikodev.music.android.dna.ui

import android.app.ActivityManager
import android.content.Context
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.rikodev.music.android.dna.core.BandSource
import com.rikodev.music.android.dna.render.visual.RikoVisual
import com.rikodev.music.android.dna.render.visual.BaseVisualRenderer
import com.rikodev.music.android.dna.render.MdnaQuality

/**
 * Generic host for any RIKO visual. Creates the renderer from the visual's
 * factory and manages its GL lifecycle. Used by VisualsLabScreen to preview
 * any visual from the registry.
 */
@Composable
fun VisualHost(
    visual: RikoVisual,
    bandSource: BandSource,
    playing: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val view = remember(context, visual.id) {
        @Suppress("DEPRECATION")
        val refresh = (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
            ?.defaultDisplay?.refreshRate ?: 60f
        val renderer = visual.factory(bandSource, refresh)
        VisualGLSurfaceView(context, renderer as BaseVisualRenderer).also {
            if ((context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.isLowRamDevice == true) {
                (renderer as? BaseVisualRenderer)?.quality = MdnaQuality.LOW
            }
        }
    }

    var attached by remember { mutableStateOf(false) }
    DisposableEffect(owner, view) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    view.onResume()
                    attached = true
                }
                Lifecycle.Event.ON_PAUSE -> {
                    view.onPause()
                    attached = false
                }
                else -> {}
            }
        }
        owner.lifecycle.addObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            view.onResume()
            attached = true
        }
        onDispose {
            owner.lifecycle.removeObserver(observer)
            if (attached) {
                view.onPause()
                attached = false
            }
        }
    }

    LaunchedEffect(playing, attached) {
        (view.renderer as? BaseVisualRenderer)?.let { r ->
            r.playing = playing
        }
    }

    AndroidView(factory = { view }, modifier = modifier)
}
