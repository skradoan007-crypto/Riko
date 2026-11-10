package com.rikodev.music.android.core.designsystem.riko.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.theme.RikoCornerRadius
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlass
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlassBorder
import com.rikodev.music.android.core.designsystem.riko.theme.RikoIconSize
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextSecondary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — SORT DROPDOWN CHIP (SM-UI-SPEC-001 v2.0, Section 2.14)
//
// Small glass pill: label (Chip-Label, text-secondary) + 4dp gap + chevron-down
// (14dp, text-secondary). Height 28-32dp.
//
// ⚠ RADIUS EXCEPTION: radius-xs (8dp) — the ONE chip in the kit that is NOT
// radius-full. Every other pill/chip/card uses radius-sm or radius-full.
// Do NOT generalize this value (spec 1.5, deck slide 6).
// ─────────────────────────────────────────────────────────────────────────────

/** Chip height 32dp (spec 2.14: 28-32dp), chevron 14dp, label-icon gap 4dp. */
private val SortChipHeight = 32.dp
private val SortChevronSize = 14.dp
private val SortChipLabelGap = 4.dp
private val SortChipHorizontalPadding = 10.dp

@Composable
fun RikoSortDropdownChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(SortChipHeight)
            .clip(RoundedCornerShape(RikoCornerRadius.XS))
            .background(RikoGlass)
            .border(1.dp, RikoGlassBorder, RoundedCornerShape(RikoCornerRadius.XS))
            .clickable(onClick = onClick)
            .padding(horizontal = SortChipHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SortChipLabelGap)
    ) {
        Text(
            text = label,
            style = RikoTypography.Label,
            color = RikoTextSecondary
        )
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowDown,
            contentDescription = null,
            tint = RikoTextSecondary,
            modifier = Modifier.size(SortChevronSize)
        )
    }
}
