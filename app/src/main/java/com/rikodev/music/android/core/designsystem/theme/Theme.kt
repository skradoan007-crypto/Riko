package com.rikodev.music.android.core.designsystem.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
// RIKO MUSIC — EMERALD DARK & EMERALD LIGHT THEME ENGINE
// Supports live reactive switching between Dark, Light, System, and AMOLED.
// ─────────────────────────────────────────────────────────────────────────────

private val RikoEmeraldDarkColorScheme = darkColorScheme(
    primary = Color(0xFF10B981),
    onPrimary = Color(0xFF04201B),
    primaryContainer = Color(0xFF0B3B33),
    onPrimaryContainer = Color(0xFFECFDF5),

    secondary = Color(0xFF6EE7B7),
    onSecondary = Color(0xFF05241E),
    secondaryContainer = Color(0xFF0F2E2A),
    onSecondaryContainer = Color(0xFFECFDF5),

    tertiary = Color(0xFF2DD4BF),
    onTertiary = Color(0xFF04201E),

    background = Color(0xFF061414),
    onBackground = Color(0xFFECFDF5),

    surface = Color(0xFF0B2220),
    onSurface = Color(0xFFECFDF5),

    surfaceVariant = Color(0xFF0F2B28),
    onSurfaceVariant = Color(0xB8ECFDF5),

    surfaceTint = Color(0x1A10B981),

    error = ColorError,
    onError = Color.White,

    outline = Color(0x3834D399),
    outlineVariant = Color(0x226EE7B7),
    scrim = Color(0xCC030C0C)
)

private val RikoEmeraldAmoledColorScheme = RikoEmeraldDarkColorScheme.copy(
    background = Color(0xFF000000),
    surface = Color(0xFF051010),
    surfaceVariant = Color(0xFF081918)
)

private val RikoEmeraldLightColorScheme = lightColorScheme(
    primary = Color(0xFF0D9488),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF062626),

    secondary = Color(0xFF14B8A6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7F5EC),
    onSecondaryContainer = Color(0xFF062626),

    tertiary = Color(0xFF059669),
    onTertiary = Color.White,

    background = Color(0xFFE8FAF4),
    onBackground = Color(0xFF062626),

    surface = Color(0xFFF0FDF9),
    onSurface = Color(0xFF062626),

    surfaceVariant = Color(0xFFD7F5EC),
    onSurfaceVariant = Color(0xFF1F4A4A),

    surfaceTint = Color(0x140D9488),

    error = ColorError,
    onError = Color.White,

    outline = Color(0x450D9488),
    outlineVariant = Color(0x290D9488),
    scrim = Color(0x66062626)
)

// ── Dynamic Color & Theme State ───────────────────────────────────────────────
data class DynamicColors(
    val dominantColor: Color = DynamicColorDefault,
    val vibrantColor: Color = AccentCyan,
    val darkVibrantColor: Color = AccentCyanDim,
    val lightVibrantColor: Color = Color(0xFF6EE7B7),
    val mutedColor: Color = Color(0xFF2DD4BF),
    val darkMutedColor: Color = Color(0xFF0B2220)
)

val LocalDynamicColors = compositionLocalOf { DynamicColors() }
val LocalIsLightTheme = compositionLocalOf { false }

@Composable
fun RikoMusicTheme(
    darkTheme: Boolean = true,
    amoled: Boolean = false,
    dynamicColors: DynamicColors = DynamicColors(),
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val isLight = !darkTheme
    val colorScheme = when {
        isLight -> RikoEmeraldLightColorScheme
        amoled -> RikoEmeraldAmoledColorScheme
        else -> RikoEmeraldDarkColorScheme
    }

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                WindowCompat.setDecorFitsSystemWindows(window, false)

                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = isLight
                    isAppearanceLightNavigationBars = isLight
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalDynamicColors provides dynamicColors,
        LocalIsLightTheme provides isLight
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = RikoMaterialTypography,
            content = content
        )
    }
}