package com.rikodev.music.android.presentation.debug

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.music.android.core.experience.calibration.ExperienceProfile
import com.rikodev.music.android.core.experience.wave.WaveQuality
import com.rikodev.music.android.presentation.debug.components.AudioPanel
import com.rikodev.music.android.presentation.debug.components.HiddenDevPanel
import com.rikodev.music.android.presentation.debug.components.InspectorToggle
import com.rikodev.music.android.presentation.debug.components.ProfileSelector
import com.rikodev.music.android.presentation.debug.components.RibbonViewport
import com.rikodev.music.android.presentation.debug.components.SnapshotBar
import com.rikodev.music.android.presentation.debug.components.panels.MotionPanel
import com.rikodev.music.android.presentation.debug.components.panels.PhysicsPanel
import com.rikodev.music.android.presentation.debug.components.panels.PerformancePanel
import com.rikodev.music.android.presentation.debug.components.panels.RenderPanel

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — WAVE LABORATORY SCREEN
//
// Phase 3.4J.2 — The Physics Tuning Console.
//
// This is the developer's tool for tuning the Riko Wave Engine feel.
// It is NOT a user-facing screen. It exists to make every preset feel
// DISTINCT, not just numerically different.
//
// ── SCREEN LAYOUT ──────────────────────────────────────────────────────────
//
//   ┌──────────────────────────────────────────┐
//   │  WAVE LABORATORY                         │
//   │  [Classic] [Fluid] [Elastic] [LG] [Exp] │
//   ├──────────────────────────────────────────┤
//   │                                          │
//   │         Album Art + Ribbon               │
//   │         (70% of visible area)            │
//   │                                          │
//   │  [INSPECT ON/OFF]  [BALANCED ▼]         │
//   ├──────────────────────────────────────────┤
//   │  ━━ PHYSICS ━━                           │
//   │  ━━ MOTION ━━                            │
//   │  ━━ RENDER ━━                            │
//   │  ━━ PERFORMANCE ━━                      │
//   ├──────────────────────────────────────────┤
//   │  [Save] [Load] [Reset] [Export] [Import] │
//   ├──────────────────────────────────────────┤
//   │  ━━ EXPERIENCE SCORE ━━                  │
//   │  ━━ SESSION NOTES ━━                    │
//   │  ━━ GENRE QUICK SWITCH ━━               │
//   ├──────────────────────────────────────────┤
//   │  (Hidden Dev Panel — long press)         │
//   └──────────────────────────────────────────┘
//
// ── RIBBON VIEWPORT RULES ──────────────────────────────────────────────────
//   ❌ No playlist
//   ❌ No lyrics
//   ❌ No bottom navigation
//   ❌ No mini player
//   ❌ No queue
//   ❌ No buttons
//
//   ✅ Album Art (simulated gradient)
//   ✅ Ribbon
//   ✅ Black Background
//
// ── SLIDER RULES ───────────────────────────────────────────────────────────
//   All sliders are LIVE.
//   Pressure ↓ Ribbon changes.
//   No restart. No rebuild. No recomposition trick.
//   Live.
//
// ── PROFILE SELECTOR ────────────────────────────────────────────────────────
//   Click → WaveCalibration.applyPreset(...)
//   Nothing else.
//
// ── SNAPSHOT BAR ──────────────────────────────────────────────────────────
//   Save → JSON
//   Load → Restore
//   Reset → CLASSIC defaults
//   Export → Clipboard (future)
//   Import → Paste (future)
//
// ── EXIT CRITERIA ──────────────────────────────────────────────────────────
//   ✅ Preset switching is instant
//   ✅ Sliders update the ribbon in real time
//   ✅ Snapshots save and restore perfectly
//   ✅ Export/Import works
//   ✅ Inspector can be hidden/shown
//   ✅ 60 FPS maintained on target devices
//   ✅ No memory leaks after 30+ minutes
//   ✅ The ribbon feels different for all five presets
//
// PRODUCER: Phase 3.4J.2
// CONSUMER: Developer (debug/tuning tool)
//
// ⭐ NOTE
//   This is a DEVELOPER TOOL, not a user-facing feature.
//   Gate behind BuildConfig.DEBUG or a hidden settings gesture.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun WaveLaboratoryScreen(
    viewModel: WaveLaboratoryViewModel = remember { WaveLaboratoryViewModel() },
    modifier: Modifier = Modifier,
    // §107 back target: the Lab pops to its entry point (Settings, D-14
    // hidden entry — wired when the entry lands in Phase 7 F).
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current

    // System back leaves the Lab through the same target as the header arrow.
    BackHandler(enabled = onBack != null) {
        onBack?.invoke()
    }

    // ── Animation Loop ──────────────────────────────────────────────
    // This drives the synthetic energy generation.
    // Every frame: generate energy → build WaveState → update flow.
    // WaveCalibration values are read per-frame → slider changes are LIVE.

    LaunchedEffect(Unit) {
        var time = 0L
        while (true) {
            kotlinx.coroutines.delay(16) // ~60fps
            time += 16_000_000L
            viewModel.updateFrame(time)
        }
    }

    // ── Long Press State for Hidden Dev Panel ────────────────────────
    var pressStartTime by remember { mutableLongStateOf(0L) }

    // ── Main Layout ─────────────────────────────────────────────────

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050A14))
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ───────────────────────────────────────────────────
        LaboratoryHeader(onBack = onBack)

        // ── Profile Selector ────────────────────────────────────────
        ProfileSelector(
            activeProfile = viewModel.activeProfile,
            onProfileSelected = { profile ->
                viewModel.selectProfile(profile)
                showToast(context, "Profile: ${profile.name}")
            }
        )

        // ── Ribbon Viewport (70% priority) ─────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        // Single tap: toggle dev panel off
                        if (viewModel.showDevPanel) {
                            viewModel.showDevPanel = false
                        }
                    },
                    onLongClick = {
                        // Long press: toggle hidden dev panel
                        viewModel.showDevPanel = !viewModel.showDevPanel
                    }
                )
        ) {
            RibbonViewport(
                waveState = viewModel.waveState,
                colorState = viewModel.colorState,
                showInspector = viewModel.showInspector,
                fps = viewModel.performance.fps,
                drawTimeMs = viewModel.performance.frameTimeMs,
                qualityName = viewModel.quality.name
            )

            // Hidden Dev Panel overlay
            HiddenDevPanel(
                visible = viewModel.showDevPanel,
                performance = viewModel.performance,
                activeProfile = viewModel.activeProfile.name,
                quality = viewModel.quality.name,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }

        // ── Viewport Controls Row ───────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            InspectorToggle(
                isInspectorVisible = viewModel.showInspector,
                onToggle = { viewModel.showInspector = !viewModel.showInspector }
            )

            QualitySelector(
                currentQuality = viewModel.quality,
                onQualitySelected = { viewModel.quality = it }
            )

            EditableToggle(
                isEditable = viewModel.isEditable,
                onToggle = { viewModel.isEditable = !viewModel.isEditable }
            )
        }

        // ── Collapsible Panels ──────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PhysicsPanel(
                expanded = viewModel.physicsPanelExpanded,
                onToggle = { viewModel.physicsPanelExpanded = !viewModel.physicsPanelExpanded },
                editable = viewModel.isEditable
            )

            MotionPanel(
                expanded = viewModel.motionPanelExpanded,
                onToggle = { viewModel.motionPanelExpanded = !viewModel.motionPanelExpanded },
                editable = viewModel.isEditable
            )

            RenderPanel(
                expanded = viewModel.renderPanelExpanded,
                onToggle = { viewModel.renderPanelExpanded = !viewModel.renderPanelExpanded },
                editable = viewModel.isEditable
            )

            PerformancePanel(
                expanded = viewModel.performancePanelExpanded,
                onToggle = { viewModel.performancePanelExpanded = !viewModel.performancePanelExpanded },
                performance = viewModel.performance
            )
        }

        // ── Snapshot Bar ───────────────────────────────────────────
        SnapshotBar(
            onSave = {
                val name = "snapshot_${System.currentTimeMillis()}"
                viewModel.saveSnapshot(name)
                showToast(context, "Snapshot saved: $name")
            },
            onLoad = {
                val names = viewModel.savedSnapshots.keys.toList()
                if (names.isNotEmpty()) {
                    viewModel.loadSnapshot(names.last())
                    showToast(context, "Snapshot loaded: ${names.last()}")
                }
            },
            onReset = {
                viewModel.resetCalibration()
                showToast(context, "Reset to CLASSIC defaults")
            },
            onExport = {
                val json = viewModel.exportPreset()
                // Copy to clipboard
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                    as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("preset", json))
                showToast(context, "Preset exported to clipboard")
            },
            onImport = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                    as android.content.ClipboardManager
                val clip = clipboard.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val json = clip.getItemAt(0).text.toString()
                    if (viewModel.importPreset(json)) {
                        showToast(context, "Preset imported!")
                    } else {
                        showToast(context, "Import failed: invalid format")
                    }
                }
            },
            hasSnapshots = viewModel.savedSnapshots.isNotEmpty(),
            modifier = Modifier.padding(horizontal = 0.dp)
        )

        // ── Audio Panel (Experience Score + Notes + Genre) ────────────
        AudioPanel(
            scoreSmoothness = viewModel.scoreSmoothness,
            scoreFluidity = viewModel.scoreFluidity,
            scoreGlassFeel = viewModel.scoreGlassFeel,
            scorePerformance = viewModel.scorePerformance,
            scoreOverall = viewModel.scoreOverall,
            onSmoothnessChange = { viewModel.scoreSmoothness = it },
            onFluidityChange = { viewModel.scoreFluidity = it },
            onGlassFeelChange = { viewModel.scoreGlassFeel = it },
            onPerformanceChange = { viewModel.scorePerformance = it },
            selectedGenre = viewModel.selectedGenre,
            onGenreSelected = { viewModel.selectedGenre = it },
            sessionNotes = viewModel.sessionNotes,
            onNotesChanged = { genre, notes -> viewModel.updateNotes(genre, notes) }
        )

        // Bottom spacer
        Spacer(Modifier.height(32.dp))
    }
}

