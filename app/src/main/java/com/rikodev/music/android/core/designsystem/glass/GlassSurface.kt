package com.rikodev.music.android.core.designsystem.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.riko.theme.RikoMotion
import com.rikodev.music.android.core.designsystem.theme.GlassBorderLight
import com.rikodev.music.android.core.designsystem.theme.GlassBorderSubtle
import com.rikodev.music.android.core.designsystem.theme.GlassSpecular
import com.rikodev.music.android.core.designsystem.theme.GlassStyle
import com.rikodev.music.android.core.designsystem.riko.theme.RikoShape

// ─────────────────────────────────────────────────────────────────────────────
// RIKO GLASS ENGINE V3 — DEPTH-AWARE GLASS SURFACE
//
// Build 26 Phase 2: Glass System Rebuild
//
// Premium glass has 5 optical layers, not 2:
//   Layer 1: Background (existing content behind the glass)
//   Layer 2: Frosted Base — vertical gradient white overlay (blur simulation)
//   Layer 3: Tint Layer — album-art-driven color wash (spring-animated)
//   Layer 4: Specular Highlight — top-left catch-light (light source simulation)
//   Layer 5: Grounding Shadow — bottom-right ink shadow (depth/physics)
//   +: Edge Refraction — subtle inner-edge bright line (glass edge effect)
//   +: Color Glow — outer emission when glowAlpha > 0
//
// v3 CHANGES:
//   - Tint color now animates with spring (RikoMotion.glassTintTransition)
//   - Edge refraction line added for "real glass" feel
//   - Frost gradient now has 3 stops (top highlight, mid body, bottom shadow)
//   - Specular highlight position uses consistent light source angle
//   - Grounding shadow expanded for stronger depth perception
//   - All layers compositing order documented
//
// Usage:
//   GlassSurface(style = GlassTokens.MiniPlayer, tintColor = accentColor, ...)
// ─────────────────────────────────────────────────────────────────────────────

// PERF PASS (Priority 3 — GLASS TIERS):
// Glass has one premium rendering path — but list rows are a genuinely
// different context from hero surfaces. A row in a 10,000-song LazyColumn
// renders dozens of times per scroll second; per-row specular catch-lights,
// edge-refraction strokes, grounding shadows, color glow shadow-layers and
// spring-animated tints are invisible subtleties at 52dp row size but very
// real frame-budget costs. Tiers keep RIKO's glass identity (frost, tint,
// border) on every surface while right-sizing depth effects per context:
//
//   Hero        — full five-layer glass (Now Playing sheets, dialogs)
//   Interactive — default; everything interactive (buttons, chips, cards)
//   List        — rows only; frost + tint + border, no depth subtleties
enum class GlassTier {
    Hero,
    Interactive,
    List
}

// Placeholder — kept so LocalHazeState references elsewhere still compile
// without the haze library. Replace with real HazeState if haze is re-added.
object FakeHazeState

