package com.rikodev.music.android.audiofx

import com.rikodev.music.android.audiofx.visualizer.SpectrumEngine
import com.rikodev.music.android.audiofx.visualizer.MdnaSpectrumMath
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sin

class MdnaSpectrumResponseTest {
    private fun bands(amplitude: Double, hz: Double, size: Int = 1024): FloatArray {
        val pcm = ShortArray(size) { (amplitude * 32767 * sin(it * 2 * Math.PI * hz / 44100)).toInt().toShort() }
        val output = FloatArray(40)
        SpectrumEngine(size).analyzePcm16(pcm,1,44100,FloatArray(40),output)
        return output
    }
    @Test fun quieterPcmDoesNotSaturateAndSilenceIsZero() {
        val loud = MdnaSpectrumMath.envelope(bands(.8, 440.0),0,40)
        val quiet = MdnaSpectrumMath.envelope(bands(.08, 440.0),0,40)
        assertTrue(loud > quiet * 2.5f)
        assertTrue(loud < 1f)
        assertEquals(0f,MdnaSpectrumMath.envelope(bands(0.0,440.0),0,40),0f)
    }
    @Test fun fftSizesRetainSamePhysicalLevelAndFrequencyChangesLocalBands() {
        val a = bands(.4, 1000.0,1024); val b = bands(.4,1000.0,2048)
        assertEquals(MdnaSpectrumMath.envelope(a,0,40),MdnaSpectrumMath.envelope(b,0,40),.003f)
        val bass = bands(.4,100.0); val high = bands(.4,8000.0)
        assertTrue(bass.indices.maxBy { bass[it] } < high.indices.maxBy { high[it] })
    }
}
