package com.rikodev.music.android.presentation.debug

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.rikodev.music.android.core.experience.ColorState
import com.rikodev.music.android.core.experience.calibration.ExperiencePreset
import com.rikodev.music.android.core.experience.calibration.ExperienceProfile
import com.rikodev.music.android.core.experience.calibration.ExperienceProfiles
import com.rikodev.music.android.core.experience.calibration.WaveCalibration
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
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.pow
import androidx.compose.ui.graphics.Path
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — WAVE LABORATORY VIEW MODEL
//
// The brain of the Wave Laboratory — Phase 3.4J.2.
//
// Manages all state for the physics tuning console:
//   - Active experience profile (CLASSIC/FLUID/ELASTIC/LIQUID_GLASS/EXPERIMENTAL)
//   - Synthetic energy generation (simulates genres without real audio)
//   - Real-time slider overrides for WaveCalibration values
//   - Snapshot system (save/load/export/import JSON)
//   - Session notes (developer calibration log)
//   - Experience scores (developer-rated feel metrics)
//   - Performance metrics
//
// ARCHITECTURE:
//   This ViewModel generates synthetic WaveState for WaveCanvas.
//   It reads FROM WaveCalibration (the Physics BIOS) for all physics values.
//   When WaveCalibration values change (profile or slider), the NEXT FRAME
//   automatically uses the new values. This is the "Live" behavior.
//
//   Flow:
//     Profile selected → WaveCalibration.applyPreset(preset)
//     Slider moved      → WaveCalibration.Physics.X = sliderValue
//     Animation frame   → generateState(time) reads WaveCalibration
//     WaveCanvas        → renders the state
//
// THREADING:
//   All mutations happen on main thread (Compose).
//   The animation loop runs on main thread via withFrameNanos.
//
// "The ribbon feels different for all five presets, not just numerically different."
// — Wave Laboratory Exit Criteria
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Represents a test genre for synthetic energy generation.
 * Each genre has a distinct energy fingerprint that the ribbon should respond to.
 */
enum class TestGenre(
    val displayName: String,
    /** Bass intensity weight (0-1). */
    val bassWeight: Float,
    /** Mid intensity weight (0-1). */
    val midWeight: Float,
    /** Treble intensity weight (0-1). */
    val trebleWeight: Float,
    /** Beat frequency multiplier (higher = faster beats). */
    val beatFreq: Float,
    /** Beat sharpness (how short each beat pulse is). */
    val beatSharpness: Float,
    /** Overall energy scale (0-1). */
    val energyScale: Float
) {
    AMBIENT("Ambient", 0.2f, 0.6f, 0.3f, 0.3f, 0.4f, 0.3f),
    ROCK("Rock", 0.7f, 0.6f, 0.5f, 2.0f, 0.7f, 0.7f),
    EDM("EDM", 0.9f, 0.5f, 0.6f, 4.0f, 0.9f, 0.85f),
    POP("Pop", 0.5f, 0.7f, 0.5f, 2.5f, 0.6f, 0.6f),
    PHONK("Phonk", 0.95f, 0.4f, 0.3f, 1.5f, 0.85f, 0.75f),
    CLASSICAL("Classical", 0.3f, 0.7f, 0.6f, 0.5f, 0.5f, 0.5f),
    JAZZ("Jazz", 0.4f, 0.8f, 0.5f, 1.2f, 0.45f, 0.45f),
    HIPHOP("Hip-Hop", 0.85f, 0.5f, 0.4f, 2.0f, 0.75f, 0.7f)
}

/**
 * Performance metrics captured from the render loop.
 */
data class LabPerformance(
    val fps: Int = 0,
    val frameTimeMs: Float = 0f,
    val memoryMB: Float = 0f,
    val gcCount: Int = 0,
    val drawCalls: Int = 0,
    val bezierCount: Int = 0,
    val pointCount: Int = 0
)

class WaveLaboratoryViewModel {

    // ═══════════════════════════════════════════════════════════════════════
    // PROFILE STATE
    // ═══════════════════════════════════════════════════════════════════════