val LocalHazeState = compositionLocalOf<FakeHazeState?> { null }

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    style: GlassStyle,
    cornerRadius: Dp = 20.dp,
    shape: Shape? = null,
    tintColor: Color = Color.Transparent,
    showSpecular: Boolean = true,
    tier: GlassTier = GlassTier.Interactive,
    showBorder: Boolean = true,
    borderColor: Color = GlassBorderLight,
    // PERF: real elevation shadows are genuinely expensive to compute.
    // Fine for a handful of sheets/buttons, but murder on a scrolling list
    // where dozens of cards render every second — that's exactly what was
    // causing the ~15fps song-list scroll. Rows in a LazyColumn should pass
    // enableShadow = false; the specular + grounding-shadow draw (below)
    // still gives a cheap sense of depth without the real shadow layer.
    enableShadow: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val resolvedShape = shape ?: RoundedCornerShape(cornerRadius)

    // ── Glass tier resolution (PERF PASS Priority 3) ─────────────────────
    // List tier: skip specular/refraction/grounding/glow and snap the tint
    // instead of springing it. Frost + tint + border (the actual glass
    // identity) are unchanged on every tier.
    val isListTier = tier == GlassTier.List
    val effectiveShowSpecular = showSpecular && !isListTier

    // ── Spring-animated tint color ────────────────────────────────────
    // Build 26: Tint morphs with spring physics when song changes.
    // Glass surfaces feel "alive" — they respond to the music environment.
    // List tier snaps: a spring per row is wasted animation frames.
    val animatedTintColor by animateColorAsState(
        targetValue = tintColor,
        animationSpec = if (isListTier) snap() else RikoMotion.glassTintTransition(),
        label = "glass_tint_morph"
    )

    val tintAlpha = style.tintAlpha
    val shadowAlpha = style.shadowAlpha
    val borderAlpha = style.borderAlpha
    val glowAlpha = style.glowAlpha

    // Elevation: derived from shadowAlpha
    val elevation = (80.dp * shadowAlpha).coerceAtMost(24.dp)

    // Shadow colors: warm base (0x211A12) with style-driven alpha
    val shadowBase = Color(0xFF211A12)
    val shadowAmbient = shadowBase.copy(alpha = shadowAlpha * 0.7f)
    val shadowSpot = shadowBase.copy(alpha = shadowAlpha)

    // Border color: apply style-driven alpha to the provided borderColor
    val effectiveBorderColor = borderColor.copy(alpha = borderAlpha)

    Box(
        modifier = modifier
            .then(
                if (enableShadow) {
                    Modifier.shadow(
                        elevation = elevation,
                        shape = resolvedShape,
                        ambientColor = shadowAmbient,
                        spotColor = shadowSpot
                    )
                } else Modifier
            )
            .then(
                if (!isListTier && glowAlpha > 0f && tintColor != Color.Transparent) {
                    Modifier.colorGlow(tintColor, alpha = glowAlpha)
                } else Modifier
            )
            .clip(resolvedShape)
            // ── Layer 2: Frosted Base (3-stop gradient) ───────────────
            // Top: brighter frost (catches light)
            // Middle: body frost (main translucency)
            // Bottom: subtle shadow frost (depth cue)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = (tintAlpha + 0.12f).coerceAtMost(1f)),
                        Color.White.copy(alpha = tintAlpha),
                        Color.White.copy(alpha = (tintAlpha * 0.85f).coerceAtMost(1f))
                    )
                )
            )
            // ── Layer 3: Tint Layer (spring-animated) ─────────────────
            .background(animatedTintColor)
            .then(
                if (showBorder) Modifier.border(0.8.dp, effectiveBorderColor, resolvedShape) else Modifier
            )
            .drawWithContent {
                drawContent()
                // ── Layer 4: Specular Highlight ───────────────────────
                if (effectiveShowSpecular) drawSpecularHighlight()
                // ── Layer 5: Edge Refraction ──────────────────────────
                // Inner-edge bright line that simulates light bending
                // through glass edges. Very subtle but critical for
                // "this is a real glass object" perception.
                // List tier: invisible at row size — skipped.
                if (!isListTier) drawEdgeRefraction(borderAlpha)
                // ── Grounding Shadow ──────────────────────────────────
                if (effectiveShowSpecular) drawGroundingShadow(shadowAlpha)
            },
        content = content
    )
}

@Composable
fun GlassCircle(
    modifier: Modifier = Modifier,
    style: GlassStyle,
    tintColor: Color = Color.Transparent,
    showSpecular: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    GlassSurface(
        modifier = modifier,
        style = style,
        shape = RikoShape.Circle,
        tintColor = tintColor,
        showSpecular = showSpecular,
        content = content
    )
}

