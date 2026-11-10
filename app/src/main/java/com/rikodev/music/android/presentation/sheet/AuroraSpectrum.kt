package com.rikodev.music.android.presentation.sheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Surface
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.composed
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rikodev.music.android.audiofx.SpectrumBands
import com.rikodev.music.android.audiofx.visualizer.SpectrumConfig
import com.rikodev.music.android.audiofx.visualizer.SpectrumController
import com.rikodev.music.android.audiofx.visualizer.SpectrumTier
import com.rikodev.music.android.core.designsystem.riko.components.RikoRotaryKnob
import com.rikodev.music.android.core.designsystem.riko.components.knobValueFrom
import com.rikodev.music.android.core.designsystem.riko.components.knobValueTo
import com.rikodev.music.android.core.designsystem.theme.AccentCyan
import com.rikodev.music.android.core.designsystem.riko.theme.AccentMagenta
import com.rikodev.music.android.core.designsystem.riko.theme.AccentViolet
import com.rikodev.music.android.core.designsystem.riko.theme.RikoPrimaryGradient
import com.rikodev.music.android.core.designsystem.riko.theme.RikoError
import com.rikodev.music.android.core.designsystem.riko.theme.RikoShape
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextSecondary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextTertiary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography
import com.rikodev.music.android.presentation.nowplaying.PlayerViewModel
import com.rikodev.music.android.presentation.nowplaying.rememberWavePhase
import com.rikodev.music.android.presentation.nowplaying.visualization.VisualizerExport
import com.rikodev.music.android.data.repository.VisualizerPrefs
import com.rikodev.music.android.data.repository.VisualizerTheme
import com.rikodev.music.android.presentation.nowplaying.visualization.SpectrumDrawColors
import com.rikodev.music.android.presentation.nowplaying.visualization.SpectrumLevelSource
import com.rikodev.music.android.presentation.nowplaying.visualization.SpectrumStyle
import com.rikodev.music.android.presentation.nowplaying.visualization.SpectrogramHistory
import com.rikodev.music.android.presentation.nowplaying.visualization.VisualizerDensity
import com.rikodev.music.android.presentation.nowplaying.visualization.VisualizerLabPreset
import com.rikodev.music.android.presentation.nowplaying.visualization.drawColors
import com.rikodev.music.android.presentation.nowplaying.visualization.drawSpectrum
import com.rikodev.music.android.presentation.nowplaying.visualization.matchingPreset
import com.rikodev.music.android.presentation.nowplaying.visualization.swatchColors
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — VISUALIZER LAB (M6 + visualizer_plan v1, product pass)
//
// Full-screen second visual layer of Now Playing (plan §9-10): artwork never
// disappears — it becomes the soft blurred ambient under the live spectrum,
// playback controls stay reachable. This is a LAB, not a demo canvas:
//
//   • Presets       — one-tap complete setups (Signature / Calm / Pulse / Studio)
//   • Colour theme  — Album accent (live palette) or fixed identities
//   • Mode          — Aurora (signature contour) / Classic (precise bars)
//   • Sensitivity   — 0.5×–1.5× level scaling, live while dragging
//   • Glow + Detail — depth toggle, 32/40/48 display bands
//   • On/off        — persistent master switch with its own state
//   • States        — LIVE / PREVIEW chips, buffering, playback error+retry,
//                     no-playlist-of-songs empty state, disabled transport
//
// Data (zero new permissions, playback never touched):
//   - spectrumState: 40-band log spectrum + peak-hold from the Media3 PCM
//     tap (~30Hz). Fresh = active + right size + young (SpectrumController).
//   - Fallback (stale/absent: paused, offload path, pre-first-frame):
//     3-band energy interpolation — never dead bars.
//   - colorState: artwork-derived accent (one song = one environment).
//   - visualizerPrefs: persisted Lab configuration (SettingsRepository).
// ─────────────────────────────────────────────────────────────────────────────

private const val WAVEFORM_SAMPLES = 128
private const val SENSITIVITY_MIN = 0.5f
private const val SENSITIVITY_MAX = 1.5f

private val StatusLive = Color(0xFF4ADE80)
private val StatusPreview = Color(0xFFFBBF24)
private val StatusBuffering = Color(0xFF38BDF8)
private val StatusError = RikoError
private val LabBackground = Color(0xFF0D0716)

