/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B3 — Collection Engine
 *
 * File 4 / 14 — CollectionScorer.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * The 6 scorers that turn raw [EngineInput] data into
 * [SignalContribution]s for a given [SongRef].
 *
 * Each scorer is RESPONSIBLE FOR ONE SIGNAL ONLY. This separation
 * is critical — it lets us:
 *
 *   • Unit test each signal in isolation
 *   • Tune weights without touching scorer math
 *   • Add a new signal without modifying existing scorers
 *   • Disable a signal per-collection by setting its weight to 0
 *
 * DESIGN
 * ------
 * Each scorer is a stateless object (Kotlin object singleton) that
 * implements [Scorer]. The scorer reads from [EngineInput] (which
 * is immutable) and returns a [SignalContribution] — also immutable.
 *
 * Scorers NEVER:
 *   • Mutate input
 *   • Read wall-clock time (the host passes nowMs via EngineInput)
 *   • Touch Android / IO / network
 *   • Know which collection they're scoring for (the [ScoreProfile]
 *     handles that)
 *
 * Scorers DO:
 *   • Compute a raw score 0.0 → 1.0
 *   • Compute a confidence 0.0 → 1.0 (low if data is missing)
 *   • Emit a short human-readable note for the Inspector
 *
 * RULES ENFORCED HERE
 * -------------------
 * #42  Diagnostics Is First-Class — every score has a note
 * #46  Runtime Never Creates Identity — scorer reads itemId from input
 * #47  Reducer Never Predicts — scorer IS the prediction
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Android, no IO, no time reads, no random without seed.
 *
 * © RIKO Experience OS — Engine Tier
 */

package com.rikodev.music.android.presentation.library.engine.scoring

import com.rikodev.music.android.presentation.library.engine.EngineInput
import com.rikodev.music.android.presentation.library.engine.SongRef
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import androidx.compose.animation.core.Animatable

/**
 * Contract for all scorers. Stateless — safe to call from any thread.
 */
interface Scorer {

    /** Which signal this scorer produces. */
    val signal: ScoringSignal

    /** Stable name for diagnostics. */
    val name: String

    /**
     * Score a single song against the input.
     *
     * @param song        The song to score.
     * @param input       The complete engine input.
     * @param nowMs       Wall-clock "now" (passed in, never read by scorer).
     * @return            A [SignalContribution] with rawScore + confidence + note.
     *                    Weight is left at 0 — the [ScoreComposer] applies weight.
     */
    fun score(
        song: SongRef,
        input: EngineInput,
        nowMs: Long,
    ): SignalContribution
}

/* =================================================================================
 * SCORER 1 — FREQUENCY
 * How often the user plays this song.
 * Raw score uses log-compression so a song played 100 times doesn't dominate
 * a song played 20 times.
 * ================================================================================= */
object FrequencyScorer : Scorer {

    override val signal = ScoringSignal.FREQUENCY
    override val name = "FrequencyScorer"

    /** Play count at which raw score saturates to ~1.0. Tunable. */
    private const val SATURATION_COUNT = 30

    override fun score(song: SongRef, input: EngineInput, nowMs: Long): SignalContribution {
        val playCount = song.playCount

        val rawScore = if (playCount <= 0) {
            0f
        } else {
            // log curve: 1 play → ~0.21, 5 plays → ~0.46, 30 plays → ~1.0
            val compressed = ln(playCount + 1.0) / ln(SATURATION_COUNT + 1.0)
            compressed.toFloat().coerceIn(0f, 1f)
        }

        val confidence = when {
            playCount >= 10 -> 1.0f
            playCount >= 3 -> 0.7f
            playCount >= 1 -> 0.4f
            else -> 0.1f  // never played, but we have the song in library
        }

        val note = when {
            playCount == 0 -> "never played"
            playCount == 1 -> "played once"
            playCount < 10 -> "played $playCount times"
            else -> "played $playCount times (frequent)"
        }

        return SignalContribution(
            signal = signal,
            rawScore = rawScore,
            weight = 0f,  // composer fills
            confidence = confidence,
            weightedScore = 0f,  // composer fills
            note = note,
        )
    }
}

