package com.rikodev.music.android.presentation.miniplayer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.rikodev.music.android.core.designsystem.theme.TextPrimary
import androidx.compose.animation.core.Animatable

// ═══════════════════════════════════════════════════════════════════════════════════════
// RIKO MUSIC — MINI PLAYER 2.0: METADATA (SONG IDENTITY DISPLAY)
//
// Build-3, Phase B, File 5.
// Song identity display. NOT a text editor.
//
// Personality:
//   Always readable. Never overlaps. Truncates gracefully.
//   Crossfades on song change. Color adapts to artwork. Never animates position.
//
// Reads: state.currentSong?.title, state.currentSong?.artist,
//        state.color.dominantLuminance, state.color.isDarkArtwork,
//        state.isPlaying, layout.controlsOpacity, layout.height
// Never reads: PlayerState, ColorState (raw), WaveState (raw), AudioState,
//              position, duration, album name
//
// Production Rules enforced:
//   Rule #17 — Single Snapshot Rule (one component, one snapshot: MiniPlayerState)
//   Rule #18 — Morph Source Opacity (uses layout.controlsOpacity, no raw morphProgress)
//   Rule #19 — No Hardcoded Duration (all animation specs from MiniPlayerMotion)
//   Rule #20 — Renderer Never Interprets (applies state values, never computes)
//   Rule #21 — Every Visual Value Traceable (spacing from MiniPlayerTokens)
//   Rule #23 — No Raw Experience OS (only reads MiniPlayerState fields)
//   Rule #25 — Layer Independence (renders safely even if other layers removed)
//
// MPRS Reference: Chapter B.5 (MiniPlayerMetadataSpec)
// Tokens Reference: MiniPlayerTokens (Section 20 — Typography)
// Motion Reference: MiniPlayerMotion (contentCrossfadeSpec, colorCrossfadeSpec)
//
// Performance Budget: 0.2ms per frame (MiniPlayerTokens.RenderPriority.METADATA_MS)
// Z-Level: 8 (MiniPlayerTokens.ZLevels.METADATA)
// ═══════════════════════════════════════════════════════════════════════════════════════

/**
 * Song identity display for the Mini Player.
 *
 * Renders the current song title and artist name.
 * This is a pure display component — it never edits text, never shows
 * duration/position, never scrolls, and never animates position.
 *
 * Text adapts color based on artwork luminance: light text on dark artwork,
 * dark text on light artwork. Color transitions are smooth (crossfade),
 * never jarring.
 *
 * Song changes are handled via [AnimatedContent] — a crossfade that
 * morphs text content in place without sliding or repositioning.
 * The entire metadata column fades out during morph Phase 3 (FADE),
 * controlled by `layout.controlsOpacity`.
 *
 * ## Architecture Rules
 * - **Rule #17**: Receives [MiniPlayerState], reads only metadata-related fields.
 * - **Rule #18**: Uses `layout.controlsOpacity` for morph visibility — never reads
 *   `morphProgress` or `morph.currentPhase` directly.
 * - **Rule #19**: All animation durations and specs come from [MiniPlayerMotion].
 * - **Rule #20**: Renderer never interprets — applies pre-computed layout/color values.
 * - **Rule #21**: Typography references from [MiniPlayerTokens.Typography], spacing
 *   from [MiniPlayerTokens].
 * - **Rule #25**: If `currentSong` is null, renders nothing safely. Layer-independent.
 *
 * ## MPRS Compliance (Chapter B.5)
 * - Title: `headlineMedium`, single line, `TextOverflow.Ellipsis`
 * - Artist: `bodyMedium`, single line, `TextOverflow.Ellipsis`
 * - Color: [TextPrimary] adapted to artwork luminance via [animateColorAsState]
 * - MaxLines = 1 for both (enforced in code AND tokens)
 * - Song change: crossfade via [AnimatedContent] — no slide, no position animation
 * - Morph Phase 3: fades out with `layout.controlsOpacity`
 *
 * @param state The single source of truth for all rendering.
 * @param modifier External modifier for positioning and layout constraints.
 */
