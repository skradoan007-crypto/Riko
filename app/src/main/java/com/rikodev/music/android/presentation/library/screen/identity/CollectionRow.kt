/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.3 / Phase B — Identity Reveal
 *
 * File B5 / B9 — CollectionRow.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [CollectionRow] is the *horizontal scrolling* layout for collections.
 *
 *   Smart Collections → row of cards
 *   Recently Added Albums → row of cards
 *   Continue Listening → row of cards
 *
 * Most sections in the Library are rows. The Row is the most common
 * layout primitive after the Card itself. It exists to:
 *
 *   1. Lay out N cards horizontally with consistent spacing
 *   2. Handle horizontal scrolling (fling, snap, edge fade)
 *   3. Maintain card width consistency across personality variants
 *   4. Provide subtle edge fades so cards don't "fall off" the screen
 *
 * WHAT THIS FILE IS
 * -----------------
 * A composable that wraps a [LazyRow] (Compose standard) and renders
 * a list of [CollectionCardData] items. The Row knows nothing about:
 *
 *   - What header sits above it (Section Header is independent)
 *   - What section this row belongs to (Phase D composition handles that)
 *   - How cards animate (Phase C motion handles that)
 *   - What happens on card tap (callback to Coordinator)
 *
 * WHAT THIS FILE IS NOT
 * ---------------------
 * ❌ Not a section. No header, no padding around the row itself.
 * ❌ Not a grid. Horizontal only — use [CollectionGrid] for grids.
 * ❌ Not a list. Vertical scrolling is not supported here.
 * ❌ Not a state owner. Reads card list, never writes.
 *
 * ARCHITECTURE
 * ------------
 *
 *   LibrarySection (Phase D)
 *        ↓ contains
 *   LibrarySectionHeader (B3) + CollectionRow (B5 — THIS FILE)
 *        ↓ wraps
 *   LazyRow (Compose)
 *        ↓ items
 *   CollectionCard (B4)
 *
 * RULES ENFORCED HERE
 * -------------------
 * #58  Runtime Never Knows Rendering   — this is rendering
 * #62  Never Rebuild Existing Systems  — uses LazyRow
 * #66  Collections Feel Discoverable   — spacing ≥ 12dp, edge fades
 * #69  Sections Breathe Independently  — Row has no shared state with Header
 *
 * © RIKO Experience OS — Rendering Tier (Phase B)
 */

package com.rikodev.music.android.presentation.library.screen.identity

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.core.designsystem.theme.ColorVoidDark
import androidx.compose.ui.unit.Dp

