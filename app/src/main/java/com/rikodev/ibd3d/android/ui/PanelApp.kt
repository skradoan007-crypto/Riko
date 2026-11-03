package com.rikodev.ibd3d.android.ui

import android.content.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import com.rikodev.ibd3d.android.ui.components.CheatImage
import com.rikodev.ibd3d.android.ui.screens.*
import com.rikodev.ibd3d.android.updates.PlayUpdates
import com.rikodev.ibd3d.core.designsystem.PanelTheme
import com.rikodev.ibd3d.core.model.Cheat
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelApp(updates: PlayUpdates, vm: PanelViewModel = hiltViewModel()) {
    val prefs by vm.preferences.collectAsStateWithLifecycle()
    val data by vm.catalog.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val category by vm.category.collectAsStateWithLifecycle()
    val update by updates.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val stack by nav.currentBackStackEntryAsState()
    val route = stack?.destination?.route ?: "home"
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = data?.items?.find { it.id == selectedId }
    fun message(text: String) {
        scope.launch {
            snack.currentSnackbarData?.dismiss()
            snack.showSnackbar(text)
        }
    }
    val copy: (Cheat) -> Boolean = { item ->
        val ok =
            runCatching {
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(ClipData.newPlainText(item.name, item.code))
                }
                .isSuccess
        if (ok) {
            vm.copied(item.id)
            if (prefs.copyFeedback) message("Copied")
        } else message("Could not copy. Please retry.")
        ok
    }
    fun tab(target: String) {
        nav.navigate(target) {
            popUpTo(nav.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    fun codes(filter: String) {
        vm.category(filter)
        vm.query("")
        tab("codes")
    }
    LaunchedEffect(vm) { vm.messages.collect { message(it) } }
    PanelTheme(prefs.accent) {
        Scaffold(
            snackbarHost = { SnackbarHost(snack) },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    listOf(
                            Triple("home", "Home", Icons.Outlined.Home),
                            Triple("codes", "Codes", Icons.Outlined.GridView),
                            Triple("plugins", "Plugins", Icons.Outlined.Extension),
                            Triple("more", "More", Icons.Outlined.MoreHoriz),
                        )
                        .forEach { (target, label, icon) ->
                            NavigationBarItem(
                                selected =
                                    route == target ||
                                        (target == "more" &&
                                            route in listOf("favorites", "recents", "about")),
                                onClick = { tab(target) },
                                icon = { Icon(icon, null) },
                                label = { Text(label) },
                            )
                        }
                }
            },
        ) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (update.downloaded)
                    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Update ready to install", Modifier.weight(1f))
                            TextButton(onClick = updates::complete) { Text("Restart") }
                        }
                    }
                NavHost(
                    nav,
                    startDestination = "home",
                    modifier = Modifier.weight(1f).widthIn(max = 720.dp).fillMaxWidth(),
                ) {
                    composable("home") {
                        HomeScreen(
                            data,
                            prefs,
                            ::codes,
                            { tab("plugins") },
                            { nav.navigate("favorites") },
                            { nav.navigate("recents") },
                            copy,
                            vm::favorite,
                            { selectedId = it.id },
                            error,
                            vm::load,
                        )
                    }
                    composable("codes") {
                        CodesScreen(
                            data,
                            prefs,
                            query,
                            category,
                            vm::query,
                            vm::category,
                            copy,
                            vm::favorite,
                            { selectedId = it.id },
                            error,
                            vm::load,
                        )
                    }
                    composable("favorites") {
                        SavedScreen(
                            "Favorites",
                            data?.items?.filter { it.id in prefs.favorites }.orEmpty(),
                            prefs.favorites,
                            copy,
                            vm::favorite,
                            { selectedId = it.id },
                            { nav.popBackStack() },
                            { codes("All") },
                        )
                    }
                    composable("recents") {
                        SavedScreen(
                            "Recent copies",
                            prefs.recent.mapNotNull { r -> data?.items?.find { it.id == r.id } },
                            prefs.favorites,
                            copy,
                            vm::favorite,
                            { selectedId = it.id },
                            { nav.popBackStack() },
                            { codes("All") },
                            vm::clear,
                        )
                    }
                    composable("plugins") { PluginsScreen() }
                    composable("more") {
                        SettingsScreen(
                            prefs,
                            update,
                            updates::check,
                            updates::complete,
                            vm::feedback,
                            vm::accent,
                            { nav.navigate("favorites") },
                            { nav.navigate("recents") },
                            { nav.navigate("about") },
                        )
                    }
                    composable("about") { AboutScreen { nav.popBackStack() } }
                }
            }
        }
        if (selected != null)
            ModalBottomSheet(onDismissRequest = { selectedId = null }) {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = { selectedId = null }) {
                            Icon(Icons.Outlined.Close, "Close details")
                        }
                    }
                    CheatImage(selected, Modifier.fillMaxWidth().height(190.dp))
                    Text(selected.name, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        selected.code,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "${selected.badge} • ${if(selected.inputMethod=="rgs_tool")"RGS Tool command" else "In-game phone dialer"}"
                    )
                    Text(selected.category)
                    if (selected.notes.isNotBlank()) Text(selected.notes)
                    if (selected.imageStatus == "unconfirmed_match")
                        Text("Image association is unconfirmed.")
                    if (selected.status != "official")
                        Text(
                            "Community and experimental entries may not work in your game version."
                        )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { copy(selected) }, modifier = Modifier.weight(1f)) {
                            Text("Copy code")
                        }
                        OutlinedButton(
                            onClick = { vm.favorite(selected.id) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (selected.id in prefs.favorites) "Unfavorite" else "Favorite")
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
    }
}
