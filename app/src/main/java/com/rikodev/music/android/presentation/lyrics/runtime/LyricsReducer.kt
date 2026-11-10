/*
 * RIKO EXPERIENCE OS — PRODUCTION-GRADE ROADMAP v2.0
 * Milestone 5 — Lyrics Experience
 *
 * File 2 / 12 — LyricsReducer.kt + Mapper
 * --------------------------------------------------------------------
 * Pure reducer and mapper for lyrics state.
 * © RIKO Experience OS — Lyrics Runtime Tier
 */

package com.rikodev.music.android.presentation.lyrics.runtime

import com.rikodev.music.android.presentation.lyrics.contract.*
import androidx.compose.animation.core.Animatable

// ═══════════════════════════════════════════════════════════════════════════
// REDUCER
// ═══════════════════════════════════════════════════════════════════════════

object LyricsReducer {

    fun reduce(state: LyricsState, event: LyricsEvent): Pair<LyricsState, List<LyricsAction>> {
        return when (event) {
            is LyricsEvent.Shown -> {
                state.copy(
                    phase = LyricsPhase.LOADING,
                    songId = event.songId,
                    lyrics = LyricsSet.EMPTY,
                    currentLineIndex = null,
                    playbackPositionMs = 0,
                    errorMessage = null,
                ) to listOf(LyricsAction.Show(event.songId))
            }

            is LyricsEvent.Hidden -> {
                state.copy(phase = LyricsPhase.HIDDEN) to listOf(LyricsAction.Hide)
            }

            is LyricsEvent.LyricsLoaded -> {
                val newPhase = if (event.lyrics.isEmpty) LyricsPhase.NO_LYRICS else LyricsPhase.READY
                state.copy(
                    phase = newPhase,
                    songId = event.songId,
                    lyrics = event.lyrics,
                    errorMessage = null,
                    isOffline = false,
                ) to emptyList()
            }

            is LyricsEvent.LyricsLoadFailed -> {
                state.copy(
                    phase = if (event.isOffline) LyricsPhase.OFFLINE else LyricsPhase.ERROR,
                    errorMessage = event.error,
                    isOffline = event.isOffline,
                ) to listOf()
            }

            is LyricsEvent.NoLyricsFound -> {
                state.copy(
                    phase = LyricsPhase.NO_LYRICS,
                    lyrics = LyricsSet.EMPTY,
                    errorMessage = null,
                ) to emptyList()
            }

            is LyricsEvent.PlaybackPositionUpdated -> {
                val newState = state.copy(playbackPositionMs = event.positionMs)
                if (state.isFollowingPlayback && state.lyrics.isSynced) {
                    val newLineIndex = findCurrentLine(state.lyrics, event.positionMs)
                    if (newLineIndex != state.currentLineIndex && newLineIndex != null) {
                        newState.copy(currentLineIndex = newLineIndex) to
                            listOf(LyricsAction.PlaybackPositionChanged(event.positionMs))
                    } else {
                        newState to listOf(LyricsAction.PlaybackPositionChanged(event.positionMs))
                    }
                } else {
                    newState to listOf(LyricsAction.PlaybackPositionChanged(event.positionMs))
                }
            }

            is LyricsEvent.CurrentLineChanged -> {
                state.copy(currentLineIndex = event.lineIndex) to emptyList()
            }

            is LyricsEvent.ReadingModeApplied -> {
                state.copy(readingMode = event.mode) to listOf(LyricsAction.ReadingModeChanged(event.mode))
            }

            is LyricsEvent.FontSizeApplied -> {
                state.copy(fontSizeScale = event.scale) to listOf(LyricsAction.FontSizeScaled(event.scale))
            }

            is LyricsEvent.FollowToggled -> {
                state.copy(isFollowingPlayback = event.isFollowing) to listOf(LyricsAction.ToggleFollowPlayback)
            }

            is LyricsEvent.ScrolledToLine -> {
                state.copy(
                    currentLineIndex = event.lineIndex,
                    isFollowingPlayback = false,
                ) to listOf(LyricsAction.ScrollToLine(event.lineIndex))
            }

            is LyricsEvent.LineSeeked -> {
                state.copy(
                    currentLineIndex = event.lineIndex,
                    isFollowingPlayback = true,
                    playbackPositionMs = event.positionMs,
                ) to listOf(LyricsAction.SeekToLine(event.lineIndex))
            }

            is LyricsEvent.Refreshed -> {
                state.copy(phase = LyricsPhase.LOADING, errorMessage = null) to listOf(LyricsAction.Refresh)
            }

            is LyricsEvent.ErrorDismissed -> {
                state.copy(phase = LyricsPhase.HIDDEN, errorMessage = null) to listOf(LyricsAction.DismissError)
            }

            is LyricsEvent.AttributionOpened -> {
                state to listOf(LyricsAction.OpenProviderAttribution(event.url))
            }

            is LyricsEvent.LyricsShared -> {
                state to listOf(LyricsAction.ShareLyrics)
            }
        }
    }

