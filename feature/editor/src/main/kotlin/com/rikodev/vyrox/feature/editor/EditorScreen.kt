package com.rikodev.vyrox.feature.editor

import android.view.ViewGroup
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import com.rikodev.vyrox.core.design.LiquidGlassSurface
import com.rikodev.vyrox.core.design.LocalVyRoxGlass
import com.rikodev.vyrox.core.design.VyRoxPalette
import com.rikodev.vyrox.feature.color.ColorGradingPanel
import com.rikodev.vyrox.media.engine.PreviewStatus
import java.util.Locale

@Composable
fun EditorScreen(
    onBack: () -> Unit,
    onExportClick: (String) -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val glass = LocalVyRoxGlass.current
    val clip = state.primaryClip
    val lifecycleOwner = LocalLifecycleOwner.current

    // Free hardware decoder whenever Editor leaves foreground (critical before Export!)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onScreenResumed()
                Lifecycle.Event.ON_STOP -> viewModel.onScreenPaused()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onScreenPaused()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(glass.backgroundBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Studio Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                LiquidGlassSurface(modifier = Modifier.size(38.dp), cornerRadius = 19.dp, onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = glass.inkPrimary,
                        modifier = Modifier.align(Alignment.Center).size(20.dp)
                    )
                }
                Text(
                    text = state.project?.title ?: "VyRox Studio",
                    color = glass.inkPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Undo,
                    contentDescription = "Undo",
                    tint = if (state.canUndo) glass.inkPrimary else glass.inkSecondary.copy(alpha = 0.35f),
                    modifier = Modifier.size(22.dp).clickable(enabled = state.canUndo) { viewModel.undo() }
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Redo,
                    contentDescription = "Redo",
                    tint = if (state.canRedo) glass.inkPrimary else glass.inkSecondary.copy(alpha = 0.35f),
                    modifier = Modifier.size(22.dp).clickable(enabled = state.canRedo) { viewModel.redo() }
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(glass.fabGradient)
                        .clickable {
                            // Explicitly stop ExoPlayer & release hardware decoder BEFORE entering Export!
                            viewModel.onScreenPaused()
                            state.project?.projectId?.let(onExportClick)
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.FileUpload, contentDescription = "Export", tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("Export", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Live GPU Video Preview Surface with explicit Loading / Error feedback
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(22.dp))
                .background(Color.Black)
                .border(1.dp, glass.glassBorderBrush, RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (state.sourceUri != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            player = viewModel.previewController.player
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            when (val status = state.previewStatus) {
                PreviewStatus.Loading -> {
                    CircularProgressIndicator(color = VyRoxPalette.Cyan, modifier = Modifier.size(32.dp))
                }
                is PreviewStatus.Error -> {
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.82f))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "Preview Error: ${status.message}",
                            color = VyRoxPalette.Magenta,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                else -> Unit
            }
        }

        state.statusToast?.let { toast ->
            Spacer(Modifier.height(6.dp))
            LiquidGlassSurface(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp, onClick = { viewModel.dismissToast() }) {
                Text(toast, color = VyRoxPalette.Cyan, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
            }
        }

        Spacer(Modifier.height(10.dp))

        // Transport & Scrubber Bar
        LiquidGlassSurface(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${formatTimeMs(state.positionMs)} / ${formatTimeMs(state.durationMs)}",
                        color = glass.inkPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(glass.fabGradient)
                            .clickable { viewModel.togglePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play or Pause",
                            tint = Color.White
                        )
                    }

                    Icon(
                        imageVector = if (clip?.muted == true) Icons.AutoMirrored.Rounded.VolumeOff
                        else Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "Mute Toggle",
                        tint = if (clip?.muted == true) VyRoxPalette.Magenta else glass.inkPrimary,
                        modifier = Modifier.size(22.dp).clickable { viewModel.toggleMute() }
                    )
                }

                val safeMax = state.durationMs.coerceAtLeast(1_000L).toFloat()
                val safePos = state.positionMs.toFloat().coerceIn(0f, safeMax)
                Slider(
                    value = safePos,
                    onValueChange = { viewModel.seekTo(it.toLong()) },
                    valueRange = 0f..safeMax,
                    colors = SliderDefaults.colors(
                        thumbColor = VyRoxPalette.Cyan,
                        activeTrackColor = VyRoxPalette.Violet
                    )
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Active Tool Drawer
        if (clip != null) {
            when (state.activeTool) {
                EditorToolTab.TRIM -> {
                    val maxSource = clip.effectiveSourceDurationMs.toFloat().coerceAtLeast(1_000f)
                    var draftRange by remember(clip.id, clip.sourceTrimStartMs, clip.sourceTrimEndMs, maxSource) {
                        val s = clip.sourceTrimStartMs.toFloat().coerceIn(0f, maxSource)
                        val e = clip.sourceTrimEndMs.toFloat().coerceIn(s, maxSource)
                        mutableStateOf(s..e)
                    }

                    LiquidGlassSurface(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Timeline Trim", color = glass.inkPrimary, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${formatTimeMs(draftRange.start.toLong())} – ${formatTimeMs(draftRange.endInclusive.toLong())}",
                                    color = VyRoxPalette.Cyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Brush.horizontalGradient(listOf(VyRoxPalette.Violet.copy(alpha = 0.55f), VyRoxPalette.ElectricBlue.copy(alpha = 0.65f))))
                                    .border(1.dp, VyRoxPalette.Cyan.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "  V1 · ${state.project?.title ?: "Clip"} (${formatTimeMs((draftRange.endInclusive - draftRange.start).toLong())})",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            RangeSlider(
                                value = draftRange,
                                onValueChange = { draftRange = it },
                                onValueChangeFinished = {
                                    viewModel.commitTrim(draftRange.start.toLong(), draftRange.endInclusive.toLong())
                                },
                                valueRange = 0f..maxSource,
                                colors = SliderDefaults.colors(
                                    thumbColor = VyRoxPalette.Cyan,
                                    activeTrackColor = VyRoxPalette.Magenta
                                )
                            )
                        }
                    }
                }

                EditorToolTab.COLOR -> {
                    ColorGradingPanel(
                        grade = clip.colorGrade,
                        presets = state.presets,
                        onGradeChange = { viewModel.previewColorGrade(it) },
                        onCommitGrade = { viewModel.commitColorGrade(it) },
                        onSaveCustomPreset = { viewModel.saveCustomPreset(it) }
                    )
                }

                EditorToolTab.SPEED -> {
                    var draftSpeed by remember(clip.speed) { mutableFloatStateOf(clip.speed.coerceIn(0.25f, 3.0f)) }
                    LiquidGlassSurface(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Playback Speed", color = glass.inkPrimary, style = MaterialTheme.typography.titleMedium)
                                Text(String.format(Locale.US, "%.2fx", draftSpeed), color = VyRoxPalette.Cyan, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = draftSpeed,
                                onValueChange = { draftSpeed = it },
                                onValueChangeFinished = { viewModel.commitSpeed(draftSpeed) },
                                valueRange = 0.25f..3.0f,
                                colors = SliderDefaults.colors(thumbColor = VyRoxPalette.Cyan, activeTrackColor = VyRoxPalette.Violet)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        LiquidGlassSurface(modifier = Modifier.fillMaxWidth().height(58.dp), cornerRadius = 24.dp) {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                ToolDockItem(Icons.Rounded.ContentCut, "Trim", state.activeTool == EditorToolTab.TRIM) { viewModel.selectTool(EditorToolTab.TRIM) }
                ToolDockItem(Icons.Rounded.Palette, "Color", state.activeTool == EditorToolTab.COLOR) { viewModel.selectTool(EditorToolTab.COLOR) }
                ToolDockItem(Icons.Rounded.Speed, "Speed", state.activeTool == EditorToolTab.SPEED) { viewModel.selectTool(EditorToolTab.SPEED) }
            }
        }
    }
}

@Composable
private fun ToolDockItem(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    val glass = LocalVyRoxGlass.current
    val tint = if (active) VyRoxPalette.Cyan else glass.inkSecondary
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 4.dp)) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        Text(label, color = tint, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
    }
}

private fun formatTimeMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60L
    val sec = totalSec % 60L
    val tenths = ((ms % 1000L) / 100L).coerceAtLeast(0L)
    return String.format(Locale.US, "%02d:%02d.%d", min, sec, tenths)
}
