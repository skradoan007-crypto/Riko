package com.rikodev.music.android.presentation.debug.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
// RIKO MUSIC — SNAPSHOT BAR
//
// Save / Load / Reset / Export / Import controls.
//
// DESIGN:
//   - Horizontal row of action buttons
//   - Scrollable if needed
//   - Monospace, dark theme
//
// SNAPSHOT SYSTEM:
//   Save      → Serialize current WaveCalibration to JSON
//   Load      → Restore from saved snapshot
//   Reset     → WaveCalibration.reset() → back to CLASSIC defaults
//   Export    → Copy JSON to clipboard (future)
//   Import    → Paste JSON and apply (future)
//
// WHY EXPORT/IMPORT?
//   "ভাই LiquidGlass_v7.json টা অত ভালো ছিল"
//   → One click. Done.
//
// PRODUCER: Phase 3.4J.2
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SnapshotBar(
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onReset: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
    hasSnapshots: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SnapshotButton("Save", Color(0xFF19B7FF), onSave)
        SnapshotButton("Load", Color(0xFF19B7FF), onLoad, enabled = hasSnapshots)
        SnapshotButton("Reset", Color(0xFFFF6644), onReset)
        SnapshotButton("Export", Color(0xFF888888), onExport)
        SnapshotButton("Import", Color(0xFF888888), onImport)
    }
}

@Composable
private fun SnapshotButton(
    label: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val bgColor = if (enabled) Color(0xFF1A1A2E) else Color(0xFF0D0D1A)
    val textColor = if (enabled) accentColor else Color(0xFF444444)

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
