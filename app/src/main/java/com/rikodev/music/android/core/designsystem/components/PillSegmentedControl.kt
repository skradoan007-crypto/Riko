/*
 * RIKO EXPERIENCE OS — DESIGN SYSTEM
 * PillSegmentedControl.kt
 *
 * Horizontal row of text options with one pill-highlighted selection —
 * the "Off / Low / Medium / High" style control from the reference
 * design. Generic over any label list, reusable wherever this pattern
 * is needed (Glass Intensity, Motion Intensity, and similar).
 *
 * © RIKO Experience OS — Design System
 */

package com.rikodev.music.android.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.music.android.core.designsystem.theme.AccentViolet
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMotion
import com.rikodev.music.android.core.designsystem.riko.theme.RikoShape
import com.rikodev.music.android.core.designsystem.theme.TextPrimary
import com.rikodev.music.android.core.designsystem.theme.TextSecondary

/**
 * @param options display labels, in order.
 * @param selectedIndex which option is currently active.
 * @param onSelect called with the tapped option's index.
 */
@Composable
fun <T> PillSegmentedControl(
    options: List<T>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = AccentViolet,
    label: (T) -> String = { it.toString() },
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            val bg by animateColorAsState(
                targetValue = if (selected) accent else Color.Transparent,
                animationSpec = RikoMotion.normal(),
                label = "segment_bg"
            )
            val textColor by animateColorAsState(
                targetValue = if (selected) Color.White else TextSecondary,
                animationSpec = RikoMotion.normal(),
                label = "segment_text"
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(bg, RikoShape.Pill)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(index) }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label(option),
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
