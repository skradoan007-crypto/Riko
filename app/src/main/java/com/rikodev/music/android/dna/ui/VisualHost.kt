package com.rikodev.music.android.dna.ui

import android.app.ActivityManager
import android.content.Context
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.rikodev.music.android.dna.render.visual.RikoVisual
import com.rikodev.music.android.dna.render.visual.BaseVisualRenderer
import com.rikodev.music.android.dna.render.MdnaQuality
import com.rikodev.music.android.dna.sync.MdnaSyncHub

/**
 * Generic host for any RIKO visual. Creates the renderer, attaches the PCM
 * tap so real audio data flows, and manages GL lifecycle robustly.
 */
@Composable
fun VisualHost(
    visual: RikoVisual,
    hub: MdnaSyncHub,
    playing: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val view = remember(context, visual.id) {
        @Suppress("DEPRECATION")
        val refresh = (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
            ?.defaultDisplay?.refreshRate ?: 60f
        val renderer = visual.factory(hub.liveOnlySource, refresh) as BaseVisualRenderer
        VisualGLSurfaceView(context, renderer).also {
            if ((context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.isLowRamDevice == true) {
                renderer.quality = MdnaQuality.LOW
            }
        }
    }

    // Attach PCM tap + ensure GL thread is running.
    DisposableEffect(owner, view) {
        hub.attach()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> { view.onResume() }
                Lifecycle.Event.ON_PAUSE -> { view.onPause() }
                else -> {}
            }
        }
        owner.lifecycle.addObserver(observer)
        // Always resume — GLSurfaceView needs onResume() to start the GL thread.
        view.onResume()
        onDispose {
            owner.lifecycle.removeObserver(observer)
            view.onPause()
            hub.detach()
        }
    }

    // Keep renderer state in sync with playback.
    LaunchedEffect(playing) {
        (view.renderer as? BaseVisualRenderer)?.playing = playing
    }

    AndroidView(factory = { view }, modifier = modifier)
}
