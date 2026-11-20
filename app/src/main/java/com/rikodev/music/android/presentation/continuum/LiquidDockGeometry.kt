package com.rikodev.music.android.presentation.continuum

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// RIKO CONTINUUM + LIQUID NEXUS — SCULPTED ORGANIC GEOMETRY (P13)
//
// Fixes:
//   1. Floating rounded CONTINUUM wave dock (24dp bottom corners, smooth 26dp
//      rising liquid crest beneath active tab — no harsh square corners!)
//   2. Sleek organic Liquid Nexus expanded chamber (28dp corner radius instead
//      of giant 57dp football oval, with smooth center artwork dome bulge so
//      Left Name/Artist, Center Artwork, Right Controls & Bottom Timeline all
//      sit cleanly INSIDE the glass chamber with zero clipping!)
// ─────────────────────────────────────────────────────────────────────────────

object LiquidDockGeometry {

    /**
     * Sculpted floating CONTINUUM organic wave dock with rounded bottom corners
     * and a smooth elevated liquid crest beneath [activeFraction].
     */
    fun buildContinuumWavePath(
        outPath: Path,
        width: Float,
        height: Float,
        activeFraction: Float,
        crestHeightPx: Float,
        bottomCornerPx: Float,
        organicMode: Boolean
    ) {
        outPath.reset()
        if (width <= 0f || height <= 0f) return

        val clampedFocus = activeFraction.coerceIn(0f, 1f)
        val centerX = width * (1f / 6f + clampedFocus * (4f / 6f))
        val baseTopY = crestHeightPx.coerceIn(0f, height * 0.44f)
        val crestTopY = (baseTopY * 0.08f).coerceAtLeast(2f)
        val topCorner = 22f.coerceAtMost(width * 0.1f)
        val botCorner = bottomCornerPx.coerceIn(0f, height * 0.38f)
        val halfSpan = (width * 0.25f).coerceAtLeast(height * 0.90f)

        val leftFoot = (centerX - halfSpan).coerceAtLeast(topCorner)
        val rightFoot = (centerX + halfSpan).coerceAtMost(width - topCorner)

        outPath.moveTo(0f, height - botCorner)
        outPath.lineTo(0f, baseTopY + topCorner)
        outPath.quadraticTo(0f, baseTopY, topCorner, baseTopY)

        if (!organicMode) {
            if (leftFoot > topCorner) outPath.lineTo(leftFoot, baseTopY)
            outPath.quadraticTo(centerX, crestTopY - 4f, rightFoot, baseTopY)
            if (rightFoot < width - topCorner) outPath.lineTo(width - topCorner, baseTopY)
        } else {
            if (leftFoot > topCorner) outPath.lineTo(leftFoot, baseTopY)
            outPath.cubicTo(
                leftFoot + (centerX - leftFoot) * 0.46f, baseTopY,
                leftFoot + (centerX - leftFoot) * 0.54f, crestTopY,
                centerX, crestTopY
            )
            outPath.cubicTo(
                centerX + (rightFoot - centerX) * 0.46f, crestTopY,
                centerX + (rightFoot - centerX) * 0.54f, baseTopY,
                rightFoot, baseTopY
            )
            if (rightFoot < width - topCorner) outPath.lineTo(width - topCorner, baseTopY)
        }

        outPath.quadraticTo(width, baseTopY, width, baseTopY + topCorner)
        outPath.lineTo(width, height - botCorner)
        outPath.quadraticTo(width, height, width - botCorner, height)
        outPath.lineTo(botCorner, height)
        outPath.quadraticTo(0f, height, 0f, height - botCorner)
        outPath.close()
    }

