package com.rikodev.music.android.presentation.nowplaying

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rikodev.music.android.core.designsystem.glass.GlassButtonPrimary
import com.rikodev.music.android.core.designsystem.glass.GlassButtonSecondary
import com.rikodev.music.android.core.designsystem.glass.GlassDialog
import com.rikodev.music.android.core.designsystem.riko.components.RikoHapticEvent
import com.rikodev.music.android.core.designsystem.riko.components.rememberRikoHaptic
import com.rikodev.music.android.core.designsystem.riko.gesture.rikoPress
import com.rikodev.music.android.core.designsystem.theme.GradientPinkVioletEnd
import com.rikodev.music.android.core.designsystem.theme.GradientPinkVioletStart
import com.rikodev.music.android.core.designsystem.theme.RingTimerEnd
import com.rikodev.music.android.core.designsystem.theme.RingTimerStart
import com.rikodev.music.android.core.designsystem.theme.TextPrimary
import com.rikodev.music.android.core.designsystem.theme.TextSecondary
import kotlin.math.ceil
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — SLEEP TIMER SHEET (UI-target v2 rebuild)
//
// Reference: images/UI target/Tab Target (Sleep Timer mockup).
//   • Left-aligned "Sleep timer" title + "Playback fades out when the timer
//     ends" subtitle
//   • 4×2 preset chip grid — 5m…90m + Custom; the active chip fills with the
//     violet→pink gradient and glows, idle chips stay frosted with a hairline
//   • Countdown ring below: blue→green sweep around the minutes remaining,
//     "23 / MINUTES" centered inside (reference instrument face)
//   • Custom summons a dial sheet (slider 5–120 min)
// ─────────────────────────────────────────────────────────────────────────────

private val TIMER_PRESETS = listOf(5, 10, 15, 30, 45, 60, 90)

/**
 * Special timer intents beyond fixed duration.
 * Plan v3.3 §17: TimerIntent includes EndOfAlbum, EndOfQueue.
 */
private const val END_OF_QUEUE = -1

@Composable
fun SleepTimerSheet(
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val sleepRemaining by viewModel.sleepTimerRemaining.collectAsStateWithLifecycle()
    val sleepTotalMs by viewModel.sleepTimerTotalMs.collectAsStateWithLifecycle()
    val haptic = rememberRikoHaptic()

    var showCustomDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // ── Title block (reference) ──────────────────────────────────────
        Text(
            text = "Sleep timer",
            style = MaterialTheme.typography.displaySmall.copy(
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            ),
            color = TextPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Playback fades out when the timer ends",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(Modifier.height(20.dp))

        // ── Preset chip grid — 4 columns × 3 rows, End of Queue + Custom ─
        val activeMinutes = sleepRemaining?.let {
            ceil(sleepTotalMs / 60_000.0).toInt().takeIf { total -> total > 0 }
        }
        // Row 1: 5m, 10m, 15m, 30m
        // Row 2: 45m, 60m, 90m, End of Queue
        // Row 3: Custom
        val row1 = TIMER_PRESETS.take(4)
        val row2 = TIMER_PRESETS.drop(4) + listOf(END_OF_QUEUE)
        val row3 = listOf(null) // null = Custom

        listOf(row1, row2, row3).forEach { rowItems ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { minutes ->
                    val label = when (minutes) {
                        END_OF_QUEUE -> "End of Queue"
                        null -> "Custom"
                        else -> "${minutes}m"
                    }
                    TimerChip(
                        modifier = Modifier.weight(1f),
                        label = label,
                        selected = minutes != null && minutes > 0 && activeMinutes == minutes,
                        onClick = {
                            // Plan §37: timer set = confirmation haptic.
                            haptic.fire(RikoHapticEvent.TIMER_SET)
                            when (minutes) {
                                END_OF_QUEUE -> viewModel.setSleepEndOfQueue()
                                null -> showCustomDialog = true
                                else -> viewModel.setSleepTimer(minutes)
                            }
                        }
                    )
                }
                repeat(4 - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── Countdown ring (reference instrument face) ───────────────────
        val remainingMs = sleepRemaining
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            SleepTimerRing(
                modifier = Modifier.size(190.dp),
                remainingMs = remainingMs,
                totalMs = sleepTotalMs
            )
        }

        // ── Cancel affordance while a timer runs ─────────────────────────
        if (remainingMs != null) {
            Text(
                text = "Cancel timer",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    // Plan 2.4: was indication=null + no press response (dead
                    // control) — rikoPress gives the canonical press scale.
                    .rikoPress { viewModel.cancelSleepTimer() }
                    .padding(vertical = 10.dp)
            )
        }
    }

    if (showCustomDialog) {
        CustomTimerDialog(
            onDismiss = { showCustomDialog = false },
            onSet = { minutes ->
                viewModel.setSleepTimer(minutes)
                showCustomDialog = false
            }
        )
    }
}

// ── Chips ────────────────────────────────────────────────────────────────────

@Composable
private fun TimerChip(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(50.dp)
            .then(
                if (selected) {
                    Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(GradientPinkVioletStart, GradientPinkVioletEnd)
                            ),
                            shape = RoundedCornerShape(50)
                        )
                } else {
                    Modifier
                        .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(50))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(50)
                        )
                }
            )
            .clip(RoundedCornerShape(50))
            // Plan 2.4: canonical press response (was dead-feeling).
            .rikoPress(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) Color.White else TextPrimary
        )
    }
}

