package com.rikodev.music.android.presentation.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rikodev.music.android.core.designsystem.riko.components.RikoIconButton
import com.rikodev.music.android.core.designsystem.theme.RikoShapes
import com.rikodev.music.android.core.designsystem.riko.components.RikoPrimaryButton
import com.rikodev.music.android.core.designsystem.riko.components.RikoSecondaryButton
import com.rikodev.music.android.core.designsystem.riko.components.ARTWORK_PX_ROW
import com.rikodev.music.android.core.designsystem.riko.components.RikoTrackRow
import com.rikodev.music.android.core.designsystem.riko.components.rememberAlbumArtworkModel
import com.rikodev.music.android.core.designsystem.riko.components.RikoPrimaryButton
import com.rikodev.music.android.core.designsystem.riko.components.V2TestTags
import com.rikodev.music.android.core.designsystem.riko.components.rikoScreenFoundation
import com.rikodev.music.android.core.designsystem.riko.theme.RikoError
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextTertiary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoThemeV2
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography
import com.rikodev.music.android.domain.model.Song
import com.rikodev.music.android.core.designsystem.theme.LocalDynamicColors
import com.rikodev.music.android.core.designsystem.theme.TextPrimary
import com.rikodev.music.android.domain.model.ShuffleMode
import com.rikodev.music.android.presentation.collection.CollectionIdentity
import com.rikodev.music.android.presentation.collection.PlaylistIdentity
import com.rikodev.music.android.presentation.collection.RikoCollectionDetail
import com.rikodev.music.android.presentation.collection.components.CollectionEmptyState
import com.rikodev.music.android.presentation.collection.components.CollectionSkeleton
import com.rikodev.music.android.presentation.components.GlassBottomSheetContainer

import com.rikodev.music.android.presentation.nowplaying.PlayerViewModel

