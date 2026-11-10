package com.rikodev.music.android.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — CENTRALISED SHAPE TOKENS
//
// Single source of truth for every corner radius in the app. No magic numbers
// scattered across composables — reference RikoShapes.XYZ and the design
// system stays consistent.
// ─────────────────────────────────────────────────────────────────────────────

// DEFERRED (Phase 5, §80 value changes): only [TopSheet] is still consumed
// here — GlassBottomSheetContainer (28dp top corners). Reconciling that with
// canonical [RikoShape.Sheet] is a value change, out of Phase 3 scope. Every
// other shape consumer migrated value-preserving to `RikoShape`, and the
// duplicate riko/theme/RikoShapes.kt was deleted.

object RikoShapes {

    /** Perfect circle — play button, small bubbles, badges. */
    val Circle: Shape = CircleShape

    /** Full pill — buttons, chips, tags, progress bars. */
    val Pill: Shape = RoundedCornerShape(50)

    /** Standard card — list items, sheet content, general containers.
     *  Matches the precise reference's DEFAULT radius (1rem = 16dp). */
    val Card: Shape = RoundedCornerShape(16.dp)

    /** Large card — matches the precise reference's `lg` radius (2rem = 32dp). */
    val CardLarge: Shape = RoundedCornerShape(32.dp)

    /** Extra-large, very soft card — matches the precise reference's `xl`
     *  radius (3rem = 48dp), used for square category tiles (Albums/
     *  Artists/Playlists/Folders grid). */
    val CardExtraLarge: Shape = RoundedCornerShape(48.dp)

    /** Album art thumbnail — 8dp corners for compact art. */
    val Thumbnail: Shape = RoundedCornerShape(8.dp)

    /** Hero album art — 24dp corners for the large Now Playing art. */
    val AlbumArtHero: Shape = RoundedCornerShape(24.dp)

    /** Medium album art — 16dp corners for secondary art displays. */
    val AlbumArtMedium: Shape = RoundedCornerShape(16.dp)

    /** Top-sheet — rounded top corners only, flat bottom (sits on screen edge). */
    val TopSheet: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
}
