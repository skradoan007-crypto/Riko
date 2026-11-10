package com.rikodev.music.android.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * EQ-local theming for the Now Playing Equalizer sheet.
 *
 * Flat Equalizer parity: the EQ feature supports exactly two appearances —
 * dark and light — independent of the app-wide Riko theme (which stays
 * dark-only). Every EQ composable must read colors from [LocalEqColors]
 * instead of hardcoding hex values.
 */
enum class EqUiThemeMode {
    DARK,
    LIGHT;

    companion object {
        fun fromStorage(value: String?): EqUiThemeMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DARK
    }
}

/** Color tokens consumed by every EQ card, control, and curve. */
data class EqColorTokens(
    val background: Color,
    val card: Color,
    val cardAlt: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val track: Color,
    val curveStroke: Color,
    val curveFill: Color,
    val danger: Color,
    val switchThumb: Color,
)

/** Dark appearance — matches the original Flat Equalizer-inspired look. */
val DarkEqTokens = EqColorTokens(
    background = Color(0xFF0F0B1E),
    card = Color(0xFF1B192C),
    cardAlt = Color(0xFF26243A),
    accent = Color(0xFF3B82F6),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFA0A0AB),
    track = Color(0xFF26243A),
    curveStroke = Color(0xFF3B82F6),
    curveFill = Color(0x333B82F6),
    danger = Color(0xFFF87171),
    switchThumb = Color(0xFFFFFFFF),
)

/** Light appearance — EQ sheet only; app theme stays dark. */
val LightEqTokens = EqColorTokens(
    background = Color(0xFFF4F6FB),
    card = Color(0xFFFFFFFF),
    cardAlt = Color(0xFFE8ECF4),
    accent = Color(0xFF2563EB),
    textPrimary = Color(0xFF12141A),
    textSecondary = Color(0xFF5B6170),
    track = Color(0xFFD7DCE6),
    curveStroke = Color(0xFF2563EB),
    curveFill = Color(0x292563EB),
    danger = Color(0xFFDC2626),
    switchThumb = Color(0xFFFFFFFF),
)

val LocalEqColors = staticCompositionLocalOf { DarkEqTokens }

/**
 * EQ shape tokens — lives in the design-token directory so the drift
 * ratchet counts these literals once, not per UI call site.
 */
object EqShapes {
    val Card = RoundedCornerShape(24.dp)
    val Inner = RoundedCornerShape(12.dp)
    val Pill = RoundedCornerShape(16.dp)
}

/** EQ type scale — same drift rule as [EqShapes]. */
object EqType {
    val Tiny = TextStyle(fontSize = 10.sp)
    val Micro = TextStyle(fontSize = 11.sp)
    val Caption = TextStyle(fontSize = 12.sp)
    val Small = TextStyle(fontSize = 13.sp)
    val Body = TextStyle(fontSize = 14.sp)
    val BodyLarge = TextStyle(fontSize = 15.sp)
    val Title = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    val Headline = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold)
}

object EqTheme {
    val colors: EqColorTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalEqColors.current
}

@Composable
fun EqTheme(
    mode: EqUiThemeMode,
    content: @Composable () -> Unit,
) {
    val tokens = when (mode) {
        EqUiThemeMode.DARK -> DarkEqTokens
        EqUiThemeMode.LIGHT -> LightEqTokens
    }
    CompositionLocalProvider(LocalEqColors provides tokens) {
        content()
    }
}

/** Resolves the device preference when the user has not chosen explicitly. */
@Composable
fun resolveEqUiThemeMode(stored: EqUiThemeMode?): EqUiThemeMode =
    stored ?: if (isSystemInDarkTheme()) EqUiThemeMode.DARK else EqUiThemeMode.LIGHT
