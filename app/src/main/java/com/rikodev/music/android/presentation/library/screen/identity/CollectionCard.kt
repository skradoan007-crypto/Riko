/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.3 / Phase B — Identity Reveal
 *
 * File B4 / B9 — CollectionCard.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [CollectionCard] is the *most reusable* component in the Library.
 *
 *   Hero ছাড়া প্রায় সবখানে reuse হবে — এটাই workhorse.
 *
 * Every collection in every section is rendered as a CollectionCard.
 * Smart Collections, Albums, Artists, Folders, Playlists — all flow
 * through this one component. The card adapts its shape, accent, and
 * metadata layout based on the collection's personality (Rule #37 —
 * Collection Has Personality Before Data).
 *
 * WHAT THIS FILE IS
 * -----------------
 * A composable that renders a single collection card from a
 * [CollectionCardData] input. The card has:
 *
 *   1. Artwork (square or portrait, per personality)
 *   2. Title (always present)
 *   3. Subtitle (optional — context, count, or metadata)
 *   4. Optional badge (count, mood, time)
 *   5. Subtle accent glow on the artwork edge
 *
 * The card knows nothing about:
 *
 *   - Where it sits (Row / Grid / List handles that)
 *   - How it scrolls (LazyList handles that)
 *   - What happens on tap (callback to Coordinator)
 *   - How it animates (Phase C motion)
 *
 * WHAT THIS FILE IS NOT
 * ---------------------
 * ❌ Not a row. Single card only.
 * ❌ Not a grid cell. Single card only.
 * ❌ Not a list item. Single card only.
 * ❌ Not a state owner. Reads card data, never writes.
 *
 * ARCHITECTURE
 * ------------
 *
 *   CollectionCardData (B5.2 snapshot)
 *        ↓ consumed by
 *   CollectionCard (B4 — THIS FILE)
 *        ↓ wraps
 *   LibraryGlassSurface (CARD token, Phase A)
 *        ↓ inside
 *   Artwork + Title + Subtitle + Badge
 *
 * RULES ENFORCED HERE
 * -------------------
 * #58  Runtime Never Knows Rendering   — this is rendering
 * #62  Never Rebuild Existing Systems  — wraps GlassSurface, Color
 * #66  Collections Feel Discoverable   — subtitle + accent glow
 *
 * © RIKO Experience OS — Rendering Tier (Phase B)
 */

package com.rikodev.music.android.presentation.library.screen.identity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.music.android.core.designsystem.theme.AccentCyan
import com.rikodev.music.android.core.designsystem.theme.ColorVoidDark
import com.rikodev.music.android.presentation.library.screen.background.LibraryGlassSurface
import com.rikodev.music.android.presentation.library.screen.background.LibraryGlassToken
import com.rikodev.music.android.presentation.library.screen.background.toColorOrNull
import androidx.compose.ui.unit.Dp
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// COLLECTION CARD PERSONALITY
//
// Maps the B1 CollectionPersonality enum (9 variants) into concrete
// card visual treatments. The personality drives:
//
//   - Artwork aspect ratio (square vs portrait)
//   - Corner radius
//   - Accent glow intensity
//   - Subtitle visibility
//   - Badge prominence
//
// Each personality has a distinct *feel* — but the differences are
// SUBTLE. A user shouldn't be able to name them; they should just
// feel "this card is different from that one".
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The visual treatment of a CollectionCard, derived from the
 * collection's personality.
 *
 * @property artworkAspectRatioW  Width component of artwork aspect.
 * @property artworkAspectRatioH  Height component.
 * @property cornerRadiusDp       Card corner radius.
 * @property accentGlowAlpha      0.0–0.10. Subtle accent glow on artwork edge.
 * @property subtitleAlpha        0.0–1.0. Subtitle text opacity.
 * @property showBadge            True if badges should render.
 */
