/*
 * Riko Music App — Mini Player 2.0
 * Build-3 · Phase C · File 7
 *
 * MiniPlayerControls — Music interaction. NOT playback logic.
 *
 * MPRS Spec (Chapter B.6 — MiniPlayerControlsSpec):
 *   Purpose:      "Music interaction. NOT playback logic."
 *   Personality:  "Never pop. Never teleport. Always pressure-based.
 *                  Bouncy on release. Icon changes are instant.
 *                  Never disabled-looking."
 *
 *   Collapsed:    [PlayPause 38dp] [SkipNext 38dp] — 2 buttons, SmallBubbleIcon style.
 *   Expanded:     (future — prepared structurally, not rendered yet)
 *                 full controls: PlayPause + Prev + Next + Favorite.
 *
 * Architecture rules enforced:
 *   Rule #17 — Single Snapshot: reads only MiniPlayerState fields listed below.
 *   Rule #19 — Every animation value sourced from MiniPlayerMotion.
 *   Rule #20 — Renderer never interprets; emits MiniPlayerAction only.
 *   Rule #21 — Every spacing/dimension sourced from MiniPlayerTokens.
 *   Rule #23 — NO raw Experience OS reads; all state comes through
 *              MiniPlayerState fields only.
 *
 * Consumed MiniPlayerState fields:
 *   • state.isPlaying               → play/pause icon selection (instant swap)
 *   • state.isLoading               → (read but never disables buttons — AT-CT-8)
 *   • state.isFavorite              → favorite icon state (expanded, future)
 *   • state.interaction.pressScale  → press feedback, applied via graphicsLayer
 *   • state.interaction.pressTarget → which button is pressed
 *   • state.interaction.isPressed   → global press state
 *   • layout.controlsOpacity        → fades out during Phase 3 (1 → 0)
 *   • layout.albumArtSize           → button size scaling reference
 *
 * Output actions (NEVER executes playback directly):
 *   • MiniPlayerAction.TogglePlayPause   — play/pause button tap
 *   • MiniPlayerAction.SkipNext           — skip-next button tap
 *   • MiniPlayerAction.SkipPrevious       — skip-previous (expanded, future)
 *   • MiniPlayerAction.ToggleFavorite      — favorite (expanded, future)
 *   • MiniPlayerAction.HapticFeedback     — haptic on every button tap
 *
 * Pixel ownership (Rule #24):
 *   • Controls Icons   → this component
 *   • Press Feedback   → this component (graphicsLayer scale)
 *
 * Forbidden (from MiniPlayerControlsSpec):
 *   ❌ play() / pause() / seek() / favorite() — emit actions only
 *   ❌ Direct PlayerManager / PlaybackService / PlayerState access
 *   ❌ remember {} with mutable interaction state
 *   ❌ Hardcoded animation duration (all from MiniPlayerMotion)
 *   ❌ Own press detection logic (state from MiniPlayerState)
 *   ❌ Material ripple (pressure-based scale only)
 *   ❌ Business logic of any kind
 */

package com.rikodev.music.android.presentation.miniplayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import com.rikodev.music.android.core.designsystem.glass.SmallBubbleIcon
import androidx.compose.animation.core.Animatable

// ═══════════════════════════════════════════════════════════════════════════════════════
// RIKO MUSIC — MINI PLAYER 2.0: CONTROLS
//
// Build-3, Phase C, File 7.
// Music interaction. NOT playback logic.
//
// Personality:
//   Never pop. Never teleport.
//   Always pressure-based. Bouncy on release.
//   Icon changes are instant. Never disabled-looking.
//
// Reads: state.isPlaying, state.isLoading, state.isFavorite,
//        state.interaction.pressScale, state.interaction.pressTarget,
//        state.interaction.isPressed, layout.controlsOpacity, layout.albumArtSize
// Never reads: PlayerState, ColorState (raw), WaveState (raw), AudioState
//
// Pixel Owner: Controls Icons + Press Feedback scale (Rule #24)
// Layer Independence: If controls removed, rest of mini player still renders (Rule #25)
//
// MPRS Reference: Chapter B.6 (MiniPlayerControlsSpec)
// Tokens Reference: MiniPlayerTokens (Section 2 — Controls, Section 12 — Press)
// Motion Reference: MiniPlayerMotion (Rule #19 — all animation specs)
// ═══════════════════════════════════════════════════════════════════════════════════════

