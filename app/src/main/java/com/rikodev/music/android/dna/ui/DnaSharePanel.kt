package com.rikodev.music.android.dna.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.rikodev.music.android.dna.genome.*
import com.rikodev.music.android.dna.export.DnaExporter
import com.rikodev.music.android.domain.model.Song
import kotlinx.coroutines.*

@Composable
fun DnaSharePanel(genome: DnaGenome?, song: Song?, viewModel: DnaLabViewModel, onImport: (DnaGenome) -> Unit) {
    var open by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var percent by remember { mutableIntStateOf(0) }
    var task by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    fun action(body: () -> Unit) { try { body() } catch (_: Exception) { message = "Invalid or unsupported DNA code" } }
    fun export(video: Boolean) {
        val dna = genome ?: return
        task = scope.launch {
            try {
                message = "Exporting…"; percent = 0
                val timeline = if (video) song?.let { viewModel.timeline(it) } else null
                val file = DnaExporter.export(context, dna, timeline, video) { value -> percent = value }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = if(video) "video/mp4" else "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri); clipData = ClipData.newRawUri("RIKO DNA", uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share Music DNA"))
                message = "Export ready"
            } catch (cancel: CancellationException) { message = "Export cancelled"; throw cancel }
            catch (_: Exception) { message = "Export unavailable. Try PNG, or finish song analysis before video export." }
            finally { task = null }
        }
    }
    TextButton(onClick = { open = true }) { Text("Share / Import DNA") }
    if (open) AlertDialog(onDismissRequest = { task?.cancel(); open = false },
        title = { Text("Music DNA") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = code, onValueChange = { code = it.take(180) }, label = { Text("RIKO1 DNA code") })
                Row {
                    TextButton(enabled = genome != null && task == null, onClick = { action {
                        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                            .setPrimaryClip(ClipData.newPlainText("Music DNA", DnaCode.encode(genome!!)))
                        message = "DNA code copied"
                    } }) { Text("Copy") }
                    TextButton(enabled = task == null, onClick = { action { onImport(DnaCode.decode(code)); message = "Imported preview" } }) { Text("Import") }
                }
                Row {
                    TextButton(enabled = genome != null && task == null, onClick = { action {
                        message = "Visual trait distance: ${(DnaCode.distance(genome!!, DnaCode.decode(code))*100).toInt()}%"
                    } }) { Text("Compare") }
                    TextButton(enabled = genome != null && task == null, onClick = { action {
                        onImport(DnaCode.breed(genome!!, DnaCode.decode(code))); message = "Offspring preview ready"
                    } }) { Text("Breed") }
                }
                Row {
                    TextButton(enabled = genome != null && task == null, onClick = { export(false) }) { Text("PNG card") }
                    TextButton(enabled = genome != null && song != null && task == null, onClick = { export(true) }) { Text("10s MP4") }
                }
                Text(if (task != null) "$message $percent%" else message)
                if (task != null) TextButton(onClick = { task?.cancel() }) { Text("Cancel export") }
            }
        }, confirmButton = { TextButton(onClick = { task?.cancel(); open = false }) { Text("Close") } })
}
