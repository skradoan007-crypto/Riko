package com.rikodev.music.android.core.designsystem.riko.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.theme.RikoComponentSize
import com.rikodev.music.android.core.designsystem.riko.theme.RikoCornerRadius
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlassBorder
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlassStrong
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMotion
import com.rikodev.music.android.core.designsystem.riko.theme.RikoPrimaryGradient
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextSecondary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — SEGMENTED CONTROL
//
// Glass pill container (White 8.5%) with an animated purple-gradient pill for
// the selected segment. 220ms transition, no bounce.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RikoSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(RikoComponentSize.CompactButtonHeight)
            .clip(RoundedCornerShape(50))
            .background(RikoGlassStrong)
            .border(1.dp, RikoGlassBorder, RoundedCornerShape(50))
            .padding(RikoSpacing.XS)
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = index == selectedIndex
            val selectFraction by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = RikoMotion.emphasized(),
                label = "rikoSegmentPill"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) RikoTextPrimary else RikoTextSecondary,
                animationSpec = RikoMotion.emphasized(),
                label = "rikoSegmentText"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(RikoCornerRadius.Pill))
                    .background(
                        Brush.linearGradient(
                            RikoPrimaryGradient.map { it.copy(alpha = selectFraction * 0.9f) }
                        )
                    )
                    .clickable(
                        enabled = enabled && !isSelected,
                        onClick = { onOptionSelected(index) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    style = RikoTypography.Label,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}