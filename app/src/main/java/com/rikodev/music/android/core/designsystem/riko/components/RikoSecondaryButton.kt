package com.rikodev.music.android.core.designsystem.riko.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.theme.RikoComponentSize
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlassBorder
import com.rikodev.music.android.core.designsystem.riko.theme.RikoIconSize
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMotion
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — SECONDARY BUTTON (SM-UI-SPEC-001 v2.0, Section 2.8)
//
// Glass pill (White 10% + 14% border), NO accent color. 56dp, press 0.97.
// Used for: non-primary actions (Google sign-in, Follow, Cancel).
// Pressed state: glass-fill transitions from 10% → 16% white (100ms).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RikoSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,  // spec 2.8: press scale 0.97
        animationSpec = RikoMotion.fast(),
        label = "rikoSecondaryPress"
    )
    // spec 2.8: glass-fill transitions from 10% → 16% white on press (100ms)
    val glassAlpha by animateFloatAsState(
        targetValue = if (pressed) 0.16f else 0.10f,
        animationSpec = RikoMotion.fast(),
        label = "rikoSecondaryGlass"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .alpha(if (enabled) 1f else 0.45f)
            .height(RikoComponentSize.PrimaryButtonHeight)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = glassAlpha))  // spec 2.8: animated glass fill
            .border(1.dp, RikoGlassBorder, RoundedCornerShape(50))
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = RikoComponentSize.ButtonHorizontalPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RikoSpacing.SM)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = RikoTextPrimary,
                    modifier = Modifier.size(RikoIconSize.Standard)
                )
            }
            Text(
                text = text,
                style = RikoTypography.Button,
                color = RikoTextPrimary
            )
        }
    }
}