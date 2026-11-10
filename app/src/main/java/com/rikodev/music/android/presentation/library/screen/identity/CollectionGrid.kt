/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.3 / Phase B — Identity Reveal
 *
 * File B6 / B9 — CollectionGrid.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [CollectionGrid] is the *grid layout* for Album / Artist / Folder
 * sections.
 *
 *   Albums → 2-column grid (catalog feel)
 *   Artists → 2-column grid (portrait cards)
 *   Folders → 2-column grid (compact cards)
 *
 * The Grid is the *catalog* layout. Where a Row invites exploration
 * ("scroll sideways to see more"), a Grid presents a *complete* view
 * ("here is everything in this section"). Grids are for browsing,
 * Rows are for suggesting.
 *
 * WHAT THIS FILE IS
 * -----------------
 * A composable that lays out [CollectionCardData] items in a 2-column
 * grid. Uses a [Column] of [Row]s (rather than LazyVerticalGrid) so
 * it can be hosted inside the screen's vertical scroll without
 * nested-scroll conflicts. The Grid renders up to [maxRows] rows;
 * excess items are dropped (caller shows "See all" via the Section
 * Header for the full catalog view).
 *
 * WHAT THIS FILE IS NOT
 * ---------------------
 * ❌ Not a row. Grid only — use [CollectionRow] for horizontal scroll.
 * ❌ Not a full catalog screen. "See all" navigation is the host's job.
 * ❌ Not a state owner. Reads card list, never writes.
 *
 * ARCHITECTURE
 * ------------
 *
 *   LibrarySection (Phase D)
 *        ↓ contains
 *   LibrarySectionHeader (B3) + CollectionGrid (B6 — THIS FILE)
 *        ↓ wraps
 *   Column (Compose)
 *        ↓ items
 *   Row of CollectionCard (B4) × 2
 *
 * RULES ENFORCED HERE
 * -------------------
 * #58  Runtime Never Knows Rendering   — this is rendering
 * #62  Never Rebuild Existing Systems  — uses Column/Row (Compose std)
 * #66  Collections Feel Discoverable   — grid spacing, consistent card size
 * #69  Sections Breathe Independently  — Grid has no shared state with Header
 *
 * © RIKO Experience OS — Rendering Tier (Phase B)
 */

package com.rikodev.music.android.presentation.library.screen.identity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.lazy.LazyColumn

// ─────────────────────────────────────────────────────────────────────────────
// COLLECTION GRID SPEC
//
// Aggregates the resolved values the Grid needs to render. Resolved
// once per (cards list, columns, maxRows) triple.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The fully-resolved CollectionGrid spec.
 *
 * @property columns        Number of columns (1, 2, or 3).
 * @property horizontalSpacingDp  Horizontal spacing between cards.
 * @property verticalSpacingDp    Vertical spacing between rows.
 * @property contentPadding       Outer padding around the grid.
 * @property maxRows              Maximum number of rows to render.
 *                                Excess items are dropped.
 */
