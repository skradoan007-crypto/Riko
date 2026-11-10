/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B3 — Collection Engine
 *
 * File 9 / 14 — CollectionCache.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * The in-memory cache that stores generated collection snapshots
 * between engine cycles.
 *
 * Why a cache?
 *   - Library generation is expensive (scoring + ranking hundreds
 *     of items across multiple collections)
 *   - Most cycles don't change the underlying data
 *   - The UI wants instant results on foreground
 *
 * The cache is IN-MEMORY ONLY. No disk persistence here — that's
 * the host's responsibility (and only if it wants to). The cache
 * is rebuilt from scratch on cold start.
 *
 * DESIGN
 * ------
 * The cache is a single mutable map, but ALL public operations
 * return immutable copies. Internal state is mutated only inside
 * synchronized blocks.
 *
 * The cache NEVER:
 *   • Reads wall-clock time (caller passes nowMs)
 *   • Touches Android / IO
 *   • Persists to disk
 *   • Knows about strategies or scorers
 *
 * The cache DOES:
 *   • Store [CacheEntry]s keyed by stableKey
 *   • Apply TTL on read (return stale or drop)
 *   • Apply invalidation rules on trigger
 *   • Track hit/miss counts for diagnostics
 *
 * RULES ENFORCED HERE
 * -------------------
 * #42  Diagnostics Is First-Class — cache state is observable
 * #44  Stable Key Never Changes — all keys are stableKey strings
 *
 * FORBIDDEN IN THIS FILE
 * ----------------------
 * No Android, no IO, no disk, no time reads.
 *
 * © RIKO Experience OS — Engine Tier
 */

package com.rikodev.music.android.presentation.library.engine.cache

import com.rikodev.music.android.presentation.library.engine.ranking.RankedItem
import com.rikodev.music.android.presentation.library.engine.diagnostics.EngineWarning
import java.util.concurrent.ConcurrentHashMap
import androidx.compose.animation.core.Animatable

/**
 * The cache status for a single entry, derived from TTL rules.
 *
 * Maps to [com.rikodev.music.android.presentation.library.contract.DiagnosticCacheStatus]
 * at the contract layer.
 */
enum class CacheEntryStatus(val internalId: String) {
    /** Within soft TTL — fresh, no refresh needed. */
    FRESH(internalId = "fresh"),

    /** Past soft TTL but within hard TTL — serve + refresh in background. */
    STALE(internalId = "stale"),

    /** Past hard TTL — drop and regenerate. */
    EXPIRED(internalId = "expired"),

    /** Caching disabled for this collection — always regenerate. */
    DISABLED(internalId = "disabled"),

    /** No entry exists for this key. */
    ABSENT(internalId = "absent");
}

/**
 * A single cached entry.
 *
 * Holds the ranked items + the input "fingerprint" so the cache
 * can detect when the underlying data has changed.
 */
data class CacheEntry(
    /** Stable key — matches the collection. Rule #44. */
    val stableKey: String,
    /** Wall-clock epoch ms when this entry was created. */
    val createdAtMs: Long,
    /** Wall-clock epoch ms when this entry was last accessed. */
    val lastAccessedAtMs: Long,
    /** Number of times this entry has been served. */
    val hitCount: Int,
    /** Fingerprint of the input data used to generate this entry. */
    val inputFingerprint: String,
    /** The ranked items, in display order. */
    val items: List<RankedItem>,
    /** Total items considered during generation (may be > items.size). */
    val totalCandidates: Int,
    /** Number of warnings emitted during generation. */
    val warningCount: Int,
) {
    val itemCount: Int get() = items.size
}

/**
 * A snapshot of the entire cache state — for diagnostics.
 */
data class CacheSnapshot(
    /** Total entries in the cache. */
    val entryCount: Int,
    /** Total hits across all entries. */
    val totalHits: Int,
    /** Total misses (read attempted, no entry). */
    val totalMisses: Int,
    /** Total invalidations since engine start. */
    val totalInvalidations: Int,
    /** Per-entry summary, keyed by stableKey. */
    val entries: Map<String, CacheEntrySummary>,
    /** Wall-clock epoch ms when this snapshot was taken. */
    val snapshotAtMs: Long,
) {
    val hitRate: Float
        get() {
            val total = totalHits + totalMisses
            return if (total == 0) 0f else totalHits.toFloat() / total
        }
}

/**
 * Per-entry summary for diagnostics.
 */
data class CacheEntrySummary(
    val stableKey: String,
    val status: CacheEntryStatus,
    val createdAtMs: Long,
    val lastAccessedAtMs: Long,
    val hitCount: Int,
    val itemCount: Int,
)

/**
 * The result of a cache read.
 */
sealed class CacheReadResult {

    /** Cache hit — fresh entry, served immediately. */
    data class Fresh(
        val entry: CacheEntry,
    ) : CacheReadResult()

    /** Cache hit but stale — served + refresh recommended. */
    data class Stale(
        val entry: CacheEntry,
        val ageMs: Long,
    ) : CacheReadResult()

    /** Cache miss — no entry or expired. */
    data object Miss : CacheReadResult()

    /** Caching disabled for this key. */
    data object Disabled : CacheReadResult()
}

/**
 * The in-memory collection cache.
 *
 * Thread-safe via [ConcurrentHashMap]. All public operations are
 * safe to call from any thread.
 */
