package com.rikodev.music.android.presentation.nowplaying.eq

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.music.android.core.designsystem.theme.EqShapes
import com.rikodev.music.android.core.designsystem.theme.EqTheme
import com.rikodev.music.android.core.designsystem.theme.EqType
import com.rikodev.music.android.core.designsystem.theme.EqUiThemeMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Flat Equalizer-style controls rebuilt in Compose (Canvas + pointer input).
 * All colors come from [LocalEqColors] — never hardcode hex here.
 */

// ─────────────────────────────────────────────────────────────────────
// ArcSeekBar — gauge-style arc (Flat's ArcSeekBar)
// ─────────────────────────────────────────────────────────────────────

@Composable
fun ArcSeekBar(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    contentDescriptionLabel: String = "Effect strength",
) {
    val eq = EqTheme.colors
    val haptics = LocalHapticFeedback.current
    val sweepDegrees = 220f
    val startDegrees = 160f // opens at bottom-left
    val progress = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start))
        .coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .size(120.dp)
            .semantics { contentDescription = contentDescriptionLabel }
            .pointerInput(valueRange) {
                detectDragGestures(
                    onDragStart = { offset -> haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                    onDrag = { change, _ ->
                        change.consume()
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = change.position.x - cx
                        val dy = change.position.y - cy
                        // atan2 angle → 0..1 around the arc
                        val angle = (atan2(dy, dx) * 180f / PI.toFloat() + 360f) % 360f
                        val rel = (angle - startDegrees + 360f) % 360f
                        val frac = (rel / sweepDegrees).coerceIn(0f, 1f)
                        onValueChange(
                            valueRange.start + frac * (valueRange.endInclusive - valueRange.start)
                        )
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(120.dp)) {
            val stroke = 10.dp.toPx()
            val inset = stroke / 2f + 4.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)

            // background track
            drawArc(
                color = eq.track,
                startAngle = startDegrees,
                sweepAngle = sweepDegrees,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            // progress
            drawArc(
                color = eq.accent,
                startAngle = startDegrees,
                sweepAngle = sweepDegrees * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            // thumb
            val thumbAngle = (startDegrees + sweepDegrees * progress) * PI.toFloat() / 180f
            val radius = arcSize.width / 2f
            val thumb = Offset(
                x = topLeft.x + radius + cos(thumbAngle) * radius,
                y = topLeft.y + radius + sin(thumbAngle) * radius
            )
            drawCircle(color = eq.accent, radius = stroke * 0.85f, center = thumb)
            drawCircle(color = eq.switchThumb, radius = stroke * 0.45f, center = thumb)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// MidSeekBar — center-detent balance bar (Flat's MidSeekBar)
// ─────────────────────────────────────────────────────────────────────

@Composable
fun MidSeekBar(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = -1f..1f,
    leftLabel: String = "L",
    rightLabel: String = "R",
    contentDescriptionLabel: String = "Balance",
) {
    val eq = EqTheme.colors
    val haptics = LocalHapticFeedback.current
    var lastSign by remember { mutableStateOf(if (value >= 0f) 1 else -1) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(leftLabel, color = eq.textSecondary, fontSize = EqType.Caption.fontSize)
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .semantics { contentDescription = contentDescriptionLabel }
                    .pointerInput(valueRange) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val frac = (change.position.x / size.width).coerceIn(0f, 1f)
                            val raw = valueRange.start + frac * (valueRange.endInclusive - valueRange.start)
                            val snapped = if (abs(raw) < (valueRange.endInclusive - valueRange.start) * 0.03f) 0f else raw
                            val sign = if (snapped >= 0f) 1 else -1
                            if (sign != lastSign && snapped == 0f) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            lastSign = sign
                            onValueChange(snapped)
                        }
                    }
            ) {
                val cy = size.height / 2f
                val trackH = 6.dp.toPx()
                // track
                drawRoundRect(
                    color = eq.track,
                    topLeft = Offset(0f, cy - trackH / 2f),
                    size = Size(size.width, trackH),
                    cornerRadius = CornerRadius(trackH / 2f)
                )
                val frac = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start))
                    .coerceIn(0f, 1f)
                val mid = 0.5f
                val x = size.width * frac
                // filled segment from center to thumb
                val fillLeft = minOf(size.width * mid, x)
                val fillWidth = abs(x - size.width * mid)
                drawRoundRect(
                    color = eq.accent,
                    topLeft = Offset(fillLeft, cy - trackH / 2f),
                    size = Size(fillWidth, trackH),
                    cornerRadius = CornerRadius(trackH / 2f)
                )
                // center detent tick
                drawLine(
                    color = eq.textSecondary,
                    start = Offset(size.width * mid, cy - trackH),
                    end = Offset(size.width * mid, cy + trackH),
                    strokeWidth = 2.dp.toPx()
                )
                // thumb
                drawCircle(color = eq.switchThumb, radius = 10.dp.toPx(), center = Offset(x, cy))
                drawCircle(color = eq.accent, radius = 6.dp.toPx(), center = Offset(x, cy))
            }
            Text(rightLabel, color = eq.textSecondary, fontSize = EqType.Caption.fontSize)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// EqCurve — response curve through band nodes (Flat's Curve)
// ─────────────────────────────────────────────────────────────────────

data class EqBandPoint(
    val frequencyHz: Int,
    val levelDb: Float,
    val minDb: Float = -15f,
    val maxDb: Float = 15f,
)

@Composable
fun EqCurve(
    bands: List<EqBandPoint>,
    modifier: Modifier = Modifier,
    onBandLevelChange: ((Int, Float) -> Unit)? = null,
) {
    val eq = EqTheme.colors

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .pointerInput(bands) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val node = onBandLevelChange ?: return@detectDragGestures
                    if (bands.isEmpty()) return@detectDragGestures
                    // nearest band by x
                    val frac = (change.position.x / size.width).coerceIn(0f, 1f)
                    val index = (frac * (bands.size - 1)).roundToInt()
                    val band = bands[index]
                    val yFrac = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                    val db = band.minDb + yFrac * (band.maxDb - band.minDb)
                    node(index, db)
                }
            }
    ) {
        if (bands.isEmpty()) return@Canvas
        val w = size.width
        val h = size.height
        val pts = bands.mapIndexed { i, b ->
            val x = if (bands.size == 1) w / 2f else w * i / (bands.size - 1f)
            val yFrac = ((b.levelDb - b.minDb) / (b.maxDb - b.minDb)).coerceIn(0f, 1f)
            Offset(x, h * (1f - yFrac))
        }

        // 0 dB baseline
        val zeroY = h * (1f - (0f - bands.first().minDb) / (bands.first().maxDb - bands.first().minDb))
        drawLine(
            color = eq.track,
            start = Offset(0f, zeroY),
            end = Offset(w, zeroY),
            strokeWidth = 1.dp.toPx()
        )

        // Smooth cubic curve — Catmull-Rom converted to cubics with light
        // tension so band nodes connect without the quadratic "uchu-nicha"
        // bumps. Monotone-enough for EQ response; no overshoot past nodes.
        val path = Path().apply {
            if (pts.isEmpty()) return@apply
            moveTo(pts.first().x, pts.first().y)
            val tension = 0.32f
            for (i in 0 until pts.size - 1) {
                val p0 = pts.getOrElse(i - 1) { pts[i] }
                val p1 = pts[i]
                val p2 = pts[i + 1]
                val p3 = pts.getOrElse(i + 2) { pts[i + 1] }
                val c1x = p1.x + (p2.x - p0.x) * tension / 3f
                val c1y = p1.y + (p2.y - p0.y) * tension / 3f
                val c2x = p2.x - (p3.x - p1.x) * tension / 3f
                val c2y = p2.y - (p3.y - p1.y) * tension / 3f
                cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y)
            }
        }
        // fill under curve
        val fill = Path().apply {
            addPath(path)
            lineTo(pts.last().x, h)
            lineTo(pts.first().x, h)
            close()
        }
        drawPath(path = fill, color = eq.curveFill)
        drawPath(path = path, color = eq.curveStroke, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

        // nodes
        pts.forEach { p ->
            drawCircle(color = eq.curveStroke, radius = 8.dp.toPx(), center = p)
            drawCircle(color = eq.switchThumb, radius = 4.dp.toPx(), center = p)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// EqBandRow — one vertical-ish band slider with Hz label
// ─────────────────────────────────────────────────────────────────────

@Composable
fun EqBandRow(
    label: String,
    levelDb: Float,
    minDb: Float,
    maxDb: Float,
    onLevelChangeDb: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val eq = EqTheme.colors
    Column(
        modifier = modifier.width(44.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (levelDb >= 0) "+%.1f".format(levelDb) else "%.1f".format(levelDb),
            color = eq.accent,
            fontSize = EqType.Tiny.fontSize,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .height(110.dp)
                .width(28.dp)
                .pointerInput(minDb, maxDb) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val yFrac = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                        onLevelChangeDb(minDb + yFrac * (maxDb - minDb))
                    }
                }
        ) {
            val trackW = 6.dp.toPx()
            val cx = size.width / 2f
            drawRoundRect(
                color = eq.track,
                topLeft = Offset(cx - trackW / 2f, 0f),
                size = Size(trackW, size.height),
                cornerRadius = CornerRadius(trackW / 2f)
            )
            val frac = ((levelDb - minDb) / (maxDb - minDb)).coerceIn(0f, 1f)
            val y = size.height * (1f - frac)
            // fill from center
            val zeroY = size.height * (1f - (0f - minDb) / (maxDb - minDb))
            drawRoundRect(
                color = eq.accent,
                topLeft = Offset(cx - trackW / 2f, minOf(y, zeroY)),
                size = Size(trackW, abs(y - zeroY)),
                cornerRadius = CornerRadius(trackW / 2f)
            )
            drawCircle(color = eq.switchThumb, radius = 8.dp.toPx(), center = Offset(cx, y))
            drawCircle(color = eq.accent, radius = 5.dp.toPx(), center = Offset(cx, y))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = eq.textSecondary, fontSize = EqType.Tiny.fontSize)
    }
}

