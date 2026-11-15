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
 * Generic host for any RIKO visual. Creates the renderer from the visual's
 * factory, attaches the PCM tap (so audio data flows), and manages GL lifecycle.
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
        val renderer = visual.factory(hub.liveOnlySource, refresh)
        VisualGLSurfaceView(context, renderer).also {
            if ((context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.isLowRamDevice == true) {
                (renderer as? BaseVisualRenderer)?.quality = MdnaQuality.LOW
            }
        }
    }

    // Attach/detach PCM tap so the visual receives real audio data.
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
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            view.onResume()
        }
        onDispose {
            owner.lifecycle.removeObserver(observer)
            view.onPause()
            hub.detach()
        }
    }

    LaunchedEffect(playing) {
        (view.renderer as? BaseVisualRenderer)?.let { r ->
            r.playing = playing
        }
    }

    AndroidView(factory = { view }, modifier = modifier)
}