@Composable
fun PlaylistDetailScreen(
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    onSongClick: () -> Unit = {},
    /** After delete → return to the playlist index (Plan 6 §19, §34). */
    onDeleted: () -> Unit = {},
    detailViewModel: PlaylistDetailViewModel = hiltViewModel()
) {
    val uiState by detailViewModel.uiState.collectAsStateWithLifecycle()
    val currentSong by playerViewModel.currentSong.collectAsStateWithLifecycle()
    val playing by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    val shuffleMode by playerViewModel.shuffleMode.collectAsStateWithLifecycle()
    val showAddSongsSheet by detailViewModel.showAddSongsSheet.collectAsStateWithLifecycle()
    val selectedSongIds by detailViewModel.selectedSongIds.collectAsStateWithLifecycle()
    val pickableSongs by detailViewModel.pickableSongs.collectAsStateWithLifecycle()

    var showMoreSheet by remember { mutableStateOf(false) }
    var showRenameSheet by remember { mutableStateOf(false) }
    var showDeleteSheet by remember { mutableStateOf(false) }

    val addSongsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val moreSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val renameSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val deleteSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    val accentColor = LocalDynamicColors.current.vibrantColor
    val playlist = uiState.playlist
    val songs = uiState.songs

    fun shuffleAndPlay() {
        if (shuffleMode == ShuffleMode.OFF) playerViewModel.toggleShuffle()
        playerViewModel.playQueue(songs, 0)
        onSongClick()
    }

    val identity: CollectionIdentity? = playlist?.let {
        PlaylistIdentity(
            title = it.name,
            metadata = buildList {
                if (!it.description.isBlank()) add(it.description)
                add(if (songs.size == 1) "1 song" else "${songs.size} songs")
            },
            coverModel = it.coverUri?.toString()
        )
    }

    RikoCollectionDetail(
        identity = identity,
        onBack = onBack,
        onPlay = {
            playerViewModel.playQueue(songs, 0)
            onSongClick()
        },
        onShuffle = if (songs.isEmpty()) null else ::shuffleAndPlay,
        modifier = modifier
    ) {
        when {
            uiState.isLoading -> item { CollectionSkeleton() }
            songs.isEmpty() -> {
                if (playlist != null && !playlist.isFavorites) {
                    item {
                        PlaylistManagementBar(
                            isEditMode = uiState.isEditMode,
                            onAddSongs = { detailViewModel.openAddSongsSheet() },
                            onToggleEdit = { detailViewModel.toggleEditMode() },
                            onMore = { showMoreSheet = true }
                        )
                    }
                }
                item {
                    CollectionEmptyState(
                        title = "No songs yet",
                        message = if (playlist?.isFavorites == true)
                            "Songs you favorite will show up here"
                        else
                            "Add songs from your library to get started"
                    )
                }
            }
else -> {
                if (playlist != null && !playlist.isFavorites) {
                    item {
                        PlaylistManagementBar(
                            isEditMode = uiState.isEditMode,
                            onAddSongs = { detailViewModel.openAddSongsSheet() },
                            onToggleEdit = { detailViewModel.toggleEditMode() },
                            onMore = { showMoreSheet = true }
                        )
                    }
                }
                itemsIndexed(songs, key = { _, song -> song.id }, contentType = { _, _ -> "v2_track" }) { index, song ->
                    val lastIndex = songs.size - 1
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RikoTrackRow(
                            id = song.id,
                            title = song.title,
                            subtitle = song.artist,
                            duration = song.durationFormatted,
                            artworkModel = rememberAlbumArtworkModel(song.albumId, ARTWORK_PX_ROW, songId = song.id, songUri = song.uri),
                            isPlaying = currentSong?.id == song.id && playing,
                            onClick = {
                                playerViewModel.playQueue(songs, index)
                                onSongClick()
                            },
                            // G-7: was onMenu = {} (dead kebab). Edit mode's
                            // Close button owns removal — no menu needed.
                            modifier = Modifier.weight(1f)
                        )
                        if (playlist != null && !playlist.isFavorites && uiState.isEditMode) {
                            RikoIconButton(
                                icon = Icons.Filled.KeyboardArrowUp,
                                contentDescription = "Move up",
                                onClick = { detailViewModel.moveSongUp(index) },
                                container = false,
                                enabled = index > 0
                            )
                            RikoIconButton(
                                icon = Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Move down",
                                onClick = { detailViewModel.moveSongDown(index) },
                                container = false,
                                enabled = index < lastIndex
                            )
                            RikoIconButton(
                                icon = Icons.Filled.Close,
                                contentDescription = "Remove from playlist",
                                onClick = { detailViewModel.removeSong(song.id) },
                                container = false
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddSongsSheet) {
        ModalBottomSheet(
            onDismissRequest = { detailViewModel.dismissAddSongsSheet() },
            sheetState = addSongsSheetState,
            containerColor = Color.Transparent,
            contentColor = TextPrimary,
            dragHandle = null,
            shape = RikoShapes.TopSheet,
            modifier = Modifier.fillMaxHeight(0.85f)
        ) {
            GlassBottomSheetContainer(
                modifier = Modifier.fillMaxHeight(),
                accentColor = accentColor
            ) {
                AddSongsSheet(
                    modifier = Modifier.weight(1f),
                    availableSongs = pickableSongs,
                    selectedIds = selectedSongIds,
                    accentColor = accentColor,
                    onToggleSong = { detailViewModel.toggleSongSelection(it) },
                    onConfirm = { detailViewModel.confirmAddSelectedSongs() }
                )
            }
        }
    }
if (showMoreSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoreSheet = false },
            sheetState = moreSheetState,
            containerColor = Color.Transparent,
            contentColor = TextPrimary,
            dragHandle = null,
            shape = RikoShapes.TopSheet,
            modifier = Modifier.fillMaxHeight(0.5f)
        ) {
            GlassBottomSheetContainer(modifier = Modifier.fillMaxHeight(), accentColor = accentColor) {
                PlaylistMoreSheet(
                    accentColor = accentColor,
                    onRename = {
                        showMoreSheet = false
                        showRenameSheet = true
                    },
                    onDelete = {
                        showMoreSheet = false
                        showDeleteSheet = true
                    }
                )
            }
        }
    }

    if (showRenameSheet) {
        ModalBottomSheet(
            onDismissRequest = { showRenameSheet = false },
            sheetState = renameSheetState,
            containerColor = Color.Transparent,
            contentColor = TextPrimary,
            dragHandle = null,
            shape = RikoShapes.TopSheet,
            modifier = Modifier.fillMaxHeight(0.5f)
        ) {
            GlassBottomSheetContainer(modifier = Modifier.fillMaxHeight(), accentColor = accentColor) {
                PlaylistRenameSheet(
                    initialName = playlist?.name ?: "",
                    accentColor = accentColor,
                    onConfirm = { name ->
                        if (name.isNotBlank()) {
                            detailViewModel.renamePlaylist(name)
                            showRenameSheet = false
                        }
                    },
                    onDismiss = { showRenameSheet = false }
                )
            }
        }
    }

    if (showDeleteSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDeleteSheet = false },
            sheetState = deleteSheetState,
            containerColor = Color.Transparent,
            contentColor = TextPrimary,
            dragHandle = null,
            shape = RikoShapes.TopSheet,
            modifier = Modifier.fillMaxHeight(0.5f)
        ) {
            GlassBottomSheetContainer(modifier = Modifier.fillMaxHeight(), accentColor = accentColor) {
                PlaylistDeleteSheet(
                    accentColor = accentColor,
                    onDelete = {
                        // Never touches source music files — only the playlist
                        // row (PlaylistRepository.deletePlaylist). §22 semantics.
                        detailViewModel.deletePlaylist(onDeleted)
                    },
                    onDismiss = { showDeleteSheet = false }
                )
            }
        }
    }
}

@Composable
fun PlaylistDetailContent(
    title: String,
    songs: List<Song>,
    onPlayAll: () -> Unit,
    onPlay: (Int) -> Unit,
    onMenu: (Song) -> Unit,
    modifier: Modifier = Modifier,
    playingSongId: Long? = null,
    isPlayingNow: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .rikoScreenFoundation(depth = 0.82f)
    ) {
        Text(
            text = title,
            style = RikoThemeV2.Type.npTitle,
            color = RikoThemeV2.Colors.onBackground,
            modifier = Modifier.padding(horizontal = RikoSpacing.LG, vertical = RikoSpacing.SM)
        )
        Text(
            text = if (songs.size == 1) "1 song" else "${songs.size} songs",
            style = RikoThemeV2.Type.cardSubtitle,
            modifier = Modifier.padding(horizontal = RikoSpacing.LG)
        )
        Spacer(Modifier.height(RikoSpacing.MD))
        RikoPrimaryButton(
            text = "Play All",
            onClick = onPlayAll,
            modifier = Modifier
                .padding(horizontal = RikoSpacing.LG)
                .testTag(V2TestTags.PLAY_ALL_BUTTON)
        )
        Spacer(Modifier.height(RikoSpacing.MD))
        Row(
            modifier = Modifier.padding(horizontal = RikoSpacing.LG),
            horizontalArrangement = Arrangement.spacedBy(RikoSpacing.MD)
        ) {
            V2CircleIcon(icon = Icons.Filled.Favorite, description = "Favorite")
            V2CircleIcon(icon = Icons.Filled.Download, description = "Download")
        }
        Spacer(Modifier.height(RikoSpacing.SM))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(songs, key = { _, song -> song.id }, contentType = { _, _ -> "v2_track" }) { index, song ->
                RikoTrackRow(
                    id = song.id,
                    title = song.title,
                    subtitle = song.artist,
                    duration = song.durationFormatted,
                    artworkModel = rememberAlbumArtworkModel(song.albumId, ARTWORK_PX_ROW, songId = song.id, songUri = song.uri),
                    isPlaying = playingSongId == song.id && isPlayingNow,
                    onClick = { onPlay(index) },
                    onMenu = { onMenu(song) }
                )
            }
        }
    }
}

