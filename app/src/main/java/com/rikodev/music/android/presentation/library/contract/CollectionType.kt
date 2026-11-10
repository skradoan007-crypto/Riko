/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B1 — Collection Contract Pack v2 (Final)
 *
 * File 1 / 15 — CollectionType.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Every Collection in RIKO has an *identity* before it has data.
 * This file declares all collection identities the Library will
 * ever produce — past, present, and future.
 *
 * This is NOT an enum of "what we show today". This is an enum of
 * "what RIKO is capable of thinking about". Future B2 / B3 builds
 * may generate any of these — the contract already knows them.
 *
 * RULE #36 — Contract Never Thinks
 * RULE #38 — Every Collection Can Explain Itself
 * RULE #39 — Contracts Are Forever
 *
 * Each entry declares:
 *   • displayName       — human-readable name (user-facing)
 *   • internalId        — stable string identifier (never localized)
 *   • priority          — ordering weight in the library
 *   • category          — semantic group (see CollectionCategory)
 *   • emotion           — primary emotional identity (see CollectionEmotion)
 *   • supportsPin       — user can pin to top
 *   • supportsRefresh   — engine may regenerate
 *   • supportsCollapse  — user can collapse section
 *   • supportsShuffle   — collection can be shuffled
 *   • supportsSort      — collection can be re-sorted
 *   • supportsPreview   — has a 5-second inline preview
 *   • minimumItems      — engine will not emit if fewer items
 *   • maximumItems      — engine caps to this many items
 *   • cacheable         — runtime may cache the result
 *   • persistent        — survives process death
 *   • allowDuplicates   — same song may appear multiple times
 *   • visibleByDefault  — shown on cold start (vs. surfaced later)
 *   • experimental      — behind a feature flag
 *   • developerOnly     — only visible in developer mode
 *   • futureReady       — declared today, may be enabled later
 *
 * No logic. No side effects. No Android imports. No Kotlin Coroutines.
 * Pure data.
 *
 * © RIKO Experience OS — Constitution Tier
 */

package com.rikodev.music.android.presentation.library.contract

/**
 * The complete catalogue of collection identities RIKO recognizes.
 *
 * Adding a new value here is a Constitution-level decision and
 * requires a parallel entry in `CollectionConstitution` (see
 * CollectionContract.kt). The type alone is identity; the
 * Constitution gives it personality.
 */
