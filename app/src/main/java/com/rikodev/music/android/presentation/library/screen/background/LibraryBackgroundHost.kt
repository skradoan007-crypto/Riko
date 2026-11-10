/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B5.3 / Phase A — Background Language
 *
 * File A5 / A5 — LibraryBackgroundHost.kt
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * [LibraryBackgroundHost] is the *single entry point* for the entire
 * Phase A background stack.
 *
 *   Background
 *      ↓
 *   Glass
 *      ↓
 *   Depth
 *      ↓
 *   Ambient
 *      ↓
 *   Expose ONE composable
 *
 * অন্য কেউ A1-A4 জানবে না।
 * সবাই শুধু Host use করবে।
 *
 * WHAT THIS FILE IS
 * -----------------
 * A composable that composes A1 (background), A3 (depth), and A4
 * (ambient) into a single full-screen layer. Phase B composables
 * (Hero, Cards, Dock) place themselves *on top of* this host — they
 * don't need to know anything about background internals.
 *
 * Glass (A2) is NOT composed here — glass is a *material* used by
 * surfaces, not a *layer* in the background stack. Phase B surfaces
 * use [LibraryGlassSurface] (from A2) when they need glass.
 *
 * ARCHITECTURE
 * ------------
 *
 *   ┌──────────────────────────────────────┐
 *   │  LibraryBackgroundHost (A5)          │  ← one entry point
 *   │  ┌────────────────────────────────┐  │
 *   │  │  LibraryBackground (A1)        │  │  ← base color + gradient
 *   │  │  ┌──────────────────────────┐  │  │
 *   │  │  │  LibraryDepthStack (A3)  │  │  │  ← subtle tint layers
 *   │  │  │  ┌────────────────────┐  │  │  │
 *   │  │  │  │ LibraryAmbient (A4)│  │  │  │  ← breathing layer
 *   │  │  │  └────────────────────┘  │  │  │
 *   │  │  └──────────────────────────┘  │  │
 *   │  └────────────────────────────────┘  │
 *   └──────────────────────────────────────┘
 *
 * Each layer is `fillMaxSize` and positioned absolutely — the host
 * is a [Box] so children stack naturally. The order of composition
 * determines Z-order: A1 is back, A4 is front (within the background
 * stack — content from Phase B will sit on top of A4).
 *
 * RULES ENFORCED HERE
 * -------------------
 * #58  Runtime Never Knows Rendering   — this is rendering
 * #62  Never Rebuild Existing Systems  — composes A1/A3/A4 only
 * #63  Background Never Demands Attention — passes through to children
 * #64  Motion Must Be Discoverable, Not Obvious — passes through to A4
 *
 * © RIKO Experience OS — Rendering Tier (Phase A)
 */

package com.rikodev.music.android.presentation.library.screen.background

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.rikodev.music.android.presentation.library.screen.contract.LibraryConstitution
import com.rikodev.music.android.presentation.library.screen.contract.LibraryConstitutionProfile
import com.rikodev.music.android.presentation.library.screen.contract.LibraryMotionLanguage

// ─────────────────────────────────────────────────────────────────────────────
// HOST COMPOSABLE
//
// The single public API of Phase A. Reads a [LibraryConstitution] and
// paints the full background stack.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * THE LIBRARY BACKGROUND HOST.
 *
 * Paints the full background stack of the Library screen:
 *
 *   1. [LibraryBackground] (A1) — base color + gradient
 *   2. [LibraryDepthStack] (A3) — subtle tint layers for depth
 *   3. [LibraryAmbientLayer] (A4) — slow breathing radial
 *
 * Glass (A2) is intentionally NOT composed here. Glass is a *material*
 * used by individual surfaces (Hero, Cards, Dock) — it is not part of
 * the background layer stack. Phase B surfaces call [LibraryGlassSurface]
 * directly when they need glass.
 *
 * ## Usage
 *
 * Phase B's root composable ([LibraryScreenComposable], to be written
 * in Phase D) will look like:
 *
 *   Box(modifier = Modifier.fillMaxSize()) {
 *       LibraryBackgroundHost(constitution = state.constitution)
 *       // ... Top Bar, Hero, Sections, Dock ...
 *   }
 *
 * The host fills the entire screen. Content draws on top via standard
 * Compose Z-ordering (later children draw on top of earlier ones).
 *
 * ## Performance
 *
 * - **Zero allocations per frame** when the constitution is unchanged.
 *   Each layer `remember`s its inputs, so a recomposition that keeps
 *   the same constitution re-uses all brushes and gradients.
 *
 * - **Conditional layers**: if the constitution's profile is `LOW_END`,
 *   the ambient layer is replaced with [LibraryAmbientPreset.OFF] —
 *   no `InfiniteTransition`, no draw call. Background becomes a
 *   flat color + depth stack only.
 *
 * - **Reduce-motion respected**: when
 *   `constitution.motionLanguage.isReduceMotionEnabled` is true,
 *   the ambient layer renders as a static radial — no animation.
 *
 * ## Rules
 *
 * - **Rule #58**: this is rendering. Compose imports are allowed.
 * - **Rule #62**: this composable composes A1/A3/A4 — it does NOT
 *   re-implement anything they do.
 * - **Rule #63**: the layered result has a peak alpha ≤ 0.10 (the
 *   ambient peak). The base color and depth tints are pure color
 *   values, not alpha overlays — they don't add to the ambient's
 *   alpha budget.
 * - **Rule #64**: motion comes only from A4. A1 and A3 are static.
 *
 * @param constitution  The Library constitution — provides the screen
 *                      background contract, motion language, and
 *                      profile. The host reads:
 *                      - `constitution.screen.background` (for A1)
 *                      - `constitution.profile` (for A3 / A4 mode)
 *                      - `constitution.motionLanguage` (for A4)
 * @param modifier      Compose modifier for layout. Default: fillMaxSize.
 */
