package com.rikodev.music.android.presentation.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.request.ImageRequest
import com.rikodev.music.android.core.designsystem.riko.components.RikoArtwork
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMotion
import com.rikodev.music.android.core.designsystem.glass.colorGlow
import com.rikodev.music.android.core.designsystem.theme.ColorObjectMat
import com.rikodev.music.android.core.designsystem.riko.theme.RikoShape
import com.rikodev.music.android.core.experience.ColorState
import com.rikodev.music.android.core.experience.wave.WaveState
import kotlinx.coroutines.flow.StateFlow

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — HERO ALBUM ART
//
// Album art is NOT just an image — it's a PHYSICAL OBJECT.
//
// This composable renders the hero album art for the Now Playing screen with
// seven layered effects that make the artwork feel tangible, alive, and
// reactive to the music:
//
//   1. STILLNESS — the art is a fixed object. No float, no tilt,
//                  no beat pulse. Professional players keep hero art static.
//   2. GLASS   — diagonal specular reflection overlay
//   3. SHADOW  — energy-reactive elevation + color-tinted shadow
//   4. GLOW    — ambient halo behind the mat, pulses with energy
//   5. STATE   — play/pause reactivity (glow/glass dim when paused)
//   6. RITUAL  — crossfade + settle on song change (AnimatedContent)
//
// Layer stack (back → front):
//
//   Box (outer container)
//   ├── ColorGlow (ambient halo, energy-reactive)
//   └── Box (the mat — shadow + ColorObjectMat backing)
//       └── Box (the art — static image + glass sheen)
//           ├── AsyncImage (album art from Coil)
//           └── Glass Reflection Overlay (diagonal gradient)
//
// All motion uses RikoMotion tokens. All colors from ColorState.
// All dimensions from RikoDimens. Zero hardcoded anything.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Hero Album Art — the centerpiece of the Now Playing screen.
 *
 * Receives [ColorState] and [WaveState] as reactive StateFlows, collecting
 * their latest values to drive every visual property: shadow elevation,
 * glow intensity, ambient color, beat ripple, and play/pause behavior.
 *
 * @param colorState    Reactive color palette — primary, glow, shadow, energy.
 * @param waveState     Reactive wave state — beat detection, energy, activity.
 * @param albumArtModel Optional Coil ImageRequest.Builder for the current song's artwork.
 * @param contentDescription Accessibility label for the album art image.
 * @param modifier      External modifier for sizing / positioning.
 */