@Composable
fun AuroraSpectrum(
    viewModel: PlayerViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Chrome-driving state (low frequency: track, transport, user settings).
    val song by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playbackError by viewModel.playbackError.collectAsStateWithLifecycle()
    val buffering by viewModel.isLoading.collectAsStateWithLifecycle()
    val canGoPrevious by viewModel.hasPrevious.collectAsStateWithLifecycle()
    val canGoNext by viewModel.hasNext.collectAsStateWithLifecycle()
    val userPresets by viewModel.userVisualizerPresets.collectAsStateWithLifecycle()
    val prefs by viewModel.visualizerPrefs.collectAsStateWithLifecycle()

    // Frame-rate flows are held as State and never dereferenced in this
    // composable body — the canvas leaf reads them in draw scope only, and
    // the chrome reads slow projections below. A ~30Hz energy/pulse tick
    // therefore invalidates DRAW only, never this screen's composition.
    val colors = viewModel.colorState.collectAsStateWithLifecycle()
    val spectrum = viewModel.spectrumState.collectAsStateWithLifecycle()

    // Sensitivity draft — tracks the slider LIVE while dragging, commits on
    // release; the spectrum renders from the draft so the response is instant.
    var sensitivityDraft by remember(prefs.sensitivity) {
        mutableFloatStateOf(prefs.sensitivity)
    }

    // Lab drawer — session-local; every CONTROL it holds persists.
    var labOpen by remember { mutableStateOf(false) }

    val style = remember(prefs.styleName) {
        runCatching { SpectrumStyle.valueOf(prefs.styleName) }.getOrDefault(SpectrumStyle.AURORA)
    }
    val activePreset = matchingPreset(prefs)

    val hasSong = song != null

    // Signal freshness ages with wall-clock time. Derive it so a ~30Hz frame
    // only notifies when LIVE/PREVIEW actually flips. Re-keyed on isPlaying
    // so a pause re-ages the tap even after the frame stream stops.
    val fresh by remember(isPlaying) {
        derivedStateOf {
            val s = spectrum.value
            SpectrumController.isTapLive(
                isActive = s.isActive,
                bandSize = s.bands.size,
                frameAgeNanos = System.nanoTime() - s.timestampNanos
            )
        }
    }

    // Display-rate phase: keeps motion fluid between 30Hz DSP frames.
        // Plan 5.4: motion speed knob scales the studio phase velocity.
        val phaseState = rememberWavePhase(speedTarget = { (if (isPlaying) 1f else 0.2f) * prefs.motionSpeed })
    // NOTE: phaseState is deliberately NOT read here. Reading it in the
    // composition body would recompose this entire screen every display
    // frame (60/90/120Hz). Reading it inside the Canvas draw lambda below
    // invalidates DRAW only - visualizer_plan section 13.
    // It is created HERE (not in the canvas leaf) because its speedTarget
    // lambda reads isPlaying/prefs as State delegates, and rememberWavePhase
    // captures that lambda exactly once.

    // Colour identity: fixed themes stay stable, album accent follows art.
    // Slow projection (same pattern as NpProgressBar's artworkMain): the
    // chrome accent and theme swatches only move when the artwork palette
    // changes - energyIntensity/pulseAmount ticks never notify readers.
    val albumColors by remember {
        derivedStateOf {
            val c = colors.value
            SpectrumDrawColors(
                waveStart = c.waveStart,
                waveEnd = c.waveEnd,
                primary = c.primary,
                ambientGlow = c.ambientGlow
            )
        }
    }
    val drawColors = prefs.theme.drawColors(albumColors)
    val accent = drawColors.primary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LabBackground)
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp)
        ) {
            // ── Top bar: close · title · lab drawer toggle ────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close visualizer",
                        tint = RikoTextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                // Colorful bar icon (Samsung One UI 9.0 style)
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box(modifier = Modifier.width(3.dp).height(10.dp).background(AccentCyan, CircleShape))
                    Box(modifier = Modifier.width(3.dp).height(16.dp).background(AccentViolet, CircleShape))
                    Box(modifier = Modifier.width(3.dp).height(12.dp).background(AccentMagenta, CircleShape))
                    Box(modifier = Modifier.width(3.dp).height(18.dp).background(AccentViolet, CircleShape))
                    Box(modifier = Modifier.width(3.dp).height(8.dp).background(AccentCyan, CircleShape))
                }
                Text(
                    text = "Music Visualizer Lab",
                    style = RikoTypography.Title,
                    color = RikoTextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                        .padding(start = 8.dp)
                )
                IconButton(
                    onClick = { labOpen = !labOpen },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = if (labOpen) "Hide lab controls" else "Show lab controls",
                        tint = if (labOpen) accent else RikoTextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // ── Song bar + Export button ──────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = RikoTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = song?.title ?: "No song playing",
                    style = RikoTypography.Body,
                    color = RikoTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
                // Export button (Samsung style: purple gradient)
                // N3 fix: was a no-op comment. Now captures the lab screen and
                // hands it to VisualizerExport (Pictures/RIKO/Visualizer/).
                val exportContext = androidx.compose.ui.platform.LocalContext.current
                val exportView = androidx.compose.ui.platform.LocalView.current
                Surface(
                    onClick = {
                        runCatching {
                            val bitmap = android.graphics.Bitmap.createBitmap(
                                exportView.width.coerceAtLeast(1),
                                exportView.height.coerceAtLeast(1),
                                android.graphics.Bitmap.Config.ARGB_8888
                            )
                            exportView.draw(android.graphics.Canvas(bitmap))
                            VisualizerExport
                                .exportScreenshot(exportContext, bitmap.asImageBitmap())
                                .onSuccess { uri ->
                                    VisualizerExport.shareScreenshot(exportContext, uri)
                                }
                        }
                    },
                    shape = RikoShape.Card,
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(RikoPrimaryGradient),
                                RikoShape.Card
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Export",
                                style = RikoTypography.Label,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // ── Status chips: live signal honesty ─────────────────────────
            if (prefs.enabled && hasSong) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (buffering) {
                        StatusChip(
                            text = "BUFFERING",
                            color = StatusBuffering,
                            leading = {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    color = StatusBuffering
                                )
                            }
                        )
                    }
                    if (fresh) {
                        StatusChip(text = "LIVE", color = StatusLive)
                    } else {
                        StatusChip(text = "PREVIEW", color = StatusPreview)
                    }
                }
            }

            // ── Playback error: visible, actionable, never silent ─────────
            AnimatedVisibility(
                visible = playbackError != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                PlaybackErrorBanner(
                    message = playbackError ?: "",
                    onRetry = { viewModel.retry() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }

            // ── Spectrum stage: live canvas, off state, or empty state ────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp)
            ) {
                // Plan 5.3: the rolling spectrogram history lives once per lab
                // session — playback spectrum only, never microphone.
                val spectrogramHistory = remember { SpectrogramHistory() }
                if (prefs.enabled && hasSong) {
                    LabSpectrumCanvas(
                        viewModel = viewModel,
                        prefs = prefs,
                        style = style,
                        sensitivityDraft = sensitivityDraft,
                        drawColors = drawColors,
                        phaseState = phaseState,
                        tapLive = fresh,
                        spectrogramHistory = spectrogramHistory,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (!prefs.enabled) {
                    LabOffState(
                        onEnable = {
                            viewModel.updateVisualizerPrefs { it.copy(enabled = true) }
                        },
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LabEmptyState(modifier = Modifier.align(Alignment.Center))
                }
            }

            // ── Lab drawer: presets · theme · mode · tuning ───────────────
            AnimatedVisibility(
                visible = labOpen,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                        .clip(RikoShape.Card)
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color.White.copy(alpha = 0.10f), RikoShape.Card)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Presets — one-tap complete setups.
                    LabLabel("PRESETS")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VisualizerLabPreset.entries.forEach { preset ->
                            val selected = preset == activePreset
                            LabPill(
                                text = preset.label,
                                selected = selected,
                                accent = accent,
                                onClick = {
                                    viewModel.updateVisualizerPrefs { preset.applyTo(it) }
                                }
                            )
                        }
                    }

                    // Reset — restores ALL five Signature settings in ONE atomic
                    // DataStore write (settings are never persisted piecemeal, so
                    // there is no half-reset state to recover from).
                    //
                    // applyTo() deliberately leaves `enabled` untouched: a reset
                    // restores the LOOK (style / colour / sensitivity / glow /
                    // density) but never re-enables a visualizer the user turned
                    // off. That is the honest contract — reset what you asked to
                    // reset, don't silently flip the master switch.
                    //
                    // Disabled once Signature is already active, so "nothing to
                    // reset" is visible rather than a button that does nothing.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                viewModel.updateVisualizerPrefs { current ->
                                    VisualizerLabPreset.SIGNATURE.applyTo(current)
                                }
                            },
                            enabled = activePreset != VisualizerLabPreset.SIGNATURE,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = accent)
                        ) {
                            Text(
                                text = "Reset to Signature",
                                style = RikoTypography.Label
                            )
                        }
                    }

                    // Colour theme — live album palette or a fixed identity.
                    LabLabel("COLOUR THEME")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectableGroup(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        VisualizerTheme.entries.forEach { theme ->
                            val selected = theme == prefs.theme
                            Column(
                                modifier = Modifier
                                    .clip(RikoShape.Card)
                                    .selectable(
                                        selected = selected,
                                        onClick = {
                                            viewModel.updateVisualizerPrefs {
                                                it.copy(theme = theme)
                                            }
                                        },
                                        role = androidx.compose.ui.semantics.Role.RadioButton
                                    )
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(theme.swatchColors(albumColors))
                                        )
                                        .border(
                                            width = 2.dp,
                                            color = if (selected) Color.White
                                            else Color.White.copy(alpha = 0.16f),
                                            shape = CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = theme.label,
                                    style = RikoTypography.Caption,
                                    color = if (selected) RikoTextPrimary else RikoTextTertiary
                                )
                            }
                        }
                    }

                    // Mode — 2-column grid (Samsung One UI 9.0 style)
                    // FIX: Row-based grid instead of LazyVerticalGrid (crashes inside verticalScroll)
                    LabLabel("MODE")
                    val styles = SpectrumStyle.entries
                    val rows = (styles.size + 1) / 2
                    for (row in 0 until rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (col in 0 until 2) {
                                val index = row * 2 + col
                                if (index < styles.size) {
                                    val styleOpt = styles[index]
                                    val selected = styleOpt == style
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RikoShape.Card)
                                            .background(
                                                if (selected) accent.copy(alpha = 0.12f)
                                                else Color.White.copy(alpha = 0.04f)
                                            )
                                            .border(
                                                width = if (selected) 1.5.dp else 1.dp,
                                                color = if (selected) accent
                                                else Color.White.copy(alpha = 0.08f),
                                                shape = RikoShape.Card
                                            )
                                            .clickable {
                                                viewModel.updateVisualizerPrefs {
                                                    it.copy(styleName = styleOpt.name)
                                                }
                                            }
                                            .padding(12.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(56.dp)
                                                    .clip(RikoShape.Card)
                                                    .background(Color.Black.copy(alpha = 0.3f))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(
                                                            Brush.horizontalGradient(
                                                                listOf(
                                                                    accent.copy(alpha = 0.3f),
                                                                    accent.copy(alpha = 0.1f)
                                                                )
                                                            )
                                                        )
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = styleLabel(styleOpt),
                                                style = RikoTypography.Label,
                                                color = if (selected) RikoTextPrimary
                                                else RikoTextSecondary,
                                                fontWeight = if (selected) FontWeight.SemiBold
                                                else FontWeight.Normal,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                        if (selected) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size(20.dp)
                                                    .clip(RikoShape.Circle)
                                                    .background(accent),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        if (row < rows - 1) {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    // Plan 5.4/5.5: studio rotary controls + user presets.
                    StudioControls(
                        prefs = prefs,
                        accent = accent,
                        presets = userPresets,
                        onChange = { transform -> viewModel.updateVisualizerPrefs(transform) },
                        onApplyPreset = viewModel::applyVisualizerPreset,
                        onSavePreset = viewModel::saveVisualizerPreset,
                        onRenamePreset = viewModel::renameVisualizerPreset,
                        onDeletePreset = viewModel::deleteVisualizerPreset,
                        onReset = viewModel::resetVisualizer
                    )

                    // Glow — depth toggle (one row: label left, switch right).
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RikoShape.Card)
                            .semantics(mergeDescendants = true) { },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            LabLabel("GLOW")
                            Text(
                                text = "Ambient light around the spectrum",
                                style = RikoTypography.Caption,
                                color = RikoTextTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = prefs.glowEnabled,
                            onCheckedChange = { checked ->
                                viewModel.updateVisualizerPrefs {
                                    it.copy(glowEnabled = checked)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = accent,
                                checkedThumbColor = Color.White,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.16f),
                                uncheckedThumbColor = Color.White.copy(alpha = 0.85f)
                            )
                        )
                    }

                    // Detail — display band count (DSP stays untouched).
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabLabel("DETAIL")
                        LabSegmentedRow(
                            options = VisualizerDensity.entries.map {
                                it to it.label
                            },
                            selectedIndex = VisualizerDensity.forBands(prefs.bandDensity).ordinal,
                            accent = accent,
                            compact = true,
                            onSelected = { density ->
                                viewModel.updateVisualizerPrefs {
                                    it.copy(bandDensity = density.bands)
                                }
                            }
                        )
                    }
                }
            }

            // ── Now-playing footer + reachable, honest transport ──────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = song?.title ?: "Nothing playing",
                    style = RikoTypography.Body,
                    color = RikoTextPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = song?.artist.orEmpty(),
                    style = RikoTypography.Caption,
                    color = RikoTextTertiary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            val canPrevious = hasSong && canGoPrevious
            val canNext = hasSong && canGoNext
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LabTransportButton(
                    enabled = canPrevious,
                    contentDescription = "Previous track",
                    onClick = { viewModel.skipToPrevious() },
                    icon = { tint ->
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                )
                // Play keeps a stable 48dp slot; buffering swaps in a spinner
                // so the control never jumps when the state changes.
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (buffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(30.dp),
                            strokeWidth = 2.5.dp,
                            color = accent
                        )
                    } else {
                        IconButton(
                            onClick = { viewModel.togglePlayPause() },
                            enabled = hasSong
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause
                                else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = RikoTextPrimary.copy(alpha = if (hasSong) 1f else 0.35f),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
                LabTransportButton(
                    enabled = canNext,
                    contentDescription = "Next track",
                    onClick = { viewModel.skipToNext() },
                    icon = { tint ->
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

// ------------------------------------------------------------------------------------
// LAB PIECES
// ------------------------------------------------------------------------------------

// ------------------------------------------------------------------------------------
// SPECTRUM CANVAS - owns the frame-rate flows (spectrum / wave / audio / colour).
// Every hot read happens inside the draw lambda, so a ~30Hz emission invalidates
// DRAW only and never recomposes this leaf or its parents. visualizer_plan 13.
// ------------------------------------------------------------------------------------

@Composable
private fun LabSpectrumCanvas(
    viewModel: PlayerViewModel,
    prefs: VisualizerPrefs,
    style: SpectrumStyle,
    sensitivityDraft: Float,
    drawColors: SpectrumDrawColors,
    phaseState: State<Float>,
    tapLive: Boolean,
    spectrogramHistory: SpectrogramHistory,
    modifier: Modifier = Modifier
) {
    // Frame-rate flows held as State - dereferenced only in the draw scope.
    val wave = viewModel.waveState.collectAsStateWithLifecycle()
    val audio = viewModel.audioState.collectAsStateWithLifecycle()
    val spectrum = viewModel.spectrumState.collectAsStateWithLifecycle()
    val colors = viewModel.colorState.collectAsStateWithLifecycle()

    // v1 tier: balanced DSP (1024/40/~30Hz). Display count + glow now come
    // from the user's Lab config; the audio thread is never re-allocated.
    val tier = SpectrumTier.BALANCED
    val displayBands = prefs.bandDensity

    // Data source: real 40-band frames when fresh, else interpolation.
    // Fallback synthesis -> resample -> sensitivity all happen inside the
    // Canvas DRAW lambda below, through one preallocated holder. Two reasons:
    // the holder is MUTATED (mutating remembered state during composition is
    // invisible to Compose), and every array is reused, so a ~30Hz emission
    // allocates nothing at all. visualizer_plan 13 + 14.
    val levelSource = remember(displayBands) { SpectrumLevelSource(displayBands) }

    Canvas(modifier = modifier) {
        // Draw-phase only: preallocated holder, zero allocation, and nothing
        // mutated while composing. Hot State values are read HERE - draw
        // invalidation only, never composition.
        val spectrumValue = spectrum.value
        val waveValue = wave.value
        val audioValue = audio.value
        val colorsValue = colors.value

        val beatKick = if (waveValue.hasBeat && waveValue.isActive) 1.12f else 1f

        levelSource.update(
            source = spectrumValue.bands,
            sourcePeaks = spectrumValue.peaks,
            tapLive = tapLive,
            waveActive = waveValue.isActive,
            energy = waveValue.energy,
            pulseAmount = colorsValue.pulseAmount,
            // Plan 5.5: reactivityCurve scales the response.
            sensitivity = sensitivityDraft * prefs.reactivityCurve
        )
        drawSpectrum(
            style = style,
            levels = levelSource.levels,
            peaks = if (levelSource.hasPeaks) levelSource.peaks else null,
            colors = drawColors,
            beatKick = beatKick,
            bassEnergy = audioValue.bassEnergy,
            midEnergy = audioValue.midEnergy,
            trebleEnergy = audioValue.trebleEnergy,
            overallEnergy = audioValue.energy,
            // Draw-phase read: the phase State is consumed here, inside the
            // Canvas draw scope, so frame-rate updates never recompose.
            phase = phaseState.value,
            glowEnabled = prefs.glowEnabled &&
                SpectrumConfig.glowEnabledFor(tier),
            glowScale = prefs.glowAmount,
            history = spectrogramHistory
        )
        // Time-domain polyline retained in Classic for precision. Built here
        // (not in composition) so the normalized snapshot is never a
        // composition-level read of the frame-rate audioState.
        if (style == SpectrumStyle.CLASSIC) {
            val raw = audioValue.waveform
            val waveform = FloatArray(minOf(raw.size, WAVEFORM_SAMPLES)) { i ->
                val v = raw[i]
                when {
                    v > 1.5f -> ((v - 128f) / 128f).coerceIn(-1f, 1f)
                    v < -1.5f -> (v / 128f).coerceIn(-1f, 1f)
                    else -> v.coerceIn(-1f, 1f)
                }
            }
            if (waveform.size >= 2) {
                val w = size.width
                val h = size.height
                val cy = h / 2f
                val path = Path().apply {
                    waveform.forEachIndexed { i, v ->
                        val x = i / (waveform.size - 1f) * w
                        val y = cy - v * h * 0.28f
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                drawPath(
                    path = path,
                    color = drawColors.primary.copy(alpha = 0.55f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}

private fun styleLabel(style: SpectrumStyle): String = when (style) {
    SpectrumStyle.AURORA -> "Aurora"
    SpectrumStyle.CLASSIC -> "Classic"
    SpectrumStyle.SPECTROGRAM -> "Spectrogram"
    SpectrumStyle.SHAPE -> "Orbital"
    SpectrumStyle.LINE -> "Line"
    SpectrumStyle.DOT -> "Dot"
    SpectrumStyle.WAVEFORM -> "Waveform"
}

private val VisualizerTheme.label: String
    get() = when (this) {
        VisualizerTheme.ALBUM_ACCENT -> "Album"
        VisualizerTheme.NEON_CYAN -> "Cyan"
        VisualizerTheme.AURORA_VIOLET -> "Violet"
        VisualizerTheme.MONO -> "Mono"
    }

@Composable
private fun LabLabel(text: String) {
    Text(
        text = text,
        style = RikoTypography.Overline,
        color = RikoTextTertiary,
        letterSpacing = 1.5.sp
    )
}

@Composable
private fun StatusChip(
    text: String,
    color: Color,
    leading: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .clip(RikoShape.Pill)
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) leading()
        Text(
            text = text,
            style = RikoTypography.Overline,
            color = color,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun PlaybackErrorBanner(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RikoShape.Card)
            .background(StatusError.copy(alpha = 0.12f))
            .border(1.dp, StatusError.copy(alpha = 0.35f), RikoShape.Card)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = StatusError,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = message.ifBlank { "Playback failed" },
            style = RikoTypography.Caption,
            color = RikoTextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .clip(RikoShape.Pill)
                .background(StatusError.copy(alpha = 0.22f))
                .heightIn(min = 44.dp)
                .padding(horizontal = 16.dp)
                .semantics { contentDescription = "Retry playback" }
                .then(
                    Modifier.clickableNoRipple(onRetry)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Retry",
                style = RikoTypography.Button,
                color = RikoTextPrimary
            )
        }
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    composed {
        clickable(
            // remember: a fresh MutableInteractionSource per recomposition leaks
            // interaction subscriptions (plan 1.3 "unnecessary interaction sources").
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    }

@Composable
private fun LabOffState(
    onEnable: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.GraphicEq,
            contentDescription = null,
            tint = RikoTextTertiary,
            modifier = Modifier.size(40.dp)
        )
        Text(
            text = "Visualizer is off",
            style = RikoTypography.Section,
            color = RikoTextPrimary,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Turn it on to see the live spectrum",
            style = RikoTypography.Caption,
            color = RikoTextTertiary,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onEnable,
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentViolet,
                contentColor = Color.White
            ),
            shape = RikoShape.Pill,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 28.dp,
                vertical = 12.dp
            ),
            modifier = Modifier.semantics { contentDescription = "Turn on visualizer" }
        ) {
            Text(text = "Turn on", style = RikoTypography.Button)
        }
    }
}

@Composable
private fun LabEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = RikoTextTertiary,
            modifier = Modifier.size(40.dp)
        )
        Text(
            text = "Nothing playing",
            style = RikoTypography.Section,
            color = RikoTextPrimary,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Start a track to see the live spectrum",
            style = RikoTypography.Caption,
            color = RikoTextTertiary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LabPill(
    text: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(44.dp)
            .clip(RikoShape.Pill)
            .background(if (selected) accent.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.07f))
            .then(
                Modifier.clickableNoRipple(onClick)
            )
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = RikoTypography.Label,
            color = if (selected) Color.White else RikoTextTertiary
        )
    }
}

/**
 * Equal-width segmented row (mode / detail). The whole pill is one 44dp
 * touch target per option; selection reads through colour + semantics.
 */
@Composable
private fun <T> LabSegmentedRow(
    options: List<Pair<T, String>>,
    selectedIndex: Int,
    accent: Color,
    onSelected: (T) -> Unit,
    compact: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RikoShape.Pill)
            .background(Color.White.copy(alpha = 0.08f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, (value, label) ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(if (compact) 36.dp else 44.dp)
                    .clip(RikoShape.Pill)
                    .background(
                        if (selected) accent.copy(alpha = 0.85f)
                        else Color.Transparent
                    )
                    .selectable(
                        selected = selected,
                        onClick = { onSelected(value) },
                        role = androidx.compose.ui.semantics.Role.Tab
                    )
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = if (compact) RikoTypography.Caption else RikoTypography.Label,
                    color = if (selected) Color.White else RikoTextTertiary
                )
            }
        }
    }
}

@Composable
private fun LabTransportButton(
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    icon: @Composable (Color) -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(48.dp)
            .alpha(if (enabled) 1f else 0.35f)
    ) {
        icon(RikoTextPrimary)
        // Hidden label keeps TalkBack announcing a disabled-but-present control.
        Text(
            text = contentDescription,
            color = Color.Transparent,
            style = RikoTypography.Caption,
            modifier = Modifier.semantics { this.contentDescription = contentDescription }
        )
    }
}

/**
 * Plan 5.4/5.5: the studio control block � four rotary knobs (sensitivity,
 * glow, motion speed, detail density) in one interaction language, plus the
 * user preset bar (tap = restore, long-press = rename/delete) and Reset.
 * Knobs hold local drafts and commit on release � no DataStore spam.
 */
@Composable
private fun ParameterSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    onChange: (Float) -> Unit,
    onCommit: () -> Unit,
    accent: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = RikoTypography.Caption,
                color = RikoTextSecondary
            )
            Text(
                text = valueText,
                style = RikoTypography.Caption,
                color = accent,
                fontWeight = FontWeight.SemiBold
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            onValueChangeFinished = onCommit,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}

