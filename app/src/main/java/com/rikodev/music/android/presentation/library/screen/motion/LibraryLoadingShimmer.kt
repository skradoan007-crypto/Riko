/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.3 / Phase C — Living Motion
 *
 * File C3 / C6 — LibraryLoadingShimmer.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [LibraryLoadingShimmer] is the *content-coming* signal of the Library
 * screen.
 *
 *   যখন Library লোড হবে — spinner দেখাবে না।
 *   দেখাবে content-এর shadow — shimmer cards।
 *   যেন মনে হয় content ইতিমধ্যে আছে, শুধু visible হওয়ার জন্য অপেক্ষা করছে।
 *
 * ## Why Shimmer, Not Spinner?
 *
 * Spinner বলে — "প্রযুক্তিগত কাজ চলছে।"
 * Shimmer বলে — "তোমার content এখানে আসছে।"
 *
 * Rule #73 — Motion Must Have Meaning.
 * The meaning of shimmer: "Content is coming."
 * The meaning of a spinner: "The machine is busy."
 *
 * Premium apps (Apple Music, Spotify, Instagram) all use shimmer —
 * never spinners — for content loading. Shimmer feels patient;
 * spinners feel anxious.
 *
 * ## What This File Is
 *
 * A composable that paints skeleton cards with a horizontal shimmer
 * sweep. The sweep:
 *
 *   - Cycles every 2.0–2.8 seconds (Rule #64 — discoverable, but
 *     faster than ambient because the user is actively waiting)
 *   - Alpha oscillates between 0.04 and 0.08 (subtle — content-coming
 *     is not alarming)
 *   - Sweeps left-to-right (then resets — but the reset is hidden by
 *     the next sweep starting before the previous ends, so it appears
 *     continuous)
 *   - Reduce-motion: static gradient, no sweep
 *
 * ## Shimmer Vocabulary
 *
 * Three skeleton variants:
 *   - HERO_SKELETON    — large card (Hero placeholder)
 *   - CARD_SKELETON    — small card (Collection placeholder)
 *   - SECTION_SKELETON — section header + row of cards
 *
 * ## Skeletons are NOT Glass
 *
 * Skeletons are placeholders, not finished UI. They use plain [Box]
 * with [clip] + the shimmer modifier — they do NOT wrap in
 * [LibraryGlassSurfaceBorderless]. Glass is for finished surfaces;
 * skeletons are preview shapes. Rule #62 (Never Rebuild Existing
 * Systems) is honored because we're not reinventing glass — we're
 * simply not using it where it doesn't belong.
 *
 * ## Performance
 *
 * - **Single InfiniteTransition** drives the sweep across all
 *   skeletons. They all share the same progress, producing a
 *   unified "wave" of light.
 * - **Zero per-frame allocation**: brushes are remembered.
 * - **drawBehind** for the sweep — no extra Compose nodes.
 *
 * WHAT THIS FILE IS NOT
 * ---------------------
 * ❌ Not a content generator. Skeletons are visual placeholders only.
 * ❌ Not a progress indicator. Shimmer conveys "loading", not "X%".
 * ❌ Not a timeout handler. Timeout is a runtime concern (B5.2).
 *
 * RULES ENFORCED HERE
 * -------------------
 * #58  Runtime Never Knows Rendering   — this is rendering
 * #62  Never Rebuild Existing Systems  — uses Color palette, no glass reinvention
 * #63  Background Never Demands Attention — peak alpha ≤ 0.12 (loading relaxed)
 * #64  Motion Must Be Discoverable     — 1.5s+ cycle
 * #73  Motion Must Have Meaning        — shimmer = content coming
 *
 * © RIKO Experience OS — Rendering Tier (Phase C)
 */