    /** Currently selected profile. */
    var activeProfile by mutableStateOf(ExperienceProfile.CLASSIC)
        private set

    /** Whether the inspector overlay is visible. */
    var showInspector by mutableStateOf(true)

    /** Whether the hidden developer panel is visible. */
    var showDevPanel by mutableStateOf(false)

    /** Current quality level. */
    var quality by mutableStateOf(WaveQuality.BALANCED)

    /** Selected test genre for synthetic energy. */
    var selectedGenre by mutableStateOf(TestGenre.EDM)

    // ═══════════════════════════════════════════════════════════════════════
    // WAVE STATE OUTPUT — consumed by WaveCanvas
    // ═══════════════════════════════════════════════════════════════════════

    private val _waveState = MutableStateFlow(WaveState.IDLE)
    val waveState: StateFlow<WaveState> = _waveState.asStateFlow()

    private val _colorState = MutableStateFlow(defaultColorState())
    val colorState: StateFlow<ColorState> = _colorState.asStateFlow()

    // ═══════════════════════════════════════════════════════════════════════
    // PERFORMANCE METRICS
    // ═══════════════════════════════════════════════════════════════════════

    var performance by mutableStateOf(LabPerformance())
        private set

    private var lastFrameTimeNs = 0L
    private var frameCount = 0
    private var fpsAccumulator = 0L
    private var currentFps = 0

    // ═══════════════════════════════════════════════════════════════════════
    // SNAPSHOT STATE
    // ═══════════════════════════════════════════════════════════════════════

    /** Saved snapshots (name → JSON string). */
    var savedSnapshots by mutableStateOf(mapOf<String, String>())
        private set

    // ═══════════════════════════════════════════════════════════════════════
    // EXPERIENCE SCORES — developer-rated
    // ═══════════════════════════════════════════════════════════════════════

    var scoreSmoothness by mutableFloatStateOf(0f)
    var scoreFluidity by mutableFloatStateOf(0f)
    var scoreGlassFeel by mutableFloatStateOf(0f)
    var scorePerformance by mutableFloatStateOf(0f)

    val scoreOverall: Float
        get() = when {
            scoreSmoothness + scoreFluidity + scoreGlassFeel + scorePerformance == 0f -> 0f
            else -> (scoreSmoothness + scoreFluidity + scoreGlassFeel + scorePerformance) / 4f
        }

    // ═══════════════════════════════════════════════════════════════════════
    // SESSION NOTES
    // ═══════════════════════════════════════════════════════════════════════

    var sessionNotes by mutableStateOf(
        mapOf(
            "EDM" to "",
            "Rock" to "",
            "Jazz" to "",
            "Phonk" to "",
            "Classical" to "",
            "Pop" to "",
            "Ambient" to "",
            "Hip-Hop" to ""
        )
    )

    // ═══════════════════════════════════════════════════════════════════════
    // PANEL EXPANSION STATE
    // ═══════════════════════════════════════════════════════════════════════

    var physicsPanelExpanded by mutableStateOf(true)
    var motionPanelExpanded by mutableStateOf(false)
    var renderPanelExpanded by mutableStateOf(false)
    var performancePanelExpanded by mutableStateOf(false)

    // ═══════════════════════════════════════════════════════════════════════
    // CUSTOM SLIDER OVERRIDES
    // ═══════════════════════════════════════════════════════════════════════

    // These track which sliders the user has manually adjusted.
    // When a slider is moved, it writes directly to WaveCalibration.
    // When a profile is applied, all sliders reset to profile defaults.

    /** Whether sliders are in "editable" mode (user can drag) or "read-only" (profile values). */
    var isEditable by mutableStateOf(true)

    // ═══════════════════════════════════════════════════════════════════════
    // ACTIONS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Switch to a different experience profile.
     * Calls WaveCalibration.applyPreset() — entire engine personality changes.
     */
    fun selectProfile(profile: ExperienceProfile) {
        activeProfile = profile
        val preset = ExperienceProfiles.fromProfile(profile)
        WaveCalibration.applyPreset(preset)
    }

