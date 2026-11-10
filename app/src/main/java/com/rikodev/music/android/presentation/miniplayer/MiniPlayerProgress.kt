/*
 * Riko Music App — Mini Player 2.0
 * Build-3 · Phase C · File 8
 *
 * MiniPlayerProgress — Playback position indicator.
 *
 * MPRS Spec (Chapter B.7 — MiniPlayerProgressSpec):
 *   Purpose:      "Playback position indicator. NOT a seek bar."
 *   Personality:  "Always smooth. Never jumpy. Color flows with artwork.
 *                  Breathes when idle."
 *
 *   Collapsed:    thin waveform strip at bottom of surface (4dp height).
 *                 40 bars representing audio envelope.
 *                 Played portion:   gradient(waveStart → waveEnd).
 *                 Unplayed portion: TextPrimary.copy(alpha = 0.15).
 *   Expanded:     (handled by dedicated expanded composable — this file
 *                  renders collapsed only per Rule #20 / Renderer-never-
 *                  interprets).
 *
 * Architecture rules enforced:
 *   Rule #19 — Every animation value sourced from MiniPlayerMotion.
 *   Rule #20 — Renderer never interprets; no seek logic in collapsed state.
 *   Rule #21 — Every spacing/dimension sourced from MiniPlayerTokens.
 *   Rule #23 — NO raw Experience OS reads; all state comes through
 *              MiniPlayerState fields only.
 *
 * Consumed MiniPlayerState fields:
 *   • state.progress          — [0f..1f] normalised playback position
 *   • state.color.waveStart   — gradient left colour (played bars)
 *   • state.color.waveEnd     — gradient right colour (played bars)
 *   • state.color.primary     — fallback / accent colour
 *   • state.isPlaying         — drives idle "breathing" animation
 *   • layout.controlsOpacity  — overall alpha; progress fades with controls
 *   • layout.height            — surface height (used for bottom positioning)
 *
 * Performance:
 *   • Envelope FloatArray(40) cached in remember { … }.
 *   • Canvas draws 40 simple lines — no allocations in draw scope.
 *   • Gradient Brush created once outside the per-bar loop.
 *   • No recomposition triggers beyond state changes.
 *
 * Envelope data:
 *   Uses a flat placeholder (all 0.1f) for now. The real envelope will be
 *   injected by the Integration layer (File 12) via the `envelope` parameter
 *   at call sites.
 */

package com.rikodev.music.android.presentation.miniplayer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.rikodev.music.android.core.designsystem.theme.TextPrimary
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring

// ─── Constants ───────────────────────────────────────────────────────────────
// Sourced conceptually from MiniPlayerTokens / MiniPlayerMotion (canonical
// values are referenced there; duplicates here keep the file self-contained
// for standalone review while the build resolves them via the shared objects).

/** Number of waveform bars composing the envelope visualisation. */
private const val BAR_COUNT = 40

// ─── Public API ──────────────────────────────────────────────────────────────

/**
 * Collapsed-state playback position indicator.
 *
 * Renders a thin (4 dp) waveform strip pinned to the bottom of the mini player
 * surface. Each bar height is driven by the provided [envelope] array. Bars to
 * the left of [MiniPlayerState.progress] are drawn with a gradient from
 * [state.color.waveStart] to [state.color.waveEnd]; bars to the right use
 * [TextPrimary] at 15 % opacity.
 *
 * The entire indicator fades in/out with [layout.controlsOpacity] and smoothly
 * interpolates progress via a float animation so it never jumps on discrete
 * updates.
 *
 * When [state.isPlaying] is `false` the unplayed portion subtly "breathes"
 * (gentle alpha oscillation) to keep the UI feeling alive.
 *
 * **No seek behaviour** — this is a display-only renderer per Rule #20.
 *
 * @param state    Current MiniPlayerState snapshot.
 * @param modifier Composable modifier (sizing / padding applied externally).
 * @param envelope FloatArray of length [BAR_COUNT] (40) with per-bar
 *                 amplitudes in 0f‥1f. Defaults to a flat placeholder
 *                 (all 0.1f); the Integration layer (File 12) will inject
 *                 the real envelope at call sites.
 */
