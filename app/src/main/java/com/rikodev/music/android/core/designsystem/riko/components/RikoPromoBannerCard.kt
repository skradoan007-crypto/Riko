package com.rikodev.music.android.core.designsystem.riko.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.rikodev.music.android.core.designsystem.riko.theme.RikoBannerScrim
import com.rikodev.music.android.core.designsystem.riko.theme.RikoComponentSize
import com.rikodev.music.android.core.designsystem.riko.theme.RikoCornerRadius
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextOnAccent
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextSecondary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — PROMO BANNER CARD (SM-UI-SPEC-001 v2.0, Section 2.17)
//
// Full width, height 168-180dp, radius-lg (20dp), overflow hidden.
// Photo anchored right; blue-to-transparent scrim (#1B4FBF) over the LEFT
// ~65% for text legibility. Content (left, 20dp padding): Title-M heading
// (up to 2 lines), 4dp gap, Caption subtext (2 lines), 12dp gap, compact
// PrimaryButton (36-40dp).
// ─────────────────────────────────────────────────────────────────────────────

private val PromoBannerHeight = 176.dp
private val PromoScrimWidthFraction = 0.65f
private val PromoPlayIconSize = 16.dp
private val PromoPlayIconGap = 6.dp

@Composable
fun RikoPromoBannerCard(
    heading: String,
    subtext: String,
    ctaLabel: String,
    artworkModel: Any?,
    onCtaClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCardClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(PromoBannerHeight)
            .clip(RoundedCornerShape(RikoCornerRadius.LG))
            .then(
                if (onCardClick != null) {
                    Modifier.clickable(onClick = onCardClick)
                } else Modifier
            )
    ) {
        // Photo anchored right.
        if (artworkModel != null) {
            RikoArtwork(
                model = artworkModel,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )
        }

        // Blue scrim fading to transparent over the LEFT ~65%.
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(PromoScrimWidthFraction)
                .align(Alignment.CenterStart)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            RikoBannerScrim.copy(alpha = 0.92f),
                            RikoBannerScrim.copy(alpha = 0.0f)
                        )
                    )
                )
        )

        // Content — left-aligned, 20dp padding.
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(20.dp)
                .fillMaxWidth(0.62f),
            verticalArrangement = Arrangement.spacedBy(RikoSpacing.XS)
        ) {
            Text(
                text = heading,
                style = RikoTypography.Section,
                color = RikoTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtext,
                style = RikoTypography.Caption,
                color = RikoTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(RikoSpacing.MD))
            RikoPrimaryButton(
                text = ctaLabel,
                onClick = onCtaClick,
                leadingIcon = Icons.Rounded.PlayArrow,
                compact = true
            )
        }
    }
}
