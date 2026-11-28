package com.rikodev.sync.nexmesh.feature.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun NexMeshTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) NexMeshDarkPalette else NexMeshLightPalette
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = palette.accentPrimary,
            onPrimary = Color.White,
            secondary = palette.switchActiveTrack,
            background = palette.bgMid,
            onBackground = palette.inkPrimary,
            surface = palette.glassSurfaceTop,
            onSurface = palette.inkPrimary,
            surfaceVariant = palette.glassSurfaceBottom,
            onSurfaceVariant = palette.inkSecondary,
            error = palette.statusOffline
        )
    } else {
        lightColorScheme(
            primary = palette.accentPrimary,
            onPrimary = Color.White,
            secondary = palette.switchActiveTrack,
            background = palette.bgMid,
            onBackground = palette.inkPrimary,
            surface = palette.glassSurfaceTop,
            onSurface = palette.inkPrimary,
            surfaceVariant = palette.glassSurfaceBottom,
            onSurfaceVariant = palette.inkSecondary,
            error = palette.statusOffline
        )
    }

    val typography = Typography(
        headlineMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 26.sp,
            letterSpacing = (-0.5).sp,
            color = palette.inkPrimary
        ),
        titleLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            letterSpacing = (-0.3).sp,
            color = palette.inkPrimary
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = palette.inkPrimary
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = palette.inkSecondary
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = palette.inkAccent
        )
    )

    CompositionLocalProvider(LocalNexMeshPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}

/**
 * Multi-layered atmospheric backdrop with soft diffused optical orbs
 * giving translucent Liquid Glass cards true depth and contrast in both Light and Dark themes.
 */
@Composable
fun LiquidGlassBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalNexMeshPalette.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(palette.bgTop, palette.bgMid, palette.bgBottom)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.ambientOrbA, Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.14f),
                    radius = size.width * 0.72f
                ),
                radius = size.width * 0.72f,
                center = Offset(size.width * 0.85f, size.height * 0.14f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.ambientOrbB, Color.Transparent),
                    center = Offset(size.width * 0.12f, size.height * 0.62f),
                    radius = size.width * 0.78f
                ),
                radius = size.width * 0.78f,
                center = Offset(size.width * 0.12f, size.height * 0.62f)
            )
        }
        content()
    }
}
