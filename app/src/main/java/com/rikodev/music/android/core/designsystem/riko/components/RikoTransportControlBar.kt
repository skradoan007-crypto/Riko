package com.rikodev.music.android.core.designsystem.riko.components

import com.rikodev.music.android.core.designsystem.riko.theme.RikoStateTokens
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.theme.AccentViolet
import com.rikodev.music.android.core.designsystem.riko.theme.RikoComponentSize
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMotion
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextPrimary
import com.rikodev.music.android.core.designsystem.riko.theme.RikoTextSecondary

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — TRANSPORT CONTROL BAR (SM-UI-SPEC-001 v2.0, Section 2.21 + D5)
//
// 5 icon buttons, no background container, evenly spaced across full content
// width.
//
// D5 FINALIZED — exact reference order, do NOT "correct" to convention:
//   Shuffle(24dp) · SkipNext(28dp, "▶|") · PlayPause(64dp filled glass circle,
//   icon 24dp) · SkipPrevious(28dp, "|◀") · Queue/Repeat(24dp)
//
// Note: the NEXT icon sits LEFT of play and PREVIOUS sits RIGHT — the reverse
// of most players. Intentional (spec D5), not a bug.
// ─────────────────────────────────────────────────────────────────────────────

private val TransportSideIconSize = 28.dp
private val TransportSmallIconSize = 24.dp
private val TransportPlayIconSize = 24.dp
private val TransportPlayCircleSize = 64.dp

@Composable
fun RikoTransportControlBar(
    isPlaying: Boolean,
    isShuffleOn: Boolean,
    onShuffleClick: () -> Unit,
    onNextClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onQueueClick: () -> Unit,
    modifier: Modifier = Modifier,
    repeatModeOn: Boolean = false
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Shuffle — 24dp, aurora-tinted when on (accent rule).
        TransportIcon(
            icon = Icons.Rounded.Shuffle,
            description = "Shuffle",
            onClick = onShuffleClick,
            size = TransportSmallIconSize,
            tint = if (isShuffleOn) AccentVioletTransport else RikoTextPrimary.copy(alpha = 0.85f)
        )

        // 2. SkipNext — 28dp. LEFT of play (D5).
        TransportIcon(
            icon = Icons.Rounded.SkipNext,
            description = "Next",
            onClick = onNextClick,
            size = TransportSideIconSize,
            tint = RikoTextPrimary
        )

        // 3. PlayPause — 64dp glass circle, icon 24dp.
        val playInteraction = remember { MutableInteractionSource() }
        val playPressed by playInteraction.collectIsPressedAsState()
        val playScale by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (playPressed) RikoStateTokens.PressScale else 1f,
            animationSpec = RikoMotion.fast(),
            label = "transportPlayPress"
        )
        Box(
            modifier = Modifier
                .size(TransportPlayCircleSize)
                .scale(playScale)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.20f))
                .clickable(
                    interactionSource = playInteraction,
                    indication = null,
                    onClick = onPlayPauseClick
                )
                .semantics {
                    contentDescription = if (isPlaying) "Pause" else "Play"
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = RikoTextSecondary,
                modifier = Modifier.size(TransportPlayIconSize)
            )
        }

        // 4. SkipPrevious — 28dp. RIGHT of play (D5).
        TransportIcon(
            icon = Icons.Rounded.SkipPrevious,
            description = "Previous",
            onClick = onPreviousClick,
            size = TransportSideIconSize,
            tint = RikoTextPrimary
        )

        // 5. Queue — 24dp (SM spec 2.21: D5 trailing = Queue/Repeat).
        TransportIcon(
            icon = Icons.Rounded.QueueMusic,
            description = "Queue",
            onClick = onQueueClick,
            size = TransportSmallIconSize,
            tint = RikoTextPrimary.copy(alpha = 0.85f)
        )
    }
}

/** Accent for transport active states — aurora violet (accent rule). */
private val AccentVioletTransport = AccentViolet

@Composable
private fun TransportIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp,
    tint: Color
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val tintState by animateColorAsState(
        targetValue = if (pressed) tint.copy(alpha = 0.6f) else tint,
        animationSpec = RikoMotion.fast(),
        label = "transportTint"
    )
    Box(
        modifier = Modifier
            .size(56.dp) // 44dp+ hit target (C5), visible glyph smaller
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tintState,
            modifier = Modifier.size(size)
        )
    }
}