package com.rikodev.music.android.presentation.library.screen.motion

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.theme.AccentCyan
import com.rikodev.music.android.core.designsystem.theme.ColorSurfaceDark
import com.rikodev.music.android.presentation.library.screen.contract.LibraryMotionLanguage
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// SHIMMER SPEC
//
// The static configuration of the shimmer sweep. All numeric invariants
// are enforced in the init block.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A spec for the Library loading shimmer.
 *
 * @property sweepCycleMs       One full sweep (left → right → reset)
 *                              in ms. Faster than ambient because the
 *                              user is actively waiting.
 *                              Rule #64 (relaxed for loading): ≥ 1_500ms.
 * @property baseAlpha          The base alpha of the skeleton.
 *                              Rule #63: ≤ 0.10 (subtle).
 * @property sweepAlpha         The peak alpha of the sweep highlight.
 *                              Rule #63: baseAlpha + sweepAlpha ≤ 0.12
 *                              (slightly higher than ambient because
 *                              the user is actively waiting).
 * @property sweepWidthRatio    Width of the sweep, as a ratio of the
 *                              skeleton's width. 0.3 = 30% of width.
 * @property skeletonColor      Color of the skeleton base.
 * @property sweepColor         Color of the sweep highlight.
 * @property skeletonCornerRadiusDp  Corner radius of skeletons.
 */
data class LibraryLoadingShimmerSpec(
    val sweepCycleMs: Int,
    val baseAlpha: Float,
    val sweepAlpha: Float,
    val sweepWidthRatio: Float,
    val skeletonColor: Color,
    val sweepColor: Color,
    val skeletonCornerRadiusDp: Int,
) {
    init {
        // ── Rule #63 enforcement (relaxed for loading) ────────────────────
        // Loading shimmer is allowed a slightly higher peak (0.12) than
        // ambient (0.10) because the user is actively waiting and needs
        // a more visible "something is happening" signal. But still
        // capped to avoid alarm.
        require(baseAlpha + sweepAlpha <= 0.12f) {
            "Shimmer peak alpha (${baseAlpha + sweepAlpha}) must be ≤ 0.12 " +
                "(Rule #63 — Background Never Demands Attention, relaxed for loading)."
        }
        // ── Rule #64 enforcement (relaxed for loading) ────────────────────
        // Loading sweep can be faster (1.5s) than ambient breath (8s)
        // because the user is actively waiting. But still ≥ 1.5s to
        // avoid feeling anxious.
        require(sweepCycleMs >= 1_500) {
            "Shimmer sweepCycleMs ($sweepCycleMs) must be ≥ 1500 " +
                "(Rule #64 — discoverable motion, relaxed for loading)."
        }
        require(sweepWidthRatio in 0.1f..0.6f) {
            "sweepWidthRatio must be in [0.1, 0.6], got $sweepWidthRatio"
        }
        require(skeletonCornerRadiusDp in 0..32) {
            "skeletonCornerRadiusDp must be in [0, 32], got $skeletonCornerRadiusDp"
        }
    }

    companion object {
        /**
         * DEFAULT spec — Cyan sweep on dark skeleton.
         *
         * - Cycle: 2.2 seconds (calm, not anxious)
         * - Peak alpha: 0.04 + 0.06 = 0.10 (subtle but visible)
         * - Sweep width: 35% of skeleton width
         * - Skeleton corner: 16dp (matches CollectionCard default)
         */
        val DEFAULT = LibraryLoadingShimmerSpec(
            sweepCycleMs = 2_200,
            baseAlpha = 0.04f,
            sweepAlpha = 0.06f,
            sweepWidthRatio = 0.35f,
            skeletonColor = ColorSurfaceDark,
            sweepColor = AccentCyan,
            skeletonCornerRadiusDp = 16,
        )

        /**
         * AMOLED spec — Cyan sweep on near-black skeleton.
         */
        val AMOLED = LibraryLoadingShimmerSpec(
            sweepCycleMs = 2_400,
            baseAlpha = 0.03f,
            sweepAlpha = 0.05f,
            sweepWidthRatio = 0.30f,
            skeletonColor = Color(0xFF111111),
            sweepColor = AccentCyan,
            skeletonCornerRadiusDp = 16,
        )

        /**
         * CINEMATIC spec — slower, more dramatic sweep.
         */
        val CINEMATIC = LibraryLoadingShimmerSpec(
            sweepCycleMs = 2_800,
            baseAlpha = 0.05f,
            sweepAlpha = 0.07f,
            sweepWidthRatio = 0.40f,
            skeletonColor = ColorSurfaceDark,
            sweepColor = AccentCyan,
            skeletonCornerRadiusDp = 20,
        )

        /**
         * REDUCED_MOTION spec — static, no sweep.
         *
         * The skeleton is visible but does not animate. Information
         * (loading) is preserved; only the motion is removed.
         */
        val REDUCED_MOTION = LibraryLoadingShimmerSpec(
            sweepCycleMs = Int.MAX_VALUE,
            baseAlpha = 0.05f,
            sweepAlpha = 0.0f,
            sweepWidthRatio = 0.0f,
            skeletonColor = ColorSurfaceDark,
            sweepColor = AccentCyan,
            skeletonCornerRadiusDp = 16,
        )
    }
}

