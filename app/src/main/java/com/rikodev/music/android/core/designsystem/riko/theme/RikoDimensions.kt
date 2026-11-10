package com.rikodev.music.android.core.designsystem.riko.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
// RIKO PREMIUM DESIGN SYSTEM — DIMENSION TOKENS (Plan 11: Pixel-Perfect Match)
//
// RULE: .padding(17.dp), RoundedCornerShape(19.dp) etc. are FORBIDDEN.
//       Change a token here → the whole visual language shifts together.
// ─────────────────────────────────────────────────────────────────────────────

/** Spacing scale — the only horizontal/vertical gaps allowed. */
object RikoSpacing {
    val XS = 4.dp
    val SM = 8.dp
    val MD = 12.dp
    val LG = 16.dp      // default screen padding, nav capsule bottom offset
    val XL = 20.dp
    val XXL = 24.dp     // large screen / tablet, hero section min, nav horizontal padding
    val XXXL = 32.dp    // hero section
    val Huge = 40.dp
    val Massive = 48.dp
}

/** Corner radius scale — the only radii allowed. */
object RikoCornerRadius {
    val XS = 8.dp       // small badges
    val SM = 12.dp      // small controls, artwork thumbnail
    val MD = 16.dp      // list items, playlist cards (reference: 16.dp)
    val LG = 20.dp      // large card / promo banner (spec radius-lg)
    val XL = 24.dp      // surface cards (reference: 24.dp)
    val XXL = 36.dp     // large modal / premium container
    val Pill = 999.dp   // buttons, segmented controls, nav capsule (CircleShape)
}

/**
 * Canonical shapes — §11 Design System ▸ Shape.
 *
 * Every `RoundedCornerShape(...)` in production code must come from here.
 * These add no new numbers: each is the existing [RikoCornerRadius] scale
 * named by design intent, so a call site reads as language rather than as
 * an unexplained literal.
 */
object RikoShape {
    /** Buttons, segmented controls, sort chips, nav capsule. */
    val Pill = RoundedCornerShape(RikoCornerRadius.Pill)

    /** Play controls, avatars, bottom-nav active indicator. */
    val Circle = CircleShape

    /** List items, playlist cards, shelf cards. */
    val Card = RoundedCornerShape(RikoCornerRadius.MD)

    /** Promo banners, large surface cards. */
    val CardLarge = RoundedCornerShape(RikoCornerRadius.XL)

    /** Bottom sheets and modal surfaces (rounded top corners only). */
    val Sheet = RoundedCornerShape(
        topStart = RikoCornerRadius.XXL,
        topEnd = RikoCornerRadius.XXL
    )

    /** Album artwork in rows and cards. */
    val Artwork = RoundedCornerShape(RikoCornerRadius.SM)

    /** Mini player artwork — the ≈44dp container of §77. */
    val MiniArtwork = RoundedCornerShape(RikoCornerRadius.XS)

    /** Small controls, badges, notices. */
    val Control = RoundedCornerShape(RikoCornerRadius.XS)
}

/** MaterialTheme shape mapping for the RIKO design system (used by [RikoTheme]).
 *  Moved here from the deleted duplicate riko/theme/RikoShapes.kt. */
val RikoMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(RikoCornerRadius.XS),
    small = RoundedCornerShape(RikoCornerRadius.SM),
    medium = RoundedCornerShape(RikoCornerRadius.MD),
    large = RoundedCornerShape(RikoCornerRadius.LG),
    extraLarge = RoundedCornerShape(RikoCornerRadius.XL)
)

/** Icon sizing scale — one consistent icon family & stroke weight. */
object RikoIconSize {
    val Compact = 20.dp
    val Standard = 24.dp    // bottom navigation, list icons
    val Prominent = 28.dp   // primary action
    val Hero = 32.dp        // player action
}

/** Standard component dimensions — controls, rows, bars. */
object RikoComponentSize {
    // Buttons (Plan 1 spec §13) — Pill shape (CircleShape)
    val PrimaryButtonHeight = 56.dp
    val LargeButtonHeight = 52.dp
    val CompactButtonHeight = 40.dp
    val ButtonHorizontalPadding = 20.dp

    // Touch targets (accessibility — minimum 48dp)
    val TouchTargetMin = 48.dp

    // Play controls (spec §10)
    val PlayContainerHero = 64.dp       // 56–64dp primary play (reference: large white circle)
    val PlayContainerMini = 44.dp       // 40–44dp mini player play

    // Floating Bottom Navigation Capsule (SM spec 2.6 / 1.7)
    val BottomNavHeight = 64.dp         // capsule height (spec: 64, was 72)
    val BottomNavItemSize = 56.dp       // item hit-area (spec: 56x56)
    val BottomNavActiveIndicatorSize = 52.dp // active circle w/ aurora gradient (spec: 52)
    val BottomNavCapsulePadding = 16.dp // internal padding
    val BottomNavIconSize = 24.dp

    // Bars & rows (SM spec 1.7)
    val MiniPlayerHeight = 64.dp
    val ListRowHeight = 76.dp          // TrackListRow incl. 16dp gap to next row
    val ListRowContentHeight = 56.dp  // row content min-height
    val ListRowArtwork = 56.dp         // square thumbnail r12 (spec 2.11)
    val SongRowHeight = 76.dp
    val SongArtwork = 48.dp
    val SongRowInternalGap = 12.dp

    // Artwork scale (SM spec 1.7 / 2.18)
    val ArtworkThumbnail = 56.dp        // row thumbnails (spec: 56x56)
    /** Home shelf card artwork (Plan 3 spec §15: 72–88dp). */
    val ArtworkShelf = 84.dp
    val ArtworkMedium = 96.dp
    val ArtworkLarge = 240.dp
    val ArtworkHero = 280.dp  // Now Playing circular hero artwork (spec 2.18)
    val ArtworkFavoritesHero = 200.dp // playlist-detail hero (spec 3.8)
}