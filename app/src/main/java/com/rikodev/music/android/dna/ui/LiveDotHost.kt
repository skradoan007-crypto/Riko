package com.rikodev.music.android.dna.ui

import android.app.ActivityManager
import android.content.Context
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.repeatOnLifecycle
import com.rikodev.music.android.dna.genome.DnaGenome
import com.rikodev.music.android.dna.render.LiveDotSurfaceView
import com.rikodev.music.android.dna.render.MdnaQuality
import com.rikodev.music.android.dna.sync.MdnaViewModel
import kotlinx.coroutines.delay

@Composable
fun LiveDotHost(playing: Boolean, modifier: Modifier = Modifier, genome: DnaGenome? = null,
                viewModel: MdnaViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val hub = viewModel.hub
    val view = remember(context, hub) {
        @Suppress("DEPRECATION")
        val refresh = (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
            ?.defaultDisplay?.refreshRate ?: 60f
        LiveDotSurfaceView(context, hub.liveOnlySource, refresh).also {
            if ((context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.isLowRamDevice == true)
                it.renderer.quality = MdnaQuality.LOW
        }
    }
    var frozen by remember { mutableStateOf(!android.animation.ValueAnimator.areAnimatorsEnabled()) }
    var controls by remember { mutableStateOf(false) }
    var palette by remember { mutableIntStateOf(0) }
    var sensitivity by remember { mutableFloatStateOf(1.5f) }
    var automatic by remember { mutableStateOf(true) }
    var quality by remember { mutableStateOf(view.renderer.quality) }
    var signal by remember { mutableStateOf("Waiting for audio") }
    var timing by remember { mutableStateOf("Measuring frames…") }
    var levels by remember { mutableStateOf("") }
    var rendererError by remember { mutableStateOf<String?>(null) }
    var syncTick by remember { mutableIntStateOf(0) }
    SideEffect {
        view.renderer.playing = playing
        view.renderer.frozen = frozen
        view.renderer.palette = palette.toFloat()
        view.renderer.sensitivity = sensitivity
        view.renderer.autoQuality = automatic
        view.renderer.seed = genome?.let { ((it.seed xor (it.seed ushr 32)) and 65535L) / 65535f } ?: 0f
    }
    DisposableEffect(owner, view, hub) {
        var attached = false
        fun resume() {
            if (attached) return
            hub.attach(); view.onResume(); attached = true; syncTick++
        }
        fun pause() {
            if (!attached) return
            view.onPause(); hub.detach(); attached = false
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> resume()
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_DESTROY -> pause()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) resume()
        onDispose { owner.lifecycle.removeObserver(observer); pause() }
    }
    LaunchedEffect(owner, view) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val r = view.renderer
                r.sampleRateHz = hub.sampleRate.takeIf { it > 0 } ?: 44100
                quality = r.quality
                rendererError = r.error
                signal = when {
                    !hub.playing -> "Paused"
                    !r.hasSignal -> "Waiting for audio"
                    r.frozen -> "Motion paused"
                    else -> "LIVE · DOT SPECTRUM"
                }
                timing = if (r.frameMs > 0f) "${(1000f / r.frameMs).toInt()} FPS · ${"%.1f".format(r.frameMs)} ms" else "Measuring frames…"
                levels = "Bass ${(r.bass * 100).toInt()} · Mid ${(r.mid * 100).toInt()} · High ${(r.treble * 100).toInt()}"
                delay(500)
            }
        }
    }
    Column(modifier.background(Color.Black)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(signal, color = Color(0xFFE2E6BD), style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f))
            TextButton(onClick = { controls = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Controls") }
        }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            AndroidView(factory = { view }, modifier = Modifier.fillMaxSize())
            if (rendererError != null) Text("Dot rendering unavailable on this GPU. Playback continues.",
                color = Color.White, modifier = Modifier.align(Alignment.Center).padding(24.dp))
        }
    }
    if (controls) AlertDialog(onDismissRequest = { controls = false }, title = { Text("Live dot spectrum") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(timing)
            Text(levels, modifier = Modifier.padding(vertical = 8.dp))
            Text("Mid includes voices and instruments; it is not isolated vocals.", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { frozen = !frozen }) { Text(if (frozen) "Resume motion" else "Pause motion") }
            TextButton(onClick = { palette = 1 - palette }) { Text("Colour: ${if (palette == 0) "Lemon light" else "Cyan / violet"}") }
            Text("Sensitivity ${"%.1f".format(sensitivity)}×")
            Slider(value = sensitivity, onValueChange = { sensitivity = it }, valueRange = .5f..3f,
                modifier = Modifier.semantics {
                    contentDescription = "Spectrum sensitivity"
                    stateDescription = "${"%.1f".format(sensitivity)} times"
                })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Adaptive quality", Modifier.weight(1f))
                Switch(checked = automatic, onCheckedChange = { automatic = it },
                    modifier = Modifier.semantics { contentDescription = "Adaptive quality" })
            }
            TextButton(onClick = { quality = quality.next(); view.renderer.quality = quality }) { Text("Detail: ${quality.label}") }
            MdnaSyncControl(hub, syncTick, contentColor = MaterialTheme.colorScheme.onSurface)
            Text("Output timing is estimated. Adjust sync to match your speaker or headset.", style = MaterialTheme.typography.bodySmall)
            rendererError?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
        } }, confirmButton = { TextButton(onClick = { controls = false }) { Text("Done") } })
}
