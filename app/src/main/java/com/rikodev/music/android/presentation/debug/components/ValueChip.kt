package com.rikodev.music.android.presentation.debug.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — VALUE CHIP
//
// Small display chip for showing a label + value pair.
// Used in Performance panel, Experience Score card, and debug overlays.
//
// DESIGN:
//   - Dark background pill
//   - Label in gray, value in accent green
//   - Monospace font
//
// PRODUCER: Phase 3.4J.2
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ValueChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color(0xFF00FF88),
    labelColor: Color = Color(0xFF888888),
    warning: Boolean = false
) {
    val effectiveValueColor = if (warning) Color(0xFFFF6644) else valueColor

    Box(
        modifier = modifier
            .background(Color(0xFF0D0D1A), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$label $value",
            color = Color.Transparent,
            fontSize = 0.sp // invisible container for sizing
        )
        androidx.compose.foundation.layout.Row {
            Text(
                text = label,
                color = labelColor,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = " ",
                color = Color.Transparent,
                fontSize = 10.sp
            )
            Text(
                text = value,
                color = effectiveValueColor,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Value chip for numeric scores (e.g., Experience Score).
 * Shows a star rating indicator next to the value.
 */
@Composable
fun ScoreChip(
    label: String,
    score: Float,
    modifier: Modifier = Modifier
) {
    val stars = when {
        score >= 90 -> 5
        score >= 75 -> 4
        score >= 60 -> 3
        score >= 40 -> 2
        score > 0 -> 1
        else -> 0
    }
    val starText = if (stars > 0) "★".repeat(stars) + "☆".repeat(5 - stars) else "☆☆☆☆☆"

    androidx.compose.foundation.layout.Row(
        modifier = modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = Color(0xFF888888),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "  ",
            color = Color.Transparent,
            fontSize = 10.sp
        )
        Text(
            text = if (score > 0) "%.0f".format(score) else "—",
            color = if (score >= 80) Color(0xFF00FF88)
            else if (score > 0) Color(0xFFFFAA00)
            else Color(0xFF444444),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "  $starText",
            color = if (score >= 80) Color(0xFFFFD700)
            else if (score > 0) Color(0xFF666666)
            else Color(0xFF333333),
            fontSize = 9.sp
        )
    }
}
