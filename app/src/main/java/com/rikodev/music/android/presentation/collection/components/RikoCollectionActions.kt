package com.rikodev.music.android.presentation.collection.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rikodev.music.android.core.designsystem.riko.components.RikoSegmentedDualActionPill
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — COLLECTION ACTIONS (SM-UI-SPEC-001 v2.0, Section 2.9 + D3)
//
// The spec "Shuffle | Play" SegmentedDualActionPill — ONE outer glass pill:
// left ~52% aurora-gradient Shuffle segment (icon always ON per D3),
// right "Play" + play-in-circle on the transparent remainder.
// Used on Favourites, My Favourite Songs / playlist detail, My Music.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RikoCollectionActions(
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    onShuffle: (() -> Unit)? = null
) {
    if (onShuffle != null) {
        RikoSegmentedDualActionPill(
            primaryLabel = "Shuffle",
            secondaryLabel = "Play",
            onPrimaryClick = onShuffle,
            onSecondaryClick = onPlay,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = RikoSpacing.LG, vertical = RikoSpacing.LG)
        )
    } else {
        // No shuffle available (e.g. single-item collection) — Play fills the pill.
        RikoSegmentedDualActionPill(
            primaryLabel = "Play",
            secondaryLabel = "",
            onPrimaryClick = onPlay,
            onSecondaryClick = onPlay,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = RikoSpacing.LG, vertical = RikoSpacing.LG)
        )
    }
}
