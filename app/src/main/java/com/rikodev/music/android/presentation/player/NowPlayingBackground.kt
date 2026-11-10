package com.rikodev.music.android.presentation.player

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import coil.request.ImageRequest
import com.rikodev.music.android.core.designsystem.riko.components.RikoArtwork
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMotion
import com.rikodev.music.android.core.designsystem.theme.ColorAbyssBlack
import com.rikodev.music.android.core.designsystem.theme.AuroraViolet
import com.rikodev.music.android.core.designsystem.theme.AuroraVioletDeep
import com.rikodev.music.android.core.experience.ColorState
import com.rikodev.music.android.presentation.player.aurora.AuroraFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — NOW PLAYING DYNAMIC BACKGROUND ENGINE
//
// Album art is not just an image — it IS the environment.
// The background should feel alive, breathing, and reactive to audio energy.
//
// Visual Stack (bottom → top):
//   Layer 1: Album Art Blur (Android S+ only, 1.4x scale, 256px prescale, 30dp blur)
//   Layer 2–5: Single-canvas ambient stack (gradient / tint / glow / fade) —
//              one draw pass instead of four (PERF PASS P-2)
//   Layer 2: Ambient Gradient (slow-drifting radial gradients from ColorState)
//   Layer 3: Glass Tint (subtle overlay modulated by tintStrength)
//   Layer 4: Ambient Glow Pulse (center-origin radial glow with pulseAmount)
//   Layer 5: Bottom Gradient Fade (dark gradient for text readability)
//
// Data Flow:
//   Experience Hub → AudioState → Color Engine → Wave Engine →
//   WaveState → NowPlayingScreen → NowPlayingBackground → Render Only
//
// Architecture Rules:
//   ❌ No hardcoded colors — all from ColorState or theme tokens
//   ❌ No hardcoded animation — all from RikoMotion
//   ❌ No direct AudioState reads — only StateFlows from ViewModel
//   ❌ No FFT in UI
//   ❌ No physics in Compose
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Full-screen dynamic background for the Now Playing screen.
 *
 * Five composable layers stacked via Box, each driven by [ColorState]
 * values collected from the provided [StateFlow]. The result is a living,
 * breathing environment that reacts to audio energy and playback state.
 *
 * Aurora state model:
 *   Playing  → vitality (full drift, pulse, energy)
 *   Paused   → suspension (drift slows, pulse fades, glow settles)
 *   Buffering → held motion (drift pauses, pulse holds)
 *   Idle     → quiet (everything calms to dark ambient)
 *
 * @param colorState Flow of [ColorState] — the single source of truth for
 *   all color intelligence. Collected via `collectAsStateWithLifecycle()` inside.
 * @param artworkData Optional resolved artwork (embedded Bitmap or album-art URI).
 *   Must come from [resolveArtworkData] — never the raw colliding MediaStore URI.
 * @param isPlaying Whether playback is actively progressing.
 * @param modifier Optional [Modifier] applied to the root container.
 */
@Composable
fun NowPlayingBackground(
    colorState: StateFlow<ColorState>,
    waveState: StateFlow<com.rikodev.music.android.core.experience.wave.WaveState>,
    artworkData: Any?,
    isPlaying: Boolean = true,
    isVisible: () -> Boolean = { true },
    modifier: Modifier = Modifier
) {
    // Plan 4.2: the ambient stack lives in AuroraFlow (One-UI-9-category
    // waves � AGSL on API 33+, canvas fallback below, same parameter
    // contract). Layer 1 (artwork blur) stays as the atmosphere source.

    Box(modifier = modifier.fillMaxSize()) {

        // ═══════════════════════════════════════════════════════════════════
        // LAYER 1 — ALBUM ART BLUR
        // ═══════════════════════════════════════════════════════════════════
        // On Android S+ (API 31): real album art, scaled 1.4x, heavily
        // blurred, low alpha 0.5f. The album art BECOMES the atmosphere —
        // not just a thumbnail, but the entire environment.
        // On older devices: this layer is simply skipped; the gradient
        // layers below provide the full visual experience.
        //
        // PERF PASS (P-1, audit): the backdrop used to be the heaviest
        // single render in the app — FULL-RESOLUTION artwork (500–1000px+)
        // decoded and blurred at 65dp on a full-screen layer. Two changes,
        // visually near-identical at 0.5f alpha:
        //   1. Decode at 256px (PERF prescale) — the RenderEffect blur runs
        //      on a small bitmap at a fraction of the GPU cost, and upscale
        //      through the 1.4x graphicsLayer keeps the backdrop soft.
        //   2. 65dp → 30dp radius. At 30dp on a low-alpha ambient layer the
        //      artwork is already fully abstracted (audit P-1: "20–30dp").
        // PERF FORENSIC P0 (RC-07): isolate blur raster from color ticks.
        // key(uri) → energy/pulse emissions recompose Canvas below, NOT this RenderEffect.
        key(artworkData) {
            val ambientContext = LocalContext.current
            if (artworkData != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // P0 FIX (album-art mix-up): [artworkData] is per-song resolved
                // artwork (embedded-first). Cache key is derived from the data's
                // identity + song isolation via ArtworkRequestFactory's pipeline.
                val ambientRequest = remember(artworkData) {
                    ImageRequest.Builder(ambientContext)
                        .data(artworkData)
                        .size(256) // PERF: ambient backdrop prescale — never full-res
                        .memoryCacheKey("ambient256_v2_${artworkData.hashCode()}")
                        .build()
                }
                RikoArtwork(
                    model = ambientRequest,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Scale up so the blur doesn't show edges
                            scaleX = 1.4f
                            scaleY = 1.4f
                        }
                        .blur(30.dp)
                        .alpha(0.5f)
                )
            }
        }

        // Plan 4.2: AuroraFlow � fluid + colourful + premium + musical.
        // Artwork = identity, Music = motion.
        AuroraFlow(
            colorState = colorState,
            waveState = waveState,
            isPlaying = isPlaying,
            isVisible = isVisible,
            modifier = Modifier.fillMaxSize()
        )
    }
}
