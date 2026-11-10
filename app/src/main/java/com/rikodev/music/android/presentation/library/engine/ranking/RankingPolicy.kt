/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B3 — Collection Engine
 *
 * File 6 / 14 — RankingPolicy.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Declares the rules the [CollectionRanker] follows when turning
 * a list of [CollectionScore]s into a final ordered list of items.
 *
 * The ranker's pipeline is:
 *
 *   scores ──► Normalize ──► Blend ──► Diversity ──► Deduplicate ──► Cap
 *
 * This file declares what each stage does. The ranker executes.
 *
 * WHY POLICY IS SEPARATE FROM RANKER
 * ----------------------------------
 * Two reasons:
 *
 *   1. Tunability — changing a diversity threshold should NOT
 *      require touching ranker code. Policy = data, ranker = logic.
 *
 *   2. Testability — you can unit test the ranker with synthetic
 *      policies (e.g. "diversity disabled" or "dedup aggressive")
 *      without reconfiguring the production ranker.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #42  Diagnostics Is First-Class — policy is data, explainable
 * #46  Runtime Never Creates Identity — policy never invents ids
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Android, no IO. Pure data classes.
 *
 * © RIKO Experience OS — Engine Tier
 */

package com.rikodev.music.android.presentation.library.engine.ranking

/**
 * How the normalize stage maps raw scores to a 0.0 → 1.0 range.
 */
enum class NormalizeStrategy(
    val internalId: String,
    val description: String,
) {
    /** Min-max normalization: (x - min) / (max - min). */
    MIN_MAX(
        internalId = "min_max",
        description = "Linear min-max scaling to 0.0 → 1.0.",
    ),

    /** Z-score normalization: (x - mean) / std. Then clipped to 0..1. */
    Z_SCORE(
        internalId = "z_score",
        description = "Statistical z-score, clipped to 0.0 → 1.0.",
    ),

    /** Softmax with temperature — emphasizes top items. */
    SOFTMAX(
        internalId = "softmax",
        description = "Softmax with temperature — emphasizes top items.",
    ),

    /** No normalization — assume scorers already returned 0.0 → 1.0. */
    NONE(
        internalId = "none",
        description = "Skip normalization; scorers already returned 0.0 → 1.0.",
    );
}

/**
 * How the diversity stage limits over-representation of one artist/album.
 */
enum class DiversityMode(
    val internalId: String,
    val description: String,
) {
    /** No diversity enforcement — sort strictly by score. */
    OFF(
        internalId = "off",
        description = "No diversity enforcement.",
    ),

    /** Cap consecutive items from the same artist. */
    NO_MORE_THAN_N_CONSECUTIVE(
        internalId = "no_consecutive",
        description = "Cap consecutive items from the same artist/album.",
    ),

    /** Hard cap total items per artist (e.g. max 3 per artist). */
    HARD_CAP_PER_ARTIST(
        internalId = "hard_cap_artist",
        description = "Hard cap on total items per artist in the final list.",
    ),

    /** Interleave — round-robin across artists to maximize variety. */
    INTERLEAVE(
        internalId = "interleave",
        description = "Round-robin interleave across artists.",
    );
}

/**
 * How the deduplicate stage removes duplicate items.
 */
enum class DeduplicateMode(
    val internalId: String,
    val description: String,
) {
    /** No dedup — keep all items even if duplicated. */
    OFF(
        internalId = "off",
        description = "No deduplication.",
    ),

    /** Dedup by itemId — keep only the highest-scoring instance. */
    BY_ITEM_ID(
        internalId = "by_item_id",
        description = "Dedup by itemId; keep highest score.",
    ),

    /** Dedup by content signature — same title+artist = duplicate. */
    BY_CONTENT_SIGNATURE(
        internalId = "by_content",
        description = "Dedup by title+artist signature.",
    );
}

/**
 * How the final cap (trimming to maxItems) is applied.
 */
enum class CapMode(
    val internalId: String,
    val description: String,
) {
    /** Keep the top N by score. */
    TOP_N(
        internalId = "top_n",
        description = "Keep top N by score.",
    ),

    /** Keep top N, but always include at least 1 fresh item. */
    TOP_N_WITH_FRESH(
        internalId = "top_n_with_fresh",
        description = "Keep top N, ensuring at least 1 fresh item.",
    ),

    /** Keep top N, but always include at least 1 favourite. */
    TOP_N_WITH_FAVORITE(
        internalId = "top_n_with_favorite",
        description = "Keep top N, ensuring at least 1 favourite.",
    );
}

/**
 * The complete ranking policy for a single collection.
 *
 * Built by strategies to control how the ranker processes their
 * candidate set.
 */
