package com.rikodev.music.android.core.designsystem.riko.components

import com.rikodev.music.android.core.designsystem.riko.theme.RikoCornerRadius
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlass
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlassBorder
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlassBorderActive
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlassStrong

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — GLASS SURFACE
//
// Subtle frosted container. Two intensities only:
//   subtle = White 5.5% · strong = White 8.5% · border = White 10%
//
// Anti-patterns enforced (Plan 1 hard bans):
//   - Glass must NEVER be the first thing the eye notices
//   - No inner glow, no fake reflection, no excessive blur layers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RikoGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(RikoCornerRadius.LG),
    /** false = White 5.5% (subtle) · true = White 8.5% (strong). */
    strong: Boolean = false,
    /** Active/selected border → purple 45%. */
    borderActive: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (strong) RikoGlassStrong else RikoGlass)
            .border(
                width = 1.dp,
                color = if (borderActive) RikoGlassBorderActive else RikoGlassBorder,
                shape = shape
            )
    ) {
        content()
    }
}