    /**
     * Secondary softer translucent aurora wave behind the primary CONTINUUM crest.
     */
    fun buildContinuumSecondaryWavePath(
        outPath: Path,
        width: Float,
        height: Float,
        activeFraction: Float,
        crestHeightPx: Float,
        bottomCornerPx: Float,
        phaseRad: Float
    ) {
        outPath.reset()
        if (width <= 0f || height <= 0f) return

        val drift = sin(phaseRad * 0.65f) * width * 0.025f
        val clampedFocus = activeFraction.coerceIn(0f, 1f)
        val centerX = (width * (1f / 6f + clampedFocus * (4f / 6f)) + drift).coerceIn(width * 0.12f, width * 0.88f)
        val baseTopY = (crestHeightPx * 0.90f).coerceIn(0f, height * 0.44f)
        val crestTopY = (crestHeightPx * 0.03f).coerceAtLeast(1f)
        val botCorner = bottomCornerPx.coerceIn(0f, height * 0.38f)
        val halfSpan = (width * 0.33f).coerceAtLeast(height * 1.05f)
        val leftFoot = (centerX - halfSpan).coerceAtLeast(12f)
        val rightFoot = (centerX + halfSpan).coerceAtMost(width - 12f)

        outPath.moveTo(0f, height - botCorner)
        outPath.lineTo(0f, baseTopY + 12f)
        outPath.lineTo(leftFoot, baseTopY + 4f)
        outPath.cubicTo(
            leftFoot + (centerX - leftFoot) * 0.42f, baseTopY + 4f,
            leftFoot + (centerX - leftFoot) * 0.58f, crestTopY,
            centerX, crestTopY
        )
        outPath.cubicTo(
            centerX + (rightFoot - centerX) * 0.42f, crestTopY,
            centerX + (rightFoot - centerX) * 0.58f, baseTopY + 4f,
            rightFoot, baseTopY + 4f
        )
        outPath.lineTo(width, baseTopY + 12f)
        outPath.lineTo(width, height - botCorner)
        outPath.quadraticTo(width, height, width - botCorner, height)
        outPath.lineTo(botCorner, height)
        outPath.quadraticTo(0f, height, 0f, height - botCorner)
        outPath.close()
    }

    /**
     * Luminous top rim stroke of the sculpted CONTINUUM wave dock.
     */
    fun buildContinuumWaveCrestStroke(
        outPath: Path,
        width: Float,
        height: Float,
        activeFraction: Float,
        crestHeightPx: Float,
        organicMode: Boolean
    ) {
        outPath.reset()
        if (width <= 0f || height <= 0f) return

        val clampedFocus = activeFraction.coerceIn(0f, 1f)
        val centerX = width * (1f / 6f + clampedFocus * (4f / 6f))
        val baseTopY = crestHeightPx.coerceIn(0f, height * 0.44f)
        val crestTopY = (baseTopY * 0.08f).coerceAtLeast(2f)
        val topCorner = 22f.coerceAtMost(width * 0.1f)
        val halfSpan = (width * 0.25f).coerceAtLeast(height * 0.90f)
        val leftFoot = (centerX - halfSpan).coerceAtLeast(topCorner)
        val rightFoot = (centerX + halfSpan).coerceAtMost(width - topCorner)

        outPath.moveTo(0f, baseTopY + topCorner)
        outPath.quadraticTo(0f, baseTopY, topCorner, baseTopY)
        if (!organicMode) {
            if (leftFoot > topCorner) outPath.lineTo(leftFoot, baseTopY)
            outPath.quadraticTo(centerX, crestTopY - 4f, rightFoot, baseTopY)
            if (rightFoot < width - topCorner) outPath.lineTo(width - topCorner, baseTopY)
        } else {
            if (leftFoot > topCorner) outPath.lineTo(leftFoot, baseTopY)
            outPath.cubicTo(
                leftFoot + (centerX - leftFoot) * 0.46f, baseTopY,
                leftFoot + (centerX - leftFoot) * 0.54f, crestTopY,
                centerX, crestTopY
            )
            outPath.cubicTo(
                centerX + (rightFoot - centerX) * 0.46f, crestTopY,
                centerX + (rightFoot - centerX) * 0.54f, baseTopY,
                rightFoot, baseTopY
            )
            if (rightFoot < width - topCorner) outPath.lineTo(width - topCorner, baseTopY)
        }
        outPath.quadraticTo(width, baseTopY, width, baseTopY + topCorner)
    }