enum class CollectionCardPersonality(
    internal val artworkAspectRatioW: Float,
    internal val artworkAspectRatioH: Float,
    internal val cornerRadiusDp: Int,
    internal val accentGlowAlpha: Float,
    internal val subtitleAlpha: Float,
    internal val showBadge: Boolean,
) {
    /** Gentle — soft portrait artwork, warm accent. */
    GENTLE(
        artworkAspectRatioW = 4f,
        artworkAspectRatioH = 5f,
        cornerRadiusDp = 16,
        accentGlowAlpha = 0.06f,
        subtitleAlpha = 0.85f,
        showBadge = true,
    ),

    /** Explorer — square artwork, bold accent. */
    EXPLORER(
        artworkAspectRatioW = 1f,
        artworkAspectRatioH = 1f,
        cornerRadiusDp = 12,
        accentGlowAlpha = 0.10f,
        subtitleAlpha = 0.80f,
        showBadge = true,
    ),

    /** Teacher — square, neutral, no glow. */
    TEACHER(
        artworkAspectRatioW = 1f,
        artworkAspectRatioH = 1f,
        cornerRadiusDp = 12,
        accentGlowAlpha = 0.04f,
        subtitleAlpha = 0.90f,
        showBadge = false,
    ),

    /** StoryTeller — tall portrait, large title. */
    STORY_TELLER(
        artworkAspectRatioW = 3f,
        artworkAspectRatioH = 4f,
        cornerRadiusDp = 14,
        accentGlowAlpha = 0.08f,
        subtitleAlpha = 0.75f,
        showBadge = true,
    ),

    /** Minimal — square, no subtitle. */
    MINIMAL(
        artworkAspectRatioW = 1f,
        artworkAspectRatioH = 1f,
        cornerRadiusDp = 8,
        accentGlowAlpha = 0.00f,
        subtitleAlpha = 0.00f,
        showBadge = false,
    ),

    /** Collector — square, prominent count badge. */
    COLLECTOR(
        artworkAspectRatioW = 1f,
        artworkAspectRatioH = 1f,
        cornerRadiusDp = 12,
        accentGlowAlpha = 0.05f,
        subtitleAlpha = 0.80f,
        showBadge = true,
    ),

    /** Companion — square, warm tint. */
    COMPANION(
        artworkAspectRatioW = 1f,
        artworkAspectRatioH = 1f,
        cornerRadiusDp = 14,
        accentGlowAlpha = 0.07f,
        subtitleAlpha = 0.85f,
        showBadge = true,
    ),

    /** Energetic — square, strong accent glow. */
    ENERGETIC(
        artworkAspectRatioW = 1f,
        artworkAspectRatioH = 1f,
        cornerRadiusDp = 10,
        accentGlowAlpha = 0.10f,
        subtitleAlpha = 0.75f,
        showBadge = true,
    ),

    /** Reflective — portrait, dark tint, minimal text. */
    REFLECTIVE(
        artworkAspectRatioW = 4f,
        artworkAspectRatioH = 5f,
        cornerRadiusDp = 16,
        accentGlowAlpha = 0.05f,
        subtitleAlpha = 0.65f,
        showBadge = false,
    ),
}

