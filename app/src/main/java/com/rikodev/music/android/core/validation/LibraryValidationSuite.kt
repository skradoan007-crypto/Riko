/*
 * RIKO EXPERIENCE OS — PRODUCTION-GRADE ROADMAP v2.0
 * Milestone 2.9 — Experience Validation
 *
 * File 2 / 10 — LibraryValidationSuite.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Gate G1 — Library Reality Test. Generates synthetic song catalogs at
 * 10K, 20K, 50K, and 100K scales, then runs assertions against the
 * Collection Engine to verify ranking, scroll, and memory behavior.
 *
 * This is the data-generation + assertion layer. The actual execution
 * (loading the synthetic data into the live Repository) is performed
 * by the analysis-phase test harness — this file declares WHAT to
 * verify, not HOW to run it.
 *
 * SCALE TIERS
 * -----------
 *
 *   Tier S1 — 10,000 tracks    "Comfortable"  — should pass with margin
 *   Tier S2 — 20,000 tracks    "Typical user" — should pass
 *   Tier S3 — 50,000 tracks    "Power user"   — should pass, ranking <500ms
 *   Tier S4 — 100,000 tracks   "Collector"    — should pass, ranking <1s
 *
 * PER-TIER ASSERTIONS
 * -------------------
 *   - Initial load completes without OOM
 *   - First frame renders within cold-start budget
 *   - Scroll jank rate below threshold
 *   - Collection ranking completes within time budget
 *   - Memory footprint within scale-adjusted budget
 *   - No native memory growth beyond threshold
 *
 * RULES ENFORCED
 * --------------
 * #52  Cache Never Changes Data — synthetic data must round-trip unchanged
 * #53  Diagnostics Never Affects Engine — assertions observe, never mutate
 * #76  Performance Guards Never Optimize — reports only, no auto-degrade
 *
 * © RIKO Experience OS — Validation Tier
 */

package com.rikodev.music.android.core.validation



// ═══════════════════════════════════════════════════════════════════════════
// SCALE TIER
// ═══════════════════════════════════════════════════════════════════════════

/**
 * A scale tier for the Library Reality Test.
 *
 * @property code           Stable code (S1–S4).
 * @property trackCount     Number of synthetic tracks to generate.
 * @property displayName    Human-readable label.
 * @property personaDescription  Who this tier represents.
 */
