package com.rikodev.music.android.core.designsystem.riko.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.animation.LocalRikoClock
import com.rikodev.music.android.core.designsystem.animation.oscillate
import com.rikodev.music.android.core.designsystem.riko.theme.RikoComponentSize
import com.rikodev.music.android.core.designsystem.riko.theme.RikoCornerRadius
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMotion
import com.rikodev.music.android.core.designsystem.riko.theme.RikoPurple
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextSecondary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — TRACK ROW — **THE canonical song row**
//
// RIKO had two competing song rows (V2TrackRow on Favorites/MyMusic/
// PlaylistDetail/Search, RikoSongRow on Home/Collection) with different
// padding, different artwork containers and different colour sources — the
// exact "two skins, one engine" problem the product standard forbids.
//
// This file is now the single implementation. The two old entry points are
// thin @Deprecated delegates in V2TrackRow.kt / RikoSongRow.kt, so no call
// site had to change and the two can never drift apart again.
//
// API is the UNION of both old APIs:
//   id           -> testTag(V2TestTags.TRACK_ROW_PREFIX + id)      (V2)
//   duration     -> trailing duration text                          (V2)
//   onMenu       -> trailing kebab + "More options for $title"      (V2)
//   artworkModel -> Any? fed to the canonical RikoArtwork           (v1)
//   trailing     -> arbitrary RowScope slot                         (v1)
//
// All visuals now come from RIKO tokens (RikoSpacing / RikoTypography /
// RikoArtwork / RikoPurple) — no RikoThemeV2, no raw AsyncImage — so the row
// cannot drift from the design system again.
//
// BEHAVIOUR CHANGE — NEEDS DEVICE VERIFICATION: V2 call sites previously used
// RikoThemeV2.Spacing.lg (24dp) horizontal padding and a raw 128px AsyncImage;
// they now use RikoSpacing.MD (16dp) and RikoArtwork. Favorites / MyMusic /
// PlaylistDetail / Search rows therefore shift slightly. That is the intended
// unification (Phase 3 DoD: "Home rows identical to Favorites"), but it is a
// visible change and is NOT device-verified.
// ─────────────────────────────────────────────────────────────────────────────

// Plan 1.3: hoisted — allocating a shape per row composition was pure churn.
private val TrackRowShape = RoundedCornerShape(RikoCornerRadius.MD)

@Composable
fun RikoTrackRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    id: Long? = null,
    duration: String? = null,
    artworkModel: Any? = null,
    isPlaying: Boolean = false,
    onClick: () -> Unit = {},
    onMenu: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    val titleColor by animateColorAsState(
        targetValue = if (isPlaying) RikoPurple else RikoTextPrimary,
        animationSpec = RikoMotion.emphasized(),
        label = "rikoTrackRowTitle"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = RikoComponentSize.SongRowHeight)
            .clip(TrackRowShape)
            .then(if (id != null) Modifier.testTag(V2TestTags.TRACK_ROW_PREFIX + id) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = RikoSpacing.MD, vertical = RikoSpacing.SM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            RikoArtwork(model = artworkModel, size = RikoArtworkSize.Thumbnail)
            if (isPlaying) {
                PlayingEqBars(
                    color = RikoPurple,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        Spacer(Modifier.width(RikoComponentSize.SongRowInternalGap))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = RikoTypography.Body,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = RikoTypography.Secondary,
                color = RikoTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (duration != null) {
            Spacer(Modifier.width(RikoSpacing.SM))
            Text(
                text = duration,
                style = RikoTypography.Caption,
                color = RikoTextSecondary,
                maxLines = 1
            )
        }
        if (onMenu != null) {
            Spacer(Modifier.width(RikoSpacing.SM))
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "More options for $title",
                tint = RikoTextSecondary,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(role = Role.Button, onClick = onMenu)
            )
        }
        trailing()
    }
}

/**
 * Live EQ bars for the currently-playing row.
 *
 * Timing comes from the shared [LocalRikoClock] — no private infinite
 * transition per row. The clock is read inside the draw phase, so ticks
 * redraw this tiny Canvas without recomposing the row or the list.
 */
@Composable
private fun PlayingEqBars(
    color: Color,
    modifier: Modifier = Modifier
) {
    val clock = LocalRikoClock.current
    Canvas(modifier = modifier.size(20.dp)) {
        val angle = clock.value
        val barCount = 4
        val gap = 2.dp.toPx()
        val bw = (size.width - gap * (barCount - 1)) / barCount
        for (i in 0 until barCount) {
            val h = size.height * (0.25f + 0.75f * oscillate(angle, i * 180, 900))
            drawRoundRect(
                color = color,
                topLeft = Offset(i * (bw + gap), size.height - h),
                size = Size(bw, h),
                cornerRadius = CornerRadius(bw / 2f, bw / 2f)
            )
        }
    }
}
