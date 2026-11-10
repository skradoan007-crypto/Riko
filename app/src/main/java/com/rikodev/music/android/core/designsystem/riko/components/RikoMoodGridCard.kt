package com.rikodev.music.android.core.designsystem.riko.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.rikodev.music.android.core.designsystem.riko.theme.RikoCornerRadius
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextSecondary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — MOOD GRID CARD (SM-UI-SPEC-001 v2.0, Section 2.19)
//
// 2-column grid card, ~169x180dp, radius-lg (20dp), overflow hidden.
// Full-bleed photo background. Bottom 45%: black-to-transparent scrim.
// Content bottom-left (12-16dp padding): Body-Bold title (1 line), 2dp gap,
// Caption subtitle.
//
// Badge variant: the "Gold Record" one-off exception uses an inline badge chip
// instead of title+subtitle (spec 3.3 — do NOT generalize to other cards).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RikoMoodGridCard(
    title: String,
    subtitle: String?,
    artworkModel: Any?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeLabel: String? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(169f / 180f)
            .clip(RoundedCornerShape(RikoCornerRadius.LG))
            .clickable(onClick = onClick)
    ) {
        if (artworkModel != null) {
            RikoArtwork(
                model = artworkModel,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Token-derived placeholder while artwork resolves.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.10f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        )
                    )
            )
        }

        // Bottom 45% legibility scrim (black → transparent).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.55f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.85f)
                    )
                )
        )

        // Content bottom-left, 12dp padding.
        if (badgeLabel != null) {
            // Gold Record one-off: inline badge chip instead of title+subtitle.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(RikoCornerRadius.Pill))
                    .background(Color.White.copy(alpha = 0.16f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = badgeLabel,
                    style = RikoTypography.Label,
                    color = RikoTextPrimary,
                    maxLines = 1
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = RikoTypography.Body,
                    color = RikoTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = RikoTypography.Caption,
                        color = RikoTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
