package com.rikodev.music.android.presentation.miniplayer

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.rikodev.music.android.core.designsystem.riko.components.RikoArtwork
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMotion
import com.rikodev.music.android.core.designsystem.glass.colorGlow
import com.rikodev.music.android.core.designsystem.theme.GlassSpecular
import com.rikodev.music.android.core.designsystem.theme.TextSecondary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import kotlin.math.sin
import androidx.compose.animation.animateColorAsState
import androidx.compose.material.icons.filled.Pause
import androidx.compose.animation.core.Animatable

// ═══════════════════════════════════════════════════════════════════════════════════════
// RIKO MUSIC — MINI PLAYER 2.0: ALBUM ART
//
// Build-3, Phase B, File 4.
// Most important file in Mini Player.
//
// Visual anchor. Song identity. NOT a thumbnail.
//
// Personality:
//   Never rotates. Never shakes.
//   Floats (gentle bob). Breathes (subtle scale pulse).
//   Tilts (device accelerometer — Phase D/future).
//   Reflects (glass overlay). Returns (always back to rest).
//
// Reads: state.currentSong, state.albumArtUri, state.color, state.wave.hasBeat,
//        state.interaction, state.layout (from MiniPlayerState)
// Never reads: PlayerState, ColorState (raw), WaveState (raw), AudioState
//
// Pixel Owner: Album Art pixels, Album Reflection pixels (Rule #24)
// Layer Independence: Gradient placeholder on missing art (Rule #25)
//
// MPRS Reference: Chapter A.6 (Reflection), B.3 (Album)
// Tokens Reference: MiniPlayerTokens (Section 2, 9, 11)
// Motion Reference: MiniPlayerMotion (albumFloatSpec, beatPulseSpec)
// ═══════════════════════════════════════════════════════════════════════════════════════

/**
 * Album art component for the Mini Experience Surface.
 *
 * This is the visual anchor — the first thing users identify with.
 * Renders album art with living effects: float, beat pulse, reflection,
 * press feedback, and morph size transitions.
 *
 * Rule #17 — Single Snapshot: Receives [MiniPlayerState], reads only album-related fields.
 * Rule #18 — Morph Source Opacity: Uses layout.albumArtSize, layout.reflectionAlpha.
 *   Never computes morph values from morphProgress.
 * Rule #19 — No Hardcoded Duration: All specs from [MiniPlayerMotion].
 * Rule #20 — Renderer Never Interprets: Applies pre-computed layout.* values.
 * Rule #24 — Pixel Owner: Album art pixels + reflection pixels. No other component.
 * Rule #25 — Layer Independence: Gradient placeholder if no art. Still premium.
 *
 * @param state The single source of truth for all rendering.
 * @param modifier External modifier for positioning.
 */
