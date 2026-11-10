/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.3 / Phase B — Identity Reveal
 *
 * File B7 / B9 — LibraryDock.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [LibraryDock] is the *persistent anchor* at the bottom of the
 * Library screen.
 *
 *   Milestone 1-এর reward এখানেই — Mini Player integration.
 *
 * The Dock tells the user "music is here, always". It is the last
 * slot in the story (Rule #54 — Screen Tells a Story). When collapsed,
 * it shows what's playing. When tapped, it morphs into the Now Playing
 * screen.
 *
 * The Dock is NOT the existing MiniPlayer. The MiniPlayer is a
 * full-featured player preview with its own state model
 * (MiniPlayerState) tightly coupled to the Experience OS. The Dock is
 * a Library-level *anchor* — it reads LibraryDockRuntimeState (B5.2)
 * and renders a quiet, persistent presence at the screen bottom.
 *
 *   MiniPlayer → full player preview (own state, own complexity)
 *   LibraryDock → Library anchor (B5.2 state, quiet, stable)
 *
 * The two share visual vocabulary (glass, depth) but are distinct
 * components with distinct contracts. Phase D will decide whether
 * the Library forwards taps to the MiniPlayer or to the Now Playing
 * screen — that's the host's call, not the Dock's.
 *
 * WHAT THIS FILE IS
 * -----------------
 * A composable that renders the collapsed Dock from a
 * [LibraryDockContract] + [LibraryDockRuntimeState]. The Dock has:
 *
 *   1. Artwork (small, 48dp square, left)
 *   2. Title + artist (middle, single line each)
 *   3. Play/pause + next controls (right, 32dp each)
 *   4. Progress bar (1dp, very subtle, bottom edge)
 *
 * The Dock knows nothing about:
 *
 *   - How it morphs to Now Playing (DockMotion B8 handles that)
 *   - What happens on tap (callback to Coordinator)
 *   - How it hides on scroll (Phase C motion handles that)
 *
 * WHAT THIS FILE IS NOT
 * ---------------------
 * ❌ Not the existing MiniPlayer. Distinct contract, distinct state.
 * ❌ Not a queue. Queue peek is a future expanded-state feature.
 * ❌ Not a state owner. Reads dock state, never writes.
 *
 * ARCHITECTURE
 * ------------
 *
 *   LibraryScreen root (Phase D)
 *        ↓ contains
 *   LibraryDock (B7 — THIS FILE)
 *        ↓ wraps
 *   LibraryGlassSurface (DOCK token, Phase A) + LibraryDepth.FRONT
 *        ↓ inside
 *   Artwork + Title/Artist + Controls + Progress
 *
 * RULES ENFORCED HERE
 * -------------------
 * #58  Runtime Never Knows Rendering   — this is rendering
 * #62  Never Rebuild Existing Systems  — wraps GlassSurface (Phase A)
 * #67  Dock Never Interrupts           — quiet, stable, no pulsing
 *
 * © RIKO Experience OS — Rendering Tier (Phase B)
 */

package com.rikodev.music.android.presentation.library.screen.identity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.music.android.core.designsystem.theme.AccentCyan
import com.rikodev.music.android.presentation.library.screen.background.LibraryDepth
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.rikodev.music.android.presentation.library.screen.background.LibraryGlassSurface
import com.rikodev.music.android.presentation.library.screen.background.LibraryGlassToken
import com.rikodev.music.android.presentation.library.screen.background.libraryDepth
import com.rikodev.music.android.presentation.library.screen.background.resolveDockToken
import com.rikodev.music.android.presentation.library.screen.background.toColorOrNull
import com.rikodev.music.android.presentation.library.screen.contract.LibraryConstitutionProfile
import com.rikodev.music.android.presentation.library.screen.contract.LibraryDockContract
import com.rikodev.music.android.presentation.library.screen.contract.LibraryDockPosition
import com.rikodev.music.android.presentation.library.screen.runtime.LibraryDockRuntimeState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// DOCK RESOLVED SPEC
//
// Aggregates the resolved values the Dock needs to render. Resolved
// once per (contract, profile, state) triple.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The fully-resolved LibraryDock spec.
 *
 * @property glassToken       Glass token (DOCK or AMBIENT for AMOLED).
 * @property cornerRadiusDp   Corner radius.
 * @property floatingMarginDp Floating margin (0 for EDGE_ATTACHED).
 * @property heightDp         Dock height in dp (collapsed).
 * @property artworkSizeDp    Artwork size in dp.
 * @property controlSizeDp    Control icon size in dp.
 * @property progressHeightDp Progress bar height in dp.
 * @property progressColor    Progress bar color (track accent).
 * @property trackBgColor     Progress track background color.
 * @property isVisible        True if the Dock should render at all.
 */
internal data class LibraryDockSpec(
    val glassToken: LibraryGlassToken,
    val cornerRadiusDp: androidx.compose.ui.unit.Dp,
    val floatingMarginDp: Int,
    val heightDp: Int,
    val artworkSizeDp: Int,
    val controlSizeDp: Int,
    val progressHeightDp: Int,
    val progressColor: Color,
    val trackBgColor: Color,
    val isVisible: Boolean,
) {
    companion object {

        /**
         * Resolves a [LibraryDockContract] + [LibraryConstitutionProfile]
         * + [LibraryDockRuntimeState] into a [LibraryDockSpec].
         *
         * Resolution rules:
         *
         *   - Glass token: profile.resolveDockToken() (AMOLED → AMBIENT,
         *     else → DOCK).
         *   - Corner radius: from contract (0 for EDGE_ATTACHED, 20 otherwise).
         *   - Floating margin: from contract.position.floatingMarginDp.
         *   - Height: 64dp collapsed (matches LibraryScreenSlots.dockHeightDp).
         *   - Artwork size: 48dp (square, fits in 64dp height with padding).
         *   - Control size: 32dp (compact, tappable).
         *   - Progress: 2dp height, accent color.
         *   - Visible: contract.isVisible AND state.isVisible.
         */
        fun from(
            contract: LibraryDockContract,
            profile: LibraryConstitutionProfile,
            state: LibraryDockRuntimeState,
        ): LibraryDockSpec {
            val resolvedRadius = if (contract.position == LibraryDockPosition.EDGE_ATTACHED) {
                0
            } else {
                contract.cornerRadiusDp
            }

            val accent = state.accentColorHex.toColorOrNull() ?: AccentCyan

            return LibraryDockSpec(
                glassToken = profile.resolveDockToken(),
                cornerRadiusDp = resolvedRadius.dp,
                floatingMarginDp = contract.position.floatingMarginDp,
                heightDp = 64,
                artworkSizeDp = 48,
                controlSizeDp = 32,
                progressHeightDp = 2,
                progressColor = accent.copy(alpha = 0.85f),
                trackBgColor = Color.White.copy(alpha = 0.08f),
                isVisible = contract.isVisible && state.isVisible,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// THE LIBRARY DOCK COMPOSABLE
//
// Public API. Renders the collapsed Dock at the screen bottom.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * THE LIBRARY DOCK.
 *
 * Renders the persistent mini-player dock at the bottom of the Library
 * screen. The Dock is the screen's anchor — quiet, stable, always
 * present when music is loaded.
 *
 * ## Rule #67 — Dock Never Interrupts
 *
 * - The Dock does NOT pulse, flash, or animate on its own. Track-change
 *   feedback (a subtle accent flash) is handled by [DockMotion] (B8),
 *   not by this file.
 * - The progress bar is 2dp tall — barely visible, but present.
 * - The Dock's glass tint is capped at 0.12 alpha (Rule #67) — quiet,
 *   background-aware.
 * - Play/pause + next are the only interactive elements; tapping them
 *   does NOT trigger a Dock-wide flash.
 *
 * ## Visibility
 *
 * If `contract.isVisible == false` OR `state.isVisible == false`, the
 * Dock renders nothing. This allows the host to fully remove the Dock
 * from the layout (e.g. when no music is loaded, or in fullscreen mode).
 *
 * ## Performance
 *
 * - The spec is `remember`ed per (contract, profile, state) triple.
 * - The progress bar uses `Modifier.drawBehind` — no Box overhead.
 * - The artwork slot is a Compose slot — caller controls allocation.
 *
 * @param contract      Dock contract from B5.1.
 * @param state         Dock runtime state from B5.2.
 * @param profile       Active constitution profile (drives glass token).
 * @param artwork       Slot for the artwork content. Caller decides what
 *                      to draw (album art, gradient placeholder).
 * @param playPauseIcon Icon for play/pause button.
 * @param nextIcon      Icon for next button.
 * @param onDockTap     Callback fired when the Dock body is tapped
 *                      (typically morphs to Now Playing).
 * @param onPlayPauseTap  Callback fired when play/pause is tapped.
 * @param onNextTap     Callback fired when next is tapped.
 * @param modifier      Compose modifier.
 */
@Composable
fun LibraryDock(
    contract: LibraryDockContract,
    state: LibraryDockRuntimeState,
    profile: LibraryConstitutionProfile,
    artwork: @Composable BoxScope.() -> Unit,
    playPauseIcon: ImageVector,
    nextIcon: ImageVector,
    onDockTap: (() -> Unit)? = null,
    onPlayPauseTap: (() -> Unit)? = null,
    onNextTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val spec = remember(contract, profile, state) {
        LibraryDockSpec.from(contract, profile, state)
    }

    if (!spec.isVisible) return

    val dockPressSource = remember { MutableInteractionSource() }
    val isDockPressed by dockPressSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spec.floatingMarginDp.dp),
    ) {
        LibraryGlassSurface(
            token = spec.glassToken,
            modifier = Modifier
                .fillMaxWidth()
                .height(spec.heightDp.dp)
                .clip(RoundedCornerShape(spec.cornerRadiusDp))
                .libraryDepth(
                    depth = LibraryDepth.FRONT,
                    cornerRadius = spec.cornerRadiusDp,
                )
                .then(
                    if (onDockTap != null) {
                        Modifier.clickable(
                            interactionSource = dockPressSource,
                            indication = null,
                            onClick = onDockTap,
                        )
                    } else Modifier,
                ),
            cornerRadius = spec.cornerRadiusDp,
            enableShadow = true, // Dock always has shadow — it floats.
            content = {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Subtle press dim — only on the body, not on controls.
                    if (isDockPressed) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.10f)),
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // ── Artwork ───────────────────────────────────────────
                        Box(
                            modifier = Modifier
                                .size(spec.artworkSizeDp.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            content = artwork,
                        )

                        // ── Title + Artist ───────────────────────────────────
                        Column(
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = state.trackTitle,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = state.trackArtist,
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        // ── Play/Pause + Next ─────────────────────────────────
                        DockControlButton(
                            icon = playPauseIcon,
                            size = spec.controlSizeDp,
                            onTap = onPlayPauseTap,
                        )

                        DockControlButton(
                            icon = nextIcon,
                            size = spec.controlSizeDp,
                            onTap = onNextTap,
                        )
                    }

                    // ── Progress bar (bottom edge) ─────────────────────────
                    DockProgressBar(
                        progress = state.progressFraction,
                        color = spec.progressColor,
                        trackColor = spec.trackBgColor,
                        heightDp = spec.progressHeightDp,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DOCK CONTROL BUTTON
//
// A circular tappable icon button. Internal — only used by [LibraryDock].
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A circular control button for the Dock.
 *
 * No background fill — just the icon. Tappable area is the full
 * `size × size` square (≥ 32dp, well above the 48dp minimum when
 * combined with adjacent controls).
 *
 * ## Rule #67 — Dock Never Interrupts
 *
 * The button has NO press-state visual feedback of its own — pressing
 * it just fires the callback. The visual feedback (subtle dim) is
 * handled at the Dock body level, so the entire Dock feels like one
 * surface, not a collection of buttons.
 *
 * @param icon     Icon to display.
 * @param size     Button size in dp.
 * @param onTap    Tap callback.
 */
@Composable
private fun DockControlButton(
    icon: ImageVector,
    size: Int,
    onTap: (() -> Unit)? = null,
) {
    val pressSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .then(
                if (onTap != null) {
                    Modifier.clickable(
                        interactionSource = pressSource,
                        indication = null,
                        onClick = onTap,
                    )
                } else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.90f),
            modifier = Modifier.size((size * 0.75f).dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DOCK PROGRESS BAR
//
// A very subtle progress bar at the bottom edge of the Dock. Shows
// playback progress without ever demanding attention.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A subtle progress bar for the Dock.
 *
 * Drawn via `Modifier.drawBehind` — no layout overhead. The bar is
 * 2dp tall, sits at the very bottom edge of the Dock, and shows:
 *
 *   - Background track (8% white) — full width
 *   - Progress fill (accent color, 85% alpha) — proportional width
 *
 * ## Rule #67 — Dock Never Interrupts
 *
 * The progress bar is NOT animated frame-by-frame. It updates only
 * when `progress` changes (which is once per second from the runtime
 * state). Continuous animation would draw the eye; discrete updates
 * are quiet.
 *
 * @param progress   0.0–1.0. Playback progress.
 * @param color      Progress fill color.
 * @param trackColor Background track color.
 * @param heightDp   Bar height in dp.
 * @param modifier   Compose modifier.
 */
@Composable
private fun DockProgressBar(
    progress: Float,
    color: Color,
    trackColor: Color,
    heightDp: Int,
    modifier: Modifier = Modifier,
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val barHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { heightDp.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .drawBehind {
                // Track background — full width.
                drawRect(
                    color = trackColor,
                    topLeft = Offset(0f, size.height - barHeightPx),
                    size = Size(size.width, barHeightPx),
                )
                // Progress fill — proportional width.
                drawRect(
                    color = color,
                    topLeft = Offset(0f, size.height - barHeightPx),
                    size = Size(size.width * clampedProgress, barHeightPx),
                )
            },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// DOCK ARTWORK PLACEHOLDER
//
// A flat-color artwork placeholder for previews / loading.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A placeholder artwork slot — paints a flat accent-tinted background.
 *
 * @param accentColorHex  Accent color (hex).
 * @param modifier        Compose modifier.
 */
@Composable
fun LibraryDockArtworkPlaceholder(
    accentColorHex: String = "#1A1A2E",
    modifier: Modifier = Modifier,
) {
    val color = remember(accentColorHex) {
        accentColorHex.toColorOrNull() ?: AccentCyan
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// DOCK PREVIEW HELPERS
//
// Plain composables that render the Dock with sample state. Useful
// as @Preview content or as debug overlays.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Renders the default Library Dock with sample state.
 *
 * Note: caller must provide `playPauseIcon` and `nextIcon` from
 * Material Icons. This preview uses empty slots — wire up real
 * icons in your preview host.
 */
@Composable
fun LibraryDockPreviewDefault(
    playPauseIcon: ImageVector,
    nextIcon: ImageVector,
    modifier: Modifier = Modifier,
) {
    LibraryDock(
        contract = LibraryDockContract.DEFAULT,
        state = LibraryDockRuntimeState(
            state = com.rikodev.music.android.presentation.library.screen.contract.LibraryDockState.COLLAPSED,
            currentTrackId = "track.bohemian_rhapsody",
            trackTitle = "Bohemian Rhapsody",
            trackArtist = "Queen",
            isPlaying = true,
            progressFraction = 0.42f,
            artworkKey = "artwork.bohemian_rhapsody",
            accentColorHex = "#FF8A65",
        ),
        profile = LibraryConstitutionProfile.DEFAULT,
        artwork = { LibraryDockArtworkPlaceholder(accentColorHex = "#3E2723") },
        playPauseIcon = playPauseIcon,
        nextIcon = nextIcon,
        onDockTap = { /* preview: no-op */ },
        onPlayPauseTap = { /* preview: no-op */ },
        onNextTap = { /* preview: no-op */ },
        modifier = modifier,
    )
}

/**
 * Renders the AMOLED Library Dock — pure dark, no breathing.
 */
@Composable
fun LibraryDockPreviewAmoled(
    playPauseIcon: ImageVector,
    nextIcon: ImageVector,
    modifier: Modifier = Modifier,
) {
    LibraryDock(
        contract = LibraryDockContract.AMOLED,
        state = LibraryDockRuntimeState(
            state = com.rikodev.music.android.presentation.library.screen.contract.LibraryDockState.COLLAPSED,
            currentTrackId = "track.bohemian_rhapsody",
            trackTitle = "Bohemian Rhapsody",
            trackArtist = "Queen",
            isPlaying = true,
            progressFraction = 0.42f,
            artworkKey = "artwork.bohemian_rhapsody",
            accentColorHex = "#1A1A2E",
        ),
        profile = LibraryConstitutionProfile.AMOLED,
        artwork = { LibraryDockArtworkPlaceholder(accentColorHex = "#000000") },
        playPauseIcon = playPauseIcon,
        nextIcon = nextIcon,
        modifier = modifier,
    )
}
