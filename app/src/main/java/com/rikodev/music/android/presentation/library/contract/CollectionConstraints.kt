/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B1 — Collection Contract Pack v2 (Final)
 *
 * File 9 / 15 — CollectionConstraints.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * A collection's *constraints* are the hard limits the engine and
 * runtime MUST respect. Constraints are non-negotiable — they are
 * the difference between "Hidden Gems with 3 songs" (legal) and
 * "Hidden Gems with 3000 songs" (broken promise to the user).
 *
 * Constraints drive:
 *   • Engine emit decision (skip if below minimumItems)
 *   • Runtime card pagination (cap to maximumItems)
 *   • Cache TTL (refreshInterval)
 *   • Diagnostic sampling (diagnosticInterval)
 *   • Animation throttling (animationCooldown)
 *   • Multi-select ceiling (selectionLimit)
 *
 * RULE #36 — Contract Never Thinks
 * Constraints declare limits; the engine enforces them. This file
 * contains NO enforcement code — only declarations.
 *
 * No logic. Pure data.
 */

package com.rikodev.music.android.presentation.library.contract

/**
 * Time units used by constraint intervals. Stored as a typed
 * wrapper rather than raw Long millis to prevent unit confusion
 * at the call site.
 */
enum class ConstraintTimeUnit(internal val toMillis: Long) {
    MILLISECONDS(1L),
    SECONDS(1_000L),
    MINUTES(60_000L),
    HOURS(3_600_000L),
    DAYS(86_400_000L),
}

/**
 * A typed time interval — value + unit. The contract never reads
 * this as a raw Long; the runtime converts when needed.
 */
@JvmInline
value class ConstraintInterval(private val pack: Long) {

    val value: Long get() = pack ushr 8
    val unit: ConstraintTimeUnit
        get() = ConstraintTimeUnit.entries.first { it.ordinal + 1 == (pack and 0xFF).toInt() }

    constructor(value: Long, unit: ConstraintTimeUnit) : this(
        (value shl 8) or (unit.ordinal + 1).toLong()
    )

    val millis: Long get() = value * unit.toMillis

    companion object {
        val IMMEDIATE = ConstraintInterval(0, ConstraintTimeUnit.MILLISECONDS)
        val ONE_SECOND = ConstraintInterval(1, ConstraintTimeUnit.SECONDS)
        val FIVE_SECONDS = ConstraintInterval(5, ConstraintTimeUnit.SECONDS)
        val THIRTY_SECONDS = ConstraintInterval(30, ConstraintTimeUnit.SECONDS)
        val ONE_MINUTE = ConstraintInterval(1, ConstraintTimeUnit.MINUTES)
        val FIVE_MINUTES = ConstraintInterval(5, ConstraintTimeUnit.MINUTES)
        val FIFTEEN_MINUTES = ConstraintInterval(15, ConstraintTimeUnit.MINUTES)
        val ONE_HOUR = ConstraintInterval(1, ConstraintTimeUnit.HOURS)
        val SIX_HOURS = ConstraintInterval(6, ConstraintTimeUnit.HOURS)
        val ONE_DAY = ConstraintInterval(1, ConstraintTimeUnit.DAYS)
        val ONE_WEEK = ConstraintInterval(7, ConstraintTimeUnit.DAYS)
    }
}

/**
 * The complete constraint set for a collection.
 *
 * All values are declared up front. The engine MUST NOT produce
 * results that violate these constraints.
 */
