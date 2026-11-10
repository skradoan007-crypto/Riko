/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.3 / Phase B — Identity Reveal
 *
 * File B8 / B9 — DockMotion.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [DockMotion] is the *motion vocabulary* of the Library Dock.
 *
 *   Dock-এর morph feeling — track-change flash, scroll-hide,
 *   expand/collapse — এই সব motion-এর spec এখানে।
 *
 * The Dock does NOT animate on its own — that would violate Rule #67
 * (Dock Never Interrupts). Instead, the Dock receives motion *triggers*
 * from the host (e.g. "track changed", "user scrolled down") and
 * responds with a *single, subtle, one-shot* motion.
 *
 * WHAT THIS FILE IS
 * -----------------
 * A collection of:
 *
 *   1. [LibraryDockMotionSpec] — the motion spec data class
 *   2. [LibraryDockMotionPreset] — named presets (DEFAULT, AMOLED, etc.)
 *   3. [DockTrackChangeFlash] — composable that renders the one-shot
 *      accent flash when the track changes
 *   4. [DockScrollHide] — composable that animates the Dock's
 *      visibility based on scroll direction
 *
 * The motion specs are pure data — no Compose imports, no Android
 * framework. The composables consume the specs and render the motion.
 *
 * WHAT THIS FILE IS NOT
 * ---------------------
 * ❌ Not a motion engine. Uses Compose's built-in animation APIs.
 * ❌ Not a state owner. Reads triggers from caller, never writes.
 * ❌ Not a coordinator. Decides *how* to animate, not *when*.
 *
 * ARCHITECTURE
 * ------------
 *
 *   Host (Phase D) detects track-change / scroll
 *        ↓ passes trigger to
 *   LibraryDock (B7) + DockMotion (B8 — THIS FILE)
 *        ↓ renders
 *   One-shot flash / scroll-hide animation
 *
 * RULES ENFORCED HERE
 * -------------------
 * #58  Runtime Never Knows Rendering   — this is rendering
 * #62  Never Rebuild Existing Systems  — uses Compose animation APIs
 * #64  Motion Must Be Discoverable     — subtle, slow, REVERSE only
 * #67  Dock Never Interrupts           — flash ≤ 200ms, hide ≥ 320ms
 *
 * © RIKO Experience OS — Rendering Tier (Phase B)
 */

