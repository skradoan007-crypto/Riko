package com.rikodev.music.android.core.designsystem.riko.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.theme.AMBIENT_GLOW_MAX_ALPHA
import com.rikodev.music.android.core.designsystem.riko.theme.AMBIENT_GLOW_MIN_ALPHA
import com.rikodev.music.android.core.designsystem.riko.theme.RikoCanvasGradient
import com.rikodev.music.android.core.designsystem.riko.theme.RikoGlowBlob
import com.rikodev.music.android.core.designsystem.riko.theme.RikoShadowBlob
import kotlin.math.max

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — APP CANVAS BACKGROUND (SM-UI-SPEC-001 v2.0, Section 1.2-A)
//
// Layered background, per spec:
//   Layer 1 — 4-stop vertical indigo gradient (#0A0A52 → #020210)
//   Layer 2 — one or two soft violet glow blobs (#6C3CE9, 10–14% opacity,
//             ~60% screen width, heavy soft fade — never a hard edge),
//             positioned organically (upper-right-of-center / center)
//   Layer 3 — near-black shadow blob lower-right on list-heavy screens
//
// IMPLEMENTATION: Option A (code-based) — large radial-gradient blobs emulate
// the ~120dp gaussian blur on every API level (no RenderEffect requirement).
//
// accentColor: pass the current track's artwork-dominant color on Now Playing
// (spec 3.7 recommendation) for the premium ambient-lighting feel.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RikoAmbientBackground(
    modifier: Modifier = Modifier,
    accentColor: Color = RikoGlowBlob,
    /** Clamp: 0.10–0.14 per spec Layer 2. */
    ambientAlpha: Float = 0.14f,
    /** Secondary ambient tint — artwork-driven shells pass their own. */
    secondaryColor: Color = accentColor,
    /** Layer 3 vignette — enable on list-heavy screens (Favourites, Playlist). */
    shadowBlob: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val safeAlpha = ambientAlpha.coerceIn(AMBIENT_GLOW_MIN_ALPHA, AMBIENT_GLOW_MAX_ALPHA)
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(RikoCanvasGradient)
    ) {
        val w = with(density) { maxWidth.toPx() }
        val h = with(density) { maxHeight.toPx() }
        val radius = max(w, h) * 0.85f

        // Layer 2 — primary violet glow: upper-right-of-center, organic.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = safeAlpha),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.62f, h * 0.22f),
                        radius = radius
                    )
                )
        )

        // Layer 2b — secondary glow: center-left, weaker (organic asymmetry).
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            secondaryColor.copy(alpha = safeAlpha * 0.6f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.18f, h * 0.55f),
                        radius = radius
                    )
                )
        )

        // Layer 2 (blob core) — centered soft ellipse ≈60% screen width,
        // giving the spec's ~120dp-blur bloom extra body on the canvas.
        val blobSize = with(density) { (max(w, h) * 0.60f).toDp() }
        Box(
            Modifier
                .offset(x = with(density) { (w * 0.30f).toDp() }, y = with(density) { (h * 0.04f).toDp() })
                .size(blobSize)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = safeAlpha * 0.8f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Layer 3 — near-black shadow blob, lower-right (list screens).
        if (shadowBlob) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                RikoShadowBlob,
                                Color.Transparent
                            ),
                            center = Offset(w * 0.88f, h * 0.92f),
                            radius = radius * 0.9f
                        )
                    )
            )
        }

        content()
    }
}