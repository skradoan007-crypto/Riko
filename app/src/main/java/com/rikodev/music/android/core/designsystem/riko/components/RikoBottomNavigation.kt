package com.rikodev.music.android.core.designsystem.riko.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.theme.RikoComponentSize
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlassBorder
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMotion
import com.rikodev.music.android.core.designsystem.riko.theme.RikoNavCapsule
import com.rikodev.music.android.core.designsystem.riko.theme.RikoPrimaryGradient
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextOnAccent
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextSecondary

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — FLOATING CAPSULE BOTTOM NAV (SM-UI-SPEC-001 v2.0, Section 2.6)
//
// Floating glass pill capsule, height 64dp, positioned by the shell 24dp above
// bottom safe-area. 4 items, evenly distributed.
// INACTIVE: icon 24dp, text-secondary (60% white), no background.
// ACTIVE: 52dp circle filled with the Aurora gradient behind the icon (icon
// solid white text-on-accent). The circle physically SLIDES from the old tab
// to the new one with spring-snappy (spec 5.3); icon color cross-fades in
// sync; subtle selection scale pulse (5.3).
//
// ICON-ONLY: this component renders no visible label (verified: the item Box
// contains only an Icon). TalkBack must not lose it though, so each tab
// carries Role.Tab + selected state + contentDescription. Keep all three when
// touching this file, or screen readers lose both the name and the state.
//
// The two icon/fill tweens below (rikoNavIcon pulse + colour cross-fade) must
// read the *same* `isSelected` snapshot. `enabled = !isSelected` turns clicks
// off exactly when that snapshot is true, so the selected tab never re-fires.
// Touch target = full item cell (weight 1f · fillMaxHeight), well above the
// 48dp accessibility floor at any supported width.
// ─────────────────────────────────────────────────────────────────────────────

data class RikoNavItem(
    val icon: ImageVector,
    val label: String
)

@Composable
fun RikoBottomNavigation(
    items: List<RikoNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val lastIndex = (items.size - 1).coerceAtLeast(1)
    val targetFraction = selectedIndex.toFloat() / lastIndex

    // Spring-snappy physical slide of the indicator (spec 5.3).
    val indicatorFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = 300f
        ),
        label = "rikoNavIndicatorSlide"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(RikoComponentSize.BottomNavHeight)
            .clip(CircleShape)
            .background(RikoNavCapsule)
            .border(1.dp, RikoGlassBorder, CircleShape)
    ) {
        val navWidth = maxWidth
        val itemWidth = navWidth / items.size.coerceAtLeast(1)
        val indicatorSize = RikoComponentSize.BottomNavActiveIndicatorSize
        val travel = navWidth - itemWidth
        val indicatorOffsetX = travel * indicatorFraction + (itemWidth - indicatorSize) / 2f

        // Aurora circle — behind the icons (drawn first in the same Box).
        // Spec 2.6: FIXED 52dp circle regardless of label width.
        Box(
            modifier = Modifier
                .offset(x = indicatorOffsetX)
                .size(indicatorSize)
                .align(Alignment.CenterStart)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(RikoPrimaryGradient))
        )

        Row(modifier = Modifier.fillMaxSize()) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) RikoTextOnAccent else RikoTextSecondary,
                    animationSpec = RikoMotion.springSpec(),
                    label = "rikoNavIcon"
                )
                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.10f else 1f,
                    animationSpec = RikoMotion.snappy(),
                    label = "rikoNavPulse"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        // Role.Tab + selected: TalkBack announces "Home, tab,
                        // selected" instead of a bare unlabelled button.
                        // NOTE: `selected` is *not* repeated inside .semantics
                        // below. clickable propagates the pre-merge
                        // SemanticsPropertyReceiver into the merged node, so
                        // re-declaring contentDescription there keeps ONE node.
                        // Re-setting `selected` inside .semantics{} would create
                        // a SECOND element that shadows the clickable's one.
                        .clickable(
                            role = Role.Tab,
                            enabled = !isSelected
                        ) { onItemSelected(index) }
                        .semantics { contentDescription = item.label },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier
                            .size(RikoComponentSize.BottomNavIconSize)
                            .scale(iconScale)
                    )
                }
            }
        }
    }
}
