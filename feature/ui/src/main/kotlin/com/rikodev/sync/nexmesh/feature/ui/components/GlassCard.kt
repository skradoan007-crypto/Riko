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
import com.rikodev.sync.nexmesh.feature.ui.theme.NexMeshColors
import com.rikodev.sync.nexmesh.feature.ui.theme.liquidGlassSurface

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    topColor: Color = NexMeshColors.GlassSurface,
    bottomColor: Color = NexMeshColors.GlassSurfaceSoft,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.liquidGlassSurface(
            shape = RoundedCornerShape(cornerRadius),
            topColor = topColor,
            bottomColor = bottomColor
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
    val top = if (isPrimary) NexMeshColors.LavenderCtaStart else NexMeshColors.GlassSurface
    val bottom = if (isPrimary) NexMeshColors.LavenderCtaEnd else NexMeshColors.GlassSurfaceSoft

    Box(
        modifier = modifier
            .liquidGlassSurface(
                shape = RoundedCornerShape(50),
                topColor = if (enabled) top else Color(0x80F1F5F9),
                bottomColor = if (enabled) bottom else Color(0x70E2E8F0),
                elevation = if (isPrimary) 6.dp else 3.dp
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) NexMeshColors.InkAccentIndigo else NexMeshColors.InkSecondary,
            fontSize = 14.sp,
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
    Row(
        modifier = modifier
            .liquidGlassSurface(
                shape = RoundedCornerShape(50),
                topColor = Color(0x9EFFFFFF),
                bottomColor = Color(0x7AFFFFFF),
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
                                topColor = NexMeshColors.CyanLensStart,
                                bottomColor = NexMeshColors.CyanLensEnd,
                                elevation = 3.dp
                            )
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) NexMeshColors.InkPrimary else NexMeshColors.InkSecondary
                )
            }
        }
    }
}
