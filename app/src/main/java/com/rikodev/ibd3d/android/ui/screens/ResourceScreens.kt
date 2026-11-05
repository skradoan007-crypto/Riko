package com.rikodev.ibd3d.android.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rikodev.ibd3d.android.files.ModelFiles
import com.rikodev.ibd3d.android.ui.components.CheatCard
import com.rikodev.ibd3d.core.data.*
import com.rikodev.ibd3d.core.model.Cheat
import java.io.File
import kotlinx.coroutines.*

@Composable fun ToolsHub(onNavigate: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageTitle("Tools & Plugins") }
        items(listOf("rgs" to "RGS Tools", "plugin_list" to "Plugins", "loads" to "RGS Load Files", "custom" to "Custom RGS File Loader"), key = { it.first }) { (route, title) ->
            OutlinedCard(onClick = { onNavigate(route) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text(title, style = MaterialTheme.typography.titleLarge)
                    Text(when(route) { "rgs" -> "Commands, buildings, characters & props"; "plugin_list" -> "Browse and copy plugin links"; "loads" -> "23 bundled models • offline"; else -> "Choose a model from your phone" })
                }
            }
        }
    }
}

@Composable fun ResourceScreen(section: String, title: String, data: CatalogData?, prefs: UserPreferences, onCopy: (Cheat) -> Boolean, onFavorite: (String) -> Unit, onOpen: (Cheat) -> Unit, onBack: () -> Unit, error: String? = null, onRetry: () -> Unit = {}) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("All") }
    val available = remember(data, section) { data?.items.orEmpty().filter { it.isBrowseable && it.section == section } }
    val categories = remember(available) { listOf("All") + available.map { it.category }.distinct().sorted() }
    val rows = remember(available, query, category) { available.filter { (category == "All" || it.category == category) && (it.name.contains(query.trim(), true) || it.code.contains(query.trim(), true)) } }
    val list = rememberLazyListState()
    LaunchedEffect(query, category) { list.scrollToItem(0) }
    Column {
        PageTitle(title, onBack)
        OutlinedTextField(query, { query = it }, label = { Text("Search names or codes") }, singleLine = true, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        LazyRow(contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories, key = { it }) { value -> FilterChip(category == value, { category = value }, label = { Text(value) }) }
        }
        if (section == "plugins") Text("Previews and links are stored offline. The game needs internet to fetch a plugin.", Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall)
        if (section == "loads") Text("Tap a model to load or save it. Bundled files also appear in Android’s document picker under IBD3D Panel.", Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall)
        if (error != null) { Text(error, Modifier.padding(16.dp)); TextButton(onClick = onRetry) { Text("Retry") } }
        else if (data == null) CircularProgressIndicator(Modifier.padding(24.dp))
        else if (rows.isEmpty()) EmptyState("No matching items", "Try another search or category.")
        LazyColumn(state = list, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(rows, key = { it.id }, contentType = { "resource" }) { row -> CheatCard(row, row.id in prefs.favorites, { onFavorite(row.id) }, { onCopy(row) }, { onOpen(row) }, Modifier.animateItem()) }
        }
    }
}

@Composable fun ModelActions(asset: String? = null, local: File? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var path by rememberSaveable(asset, local?.path) { mutableStateOf<String?>(null) }
    var fallback by remember { mutableStateOf(false) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val file = path?.let(::File)
        if (uri != null && file != null) scope.launch {
            busy = true
            try { ModelFiles.export(context, file, uri); message = "Saved. In the game: phone → RGS → Load → select this file." }
            catch (e: Exception) { if (e is CancellationException) throw e; message = "Could not save. Try another location." }
            finally { busy = false }
        }
    }
    fun prepare(action: (File) -> Unit) {
        scope.launch {
            busy = true
            try { val file = local ?: ModelFiles.bundled(context, requireNotNull(asset)); path = file.path; action(file) }
            catch (e: Exception) { if (e is CancellationException) throw e; message = e.message ?: "Could not prepare model." }
            finally { busy = false }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(enabled = !busy, onClick = { prepare { if (!ModelFiles.loadInGame(context, it)) fallback = true } }) { Text("Load") }
        OutlinedButton(enabled = !busy, onClick = { prepare { save.launch(ModelFiles.displayName(it)) } }) { Text("Save file") }
    }
    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodySmall)
    if (fallback) AlertDialog(onDismissRequest = { fallback = false }, title = { Text("Use the game’s RGS loader") }, text = { Text("This installed game does not advertise a compatible file-opening action. Open its phone → RGS → Load. In Android’s file picker, choose IBD3D Panel, or save the file and select it from Downloads. Direct automatic import is not available through a verified game interface.") }, confirmButton = { TextButton(onClick = { fallback = false; path?.let { save.launch(ModelFiles.displayName(File(it))) } }) { Text("Save file") } }, dismissButton = { TextButton(onClick = { fallback = false }) { Text("Close") } })
}

@Composable fun CustomLoaderScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    suspend fun refresh() { files = withContext(Dispatchers.IO) { File(context.filesDir, "custom_models").listFiles()?.filter { it.extension == "glb" }?.sortedByDescending { it.lastModified() }.orEmpty() } }
    LaunchedEffect(Unit) { refresh() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try { val file = ModelFiles.importModel(context, uri); message = "Imported ${withContext(Dispatchers.IO) { ModelFiles.format(file) }}"; refresh() }
            catch (e: Exception) { if (e is CancellationException) throw e; message = e.message ?: "Import failed." }
            finally { busy = false }
        }
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageTitle("Custom RGS File Loader", onBack) }
        item { Text("Import a GLB 2 or UnityFS AssetBundle you have permission to use. Files stay on this device. Header validation does not guarantee game compatibility.") }
        item { Button(enabled = !busy, onClick = { picker.launch(arrayOf("*/*")) }) { Text("Choose model") } }
        item { if (busy) LinearProgressIndicator(Modifier.fillMaxWidth()); if (message.isNotBlank()) Text(message) }
        items(files, key = { it.name }, contentType = { "custom" }) { file ->
            OutlinedCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(ModelFiles.displayName(file), style = MaterialTheme.typography.titleSmall)
                ModelActions(local = file)
                TextButton(onClick = { scope.launch { withContext(Dispatchers.IO) { file.delete() }; refresh() } }) { Text("Remove local copy") }
            } }
        }
    }
}
