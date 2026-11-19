package com.rikodev.music.android.presentation.continuum

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlass
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlassBorder
import com.rikodev.music.android.core.designsystem.riko.theme.RikoPrimaryGradient
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextOnAccent
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextSecondary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography
import com.rikodev.music.android.presentation.favorites.FavoritesScreen
import com.rikodev.music.android.presentation.nowplaying.PlayerViewModel
import com.rikodev.music.android.presentation.playlist.PlaylistsScreen

// ─────────────────────────────────────────────────────────────────────────────
// RIKO CONTINUUM — LIBRARY HUB (P1)
//
// Unifies Playlists, Favorites, and personal collections inside the Library
// section of CONTINUUM without duplicating or breaking either existing screen.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ContinuumLibraryHubScreen(
    selectedSubTab: Int,
    onSelectSubTab: (Int) -> Unit,
    playerViewModel: PlayerViewModel,
    snackbarHostState: SnackbarHostState,
    onPlaylistClick: (Long) -> Unit,
    onPlaylistCreated: (Long) -> Unit,
    onSongClick: () -> Unit,
    onBrowseExplore: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Compact header bar with segmented Playlists / Favorites switcher + quick Search & Settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RikoSpacing.LG, vertical = RikoSpacing.SM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(RikoGlass)
                    .border(1.dp, RikoGlassBorder, CircleShape)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LibraryHubPill(
                    label = "Playlists",
                    isSelected = selectedSubTab == 0,
                    onClick = { onSelectSubTab(0) }
                )
                LibraryHubPill(
                    label = "Favorites",
                    isSelected = selectedSubTab == 1,
                    onClick = { onSelectSubTab(1) }
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onSearchClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search library",
                        tint = RikoTextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onSettingsClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        tint = RikoTextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (selectedSubTab == 0) {
                PlaylistsScreen(
                    modifier = Modifier.fillMaxSize(),
                    onPlaylistClick = onPlaylistClick,
                    onCreated = onPlaylistCreated
                )
            } else {
                FavoritesScreen(
                    modifier = Modifier.fillMaxSize(),
                    playerViewModel = playerViewModel,
                    onSongClick = onSongClick,
                    onBrowse = onBrowseExplore,
                    onSearchClick = onSearchClick,
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}

@Composable
private fun LibraryHubPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(CircleShape)
            .then(
                if (isSelected) {
                    Modifier.background(Brush.horizontalGradient(RikoPrimaryGradient))
                } else {
                    Modifier.background(Color.Transparent)
                }
            )
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { selected = isSelected }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = RikoTypography.Label.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = if (isSelected) RikoTextOnAccent else RikoTextSecondary
        )
    }
}
