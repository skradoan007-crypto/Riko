package com.rikodev.music.android.presentation.collection.components
import androidx.compose.ui.unit.dp

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import com.rikodev.music.android.core.designsystem.riko.theme.RikoCornerRadius
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlass
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSurface
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextTertiary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — COLLECTION STATES (PLAN 5, spec §22–25)
//
// Loading: hero + metadata + track skeletons (dark shimmer — no spinner, §22).
// Empty: entity-specific honest message (§23) — wired by each screen.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CollectionSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "collectionSkeleton")
    val shimmer by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "collectionShimmer"
    )
    // Dark shimmer (see HomeSkeleton): low white alphas, never gray blocks.
    val base = RikoSurface.copy(alpha = 0.10f + 0.06f * shimmer)
    val soft = RikoGlass.copy(alpha = 0.05f + 0.03f * shimmer)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RikoSpacing.LG),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(180.dp)
                .clip(RoundedCornerShape(RikoCornerRadius.XL))
                .background(base)
        )
        Spacer(Modifier.height(RikoSpacing.LG))
        Box(
            Modifier
                .width(160.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(50))
                .background(soft)
        )
        Spacer(Modifier.height(RikoSpacing.SM))
        Box(
            Modifier
                .width(100.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(50))
                .background(soft)
        )
        Spacer(Modifier.height(RikoSpacing.XXL))
        repeat(4) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = RikoSpacing.SM)
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(RikoCornerRadius.SM))
                        .background(soft)
                )
                Spacer(Modifier.width(RikoSpacing.MD))
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        Modifier
                            .height(14.dp)
                            .fillMaxWidth(0.6f)
                            .clip(RoundedCornerShape(50))
                            .background(base)
                    )
                    Spacer(Modifier.height(RikoSpacing.XS))
                    Box(
                        Modifier
                            .height(11.dp)
                            .fillMaxWidth(0.35f)
                            .clip(RoundedCornerShape(50))
                            .background(soft)
                    )
                }
            }
        }
    }
}

@Composable
fun CollectionEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RikoSpacing.LG, vertical = RikoSpacing.XXXL),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, style = RikoTypography.Section, color = RikoTextPrimary)
        Spacer(Modifier.height(RikoSpacing.XS))
        Text(
            text = message,
            style = RikoTypography.Secondary,
            color = RikoTextTertiary,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(RikoSpacing.LG))
            com.rikodev.music.android.core.designsystem.riko.components.RikoSecondaryButton(
                text = actionLabel,
                onClick = onAction
            )
        }
    }
}