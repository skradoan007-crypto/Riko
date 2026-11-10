package com.rikodev.music.android.presentation.miniplayer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.rikodev.music.android.core.designsystem.glass.GlassSurface
import com.rikodev.music.android.core.designsystem.theme.GlassBorderLight
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring

// ═══════════════════════════════════════════════════════════════════════════════════════
// RIKO MUSIC — MINI PLAYER 2.0: SURFACE
//
// Build-3, Phase A, File 2.
//
// Glass container body. The physical form of the Mini Experience Surface.
// Handles: Corner Radius, Glass Thickness, Transparency, Diffusion.
// NO shadow (that's MiniPlayerShadow's job — Rule #24).
// NO album. NO wave. NO controls.
//
// Reads: state.color, state.layout, state.morph (from MiniPlayerState)
// Forbidden: Album, Wave, Controls, Metadata, Progress, Shadow rendering
// Pixel Owner: Glass Frost, Noise Texture (Rule #24)
//
// Morph Behavior:
//   As morphProgress increases (via layout.* values):
//   - Corner radius: 28dp → 0dp (pill → rectangle)
//   - Height: 80dp → 240dp
//   - Width: 94% → 100%
//   - Glass tint alpha: 0.10 → 0.0 (glass fades to transparent)
//   - Shadow elevation: 8dp → 24dp (LIFTS, but Shadow component owns the rendering)
//   - Opacity: 1.0 → 0.0 (crossfades to NowPlaying)
//
// Rule #18 — Components never know morph progress source.
//   They see: layout.cornerRadius, layout.height, layout.glassTintAlpha
//
// MPRS Reference: Chapter A.2, B.2
// Tokens Reference: MiniPlayerTokens (Sections 2-5)
// ═══════════════════════════════════════════════════════════════════════════════════════

/**
 * Glass surface container for the Mini Experience Surface.
 *
 * This is the physical body — the glass material that holds all content.
 * It configures GlassSurface parameters from layout.* values.
 * Zero computation — all values pre-computed by MiniPlayerMapper.
 *
 * Rule #24 — Pixel Ownership: This component OWNS glass frost and noise pixels.
 * Shadow component owns shadow pixels. Background owns glow pixels.
 * No pixel overlap between components.
 *
 * Rule #20 — Renderer Never Interprets: Just applies layout values.
 * No `if (morphProgress > 0.5f)` — uses layout.glowRadius directly.
 *
 * @param state The single source of truth.
 * @param content The content to render inside the glass surface.
 * @param modifier External modifier.
 */