/* =================================================================================
 * SCORER 2 — COMPLETION
 * Does the user typically finish this song or skip it?
 * High completion = positive signal.
 * ================================================================================= */
object CompletionScorer : Scorer {

    override val signal = ScoringSignal.COMPLETION
    override val name = "CompletionScorer"

    override fun score(song: SongRef, input: EngineInput, nowMs: Long): SignalContribution {
        val avgCompletion = song.averageCompletion

        // Confidence is low if we don't have enough plays to judge
        val confidence = when {
            song.playCount >= 5 -> 1.0f
            song.playCount >= 2 -> 0.6f
            song.playCount == 1 -> 0.3f
            else -> 0.0f  // never played → no completion data
        }

        val note = when {
            song.playCount == 0 -> "no completion data"
            avgCompletion >= 0.9f -> "always finished (${(avgCompletion * 100).toInt()}%)"
            avgCompletion >= 0.5f -> "usually finished (${(avgCompletion * 100).toInt()}%)"
            avgCompletion > 0f -> "often skipped (${(avgCompletion * 100).toInt()}% complete)"
            else -> "no completion data"
        }

        return SignalContribution(
            signal = signal,
            rawScore = avgCompletion.coerceIn(0f, 1f),
            weight = 0f,
            confidence = confidence,
            weightedScore = 0f,
            note = note,
        )
    }
}

/* =================================================================================
 * SCORER 3 — SKIP
 * Negative signal — how often does the user skip this song?
 * High skip rate = should be ranked LOWER.
 * Returns rawScore INVERTED (high skip → low score) so the composer can
 * treat it uniformly with other positive signals.
 * ================================================================================= */
object SkipScorer : Scorer {

    override val signal = ScoringSignal.SKIP
    override val name = "SkipScorer"

    override fun score(song: SongRef, input: EngineInput, nowMs: Long): SignalContribution {
        val totalAttempts = song.playCount + song.skipCount
        if (totalAttempts == 0) {
            return SignalContribution(
                signal = signal,
                rawScore = 0.5f,  // neutral — no skip data
                weight = 0f,
                confidence = 0.0f,
                weightedScore = 0f,
                note = "no skip data",
            )
        }

        val skipRate = song.skipCount.toFloat() / totalAttempts
        // Invert: high skip → low score
        val rawScore = (1.0f - skipRate).coerceIn(0f, 1f)

        val confidence = when {
            totalAttempts >= 10 -> 1.0f
            totalAttempts >= 4 -> 0.6f
            totalAttempts >= 2 -> 0.3f
            else -> 0.15f
        }

        val note = when {
            skipRate >= 0.7f -> "skipped often (${(skipRate * 100).toInt()}%)"
            skipRate >= 0.4f -> "skipped sometimes (${(skipRate * 100).toInt()}%)"
            skipRate >= 0.1f -> "rarely skipped (${(skipRate * 100).toInt()}%)"
            else -> "almost never skipped"
        }

        return SignalContribution(
            signal = signal,
            rawScore = rawScore,
            weight = 0f,
            confidence = confidence,
            weightedScore = 0f,
            note = note,
        )
    }
}

/* =================================================================================
 * SCORER 4 — RECENCY
 * How recently was this song played?
 * Uses a half-life decay — a song played 1 day ago scores ~0.8,
 * 7 days ago ~0.4, 30 days ago ~0.1.
 * ================================================================================= */
object RecencyScorer : Scorer {

    override val signal = ScoringSignal.RECENCY
    override val name = "RecencyScorer"

    /** Half-life in milliseconds — 7 days. Tunable. */
    private const val HALF_LIFE_MS = 7L * 24 * 60 * 60 * 1000

