/*
 * RIKO EXPERIENCE OS — PRODUCTION-GRADE ROADMAP v2.0
 * Milestone 5 — Lyrics Experience
 *
 * File 1 / 12 — LyricsContract.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Pure-Kotlin contract for the Lyrics Experience. Lyrics are NOT
 * karaoke spam. One line focus, emotion-forward, reading-friendly.
 *
 * COPYRIGHT POLICY (v2.0 Security pillar)
 * ----------------------------------------
 * RIKO does NOT store lyrics in its own database. Lyrics are fetched
 * on-demand from user-configured sources:
 *   1. Local .lrc files alongside music files (user-owned)
 *   2. User-configured API (e.g. LRCLIB, Musixmatch with API key)
 *   3. Embedded ID3 USLT/SYLT frames (user-owned)
 *
 * RIKO never scrapes lyrics from websites. RIKO never bundles lyrics
 * in the APK. The user is responsible for providing legal access to
 * lyrics content.
 *
 * RULES ENFORCED
 * --------------
 * #1   Single Source of Identity
 * #45  Events Are Verbs (Past Tense)
 * #48  Actions Are Nouns
 * #58  Runtime Never Knows Rendering
 * #70  Architecture Freeze
 * v2.0 Security pillar — copyright-safe lyrics handling
 *
 * © RIKO Experience OS — Lyrics Tier
 */

package com.rikodev.music.android.presentation.lyrics.contract

// ═══════════════════════════════════════════════════════════════════════════
// LYRICS LINE
// ═══════════════════════════════════════════════════════════════════════════

/**
 * A single line of lyrics with optional timing.
 *
 * @property lineId           Stable ID within this lyric set.
 * @property text             The lyric text (may be empty for instrumental).
 * @property startTimeMs      When this line starts (synced lyrics), or null.
 * @property endTimeMs        When this line ends (synced lyrics), or null.
 * @property isBreakable      True if the line can be split for narrow displays.
 * @property breakPositions   Character positions where the line can break.
 */
data class LyricsLine(
    val lineId: String,
    val text: String,
    val startTimeMs: Long?,
    val endTimeMs: Long?,
    val isBreakable: Boolean,
    val breakPositions: List<Int>,
) {
    init {
        if (startTimeMs != null) require(startTimeMs >= 0) { "Start time cannot be negative" }
        if (endTimeMs != null) require(endTimeMs >= 0) { "End time cannot be negative" }
        if (startTimeMs != null && endTimeMs != null) {
            require(endTimeMs >= startTimeMs) { "End must be >= start" }
        }
    }

    /** True if this line is an instrumental break (no text). */
    val isInstrumental: Boolean get() = text.isBlank()

    /** True if this line has timing info (synced lyrics). */
    val isSynced: Boolean get() = startTimeMs != null
}

// ═══════════════════════════════════════════════════════════════════════════
// LYRICS SET
// ═══════════════════════════════════════════════════════════════════════════

/**
 * A complete set of lyrics for a song.
 *
 * @property songId           The song these lyrics belong to.
 * @property lines            Ordered list of lyric lines.
 * @property source           Where the lyrics came from.
 * @property language         ISO 639-1 language code (e.g. "en", "bn").
 * @property isSynced         True if all lines have timing info.
 * @property providerAttribution  Attribution string (e.g. "Provided by LRCLIB").
 * @property providerUrl      Optional URL to the source (for attribution link).
 */
data class LyricsSet(
    val songId: Long,
    val lines: List<LyricsLine>,
    val source: LyricsSource,
    val language: String?,
    val isSynced: Boolean,
    val providerAttribution: String?,
    val providerUrl: String?,
) {
    val isEmpty: Boolean get() = lines.isEmpty()
    val lineCount: Int get() = lines.size

    companion object {
        val EMPTY = LyricsSet(
            songId = -1,
            lines = emptyList(),
            source = LyricsSource.NONE,
            language = null,
            isSynced = false,
            providerAttribution = null,
            providerUrl = null,
        )
    }
}

/**
 * Where the lyrics came from.
 */
enum class LyricsSource(val displayName: String, val isUserOwned: Boolean) {
    NONE("No lyrics", true),
    LOCAL_LRC("Local .lrc file", true),
    EMBEDDED_ID3("Embedded in file", true),
    LRCLIB("LRCLIB API", false),
    MUSIXMATCH("Musixmatch API", false),
    GENIUS("Genius API", false),
    NETEASE("NetEase API", false),
    CUSTOM("Custom provider", false),
}

// ═══════════════════════════════════════════════════════════════════════════
// LYRICS STATE
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The phase of the lyrics experience.
 */
enum class LyricsPhase(val displayName: String) {
    HIDDEN("Hidden"),
    LOADING("Loading"),
    READY("Ready"),
    NO_LYRICS("No Lyrics"),
    ERROR("Error"),
    OFFLINE("Offline"),
}

/**
 * The reading mode for lyrics.
 */
enum class LyricsReadingMode(val displayName: String) {
    /** One line at a time, large text, focused. */
    FOCUS_ONE_LINE("Focus One Line"),

