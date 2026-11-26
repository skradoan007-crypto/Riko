package com.rikodev.sync.nexmesh.feature.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rikodev.sync.nexmesh.feature.ui.theme.NexMeshColors
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class RikoSwitchState {
    OFF,
    ENGAGING,
    ON,
    LOCKED
}

/**
 * Riko Signature Switch — Official RikoDev custom Jetpack Compose Canvas toggle.
 * - Morphing Pillar Geometry: narrow vertical pillar (|) in OFF -> balanced solid squircle in ON.
 * - Spring Physics: Touch-down anticipation, velocity-reactive stretch, 50% magnetic detent haptic tick.
 * - Native 4-State System: OFF, ENGAGING (center breathing pulse + error shake), ON, LOCKED (hatched texture).
 * - Zero bitmaps, zero Lottie, zero heavy blur shaders — locked 60 FPS on low-end devices.
 */
@Composable
fun RikoSignatureSwitch(
    state: RikoSwitchState,
    onToggleRequest: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 52.dp,
    height: Dp = 28.dp,
    failurePulseKey: Int = 0,
    activeTrackColor: Color = NexMeshColors.MintTrackActive
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val targetProgress = when (state) {
        RikoSwitchState.OFF, RikoSwitchState.LOCKED -> 0f
        RikoSwitchState.ENGAGING -> 0.5f
        RikoSwitchState.ON -> 1f
    }

    val progress = remember { Animatable(targetProgress) }
    val anticipationStretch = remember { Animatable(0f) }
    val errorShake = remember { Animatable(0f) }
    var isPressed by remember { mutableStateOf(false) }
    var dragVelocityBoost by remember { mutableFloatStateOf(0f) }
    var crossedMidpoint by remember { mutableStateOf(targetProgress >= 0.5f) }

    LaunchedEffect(state) {
        progress.animateTo(
            targetValue = targetProgress,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
        crossedMidpoint = targetProgress >= 0.5f
    }

    LaunchedEffect(failurePulseKey) {
        if (failurePulseKey > 0) {
            for (offset in listOf(-4f, 4f, -2.5f, 2.5f, 0f)) {
                errorShake.animateTo(offset, tween(45))
            }
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "riko_engaging_pulse")
    val engagingPulse by pulseTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "engaging_scale"
    )

    val isInteractive = state == RikoSwitchState.OFF || state == RikoSwitchState.ON

    Canvas(
        modifier = modifier
            .size(width, height)
            .semantics {
                role = Role.Switch
                stateDescription = state.name
            }
            .pointerInput(state) {
                if (!isInteractive) return@pointerInput
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        val dir = if (state == RikoSwitchState.OFF) 1f else -1f
                        scope.launch {
                            anticipationStretch.animateTo(dir * 0.22f, spring(stiffness = Spring.StiffnessHigh))
                        }
                        val released = tryAwaitRelease()
                        isPressed = false
                        scope.launch {
                            anticipationStretch.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                        }
                        if (released) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleRequest(state == RikoSwitchState.OFF)
                        }
                    }
                )
            }
            .pointerInput(state) {
                if (!isInteractive) return@pointerInput
                detectHorizontalDragGestures(
                    onDragEnd = {
                        dragVelocityBoost = 0f
                        val shouldBeOn = progress.value >= 0.5f
                        scope.launch {
                            progress.animateTo(
                                targetValue = if (shouldBeOn) 1f else 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                        if ((shouldBeOn && state == RikoSwitchState.OFF) || (!shouldBeOn && state == RikoSwitchState.ON)) {
                            onToggleRequest(shouldBeOn)
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        val maxTravel = (size.width - size.height).coerceAtLeast(1)
                        var next = (progress.value + dragAmount / maxTravel).coerceIn(0f, 1f)
                        // Midpoint magnetic detent feel around 0.5f
                        if (abs(next - 0.5f) < 0.045f) {
                            next = 0.5f + (next - 0.5f) * 0.4f
                        }
                        val nowPastMid = next >= 0.5f
                        if (nowPastMid != crossedMidpoint) {
                            crossedMidpoint = nowPastMid
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        dragVelocityBoost = (dragAmount / 18f).coerceIn(-0.35f, 0.35f)
                        scope.launch { progress.snapTo(next) }
                    }
                )
            }
    ) {
        val trackCorner = size.height / 2f
        val p = progress.value.coerceIn(0f, 1f)
        val shakePx = errorShake.value * density

        // 1. Track Background
        val offTrackColor = Color(0xCCEEF3FA)
        val blendedTrack = Color(
            red = offTrackColor.red + (activeTrackColor.red - offTrackColor.red) * p,
            green = offTrackColor.green + (activeTrackColor.green - offTrackColor.green) * p,
            blue = offTrackColor.blue + (activeTrackColor.blue - offTrackColor.blue) * p,
            alpha = 0.92f
        )

        drawRoundRect(
            color = if (state == RikoSwitchState.LOCKED) Color(0xB8E2E8F0) else blendedTrack,
            cornerRadius = CornerRadius(trackCorner, trackCorner)
        )

        // Subtle top-edge highlight on ON track
        if (p > 0.25f && state != RikoSwitchState.LOCKED) {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0x66FFFFFF), Color.Transparent),
                    startY = 0f,
                    endY = size.height * 0.55f
                ),
                cornerRadius = CornerRadius(trackCorner, trackCorner)
            )
        }

        // LOCKED / OVERRIDDEN crisp diagonal hatched texture
        if (state == RikoSwitchState.LOCKED) {
            val clip = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = 0f,
                        top = 0f,
                        right = size.width,
                        bottom = size.height,
                        cornerRadius = CornerRadius(trackCorner, trackCorner)
                    )
                )
            }
            clipPath(clip) {
                val step = 7.dp.toPx()
                var x = -size.height
                while (x < size.width + size.height) {
                    drawLine(
                        color = Color(0x40475569),
                        start = Offset(x, size.height),
                        end = Offset(x + size.height, 0f),
                        strokeWidth = 1.2.dp.toPx()
                    )
                    x += step
                }
            }
        }

        // Track 1dp crisp border
        drawRoundRect(
            color = if (p > 0.5f) Color(0x88FFFFFF) else Color(0x3D475569),
            cornerRadius = CornerRadius(trackCorner, trackCorner),
            style = Stroke(width = 1.dp.toPx())
        )

        // 2. Morphing Pillar (|) -> Balanced Squircle Block Geometry
        val pad = 3.5.dp.toPx()
        val maxThumbH = size.height - pad * 2f
        val pillarWidth = 4.dp.toPx()
        val squircleWidth = maxThumbH * 1.05f

        val baseWidth = pillarWidth + (squircleWidth - pillarWidth) * p
        val stretchFactor = 1f + abs(anticipationStretch.value) + abs(dragVelocityBoost)
        val engagingFactor = if (state == RikoSwitchState.ENGAGING) engagingPulse else 1f

        val thumbW = (baseWidth * stretchFactor * engagingFactor).coerceAtMost(size.width * 0.52f)
        val thumbH = (maxThumbH * (if (state == RikoSwitchState.ENGAGING) engagingFactor else 1f)).coerceAtMost(size.height - pad * 1.5f)

        val minX = pad + 4.dp.toPx() * (1f - p)
        val maxX = size.width - pad - thumbW
        val leanOffset = anticipationStretch.value * 8.dp.toPx()
        val thumbX = (minX + (maxX - minX) * p + leanOffset + shakePx).coerceIn(pad, size.width - pad - thumbW)
        val thumbY = (size.height - thumbH) / 2f

        val thumbRadius = (2.dp.toPx() + (6.5.dp.toPx() - 2.dp.toPx()) * p)

        // Thumb fill: crisp slate pillar in OFF -> luminous solid white squircle in ON
        val thumbColor = when (state) {
            RikoSwitchState.LOCKED -> Color(0xFF64748B)
            else -> Color(
                red = 0.25f + (1.0f - 0.25f) * p,
                green = 0.32f + (1.0f - 0.32f) * p,
                blue = 0.45f + (1.0f - 0.45f) * p,
                alpha = 1f
            )
        }

        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(thumbX, thumbY),
            size = Size(thumbW, thumbH),
            cornerRadius = CornerRadius(thumbRadius, thumbRadius)
        )

        // Subtle inner highlight on solid squircle when ON
        if (p > 0.5f) {
            drawLine(
                color = Color(0xAAFFFFFF),
                start = Offset(thumbX + thumbRadius, thumbY + 1.5.dp.toPx()),
                end = Offset(thumbX + thumbW - thumbRadius, thumbY + 1.5.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
