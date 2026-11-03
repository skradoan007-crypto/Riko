package com.rikodev.ibd3d.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rikodev.ibd3d.android.ui.components.CheatCard
import com.rikodev.ibd3d.core.data.*
import com.rikodev.ibd3d.core.model.*

@Composable
fun PageTitle(
    title: String,
    onBack: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null)
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        action?.invoke()
    }
}

@Composable
fun EmptyState(title: String, body: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Outlined.Search,
            null,
            Modifier.size(44.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (action != null) OutlinedButton(onClick = onAction) { Text(action) }
    }
}

@Composable
fun CodesScreen(
    data: CatalogData?,
    prefs: UserPreferences,
    query: String,
    category: String,
    onQuery: (String) -> Unit,
    onCategory: (String) -> Unit,
    onCopy: (Cheat) -> Boolean,
    onFavorite: (String) -> Unit,
    onOpen: (Cheat) -> Unit,
    error: String?,
    onRetry: () -> Unit,
) {
    val focus = LocalFocusManager.current
    val rows =
        remember(data, query, category) {
            data?.let { filterCodes(it.items, it.groups, query, category) }.orEmpty()
        }
    Column(Modifier.fillMaxSize()) {
        PageTitle("Cheat Codes")
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            label = { Text("Search codes") },
            placeholder = { Text("Item name or exact code") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty())
                    IconButton(onClick = { onQuery("") }) {
                        Icon(Icons.Outlined.Close, "Clear search")
                    }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                if (category == "Vehicles")
                    listOf("All", "Vehicles", "Bike", "Car", "Animal", "Other")
                else listOf("All", "Bike", "Car", "Animal", "Other")
            ) { value ->
                FilterChip(
                    modifier = Modifier.heightIn(min = 48.dp),
                    selected = category == value,
                    onClick = { onCategory(value) },
                    label = { Text(value) },
                )
            }
        }
        Text(
            "${rows.size} codes • available offline",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
        )
        when {
            error != null -> EmptyState("Catalog unavailable", error, "Retry", onRetry)
            data == null ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            rows.isEmpty() ->
                EmptyState(
                    "No matching codes",
                    "Try another name or code.",
                    "Reset filters",
                    {
                        onQuery("")
                        onCategory("All")
                    },
                )
            else -> {
                val state = rememberLazyListState()
                var previous by remember { mutableStateOf(query to category) }
                LaunchedEffect(query, category) {
                    if (previous != (query to category)) {
                        state.scrollToItem(0)
                        previous = query to category
                    }
                }
                LazyColumn(
                    state = state,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(rows, key = { it.id }, contentType = { "cheat" }) { item ->
                        CheatCard(
                            item,
                            item.id in prefs.favorites,
                            { onFavorite(item.id) },
                            { onCopy(item) },
                            { onOpen(item) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SavedScreen(
    title: String,
    rows: List<Cheat>,
    favorites: Set<String>,
    onCopy: (Cheat) -> Boolean,
    onFavorite: (String) -> Unit,
    onOpen: (Cheat) -> Unit,
    onBack: () -> Unit,
    onBrowse: () -> Unit,
    onClear: (() -> Unit)? = null,
) {
    var confirm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        PageTitle(title, onBack) {
            if (onClear != null && rows.isNotEmpty())
                IconButton(onClick = { confirm = true }) {
                    Icon(Icons.Outlined.DeleteOutline, "Clear recent copies")
                }
        }
        if (rows.isEmpty())
            EmptyState(
                "Nothing here yet",
                if (title == "Favorites") "Tap a heart to save a code here."
                else "Codes you copy will appear here.",
                "Browse codes",
                onBrowse,
            )
        else
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(rows, key = { it.id }) { item ->
                    CheatCard(
                        item,
                        item.id in favorites,
                        { onFavorite(item.id) },
                        { onCopy(item) },
                        { onOpen(item) },
                    )
                }
            }
    }
    if (confirm)
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Clear recent copies?") },
            text = { Text("Your favorites will stay saved.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClear?.invoke()
                        confirm = false
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } },
        )
}
