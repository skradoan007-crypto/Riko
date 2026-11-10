package com.rikodev.music.android.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMotion
import com.rikodev.music.android.core.designsystem.riko.theme.RikoStateTokens
import com.rikodev.music.android.core.designsystem.theme.TextPrimary
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import com.rikodev.music.android.core.designsystem.riko.theme.RikoShape
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — TRANSPORT CONTROL HIERARCHY (v2)
//
// The old row was five near-identical glass bubbles — no hierarchy, no
// sense of what matters. This replaces it with one deliberate contrast:
// a single heavy, dial-like hero for Play/Pause, and quiet, flat,
// unadorned icons for everything else. One thing to look at, four things
// to use without thinking about.
// ─────────────────────────────────────────────────────────────────────────────

/** The one hero control — a solid, weighted dial, not a glass bubble. */
@Composable
fun HeroPlayButton(
    isPlaying: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 88.dp
) {
    var isPressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) RikoStateTokens.PressScale else 1f,
        animationSpec = RikoMotion.MediumSpring(),
        label = "hero_play_press"
    )

    Box(
        modifier = modifier
            .size(buttonSize)
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .shadow(
                elevation = if (isPressed) 6.dp else 16.dp,
                shape = RikoShape.Circle,
                ambientColor = accentColor.copy(alpha = 0.5f),
                spotColor = accentColor.copy(alpha = 0.6f)
            )
            .clip(RikoShape.Circle)
            .drawBehind {
                val buttonPx = buttonSize.toPx()
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(
                                red = (accentColor.red * 1.15f).coerceAtMost(1f),
                                green = (accentColor.green * 1.15f).coerceAtMost(1f),
                                blue = (accentColor.blue * 1.15f).coerceAtMost(1f)
                            ),
                            accentColor
                        ),
                        center = Offset(buttonPx * 0.35f, buttonPx * 0.3f),
                        radius = buttonPx * 0.9f
                    )
                )
                // A single thin inner ring — the "dial" detail
                drawCircle(
                    color = Color.White.copy(alpha = 0.35f),
                    radius = (buttonPx / 2f) - 6.dp.toPx(),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx())
                )
                // RUNTIME FIX 5 (direct request — "দুই নাম্বারি shine... making
                // the whole app unprofessional"): a specular highlight oval
                // used to sit here — 0.4 alpha, sized 60%x50% of the entire
                // button. This is the app's single most prominent element
                // (the Now Playing hero Play/Pause button), and it was the
                // one live surface that never got the "glossy hotspot" fix
                // Design Brief Pass 1 applied elsewhere. Deleted outright
                // (not just dimmed again) per direct instruction — the dial
                // ring above already carries the glass-detail job on its
                // own.
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { isPressed = true; tryAwaitRelease(); isPressed = false },
                    onTap = { onClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
Icon(
    imageVector = if (isPlaying)
        Icons.Filled.Pause
    else
        Icons.Filled.PlayArrow,
    contentDescription = if (isPlaying) "Pause" else "Play",
    tint = Color.White,
    modifier = Modifier.size(buttonSize * 0.42f)

        )
    }
}

/** Quiet, flat, unadorned — no bubble, no shadow. Just a confident icon
 *  with a tiny press-scale for feedback. Active state = colour only. */
@Composable
fun MinimalTransportButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) RikoStateTokens.PressScale else 1f,
        animationSpec = RikoMotion.MediumSpring(),
        label = "minimal_transport_press"
    )

    Box(
        modifier = modifier
            .size(48.dp)
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { isPressed = true; tryAwaitRelease(); isPressed = false },
                    onTap = { onClick() }
                )
            },
        contentAlignment = Alignment.Center,
        content = { content() }
    )
}
