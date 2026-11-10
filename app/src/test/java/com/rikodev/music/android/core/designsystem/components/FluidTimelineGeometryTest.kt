package com.rikodev.music.android.core.designsystem.components

import com.rikodev.music.android.audiofx.WaveformData
import org.junit.Assert.*
import org.junit.Test

class FluidTimelineGeometryTest {
    @Test fun touchAndDrawShareCoordinatesIncludingEnds() {
        for (width in listOf(160f, 360f, 720f)) for (step in 0..100) {
            val f = step / 100f
            assertEquals(f, FluidTimelineGeometry.fraction(FluidTimelineGeometry.head(f, width, 12f), width, 12f), 0.00001f)
        }
        assertEquals(0f, FluidTimelineGeometry.fraction(-20f, 360f, 12f), 0f)
        assertEquals(1f, FluidTimelineGeometry.fraction(800f, 360f, 12f), 0f)
        assertEquals(0f, FluidTimelineGeometry.fraction(5f, 20f, 12f), 0f)
        assertEquals(0f, FluidTimelineGeometry.safeFraction(Float.NaN), 0f)
    }
    @Test fun monotoneCubicNeverOvershootsItsNeighbouringAmplitude() {
        for (values in listOf(floatArrayOf(0f, 1f, 0f, 0.2f, 0.21f, 0f),
            floatArrayOf(0f, 0f, 0.01f, 0.8f, 1f, 0.3f))) {
            val curve = LiquidCurve(values)
            for (i in 0 until values.lastIndex) for (j in 0..100) {
                val y = curve.sample((i + j / 100f) / values.lastIndex)
                assertTrue(y >= minOf(values[i], values[i+1]) - 0.000001f)
                assertTrue(y <= maxOf(values[i], values[i+1]) + 0.000001f)
            }
        }
    }
    @Test fun silenceQuietMusicAndSuddenPeaksRemainDifferent() {
        fun data(level: Float) = WaveformData(FloatArray(1024) { level }, FloatArray(1024) { level * 0.6f }, 1024)
        val silent = LiquidEnvelope.from(data(0f))
        assertTrue(silent.body.all { it == 0f } && silent.depth.all { it == 0f } && silent.crest.all { it == 0f })
        val quiet = LiquidEnvelope.from(data(0.01f))
        val loud = LiquidEnvelope.from(data(0.8f))
        assertTrue(quiet.body.max() < loud.body.max() * 0.15f)
        val peak = FloatArray(1024) { 0.25f }.apply { this[500] = 1f }
        val rms = FloatArray(1024) { 0.15f }.apply { this[500] = 0.7f }
        val profile = LiquidEnvelope.from(WaveformData(peak, rms, 1024))
        assertTrue(profile.body[78] > profile.body[30])
        assertTrue(profile.body[30] > 0.20f) // one outlier must not flatten the song
    }
    @Test fun audioAndLayerGeometryAreDeterministicAndSilenceStaysSilent() {
        val peak = FloatArray(1024) { if (it in 300..700) 0f else 0.7f }
        val rms = FloatArray(1024) { peak[it] * if (it < 250) 0.7f else 0.2f }
        val data = WaveformData(peak, rms, 1024)
        val a = LiquidEnvelope.from(data)
        val b = LiquidEnvelope.from(data)
        assertArrayEquals(a.body, b.body, 0f)
        assertArrayEquals(a.depth, b.depth, 0f)
        assertEquals(0f, a.body[80], 0f)
        assertEquals(0f, a.depth[80], 0f)
        assertTrue(a.body.indices.any { kotlin.math.abs(a.body[it] - a.depth[it]) > 0.02f })
        val curve = LiquidCurve(a.body)
        val timestamp = curve.sample(0.225f)
        repeat(100) { assertEquals(timestamp, curve.sample(0.225f), 0f) }
    }
}
