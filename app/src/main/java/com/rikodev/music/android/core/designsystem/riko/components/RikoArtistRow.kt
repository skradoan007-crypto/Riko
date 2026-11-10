package com.rikodev.music.android.core.designsystem.riko.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.theme.RikoComponentSize
import com.rikodev.music.android.core.designsystem.riko.theme.RikoCornerRadius
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlassBorder
import com.rikodev.music.android.core.designsystem.riko.theme.RikoPrimaryGradient
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSurfaceElevated
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextOnAccent
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextSecondary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — ARTIST ROW (SM-UI-SPEC-001 v2.0, Section 2.22)
//
// Square photo 56x56 radius-md (16dp — NOT circular) — 12dp gap — text column
// (Body-Bold name, 2dp gap, Caption "{count} Followers") — trailing compact
// secondary "Follow" button (~36 tall, ~80dp wide).
//
// Follow toggled state: invert to solid Primary Gradient fill (spec 2.8
// recommended feedback).
// ─────────────────────────────────────────────────────────────────────────────

private val FollowButtonWidth = 80.dp
private val FollowButtonHeight = 36.dp
private val ArtistRowArtworkSize = 56.dp

@Composable
fun RikoArtistRow(
    name: String,
    followersText: String,
    artworkModel: Any?,
    isFollowing: Boolean,
    onFollowToggle: () -> Unit,
    modifier: Modifier = Modifier,
    onRowClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = RikoComponentSize.ListRowContentHeight)
            .clickable(enabled = onRowClick != null) { onRowClick?.invoke() }
            .padding(vertical = RikoSpacing.SM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Square photo, radius-md 16dp — square-rounded, not circular (spec).
        Box(
            modifier = Modifier
                .size(ArtistRowArtworkSize)
                .clip(RoundedCornerShape(RikoCornerRadius.MD))
                .background(RikoSurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            if (artworkModel != null) {
                RikoArtwork(
                    model = artworkModel,
                    contentDescription = name,
                    modifier = Modifier.size(ArtistRowArtworkSize)
                )
            } else {
                Text(
                    text = name.take(1).uppercase(),
                    style = RikoTypography.Section,
                    color = RikoTextSecondary
                )
            }
        }
        Spacer(Modifier.width(RikoComponentSize.SongRowInternalGap))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = name,
                style = RikoTypography.Body,
                color = RikoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = followersText,
                style = RikoTypography.Caption,
                color = RikoTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(RikoSpacing.SM))
        RikoFollowButton(following = isFollowing, onClick = onFollowToggle)
    }
}

/** Compact Follow pill — glass outline → aurora gradient once followed (2.8). */
@Composable
private fun RikoFollowButton(
    following: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(FollowButtonWidth)
            .height(FollowButtonHeight)
            .clip(RoundedCornerShape(50))
            .then(
                if (following) {
                    Modifier.background(Brush.horizontalGradient(RikoPrimaryGradient))
                } else {
                    Modifier
                        .background(Color.Transparent)
                        .border(1.dp, RikoGlassBorder, RoundedCornerShape(50))
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (following) "Following" else "Follow",
            style = RikoTypography.Button,
            color = if (following) RikoTextOnAccent else RikoTextPrimary,
            maxLines = 1
        )
    }
}
