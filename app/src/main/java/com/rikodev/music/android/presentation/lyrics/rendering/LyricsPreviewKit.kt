/*
 * RIKO EXPERIENCE OS — PRODUCTION-GRADE ROADMAP v2.0
 * Milestone 5 — Lyrics Experience
 *
 * File 10 / 12 — LyricsPreviewKit.kt (Rule #77)
 * --------------------------------------------------------------------
 * Living documentation for the Lyrics experience.
 * © RIKO Experience OS — Lyrics Rendering Tier
 */

package com.rikodev.music.android.presentation.lyrics.rendering

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.rikodev.music.android.presentation.lyrics.contract.LineHighlightState
import com.rikodev.music.android.presentation.lyrics.contract.LyricsReadingMode
import com.rikodev.music.android.presentation.lyrics.contract.LyricsSource
import com.rikodev.music.android.presentation.lyrics.runtime.*

object LyricsPreviewData {

    fun ready() = LyricsUiState(
        phase = LyricsPhaseUi.READY,
        lines = listOf(
            LyricsLineUi("l1", "In the midnight silence", "0:12", LineHighlightState.NOT_HIGHLIGHTED, false, "Line 1"),
            LyricsLineUi("l2", "When the city falls asleep", "0:16", LineHighlightState.PRE_HIGHLIGHT, false, "Line 2"),
            LyricsLineUi("l3", "I hear your voice calling", "0:21", LineHighlightState.HIGHLIGHTED, false, "Line 3. Currently playing"),
            LyricsLineUi("l4", "Through the velvet deep", "0:26", LineHighlightState.POST_HIGHLIGHT, false, "Line 4"),
            LyricsLineUi("l5", "♪ ♪ ♪", null, LineHighlightState.NOT_HIGHLIGHTED, true, "Line 5, instrumental break"),
            LyricsLineUi("l6", "And the stars they whisper", "0:35", LineHighlightState.NOT_HIGHLIGHTED, false, "Line 6"),
        ),
        currentLineIndex = 2,
        readingModeLabel = LyricsReadingMode.FOCUS_ONE_LINE.displayName,
        fontSizeScale = 1.0f,
        isFollowingPlayback = true,
        attributionLabel = "Provided by LRCLIB",
        providerUrl = "https://lrclib.net",
        isSynced = true,
        languageLabel = "English",
        accessibilityLabel = "Lyrics, Ready, line 3, synced",
        emptyStateTitle = "",
        emptyStateHint = "",
    )

    fun loading() = LyricsUiState(
        phase = LyricsPhaseUi.LOADING,
        lines = emptyList(),
        currentLineIndex = null,
        readingModeLabel = "Focus One Line",
        fontSizeScale = 1.0f,
        isFollowingPlayback = true,
        attributionLabel = null,
        providerUrl = null,
        isSynced = false,
        languageLabel = null,
        accessibilityLabel = "Lyrics, Loading",
        emptyStateTitle = "",
        emptyStateHint = "",
    )

    fun noLyrics() = LyricsUiState(
        phase = LyricsPhaseUi.NO_LYRICS,
        lines = emptyList(),
        currentLineIndex = null,
        readingModeLabel = "Focus One Line",
        fontSizeScale = 1.0f,
        isFollowingPlayback = true,
        attributionLabel = null,
        providerUrl = null,
        isSynced = false,
        languageLabel = null,
        accessibilityLabel = "Lyrics, No Lyrics",
        emptyStateTitle = "No lyrics for this song",
        emptyStateHint = "Try adding a .lrc file alongside this song",
    )

    fun offline() = LyricsUiState(
        phase = LyricsPhaseUi.OFFLINE,
        lines = emptyList(),
        currentLineIndex = null,
        readingModeLabel = "Focus One Line",
        fontSizeScale = 1.0f,
        isFollowingPlayback = true,
        attributionLabel = null,
        providerUrl = null,
        isSynced = false,
        languageLabel = null,
        accessibilityLabel = "Lyrics, Offline",
        emptyStateTitle = "You're offline",
        emptyStateHint = "Lyrics will load when you reconnect",
    )

    fun banglaLyrics() = LyricsUiState(
        phase = LyricsPhaseUi.READY,
        lines = listOf(
            LyricsLineUi("b1", "মধ্যরাতের নীরবতায়", "0:12", LineHighlightState.NOT_HIGHLIGHTED, false, "Line 1"),
            LyricsLineUi("b2", "শহর যখন ঘুমিয়ে পড়ে", "0:16", LineHighlightState.PRE_HIGHLIGHT, false, "Line 2"),
            LyricsLineUi("b3", "তোমার গলা শুনি ডাকছে", "0:21", LineHighlightState.HIGHLIGHTED, false, "Line 3. Currently playing"),
            LyricsLineUi("b4", "মখমলের গভীরতায়", "0:26", LineHighlightState.POST_HIGHLIGHT, false, "Line 4"),
        ),
        currentLineIndex = 2,
        readingModeLabel = LyricsReadingMode.FOCUS_ONE_LINE.displayName,
        fontSizeScale = 1.0f,
        isFollowingPlayback = true,
        attributionLabel = "Local .lrc file",
        providerUrl = null,
        isSynced = true,
        languageLabel = "Bangla",
        accessibilityLabel = "Bangla lyrics, Ready",
        emptyStateTitle = "",
        emptyStateHint = "",
    )
}

@Preview(name = "Lyrics — Ready (Synced, English)", showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun Preview_Lyrics_Ready() {
    Box(modifier = Modifier.background(Color(0xFF050505)).fillMaxSize()) {
        PreviewLyricsScreen(LyricsPreviewData.ready())
    }
}

@Preview(name = "Lyrics — Loading", showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun Preview_Lyrics_Loading() {
    Box(modifier = Modifier.background(Color(0xFF050505)).fillMaxSize()) {
        PreviewLyricsScreen(LyricsPreviewData.loading())
    }
}

@Preview(name = "Lyrics — No Lyrics (Empty State)", showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun Preview_Lyrics_NoLyrics() {
    Box(modifier = Modifier.background(Color(0xFF050505)).fillMaxSize()) {
        PreviewLyricsScreen(LyricsPreviewData.noLyrics())
    }
}

@Preview(name = "Lyrics — Offline", showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun Preview_Lyrics_Offline() {
    Box(modifier = Modifier.background(Color(0xFF050505)).fillMaxSize()) {
        PreviewLyricsScreen(LyricsPreviewData.offline())
    }
}

@Preview(name = "Lyrics — Bangla (Non-Latin Script)", showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun Preview_Lyrics_Bangla() {
    Box(modifier = Modifier.background(Color(0xFF050505)).fillMaxSize()) {
        PreviewLyricsScreen(LyricsPreviewData.banglaLyrics())
    }
}

@Composable
private fun PreviewLyricsScreen(state: LyricsUiState) {
    LyricsScreen(
        state = state,
        onLineTap = {},
        onRefresh = {},
        onShare = {},
        onAttributionTap = {},
        modifier = Modifier.fillMaxSize(),
    )
}