@Composable
fun LibraryBackgroundHost(
    constitution: LibraryConstitution,
    modifier: Modifier = Modifier,
) {
    val background = constitution.screen.background
    val profile = constitution.profile
    val motionLanguage = constitution.motionLanguage

    // Resolve ambient preset once per constitution. `remember` keyed on
    // the relevant inputs so a profile change re-resolves.
    val ambientPreset = remember(motionLanguage, profile) {
        motionLanguage.resolveAmbientPreset(
            cinematic = profile == LibraryConstitutionProfile.CINEMATIC,
            amoled = profile == LibraryConstitutionProfile.AMOLED,
        )
    }

    // LOW_END profile disables ambient entirely — even a single
    // InfiniteTransition is too much for low-end devices.
    val effectivePreset = remember(ambientPreset, profile) {
        if (profile == LibraryConstitutionProfile.LOW_END) {
            LibraryAmbientPreset.OFF
        } else {
            ambientPreset
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Layer 1: Base background (A1) — color + gradient.
        LibraryBackground(
            background = background,
        )

        // Layer 2: Depth stack (A3) — subtle tint layers for visual
        // hierarchy. Skipped for AMOLED (pure black, no depth tint).
        if (profile != LibraryConstitutionProfile.AMOLED) {
            LibraryDepthStack()
        }

        // Layer 3: Ambient breathing (A4) — slow radial drift.
        // Composed last so it paints on top of the depth stack.
        LibraryAmbientLayer(
            preset = effectivePreset,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CONVENIENCE OVERLOADS
//
// Allow callers to pass individual contract pieces instead of the full
// constitution. Useful for previews and unit tests.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Convenience overload: takes the screen background contract, motion
 * language, and profile separately.
 *
 * Use this when you have the pieces but not the assembled constitution
 * (e.g. in a `@Preview` that wants to test a specific combination).
 *
 * @param background      The screen background contract (for A1).
 * @param motionLanguage  The motion language (for A4).
 * @param profile         The profile (for A3 / A4 mode).
 * @param modifier        Compose modifier.
 */
@Composable
fun LibraryBackgroundHost(
    background: com.rikodev.music.android.presentation.library.screen.contract.LibraryScreenBackground,
    motionLanguage: LibraryMotionLanguage,
    profile: LibraryConstitutionProfile = LibraryConstitutionProfile.DEFAULT,
    modifier: Modifier = Modifier,
) {
    val ambientPreset = remember(motionLanguage, profile) {
        motionLanguage.resolveAmbientPreset(
            cinematic = profile == LibraryConstitutionProfile.CINEMATIC,
            amoled = profile == LibraryConstitutionProfile.AMOLED,
        )
    }
    val effectivePreset = remember(ambientPreset, profile) {
        if (profile == LibraryConstitutionProfile.LOW_END) {
            LibraryAmbientPreset.OFF
        } else {
            ambientPreset
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LibraryBackground(background = background)

        if (profile != LibraryConstitutionProfile.AMOLED) {
            LibraryDepthStack()
        }

        LibraryAmbientLayer(preset = effectivePreset)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PREVIEW HELPERS
//
// Small composables that render the host with each profile, so a
// designer can quickly see all 8 profile variations side-by-side.
//
// (These are NOT `@Preview` annotated — that requires the Compose
// preview tooling. They are plain composables that can be used as
// preview content.)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Renders the background host with the DEFAULT profile.
 *
 * Useful as `@Preview` content or as a debug overlay.
 */
@Composable
fun LibraryBackgroundHostPreviewDefault(
    modifier: Modifier = Modifier,
) {
    LibraryBackgroundHost(
        constitution = LibraryConstitution.DEFAULT,
        modifier = modifier,
    )
}

/**
 * Renders the background host with the AMOLED profile.
 */
@Composable
fun LibraryBackgroundHostPreviewAmoled(
    modifier: Modifier = Modifier,
) {
    LibraryBackgroundHost(
        constitution = LibraryConstitution.AMOLED,
        modifier = modifier,
    )
}

/**
 * Renders the background host with the CINEMATIC profile.
 */
@Composable
fun LibraryBackgroundHostPreviewCinematic(
    modifier: Modifier = Modifier,
) {
    LibraryBackgroundHost(
        constitution = LibraryConstitution.CINEMATIC,
        modifier = modifier,
    )
}

/**
 * Renders the background host with the REDUCED_MOTION profile.
 *
 * Used to verify that reduce-motion renders as a static layer (no
 * InfiniteTransition).
 */
@Composable
fun LibraryBackgroundHostPreviewReducedMotion(
    modifier: Modifier = Modifier,
) {
    LibraryBackgroundHost(
        constitution = LibraryConstitution.REDUCED_MOTION,
        modifier = modifier,
    )
}

/**
 * Renders the background host with the LOW_END profile.
 *
 * Used to verify that LOW_END disables the ambient layer entirely.
 */
@Composable
fun LibraryBackgroundHostPreviewLowEnd(
    modifier: Modifier = Modifier,
) {
    LibraryBackgroundHost(
        constitution = LibraryConstitution.LOW_END,
        modifier = modifier,
    )
}