// ─────────────────────────────────────────────────────────────────────
// EqThemeToggle — dark/light pill
// ─────────────────────────────────────────────────────────────────────

@Composable
fun EqThemeToggle(
    mode: EqUiThemeMode,
    onModeChange: (EqUiThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val eq = EqTheme.colors
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val darkSelected = mode == EqUiThemeMode.DARK
        ThemePill(
            label = "Dark",
            selected = darkSelected,
            selectedColor = eq.accent,
            textColor = if (darkSelected) eq.switchThumb else eq.textSecondary,
            onClick = { onModeChange(EqUiThemeMode.DARK) }
        )
        Spacer(Modifier.width(6.dp))
        ThemePill(
            label = "Light",
            selected = !darkSelected,
            selectedColor = eq.accent,
            textColor = if (!darkSelected) eq.switchThumb else eq.textSecondary,
            onClick = { onModeChange(EqUiThemeMode.LIGHT) }
        )
    }
}

@Composable
private fun ThemePill(
    label: String,
    selected: Boolean,
    selectedColor: Color,
    textColor: Color,
    onClick: () -> Unit,
) {
    val eq = EqTheme.colors
    Box(
        modifier = Modifier
            .clip(EqShapes.Pill)
            .background(if (selected) selectedColor else eq.cardAlt)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, color = textColor, fontSize = EqType.Caption.fontSize, fontWeight = FontWeight.Medium)
    }
}
