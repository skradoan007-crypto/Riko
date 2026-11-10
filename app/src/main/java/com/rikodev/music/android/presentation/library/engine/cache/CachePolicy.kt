/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B3 — Collection Engine
 *
 * File 8 / 14 — CachePolicy.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Declares WHEN a cached collection becomes stale and WHEN it must
 * be invalidated entirely.
 *
 * Two concepts:
 *
 *   TTL (Time-To-Live)
 *     How long a cached entry is considered "fresh". After TTL
 *     expires, the entry is "stale" — served while a refresh runs
 *     in the background. After HARD TTL, the entry is dropped.
 *
 *   INVALIDATION
 *     A rule that says "drop this entry NOW, regardless of TTL".
 *     Triggered by external events (new song added, favourite
 *     toggled, playlist modified, etc).
 *
 * WHY THIS IS SEPARATE FROM THE CACHE
 * -----------------------------------
 * Same reason RankingPolicy is separate from Ranker:
 *   • Tunable without code changes
 *   • Testable in isolation
 *   • Per-collection customization (Recently Played has short TTL;
 *     Favourites has long TTL; Discovery has medium TTL)
 *
 * RULES ENFORCED HERE
 * -------------------
 * #42  Diagnostics Is First-Class — cache state is observable
 * #44  Stable Key Never Changes — cache keys ARE stableKeys
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Android, no IO, no time reads. Pure data.
 *
 * © RIKO Experience OS — Engine Tier
 */

package com.rikodev.music.android.presentation.library.engine.cache

/**
 * The trigger that causes a cache entry to be invalidated.
 *
 * Each trigger maps to a real user/system action that should
 * cause the cache to drop entries that depend on the changed data.
 */
enum class InvalidationTrigger(
    val internalId: String,
    val description: String,
) {
    /** A new song was added to the library. */
    SONG_ADDED(
        internalId = "song_added",
        description = "A new song was added to the library.",
    ),

    /** A song was removed from the library. */
    SONG_REMOVED(
        internalId = "song_removed",
        description = "A song was removed from the library.",
    ),

    /** A song's play count changed (play/skip event). */
    PLAY_COUNT_CHANGED(
        internalId = "play_count_changed",
        description = "A song's play count changed.",
    ),

    /** A song was favourited or unfavourited. */
    FAVORITE_TOGGLED(
        internalId = "favorite_toggled",
        description = "A song's favourite state changed.",
    ),

    /** A playlist was created, modified, or deleted. */
    PLAYLIST_MODIFIED(
        internalId = "playlist_modified",
        description = "A playlist was created, modified, or deleted.",
    ),

    /** The user manually requested a refresh. */
    USER_REQUESTED(
        internalId = "user_requested",
        description = "The user manually requested a refresh.",
    ),

    /** The app foregrounded after a long background period. */
    APP_FOREGROUNDED(
        internalId = "app_foregrounded",
        description = "App returned to foreground after >1 hour.",
    ),

    /** The user changed their mood hint. */
    MOOD_CHANGED(
        internalId = "mood_changed",
        description = "The user's mood hint changed.",
    ),

    /** The time-of-day bucket changed (morning → afternoon, etc). */
    TIME_OF_DAY_CHANGED(
        internalId = "time_of_day_changed",
        description = "The time-of-day bucket changed.",
    ),

    /** A new session started. */
    SESSION_STARTED(
        internalId = "session_started",
        description = "A new listening session started.",
    ),

    /** The engine's configuration changed (e.g. weights retuned). */
    ENGINE_CONFIG_CHANGED(
        internalId = "engine_config_changed",
        description = "Engine configuration changed; all cache invalidated.",
    );
}

/**
 * A rule that maps an [InvalidationTrigger] to a set of collections
 * that should be invalidated.
 *
 * Example:
 *   SONG_ADDED → invalidate "Just Added" immediately,
 *                invalidate "Discovery" lazily,
 *                don't touch "Favourites"
 */
