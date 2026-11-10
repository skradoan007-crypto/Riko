package com.rikodev.music.android.core.designsystem.riko.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.components.RikoAmbientBackground
import com.rikodev.music.android.core.designsystem.riko.theme.RikoBlue
import com.rikodev.music.android.core.designsystem.riko.theme.RikoComponentSize
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMotion
import com.rikodev.music.android.core.designsystem.riko.theme.RikoPurple
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — GLOBAL APP SHELL (PLAN 2)
//
// The new premium environment every production screen lives inside:
//
//   RikoAppShell
//   ├── Ambient Background (dark cinematic, purple/blue)
//   ├── Screen Content (full-size — scrolls UNDER the floating layers)
//   ├── Bottom Navigation (floating, bottom)
//   └── Snackbar overlay (topmost — transient undo/feedback, e.g. Favorites)
//
// Mini player: the shell slot is RETIRED (M1) — the player sheet's mini bar
// replaces it. Shell METRICS still reserve the same bottom space so list
// padding and the sheet's anchors never shift.
//
// Business logic stays OUTSIDE the shell — it only hosts presentation.
// Shell state ownership (Plan 2 spec 2AK): the ROOT owns the mini player
// and bottom nav — screens never create their own.
// ─────────────────────────────────────────────────────────────────────────────

/** Ambient tint sources — future Now Playing/artwork extraction feeds this. */
data class RikoAmbientColors(
    val primary: Color = RikoPurple,
    val secondary: Color = RikoBlue
)

/**
 * Global layout metrics (Plan 2 spec 2U/2V).
 * Screens read this (via [LocalRikoShellMetrics]) instead of hardcoding
 * bottom paddings like 80.dp — the occupied bottom space is measured here.
 */
data class RikoShellMetrics(
    val miniPlayerVisible: Boolean = false,
    val miniPlayerHeight: Dp = RikoComponentSize.MiniPlayerHeight,
    val bottomNavVisible: Boolean = false,
    val bottomNavHeight: Dp = RikoComponentSize.BottomNavHeight
) {
    /** Gap between mini player and bottom navigation (8–12dp contract). */
    val interLayerGap: Dp = RikoSpacing.SM

    /** Margin below the bottom navigation (spec 2.6: 24dp above bottom inset). */
    val bottomMargin: Dp = 24.dp

    /** Total bottom space occupied by the shell (excludes system insets). */
    val contentBottomPadding: Dp
        get() {
            var pad = 0.dp
            if (miniPlayerVisible) pad += miniPlayerHeight + interLayerGap
            if (bottomNavVisible) pad += bottomNavHeight + bottomMargin
            return pad
        }
}

val LocalRikoShellMetrics = staticCompositionLocalOf { RikoShellMetrics() }

/**
 * Global app shell — theme-agnostic (riko components use explicit tokens),
 * inset-aware, and fully presentational.
 *
 * Z-order policy (Plan 2 spec 2Z — inside this shell):
 *   1. screen content → 2. bottom navigation → 3. snackbar.
 * The mini player is owned by the player sheet (M1) — the shell only
 * reserves its metrics. Fullscreen overlays (Now Playing/Visual OS host/
 * sheets) render above the shell by their own hosting — shell never
 * covers them.
 */
@Composable
fun RikoAppShell(
    modifier: Modifier = Modifier,
    ambient: RikoAmbientColors = RikoAmbientColors(),
    /** 0.10–0.16 for normal screens — the component clamps the range anyway. */
    ambientIntensity: Float = 0.14f,
    miniPlayerVisible: Boolean = false,
    bottomNavVisible: Boolean = false,
    bottomNav: @Composable () -> Unit = {},
    /** Topmost transient-feedback slot (snackbar/toast). Root owns the host. */
    snackbar: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    val metrics = RikoShellMetrics(
        miniPlayerVisible = miniPlayerVisible,
        bottomNavVisible = bottomNavVisible
    )

    CompositionLocalProvider(LocalRikoShellMetrics provides metrics) {
        RikoAmbientBackground(
            modifier = modifier,
            accentColor = ambient.primary,
            secondaryColor = ambient.secondary,
            ambientAlpha = ambientIntensity
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                content()

                // ── Global bottom stack: (sheet mini floats above) → nav ──
                // Spec 2.6: the nav capsule spans screen-width minus 80dp
                // (40dp each side); the mini player keeps the normal screen
                // gutter (16dp) — metrics only, the shell draws nothing.
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = RikoSpacing.LG)
                ) {
                    AnimatedVisibility(
                        visible = bottomNavVisible,
                        enter = fadeIn(RikoMotion.normal()) +
                            expandVertically(RikoMotion.normal()),
                        exit = fadeOut(RikoMotion.normal()) +
                            shrinkVertically(RikoMotion.normal())
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                            bottomNav()
                            Spacer(Modifier.height(metrics.bottomMargin))
                        }
                    }
                }

                // Z4: transient feedback floats above the whole bottom stack.
                snackbar()
            }
        }
    }
}