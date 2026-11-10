package com.rikodev.music.android.presentation.home.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.components.RikoPrimaryButton
import com.rikodev.music.android.core.designsystem.riko.components.RikoSecondaryButton
import com.rikodev.music.android.core.designsystem.riko.theme.RikoCornerRadius
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlass
import com.rikodev.music.android.core.designsystem.riko.theme.RikoShape
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSurface
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextTertiary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography
import com.rikodev.music.android.core.designsystem.riko.theme.RikoWarning

// ─────────────────────────────────────────────────────────────────────────────
// RIKO HOME — STATE VIEWS (PLAN 3, spec §37–41)
//
// Loading: restrained DARK skeleton shimmer (no bright white, spec §37).
// Empty: honest message + existing scanner action (no fake content, §17/§39).
// Error: retry wired to existing retry flow (§41).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "rikoSkeleton")
    val shimmer by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rikoShimmer"
    )
    // Dark shimmer: whisper white on near-black. The old math drove
    // white up to 45% alpha — flat gray blocks instead of a loading shimmer.
    val base = RikoSurface.copy(alpha = 0.10f + 0.06f * shimmer)
    val soft = RikoGlass.copy(alpha = 0.05f + 0.03f * shimmer)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RikoSpacing.LG)
    ) {
        Box(
            Modifier
                .width(180.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(50))
                .background(soft)
        )
        Spacer(Modifier.height(RikoSpacing.XXL))
        Box(
            Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RikoShape.CardLarge)
                .background(base)
        )
        Spacer(Modifier.height(RikoSpacing.XXXL))
        Row(horizontalArrangement = Arrangement.spacedBy(RikoSpacing.MD)) {
            repeat(3) {
                Box(
                    Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(RikoCornerRadius.SM))
                        .background(soft)
                )
            }
        }
        Spacer(Modifier.height(RikoSpacing.XXXL))
        repeat(3) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = RikoSpacing.SM)
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(soft)
                )
                Spacer(Modifier.width(RikoSpacing.MD))
                Column {
                    Box(
                        Modifier
                            .width(180.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(50))
                            .background(base)
                    )
                    Spacer(Modifier.height(RikoSpacing.XS))
                    Box(
                        Modifier
                            .width(110.dp)
                            .height(11.dp)
                            .clip(RoundedCornerShape(50))
                            .background(soft)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeEmpty(
    onScanRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    StateMessage(
        modifier = modifier,
        icon = {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = RikoTextTertiary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = "Your library is empty",
        message = "Riko couldn't find any music yet.\nAllow music access and scan your library.",
        action = {
            RikoPrimaryButton(
                text = "Scan Music",
                onClick = onScanRequest,
                leadingIcon = Icons.Rounded.Refresh
            )
        }
    )
}

@Composable
fun HomeError(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    StateMessage(
        modifier = modifier,
        icon = {
            Icon(
                Icons.Rounded.Warning,
                contentDescription = null,
                tint = RikoWarning,
                modifier = Modifier.size(48.dp)
            )
        },
        title = "Couldn't load your music",
        message = "Something went wrong. Please try again.",
        action = { RikoSecondaryButton(text = "Retry", onClick = onRetry) }
    )
}

@Composable
private fun StateMessage(
    icon: @Composable () -> Unit,
    title: String,
    message: String,
    action: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RikoSpacing.LG, vertical = RikoSpacing.Huge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        icon()
        Spacer(Modifier.height(RikoSpacing.LG))
        Text(text = title, style = RikoTypography.Title, color = RikoTextPrimary)
        Spacer(Modifier.height(RikoSpacing.XS))
        Text(
            text = message,
            style = RikoTypography.Secondary,
            color = RikoTextTertiary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(RikoSpacing.XXL))
        action()
    }
}