@Composable
private fun StudioControls(
    prefs: com.rikodev.music.android.data.repository.VisualizerPrefs,
    accent: Color,
    presets: List<com.rikodev.music.android.data.repository.UserVisualizerPreset>,
    onChange: ((com.rikodev.music.android.data.repository.VisualizerPrefs) -> com.rikodev.music.android.data.repository.VisualizerPrefs) -> Unit,
    onApplyPreset: (com.rikodev.music.android.data.repository.UserVisualizerPreset) -> Unit,
    onSavePreset: (String) -> Unit,
    onRenamePreset: (String, String) -> Unit,
    onDeletePreset: (String) -> Unit,
    onReset: () -> Unit
) {
    var sens by remember(prefs.sensitivity) { mutableFloatStateOf(prefs.sensitivity) }
    var glow by remember(prefs.glowAmount) { mutableFloatStateOf(prefs.glowAmount) }
    var motion by remember(prefs.motionSpeed) { mutableFloatStateOf(prefs.motionSpeed) }
    var detail by remember(prefs.bandDensity) { mutableFloatStateOf(prefs.bandDensity / 48f) }

    LabLabel("STUDIO")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RikoRotaryKnob(
            value = knobValueFrom(0.5f..1.5f, sens),
            onValueChange = { sens = knobValueTo(0.5f..1.5f, it) },
            label = "Sensitivity",
            onCommit = { onChange { p -> p.copy(sensitivity = sens.coerceIn(0.5f, 1.5f)) } }
        )
        RikoRotaryKnob(
            value = glow,
            onValueChange = { glow = it.coerceIn(0f, 1f) },
            label = "Glow",
            onCommit = { onChange { p -> p.copy(glowAmount = glow) } }
        )
        RikoRotaryKnob(
            value = knobValueFrom(0.25f..2f, motion),
            onValueChange = { motion = knobValueTo(0.25f..2f, it) },
            label = "Motion",
            onCommit = { onChange { p -> p.copy(motionSpeed = motion.coerceIn(0.25f, 2f)) } }
        )
        RikoRotaryKnob(
            value = detail,
            onValueChange = { detail = it.coerceIn(0f, 1f) },
            label = "Detail",
            onCommit = {
                val bands = when {
                    detail < 0.33f -> 32
                    detail < 0.66f -> 40
                    else -> 48
                }
                onChange { p -> p.copy(bandDensity = bands) }
            }
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Parameters sliders (Samsung One UI 9.0 style)
    LabLabel("PARAMETERS")
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        var barWidth by remember(prefs.barWidth) { mutableFloatStateOf(prefs.barWidth) }
        ParameterSlider(
            label = "Bar width",
            value = barWidth,
            valueRange = 0.5f..2f,
            valueText = "%.1f".format(barWidth),
            onChange = { barWidth = it },
            onCommit = { onChange { p -> p.copy(barWidth = barWidth.coerceIn(0.5f, 2f)) } },
            accent = accent
        )
        var spacing by remember(prefs.barSpacing) { mutableFloatStateOf(prefs.barSpacing) }
        ParameterSlider(
            label = "Spacing",
            value = spacing,
            valueRange = 0f..1f,
            valueText = "%.2f".format(spacing),
            onChange = { spacing = it },
            onCommit = { onChange { p -> p.copy(barSpacing = spacing.coerceIn(0f, 1f)) } },
            accent = accent
        )
        var smoothing by remember(prefs.smoothing) { mutableFloatStateOf(prefs.smoothing) }
        ParameterSlider(
            label = "Smoothing",
            value = smoothing,
            valueRange = 0f..1f,
            valueText = "%.2f".format(smoothing),
            onChange = { smoothing = it },
            onCommit = { onChange { p -> p.copy(smoothing = smoothing.coerceIn(0f, 1f)) } },
            accent = accent
        )
    }

    Spacer(modifier = Modifier.height(16.dp))
    LabLabel("PRESETS")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        presets.forEach { preset ->
            var renameOpen by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .clip(RikoShape.Pill)
                    .background(Color.White.copy(alpha = 0.10f))
                    .combinedClickable(
                        onClick = { onApplyPreset(preset) },
                        onLongClick = { renameOpen = true }
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = preset.name,
                    style = RikoTypography.Caption,
                    color = Color.White
                )
            }
            if (renameOpen) {
                PresetDialog(
                    initialName = preset.name,
                    accent = accent,
                    onDismiss = { renameOpen = false },
                    onRename = { name ->
                        renameOpen = false
                        onRenamePreset(preset.id, name)
                    },
                    onDelete = {
                        renameOpen = false
                        onDeletePreset(preset.id)
                    }
                )
            }
        }
        StudioActionChip(label = "Save", accent = accent) { onSavePreset("Preset ${presets.size + 1}") }
        StudioActionChip(label = "Reset", accent = accent, onClick = onReset)
    }
}

@Composable
private fun StudioActionChip(
    label: String,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RikoShape.Pill)
            .background(accent.copy(alpha = 0.22f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text = label, style = RikoTypography.Caption, color = accent)
    }
}

@Composable
private fun PresetDialog(
    initialName: String,
    accent: Color,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1B1230),
        title = { Text("Preset", color = Color.White) },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.White.copy(alpha = 0.08f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                    focusedIndicatorColor = accent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        },
        confirmButton = {
            Text(
                text = "Rename",
                color = accent,
                modifier = Modifier.clickable { onRename(name.ifBlank { initialName }) }.padding(8.dp)
            )
        },
        dismissButton = {
            Row {
                Text(
                    text = "Delete",
                    color = RikoError,
                    modifier = Modifier.clickable { onDelete() }.padding(8.dp)
                )
                Text(
                    text = "Cancel",
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp)
                )
            }
        }
    )
}
