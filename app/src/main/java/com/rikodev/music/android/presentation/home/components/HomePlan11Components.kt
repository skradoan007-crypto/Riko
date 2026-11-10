package com.rikodev.music.android.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import com.rikodev.music.android.core.designsystem.riko.components.ARTWORK_PX_ROW
import com.rikodev.music.android.core.designsystem.riko.components.RikoArtwork
import com.rikodev.music.android.core.designsystem.riko.components.RikoFilterTabPill
import com.rikodev.music.android.core.designsystem.riko.components.RikoIconButton
import com.rikodev.music.android.core.designsystem.riko.components.RikoSectionHeader
import com.rikodev.music.android.core.designsystem.riko.components.RikoTrackRow
import com.rikodev.music.android.core.designsystem.riko.components.rememberAlbumArtworkModel
import com.rikodev.music.android.core.designsystem.riko.theme.RikoAmbientGradient
import com.rikodev.music.android.core.designsystem.riko.theme.RikoCornerRadius
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlass
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlassBorder
import com.rikodev.music.android.core.designsystem.riko.theme.RikoIconSize
import com.rikodev.music.android.core.designsystem.riko.theme.RikoPrimaryGradient
import com.rikodev.music.android.core.designsystem.riko.theme.rikoAuroraBrush
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextSecondary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextTertiary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography
import com.rikodev.music.android.domain.model.Song

// ---------------------------------------------------------------------------
// RIKO - HOME PLAN 11 REFERENCE PARTS
//
// Missing pieces referenced by HomeScreen (Plan 11 reference match):
//   - HomeCategory        : All / Album / Artist / New Release filter chips
//   - HomeCategoryChips   : horizontal scroll pills, active = purple fill
//   - HomeFeaturedBanner  : Top Hits Right Now gradient card + Play Now pill
//   - RecentlyPlayedSection : header + rows with duration + circular play
// All playback goes through the actions supplied by HomeScreen - no new logic.
// ---------------------------------------------------------------------------

enum class HomeCategory(val label: String) {
    ALL("All"),
    ALBUMS("Album"),
    NEW_ARTISTS("New Artist"),
    NEW_RELEASES("New Release")
}

@Composable
fun HomeCategoryChips(
    selected: HomeCategory,
    onSelect: (HomeCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = RikoSpacing.XL),
        horizontalArrangement = Arrangement.spacedBy(RikoSpacing.SM)
    ) {
        items(HomeCategory.entries, key = { it.name }) { category ->
            RikoFilterTabPill(
                label = category.label,
                selected = category == selected,
                onClick = { onSelect(category) }
            )
        }
    }
}

@Composable
fun HomeFeaturedBanner(
    sectionLabel: String,
    title: String,
    subtitle: String,
    artworkModel: Any?,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(RikoCornerRadius.LG))
    ) {
        // Background artwork (safe fallback = pure gradient when absent).
        // Plan 1.2: caller passes the factory-built request (stable identity,
        // hero crossfade) — no request rebuilding here.
        if (artworkModel != null) {
            RikoArtwork(
                model = artworkModel,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
        // Reference tint: dark navy → purple wash (tokenised: ambient pair,
        // same alphas so the artwork still breathes through).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            RikoAmbientGradient[1].copy(alpha = 0.94f),
                            RikoAmbientGradient[0].copy(alpha = 0.78f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(RikoSpacing.LG),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = sectionLabel,
                style = RikoTypography.Secondary,
                color = RikoTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(RikoSpacing.XS))
            Text(
                text = title,
                style = RikoTypography.Title,
                color = RikoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(RikoSpacing.XS))
            Text(
                text = subtitle,
                style = RikoTypography.Secondary,
                color = RikoTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(RikoSpacing.MD))
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(RikoPrimaryGradient))
                    .clickable(onClick = onPlay)
                    .padding(horizontal = RikoSpacing.MD, vertical = RikoSpacing.XS),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RikoSpacing.XS)
            ) {
                Text(
                    text = "Play Now",
                    style = RikoTypography.Button,
                    color = Color.White,
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(RikoIconSize.Compact)
                )
            }
        }
    }
}

@Composable
fun RecentlyPlayedSection(
    songs: List<Song>,
    onSongClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        RikoSectionHeader(
            title = "Recently Played",
            modifier = Modifier.padding(horizontal = RikoSpacing.LG)
        )
        Spacer(Modifier.height(RikoSpacing.SM))
        songs.forEachIndexed { index, song ->
            val durationText = song.durationFormatted
            RikoTrackRow(
                title = song.title,
                subtitle = song.artist,
                artworkModel = rememberAlbumArtworkModel(song.albumId, ARTWORK_PX_ROW, songId = song.id, songUri = song.uri),
                onClick = { onSongClick(index) },
                modifier = Modifier.padding(horizontal = RikoSpacing.SM),
                trailing = {
                    Text(
                        text = durationText,
                        style = RikoTypography.Caption,
                        color = RikoTextTertiary,
                        maxLines = 1
                    )
                    Spacer(Modifier.width(RikoSpacing.XS))
                    RikoIconButton(
                        icon = Icons.Rounded.PlayArrow,
                        contentDescription = "Play " + song.title,
                        onClick = { onSongClick(index) },
                        container = true
                    )
                }
            )
        }
    }
}