    override fun score(song: SongRef, input: EngineInput, nowMs: Long): SignalContribution {
        val lastPlayed = song.lastPlayedAtMs

        if (lastPlayed <= 0L) {
            return SignalContribution(
                signal = signal,
                rawScore = 0f,
                weight = 0f,
                confidence = 0f,
                weightedScore = 0f,
                note = "never played",
            )
        }

        val ageMs = (nowMs - lastPlayed).coerceAtLeast(0L)
        val rawScore = if (ageMs == 0L) {
            1f  // played right now
        } else {
            // Exponential decay: 0.5^(age / halfLife)
            val decay = Math.pow(0.5, ageMs.toDouble() / HALF_LIFE_MS)
            decay.toFloat().coerceIn(0f, 1f)
        }

        val confidence = if (lastPlayed > 0L) 1.0f else 0.0f

        val note = when {
            ageMs < 60 * 60 * 1000 -> "played in the last hour"
            ageMs < 24 * 60 * 60 * 1000 -> "played today"
            ageMs < 7L * 24 * 60 * 60 * 1000 -> "played this week"
            ageMs < 30L * 24 * 60 * 60 * 1000 -> "played this month"
            else -> "played ${(ageMs / (30L * 24 * 60 * 60 * 1000))} months ago"
        }

        return SignalContribution(
            signal = signal,
            rawScore = rawScore,
            weight = 0f,
            confidence = confidence,
            weightedScore = 0f,
            note = note,
        )
    }
}

/* =================================================================================
 * SCORER 5 — FAVORITE
 * Binary-ish signal — is this song favourited?
 * Uses a small recency boost for recently-favourited songs.
 * ================================================================================= */
object FavoriteScorer : Scorer {

    override val signal = ScoringSignal.FAVORITE
    override val name = "FavoriteScorer"

    override fun score(song: SongRef, input: EngineInput, nowMs: Long): SignalContribution {
        if (!song.isFavorite) {
            return SignalContribution(
                signal = signal,
                rawScore = 0f,
                weight = 0f,
                confidence = 1.0f,  // confident — we know it's NOT favorite
                weightedScore = 0f,
                note = "not favourited",
            )
        }

        // Favorited — base score 0.8, with recency boost up to 1.0
        val favoritedAgeMs = (nowMs - song.favoritedAtMs).coerceAtLeast(0L)
        val recentBoost = when {
            favoritedAgeMs < 24 * 60 * 60 * 1000 -> 0.2f  // favourited today
            favoritedAgeMs < 7L * 24 * 60 * 60 * 1000 -> 0.1f  // this week
            else -> 0f
        }
        val rawScore = (0.8f + recentBoost).coerceIn(0f, 1f)

        return SignalContribution(
            signal = signal,
            rawScore = rawScore,
            weight = 0f,
            confidence = 1.0f,
            weightedScore = 0f,
            note = "favourited",
        )
    }
}

/* =================================================================================
 * SCORER 6 — SESSION
 * Is this song part of the current listening session?
 * Strong signal for "Continue Listening" collection.
 * ================================================================================= */
object SessionScorer : Scorer {

    override val signal = ScoringSignal.SESSION
    override val name = "SessionScorer"

    override fun score(song: SongRef, input: EngineInput, nowMs: Long): SignalContribution {
        // Find the most recent ongoing session
        val ongoingSession = input.sessions.lastOrNull { it.isOngoing }
        val recentSession = ongoingSession ?: input.sessions.lastOrNull()

        if (recentSession == null) {
            return SignalContribution(
                signal = signal,
                rawScore = 0f,
                weight = 0f,
                confidence = 0f,
                weightedScore = 0f,
                note = "no session data",
            )
        }

        val playedInSession = recentSession.playedSongIds
        val index = playedInSession.indexOf(song.songId)

        if (index < 0) {
            return SignalContribution(
                signal = signal,
                rawScore = 0f,
                weight = 0f,
                confidence = 0.7f,
                weightedScore = 0f,
                note = "not in current session",
            )
        }

        // Songs played more recently in session get higher score
        val recencyRank = (playedInSession.size - index).toFloat() / playedInSession.size
        val rawScore = (0.5f + 0.5f * recencyRank).coerceIn(0f, 1f)

        val note = when (index) {
            playedInSession.size - 1 -> "last played in session"
            playedInSession.size - 2 -> "second-to-last in session"
            else -> "played at position ${index + 1} in session"
        }

        return SignalContribution(
            signal = signal,
            rawScore = rawScore,
            weight = 0f,
            confidence = 1.0f,
            weightedScore = 0f,
            note = note,
        )
    }
}

