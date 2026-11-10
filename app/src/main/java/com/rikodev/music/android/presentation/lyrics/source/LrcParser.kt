/*
 * RIKO EXPERIENCE OS — PRODUCTION-GRADE ROADMAP v2.0
 * Milestone 5 — Lyrics Experience
 *
 * File 3 / 12 — LrcParser.kt (Lyrics Source Parser)
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Parses .lrc file format into [LyricsSet]. Pure function — no I/O.
 *
 * LRC FORMAT
 * ----------
 * Standard LRC format:
 *   [mm:ss.xx]Lyric line text
 *   [mm:ss.xx]Another line
 *
 * Enhanced LRC (word/character sync):
 *   [mm:ss.xx] <mm:ss.xx> Word <mm:ss.xx> word
 *
 * ID3 tag style (metadata):
 *   [ar:Artist Name]
 *   [ti:Song Title]
 *   [al:Album Name]
 *   [la:EN]
 *
 * © RIKO Experience OS — Lyrics Source Tier
 */

package com.rikodev.music.android.presentation.lyrics.source

import com.rikodev.music.android.presentation.lyrics.contract.*

// ═══════════════════════════════════════════════════════════════════════════
// LRC PARSER
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Parses LRC format text into a [LyricsSet].
 *
 * Pure function: (lrcText, songId) → LyricsSet
 */
object LrcParser {

    private val TIME_TAG_REGEX = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val METADATA_REGEX = Regex("""\[(ar|ti|al|la|by|offset|length):(.*)]""")
    private val ENHANCED_TIME_REGEX = Regex("""<(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?>""")

    /**
     * Parse [lrcText] into a [LyricsSet] for [songId].
     */
    fun parse(lrcText: String, songId: Long): LyricsSet {
        if (lrcText.isBlank()) return LyricsSet.EMPTY.copy(songId = songId)

        val lines = lrcText.lines()
        val metadata = mutableMapOf<String, String>()
        val parsedLines = mutableListOf<LyricsLine>()
        var lineCounter = 0

        for (rawLine in lines) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) continue

            // Check for metadata tags
            val metadataMatch = METADATA_REGEX.find(trimmed)
            if (metadataMatch != null) {
                val key = metadataMatch.groups[1]?.value ?: continue
                val value = metadataMatch.groups[2]?.value?.trim() ?: ""
                metadata[key] = value
                continue
            }

            // Find all time tags at the start of the line
            // (a single line can have multiple time tags — shared lyrics)
            val timeTags = TIME_TAG_REGEX.findAll(trimmed).toList()
            if (timeTags.isEmpty()) {
                // No time tag — unsynced line
                parsedLines += LyricsLine(
                    lineId = "line_${lineCounter++}",
                    text = trimmed,
                    startTimeMs = null,
                    endTimeMs = null,
                    isBreakable = trimmed.length > 40,
                    breakPositions = findBreakPositions(trimmed),
                )
                continue
            }

            // Extract the lyric text (everything after the last time tag)
            val lastTag = timeTags.last()
            val lyricText = trimmed.substring(lastTag.range.last + 1).trim()

