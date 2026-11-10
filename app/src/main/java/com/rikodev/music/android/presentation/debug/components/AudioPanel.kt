package com.rikodev.music.android.presentation.debug.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.music.android.presentation.debug.TestGenre
import androidx.compose.foundation.layout.height
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — AUDIO PANEL
//
// Three sections combined:
//   1. Experience Score — developer-rated feel metrics
//   2. Calibration Session Notes — per-genre text notes
//   3. Genre Quick Switch — predefined test songs (no music library needed)
//
// EXPERIENCE SCORE:
//   These are NOT AI-calculated.
//   The DEVELOPER rates them during the tuning session.
//   Smoothness, Fluidity, Glass Feel, Performance, Overall.
//
// SESSION NOTES:
//   Simple text. Future reference.
//   "EDM ★★★★★ — feels like liquid mercury"
//   "Phonk ★★★☆☆ — needs more bounce"
//
// GENRE QUICK SWITCH:
//   Temporary. No music library needed.
//   Just predefined test songs that generate synthetic energy patterns.
//
// PRODUCER: Phase 3.4J.2
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AudioPanel(
    scoreSmoothness: Float,
    scoreFluidity: Float,
    scoreGlassFeel: Float,
    scorePerformance: Float,
    scoreOverall: Float,
    onSmoothnessChange: (Float) -> Unit,
    onFluidityChange: (Float) -> Unit,
    onGlassFeelChange: (Float) -> Unit,
    onPerformanceChange: (Float) -> Unit,
    selectedGenre: TestGenre,
    onGenreSelected: (TestGenre) -> Unit,
    sessionNotes: Map<String, String>,
    onNotesChanged: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0A18), RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Section 1: Experience Score ──────────────────────────────
        Text(
            text = "━━ EXPERIENCE SCORE ━━",
            color = Color(0xFFFFD700),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ScoreSlider("Smoothness", scoreSmoothness, onSmoothnessChange)
            ScoreSlider("Fluidity", scoreFluidity, onFluidityChange)
            ScoreSlider("Glass Feel", scoreGlassFeel, onGlassFeelChange)
            ScoreSlider("Performance", scorePerformance, onPerformanceChange)

            // Overall (computed)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Overall",
                    color = Color(0xFFAAAAAA),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (scoreOverall > 0) "%.0f".format(scoreOverall) else "—",
                    color = if (scoreOverall >= 80) Color(0xFF00FF88)
                    else if (scoreOverall > 0) Color(0xFFFFAA00)
                    else Color(0xFF444444),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ── Section 2: Calibration Session Notes ──────────────────
        Text(
            text = "━━ SESSION NOTES ━━",
            color = Color(0xFF88AAFF),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )

        val currentGenreName = selectedGenre.displayName
        val currentNotes = sessionNotes[currentGenreName] ?: ""

        OutlinedTextField(
            value = currentNotes,
            onValueChange = { onNotesChanged(currentGenreName, it) },
            placeholder = {
                Text(
                    text = "Notes for ${selectedGenre.displayName}...",
                    color = Color(0xFF444444),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF19B7FF),
                    unfocusedBorderColor = Color(0xFF1A1A2E),
                    cursorColor = Color(0xFF19B7FF),
                    ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        )

        // Genre notes summary
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            sessionNotes.entries.forEach { (genre, notes) ->
                if (notes.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1A1A2E))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$genre: ${notes.take(20)}${if (notes.length > 20) "..." else ""}",
                            color = Color(0xFF666666),
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // ── Section 3: Genre Quick Switch ────────────────────────────
        Text(
            text = "━━ GENRE QUICK SWITCH ━━",
            color = Color(0xFFFF6FD8),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Synthetic energy — no music library needed",
            color = Color(0xFF444444),
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TestGenre.entries.forEach { genre ->
                GenreChip(
                    name = genre.displayName,
                    isActive = genre == selectedGenre,
                    onClick = { onGenreSelected(genre) }
                )
            }
        }
    }
}

@Composable
private fun ScoreSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFF888888),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(0.4f)
        )

        androidx.compose.material3.Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..100f,
            modifier = Modifier.weight(0.5f),
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = Color(0xFFFFD700),
                activeTrackColor = Color(0xFFFFD700),
                inactiveTrackColor = Color(0xFF1A1A2E)
            )
        )

        Text(
            text = if (value > 0) "%.0f".format(value) else "—",
            color = if (value >= 80) Color(0xFF00FF88)
            else if (value > 0) Color(0xFFFFAA00)
            else Color(0xFF444444),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.1f)
        )
    }
}

@Composable
private fun GenreChip(
    name: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isActive) Color(0xFFFF6FD8) else Color(0xFF1A1A2E)
    val textColor = if (isActive) Color.White else Color(0xFF666666)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            color = textColor,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
