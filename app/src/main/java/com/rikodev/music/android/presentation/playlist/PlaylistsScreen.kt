package com.rikodev.music.android.presentation.playlist
import com.rikodev.music.android.core.designsystem.riko.theme.RikoShape
import com.rikodev.music.android.core.designsystem.theme.RikoShapes
import androidx.compose.ui.platform.LocalDensity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.request.ImageRequest
import com.rikodev.music.android.core.designsystem.riko.components.RikoArtwork
import com.rikodev.music.android.core.designsystem.riko.components.RikoGlassSurface
import com.rikodev.music.android.core.designsystem.riko.components.RikoIconButton
import com.rikodev.music.android.core.designsystem.riko.components.RikoPrimaryButton
import com.rikodev.music.android.core.designsystem.riko.components.RikoSecondaryButton
import com.rikodev.music.android.core.designsystem.riko.components.RikoSortDropdownChip
import com.rikodev.music.android.core.designsystem.riko.shell.LocalRikoShellMetrics
import com.rikodev.music.android.core.designsystem.riko.theme.RikoComponentSize
import com.rikodev.music.android.core.designsystem.riko.theme.RikoCornerRadius
import com.rikodev.music.android.core.designsystem.riko.theme.RikoError
import com.rikodev.music.android.core.designsystem.riko.theme.RikoFavoriteActive
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlass
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlassBorder
import com.rikodev.music.android.core.designsystem.riko.theme.RikoIconSize
import com.rikodev.music.android.core.designsystem.riko.theme.RikoPurple
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSurface
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSurfaceElevated
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextSecondary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextTertiary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography
import com.rikodev.music.android.core.designsystem.theme.LocalDynamicColors
import com.rikodev.music.android.domain.model.Playlist
import com.rikodev.music.android.presentation.collection.components.CollectionEmptyState
import com.rikodev.music.android.presentation.collection.components.CollectionSkeleton
import com.rikodev.music.android.presentation.components.GlassBottomSheetContainer

// ---------------------------------------------------------------------------
// RIKO - PLAYLISTS SCREEN (PLAN 6 / PLAN 11 reference match)
//
// Reference layout: large Playlist title + Search action, count +
// active-sort chip strip, Add New Playlist capsule with solid purple plus,
// playlist rows with 3-dot overflow -> confirm sheet -> delete (repository).
// ---------------------------------------------------------------------------