            // For each time tag, create a line (handles shared lyrics)
            for (tag in timeTags) {
                val startTimeMs = parseTimeMs(tag.groupValues)
                parsedLines += LyricsLine(
                    lineId = "line_${lineCounter++}",
                    text = lyricText,
                    startTimeMs = startTimeMs,
                    endTimeMs = null, // will be filled in next pass
                    isBreakable = lyricText.length > 40,
                    breakPositions = findBreakPositions(lyricText),
                )
            }
        }

        // Sort by start time
        val sortedLines = parsedLines.sortedBy { it.startTimeMs ?: Long.MAX_VALUE }

        // Fill in end times (end of line N = start of line N+1)
        val withEndTimes = sortedLines.mapIndexed { index, line ->
            val nextStart = if (index + 1 < sortedLines.size) sortedLines[index + 1].startTimeMs else null
            line.copy(endTimeMs = nextStart ?: (line.startTimeMs?.plus(10_000)))
        }

        val isSynced = withEndTimes.all { it.startTimeMs != null }
        val language = metadata["la"]?.lowercase()

        return LyricsSet(
            songId = songId,
            lines = withEndTimes,
            source = LyricsSource.LOCAL_LRC,
            language = language,
            isSynced = isSynced,
            providerAttribution = null,
            providerUrl = null,
        )
    }

    private fun parseTimeMs(groups: List<String>): Long {
        val minutes = groups.getOrNull(1)?.toLongOrNull() ?: 0L
        val seconds = groups.getOrNull(2)?.toLongOrNull() ?: 0L
        val fractionStr = groups.getOrNull(3)
        val fractionMs = when {
            fractionStr == null -> 0L
            fractionStr.length == 1 -> fractionStr.toLongOrNull()?.times(100) ?: 0L
            fractionStr.length == 2 -> fractionStr.toLongOrNull()?.times(10) ?: 0L
            fractionStr.length == 3 -> fractionStr.toLongOrNull() ?: 0L
            else -> fractionStr.take(3).toLongOrNull() ?: 0L
        }
        return minutes * 60_000 + seconds * 1000 + fractionMs
    }

    private fun findBreakPositions(text: String): List<Int> {
        val positions = mutableListOf<Int>()
        for (i in text.indices) {
            if (text[i].isWhitespace()) positions += i
        }
        return positions
    }

    /**
     * Check if [text] looks like a valid LRC file.
     */
    fun looksLikeLrc(text: String): Boolean {
        if (text.isBlank()) return false
        // Must have at least one time tag
        return TIME_TAG_REGEX.containsMatchIn(text)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ID3 LYRICS EXTRACTOR (USLT/SYLT)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Extracts lyrics from ID3 USLT (Unsynchronized) or SYLT (Synchronized)
 * frames. This is a declaration — the actual extraction happens in
 * the audio metadata reader (not in this package).
 */
object Id3LyricsExtractor {

    /**
     * Convert a USLT frame (unsynchronized lyrics) to a [LyricsSet].
     *
     * USLT frames have:
     *   - language (3-char ISO 639-2)
     *   - content descriptor (description)
     *   - lyrics text (full text, no timing)
     */
    fun fromUslt(
        songId: Long,
        language: String,
        lyricsText: String,
    ): LyricsSet {
        val lines = lyricsText.lines().filter { it.isNotBlank() }.mapIndexed { index, text ->
            LyricsLine(
                lineId = "uslt_line_$index",
                text = text.trim(),
                startTimeMs = null,
                endTimeMs = null,
                isBreakable = text.length > 40,
                breakPositions = emptyList(),
            )
        }
        return LyricsSet(
            songId = songId,
            lines = lines,
            source = LyricsSource.EMBEDDED_ID3,
            language = language.take(2).lowercase(),
            isSynced = false,
            providerAttribution = "Embedded in file",
            providerUrl = null,
        )
    }

    /**
     * Convert a SYLT frame (synchronized lyrics) to a [LyricsSet].
     *
     * SYLT frames have:
     *   - language
     *   - time stamp format (usually MPEG frames or milliseconds)
     *   - content type (lyrics, text, etc.)
     *   - content descriptor
     *   - sync'd text (text + timestamp pairs)
     */
    fun fromSylt(
        songId: Long,
        language: String,
        syncedText: List<Pair<String, Long>>, // (text, timestampMs)
    ): LyricsSet {
        val sorted = syncedText.sortedBy { it.second }
        val lines = sorted.mapIndexed { index, (text, timestamp) ->
            val nextTimestamp = sorted.getOrNull(index + 1)?.second ?: (timestamp + 10_000)
            LyricsLine(
                lineId = "sylt_line_$index",
                text = text,
                startTimeMs = timestamp,
                endTimeMs = nextTimestamp,
                isBreakable = text.length > 40,
                breakPositions = emptyList(),
            )
        }
        return LyricsSet(
            songId = songId,
            lines = lines,
            source = LyricsSource.EMBEDDED_ID3,
            language = language.take(2).lowercase(),
            isSynced = true,
            providerAttribution = "Embedded in file",
            providerUrl = null,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// LYRICS PROVIDER (interface)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Interface for lyrics providers (LRCLIB, Musixmatch, etc.).
 *
 * Implementations live in the host layer. Each provider is responsible
 * for its own authentication, rate limiting, and error handling.
 */
interface LyricsProvider {

    /** The source enum value this provider represents. */
    val source: LyricsSource

    /** Whether this provider is configured (has API key, etc.). */
    val isConfigured: Boolean

    /**
     * Fetch lyrics for a song.
     *
     * @return [LyricsSet] or null if not found.
     * @throws Exception if the request fails (caller handles).
     */
    suspend fun fetch(
        songTitle: String,
        artistName: String,
        albumName: String?,
        durationMs: Long,
    ): LyricsSet?
}

/**
 * No-op provider for when no provider is configured.
 */
object NoOpLyricsProvider : LyricsProvider {
    override val source: LyricsSource = LyricsSource.NONE
    override val isConfigured: Boolean = false
    override suspend fun fetch(
        songTitle: String,
        artistName: String,
        albumName: String?,
        durationMs: Long,
    ): LyricsSet? = null
}

// ═══════════════════════════════════════════════════════════════════════════
// LYRICS FETCHER
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Coordinates lyrics fetching from multiple sources in priority order:
 *   1. Local .lrc file
 *   2. Embedded ID3 frame
 *   3. User-configured API provider
 *
 * Pure orchestration — actual I/O happens in the host layer via
 * [LyricsFetcherCallbacks].
 */
class LyricsFetcher(
    private val callbacks: LyricsFetcherCallbacks,
) {

    suspend fun fetch(songId: Long, songTitle: String, artistName: String, albumName: String?, durationMs: Long): LyricsSet {
        // 1. Try local .lrc file
        callbacks.fetchLocalLrc(songId)?.let { lrcText ->
            if (LrcParser.looksLikeLrc(lrcText)) {
                return LrcParser.parse(lrcText, songId)
            }
        }

        // 2. Try embedded ID3
        callbacks.fetchEmbeddedLyrics(songId)?.let { embedded ->
            return embedded
        }

        // 3. Try configured providers in order
        for (provider in callbacks.getProviders()) {
            if (!provider.isConfigured) continue
            try {
                val result = provider.fetch(songTitle, artistName, albumName, durationMs)
                if (result != null && !result.isEmpty) {
                    return result.copy(songId = songId)
                }
            } catch (e: Exception) {
                // Continue to next provider
            }
        }

        return LyricsSet.EMPTY.copy(songId = songId)
    }
}

/**
 * Callbacks the fetcher needs to access local data and providers.
 */
interface LyricsFetcherCallbacks {
    suspend fun fetchLocalLrc(songId: Long): String?
    suspend fun fetchEmbeddedLyrics(songId: Long): LyricsSet?
    fun getProviders(): List<LyricsProvider>
}