@Composable
fun MiniPlayerSurface(
    state: MiniPlayerState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    // ═════════════════════════════════════════════════════════════════════
    // READ STATE — Rule #17: Single Snapshot
    // ═════════════════════════════════════════════════════════════════════
    val layout = state.layout
    val color = state.color

    // ═════════════════════════════════════════════════════════════════════
    // ANIMATED LAYOUT VALUES — Smooth morph transitions
    // ═════════════════════════════════════════════════════════════════════
    // Rule #19: Animation specs from MiniPlayerMotion.
    // These animate layout.* changes when user drags.

    val density = LocalDensity.current

    val animatedHeight by animateDpAsState(
        targetValue = with(density) { layout.height.dp.coerceAtLeast(0.dp) },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "mp_surface_height",
        finishedListener = {} // No-op, morph settle handled by reducer
    )

    val animatedWidthFraction by animateFloatAsState(
        targetValue = layout.widthFraction,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "mp_surface_width_fraction"
    )

    val animatedCornerRadius by animateDpAsState(
        targetValue = with(density) { layout.cornerRadius.dp.coerceAtLeast(0.dp) },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "mp_surface_corner_radius"
    )

    val animatedGlassTintAlpha by animateFloatAsState(
        targetValue = layout.glassTintAlpha,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "mp_surface_glass_tint"
    )

    val animatedGlassShadowAlpha by animateFloatAsState(
        targetValue = layout.glassShadowAlpha,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "mp_surface_glass_shadow"
    )

    val animatedOpacity by animateFloatAsState(
        targetValue = layout.miniPlayerOpacity,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "mp_surface_opacity"
    )

    // ═════════════════════════════════════════════════════════════════════
    // GLASS STYLE — Dynamic from layout state
    // ═════════════════════════════════════════════════════════════════════
    // Creates a dynamic GlassStyle that fades with morph.
    // During morph: glass fades to transparent (product of surface → screen).
    val currentGlassStyle = com.rikodev.music.android.core.designsystem.theme.GlassStyle(
        blur = 0.dp, // V2: will add backdrop blur
        tintAlpha = animatedGlassTintAlpha,
        borderAlpha = animatedGlassTintAlpha * 0.8f,
        glowAlpha = layout.glowIntensity * 0.1f,
        shadowAlpha = animatedGlassShadowAlpha
    )

    // ═════════════════════════════════════════════════════════════════════
    // TINT COLOR — From state.color, never hardcoded
    // ═════════════════════════════════════════════════════════════════════
    val tintAlpha = animatedGlassTintAlpha.coerceIn(0f, 0.15f)
    val surfaceTintColor = color.glassTint.copy(alpha = tintAlpha)

    // ═════════════════════════════════════════════════════════════════════
    // SHAPE — Animated corner radius from layout
    // ═════════════════════════════════════════════════════════════════════
    val surfaceShape = RoundedCornerShape(animatedCornerRadius)

    // ═════════════════════════════════════════════════════════════════════
    // RENDER — Delegates to GlassSurface (existing design system component)
    // ═════════════════════════════════════════════════════════════════════
    Box(
        modifier = modifier
            .fillMaxWidth(animatedWidthFraction)
            .height(animatedHeight)
            .graphicsLayer {
                // Crossfade opacity during handoff (Phase 6)
                alpha = animatedOpacity
            }
    ) {
        // Glow layer (rendered BEFORE glass surface, behind it)
        // Rule #24: Background owns glow, but for Mini Player,
        // surface glow is rendered here because it's surface-specific.
        // Background glow is ambient (full canvas).
        // (colorGlow is applied as a Modifier below if glow is active)

        GlassSurface(
            modifier = Modifier
                .clip(surfaceShape),
            style = currentGlassStyle,
            cornerRadius = animatedCornerRadius,
            shape = surfaceShape,
            tintColor = surfaceTintColor,
            showBorder = true,
            borderColor = GlassBorderLight,
            enableShadow = layout.shadowElevation > 0f,
            content = { content() }
        )
    }
}

/**
 * Draw noise texture overlay on glass surface.
 *
 * Pre-generated static noise for material feel.
 * Performance Constitution: created once in remember, reused every frame.
 *
 * @param noiseAlpha Noise opacity (typically 0.03).
 */
@Composable
private fun MiniPlayerNoiseOverlay(
    noiseAlpha: Float,
    tileSize: Int = MiniPlayerTokens.NoiseTileSize
) {
    // Performance Constitution — noise texture is a pre-generated pattern.
    // Cached in remember {} so it's generated ONCE and reused across frames.
    val noiseBitmap = remember(tileSize) {
        val bitmap = android.graphics.Bitmap.createBitmap(
            tileSize, tileSize, android.graphics.Bitmap.Config.ARGB_8888
        )
        val pixels = IntArray(tileSize * tileSize)
        val random = java.util.Random(42L) // fixed seed for consistency
        for (i in pixels.indices) {
            val alpha = random.nextInt(24) // very subtle noise
            pixels[i] = (alpha shl 24) or 0x00FFFFFF
        }
        bitmap.setPixels(pixels, 0, tileSize, 0, 0, tileSize, tileSize)
        bitmap
    }

    // Noise is intentionally minimal — barely visible, just enough to
    // break gradient banding on glass surfaces.
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val paint = android.graphics.Paint().apply {
            isAntiAlias = false
            alpha = 30 // very subtle
        }
        val canvas = drawContext.canvas.nativeCanvas
        val shader = android.graphics.BitmapShader(
            noiseBitmap,
            android.graphics.Shader.TileMode.REPEAT,
            android.graphics.Shader.TileMode.REPEAT
        )
        paint.shader = shader
        canvas.drawRect(0f, 0f, size.width, size.height, paint)
    }
}
