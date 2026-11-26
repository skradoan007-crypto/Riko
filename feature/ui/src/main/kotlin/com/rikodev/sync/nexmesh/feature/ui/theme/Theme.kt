package com.rikodev.sync.nexmesh.feature.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val NexMeshLightScheme = lightColorScheme(
    primary = NexMeshColors.MeshNodePrimary,
    onPrimary = Color.White,
    secondary = NexMeshColors.MintTrackActive,
    onSecondary = NexMeshColors.InkPrimary,
    background = NexMeshColors.BgWhisperLavender,
    onBackground = NexMeshColors.InkPrimary,
    surface = NexMeshColors.GlassSurface,
    onSurface = NexMeshColors.InkPrimary,
    surfaceVariant = NexMeshColors.GlassSurfaceSoft,
    onSurfaceVariant = NexMeshColors.InkSecondary,
    error = NexMeshColors.StatusCritical
)

private val NexMeshTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        letterSpacing = (-0.4).sp,
        color = NexMeshColors.InkPrimary
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = (-0.2).sp,
        color = NexMeshColors.InkPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        color = NexMeshColors.InkPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        color = NexMeshColors.InkSecondary
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        color = NexMeshColors.InkAccentIndigo
    )
)

@Composable
fun NexMeshTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NexMeshLightScheme,
        typography = NexMeshTypography,
        content = content
    )
}

/**
 * Full-screen atmospheric pastel Liquid Glass light backdrop matching approved preview.
 */
@Composable
fun LiquidGlassBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        NexMeshColors.BgWarmIvory,
                        NexMeshColors.BgWhisperLavender,
                        NexMeshColors.BgPeriwinkleMist
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(900f, 1900f)
                )
            ),
        content = content
    )
}