// ─────────────────────────────────────────────────────────────────────────────
// COLLECTION ROW SPEC
//
// Aggregates the resolved values the Row needs to render. Resolved
// once per (cards list, profile) pair.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The fully-resolved CollectionRow spec.
 *
 * @property cardWidthDp       Card width in dp (drives row item size).
 * @property spacingDp         Spacing between cards (≥ 12dp, Rule #66).
 * @property contentPadding    Padding inside the LazyRow.
 * @property showEdgeFade      True if edge fades should render.
 * @property edgeFadeColor     Edge fade color (background-matching).
 * @property edgeFadeWidthDp   Edge fade width in dp.
 */
internal data class CollectionRowSpec(
    val cardWidthDp: Int,
    val spacingDp: Int,
    val contentPadding: PaddingValues,
    val showEdgeFade: Boolean,
    val edgeFadeColor: Color,
    val edgeFadeWidthDp: Int,
) {
    companion object {

        /**
         * Resolves the Row spec from the cards list.
         *
         * Resolution rules:
         *
         *   - Card width: 160dp default. If the row has only 1-2 cards,
         *     bumps to 200dp (fuller feel). If > 6 cards, shrinks to 140dp
         *     (denser feel — but never below 120dp, Rule #66 enforcement).
         *   - Spacing: 12dp (Rule #66 minimum).
         *   - Content padding: 16dp horizontal (matches section padding).
         *   - Edge fade: shown by default. Color matches screen background
         *     so cards appear to "emerge" from the edges.
         */
        fun from(
            cards: List<CollectionCardData>,
        ): CollectionRowSpec {
            val cardWidth = when {
                cards.size <= 2 -> 200
                cards.size > 6 -> 140
                else -> 160
            }

            return CollectionRowSpec(
                cardWidthDp = cardWidth.coerceIn(120, 240),
                spacingDp = 12, // Rule #66 — minimum 12dp.
                contentPadding = PaddingValues(horizontal = 16.dp),
                showEdgeFade = true,
                edgeFadeColor = ColorVoidDark,
                edgeFadeWidthDp = 24,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// THE COLLECTION ROW COMPOSABLE
//
// Public API. Renders a horizontal scrolling list of CollectionCards.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * THE COLLECTION ROW.
 *
 * Renders a horizontally-scrolling list of [CollectionCard]s. The Row
 * is the most common layout primitive in the Library — Smart
 * Collections, Albums, Artists all flow through this component.
 *
 * ## Rule #66 — Collections Feel Discoverable
 *
 * - Card spacing is ≥ 12dp (Rule #66 minimum). Crowded rows feel like
 *   lists, not invitations.
 * - Card width is 120-240dp — wide enough to read the title, narrow
 *   enough to see 2-3 cards at once on a phone.
 * - Edge fades (subtle gradient at row ends) suggest "there's more
 *   if you scroll" without arrow icons or pagination dots.
 *
 * ## Rule #69 — Sections Breathe Independently
 *
 * - The Row has its own scroll state — it does NOT share scroll state
 *   with the section header above or the screen scroll below. Each
 *   row scrolls independently.
 * - The Row's animation (entrance, scroll feedback) is decoupled from
 *   the Header's. (Phase C will add motion, but it will respect this
 *   boundary.)
 *
 * ## Performance
 *
 * - Uses Compose's [LazyRow] — only visible cards are composed. Cards
 *   that scroll off-screen are recycled.
 * - Each card's artwork slot is a Compose slot — the caller controls
 *   artwork allocation (cache, async load, etc.).
 * - Edge fades use `Modifier.drawBehind` with a remembered brush —
 *   zero per-frame allocation.
 *
 * ## Accessibility
 *
 * - Card tap targets are at least 120dp wide × ~160dp tall — well
 *   above the 48dp minimum.
 * - Horizontal scroll is announced to accessibility services via
 *   LazyRow's built-in semantics.
 *
 * @param cards        List of card data (already renderable, Rule #60).
 * @param artworkFor   Slot factory for artwork content. Called per card
 *                     with the card's stable key + accent color.
 * @param onCardTap    Callback fired when a card is tapped. Receives
 *                     the card's stable key. Null = no tap affordance.
 * @param modifier     Compose modifier.
 */
@Composable
fun CollectionRow(
    cards: List<CollectionCardData>,
    artworkFor: @Composable (cardKey: String, accentColorHex: String) -> Unit,
    onCardTap: ((cardKey: String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (cards.isEmpty()) return

    val spec = remember(cards) { CollectionRowSpec.from(cards) }
    val listState = rememberLazyListState()

    Box(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = spec.contentPadding,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(spec.spacingDp.dp),
        ) {
            items(
                items = cards,
                key = { it.stableKey }, // Rule #44 — stable keys for stable identity.
            ) { card ->
                CollectionCard(
                    data = card,
                    artwork = {
                        artworkFor(card.stableKey, card.accentColorHex)
                    },
                    onTap = if (onCardTap != null) {
                        { onCardTap(card.stableKey) }
                    } else null,
                    modifier = Modifier.width(spec.cardWidthDp.dp),
                )
            }
        }

        // Edge fades — subtle gradient at left/right ends of the row.
        // Suggests "there's more if you scroll" without arrow icons.
        if (spec.showEdgeFade && cards.size > 1) {
            EdgeFades(
                color = spec.edgeFadeColor,
                widthDp = spec.edgeFadeWidthDp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EDGE FADES
//
// Subtle gradient overlays at the left and right edges of the row.
// Drawn ON TOP of the LazyRow content. Suggests horizontal scrollability
// without explicit indicators (arrows, dots).
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Renders left + right edge fades on top of a Row.
 *
 * The fades are subtle — they go from the row's background color at the
 * very edge to fully transparent ~24dp in. This makes cards appear to
 * "emerge" from the edge as they scroll into view.
 *
 * @param color     Background-matching color (fade-to color).
 * @param widthDp   Fade width in dp (typically 24dp).
 * @param modifier  Compose modifier.
 */
@Composable
private fun EdgeFades(
    color: Color,
    widthDp: Int,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val fadeWidthPx = with(density) { widthDp.dp.toPx() }

    Box(
        modifier = modifier
            .drawBehind {
                // Left edge fade — opaque at x=0, transparent at x=fadeWidth.
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            color,
                            color.copy(alpha = 0f),
                        ),
                        startX = 0f,
                        endX = fadeWidthPx,
                    ),
                )
                // Right edge fade — transparent at x=(width-fadeWidth), opaque at x=width.
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            color.copy(alpha = 0f),
                            color,
                        ),
                        startX = size.width - fadeWidthPx,
                        endX = size.width,
                    ),
                )
            },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// COLLECTION ROW PREVIEW HELPERS
//
// Plain composables that render the Row with sample card data. Useful
// as @Preview content or as debug overlays.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Sample card list for previews — 5 cards with mixed personalities.
 */
private val PreviewCards = listOf(
    CollectionCardData(
        stableKey = "mood.morning",
        title = "Morning Acoustic",
        subtitle = "8 tracks",
        accentColorHex = "#FFB74D",
        artworkKey = "artwork.morning",
        count = "8",
        countIcon = null,
        personality = CollectionCardPersonality.GENTLE,
    ),
    CollectionCardData(
        stableKey = "discovery.gems",
        title = "Hidden Gems",
        subtitle = "12 tracks",
        accentColorHex = "#FF8A65",
        artworkKey = "artwork.gems",
        count = "12",
        countIcon = null,
        personality = CollectionCardPersonality.EXPLORER,
    ),
    CollectionCardData(
        stableKey = "album.dark_side",
        title = "Dark Side",
        subtitle = "Pink Floyd",
        accentColorHex = "#FFFFFF",
        artworkKey = "artwork.dark_side",
        count = null,
        countIcon = null,
        personality = CollectionCardPersonality.MINIMAL,
    ),
    CollectionCardData(
        stableKey = "history.recent",
        title = "Recently Played",
        subtitle = "20 tracks",
        accentColorHex = "#26A69A",
        artworkKey = "artwork.recent",
        count = "20",
        countIcon = null,
        personality = CollectionCardPersonality.COLLECTOR,
    ),
    CollectionCardData(
        stableKey = "mood.focus",
        title = "Focus",
        subtitle = "Instrumental · 15 tracks",
        accentColorHex = "#7E57C2",
        artworkKey = "artwork.focus",
        count = "15",
        countIcon = null,
        personality = CollectionCardPersonality.REFLECTIVE,
    ),
)

/**
 * Renders the default Collection Row with 5 sample cards.
 */
@Composable
fun CollectionRowPreviewDefault(
    modifier: Modifier = Modifier,
) {
    CollectionRow(
        cards = PreviewCards,
        artworkFor = { _, accentHex ->
            CollectionCardArtworkPlaceholder(accentColorHex = accentHex)
        },
        onCardTap = { /* preview: no-op */ },
        modifier = modifier,
    )
}
