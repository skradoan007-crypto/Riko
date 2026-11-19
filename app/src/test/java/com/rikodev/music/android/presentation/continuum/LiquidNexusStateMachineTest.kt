package com.rikodev.music.android.presentation.continuum

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidNexusStateMachineTest {

    @Test
    fun `expand and settle transitions Compact to Expanding to Expanded`() {
        val initial = LiquidNexusState(mode = LiquidNexusMode.Compact, activeTrackId = 42L)
        val (expanding, _) = LiquidNexusStateMachine.reduce(initial, LiquidNexusEvent.ExpandRequested)
        assertEquals(LiquidNexusMode.Expanding, expanding.mode)

        val (expanded, _) = LiquidNexusStateMachine.reduce(
            expanding,
            LiquidNexusEvent.AnimationSettled(LiquidNexusMode.Expanded)
        )
        assertEquals(LiquidNexusMode.Expanded, expanded.mode)
    }

    @Test
    fun `mid-animation interruption reverses Expanding to Collapsing and back`() {
        val initial = LiquidNexusState(mode = LiquidNexusMode.Compact, activeTrackId = 7L)
        val (expanding, _) = LiquidNexusStateMachine.reduce(initial, LiquidNexusEvent.ExpandRequested)
        val (collapsing, _) = LiquidNexusStateMachine.reduce(expanding, LiquidNexusEvent.CollapseRequested)
        assertEquals(LiquidNexusMode.Collapsing, collapsing.mode)

        val (reExpanding, _) = LiquidNexusStateMachine.reduce(collapsing, LiquidNexusEvent.ExpandRequested)
        assertEquals(LiquidNexusMode.Expanding, reExpanding.mode)
    }

    @Test
    fun `play pause executes immediately regardless of transition state`() {
        val expanding = LiquidNexusState(mode = LiquidNexusMode.Expanding, activeTrackId = 9L)
        val (next, effects) = LiquidNexusStateMachine.reduce(expanding, LiquidNexusEvent.PlayPauseRequested)
        assertEquals(LiquidNexusMode.Expanding, next.mode)
        assertEquals(listOf(LiquidNexusEffect.TogglePlayPause), effects)
    }

    @Test
    fun `scrubbing updates preview without seeking until commit and activates background blur`() {
        val expanded = LiquidNexusState(mode = LiquidNexusMode.Expanded, activeTrackId = 10L)
        val (scrubbing, startEffects) = LiquidNexusStateMachine.reduce(
            expanded,
            LiquidNexusEvent.ScrubStarted(initialFraction = 0.25f, durationMs = 200_000L)
        )
        assertEquals(LiquidNexusMode.Scrubbing, scrubbing.mode)
        assertTrue(scrubbing.shouldBlurBackground)
        assertEquals(50_000L, scrubbing.scrubPreviewMs)
        assertTrue(startEffects.isEmpty())

        val (moved, moveEffects) = LiquidNexusStateMachine.reduce(
            scrubbing,
            LiquidNexusEvent.ScrubPositionChanged(0.625f)
        )
        assertEquals(125_000L, moved.scrubPreviewMs)
        assertTrue(moveEffects.isEmpty())

        val (committed, commitEffects) = LiquidNexusStateMachine.reduce(
            moved,
            LiquidNexusEvent.ScrubCommitted
        )
        assertEquals(LiquidNexusMode.Expanded, committed.mode)
        assertFalse(committed.shouldBlurBackground)
        assertNull(committed.scrubFraction)
        assertEquals(listOf(LiquidNexusEffect.CommitSeek(125_000L)), commitEffects)
    }

    @Test
    fun `track change during scrubbing exits scrubbing safely without committing stale seek`() {
        val scrubbing = LiquidNexusState(
            mode = LiquidNexusMode.Scrubbing,
            activeTrackId = 10L,
            scrubFraction = 0.5f,
            scrubPreviewMs = 90_000L,
            scrubDurationMs = 180_000L
        )
        val (next, effects) = LiquidNexusStateMachine.reduce(
            scrubbing,
            LiquidNexusEvent.TrackChanged(songId = 11L, durationMs = 210_000L)
        )
        assertEquals(LiquidNexusMode.Expanded, next.mode)
        assertNull(next.scrubFraction)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `back press follows strict hierarchy FullPlayer to Expanded to Collapsing`() {
        val full = LiquidNexusState(mode = LiquidNexusMode.FullPlayer, activeTrackId = 1L)
        val (afterBack1, fx1) = LiquidNexusStateMachine.reduce(full, LiquidNexusEvent.BackPressed)
        assertEquals(LiquidNexusMode.Expanded, afterBack1.mode)
        assertEquals(listOf(LiquidNexusEffect.CloseFullPlayer), fx1)

        val (afterBack2, fx2) = LiquidNexusStateMachine.reduce(afterBack1, LiquidNexusEvent.BackPressed)
        assertEquals(LiquidNexusMode.Collapsing, afterBack2.mode)
        assertTrue(fx2.isEmpty())
    }

    @Test
    fun `lifecycle restore normalizes transient states into stable states`() {
        val expanding = LiquidNexusState(mode = LiquidNexusMode.Expanding, activeTrackId = 3L)
        val (normExp, _) = LiquidNexusStateMachine.reduce(expanding, LiquidNexusEvent.LifecycleRestored)
        assertEquals(LiquidNexusMode.Expanded, normExp.mode)

        val collapsing = LiquidNexusState(mode = LiquidNexusMode.Collapsing, activeTrackId = 3L)
        val (normCol, _) = LiquidNexusStateMachine.reduce(collapsing, LiquidNexusEvent.LifecycleRestored)
        assertEquals(LiquidNexusMode.Compact, normCol.mode)
    }
}
