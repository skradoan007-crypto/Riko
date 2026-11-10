package com.rikodev.music.android.core.experience.wave.render

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — WAVE PREVIEW SCREEN
//
// A standalone tuning lab for the Wave Engine and Renderer.
//
// This screen allows tweaking physics parameters WITHOUT playing music.
// Sliders control energy, ribbon, and highlight values directly.
// The Wave Engine processes these synthetic inputs and the renderers
// display the result in real-time.
//
// ── PURPOSE ────────────────────────────────────────────────────────────────
//   1. Visual tuning: Adjust ribbon thickness, opacity, glow intensity, etc.
//   2. Quality testing: Switch between LOW/BALANCED/HIGH/ULTRA and compare.
//   3. Color testing: Preview with different ColorState palettes.
//   4. Performance profiling: Measure draw time at each quality level.
//   5. Edge case testing: Extreme values, rapid transitions, mode changes.
//
// ── SLIDERS ───────────────────────────────────────────────────────────────
//   Energy Group:     Bass, Mid, Treble, Overall Energy, Beat Confidence
//   Ribbon Group:     Thickness, Opacity, Pressure, Momentum, Expansion
//   Highlight Group:  Specular, Fresnel, Glass, Edge Glow, Glow
//   Motion Group:     Velocity, Offset
//   Quality:          LOW / BALANCED / HIGH / ULTRA toggle
//
// ── ARCHITECTURE ───────────────────────────────────────────────────────────
//   This screen creates SYNTHETIC WaveState directly (bypassing the engine).
//   In production, WaveState comes from WaveEngine.state.
//   The preview screen manually constructs WaveState from slider values.
//
//   WHY bypass the engine?
//   - The engine needs real AudioState (from ExoPlayer).
//   - The preview screen needs to work without music.
//   - Sliders map directly to WaveEnergy/RibbonState/HighlightState fields.
//   - The renderers don't care WHERE the state comes from — they just draw.
//
// PRODUCER: Phase 3.4H
// CONSUMER: Developer / Designer (debug/tuning tool, not end-user facing)
//
// ⭐ NOTE
//   This is a DEVELOPER TOOL, not a user-facing feature.
//   It should be gated behind a debug flag or hidden in a settings menu.
// ─────────────────────────────────────────────────────────────────────────────

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.music.android.core.experience.ColorState
import com.rikodev.music.android.core.experience.wave.BezierControl
import com.rikodev.music.android.core.experience.wave.HighlightPoint
import com.rikodev.music.android.core.experience.wave.HighlightState
import com.rikodev.music.android.core.experience.wave.OrganicPath
import com.rikodev.music.android.core.experience.wave.RibbonMode
import com.rikodev.music.android.core.experience.wave.RibbonPoint
import com.rikodev.music.android.core.experience.wave.RibbonState
import com.rikodev.music.android.core.experience.wave.WaveEnergy
import com.rikodev.music.android.core.experience.wave.WaveQuality
import com.rikodev.music.android.core.experience.wave.WaveState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.foundation.Canvas

/**
 * The Wave Preview Screen — a physics tuning lab.
 *
 * Provides sliders for every major parameter and renders the ribbon
 * in real-time using the same render pipeline as the player.
 */