data class CollectionConstraints(
    /** Minimum items the engine must produce to emit this collection. */
    val minimumItems: Int,
    /** Hard cap on items the engine may produce. */
    val maximumItems: Int,
    /** Minimum items to render when collapsed (for partial-expand states). */
    val minimumVisible: Int,
    /** How long the runtime may cache the engine's result. */
    val cacheTimeout: ConstraintInterval,
    /** How often the engine should re-evaluate this collection. */
    val refreshInterval: ConstraintInterval,
    /** How often the engine should re-sort this collection. */
    val sortInterval: ConstraintInterval,
    /** How often the diagnostic inspector samples this collection. */
    val diagnosticInterval: ConstraintInterval,
    /** Minimum time between two animation runs on this section. */
    val animationCooldown: ConstraintInterval,
    /** Maximum number of cards the user can multi-select at once. */
    val selectionLimit: Int,
    /** Maximum number of items the user can add to queue in one action. */
    val queueAddLimit: Int,
    /** Maximum number of items the user can add to a playlist in one action. */
    val playlistAddLimit: Int,
    /** Pre-fetch window — how many cards to render ahead of viewport. */
    val prefetchWindow: Int,
) {

    companion object {

        /** Used when a collection does not declare its own constraints. */
        val DEFAULT = CollectionConstraints(
            minimumItems = 1,
            maximumItems = 50,
            minimumVisible = 3,
            cacheTimeout = ConstraintInterval.FIFTEEN_MINUTES,
            refreshInterval = ConstraintInterval.ONE_HOUR,
            sortInterval = ConstraintInterval.SIX_HOURS,
            diagnosticInterval = ConstraintInterval.ONE_DAY,
            animationCooldown = ConstraintInterval.ONE_SECOND,
            selectionLimit = 50,
            queueAddLimit = 100,
            playlistAddLimit = 100,
            prefetchWindow = 5,
        )

        /**
         * Hero / continue-listening constraints — short, focused,
         * refreshed often.
         */
        val HERO = DEFAULT.copy(
            minimumItems = 1,
            maximumItems = 10,
            minimumVisible = 3,
            cacheTimeout = ConstraintInterval.FIVE_MINUTES,
            refreshInterval = ConstraintInterval.FIVE_MINUTES,
            sortInterval = ConstraintInterval.ONE_HOUR,
            animationCooldown = ConstraintInterval.ONE_SECOND,
            prefetchWindow = 3,
        )

        /**
         * Smart-collection constraints — broader window, longer cache.
         */
        val SMART = DEFAULT.copy(
            minimumItems = 3,
            maximumItems = 30,
            minimumVisible = 5,
            cacheTimeout = ConstraintInterval.SIX_HOURS,
            refreshInterval = ConstraintInterval.SIX_HOURS,
            sortInterval = ConstraintInterval.ONE_DAY,
            animationCooldown = ConstraintInterval.FIVE_SECONDS,
            prefetchWindow = 5,
        )

        /**
         * Library structure constraints — large lists, long cache.
         */
        val LIBRARY_STRUCTURE = DEFAULT.copy(
            minimumItems = 1,
            maximumItems = Int.MAX_VALUE,
            minimumVisible = 10,
            cacheTimeout = ConstraintInterval.ONE_DAY,
            refreshInterval = ConstraintInterval.ONE_DAY,
            sortInterval = ConstraintInterval.ONE_WEEK,
            animationCooldown = ConstraintInterval.IMMEDIATE,
            prefetchWindow = 15,
        )

        /**
         * Discovery constraints — never empty (engine tries hard),
         * capped at 30, refreshed twice a day.
         */
        val DISCOVERY = DEFAULT.copy(
            minimumItems = 3,
            maximumItems = 30,
            minimumVisible = 5,
            cacheTimeout = ConstraintInterval.SIX_HOURS,
            refreshInterval = ConstraintInterval.SIX_HOURS,
            sortInterval = ConstraintInterval.ONE_DAY,
            animationCooldown = ConstraintInterval.FIVE_SECONDS,
            prefetchWindow = 5,
        )

        /**
         * Mood constraints — short, focused, refreshed every hour.
         */
        val MOOD = DEFAULT.copy(
            minimumItems = 5,
            maximumItems = 40,
            minimumVisible = 5,
            cacheTimeout = ConstraintInterval.ONE_HOUR,
            refreshInterval = ConstraintInterval.ONE_HOUR,
            sortInterval = ConstraintInterval.SIX_HOURS,
            animationCooldown = ConstraintInterval.ONE_SECOND,
            prefetchWindow = 5,
        )

        /**
         * History constraints — read-only, refreshed rarely.
         */
        val HISTORY = DEFAULT.copy(
            minimumItems = 1,
            maximumItems = 5000,
            minimumVisible = 20,
            cacheTimeout = ConstraintInterval.ONE_DAY,
            refreshInterval = ConstraintInterval.ONE_DAY,
            sortInterval = ConstraintInterval.ONE_WEEK,
            animationCooldown = ConstraintInterval.IMMEDIATE,
            prefetchWindow = 20,
        )

        /**
         * Diagnostic constraints — small, refreshed on demand.
         */
        val DIAGNOSTIC = DEFAULT.copy(
            minimumItems = 1,
            maximumItems = 500,
            minimumVisible = 10,
            cacheTimeout = ConstraintInterval.IMMEDIATE,
            refreshInterval = ConstraintInterval.ONE_MINUTE,
            sortInterval = ConstraintInterval.ONE_MINUTE,
            animationCooldown = ConstraintInterval.IMMEDIATE,
            selectionLimit = Int.MAX_VALUE,
            prefetchWindow = 20,
        )
    }
}
