package com.rikodev.ibd3d.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object PanelColors {
    val Background = Color(0xFFF7F5FF)
    val Ink = Color(0xFF1B1730)
    val Muted = Color(0xFF625B71)
    val Border = Color(0xFFE5DEF3)
    val Codes = Color(0xFFF0E6FF)
    val Vehicles = Color(0xFFE8F1FF)
    val Animals = Color(0xFFFFEBF3)
    val Others = Color(0xFFEBEDFF)
    val HeroScrim = Color(0xD91C1036)
    val OnHero = Color.White
}

private val PanelTypography =
    Typography(
        headlineMedium =
            TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
        headlineSmall =
            TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
        titleLarge =
            TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
        bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
        labelLarge =
            TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
        labelMedium =
            TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    )

@Composable
fun PanelTheme(accent: String, content: @Composable () -> Unit) {
    val primary by
        animateColorAsState(
            targetValue =
                when (accent) {
                    "Blue" -> Color(0xFF2563EB)
                    "Rose" -> Color(0xFFBE185D)
                    else -> Color(0xFF7C3AED)
                },
            animationSpec = tween(180),
            label = "accent",
        )
    val container by
        animateColorAsState(
            when (accent) {
                "Blue" -> Color(0xFFE7EFFF)
                "Rose" -> Color(0xFFFCE7F0)
                else -> Color(0xFFEFE5FF)
            },
            animationSpec = tween(180),
            label = "accent container",
        )
    MaterialTheme(
        colorScheme =
            lightColorScheme(
                primary = primary,
                onPrimary = Color.White,
                primaryContainer = container,
                onPrimaryContainer = primary,
                background = PanelColors.Background,
                onBackground = PanelColors.Ink,
                surface = Color.White,
                onSurface = PanelColors.Ink,
                onSurfaceVariant = PanelColors.Muted,
                outlineVariant = PanelColors.Border,
            ),
        typography = PanelTypography,
        shapes =
            Shapes(
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(18.dp),
                large = RoundedCornerShape(24.dp),
            ),
        content = content,
    )
}
