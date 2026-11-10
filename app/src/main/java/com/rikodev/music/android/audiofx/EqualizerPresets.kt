package com.rikodev.music.android.audiofx

import kotlin.math.log10
import kotlin.math.roundToInt

/**
 * EqualizerPresets — 18 custom presets extracted from Flat Equalizer v6.3.5.8
 * (Jazib Khan / BeatBlendLabs).
 *
 * Each preset defines band gain values in millibels for the standard 5-band
 * REFERENCE layout: 60 Hz, 230 Hz, 910 Hz, 3.6 kHz, 14 kHz.
 *
 * Band level range: -1500 mB (-15 dB) to +1500 mB (+15 dB); 0 mB = flat.
 *
 * Plan 6.1: device layouts differ (5/6/10 bands at THEIR OWN center
 * frequencies) — presets are mapped to device bands by LOG-frequency
 * interpolation across the reference points, never by index (the old
 * index-mapping silently put 60 Hz energy on whatever the device's band 0
 * happens to be).
 */
object EqualizerPresets {

    /** Reference center frequencies of the 5-band preset layout, in Hz. */
    val REFERENCE_FREQS_HZ: DoubleArray = doubleArrayOf(60.0, 230.0, 910.0, 3600.0, 14000.0)

    data class Preset(
        val name: String,
        val bandLevels: List<Int>
    ) {
        init {
            require(bandLevels.size == 5) { "Presets must define exactly 5 bands" }
        }
    }

    val presets: List<Preset> = listOf(
        Preset("Normal",       listOf( 300,    0,    0,    0,  300)),
        Preset("Classical",    listOf( 500,  300, -200,  400,  400)),
        Preset("Dance",        listOf( 600,    0,  200,  400,  100)),
        Preset("Flat",         listOf(   0,    0,    0,    0,    0)),
        Preset("Folk",         listOf( 400,    0,    0,  200, -100)),
        Preset("Heavy Metal",  listOf( 400,  100,  900,  400,    0)),
        Preset("Hip Hop",      listOf( 500,  400,    0,  100,  400)),
        Preset("Jazz",         listOf( 400,  200, -200,  200,  500)),
        Preset("Pop",          listOf(-100,  200,  500,  100, -200)),
        Preset("Rock",         listOf( 500,  400, -100,  400,  500)),
        Preset("Acoustic",     listOf( 700,  400,    0,  400,  400)),
        Preset("Vocal Boost",  listOf(-300, -100,  400,  400,  200)),
        Preset("Bass Boost",   listOf(1000,  400,    0,    0,    0)),
        Preset("Treble Boost", listOf(   0,    0,  100,  400,  600)),
        Preset("Deep House",   listOf( 600,  600, -100,  400, -100)),
        Preset("EDM",          listOf( 600,    0,  400,    0, -100)),
        Preset("R&B",          listOf( 400,  100,  100,  500,  500)),
        Preset("Dream",        listOf( 800,  600, -400, -500, -600)),
    )

    val presetNames: List<String> = presets.map { it.name }

    fun getPreset(name: String): Preset? = presets.find { it.name == name }

    /**
     * Plan 6.1: frequency-correct mapping — for every DEVICE band center
     * frequency, interpolate the preset's gains in the log-frequency domain
     * (musical octaves are logarithmic; linear Hz interpolation would crush
     * the low end). Below 60 Hz / above 14 kHz the edge values hold.
     */
    fun getBandLevels(name: String, deviceFreqsHz: LongArray): List<Int> {
        val preset = getPreset(name) ?: return List(deviceFreqsHz.size) { 0 }
        return deviceFreqsHz.map { hz -> interpolateAt(preset.bandLevels, hz.toDouble()) }
    }

    private fun interpolateAt(levels: List<Int>, freqHz: Double): Int {
        val ref = REFERENCE_FREQS_HZ
        if (freqHz <= ref.first()) return levels.first()
        if (freqHz >= ref.last()) return levels.last()
        val lf = log10(freqHz)
        for (i in 0 until ref.size - 1) {
            if (freqHz >= ref[i] && freqHz <= ref[i + 1]) {
                val t = (lf - log10(ref[i])) / (log10(ref[i + 1]) - log10(ref[i]))
                return (levels[i] + t * (levels[i + 1] - levels[i])).roundToInt()
            }
        }
        return 0
    }
}
