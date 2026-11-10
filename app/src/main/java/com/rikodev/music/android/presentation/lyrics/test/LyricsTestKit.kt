/*
 * RIKO EXPERIENCE OS — PRODUCTION-GRADE ROADMAP v2.0
 * Milestone 5 — Lyrics Experience
 *
 * File 9 / 12 — LyricsTestKit.kt + Production Gate
 * --------------------------------------------------------------------
 * Test specs and production gate for Lyrics.
 * © RIKO Experience OS — Lyrics Test Tier
 */

package com.rikodev.music.android.presentation.lyrics.test

import com.rikodev.music.android.presentation.lyrics.contract.*

data class LyricsTestCase(val id: String, val category: LyricsTestCategory, val description: String)

enum class LyricsTestCategory(val displayName: String) {
    PARSER("LRC Parser Test"),
    REDUCER("Reducer Test"),
    LINE_BREAKER("Line Breaker Test"),
    SYNC("Sync Test"),
    FALLBACK("Fallback Test"),
    OFFLINE("Offline Test"),
    NON_LATIN("Non-Latin Script Test"),
}

object LyricsTestRegistry {
    val allTestCases: List<LyricsTestCase> = listOf(
        LyricsTestCase("L-PAR-001", LyricsTestCategory.PARSER, "Standard LRC parses correctly"),
        LyricsTestCase("L-PAR-002", LyricsTestCategory.PARSER, "Enhanced LRC with word sync parses"),
        LyricsTestCase("L-PAR-003", LyricsTestCategory.PARSER, "Metadata tags (ar/ti/al/la) extracted"),
        LyricsTestCase("L-PAR-004", LyricsTestCategory.PARSER, "Multi-timestamp line creates multiple lines"),
        LyricsTestCase("L-PAR-005", LyricsTestCategory.PARSER, "Empty LRC returns empty set"),
        LyricsTestCase("L-PAR-006", LyricsTestCategory.PARSER, "Malformed timestamps are skipped"),
        LyricsTestCase("L-RED-001", LyricsTestCategory.REDUCER, "Show event transitions to LOADING"),
        LyricsTestCase("L-RED-002", LyricsTestCategory.REDUCER, "LyricsLoaded transitions to READY"),
        LyricsTestCase("L-RED-003", LyricsTestCategory.REDUCER, "NoLyricsFound transitions to NO_LYRICS"),
        LyricsTestCase("L-RED-004", LyricsTestCategory.REDUCER, "PlaybackPositionUpdated finds current line"),
        LyricsTestCase("L-RED-005", LyricsTestCategory.REDUCER, "LineSeeked updates current and seeks"),
        LyricsTestCase("L-LBR-001", LyricsTestCategory.LINE_BREAKER, "Short line returns single segment"),
        LyricsTestCase("L-LBR-002", LyricsTestCategory.LINE_BREAKER, "Long line breaks at spaces"),
        LyricsTestCase("L-LBR-003", LyricsTestCategory.LINE_BREAKER, "No-space line hard-breaks"),
        LyricsTestCase("L-SYN-001", LyricsTestCategory.SYNC, "Synced lyrics auto-scroll to current line"),
        LyricsTestCase("L-SYN-002", LyricsTestCategory.SYNC, "Tap on synced line seeks to that time"),
        LyricsTestCase("L-SYN-003", LyricsTestCategory.SYNC, "Unsynced lyrics do not auto-scroll"),
        LyricsTestCase("L-FAL-001", LyricsTestCategory.FALLBACK, "Local LRC takes priority over API"),
        LyricsTestCase("L-FAL-002", LyricsTestCategory.FALLBACK, "Embedded ID3 takes priority over API"),
        LyricsTestCase("L-FAL-003", LyricsTestCategory.FALLBACK, "API provider tried when local not found"),
        LyricsTestCase("L-FAL-004", LyricsTestCategory.FALLBACK, "All sources fail → NoLyricsFound"),
        LyricsTestCase("L-OFF-001", LyricsTestCategory.OFFLINE, "Offline + local LRC available → load succeeds"),
        LyricsTestCase("L-OFF-002", LyricsTestCategory.OFFLINE, "Offline + only API available → OFFLINE phase"),
        LyricsTestCase("L-OFF-003", LyricsTestCategory.OFFLINE, "Reconnect → refresh loads lyrics"),
        LyricsTestCase("L-NON-001", LyricsTestCategory.NON_LATIN, "Bangla lyrics render correctly"),
        LyricsTestCase("L-NON-002", LyricsTestCategory.NON_LATIN, "Bangla .lrc file parses correctly"),
        LyricsTestCase("L-NON-003", LyricsTestCategory.NON_LATIN, "CJK lyrics line-break correctly"),
        LyricsTestCase("L-NON-004", LyricsTestCategory.NON_LATIN, "RTL languages (Arabic/Hebrew) render correctly"),
    )
}

