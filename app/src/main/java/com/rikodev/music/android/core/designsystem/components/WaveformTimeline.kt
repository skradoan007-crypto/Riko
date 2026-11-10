package com.rikodev.music.android.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.rikodev.music.android.audiofx.WaveformData
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMotion
import kotlinx.coroutines.flow.collectLatest

/** Audio-driven flowing foreground available before file analysis completes.
 * The cached full-song envelope remains available when inspecting/seeking.
 * The playhead samples the canonical player, without global high-rate state.
 */
@Composable
fun WaveformTimeline(
    modifier: Modifier = Modifier,
    progress: Float,
    isPlaying: Boolean,
    artworkColor: Color,
    waveformData: WaveformData? = null,
    onSeekStart: () -> Unit = {},
    onSeekChange: (Float) -> Unit = {},
    onSeekEnd: (Float) -> Unit = {},
    height: Dp = 56.dp,
    isVisible: () -> Boolean = { true },
    artworkSecondary: Color = artworkColor,
    trackId: Long? = null,
    enabled: Boolean = true,
    onSeekCancel: () -> Unit = {},
    sampleProgress: (() -> Float)? = null,
    sampleAmplitude: (() -> Float)? = null
) {
    val inset = with(LocalDensity.current) { 12.dp.toPx() }
    val owner = LocalLifecycleOwner.current
    var foreground by remember(owner) { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, _ -> foreground = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    var dragging by remember(trackId) { mutableStateOf(false) }
    var dragFraction by remember(trackId) { mutableFloatStateOf(0f) }
    val currentProgress by rememberUpdatedState(progress)
    val playing by rememberUpdatedState(isPlaying)
    val visible by rememberUpdatedState(isVisible)
    val sampler by rememberUpdatedState(sampleProgress)
    val audioSampler by rememberUpdatedState(sampleAmplitude)
    val currentWaveform by rememberUpdatedState(waveformData)
    val flow = remember(trackId) { AudioRibbonFlow() }
    val frameTick = remember(trackId) { mutableFloatStateOf(0f) }
    val flowPaths = remember(trackId) { Array(3) { Path() } }
    val seekStart by rememberUpdatedState(onSeekStart)
    val seekChange by rememberUpdatedState(onSeekChange)
    val seekEnd by rememberUpdatedState(onSeekEnd)
    val seekCancel by rememberUpdatedState(onSeekCancel)
    val drawProgress = remember(trackId) { mutableFloatStateOf(FluidTimelineGeometry.safeFraction(progress)) }
    var active by remember { mutableStateOf(false) }
    LaunchedEffect(owner) {
        snapshotFlow { foreground && visible() }.collectLatest { active = it }
    }
    LaunchedEffect(trackId, active, enabled) {
        snapshotFlow { active && enabled && playing && !dragging }.collectLatest { animate ->
            if (animate) {
                var previous = 0L
                var lastProgress = drawProgress.floatValue
                while (true) withFrameNanos { now ->
                    val fraction = FluidTimelineGeometry.safeFraction(sampler?.invoke() ?: currentProgress)
                    if (kotlin.math.abs(fraction - lastProgress) > 0.04f) flow.reset()
                    lastProgress = fraction
                    val dt = if (previous == 0L) 0f else ((now - previous) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    previous = now
                    val data = currentWaveform
                    val energy = audioSampler?.invoke() ?: data?.let {
                        it.rmsAt((fraction * (it.bucketCount - 1)).toInt())
                    } ?: 0f
                    if (RikoMotion.motionScale > 0f && audioSampler != null) flow.advance(dt, energy)
                    drawProgress.floatValue = fraction
                    frameTick.floatValue += dt
                }
            }
        }
    }
    LaunchedEffect(progress, isPlaying, trackId, active) {
        if (!dragging) drawProgress.floatValue = FluidTimelineGeometry.safeFraction(sampler?.invoke() ?: progress)
    }
    val tones = remember(artworkColor, artworkSecondary) { FluidTimelinePalette.forArtwork(artworkColor, artworkSecondary) }
    val animate = active && RikoMotion.motionScale > 0f
    val body = animateColorAsState(tones.body, if (animate) tween(420) else snap(), label = "timelineBody")
    val depth = animateColorAsState(tones.depth, if (animate) tween(420) else snap(), label = "timelineDepth")
    val highlight = animateColorAsState(tones.highlight, if (animate) tween(420) else snap(), label = "timelineHighlight")
    val headColor = animateColorAsState(tones.head, if (animate) tween(420) else snap(), label = "timelineHead")
    val glowGain = animateFloatAsState(if (isPlaying) 1f else 0.35f,
        if (animate) tween(180) else snap(), label = "timelineGlow")
    val envelope = remember(waveformData) { waveformData?.let { LiquidEnvelope.from(it) } }
    // Palette animation may refresh brushes, but this holder only rebuilds
    // curves for changed data/size/density. It never reads progress state.
    val pathCache = remember(envelope) { TimelinePathCache(envelope) }

    Box(modifier.fillMaxWidth().height(maxOf(height, 48.dp))
        .semantics {
            contentDescription = "Playback timeline"
            progressBarRangeInfo = ProgressBarRangeInfo(FluidTimelineGeometry.safeFraction(currentProgress), 0f..1f)
            if (!enabled) disabled()
            setProgress { value ->
                if (!enabled || !value.isFinite()) false else {
                    val fraction = FluidTimelineGeometry.safeFraction(value)
                    flow.reset()
                    seekStart(); seekChange(fraction); seekEnd(fraction)
                    true
                }
            }
        }
        .pointerInput(inset, trackId, enabled) {
            if (enabled) detectTapGestures { point ->
                val fraction = FluidTimelineGeometry.fraction(point.x, size.width.toFloat(), inset)
                flow.reset()
                seekStart(); seekChange(fraction); seekEnd(fraction)
                drawProgress.floatValue = fraction
            }
        }
        .pointerInput(inset, trackId, enabled) {
            if (enabled) detectHorizontalDragGestures(
                onDragStart = { point ->
                    dragging = true
                    dragFraction = FluidTimelineGeometry.fraction(point.x, size.width.toFloat(), inset)
                    seekStart(); seekChange(dragFraction)
                },
                onDragEnd = {
                    drawProgress.floatValue = dragFraction
                    flow.reset()
                    seekEnd(dragFraction)
                    dragging = false
                },
                onDragCancel = { dragging = false; seekCancel() },
                onHorizontalDrag = { change, _ ->
                    change.consume()
                    dragFraction = FluidTimelineGeometry.fraction(change.position.x, size.width.toFloat(), inset)
                    seekChange(dragFraction)
                }
            )
        }) {
        Box(Modifier.fillMaxSize().drawWithCache {
            pathCache.prepare(size.width, size.height, density)
            val baseline = size.height * 0.58f
            val radius = FluidTimelineGeometry.HEAD_RADIUS_DP.dp.toPx()
            val railEnd = size.width - inset
            val amplitude = minOf(24.dp.toPx(), size.height * 0.44f)
            val a = body.value
            val b = depth.value
            val h = highlight.value
            val light = headColor.value
            val bodyBrush = Brush.linearGradient(listOf(a.copy(alpha = 0.76f), b.copy(alpha = 0.52f), a.copy(alpha = 0.64f)),
                Offset(inset, baseline - amplitude), Offset(railEnd, baseline))
            val depthBrush = Brush.verticalGradient(listOf(b.copy(alpha = 0.57f), b.copy(alpha = 0.16f)),
                baseline - amplitude, baseline)
            val crestBrush = Brush.verticalGradient(listOf(h.copy(alpha = 0.38f), h.copy(alpha = 0.06f)),
                baseline - amplitude, baseline)
            val railBrush = Brush.horizontalGradient(listOf(lerp(a, Color.White, 0.70f), lerp(b, Color.White, 0.86f)), inset, railEnd)
            // Centred at zero and translated during drawing: no moving shader allocations.
            val haloRadius = radius * 2.8f
            val halo = Brush.radialGradient(0f to light.copy(alpha = 0.36f),
                0.35f to light.copy(alpha = 0.20f), 0.70f to light.copy(alpha = 0.05f),
                1f to Color.Transparent, center = Offset.Zero, radius = haloRadius)
            val outline = Stroke(1.55.dp.toPx())
            val contrastOutline = Stroke(2.2.dp.toPx())
            val taper = maxOf(10.dp.toPx(), (railEnd - inset) * 0.045f)
            onDrawBehind {
                if (railEnd <= inset) return@onDrawBehind
                frameTick.floatValue
                val fraction = if (dragging) dragFraction else drawProgress.floatValue
                val x = FluidTimelineGeometry.head(fraction, size.width, inset)
                // Neutral rail exists continuously under the circle; no dry gap.
                drawLine(Color.Black.copy(alpha = 0.14f), Offset(inset, baseline), Offset(railEnd, baseline), 2.8.dp.toPx(), StrokeCap.Round)
                drawLine(Color.White.copy(alpha = 0.28f), Offset(inset, baseline), Offset(railEnd, baseline), 1.5.dp.toPx(), StrokeCap.Round)
                if (x > inset) {
                    clipRect(left = inset, right = x) {
                        fun ribbon(path: LiquidRibbonPath?, brush: Brush) {
                            if (path == null) return
                            path.updateHead(x, taper)
                            if (path.seam > inset) clipRect(right = path.seam) { drawPath(path.full, brush) }
                            drawPath(path.tail, brush)
                        }
                        // During playback the foreground is actual-PCM-driven
                        // travelling membranes, independent of offline analysis readiness.
                        // Scrubbing retains the stable full-song audio envelope.
                        if (flow.hasHistory && !dragging) {
                            val span = x - inset
                            for (layer in 0..2) {
                                val path = flowPaths[layer]
                                path.reset(); path.moveTo(inset, baseline)
                                // Smoothstep Bezier segments stay within adjacent heights.
                                var px = inset
                                var py = baseline
                                for (i in 1..64) {
                                    val u = i / 64f
                                    val nx = inset + span * u
                                    val ny = baseline - flow.height(layer, u) * amplitude
                                    val dx = (nx - px) / 3f
                                    path.cubicTo(px + dx, py, nx - dx, ny, nx, ny)
                                    px = nx; py = ny
                                }
                                path.lineTo(x, baseline); path.close()
                                drawPath(path, when (layer) { 0 -> depthBrush; 1 -> bodyBrush; else -> crestBrush })
                            }
                        } else if (audioSampler == null || dragging || !isPlaying || RikoMotion.motionScale <= 0f) {
                            ribbon(pathCache.depth, depthBrush)
                            ribbon(pathCache.body, bodyBrush)
                            ribbon(pathCache.crest, crestBrush)
                        }
                        drawLine(railBrush, Offset(inset, baseline), Offset(x, baseline), 2.dp.toPx(), StrokeCap.Round)
                    }
                }
                val data = waveformData
                val energy = if (data == null) 0f else data.rmsAt((fraction * (data.bucketCount - 1)).toInt().coerceIn(0, data.bucketCount - 1))
                // The halo is localized; PCM drives the foreground independently.
                translate(left = x, top = baseline) {
                    drawCircle(halo, haloRadius, Offset.Zero, alpha = (0.50f + energy * 0.50f) * glowGain.value)
                    drawCircle(light.copy(alpha = 0.34f), radius, Offset.Zero)
                    drawCircle(Color.Black.copy(alpha = 0.20f), radius, Offset.Zero, style = contrastOutline)
                    drawCircle(Color.White.copy(alpha = 0.96f), radius, Offset.Zero, style = outline)
                }
            }
        })
    }
}

private class TimelinePathCache(private val envelope: LiquidEnvelope?) {
    private var dimensions = Triple(0f, 0f, 0f)
    var depth: LiquidRibbonPath? = null; private set
    var body: LiquidRibbonPath? = null; private set
    var crest: LiquidRibbonPath? = null; private set
    fun prepare(width: Float, height: Float, density: Float) {
        if (dimensions.first == width && dimensions.second == height && dimensions.third == density) return
        dimensions = Triple(width, height, density)
        val data = envelope ?: return
        val inset = 12f * density
        val span = width - 2f * inset
        if (span <= 0f) return
        val amplitude = minOf(FluidTimelineGeometry.MAX_AMPLITUDE_DP * density, height * 0.44f)
        body = LiquidRibbonPath(data.body, inset, span, height * 0.58f, amplitude)
        depth = LiquidRibbonPath(data.depth, inset, span, height * 0.58f, amplitude * 0.95f)
        crest = LiquidRibbonPath(data.crest, inset, span, height * 0.58f, amplitude * 0.88f)
    }
}
