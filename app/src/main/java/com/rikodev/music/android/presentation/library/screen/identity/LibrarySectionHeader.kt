/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.3 / Phase B — Identity Reveal
 *
 * File B3 / B9 — LibrarySectionHeader.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [LibrarySectionHeader] is the *visual grammar* of every section
 * in the Library content slot.
 *
 *   Smart Collection → "Smart Picks for You"        [See all]
 *   Album            → "Recently Added Albums"      [See all]
 *   Artist           → "Artists You Love"           [See all]
 *   Folder           → "Folders"                    [See all]
 *
 * Every section starts with a header. The header is the *chapter
 * title* of that section's story beat (Rule #54 — Screen Tells a
 * Story). It orients the user: "you are now entering the Albums
 * chapter".
 *
 * The header is quiet authority. It is NOT a toolbar:
 *
 *   ❌ No icons (unless count badge)
 *   ❌ No background fill
 *   ❌ No borders
 *   ❌ No drop shadows
 *
 * Just title + optional subtitle + optional action chip. The eye
 * scans it in 200ms and moves on — that's the goal.
 *
 * WHAT THIS FILE IS
 * -----------------
 * A composable that renders a section header from the B5.1
 * [LibrarySectionHeaderStyle] contract. Knows nothing about:
 *
 *   - What cards live below it (that's the Row / Grid's job)
 *   - How the section collapses (that's Phase C motion)
 *   - What happens when "See all" is tapped (that's a callback)
 *
 * WHAT THIS FILE IS NOT
 * ---------------------
 * ❌ Not a section container. No body content hosted here.
 * ❌ Not a motion spec. Collapse animation lives in Phase C.
 * ❌ Not a state owner. Reads header style, never writes.
 *
 * ARCHITECTURE
 * ------------
 *
 *   ┌──────────────────────────────────────────────────────────┐
 *   │  LibrarySectionHeader (B3 — THIS FILE)                  │
 *   │  ┌──────────────┐  ┌─────────────────────────────────┐  │
 *   │  │ Accent Mark  │  │ Title                          │  │
 *   │  │ (small bar)  │  │ Subtitle (optional)            │  │
 *   │  └──────────────┘  └─────────────────────────────────┘  │
 *   │                                  ┌──────────────────┐   │
 *   │                                  │ Action Chip      │   │
 *   │                                  │ ("See all")      │   │
 *   │                                  └──────────────────┘   │
 *   └──────────────────────────────────────────────────────────┘
 *
 * RULES ENFORCED HERE
 * -------------------
 * #58  Runtime Never Knows Rendering   — this is rendering
 * #62  Never Rebuild Existing Systems  — wraps GlassSurface for chip
 * #66  Collections Feel Discoverable   — header invites, doesn't shout
 * #69  Sections Breathe Independently  — header is decoupled from body
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rikodev.music.android.core.designsystem.theme.AccentCyan
import com.rikodev.music.android.presentation.library.screen.background.LibraryGlassSurface
import com.rikodev.music.android.presentation.library.screen.background.LibraryGlassToken
import com.rikodev.music.android.presentation.library.screen.background.toColorOrNull
import com.rikodev.music.android.presentation.library.screen.contract.LibrarySectionHeaderStyle
import com.rikodev.music.android.presentation.library.screen.contract.LibrarySectionKind
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// SECTION HEADER RESOLVED SPEC
//
// Aggregates the resolved values the header needs to render. Resolved
// once per header style + section kind pair.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The fully-resolved section header spec.
 *
 * @property title             Title text (already localized by Mapper).
 * @property subtitle          Optional subtitle (already localized).
 * @property actionLabel       Optional action label ("See all").
 * @property actionKey         Optional stable key for action routing.
 * @property accentColor       Accent color for the title mark + action chip.
 * @property isVisible         True if the header should render.
 * @property titleSizeSp       Title font size in sp.
 * @property subtitleSizeSp    Subtitle font size in sp.
 * @property hasAccentMark     True if a small accent bar precedes the title.
 */
internal data class LibrarySectionHeaderSpec(
    val title: String,
    val subtitle: String?,
    val actionLabel: String?,
    val actionKey: String?,
    val accentColor: Color,
    val isVisible: Boolean,
    val titleSizeSp: Float,
    val subtitleSizeSp: Float,
    val hasAccentMark: Boolean,
) {
    companion object {

        /**
         * Resolves a [LibrarySectionHeaderStyle] + [LibrarySectionKind]
         * into a [LibrarySectionHeaderSpec].
         *
         * Resolution rules:
         *
         *   - Title size: 18sp for HERO/SMART (premium feel),
         *                 16sp for ALBUM/ARTIST/FOLDER (catalog feel),
         *                 14sp for GENERIC (subdued).
         *   - Subtitle size: always title - 4sp.
         *   - Accent mark: shown only for SMART_COLLECTION and HERO sections
         *     (these are the "curated" sections — they earn the mark).
         *   - Accent color: from header style, falls back to AccentCyan.
         */
        fun from(
            style: LibrarySectionHeaderStyle,
            kind: LibrarySectionKind,
        ): LibrarySectionHeaderSpec {
            val titleSize = when (kind) {
                LibrarySectionKind.HERO,
                LibrarySectionKind.SMART_COLLECTION,
                -> 18f
                LibrarySectionKind.ALBUM_GRID,
                LibrarySectionKind.ARTIST_SECTION,
                LibrarySectionKind.FOLDER_SECTION,
                LibrarySectionKind.PLAYLIST_SECTION,
                LibrarySectionKind.GENRE_SECTION,
                LibrarySectionKind.TIME_PERIOD_SECTION,
                -> 16f
                LibrarySectionKind.GENERIC -> 14f
            }

            return LibrarySectionHeaderSpec(
                title = style.titleText,
                subtitle = style.subtitleText,
                actionLabel = style.actionLabel,
                actionKey = style.actionKey,
                accentColor = style.accentColorHex.toColorOrNull() ?: AccentCyan,
                isVisible = style.isVisible,
                titleSizeSp = titleSize,
                subtitleSizeSp = titleSize - 4f,
                hasAccentMark = kind == LibrarySectionKind.SMART_COLLECTION ||
                    kind == LibrarySectionKind.HERO,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// THE SECTION HEADER COMPOSABLE
//
// Public API. Renders title + optional subtitle + optional action chip.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * THE LIBRARY SECTION HEADER.
 *
 * Renders a section header from the B5.1 [LibrarySectionHeaderStyle]
 * contract. The header is the chapter title of the section — large
 * enough to scan in 200ms, quiet enough to never compete with the
 * cards below.
 *
 * ## Rule #66 — Collections Feel Discoverable
 *
 * - Title is white at 100% opacity — readable, but not bold-heavy.
 * - Subtitle is muted (60% white) — supporting context, not competing.
 * - Accent mark (small 3dp-wide vertical bar) only appears for curated
 *   sections (SMART_COLLECTION, HERO). Catalog sections (ALBUM, ARTIST,
 *   FOLDER) don't get the mark — they are factual, not curated.
 * - Action chip is a glass pill — quiet affordance, not a button.
 *
 * ## Rule #69 — Sections Breathe Independently
 *
 * - The header is a pure function of its inputs. It does NOT share
 *   state with the section body. When the body collapses, the header
 *   stays. When the body animates, the header is decoupled.
 * - This is enforced by the header having NO motion of its own — it
 *   is purely declarative. Motion (entrance, collapse) is added by
 *   Phase C wrappers, not here.
 *
 * ## Performance
 *
 * - The spec is `remember`ed per (style, kind) pair — zero per-frame
 *   resolution.
 * - The accent mark is a `Modifier.background(color)` — cheapest
 *   possible visual.
 * - The action chip uses [LibraryGlassSurface] with the CHIP token
 *   (Phase A) — wraps existing glass engine, no new chrome.
 *
 * @param style         Header style from B5.1.
 * @param kind          Section kind — drives title size + accent mark.
 * @param onActionTap   Callback fired when the action chip is tapped.
 *                      Receives the action's stable key. Null if no action.
 * @param modifier      Compose modifier.
 */
@Composable
fun LibrarySectionHeader(
    style: LibrarySectionHeaderStyle,
    kind: LibrarySectionKind,
    onActionTap: ((actionKey: String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val spec = remember(style, kind) {
        LibrarySectionHeaderSpec.from(style, kind)
    }

    if (!spec.isVisible || spec.title.isBlank()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ── Left side: accent mark + title + subtitle ────────────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            // Accent mark — small vertical bar. Only for curated sections.
            if (spec.hasAccentMark) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(spec.accentColor.copy(alpha = 0.70f)),
                )
            }

            Column {
                Text(
                    text = spec.title,
                    color = Color.White,
                    fontSize = spec.titleSizeSp.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (!spec.subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = spec.subtitle,
                        color = Color.White.copy(alpha = 0.60f),
                        fontSize = spec.subtitleSizeSp.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // ── Right side: action chip ("See all") ──────────────────────────
        if (!spec.actionLabel.isNullOrBlank() && !spec.actionKey.isNullOrBlank()) {
            val actionKey = spec.actionKey
            SectionHeaderActionChip(
                label = spec.actionLabel,
                accent = spec.accentColor,
                onTap = if (onActionTap != null) {
                    { onActionTap(actionKey) }
                } else null,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION HEADER ACTION CHIP
//
// A small glass pill that says "See all" (or similar). Internal —
// only used by [LibrarySectionHeader].
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A small glass action chip for the section header.
 *
 * Uses [LibraryGlassSurface] with the CHIP token (Phase A) — ultra-thin
 * glass, subtle border, just enough presence to be touched without
 * ever drawing the eye away from the title.
 *
 * ## Rule #66 — Collections Feel Discoverable
 *
 * The chip is an *invitation*, not a button. It says "there's more
 * here if you want it" — quiet, optional, never demanding.
 *
 * @param label    Chip label (e.g. "See all").
 * @param accent   Accent color for the chip text + subtle tint.
 * @param onTap    Tap callback. Null = no tap affordance.
 */
@Composable
private fun SectionHeaderActionChip(
    label: String,
    accent: Color,
    onTap: (() -> Unit)? = null,
) {
    val pressSource = remember { MutableInteractionSource() }
    val isPressed by pressSource.collectIsPressedAsState()

    // Subtle press feedback — text brightens slightly when pressed.
    val textAlpha = if (isPressed) 1.0f else 0.85f

    LibraryGlassSurface(
        token = LibraryGlassToken.CHIP,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .then(
                if (onTap != null) {
                    Modifier.clickable(
                        interactionSource = pressSource,
                        indication = null,
                        onClick = onTap,
                    )
                } else Modifier,
            ),
        cornerRadius = 999.dp,
        tintColor = accent.copy(alpha = 0.06f), // Rule #63 — very low tint.
        content = {
            Text(
                text = label,
                color = Color.White.copy(alpha = textAlpha),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION HEADER PREVIEW HELPERS
//
// Plain composables that render the header with each section kind.
// Useful as @Preview content or as debug overlays.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Renders the default section header (no action, no accent mark).
 */
@Composable
fun LibrarySectionHeaderPreviewDefault(
    modifier: Modifier = Modifier,
) {
    LibrarySectionHeader(
        style = LibrarySectionHeaderStyle.DEFAULT.copy(
            titleText = "Recently Played",
            subtitleText = "12 tracks",
        ),
        kind = LibrarySectionKind.ALBUM_GRID,
        modifier = modifier,
    )
}

/**
 * Renders a Smart Collection header — with accent mark + "See all".
 */
@Composable
fun LibrarySectionHeaderPreviewSmart(
    modifier: Modifier = Modifier,
) {
    LibrarySectionHeader(
        style = LibrarySectionHeaderStyle.withSeeAll(
            title = "Smart Picks for You",
            actionKey = "seeAll.smart",
        ).copy(
            subtitleText = "Curated from your last 30 days",
            accentColorHex = "#FFB74D",
        ),
        kind = LibrarySectionKind.SMART_COLLECTION,
        onActionTap = { /* preview: no-op */ },
        modifier = modifier,
    )
}

/**
 * Renders a Folder section header — no accent mark, with action.
 */
@Composable
fun LibrarySectionHeaderPreviewFolder(
    modifier: Modifier = Modifier,
) {
    LibrarySectionHeader(
        style = LibrarySectionHeaderStyle.withSeeAll(
            title = "Folders",
            actionKey = "seeAll.folders",
        ),
        kind = LibrarySectionKind.FOLDER_SECTION,
        onActionTap = { /* preview: no-op */ },
        modifier = modifier,
    )
}
