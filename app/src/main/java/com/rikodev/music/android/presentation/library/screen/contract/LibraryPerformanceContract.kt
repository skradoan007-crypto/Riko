/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.1 — Library Screen Constitution
 *
 * File 10 / 15 — LibraryPerformanceContract.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * The Library Performance Contract declares the *performance
 * budget* of the Library screen — target FPS, jank tolerance,
 * prefetch rules, and memory ceilings.
 *
 * RIKO's performance philosophy: performance is a *feature*. A
 * janky Library is a broken Library, no matter how beautiful the
 * contracts are. This contract makes performance *declarable* —
 * the renderer (B5.3) reads it and tunes itself accordingly.
 *
 * B5 ACCEPTANCE GATE (from scope freeze):
 *   □ Library opens under 250ms
 *   □ 60 FPS sustained
 *   □ 50k+ songs scroll smoothly
 *   □ Performance gate pass
 *
 * This contract is the declaration behind those gates.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #53  Diagnostics Never Affects Engine — perf targets are observation
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Compose, no Android framework, no Coroutine, no Flow, no
 * ViewModel. Pure Kotlin data declaration.
 *
 * © RIKO Experience OS — Constitution Tier
 */

package com.rikodev.music.android.presentation.library.screen.contract

/**
 * The target FPS for the Library screen.
 */
enum class LibraryFpsTarget(
    internal val description: String,
    internal val targetHz: Int,
    internal val minAcceptableHz: Int,
) {
    /** 30 FPS — for low-end devices / battery saver. */
    LOW_END(
        description = "30 FPS — low-end / battery saver.",
        targetHz = 30,
        minAcceptableHz = 24,
    ),

    /** 60 FPS — standard target. */
    STANDARD(
        description = "60 FPS — standard.",
        targetHz = 60,
        minAcceptableHz = 50,
    ),

    /** 90 FPS — for high-refresh-rate phones. */
    HIGH(
        description = "90 FPS — high-refresh-rate.",
        targetHz = 90,
        minAcceptableHz = 75,
    ),

    /** 120 FPS — for premium phones. */
    PREMIUM(
        description = "120 FPS — premium.",
        targetHz = 120,
        minAcceptableHz = 100,
    ),

    /** 144 FPS — for tablets / foldables. */
    ULTRA(
        description = "144 FPS — tablets / foldables.",
        targetHz = 144,
        minAcceptableHz = 120,
    ),
}

/**
 * The jank budget — how much jank is tolerable.
 *
 * @property maxJankFramesPerSecond   Max janky frames allowed per second.
 * @property maxFrameDropStreak       Max consecutive dropped frames.
 * @property jankDetectionThresholdMs  Frame time (ms) above which is "jank".
 */
data class LibraryJankBudget(
    val maxJankFramesPerSecond: Int,
    val maxFrameDropStreak: Int,
    val jankDetectionThresholdMs: Long,
) {
    companion object {
        /** Strict jank budget — for high-refresh devices. */
        val STRICT = LibraryJankBudget(
            maxJankFramesPerSecond = 2,
            maxFrameDropStreak = 3,
            jankDetectionThresholdMs = 16,
        )

        /** Standard jank budget. */
        val STANDARD = LibraryJankBudget(
            maxJankFramesPerSecond = 5,
            maxFrameDropStreak = 5,
            jankDetectionThresholdMs = 20,
        )

        /** Relaxed jank budget — for low-end devices. */
        val RELAXED = LibraryJankBudget(
            maxJankFramesPerSecond = 10,
            maxFrameDropStreak = 10,
            jankDetectionThresholdMs = 33,
        )
    }
}

/**
 * Frame timing rule — when to skip work to maintain FPS.
 *
 * @property frameTimeBudgetMs       Max time (ms) per frame for layout/draw.
 * @property prefetchAheadDistanceDp  How far ahead (dp) to prefetch content.
 * @property recycleBehindDistanceDp  How far behind (dp) to recycle content.
 * @property isPrefetchEnabled       True if prefetching is enabled.
 */
data class LibraryFrameTimingRule(
    val frameTimeBudgetMs: Long,
    val prefetchAheadDistanceDp: Int,
    val recycleBehindDistanceDp: Int,
    val isPrefetchEnabled: Boolean,
) {
    companion object {
        val STANDARD = LibraryFrameTimingRule(
            frameTimeBudgetMs = 8,
            prefetchAheadDistanceDp = 600,
            recycleBehindDistanceDp = 1200,
            isPrefetchEnabled = true,
        )

        val AGGRESSIVE = LibraryFrameTimingRule(
            frameTimeBudgetMs = 6,
            prefetchAheadDistanceDp = 900,
            recycleBehindDistanceDp = 900,
            isPrefetchEnabled = true,
        )

        val CONSERVATIVE = LibraryFrameTimingRule(
            frameTimeBudgetMs = 12,
            prefetchAheadDistanceDp = 400,
            recycleBehindDistanceDp = 800,
            isPrefetchEnabled = true,
        )
    }
}

/**
 * Prefetch rule — what to prefetch and when.
 *
 * @property imagePrefetchCount       How many images to prefetch ahead.
 * @property metadataPrefetchCount    How many metadata entries to prefetch.
 * @property prefetchOnIdle           True if prefetch happens during idle.
 * @property prefetchNetworkEnabled   True if network prefetch is allowed.
 */