// ── Countdown ring ───────────────────────────────────────────────────────────

/**
 * The reference's analog instrument face: a blue→green sweep showing how much
 * of the sleep window remains, minutes printed large in the center. Idle state
 * renders the dim track with "OFF" so the sheet keeps its anchor element.
 */
@Composable
private fun SleepTimerRing(
    modifier: Modifier = Modifier,
    remainingMs: Long?,
    totalMs: Long
) {
    val active = remainingMs != null && totalMs > 0
    val fraction = if (active && totalMs > 0) {
        (remainingMs!!.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val minutesLeft = remainingMs?.let { ceil(it / 60_000.0).toInt() } ?: 0

    val trackColor = Color.White.copy(alpha = 0.07f)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val ringStroke = 15.dp.toPx()
            val ringRadius = (size.minDimension - ringStroke) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Dim full track
            drawCircle(
                color = trackColor,
                radius = ringRadius,
                center = center,
                style = Stroke(width = ringStroke)
            )

            if (active) {
                val sweep = fraction * 360f
                // Glow pass under the crisp arc
                drawArc(
                    brush = Brush.sweepGradient(
                        0f to RingTimerStart.copy(alpha = 0.35f),
                        (fraction * 0.99f).coerceAtLeast(0.01f) to RingTimerEnd.copy(alpha = 0.05f),
                        1f to Color.Transparent,
                        center = center
                    ),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                    size = androidx.compose.ui.geometry.Size(ringRadius * 2f, ringRadius * 2f),
                    style = Stroke(width = ringStroke * 1.9f, cap = StrokeCap.Round)
                )
                // Crisp sweep — rotate so the gradient's 0° meets the arc's
                // 12-o'clock start and blue→green runs along the stroke.
                rotate(-90f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            0f to RingTimerStart,
                            (fraction * 0.99f).coerceAtLeast(0.01f) to RingTimerEnd,
                            1f to Color.Transparent,
                            center = center
                        ),
                        startAngle = 0f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                        size = androidx.compose.ui.geometry.Size(ringRadius * 2f, ringRadius * 2f),
                        style = Stroke(width = ringStroke, cap = StrokeCap.Round)
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (active) {
                Text(
                    text = "$minutesLeft",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextPrimary
                )
                Text(
                    text = "MINUTES",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 3.sp),
                    color = TextSecondary
                )
            } else {
                Text(
                    text = "OFF",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp
                    ),
                    color = TextSecondary
                )
            }
        }
    }
}

// ── Custom duration dialog ───────────────────────────────────────────────────

@Composable
private fun CustomTimerDialog(
    onDismiss: () -> Unit,
    onSet: (Int) -> Unit
) {
    var minutes by remember { mutableStateOf(25f) }

    GlassDialog(onDismissRequest = onDismiss, accentColor = GradientPinkVioletStart) {
        Text(
            text = "Custom timer",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = "${minutes.roundToInt()} minutes",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        androidx.compose.material3.Slider(
            value = minutes,
            onValueChange = { minutes = it },
            valueRange = 5f..120f,
            colors = SliderDefaults.colors(
                thumbColor = GradientPinkVioletEnd,
                activeTrackColor = GradientPinkVioletStart,
                inactiveTrackColor = Color.White.copy(alpha = 0.12f)
            )
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassButtonSecondary(
                text = "Cancel",
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
            GlassButtonPrimary(
                text = "Set",
                onClick = { onSet(minutes.roundToInt()) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
