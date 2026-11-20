package com.rikodev.music.android.presentation.continuum

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.theme.AccentMagenta
import com.rikodev.music.android.core.designsystem.riko.theme.AccentViolet

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — PURE OPTICAL LIQUID GLASS SYSTEM (P12)
//
// Replaces flat muddy tinted fills with true multi-layered optical glass:
//   1. Sanitized luminous accent (never lets brown/muddy art dirty the glass)
//   2. Deep translucent obsidian crystal core (background shimmers cleanly)
//   3. Internal focal aurora refraction pool
//   4. Upper curved Fresnel specular highlight + bottom caustic reflection
//   5. Crisp prismatic glass rim stroke (bright white-top -> iridescent edge)
// ─────────────────────────────────────────────────────────────────────────────

object LiquidGlassStyle {

    /**
     * Sanitizes an extracted ARGB album-art color so the Liquid Glass never turns
     * muddy brown, dull grey, or overly dark. Preserves hue while lifting saturation
     * and brightness into clean optical aurora glass tones.
     */
    fun sanitizeGlassAccent(argb: Int, fallback: Color = AccentViolet): Color {
        if (argb == 0) return fallback
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        // If color is desaturated/muddy or dark brown/orange-grey, blend gently toward violet-cyan aurora
        val sat = hsv[1]
        val value = hsv[2]
        if (sat < 0.22f || value < 0.22f) return fallback
        // Lift dull brown/olive hues (15°..55° with low brightness) into luminous violet-rose glass
        val hue = hsv[0]
        if (hue in 15f..55f && value < 0.72f) {
            return AccentViolet
        }
        hsv[1] = sat.coerceIn(0.45f, 0.85f)
        hsv[2] = value.coerceIn(0.68f, 0.96f)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }

    /**
     * Draws a pure optical Liquid Glass body inside [path] with clipped inner
     * Fresnel specular highlights, bottom caustic bounce, and prismatic glass rim.
     */
    fun DrawScope.drawLiquidGlassBody(
        path: Path,
        focalX: Float,
        focalY: Float,
        primaryAccent: Color,
        secondaryAccent: Color,
        energy: Float,
        renderProfile: EffectiveRenderProfile,
        shaderBrush: Brush? = null
    ) {
        val clampedEnergy = energy.coerceIn(0f, 1f)
        val isLite = renderProfile.selectedTier == VisualQualityTier.LITE

        // 1. Optical glass base fill (translucent dark crystal, NOT opaque mud)
        if (shaderBrush != null) {
            drawPath(path = path, brush = shaderBrush)
        } else {
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x9E161428), // translucent top crystal
                        Color(0xB80B0918)  // deeper optical base
                    ),
                    startY = 0f,
                    endY = size.height
                )
            )
        }

        // 2. Clipped internal optical reflections (Fresnel specular + focal aurora + bottom caustic)
        clipPath(path) {
            // Soft internal focal aurora glow (subtle, keeps glass pure & readable)
            val auroraRadius = (size.width * 0.42f).coerceAtLeast(size.height * 1.1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryAccent.copy(alpha = if (isLite) 0.16f else 0.28f + 0.10f * clampedEnergy),
                        secondaryAccent.copy(alpha = if (isLite) 0.06f else 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(focalX, focalY),
                    radius = auroraRadius
                ),
                radius = auroraRadius,
                center = Offset(focalX, focalY)
            )

            // Upper curved Fresnel glass highlight (signature pure liquid glass shine)
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to Color.White.copy(alpha = if (isLite) 0.18f else 0.34f),
                        0.16f to Color.White.copy(alpha = if (isLite) 0.06f else 0.13f),
                        0.42f to Color.Transparent,
                        0.82f to Color.Transparent,
                        1.00f to Color.White.copy(alpha = if (isLite) 0.05f else 0.12f)
                    ),
                    startY = 0f,
                    endY = size.height
                )
            )

            // Focused specular sheen right above the active focal point
            if (!isLite) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.26f),
                            Color.Transparent
                        ),
                        center = Offset(focalX, size.height * 0.08f),
                        radius = size.height * 0.85f
                    ),
                    radius = size.height * 0.85f,
                    center = Offset(focalX, size.height * 0.08f)
                )
            }
        }

        // 3. Soft outer refraction halo (Ultra only)
        if (renderProfile.useGlowAndSpecular) {
            drawPath(
                path = path,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        primaryAccent.copy(alpha = 0.16f),
                        Color.White.copy(alpha = 0.24f),
                        secondaryAccent.copy(alpha = 0.22f),
                        primaryAccent.copy(alpha = 0.16f)
                    )
                ),
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        // 4. Crisp 1.25dp optical glass rim (bright top-edge catchlight fading to subtle side edge)
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (isLite) 0.42f else 0.72f),
                    primaryAccent.copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.22f)
                ),
                startY = 0f,
                endY = size.height
            ),
            style = Stroke(width = 1.25.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    /** Fallback secondary aurora color paired cleanly with primary. */
    fun fallbackSecondary(): Color = AccentMagenta
}
