package com.rikodev.sync.nexmesh.feature.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.sync.nexmesh.feature.ui.theme.LocalNexMeshPalette
import com.rikodev.sync.nexmesh.feature.ui.theme.liquidGlassSurface

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    elevated: Boolean = false,
    topColor: Color? = null,
    bottomColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val p = LocalNexMeshPalette.current
    val resolvedTop = topColor ?: if (elevated) p.glassElevatedTop else p.glassSurfaceTop
    val resolvedBottom = bottomColor ?: if (elevated) p.glassElevatedBottom else p.glassSurfaceBottom
    Box(
        modifier = modifier.liquidGlassSurface(
            shape = RoundedCornerShape(cornerRadius),
            topColor = resolvedTop,
            bottomColor = resolvedBottom,
            rimTop = p.glassRimTop,
            rimBottom = p.glassRimBottom,
            shadowColor = p.glassShadow,
            elevation = if (elevated) 10.dp else 5.dp
        ),
        content = content
    )
}

@Composable
fun LiquidGlassPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true,
    enabled: Boolean = true
) {
    val p = LocalNexMeshPalette.current
    val top = if (isPrimary) p.accentPillTop else p.glassSurfaceTop
    val bottom = if (isPrimary) p.accentPillBottom else p.glassSurfaceBottom

    Box(
        modifier = modifier
            .liquidGlassSurface(
                shape = RoundedCornerShape(50),
                topColor = if (enabled) top else p.glassSurfaceBottom.copy(alpha = 0.45f),
                bottomColor = if (enabled) bottom else p.glassSurfaceBottom.copy(alpha = 0.35f),
                rimTop = p.glassRimTop,
                rimBottom = p.glassRimBottom,
                shadowColor = p.glassShadow,
                elevation = if (isPrimary && enabled) 6.dp else 2.dp
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) {
                if (isPrimary) p.inkAccent else p.inkPrimary
            } else {
                p.inkSecondary.copy(alpha = 0.6f)
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun LiquidGlassSegmentedBar(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalNexMeshPalette.current
    Row(
        modifier = modifier
            .liquidGlassSurface(
                shape = RoundedCornerShape(50),
                topColor = p.glassSurfaceTop,
                bottomColor = p.glassSurfaceBottom,
                rimTop = p.glassRimTop,
                rimBottom = p.glassRimBottom,
                shadowColor = p.glassShadow,
                elevation = 4.dp
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (selected) {
                            Modifier.liquidGlassSurface(
                                shape = RoundedCornerShape(50),
                                topColor = p.accentPillTop,
                                bottomColor = p.accentPillBottom,
                                rimTop = p.glassRimTop,
                                rimBottom = p.glassRimBottom,
                                shadowColor = p.glassShadow,
                                elevation = 3.dp
                            )
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) p.inkAccent else p.inkSecondary
                )
            }
        }
    }
}