enum class CollectionType(
    val displayName: String,
    val internalId: String,
    val priority: CollectionPriority,
    val category: CollectionCategory,
    val emotion: CollectionEmotion,
    val supportsPin: Boolean,
    val supportsRefresh: Boolean,
    val supportsCollapse: Boolean,
    val supportsShuffle: Boolean,
    val supportsSort: Boolean,
    val supportsPreview: Boolean,
    val minimumItems: Int,
    val maximumItems: Int,
    val cacheable: Boolean,
    val persistent: Boolean,
    val allowDuplicates: Boolean,
    val visibleByDefault: Boolean,
    val experimental: Boolean,
    val developerOnly: Boolean,
    val futureReady: Boolean,
) {

    // ─────────────────────────────────────────────────────────────
    // LISTENING CONTINUITY — the "pick up where you left off" family
    // ─────────────────────────────────────────────────────────────

    /**
     * The single most important collection on cold start. Surfaces
     * the 5–10 tracks the user was most recently immersed in.
     * Engine: ContinueStrategy (B3).
     */
    RECENTLY_PLAYED(
        displayName = "Recently Played",
        internalId = "recently_played",
        priority = CollectionPriority.PRIMARY,
        category = CollectionCategory.LISTENING,
        emotion = CollectionEmotion.WARM,
        supportsPin = true,
        supportsRefresh = true,
        supportsCollapse = true,
        supportsShuffle = false,
        supportsSort = true,
        supportsPreview = true,
        minimumItems = 1,
        maximumItems = 50,
        cacheable = true,
        persistent = true,
        allowDuplicates = false,
        visibleByDefault = true,
        experimental = false,
        developerOnly = false,
        futureReady = true,
    ),

    /**
     * A short, opinionated strip of 3–6 tracks the user started but
     * did not finish. The emotional message: "we noticed you left
     * something — want to continue?"
     */
    CONTINUE_LISTENING(
        displayName = "Continue Listening",
        internalId = "continue_listening",
        priority = CollectionPriority.PRIMARY,
        category = CollectionCategory.LISTENING,
        emotion = CollectionEmotion.GENTLE,
        supportsPin = true,
        supportsRefresh = true,
        supportsCollapse = true,
        supportsShuffle = false,
        supportsSort = false,
        supportsPreview = true,
        minimumItems = 1,
        maximumItems = 10,
        cacheable = true,
        persistent = true,
        allowDuplicates = false,
        visibleByDefault = true,
        experimental = false,
        developerOnly = false,
        futureReady = true,
    ),

    /**
     * Tracks the user finished in the last 72 hours. Subtly says:
     * "you completed these — that means something."
     */
    RECENTLY_FINISHED(
        displayName = "Recently Finished",
        internalId = "recently_finished",
        priority = CollectionPriority.SECONDARY,
        category = CollectionCategory.LISTENING,
        emotion = CollectionEmotion.WARM,
        supportsPin = false,
        supportsRefresh = true,
        supportsCollapse = true,
        supportsShuffle = true,
        supportsSort = true,
        supportsPreview = true,
        minimumItems = 3,
        maximumItems = 25,
        cacheable = true,
        persistent = true,
        allowDuplicates = false,
        visibleByDefault = false,
        experimental = false,
        developerOnly = false,
        futureReady = true,
    ),

    // ─────────────────────────────────────────────────────────────
    // PLAY FREQUENCY — what your library actually loves
    // ─────────────────────────────────────────────────────────────

    MOST_PLAYED(
        displayName = "Most Played",
        internalId = "most_played",
        priority = CollectionPriority.PRIMARY,
        category = CollectionCategory.LISTENING,
        emotion = CollectionEmotion.POWERFUL,
        supportsPin = true,
        supportsRefresh = true,
        supportsCollapse = true,
        supportsShuffle = true,
        supportsSort = true,
        supportsPreview = true,
        minimumItems = 5,
        maximumItems = 100,
        cacheable = true,
        persistent = true,
        allowDuplicates = false,
        visibleByDefault = true,
        experimental = false,
        developerOnly = false,
        futureReady = true,
    ),

    LEAST_PLAYED(
        displayName = "Least Played",
        internalId = "least_played",
        priority = CollectionPriority.SECONDARY,
        category = CollectionCategory.DISCOVERY,
        emotion = CollectionEmotion.REFLECTIVE,
        supportsPin = false,
        supportsRefresh = true,
        supportsCollapse = true,
        supportsShuffle = true,
        supportsSort = true,
        supportsPreview = true,
        minimumItems = 5,
        maximumItems = 50,
        cacheable = true,
        persistent = true,
        allowDuplicates = false,
        visibleByDefault = false,
        experimental = false,
        developerOnly = false,
        futureReady = true,
    ),

    NEVER_PLAYED(
        displayName = "Never Played",
        internalId = "never_played",
        priority = CollectionPriority.SECONDARY,
        category = CollectionCategory.DISCOVERY,
        emotion = CollectionEmotion.FLOATING,
        supportsPin = false,
        supportsRefresh = true,
        supportsCollapse = true,
        supportsShuffle = true,
        supportsSort = true,
        supportsPreview = true,
        minimumItems = 5,
        maximumItems = 100,
        cacheable = true,
        persistent = true,
        allowDuplicates = false,
        visibleByDefault = false,
        experimental = false,
        developerOnly = false,
        futureReady = true,
    ),

    // ─────────────────────────────────────────────────────────────
    // LIBRARY INTAKE — fresh imports
    // ─────────────────────────────────────────────────────────────

    RECENTLY_ADDED(
        displayName = "Recently Added",
        internalId = "recently_added",
        priority = CollectionPriority.PRIMARY,
        category = CollectionCategory.LIBRARY,
        emotion = CollectionEmotion.BRIGHT,
        supportsPin = true,
        supportsRefresh = true,
        supportsCollapse = true,
        supportsShuffle = true,
        supportsSort = true,
        supportsPreview = true,
        minimumItems = 1,
        maximumItems = 50,
        cacheable = true,
        persistent = true,
        allowDuplicates = false,
        visibleByDefault = true,
        experimental = false,
        developerOnly = false,
        futureReady = true,
    ),

    // ─────────────────────────────────────────────────────────────
    // HIDDEN GEMS — the soul of RIKO's Library
    // ─────────────────────────────────────────────────────────────

    /**
     * Songs the user rarely plays but usually finishes when they do.
     * RIKO's quiet love letter to the user.
     */
    HIDDEN_GEMS(
        displayName = "Hidden Gems",
        internalId = "hidden_gems",
        priority = CollectionPriority.PRIMARY,
        category = CollectionCategory.DISCOVERY,
        emotion = CollectionEmotion.DREAMY,
        supportsPin = true,
        supportsRefresh = true,
        supportsCollapse = true,
        supportsShuffle = true,
        supportsSort = false,
        supportsPreview = true,
        minimumItems = 3,
        maximumItems = 30,
        cacheable = true,
        persistent = true,
        allowDuplicates = false,
        visibleByDefault = true,
        experimental = false,
        developerOnly = false,
        futureReady = true,
    ),

    /**
     * Tracks the user once loved but hasn't touched in 90+ days.
     * Engine: ForgottenStrategy (B3). Message: "remember this?"
     */
    FORGOTTEN_CLASSICS(
        displayName = "Forgotten Classics",
        internalId = "forgotten_classics",
        priority = CollectionPriority.SECONDARY,
        category = CollectionCategory.DISCOVERY,
        emotion = CollectionEmotion.REFLECTIVE,
        supportsPin = false,
        supportsRefresh = true,
        supportsCollapse = true,
        supportsShuffle = true,
        supportsSort = false,
        supportsPreview = true,
        minimumItems = 3,
        maximumItems = 25,
        cacheable = true,
        persistent = true,
        allowDuplicates = false,
        visibleByDefault = true,
        experimental = false,
        developerOnly = false,
        futureReady = true,
    ),

    // ─────────────────────────────────────────────────────────────
    // EMOTIONAL TIME-OF-DAY COLLECTIONS
    // ─────────────────────────────────────────────────────────────

    NIGHT_CALM(
        displayName = "Night Calm",
        internalId = "night_calm",
        priority = CollectionPriority.PRIMARY,
        category = CollectionCategory.MOOD,
        emotion = CollectionEmotion.NIGHT,
        supportsPin = true,
        supportsRefresh = true,
        supportsCollapse = true,
        supportsShuffle = true,
        supportsSort = false,
        supportsPreview = true,
        minimumItems = 5,
        maximumItems = 40,
        cacheable = true,
        persistent = true,
        allowDuplicates = false,
        visibleByDefault = true,
        experimental = false,
        developerOnly = false,
        futureReady = true,
    ),

    MORNING_ENERGY(
        displayName = "Morning Energy",
        internalId = "morning_energy",
        priority = CollectionPriority.PRIMARY,
        category = CollectionCategory.MOOD,
        emotion = CollectionEmotion.MORNING,
        supportsPin = true,
        supportsRefresh = true,
        supportsCollapse = true,
        supportsShuffle = true,
        supportsSort = false,
        supportsPreview = true,
        minimumItems = 5,
        maximumItems = 40,
        cacheable = true,
        persistent = true,
        allowDuplicates = false,
        visibleByDefault = true,
        experimental = false,
        developerOnly = false,
        futureReady = true,
    ),

    EVENING_UNWIND(
        displayName = "Evening Unwind",
        internalId = "evening_unwind",
        priority = CollectionPriority.SECONDARY,
        category = CollectionCategory.MOOD,
        emotion = CollectionEmotion.SUNSET,
        supportsPin = true,
        supportsRefresh = true,
        supportsCollapse = true,
        supportsShuffle = true,
        supportsSort = false,
        supportsPreview = true,
        minimumItems = 5,
        maximumItems = 40,
        cacheable = true,
        persistent = true,
        allowDuplicates = false,
        visibleByDefault = true,
        experimental = false,
        developerOnly = false,
        futureReady = true,
    ),

    COMMUTE_MUSIC(
        displayName = "Commute",
        internalId = "commute_music",
        priority = CollectionPriority.SECONDARY,
        category = CollectionCategory.MOOD,
        emotion = CollectionEmotion.FLOATING,
        supportsPin = true,
        supportsRefresh = true,
        supportsCollapse = true,
        supportsShuffle = true,
        supportsSort = false,
        supportsPreview = true,
        minimumItems = 5,
        maximumItems = 30,
        cacheable = true,
        persistent = true,
        allowDuplicates = false,
        visibleByDefault = false,
        experimental = false,
        developerOnly = false,
        futureReady = true,
    ),

    STUDY_FOCUS(
        displayName = "Study Focus",
        internalId = "study_focus",
        priority = CollectionPriority.SECONDARY,
        category = CollectionCategory.MOOD,
        emotion = CollectionEmotion.FOCUSED,
        supportsPin = true,
        supportsRefresh = true,
        supportsCollapse = true,
        supportsShuffle = true,
        supportsSort = false,
        supportsPreview = true,
        minimumItems = 5,
        maximumItems = 50,
        cacheable = true,
        persistent = true,
        allowDuplicates = false,
        visibleByDefault = false,
        experimental = false,
        developerOnly = false,
        futureReady = true,
    ),

    SLEEP_DRIFT(
        displayName = "Sleep Drift",
        internalId = "sleep_drift",
        priority = CollectionPriority.OPTIONAL,
        category = CollectionCategory.MOOD,
        emotion = CollectionEmotion.DREAMY,
        supportsPin = true,
        supportsRefresh = true,
        supportsCollapse = true,
        supportsShuffle = false,
        supportsSort = false,
        supportsPreview = true,
        minimumItems = 5,
        maximumItems = 30,
        cacheable = true,
        persistent = true,
        allowDuplicates = false,
        visibleByDefault = false,
        experimental = false,
        developerOnly = false,
        futureReady = true,
    ),

    WORKOUT_PUMP(
        displayName = "Workout Pump",
        internalId = "workout_pump",
        priority = CollectionPriority.OPTIONAL,
        category = CollectionCategory.MOOD,
        emotion = CollectionEmotion.POWERFUL,
        supportsPin = true,
        supportsRefresh = true,
        supportsCollapse = true,
        supportsShuffle = true,
        supportsSort = false,
        supportsPreview = true,
        minimumItems = 5,
        maximumItems = 40,
        cacheable = true,
        persistent = true,
        allowDuplicates = false,
        visibleByDefault = false,
        experimental = false,
        developerOnly = false,
        futureReady = true,
    ),

    // ─────────────────────────────────────────────────────────────
    // EMOTIONAL DEEP CUTS — feeling-led collections
    // ─────────────────────────────────────────────────────────────

    ROMANTIC_MOOD(
        displayName = "Romantic",
        internalId = "romantic_mood",
        priority = CollectionPriority.OPTIONAL,
        category = CollectionCategory.EMOTION,
        emotion = CollectionEmotion.WARM,
        supportsPin = true,
        supportsRefresh = true,
        supportsCollapse = true,
        supportsShuffle = true,
        supportsSort = false,
        supportsPreview = true,
        minimumItems = 5,
        maximumItems = 40,
        cacheable = true,
        persistent = true,
        allowDuplicates = false,
        visibleByDefault = false,
        experimental = false,
        developerOnly = false,
        futureReady = true,
    ),

    MELANCHOLY_JOURNEY(
        displayName = "Melancholy",
        internalId = "melancholy_journey",
        priority = CollectionPriority.OPTIONAL,
        category = CollectionCategory.EMOTION,
        emotion = CollectionEmotion.REFLECTIVE,
        supportsPin = true,
        supportsRefresh = true,
        supportsCollapse = true,
        supportsShuffle = true,
        supportsSort = false,
        supportsPreview = true,
        minimumItems = 5,
        maximumItems = 40,
        cacheable = true,
        persistent = true,
        allowDuplicates = false,
        visibleByDefault = false,
        experimental = false,
        developerOnly = false,
        futureReady = true,
    ),

    INSTRUMENTAL_ESCAPE(
        displayName = "Instrumental Escape",
        internalId = "instrumental_escape",
        priority = CollectionPriority.OPTIONAL,
        category = CollectionCategory.EMOTION,
        emotion = CollectionEmotion.MINIMAL,
        supportsPin = true,
        supportsRefresh = true,
        supportsCollapse = true,
        supportsShuffle = true,
        supportsSort = false,
        supportsPreview = true,
        minimumItems = 3,
        maximumItems = 40,
        cacheable = true,
        persistent = true,
        allowDuplicates = false,
        visibleByDefault = false,
        experimental = false,
        developerOnly = false,
        futureReady = true,
    ),

    VOCAL_ONLY(
        displayName = "Vocals Only",
        internalId = "vocal_only",
        priority = CollectionPriority.OPTIONAL,
        category = CollectionCategory.EMOTION,
        emotion = CollectionEmotion.WARM,
        supportsPin = true,
        supportsRefresh = true,
        supportsCollapse = true,
        supportsShuffle = true,
        supportsSort = false,
        supportsPreview = true,
        minimumItems = 3,
        maximumItems = 40,
        cacheable = true,
        persistent = true,
        allowDuplicates = false,
        visibleByDefault = false,
        experimental = false,
        developerOnly = false,
        futureReady = true,
    ),

    // ─────────────────────────────────────────────────────────────
    // DISCOVERY — engine-curated exploration
    // ─────────────────────────────────────────────────────────────

    DISCOVERY(
        displayName = "Discovery",
        internalId = "discovery",
        priority = CollectionPriority.PRIMARY,
        category = CollectionCategory.DISCOVERY,
        emotion = CollectionEmotion.FLOATING,
        supportsPin = true,
        supportsRefresh = true,
        supportsCollapse = true,
        supportsShuffle = true,
        supportsSort = false,
        supportsPreview = true,
        minimumItems = 5,
        maximumItems = 30,
        cacheable = true,
        persistent = true,
        allowDuplicates = false,
        visibleByDefault = true,
        experimental = false,
        developerOnly = false,
        futureReady = true,
    ),

    SIMILAR_TO(
        displayName = "Similar To",
        internalId = "similar_to",
        priority = CollectionPriority.SECONDARY,
        category = CollectionCategory.DISCOVERY,
        emotion = CollectionEmotion.FLOATING,
        supportsPin = false,
        supportsRefresh = true,
        supportsCollapse = true,
        supportsShuffle = true,
        supportsSort = false,
        supportsPreview = true,
        minimumItems = 3,
        maximumItems = 20,
        cacheable = true,
        persistent = false,
        allowDuplicates = false,
        visibleByDefault = false,
        experimental = false,
        developerOnly = false,
        futureReady = true,
    ),

    SURPRISE_ME(
        displayName = "Surprise Me",
        internalId = "surprise_me",
        priority = CollectionPriority.SECONDARY,
        category = CollectionCategory.DISCOVERY,
        emotion = CollectionEmotion.ALIVE,
        supportsPin = false,
        supportsRefresh = true,
        supportsCollapse = true,
        supportsShuffle = true,
        supportsSort = false,
        supportsPreview = true,
        minimumItems = 5,
        maximumItems = 25,
        cacheable = false,
        persistent = false,
        allowDuplicates = true,
        visibleByDefault = false,
        experimental = false,
        developerOnly = false,
        futureReady = true,
    ),

    // ─────────────────────────────────────────────────────────────
    // CONTEXT-AWARE (Future-ready — declared now, enabled later)
    // ─────────────────────────────────────────────────────────────

    MOOD_BASED(
        displayName = "Mood",
        internalId = "mood_based",
        priority = CollectionPriority.SECONDARY,
        category = CollectionCategory.SMART,
        emotion = CollectionEmotion.ADAPTIVE,
        supportsPin = true,
        supportsRefresh = true,
        supportsCollapse = true,
        supportsShuffle = true,
        supportsSort = false,
        supportsPreview = true,
        minimumItems = 5,
        maximumItems = 40,
        cacheable = true,
        persistent = true,
        allowDuplicates = false,
        visibleByDefault = false,
        experimental = true,
        developerOnly = false,
        futureReady = true,
    ),

    WEATHER_BASED(
        displayName = "Weather",
        internalId = "weather_based",
        priority = CollectionPriority.OPTIONAL,
        category = CollectionCategory.SMART,
        emotion = CollectionEmotion.ADAPTIVE,
        supportsPin = false,
        supportsRefresh = true,
        supportsCollapse = true,
        supportsShuffle = true,
        supportsSort = false,
        supportsPreview = true,
        minimumItems = 5,
        maximumItems = 30,
        cacheable = true,
        persistent = false,
        allowDuplicates = false,
        visibleByDefault = false,
        experimental = true,
        developerOnly = false,
        futureReady = true,
    ),

    TIME_BASED(
        displayName = "Time of Day",
        internalId = "time_based",
        priority = CollectionPriority.OPTIONAL,
        category = CollectionCategory.SMART,
        emotion = CollectionEmotion.ADAPTIVE,
        supportsPin = false,
        supportsRefresh = true,
        supportsCollapse = true,
        supportsShuffle = true,
        supportsSort = false,
        supportsPreview = true,
        minimumItems = 5,
        maximumItems = 30,
        cacheable = true,
        persistent = false,
        allowDuplicates = false,
        visibleByDefault = false,
        experimental = true,
        developerOnly = false,
        futureReady = true,
    ),

    // ─────────────────────────────────────────────────────────────
    // USER-OWNED — explicit user actions
    // ─────────────────────────────────────────────────────────────

    FAVORITES(
        displayName = "Favorites",
        internalId = "favorites",
        priority = CollectionPriority.PRIMARY,
        category = CollectionCategory.LIBRARY,
        emotion = CollectionEmotion.WARM,
        supportsPin = true,
        supportsRefresh = false,
        supportsCollapse = true,
        supportsShuffle = true,
        supportsSort = true,
        supportsPreview = true,
        minimumItems = 1,
        maximumItems = 1000,
        cacheable = true,
        persistent = true,
        allowDuplicates = false,
        visibleByDefault = true,
        experimental = false,
        developerOnly = false,
        futureReady = true,
    ),

    PINNED(
        displayName = "Pinned",
        internalId = "pinned",
        priority = CollectionPriority.PRIMARY,
        category = CollectionCategory.LIBRARY,
        emotion = CollectionEmotion.ORGANIC,
        supportsPin = false,
        supportsRefresh = false,
        supportsCollapse = false,
        supportsShuffle = true,
        supportsSort = true,
        supportsPreview = true,
        minimumItems = 1,
        maximumItems = 50,
        cacheable = true,
        persistent = true,
        allowDuplicates = false,
        visibleByDefault = true,
        experimental = false,
        developerOnly = false,
        futureReady = true,
    ),

    PLAYLIST_USER(
        displayName = "Your Playlists",
        internalId = "playlist_user",
        priority = CollectionPriority.PRIMARY,
        category = CollectionCategory.PLAYLIST,
        emotion = CollectionEmotion.ORGANIC,
        supportsPin = true,
        supportsRefresh = false,
        supportsCollapse = true,
        supportsShuffle = true,
        supportsSort = true,
        supportsPreview = true,
        minimumItems = 1,
        maximumItems = 200,
        cacheable = true,
        persistent = true,
        allowDuplicates = false,
        visibleByDefault = true,
        experimental = false,
        developerOnly = false,
        futureReady = true,
    ),

    PLAYLIST_AUTO(
        displayName = "Smart Playlists",
        internalId = "playlist_auto",
        priority = CollectionPriority.SECONDARY,
        category = CollectionCategory.PLAYLIST,
        emotion = CollectionEmotion.MINIMAL,
        supportsPin = false,
        supportsRefresh = true,
        supportsCollapse = true,
        supportsShuffle = true,
        supportsSort = true,
        supportsPreview = true,
        minimumItems = 1,
        maximumItems = 50,
        cacheable = true,
        persistent = true,
        allowDuplicates = false,
        visibleByDefault = false,
        experimental = false,
        developerOnly = false,
        futureReady = true,
    ),

    // ─────────────────────────────────────────────────────────────
    // STRUCTURAL — Library organization views
    // ─────────────────────────────────────────────────────────────

    ALL_SONGS(
        displayName = "All Songs",
        internalId = "all_songs",
        priority = CollectionPriority.PRIMARY,
        category = CollectionCategory.LIBRARY,
        emotion = CollectionEmotion.MINIMAL,
        supportsPin = false,
        supportsRefresh = false,
        supportsCollapse = false,
        supportsShuffle = true,
        supportsSort = true,
        supportsPreview = false,
        minimumItems = 1,
        maximumItems = Int.MAX_VALUE,
        cacheable = true,
        persistent = true,
        allowDuplicates = false,
        visibleByDefault = true,
        experimental = false,
        developerOnly = false,
        futureReady = true,
    ),

    ALL_ALBUMS(
        displayName = "Albums",
        internalId = "all_albums",
        priority = CollectionPriority.PRIMARY,
        category = CollectionCategory.ALBUM,
        emotion = CollectionEmotion.MINIMAL,
        supportsPin = false,
        supportsRefresh = false,
        supportsCollapse = false,
        supportsShuffle = false,
        supportsSort = true,
        supportsPreview = false,
        minimumItems = 1,
        maximumItems = Int.MAX_VALUE,
        cacheable = true,
        persistent = true,
        allowDuplicates = false,
        visibleByDefault = true,
        experimental = false,
        developerOnly = false,
        futureReady = true,
    ),

    ALL_ARTISTS(
        displayName = "Artists",
        internalId = "all_artists",
        priority = CollectionPriority.PRIMARY,
        category = CollectionCategory.ARTIST,
        emotion = CollectionEmotion.MINIMAL,
        supportsPin = false,
        supportsRefresh = false,
        supportsCollapse = false,
        supportsShuffle = false,
        supportsSort = true,
        supportsPreview = false,
        minimumItems = 1,
        maximumItems = Int.MAX_VALUE,
        cacheable = true,
        persistent = true,
        allowDuplicates = false,
        visibleByDefault = true,
        experimental = false,
        developerOnly = false,
        futureReady = true,
    ),

    ALL_FOLDERS(
        displayName = "Folders",
        internalId = "all_folders",
        priority = CollectionPriority.SECONDARY,
        category = CollectionCategory.FOLDER,
        emotion = CollectionEmotion.MINIMAL,
        supportsPin = false,
        supportsRefresh = false,
        supportsCollapse = false,
        supportsShuffle = false,
        supportsSort = true,
        supportsPreview = false,
        minimumItems = 1,
        maximumItems = Int.MAX_VALUE,
        cacheable = true,
        persistent = true,
        allowDuplicates = false,
        visibleByDefault = true,
        experimental = false,
        developerOnly = false,
        futureReady = true,
    ),

    // ─────────────────────────────────────────────────────────────
    // DEEP DIVE — exploration by metadata
    // ─────────────────────────────────────────────────────────────

    ARTIST_SPOTLIGHT(
        displayName = "Artist Spotlight",
        internalId = "artist_spotlight",
        priority = CollectionPriority.SECONDARY,
        category = CollectionCategory.ARTIST,
        emotion = CollectionEmotion.WARM,
        supportsPin = true,
        supportsRefresh = true,
        supportsCollapse = true,
        supportsShuffle = true,
        supportsSort = false,
        supportsPreview = true,
        minimumItems = 5,
        maximumItems = 30,
        cacheable = true,
        persistent = false,
        allowDuplicates = true,
        visibleByDefault = false,
        experimental = false,
        developerOnly = false,
        futureReady = true,
    ),

    GENRE_DEEP_DIVE(
        displayName = "Genre Deep Dive",
        internalId = "genre_deep_dive",
        priority = CollectionPriority.OPTIONAL,
        category = CollectionCategory.DISCOVERY,
        emotion = CollectionEmotion.EXPLORER,
        supportsPin = false,
        supportsRefresh = true,
        supportsCollapse = true,
        supportsShuffle = true,
        supportsSort = false,
        supportsPreview = true,
        minimumItems = 5,
        maximumItems = 50,
        cacheable = true,
        persistent = false,
        allowDuplicates = false,
        visibleByDefault = false,
        experimental = true,
        developerOnly = false,
        futureReady = true,
    ),

    DECADE_MIX(
        displayName = "Decade Mix",
        internalId = "decade_mix",
        priority = CollectionPriority.OPTIONAL,
        category = CollectionCategory.DISCOVERY,
        emotion = CollectionEmotion.REFLECTIVE,
        supportsPin = false,
        supportsRefresh = true,
        supportsCollapse = true,
        supportsShuffle = true,
        supportsSort = false,
        supportsPreview = true,
        minimumItems = 5,
        maximumItems = 50,
        cacheable = true,
        persistent = false,
        allowDuplicates = false,
        visibleByDefault = false,
        experimental = true,
        developerOnly = false,
        futureReady = true,
    ),

    TIME_TRAVEL(
        displayName = "Time Travel",
        internalId = "time_travel",
        priority = CollectionPriority.OPTIONAL,
        category = CollectionCategory.HISTORY,
        emotion = CollectionEmotion.REFLECTIVE,
        supportsPin = false,
        supportsRefresh = true,
        supportsCollapse = true,
        supportsShuffle = true,
        supportsSort = true,
        supportsPreview = true,
        minimumItems = 5,
        maximumItems = 50,
        cacheable = true,
        persistent = true,
        allowDuplicates = false,
        visibleByDefault = false,
        experimental = true,
        developerOnly = false,
        futureReady = true,
    ),

    HISTORY_FULL(
        displayName = "Full History",
        internalId = "history_full",
        priority = CollectionPriority.BACKGROUND,
        category = CollectionCategory.HISTORY,
        emotion = CollectionEmotion.MINIMAL,
        supportsPin = false,
        supportsRefresh = true,
        supportsCollapse = true,
        supportsShuffle = false,
        supportsSort = true,
        supportsPreview = false,
        minimumItems = 1,
        maximumItems = 5000,
        cacheable = true,
        persistent = true,
        allowDuplicates = true,
        visibleByDefault = false,
        experimental = false,
        developerOnly = false,
        futureReady = true,
    ),

    // ─────────────────────────────────────────────────────────────
    // EXPERIMENTAL / FUTURE — declared now, not yet enabled
    // ─────────────────────────────────────────────────────────────

    RIKO_RADIO(
        displayName = "RIKO Radio",
        internalId = "riko_radio",
        priority = CollectionPriority.SECONDARY,
        category = CollectionCategory.SMART,
        emotion = CollectionEmotion.ALIVE,
        supportsPin = false,
        supportsRefresh = true,
        supportsCollapse = true,
        supportsShuffle = false,
        supportsSort = false,
        supportsPreview = true,
        minimumItems = 10,
        maximumItems = 100,
        cacheable = false,
        persistent = false,
        allowDuplicates = true,
        visibleByDefault = false,
        experimental = true,
        developerOnly = false,
        futureReady = true,
    ),

    ON_THIS_DAY(
        displayName = "On This Day",
        internalId = "on_this_day",
        priority = CollectionPriority.OPTIONAL,
        category = CollectionCategory.HISTORY,
        emotion = CollectionEmotion.REFLECTIVE,
        supportsPin = false,
        supportsRefresh = true,
        supportsCollapse = true,
        supportsShuffle = true,
        supportsSort = false,
        supportsPreview = true,
        minimumItems = 3,
        maximumItems = 30,
        cacheable = true,
        persistent = true,
        allowDuplicates = false,
        visibleByDefault = false,
        experimental = true,
        developerOnly = false,
        futureReady = true,
    ),

    /**
     * Diagnostic-only collection. Never shown in production UI.
     * Surfaces under-engineered songs (missing art, missing lyrics,
     * corrupt waveforms) so the developer can investigate.
     */
    DIAGNOSTIC_ORPHANS(
        displayName = "Orphan Tracks",
        internalId = "diagnostic_orphans",
        priority = CollectionPriority.HIDDEN,
        category = CollectionCategory.EXPERIMENTAL,
        emotion = CollectionEmotion.MINIMAL,
        supportsPin = false,
        supportsRefresh = true,
        supportsCollapse = false,
        supportsShuffle = false,
        supportsSort = true,
        supportsPreview = false,
        minimumItems = 1,
        maximumItems = Int.MAX_VALUE,
        cacheable = false,
        persistent = false,
        allowDuplicates = false,
        visibleByDefault = false,
        experimental = false,
        developerOnly = true,
        futureReady = true,
    ),
    ;

    /**
     * Stable identity for cross-process references, deep links,
     * analytics, and persistence. NEVER localize this string.
     */
    val stableKey: String get() = internalId

    /**
     * Convenience flag — true if this collection is meant to be a
     * "top-of-library" surface on cold start.
     */
    val isHeroCandidate: Boolean
        get() = priority == CollectionPriority.PRIMARY && visibleByDefault && !developerOnly

    /**
     * Convenience flag — true if the user is allowed to take any
     * destructive or organizational action on this collection.
     */
    val isUserMutable: Boolean
        get() = supportsPin || supportsRefresh || supportsCollapse || supportsSort

    /**
     * Convenience flag — true if this collection is part of the
     * public contract but currently behind a flag.
     */
    val isLocked: Boolean
        get() = experimental || developerOnly
}
