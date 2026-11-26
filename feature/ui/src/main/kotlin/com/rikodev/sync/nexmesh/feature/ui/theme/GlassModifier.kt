package com.rikodev.sync.nexmesh.feature.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Version-aware Liquid Glass modifier optimized for 60 FPS across API 24–35.
 * Uses crisp specular top-left white refraction rim and airy translucent layering
 * without heavy full-screen blur shaders that drop frames on 2GB RAM devices.
 */
fun Modifier.liquidGlassSurface(
    shape: Shape = RoundedCornerShape(24.dp),
    topColor: Color = NexMeshColors.GlassSurface,
    bottomColor: Color = NexMeshColors.GlassSurfaceSoft,
    elevation: Dp = 8.dp
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = Color(0x183B4A6B),
        spotColor = Color(0x1A3B4A6B)
    )
    .clip(shape)
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(topColor, bottomColor)
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            colors = listOf(
                NexMeshColors.GlassSpecularTop,
                NexMeshColors.GlassBorderCool
            )
        ),
        shape = shape
    )