@Composable
fun GlassPill(
    modifier: Modifier = Modifier,
    style: GlassStyle,
    tintColor: Color = Color.Transparent,
    content: @Composable BoxScope.() -> Unit
) {
    GlassSurface(
        modifier = modifier,
        style = style,
        shape = RikoShape.Pill,
        tintColor = tintColor,
        content = content
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    style: GlassStyle,
    tintColor: Color = Color.Transparent,
    cornerRadius: Dp = 16.dp,
    enableShadow: Boolean = true,
    tier: GlassTier = GlassTier.Interactive,
    content: @Composable BoxScope.() -> Unit
) {
    GlassSurface(
        modifier = modifier,
        style = style,
        cornerRadius = cornerRadius,
        tintColor = tintColor,
        borderColor = GlassBorderSubtle,
        enableShadow = enableShadow,
        tier = tier,
        content = content
    )
}

/**
 * Layer 4: Specular Highlight — top-left catch-light.
 * Consistent light source angle (22% from left, 12% from top) across the app.
 * Two-zone gradient: bright center → soft falloff → transparent.
 *
 * BUG FIX (real device report — huge blur ovals covering Settings text):
 * size was purely proportional to the container (60%/50% of width/height)
 * with no upper bound. That looks like a tasteful highlight on a small
 * button or pill, but on a tall container — like a Settings section card
 * stacking several rows, easily 500dp+ tall — the same math produces an
 * oval hundreds of dp tall sitting on top of the content. A real light
 * reflection doesn't grow just because the glass pane is taller; it now
 * has a sane absolute ceiling.
 */
private fun DrawScope.drawSpecularHighlight() {
    val maxHighlightWidth = 130.dp.toPx()
    val maxHighlightHeight = 90.dp.toPx()
    val highlightWidth = (size.width * 0.6f).coerceAtMost(maxHighlightWidth)
    val highlightHeight = (size.height * 0.5f).coerceAtMost(maxHighlightHeight)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                GlassSpecular.copy(alpha = 0.16f),
                GlassSpecular.copy(alpha = 0.05f),
                Color.Transparent
            ),
            center = Offset(size.width * 0.20f, size.height * 0.12f),
            radius = minOf(highlightWidth, highlightHeight) * 0.75f
        ),
        topLeft = Offset(0f, 0f),
        size = Size(highlightWidth, highlightHeight)
    )
}

/**
 * Layer 5: Edge Refraction — subtle inner-edge bright line.
 * Simulates light bending through glass edges. This is what separates
 * "frosted rectangle" from "glass object".
 *
 * Draws a thin (0.5dp) stroke just inside the border that's brighter
 * at the top-left (light source side) and fades toward bottom-right.
 */
private fun DrawScope.drawEdgeRefraction(borderAlpha: Float) {
    if (borderAlpha < 0.02f) return // Skip on nearly-invisible glass
    val refractionAlpha = (borderAlpha * 0.4f).coerceAtMost(0.12f)
    drawOval(
        brush = Brush.linearGradient(
            colors = listOf(
                GlassSpecular.copy(alpha = refractionAlpha),
                GlassSpecular.copy(alpha = refractionAlpha * 0.3f)
            ),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        ),
        style = Stroke(
            width = 0.5.dp.toPx(),
            pathEffect = null
        )
    )
}

/**
 * Grounding Shadow — bottom-right ink shadow.
 * Anchors the glass to the surface beneath it, creating depth.
 * Separated from specular for cleaner compositing order.
 *
 * Same fix as drawSpecularHighlight() — absolute size cap instead of
 * unbounded proportional-to-container sizing.
 */
private fun DrawScope.drawGroundingShadow(shadowAlpha: Float) {
    if (shadowAlpha < 0.02f) return
    val maxShadowWidth = 140.dp.toPx()
    val maxShadowHeight = 90.dp.toPx()
    val shadowWidth = (size.width * 0.60f).coerceAtMost(maxShadowWidth)
    val shadowHeight = (size.height * 0.50f).coerceAtMost(maxShadowHeight)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x14211A12).copy(alpha = shadowAlpha * 3f),
                Color.Transparent
            ),
            center = Offset(size.width - shadowWidth * 0.30f, size.height - shadowHeight * 0.15f),
            radius = minOf(shadowWidth, shadowHeight) * 0.75f
        ),
        topLeft = Offset(size.width - shadowWidth, size.height - shadowHeight),
        size = Size(shadowWidth, shadowHeight)
    )
}

// PERF PASS (P-5, audit): the glow Paint used to be allocated INSIDE the
// draw phase — a fresh Compose Paint + shadow-layer Paint per glass surface
// per FRAME (every scrolling list row, every frame). Draw code runs on the
// UI thread only, so a single shared, sequentially-mutated paint is safe
// and allocation-free.
private val colorGlowPaint = androidx.compose.ui.graphics.Paint()

fun Modifier.colorGlow(
    color: Color,
    radius: Dp = 24.dp,
    alpha: Float = 0.35f
): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val frameworkPaint = colorGlowPaint.asFrameworkPaint().apply {
            isAntiAlias = true
            this.color = android.graphics.Color.TRANSPARENT
            setShadowLayer(radius.toPx(), 0f, 0f, color.copy(alpha = alpha).toArgb())
        }
        canvas.drawCircle(
            center = Offset(size.width / 2f, size.height / 2f),
            radius = size.minDimension / 2f,
            paint = colorGlowPaint
        )
        frameworkPaint.clearShadowLayer()
    }
}
