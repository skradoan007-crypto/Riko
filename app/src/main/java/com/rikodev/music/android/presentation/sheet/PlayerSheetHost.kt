package com.rikodev.music.android.presentation.sheet

import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.imageLoader
import coil.request.ImageRequest
import com.rikodev.music.android.core.designsystem.riko.components.ARTWORK_PX_ROW
import com.rikodev.music.android.core.designsystem.riko.components.RikoArtwork
import com.rikodev.music.android.core.designsystem.riko.components.rememberAlbumArtworkModel
import com.rikodev.music.android.core.designsystem.riko.shell.LocalRikoShellMetrics
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextTertiary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography
import com.rikodev.music.android.domain.model.Song
import com.rikodev.music.android.presentation.nowplaying.PlayerViewModel
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — PLAYER SHEET HOST (M2: target layout + morph choreography)
//
// Overlay above NavHost, below shell chrome. Drag-driven Mini <-> Full morph.
//
// M2 structure (transition plan Phase 2):
//   ┌─ BoxWithConstraints (fillMaxSize) ─────────────────────────────┐
//   │  [translating Box]  PlayerSheetFull   ← offset + drag surface  │
//   │  [SheetMiniBar]     BottomCenter, OUTSIDE the translate box    │
//   └────────────────────────────────────────────────────────────────┘
//   - Anchors: Full = 0f, Mini = full screen height (full content parks
//     entirely below the viewport). Stable regardless of shell nav state.
//   - Mini is tap-to-expand only; drag collapses from the Full content.
//   - Mini alpha = 1 - stagger(progress, 0, 0.35) — fades as the morph
//     begins; tap disabled once settled at Full.
//   - Sheet progress 0..1 is read ONLY inside graphicsLayer lambdas.
//   - Artwork feed (256px, allowHardware=false → Color Brain) and
//     closeAllOverlays() live here — the blanked NP route no longer owns them.
//   - Background screens are never destroyed (pure overlay).
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerSheetHost(
    sheetState: PlayerSheetState,
    viewModel: PlayerViewModel,
    reserveNavSpace: Boolean,
    onCollapse: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenInfo: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenVisualizer: () -> Unit,
    onOpenAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val metrics = LocalRikoShellMetrics.current
    val context = LocalContext.current
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val queueIndex by viewModel.currentQueueIndex.collectAsStateWithLifecycle()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxHPx = with(density) { maxHeight.toPx() }
        val navBarsDp = with(density) {
            WindowInsets.navigationBars.getBottom(this).toDp()
        }
        // Mini dock mirrors the shell bottom stack (nav capsule + margin +
        // gap above system inset). Driven by reserveNavSpace (isTopLevel),
        // NOT the live nav visibility — anchors never shift mid-morph.
        val bottomPadDp = navBarsDp + if (reserveNavSpace) {
            metrics.bottomNavHeight + metrics.bottomMargin + metrics.interLayerGap
        } else 0.dp

        // M2 anchors: Mini = full-height travel — the full content parks
        // entirely below the viewport; the mini bar is a fixed dock.
        LaunchedEffect(maxHPx) {
            sheetState.installAnchors(DraggableAnchors {
                SheetAnchor.Full at 0f
                SheetAnchor.Mini at maxHPx
            })
        }

        // ── ARTWORK FEED — palette source of truth (moved from NP route) ──
        // 256px decode, allowHardware=false so Palette can read pixels.
        // PERF: Image decode moved to Dispatchers.IO (was Main thread).
        // P0 FIX (album-art mix-up): resolve embedded art per-song — the
        // MediaStore albumart URI collides across YouTube downloads.
        LaunchedEffect(currentSong?.id, currentSong?.uri) {
            val song = currentSong
            if (song == null) {
                viewModel.onArtworkLoaded(null)
                return@LaunchedEffect
            }
            val data = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.rikodev.music.android.core.designsystem.riko.components
                    .resolveArtworkData(context, song.id, song.albumId, song.uri)
            }
            if (data == null) {
                viewModel.onArtworkLoaded(null)
                return@LaunchedEffect
            }
            val bitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    if (data is android.graphics.Bitmap) {
                        data
                    } else {
                        val request = ImageRequest.Builder(context)
                            .data(data)
                            .size(128)
                            .allowHardware(false)
                            .build()
                        val result = context.imageLoader.execute(request)
                        (result.drawable as? BitmapDrawable)?.bitmap
                    }
                }.getOrNull()
            }
            viewModel.onArtworkLoaded(bitmap)
        }

        // ── M4 PRELOAD — warm Coil's memory cache for adjacent tracks ────
        // No palette publish here (the engine is single-slot by design);
        // warming the cache makes the hero crossfade AND the palette
        // extraction on track advance instant. Cheap: two 256px decodes.
        // P0 FIX (cache poison + album-art mix-up): same resolveArtworkData
        // pipeline as the UI, so warmup never caches a colliding URI under
        // a song-scoped key.
        LaunchedEffect(currentSong?.id, queueIndex) {
            if (queue.isEmpty()) return@LaunchedEffect
            listOf(queueIndex - 1, queueIndex + 1).forEach { i ->
                val s = queue.getOrNull(i)?.song ?: return@forEach
                val data = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.rikodev.music.android.core.designsystem.riko.components
                        .resolveArtworkData(context, s.id, s.albumId, s.uri)
                } ?: return@forEach
                // P0 FIX: enqueue without listener + activity destroyed =
                // IllegalStateException "You must collect the progress flow".
                // runCatching absorbs the crash; listener consumes the result.
                runCatching {
                    context.imageLoader.enqueue(
                        ImageRequest.Builder(context)
                            .data(data)
                            .size(128)
                            .allowHardware(false)
                            .listener(
                                onSuccess = { _, _ -> },
                                onError = { _, _ -> }
                            )
                            .build()
                    )
                }
            }
        }

        // Track change closes queue/lyrics overlays (moved from NP route).
        LaunchedEffect(currentSong?.id) {
            viewModel.closeAllOverlays()
        }

        // Null song after playback ends hides the sheet; a fresh song after
        // null re-opens at Mini (auto-advance between songs keeps Full).
        var lastSongId by remember { mutableStateOf<Long?>(null) }
        LaunchedEffect(currentSong?.id) {
            if (lastSongId == null && currentSong != null) sheetState.snapToMini()
            lastSongId = currentSong?.id
        }

        // M6: Aurora spectrum overlay visibility (sheet-local).
        var visualizerVisible by remember { mutableStateOf(false) }

        // Delegated properties never smart-cast — copy to a local val first.
        val song = currentSong
        if (song == null || !sheetState.anchorsReady) return@BoxWithConstraints

        // System back: Full collapses, Mini falls through to nav pop.
        // P0 FIX (back crash): PredictiveBackHandler REQUIRES the progress
        // Flow to be collected — AndroidX throws
        // IllegalStateException "You must collect the progress flow" otherwise.
        PredictiveBackHandler(enabled = sheetState.settled == SheetAnchor.Full) { progress ->
            try {
                progress.collect { }
                sheetState.collapse()
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                throw e
            }
        }

        val draggable = sheetState.draggable

        // Sheet progress 0 (Mini) .. 1 (Full) — the single motion driver.
        // The lambda reads requireOffset() (observable float state) and must
        // only ever be invoked from draw/layout/graphicsLayer lambdas.
        val sheetProgress: () -> Float = remember(maxHPx) {
            { PlayerSheetReducer.progressFor(draggable.requireOffset(), maxHPx) }
        }

        // ── Translating full content ──────────────────────────────────────
        // The trailing tap consumer blocks taps from falling through the
        // sheet to the home screen behind it (children still win — Main
        // pass dispatches deepest-first).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = draggable.requireOffset() }
                .anchoredDraggable(draggable, Orientation.Vertical)
                .pointerInput(Unit) { detectTapGestures { } }
        ) {
            PlayerSheetFull(
                viewModel = viewModel,
                progress = sheetProgress,
                onCollapse = onCollapse,
                onOpenQueue = onOpenQueue,
                onOpenEqualizer = onOpenEqualizer,
                onOpenLyrics = onOpenLyrics,
                onOpenInfo = onOpenInfo,
                onOpenSleepTimer = onOpenSleepTimer,
                onOpenVisualizer = {
                    onOpenVisualizer()
                    visualizerVisible = true
                },
                onOpenAddToPlaylist = onOpenAddToPlaylist
            )
        }

        // ── Mini bar — fixed dock, outside the translating box ────────────
        // Fades out over the first 30% of the morph (plan Phase 2: mini
        // controls 0 -> 0.3 fade out); tap expands when settled at Mini.
        SheetMiniBar(
            song = song,
            viewModel = viewModel,
            onTap = { scope.launch { sheetState.expand() } },
            tapEnabled = sheetState.settled == SheetAnchor.Mini,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    alpha = 1f - PlayerSheetReducer.stagger(sheetProgress(), 0f, 0.30f)
                }
                .padding(horizontal = 16.dp)
                .padding(bottom = bottomPadDp)
                .fillMaxWidth()
                .height(metrics.miniPlayerHeight)
        )

        // ── M6: Aurora spectrum overlay — full-screen, above the sheet ────
        // Declared after the sheet's own back handler, so back closes the
        // overlay first (LIFO) and only then collapses the sheet.
        if (visualizerVisible) {
            PredictiveBackHandler(enabled = sheetState.settled == SheetAnchor.Full) { progress ->
                try {
                    progress.collect { }
                    visualizerVisible = false
                } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                    throw e
                }
            }
            // Nested MDNA back handling must register after the overlay handler.
            AuroraSpectrum(
                viewModel = viewModel,
                onClose = { visualizerVisible = false },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** Functional mini bar: artwork, title, play toggle, hairline. Replaces shell mini. */
@Composable
private fun SheetMiniBar(
    song: Song,
    viewModel: PlayerViewModel,
    onTap: () -> Unit,
    tapEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    // No hot reads here — the 4Hz position ticker collects inside
    // MiniPlayRing, so artwork/title stay composed across every tick.
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xE6140A24))
            .clickable(enabled = tapEnabled, onClick = onTap)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val artModel = rememberAlbumArtworkModel(song.albumId, ARTWORK_PX_ROW, songId = song.id, songUri = song.uri)
            RikoArtwork(
                model = artModel,
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
                fallback = {}
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = RikoTypography.Body,
                    color = RikoTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = RikoTypography.Caption,
                    color = RikoTextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            MiniPlayRing(
                viewModel = viewModel,
                onToggle = { viewModel.togglePlayPause() }
            )
        }
    }
}

/**
 * Mini play button with circular progress ring (plan Phase 5).
 * The ring replaces the old top-edge hairline — progress lives on the
 * control itself, readable at mini scale.
 *
 * Collects the 4Hz position ticker HERE so only this ring recomposes —
 * the surrounding mini bar (artwork, titles) stays composed.
 */
@Composable
private fun MiniPlayRing(
    viewModel: PlayerViewModel,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val positionMs by viewModel.playbackPosition.collectAsStateWithLifecycle()
    val durationMs by viewModel.duration.collectAsStateWithLifecycle()
    val progress = if (durationMs > 0) {
        positionMs.toFloat() / durationMs.toFloat()
    } else 0f

    IconButton(onClick = onToggle, modifier = modifier.size(48.dp)) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(3.dp)) {
                drawArc(
                    color = Color.White.copy(alpha = 0.18f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx())
                )
                drawArc(
                    color = Color(0xFFFF2E88),
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = RikoTextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
