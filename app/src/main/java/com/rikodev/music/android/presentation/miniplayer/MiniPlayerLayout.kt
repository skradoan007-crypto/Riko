package com.rikodev.music.android.presentation.miniplayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring

// ═══════════════════════════════════════════════════════════════════════════════════════
// RIKO MUSIC — MINI PLAYER 2.0: LAYOUT — THE ORCHESTRATOR
//
// Build-3, Phase D, File 10.
//
// THE most important file in Phase D.
// Component composition root. Orders z-layers, sizes content.
//
// Personality: "Invisible organizer. Zero visual output. Pure structure."
//
// This component produces NO pixels of its own.
// It ONLY composes child components in the correct z-order.
// Every visual pixel is owned by exactly one child component (Rule #24).
//
// ── Architecture Rules Enforced ─────────────────────────────────────────────────
//
//   Rule #17 — Receives MiniPlayerState, passes full state to children.
//              Children read their own sub-state fields.
//
//   Rule #18 — Children NEVER see morphProgress.
//              They see layout.height, layout.albumArtSize, layout.controlsOpacity.
//              This component never extracts or exposes morphProgress.
//
//   Rule #19 — All animation specs sourced from MiniPlayerMotion.
//              No hardcoded durations, no hardcoded easing curves.
//
//   Rule #20 — No interpretation, no computation in Layout.
//              Layout passes state to children. Layout does NOT decide
//              "if morphProgress > 0.5, switch to Column."
//              Children read layout.* and adapt themselves.
//
//   Rule #24 — Z-order strictly enforced:
//     Background (Z-0) → Shadow (Z-1,2,3) → Surface (Z-3) → Noise (Z-4) →
//     Wave (Z-5) → Album (Z-6, Z-7 reflection) → Metadata (Z-8) →
//     Progress (Z-9) → Controls (Z-10) → Gesture (Z-11, Z-12)
//
// ── Composition Structure ─────────────────────────────────────────────────────────
//
//   AnimatedVisibility (appear / disappear gate)
//     └─ MiniPlayerGestureLayer (Z-11, Z-12 — outermost, intercepts gestures first)
//        └─ MiniPlayerShadow (Z-1, Z-2, Z-3 — provides depth shadow to surface)
//           ├─ MiniPlayerBackground (Z-0 — ambient atmosphere, behind surface)
//           └─ MiniPlayerSurface (Z-3 — glass body container)
//              └─ MiniPlayerContent (internal layout organizer)
//                 ├─ MiniPlayerWave (Z-5 — audio-reactive ribbon)
//                 ├─ MiniPlayerProgress (Z-9 — progress strip at bottom)
//                 └─ Row (main content)
//                    ├─ MiniPlayerAlbum (Z-6, Z-7 reflection)
//                    ├─ MiniPlayerMetadata (Z-8)
//                    └─ MiniPlayerControls (Z-10)
//
// ── Collapsed Layout (80dp height) ────────────────────────────────────────────────
//
//   ┌────────────────────────────────────────────────────────────┐
//   │  [Album] ─12dp─ [Title          ] ─8dp─ [▶] [⏭]         │
//   │  [52dp ]         [Artist         ]       [38dp][38dp]      │
//   │  ══════════════════════════════════════════════  (4dp)     │
//   │                                        8dp ↓               │
//   └────────────────────────────────────────────────────────────┘
//     ← 10dp →                                  ← 10dp →
//
//   Dimensions:
//     Total height: 80dp (from layout.height)
//     Corner radius: 28dp (from layout.cornerRadius)
//     Width fraction: 0.94 (from layout.widthFraction)
//     Content padding horizontal: 10dp (MiniPlayerTokens.ContentPaddingHorizontal)
//     Album–Metadata gap: 12dp (MiniPlayerTokens.AlbumMetadataGap)
//     Metadata–Controls gap: 8dp (MiniPlayerTokens.MetadataControlsGap)
//     Controls internal gap: 8dp (MiniPlayerTokens.ControlsGap)
//     Progress bottom padding: 8dp (MiniPlayerTokens.ProgressBottomPadding)
//
// ── Expanded Layout (morph in progress) ────────────────────────────────────────────
//
//   Driven entirely by layout.* values. Children read their own fields.
//   Layout does NOT switch structure — same Row, same Box.
//   Album grows via layout.albumArtSize, controls fade via layout.controlsOpacity,
//   wave appears via layout.waveVisibility, height grows via layout.height.
//
// ── MPRS Reference ─────────────────────────────────────────────────────────────────
//
//   Chapter B.10 — MiniPlayerLayoutSpec
//   Chapter D — Composition (layout, z-order, spacing, adaptive sizing)
//
// ═══════════════════════════════════════════════════════════════════════════════════════