@Composable
fun PlaylistsScreen(
    modifier: Modifier = Modifier,
    onPlaylistClick: (Long) -> Unit,
    /** After create -> navigate into the new playlist (Plan 6 34). */
    onCreated: (Long) -> Unit = {},
    /** Header Search icon -> existing Search tab/route (Plan 11 reference). */
    onSearchClick: () -> Unit = {},
    viewModel: PlaylistsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showCreateDialog by viewModel.showCreateDialog.collectAsStateWithLifecycle()
    val accentColor = LocalDynamicColors.current.vibrantColor
    val shellMetrics = LocalRikoShellMetrics.current
    val createSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val navigationBarsBottom = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }
    var menuPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var confirmDelete by remember { mutableStateOf<Playlist?>(null) }

    // Sort (screen-local; repository order untouched). G-7 seed: the chip's
    // onClick was a TODO no-op — chevron now opens a real menu (like Favorites).
    var sortMode by rememberSaveable { mutableIntStateOf(0) }
    var sortMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val sortLabels = listOf("Recently Added", "Name", "Most Songs")
    val playlists = when (sortMode) {
        1 -> uiState.playlists.sortedBy { it.name.lowercase() }
        2 -> uiState.playlists.sortedByDescending { it.songIds.size }
        else -> uiState.playlists.sortedByDescending { it.modifiedAt }
    }

    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        // Reference header: large title + Search + Heart icons.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RikoSpacing.XL, vertical = RikoSpacing.SM),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Playlists", style = RikoTypography.LargeTitle, color = RikoTextPrimary)
            Spacer(Modifier.weight(1f))
            RikoIconButton(
                icon = Icons.Rounded.Search,
                contentDescription = "Search",
                onClick = onSearchClick,
                container = false
            )
            // G-7: the heart button had an empty onClick (dead control).
            // Title + count identify the screen — dropped (as in Favorites).
        }

        // Reference count strip: "N Playlist" + active-sort chip.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RikoSpacing.XL, vertical = RikoSpacing.SM),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    uiState.isLoading -> "Loading..."
                    uiState.error -> "Unavailable"
                    playlists.size == 1 -> "1 Playlist"
                    else -> "${playlists.size} Playlists"
                },
                style = RikoTypography.Secondary,
                color = RikoTextTertiary
            )
            Spacer(Modifier.weight(1f))
            // Sort control only means something once there is data to sort.
            if (!uiState.isLoading && !uiState.error && playlists.isNotEmpty()) {
                Box {
                    RikoSortDropdownChip(
                        label = sortLabels[sortMode],
                        onClick = { sortMenuExpanded = true }
                    )
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        sortLabels.forEachIndexed { index, label ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = label,
                                        style = RikoTypography.Label,
                                        color = if (index == sortMode) {
                                            RikoTextPrimary
                                        } else {
                                            RikoTextSecondary
                                        }
                                    )
                                },
                                trailingIcon = {
                                    if (index == sortMode) {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = "Active"
                                        )
                                    }
                                },
                                onClick = {
                                    sortMode = index
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        NewPlaylistCta(accentColor = accentColor, onClick = { viewModel.openCreateDialog() })
        LazyColumn(
            contentPadding = PaddingValues(
                top = RikoSpacing.SM,
                bottom = shellMetrics.contentBottomPadding + navigationBarsBottom + RikoSpacing.XL
            )
        ) {
            // Full state machine (A13-18): loading skeleton → error → empty → list.
            when {
                uiState.isLoading -> item { CollectionSkeleton() }
                uiState.error -> item {
                    CollectionEmptyState(
                        title = "Couldn't load playlists",
                        message = "Something went wrong while loading. Try again in a moment.",
                        actionLabel = "Try again",
                        onAction = { viewModel.retry() }
                    )
                }
                playlists.isEmpty() -> item {
                    PlaylistEmptyState(onCreate = { viewModel.openCreateDialog() })
                }
                else -> {
                    items(playlists, key = { it.id }, contentType = { "playlist_card" }) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            accentColor = accentColor,
                            onClick = { onPlaylistClick(playlist.id) },
                            onMenuClick = { menuPlaylist = playlist }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissCreateDialog() },
            sheetState = createSheetState,
            containerColor = Color.Transparent,
            contentColor = RikoTextPrimary,
            dragHandle = null,
            shape = RikoShapes.TopSheet,
            modifier = Modifier.fillMaxHeight(0.5f)
        ) {
            GlassBottomSheetContainer(modifier = Modifier.fillMaxHeight(), accentColor = accentColor) {
                CreatePlaylistSheet(
                    accentColor = accentColor,
                    existingNames = playlists.map { it.name },
                    onConfirm = { name -> viewModel.createPlaylist(name, onCreated) },
                    onDismiss = { viewModel.dismissCreateDialog() }
                )
            }
        }
    }

    // Reference 3-dot overflow: delete playlist (favorites blocked in repository).
    if (menuPlaylist != null) {
        ModalBottomSheet(
            onDismissRequest = { menuPlaylist = null },
            containerColor = Color.Transparent,
            contentColor = RikoTextPrimary,
            dragHandle = null,
            shape = RikoShapes.TopSheet
        ) {
            GlassBottomSheetContainer(modifier = Modifier.fillMaxWidth(), accentColor = accentColor) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RikoSpacing.LG, vertical = RikoSpacing.LG)
                ) {
                    Text(
                        text = menuPlaylist?.name ?: "",
                        style = RikoTypography.Section,
                        color = RikoTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(RikoSpacing.LG))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RikoCornerRadius.MD))
                            .background(RikoFavoriteActive.copy(alpha = 0.12f))
                            .clickable {
                                val target = menuPlaylist
                                if (target != null && !target.isFavorites) {
                                    // A83: destructive action gets a confirm step
                                    // (was: instant delete). Close menu first.
                                    confirmDelete = target
                                    menuPlaylist = null
                                }
                            }
                            .padding(RikoSpacing.MD),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = null,
                            tint = RikoFavoriteActive
                        )
                        Spacer(Modifier.width(RikoSpacing.MD))
                        Text(
                            text = "Delete Playlist",
                            style = RikoTypography.Button,
                            color = RikoFavoriteActive
                        )
                    }
                    Spacer(Modifier.height(RikoSpacing.LG))
                }
            }
        }
    }

    // A83 destructive confirmation — reuses the detail screen's confirm sheet.
    if (confirmDelete != null) {
        val target = confirmDelete
        ModalBottomSheet(
            onDismissRequest = { confirmDelete = null },
            containerColor = Color.Transparent,
            contentColor = RikoTextPrimary,
            dragHandle = null,
            shape = RikoShapes.TopSheet
        ) {
            GlassBottomSheetContainer(modifier = Modifier.fillMaxWidth(), accentColor = accentColor) {
                PlaylistDeleteSheet(
                    accentColor = accentColor,
                    onDelete = {
                        target?.let { viewModel.deletePlaylist(it.id) }
                        confirmDelete = null
                    },
                    onDismiss = { confirmDelete = null }
                )
            }
        }
    }
}
// -- New Playlist CTA — SM spec 3.5 step 5: glass OUTER pill (56-60dp,
// radius-full) containing an INSET 52dp aurora "+" circle at far-left
// (4dp inset), label "Add New Playlist" (D7 corrected spelling) -------------
@Composable
private fun NewPlaylistCta(accentColor: Color, onClick: () -> Unit) {
    RikoGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RikoSpacing.XL, vertical = RikoSpacing.SM)
            .height(56.dp)
            .clickable(onClick = onClick),
        shape = RikoShape.Pill
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RikoSpacing.LG)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            com.rikodev.music.android.core.designsystem.riko.theme.RikoPrimaryGradient
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(RikoIconSize.Compact)
                )
            }
            Text(text = "Add New Playlist", style = RikoTypography.Button, color = RikoTextPrimary)
            Spacer(Modifier.weight(1f))
        }
    }
}