class CollectionCache(
    private val policy: CachePolicy = CachePolicy.PRODUCTION,
) {

    private val entries = ConcurrentHashMap<String, CacheEntry>()
    private val hitCounts = ConcurrentHashMap<String, Int>()
    private val missCounts = ConcurrentHashMap<String, Int>()

    @Volatile private var totalInvalidations: Int = 0

    /**
     * Read an entry from the cache.
     *
     * @param stableKey  The collection's stable key. Rule #44.
     * @param nowMs      Wall-clock "now" (passed in, never read by cache).
     * @return           The read result.
     */
    fun read(stableKey: String, nowMs: Long): CacheReadResult {
        val ttl = policy.ttlFor(stableKey)
        if (ttl.disabled) {
            incrementMiss(stableKey)
            return CacheReadResult.Disabled
        }

        val entry = entries[stableKey]
        if (entry == null) {
            incrementMiss(stableKey)
            return CacheReadResult.Miss
        }

        val ageMs = nowMs - entry.createdAtMs
        return when {
            ageMs <= ttl.softTtlMs -> {
                incrementHit(stableKey)
                // Update last accessed
                entries[stableKey] = entry.copy(lastAccessedAtMs = nowMs)
                CacheReadResult.Fresh(entry)
            }
            ageMs <= ttl.hardTtlMs -> {
                incrementHit(stableKey)
                entries[stableKey] = entry.copy(lastAccessedAtMs = nowMs)
                CacheReadResult.Stale(entry, ageMs)
            }
            else -> {
                // Expired — drop and miss
                entries.remove(stableKey)
                incrementMiss(stableKey)
                CacheReadResult.Miss
            }
        }
    }

    /**
     * Write an entry to the cache.
     */
    fun write(
        stableKey: String,
        items: List<RankedItem>,
        totalCandidates: Int,
        warningCount: Int,
        inputFingerprint: String,
        nowMs: Long,
    ) {
        val ttl = policy.ttlFor(stableKey)
        if (ttl.disabled) return  // Don't cache disabled collections

        val entry = CacheEntry(
            stableKey = stableKey,
            createdAtMs = nowMs,
            lastAccessedAtMs = nowMs,
            hitCount = 0,
            inputFingerprint = inputFingerprint,
            items = items,
            totalCandidates = totalCandidates,
            warningCount = warningCount,
        )
        entries[stableKey] = entry
    }

    /**
     * Invalidate entries based on a trigger.
     *
     * @return The list of stableKeys that were invalidated.
     */
    fun invalidate(trigger: InvalidationTrigger, nowMs: Long): List<String> {
        val rule = policy.ruleFor(trigger) ?: return emptyList()
        val invalidated = mutableListOf<String>()

        if (rule.invalidateAll) {
            invalidated.addAll(entries.keys)
            entries.clear()
            totalInvalidations += invalidated.size
            return invalidated
        }

        for (key in rule.invalidateImmediately) {
            if (entries.remove(key) != null) {
                invalidated.add(key)
            }
        }
        // Lazy invalidation: mark entries stale by adjusting their createdAtMs
        for (key in rule.invalidateLazily) {
            val entry = entries[key]
            if (entry != null) {
                // Push createdAt back to make entry stale
                val ttl = policy.ttlFor(key)
                entries[key] = entry.copy(createdAtMs = nowMs - ttl.softTtlMs - 1)
                // Don't add to invalidated list — entry is still served (stale)
            }
        }

        totalInvalidations += invalidated.size
        return invalidated
    }

    /**
     * Drop a single entry by stableKey.
     */
    fun drop(stableKey: String): Boolean {
        return entries.remove(stableKey) != null
    }

    /**
     * Clear all entries.
     */
    fun clear() {
        val count = entries.size
        entries.clear()
        hitCounts.clear()
        missCounts.clear()
        totalInvalidations += count
    }

    /**
     * Take a snapshot of the cache state for diagnostics.
     */
    fun snapshot(nowMs: Long): CacheSnapshot {
        val summaries = entries.mapValues { (key, entry) ->
            val ttl = policy.ttlFor(key)
            val ageMs = nowMs - entry.createdAtMs
            val status = when {
                ttl.disabled -> CacheEntryStatus.DISABLED
                ageMs <= ttl.softTtlMs -> CacheEntryStatus.FRESH
                ageMs <= ttl.hardTtlMs -> CacheEntryStatus.STALE
                else -> CacheEntryStatus.EXPIRED
            }
            CacheEntrySummary(
                stableKey = key,
                status = status,
                createdAtMs = entry.createdAtMs,
                lastAccessedAtMs = entry.lastAccessedAtMs,
                hitCount = entry.hitCount + (hitCounts[key] ?: 0),
                itemCount = entry.itemCount,
            )
        }

        val totalHits = hitCounts.values.sum()
        val totalMisses = missCounts.values.sum()

        return CacheSnapshot(
            entryCount = entries.size,
            totalHits = totalHits,
            totalMisses = totalMisses,
            totalInvalidations = totalInvalidations,
            entries = summaries,
            snapshotAtMs = nowMs,
        )
    }

    /**
     * Generate warnings for any entry served stale past hard TTL.
     * Called by the engine after each cycle for diagnostics.
     */
    fun collectWarnings(nowMs: Long): List<EngineWarning> {
        val warnings = mutableListOf<EngineWarning>()
        for ((key, entry) in entries) {
            val ttl = policy.ttlFor(key)
            val ageMs = nowMs - entry.createdAtMs
            if (ageMs > ttl.hardTtlMs && !ttl.disabled) {
                warnings.add(
                    EngineWarning.CacheServedStale(
                        stableKey = key,
                        ageMs = ageMs,
                        hardTtlMs = ttl.hardTtlMs,
                    )
                )
            }
        }
        return warnings
    }

    /* ─── Internal helpers ──────────────────────────────────────────── */

    private fun incrementHit(stableKey: String) {
        hitCounts[stableKey] = (hitCounts[stableKey] ?: 0) + 1
    }

    private fun incrementMiss(stableKey: String) {
        missCounts[stableKey] = (missCounts[stableKey] ?: 0) + 1
    }
}