@Composable
private fun V2CircleIcon(icon: ImageVector, description: String) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .border(1.dp, RikoThemeV2.Colors.divider, CircleShape)
            .clickable(role = Role.Button, onClick = {}),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = RikoThemeV2.Colors.onBackground,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun PlaylistManagementBar(
    isEditMode: Boolean,
    onAddSongs: () -> Unit,
    onToggleEdit: () -> Unit,
    onMore: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RikoSpacing.LG, vertical = RikoSpacing.SM),
        horizontalArrangement = Arrangement.spacedBy(RikoSpacing.MD),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RikoPrimaryButton(
            text = "Add songs",
            leadingIcon = Icons.Filled.PlaylistAdd,
            onClick = onAddSongs,
            modifier = Modifier.weight(1f)
        )
        RikoIconButton(
            icon = if (isEditMode) Icons.Filled.Done else Icons.Filled.Edit,
            contentDescription = if (isEditMode) "Done editing" else "Edit playlist",
            onClick = onToggleEdit,
            container = false
        )
        RikoIconButton(
            icon = Icons.Rounded.MoreVert,
            contentDescription = "More options",
            onClick = onMore,
            container = false
        )
    }
}
// ── More-sheet action rows ───────────────────────────────────────────────────
@Composable
private fun SheetActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = RikoTextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = RikoSpacing.LG, vertical = RikoSpacing.MD),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(RikoSpacing.MD))
        Text(text = label, style = RikoTypography.Body, color = tint, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PlaylistMoreSheet(
    accentColor: Color,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = RikoSpacing.LG)) {
        Text(text = "Playlist options", style = RikoTypography.Section, color = RikoTextPrimary)
        Spacer(Modifier.height(RikoSpacing.MD))
        SheetActionRow(icon = Icons.Filled.Edit, label = "Rename", onClick = onRename)
        SheetActionRow(icon = Icons.Filled.DeleteOutline, label = "Delete playlist", onClick = onDelete, tint = RikoError)
    }
}

