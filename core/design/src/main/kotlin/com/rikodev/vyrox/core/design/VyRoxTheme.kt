package com.rikodev.vyrox.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object VyRoxPalette {
    // Brand signature gradient (extracted from VyRox icon & target UI)
    val Magenta = Color(0xFFE026FF)
    val Violet = Color(0xFF6C3BFF)
    val ElectricBlue = Color(0xFF2979FF)
    val Cyan = Color(0xFF00E5FF)
    val Amber = Color(0xFFFFB020)

    // Light Liquid Glass canvas
    val LightBgTop = Color(0xFFF6F8FF)
    val LightBgMid = Color(0xFFEDF2FF)
    val LightBgBottom = Color(0xFFE4EAFC)
    val LightInk = Color(0xFF0E1324)
    val LightInkMuted = Color(0xFF56607B)
    val LightGlassFill = Color(0xCCFFFFFF)
    val LightGlassBorderTop = Color(0xF2FFFFFF)
    val LightGlassBorderBottom = Color(0x337B96D4)

    // Dark Liquid Glass canvas
    val DarkBgTop = Color(0xFF060A14)
    val DarkBgMid = Color(0xFF0B1224)
    val DarkBgBottom = Color(0xFF101933)
    val DarkInk = Color(0xFFF4F7FF)
    val DarkInkMuted = Color(0xFF9AA8C7)
    val DarkGlassFill = Color(0x16FFFFFF)
    val DarkGlassBorderTop = Color(0x38FFFFFF)
    val DarkGlassBorderBottom = Color(0x1800E5FF)
}

@Immutable
data class VyRoxGlassTokens(
    val isDark: Boolean,
    val backgroundBrush: Brush,
    val glassFill: Color,
    val glassBorderBrush: Brush,
    val brandGradient: Brush,
    val fabGradient: Brush,
    val heroBannerBrush: Brush,
    val inkPrimary: Color,
    val inkSecondary: Color
)

private val LightGlassTokens = VyRoxGlassTokens(
    isDark = false,
    backgroundBrush = Brush.verticalGradient(
        listOf(VyRoxPalette.LightBgTop, VyRoxPalette.LightBgMid, VyRoxPalette.LightBgBottom)
    ),
    glassFill = VyRoxPalette.LightGlassFill,
    glassBorderBrush = Brush.linearGradient(
        listOf(VyRoxPalette.LightGlassBorderTop, VyRoxPalette.LightGlassBorderBottom)
    ),
    brandGradient = Brush.horizontalGradient(
        listOf(VyRoxPalette.Magenta, VyRoxPalette.Violet, VyRoxPalette.Cyan)
    ),
    fabGradient = Brush.linearGradient(
        listOf(VyRoxPalette.Magenta, VyRoxPalette.Violet)
    ),
    heroBannerBrush = Brush.linearGradient(
        listOf(Color(0xFF193773), Color(0xFF28559A), Color(0xFF4E6DB8))
    ),
    inkPrimary = VyRoxPalette.LightInk,
    inkSecondary = VyRoxPalette.LightInkMuted
)

private val DarkGlassTokens = VyRoxGlassTokens(
    isDark = true,
    backgroundBrush = Brush.verticalGradient(
        listOf(VyRoxPalette.DarkBgTop, VyRoxPalette.DarkBgMid, VyRoxPalette.DarkBgBottom)
    ),
    glassFill = VyRoxPalette.DarkGlassFill,
    glassBorderBrush = Brush.linearGradient(
        listOf(VyRoxPalette.DarkGlassBorderTop, VyRoxPalette.DarkGlassBorderBottom)
    ),
    brandGradient = Brush.horizontalGradient(
        listOf(VyRoxPalette.Magenta, VyRoxPalette.Violet, VyRoxPalette.Cyan)
    ),
    fabGradient = Brush.linearGradient(
        listOf(VyRoxPalette.Magenta, VyRoxPalette.Violet)
    ),
    heroBannerBrush = Brush.linearGradient(
        listOf(Color(0xFF0A162E), Color(0xFF172C59), Color(0xFF253F7A))
    ),
    inkPrimary = VyRoxPalette.DarkInk,
    inkSecondary = VyRoxPalette.DarkInkMuted
)

val LocalVyRoxGlass = staticCompositionLocalOf { DarkGlassTokens }

private val VyRoxTypography = Typography(
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.4).sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

@Composable
fun VyRoxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val tokens = if (darkTheme) DarkGlassTokens else LightGlassTokens
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = VyRoxPalette.Violet,
            secondary = VyRoxPalette.Cyan,
            tertiary = VyRoxPalette.Magenta,
            background = VyRoxPalette.DarkBgTop,
            surface = VyRoxPalette.DarkBgMid,
            onBackground = VyRoxPalette.DarkInk,
            onSurface = VyRoxPalette.DarkInk
        )
    } else {
        lightColorScheme(
            primary = VyRoxPalette.Violet,
            secondary = VyRoxPalette.ElectricBlue,
            tertiary = VyRoxPalette.Magenta,
            background = VyRoxPalette.LightBgTop,
            surface = Color.White,
            onBackground = VyRoxPalette.LightInk,
            onSurface = VyRoxPalette.LightInk
        )
    }

    CompositionLocalProvider(LocalVyRoxGlass provides tokens) {
        MaterialTheme(
            colorScheme = colors,
            typography = VyRoxTypography,
            content = content
        )
    }
}