package com.rikodev.music.android.presentation.library.screen.identity

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.presentation.library.screen.background.toColorOrNull
import kotlinx.coroutines.delay
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// DOCK MOTION SPEC
//
// Pure data — no Compose imports. Encodes the motion vocabulary of
// the Dock.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The motion spec for the Library Dock.
 *
 * All durations are in milliseconds. All are bounded by Rule #67
 * (Dock Never Interrupts) and Rule #64 (Motion Must Be Discoverable).
 *
 * @property trackChangeFlashDurationMs  Duration of the one-shot accent
 *        flash when the track changes. Rule #67: ≤ 200ms.
 * @property trackChangeFlashPeakAlpha   Peak alpha of the flash. Subtle —
 *        0.18 (visible but not jarring).
 * @property scrollHideDurationMs        Duration of the scroll-down hide
 *        animation. Rule #67: ≥ 320ms (smooth, not jarring).
 * @property scrollRevealDurationMs      Duration of the scroll-up reveal
 *        animation. Same as hide (Rule #67 symmetry).
 * @property expandDurationMs            Duration of the expand animation
 *        (collapsed → expanded). Phase C will use this.
 * @property collapseDurationMs          Duration of the collapse animation.
 * @property morphToNowPlayingDurationMs Duration of the morph to the Now
 *        Playing screen. Phase D will use this.
 */
data class LibraryDockMotionSpec(
    val trackChangeFlashDurationMs: Long,
    val trackChangeFlashPeakAlpha: Float,
    val scrollHideDurationMs: Long,
    val scrollRevealDurationMs: Long,
    val expandDurationMs: Long,
    val collapseDurationMs: Long,
    val morphToNowPlayingDurationMs: Long,
) {
    init {
        // ── Rule #67 enforcement ─────────────────────────────────────────
        require(trackChangeFlashDurationMs <= 200L) {
            "trackChangeFlashDurationMs ($trackChangeFlashDurationMs) must be ≤ 200 " +
                "(Rule #67 — Dock Never Interrupts)."
        }
        require(scrollHideDurationMs >= 320L) {
            "scrollHideDurationMs ($scrollHideDurationMs) must be ≥ 320 " +
                "(Rule #67 — Dock Never Interrupts)."
        }
        require(scrollRevealDurationMs >= 320L) {
            "scrollRevealDurationMs ($scrollRevealDurationMs) must be ≥ 320 " +
                "(Rule #67 — Dock Never Interrupts)."
        }
        // ── Rule #64 enforcement ─────────────────────────────────────────
        require(trackChangeFlashPeakAlpha <= 0.30f) {
            "trackChangeFlashPeakAlpha ($trackChangeFlashPeakAlpha) must be ≤ 0.30 " +
                "(Rule #64 — Motion Must Be Discoverable)."
        }
    }

    companion object {

        /**
         * DEFAULT motion spec — balanced, subtle.
         *
         * - Flash: 180ms, peak 0.18 (one-shot, quick fade-out)
         * - Hide: 360ms (smooth slide-down)
         * - Reveal: 360ms (smooth slide-up)
         * - Expand: 480ms (Phase C will wire this)
         * - Collapse: 360ms (slightly faster than expand)
         * - Morph: 720ms (cinematic — Library → Now Playing)
         */
        val DEFAULT = LibraryDockMotionSpec(
            trackChangeFlashDurationMs = 180L,
            trackChangeFlashPeakAlpha = 0.18f,
            scrollHideDurationMs = 360L,
            scrollRevealDurationMs = 360L,
            expandDurationMs = 480L,
            collapseDurationMs = 360L,
            morphToNowPlayingDurationMs = 720L,
        )

        /**
         * AMOLED motion spec — slightly faster, no flash glow.
         *
         * - Flash: 160ms, peak 0.12 (even quieter on pure black)
         * - Hide: 320ms (slightly snappier)
         * - Reveal: 320ms
         * - Expand: 420ms
         * - Collapse: 320ms
         * - Morph: 640ms
         */
        val AMOLED = LibraryDockMotionSpec(
            trackChangeFlashDurationMs = 160L,
            trackChangeFlashPeakAlpha = 0.12f,
            scrollHideDurationMs = 320L,
            scrollRevealDurationMs = 320L,
            expandDurationMs = 420L,
            collapseDurationMs = 320L,
            morphToNowPlayingDurationMs = 640L,
        )

        /**
         * REDUCED_MOTION spec — instant, no animation.
         *
         * All durations are minimal. Flash is disabled (peak alpha 0).
         * Used when the constitution's reduce-motion flag is on.
         */
        val REDUCED_MOTION = LibraryDockMotionSpec(
            trackChangeFlashDurationMs = 0L,
            trackChangeFlashPeakAlpha = 0.0f,
            scrollHideDurationMs = 0L,
            scrollRevealDurationMs = 0L,
            expandDurationMs = 0L,
            collapseDurationMs = 0L,
            morphToNowPlayingDurationMs = 0L,
        )
    }
}

/**
 * Named motion presets, mapped to constitution profiles.
 */
enum class LibraryDockMotionPreset(
    val spec: LibraryDockMotionSpec,
) {
    DEFAULT(LibraryDockMotionSpec.DEFAULT),
    AMOLED(LibraryDockMotionSpec.AMOLED),
    REDUCED_MOTION(LibraryDockMotionSpec.REDUCED_MOTION);

    companion object {
        /** Resolves the preset for a given profile + reduce-motion flag. */
        fun forProfile(
            isAmoled: Boolean,
            isReducedMotion: Boolean,
        ): LibraryDockMotionPreset = when {
            isReducedMotion -> REDUCED_MOTION
            isAmoled -> AMOLED
            else -> DEFAULT
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TRACK-CHANGE FLASH
//
// A one-shot accent overlay that fires when the track changes. The
// flash is subtle — peak alpha 0.18, duration 180ms. It draws an
// accent-tinted overlay over the entire Dock body, fading from peak
// to transparent.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Renders a one-shot track-change flash overlay on top of the Dock.
 *
 * ## Behavior
 *
 * When [triggerKey] changes (i.e. a new track ID arrives), the flash
 * fires:
 *
 *   1. The overlay's alpha jumps to [LibraryDockMotionSpec.trackChangeFlashPeakAlpha]
 *   2. The alpha animates to 0 over [LibraryDockMotionSpec.trackChangeFlashDurationMs]
 *
 * The flash is one-shot — it does NOT repeat. To fire again, the
 * caller must change [triggerKey] to a new value.
 *
 * ## Rule #67 — Dock Never Interrupts
 *
 * - Flash duration ≤ 200ms (enforced in spec init block)
 * - Flash peak alpha ≤ 0.30 (enforced in spec init block)
 * - The flash does NOT pulse — it fires once and fades.
 *
 * ## Rule #64 — Motion Must Be Discoverable
 *
 * The flash is *discoverable* — the user sees a subtle accent glow
 * when the track changes, but they don't see "an animation". The
 * motion is felt, not watched.
 *
 * @param triggerKey    Stable key that triggers the flash when it
 *                      changes (typically the current track ID).
 * @param accentColorHex  Accent color (hex). Falls back to AccentCyan.
 * @param spec          Motion spec.
 * @param modifier      Compose modifier.
 */
@Composable
fun DockTrackChangeFlash(
    triggerKey: String?,
    accentColorHex: String,
    spec: LibraryDockMotionSpec,
    modifier: Modifier = Modifier,
) {
    // Skip entirely if the spec disables the flash (REDUCED_MOTION).
    if (spec.trackChangeFlashPeakAlpha <= 0f || spec.trackChangeFlashDurationMs <= 0L) {
        return
    }

    val accentColor = remember(accentColorHex) {
        accentColorHex.toColorOrNull() ?: com.rikodev.music.android.core.designsystem.theme.AccentCyan
    }

    // The flash alpha — animated from peak to 0 when triggerKey changes.
    var flashAlpha by remember { mutableStateOf(0f) }

    LaunchedEffect(triggerKey) {
        if (triggerKey != null) {
            // Jump to peak, then animate to 0.
            flashAlpha = spec.trackChangeFlashPeakAlpha
            delay(16) // One frame at 60Hz — let the peak render.
            val steps = 8
            val stepDelay = spec.trackChangeFlashDurationMs / steps
            val stepAlpha = spec.trackChangeFlashPeakAlpha / steps
            repeat(steps) {
                delay(stepDelay)
                flashAlpha -= stepAlpha
            }
            flashAlpha = 0f
        }
    }

    if (flashAlpha > 0f) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(accentColor.copy(alpha = flashAlpha)),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCROLL HIDE / REVEAL
//
// Animates the Dock's visibility based on scroll direction. When the
// user scrolls down (content moving up), the Dock slides down and
// fades out. When the user scrolls up, the Dock slides back up and
// fades in.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Wraps the Dock content with scroll-driven hide/reveal animation.
 *
 * ## Behavior
 *
 * - When [isVisible] is true: the Dock is visible (slides up, fades in)
 * - When [isVisible] is false: the Dock is hidden (slides down, fades out)
 *
 * The animation duration comes from [LibraryDockMotionSpec.scrollHideDurationMs]
 * and [LibraryDockMotionSpec.scrollRevealDurationMs].
 *
 * ## Rule #67 — Dock Never Interrupts
 *
 * - Hide duration ≥ 320ms (enforced in spec init block)
 * - Reveal duration ≥ 320ms (enforced in spec init block)
 * - The Dock NEVER jumps — it always animates between states.
 *
 * @param isVisible   True if the Dock should be visible.
 * @param spec        Motion spec.
 * @param modifier    Compose modifier.
 * @param content     The Dock content to show/hide.
 */
@Composable
fun DockScrollHide(
    isVisible: Boolean,
    spec: LibraryDockMotionSpec,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // REDUCED_MOTION — no animation, just show/hide.
    if (spec.scrollHideDurationMs <= 0L || spec.scrollRevealDurationMs <= 0L) {
        if (isVisible) content()
        return
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(spec.scrollRevealDurationMs.toInt()),
        ) + fadeIn(
            animationSpec = tween(spec.scrollRevealDurationMs.toInt()),
        ),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(spec.scrollHideDurationMs.toInt()),
        ) + fadeOut(
            animationSpec = tween(spec.scrollHideDurationMs.toInt()),
        ),
        modifier = modifier,
    ) {
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DOCK MOTION PREVIEW HELPERS
//
// Plain composables that render the motion components with sample
// state. Useful as @Preview content or as debug overlays.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Renders the track-change flash with the DEFAULT spec, fired once.
 *
 * The preview fires the flash on first composition, then it fades
 * out. Useful for visually verifying the flash duration + alpha.
 */
@Composable
fun DockTrackChangeFlashPreview(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        DockTrackChangeFlash(
            triggerKey = "preview-trigger",
            accentColorHex = "#FF8A65",
            spec = LibraryDockMotionSpec.DEFAULT,
        )
    }
}

/**
 * Renders the scroll-hide wrapper with a sample Box inside, toggling
 * visibility every 2 seconds for preview.
 */
@Composable
fun DockScrollHidePreview(
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            visible = !visible
        }
    }

    DockScrollHide(
        isVisible = visible,
        spec = LibraryDockMotionSpec.DEFAULT,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(com.rikodev.music.android.core.designsystem.theme.ColorVoidDark),
        )
    }
}
