package com.rikodev.music.android.presentation.nowplaying

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rikodev.music.android.core.designsystem.theme.InfoTileBottom
import com.rikodev.music.android.core.designsystem.theme.InfoTileTop
import com.rikodev.music.android.core.designsystem.theme.SongInfoCardBottom
import com.rikodev.music.android.core.designsystem.theme.SongInfoCardTop
import com.rikodev.music.android.core.designsystem.theme.TextPrimary
import com.rikodev.music.android.core.designsystem.theme.TextSecondary

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — SONG INFO SHEET (UI-target v2 rebuild)
//
// Reference: images/UI target/Tab Target (Song Info mockup).
//   • Header bar — glass back squircle + centered "Song info" title
//   • ONE gradient utility card (indigo-violet → deep teal) instead of seven
//     separate cards: header row (gradient art tile + title + share), then
//     hairline-divided primary rows (Title / Album / Duration), then a 2×2
//     technical grid (Size / Bitrate / Year / Format)
//   • Hi-Res badge beside Bitrate at ≥ 320 kbps
//   • Share wires the system chooser (title — artist); fully offline
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SongInfoSheet(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel()
) {
    // GRANULAR (PERF PASS Priority 1): the info sheet renders the current
    // song only — subscribing to the monolithic 4Hz PlayerState made the
    // whole sheet recompose four times per second while open.
    // Plain local (not `by`) so null checks smart-cast through the branches.
    val song = viewModel.currentSong.collectAsStateWithLifecycle().value
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 6.dp, bottom = 18.dp)
    ) {
        // ── Header bar — back squircle + centered title ──────────────────
        Box(modifier = Modifier.fillMaxWidth().height(44.dp)) {
            SheetSquircleButton(
                modifier = Modifier.align(Alignment.CenterStart),
                icon = Icons.Filled.ChevronLeft,
                contentDescription = "Back",
                onClick = onDismiss
            )
            Text(
                text = "Song info",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(Modifier.height(16.dp))

        if (song == null) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Nothing playing", color = TextSecondary)
            }
        } else {
            // ── The single gradient utility card ─────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(SongInfoCardTop, SongInfoCardBottom),
                            start = Offset_Zero,
                            end = Offset_Infinite
                        )
                    )
                    .border(
                        width = 0.8.dp,
                        color = Color.White.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(26.dp)
                    )
            ) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                    // Card header — art tile + title + share
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    brush = Brush.verticalGradient(
                                        listOf(InfoTileTop, InfoTileBottom)
                                    )
                                )
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = "Song info",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        SheetSquircleButton(
                            icon = Icons.Filled.Share,
                            contentDescription = "Share song info",
                            onClick = {
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "${song.title} — ${song.artist}"
                                    )
                                }
                                context.startActivity(
                                    Intent.createChooser(send, "Share song info")
                                )
                            }
                        )
                    }

                    InfoDivider()

                    // Primary metadata rows
                    PrimaryInfoRow(Icons.Filled.MusicNote, "TITLE", song.title)
                    InfoDivider()
                    PrimaryInfoRow(Icons.Filled.Album, "ALBUM", song.album)
                    InfoDivider()
                    PrimaryInfoRow(Icons.Filled.Schedule, "DURATION", song.durationFormatted)

                    InfoDivider()

                    // Technical grid — quiet, 2 columns, no icons
                    Row(modifier = Modifier.fillMaxWidth()) {
                        TechInfoCell("SIZE", song.sizeFormatted, Modifier.weight(1f))
                        TechInfoCell(
                            label = "BITRATE",
                            value = song.bitrateFormatted,
                            modifier = Modifier.weight(1f),
                            badge = if (song.bitrate >= 320) "Hi-Res" else null
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        TechInfoCell(
                            "YEAR",
                            if (song.year > 0) song.year.toString() else "Unknown",
                            Modifier.weight(1f)
                        )
                        TechInfoCell(
                            "FORMAT",
                            song.mimeType.removePrefix("audio/").uppercase(),
                            Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// Offset shorthand for the card gradient (top-left → bottom-right)
private val Offset_Zero = androidx.compose.ui.geometry.Offset(0f, 0f)
private val Offset_Infinite = androidx.compose.ui.geometry.Offset(
    Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY
)

// ── Pieces ───────────────────────────────────────────────────────────────────

/** Small glass squircle icon button used in the header bar + share slot. */
@Composable
private fun SheetSquircleButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(13.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = TextPrimary, modifier = Modifier.size(20.dp))
    }
}

/** Hairline between card sections. */
@Composable
private fun InfoDivider() {
    Spacer(Modifier.height(16.dp))
    androidx.compose.foundation.layout.Box(
        Modifier
            .fillMaxWidth()
            .height(0.7.dp)
            .background(Color.White.copy(alpha = 0.10f))
    )
    Spacer(Modifier.height(16.dp))
}

/** Icon + uppercase field label over a large white value (Title/Album/Duration). */
@Composable
private fun PrimaryInfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = label,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    letterSpacing = 1.2.sp
                ),
                color = TextSecondary
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Compact technical cell (Size/Bitrate/Year/Format) with optional badge. */
@Composable
private fun TechInfoCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    badge: String? = null
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    letterSpacing = 1.2.sp
                ),
                color = TextSecondary
            )
            if (badge != null) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF4FA8FF))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF06121F)
                    )
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