@Composable
fun HeroAlbum(
    colorState: StateFlow<ColorState>,
    waveState: StateFlow<WaveState>,
    albumArtModel: ImageRequest.Builder?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    // ── Collect reactive state from flows ─────────────────────────────────
    // Every visual property derives from these two sources of truth.
    val colors by colorState.collectAsStateWithLifecycle()
    val wave by waveState.collectAsStateWithLifecycle()

    val density = LocalDensity.current

    // ── Play/pause state modulator ───────────────────────────────────────
    // When paused: glow dims, glass sheen settles. The art itself never moves.
    val isPlaying = wave.isActive
    val playFactor by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = RikoMotion.SoftSpring(),
        label = "playFactor"
    )

    // ── Energy-reactive shadow elevation ────────────────────────────────
    // 20dp at idle, up to 32dp when energy is high.
    // Smooth spring ensures the shadow doesn't snap.
    // Plan 4.1: motion signal = Wave Engine bass pressure (smooth attack/
    // release, never the quantized staircase), non-bouncy settle.
    val waveEnergy = wave.energy
    val shadowEnergy = waveEnergy.bassPressure.coerceIn(0f, 1f)
    val glowEnergy = waveEnergy.overallEnergy.coerceIn(0f, 1f)
    val shadowElevation by animateFloatAsState(
        targetValue = 20f + shadowEnergy * 12f,
        animationSpec = RikoMotion.ambientBreathing(),
        label = "shadowElevation"
    )
    val shadowElevationDp: Dp = remember(shadowElevation, density) {
        with(density) { shadowElevation.dp }
    }

    // ── Animated shadow colors ───────────────────────────────────────────
    // Ambient: warm dark tinted by ColorState.shadowColor
    // Spot:    accent-colored spot shadow for chromatic grounding
    val shadowAmbient by animateColorAsState(
        targetValue = colors.shadowColor,
        animationSpec = RikoMotion.colorTransition(),
        label = "shadowAmbient"
    )
    val shadowSpot by animateColorAsState(
        targetValue = colors.primary.copy(alpha = 0.45f),
        animationSpec = RikoMotion.colorTransition(),
        label = "shadowSpot"
    )

    // ── Ambient glow parameters ─────────────────────────────────────────
    // Glow radius and alpha modulated by wave energy (plan 4.1 fast state).
    // Idle: subtle halo. Active: vivid, expansive bloom behind the mat.
    val glowRadius by animateFloatAsState(
        targetValue = 24f + glowEnergy * 28f,
        animationSpec = RikoMotion.ambientBreathing(),
        label = "glowRadius"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = 0.20f + glowEnergy * 0.30f,
        animationSpec = RikoMotion.ambientBreathing(),
        label = "glowAlpha"
    )
    val glowColor by animateColorAsState(
        targetValue = colors.ambientGlow,
        animationSpec = RikoMotion.colorTransition(),
        label = "glowColor"
    )

    // ── Glass reflection alpha — dims when paused ────────────────────────
    // 0.15 at full play, fades toward 0.05 when paused.
    val glassAlpha by animateFloatAsState(
        targetValue = 0.05f + playFactor * 0.10f,
        animationSpec = RikoMotion.colorTransition(),
        label = "glassAlpha"
    )

    // ═════════════════════════════════════════════════════════════════════
    // RENDER STACK
    // ═════════════════════════════════════════════════════════════════════

    // Static container — no float offset. The art sits still like a record sleeve.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {

        // ── Layer 0: Ambient Glow ──────────────────────────────────────
        // Outer halo behind the mat. Uses the colorGlow pattern from
        // GlassSurface — draws a shadow-layer circle centered on the
        // album art. Reacts to energy: higher energy = bigger, brighter glow.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .colorGlow(
                    color = glowColor,
                    radius = with(density) { glowRadius.dp },
                    alpha = glowAlpha * playFactor
                )
        )

        // ── Layer 1: The Mat — shadow + backing card ────────────────────
        // The warm ColorObjectMat sits behind the art like a passe-partout
        // mount. Shadow elevation is energy-reactive. This grounds the
        // album art against the dark background with chromatic depth.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(
                    elevation = shadowElevationDp,
                    shape = RikoShape.CardLarge,
                    ambientColor = shadowAmbient,
                    spotColor = shadowSpot
                )
                .clip(RikoShape.CardLarge)
                .drawWithContent {
                    drawRect(ColorObjectMat)
                    drawContent()
                }
        )

        // ── Layer 2: The Art — static ────────────────────────────────────
        // The actual album art image, inset at 90% of the mat (matching
        // the TactileAlbumArt fill ratio). No tilt, no float, no pulse —
        // stillness is what makes it feel expensive.
        Box(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .aspectRatio(1f)
                .clip(RikoShape.Card),
            contentAlignment = Alignment.Center
        ) {
            // ── Album Art Image ────────────────────────────────────────
            // Loads from Coil via the provided ImageRequest.Builder.
            // Crossfades on song change for the Song-Change Ritual.
            // If no model provided, draws a radial gradient placeholder.
            // PERF FORENSIC P1 (RC-10): Builder has no stable equals → every
            // recomposition restarted the 400ms crossfade infinitely. Key on
            // stable artwork URI string instead; image request built once per song.
            val artworkKey = remember(albumArtModel) { albumArtModel?.build()?.data.toString() }
            val heroRequest = remember(albumArtModel) { albumArtModel?.build() }
            AnimatedContent(
                targetState = artworkKey,
                transitionSpec = {
                    fadeIn(RikoMotion.screen()) + scaleIn(
                        initialScale = 0.95f,
                        animationSpec = RikoMotion.screen()
                    ) togetherWith fadeOut(RikoMotion.normal())
                },
                label = "hero_artwork_ritual"
            ) { key ->
                if (key != "null" && heroRequest != null) {
                    RikoArtwork(
                        model = heroRequest,
                        contentDescription = contentDescription,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .drawWithContent {
                                drawRect(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            colors.primary.copy(alpha = 0.25f),
                                            ColorObjectMat
                                        )
                                    )
                                )
                            }
                    )
                }
            }

            // ── Glass Reflection Overlay ────────────────────────────────
            // A diagonal band of white-to-transparent gradient, anchored at
            // top-left, sweeping across the art surface. Very low alpha
            // (0.05–0.15) — reads as a glossy glass sheen, not a visible
            // overlay. Fades further when paused via glassAlpha modulation.
            //
            // The gradient goes from top-left (0.0, 0.0) to bottom-right
            // (1.0, 1.0), with a narrow transparent-white-transparent
            // band in the middle — mimicking how real glass catches
            // directional light at an angle.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.White.copy(alpha = glassAlpha),
                                    Color.Transparent,
                                    Color.Transparent
                                ),
                                start = Offset.Zero,
                                end = Offset(size.width, size.height)
                            )
                        )
                    }
            )
        }
    }
}

