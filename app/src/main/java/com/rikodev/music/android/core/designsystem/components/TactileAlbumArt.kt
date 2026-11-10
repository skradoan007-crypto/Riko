package com.rikodev.music.android.core.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.theme.ColorObjectMat
import com.rikodev.music.android.core.designsystem.riko.theme.RikoShape
import com.rikodev.music.android.core.util.rememberDeviceTilt
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — TACTILE HERO ALBUM ART
//
// Not a rounded-square image in a box. A layered physical object: a warm
// "mat" card sits behind the art (like a passe-partout mount in a picture
// frame), a real directional shadow grounds it against the background, and
// tilting the phone shifts the art's perspective slightly — like it's a
// real object sitting in your hand, not a flat bitmap. This is the single
// biggest lever for the "hero moment" the Now Playing screen was missing.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TactileAlbumArt(
    painter: Painter?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    accentColor: Color
) {
    val density = LocalDensity.current
    val tilt = rememberDeviceTilt().value
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .graphicsLayer {
                // Subtle perspective response to device tilt — small,
                // deliberate range so it reads as physics, not a gimmick.
                rotationX = -tilt.second * 4.5f
                rotationY = tilt.first * 4.5f
                cameraDistance = 24f * density.density
            },
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        // ── The mat — warm backing card, slightly larger than the art,
        // this is what makes it read as a mounted object rather than a
        // floating image.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(
                    elevation = 28.dp,
                    shape = RikoShape.CardLarge,
                    ambientColor = Color(0x40211A12),
                    spotColor = Color(0x59211A12)
                )
                .clip(RikoShape.CardLarge)
                .drawWithContent {
                    drawRect(ColorObjectMat)
                    drawContent()
                }
        )

        // ── The art itself, inset within the mat like a mounted print ──────
        Box(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .aspectRatio(1f)
                .clip(RikoShape.Card)
        ) {
            if (painter != null) {
                Image(
                    painter = painter,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .drawWithContent {
                            drawRect(
                                Brush.radialGradient(
                                    colors = listOf(accentColor.copy(alpha = 0.25f), ColorObjectMat)
                                )
                            )
                        }
                )
            }

            // RUNTIME FIX 7 (same root issue as RuntimeFix5 — "fake shine
            // making the app look unprofessional"): a static white catch-
            // light oval used to sit here, top-left, 0.30 alpha, with a
            // radius covering roughly half the image. Unlike the hero
            // Play button's shine (pure UI chrome), this one was drawn
            // directly on top of REAL album art photos — every actual
            // photo in the user's library got a fixed fake glare baked
            // over it regardless of the photo's own lighting. Deleted
            // outright, not dimmed. The mat card + directional shadow +
            // tilt-based perspective above already carry the "physical
            // object" feeling on their own; a static decal glued on top
            // of real photos was undermining it, not adding to it.
        }
    }
}
