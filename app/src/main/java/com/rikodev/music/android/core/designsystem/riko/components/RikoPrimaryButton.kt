package com.rikodev.music.android.core.designsystem.riko.components

import com.rikodev.music.android.core.designsystem.riko.theme.RikoStateTokens
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.rikodev.music.android.core.designsystem.riko.theme.RikoComponentSize
import com.rikodev.music.android.core.designsystem.riko.theme.RikoIconSize
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMotion
import com.rikodev.music.android.core.designsystem.riko.theme.RikoPrimaryGradient
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — PRIMARY BUTTON (SM-UI-SPEC-001 v2.0, Section 2.7)
//
// The ONE accent CTA: Aurora gradient pill, 56dp, subtle press scale (0.97).
// Used for: Play, Primary CTA, main actions only — never for list rows.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RikoPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    /** Compact 36-40dp variant for in-card CTAs (spec 1.7 small pill). */
    compact: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) RikoStateTokens.PressScale else 1f,
        animationSpec = RikoMotion.fast(),
        label = "rikoPrimaryPress"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .alpha(if (enabled) 1f else 0.40f)  // spec 2.7: disabled = 40% opacity
            .height(
                if (compact) RikoComponentSize.CompactButtonHeight
                else RikoComponentSize.PrimaryButtonHeight
            )
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(RikoPrimaryGradient))
            .then(
                // spec 2.7: pressed state — 8% darken overlay for tactile depth
                if (pressed) Modifier.drawBehind {
                    drawRect(Color.Black.copy(alpha = RikoMotion.PressedOverlayAlpha))
                } else Modifier
            )
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
                    tint = Color.White,
                    modifier = Modifier.size(if (compact) RikoIconSize.Compact else RikoIconSize.Standard)
                )
            }
            Text(
                text = text,
                style = RikoTypography.Button,
                color = Color.White
            )
        }
    }
}