/**
 * Transport controls for the Mini Experience Surface.
 *
 * Renders pressure-responsive glass buttons for playback interaction.
 * Collapsed layout shows PlayPause + SkipNext (2 buttons, 38dp each).
 * Expanded layout (future): PlayPause + Prev + Next + Favorite.
 *
 * **Pure renderer** — emits [MiniPlayerAction] on interaction, never executes
 * playback directly. Press feedback reads from [MiniPlayerState.interaction]
 * and applies via `graphicsLayer` (GPU-composited, zero allocations in draw scope).
 *
 * Rule #17 — Single Snapshot: Receives [MiniPlayerState], reads only
 *   controls-related fields listed in the header.
 * Rule #18 — Morph Source Opacity: Uses `layout.controlsOpacity` for fade.
 *   Never computes morph values from `morphProgress`.
 * Rule #19 — No Hardcoded Duration: All specs from [MiniPlayerMotion].
 * Rule #20 — Renderer Never Interprets: Applies pre-computed state values.
 *   Emits actions, never calls play()/pause()/seek()/favorite().
 * Rule #21 — Every Visual Value Traceable: All sizes/gaps from [MiniPlayerTokens].
 * Rule #23 — No raw Experience OS reads.
 * Rule #24 — Pixel Owner: Controls icons + press feedback scale.
 *
 * @param state    The single source of truth for all rendering.
 * @param onAction Emits [MiniPlayerAction] to PlayerViewModel. Never executes directly.
 * @param modifier External modifier for positioning from parent layout.
 */
