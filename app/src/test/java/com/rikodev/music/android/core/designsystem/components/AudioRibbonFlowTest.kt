package com.rikodev.music.android.core.designsystem.components

import org.junit.Assert.*
import org.junit.Test

class AudioRibbonFlowTest {
    @Test fun silenceNeverProducesDecorativeWaves() {
        val flow = AudioRibbonFlow()
        repeat(600) { flow.advance(1f / 60f, 0f) }
        assertFalse(flow.hasHistory)
        for (layer in 0..2) for (i in 0..100) assertEquals(0f, flow.height(layer, i / 100f), 0f)
    }
    @Test fun firstResponseDoesNotDependOnFullSongData() {
        val flow = AudioRibbonFlow()
        repeat(18) { flow.advance(1f / 60f, 0.4f) }
        assertTrue(flow.hasHistory)
        assertTrue((0..100).any { flow.height(0, it / 100f) > 0.02f })
    }
    @Test fun crestsTravelLeftAndAllLayersJoinTheRail() {
        val flow = AudioRibbonFlow()
        repeat(18) { flow.advance(1f/60f, 0.5f) }
        fun crest(): Int = (0..100).maxBy { flow.height(0, it/100f) }
        val first = crest()
        repeat(18) { flow.advance(1f/60f, 0f) }
        assertTrue(crest() < first)
        for (layer in 0..2) {
            assertEquals(0f, flow.height(layer, 0f), 0f)
            assertEquals(0f, flow.height(layer, 1f), 0f)
        }
    }
    @Test fun pauseFreezesAndResetDropsOldTrackHistory() {
        val flow = AudioRibbonFlow()
        repeat(90) { flow.advance(1f/60f, 0.3f) }
        val before = FloatArray(101) { flow.height(1, it/100f) }
        repeat(100) { assertArrayEquals(before, FloatArray(101) { flow.height(1, it/100f) }, 0f) }
        flow.reset()
        assertFalse(flow.hasHistory)
        for (i in 0..100) assertEquals(0f, flow.height(1, i/100f), 0f)
    }
    @Test fun boundedAudioRelatedLayersRemainDistinctWithoutABlock() {
        val quiet = AudioRibbonFlow(); val loud = AudioRibbonFlow()
        repeat(75) { quiet.advance(1f/60f, 0.01f); loud.advance(1f/60f, 0.6f) }
        var different = false
        for (layer in 0..2) for (i in 0..100) {
            val u = i/100f
            assertTrue(loud.height(layer,u) in 0f..1f)
            if (kotlin.math.abs(loud.height(0,u)-loud.height(1,u))>0.02f) different = true
        }
        assertTrue(different)
        assertTrue((0..100).maxOf { quiet.height(0,it/100f) } < (0..100).maxOf { loud.height(0,it/100f) })
    }
}
