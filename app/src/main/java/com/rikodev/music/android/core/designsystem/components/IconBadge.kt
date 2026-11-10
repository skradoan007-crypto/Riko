/*
 * RIKO EXPERIENCE OS — DESIGN SYSTEM
 * IconBadge.kt
 *
 * A colored rounded-square icon container. Each item gets its own
 * semantic color (Battery Saver = green, Light theme = orange, etc.) —
 * matching the reference design where icons are never a flat mono
 * icon set. Reusable across Settings, Library, and anywhere else this
 * pattern shows up.
 *
 * © RIKO Experience OS — Design System
 */

package com.rikodev.music.android.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.theme.RikoDimens

/**
 * A colored rounded-square badge with a centered icon.
 *
 * @param tint The badge's semantic color (background derives from this
 * at low alpha; the icon itself uses full-strength tint).
 * @param size Overall badge size. Defaults to a comfortable row-icon size.
 */
@Composable
fun IconBadge(
    icon: ImageVector,
    tint: Color,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}
