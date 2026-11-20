package com.rikodev.music.android.presentation.continuum

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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
import com.rikodev.music.android.presentation.continuum.LiquidGlassStyle.drawLiquidGlassBody
import com.rikodev.music.android.presentation.nowplaying.PlayerViewModel
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// RIKO LIQUID NEXUS — PURE LIQUID GLASS LIVING DOCK & MORPHING CHAMBER (P12)
//
// 1. Compact Living Dock: Album artwork ITSELF is masked/clipped by
//    [MorphingLiquidArtworkShape] (Pentagon <-> 10-Petal Cookie <-> Pill <->
//    12-Petal Cookie <-> 8-Point Starburst) with crisp optical glass rim.
// 2. Tap Expand: Morphing shape smoothly settles into a pure Circle as the
//    SAME artwork glides Right -> Center and Pure Liquid Glass chamber expands
//    horizontally around it.
// 3. Exact Expanded Layout:
//      [ Left: Name + Artist ]  [ Center: Art + Play/Pause ]  [ Right: Controls ]
//      [ Lower Integrated Portion: Bulletproof Scrubbing Liquid Timeline ]
// 4. Scrubbing: Immediate awaitEachGesture capture, smooth background blur
//    (CONTINUUM navbar & Liquid Nexus stay sharp), floating Pure Liquid Glass
//    time bubble (e.g., 2:05 / 3:20) attached via elastic liquid meniscus.
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

    // Sanitized luminous glass accents — never lets brown/muddy art dirty the glass
    val primaryAccent: Color = if (renderProfile.useDynamicArtworkColors) {
        LiquidGlassStyle.sanitizeGlassAccent(dynamicColors.vibrant, AccentViolet)
    } else AccentViolet
    val secondaryAccent: Color = if (renderProfile.useDynamicArtworkColors) {
        LiquidGlassStyle.sanitizeGlassAccent(dynamicColors.lightVibrant, AccentMagenta)
    } else RikoBlue

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

    // ── Continuous Expressive Shape Morphing + MDNA Reactivity Loop ──
    var phaseRad by remember { mutableFloatStateOf(0f) }
    var morphCycle by remember { mutableFloatStateOf(0f) }
    var morphRotation by remember { mutableFloatStateOf(0f) }
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

                val pVal = expandAnim.value
                // Advance expressive artwork shape morph ONLY while compact/idle (stops cleanly when expanded)
                if (pVal < 0.85f) {
                    val playFactor = if (isPlaying) 1f else 0.16f
                    val speedBoost = 1.25f + motionFrame.energy * 0.80f + motionFrame.bassPulse * 0.35f
                    morphCycle = (morphCycle + dt * speedBoost * playFactor) % 500f
                    morphRotation = (morphRotation + dt * 0.72f * playFactor) % (Math.PI.toFloat() * 200f)
                }
                val idleFactor = (1f - pVal * 0.55f).coerceIn(0.30f, 1f)
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
    val artworkRimPath = remember { Path() }
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
            .padding(bottom = ContinuumWaveBarHeight + 6.dp)
    ) {
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val p = expandAnim.value.coerceIn(0f, 1f)

        // Sleek sculpted geometry:
        // Compact: 68dp expressive masked artwork floating right above right side of navbar
        // Expanded: 114dp pure optical liquid glass horizontal chamber
        val compactSizeDp = 76.dp
        val expandedHeightDp = 118.dp
        val chamberHeightDp = compactSizeDp + (expandedHeightDp - compactSizeDp) * p
        val horizontalMarginPx = with(density) { 12.dp.toPx() }
        val compactRightCenterPx = containerWidthPx - with(density) { 50.dp.toPx() }
        val expandedCenterPx = containerWidthPx * 0.5f

        // Single shared artwork X coordinate: smoothly travels Right -> Center
        val artTravelP = ((p - 0.08f) / 0.54f).coerceIn(0f, 1f)
        val artCenterX = compactRightCenterPx + (expandedCenterPx - compactRightCenterPx) * artTravelP

        // Chamber horizontal bounds emerge smoothly out of the artwork
        val compactHalfW = with(density) { 34.dp.toPx() }
        val chamberLeft = (artCenterX - compactHalfW) * (1f - p) + horizontalMarginPx * p
        val chamberRight = (artCenterX + compactHalfW) * (1f - p) + (containerWidthPx - horizontalMarginPx) * p

        val contentReveal = ((p - 0.44f) / 0.44f).coerceIn(0f, 1f)
        val compactBadgeAlpha = (1f - p * 3.4f).coerceIn(0f, 1f)
        // Morph intensity: 1.0 in Compact idle (expressive shapes), 0.0 once Expanded (crisp circle)
        val artworkMorphIntensity = if (renderProfile.reduceMotion) 0f else (1f - p * 2.2f).coerceIn(0f, 1f)

        // ── Floating Pure Liquid Glass Smart Mode Cluster (Shuffle / Repeat / Queue) ──
        AnimatedVisibility(
            visible = nexusState.mode == LiquidNexusMode.ModeSelection && p > 0.85f,
            enter = fadeIn() + scaleIn(initialScale = 0.88f),
            exit = fadeOut() + scaleOut(targetScale = 0.88f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = expandedHeightDp + 18.dp)
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

        // ── Floating Pure Liquid Glass Time Bubble + Elastic Meniscus during Scrubbing ──
        val bubbleProgress by animateFloatAsState(
            targetValue = if (nexusState.mode == LiquidNexusMode.Scrubbing) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.72f, stiffness = 360f),
            label = "scrubBubbleAnim"
        )
        if (bubbleProgress > 0.02f) {
            val timelineLeftPx = horizontalMarginPx + with(density) { 54.dp.toPx() }
            val timelineRightPx = containerWidthPx - horizontalMarginPx - with(density) { 54.dp.toPx() }
            val timelineSpan = (timelineRightPx - timelineLeftPx).coerceAtLeast(1f)
            val thumbX = timelineLeftPx + timelineSpan * displayFraction
            val bubbleHalfW = with(density) { 64.dp.toPx() }
            val clampedBubbleCenterX = thumbX.coerceIn(
                horizontalMarginPx + bubbleHalfW,
                (containerWidthPx - horizontalMarginPx - bubbleHalfW).coerceAtLeast(horizontalMarginPx + bubbleHalfW)
            )
            val timeLabel = "${formatNexusTime(displayPosMs)} / ${formatNexusTime(safeDuration)}"

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(expandedHeightDp + 72.dp)
                    .graphicsLayer { alpha = bubbleProgress }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val bubbleBottomY = with(density) { 46.dp.toPx() }
                    val chamberTopRimY = size.height - with(density) { (expandedHeightDp + 6.dp).toPx() }
                    LiquidDockGeometry.buildScrubNeckPath(
                        outPath = neckPath,
                        anchorX = clampedBubbleCenterX,
                        bubbleBottomY = bubbleBottomY,
                        chamberTopRimY = chamberTopRimY,
                        neckWidthPx = 22.dp.toPx() * bubbleProgress
                    )
                    drawPath(
                        path = neckPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.45f),
                                primaryAccent.copy(alpha = 0.58f),
                                secondaryAccent.copy(alpha = 0.40f)
                            ),
                            startY = bubbleBottomY,
                            endY = chamberTopRimY
                        )
                    )
                }

                // Pure Liquid Glass floating time pill (e.g., 2:05 / 3:20)
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (clampedBubbleCenterX - bubbleHalfW).roundToInt(),
                                y = with(density) { 6.dp.roundToPx() }
                            )
                        }
                        .width(128.dp)
                        .height(40.dp)
                        .graphicsLayer {
                            scaleX = 0.86f + 0.14f * bubbleProgress
                            scaleY = 0.86f + 0.14f * bubbleProgress
                        }
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xD8221F3B),
                                    Color(0xE80E0C1D)
                                )
                            )
                        )
                        .border(
                            width = 1.25.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.82f),
                                    primaryAccent.copy(alpha = 0.50f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = timeLabel,
                        style = RikoTypography.Label.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }

        // ── Main Morphing Pure Liquid Glass Chamber Container ──
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(chamberHeightDp + 16.dp)

                .drawWithCache {
                    onDrawBehind {
                        // Draw expanding Liquid Glass chamber ONLY once morph begins (p > 0.03f);
                        // in pure Compact idle (p <= 0.03f) the masked morphing album art itself floats cleanly!
                        if (p > 0.03f) {
                            val topY = 8.dp.toPx()
                            val bottomY = size.height - 6.dp.toPx()
                            val centerY = (topY + bottomY) * 0.5f

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
                                    base = Color(0xAA0C0A1A)
                                )
                            } else null

                            drawLiquidGlassBody(
                                path = chamberPath,
                                focalX = artCenterX,
                                focalY = centerY,
                                primaryAccent = primaryAccent,
                                secondaryAccent = secondaryAccent,
                                energy = motionFrame.energy,
                                renderProfile = renderProfile,
                                shaderBrush = shaderBrush
                            )
                        }
                    }
                }
        ) {
            // ── EXPANDED CONTENT: Left (Name + Artist) | Center Artwork Gap | Right (Controls) + Integrated Timeline ──
            if (contentReveal > 0.02f) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 20.dp, end = 18.dp, top = 14.dp, bottom = 8.dp)
                        .graphicsLayer {
                            alpha = contentReveal
                        },
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .pointerInput(Unit) {
                                var cumulativeDragY = 0f
                                detectVerticalDragGestures(
                                    onDragStart = { cumulativeDragY = 0f },
                                    onVerticalDrag = { _, dragAmount -> cumulativeDragY += dragAmount },
                                    onDragEnd = {
                                        when {
                                            cumulativeDragY < -48f -> {
                                                onEvent(LiquidNexusEvent.FullPlayerRequested)
                                                onOpenFullPlayer()
                                            }
                                            cumulativeDragY > 48f -> {
                                                onEvent(LiquidNexusEvent.CollapseRequested)
                                            }
                                        }
                                    }
                                )
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // LEFT: Song Name + Artist Name emerging from center liquid body
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
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                            )
                            Spacer(Modifier.height(1.dp))
                            Text(
                                text = song.artist.ifBlank { "Unknown Artist" },
                                style = RikoTypography.Caption.copy(fontSize = 12.sp),
                                color = Color.White.copy(alpha = 0.72f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // CENTER SLOT: exact reserve for the single shared Album Art + Play/Pause
                        Spacer(Modifier.width(68.dp))

                        // RIGHT: Playback Controls + Smart Mode Selector + Contextual Menu
                        Row(
                            modifier = Modifier
                                .weight(1.06f)
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
                                ) primaryAccent else Color.White,
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

                    // LOWER INTEGRATED PORTION: Bulletproof Liquid Timeline + Collapse affordance
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

            // ── SINGLE SHARED ALBUM ARTWORK — DIRECTLY MASKED BY EXPRESSIVE MORPHING SHAPE ──
            val artSizeDp = 74.dp - 14.dp * p
            val artSizePx = with(density) { artSizeDp.toPx() }
            val artTopDp = 8.dp + 8.dp * p

            val liveMorphShape = MorphingLiquidArtworkShape(
                morphCycle = morphCycle,
                morphIntensity = artworkMorphIntensity,
                rotationRad = morphRotation,
                energy = motionFrame.energy
            )

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (artCenterX - artSizePx * 0.5f).roundToInt(),
                            y = with(density) { artTopDp.roundToPx() }
                        )
                    }
                    .size(artSizeDp)
                    .drawWithContent {
                        drawContent()
                        // Crisp optical glass rim tracing the EXACT morphing artwork contour
                        MorphingLiquidArtworkShape.buildMorphingArtworkPath(
                            outPath = artworkRimPath,
                            width = size.width,
                            height = size.height,
                            morphCycle = morphCycle,
                            morphIntensity = artworkMorphIntensity,
                            rotationRad = morphRotation,
                            energy = motionFrame.energy
                        )
                        if (renderProfile.useGlowAndSpecular) {
                            drawPath(
                                path = artworkRimPath,
                                color = primaryAccent.copy(alpha = 0.34f),
                                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                        drawPath(
                            path = artworkRimPath,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.85f),
                                    primaryAccent.copy(alpha = 0.55f),
                                    Color.White.copy(alpha = 0.35f)
                                )
                            ),
                            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                    .graphicsLayer {
                        shape = liveMorphShape
                        clip = true
                    }
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
                // Pure frosted glass Play/Pause button centered inside the artwork
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0x660B0918))
                        .border(1.dp, Color.White.copy(alpha = 0.55f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // ── COMPACT LIQUID GLASS EXPAND BUTTON (Beside right-side morphing artwork) ──
            if (compactBadgeAlpha > 0.05f) {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (artCenterX + artSizePx * 0.14f).roundToInt(),
                                y = with(density) { 32.dp.roundToPx() }
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
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xCC1E1A38),
                                        primaryAccent.copy(alpha = 0.85f)
                                    )
                                )
                            )
                            .border(1.2.dp, Color.White.copy(alpha = 0.85f), CircleShape),
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
    tint: Color = Color.White,
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
            modifier = Modifier.size(19.dp)
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
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xD8201D38), Color(0xE80D0B1C))
                )
            )
            .border(
                1.2.dp,
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.72f), primaryAccent.copy(alpha = 0.45f))),
                CircleShape
            )
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
            .background(if (active) accent.copy(alpha = 0.34f) else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) Color.White else Color.White.copy(alpha = 0.75f),
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Bulletproof unified awaitEachGesture timeline: immediately enters Scrubbing
 * on touch/drag (zero gesture conflict with parent), updates preview live,
 * and commits accurate seek ONLY on release.
 */
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
    val currentOnStart by rememberUpdatedState(onScrubStart)
    val currentOnChange by rememberUpdatedState(onScrubChange)
    val currentOnEnd by rememberUpdatedState(onScrubEnd)
    val currentOnCancel by rememberUpdatedState(onScrubCancel)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatNexusTime(elapsedMs),
            style = RikoTypography.Caption.copy(fontSize = 11.sp),
            color = if (isScrubbing) Color.White else Color.White.copy(alpha = 0.75f),
            modifier = Modifier.width(36.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(38.dp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        val widthF = size.width.toFloat().coerceAtLeast(1f)
                        currentOnStart((down.position.x / widthF).coerceIn(0f, 1f))

                        var pointerId = down.id
                        var completedNormally = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId }
                                ?: event.changes.firstOrNull()
                                ?: break
                            pointerId = change.id
                            if (!change.pressed) {
                                change.consume()
                                completedNormally = true
                                break
                            }
                            change.consume()
                            val frac = (change.position.x / widthF).coerceIn(0f, 1f)
                            currentOnChange(frac)
                        }
                        if (completedNormally) currentOnEnd() else currentOnCancel()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val y = size.height * 0.5f
                val trackH = if (isScrubbing) 5.dp.toPx() else 3.2.dp.toPx()
                val clamped = progress.coerceIn(0f, 1f)
                val fillX = size.width * clamped

                drawLine(
                    color = Color.White.copy(alpha = 0.24f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = trackH,
                    cap = StrokeCap.Round
                )
                drawLine(
                    brush = Brush.horizontalGradient(listOf(primaryAccent, secondaryAccent, Color.White)),
                    start = Offset(0f, y),
                    end = Offset(fillX, y),
                    strokeWidth = trackH,
                    cap = StrokeCap.Round
                )
                val thumbR = if (isScrubbing) 8.dp.toPx() else 5.5.dp.toPx()
                drawCircle(
                    color = primaryAccent.copy(alpha = 0.45f),
                    radius = thumbR * 1.9f,
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
            color = Color.White.copy(alpha = 0.72f),
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
                tint = Color.White.copy(alpha = 0.75f),
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
            .background(Color(0xF5141226), RoundedCornerShape(16.dp))
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
