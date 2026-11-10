package com.rikodev.music.android.core.designsystem.riko.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─────────────────────────────────────────────────────────────────────────────
// RIKO PREMIUM DESIGN SYSTEM — THEME
//
// ⚠️ IMPORTANT (Plan 1 guardrail):
//   RikoTheme is NOT an app-root replacement. The production app keeps
//   RikoMusicTheme (legacy). RikoTheme wraps ONLY the new design-system
//   surfaces — i.e. RikoDesignSystemPreviewScreen — until Plan 2 migration:
//
//       RikoTheme {
//           RikoDesignSystemPreviewScreen()
//       }
// ─────────────────────────────────────────────────────────────────────────────

/** Riko dark color scheme — always dark, RIKO is a dark-first product. */
private val RikoDarkColorScheme = darkColorScheme(
    // Primary — purple accent
    primary = RikoPurple,
    onPrimary = Color.White,
    primaryContainer = RikoPurpleDeep,
    onPrimaryContainer = RikoTextPrimary,

    // Secondary — blue is ambient/tertiary only, never a button color
    secondary = RikoBlue,
    onSecondary = Color.White,
    secondaryContainer = RikoBlue.copy(alpha = 0.16f),
    onSecondaryContainer = RikoTextPrimary,

    // Tertiary — bright purple for subtle accent variants
    tertiary = RikoPurpleBright,
    onTertiary = Color.White,
    tertiaryContainer = RikoPurple.copy(alpha = 0.16f),
    onTertiaryContainer = RikoTextPrimary,

    // Backgrounds
    background = RikoBackground,
    onBackground = RikoTextPrimary,

    // Surfaces
    surface = RikoSurface,
    onSurface = RikoTextPrimary,
    surfaceVariant = RikoSurfaceElevated,
    onSurfaceVariant = RikoTextSecondary,
    surfaceContainerLowest = RikoBackground,
    surfaceContainerLow = RikoBackgroundElevated,
    surfaceContainer = RikoSurface,
    surfaceContainerHigh = RikoSurfaceElevated,
    surfaceContainerHighest = RikoSurfaceStrong,
    surfaceTint = Color.Transparent, // no elevation tint — surfaces stay true to token

    // Text hierarchy
    inverseSurface = RikoTextPrimary,
    inverseOnSurface = RikoBackground,

    // Outlines — glass-consistent
    outline = RikoGlassBorder,
    outlineVariant = RikoGlass,

    // Status
    error = RikoError,
    onError = Color.White,
    errorContainer = RikoError.copy(alpha = 0.16f),
    onErrorContainer = RikoTextPrimary,

    // Scrim — deep overlay for sheets/dialogs
    scrim = Color(0xCC05050A)
)

/**
 * Riko design system theme — dark-only, purple/blue ambient identity.
 * Wraps ONLY the new design system surfaces (preview lab) in Plan 1.
 */
@Composable
fun RikoTheme(
    forceDark: Boolean = true, // RIKO is dark-first; light system theme never flips it
    content: @Composable () -> Unit
) {
    // forceDark reserved for future dynamic ambient variants — always dark today.
    @Suppress("UNUSED_EXPRESSION")
    isSystemInDarkTheme()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window
            if (window != null) {
                // Dark product → light status bar icons.
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                window.statusBarColor = android.graphics.Color.TRANSPARENT
            }
        }
    }

    MaterialTheme(
        colorScheme = RikoDarkColorScheme,
        typography = RikoMaterialTypography,
        shapes = RikoMaterialShapes,
        content = content
    )
}