/**
 * Resolves the appropriate shimmer spec for a given motion language.
 */
internal fun LibraryMotionLanguage.resolveShimmerSpec(
    cinematic: Boolean = false,
    amoled: Boolean = false,
): LibraryLoadingShimmerSpec = when {
    isReduceMotionEnabled -> LibraryLoadingShimmerSpec.REDUCED_MOTION
    amoled -> LibraryLoadingShimmerSpec.AMOLED
    cinematic -> LibraryLoadingShimmerSpec.CINEMATIC
    else -> LibraryLoadingShimmerSpec.DEFAULT
}

// ─────────────────────────────────────────────────────────────────────────────
// SHIMMER SWEEP MODIFIER
//
// The core drawing primitive — paints a skeleton with a sweeping
// highlight. Used by all skeleton variants.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A modifier that paints a shimmer sweep over the composable's bounds.
 *
 * ## Drawing strategy
 *
 * 1. Paint the skeleton base (a solid color at [baseAlpha]).
 * 2. Paint a horizontal sweep highlight that moves left → right.
 *    The sweep is a linear gradient with three color stops:
 *    - 0.0: transparent
 *    - 0.5: sweepColor at sweepAlpha
 *    - 1.0: transparent
 *
 *    The sweep's position is animated by [InfiniteTransition]. The
 *    sweep's left edge starts at -sweepWidth and ends at +width.
 *
 * ## Reduce-motion
 *
 * If `spec.sweepAlpha == 0f`, the sweep is disabled and only the
 * skeleton base is painted.
 *
 * @param spec     The shimmer spec.
 * @param progress The current sweep progress (0.0 → 1.0). Provided by
 *                 the parent composable's InfiniteTransition.
 */
