package com.rikodev.music.android.presentation.continuum

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// RIKO CONTINUUM + LIQUID NEXUS — ORGANIC LIQUID GEOMETRY (P2, P3, P4, P6, P11)
//
// Zero-allocation reusable Path builders:
//   1. CONTINUUM dual-layer organic wave navbar (primary crest + secondary
//      counter-flowing liquid wave behind it for true depth)
//   2. Continuous 0..1 morph path: smoothly blends 12-lobe organic breathing
//      blob (p=0) -> elastic stretching liquid peanut (p=0.3..0.6) ->
//      organic horizontal player chamber (p=1) with ZERO visual pop/jump
//   3. Elastic liquid meniscus neck connecting floating time bubble to the
//      top rim of the chamber
// ─────────────────────────────────────────────────────────────────────────────

object LiquidDockGeometry {

    /**
     * Primary CONTINUUM organic wave path: elevated liquid crest beneath
     * [activeFraction] (0f = Listen, 0.5f = Explore, 1f = Library).
     */
    fun buildContinuumWavePath(
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
        val baseTopY = crestHeightPx.coerceIn(0f, height * 0.45f)
        val crestTopY = (baseTopY * 0.10f).coerceAtLeast(2f)
        val halfSpan = (width * 0.26f).coerceAtLeast(height * 0.95f)

        val leftFoot = (centerX - halfSpan).coerceAtLeast(0f)
        val rightFoot = (centerX + halfSpan).coerceAtMost(width)

        outPath.moveTo(0f, height)
        outPath.lineTo(0f, baseTopY + 10f)
        outPath.quadraticTo(0f, baseTopY, 20f, baseTopY)

        if (!organicMode) {
            if (leftFoot > 20f) outPath.lineTo(leftFoot, baseTopY)
            outPath.quadraticTo(centerX, crestTopY - 4f, rightFoot, baseTopY)
            if (rightFoot < width - 20f) outPath.lineTo(width - 20f, baseTopY)
        } else {
            if (leftFoot > 20f) outPath.lineTo(leftFoot, baseTopY)
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
            if (rightFoot < width - 20f) outPath.lineTo(width - 20f, baseTopY)
        }

        outPath.quadraticTo(width, baseTopY, width, baseTopY + 10f)
        outPath.lineTo(width, height)
        outPath.close()
    }

    /**
     * Secondary softer translucent liquid wave flowing behind the primary
     * CONTINUUM crest to give the layered aurora depth seen in the reference.
     */
    fun buildContinuumSecondaryWavePath(
        outPath: Path,
        width: Float,
        height: Float,
        activeFraction: Float,
        crestHeightPx: Float,
        phaseRad: Float
    ) {
        outPath.reset()
        if (width <= 0f || height <= 0f) return

        val drift = sin(phaseRad * 0.6f) * width * 0.03f
        val clampedFocus = activeFraction.coerceIn(0f, 1f)
        val centerX = (width * (1f / 6f + clampedFocus * (4f / 6f)) + drift).coerceIn(width * 0.1f, width * 0.9f)
        val baseTopY = (crestHeightPx * 0.88f).coerceIn(0f, height * 0.45f)
        val crestTopY = (crestHeightPx * 0.04f).coerceAtLeast(1f)
        val halfSpan = (width * 0.34f).coerceAtLeast(height * 1.1f)
        val leftFoot = (centerX - halfSpan).coerceAtLeast(0f)
        val rightFoot = (centerX + halfSpan).coerceAtMost(width)

        outPath.moveTo(0f, height)
        outPath.lineTo(0f, baseTopY + 8f)
        if (leftFoot > 0f) outPath.lineTo(leftFoot, baseTopY + 4f)
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
        if (rightFoot < width) outPath.lineTo(width, baseTopY + 8f)
        outPath.lineTo(width, height)
        outPath.close()
    }