    /**
     * Sleek organic Liquid Nexus chamber path (28dp rounded corners, NOT a giant
     * 57dp football oval!). Smoothly stretches from compact circle (p=0) to a
     * sculpted liquid glass chamber with a soft dome around the center artwork
     * and gentle organic waist curvature, so Left/Center/Right/Timeline sit
     * comfortably inside the glass boundary with zero text clipping.
     */
    fun buildContinuousNexusPath(
        outPath: Path,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        expandProgress: Float,
        organicMode: Boolean,
        phaseRad: Float,
        energy: Float,
        bassPulse: Float,
        scrubFraction: Float? = null
    ) {
        outPath.reset()
        val w = right - left
        val h = bottom - top
        if (w <= 0f || h <= 0f) return

        val p = expandProgress.coerceIn(0f, 1f)
        // Corner radius transitions from circle (h/2 at p=0) to sleek rounded chamber (h*0.26 at p=1)
        val cornerR = (h * (0.50f - 0.24f * p)).coerceAtMost(w * 0.5f)

        if (!organicMode) {
            outPath.addRoundRect(
                RoundRect(
                    left = left,
                    top = top,
                    right = right,
                    bottom = bottom,
                    cornerRadius = CornerRadius(cornerR, cornerR)
                )
            )
            return
        }

        val cx = (left + right) * 0.5f
        val crownLift = p * h * (0.09f + 0.025f * energy.coerceIn(0f, 1f))
        val waistPinch = (sin(p * PI.toFloat()) * h * 0.08f) + (p * h * 0.025f)
        val scrubX = scrubFraction?.let { left + cornerR + (w - 2f * cornerR) * it.coerceIn(0f, 1f) }
        val scrubLift = if (scrubX != null) h * 0.08f else 0f

        fun topYAt(x: Float): Float {
            val ndx = ((x - cx) / (w * 0.5f).coerceAtLeast(1f)).coerceIn(-1f, 1f)
            val centerDome = exp(-ndx * ndx * 8.5f)
            val waistBell = exp(-((abs(ndx) - 0.52f) * 3.8f).let { it * it })
            val scrubBell = if (scrubX != null) {
                val sdx = (x - scrubX) / (w * 0.12f).coerceAtLeast(1f)
                exp(-sdx * sdx)
            } else 0f
            val breathe = sin(phaseRad + ndx * 2f) * (1.2f + 1.5f * bassPulse.coerceIn(0f, 1f)) * p
            return top - crownLift * centerDome + waistPinch * waistBell - scrubLift * scrubBell + breathe
        }

        fun bottomYAt(x: Float): Float {
            val ndx = ((x - cx) / (w * 0.5f).coerceAtLeast(1f)).coerceIn(-1f, 1f)
            val centerDrop = exp(-ndx * ndx * 7.0f) * p * h * 0.03f
            val waistBell = exp(-((abs(ndx) - 0.52f) * 3.8f).let { it * it })
            return bottom + centerDrop - waistPinch * 0.55f * waistBell
        }

        val innerLeft = left + cornerR
        val innerRight = right - cornerR
        val steps = 16

        // Start at top-left after corner arc
        outPath.moveTo(innerLeft, topYAt(innerLeft))
        // Smooth sculpted top edge across center dome
        for (i in 1..steps) {
            val t = i / steps.toFloat()
            val x = innerLeft + (innerRight - innerLeft) * t
            outPath.lineTo(x, topYAt(x))
        }
        // Top-right corner
        outPath.quadraticTo(right, topYAt(innerRight), right, top + cornerR)
        // Right side
        outPath.lineTo(right, bottom - cornerR)
        // Bottom-right corner
        outPath.quadraticTo(right, bottomYAt(innerRight), innerRight, bottomYAt(innerRight))
        // Smooth sculpted bottom edge
        for (i in (steps - 1) downTo 0) {
            val t = i / steps.toFloat()
            val x = innerLeft + (innerRight - innerLeft) * t
            outPath.lineTo(x, bottomYAt(x))
        }
        // Bottom-left corner
        outPath.quadraticTo(left, bottomYAt(innerLeft), left, bottom - cornerR)
        // Left side
        outPath.lineTo(left, top + cornerR)
        // Top-left corner
        outPath.quadraticTo(left, topYAt(innerLeft), innerLeft, topYAt(innerLeft))
        outPath.close()
    }

    /**
     * Glossy elastic liquid meniscus connecting the floating time bubble cleanly
     * to the TOP rim of the expanded chamber.
     */
    fun buildScrubNeckPath(
        outPath: Path,
        anchorX: Float,
        bubbleBottomY: Float,
        chamberTopRimY: Float,
        neckWidthPx: Float
    ) {
        outPath.reset()
        val halfTop = neckWidthPx * 0.52f
        val halfMid = neckWidthPx * 0.20f
        val halfBot = neckWidthPx * 0.68f
        val midY = (bubbleBottomY + chamberTopRimY) * 0.5f

        outPath.moveTo(anchorX - halfTop, bubbleBottomY - 2f)
        outPath.cubicTo(
            anchorX - halfMid, midY,
            anchorX - halfMid, midY,
            anchorX - halfBot, chamberTopRimY + 4f
        )
        outPath.lineTo(anchorX + halfBot, chamberTopRimY + 4f)
        outPath.cubicTo(
            anchorX + halfMid, midY,
            anchorX + halfMid, midY,
            anchorX + halfTop, bubbleBottomY - 2f
        )
        outPath.close()
    }
}
