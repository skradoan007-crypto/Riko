package com.rikodev.music.android.core.designsystem.theme

import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons

/**
 * RIKO MUSIC
 * Global Dimension System
 *
 * Never hardcode dp values inside UI.
 * Always use RikoDimens.
 */

object RikoDimens {

    //────────────────────────────────────────────
    // Spacing (direct access)
    //────────────────────────────────────────────

    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val huge = 40.dp
    val giant = 48.dp
    val massive = 64.dp

    //────────────────────────────────────────────
    // Spacing via namespace (RikoDimens.spacing.xxx)
    //────────────────────────────────────────────

    object spacing {
        val xxs = 2.dp
        val xs = 4.dp
        val xSmall = 4.dp
        val sm = 8.dp
        val small = 8.dp
        val md = 12.dp
        val medium = 12.dp
        val lg = 16.dp
        val large = 16.dp
        val xl = 20.dp
        val xLarge = 20.dp
        val xxl = 24.dp
        val xxLarge = 24.dp
        val xxxl = 32.dp
        val huge = 40.dp
    }

    //────────────────────────────────────────────
    // Corner Radius
    //────────────────────────────────────────────

    val radiusSmall = 12.dp
    val radiusMedium = 16.dp
    val radiusLarge = 20.dp
    val radiusXL = 24.dp
    val radiusXXL = 28.dp
    val radiusPill = 999.dp

    //────────────────────────────────────────────
    // Stroke
    //────────────────────────────────────────────

    val strokeThin = 1.dp
    val strokeNormal = 1.5.dp
    val strokeThick = 2.dp

    //────────────────────────────────────────────
    // Blur
    //────────────────────────────────────────────

    val blurSmall = 8.dp
    val blurMedium = 16.dp
    val blurLarge = 24.dp
    val blurHero = 40.dp

    //────────────────────────────────────────────
    // Icons
    //────────────────────────────────────────────

    val iconSmall = 18.dp
    val iconNormal = 24.dp
    val iconLarge = 32.dp
    val iconHero = 48.dp

    //────────────────────────────────────────────
    // Components
    //────────────────────────────────────────────

    val navigationHeight = 74.dp
    val miniPlayerHeight = 72.dp
    val playerButton = 58.dp
    val albumArtMini = 56.dp
    val albumArtHero = 300.dp

    val waveformHeight = 64.dp

    val seekBarHeight = 6.dp

    val dialogPadding = 24.dp

    val screenHorizontalPadding = 20.dp
    val screenVerticalPadding = 16.dp
}