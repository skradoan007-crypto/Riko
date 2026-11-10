package com.rikodev.music.android.core.designsystem.riko.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlass
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlassBorder
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMotion
import com.rikodev.music.android.core.designsystem.riko.theme.RikoPrimaryGradient
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextOnAccent
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextSecondary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — FILTER TAB PILL (SM-UI-SPEC-001 v2.0, Section 2.13)
//
// Horizontally scrollable pill row, 8dp gaps, height 36-40dp, radius-full.
// ACTIVE: Aurora gradient fill, text-on-accent, Chip-Label style.
// INACTIVE: glass fill (~6-8% white) + 1dp glass border, text-secondary.
// Used on Home + My Music filter rows.
// ─────────────────────────────────────────────────────────────────────────────

/** Pill height 40dp (spec 2.13: 36-40dp), horizontal padding 16dp, gap 8dp. */
private val FilterPillMinHeight = 40.dp
private val FilterPillHorizontalPadding = 16.dp
private val FilterPillGap = 8.dp

/** Single filter pill — the atom behind the row. */
@Composable
fun RikoFilterTabPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor by animateColorAsState(
        targetValue = if (selected) RikoTextOnAccent else RikoTextSecondary,
        animationSpec = RikoMotion.normal(),
        label = "rikoFilterPillText"
    )

    Box(
        modifier = modifier
            .heightIn(min = FilterPillMinHeight)
            .clip(CircleShape)
            .background(
                if (selected) Brush.horizontalGradient(RikoPrimaryGradient)
                else Brush.horizontalGradient(listOf(RikoGlass, RikoGlass))
            )
            .then(
                if (selected) Modifier
                else Modifier.border(1.dp, RikoGlassBorder, CircleShape)
            )
            .clickable(enabled = !selected, onClick = onClick)
            .padding(horizontal = FilterPillHorizontalPadding, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = RikoTypography.Label,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * Scrollable filter row — spec 2.13. [contentPadding] lets the caller align
 * the first pill to the screen margin (pass RikoSpacing.XL for 20dp).
 */
@Composable
fun RikoFilterTabRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = RikoSpacing.XL)
) {
    LazyRow(
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(FilterPillGap)
    ) {
        itemsIndexed(labels, key = { index, label -> "$index-$label" }, contentType = { _, _ -> "filter_pill" }) { index, label ->
            RikoFilterTabPill(
                label = label,
                selected = index == selectedIndex,
                onClick = { onSelected(index) }
            )
        }
    }
}
