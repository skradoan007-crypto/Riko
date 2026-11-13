package com.rikodev.music.android.dna.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rikodev.music.android.dna.data.DnaProfileResult
import com.rikodev.music.android.dna.genome.DnaGenome
import com.rikodev.music.android.domain.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Canonical song surface now uses independent 2D points from the live PCM tap. */
@Suppress("UNUSED_PARAMETER")
@Composable
fun DnaOrganism(genome: DnaGenome?, playing: Boolean, modifier: Modifier = Modifier, generation: Int = 0) {
    LiveDotHost(playing = playing, modifier = modifier, genome = genome)
}

@Composable
fun DnaLabScreen(song: Song?, playing: Boolean, onClose: () -> Unit, onTogglePlayback: () -> Unit,
                 viewModel: DnaLabViewModel = hiltViewModel()) {
    var genome by remember(song?.uri, song?.dateModified) { mutableStateOf<DnaGenome?>(null) }
    var tools by remember(song?.uri, song?.dateModified) { mutableStateOf(false) }
    BackHandler(onBack = onClose)
    Column(Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClose, modifier = Modifier.heightIn(min = 48.dp)) { Text("Back") }
            Text("MDNA", color = Color.White, style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
            TextButton(onClick = { tools = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text("DNA tools") }
        }
        Text(song?.title ?: "Choose a song to begin", color = Color.White, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        DnaOrganism(genome, playing, Modifier.fillMaxWidth().weight(1f))
        TextButton(onClick = onTogglePlayback, enabled = song != null,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text(if (playing) "Pause playback" else "Play", color = Color.White)
        }
    }
    // Merely opening MDNA does not launch offline decoding, database reads or jobs.
    if (tools) DnaToolsPanel(song, genome, viewModel, onGenome = { genome = it }, onClose = { tools = false })
}

@Composable
private fun DnaToolsPanel(song: Song?, genome: DnaGenome?, viewModel: DnaLabViewModel,
                          onGenome: (DnaGenome) -> Unit, onClose: () -> Unit) {
    val scope = rememberCoroutineScope() // Closing tools cancels its foreground analysis/export.
    val context = LocalContext.current
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Optional: analyze a song to store its DNA and enable timeline export.") }
    var diagnostic by remember { mutableStateOf<String?>(null) }
    var libraryControls by remember { mutableStateOf(false) }
    var libraryStatus by remember { mutableStateOf("Runs while charging, one song at a time.") }
    AlertDialog(onDismissRequest = onClose, title = { Text("Song DNA tools") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(status, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            diagnostic?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }
            TextButton(enabled = song != null && !busy, onClick = {
                val selected = song ?: return@TextButton
                scope.launch {
                    busy = true; diagnostic = null; status = "Analyzing song… Live dots keep using playback audio."
                    try {
                        when (val result = viewModel.load(selected)) {
                            is DnaProfileResult.Ready -> {
                                onGenome(result.genome)
                                val generation = viewModel.generation(selected)
                                status = "DNA v${result.genome.algorithmVersion.id} · Generation $generation · Saved"
                            }
                            is DnaProfileResult.Unavailable -> {
                                status = "DNA not saved (${result.reason.name}). ${result.reason.description} You can retry here."
                                diagnostic = result.diagnostic
                            }
                        }
                    } catch (cancel: CancellationException) { throw cancel }
                    catch (_: Exception) { status = "Song DNA storage unavailable. Live dots remain available. Try again later." }
                    finally { busy = false }
                }
            }) { Text(if (busy) "Analyzing…" else "Analyze / retry song") }
            DnaSharePanel(genome, song, viewModel, onGenome)
            TextButton(onClick = { libraryControls = true }) { Text("Library analysis") }
            Text("Live visualization does not need a saved DNA profile.", style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { TextButton(onClick = onClose) { Text(if (busy) "Cancel and close" else "Done") } })
    if (libraryControls) AlertDialog(onDismissRequest = { libraryControls = false },
        title = { Text("Background DNA analysis") }, text = { Text(libraryStatus) },
        confirmButton = { TextButton(onClick = {
            libraryStatus = try {
                if (com.rikodev.music.android.dna.data.DnaAnalysisJob.schedule(context) == android.app.job.JobScheduler.RESULT_SUCCESS)
                    "Scheduled. Songs are analyzed one at a time while charging."
                else "Could not schedule analysis. Try again later."
            } catch (_: Exception) { "Background analysis unavailable on this device." }
        }) { Text("Start") } },
        dismissButton = { Row {
            TextButton(onClick = {
                com.rikodev.music.android.dna.data.DnaAnalysisJob.cancel(context)
                libraryStatus = "Background analysis stopped. Progress is saved."
            }) { Text("Stop") }
            TextButton(onClick = { libraryControls = false }) { Text("Close") }
        } })
}
