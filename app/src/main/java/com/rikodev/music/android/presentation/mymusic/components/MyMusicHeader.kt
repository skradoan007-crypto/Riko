package com.rikodev.music.android.presentation.mymusic.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rikodev.music.android.core.designsystem.riko.components.RikoIconButton
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextTertiary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — MY MUSIC HEADER (PLAN 4, spec §3)
//
// "My Music" + max 2 actions (Search). No giant hero — the library's job is
// content access, not decoration (spec §3, §48). The global shell owns the
// top region; this is a compact content header like Plan 3's HomeHeader.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MyMusicHeader(
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RikoSpacing.LG),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "My Music",
                style = RikoTypography.LargeTitle,
                color = RikoTextPrimary,
                maxLines = 1
            )
            Text(
                text = "Your library, your way",
                style = RikoTypography.Secondary,
                color = RikoTextTertiary
            )
        }
        RikoIconButton(
            icon = Icons.Rounded.Search,
            contentDescription = "Search",
            onClick = onSearchClick,
            container = false
        )
    }
}