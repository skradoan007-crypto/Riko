package com.rikodev.music.android.presentation.sheet

import androidx.compose.animation.core.Animatable
import com.rikodev.music.android.core.designsystem.components.WaveformTimeline
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.request.ImageRequest
import com.rikodev.music.android.core.designsystem.animation.rememberReduceMotion
import com.rikodev.music.android.core.designsystem.riko.components.ARTWORK_PX_HERO
import com.rikodev.music.android.core.designsystem.riko.components.ARTWORK_PX_ROW
import com.rikodev.music.android.core.designsystem.riko.components.ARTWORK_PX_SHELF
import com.rikodev.music.android.core.designsystem.riko.components.RikoArtwork
import com.rikodev.music.android.core.designsystem.riko.components.RikoHapticEvent
import com.rikodev.music.android.core.designsystem.riko.components.albumArtCacheKey
import com.rikodev.music.android.core.designsystem.riko.components.rememberAlbumArtworkModel
import com.rikodev.music.android.core.designsystem.riko.components.rememberRikoHaptic
import com.rikodev.music.android.core.designsystem.riko.components.resolveArtworkData
import com.rikodev.music.android.core.designsystem.riko.theme.AccentViolet
import com.rikodev.music.android.core.designsystem.riko.theme.RikoCornerRadius
import com.rikodev.music.android.core.designsystem.riko.theme.RikoError
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextSecondary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextTertiary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoShape
import com.rikodev.music.android.core.designsystem.riko.theme.RikoSpacing
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography
import com.rikodev.music.android.core.designsystem.riko.theme.RikoThemeV2
import com.rikodev.music.android.core.experience.ColorState
import com.rikodev.music.android.core.experience.wave.WaveState
import com.rikodev.music.android.presentation.nowplaying.PlasmaTimeline
import com.rikodev.music.android.domain.model.RepeatMode
import com.rikodev.music.android.domain.model.ShuffleMode
import com.rikodev.music.android.domain.model.Song
import com.rikodev.music.android.presentation.nowplaying.PlayerViewModel
import com.rikodev.music.android.presentation.player.HeroAlbum
import com.rikodev.music.android.presentation.player.NowPlayingBackground
import com.rikodev.music.android.presentation.player.PlayPauseHeroButton
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — PLAYER SHEET FULL (M2: target layout + morph choreography)
//
// The full Now Playing surface, rendered inside the translating sheet box.
// Layout mirrors the UI-target reference top → bottom:
//   1. Glass top capsule — back · NOW PLAYING · star · overflow menu
//   2. Hero album art (rounded square, glow/mat/glass from HeroAlbum)
//   3. Metadata — title (marquee) · artist · album (year)
//   4. flexible gap
//   5. Gradient progress bar + glowing thumb + elapsed/total
//   6. Transport — shuffle · prev · glass play (72dp) · next · queue
//   7. Up Next card — next track preview + Queue (N tracks)
//
// MORPH RULES (M2):
//   - Every section reads sheet progress ONLY inside graphicsLayer lambdas
//     via [PlayerSheetReducer.stagger] — zero recomposition while dragging.
//   - Stagger windows: bg 0.02–0.22 · capsule 0.2–0.5 · art 0.15–0.5 ·
//     metadata 0.3–0.6 · progress 0.4–0.7 · transport 0.45–0.8 ·
//     up-next 0.5–0.8. Sections rise 24dp as they fade in.
//   - Color/beat reactivity stays isolated in small subtrees so energy
//     ticks never recompose the whole column (RC-01 discipline).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PlayerSheetFull(
    viewModel: PlayerViewModel,
    progress: () -> Float,
    onCollapse: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenInfo: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenVisualizer: () -> Unit,
    onOpenDna: () -> Unit = onOpenVisualizer,
    onOpenAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Low-frequency collections only — position/duration/energy collect
    // inside their small leaf composables (4Hz/30Hz isolation).
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val queueIndex by viewModel.currentQueueIndex.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isCurrentSongFavorite.collectAsStateWithLifecycle()
    val shuffleMode by viewModel.shuffleMode.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val playbackError by viewModel.playbackError.collectAsStateWithLifecycle()

    val song = currentSong ?: return
    val haptic = rememberRikoHaptic()
    val reduceMotion = rememberReduceMotion()

    // P0 FIX (album-art mix-up): resolve per-song embedded art for the
    // ambient backdrop — the raw MediaStore albumart URI collides across
    // songs that share a bad albumId (YouTube downloads).
    val bgContext = LocalContext.current
    val ambientArtwork = remember(song.id, song.uri, song.albumId) {
        resolveArtworkData(bgContext, song.id, song.albumId, song.uri)
    }
    val nextSong = queue.getOrNull(queueIndex + 1)?.song

    // G-5: prev/next disabled at the queue ends when repeat is off and
    // shuffle isn't reshuffling the order (spec A104 matrix).
    val atStart = queueIndex <= 0
    val atEnd = queueIndex >= queue.size - 1
    val canPrev = shuffleMode != ShuffleMode.OFF || repeatMode != RepeatMode.OFF || !atStart
    val canNext = shuffleMode != ShuffleMode.OFF || repeatMode != RepeatMode.OFF || !atEnd

    // ── M3 windowed pager: prev / current / next only (plan Phase 3) ────
    // The full queue never enters the pager — big queues stay cheap and
    // shuffle/remove/reorder just rebuilds the window around Media3's
    // index (which never jumps). centerPos is the current track's slot.
    val windowSongs: List<Song> = remember(song.id, queueIndex, queue) {
        buildList {
            queue.getOrNull(queueIndex - 1)?.song?.let { add(it) }
            add(song)
            queue.getOrNull(queueIndex + 1)?.song?.let { add(it) }
        }
    }
    val centerPos = if (queue.getOrNull(queueIndex - 1) != null) 1 else 0

    // ── Phase 4 neighbor palette cache (songId → last settled palette) ──
    // Collected in an effect (never composition) so energy ticks don't
    // recompose this column. Missing entries → hold current color.
    // Capped at 20 entries (LRU-ish eviction): big-queue swipes can't grow
    // this map without bound; oldest inserted entry is dropped first.
    val paletteCache = remember { LinkedHashMap<Long, ColorState>(20, 0.75f, true) }
    LaunchedEffect(song.id) {
        viewModel.colorState.collect { cs ->
            if (cs.isFromArtwork) {
                paletteCache[song.id] = cs
                if (paletteCache.size > 20) {
                    val oldest = paletteCache.keys.firstOrNull()
                    if (oldest != null) paletteCache.remove(oldest)
                }
            }
        }
    }

    // ── Pager ↔ Media3 sync (plan Phase 3, both directions) ─────────────
    // Media3 is the source of truth; the pager is its shadow. Fresh
    // currentQueueIndex reads at effect time make redundant jumps
    // (which would restart the track) impossible. scrollToPage cancels
    // any running animation, so rapid next/prev always wins.
    val pagerState = key(song.id) {
        // Fresh state per track: a rebuilt window can never strand the
        // pager on an out-of-bounds page, and every track opens centered.
        rememberPagerState(
            initialPage = centerPos,
            pageCount = { windowSongs.size }
        )
    }
    LaunchedEffect(song.id, centerPos) {
        if (pagerState.currentPage != centerPos && !pagerState.isScrollInProgress) {
            pagerState.scrollToPage(centerPos)
        }
    }
    // Skips the effect's initial fire for each pager lifetime — a fresh
    // state settles on page 0 before the centering scroll lands.
    // Edge guard (plan Phase 3: first/last track, repeat off → no wrap):
    // out-of-bounds targets are ignored so the pager feels resistance at
    // the edges instead of wrapping or restarting the track.
    var pagerAudited by remember(song.id) { mutableStateOf(false) }
    LaunchedEffect(pagerState.settledPage) {
        val settled = pagerState.settledPage
        if (!pagerAudited) {
            pagerAudited = true
            return@LaunchedEffect
        }
        if (settled != centerPos) haptic.fire(RikoHapticEvent.SEEK_SETTLE)
        val targetQueueIndex = queueIndex + (settled - centerPos)
        if (targetQueueIndex !in queue.indices) return@LaunchedEffect
        if (targetQueueIndex != viewModel.currentQueueIndex.value
        ) {
            viewModel.skipToQueueItem(targetQueueIndex)
        }
    }

    // ── Live neighbor blend (plan Phase 4): continuous shift, no gray ───
    // Fraction of the way toward the adjacent page + that page's cached
    // palette (or a deterministic hash fallback). The current bg always
    // stays underneath, so a missing palette holds color instead of
    // flashing neutral.
    val scrollPos = pagerState.currentPage + pagerState.currentPageOffsetFraction
    val drift = (scrollPos - centerPos).coerceIn(-1f, 1f)
    val neighborSong = windowSongs.getOrNull(
        (centerPos + if (drift >= 0f) 1 else -1).coerceIn(windowSongs.indices)
    )
    val neighborColor = neighborSong?.let { ns ->
        paletteCache[ns.id]?.primary ?: fallbackColorFor(ns)
    } ?: Color.Transparent
    val blendAlpha = abs(drift) * 0.55f

    Box(modifier = modifier.fillMaxSize()) {
        // ── Aurora background — the sheet's opaque base, fades in first ──
        NowPlayingBackground(
            colorState = viewModel.colorState,
            waveState = viewModel.waveState,
            artworkData = ambientArtwork,
            isPlaying = isPlaying,
            // C5: aurora frame loop pauses while the sheet is collapsed.
            isVisible = { progress() > 0.05f },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = PlayerSheetReducer.stagger(progress(), 0.02f, 0.22f)
                }
        )

        // ── Neighbor wash — alpha read in the draw phase; zero at rest ───
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = blendAlpha }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(neighborColor, Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = RikoSpacing.LG)
                .padding(bottom = RikoSpacing.SM),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── 1. Top glass capsule ──────────────────────────────────────
            NpTopCapsule(
                isFavorite = isFavorite,
                onCollapse = onCollapse,
                onToggleFavorite = { viewModel.toggleFavoriteCurrentSong() },
                onOpenEqualizer = onOpenEqualizer,
                onOpenLyrics = onOpenLyrics,
                onOpenVisualizer = onOpenVisualizer,
                onOpenDna = onOpenDna,
                onOpenInfo = onOpenInfo,
                onOpenSleepTimer = onOpenSleepTimer,
                onOpenAddToPlaylist = onOpenAddToPlaylist,
                reduceMotion = reduceMotion,
                modifier = Modifier.sheetSection(progress, 0.20f, 0.50f, reduceMotion)
            )

            Spacer(modifier = Modifier.height(RikoSpacing.XL))

            // ── 2. Windowed artwork pager (plan Phase 3) ───────────────────
            // Per-page: art + title + artist (live during the swipe).
            // Fixed below: seekbar, transport, up-next. Neighbors shrink
            // and fade by |pageOffset|; the block keeps the art stagger
            // window (0.15–0.5).
            HorizontalPager(
                state = pagerState,
                pageSpacing = 24.dp,
                beyondViewportPageCount = 1,
                key = { page -> "${page - centerPos}:${windowSongs.getOrNull(page)?.id}" },
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val s = PlayerSheetReducer.stagger(progress(), 0.15f, 0.50f)
                        alpha = s
                        val scale = if (reduceMotion) 1f else 0.92f + 0.08f * s
                        scaleX = scale
                        scaleY = scale
                        translationY = if (reduceMotion) 0f else (1f - s) * 24.dp.toPx()
                    }
            ) { page ->
                val pageSong = windowSongs.getOrNull(page) ?: song
                val pageOffset = (page - pagerState.currentPage) +
                        pagerState.currentPageOffsetFraction
                ArtworkPage(
                    pageSong = pageSong,
                    isCurrent = page == centerPos,
                    pageOffset = pageOffset,
                    reduceMotion = reduceMotion,
                    progress = progress,
                    colorState = viewModel.colorState,
                    waveState = viewModel.waveState
                )
            }

            // Flexible gap — pushes playback controls toward the bottom,
            // matching the target's airy mid-section.
            Spacer(modifier = Modifier.weight(1f))

            // ── 5. Progress bar + times ───────────────────────────────────
            // Plan Phase 2: full seekbar enters from p 0.35.
            // Real-audio liquid ribbon: geometry comes from WaveformRepository
            // (cached PCM peaks/RMS), clipped at the playhead.
            val wfState by viewModel.realWaveformState.collectAsStateWithLifecycle()
            val readyWaveform = wfState as? com.rikodev.music.android.audiofx.WaveformState.Ready
            val wfData = readyWaveform?.takeIf { it.songId == song.id }?.data
            NpProgressBar(
                trackId = song.id,
                samplePosition = { viewModel.timelinePositionMs(song.id) },
                sampleAmplitude = { viewModel.timelineAmplitude.value },
                positionFlow = viewModel.playbackPosition,
                durationFlow = viewModel.duration,
                colorState = viewModel.colorState,
                isPlaying = isPlaying,
                onSeek = { viewModel.seekToProgress(it) },
                waveformData = wfData,
                isTimelineVisible = { progress() > 0.35f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .sheetSection(progress, 0.35f, 0.65f, reduceMotion)
            )

            Spacer(modifier = Modifier.height(RikoSpacing.SM))

            // ── 6. Transport ──────────────────────────────────────────────
            // Plan Phase 2: prev/play/next stagger in right after the seekbar.
            // G-5 error banner — Media3 failures land here with retry.
            if (playbackError != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .clip(RikoShape.Card)
                        .background(RikoError.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Playback error — tap retry",
                        style = RikoTypography.Secondary,
                        color = RikoError,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.retry() }) {
                        Text("Retry", color = AccentViolet, style = RikoTypography.Label)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .sheetSection(progress, 0.40f, 0.75f, reduceMotion),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular transport button with glow (Samsung One UI 9.0 style)
                CircularTransportButton(
                    onClick = { viewModel.toggleShuffle() },
                    icon = Icons.Rounded.Shuffle,
                    contentDescription = if (shuffleMode != ShuffleMode.OFF) "Shuffle on" else "Shuffle off",
                    tint = if (shuffleMode != ShuffleMode.OFF) AccentViolet
                    else RikoTextPrimary.copy(alpha = 0.85f),
                    isActive = shuffleMode != ShuffleMode.OFF
                )
                CircularTransportButton(
                    onClick = { viewModel.skipToPrevious() },
                    icon = Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous",
                    tint = if (canPrev) RikoTextPrimary else RikoTextPrimary.copy(alpha = 0.3f),
                    enabled = canPrev,
                    size = 52.dp,
                    iconSize = 28.dp
                )
                SheetPlayButton(
                    isPlaying = isPlaying,
                    colorState = viewModel.colorState,
                    waveState = viewModel.waveState,
                    onClick = { viewModel.togglePlayPause() }
                )
                CircularTransportButton(
                    onClick = { viewModel.skipToNext() },
                    icon = Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    tint = if (canNext) RikoTextPrimary else RikoTextPrimary.copy(alpha = 0.3f),
                    enabled = canNext,
                    size = 52.dp,
                    iconSize = 28.dp
                )
                // G-5: the repeat control the state matrix was missing —
                // cycles OFF → ALL → ONE, live against playerManager.
                CircularTransportButton(
                    onClick = { viewModel.cycleRepeatMode() },
                    icon = if (repeatMode == RepeatMode.REPEAT_ONE) {
                        Icons.Rounded.RepeatOne
                    } else {
                        Icons.Rounded.Repeat
                    },
                    contentDescription = when (repeatMode) {
                        RepeatMode.OFF -> "Repeat off"
                        RepeatMode.REPEAT_ALL -> "Repeat all"
                        RepeatMode.REPEAT_ONE -> "Repeat one"
                    },
                    tint = if (repeatMode != RepeatMode.OFF) AccentViolet
                    else RikoTextPrimary.copy(alpha = 0.85f),
                    isActive = repeatMode != RepeatMode.OFF
                )
                CircularTransportButton(
                    onClick = onOpenQueue,
                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = "Queue",
                    tint = RikoTextPrimary.copy(alpha = 0.85f),
                    isActive = false
                )
            }

            Spacer(modifier = Modifier.height(RikoSpacing.XL))
            Spacer(modifier = Modifier.height(16.dp))

            // ── 7. Bottom action row: Queue | Lyrics | EQ ─────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .sheetSection(progress, 0.45f, 0.75f, reduceMotion),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NpActionTab(
                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                    label = "Queue",
                    onClick = onOpenQueue,
                    modifier = Modifier.weight(1f)
                )
                NpActionTab(
                    icon = Icons.Rounded.MusicNote,
                    label = "Lyrics",
                    onClick = onOpenLyrics,
                    modifier = Modifier.weight(1f),
                    isHighlighted = true
                )
                NpActionTab(
                    icon = Icons.Rounded.GraphicEq,
                    label = "EQ",
                    onClick = onOpenEqualizer,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── 8. Up Next card ───────────────────────────────────────────
            UpNextSection(
                next = nextSong,
                trackCount = queue.size,
                onOpenQueue = onOpenQueue,
                modifier = Modifier
                    .fillMaxWidth()
                    .sheetSection(progress, 0.50f, 0.80f, reduceMotion)
            )

            Spacer(modifier = Modifier.height(RikoSpacing.XS))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ARTWORK PAGE (M3 window + Phase 3) — one pager page: art + title +
// artist, live during the swipe. The current track keeps the full
// HeroAlbum treatment (glow/mat/glass + crossfade ritual); adjacent tracks
// render plain artwork so idle pages cost almost nothing and never collect
// energy ticks.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ArtworkPage(
    pageSong: Song,
    isCurrent: Boolean,
    pageOffset: Float,
    reduceMotion: Boolean,
    progress: () -> Float,
    colorState: StateFlow<ColorState>,
    waveState: StateFlow<WaveState>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // P0 FIX (album-art mix-up): resolve embedded art per-song. The old
    // `content://…/albumart/$albumId` URI collides across YouTube downloads
    // that share a MediaStore albumId — Now Playing must never use it directly.
    val artworkData = remember(pageSong.id, pageSong.uri, pageSong.albumId) {
        resolveArtworkData(context, pageSong.id, pageSong.albumId, pageSong.uri)
    }
    val albumLine = when {
        pageSong.album.isBlank() -> null
        pageSong.album == pageSong.artist -> null
        pageSong.year > 0 -> "${pageSong.album} (${pageSong.year})"
        else -> pageSong.album
    }
    // Neighbor effect (plan Phase 3): scale 1 → 0.92, alpha 1 → 0.6
    // by |pageOffset|. Skipped under reduce-motion.
    val absOff = kotlin.math.abs((if (reduceMotion) 0f else pageOffset).coerceIn(-1f, 1f))
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                val nScale = 1f - 0.08f * absOff
                scaleX = nScale
                scaleY = nScale
                alpha = 1f - 0.4f * absOff
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (isCurrent) {
                // Builder has no stable equals (RC-10) — key remember on song.id
                // (NOT albumId: shared albumIds must still rebuild per track).
                // HeroAlbum takes the Builder itself (it builds internally).
                // Plan 1.2: size-aware cache key so hero (512) never shares a
                // memory entry with row thumbs of the same album.
                val request = remember(pageSong.id, artworkData) {
                    artworkData?.let {
                        ImageRequest.Builder(context)
                            .data(it)
                            .size(ARTWORK_PX_HERO)
                            .memoryCacheKey(albumArtCacheKey(pageSong.id, pageSong.albumId, ARTWORK_PX_HERO))
                            .diskCacheKey(albumArtCacheKey(pageSong.id, pageSong.albumId, ARTWORK_PX_HERO))
                    }
                }
                HeroAlbum(
                    colorState = colorState,
                    waveState = waveState,
                    albumArtModel = request,
                    contentDescription = pageSong.title,
                    modifier = Modifier.fillMaxWidth(0.66f)
                )
            } else {
                RikoArtwork(
                    model = rememberAlbumArtworkModel(
                        pageSong.albumId,
                        ARTWORK_PX_SHELF,
                        songId = pageSong.id,
                        songUri = pageSong.uri
                    ),
                    contentDescription = pageSong.title,
                    modifier = Modifier
                        .fillMaxWidth(0.66f)
                        .aspectRatio(1f)
                        .clip(RikoShape.CardLarge)
                        .background(Color.White.copy(alpha = 0.08f)),
                    fallback = {}
                )
            }
        }

        Spacer(modifier = Modifier.height(RikoSpacing.XL))

        // Per-page title/artist — live during the swipe (plan Phase 3).
        // Plan Phase 2: full title/artist fade in over p 0.55 -> 1.0 with a
        // 12dp rise (handled in sheetSection).
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .sheetSection(progress, 0.55f, 1.0f, reduceMotion),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = pageSong.title,
                style = RikoThemeV2.Type.npTitle,
                color = RikoTextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(
                        iterations = if (reduceMotion) 0 else Int.MAX_VALUE,
                        initialDelayMillis = 3_000
                    )
            )
            Spacer(modifier = Modifier.height(RikoSpacing.XS))
            Text(
                text = pageSong.artist,
                style = RikoThemeV2.Type.npArtist,
                color = RikoTextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            if (albumLine != null) {
                Spacer(modifier = Modifier.height(RikoSpacing.SM))
                Text(
                    text = albumLine,
                    style = RikoTypography.Caption,
                    color = RikoTextTertiary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TOP GLASS CAPSULE — back · NOW PLAYING · star (fav micro-anim) · overflow
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NpActionTab(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false
) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RikoShape.Card)
            .background(
                if (isHighlighted) AccentViolet.copy(alpha = 0.15f)
                else Color.White.copy(alpha = 0.06f)
            )
            .border(
                width = 1.dp,
                color = if (isHighlighted) AccentViolet.copy(alpha = 0.3f)
                else Color.White.copy(alpha = 0.08f),
                shape = RikoShape.Card
            )
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isHighlighted) AccentViolet
            else RikoTextPrimary.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = RikoTypography.Label,
            color = if (isHighlighted) AccentViolet
            else RikoTextPrimary.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun CircularTransportButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isActive: Boolean = false,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            // N2: subtle glow ring so secondary transport matches the play
            // button's premium tier (was a flat 6% white circle).
            .shadow(
                elevation = if (isActive) 12.dp else 6.dp,
                shape = CircleShape,
                ambientColor = if (isActive) AccentViolet.copy(alpha = 0.55f)
                else Color.White.copy(alpha = 0.18f),
                spotColor = if (isActive) AccentViolet.copy(alpha = 0.55f)
                else Color.White.copy(alpha = 0.18f)
            )
            .background(
                if (isActive) AccentViolet.copy(alpha = 0.15f)
                else Color.White.copy(alpha = 0.06f)
            )
            .border(
                width = 1.dp,
                color = if (isActive) AccentViolet.copy(alpha = 0.4f)
                else Color.White.copy(alpha = 0.12f),
                shape = CircleShape
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun NpTopCapsule(
    isFavorite: Boolean,
    onCollapse: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenVisualizer: () -> Unit,
    onOpenDna: () -> Unit,
    onOpenInfo: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenAddToPlaylist: () -> Unit,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }

    // Star pop — fires only on favorite TOGGLES, not on first composition.
    // Skipped entirely under reduce-motion (static icon swap instead).
    val starScale = remember { Animatable(1f) }
    var starInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(isFavorite) {
        if (!starInitialized) {
            starInitialized = true
        } else if (!reduceMotion) {
            starScale.snapTo(1.35f)
            starScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RikoShape.Card)
            .background(Color(0xB3140A24))
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onCollapse, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Collapse player",
                tint = RikoTextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "NOW PLAYING",
                style = RikoTypography.Overline,
                color = RikoTextPrimary,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "RIKO MUSIC",
                style = RikoTypography.Caption,
                color = RikoTextSecondary,
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center
            )
        }
        IconButton(onClick = onOpenVisualizer, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Rounded.GraphicEq,
                contentDescription = "Open visualizer",
                tint = RikoTextPrimary.copy(alpha = 0.75f),
                modifier = Modifier.size(22.dp)
            )
        }
        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (isFavorite) AccentViolet else RikoTextPrimary.copy(alpha = 0.75f),
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = starScale.value
                        scaleY = starScale.value
                    }
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "More options",
                    tint = RikoTextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Equalizer") },
                    leadingIcon = { Icon(Icons.Rounded.Tune, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onOpenEqualizer()
                    }
                )
                // D-15: the missing 6th entry — routes to the existing lyrics
                // sheet (was unreachable: no menu path at all).
                DropdownMenuItem(
                    text = { Text("Lyrics") },
                    leadingIcon = { Icon(Icons.Rounded.MusicNote, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onOpenLyrics()
                    }
                )
                DropdownMenuItem(text = { Text("Music DNA") }, onClick = {
                    menuOpen = false
                    onOpenDna()
                })
                DropdownMenuItem(
                    text = { Text("Visualizer") },
                    leadingIcon = { Icon(Icons.Rounded.GraphicEq, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onOpenVisualizer()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Song info") },
                    leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onOpenInfo()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Sleep timer") },
                    leadingIcon = { Icon(Icons.Rounded.Timer, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onOpenSleepTimer()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Add to playlist") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onOpenAddToPlaylist()
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PLAY BUTTON WRAPPER — isolates colorState/waveState collection so energy
// ticks (30Hz) recompose this leaf only, never the whole sheet column.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SheetPlayButton(
    isPlaying: Boolean,
    colorState: StateFlow<ColorState>,
    waveState: StateFlow<WaveState>,
    onClick: () -> Unit
) {
    val colors by colorState.collectAsStateWithLifecycle()
    val wave by waveState.collectAsStateWithLifecycle()
    PlayPauseHeroButton(
        isPlaying = isPlaying,
        energy = colors.energyIntensity,
        hasBeat = wave.hasBeat,
        onClick = onClick
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// PROGRESS BAR — time-driven liquid flame, hollow playhead, tap/drag seek.
// Collects the 4Hz position ticker itself so only this leaf recomposes.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NpProgressBar(
    trackId: Long,
    samplePosition: () -> Long,
    sampleAmplitude: () -> Float,
    positionFlow: StateFlow<Long>,
    durationFlow: StateFlow<Long>,
    colorState: StateFlow<ColorState>,
    isPlaying: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    waveformData: com.rikodev.music.android.audiofx.WaveformData? = null,
    isTimelineVisible: () -> Boolean = { true }
) {
    val positionMs by positionFlow.collectAsStateWithLifecycle()
    val durationMs by durationFlow.collectAsStateWithLifecycle()
    val currentOnSeek by rememberUpdatedState(onSeek)
    val currentSample by rememberUpdatedState(samplePosition)
    val colors = colorState.collectAsStateWithLifecycle()
    // Only artwork swatch changes invalidate this projection, not 30Hz energy.
    val tones by remember {
        derivedStateOf {
            if (colors.value.isMonochrome) Color.LightGray to Color.Gray
            else colors.value.primary to colors.value.secondary
        }
    }
    var scrubbing by remember(trackId) { mutableStateOf(false) }
    var scrubProgress by remember(trackId) { mutableStateOf(0f) }
    var pending by remember(trackId) { mutableStateOf<Float?>(null) }
    val canSeek = durationMs > 0L
    val playbackProgress = if (canSeek) (positionMs.toDouble() / durationMs).toFloat().coerceIn(0f, 1f) else 0f
    fun actualProgress(): Float = if (canSeek) (currentSample().toDouble() / durationMs).toFloat().coerceIn(0f, 1f) else 0f
    val shown = if (scrubbing) scrubProgress else pending ?: playbackProgress

    // Hold the requested UI position until the canonical player reports it.
    // New drags replace this job; rejected seeks reconcile after a bounded wait.
    LaunchedEffect(pending, trackId, durationMs) {
        val target = pending ?: return@LaunchedEffect
        var start = 0L
        while (true) {
            val done = withFrameNanos { now ->
                if (start == 0L) start = now
                abs(actualProgress() - target) * durationMs <= 150f || now - start >= 1_000_000_000L
            }
            if (done) { pending = null; break }
        }
    }
    LaunchedEffect(canSeek) {
        if (!canSeek) { scrubbing = false; pending = null }
    }
    Column(modifier = modifier) {
        WaveformTimeline(
            progress = shown,
            sampleProgress = { if (scrubbing) scrubProgress else pending ?: actualProgress() },
            sampleAmplitude = sampleAmplitude,
            trackId = trackId,
            enabled = canSeek,
            isPlaying = isPlaying,
            artworkColor = tones.first,
            artworkSecondary = tones.second,
            waveformData = waveformData,
            onSeekStart = { pending = null; scrubProgress = actualProgress(); scrubbing = true },
            onSeekChange = { fraction -> scrubProgress = fraction },
            onSeekEnd = { fraction ->
                if (canSeek) {
                    // Exactly one Media3 seek per completed gesture/accessibility action.
                    scrubProgress = fraction
                    pending = fraction
                    scrubbing = false
                    currentOnSeek(fraction)
                }
            },
            onSeekCancel = { scrubbing = false; pending = null },
            modifier = Modifier.fillMaxWidth(),
            height = 56.dp,
            isVisible = isTimelineVisible
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = formatMs(if (scrubbing || pending != null) (shown * durationMs).toLong() else positionMs),
                style = RikoTypography.TimeTabular, color = RikoTextSecondary)
            Text(text = if (canSeek) formatMs(durationMs) else "LIVE",
                style = RikoTypography.TimeTabular, color = RikoTextSecondary)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UP NEXT CARD — next-track preview + queue affordance.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun UpNextSection(
    next: Song?,
    trackCount: Int,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Up Next",
            style = RikoTypography.Label,
            color = RikoTextTertiary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(RikoCornerRadius.MD))
                .background(RikoThemeV2.Colors.surface.copy(alpha = 0.85f))
                .clickable(onClick = onOpenQueue)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (next != null) {
                RikoArtwork(
                    model = rememberAlbumArtworkModel(next.albumId, ARTWORK_PX_ROW, songId = next.id, songUri = next.uri),
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(RikoCornerRadius.XS))
                        .background(Color.White.copy(alpha = 0.08f)),
                    fallback = {}
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = next.title,
                        style = RikoTypography.Body,
                        color = RikoTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = next.artist,
                        style = RikoTypography.Caption,
                        color = RikoTextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Queue",
                    style = RikoTypography.Body,
                    color = RikoTextPrimary
                )
                Text(
                    text = "($trackCount tracks)",
                    style = RikoTypography.Caption,
                    color = RikoTextTertiary
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = RikoTextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MORPH SECTION MODIFIER — staggered fade + rise off ONE sheet progress value.
// MUST only be read inside the graphicsLayer lambda (draw phase).
// ─────────────────────────────────────────────────────────────────────────────

private fun Modifier.sheetSection(
    progress: () -> Float,
    start: Float,
    end: Float,
    reduceMotion: Boolean = false
): Modifier = graphicsLayer {
    val s = PlayerSheetReducer.stagger(progress(), start, end)
    alpha = s
    translationY = if (reduceMotion) 0f else (1f - s) * 24.dp.toPx()
}

/** mm:ss from milliseconds. */
private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

/**
 * Deterministic artwork fallback (plan Phase 4): when a track has no
 * artwork/palette, its blend color derives from a title/artist hash —
 * stable per song, never gray.
 */
private fun fallbackColorFor(song: Song): Color {
    val hue = (kotlin.math.abs(song.title.hashCode() * 31 + song.artist.hashCode()) % 360).toFloat()
    return Color.hsv(hue, 0.55f, 0.45f)
}
