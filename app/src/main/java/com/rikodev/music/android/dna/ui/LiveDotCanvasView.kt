package com.rikodev.music.android.dna.ui

import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import com.rikodev.music.android.dna.core.LiveDotInput
import com.rikodev.music.android.dna.sync.MdnaSyncHub
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// LIVE·DOT SPECTRUM v4 — Canvas-based 3D neon dot-matrix visualizer
//
// 50 concentric rings × 120 dots = 6000 dots in a pre-allocated FloatArray.
// Rest state: all rings collapsed near the outer neon border (dense glow ring).
// Beat state: 3D wave folds spread inward like fabric (bright ribbons).
//
// ZERO allocation in the draw loop — all arrays and paints are pre-allocated.
// Additive blending (PorterDuff.Mode.ADD) creates natural bloom where dots overlap.
// Starfield background with subtle deep-space particles.
//
// Strict requirements met:
//  1. No Black Screen — Canvas + drawPoints (no shader compilation risk)
//  2. Rest vs Beat State — rings collapse at border, fold inward on beat
//  3. Zero Allocation — pre-allocated FloatArray + Paint, no objects in draw
// ─────────────────────────────────────────────────────────────────────────────

private const val RING_COUNT = 50
private const val DOTS_PER_RING = 120
private const val TOTAL_DOTS = RING_COUNT * DOTS_PER_RING // 6000
private const val STAR_COUNT = 200

@Composable
fun LiveDotCanvasView(
    hub: MdnaSyncHub,
    playing: Boolean,
    modifier: Modifier = Modifier
) {
    // ── Pre-allocated arrays (ZERO allocation in draw loop) ─────────────
    val positions = remember { FloatArray(TOTAL_DOTS * 2) } // x,y pairs
    val starPositions = remember { FloatArray(STAR_COUNT * 2) }

    // ── Pre-allocated paints with additive blending ────────────────────
    val dotPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
            style = Paint.Style.FILL
            color = android.graphics.Color.argb(200, 255, 235, 0) // neon yellow
        }
    }
    val dotGlowPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
            style = Paint.Style.FILL
            color = android.graphics.Color.argb(60, 255, 240, 50) // soft glow
        }
    }
    val ringPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            color = android.graphics.Color.argb(230, 255, 255, 0) // #FFFF00
        }
    }
    val ringGlowPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            color = android.graphics.Color.argb(50, 255, 240, 30)
        }
    }
    val starPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = android.graphics.Color.argb(70, 180, 190, 255)
        }
    }

    // ── Audio input (reuses existing smoothing + classification) ───────
    val input = remember { LiveDotInput(hub.liveOnlySource) }

    // ── Animation state ────────────────────────────────────────────────
    var bass by remember { mutableFloatStateOf(0f) }
    var mid by remember { mutableFloatStateOf(0f) }
    var punch by remember { mutableFloatStateOf(0f) }
    var phase by remember { mutableFloatStateOf(0f) }
    var roll by remember { mutableFloatStateOf(0f) }

    // ── Starfield init (one-time) ──────────────────────────────────────
    LaunchedEffect(Unit) {
        for (i in 0 until STAR_COUNT) {
            val h1 = ((i * 7919 + 1) % 10000) / 10000f
            val h2 = ((i * 104729 + 1) % 10000) / 10000f
            starPositions[i * 2] = h1 * 2.4f - 1.2f
            starPositions[i * 2 + 1] = h2 * 2.4f - 1.2f
        }
    }

    // ── 60fps animation loop ───────────────────────────────────────────
    LaunchedEffect(playing) {
        var lastNanos = 0L
        while (true) {
            val nanos = withFrameNanos { it }
            val dt = if (lastNanos == 0L) 0.016f
                     else ((nanos - lastNanos) / 1e9f).coerceIn(0.001f, 0.05f)
            lastNanos = nanos

            input.step(nanos, dt, playing, false)
            bass = input.bass
            mid = input.mid
            punch = input.punch
            phase = input.phase
            roll = input.roll
        }
    }

    // ── Canvas draw (ZERO allocation inside this block) ────────────────
    Canvas(modifier = modifier.background(Color.Black)) {
        val canvas = drawContext.canvas.nativeCanvas
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val R = min(cx, cy) * 0.82f

        // ── 1. Starfield background ────────────────────────────────────
        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(w / 2.4f, h / 2.4f)
        canvas.drawPoints(starPositions, starPaint)
        canvas.restore()

        // ── 2. Outer glowing neon border ring ──────────────────────────
        // Liquid wave deformation on beat
        val ringWidth = 3.5f + 5f * bass + 3f * punch
        ringGlowPaint.strokeWidth = ringWidth * 3f
        ringGlowPaint.alpha = (40 + 50 * bass + 30 * punch).toInt().coerceIn(0, 255)
        canvas.drawCircle(cx, cy, R, ringGlowPaint)

        ringPaint.strokeWidth = ringWidth
        ringPaint.alpha = (200 + 55 * bass + 50 * punch).toInt().coerceIn(0, 255)
        canvas.drawCircle(cx, cy, R, ringPaint)

        // ── 3. Update 6000 dot positions (ZERO allocation) ─────────────
        val body = bass.coerceIn(0f, 1f)
        val mids = mid.coerceIn(0f, 1f)

        for (ring in 0 until RING_COUNT) {
            val v = ring / (RING_COUNT - 1f) // 0..1
            val ringFactor = sin(v * PI.toFloat()) // 0 at edges, 1 at center
            val delay = v * 2.5f
            val wavePhase = phase - delay

            for (col in 0 until DOTS_PER_RING) {
                val angle = col.toFloat() / DOTS_PER_RING * 2f * PI.toFloat()

                // Multi-layered 3D wave (3 harmonics — smooth, no jitter)
                val wave = 0.50f * sin(3f * angle + wavePhase) +
                           0.30f * sin(5f * angle - 1.3f * wavePhase) +
                           0.20f * sin(2f * angle + 0.7f * wavePhase)

                // Rest radius: collapsed near outer border (dense glow ring)
                var radius = R * (1f - 0.02f * ringFactor)

                // Beat fold: push inward (3D fabric fold)
                val fold = body * ringFactor * (0.45f + 0.55f * punch)
                radius -= R * fold * 0.75f

                // Wave displacement (fabric ripples)
                radius += R * wave * 0.18f * ringFactor * (0.15f + 0.85f * body)

                // 3D depth
                val z = body * sin(angle + wavePhase) * ringFactor * 0.8f

                // Perspective projection
                val persp = 1f / (1f - z * 0.22f)
                val x = cx + radius * cos(angle) * persp
                val y = cy + radius * sin(angle) * persp

                // Store in pre-allocated array (ZERO allocation)
                val idx = (ring * DOTS_PER_RING + col) * 2
                positions[idx] = x
                positions[idx + 1] = y
            }
        }

        // ── 4. Draw dots with additive blending (bloom at overlaps) ────
        // Glow pass (wider, softer)
        dotGlowPaint.strokeWidth = 8f
        canvas.drawPoints(positions, dotGlowPaint)
        // Core pass (sharp, bright)
        dotPaint.strokeWidth = 3f
        canvas.drawPoints(positions, dotPaint)
    }
}
