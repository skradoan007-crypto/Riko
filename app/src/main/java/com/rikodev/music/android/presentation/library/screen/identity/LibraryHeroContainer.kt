/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.3 / Phase B — Identity Reveal
 *
 * File B2 / B9 — LibraryHeroContainer.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [LibraryHeroContainer] is the *frame* around the Hero card.
 *
 *   Hero নিজে শুধু face — Container হলো তার posture.
 *
 * The Hero card (B1) knows how to draw itself — artwork, text, badges.
 * But it doesn't know:
 *
 *   • Where on screen it sits (top margin, horizontal margin)
 *   • What glass material wraps it (HERO token, profile-aware)
 *   • What depth stratum it occupies (FRONT — closest to the user)
 *   • How it relates to the Background Host below it
 *   • How it visually responds to press (subtle dimming)
 *   • How its subtle effects (light, breathing) are layered
 *
 * That's the Container's job. The Container is the *spatial contract*
 * between the Hero card and the rest of the screen.
 *
 * WHAT THIS FILE IS
 * -----------------
 * A composable that wraps [LibraryHero] (B1) with:
 *
 *   1. Spacing — top/bottom/horizontal margins from [LibraryHeroContract]
 *   2. Glass — [LibraryGlassSurfaceBorderless] with the profile-resolved
 *      HERO token (Phase A file A2)
 *   3. Depth — [LibraryDepth.FRONT] modifier for visual lift (Phase A file A3)
 *   4. Press feedback — a subtle dim overlay when the Hero is pressed
 *   5. Effects slot — composes [LibraryHeroEffects] (B9) on top
 *   6. Visibility guard — renders nothing if the Hero is dismissed
 *
 * WHAT THIS FILE IS NOT
 * ---------------------
 * ❌ Not the Hero card itself — that's [LibraryHero] (B1).
 * ❌ Not the Background — that's [LibraryBackgroundHost] (Phase A).
 * ❌ Not the motion spec — entrance + parallax live in Phase C.
 * ❌ Not the state owner — reads state, never writes.
 *
 * ARCHITECTURE
 * ------------
 *
 *   LibraryBackgroundHost (Phase A)
 *        ↑ paints on top
 *   LibraryHeroContainer (B2 — THIS FILE)
 *        ↑ wraps
 *   LibraryGlassSurfaceBorderless (Phase A — HERO token)
 *        ↑ inside
 *   LibraryHeroEffects (B9 — subtle light + breathing)
 *        ↑ inside
 *   LibraryHero (B1 — the card)
 *
 * The Container is the ONLY file that knows about all of: spacing,
 * glass, depth, effects, and the Hero card. Phase D's root composable
 * places the Container at the top of the content slot — it doesn't
 * need to know anything else.
 *
 * RULES ENFORCED HERE
 * -------------------
 * #58  Runtime Never Knows Rendering   — this is rendering
 * #62  Never Rebuild Existing Systems  — wraps GlassSurface + Hero
 * #65  Hero Never Competes             — bottom margin ≥ 24dp from Collections
 * #68  Hero Explains Library           — passes state through unchanged
 *
 * © RIKO Experience OS — Rendering Tier (Phase B)
 */

package com.rikodev.music.android.presentation.library.screen.identity

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rikodev.music.android.presentation.library.screen.background.LibraryGlassSurfaceBorderless
import com.rikodev.music.android.presentation.library.screen.background.LibraryGlassToken
import com.rikodev.music.android.presentation.library.screen.background.libraryDepth
import com.rikodev.music.android.presentation.library.screen.background.resolveHeroToken
import com.rikodev.music.android.presentation.library.screen.contract.LibraryConstitutionProfile
import com.rikodev.music.android.presentation.library.screen.contract.LibraryHeroContract
import com.rikodev.music.android.presentation.library.screen.runtime.LibraryHeroRuntimeState
import androidx.compose.ui.unit.Dp
import androidx.compose.animation.core.Animatable

// ─────────────────────────────────────────────────────────────────────────────
// HERO CONTAINER RESOLVED SPEC
//
// Aggregates everything the Container needs to know to render the Hero
// correctly. Resolved once per (contract, profile) pair.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The fully-resolved Hero container spec — concrete values derived
 * from the contract + profile.
 *
 * @property glassToken       Library glass token to use (HERO or SECTION,
 *                            depending on profile).
 * @property cornerRadiusDp   Corner radius for the glass surface.
 * @property margins          Padding around the Hero card.
 * @property pressDimAlpha    How much to dim the Hero when pressed (0.0–0.30).
 * @property isVisible        True if the Hero should render at all.
 */
