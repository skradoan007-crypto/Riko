package com.rikodev.music.android.presentation.nowplaying.visualization

import com.rikodev.music.android.core.experience.wave.WaveEnergy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpectrumLevelSourceTest {

    @Test fun liveTap_resamplesToConfiguredBands() {
        val source = SpectrumLevelSource(32)
        val dsp40 = FloatArray(40) { it / 39f }
        val energy = WaveEnergy()
        source.update(
            source = dsp40,
            sourcePeaks = null,
            tapLive = true,
            waveActive = true,
            energy = energy,
            pulseAmount = 0f,
            sensitivity = 1.0f
        )
        assertEquals(32, source.levels.size)
        assertEquals(0f, source.levels.first(), 1e-4f)
        assertEquals(1f, source.levels.last(), 1e-3f)
        // Monotonic ramp preserved
        var prev = -1f
        for (v in source.levels) {
            assertTrue(v >= prev - 1e-5f)
            prev = v
        }
    }

    @Test fun liveTap_handlesPeaksWhenPresent() {
        val source = SpectrumLevelSource(40)
        val dsp = FloatArray(40) { 0.5f }
        val peaks = FloatArray(40) { 0.8f }
        source.update(
            source = dsp,
            sourcePeaks = peaks,
            tapLive = true,
            waveActive = true,
            energy = WaveEnergy(),
            pulseAmount = 0f,
            sensitivity = 1.0f
        )
        assertTrue(source.hasPeaks)
        assertEquals(0.8f, source.peaks[0], 1e-4f)
    }

    @Test fun sensitivity_scalesBothLevelsAndPeaks() {
        val source = SpectrumLevelSource(10)
        val dsp = FloatArray(10) { 0.4f }
        val peaks = FloatArray(10) { 0.6f }
        source.update(
            source = dsp,
            sourcePeaks = peaks,
            tapLive = true,
            waveActive = true,
            energy = WaveEnergy(),
            pulseAmount = 0f,
            sensitivity = 1.5f
        )
        assertEquals(0.6f, source.levels[0], 1e-4f) // 0.4 * 1.5
        assertEquals(0.9f, source.peaks[0], 1e-4f)  // 0.6 * 1.5
    }

    @Test fun fallback_synthesizesFromWaveEnergyWhenTapNotLive() {
        val source = SpectrumLevelSource(40)
        val energy = WaveEnergy(
            bassPressure = 0.9f,
            midTexture = 0.5f,
            trebleSpark = 0.2f,
            overallEnergy = 0.7f
        )
        source.update(
            source = FloatArray(0),
            sourcePeaks = null,
            tapLive = false,
            waveActive = true,
            energy = energy,
            pulseAmount = 0f,
            sensitivity = 1.0f
        )
        assertTrue("Levels must not be completely silent in fallback", source.levels.any { it > 0.05f })
        assertTrue("Peaks must be disabled in fallback", !source.hasPeaks)
    }

    @Test fun zeroAllocations_reusesBuffersAcrossUpdates() {
        val source = SpectrumLevelSource(40)
        val initialLevelsRef = source.levels
        val initialPeaksRef = source.peaks
        source.update(
            source = FloatArray(40) { 0.2f },
            sourcePeaks = null,
            tapLive = true,
            waveActive = true,
            energy = WaveEnergy(),
            pulseAmount = 0f,
            sensitivity = 1.0f
        )
        // Array references must NEVER change — no garbage per update
        assertTrue(initialLevelsRef === source.levels)
        assertTrue(initialPeaksRef === source.peaks)
    }
}
