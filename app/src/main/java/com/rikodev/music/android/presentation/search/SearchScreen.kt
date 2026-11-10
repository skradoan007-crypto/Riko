package com.rikodev.music.android.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.clickable
import com.rikodev.music.android.core.designsystem.glass.GlassSurface
import com.rikodev.music.android.core.designsystem.riko.components.ARTWORK_PX_ROW
import com.rikodev.music.android.core.designsystem.riko.components.RikoArtwork
import com.rikodev.music.android.core.designsystem.riko.components.RikoSecondaryButton
import com.rikodev.music.android.core.designsystem.riko.components.RikoTrackRow
import com.rikodev.music.android.core.designsystem.riko.components.rememberAlbumArtworkModel
import com.rikodev.music.android.core.designsystem.riko.components.V2TestTags
import com.rikodev.music.android.core.designsystem.riko.components.RikoTopBar
import com.rikodev.music.android.core.designsystem.riko.components.RikoTopBarStyle
import com.rikodev.music.android.core.designsystem.riko.components.rikoScreenFoundation
import com.rikodev.music.android.core.designsystem.riko.shell.LocalRikoShellMetrics
import com.rikodev.music.android.core.designsystem.riko.theme.RikoThemeV2
import com.rikodev.music.android.core.designsystem.riko.theme.RikoCornerRadius
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextTertiary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography
import com.rikodev.music.android.core.designsystem.theme.GlassTokens
import com.rikodev.music.android.core.designsystem.theme.LocalDynamicColors
import com.rikodev.music.android.presentation.nowplaying.PlayerViewModel

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — SEARCH SCREEN (Plan 8)
//
// Visual migration to Riko Premium UI. All existing search behavior preserved:
//   - SearchViewModel: 280ms debounce, flatMapLatest cancellation
//   - MusicRepository.searchSongs(): title/artist/album LIKE query
//   - Query state, clear query, empty/no-results/loading states
//
// Design: Glass search field + RikoTrackRow results + Riko skeleton loading.
// Albums/Artists sections reuse the shared AlbumDetail/ArtistDetail routes.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    /** Album section row -> shared collection route (was unreachable). */
    onAlbumClick: (Long) -> Unit = {},
    /** Artist section row -> shared collection route (was unreachable). */
    onArtistClick: (Long) -> Unit = {}
) {
    val query by searchViewModel.query.collectAsStateWithLifecycle()
    val searchState by searchViewModel.searchState.collectAsStateWithLifecycle()
    val isSearching by searchViewModel.isSearching.collectAsStateWithLifecycle()
    val currentSong by playerViewModel.currentSong.collectAsStateWithLifecycle()
    val playing by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    val dynamicColors = LocalDynamicColors.current
    val accentColor = dynamicColors.vibrantColor
    val shellMetrics = LocalRikoShellMetrics.current
    val navigationBarsBottom = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    fun dismissIme() {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .rikoScreenFoundation(depth = 0.75f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            RikoTopBar(
                title = "Search",
                style = RikoTopBarStyle.Back,
                onBack = {
                    dismissIme()
                    onBack()
                }
            )
            com.rikodev.music.android.presentation.search.V2SearchBar(
                query = query,
                onQuery = { searchViewModel.onQueryChange(it) },
                onClear = { searchViewModel.clearQuery() },
                onSearch = { dismissIme() },
                modifier = Modifier.padding(horizontal = RikoSpacing.LG, vertical = RikoSpacing.SM)
            )

            when {
                isSearching -> {
                    SearchLoadingState(
                        modifier = Modifier.fillMaxSize(),
                        accentColor = accentColor
                    )
                }
                query.isBlank() -> {
                    SearchEmptyQueryState(
                        modifier = Modifier.fillMaxSize()
                    )
                }
                searchState.error -> {
                    SearchErrorState(
                        modifier = Modifier.fillMaxSize(),
                        onRetry = { searchViewModel.retry() }
                    )
                }
                searchState.songs.isEmpty() -> {
                    SearchNoResultsState(
                        modifier = Modifier.fillMaxSize(),
                        query = query
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = RikoSpacing.LG,
                            end = RikoSpacing.LG,
                            top = RikoSpacing.MD,
                            // Shell stack (mini + nav) instead of the old raw 80.dp.
                            bottom = shellMetrics.contentBottomPadding + navigationBarsBottom + RikoSpacing.LG
                        ),
                        verticalArrangement = Arrangement.spacedBy(RikoSpacing.XS)
                    ) {
                        item(key = "count") {
                            Text(
                                text = "${searchState.songs.size} result${if (searchState.songs.size != 1) "s" else ""}",
                                style = RikoTypography.Secondary,
                                color = RikoTextTertiary,
                                modifier = Modifier.padding(vertical = RikoSpacing.SM)
                            )
                        }

                        // Sections: derived views of the same matched set —
                        // their taps reach the shared collection detail routes.
                        if (searchState.albums.isNotEmpty()) {
                            item(key = "hdr_albums") { SearchSectionHeader("Albums") }
                            itemsIndexed(
                                items = searchState.albums,
                                key = { _, album -> "album_${album.albumId}" },
                                contentType = { _, _ -> "search_album" }
                            ) { _, album ->
                                SearchAlbumRow(album = album) {
                                    onAlbumClick(album.albumId)
                                }
                            }
                        }
                        if (searchState.artists.isNotEmpty()) {
                            item(key = "hdr_artists") { SearchSectionHeader("Artists") }
                            itemsIndexed(
                                items = searchState.artists,
                                key = { _, artist -> "artist_${artist.artistId}" },
                                contentType = { _, _ -> "search_artist" }
                            ) { _, artist ->
                                SearchArtistRow(artist = artist, accentColor = accentColor) {
                                    onArtistClick(artist.artistId)
                                }
                            }
                        }

                        item(key = "hdr_songs") { SearchSectionHeader("Songs") }
                        itemsIndexed(
                            items = searchState.songs,
                            key = { _, song -> song.id },
                            contentType = { _, _ -> "v2_track" }
                        ) { index, song ->
                            val subtitle = remember(song.id) { "${song.artist} — ${song.album}" }
                            RikoTrackRow(
                                id = song.id,
                                title = song.title,
                                subtitle = subtitle,
                                duration = song.durationFormatted,
                                artworkModel = rememberAlbumArtworkModel(song.albumId, ARTWORK_PX_ROW, songId = song.id, songUri = song.uri),
                                isPlaying = currentSong?.id == song.id && playing,
                                onClick = {
                                    playerViewModel.playQueue(searchState.songs, index)
                                    dismissIme()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun V2SearchBar(
    query: String,
    onQuery: (String) -> Unit,
    onClear: () -> Unit = {},
    /** IME search action — was an empty no-op; now dismisses keyboard. */
    onSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(RikoCornerRadius.MD))
            .background(RikoThemeV2.Colors.surface)
            .testTag(V2TestTags.SEARCH_FIELD)
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQuery,
            textStyle = RikoTypography.Body.copy(color = RikoThemeV2.Colors.onBackground),
            cursorBrush = SolidColor(RikoThemeV2.Colors.accent),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = RikoSpacing.MD),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = RikoThemeV2.Colors.onBackgroundSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(RikoSpacing.SM))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search songs, artists...",
                                style = RikoTypography.Body,
                                color = RikoThemeV2.Colors.onBackgroundSecondary
                            )
                        }
                        innerTextField()
                    }
                    if (query.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear search",
                            tint = RikoThemeV2.Colors.onBackgroundSecondary,
                            modifier = Modifier.size(40.dp).clickable(role = Role.Button, onClick = onClear).padding(10.dp)
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun SearchEmptyQueryState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = RikoTextTertiary,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(RikoSpacing.MD))
        Text(
            text = "Search your music library",
            style = RikoTypography.Title,
            color = RikoTextPrimary
        )
        Spacer(modifier = Modifier.height(RikoSpacing.XS))
        Text(
            text = "Find songs by title, artist, or album",
            style = RikoTypography.Secondary,
            color = RikoTextTertiary
        )
    }
}

@Composable
private fun SearchNoResultsState(
    modifier: Modifier = Modifier,
    query: String
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.SearchOff,
            contentDescription = null,
            tint = RikoTextTertiary,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(RikoSpacing.MD))
        Text(
            text = "No results",
            style = RikoTypography.Title,
            color = RikoTextPrimary
        )
        Spacer(modifier = Modifier.height(RikoSpacing.XS))
        Text(
            text = "We couldn't find anything matching \"$query\"",
            style = RikoTypography.Secondary,
            color = RikoTextTertiary
        )
        Spacer(modifier = Modifier.height(RikoSpacing.SM))
        Text(
            text = "Try another search",
            style = RikoTypography.Body,
            color = RikoTextTertiary
        )
    }
}