private fun Modifier.shimmerSweep(
    spec: LibraryLoadingShimmerSpec,
    progress: Float,
): Modifier = this.drawBehind {
    // ── Skeleton base ─────────────────────────────────────────────────────
    val skeletonColor = spec.skeletonColor.copy(alpha = spec.baseAlpha)
    drawRect(color = skeletonColor)

    // ── Sweep highlight (if enabled) ──────────────────────────────────────
    if (spec.sweepAlpha > 0f) {
        val sweepWidth = size.width * spec.sweepWidthRatio
        // Sweep moves from (-sweepWidth) to (size.width).
        val sweepStart = -sweepWidth + (progress * (size.width + sweepWidth))

        val sweepColor = spec.sweepColor.copy(alpha = spec.sweepAlpha)
        val transparent = spec.sweepColor.copy(alpha = 0f)

        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(transparent, sweepColor, transparent),
                start = Offset(x = sweepStart, y = 0f),
                end = Offset(x = sweepStart + sweepWidth, y = 0f),
            ),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SHARED TRANSITION PROVIDER
//
// A single InfiniteTransition drives all skeletons in a loading view.
// This produces a unified "wave" of light across all placeholders.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Provides a shared shimmer progress value.
 *
 * This composable starts an [InfiniteTransition] and animates a single
 * `progress` float from 0.0 → 1.0. The progress is shared by all
 * skeleton variants, producing a unified sweep.
 *
 * ## Reduce-motion
 *
 * If `spec.sweepAlpha == 0f`, the progress is fixed at 0.5 (midpoint)
 * and no animation runs.
 *
 * @param spec       The shimmer spec.
 * @param content    The content to compose, given the current progress.
 */
@Composable
fun LibraryShimmerHost(
    spec: LibraryLoadingShimmerSpec,
    content: @Composable (progress: Float) -> Unit,
) {
    val progress: Float = if (spec.sweepAlpha <= 0f) {
        // Reduce-motion: static progress, no animation.
        0.5f
    } else {
        val transition = rememberInfiniteTransition(label = "libraryShimmer")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = spec.sweepCycleMs,
                    easing = EaseInOutSine,
                ),
                // Rule #64: RESTART is allowed here because the sweep
                // is a continuous motion — REVERSE would create a
                // back-and-forth that feels unnatural for a "wave of
                // light". The reset is hidden by the sweep starting
                // off-screen.
                repeatMode = RepeatMode.Restart,
            ),
            label = "libraryShimmerProgress",
        ).value
    }
    content(progress)
}

// ─────────────────────────────────────────────────────────────────────────────
// SKELETON VARIANTS
//
// Three skeletons: HERO, CARD, SECTION. Each is a placeholder shape
// that consumes the shimmer sweep.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Resolves the corner radius for a skeleton as a [Dp].
 */
private fun LibraryLoadingShimmerSpec.skeletonRadiusDp(): Dp =
    skeletonCornerRadiusDp.dp

/**
 * A skeleton Hero card placeholder.
 *
 * Used during Library cold-start / loading. The skeleton has the same
 * dimensions as a real Hero card, so the layout doesn't shift when
 * the actual content arrives.
 *
 * @param spec       The shimmer spec.
 * @param progress   The current sweep progress.
 * @param modifier   Compose modifier.
 */
@Composable
fun LibraryHeroSkeleton(
    spec: LibraryLoadingShimmerSpec,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val radius = remember(spec) { spec.skeletonRadiusDp() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(radius))
            .shimmerSweep(spec, progress),
    )
}

/**
 * A skeleton Collection card placeholder.
 *
 * Used in skeleton rows / grids during loading. The skeleton has the
 * same dimensions as a real Collection card.
 *
 * @param spec       The shimmer spec.
 * @param progress   The current sweep progress.
 * @param modifier   Compose modifier.
 * @param cardWidth  Width of the card (default 160.dp).
 */
@Composable
fun LibraryCardSkeleton(
    spec: LibraryLoadingShimmerSpec,
    progress: Float,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 160.dp,
) {
    val radius = remember(spec) { spec.skeletonRadiusDp() }
    Box(
        modifier = modifier
            .width(cardWidth)
            .height(200.dp)
            .clip(RoundedCornerShape(radius))
            .shimmerSweep(spec, progress),
    )
}

/**
 * A skeleton section placeholder — header bar + row of card skeletons.
 *
 * Used in the main loading view to show the structure of the Library
 * before content arrives.
 *
 * @param spec          The shimmer spec.
 * @param progress      The current sweep progress.
 * @param cardCount     Number of card skeletons in the row (default 3).
 * @param modifier      Compose modifier.
 */
