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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.theme.AccentMagenta
import com.rikodev.music.android.core.designsystem.riko.theme.AccentViolet

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — PURE OPTICAL LIQUID GLASS SYSTEM (P13)
//
// Keeps the glass crystal-clear, luminous, and pure across ALL album art
// colors (including harsh red/brown/dark covers) by anchoring the optical
// palette in RIKO's signature violet-indigo-cyan crystal glass and blending
// only a refined 28% whisper of the track's sanitized hue.
// ─────────────────────────────────────────────────────────────────────────────

object LiquidGlassStyle {

    private val CrystalViolet = Color(0xFF8B5CF6)
    private val CrystalCyan = Color(0xFF60A5FA)
    private val CrystalMagenta = Color(0xFFC084FC)

    /**
     * Produces a refined luminous glass accent that preserves RIKO's pure
     * crystal-aurora Liquid Glass look while subtly echoing the track's mood.
     * Harsh red/brown/muddy album art can NEVER turn the glass muddy or bloody.
     */
    fun sanitizeGlassAccent(argb: Int, fallback: Color = CrystalViolet): Color {
        if (argb == 0) return fallback
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        val sat = hsv[1]
        val value = hsv[2]
        if (sat < 0.18f || value < 0.18f) return fallback

        hsv[1] = sat.coerceIn(0.45f, 0.80f)
        hsv[2] = value.coerceIn(0.74f, 0.96f)
        val lifted = Color(android.graphics.Color.HSVToColor(hsv))
        // Blend 28% track hue into 72% pure crystal violet/aurora so glass always looks pure & cohesive
        return lerp(fallback, lifted, 0.28f)
    }

    /**
     * Draws a pure optical Liquid Glass body inside [path] with clipped inner
     * Fresnel top specular shine, bottom caustic bounce, subtle focal aurora,
     * and crisp 1.25dp optical white-glass rim.
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

        // 1. Crystal translucent dark-indigo glass base
        if (shaderBrush != null) {
            drawPath(path = path, brush = shaderBrush)
        } else {
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x981A1738),
                        Color(0xB80E0C22)
                    ),
                    startY = 0f,
                    endY = size.height
                )
            )
        }

        // 2. Clipped internal optical refraction layers
        clipPath(path) {
            // Focal luminous aurora pool (clean violet/cyan crystal glow)
            val auroraRadius = (size.width * 0.40f).coerceAtLeast(size.height * 1.15f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryAccent.copy(alpha = if (isLite) 0.22f else 0.36f + 0.10f * clampedEnergy),
                        secondaryAccent.copy(alpha = if (isLite) 0.08f else 0.16f),
                        Color.Transparent
                    ),
                    center = Offset(focalX, focalY),
                    radius = auroraRadius
                ),
                radius = auroraRadius,
                center = Offset(focalX, focalY)
            )

            // Secondary cool cyan optical dispersion off-center for real crystal glass depth
            if (!isLite) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CrystalCyan.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.82f, size.height * 0.25f),
                        radius = size.height * 1.1f
                    ),
                    radius = size.height * 1.1f,
                    center = Offset(size.width * 0.82f, size.height * 0.25f)
                )
            }

            // Pure white curved Fresnel specular highlight along top edge + subtle bottom caustic
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to Color.White.copy(alpha = if (isLite) 0.20f else 0.38f),
                        0.15f to Color.White.copy(alpha = if (isLite) 0.07f else 0.14f),
                        0.40f to Color.Transparent,
                        0.82f to Color.Transparent,
                        1.00f to Color.White.copy(alpha = if (isLite) 0.06f else 0.15f)
                    ),
                    startY = 0f,
                    endY = size.height
                )
            )

            // Focused bright specular sheen right above active focal point
            if (!isLite) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.32f),
                            Color.Transparent
                        ),
                        center = Offset(focalX, size.height * 0.06f),
                        radius = size.height * 0.78f
                    ),
                    radius = size.height * 0.78f,
                    center = Offset(focalX, size.height * 0.06f)
                )
            }
        }

        // 3. Soft outer optical glass halo (Ultra)
        if (renderProfile.useGlowAndSpecular) {
            drawPath(
                path = path,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        primaryAccent.copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0.30f),
                        CrystalMagenta.copy(alpha = 0.24f),
                        primaryAccent.copy(alpha = 0.18f)
                    )
                ),
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        // 4. Crisp 1.3dp pure white-to-lavender glass rim
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (isLite) 0.45f else 0.80f),
                    primaryAccent.copy(alpha = 0.42f),
                    Color.White.copy(alpha = 0.28f)
                ),
                startY = 0f,
                endY = size.height
            ),
            style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    fun fallbackPrimary(): Color = AccentViolet
    fun fallbackSecondary(): Color = AccentMagenta
}
