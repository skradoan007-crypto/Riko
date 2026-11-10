/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B3 — Collection Engine
 *
 * File 7 / 14 — CollectionRanker.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Executes the ranking pipeline declared in [RankingPolicy].
 *
 * Pipeline:
 *
 *   scores ──► Normalize ──► Diversity ──► Deduplicate ──► Cap ──► result
 *
 * The ranker is stateless and pure. Given the same inputs + policy,
 * it always produces the same output. This makes it trivially
 * testable on the JVM.
 *
 * DESIGN
 * ------
 * Each pipeline stage is a separate private function. The main
 * [rank] entry point chains them. This makes it easy to:
 *   • Unit test each stage in isolation
 *   • Skip stages (return early) without affecting others
 *   • Add new stages without rewriting the orchestrator
 *
 * The ranker takes a [RankedItem] intermediate type so it can carry
 * per-item context (artistId, isFresh, isFavorite) through the
 * pipeline without re-looking-up from input.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #42  Diagnostics Is First-Class — ranker emits warnings for over-pruning
 * #46  Runtime Never Creates Identity — ranker reads ids from scores
 * #47  Reducer Never Predicts — ranker IS the prediction
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Android, no IO, no time reads.
 *
 * © RIKO Experience OS — Engine Tier
 */

package com.rikodev.music.android.presentation.library.engine.ranking

import com.rikodev.music.android.presentation.library.engine.AlbumRef
import com.rikodev.music.android.presentation.library.engine.EngineInput
import com.rikodev.music.android.presentation.library.engine.SongRef
import com.rikodev.music.android.presentation.library.engine.diagnostics.EngineWarning
import com.rikodev.music.android.presentation.library.engine.scoring.CollectionScore
import com.rikodev.music.android.presentation.library.engine.scoring.ScoringSignal
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt
import androidx.compose.animation.core.Animatable

/**
 * An intermediate item carried through the ranking pipeline.
 *
 * Holds the score + context the ranker needs (artistId, isFresh, isFavorite)
 * so it doesn't have to re-look-up from input at every stage.
 */
data class RankedItem(
    /** Stable item id. Rule #44. */
    val itemId: String,
    /** Current score (may change through the pipeline). */
    var score: Float,
    /** Original score (before normalization) — for diagnostics. */
    val originalScore: Float,
    /** Artist id — for diversity. */
    val artistId: String,
    /** Album id — for diversity. */
    val albumId: String,
    /** True if the song was added recently (within 7 days). */
    val isFresh: Boolean,
    /** True if the song is favourited. */
    val isFavorite: Boolean,
    /** Top-3 reasons for this item's score. */
    val topReasons: List<String>,
) {
    /** Final score after all stages — read-only view. */
    val finalScore: Float get() = score
}

/**
 * The result of ranking a single collection.
 */
data class RankingResult(
    /** Stable key of the collection. Rule #44. */
    val collectionStableKey: String,
    /** Final ordered list of items (best first). */
    val items: List<RankedItem>,
    /** Number of items dropped by each stage. */
    val droppedByNormalize: Int,
    val droppedByDiversity: Int,
    val droppedByDedup: Int,
    val droppedByCap: Int,
    /** Warnings emitted during ranking. */
    val warnings: List<EngineWarning>,
) {
    val itemCount: Int get() = items.size
    val totalDropped: Int
        get() = droppedByNormalize + droppedByDiversity + droppedByDedup + droppedByCap
}

/**
 * Stateless ranker. Entry point is [rank].
 */
object CollectionRanker {

    /** Freshness window — songs added within this many ms are "fresh". */
    private const val FRESH_WINDOW_MS = 7L * 24 * 60 * 60 * 1000