    /**
     * Luminous top rim stroke of the primary CONTINUUM wave navbar.
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
        val baseTopY = crestHeightPx.coerceIn(0f, height * 0.45f)
        val crestTopY = (baseTopY * 0.10f).coerceAtLeast(2f)
        val halfSpan = (width * 0.26f).coerceAtLeast(height * 0.95f)
        val leftFoot = (centerX - halfSpan).coerceAtLeast(0f)
        val rightFoot = (centerX + halfSpan).coerceAtMost(width)

        outPath.moveTo(0f, baseTopY + 10f)
        outPath.quadraticTo(0f, baseTopY, 20f, baseTopY)
        if (!organicMode) {
            if (leftFoot > 20f) outPath.lineTo(leftFoot, baseTopY)
            outPath.quadraticTo(centerX, crestTopY - 4f, rightFoot, baseTopY)
            if (rightFoot < width - 20f) outPath.lineTo(width - 20f, baseTopY)
        } else {
            if (leftFoot > 20f) outPath.lineTo(leftFoot, baseTopY)
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
            if (rightFoot < width - 20f) outPath.lineTo(width - 20f, baseTopY)
        }
        outPath.quadraticTo(width, baseTopY, width, baseTopY + 10f)
    }

    /**
     * Continuous, discontinuity-free organic morph path for Liquid Nexus (p in 0..1).
     * Samples 24 smooth perimeter control points around a super-ellipse / stadium
     * contour that seamlessly transitions from:
     *   - p = 0.0 : organic breathing 12-lobe liquid blob around right-side artwork
     *   - p = 0.4 : horizontally stretching elastic liquid peanut
     *   - p = 1.0 : full organic player chamber with center artwork crown & optional
     *               localized scrub deformation
     * Zero abrupt path switching at any frame.
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
        val r = (h * 0.5f).coerceAtMost(w * 0.5f)

        if (!organicMode) {
            outPath.addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = left,
                    top = top,
                    right = right,
                    bottom = bottom,
                    radiusX = r,
                    radiusY = r
                )
            )
            return
        }

        val cx = (left + right) * 0.5f
        val cy = (top + bottom) * 0.5f
        val halfStraight = ((w - 2f * r) * 0.5f).coerceAtLeast(0f)

        // Idle organic wobble fades smoothly as expansion progresses, replaced by
        // elastic mid-morph waist pinch and gentle center artwork crown.
        val idleWeight = (1f - p * 1.15f).coerceIn(0f, 1f)
        val idleAmp = (0.042f + 0.062f * energy.coerceIn(0f, 1f) + 0.032f * bassPulse.coerceIn(0f, 1f)) * r * idleWeight
        val midMorphPinch = sin(p * PI.toFloat()) * h * 0.11f
        val crownLift = p * h * (0.055f + 0.025f * energy.coerceIn(0f, 1f))
        val scrubX = scrubFraction?.let { left + r + (w - 2f * r) * it.coerceIn(0f, 1f) }

        val samples = 24
        val xs = FloatArray(samples)
        val ys = FloatArray(samples)

        for (i in 0 until samples) {
            val theta = (2.0 * PI * i / samples).toFloat()
            val cosT = cos(theta)
            val sinT = sin(theta)

            // Base stadium perimeter point: smoothly degenerates to a pure circle when halfStraight == 0
            val baseX = cx + halfStraight * (if (cosT >= 0f) 1f else -1f) * kotlin.math.abs(cosT).let { kotlin.math.sqrt(it) } + r * cosT
            // Keep X clamped cleanly within [left, right]
            val stadiumX = if (halfStraight > 0.5f) {
                val capSign = if (cosT >= 0f) 1f else -1f
                val capCenter = cx + capSign * halfStraight
                val t = kotlin.math.abs(cosT)
                // Smoothly blend straight horizontal segment & semicircular end caps
                if (t < 0.55f) {
                    cx + (cosT / 0.55f) * halfStraight
                } else {
                    val capAngle = ((t - 0.55f) / 0.45f) * (PI.toFloat() * 0.5f)
                    capCenter + capSign * r * sin(capAngle)
                }
            } else {
                cx + r * cosT
            }

            val stadiumY = if (halfStraight > 0.5f) {
                val t = kotlin.math.abs(cosT)
                if (t < 0.55f) {
                    cy + (if (sinT >= 0f) 1f else -1f) * r
                } else {
                    val capAngle = ((t - 0.55f) / 0.45f) * (PI.toFloat() * 0.5f)
                    cy + (if (sinT >= 0f) 1f else -1f) * r * cos(capAngle)
                }
            } else {
                cy + r * sinT
            }

            // 1. Organic breathing harmonics (strongest in compact idle dock)
            val wobble = idleAmp * (sin(theta * 2f + phaseRad) * 0.55f + cos(theta * 3f - phaseRad * 1.35f) * 0.45f)

            // 2. Center proximity factor (1 at horizontal center, 0 at ends)
            val normDx = ((stadiumX - cx) / (w * 0.5f).coerceAtLeast(1f)).coerceIn(-1f, 1f)
            val centerBell = kotlin.math.exp(-normDx * normDx * 5.5f)
            val waistBell = kotlin.math.exp(-((kotlin.math.abs(normDx) - 0.45f) * 3.8f).let { it * it })

            // 3. Top center crown + mid-morph elastic waist pinch
            val isTop = sinT < 0f
            val crownOffset = if (isTop) -crownLift * centerBell else 0f
            val pinchOffset = (if (isTop) 1f else -1f) * midMorphPinch * waistBell

            // 4. Localized scrub meniscus swell toward the active finger X
            val scrubOffset = if (scrubX != null) {
                val sdx = ((stadiumX - scrubX) / (w * 0.14f).coerceAtLeast(1f))
                val sBell = kotlin.math.exp(-sdx * sdx)
                if (isTop) -h * 0.08f * sBell else h * 0.05f * sBell
            } else 0f

            xs[i] = stadiumX + wobble * cosT
            ys[i] = stadiumY + wobble * sinT + crownOffset + pinchOffset + scrubOffset
        }

        val startMidX = (xs[samples - 1] + xs[0]) * 0.5f
        val startMidY = (ys[samples - 1] + ys[0]) * 0.5f
        outPath.moveTo(startMidX, startMidY)
        for (i in 0 until samples) {
            val next = (i + 1) % samples
            val midX = (xs[i] + xs[next]) * 0.5f
            val midY = (ys[i] + ys[next]) * 0.5f
            outPath.quadraticTo(xs[i], ys[i], midX, midY)
        }
        outPath.close()
    }

    /**
     * Short, glossy elastic liquid meniscus connecting the floating time bubble
     * cleanly to the TOP liquid rim of the expanded chamber (never cutting across
     * the album art or song metadata).
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
        val halfBot = neckWidthPx * 0.65f
        val midY = (bubbleBottomY + chamberTopRimY) * 0.5f

        outPath.moveTo(anchorX - halfTop, bubbleBottomY - 2f)
        outPath.cubicTo(
            anchorX - halfMid, midY,
            anchorX - halfMid, midY,
            anchorX - halfBot, chamberTopRimY + 3f
        )
        outPath.lineTo(anchorX + halfBot, chamberTopRimY + 3f)
        outPath.cubicTo(
            anchorX + halfMid, midY,
            anchorX + halfMid, midY,
            anchorX + halfTop, bubbleBottomY - 2f
        )
        outPath.close()
    }
}
