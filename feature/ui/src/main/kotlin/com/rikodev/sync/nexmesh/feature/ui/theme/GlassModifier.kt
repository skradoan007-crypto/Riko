package com.rikodev.sync.nexmesh.feature.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Refined Liquid Glass surface modifier with top-edge specular reflection,
 * fine 1dp refractive rim gradient, and soft layered depth — 60 FPS across API 24–35.
 */
fun Modifier.liquidGlassSurface(
    shape: Shape = RoundedCornerShape(22.dp),
    topColor: Color = NexMeshColors.GlassSurface,
    bottomColor: Color = NexMeshColors.GlassSurfaceSoft,
    rimTop: Color = NexMeshColors.GlassSpecularTop,
    rimBottom: Color = NexMeshColors.GlassBorderCool,
    shadowColor: Color = Color(0x16243658),
    elevation: Dp = 8.dp
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = shadowColor,
        spotColor = shadowColor
    )
    .clip(shape)
    .background(
        brush = Brush.verticalGradient(colors = listOf(topColor, bottomColor))
    )
    .drawWithContent {
        drawContent()
        // Subtle top glass reflection sheen
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(rimTop.copy(alpha = 0.22f), Color.Transparent),
                startY = 0f,
                endY = size.height * 0.28f
            ),
            topLeft = Offset.Zero,
            size = size
        )
    }
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(colors = listOf(rimTop, rimBottom)),
        shape = shape
    )
