package com.rikodev.vyrox.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Riko Premium Liquid Glass surface with specular rim border and subtle
 * top-left sheen that stays crisp at 60fps across low-RAM and flagship devices.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val tokens = LocalVyRoxGlass.current
    val shape = RoundedCornerShape(cornerRadius)
    val sheenBrush = Brush.verticalGradient(
        listOf(
            if (tokens.isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.92f),
            if (tokens.isDark) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.68f)
        )
    )

    val base = modifier
        .clip(shape)
        .background(sheenBrush)
        .border(width = 1.dp, brush = tokens.glassBorderBrush, shape = shape)
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }

    Box(modifier = base, content = content)
}
