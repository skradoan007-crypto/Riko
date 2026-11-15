package com.rikodev.music.android.dna.ui

import android.graphics.Paint
import android.graphics.Path
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
import kotlin.math.sqrt

// ─────────────────────────────────────────────────────────────────────────────
// LIVE·DOT SPECTRUM v5 — Canvas 3D neon dot-matrix (ALL 4 BUGS FIXED)
//
// Bug 1 FIX: hub.attach()/detach() in DisposableEffect → PCM tap active
// Bug 2 FIX: ALL audio used (bass, mid, vocal, high, punch, bands[40], roll)
// Bug 3 FIX: Path-based deformed ring (not drawCircle) → liquid wave
// Bug 4 FIX: Paint.Cap.ROUND + Style.STROKE + proper ring spread + alpha
//
// 50 rings × 120 dots = 6000 dots. Zero allocation in draw loop.
// Rest: dots dense near outer border. Beat: 3D fabric folds inward.
// 120 angles ↔ 40 frequency bands: each angle responds to its own band.
// ─────────────────────────────────────────────────────────────────────────────

private const val RING_COUNT = 50
private const val DOTS_PER_RING = 120
private const val TOTAL_DOTS = RING_COUNT * DOTS_PER_RING
private const val STAR_COUNT = 200

@Composable
fun LiveDotCanvasView(
    hub: MdnaSyncHub,
    playing: Boolean,
    modifier: Modifier = Modifier
) {
    // ═══ PRE-ALLOCATED (zero allocation in draw) ═══════════════════════
    val positions = remember { FloatArray(TOTAL_DOTS * 2) }
    val starPositions = remember { FloatArray(STAR_COUNT * 2) }
    val ringPath = remember { Path() }
    val bands = remember { FloatArray(40) }

    // ═══ PAINTS (Bug 4: ROUND + STROKE + proper alpha) ═══════════════
    val dotPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 2.5f
            color = android.graphics.Color.argb(100, 255, 255, 0) // #FFFF00 core
        }
    }
    val dotGlowPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 7f
            color = android.graphics.Color.argb(30, 212, 255, 0) // #D4FF00 glow
        }
    }
    val ringPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 3f
            color = android.graphics.Color.argb(200, 255, 255, 0)
        }
    }
    val ringGlowPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 12f
            color = android.graphics.Color.argb(35, 255, 240, 30)
        }
    }
    val starPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = android.graphics.Color.argb(60, 180, 190, 255)
        }
    }

    // ═══ AUDIO (Bug 1 FIX: attach PCM tap) ════════════════════════════
    val input = remember { LiveDotInput(hub.liveOnlySource) }
    DisposableEffect(hub) {
        hub.attach()
        onDispose { hub.detach() }
    }

    // ═══ STATE (Bug 2 FIX: ALL audio components) ═════════════════════
    var bass by remember { mutableFloatStateOf(0f) }
    var mid by remember { mutableFloatStateOf(0f) }
    var punch by remember { mutableFloatStateOf(0f) }
    var vocal by remember { mutableFloatStateOf(0f) }
    var high by remember { mutableFloatStateOf(0f) }
    var phase by remember { mutableFloatStateOf(0f) }
    var roll by remember { mutableFloatStateOf(0f) }
    var frameTick by remember { mutableLongStateOf(0L) }

    // ═══ STARFIELD INIT ═══════════════════════════════════════════════
    LaunchedEffect(Unit) {
        for (i in 0 until STAR_COUNT) {
            val h1 = ((i * 7919 + 1) % 10000) / 10000f
            val h2 = ((i * 104729 + 1) % 10000) / 10000f
            starPositions[i * 2] = h1 * 2.4f - 1.2f
            starPositions[i * 2 + 1] = h2 * 2.4f - 1.2f
        }
    }

    // ═══ 60fps LOOP (Bug 2 FIX: compute ALL audio) ═══════════════════
    LaunchedEffect(playing) {
        var lastNanos = 0L
        while (true) {
            val nanos = withFrameNanos { it }
            val dt = if (lastNanos == 0L) 0.016f
                     else ((nanos - lastNanos) / 1e9f).coerceIn(0.001f, 0.05f)
            lastNanos = nanos

            input.step(nanos, dt, playing, false)

            // Core envelopes
            bass = input.bass
            mid = input.mid
            punch = input.punch

            // Vocal (bands 20-32) + High (bands 33-39) from live spectrum
            val fast = input.fast
            var vs = 0f
            for (i in 20..32) vs += fast[i] * fast[i]
            vocal = sqrt(vs / 13f).coerceIn(0f, 1f)
            var hs = 0f
            for (i in 33..39) hs += fast[i] * fast[i]
            high = sqrt(hs / 7f).coerceIn(0f, 1f)

            // Copy 40-band spectrum for per-angle frequency response
            fast.copyInto(bands)

            // Always advance phase (breathing) + roll (rotation)
            phase += dt * 2f
            roll += dt * 0.3f

            // Trigger Canvas redraw
            frameTick = nanos
        }
    }

    // ═══ CANVAS DRAW (Bug 3+4 FIX) ═══════════════════════════════════
    Canvas(modifier = modifier.background(Color.Black)) {
        val tick = frameTick // ensures redraw every frame
        val canvas = drawContext.canvas.nativeCanvas
        val w = size.width
        val h = size.height
        if (w < 1f || h < 1f) return@Canvas
        val cx = w / 2f
        val cy = h / 2f
        val R = min(cx, cy) * 0.82f
        val body = bass.coerceIn(0f, 1f)
        val mids = mid.coerceIn(0f, 1f)
        val voc = vocal.coerceIn(0f, 1f)
        val hi = high.coerceIn(0f, 1f)
        val p = punch.coerceIn(0f, 1f)

        // ── Starfield background ──────────────────────────────────────
        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(w / 2.4f, h / 2.4f)
        canvas.drawPoints(starPositions, starPaint)
        canvas.restore()

        // ── Bug 3 FIX: Path-based deformed neon ring ──────────────────
        ringPath.reset()
        for (i in 0..DOTS_PER_RING) {
            val angle = i.toFloat() / DOTS_PER_RING * 2f * PI.toFloat()
            val bandIdx = (i % 40)
            val bandE = bands[bandIdx]
            // Liquid wave deformation
            val waveOff = sin(angle * 4f + phase * 1.2f) * 0.025f * R * (0.3f + 0.7f * mids)
            val bandOff = bandE * 0.035f * R
            val punchOff = p * sin(angle * 6f + phase * 2f) * 0.03f * R
            val breathe = sin(phase * 0.5f + angle * 2f) * 0.01f * R
            val rr = R + waveOff + bandOff + punchOff + breathe
            val x = cx + rr * cos(angle)
            val y = cy + rr * sin(angle)
            if (i == 0) ringPath.moveTo(x, y) else ringPath.lineTo(x, y)
        }
        ringPath.close()
        // Glow + core ring
        ringGlowPaint.strokeWidth = 12f + 8f * body + 5f * p
        ringGlowPaint.alpha = (35 + 45 * body + 30 * p).toInt().coerceIn(10, 80)
        canvas.drawPath(ringPath, ringGlowPaint)
        ringPaint.strokeWidth = 3f + 4f * body + 2.5f * p
        ringPaint.alpha = (180 + 60 * body + 50 * p).toInt().coerceIn(0, 255)
        canvas.drawPath(ringPath, ringPaint)

        // ── Bug 2+4 FIX: 50×120 dot matrix with ALL audio ═══════════
        for (ring in 0 until RING_COUNT) {
            val v = ring / (RING_COUNT - 1f)
            val ringFactor = sin(v * PI.toFloat())
            val delay = v * 2.5f
            val wavePhase = phase - delay

            for (col in 0 until DOTS_PER_RING) {
                val angle = col.toFloat() / DOTS_PER_RING * 2f * PI.toFloat() + roll * 0.5f

                // Bug 2 FIX: per-angle frequency response (120 angles ↔ 40 bands)
                val bandIdx = col / 3
                val bandE = bands[bandIdx.coerceIn(0, 39)]

                // Multi-layered wave — ALL audio components used
                // Fabric fold (vocal/mid): sin(angle*3+phase)*cos(v*PI)
                val fabricFold = sin(angle * 3f + wavePhase) * cos(v * PI.toFloat()) *
                                 (0.05f + 0.12f * mids + 0.08f * voc)
                // Frequency band wave (spectrum response at this angle)
                val freqWave = bandE * sin(angle * 2f + wavePhase * 1.5f) * 0.10f
                // Bass shockwave (radial inward pulse)
                val shockwave = body * sin(v * PI.toFloat() * 2f - wavePhase * 2f) * 0.15f
                // High shimmer (sparkle)
                val shimmer = hi * sin(angle * 8f + phase * 3f) * 0.03f
                // Punch snap (quick beat response)
                val punchSnap = p * sin(v * PI.toFloat() * 3f - phase * 3f) * 0.12f
                // Always-on breathing
                val breathe = sin(phase * 0.5f + v * 3f) * 0.015f

                // Bug 4 FIX: proper ring spread (NOT collapsed at 0.02f)
                var radius = R * (1f - (0.18f + 0.65f * body + 0.25f * mids) * ringFactor)

                // Combine all wave displacement
                radius += (fabricFold + freqWave + shockwave + shimmer + punchSnap + breathe) *
                          R * ringFactor

                // 3D depth
                val z = body * sin(angle + wavePhase) * ringFactor * 0.6f +
                        bandE * 0.2f * ringFactor
                val persp = 1f / (1f - z * 0.22f)

                // Store in pre-allocated array
                val idx = (ring * DOTS_PER_RING + col) * 2
                positions[idx] = cx + radius * cos(angle) * persp
                positions[idx + 1] = cy + radius * sin(angle) * persp
            }
        }

        // ── Draw dots (Bug 4 FIX: round + proper alpha) ───────────────
        // Glow pass (wider, dimmer — alpha 20..45 range)
        dotGlowPaint.strokeWidth = 7f
        dotGlowPaint.alpha = (25 + 20 * body + 15 * p).toInt().coerceIn(20, 45)
        canvas.drawPoints(positions, dotGlowPaint)
        // Core pass (sharp, brighter — alpha 70..140 range)
        dotPaint.strokeWidth = 2.5f
        dotPaint.alpha = (90 + 40 * body + 10 * p).toInt().coerceIn(70, 140)
        canvas.drawPoints(positions, dotPaint)
    }
}
