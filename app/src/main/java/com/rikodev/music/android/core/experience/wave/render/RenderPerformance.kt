package com.rikodev.music.android.core.experience.wave.render

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — RENDER PERFORMANCE LAYER
//
// Zero-allocation-per-frame utilities for the render pipeline.
//
// ── PHILOSOPHY ──────────────────────────────────────────────────────────────
//   At 60fps, the render pipeline has ~16ms per frame.
//   Every allocation is a GC pause. Every GC pause is a dropped frame.
//   The goal: zero heap allocations during the draw pass.
//
//   This layer provides:
//   1. Pre-allocated Path pool — reuse Path objects across frames
//   2. Pre-allocated Brush cache — reuse gradient brushes when colors match
//   3. Pre-allocated Matrix — reuse transformation matrices
//   4. FloatArray reuse — avoid creating new arrays per frame
//
// ── RULES ──────────────────────────────────────────────────────────────────
//   #1: All objects are pre-allocated at initialization or first use.
//   #2: Paths are reset before reuse (Path.reset()).
//   #3: Brushes are recreated only when color inputs change.
//   #4: No allocations in the hot path (draw calls).
//   #5: Object pool has a maximum size — excess objects are recycled.
//
// PRODUCER: Phase 3.4G
// CONSUMER: RibbonRenderer, GlassRibbonRenderer, HighlightRenderer, GlowRenderer
//
// ⭐ NOTE ⭐
//   The renderers already use object-level reusable paths (ribbonPath,
//   edgeLightPath, etc.). This layer adds systematic pooling and caching.
// ─────────────────────────────────────────────────────────────────────────────

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import kotlin.math.min
import androidx.compose.animation.core.Animatable

/**
 * Performance utilities for the render pipeline.
 *
 * Provides pre-allocated object pools and caching mechanisms
 * to minimize heap allocations during the draw pass.
 */
object RenderPerformance {

    // ── Path Pool ──────────────────────────────────────────────────────────

    /**
     * Maximum number of Path objects in the pool.
     * Enough for: ribbon + glass(frost/reflection/sheen) + highlight(specular/fresnel)
     * + glow(shadow/beat) + debug overlay = ~8 paths. Pool of 12 provides headroom.
     */
    private const val PATH_POOL_SIZE = 12

    /** Pre-allocated pool of Path objects. */
    private val pathPool = Array(PATH_POOL_SIZE) { Path() }

    /** Track which paths are currently in use. */
    private val pathInUse = BooleanArray(PATH_POOL_SIZE) { false }

    /**
     * Acquire a Path from the pool. Reset before returning.
     *
     * @return A clean Path ready for use, or a new Path if pool is exhausted.
     */
    fun acquirePath(): Path {
        for (i in 0 until PATH_POOL_SIZE) {
            if (!pathInUse[i]) {
                pathInUse[i] = true
                pathPool[i].reset()
                return pathPool[i]
            }
        }
        // Pool exhausted — create a temporary (will be GC'd, but rare)
        return Path()
    }

    /**
     * Release a Path back to the pool.
     *
     * @param path The Path to release. If it's a pool object, it's reset and recycled.
     *             If it's a temporary (not from pool), it's simply ignored.
     */
    fun releasePath(path: Path) {
        path.reset()
        for (i in 0 until PATH_POOL_SIZE) {
            if (pathPool[i] === path) {
                pathInUse[i] = false
                return
            }
        }
        // Not a pool object — let it be GC'd
    }

    // ── FloatArray Pool ──────────────────────────────────────────────────

    /**
     * Maximum number of pre-allocated FloatArrays.
     * Used for half-thickness, alpha, and other per-point computations.
     */
    private const val ARRAY_POOL_SIZE = 8

    /** Pre-allocated FloatArrays of maximum expected size (96 points = ULTRA). */
    private val arrayPool = Array(ARRAY_POOL_SIZE) { FloatArray(96) }

    private val arrayInUse = BooleanArray(ARRAY_POOL_SIZE) { false }

    /**
     * Acquire a FloatArray from the pool.
     * The returned array is zeroed and ready for use.
     *
     * @param size Required minimum size. If larger than pool array size,
     *             a new array is allocated (rare — only for quality > ULTRA).
     */
    fun acquireArray(size: Int): FloatArray {
        for (i in 0 until ARRAY_POOL_SIZE) {
            if (!arrayInUse[i]) {
                arrayInUse[i] = true
                arrayPool[i].fill(0f, 0, min(size, arrayPool[i].size))
                return arrayPool[i]
            }
        }
        return FloatArray(size)
    }

    /**
     * Release a FloatArray back to the pool.
     */
    fun releaseArray(array: FloatArray) {
        for (i in 0 until ARRAY_POOL_SIZE) {
            if (arrayPool[i] === array) {
                arrayInUse[i] = false
                return
            }
        }
    }

    // ── Draw Timing ──────────────────────────────────────────────────────

    /**
     * Simple frame timing tracker for render diagnostics.
     * Tracks draw time per layer for performance monitoring.
     */
    class DrawTimer {
        private val layerTimesNs = LongArray(MAX_LAYERS)
        private val layerNames = arrayOf(
            "glow", "ribbon", "glass", "highlight", "overlay"
        )

        /** Begin timing a layer. Returns the start timestamp. */
        fun beginLayer(): Long = System.nanoTime()

        /**
         * End timing a layer and record the duration.
         *
         * @param layerIndex The layer index (0=glow, 1=ribbon, 2=glass, 3=highlight, 4=overlay)
         * @param startTimeNs The timestamp from [beginLayer]
         */
        fun endLayer(layerIndex: Int, startTimeNs: Long) {
            if (layerIndex in 0 until MAX_LAYERS) {
                layerTimesNs[layerIndex] = System.nanoTime() - startTimeNs
            }
        }

        /** Get the total draw time in nanoseconds. */
        val totalTimeNs: Long
            get() = layerTimesNs.sum()

        /** Get the total draw time in milliseconds. */
        val totalTimeMs: Float
            get() = totalTimeNs / 1_000_000f

        /** Get individual layer times as name→nanoseconds pairs. */
        fun getLayerTimes(): List<Pair<String, Long>> {
            return layerNames.mapIndexed { i, name ->
                name to layerTimesNs[i]
            }
        }

        companion object {
            const val MAX_LAYERS = 5
        }
    }

    // ── Reusable Matrix ───────────────────────────────────────────────────

    /**
     * Reusable Matrix for canvas transformations.
     * Reset before each use via matrix.reset().
     */
    val sharedMatrix: Matrix
        get() {
            reusableMatrix.reset()
            return reusableMatrix
        }

    private val reusableMatrix = Matrix()
}
