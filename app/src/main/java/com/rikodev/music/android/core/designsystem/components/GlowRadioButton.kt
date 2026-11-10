/*
 * RIKO EXPERIENCE OS — DESIGN SYSTEM
 * GlowRadioButton.kt
 *
 * A radio button that glows when selected, instead of Material3's flat
 * default. Selected state gets a soft colored halo behind the ring —
 * unselected stays a plain outline. Real state, real transition (real
 * cause for the glow: this option IS selected — not decoration).
 *
 * © RIKO Experience OS — Design System
 */

package com.rikodev.music.android.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.theme.AccentViolet
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMotion
import com.rikodev.music.android.core.designsystem.theme.GlassBorderStrong

@Composable
fun GlowRadioButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = AccentViolet,
) {
    val ringColor by animateColorAsState(
        targetValue = if (selected) accent else GlassBorderStrong,
        animationSpec = RikoMotion.normal(),
        label = "radio_ring_color"
    )
    val dotSize by animateDpAsState(
        targetValue = if (selected) 10.dp else 0.dp,
        animationSpec = RikoMotion.normal(),
        label = "radio_dot_size"
    )

    Box(
        modifier = modifier
            .size(24.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Soft glow halo — only present when selected, so it's a real
        // signal of state, not permanent decoration.
        if (selected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .blur(10.dp)
                    .background(accent.copy(alpha = 0.55f), CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .size(22.dp)
                .border(2.dp, ringColor, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(dotSize)
                .background(accent, CircleShape)
        )
    }
}