@Composable
fun WavePreviewScreen(
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    // ── Slider State ─────────────────────────────────────────────────────

    // Energy sliders
    var bass by remember { mutableFloatStateOf(0.3f) }
    var mid by remember { mutableFloatStateOf(0.4f) }
    var treble by remember { mutableFloatStateOf(0.2f) }
    var overallEnergy by remember { mutableFloatStateOf(0.5f) }
    var beatConfidence by remember { mutableFloatStateOf(0f) }
    var momentum by remember { mutableFloatStateOf(0.3f) }
    var expansion by remember { mutableFloatStateOf(0.2f) }

    // Ribbon sliders
    var thickness by remember { mutableFloatStateOf(0.4f) }
    var opacity by remember { mutableFloatStateOf(0.6f) }
    var pressure by remember { mutableFloatStateOf(0.3f) }
    var elasticity by remember { mutableFloatStateOf(0.5f) }
    var viscosity by remember { mutableFloatStateOf(0.5f) }

    // Highlight sliders
    var specular by remember { mutableFloatStateOf(0.3f) }
    var fresnel by remember { mutableFloatStateOf(0.4f) }
    var glassOpacity by remember { mutableFloatStateOf(0.2f) }
    var edgeGlow by remember { mutableFloatStateOf(0.3f) }
    var glowIntensity by remember { mutableFloatStateOf(0.4f) }

    // Motion sliders
    var velocity by remember { mutableFloatStateOf(0f) }
    var offset by remember { mutableFloatStateOf(0f) }

    // Quality
    var quality by remember { mutableStateOf(WaveQuality.BALANCED) }

    // Debug overlay toggle
    var showOverlay by remember { mutableStateOf(true) }

    // ── Build Synthetic WaveState ─────────────────────────────────────────

    val state = remember(
        bass, mid, treble, overallEnergy, beatConfidence, momentum, expansion,
        thickness, opacity, pressure, elasticity, viscosity,
        specular, fresnel, glassOpacity, edgeGlow, glowIntensity,
        velocity, offset, quality
    ) {
        buildSyntheticState(
            bass = bass, mid = mid, treble = treble,
            overallEnergy = overallEnergy, beatConfidence = beatConfidence,
            momentum = momentum, expansion = expansion,
            thickness = thickness, opacity = opacity, pressure = pressure,
            elasticity = elasticity, viscosity = viscosity,
            specular = specular, fresnel = fresnel,
            glassOpacity = glassOpacity, edgeGlow = edgeGlow,
            glowIntensity = glowIntensity,
            velocity = velocity, offset = offset,
            quality = quality
        )
    }

    val stateFlow = remember(state) { MutableStateFlow(state) }

    // Default color state for preview
    val previewColor = remember {
        MutableStateFlow(
            ColorState(
                primary = Color(0xFF19B7FF),
                secondary = Color(0xFF9A6CFF),
                accent = Color(0xFFFF6FD8),
                waveStart = Color(0xFF19B7FF),
                waveEnd = Color(0xFF9A6CFF),
                ambientGlow = Color(0x3319B7FF),
                isFromArtwork = true,
                energyIntensity = 0.5f
            )
        )
    }

    // Inspector overlay
    val overlay = remember(textMeasurer) {
        InspectorOverlay(textMeasurer).apply {
            isEnabled = showOverlay
            qualityName = quality.name
        }
    }

    // ── Layout ─────────────────────────────────────────────────────────────

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF09111D))
    ) {
        // Left panel: Canvas preview
        Box(
            modifier = Modifier
                .weight(2f)
                .fillMaxSize()
        ) {
            WaveCanvas(
                waveState = stateFlow,
                colorState = previewColor,
                modifier = Modifier.fillMaxSize(),
                debugOverlay = if (showOverlay) overlay else null
            )
        }

        // Right panel: Sliders
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // ── Energy Group ──────────────────────────────────────────
            SectionHeader("── ENERGY ──")
            SliderRow("Bass", bass) { bass = it }
            SliderRow("Mid", mid) { mid = it }
            SliderRow("Treble", treble) { treble = it }
            SliderRow("Overall", overallEnergy) { overallEnergy = it }
            SliderRow("Beat", beatConfidence) { beatConfidence = it }
            SliderRow("Momentum", momentum) { momentum = it }
            SliderRow("Expansion", expansion) { expansion = it }

            Spacer(Modifier.height(8.dp))

            // ── Ribbon Group ──────────────────────────────────────────
            SectionHeader("── RIBBON ──")
            SliderRow("Thickness", thickness) { thickness = it }
            SliderRow("Opacity", opacity) { opacity = it }
            SliderRow("Pressure", pressure) { pressure = it }
            SliderRow("Elasticity", elasticity) { elasticity = it }
            SliderRow("Viscosity", viscosity) { viscosity = it }

            Spacer(Modifier.height(8.dp))

            // ── Highlight Group ────────────────────────────────────────
            SectionHeader("── HIGHLIGHT ──")
            SliderRow("Specular", specular) { specular = it }
            SliderRow("Fresnel", fresnel) { fresnel = it }
            SliderRow("Glass", glassOpacity) { glassOpacity = it }
            SliderRow("Edge Glow", edgeGlow) { edgeGlow = it }
            SliderRow("Glow", glowIntensity) { glowIntensity = it }

            Spacer(Modifier.height(8.dp))

            // ── Motion Group ──────────────────────────────────────────
            SectionHeader("── MOTION ──")
            SliderRow("Velocity", velocity) { velocity = it }
            SliderRow("Offset", offset) { offset = it }

            Spacer(Modifier.height(8.dp))

            // ── Quality Toggle ────────────────────────────────────────
            SectionHeader("── QUALITY ──")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                WaveQuality.entries.forEach { q ->
                    TextButton(
                        onClick = { quality = q },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = q.name,
                            color = if (quality == q) Color(0xFF19B7FF)
                            else Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Overlay Toggle ─────────────────────────────────────────
            TextButton(onClick = { showOverlay = !showOverlay }) {
                Text(
                    text = if (showOverlay) "Hide Overlay" else "Show Overlay",
                    color = Color(0xFF888888),
                    fontSize = 11.sp
                )
            }
        }
    }
}