@Composable
fun MiniPlayerControls(
    state: MiniPlayerState,
    onAction: (MiniPlayerAction) -> Unit,
    modifier: Modifier = Modifier
) {
    // ═════════════════════════════════════════════════════════════════════
    // READ STATE — Rule #17: Only controls-related fields
    // ═════════════════════════════════════════════════════════════════════
    val isPlaying = state.isPlaying
    val isFavorite = state.isFavorite
    val pressScale = state.interaction.pressScale
    val pressTarget = state.interaction.pressTarget
    val isPressed = state.interaction.isPressed
    val controlsOpacity = state.layout.controlsOpacity
    val albumArtSize = state.layout.albumArtSize

    // ═════════════════════════════════════════════════════════════════════
    // LAYOUT — Collapsed: 2 buttons (PlayPause + SkipNext)
    // Spacing from MiniPlayerTokens (Rule #21)
    // ═════════════════════════════════════════════════════════════════════
    Row(
        modifier = modifier
            // Rule #18: Opacity driven by layout.controlsOpacity (from state, not morphProgress).
            // GPU-composited — zero draw-scope allocations.
            .graphicsLayer { alpha = controlsOpacity },
        horizontalArrangement = Arrangement.spacedBy(MiniPlayerTokens.ControlsGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Play / Pause ────────────────────────────────────────────────
        // Primary control. 38dp. SmallBubbleIcon style.
        // Icon swap is INSTANT — no morph, no crossfade (spec requirement).
        // "Never disabled-looking" — no alpha reduction, no color desaturation,
        // even when isLoading is true (AT-CT-8).
        ControlBubbleButton(
            target = PressTarget.PLAY_PAUSE,
            isPressed = isPressed,
            pressTarget = pressTarget,
            pressScale = pressScale,
            size = MiniPlayerTokens.PlayPauseButtonSize,
            onClick = {
                onAction(MiniPlayerAction.TogglePlayPause)
                onAction(MiniPlayerAction.HapticFeedback(HapticType.TAP))
            }
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(MiniPlayerTokens.IconSize),
                tint = Color.White
            )
        }

        // ── Skip Next ───────────────────────────────────────────────────
        // Secondary control. 38dp. SmallBubbleIcon style.
        ControlBubbleButton(
            target = PressTarget.SKIP_NEXT,
            isPressed = isPressed,
            pressTarget = pressTarget,
            pressScale = pressScale,
            size = MiniPlayerTokens.SkipButtonSize,
            onClick = {
                onAction(MiniPlayerAction.SkipNext)
                onAction(MiniPlayerAction.HapticFeedback(HapticType.TAP))
            }
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Skip to next track",
                modifier = Modifier.size(MiniPlayerTokens.IconSize),
                tint = Color.White
            )
        }

        // ── Expanded Controls (future — structural preparation) ──────────
        // When expanded layout is activated, add here:
        //   ControlBubbleButton(PressTarget.SKIP_PREVIOUS, …) { SkipPrevious icon }
        //   ControlBubbleButton(PressTarget.FAVORITE, …) { Favorite icon }
        // Layout gap, size, and press behavior already tokenized.
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// PRIVATE — Control Bubble Button Wrapper
//
// Wraps [SmallBubbleIcon] with centralized press feedback from
// [MiniPlayerState.interaction]. The press scale is applied via graphicsLayer
// on a wrapper, sourced from the interaction engine (not from local state).
//
// "NEVER own press detection logic" — SmallBubbleIcon's built-in tap detection
// is from the design system, not from this file. The press VISUAL FEEDBACK
// (scale) comes from state.interaction.pressScale.
//
// Float is disabled — controls are grounded, pressure-focused.
// No Material ripple — scale is the only press indicator.
// ═══════════════════════════════════════════════════════════════════════════════════════

/**
 * A single control button wrapped in the SmallBubbleIcon glass style
 * with centralized press feedback from [MiniPlayerState.interaction].
 *
 * Press feedback flow:
 *   Interaction Engine → state.interaction.pressScale / pressTarget
 *   → this composable reads it → applies via graphicsLayer (GPU)
 *
 * Tap flow:
 *   SmallBubbleIcon (design system) → onClick → caller emits MiniPlayerAction
 *
 * @param target      Which [PressTarget] this button represents.
 * @param isPressed   Whether ANY press is active (from state.interaction.isPressed).
 * @param pressTarget Which element is currently pressed (from state.interaction.pressTarget).
 * @param pressScale  Current press scale (from state.interaction.pressScale).
 * @param size        Button visual size (from [MiniPlayerTokens]).
 * @param onClick     Action to emit when tapped.
 * @param content     Icon content to render inside the bubble.
 */
@Composable
private fun ControlBubbleButton(
    target: PressTarget,
    isPressed: Boolean,
    pressTarget: PressTarget,
    pressScale: Float,
    size: Dp,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    // ═════════════════════════════════════════════════════════════════════
    // PRESS FEEDBACK — From state.interaction, applied via graphicsLayer
    // Rule #20: Renderer never interprets. Just applies the pre-computed value.
    // Only scales when THIS button is the active press target.
    // When not the target, scale remains 1.0f (no visual change).
    // ═════════════════════════════════════════════════════════════════════
    val isThisButtonPressed = isPressed && pressTarget == target
    val effectiveScale = if (isThisButtonPressed) pressScale else 1f

    SmallBubbleIcon(
        modifier = Modifier.graphicsLayer {
            // Centralized press feedback from interaction engine.
            // Applied on the SmallBubbleIcon's layer — GPU-composited.
            // Spring animation on release is handled by the interaction engine
            // updating pressScale over time (Rule #19: animation from MiniPlayerMotion).
            scaleX = effectiveScale
            scaleY = effectiveScale
        },
        size = size,
        // Controls are always "active" visually — never disabled-looking.
        // The isActive tint uses the primary color from the design system default.
        isActive = true,
        // Float disabled — controls are grounded, pressure-focused.
        // The personality is "Always pressure-based", not floating.
        enableFloat = false,
        onClick = onClick,
        content = content
    )
}
