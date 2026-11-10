package com.rikodev.music.android

import com.rikodev.music.android.presentation.sheet.PlayerSheetEvent
import com.rikodev.music.android.presentation.sheet.PlayerSheetReducer
import com.rikodev.music.android.presentation.sheet.SheetAnchor
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerSheetReducerTest {

    @Test fun expand_alwaysTargetsFull() {
        assertEquals(SheetAnchor.Full, PlayerSheetReducer.targetFor(PlayerSheetEvent.Expand, SheetAnchor.Mini))
        assertEquals(SheetAnchor.Full, PlayerSheetReducer.targetFor(PlayerSheetEvent.Expand, SheetAnchor.Full))
    }

    @Test fun miniTap_targetsFull() {
        assertEquals(SheetAnchor.Full, PlayerSheetReducer.targetFor(PlayerSheetEvent.MiniTapped, SheetAnchor.Mini))
    }

    @Test fun collapseAndBack_targetMini() {
        assertEquals(SheetAnchor.Mini, PlayerSheetReducer.targetFor(PlayerSheetEvent.Collapse, SheetAnchor.Full))
        assertEquals(SheetAnchor.Mini, PlayerSheetReducer.targetFor(PlayerSheetEvent.BackPressed, SheetAnchor.Full))
    }

    @Test fun autoAdvance_neverYanksSheet() {
        assertEquals(SheetAnchor.Full, PlayerSheetReducer.targetFor(PlayerSheetEvent.TrackAutoAdvanced, SheetAnchor.Full))
        assertEquals(SheetAnchor.Mini, PlayerSheetReducer.targetFor(PlayerSheetEvent.TrackAutoAdvanced, SheetAnchor.Mini))
    }

    @Test fun progress_mapsMiniToZeroAndFullToOne() {
        assertEquals(0f, PlayerSheetReducer.progressFor(1000f, 1000f))
        assertEquals(1f, PlayerSheetReducer.progressFor(0f, 1000f))
        assertEquals(0.5f, PlayerSheetReducer.progressFor(500f, 1000f))
    }

    @Test fun progress_clampsAndGuardsDegenerate() {
        assertEquals(1f, PlayerSheetReducer.progressFor(-50f, 1000f))
        assertEquals(0f, PlayerSheetReducer.progressFor(1400f, 1000f))
        assertEquals(0f, PlayerSheetReducer.progressFor(0f, 0f))
    }

    @Test fun snapFraction_isMidpoint() {
        assertEquals(0.5f, PlayerSheetReducer.SNAP_FRACTION)
    }

    @Test fun stagger_clampsOutsideWindow() {
        assertEquals(0f, PlayerSheetReducer.stagger(0f, 0.2f, 0.6f))
        assertEquals(0f, PlayerSheetReducer.stagger(0.19f, 0.2f, 0.6f))
        assertEquals(1f, PlayerSheetReducer.stagger(0.6f, 0.2f, 0.6f))
        assertEquals(1f, PlayerSheetReducer.stagger(1f, 0.2f, 0.6f))
    }

    @Test fun stagger_interpolatesInsideWindow() {
        assertEquals(0f, PlayerSheetReducer.stagger(0.2f, 0.2f, 0.6f))
        // IEEE float: (0.4-0.2)/(0.6-0.2) == 0.49999997, not exactly 0.5 —
        // compare with tolerance, never exact equality, for interior points.
        assertEquals(0.5f, PlayerSheetReducer.stagger(0.4f, 0.2f, 0.6f), 1e-6f)
        assertEquals(1f, PlayerSheetReducer.stagger(0.6f, 0.2f, 0.6f))
    }

    @Test fun stagger_degenerateWindowIsStepFunction() {
        assertEquals(0f, PlayerSheetReducer.stagger(0.49f, 0.5f, 0.5f))
        assertEquals(1f, PlayerSheetReducer.stagger(0.5f, 0.5f, 0.5f))
        assertEquals(1f, PlayerSheetReducer.stagger(0.9f, 0.5f, 0.5f))
    }
}
