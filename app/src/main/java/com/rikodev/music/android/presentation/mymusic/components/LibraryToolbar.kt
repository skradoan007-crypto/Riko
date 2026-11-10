package com.rikodev.music.android.presentation.mymusic.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlass
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlassBorder
import com.rikodev.music.android.core.designsystem.riko.theme.RikoPurple
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextSecondary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography
import com.rikodev.music.android.data.repository.SortBy
import com.rikodev.music.android.data.repository.SortOrder
import com.rikodev.music.android.presentation.components.GlassBottomSheetContainer

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — LIBRARY TOOLBAR + SORT SHEET (PLAN 4, spec §9–11, §34)
//
// Compact toolbar: "N Songs" summary + "Sort: X ▾" pill (glass, spec §43).
// Sort sheet: Riko-styled bottom sheet using the EXISTING GlassBottomSheet
// container — NOT an oversized dialog. Only REAL repository sort fields
// (SortBy: Title/Artist/Album/DateAdded/Duration/Size) — no invented sorts.
// ─────────────────────────────────────────────────────────────────────────────

internal val SortBy.label: String
    get() = when (this) {
        SortBy.TITLE -> "Name"
        SortBy.ARTIST -> "Artist"
        SortBy.ALBUM -> "Album"
        SortBy.DATE_ADDED -> "Date Added"
        SortBy.DURATION -> "Duration"
        SortBy.SIZE -> "Size"
    }

@Composable
fun LibraryToolbar(
    itemCount: Int,
    itemLabel: String,
    sortBy: SortBy,
    onSortClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RikoSpacing.LG, vertical = RikoSpacing.SM),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$itemCount $itemLabel",
            style = RikoTypography.Secondary,
            color = RikoTextSecondary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // Compact sort control — "Date Added ▾" style (spec §9).
        Box(
            modifier = Modifier
                .heightIn(min = 36.dp)
                .clip(RoundedCornerShape(50))
                .background(RikoGlass)
                .border(1.dp, RikoGlassBorder, RoundedCornerShape(50))
                .clickable(onClick = onSortClick)
                .padding(horizontal = RikoSpacing.MD, vertical = RikoSpacing.XS),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = sortBy.label,
                    style = RikoTypography.Label,
                    color = RikoTextPrimary,
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = "Sort options",
                    tint = RikoTextSecondary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySortSheet(
    currentSort: SortBy,
    currentOrder: SortOrder,
    onSortSelected: (SortBy) -> Unit,
    onOrderSelected: (SortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent,
        dragHandle = null
    ) {
        GlassBottomSheetContainer(accentColor = RikoPurple) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RikoSpacing.LG, vertical = RikoSpacing.LG)
            ) {
                Text(
                    text = "Sort by",
                    style = RikoTypography.Section,
                    color = RikoTextPrimary
                )
                Spacer(Modifier.height(RikoSpacing.XS))
                SortBy.entries.forEach { option ->
                    SortOptionRow(
                        label = option.label,
                        selected = option == currentSort,
                        onClick = { onSortSelected(option) }
                    )
                }
                Spacer(Modifier.height(RikoSpacing.MD))
                Text(
                    text = "Order",
                    style = RikoTypography.Section,
                    color = RikoTextPrimary
                )
                Spacer(Modifier.height(RikoSpacing.XS))
                SortOrder.entries.forEach { option ->
                    SortOptionRow(
                        label = if (option == SortOrder.ASCENDING) "Ascending" else "Descending",
                        selected = option == currentOrder,
                        onClick = { onOrderSelected(option) }
                    )
                }
                Spacer(Modifier.height(RikoSpacing.MD))
            }
        }
    }
}

@Composable
private fun SortOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(onClick = onClick)
            .padding(vertical = RikoSpacing.SM),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RikoSpacing.MD)
    ) {
        // Radio indicator — purple accent when selected (spec §10).
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    color = if (selected) RikoPurple else RikoGlassBorder,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(RikoPurple)
                )
            }
        }
        Text(
            text = label,
            style = if (selected) {
                RikoTypography.Body.copy(
                    color = RikoPurple,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                RikoTypography.Body.copy(color = RikoTextSecondary)
            },
            maxLines = 1
        )
    }
}