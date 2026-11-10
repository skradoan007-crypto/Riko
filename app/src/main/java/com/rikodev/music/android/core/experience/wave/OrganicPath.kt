package com.rikodev.music.android.core.experience.wave

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — ORGANIC PATH
//
// The complete geometric representation of the Organic Energy Ribbon™.
//
// This is the output of the Path Generator (Step 3.3E) and the input
// to the Highlight Solver (3.3F) and Canvas Renderer (3.4).
//
// An OrganicPath contains everything needed to draw the ribbon:
//   - RibbonPoints: the ribbon's center line with full per-point metadata
//   - BezierControls: cubic Bezier segments connecting each pair of points
//   - Geometric analysis: total length, average curvature, bounding box
//
// INVARIANTS:
//   - Immutable — replaced entirely each frame, never modified
//   - points.size == controls.size + 1 (N points → N-1 segments → N-1 controls)
//   - All x values are monotonically increasing (left to right)
//   - All points have valid normals (normalX² + normalY² ≈ 1.0)
//
// PRODUCER: Organic Path Generator (Step 3.3E)
// CONSUMER: Highlight Solver (3.3F), Canvas Renderer (Phase 3.4), Glass
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Bounding box of the ribbon path.
 * Used by the renderer for culling and the glass engine for positioning.
 */
data class BoundingBox(
    val minX: Float = 0f,
    val minY: Float = 0f,
    val maxX: Float = 1f,
    val maxY: Float = 1f
) {
    /** Width of the bounding box. */
    val width: Float get() = maxX - minX

    /** Height of the bounding box. */
    val height: Float get() = maxY - minY

    /** Center X of the bounding box. */
    val centerX: Float get() = (minX + maxX) * 0.5f

    /** Center Y of the bounding box. */
    val centerY: Float get() = (minY + maxY) * 0.5f
}

/**
 * The complete geometric path of the ribbon for one frame.
 *
 * This is a frozen, immutable snapshot. The Path Generator creates a new
 * OrganicPath every frame. Consumers read from it but never modify it.
 */
data class OrganicPath(

    // ── Geometry ────────────────────────────────────────────────────────

    /**
     * The ribbon's center line points with full per-point metadata.
     * Ordered left to right (x is monotonically increasing).
     * Size is determined by WaveQuality.segmentCount.
     */
    val points: List<RibbonPoint> = emptyList(),

    /**
     * Cubic Bezier control points for each segment between consecutive points.
     * controls.size == points.size - 1
     * controls[i] defines the curve from points[i] to points[i+1].
     */
    val controls: List<BezierControl> = emptyList(),

    // ── Analysis ────────────────────────────────────────────────────────

    /** Total arc length of the ribbon center line (sum of segment lengths). */
    val totalLength: Float = 0f,

    /** Average curvature across all points. 0 = straight, higher = more bendy. */
    val averageCurvature: Float = 0f,

    /** Axis-aligned bounding box of all points. */
    val boundingBox: BoundingBox = BoundingBox(),

    // ── Metadata ──────────────────────────────────────────────────────────

    /** Quality level used to generate this path. */
    val quality: WaveQuality = WaveQuality.BALANCED,

    /** Frame timestamp (System.nanoTime()). */
    val timestamp: Long = 0L

) {
    /** Number of points in this path. */
    val pointCount: Int get() = points.size

    /** Number of segments (curves) in this path. */
    val segmentCount: Int get() = controls.size

    /** True if the path has no points (idle/empty state). */
    val isEmpty: Boolean get() = points.isEmpty()

    /** True if the path has meaningful geometry. */
    val hasGeometry: Boolean get() = points.size >= 2

    companion object {
        /** Pre-built empty path for idle state. */
        val EMPTY = OrganicPath()
    }
}