    /**
     * Reset to CLASSIC profile and defaults.
     */
    fun resetCalibration() {
        activeProfile = ExperienceProfile.CLASSIC
        WaveCalibration.reset()
    }

    /**
     * Update the animation frame. Called by the render loop.
     *
     * @param timeNanos Current frame time in nanoseconds (from withFrameNanos).
     */
    fun updateFrame(timeNanos: Long) {
        // Track FPS
        if (lastFrameTimeNs > 0) {
            val delta = timeNanos - lastFrameTimeNs
            fpsAccumulator += delta
            frameCount++
            if (fpsAccumulator >= 1_000_000_000L) {
                currentFps = frameCount
                frameCount = 0
                fpsAccumulator = 0L
            }
            performance = performance.copy(
                fps = currentFps,
                frameTimeMs = delta / 1_000_000f
            )
        }
        lastFrameTimeNs = timeNanos

        // Generate synthetic state
        val timeSec = timeNanos / 1_000_000_000f
        val state = generateSyntheticState(timeSec)
        _waveState.value = state
    }

    /**
     * Save a snapshot with the given name.
     * Serializes the current WaveCalibration values to JSON.
     */
    fun saveSnapshot(name: String) {
        val preset = buildCurrentPreset(name)
        val json = preset.toSnapshot().toString()
        savedSnapshots = savedSnapshots + (name to json)
    }

