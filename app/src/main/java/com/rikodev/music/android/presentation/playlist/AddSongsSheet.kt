package com.rikodev.music.android.presentation.playlist

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.gesture.RikoTouch
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMotion
import com.rikodev.music.android.core.designsystem.glass.GlassButtonPrimary
import com.rikodev.music.android.core.designsystem.glass.GlassSurface
import com.rikodev.music.android.core.designsystem.theme.GlassTokens
import com.rikodev.music.android.core.designsystem.theme.TextPrimary
import com.rikodev.music.android.core.designsystem.theme.TextSecondary
import com.rikodev.music.android.core.designsystem.theme.TextTertiary
import com.rikodev.music.android.domain.model.Song

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — ADD SONGS SHEET
//
// RUNTIME FIX 8: PlaylistDetailViewModel.addSongs() → PlaylistRepository
// .addSongsToPlaylist() was already real and fully wired — the only thing
// missing anywhere in the app was a UI that could call it. This sheet is
// that missing entry point: full library (minus songs already in this
// playlist), a local search filter, tap-to-select, confirm at the bottom.
//
// Deliberately NOT built from SongListItem/GlassCard per row — a picker
// over the whole device library needs to render potentially thousands of
// rows, and GlassSurface's blur/specular layers are real GPU cost per row
// (the exact "murder on a scrolling list" problem GlassSurface's own code
// comments already warn about). Flat rows with a simple selection tint
// instead — cheaper, and a denser checklist reads correctly as a different
// interaction mode from the main library's rich cards anyway.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AddSongsSheet(
    modifier: Modifier = Modifier,
    availableSongs: List<Song>,
    selectedIds: Set<Long>,
    accentColor: Color,
    onToggleSong: (Long) -> Unit,
    onConfirm: () -> Unit
) {
    var filterText by remember { mutableStateOf("") }
    val filteredSongs = remember(availableSongs, filterText) {
        if (filterText.isBlank()) {
            availableSongs
        } else {
            availableSongs.filter {
                it.title.contains(filterText, ignoreCase = true) ||
                    it.artist.contains(filterText, ignoreCase = true)
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Add songs",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            if (selectedIds.isNotEmpty()) {
                Text(
                    "${selectedIds.size} selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = accentColor
                )
            }
        }

        GlassSurface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(50.dp),
            style = GlassTokens.Chip,
            cornerRadius = 25.dp,
            showBorder = true,
            enableShadow = false
        ) {
            TextField(
                value = filterText,
                onValueChange = { filterText = it },
                placeholder = { Text("Filter your library...", color = TextTertiary) },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = accentColor) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(Modifier.height(8.dp))

        if (filteredSongs.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.LibraryMusic,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (availableSongs.isEmpty()) "Every song is already in this playlist"
                        else "No matches for \"$filterText\"",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(filteredSongs, key = { it.id }, contentType = { "pickable_song" }) { song ->
                    PickableSongRow(
                        song = song,
                        isSelected = song.id in selectedIds,
                        accentColor = accentColor,
                        onClick = { onToggleSong(song.id) }
                    )
                }
            }
        }

        if (selectedIds.isNotEmpty()) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                GlassButtonPrimary(
                    modifier = Modifier.fillMaxWidth(),
                    text = if (selectedIds.size == 1) "Add 1 song" else "Add ${selectedIds.size} songs",
                    icon = Icons.Filled.PlaylistAdd,
                    accentColor = accentColor,
                    onClick = onConfirm
                )
            }
        } else {
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun PickableSongRow(
    song: Song,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) RikoTouch.PressScale else 1f,
        animationSpec = RikoMotion.MediumSpring(),
        label = "pickable_row_press"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .background(if (isSelected) accentColor.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isSelected) accentColor else Color.Transparent)
                .then(
                    if (!isSelected) {
                        Modifier.border(width = 1.5.dp, color = TextTertiary, shape = CircleShape)
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cleanSongTitle(song.title),
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val artist = cleanArtistName(song.artist)
            if (artist.isNotBlank()) {
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Title/artist cleanup helpers (moved from the deleted SongListItem.kt).
/**
 * Clean raw filename into human-readable song title.
 * Handles: underscores, bitrate tags, file extensions, YouTube naming,
 * duplicate words, emoji stripping, quality labels, trailing separators.
 */
private fun cleanSongTitle(raw: String): String {
    var cleaned = raw
    // Remove bitrate patterns: (128k), (320kbps), (192 Kbps), (256K), [128kbps]
    cleaned = cleaned.replace(Regex("[\\[\\(]\\d{2,3}\\s*[kK][bB]?[pP]?[sS]?[\\]\\)]"), "")
    // Remove quality labels: 320Kbps, 128kbps at end of string
    cleaned = cleaned.replace(Regex("[\\s_\\-]\\d{2,3}[kK][bB][pP][sS]?$", RegexOption.IGNORE_CASE), "")
    // Remove file extensions: .mp3, .wav, .flac, .m4a, .ogg, .aac, .wma, .opus
    cleaned = cleaned.replace(Regex("\\.(mp3|wav|flac|m4a|ogg|aac|wma|opus)$", RegexOption.IGNORE_CASE), "")
    // Remove YouTube-style suffixes: "(Official Music Video)", "[Official Video]", "(Lyrics)"
    cleaned = cleaned.replace(Regex("[\\[\\(]((Official )?(Music )?(Video|Audio|Lyric|Lyrics|Audio|Track)|Lyrical Video|Video Song|Full Song|HD|4K|Remix|Extended|Radio Edit|Clean Version|Explicit)[\\]\\)]", RegexOption.IGNORE_CASE), "")
    // Remove channel/artist prefixes common in downloaded files: "ArtistName - "
    // (only if there are multiple separators, suggesting "Artist - Title - Album" pattern)
    val parts = cleaned.split(Regex("[\\s]*[\\-\\|\\~][\\s]*")).map { it.trim() }.filter { it.isNotBlank() }
    cleaned = if (parts.size > 2) {
        // Take last 2 parts (likely Title and Album/Year)
        parts.takeLast(2).joinToString(" - ")
    } else {
        parts.joinToString(" - ")
    }
    // Replace underscores with spaces
    cleaned = cleaned.replace('_', ' ')
    // Strip emoji and non-printable characters
    cleaned = cleaned.replace(Regex("[\\x{1F600}-\\x{1F64F}\\x{1F300}-\\x{1F5FF}\\x{1F680}-\\x{1F6FF}\\x{1F1E0}-\\x{1F1FF}\\x{2600}-\\x{26FF}\\x{2700}-\\x{27BF}]"), "")
    // Remove trailing/leading dashes, dots, spaces
    cleaned = cleaned.replace(Regex("^[\\-\\.\\s]+|[\\-\\.\\s]+$"), "")
    // Remove duplicate consecutive words (case-insensitive)
    val words = cleaned.split(Regex("\\s+"))
    val deduped = mutableListOf<String>()
    for (word in words) {
        if (deduped.isEmpty() || !deduped.last().equals(word, ignoreCase = true)) {
            deduped.add(word)
        }
    }
    cleaned = deduped.joinToString(" ")
    // Replace multiple spaces with single space
    cleaned = cleaned.replace(Regex("\\s+"), " ").trim()
    // Capitalize first letter of each word (title case)
    if (cleaned.isNotEmpty() && cleaned[0].isLowerCase()) {
        cleaned = cleaned.replaceFirstChar { it.uppercase() }
    }
    return cleaned.ifBlank { raw }
}

/**
 * Clean artist name ΓÇö hide if unknown/generic, strip noisy suffixes.
 */
private fun cleanArtistName(raw: String): String {
    val lower = raw.lowercase().trim()
    if (lower in setOf("<unknown>", "unknown artist", "unknown", "", "various artists", "various")) return ""
    // Remove common noise suffixes from downloaded files
    var cleaned = raw.trim()
    cleaned = cleaned.replace(Regex("[\\s]*[\\-\\|][\\s]*(Official|VEVO|Topic|Channel|Music|Records|Entertainment|Productions|Digital|Studio)$", RegexOption.IGNORE_CASE), "")
    cleaned = cleaned.replace(Regex("[\\[\\(]\\s*(Official|VEVO|Topic)\\s*[\\]\\)]$", RegexOption.IGNORE_CASE), "")
    return cleaned.trim().ifBlank { "" }
}
