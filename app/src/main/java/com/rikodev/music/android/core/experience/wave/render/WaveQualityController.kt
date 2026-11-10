package com.rikodev.music.android.core.experience.wave.render

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — WAVE QUALITY CONTROLLER
//
// Automatically selects the optimal WaveQuality based on device capability.
//
// ── DESIGN ────────────────────────────────────────────────────────────────
//   A 30 FPS device should NOT render the same segments as a 144 Hz device.
//   A low-end device with 2GB RAM should NOT use ULTRA quality.
//
//   This controller reads device characteristics (once, at initialization)
//   and provides the appropriate WaveQuality level.
//
//   Quality is NOT a rendering concern — it's an ENGINE concern.
//   The Wave Engine uses this quality to generate the path.
//   The Renderer simply draws whatever it receives.
//
// ── QUALITY TIERS ──────────────────────────────────────────────────────────
//   LOW:      32 segments — Low-end devices, battery saver, background
//   BALANCED: 48 segments — Default for most devices
//   HIGH:     64 segments — Mid-range to high-end
//   ULTRA:    96 segments — Hero moments, high-refresh devices
//
// ── SELECTION CRITERIA ──────────────────────────────────────────────────────
//   Display refresh rate:
//     < 60 Hz  → LOW or BALANCED
//     60 Hz    → BALANCED
//     90–120 Hz → HIGH
//     > 120 Hz → ULTRA
//
//   Screen density:
//     < 2.0 (mdpi) → Can use higher quality (fewer total pixels)
//     2.0–3.0 (hdpi/xhdpi) → BALANCED
//     > 3.0 (xxhdpi+) → May need to reduce quality (more pixels to fill)
//
//   Combined: refresh rate is primary, density is modifier.
//
// PRODUCER: Phase 3.4F
// CONSUMER: WaveEngine (via DI), WavePreviewScreen (manual override)
//
// ⭐ ARCHITECTURAL DECISION
//   Quality is set ONCE at initialization and only changes on
//   configuration change (screen rotation, display change) or manual override.
//   It does NOT fluctuate per-frame.
// ─────────────────────────────────────────────────────────────────────────────

import android.content.Context
import android.view.Display
import android.view.WindowManager
import com.rikodev.music.android.core.experience.wave.WaveQuality
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.platform.LocalDensity

/**
 * Determines the optimal WaveQuality for the current device.
 *
 * Injected as a Singleton — quality is computed once and cached.
 * Call [resolveQuality] after injection, then read [currentQuality].
 */
@Singleton
class WaveQualityController @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // ── State ────────────────────────────────────────────────────────────

    /**
     * The currently selected quality level.
     * Set once at initialization, updated on [refresh] or [setManualOverride].
     */
    var currentQuality: WaveQuality = WaveQuality.BALANCED
        private set

    /**
     * True if a manual override is active (from Preview Screen or settings).
     * When manual override is active, auto-detection is suspended.
     */
    var isManualOverride: Boolean = false
        private set

    // ── Initialization ───────────────────────────────────────────────────

    init {
        resolveQuality()
    }

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Re-detect device quality. Call on configuration changes.
     */
    fun refresh() {
        if (!isManualOverride) {
            resolveQuality()
        }
    }

    /**
     * Manually override the quality level.
     * Used by WavePreviewScreen (3.4H) and settings.
     *
     * @param quality The desired quality level, or null to clear override
     *                and resume auto-detection.
     */
    fun setManualOverride(quality: WaveQuality?) {
        if (quality != null) {
            isManualOverride = true
            currentQuality = quality
        } else {
            isManualOverride = false
            resolveQuality()
        }
    }

    /**
     * Get the segment count for the current quality level.
     * Convenience for the Wave Engine.
     */
    val segmentCount: Int
        get() = currentQuality.segmentCount

    // ── Internal: Quality Detection ───────────────────────────────────────

    /**
     * Detect device capabilities and select appropriate quality.
     *
     * Algorithm:
     *   1. Get display refresh rate
     *   2. Get screen density
     *   3. Primary: refresh rate determines base tier
     *   4. Modifier: density adjusts within tier
     */
    private fun resolveQuality() {
        val refreshRate = getRefreshRate()
        val density = getDensity()

        currentQuality = when {
            // High refresh: premium experience
            refreshRate >= 120f -> {
                if (density > 3.5f) WaveQuality.HIGH else WaveQuality.ULTRA
            }
            // Good refresh: smooth experience
            refreshRate >= 90f -> WaveQuality.HIGH
            // Standard refresh: balanced default
            refreshRate >= 60f -> {
                if (density > 3.5f) WaveQuality.BALANCED else WaveQuality.HIGH
            }
            // Low refresh: conserving resources
            else -> {
                if (density > 3.0f) WaveQuality.LOW else WaveQuality.BALANCED
            }
        }
    }

    /**
     * Get the display refresh rate in Hz.
     * Falls back to 60 Hz if detection fails.
     */
    private fun getRefreshRate(): Float {
        return try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            val display = windowManager?.defaultDisplay
            display?.refreshRate ?: 60f
        } catch (e: Exception) {
            60f // Safe fallback
        }
    }

    /**
     * Get the screen density (DPI scale factor).
     * Falls back to 2.0 (xhdpi) if detection fails.
     */
    private fun getDensity(): Float {
        return try {
            context.resources.displayMetrics.density
        } catch (e: Exception) {
            2.0f // Safe fallback
        }
    }
}
