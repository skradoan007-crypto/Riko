package com.rikodev.music.android.dna.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rikodev.music.android.dna.analysis.DnaAnalysisFailure
import com.rikodev.music.android.dna.data.DnaProfileResult
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.rikodev.music.android.dna.genome.DnaGenome
import com.rikodev.music.android.domain.model.Song
import kotlinx.coroutines.CancellationException

@Composable
fun DnaOrganism(genome: DnaGenome?, playing: Boolean, modifier: Modifier = Modifier, generation: Int = 0) {
    MdnaVisualizerHost(modifier = modifier, genome = genome, playing = playing, generation = generation)
}

@Composable
fun DnaLabScreen(song: Song?, playing: Boolean, onClose: () -> Unit, onTogglePlayback: () -> Unit,
                 viewModel: DnaLabViewModel = hiltViewModel()) {
    var genome by remember(song?.uri, song?.dateModified) { mutableStateOf<DnaGenome?>(null) }
    var status by remember(song?.uri, song?.dateModified) { mutableStateOf("Preparing song DNA…") }
    var failure by remember(song?.uri, song?.dateModified) { mutableStateOf<DnaAnalysisFailure?>(null) }
    var diagnostic by remember(song?.uri, song?.dateModified) { mutableStateOf<String?>(null) }
    var retry by remember(song?.uri, song?.dateModified) { mutableIntStateOf(0) }
    var showFailure by remember(song?.uri, song?.dateModified) { mutableStateOf(false) }
    val sync: com.rikodev.music.android.dna.sync.MdnaViewModel = hiltViewModel()
    DisposableEffect(sync) { onDispose { sync.hub.timelineInput = null } }
    var generation by remember(song?.uri) { mutableIntStateOf(0) }
    var libraryControls by remember { mutableStateOf(false) }
    var libraryStatus by remember { mutableStateOf("Analysis runs while charging and battery/storage conditions allow.") }
    val context = androidx.compose.ui.platform.LocalContext.current
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(song, owner, retry) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            sync.hub.timelineInput = null
            failure = null
            diagnostic = null
            if (song == null) status = "Choose a song to create its DNA"
            else {
                status = "Analyzing offline · live organism available"
                when (val result = viewModel.load(song)) {
                    is DnaProfileResult.Ready -> {
                        genome = result.genome
                        sync.hub.timelineInput = com.rikodev.music.android.dna.sync.MdnaSyncHub.TimelineInput(song.id, result.timeline)
                        status = "${result.genome.archetype.name.lowercase().replaceFirstChar { it.uppercase() }} · DNA v${result.genome.algorithmVersion.id}"
                    }
                    is DnaProfileResult.Unavailable -> {
                        failure = result.reason
                        diagnostic = result.diagnostic
                        status = "Live only · DNA analysis failed (${result.reason.name})"
                    }
                }
            }
        }
    }
    LaunchedEffect(song, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                if (song != null) try { generation = viewModel.generation(song) }
                catch (cancel: CancellationException) { throw cancel }
                catch (_: Exception) { /* Keep last known generation on storage failure. */ }
                kotlinx.coroutines.delay(5000)
            }
        }
    }
    BackHandler(onBack = onClose)
    Column(Modifier.fillMaxSize().background(Color(0xFF080712)).safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp)) {
            TextButton(onClick = onClose, modifier = Modifier.heightIn(min = 48.dp)) { Text("Back") }
            Text("MDNA Genesis", style = MaterialTheme.typography.titleLarge,
                color = Color.White, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.padding(12.dp))
        }
        Text(song?.title ?: "No song selected", color = Color.White, maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal=16.dp))
        Text("$status · Generation $generation", color = Color(0xFFC7BDE8), maxLines = 2,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics { liveRegion = LiveRegionMode.Polite })
        if (failure != null) Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            TextButton(onClick = { showFailure = true }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Why analysis failed") }
            TextButton(onClick = { failure = null; retry++ }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Retry analysis") }
        }
        Row {
            DnaSharePanel(genome, song, viewModel) { genome = it }
            TextButton(onClick = { libraryControls = true }) { Text("Library analysis") }
        }
        DnaOrganism(genome, playing, Modifier.fillMaxWidth().weight(1f), generation)
        TextButton(onClick = onTogglePlayback, enabled = song != null,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(if (playing) "Pause playback" else "Play", color = Color.White)
        }
    }
    if (showFailure && failure != null) AlertDialog(onDismissRequest = { showFailure = false },
        title = { Text("DNA analysis unavailable") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("${failure?.description}\n\nCode: ${failure?.name}\nLive playback visualization remains available. No complete DNA result was produced by this attempt.")
            diagnostic?.let { Text(it, modifier = Modifier.padding(top = 12.dp)) }
        } },
        confirmButton = { TextButton(onClick = { showFailure = false; failure = null; retry++ }) { Text("Retry analysis") } },
        dismissButton = { TextButton(onClick = { showFailure = false }) { Text("Close") } })
    if (libraryControls) AlertDialog(onDismissRequest = { libraryControls = false },
        title = { Text("Background DNA analysis") },
        text = { Text(libraryStatus) },
        confirmButton = { TextButton(onClick = {
            libraryStatus = try {
                if (com.rikodev.music.android.dna.data.DnaAnalysisJob.schedule(context) == android.app.job.JobScheduler.RESULT_SUCCESS)
                    "Scheduled. Analysis resumes while charging; songs are processed one at a time."
                else "Could not schedule analysis. Try again later."
            } catch (_: Exception) { "Background analysis unavailable on this device." }
        }) { Text("Start") } },
        dismissButton = { TextButton(onClick = {
            com.rikodev.music.android.dna.data.DnaAnalysisJob.cancel(context)
            libraryStatus = "Background analysis stopped. Progress is saved for next time."
        }) { Text("Stop") } })

}
