package com.rikodev.music.android.dna.ui

import android.app.ActivityManager
import android.content.Context
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.rikodev.music.android.audiofx.visualizer.SpectrumConfig
import com.rikodev.music.android.audiofx.visualizer.SpectrumTier
import com.rikodev.music.android.dna.render.MdnaGlSurfaceView
import com.rikodev.music.android.dna.render.MdnaQuality
import com.rikodev.music.android.dna.sync.MdnaSyncHub
import com.rikodev.music.android.dna.sync.MdnaViewModel

// ─────────────────────────────────────────────────────────────────────────────
// RIKO DNA (MDNA) — VISUALIZER HOST (P1)
//
// Compose seam for the GL engine. Compose owns only: creating the surface once,
// tying the tap + GL context to the screen's lifecycle, and the small sync
// control. Every frame after that is the render thread's business — nothing
// here is read or recomposed per frame.
//
// Lifecycle contract: while this is RESUMED on screen the audio-thread tap is
// attached (ref-counted in [MdnaSyncHub]); on pause/dispose it is detached, so
// every other screen pays nothing.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MdnaVisualizerHost(
    modifier: Modifier = Modifier,
    viewModel: MdnaViewModel = hiltViewModel()
) {
    val hub = viewModel.hub
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Bumped after every attach() so the sync control re-reads the (possibly changed) output kind.
    var resumeTick by remember { mutableIntStateOf(0) }

    val glView = remember {
        val refreshHz = displayRefreshRate(context)
        MdnaGlSurfaceView(context, hub.source, refreshHz).also { view ->
            // Reuse the project's own device-tier rule. Never START at HIGH: the governor
            // only steps down, so a heavy first guess would stutter before it corrects.
            val tier = SpectrumConfig.tierFor(
                isLowRam = isLowRamDevice(context),
                cpuCores = Runtime.getRuntime().availableProcessors(),
                refreshHz = refreshHz
            )
            view.renderer.quality = if (tier == SpectrumTier.LOW) MdnaQuality.LOW else MdnaQuality.MID
            view.renderer.template = viewModel.settings.template
        }
    }
    var templateLabel by remember { mutableStateOf(glView.renderer.template.label) }

    DisposableEffect(lifecycleOwner, glView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    hub.attach()
                    glView.onResume()
                    resumeTick++
                }
                Lifecycle.Event.ON_PAUSE -> {
                    glView.onPause()
                    hub.detach()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // Leaving composition while still RESUMED never delivers ON_PAUSE.
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                glView.onPause()
                hub.detach()
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { glView },
            modifier = Modifier.fillMaxSize()
        )
        MdnaTemplatePicker(
            label = templateLabel,
            onPrevious = { switchTemplate(glView, viewModel, previous = true) { templateLabel = it } },
            onNext = { switchTemplate(glView, viewModel, previous = false) { templateLabel = it } },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp)
        )
        MdnaSyncControl(
            hub = hub,
            refreshKey = resumeTick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )
    }
}

private fun switchTemplate(
    glView: MdnaGlSurfaceView,
    viewModel: MdnaViewModel,
    previous: Boolean,
    onLabel: (String) -> Unit
) {
    val renderer = glView.renderer
    val next = if (previous) renderer.template.previous() else renderer.template.next()
    renderer.template = next
    viewModel.settings.template = next
    onLabel(next.label)
}

/** ‹ Template › — one tap each way; the choice is remembered. */
@Composable
private fun MdnaTemplatePicker(
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onPrevious) {
            Text("‹", color = CONTROL_COLOR, fontSize = 22.sp)
        }
        Text(text = label, color = CONTROL_COLOR, fontSize = 13.sp)
        TextButton(onClick = onNext) {
            Text("›", color = CONTROL_COLOR, fontSize = 22.sp)
        }
    }
}

/** − / + 10 ms nudge for the A/V offset of the CURRENT output (speaker vs Bluetooth). */
@Composable
private fun MdnaSyncControl(hub: MdnaSyncHub, refreshKey: Int, modifier: Modifier = Modifier) {
    var localTick by remember { mutableIntStateOf(0) }
    val offset = remember(refreshKey, localTick) { hub.offsetMs }
    val output = remember(refreshKey, localTick) { hub.outputLabel }
    val sign = if (offset > 0) "+" else ""
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { hub.setOffsetMs(hub.offsetMs - STEP_MS); localTick++ }) {
            Text("−", color = CONTROL_COLOR, fontSize = 18.sp)
        }
        Text(
            text = "Sync $sign$offset ms · $output",
            color = CONTROL_COLOR,
            fontSize = 12.sp
        )
        TextButton(onClick = { hub.setOffsetMs(hub.offsetMs + STEP_MS); localTick++ }) {
            Text("+", color = CONTROL_COLOR, fontSize = 18.sp)
        }
    }
}

@Suppress("DEPRECATION")
private fun displayRefreshRate(context: Context): Float {
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    val hz = wm?.defaultDisplay?.refreshRate ?: 60f
    return if (hz in 30f..240f) hz else 60f
}

private fun isLowRamDevice(context: Context): Boolean =
    (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.isLowRamDevice == true

private const val STEP_MS = 10
private val CONTROL_COLOR = Color.White.copy(alpha = 0.65f)