/* =================================================================================
 * SCORER 7 — FRESHNESS (BONUS)
 * How recently was this song ADDED to the library?
 * Useful for "Just Added" / discovery collections.
 * ================================================================================= */
object FreshnessScorer : Scorer {

    override val signal = ScoringSignal.FRESHNESS
    override val name = "FreshnessScorer"

    /** Freshness half-life — 14 days. */
    private const val FRESH_HALF_LIFE_MS = 14L * 24 * 60 * 60 * 1000

    override fun score(song: SongRef, input: EngineInput, nowMs: Long): SignalContribution {
        val addedAgeMs = (nowMs - song.dateAddedMs).coerceAtLeast(0L)

        val rawScore = if (addedAgeMs == 0L) {
            1f
        } else {
            val decay = Math.pow(0.5, addedAgeMs.toDouble() / FRESH_HALF_LIFE_MS)
            decay.toFloat().coerceIn(0f, 1f)
        }

        val confidence = 1.0f  // we always know dateAdded

        val note = when {
            addedAgeMs < 24 * 60 * 60 * 1000 -> "added today"
            addedAgeMs < 7L * 24 * 60 * 60 * 1000 -> "added this week"
            addedAgeMs < 30L * 24 * 60 * 60 * 1000 -> "added this month"
            else -> "added ${(addedAgeMs / (30L * 24 * 60 * 60 * 1000))} months ago"
        }

        return SignalContribution(
            signal = signal,
            rawScore = rawScore,
            weight = 0f,
            confidence = confidence,
            weightedScore = 0f,
            note = note,
        )
    }
}

/* =================================================================================
 * SCORER 8 — DIVERSITY (BONUS)
 * Boost underrepresented artists/albums in the candidate set.
 * This is a context-dependent scorer — it needs to know what OTHER items
 * are being considered. Score is computed by the ranker, not per-item.
 * Here we just emit a placeholder contribution.
 * ================================================================================= */
object DiversityScorer : Scorer {

    override val signal = ScoringSignal.DIVERSITY
    override val name = "DiversityScorer"

    override fun score(song: SongRef, input: EngineInput, nowMs: Long): SignalContribution {
        // Diversity is computed at the ranker level (needs full candidate set).
        // Here we emit a neutral contribution that the ranker will replace.
        return SignalContribution(
            signal = signal,
            rawScore = 0.5f,  // neutral
            weight = 0f,
            confidence = 0.0f,  // ranker will fill
            weightedScore = 0f,
            note = "computed by ranker",
        )
    }
}

/* =================================================================================
 * SCORER 9 — MOOD (BONUS)
 * Match between song mood tag and the current context's mood hint.
 * ================================================================================= */
object MoodScorer : Scorer {

    override val signal = ScoringSignal.MOOD
    override val name = "MoodScorer"

    /** Simple mood affinity map — pairs that go well together. */
    private val moodAffinity: Map<String, Set<String>> = mapOf(
        "focus" to setOf("instrumental", "ambient", "classical", "lofi"),
        "evening" to setOf("chill", "jazz", "acoustic", "soul"),
        "morning" to setOf("pop", "indie", "folk", "acoustic"),
        "night" to setOf("ambient", "electronic", "lofi", "classical"),
        "workout" to setOf("electronic", "rock", "hip-hop", "pop"),
        "party" to setOf("pop", "dance", "hip-hop", "electronic"),
        "sleep" to setOf("ambient", "classical", "acoustic", "instrumental"),
    )

