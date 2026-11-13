package com.rikodev.music.android.dna.ui

import android.app.ActivityManager
import android.content.Context
import android.view.WindowManager
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
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
import androidx.lifecycle.repeatOnLifecycle
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
    viewModel: MdnaViewModel = hiltViewModel(),
    genome: com.rikodev.music.android.dna.genome.DnaGenome? = null,
    playing: Boolean = true,
    generation: Int = 0
) {
    val haptic = com.rikodev.music.android.core.designsystem.riko.components.rememberRikoHaptic()
    val hub = viewModel.hub
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Bumped after every attach() so the sync control re-reads the (possibly changed) output kind.
    var resumeTick by remember { mutableIntStateOf(0) }

    val supported = remember(context) {
        (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)
            ?.deviceConfigurationInfo?.reqGlEsVersion?.let { it >= 0x30000 } == true
    }
    if (!supported) {
        Text("MDNA needs OpenGL ES 3.0 on this device", color = Color.White, modifier = modifier)
        return
    }
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
            view.renderer.template = com.rikodev.music.android.dna.render.MdnaTemplate.GENESIS_ORB
        }
    }
    val renderState = remember(genome) { genome?.let { com.rikodev.music.android.dna.render.DnaRenderState(it) } }
    var motionPaused by remember { mutableStateOf(!android.animation.ValueAnimator.areAnimatorsEnabled()) }
    var qualityLabel by remember { mutableStateOf(glView.renderer.quality.label) }
    var signalLabel by remember { mutableStateOf("Waiting for audio") }
    var frameLabel by remember { mutableStateOf("") }
    var levelsLabel by remember { mutableStateOf("") }
    androidx.compose.runtime.LaunchedEffect(renderState) {
        if (renderState == null) glView.renderer.template = com.rikodev.music.android.dna.render.MdnaTemplate.GENESIS_ORB
    }
    androidx.compose.runtime.SideEffect {
        glView.onPulse = { haptic.fire(com.rikodev.music.android.core.designsystem.riko.components.RikoHapticEvent.CONFIRMATION) }
        glView.renderer.generation = generation.coerceIn(0, 64)
        glView.renderer.organism = renderState
        glView.renderer.animationEnabled = playing && !motionPaused
    }
    var rendererError by remember { mutableStateOf<String?>(null) }
    androidx.compose.runtime.LaunchedEffect(glView, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            while (true) {
                rendererError = glView.renderer.error ?: glView.renderer.templateError
                glView.renderer.sampleRateHz = hub.sampleRate.takeIf { it > 0 } ?: 44100
                qualityLabel = glView.renderer.quality.label
                signalLabel = when {
                    !hub.playing -> "Paused"
                    hub.liveSpectrumAvailable && hub.offlineTimelineAvailable -> "HYBRID"
                    hub.liveSpectrumAvailable -> "LIVE"
                    hub.offlineTimelineAvailable -> "Offline timeline"
                    else -> "Waiting for audio"
                }
                levelsLabel = "Bass ${(glView.renderer.bassLevel*100).toInt()} · Mid ${(glView.renderer.midLevel*100).toInt()} · High ${(glView.renderer.trebleLevel*100).toInt()}"
                val ms = glView.renderer.frameMsAvg
                frameLabel = if (ms > 0f) "${(1000f / ms).toInt()} FPS · ${"%.1f".format(ms)} ms" else ""
                kotlinx.coroutines.delay(500)
            }
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
        var controls by remember { mutableStateOf(false) }
        Row(Modifier.align(Alignment.TopCenter), verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(signalLabel, color = CONTROL_COLOR, fontSize = 12.sp)
                Text(frameLabel, color = CONTROL_COLOR, fontSize = 12.sp)
            }
            TextButton(onClick = { controls = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Controls", color = CONTROL_COLOR)
            }
        }
        if (controls) AlertDialog(
            onDismissRequest = { controls = false },
            containerColor = Color(0xFF171322),
            title = { Text("Organism controls", color = Color.White) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(levelsLabel, color = CONTROL_COLOR)
                    Text("Mid includes voice and instruments; it is not isolated vocals.", color = CONTROL_COLOR, fontSize = 12.sp)
                    TextButton(onClick = { glView.renderer.quality = glView.renderer.quality.next() }) {
                        Text("Quality: $qualityLabel · Auto", color = CONTROL_COLOR)
                    }
                    TextButton(onClick = { motionPaused = !motionPaused }) {
                        Text(if (motionPaused) "Resume motion" else "Freeze motion", color = CONTROL_COLOR)
                    }
                    Row {
                        TextButton(onClick = { glView.queueEvent { glView.renderer.dragBy(-15f, 0f) } }) { Text("Left", color = CONTROL_COLOR) }
                        TextButton(onClick = { glView.queueEvent { glView.renderer.dragBy(15f, 0f) } }) { Text("Right", color = CONTROL_COLOR) }
                        TextButton(onClick = { glView.performClick() }) { Text("Pulse", color = CONTROL_COLOR) }
                    }
                    Row {
                        TextButton(onClick = { glView.queueEvent { glView.renderer.zoomBy(1.15f) } }) { Text("Zoom in", color = CONTROL_COLOR) }
                        TextButton(onClick = { glView.queueEvent { glView.renderer.zoomBy(1f / 1.15f) } }) { Text("Zoom out", color = CONTROL_COLOR) }
                    }
                    TextButton(onClick = { glView.queueEvent { glView.renderer.camera.reset() } }) { Text("Reset camera", color = CONTROL_COLOR) }
                    if (genome == null) MdnaTemplatePicker(
                        label = templateLabel,
                        onPrevious = { switchTemplate(glView, viewModel, previous = true) { templateLabel = it } },
                        onNext = { switchTemplate(glView, viewModel, previous = false) { templateLabel = it } }
                    )
                    MdnaSyncControl(hub = hub, refreshKey = resumeTick)
                }
            },
            confirmButton = { TextButton(onClick = { controls = false }) { Text("Done", color = CONTROL_COLOR) } }
        )
        rendererError?.let { Text("Renderer unavailable: $it", color=Color.White, modifier=Modifier.align(Alignment.Center)) }
    }
}