    /**
     * Load a snapshot by name.
     * Restores WaveCalibration values from the saved JSON.
     */
    fun loadSnapshot(name: String): Boolean {
        val json = savedSnapshots[name] ?: return false
        // Simple parse — reconstruct map from string
        return try {
            val clean = json.removePrefix("{").removeSuffix("}")
            val pairs = clean.split(",").map { it.trim() }
            val map = mutableMapOf<String, Any>()
            for (pair in pairs) {
                val (key, value) = pair.split("=")
                val v = value.trim()
                map[key.trim()] = when {
                    v.startsWith("\"") -> v.removeSurrounding("\"")
                    v.contains(".") -> v.toFloat()
                    else -> v.toInt()
                }
            }
            val preset = ExperiencePreset.fromSnapshot(map) ?: return false
            WaveCalibration.applyPreset(preset)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Export the current preset as a JSON string.
     * For clipboard sharing or file export.
     */
    fun exportPreset(): String {
        val preset = buildCurrentPreset("Custom")
        return preset.toSnapshot().entries.joinToString(",") { (k, v) ->
            "$k=$v"
        }
    }

    /**
     * Import a preset from a JSON string.
     */
    fun importPreset(json: String): Boolean {
        return try {
            val clean = json.removePrefix("{").removeSuffix("}")
            val pairs = clean.split(",").map { it.trim() }
            val map = mutableMapOf<String, Any>()
            for (pair in pairs) {
                val (key, value) = pair.split("=")
                map[key.trim()] = when {
                    value.trim().startsWith("\"") -> value.trim().removeSurrounding("\"")
                    value.trim().contains(".") -> value.trim().toFloat()
                    else -> value.trim().toInt()
                }
            }
            val preset = ExperiencePreset.fromSnapshot(map) ?: return false
            WaveCalibration.applyPreset(preset)
            activeProfile = preset.profile
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Update session notes for a specific genre.
     */
    fun updateNotes(genre: String, notes: String) {
        sessionNotes = sessionNotes + (genre to notes)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SYNTHETIC ENERGY GENERATION
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Generate synthetic energy patterns based on the selected genre.
     *
     * This simulates audio energy WITHOUT real audio.
     * Uses layered sine waves + beat pulses at genre-specific frequencies.
     *
     * The generated energy is then processed using WaveCalibration values,
     * so changing the BIOS affects the output in real-time.
     */
    private fun generateSyntheticState(timeSec: Float): WaveState {
        val genre = selectedGenre
        
        // ── Synthetic energy composition ──────────────────────────────
        val beatPhase = (timeSec * genre.beatFreq * 2f * PI.toFloat())
        val beatPulse = max(0f, sin(beatPhase)).pow(genre.beatSharpness * 3f)

        // Organic bass: slow sine + beat
        val bassBase = (sin(timeSec * 1.2f) * 0.5f + 0.5f) * genre.bassWeight
        val bass = (bassBase * 0.4f + beatPulse * 0.6f) * genre.energyScale

        // Mid: moderate oscillation
        val mid = ((sin(timeSec * 2.7f + 1f) * 0.5f + 0.5f) *
                (sin(timeSec * 4.1f) * 0.3f + 0.7f)) * genre.midWeight * genre.energyScale

        // Treble: fast shimmer
        val treble = ((sin(timeSec * 6.3f + 2f) * 0.5f + 0.5f) *
                (sin(timeSec * 11.1f) * 0.2f + 0.8f)) * genre.trebleWeight * genre.energyScale

        // Overall energy
        val overall = (bass * WaveCalibration.Energy.BASS_PRESSURE_BASS_W +
                mid * WaveCalibration.Energy.MID_TEXTURE_MID_W +
                treble * WaveCalibration.Energy.TREBLE_SPARK_TREBLE_W).coerceIn(0f, 1f)

        // Momentum simulation
        val momentum = (beatPulse * 0.6f + overall * 0.4f) *
                WaveCalibration.Energy.MOMENTUM_RISE

        // Expansion simulation
        val expansion = overall * WaveCalibration.Energy.EXPANSION_PRESSURE_W

        // Beat confidence
        val beatConfidence = beatPulse * genre.energyScale

        // Pressure (bass-weighted)
        val pressure = bass * 0.6f + mid * 0.25f + treble * 0.15f

        // ── Mode selection ────────────────────────────────────────────
        val mode = when {
            overall > WaveCalibration.Energy.THRESHOLD_EXPANDING -> RibbonMode.EXPANDING
            overall > WaveCalibration.Energy.THRESHOLD_PRESSURE_BUILD_ATTACK -> RibbonMode.PRESSURE_BUILD
            overall > WaveCalibration.Energy.THRESHOLD_BREATHING -> RibbonMode.ACTIVE
            overall > 0.05f -> RibbonMode.BREATHING
            else -> RibbonMode.IDLE
        }

        // ── Ribbon physics from WaveCalibration ────────────────────────
        val targetThickness = when (mode) {
            RibbonMode.IDLE -> WaveCalibration.Physics.THICKNESS_IDLE
            RibbonMode.BREATHING -> WaveCalibration.Physics.THICKNESS_BREATHING
            RibbonMode.ACTIVE -> WaveCalibration.Physics.THICKNESS_ACTIVE
            RibbonMode.PRESSURE_BUILD -> WaveCalibration.Physics.THICKNESS_PRESSURE_BUILD
            RibbonMode.EXPANDING -> WaveCalibration.Physics.THICKNESS_EXPANDING
            RibbonMode.CALM_SETTLING -> WaveCalibration.Physics.THICKNESS_BREATHING
            else -> WaveCalibration.Physics.THICKNESS_ACTIVE
        }
        val thickness = targetThickness * (0.8f + overall * 0.2f)

        val opacity = max(WaveCalibration.Physics.OPACITY_MINIMUM, overall * 0.7f + 0.15f)

        val elasticity = WaveCalibration.Physics.ELASTICITY_BASE +
                WaveCalibration.Physics.ELASTICITY_ENERGY_W * overall
        val viscosity = max(0.1f, 1f - WaveCalibration.Physics.VISCOSITY_ENERGY_W * overall)

        val stretch = when (mode) {
            RibbonMode.EXPANDING -> WaveCalibration.Physics.STRETCH_EXPANDING
            RibbonMode.PRESSURE_BUILD -> WaveCalibration.Physics.STRETCH_PRESSURE_BUILD
            else -> 1f
        }

        val damping = when (mode) {
            RibbonMode.IDLE -> WaveCalibration.Physics.DAMPING_IDLE
            RibbonMode.BREATHING -> WaveCalibration.Physics.DAMPING_BREATHING
            RibbonMode.ACTIVE -> WaveCalibration.Physics.DAMPING_ACTIVE
            RibbonMode.PRESSURE_BUILD -> WaveCalibration.Physics.DAMPING_PRESSURE_BUILD
            RibbonMode.EXPANDING -> WaveCalibration.Physics.DAMPING_EXPANDING
            else -> WaveCalibration.Physics.DAMPING_SETTLING
        }

        val ribbonState = RibbonState(
            thickness = thickness,
            opacity = opacity,
            pressure = pressure,
            elasticity = elasticity,
            viscosity = viscosity,
            stretch = stretch,
            highlightIntensity = treble * WaveCalibration.Physics.HIGHLIGHT_TREBLE_W,
            relaxation = damping
        )

        // ── Generate organic path using WaveCalibration geometry ─────
        val segments = quality.segmentCount
        val pointCount = segments + 1
        val pathPoints = mutableListOf<RibbonPoint>()
        val controls = mutableListOf<BezierControl>()

        for (i in 0 until pointCount) {
            val t = i.toFloat() / segments

            // Amplitude from pressure + momentum
            val pressureAmp = pressure * WaveCalibration.Geometry.PRESSURE_AMPLITUDE_SCALE
            val baseY = sin(t * PI.toFloat() * 2f + timeSec * 3f) * pressureAmp

            // Turbulence from mid
            val turb = sin(t * WaveCalibration.Geometry.TURBULENCE_FREQ + timeSec * 5f) *
                    WaveCalibration.Geometry.TURBULENCE_SCALE * mid

            // Motion offset
            val motionOff = sin(timeSec * 2f + t * 3f) * 0.05f * overall

            val y = (baseY + turb + motionOff).coerceIn(-1f, 1f)

            // Curvature from momentum + geometry
            val curvature = (abs(cos(t * PI.toFloat() * 2f + timeSec * 3f)) *
                    WaveCalibration.Geometry.MOMENTUM_CURVATURE_W * momentum).coerceIn(0f, 1f)

            // Thickness profile (ink brush)
            val thickProfile = computeThicknessProfile(t, thickness)

            // Opacity profile (head fade, body, tail fade)
            val opProfile = computeOpacityProfile(t, opacity)

            pathPoints.add(
                RibbonPoint(
                    x = t,
                    y = y,
                    normalX = 0f,
                    normalY = -1f,
                    thickness = thickProfile.coerceIn(0f, 1f),
                    opacity = opProfile.coerceIn(0f, 1f),
                    curvature = curvature,
                    pressure = pressure,
                    velocity = sin(timeSec * 4f + t * 2f) * overall * 0.3f,
                    highlightIntensity = treble * curvature * WaveCalibration.Geometry.HIGHLIGHT_CURVATURE_W,
                    reflectionFactor = (1f - opacity) * WaveCalibration.Geometry.REFLECTION_CENTER_W,
                    edgeAlpha = curvature * WaveCalibration.Geometry.EDGE_ALPHA_TENSION_W * viscosity
                )
            )
        }

        // Bezier controls (Catmull-Rom approximation)
        for (i in 0 until segments) {
            val p0 = pathPoints[maxOf(0, i - 1)]
            val p1 = pathPoints[i]
            val p2 = pathPoints[minOf(pointCount - 1, i + 1)]
            val p3 = pathPoints[minOf(pointCount - 1, i + 2)]
            val tension = 0.5f

            controls.add(
                BezierControl(
                    cp1X = p1.x + (p2.x - p0.x) * tension / 3f,
                    cp1Y = p1.y + (p2.y - p0.y) * tension / 3f,
                    cp2X = p2.x - (p3.x - p1.x) * tension / 3f,
                    cp2Y = p2.y - (p3.y - p1.y) * tension / 3f,
                    tension = tension
                )
            )
        }

        val path = OrganicPath(
            points = pathPoints.toList(),
            controls = controls.toList(),
            totalLength = segments.toFloat(),
            averageCurvature = pathPoints.map { it.curvature }.average().toFloat(),
            quality = quality,
            timestamp = System.nanoTime()
        )

        // ── Highlight state using WaveCalibration light values ────────
        val highlightPoints = pathPoints.map { p ->
            val spec = p.highlightIntensity * WaveCalibration.Light.SPECULAR_MAX *
                    WaveCalibration.Light.TREBLE_SPARKLE_FACTOR
            val edgeGlow = p.edgeAlpha * WaveCalibration.Light.EDGE_GLOW_MAX *
                    WaveCalibration.Light.SURFACE_TENSION_GLOW_FACTOR
            val rimLight = (1f - p.opacity) * WaveCalibration.Light.RIM_LIGHT_MAX *
                    WaveCalibration.Light.RIM_LIGHT_NORMAL_FACTOR

            HighlightPoint(
                specularIntensity = spec.coerceIn(0f, 1f),
                edgeGlow = edgeGlow.coerceIn(0f, 1f),
                rimLight = rimLight.coerceIn(0f, 1f),
                reflectionAlpha = (p.reflectionFactor * WaveCalibration.Light.REFLECTION_MAX +
                        WaveCalibration.Light.REFLECTION_BASE).coerceIn(0f, 1f),
                glassOpacity = (p.opacity * WaveCalibration.Light.GLASS_OPACITY_WEIGHT +
                        WaveCalibration.Light.GLASS_MIN).coerceIn(0f, WaveCalibration.Light.GLASS_MAX),
                fresnel = p.curvature * WaveCalibration.Light.BASE_FRESNEL_POWER,
                shadowStrength = p.thickness * WaveCalibration.Light.SHADOW_THICKNESS_WEIGHT *
                        WaveCalibration.Light.SHADOW_MAX,
                highlightWidth = spec * WaveCalibration.Light.HIGHLIGHT_WIDTH_MAX,
                lightDirectionX = sin(timeSec * 0.5f) * WaveCalibration.Light.LIGHT_BEND_INFLUENCE,
                lightDirectionY = -1f
            )
        }

        val avgSpec = highlightPoints.map { it.specularIntensity }.average().toFloat()
        val avgFresnel = highlightPoints.map { it.fresnel }.average().toFloat()
        val avgGlass = highlightPoints.map { it.glassOpacity }.average().toFloat()

        val highlightState = HighlightState(
            points = highlightPoints,
            specularIntensity = avgSpec,
            edgeGlow = highlightPoints.map { it.edgeGlow }.average().toFloat(),
            rimLight = highlightPoints.map { it.rimLight }.average().toFloat(),
            reflectionAlpha = highlightPoints.map { it.reflectionAlpha }.average().toFloat(),
            glassOpacity = avgGlass,
            fresnel = avgFresnel,
            shadowStrength = highlightPoints.map { it.shadowStrength }.average().toFloat(),
            highlightWidth = highlightPoints.map { it.highlightWidth }.average().toFloat(),
            viewAngle = 0.7f,
            surfaceNormal = 1f,
            fresnelPower = WaveCalibration.Light.BASE_FRESNEL_POWER + overall * (WaveCalibration.Light.MAX_FRESNEL_POWER - WaveCalibration.Light.BASE_FRESNEL_POWER),
            lightDirection = sin(timeSec * 0.5f) * WaveCalibration.Light.LIGHT_BEND_INFLUENCE,
            timestamp = System.nanoTime()
        )

        val energy = WaveEnergy(
            overallEnergy = overall,
            bassPressure = bass,
            midTexture = mid,
            trebleSpark = treble,
            momentum = momentum,
            expansion = expansion,
            beatConfidence = beatConfidence,
            attack = beatPulse * WaveCalibration.Energy.ATTACK_SENSITIVITY * 0.15f,
            release = (1f - beatPulse) * WaveCalibration.Energy.RELEASE_SENSITIVITY * 0.15f,
            pressure = pressure,
            silence = (1f - overall).coerceIn(0f, 1f)
        )

        return WaveState(
            energy = energy,
            ribbon = ribbonState,
            mode = mode,
            isActive = overall > 0.01f,
            isVisible = true,
            path = path,
            highlight = highlightState,
            timestamp = System.nanoTime()
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private fun computeThicknessProfile(
        t: Float,
        targetThickness: Float
    ): Float {
        // Ink brush profile from WaveCalibration.Geometry
        val profile = when {
            t < WaveCalibration.Geometry.THICKNESS_HEAD_END -> t / WaveCalibration.Geometry.THICKNESS_HEAD_END
            t < WaveCalibration.Geometry.THICKNESS_BODY_START -> 1f
            t < WaveCalibration.Geometry.THICKNESS_BODY_END -> 1f
            t < WaveCalibration.Geometry.THICKNESS_TAIL_START -> {
                val tailT = (t - WaveCalibration.Geometry.THICKNESS_BODY_END) /
                        (WaveCalibration.Geometry.THICKNESS_TAIL_START - WaveCalibration.Geometry.THICKNESS_BODY_END)
                1f - tailT * 0.3f
            }
            t < WaveCalibration.Geometry.THICKNESS_TAIL_END -> {
                val fadeT = (t - WaveCalibration.Geometry.THICKNESS_TAIL_START) /
                        (WaveCalibration.Geometry.THICKNESS_TAIL_END - WaveCalibration.Geometry.THICKNESS_TAIL_START)
                0.7f * (1f - fadeT)
            }
            else -> 0f
        }
        return targetThickness * profile
    }

    private fun computeOpacityProfile(
        t: Float,
        targetOpacity: Float
    ): Float {
        return when {
            t < WaveCalibration.Geometry.OPACITY_HEAD_FADE -> t / WaveCalibration.Geometry.OPACITY_HEAD_FADE
            t > WaveCalibration.Geometry.OPACITY_TAIL_START -> {
                val fadeT = (t - WaveCalibration.Geometry.OPACITY_TAIL_START) /
                        (1f - WaveCalibration.Geometry.OPACITY_TAIL_START)
                1f - fadeT
            }
            else -> 1f
        } * targetOpacity
    }

    private fun buildCurrentPreset(name: String): ExperiencePreset {
        return ExperiencePreset(
            pressureDecay = WaveCalibration.Physics.PRESSURE_DECAY,
            momentumRise = WaveCalibration.Energy.MOMENTUM_RISE,
            momentumFall = WaveCalibration.Energy.MOMENTUM_FALL,
            expansionDecay = WaveCalibration.Energy.EXPANSION_RELEASE_DECAY,
            breathingOpacity = WaveCalibration.Physics.OPACITY_MINIMUM,
            glowIntensity = WaveCalibration.Glow.AMBIENT_RADIUS_FRAC,
            highlightWidth = WaveCalibration.Light.HIGHLIGHT_WIDTH_MAX,
            turbulence = WaveCalibration.Physics.VISCOSITY_ENERGY_W,
            ribbonThickness = WaveCalibration.Physics.THICKNESS_ACTIVE,
            profile = activeProfile,
            displayName = name,
            description = "Lab snapshot"
        )
    }

    private fun Float.pow(exp: Float): Float = this.toDouble().pow(exp.toDouble()).toFloat()

    private fun defaultColorState() = ColorState(
        primary = Color(0xFF19B7FF),
        secondary = Color(0xFF9A6CFF),
        accent = Color(0xFFFF6FD8),
        waveStart = Color(0xFF19B7FF),
        waveEnd = Color(0xFF9A6CFF),
        ambientGlow = Color(0x3319B7FF),
        isFromArtwork = true,
        energyIntensity = 0.5f
    )

    companion object {
        /** Snapshot name for auto-save. */
        const val AUTO_SNAPSHOT_NAME = "_auto"
    }
}