data class RankingPolicy(
    /** Stable key — matches the collection this policy is for. Rule #44. */
    val collectionStableKey: String,
    /** How to normalize raw scores. */
    val normalizeStrategy: NormalizeStrategy,
    /** Diversity enforcement mode. */
    val diversityMode: DiversityMode,
    /** Max consecutive items from the same artist (if diversity = NO_MORE_THAN_N_CONSECUTIVE). */
    val maxConsecutivePerArtist: Int,
    /** Hard cap per artist (if diversity = HARD_CAP_PER_ARTIST). */
    val hardCapPerArtist: Int,
    /** Deduplication mode. */
    val deduplicateMode: DeduplicateMode,
    /** Final cap mode. */
    val capMode: CapMode,
    /** Maximum items in the final output (from CollectionConstraints). */
    val maxItems: Int,
    /** Minimum items required — below this, the strategy falls back. */
    val minItems: Int,
    /** Softmax temperature (used only if normalizeStrategy = SOFTMAX). */
    val softmaxTemperature: Float,
    /** Score threshold below which an item is dropped before cap. */
    val minScoreThreshold: Float,
) {

    init {
        require(maxItems > 0) { "maxItems must be > 0; got $maxItems" }
        require(minItems >= 0) { "minItems must be >= 0; got $minItems" }
        require(minItems <= maxItems) { "minItems ($minItems) must be <= maxItems ($maxItems)" }
        require(maxConsecutivePerArtist >= 0) { "maxConsecutivePerArtist must be >= 0" }
        require(hardCapPerArtist >= 0) { "hardCapPerArtist must be >= 0" }
        require(softmaxTemperature > 0f) { "softmaxTemperature must be > 0" }
        require(minScoreThreshold in 0f..1f) { "minScoreThreshold must be in 0..1" }
    }

    companion object {

        /** Preset: continue listening — minimal diversity (user wants continuity). */
        fun continueListening(stableKey: String, maxItems: Int) = RankingPolicy(
            collectionStableKey = stableKey,
            normalizeStrategy = NormalizeStrategy.MIN_MAX,
            diversityMode = DiversityMode.OFF,
            maxConsecutivePerArtist = 0,
            hardCapPerArtist = 0,
            deduplicateMode = DeduplicateMode.BY_ITEM_ID,
            capMode = CapMode.TOP_N,
            maxItems = maxItems,
            minItems = 1,
            softmaxTemperature = 0.5f,
            minScoreThreshold = 0.1f,
        )

        /** Preset: favourites — no diversity (user wants all their favourites). */
        fun favorites(stableKey: String, maxItems: Int) = RankingPolicy(
            collectionStableKey = stableKey,
            normalizeStrategy = NormalizeStrategy.MIN_MAX,
            diversityMode = DiversityMode.OFF,
            maxConsecutivePerArtist = 0,
            hardCapPerArtist = 0,
            deduplicateMode = DeduplicateMode.BY_ITEM_ID,
            capMode = CapMode.TOP_N,
            maxItems = maxItems,
            minItems = 1,
            softmaxTemperature = 0.5f,
            minScoreThreshold = 0.3f,
        )

        /** Preset: hidden gems — high diversity to surface varied discoveries. */
        fun hiddenGems(stableKey: String, maxItems: Int) = RankingPolicy(
            collectionStableKey = stableKey,
            normalizeStrategy = NormalizeStrategy.Z_SCORE,
            diversityMode = DiversityMode.INTERLEAVE,
            maxConsecutivePerArtist = 0,
            hardCapPerArtist = 0,
            deduplicateMode = DeduplicateMode.BY_ITEM_ID,
            capMode = CapMode.TOP_N_WITH_FRESH,
            maxItems = maxItems,
            minItems = 3,
            softmaxTemperature = 0.7f,
            minScoreThreshold = 0.05f,
        )

        /** Preset: forgotten — moderate diversity, dedup by content. */
        fun forgotten(stableKey: String, maxItems: Int) = RankingPolicy(
            collectionStableKey = stableKey,
            normalizeStrategy = NormalizeStrategy.MIN_MAX,
            diversityMode = DiversityMode.NO_MORE_THAN_N_CONSECUTIVE,
            maxConsecutivePerArtist = 2,
            hardCapPerArtist = 0,
            deduplicateMode = DeduplicateMode.BY_ITEM_ID,
            capMode = CapMode.TOP_N,
            maxItems = maxItems,
            minItems = 2,
            softmaxTemperature = 0.5f,
            minScoreThreshold = 0.1f,
        )

        /** Preset: mood — high diversity, interleave. */
        fun mood(stableKey: String, maxItems: Int) = RankingPolicy(
            collectionStableKey = stableKey,
            normalizeStrategy = NormalizeStrategy.SOFTMAX,
            diversityMode = DiversityMode.INTERLEAVE,
            maxConsecutivePerArtist = 0,
            hardCapPerArtist = 0,
            deduplicateMode = DeduplicateMode.BY_ITEM_ID,
            capMode = CapMode.TOP_N,
            maxItems = maxItems,
            minItems = 3,
            softmaxTemperature = 0.6f,
            minScoreThreshold = 0.1f,
        )

        /** Preset: discovery — max diversity, fresh slant. */
        fun discovery(stableKey: String, maxItems: Int) = RankingPolicy(
            collectionStableKey = stableKey,
            normalizeStrategy = NormalizeStrategy.Z_SCORE,
            diversityMode = DiversityMode.INTERLEAVE,
            maxConsecutivePerArtist = 0,
            hardCapPerArtist = 3,  // max 3 per artist even after interleave
            deduplicateMode = DeduplicateMode.BY_CONTENT_SIGNATURE,
            capMode = CapMode.TOP_N_WITH_FRESH,
            maxItems = maxItems,
            minItems = 4,
            softmaxTemperature = 0.8f,
            minScoreThreshold = 0.0f,  // discovery doesn't filter by score
        )

        /** Preset: default — balanced, suitable for most collections. */
        fun balanced(stableKey: String, maxItems: Int) = RankingPolicy(
            collectionStableKey = stableKey,
            normalizeStrategy = NormalizeStrategy.MIN_MAX,
            diversityMode = DiversityMode.NO_MORE_THAN_N_CONSECUTIVE,
            maxConsecutivePerArtist = 2,
            hardCapPerArtist = 0,
            deduplicateMode = DeduplicateMode.BY_ITEM_ID,
            capMode = CapMode.TOP_N,
            maxItems = maxItems,
            minItems = 1,
            softmaxTemperature = 0.5f,
            minScoreThreshold = 0.1f,
        )
    }
}
