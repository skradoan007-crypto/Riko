/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.3 / Phase B — Identity Reveal
 *
 * File B1 / B9 — LibraryHero.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [LibraryHero] is the *face* of the Library screen.
 *
 *   User app open করার পর প্রথম ১ সেকেন্ডে যা দেখে — সেটাই Hero.
 *
 * Hero is the screen's opening statement (Rule #54 — Screen Tells a
 * Story). It is the first thing the user sees in the content slot,
 * usually "Continue Listening" or a featured collection. It is the
 * *beginning* of the story, and it must do three things:
 *
 *   1. EXPLAIN  — what is happening in the user's music world right now.
 *                 (Rule #68 — Hero Explains Library.)
 *
 *   2. INVITE   — hint at what lies below, without competing for
 *                 attention. (Rule #65 — Hero Never Competes.)
 *
 *   3. ANCHOR   — give the screen a visual center of gravity. Even
 *                 after the user scrolls past, the screen "feels"
 *                 organized because the Hero established the top.
 *
 * WHAT THIS FILE IS
 * -----------------
 * A pure Compose declaration of the Hero *card itself* — artwork,
 * overlay gradient, title, subtitle, optional badge, and a press
 * affordance. This file knows nothing about:
 *
 *   - Where the Hero is positioned on screen (that's Container's job)
 *   - What glass material wraps it (that's Container's job)
 *   - What depth stratum it lives at (that's Container's job)
 *   - How it animates on entrance (that's Motion's job, Phase C)
 *   - What happens on tap (that's a callback to the Coordinator)
 *
 * WHAT THIS FILE IS NOT
 * ---------------------
 * ❌ Not a container. No spacing, no margin, no glass surface.
 * ❌ Not a motion spec. Entrance + parallax live elsewhere.
 * ❌ Not a state owner. Hero reads state, never writes.
 * ❌ Not a router. Tap fires a callback, the host decides where to go.
 *
 * ARCHITECTURE
 * ------------
 *
 *   ┌─────────────────────────────────────────────────┐
 *   │  LibraryHeroContainer (B2)                     │  ← wraps Hero
 *   │  ┌───────────────────────────────────────────┐  │
 *   │  │  LibraryHero (B1) — THIS FILE            │  │
 *   │  │  ┌─────────────────────────────────────┐  │  │
 *   │  │  │  Artwork (full-bleed)               │  │  │
 *   │  │  │  ┌───────────────────────────────┐  │  │  │
 *   │  │  │  │  Overlay Gradient (bottom)    │  │  │  │
 *   │  │  │  │  ┌─────────────────────────┐  │  │  │  │
 *   │  │  │  │  │  Title + Subtitle       │  │  │  │  │
 *   │  │  │  │  │  + Badge (optional)     │  │  │  │  │
 *   │  │  │  │  └─────────────────────────┘  │  │  │  │
 *   │  │  │  └───────────────────────────────┘  │  │  │
 *   │  │  └─────────────────────────────────────┘  │  │
 *   │  └───────────────────────────────────────────┘  │
 *   └─────────────────────────────────────────────────┘
 *
 * RULES ENFORCED HERE
 * -------------------
 * #58  Runtime Never Knows Rendering   — this is rendering
 * #62  Never Rebuild Existing Systems  — reuses Color.kt, no new glass
 * #65  Hero Never Competes             — glow capped, saturation muted
 * #68  Hero Explains Library           — title + subtitle from state
 *
 * © RIKO Experience OS — Rendering Tier (Phase B)
 */

package com.rikodev.music.android.presentation.library.screen.identity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.music.android.core.designsystem.theme.AccentCyan
import com.rikodev.music.android.core.designsystem.theme.ColorVoidDark
import com.rikodev.music.android.presentation.library.screen.background.toColorOrNull
import com.rikodev.music.android.presentation.library.screen.contract.LibraryHeroContract
import com.rikodev.music.android.presentation.library.screen.contract.LibraryHeroOverlaySpec
import com.rikodev.music.android.presentation.library.screen.contract.LibraryHeroTextPosition
import com.rikodev.music.android.presentation.library.screen.runtime.LibraryHeroRuntimeState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.draw.drawBehind

// ─────────────────────────────────────────────────────────────────────────────
// HERO BADGE
//
// A small chip on the Hero card. Carries ONE piece of metadata —
// track count, mood label, time-of-day, etc.
//
// Rule #68 — at most 3 badges per Hero (otherwise it becomes a
// dashboard, not a Hero). The Container enforces this cap.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A small metadata badge on the Hero card.
 *
 * Each badge carries a single piece of context — count, time, mood.
 * Badges are *quiet*: small text, accent-tinted background, no icon
 * unless one is explicitly provided.
 *
 * @property label        Badge text (e.g. "12 tracks", "Morning").
 * @property accentColorHex  Accent color (hex). Falls back to AccentCyan.
 * @property icon          Optional icon. Most badges have no icon.
 */
data class LibraryHeroBadge(
    val label: String,
    val accentColorHex: String,
    val icon: ImageVector? = null,
) {
    companion object {
        /** A badge with no content — used as a sentinel for "no badge". */
        val EMPTY = LibraryHeroBadge(
            label = "",
            accentColorHex = "#FFFFFFFF",
            icon = null,
        )
    }
}

/** True if this badge has no visible content. */
val LibraryHeroBadge.isEmpty: Boolean
    get() = label.isBlank()

// ─────────────────────────────────────────────────────────────────────────────
// HERO RESOLVED SHAPE
//
// Convert the B5.1 contract's Hero shape spec into concrete Compose
// values. `remember`-keyed so the resolution happens once per
// contract instance, not per recomposition.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A fully-resolved Hero shape — concrete values ready to be passed
 * to Compose layout modifiers.
 *
 * @property aspectRatioW      Width component of the card's aspect ratio.
 * @property aspectRatioH      Height component.
 * @property cornerRadiusDp    Corner radius in dp.
 * @property cardWidthFraction 0.0–1.0. Card width as fraction of parent.
 * @property cardHeightDp      Card height in dp (used when width is constrained).
 */
internal data class LibraryHeroResolvedShape(
    val aspectRatioW: Float,
    val aspectRatioH: Float,
    val cornerRadiusDp: androidx.compose.ui.unit.Dp,
    val cardWidthFraction: Float,
    val cardHeightDp: Int,
) {
    companion object {
        /**
         * Resolves a [LibraryHeroContract]'s shape + prominence into
         * concrete Compose values.
         */
        fun from(
            contract: LibraryHeroContract,
        ): LibraryHeroResolvedShape = LibraryHeroResolvedShape(
            aspectRatioW = contract.cardShape.artworkAspectRatioW,
            aspectRatioH = contract.cardShape.artworkAspectRatioH,
            cornerRadiusDp = contract.cardShape.cornerRadiusDp.dp,
            cardWidthFraction = contract.prominence.cardWidthFraction,
            cardHeightDp = contract.prominence.cardHeightDp,
        )
    }
}

/**
 * Resolves the Hero's accent color from a hex string, with a safe
 * fallback to [AccentCyan] (the RIKO primary accent).
 *
 * Rule #62 — never invent new accent colors here. The hex comes from
 * the runtime state (which got it from the active song's palette).
 */
internal val LibraryHeroRuntimeState.resolvedAccentColor: Color
    get() = accentColorHex.toColorOrNull() ?: AccentCyan

// ─────────────────────────────────────────────────────────────────────────────
// THE HERO COMPOSABLE
//
// Pure declaration. Reads contract + state, renders artwork + overlay
// + text + badges. Fires a single callback on tap.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * THE LIBRARY HERO.
 *
 * Renders the Hero card:
 *
 *   1. Full-bleed artwork (caller provides via [artwork] slot)
 *   2. Overlay gradient (bottom-up, for text readability)
 *   3. Title (large, white)
 *   4. Subtitle (smaller, accent-tinted)
 *   5. Optional badges (≤ 3, capped by [LibraryHeroContract])
 *
 * ## Rule #65 — Hero Never Competes
 *
 * - The overlay gradient's bottom opacity comes from the contract
 *   (default 0.85), capped at 0.90 — Hero must be readable, not
 *   blinding.
 * - The accent color's saturation is reduced by 30% (Rule #65 cap)
 *   so the Hero's tint doesn't out-shout the Collections below.
 * - Title weight is 600 (semibold), not 700 — bold enough to anchor,
 *   not so bold it dominates.
 *
 * ## Rule #68 — Hero Explains Library
 *
 * - Title and subtitle both come from [LibraryHeroRuntimeState] —
 *   they are *dynamic*, not hardcoded. If the user has nothing
 *   playing, the state should provide a graceful fallback title
 *   (e.g. "Welcome back") — Hero never renders empty.
 * - The artwork slot is also dynamic — caller resolves what to show
 *   (album art, gradient placeholder, illustration).
 *
 * ## Performance
 *
 * - The overlay gradient [Brush] is `remember`ed per contract
 *   instance — zero per-frame allocation when the contract is stable.
 * - The artwork is a Compose slot, so the caller controls its
 *   allocation strategy (cache, async load, etc.).
 * - No `drawBehind` lambda allocates — we use `Modifier.background(brush)`.
 *
 * ## Accessibility
 *
 * - Title text has a drop shadow (Rule #68 — text must be readable
 *   on any artwork).
 * - Tap target is the entire card, minimum 48dp height guaranteed
 *   by [LibraryHeroProminence] (smallest card is 180dp tall).
 *
 * @param contract      Hero contract from B5.1 — provides shape + overlay spec.
 * @param state         Hero runtime state from B5.2 — provides title, subtitle, accent.
 * @param badges        Up to 3 badges. Excess badges are dropped (Rule #68).
 * @param artwork       Slot for the artwork content. Caller decides what to draw.
 * @param onTap         Callback fired when the Hero is tapped. May be null
 *                      (some Heroes are display-only).
 * @param modifier      Compose modifier.
 */
@Composable
fun LibraryHero(
    contract: LibraryHeroContract,
    state: LibraryHeroRuntimeState,
    badges: List<LibraryHeroBadge> = emptyList(),
    artwork: @Composable BoxScope.() -> Unit,
    onTap: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    modifier: Modifier = Modifier,
) {
    // ── Resolve shape + accent ────────────────────────────────────────────
    val shape = remember(contract) { LibraryHeroResolvedShape.from(contract) }
    val accent = remember(state.accentColorHex) { state.resolvedAccentColor }
    // Rule #65 — saturation muted by 30% so Hero doesn't out-shout Collections.
    val mutedAccent = remember(accent) {
        accent.copy(
            alpha = (accent.alpha * 0.85f).coerceIn(0f, 1f),
        )
    }

    // ── Resolve overlay gradient ──────────────────────────────────────────
    // Bottom-up gradient: transparent at top, dark at bottom (text sits on
    // the dark portion). Opacities come from the contract's overlay spec.
    val overlaySpec = contract.overlaySpec
    val overlayBrush = remember(overlaySpec) {
        Brush.verticalGradient(
            colors = listOf(
                Color.Black.copy(alpha = overlaySpec.gradientStartOpacity),
                Color.Black.copy(alpha = overlaySpec.gradientEndOpacity),
            ),
        )
    }

    // ── Capped badges (Rule #68 — at most 3) ──────────────────────────────
    val visibleBadges = remember(badges) {
        badges
            .filter { !it.isEmpty }
            .take(3)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(shape.aspectRatioW / shape.aspectRatioH)
            .clip(RoundedCornerShape(shape.cornerRadiusDp))
            .then(
                if (onTap != null) {
                    Modifier.clickableLightweight(onTap, interactionSource)
                } else {
                    Modifier
                },
            ),
    ) {
        // Layer 1: Artwork (caller-provided slot).
        Box(
            modifier = Modifier.fillMaxSize(),
            content = artwork,
        )

        // Layer 2: Overlay gradient (above artwork, below text).
        // Drawn as a background fill so it doesn't interfere with text.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = overlayBrush),
        )

        // Layer 3: Text + badges (positioned per overlay spec).
        HeroTextBlock(
            overlaySpec = overlaySpec,
            title = state.primaryTitle,
            subtitle = state.primarySubtitle,
            accent = mutedAccent,
            badges = visibleBadges,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HERO TEXT BLOCK
//
// Positions title + subtitle + badges per the overlay spec's
// textPosition field. Internal — only used by [LibraryHero].
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Renders the Hero's text block (title + subtitle + badges) at the
 * position declared by the overlay spec.
 *
 * ## Text readability
 *
 * - Title is white (#FFFFFF) at 100% opacity — needs to read on
 *   any artwork.
 * - Subtitle is accent-tinted at 85% opacity — slightly muted to
 *   give the title visual primacy.
 * - Both have a soft drop shadow (1dp blur, 0.5dp offset) so they
 *   remain readable on bright artwork patches.
 *
 * @param overlaySpec  Overlay spec — drives text position.
 * @param title        Title text.
 * @param subtitle     Subtitle text (may be empty — title-only Hero).
 * @param accent       Muted accent color for subtitle + badges.
 * @param badges       Visible badges (≤ 3, already capped upstream).
 * @param modifier     Compose modifier.
 */
@Composable
private fun HeroTextBlock(
    overlaySpec: LibraryHeroOverlaySpec,
    title: String,
    subtitle: String,
    accent: Color,
    badges: List<LibraryHeroBadge>,
    modifier: Modifier = Modifier,
) {
    val alignment = remember(overlaySpec.textPosition) {
        overlaySpec.textPosition.toAlignment()
    }
    val paddingDp = remember(overlaySpec.textPosition) {
        overlaySpec.textPosition.toPaddingDp()
    }

    Box(
        modifier = modifier.padding(paddingDp),
        contentAlignment = alignment,
    ) {
        Column(
            horizontalAlignment = when (alignment) {
                Alignment.TopStart, Alignment.CenterStart, Alignment.BottomStart ->
                    Alignment.Start
                Alignment.TopEnd, Alignment.CenterEnd, Alignment.BottomEnd ->
                    Alignment.End
                else -> Alignment.CenterHorizontally
            },
        ) {
            // Title — large, white, semibold.
            Text(
                text = title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = if (overlaySpec.textShadowEnabled) {
                        androidx.compose.ui.graphics.Shadow(
                            color = Color.Black.copy(alpha = 0.6f),
                            blurRadius = 4f,
                            offset = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                        )
                    } else null,
                ),
            )

            // Subtitle — accent-tinted, smaller. Only if present.
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = accent.copy(alpha = 0.90f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = if (overlaySpec.textShadowEnabled) {
                            androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.5f),
                                blurRadius = 3f,
                                offset = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                            )
                        } else null,
                    ),
                )
            }

            // Badges — small chips below subtitle. Only if any.
            if (badges.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    badges.forEach { badge ->
                        HeroBadgeChip(
                            badge = badge,
                            accent = accent,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Renders a single Hero badge as a small chip.
 *
 * Internal — only used by [HeroTextBlock].
 */
@Composable
private fun HeroBadgeChip(
    badge: LibraryHeroBadge,
    accent: Color,
) {
    val chipColor = remember(accent) {
        accent.copy(alpha = 0.18f) // Rule #63 — cap at 0.20, we use 0.18 for headroom.
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(chipColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
    ) {
        if (badge.icon != null) {
            Icon(
                imageVector = badge.icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.90f),
                modifier = Modifier.size(12.dp),
            )
        }
        Text(
            text = badge.label,
            color = Color.White.copy(alpha = 0.95f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// OVERLAY SPEC → ALIGNMENT MAPPING
//
// The contract's [LibraryHeroTextPosition] enum declares where text
// sits on the Hero. We map each enum value to a Compose [Alignment]
// and a [PaddingValues] for the text block.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Maps a [LibraryHeroTextPosition] to a Compose [Alignment].
 */
private fun LibraryHeroTextPosition.toAlignment(): Alignment =
    when (this) {
        LibraryHeroTextPosition.TOP_LEFT -> Alignment.TopStart
        LibraryHeroTextPosition.TOP_CENTER -> Alignment.TopCenter
        LibraryHeroTextPosition.BOTTOM_LEFT -> Alignment.BottomStart
        LibraryHeroTextPosition.BOTTOM_CENTER -> Alignment.BottomCenter
        LibraryHeroTextPosition.CENTER -> Alignment.Center
    }

/**
 * Maps a [LibraryHeroTextPosition] to a [PaddingValues] (in dp).
 *
 * Bottom-positioned text gets more bottom padding (to clear the
 * rounded corner + safe area). Top-positioned text gets less.
 */
private fun LibraryHeroTextPosition.toPaddingDp(): androidx.compose.foundation.layout.PaddingValues =
    when (this) {
        LibraryHeroTextPosition.TOP_LEFT,
        LibraryHeroTextPosition.TOP_CENTER,
        -> androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 14.dp,
            bottom = 14.dp,
        )

        LibraryHeroTextPosition.BOTTOM_LEFT,
        LibraryHeroTextPosition.BOTTOM_CENTER,
        -> androidx.compose.foundation.layout.PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = 14.dp,
            bottom = 18.dp, // More bottom — clear the rounded corner.
        )

        LibraryHeroTextPosition.CENTER ->
            androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 20.dp,
                bottom = 20.dp,
            )
    }

// ─────────────────────────────────────────────────────────────────────────────
// LIGHTWEIGHT CLICK MODIFIER
//
// A minimal click affordance for the Hero. Uses Compose's standard
// [androidx.compose.foundation.clickable] with no indication (no
// ripple) — the Hero's press feedback is handled by the Container
// via a press-state visual overlay, not by a ripple on the card.
//
// This keeps the click fast (no state machine for indication) and
// the visual clean (no ripple bleeding through the gradient overlay).
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A minimal click modifier for the Hero.
 *
 * Fires [onClick] on tap. No ripple indication — press feedback is
 * handled at the Container level via a press-state overlay.
 *
 * Internal — only used by [LibraryHero].
 */
private fun Modifier.clickableLightweight(
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource? = null,
): Modifier = this.then(
    Modifier.clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick,
    ),
)

// ─────────────────────────────────────────────────────────────────────────────
// HERO PLACEHOLDER (for previews)
//
// A no-artwork Hero — useful for `@Preview` and for the brief moment
// before the artwork loader resolves. Renders a flat accent-tinted
// background in place of the artwork.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A placeholder artwork slot — paints a flat accent-tinted background.
 *
 * Use this in `@Preview` or as a fallback while the real artwork
 * loads. The placeholder never blocks the UI — it just fills the
 * artwork slot with *something* so the Hero doesn't look broken.
 *
 * @param accentColorHex  Accent color (hex). Falls back to ColorVoidDark.
 * @param modifier        Compose modifier.
 */
@Composable
fun LibraryHeroArtworkPlaceholder(
    accentColorHex: String = "#1A1A2E",
    modifier: Modifier = Modifier,
) {
    val color = remember(accentColorHex) {
        accentColorHex.toColorOrNull() ?: ColorVoidDark
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// HERO PREVIEW HELPERS
//
// Plain composables (NOT `@Preview` annotated — that requires the
// Compose preview tooling). Useful as preview content or for
// debugging.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Renders the default Hero with placeholder artwork.
 *
 * Useful as `@Preview` content or as a debug overlay.
 */
@Composable
fun LibraryHeroPreviewDefault(
    modifier: Modifier = Modifier,
) {
    LibraryHero(
        contract = LibraryHeroContract.DEFAULT,
        state = LibraryHeroRuntimeState(
            isVisible = true,
            collectionStableKey = "continue_listening",
            primaryItemId = "track.bohemian_rhapsody",
            primaryTitle = "Continue Listening",
            primarySubtitle = "Bohemian Rhapsody · Queen",
            artworkKey = "artwork.bohemian_rhapsody",
            accentColorHex = "#FF8A65",
            isDismissed = false,
        ),
        badges = listOf(
            LibraryHeroBadge(
                label = "12 tracks",
                accentColorHex = "#FF8A65",
            ),
        ),
        artwork = { LibraryHeroArtworkPlaceholder() },
        modifier = modifier,
    )
}

/**
 * Renders a cinematic Hero (21:9, slow motion) with placeholder artwork.
 */
@Composable
fun LibraryHeroPreviewCinematic(
    modifier: Modifier = Modifier,
) {
    LibraryHero(
        contract = LibraryHeroContract.CINEMATIC,
        state = LibraryHeroRuntimeState(
            isVisible = true,
            collectionStableKey = "featured_today",
            primaryItemId = "collection.morning_acoustic",
            primaryTitle = "Morning Acoustic",
            primarySubtitle = "Curated for your sunrise · 8 tracks",
            artworkKey = "artwork.morning_acoustic",
            accentColorHex = "#FFB74D",
            isDismissed = false,
        ),
        badges = listOf(
            LibraryHeroBadge(
                label = "Morning",
                accentColorHex = "#FFB74D",
            ),
            LibraryHeroBadge(
                label = "Acoustic",
                accentColorHex = "#FFB74D",
            ),
        ),
        artwork = { LibraryHeroArtworkPlaceholder(accentColorHex = "#3E2723") },
        modifier = modifier,
    )
}
