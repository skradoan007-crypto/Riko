/*
 * RIKO EXPERIENCE OS — PRODUCTION-GRADE ROADMAP v2.0
 * Milestone 5 — Lyrics Experience
 *
 * File 4 / 12 — LyricsRendering.kt
 * --------------------------------------------------------------------
 * Compose rendering for lyrics. One line focus, emotion-forward.
 * © RIKO Experience OS — Lyrics Rendering Tier
 */

package com.rikodev.music.android.presentation.lyrics.rendering

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.music.android.core.designsystem.glass.GlassSurface
import com.rikodev.music.android.core.designsystem.theme.GlassTokens
import com.rikodev.music.android.core.designsystem.theme.RikoDimens
import com.rikodev.music.android.presentation.lyrics.contract.LineHighlightState
import com.rikodev.music.android.presentation.lyrics.runtime.LyricsLineUi
import com.rikodev.music.android.presentation.lyrics.runtime.LyricsPhaseUi
import com.rikodev.music.android.presentation.lyrics.runtime.LyricsUiState
import androidx.compose.animation.core.Animatable

@Composable
fun LyricsScreen(
    state: LyricsUiState,
    onLineTap: (Int) -> Unit,
    onRefresh: () -> Unit,
    onShare: () -> Unit,
    onAttributionTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.phase == LyricsPhaseUi.HIDDEN) return

    GlassSurface(
        style = GlassTokens.FloatingCard,
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = state.accessibilityLabel },
    ) {
        Column(modifier = Modifier.padding(RikoDimens.md)) {
            LyricsHeader(
                state = state,
                onRefresh = onRefresh,
                onShare = onShare,
            )
            Spacer(modifier = Modifier.height(RikoDimens.md))
            LyricsContent(
                state = state,
                onLineTap = onLineTap,
                modifier = Modifier.weight(1f),
            )
            if (state.attributionLabel != null) {
                Spacer(modifier = Modifier.height(RikoDimens.sm))
                AttributionFooter(
                    label = state.attributionLabel,
                    onTap = onAttributionTap,
                )
            }
        }
    }
}

@Composable
private fun LyricsHeader(
    state: LyricsUiState,
    onRefresh: () -> Unit,
    onShare: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Lyrics",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (state.isSynced) {
                Text(
                    text = "Synced • ${state.readingModeLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = state.readingModeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onRefresh) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh lyrics",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onShare) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share lyrics",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LyricsContent(
    state: LyricsUiState,
    onLineTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state.phase) {
        LyricsPhaseUi.LOADING -> LoadingContent(modifier)
        LyricsPhaseUi.READY -> ReadyContent(state, onLineTap, modifier)
        LyricsPhaseUi.NO_LYRICS, LyricsPhaseUi.ERROR, LyricsPhaseUi.OFFLINE -> EmptyContent(state, modifier)
        LyricsPhaseUi.HIDDEN -> {}
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        com.rikodev.music.android.core.designsystem.glass.GlassLoadingIndicator()
        Spacer(modifier = Modifier.height(RikoDimens.md))
        Text(
            text = "Loading lyrics...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReadyContent(
    state: LyricsUiState,
    onLineTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val currentIdx = state.currentLineIndex

    // Auto-scroll to current line if following playback
    LaunchedEffect(currentIdx, state.lines.size) {
        if (currentIdx != null && currentIdx >= 0 && currentIdx < state.lines.size) {
            listState.animateScrollToItem(currentIdx)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = RikoDimens.xl),
        verticalArrangement = Arrangement.spacedBy(RikoDimens.sm),
    ) {
        // PERF FORENSIC P1 (RC-09): stable keys + indexed — was O(N²) indexOf
        // per composition with no keys (full rebind on every highlight tick).
        itemsIndexed(
            items = state.lines,
            key = { _, line -> line.lineId },
            contentType = { _, _ -> "lyric_line" }
        ) { index, line ->
            LyricsLineView(
                line = line,
                fontSizeScale = state.fontSizeScale,
                onTap = { onLineTap(index) },
            )
        }
    }
}

@Composable
private fun LyricsLineView(
    line: LyricsLineUi,
    fontSizeScale: Float,
    onTap: () -> Unit,
) {
    val baseSize = 18.sp
    val scaledSize = baseSize * fontSizeScale
    val (textColor, fontWeight) = when (line.highlightState) {
        LineHighlightState.HIGHLIGHTED -> MaterialTheme.colorScheme.onSurface to FontWeight.SemiBold
        LineHighlightState.PRE_HIGHLIGHT -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) to FontWeight.Normal
        LineHighlightState.POST_HIGHLIGHT -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) to FontWeight.Normal
        LineHighlightState.NOT_HIGHLIGHTED -> MaterialTheme.colorScheme.onSurfaceVariant to FontWeight.Normal
    }
    val scaledHighlightSize = (baseSize.value * 1.4 * fontSizeScale).sp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = line.accessibilityLabel }
            .padding(horizontal = RikoDimens.md, vertical = RikoDimens.xs),
        contentAlignment = Alignment.Center,
    ) {
        if (line.isInstrumental) {
            Text(
                text = "♪ ♪ ♪",
                style = MaterialTheme.typography.bodyLarge,
                color = textColor.copy(alpha = 0.5f),
                fontSize = scaledSize,
            )
        } else {
            Text(
                text = line.text,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                fontWeight = fontWeight,
                fontSize = if (line.highlightState == LineHighlightState.HIGHLIGHTED) scaledHighlightSize else scaledSize,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onTap),
            )
        }
    }
}

@Composable
private fun EmptyContent(state: LyricsUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = state.emptyStateTitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(RikoDimens.sm))
        Text(
            text = state.emptyStateHint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AttributionFooter(label: String, onTap: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clickable(onClick = onTap)
            .semantics { contentDescription = "Lyrics source: $label" },
    )
}