@Composable
private fun SearchLoadingState(
    modifier: Modifier = Modifier,
    accentColor: Color
) {
    Column(
        modifier = modifier.padding(RikoSpacing.LG),
        verticalArrangement = Arrangement.spacedBy(RikoSpacing.MD)
    ) {
        repeat(5) {
            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                style = GlassTokens.Chip,
                cornerRadius = RikoCornerRadius.MD
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(RikoSpacing.MD),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(RikoCornerRadius.SM))
                            .background(RikoTextTertiary.copy(alpha = 0.1f))
                    )
                    Spacer(modifier = Modifier.size(RikoSpacing.MD))
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(14.dp)
                                .clip(RoundedCornerShape(RikoCornerRadius.SM))
                                .background(RikoTextTertiary.copy(alpha = 0.1f))
                        )
                        Spacer(modifier = Modifier.height(RikoSpacing.XS))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(RikoCornerRadius.SM))
                                .background(RikoTextTertiary.copy(alpha = 0.08f))
                        )
                    }
                }
            }
        }
    }
}

// ── Section header + section rows (Album/Artist tap -> shared detail routes) ─
@Composable
private fun SearchSectionHeader(text: String) {
    Text(
        text = text,
        style = RikoTypography.Label,
        color = RikoTextTertiary,
        modifier = Modifier.padding(top = RikoSpacing.SM, bottom = RikoSpacing.XS)
    )
}