data class InvalidationRule(
    val trigger: InvalidationTrigger,
    /** Stable keys of collections to invalidate immediately. */
    val invalidateImmediately: Set<String>,
    /** Stable keys of collections to invalidate lazily (on next read). */
    val invalidateLazily: Set<String>,
    /** True if ALL collections should be invalidated. */
    val invalidateAll: Boolean,
) {
    companion object {

        /** The default rule set — used when no custom rules are provided. */
        val DEFAULTS: List<InvalidationRule> = listOf(
            InvalidationRule(
                trigger = InvalidationTrigger.SONG_ADDED,
                invalidateImmediately = setOf("recently_added", "discovery_new"),
                invalidateLazily = setOf("all_songs", "albums"),
                invalidateAll = false,
            ),
            InvalidationRule(
                trigger = InvalidationTrigger.SONG_REMOVED,
                invalidateImmediately = emptySet(),
                invalidateLazily = setOf("all_songs", "recently_played", "favorites"),
                invalidateAll = false,
            ),
            InvalidationRule(
                trigger = InvalidationTrigger.PLAY_COUNT_CHANGED,
                invalidateImmediately = setOf("recently_played", "continue_listening"),
                invalidateLazily = setOf("most_played", "hidden_gems", "forgotten_favorites"),
                invalidateAll = false,
            ),
            InvalidationRule(
                trigger = InvalidationTrigger.FAVORITE_TOGGLED,
                invalidateImmediately = setOf("favorites", "forgotten_favorites"),
                invalidateLazily = setOf("hidden_gems", "discovery"),
                invalidateAll = false,
            ),
            InvalidationRule(
                trigger = InvalidationTrigger.PLAYLIST_MODIFIED,
                invalidateImmediately = setOf("playlists"),
                invalidateLazily = emptySet(),
                invalidateAll = false,
            ),
            InvalidationRule(
                trigger = InvalidationTrigger.USER_REQUESTED,
                invalidateImmediately = emptySet(),
                invalidateLazily = emptySet(),
                invalidateAll = true,
            ),
            InvalidationRule(
                trigger = InvalidationTrigger.APP_FOREGROUNDED,
                invalidateImmediately = emptySet(),
                invalidateLazily = setOf("recently_played", "continue_listening"),
                invalidateAll = false,
            ),
            InvalidationRule(
                trigger = InvalidationTrigger.MOOD_CHANGED,
                invalidateImmediately = setOf("mood_focus", "mood_evening", "mood_night"),
                invalidateLazily = setOf("discovery"),
                invalidateAll = false,
            ),
            InvalidationRule(
                trigger = InvalidationTrigger.TIME_OF_DAY_CHANGED,
                invalidateImmediately = setOf("morning", "afternoon", "evening", "night"),
                invalidateLazily = emptySet(),
                invalidateAll = false,
            ),
            InvalidationRule(
                trigger = InvalidationTrigger.SESSION_STARTED,
                invalidateImmediately = setOf("continue_listening"),
                invalidateLazily = emptySet(),
                invalidateAll = false,
            ),
            InvalidationRule(
                trigger = InvalidationTrigger.ENGINE_CONFIG_CHANGED,
                invalidateImmediately = emptySet(),
                invalidateLazily = emptySet(),
                invalidateAll = true,
            ),
        )
    }
}

/**
 * Time-to-live configuration for a single collection's cache entry.
 *
 * Two thresholds:
 *   softTtlMs  — within this window, the entry is "fresh" (no refresh)
 *   hardTtlMs  — after this, the entry is dropped entirely
 *
 * Between soft and hard TTL, the entry is "stale" — served immediately
 * while a background refresh runs.
 */