internal data class CollectionGridSpec(
    val columns: Int,
    val horizontalSpacingDp: Int,
    val verticalSpacingDp: Int,
    val contentPadding: androidx.compose.foundation.layout.PaddingValues,
    val maxRows: Int,
) {
    companion object {

        /**
         * Resolves the Grid spec from the cards list + requested columns.
         *
         * Resolution rules:
         *
         *   - Columns: 2 by default (phone). Caller can request 1 or 3.
         *   - Horizontal spacing: 12dp (Rule #66 — minimum).
         *   - Vertical spacing: 16dp (slightly more than horizontal —
         *     rows should feel distinct, not crowded).
         *   - Content padding: 16dp horizontal (matches section padding).
         *   - Max rows: 3 (so 6 cards max for 2-column grid). Caller
         *     can override.
         */
        fun from(
            cards: List<CollectionCardData>,
            columns: Int,
            maxRows: Int,
        ): CollectionGridSpec {
            val resolvedColumns = columns.coerceIn(1, 3)

            return CollectionGridSpec(
                columns = resolvedColumns,
                horizontalSpacingDp = 12,
                verticalSpacingDp = 16,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                maxRows = maxRows.coerceAtLeast(1),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// THE COLLECTION GRID COMPOSABLE
//
// Public API. Renders a 2-column grid of CollectionCards.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * THE COLLECTION GRID.
 *
 * Renders a 2-column grid of [CollectionCard]s. Used by Album, Artist,
 * and Folder sections where a catalog view is preferred over a
 * horizontal scrolling row.
 *
 * ## Layout strategy
 *
 * The Grid is implemented as a [Column] of [Row]s (rather than
 * [androidx.compose.foundation.lazy.grid.LazyVerticalGrid]). This is
 * intentional:
 *
 *   - The Library screen has a single vertical scroll (LazyColumn in
 *     Phase D). LazyVerticalGrid inside a LazyColumn creates nested
 *     scroll conflicts.
 *   - The Grid renders a *bounded* number of items (maxRows × columns).
 *     For unbounded grids, the host navigates to a separate full-screen
 *     catalog view (Phase B doesn't implement that — it's a Phase D
 *     concern).
 *
 * ## Rule #66 — Collections Feel Discoverable
 *
 * - Horizontal spacing ≥ 12dp (Rule #66 minimum).
 * - Vertical spacing 16dp — rows feel distinct.
 * - Each card gets equal weight in its row (no card dominates).
 * - "See all" affordance comes from the Section Header (B3), not the
 *   Grid itself.
 *
 * ## Rule #69 — Sections Breathe Independently
 *
 * - The Grid is a pure function of its inputs. It does NOT share
 *   state with the section header. Each card's tap state is local to
 *   that card.
 *
 * ## Performance
 *
 * - Uses standard Column/Row — no lazy machinery overhead for a small
 *   bounded grid (max 9 cards = 3 rows × 3 cols).
 * - Cards are keyed by stable key (Rule #44) for stable identity.
 * - Each card's artwork slot is a Compose slot — caller controls
 *   allocation.
 *
 * @param cards        List of card data (already renderable, Rule #60).
 * @param columns      Number of columns (1, 2, or 3). Default: 2.
 * @param maxRows      Maximum rows to render. Excess cards dropped.
 *                     Default: 3 (so 6 cards max for 2-column grid).
 * @param artworkFor   Slot factory for artwork content.
 * @param onCardTap    Callback fired when a card is tapped.
 * @param modifier     Compose modifier.
 */
@Composable
fun CollectionGrid(
    cards: List<CollectionCardData>,
    artworkFor: @Composable (cardKey: String, accentColorHex: String) -> Unit,
    onCardTap: ((cardKey: String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    maxRows: Int = 3,
) {
    if (cards.isEmpty()) return

    val spec = remember(cards, columns, maxRows) {
        CollectionGridSpec.from(cards, columns, maxRows)
    }

    // Chunk cards into rows of `columns` items. Take only `maxRows` rows.
    val rows = remember(cards, spec.columns, spec.maxRows) {
        cards
            .chunked(spec.columns)
            .take(spec.maxRows)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(spec.contentPadding),
        verticalArrangement = Arrangement.spacedBy(spec.verticalSpacingDp.dp),
    ) {
        rows.forEach { rowCards ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spec.horizontalSpacingDp.dp),
            ) {
                rowCards.forEach { card ->
                    Box(modifier = Modifier.weight(1f)) {
                        CollectionCard(
                            data = card,
                            artwork = {
                                artworkFor(card.stableKey, card.accentColorHex)
                            },
                            onTap = if (onCardTap != null) {
                                { onCardTap(card.stableKey) }
                            } else null,
                        )
                    }
                }

                // If the last row is incomplete (fewer cards than columns),
                // add spacers to keep the layout grid-aligned.
                val missing = spec.columns - rowCards.size
                if (missing > 0) {
                    repeat(missing) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COLLECTION GRID PREVIEW HELPERS
//
// Plain composables that render the Grid with sample card data.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Sample card list for grid previews — 6 album-style cards.
 */
private val PreviewGridCards = listOf(
    CollectionCardData(
        stableKey = "album.1",
        title = "Album One",
        subtitle = "Artist A · 2023",
        accentColorHex = "#FF8A65",
        artworkKey = "artwork.1",
        count = "10",
        countIcon = null,
        personality = CollectionCardPersonality.MINIMAL,
    ),
    CollectionCardData(
        stableKey = "album.2",
        title = "Album Two",
        subtitle = "Artist B · 2022",
        accentColorHex = "#FFB74D",
        artworkKey = "artwork.2",
        count = "12",
        countIcon = null,
        personality = CollectionCardPersonality.MINIMAL,
    ),
    CollectionCardData(
        stableKey = "album.3",
        title = "Album Three",
        subtitle = "Artist C · 2024",
        accentColorHex = "#26A69A",
        artworkKey = "artwork.3",
        count = "8",
        countIcon = null,
        personality = CollectionCardPersonality.MINIMAL,
    ),
    CollectionCardData(
        stableKey = "album.4",
        title = "Album Four",
        subtitle = "Artist D · 2021",
        accentColorHex = "#7E57C2",
        artworkKey = "artwork.4",
        count = "15",
        countIcon = null,
        personality = CollectionCardPersonality.MINIMAL,
    ),
    CollectionCardData(
        stableKey = "album.5",
        title = "Album Five",
        subtitle = "Artist E · 2023",
        accentColorHex = "#42A5F5",
        artworkKey = "artwork.5",
        count = "9",
        countIcon = null,
        personality = CollectionCardPersonality.MINIMAL,
    ),
    CollectionCardData(
        stableKey = "album.6",
        title = "Album Six",
        subtitle = "Artist F · 2022",
        accentColorHex = "#EC407A",
        artworkKey = "artwork.6",
        count = "11",
        countIcon = null,
        personality = CollectionCardPersonality.MINIMAL,
    ),
)

/**
 * Renders the default Collection Grid with 6 sample cards in 2 columns.
 */
@Composable
fun CollectionGridPreviewDefault(
    modifier: Modifier = Modifier,
) {
    CollectionGrid(
        cards = PreviewGridCards,
        artworkFor = { _, accentHex ->
            CollectionCardArtworkPlaceholder(accentColorHex = accentHex)
        },
        onCardTap = { /* preview: no-op */ },
        modifier = modifier,
        columns = 2,
        maxRows = 3,
    )
}

/**
 * Renders a 3-column Collection Grid (denser layout for tablets).
 */
@Composable
fun CollectionGridPreviewDense(
    modifier: Modifier = Modifier,
) {
    CollectionGrid(
        cards = PreviewGridCards,
        artworkFor = { _, accentHex ->
            CollectionCardArtworkPlaceholder(accentColorHex = accentHex)
        },
        onCardTap = { /* preview: no-op */ },
        modifier = modifier,
        columns = 3,
        maxRows = 2,
    )
}
