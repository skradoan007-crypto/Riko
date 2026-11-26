package com.rikodev.sync.nexmesh.feature.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.sync.nexmesh.feature.ui.theme.NexMeshColors
import com.rikodev.sync.nexmesh.feature.ui.theme.liquidGlassSurface
import kotlin.math.roundToInt

/**
 * Nex Mesh Kinetic Square Slider (Red-marked spec):
 * - Rounded-square (squircle) frosted glass thumb.
 * - Rotates live in real time proportionally to how much it moves/slides (0%..100% -> 0°..540°).
 * - Pure Light Liquid Glass track with airy sky-blue fill.
 */
@Composable
fun KineticSquareSlider(
    progress: Float,
    modifier: Modifier = Modifier,
    onProgressChange: ((Float) -> Unit)? = null
) {
    val clamped = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clamped,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "kinetic_slider_progress"
    )
    // Live proportional rotation: every movement rotates the square thumb smoothly on-time
    val liveRotationDegrees = animatedProgress * 540f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlassSurface(
                shape = RoundedCornerShape(50),
                topColor = Color(0xC2FFFFFF),
                bottomColor = Color(0x9CE8F2FF),
                elevation = 5.dp
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(34.dp)
                .pointerInput(onProgressChange) {
                    if (onProgressChange == null) return@pointerInput
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        val fraction = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        onProgressChange(fraction)
                    }
                }
        ) {
            val trackH = 12.dp.toPx()
            val trackY = (size.height - trackH) / 2f
            val corner = trackH / 2f

            // Light frosted glass track background
            drawRoundRect(
                color = Color(0x99E2ECF9),
                topLeft = Offset(0f, trackY),
                size = Size(size.width, trackH),
                cornerRadius = CornerRadius(corner, corner)
            )

            // Sky-blue liquid glass active fill
            val fillW = (size.width * animatedProgress).coerceAtLeast(trackH)
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(NexMeshColors.SkyTrackFillStart, NexMeshColors.SkyTrackFillEnd)
                ),
                topLeft = Offset(0f, trackY),
                size = Size(fillW, trackH),
                cornerRadius = CornerRadius(corner, corner)
            )

            // Rotating Frosted-White Rounded-Square (Squircle) Thumb
            val thumbSize = 24.dp.toPx()
            val half = thumbSize / 2f
            val centerX = (half + (size.width - thumbSize) * animatedProgress).coerceIn(half, size.width - half)
            val centerY = size.height / 2f
            val sqCorner = 6.dp.toPx()

            withTransform({
                rotate(degrees = liveRotationDegrees, pivot = Offset(centerX, centerY))
            }) {
                // Soft light drop shadow
                drawRoundRect(
                    color = Color(0x223B507A),
                    topLeft = Offset(centerX - half, centerY - half + 2.dp.toPx()),
                    size = Size(thumbSize, thumbSize),
                    cornerRadius = CornerRadius(sqCorner, sqCorner)
                )
                // Crisp frosted white square body
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFCFFFFFF), Color(0xE6EEF5FF)),
                        start = Offset(centerX - half, centerY - half),
                        end = Offset(centerX + half, centerY + half)
                    ),
                    topLeft = Offset(centerX - half, centerY - half),
                    size = Size(thumbSize, thumbSize),
                    cornerRadius = CornerRadius(sqCorner, sqCorner)
                )
                // Specular glass border
                drawRoundRect(
                    color = Color(0xFFFFFFFF),
                    topLeft = Offset(centerX - half, centerY - half),
                    size = Size(thumbSize, thumbSize),
                    cornerRadius = CornerRadius(sqCorner, sqCorner),
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }
        }

        Text(
            text = "${(animatedProgress * 100).roundToInt()}%",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = NexMeshColors.InkPrimary
        )
    }
}