// ── Header ─────────────────────────────────────────────────────────────

@Composable
private fun LaboratoryHeader(onBack: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (onBack != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF19B7FF),
                modifier = Modifier
                    .size(32.dp)
                    .clickable(onClick = onBack)
                    .padding(4.dp)
            )
            Spacer(Modifier.height(4.dp))
        }
        Text(
            text = "WAVE LABORATORY",
            color = Color(0xFF19B7FF),
            fontSize = 18.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Phase 3.4J.2 — Physics Tuning Console",
            color = Color(0xFF446688),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

// ── Quality Selector ──────────────────────────────────────────────────

@Composable
private fun QualitySelector(
    currentQuality: WaveQuality,
    onQualitySelected: (WaveQuality) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        WaveQuality.entries.forEach { q ->
            val isActive = q == currentQuality
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isActive) Color(0xFF19B7FF) else Color(0xFF1A1A2E)
                    )
                    .clickable { onQualitySelected(q) }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = q.name,
                    color = if (isActive) Color.White else Color(0xFF666666),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// ── Editable Toggle ───────────────────────────────────────────────────

@Composable
private fun EditableToggle(
    isEditable: Boolean,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isEditable) Color(0xFF1A1A2E) else Color(0xFF0D0D1A)
            )
            .clickable { onToggle() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = if (isEditable) "EDITABLE" else "READ ONLY",
            color = if (isEditable) Color(0xFF00FF88) else Color(0xFF444444),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

// ── Toast Helper ─────────────────────────────────────────────────────

private fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
