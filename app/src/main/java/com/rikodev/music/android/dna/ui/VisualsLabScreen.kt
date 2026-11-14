package com.rikodev.music.android.dna.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.music.android.dna.core.BandSource
import com.rikodev.music.android.dna.render.visual.RikoVisual
import com.rikodev.music.android.dna.render.visual.RikoVisuals

/**
 * VisualsLabScreen — browse and preview all RIKO visuals.
 *
 * Left side: scrollable list of visual cards (name + description)
 * Right side: live preview of the selected visual
 *
 * This is the command center for visual selection in Phase 5.
 */
@Composable
fun VisualsLabScreen(
    bandSource: BandSource,
    playing: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedVisual by remember { mutableStateOf<RikoVisual?>(null) }
    val context = LocalContext.current

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Left panel: visual list
        LazyColumn(
            modifier = Modifier
                .weight(0.35f)
                .fillMaxHeight()
                .background(Color(0xFF0D0D12))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Visuals Lab",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE2E6BD),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(RikoVisuals.ALL) { visual ->
                VisualCard(
                    visual = visual,
                    isSelected = visual == selectedVisual,
                    onClick = { selectedVisual = visual }
                )
            }
        }

        // Right panel: live preview
        Box(
            modifier = Modifier
                .weight(0.65f)
                .fillMaxHeight()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            selectedVisual?.let { visual ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        visual.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE2E6BD),
                        modifier = Modifier.padding(12.dp)
                    )
                    VisualHost(
                        visual = visual,
                        bandSource = bandSource,
                        playing = playing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .padding(16.dp)
                    )
                }
            } ?: run {
                Text(
                    "Select a visual →",
                    fontSize = 16.sp,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}

@Composable
private fun VisualCard(
    visual: RikoVisual,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1A237E) else Color(0xFF1A1A24)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                visual.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) Color.White else Color(0xFFCCCCCC)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                visual.id,
                fontSize = 11.sp,
                color = Color(0xFF888888)
            )
        }
    }
}