internal data class LibraryHeroContainerSpec(
    val glassToken: LibraryGlassToken,
    val cornerRadiusDp: androidx.compose.ui.unit.Dp,
    val margins: PaddingValues,
    val pressDimAlpha: Float,
    val isVisible: Boolean,
) {
    companion object {

        /**
         * Resolves a [LibraryHeroContract] + [LibraryConstitutionProfile]
         * into a [LibraryHeroContainerSpec].
         *
         * Resolution rules:
         *
         *   - Glass token: profile.resolveHeroToken() (CINEMATIC/HIGH_END
         *     → HERO real blur, else → SECTION lighter glass).
         *   - Corner radius: from the contract's cardShape (24dp default).
         *   - Margins: from the contract's horizontalMarginDp + topMarginDp
         *     + bottomMarginDp. Bottom margin is enforced ≥ 24dp (Rule #65
         *     — Hero must have breathing room above the first Collection).
         *   - Press dim alpha: 0.18 (subtle, not jarring).
         *   - Visible: contract.isVisible AND state.isVisible AND !state.isDismissed.
         */
        fun from(
            contract: LibraryHeroContract,
            profile: LibraryConstitutionProfile,
            state: LibraryHeroRuntimeState,
        ): LibraryHeroContainerSpec {
            val resolvedBottomMargin = contract.bottomMarginDp.coerceAtLeast(24)

            return LibraryHeroContainerSpec(
                glassToken = profile.resolveHeroToken(),
                cornerRadiusDp = contract.cardShape.cornerRadiusDp.dp,
                margins = PaddingValues(
                    start = contract.horizontalMarginDp.dp,
                    end = contract.horizontalMarginDp.dp,
                    top = contract.topMarginDp.dp,
                    bottom = resolvedBottomMargin.dp,
                ),
                pressDimAlpha = 0.18f,
                isVisible = contract.isVisible && state.isVisible && !state.isDismissed,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// THE HERO CONTAINER COMPOSABLE
//
// Public API. Composes glass + depth + Hero card + effects + press
// feedback into a single frame around the Hero.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * THE LIBRARY HERO CONTAINER.
 *
 * Wraps [LibraryHero] (B1) with:
 *
 *   1. Spacing (margins from the contract)
 *   2. Glass material (HERO token, profile-resolved)
 *   3. Depth stratum (FRONT — closest to the user)
 *   4. Press feedback (subtle dim overlay when pressed)
 *   5. Effects slot (composes [LibraryHeroEffects] on top)
 *
 * ## Rule #65 — Hero Never Competes
 *
 * - Bottom margin is enforced ≥ 24dp so the Hero has clear separation
 *   from the first Collection below it.
 * - The glass glow comes from the HERO token (already capped at 0.50
 *   in GlassTokens.Hero — Phase A).
 * - Press feedback is subtle (0.18 alpha dim) — not a flash, not a
 *   ripple, just a "I felt that" whisper.
 *
 * ## Rule #68 — Hero Explains Library
 *
 * - The Container does NOT modify the state it receives. It passes
 *   the state straight through to [LibraryHero]. The Hero is the
 *   voice; the Container is the stage.
 *
 * ## Visibility
 *
 * If `contract.isVisible == false` OR `state.isVisible == false` OR
 * `state.isDismissed == true`, the Container renders nothing — not
 * even empty space. This allows the host to fully remove the Hero
 * from the layout (e.g. in fullscreen mode, or after the user
 * dismisses it).
 *
 * ## Performance
 *
 * - The Container spec is `remember`ed per (contract, profile, state)
 *   triple — zero per-frame resolution when inputs are stable.
 * - The press interaction source is `remember`ed — one allocation
 *   per Hero instance, not per recomposition.
 * - The dim overlay uses `Modifier.background(color)` — the cheapest
 *   possible visual feedback.
 *
 * @param contract      Hero contract from B5.1.
 * @param state         Hero runtime state from B5.2.
 * @param profile       Active constitution profile (drives glass token).
 * @param badges        Badges to forward to [LibraryHero]. Capped at 3 there.
 * @param artwork       Artwork slot — forwarded to [LibraryHero].
 * @param onTap         Tap callback — forwarded to [LibraryHero].
 * @param effects       Optional effects layer (typically [LibraryHeroEffects]
 *                      from B9). Drawn ON TOP of the Hero card but BELOW
 *                      the press dim overlay. Default: no effects.
 * @param modifier      Compose modifier.
 */
@Composable
fun LibraryHeroContainer(
    contract: LibraryHeroContract,
    state: LibraryHeroRuntimeState,
    profile: LibraryConstitutionProfile,
    badges: List<LibraryHeroBadge> = emptyList(),
    artwork: @Composable BoxScope.() -> Unit,
    onTap: (() -> Unit)? = null,
    effects: (@Composable BoxScope.() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val spec = remember(contract, profile, state) {
        LibraryHeroContainerSpec.from(
            contract = contract,
            profile = profile,
            state = state,
        )
    }

    // Fast path: if the Hero is not visible, render nothing.
    if (!spec.isVisible) return

    // Press interaction source — shared between the click handler and
    // the dim overlay. One allocation per Hero instance.
    val pressSource = remember { MutableInteractionSource() }
    val isPressed by pressSource.collectIsPressedAsState()

    // Dim overlay color — only nonzero alpha when pressed.
    val dimColor = remember(spec.pressDimAlpha) {
        Color.Black.copy(alpha = spec.pressDimAlpha)
    }
    val effectiveDimAlpha = if (isPressed) 1f else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(spec.margins),
    ) {
        LibraryGlassSurfaceBorderless(
            token = spec.glassToken,
            cornerRadius = spec.cornerRadiusDp,
            // Apply depth stratum FRONT — Hero is the closest content
            // to the user. (Phase A file A3 — LibraryDepthLayer.kt.)
            modifier = Modifier.libraryDepth(
                depth = com.rikodev.music.android.presentation.library.screen.background.LibraryDepth.FRONT,
                cornerRadius = spec.cornerRadiusDp,
            ),
            content = {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Layer 1: The Hero card (B1).
                    LibraryHero(
                        contract = contract,
                        state = state,
                        badges = badges,
                        artwork = artwork,
                        onTap = onTap,
                        interactionSource = pressSource,
                    )

                    // Layer 2: Effects (B9 — subtle light, breathing, reflection).
                    // Drawn ON TOP of the Hero card. Caller-provided so this
                    // file doesn't depend on B9 directly (avoids a circular
                    // import risk if B9 is ever refactored).
                    if (effects != null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            content = effects,
                        )
                    }

                    // Layer 3: Press dim overlay.
                    // Drawn LAST so it sits on top of everything. Alpha
                    // animates from 0 to 1 on press — the press-state visual.
                    if (spec.pressDimAlpha > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(dimColor.copy(alpha = dimColor.alpha * effectiveDimAlpha)),
                        )
                    }
                }
            },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HERO CONTAINER PREVIEW HELPERS
//
// Plain composables that render the Container with each profile. Useful
// as @Preview content or as debug overlays.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Renders the default Hero Container with placeholder artwork.
 */
@Composable
fun LibraryHeroContainerPreviewDefault(
    modifier: Modifier = Modifier,
) {
    LibraryHeroContainer(
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
        profile = LibraryConstitutionProfile.DEFAULT,
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
 * Renders the cinematic Hero Container (21:9, real backdrop blur on
 * high-end devices) with placeholder artwork.
 */
@Composable
fun LibraryHeroContainerPreviewCinematic(
    modifier: Modifier = Modifier,
) {
    LibraryHeroContainer(
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
        profile = LibraryConstitutionProfile.CINEMATIC,
        badges = listOf(
            LibraryHeroBadge(
                label = "Morning",
                accentColorHex = "#FFB74D",
            ),
        ),
        artwork = { LibraryHeroArtworkPlaceholder(accentColorHex = "#3E2723") },
        modifier = modifier,
    )
}

/**
 * Renders the AMOLED Hero Container — pure dark, no glow.
 */
@Composable
fun LibraryHeroContainerPreviewAmoled(
    modifier: Modifier = Modifier,
) {
    LibraryHeroContainer(
        contract = LibraryHeroContract.DEFAULT,
        state = LibraryHeroRuntimeState(
            isVisible = true,
            collectionStableKey = "continue_listening",
            primaryItemId = "track.bohemian_rhapsody",
            primaryTitle = "Continue Listening",
            primarySubtitle = "Bohemian Rhapsody · Queen",
            artworkKey = "artwork.bohemian_rhapsody",
            accentColorHex = "#1A1A2E",
            isDismissed = false,
        ),
        profile = LibraryConstitutionProfile.AMOLED,
        badges = emptyList(),
        artwork = { LibraryHeroArtworkPlaceholder(accentColorHex = "#000000") },
        modifier = modifier,
    )
}