enum class LibraryScaleTier(
    val code: String,
    val trackCount: Int,
    val displayName: String,
    val personaDescription: String,
) {
    S1("S1", 10_000, "Comfortable", "Casual listener — small library"),
    S2("S2", 20_000, "Typical", "Average streaming-era listener"),
    S3("S3", 50_000, "Power User", "Music enthusiast with curated collection"),
    S4("S4", 100_000, "Collector", "Audiophile with deep archive");

    init {
        require(trackCount > 0) { "Track count must be positive for $code" }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SYNTHETIC SONG GENERATION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * A minimal synthetic song representation for validation.
 *
 * Deliberately NOT the production [Song] model — validation data must
 * not depend on production types, so changes to production models do
 * not break historical validation reports.
 */
data class SyntheticSong(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val year: Int,
    val genre: String,
    val playCount: Int,
    val dateAddedMs: Long,
    val filePath: String,
)

/**
 * Configuration for synthetic catalog generation.
 *
 * @property seed              Deterministic seed for reproducible data.
 * @property titleVocabulary   Words to combine when generating titles.
 * @property artistCount       Distinct artists in the catalog.
 * @property albumDensity      Average tracks per album.
 * @property genreDistribution Probability weights per genre (sums to 1.0).
 */
data class SyntheticCatalogConfig(
    val seed: Long = 0xC0FFEE,
    val titleVocabulary: List<String> = defaultTitleVocabulary(),
    val artistCount: Int = 500,
    val albumDensity: Int = 12,
    val genreDistribution: Map<String, Double> = defaultGenreDistribution(),
) {
    init {
        require(artistCount > 0) { "Artist count must be positive" }
        require(albumDensity in 1..100) { "Album density must be 1..100" }
        val sum = genreDistribution.values.sum()
        require(sum in 0.99..1.01) { "Genre distribution must sum to 1.0, got $sum" }
    }

    companion object {
        fun defaultTitleVocabulary(): List<String> = listOf(
            "Midnight", "Echoes", "Velvet", "Horizon", "Whisper", "Ember",
            "Tide", "Cascade", "Solace", "Mirage", "Aurora", "Reverie",
            "Lantern", "Sapphire", "Drift", "Cinder", "Lullaby", "Mosaic",
            "Phantom", "Serenade", "Twilight", "Voyage", "Zenith", "Bloom",
        )

        fun defaultGenreDistribution(): Map<String, Double> = mapOf(
            "Rock" to 0.20,
            "Pop" to 0.18,
            "Electronic" to 0.15,
            "Hip Hop" to 0.12,
            "Classical" to 0.10,
            "Jazz" to 0.08,
            "Folk" to 0.06,
            "Ambient" to 0.05,
            "Metal" to 0.03,
            "World" to 0.03,
        )
    }
}

/**
 * Deterministic pseudo-random generator for synthetic data.
 *
 * Uses a simple LCG so results are reproducible across runs and
 * platforms. NOT cryptographically secure — validation data only.
 */
class ValidationRandom(state: Long) {

    private var state: Long = state xor 0x5DEECE66DL

    /** Next non-negative Int bits. */
    fun nextInt(bound: Int): Int {
        require(bound > 0) { "Bound must be positive" }
        state = (state * 0x5DEECE66DL + 0xBL) and ((1L shl 48) - 1)
        return ((state shr 16).toInt() and Int.MAX_VALUE) % bound
    }

    /** Next Long in [0, bound). */
    fun nextLong(bound: Long): Long {
        require(bound > 0) { "Bound must be positive" }
        state = (state * 0x5DEECE66DL + 0xBL) and ((1L shl 48) - 1)
        return (state shr 16) % bound
    }

    /** Next Double in [0.0, 1.0). */
    fun nextDouble(): Double {
        state = (state * 0x5DEECE66DL + 0xBL) and ((1L shl 48) - 1)
        return (state shr 16).toDouble() / (1L shl 32).toDouble()
    }

    /** Next Boolean with 50/50 probability. */
    fun nextBoolean(): Boolean = nextInt(2) != 0

    /** Pick a random element from [items]. */
    fun <T> pick(items: List<T>): T {
        require(items.isNotEmpty()) { "Cannot pick from empty list" }
        return items[nextInt(items.size)]
    }

    /** Pick a weighted random key from a probability map. */
    fun <T> pickWeighted(weights: Map<T, Double>): T {
        require(weights.isNotEmpty()) { "Weights map cannot be empty" }
        var roll = nextDouble()
        for ((key, weight) in weights) {
            roll -= weight
            if (roll <= 0.0) return key
        }
        return weights.keys.last()
    }
}

/**
 * Generator for synthetic song catalogs.
 *
 * Produces deterministic data for a given scale tier. Each tier
 * produces the same catalog every run (given the same seed), so
 * historical comparisons are meaningful.
 */
class SyntheticCatalogGenerator(
    private val config: SyntheticCatalogConfig = SyntheticCatalogConfig(),
) {

    private val random = ValidationRandom(config.seed)

    /** Artist names generated once and reused. */
    private val artists: List<String> = (1..config.artistCount).map { idx ->
        val prefix = random.pick(listOf("The", "Lazy", "Silent", "Neon", "Ancient", "Glass", "Iron", "Velvet"))
        val root = random.pick(config.titleVocabulary)
        val suffix = random.pick(listOf("Project", "Collective", "Trio", "Ensemble", "Band", "Solo", ""))
        "$prefix $root $suffix".trim().replace("  ", " ")
    }

    /** Album pool keyed by artist for album-density realism. */
    private val albumsByArtist: Map<String, List<String>> = artists.associateWith { artist ->
        val albumCount = (config.albumDensity * (1 + random.nextInt(3) * 0.3).toInt()).coerceAtLeast(1)
        (1..albumCount).map { idx ->
            "${random.pick(config.titleVocabulary)} ${random.pick(config.titleVocabulary)} — Vol. $idx"
        }
    }

    /**
     * Generate [tier.trackCount] synthetic songs.
     */
    fun generate(tier: LibraryScaleTier): List<SyntheticSong> {
        val baseTimestamp = 1_700_000_000_000L
        return (1..tier.trackCount).map { id ->
            val artist = random.pick(artists)
            val album = random.pick(albumsByArtist[artist]!!)
            SyntheticSong(
                id = id.toLong(),
                title = "${random.pick(config.titleVocabulary)} ${random.pick(config.titleVocabulary)}",
                artist = artist,
                album = album,
                durationMs = 90_000L + random.nextLong(360_000L),
                year = 1960 + random.nextInt(65),
                genre = random.pickWeighted(config.genreDistribution),
                playCount = random.nextInt(500),
                dateAddedMs = baseTimestamp - random.nextLong(365L * 24 * 3600 * 1000),
                filePath = "/music/$artist/$album/${id.toString().padStart(6, '0')}.mp3",
            )
        }
    }

    /**
     * Stream generation — useful for very large tiers (S4) to avoid
     * holding the entire catalog in memory at once during generation.
     */
    fun stream(tier: LibraryScaleTier): Sequence<SyntheticSong> = sequence {
        val baseTimestamp = 1_700_000_000_000L
        for (id in 1..tier.trackCount) {
            val artist = random.pick(artists)
            val album = random.pick(albumsByArtist[artist]!!)
            yield(
                SyntheticSong(
                    id = id.toLong(),
                    title = "${random.pick(config.titleVocabulary)} ${random.pick(config.titleVocabulary)}",
                    artist = artist,
                    album = album,
                    durationMs = 90_000L + random.nextLong(360_000L),
                    year = 1960 + random.nextInt(65),
                    genre = random.pickWeighted(config.genreDistribution),
                    playCount = random.nextInt(500),
                    dateAddedMs = baseTimestamp - random.nextLong(365L * 24 * 3600 * 1000),
                    filePath = "/music/$artist/$album/${id.toString().padStart(6, '0')}.mp3",
                )
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// PER-TIER ASSERTIONS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Performance budget for a single scale tier.
 *
 * @property tier                  The scale tier this budget applies to.
 * @property coldStartMs           Max time for first frame after load.
 * @property scrollFrameMs         Max frame time during sustained scroll.
 * @property scrollJankRate        Max acceptable jank rate (0.0–1.0).
 * @property rankingMs             Max time for Collection Engine ranking pass.
 * @property heapMb                Max Java heap after load.
 * @property nativeGrowthMb        Max native memory growth during churn.
 * @property recomposePerScroll    Max recompositions per scroll gesture.
 */
data class ScaleTierBudget(
    val tier: LibraryScaleTier,
    val coldStartMs: Long,
    val scrollFrameMs: Long,
    val scrollJankRate: Double,
    val rankingMs: Long,
    val heapMb: Long,
    val nativeGrowthMb: Long,
    val recomposePerScroll: Int,
) {
    init {
        require(scrollJankRate in 0.0..1.0) { "Jank rate must be 0.0–1.0" }
    }

    companion object {
        /**
         * Default budgets per tier. Tuned for mid-range 2023 device.
         * Analysis phase may override these via [LibraryValidationSuite.overrideBudget].
         */
        fun defaults(): Map<LibraryScaleTier, ScaleTierBudget> = mapOf(
            LibraryScaleTier.S1 to ScaleTierBudget(
                tier = LibraryScaleTier.S1,
                coldStartMs = 600,
                scrollFrameMs = 16,
                scrollJankRate = 0.01,
                rankingMs = 200,
                heapMb = 80,
                nativeGrowthMb = 2,
                recomposePerScroll = 6,
            ),
            LibraryScaleTier.S2 to ScaleTierBudget(
                tier = LibraryScaleTier.S2,
                coldStartMs = 800,
                scrollFrameMs = 16,
                scrollJankRate = 0.02,
                rankingMs = 350,
                heapMb = 140,
                nativeGrowthMb = 3,
                recomposePerScroll = 8,
            ),
            LibraryScaleTier.S3 to ScaleTierBudget(
                tier = LibraryScaleTier.S3,
                coldStartMs = 1_200,
                scrollFrameMs = 18,
                scrollJankRate = 0.03,
                rankingMs = 500,
                heapMb = 240,
                nativeGrowthMb = 4,
                recomposePerScroll = 10,
            ),
            LibraryScaleTier.S4 to ScaleTierBudget(
                tier = LibraryScaleTier.S4,
                coldStartMs = 1_800,
                scrollFrameMs = 20,
                scrollJankRate = 0.05,
                rankingMs = 1_000,
                heapMb = 400,
                nativeGrowthMb = 6,
                recomposePerScroll = 12,
            ),
        )
    }
}

/**
 * A single measurement taken during a tier test.
 */
data class TierMeasurement(
    val tier: LibraryScaleTier,
    val coldStartMs: Long,
    val scrollFrameMsP50: Long,
    val scrollFrameMsP95: Long,
    val scrollFrameMsP99: Long,
    val scrollJankRate: Double,
    val rankingMs: Long,
    val heapMb: Long,
    val nativeGrowthMb: Long,
    val recomposePerScroll: Int,
) {
    /**
     * Evaluate this measurement against the given budget.
     * Returns a list of failures (empty if pass).
     */
    fun evaluateAgainst(budget: ScaleTierBudget): List<TierAssertionFailure> {
        val failures = mutableListOf<TierAssertionFailure>()
        if (coldStartMs > budget.coldStartMs) {
            failures += TierAssertionFailure("Cold start", coldStartMs, budget.coldStartMs, "ms")
        }
        if (scrollFrameMsP95 > budget.scrollFrameMs) {
            failures += TierAssertionFailure("Scroll P95 frame", scrollFrameMsP95, budget.scrollFrameMs, "ms")
        }
        if (scrollJankRate > budget.scrollJankRate) {
            failures += TierAssertionFailure("Scroll jank rate", (scrollJankRate * 100).toLong(), (budget.scrollJankRate * 100).toLong(), "%")
        }
        if (rankingMs > budget.rankingMs) {
            failures += TierAssertionFailure("Ranking time", rankingMs, budget.rankingMs, "ms")
        }
        if (heapMb > budget.heapMb) {
            failures += TierAssertionFailure("Heap size", heapMb, budget.heapMb, "MB")
        }
        if (nativeGrowthMb > budget.nativeGrowthMb) {
            failures += TierAssertionFailure("Native growth", nativeGrowthMb, budget.nativeGrowthMb, "MB")
        }
        if (recomposePerScroll > budget.recomposePerScroll) {
            failures += TierAssertionFailure("Recompositions/scroll", recomposePerScroll.toLong(), budget.recomposePerScroll.toLong(), "count")
        }
        return failures
    }
}

/**
 * A single assertion failure with context.
 */
data class TierAssertionFailure(
    val metric: String,
    val actual: Long,
    val expected: Long,
    val unit: String,
) {
    val summary: String
        get() = "$metric: $actual $unit (budget $expected $unit)"
}

// ═══════════════════════════════════════════════════════════════════════════
// TIER RESULT
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Result of running one tier of the Library Reality Test.
 */
data class TierResult(
    val tier: LibraryScaleTier,
    val measurement: TierMeasurement,
    val budget: ScaleTierBudget,
    val failures: List<TierAssertionFailure>,
    val generatedTrackCount: Int,
    val timestampMs: Long,
) {
    val passed: Boolean get() = failures.isEmpty()

    val summary: String
        get() = buildString {
            append("[${tier.code}] ${tier.displayName} (${tier.trackCount} tracks): ")
            if (passed) {
                append("PASS")
            } else {
                append("FAIL — ${failures.size} assertion(s) failed")
            }
        }
}

// ═══════════════════════════════════════════════════════════════════════════
// VALIDATION SUITE
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The Library Reality Test suite (Gate G1).
 *
 * Orchestrates synthetic catalog generation, declares per-tier
 * budgets, and aggregates results. The actual execution (loading
 * data into the live Repository, measuring frame times, etc.) is
 * performed by the analysis-phase harness — this class declares
 * WHAT to verify and HOW to interpret results.
 *
 * Thread-safety: not thread-safe. Intended for single-threaded use.
 */
class LibraryValidationSuite(
    private val generator: SyntheticCatalogGenerator = SyntheticCatalogGenerator(),
    private var budgets: Map<LibraryScaleTier, ScaleTierBudget> = ScaleTierBudget.defaults(),
) {

    private val _results: MutableList<TierResult> = mutableListOf()
    val results: List<TierResult> get() = _results.toList()

    /**
     * Override the default budget for a tier. Use when the analysis
     * phase decides a different target is appropriate for the device.
     */
    fun overrideBudget(tier: LibraryScaleTier, budget: ScaleTierBudget) {
        require(budget.tier == tier) {
            "Budget tier mismatch: ${budget.tier} != $tier"
        }
        budgets = budgets + (tier to budget)
    }

    /**
     * Generate a synthetic catalog for [tier] without running assertions.
     * Useful for pre-warming caches or for the analysis phase to inspect
     * the data before testing.
     */
    fun generateCatalog(tier: LibraryScaleTier): List<SyntheticSong> =
        generator.generate(tier)

    /**
     * Generate a streaming sequence for [tier]. Use for S4 to avoid
     * holding 100K songs in memory during generation.
     */
    fun streamCatalog(tier: LibraryScaleTier): Sequence<SyntheticSong> =
        generator.stream(tier)

    /**
     * Record a measurement taken by the analysis-phase harness.
     *
     * The harness is responsible for actually loading the synthetic
     * catalog, running the app, and collecting measurements. This
     * method evaluates the measurement against the budget and stores
     * the result.
     */
    fun recordMeasurement(measurement: TierMeasurement): TierResult {
        val budget = budgets[measurement.tier]
            ?: error("No budget declared for tier ${measurement.tier.code}")
        val failures = measurement.evaluateAgainst(budget)
        val result = TierResult(
            tier = measurement.tier,
            measurement = measurement,
            budget = budget,
            failures = failures,
            generatedTrackCount = measurement.tier.trackCount,
            timestampMs = System.currentTimeMillis(),
        )
        _results += result
        return result
    }

    /**
     * Evaluate whether Gate G1 passes.
     *
     * Pass requires every tier to have a recorded result, and every
     * recorded result to have passed.
     */
    fun evaluate(): GateG1Verdict {
        val expected = LibraryScaleTier.values().toSet()
        val recorded = _results.map { it.tier }.toSet()
        val missing = expected - recorded
        val failed = _results.filter { !it.passed }

        return when {
            missing.isNotEmpty() -> GateG1Verdict.Incomplete(missing)
            failed.isNotEmpty() -> GateG1Verdict.Failed(failed)
            else -> GateG1Verdict.Passed(_results.toList())
        }
    }

    /**
     * Generate a human-readable summary of all recorded results.
     */
    fun summaryReport(): String = buildString {
        appendLine("═══ GATE G1 — LIBRARY REALITY TEST ═══")
        appendLine()
        if (_results.isEmpty()) {
            appendLine("No measurements recorded yet.")
            return@buildString
        }
        _results.forEach { result ->
            appendLine(result.summary)
            if (!result.passed) {
                result.failures.forEach { failure ->
                    appendLine("    ✗ ${failure.summary}")
                }
            }
        }
        appendLine()
        appendLine("Verdict: ${evaluate().javaClass.simpleName}")
    }
}

/**
 * Verdict for Gate G1.
 */
sealed class GateG1Verdict {
    /** All tiers passed. */
    data class Passed(val results: List<TierResult>) : GateG1Verdict()

    /** Some tiers have not been measured. */
    data class Incomplete(val missingTiers: Set<LibraryScaleTier>) : GateG1Verdict() {
        init {
            require(missingTiers.isNotEmpty()) { "Incomplete verdict must list missing tiers" }
        }
    }

    /** One or more tiers failed. */
    data class Failed(val failedResults: List<TierResult>) : GateG1Verdict() {
        init {
            require(failedResults.isNotEmpty()) { "Failed verdict must list failed results" }
        }
    }

    val isPassed: Boolean get() = this is Passed
}


