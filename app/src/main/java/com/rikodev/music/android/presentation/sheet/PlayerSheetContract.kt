package com.rikodev.music.android.presentation.sheet

// ─────────────────────────────────────────────────────────────────────────────
// RIKO MUSIC — PLAYER SHEET CONTRACT (M1: sheet foundation)
//
// Lark-plan architecture: Contract -> State -> Event -> Reducer ->
// Coordinator -> Mapper -> UI. This file owns the first three links.
// The drag/settle physics live in AnchoredDraggable (foundation); the
// reducer owns every PROGRAMMATIC anchor decision so behavior is unit
// tested without a device.
//
// Rules (from the transition plan):
//   - Source of truth for the track = Media3 index, never the sheet.
//   - Reducer is pure: no Android, no audio, no nav calls.
//   - Single progress value 0..1 drives all motion (M2 choreography).
//     Progress is derived from the draggable offset and must only be read
//     inside draw/layout lambdas — never in a composable body.
// ─────────────────────────────────────────────────────────────────────────────

/** Sheet anchors. Mini peeks at the bottom; Full covers the screen. */
enum class SheetAnchor { Mini, Full }

/** Programmatic intents. Drags/flings settle inside AnchoredDraggable. */
sealed interface PlayerSheetEvent {
    data object Expand : PlayerSheetEvent
    data object Collapse : PlayerSheetEvent
    data object MiniTapped : PlayerSheetEvent
    data object BackPressed : PlayerSheetEvent
    data object TrackAutoAdvanced : PlayerSheetEvent
}

/**
 * Pure anchor decisions. No side effects — the host executes the returned
 * anchor via [PlayerSheetState.expand]/[PlayerSheetState.collapse].
 */
object PlayerSheetReducer {

    /**
     * Fraction of the Mini<->Full travel where the settle target flips.
     * Mirrors the AnchoredDraggable positional threshold configured in the
     * host — keep both at this constant or drag-settle and tap-settle
     * disagree about the midpoint.
     */
    const val SNAP_FRACTION = 0.5f

    fun targetFor(event: PlayerSheetEvent, current: SheetAnchor): SheetAnchor =
        when (event) {
            PlayerSheetEvent.Expand -> SheetAnchor.Full
            PlayerSheetEvent.MiniTapped -> SheetAnchor.Full
            PlayerSheetEvent.Collapse -> SheetAnchor.Mini
            PlayerSheetEvent.BackPressed -> SheetAnchor.Mini
            // Auto-advance never yanks the sheet: a Full sheet stays Full so
            // the new track opens in place; a Mini sheet stays Mini.
            PlayerSheetEvent.TrackAutoAdvanced -> current
        }

    /** Progress 0f (Mini) .. 1f (Full) from a drag offset in pixels. */
    fun progressFor(offsetPx: Float, miniOffsetPx: Float): Float =
        if (miniOffsetPx <= 0f) 0f
        else (1f - (offsetPx / miniOffsetPx)).coerceIn(0f, 1f)

    /**
     * M2 morph choreography: maps global sheet progress into a per-section
     * 0..1 window [start, end]. Sections declare staggered ranges so the
     * full player rises in waves (bg first, art, metadata, controls, card)
     * off ONE progress value. Pure — unit tested; callers must only read
     * the result inside draw/layout/graphicsLayer lambdas.
     */
    fun stagger(progress: Float, start: Float, end: Float): Float {
        if (end <= start) return if (progress >= end) 1f else 0f
        return ((progress - start) / (end - start)).coerceIn(0f, 1f)
    }
}
