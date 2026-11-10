package com.rikodev.music.android.core.experience.wave

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — BEZIER CONTROL
//
// Cubic Bezier control points for one segment of the ribbon.
//
// For each pair of adjacent RibbonPoints, there is exactly one BezierControl
// that defines how the curve should be drawn between them.
//
// The control points define the cubic Bezier:
//   B(t) = (1-t)³P1 + 3(1-t)²tCP1 + 3(1-t)t²CP2 + t³P2
//
// The [tension] field controls how tight or loose the curve is.
//   0.0 = very tight (almost straight)
//   0.5 = default Catmull-Rom smoothness
//   1.0 = very loose (wide, flowing curves)
//
// NO DRAWING. This is pure geometry. The Canvas Renderer (3.4)
// reads these control points to build actual android.graphics.Path objects.
//
// PRODUCER: Organic Path Generator (Step 3.3E) — Catmull-Rom → Bezier conversion
// CONSUMER: Canvas Renderer (Phase 3.4) — builds Path from control points
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Cubic Bezier control points for one ribbon segment.
 *
 * Given two adjacent RibbonPoints P1 and P2:
 *   - cp1X/cp1Y = first control point (near P1)
 *   - cp2X/cp2Y = second control point (near P2)
 *   - tension = how much the curve "pulls" toward the control points
 */
data class BezierControl(

    /** First control point X (near the start of this segment). */
    val cp1X: Float = 0f,

    /** First control point Y (near the start of this segment). */
    val cp1Y: Float = 0f,

    /** Second control point X (near the end of this segment). */
    val cp2X: Float = 0f,

    /** Second control point Y (near the end of this segment). */
    val cp2Y: Float = 0f,

    /**
     * Tension factor 0f..1f. How tight/loose this curve segment is.
     * Affects how far the control points pull from the base line.
     */
    val tension: Float = 0.5f
)
