package com.rikodev.music.android.presentation.continuum

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// RIKO LIQUID NEXUS — EXPRESSIVE MORPHING ARTWORK MASK (P12)
//
// Directly masks/clips the album artwork ITSELF (matching Radoan's reference
// video: rounded pentagon <-> 10-petal scalloped cookie <-> soft pill/squircle
// <-> 12-petal cookie <-> 8-point soft starburst).
//
// When [morphIntensity] transitions 1f -> 0f (on Expand tap), the artwork
// smoothly morphs from the active organic shape into a pure crisp Circle.
// ─────────────────────────────────────────────────────────────────────────────

class MorphingLiquidArtworkShape(
    /** Continuous cycle phase (0f..5f wraps through all 5 expressive shapes). */
    private val morphCycle: Float,
    /** 1f = full expressive shape morphing (Compact idle), 0f = clean circle (Expanded). */
    private val morphIntensity: Float,
    /** Slow graceful rotation in radians. */
    private val rotationRad: Float,
    /** Audio/MDNA energy 0f..1f subtly accentuates scalloped depth. */
    private val energy: Float = 0f
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        buildMorphingArtworkPath(
            outPath = path,
            width = size.width,
            height = size.height,
            morphCycle = morphCycle,
            morphIntensity = morphIntensity,
            rotationRad = rotationRad,
            energy = energy
        )
        return Outline.Generic(path)
    }

    companion object {
        private const val SHAPE_COUNT = 5
        private const val SAMPLES = 64

        /**
         * Writes the exact morphing artwork contour into [outPath] so both the
         * `.clip(shape)` mask and the outer glass rim stroke share 100% identical geometry.
         */
        fun buildMorphingArtworkPath(
            outPath: Path,
            width: Float,
            height: Float,
            morphCycle: Float,
            morphIntensity: Float,
            rotationRad: Float,
            energy: Float = 0f
        ) {
            outPath.reset()
            val minDim = minOf(width, height)
            if (minDim <= 0f) return

            val cx = width * 0.5f
            val cy = height * 0.5f
            val baseR = minDim * 0.485f
            val intensity = morphIntensity.coerceIn(0f, 1f)

            if (intensity < 0.005f) {
                outPath.addOval(
                    androidx.compose.ui.geometry.Rect(
                        cx - baseR,
                        cy - baseR,
                        cx + baseR,
                        cy + baseR
                    )
                )
                return
            }

            // Smooth cubic-eased interpolation between consecutive expressive shapes
            val wrapped = ((morphCycle % SHAPE_COUNT) + SHAPE_COUNT) % SHAPE_COUNT
            val idxA = wrapped.toInt() % SHAPE_COUNT
            val idxB = (idxA + 1) % SHAPE_COUNT
            val rawT = wrapped - wrapped.toInt()
            // Hold each iconic shape briefly, then glide smoothly into the next
            val smoothT = smoothStep(0.18f, 0.82f, rawT)
            val energyBoost = 1f + energy.coerceIn(0f, 1f) * 0.18f

            for (i in 0 until SAMPLES) {
                val theta = (2.0 * PI * i / SAMPLES).toFloat()
                val sampleAngle = theta - rotationRad

                val rA = shapeRadiusFactor(idxA, sampleAngle, energyBoost)
                val rB = shapeRadiusFactor(idxB, sampleAngle, energyBoost)
                val morphedFactor = rA + (rB - rA) * smoothT
                // Blend smoothly toward 1.0 (pure circle) as intensity -> 0
                val finalFactor = 1f + (morphedFactor - 1f) * intensity

                val r = baseR * finalFactor
                val x = cx + r * cos(theta)
                val y = cy + r * sin(theta)
                if (i == 0) outPath.moveTo(x, y) else outPath.lineTo(x, y)
            }
            outPath.close()
        }

        /**
         * Normalized polar radius factor for each of the 5 iconic expressive shapes:
         *   0 -> Rounded Pentagon (matching video 00:00, 00:02, 00:05)
         *   1 -> 10-Petal Scalloped Cookie (matching video 00:01)
         *   2 -> Soft Rounded Pill / Oval (matching video 00:03)
         *   3 -> 12-Petal Scalloped Flower (matching video 00:04)
         *   4 -> 8-Point Soft Starburst (matching video 00:06)
         */
        private fun shapeRadiusFactor(shapeIndex: Int, angle: Float, energyBoost: Float): Float = when (shapeIndex) {
            0 -> {
                // Soft rounded pentagon
                val n = 5f
                val sector = (2f * PI.toFloat()) / n
                val local = ((angle % sector) + sector) % sector - sector * 0.5f
                val poly = cos(PI.toFloat() / n) / cos(local).coerceAtLeast(0.65f)
                // Soften corners so artwork stays super pleasing
                val softened = 0.88f + (poly - 0.88f) * 0.58f
                softened.coerceIn(0.84f, 1.03f)
            }
            1 -> {
                // 10-petal scalloped cookie
                (0.93f + 0.068f * energyBoost * cos(10f * angle)).coerceIn(0.83f, 1.02f)
            }
            2 -> {
                // Soft vertical/tilted pill / squircle
                val c = cos(angle)
                val s = sin(angle)
                val superR = (abs(c / 0.84f).pow(3.2f) + abs(s / 1.02f).pow(3.2f)).pow(-1f / 3.2f)
                superR.coerceIn(0.80f, 1.03f)
            }
            3 -> {
                // 12-petal scalloped cookie
                (0.94f + 0.056f * energyBoost * cos(12f * angle)).coerceIn(0.85f, 1.02f)
            }
            else -> {
                // 8-point soft starburst
                (0.90f + 0.095f * energyBoost * cos(8f * angle)).coerceIn(0.79f, 1.03f)
            }
        }

        private fun smoothStep(edge0: Float, edge1: Float, x: Float): Float {
            val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
            return t * t * (3f - 2f * t)
        }
    }
}
