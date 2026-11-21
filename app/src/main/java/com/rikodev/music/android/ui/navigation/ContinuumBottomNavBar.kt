package com.rikodev.music.android.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.mergeDescendants
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
    val tabs = remember { ContinuumTab.values().toList() }

    val activeProgress by animateFloatAsState(
        targetValue = selectedTab.ordinal.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.76f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "ContinuumOrbSpring"
    )

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
                    cache = drawCache
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
                        glowPaint = drawCache.glowPaint
                    )
                }
            }

            // Keep the artwork on Canvas, but expose three independent, full-height
            // touch/accessibility targets instead of one anonymous tab-sized canvas.
            Row(modifier = Modifier.fillMaxWidth().height(barHeight)) {
                tabs.forEach { tab ->
                    val isSelected = tab == selectedTab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(barHeight)
                            .semantics(mergeDescendants = true) {
                                role = Role.Tab
                                selected = isSelected
                                contentDescription = "${tab.label}${if (isSelected) ", selected" else ""}"
                            }
                            .clickable {
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
