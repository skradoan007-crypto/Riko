package com.rikodev.music.android.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlinx.coroutines.launch

@Composable
fun ContinuumBottomNavBar(
    selectedTab: ContinuumTab,
    onTabSelected: (ContinuumTab) -> Unit,
    modifier: Modifier = Modifier,
    barHeight: Dp = 94.dp,
    renderTier: ContinuumRenderTier = ContinuumRenderTier.ULTRA,
    colors: ContinuumNavColors = ContinuumNavColors(),
    showBottomPill: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val drawCache = remember { ContinuumDrawCache() }
    val tabs = remember { ContinuumTab.entries }

    DisposableEffect(drawCache) {
        onDispose { drawCache.release() }
    }

    val latestSelectedTab by rememberUpdatedState(selectedTab)
    val latestOnTabSelected by rememberUpdatedState(onTabSelected)

    var isPressed by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableStateOf<Float?>(null) }

    val targetProgress = dragProgress ?: selectedTab.ordinal.toFloat()
    val activeProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = if (dragProgress != null) {
            spring(dampingRatio = 0.92f, stiffness = Spring.StiffnessHigh)
        } else {
            spring(dampingRatio = 0.76f, stiffness = Spring.StiffnessMediumLow)
        },
        label = "ContinuumOrbSpring"
    )

    val touchIntensity by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "ContinuumTouchBloom"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "ContinuumIdlePulse")
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "IdlePulseValue"
    )

    val wavePulse = remember { Animatable(0f) }
    val iconAnimatables = remember { List(3) { Animatable(0f) } }
    var hasInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(selectedTab) {
        if (!hasInitialized) {
            hasInitialized = true
        } else {
            val activeIdx = selectedTab.ordinal
            iconAnimatables.forEachIndexed { idx, anim ->
                if (idx != activeIdx) anim.snapTo(0f)
            }
            launch {
                wavePulse.snapTo(0f)
                wavePulse.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
                )
                wavePulse.snapTo(0f)
            }
            launch {
                val targetAnim = iconAnimatables[activeIdx]
                targetAnim.snapTo(0f)
                targetAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 700, easing = LinearEasing)
                )
                targetAnim.snapTo(0f)
            }
        }
    }

    // 40dp offscreen glow bleed prevents hard clipping at top/bottom/sides while keeping layout height unchanged
    val glowBleed = 40.dp
    val horizontalInset = 12.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 444.dp)
                .height(barHeight)
                .pointerInput(tabs) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isPressed = true
                        val insetPx = horizontalInset.toPx()
                        val dockW = (size.width.toFloat() - insetPx * 2f).coerceAtLeast(1f)
                        var currentX = (down.position.x - insetPx).coerceIn(0f, dockW)

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.changedToUpIgnoreConsumed()) {
                                currentX = (change.position.x - insetPx).coerceIn(0f, dockW)
                                break
                            }
                            if (!change.pressed) break
                            val localX = (change.position.x - insetPx).coerceIn(0f, dockW)
                            if (abs(change.position.x - down.position.x) > 8f) {
                                dragProgress = ((localX / dockW) * 2f).coerceIn(0f, 2f)
                            }
                            currentX = localX
                        }

                        isPressed = false
                        dragProgress = null

                        val tappedIndex = when {
                            currentX < dockW * 0.333f -> 0
                            currentX < dockW * 0.666f -> 1
                            else -> 2
                        }
                        val target = tabs[tappedIndex]
                        if (target != latestSelectedTab) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            latestOnTabSelected(target)
                        }
                    }
                }
                .semantics {
                    role = Role.Tab
                    contentDescription = "RIKO Continuum Navigation Bar, selected ${selectedTab.label}"
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeight(barHeight + glowBleed * 2)
                    .graphicsLayer {
                        clip = false
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
            ) {
                val insetPx = horizontalInset.toPx()
                val bleedPx = glowBleed.toPx()
                val dockW = (size.width - insetPx * 2f).coerceAtLeast(2f)
                val dockH = barHeight.toPx()

                translate(left = insetPx, top = bleedPx) {
                    val metrics = ContinuumMetrics.calculate(dockW, dockH)
                    val isTransitioning = isPressed || wavePulse.isRunning
                    val effectiveIdlePulse = if (isTransitioning) 0f else idlePulse

                    renderContinuumLayers1To4(
                        metrics = metrics,
                        activeProgress = activeProgress,
                        idlePulse = effectiveIdlePulse,
                        touchIntensity = touchIntensity,
                        wavePulseProgress = wavePulse.value,
                        colors = colors,
                        renderTier = renderTier,
                        cache = drawCache
                    )

                    val baseIconSize = dockH * 0.30f
                    tabs.forEachIndexed { index, tab ->
                        val weight = nodeWeight(activeProgress, index)
                        drawContinuumIcon(
                            tab = tab,
                            center = Offset(metrics.slotX[index], metrics.centerY),
                            baseSize = baseIconSize,
                            activeWeight = weight,
                            colors = colors,
                            renderTier = renderTier,
                            glowPaint = drawCache.glowPaint,
                            animProgress = iconAnimatables[index].value
                        )
                    }
                }
            }
        }

        if (showBottomPill) {
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.gesturePill)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0714, widthDp = 390)
@Composable
private fun ContinuumBottomNavBarPreview() {
    var currentTab by remember { mutableStateOf(ContinuumTab.LISTEN) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0714))
            .padding(vertical = 36.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        ContinuumBottomNavBar(
            selectedTab = currentTab,
            onTabSelected = { currentTab = it },
            renderTier = ContinuumRenderTier.ULTRA
        )
    }
}