    private fun findCurrentLine(lyrics: LyricsSet, positionMs: Long): Int? {
        if (!lyrics.isSynced) return null
        for ((index, line) in lyrics.lines.withIndex()) {
            val start = line.startTimeMs ?: continue
            val end = line.endTimeMs ?: continue
            if (positionMs in start..end) return index
        }
        // Find the last line whose start is before positionMs
        for (i in lyrics.lines.indices.reversed()) {
            val line = lyrics.lines[i]
            val start = line.startTimeMs ?: continue
            if (start <= positionMs) return i
        }
        return null
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SCREEN COORDINATOR (pure)
// ═══════════════════════════════════════════════════════════════════════════

object LyricsScreenCoordinator {

    fun plan(state: LyricsState, actions: List<LyricsAction>): List<LyricsCoordinatorRequest> {
        val requests = mutableListOf<LyricsCoordinatorRequest>()
        actions.forEach { action ->
            when (action) {
                is LyricsAction.Show -> {
                    // Host layer needs song metadata to fetch lyrics
                    // In a real impl, the ViewModel would pass title/artist/duration
                    requests += LyricsCoordinatorRequest.FetchLyrics(
                        songId = action.songId,
                        songTitle = "", // filled by host layer from repository
                        artistName = "",
                        durationMs = 0,
                    )
                }
                is LyricsAction.Hide -> {
                    requests += LyricsCoordinatorRequest.CancelFetch
                }
                is LyricsAction.PlaybackPositionChanged -> {
                    // No external action needed; state update only
                }
                is LyricsAction.ReadingModeChanged -> {
                    requests += LyricsCoordinatorRequest.EmitHaptic
                }
                is LyricsAction.FontSizeScaled -> {
                    requests += LyricsCoordinatorRequest.EmitHaptic
                }
                is LyricsAction.ToggleFollowPlayback -> {
                    requests += LyricsCoordinatorRequest.EmitHaptic
                }
                is LyricsAction.ScrollToLine -> {
                    requests += LyricsCoordinatorRequest.EmitHaptic
                }
                is LyricsAction.SeekToLine -> {
                    val line = state.lyrics.lines.getOrNull(action.lineIndex)
                    if (line != null && line.startTimeMs != null) {
                        requests += LyricsCoordinatorRequest.SeekPlayback(line.startTimeMs)
                        requests += LyricsCoordinatorRequest.EmitHaptic
                    }
                }
                is LyricsAction.Refresh -> {
                    requests += LyricsCoordinatorRequest.EmitHaptic
                }
                is LyricsAction.DismissError -> {
                    // No external action needed
                }
                is LyricsAction.OpenProviderAttribution -> {
                    requests += LyricsCoordinatorRequest.OpenUrl(action.url)
                }
                is LyricsAction.ShareLyrics -> {
                    val text = state.lyrics.lines.joinToString("\n") { it.text }
                    requests += LyricsCoordinatorRequest.ShareText(text)
                }
            }
        }
        return requests
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// LINE BREAKER
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Breaks long lyric lines into segments for narrow displays.
 *
 * Pure function — deterministic given the same line and width budget.
 */
object LineBreaker {

    /**
     * Break [line] into segments, each fitting within [maxChars].
     *
     * Prefers breaking at spaces. Falls back to hard break if no space available.
     */
    fun breakLine(line: LyricsLine, maxChars: Int): BrokenLine {
        if (line.text.length <= maxChars) {
            return BrokenLine(
                originalLineId = line.lineId,
                segments = listOf(LineSegment(line.text, 0, line.text.length)),
            )
        }
        val segments = mutableListOf<LineSegment>()
        var currentStart = 0
        while (currentStart < line.text.length) {
            val remaining = line.text.length - currentStart
            val targetEnd = if (remaining <= maxChars) {
                line.text.length
            } else {
                findBreakPoint(line.text, currentStart, maxChars)
            }
            segments += LineSegment(
                text = line.text.substring(currentStart, targetEnd).trim(),
                startIndex = currentStart,
                endIndex = targetEnd,
            )
            currentStart = targetEnd
            // Skip whitespace at the start of the next segment
            while (currentStart < line.text.length && line.text[currentStart].isWhitespace()) {
                currentStart++
            }
        }
        return BrokenLine(originalLineId = line.lineId, segments = segments)
    }

    private fun findBreakPoint(text: String, start: Int, maxChars: Int): Int {
        val target = start + maxChars
        // Look for the last space before target
        for (i in target - 1 downTo start + 1) {
            if (text[i].isWhitespace()) return i
        }
        // No space found — hard break at target
        return target.coerceAtMost(text.length)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MAPPER (Rule #59 — last translator)
// ═══════════════════════════════════════════════════════════════════════════

data class LyricsUiState(
    val phase: LyricsPhaseUi,
    val lines: List<LyricsLineUi>,
    val currentLineIndex: Int?,
    val readingModeLabel: String,
    val fontSizeScale: Float,
    val isFollowingPlayback: Boolean,
    val attributionLabel: String?,
    val providerUrl: String?,
    val isSynced: Boolean,
    val languageLabel: String?,
    val accessibilityLabel: String,
    val emptyStateTitle: String,
    val emptyStateHint: String,
)

enum class LyricsPhaseUi(val displayName: String) {
    HIDDEN("Hidden"),
    LOADING("Loading"),
    READY("Ready"),
    NO_LYRICS("No Lyrics"),
    ERROR("Error"),
    OFFLINE("Offline"),
}

data class LyricsLineUi(
    val lineId: String,
    val text: String,
    val startTimeLabel: String?,
    val highlightState: LineHighlightState,
    val isInstrumental: Boolean,
    val accessibilityLabel: String,
)

object LyricsMapper {

    fun map(state: LyricsState): LyricsUiState {
        val phaseUi = mapPhase(state.phase)
        val lines = state.lyrics.lines.mapIndexed { index, line ->
            mapLine(line, index, state.currentLineIndex)
        }
        val attribution = state.lyrics.providerAttribution
        val language = state.lyrics.language?.let { code ->
            when (code.lowercase()) {
                "en" -> "English"
                "bn" -> "Bangla"
                "hi" -> "Hindi"
                "ar" -> "Arabic"
                "es" -> "Spanish"
                "fr" -> "French"
                "de" -> "German"
                "ja" -> "Japanese"
                "ko" -> "Korean"
                "zh" -> "Chinese"
                else -> code.uppercase()
            }
        }
        val emptyTitle = when (state.phase) {
            LyricsPhase.NO_LYRICS -> "No lyrics for this song"
            LyricsPhase.ERROR -> "Couldn't load lyrics"
            LyricsPhase.OFFLINE -> "You're offline"
            else -> ""
        }
        val emptyHint = when (state.phase) {
            LyricsPhase.NO_LYRICS -> "Try adding a .lrc file alongside this song"
            LyricsPhase.ERROR -> state.errorMessage ?: "Tap refresh to try again"
            LyricsPhase.OFFLINE -> "Lyrics will load when you reconnect"
            else -> ""
        }
        val accessibility = buildString {
            append("Lyrics, ${phaseUi.displayName}")
            if (state.songId != null) append(", song ${state.songId}")
            if (state.currentLineIndex != null) append(", line ${state.currentLineIndex + 1}")
            if (state.lyrics.isSynced) append(", synced")
        }

        return LyricsUiState(
            phase = phaseUi,
            lines = lines,
            currentLineIndex = state.currentLineIndex,
            readingModeLabel = state.readingMode.displayName,
            fontSizeScale = state.fontSizeScale,
            isFollowingPlayback = state.isFollowingPlayback,
            attributionLabel = attribution,
            providerUrl = state.lyrics.providerUrl,
            isSynced = state.lyrics.isSynced,
            languageLabel = language,
            accessibilityLabel = accessibility,
            emptyStateTitle = emptyTitle,
            emptyStateHint = emptyHint,
        )
    }

    private fun mapPhase(phase: LyricsPhase): LyricsPhaseUi = when (phase) {
        LyricsPhase.HIDDEN -> LyricsPhaseUi.HIDDEN
        LyricsPhase.LOADING -> LyricsPhaseUi.LOADING
        LyricsPhase.READY -> LyricsPhaseUi.READY
        LyricsPhase.NO_LYRICS -> LyricsPhaseUi.NO_LYRICS
        LyricsPhase.ERROR -> LyricsPhaseUi.ERROR
        LyricsPhase.OFFLINE -> LyricsPhaseUi.OFFLINE
    }

    private fun mapLine(line: LyricsLine, index: Int, currentIndex: Int?): LyricsLineUi {
        val highlight = when {
            index == currentIndex -> LineHighlightState.HIGHLIGHTED
            currentIndex != null && index == currentIndex - 1 -> LineHighlightState.PRE_HIGHLIGHT
            currentIndex != null && index == currentIndex + 1 -> LineHighlightState.POST_HIGHLIGHT
            else -> LineHighlightState.NOT_HIGHLIGHTED
        }
        val timeLabel = line.startTimeMs?.let { ms ->
            val total = ms / 1000
            val m = total / 60
            val s = total % 60
            "$m:${s.toString().padStart(2, '0')}"
        }
        val a11y = buildString {
            append("Line ${index + 1}")
            if (line.isInstrumental) {
                append(", instrumental break")
            } else {
                append(": ${line.text}")
            }
            if (highlight == LineHighlightState.HIGHLIGHTED) {
                append(". Currently playing")
            }
        }
        return LyricsLineUi(
            lineId = line.lineId,
            text = line.text,
            startTimeLabel = timeLabel,
            highlightState = highlight,
            isInstrumental = line.isInstrumental,
            accessibilityLabel = a11y,
        )
    }
}
