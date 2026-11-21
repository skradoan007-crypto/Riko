package com.rikodev.music.android.core.designsystem.riko.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.theme.RikoCornerRadius
import com.rikodev.music.android.core.designsystem.theme.LocalIsLightTheme

@Composable
fun RikoGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(RikoCornerRadius.LG),
    strong: Boolean = false,
    borderActive: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val isLight = LocalIsLightTheme.current
    val fillColor = when {
        isLight && strong -> Color(0xD9FFFFFF)
        isLight -> Color(0xB8E8FAF4)
        strong -> Color(0xBA0F2B28)
        else -> Color(0x990B2220)
    }
    val borderColor = when {
        isLight && borderActive -> Color(0x880D9488)
        isLight -> Color(0x400D9488)
        borderActive -> Color(0x736EE7B7)
        else -> Color(0x3834D399)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(fillColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = shape
            )
    ) {
        content()
    }
}