private fun switchTemplate(
    glView: MdnaGlSurfaceView,
    viewModel: MdnaViewModel,
    previous: Boolean,
    onLabel: (String) -> Unit
) {
    val renderer = glView.renderer
    val choices = com.rikodev.music.android.dna.render.MdnaTemplate.entries.filterNot { it.requiresGenome }
    val index = choices.indexOf(renderer.template).coerceAtLeast(0)
    val next = choices[(index + (if (previous) choices.size - 1 else 1)) % choices.size]
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
        TextButton(onClick = onPrevious, modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Previous visual" }) {
            Text("‹", color = CONTROL_COLOR, fontSize = 22.sp)
        }
        Text(text = label, color = CONTROL_COLOR, fontSize = 13.sp)
        TextButton(onClick = onNext, modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Next visual" }) {
            Text("›", color = CONTROL_COLOR, fontSize = 22.sp)
        }
    }
}

/** − / + 10 ms nudge for the A/V offset of the CURRENT output (speaker vs Bluetooth). */
@Composable
internal fun MdnaSyncControl(hub: MdnaSyncHub, refreshKey: Int, modifier: Modifier = Modifier,
                             contentColor: Color = CONTROL_COLOR) {
    var localTick by remember { mutableIntStateOf(0) }
    val offset = remember(refreshKey, localTick) { hub.offsetMs }
    val output = remember(refreshKey, localTick) { hub.outputLabel }
    val sign = if (offset > 0) "+" else ""
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { hub.setOffsetMs(hub.offsetMs - STEP_MS); localTick++ },
            modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Decrease audio visual offset by 10 milliseconds" }) {
            Text("−", color = contentColor, fontSize = 18.sp)
        }
        Text(
            text = "Sync $sign$offset ms · $output",
            color = contentColor,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = { hub.setOffsetMs(hub.offsetMs + STEP_MS); localTick++ },
            modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Increase audio visual offset by 10 milliseconds" }) {
            Text("+", color = contentColor, fontSize = 18.sp)
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
