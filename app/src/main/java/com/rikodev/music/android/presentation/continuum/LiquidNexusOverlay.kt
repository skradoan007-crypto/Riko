package com.rikodev.music.android.presentation.continuum

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.CloseFullscreen
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rikodev.music.android.core.designsystem.riko.components.ARTWORK_PX_HERO
import com.rikodev.music.android.core.designsystem.riko.components.RikoArtwork
import com.rikodev.music.android.core.designsystem.riko.components.RikoHapticEvent
import com.rikodev.music.android.core.designsystem.riko.components.rememberAlbumArtworkModel
import com.rikodev.music.android.core.designsystem.riko.components.rememberRikoHaptic
import com.rikodev.music.android.core.designsystem.riko.theme.AccentMagenta
import com.rikodev.music.android.core.designsystem.riko.theme.AccentViolet
import com.rikodev.music.android.core.designsystem.riko.theme.RikoBlue
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextSecondary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTypography
import com.rikodev.music.android.domain.model.RepeatMode
import com.rikodev.music.android.domain.model.ShuffleMode
import com.rikodev.music.android.domain.model.Song
import com.rikodev.music.android.presentation.nowplaying.PlayerViewModel
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// RIKO LIQUID NEXUS — LIVING MUSIC DOCK & MORPHING PLAYER CHAMBER
// (P3 Compact Living Dock · P4 Right-to-Center Morph · P5 Controls & Timeline ·
//  P6 Scrubbing Floating Time Bubble · P8 MDNA Reactivity & Full Player Handoff)
//
// Exact expanded layout hierarchy:
//   [ Left: Name + Artist ]  [ Center: Album Art + Play/Pause ]  [ Right: Controls & Modes ]
//   [ Lower Integrated Portion: Liquid Timeline with Scrubbing Bubble ]
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LiquidNexusOverlay(
    nexusState: LiquidNexusState,
    activeSection: ContinuumSection,
    renderProfile: EffectiveRenderProfile,
    playerViewModel: PlayerViewModel,
    onEvent: (LiquidNexusEvent) -> Unit,
    onOpenQueue: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenSongInfo: () -> Unit,
    onOpenAddToPlaylist: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onOpenPlaylistsSection: () -> Unit,
    onOpenFullPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSong by playerViewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    val playbackPosMs by playerViewModel.playbackPosition.collectAsStateWithLifecycle()
    val durationMs by playerViewModel.duration.collectAsStateWithLifecycle()
    val shuffleMode by playerViewModel.shuffleMode.collectAsStateWithLifecycle()
    val repeatMode by playerViewModel.repeatMode.collectAsStateWithLifecycle()
    val isFavorite by playerViewModel.isCurrentSongFavorite.collectAsStateWithLifecycle()
    val audioState by playerViewModel.audioState.collectAsStateWithLifecycle()
    val dynamicColors by playerViewModel.dynamicColors.collectAsStateWithLifecycle()

    val song = currentSong ?: return
    if (nexusState.mode == LiquidNexusMode.FullPlayer) return

    val haptic = rememberRikoHaptic()
    val density = LocalDensity.current

    // Primary & secondary accents (artwork-aware on Ultra/Balanced, static on Lite)
    val primaryAccent: Color = if (renderProfile.useDynamicArtworkColors && dynamicColors.vibrant != 0) {
        Color(dynamicColors.vibrant)
    } else AccentViolet
    val secondaryAccent: Color = if (renderProfile.useDynamicArtworkColors && dynamicColors.lightVibrant != 0) {
        Color(dynamicColors.lightVibrant)
    } else AccentMagenta

    // ── Coordinated expansion Animatable (0f = Compact right dock, 1f = Expanded centered chamber) ──
    val targetExpand = when (nexusState.mode) {
        LiquidNexusMode.Compact,
        LiquidNexusMode.Collapsing -> 0f
        LiquidNexusMode.Expanding,
        LiquidNexusMode.Expanded,
        LiquidNexusMode.Scrubbing,
        LiquidNexusMode.ModeSelection,
        LiquidNexusMode.FullPlayer -> 1f
    }
    val expandAnim = remember { Animatable(targetExpand) }

    LaunchedEffect(targetExpand, renderProfile.reduceMotion) {
        if (renderProfile.reduceMotion) {
            expandAnim.snapTo(targetExpand)
        } else {
            expandAnim.animateTo(
                targetValue = targetExpand,
                animationSpec = spring(
                    dampingRatio = 0.76f,
                    stiffness = 260f
                )
            )
        }
        val settledMode = if (targetExpand >= 0.5f) LiquidNexusMode.Expanded else LiquidNexusMode.Compact
        onEvent(LiquidNexusEvent.AnimationSettled(settledMode))
    }

    // ── MDNA / Audio-reactive contour motion loop (lifecycle-safe via LaunchedEffect) ──
    var phaseRad by remember { mutableFloatStateOf(0f) }
    var motionFrame by remember { mutableStateOf(DockMotionFrame()) }
    LaunchedEffect(isPlaying, renderProfile.reduceMotion, renderProfile.useMdnaReactivity) {
        if (renderProfile.reduceMotion) {
            motionFrame = DockMotionFrame()
            return@LaunchedEffect
        }
        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                val dt = if (lastNanos == 0L) 0.016f else ((frameNanos - lastNanos) / 1e9f).coerceIn(0f, 0.05f)
                lastNanos = frameNanos
                val target = MdnaDockMotionBridge.computeTarget(
                    audioState = audioState,
                    isPlaying = isPlaying,
                    reactivityEnabled = renderProfile.useMdnaReactivity
                )
                motionFrame = MdnaDockMotionBridge.smoothStep(motionFrame, target, dt)
                // Gently calm organic phase while expanding or paused
                val idleFactor = (1f - expandAnim.value * 0.65f).coerceIn(0.25f, 1f)
                phaseRad = (phaseRad + dt * motionFrame.phaseSpeed * idleFactor) % (Math.PI.toFloat() * 200f)
            }
        }
    }

    val agslShader = remember(renderProfile.useAgslShaders) {
        if (renderProfile.useAgslShaders && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LiquidNexusShaders.createChamberShaderOrNull()
        } else null
    }
    val chamberPath = remember { Path() }
    val neckPath = remember { Path() }

    val artworkModel = rememberAlbumArtworkModel(
        albumId = song.albumId,
        px = ARTWORK_PX_HERO,
        songId = song.id,
        songUri = song.uri
    )

    val safeDuration = durationMs.coerceAtLeast(0L)
    val liveFraction = if (safeDuration > 0L) {
        (playbackPosMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val displayFraction = nexusState.scrubFraction ?: liveFraction
    val displayPosMs = if (nexusState.mode == LiquidNexusMode.Scrubbing) {
        nexusState.scrubPreviewMs
    } else {
        playbackPosMs.coerceAtLeast(0L)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(bottom = ContinuumWaveBarHeight - 8.dp)
    ) {
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val p = expandAnim.value.coerceIn(0f, 1f)

        // Geometry targets:
        // Compact right-side living dock: 64dp artwork centered near right edge
        // Expanded liquid chamber: full width minus 16dp horizontal gutters, height 124dp
        val compactSizeDp = 64.dp
        val expandedHeightDp = 124.dp
        val chamberHeightDp = compactSizeDp + (expandedHeightDp - compactSizeDp) * p
        val horizontalMarginPx = with(density) { 12.dp.toPx() }
        val compactRightCenterPx = containerWidthPx - with(density) { 48.dp.toPx() }
        val expandedCenterPx = containerWidthPx * 0.5f

        // Single shared artwork X coordinate: smoothly travels Right -> Center
        val artTravelP = ((p - 0.10f) / 0.52f).coerceIn(0f, 1f)
        val artCenterX = compactRightCenterPx + (expandedCenterPx - compactRightCenterPx) * artTravelP

        // Chamber horizontal bounds stretch organically around the moving artwork
        val compactHalfW = with(density) { 36.dp.toPx() }
        val chamberLeft = (artCenterX - compactHalfW) * (1f - p) + horizontalMarginPx * p
        val chamberRight = (artCenterX + compactHalfW) * (1f - p) + (containerWidthPx - horizontalMarginPx) * p

        // Content reveal alpha (staggered after artwork reaches center)
        val contentReveal = ((p - 0.45f) / 0.45f).coerceIn(0f, 1f)
        val compactBadgeAlpha = (1f - p * 3.2f).coerceIn(0f, 1f)

        // ── Floating Smart Mode Cluster (Shuffle / Repeat / Queue) above right controls ──
        AnimatedVisibility(
            visible = nexusState.mode == LiquidNexusMode.ModeSelection && p > 0.85f,
            enter = fadeIn() + scaleIn(initialScale = 0.88f),
            exit = fadeOut() + scaleOut(targetScale = 0.88f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = expandedHeightDp + 14.dp)
        ) {
            LiquidModeCluster(
                shuffleMode = shuffleMode,
                repeatMode = repeatMode,
                primaryAccent = primaryAccent,
                onToggleShuffle = {
                    haptic.fire(RikoHapticEvent.CONFIRMATION)
                    playerViewModel.toggleShuffle()
                },
                onCycleRepeat = {
                    haptic.fire(RikoHapticEvent.CONFIRMATION)
                    playerViewModel.cycleRepeatMode()
                },
                onOpenQueue = {
                    haptic.fire(RikoHapticEvent.CONFIRMATION)
                    onEvent(LiquidNexusEvent.ModeSelectionDismissed)
                    onOpenQueue()
                }
            )
        }

        // ── Floating Elastic Liquid Time Bubble during Scrubbing (P6) ──
        val bubbleProgress by animateFloatAsState(
            targetValue = if (nexusState.mode == LiquidNexusMode.Scrubbing) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.72f, stiffness = 360f),
            label = "scrubBubbleAnim"
        )
        if (bubbleProgress > 0.02f) {
            val timelineLeftPx = horizontalMarginPx + with(density) { 26.dp.toPx() }
            val timelineRightPx = containerWidthPx - horizontalMarginPx - with(density) { 26.dp.toPx() }
            val timelineSpan = (timelineRightPx - timelineLeftPx).coerceAtLeast(1f)
            val thumbX = timelineLeftPx + timelineSpan * displayFraction
            val bubbleHalfW = with(density) { 60.dp.toPx() }
            val clampedBubbleCenterX = thumbX.coerceIn(
                horizontalMarginPx + bubbleHalfW,
                (containerWidthPx - horizontalMarginPx - bubbleHalfW).coerceAtLeast(horizontalMarginPx + bubbleHalfW)
            )
            val timeLabel = "${formatNexusTime(displayPosMs)} / ${formatNexusTime(safeDuration)}"

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(expandedHeightDp + 68.dp)
                    .graphicsLayer { alpha = bubbleProgress }
            ) {
                // Elastic liquid neck connecting bubble down to timeline thumb
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val bubbleBottomY = with(density) { 48.dp.toPx() }
                    val chamberTopRimY = size.height - with(density) { (expandedHeightDp + 4.dp).toPx() }
                    LiquidDockGeometry.buildScrubNeckPath(
                        outPath = neckPath,
                        anchorX = clampedBubbleCenterX,
                        bubbleBottomY = bubbleBottomY,
                        chamberTopRimY = chamberTopRimY,
                        neckWidthPx = 20.dp.toPx() * bubbleProgress
                    )
                    drawPath(
                        path = neckPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primaryAccent.copy(alpha = 0.85f),
                                secondaryAccent.copy(alpha = 0.58f)
                            ),
                            startY = bubbleBottomY,
                            endY = chamberTopRimY
                        )
                    )
                }

                // Floating time pill clamped within screen bounds
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (clampedBubbleCenterX - bubbleHalfW).roundToInt(),
                                y = with(density) { 10.dp.roundToPx() }
                            )
                        }
                        .width(120.dp)
                        .height(38.dp)
                        .graphicsLayer {
                            scaleX = 0.85f + 0.15f * bubbleProgress
                            scaleY = 0.85f + 0.15f * bubbleProgress
                        }
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xF21B1436),
                                    primaryAccent.copy(alpha = 0.55f),
                                    Color(0xF21B1436)
                                )
                            )
                        )
                        .border(1.2.dp, Color.White.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = timeLabel,
                        style = RikoTypography.Label.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }

        // ── Main Morphing Liquid Dock / Chamber Container ──
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(chamberHeightDp + 16.dp)
                .then(
                    if (nexusState.mode == LiquidNexusMode.Expanded ||
                        nexusState.mode == LiquidNexusMode.ModeSelection
                    ) {
                        Modifier.pointerInput(nexusState.mode) {
                            var cumulativeDragY = 0f
                            detectVerticalDragGestures(
                        onDragStart = { cumulativeDragY = 0f },
                        onVerticalDrag = { _, dragAmount ->
                            cumulativeDragY += dragAmount
                        },
                        onDragEnd = {
                            if (nexusState.mode == LiquidNexusMode.Expanded ||
                                nexusState.mode == LiquidNexusMode.ModeSelection
                            ) {
                                when {
                                    cumulativeDragY < -52f -> {
                                        onEvent(LiquidNexusEvent.FullPlayerRequested)
                                        onOpenFullPlayer()
                                    }
                                    cumulativeDragY > 52f -> {
                                        onEvent(LiquidNexusEvent.CollapseRequested)
                                    }
                                }
                            }
                        }
                    )
                        }
                    } else Modifier
                )
                .drawWithCache {
                    val strokeThin = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
                    val strokeGlow = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)

                    onDrawBehind {
                        val topY = 8.dp.toPx()
                        val bottomY = size.height - 6.dp.toPx()
                        val centerY = (topY + bottomY) * 0.5f

                        // Discontinuity-free 0..1 continuous organic morph (blob -> stretch -> chamber)
                        LiquidDockGeometry.buildContinuousNexusPath(
                            outPath = chamberPath,
                            left = chamberLeft,
                            top = topY,
                            right = chamberRight,
                            bottom = bottomY,
                            expandProgress = p,
                            organicMode = renderProfile.useOrganicBezier,
                            phaseRad = phaseRad,
                            energy = motionFrame.energy,
                            bassPulse = motionFrame.bassPulse,
                            scrubFraction = nexusState.scrubFraction
                        )

                            val shaderBrush = if (
                                agslShader != null &&
                                renderProfile.useAgslShaders &&
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                            ) {
                                LiquidNexusShaders.updateAndBuildBrush(
                                    shader = agslShader,
                                    width = size.width,
                                    height = size.height,
                                    timeSec = phaseRad,
                                    focalFractionX = (artCenterX / size.width.coerceAtLeast(1f)),
                                    energy = motionFrame.energy,
                                    primary = primaryAccent,
                                    secondary = secondaryAccent,
                                    base = Color(0xEB130E24)
                                )
                            } else null

                            if (shaderBrush != null) {
                                drawPath(path = chamberPath, brush = shaderBrush)
                            } else {
                                drawPath(
                                    path = chamberPath,
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xF017112B),
                                            primaryAccent.copy(alpha = 0.34f),
                                            secondaryAccent.copy(alpha = 0.28f),
                                            Color(0xF017112B)
                                        ),
                                        startX = chamberLeft,
                                        endX = chamberRight
                                    )
                                )
                            }

                            if (renderProfile.useGlowAndSpecular) {
                                drawPath(
                                    path = chamberPath,
                                    color = primaryAccent.copy(alpha = 0.30f),
                                    style = strokeGlow
                                )
                            }
                            drawPath(
                                path = chamberPath,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.22f),
                                        primaryAccent.copy(alpha = 0.72f),
                                        Color.White.copy(alpha = 0.78f),
                                        secondaryAccent.copy(alpha = 0.72f),
                                        Color.White.copy(alpha = 0.22f)
                                    ),
                                    startX = chamberLeft,
                                    endX = chamberRight
                                ),
                                style = strokeThin
                            )
                    }
                }
        ) {
            // ── EXPANDED CONTENT LAYER: Left (Name + Artist) | Center Gap | Right (Controls) + Bottom Timeline ──
            if (contentReveal > 0.02f) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .graphicsLayer {
                            alpha = contentReveal
                            translationY = (1f - contentReveal) * 10f
                        },
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Upper row: Left (Name + Artist) | Center (Reserved for Shared Artwork) | Right (Controls)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // LEFT: Song Name + Artist Name (reveals outward from center liquid body)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 6.dp)
                                .graphicsLayer {
                                    translationX = (1f - contentReveal) * 28f
                                }
                                .clickable(role = Role.Button) {
                                    onEvent(LiquidNexusEvent.FullPlayerRequested)
                                    onOpenFullPlayer()
                                },
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = song.title.ifBlank { "Unknown Title" },
                                style = RikoTypography.Body.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                ),
                                color = RikoTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = song.artist.ifBlank { "Unknown Artist" },
                                style = RikoTypography.Caption.copy(fontSize = 12.sp),
                                color = RikoTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // CENTER SLOT: reserves exact space for the single shared Album Art + Play/Pause node
                        Spacer(Modifier.width(68.dp))

                        // RIGHT: Playback Controls + Smart Mode Selector + Contextual Menu (reveals outward from center)
                        Row(
                            modifier = Modifier
                                .weight(1.05f)
                                .padding(start = 2.dp)
                                .graphicsLayer {
                                    translationX = -(1f - contentReveal) * 28f
                                },
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NexusControlIcon(
                                icon = Icons.Rounded.SkipPrevious,
                                contentDesc = "Previous track",
                                onClick = {
                                    haptic.fire(RikoHapticEvent.CONFIRMATION)
                                    playerViewModel.skipToPrevious()
                                }
                            )
                            NexusControlIcon(
                                icon = Icons.Rounded.SkipNext,
                                contentDesc = "Next track",
                                onClick = {
                                    haptic.fire(RikoHapticEvent.CONFIRMATION)
                                    playerViewModel.skipToNext()
                                }
                            )
                            NexusControlIcon(
                                icon = when {
                                    repeatMode == RepeatMode.REPEAT_ONE -> Icons.Rounded.RepeatOne
                                    shuffleMode == ShuffleMode.ON -> Icons.Rounded.Shuffle
                                    else -> Icons.Rounded.Tune
                                },
                                contentDesc = "Playback modes",
                                tint = if (
                                    nexusState.mode == LiquidNexusMode.ModeSelection ||
                                    shuffleMode == ShuffleMode.ON ||
                                    repeatMode != RepeatMode.OFF
                                ) primaryAccent else RikoTextPrimary,
                                onClick = {
                                    haptic.fire(RikoHapticEvent.CONFIRMATION)
                                    onEvent(LiquidNexusEvent.ModeSelectionToggled)
                                }
                            )

                            Box {
                                NexusControlIcon(
                                    icon = Icons.Rounded.MoreVert,
                                    contentDesc = "More playback & section actions",
                                    onClick = { onEvent(LiquidNexusEvent.SecondaryMenuToggled) }
                                )
                                LiquidSecondaryContextMenu(
                                    expanded = nexusState.secondaryMenuOpen,
                                    activeSection = activeSection,
                                    song = song,
                                    isFavorite = isFavorite,
                                    onDismiss = { onEvent(LiquidNexusEvent.SecondaryMenuDismissed) },
                                    onToggleFavorite = { playerViewModel.toggleFavoriteCurrentSong() },
                                    onOpenAddToPlaylist = onOpenAddToPlaylist,
                                    onOpenSongInfo = onOpenSongInfo,
                                    onOpenLyrics = onOpenLyrics,
                                    onOpenQueue = onOpenQueue,
                                    onOpenEqualizer = onOpenEqualizer,
                                    onOpenSearch = onOpenSearch,
                                    onOpenAlbum = onOpenAlbum,
                                    onOpenPlaylistsSection = onOpenPlaylistsSection,
                                    onOpenFullPlayer = {
                                        onEvent(LiquidNexusEvent.FullPlayerRequested)
                                        onOpenFullPlayer()
                                    },
                                    onCollapse = { onEvent(LiquidNexusEvent.CollapseRequested) }
                                )
                            }
                        }
                    }

                    // LOWER INTEGRATED PORTION: Liquid Timeline + Collapse / Full Player affordances
                    LiquidIntegratedTimeline(
                        progress = displayFraction,
                        elapsedMs = displayPosMs,
                        durationMs = safeDuration,
                        isScrubbing = nexusState.mode == LiquidNexusMode.Scrubbing,
                        primaryAccent = primaryAccent,
                        secondaryAccent = secondaryAccent,
                        onScrubStart = { fraction ->
                            haptic.fire(RikoHapticEvent.CONFIRMATION)
                            onEvent(LiquidNexusEvent.ScrubStarted(fraction, safeDuration))
                        },
                        onScrubChange = { fraction ->
                            onEvent(LiquidNexusEvent.ScrubPositionChanged(fraction))
                        },
                        onScrubEnd = {
                            haptic.fire(RikoHapticEvent.CONFIRMATION)
                            onEvent(LiquidNexusEvent.ScrubCommitted)
                        },
                        onScrubCancel = {
                            onEvent(LiquidNexusEvent.ScrubCanceled)
                        },
                        onCollapseClick = { onEvent(LiquidNexusEvent.CollapseRequested) }
                    )
                }
            }

            // ── SINGLE SHARED ALBUM ARTWORK + PLAY/PAUSE BUTTON (Moves Right -> Center) ──
            val artSizeDp = 56.dp + 4.dp * p
            val artSizePx = with(density) { artSizeDp.toPx() }
            val artTopDp = 12.dp + 4.dp * (1f - p)

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (artCenterX - artSizePx * 0.5f).roundToInt(),
                            y = with(density) { artTopDp.roundToPx() }
                        )
                    }
                    .size(artSizeDp)
                    .clip(CircleShape)
                    .border(1.5.dp, Color.White.copy(alpha = 0.55f), CircleShape)
                    .clickable(
                        role = Role.Button,
                        onClick = {
                            haptic.fire(RikoHapticEvent.CONFIRMATION)
                            onEvent(LiquidNexusEvent.PlayPauseRequested)
                        }
                    )
                    .semantics {
                        contentDescription = if (isPlaying) "Pause ${song.title}" else "Play ${song.title}"
                    },
                contentAlignment = Alignment.Center
            ) {
                RikoArtwork(
                    model = artworkModel,
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize()
                )
                // Subtle center scrim so the single Play/Pause glyph stays crisp over any cover art
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.48f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // ── COMPACT EXPAND BUTTON (Beside/corner of right-side floating artwork, 48dp touch target) ──
            if (compactBadgeAlpha > 0.05f) {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (artCenterX + artSizePx * 0.14f).roundToInt(),
                                y = with(density) { 30.dp.roundToPx() }
                            )
                        }
                        .size(48.dp)
                        .graphicsLayer { alpha = compactBadgeAlpha }
                        .clickable(
                            role = Role.Button,
                            onClick = {
                                haptic.fire(RikoHapticEvent.CONFIRMATION)
                                onEvent(LiquidNexusEvent.ExpandRequested)
                            }
                        )
                        .semantics { contentDescription = "Expand Liquid Nexus player" },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(primaryAccent, secondaryAccent)))
                            .border(1.dp, Color.White.copy(alpha = 0.75f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.OpenInFull,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NexusControlIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDesc: String,
    tint: Color = RikoTextPrimary,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(31.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = contentDesc },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun LiquidModeCluster(
    shuffleMode: ShuffleMode,
    repeatMode: RepeatMode,
    primaryAccent: Color,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenQueue: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color(0xEE18122E))
            .border(1.dp, primaryAccent.copy(alpha = 0.55f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ModeChipButton(
            icon = Icons.Rounded.Shuffle,
            label = "Shuffle",
            active = shuffleMode == ShuffleMode.ON,
            accent = primaryAccent,
            onClick = onToggleShuffle
        )
        ModeChipButton(
            icon = if (repeatMode == RepeatMode.REPEAT_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
            label = when (repeatMode) {
                RepeatMode.OFF -> "Repeat Off"
                RepeatMode.REPEAT_ALL -> "Repeat All"
                RepeatMode.REPEAT_ONE -> "Repeat One"
            },
            active = repeatMode != RepeatMode.OFF,
            accent = primaryAccent,
            onClick = onCycleRepeat
        )
        ModeChipButton(
            icon = Icons.AutoMirrored.Rounded.QueueMusic,
            label = "Queue",
            active = false,
            accent = primaryAccent,
            onClick = onOpenQueue
        )
    }
}

@Composable
private fun ModeChipButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (active) accent.copy(alpha = 0.28f) else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) Color.White else RikoTextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun LiquidIntegratedTimeline(
    progress: Float,
    elapsedMs: Long,
    durationMs: Long,
    isScrubbing: Boolean,
    primaryAccent: Color,
    secondaryAccent: Color,
    onScrubStart: (Float) -> Unit,
    onScrubChange: (Float) -> Unit,
    onScrubEnd: () -> Unit,
    onScrubCancel: () -> Unit,
    onCollapseClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatNexusTime(elapsedMs),
            style = RikoTypography.Caption.copy(fontSize = 11.sp),
            color = if (isScrubbing) Color.White else RikoTextSecondary,
            modifier = Modifier.width(38.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(34.dp)
                .pointerInput(durationMs) {
                    detectTapGestures { offset ->
                        val frac = (offset.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                        onScrubStart(frac)
                        onScrubEnd()
                    }
                }
                .pointerInput(durationMs) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            val frac = (offset.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                            onScrubStart(frac)
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val frac = (change.position.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                            onScrubChange(frac)
                        },
                        onDragEnd = { onScrubEnd() },
                        onDragCancel = { onScrubCancel() }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val y = size.height * 0.5f
                val trackH = if (isScrubbing) 5.dp.toPx() else 3.5.dp.toPx()
                val clamped = progress.coerceIn(0f, 1f)
                val fillX = size.width * clamped

                // Inactive track
                drawLine(
                    color = Color.White.copy(alpha = 0.22f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = trackH,
                    cap = StrokeCap.Round
                )
                // Active liquid progress
                drawLine(
                    brush = Brush.horizontalGradient(listOf(primaryAccent, secondaryAccent, Color.White)),
                    start = Offset(0f, y),
                    end = Offset(fillX, y),
                    strokeWidth = trackH,
                    cap = StrokeCap.Round
                )
                // Glowing seek thumb
                val thumbR = if (isScrubbing) 8.dp.toPx() else 5.5.dp.toPx()
                drawCircle(
                    color = primaryAccent.copy(alpha = 0.40f),
                    radius = thumbR * 1.8f,
                    center = Offset(fillX, y)
                )
                drawCircle(
                    color = Color.White,
                    radius = thumbR,
                    center = Offset(fillX, y)
                )
            }
        }

        Text(
            text = formatNexusTime(durationMs),
            style = RikoTypography.Caption.copy(fontSize = 11.sp),
            color = RikoTextSecondary,
            modifier = Modifier.padding(start = 8.dp, end = 4.dp)
        )

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onCollapseClick)
                .semantics { contentDescription = "Collapse Liquid Nexus player" },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.CloseFullscreen,
                contentDescription = null,
                tint = RikoTextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun LiquidSecondaryContextMenu(
    expanded: Boolean,
    activeSection: ContinuumSection,
    song: Song,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenAddToPlaylist: () -> Unit,
    onOpenSongInfo: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onOpenPlaylistsSection: () -> Unit,
    onOpenFullPlayer: () -> Unit,
    onCollapse: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .background(Color(0xF517112B), RoundedCornerShape(16.dp))
    ) {
        DropdownMenuItem(
            text = { Text("Full Now Playing", color = RikoTextPrimary) },
            leadingIcon = { Icon(Icons.Rounded.KeyboardArrowUp, null, tint = AccentViolet) },
            onClick = { onDismiss(); onOpenFullPlayer() }
        )
        DropdownMenuItem(
            text = { Text(if (isFavorite) "Remove Favorite" else "Add to Favorites", color = RikoTextPrimary) },
            leadingIcon = {
                Icon(
                    if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    null,
                    tint = AccentMagenta
                )
            },
            onClick = { onDismiss(); onToggleFavorite() }
        )
        DropdownMenuItem(
            text = { Text("Add to Playlist", color = RikoTextPrimary) },
            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null, tint = RikoTextPrimary) },
            onClick = { onDismiss(); onOpenAddToPlaylist() }
        )

        // Section-aware Contextual Action Pocket (P9)
        ContinuumContextAction.forSection(activeSection).forEach { action ->
            when (action) {
                ContinuumContextAction.QUEUE -> DropdownMenuItem(
                    text = { Text("Queue", color = RikoTextPrimary) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, null, tint = RikoTextSecondary) },
                    onClick = { onDismiss(); onOpenQueue() }
                )
                ContinuumContextAction.LYRICS -> DropdownMenuItem(
                    text = { Text("Lyrics", color = RikoTextPrimary) },
                    leadingIcon = { Icon(Icons.Rounded.Lyrics, null, tint = RikoTextSecondary) },
                    onClick = { onDismiss(); onOpenLyrics() }
                )
                ContinuumContextAction.MDNA -> DropdownMenuItem(
                    text = { Text("MDNA & Equalizer", color = RikoTextPrimary) },
                    leadingIcon = { Icon(Icons.Rounded.GraphicEq, null, tint = RikoTextSecondary) },
                    onClick = { onDismiss(); onOpenEqualizer() }
                )
                ContinuumContextAction.ARTIST -> DropdownMenuItem(
                    text = { Text("Explore Artist", color = RikoTextPrimary) },
                    leadingIcon = { Icon(Icons.Rounded.Info, null, tint = RikoTextSecondary) },
                    onClick = { onDismiss(); onOpenSearch() }
                )
                ContinuumContextAction.ALBUM -> if (song.albumId > 0L) {
                    DropdownMenuItem(
                        text = { Text("Go to Album", color = RikoTextPrimary) },
                        leadingIcon = { Icon(Icons.Rounded.Info, null, tint = RikoTextSecondary) },
                        onClick = { onDismiss(); onOpenAlbum(song.albumId) }
                    )
                }
                ContinuumContextAction.SONG_INFO -> DropdownMenuItem(
                    text = { Text("Song Info", color = RikoTextPrimary) },
                    leadingIcon = { Icon(Icons.Rounded.Info, null, tint = RikoTextSecondary) },
                    onClick = { onDismiss(); onOpenSongInfo() }
                )
                ContinuumContextAction.PLAYLISTS -> DropdownMenuItem(
                    text = { Text("Open Playlists", color = RikoTextPrimary) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, null, tint = RikoTextSecondary) },
                    onClick = { onDismiss(); onOpenPlaylistsSection() }
                )
                else -> Unit
            }
        }

        if (activeSection != ContinuumSection.EXPLORE) {
            DropdownMenuItem(
                text = { Text("Song Info", color = RikoTextPrimary) },
                leadingIcon = { Icon(Icons.Rounded.Info, null, tint = RikoTextSecondary) },
                onClick = { onDismiss(); onOpenSongInfo() }
            )
        }
        DropdownMenuItem(
            text = { Text("Collapse to Living Dock", color = RikoTextSecondary) },
            leadingIcon = { Icon(Icons.Rounded.CloseFullscreen, null, tint = RikoTextSecondary) },
            onClick = { onDismiss(); onCollapse() }
        )
    }
}

fun formatNexusTime(ms: Long): String {
    if (ms <= 0L) return "--:--"
    val totalSec = ms / 1000L
    val minutes = totalSec / 60L
    val seconds = totalSec % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