/**
 * Root composable for Mini Player 2.0.
 *
 * THE ORCHESTRATOR — composes ALL child components in strict z-order.
 * Produces zero visual output. Pure structural composition.
 *
 * Handles:
 *   1. Visibility gate (AnimatedVisibility with appear/disappear specs)
 *   2. Gesture interception (outermost wrapper)
 *   3. Shadow + Surface depth hierarchy
 *   4. Background behind surface
 *   5. Content z-ordering (Wave → Progress → Album → Metadata → Controls)
 *   6. Collapsed row layout with correct spacing
 *
 * Does NOT handle:
 *   - Any visual rendering (delegates to children)
 *   - Any morph computation (delegates to layout.* values)
 *   - Any gesture processing (delegates to GestureLayer)
 *   - Any animation spec creation (delegates to MiniPlayerMotion)
 *
 * @param state Single source of truth for Mini Player UI.
 *              Children read their own sub-state fields (Rule #17).
 * @param onAction Callback for user-initiated actions (play, skip, expand, etc.).
 *                 Routed to GestureLayer and Controls.
 * @param modifier External modifier for positioning (margin, alignment from parent).
 */
@Composable
fun MiniPlayerRoot(
    state: MiniPlayerState,
    onAction: (MiniPlayerAction) -> Unit,
    modifier: Modifier = Modifier
) {
    // ═════════════════════════════════════════════════════════════════════
    // VISIBILITY GATE — AnimatedVisibility
    // Rule #19: enter/exit specs from MiniPlayerMotion
    // ═════════════════════════════════════════════════════════════════════
    AnimatedVisibility(
        visible = state.isVisible,
        enter = fadeIn(
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ) + slideInVertically(
            // Slide up from bottom of parent bounds
            initialOffsetY = { fullHeight -> fullHeight }
        ),
        exit = fadeOut(
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ) + slideOutVertically(
            // Slide down to bottom of parent bounds
            targetOffsetY = { fullHeight -> fullHeight }
        ),
        modifier = modifier
    ) {
        // ═════════════════════════════════════════════════════════════════════
        // Z-11, Z-12: GESTURE LAYER — Outermost wrapper
        // Invisible touch interception layer.
        // Catches ALL gestures before they reach child components.
        // Touch event flow: GestureLayer → Shadow → Surface → Content
        //
        // GestureLayer wraps the entire visual tree so no gesture is missed.
        // onEvent uses default empty lambda — ViewModel handles events via
        // a separate mechanism (reducer dispatch, not Layout forwarding).
        // ═════════════════════════════════════════════════════════════════════
        MiniPlayerGestureLayer(
            state = state,
            onAction = onAction
        ) {
            // ═════════════════════════════════════════════════════════════════════
            // Z-1, Z-2, Z-3: SHADOW — Depth layer wrapping Surface
            // Provides elevation shadow to the glass surface.
            // Corner radius from layout.cornerRadius (children never see morph).
            //
            // Shadow wraps the entire surface so its shadow appears behind both
            // Background and Surface content.
            // ═════════════════════════════════════════════════════════════════════
            MiniPlayerShadow(
                state = state,
                cornerRadius = with(LocalDensity.current) {
                    state.layout.cornerRadius.dp.coerceAtLeast(0.dp)
                }
            ) {
                // ═════════════════════════════════════════════════════════════════════
                // Z-0: BACKGROUND — Ambient atmosphere (behind surface)
                // matchParentSize fills the Shadow's Box, covering the full area.
                // Rendered BEFORE Surface so it appears behind the glass body.
                // ═════════════════════════════════════════════════════════════════════
                MiniPlayerBackground(
                    state = state,
                    modifier = Modifier.matchParentSize()
                )

                // ═════════════════════════════════════════════════════════════════════
                // Z-3: GLASS SURFACE — Container body
                // The physical glass material that holds all content.
                // Handles: corner radius, glass tint, dimensions, opacity.
                // Content lambda receives all child z-layers (Z-4 through Z-10).
                // ═════════════════════════════════════════════════════════════════════
                MiniPlayerSurface(
                    state = state
                ) {
                    // ── Internal Content Layout ───────────────────────────
                    // Composes child layers in strict z-order.
                    MiniPlayerContent(
                        state = state,
                        onAction = onAction
                    )
                }
            }
        }
    }
}

/**
 * Internal content layout organizer.
 *
 * Arranges child components inside the glass surface in strict z-order.
 * Handles the collapsed Row layout (Album → Metadata → Controls) with
 * correct spacing from MiniPlayerTokens.
 *
 * Z-order (composition order, back → front):
 *   Z-4:  Noise (handled internally by GlassSurface / Surface)
 *   Z-5:  MiniPlayerWave (audio-reactive ribbon)
 *   Z-6:  MiniPlayerAlbum (album art)
 *   Z-7:  Album reflection (handled internally by MiniPlayerAlbum)
 *   Z-8:  MiniPlayerMetadata (title + artist text)
 *   Z-9:  MiniPlayerProgress (waveform strip at bottom)
 *   Z-10: MiniPlayerControls (play/pause, skip)
 *
 * During morph expansion:
 *   Children read layout.* values directly. Layout does NOT switch structure.
 *   Album grows via layout.albumArtSize, controls fade via layout.controlsOpacity,
 *   wave appears via layout.waveVisibility, surface grows via layout.height.
 *
 * Rule #20 — No interpretation: this component only arranges. It does not
 * compute any visual values or make conditional layout decisions based on state.
 *
 * @param state Single source of truth.
 * @param onAction Action callback routed to controls.
 */
