package com.rikodev.music.android.core.designsystem.riko.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rikodev.music.android.core.designsystem.riko.theme.AccentViolet
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — SECTION HEADER
//
// Section title + optional "See all" style action. Title uses Section style;
// action text is the ONE purple text accent allowed per section.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RikoSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = RikoTypography.Section,
            color = RikoTextPrimary
        )
        if (actionText != null && onAction != null) {
            Text(
                text = actionText,
                style = RikoTypography.Label,
                color = AccentViolet,  // spec 2.12: "See all" = ONE purple text accent per section
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
    }
}