data class TTLPolicy(
    /** Stable key — matches the collection. Rule #44. */
    val collectionStableKey: String,
    /** Fresh window — entry served without refresh. */
    val softTtlMs: Long,
    /** Hard drop — entry removed entirely. */
    val hardTtlMs: Long,
    /** True if this collection should never be cached. */
    val disabled: Boolean,
) {

    init {
        require(softTtlMs >= 0) { "softTtlMs must be >= 0" }
        require(hardTtlMs >= softTtlMs) { "hardTtlMs must be >= softTtlMs" }
    }

    companion object {

        /** 5 minutes soft / 1 hour hard — for volatile collections. */
        fun shortTtl(stableKey: String) = TTLPolicy(
            collectionStableKey = stableKey,
            softTtlMs = 5L * 60 * 1000,
            hardTtlMs = 60L * 60 * 1000,
            disabled = false,
        )

        /** 30 minutes soft / 6 hours hard — for moderately stable collections. */
        fun mediumTtl(stableKey: String) = TTLPolicy(
            collectionStableKey = stableKey,
            softTtlMs = 30L * 60 * 1000,
            hardTtlMs = 6L * 60 * 60 * 1000,
            disabled = false,
        )

        /** 6 hours soft / 24 hours hard — for stable collections like Favourites. */
        fun longTtl(stableKey: String) = TTLPolicy(
            collectionStableKey = stableKey,
            softTtlMs = 6L * 60 * 60 * 1000,
            hardTtlMs = 24L * 60 * 60 * 1000,
            disabled = false,
        )

        /** Caching disabled — always regenerate. */
        fun disabled(stableKey: String) = TTLPolicy(
            collectionStableKey = stableKey,
            softTtlMs = 0,
            hardTtlMs = 0,
            disabled = true,
        )
    }
}

/**
 * The complete cache policy for the engine.
 *
 * Holds per-collection TTL policies + the invalidation rule set.
 */
data class CachePolicy(
    /** Per-collection TTL, keyed by stableKey. Rule #44. */
    val ttlPolicies: Map<String, TTLPolicy>,
    /** Invalidation rules — applied when triggers fire. */
    val invalidationRules: List<InvalidationRule>,
    /** Default TTL to use when a collection has no specific policy. */
    val defaultTtl: TTLPolicy,
) {

    /** Get the TTL policy for a collection, falling back to default. */
    fun ttlFor(stableKey: String): TTLPolicy =
        ttlPolicies[stableKey] ?: defaultTtl

    /** Look up the invalidation rule for a trigger, if any. */
    fun ruleFor(trigger: InvalidationTrigger): InvalidationRule? =
        invalidationRules.firstOrNull { it.trigger == trigger }

    companion object {

        /** Default policy — medium TTL for everything, default invalidation rules. */
        val DEFAULT = CachePolicy(
            ttlPolicies = emptyMap(),
            invalidationRules = InvalidationRule.DEFAULTS,
            defaultTtl = TTLPolicy.mediumTtl("__default__"),
        )

        /**
         * Production policy — per-collection TTL based on stability.
         *
         * Volatile (Recently Played, Continue Listening): short
         * Moderate (Hidden Gems, Forgotten, Discovery): medium
         * Stable (Favourites, Playlists, Albums): long
         * Real-time (Diagnostic): disabled
         */
        val PRODUCTION = CachePolicy(
            ttlPolicies = mapOf(
                "recently_played" to TTLPolicy.shortTtl("recently_played"),
                "continue_listening" to TTLPolicy.shortTtl("continue_listening"),
                "most_played" to TTLPolicy.mediumTtl("most_played"),
                "hidden_gems" to TTLPolicy.mediumTtl("hidden_gems"),
                "forgotten_favorites" to TTLPolicy.mediumTtl("forgotten_favorites"),
                "discovery" to TTLPolicy.mediumTtl("discovery"),
                "discovery_new" to TTLPolicy.shortTtl("discovery_new"),
                "favorites" to TTLPolicy.longTtl("favorites"),
                "playlists" to TTLPolicy.longTtl("playlists"),
                "albums" to TTLPolicy.longTtl("albums"),
                "artists" to TTLPolicy.longTtl("artists"),
                "diagnostic_orphans" to TTLPolicy.disabled("diagnostic_orphans"),
            ),
            invalidationRules = InvalidationRule.DEFAULTS,
            defaultTtl = TTLPolicy.mediumTtl("__default__"),
        )
    }
}
