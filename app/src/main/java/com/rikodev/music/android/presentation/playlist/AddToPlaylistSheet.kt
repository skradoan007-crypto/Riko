package com.rikodev.music.android.presentation.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.components.RikoSecondaryButton
import com.rikodev.music.android.core.designsystem.riko.theme.RikoFavoriteActive
import com.rikodev.music.android.core.designsystem.riko.theme.RikoShape
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextTertiary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography
import com.rikodev.music.android.domain.model.Playlist

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — ADD TO PLAYLIST PICKER (G-5: NP menu "Add to playlist" was a no-op)
//
// Minimal honest picker: the playlists that exist, tap = add current song
// (PlayerViewModel.addSongToPlaylist). Favorites stays selectable — the
// repository routes it to favoriteIds.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AddToPlaylistSheet(
    playlists: List<Playlist>,
    onPick: (Playlist) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Add to playlist",
            style = RikoTypography.Section,
            color = RikoTextPrimary
        )
        Spacer(Modifier.height(RikoSpacing.MD))

        if (playlists.isEmpty()) {
            Text(
                text = "No playlists yet — create one from the Playlists tab.",
                style = RikoTypography.Secondary,
                color = RikoTextTertiary
            )
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 340.dp)) {
                items(playlists, key = { it.id }, contentType = { "playlist_row" }) { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RikoShape.Card)
                            .clickable { onPick(playlist) }
                            .padding(vertical = RikoSpacing.SM, horizontal = RikoSpacing.XS),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (playlist.isFavorites) {
                                Icons.Rounded.Favorite
                            } else {
                                Icons.Rounded.PlaylistPlay
                            },
                            contentDescription = null,
                            tint = if (playlist.isFavorites) RikoFavoriteActive else RikoTextTertiary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(RikoSpacing.MD))
                        Text(
                            text = playlist.name,
                            style = RikoTypography.Body,
                            color = RikoTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(RikoSpacing.LG))
        RikoSecondaryButton(text = "Cancel", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(RikoSpacing.LG))
    }
}
