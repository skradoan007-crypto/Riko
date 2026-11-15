package com.rikodev.music.android.dna.ui

import android.app.ActivityManager
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import com.rikodev.music.android.dna.core.LiveDotInput
import com.rikodev.music.android.dna.sync.MdnaSyncHub
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// ─────────────────────────────────────────────────────────────────────────────
// LIVE·DOT SPECTRUM v5 — True Reference 3D Spherical Dot-Wave Visualizer
//
// Fixes applied:
//  1. Calls hub.attach() / hub.detach() in DisposableEffect so PCM audio flows!
//  2. Samples full 40-band spectrum + Bass + SubBass + Mid + Vocal + Treble + Punch.
//  3. Pure Neon Yellow (Blue = 0 in ADD mode) so overlaps NEVER bleach to white.
//  4. Paint.Cap.ROUND + STROKE so every dot is a crisp circle, never square pixels.
//  5. True video_1.mp4 physics: rings hug outer neon border at rest, peel inward
//     in liquid angular lobes on mid/bands, and expand into a full 3D folded
//     spherical dot-mesh across the entire circle on bass/vocal/beat hits.
// ─────────────────────────────────────────────────────────────────────────────

private const val DOTS_PER_RING = 120
private const val STAR_COUNT = 220

@Composable
fun LiveDotCanvasView(
    hub: MdnaSyncHub,
    playing: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Quality adaptation (matches VisualHost low-RAM check)
    val ringCount = remember(context) {
        val isLowRam = (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)
            ?.isLowRamDevice == true
        if (isLowRam) 40 else 54
    }
    val totalDots = ringCount * DOTS_PER_RING

    // ── CRITICAL FIX: Attach MdnaSyncHub PCM tap so real audio flows! ───
    DisposableEffect(hub) {
        hub.attach()
        onDispose {
            hub.detach()
        }
    }

    // ── Pre-allocated arrays (ZERO allocation in draw loop) ─────────────
    val corePositions = remember(totalDots) { FloatArray(totalDots * 2) }
    val foldPositions = remember(totalDots) { FloatArray(totalDots * 2) }
    val starPositions = remember { FloatArray(STAR_COUNT * 2) }

    val bandCount = remember(hub) { hub.liveOnlySource.bandCount.coerceAtLeast(16) }
    val rawBands = remember(bandCount) { FloatArray(bandCount) }
    val smoothBands = remember(bandCount) { FloatArray(bandCount) }
    val angleSpectrum = remember { FloatArray(DOTS_PER_RING) }

    // ── Pre-allocated paints (Blue = 0 in ADD mode prevents white washout) ──
    val outerHaloPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = android.graphics.Color.argb(110, 255, 240, 0)
            maskFilter = BlurMaskFilter(28f, BlurMaskFilter.Blur.NORMAL)
        }
    }
    val outerCoreRingPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
            style = Paint.Style.STROKE
            color = android.graphics.Color.argb(235, 255, 250, 0)
        }
    }
    val dotGlowPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            // Blue = 0 guarantees pure #FFFF00 neon yellow even when 50 rings stack!
            color = android.graphics.Color.argb(26, 255, 235, 0)
        }
    }
    val dotCorePaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            color = android.graphics.Color.argb(165, 255, 250, 0)
        }
    }
    val starPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 2.2f
            color = android.graphics.Color.argb(85, 200, 210, 255)
        }
    }

    // ── Audio input processor ──────────────────────────────────────────
    val input = remember(hub) { LiveDotInput(hub.liveOnlySource) }

    // ── Reactive animation states (drives Compose Canvas redraw at 60fps) ──
    var frameTick by remember { mutableLongStateOf(0L) }
    var subBassState by remember { mutableFloatStateOf(0f) }
    var bassState by remember { mutableFloatStateOf(0f) }
    var midState by remember { mutableFloatStateOf(0f) }
    var vocalState by remember { mutableFloatStateOf(0f) }
    var trebleState by remember { mutableFloatStateOf(0f) }
    var punchState by remember { mutableFloatStateOf(0f) }
    var envelopeState by remember { mutableFloatStateOf(0f) }
    var phaseState by remember { mutableFloatStateOf(0f) }
    var rollState by remember { mutableFloatStateOf(0f) }

    // ── Starfield initialization (deep space background like video_1.mp4) ──
    LaunchedEffect(Unit) {
        for (i in 0 until STAR_COUNT) {
            val h1 = ((i * 7919 + 13) % 10000) / 10000f
            val h2 = ((i * 104729 + 29) % 10000) / 10000f
            starPositions[i * 2] = h1 * 2.2f - 1.1f
            starPositions[i * 2 + 1] = h2 * 2.2f - 1.1f
        }
    }

    // ── 60fps real-time audio + physics loop ───────────────────────────
    val currentPlaying by rememberUpdatedState(playing)
    LaunchedEffect(hub) {
        var lastNanos = 0L
        var env = 0f
        var sSub = 0f
        var sBass = 0f
        var sMid = 0f
        var sVocal = 0f
        var sTreble = 0f
        var sPunch = 0f
        var continuousPhase = 0f
        var continuousRoll = 0f

        while (true) {
            val nanos = withFrameNanos { it }
            val dt = if (lastNanos == 0L) 0.016f
                     else ((nanos - lastNanos) / 1e9f).coerceIn(0.001f, 0.05f)
            lastNanos = nanos

            val isPlayingNow = currentPlaying || hub.playing

            // 1. Sample full 40-band spectrum directly from MdnaSyncHub
            var hasLive = hub.liveOnlySource.sample(nanos, rawBands)
            if (!hasLive && isPlayingNow) {
                hasLive = hub.source.sample(nanos, rawBands)
            }

            // 2. Also step LiveDotInput for its internal transient classification
            input.step(nanos, dt, isPlayingNow, false)

            // 3. Extract Sub-Bass, Bass, Mid, Vocal (presence), and Treble from bands
            val n = bandCount
            val subEnd = (n * 0.10f).toInt().coerceAtLeast(2)
            val bassEnd = (n * 0.22f).toInt().coerceAtLeast(subEnd + 2)
            val midEnd = (n * 0.50f).toInt().coerceAtLeast(bassEnd + 4)
            val vocalEnd = (n * 0.75f).toInt().coerceAtLeast(midEnd + 4)

            var rawSub = 0f
            var rawBass = 0f
            var rawMid = 0f
            var rawVocal = 0f
            var rawTreble = 0f

            for (i in 0 until n) {
                val v = rawBands[i].coerceIn(0f, 1.5f)
                // Smooth individual bands with fast attack, smooth release
                val rate = if (v > smoothBands[i]) (dt * 28f).coerceAtMost(1f)
                           else (dt * 9f).coerceAtMost(1f)
                smoothBands[i] += (v - smoothBands[i]) * rate

                when {
                    i < subEnd -> rawSub += smoothBands[i]
                    i < bassEnd -> rawBass += smoothBands[i]
                    i < midEnd -> rawMid += smoothBands[i]
                    i < vocalEnd -> rawVocal += smoothBands[i]
                    else -> rawTreble += smoothBands[i]
                }
            }

            rawSub /= subEnd.toFloat()
            rawBass /= (bassEnd - subEnd).toFloat()
            rawMid /= (midEnd - bassEnd).toFloat()
            rawVocal /= (vocalEnd - midEnd).toFloat()
            rawTreble /= (n - vocalEnd).coerceAtLeast(1).toFloat()

            // Combine band energy with LiveDotInput features and boost sensitivity
            val targetSub = maxOf(rawSub * 1.85f, input.bass * 1.15f).coerceIn(0f, 1.3f)
            val targetBass = maxOf(rawBass * 1.75f, input.bass * 1.25f).coerceIn(0f, 1.3f)
            val targetMid = maxOf(rawMid * 1.65f, input.mid * 1.25f).coerceIn(0f, 1.2f)
            val targetVocal = (rawVocal * 1.80f + input.mid * 0.45f).coerceIn(0f, 1.2f)
            val targetTreble = (rawTreble * 1.90f).coerceIn(0f, 1.2f)

            // Detect beat punch from rapid bass/sub-bass rise + input.punch
            val bassDelta = ((targetSub + targetBass) * 0.5f - sBass).coerceAtLeast(0f)
            val targetPunch = maxOf(input.punch * 1.3f, bassDelta * 3.2f).coerceIn(0f, 1.3f)

            // Asymmetric smoothing (snappy attack on beats, silky decay like video_1.mp4)
            val attack = (dt * 24f).coerceAtMost(1f)
            val release = (dt * 6.5f).coerceAtMost(1f)

            sSub += (targetSub - sSub) * if (targetSub > sSub) attack else release
            sBass += (targetBass - sBass) * if (targetBass > sBass) attack else release
            sMid += (targetMid - sMid) * if (targetMid > sMid) attack else release
            sVocal += (targetVocal - sVocal) * if (targetVocal > sVocal) attack else release
            sTreble += (targetTreble - sTreble) * if (targetTreble > sTreble) attack else release
            sPunch += (targetPunch - sPunch) * if (targetPunch > sPunch) (dt * 32f).coerceAtMost(1f) else (dt * 5.5f).coerceAtMost(1f)

            // Overall sphere expansion envelope:
            // 0 = all rings collapsed into outer neon border (00:00.5s in video_1.mp4)
            // 1 = full 3D sphere mesh expanded across the whole circle (00:02.5s / 00:11.0s)
            val rawEnergy = (sBass * 0.55f + sSub * 0.35f + sMid * 0.30f + sVocal * 0.25f + sPunch * 0.45f).coerceIn(0f, 1.15f)
            val envRate = if (rawEnergy > env) (dt * 14f).coerceAtMost(1f) else (dt * 3.8f).coerceAtMost(1f)
            env += (rawEnergy - env) * envRate

            // Map smoothed frequency bands symmetrically around the 120 angular columns
            for (col in 0 until DOTS_PER_RING) {
                // Mirror spectrum around the circle so there is no ugly seam at angle 0
                val half = DOTS_PER_RING / 2
                val mirrored = if (col < half) col else (DOTS_PER_RING - 1 - col)
                val norm = mirrored.toFloat() / (half - 1).toFloat() // 0..1
                val bIdx = (norm * (n - 1)).toInt().coerceIn(0, n - 1)
                angleSpectrum[col] = smoothBands[bIdx].coerceIn(0f, 1.2f)
            }

            // Advance wave phase & 3D rotation speed proportional to music energy
            val speed = 1.4f + 4.2f * sBass + 2.5f * sMid + 2.0f * sPunch
            continuousPhase += dt * speed
            continuousRoll += dt * (0.45f + 1.65f * sVocal + 1.1f * sMid)

            subBassState = sSub
            bassState = sBass
            midState = sMid
            vocalState = sVocal
            trebleState = sTreble
            punchState = sPunch
            envelopeState = env
            phaseState = continuousPhase
            rollState = continuousRoll
            frameTick = nanos
        }
    }

    // ── Canvas Render (ZERO allocation in draw block) ──────────────────
    Canvas(modifier = modifier.background(Color.Black)) {
        // Read frameTick so Compose invalidates Canvas every vsync frame
        val tick = frameTick
        if (tick == 0L) return@Canvas

        val canvas = drawContext.canvas.nativeCanvas
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val R = min(cx, cy) * 0.82f

        val sub = subBassState
        val bass = bassState
        val mid = midState
        val vocal = vocalState
        val treble = trebleState
        val punch = punchState
        val env = envelopeState
        val phase = phaseState
        val roll = rollState

        // ── 1. Deep-Space Starfield Background ─────────────────────────
        canvas.save()
        canvas.translate(cx, cy)
        val starScale = maxOf(w, h) * 0.55f
        canvas.scale(starScale, starScale)
        canvas.drawPoints(starPositions, starPaint)
        canvas.restore()

        // ── 2. Compute 3D Spherical Dot-Wave Positions ─────────────────
        var foldDotCount = 0
        val twoPi = (2.0 * PI).toFloat()
        val halfPi = (PI * 0.5).toFloat()

        // 3D tilt angles driven by vocal, mid, and roll (creates sweeping 3D dome perspective)
        val tiltX = sin(roll * 0.8f) * (0.25f + 0.55f * vocal + 0.35f * bass)
        val tiltY = cos(roll * 0.65f) * (0.25f + 0.55f * mid + 0.35f * punch)

        for (ring in 0 until ringCount) {
            // v = 0.0 at outer neon border, v = 1.0 at center pole of sphere
            val v = ring.toFloat() / (ringCount - 1).toFloat()
            // Spherical latitude mapping: near rim rings are denser, center rings open up
            val sphereRad = cos(v * halfPi) // 1.0 at v=0 (rim), 0.0 at v=1 (center)
            val sphereZ = sin(v * halfPi)   // 0.0 at v=0 (rim), 1.0 at v=1 (center)

            // Travelling radial wave delay from rim to center
            val ringWavePhase = phase - v * 4.2f

            for (col in 0 until DOTS_PER_RING) {
                val angle = col.toFloat() / DOTS_PER_RING.toFloat() * twoPi
                val bandVal = angleSpectrum[col]

                // Angular lobes: 2-lobe (bass/sub), 3-lobe (vocal/mid fold), 5-lobe (treble/spectrum)
                val lobe2 = sin(2f * angle + roll * 1.1f)
                val lobe3 = cos(3f * angle - phase * 0.65f + v * 2.2f)
                val lobe5 = sin(5f * angle + phase * 0.9f - v * 3.0f)

                // Directional wave determines which side of the ring peels inward first
                // (matches 00:01.5s, 00:04.0s, 00:07.5s, 00:09.5s in video_1.mp4)
                val angularExcite = (
                    0.45f * (0.5f + 0.5f * lobe2) * (bass + sub * 0.8f) +
                    0.35f * (0.5f + 0.5f * lobe3) * (vocal + mid * 0.7f) +
                    0.30f * (0.5f + 0.5f * lobe5) * (bandVal + treble * 0.6f)
                ).coerceIn(0f, 1.3f)

                // Local inward expansion:
                // When music is silent (env ~ 0), pull is 0 -> all dots sit at r = R (outer ring)!
                // On partial beats, specific angular lobes pull inward.
                // On full bass/beat hits (00:02.5s, 00:05.5s, 00:11.0s), pull reaches 1.0 across the circle!
                val fullSphereDrive = (env * 0.85f + punch * 0.45f + bass * 0.35f).coerceIn(0f, 1.15f)
                val localPull = (fullSphereDrive * 0.72f + angularExcite * 0.68f).coerceIn(0f, 1.08f)

                // Base radius interpolates between R (compressed at rim) and R * sphereRad (full 3D dome)
                val pullCurve = localPull.pow(1.15f).coerceIn(0f, 1f)
                var normR = 1f - (1f - sphereRad) * pullCurve

                // 3D Fabric Folding Waves (creates the undulating ribbons/creases in 00:03.0s, 00:08.0s, 00:10.5s, 00:11.0s)
                val edgeEnvelope = sin(v * PI.toFloat()).pow(0.75f) // 0 at rim, 1 in interior
                val radialRipple = (
                    0.52f * sin(ringWavePhase + 2f * angle + lobe3 * 1.4f) * (bass + punch * 0.8f) +
                    0.33f * cos(ringWavePhase * 1.35f - 3f * angle) * (vocal + mid) +
                    0.22f * sin(v * 9f - phase * 1.6f + 4f * angle) * (bandVal + treble)
                )

                // Apply radial fold displacement
                val foldDisplacement = radialRipple * 0.26f * edgeEnvelope * pullCurve
                normR += foldDisplacement

                // Tangential (angular) twist creates the 3D S-curve crease across the center (like 00:10.5s & 00:11.0s)
                val twistAngle = angle + edgeEnvelope * pullCurve * (
                    0.28f * sin(2f * angle - phase * 0.7f + v * 3.2f) * (vocal + mid) +
                    0.18f * cos(3f * angle + roll + v * 2.5f) * (bass + punch)
                )

                // 3D Z-elevation for perspective projection
                val zHeight = sphereZ * pullCurve * (
                    0.55f * sin(angle + roll) * tiltX +
                    0.55f * cos(angle - roll * 0.8f) * tiltY +
                    0.35f * radialRipple
                )
                val persp = 1f / (1f - zHeight * 0.28f).coerceIn(0.65f, 1.45f)

                // Convert to 2D coordinates and strictly clamp inside the outer ring R
                // (In video_1.mp4, dots NEVER fly outside the outer neon border R;
                // instead, any outward push stacks densely against the inner edge of R!)
                var dx = normR * cos(twistAngle) * persp
                var dy = normR * sin(twistAngle) * persp
                val dist = hypot(dx, dy)

                // Soft liquid wall at r = 1.0 (outer border)
                val clampedDist = if (dist > 0.995f) {
                    0.995f - 0.012f * ((dist - 0.995f).coerceIn(0f, 0.5f))
                } else {
                    dist
                }
                if (dist > 0.0001f) {
                    val scale = (clampedDist * R) / dist
                    dx *= scale
                    dy *= scale
                }

                val px = cx + dx
                val py = cy + dy

                val idx = (ring * DOTS_PER_RING + col) * 2
                corePositions[idx] = px
                corePositions[idx + 1] = py

                // Points near the outer rim or on steep 3D wave crests get extra additive glow
                val isRimOrFoldCrest = (clampedDist > 0.88f) || (abs(radialRipple) > 0.42f && pullCurve > 0.25f)
                if (isRimOrFoldCrest && foldDotCount < totalDots) {
                    val fIdx = foldDotCount * 2
                    foldPositions[fIdx] = px
                    foldPositions[fIdx + 1] = py
                    foldDotCount++
                }
            }
        }

        // ── 3. Draw Outer Neon Halo & Solid Boundary Ring ──────────────
        val borderPulse = (bass * 0.6f + punch * 0.6f + env * 0.3f).coerceIn(0f, 1.2f)

        // Soft outer neon bloom around the circle
        outerHaloPaint.strokeWidth = 22f + 16f * borderPulse
        outerHaloPaint.alpha = (95 + 85 * borderPulse).toInt().coerceIn(70, 210)
        canvas.drawCircle(cx, cy, R, outerHaloPaint)

        // Crisp continuous neon yellow rim
        outerCoreRingPaint.strokeWidth = 6.5f + 5.5f * borderPulse
        outerCoreRingPaint.alpha = (210 + 45 * borderPulse).toInt().coerceIn(180, 255)
        canvas.drawCircle(cx, cy, R, outerCoreRingPaint)

        // ── 4. Draw 3D Dot-Matrix (Glow Pass + Crisp Dot Core Pass) ────
        // Pass A: Soft additive bloom on rim & folded wave ribbons
        if (foldDotCount > 0) {
            dotGlowPaint.strokeWidth = 9.5f + 3.5f * borderPulse
            dotGlowPaint.alpha = (24 + 18 * borderPulse).toInt().coerceIn(18, 48)
            canvas.drawPoints(foldPositions, 0, foldDotCount * 2, dotGlowPaint)
        }

        // Pass B: Subtle ambient glow on all dots
        dotGlowPaint.strokeWidth = 6.2f
        dotGlowPaint.alpha = 18
        canvas.drawPoints(corePositions, dotGlowPaint)

        // Pass C: Crisp, distinct neon-yellow dots (matches the clean dotted mesh in video_1.mp4)
        dotCorePaint.strokeWidth = 3.5f
        dotCorePaint.alpha = 175
        canvas.drawPoints(corePositions, dotCorePaint)
    }
}
