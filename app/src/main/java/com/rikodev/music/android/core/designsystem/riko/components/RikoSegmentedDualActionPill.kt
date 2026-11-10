package com.rikodev.music.android.core.designsystem.riko.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.theme.RikoComponentSize
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlass
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlassBorder
import com.rikodev.music.android.core.designsystem.riko.theme.RikoIconSize
import com.rikodev.music.android.core.designsystem.riko.theme.RikoPrimaryGradient
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextOnAccent
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — SEGMENTED DUAL ACTION PILL (SM-UI-SPEC-001 v2.0, Section 2.9)
//
// "Shuffle | Play" — one outer glass pill (height 56, radius-full):
//   • Left inner segment: Aurora gradient pill, inset 4dp from the outer
//     pill's top/bottom/left, width ~52%, shuffle icon 16dp + "Shuffle"
//     Button-Label, 6dp icon-text gap.
//   • Right side: "Play" text + small filled play-icon-in-circle (16dp),
//     centered in the remaining transparent space, text-primary.
//
// D3 FINALIZED: icons are ALWAYS ON (no variant toggle — one component).
// ─────────────────────────────────────────────────────────────────────────────

private val DualPillHeight = 56.dp
private val DualPillInset = 4.dp
private val DualPillSegmentWidthFraction = 0.52f
private val DualPillIconSize = 16.dp
private val DualPillIconTextGap = 6.dp

@Composable
fun RikoSegmentedDualActionPill(
    primaryLabel: String,
    secondaryLabel: String,
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    primaryIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Rounded.Shuffle,
    secondaryIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Rounded.PlayArrow
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(DualPillHeight)
            .clip(CircleShape)
            .background(RikoGlass)
            .border(1.dp, RikoGlassBorder, CircleShape)
            .padding(DualPillInset),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Aurora-filled Shuffle segment (~52% width).
        Row(
            modifier = Modifier
                .weight(DualPillSegmentWidthFraction)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(Brush.horizontalGradient(RikoPrimaryGradient))
                .clickable(onClick = onPrimaryClick),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = primaryIcon,
                contentDescription = null,
                tint = RikoTextOnAccent,
                modifier = Modifier.size(DualPillIconSize)
            )
            Spacer(Modifier.width(DualPillIconTextGap))
            Text(primaryLabel, style = RikoTypography.Button, color = RikoTextOnAccent)
        }

        // Right: plain "Play" + play-in-circle, centered in remaining space.
        Row(
            modifier = Modifier
                .weight(1f - DualPillSegmentWidthFraction)
                .fillMaxHeight()
                .clickable(onClick = onSecondaryClick),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(secondaryLabel, style = RikoTypography.Button, color = RikoTextPrimary)
            Spacer(Modifier.width(DualPillIconTextGap))
            Box(
                modifier = Modifier
                    .size(DualPillIconSize)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = secondaryIcon,
                    contentDescription = null,
                    tint = RikoTextPrimary,
                    modifier = Modifier.size(DualPillIconSize * 0.6f)
                )
            }
        }
    }
}
