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
        val background = Color(0xFF061414)
        val surface = Color(0xFF0B2220)
        val surfaceElevated = Color(0xFF0F2B28)
        val onBackground = Color(0xFFECFDF5)
        val onBackgroundSecondary = Color(0xFF99D6C4)
        val accent = Color(0xFF10B981)
        val ctaGradient = listOf(Color(0xFF059669), Color(0xFF10B981), Color(0xFF34D399))
        val ctaBrush = Brush.horizontalGradient(ctaGradient)
        val progressTrack = Color(0xFF133A36)
        val divider = Color(0x2E6EE7B7)
    }

    object Type {
        val sectionTitle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Colors.onBackground)
        val sectionAction = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Colors.accent)
        val cardTitle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Colors.onBackground)
        val cardSubtitle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, color = Colors.onBackgroundSecondary)
        val trackTitle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Colors.onBackground)
        val trackSubtitle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, color = Colors.onBackgroundSecondary)
        val npTitle = TextStyle(
            fontFamily = com.rikodev.music.android.core.designsystem.riko.theme.RikoFontFamily,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Colors.onBackground
        )
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