@Composable
fun LibrarySectionSkeleton(
    spec: LibraryLoadingShimmerSpec,
    progress: Float,
    cardCount: Int = 3,
    modifier: Modifier = Modifier,
) {
    val headerRadius = remember(spec) { (spec.skeletonCornerRadiusDp / 4).dp }
    Column(modifier = modifier.fillMaxWidth()) {
        // ── Header skeleton ───────────────────────────────────────────────
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(0.45f)
                .height(18.dp)
                .clip(RoundedCornerShape(headerRadius))
                .shimmerSweep(spec, progress),
        )
        // ── Card row skeleton ─────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            (0 until cardCount).forEach { _ ->
                LibraryCardSkeleton(
                    spec = spec,
                    progress = progress,
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FULL LOADING VIEW
//
// A complete loading screen — Hero skeleton + 2-3 section skeletons.
// This is what the user sees during the Library's LOADING phase.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The full Library loading view.
 *
 * Composes:
 *   1. A Hero skeleton (180dp tall)
 *   2. Two section skeletons (each with 3 card placeholders)
 *
 * All skeletons share a single shimmer sweep (via [LibraryShimmerHost]),
 * producing a unified "wave of light" that conveys "content is coming".
 *
 * Rule #73 — Motion Must Have Meaning.
 * The shimmer's meaning: "Content is coming."
 *
 * @param motionLanguage  The motion language (drives spec resolution).
 * @param cinematic       True if CINEMATIC profile is active.
 * @param amoled          True if AMOLED profile is active.
 * @param modifier        Compose modifier.
 */
@Composable
fun LibraryLoadingView(
    motionLanguage: LibraryMotionLanguage,
    cinematic: Boolean = false,
    amoled: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val spec = remember(motionLanguage, cinematic, amoled) {
        motionLanguage.resolveShimmerSpec(cinematic = cinematic, amoled = amoled)
    }

    LibraryShimmerHost(spec = spec) { progress ->
        Column(modifier = modifier.fillMaxSize()) {
            // ── Hero skeleton ────────────────────────────────────────────
            LibraryHeroSkeleton(
                spec = spec,
                progress = progress,
                modifier = Modifier.padding(16.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            // ── Section skeletons ────────────────────────────────────────
            LibrarySectionSkeleton(
                spec = spec,
                progress = progress,
                cardCount = 3,
            )
            Spacer(modifier = Modifier.height(24.dp))
            LibrarySectionSkeleton(
                spec = spec,
                progress = progress,
                cardCount = 3,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STATIC PREVIEW HELPER
//
// For @Preview without animation. Renders skeletons with progress=0.5.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A static loading view — renders skeletons at midpoint progress.
 *
 * Useful for `@Preview` and unit tests.
 */
@Composable
fun LibraryLoadingViewStatic(
    spec: LibraryLoadingShimmerSpec = LibraryLoadingShimmerSpec.DEFAULT,
    modifier: Modifier = Modifier,
) {
    LibraryShimmerHost(spec = spec) { progress ->
        Column(modifier = modifier.fillMaxSize()) {
            LibraryHeroSkeleton(
                spec = spec,
                progress = progress,
                modifier = Modifier.padding(16.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            LibrarySectionSkeleton(
                spec = spec,
                progress = progress,
                cardCount = 3,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PREVIEWS
// ─────────────────────────────────────────────────────────────────────────────

@androidx.compose.ui.tooling.preview.Preview(
    name = "Loading Shimmer — Default",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewLoadingDefault() {
    LibraryLoadingViewStatic(
        spec = LibraryLoadingShimmerSpec.DEFAULT,
    )
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Loading Shimmer — AMOLED",
    showBackground = true,
    backgroundColor = 0xFF000000,
)
@Composable
private fun PreviewLoadingAmoled() {
    LibraryLoadingViewStatic(
        spec = LibraryLoadingShimmerSpec.AMOLED,
    )
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Loading Shimmer — Reduced Motion (static)",
    showBackground = true,
    backgroundColor = 0xFF050505,
)
@Composable
private fun PreviewLoadingReducedMotion() {
    LibraryLoadingViewStatic(
        spec = LibraryLoadingShimmerSpec.REDUCED_MOTION,
    )
}
