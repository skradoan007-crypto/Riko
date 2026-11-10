package com.rikodev.music.android.core.designsystem.riko.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.theme.RikoShape
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSurfaceElevated
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextSecondary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — GRID CARD (canonical replacement for V2PlaylistCard, D-9 tail)
//
// Square artwork + title + subtitle + overlaid play affordance. Artwork goes
// through the canonical RikoArtwork (§76 — no raw AsyncImage); every value is
// a RIKO token (RikoShape / RikoSurface / RikoTypography).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RikoPlaylistCard(
    title: String,
    subtitle: String,
    artworkModel: Any?,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.clickable(role = Role.Button, onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RikoShape.Card)
                .background(RikoSurfaceElevated)
        ) {
            RikoArtwork(
                model = artworkModel,
                contentDescription = "Artwork for $title",
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RikoShape.Pill)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(role = Role.Button, onClick = onPlay),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Play $title",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(
            text = title,
            style = RikoTypography.Body,
            color = RikoTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = subtitle,
            style = RikoTypography.Secondary,
            color = RikoTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
