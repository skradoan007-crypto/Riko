/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B1 — Collection Contract Pack v2 (Final)
 *
 * File 11 / 15 — CollectionHeaderContract.kt  ⭐ NEW in v2
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * The Library Header is the topmost surface of the Library screen.
 * It is NOT a section header — it is the *identity* of the entire
 * Library at a moment in time. It greets the user, sets context,
 * and ties the Library together.
 *
 * In v1 the header was hardcoded. In v2 it is *declared* here, so
 * future dynamic headers (time-aware, mood-aware, weather-aware)
 * have a stable contract to render against.
 *
 * RULE #34 — Header Explains Context
 * RULE #40 — Metadata Lives Forever
 *
 * The Header contract declares:
 *   • Greeting        — first-person address to the user
 *   • Subtitle        — single line of context
 *   • Context Line    — optional third line (e.g. weather, time)
 *   • Badge           — small status indicator (e.g. "scanning")
 *   • Chip            — actionable chip (e.g. "Shuffle All")
 *   • Wave Mode       — how the header's wave (if any) renders
 *   • Collapse Behaviour — what happens on scroll
 *   • Animation Style — entry / exit motion
 *   • Glass Strength  — background frost level
 *
 * No logic. Pure declaration.
 */

package com.rikodev.music.android.presentation.library.contract

/**
 * The greeting the user sees when they open the Library.
 *
 * Greetings are NOT generated at runtime — they are declared here
 * and selected by the runtime based on context (time of day, day
 * of week, season, etc.).
 */
enum class HeaderGreeting(
    val displayText: String,
    val internalId: String,
    val description: String,
) {
    EARLY_MORNING(
        displayText = "Good morning",
        internalId = "early_morning",
        description = "5:00 – 9:00 — fresh, optimistic.",
    ),

    LATE_MORNING(
        displayText = "Hello",
        internalId = "late_morning",
        description = "9:00 – 12:00 — neutral, daytime.",
    ),

    AFTERNOON(
        displayText = "Good afternoon",
        internalId = "afternoon",
        description = "12:00 – 17:00 — neutral, daytime.",
    ),

    EVENING(
        displayText = "Good evening",
        internalId = "evening",
        description = "17:00 – 21:00 — warm, settling.",
    ),

    NIGHT(
        displayText = "Late night",
        internalId = "night",
        description = "21:00 – 5:00 — quiet, intimate.",
    ),

    NEW_USER(
        displayText = "Welcome to RIKO",
        internalId = "new_user",
        description = "First session — onboarding tone.",
    ),

    RETURNING_USER(
        displayText = "Welcome back",
        internalId = "returning_user",
        description = "Returning after 7+ days absence.",
    ),

    EMPTY_LIBRARY(
        displayText = "Your Music World",
        internalId = "empty_library",
        description = "Library has no songs yet — Rule #30.",
    ),

    SCANNING(
        displayText = "Building your library",
        internalId = "scanning",
        description = "Media scan in progress.",
    ),

    ERROR(
        displayText = "Let's try again",
        internalId = "error",
        description = "Last operation failed — recovery prompt.",
    ),
}

/**
 * The subtitle shown beneath the greeting. Drives the emotional
 * tone of the Library at this moment.
 */
enum class HeaderSubtitle(
    val displayText: String,
    val internalId: String,
    val description: String,
) {
    DEFAULT(
        displayText = "Your music, your world",
        internalId = "default",
        description = "Generic subtitle — used when no specific context applies.",
    ),

    CONTINUE_SESSION(
        displayText = "Pick up where you left off",
        internalId = "continue_session",
        description = "Last session was recent — invite to resume.",
    ),

    NEW_MUSIC(
        displayText = "You have new music to explore",
        internalId = "new_music",
        description = "Recently Added has new items since last session.",
    ),

    QUIET_TIME(
        displayText = "Winding down for the night",
        internalId = "quiet_time",
        description = "Late night — gentle invitation.",
    ),

    FRESH_START(
        displayText = "A fresh day, a fresh sound",
        internalId = "fresh_start",
        description = "Early morning — energetic invitation.",
    ),

    EMPTY_LIBRARY(
        displayText = "Start building your collection",
        internalId = "empty_library",
        description = "Library has no songs — Rule #30.",
    ),

    SCANNING(
        displayText = "Finding tracks on your device",
        internalId = "scanning",
        description = "Scan in progress — informative.",
    ),

    ERROR(
        displayText = "Something went wrong. Tap to retry.",
        internalId = "error",
        description = "Recovery prompt.",
    ),

    PERMISSION_REQUIRED(
        displayText = "RIKO needs access to your music",
        internalId = "permission_required",
        description = "Permission gate.",
    ),
}

/**
 * Optional context line — appears below the subtitle, smaller and
 * more muted. Used for context like weather, time, or recent activity.
 */
