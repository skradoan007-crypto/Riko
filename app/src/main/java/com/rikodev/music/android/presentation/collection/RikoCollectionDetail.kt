package com.rikodev.music.android.presentation.collection
import androidx.compose.ui.platform.LocalDensity

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.components.RikoIconButton
import com.rikodev.music.android.core.designsystem.riko.shell.LocalRikoShellMetrics
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography
import com.rikodev.music.android.presentation.collection.components.RikoCollectionActions
import com.rikodev.music.android.presentation.collection.components.RikoCollectionHero

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — COLLECTION DETAIL CONTAINER (PLAN 5 §3,§16 · PLAN 11 reference)
//
// Shared parent for ALL collection detail screens. Plan 11 additions:
//   • headerActions — top-right icon slot (Search / Heart, per reference)
//   • showHero      — tab-embedded screens (Favourites) skip the big hero and
//                     use the compact large-title header from the reference.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RikoCollectionDetail(
    identity: CollectionIdentity?,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    /** null → no back button (tab-embedded collections like Favorites). */
    onBack: (() -> Unit)? = null,
    /** null → Play-only action bar (spec §13). */
    onShuffle: (() -> Unit)? = null,
    /** false → skip the large hero block (compact reference header instead). */
    showHero: Boolean = true,
    /** false → hide the Play/Shuffle action bar (e.g. empty collections). */
    showActions: Boolean = true,
    /** Top-right header icon slot (Plan 11 reference: Search + Heart). */
    headerActions: (@Composable RowScope.() -> Unit)? = null,
    /** Extra sections + track lists appended after hero/actions (spec §16). */
    content: LazyListScope.() -> Unit
) {
    val shellMetrics = LocalRikoShellMetrics.current
    val navigationBarsBottom = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }
    val listState = rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Reference header: large bold title + optional back + trailing actions.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RikoSpacing.LG, vertical = RikoSpacing.SM),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                RikoIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack,
                    container = false
                )
            }
            Text(
                text = identity?.title ?: "",
                style = if (onBack == null) RikoTypography.LargeTitle else RikoTypography.Section,
                color = RikoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (onBack == null) 0.dp else RikoSpacing.SM)
            )
            headerActions?.invoke(this)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                bottom = shellMetrics.contentBottomPadding + navigationBarsBottom + RikoSpacing.XL
            )
        ) {
            if (identity != null) {
                if (showHero) {
                    item { RikoCollectionHero(identity = identity) }
                }
                if (showActions) {
                    item {
                        RikoCollectionActions(
                            onPlay = onPlay,
                            onShuffle = onShuffle
                        )
                    }
                }
            }
            content()
        }
    }
}