    /**
     * Run the full ranking pipeline.
     *
     * @param scores    The composer's output for this collection.
     * @param songs     The candidate songs (for context lookups).
     * @param input     The full engine input.
     * @param policy    The ranking policy for this collection.
     * @param nowMs     Wall-clock "now".
     * @return          The ranking result with final ordered items.
     */
    fun rank(
        scores: Map<String, CollectionScore>,
        songs: List<SongRef>,
        input: EngineInput,
        policy: RankingPolicy,
        nowMs: Long,
    ): RankingResult {
        val warnings = mutableListOf<EngineWarning>()
        val songById = songs.associateBy { it.songId }

        // ── Build initial RankedItem list ─────────────────────────────
        val initial: List<RankedItem> = scores.values.map { score ->
            val song = songById[score.itemId]
            RankedItem(
                itemId = score.itemId,
                score = score.finalScore,
                originalScore = score.finalScore,
                artistId = song?.artistId ?: "",
                albumId = song?.albumId ?: "",
                isFresh = song != null && (nowMs - song.dateAddedMs) < FRESH_WINDOW_MS,
                isFavorite = song?.isFavorite ?: false,
                topReasons = score.topReasons,
            )
        }

        // ── Stage 1: Normalize ────────────────────────────────────────
        val (normalized, droppedByNormalize) = normalize(initial, policy)
        if (droppedByNormalize > 0) {
            warnings.add(
                EngineWarning.Generic(
                    codeOverride = "NORMALIZE_DROPPED",
                    messageOverride = "Normalize stage dropped $droppedByNormalize items " +
                        "(below threshold ${policy.minScoreThreshold}).",
                    stableKeyOverride = policy.collectionStableKey,
                )
            )
        }

        // ── Stage 2: Diversity ────────────────────────────────────────
        val (diversified, droppedByDiversity) = applyDiversity(normalized, policy)
        if (droppedByDiversity > 0 && droppedByDiversity > normalized.size / 2) {
            warnings.add(
                EngineWarning.DiversityOverPruned(
                    stableKey = policy.collectionStableKey,
                    before = normalized.size,
                    after = diversified.size,
                    target = policy.maxItems,
                )
            )
        }

        // ── Stage 3: Deduplicate ──────────────────────────────────────
        val (deduped, droppedByDedup) = deduplicate(diversified, songs, policy)

        // ── Stage 4: Cap ──────────────────────────────────────────────
        val (capped, droppedByCap) = applyCap(deduped, policy)

        return RankingResult(
            collectionStableKey = policy.collectionStableKey,
            items = capped,
            droppedByNormalize = droppedByNormalize,
            droppedByDiversity = droppedByDiversity,
            droppedByDedup = droppedByDedup,
            droppedByCap = droppedByCap,
            warnings = warnings,
        )
    }

    /* =================================================================================
     * STAGE 1 — NORMALIZE
     * ================================================================================= */

    private fun normalize(
        items: List<RankedItem>,
        policy: RankingPolicy,
    ): Pair<List<RankedItem>, Int> {
        if (items.isEmpty()) return emptyList<RankedItem>() to 0

        // Drop items below threshold
        val filtered = items.filter { it.score >= policy.minScoreThreshold }
        val droppedByThreshold = items.size - filtered.size

        val normalized: List<RankedItem> = when (policy.normalizeStrategy) {
            NormalizeStrategy.NONE -> filtered

            NormalizeStrategy.MIN_MAX -> {
                val min = filtered.minOfOrNull { it.score } ?: 0f
                val max = filtered.maxOfOrNull { it.score } ?: 0f
                val range = max - min
                if (range <= 0f) {
                    // All same score — leave as-is
                    filtered
                } else {
                    filtered.map { item ->
                        item.score = ((item.score - min) / range).coerceIn(0f, 1f)
                        item
                    }
                }
            }

            NormalizeStrategy.Z_SCORE -> {
                val mean = filtered.map { it.score }.average().toFloat()
                val variance = filtered.map { (it.score - mean).let { d -> d * d } }.average().toFloat()
                val std = sqrt(variance).coerceAtLeast(0.0001f)
                filtered.map { item ->
                    val z = (item.score - mean) / std
                    // Map z to 0..1 via sigmoid
                    val sigmoid = (1.0 / (1.0 + exp(-z.toDouble()))).toFloat()
                    item.score = sigmoid
                    item
                }
            }

            NormalizeStrategy.SOFTMAX -> {
                val temp = policy.softmaxTemperature.coerceAtLeast(0.01f)
                val expValues = filtered.map { exp(it.score / temp) }
                val sum = expValues.sum()
                if (sum <= 0f) {
                    filtered
                } else {
                    filtered.mapIndexed { i, item ->
                        item.score = (expValues[i] / sum).toFloat()
                        item
                    }
                }
            }
        }

        return normalized to droppedByThreshold
    }

    /* =================================================================================
     * STAGE 2 — DIVERSITY
     * ================================================================================= */

    private fun applyDiversity(
        items: List<RankedItem>,
        policy: RankingPolicy,
    ): Pair<List<RankedItem>, Int> {
        // Sort by score descending first
        val sorted = items.sortedByDescending { it.score }

        val result: List<RankedItem> = when (policy.diversityMode) {
            DiversityMode.OFF -> sorted

            DiversityMode.NO_MORE_THAN_N_CONSECUTIVE -> {
                val max = policy.maxConsecutivePerArtist.coerceAtLeast(1)
                enforceNoConsecutive(sorted, max)
            }

            DiversityMode.HARD_CAP_PER_ARTIST -> {
                val max = policy.hardCapPerArtist.coerceAtLeast(1)
                enforceHardCapPerArtist(sorted, max)
            }

            DiversityMode.INTERLEAVE -> interleaveByArtist(sorted)
        }

        val dropped = items.size - result.size
        return result to dropped
    }

