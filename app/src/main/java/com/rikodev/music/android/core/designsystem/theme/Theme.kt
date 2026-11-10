package com.rikodev.music.android.core.designsystem.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMaterialTypography

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — DARK THEME ENGINE
// Dark-only, glass-first, premium ambient system.
// ─────────────────────────────────────────────────────────────────────────────

private val RikoDarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = ColorAbyssBlack,
    primaryContainer = Color(0xFF12354A),
    onPrimaryContainer = Color.White,

    secondary = AccentViolet,
    onSecondary = ColorAbyssBlack,
    secondaryContainer = Color(0xFF2B2144),
    onSecondaryContainer = Color.White,

    tertiary = Color(0xFFFF6FD8),
    onTertiary = ColorAbyssBlack,

    background = ColorVoidDark,
    onBackground = TextPrimary,

    surface = ColorDepthDark,
    onSurface = TextPrimary,

    surfaceVariant = ColorSurfaceDark,
    onSurfaceVariant = TextSecondary,

    surfaceTint = GlassWhite08,

    error = ColorError,
    onError = Color.White,

    outline = GlassBorderLight,
    outlineVariant = GlassBorderSubtle,
    scrim = Color(0xCC000000)
)

// ── Dynamic Color State ────────────────────────────────────────────────────────
data class DynamicColors(
    val dominantColor: Color = DynamicColorDefault,
    val vibrantColor: Color = AccentCyan,
    val darkVibrantColor: Color = AccentCyanDim,
    val lightVibrantColor: Color = Color(0xFFBFEAFF),
    val mutedColor: Color = Color(0xFF8DA0C6),
    val darkMutedColor: Color = Color(0xFF60708F)
)

val LocalDynamicColors = compositionLocalOf { DynamicColors() }

@Composable
fun RikoMusicTheme(
    dynamicColors: DynamicColors = DynamicColors(),
    content: @Composable () -> Unit
) {
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.setDecorFitsSystemWindows(window, false)

            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    CompositionLocalProvider(
        LocalDynamicColors provides dynamicColors
    ) {
        MaterialTheme(
            colorScheme = RikoDarkColorScheme,
            typography = RikoMaterialTypography,
            content = content
        )
    }
}