enum class HeaderContextLine(
    val displayText: String,
    val internalId: String,
    val description: String,
) {
    NONE(
        displayText = "",
        internalId = "none",
        description = "No context line — subtitle alone is enough.",
    ),

    LIBRARY_SIZE(
        displayText = "1,247 tracks across 89 albums",
        internalId = "library_size",
        description = "Static-ish library stats. Runtime replaces the numbers.",
    ),

    LAST_PLAYED(
        displayText = "Last played: Track Name — 2h ago",
        internalId = "last_played",
        description = "Recently played context. Runtime fills in track + time.",
    ),

    WEATHER(
        displayText = "Rainy evening — perfect for soft sounds",
        internalId = "weather",
        description = "Weather context (future).",
    ),

    TIME_OF_DAY(
        displayText = "It's late — go easy on yourself",
        internalId = "time_of_day",
        description = "Time-aware context line.",
    ),

    SESSION_COUNT(
        displayText = "This is your 142nd session",
        internalId = "session_count",
        description = "Engagement counter — used sparingly.",
    ),
}

/**
 * Small status badge shown at the top-right of the header.
 */
enum class HeaderBadge(internal val description: String) {
    NONE("No badge."),
    SCANNING("Pulsing dot — scan in progress."),
    NEW_MUSIC("'New' chip — recently added since last session."),
    OFFLINE("Cloud-off icon — device is offline."),
    SYNCING("Spinner — sync in progress."),
    ERROR("Warning icon — last operation failed."),
}

/**
 * Actionable chip rendered at the top-right or below the subtitle.
 */
enum class HeaderChip(
    val displayText: String,
    val internalId: String,
    val description: String,
) {
    NONE(
        displayText = "",
        internalId = "none",
        description = "No chip.",
    ),

    SHUFFLE_ALL(
        displayText = "Shuffle All",
        internalId = "shuffle_all",
        description = "Shuffle the entire library.",
    ),

    SEARCH(
        displayText = "Search",
        internalId = "search",
        description = "Jump to search.",
    ),

    REFRESH(
        displayText = "Refresh",
        internalId = "refresh",
        description = "Re-scan the library.",
    ),

    SORT(
        displayText = "Sort",
        internalId = "sort",
        description = "Open the sort menu.",
    ),

    GRANT_PERMISSION(
        displayText = "Grant Access",
        internalId = "grant_permission",
        description = "Request media access permission.",
    ),

    RETRY(
        displayText = "Retry",
        internalId = "retry",
        description = "Retry the failed operation.",
    ),
}

/**
 * How the header's ambient wave renders. Rule #28 — Library wave
 * is always capped, but the header can have its own subtle wave.
 */
enum class HeaderWaveMode(
    internal val description: String,
    internal val opacity: Float,
    internal val thickness: Float,
) {
    OFF("No wave in header.", 0.0f, 0.0f),
    WHISPER("Barely-there wave — late night / minimal state.", 0.10f, 0.3f),
    SUBTLE("Subtle wave — default.", 0.20f, 0.4f),
    PRESENT("Present wave — energetic state.", 0.30f, 0.5f),
}

/**
 * What the header does when the user scrolls the Library.
 */
enum class HeaderCollapseBehaviour(internal val description: String) {
    STICKY("Header pins to top — always visible."),
    COLLAPSE_TO_GREETING("Header collapses to just the greeting line."),
    COLLAPSE_TO_CHIP("Header collapses to just the action chip."),
    FADE_AND_PIN("Header fades to 50% opacity and pins."),
    SCROLL_AWAY("Header scrolls away with the content."),
}

/**
 * Entry / exit animation profile for the header.
 */
enum class HeaderAnimationStyle(
    internal val damping: Float,
    internal val durationMs: Int,
    internal val description: String,
) {
    NONE(1.0f, 0, "No animation."),
    GENTLE_FADE(0.8f, 240, "Gentle fade-in."),
    SLIDE_AND_FADE(0.7f, 320, "Slide from top + fade."),
    EXPAND_REVEAL(0.6f, 420, "Expand from greeting line — hero feel."),
    DRIFT_IN(0.5f, 540, "Dreamy drift-in — late night / mood."),
}

/**
 * Glass tint strength behind the header.
 */
enum class HeaderGlassStrength(internal val weight: Float) {
    NONE(0.0f),
    LIGHT(0.3f),
    MEDIUM(0.5f),
    STRONG(0.7f),
    HEAVY(0.9f),
}

/**
 * The complete header contract — the Library Header's identity
 * at a given moment.
 *
 * The runtime (B2) holds a [CollectionHeaderContract] in LibraryState
 * and updates it as context changes. The UI (B4) renders whatever
 * the contract says.
 */