@Composable
fun MiniPlayerAlbum(
    state: MiniPlayerState,
    modifier: Modifier = Modifier
) {
    // ═════════════════════════════════════════════════════════════════════
    // READ STATE — Rule #17: Only album-related fields
    // ═══════════════════════════════════════════════════════════════════
    val albumArtUri = state.albumArtUri
    val hasAlbumArt = albumArtUri != null
    val layout = state.layout
    val color = state.color
    val isBeatReacting = state.interaction.isBeatReacting
    val pressScale = state.interaction.pressScale
    val reflectionAlpha = layout.reflectionAlpha

    // ═════════════════════════════════════════════════════════════════════
    // ANIMATED SIZE — Smooth morph transitions
    // ═════════════════════════════════════════════════════════════════════
    val animatedSize by animateFloatAsState(
        targetValue = with(LocalDensity.current) {
            layout.albumArtSize.dp.value
        },
        animationSpec = MiniPlayerMotion.morphSettleSpec,
        label = "mp_album_size"
    )

    val albumSize = with(LocalDensity.current) { animatedSize.dp }

    // ═════════════════════════════════════════════════════════════════════
    // FLOAT ANIMATION — Gentle vertical bob when playing
    // ═══════════════════════════════════════════════════════════════════
    // Rule #19: Animation spec from MiniPlayerMotion.
    // Playing: 4000ms cycle, 3dp amplitude.
    // Paused/idle: 8000ms cycle, 1dp amplitude.
    val floatTransition = rememberInfiniteTransition(label = "mp_album_float")
    val floatOffset by floatTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (state.isPlaying)
            MiniPlayerTokens.AlbumFloatAmplitude.value
        else
            MiniPlayerTokens.AlbumIdleFloatAmplitude.value,
        animationSpec = if (state.isPlaying)
            MiniPlayerMotion.albumFloatSpec
        else
            MiniPlayerMotion.albumIdleFloatSpec,
        label = "mp_album_float"
    )

    // ═════════════════════════════════════════════════════════════════════
    // BEAT PULSE — Scale ripple on beat detection
    // ═══════════════════════════════════════════════════════════════════
    val beatScale by animateFloatAsState(
        targetValue = if (isBeatReacting) MiniPlayerTokens.BeatAlbumScalePeak else 1f,
        animationSpec = MiniPlayerMotion.beatPulseSpec,
        label = "mp_album_beat"
    )

    // ═════════════════════════════════════════════════════════════════════
    // PRESS SCALE — From interaction state (gesture engine provides this)
    // ═══════════════════════════════════════════════════════════════════
    val totalScale = beatScale * pressScale

    // ═══════════════════════════════════════════════════════════════════
    // CORNER RADIUS — Grows during morph
    // ═══════════════════════════════════════════════════════════════════
    // Collapsed: 8dp (RikoShape.MiniArtwork). Expanded: 16dp.
    val cornerRadiusDp = if (layout.albumArtSize > 100f) 16.dp else 8.dp

    // ═════════════════════════════════════════════════════════════════════
    // ELEVATION SHADOW — Energy-reactive, owned by Shadow component
    // ═════════════════════════════════════════════════════════════════════
    // Note: Shadow rendering is delegated to MiniPlayerShadow (Rule #24).
    // Album only provides the visual content — shadow wraps it externally.

    // ═════════════════════════════════════════════════════════════════════
    // GLOW — Ambient halo behind album (when expanding, Phase 4+)
    // ═════════════════════════════════════════════════════════════════════
    // Glow is owned by Background (Rule #24), but album-specific glow
    // (the halo directly behind the album art) is rendered here
    // because it moves with the album art position.
    // This is the ALBUM glow, not the SURFACE glow.

    // ═════════════════════════════════════════════════════════════════════
    // RENDER — Album art or placeholder
    // ═════════════════════════════════════════════════════════════════════

    Box(
        modifier = modifier
            .size(albumSize)
            .offset(y = floatOffset.dp)
            .graphicsLayer {
                scaleX = totalScale
                scaleY = totalScale
            }
            .clip(RoundedCornerShape(cornerRadiusDp)),
        contentAlignment = Alignment.Center
    ) {
        if (hasAlbumArt || state.currentSong != null) {
            // ═══ Real Album Art ═══
            // P0 FIX (album-art mix-up): resolve embedded art per-song from
            // currentSong. The MediaStore albumart URI (state.albumArtUri)
            // collides across YouTube downloads — only a fallback.
            // PERF P0 (RC-06): mini-player art prescale
            // C4: remembered request + crossfade(false) — mini art is always
            // visible (not a hero); rebuilding the request per recomposition
            // restarts Coil and crossfade(true) animates every playback tick.
            val albumContext = LocalContext.current
            val song = state.currentSong
            val artworkModel = remember(song?.id, song?.uri, song?.albumId, albumArtUri, albumContext) {
                val data = if (song != null) {
                    com.rikodev.music.android.core.designsystem.riko.components
                        .resolveArtworkData(albumContext, song.id, song.albumId, song.uri)
                } else {
                    albumArtUri
                } ?: return@remember null
                com.rikodev.music.android.core.designsystem.riko.components
                    .buildArtworkRequest(
                        context = albumContext,
                        data = data,
                        songId = song?.id ?: 0L,
                        albumId = song?.albumId ?: 0L,
                        px = 256
                    )
            }
            if (artworkModel != null) {
                RikoArtwork(
                    model = artworkModel,
                    contentDescription = state.currentSong?.title ?: "Album art",
                    modifier = Modifier.fillMaxSize()
                )
            }

            // ═══ Reflection Overlay (Rule #24: Album owns reflection) ═══
            // Diagonal gradient creating "glass over print" effect.
            // Only visible when layout.reflectionAlpha > 0 (morphProgress > 0.70).
            if (reflectionAlpha > 0.01f) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = reflectionAlpha }
                ) {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                GlassSpecular.copy(alpha = 0.4f),
                                Color.Transparent
                            ),
                            start = Offset.Zero,
                            end = Offset(size.width, size.height)
                        )
                    )
                }
            }
        } else {
            // ═══ Placeholder (Rule #25: Layer Independence) ═══
            // Gradient placeholder using state.colors when no album art.
            // Still looks premium — never broken.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                color.primary.copy(alpha = 0.6f),
                                color.secondary.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .clip(RoundedCornerShape(cornerRadiusDp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = "No album art",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(albumSize * 0.4f)
                )
            }
        }
    }
}