    override fun score(song: SongRef, input: EngineInput, nowMs: Long): SignalContribution {
        val hint = input.moodHint?.lowercase()?.trim()
        val timeBucket = input.timeOfDayBucket?.lowercase()?.trim()

        if (hint.isNullOrBlank() && timeBucket.isNullOrBlank()) {
            return SignalContribution(
                signal = signal,
                rawScore = 0f,
                weight = 0f,
                confidence = 0f,
                weightedScore = 0f,
                note = "no mood hint",
            )
        }

        val songMood = song.moodTag.lowercase().trim()
        if (songMood.isBlank()) {
            return SignalContribution(
                signal = signal,
                rawScore = 0.3f,  // neutral-ish — no mood data
                weight = 0f,
                confidence = 0.2f,
                weightedScore = 0f,
                note = "song has no mood tag",
            )
        }

        // Check against hint first, then time-of-day fallback
        val expectedMoods = moodAffinity[hint]
            ?: moodAffinity[timeBucket]
            ?: emptySet()

        val rawScore = when {
            expectedMoods.contains(songMood) -> 1.0f
            expectedMoods.isEmpty() -> 0.3f
            // Partial match — song mood overlaps with adjacent moods
            else -> 0.2f
        }

        val confidence = if (expectedMoods.isEmpty()) 0.2f else 0.8f

        val note = when {
            expectedMoods.contains(songMood) -> "mood '$songMood' matches hint '$hint'"
            else -> "mood '$songMood' does not match hint '$hint'"
        }

        return SignalContribution(
            signal = signal,
            rawScore = rawScore,
            weight = 0f,
            confidence = confidence,
            weightedScore = 0f,
            note = note,
        )
    }
}

/* =================================================================================
 * SCORER 10 — ENTROPY (BONUS)
 * Controlled randomness to break ties and surface variety.
 * Uses a deterministic seed (itemId + collectionStableKey + nowMs / window)
 * so the same input produces the same output within a generation cycle,
 * but different cycles see different entropy.
 * ================================================================================= */
object EntropyScorer : Scorer {

    override val signal = ScoringSignal.ENTROPY
    override val name = "EntropyScorer"

    /** Window size for entropy seed — 1 hour. Same hour = same entropy. */
    private const val SEED_WINDOW_MS = 60L * 60 * 1000

    override fun score(song: SongRef, input: EngineInput, nowMs: Long): SignalContribution {
        // We don't have collectionStableKey here — caller passes via input.
        // Use a hash of itemId + hour bucket.
        val hourBucket = nowMs / SEED_WINDOW_MS
        val seed = stableHash(song.songId, hourBucket.toString())
        val rawScore = (seed % 1000) / 1000f  // 0.0 → 0.999

        val confidence = 1.0f

        return SignalContribution(
            signal = signal,
            rawScore = rawScore,
            weight = 0f,
            confidence = confidence,
            weightedScore = 0f,
            note = "entropy ${(rawScore * 100).toInt()}%",
        )
    }

    /** Stable string hash — deterministic across runs. */
    private fun stableHash(vararg parts: String): Long {
        var h = 0L
        for (p in parts) {
            for (c in p) {
                h = h * 31L + c.code
            }
        }
        return abs(h)
    }
}

/* =================================================================================
 * SCORER REGISTRY
 * ================================================================================= */

/**
 * The complete set of scorers. Strategies query this to know which
 * scorers to run.
 */
object ScorerRegistry {

    /** All scorers, keyed by signal. */
    val ALL: Map<ScoringSignal, Scorer> = mapOf(
        ScoringSignal.FREQUENCY to FrequencyScorer,
        ScoringSignal.COMPLETION to CompletionScorer,
        ScoringSignal.SKIP to SkipScorer,
        ScoringSignal.RECENCY to RecencyScorer,
        ScoringSignal.FAVORITE to FavoriteScorer,
        ScoringSignal.SESSION to SessionScorer,
        ScoringSignal.FRESHNESS to FreshnessScorer,
        ScoringSignal.DIVERSITY to DiversityScorer,
        ScoringSignal.MOOD to MoodScorer,
        ScoringSignal.ENTROPY to EntropyScorer,
    )

    /** Get a scorer by signal. */
    fun forSignal(signal: ScoringSignal): Scorer? = ALL[signal]

    /** Get all scorers needed for a given [ScoreProfile]. */
    fun forProfile(profile: ScoreProfile): List<Scorer> {
        return profile.weights().keys.mapNotNull { ALL[it] }
    }
}
