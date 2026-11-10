package com.rikodev.music.android.core.designsystem.riko.components

import com.rikodev.music.android.core.designsystem.riko.theme.RikoStateTokens
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.gesture.RikoTouch
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlass
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlassBorder
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMotion
import com.rikodev.music.android.core.designsystem.riko.theme.RikoPurple
import com.rikodev.music.android.core.designsystem.riko.theme.RikoShape
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextSecondary

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — ICON BUTTON (SM-UI-SPEC-001 v2.0, Section 2.10)
//
// 44dp circular touch target, 20dp icon. Optional glass container.
// Active state → purple tint (accent rule: only for active/selected actions).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RikoIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Purple tint — reserve for active/selected/primary actions. */
    active: Boolean = false,
    /** Glass circle background behind the icon. */
    container: Boolean = true,
    enabled: Boolean = true,
    tint: Color? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) RikoStateTokens.PressScale else 1f,
        animationSpec = RikoMotion.fast(),
        label = "rikoIconPress"
    )

    val resolvedTint = tint
        ?: when {
            !enabled -> RikoTextSecondary.copy(alpha = 0.4f)
            active -> RikoPurple
            else -> RikoTextPrimary  // spec 2.10: default tint = text-primary (100% white)
        }

    var base = modifier
        .size(RikoTouch.TouchTarget) // plan R6: 48dp minimum target (was 44)
        .scale(scale)
        .clip(RikoShape.Circle)
    if (container) {
        base = base
            .background(RikoGlass)
            .border(1.dp, RikoGlassBorder, RikoShape.Circle)
    }
    base = base.clickable(
        interactionSource = interaction,
        indication = null,
        enabled = enabled,
        onClick = onClick
    )

    Box(modifier = base, contentAlignment = Alignment.Center) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = resolvedTint,
            modifier = Modifier.size(20.dp)  // spec 2.10: 20dp icon
        )
    }
}