data class CollectionHeaderContract(
    val greeting: HeaderGreeting,
    val subtitle: HeaderSubtitle,
    val contextLine: HeaderContextLine,
    val badge: HeaderBadge,
    val chip: HeaderChip,
    val waveMode: HeaderWaveMode,
    val collapseBehaviour: HeaderCollapseBehaviour,
    val animationStyle: HeaderAnimationStyle,
    val glassStrength: HeaderGlassStrength,
) {

    companion object {

        /** Default cold-start header. */
        val DEFAULT = CollectionHeaderContract(
            greeting = HeaderGreeting.AFTERNOON,
            subtitle = HeaderSubtitle.DEFAULT,
            contextLine = HeaderContextLine.NONE,
            badge = HeaderBadge.NONE,
            chip = HeaderChip.SEARCH,
            waveMode = HeaderWaveMode.SUBTLE,
            collapseBehaviour = HeaderCollapseBehaviour.COLLAPSE_TO_GREETING,
            animationStyle = HeaderAnimationStyle.GENTLE_FADE,
            glassStrength = HeaderGlassStrength.MEDIUM,
        )

        /** Header shown on first session. */
        val NEW_USER = DEFAULT.copy(
            greeting = HeaderGreeting.NEW_USER,
            subtitle = HeaderSubtitle.EMPTY_LIBRARY,
            contextLine = HeaderContextLine.NONE,
            chip = HeaderChip.GRANT_PERMISSION,
            waveMode = HeaderWaveMode.SUBTLE,
            animationStyle = HeaderAnimationStyle.EXPAND_REVEAL,
        )

        /** Header shown while scanning media. */
        val SCANNING = DEFAULT.copy(
            greeting = HeaderGreeting.SCANNING,
            subtitle = HeaderSubtitle.SCANNING,
            contextLine = HeaderContextLine.NONE,
            badge = HeaderBadge.SCANNING,
            chip = HeaderChip.NONE,
            waveMode = HeaderWaveMode.SUBTLE,
            animationStyle = HeaderAnimationStyle.GENTLE_FADE,
        )

        /** Header shown when library is empty after scan. */
        val EMPTY = DEFAULT.copy(
            greeting = HeaderGreeting.EMPTY_LIBRARY,
            subtitle = HeaderSubtitle.EMPTY_LIBRARY,
            contextLine = HeaderContextLine.NONE,
            chip = HeaderChip.SEARCH,
            waveMode = HeaderWaveMode.WHISPER,
            animationStyle = HeaderAnimationStyle.DRIFT_IN,
        )

        /** Header shown on error. */
        val ERROR = DEFAULT.copy(
            greeting = HeaderGreeting.ERROR,
            subtitle = HeaderSubtitle.ERROR,
            contextLine = HeaderContextLine.NONE,
            badge = HeaderBadge.ERROR,
            chip = HeaderChip.RETRY,
            waveMode = HeaderWaveMode.OFF,
            animationStyle = HeaderAnimationStyle.GENTLE_FADE,
        )

        /** Header shown when permission is required. */
        val PERMISSION_REQUIRED = DEFAULT.copy(
            greeting = HeaderGreeting.NEW_USER,
            subtitle = HeaderSubtitle.PERMISSION_REQUIRED,
            contextLine = HeaderContextLine.NONE,
            chip = HeaderChip.GRANT_PERMISSION,
            waveMode = HeaderWaveMode.OFF,
            animationStyle = HeaderAnimationStyle.GENTLE_FADE,
        )

        /** Late-night header — quiet, intimate. */
        val LATE_NIGHT = DEFAULT.copy(
            greeting = HeaderGreeting.NIGHT,
            subtitle = HeaderSubtitle.QUIET_TIME,
            contextLine = HeaderContextLine.TIME_OF_DAY,
            chip = HeaderChip.SHUFFLE_ALL,
            waveMode = HeaderWaveMode.WHISPER,
            animationStyle = HeaderAnimationStyle.DRIFT_IN,
            glassStrength = HeaderGlassStrength.STRONG,
        )

        /** Morning header — fresh, energetic. */
        val MORNING = DEFAULT.copy(
            greeting = HeaderGreeting.EARLY_MORNING,
            subtitle = HeaderSubtitle.FRESH_START,
            contextLine = HeaderContextLine.NONE,
            chip = HeaderChip.SHUFFLE_ALL,
            waveMode = HeaderWaveMode.PRESENT,
            animationStyle = HeaderAnimationStyle.SLIDE_AND_FADE,
            glassStrength = HeaderGlassStrength.LIGHT,
        )

        /** Returning-user header — after 7+ day absence. */
        val RETURNING = DEFAULT.copy(
            greeting = HeaderGreeting.RETURNING_USER,
            subtitle = HeaderSubtitle.CONTINUE_SESSION,
            contextLine = HeaderContextLine.LAST_PLAYED,
            chip = HeaderChip.SEARCH,
            waveMode = HeaderWaveMode.SUBTLE,
            animationStyle = HeaderAnimationStyle.EXPAND_REVEAL,
        )

        /** New-music-available header. */
        val NEW_MUSIC = DEFAULT.copy(
            greeting = HeaderGreeting.AFTERNOON,
            subtitle = HeaderSubtitle.NEW_MUSIC,
            contextLine = HeaderContextLine.NONE,
            badge = HeaderBadge.NEW_MUSIC,
            chip = HeaderChip.SEARCH,
            waveMode = HeaderWaveMode.PRESENT,
            animationStyle = HeaderAnimationStyle.SLIDE_AND_FADE,
        )
    }
}
