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
fun DnaOrganism(genome: DnaGenome?, playing: Boolean, modifier: Modifier = Modifier) {
    MdnaVisualizerHost(modifier = modifier, genome = genome, playing = playing)
}

@Composable
fun DnaLabScreen(song: Song?, playing: Boolean, onClose: () -> Unit, onTogglePlayback: () -> Unit,
                 viewModel: DnaLabViewModel = hiltViewModel()) {
    var genome by remember(song?.uri, song?.dateModified) { mutableStateOf<DnaGenome?>(null) }
    var status by remember(song?.uri, song?.dateModified) { mutableStateOf("Preparing song DNA…") }
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(song, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            if (song == null) status = "Choose a song to create its DNA"
            else try {
                status = "Analyzing offline · live organism available"
                genome = viewModel.genome(song)
                status = genome?.let { "${it.archetype.name.lowercase().replaceFirstChar { c -> c.uppercase() }} · DNA v${it.algorithmVersion.id}" }
                    ?: "Analysis unavailable · live organism only"
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { status = "Profile unavailable · live organism only" }
        }
    }
    BackHandler(onBack = onClose)
    Column(Modifier.fillMaxSize().background(Color(0xFF080712)).safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp)) {
            TextButton(onClick = onClose, modifier = Modifier.heightIn(min = 48.dp)) { Text("Back") }
            Text("MDNA Genesis", style = MaterialTheme.typography.titleLarge,
                color = Color.White, modifier = Modifier.padding(12.dp))
        }
        Text(song?.title ?: "No song selected", color = Color.White, maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal=16.dp))
        Text(status, color = Color(0xFFC7BDE8), modifier = Modifier.padding(16.dp))
        DnaOrganism(genome, playing, Modifier.fillMaxWidth().weight(1f))
        TextButton(onClick = onTogglePlayback, enabled = song != null,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(if (playing) "Pause playback" else "Play", color = Color.White)
        }
    }
}
