package com.rikodev.music.android.audiofx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plan 6.1 — preset mapping correctness across real device layouts:
 * reference-frequency fidelity, 10-band interpolation, edge clamping.
 */
class EqualizerPresetsMappingTest {

    private val reference = EqualizerPresets.REFERENCE_FREQS_HZ.map { it.toLong() }.toLongArray()

    @Test
    fun referenceLayout_mapsExactlyToPresetLevels() {
        // A device with exactly the 5 reference bands must receive the raw
        // preset values — interpolation must not round-trip-drift.
        for (preset in EqualizerPresets.presets) {
            val mapped = EqualizerPresets.getBandLevels(preset.name, reference)
            assertEquals(preset.name, preset.bandLevels, mapped)
        }
    }

    @Test
    fun tenBandDevice_interpolatesBetweenReferencePoints() {
        // 10-band device: 31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000.
        val ten = longArrayOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
        val mapped = EqualizerPresets.getBandLevels("Bass Boost", ten)
        assertEquals(10, mapped.size)
        // Bass Boost = [1000, 400, 0, 0, 0]: edges clamp to the nearest value.
        assertEquals(1000, mapped[0])   // 31 Hz below the 60 Hz reference
        // 62 Hz interpolates between 1000 (60Hz) and 400 (230Hz).
        assertTrue("interpolated ${mapped[1]}", mapped[1] in 900..999)
        // 125 Hz interpolates between 1000 (60Hz) and 400 (230Hz) — strictly between.
        assertTrue("interpolated ${mapped[2]}", mapped[2] in 401..999)
        // High band clamps to the last reference value (0 for Bass Boost).
        assertEquals(0, mapped[9])
    }

    @Test
    fun sixBandDevice_saneInterpolation() {
        val six = longArrayOf(60, 230, 910, 3600, 14000, 16000)
        val mapped = EqualizerPresets.getBandLevels("Rock", six)
        // Rock = [500, 400, -100, 400, 500]: first five match exactly, last clamps.
        assertEquals(listOf(500, 400, -100, 400, 500, 500), mapped)
    }

    @Test
    fun unknownPreset_fallsBackToFlat() {
        val mapped = EqualizerPresets.getBandLevels("No Such Preset", reference)
        assertEquals(List(5) { 0 }, mapped)
    }

    @Test
    fun edgeFrequencies_clampToEdgeLevels() {
        val mapped = EqualizerPresets.getBandLevels("Treble Boost", longArrayOf(1, 25000))
        // Treble Boost = [0, 0, 100, 400, 600]
        assertEquals(0, mapped[0])
        assertEquals(600, mapped[1])
    }
}
