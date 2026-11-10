package com.rikodev.music.android.core.designsystem.riko.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object RikoThemeV2 {
    object Colors {
        val background = Color(0xFF0D0716)
        val surface = Color(0xFF1B1230)
        val surfaceElevated = Color(0xFF241640)
        val onBackground = Color(0xFFFFFFFF)
        val onBackgroundSecondary = Color(0xFFA79BC7)
        val accent = Color(0xFFFF3D8A)
        val ctaGradient = listOf(Color(0xFFFF2E88), Color(0xFF7B2FF7), Color(0xFF2E7CF6))
        val ctaBrush = Brush.horizontalGradient(ctaGradient)
        val progressTrack = Color(0xFF3A2B5C)
        val divider = Color(0x26A79BC7)
    }

    // Plan 7.2 typography policy — deliberate split, NOT mass-Baloo:
    //   * RIKO identity / expressive display headings -> Baloo 2 (RikoFontFamily)
    //   * dense UI body / metadata / captions -> readable sans (platform default)
    // Personality stays in the display layer; body copy stays professional.
    object Type {
        val sectionTitle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Colors.onBackground)
        val sectionAction = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Colors.accent)
        val cardTitle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Colors.onBackground)
        val cardSubtitle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, color = Colors.onBackgroundSecondary)
        val trackTitle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Colors.onBackground)
        val trackSubtitle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, color = Colors.onBackgroundSecondary)
        // Identity display heading (Now Playing title, onboarding hero, playlist
        // hero) — the ONLY V2 style that wears the brand face.
        val npTitle = TextStyle(
            fontFamily = com.rikodev.music.android.core.designsystem.riko.theme.RikoFontFamily,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Colors.onBackground
        )
        // Dense metadata (artist/album lines) — readable sans, deliberately.
        val npArtist = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, color = Colors.onBackgroundSecondary)
    }

    object Shapes {
        val card = RoundedCornerShape(20.dp)
        val row = RoundedCornerShape(16.dp)
        val pill = RoundedCornerShape(999.dp)
    }

    object Spacing {
        val xs = 4.dp; val sm = 8.dp; val md = 12.dp; val lg = 16.dp; val xl = 24.dp
    }

    val scheme = darkColorScheme(
        background = Colors.background,
        surface = Colors.surface,
        primary = Colors.accent,
        onBackground = Colors.onBackground,
        onSurface = Colors.onBackground,
        onSurfaceVariant = Colors.onBackgroundSecondary
    )

    val type = Typography(
        titleLarge = Type.sectionTitle,
        titleMedium = Type.cardTitle,
        bodyMedium = Type.trackTitle,
        bodySmall = Type.trackSubtitle,
        labelSmall = Type.sectionAction
    )
}

@Composable
fun V2Theme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = RikoThemeV2.scheme, typography = RikoThemeV2.type, content = content)
}