@Composable
private fun MiniPlayerContent(
    state: MiniPlayerState,
    onAction: (MiniPlayerAction) -> Unit
) {
    // ═════════════════════════════════════════════════════════════════════
    // LAYOUT SNAPSHOT — Read once (Rule #17)
    // All children receive the full state and read their own fields.
    // Layout only reads what it needs for its own structural decisions.
    // ═════════════════════════════════════════════════════════════════════
    val layout = state.layout
    val density = LocalDensity.current

    // ═════════════════════════════════════════════════════════════════════
    // ANIMATED ALBUM SIZE — For the collapsed Row sizing
    // Rule #19: Animation spec from MiniPlayerMotion
    // Rule #20: Just passes layout value through animation. No interpretation.
    // ═════════════════════════════════════════════════════════════════════
    val animatedAlbumSize by animateDpAsState(
        targetValue = with(density) {
            layout.albumArtSize.dp.coerceAtLeast(0.dp)
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "mp_layout_album_art_size"
    )

    // ═════════════════════════════════════════════════════════════════════
    // CONTENT BOX — Full-width container inside surface
    // Padding: horizontal from ContentPaddingHorizontal token
    // ═════════════════════════════════════════════════════════════════════
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MiniPlayerTokens.ContentPaddingHorizontal)
    ) {
        // ═════════════════════════════════════════════════════════════════════
        // Z-5: WAVE RIBBON — Audio-reactive visual
        // Composed first in Box → rendered behind content.
        // Full-width, fills available space inside padding.
        // Wave component handles its own visibility via layout.waveVisibility.
        // ═════════════════════════════════════════════════════════════════════
        MiniPlayerWave(
            state = state,
            modifier = Modifier.fillMaxWidth()
        )

        // ═════════════════════════════════════════════════════════════════════
        // Z-9: PROGRESS STRIP — Waveform at bottom
        // Anchored to bottom of surface with ProgressBottomPadding.
        // Composed before the Row so it renders behind controls (Z-10 > Z-9).
        // ═════════════════════════════════════════════════════════════════════
        MiniPlayerProgress(
            state = state,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = MiniPlayerTokens.ProgressBottomPadding)
        )

        // ═════════════════════════════════════════════════════════════════════
        // Z-6, Z-7, Z-8, Z-10: MAIN CONTENT ROW
        //
        // Collapsed arrangement:
        //   [Album 52dp] --12dp-- [Metadata (weight)] --8dp-- [Controls]
        //
        // Vertical centering keeps the content row centered within the surface.
        // Album size is animated via layout.albumArtSize (Rule #19).
        // Controls opacity is read by Controls via layout.controlsOpacity (Rule #18).
        // Metadata takes remaining space (weight 1f).
        //
        // During morph: same Row structure. Children adapt via layout.* values.
        // Layout does NOT switch between Row and Column (Rule #20).
        // ═════════════════════════════════════════════════════════════════════
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Z-6, Z-7: Album Art (+ Reflection handled internally) ──
            // Size driven by layout.albumArtSize: 52dp collapsed → 180dp expanded.
            // Reflection alpha driven by layout.reflectionAlpha (read inside Album).
            MiniPlayerAlbum(
                state = state,
                modifier = Modifier.size(animatedAlbumSize)
            )

            // ── Gap: Album → Metadata ──────────────────────────────────
            // Spacing from MiniPlayerTokens.AlbumMetadataGap (12dp).
            Spacer(modifier = Modifier.width(MiniPlayerTokens.AlbumMetadataGap))

            // ── Z-8: Metadata (Title + Artist) ────────────────────────
            // Takes remaining horizontal space. Text truncates with ellipsis.
            // Text color adapts to dark/light artwork (read inside Metadata).
            // Controls opacity affects metadata text alpha (read inside Metadata).
            MiniPlayerMetadata(
                state = state,
                modifier = Modifier.weight(1f)
            )

            // ── Gap: Metadata → Controls ────────────────────────────────
            // Spacing from MiniPlayerTokens.MetadataControlsGap (8dp).
            Spacer(modifier = Modifier.width(MiniPlayerTokens.MetadataControlsGap))

            // ── Z-10: Transport Controls ──────────────────────────────
            // PlayPause (38dp) + SkipNext (38dp) with ControlsGap (8dp).
            // Opacity driven by layout.controlsOpacity: 1.0 → 0.0 during FADE phase.
            // Actions routed to parent via onAction callback.
            MiniPlayerControls(
                state = state,
                onAction = onAction
            )
        }
    }
}