// ─────────────────────────────────────────────────────────────────────────────
// COLLECTION CARD DATA
//
// The already-renderable card data. Rule #60 — Screen State Is Already
// Renderable. This data class contains NO logic — just strings, colors,
// and an icon reference. The Mapper (B5.2) produced this; the Card
// just renders it.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The renderable data for a single CollectionCard.
 *
 * All transformations (sorting, truncation, uppercase, count formatting)
 * have already happened in the Mapper (Rule #59 — Mapper Is The Last
 * Translator). This data class is *final* — the Card renders it as-is.
 *
 * @property stableKey         Stable identity (Rule #44).
 * @property title             Card title (already formatted).
 * @property subtitle          Card subtitle (already formatted, may be empty).
 * @property accentColorHex    Accent color (hex). Falls back to AccentCyan.
 * @property artworkKey        Stable key for the artwork loader.
 * @property count             Optional item count (already formatted as string).
 * @property countIcon         Optional icon next to count.
 * @property personality       Card personality — drives visual treatment.
 */
data class CollectionCardData(
    val stableKey: String,
    val title: String,
    val subtitle: String,
    val accentColorHex: String,
    val artworkKey: String,
    val count: String?,
    val countIcon: ImageVector?,
    val personality: CollectionCardPersonality,
) {
    companion object {
        /** A placeholder card — for previews and loading states. */
        val PLACEHOLDER = CollectionCardData(
            stableKey = "placeholder",
            title = "Loading...",
            subtitle = "",
            accentColorHex = "#FF8A65",
            artworkKey = "",
            count = null,
            countIcon = null,
            personality = CollectionCardPersonality.EXPLORER,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COLLECTION CARD COMPOSABLE
//
// Public API. Renders a single card with artwork + title + subtitle + badge.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * THE COLLECTION CARD.
 *
 * Renders a single collection card from [CollectionCardData]. The card
 * adapts its visual treatment based on the data's personality — artwork
 * aspect ratio, corner radius, accent glow, subtitle visibility.
 *
 * ## Rule #66 — Collections Feel Discoverable
 *
 * - Every card has at least a title. Subtitle is personality-dependent
 *   (Minimal personality hides it; others show it).
 * - Accent glow on the artwork edge is subtle (≤ 0.10 alpha, Rule #63).
 * - Count badge is small and accent-tinted — present but not loud.
 * - Corner radius ≥ 8dp (sharp cards feel cold — Rule #66 enforcement).
 *
 * ## Rule #60 — Screen State Is Already Renderable
 *
 * - The Card performs ZERO transformations on the data. No `.uppercase()`,
 *   no `.take()`, no `.sortedBy()`. The Mapper did all that. The Card
 *   just renders what it's given.
 *
 * ## Performance
 *
 * - Personality is read from the data — no per-frame resolution.
 * - Accent color is `remember`ed per hex string.
 * - Glass surface uses CARD token (Phase A) — smart shadow disabling
 *   for scrolling lists (cheaper than full elevation).
 *
 * @param data         Card data (already renderable).
 * @param artwork      Slot for the artwork content. Caller decides what
 *                     to draw (album art, gradient, illustration).
 * @param onTap        Tap callback. Null = no tap affordance.
 * @param modifier     Compose modifier.
 */
@Composable
fun CollectionCard(
    data: CollectionCardData,
    artwork: @Composable BoxScope.() -> Unit,
    onTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val personality = data.personality
    val accent = remember(data.accentColorHex) {
        data.accentColorHex.toColorOrNull() ?: AccentCyan
    }

    val pressSource = remember { MutableInteractionSource() }
    val isPressed by pressSource.collectIsPressedAsState()

    // Subtle press feedback — title brightens slightly when pressed.
    val titleAlpha = if (isPressed) 1.0f else 0.95f

    LibraryGlassSurface(
        token = LibraryGlassToken.CARD,
        modifier = modifier
            .clip(RoundedCornerShape(personality.cornerRadiusDp.dp))
            .then(
                if (onTap != null) {
                    Modifier.clickable(
                        interactionSource = pressSource,
                        indication = null,
                        onClick = onTap,
                    )
                } else Modifier,
            ),
        cornerRadius = personality.cornerRadiusDp.dp,
        enableShadow = false, // Cards in scroll lists — no real shadow (Phase A rule).
        content = {
            Column(
                modifier = Modifier.padding(8.dp),
            ) {
                // ── Artwork ─────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(personality.artworkAspectRatioW / personality.artworkAspectRatioH)
                        .clip(RoundedCornerShape((personality.cornerRadiusDp - 4).coerceAtLeast(0).dp)),
                ) {
                    // Artwork slot (caller-provided).
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        content = artwork,
                    )

                    // Accent glow overlay — subtle edge tint.
                    if (personality.accentGlowAlpha > 0f) {
                        val glowColor = remember(accent, personality.accentGlowAlpha) {
                            accent.copy(alpha = personality.accentGlowAlpha)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(glowColor),
                        )
                    }

                    // Count badge — top-end corner.
                    if (personality.showBadge && !data.count.isNullOrBlank()) {
                        CardCountBadge(
                            count = data.count,
                            icon = data.countIcon,
                            accent = accent,
                            modifier = Modifier.align(Alignment.TopEnd),
                        )
                    }
                }

                // ── Title + Subtitle ────────────────────────────────────────
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = data.title,
                    color = Color.White.copy(alpha = titleAlpha),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (personality.subtitleAlpha > 0f && data.subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = data.subtitle,
                        color = Color.White.copy(alpha = personality.subtitleAlpha),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// CARD COUNT BADGE
//
// A tiny chip in the artwork's top-end corner showing item count or
// metadata. Internal — only used by [CollectionCard].
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A small count badge for the CollectionCard.
 *
 * Positioned at the top-end corner of the artwork. Renders as a small
 * accent-tinted pill with optional icon + count text.
 *
 * ## Rule #66 — Collections Feel Discoverable
 *
 * The badge is small (max 28dp tall) and accent-tinted — present but
 * not loud. It tells the user "this collection has N items" without
 * screaming.
 *
 * @param count     Count text (already formatted — e.g. "12", "1.2k").
 * @param icon      Optional icon. Most badges have no icon.
 * @param accent    Accent color for tint + text.
 * @param modifier  Compose modifier.
 */
@Composable
private fun CardCountBadge(
    count: String,
    icon: ImageVector?,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val bgColor = remember(accent) {
        Color.Black.copy(alpha = 0.55f)
    }
    val tintColor = remember(accent) {
        accent.copy(alpha = 0.18f)
    }

    Row(
        modifier = modifier
            .padding(6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .background(tintColor)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.90f),
                modifier = Modifier.size(10.dp),
            )
        }
        Text(
            text = count,
            color = Color.White.copy(alpha = 0.95f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COLLECTION CARD ARTWORK PLACEHOLDER
//
// A no-artwork card — for previews and loading states. Paints a flat
// accent-tinted background.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A placeholder artwork slot — paints a flat accent-tinted background.
 *
 * Use this in `@Preview` or as a fallback while the real artwork loads.
 *
 * @param accentColorHex  Accent color (hex). Falls back to ColorVoidDark.
 * @param modifier        Compose modifier.
 */
@Composable
fun CollectionCardArtworkPlaceholder(
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
// COLLECTION CARD PREVIEW HELPERS
//
// Plain composables that render the card with each personality. Useful
// as @Preview content or as debug overlays.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Renders a Gentle-personality card (portrait artwork, warm accent).
 */
@Composable
fun CollectionCardPreviewGentle(
    modifier: Modifier = Modifier,
) {
    CollectionCard(
        data = CollectionCardData(
            stableKey = "mood.morning_acoustic",
            title = "Morning Acoustic",
            subtitle = "8 tracks · 32 min",
            accentColorHex = "#FFB74D",
            artworkKey = "artwork.morning_acoustic",
            count = "8",
            countIcon = null,
            personality = CollectionCardPersonality.GENTLE,
        ),
        artwork = { CollectionCardArtworkPlaceholder(accentColorHex = "#3E2723") },
        modifier = modifier,
    )
}

/**
 * Renders an Explorer-personality card (square artwork, bold accent).
 */
@Composable
fun CollectionCardPreviewExplorer(
    modifier: Modifier = Modifier,
) {
    CollectionCard(
        data = CollectionCardData(
            stableKey = "discovery.hidden_gems",
            title = "Hidden Gems",
            subtitle = "12 tracks you haven't heard",
            accentColorHex = "#FF8A65",
            artworkKey = "artwork.hidden_gems",
            count = "12",
            countIcon = null,
            personality = CollectionCardPersonality.EXPLORER,
        ),
        artwork = { CollectionCardArtworkPlaceholder(accentColorHex = "#1A237E") },
        modifier = modifier,
    )
}

/**
 * Renders a Minimal-personality card (square, no subtitle, no badge).
 */
@Composable
fun CollectionCardPreviewMinimal(
    modifier: Modifier = Modifier,
) {
    CollectionCard(
        data = CollectionCardData(
            stableKey = "album.dark_side",
            title = "The Dark Side of the Moon",
            subtitle = "", // Minimal hides subtitle.
            accentColorHex = "#FFFFFF",
            artworkKey = "artwork.dark_side",
            count = null,
            countIcon = null,
            personality = CollectionCardPersonality.MINIMAL,
        ),
        artwork = { CollectionCardArtworkPlaceholder(accentColorHex = "#212121") },
        modifier = modifier,
    )
}