    /** Three lines visible (previous, current, next). */
    THREE_LINE("Three Line"),

    /** Full karaoke-style scroll. */
    SCROLL("Scroll"),
}

/**
 * The highlight state for the current line.
 */
enum class LineHighlightState {
    NOT_HIGHLIGHTED,
    PRE_HIGHLIGHT,
    HIGHLIGHTED,
    POST_HIGHLIGHT,
}

/**
 * The complete lyrics state.
 */
data class LyricsState(
    val phase: LyricsPhase,
    val songId: Long?,
    val lyrics: LyricsSet,
    val currentLineIndex: Int?,
    val readingMode: LyricsReadingMode,
    val playbackPositionMs: Long,
    val scrollOffsetPx: Int,
    val isFollowingPlayback: Boolean,
    val fontSizeScale: Float,
    val errorMessage: String?,
    val isOffline: Boolean,
) {
    companion object {
        val INITIAL = LyricsState(
            phase = LyricsPhase.HIDDEN,
            songId = null,
            lyrics = LyricsSet.EMPTY,
            currentLineIndex = null,
            readingMode = LyricsReadingMode.FOCUS_ONE_LINE,
            playbackPositionMs = 0,
            scrollOffsetPx = 0,
            isFollowingPlayback = true,
            fontSizeScale = 1.0f,
            errorMessage = null,
            isOffline = false,
        )
    }

    val hasLyrics: Boolean get() = phase == LyricsPhase.READY && !lyrics.isEmpty
    val isSynced: Boolean get() = lyrics.isSynced
}

// ═══════════════════════════════════════════════════════════════════════════
// LYRICS ACTION
// ═══════════════════════════════════════════════════════════════════════════

sealed class LyricsAction {
    data class Show(val songId: Long) : LyricsAction()
    data object Hide : LyricsAction()
    data class PlaybackPositionChanged(val positionMs: Long) : LyricsAction()
    data class ReadingModeChanged(val mode: LyricsReadingMode) : LyricsAction()
    data class FontSizeScaled(val scale: Float) : LyricsAction()
    data object ToggleFollowPlayback : LyricsAction()
    data class ScrollToLine(val lineIndex: Int) : LyricsAction()
    data class SeekToLine(val lineIndex: Int) : LyricsAction()
    data object Refresh : LyricsAction()
    data object DismissError : LyricsAction()
    data class OpenProviderAttribution(val url: String) : LyricsAction()
    data object ShareLyrics : LyricsAction()
}

// ═══════════════════════════════════════════════════════════════════════════
// LYRICS EVENT
// ═══════════════════════════════════════════════════════════════════════════

sealed class LyricsEvent {
    data class Shown(val songId: Long) : LyricsEvent()
    data object Hidden : LyricsEvent()
    data class LyricsLoaded(val songId: Long, val lyrics: LyricsSet) : LyricsEvent()
    data class LyricsLoadFailed(val songId: Long, val error: String, val isOffline: Boolean) : LyricsEvent()
    data class NoLyricsFound(val songId: Long) : LyricsEvent()
    data class PlaybackPositionUpdated(val positionMs: Long) : LyricsEvent()
    data class CurrentLineChanged(val lineIndex: Int) : LyricsEvent()
    data class ReadingModeApplied(val mode: LyricsReadingMode) : LyricsEvent()
    data class FontSizeApplied(val scale: Float) : LyricsEvent()
    data class FollowToggled(val isFollowing: Boolean) : LyricsEvent()
    data class ScrolledToLine(val lineIndex: Int) : LyricsEvent()
    data class LineSeeked(val lineIndex: Int, val positionMs: Long) : LyricsEvent()
    data object Refreshed : LyricsEvent()
    data object ErrorDismissed : LyricsEvent()
    data class AttributionOpened(val url: String) : LyricsEvent()
    data object LyricsShared : LyricsEvent()
}

// ═══════════════════════════════════════════════════════════════════════════
// LYRICS COORDINATOR REQUEST
// ═══════════════════════════════════════════════════════════════════════════

sealed class LyricsCoordinatorRequest {
    data class FetchLyrics(val songId: Long, val songTitle: String, val artistName: String, val durationMs: Long) : LyricsCoordinatorRequest()
    data class SeekPlayback(val positionMs: Long) : LyricsCoordinatorRequest()
    data object EmitHaptic : LyricsCoordinatorRequest()
    data class ShowSnackbar(val message: String) : LyricsCoordinatorRequest()
    data class OpenUrl(val url: String) : LyricsCoordinatorRequest()
    data class ShareText(val text: String) : LyricsCoordinatorRequest()
    data object CancelFetch : LyricsCoordinatorRequest()
}

// ═══════════════════════════════════════════════════════════════════════════
// LINE BREAKER
// ═══════════════════════════════════════════════════════════════════════════

/**
 * A line broken into segments for narrow displays.
 */
data class BrokenLine(
    val originalLineId: String,
    val segments: List<LineSegment>,
)

data class LineSegment(
    val text: String,
    val startIndex: Int,
    val endIndex: Int,
)
