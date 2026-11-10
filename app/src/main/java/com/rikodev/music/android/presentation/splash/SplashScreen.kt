package com.rikodev.music.android.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.music.android.R
import com.rikodev.music.android.core.designsystem.animation.rememberReduceMotion
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMotion
import com.rikodev.music.android.core.designsystem.theme.AccentCyan
import com.rikodev.music.android.core.designsystem.theme.GradientMid
import com.rikodev.music.android.core.designsystem.theme.GradientTop
import com.rikodev.music.android.core.designsystem.theme.TextPrimary
import com.rikodev.music.android.core.designsystem.theme.TextSecondary
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — ANIMATED LAUNCH SCREEN
//
// The system splash (icon-on-background, the OS default for cold launch)
// hands off to THIS the instant the Activity draws — a proper branded
// intro with the R mark, not just a static icon. Original animation,
// original composition — no third-party splash asset used.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RikoSplashScreen(
    progress: Float = 1f,
    onFinished: () -> Unit
) {
    var logoVisible by remember { mutableStateOf(false) }
    var textVisible by remember { mutableStateOf(false) }
    val ringProgressAnim = remember { Animatable(0f) }
    // Plan 3.3: progress reflects real readiness stages — the ring cannot
    // complete before critical readiness does (min-display sweep still plays).
    val ringProgress = ringProgressAnim.value.coerceAtMost(0.25f + 0.75f * progress)

    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0.55f,
        animationSpec = RikoMotion.SoftSpring(),
        label = "splash_logo_scale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = RikoMotion.screen(),
        label = "splash_logo_alpha"
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (textVisible) 1f else 0f,
        animationSpec = tween(450),
        label = "splash_text_alpha"
    )
    val ringAlpha by animateFloatAsState(
        targetValue = if (logoVisible) 0.55f else 0f,
        animationSpec = tween(700),
        label = "splash_ring_alpha"
    )

    val reduceMotion = rememberReduceMotion()

    LaunchedEffect(Unit) {
        // G-7 reduced-motion path: never animate — hand off immediately
        // (the system splash already showed the icon).
        if (reduceMotion) {
            logoVisible = true
            textVisible = true
            ringProgressAnim.snapTo(1f)
            onFinished()
            return@LaunchedEffect
        }
        coroutineScope {
            // G-7 timeout path: no matter what the animation stack does,
            // the user can never be trapped on this screen.
            val watchdog = launch {
                delay(4_000)
                onFinished()
            }
            logoVisible = true
            delay(280)
            textVisible = true
            // Glow ring sweeps outward once, like light catching the mark
            ringProgressAnim.animateTo(1f, animationSpec = tween(900))
            delay(500)
            onFinished()
            watchdog.cancel()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(GradientTop, GradientMid))
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                // Soft expanding glow ring — a one-shot "light catching glass" sweep
                Canvas(modifier = Modifier.size(220.dp)) {
                    val radius = size.minDimension / 2f * (0.55f + ringProgress * 0.5f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AccentCyan.copy(alpha = ringAlpha * (1f - ringProgress) * 0.5f),
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = radius.coerceAtLeast(1f)
                        ),
                        radius = radius.coerceAtLeast(1f),
                        center = Offset(size.width / 2f, size.height / 2f)
                    )
                }

                Image(
                    painter = painterResource(R.mipmap.ic_launcher_foreground),
                    contentDescription = "Riko Music",
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer {
                            scaleX = logoScale
                            scaleY = logoScale
                            alpha = logoAlpha
                        }
                )
            }

            Text(
    text = "Riko Music",
    style = MaterialTheme.typography.headlineSmall,
    color = TextPrimary,
)
            Text(
    text = "Enjoy with your style",
    style = MaterialTheme.typography.bodySmall,
    color = TextSecondary,
)
        }
    }
}