object LyricsProductionGate {

    data class PerformanceBudget(
        val lrcParseMs: Long,
        val fetchLatencyMs: Long,
        val autoScrollAnimationMs: Long,
        val maxRecompositionsPerLineChange: Int,
        val lineBreakMs: Long,
    )

    val performanceBudget = PerformanceBudget(
        lrcParseMs = 20,
        fetchLatencyMs = 2_000,
        autoScrollAnimationMs = 320,
        maxRecompositionsPerLineChange = 3,
        lineBreakMs = 5,
    )

    val securityRequirements = listOf(
        "RIKO does NOT store lyrics in its own database",
        "RIKO does NOT scrape lyrics from websites",
        "RIKO does NOT bundle lyrics in the APK",
        "Lyrics fetched from user-configured sources only",
        "API keys stored in EncryptedSharedPreferences",
        "Provider attribution always displayed (copyright compliance)",
        "Lyrics provider URL clickable for source attribution",
        "Lyrics share includes attribution",
    )

    val accessibilityRequirements = listOf(
        "Each lyric line has contentDescription (line number + text)",
        "Highlighted line announced as 'Currently playing'",
        "Instrumental breaks announced as 'instrumental break'",
        "Refresh button has contentDescription 'Refresh lyrics'",
        "Share button has contentDescription 'Share lyrics'",
        "Attribution link has contentDescription",
        "Font size scale supports up to 200%",
    )

    val observabilityEvents = listOf(
        "lyrics_show", "lyrics_hide", "lyrics_loaded", "lyrics_not_found",
        "lyrics_error", "lyrics_seek", "lyrics_refresh", "lyrics_share",
        "lyrics_attribution_opened", "lyrics_offline",
    )

    val resilienceScenarios = listOf(
        "Lyrics API down → lyrics tab silently unavailable, no crash",
        "Lyrics API rate-limited → graceful message, retry after delay",
        "Malformed LRC → skipped lines, valid lines still shown",
        "Empty LRC file → NO_LYRICS phase",
        "Offline + local LRC → loads successfully",
        "Offline + API only → OFFLINE phase with retry hint",
        "Very long lyrics (500+ lines) → smooth scroll",
        "Process death → lyrics re-fetched on restore",
        "Song with no metadata → NoLyricsFound",
        "Non-Latin lyrics (Bangla/CJK/RTL) → correct rendering",
    )

    val i18nStrings = listOf(
        "lyrics_title", "lyrics_loading", "lyrics_synced", "lyrics_unsynced",
        "lyrics_no_lyrics_title", "lyrics_no_lyrics_hint",
        "lyrics_error_title", "lyrics_error_hint",
        "lyrics_offline_title", "lyrics_offline_hint",
        "lyrics_refresh", "lyrics_share", "lyrics_attribution",
        "lyrics_instrumental", "lyrics_line_number",
        "lyrics_reading_mode_focus", "lyrics_reading_mode_three_line", "lyrics_reading_mode_scroll",
    )

    val modularityRules = listOf(
        "Lyrics contract package has ZERO Android imports",
        "Lyrics source package has ZERO Android imports (parser is pure)",
        "Lyrics runtime package has ZERO Android imports",
        "Lyrics rendering package has Compose imports only",
        "Lyrics host package is the ONLY place with Android imports",
        "LyricsProvider implementations may use Android network APIs",
    )

    fun summary(): String = buildString {
        appendLine("═══ M5 LYRICS — PRODUCTION GATE SUMMARY ═══")
        appendLine()
        appendLine("🧪 Tests: ${LyricsTestRegistry.allTestCases.size}")
        appendLine("📊 Perf: LRC parse <${performanceBudget.lrcParseMs}ms, fetch <${performanceBudget.fetchLatencyMs}ms")
        appendLine("🔒 Security: ${securityRequirements.size} requirements (copyright-safe)")
        appendLine("♿ Accessibility: ${accessibilityRequirements.size} requirements")
        appendLine("📡 Observability: ${observabilityEvents.size} events")
        appendLine("🛡 Resilience: ${resilienceScenarios.size} scenarios")
        appendLine("🌍 i18n: ${i18nStrings.size} strings (incl. non-Latin)")
        appendLine("🧩 Modularity: ${modularityRules.size} rules")
        appendLine()
        appendLine("Definition of Done: Lyrics API down → player does not break, lyrics tab silently unavailable.")
    }
}