data class LibraryPrefetchRule(
    val imagePrefetchCount: Int,
    val metadataPrefetchCount: Int,
    val prefetchOnIdle: Boolean,
    val prefetchNetworkEnabled: Boolean,
) {
    companion object {
        val DEFAULT = LibraryPrefetchRule(
            imagePrefetchCount = 12,
            metadataPrefetchCount = 50,
            prefetchOnIdle = true,
            prefetchNetworkEnabled = true,
        )

        val AGGRESSIVE = LibraryPrefetchRule(
            imagePrefetchCount = 24,
            metadataPrefetchCount = 100,
            prefetchOnIdle = true,
            prefetchNetworkEnabled = true,
        )

        val CONSERVATIVE = LibraryPrefetchRule(
            imagePrefetchCount = 6,
            metadataPrefetchCount = 20,
            prefetchOnIdle = true,
            prefetchNetworkEnabled = false,
        )
    }
}

/**
 * Memory budget — ceilings for memory usage.
 *
 * @property maxImageCacheMb          Max image cache size (MB).
 * @property maxMetadataCacheMb       Max metadata cache size (MB).
 * @property maxSnapshotCacheMb       Max render snapshot cache (MB).
 * @property trimOnPressure           True if caches trim under memory pressure.
 */
data class LibraryMemoryBudget(
    val maxImageCacheMb: Int,
    val maxMetadataCacheMb: Int,
    val maxSnapshotCacheMb: Int,
    val trimOnPressure: Boolean,
) {
    companion object {
        val DEFAULT = LibraryMemoryBudget(
            maxImageCacheMb = 64,
            maxMetadataCacheMb = 16,
            maxSnapshotCacheMb = 8,
            trimOnPressure = true,
        )

        val LOW_END = LibraryMemoryBudget(
            maxImageCacheMb = 24,
            maxMetadataCacheMb = 8,
            maxSnapshotCacheMb = 4,
            trimOnPressure = true,
        )

        val HIGH_END = LibraryMemoryBudget(
            maxImageCacheMb = 128,
            maxMetadataCacheMb = 32,
            maxSnapshotCacheMb = 16,
            trimOnPressure = true,
        )
    }
}

/**
 * Opening time budget — how fast the Library should open.
 *
 * @property coldStartTargetMs         Cold start target (ms).
 * @property coldStartMaxMs            Cold start max acceptable (ms).
 * @property warmStartTargetMs         Warm start target (ms).
 * @property warmStartMaxMs            Warm start max acceptable (ms).
 * @property firstFrameTargetMs        First frame target (ms).
 */
data class LibraryOpeningBudget(
    val coldStartTargetMs: Long,
    val coldStartMaxMs: Long,
    val warmStartTargetMs: Long,
    val warmStartMaxMs: Long,
    val firstFrameTargetMs: Long,
) {
    companion object {
        val DEFAULT = LibraryOpeningBudget(
            coldStartTargetMs = 250,
            coldStartMaxMs = 500,
            warmStartTargetMs = 120,
            warmStartMaxMs = 240,
            firstFrameTargetMs = 80,
        )

        /** Strict opening budget — for acceptance gate. */
        val STRICT = LibraryOpeningBudget(
            coldStartTargetMs = 200,
            coldStartMaxMs = 350,
            warmStartTargetMs = 100,
            warmStartMaxMs = 180,
            firstFrameTargetMs = 64,
        )
    }
}

/**
 * The Library Performance Contract.
 *
 * @property fpsTarget         Target FPS.
 * @property jankBudget        Jank budget.
 * @property frameTimingRule   Frame timing rule.
 * @property prefetchRule      Prefetch rule.
 * @property memoryBudget      Memory budget.
 * @property openingBudget     Opening time budget.
 * @property isPerformanceOverlayEnabled  True if the perf overlay is on.
 */
data class LibraryPerformanceContract(
    val fpsTarget: LibraryFpsTarget,
    val jankBudget: LibraryJankBudget,
    val frameTimingRule: LibraryFrameTimingRule,
    val prefetchRule: LibraryPrefetchRule,
    val memoryBudget: LibraryMemoryBudget,
    val openingBudget: LibraryOpeningBudget,
    val isPerformanceOverlayEnabled: Boolean,
) {
    companion object {
        val DEFAULT = LibraryPerformanceContract(
            fpsTarget = LibraryFpsTarget.STANDARD,
            jankBudget = LibraryJankBudget.STANDARD,
            frameTimingRule = LibraryFrameTimingRule.STANDARD,
            prefetchRule = LibraryPrefetchRule.DEFAULT,
            memoryBudget = LibraryMemoryBudget.DEFAULT,
            openingBudget = LibraryOpeningBudget.DEFAULT,
            isPerformanceOverlayEnabled = false,
        )

        /** Low-end device profile. */
        val LOW_END = LibraryPerformanceContract(
            fpsTarget = LibraryFpsTarget.LOW_END,
            jankBudget = LibraryJankBudget.RELAXED,
            frameTimingRule = LibraryFrameTimingRule.CONSERVATIVE,
            prefetchRule = LibraryPrefetchRule.CONSERVATIVE,
            memoryBudget = LibraryMemoryBudget.LOW_END,
            openingBudget = LibraryOpeningBudget.DEFAULT,
            isPerformanceOverlayEnabled = false,
        )

        /** High-end device profile. */
        val HIGH_END = LibraryPerformanceContract(
            fpsTarget = LibraryFpsTarget.HIGH,
            jankBudget = LibraryJankBudget.STRICT,
            frameTimingRule = LibraryFrameTimingRule.AGGRESSIVE,
            prefetchRule = LibraryPrefetchRule.AGGRESSIVE,
            memoryBudget = LibraryMemoryBudget.HIGH_END,
            openingBudget = LibraryOpeningBudget.STRICT,
            isPerformanceOverlayEnabled = false,
        )
    }
}
