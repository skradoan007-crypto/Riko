package com.rikodev.music.android.core.designsystem.riko.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
// RIKO — STATE TOKENS (§97 Phase 2 ▸ "state tokens")
//
// One entry point for every state a surface can be in. Before this file the
// press state lived in RikoMotion and the semantic/disabled/favourite/toggle
// states lived in RikoColor, so a call site had to know which file a state
// came from. Nothing here invents a value: each token delegates to the
// existing owner, so the visual language cannot drift (§8, no duplicate
// source of truth).
//
// Refs: §26 edge-state rules · §84 loading · §85 empty state ·
//       §86 accessibility contract · §115 microinteraction matrix
// ─────────────────────────────────────────────────────────────────────────────

object RikoStateTokens {

    // ── Interaction (§115 Press / Select) ────────────────────────────────────
    /** Pressed scale-down before spring return. */
    val PressScale: Float = RikoMotion.PressScale

    /** Surface dimming applied while a control is held. */
    val PressedOverlayAlpha: Float = RikoMotion.PressedOverlayAlpha

    // ── Availability (§26, §86 "enabled") ────────────────────────────────────
    /** Foreground for a control that cannot currently be acted on. */
    val Disabled: Color = RikoTextDisabled

    // ── Semantic content states (§26, §85) ───────────────────────────────────
    /** Recoverable failure — banner → recovery (§115 Error). */
    val Error: Color = RikoError

    /** Permission granted, track playable, operation confirmed. */
    val Success: Color = RikoSuccess

    /** Degraded but working — buffering, thermal pressure, battery saver. */
    val Warning: Color = RikoWarning

    // ── Content presence (§26 missing artwork, §85 empty) ────────────────────
    /** Skeleton / placeholder body — never a blank rectangle (§84, §85). */
    val Skeleton: Color = RikoSurface

    // ── Item states (§115 Select) ────────────────────────────────────────────
    val FavoriteActive: Color = RikoFavoriteActive
    val FavoriteInactive: Color = RikoFavoriteInactive
    val ToggleTrackOn: Color = RikoToggleTrackOn
    val ToggleTrackOff: Color = RikoToggleTrackOff
    val ToggleThumb: Color = RikoToggleThumb

    // ── Focus (§86 accessibility contract) ───────────────────────────────────
    /**
     * Visible focus indicator for keyboard / switch-access traversal.
     * Color reuses the primary foreground so no new hue enters the palette.
     */
    val FocusRingColor: Color = RikoTextPrimary

    /** Stroke width of [FocusRingColor]. Structural a11y constant, not tuning. */
    val FocusRingWidth = 2.dp
}
