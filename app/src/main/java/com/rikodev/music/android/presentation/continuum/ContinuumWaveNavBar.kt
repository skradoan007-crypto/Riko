package com.rikodev.music.android.presentation.continuum

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.theme.AccentMagenta
import com.rikodev.music.android.core.designsystem.riko.theme.AccentViolet

// ─────────────────────────────────────────────────────────────────────────────
// Thin adapter wiring Radoan's ContinuumBottomNavBar into RikoNavGraph
// without breaking any existing shell metrics or LiquidNexusOverlay clearance.
// ─────────────────────────────────────────────────────────────────────────────

val ContinuumWaveBarHeight = 104.dp

@Composable
fun ContinuumWaveNavBar(
    activeSection: ContinuumSection,
    onSectionSelected: (ContinuumSection) -> Unit,
    renderProfile: EffectiveRenderProfile,
    primaryAccent: Color = AccentViolet,
    secondaryAccent: Color = AccentMagenta,
    audioEnergy: Float = 0f,
    modifier: Modifier = Modifier
) {
    val selectedTab = when (activeSection) {
        ContinuumSection.EXPLORE -> ContinuumTab.EXPLORE
        ContinuumSection.LISTEN -> ContinuumTab.LISTEN
        ContinuumSection.LIBRARY -> ContinuumTab.LIBRARY
    }

    val renderTier = when (renderProfile.selectedTier) {
        VisualQualityTier.ULTRA -> ContinuumRenderTier.ULTRA
        VisualQualityTier.BALANCED -> ContinuumRenderTier.BALANCED
        VisualQualityTier.LITE -> ContinuumRenderTier.LITE
    }

    ContinuumBottomNavBar(
        selectedTab = selectedTab,
        onTabSelected = { tab ->
            val targetSection = when (tab) {
                ContinuumTab.EXPLORE -> ContinuumSection.EXPLORE
                ContinuumTab.LISTEN -> ContinuumSection.LISTEN
                ContinuumTab.LIBRARY -> ContinuumSection.LIBRARY
            }
            onSectionSelected(targetSection)
        },
        modifier = modifier,
        barHeight = 94.dp,
        renderTier = renderTier,
        showBottomPill = true
    )
}