// ── Helper Composables ───────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF4488FF),
        fontSize = 11.sp,
        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFF888888),
            fontSize = 9.sp,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            modifier = Modifier.width(60.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF19B7FF),
                activeTrackColor = Color(0xFF19B7FF),
                inactiveTrackColor = Color(0xFF333333)
            )
        )
        Text(
            text = "%.2f".format(value),
            color = Color(0xFF00FF88),
            fontSize = 9.sp,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            modifier = Modifier.width(40.dp)
        )
    }
}

// ── Synthetic State Builder ─────────────────────────────────────────────────

/**
 * Build a synthetic WaveState from slider values.
 * This bypasses the Wave Engine entirely — the renderers don't care.
 */
private fun buildSyntheticState(
    bass: Float,
    mid: Float,
    treble: Float,
    overallEnergy: Float,
    beatConfidence: Float,
    momentum: Float,
    expansion: Float,
    thickness: Float,
    opacity: Float,
    pressure: Float,
    elasticity: Float,
    viscosity: Float,
    specular: Float,
    fresnel: Float,
    glassOpacity: Float,
    edgeGlow: Float,
    glowIntensity: Float,
    velocity: Float,
    offset: Float,
    quality: WaveQuality
): WaveState {
    val segments = quality.segmentCount
    val pointCount = segments + 1

    // Build synthetic ribbon points (a smooth sine-like curve)
    val points = (0 until pointCount).map { i ->
        val t = i.toFloat() / segments
        val baseY = offset + kotlin.math.sin(t * kotlin.math.PI.toFloat() * 2f + velocity * 3f) * thickness * 0.5f
        val localThickness = thickness * (1f - kotlin.math.abs(t - 0.5f) * 0.6f) // ink brush profile
        val localOpacity = opacity * (1f - kotlin.math.abs(t - 0.5f) * 0.4f)
        val curvature = kotlin.math.abs(kotlin.math.cos(t * kotlin.math.PI.toFloat() * 2f + velocity * 3f)) * overallEnergy

        RibbonPoint(
            x = t,
            y = baseY.coerceIn(-1f, 1f),
            normalX = 0f,
            normalY = -1f,
            thickness = localThickness.coerceIn(0f, 1f),
            opacity = localOpacity.coerceIn(0f, 1f),
            curvature = curvature.coerceIn(0f, 1f),
            pressure = pressure,
            velocity = velocity,
            highlightIntensity = specular * curvature,
            reflectionFactor = fresnel * 0.5f,
            edgeAlpha = edgeGlow
        )
    }

    // Build synthetic bezier controls (smooth Catmull-Rom approximation)
    val controls = (0 until segments).map { i ->
        val p0 = points[maxOf(0, i - 1)]
        val p1 = points[i]
        val p2 = points[minOf(pointCount - 1, i + 1)]
        val p3 = points[minOf(pointCount - 1, i + 2)]

        val tension = 0.5f
        BezierControl(
            cp1X = p1.x + (p2.x - p0.x) * tension / 3f,
            cp1Y = p1.y + (p2.y - p0.y) * tension / 3f,
            cp2X = p2.x - (p3.x - p1.x) * tension / 3f,
            cp2Y = p2.y - (p3.y - p1.y) * tension / 3f,
            tension = tension
        )
    }

    // Build synthetic highlight points
    val highlightPoints = points.map { p ->
        HighlightPoint(
            specularIntensity = specular * p.curvature,
            edgeGlow = edgeGlow * p.curvature,
            rimLight = fresnel * (1f - p.opacity) * 0.5f,
            reflectionAlpha = fresnel * 0.3f,
            glassOpacity = glassOpacity * p.opacity,
            fresnel = fresnel,
            shadowStrength = thickness * 0.3f,
            highlightWidth = specular * 0.6f,
            lightDirectionX = kotlin.math.sin((velocity * 2f).toDouble()).toFloat() * 0.3f,
            lightDirectionY = -1f
        )
    }

    val path = OrganicPath(
        points = points,
        controls = controls,
        totalLength = segments.toFloat(),
        averageCurvature = points.map { it.curvature }.average().toFloat(),
        quality = quality,
        timestamp = System.nanoTime()
    )

    val highlight = HighlightState(
        points = highlightPoints,
        specularIntensity = specular,
        edgeGlow = edgeGlow,
        rimLight = fresnel * 0.3f,
        reflectionAlpha = fresnel * 0.3f,
        glassOpacity = glassOpacity,
        fresnel = fresnel,
        shadowStrength = thickness * 0.3f,
        highlightWidth = specular * 0.6f,
        viewAngle = 0.7f,
        surfaceNormal = 1f,
        fresnelPower = 3f + overallEnergy * 2f,
        lightDirection = kotlin.math.sin((velocity * 2f).toDouble()).toFloat() * 0.3f,
        timestamp = System.nanoTime()
    )

    val mode = when {
        overallEnergy > 0.7f -> RibbonMode.EXPANDING
        overallEnergy > 0.4f -> RibbonMode.ACTIVE
        overallEnergy > 0.1f -> RibbonMode.BREATHING
        else -> RibbonMode.IDLE
    }

    return WaveState(
        energy = WaveEnergy(
            overallEnergy = overallEnergy,
            bassPressure = bass,
            midTexture = mid,
            trebleSpark = treble,
            momentum = momentum,
            expansion = expansion,
            beatConfidence = beatConfidence,
            attack = overallEnergy * 0.7f,
            release = overallEnergy * 0.3f,
            pressure = pressure
        ),
        ribbon = RibbonState(
            thickness = thickness,
            opacity = opacity.coerceAtLeast(0.12f),
            pressure = pressure,
            elasticity = elasticity,
            viscosity = viscosity,
            stretch = 1f + expansion * 0.3f,
            highlightIntensity = specular,
            relaxation = 1f - viscosity * 0.5f
        ),
        mode = mode,
        isActive = overallEnergy > 0.01f,
        isVisible = true,
        path = path,
        highlight = highlight,
        timestamp = System.nanoTime()
    )
}
