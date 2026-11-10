package com.rikodev.music.android.presentation.mymusic.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlass
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlassBorder
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMotion
import com.rikodev.music.android.core.designsystem.riko.theme.RikoPrimaryGradient
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextSecondary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — LIBRARY MODE SELECTOR (PLAN 4, spec §4–5)
//
// Horizontally scrollable chip selector (5 tabs squeezed = tiny text FORBIDDEN).
// Selected chip = purple gradient (accent rule); unselected = restrained glass.
// This switches CONTENT MODE only — it is NOT a second bottom navigation.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LibraryModeSelector(
    selectedMode: com.rikodev.music.android.presentation.mymusic.LibraryMode,
    onModeSelected: (com.rikodev.music.android.presentation.mymusic.LibraryMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = RikoSpacing.LG),
        horizontalArrangement = Arrangement.spacedBy(RikoSpacing.SM)
    ) {
        com.rikodev.music.android.presentation.mymusic.LibraryMode.entries.forEach { mode ->
            val isSelected = mode == selectedMode
            val textColor by animateColorAsState(
                targetValue = if (isSelected) com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
                else RikoTextSecondary,
                animationSpec = RikoMotion.normal(),
                label = "modeChipText"
            )
            Box(
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(50))
                    .then(
                        if (isSelected) {
                            Modifier.background(
                                Brush.linearGradient(RikoPrimaryGradient)
                            )
                        } else {
                            Modifier
                                .background(RikoGlass)
                                .border(1.dp, RikoGlassBorder, RoundedCornerShape(50))
                        }
                    )
                    .clickable(enabled = !isSelected) { onModeSelected(mode) }
                    .padding(horizontal = RikoSpacing.LG, vertical = RikoSpacing.SM),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.label,
                    style = RikoTypography.Label,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}