package com.rikodev.music.android.presentation.debug.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — INSPECTOR TOGGLE
//
// A simple toggle button for the Inspector overlay.
// On/Off. Nothing more.
//
// DESIGN:
//   - Small circle button with "INSPECT" text
//   - Active = green, Inactive = gray
//   - Positioned in the corner of the Ribbon Viewport
//
// PRODUCER: Phase 3.4J.2
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun InspectorToggle(
    isInspectorVisible: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isInspectorVisible) Color(0xFF19B7FF) else Color(0xFF1A1A2E))
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isInspectorVisible) "INSPECT ON" else "INSPECT OFF",
            color = if (isInspectorVisible) Color.White else Color(0xFF666666),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