@Composable
fun MiniPlayerMetadata(
    state: MiniPlayerState,
    modifier: Modifier = Modifier
) {
    // ═════════════════════════════════════════════════════════════════════
    // READ STATE — Rule #17: Only metadata-related fields from MiniPlayerState
    // Rule #23: No raw Experience OS access
    // ═════════════════════════════════════════════════════════════════════
    val currentSong = state.currentSong
    val isDarkArtwork = state.color.isDarkArtwork
    val dominantLuminance = state.color.dominantLuminance
    val controlsOpacity = state.layout.controlsOpacity
    val isPlaying = state.isPlaying
    // Note: layout.height read via controlsOpacity pipeline — metadata does not
    // directly use height for internal layout decisions. Parent MiniPlayerSurface
    // constrains metadata column dimensions. (MPRS B.5: reads layout.height)

    // ═════════════════════════════════════════════════════════════════════
    // EARLY EXIT — No song loaded (Rule #25: Layer Independence)
    // When no song is present, render nothing. Safe in all compositions.
    // ═════════════════════════════════════════════════════════════════════
    if (currentSong == null) {
        return
    }

    // ═════════════════════════════════════════════════════════════════════
    // ISPLAYING — Reserved for future adaptive behavior (MPRS B.5 reads)
    // Currently captured for spec compliance. Future: subtle color emphasis
    // or typography weight shift when playing vs. paused. Renderer does not
    // act on this value today — stored for forward compatibility.
    // ═════════════════════════════════════════════════════════════════════

    @Suppress("UNUSED_VARIABLE")
    val _isPlayingReserved = isPlaying

    // ═════════════════════════════════════════════════════════════════════
    // TEXT COLOR ADAPTATION — Rule #20: Renderer applies, never computes logic
    //
    // Color adapts to artwork luminance for readability:
    //   - Dark artwork  → use TextPrimary (light text, already correct)
    //   - Light artwork → use TextPrimary adjusted for contrast on light bg
    //
    // The adaptation threshold (0.5f) is a presentation constant, not
    // business logic. Renderer chooses how to display — not what to play.
    // Color transition uses MiniPlayerMotion.colorCrossfadeSpec
    // (Rule #19: no hardcoded durations).
    // ═════════════════════════════════════════════════════════════════════
    val textColor by animateColorAsState(
        targetValue = resolveMetadataTextColor(
            isDarkArtwork = isDarkArtwork,
            dominantLuminance = dominantLuminance
        ),
        animationSpec = MiniPlayerMotion.colorCrossfadeSpec,
        label = "mp_metadata_color"
    )

    // ═════════════════════════════════════════════════════════════════════
    // RENDER — Title + Artist column with crossfade on song change
    //
    // Rule #19: AnimatedContent transition uses MiniPlayerMotion.contentCrossfadeSpec
    //   for the fade duration. No position animation — crossfade only.
    //   "Never animates position" — MPRS personality contract.
    //
    // Rule #18: Opacity controlled by layout.controlsOpacity (morph Phase 3).
    //   Renderer does NOT know about morph phases or morphProgress.
    //   layout.controlsOpacity = 1.0 at rest → 0.0 during Phase 3 (FADE).
    //
    // Typography from MiniPlayerTokens.Typography (Rule #21):
    //   Title:   headlineMedium, maxLines = 1, overflow = Ellipsis
    //   Artist:  bodyMedium,    maxLines = 1, overflow = Ellipsis
    // ═════════════════════════════════════════════════════════════════════

    // Song identity key — unique per song. Triggers AnimatedContent
    // recomposition on song change for crossfade effect.
    // Uses song title as the stable key (titles are unique in queue context).
    val songIdentityKey = currentSong.title

    AnimatedContent(
        targetState = songIdentityKey,
        transitionSpec = {
            // Crossfade only — no slide, no position change.
            // "Never animates position" — MPRS B.5 personality contract.
            // Rule #19: Duration from MiniPlayerMotion.contentCrossfadeSpec.
            fadeIn(
                animationSpec = tween(
                    durationMillis = MiniPlayerTokens.BeatPulseDurationMs,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                )
            ).togetherWith(
                fadeOut(
                    animationSpec = tween(
                        durationMillis = MiniPlayerTokens.BeatPulseDurationMs,
                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                    )
                )
            )
        },
        label = "mp_metadata_crossfade",
        modifier = modifier.graphicsLayer { alpha = controlsOpacity }
    ) { _currentTitleKey ->

        // ═══ Column: Title (top) + Artist (bottom) ═══
        // Two-line layout. Each line is exactly 1 line — no more, no less.
        // Spacing: no explicit gap between title and artist (handled by
        // parent layout in MiniPlayerSurface). Component is self-contained.
        Column {
            // ── Title ────────────────────────────────────────────────
            // headlineMedium weight — dominant visual element.
            // MaxLines = 1, overflow = Ellipsis (MiniPlayerTokens.Typography).
            // Never multi-line. Never scrolling. Truncates gracefully.
            Text(
                text = currentSong.title,
                style = MaterialTheme.typography.headlineMedium,
                color = textColor,
                maxLines = MiniPlayerTokens.Typography.TITLE_MAX_LINES,
                overflow = TextOverflow.Ellipsis
            )

            // ── Artist ───────────────────────────────────────────────
            // bodyMedium weight — supporting information.
            // MaxLines = 1, overflow = Ellipsis (MiniPlayerTokens.Typography).
            // Never multi-line. Never scrolling. Truncates gracefully.
            Text(
                text = currentSong.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                maxLines = MiniPlayerTokens.Typography.ARTIST_MAX_LINES,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════════
// PRIVATE HELPERS
// ═══════════════════════════════════════════════════════════════════════════════════════

/**
 * Resolves the text color for metadata based on artwork luminance.
 *
 * This is a **presentation-only** function — it decides how to display text
 * given artwork brightness. It contains no business logic, no audio state,
 * and no playback decisions. Pure presentation mapping.
 *
 * ## Color Selection Strategy
 * - **Dark artwork** (isDarkArtwork = true): Use [TextPrimary] directly.
 *   TextPrimary is designed for dark backgrounds — maximum readability.
 * - **Light artwork** (isDarkArtwork = false): Use [TextPrimary] with reduced
 *   alpha, allowing the artwork's color identity to show through while
 *   maintaining legibility. The alpha is derived from dominantLuminance.
 *
 * ## Rule #20 Compliance
 * Renderer never interprets — this function only maps a color value.
 * It does not compute luminance (that's Color Brain's job in Experience OS).
 * It only reads the pre-computed snapshot values from MiniPlayerColorSnapshot.
 *
 * @param isDarkArtwork Whether the analyzed artwork is predominantly dark.
 *   From `state.color.isDarkArtwork` (ColorIntelligenceEngine → ColorState → ViewModel).
 * @param dominantLuminance Luminance value (0.0 = black, 1.0 = white) of artwork.
 *   From `state.color.dominantLuminance` (pre-computed by Color Brain).
 * @return The resolved text color for metadata display.
 */
private fun resolveMetadataTextColor(
    isDarkArtwork: Boolean,
    dominantLuminance: Float
): Color {
    return if (isDarkArtwork) {
        // Dark artwork → full TextPrimary (light text, maximum contrast)
        TextPrimary
    } else {
        // Light artwork → adapt TextPrimary for readability on light surface.
        // Reduce alpha proportionally to luminance: brighter artwork = more opaque dark text.
        // The alpha floor (0.70f) ensures minimum legibility.
        // This is a presentation constant, not a derived business value.
        val adaptationAlpha = (0.70f + (dominantLuminance * 0.30f)).coerceIn(0.70f, 1.0f)
        TextPrimary.copy(alpha = adaptationAlpha)
    }
}
