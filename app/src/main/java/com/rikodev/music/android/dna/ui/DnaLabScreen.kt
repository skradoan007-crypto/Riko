package com.rikodev.music.android.dna.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
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
    val sync: com.rikodev.music.android.dna.sync.MdnaViewModel = hiltViewModel()
    DisposableEffect(sync) { onDispose { sync.hub.timelineInput = null } }
    var generation by remember(song?.uri) { mutableIntStateOf(0) }
    var libraryControls by remember { mutableStateOf(false) }
    var libraryStatus by remember { mutableStateOf("Analysis runs while charging and battery/storage conditions allow.") }
    val context = androidx.compose.ui.platform.LocalContext.current
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(song, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            sync.hub.timelineInput = null
            if (song == null) status = "Choose a song to create its DNA"
            else try {
                status = "Analyzing offline · live organism available"
                genome = viewModel.genome(song)
                val timeline = viewModel.timeline(song)
                if (timeline != null) sync.hub.timelineInput = com.rikodev.music.android.dna.sync.MdnaSyncHub.TimelineInput(song.id, timeline)
                status = genome?.let { "${it.archetype.name.lowercase().replaceFirstChar { c -> c.uppercase() }} · DNA v${it.algorithmVersion.id}" }
                    ?: "Analysis unavailable · live organism only"
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { status = "Profile unavailable · live organism only" }
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
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
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