@Composable
private fun SearchAlbumRow(album: SearchAlbumUi, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RikoCornerRadius.MD))
            .clickable(onClick = onClick)
            .padding(vertical = RikoSpacing.XS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(RikoCornerRadius.SM))
                .background(RikoThemeV2.Colors.surface)
        ) {
            RikoArtwork(
                model = rememberAlbumArtworkModel(album.albumId, ARTWORK_PX_ROW, songId = album.albumId),
                contentDescription = album.title,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.size(RikoSpacing.MD))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.title,
                style = RikoTypography.Body,
                color = RikoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Album • " + album.artist + " • " + album.songCount +
                    if (album.songCount == 1) " song" else " songs",
                style = RikoTypography.Secondary,
                color = RikoTextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SearchArtistRow(artist: SearchArtistUi, accentColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RikoCornerRadius.MD))
            .clickable(onClick = onClick)
            .padding(vertical = RikoSpacing.XS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = artist.name.take(1).uppercase(),
                style = RikoTypography.Button,
                color = accentColor
            )
        }
        Spacer(modifier = Modifier.size(RikoSpacing.MD))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                style = RikoTypography.Body,
                color = RikoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Artist • " + artist.songCount +
                    if (artist.songCount == 1) " song" else " songs",
                style = RikoTypography.Secondary,
                color = RikoTextTertiary,
                maxLines = 1
            )
        }
    }
}

// ── Error state (G-7) — retry re-runs the current query --------------------
@Composable
private fun SearchErrorState(modifier: Modifier = Modifier, onRetry: () -> Unit) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.SearchOff,
            contentDescription = null,
            tint = RikoTextTertiary,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(RikoSpacing.MD))
        Text(
            text = "Something went wrong",
            style = RikoTypography.Title,
            color = RikoTextPrimary
        )
        Spacer(modifier = Modifier.height(RikoSpacing.XS))
        Text(
            text = "We couldn't search your library.",
            style = RikoTypography.Secondary,
            color = RikoTextTertiary
        )
        Spacer(modifier = Modifier.height(RikoSpacing.LG))
        RikoSecondaryButton(text = "Try again", onClick = onRetry)
    }
}