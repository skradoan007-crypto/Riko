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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(selectedTab.ordinal.toFloat()) }

    val targetProgress = if (isDragging) dragProgress else selectedTab.ordinal.toFloat()
    val activeProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = if (isDragging) {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)
        } else {
            spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
        },
        label = "ContinuumOrbSpring"
    )

    val touchAwaken by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0f,
        animationSpec = tween(durationMillis = 160),
        label = "ContinuumTouchAwaken"
    )

    // State A (Idle): Gentle breathing glow in ULTRA/BALANCED tiers
    val idlePulse = if (renderTier != ContinuumRenderTier.LITE) {
        val infinite = rememberInfiniteTransition(label = "ContinuumIdlePulse")
        val value by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "IdleGlow"
        )
        value
    } else {
        0.5f
    }

    // State C (Active Selection): One-shot sonar wave + one-shot Lottie-style icon animation (~720ms)
    val selectionWave = remember { Animatable(1f) }
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
            if (renderTier != ContinuumRenderTier.LITE) {
                launch {
                    selectionWave.snapTo(0f)
                    selectionWave.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing)
                    )
                }
            }
            launch {
                val targetAnim = iconAnimatables[activeIdx]
                targetAnim.snapTo(0f)
                targetAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 720, easing = LinearEasing)
                )
                targetAnim.snapTo(0f)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .height(barHeight)
                .pointerInput(selectedTab) {
                    detectHorizontalDragGestures(
                        onDragStart = { startOffset ->
                            isDragging = true
                            val slotSpan = (size.width * 0.696f).coerceAtLeast(1f)
                            val leftSlot = size.width * 0.152f
                            dragProgress = ((startOffset.x - leftSlot) / slotSpan * 2f).coerceIn(0f, 2f)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onHorizontalDrag = { change, _ ->
                            val slotSpan = (size.width * 0.696f).coerceAtLeast(1f)
                            val leftSlot = size.width * 0.152f
                            val next = ((change.position.x - leftSlot) / slotSpan * 2f).coerceIn(0f, 2f)
                            if (next.roundToInt() != dragProgress.roundToInt()) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            dragProgress = next
                        },
                        onDragEnd = {
                            val resolved = tabs[dragProgress.roundToInt().coerceIn(0, tabs.lastIndex)]
                            isDragging = false
                            if (resolved != selectedTab) {
                                onTabSelected(resolved)
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                        }
                    )
                }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight)
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
            ) {
                val metrics = ContinuumMetrics.calculate(size.width, size.height)

                renderContinuumLayers1To4(
                    metrics = metrics,
                    activeProgress = activeProgress,
                    colors = colors,
                    renderTier = renderTier,
                    cache = drawCache,
                    idlePulse = idlePulse,
                    touchAwaken = touchAwaken,
                    selectionWave = selectionWave.value
                )

                val baseIconSize = size.height * 0.30f
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

            // Three independent, full-height touch & accessibility targets (direct tap)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight)
            ) {
                tabs.forEach { tab ->
                    val isSelected = tab == selectedTab
                    val interactionSource = remember(tab) { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(barHeight)
                            .semantics(mergeDescendants = true) {
                                role = Role.Tab
                                selected = isSelected
                                contentDescription = "${tab.label}${if (isSelected) ", selected" else ""}"
                            }
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                if (!isSelected) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onTabSelected(tab)
                                }
                            }
                    )
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
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        ContinuumBottomNavBar(
            selectedTab = currentTab,
            onTabSelected = { currentTab = it },
            renderTier = ContinuumRenderTier.ULTRA
        )
        ContinuumBottomNavBar(
            selectedTab = ContinuumTab.EXPLORE,
            onTabSelected = {},
            renderTier = ContinuumRenderTier.ULTRA
        )
        ContinuumBottomNavBar(
            selectedTab = ContinuumTab.LIBRARY,
            onTabSelected = {},
            renderTier = ContinuumRenderTier.ULTRA
        )
    }
}