@Composable
private fun PlaylistRenameSheet(
    initialName: String,
    accentColor: Color,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = RikoSpacing.LG)) {
        Text(text = "Rename playlist", style = RikoTypography.Section, color = RikoTextPrimary)
        Spacer(Modifier.height(RikoSpacing.MD))
        SheetTextField(
            value = name,
            placeholder = "Playlist name",
            onValueChange = { name = it },
            onDone = { if (name.isNotBlank()) onConfirm(name) }
        )
        Spacer(Modifier.height(RikoSpacing.LG))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RikoSpacing.MD)
        ) {
            RikoSecondaryButton(text = "Cancel", onClick = onDismiss, modifier = Modifier.weight(1f))
            RikoPrimaryButton(text = "Save", onClick = { if (name.isNotBlank()) onConfirm(name) }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(RikoSpacing.LG))
    }
}

/** Shared destructive-confirm sheet — index PlaylistsScreen reuses it (A83). */
@Composable
fun PlaylistDeleteSheet(
    accentColor: Color,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = RikoSpacing.LG)) {
        Text(text = "Delete playlist?", style = RikoTypography.Section, color = RikoTextPrimary)
        Spacer(Modifier.height(RikoSpacing.XS))
        Text(text = "This cannot be undone.", style = RikoTypography.Secondary, color = RikoTextTertiary)
        Spacer(Modifier.height(RikoSpacing.LG))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RikoSpacing.MD)
        ) {
            RikoSecondaryButton(text = "Cancel", onClick = onDismiss, modifier = Modifier.weight(1f))
            RikoPrimaryButton(text = "Delete", onClick = onDelete, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(RikoSpacing.LG))
    }
}

@Composable
private fun SheetTextField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = RikoTextTertiary) },
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