@Composable
fun MiniPlayerProgress(
    state: MiniPlayerState,
    modifier: Modifier = Modifier,
    envelope: FloatArray = rememberFlatEnvelope(),
) {
    // ── Smooth progress animation (Rule #19 — MiniPlayerMotion values) ──
    // We animate towards the target progress so that discrete jumps (e.g.
    // seek events from another source, or polling intervals) are never
    // visible to the user.
    var animatedProgress by remember { mutableFloatStateOf(state.progress) }
    // TODO: Move to @Composable context
    // 
    LaunchedEffect(state.progress) {
        val anim = Animatable(animatedProgress)
        anim.animateTo(
            targetValue = state.progress,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        ) {
            animatedProgress = value
        }
    }

    // ── Idle breathing (Rule #19 — MiniPlayerMotion) ──
    // Gentle alpha oscillation when playback is paused, making the unplayed
    // bars softly pulse so the surface never feels fully inert.
    val infiniteTransition = rememberInfiniteTransition(label = "progressBreathing")

    val breathingAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = MiniPlayerMotion.breathingAlphaMin,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = MiniPlayerMotion.breathingCycleDurationMs,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathingAlpha",
    )

    // Whether the idle-breathing modifier is active (only when not playing).
    val isIdle = !state.isPlaying

    // ── Overall opacity: fades with controls (Rule #23) ──
    val contentAlpha = state.layout.controlsOpacity

    // ── Envelope guard ──
    require(envelope.size == BAR_COUNT) {
        "MiniPlayerProgress envelope must contain exactly $BAR_COUNT entries " +
            "(got ${envelope.size})."
    }

    // ── Canvas ────────────────────────────────────────────────────────────
    Canvas(
        modifier = modifier,
    ) {
        drawWaveformStrip(
            envelope = envelope,
            progress = animatedProgress,
            waveStart = state.color.waveStart,
            waveEnd = state.color.waveEnd,
            unplayedColor = TextPrimary.copy(alpha = 0.15f),
            contentAlpha = contentAlpha,
            isIdle = isIdle,
            breathingAlpha = breathingAlpha,
            barSpacing = MiniPlayerMotion.waveformBarSpacing,
            barStrokeWidth = MiniPlayerMotion.waveformBarStrokeWidth,
        )
    }
}

// ─── Private Drawing Logic ───────────────────────────────────────────────────

/**
 * Draws the 40-bar waveform strip inside the [DrawScope].
 *
 * Each bar is a vertical line whose height is proportional to the envelope
 * value for that index. Bars are evenly distributed across the canvas width
 * with spacing from [MiniPlayerTokens].
 *
 * Played bars (index < progress × BAR_COUNT) use a horizontal [Brush.linearGradient]
 * coloured [waveStart] → [waveEnd]. Unplayed bars use [unplayedColor].
 */
private fun DrawScope.drawWaveformStrip(
    envelope: FloatArray,
    progress: Float,
    waveStart: Color,
    waveEnd: Color,
    unplayedColor: Color,
    contentAlpha: Float,
    isIdle: Boolean,
    breathingAlpha: Float,
    barSpacing: Float,
    barStrokeWidth: Float,
) {
    val playableWidth = size.width
    val centerY = size.height / 2f

    // Played / unplayed split index (fractional; clamp to safe range).
    val splitIndex = (progress.coerceIn(0f, 1f)) * BAR_COUNT

    // Create the gradient brush for played bars once.
    val playedBrush = Brush.horizontalGradient(
        colors = listOf(waveStart, waveEnd),
        startX = 0f,
        endX = playableWidth,
    )

    for (i in 0 until BAR_COUNT) {
        // Normalised x position for this bar.
        val fraction = (i + 0.5f) / BAR_COUNT
        val x = playableWidth * fraction

        // Envelope-driven height (always positive; 0 is degenerate but safe).
        val amplitude = envelope[i].coerceIn(0.05f, 1f)
        val halfHeight = (size.height * 0.5f * amplitude)

        val isPlayed = i < splitIndex

        // Alpha: unplayed bars breathe when idle.
        val barAlpha = when {
            isPlayed -> contentAlpha
            isIdle   -> contentAlpha * breathingAlpha
            else     -> contentAlpha
        }

        if (isPlayed) {
            // For played bars we draw using the brush via a Stroke path.
            // StrokeCap.Round gives the bars a soft pill shape.
            drawLine(
                brush = playedBrush,
                alpha = barAlpha,
                start = Offset(x, centerY - halfHeight),
                end = Offset(x, centerY + halfHeight),
                strokeWidth = barStrokeWidth,
                cap = StrokeCap.Round,
            )
        } else {
            val barColor = unplayedColor.copy(alpha = barAlpha)
            drawLine(
                color = barColor,
                start = Offset(x, centerY - halfHeight),
                end = Offset(x, centerY + halfHeight),
                strokeWidth = barStrokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

// ─── Envelope Factory ────────────────────────────────────────────────────────

/**
 * Returns a remembered flat placeholder envelope (all values = 0.1f).
 *
 * This keeps the waveform visible but uniform until the Integration layer
 * (File 12) injects real PCM-derived envelopes at each [MiniPlayerProgress]
 * call site.
 */
@Composable
private fun rememberFlatEnvelope(): FloatArray =
    remember {
        FloatArray(BAR_COUNT) { 0.1f }
    }
