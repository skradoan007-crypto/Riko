package com.rikodev.music.android.presentation.mymusic.components
import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.components.RikoIconButton
import com.rikodev.music.android.core.designsystem.riko.components.ARTWORK_PX_ROW
import com.rikodev.music.android.core.designsystem.riko.components.RikoTrackRow
import com.rikodev.music.android.core.designsystem.riko.components.rememberAlbumArtworkModel
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.domain.model.Song

import com.rikodev.music.android.presentation.mymusic.FolderUi

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — LIBRARY CONTENTS (PLAN 4, spec §6–8, §15–21, §27–28, §30)
//
// One content composable per mode. Scroll states are HOISTED by
// MyMusicScreen (rememberSaveable) so mode round-trips keep their own position
// behave predictably (spec §30) — never a shared global scroll state.
// All containers are lazy — large libraries stay smooth (spec §28).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SongLibraryContent(
    songs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    listState: LazyListState,
    contentPadding: PaddingValues,
    onSongClick: (Int) -> Unit,
    onFavorite: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        itemsIndexed(songs, key = { _, song -> song.id }, contentType = { _, _ -> "v2_track" }) { index, song ->
            RikoTrackRow(
                id = song.id,
                title = song.title,
                subtitle = song.artist,
                duration = song.durationFormatted,
                artworkModel = rememberAlbumArtworkModel(song.albumId, ARTWORK_PX_ROW, songId = song.id, songUri = song.uri),
                isPlaying = currentSongId == song.id && isPlaying,
                onClick = {
                    if (index >= 0) onSongClick(index)
                },
                onMenu = { onFavorite(song.id) }
            )
        }
    }
}

@Composable
fun FolderLibraryContent(
    folders: List<FolderUi>,
    listState: LazyListState,
    contentPadding: PaddingValues,
    onFolderClick: (FolderUi) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().padding(top = RikoSpacing.XS),
        contentPadding = contentPadding
    ) {
        items(folders, key = { folder -> folder.id }, contentType = { "folder_row" }) { folder ->
            RikoFolderRow(folder = folder, onClick = { onFolderClick(folder) })
        }
    }
}
