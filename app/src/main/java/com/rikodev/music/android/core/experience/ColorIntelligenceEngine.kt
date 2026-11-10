package com.rikodev.music.android.core.experience

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.rikodev.music.android.audiofx.AudioState
import com.rikodev.music.android.audiofx.ExperienceDataHub
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — COLOR INTELLIGENCE ENGINE
//
// The Color Brain of Riko. Takes album art + audio energy and produces
// a unified ColorState that drives the entire visual experience.
//
// Architecture:
//
//   Album Art Bitmap
//       │
//       ▼
//   Palette API ──→ PaletteSwatches
//       │                │
//       │                ▼
//       │         PaletteAnalyzer ──→ PaletteAnalysis
//       │                │
//       └────────────────┤
//                        ▼
//   ═════════════════════════════════
//   ║  COLOR INTELLIGENCE ENGINE   ║  ← single writer of ColorState
//   ═════════════════════════════════
//       ▲                │
//       │                ▼
//   AudioState      ColorState
//   (from Hub)    StateFlow<ColorState>
//                        │
//   ─────────────────────┼────────────────────
//   │           │         │          │
//   Wave       Glass    Theme     Nav
//
// ⭐ GOLDEN RULE ⭐
//   Nobody computes ColorState.
//   Everybody subscribes.
//   Only Color Intelligence Engine writes.
//
// RESPONSIBILITY:
//   - Extract palette from album art (Palette API)
//   - Analyze palette (warmth, luminance, monochrome, contrast)
//   - Generate purpose-built colors (wave, glow, glass tint, shadow)
//   - React to audio energy (pulse, tint intensity, glow intensity)
//   - Publish unified StateFlow<ColorState>
//
// WHAT IT DOES NOT DO:
//   - Render anything
//   - Control playback
//   - Manage glass composable lifecycle
// ─────────────────────────────────────────────────────────────────────────────

private const val TAG = "ColorIntelligenceEngine"

