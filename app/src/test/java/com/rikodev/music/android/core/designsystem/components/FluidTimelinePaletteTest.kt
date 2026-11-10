package com.rikodev.music.android.core.designsystem.components

import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test

class FluidTimelinePaletteTest {
    @Test fun blueArtworkProducesRestrainedBlueFamilyInsteadOfFixedPink() {
        val p = FluidTimelinePalette.forArtwork(Color.hsv(210f, 0.9f, 0.3f), Color.hsv(185f, 0.8f, 0.8f))
        assertTrue(p.body.blue > p.body.red && p.depth.blue > p.depth.red)
        assertTrue(p.body.red > 0.15f) // restrained saturation
        assertTrue(p.highlight.red > p.body.red)
        assertTrue(p.head.blue > 0.8f)
    }
    @Test fun swatchesStayFiniteAndMonochromeStaysNeutral() {
        for (hue in 0..359 step 10) {
            val tones = FluidTimelinePalette.forArtwork(Color.hsv(hue.toFloat(), 0.9f, 0.05f))
            for (c in listOf(tones.body, tones.depth, tones.highlight, tones.head)) {
                assertTrue(c.red.isFinite() && c.green.isFinite() && c.blue.isFinite())
                assertTrue(maxOf(c.red, c.green, c.blue) > 0.8f)
            }
        }
        val neutral = FluidTimelinePalette.forArtwork(Color.Gray)
        assertTrue(kotlin.math.abs(neutral.body.red - neutral.body.blue) < 0.09f)
    }
}
