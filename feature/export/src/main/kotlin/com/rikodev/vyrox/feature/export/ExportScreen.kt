package com.rikodev.vyrox.feature.export

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rikodev.vyrox.core.design.LiquidGlassSurface
import com.rikodev.vyrox.core.design.LocalVyRoxGlass
import com.rikodev.vyrox.core.design.VyRoxPalette
import com.rikodev.vyrox.core.model.ExportResolution
import com.rikodev.vyrox.core.model.ExportVideoCodec
import com.rikodev.vyrox.media.engine.ExportState

@Composable
fun ExportScreen(
    onBack: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val glass = LocalVyRoxGlass.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(glass.backgroundBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LiquidGlassSurface(modifier = Modifier.size(40.dp), cornerRadius = 20.dp, onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = glass.inkPrimary,
                    modifier = Modifier.align(Alignment.Center).size(20.dp)
                )
            }
            Text("Export Studio", color = glass.inkPrimary, style = MaterialTheme.typography.headlineMedium)
        }

        LiquidGlassSurface(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Resolution", color = glass.inkPrimary, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportResolution.entries.forEach { res ->
                        val selected = state.config.resolution == res
                        ChoiceChip(res.label, selected) { viewModel.selectResolution(res) }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text("Video Codec", color = glass.inkPrimary, style = MaterialTheme.typography.titleMedium)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportVideoCodec.entries.forEach { codec ->
                        val selected = state.config.codec == codec
                        ChoiceChip(codec.label, selected) { viewModel.selectCodec(codec) }
                    }
                }
            }
        }

        when (val status = state.exportState) {
            ExportState.Idle -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(glass.fabGradient)
                        .clickable { viewModel.startExport() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Render & Save to Gallery", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            is ExportState.Running -> {
                LiquidGlassSurface(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Encoding via Media3 Hardware Pipeline · ${status.progressPct}%",
                            color = glass.inkPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        LinearProgressIndicator(
                            progress = { status.progressPct / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = VyRoxPalette.Cyan
                        )
                        Text(
                            text = "Cancel Export",
                            color = VyRoxPalette.Magenta,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { viewModel.cancelExport() }
                        )
                    }
                }
            }

            is ExportState.Completed -> {
                LiquidGlassSurface(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = VyRoxPalette.Cyan, modifier = Modifier.size(32.dp))
                        Column {
                            Text("Export Complete", color = glass.inkPrimary, style = MaterialTheme.typography.titleMedium)
                            Text("Saved to ${status.galleryDisplayPath}", color = glass.inkSecondary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            is ExportState.Failed -> {
                LiquidGlassSurface(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Export Failed", color = VyRoxPalette.Magenta, style = MaterialTheme.typography.titleMedium)
                        Text(status.message, color = glass.inkSecondary, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Try Again",
                            color = VyRoxPalette.Cyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { viewModel.startExport() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val glass = LocalVyRoxGlass.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) VyRoxPalette.Violet.copy(alpha = 0.35f) else Color.White.copy(alpha = if (glass.isDark) 0.08f else 0.6f))
            .border(1.dp, if (selected) VyRoxPalette.Cyan else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label, color = glass.inkPrimary, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}
