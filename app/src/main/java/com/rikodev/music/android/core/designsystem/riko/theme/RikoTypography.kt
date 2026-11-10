package com.rikodev.music.android.core.designsystem.riko.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.rikodev.music.android.R

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — SOUND MOVES TYPOGRAPHY (SM-UI-SPEC-001 v2.0, Section 1.3)
//
// Typeface: "Baloo 2" (rounded geometric sans — matches reference letterforms).
// Bundled OFL TTFs in res/font/ (offline-first: no INTERNET permission, no
// downloadable-fonts provider). Fallback chain per spec: Fredoka → Poppins.
//
// RULE: Never hardcode fontSize/sp in UI — always use RikoTypography.XYZ.
// ─────────────────────────────────────────────────────────────────────────────

/** Baloo 2 — weights 400/500/600/700 bundled as res/font. */
val RikoFontFamily: FontFamily = FontFamily(
    Font(R.font.baloo2_regular, weight = FontWeight.Normal),
    Font(R.font.baloo2_medium, weight = FontWeight.Medium),
    Font(R.font.baloo2_semibold, weight = FontWeight.SemiBold),
    Font(R.font.baloo2_bold, weight = FontWeight.Bold)
)

object RikoTypography {

    /** Screen titles: "Favourites", "Playlist", "Setting" (spec Title-XL). */
    val LargeTitle = TextStyle(
        fontFamily = RikoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    )

    /** Oversized statements (kept for legacy call-sites; = Title-XL). */
    val Display = TextStyle(
        fontFamily = RikoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    )

    /** "Now Playing" header, song title (spec Title-L). */
    val Title = TextStyle(
        fontFamily = RikoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    )

    /** Promo banner heading, section emphasis (spec Title-M). */
    val Section = TextStyle(
        fontFamily = RikoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    )

    /** List row titles — track/playlist/artist names (spec Body-Bold). */
    val Body = TextStyle(
        fontFamily = RikoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    )

    /** Artist names / subtitles (spec Caption: 12 Medium). */
    val Secondary = TextStyle(
        fontFamily = RikoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp
    )

    /** Durations, counts, follower text (spec Caption). */
    val Caption = TextStyle(
        fontFamily = RikoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp
    )

    /** "170 favourites", "15 Playlist" (spec Overline/Meta). */
    val Overline = TextStyle(
        fontFamily = RikoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    )

    /** Tab pills, sort dropdown chips (spec Chip-Label). */
    val Label = TextStyle(
        fontFamily = RikoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    )

    /** All button text (spec Button-Label). */
    val Button = TextStyle(
        fontFamily = RikoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp
    )

    /** "By continuing, you agree…" (spec Legal/Fine-print). */
    val Legal = TextStyle(
        fontFamily = RikoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp
    )

    /** Body copy / promo subtext (spec Body-Regular). */
    val BodyRegular = TextStyle(
        fontFamily = RikoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )

    /**
     * Time / progress read-outs — §11 Design System ▸ Typography:
     * "tabular numerals for time/progress".
     *
     * Metrics match [Caption] exactly so a ticking clock never changes the
     * width of a row, seek bar or queue item underneath it.
     */
    val TimeTabular = TextStyle(
        fontFamily = RikoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp,
        fontFeatureSettings = "tnum"
    )
}

/**
 * Canonical [RikoTypography] roles mapped onto Material3 typography slots.
 *
 * Single source of truth (Phase 5 Step 0, G-1/D-16): every theme entry
 * (production `RikoMusicTheme`, preview/onboarding `RikoTheme`) resolves
 * Material slots from here, so `MaterialTheme.typography.X` and
 * `RikoTypography.Y` can never diverge. Mirrors the mapping previously
 * private to `RikoTheme.kt:27-42`.
 */
val RikoMaterialTypography = Typography(
    displayLarge = RikoTypography.Display,
    displayMedium = RikoTypography.Display,
    displaySmall = RikoTypography.LargeTitle,
    headlineLarge = RikoTypography.LargeTitle,
    headlineMedium = RikoTypography.LargeTitle,
    headlineSmall = RikoTypography.Title,
    titleLarge = RikoTypography.Title,
    titleMedium = RikoTypography.Section,
    titleSmall = RikoTypography.Body,
    bodyLarge = RikoTypography.Body,
    bodyMedium = RikoTypography.Secondary,
    bodySmall = RikoTypography.Caption,
    labelLarge = RikoTypography.Label,
    labelMedium = RikoTypography.Label,
    labelSmall = RikoTypography.Caption
)