@Singleton
class ColorIntelligenceEngine @Inject constructor(
    private val experienceDataHub: ExperienceDataHub,
    private val paletteAnalyzer: PaletteAnalyzer
) {

    // ── Output ────────────────────────────────────────────────────────────
    private val _colorState = MutableStateFlow(ColorState.IDLE)
    val colorState: StateFlow<ColorState> = _colorState.asStateFlow()

    // ── Internal state ────────────────────────────────────────────────────
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var currentAnalysis: PaletteAnalysis? = null

    // ── Emission throttle ───────────────────────────────────────────────
    // AudioState arrives at visualizer rate. Rebuilding + publishing a new
    // ColorState per frame recomposes every color consumer and re-targets
    // every color spring forever — colors never settle (visible flicker)
    // and the CPU never sleeps (battery burn). Energy inputs are quantized
    // to 0.05 steps and frames that change nothing are dropped before any
    // color math runs.
    private data class EnergyKey(
        val energy: Float,
        val bass: Float,
        val treble: Float,
        val beat: Float,
        val analysis: PaletteAnalysis?
    )
    private var lastEnergyKey: EnergyKey? = null

    private fun quantize(v: Float, step: Float = 0.05f): Float =
        (kotlin.math.round(v / step) * step).coerceIn(0f, 1f)

    // ── Default fallback palette (when no artwork is available) ────────────
    private val defaultPrimary = Color(0xFF19B7FF)    // AccentCyan
    private val defaultSecondary = Color(0xFF9A6CFF)  // AccentViolet
    private val defaultAccent = Color(0xFFFF6FD8)    // Pink

    // ── Aurora Violet Identity bias (UI-target sampled) ─────────────────
    // Backgrounds and glow are pulled toward this family so every screen
    // reads as the reference aurora look, while artwork still contributes
    // its own hue (Color Intelligence stays alive, never fully overridden).
    private val auroraDeepIndigoArgb = 0xFF101060.toInt()
    private val auroraVioletArgb = 0xFFA020F0.toInt()
    private val auroraBgBias = 0.55f    // 0..1 — how strongly bg leans aurora
    private val auroraGlowBias = 0.45f  // 0..1 — how strongly glow leans violet

    init {
        // Subscribe to AudioState for energy-reactive color updates
        scope.launch {
            experienceDataHub.audioState.collectLatest { audioState ->
                updateWithAudioEnergy(audioState)
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Process a new album art bitmap and generate ColorState.
     * Called by PlayerViewModel when artwork loads (onArtworkLoaded).
     *
     * This is the main entry point for color intelligence.
     */
    fun processArtwork(bitmap: Bitmap?) {
        scope.launch {
            if (bitmap == null || bitmap.isRecycled) {
                // Keep the last artwork palette on a failed load — flashing
                // back to IDLE defaults strobes the whole UI blue for a frame.
                // Only fall back to IDLE when no artwork was ever published.
                if (!_colorState.value.isFromArtwork) publishIdleState()
                return@launch
            }

            val swatches = extractPalette(bitmap)
            val analysis = paletteAnalyzer.analyze(swatches)
            currentAnalysis = analysis

            val audioState = experienceDataHub.audioState.value
            buildAndPublish(analysis, audioState)
        }
    }

    /**
     * Reset to idle state (no artwork, no audio).
     * Called on track change cleanup or app background.
     */
    fun reset() {
        currentAnalysis = null
        _colorState.value = ColorState.IDLE
    }

    // ═════════════════════════════════════════════════════════════════════
    // PALETTE EXTRACTION (wraps Android Palette API)
    // ═════════════════════════════════════════════════════════════════════

    private suspend fun extractPalette(bitmap: Bitmap): PaletteSwatches =
        withContext(Dispatchers.Default) {
            try {
                val palette = Palette.from(bitmap)
                    .maximumColorCount(24)
                    .generate()

                PaletteSwatches(
                    dominant = palette.dominantSwatch?.rgb ?: 0,
                    vibrant = palette.vibrantSwatch?.rgb ?: 0,
                    darkVibrant = palette.darkVibrantSwatch?.rgb ?: 0,
                    lightVibrant = palette.lightVibrantSwatch?.rgb ?: 0,
                    muted = palette.mutedSwatch?.rgb ?: 0,
                    darkMuted = palette.darkMutedSwatch?.rgb ?: 0
                )
            } catch (e: Exception) {
                Log.w(TAG, "Palette extraction failed: ${e.message}")
                PaletteSwatches()
            }
        }

    // ═════════════════════════════════════════════════════════════════════
    // COLOR GENERATION — The Core Intelligence
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Build the complete ColorState from palette analysis + audio context.
     *
     * This is where the "intelligence" lives — each color is not a raw
     * palette swatch but a PURPOSE-BUILT value derived from analysis.
     */
    private fun buildAndPublish(
        analysis: PaletteAnalysis,
        audioState: AudioState
    ) {
        val hsl = FloatArray(3)

        // ── Primary: Most recognizable color ─────────────────────────────
        // Prefer vibrant, fallback to dominant. Force minimum vibrancy.
        val primaryArgb = if (analysis.vibrant != 0) {
            ensureColorPop(analysis.vibrant, analysis.isMono)
        } else if (analysis.dominant != 0) {
            ensureColorPop(analysis.dominant, analysis.isMono)
        } else {
            defaultPrimary.toArgb()
        }
        val primary = Color(primaryArgb)

        // ── Secondary: Complementary or analogous ────────────────────────
        val secondaryArgb = if (analysis.lightVibrant != 0) {
            analysis.lightVibrant
        } else if (analysis.muted != 0) {
            analysis.muted
        } else {
            defaultSecondary.toArgb()
        }
        val secondary = Color(secondaryArgb)

        // ── Accent: Tertiary highlight ────────────────────────────────────
        // Derive from temperature — warm art gets cool accent, cool art gets warm
        val accentArgb = when (analysis.temperature) {
            ColorTemperature.WARM, ColorTemperature.WARM_ACCENT ->
                shiftHue(primaryArgb, 180f) // complementary
            ColorTemperature.COOL ->
                shiftHue(primaryArgb, 30f)  // warm shift
            ColorTemperature.NEUTRAL ->
                defaultAccent.toArgb()
        }
        val accent = Color(accentArgb)

        // ── Wave colors: Gradient from primary → secondary with audio shift ─
        // Quantized (see EnergyKey): identical frames produce identical
        // colors, so the equality guard below actually dedupes.
        val energy = quantize(audioState.energy)
        val bass = quantize(audioState.bassEnergy)
        val treble = quantize(audioState.trebleEnergy)
        val beat = quantize(audioState.beatConfidence)

        val waveStart = Color(shiftSaturation(primaryArgb, 1f + bass * 0.3f))
        val waveEnd = Color(shiftLightness(secondaryArgb, 1f + treble * 0.2f))

        // ── Glass tint: Subtle version of dominant with alpha ─────────────
        val tintAlpha = (0.08f + energy * 0.12f).coerceIn(0.08f, 0.25f)
        val glassTint = Color(
            red = primary.red,
            green = primary.green,
            blue = primary.blue,
            alpha = tintAlpha
        )

        // ── Background gradient: Aurora-violet identity bias ─────────────
        // Artwork-derived dark swatch still contributes hue, but the result
        // is blended into RIKO's indigo→violet family (target reference).
        val bgArgb = if (analysis.darkVibrant != 0) {
            forceDarkEnough(analysis.darkVibrant, 0.12f)
        } else if (analysis.darkMuted != 0) {
            forceDarkEnough(analysis.darkMuted, 0.10f)
        } else {
            0xFF09111D.toInt()
        }
        val backgroundGradient = Color(
            ColorUtils.blendARGB(bgArgb, auroraDeepIndigoArgb, auroraBgBias)
        )

        // ── Ambient glow: Soft halo behind album art ─────────────────────
        // Blended toward the aurora violet identity so the halo reads as
        // the reference violet glow while artwork hue still shows through.
        val glowAlpha = (0.15f + energy * 0.35f).coerceIn(0.15f, 0.5f)
        val ambientGlow = Color(
            ColorUtils.blendARGB(primary.toArgb(), auroraVioletArgb, auroraGlowBias)
        ).copy(alpha = glowAlpha)

        // ── Shadow: Tinted by dominant hue, not pure black ───────────────
        val shadowAlpha = (0.2f + energy * 0.15f).coerceIn(0.2f, 0.4f)
        val shadowColor = Color(
            red = primary.red * 0.3f,
            green = primary.green * 0.3f,
            blue = primary.blue * 0.3f,
            alpha = shadowAlpha
        )

        // ── Build final state ───────────────────────────────────────────
        val next = ColorState(
            primary = primary,
            secondary = secondary,
            accent = accent,
            waveStart = waveStart,
            waveEnd = waveEnd,
            glassTint = glassTint,
            backgroundGradient = backgroundGradient,
            ambientGlow = ambientGlow,
            shadowColor = shadowColor,
            dominantLuminance = analysis.luminance,
            isDarkArtwork = analysis.isDark,
            isMonochrome = analysis.isMono,
            temperature = analysis.temperature,
            contrastLevel = analysis.contrast,
            dominantHue = analysis.hue,
            dominantSaturation = analysis.saturation,
            energyIntensity = audioState.energy.coerceIn(0f, 1f),
            pulseAmount = audioState.envelope.coerceIn(0f, 1f),
            tintStrength = audioState.energy.coerceIn(0f, 1f),
            isFromArtwork = true
        )
        // Publish only on real change — StateFlow collectors + every color
        // spring downstream stay quiet when the frame is a duplicate.
        if (next != _colorState.value) _colorState.value = next
    }

    // ═════════════════════════════════════════════════════════════════════
    // AUDIO-REACTIVE UPDATES (called per frame from AudioState)
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Update energy-reactive color modifiers based on current audio state.
     * This is called on every AudioState update — lightweight, no allocation.
     */
    private fun updateWithAudioEnergy(audioState: AudioState) {
        val analysis = currentAnalysis

        // Plan 4.1: FAST motion fields publish EVERY tick, raw and quantize-
        // free — quantizing the motion signal made the glow jump in 21 visible
        // steps. Smoothing belongs to the Wave Engine's attack/release, not a
        // color staircase. Pulse = real envelope (attack + sustain + release),
        // never a scaled beat flag.
        val fast = _colorState.value.copy(
            energyIntensity = audioState.energy.coerceIn(0f, 1f),
            pulseAmount = audioState.envelope.coerceIn(0f, 1f),
            tintStrength = audioState.energy.coerceIn(0f, 1f)
        )

        // The quantized key still gates the expensive COLOR rebuild only —
        // slow palette state is allowed to be coarse; motion is not.
        val key = EnergyKey(
            energy = quantize(audioState.energy),
            bass = quantize(audioState.bassEnergy),
            treble = quantize(audioState.trebleEnergy),
            beat = quantize(audioState.beatConfidence),
            analysis = analysis
        )
        if (key == lastEnergyKey) {
            _colorState.value = fast
            return
        }
        lastEnergyKey = key

        // If no analysis yet, just update energy fields
        if (analysis == null) {
            _colorState.value = fast
            return
        }

        // Rebuild with current energy — this updates reactive fields
        buildAndPublish(analysis, audioState)
    }

    private fun publishIdleState() {
        _colorState.value = ColorState.IDLE
    }

    // ═════════════════════════════════════════════════════════════════════
    // COLOR MATH UTILITIES
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Ensure a color has enough pop for an accent role.
     * Monochrome artwork gets the default cyan instead of fake color.
     */
    private fun ensureColorPop(color: Int, isMonochrome: Boolean): Int {
        if (isMonochrome) return defaultPrimary.toArgb()
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)

        // If saturation too low, use default
        if (hsl[1] < 0.15f) return defaultPrimary.toArgb()

        // Boost saturation and lightness for accent use
        if (hsl[1] < 0.5f) hsl[1] = 0.5f
        if (hsl[2] < 0.45f) hsl[2] = 0.45f
        if (hsl[2] > 0.75f) hsl[2] = 0.75f
        if (hsl[1] > 0.85f) hsl[1] = 0.85f

        return ColorUtils.HSLToColor(hsl)
    }

    /**
     * Force luminance below a threshold (for dark backgrounds).
     */
    private fun forceDarkEnough(color: Int, maxLuminance: Float): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        if (hsl[2] > maxLuminance) hsl[2] = maxLuminance
        if (hsl[1] > 0.65f) hsl[1] = 0.65f
        return ColorUtils.HSLToColor(hsl)
    }

    /**
     * Shift hue by degrees (wraps around 360).
     */
    private fun shiftHue(color: Int, degrees: Float): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        hsl[0] = (hsl[0] + degrees) % 360f
        return ColorUtils.HSLToColor(hsl)
    }

    /**
     * Scale saturation by a multiplier (>1 = more saturated, <1 = less).
     */
    private fun shiftSaturation(color: Int, multiplier: Float): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        hsl[1] = (hsl[1] * multiplier).coerceIn(0f, 1f)
        return ColorUtils.HSLToColor(hsl)
    }

    /**
     * Scale lightness by a multiplier.
     */
    private fun shiftLightness(color: Int, multiplier: Float): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        hsl[2] = (hsl[2] * multiplier).coerceIn(0f, 1f)
        return ColorUtils.HSLToColor(hsl)
    }
}
