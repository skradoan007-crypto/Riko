package com.rikodev.music.android.presentation.continuum

import androidx.compose.ui.geometry.Rect
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
// RIKO LIQUID NEXUS — BOLD EXPRESSIVE MORPHING ARTWORK MASK (P13)
//
// Data class (proper equals/hashCode!) so Compose ALWAYS invalidates & re-clips
// the actual album artwork bitmap on every frame as it morphs boldly through:
//   0. Rounded Pentagon        (video 00:00, 00:02, 00:05)
//   1. 10-Petal Scalloped Cookie (video 00:01)
//   2. Soft Rounded Pill/Oval  (video 00:03)
//   3. 12-Petal Flower Cookie  (video 00:04)
//   4. 8-Point Soft Starburst  (video 00:06)
//
// When Expand is tapped, [morphIntensity] eases 1f -> 0f, stopping the morph
// and settling the artwork cleanly into a pure crisp Circle.
// ─────────────────────────────────────────────────────────────────────────────

data class MorphingLiquidArtworkShape(
    val morphCycle: Float,
    val morphIntensity: Float,
    val rotationRad: Float,
    val energy: Float = 0f
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
        private const val SAMPLES = 72

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
            val baseR = minDim * 0.49f
            val intensity = morphIntensity.coerceIn(0f, 1f)

            if (intensity < 0.005f) {
                outPath.addOval(Rect(cx - baseR, cy - baseR, cx + baseR, cy + baseR))
                return
            }

            val wrapped = ((morphCycle % SHAPE_COUNT) + SHAPE_COUNT) % SHAPE_COUNT
            val idxA = wrapped.toInt() % SHAPE_COUNT
            val idxB = (idxA + 1) % SHAPE_COUNT
            val rawT = wrapped - wrapped.toInt()
            // Hold each iconic shape clearly, then smoothly glide into the next
            val smoothT = smoothStep(0.22f, 0.78f, rawT)
            val energyBoost = 1f + energy.coerceIn(0f, 1f) * 0.16f

            for (i in 0 until SAMPLES) {
                val theta = (2.0 * PI * i / SAMPLES).toFloat()
                val sampleAngle = theta - rotationRad

                val rA = shapeRadiusFactor(idxA, sampleAngle, energyBoost)
                val rB = shapeRadiusFactor(idxB, sampleAngle, energyBoost)
                val morphedFactor = rA + (rB - rA) * smoothT
                val finalFactor = 1f + (morphedFactor - 1f) * intensity

                val r = baseR * finalFactor
                val x = cx + r * cos(theta)
                val y = cy + r * sin(theta)
                if (i == 0) outPath.moveTo(x, y) else outPath.lineTo(x, y)
            }
            outPath.close()
        }

        private fun shapeRadiusFactor(shapeIndex: Int, angle: Float, energyBoost: Float): Float = when (shapeIndex) {
            0 -> {
                // Bold Rounded Pentagon (clear 5-sided silhouette like reference video)
                val n = 5f
                val sector = (2f * PI.toFloat()) / n
                val local = ((angle % sector) + sector) % sector - sector * 0.5f
                val poly = cos(PI.toFloat() / n) / cos(local).coerceAtLeast(0.58f)
                // Soften just the tips while keeping strong unmistakable pentagon edges
                val softened = 0.80f + (poly - 0.80f) * 0.78f
                softened.coerceIn(0.76f, 1.01f)
            }
            1 -> {
                // Bold 10-Petal Scalloped Cookie
                (0.87f + 0.125f * energyBoost * cos(10f * angle)).coerceIn(0.73f, 1.01f)
            }
            2 -> {
                // Bold Organic Pill / Rounded Stadium Oval
                val c = cos(angle)
                val s = sin(angle)
                val superR = (abs(c / 0.74f).pow(3.4f) + abs(s / 1.00f).pow(3.4f)).pow(-1f / 3.4f)
                superR.coerceIn(0.70f, 1.01f)
            }
            3 -> {
                // Bold 12-Petal Flower Cookie
                (0.885f + 0.108f * energyBoost * cos(12f * angle)).coerceIn(0.75f, 1.01f)
            }
            else -> {
                // Bold 8-Point Soft Starburst (like reference video 00:06)
                (0.84f + 0.155f * energyBoost * cos(8f * angle)).coerceIn(0.68f, 1.01f)
            }
        }

        private fun smoothStep(edge0: Float, edge1: Float, x: Float): Float {
            val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
            return t * t * (3f - 2f * t)
        }
    }
}
