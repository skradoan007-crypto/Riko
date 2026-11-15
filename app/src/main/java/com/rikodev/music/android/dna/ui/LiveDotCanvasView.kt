package com.rikodev.music.android.dna.ui

import android.app.ActivityManager
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
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

// ─────────────────────────────────────────────────────────────────────────────
// LIVE·DOT SPECTRUM v6 — Rock-Solid Neon Yellow 3D Dot-Wave Visualizer
//
// Fixed:
//  1. Removed PorterDuff.Mode.ADD completely (fixes full-screen pale-blue washout).
//  2. Uses 3-pass alpha layering (Outer Glow + Mid Bloom + Crisp #FFFF00 Core)
//     for true neon-yellow luminance on pitch-black background.
//  3. Multi-fallback audio sampling (liveOnlySource -> source -> direct ring)
//     plus LiveDotInput envelopes so bass, mid, vocal, treble & bands ALWAYS drive
//     the 3D spherical dot-matrix.
// ─────────────────────────────────────────────────────────────────────────────

private const val DOTS_PER_RING = 120
private const val STAR_COUNT = 180

@Composable
fun LiveDotCanvasView(
    hub: MdnaSyncHub,
    playing: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val ringCount = remember(context) {
        val isLowRam = (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)
            ?.isLowRamDevice == true
        if (isLowRam) 38 else 50
    }
    val totalDots = ringCount * DOTS_PER_RING

    // Attach MdnaSyncHub PCM tap while this screen is visible
    DisposableEffect(hub) {
        hub.attach()
        onDispose {
            hub.detach()
        }
    }

    // Pre-allocated arrays (ZERO allocation in draw loop)
    val allDotPositions = remember(totalDots) { FloatArray(totalDots * 2) }
    val crestDotPositions = remember(totalDots) { FloatArray(totalDots * 2) }
    val starPositions = remember { FloatArray(STAR_COUNT * 2) }
    val outerRingPath = remember { Path() }

    val bandCount = remember(hub) { hub.liveOnlySource.bandCount.coerceAtLeast(16) }
    val rawBands = remember(bandCount) { FloatArray(bandCount) }
    val smoothBands = remember(bandCount) { FloatArray(bandCount) }
    val angleSpectrum = remember { FloatArray(DOTS_PER_RING) }

    // Safe SRC_OVER Paints (NO PorterDuff.ADD — prevents screen tinting bug!)
    val outerBlurPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            color = android.graphics.Color.argb(130, 255, 240, 0)
            maskFilter = BlurMaskFilter(24f, BlurMaskFilter.Blur.NORMAL)
        }
    }
    val outerRimPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            color = android.graphics.Color.argb(255, 255, 252, 20)
        }
    }
    val dotAmbientGlowPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            color = android.graphics.Color.argb(38, 255, 230, 0)
        }
    }
    val dotCrestGlowPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            color = android.graphics.Color.argb(95, 255, 245, 30)
        }
    }
    val dotCorePaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            color = android.graphics.Color.argb(215, 255, 255, 40)
        }
    }
    val starPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 2f
            color = android.graphics.Color.argb(75, 190, 200, 235)
        }
    }

    val input = remember(hub) { LiveDotInput(hub.liveOnlySource) }

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

    LaunchedEffect(Unit) {
        for (i in 0 until STAR_COUNT) {
            val h1 = ((i * 7919 + 13) % 10000) / 10000f
            val h2 = ((i * 104729 + 29) % 10000) / 10000f
            starPositions[i * 2] = h1 * 2.1f - 1.05f
            starPositions[i * 2 + 1] = h2 * 2.1f - 1.05f
        }
    }

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

            // Sample spectrum with fallback so audio data never drops
            var hasLive = hub.liveOnlySource.sample(nanos, rawBands)
            if (!hasLive) {
                hasLive = hub.source.sample(nanos, rawBands)
            }

            input.step(nanos, dt, isPlayingNow, false)

            // Check if rawBands actually has non-zero energy; if not, pull from input.fast
            var bandEnergySum = 0f
            for (i in 0 until bandCount) {
                bandEnergySum += rawBands[i]
            }
            if (bandEnergySum < 0.0001f && input.hasSignal) {
                val copyLen = min(bandCount, input.fast.size)
                for (i in 0 until copyLen) rawBands[i] = input.fast[i]
            }

            val n = bandCount
            val subEnd = (n * 0.10f).toInt().coerceAtLeast(2)
            val bassEnd = (n * 0.25f).toInt().coerceAtLeast(subEnd + 2)
            val midEnd = (n * 0.55f).toInt().coerceAtLeast(bassEnd + 4)
            val vocalEnd = (n * 0.78f).toInt().coerceAtLeast(midEnd + 4)

            var rawSub = 0f
            var rawBass = 0f
            var rawMid = 0f
            var rawVocal = 0f
            var rawTreble = 0f

            for (i in 0 until n) {
                val v = rawBands[i].coerceIn(0f, 1.5f)
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

            // Strong sensitivity boost so normal music volume drives full 3D sphere waves
            val targetSub = maxOf(rawSub * 2.4f, input.bass * 1.35f).coerceIn(0f, 1.4f)
            val targetBass = maxOf(rawBass * 2.2f, input.bass * 1.45f).coerceIn(0f, 1.4f)
            val targetMid = maxOf(rawMid * 2.0f, input.mid * 1.40f).coerceIn(0f, 1.3f)
            val targetVocal = (rawVocal * 2.2f + input.mid * 0.55f).coerceIn(0f, 1.3f)
            val targetTreble = (rawTreble * 2.3f).coerceIn(0f, 1.3f)

            val bassDelta = ((targetSub + targetBass) * 0.5f - sBass).coerceAtLeast(0f)
            val targetPunch = maxOf(input.punch * 1.5f, bassDelta * 3.8f).coerceIn(0f, 1.4f)

            val attack = (dt * 26f).coerceAtMost(1f)
            val release = (dt * 6.0f).coerceAtMost(1f)

            sSub += (targetSub - sSub) * if (targetSub > sSub) attack else release
            sBass += (targetBass - sBass) * if (targetBass > sBass) attack else release
            sMid += (targetMid - sMid) * if (targetMid > sMid) attack else release
            sVocal += (targetVocal - sVocal) * if (targetVocal > sVocal) attack else release
            sTreble += (targetTreble - sTreble) * if (targetTreble > sTreble) attack else release
            sPunch += (targetPunch - sPunch) * if (targetPunch > sPunch) (dt * 34f).coerceAtMost(1f) else (dt * 5.5f).coerceAtMost(1f)

            val rawEnergy = (sBass * 0.60f + sSub * 0.40f + sMid * 0.35f + sVocal * 0.30f + sPunch * 0.50f).coerceIn(0f, 1.25f)
            val envRate = if (rawEnergy > env) (dt * 15f).coerceAtMost(1f) else (dt * 3.8f).coerceAtMost(1f)
            env += (rawEnergy - env) * envRate

            for (col in 0 until DOTS_PER_RING) {
                val half = DOTS_PER_RING / 2
                val mirrored = if (col < half) col else (DOTS_PER_RING - 1 - col)
                val norm = mirrored.toFloat() / (half - 1).toFloat()
                val bIdx = (norm * (n - 1)).toInt().coerceIn(0, n - 1)
                angleSpectrum[col] = (smoothBands[bIdx] * 1.8f).coerceIn(0f, 1.3f)
            }

            val speed = 1.6f + 4.5f * sBass + 2.8f * sMid + 2.2f * sPunch
            continuousPhase += dt * speed
            continuousRoll += dt * (0.5f + 1.8f * sVocal + 1.2f * sMid)

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

    Canvas(
        modifier = modifier
            .clipToBounds()
            .background(Color.Black)
    ) {
        if (frameTick == 0L) return@Canvas

        val canvas = drawContext.canvas.nativeCanvas
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val R = min(cx, cy) * 0.80f

        val sub = subBassState
        val bass = bassState
        val mid = midState
        val vocal = vocalState
        val treble = trebleState
        val punch = punchState
        val env = envelopeState
        val phase = phaseState
        val roll = rollState

        // 1. Starfield Background
        canvas.save()
        canvas.translate(cx, cy)
        val starScale = maxOf(w, h) * 0.55f
        canvas.scale(starScale, starScale)
        canvas.drawPoints(starPositions, starPaint)
        canvas.restore()

        // 2. Compute 3D Spherical Dot-Wave Positions
        var crestCount = 0
        val twoPi = (2.0 * PI).toFloat()
        val halfPi = (PI * 0.5).toFloat()

        val tiltX = sin(roll * 0.8f) * (0.25f + 0.60f * vocal + 0.40f * bass)
        val tiltY = cos(roll * 0.65f) * (0.25f + 0.60f * mid + 0.40f * punch)

        for (ring in 0 until ringCount) {
            val v = ring.toFloat() / (ringCount - 1).toFloat()
            val sphereRad = cos(v * halfPi)
            val sphereZ = sin(v * halfPi)
            val ringWavePhase = phase - v * 4.2f

            for (col in 0 until DOTS_PER_RING) {
                val angle = col.toFloat() / DOTS_PER_RING.toFloat() * twoPi
                val bandVal = angleSpectrum[col]

                val lobe2 = sin(2f * angle + roll * 1.1f)
                val lobe3 = cos(3f * angle - phase * 0.65f + v * 2.2f)
                val lobe5 = sin(5f * angle + phase * 0.9f - v * 3.0f)

                val angularExcite = (
                    0.48f * (0.5f + 0.5f * lobe2) * (bass + sub * 0.85f) +
                    0.38f * (0.5f + 0.5f * lobe3) * (vocal + mid * 0.75f) +
                    0.32f * (0.5f + 0.5f * lobe5) * (bandVal + treble * 0.65f)
                ).coerceIn(0f, 1.35f)

                val fullSphereDrive = (env * 0.90f + punch * 0.50f + bass * 0.40f).coerceIn(0f, 1.2f)
                val localPull = (fullSphereDrive * 0.75f + angularExcite * 0.70f).coerceIn(0f, 1.1f)

                val pullCurve = localPull.pow(1.1f).coerceIn(0f, 1f)
                var normR = 1f - (1f - sphereRad) * pullCurve

                val edgeEnvelope = sin(v * PI.toFloat()).pow(0.75f)
                val radialRipple = (
                    0.55f * sin(ringWavePhase + 2f * angle + lobe3 * 1.4f) * (bass + punch * 0.85f) +
                    0.35f * cos(ringWavePhase * 1.35f - 3f * angle) * (vocal + mid) +
                    0.24f * sin(v * 9f - phase * 1.6f + 4f * angle) * (bandVal + treble)
                )

                normR += radialRipple * 0.28f * edgeEnvelope * pullCurve

                val twistAngle = angle + edgeEnvelope * pullCurve * (
                    0.30f * sin(2f * angle - phase * 0.7f + v * 3.2f) * (vocal + mid) +
                    0.20f * cos(3f * angle + roll + v * 2.5f) * (bass + punch)
                )

                val zHeight = sphereZ * pullCurve * (
                    0.55f * sin(angle + roll) * tiltX +
                    0.55f * cos(angle - roll * 0.8f) * tiltY +
                    0.35f * radialRipple
                )
                val persp = 1f / (1f - zHeight * 0.28f).coerceIn(0.65f, 1.45f)

                var dx = normR * cos(twistAngle) * persp
                var dy = normR * sin(twistAngle) * persp
                val dist = hypot(dx, dy)

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
                allDotPositions[idx] = px
                allDotPositions[idx + 1] = py

                val isCrest = (clampedDist > 0.88f) || (abs(radialRipple) > 0.38f && pullCurve > 0.22f)
                if (isCrest && crestCount < totalDots) {
                    val cIdx = crestCount * 2
                    crestDotPositions[cIdx] = px
                    crestDotPositions[cIdx + 1] = py
                    crestCount++
                }
            }
        }

        // 3. Liquid Outer Neon Ring (Path with subtle wave deformation on beats)
        val borderPulse = (bass * 0.65f + punch * 0.65f + env * 0.35f).coerceIn(0f, 1.25f)
        outerRingPath.reset()
        for (i in 0 until DOTS_PER_RING) {
            val a = (i.toFloat() / DOTS_PER_RING.toFloat()) * twoPi
            val liquidWave = (
                sin(a * 3f + phase * 0.8f) * 0.022f * (bass + punch) +
                cos(a * 5f - roll * 1.2f) * 0.014f * (vocal + mid)
            )
            val rimR = R * (1f + liquidWave)
            val rx = cx + rimR * cos(a)
            val ry = cy + rimR * sin(a)
            if (i == 0) outerRingPath.moveTo(rx, ry) else outerRingPath.lineTo(rx, ry)
        }
        outerRingPath.close()

        outerBlurPaint.strokeWidth = 20f + 14f * borderPulse
        outerBlurPaint.alpha = (110 + 90 * borderPulse).toInt().coerceIn(90, 220)
        canvas.drawPath(outerRingPath, outerBlurPaint)

        outerRimPaint.strokeWidth = 6f + 5f * borderPulse
        canvas.drawPath(outerRingPath, outerRimPaint)

        // 4. Draw 3D Dot-Matrix (3 Safe Alpha Passes)
        // Pass 1: Soft ambient glow under all dots
        dotAmbientGlowPaint.strokeWidth = 7.5f
        canvas.drawPoints(allDotPositions, dotAmbientGlowPaint)

        // Pass 2: Brighter neon bloom on folded wave crests & rim
        if (crestCount > 0) {
            dotCrestGlowPaint.strokeWidth = 8.5f + 3f * borderPulse
            canvas.drawPoints(crestDotPositions, 0, crestCount * 2, dotCrestGlowPaint)
        }

        // Pass 3: Sharp, crisp #FFFF00 neon yellow dots
        dotCorePaint.strokeWidth = 3.4f
        canvas.drawPoints(allDotPositions, dotCorePaint)
    }
}