// -- Playlist card - artwork + identity + 3-dot overflow (reference) ---------
@Composable
private fun PlaylistCard(
    playlist: Playlist,
    accentColor: Color,
    onClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RikoSpacing.XL, vertical = RikoSpacing.SM)
            .clip(RoundedCornerShape(RikoCornerRadius.MD))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlaylistArtwork(playlist = playlist, accentColor = accentColor)
        Spacer(Modifier.width(RikoSpacing.MD))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = RikoTypography.Body,
                color = RikoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                // SM spec D8: subtitle = curator/owner display name (not song count)
                text = playlist.description.ifBlank { "My Playlist" },
                style = RikoTypography.Secondary,
                color = RikoTextSecondary,
                maxLines = 1
            )
        }
        // Favorites playlist: rename + delete are both repository-blocked, so
        // a menu here would only ever hold a dead action (G-7) — no button.
        if (!playlist.isFavorites) {
            RikoIconButton(
                icon = Icons.Rounded.MoreVert,
                contentDescription = "More options for " + playlist.name,
                onClick = onMenuClick,
                container = false
            )
        }
    }
}

@Composable
private fun PlaylistArtwork(playlist: Playlist, accentColor: Color) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(RikoCornerRadius.SM))
            .background(RikoSurfaceElevated),
        contentAlignment = Alignment.Center
    ) {
        if (playlist.isFavorites) {
            Icon(Icons.Rounded.Favorite, contentDescription = null, tint = RikoFavoriteActive, modifier = Modifier.size(RikoIconSize.Standard))
        } else if (playlist.coverUri != null) {
            // C4: remembered request — inline Builder has no stable equals and
            // rebuilds Coil on every card recomposition.
            // Context must be read OUTSIDE remember (LocalContext.current is @Composable).
            val context = LocalContext.current
            val coverModel = remember(playlist.coverUri, context) {
                ImageRequest.Builder(context)
                    .data(playlist.coverUri)
                    .size(256)
                    .memoryCacheKey("${playlist.coverUri}_cover256")
                    .crossfade(200)
                    .allowHardware(true)
                    .build()
            }
            RikoArtwork(
                // PERF P0 (RC-06): cover thumb prescale
                model = coverModel,
                contentDescription = playlist.name,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = RikoTextTertiary, modifier = Modifier.size(RikoIconSize.Prominent))
        }
    }
}

// -- Empty state (Plan 6 28) -------------------------------------------------
@Composable
private fun PlaylistEmptyState(onCreate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(RikoSpacing.XXXL),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.PlaylistAdd, contentDescription = null, tint = RikoTextTertiary, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(RikoSpacing.LG))
        Text(text = "No playlists yet", style = RikoTypography.Section, color = RikoTextPrimary)
        Spacer(Modifier.height(RikoSpacing.XS))
        Text(
            text = "Create a playlist to organize your music.",
            style = RikoTypography.Secondary,
            color = RikoTextTertiary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(RikoSpacing.LG))
        RikoPrimaryButton(text = "Create Playlist", leadingIcon = Icons.Filled.Add, onClick = onCreate)
    }
}

// -- Create playlist - compact Riko sheet (Plan 6 16-17) ---------------------
@Composable
private fun CreatePlaylistSheet(
    accentColor: Color,
    /** A14 validation: names already taken (case-insensitive, trimmed). */
    existingNames: List<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun submit() {
        val trimmed = name.trim()
        errorText = when {
            trimmed.isEmpty() -> "Playlist name can't be empty"
            existingNames.any { it.trim().equals(trimmed, ignoreCase = true) } ->
                "You already have a playlist named \"$trimmed\""
            else -> null
        }
        if (errorText == null) onConfirm(trimmed)
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = RikoSpacing.LG)) {
        Text(text = "Create Playlist", style = RikoTypography.Section, color = RikoTextPrimary)
        Spacer(Modifier.height(RikoSpacing.MD))
        PlaylistNameField(
            value = name,
            onValueChange = { name = it; errorText = null },
            onDone = { submit() }
        )
        errorText?.let { message ->
            Spacer(Modifier.height(RikoSpacing.XS))
            Text(
                text = message,
                style = RikoTypography.Secondary,
                color = RikoError,
                maxLines = 2
            )
        }
        Spacer(Modifier.height(RikoSpacing.LG))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RikoSpacing.MD)
        ) {
            RikoSecondaryButton(text = "Cancel", onClick = onDismiss, modifier = Modifier.weight(1f))
            RikoPrimaryButton(text = "Create", onClick = { submit() }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(RikoSpacing.LG))
    }
}

@Composable
private fun PlaylistNameField(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Playlist name", color = RikoTextTertiary) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedTextColor = RikoTextPrimary,
            unfocusedTextColor = RikoTextPrimary,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = RikoTextPrimary
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier.fillMaxWidth().height(52.dp)
    )
}
