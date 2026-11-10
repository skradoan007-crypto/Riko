/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.3 / Phase C — Living Motion
 *
 * File C4 / C6 — LibraryEmptyState.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [LibraryEmptyState] is the *opening chapter* of the Library story.
 *
 *   যখন Library ফাঁকা — সেটাকে "ফাঁকা" বলবে না।
 *   সেটাকে "অপেক্ষা করছে" বলবে।
 *   Empty না — Resting.
 *
 * Rule #30 — Empty State Has Emotion.
 * Rule #72 — Identity Before Feature.
 *
 * An empty Library is still a story — it's the *opening chapter*.
 * The user just opened RIKO for the first time; the empty state is
 * RIKO's first words to them. If those words are cold and clinical
 * ("No items found"), the user closes the app. If those words are
 * warm and inviting ("Your library is silent — bring your songs in"),
 * the user stays.
 *
 * ## What This File Is
 *
 * A composable that renders the screen-level empty state, replacing
 * the entire content slot. It draws:
 *
 *   1. An illustration placeholder (simple geometric shapes — no Lottie
 *      yet; the B5.1 contract declares the *kind*, the asset is
 *      loaded by B5.5 host)
 *   2. A title (large, warm)
 *   3. A message (smaller, helpful)
 *   4. Up to two action buttons (primary + secondary)
 *   5. A subtle breathing animation (Rule #73 — "Library is resting")
 *
 * ## 7 Empty Kinds
 *
 * Per B5.1 [LibraryEmptyStateKind], there are 7 empty kinds. Each has
 * a distinct emotional tone:
 *
 *   | Kind               | Tone                   | Illustration       |
 *   |--------------------|------------------------|--------------------|
 *   | FIRST_LAUNCH       | Welcoming, anticipatory| Welcoming hand     |
 *   | PERMISSION_DENIED  | Apologetic, helpful    | Locked vault       |
 *   | ALL_DELETED        | Somber, gentle         | Empty shelf        |
 *   | NO_COLLECTIONS     | Curious, investigative | Searching          |
 *   | FILTER_NO_MATCH    | Neutral, actionable    | Filter empty       |
 *   | SEARCH_NO_RESULT   | Curious, helpful       | Searching          |
 *   | LOAD_FAILED        | Apologetic, recoverable| Broken wave        |
 *
 * Each kind has its own personality variant — different illustration
 * shape, different accent color, different breath depth. This is
 * Rule #72 in action: feel over feature.
 *
 * ## Performance
 *
 * - **Single breathing animation** — one [InfiniteTransition] for
 *   the subtle empty-state breath. No other animations.
 * - **No Lottie / No SVG** — illustrations are drawn with Compose
 *   Canvas primitives (circles, rectangles, paths). Keeps the file
 *   self-contained and avoids asset loading.
 * - **drawBehind** for the illustration — no extra Compose nodes.
 *
 * WHAT THIS FILE IS NOT
 * ---------------------
 * ❌ Not a per-section empty state. That's B4 [RenderEmptyState].
 * ❌ Not a content placeholder. That's C3 [LibraryLoadingShimmer].
 * ❌ Not an error renderer. Errors use LOAD_FAILED kind.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #30  Empty State Has Emotion          — each kind has tone
 * #58  Runtime Never Knows Rendering    — this is rendering
 * #62  Never Rebuild Existing Systems   — uses Color palette
 * #63  Background Never Demands Attention — breath alpha ≤ 0.10
 * #64  Motion Must Be Discoverable      — 8s+ breath cycle
 * #72  Identity Before Feature          — feel over feature
 * #73  Motion Must Have Meaning         — breath = "resting, not dead"
 * #74  Silence Is Also UX               — only one motion (breath)
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.music.android.core.designsystem.theme.AccentCyan
import com.rikodev.music.android.core.designsystem.theme.AccentViolet
import com.rikodev.music.android.presentation.library.screen.contract.LibraryEmptyAction
import com.rikodev.music.android.presentation.library.screen.contract.LibraryEmptyIllustrationKind
import com.rikodev.music.android.presentation.library.screen.contract.LibraryEmptyStateKind
import com.rikodev.music.android.presentation.library.screen.contract.LibraryEmptyStateShape
import com.rikodev.music.android.presentation.library.screen.contract.LibraryMotionLanguage
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// EMPTY STATE PERSONALITY
//
// Each empty kind has a distinct personality — accent color, breath
// depth, illustration hue. This is Rule #72 (Identity Before Feature)
// in action.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The personality of an empty state.
 *
 * Rule #72 — Identity Before Feature.
 * Each empty kind has its own personality — distinct accent, breath,
 * and illustration hue. This makes each empty state feel intentional,
 * not generic.
 *
 * @property kind                The empty state kind.
 * @property accentColor         Accent color for the illustration + buttons.
 * @property illustrationGlowAlpha  Glow alpha around the illustration.
 *                                  Rule #63: ≤ 0.15 (slightly higher
 *                                  than ambient because empty state
 *                                  is the focus, not background).
 * @property breathDepth         How much the illustration breathes.
 *                                  Rule #64: ≤ 0.08.
 * @property breathCycleMs       One full breath cycle in ms.
 *                                  Rule #64: ≥ 8_000ms.
 */