    /** Drop items that would create a run of N+1 consecutive same-artist items. */
    private fun enforceNoConsecutive(
        sorted: List<RankedItem>,
        maxConsecutive: Int,
    ): List<RankedItem> {
        val result = mutableListOf<RankedItem>()
        var lastArtist = ""
        var consecutiveCount = 0

        for (item in sorted) {
            if (item.artistId == lastArtist) {
                if (consecutiveCount >= maxConsecutive) {
                    // Skip — would exceed limit
                    continue
                }
                consecutiveCount++
            } else {
                lastArtist = item.artistId
                consecutiveCount = 1
            }
            result.add(item)
        }
        return result
    }

    /** Cap total items per artist to N, keeping the highest-scoring. */
    private fun enforceHardCapPerArtist(
        sorted: List<RankedItem>,
        maxPerArtist: Int,
    ): List<RankedItem> {
        val perArtistCount = mutableMapOf<String, Int>()
        val result = mutableListOf<RankedItem>()

        for (item in sorted) {
            val count = perArtistCount[item.artistId] ?: 0
            if (count < maxPerArtist) {
                result.add(item)
                perArtistCount[item.artistId] = count + 1
            }
        }
        return result
    }

    /** Round-robin interleave across artists, preserving score order within each artist. */
    private fun interleaveByArtist(sorted: List<RankedItem>): List<RankedItem> {
        // Group by artist (preserving score order within each group)
        val byArtist = LinkedHashMap<String, MutableList<RankedItem>>()
        for (item in sorted) {
            byArtist.getOrPut(item.artistId) { mutableListOf() }.add(item)
        }

        // Round-robin pull from each artist
        val result = mutableListOf<RankedItem>()
        val iterators = byArtist.values.map { it.iterator() }.toMutableList()

        while (iterators.isNotEmpty()) {
            val toRemove = mutableListOf<MutableIterator<RankedItem>>()
            for (iter in iterators) {
                if (iter.hasNext()) {
                    result.add(iter.next())
                } else {
                    toRemove.add(iter)
                }
            }
            iterators.removeAll(toRemove)
        }
        return result
    }

    /* =================================================================================
     * STAGE 3 — DEDUPLICATE
     * ================================================================================= */

    private fun deduplicate(
        items: List<RankedItem>,
        songs: List<SongRef>,
        policy: RankingPolicy,
    ): Pair<List<RankedItem>, Int> {
        if (policy.deduplicateMode == DeduplicateMode.OFF) {
            return items to 0
        }

        val songById = songs.associateBy { it.songId }
        val seen = mutableSetOf<String>()
        val result = mutableListOf<RankedItem>()

        for (item in items) {
            val key = when (policy.deduplicateMode) {
                DeduplicateMode.BY_ITEM_ID -> item.itemId
                DeduplicateMode.BY_CONTENT_SIGNATURE -> {
                    val song = songById[item.itemId]
                    val title = song?.title?.lowercase()?.trim() ?: ""
                    val artist = song?.artistName?.lowercase()?.trim() ?: ""
                    "${title}::${artist}"
                }
                DeduplicateMode.OFF -> item.itemId  // unreachable
            }
            if (seen.add(key)) {
                result.add(item)
            }
        }

        val dropped = items.size - result.size
        return result to dropped
    }

    /* =================================================================================
     * STAGE 4 — CAP
     * ================================================================================= */

    private fun applyCap(
        items: List<RankedItem>,
        policy: RankingPolicy,
    ): Pair<List<RankedItem>, Int> {
        val max = policy.maxItems
        if (items.size <= max) return items to 0

        val result: List<RankedItem> = when (policy.capMode) {
            CapMode.TOP_N -> items.take(max)

            CapMode.TOP_N_WITH_FRESH -> {
                val topN = items.take(max).toMutableList()
                // If no fresh item in top N, swap the lowest-ranked for the highest fresh
                if (topN.none { it.isFresh }) {
                    val highestFresh = items.drop(max).firstOrNull { it.isFresh }
                    if (highestFresh != null && topN.isNotEmpty()) {
                        topN[topN.lastIndex] = highestFresh
                    }
                }
                topN
            }

            CapMode.TOP_N_WITH_FAVORITE -> {
                val topN = items.take(max).toMutableList()
                if (topN.none { it.isFavorite }) {
                    val highestFav = items.drop(max).firstOrNull { it.isFavorite }
                    if (highestFav != null && topN.isNotEmpty()) {
                        topN[topN.lastIndex] = highestFav
                    }
                }
                topN
            }
        }

        val dropped = items.size - result.size
        return result to dropped
    }
}
