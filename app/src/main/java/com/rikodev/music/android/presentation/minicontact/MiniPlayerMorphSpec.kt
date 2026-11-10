package com.rikodev.music.android.presentation.minicontact

import kotlin.math.abs
import androidx.compose.animation.core.Animatable

/**
 * Morph specification thresholds for the Mini Player.
 *
 * Defines progress-based phase boundaries and commit thresholds.
 * Used by both the reducer (for deciding commit/cancel) and the
 * gesture layer (for phase detection).
 *
 * Also provides interpolation functions that map morph progress (0f..1f)
 * to concrete layout values (height, width, corner radius, etc.).
 */
object MiniPlayerMorphSpec {
    /** Morph progress above which an expand is committed on release. */
    const val EXPAND_COMMIT_THRESHOLD = 0.5f

    /** Phase boundaries for multi-phase morph animation. */
    const val PHASE_STRETCH_END = 0.20f
    const val PHASE_SCALE_END = 0.40f
    const val PHASE_FADE_END = 0.55f
    const val PHASE_WAVE_END = 0.70f
    const val PHASE_ALBUM_END = 0.85f

    // ── Interpolation Endpoints ─────────────────────────────────────────
    const val HEIGHT_COLLAPSED = 80f   // dp
    const val HEIGHT_EXPANDED = 240f    // dp
    const val WIDTH_FRACTION_COLLAPSED = 0.94f
    const val WIDTH_FRACTION_EXPANDED = 1.0f
    const val CORNER_RADIUS_COLLAPSED = 28f  // dp
    const val CORNER_RADIUS_EXPANDED = 0f   // dp
    const val ALBUM_ART_SIZE_COLLAPSED = 52f  // dp
    const val ALBUM_ART_SIZE_EXPANDED = 180f // dp

    /** Controls opacity: full at collapsed, fade out during mid-morph. */
    const val CONTROLS_FADE_START = 0.4f
    const val CONTROLS_FADE_END = 0.7f

    /** Wave visibility: full at collapsed, fade out during mid-morph. */
    const val WAVE_FADE_START = 0.3f
    const val WAVE_FADE_END = 0.6f

    // ── Interpolation Functions ──────────────────────────────────────────
    /** Smooth step for non-linear interpolation. */
    private fun smoothStep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /** Height in dp at given morph progress. 80dp → 240dp. */
    fun heightAt(progress: Float, variant: Int = 0): Float {
        val p = progress.coerceIn(0f, 1f)
        return HEIGHT_COLLAPSED + (HEIGHT_EXPANDED - HEIGHT_COLLAPSED) * smoothStep(0f, 1f, p)
    }

    /** Width fraction at given morph progress. 0.94 → 1.0. */
    fun widthFractionAt(progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        return WIDTH_FRACTION_COLLAPSED + (WIDTH_FRACTION_EXPANDED - WIDTH_FRACTION_COLLAPSED) * smoothStep(0.3f, 0.8f, p)
    }

    /** Corner radius in dp at given morph progress. 28dp → 0dp. */
    fun cornerRadiusAt(progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        return CORNER_RADIUS_COLLAPSED * (1f - smoothStep(0.5f, 0.9f, p))
    }

    /** Album art size in dp at given morph progress. 52dp → 180dp. */
    fun albumArtSizeAt(progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        return ALBUM_ART_SIZE_COLLAPSED + (ALBUM_ART_SIZE_EXPANDED - ALBUM_ART_SIZE_COLLAPSED) * smoothStep(0.4f, 0.8f, p)
    }

    /** Controls opacity at given morph progress. Fades out during mid-morph. */
    fun controlsOpacityAt(progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        return if (p < CONTROLS_FADE_START) 1f
               else if (p > CONTROLS_FADE_END) 0f
               else 1f - (p - CONTROLS_FADE_START) / (CONTROLS_FADE_END - CONTROLS_FADE_START)
    }

    /** Wave visibility at given morph progress. Fades out during mid-morph. */
    fun waveVisibilityAt(progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        return if (p < WAVE_FADE_START) 1f
               else if (p > WAVE_FADE_END) 0f
               else 1f - (p - WAVE_FADE_START) / (WAVE_FADE_END - WAVE_FADE_START)
    }
}