data class LibraryEmptyStatePersonality(
    val kind: LibraryEmptyStateKind,
    val accentColor: Color,
    val illustrationGlowAlpha: Float,
    val breathDepth: Float,
    val breathCycleMs: Int,
) {
    init {
        require(illustrationGlowAlpha <= 0.15f) {
            "Empty state illustrationGlowAlpha ($illustrationGlowAlpha) must be ≤ 0.15 " +
                "(Rule #63 — Background Never Demands Attention, relaxed for empty state focus)."
        }
        require(breathDepth <= 0.08f) {
            "Empty state breathDepth ($breathDepth) must be ≤ 0.08 " +
                "(Rule #64 — alpha delta ≤ 0.10, self-limited to 0.08)."
        }
        require(breathCycleMs >= 8_000) {
            "Empty state breathCycleMs ($breathCycleMs) must be ≥ 8000 " +
                "(Rule #64 — discoverable motion, not obvious)."
        }
    }

    companion object {

        /** FIRST_LAUNCH — warm cyan, inviting. */
        val FIRST_LAUNCH = LibraryEmptyStatePersonality(
            kind = LibraryEmptyStateKind.FIRST_LAUNCH,
            accentColor = AccentCyan,
            illustrationGlowAlpha = 0.12f,
            breathDepth = 0.05f,
            breathCycleMs = 10_000,
        )

        /** PERMISSION_DENIED — apologetic violet. */
        val PERMISSION_DENIED = LibraryEmptyStatePersonality(
            kind = LibraryEmptyStateKind.PERMISSION_DENIED,
            accentColor = AccentViolet,
            illustrationGlowAlpha = 0.10f,
            breathDepth = 0.04f,
            breathCycleMs = 12_000,
        )

        /** ALL_DELETED — somber, muted cyan. */
        val ALL_DELETED = LibraryEmptyStatePersonality(
            kind = LibraryEmptyStateKind.ALL_DELETED,
            accentColor = AccentCyan.copy(alpha = 0.6f),
            illustrationGlowAlpha = 0.08f,
            breathDepth = 0.03f,
            breathCycleMs = 14_000,
        )

        /** NO_COLLECTIONS — curious cyan, brighter. */
        val NO_COLLECTIONS = LibraryEmptyStatePersonality(
            kind = LibraryEmptyStateKind.NO_COLLECTIONS,
            accentColor = AccentCyan,
            illustrationGlowAlpha = 0.13f,
            breathDepth = 0.06f,
            breathCycleMs = 9_000,
        )

        /** FILTER_NO_MATCH — neutral, calm. */
        val FILTER_NO_MATCH = LibraryEmptyStatePersonality(
            kind = LibraryEmptyStateKind.FILTER_NO_MATCH,
            accentColor = AccentCyan.copy(alpha = 0.7f),
            illustrationGlowAlpha = 0.09f,
            breathDepth = 0.04f,
            breathCycleMs = 11_000,
        )

        /** SEARCH_NO_RESULT — curious, helpful cyan. */
        val SEARCH_NO_RESULT = LibraryEmptyStatePersonality(
            kind = LibraryEmptyStateKind.SEARCH_NO_RESULT,
            accentColor = AccentCyan,
            illustrationGlowAlpha = 0.11f,
            breathDepth = 0.05f,
            breathCycleMs = 10_000,
        )

        /** LOAD_FAILED — apologetic violet, somber. */
        val LOAD_FAILED = LibraryEmptyStatePersonality(
            kind = LibraryEmptyStateKind.LOAD_FAILED,
            accentColor = AccentViolet,
            illustrationGlowAlpha = 0.10f,
            breathDepth = 0.04f,
            breathCycleMs = 13_000,
        )

        /**
         * Resolves the personality for a given empty state kind.
         */
        fun forKind(kind: LibraryEmptyStateKind): LibraryEmptyStatePersonality =
            when (kind) {
                LibraryEmptyStateKind.FIRST_LAUNCH -> FIRST_LAUNCH
                LibraryEmptyStateKind.PERMISSION_DENIED -> PERMISSION_DENIED
                LibraryEmptyStateKind.ALL_DELETED -> ALL_DELETED
                LibraryEmptyStateKind.NO_COLLECTIONS -> NO_COLLECTIONS
                LibraryEmptyStateKind.FILTER_NO_MATCH -> FILTER_NO_MATCH
                LibraryEmptyStateKind.SEARCH_NO_RESULT -> SEARCH_NO_RESULT
                LibraryEmptyStateKind.LOAD_FAILED -> LOAD_FAILED
            }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ILLUSTRATION RENDERER
//
// Draws simple geometric illustrations for each empty kind. No Lottie,
// no SVG — pure Compose Canvas. This keeps the file self-contained
// and avoids asset loading.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Renders the illustration for an empty state.
 *
 * Each illustration is drawn with Compose Canvas primitives:
 *   - WELCOMING_HAND: An open arc (suggesting a hand offering)
 *   - EMPTY_SHELF:    A horizontal line with small vertical marks (shelf)
 *   - LOCKED_VAULT:   A circle with a smaller circle inside (lock)
 *   - SEARCHING:      A circle with a diagonal line (magnifier)
 *   - FILTER_EMPTY:   A funnel shape (triangle)
 *   - BROKEN_WAVE:    A wavy path with a gap (broken wave)
 *   - NONE:           No illustration
 *
 * The illustration is drawn with the personality's accent color,
 * surrounded by a subtle radial glow that breathes.
 *
 * Rule #73 — Motion Must Have Meaning.
 * The breath carries: "Library is resting, not dead."
 *
 * @param personality  The empty state personality.
 * @param breath       The current breath value (0f..1f).
 * @param modifier     Compose modifier.
 */
@Composable
fun LibraryEmptyIllustration(
    personality: LibraryEmptyStatePersonality,
    breath: Float,
    modifier: Modifier = Modifier,
) {
    val accent = personality.accentColor
    val illustrationKind = illustrationKindFor(personality.kind)

    // ── Reduce-motion fast path: no breath, static illustration ───────────
    // (The breath value will be 0.5 when reduce-motion is on — see
    // LibraryEmptyState composable below. The illustration still
    // renders, just without animation.)

    Box(
        modifier = modifier
            .size(180.dp)
            .drawBehind {
                // ── Glow (breathing) ───────────────────────────────────────
                // The glow breathes around the illustration. Its alpha
                // is modulated by the breath value.
                val glowAlpha = personality.illustrationGlowAlpha *
                    (1f - personality.breathDepth + (personality.breathDepth * breath))
                val glowColor = accent.copy(alpha = glowAlpha)

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor, Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.5f),
                        radius = size.minDimension * 0.7f,
                    ),
                )
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasAccent = accent.copy(alpha = 0.8f)
            val stroke = Stroke(width = 3.dp.toPx())

            when (illustrationKind) {
                LibraryEmptyIllustrationKind.WELCOMING_HAND -> {
                    // Open arc — suggesting a hand offering music.
                    val path = Path().apply {
                        moveTo(size.width * 0.2f, size.height * 0.7f)
                        cubicTo(
                            size.width * 0.2f, size.height * 0.4f,
                            size.width * 0.5f, size.height * 0.2f,
                            size.width * 0.8f, size.height * 0.4f,
                        )
                    }
                    drawPath(path, color = canvasAccent, style = stroke)
                    // Dot in the center — the "offered" music
                    drawCircle(
                        color = canvasAccent,
                        radius = 6.dp.toPx(),
                        center = Offset(size.width * 0.5f, size.height * 0.45f),
                    )
                }

                LibraryEmptyIllustrationKind.EMPTY_SHELF -> {
                    // Horizontal line with small vertical marks — a shelf.
                    val shelfY = size.height * 0.6f
                    drawLine(
                        color = canvasAccent,
                        start = Offset(size.width * 0.15f, shelfY),
                        end = Offset(size.width * 0.85f, shelfY),
                        strokeWidth = 2.dp.toPx(),
                    )
                    // Small marks on the shelf (where items would stand)
                    (0..3).forEach { i ->
                        val x = size.width * (0.25f + i * 0.18f)
                        drawLine(
                            color = canvasAccent.copy(alpha = 0.4f),
                            start = Offset(x, shelfY - 12.dp.toPx()),
                            end = Offset(x, shelfY),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                }

                LibraryEmptyIllustrationKind.LOCKED_VAULT -> {
                    // Outer circle (vault) + inner circle (lock body)
                    drawCircle(
                        color = canvasAccent,
                        radius = size.minDimension * 0.32f,
                        center = Offset(size.width * 0.5f, size.height * 0.5f),
                        style = stroke,
                    )
                    drawCircle(
                        color = canvasAccent,
                        radius = 8.dp.toPx(),
                        center = Offset(size.width * 0.5f, size.height * 0.5f),
                    )
                    // Lock shackle (arc above)
                    drawArc(
                        color = canvasAccent,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(
                            size.width * 0.5f - 12.dp.toPx(),
                            size.height * 0.5f - 20.dp.toPx(),
                        ),
                        size = Size(24.dp.toPx(), 24.dp.toPx()),
                        style = stroke,
                    )
                }

                LibraryEmptyIllustrationKind.SEARCHING -> {
                    // Magnifier: circle + diagonal handle
                    drawCircle(
                        color = canvasAccent,
                        radius = size.minDimension * 0.22f,
                        center = Offset(size.width * 0.42f, size.height * 0.42f),
                        style = stroke,
                    )
                    drawLine(
                        color = canvasAccent,
                        start = Offset(size.width * 0.58f, size.height * 0.58f),
                        end = Offset(size.width * 0.78f, size.height * 0.78f),
                        strokeWidth = 3.dp.toPx(),
                    )
                }

                LibraryEmptyIllustrationKind.FILTER_EMPTY -> {
                    // Funnel: triangle pointing down
                    val path = Path().apply {
                        moveTo(size.width * 0.25f, size.height * 0.35f)
                        lineTo(size.width * 0.75f, size.height * 0.35f)
                        lineTo(size.width * 0.55f, size.height * 0.6f)
                        lineTo(size.width * 0.45f, size.height * 0.6f)
                        close()
                    }
                    drawPath(path, color = canvasAccent, style = stroke)
                    // Drop below (suggesting "nothing came out")
                    drawCircle(
                        color = canvasAccent.copy(alpha = 0.3f),
                        radius = 3.dp.toPx(),
                        center = Offset(size.width * 0.5f, size.height * 0.75f),
                    )
                }

                LibraryEmptyIllustrationKind.BROKEN_WAVE -> {
                    // Wavy path with a gap — a "broken" wave
                    val path = Path().apply {
                        moveTo(size.width * 0.15f, size.height * 0.5f)
                        cubicTo(
                            size.width * 0.25f, size.height * 0.35f,
                            size.width * 0.35f, size.height * 0.65f,
                            size.width * 0.45f, size.height * 0.5f,
                        )
                        // Gap (no lineTo here — break)
                        moveTo(size.width * 0.55f, size.height * 0.5f)
                        cubicTo(
                            size.width * 0.65f, size.height * 0.35f,
                            size.width * 0.75f, size.height * 0.65f,
                            size.width * 0.85f, size.height * 0.5f,
                        )
                    }
                    drawPath(path, color = canvasAccent, style = stroke)
                }

                LibraryEmptyIllustrationKind.NONE -> {
                    // No illustration — text-only empty state.
                }
            }
        }
    }
}

/**
 * Maps an empty state kind to its illustration kind.
 *
 * Per B5.1 [LibraryEmptyStateShape] companion objects, each kind has
 * a designated illustration.
 */
private fun illustrationKindFor(kind: LibraryEmptyStateKind): LibraryEmptyIllustrationKind =
    when (kind) {
        LibraryEmptyStateKind.FIRST_LAUNCH -> LibraryEmptyIllustrationKind.WELCOMING_HAND
        LibraryEmptyStateKind.PERMISSION_DENIED -> LibraryEmptyIllustrationKind.LOCKED_VAULT
        LibraryEmptyStateKind.ALL_DELETED -> LibraryEmptyIllustrationKind.EMPTY_SHELF
        LibraryEmptyStateKind.NO_COLLECTIONS -> LibraryEmptyIllustrationKind.SEARCHING
        LibraryEmptyStateKind.FILTER_NO_MATCH -> LibraryEmptyIllustrationKind.FILTER_EMPTY
        LibraryEmptyStateKind.SEARCH_NO_RESULT -> LibraryEmptyIllustrationKind.SEARCHING
        LibraryEmptyStateKind.LOAD_FAILED -> LibraryEmptyIllustrationKind.BROKEN_WAVE
    }

// ─────────────────────────────────────────────────────────────────────────────
// ACTION BUTTON
//
// A simple tinted button for empty state CTAs.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * An action button for an empty state.
 *
 * Primary buttons have a filled accent tint; secondary buttons have
 * a subtle outline only.
 *
 * @param action     The action spec (label + actionKey + isPrimary).
 * @param accentColor  The accent color (from personality).
 * @param onAction   Callback invoked when the button is tapped. The
 *                   action's `actionKey` is passed for the host to
 *                   dispatch.
 * @param modifier   Compose modifier.
 */
@Composable
fun LibraryEmptyActionButton(
    action: LibraryEmptyAction,
    accentColor: Color,
    onAction: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = remember(action, accentColor) {
        if (action.isPrimary) {
            accentColor.copy(alpha = 0.20f)
        } else {
            accentColor.copy(alpha = 0.08f)
        }
    }
    val borderColor = remember(action, accentColor) {
        if (action.isPrimary) {
            accentColor.copy(alpha = 0.40f)
        } else {
            accentColor.copy(alpha = 0.20f)
        }
    }

    Box(
        modifier = modifier
            .clickable { onAction(action.actionKey) }
            .clip(RoundedCornerShape(12.dp))
            .drawBehind {
                drawRect(color = tint)
                drawRect(color = borderColor, style = Stroke(width = 1.dp.toPx()))
            }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = action.label,
            color = if (action.isPrimary) accentColor else accentColor.copy(alpha = 0.85f),
            fontSize = 14.sp,
            fontWeight = if (action.isPrimary) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// THE COMPOSABLE
//
// Pure rendering. Animation via InfiniteTransition for the breath.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * THE LIBRARY EMPTY STATE.
 *
 * Renders the screen-level empty state — replacing the entire content
 * slot when the Library has no content to show.
 *
 * ## Drawing strategy
 *
 * 1. Resolves the [LibraryEmptyStatePersonality] for the shape's kind.
 * 2. Starts an [InfiniteTransition] for the breath (unless reduce-motion).
 * 3. Composes:
 *    - The illustration (with breathing glow)
 *    - The title (large, warm)
 *    - The message (smaller, helpful)
 *    - Up to two action buttons
 *
 * ## Reduce-motion
 *
 * When `motionLanguage.isReduceMotionEnabled` is true:
 *   - The breath is fixed at 0.5 (midpoint)
 *   - No animation runs
 *   - The illustration still renders with a static glow
 *
 * ## Rules
 *
 * - **Rule #30**: each kind has its own emotional tone.
 * - **Rule #63**: glow alpha ≤ 0.15 (enforced in personality init).
 * - **Rule #64**: 8s+ breath cycle, ≤ 0.08 alpha delta, REVERSE only.
 * - **Rule #72**: each kind has a distinct personality.
 * - **Rule #73**: breath carries "Library is resting, not dead."
 * - **Rule #74**: only one motion (the breath). No other animations.
 *
 * @param shape          The empty state shape (from contract).
 * @param motionLanguage The motion language (for reduce-motion).
 * @param onAction       Callback for action button taps.
 * @param modifier       Compose modifier.
 */
@Composable
fun LibraryEmptyState(
    shape: LibraryEmptyStateShape,
    motionLanguage: LibraryMotionLanguage,
    onAction: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val personality = remember(shape.kind) {
        LibraryEmptyStatePersonality.forKind(shape.kind)
    }

    // ── Breath animation ──────────────────────────────────────────────────
    // Rule #73: breath = "Library is resting, not dead."
    // Rule #74: only one motion (the breath).
    val breath: Float = if (motionLanguage.isReduceMotionEnabled || personality.breathDepth <= 0f) {
        0.5f // Static midpoint.
    } else {
        val transition = rememberInfiniteTransition(label = "libraryEmptyBreath")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = personality.breathCycleMs / 2,
                    easing = EaseInOutSine,
                ),
                repeatMode = RepeatMode.Reverse, // Rule #64: never RESTART.
            ),
            label = "libraryEmptyBreathValue",
        ).value
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // ── Illustration ──────────────────────────────────────────────────
        LibraryEmptyIllustration(
            personality = personality,
            breath = breath,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ── Title ─────────────────────────────────────────────────────────
        Text(
            text = shape.title,
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Message ───────────────────────────────────────────────────────
        Text(
            text = shape.message,
            color = Color.White.copy(alpha = 0.62f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )

        // ── Action buttons ────────────────────────────────────────────────
        if (shape.primaryAction != null || shape.secondaryAction != null) {
            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                shape.secondaryAction?.let { action ->
                    LibraryEmptyActionButton(
                        action = action,
                        accentColor = personality.accentColor,
                        onAction = onAction,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                shape.primaryAction?.let { action ->
                    LibraryEmptyActionButton(
                        action = action,
                        accentColor = personality.accentColor,
                        onAction = onAction,
                    )
                }
            }
        }

        // ── Recoverable hint ──────────────────────────────────────────────
        if (shape.isRecoverable) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "—",
                color = Color.White.copy(alpha = 0.20f),
                fontSize = 12.sp,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STATIC PREVIEW HELPER
//
// For @Preview without animation. Renders at midpoint breath.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A static empty state — renders at midpoint breath, no animation.
 *
 * Useful for `@Preview` and unit tests.
 */
@Composable
fun LibraryEmptyStateStatic(
    shape: LibraryEmptyStateShape = LibraryEmptyStateShape.FIRST_LAUNCH,
    modifier: Modifier = Modifier,
) {
    val staticMotionLanguage = LibraryMotionLanguage.DEFAULT.copy(isReduceMotionEnabled = true)
    LibraryEmptyState(
        shape = shape,
        motionLanguage = staticMotionLanguage,
        onAction = {},
        modifier = modifier,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// PREVIEWS
// ─────────────────────────────────────────────────────────────────────────────

@androidx.compose.ui.tooling.preview.Preview(
    name = "Empty — First Launch",
    showBackground = true,
    backgroundColor = 0xFF05070B,
)
@Composable
private fun PreviewEmptyFirstLaunch() {
    LibraryEmptyStateStatic(shape = LibraryEmptyStateShape.FIRST_LAUNCH)
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Empty — Permission Denied",
    showBackground = true,
    backgroundColor = 0xFF05070B,
)
@Composable
private fun PreviewEmptyPermissionDenied() {
    LibraryEmptyStateStatic(shape = LibraryEmptyStateShape.PERMISSION_DENIED)
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Empty — All Deleted",
    showBackground = true,
    backgroundColor = 0xFF05070B,
)
@Composable
private fun PreviewEmptyAllDeleted() {
    LibraryEmptyStateStatic(shape = LibraryEmptyStateShape.ALL_DELETED)
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Empty — Load Failed",
    showBackground = true,
    backgroundColor = 0xFF05070B,
)
@Composable
private fun PreviewEmptyLoadFailed() {
    LibraryEmptyStateStatic(shape = LibraryEmptyStateShape.LOAD_FAILED)
}
