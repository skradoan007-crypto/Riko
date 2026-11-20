package com.rikodev.music.android.presentation.continuum

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
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
    val tabs = remember { ContinuumTab.entries }

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
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .height(barHeight)
                // Critical: Offscreen compositing ensures SCREEN & ADD blend modes render cleanly
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .pointerInput(tabs, onTabSelected) {
                    detectTapGestures { offset ->
                        val width = size.width.toFloat()
                        if (width <= 0f) return@detectTapGestures
                        val tappedIndex = when {
                            offset.x < width * 0.333f -> 0
                            offset.x < width * 0.666f -> 1
                            else -> 2
                        }
                        val target = tabs[tappedIndex]
                        if (target != selectedTab) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTabSelected(target)
                        }
                    }
                }
                .semantics {
                    role = Role.Tab
                    contentDescription = "RIKO Continuum Navigation Bar, selected ${selectedTab.label}"
                }
        ) {
            val metrics = ContinuumMetrics.calculate(size.width, size.height)

            // Layers 1 to 4: Glass Base + Volumetric Silk Cocoon & Ribbons + Pockets + Active Plasma Orb
            renderContinuumLayers1To4(
                metrics = metrics,
                activeProgress = activeProgress,
                colors = colors,
                renderTier = renderTier,
                cache = drawCache
            )

            // Layer 5: Foreground Fixed-Order Vector Icons
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